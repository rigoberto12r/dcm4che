package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.dcm4che.ris.api.enums.ReportStatus;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Report entity.
 * Represents a radiology report with complete workflow support.
 * Based on DICOM SR (Structured Reporting) concepts and standard RIS reporting.
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "report", indexes = {
    @Index(name = "idx_report_status", columnList = "report_status"),
    @Index(name = "idx_report_dictated_by", columnList = "dictated_by_user_id"),
    @Index(name = "idx_report_verified_by", columnList = "verified_by_user_id"),
    @Index(name = "idx_report_locked_by", columnList = "locked_by_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    /**
     * DICOM Study this report is for
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id")
    private Study study;

    /**
     * Requested Procedure this report is for
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_procedure_id", nullable = false)
    private RequestedProcedure requestedProcedure;

    /**
     * Report Template used (if any)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_template_id")
    private ReportTemplate reportTemplate;

    /**
     * Report Status
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false, length = 16)
    @Builder.Default
    private ReportStatus reportStatus = ReportStatus.PENDING;

    // Report Content Fields

    /**
     * Dictation/Draft text
     * Used during initial creation and dictation
     */
    @Column(name = "dictation_text", columnDefinition = "TEXT")
    private String dictationText;

    /**
     * Final complete report text
     * Contains the full formatted report
     */
    @Column(name = "final_report_text", columnDefinition = "TEXT")
    private String finalReportText;

    /**
     * Findings section
     * Detailed description of imaging findings
     */
    @Column(name = "findings", columnDefinition = "TEXT")
    private String findings;

    /**
     * Impression/Conclusion section
     * Summary and diagnostic impression
     */
    @Column(name = "impression", columnDefinition = "TEXT")
    private String impression;

    /**
     * Recommendations section
     * Suggested follow-up or additional studies
     */
    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    /**
     * Comparison section
     * Comparison with prior studies
     */
    @Column(name = "comparison_text", columnDefinition = "TEXT")
    private String comparisonText;

    /**
     * Technique section
     * Description of imaging technique and protocol
     */
    @Column(name = "technique_text", columnDefinition = "TEXT")
    private String techniqueText;

    /**
     * Indication/Clinical History section
     */
    @Column(name = "indication_text", columnDefinition = "TEXT")
    private String indicationText;

    /**
     * Additional comments
     */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    // Workflow Tracking

    /**
     * User who dictated/created the report
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dictated_by_user_id")
    private User dictatedByUser;

    /**
     * Timestamp when dictation occurred
     */
    @Column(name = "dictated_at")
    private LocalDateTime dictatedAt;

    /**
     * User who transcribed the report (if applicable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transcribed_by_user_id")
    private User transcribedByUser;

    /**
     * Timestamp when transcription occurred
     */
    @Column(name = "transcribed_at")
    private LocalDateTime transcribedAt;

    /**
     * User who verified/signed the final report
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedByUser;

    /**
     * Timestamp when report was verified/signed
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * User who amended the report (if applicable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amended_by_user_id")
    private User amendedByUser;

    /**
     * Timestamp when amendment occurred
     */
    @Column(name = "amended_at")
    private LocalDateTime amendedAt;

    /**
     * Reason for amendment
     */
    @Column(name = "amendment_reason", columnDefinition = "TEXT")
    private String amendmentReason;

    // Concurrency Control (Lock Mechanism)

    /**
     * User who currently has the report locked for editing
     * Implements pessimistic locking to prevent concurrent edits
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by_user_id")
    private User lockedByUser;

    /**
     * Timestamp when lock was acquired
     * Locks expire after a configurable timeout (e.g., 30 minutes)
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    // DICOM Structured Reporting

    /**
     * DICOM SR Instance UID (if report is stored as DICOM SR)
     * SOP Instance UID (0008,0018)
     */
    @Size(max = 128)
    @Column(name = "report_sr_instance_uid", length = 128)
    private String reportSRInstanceUID;

    // Digital Signature

    /**
     * Digital signature data (base64 encoded)
     * Used for legal compliance and non-repudiation
     */
    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;

    /**
     * Timestamp of digital signature
     */
    @Column(name = "signature_date_time")
    private LocalDateTime signatureDateTime;

    /**
     * Signature method/algorithm used
     */
    @Size(max = 64)
    @Column(name = "signature_method", length = 64)
    private String signatureMethod;

    // Metadata

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // JPA Callbacks

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (reportStatus == null) {
            reportStatus = ReportStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Methods

    /**
     * Acquire lock for editing
     * Returns true if lock was successfully acquired
     */
    public boolean acquireLock(User user, int lockTimeoutMinutes) {
        // Check if already locked by someone else
        if (lockedByUser != null && !lockedByUser.equals(user)) {
            // Check if lock has expired
            LocalDateTime lockExpiry = lockedAt.plusMinutes(lockTimeoutMinutes);
            if (LocalDateTime.now().isBefore(lockExpiry)) {
                return false; // Still locked by another user
            }
        }

        // Acquire lock
        this.lockedByUser = user;
        this.lockedAt = LocalDateTime.now();
        return true;
    }

    /**
     * Release lock
     */
    public void releaseLock(User user) {
        if (lockedByUser != null && lockedByUser.equals(user)) {
            this.lockedByUser = null;
            this.lockedAt = null;
        }
    }

    /**
     * Check if report is currently locked
     */
    public boolean isLocked(int lockTimeoutMinutes) {
        if (lockedByUser == null || lockedAt == null) {
            return false;
        }
        LocalDateTime lockExpiry = lockedAt.plusMinutes(lockTimeoutMinutes);
        return LocalDateTime.now().isBefore(lockExpiry);
    }

    /**
     * Transition to new status with validation
     */
    public void transitionTo(ReportStatus newStatus, User user) {
        if (!reportStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition report from %s to %s", reportStatus, newStatus));
        }

        this.reportStatus = newStatus;
        LocalDateTime now = LocalDateTime.now();

        switch (newStatus) {
            case DRAFT:
                if (dictatedByUser == null) {
                    dictatedByUser = user;
                    dictatedAt = now;
                }
                break;
            case PRELIMINARY:
            case FINAL:
                verifiedByUser = user;
                verifiedAt = now;
                // Release lock when report is finalized
                releaseLock(user);
                break;
            case AMENDED:
                amendedByUser = user;
                amendedAt = now;
                break;
        }
    }

    /**
     * Sign the report (mark as FINAL)
     */
    public void sign(User user, String signature) {
        transitionTo(ReportStatus.FINAL, user);
        this.signatureData = signature;
        this.signatureDateTime = LocalDateTime.now();
    }

    /**
     * Amend a final report
     */
    public void amend(User user, String reason) {
        if (!reportStatus.isFinal()) {
            throw new IllegalStateException("Can only amend finalized reports");
        }
        transitionTo(ReportStatus.AMENDED, user);
        this.amendmentReason = reason;
    }

    /**
     * Get patient from study or requested procedure
     */
    public Patient getPatient() {
        if (study != null && study.getPatient() != null) {
            return study.getPatient();
        }
        if (requestedProcedure != null && requestedProcedure.getPatient() != null) {
            return requestedProcedure.getPatient();
        }
        return null;
    }

    /**
     * Get accession number
     */
    public String getAccessionNumber() {
        if (study != null && study.getAccessionNumber() != null) {
            return study.getAccessionNumber();
        }
        if (requestedProcedure != null) {
            return requestedProcedure.getAccessionNumber();
        }
        return null;
    }

    /**
     * Calculate time from creation to verification (TAT - Turnaround Time)
     */
    public Duration getTurnaroundTime() {
        if (verifiedAt != null && createdAt != null) {
            return Duration.between(createdAt, verifiedAt);
        }
        return null;
    }

    /**
     * Check if report is editable by user
     */
    public boolean isEditableBy(User user, int lockTimeoutMinutes) {
        // Final reports cannot be edited (only amended)
        if (reportStatus.isFinal()) {
            return false;
        }

        // If locked, must be locked by this user
        if (isLocked(lockTimeoutMinutes)) {
            return lockedByUser.equals(user);
        }

        return true;
    }

    /**
     * Build complete report text from sections
     */
    public String buildCompleteReport() {
        StringBuilder sb = new StringBuilder();

        if (indicationText != null && !indicationText.trim().isEmpty()) {
            sb.append("INDICATION:\n").append(indicationText).append("\n\n");
        }

        if (techniqueText != null && !techniqueText.trim().isEmpty()) {
            sb.append("TECHNIQUE:\n").append(techniqueText).append("\n\n");
        }

        if (comparisonText != null && !comparisonText.trim().isEmpty()) {
            sb.append("COMPARISON:\n").append(comparisonText).append("\n\n");
        }

        if (findings != null && !findings.trim().isEmpty()) {
            sb.append("FINDINGS:\n").append(findings).append("\n\n");
        }

        if (impression != null && !impression.trim().isEmpty()) {
            sb.append("IMPRESSION:\n").append(impression).append("\n\n");
        }

        if (recommendations != null && !recommendations.trim().isEmpty()) {
            sb.append("RECOMMENDATIONS:\n").append(recommendations).append("\n\n");
        }

        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Report)) return false;
        Report report = (Report) o;
        return reportId != null && reportId.equals(report.reportId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Report{" +
                "reportId=" + reportId +
                ", reportStatus=" + reportStatus +
                ", accessionNumber='" + getAccessionNumber() + '\'' +
                ", dictatedBy=" + (dictatedByUser != null ? dictatedByUser.getUsername() : null) +
                ", verifiedBy=" + (verifiedByUser != null ? verifiedByUser.getUsername() : null) +
                '}';
    }
}
