package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Patient entity.
 * Provides CRUD operations and custom queries for patient management.
 *
 * @author dcm4che-ris
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    /**
     * Find patient by Medical Record Number (MRN)
     * Most common lookup method for patient identification
     */
    Optional<Patient> findByMrn(String mrn);

    /**
     * Find patient by MRN and issuer
     * For multi-facility environments where MRN may not be globally unique
     */
    Optional<Patient> findByMrnAndIssuerOfPatientId(String mrn, String issuerOfPatientId);

    /**
     * Check if patient exists by MRN
     */
    boolean existsByMrn(String mrn);

    /**
     * Find all active patients
     */
    List<Patient> findByIsActiveTrue();

    /**
     * Find patients by name (case-insensitive, partial match)
     * DICOM PatientName format: Last^First^Middle^Prefix^Suffix
     */
    @Query("SELECT p FROM Patient p WHERE LOWER(p.patientName) LIKE LOWER(CONCAT('%', :name, '%')) AND p.isActive = true")
    List<Patient> searchByName(@Param("name") String name);

    /**
     * Find patients by birth date
     */
    List<Patient> findByBirthDate(LocalDate birthDate);

    /**
     * Find patients by birth date range
     */
    @Query("SELECT p FROM Patient p WHERE p.birthDate BETWEEN :startDate AND :endDate AND p.isActive = true")
    List<Patient> findByBirthDateRange(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /**
     * Find patients by sex
     */
    List<Patient> findBySexAndIsActiveTrue(String sex);

    /**
     * Find patients by nationality
     */
    List<Patient> findByNationalityAndIsActiveTrue(String nationality);

    /**
     * Search patients by multiple criteria
     * Used for patient lookup/search functionality
     */
    @Query("SELECT p FROM Patient p WHERE " +
           "(:mrn IS NULL OR p.mrn = :mrn) AND " +
           "(:name IS NULL OR LOWER(p.patientName) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:birthDate IS NULL OR p.birthDate = :birthDate) AND " +
           "(:sex IS NULL OR p.sex = :sex) AND " +
           "p.isActive = true")
    List<Patient> searchPatients(@Param("mrn") String mrn,
                                 @Param("name") String name,
                                 @Param("birthDate") LocalDate birthDate,
                                 @Param("sex") String sex);

    /**
     * Find patients with allergies
     */
    @Query("SELECT p FROM Patient p WHERE p.allergies IS NOT NULL AND p.allergies <> '' AND p.isActive = true")
    List<Patient> findPatientsWithAllergies();

    /**
     * Find VIP patients
     */
    List<Patient> findByIsVipTrueAndIsActiveTrue();

    /**
     * Find pregnant patients (for radiation safety)
     */
    @Query("SELECT p FROM Patient p WHERE p.pregnancyStatus IN ('PREGNANT', 'POSSIBLY_PREGNANT') AND p.isActive = true")
    List<Patient> findPregnantPatients();

    /**
     * Count active patients
     */
    long countByIsActiveTrue();

    /**
     * Find patients created in date range
     */
    @Query("SELECT p FROM Patient p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Patient> findPatientsCreatedBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                             @Param("endDate") java.time.LocalDateTime endDate);
}
