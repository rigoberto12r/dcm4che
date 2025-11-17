package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ReportTemplate entity.
 * Manages radiology report templates.
 *
 * @author dcm4che-ris
 */
@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long>,
                                                  JpaSpecificationExecutor<ReportTemplate> {

    /**
     * Find template by name
     */
    Optional<ReportTemplate> findByTemplateName(String templateName);

    /**
     * Find all active templates
     */
    List<ReportTemplate> findByIsActiveTrue();

    /**
     * Find templates by modality
     */
    List<ReportTemplate> findByModalityAndIsActiveTrue(String modality);

    /**
     * Find templates by body part
     */
    List<ReportTemplate> findByBodyPartAndIsActiveTrue(String bodyPart);

    /**
     * Find templates by modality and body part
     */
    List<ReportTemplate> findByModalityAndBodyPartAndIsActiveTrue(String modality, String bodyPart);

    /**
     * Find templates by category
     */
    List<ReportTemplate> findByTemplateCategoryAndIsActiveTrue(String templateCategory);

    /**
     * Find default template for modality and body part
     */
    @Query("SELECT t FROM ReportTemplate t WHERE t.modality = :modality " +
           "AND t.bodyPart = :bodyPart " +
           "AND t.isDefault = true " +
           "AND t.isActive = true")
    Optional<ReportTemplate> findDefaultTemplate(@Param("modality") String modality,
                                                  @Param("bodyPart") String bodyPart);

    /**
     * Find all default templates
     */
    @Query("SELECT t FROM ReportTemplate t WHERE t.isDefault = true AND t.isActive = true")
    List<ReportTemplate> findDefaultTemplates();

    /**
     * Find templates by procedure code
     */
    @Query("SELECT t FROM ReportTemplate t WHERE t.procedureCode.procedureCodeId = :procedureCodeId AND t.isActive = true")
    List<ReportTemplate> findByProcedureCode(@Param("procedureCodeId") Long procedureCodeId);

    /**
     * Find templates created by user
     */
    @Query("SELECT t FROM ReportTemplate t WHERE t.createdByUser.userId = :userId ORDER BY t.createdAt DESC")
    List<ReportTemplate> findByCreatedByUser(@Param("userId") Long userId);

    /**
     * Search templates by name or keywords
     */
    @Query("SELECT t FROM ReportTemplate t WHERE " +
           "(LOWER(t.templateName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.keywords) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND t.isActive = true")
    List<ReportTemplate> searchTemplates(@Param("search") String search);

    /**
     * Find most used templates
     */
    @Query("SELECT t FROM ReportTemplate t WHERE t.isActive = true ORDER BY t.usageCount DESC")
    List<ReportTemplate> findMostUsedTemplates();

    /**
     * Find recently used templates
     */
    @Query("SELECT t FROM ReportTemplate t WHERE t.lastUsedAt IS NOT NULL " +
           "AND t.isActive = true ORDER BY t.lastUsedAt DESC")
    List<ReportTemplate> findRecentlyUsedTemplates();

    /**
     * Find templates by format
     */
    List<ReportTemplate> findByTemplateFormatAndIsActiveTrue(String templateFormat);

    /**
     * Count templates by modality
     */
    long countByModalityAndIsActiveTrue(String modality);

    /**
     * Get all distinct modalities in templates
     */
    @Query("SELECT DISTINCT t.modality FROM ReportTemplate t WHERE t.modality IS NOT NULL AND t.isActive = true ORDER BY t.modality")
    List<String> findDistinctModalities();

    /**
     * Get all distinct body parts in templates
     */
    @Query("SELECT DISTINCT t.bodyPart FROM ReportTemplate t WHERE t.bodyPart IS NOT NULL AND t.isActive = true ORDER BY t.bodyPart")
    List<String> findDistinctBodyParts();

    /**
     * Get all distinct categories
     */
    @Query("SELECT DISTINCT t.templateCategory FROM ReportTemplate t WHERE t.templateCategory IS NOT NULL AND t.isActive = true ORDER BY t.templateCategory")
    List<String> findDistinctCategories();
}
