package org.dcm4che.ris.hl7.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.hl7.HL7Exception;
import org.dcm4che.hl7.HL7Message;
import org.dcm4che.hl7.HL7Segment;
import org.dcm4che.ris.api.entity.*;
import org.dcm4che.ris.api.enums.OrderStatus;
import org.dcm4che.ris.core.service.OrderService;
import org.dcm4che.ris.core.service.PatientService;
import org.dcm4che.ris.hl7.config.HL7Configuration;
import org.dcm4che.ris.persistence.repository.PhysicianRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for processing HL7 messages and synchronizing with RIS database.
 * Provides common HL7 parsing and entity creation logic.
 *
 * @author dcm4che-ris
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class HL7MessageService {

    private final PatientService patientService;
    private final OrderService orderService;
    private final PhysicianRepository physicianRepository;
    private final HL7Configuration hl7Configuration;

    // HL7 date/time formatters
    private static final DateTimeFormatter HL7_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HL7_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Process ADT (Admission, Discharge, Transfer) message.
     * Creates or updates patient demographics.
     */
    public Patient processADTMessage(HL7Message msg) throws HL7Exception {
        log.info("Processing ADT message: {}", msg.msh().getMessageType());

        // Extract PID segment
        HL7Segment pid = msg.getSegment("PID");
        if (pid == null) {
            throw new HL7Exception("PID segment is required in ADT message");
        }

        // Extract patient data
        String mrn = extractMRN(pid);
        String issuerOfPatientId = extractIssuerOfPatientId(pid);
        String patientName = pid.getField(5, ""); // PID-5: Patient Name
        String birthDate = pid.getField(7, ""); // PID-7: Date of Birth
        String sex = pid.getField(8, ""); // PID-8: Sex
        String address = pid.getField(11, ""); // PID-11: Patient Address
        String phone = pid.getField(13, ""); // PID-13: Phone Number
        String email = pid.getField(13, ""); // Can be in PID-13 or custom field

        // Parse birth date
        LocalDate birthDateParsed = null;
        if (birthDate != null && !birthDate.isEmpty()) {
            try {
                birthDateParsed = LocalDate.parse(birthDate, HL7_DATE_FORMAT);
            } catch (Exception e) {
                log.warn("Invalid birth date format: {}", birthDate);
            }
        }

        // Check if patient exists
        Patient patient = patientService.findByMRN(mrn, issuerOfPatientId).orElse(null);

        if (patient == null && hl7Configuration.isAutoCreatePatients()) {
            // Create new patient
            log.info("Creating new patient from ADT: MRN={}, Name={}", mrn, patientName);

            patient = Patient.builder()
                    .mrn(mrn)
                    .issuerOfPatientId(issuerOfPatientId)
                    .patientName(patientName)
                    .birthDate(birthDateParsed)
                    .sex(sex)
                    .address(address)
                    .phoneNumbers(phone)
                    .email(email)
                    .patientState("ACTIVE")
                    .build();

            patient = patientService.createPatient(patient);
            log.info("Patient created successfully: {}", patient.getPatientId());

        } else if (patient != null) {
            // Update existing patient
            log.info("Updating existing patient from ADT: MRN={}", mrn);

            patient.setPatientName(patientName);
            patient.setBirthDate(birthDateParsed);
            patient.setSex(sex);
            patient.setAddress(address);
            patient.setPhoneNumbers(phone);
            patient.setEmail(email);

            patient = patientService.updatePatient(patient.getPatientId(), patient);
            log.info("Patient updated successfully: {}", patient.getPatientId());

        } else {
            log.warn("Auto-create patients is disabled and patient not found: MRN={}", mrn);
            throw new HL7Exception("Patient not found and auto-create is disabled: " + mrn);
        }

        return patient;
    }

    /**
     * Process ORM (Order Message) message.
     * Creates imaging service request (order).
     */
    public ImagingServiceRequest processORMMessage(HL7Message msg) throws HL7Exception {
        log.info("Processing ORM message: {}", msg.msh().getMessageType());

        // Extract PID segment
        HL7Segment pid = msg.getSegment("PID");
        if (pid == null) {
            throw new HL7Exception("PID segment is required in ORM message");
        }

        // Extract ORC segment (Order Control)
        HL7Segment orc = msg.getSegment("ORC");
        if (orc == null) {
            throw new HL7Exception("ORC segment is required in ORM message");
        }

        // Extract OBR segment (Observation Request)
        HL7Segment obr = msg.getSegment("OBR");
        if (obr == null) {
            throw new HL7Exception("OBR segment is required in ORM message");
        }

        // Get or create patient
        String mrn = extractMRN(pid);
        String issuerOfPatientId = extractIssuerOfPatientId(pid);

        Patient patient = patientService.findByMRN(mrn, issuerOfPatientId)
                .orElseThrow(() -> new HL7Exception("Patient not found: " + mrn));

        // Extract order data
        String orderControl = orc.getField(1, ""); // ORC-1: Order Control (NW=New, CA=Cancel, etc.)
        String placerOrderNumber = orc.getField(2, ""); // ORC-2: Placer Order Number
        String orderPriority = orc.getField(7, "ROUTINE"); // ORC-7: Priority (ROUTINE, URGENT, STAT)

        // Extract procedure data from OBR
        String procedureCode = obr.getField(4, ""); // OBR-4: Universal Service ID
        String reasonForExam = obr.getField(31, ""); // OBR-31: Reason for Study
        String clinicalInfo = obr.getField(13, ""); // OBR-13: Relevant Clinical Info

        // Extract ordering physician
        String orderingPhysician = obr.getField(16, ""); // OBR-16: Ordering Provider

        log.info("Creating order: Placer={}, Patient MRN={}, Procedure={}, Priority={}",
                placerOrderNumber, mrn, procedureCode, orderPriority);

        // Handle order control
        if ("NW".equals(orderControl)) {
            // New order
            if (!hl7Configuration.isAutoCreateOrders()) {
                throw new HL7Exception("Auto-create orders is disabled");
            }

            ImagingServiceRequest order = ImagingServiceRequest.builder()
                    .patient(patient)
                    .placerOrderNumber(placerOrderNumber)
                    .orderPriority(orderPriority)
                    .reasonForExam(reasonForExam)
                    .clinicalInfo(clinicalInfo)
                    .orderStatus(OrderStatus.PENDING)
                    .build();

            order = orderService.createOrder(order);
            log.info("Order created successfully: {}", order.getOrderId());

            return order;

        } else if ("CA".equals(orderControl)) {
            // Cancel order
            ImagingServiceRequest order = orderService.findByPlacerOrderNumber(placerOrderNumber)
                    .orElseThrow(() -> new HL7Exception("Order not found: " + placerOrderNumber));

            orderService.cancelOrder(order.getOrderId(), "Canceled via HL7 ORM message");
            log.info("Order canceled successfully: {}", order.getOrderId());

            return order;

        } else {
            throw new HL7Exception("Unsupported order control: " + orderControl);
        }
    }

    /**
     * Extract MRN (Medical Record Number) from PID segment.
     * PID-3: Patient Identifier List
     */
    private String extractMRN(HL7Segment pid) throws HL7Exception {
        // PID-3 can contain multiple identifiers in format: ID^^^&AssigningAuthority&ISO^MR
        String patientId = pid.getField(3, "");

        if (patientId == null || patientId.isEmpty()) {
            throw new HL7Exception("Patient ID (PID-3) is required");
        }

        // Simple extraction - just get the first component
        // In production, would parse the full composite field
        String[] components = patientId.split("\\^");
        return components[0];
    }

    /**
     * Extract Issuer of Patient ID from PID segment.
     */
    private String extractIssuerOfPatientId(HL7Segment pid) {
        String patientId = pid.getField(3, "");

        if (patientId == null || patientId.isEmpty()) {
            return hl7Configuration.getDefaultIssuerOfPatientId();
        }

        // Parse composite field: ID^^^&AssigningAuthority&ISO^MR
        String[] components = patientId.split("\\^");
        if (components.length >= 4) {
            String authorityComponent = components[3];
            if (authorityComponent.startsWith("&") && authorityComponent.contains("&")) {
                String[] authParts = authorityComponent.split("&");
                if (authParts.length >= 2) {
                    return authParts[1];
                }
            }
        }

        return hl7Configuration.getDefaultIssuerOfPatientId();
    }

    /**
     * Parse HL7 date (YYYYMMDD)
     */
    public LocalDate parseHL7Date(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, HL7_DATE_FORMAT);
        } catch (Exception e) {
            log.warn("Invalid HL7 date format: {}", dateStr);
            return null;
        }
    }

    /**
     * Parse HL7 datetime (YYYYMMDDHHMMSS)
     */
    public LocalDateTime parseHL7DateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, HL7_DATETIME_FORMAT);
        } catch (Exception e) {
            log.warn("Invalid HL7 datetime format: {}", dateTimeStr);
            return null;
        }
    }

    /**
     * Format LocalDate as HL7 date
     */
    public String formatHL7Date(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(HL7_DATE_FORMAT);
    }

    /**
     * Format LocalDateTime as HL7 datetime
     */
    public String formatHL7DateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(HL7_DATETIME_FORMAT);
    }
}
