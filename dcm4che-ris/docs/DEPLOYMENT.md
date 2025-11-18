# Production Deployment Guide

This guide covers deploying dcm4che-ris to production environments with high availability, security, and performance.

## Table of Contents

1. [Infrastructure Requirements](#infrastructure-requirements)
2. [Production Architecture](#production-architecture)
3. [Database Setup](#database-setup)
4. [Application Configuration](#application-configuration)
5. [SSL/TLS Configuration](#ssltls-configuration)
6. [Load Balancing](#load-balancing)
7. [High Availability](#high-availability)
8. [Deployment Strategies](#deployment-strategies)
9. [Health Checks](#health-checks)
10. [Disaster Recovery](#disaster-recovery)

---

## Infrastructure Requirements

### Minimum Requirements

**Application Server:**
- CPU: 4 cores (8+ recommended)
- RAM: 8GB (16GB+ recommended)
- Disk: 100GB SSD
- OS: Linux (Ubuntu 22.04 LTS or RHEL 8+)

**Database Server:**
- CPU: 4 cores (8+ recommended)
- RAM: 16GB (32GB+ recommended)
- Disk: 500GB SSD (RAID 10)
- OS: Linux (Ubuntu 22.04 LTS or RHEL 8+)

**Network:**
- 1 Gbps network connectivity minimum
- Low latency between app and database (<5ms)
- Firewall with restricted access

### Recommended Production Architecture

```
                          ┌─────────────────┐
                          │   Load Balancer │
                          │   (nginx/HAProxy)│
                          └────────┬─────────┘
                                   │
                ┌──────────────────┼──────────────────┐
                │                  │                  │
        ┌───────▼───────┐  ┌──────▼──────┐  ┌───────▼───────┐
        │  RIS Node 1   │  │  RIS Node 2 │  │  RIS Node 3   │
        │  (Active)     │  │  (Active)   │  │  (Active)     │
        └───────┬───────┘  └──────┬──────┘  └───────┬───────┘
                │                  │                  │
                └──────────────────┼──────────────────┘
                                   │
                          ┌────────▼─────────┐
                          │  PostgreSQL      │
                          │  Primary + Replica│
                          └──────────────────┘
```

---

## Production Architecture

### Option 1: Docker Swarm (Recommended for Small-Medium)

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    deploy:
      replicas: 1
      placement:
        constraints:
          - node.role == manager
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
    environment:
      POSTGRES_DB: ris_db
      POSTGRES_USER: ris_user
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
    secrets:
      - db_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - ris-internal

  ris-rest:
    image: dcm4che/ris:1.0.0
    deploy:
      replicas: 3
      update_config:
        parallelism: 1
        delay: 30s
        failure_action: rollback
      restart_policy:
        condition: on-failure
        delay: 10s
        max_attempts: 3
    environment:
      SPRING_PROFILES_ACTIVE: production
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ris_db
      SPRING_DATASOURCE_PASSWORD_FILE: /run/secrets/db_password
    secrets:
      - db_password
      - ssl_keystore
    networks:
      - ris-internal
      - ris-external

  nginx:
    image: nginx:alpine
    deploy:
      replicas: 2
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    networks:
      - ris-external
    depends_on:
      - ris-rest

volumes:
  postgres_data:
    driver: local

networks:
  ris-internal:
    driver: overlay
    internal: true
  ris-external:
    driver: overlay

secrets:
  db_password:
    external: true
  ssl_keystore:
    external: true
```

### Option 2: Kubernetes (Enterprise)

```yaml
# k8s-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ris-rest
  namespace: dcm4che-ris
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: ris-rest
  template:
    metadata:
      labels:
        app: ris-rest
        version: v1.0.0
    spec:
      containers:
      - name: ris-rest
        image: dcm4che/ris:1.0.0
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        - containerPort: 11112
          name: dicom
        - containerPort: 2575
          name: hl7
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: ris-config
              key: database.url
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ris-secrets
              key: db-password
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /api/ris/actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /api/ris/actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        volumeMounts:
        - name: config
          mountPath: /app/config
          readOnly: true
      volumes:
      - name: config
        configMap:
          name: ris-config
---
apiVersion: v1
kind: Service
metadata:
  name: ris-rest-service
  namespace: dcm4che-ris
spec:
  type: LoadBalancer
  ports:
  - port: 443
    targetPort: 8080
    name: https
  - port: 11112
    targetPort: 11112
    name: dicom
  - port: 2575
    targetPort: 2575
    name: hl7
  selector:
    app: ris-rest
```

---

## Database Setup

### PostgreSQL Production Configuration

**postgresql.conf:**

```ini
# Connections
max_connections = 200
superuser_reserved_connections = 3

# Memory
shared_buffers = 8GB                    # 25% of RAM
effective_cache_size = 24GB             # 75% of RAM
maintenance_work_mem = 2GB
work_mem = 64MB
huge_pages = try

# WAL
wal_buffers = 16MB
min_wal_size = 2GB
max_wal_size = 8GB
checkpoint_completion_target = 0.9
checkpoint_timeout = 15min

# Query Tuning
random_page_cost = 1.1                  # SSD
effective_io_concurrency = 200          # SSD
default_statistics_target = 500

# Logging
log_min_duration_statement = 1000       # Log queries > 1s
log_line_prefix = '%t [%p]: user=%u,db=%d,app=%a,client=%h '
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on

# Autovacuum
autovacuum = on
autovacuum_max_workers = 4
autovacuum_naptime = 10s
```

**pg_hba.conf:**

```ini
# TYPE  DATABASE    USER        ADDRESS         METHOD
local   all         postgres                    peer
local   all         all                         peer

# Production connections
hostssl ris_db      ris_user    10.0.0.0/8      scram-sha-256
hostssl replication ris_user    10.0.0.0/8      scram-sha-256

# Reject all other
host    all         all         0.0.0.0/0       reject
```

### Database Replication Setup

**Primary Server:**

```sql
-- Create replication user
CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'strong_password';

-- Configure replication slots
SELECT * FROM pg_create_physical_replication_slot('replica_1_slot');
```

**postgresql.conf (Primary):**

```ini
wal_level = replica
max_wal_senders = 5
max_replication_slots = 5
hot_standby = on
```

**Replica Server (recovery.conf):**

```ini
standby_mode = on
primary_conninfo = 'host=primary_host port=5432 user=replicator password=strong_password'
primary_slot_name = 'replica_1_slot'
```

---

## Application Configuration

### application-production.yml

```yaml
spring:
  datasource:
    # Connection Pool (HikariCP)
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1800000
      leak-detection-threshold: 60000

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        # Performance
        jdbc.batch_size: 20
        order_inserts: true
        order_updates: true
        jdbc.batch_versioned_data: true

        # Second-level cache
        cache.use_second_level_cache: true
        cache.use_query_cache: true
        cache.region.factory_class: org.hibernate.cache.jcache.JCacheRegionFactory

        # Statistics (disable in production)
        generate_statistics: false

  # Actuator
  management:
    endpoints:
      web:
        exposure:
          include: health,metrics,prometheus,info
    endpoint:
      health:
        show-details: when-authorized
    metrics:
      export:
        prometheus:
          enabled: true

# RIS Configuration
ris:
  dicom:
    ae-title: ${DICOM_AE_TITLE:RIS_PROD}
    hostname: ${DICOM_HOSTNAME:0.0.0.0}
    port: ${DICOM_PORT:11112}
    max-associations: 100
    tls-enabled: true
    keystore-path: ${SSL_KEYSTORE_PATH:/app/ssl/keystore.jks}
    keystore-password: ${SSL_KEYSTORE_PASSWORD}
    calling-ae-title-whitelist: ${DICOM_WHITELIST:}

  hl7:
    tls-enabled: true
    sending-application-whitelist: ${HL7_WHITELIST:}

  rest:
    security:
      enabled: true
      jwt-secret: ${JWT_SECRET}
      jwt-expiration: 3600000  # 1 hour

# Logging
logging:
  level:
    root: INFO
    org.dcm4che.ris: INFO
    org.hibernate.SQL: WARN
  file:
    name: /app/logs/ris.log
    max-size: 100MB
    max-history: 30
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Server
server:
  port: 8080
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
  http2:
    enabled: true
  tomcat:
    max-threads: 200
    min-spare-threads: 20
    max-connections: 10000
    accept-count: 100
```

---

## SSL/TLS Configuration

### Generate SSL Certificates

```bash
# 1. Generate private key
openssl genrsa -out server.key 4096

# 2. Generate CSR
openssl req -new -key server.key -out server.csr \
  -subj "/C=US/ST=State/L=City/O=Hospital/CN=ris.hospital.com"

# 3. Generate self-signed certificate (or use CA-signed)
openssl x509 -req -days 365 -in server.csr \
  -signkey server.key -out server.crt

# 4. Create Java keystore
keytool -importcert -file server.crt -keystore keystore.jks \
  -alias ris-server -storepass changeit

# 5. Import private key
openssl pkcs12 -export -in server.crt -inkey server.key \
  -out server.p12 -name ris-server -passout pass:changeit

keytool -importkeystore -srckeystore server.p12 -srcstoretype PKCS12 \
  -destkeystore keystore.jks -deststoretype JKS \
  -srcstorepass changeit -deststorepass changeit
```

### NGINX SSL Configuration

```nginx
# /etc/nginx/nginx.conf
upstream ris_backend {
    least_conn;
    server ris-rest-1:8080 max_fails=3 fail_timeout=30s;
    server ris-rest-2:8080 max_fails=3 fail_timeout=30s;
    server ris-rest-3:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name ris.hospital.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name ris.hospital.com;

    # SSL Configuration
    ssl_certificate /etc/nginx/ssl/server.crt;
    ssl_certificate_key /etc/nginx/ssl/server.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Proxy Configuration
    location /api/ris {
        proxy_pass http://ris_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        # WebSocket support (if needed)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # Health check endpoint
    location /health {
        access_log off;
        proxy_pass http://ris_backend/api/ris/actuator/health;
    }
}
```

---

## Load Balancing

### HAProxy Configuration

```haproxy
# /etc/haproxy/haproxy.cfg
global
    log /dev/log local0
    log /dev/log local1 notice
    chroot /var/lib/haproxy
    stats socket /run/haproxy/admin.sock mode 660 level admin
    stats timeout 30s
    user haproxy
    group haproxy
    daemon

    # SSL
    ssl-default-bind-ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256
    ssl-default-bind-options ssl-min-ver TLSv1.2 no-tls-tickets

defaults
    log     global
    mode    http
    option  httplog
    option  dontlognull
    timeout connect 5000
    timeout client  50000
    timeout server  50000
    errorfile 400 /etc/haproxy/errors/400.http
    errorfile 403 /etc/haproxy/errors/403.http
    errorfile 408 /etc/haproxy/errors/408.http
    errorfile 500 /etc/haproxy/errors/500.http
    errorfile 502 /etc/haproxy/errors/502.http
    errorfile 503 /etc/haproxy/errors/503.http
    errorfile 504 /etc/haproxy/errors/504.http

frontend ris_https
    bind *:443 ssl crt /etc/haproxy/ssl/ris.pem
    mode http
    option forwardfor
    http-request add-header X-Forwarded-Proto https
    default_backend ris_servers

frontend ris_dicom
    bind *:11112
    mode tcp
    option tcplog
    default_backend ris_dicom_servers

frontend ris_hl7
    bind *:2575
    mode tcp
    option tcplog
    default_backend ris_hl7_servers

backend ris_servers
    mode http
    balance leastconn
    option httpchk GET /api/ris/actuator/health
    http-check expect status 200

    server ris1 10.0.1.10:8080 check inter 2000 rise 2 fall 3
    server ris2 10.0.1.11:8080 check inter 2000 rise 2 fall 3
    server ris3 10.0.1.12:8080 check inter 2000 rise 2 fall 3

backend ris_dicom_servers
    mode tcp
    balance roundrobin
    option tcp-check

    server ris1 10.0.1.10:11112 check
    server ris2 10.0.1.11:11112 check
    server ris3 10.0.1.12:11112 check

backend ris_hl7_servers
    mode tcp
    balance roundrobin
    option tcp-check

    server ris1 10.0.1.10:2575 check
    server ris2 10.0.1.11:2575 check
    server ris3 10.0.1.12:2575 check

listen stats
    bind *:8404
    stats enable
    stats uri /stats
    stats refresh 10s
    stats admin if TRUE
```

---

## High Availability

### Database HA with Patroni

```yaml
# patroni.yml
scope: ris-postgres
name: postgres1

restapi:
  listen: 0.0.0.0:8008
  connect_address: 10.0.1.5:8008

etcd:
  hosts: 10.0.1.20:2379,10.0.1.21:2379,10.0.1.22:2379

bootstrap:
  dcs:
    ttl: 30
    loop_wait: 10
    retry_timeout: 10
    maximum_lag_on_failover: 1048576
    postgresql:
      use_pg_rewind: true
      parameters:
        max_connections: 200
        shared_buffers: 8GB
        effective_cache_size: 24GB

  initdb:
    - encoding: UTF8
    - data-checksums

  pg_hba:
    - host replication replicator 10.0.0.0/8 md5
    - host all all 10.0.0.0/8 md5

postgresql:
  listen: 0.0.0.0:5432
  connect_address: 10.0.1.5:5432
  data_dir: /var/lib/postgresql/16/main
  pgpass: /tmp/pgpass
  authentication:
    replication:
      username: replicator
      password: repl_password
    superuser:
      username: postgres
      password: postgres_password
  parameters:
    wal_level: replica
    hot_standby: on
    max_wal_senders: 10
    max_replication_slots: 10
    wal_keep_size: 1GB
```

---

## Deployment Strategies

### Blue-Green Deployment

```bash
#!/bin/bash
# blue-green-deploy.sh

BLUE_CONTAINERS="ris-blue-1 ris-blue-2 ris-blue-3"
GREEN_CONTAINERS="ris-green-1 ris-green-2 ris-green-3"
CURRENT_ENV=$(cat /var/run/current_env)

if [ "$CURRENT_ENV" == "blue" ]; then
    NEW_ENV="green"
    NEW_CONTAINERS=$GREEN_CONTAINERS
    OLD_CONTAINERS=$BLUE_CONTAINERS
else
    NEW_ENV="blue"
    NEW_CONTAINERS=$BLUE_CONTAINERS
    OLD_CONTAINERS=$GREEN_CONTAINERS
fi

echo "Deploying to $NEW_ENV environment..."

# Start new containers
docker-compose -f docker-compose.$NEW_ENV.yml up -d

# Wait for health checks
for container in $NEW_CONTAINERS; do
    echo "Waiting for $container to be healthy..."
    while ! docker inspect --format='{{.State.Health.Status}}' $container | grep -q healthy; do
        sleep 5
    done
done

# Switch load balancer
echo "Switching traffic to $NEW_ENV..."
cp nginx.$NEW_ENV.conf /etc/nginx/sites-enabled/ris.conf
nginx -s reload

# Wait for in-flight requests
sleep 30

# Stop old containers
docker-compose -f docker-compose.$CURRENT_ENV.yml down

# Update current environment
echo $NEW_ENV > /var/run/current_env

echo "Deployment to $NEW_ENV complete!"
```

### Rolling Update (Kubernetes)

```bash
# Update image
kubectl set image deployment/ris-rest \
  ris-rest=dcm4che/ris:1.0.1 \
  -n dcm4che-ris

# Monitor rollout
kubectl rollout status deployment/ris-rest -n dcm4che-ris

# Rollback if needed
kubectl rollout undo deployment/ris-rest -n dcm4che-ris
```

---

## Health Checks

### Application Health Endpoints

```java
// Custom health indicator
@Component
public class DicomHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            // Check DICOM server
            if (dicomServer.isRunning()) {
                return Health.up()
                    .withDetail("dicom.port", dicomConfiguration.getPort())
                    .withDetail("dicom.aeTitle", dicomConfiguration.getAeTitle())
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down().build();
    }
}
```

### Monitoring Script

```bash
#!/bin/bash
# health-check.sh

API_URL="http://localhost:8080/api/ris/actuator/health"
TIMEOUT=10

response=$(curl -s -o /dev/null -w "%{http_code}" --max-time $TIMEOUT $API_URL)

if [ "$response" == "200" ]; then
    echo "OK: Application is healthy"
    exit 0
else
    echo "CRITICAL: Application health check failed (HTTP $response)"
    exit 2
fi
```

---

## Disaster Recovery

### Backup Strategy

```bash
#!/bin/bash
# backup-database.sh

BACKUP_DIR=/backups/postgresql
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/ris_db_$DATE.sql.gz"
RETENTION_DAYS=30

# Create backup
pg_dump -U ris_user -h localhost ris_db | gzip > $BACKUP_FILE

# Upload to S3
aws s3 cp $BACKUP_FILE s3://ris-backups/database/

# Clean old backups
find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete

echo "Backup completed: $BACKUP_FILE"
```

### Disaster Recovery Plan

1. **RTO (Recovery Time Objective)**: 1 hour
2. **RPO (Recovery Point Objective)**: 15 minutes

**Recovery Procedure:**

```bash
# 1. Restore database from latest backup
gunzip -c /backups/postgresql/ris_db_latest.sql.gz | \
  psql -U ris_user -d ris_db

# 2. Start application servers
docker-compose up -d

# 3. Verify health
curl http://localhost:8080/api/ris/actuator/health

# 4. Restore network routing
# (update DNS or load balancer configuration)
```

---

## Checklist

### Pre-Deployment

- [ ] SSL certificates configured
- [ ] Database backups automated
- [ ] Monitoring configured
- [ ] Log aggregation setup
- [ ] Security hardening complete
- [ ] Load testing performed
- [ ] Disaster recovery tested
- [ ] Documentation updated

### Post-Deployment

- [ ] Health checks passing
- [ ] Monitoring dashboards working
- [ ] Logs being collected
- [ ] Backups running
- [ ] Performance metrics baseline
- [ ] Security scan completed
- [ ] Team notified

---

## Support

For production issues:
- On-call: [oncall@hospital.com](mailto:oncall@hospital.com)
- Documentation: [Wiki](https://wiki.hospital.com/ris)
- Monitoring: [Dashboard](https://monitoring.hospital.com/ris)
