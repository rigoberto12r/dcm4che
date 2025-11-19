package org.dcm4che.ris.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.ris.api.entity.Patient;
import org.dcm4che.ris.core.service.PatientService;
import org.dcm4che.ris.rest.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for patient management.
 *
 * @author dcm4che-ris
 */
@RestController
@RequestMapping("/v1/patients")
@Tag(name = "Patients", description = "Patient management endpoints")
@Slf4j
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @Operation(summary = "Create a new patient")
    public ResponseEntity<ApiResponse<Patient>> createPatient(
            @Valid @RequestBody Patient patient) {
        log.info("REST: Creating new patient with MRN: {}", patient.getMrn());
        Patient created = patientService.createPatient(patient);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient created successfully", created));
    }

    @GetMapping("/{patientId}")
    @Operation(summary = "Get patient by ID")
    public ResponseEntity<ApiResponse<Patient>> getPatientById(
            @Parameter(description = "Patient ID") @PathVariable Long patientId) {
        log.info("REST: Fetching patient with ID: {}", patientId);
        Patient patient = patientService.getPatientById(patientId);
        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    @GetMapping("/mrn/{mrn}")
    @Operation(summary = "Get patient by MRN")
    public ResponseEntity<ApiResponse<Patient>> getPatientByMRN(
            @Parameter(description = "Medical Record Number") @PathVariable String mrn,
            @Parameter(description = "Issuer of Patient ID") @RequestParam(required = false) String issuer) {
        log.info("REST: Fetching patient with MRN: {}, issuer: {}", mrn, issuer);
        Patient patient;
        if (issuer != null) {
            patient = patientService.getPatientByMRN(mrn, issuer);
        } else {
            patient = patientService.getPatientByMRN(mrn);
        }
        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    @PutMapping("/{patientId}")
    @Operation(summary = "Update patient")
    public ResponseEntity<ApiResponse<Patient>> updatePatient(
            @Parameter(description = "Patient ID") @PathVariable Long patientId,
            @Valid @RequestBody Patient patient) {
        log.info("REST: Updating patient with ID: {}", patientId);
        Patient updated = patientService.updatePatient(patientId, patient);
        return ResponseEntity.ok(ApiResponse.success("Patient updated successfully", updated));
    }

    @DeleteMapping("/{patientId}")
    @Operation(summary = "Delete patient")
    public ResponseEntity<ApiResponse<Void>> deletePatient(
            @Parameter(description = "Patient ID") @PathVariable Long patientId) {
        log.info("REST: Deleting patient with ID: {}", patientId);
        patientService.deletePatient(patientId);
        return ResponseEntity.ok(ApiResponse.success("Patient deleted successfully", null));
    }

    @GetMapping
    @Operation(summary = "Search patients")
    public ResponseEntity<ApiResponse<List<Patient>>> searchPatients(
            @Parameter(description = "Patient name") @RequestParam(required = false) String name,
            @Parameter(description = "MRN") @RequestParam(required = false) String mrn,
            @Parameter(description = "Date of birth") @RequestParam(required = false) LocalDate dateOfBirth,
            @Parameter(description = "Gender") @RequestParam(required = false) String gender) {
        log.info("REST: Searching patients with name={}, mrn={}, dob={}, gender={}",
                name, mrn, dateOfBirth, gender);
        List<Patient> patients = patientService.searchPatients(name, mrn, dateOfBirth, gender);
        return ResponseEntity.ok(ApiResponse.success(patients));
    }

    @GetMapping("/{patientId}/active")
    @Operation(summary = "Check if patient has active visits")
    public ResponseEntity<ApiResponse<Boolean>> hasActiveVisits(
            @Parameter(description = "Patient ID") @PathVariable Long patientId) {
        log.info("REST: Checking active visits for patient: {}", patientId);
        boolean hasActive = patientService.hasActiveVisits(patientId);
        return ResponseEntity.ok(ApiResponse.success(hasActive));
    }

    @GetMapping("/{patientId}/pending-orders")
    @Operation(summary = "Check if patient has pending orders")
    public ResponseEntity<ApiResponse<Boolean>> hasPendingOrders(
            @Parameter(description = "Patient ID") @PathVariable Long patientId) {
        log.info("REST: Checking pending orders for patient: {}", patientId);
        boolean hasPending = patientService.hasPendingOrders(patientId);
        return ResponseEntity.ok(ApiResponse.success(hasPending));
    }

    @GetMapping("/count")
    @Operation(summary = "Count total patients")
    public ResponseEntity<ApiResponse<Long>> countPatients() {
        log.info("REST: Counting total patients");
        long count = patientService.countAllPatients();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
