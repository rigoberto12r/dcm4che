package org.dcm4che.ris.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.ris.api.entity.Report;
import org.dcm4che.ris.api.entity.ReportTemplate;
import org.dcm4che.ris.api.entity.Study;
import org.dcm4che.ris.api.entity.User;
import org.dcm4che.ris.api.enums.ReportStatus;
import org.dcm4che.ris.core.exception.InvalidStatusTransitionException;
import org.dcm4che.ris.core.exception.ReportLockedException;
import org.dcm4che.ris.core.exception.ResourceNotFoundException;
import org.dcm4che.ris.persistence.repository.ReportRepository;
import org.dcm4che.ris.persistence.repository.ReportTemplateRepository;
import org.dcm4che.ris.persistence.repository.StudyRepository;
import org.dcm4che.ris.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing radiology reports and reporting workflow.
 *
 * @author dcm4che-ris
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ReportingService {

    private final ReportRepository reportRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final ReportTemplateRepository templateRepository;

    private static final int DEFAULT_LOCK_TIMEOUT_MINUTES = 15;

    /**
     * Create a new report for a study.
     */
    public Report createReport(Long studyId, Long userId, Long templateId) {
        log.info("Creating new report for study: {}", studyId);

        // Validate study exists
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new ResourceNotFoundException("Study", studyId));

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Create new report
        Report report = Report.builder()
                .study(study)
                .reportStatus(ReportStatus.PENDING)
                .dictatedByUser(user)
                .dictatedAt(LocalDateTime.now())
                .studyReceivedAt(study.getStudyReceivedDate())
                .reportStartedAt(LocalDateTime.now())
                .build();

        // Apply template if provided
        if (templateId != null) {
            ReportTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new ResourceNotFoundException("ReportTemplate", templateId));
            applyTemplate(report, template);
            report.setTemplate(template);

            // Increment template usage
            template.incrementUsage();
            templateRepository.save(template);
        }

        Report savedReport = reportRepository.save(report);
        log.info("Report created successfully with ID: {}", savedReport.getReportId());

        return savedReport;
    }

    /**
     * Acquire lock on a report for editing.
     */
    public Report acquireLock(Long reportId, Long userId) {
        log.info("User {} attempting to acquire lock on report {}", userId, reportId);

        Report report = getReportById(reportId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Check if report is in editable status
        if (!report.getReportStatus().isEditable()) {
            throw new InvalidStatusTransitionException(
                    "Cannot edit report in status: " + report.getReportStatus(),
                    report.getReportStatus().name(),
                    "EDIT");
        }

        // Try to acquire lock
        boolean lockAcquired = report.acquireLock(user, DEFAULT_LOCK_TIMEOUT_MINUTES);
        if (!lockAcquired) {
            throw new ReportLockedException(reportId, report.getLockedByUser().getUsername());
        }

        Report savedReport = reportRepository.save(report);
        log.info("Lock acquired on report {} by user {}", reportId, userId);

        return savedReport;
    }

    /**
     * Release lock on a report.
     */
    public Report releaseLock(Long reportId, Long userId) {
        log.info("User {} releasing lock on report {}", userId, reportId);

        Report report = getReportById(reportId);

        // Check if user has the lock
        if (report.getLockedByUser() == null ||
            !report.getLockedByUser().getUserId().equals(userId)) {
            throw new IllegalStateException("User does not have lock on this report");
        }

        report.releaseLock();
        Report savedReport = reportRepository.save(report);

        log.info("Lock released on report {}", reportId);
        return savedReport;
    }

    /**
     * Update report content (findings, impression, etc.).
     */
    public Report updateReportContent(Long reportId, Long userId, Report updatedContent) {
        log.info("Updating report content: {}", reportId);

        Report report = getReportById(reportId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Verify user has lock
        if (!report.isEditableBy(user)) {
            throw new ReportLockedException(reportId,
                    report.getLockedByUser() != null ? report.getLockedByUser().getUsername() : "Unknown");
        }

        // Update content fields
        if (updatedContent.getDictationText() != null) {
            report.setDictationText(updatedContent.getDictationText());
        }
        if (updatedContent.getFindings() != null) {
            report.setFindings(updatedContent.getFindings());
        }
        if (updatedContent.getImpression() != null) {
            report.setImpression(updatedContent.getImpression());
        }
        if (updatedContent.getRecommendations() != null) {
            report.setRecommendations(updatedContent.getRecommendations());
        }
        if (updatedContent.getComparisonText() != null) {
            report.setComparisonText(updatedContent.getComparisonText());
        }
        if (updatedContent.getTechniqueText() != null) {
            report.setTechniqueText(updatedContent.getTechniqueText());
        }
        if (updatedContent.getClinicalHistory() != null) {
            report.setClinicalHistory(updatedContent.getClinicalHistory());
        }

        // Transition to DRAFT if still PENDING
        if (report.getReportStatus() == ReportStatus.PENDING) {
            report.setReportStatus(ReportStatus.DRAFT);
        }

        Report savedReport = reportRepository.save(report);
        log.info("Report content updated: {}", reportId);

        return savedReport;
    }

    /**
     * Sign a report (finalize it).
     */
    public Report signReport(Long reportId, Long userId, String signature) {
        log.info("Signing report: {} by user: {}", reportId, userId);

        Report report = getReportById(reportId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Verify user can sign reports
        if (!user.canSignReports()) {
            throw new IllegalStateException("User is not authorized to sign reports");
        }

        // Sign the report
        report.sign(user, signature);
        report.setReportCompletedAt(LocalDateTime.now());

        Report savedReport = reportRepository.save(report);
        log.info("Report signed successfully: {}", reportId);

        return savedReport;
    }

    /**
     * Amend a finalized report.
     */
    public Report amendReport(Long reportId, Long userId, String amendmentReason, String amendmentText) {
        log.info("Amending report: {} by user: {}", reportId, userId);

        Report report = getReportById(reportId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Amend the report
        report.amend(user, amendmentReason);

        // Update content if provided
        if (amendmentText != null) {
            report.setFindings(report.getFindings() + "\n\nAMENDMENT: " + amendmentText);
        }

        Report savedReport = reportRepository.save(report);
        log.info("Report amended successfully: {}", reportId);

        return savedReport;
    }

    /**
     * Create a preliminary report.
     */
    public Report createPreliminaryReport(Long reportId, Long userId) {
        log.info("Creating preliminary report: {} by user: {}", reportId, userId);

        Report report = getReportById(reportId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Transition to PRELIMINARY
        if (!report.getReportStatus().canTransitionTo(ReportStatus.PRELIMINARY)) {
            throw new InvalidStatusTransitionException(
                    report.getReportStatus().name(),
                    ReportStatus.PRELIMINARY.name());
        }

        report.setReportStatus(ReportStatus.PRELIMINARY);
        report.setVerifiedByUser(user);
        report.setVerifiedAt(LocalDateTime.now());

        Report savedReport = reportRepository.save(report);
        log.info("Preliminary report created: {}", reportId);

        return savedReport;
    }

    /**
     * Get report by ID.
     */
    @Transactional(readOnly = true)
    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));
    }

    /**
     * Get reports for a study.
     */
    @Transactional(readOnly = true)
    public List<Report> getReportsByStudy(Long studyId) {
        return reportRepository.findByStudyStudyId(studyId);
    }

    /**
     * Get final report for a study.
     */
    @Transactional(readOnly = true)
    public Report getFinalReportForStudy(Long studyId) {
        return reportRepository.findFinalReportForStudy(studyId)
                .orElseThrow(() -> new ResourceNotFoundException("Final report for study: " + studyId));
    }

    /**
     * Get pending reports.
     */
    @Transactional(readOnly = true)
    public List<Report> getPendingReports() {
        return reportRepository.findPendingReports();
    }

    /**
     * Get draft reports.
     */
    @Transactional(readOnly = true)
    public List<Report> getDraftReports() {
        return reportRepository.findDraftReports();
    }

    /**
     * Get preliminary reports.
     */
    @Transactional(readOnly = true)
    public List<Report> getPreliminaryReports() {
        return reportRepository.findPreliminaryReports();
    }

    /**
     * Get reports by dictating user.
     */
    @Transactional(readOnly = true)
    public List<Report> getReportsByDictatingUser(Long userId) {
        return reportRepository.findByDictatingUser(userId);
    }

    /**
     * Get reports by verifying user (radiologist).
     */
    @Transactional(readOnly = true)
    public List<Report> getReportsByVerifyingUser(Long userId) {
        return reportRepository.findByVerifyingUser(userId);
    }

    /**
     * Get reports locked by user.
     */
    @Transactional(readOnly = true)
    public List<Report> getReportsLockedByUser(Long userId) {
        return reportRepository.findLockedByUser(userId);
    }

    /**
     * Get urgent unfanalized reports.
     */
    @Transactional(readOnly = true)
    public List<Report> getUrgentUnfinalizedReports() {
        return reportRepository.findUrgentUnfinalizedReports();
    }

    /**
     * Get reports finalized in date range.
     */
    @Transactional(readOnly = true)
    public List<Report> getReportsFinalizedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return reportRepository.findFinalizedBetween(startDate, endDate);
    }

    /**
     * Get average turnaround time.
     */
    @Transactional(readOnly = true)
    public Double getAverageTurnaroundTime(LocalDateTime startDate, LocalDateTime endDate) {
        return reportRepository.getAverageTurnaroundTime(startDate, endDate);
    }

    /**
     * Count reports by status.
     */
    @Transactional(readOnly = true)
    public long countReportsByStatus(ReportStatus status) {
        return reportRepository.countByReportStatus(status);
    }

    /**
     * Search reports by content.
     */
    @Transactional(readOnly = true)
    public List<Report> searchReportsByContent(String keyword) {
        return reportRepository.searchByContent(keyword);
    }

    // Helper methods

    /**
     * Apply a template to a report.
     */
    private void applyTemplate(Report report, ReportTemplate template) {
        Map<String, String> templateValues = new HashMap<>();
        // Add default values that can be filled from study/patient
        templateValues.put("patient_name", report.getStudy().getPatient().getPatientName());
        templateValues.put("patient_id", report.getStudy().getPatient().getMrn());
        templateValues.put("study_date", report.getStudy().getStudyDate().toString());

        String filledTemplate = template.fillTemplate(templateValues);

        report.setFinalReportText(filledTemplate);
        if (template.getDefaultFindings() != null) {
            report.setFindings(template.getDefaultFindings());
        }
        if (template.getDefaultImpression() != null) {
            report.setImpression(template.getDefaultImpression());
        }
        if (template.getDefaultTechnique() != null) {
            report.setTechniqueText(template.getDefaultTechnique());
        }
        if (template.getDefaultComparison() != null) {
            report.setComparisonText(template.getDefaultComparison());
        }
    }
}
