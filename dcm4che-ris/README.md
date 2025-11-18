# dcm4che-ris

Radiology Information System (RIS) extension for [dcm4che](https://github.com/dcm4che/dcm4che) - A comprehensive Java-based RIS implementation with DICOM, HL7, and REST API support.

[![License: MPL 1.1](https://img.shields.io/badge/License-MPL%201.1-blue.svg)](https://www.mozilla.org/en-US/MPL/1.1/)
[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-blue.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html)
[![License: LGPL v2.1](https://img.shields.io/badge/License-LGPL%20v2.1-blue.svg)](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html)

## 🏥 Overview

dcm4che-ris is a complete, enterprise-grade Radiology Information System built on top of dcm4che 5.x. It provides comprehensive workflow management for radiology departments, including patient registration, order management, procedure scheduling, reporting, and full DICOM/HL7 integration.

### Key Features

- **🔐 Patient Management**: Complete patient demographics with MRN, visits, and clinical data
- **📋 Order Management**: Imaging service requests with procedure tracking and status management
- **📅 Procedure Scheduling**: Advanced scheduling with modality worklist support
- **📝 Reporting**: Full report lifecycle from draft to signed with addendum support
- **🔌 DICOM Integration**: Modality Worklist (MWL) and Modality Performed Procedure Step (MPPS)
- **📡 HL7 Integration**: ADT and ORM message handlers for HIS/EMR integration
- **🌐 REST API**: Complete RESTful API with OpenAPI/Swagger documentation
- **💾 Database Support**: PostgreSQL and MySQL with Liquibase migrations
- **🏗️ Modular Architecture**: Clean separation of concerns with Spring Boot

## 📐 Architecture

```
dcm4che-ris/
│
├── dcm4che-ris-api/              # Entity definitions and enums
│   ├── entity/                   # JPA entities (16 entities)
│   └── enums/                    # Status enums and constants (12 enums)
│
├── dcm4che-ris-persistence/      # Data access layer
│   ├── repository/               # Spring Data JPA repositories (14 repos)
│   └── db/changelog/             # Liquibase database migrations (13 changesets)
│
├── dcm4che-ris-core/             # Business logic layer
│   ├── service/                  # Service implementations (7 services)
│   └── exception/                # Custom exceptions
│
├── dcm4che-ris-dicom/            # DICOM services
│   ├── scp/                      # DICOM SCP implementations
│   │   ├── MWLQuerySCP          # Modality Worklist C-FIND
│   │   └── MPPSServiceSCP       # MPPS N-CREATE/N-SET
│   └── config/                   # DICOM configuration
│
├── dcm4che-ris-hl7/              # HL7 v2.x integration
│   ├── handler/                  # HL7 message handlers
│   │   ├── ADTHandler           # Patient demographics (A01, A04, A08, A40)
│   │   └── ORMHandler           # Order messages (O01)
│   └── service/                  # HL7 message processing
│
└── dcm4che-ris-rest/             # REST API
    ├── controller/               # REST controllers (4 controllers, 52 endpoints)
    ├── dto/                      # Data transfer objects
    └── config/                   # REST/CORS/OpenAPI configuration
```

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 12+ or MySQL 8+
- Docker and Docker Compose (optional, for containerized deployment)

### Database Setup

#### PostgreSQL

```sql
CREATE DATABASE ris_db;
CREATE USER ris_user WITH PASSWORD 'ris_password';
GRANT ALL PRIVILEGES ON DATABASE ris_db TO ris_user;
```

#### MySQL

```sql
CREATE DATABASE ris_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ris_user'@'%' IDENTIFIED BY 'ris_password';
GRANT ALL PRIVILEGES ON ris_db.* TO 'ris_user'@'%';
FLUSH PRIVILEGES;
```

### Build from Source

```bash
# Clone repository
git clone https://github.com/dcm4che/dcm4che.git
cd dcm4che/dcm4che-ris

# Build all modules
mvn clean install

# Run database migrations
mvn liquibase:update -pl dcm4che-ris-persistence
```

### Configuration

Create `application.yml` in your runtime directory:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ris_db
    username: ris_user
    password: ris_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    enabled: true

# RIS DICOM Configuration
ris:
  dicom:
    ae-title: RIS_SCP
    hostname: 0.0.0.0
    port: 11112
    mwl-enabled: true
    mpps-enabled: true

# RIS HL7 Configuration
  hl7:
    application-name: RIS
    facility-name: HOSPITAL
    hostname: 0.0.0.0
    port: 2575
    adt-enabled: true
    orm-enabled: true
    auto-create-patients: true
    auto-create-orders: true

# REST API Configuration
server:
  port: 8080
  servlet:
    context-path: /api/ris
```

### Run the Application

```bash
# Option 1: Run REST API module
java -jar dcm4che-ris-rest/target/dcm4che-ris-rest-1.0.0-SNAPSHOT.jar

# Option 2: Using Docker Compose (recommended)
docker-compose up -d
```

### Access the Application

- **REST API**: http://localhost:8080/api/ris
- **Swagger UI**: http://localhost:8080/api/ris/swagger-ui.html
- **DICOM MWL**: DICOM C-FIND on port 11112 (AE Title: RIS_SCP)
- **HL7 Server**: HL7 v2.x on port 2575

## 📚 Core Entities

### Patient
- Medical Record Number (MRN)
- Demographics (name, DOB, gender, contact info)
- Patient state and allergies
- Visit history

### Visit
- Visit number and type
- Admission/discharge dates
- Attending physician
- Department and location

### Imaging Service Request (Order)
- Placer and filler order numbers
- Order status and priority
- Requesting physician
- Clinical information
- Requested procedures

### Scheduled Procedure Step (SPS)
- Scheduled date/time and duration
- Modality and station AE Title
- Procedure code and description
- SPS status (SCHEDULED, STARTED, COMPLETED, etc.)
- Contrast agent information

### Report
- Report content and template
- Report status (DRAFT, PRELIMINARY, FINAL, VERIFIED)
- Author and verifying physician
- Signature timestamps
- Addendums and amendments

## 🔌 Integration

### DICOM Services

#### Modality Worklist (MWL)

The RIS provides a C-FIND SCP for Modality Worklist queries:

```bash
# Query worklist using dcm4che tools
dcmqrscp --bind RIS_SCP@localhost:11112 --query \
  -m ScheduledStationAETitle=CT_01 \
  -m Modality=CT \
  -m ScheduledProcedureStepStartDate=20250118
```

**Supported Matching Keys:**
- Scheduled Station AE Title
- Modality
- Scheduled Procedure Step Start Date
- Patient ID, Name
- Accession Number

#### Modality Performed Procedure Step (MPPS)

The RIS accepts MPPS N-CREATE and N-SET messages:

- **N-CREATE**: Procedure started (IN PROGRESS status)
- **N-SET**: Procedure completed or discontinued

MPPS automatically updates the corresponding SPS status.

### HL7 Integration

#### ADT Messages (Patient Demographics)

Supported trigger events:
- **A01**: Admit/Visit Notification
- **A04**: Register a Patient
- **A05**: Pre-Admit a Patient
- **A08**: Update Patient Information
- **A40**: Merge Patient (partial support)

Example ADT^A04 message:
```
MSH|^~\&|HIS|HOSPITAL|RIS|HOSPITAL|20250118120000||ADT^A04|MSG001|P|2.5
PID|||12345678^^^HOSPITAL^MR||DOE^JOHN^A||19800115|M|||123 MAIN ST^^ANYTOWN^CA^12345||555-1234
```

#### ORM Messages (Radiology Orders)

Supported trigger events:
- **O01**: General Order Message

Supported order controls:
- **NW**: New order
- **CA**: Cancel order

Example ORM^O01 message:
```
MSH|^~\&|HIS|HOSPITAL|RIS|HOSPITAL|20250118120000||ORM^O01|MSG002|P|2.5
PID|||12345678^^^HOSPITAL^MR||DOE^JOHN^A||19800115|M
ORC|NW|ORD-001|||||||20250118120000
OBR||||CT CHEST^CT Chest with Contrast|||||||||||DR. SMITH||||||CT|||ROUTINE
```

### REST API

#### Patient Endpoints

```bash
# Create patient
POST /api/ris/v1/patients
Content-Type: application/json

{
  "mrn": "12345678",
  "patientName": "DOE^JOHN^A",
  "birthDate": "1980-01-15",
  "sex": "M"
}

# Search patients
GET /api/ris/v1/patients?name=DOE&gender=M

# Get patient by ID
GET /api/ris/v1/patients/{patientId}
```

#### Order Endpoints

```bash
# Create order
POST /api/ris/v1/orders
Content-Type: application/json

{
  "patient": {"patientId": 1},
  "placerOrderNumber": "ORD-001",
  "orderPriority": "ROUTINE",
  "requestingPhysician": {"physicianId": 1},
  "reasonForExam": "Chest pain"
}

# Get pending orders
GET /api/ris/v1/orders/pending

# Update order status
PATCH /api/ris/v1/orders/{orderId}/status?status=IN_PROGRESS
```

#### Scheduling Endpoints

```bash
# Schedule procedure
POST /api/ris/v1/scheduling
Content-Type: application/json

{
  "requestedProcedure": {"requestedProcedureId": 1},
  "scheduledStartDateTime": "2025-01-18T14:00:00",
  "scheduledModality": "CT",
  "scheduledStationAETitle": "CT_01"
}

# Get today's schedule
GET /api/ris/v1/scheduling/today

# Get schedule by modality
GET /api/ris/v1/scheduling/modality/CT
```

#### Reporting Endpoints

```bash
# Create report
POST /api/ris/v1/reports
Content-Type: application/json

{
  "scheduledProcedureStep": {"spsId": 1},
  "reportContent": "Preliminary findings...",
  "reportStatus": "DRAFT"
}

# Sign report
POST /api/ris/v1/reports/{reportId}/sign?physicianId=1

# Add addendum
POST /api/ris/v1/reports/{reportId}/addendum?content=Additional findings...&physicianId=1
```

## 🔧 Configuration

### DICOM Configuration

```yaml
ris:
  dicom:
    ae-title: RIS_SCP                    # Application Entity Title
    hostname: 0.0.0.0                    # Bind address
    port: 11112                          # DICOM port
    max-associations: 50                 # Max concurrent associations

    # MWL configuration
    mwl-enabled: true
    mwl-date-range-days: 7              # Default query range
    mwl-max-results: 1000               # Max results per query

    # MPPS configuration
    mpps-enabled: true

    # TLS (optional)
    tls-enabled: false
    tls-protocols: TLSv1.2,TLSv1.3

    # Security (optional)
    # calling-ae-title-whitelist: MODALITY1,MODALITY2

    # Logging
    log-dicom-messages: false
```

### HL7 Configuration

```yaml
ris:
  hl7:
    application-name: RIS
    facility-name: HOSPITAL
    hostname: 0.0.0.0
    port: 2575

    # Message handlers
    adt-enabled: true                   # Patient demographics
    orm-enabled: true                   # Radiology orders
    oru-enabled: false                  # Results (future)

    # Auto-create settings
    auto-create-patients: true
    auto-create-orders: true

    # Security (optional)
    # sending-application-whitelist: HIS,EMR

    # Default issuer of patient ID
    default-issuer-of-patient-id: HOSPITAL

    # HL7 settings
    character-encoding: UTF-8
    hl7-version: "2.5"
    log-hl7-messages: false
```

### REST API Configuration

```yaml
server:
  port: 8080
  servlet:
    context-path: /api/ris

ris:
  rest:
    # CORS
    cors:
      enabled: true
      allowed-origins: "*"
      allowed-methods: "GET,POST,PUT,DELETE,PATCH,OPTIONS"

    # Pagination
    pagination:
      default-page-size: 20
      max-page-size: 100

    # Security (optional)
    security:
      enabled: false

# OpenAPI/Swagger
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

## 🐳 Docker Deployment

```bash
# Build images
docker-compose build

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f ris-rest

# Stop services
docker-compose down
```

## 📊 Database Schema

The RIS uses Liquibase for database migrations. The schema includes:

- **patient**: Patient demographics
- **visit**: Patient visits
- **physician**: Physician directory
- **imaging_service_request**: Radiology orders
- **requested_procedure**: Ordered procedures
- **scheduled_procedure_step**: Scheduled procedures
- **performed_procedure_step**: Performed procedures (MPPS)
- **report**: Radiology reports
- **report_template**: Report templates
- **procedure_code**: Procedure catalog
- **modality**: Modality catalog

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl dcm4che-ris-core

# Run integration tests
mvn verify -P integration-tests
```

## 📖 Documentation

- [API Documentation](./docs/API.md) - REST API reference
- [DICOM Integration](./docs/DICOM.md) - DICOM services guide
- [HL7 Integration](./docs/HL7.md) - HL7 message specifications
- [Configuration Guide](./docs/CONFIGURATION.md) - Detailed configuration options
- [Development Guide](./docs/DEVELOPMENT.md) - Developer documentation

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## 📄 License

This project is licensed under multiple licenses:
- Mozilla Public License Version 1.1 or later
- GNU General Public License v2 or later
- GNU Lesser General Public License v2.1 or later

See the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [dcm4che](https://www.dcm4che.org/) - DICOM toolkit
- [Spring Boot](https://spring.boot/) - Application framework
- [Liquibase](https://www.liquibase.org/) - Database migrations
- IHE Radiology Technical Framework

## 📞 Support

- Issues: [GitHub Issues](https://github.com/dcm4che/dcm4che/issues)
- Documentation: [Wiki](https://github.com/dcm4che/dcm4che/wiki)
- Community: [Forum](https://groups.google.com/g/dcm4che)

## 🗺️ Roadmap

### Completed ✅
- [x] Complete entity model (16 entities)
- [x] Persistence layer with Liquibase migrations
- [x] Core business services (7 services)
- [x] DICOM MWL and MPPS integration
- [x] HL7 ADT and ORM handlers
- [x] Complete REST API (52 endpoints)

### In Progress 🚧
- [ ] Unit and integration tests
- [ ] Docker deployment
- [ ] Production documentation

### Planned 📋
- [ ] ORU message support (results)
- [ ] FHIR integration
- [ ] Web-based UI
- [ ] Advanced reporting templates
- [ ] DICOM Storage SCP
- [ ] Query/Retrieve SCP
- [ ] Audit trail and ATNA support
- [ ] Multi-tenancy support
- [ ] Cloud deployment (AWS, Azure, GCP)

---

**Version**: 1.0.0-SNAPSHOT
**Built with**: Java 17, Spring Boot 3.2, dcm4che 5.34.2
**Status**: Active Development
**Lines of Code**: ~14,700 LOC
