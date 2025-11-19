# dcm4che-ris - Modelo de Datos Completo

Modelo de datos basado en **IHE Scheduled Workflow Profile** e inspirado en ThaiRIS.

## Jerarquía de Entidades

```
PATIENT (Demographics)
  │
  └── VISIT (Admission/Encounter)
        │
        └── IMAGING_SERVICE_REQUEST (Order)
              │
              ├── Placer Order Number (del sistema que ordena - HIS/EMR)
              ├── Filler Order Number (del RIS)
              │
              └── REQUESTED_PROCEDURE
                    │
                    ├── Accession Number (único por estudio)
                    ├── Study Instance UID
                    │
                    └── SCHEDULED_PROCEDURE_STEP (SPS)
                          │
                          ├── Scheduled Station AE Title
                          ├── Scheduled DateTime
                          ├── Modality
                          │
                          └── PERFORMED_PROCEDURE_STEP (PPS)
                                │
                                ├── PPS Start/End DateTime
                                ├── Performed Station AE Title
                                │
                                └── STUDY (DICOM Study)
                                      │
                                      ├── Study Instance UID
                                      ├── Series
                                      ├── Images
                                      │
                                      └── REPORT (Radiology Report)
                                            │
                                            ├── Template (opcional)
                                            ├── Draft → Preliminary → Final
                                            └── Digital Signature
```

---

## 1. PATIENT (Paciente)

Datos demográficos del paciente siguiendo DICOM Patient Module.

### Campos

| Campo | Tipo | DICOM Tag | HL7 | Descripción |
|-------|------|-----------|-----|-------------|
| `patient_id` | BIGINT PK | - | - | ID interno |
| `mrn` | VARCHAR(64) | (0010,0020) | PID-3 | Medical Record Number |
| `issuer_of_patient_id` | VARCHAR(64) | (0010,0021) | PID-3.4 | Emisor del ID |
| `patient_name` | VARCHAR(255) | (0010,0010) | PID-5 | Nombre (Last^First^Middle^Prefix^Suffix) |
| `birth_date` | DATE | (0010,0030) | PID-7 | Fecha de nacimiento |
| `sex` | CHAR(1) | (0010,0040) | PID-8 | M/F/O |
| `address` | TEXT | - | PID-11 | Dirección |
| `phone_numbers` | VARCHAR(255) | - | PID-13 | Teléfonos |
| `email` | VARCHAR(255) | - | PID-13 | Email |
| `responsible_person` | VARCHAR(255) | (0010,2297) | PID-16 | Tutor/Responsable |
| `patient_species` | VARCHAR(64) | (0010,2201) | - | Especie (veterinaria) |
| `patient_breed` | VARCHAR(64) | (0010,2292) | - | Raza (veterinaria) |
| `allergies` | TEXT | - | AL1 | Alergias |
| `medical_alerts` | TEXT | - | - | Alertas médicas |
| `pregnancy_status` | VARCHAR(16) | (0010,21C0) | - | PREGNANT/NOT_PREGNANT/UNKNOWN |
| `patient_state` | VARCHAR(16) | (0038,0500) | - | ACTIVE/MERGED/INACTIVE |
| `merged_with_patient_id` | BIGINT FK | - | - | Si fue fusionado con otro paciente |
| `created_at` | TIMESTAMP | - | - | Fecha de creación |
| `updated_at` | TIMESTAMP | - | - | Última actualización |

### Índices
- UNIQUE: `mrn`, `issuer_of_patient_id`
- INDEX: `patient_name`, `birth_date`, `patient_state`

### Ejemplo
```sql
INSERT INTO patient (mrn, issuer_of_patient_id, patient_name, birth_date, sex, phone_numbers, email)
VALUES ('MRN123456', 'HOSPITAL_A', 'Doe^John^Michael^^Mr', '1980-05-15', 'M', '555-1234', 'john.doe@email.com');
```

---

## 2. VISIT (Visita/Admisión)

Representa una visita o admisión del paciente al hospital.

### Campos

