# Performance Tuning Guide

This guide covers performance optimization strategies for dcm4che-ris in production environments.

## Table of Contents

1. [Performance Monitoring](#performance-monitoring)
2. [JVM Tuning](#jvm-tuning)
3. [Database Optimization](#database-optimization)
4. [Caching Strategies](#caching-strategies)
5. [Connection Pooling](#connection-pooling)
6. [Query Optimization](#query-optimization)
7. [DICOM Performance](#dicom-performance)
8. [HL7 Performance](#hl7-performance)
9. [Load Testing](#load-testing)
10. [Troubleshooting](#troubleshooting)

---

## Performance Monitoring

### Key Performance Indicators (KPIs)

```yaml
# Target Performance Metrics
api:
  response_time_p95: <200ms    # 95th percentile
  response_time_p99: <500ms    # 99th percentile
  throughput: >1000 req/s      # Requests per second
  error_rate: <0.1%            # Error percentage

database:
  query_time_p95: <100ms
  connection_wait_time: <10ms
  active_connections: <80%

dicom:
  mwl_query_time: <500ms
  mpps_processing_time: <200ms

hl7:
  message_processing_time: <100ms
  ack_generation_time: <50ms

system:
  cpu_usage: <70%
  memory_usage: <80%
  disk_io_wait: <10%
```

### Enable Application Metrics

```yaml
# application-production.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus

  metrics:
    export:
      prometheus:
        enabled: true

    tags:
      application: ${spring.application.name}
      environment: production

    distribution:
      percentiles-histogram:
        http.server.requests: true

      slo:
        http.server.requests: 50ms,100ms,200ms,500ms,1s
```

---

## JVM Tuning

### Optimal JVM Settings

```bash
# Production JVM options
JAVA_OPTS="
  # Heap Size (adjust based on available RAM)
  -Xms4g
  -Xmx4g

  # Garbage Collector (G1GC for low latency)
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:ParallelGCThreads=8
  -XX:ConcGCThreads=2
  -XX:InitiatingHeapOccupancyPercent=45

  # GC Logging
  -Xlog:gc*:file=/app/logs/gc.log:time,uptime:filecount=10,filesize=100M

  # Memory Optimization
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat

  # Performance
  -XX:+AlwaysPreTouch
  -XX:+UseNUMA

  # Monitoring
  -XX:+UnlockDiagnosticVMOptions
  -XX:+PrintFlagsFinal

  # Remote JMX (for monitoring)
  -Dcom.sun.management.jmxremote
  -Dcom.sun.management.jmxremote.port=9010
  -Dcom.sun.management.jmxremote.local.only=false
  -Dcom.sun.management.jmxremote.authenticate=false
  -Dcom.sun.management.jmxremote.ssl=false
"
```

### Alternative: ZGC for Large Heaps (>8GB)

```bash
JAVA_OPTS="
  -Xms16g
  -Xmx16g

  # ZGC - Ultra-low latency GC
  -XX:+UseZGC
  -XX:ConcGCThreads=4
  -XX:ZAllocationSpikeTolerance=2

  # ZGC Logging
  -Xlog:gc*:file=/app/logs/gc.log:time,uptime:filecount=10,filesize=100M
"
```

### GC Tuning Guidelines

| Heap Size | GC Recommendation | Max Pause Target |
|-----------|-------------------|------------------|
| < 4GB     | G1GC              | 200ms            |
| 4-8GB     | G1GC              | 200ms            |
| 8-32GB    | ZGC               | 10ms             |
| > 32GB    | ZGC               | 10ms             |

---

## Database Optimization

### PostgreSQL Performance Tuning

#### postgresql.conf (Production)

```ini
# Memory
shared_buffers = 8GB                    # 25% of RAM
effective_cache_size = 24GB             # 75% of RAM
maintenance_work_mem = 2GB
work_mem = 64MB                         # Per operation

# Parallelism
max_worker_processes = 8
max_parallel_workers_per_gather = 4
max_parallel_workers = 8

# WAL
wal_buffers = 16MB
min_wal_size = 2GB
max_wal_size = 8GB
wal_compression = on

# Checkpoint
checkpoint_timeout = 15min
checkpoint_completion_target = 0.9

# Query Planning
random_page_cost = 1.1                  # SSD
effective_io_concurrency = 200          # SSD
default_statistics_target = 500

# Connection
max_connections = 200
```

### Index Optimization

```sql
-- Create appropriate indexes
CREATE INDEX CONCURRENTLY idx_patient_mrn
  ON patient(mrn);

CREATE INDEX CONCURRENTLY idx_patient_name_trgm
  ON patient USING gin(patient_name gin_trgm_ops);

CREATE INDEX CONCURRENTLY idx_order_status
  ON imaging_service_request(order_status);

CREATE INDEX CONCURRENTLY idx_sps_date
  ON scheduled_procedure_step(scheduled_start_date_time);

CREATE INDEX CONCURRENTLY idx_sps_modality
  ON scheduled_procedure_step(scheduled_modality);

-- Composite indexes for common queries
CREATE INDEX CONCURRENTLY idx_sps_modality_date
  ON scheduled_procedure_step(scheduled_modality, scheduled_start_date_time);

CREATE INDEX CONCURRENTLY idx_order_patient_status
  ON imaging_service_request(patient_id, order_status);
```

### Query Optimization

```sql
-- Enable query statistics
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Find slow queries
SELECT
  calls,
  mean_exec_time,
  max_exec_time,
  query
FROM pg_stat_statements
WHERE mean_exec_time > 100  -- queries > 100ms
ORDER BY mean_exec_time DESC
LIMIT 20;

-- Analyze query plans
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT * FROM patient WHERE mrn = '12345';

-- Update statistics
ANALYZE patient;
ANALYZE imaging_service_request;
ANALYZE scheduled_procedure_step;

-- Vacuum regularly
VACUUM ANALYZE;
```

### Partitioning for Large Tables

```sql
-- Partition by date for large tables
CREATE TABLE report_partitioned (
    report_id BIGSERIAL,
    created_at TIMESTAMP NOT NULL,
    -- other columns
) PARTITION BY RANGE (created_at);

-- Create monthly partitions
CREATE TABLE report_2025_01
  PARTITION OF report_partitioned
  FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE report_2025_02
  PARTITION OF report_partitioned
  FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

-- Automatic partition creation (using pg_partman extension)
```

---

## Caching Strategies

### Hibernate Second-Level Cache

```yaml
spring:
  jpa:
    properties:
      hibernate:
        # Enable second-level cache
        cache.use_second_level_cache: true
        cache.use_query_cache: true
        cache.region.factory_class: org.hibernate.cache.jcache.JCacheRegionFactory

        # Cache provider (Ehcache)
        javax.cache.provider: org.ehcache.jsr107.EhcacheCachingProvider
        javax.cache.uri: classpath:ehcache.xml
```

#### ehcache.xml

```xml
<config xmlns:jsr107="http://www.ehcache.org/v3/jsr107">
    <!-- Default cache configuration -->
    <cache alias="default">
        <expiry>
            <ttl unit="minutes">30</ttl>
        </expiry>
        <heap unit="entries">1000</heap>
    </cache>

    <!-- Entity caches -->
    <cache alias="org.dcm4che.ris.api.entity.Patient">
        <expiry>
            <ttl unit="hours">1</ttl>
        </expiry>
        <heap unit="entries">10000</heap>
    </cache>

    <cache alias="org.dcm4che.ris.api.entity.ProcedureCode">
        <expiry>
            <ttl unit="hours">24</ttl>
        </expiry>
        <heap unit="entries">5000</heap>
    </cache>

    <!-- Query cache -->
    <cache alias="query-cache">
        <expiry>
            <ttl unit="minutes">10</ttl>
        </expiry>
        <heap unit="entries">5000</heap>
    </cache>
</config>
```

### Redis Cache (Distributed)

```yaml
spring:
  cache:
    type: redis

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD}

    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 2000
```

```java
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Different TTLs for different caches
        cacheConfigurations.put("patients",
            config.entryTtl(Duration.ofHours(1)));

        cacheConfigurations.put("procedureCodes",
            config.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}

// Usage
@Service
public class PatientService {

    @Cacheable(value = "patients", key = "#patientId")
    public Patient getPatientById(Long patientId) {
        return patientRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
    }

    @CacheEvict(value = "patients", key = "#patient.patientId")
    public Patient updatePatient(Long patientId, Patient patient) {
        // Update logic
    }
}
```

---

## Connection Pooling

### HikariCP Optimization

```yaml
spring:
  datasource:
    hikari:
      # Pool sizing
      maximum-pool-size: 20              # Adjust based on load
      minimum-idle: 5

      # Connection lifecycle
      max-lifetime: 1800000              # 30 minutes
      idle-timeout: 600000               # 10 minutes
      connection-timeout: 20000          # 20 seconds

      # Leak detection
      leak-detection-threshold: 60000    # 60 seconds

      # Connection testing
      connection-test-query: SELECT 1
      validation-timeout: 5000

      # Performance
      auto-commit: false

      # Monitoring
      register-mbeans: true
```

### Pool Size Calculation

```
Optimal Pool Size = ((core_count * 2) + effective_spindle_count)

Example:
- 8 CPU cores
- SSD (effective spindle count = 1)
- Pool Size = (8 * 2) + 1 = 17 ≈ 20
```

---

## Query Optimization

### N+1 Query Problem

```java
// Bad - N+1 queries
@Entity
public class Order {
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<RequestedProcedure> procedures;
}

List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    // Each iteration triggers a separate query!
    order.getProcedures().size();
}

// Good - Use JOIN FETCH
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.procedures")
List<Order> findAllWithProcedures();

// Or use @EntityGraph
@EntityGraph(attributePaths = {"procedures"})
List<Order> findAll();
```

### Batch Processing

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc.batch_size: 20
        order_inserts: true
        order_updates: true
        jdbc.batch_versioned_data: true
```

```java
@Service
public class BatchImportService {

    @Transactional
    public void importPatients(List<Patient> patients) {
        int batchSize = 20;

        for (int i = 0; i < patients.size(); i++) {
            patientRepository.save(patients.get(i));

            if (i % batchSize == 0 && i > 0) {
                // Flush and clear to prevent OutOfMemoryError
                entityManager.flush();
                entityManager.clear();
            }
        }
    }
}
```

### Pagination

```java
// Use Pageable for large result sets
@RestController
public class OrderController {

    @GetMapping("/api/ris/v1/orders")
    public Page<Order> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
            Sort.by("createdAt").descending());

        return orderService.getOrders(pageable);
    }
}
```

---

## DICOM Performance

### MWL Query Optimization

```java
@Service
public class WorklistService {

    // Optimize MWL queries with indexes and caching
    @Cacheable(value = "mwl", key = "#modality + '_' + #date")
    public List<ScheduledProcedureStep> getMWLByModality(
            String modality, LocalDateTime date) {

        // Use indexed query
        return spsRepository.findByModalityAndDate(modality, date);
    }

    // Limit result size
    public List<ScheduledProcedureStep> getMWLByAETitle(
            String aeTitle, int maxResults) {

        Pageable limit = PageRequest.of(0, maxResults);
        return spsRepository.findByScheduledStationAETitle(aeTitle, limit);
    }
}
```

### DICOM Connection Pool

```java
@Configuration
public class DicomConfiguration {

    @Bean
    public ExecutorService dicomExecutor() {
        return new ThreadPoolExecutor(
            10,                             // Core pool size
            50,                             // Max pool size
            60L, TimeUnit.SECONDS,         // Keep alive time
            new LinkedBlockingQueue<>(100), // Queue size
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

---

## HL7 Performance

### Async Message Processing

```java
@Service
public class HL7MessageService {

    @Async("hl7Executor")
    public CompletableFuture<Patient> processADTMessageAsync(HL7Message msg) {
        Patient patient = processADTMessage(msg);
        return CompletableFuture.completedFuture(patient);
    }
}

@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean(name = "hl7Executor")
    public Executor hl7Executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("hl7-");
        executor.initialize();
        return executor;
    }
}
```

---

## Load Testing

### Apache JMeter Test Plan

```xml
<!-- jmeter-load-test.jmx -->
<jmeterTestPlan version="1.2">
  <TestPlan guiclass="TestPlanGui" testclass="TestPlan">
    <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup">
      <stringProp name="ThreadGroup.num_threads">100</stringProp>
      <stringProp name="ThreadGroup.ramp_time">60</stringProp>
      <stringProp name="ThreadGroup.duration">300</stringProp>

      <HTTPSamplerProxy>
        <stringProp name="HTTPSampler.domain">localhost</stringProp>
        <stringProp name="HTTPSampler.port">8080</stringProp>
        <stringProp name="HTTPSampler.path">/api/ris/v1/patients</stringProp>
        <stringProp name="HTTPSampler.method">GET</stringProp>
      </HTTPSamplerProxy>
    </ThreadGroup>
  </TestPlan>
</jmeterTestPlan>
```

### Gatling Load Test

```scala
// LoadTest.scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class RisLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")

  val scn = scenario("RIS Load Test")
    .exec(
      http("Get Patients")
        .get("/api/ris/v1/patients")
        .check(status.is(200))
    )
    .pause(1)

  setUp(
    scn.inject(
      rampUsers(100) during (60 seconds),
      constantUsersPerSec(50) during (5 minutes)
    )
  ).protocols(httpProtocol)
}
```

### Performance Benchmarks

```bash
# Run load test
gatling.sh -s RisLoadTest

# Expected Results (baseline):
# - Mean response time: < 200ms
# - 95th percentile: < 500ms
# - 99th percentile: < 1000ms
# - Throughput: > 1000 req/s
# - Error rate: < 0.1%
```

---

## Troubleshooting

### High CPU Usage

```bash
# Find CPU-intensive threads
jstack <pid> | grep -A 5 "runnable"

# Profile with async-profiler
./profiler.sh -d 60 -f flamegraph.html <pid>

# Check GC overhead
jstat -gcutil <pid> 1000
```

### High Memory Usage

```bash
# Heap dump
jmap -dump:format=b,file=heap.bin <pid>

# Analyze with Eclipse MAT or VisualVM

# Check for memory leaks
jcmd <pid> GC.heap_info
```

### Slow Database Queries

```sql
-- Enable slow query log
ALTER SYSTEM SET log_min_duration_statement = 1000;  -- 1 second

-- Find slow queries
SELECT
  query,
  calls,
  total_exec_time,
  mean_exec_time,
  max_exec_time
FROM pg_stat_statements
WHERE mean_exec_time > 100
ORDER BY mean_exec_time DESC
LIMIT 20;

-- Check table bloat
SELECT
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
  n_live_tup,
  n_dead_tup
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY n_dead_tup DESC;

-- Fix with VACUUM
VACUUM ANALYZE <table_name>;
```

### Connection Pool Exhaustion

```bash
# Check active connections
psql -U postgres -c "
  SELECT
    datname,
    count(*) as connections,
    max_conn,
    max_conn - count(*) as available
  FROM pg_stat_activity,
       (SELECT setting::int as max_conn FROM pg_settings WHERE name='max_connections') mc
  GROUP BY datname, max_conn
"

# Kill idle connections
psql -U postgres -c "
  SELECT pg_terminate_backend(pid)
  FROM pg_stat_activity
  WHERE state = 'idle'
  AND state_change < current_timestamp - INTERVAL '10 minutes'
"
```

---

## Performance Checklist

### Application
- [ ] JVM tuned for workload
- [ ] Garbage collector optimized
- [ ] Connection pool sized correctly
- [ ] Caching enabled
- [ ] Async processing where appropriate
- [ ] Batch operations implemented

### Database
- [ ] Appropriate indexes created
- [ ] Statistics updated
- [ ] Vacuum running regularly
- [ ] Query plans optimized
- [ ] Connection pooling configured

### Infrastructure
- [ ] SSD storage for database
- [ ] Adequate RAM allocated
- [ ] Network latency < 5ms
- [ ] Load balancer configured
- [ ] CDN for static assets (if applicable)

### Monitoring
- [ ] Metrics collection enabled
- [ ] Dashboards configured
- [ ] Alerts set up
- [ ] Log aggregation working
- [ ] APM tool integrated

---

## Resources

- [PostgreSQL Performance Tuning](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [Hibernate Performance](https://vladmihalcea.com/tutorials/hibernate/)
- [Spring Boot Performance](https://spring.io/blog/2015/12/10/spring-boot-memory-performance)
- [JVM Performance](https://www.oracle.com/technical-resources/articles/java/vmoptions-jsp.html)
