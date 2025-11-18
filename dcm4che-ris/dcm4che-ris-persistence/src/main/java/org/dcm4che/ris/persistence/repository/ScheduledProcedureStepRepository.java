package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.ScheduledProcedureStep;
import org.dcm4che.ris.api.enums.SPSStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ScheduledProcedureStep entity.
 * Core repository for DICOM Modality Worklist (MWL) functionality.
 *
 * @author dcm4che-ris
 */
@Repository
public interface ScheduledProcedureStepRepository extends JpaRepository<ScheduledProcedureStep, Long>,
                                                          JpaSpecificationExecutor<ScheduledProcedureStep> {

    /**
     * Find SPS by Scheduled Procedure Step ID (database ID)
     */
    Optional<ScheduledProcedureStep> findBySpsId(Long spsId);

    /**
     * Find SPS by Scheduled Procedure Step ID Code (DICOM attribute 0040,0009)
     */
    Optional<ScheduledProcedureStep> findBySpsIdCode(String spsIdCode);

    /**
     * Find SPS by status
     */
    List<ScheduledProcedureStep> findBySpsStatus(SPSStatus spsStatus);

    /**
     * Find scheduled SPS (for MWL)
     * Returns all SPS with SCHEDULED status for a specific date range
     */
    @Query("SELECT sps FROM ScheduledProcedureStep sps WHERE sps.spsStatus = 'SCHEDULED' " +
           "AND sps.scheduledStartDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY sps.scheduledStartDateTime ASC")
    List<ScheduledProcedureStep> findScheduledSPS(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Find SPS by modality and date range (for MWL C-FIND)
     * This is the primary query for DICOM Modality Worklist
     */
    @Query("SELECT sps FROM ScheduledProcedureStep sps " +
           "WHERE sps.modality = :modality " +
           "AND sps.spsStatus = 'SCHEDULED' " +
           "AND sps.scheduledStartDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY sps.scheduledStartDateTime ASC")
    List<ScheduledProcedureStep> findMWLByModalityAndDate(@Param("modality") String modality,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Find SPS by AE Title (for specific modality device)
     */
    @Query("SELECT sps FROM ScheduledProcedureStep sps " +
           "WHERE sps.scheduledStationAETitle = :aeTitle " +
           "AND sps.spsStatus IN ('SCHEDULED', 'ARRIVED', 'READY') " +
           "ORDER BY sps.scheduledStartDateTime ASC")
    List<ScheduledProcedureStep> findByScheduledStationAETitle(@Param("aeTitle") String aeTitle);

    /**
     * Find SPS for a patient
     */
    @Query("SELECT sps FROM ScheduledProcedureStep sps " +
           "WHERE sps.requestedProcedure.order.patient.patientId = :patientId " +
           "ORDER BY sps.scheduledStartDateTime DESC")
    List<ScheduledProcedureStep> findByPatient(@Param("patientId") Long patientId);

    /**
     * Find today's schedule for a modality
     */
    @Query("SELECT sps FROM ScheduledProcedureStep sps " +
           "WHERE sps.modality = :modality " +
           "AND DATE(sps.scheduledStartDateTime) = CURRENT_DATE " +
           "AND sps.spsStatus IN ('SCHEDULED', 'ARRIVED', 'READY', 'STARTED') " +
           "ORDER BY sps.scheduledStartDateTime ASC")
    List<ScheduledProcedureStep> findTodayScheduleByModality(@Param("modality") String modality);

    /**
     * Find arrived patients (checked in but not started)
     */
    @Query("SELECT sps FROM ScheduledProcedureStep sps " +
           "WHERE sps.spsStatus = 'ARRIVED' " +
           "ORDER BY sps.scheduledStartDateTime ASC")
    List<ScheduledProcedureStep> findArrivedPatients();

    /**
     * Find started procedures (in progress)
     */
    @Query("SELECT sps FROM ScheduledProcedureStep sps " +
           "WHERE sps.spsStatus = 'STARTED' " +
           "ORDER BY sps.actualStartDateTime ASC")
    List<ScheduledProcedureStep> findStartedProcedures();

    /**
     * Find overdue procedures (scheduled time passed but not started)
     */
    @Query("SELECT sps FROM ScheduledProcedureStep sps " +
           "WHERE sps.spsStatus IN ('SCHEDULED', 'ARRIVED', 'READY') " +
           "AND sps.scheduledStartDateTime < :currentTime " +
           "ORDER BY sps.scheduledStartDateTime ASC")
    List<ScheduledProcedureStep> findOverdueProcedures(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find procedures by performing physician
     */
    @Query("SELECT sps FROM ScheduledProcedureStep sps " +
           "WHERE sps.scheduledPerformingPhysician.physicianId = :physicianId " +
           "ORDER BY sps.scheduledStartDateTime DESC")
    List<ScheduledProcedureStep> findByPerformingPhysician(@Param("physicianId") Long physicianId);

    /**
     * Find procedures scheduled in date range
     */
    @Query("SELECT sps FROM ScheduledProcedureStep sps " +
           "WHERE sps.scheduledStartDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY sps.scheduledStartDateTime ASC")
    List<ScheduledProcedureStep> findScheduledBetween(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find procedures completed in date range
     */
    @Query("SELECT sps FROM ScheduledProcedureStep sps " +
           "WHERE sps.spsStatus = 'COMPLETED' " +
           "AND sps.actualEndDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY sps.actualEndDateTime DESC")
    List<ScheduledProcedureStep> findCompletedBetween(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Count procedures by status
     */
    long countBySpsStatus(SPSStatus spsStatus);

    /**
     * Count today's scheduled procedures by modality
     */
    @Query("SELECT COUNT(sps) FROM ScheduledProcedureStep sps " +
           "WHERE sps.modality = :modality " +
           "AND DATE(sps.scheduledStartDateTime) = CURRENT_DATE")
    long countTodayByModality(@Param("modality") String modality);

    /**
     * Get average procedure duration by modality
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, sps.actualStartDateTime, sps.actualEndDateTime)) " +
           "FROM ScheduledProcedureStep sps " +
           "WHERE sps.modality = :modality " +
           "AND sps.spsStatus = 'COMPLETED' " +
           "AND sps.actualStartDateTime IS NOT NULL " +
           "AND sps.actualEndDateTime IS NOT NULL")
    Double getAverageDurationByModality(@Param("modality") String modality);

    /**
     * Find SPS requiring contrast
     */
    @Query("SELECT sps FROM ScheduledProcedureStep sps " +
           "WHERE sps.requestedContrastAgent IS NOT NULL " +
           "AND sps.spsStatus IN ('SCHEDULED', 'ARRIVED', 'READY') " +
           "ORDER BY sps.scheduledStartDateTime ASC")
    List<ScheduledProcedureStep> findProceduresRequiringContrast();
}
