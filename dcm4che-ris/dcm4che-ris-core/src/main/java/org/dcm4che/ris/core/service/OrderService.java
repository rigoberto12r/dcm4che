package org.dcm4che.ris.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.ris.api.entity.ImagingServiceRequest;
import org.dcm4che.ris.api.entity.Patient;
import org.dcm4che.ris.api.entity.Physician;
import org.dcm4che.ris.api.entity.RequestedProcedure;
import org.dcm4che.ris.api.entity.Visit;
import org.dcm4che.ris.api.enums.OrderStatus;
import org.dcm4che.ris.core.exception.DuplicateResourceException;
import org.dcm4che.ris.core.exception.InvalidStatusTransitionException;
import org.dcm4che.ris.core.exception.ResourceNotFoundException;
import org.dcm4che.ris.persistence.repository.ImagingServiceRequestRepository;
import org.dcm4che.ris.persistence.repository.PatientRepository;
import org.dcm4che.ris.persistence.repository.PhysicianRepository;
import org.dcm4che.ris.persistence.repository.VisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing imaging service requests (orders).
 * Handles HL7 ORM compliant order management.
 *
 * @author dcm4che-ris
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final ImagingServiceRequestRepository orderRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final PhysicianRepository physicianRepository;

    /**
     * Create a new imaging order.
     */
    public ImagingServiceRequest createOrder(ImagingServiceRequest order) {
        log.info("Creating new imaging order for patient ID: {}", order.getPatient().getPatientId());

        // Validate patient exists
        if (order.getPatient() == null || order.getPatient().getPatientId() == null) {
            throw new IllegalArgumentException("Patient is required for order");
        }
        Patient patient = patientRepository.findById(order.getPatient().getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", order.getPatient().getPatientId()));
        order.setPatient(patient);

        // Validate requesting physician exists
        if (order.getRequestingPhysician() == null || order.getRequestingPhysician().getPhysicianId() == null) {
            throw new IllegalArgumentException("Requesting physician is required for order");
        }
        Physician physician = physicianRepository.findById(order.getRequestingPhysician().getPhysicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Physician", order.getRequestingPhysician().getPhysicianId()));
        order.setRequestingPhysician(physician);

        // Validate visit if provided
        if (order.getVisit() != null && order.getVisit().getVisitId() != null) {
            Visit visit = visitRepository.findById(order.getVisit().getVisitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Visit", order.getVisit().getVisitId()));
            order.setVisit(visit);
        }

        // Check for duplicate placer order number
        if (order.getPlacerOrderNumber() != null &&
            orderRepository.existsByPlacerOrderNumber(order.getPlacerOrderNumber())) {
            throw new DuplicateResourceException("Order", "placerOrderNumber", order.getPlacerOrderNumber());
        }

        // Generate filler order number if not provided
        if (order.getFillerOrderNumber() == null) {
            order.setFillerOrderNumber(generateFillerOrderNumber());
        }

        // Set order date/time if not provided
        if (order.getOrderDateTime() == null) {
            order.setOrderDateTime(LocalDateTime.now());
        }

        // Set initial status
        if (order.getOrderStatus() == null) {
            order.setOrderStatus(OrderStatus.PENDING);
        }

        // Save order
        ImagingServiceRequest savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {} and filler number: {}",
                 savedOrder.getOrderId(), savedOrder.getFillerOrderNumber());

        return savedOrder;
    }

    /**
     * Get order by ID.
     */
    @Transactional(readOnly = true)
    public ImagingServiceRequest getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    /**
     * Get order by placer order number.
     */
    @Transactional(readOnly = true)
    public ImagingServiceRequest getOrderByPlacerNumber(String placerOrderNumber) {
        return orderRepository.findByPlacerOrderNumber(placerOrderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order with placer number: " + placerOrderNumber));
    }

    /**
     * Find order by placer order number (returns Optional).
     * Used by HL7 handlers.
     */
    @Transactional(readOnly = true)
    public Optional<ImagingServiceRequest> findByPlacerOrderNumber(String placerOrderNumber) {
        return orderRepository.findByPlacerOrderNumber(placerOrderNumber);
    }

    /**
     * Get order by filler order number.
     */
    @Transactional(readOnly = true)
    public ImagingServiceRequest getOrderByFillerNumber(String fillerOrderNumber) {
        return orderRepository.findByFillerOrderNumber(fillerOrderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order with filler number: " + fillerOrderNumber));
    }

    /**
     * Update order status.
     */
    public ImagingServiceRequest updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.info("Updating order {} status to: {}", orderId, newStatus);

        ImagingServiceRequest order = getOrderById(orderId);

        // Validate status transition
        if (!order.getOrderStatus().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                    order.getOrderStatus().name(), newStatus.name());
        }

        order.setOrderStatus(newStatus);
        ImagingServiceRequest savedOrder = orderRepository.save(order);

        log.info("Order status updated successfully: {} -> {}", orderId, newStatus);
        return savedOrder;
    }

    /**
     * Cancel an order.
     */
    public ImagingServiceRequest cancelOrder(Long orderId, String reason) {
        log.info("Canceling order: {}", orderId);

        ImagingServiceRequest order = getOrderById(orderId);

        if (!order.getOrderStatus().canTransitionTo(OrderStatus.CANCELED)) {
            throw new InvalidStatusTransitionException(
                    "Cannot cancel order in status: " + order.getOrderStatus(),
                    order.getOrderStatus().name(),
                    OrderStatus.CANCELED.name());
        }

        order.setOrderStatus(OrderStatus.CANCELED);
        if (reason != null) {
            order.setNotes(order.getNotes() != null ?
                    order.getNotes() + "\nCancellation reason: " + reason :
                    "Cancellation reason: " + reason);
        }

        ImagingServiceRequest savedOrder = orderRepository.save(order);
        log.info("Order canceled: {}", orderId);
        return savedOrder;
    }

    /**
     * Add requested procedure to order.
     */
    public ImagingServiceRequest addRequestedProcedure(Long orderId, RequestedProcedure procedure) {
        log.info("Adding requested procedure to order: {}", orderId);

        ImagingServiceRequest order = getOrderById(orderId);
        order.addRequestedProcedure(procedure);

        ImagingServiceRequest savedOrder = orderRepository.save(order);
        log.info("Requested procedure added to order: {}", orderId);
        return savedOrder;
    }

    /**
     * Get orders for a patient.
     */
    @Transactional(readOnly = true)
    public List<ImagingServiceRequest> getOrdersByPatient(Long patientId) {
        return orderRepository.findByPatientPatientId(patientId);
    }

    /**
     * Get orders for a visit.
     */
    @Transactional(readOnly = true)
    public List<ImagingServiceRequest> getOrdersByVisit(Long visitId) {
        return orderRepository.findByVisitVisitId(visitId);
    }

    /**
     * Get orders by status.
     */
    @Transactional(readOnly = true)
    public List<ImagingServiceRequest> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByOrderStatus(status);
    }

    /**
     * Get pending orders.
     */
    @Transactional(readOnly = true)
    public List<ImagingServiceRequest> getPendingOrders() {
        return orderRepository.findPendingOrders();
    }

    /**
     * Get urgent/STAT orders.
     */
    @Transactional(readOnly = true)
    public List<ImagingServiceRequest> getUrgentOrders() {
        return orderRepository.findUrgentOrders();
    }

    /**
     * Get orders by requesting physician.
     */
    @Transactional(readOnly = true)
    public List<ImagingServiceRequest> getOrdersByPhysician(Long physicianId) {
        return orderRepository.findByRequestingPhysicianPhysicianId(physicianId);
    }

    /**
     * Get orders in date range.
     */
    @Transactional(readOnly = true)
    public List<ImagingServiceRequest> getOrdersInDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findOrdersInDateRange(startDate, endDate);
    }

    /**
     * Search orders by multiple criteria.
     */
    @Transactional(readOnly = true)
    public List<ImagingServiceRequest> searchOrders(Long patientId, OrderStatus status,
                                                     String priority, Long physicianId) {
        return orderRepository.searchOrders(patientId, status, priority, physicianId);
    }

    /**
     * Count orders by status.
     */
    @Transactional(readOnly = true)
    public long countOrdersByStatus(OrderStatus status) {
        return orderRepository.countByOrderStatus(status);
    }

    /**
     * Count active urgent orders.
     */
    @Transactional(readOnly = true)
    public long countActiveUrgentOrders() {
        return orderRepository.countUrgentOrdersActive();
    }

    // Helper methods

    /**
     * Generate a unique filler order number.
     */
    private String generateFillerOrderNumber() {
        // Format: RIS-YYYYMMDD-UUID
        String datePart = LocalDateTime.now().toString().substring(0, 10).replace("-", "");
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "RIS-" + datePart + "-" + uuidPart;
    }
}
