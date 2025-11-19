package org.dcm4che.ris.dicom.scp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4che.net.*;
import org.dcm4che.net.pdu.PresentationContext;
import org.dcm4che.net.service.BasicCFindSCP;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4che.net.service.QueryRetrieveLevel2;
import org.dcm4che.ris.api.entity.ScheduledProcedureStep;
import org.dcm4che.ris.core.service.WorklistService;
import org.dcm4che.ris.dicom.config.DicomConfiguration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * DICOM Modality Worklist C-FIND SCP implementation.
 * Responds to C-FIND requests from modalities with scheduled procedure steps.
 *
 * Based on DICOM PS 3.4 K.6 Modality Worklist Information Model.
 *
 * @author dcm4che-ris
 */
@Slf4j
@Component
public class MWLQuerySCP extends BasicCFindSCP {

    private final WorklistService worklistService;
    private final DicomConfiguration dicomConfiguration;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    /**
     * Constructor
     */
    public MWLQuerySCP(WorklistService worklistService, DicomConfiguration dicomConfiguration) {
        super(UID.ModalityWorklistInformationModelFind);
        this.worklistService = worklistService;
        this.dicomConfiguration = dicomConfiguration;
    }

    @Override
    protected QueryTask calculateMatches(Association as, PresentationContext pc, Attributes rq, Attributes keys)
            throws DicomServiceException {

        log.info("Received MWL C-FIND request from {}", as.getCallingAET());

        // Log request keys if enabled
        if (dicomConfiguration.isLogDicomMessages()) {
            log.debug("MWL Query Keys:\n{}", keys);
        }

        // Extract query parameters from keys
        String scheduledStationAETitle = keys.getString(Tag.ScheduledStationAETitle);
        String modality = keys.getString(Tag.Modality);
        String scheduledStartDate = keys.getString(Tag.ScheduledProcedureStepStartDate);

        // Validate request
        if (scheduledStationAETitle == null && modality == null) {
            throw new DicomServiceException(Status.IdentifierDoesNotMatchSOPClass,
                    "Scheduled Station AE Title or Modality is required");
        }

        // Parse date range
        LocalDateTime startDate;
        LocalDateTime endDate;

        if (scheduledStartDate != null && !scheduledStartDate.isEmpty()) {
            // Use specified date
            try {
                startDate = LocalDateTime.parse(scheduledStartDate + "000000",
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                endDate = startDate.plusDays(1);
            } catch (Exception e) {
                throw new DicomServiceException(Status.InvalidAttributeValue,
                        "Invalid Scheduled Procedure Step Start Date: " + scheduledStartDate);
            }
        } else {
            // Use default range from configuration
            startDate = LocalDateTime.now().minusDays(1); // Yesterday
            endDate = LocalDateTime.now().plusDays(dicomConfiguration.getMwlDateRangeDays());
        }

        log.info("MWL Query: AET={}, Modality={}, DateRange={} to {}",
                scheduledStationAETitle, modality, startDate, endDate);

        // Query worklist
        List<ScheduledProcedureStep> spsList;
        try {
            if (modality != null && !modality.isEmpty()) {
                spsList = worklistService.getMWLByModality(modality, startDate, endDate);
            } else if (scheduledStationAETitle != null && !scheduledStationAETitle.isEmpty()) {
                spsList = worklistService.getMWLByAETitle(scheduledStationAETitle);
            } else {
                // Fallback: get all scheduled SPS
                spsList = worklistService.getScheduledSPS(startDate, endDate);
            }

            // Limit results
            if (spsList.size() > dicomConfiguration.getMwlMaxResults()) {
                log.warn("MWL Query returned {} results, limiting to {}",
                        spsList.size(), dicomConfiguration.getMwlMaxResults());
                spsList = spsList.subList(0, dicomConfiguration.getMwlMaxResults());
            }

            log.info("MWL Query returned {} results", spsList.size());

        } catch (Exception e) {
            log.error("Error querying worklist", e);
            throw new DicomServiceException(Status.ProcessingFailure, e);
        }

        // Create query task
        return new MWLQueryTask(as, pc, rq, keys, spsList);
    }

    /**
     * Query task for MWL
     */
    private class MWLQueryTask implements QueryTask {
        private final Association as;
        private final PresentationContext pc;
        private final Attributes rq;
        private final Attributes keys;
        private final List<ScheduledProcedureStep> spsList;
        private int index = 0;

        public MWLQueryTask(Association as, PresentationContext pc, Attributes rq, Attributes keys,
                           List<ScheduledProcedureStep> spsList) {
            this.as = as;
            this.pc = pc;
            this.rq = rq;
            this.keys = keys;
            this.spsList = spsList;
        }

        @Override
        public void onCancelRQ(Association as) {
            log.info("MWL Query canceled by {}", as.getCallingAET());
        }

        @Override
        public boolean hasMoreMatches() {
            return index < spsList.size();
        }

        @Override
        public Attributes nextMatch() throws DicomServiceException {
            if (!hasMoreMatches()) {
                return null;
            }

            ScheduledProcedureStep sps = spsList.get(index++);

            try {
                // Convert SPS to DICOM Attributes using WorklistService
                Attributes match = worklistService.spsToMWLAttributes(sps);

                // Log response if enabled
                if (dicomConfiguration.isLogDicomMessages()) {
                    log.debug("MWL Match {}:\n{}", index, match);
                }

                return match;

            } catch (Exception e) {
                log.error("Error converting SPS to MWL attributes: {}", sps.getSpsId(), e);
                throw new DicomServiceException(Status.ProcessingFailure, e);
            }
        }

        @Override
        public void close() {
            // Cleanup if needed
            log.debug("MWL Query task closed, returned {} of {} results",
                     index, spsList.size());
        }
    }
}
