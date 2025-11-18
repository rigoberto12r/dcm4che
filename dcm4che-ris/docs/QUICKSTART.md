# Quick Start Guide

This guide will help you get dcm4che-ris up and running in under 10 minutes using Docker Compose.

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- 4GB RAM minimum
- 10GB disk space

## Step 1: Clone the Repository

```bash
git clone https://github.com/dcm4che/dcm4che.git
cd dcm4che/dcm4che-ris
```

## Step 2: Start the Services

```bash
# Start all services (PostgreSQL + RIS)
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f ris-rest
```

## Step 3: Verify Installation

### Database

```bash
# Check database connection
docker-compose exec postgres psql -U ris_user -d ris_db -c "\dt"
```

You should see all RIS tables created by Liquibase.

### REST API

```bash
# Check API health
curl http://localhost:8080/api/ris/actuator/health

# Access Swagger UI
open http://localhost:8080/api/ris/swagger-ui.html
```

### DICOM MWL

```bash
# Test DICOM C-FIND (requires dcm4che tools)
dcmqrscp --bind RIS_SCP@localhost:11112 --query \
  -m ScheduledStationAETitle=CT_01
```

### HL7 Server

```bash
# Test HL7 connection (requires hl7 tools or telnet)
telnet localhost 2575
```

## Step 4: Create Test Data

### Create a Patient

```bash
curl -X POST http://localhost:8080/api/ris/v1/patients \
  -H "Content-Type: application/json" \
  -d '{
    "mrn": "TEST001",
    "issuerOfPatientId": "HOSPITAL",
    "patientName": "TEST^PATIENT^A",
    "birthDate": "1980-01-15",
    "sex": "M",
    "phoneNumbers": "555-1234",
    "email": "test@example.com"
  }'
```

### Create a Physician

```bash
curl -X POST http://localhost:8080/api/ris/v1/physicians \
  -H "Content-Type: application/json" \
  -d '{
    "physicianName": "DR. SMITH^JOHN",
    "npi": "1234567890",
    "email": "drsmith@hospital.com"
  }'
```

### Create an Order

```bash
curl -X POST http://localhost:8080/api/ris/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "patient": {"patientId": 1},
    "requestingPhysician": {"physicianId": 1},
    "placerOrderNumber": "ORD-001",
    "orderPriority": "ROUTINE",
    "reasonForExam": "Chest pain",
    "clinicalInfo": "Patient complaining of chest pain"
  }'
```

### Schedule a Procedure

```bash
curl -X POST http://localhost:8080/api/ris/v1/scheduling \
  -H "Content-Type: application/json" \
  -d '{
    "requestedProcedure": {"requestedProcedureId": 1},
    "scheduledStartDateTime": "2025-01-20T10:00:00",
    "scheduledModality": "CT",
    "scheduledStationAETitle": "CT_01",
    "scheduledPerformingPhysician": {"physicianId": 1}
  }'
```

## Step 5: Test Worklist

```bash
# Query MWL using dcm4che tools
dcmqrscp --bind RIS_SCP@localhost:11112 --query \
  -m ScheduledStationAETitle=CT_01 \
  -m Modality=CT \
  -m ScheduledProcedureStepStartDate=20250120
```

You should see the scheduled procedure in the worklist response.

## Step 6: Test HL7 Integration

### Send ADT Message (Patient Registration)

Create file `adt_a04.hl7`:

```
MSH|^~\&|HIS|HOSPITAL|RIS|HOSPITAL|20250118120000||ADT^A04|MSG001|P|2.5
PID|||TEST002^^^HOSPITAL^MR||JOHNSON^MARY^L||19750520|F|||456 ELM ST^^CITYVILLE^CA^54321||555-5678
```

Send via HL7 client:

```bash
# Using hl7tools (example)
hl7send localhost 2575 < adt_a04.hl7
```

### Send ORM Message (New Order)

Create file `orm_o01.hl7`:

```
MSH|^~\&|HIS|HOSPITAL|RIS|HOSPITAL|20250118120000||ORM^O01|MSG002|P|2.5
PID|||TEST001^^^HOSPITAL^MR||TEST^PATIENT^A||19800115|M
ORC|NW|ORD-002|||||||20250118120000
OBR||||XRAY CHEST PA^Chest X-Ray PA|||||||||||DR. SMITH||||||XR|||ROUTINE
```

Send:

```bash
hl7send localhost 2575 < orm_o01.hl7
```

## Step 7: Access Management Tools

### pgAdmin (Database Management)

```
URL: http://localhost:5050
Email: admin@ris.local
Password: admin
```

Add server:
- Host: postgres
- Port: 5432
- Database: ris_db
- Username: ris_user
- Password: ris_password

### Swagger UI (API Documentation)

```
URL: http://localhost:8080/api/ris/swagger-ui.html
```

## Common Operations

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f ris-rest

# Database logs
docker-compose logs -f postgres
```

### Restart Services

```bash
# Restart all
docker-compose restart

# Restart specific service
docker-compose restart ris-rest
```

### Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes all data)
docker-compose down -v
```

### Update Code

```bash
# Pull latest code
git pull

# Rebuild and restart
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Access Database

```bash
# psql shell
docker-compose exec postgres psql -U ris_user -d ris_db

# Run SQL file
docker-compose exec -T postgres psql -U ris_user -d ris_db < script.sql
```

### Backup Database

```bash
# Backup
docker-compose exec -T postgres pg_dump -U ris_user ris_db > backup.sql

# Restore
docker-compose exec -T postgres psql -U ris_user -d ris_db < backup.sql
```

## Troubleshooting

### Service Won't Start

```bash
# Check logs
docker-compose logs ris-rest

# Check service health
docker-compose ps
```

### Database Connection Error

```bash
# Verify database is running
docker-compose ps postgres

# Check database logs
docker-compose logs postgres

# Test connection
docker-compose exec postgres psql -U ris_user -d ris_db -c "SELECT 1"
```

### Port Already in Use

Edit `docker-compose.yml` and change the port mapping:

```yaml
ports:
  - "8081:8080"  # Change 8080 to 8081
```

### Out of Memory

Increase memory limits in `docker-compose.yml`:

```yaml
environment:
  JAVA_OPTS: "-Xms512m -Xmx4096m"  # Increase from 2GB to 4GB
```

## Next Steps

1. **Explore the API**: Use Swagger UI to explore all available endpoints
2. **Configure Security**: Enable authentication in application.yml
3. **Integrate with Modalities**: Configure modalities to query MWL
4. **Connect HIS/EMR**: Configure HL7 sender to send ADT/ORM messages
5. **Customize Templates**: Create custom report templates
6. **Set up Backup**: Configure automated database backups
7. **Monitor Performance**: Set up monitoring with Prometheus/Grafana

## Production Deployment

For production deployment, see:
- [DEPLOYMENT.md](./DEPLOYMENT.md) - Production deployment guide
- [SECURITY.md](./SECURITY.md) - Security hardening guide
- [PERFORMANCE.md](./PERFORMANCE.md) - Performance tuning guide

## Support

- Documentation: [/docs](../docs)
- Issues: [GitHub Issues](https://github.com/dcm4che/dcm4che/issues)
- Community: [dcm4che Forum](https://groups.google.com/g/dcm4che)