| Campo | Tipo | DICOM Tag | HL7 | Descripción |
|-------|------|-----------|-----|-------------|
| `visit_id` | BIGINT PK | - | - | ID interno |
| `patient_id` | BIGINT FK | - | PID | Referencia a paciente |
| `admission_id` | VARCHAR(64) | (0038,0010) | PV1-19 | Admission ID |
| `issuer_of_admission_id` | VARCHAR(64) | (0038,0014) | - | Emisor del Admission ID |
| `admission_date` | TIMESTAMP | - | PV1-44 | Fecha de admisión |
| `discharge_date` | TIMESTAMP | - | PV1-45 | Fecha de alta |
| `visit_type` | VARCHAR(16) | - | PV1-2 | OUTPATIENT/INPATIENT/EMERGENCY |
| `referring_physician_id` | BIGINT FK | - | PV1-8 | Médico referente |
| `attending_physician_id` | BIGINT FK | - | PV1-7 | Médico tratante |
| `department_id` | BIGINT FK | - | PV1-3 | Departamento |
| `visit_status` | VARCHAR(16) | - | - | ACTIVE/DISCHARGED/CANCELED |
| `created_at` | TIMESTAMP | - | - | Fecha de creación |
| `updated_at` | TIMESTAMP | - | - | Última actualización |

### Índices
- INDEX: `patient_id`, `admission_id`, `visit_status`

---

## 3. IMAGING_SERVICE_REQUEST (Order - Orden de Imagen)

Orden de estudios de imagen, puede contener múltiples procedimientos.

### Campos

| Campo | Tipo | DICOM Tag | HL7 | Descripción |
|-------|------|-----------|-----|-------------|
| `order_id` | BIGINT PK | - | - | ID interno |
| `placer_order_number` | VARCHAR(64) | (0040,2016) | ORC-2 | Número de orden del sistema ordenador (HIS) |
| `filler_order_number` | VARCHAR(64) | (0040,2017) | ORC-3 | Número de orden del RIS |
| `patient_id` | BIGINT FK | - | PID | Paciente |
| `visit_id` | BIGINT FK | - | PV1 | Visita (opcional) |
| `requesting_physician_id` | BIGINT FK | - | OBR-16 | Médico que ordena |
| `order_date_time` | TIMESTAMP | - | ORC-9 | Fecha/hora de la orden |
| `order_status` | VARCHAR(16) | - | ORC-5 | PENDING/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELED |
| `order_priority` | VARCHAR(16) | - | ORC-7 | ROUTINE/URGENT/STAT |
| `reason_for_exam` | TEXT | (0040,1002) | OBR-31 | Razón del examen |
| `reason_for_exam_code` | VARCHAR(64) | (0040,100A) | - | Código ICD-10/SNOMED |
| `clinical_info` | TEXT | - | OBR-13 | Información clínica |
| `patient_transport` | VARCHAR(64) | - | - | Tipo de transporte del paciente |
| `confidentiality_code` | VARCHAR(16) | - | - | NORMAL/RESTRICTED/VIP |
| `created_at` | TIMESTAMP | - | - | Fecha de creación |
| `updated_at` | TIMESTAMP | - | - | Última actualización |

### Índices
- UNIQUE: `placer_order_number`, `filler_order_number`
- INDEX: `patient_id`, `order_status`, `order_date_time`

---

## 4. REQUESTED_PROCEDURE (Procedimiento Solicitado)

Procedimiento específico dentro de una orden. Una orden puede tener múltiples procedimientos.

### Campos

| Campo | Tipo | DICOM Tag | HL7 | Descripción |
|-------|------|-----------|-----|-------------|
| `requested_procedure_id` | BIGINT PK | - | - | ID interno |
| `order_id` | BIGINT FK | - | OBR | Orden padre |
| `requested_procedure_id_code` | VARCHAR(64) | (0040,1001) | - | Código del procedimiento |
| `requested_procedure_description` | VARCHAR(255) | (0032,1060) | OBR-4 | Descripción |
| `study_instance_uid` | VARCHAR(128) | (0020,000D) | - | UID del estudio (generado por RIS) |
| `accession_number` | VARCHAR(64) | (0008,0050) | OBR-18 | Número de acceso (único por estudio) |
| `procedure_code_id` | BIGINT FK | - | - | Código del catálogo de procedimientos |
| `procedure_priority` | VARCHAR(16) | - | OBR-5 | ROUTINE/URGENT/STAT |
| `requested_date_time` | TIMESTAMP | - | OBR-6 | Fecha/hora solicitada |
| `requested_contrast_agent` | VARCHAR(64) | (0018,0010) | - | Agente de contraste |
| `contrast_allergies` | BOOLEAN | - | - | Alergias a contraste |
| `created_at` | TIMESTAMP | - | - | Fecha de creación |
| `updated_at` | TIMESTAMP | - | - | Última actualización |

