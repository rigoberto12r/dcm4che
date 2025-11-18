# Security Hardening Guide

This guide covers security best practices and hardening procedures for dcm4che-ris in production environments.

## Table of Contents

1. [Security Architecture](#security-architecture)
2. [Authentication & Authorization](#authentication--authorization)
3. [Network Security](#network-security)
4. [Database Security](#database-security)
5. [Application Security](#application-security)
6. [DICOM/HL7 Security](#dicomhl7-security)
7. [Container Security](#container-security)
8. [Compliance](#compliance)
9. [Security Monitoring](#security-monitoring)
10. [Incident Response](#incident-response)

---

## Security Architecture

### Defense in Depth

```
┌─────────────────────────────────────────────────────────────┐
│                        Internet                              │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  Layer 1: Network Perimeter (Firewall/WAF)                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  Layer 2: Load Balancer (SSL Termination)                   │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  Layer 3: API Gateway (Authentication/Rate Limiting)        │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  Layer 4: Application (Authorization/Input Validation)      │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  Layer 5: Database (Encryption at Rest/Access Control)      │
└─────────────────────────────────────────────────────────────┘
```

---

## Authentication & Authorization

### JWT-Based Authentication

#### application-security.yml

```yaml
ris:
  security:
    jwt:
      secret: ${JWT_SECRET}  # Must be 256+ bits, stored in secrets manager
      expiration: 3600000    # 1 hour
      refresh-expiration: 86400000  # 24 hours

    # Password policy
    password:
      min-length: 12
      require-uppercase: true
      require-lowercase: true
      require-digit: true
      require-special: true
      max-age-days: 90
      history-count: 5  # Prevent reuse of last 5 passwords

    # Session management
    session:
      timeout: 1800  # 30 minutes
      max-concurrent: 3
      invalidate-on-password-change: true

    # Account lockout
    lockout:
      max-attempts: 5
      duration-minutes: 30
```

#### Spring Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Using JWT
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/ris/v1/auth/**").permitAll()
                .requestMatchers("/api/ris/actuator/health").permitAll()

                // DICOM endpoints - require TECHNICIAN role
                .requestMatchers("/api/ris/v1/scheduling/**").hasRole("TECHNICIAN")

                // Order endpoints - require PHYSICIAN role
                .requestMatchers("/api/ris/v1/orders/**").hasAnyRole("PHYSICIAN", "RADIOLOGIST")

                // Report endpoints - require RADIOLOGIST role
                .requestMatchers("/api/ris/v1/reports/**").hasRole("RADIOLOGIST")

                // Admin endpoints
                .requestMatchers("/api/ris/admin/**").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(unauthorizedHandler)
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use Argon2 for password hashing
        return new Argon2PasswordEncoder(
            16,     // salt length
            32,     // hash length
            1,      // parallelism
            65536,  // memory cost (64 MB)
            3       // iterations
        );
    }
}
```

### Role-Based Access Control (RBAC)

```java
public enum Role {
    ADMIN(          // Full system access
        "ADMIN",
        "SYSTEM_ADMIN", "USER_MANAGEMENT", "AUDIT_ACCESS"
    ),
    RADIOLOGIST(    // Read/write reports, read orders
        "RADIOLOGIST",
        "REPORT_READ", "REPORT_WRITE", "ORDER_READ"
    ),
    PHYSICIAN(      // Create orders, read reports
        "PHYSICIAN",
        "ORDER_CREATE", "ORDER_READ", "REPORT_READ"
    ),
    TECHNICIAN(     // Schedule procedures, update status
        "TECHNICIAN",
        "SCHEDULE_READ", "SCHEDULE_WRITE", "SPS_UPDATE"
    ),
    CLERK(          // Patient registration
        "CLERK",
        "PATIENT_READ", "PATIENT_WRITE"
    );

    private final String name;
    private final List<String> permissions;
}
```

### OAuth2 Integration (Optional)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: ris-client
            client-secret: ${OAUTH_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope:
              - openid
              - profile
              - email
        provider:
          keycloak:
            issuer-uri: https://keycloak.hospital.com/realms/hospital
            user-name-attribute: preferred_username
```

---

## Network Security

### Firewall Rules

```bash
# Allow only necessary ports
sudo ufw default deny incoming
sudo ufw default allow outgoing

# SSH (from admin network only)
sudo ufw allow from 10.0.100.0/24 to any port 22

# HTTPS
sudo ufw allow 443/tcp

# DICOM (from modality network only)
sudo ufw allow from 10.0.50.0/24 to any port 11112

# HL7 (from HIS network only)
sudo ufw allow from 10.0.60.0/24 to any port 2575

# PostgreSQL (from app servers only)
sudo ufw allow from 10.0.10.0/24 to any port 5432

sudo ufw enable
```

### Network Segmentation

```yaml
# docker-compose with network segmentation
networks:
  # DMZ - exposed to internet
  dmz:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/24

  # Application tier - internal only
  app-tier:
    driver: bridge
    internal: true
    ipam:
      config:
        - subnet: 172.21.0.0/24

  # Database tier - most restricted
  db-tier:
    driver: bridge
    internal: true
    ipam:
      config:
        - subnet: 172.22.0.0/24

services:
  nginx:
    networks:
      - dmz
      - app-tier

  ris-rest:
    networks:
      - app-tier
      - db-tier

  postgres:
    networks:
      - db-tier
```

### TLS Configuration

#### Minimum TLS 1.2

```yaml
server:
  ssl:
    enabled: true
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: JKS
    key-alias: ris-server

    # TLS settings
    enabled-protocols: TLSv1.2,TLSv1.3
    ciphers:
      - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
      - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
      - TLS_DHE_RSA_WITH_AES_256_GCM_SHA384
      - TLS_DHE_RSA_WITH_AES_128_GCM_SHA256

    # Client certificate authentication (optional)
    client-auth: want
    trust-store: ${SSL_TRUSTSTORE_PATH}
    trust-store-password: ${SSL_TRUSTSTORE_PASSWORD}
```

### Rate Limiting

```java
@Configuration
public class RateLimitConfiguration {

    @Bean
    public RateLimiter apiRateLimiter() {
        return RateLimiter.create(100.0);  // 100 requests per second
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registrationBean =
            new FilterRegistrationBean<>();

        registrationBean.setFilter(new RateLimitFilter(apiRateLimiter()));
        registrationBean.addUrlPatterns("/api/ris/*");
        registrationBean.setOrder(1);

        return registrationBean;
    }
}
```

---

## Database Security

### PostgreSQL Security Configuration

#### User Privileges

```sql
-- Create application user with minimal privileges
CREATE USER ris_app WITH PASSWORD 'strong_password';

-- Grant only necessary privileges
GRANT CONNECT ON DATABASE ris_db TO ris_app;
GRANT USAGE ON SCHEMA public TO ris_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ris_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ris_app;

-- Revoke dangerous privileges
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON pg_catalog.pg_authid FROM PUBLIC;

-- Create read-only user for reports
CREATE USER ris_readonly WITH PASSWORD 'readonly_password';
GRANT CONNECT ON DATABASE ris_db TO ris_readonly;
GRANT USAGE ON SCHEMA public TO ris_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO ris_readonly;
```

#### Encryption at Rest

```bash
# Enable PostgreSQL encryption
# /etc/postgresql/16/main/postgresql.conf
ssl = on
ssl_cert_file = '/etc/postgresql/16/main/server.crt'
ssl_key_file = '/etc/postgresql/16/main/server.key'
ssl_ca_file = '/etc/postgresql/16/main/root.crt'

# Require SSL for connections
ssl_min_protocol_version = 'TLSv1.2'
ssl_prefer_server_ciphers = on
ssl_ciphers = 'HIGH:!aNULL:!MD5'
```

#### Column-Level Encryption

```java
@Entity
@Table(name = "patient")
public class Patient {

    @Column(name = "ssn")
    @Convert(converter = SensitiveDataConverter.class)
    private String ssn;  // Encrypted in database

    @Column(name = "medical_record")
    @Convert(converter = SensitiveDataConverter.class)
    private String medicalRecord;  // Encrypted
}

@Converter
public class SensitiveDataConverter implements AttributeConverter<String, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }
}
```

### SQL Injection Prevention

```java
// ALWAYS use parameterized queries
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Good - parameterized
    @Query("SELECT p FROM Patient p WHERE p.mrn = :mrn")
    Optional<Patient> findByMrn(@Param("mrn") String mrn);

    // Bad - string concatenation (vulnerable to SQL injection)
    // NEVER DO THIS:
    // @Query("SELECT p FROM Patient p WHERE p.mrn = '" + mrn + "'")
}
```

---

## Application Security

### Input Validation

```java
@RestController
@RequestMapping("/api/ris/v1/patients")
@Validated
public class PatientController {

    @PostMapping
    public ResponseEntity<Patient> createPatient(
            @Valid @RequestBody PatientDTO patientDTO) {

        // DTO with validation annotations
        return ResponseEntity.ok(patientService.createPatient(patientDTO));
    }
}

@Data
public class PatientDTO {

    @NotBlank(message = "MRN is required")
    @Pattern(regexp = "^[A-Z0-9]{1,20}$", message = "Invalid MRN format")
    private String mrn;

    @NotBlank(message = "Patient name is required")
    @Size(min = 1, max = 200, message = "Name must be 1-200 characters")
    @Pattern(regexp = "^[A-Za-z^\\s]+$", message = "Invalid name format")
    private String patientName;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
    private String phoneNumber;
}
```

### Output Encoding (XSS Prevention)

```java
@Configuration
public class XssConfiguration {

    @Bean
    public FilterRegistrationBean<XssFilter> xssFilter() {
        FilterRegistrationBean<XssFilter> registrationBean =
            new FilterRegistrationBean<>();
        registrationBean.setFilter(new XssFilter());
        registrationBean.addUrlPatterns("/api/ris/*");
        return registrationBean;
    }
}

public class XssFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }
}
```

### Audit Logging

```java
@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Around("@annotation(Audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        String ipAddress = getClientIpAddress();

        AuditLog log = AuditLog.builder()
            .timestamp(LocalDateTime.now())
            .username(username)
            .action(methodName)
            .ipAddress(ipAddress)
            .build();

        try {
            Object result = joinPoint.proceed();
            log.setStatus("SUCCESS");
            return result;
        } catch (Exception e) {
            log.setStatus("FAILURE");
            log.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            auditLogRepository.save(log);
        }
    }
}

// Usage
@Service
public class PatientService {

    @Audited
    @PreAuthorize("hasRole('CLERK')")
    public Patient createPatient(Patient patient) {
        // Method implementation
    }
}
```

### Sensitive Data Masking in Logs

```java
@Configuration
public class LoggingConfiguration {

    @Bean
    public FilterRegistrationBean<SensitiveDataMaskingFilter>
            sensitiveDataMaskingFilter() {
        FilterRegistrationBean<SensitiveDataMaskingFilter> registrationBean =
            new FilterRegistrationBean<>();
        registrationBean.setFilter(new SensitiveDataMaskingFilter());
        return registrationBean;
    }
}

public class SensitiveDataMaskingFilter implements Filter {

    private static final Pattern SSN_PATTERN =
        Pattern.compile("\\d{3}-\\d{2}-\\d{4}");
    private static final Pattern CREDIT_CARD_PATTERN =
        Pattern.compile("\\d{4}-\\d{4}-\\d{4}-\\d{4}");

    public String maskSensitiveData(String input) {
        String masked = input;
        masked = SSN_PATTERN.matcher(masked).replaceAll("XXX-XX-XXXX");
        masked = CREDIT_CARD_PATTERN.matcher(masked).replaceAll("XXXX-XXXX-XXXX-XXXX");
        return masked;
    }
}
```

---

## DICOM/HL7 Security

### DICOM TLS Configuration

```yaml
ris:
  dicom:
    tls-enabled: true
    tls-protocols: TLSv1.2,TLSv1.3
    keystore-path: /app/ssl/dicom-keystore.jks
    keystore-password: ${DICOM_KEYSTORE_PASSWORD}

    # Client authentication
    client-auth-enabled: true
    truststore-path: /app/ssl/dicom-truststore.jks
    truststore-password: ${DICOM_TRUSTSTORE_PASSWORD}

    # Whitelist calling AE titles
    calling-ae-title-whitelist: CT_01,MR_01,XR_01
```

### HL7 Security

```yaml
ris:
  hl7:
    # TLS for HL7
    tls-enabled: true
    keystore-path: /app/ssl/hl7-keystore.jks
    keystore-password: ${HL7_KEYSTORE_PASSWORD}

    # Whitelist sending applications
    sending-application-whitelist: HIS^HOSPITAL,EMR^CLINIC

    # Message validation
    validate-messages: true
    reject-invalid-messages: true

    # Audit logging
    log-hl7-messages: true
    log-pii: false  # Don't log PHI
```

### DICOM Association Control

```java
@Component
public class DicomSecurityHandler implements AssociationHandler {

    @Override
    public void onAssociationRequest(Association as,
                                     PresentationContext pc) throws IOException {
        String callingAET = as.getCallingAET();
        String remoteAddress = as.getSocket().getRemoteAddress().toString();

        // Check whitelist
        if (!isAuthorizedAET(callingAET)) {
            log.warn("Unauthorized AE Title: {} from {}", callingAET, remoteAddress);
            as.reject(AssociationRejectException.UL_SERIVCE_USER);
            return;
        }

        // Check IP whitelist
        if (!isAuthorizedIP(remoteAddress)) {
            log.warn("Unauthorized IP: {} for AET: {}", remoteAddress, callingAET);
            as.reject(AssociationRejectException.UL_SERIVCE_USER);
            return;
        }

        // Audit log
        auditLog(callingAET, remoteAddress, "ASSOCIATION_ACCEPTED");
    }
}
```

---

## Container Security

### Docker Security

```dockerfile
# Use minimal base image
FROM eclipse-temurin:17-jre-alpine

# Don't run as root
RUN addgroup -g 1000 risapp && \
    adduser -D -u 1000 -G risapp risapp

# Set working directory
WORKDIR /app

# Copy with correct permissions
COPY --chown=risapp:risapp target/*.jar app.jar

# Drop capabilities
RUN setcap 'cap_net_bind_service=+ep' /usr/lib/jvm/java-17-openjdk/bin/java

# Run as non-root
USER risapp

# Read-only root filesystem
# (requires writable volumes for logs, tmp)
```

### Docker Compose Security

```yaml
services:
  ris-rest:
    image: dcm4che/ris:1.0.0

    # Security options
    security_opt:
      - no-new-privileges:true

    # Read-only root filesystem
    read_only: true

    # Temporary filesystem for /tmp
    tmpfs:
      - /tmp

    # Limit resources
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 4G
        reservations:
          cpus: '1'
          memory: 2G

    # Drop capabilities
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE
```

### Secrets Management

```bash
# Don't use environment variables for secrets
# Use Docker secrets

# Create secret
echo "my_secret_password" | docker secret create db_password -

# Use in compose
services:
  postgres:
    secrets:
      - db_password
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password

secrets:
  db_password:
    external: true
```

---

## Compliance

### HIPAA Compliance

**Required Controls:**

1. **Access Control** ✓
   - Unique user identification
   - Automatic logoff after inactivity
   - Encryption and decryption

2. **Audit Controls** ✓
   - Audit logs for all access to PHI
   - Review of audit logs

3. **Integrity** ✓
   - Mechanisms to authenticate PHI
   - Protection from improper alteration

4. **Transmission Security** ✓
   - Encryption of PHI in transit (TLS)
   - Integrity controls

### GDPR Compliance

```java
// Right to erasure (Right to be forgotten)
@Service
public class GdprService {

    @Transactional
    public void anonymizePatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        // Anonymize personal data
        patient.setPatientName("ANONYMIZED");
        patient.setMrn("ANON-" + patientId);
        patient.setBirthDate(null);
        patient.setSex(null);
        patient.setAddress(null);
        patient.setPhoneNumbers(null);
        patient.setEmail(null);
        patient.setGdprAnonymized(true);
        patient.setAnonymizedAt(LocalDateTime.now());

        patientRepository.save(patient);

        // Audit log
        auditLog("GDPR_ANONYMIZATION", patientId);
    }

    // Right to data portability
    public byte[] exportPatientData(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        // Export all patient data in machine-readable format
        return jsonMapper.writeValueAsBytes(patient);
    }
}
```

---

## Security Monitoring

### Security Events to Monitor

```yaml
# application.yml
logging:
  level:
    org.springframework.security: INFO
    org.dcm4che.ris.security: INFO

management:
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ris
```

### Prometheus Metrics

```java
@Component
public class SecurityMetrics {

    private final Counter failedLogins;
    private final Counter unauthorizedAccess;
    private final Counter suspiciousActivity;

    public SecurityMetrics(MeterRegistry registry) {
        this.failedLogins = Counter.builder("security.failed.logins")
            .description("Number of failed login attempts")
            .register(registry);

        this.unauthorizedAccess = Counter.builder("security.unauthorized.access")
            .description("Number of unauthorized access attempts")
            .register(registry);

        this.suspiciousActivity = Counter.builder("security.suspicious.activity")
            .description("Number of suspicious activities detected")
            .register(registry);
    }
}
```

### Alert Rules (Prometheus)

```yaml
# security-alerts.yml
groups:
  - name: security_alerts
    interval: 30s
    rules:
      - alert: HighFailedLoginRate
        expr: rate(security_failed_logins_total[5m]) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High rate of failed logins"
          description: "More than 10 failed logins per minute"

      - alert: UnauthorizedAccessAttempts
        expr: security_unauthorized_access_total > 50
        for: 10m
        labels:
          severity: critical
        annotations:
          summary: "Multiple unauthorized access attempts"
          description: "Possible brute force attack"
```

---

## Incident Response

### Security Incident Response Plan

**1. Detection and Analysis**
- Monitor security alerts
- Review audit logs
- Investigate anomalies

**2. Containment**
```bash
# Immediate actions
# Block suspicious IP
sudo ufw deny from <suspicious_ip>

# Disable compromised user
psql -U postgres -d ris_db -c \
  "UPDATE users SET enabled = false WHERE username = '<compromised_user>'"

# Force logout all sessions
redis-cli FLUSHDB
```

**3. Eradication**
- Identify root cause
- Remove malware/backdoors
- Patch vulnerabilities

**4. Recovery**
- Restore from clean backups
- Reset credentials
- Monitor for reinfection

**5. Post-Incident**
- Document incident
- Update procedures
- Implement preventive measures

### Security Checklist

#### Daily
- [ ] Review failed login attempts
- [ ] Check security alerts
- [ ] Verify backup completion

#### Weekly
- [ ] Review audit logs
- [ ] Check for software updates
- [ ] Test backup restoration

#### Monthly
- [ ] Security scan
- [ ] Review access controls
- [ ] Update security documentation

#### Quarterly
- [ ] Penetration testing
- [ ] Security training
- [ ] Disaster recovery drill

---

## Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CIS Benchmarks](https://www.cisecurity.org/cis-benchmarks/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [HIPAA Security Rule](https://www.hhs.gov/hipaa/for-professionals/security/index.html)
- [GDPR](https://gdpr.eu/)
