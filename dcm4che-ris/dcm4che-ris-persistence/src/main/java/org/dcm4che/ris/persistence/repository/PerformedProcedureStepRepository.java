package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.PerformedProcedureStep;
import org.dcm4che.ris.api.enums.PPSStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for PerformedProcedureStep entity.
 * Manages DICOM MPPS (Modality Performed Procedure Step) data.
 *
 * @author dcm4che-ris
 */
@Repository
public interface PerformedProcedureStepRepository extends JpaRepository<PerformedProcedureStep, Long>,
                                                          JpaSpecificationExecutor<PerformedProcedureStep> {

    /**
     * Find PPS by SOP Instance UID (from MPPS N-CREATE)
     */
    Optional<PerformedProcedureStep> findBySopInstanceUID(String sopInstanceUID);

    /**
     * Check if SOP Instance UID exists
     */
    boolean existsBySopInstanceUID(String sopInstanceUID);

    /**
     * Find PPS by scheduled procedure step
     */
    @Query("SELECT pps FROM PerformedProcedureStep pps WHERE pps.scheduledProcedureStep.spsId = :spsId")
    Optional<PerformedProcedureStep> findByScheduledProcedureStepSpsId(@Param("spsId") Long spsId);

    /**
     * Find PPS by status
     */
    List<PerformedProcedureStep> findByPpsStatus(PPSStatus ppsStatus);

    /**
     * Find in-progress PPS
     */
    @Query("SELECT pps FROM PerformedProcedureStep pps WHERE pps.ppsStatus = 'IN_PROGRESS' ORDER BY pps.ppsStartDateTime ASC")
    List<PerformedProcedureStep> findInProgressPPS();

    /**
     * Find completed PPS in date range
     */
    @Query("SELECT pps FROM PerformedProcedureStep pps WHERE pps.ppsStatus = 'COMPLETED' " +
           "AND pps.ppsEndDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY pps.ppsEndDateTime DESC")
    List<PerformedProcedureStep> findCompletedBetween(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find PPS by performing physician
     */
    @Query("SELECT pps FROM PerformedProcedureStep pps WHERE pps.performingPhysician.physicianId = :physicianId " +
           "ORDER BY pps.ppsStartDateTime DESC")
    List<PerformedProcedureStep> findByPerformingPhysician(@Param("physicianId") Long physicianId);

    /**
     * Find PPS by performed station AE Title
     */
    List<PerformedProcedureStep> findByPerformedStationAETitle(String performedStationAETitle);

    /**
     * Find PPS for a patient
     */
    @Query("SELECT pps FROM PerformedProcedureStep pps " +
           "WHERE pps.scheduledProcedureStep.requestedProcedure.order.patient.patientId = :patientId " +
           "ORDER BY pps.ppsStartDateTime DESC")
    List<PerformedProcedureStep> findByPatient(@Param("patientId") Long patientId);

    /**
     * Count PPS by status
     */
    long countByPpsStatus(PPSStatus ppsStatus);

    /**
     * Get average procedure duration
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, pps.ppsStartDateTime, pps.ppsEndDateTime)) " +
           "FROM PerformedProcedureStep pps " +
           "WHERE pps.ppsStatus = 'COMPLETED' " +
           "AND pps.ppsStartDateTime IS NOT NULL " +
           "AND pps.ppsEndDateTime IS NOT NULL")
    Double getAverageProcedureDuration();

    /**
     * Get total radiation dose for a patient
     */
    @Query("SELECT SUM(pps.doseAreaProduct) FROM PerformedProcedureStep pps " +
           "WHERE pps.scheduledProcedureStep.requestedProcedure.order.patient.patientId = :patientId " +
           "AND pps.doseAreaProduct IS NOT NULL")
    Double getTotalDoseForPatient(@Param("patientId") Long patientId);
}
