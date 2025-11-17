package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Procedure Catalog entity.
 * Master catalog of available imaging procedures with billing codes.
 * Used for procedure ordering, scheduling, and billing.
 * Based on DICOM Requested Procedure Code (0032,1064) and Scheduled Protocol Code (0040,0008).
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "procedure_catalog", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"procedure_code"})
}, indexes = {
    @Index(name = "idx_procedure_modality", columnList = "modality"),
    @Index(name = "idx_procedure_body_part", columnList = "body_part"),
    @Index(name = "idx_procedure_active", columnList = "is_active"),
    @Index(name = "idx_procedure_cpt", columnList = "cpt_code"),
    @Index(name = "idx_procedure_loinc", columnList = "loinc_code")
})
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

    /**
     * Internal procedure code (institutional)
     * DICOM: (0032,1064) Requested Procedure Code Sequence > Code Value
     * Must be unique
     */
    @NotBlank
    @Size(max = 64)
    @Column(name = "procedure_code", nullable = false, unique = true, length = 64)
    private String procedureCode;

    /**
     * Procedure name/title
     * DICOM: (0032,1064) Requested Procedure Code Sequence > Code Meaning
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "procedure_name", nullable = false, length = 255)
    private String procedureName;

    /**
     * Detailed procedure description
     */
    @Column(name = "procedure_description", columnDefinition = "TEXT")
    private String procedureDescription;

    /**
     * Primary modality type
     * Values: CT, MR, CR, DX, XA, RF, US, NM, PT, MG, ES, OT, etc.
     * DICOM: (0008,0060) Modality
     */
    @NotBlank
    @Size(max = 16)
    @Column(name = "modality", nullable = false, length = 16)
    private String modality;

    /**
     * Primary body part examined
     * DICOM: (0018,0015) Body Part Examined
     * Values: CHEST, ABDOMEN, PELVIS, HEAD, NECK, SPINE, EXTREMITY, etc.
     */
    @Size(max = 64)
    @Column(name = "body_part", length = 64)
    private String bodyPart;

    /**
     * CPT Code (Current Procedural Terminology) - US billing
     * E.g., "71020" for Chest X-ray, 2 views
     */
    @Size(max = 16)
    @Column(name = "cpt_code", length = 16)
    private String cptCode;

    /**
     * LOINC Code (Logical Observation Identifiers Names and Codes)
     * E.g., "30746-2" for CT Chest with contrast
     */
    @Size(max = 16)
    @Column(name = "loinc_code", length = 16)
    private String loincCode;

    /**
     * SNOMED CT Code
     * Systematized Nomenclature of Medicine - Clinical Terms
     * E.g., "241541005" for High resolution CT of chest
     */
    @Size(max = 32)
    @Column(name = "snomed_code", length = 32)
    private String snomedCode;

    /**
     * RadLex Code (Radiology Lexicon)
     * E.g., "RID10321" for CT angiography of head
     */
    @Size(max = 32)
    @Column(name = "radlex_code", length = 32)
    private String radlexCode;

    /**
     * Estimated procedure duration in minutes
     * Used for scheduling
     */
    @Positive
    @Column(name = "estimated_duration_minutes")
    @Builder.Default
    private Integer estimatedDurationMinutes = 30;

    /**
     * Contrast agent requirement
     * Values: NO, OPTIONAL, REQUIRED
     * DICOM: (0018,0010) Contrast/Bolus Agent
     */
    @Size(max = 16)
    @Column(name = "contrast_required", length = 16)
    @Builder.Default
    private String contrastRequired = "NO";

    /**
     * Specific contrast agent recommended
     * E.g., "Iodine-based", "Gadolinium", "Barium"
     */
    @Size(max = 64)
    @Column(name = "contrast_agent", length = 64)
    private String contrastAgent;

    /**
     * Patient preparation instructions
     * E.g., "NPO 4 hours before exam", "Drink 1L water before scan"
     */
    @Column(name = "patient_preparation", columnDefinition = "TEXT")
    private String patientPreparation;

    /**
     * Special instructions for technologist
     */
    @Column(name = "technologist_instructions", columnDefinition = "TEXT")
    private String technologistInstructions;

    /**
     * Estimated radiation dose (for CT/XR/Fluoro)
     * Dose Reference Level in mGy
     */
    @Column(name = "dose_reference_level")
    private Double doseReferenceLevel;

    /**
     * Procedure price/cost
     */
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Insurance reimbursement amount
     */
    @Column(name = "reimbursement_amount", precision = 10, scale = 2)
    private BigDecimal reimbursementAmount;

    /**
     * Procedure category/group
     * E.g., "CARDIAC", "NEURO", "MUSCULOSKELETAL", "INTERVENTIONAL"
     */
    @Size(max = 64)
    @Column(name = "procedure_category", length = 64)
    private String procedureCategory;

    /**
     * Procedure complexity level (1-5)
     * Used for scheduling and radiologist assignment
     */
    @Column(name = "complexity_level")
    private Integer complexityLevel;

    /**
     * Requires physician present during procedure
     */
    @Column(name = "requires_physician_present")
    @Builder.Default
    private Boolean requiresPhysicianPresent = false;

    /**
     * Pre-authorization required from insurance
     */
    @Column(name = "requires_preauth")
    @Builder.Default
    private Boolean requiresPreauth = false;

    /**
     * Procedure is active/available for ordering
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Default report template for this procedure
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_template_id")
    private ReportTemplate defaultTemplate;

    /**
     * Keywords for searching (comma-separated)
     */
    @Size(max = 255)
    @Column(name = "keywords", length = 255)
    private String keywords;

    /**
     * Notes about this procedure
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
        if (estimatedDurationMinutes == null) {
            estimatedDurationMinutes = 30;
        }
        if (contrastRequired == null) {
            contrastRequired = "NO";
        }
        if (requiresPhysicianPresent == null) {
            requiresPhysicianPresent = false;
        }
        if (requiresPreauth == null) {
            requiresPreauth = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Methods

    /**
     * Check if this procedure requires contrast
     */
    public boolean requiresContrast() {
        return "REQUIRED".equalsIgnoreCase(contrastRequired);
    }

    /**
     * Check if contrast is optional for this procedure
     */
    public boolean hasOptionalContrast() {
        return "OPTIONAL".equalsIgnoreCase(contrastRequired);
    }

    /**
     * Check if this procedure involves radiation
     */
    public boolean involvesRadiation() {
        if (modality == null) {
            return false;
        }
        String mod = modality.toUpperCase();
        return mod.equals("CT") || mod.equals("CR") || mod.equals("DX") ||
               mod.equals("XA") || mod.equals("RF") || mod.equals("NM") ||
               mod.equals("PT") || mod.equals("MG");
    }

    /**
     * Check if procedure matches search criteria
     */
    public boolean matches(String modality, String bodyPart) {
        boolean modalityMatch = this.modality == null ||
                                this.modality.equalsIgnoreCase(modality);
        boolean bodyPartMatch = bodyPart == null ||
                               this.bodyPart == null ||
                               this.bodyPart.equalsIgnoreCase(bodyPart);
        return modalityMatch && bodyPartMatch && Boolean.TRUE.equals(isActive);
    }

    /**
     * Get estimated cost (price or reimbursement)
     */
    public BigDecimal getEstimatedCost() {
        if (price != null) {
            return price;
        }
        if (reimbursementAmount != null) {
            return reimbursementAmount;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Build full display name with modality
     */
    public String getFullDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (modality != null) {
            sb.append(modality).append(" ");
        }
        sb.append(procedureName);
        if (bodyPart != null) {
            sb.append(" (").append(bodyPart).append(")");
        }
        return sb.toString();
    }

    /**
     * Check if this procedure requires special preparations
     */
    public boolean requiresPreparation() {
        return patientPreparation != null && !patientPreparation.trim().isEmpty();
    }

    /**
     * Check if this is a high-complexity procedure
     */
    public boolean isHighComplexity() {
        return complexityLevel != null && complexityLevel >= 4;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcedureCatalog)) return false;
        ProcedureCatalog that = (ProcedureCatalog) o;
        return procedureCodeId != null && procedureCodeId.equals(that.procedureCodeId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ProcedureCatalog{" +
                "procedureCodeId=" + procedureCodeId +
                ", procedureCode='" + procedureCode + '\'' +
                ", procedureName='" + procedureName + '\'' +
                ", modality='" + modality + '\'' +
                ", bodyPart='" + bodyPart + '\'' +
                ", cptCode='" + cptCode + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