### Índices
- UNIQUE: `accession_number`, `study_instance_uid`
- INDEX: `order_id`, `procedure_code_id`

---

## 5. SCHEDULED_PROCEDURE_STEP (SPS - Paso de Procedimiento Programado)

Representa la programación de un procedimiento en una modalidad específica.

### Campos

| Campo | Tipo | DICOM Tag | HL7 | Descripción |
|-------|------|-----------|-----|-------------|
| `sps_id` | BIGINT PK | - | - | ID interno |
| `requested_procedure_id` | BIGINT FK | - | - | Procedimiento solicitado |
| `sps_id_code` | VARCHAR(64) | (0040,0009) | IPC-3 | Código del SPS |
| `modality` | VARCHAR(16) | (0008,0060) | OBR-24 | CT/MR/CR/DX/US/NM/PT/MG/etc |
| `scheduled_station_ae_title` | VARCHAR(16) | (0040,0001) | - | AE Title de la modalidad |
| `scheduled_station_name` | VARCHAR(64) | (0040,0010) | - | Nombre de la estación |
| `scheduled_start_date_time` | TIMESTAMP | (0040,0002) | IPC-4 | Inicio programado |
| `scheduled_end_date_time` | TIMESTAMP | - | - | Fin programado |
| `scheduled_performing_physician_id` | BIGINT FK | (0040,0006) | - | Médico programado |
| `scheduled_procedure_step_location` | VARCHAR(64) | (0040,0011) | - | Ubicación |
| `sps_status` | VARCHAR(16) | (0040,0020) | IPC-6 | Ver estados abajo |
| `sps_description` | VARCHAR(255) | (0040,0007) | - | Descripción del paso |
| `protocol_code` | VARCHAR(64) | - | - | Protocolo de adquisición |
| `pre_medication` | TEXT | (0040,0012) | - | Pre-medicación requerida |
| `patient_position` | VARCHAR(16) | (0018,5100) | - | HFS/HFP/HFDR/HFDL/FFP/FFS/etc |
| `created_at` | TIMESTAMP | - | - | Fecha de creación |
| `updated_at` | TIMESTAMP | - | - | Última actualización |

### Estados de SPS (`sps_status`)
- `SCHEDULED`: Programado
- `ARRIVED`: Paciente llegó
- `READY`: Listo para realizar
- `STARTED`: En progreso
- `COMPLETED`: Completado
- `CANCELED`: Cancelado
- `DISCONTINUED`: Interrumpido

### Índices
- INDEX: `requested_procedure_id`, `sps_status`, `scheduled_station_ae_title`, `scheduled_start_date_time`

---

## 6. PERFORMED_PROCEDURE_STEP (PPS - Paso de Procedimiento Realizado)

Información sobre la ejecución real del procedimiento (vía MPPS).

### Campos

| Campo | Tipo | DICOM Tag | HL7 | Descripción |
|-------|------|-----------|-----|-------------|
| `pps_id` | BIGINT PK | - | - | ID interno |
| `sps_id` | BIGINT FK | - | - | SPS relacionado |
| `performed_procedure_step_id` | VARCHAR(64) | (0040,0253) | - | ID del PPS (del MPPS N-CREATE) |
| `sop_instance_uid` | VARCHAR(128) | (0008,0018) | - | SOP Instance UID del MPPS |
| `performed_station_ae_title` | VARCHAR(16) | (0040,0241) | - | AE Title donde se realizó |
| `performed_station_name` | VARCHAR(64) | (0040,0242) | - | Nombre de la estación |
| `pps_start_date_time` | TIMESTAMP | (0040,0244) | - | Inicio real |
| `pps_end_date_time` | TIMESTAMP | (0040,0250) | - | Fin real |
| `pps_status` | VARCHAR(16) | (0040,0252) | - | IN_PROGRESS/COMPLETED/DISCONTINUED |
| `performed_procedure_step_description` | VARCHAR(255) | (0040,0254) | - | Descripción |
| `performed_protocol_code` | VARCHAR(64) | (0040,0260) | - | Protocolo usado |
| `performed_physician_id` | BIGINT FK | - | - | Médico que realizó |
| `dose_area_product` | DECIMAL(10,2) | - | - | DAP (dGy·cm²) |
| `total_time_of_fluoroscopy` | DECIMAL(10,2) | - | - | Tiempo de fluoroscopía (s) |
| `comments_on_performed_procedure_step` | TEXT | (0040,0280) | - | Comentarios |
| `created_at` | TIMESTAMP | - | - | Fecha de creación |
| `updated_at` | TIMESTAMP | - | - | Última actualización |

