package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Modality entity.
 * Represents a DICOM modality/device configuration.
 * TODO: Complete implementation
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "modality")
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

    @Column(name = "ae_title", unique = true, length = 16)
    private String aeTitle;

    @Column(name = "modality_type", length = 16)
    private String modalityType; // CT, MR, CR, DX, US, etc.

    @Column(name = "station_name", length = 64)
    private String stationName;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "dicom_port")
    private Integer dicomPort;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "supports_mwl")
    @Builder.Default
    private Boolean supportsMWL = false;

    @Column(name = "supports_mpps")
    @Builder.Default
    private Boolean supportsMPPS = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
