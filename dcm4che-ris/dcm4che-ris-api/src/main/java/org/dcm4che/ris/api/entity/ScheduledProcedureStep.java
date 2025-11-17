package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.dcm4che.ris.api.enums.SPSStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduled Procedure Step (SPS) entity.
 * Represents the scheduling of a procedure on a specific modality.
 * Based on DICOM Scheduled Procedure Step Module and IHE Scheduled Workflow.
 *
 * This is the core entity for DICOM Modality Worklist (MWL) functionality.
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "scheduled_procedure_step", indexes = {
    @Index(name = "idx_sps_ae_title_date", columnList = "scheduled_station_ae_title,scheduled_start_date_time"),
    @Index(name = "idx_sps_status", columnList = "sps_status"),
    @Index(name = "idx_sps_scheduled_start", columnList = "scheduled_start_date_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledProcedureStep implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sps_id")
    private Long spsId;

    /**
     * Parent Requested Procedure
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_procedure_id", nullable = false)
    private RequestedProcedure requestedProcedure;

    /**
     * Scheduled Procedure Step ID (0040,0009)
     * Unique identifier for this SPS
     * HL7: IPC-3
     */
    @Size(max = 64)
    @Column(name = "sps_id_code", length = 64)
    private String spsIdCode;

    /**
     * Modality (0008,0060)
     * Type of equipment (CT, MR, CR, DX, US, NM, PT, MG, etc.)
     * HL7: OBR-24
     */
    @NotNull
    @Size(max = 16)
    @Column(name = "modality", nullable = false, length = 16)
    private String modality;

    /**
     * Scheduled Station AE Title (0040,0001)
     * DICOM AE Title of the modality where procedure is scheduled
     */
    @NotNull
    @Size(max = 16)
    @Column(name = "scheduled_station_ae_title", nullable = false, length = 16)
    private String scheduledStationAETitle;

    /**
     * Scheduled Station Name (0040,0010)
     * Human-readable name of the station
     */
    @Size(max = 64)
    @Column(name = "scheduled_station_name", length = 64)
    private String scheduledStationName;

    /**
     * Scheduled Procedure Step Start Date and Time (0040,0002)
     * HL7: IPC-4
     */
    @NotNull
    @Column(name = "scheduled_start_date_time", nullable = false)
    private LocalDateTime scheduledStartDateTime;

    /**
     * Expected end date/time (calculated or manual)
     */
    @Column(name = "scheduled_end_date_time")
    private LocalDateTime scheduledEndDateTime;

    /**
     * Scheduled Performing Physician's Name (0040,0006)
     * Reference to the physician who will perform the procedure
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_performing_physician_id")
    private Physician scheduledPerformingPhysician;

    /**
     * Scheduled Procedure Step Location (0040,0011)
     * Physical location where procedure will be performed
     */
    @Size(max = 64)
    @Column(name = "scheduled_procedure_step_location", length = 64)
    private String scheduledProcedureStepLocation;

    /**
     * Scheduled Procedure Step Status (0040,0020)
     * HL7: IPC-6
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sps_status", nullable = false, length = 16)
    @Builder.Default
    private SPSStatus spsStatus = SPSStatus.SCHEDULED;

    /**
     * Scheduled Procedure Step Description (0040,0007)
     */
    @Size(max = 255)
    @Column(name = "sps_description", length = 255)
    private String spsDescription;

    /**
     * Protocol Code/Name
     * Imaging protocol to be used
     */
    @Size(max = 64)
    @Column(name = "protocol_code", length = 64)
    private String protocolCode;

    /**
     * Pre-medication (0040,0012)
     * Required pre-medication for this procedure
     */
    @Column(name = "pre_medication", columnDefinition = "TEXT")
    private String preMedication;

    /**
     * Requested Contrast Agent (0018,0010)
     */
    @Size(max = 64)
    @Column(name = "requested_contrast_agent", length = 64)
    private String requestedContrastAgent;

    /**
     * Patient Position (0018,5100)
     * Values: HFS, HFP, HFDR, HFDL, FFP, FFS, etc.
     */
    @Size(max = 16)
    @Column(name = "patient_position", length = 16)
    private String patientPosition;

    /**
     * Comments/Special instructions for technologist
     */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    /**
     * Actual arrival time (when patient arrived)
     */
    @Column(name = "arrival_date_time")
    private LocalDateTime arrivalDateTime;

    /**
     * Actual start time (when procedure started)
     */
    @Column(name = "actual_start_date_time")
    private LocalDateTime actualStartDateTime;

    /**
     * Actual completion time
     */
    @Column(name = "actual_end_date_time")
    private LocalDateTime actualEndDateTime;

    /**
     * User who last modified the status
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by_user_id")
    private User modifiedByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships

    /**
     * Performed Procedure Steps (MPPS) for this SPS
     */
    @OneToMany(mappedBy = "scheduledProcedureStep", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PerformedProcedureStep> performedProcedureSteps = new ArrayList<>();

    /**
     * Reference to Modality configuration
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modality_id")
    private Modality modalityDevice;

    // JPA Callbacks

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (spsStatus == null) {
            spsStatus = SPSStatus.SCHEDULED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Methods

    /**
     * Transition to new status with validation
     */
    public void transitionTo(SPSStatus newStatus, User user) {
        if (!spsStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", spsStatus, newStatus));
        }

        this.spsStatus = newStatus;
        this.modifiedByUser = user;

        // Set timestamps based on status
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case ARRIVED:
                if (arrivalDateTime == null) {
                    arrivalDateTime = now;
                }
                break;
            case STARTED:
                if (actualStartDateTime == null) {
                    actualStartDateTime = now;
                }
                break;
            case COMPLETED:
            case DISCONTINUED:
                if (actualEndDateTime == null) {
                    actualEndDateTime = now;
                }
                break;
        }
    }

    /**
     * Mark patient as arrived
     */
    public void markArrived(User user) {
        transitionTo(SPSStatus.ARRIVED, user);
    }

    /**
     * Start the procedure
     */
    public void start(User user) {
        transitionTo(SPSStatus.STARTED, user);
    }

    /**
     * Complete the procedure
     */
    public void complete(User user) {
        transitionTo(SPSStatus.COMPLETED, user);
    }

    /**
     * Cancel the procedure
     */
    public void cancel(User user) {
        transitionTo(SPSStatus.CANCELED, user);
    }

    /**
     * Add a Performed Procedure Step
     */
    public void addPerformedProcedureStep(PerformedProcedureStep pps) {
        performedProcedureSteps.add(pps);
        pps.setScheduledProcedureStep(this);
    }

    /**
     * Get the patient for this SPS
     */
    public Patient getPatient() {
        return requestedProcedure != null && requestedProcedure.getOrder() != null ?
               requestedProcedure.getOrder().getPatient() : null;
    }

    /**
     * Get the accession number
     */
    public String getAccessionNumber() {
        return requestedProcedure != null ? requestedProcedure.getAccessionNumber() : null;
    }

    /**
     * Get the study instance UID
     */
    public String getStudyInstanceUID() {
        return requestedProcedure != null ? requestedProcedure.getStudyInstanceUID() : null;
    }

    /**
     * Check if SPS is overdue
     */
    public boolean isOverdue() {
        if (spsStatus == SPSStatus.COMPLETED || spsStatus == SPSStatus.CANCELED) {
            return false;
        }
        return scheduledStartDateTime != null &&
               scheduledStartDateTime.isBefore(LocalDateTime.now());
    }

    /**
     * Calculate duration in minutes
     */
    public Long getDurationMinutes() {
        if (actualStartDateTime != null && actualEndDateTime != null) {
            return java.time.Duration.between(actualStartDateTime, actualEndDateTime).toMinutes();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduledProcedureStep)) return false;
        ScheduledProcedureStep that = (ScheduledProcedureStep) o;
        return spsId != null && spsId.equals(that.spsId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ScheduledProcedureStep{" +
                "spsId=" + spsId +
                ", modality='" + modality + '\'' +
                ", scheduledStationAETitle='" + scheduledStationAETitle + '\'' +
                ", scheduledStartDateTime=" + scheduledStartDateTime +
                ", spsStatus=" + spsStatus +
                '}';
    }
}
