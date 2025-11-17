package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.dcm4che.data.PersonName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Physician entity.
 * Represents referring physicians, radiologists, and other clinicians.
 * Based on DICOM Referring Physician (0008,0090) and Performing Physician (0008,1050).
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "physician", indexes = {
    @Index(name = "idx_physician_specialty", columnList = "specialty"),
    @Index(name = "idx_physician_active", columnList = "is_active"),
    @Index(name = "idx_physician_npi", columnList = "npi")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Physician implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "physician_id")
    private Long physicianId;

    /**
     * Physician's Name in DICOM PersonName format
     * Format: Last^First^Middle^Prefix^Suffix
     * (0008,0090) Referring Physician's Name
     * (0008,1050) Performing Physician's Name
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "physician_name", nullable = false, length = 255)
    private String physicianName;

    /**
     * Primary medical specialty
     * E.g., RADIOLOGY, CARDIOLOGY, NEUROLOGY, ORTHOPEDICS, etc.
     */
    @Size(max = 64)
    @Column(name = "specialty", length = 64)
    private String specialty;

    /**
     * Sub-specialty (for radiologists)
     * E.g., NEURORADIOLOGY, MUSCULOSKELETAL, CHEST, ABDOMINAL, etc.
     */
    @Size(max = 64)
    @Column(name = "sub_specialty", length = 64)
    private String subSpecialty;

    /**
     * Medical license number
     */
    @Size(max = 64)
    @Column(name = "license_number", length = 64)
    private String licenseNumber;

    /**
     * National Provider Identifier (NPI) - US specific
     * Used for billing and identification
     */
    @Size(max = 16)
    @Column(name = "npi", length = 16)
    private String npi;

    /**
     * DEA number (for controlled substance prescriptions) - US specific
     */
    @Size(max = 16)
    @Column(name = "dea_number", length = 16)
    private String deaNumber;

    /**
     * Phone number
     */
    @Size(max = 64)
    @Column(name = "phone", length = 64)
    private String phone;

    /**
     * Email address
     */
    @Email
    @Size(max = 255)
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Fax number
     */
    @Size(max = 64)
    @Column(name = "fax", length = 64)
    private String fax;

    /**
     * Office/practice address
     */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /**
     * Hospital/clinic affiliation
     */
    @Size(max = 255)
    @Column(name = "affiliation", length = 255)
    private String affiliation;

    /**
     * Department within institution
     */
    @Size(max = 64)
    @Column(name = "department", length = 64)
    private String department;

    /**
     * Signature image (base64 encoded PNG/JPEG)
     * Used for report signing
     */
    @Column(name = "signature_image", columnDefinition = "TEXT")
    private String signatureImage;

    /**
     * Digital signature certificate (X.509 certificate in PEM format)
     * For legally binding digital signatures
     */
    @Column(name = "digital_signature_cert", columnDefinition = "TEXT")
    private String digitalSignatureCert;

    /**
     * Physician type/role
     * Values: REFERRING, RADIOLOGIST, RESIDENT, FELLOW, ATTENDING, etc.
     */
    @Size(max = 32)
    @Column(name = "physician_type", length = 32)
    private String physicianType;

    /**
     * Is this physician a radiologist (can read/report studies)
     */
    @Column(name = "is_radiologist")
    @Builder.Default
    private Boolean isRadiologist = false;

    /**
     * Can this radiologist sign final reports
     */
    @Column(name = "can_sign_reports")
    @Builder.Default
    private Boolean canSignReports = false;

    /**
     * Preferred modalities (comma-separated)
     * E.g., "CT,MR" for radiologists
     */
    @Size(max = 128)
    @Column(name = "preferred_modalities", length = 128)
    private String preferredModalities;

    /**
     * Reading workload capacity (studies per day)
     */
    @Column(name = "daily_capacity")
    private Integer dailyCapacity;

    /**
     * Is physician currently active
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Notes about this physician
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // JPA Callbacks

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (isRadiologist == null) {
            isRadiologist = false;
        }
        if (canSignReports == null) {
            canSignReports = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Methods

    /**
     * Parse DICOM PersonName from physicianName field
     */
    public PersonName getPhysicianNameAsPersonName() {
        return new PersonName(physicianName);
    }

    /**
     * Set physician name from DICOM PersonName
     */
    public void setPhysicianNameFromPersonName(PersonName pn) {
        this.physicianName = pn.toString();
    }

    /**
     * Get display name (First Last)
     */
    public String getDisplayName() {
        PersonName pn = new PersonName(physicianName);
        String given = pn.get(PersonName.Component.GivenName);
        String family = pn.get(PersonName.Component.FamilyName);

        if (given != null && family != null) {
            return given + " " + family;
        } else if (family != null) {
            return family;
        } else if (given != null) {
            return given;
        }
        return physicianName;
    }

    /**
     * Get formal name with title (Dr. Last, First)
     */
    public String getFormalName() {
        PersonName pn = new PersonName(physicianName);
        String given = pn.get(PersonName.Component.GivenName);
        String family = pn.get(PersonName.Component.FamilyName);
        String prefix = pn.get(PersonName.Component.NamePrefix);

        StringBuilder sb = new StringBuilder();
        if (prefix != null && !prefix.isEmpty()) {
            sb.append(prefix).append(" ");
        } else {
            sb.append("Dr. ");
        }

        if (family != null) {
            sb.append(family);
            if (given != null) {
                sb.append(", ").append(given);
            }
        } else {
            sb.append(physicianName);
        }

        return sb.toString();
    }

    /**
     * Check if physician can read studies of given modality
     */
    public boolean canRead(String modality) {
        if (!isRadiologist || !isActive) {
            return false;
        }

        if (preferredModalities == null || preferredModalities.isEmpty()) {
            return true; // Can read all modalities
        }

        String[] modalities = preferredModalities.split(",");
        for (String m : modalities) {
            if (m.trim().equalsIgnoreCase(modality)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Physician)) return false;
        Physician physician = (Physician) o;
        return physicianId != null && physicianId.equals(physician.physicianId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Physician{" +
                "physicianId=" + physicianId +
                ", physicianName='" + physicianName + '\'' +
                ", specialty='" + specialty + '\'' +
                ", npi='" + npi + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}

