package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.Report;
import org.dcm4che.ris.api.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Report entity.
 * Manages radiology reports and reporting workflow.
 *
 * @author dcm4che-ris
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    /**
     * Find reports by study
     */
    @Query("SELECT r FROM Report r WHERE r.study.studyId = :studyId ORDER BY r.createdAt DESC")
    List<Report> findByStudyStudyId(@Param("studyId") Long studyId);

    /**
     * Find latest report for a study
     */
    @Query("SELECT r FROM Report r WHERE r.study.studyId = :studyId ORDER BY r.createdAt DESC")
    Optional<Report> findLatestReportForStudy(@Param("studyId") Long studyId);

    /**
     * Find final report for a study
     */
    @Query("SELECT r FROM Report r WHERE r.study.studyId = :studyId " +
           "AND r.reportStatus IN ('FINAL', 'AMENDED', 'CORRECTED') " +
           "ORDER BY r.verifiedAt DESC")
    Optional<Report> findFinalReportForStudy(@Param("studyId") Long studyId);

    /**
     * Find reports by status
     */
    List<Report> findByReportStatus(ReportStatus reportStatus);

    /**
     * Find draft reports (being worked on)
     */
    @Query("SELECT r FROM Report r WHERE r.reportStatus = 'DRAFT' ORDER BY r.updatedAt DESC")
    List<Report> findDraftReports();

    /**
     * Find preliminary reports (pending final sign-off)
     */
    @Query("SELECT r FROM Report r WHERE r.reportStatus = 'PRELIMINARY' ORDER BY r.createdAt DESC")
    List<Report> findPreliminaryReports();

    /**
     * Find pending reports (not yet started)
     */
    @Query("SELECT r FROM Report r WHERE r.reportStatus = 'PENDING' ORDER BY r.createdAt ASC")
    List<Report> findPendingReports();

    /**
     * Find reports by dictating user
     */
    @Query("SELECT r FROM Report r WHERE r.dictatedByUser.userId = :userId ORDER BY r.dictatedAt DESC")
    List<Report> findByDictatingUser(@Param("userId") Long userId);

    /**
     * Find reports by verifying/signing radiologist
     */
    @Query("SELECT r FROM Report r WHERE r.verifiedByUser.userId = :userId ORDER BY r.verifiedAt DESC")
    List<Report> findByVerifyingUser(@Param("userId") Long userId);

    /**
     * Find reports locked by user
     */
    @Query("SELECT r FROM Report r WHERE r.lockedByUser.userId = :userId AND r.reportStatus NOT IN ('FINAL', 'AMENDED')")
    List<Report> findLockedByUser(@Param("userId") Long userId);

    /**
     * Find reports for a specific patient
     */
    @Query("SELECT r FROM Report r WHERE r.study.patient.patientId = :patientId ORDER BY r.createdAt DESC")
    List<Report> findByPatient(@Param("patientId") Long patientId);

    /**
     * Find final reports for a patient
     */
    @Query("SELECT r FROM Report r WHERE r.study.patient.patientId = :patientId " +
           "AND r.reportStatus IN ('FINAL', 'AMENDED', 'CORRECTED') " +
           "ORDER BY r.verifiedAt DESC")
    List<Report> findFinalReportsByPatient(@Param("patientId") Long patientId);

    /**
     * Find reports finalized in date range
     */
    @Query("SELECT r FROM Report r WHERE r.verifiedAt BETWEEN :startDate AND :endDate " +
           "AND r.reportStatus IN ('FINAL', 'AMENDED', 'CORRECTED') " +
           "ORDER BY r.verifiedAt DESC")
    List<Report> findFinalizedBetween(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Find reports created in date range
     */
    @Query("SELECT r FROM Report r WHERE r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    List<Report> findCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Find addenda for a report
     */
    @Query("SELECT r FROM Report r WHERE r.originalReport.reportId = :reportId ORDER BY r.createdAt ASC")
    List<Report> findAddenda(@Param("reportId") Long reportId);

    /**
     * Find amended reports
     */
    @Query("SELECT r FROM Report r WHERE r.reportStatus IN ('AMENDED', 'CORRECTED') ORDER BY r.amendedAt DESC")
    List<Report> findAmendedReports();

    /**
     * Find reports by priority
     */
    List<Report> findByReportPriority(String reportPriority);

    /**
     * Find urgent reports not yet finalized
     */
    @Query("SELECT r FROM Report r WHERE r.reportPriority IN ('URGENT', 'STAT') " +
           "AND r.reportStatus NOT IN ('FINAL', 'AMENDED', 'CORRECTED') " +
           "ORDER BY r.createdAt ASC")
    List<Report> findUrgentUnfinalizedReports();

    /**
     * Count reports by status
     */
    long countByReportStatus(ReportStatus reportStatus);

    /**
     * Count reports by radiologist and date range
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.verifiedByUser.userId = :userId " +
           "AND r.verifiedAt BETWEEN :startDate AND :endDate " +
           "AND r.reportStatus IN ('FINAL', 'AMENDED')")
    long countReportsByRadiologistAndDateRange(@Param("userId") Long userId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Get average turnaround time (study received to report signed)
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, r.study.studyReceivedDate, r.verifiedAt)) " +
           "FROM Report r WHERE r.reportStatus IN ('FINAL', 'AMENDED') " +
           "AND r.verifiedAt BETWEEN :startDate AND :endDate")
    Double getAverageTurnaroundTime(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Find reports exceeding turnaround time threshold
     */
    @Query("SELECT r FROM Report r WHERE r.reportStatus NOT IN ('FINAL', 'AMENDED', 'CORRECTED') " +
           "AND TIMESTAMPDIFF(HOUR, r.createdAt, CURRENT_TIMESTAMP) > :hoursThreshold " +
           "ORDER BY r.createdAt ASC")
    List<Report> findReportsExceedingTurnaround(@Param("hoursThreshold") int hoursThreshold);

    /**
     * Search reports by content (findings, impression)
     */
    @Query("SELECT r FROM Report r WHERE " +
           "(LOWER(r.findings) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.impression) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND r.reportStatus IN ('FINAL', 'AMENDED', 'CORRECTED') " +
           "ORDER BY r.verifiedAt DESC")
    List<Report> searchByContent(@Param("keyword") String keyword);
}
