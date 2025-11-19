package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.Modality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Modality entity.
 * Manages DICOM modality/device configurations.
 *
 * @author dcm4che-ris
 */
@Repository
public interface ModalityRepository extends JpaRepository<Modality, Long>, JpaSpecificationExecutor<Modality> {

    /**
     * Find modality by AE Title (primary identifier for DICOM)
     */
    Optional<Modality> findByAeTitle(String aeTitle);

    /**
     * Check if AE Title exists
     */
    boolean existsByAeTitle(String aeTitle);

    /**
     * Find all active modalities
     */
    List<Modality> findByIsActiveTrue();

    /**
     * Find modalities by type
     */
    List<Modality> findByModalityTypeAndIsActiveTrue(String modalityType);

    /**
     * Find modalities by location
     */
    List<Modality> findByLocationAndIsActiveTrue(String location);

    /**
     * Find modalities by department
     */
    List<Modality> findByDepartmentAndIsActiveTrue(String department);

    /**
     * Find modalities that support MWL (Modality Worklist)
     */
    @Query("SELECT m FROM Modality m WHERE m.supportsMWL = true AND m.isActive = true")
    List<Modality> findModalitiesSupportingMWL();

    /**
     * Find modalities that support MPPS
     */
    @Query("SELECT m FROM Modality m WHERE m.supportsMPPS = true AND m.isActive = true")
    List<Modality> findModalitiesSupportingMPPS();

    /**
     * Find modalities that support Storage
     */
    @Query("SELECT m FROM Modality m WHERE m.supportsStorage = true AND m.isActive = true")
    List<Modality> findModalitiesSupportingStorage();

    /**
     * Find modalities that support Storage Commitment
     */
    @Query("SELECT m FROM Modality m WHERE m.supportsStorageCommitment = true AND m.isActive = true")
    List<Modality> findModalitiesSupportingStorageCommitment();

    /**
     * Find modalities by manufacturer
     */
    List<Modality> findByManufacturerAndIsActiveTrue(String manufacturer);

    /**
     * Find modalities by model
     */
    List<Modality> findByModelNameAndIsActiveTrue(String modelName);

    /**
     * Search modalities by station name
     */
    @Query("SELECT m FROM Modality m WHERE LOWER(m.stationName) LIKE LOWER(CONCAT('%', :name, '%')) AND m.isActive = true")
    List<Modality> searchByStationName(@Param("name") String name);

    /**
     * Find modalities by IP address
     */
    Optional<Modality> findByIpAddress(String ipAddress);

    /**
     * Count modalities by type
     */
    long countByModalityTypeAndIsActiveTrue(String modalityType);

    /**
     * Count all active modalities
     */
    long countByIsActiveTrue();

    /**
     * Find modalities requiring service (based on last service date)
     */
    @Query("SELECT m FROM Modality m WHERE m.lastServiceDate < :thresholdDate AND m.isActive = true")
    List<Modality> findModalitiesRequiringService(@Param("thresholdDate") java.time.LocalDateTime thresholdDate);

    /**
     * Find all CT scanners
     */
    @Query("SELECT m FROM Modality m WHERE m.modalityType = 'CT' AND m.isActive = true")
    List<Modality> findCTScanners();

    /**
     * Find all MRI scanners
     */
    @Query("SELECT m FROM Modality m WHERE m.modalityType = 'MR' AND m.isActive = true")
    List<Modality> findMRIScanners();

    /**
     * Find all X-ray systems
     */
    @Query("SELECT m FROM Modality m WHERE m.modalityType IN ('CR', 'DX') AND m.isActive = true")
    List<Modality> findXRaySystems();

    /**
     * Find all ultrasound systems
     */
    @Query("SELECT m FROM Modality m WHERE m.modalityType = 'US' AND m.isActive = true")
    List<Modality> findUltrasoundSystems();
}