### Índices
- UNIQUE: `sop_instance_uid`
- INDEX: `sps_id`, `pps_status`, `pps_start_date_time`

---

## 7. STUDY (Estudio DICOM)

Representa un estudio DICOM con metadatos.

### Campos

| Campo | Tipo | DICOM Tag | Descripción |
|-------|------|-----------|-------------|
| `study_id` | BIGINT PK | - | ID interno |
| `study_instance_uid` | VARCHAR(128) UNIQUE | (0020,000D) | UID del estudio |
| `accession_number` | VARCHAR(64) | (0008,0050) | Número de acceso |
| `patient_id` | BIGINT FK | - | Paciente |
| `requested_procedure_id` | BIGINT FK | - | Procedimiento solicitado |
| `study_date` | DATE | (0008,0020) | Fecha del estudio |
| `study_time` | TIME | (0008,0030) | Hora del estudio |
| `study_description` | VARCHAR(255) | (0008,1030) | Descripción |
| `modalities_in_study` | VARCHAR(128) | (0008,0061) | Lista de modalidades (CT\MR) |
| `number_of_series` | INT | (0020,1206) | Número de series |
| `number_of_instances` | INT | (0020,1208) | Número de imágenes |
| `referring_physician_id` | BIGINT FK | - | Médico referente |
| `reading_physician_id` | BIGINT FK | - | Radiólogo asignado |
| `study_status` | VARCHAR(16) | - | SCHEDULED/RECEIVED/READING/PRELIMINARY/FINAL/AMENDED |
| `storage_status` | VARCHAR(16) | - | PENDING/STORED/COMMITTED/ARCHIVED |
| `pacs_url` | VARCHAR(512) | - | URL del visor PACS |
| `study_size_bytes` | BIGINT | - | Tamaño en bytes |
| `created_at` | TIMESTAMP | - | Fecha de creación |
| `updated_at` | TIMESTAMP | - | Última actualización |

### Índices
- UNIQUE: `study_instance_uid`
- INDEX: `accession_number`, `patient_id`, `study_date`, `study_status`

---

## 8. REPORT (Reporte Radiológico)

Reporte final del radiólogo.

### Campos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `report_id` | BIGINT PK | ID interno |
| `study_id` | BIGINT FK | Estudio relacionado |
| `requested_procedure_id` | BIGINT FK | Procedimiento solicitado |
| `report_template_id` | BIGINT FK NULL | Plantilla usada (opcional) |
| `report_status` | VARCHAR(16) | PENDING/DRAFT/PRELIMINARY/FINAL/AMENDED/CORRECTED |
| `dictation_text` | TEXT | Transcripción/borrador |
| `final_report_text` | TEXT | Reporte final completo |
| `findings` | TEXT | Hallazgos |
| `impression` | TEXT | Impresión/Conclusión |
| `recommendations` | TEXT | Recomendaciones |
| `comparison_text` | TEXT | Comparación con estudios previos |
| `technique_text` | TEXT | Técnica utilizada |
| `indication_text` | TEXT | Indicación clínica |
| `dictated_by_user_id` | BIGINT FK | Usuario que dictó |
| `dictated_at` | TIMESTAMP | Fecha de dictado |
| `transcribed_by_user_id` | BIGINT FK | Usuario que transcribió |
| `transcribed_at` | TIMESTAMP | Fecha de transcripción |
| `verified_by_user_id` | BIGINT FK | Usuario que verificó/firmó |
| `verified_at` | TIMESTAMP | Fecha de verificación |
| `amended_by_user_id` | BIGINT FK | Usuario que enmendó |
| `amended_at` | TIMESTAMP | Fecha de enmienda |
| `amendment_reason` | TEXT | Razón de la enmienda |
| `locked_by_user_id` | BIGINT FK | Usuario que tiene el lock (concurrency) |
| `locked_at` | TIMESTAMP | Fecha del lock |
| `report_sr_instance_uid` | VARCHAR(128) | DICOM SR UID (si se genera) |
| `signature_data` | TEXT | Firma digital (base64) |
| `signature_date_time` | TIMESTAMP | Fecha de firma |
| `created_at` | TIMESTAMP | Fecha de creación |
| `updated_at` | TIMESTAMP | Última actualización |

