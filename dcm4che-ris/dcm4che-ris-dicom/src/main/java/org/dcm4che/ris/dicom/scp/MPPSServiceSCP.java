package org.dcm4che.ris.dicom.scp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.UID;
import org.dcm4che.net.Association;
import org.dcm4che.net.Commands;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.Status;
import org.dcm4che.net.pdu.PresentationContext;
import org.dcm4che.net.service.BasicMPPSSCP;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4che.ris.api.entity.PerformedProcedureStep;
import org.dcm4che.ris.api.entity.ScheduledProcedureStep;
import org.dcm4che.ris.api.enums.PPSStatus;
import org.dcm4che.ris.core.exception.ResourceNotFoundException;
import org.dcm4che.ris.core.service.SchedulingService;
import org.dcm4che.ris.dicom.config.DicomConfiguration;
import org.dcm4che.ris.persistence.repository.PerformedProcedureStepRepository;
import org.dcm4che.ris.persistence.repository.ScheduledProcedureStepRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DICOM Modality Performed Procedure Step (MPPS) N-CREATE and N-SET SCP.
 * Receives MPPS notifications from modalities when procedures are performed.
 *
 * Based on DICOM PS 3.4 F.7 Modality Performed Procedure Step SOP Class.
 *
 * @author dcm4che-ris
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MPPSServiceSCP extends BasicMPPSSCP {

    private final ScheduledProcedureStepRepository spsRepository;
    private final PerformedProcedureStepRepository ppsRepository;
    private final SchedulingService schedulingService;
    private final DicomConfiguration dicomConfiguration;

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    protected Attributes create(Association as, Attributes rq, Attributes rqAttrs, Attributes rsp)
            throws DicomServiceException {

        String sopInstanceUID = rq.getString(Tag.AffectedSOPInstanceUID);
        log.info("Received MPPS N-CREATE from {}, SOP Instance UID: {}",
                as.getCallingAET(), sopInstanceUID);

        // Log MPPS if enabled
        if (dicomConfiguration.isLogDicomMessages()) {
            log.debug("MPPS N-CREATE Attributes:\n{}", rqAttrs);
        }

        try {
            // Extract MPPS attributes
            String performedProcedureStepID = rqAttrs.getString(Tag.PerformedProcedureStepID);
            String ppsStatus = rqAttrs.getString(Tag.PerformedProcedureStepStatus);
            String ppsStartDate = rqAttrs.getString(Tag.PerformedProcedureStepStartDate);
            String ppsStartTime = rqAttrs.getString(Tag.PerformedProcedureStepStartTime);
            String stationAETitle = rqAttrs.getString(Tag.PerformedStationAETitle);
            String modality = rqAttrs.getString(Tag.Modality);

            // Extract Scheduled Step Attributes Sequence
            Attributes ssaSeq = rqAttrs.getNestedDataset(Tag.ScheduledStepAttributesSequence);
            if (ssaSeq == null) {
                throw new DicomServiceException(Status.MissingAttributeValue,
                        "Scheduled Step Attributes Sequence is required");
            }

            String spsIdCode = ssaSeq.getString(Tag.ScheduledProcedureStepID);
            String accessionNumber = ssaSeq.getString(Tag.AccessionNumber);
            String studyInstanceUID = ssaSeq.getString(Tag.StudyInstanceUID);

            log.info("MPPS N-CREATE: PPS ID={}, Status={}, SPS ID={}, Study UID={}, Accession={}",
                    performedProcedureStepID, ppsStatus, spsIdCode, studyInstanceUID, accessionNumber);

            // Find scheduled procedure step
            ScheduledProcedureStep sps = spsRepository.findBySpsIdCode(spsIdCode)
                    .orElseThrow(() -> new DicomServiceException(Status.NoSuchObjectInstance,
                            "Scheduled Procedure Step not found: " + spsIdCode));

            // Parse start date/time
            LocalDateTime startDateTime = null;
            if (ppsStartDate != null && ppsStartTime != null) {
                startDateTime = LocalDateTime.parse(ppsStartDate + ppsStartTime, DATETIME_FORMATTER);
            }

            // Create PPS entity
            PerformedProcedureStep pps = PerformedProcedureStep.builder()
                    .scheduledProcedureStep(sps)
                    .sopInstanceUID(sopInstanceUID)
                    .ppsID(performedProcedureStepID)
                    .ppsStatus(PPSStatus.valueOf(ppsStatus))
                    .ppsStartDateTime(startDateTime)
                    .performedStationAETitle(stationAETitle)
                    .performedModality(modality)
                    .build();

            // Save PPS
            ppsRepository.save(pps);

            // Update SPS status to STARTED
            if ("IN PROGRESS".equals(ppsStatus)) {
                sps.transitionTo(org.dcm4che.ris.api.enums.SPSStatus.STARTED, null);
                spsRepository.save(sps);
            }

            log.info("MPPS created successfully: {}", sopInstanceUID);

            // Return empty response (success)
            return new Attributes();

        } catch (DicomServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing MPPS N-CREATE", e);
            throw new DicomServiceException(Status.ProcessingFailure, e);
        }
    }

    @Override
    protected Attributes set(Association as, Attributes rq, Attributes rqAttrs, Attributes rsp)
            throws DicomServiceException {

        String sopInstanceUID = rq.getString(Tag.RequestedSOPInstanceUID);
        log.info("Received MPPS N-SET from {}, SOP Instance UID: {}",
                as.getCallingAET(), sopInstanceUID);

        // Log MPPS if enabled
        if (dicomConfiguration.isLogDicomMessages()) {
            log.debug("MPPS N-SET Attributes:\n{}", rqAttrs);
        }

        try {
            // Find existing PPS
            PerformedProcedureStep pps = ppsRepository.findBySopInstanceUID(sopInstanceUID)
                    .orElseThrow(() -> new DicomServiceException(Status.NoSuchObjectInstance,
                            "Performed Procedure Step not found: " + sopInstanceUID));

            // Extract updated attributes
            String ppsStatus = rqAttrs.getString(Tag.PerformedProcedureStepStatus);
            String ppsEndDate = rqAttrs.getString(Tag.PerformedProcedureStepEndDate);
            String ppsEndTime = rqAttrs.getString(Tag.PerformedProcedureStepEndTime);

            log.info("MPPS N-SET: PPS UID={}, New Status={}", sopInstanceUID, ppsStatus);

            // Update PPS status
            if (ppsStatus != null) {
                pps.setPpsStatus(PPSStatus.valueOf(ppsStatus));
            }

            // Parse end date/time
            if (ppsEndDate != null && ppsEndTime != null) {
                LocalDateTime endDateTime = LocalDateTime.parse(ppsEndDate + ppsEndTime, DATETIME_FORMATTER);
                pps.setPpsEndDateTime(endDateTime);
            }

            // Save updated PPS
            ppsRepository.save(pps);

            // Update SPS status based on PPS status
            ScheduledProcedureStep sps = pps.getScheduledProcedureStep();
            if ("COMPLETED".equals(ppsStatus)) {
                sps.transitionTo(org.dcm4che.ris.api.enums.SPSStatus.COMPLETED, null);
                spsRepository.save(sps);
            } else if ("DISCONTINUED".equals(ppsStatus)) {
                sps.transitionTo(org.dcm4che.ris.api.enums.SPSStatus.DISCONTINUED, null);
                spsRepository.save(sps);
            }

            log.info("MPPS updated successfully: {}", sopInstanceUID);

            // Return empty response (success)
            return new Attributes();

        } catch (DicomServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing MPPS N-SET", e);
            throw new DicomServiceException(Status.ProcessingFailure, e);
        }
    }
}
