package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.RequestedProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for RequestedProcedure entity.
 * Manages requested imaging procedures within orders.
 *
 * @author dcm4che-ris
 */
@Repository
public interface RequestedProcedureRepository extends JpaRepository<RequestedProcedure, Long>,
                                                      JpaSpecificationExecutor<RequestedProcedure> {

    /**
     * Find requested procedure by accession number
     */
    Optional<RequestedProcedure> findByAccessionNumber(String accessionNumber);

    /**
     * Find requested procedure by Study Instance UID
     */
    Optional<RequestedProcedure> findByStudyInstanceUID(String studyInstanceUID);

    /**
     * Check if accession number exists
     */
    boolean existsByAccessionNumber(String accessionNumber);

    /**
     * Find requested procedures by order
     */
    @Query("SELECT rp FROM RequestedProcedure rp WHERE rp.order.orderId = :orderId")
    List<RequestedProcedure> findByOrderOrderId(@Param("orderId") Long orderId);

    /**
     * Find requested procedures by patient
     */
    @Query("SELECT rp FROM RequestedProcedure rp WHERE rp.order.patient.patientId = :patientId ORDER BY rp.createdAt DESC")
    List<RequestedProcedure> findByPatient(@Param("patientId") Long patientId);

    /**
     * Find requested procedures by procedure code
     */
    @Query("SELECT rp FROM RequestedProcedure rp WHERE rp.procedureCode.procedureCodeId = :procedureCodeId ORDER BY rp.createdAt DESC")
    List<RequestedProcedure> findByProcedureCode(@Param("procedureCodeId") Long procedureCodeId);

    /**
     * Find requested procedures requiring contrast
     */
    @Query("SELECT rp FROM RequestedProcedure rp WHERE rp.contrastAgent IS NOT NULL")
    List<RequestedProcedure> findProceduresRequiringContrast();

    /**
     * Find urgent requested procedures
     */
    @Query("SELECT rp FROM RequestedProcedure rp WHERE rp.order.orderPriority IN ('URGENT', 'STAT')")
    List<RequestedProcedure> findUrgentProcedures();

    /**
     * Count requested procedures by order
     */
    long countByOrderOrderId(Long orderId);
}
