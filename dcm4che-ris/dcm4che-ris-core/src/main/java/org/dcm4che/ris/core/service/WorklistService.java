package org.dcm4che.ris.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4che.ris.api.entity.Patient;
import org.dcm4che.ris.api.entity.ScheduledProcedureStep;
import org.dcm4che.ris.api.entity.RequestedProcedure;
import org.dcm4che.ris.api.entity.ImagingServiceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating DICOM Modality Worklist (MWL) data.
 * Converts database entities to DICOM Attributes format.
 *
 * @author dcm4che-ris
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorklistService {

    private final SchedulingService schedulingService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Get worklist items for a specific modality and date range.
     * Returns DICOM Attributes for MWL C-FIND SCP response.
     */
    public List<Attributes> getWorklistItems(String modality, LocalDate startDate, LocalDate endDate) {
        log.debug("Generating worklist for modality: {} from {} to {}", modality, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Get scheduled procedures from database
        List<ScheduledProcedureStep> scheduledProcedures =
                schedulingService.getWorklistForModality(modality, startDateTime, endDateTime);

        // Convert each SPS to DICOM Attributes
        List<Attributes> worklistItems = scheduledProcedures.stream()
                .map(this::convertSpsToAttributes)
                .collect(Collectors.toList());

        log.info("Generated {} worklist items for modality: {}", worklistItems.size(), modality);
        return worklistItems;
    }

    /**
     * Get today's worklist for a modality.
     */
    public List<Attributes> getTodayWorklist(String modality) {
        LocalDate today = LocalDate.now();
        return getWorklistItems(modality, today, today);
    }

    /**
     * Convert ScheduledProcedureStep entity to DICOM Attributes.
     * Implements DICOM Modality Worklist Information Model.
     */
    private Attributes convertSpsToAttributes(ScheduledProcedureStep sps) {
        Attributes attrs = new Attributes();

        // Get related entities
        RequestedProcedure rp = sps.getRequestedProcedure();
        ImagingServiceRequest order = rp.getOrder();
        Patient patient = order.getPatient();

        // Patient Module (Level: PATIENT)
        addPatientAttributes(attrs, patient);

        // Imaging Service Request Module (Level: VISIT)
        addImagingServiceRequestAttributes(attrs, order);

        // Requested Procedure Module (Level: REQUESTED PROCEDURE)
        addRequestedProcedureAttributes(attrs, rp);

        // Scheduled Procedure Step Module (Level: SCHEDULED PROCEDURE STEP)
        addScheduledProcedureStepAttributes(attrs, sps);

        return attrs;
    }

    /**
     * Add Patient Module attributes.
     */
    private void addPatientAttributes(Attributes attrs, Patient patient) {
        // Patient's Name (0010,0010) PN
        if (patient.getPatientName() != null) {
            attrs.setString(Tag.PatientName, VR.PN, patient.getPatientName());
        }

        // Patient ID (0010,0020) LO
        if (patient.getMrn() != null) {
            attrs.setString(Tag.PatientID, VR.LO, patient.getMrn());
        }

        // Issuer of Patient ID (0010,0021) LO
        if (patient.getIssuerOfPatientId() != null) {
            attrs.setString(Tag.IssuerOfPatientID, VR.LO, patient.getIssuerOfPatientId());
        }

        // Patient's Birth Date (0010,0030) DA
        if (patient.getBirthDate() != null) {
            attrs.setString(Tag.PatientBirthDate, VR.DA,
                    patient.getBirthDate().format(DATE_FORMATTER));
        }

        // Patient's Sex (0010,0040) CS
        if (patient.getSex() != null) {
            attrs.setString(Tag.PatientSex, VR.CS, patient.getSex());
        }

        // Patient's Weight (0010,1030) DS
        if (patient.getWeight() != null) {
            attrs.setString(Tag.PatientWeight, VR.DS, patient.getWeight().toString());
        }

        // Patient's Size (0010,1020) DS (height in meters)
        if (patient.getHeight() != null) {
            double heightInMeters = patient.getHeight() / 100.0; // cm to meters
            attrs.setString(Tag.PatientSize, VR.DS, String.format("%.2f", heightInMeters));
        }

        // Pregnancy Status (0010,21C0) US
        if (patient.getPregnancyStatus() != null) {
            int pregnancyCode = switch (patient.getPregnancyStatus()) {
                case "NOT_PREGNANT" -> 1;
                case "PREGNANT" -> 3;
                case "POSSIBLY_PREGNANT" -> 2;
                default -> 4; // Unknown
            };
            attrs.setInt(Tag.PregnancyStatus, VR.US, pregnancyCode);
        }

        // Medical Alerts (0010,2000) LO
        if (patient.getMedicalAlerts() != null) {
            attrs.setString(Tag.MedicalAlerts, VR.LO, patient.getMedicalAlerts());
        }

        // Allergies (0010,2110) LO
        if (patient.getAllergies() != null) {
            attrs.setString(Tag.Allergies, VR.LO, patient.getAllergies());
        }
    }

    /**
     * Add Imaging Service Request attributes.
     */
    private void addImagingServiceRequestAttributes(Attributes attrs, ImagingServiceRequest order) {
        // Accession Number (0008,0050) SH
        if (order.getRequestedProcedures() != null && !order.getRequestedProcedures().isEmpty()) {
            String accessionNumber = order.getRequestedProcedures().get(0).getAccessionNumber();
            if (accessionNumber != null) {
                attrs.setString(Tag.AccessionNumber, VR.SH, accessionNumber);
            }
        }

        // Referring Physician's Name (0008,0090) PN
        if (order.getRequestingPhysician() != null &&
            order.getRequestingPhysician().getPhysicianName() != null) {
            attrs.setString(Tag.ReferringPhysicianName, VR.PN,
                    order.getRequestingPhysician().getPhysicianName());
        }

        // Requested Procedure Priority (0040,1003) SH
        if (order.getOrderPriority() != null) {
            attrs.setString(Tag.RequestedProcedurePriority, VR.SH, order.getOrderPriority());
        }
    }

    /**
     * Add Requested Procedure attributes.
     */
    private void addRequestedProcedureAttributes(Attributes attrs, RequestedProcedure rp) {
        // Requested Procedure ID (0040,1001) SH
        if (rp.getRequestedProcedureId() != null) {
            attrs.setString(Tag.RequestedProcedureID, VR.SH,
                    rp.getRequestedProcedureId().toString());
        }

        // Requested Procedure Description (0032,1060) LO
        if (rp.getRequestedProcedureDescription() != null) {
            attrs.setString(Tag.RequestedProcedureDescription, VR.LO,
                    rp.getRequestedProcedureDescription());
        }

        // Study Instance UID (0020,000D) UI
        if (rp.getStudyInstanceUID() != null) {
            attrs.setString(Tag.StudyInstanceUID, VR.UI, rp.getStudyInstanceUID());
        }

        // Requested Procedure Code Sequence (0032,1064) SQ
        if (rp.getRequestedProcedureCode() != null) {
            Attributes codeItem = new Attributes();
            codeItem.setString(Tag.CodeValue, VR.SH, rp.getRequestedProcedureCode());
            if (rp.getRequestedProcedureDescription() != null) {
                codeItem.setString(Tag.CodeMeaning, VR.LO, rp.getRequestedProcedureDescription());
            }
            attrs.newSequence(Tag.RequestedProcedureCodeSequence, 1).add(codeItem);
        }
    }

    /**
     * Add Scheduled Procedure Step attributes.
     */
    private void addScheduledProcedureStepAttributes(Attributes attrs, ScheduledProcedureStep sps) {
        // Scheduled Procedure Step Sequence (0040,0100) SQ
        Attributes spsItem = new Attributes();

        // Scheduled Station AE Title (0040,0001) AE
        if (sps.getScheduledStationAETitle() != null) {
            spsItem.setString(Tag.ScheduledStationAETitle, VR.AE, sps.getScheduledStationAETitle());
        }

        // Scheduled Procedure Step Start Date (0040,0002) DA
        // Scheduled Procedure Step Start Time (0040,0003) TM
        if (sps.getScheduledStartDateTime() != null) {
            spsItem.setString(Tag.ScheduledProcedureStepStartDate, VR.DA,
                    sps.getScheduledStartDateTime().format(DATE_FORMATTER));
            spsItem.setString(Tag.ScheduledProcedureStepStartTime, VR.TM,
                    sps.getScheduledStartDateTime().format(TIME_FORMATTER));
        }

        // Modality (0008,0060) CS
        if (sps.getModality() != null) {
            spsItem.setString(Tag.Modality, VR.CS, sps.getModality());
        }

        // Scheduled Performing Physician's Name (0040,0006) PN
        if (sps.getPerformingPhysician() != null &&
            sps.getPerformingPhysician().getPhysicianName() != null) {
            spsItem.setString(Tag.ScheduledPerformingPhysicianName, VR.PN,
                    sps.getPerformingPhysician().getPhysicianName());
        }

        // Scheduled Procedure Step Description (0040,0007) LO
        if (sps.getScheduledProcedureStepDescription() != null) {
            spsItem.setString(Tag.ScheduledProcedureStepDescription, VR.LO,
                    sps.getScheduledProcedureStepDescription());
        }

        // Scheduled Station Name (0040,0010) SH
        if (sps.getScheduledStationName() != null) {
            spsItem.setString(Tag.ScheduledStationName, VR.SH, sps.getScheduledStationName());
        }

        // Scheduled Procedure Step ID (0040,0009) SH
        if (sps.getScheduledProcedureStepId() != null) {
            spsItem.setString(Tag.ScheduledProcedureStepID, VR.SH,
                    sps.getScheduledProcedureStepId());
        }

        // Scheduled Procedure Step Status (0040,0020) CS
        if (sps.getSpsStatus() != null) {
            spsItem.setString(Tag.ScheduledProcedureStepStatus, VR.CS,
                    sps.getSpsStatus().name());
        }

        // Scheduled Protocol Code Sequence (0040,0008) SQ
        if (sps.getScheduledProtocolCode() != null) {
            Attributes protocolItem = new Attributes();
            protocolItem.setString(Tag.CodeValue, VR.SH, sps.getScheduledProtocolCode());
            spsItem.newSequence(Tag.ScheduledProtocolCodeSequence, 1).add(protocolItem);
        }

        // Pre-Medication (0040,0012) LO
        if (sps.getPreMedication() != null) {
            spsItem.setString(Tag.PreMedication, VR.LO, sps.getPreMedication());
        }

        // Comments on the Scheduled Procedure Step (0040,0400) LT
        if (sps.getComments() != null) {
            spsItem.setString(Tag.CommentsOnTheScheduledProcedureStep, VR.LT, sps.getComments());
        }

        // Add SPS item to sequence
        attrs.newSequence(Tag.ScheduledProcedureStepSequence, 1).add(spsItem);
    }

    /**
     * Search worklist items by patient name (for MWL queries with patient name filter).
     */
    public List<Attributes> searchWorklistByPatientName(String modality, String patientName,
                                                         LocalDate startDate, LocalDate endDate) {
        List<Attributes> allItems = getWorklistItems(modality, startDate, endDate);

        // Filter by patient name
        return allItems.stream()
                .filter(attrs -> {
                    String name = attrs.getString(Tag.PatientName);
                    return name != null && name.toUpperCase().contains(patientName.toUpperCase());
                })
                .collect(Collectors.toList());
    }

    /**
     * Search worklist items by patient ID (MRN).
     */
    public List<Attributes> searchWorklistByPatientId(String modality, String patientId,
                                                       LocalDate startDate, LocalDate endDate) {
        List<Attributes> allItems = getWorklistItems(modality, startDate, endDate);

        // Filter by patient ID
        return allItems.stream()
                .filter(attrs -> {
                    String id = attrs.getString(Tag.PatientID);
                    return id != null && id.equals(patientId);
                })
                .collect(Collectors.toList());
    }
}
