# dcm4che-ris - Radiology Information System Extension

Extensión RIS completa para dcm4che, siguiendo el estándar IHE Scheduled Workflow Profile.

## Visión General

dcm4che-ris extiende la biblioteca dcm4che DICOM con funcionalidades completas de RIS (Radiology Information System), permitiendo:

- Gestión completa de pacientes y órdenes de imágenes
- Integración DICOM (Modality Worklist, MPPS, Storage Commitment)
- Integración HL7 v2.x (ADT, ORM, OMI, ORU)
- Sistema de reportes radiológicos con plantillas
- Workflow de estados (Scheduled → In Progress → Completed)
- REST API para interfaces web
- Multi-tenancy y auditoría

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                     WEB UI (React/Vue)                       │
│              Worklist │ Reporting │ Admin                    │
└──────────────────────────┬──────────────────────────────────┘
                           │ REST API
┌──────────────────────────┴──────────────────────────────────┐
│              dcm4che-ris-rest (Spring Boot)                  │
│              Controllers │ DTOs │ Security                   │
└──┬────────────────────────────────────────────────────────┬──┘
   │                                                        │
┌──┴────────────────────────┐              ┌────────────────┴──┐
│   dcm4che-ris-dicom       │              │  dcm4che-ris-hl7  │
│   - MWL Service (C-FIND)  │              │  - ADT Listener   │
│   - MPPS Service          │              │  - ORM Listener   │
│   - Storage Commitment    │              │  - ORU Sender     │
└──┬────────────────────────┘              └────────────────┬──┘
   │                                                        │
┌──┴────────────────────────────────────────────────────────┴──┐
│              dcm4che-ris-core (Business Logic)               │
│   OrderService │ WorklistService │ ReportService             │
│   PatientService │ SPSService │ StudyService                 │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────┴──────────────────────────────────┐
│              dcm4che-ris-api (Entities & Interfaces)         │
│   Patient │ Order │ SPS │ PPS │ Report │ Template           │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────┴──────────────────────────────────┐
│         dcm4che-ris-persistence (JPA/Hibernate)              │
│              PostgreSQL / MySQL Database                     │
└─────────────────────────────────────────────────────────────┘
```

## Modelo de Datos

Basado en **IHE Scheduled Workflow Profile**:

```
PATIENT
  └── VISIT
        └── IMAGING_SERVICE_REQUEST (Order)
              └── REQUESTED_PROCEDURE
                    └── SCHEDULED_PROCEDURE_STEP (SPS)
                          └── PERFORMED_PROCEDURE_STEP (PPS)
                                └── STUDY
                                      └── REPORT
```

### Entidades Principales

1. **Patient**: Datos demográficos del paciente
2. **Visit**: Visita/Admisión hospitalaria
3. **Order**: Orden de imagen (Imaging Service Request)
4. **RequestedProcedure**: Procedimiento solicitado
5. **ScheduledProcedureStep (SPS)**: Paso de procedimiento programado
6. **PerformedProcedureStep (PPS)**: Paso de procedimiento realizado
7. **Study**: Estudio DICOM
8. **Report**: Reporte radiológico
9. **ReportTemplate**: Plantillas de reportes
10. **ProcedureCatalog**: Catálogo de procedimientos
11. **Physician**: Médicos/Radiólogos
12. **User**: Usuarios del sistema
13. **Modality**: Modalidades DICOM

Ver [DATA_MODEL.md](docs/DATA_MODEL.md) para detalles completos.

## Módulos

### dcm4che-ris-api
Interfaces, DTOs y entidades de dominio. Sin dependencias de implementación.

### dcm4che-ris-core
Lógica de negocio, servicios, workflows.

### dcm4che-ris-dicom
Servicios DICOM:
- **MWL Service**: C-FIND SCP para Modality Worklist
- **MPPS Service**: N-CREATE/N-SET SCP para Modality Performed Procedure Step
- **Storage Commitment**: Confirmación de almacenamiento

### dcm4che-ris-hl7
Integración HL7 v2.x:
- **ADT Messages**: A01, A08, A40 (Patient demographics)
- **ORM/OMI Messages**: Imaging orders
- **ORU Messages**: Results (reportes)

### dcm4che-ris-rest
REST API con Spring Boot para interfaces web.

### dcm4che-ris-persistence
Capa de persistencia con JPA/Hibernate y migraciones de base de datos.

### dcm4che-ris-web (opcional)
Interfaz web de usuario con React/Vue.

## Integración con dcm4che

dcm4che-ris se integra con dcm4che mediante el patrón **DeviceExtension**:

```java
Device device = new Device("RIS-DEVICE");
RISDeviceExtension risExt = new RISDeviceExtension();
device.addDeviceExtension(risExt);

