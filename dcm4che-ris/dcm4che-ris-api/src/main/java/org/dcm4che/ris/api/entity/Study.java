package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DICOM Study entity.
 * Represents a DICOM study with metadata from PACS.
 * Based on DICOM Study Module (PS 3.3 C.7.2.1).
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "study", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"study_instance_uid"})
}, indexes = {
    @Index(name = "idx_study_accession", columnList = "accession_number"),
    @Index(name = "idx_study_date", columnList = "study_date"),
    @Index(name = "idx_study_status", columnList = "study_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Study implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_id")
    private Long studyId;

    /**
     * Study Instance UID (0020,000D)
     */
    @NotBlank
    @Size(max = 128)
    @Column(name = "study_instance_uid", nullable = false, unique = true, length = 128)
    private String studyInstanceUID;

    /**
     * Accession Number (0008,0050)
     */
    @Size(max = 64)
    @Column(name = "accession_number", length = 64)
    private String accessionNumber;

    /**
     * Patient
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Related Requested Procedure
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_procedure_id")
    private RequestedProcedure requestedProcedure;

    /**
     * Study Date (0008,0020)
     */
    @Column(name = "study_date")
    private LocalDate studyDate;

    /**
     * Study Time (0008,0030)
     */
    @Column(name = "study_time")
    private LocalTime studyTime;

    /**
     * Study Description (0008,1030)
     */
    @Size(max = 255)
    @Column(name = "study_description", length = 255)
    private String studyDescription;

    /**
     * Modalities in Study (0008,0061)
     * Multiple values separated by backslash (e.g., "CT\\MR")
     */
    @Size(max = 128)
    @Column(name = "modalities_in_study", length = 128)
    private String modalitiesInStudy;

    /**
     * Number of Study Related Series (0020,1206)
     */
    @Column(name = "number_of_series")
    private Integer numberOfSeries;

    /**
     * Number of Study Related Instances (0020,1208)
     */
    @Column(name = "number_of_instances")
    private Integer numberOfInstances;

    /**
     * Referring Physician
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referring_physician_id")
    private Physician referringPhysician;

    /**
     * Reading/Reporting Radiologist
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_physician_id")
    private Physician readingPhysician;

    /**
     * Study Status
     * Values: SCHEDULED, RECEIVED, READING, PRELIMINARY, FINAL, AMENDED
     */
    @Size(max = 16)
    @Column(name = "study_status", length = 16)
    @Builder.Default
    private String studyStatus = "SCHEDULED";

    /**
     * Storage Status
     * Values: PENDING, STORED, COMMITTED, ARCHIVED
     */
    @Size(max = 16)
    @Column(name = "storage_status", length = 16)
    @Builder.Default
    private String storageStatus = "PENDING";

    /**
     * URL to PACS viewer for this study
     */
    @Size(max = 512)
    @Column(name = "pacs_url", length = 512)
    private String pacsUrl;

    /**
     * Total size of study in bytes
     */
    @Column(name = "study_size_bytes")
    private Long studySizeBytes;

    /**
     * Study ID (0020,0010) - different from our internal study_id
     */
    @Size(max = 16)
    @Column(name = "dicom_study_id", length = 16)
    private String dicomStudyId;

    /**
     * Study received date/time (when first images arrived)
     */
    @Column(name = "study_received_at")
    private LocalDateTime studyReceivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships

    /**
     * Reports for this study
     */
    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Report> reports = new ArrayList<>();

    // JPA Callbacks

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (studyStatus == null) {
            studyStatus = "SCHEDULED";
        }
        if (storageStatus == null) {
            storageStatus = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Methods

    /**
     * Check if study has been received from PACS
     */
    public boolean isReceived() {
        return studyReceivedAt != null || "RECEIVED".equals(studyStatus) ||
               "READING".equals(studyStatus) || "PRELIMINARY".equals(studyStatus) ||
               "FINAL".equals(studyStatus);
    }

    /**
     * Check if study is finalized
     */
    public boolean isFinal() {
        return "FINAL".equals(studyStatus) || "AMENDED".equals(studyStatus);
    }

    /**
     * Add a report
     */
    public void addReport(Report report) {
        reports.add(report);
        report.setStudy(this);
    }

    /**
     * Get the final report if exists
     */
    public Report getFinalReport() {
        return reports.stream()
            .filter(r -> "FINAL".equals(r.getReportStatus()) ||
                        "AMENDED".equals(r.getReportStatus()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Study)) return false;
        Study study = (Study) o;
        return studyId != null && studyId.equals(study.studyId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Study{" +
                "studyId=" + studyId +
                ", studyInstanceUID='" + studyInstanceUID + '\'' +
                ", accessionNumber='" + accessionNumber + '\'' +
                ", studyDate=" + studyDate +
                ", studyDescription='" + studyDescription + '\'' +
                '}';
    }
}
