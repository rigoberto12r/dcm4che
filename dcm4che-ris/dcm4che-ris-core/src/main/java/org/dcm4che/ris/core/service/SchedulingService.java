package org.dcm4che.ris.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.ris.api.entity.*;
import org.dcm4che.ris.api.enums.SPSStatus;
import org.dcm4che.ris.core.exception.InvalidStatusTransitionException;
import org.dcm4che.ris.core.exception.ResourceNotFoundException;
import org.dcm4che.ris.persistence.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for scheduling procedures and managing the worklist.
 * Creates and manages Scheduled Procedure Steps (SPS).
 *
 * @author dcm4che-ris
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SchedulingService {

    private final ScheduledProcedureStepRepository spsRepository;
    private final RequestedProcedureRepository requestedProcedureRepository;
    private final ModalityRepository modalityRepository;
    private final PhysicianRepository physicianRepository;
    private final UserRepository userRepository;

    /**
     * Schedule a procedure step.
     * Creates a new SPS for a requested procedure.
     */
    public ScheduledProcedureStep scheduleProcedure(ScheduledProcedureStep sps) {
        log.info("Scheduling procedure for requested procedure ID: {}",
                 sps.getRequestedProcedure().getRequestedProcedureId());

        // Validate requested procedure exists
        if (sps.getRequestedProcedure() == null || sps.getRequestedProcedure().getRequestedProcedureId() == null) {
            throw new IllegalArgumentException("Requested procedure is required");
        }

        RequestedProcedure requestedProcedure = requestedProcedureRepository
                .findById(sps.getRequestedProcedure().getRequestedProcedureId())
                .orElseThrow(() -> new ResourceNotFoundException("RequestedProcedure",
                        sps.getRequestedProcedure().getRequestedProcedureId()));
        sps.setRequestedProcedure(requestedProcedure);

        // Validate performing physician if provided
        if (sps.getScheduledPerformingPhysician() != null && sps.getScheduledPerformingPhysician().getPhysicianId() != null) {
            Physician physician = physicianRepository.findById(sps.getScheduledPerformingPhysician().getPhysicianId())
                    .orElseThrow(() -> new ResourceNotFoundException("Physician",
                            sps.getScheduledPerformingPhysician().getPhysicianId()));
            sps.setScheduledPerformingPhysician(physician);
        }

        // Generate SPS ID if not provided
        if (sps.getScheduledProcedureStepId() == null) {
            sps.setScheduledProcedureStepId(generateSpsId());
        }

        // Set initial status
        if (sps.getSpsStatus() == null) {
            sps.setSpsStatus(SPSStatus.SCHEDULED);
        }

        // Validate scheduled time
        if (sps.getScheduledStartDateTime() == null) {
            throw new IllegalArgumentException("Scheduled start date/time is required");
        }

        // Save SPS
        ScheduledProcedureStep savedSps = spsRepository.save(sps);
        log.info("Procedure scheduled successfully with SPS ID: {}", savedSps.getSpsId());

        return savedSps;
    }

    /**
     * Reschedule a procedure to a different date/time.
     */
    public ScheduledProcedureStep rescheduleProcedure(Long spsId, LocalDateTime newDateTime) {
        log.info("Rescheduling SPS {} to: {}", spsId, newDateTime);

        ScheduledProcedureStep sps = getSpsById(spsId);

        // Only allow rescheduling for SCHEDULED procedures
        if (sps.getSpsStatus() != SPSStatus.SCHEDULED) {
            throw new InvalidStatusTransitionException(
                    "Cannot reschedule procedure in status: " + sps.getSpsStatus(),
                    sps.getSpsStatus().name(),
                    "RESCHEDULED");
        }

        sps.setScheduledStartDateTime(newDateTime);
        ScheduledProcedureStep savedSps = spsRepository.save(sps);

        log.info("Procedure rescheduled successfully: {}", spsId);
        return savedSps;
    }

    /**
     * Mark patient as arrived for procedure.
     */
    public ScheduledProcedureStep markPatientArrived(Long spsId, User user) {
        log.info("Marking patient arrived for SPS: {}", spsId);

        ScheduledProcedureStep sps = getSpsById(spsId);
        sps.markArrived(user);

        ScheduledProcedureStep savedSps = spsRepository.save(sps);
        log.info("Patient marked as arrived for SPS: {}", spsId);
        return savedSps;
    }

    /**
     * Start a procedure.
     */
    public ScheduledProcedureStep startProcedure(Long spsId, User user) {
        log.info("Starting procedure for SPS: {}", spsId);

        ScheduledProcedureStep sps = getSpsById(spsId);
        sps.start(user);

        ScheduledProcedureStep savedSps = spsRepository.save(sps);
        log.info("Procedure started for SPS: {}", spsId);
        return savedSps;
    }

    /**
     * Complete a procedure.
     */
    public ScheduledProcedureStep completeProcedure(Long spsId, User user) {
        log.info("Completing procedure for SPS: {}", spsId);

        ScheduledProcedureStep sps = getSpsById(spsId);
        sps.complete(user);

        ScheduledProcedureStep savedSps = spsRepository.save(sps);
        log.info("Procedure completed for SPS: {}", spsId);
        return savedSps;
    }

    /**
     * Cancel a scheduled procedure.
     */
    public ScheduledProcedureStep cancelProcedure(Long spsId, User user, String reason) {
        log.info("Canceling procedure for SPS: {}", spsId);

        ScheduledProcedureStep sps = getSpsById(spsId);
        sps.cancel(user);

        if (reason != null) {
            sps.setComments(sps.getComments() != null ?
                    sps.getComments() + "\nCancellation reason: " + reason :
                    "Cancellation reason: " + reason);
        }

        ScheduledProcedureStep savedSps = spsRepository.save(sps);
        log.info("Procedure canceled for SPS: {}", spsId);
        return savedSps;
    }

    /**
     * Get SPS by ID.
     */
    @Transactional(readOnly = true)
    public ScheduledProcedureStep getSpsById(Long spsId) {
        return spsRepository.findById(spsId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledProcedureStep", spsId));
    }

    /**
     * Get SPS by Scheduled Procedure Step ID (DICOM identifier).
     */
    @Transactional(readOnly = true)
    public ScheduledProcedureStep getSpsBySpsId(String spsId) {
        return spsRepository.findBySpsId(spsId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledProcedureStep with SPS ID: " + spsId));
    }

    /**
     * Get today's schedule for a modality.
     */
    @Transactional(readOnly = true)
    public List<ScheduledProcedureStep> getTodayScheduleByModality(String modality) {
        return spsRepository.findTodayScheduleByModality(modality);
    }

    /**
     * Get scheduled procedures for date range and modality.
     * This is used for Modality Worklist (MWL) queries.
     */
    @Transactional(readOnly = true)
    public List<ScheduledProcedureStep> getWorklistForModality(String modality,
                                                                LocalDateTime startDate,
                                                                LocalDateTime endDate) {
        log.debug("Fetching worklist for modality: {} from {} to {}", modality, startDate, endDate);
        return spsRepository.findMWLByModalityAndDate(modality, startDate, endDate);
    }

    /**
     * Get scheduled procedures by status.
     */
    @Transactional(readOnly = true)
    public List<ScheduledProcedureStep> getProceduresByStatus(SPSStatus status) {
        return spsRepository.findBySpsStatus(status);
    }

    /**
     * Get arrived patients (checked in but not started).
     */
    @Transactional(readOnly = true)
    public List<ScheduledProcedureStep> getArrivedPatients() {
        return spsRepository.findArrivedPatients();
    }

    /**
     * Get procedures in progress.
     */
    @Transactional(readOnly = true)
    public List<ScheduledProcedureStep> getProceduresInProgress() {
        return spsRepository.findStartedProcedures();
    }

    /**
     * Get overdue procedures.
     */
    @Transactional(readOnly = true)
    public List<ScheduledProcedureStep> getOverdueProcedures() {
        return spsRepository.findOverdueProcedures(LocalDateTime.now());
    }

    /**
     * Get procedures by AE Title.
     */
    @Transactional(readOnly = true)
    public List<ScheduledProcedureStep> getProceduresByAeTitle(String aeTitle) {
        return spsRepository.findByScheduledStationAETitle(aeTitle);
    }

    /**
     * Get procedures for a patient.
     */
    @Transactional(readOnly = true)
    public List<ScheduledProcedureStep> getProceduresByPatient(Long patientId) {
        return spsRepository.findByPatient(patientId);
    }

    /**
     * Get procedures by performing physician.
     */
    @Transactional(readOnly = true)
    public List<ScheduledProcedureStep> getProceduresByPhysician(Long physicianId) {
        return spsRepository.findByPerformingPhysician(physicianId);
    }

    /**
     * Get scheduled procedures in date range.
     */
    @Transactional(readOnly = true)
    public List<ScheduledProcedureStep> getProceduresScheduledBetween(LocalDateTime startDate,
                                                                       LocalDateTime endDate) {
        return spsRepository.findScheduledBetween(startDate, endDate);
    }

    /**
     * Count procedures by status.
     */
    @Transactional(readOnly = true)
    public long countProceduresByStatus(SPSStatus status) {
        return spsRepository.countBySpsStatus(status);
    }

    /**
     * Count today's scheduled procedures by modality.
     */
    @Transactional(readOnly = true)
    public long countTodayByModality(String modality) {
        return spsRepository.countTodayByModality(modality);
    }

    /**
     * Get average procedure duration by modality.
     */
    @Transactional(readOnly = true)
    public Double getAverageDurationByModality(String modality) {
        return spsRepository.getAverageDurationByModality(modality);
    }

    // Helper methods

    /**
     * Generate a unique SPS ID.
     */
    private String generateSpsId() {
        return "SPS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