### Estados de Report (`report_status`)
- `PENDING`: No iniciado
- `DRAFT`: Borrador en progreso
- `PRELIMINARY`: Reporte preliminar emitido
- `FINAL`: Reporte final firmado
- `AMENDED`: Enmendado después de final
- `CORRECTED`: Corregido
- `ADDENDUM`: Adenda agregada

### Índices
- INDEX: `study_id`, `report_status`, `dictated_by_user_id`, `verified_by_user_id`

---

## 9. REPORT_TEMPLATE (Plantilla de Reporte)

Plantillas reutilizables para reportes.

### Campos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `template_id` | BIGINT PK | ID interno |
| `template_name` | VARCHAR(255) | Nombre de la plantilla |
| `template_category` | VARCHAR(64) | Categoría (por modalidad/body part) |
| `modality` | VARCHAR(16) | CT/MR/CR/etc (NULL = todas) |
| `body_part` | VARCHAR(64) | CHEST/HEAD/ABDOMEN/etc |
| `procedure_code_id` | BIGINT FK NULL | Procedimiento específico (opcional) |
| `template_text` | TEXT | Texto de la plantilla con placeholders |
| `template_format` | VARCHAR(16) | PLAIN_TEXT/HTML/MARKDOWN |
| `sections` | JSON | Estructura de secciones (findings, impression, etc) |
| `macros` | JSON | Macros/snippets disponibles |
| `created_by_user_id` | BIGINT FK | Usuario creador |
| `is_active` | BOOLEAN | Activa/inactiva |
| `is_default` | BOOLEAN | Plantilla por defecto para este tipo |
| `usage_count` | INT | Contador de uso |
| `created_at` | TIMESTAMP | Fecha de creación |
| `updated_at` | TIMESTAMP | Última actualización |

### Ejemplo de Template
```text
TECHNIQUE: {modality} {body_part} examination performed with {protocol}.

COMPARISON: {comparison}

FINDINGS:
- {findings}

IMPRESSION:
1. {impression}

RECOMMENDATIONS:
{recommendations}
```

### Índices
- INDEX: `modality`, `body_part`, `is_active`

---

## 10. PROCEDURE_CATALOG (Catálogo de Procedimientos)

Catálogo de procedimientos de imagen disponibles.

### Campos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `procedure_code_id` | BIGINT PK | ID interno |
| `procedure_code` | VARCHAR(64) UNIQUE | Código del procedimiento |
| `procedure_name` | VARCHAR(255) | Nombre |
| `procedure_description` | TEXT | Descripción detallada |
| `modality` | VARCHAR(16) | CT/MR/CR/etc |
| `body_part` | VARCHAR(64) | CHEST/HEAD/ABDOMEN/SPINE/etc |
| `laterality` | VARCHAR(16) | LEFT/RIGHT/BILATERAL/UNSPECIFIED |
| `cpt_code` | VARCHAR(16) | Current Procedural Terminology |
| `loinc_code` | VARCHAR(16) | Logical Observation Identifiers Names and Codes |
| `radlex_code` | VARCHAR(16) | RadLex RID |
| `snomed_code` | VARCHAR(16) | SNOMED CT |
| `estimated_duration_minutes` | INT | Duración estimada |
| `contrast_required` | VARCHAR(16) | NO/OPTIONAL/REQUIRED |
| `special_instructions` | TEXT | Instrucciones especiales |
| `patient_preparation` | TEXT | Preparación del paciente |
| `dose_reference_level` | DECIMAL(10,2) | DRL (mGy) |
| `price` | DECIMAL(10,2) | Precio |
| `is_active` | BOOLEAN | Activo/inactivo |
| `created_at` | TIMESTAMP | Fecha de creación |
| `updated_at` | TIMESTAMP | Última actualización |

