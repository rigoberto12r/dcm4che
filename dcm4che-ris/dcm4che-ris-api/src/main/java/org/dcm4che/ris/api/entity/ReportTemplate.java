package org.dcm4che.ris.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Report Template entity.
 * Stores reusable templates for radiology reports.
 * Templates can be specific to modality, body part, or procedure.
 *
 * @author dcm4che-ris
 */
@Entity
@Table(name = "report_template", indexes = {
    @Index(name = "idx_template_modality", columnList = "modality"),
    @Index(name = "idx_template_body_part", columnList = "body_part"),
    @Index(name = "idx_template_active", columnList = "is_active"),
    @Index(name = "idx_template_default", columnList = "is_default")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    /**
     * Template name/title
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "template_name", nullable = false, length = 255)
    private String templateName;

    /**
     * Template category (e.g., "Chest CT", "Brain MRI", "Musculoskeletal")
     */
    @Size(max = 64)
    @Column(name = "template_category", length = 64)
    private String templateCategory;

    /**
     * Modality this template is for (CT, MR, CR, DX, US, etc.)
     * NULL means applicable to all modalities
     */
    @Size(max = 16)
    @Column(name = "modality", length = 16)
    private String modality;

    /**
     * Body part this template is for (CHEST, HEAD, ABDOMEN, etc.)
     * NULL means applicable to all body parts
     */
    @Size(max = 64)
    @Column(name = "body_part", length = 64)
    private String bodyPart;

    /**
     * Specific procedure code (optional)
     * Links to ProcedureCatalog
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_code_id")
    private ProcedureCatalog procedureCode;

    /**
     * Complete template text with placeholders
     * Placeholders use {variable} syntax, e.g., {patientName}, {age}, {indication}
     */
    @Column(name = "template_text", columnDefinition = "TEXT")
    private String templateText;

    /**
     * Template format (PLAIN_TEXT, HTML, MARKDOWN)
     */
    @Size(max = 16)
    @Column(name = "template_format", length = 16)
    @Builder.Default
    private String templateFormat = "PLAIN_TEXT";

    /**
     * Structured sections configuration (JSON)
     * Defines which sections are included: indication, technique, findings, etc.
     * Example: {"sections": ["indication", "technique", "comparison", "findings", "impression", "recommendations"]}
     */
    @Column(name = "sections", columnDefinition = "TEXT")
    private String sections;

    /**
     * Default content for Indication section
     */
    @Column(name = "default_indication", columnDefinition = "TEXT")
    private String defaultIndication;

    /**
     * Default content for Technique section
     */
    @Column(name = "default_technique", columnDefinition = "TEXT")
    private String defaultTechnique;

    /**
     * Default content for Comparison section
     */
    @Column(name = "default_comparison", columnDefinition = "TEXT")
    private String defaultComparison;

    /**
     * Default content for Findings section
     */
    @Column(name = "default_findings", columnDefinition = "TEXT")
    private String defaultFindings;

    /**
     * Default content for Impression section
     */
    @Column(name = "default_impression", columnDefinition = "TEXT")
    private String defaultImpression;

    /**
     * Default content for Recommendations section
     */
    @Column(name = "default_recommendations", columnDefinition = "TEXT")
    private String defaultRecommendations;

    /**
     * Macros/snippets available for this template (JSON)
     * Common phrases that can be inserted quickly
     * Example: {"macros": [{"key": "normal_chest", "text": "The lungs are clear. No pleural effusion or pneumothorax."}]}
     */
    @Column(name = "macros", columnDefinition = "TEXT")
    private String macros;

    /**
     * User who created this template
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    /**
     * Is this template active/available for use
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Is this the default template for this modality/body part combination
     * Only one template should be default per combination
     */
    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Usage counter - how many times this template has been used
     */
    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    /**
     * Last used date/time
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * Template description/notes
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Keywords for searching (comma-separated)
     */
    @Size(max = 255)
    @Column(name = "keywords", length = 255)
    private String keywords;

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
        if (isDefault == null) {
            isDefault = false;
        }
        if (usageCount == null) {
            usageCount = 0;
        }
        if (templateFormat == null) {
            templateFormat = "PLAIN_TEXT";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Methods

    /**
     * Increment usage counter
     */
    public void incrementUsage() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Replace placeholders in template text with actual values
     */
    public String fillTemplate(java.util.Map<String, String> values) {
        if (templateText == null || templateText.isEmpty()) {
            return "";
        }

        String result = templateText;
        for (java.util.Map.Entry<String, String> entry : values.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    /**
     * Build a complete report structure from this template
     */
    public String buildReportStructure() {
        StringBuilder sb = new StringBuilder();

        if (defaultIndication != null && !defaultIndication.trim().isEmpty()) {
            sb.append("INDICATION:\n").append(defaultIndication).append("\n\n");
        }

        if (defaultTechnique != null && !defaultTechnique.trim().isEmpty()) {
            sb.append("TECHNIQUE:\n").append(defaultTechnique).append("\n\n");
        }

        if (defaultComparison != null && !defaultComparison.trim().isEmpty()) {
            sb.append("COMPARISON:\n").append(defaultComparison).append("\n\n");
        }

        if (defaultFindings != null && !defaultFindings.trim().isEmpty()) {
            sb.append("FINDINGS:\n").append(defaultFindings).append("\n\n");
        }

        if (defaultImpression != null && !defaultImpression.trim().isEmpty()) {
            sb.append("IMPRESSION:\n").append(defaultImpression).append("\n\n");
        }

        if (defaultRecommendations != null && !defaultRecommendations.trim().isEmpty()) {
            sb.append("RECOMMENDATIONS:\n").append(defaultRecommendations).append("\n\n");
        }

        return sb.toString().trim();
    }

    /**
     * Check if this template matches the given criteria
     */
    public boolean matches(String modality, String bodyPart) {
        boolean modalityMatch = this.modality == null || this.modality.equalsIgnoreCase(modality);
        boolean bodyPartMatch = this.bodyPart == null || this.bodyPart.equalsIgnoreCase(bodyPart);
        return modalityMatch && bodyPartMatch && isActive;
    }

    /**
     * Create a deep copy of this template
     */
    public ReportTemplate copy(String newName, User user) {
        return ReportTemplate.builder()
            .templateName(newName)
            .templateCategory(this.templateCategory)
            .modality(this.modality)
            .bodyPart(this.bodyPart)
            .procedureCode(this.procedureCode)
            .templateText(this.templateText)
            .templateFormat(this.templateFormat)
            .sections(this.sections)
            .defaultIndication(this.defaultIndication)
            .defaultTechnique(this.defaultTechnique)
            .defaultComparison(this.defaultComparison)
            .defaultFindings(this.defaultFindings)
            .defaultImpression(this.defaultImpression)
            .defaultRecommendations(this.defaultRecommendations)
            .macros(this.macros)
            .createdByUser(user)
            .description("Copy of: " + this.templateName)
            .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReportTemplate)) return false;
        ReportTemplate that = (ReportTemplate) o;
        return templateId != null && templateId.equals(that.templateId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ReportTemplate{" +
                "templateId=" + templateId +
                ", templateName='" + templateName + '\'' +
                ", modality='" + modality + '\'' +
                ", bodyPart='" + bodyPart + '\'' +
                ", isActive=" + isActive +
                ", isDefault=" + isDefault +
                ", usageCount=" + usageCount +
                '}';
    }
}
