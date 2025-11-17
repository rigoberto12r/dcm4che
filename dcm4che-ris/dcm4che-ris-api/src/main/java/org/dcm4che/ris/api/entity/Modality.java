package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Modality entity.
 * Represents a DICOM modality/device configuration.
 * Used for DICOM networking (MWL, MPPS, Storage).
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "modality", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ae_title"})
}, indexes = {
    @Index(name = "idx_modality_type", columnList = "modality_type"),
    @Index(name = "idx_modality_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modality implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "modality_id")
    private Long modalityId;

    /**
     * DICOM Application Entity Title
     * Must be unique, max 16 characters
     */
    @NotBlank
    @Size(max = 16)
    @Column(name = "ae_title", nullable = false, unique = true, length = 16)
    private String aeTitle;

    /**
     * Modality type
     * Values: CT, MR, CR, DX, XA, RF, US, NM, PT, MG, ES, OT, etc.
     */
    @NotBlank
    @Size(max = 16)
    @Column(name = "modality_type", nullable = false, length = 16)
    private String modalityType;

    /**
     * Manufacturer name
     */
    @Size(max = 64)
    @Column(name = "manufacturer", length = 64)
    private String manufacturer;

    /**
     * Manufacturer model name
     */
    @Size(max = 64)
    @Column(name = "model_name", length = 64)
    private String modelName;

    /**
     * Station name (human-readable)
     */
    @Size(max = 64)
    @Column(name = "station_name", length = 64)
    private String stationName;

    /**
     * IP address or hostname
     */
    @Size(max = 64)
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /**
     * DICOM port (default 104)
     */
    @Column(name = "dicom_port")
    @Builder.Default
    private Integer dicomPort = 104;

    /**
     * Description
     */
    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Physical location
     */
    @Size(max = 255)
    @Column(name = "location", length = 255)
    private String location;

    /**
     * Department owning this modality
     */
    @Size(max = 64)
    @Column(name = "department", length = 64)
    private String department;

    /**
     * Is this modality active/available
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Supports DICOM Modality Worklist (C-FIND SCP)
     */
    @Column(name = "supports_mwl")
    @Builder.Default
    private Boolean supportsMWL = false;

    /**
     * Supports DICOM MPPS (N-CREATE/N-SET SCU)
     */
    @Column(name = "supports_mpps")
    @Builder.Default
    private Boolean supportsMPPS = false;

    /**
     * Supports Storage Commitment
     */
    @Column(name = "supports_storage_commitment")
    @Builder.Default
    private Boolean supportsStorageCommitment = false;

    /**
     * Supports DICOM Storage (C-STORE SCU)
     */
    @Column(name = "supports_storage")
    @Builder.Default
    private Boolean supportsStorage = true;

    /**
     * Default imaging protocol for this modality
     */
    @Size(max = 64)
    @Column(name = "default_protocol", length = 64)
    private String defaultProtocol;

    /**
     * Serial number
     */
    @Size(max = 64)
    @Column(name = "serial_number", length = 64)
    private String serialNumber;

    /**
     * Installation date
     */
    @Column(name = "installation_date")
    private LocalDateTime installationDate;

    /**
     * Last service/maintenance date
     */
    @Column(name = "last_service_date")
    private LocalDateTime lastServiceDate;

    /**
     * Notes
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
        if (supportsMWL == null) {
            supportsMWL = false;
        }
        if (supportsMPPS == null) {
            supportsMPPS = false;
        }
        if (supportsStorageCommitment == null) {
            supportsStorageCommitment = false;
        }
        if (supportsStorage == null) {
            supportsStorage = true;
        }
        if (dicomPort == null) {
            dicomPort = 104;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Methods

    /**
     * Check if modality supports a given DICOM service
     */
    public boolean supportsService(String service) {
        switch (service.toUpperCase()) {
            case "MWL":
            case "WORKLIST":
                return Boolean.TRUE.equals(supportsMWL);
            case "MPPS":
                return Boolean.TRUE.equals(supportsMPPS);
            case "STORAGE":
            case "C-STORE":
                return Boolean.TRUE.equals(supportsStorage);
            case "STORAGE_COMMITMENT":
                return Boolean.TRUE.equals(supportsStorageCommitment);
            default:
                return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Modality)) return false;
        Modality modality = (Modality) o;
        return modalityId != null && modalityId.equals(modality.modalityId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Modality{" +
                "modalityId=" + modalityId +
                ", aeTitle='" + aeTitle + '\'' +
                ", modalityType='" + modalityType + '\'' +
                ", stationName='" + stationName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}

