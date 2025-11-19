package org.dcm4che.ris.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.ris.api.entity.ScheduledProcedureStep;
import org.dcm4che.ris.api.enums.SPSStatus;
import org.dcm4che.ris.core.service.SchedulingService;
import org.dcm4che.ris.rest.dto.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for procedure scheduling.
 *
 * @author dcm4che-ris
 */
@RestController
@RequestMapping("/v1/scheduling")
@Tag(name = "Scheduling", description = "Procedure scheduling endpoints")
@Slf4j
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingService schedulingService;

    @PostMapping
    @Operation(summary = "Schedule a new procedure step")
    public ResponseEntity<ApiResponse<ScheduledProcedureStep>> scheduleProcedure(
            @Valid @RequestBody ScheduledProcedureStep sps) {
        log.info("REST: Scheduling new procedure step");
        ScheduledProcedureStep scheduled = schedulingService.scheduleProcedure(sps);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Procedure scheduled successfully", scheduled));
    }

    @GetMapping("/{spsId}")
    @Operation(summary = "Get scheduled procedure step by ID")
    public ResponseEntity<ApiResponse<ScheduledProcedureStep>> getSPSById(
            @Parameter(description = "SPS ID") @PathVariable Long spsId) {
        log.info("REST: Fetching SPS with ID: {}", spsId);
        ScheduledProcedureStep sps = schedulingService.getSPSById(spsId);
        return ResponseEntity.ok(ApiResponse.success(sps));
    }

    @PutMapping("/{spsId}")
    @Operation(summary = "Update scheduled procedure step")
    public ResponseEntity<ApiResponse<ScheduledProcedureStep>> updateSPS(
            @Parameter(description = "SPS ID") @PathVariable Long spsId,
            @Valid @RequestBody ScheduledProcedureStep sps) {
        log.info("REST: Updating SPS with ID: {}", spsId);
        ScheduledProcedureStep updated = schedulingService.updateSPS(spsId, sps);
        return ResponseEntity.ok(ApiResponse.success("SPS updated successfully", updated));
    }

    @PatchMapping("/{spsId}/status")
    @Operation(summary = "Update SPS status")
    public ResponseEntity<ApiResponse<ScheduledProcedureStep>> updateSPSStatus(
            @Parameter(description = "SPS ID") @PathVariable Long spsId,
            @Parameter(description = "New status") @RequestParam SPSStatus status,
            @Parameter(description = "Reason") @RequestParam(required = false) String reason) {
        log.info("REST: Updating SPS {} status to: {}", spsId, status);
        ScheduledProcedureStep updated = schedulingService.updateSPSStatus(spsId, status, reason);
        return ResponseEntity.ok(ApiResponse.success("SPS status updated successfully", updated));
    }

    @PostMapping("/{spsId}/cancel")
    @Operation(summary = "Cancel a scheduled procedure step")
    public ResponseEntity<ApiResponse<ScheduledProcedureStep>> cancelSPS(
            @Parameter(description = "SPS ID") @PathVariable Long spsId,
            @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason) {
        log.info("REST: Canceling SPS: {}", spsId);
        ScheduledProcedureStep canceled = schedulingService.cancelSPS(spsId, reason);
        return ResponseEntity.ok(ApiResponse.success("SPS canceled successfully", canceled));
    }

    @PostMapping("/{spsId}/reschedule")
    @Operation(summary = "Reschedule a procedure step")
    public ResponseEntity<ApiResponse<ScheduledProcedureStep>> rescheduleSPS(
            @Parameter(description = "SPS ID") @PathVariable Long spsId,
            @Parameter(description = "New date/time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDateTime,
            @Parameter(description = "Reason") @RequestParam(required = false) String reason) {
        log.info("REST: Rescheduling SPS {} to: {}", spsId, newDateTime);
        ScheduledProcedureStep rescheduled = schedulingService.rescheduleSPS(spsId, newDateTime, reason);
        return ResponseEntity.ok(ApiResponse.success("SPS rescheduled successfully", rescheduled));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get SPS list for an order")
    public ResponseEntity<ApiResponse<List<ScheduledProcedureStep>>> getSPSListByOrder(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        log.info("REST: Fetching SPS list for order: {}", orderId);
        List<ScheduledProcedureStep> spsList = schedulingService.getSPSListByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(spsList));
    }

    @GetMapping("/modality/{modality}")
    @Operation(summary = "Get scheduled procedures for a modality")
    public ResponseEntity<ApiResponse<List<ScheduledProcedureStep>>> getSPSByModality(
            @Parameter(description = "Modality") @PathVariable String modality,
            @Parameter(description = "Start date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("REST: Fetching SPS for modality: {} between {} and {}", modality, startDate, endDate);
        List<ScheduledProcedureStep> spsList = schedulingService.getSPSByModality(modality, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(spsList));
    }

    @GetMapping("/station/{stationAET}")
    @Operation(summary = "Get scheduled procedures for a station")
    public ResponseEntity<ApiResponse<List<ScheduledProcedureStep>>> getSPSByStation(
            @Parameter(description = "Station AE Title") @PathVariable String stationAET) {
        log.info("REST: Fetching SPS for station: {}", stationAET);
        List<ScheduledProcedureStep> spsList = schedulingService.getSPSByScheduledStation(stationAET);
        return ResponseEntity.ok(ApiResponse.success(spsList));
    }

    @GetMapping("/physician/{physicianId}")
    @Operation(summary = "Get scheduled procedures for a physician")
    public ResponseEntity<ApiResponse<List<ScheduledProcedureStep>>> getSPSByPhysician(
            @Parameter(description = "Physician ID") @PathVariable Long physicianId) {
        log.info("REST: Fetching SPS for physician: {}", physicianId);
        List<ScheduledProcedureStep> spsList = schedulingService.getSPSByPerformingPhysician(physicianId);
        return ResponseEntity.ok(ApiResponse.success(spsList));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get SPS by status")
    public ResponseEntity<ApiResponse<List<ScheduledProcedureStep>>> getSPSByStatus(
            @Parameter(description = "SPS status") @PathVariable SPSStatus status) {
        log.info("REST: Fetching SPS with status: {}", status);
        List<ScheduledProcedureStep> spsList = schedulingService.getSPSByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(spsList));
    }

    @GetMapping("/today")
    @Operation(summary = "Get today's scheduled procedures")
    public ResponseEntity<ApiResponse<List<ScheduledProcedureStep>>> getTodaySchedule() {
        log.info("REST: Fetching today's schedule");
        List<ScheduledProcedureStep> spsList = schedulingService.getTodaySchedule();
        return ResponseEntity.ok(ApiResponse.success(spsList));
    }

    @GetMapping("/date/{date}")
    @Operation(summary = "Get schedule for a specific date")
    public ResponseEntity<ApiResponse<List<ScheduledProcedureStep>>> getScheduleByDate(
            @Parameter(description = "Date") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime date) {
        log.info("REST: Fetching schedule for date: {}", date);
        List<ScheduledProcedureStep> spsList = schedulingService.getScheduleByDate(date);
        return ResponseEntity.ok(ApiResponse.success(spsList));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "Count SPS by status")
    public ResponseEntity<ApiResponse<Long>> countSPSByStatus(
            @Parameter(description = "SPS status") @PathVariable SPSStatus status) {
        log.info("REST: Counting SPS with status: {}", status);
        long count = schedulingService.countSPSByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
