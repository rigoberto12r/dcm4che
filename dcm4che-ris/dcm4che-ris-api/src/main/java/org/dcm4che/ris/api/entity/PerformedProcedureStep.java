package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.dcm4che.ris.api.enums.PPSStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Performed Procedure Step (PPS) entity.
 * Represents actual execution of a procedure, received via DICOM MPPS.
 * Based on DICOM Performed Procedure Step SOP Class (PS 3.4 F.7).
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "performed_procedure_step", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sop_instance_uid"})
}, indexes = {
    @Index(name = "idx_pps_status", columnList = "pps_status"),
    @Index(name = "idx_pps_start_date", columnList = "pps_start_date_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformedProcedureStep implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pps_id")
    private Long ppsId;

    /**
     * Related Scheduled Procedure Step
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sps_id")
    private ScheduledProcedureStep scheduledProcedureStep;

    /**
     * Performed Procedure Step ID (0040,0253)
     * ID received from MPPS N-CREATE
     */
    @NotBlank
    @Size(max = 64)
    @Column(name = "performed_procedure_step_id", nullable = false, length = 64)
    private String performedProcedureStepId;

    /**
     * SOP Instance UID (0008,0018)
     * UID of the MPPS SOP Instance
     */
    @NotBlank
    @Size(max = 128)
    @Column(name = "sop_instance_uid", nullable = false, unique = true, length = 128)
    private String sopInstanceUID;

    /**
     * Performed Station AE Title (0040,0241)
     * AE Title where procedure was actually performed
     */
    @NotBlank
    @Size(max = 16)
    @Column(name = "performed_station_ae_title", nullable = false, length = 16)
    private String performedStationAETitle;

    /**
     * Performed Station Name (0040,0242)
     */
    @Size(max = 64)
    @Column(name = "performed_station_name", length = 64)
    private String performedStationName;

    /**
     * Performed Procedure Step Start Date and Time (0040,0244)
     */
    @NotNull
    @Column(name = "pps_start_date_time", nullable = false)
    private LocalDateTime ppsStartDateTime;

    /**
     * Performed Procedure Step End Date and Time (0040,0250)
     */
    @Column(name = "pps_end_date_time")
    private LocalDateTime ppsEndDateTime;

    /**
     * Performed Procedure Step Status (0040,0252)
     * Values: IN_PROGRESS, COMPLETED, DISCONTINUED
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "pps_status", nullable = false, length = 16)
    @Builder.Default
    private PPSStatus ppsStatus = PPSStatus.IN_PROGRESS;

    /**
     * Performed Procedure Step Description (0040,0254)
     */
    @Size(max = 255)
    @Column(name = "performed_procedure_step_description", length = 255)
    private String performedProcedureStepDescription;

    /**
     * Performed Protocol Code Value (0040,0260)
     */
    @Size(max = 64)
    @Column(name = "performed_protocol_code", length = 64)
    private String performedProtocolCode;

    /**
     * Modality (0008,0060)
     */
    @Size(max = 16)
    @Column(name = "modality", length = 16)
    private String modality;

    /**
     * Study Instance UID (0020,000D)
     * From MPPS, identifies the study
     */
    @Size(max = 128)
    @Column(name = "study_instance_uid", length = 128)
    private String studyInstanceUID;

    /**
     * Accession Number (0008,0050)
     */
    @Size(max = 64)
    @Column(name = "accession_number", length = 64)
    private String accessionNumber;

    /**
     * Performing Physician
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_physician_id")
    private Physician performedPhysician;

    /**
     * Performing Physician's Name (0008,1050) - if not linked to Physician entity
     */
    @Size(max = 255)
    @Column(name = "performing_physician_name", length = 255)
    private String performingPhysicianName;

    /**
     * Dose Area Product (DAP) in dGy·cm²
     * For radiation dose tracking
     */
    @Column(name = "dose_area_product", precision = 10, scale = 2)
    private BigDecimal doseAreaProduct;

    /**
     * Total Time of Fluoroscopy in seconds
     */
    @Column(name = "total_time_of_fluoroscopy", precision = 10, scale = 2)
    private BigDecimal totalTimeOfFluoroscopy;

    /**
     * Number of Series Created
     */
    @Column(name = "number_of_series")
    private Integer numberOfSeries;

    /**
     * Number of Instances Created
     */
    @Column(name = "number_of_instances")
    private Integer numberOfInstances;

    /**
     * Comments on the Performed Procedure Step (0040,0280)
     */
    @Column(name = "comments_on_performed_procedure_step", columnDefinition = "TEXT")
    private String commentsOnPerformedProcedureStep;

    /**
     * Discontinuation Reason Code (0040,0281)
     * If status is DISCONTINUED
     */
    @Size(max = 64)
    @Column(name = "discontinuation_reason_code", length = 64)
    private String discontinuationReasonCode;

    /**
     * Referenced Series Instance UIDs (comma-separated)
     * Series created during this PPS
     */
    @Column(name = "series_instance_uids", columnDefinition = "TEXT")
    private String seriesInstanceUIDs;

    /**
     * MPPS received date/time (when N-CREATE was received)
     */
    @Column(name = "mpps_received_at")
    private LocalDateTime mppsReceivedAt;

    /**
     * MPPS updated date/time (when N-SET was received)
     */
    @Column(name = "mpps_updated_at")
    private LocalDateTime mppsUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // JPA Callbacks

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (ppsStatus == null) {
            ppsStatus = PPSStatus.IN_PROGRESS;
        }
        if (mppsReceivedAt == null) {
            mppsReceivedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        mppsUpdatedAt = LocalDateTime.now();
    }

    // Business Methods

    /**
     * Mark PPS as completed
     */
    public void complete() {
        if (this.ppsStatus != PPSStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete PPS that is IN_PROGRESS");
        }
        this.ppsStatus = PPSStatus.COMPLETED;
        if (this.ppsEndDateTime == null) {
            this.ppsEndDateTime = LocalDateTime.now();
        }
    }

    /**
     * Mark PPS as discontinued
     */
    public void discontinue(String reasonCode) {
        if (this.ppsStatus != PPSStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only discontinue PPS that is IN_PROGRESS");
        }
        this.ppsStatus = PPSStatus.DISCONTINUED;
        this.discontinuationReasonCode = reasonCode;
        if (this.ppsEndDateTime == null) {
            this.ppsEndDateTime = LocalDateTime.now();
        }
    }

    /**
     * Calculate procedure duration in minutes
     */
    public Long getDurationMinutes() {
        if (ppsStartDateTime != null && ppsEndDateTime != null) {
            return java.time.Duration.between(ppsStartDateTime, ppsEndDateTime).toMinutes();
        }
        return null;
    }

    /**
     * Check if PPS is final (COMPLETED or DISCONTINUED)
     */
    public boolean isFinal() {
        return ppsStatus == PPSStatus.COMPLETED || ppsStatus == PPSStatus.DISCONTINUED;
    }

    /**
     * Get patient from related SPS
     */
    public Patient getPatient() {
        if (scheduledProcedureStep != null) {
            return scheduledProcedureStep.getPatient();
        }
        return null;
    }

    /**
     * Update status from MPPS N-SET
     */
    public void updateFromMPPS(PPSStatus newStatus, LocalDateTime endDateTime,
                               Integer series, Integer instances) {
        this.ppsStatus = newStatus;
        this.ppsEndDateTime = endDateTime;
        this.numberOfSeries = series;
        this.numberOfInstances = instances;

        // Update corresponding SPS if exists
        if (scheduledProcedureStep != null && newStatus == PPSStatus.COMPLETED) {
            // SPS should transition to COMPLETED as well
            scheduledProcedureStep.setActualStartDateTime(this.ppsStartDateTime);
            scheduledProcedureStep.setActualEndDateTime(this.ppsEndDateTime);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PerformedProcedureStep)) return false;
        PerformedProcedureStep that = (PerformedProcedureStep) o;
        return ppsId != null && ppsId.equals(that.ppsId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PerformedProcedureStep{" +
                "ppsId=" + ppsId +
                ", performedProcedureStepId='" + performedProcedureStepId + '\'' +
                ", sopInstanceUID='" + sopInstanceUID + '\'' +
                ", performedStationAETitle='" + performedStationAETitle + '\'' +
                ", ppsStatus=" + ppsStatus +
                ", ppsStartDateTime=" + ppsStartDateTime +
                '}';
    }
}