### Ejemplos
```sql
INSERT INTO procedure_catalog (procedure_code, procedure_name, modality, body_part, cpt_code) VALUES
('XR-CHEST-PA-LAT', 'Chest X-Ray PA and Lateral', 'CR', 'CHEST', '71020'),
('CT-HEAD-NO-CONTRAST', 'CT Head without Contrast', 'CT', 'HEAD', '70450'),
('MR-KNEE-LEFT', 'MRI Left Knee', 'MR', 'KNEE', '73721');
```

### Índices
- UNIQUE: `procedure_code`
- INDEX: `modality`, `body_part`, `is_active`

---

## 11. PHYSICIAN (Médico/Radiólogo)

Médicos referentes y radiólogos.

### Campos

| Campo | Tipo | DICOM Tag | Descripción |
|-------|------|-----------|-------------|
| `physician_id` | BIGINT PK | - | ID interno |
| `physician_name` | VARCHAR(255) | (0008,0090) | Nombre (PN format) |
| `specialty` | VARCHAR(64) | - | RADIOLOGY/CARDIOLOGY/NEUROLOGY/etc |
| `sub_specialty` | VARCHAR(64) | - | NEURORADIOLOGY/MSK/etc |
| `license_number` | VARCHAR(64) | - | Número de licencia médica |
| `npi` | VARCHAR(16) | - | National Provider Identifier |
| `phone` | VARCHAR(64) | - | Teléfono |
| `email` | VARCHAR(255) | - | Email |
| `signature_image` | TEXT | - | Firma escaneada (base64) |
| `digital_signature_cert` | TEXT | - | Certificado de firma digital |
| `is_active` | BOOLEAN | - | Activo/inactivo |
| `created_at` | TIMESTAMP | - | Fecha de creación |
| `updated_at` | TIMESTAMP | - | Última actualización |

### Índices
- INDEX: `specialty`, `is_active`

---

## 12. USER (Usuario del Sistema)

Usuarios del sistema RIS.

### Campos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `user_id` | BIGINT PK | ID interno |
| `username` | VARCHAR(64) UNIQUE | Username |
| `password_hash` | VARCHAR(255) | Hash de contraseña (bcrypt) |
| `full_name` | VARCHAR(255) | Nombre completo |
| `email` | VARCHAR(255) UNIQUE | Email |
| `phone` | VARCHAR(64) | Teléfono |
| `role` | VARCHAR(32) | Ver roles abajo |
| `physician_id` | BIGINT FK NULL | Si es médico/radiólogo |
| `signature` | TEXT | Firma para reportes |
| `preferences` | JSON | Preferencias del usuario |
| `is_active` | BOOLEAN | Activo/inactivo |
| `last_login` | TIMESTAMP | Último login |
| `failed_login_attempts` | INT | Intentos fallidos |
| `account_locked_until` | TIMESTAMP | Bloqueo temporal |
| `created_at` | TIMESTAMP | Fecha de creación |
| `updated_at` | TIMESTAMP | Última actualización |

### Roles de Usuario
- `ADMIN`: Administrador del sistema
- `RADIOLOGIST`: Radiólogo (lee y reporta estudios)
- `RESIDENT`: Residente de radiología
- `TECHNOLOGIST`: Tecnólogo/Operador de modalidad
- `RECEPTIONIST`: Recepcionista (crea órdenes)
- `REFERRING_PHYSICIAN`: Médico referente (puede ver reportes)
- `TRANSCRIPTIONIST`: Transcriptor
- `NURSE`: Enfermera

### Índices
- UNIQUE: `username`, `email`
- INDEX: `role`, `is_active`

---

## 13. MODALITY (Modalidad DICOM)

Modalidades/equipos DICOM configurados.

