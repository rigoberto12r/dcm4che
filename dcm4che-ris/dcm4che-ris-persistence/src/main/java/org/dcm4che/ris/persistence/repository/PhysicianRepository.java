package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.Physician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Physician entity.
 * Manages radiologists, referring physicians, and other clinicians.
 *
 * @author dcm4che-ris
 */
@Repository
public interface PhysicianRepository extends JpaRepository<Physician, Long>, JpaSpecificationExecutor<Physician> {

    /**
     * Find physician by NPI (National Provider Identifier)
     */
    Optional<Physician> findByNpi(String npi);

    /**
     * Find physician by license number
     */
    Optional<Physician> findByLicenseNumber(String licenseNumber);

    /**
     * Find physician by name (exact match)
     */
    Optional<Physician> findByPhysicianName(String physicianName);

    /**
     * Search physicians by name (partial, case-insensitive)
     */
    @Query("SELECT p FROM Physician p WHERE LOWER(p.physicianName) LIKE LOWER(CONCAT('%', :name, '%')) AND p.isActive = true")
    List<Physician> searchByName(@Param("name") String name);

    /**
     * Find all active physicians
     */
    List<Physician> findByIsActiveTrue();

    /**
     * Find all radiologists
     */
    @Query("SELECT p FROM Physician p WHERE p.isRadiologist = true AND p.isActive = true")
    List<Physician> findRadiologists();

    /**
     * Find radiologists who can sign reports
     */
    @Query("SELECT p FROM Physician p WHERE p.isRadiologist = true AND p.canSignReports = true AND p.isActive = true")
    List<Physician> findSigningRadiologists();

    /**
     * Find physicians by specialty
     */
    List<Physician> findBySpecialtyAndIsActiveTrue(String specialty);

    /**
     * Find physicians by sub-specialty
     */
    List<Physician> findBySubSpecialtyAndIsActiveTrue(String subSpecialty);

    /**
     * Find physicians by type (REFERRING, RADIOLOGIST, etc.)
     */
    List<Physician> findByPhysicianTypeAndIsActiveTrue(String physicianType);

    /**
     * Find radiologists by preferred modality
     */
    @Query("SELECT p FROM Physician p WHERE p.isRadiologist = true " +
           "AND p.preferredModalities LIKE %:modality% " +
           "AND p.isActive = true")
    List<Physician> findRadiologistsByModality(@Param("modality") String modality);

    /**
     * Find physicians by affiliation/hospital
     */
    List<Physician> findByAffiliationAndIsActiveTrue(String affiliation);

    /**
     * Find physicians by department
     */
    List<Physician> findByDepartmentAndIsActiveTrue(String department);

    /**
     * Find physicians with daily capacity available
     */
    @Query("SELECT p FROM Physician p WHERE p.isRadiologist = true " +
           "AND p.dailyCapacity IS NOT NULL " +
           "AND p.dailyCapacity > 0 " +
           "AND p.isActive = true")
    List<Physician> findRadiologistsWithCapacity();

    /**
     * Count radiologists by specialty
     */
    @Query("SELECT COUNT(p) FROM Physician p WHERE p.specialty = :specialty " +
           "AND p.isRadiologist = true " +
           "AND p.isActive = true")
    long countRadiologistsBySpecialty(@Param("specialty") String specialty);

    /**
     * Find referring physicians (non-radiologists)
     */
    @Query("SELECT p FROM Physician p WHERE p.isRadiologist = false AND p.isActive = true")
    List<Physician> findReferringPhysicians();
}
