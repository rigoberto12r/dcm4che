package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Procedure Catalog entity.
 * Catalog of available imaging procedures.
 * TODO: Complete implementation
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "procedure_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcedureCatalog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "procedure_code_id")
    private Long procedureCodeId;

    @Column(name = "procedure_code", unique = true, length = 64)
    private String procedureCode;

    @Column(name = "procedure_name", length = 255)
    private String procedureName;

    @Column(name = "procedure_description", columnDefinition = "TEXT")
    private String procedureDescription;

    @Column(name = "modality", length = 16)
    private String modality; // CT, MR, CR, DX, US, etc.

    @Column(name = "body_part", length = 64)
    private String bodyPart;

    @Column(name = "cpt_code", length = 16)
    private String cptCode;

    @Column(name = "loinc_code", length = 16)
    private String loincCode;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "contrast_required", length = 16)
    private String contrastRequired; // NO, OPTIONAL, REQUIRED

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

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