### Campos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `modality_id` | BIGINT PK | ID interno |
| `ae_title` | VARCHAR(16) UNIQUE | AE Title |
| `modality_type` | VARCHAR(16) | CT/MR/CR/DX/US/NM/PT/MG/etc |
| `manufacturer` | VARCHAR(64) | Fabricante |
| `model_name` | VARCHAR(64) | Modelo |
| `station_name` | VARCHAR(64) | Nombre de la estación |
| `ip_address` | VARCHAR(64) | IP address |
| `dicom_port` | INT | Puerto DICOM (default 104) |
| `description` | VARCHAR(255) | Descripción |
| `location` | VARCHAR(255) | Ubicación física |
| `department_id` | BIGINT FK | Departamento |
| `is_active` | BOOLEAN | Activa/inactiva |
| `supports_mwl` | BOOLEAN | Soporta Modality Worklist |
| `supports_mpps` | BOOLEAN | Soporta MPPS |
| `supports_storage_commitment` | BOOLEAN | Soporta Storage Commitment |
| `default_protocol` | VARCHAR(64) | Protocolo por defecto |
| `created_at` | TIMESTAMP | Fecha de creación |
| `updated_at` | TIMESTAMP | Última actualización |

### Índices
- UNIQUE: `ae_title`
- INDEX: `modality_type`, `is_active`

---

## 14. DEPARTMENT (Departamento)

Departamentos del hospital.

### Campos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `department_id` | BIGINT PK | ID interno |
| `department_code` | VARCHAR(32) UNIQUE | Código del departamento |
| `department_name` | VARCHAR(255) | Nombre |
| `department_type` | VARCHAR(64) | RADIOLOGY/CARDIOLOGY/etc |
| `facility_id` | BIGINT FK | Instalación/Hospital |
| `is_active` | BOOLEAN | Activo/inactivo |
| `created_at` | TIMESTAMP | Fecha de creación |
| `updated_at` | TIMESTAMP | Última actualización |

---

## 15. AUDIT_LOG (Log de Auditoría)

Registro completo de auditoría para cumplimiento HIPAA/normativas.

### Campos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `audit_id` | BIGINT PK | ID interno |
| `event_type` | VARCHAR(64) | PATIENT_VIEW/REPORT_SIGN/ORDER_CREATE/etc |
| `event_action` | VARCHAR(32) | CREATE/READ/UPDATE/DELETE |
| `user_id` | BIGINT FK | Usuario que realizó la acción |
| `patient_id` | BIGINT FK NULL | Paciente afectado (si aplica) |
| `study_id` | BIGINT FK NULL | Estudio afectado (si aplica) |
| `order_id` | BIGINT FK NULL | Orden afectada (si aplica) |
| `entity_type` | VARCHAR(64) | Tipo de entidad afectada |
| `entity_id` | BIGINT | ID de la entidad |
| `event_date_time` | TIMESTAMP | Fecha/hora del evento |
| `ip_address` | VARCHAR(64) | IP del usuario |
| `user_agent` | VARCHAR(255) | User agent del navegador |
| `session_id` | VARCHAR(255) | ID de sesión |
| `details` | JSON | Detalles adicionales |
| `success` | BOOLEAN | Éxito/Fallo |
| `error_message` | TEXT | Mensaje de error (si aplica) |

### Índices
- INDEX: `event_type`, `user_id`, `event_date_time`, `patient_id`

---

## Relaciones Principales

### One-to-Many
- `PATIENT` → `VISIT` (1:N)
- `PATIENT` → `ORDER` (1:N)
- `VISIT` → `ORDER` (1:N)
- `ORDER` → `REQUESTED_PROCEDURE` (1:N)
- `REQUESTED_PROCEDURE` → `SPS` (1:N)
- `SPS` → `PPS` (1:1 o 1:N si hay re-intentos)
- `REQUESTED_PROCEDURE` → `STUDY` (1:1)
- `STUDY` → `REPORT` (1:N, puede haber preliminary y final)

### Many-to-One
- Múltiples entidades → `PHYSICIAN` (referente, lector, etc.)
- Múltiples entidades → `USER`
- `SPS` → `MODALITY`
- `REQUESTED_PROCEDURE` → `PROCEDURE_CATALOG`
- `REPORT` → `REPORT_TEMPLATE` (opcional)