ApplicationEntity ae = new ApplicationEntity("RIS-AE");
RISAEExtension risAE = new RISAEExtension();
ae.addAEExtension(risAE);

// Configurar servicios DICOM
ae.addTransferCapability(new TransferCapability(/* MWL */));
ae.addTransferCapability(new TransferCapability(/* MPPS */));
```

## Estados de Workflow

### Order Status
- `PENDING`: Orden creada, no programada
- `SCHEDULED`: Al menos un SPS programado
- `IN_PROGRESS`: Al menos un SPS en progreso
- `COMPLETED`: Todos los SPS completados
- `CANCELED`: Cancelado

### SPS Status
- `SCHEDULED`: Programado
- `ARRIVED`: Paciente llegó
- `READY`: Listo para realizar
- `STARTED`: En progreso
- `COMPLETED`: Completado
- `CANCELED`: Cancelado
- `DISCONTINUED`: Interrumpido

### Report Status
- `PENDING`: No iniciado
- `DRAFT`: Borrador en progreso
- `PRELIMINARY`: Preliminar emitido
- `FINAL`: Final firmado
- `AMENDED`: Enmendado
- `CORRECTED`: Corregido

## Roadmap de Desarrollo

### Fase 1: Fundamentos ✓ (Actual)
- [x] Análisis de arquitectura
- [x] Investigación de proyectos RIS existentes
- [x] Diseño de modelo de datos
- [ ] Estructura de módulos Maven
- [ ] Entidades JPA básicas
- [ ] Schema de base de datos

### Fase 2: Core Services
- [ ] PatientService (CRUD)
- [ ] OrderService (crear, modificar, cancelar)
- [ ] SPSService (scheduling)
- [ ] WorklistService (queries)
- [ ] StudyService (tracking)

### Fase 3: DICOM Integration
- [ ] Modality Worklist Service (C-FIND SCP)
- [ ] MPPS Service (N-CREATE/N-SET SCP)
- [ ] Storage Commitment
- [ ] Integración con dcm4che-net

### Fase 4: HL7 Integration
- [ ] ADT Message Listener (A01, A08, A40)
- [ ] ORM/OMI Message Listener (órdenes)
- [ ] ORU Message Sender (reportes)
- [ ] Integración con dcm4che-hl7

### Fase 5: Reporting System
- [ ] Report Templates
- [ ] Report Editor Service
- [ ] Workflow (draft → preliminary → final)
- [ ] Concurrency control (lock mechanism)
- [ ] Digital signatures

### Fase 6: REST API
- [ ] Spring Boot setup
- [ ] Controllers (Patient, Order, Worklist, Report)
- [ ] Security (JWT, OAuth2)
- [ ] API Documentation (Swagger/OpenAPI)

### Fase 7: Advanced Features
- [ ] Multi-tenancy
- [ ] Audit logging completo
- [ ] Statistics y reporting
- [ ] Billing integration
- [ ] Web UI

## Requisitos

- **Java**: 17+ (LTS)
- **dcm4che**: 5.34.2+
- **Spring Boot**: 3.x
- **Database**: PostgreSQL 14+ o MySQL 8+
- **Build**: Maven 3.8+

## Licencia

Siguiendo dcm4che: MPL 1.1 / GPL 2.0 / LGPL 2.1

## Contribuir

Ver [CONTRIBUTING.md](docs/CONTRIBUTING.md)

## Referencias

- [IHE Scheduled Workflow Profile](https://www.ihe.net/uploadedFiles/Documents/Radiology/IHE_RAD_TF_Vol1.pdf)
- [DICOM Standard PS 3.4 - Service Class Specifications](https://dicom.nema.org/medical/dicom/current/output/html/part04.html)
- [HL7 v2.5.1 Imaging Integration](https://www.hl7.org/implement/standards/product_brief.cfm?product_id=144)
- [dcm4che Documentation](https://github.com/dcm4che/dcm4che)
