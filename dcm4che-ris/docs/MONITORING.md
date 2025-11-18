# Monitoring and Observability Guide

This guide covers comprehensive monitoring and observability setup for dcm4che-ris using industry-standard tools.

## Table of Contents

1. [Observability Stack](#observability-stack)
2. [Metrics Collection](#metrics-collection)
3. [Logging](#logging)
4. [Tracing](#tracing)
5. [Dashboards](#dashboards)
6. [Alerting](#alerting)
7. [Health Checks](#health-checks)
8. [SLIs and SLOs](#slis-and-slos)

---

## Observability Stack

### Recommended Stack: Prometheus + Grafana + Loki

```
┌─────────────────────────────────────────────────────────┐
│                   Grafana                                │
│         (Visualization & Dashboards)                     │
└────────┬───────────┬────────────┬───────────────────────┘
         │           │            │
    ┌────▼────┐ ┌───▼────┐  ┌────▼─────┐
    │Prometheus│ │  Loki  │  │  Jaeger  │
    │(Metrics) │ │ (Logs) │  │(Tracing) │
    └────┬────┘ └───┬────┘  └────┬─────┘
         │          │             │
    ┌────▼──────────▼─────────────▼─────┐
    │        dcm4che-ris                 │
    │    (Application Instrumentation)   │
    └────────────────────────────────────┘
```

### Docker Compose for Monitoring Stack

```yaml
# docker-compose.monitoring.yml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus/alerts.yml:/etc/prometheus/alerts.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'
    ports:
      - "9090:9090"
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./grafana/datasources:/etc/grafana/provisioning/datasources
    ports:
      - "3000:3000"
    networks:
      - monitoring
    depends_on:
      - prometheus

  loki:
    image: grafana/loki:latest
    container_name: loki
    ports:
      - "3100:3100"
    volumes:
      - loki_data:/loki
      - ./loki/loki-config.yml:/etc/loki/local-config.yaml
    command: -config.file=/etc/loki/local-config.yaml
    networks:
      - monitoring

  promtail:
    image: grafana/promtail:latest
    container_name: promtail
    volumes:
      - /var/log:/var/log
      - ./promtail/promtail-config.yml:/etc/promtail/config.yml
      - ris_logs:/app/logs
    command: -config.file=/etc/promtail/config.yml
    networks:
      - monitoring

  jaeger:
    image: jaegertracing/all-in-one:latest
    container_name: jaeger
    environment:
      - COLLECTOR_ZIPKIN_HOST_PORT=:9411
    ports:
      - "5775:5775/udp"
      - "6831:6831/udp"
      - "6832:6832/udp"
      - "5778:5778"
      - "16686:16686"
      - "14268:14268"
      - "14250:14250"
      - "9411:9411"
    networks:
      - monitoring

  node-exporter:
    image: prom/node-exporter:latest
    container_name: node-exporter
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    networks:
      - monitoring

  postgres-exporter:
    image: prometheuscommunity/postgres-exporter:latest
    container_name: postgres-exporter
    environment:
      DATA_SOURCE_NAME: "postgresql://ris_user:ris_password@postgres:5432/ris_db?sslmode=disable"
    ports:
      - "9187:9187"
    networks:
      - monitoring

  alertmanager:
    image: prom/alertmanager:latest
    container_name: alertmanager
    volumes:
      - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager_data:/alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
    ports:
      - "9093:9093"
    networks:
      - monitoring

volumes:
  prometheus_data:
  grafana_data:
  loki_data:
  alertmanager_data:
  ris_logs:
    external: true

networks:
  monitoring:
    driver: bridge
```

---

## Metrics Collection

### Prometheus Configuration

```yaml
# prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

  external_labels:
    cluster: 'production'
    environment: 'prod'

# Alertmanager configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

# Load alert rules
rule_files:
  - 'alerts.yml'

# Scrape configurations
scrape_configs:
  # RIS Application
  - job_name: 'ris-rest'
    metrics_path: '/api/ris/actuator/prometheus'
    static_configs:
      - targets: ['ris-rest:8080']
        labels:
          application: 'ris'
          tier: 'application'

  # PostgreSQL
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
        labels:
          tier: 'database'

  # System metrics
  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']
        labels:
          tier: 'infrastructure'

  # Prometheus itself
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

### Application Metrics

```java
// Custom metrics in application
@Component
public class RisMetrics {

    private final MeterRegistry registry;

    // Counters
    private final Counter patientsCreated;
    private final Counter ordersCreated;
    private final Counter mwlQueries;
    private final Counter hl7Messages;

    // Gauges
    private final AtomicInteger activeSessions;
    private final AtomicInteger pendingOrders;

    // Timers
    private final Timer orderProcessingTime;
    private final Timer reportGenerationTime;

    public RisMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Initialize counters
        this.patientsCreated = Counter.builder("ris.patients.created")
            .description("Total number of patients created")
            .tag("type", "patient")
            .register(registry);

        this.ordersCreated = Counter.builder("ris.orders.created")
            .description("Total number of orders created")
            .tag("type", "order")
            .register(registry);

        this.mwlQueries = Counter.builder("ris.dicom.mwl.queries")
            .description("Total number of MWL queries")
            .tag("protocol", "dicom")
            .register(registry);

        this.hl7Messages = Counter.builder("ris.hl7.messages")
            .description("Total number of HL7 messages processed")
            .tag("protocol", "hl7")
            .register(registry);

        // Initialize gauges
        this.activeSessions = registry.gauge("ris.sessions.active",
            new AtomicInteger(0));

        this.pendingOrders = registry.gauge("ris.orders.pending",
            new AtomicInteger(0));

        // Initialize timers
        this.orderProcessingTime = Timer.builder("ris.orders.processing.time")
            .description("Time to process an order")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);

        this.reportGenerationTime = Timer.builder("ris.reports.generation.time")
            .description("Time to generate a report")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    // Usage
    public void recordPatientCreated() {
        patientsCreated.increment();
    }

    public void recordOrderProcessing(Runnable task) {
        orderProcessingTime.record(task);
    }
}
```

---

## Logging

### Logback Configuration

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- Console appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
            <customFields>{"application":"ris","environment":"production"}</customFields>
        </encoder>
    </appender>

    <!-- File appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/app/logs/ris.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/app/logs/ris-%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <!-- Async appender for performance -->
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <appender-ref ref="FILE"/>
    </appender>

    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC"/>
    </root>

    <!-- Application loggers -->
    <logger name="org.dcm4che.ris" level="INFO"/>
    <logger name="org.dcm4che.ris.dicom" level="DEBUG"/>
    <logger name="org.dcm4che.ris.hl7" level="DEBUG"/>

    <!-- External libraries -->
    <logger name="org.springframework" level="WARN"/>
    <logger name="org.hibernate" level="WARN"/>
</configuration>
```

### Structured Logging

```java
@Slf4j
@Service
public class OrderService {

    public Order createOrder(Order order) {
        // Structured logging with MDC
        MDC.put("orderId", order.getOrderId().toString());
        MDC.put("patientId", order.getPatient().getPatientId().toString());
        MDC.put("userId", SecurityContextHolder.getContext()
            .getAuthentication().getName());

        try {
            log.info("Creating order",
                kv("orderPriority", order.getOrderPriority()),
                kv("procedureCount", order.getProcedures().size()));

            // Business logic

            log.info("Order created successfully");
            return savedOrder;

        } catch (Exception e) {
            log.error("Failed to create order", e,
                kv("errorType", e.getClass().getSimpleName()));
            throw e;

        } finally {
            MDC.clear();
        }
    }
}
```

### Loki Configuration

```yaml
# loki/loki-config.yml
auth_enabled: false

server:
  http_listen_port: 3100

ingester:
  lifecycler:
    ring:
      kvstore:
        store: inmemory
      replication_factor: 1
  chunk_idle_period: 5m
  chunk_retain_period: 30s

schema_config:
  configs:
    - from: 2024-01-01
      store: boltdb
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 168h

storage_config:
  boltdb:
    directory: /loki/index

  filesystem:
    directory: /loki/chunks

limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h

chunk_store_config:
  max_look_back_period: 720h

table_manager:
  retention_deletes_enabled: true
  retention_period: 720h
```

### Promtail Configuration

```yaml
# promtail/promtail-config.yml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: ris-application
    static_configs:
      - targets:
          - localhost
        labels:
          job: ris
          __path__: /app/logs/*.log

    pipeline_stages:
      # Parse JSON logs
      - json:
          expressions:
            timestamp: timestamp
            level: level
            message: message
            logger: logger_name

      # Extract labels
      - labels:
          level:
          logger:

      # Parse timestamp
      - timestamp:
          source: timestamp
          format: RFC3339

      # Drop debug logs in production
      - match:
          selector: '{level="DEBUG"}'
          action: drop
```

---

## Tracing

### Distributed Tracing with Jaeger

```yaml
# application-production.yml
spring:
  application:
    name: ris

  # Zipkin/Jaeger integration
  zipkin:
    base-url: http://jaeger:9411
    sender:
      type: web

  sleuth:
    sampler:
      probability: 0.1  # Sample 10% of traces in production

management:
  tracing:
    sampling:
      probability: 0.1
```

```java
// Custom spans
@Service
@Slf4j
public class OrderService {

    @Autowired
    private Tracer tracer;

    public Order createOrder(Order order) {
        Span span = tracer.nextSpan().name("createOrder").start();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("order.priority", order.getOrderPriority());
            span.tag("patient.id", order.getPatient().getPatientId().toString());

            // Business logic
            Order created = orderRepository.save(order);

            span.tag("order.id", created.getOrderId().toString());
            return created;

        } catch (Exception e) {
            span.tag("error", e.getMessage());
            span.error(e);
            throw e;

        } finally {
            span.end();
        }
    }
}
```

---

## Dashboards

### Grafana Dashboard: RIS Overview

```json
{
  "dashboard": {
    "title": "dcm4che-ris Overview",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])"
          }
        ],
        "type": "graph"
      },
      {
        "title": "Response Time (p95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, http_server_requests_seconds_bucket)"
          }
        ],
        "type": "graph"
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])"
          }
        ],
        "type": "graph"
      },
      {
        "title": "Active Patients",
        "targets": [
          {
            "expr": "ris_patients_total"
          }
        ],
        "type": "stat"
      },
      {
        "title": "Pending Orders",
        "targets": [
          {
            "expr": "ris_orders_pending"
          }
        ],
        "type": "stat"
      },
      {
        "title": "MWL Queries",
        "targets": [
          {
            "expr": "rate(ris_dicom_mwl_queries_total[5m])"
          }
        ],
        "type": "graph"
      },
      {
        "title": "HL7 Messages",
        "targets": [
          {
            "expr": "rate(ris_hl7_messages_total[5m])"
          }
        ],
        "type": "graph"
      },
      {
        "title": "Database Connections",
        "targets": [
          {
            "expr": "hikaricp_connections_active"
          }
        ],
        "type": "graph"
      },
      {
        "title": "JVM Memory",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes"
          }
        ],
        "type": "graph"
      },
      {
        "title": "GC Pause Time",
        "targets": [
          {
            "expr": "rate(jvm_gc_pause_seconds_sum[5m])"
          }
        ],
        "type": "graph"
      }
    ]
  }
}
```

---

## Alerting

### Prometheus Alert Rules

```yaml
# prometheus/alerts.yml
groups:
  - name: ris_alerts
    interval: 30s
    rules:
      # Application alerts
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} req/s on {{ $labels.instance }}"

      - alert: HighResponseTime
        expr: histogram_quantile(0.95, http_server_requests_seconds_bucket) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time"
          description: "95th percentile response time is {{ $value }}s"

      - alert: LowThroughput
        expr: rate(http_server_requests_seconds_count[5m]) < 10
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Low request throughput"
          description: "Request rate is {{ $value }} req/s"

      # Database alerts
      - alert: DatabaseConnectionPoolExhaustion
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "{{ $value | humanizePercentage }} of connections are in use"

      - alert: SlowDatabaseQueries
        expr: rate(hikaricp_connections_timeout_total[5m]) > 0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Database queries timing out"
          description: "{{ $value }} connections timed out"

      # JVM alerts
      - alert: HighHeapMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High heap memory usage"
          description: "Heap memory usage is {{ $value | humanizePercentage }}"

      - alert: HighGCTime
        expr: rate(jvm_gc_pause_seconds_sum[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High GC time"
          description: "GC is taking {{ $value }}s per second"

      # DICOM/HL7 alerts
      - alert: MWLQueryFailures
        expr: rate(ris_dicom_mwl_failures_total[5m]) > 0.01
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "MWL queries failing"
          description: "{{ $value }} MWL queries failing per second"

      - alert: HL7MessageProcessingDelay
        expr: ris_hl7_message_processing_seconds > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "HL7 message processing delayed"
          description: "HL7 messages taking {{ $value }}s to process"

      # Infrastructure alerts
      - alert: HighCPUUsage
        expr: rate(node_cpu_seconds_total{mode!="idle"}[5m]) > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage"
          description: "CPU usage is {{ $value | humanizePercentage }}"

      - alert: LowDiskSpace
        expr: node_filesystem_avail_bytes / node_filesystem_size_bytes < 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Low disk space"
          description: "Only {{ $value | humanizePercentage }} disk space remaining"
```

### Alertmanager Configuration

```yaml
# alertmanager/alertmanager.yml
global:
  smtp_smarthost: 'smtp.hospital.com:587'
  smtp_from: 'ris-alerts@hospital.com'
  smtp_auth_username: 'ris-alerts'
  smtp_auth_password: 'password'

route:
  receiver: 'team-email'
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

  routes:
    # Critical alerts to PagerDuty
    - match:
        severity: critical
      receiver: pagerduty
      continue: true

    # Warnings to Slack
    - match:
        severity: warning
      receiver: slack

receivers:
  - name: 'team-email'
    email_configs:
      - to: 'ris-team@hospital.com'
        send_resolved: true

  - name: 'slack'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/...'
        channel: '#ris-alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: 'your-pagerduty-key'
        description: '{{ .GroupLabels.alertname }}'
```

---

## Health Checks

### Actuator Health Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

  endpoint:
    health:
      show-details: when-authorized
      show-components: when-authorized

  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true

    db:
      enabled: true
    diskSpace:
      enabled: true
    ping:
      enabled: true
```

### Custom Health Indicators

```java
@Component
public class DicomHealthIndicator implements HealthIndicator {

    @Autowired
    private DicomServer dicomServer;

    @Override
    public Health health() {
        try {
            if (dicomServer.isRunning()) {
                return Health.up()
                    .withDetail("dicom.status", "running")
                    .withDetail("dicom.port", dicomServer.getPort())
                    .build();
            } else {
                return Health.down()
                    .withDetail("dicom.status", "stopped")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

---

## SLIs and SLOs

### Service Level Indicators

```yaml
# SLIs (what we measure)
slis:
  availability:
    query: "up{job='ris-rest'}"
    threshold: 0.999  # 99.9%

  latency:
    query: "histogram_quantile(0.95, http_server_requests_seconds_bucket)"
    threshold: 0.5  # 500ms

  error_rate:
    query: "rate(http_server_requests_seconds_count{status=~'5..'}[5m])"
    threshold: 0.001  # 0.1%
```

### Service Level Objectives

```yaml
# SLOs (what we promise)
slos:
  - name: "API Availability"
    target: 99.9%
    window: 30d
    description: "API must be available 99.9% of the time"

  - name: "API Latency"
    target: 95% < 500ms
    window: 30d
    description: "95% of requests must complete in less than 500ms"

  - name: "API Error Rate"
    target: < 0.1%
    window: 30d
    description: "Less than 0.1% of requests should result in errors"
```

---

## Monitoring Checklist

### Setup
- [ ] Prometheus deployed and scraping metrics
- [ ] Grafana deployed with dashboards
- [ ] Loki/Promtail for log aggregation
- [ ] Jaeger for distributed tracing
- [ ] Alertmanager configured
- [ ] PagerDuty/Slack integration

### Metrics
- [ ] Application metrics exposed
- [ ] Database metrics collected
- [ ] System metrics monitored
- [ ] Custom business metrics tracked

### Dashboards
- [ ] Overview dashboard created
- [ ] Per-service dashboards
- [ ] Database dashboard
- [ ] Infrastructure dashboard

### Alerts
- [ ] Alert rules defined
- [ ] Alert routing configured
- [ ] On-call rotation setup
- [ ] Runbooks created

---

## Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Loki Documentation](https://grafana.com/docs/loki/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