---

## Diagrama ER Simplificado

```
┌─────────────┐
│   PATIENT   │
└──────┬──────┘
       │ 1:N
┌──────┴──────┐
│    VISIT    │
└──────┬──────┘
       │ 1:N
┌──────┴──────┐         ┌──────────────┐
│    ORDER    │────────>│  PHYSICIAN   │
└──────┬──────┘  N:1    └──────────────┘
       │ 1:N
┌──────┴──────────────┐     ┌──────────────────┐
│ REQUESTED_PROCEDURE │────>│ PROCEDURE_CATALOG│
└──────┬──────────────┘ N:1 └──────────────────┘
       │ 1:N
┌──────┴──────┐         ┌──────────┐
│     SPS     │────────>│ MODALITY │
└──────┬──────┘  N:1    └──────────┘
       │ 1:1
┌──────┴──────┐
│     PPS     │
└──────┬──────┘
       │ 1:1
┌──────┴──────┐
│    STUDY    │
└──────┬──────┘
       │ 1:N
┌──────┴──────┐     ┌──────────────────┐
│   REPORT    │────>│ REPORT_TEMPLATE  │
└─────────────┘ N:1 └──────────────────┘
```

---

## Consideraciones de Implementación

### 1. Generación de UIDs y Números

```java
// Study Instance UID
String studyUID = UIDUtils.createUID();

// Accession Number (año + secuencial)
String accessionNumber = String.format("%d%08d",
    LocalDate.now().getYear(),
    sequenceService.getNextAccessionNumber());

// Filler Order Number
String fillerOrderNumber = String.format("RIS%d%08d",
    LocalDate.now().getYear(),
    sequenceService.getNextOrderNumber());
```

### 2. Validaciones de Transición de Estados

```java
public enum OrderStatus {
    PENDING, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELED;

    public boolean canTransitionTo(OrderStatus newStatus) {
        switch (this) {
            case PENDING:
                return newStatus == SCHEDULED || newStatus == CANCELED;
            case SCHEDULED:
                return newStatus == IN_PROGRESS || newStatus == CANCELED;
            case IN_PROGRESS:
                return newStatus == COMPLETED || newStatus == DISCONTINUED;
            case COMPLETED:
                return newStatus == AMENDED;
            case CANCELED:
                return false; // No se puede salir de CANCELED
            default:
                return false;
        }
    }
}
```

### 3. Concurrency Control para Reportes

```java
// Intentar obtener lock
@Transactional
public boolean lockReport(Long reportId, Long userId) {
    int updated = reportRepository.lockReport(reportId, userId,
        LocalDateTime.now().minusMinutes(30)); // Lock expira en 30 min
    return updated > 0;
}

// SQL
UPDATE report
SET locked_by_user_id = ?, locked_at = NOW()
WHERE report_id = ?
  AND (locked_by_user_id IS NULL
       OR locked_at < NOW() - INTERVAL 30 MINUTE)
```

### 4. Indexación para Performance

Las consultas más comunes serán:
- Worklist por modalidad y fecha: `INDEX(scheduled_station_ae_title, scheduled_start_date_time)`
- Búsqueda de pacientes: `INDEX(patient_name, mrn, birth_date)`
- Reportes pendientes: `INDEX(report_status, dictated_by_user_id)`
- Estudios por accession: `UNIQUE INDEX(accession_number)`

### 5. Particionamiento

Para grandes volúmenes, considerar particionar tablas grandes por fecha:
```sql
-- Particionar AUDIT_LOG por mes
CREATE TABLE audit_log_2025_01 PARTITION OF audit_log
FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
```

---

## Scripts SQL de Creación

Ver archivos en `/dcm4che-ris-persistence/src/main/resources/db/migration/`:
- `V1__initial_schema.sql`
- `V2__indexes.sql`
- `V3__sample_data.sql`

---

## Próximos Pasos

1. ✅ Modelo de datos documentado
2. [ ] Crear entidades JPA
3. [ ] Crear repositories
4. [ ] Crear servicios CRUD
5. [ ] Implementar validaciones de workflow
6. [ ] Crear scripts de migración SQL
