package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Study entity.
 * Manages DICOM studies received from modalities/PACS.
 *
 * @author dcm4che-ris
 */
@Repository
public interface StudyRepository extends JpaRepository<Study, Long>, JpaSpecificationExecutor<Study> {

    /**
     * Find study by Study Instance UID (primary DICOM identifier)
     */
    Optional<Study> findByStudyInstanceUID(String studyInstanceUID);

    /**
     * Check if Study Instance UID exists
     */
    boolean existsByStudyInstanceUID(String studyInstanceUID);

    /**
     * Find study by Accession Number
     */
    Optional<Study> findByAccessionNumber(String accessionNumber);

    /**
     * Find studies by patient
     */
    @Query("SELECT s FROM Study s WHERE s.patient.patientId = :patientId ORDER BY s.studyDate DESC, s.studyTime DESC")
    List<Study> findByPatientPatientId(@Param("patientId") Long patientId);

    /**
     * Find studies by study date
     */
    List<Study> findByStudyDate(LocalDate studyDate);

    /**
     * Find studies by date range
     */
    @Query("SELECT s FROM Study s WHERE s.studyDate BETWEEN :startDate AND :endDate ORDER BY s.studyDate DESC, s.studyTime DESC")
    List<Study> findByStudyDateBetween(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /**
     * Find studies by modality
     */
    @Query("SELECT s FROM Study s WHERE s.modalitiesInStudy LIKE %:modality% ORDER BY s.studyDate DESC")
    List<Study> findByModality(@Param("modality") String modality);

    /**
     * Find studies by status
     */
    List<Study> findByStudyStatus(String studyStatus);

    /**
     * Find unreported studies (no final report)
     */
    @Query("SELECT s FROM Study s WHERE s.studyStatus = 'RECEIVED' " +
           "AND NOT EXISTS (SELECT r FROM Report r WHERE r.study.studyId = s.studyId " +
           "AND r.reportStatus IN ('FINAL', 'AMENDED', 'CORRECTED')) " +
           "ORDER BY s.studyDate DESC")
    List<Study> findUnreportedStudies();

    /**
     * Find studies with preliminary reports
     */
    @Query("SELECT DISTINCT s FROM Study s JOIN s.reports r " +
           "WHERE r.reportStatus = 'PRELIMINARY' " +
           "ORDER BY s.studyDate DESC")
    List<Study> findStudiesWithPreliminaryReports();

    /**
     * Find studies by reading physician
     */
    @Query("SELECT DISTINCT s FROM Study s JOIN s.reports r " +
           "WHERE r.verifiedByUser.userId = :userId " +
           "ORDER BY s.studyDate DESC")
    List<Study> findByReadingPhysician(@Param("userId") Long userId);

    /**
     * Find studies by referring physician
     */
    @Query("SELECT s FROM Study s WHERE s.referringPhysician.physicianId = :physicianId " +
           "ORDER BY s.studyDate DESC")
    List<Study> findByReferringPhysician(@Param("physicianId") Long physicianId);

    /**
     * Find studies received in date range
     */
    @Query("SELECT s FROM Study s WHERE s.studyReceivedDate BETWEEN :startDate AND :endDate " +
           "ORDER BY s.studyReceivedDate DESC")
    List<Study> findStudiesReceivedBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Search studies by patient name (case-insensitive)
     */
    @Query("SELECT s FROM Study s WHERE LOWER(s.patient.patientName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "ORDER BY s.studyDate DESC")
    List<Study> searchByPatientName(@Param("name") String name);

    /**
     * Search studies by patient MRN
     */
    @Query("SELECT s FROM Study s WHERE s.patient.mrn = :mrn ORDER BY s.studyDate DESC")
    List<Study> searchByPatientMRN(@Param("mrn") String mrn);

    /**
     * Find studies by description (exam type)
     */
    @Query("SELECT s FROM Study s WHERE LOWER(s.studyDescription) LIKE LOWER(CONCAT('%', :description, '%')) " +
           "ORDER BY s.studyDate DESC")
    List<Study> searchByDescription(@Param("description") String description);

    /**
     * Find stat/urgent studies not yet reported
     */
    @Query("SELECT s FROM Study s WHERE s.studyPriority IN ('URGENT', 'STAT') " +
           "AND s.studyStatus = 'RECEIVED' " +
           "AND NOT EXISTS (SELECT r FROM Report r WHERE r.study.studyId = s.studyId " +
           "AND r.reportStatus IN ('FINAL', 'AMENDED')) " +
           "ORDER BY s.studyDate ASC")
    List<Study> findUnreportedStatStudies();

    /**
     * Count studies by modality and date range
     */
    @Query("SELECT COUNT(s) FROM Study s WHERE s.modalitiesInStudy LIKE %:modality% " +
           "AND s.studyDate BETWEEN :startDate AND :endDate")
    long countByModalityAndDateRange(@Param("modality") String modality,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    /**
     * Count studies by status
     */
    long countByStudyStatus(String studyStatus);

    /**
     * Get total number of series across all studies
     */
    @Query("SELECT SUM(s.numberOfSeries) FROM Study s WHERE s.numberOfSeries IS NOT NULL")
    Long getTotalSeriesCount();

    /**
     * Get total number of instances across all studies
     */
    @Query("SELECT SUM(s.numberOfInstances) FROM Study s WHERE s.numberOfInstances IS NOT NULL")
    Long getTotalInstanceCount();

    /**
     * Find studies missing PACS URL (not yet stored)
     */
    @Query("SELECT s FROM Study s WHERE s.pacsUrl IS NULL OR s.pacsUrl = '' " +
           "ORDER BY s.studyDate DESC")
    List<Study> findStudiesNotInPACS();

    /**
     * Complex study search
     */
    @Query("SELECT s FROM Study s WHERE " +
           "(:patientId IS NULL OR s.patient.patientId = :patientId) AND " +
           "(:modality IS NULL OR s.modalitiesInStudy LIKE %:modality%) AND " +
           "(:startDate IS NULL OR s.studyDate >= :startDate) AND " +
           "(:endDate IS NULL OR s.studyDate <= :endDate) AND " +
           "(:status IS NULL OR s.studyStatus = :status) " +
           "ORDER BY s.studyDate DESC, s.studyTime DESC")
    List<Study> searchStudies(@Param("patientId") Long patientId,
                             @Param("modality") String modality,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate,
                             @Param("status") String status);
}
