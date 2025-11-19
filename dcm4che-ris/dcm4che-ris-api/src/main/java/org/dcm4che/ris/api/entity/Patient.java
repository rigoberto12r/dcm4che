package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.dcm4che.data.PersonName;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Patient entity representing patient demographics.
 * Based on DICOM Patient Module (PS 3.3 C.7.1.1) and HL7 PID segment.
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "patient", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"mrn", "issuer_of_patient_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long patientId;

    /**
     * Medical Record Number (0010,0020)
     * HL7: PID-3
     */
    @NotBlank
    @Size(max = 64)
    @Column(name = "mrn", nullable = false, length = 64)
    private String mrn;

    /**
     * Issuer of Patient ID (0010,0021)
     * HL7: PID-3.4
     */
    @Size(max = 64)
    @Column(name = "issuer_of_patient_id", length = 64)
    private String issuerOfPatientId;

    /**
     * Patient's Name (0010,0010)
     * Format: Last^First^Middle^Prefix^Suffix
     * HL7: PID-5
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "patient_name", nullable = false, length = 255)
    private String patientName;

    /**
     * Patient's Birth Date (0010,0030)
     * HL7: PID-7
     */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /**
     * Patient's Sex (0010,0040)
     * Values: M (Male), F (Female), O (Other)
     * HL7: PID-8
     */
    @Size(max = 1)
    @Column(name = "sex", length = 1)
    private String sex;

    /**
     * Patient's Address
     * HL7: PID-11
     */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /**
     * Patient's Phone Numbers
     * HL7: PID-13
     */
    @Size(max = 255)
    @Column(name = "phone_numbers", length = 255)
    private String phoneNumbers;

    /**
     * Patient's Email
     * HL7: PID-13
     */
    @Email
    @Size(max = 255)
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Responsible Person (0010,2297)
     * For pediatric or veterinary cases
     * HL7: PID-16
     */
    @Size(max = 255)
    @Column(name = "responsible_person", length = 255)
    private String responsiblePerson;

    /**
     * Patient Species Description (0010,2201)
     * For veterinary radiology (e.g., "CANINE", "FELINE")
     */
    @Size(max = 64)
    @Column(name = "patient_species", length = 64)
    private String patientSpecies;

    /**
     * Patient Breed Description (0010,2292)
     * For veterinary radiology
     */
    @Size(max = 64)
    @Column(name = "patient_breed", length = 64)
    private String patientBreed;

    /**
     * Known allergies
     * HL7: AL1 segment
     */
    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    /**
     * Medical alerts/warnings
     */
    @Column(name = "medical_alerts", columnDefinition = "TEXT")
    private String medicalAlerts;

    /**
     * Pregnancy Status (0010,21C0)
     * Values: NOT_PREGNANT, POSSIBLY_PREGNANT, DEFINITELY_PREGNANT, UNKNOWN
     */
    @Size(max = 16)
    @Column(name = "pregnancy_status", length = 16)
    private String pregnancyStatus;

    /**
     * Patient State
     * Values: ACTIVE, MERGED, INACTIVE
     */
    @NotBlank
    @Size(max = 16)
    @Column(name = "patient_state", nullable = false, length = 16)
    @Builder.Default
    private String patientState = "ACTIVE";

    /**
     * If this patient was merged, reference to the patient it was merged with
     */
    @Column(name = "merged_with_patient_id")
    private Long mergedWithPatientId;

    /**
     * Creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships

    /**
     * Patient visits/admissions
     */
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Visit> visits = new ArrayList<>();

    /**
     * Imaging service requests (orders) for this patient
     */
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ImagingServiceRequest> orders = new ArrayList<>();

    /**
     * DICOM studies for this patient
     */
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Study> studies = new ArrayList<>();

    // JPA Callbacks

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (patientState == null) {
            patientState = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Methods

    /**
     * Parse DICOM PersonName from patientName field
     */
    public PersonName getPatientNameAsPersonName() {
        return new PersonName(patientName);
    }

    /**
     * Set patient name from DICOM PersonName
     */
    public void setPatientNameFromPersonName(PersonName pn) {
        this.patientName = pn.toString();
    }

    /**
     * Get patient's age in years
     */
    public Integer getAge() {
        if (birthDate == null) {
            return null;
        }
        return java.time.Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Check if patient is active
     */
    public boolean isActive() {
        return "ACTIVE".equals(patientState);
    }

    /**
     * Check if patient was merged
     */
    public boolean isMerged() {
        return "MERGED".equals(patientState);
    }

    /**
     * Merge this patient with another
     */
    public void mergeWith(Long targetPatientId) {
        this.patientState = "MERGED";
        this.mergedWithPatientId = targetPatientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient)) return false;
        Patient patient = (Patient) o;
        return patientId != null && patientId.equals(patient.patientId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Patient{" +
                "patientId=" + patientId +
                ", mrn='" + mrn + '\'' +
                ", patientName='" + patientName + '\'' +
                ", birthDate=" + birthDate +
                ", sex='" + sex + '\'' +
                '}';
    }
}
