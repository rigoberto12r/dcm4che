package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Visit entity.
 * Manages patient visits/admissions/encounters.
 *
 * @author dcm4che-ris
 */
@Repository
public interface VisitRepository extends JpaRepository<Visit, Long>, JpaSpecificationExecutor<Visit> {

    /**
     * Find visit by admission ID
     */
    Optional<Visit> findByAdmissionId(String admissionId);

    /**
     * Check if admission ID exists
     */
    boolean existsByAdmissionId(String admissionId);

    /**
     * Find all active visits for a patient
     */
    @Query("SELECT v FROM Visit v WHERE v.patient.patientId = :patientId AND v.visitStatus = 'ACTIVE' ORDER BY v.admissionDate DESC")
    List<Visit> findActiveVisitsByPatient(@Param("patientId") Long patientId);

    /**
     * Find all visits for a patient
     */
    @Query("SELECT v FROM Visit v WHERE v.patient.patientId = :patientId ORDER BY v.admissionDate DESC")
    List<Visit> findByPatientPatientId(Long patientId);

    /**
     * Find visits by status
     */
    List<Visit> findByVisitStatus(String visitStatus);

    /**
     * Find visits by type
     */
    List<Visit> findByVisitType(String visitType);

    /**
     * Find active inpatient visits
     */
    @Query("SELECT v FROM Visit v WHERE v.visitType = 'INPATIENT' AND v.visitStatus = 'ACTIVE'")
    List<Visit> findActiveInpatients();

    /**
     * Find active emergency visits
     */
    @Query("SELECT v FROM Visit v WHERE v.visitType = 'EMERGENCY' AND v.visitStatus = 'ACTIVE'")
    List<Visit> findActiveEmergencyVisits();

    /**
     * Find visits by attending physician
     */
    @Query("SELECT v FROM Visit v WHERE v.attendingPhysician.physicianId = :physicianId AND v.visitStatus = 'ACTIVE'")
    List<Visit> findActiveVisitsByAttendingPhysician(@Param("physicianId") Long physicianId);

    /**
     * Find visits by ward/location
     */
    List<Visit> findByWardAndVisitStatus(String ward, String visitStatus);

    /**
     * Find visits admitted in date range
     */
    @Query("SELECT v FROM Visit v WHERE v.admissionDate BETWEEN :startDate AND :endDate ORDER BY v.admissionDate DESC")
    List<Visit> findVisitsAdmittedBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Find visits discharged in date range
     */
    @Query("SELECT v FROM Visit v WHERE v.dischargeDate BETWEEN :startDate AND :endDate ORDER BY v.dischargeDate DESC")
    List<Visit> findVisitsDischargedBetween(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Find visits by hospital service/department
     */
    List<Visit> findByHospitalServiceAndVisitStatus(String hospitalService, String visitStatus);

    /**
     * Find VIP visits
     */
    @Query("SELECT v FROM Visit v WHERE v.isVip = true AND v.visitStatus = 'ACTIVE'")
    List<Visit> findActiveVipVisits();

    /**
     * Find readmission visits
     */
    @Query("SELECT v FROM Visit v WHERE v.isReadmission = true ORDER BY v.admissionDate DESC")
    List<Visit> findReadmissions();

    /**
     * Count active visits by type
     */
    long countByVisitTypeAndVisitStatus(String visitType, String visitStatus);

    /**
     * Get average length of stay for discharged visits in date range
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(DAY, v.admissionDate, v.dischargeDate)) " +
           "FROM Visit v WHERE v.visitStatus = 'DISCHARGED' " +
           "AND v.dischargeDate BETWEEN :startDate AND :endDate")
    Double getAverageLengthOfStay(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
}
