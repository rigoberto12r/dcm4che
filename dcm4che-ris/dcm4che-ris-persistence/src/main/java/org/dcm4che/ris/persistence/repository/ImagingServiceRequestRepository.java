package org.dcm4che.ris.persistence.repository;

import org.dcm4che.ris.api.entity.ImagingServiceRequest;
import org.dcm4che.ris.api.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ImagingServiceRequest (Order) entity.
 * Manages imaging orders from HIS/EMR.
 *
 * @author dcm4che-ris
 */
@Repository
public interface ImagingServiceRequestRepository extends JpaRepository<ImagingServiceRequest, Long>,
                                                         JpaSpecificationExecutor<ImagingServiceRequest> {

    /**
     * Find order by placer order number (from HIS/EMR)
     */
    Optional<ImagingServiceRequest> findByPlacerOrderNumber(String placerOrderNumber);

    /**
     * Find order by filler order number (assigned by RIS)
     */
    Optional<ImagingServiceRequest> findByFillerOrderNumber(String fillerOrderNumber);

    /**
     * Check if placer order number exists
     */
    boolean existsByPlacerOrderNumber(String placerOrderNumber);

    /**
     * Find orders by patient
     */
    @Query("SELECT o FROM ImagingServiceRequest o WHERE o.patient.patientId = :patientId ORDER BY o.orderDateTime DESC")
    List<ImagingServiceRequest> findByPatientPatientId(@Param("patientId") Long patientId);

    /**
     * Find orders by visit
     */
    @Query("SELECT o FROM ImagingServiceRequest o WHERE o.visit.visitId = :visitId ORDER BY o.orderDateTime DESC")
    List<ImagingServiceRequest> findByVisitVisitId(@Param("visitId") Long visitId);

    /**
     * Find orders by status
     */
    List<ImagingServiceRequest> findByOrderStatus(OrderStatus orderStatus);

    /**
     * Find orders by priority
     */
    List<ImagingServiceRequest> findByOrderPriority(String orderPriority);

    /**
     * Find urgent/STAT orders
     */
    @Query("SELECT o FROM ImagingServiceRequest o WHERE o.orderPriority IN ('URGENT', 'STAT') " +
           "AND o.orderStatus NOT IN ('COMPLETED', 'CANCELED') ORDER BY o.orderDateTime ASC")
    List<ImagingServiceRequest> findUrgentOrders();

    /**
     * Find orders by requesting physician
     */
    @Query("SELECT o FROM ImagingServiceRequest o WHERE o.requestingPhysician.physicianId = :physicianId ORDER BY o.orderDateTime DESC")
    List<ImagingServiceRequest> findByRequestingPhysicianPhysicianId(@Param("physicianId") Long physicianId);

    /**
     * Find pending orders (not yet scheduled)
     */
    @Query("SELECT o FROM ImagingServiceRequest o WHERE o.orderStatus = 'PENDING' ORDER BY o.orderDateTime ASC")
    List<ImagingServiceRequest> findPendingOrders();

    /**
     * Find scheduled orders
     */
    @Query("SELECT o FROM ImagingServiceRequest o WHERE o.orderStatus = 'SCHEDULED' ORDER BY o.orderDateTime ASC")
    List<ImagingServiceRequest> findScheduledOrders();

    /**
     * Find in-progress orders
     */
    @Query("SELECT o FROM ImagingServiceRequest o WHERE o.orderStatus = 'IN_PROGRESS' ORDER BY o.orderDateTime ASC")
    List<ImagingServiceRequest> findInProgressOrders();

    /**
     * Find orders created in date range
     */
    @Query("SELECT o FROM ImagingServiceRequest o WHERE o.orderDateTime BETWEEN :startDate AND :endDate ORDER BY o.orderDateTime DESC")
    List<ImagingServiceRequest> findOrdersInDateRange(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find orders by status and date range
     */
    @Query("SELECT o FROM ImagingServiceRequest o WHERE o.orderStatus = :status " +
           "AND o.orderDateTime BETWEEN :startDate AND :endDate ORDER BY o.orderDateTime DESC")
    List<ImagingServiceRequest> findOrdersByStatusAndDateRange(@Param("status") OrderStatus status,
                                                                @Param("startDate") LocalDateTime startDate,
                                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Count orders by status
     */
    long countByOrderStatus(OrderStatus orderStatus);

    /**
     * Count urgent orders not completed
     */
    @Query("SELECT COUNT(o) FROM ImagingServiceRequest o WHERE o.orderPriority IN ('URGENT', 'STAT') " +
           "AND o.orderStatus NOT IN ('COMPLETED', 'CANCELED')")
    long countUrgentOrdersActive();

    /**
     * Find orders with specific confidentiality code
     */
    List<ImagingServiceRequest> findByConfidentialityCode(String confidentialityCode);

    /**
     * Search orders by multiple criteria
     */
    @Query("SELECT o FROM ImagingServiceRequest o WHERE " +
           "(:patientId IS NULL OR o.patient.patientId = :patientId) AND " +
           "(:status IS NULL OR o.orderStatus = :status) AND " +
           "(:priority IS NULL OR o.orderPriority = :priority) AND " +
           "(:physicianId IS NULL OR o.requestingPhysician.physicianId = :physicianId) " +
           "ORDER BY o.orderDateTime DESC")
    List<ImagingServiceRequest> searchOrders(@Param("patientId") Long patientId,
                                             @Param("status") OrderStatus status,
                                             @Param("priority") String priority,
                                             @Param("physicianId") Long physicianId);
}
