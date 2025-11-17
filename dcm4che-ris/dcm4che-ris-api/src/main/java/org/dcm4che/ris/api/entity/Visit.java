package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Visit/Admission/Encounter entity.
 * Represents a patient's visit to the hospital or clinic.
 * Based on HL7 v2.x ADT (Admit/Discharge/Transfer) messages.
 * Links imaging orders to a specific admission or outpatient encounter.
 *
 * HL7 ADT Messages:
 * - A01: Admit/visit notification
 * - A02: Transfer a patient
 * - A03: Discharge/end visit
 * - A08: Update patient information
 * - A11: Cancel admit/visit notification
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "visit", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"admission_id"})
}, indexes = {
    @Index(name = "idx_visit_patient", columnList = "patient_id"),
    @Index(name = "idx_visit_type", columnList = "visit_type"),
    @Index(name = "idx_visit_status", columnList = "visit_status"),
    @Index(name = "idx_visit_admission_date", columnList = "admission_date"),
    @Index(name = "idx_visit_attending", columnList = "attending_physician_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visit_id")
    private Long visitId;

    /**
     * Patient for this visit
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Admission ID / Visit Number (unique identifier)
     * HL7 PV1-19: Visit Number
     * Must be unique across all visits
     */
    @Size(max = 64)
    @Column(name = "admission_id", unique = true, length = 64)
    private String admissionId;

    /**
     * Visit type/class
     * HL7 PV1-2: Patient Class
     * Values: OUTPATIENT, INPATIENT, EMERGENCY, PREADMIT, RECURRING, OBSERVATION
     */
    @Size(max = 16)
    @Column(name = "visit_type", length = 16)
    @Builder.Default
    private String visitType = "OUTPATIENT";

    /**
     * Visit status
     * Values: ACTIVE, DISCHARGED, CANCELED, PENDING
     */
    @Size(max = 16)
    @Column(name = "visit_status", length = 16)
    @Builder.Default
    private String visitStatus = "ACTIVE";

    /**
     * Admission date/time
     * HL7 PV1-44: Admit Date/Time
     */
    @Column(name = "admission_date")
    private LocalDateTime admissionDate;

    /**
     * Discharge date/time
     * HL7 PV1-45: Discharge Date/Time
     */
    @Column(name = "discharge_date")
    private LocalDateTime dischargeDate;

    /**
     * Attending physician
     * HL7 PV1-7: Attending Doctor
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attending_physician_id")
    private Physician attendingPhysician;

    /**
     * Referring physician
     * HL7 PV1-8: Referring Doctor
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referring_physician_id")
    private Physician referringPhysician;

    /**
     * Consulting physician
     * HL7 PV1-9: Consulting Doctor
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulting_physician_id")
    private Physician consultingPhysician;

    /**
     * Hospital service/department
     * HL7 PV1-10: Hospital Service
     * E.g., CARDIOLOGY, NEUROLOGY, ORTHOPEDICS, GENERAL
     */
    @Size(max = 64)
    @Column(name = "hospital_service", length = 64)
    private String hospitalService;

    /**
     * Admission source
     * HL7 PV1-14: Admit Source
     * E.g., PHYSICIAN_REFERRAL, CLINIC_REFERRAL, EMERGENCY_ROOM, TRANSFER
     */
    @Size(max = 64)
    @Column(name = "admission_source", length = 64)
    private String admissionSource;

    /**
     * Admission type
     * HL7 PV1-4: Admission Type
     * Values: ELECTIVE, EMERGENCY, URGENT, NEWBORN, ROUTINE
     */
    @Size(max = 32)
    @Column(name = "admission_type", length = 32)
    private String admissionType;

    /**
     * Readmission indicator
     * HL7 PV1-13: Re-admission Indicator
     */
    @Column(name = "is_readmission")
    @Builder.Default
    private Boolean isReadmission = false;

    /**
     * Assigned patient location - Ward/Building
     * HL7 PV1-3: Assigned Patient Location - Point of Care
     */
    @Size(max = 64)
    @Column(name = "ward", length = 64)
    private String ward;

    /**
     * Room number
     * HL7 PV1-3: Assigned Patient Location - Room
     */
    @Size(max = 32)
    @Column(name = "room", length = 32)
    private String room;

    /**
     * Bed number
     * HL7 PV1-3: Assigned Patient Location - Bed
     */
    @Size(max = 32)
    @Column(name = "bed", length = 32)
    private String bed;

    /**
     * Facility/building
     * HL7 PV1-3: Assigned Patient Location - Facility
     */
    @Size(max = 64)
    @Column(name = "facility", length = 64)
    private String facility;

    /**
     * Prior patient location (for transfers)
     * HL7 PV1-6: Prior Patient Location
     */
    @Size(max = 128)
    @Column(name = "prior_location", length = 128)
    private String priorLocation;

    /**
     * Discharge disposition
     * HL7 PV1-36: Discharge Disposition
     * E.g., HOME, HOME_HEALTH_CARE, SKILLED_NURSING, EXPIRED, LEFT_AMA
     */
    @Size(max = 64)
    @Column(name = "discharge_disposition", length = 64)
    private String dischargeDisposition;

    /**
     * Discharge to location
     * HL7 PV1-37: Discharged to Location
     */
    @Size(max = 128)
    @Column(name = "discharge_to_location", length = 128)
    private String dischargeToLocation;

    /**
     * Visit diagnosis
     * Primary diagnosis for this visit
     */
    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    /**
     * Diagnosis code (ICD-10)
     */
    @Size(max = 32)
    @Column(name = "diagnosis_code", length = 32)
    private String diagnosisCode;

    /**
     * Financial class/Insurance class
     * HL7 PV1-20: Financial Class
     * E.g., INSURANCE, MEDICARE, MEDICAID, SELF_PAY, WORKER_COMP
     */
    @Size(max = 32)
    @Column(name = "financial_class", length = 32)
    private String financialClass;

    /**
     * VIP indicator
     * HL7 PV1-16: VIP Indicator
     */
    @Column(name = "is_vip")
    @Builder.Default
    private Boolean isVip = false;

    /**
     * Visit notes
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships

    /**
     * Imaging orders for this visit
     */
    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ImagingServiceRequest> orders = new ArrayList<>();

    // JPA Callbacks

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (admissionDate == null) {
            admissionDate = LocalDateTime.now();
        }
        if (visitType == null) {
            visitType = "OUTPATIENT";
        }
        if (visitStatus == null) {
            visitStatus = "ACTIVE";
        }
        if (isReadmission == null) {
            isReadmission = false;
        }
        if (isVip == null) {
            isVip = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Methods

    /**
     * Discharge this visit
     */
    public void discharge(String disposition, String toLocation) {
        this.visitStatus = "DISCHARGED";
        this.dischargeDate = LocalDateTime.now();
        this.dischargeDisposition = disposition;
        this.dischargeToLocation = toLocation;
    }

    /**
     * Cancel this visit
     */
    public void cancel() {
        this.visitStatus = "CANCELED";
    }

    /**
     * Transfer patient to new location
     */
    public void transferTo(String newWard, String newRoom, String newBed) {
        this.priorLocation = getFullLocation();
        this.ward = newWard;
        this.room = newRoom;
        this.bed = newBed;
    }

    /**
     * Get full patient location string
     */
    public String getFullLocation() {
        StringBuilder sb = new StringBuilder();
        if (ward != null) {
            sb.append(ward);
        }
        if (room != null) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append("Room ").append(room);
        }
        if (bed != null) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append("Bed ").append(bed);
        }
        return sb.length() > 0 ? sb.toString() : "Unknown";
    }

    /**
     * Check if visit is currently active
     */
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(visitStatus);
    }

    /**
     * Check if this is an inpatient visit
     */
    public boolean isInpatient() {
        return "INPATIENT".equalsIgnoreCase(visitType);
    }

    /**
     * Check if this is an emergency visit
     */
    public boolean isEmergency() {
        return "EMERGENCY".equalsIgnoreCase(visitType) ||
               "EMERGENCY".equalsIgnoreCase(admissionType);
    }

    /**
     * Get length of stay in days
     */
    public Long getLengthOfStay() {
        if (admissionDate == null) {
            return 0L;
        }
        LocalDateTime endDate = dischargeDate != null ? dischargeDate : LocalDateTime.now();
        return ChronoUnit.DAYS.between(admissionDate, endDate);
    }

    /**
     * Get length of stay in hours
     */
    public Long getLengthOfStayHours() {
        if (admissionDate == null) {
            return 0L;
        }
        LocalDateTime endDate = dischargeDate != null ? dischargeDate : LocalDateTime.now();
        return ChronoUnit.HOURS.between(admissionDate, endDate);
    }

    /**
     * Check if visit is discharged
     */
    public boolean isDischarged() {
        return "DISCHARGED".equalsIgnoreCase(visitStatus);
    }

    /**
     * Add imaging order to this visit
     */
    public void addOrder(ImagingServiceRequest order) {
        orders.add(order);
        order.setVisit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Visit)) return false;
        Visit visit = (Visit) o;
        return visitId != null && visitId.equals(visit.visitId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Visit{" +
                "visitId=" + visitId +
                ", admissionId='" + admissionId + '\'' +
                ", visitType='" + visitType + '\'' +
                ", visitStatus='" + visitStatus + '\'' +
                ", admissionDate=" + admissionDate +
                ", patient=" + (patient != null ? patient.getPatientId() : null) +
                '}';
    }
}

