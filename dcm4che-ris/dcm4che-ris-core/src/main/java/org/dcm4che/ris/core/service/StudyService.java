package org.dcm4che.ris.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.ris.api.entity.Patient;
import org.dcm4che.ris.api.entity.Physician;
import org.dcm4che.ris.api.entity.RequestedProcedure;
import org.dcm4che.ris.api.entity.Study;
import org.dcm4che.ris.core.exception.DuplicateResourceException;
import org.dcm4che.ris.core.exception.ResourceNotFoundException;
import org.dcm4che.ris.persistence.repository.PatientRepository;
import org.dcm4che.ris.persistence.repository.PhysicianRepository;
import org.dcm4che.ris.persistence.repository.RequestedProcedureRepository;
import org.dcm4che.ris.persistence.repository.StudyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing DICOM studies.
 * Handles study registration and queries.
 *
 * @author dcm4che-ris
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class StudyService {

    private final StudyRepository studyRepository;
    private final PatientRepository patientRepository;
    private final PhysicianRepository physicianRepository;
    private final RequestedProcedureRepository requestedProcedureRepository;

    /**
     * Register a new study (from PACS/modality).
     */
    public Study registerStudy(Study study) {
        log.info("Registering new study with Study Instance UID: {}", study.getStudyInstanceUID());

        // Check for duplicate Study Instance UID
        if (studyRepository.existsByStudyInstanceUID(study.getStudyInstanceUID())) {
            throw new DuplicateResourceException("Study", "studyInstanceUID", study.getStudyInstanceUID());
        }

        // Validate patient exists
        if (study.getPatient() == null || study.getPatient().getPatientId() == null) {
            throw new IllegalArgumentException("Patient is required for study");
        }
        Patient patient = patientRepository.findById(study.getPatient().getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", study.getPatient().getPatientId()));
        study.setPatient(patient);

        // Link to requested procedure if accession number provided
        if (study.getAccessionNumber() != null) {
            requestedProcedureRepository.findByAccessionNumber(study.getAccessionNumber())
                    .ifPresent(study::setRequestedProcedure);
        }

        // Validate referring physician if provided
        if (study.getReferringPhysician() != null && study.getReferringPhysician().getPhysicianId() != null) {
            Physician physician = physicianRepository.findById(study.getReferringPhysician().getPhysicianId())
                    .orElseThrow(() -> new ResourceNotFoundException("Physician",
                            study.getReferringPhysician().getPhysicianId()));
            study.setReferringPhysician(physician);
        }

        // Set received date if not provided
        if (study.getStudyReceivedDate() == null) {
            study.setStudyReceivedDate(LocalDateTime.now());
        }

        // Set initial status
        if (study.getStudyStatus() == null) {
            study.setStudyStatus("RECEIVED");
        }

        Study savedStudy = studyRepository.save(study);
        log.info("Study registered successfully with ID: {}", savedStudy.getStudyId());

        return savedStudy;
    }

    /**
     * Update study information (from PACS updates).
     */
    public Study updateStudy(String studyInstanceUID, Study updatedStudy) {
        log.info("Updating study: {}", studyInstanceUID);

        Study existingStudy = getStudyByInstanceUID(studyInstanceUID);

        // Update allowed fields
        if (updatedStudy.getNumberOfSeries() != null) {
            existingStudy.setNumberOfSeries(updatedStudy.getNumberOfSeries());
        }
        if (updatedStudy.getNumberOfInstances() != null) {
            existingStudy.setNumberOfInstances(updatedStudy.getNumberOfInstances());
        }
        if (updatedStudy.getStudyDescription() != null) {
            existingStudy.setStudyDescription(updatedStudy.getStudyDescription());
        }
        if (updatedStudy.getModalitiesInStudy() != null) {
            existingStudy.setModalitiesInStudy(updatedStudy.getModalitiesInStudy());
        }
        if (updatedStudy.getPacsUrl() != null) {
            existingStudy.setPacsUrl(updatedStudy.getPacsUrl());
        }
        if (updatedStudy.getStudyStatus() != null) {
            existingStudy.setStudyStatus(updatedStudy.getStudyStatus());
        }

        Study savedStudy = studyRepository.save(existingStudy);
        log.info("Study updated: {}", studyInstanceUID);

        return savedStudy;
    }

    /**
     * Assign radiologist to study.
     */
    public Study assignRadiologist(Long studyId, Long physicianId) {
        log.info("Assigning radiologist {} to study {}", physicianId, studyId);

        Study study = getStudyById(studyId);
        Physician radiologist = physicianRepository.findById(physicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Physician", physicianId));

        if (!radiologist.getIsRadiologist()) {
            throw new IllegalArgumentException("Physician is not a radiologist");
        }

        study.setAssignedRadiologist(radiologist);
        Study savedStudy = studyRepository.save(study);

        log.info("Radiologist assigned to study: {}", studyId);
        return savedStudy;
    }

    /**
     * Get study by ID.
     */
    @Transactional(readOnly = true)
    public Study getStudyById(Long studyId) {
        return studyRepository.findById(studyId)
                .orElseThrow(() -> new ResourceNotFoundException("Study", studyId));
    }

    /**
     * Get study by Study Instance UID.
     */
    @Transactional(readOnly = true)
    public Study getStudyByInstanceUID(String studyInstanceUID) {
        return studyRepository.findByStudyInstanceUID(studyInstanceUID)
                .orElseThrow(() -> new ResourceNotFoundException("Study with UID: " + studyInstanceUID));
    }

    /**
     * Get study by Accession Number.
     */
    @Transactional(readOnly = true)
    public Study getStudyByAccessionNumber(String accessionNumber) {
        return studyRepository.findByAccessionNumber(accessionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Study with accession number: " + accessionNumber));
    }

    /**
     * Get studies for a patient.
     */
    @Transactional(readOnly = true)
    public List<Study> getStudiesByPatient(Long patientId) {
        return studyRepository.findByPatientPatientId(patientId);
    }

    /**
     * Get studies by date.
     */
    @Transactional(readOnly = true)
    public List<Study> getStudiesByDate(LocalDate date) {
        return studyRepository.findByStudyDate(date);
    }

    /**
     * Get studies by date range.
     */
    @Transactional(readOnly = true)
    public List<Study> getStudiesByDateRange(LocalDate startDate, LocalDate endDate) {
        return studyRepository.findByStudyDateBetween(startDate, endDate);
    }

    /**
     * Get studies by modality.
     */
    @Transactional(readOnly = true)
    public List<Study> getStudiesByModality(String modality) {
        return studyRepository.findByModality(modality);
    }

    /**
     * Get studies by status.
     */
    @Transactional(readOnly = true)
    public List<Study> getStudiesByStatus(String status) {
        return studyRepository.findByStudyStatus(status);
    }

    /**
     * Get unreported studies (no final report).
     */
    @Transactional(readOnly = true)
    public List<Study> getUnreportedStudies() {
        return studyRepository.findUnreportedStudies();
    }

    /**
     * Get studies with preliminary reports.
     */
    @Transactional(readOnly = true)
    public List<Study> getStudiesWithPreliminaryReports() {
        return studyRepository.findStudiesWithPreliminaryReports();
    }

    /**
     * Get studies by reading physician.
     */
    @Transactional(readOnly = true)
    public List<Study> getStudiesByReadingPhysician(Long userId) {
        return studyRepository.findByReadingPhysician(userId);
    }

    /**
     * Get studies by referring physician.
     */
    @Transactional(readOnly = true)
    public List<Study> getStudiesByReferringPhysician(Long physicianId) {
        return studyRepository.findByReferringPhysician(physicianId);
    }

    /**
     * Get studies received in date range.
     */
    @Transactional(readOnly = true)
    public List<Study> getStudiesReceivedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return studyRepository.findStudiesReceivedBetween(startDate, endDate);
    }

    /**
     * Search studies by patient name.
     */
    @Transactional(readOnly = true)
    public List<Study> searchStudiesByPatientName(String name) {
        return studyRepository.searchByPatientName(name);
    }

    /**
     * Search studies by patient MRN.
     */
    @Transactional(readOnly = true)
    public List<Study> searchStudiesByPatientMRN(String mrn) {
        return studyRepository.searchByPatientMRN(mrn);
    }

    /**
     * Search studies by description.
     */
    @Transactional(readOnly = true)
    public List<Study> searchStudiesByDescription(String description) {
        return studyRepository.searchByDescription(description);
    }

    /**
     * Get urgent studies not yet reported.
     */
    @Transactional(readOnly = true)
    public List<Study> getUnreportedStatStudies() {
        return studyRepository.findUnreportedStatStudies();
    }

    /**
     * Search studies by multiple criteria.
     */
    @Transactional(readOnly = true)
    public List<Study> searchStudies(Long patientId, String modality,
                                     LocalDate startDate, LocalDate endDate, String status) {
        return studyRepository.searchStudies(patientId, modality, startDate, endDate, status);
    }

    /**
     * Count studies by status.
     */
    @Transactional(readOnly = true)
    public long countStudiesByStatus(String status) {
        return studyRepository.countByStudyStatus(status);
    }

    /**
     * Count studies by modality and date range.
     */
    @Transactional(readOnly = true)
    public long countStudiesByModalityAndDateRange(String modality, LocalDate startDate, LocalDate endDate) {
        return studyRepository.countByModalityAndDateRange(modality, startDate, endDate);
    }

    /**
     * Check if study exists by Study Instance UID.
     */
    @Transactional(readOnly = true)
    public boolean existsByStudyInstanceUID(String studyInstanceUID) {
        return studyRepository.existsByStudyInstanceUID(studyInstanceUID);
    }
}
