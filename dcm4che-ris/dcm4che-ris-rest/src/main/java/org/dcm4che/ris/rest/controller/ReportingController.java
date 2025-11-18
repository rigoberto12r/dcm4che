package org.dcm4che.ris.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.ris.api.entity.Report;
import org.dcm4che.ris.api.enums.ReportStatus;
import org.dcm4che.ris.core.service.ReportingService;
import org.dcm4che.ris.rest.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for report management.
 *
 * @author dcm4che-ris
 */
@RestController
@RequestMapping("/v1/reports")
@Tag(name = "Reports", description = "Report management endpoints")
@Slf4j
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @PostMapping
    @Operation(summary = "Create a new report")
    public ResponseEntity<ApiResponse<Report>> createReport(
            @Valid @RequestBody Report report) {
        log.info("REST: Creating new report");
        Report created = reportingService.createReport(report);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report created successfully", created));
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get report by ID")
    public ResponseEntity<ApiResponse<Report>> getReportById(
            @Parameter(description = "Report ID") @PathVariable Long reportId) {
        log.info("REST: Fetching report with ID: {}", reportId);
        Report report = reportingService.getReportById(reportId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PutMapping("/{reportId}")
    @Operation(summary = "Update report content")
    public ResponseEntity<ApiResponse<Report>> updateReportContent(
            @Parameter(description = "Report ID") @PathVariable Long reportId,
            @Valid @RequestBody Report updatedContent,
            @Parameter(description = "User ID") @RequestParam String userId) {
        log.info("REST: Updating report with ID: {} by user: {}", reportId, userId);
        Report updated = reportingService.updateReportContent(reportId, updatedContent, userId);
        return ResponseEntity.ok(ApiResponse.success("Report updated successfully", updated));
    }

    @PostMapping("/{reportId}/sign")
    @Operation(summary = "Sign a report")
    public ResponseEntity<ApiResponse<Report>> signReport(
            @Parameter(description = "Report ID") @PathVariable Long reportId,
            @Parameter(description = "Physician ID") @RequestParam Long physicianId) {
        log.info("REST: Signing report {} by physician: {}", reportId, physicianId);
        Report signed = reportingService.signReport(reportId, physicianId);
        return ResponseEntity.ok(ApiResponse.success("Report signed successfully", signed));
    }

    @PostMapping("/{reportId}/verify")
    @Operation(summary = "Verify a report")
    public ResponseEntity<ApiResponse<Report>> verifyReport(
            @Parameter(description = "Report ID") @PathVariable Long reportId,
            @Parameter(description = "Physician ID") @RequestParam Long physicianId) {
        log.info("REST: Verifying report {} by physician: {}", reportId, physicianId);
        Report verified = reportingService.verifyReport(reportId, physicianId);
        return ResponseEntity.ok(ApiResponse.success("Report verified successfully", verified));
    }

    @PostMapping("/{reportId}/amend")
    @Operation(summary = "Amend a signed report")
    public ResponseEntity<ApiResponse<Report>> amendReport(
            @Parameter(description = "Report ID") @PathVariable Long reportId,
            @Parameter(description = "Amendment reason") @RequestParam String reason,
            @Parameter(description = "Physician ID") @RequestParam Long physicianId) {
        log.info("REST: Amending report {} by physician: {}", reportId, physicianId);
        Report amended = reportingService.amendReport(reportId, reason, physicianId);
        return ResponseEntity.ok(ApiResponse.success("Report amended successfully", amended));
    }

    @PostMapping("/{reportId}/addendum")
    @Operation(summary = "Add addendum to report")
    public ResponseEntity<ApiResponse<Report>> addAddendum(
            @Parameter(description = "Report ID") @PathVariable Long reportId,
            @Parameter(description = "Addendum content") @RequestParam String content,
            @Parameter(description = "Physician ID") @RequestParam Long physicianId) {
        log.info("REST: Adding addendum to report {} by physician: {}", reportId, physicianId);
        Report updated = reportingService.addAddendum(reportId, content, physicianId);
        return ResponseEntity.ok(ApiResponse.success("Addendum added successfully", updated));
    }

    @GetMapping("/sps/{spsId}")
    @Operation(summary = "Get report by scheduled procedure step ID")
    public ResponseEntity<ApiResponse<Report>> getReportBySPS(
            @Parameter(description = "SPS ID") @PathVariable Long spsId) {
        log.info("REST: Fetching report for SPS: {}", spsId);
        Report report = reportingService.getReportBySPS(spsId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get reports by status")
    public ResponseEntity<ApiResponse<List<Report>>> getReportsByStatus(
            @Parameter(description = "Report status") @PathVariable ReportStatus status) {
        log.info("REST: Fetching reports with status: {}", status);
        List<Report> reports = reportingService.getReportsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/author/{physicianId}")
    @Operation(summary = "Get reports by author")
    public ResponseEntity<ApiResponse<List<Report>>> getReportsByAuthor(
            @Parameter(description = "Physician ID") @PathVariable Long physicianId) {
        log.info("REST: Fetching reports by author: {}", physicianId);
        List<Report> reports = reportingService.getReportsByAuthor(physicianId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/verifier/{physicianId}")
    @Operation(summary = "Get reports by verifying physician")
    public ResponseEntity<ApiResponse<List<Report>>> getReportsByVerifier(
            @Parameter(description = "Physician ID") @PathVariable Long physicianId) {
        log.info("REST: Fetching reports by verifier: {}", physicianId);
        List<Report> reports = reportingService.getReportsByVerifyingPhysician(physicianId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending reports")
    public ResponseEntity<ApiResponse<List<Report>>> getPendingReports() {
        log.info("REST: Fetching pending reports");
        List<Report> reports = reportingService.getPendingReports();
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/unsigned")
    @Operation(summary = "Get unsigned reports")
    public ResponseEntity<ApiResponse<List<Report>>> getUnsignedReports() {
        log.info("REST: Fetching unsigned reports");
        List<Report> reports = reportingService.getUnsignedReports();
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "Count reports by status")
    public ResponseEntity<ApiResponse<Long>> countReportsByStatus(
            @Parameter(description = "Report status") @PathVariable ReportStatus status) {
        log.info("REST: Counting reports with status: {}", status);
        long count = reportingService.countReportsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/unsigned")
    @Operation(summary = "Count unsigned reports")
    public ResponseEntity<ApiResponse<Long>> countUnsignedReports() {
        log.info("REST: Counting unsigned reports");
        long count = reportingService.countUnsignedReports();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
