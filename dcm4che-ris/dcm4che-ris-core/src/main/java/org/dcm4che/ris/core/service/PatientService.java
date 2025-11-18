package org.dcm4che.ris.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.ris.api.entity.Patient;
import org.dcm4che.ris.core.exception.DuplicateResourceException;
import org.dcm4che.ris.core.exception.ResourceNotFoundException;
import org.dcm4che.ris.persistence.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing patients.
 * Provides CRUD operations and patient search functionality.
 *
 * @author dcm4che-ris
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;

    /**
     * Create a new patient.
     * Validates that MRN doesn't already exist.
     */
    public Patient createPatient(Patient patient) {
        log.info("Creating new patient with MRN: {}", patient.getMrn());

        // Check for duplicate MRN
        if (patientRepository.existsByMrn(patient.getMrn())) {
            throw new DuplicateResourceException("Patient", "MRN", patient.getMrn());
        }

        // Save and return
        Patient savedPatient = patientRepository.save(patient);
        log.info("Patient created successfully with ID: {}", savedPatient.getPatientId());
        return savedPatient;
    }

    /**
     * Get patient by ID.
     */
    @Transactional(readOnly = true)
    public Patient getPatientById(Long patientId) {
        log.debug("Fetching patient with ID: {}", patientId);
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }

    /**
     * Get patient by MRN (Medical Record Number).
     */
    @Transactional(readOnly = true)
    public Patient getPatientByMrn(String mrn) {
        log.debug("Fetching patient with MRN: {}", mrn);
        return patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new ResourceNotFoundException("Patient with MRN: " + mrn));
    }

    /**
     * Get patient by MRN and issuer (for multi-facility environments).
     */
    @Transactional(readOnly = true)
    public Patient getPatientByMrnAndIssuer(String mrn, String issuerOfPatientId) {
        log.debug("Fetching patient with MRN: {} from issuer: {}", mrn, issuerOfPatientId);
        return patientRepository.findByMrnAndIssuerOfPatientId(mrn, issuerOfPatientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient with MRN: " + mrn + " and issuer: " + issuerOfPatientId));
    }

    /**
     * Find patient by MRN and issuer (returns Optional).
     * Used by HL7 handlers.
     */
    @Transactional(readOnly = true)
    public Optional<Patient> findByMRN(String mrn, String issuerOfPatientId) {
        log.debug("Finding patient with MRN: {} from issuer: {}", mrn, issuerOfPatientId);
        if (issuerOfPatientId != null && !issuerOfPatientId.isEmpty()) {
            return patientRepository.findByMrnAndIssuerOfPatientId(mrn, issuerOfPatientId);
        } else {
            return patientRepository.findByMrn(mrn);
        }
    }

    /**
     * Update patient information.
     */
    public Patient updatePatient(Long patientId, Patient updatedPatient) {
        log.info("Updating patient with ID: {}", patientId);

        Patient existingPatient = getPatientById(patientId);

        // Check if MRN is being changed and if new MRN already exists
        if (!existingPatient.getMrn().equals(updatedPatient.getMrn())) {
            if (patientRepository.existsByMrn(updatedPatient.getMrn())) {
                throw new DuplicateResourceException("Patient", "MRN", updatedPatient.getMrn());
            }
        }

        // Update fields (keeping the same ID)
        updatedPatient.setPatientId(patientId);
        Patient savedPatient = patientRepository.save(updatedPatient);

        log.info("Patient updated successfully: {}", patientId);
        return savedPatient;
    }

    /**
     * Deactivate a patient (soft delete).
     */
    public void deactivatePatient(Long patientId) {
        log.info("Deactivating patient with ID: {}", patientId);

        Patient patient = getPatientById(patientId);
        patient.setIsActive(false);
        patientRepository.save(patient);

        log.info("Patient deactivated successfully: {}", patientId);
    }

    /**
     * Reactivate a patient.
     */
    public void reactivatePatient(Long patientId) {
        log.info("Reactivating patient with ID: {}", patientId);

        Patient patient = getPatientById(patientId);
        patient.setIsActive(true);
        patientRepository.save(patient);

        log.info("Patient reactivated successfully: {}", patientId);
    }

    /**
     * Delete a patient permanently.
     * Use with caution - this is irreversible.
     */
    public void deletePatient(Long patientId) {
        log.warn("Permanently deleting patient with ID: {}", patientId);

        Patient patient = getPatientById(patientId);
        patientRepository.delete(patient);

        log.warn("Patient permanently deleted: {}", patientId);
    }

    /**
     * Search patients by name (partial match, case-insensitive).
     */
    @Transactional(readOnly = true)
    public List<Patient> searchPatientsByName(String name) {
        log.debug("Searching patients by name: {}", name);
        return patientRepository.searchByName(name);
    }

    /**
     * Search patients by multiple criteria.
     */
    @Transactional(readOnly = true)
    public List<Patient> searchPatients(String mrn, String name, LocalDate birthDate, String sex) {
        log.debug("Searching patients with criteria - MRN: {}, Name: {}, BirthDate: {}, Sex: {}",
                  mrn, name, birthDate, sex);
        return patientRepository.searchPatients(mrn, name, birthDate, sex);
    }

    /**
     * Get all active patients.
     */
    @Transactional(readOnly = true)
    public List<Patient> getAllActivePatients() {
        log.debug("Fetching all active patients");
        return patientRepository.findByIsActiveTrue();
    }

    /**
     * Get patients by birth date.
     */
    @Transactional(readOnly = true)
    public List<Patient> getPatientsByBirthDate(LocalDate birthDate) {
        log.debug("Fetching patients with birth date: {}", birthDate);
        return patientRepository.findByBirthDate(birthDate);
    }

    /**
     * Get patients with allergies.
     */
    @Transactional(readOnly = true)
    public List<Patient> getPatientsWithAllergies() {
        log.debug("Fetching patients with allergies");
        return patientRepository.findPatientsWithAllergies();
    }

    /**
     * Get pregnant patients (for radiation safety).
     */
    @Transactional(readOnly = true)
    public List<Patient> getPregnantPatients() {
        log.debug("Fetching pregnant patients");
        return patientRepository.findPregnantPatients();
    }

    /**
     * Get VIP patients.
     */
    @Transactional(readOnly = true)
    public List<Patient> getVipPatients() {
        log.debug("Fetching VIP patients");
        return patientRepository.findByIsVipTrueAndIsActiveTrue();
    }

    /**
     * Merge two patient records (for duplicate resolution).
     * Keeps the 'targetPatient' and merges data from 'sourcePatient'.
     */
    public Patient mergePatients(Long targetPatientId, Long sourcePatientId) {
        log.info("Merging patient {} into patient {}", sourcePatientId, targetPatientId);

        Patient targetPatient = getPatientById(targetPatientId);
        Patient sourcePatient = getPatientById(sourcePatientId);

        // Use the entity's merge method
        targetPatient.mergeWith(sourcePatient);

        // Save merged patient
        Patient mergedPatient = patientRepository.save(targetPatient);

        // Deactivate source patient
        sourcePatient.setIsActive(false);
        patientRepository.save(sourcePatient);

        log.info("Patients merged successfully. Target: {}, Source (deactivated): {}",
                 targetPatientId, sourcePatientId);

        return mergedPatient;
    }

    /**
     * Count active patients.
     */
    @Transactional(readOnly = true)
    public long countActivePatients() {
        return patientRepository.countByIsActiveTrue();
    }

    /**
     * Check if a patient with the given MRN exists.
     */
    @Transactional(readOnly = true)
    public boolean existsByMrn(String mrn) {
        return patientRepository.existsByMrn(mrn);
    }
}
