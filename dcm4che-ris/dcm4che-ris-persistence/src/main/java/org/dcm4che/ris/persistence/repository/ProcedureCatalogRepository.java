package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.ProcedureCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ProcedureCatalog entity.
 * Manages imaging procedure catalog and billing codes.
 *
 * @author dcm4che-ris
 */
@Repository
public interface ProcedureCatalogRepository extends JpaRepository<ProcedureCatalog, Long>,
                                                    JpaSpecificationExecutor<ProcedureCatalog> {

    /**
     * Find procedure by internal code
     */
    Optional<ProcedureCatalog> findByProcedureCode(String procedureCode);

    /**
     * Find procedure by CPT code
     */
    Optional<ProcedureCatalog> findByCptCode(String cptCode);

    /**
     * Find procedure by LOINC code
     */
    Optional<ProcedureCatalog> findByLoincCode(String loincCode);

    /**
     * Find procedure by SNOMED code
     */
    Optional<ProcedureCatalog> findBySnomedCode(String snomedCode);

    /**
     * Find procedure by RadLex code
     */
    Optional<ProcedureCatalog> findByRadlexCode(String radlexCode);

    /**
     * Check if procedure code exists
     */
    boolean existsByProcedureCode(String procedureCode);

    /**
     * Find all active procedures
     */
    List<ProcedureCatalog> findByIsActiveTrue();

    /**
     * Find procedures by modality
     */
    List<ProcedureCatalog> findByModalityAndIsActiveTrue(String modality);

    /**
     * Find procedures by body part
     */
    List<ProcedureCatalog> findByBodyPartAndIsActiveTrue(String bodyPart);

    /**
     * Find procedures by modality and body part
     */
    List<ProcedureCatalog> findByModalityAndBodyPartAndIsActiveTrue(String modality, String bodyPart);

    /**
     * Find procedures by category
     */
    List<ProcedureCatalog> findByProcedureCategoryAndIsActiveTrue(String procedureCategory);

    /**
     * Search procedures by name
     */
    @Query("SELECT p FROM ProcedureCatalog p WHERE LOWER(p.procedureName) LIKE LOWER(CONCAT('%', :name, '%')) AND p.isActive = true")
    List<ProcedureCatalog> searchByName(@Param("name") String name);

    /**
     * Search procedures by keyword
     */
    @Query("SELECT p FROM ProcedureCatalog p WHERE " +
           "LOWER(p.procedureName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.procedureDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.keywords) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND p.isActive = true")
    List<ProcedureCatalog> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Find procedures requiring contrast
     */
    @Query("SELECT p FROM ProcedureCatalog p WHERE p.contrastRequired = 'REQUIRED' AND p.isActive = true")
    List<ProcedureCatalog> findProceduresRequiringContrast();

    /**
     * Find procedures with optional contrast
     */
    @Query("SELECT p FROM ProcedureCatalog p WHERE p.contrastRequired = 'OPTIONAL' AND p.isActive = true")
    List<ProcedureCatalog> findProceduresWithOptionalContrast();

    /**
     * Find procedures requiring preparation
     */
    @Query("SELECT p FROM ProcedureCatalog p WHERE p.patientPreparation IS NOT NULL " +
           "AND p.patientPreparation <> '' AND p.isActive = true")
    List<ProcedureCatalog> findProceduresRequiringPreparation();

    /**
     * Find procedures requiring pre-authorization
     */
    @Query("SELECT p FROM ProcedureCatalog p WHERE p.requiresPreauth = true AND p.isActive = true")
    List<ProcedureCatalog> findProceduresRequiringPreauth();

    /**
     * Find procedures requiring physician present
     */
    @Query("SELECT p FROM ProcedureCatalog p WHERE p.requiresPhysicianPresent = true AND p.isActive = true")
    List<ProcedureCatalog> findProceduresRequiringPhysician();

    /**
     * Find high complexity procedures
     */
    @Query("SELECT p FROM ProcedureCatalog p WHERE p.complexityLevel >= 4 AND p.isActive = true")
    List<ProcedureCatalog> findHighComplexityProcedures();

    /**
     * Find procedures by complexity level
     */
    List<ProcedureCatalog> findByComplexityLevelAndIsActiveTrue(Integer complexityLevel);

    /**
     * Find procedures with dose reference levels (radiation procedures)
     */
    @Query("SELECT p FROM ProcedureCatalog p WHERE p.doseReferenceLevel IS NOT NULL AND p.isActive = true")
    List<ProcedureCatalog> findRadiationProcedures();

    /**
     * Count procedures by modality
     */
    long countByModalityAndIsActiveTrue(String modality);

    /**
     * Get all distinct modalities in catalog
     */
    @Query("SELECT DISTINCT p.modality FROM ProcedureCatalog p WHERE p.isActive = true ORDER BY p.modality")
    List<String> findDistinctModalities();

    /**
     * Get all distinct body parts in catalog
     */
    @Query("SELECT DISTINCT p.bodyPart FROM ProcedureCatalog p WHERE p.bodyPart IS NOT NULL AND p.isActive = true ORDER BY p.bodyPart")
    List<String> findDistinctBodyParts();

    /**
     * Get all distinct categories
     */
    @Query("SELECT DISTINCT p.procedureCategory FROM ProcedureCatalog p WHERE p.procedureCategory IS NOT NULL AND p.isActive = true ORDER BY p.procedureCategory")
    List<String> findDistinctCategories();

    /**
     * Find CT procedures
     */
    @Query("SELECT p FROM ProcedureCatalog p WHERE p.modality = 'CT' AND p.isActive = true")
    List<ProcedureCatalog> findCTProcedures();

    /**
     * Find MRI procedures
     */
    @Query("SELECT p FROM ProcedureCatalog p WHERE p.modality = 'MR' AND p.isActive = true")
    List<ProcedureCatalog> findMRIProcedures();

    /**
     * Find X-ray procedures
     */
    @Query("SELECT p FROM ProcedureCatalog p WHERE p.modality IN ('CR', 'DX') AND p.isActive = true")
    List<ProcedureCatalog> findXRayProcedures();
}
