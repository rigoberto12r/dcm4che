package org.dcm4che.ris.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.ris.api.entity.ImagingServiceRequest;
import org.dcm4che.ris.api.entity.RequestedProcedure;
import org.dcm4che.ris.api.enums.OrderStatus;
import org.dcm4che.ris.core.service.OrderService;
import org.dcm4che.ris.rest.dto.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for imaging order management.
 *
 * @author dcm4che-ris
 */
@RestController
@RequestMapping("/v1/orders")
@Tag(name = "Orders", description = "Imaging order management endpoints")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new imaging order")
    public ResponseEntity<ApiResponse<ImagingServiceRequest>> createOrder(
            @Valid @RequestBody ImagingServiceRequest order) {
        log.info("REST: Creating new imaging order for patient ID: {}",
                order.getPatient() != null ? order.getPatient().getPatientId() : null);
        ImagingServiceRequest created = orderService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", created));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<ImagingServiceRequest>> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        log.info("REST: Fetching order with ID: {}", orderId);
        ImagingServiceRequest order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/placer/{placerOrderNumber}")
    @Operation(summary = "Get order by placer order number")
    public ResponseEntity<ApiResponse<ImagingServiceRequest>> getOrderByPlacerNumber(
            @Parameter(description = "Placer order number") @PathVariable String placerOrderNumber) {
        log.info("REST: Fetching order with placer number: {}", placerOrderNumber);
        ImagingServiceRequest order = orderService.getOrderByPlacerNumber(placerOrderNumber);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/filler/{fillerOrderNumber}")
    @Operation(summary = "Get order by filler order number")
    public ResponseEntity<ApiResponse<ImagingServiceRequest>> getOrderByFillerNumber(
            @Parameter(description = "Filler order number") @PathVariable String fillerOrderNumber) {
        log.info("REST: Fetching order with filler number: {}", fillerOrderNumber);
        ImagingServiceRequest order = orderService.getOrderByFillerNumber(fillerOrderNumber);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<ImagingServiceRequest>> updateOrderStatus(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @Parameter(description = "New status") @RequestParam OrderStatus status) {
        log.info("REST: Updating order {} status to: {}", orderId, status);
        ImagingServiceRequest updated = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", updated));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<ImagingServiceRequest>> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason) {
        log.info("REST: Canceling order: {}", orderId);
        ImagingServiceRequest canceled = orderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Order canceled successfully", canceled));
    }

    @PostMapping("/{orderId}/procedures")
    @Operation(summary = "Add requested procedure to order")
    public ResponseEntity<ApiResponse<ImagingServiceRequest>> addRequestedProcedure(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @Valid @RequestBody RequestedProcedure procedure) {
        log.info("REST: Adding requested procedure to order: {}", orderId);
        ImagingServiceRequest updated = orderService.addRequestedProcedure(orderId, procedure);
        return ResponseEntity.ok(ApiResponse.success("Procedure added successfully", updated));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get orders for a patient")
    public ResponseEntity<ApiResponse<List<ImagingServiceRequest>>> getOrdersByPatient(
            @Parameter(description = "Patient ID") @PathVariable Long patientId) {
        log.info("REST: Fetching orders for patient: {}", patientId);
        List<ImagingServiceRequest> orders = orderService.getOrdersByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/visit/{visitId}")
    @Operation(summary = "Get orders for a visit")
    public ResponseEntity<ApiResponse<List<ImagingServiceRequest>>> getOrdersByVisit(
            @Parameter(description = "Visit ID") @PathVariable Long visitId) {
        log.info("REST: Fetching orders for visit: {}", visitId);
        List<ImagingServiceRequest> orders = orderService.getOrdersByVisit(visitId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status")
    public ResponseEntity<ApiResponse<List<ImagingServiceRequest>>> getOrdersByStatus(
            @Parameter(description = "Order status") @PathVariable OrderStatus status) {
        log.info("REST: Fetching orders with status: {}", status);
        List<ImagingServiceRequest> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending orders")
    public ResponseEntity<ApiResponse<List<ImagingServiceRequest>>> getPendingOrders() {
        log.info("REST: Fetching pending orders");
        List<ImagingServiceRequest> orders = orderService.getPendingOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/urgent")
    @Operation(summary = "Get urgent/STAT orders")
    public ResponseEntity<ApiResponse<List<ImagingServiceRequest>>> getUrgentOrders() {
        log.info("REST: Fetching urgent orders");
        List<ImagingServiceRequest> orders = orderService.getUrgentOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/physician/{physicianId}")
    @Operation(summary = "Get orders by requesting physician")
    public ResponseEntity<ApiResponse<List<ImagingServiceRequest>>> getOrdersByPhysician(
            @Parameter(description = "Physician ID") @PathVariable Long physicianId) {
        log.info("REST: Fetching orders for physician: {}", physicianId);
        List<ImagingServiceRequest> orders = orderService.getOrdersByPhysician(physicianId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get orders in date range")
    public ResponseEntity<ApiResponse<List<ImagingServiceRequest>>> getOrdersInDateRange(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("REST: Fetching orders between {} and {}", startDate, endDate);
        List<ImagingServiceRequest> orders = orderService.getOrdersInDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/search")
    @Operation(summary = "Search orders by multiple criteria")
    public ResponseEntity<ApiResponse<List<ImagingServiceRequest>>> searchOrders(
            @Parameter(description = "Patient ID") @RequestParam(required = false) Long patientId,
            @Parameter(description = "Order status") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Priority") @RequestParam(required = false) String priority,
            @Parameter(description = "Physician ID") @RequestParam(required = false) Long physicianId) {
        log.info("REST: Searching orders with patientId={}, status={}, priority={}, physicianId={}",
                patientId, status, priority, physicianId);
        List<ImagingServiceRequest> orders = orderService.searchOrders(patientId, status, priority, physicianId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "Count orders by status")
    public ResponseEntity<ApiResponse<Long>> countOrdersByStatus(
            @Parameter(description = "Order status") @PathVariable OrderStatus status) {
        log.info("REST: Counting orders with status: {}", status);
        long count = orderService.countOrdersByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/urgent")
    @Operation(summary = "Count active urgent orders")
    public ResponseEntity<ApiResponse<Long>> countActiveUrgentOrders() {
        log.info("REST: Counting active urgent orders");
        long count = orderService.countActiveUrgentOrders();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
