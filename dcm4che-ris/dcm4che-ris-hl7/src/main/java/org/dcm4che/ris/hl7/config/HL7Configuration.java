package org.dcm4che.ris.hl7.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * HL7 v2.x configuration properties for RIS.
 * Configures HL7 message handlers for ADT and ORM messages.
 *
 * @author dcm4che-ris
 */
@Configuration
@ConfigurationProperties(prefix = "ris.hl7")
@Getter
@Setter
public class HL7Configuration {

    /**
     * Application name for HL7 messages
     * Default: RIS
     */
    private String applicationName = "RIS";

    /**
     * Facility name for HL7 messages
     * Default: HOSPITAL
     */
    private String facilityName = "HOSPITAL";

    /**
     * Hostname/IP address to bind HL7 server
     * Default: 0.0.0.0 (all interfaces)
     */
    private String hostname = "0.0.0.0";

    /**
     * Port for HL7 server
     * Default: 2575 (common HL7 port)
     */
    private int port = 2575;

    /**
     * Maximum number of concurrent HL7 connections
     * Default: 50
     */
    private int maxConnections = 50;

    /**
     * Request timeout in milliseconds
     * Default: 60000 (60 seconds)
     */
    private int requestTimeout = 60000;

    /**
     * Idle timeout in milliseconds
     * Default: 60000 (60 seconds)
     */
    private int idleTimeout = 60000;

    /**
     * Accept timeout in milliseconds
     * Default: 5000 (5 seconds)
     */
    private int acceptTimeout = 5000;

    /**
     * Release timeout in milliseconds
     * Default: 5000 (5 seconds)
     */
    private int releaseTimeout = 5000;

    /**
     * Socket buffer size
     * Default: 8192
     */
    private int socketBufferSize = 8192;

    /**
     * Enable TLS for HL7 connections
     * Default: false
     */
    private boolean tlsEnabled = false;

    /**
     * TLS protocols (comma-separated)
     * Default: TLSv1.2,TLSv1.3
     */
    private String tlsProtocols = "TLSv1.2,TLSv1.3";

    /**
     * Keystore path for TLS
     */
    private String keystorePath;

    /**
     * Keystore password for TLS
     */
    private String keystorePassword;

    /**
     * Keystore type (JKS, PKCS12)
     * Default: JKS
     */
    private String keystoreType = "JKS";

    /**
     * Enable ADT message handler
     * Default: true
     */
    private boolean adtEnabled = true;

    /**
     * Enable ORM message handler
     * Default: true
     */
    private boolean ormEnabled = true;

    /**
     * Enable ORU message handler (for results)
     * Default: false
     */
    private boolean oruEnabled = false;

    /**
     * Sending application whitelist (comma-separated)
     * If specified, only messages from these applications will be accepted
     * Default: null (allow all)
     */
    private String sendingApplicationWhitelist;

    /**
     * Auto-create patients from ADT messages
     * Default: true
     */
    private boolean autoCreatePatients = true;

    /**
     * Auto-create orders from ORM messages
     * Default: true
     */
    private boolean autoCreateOrders = true;

    /**
     * Default issuer of patient ID
     * Used when not specified in HL7 message
     */
    private String defaultIssuerOfPatientId;

    /**
     * Enable logging of HL7 messages
     * Default: false (can be verbose)
     */
    private boolean logHL7Messages = false;

    /**
     * Character encoding for HL7 messages
     * Default: UTF-8
     */
    private String characterEncoding = "UTF-8";

    /**
     * HL7 version
     * Default: 2.5
     */
    private String hl7Version = "2.5";

    /**
     * Check if sending application is allowed
     */
    public boolean isSendingApplicationAllowed(String sendingApplication) {
        if (sendingApplicationWhitelist == null || sendingApplicationWhitelist.trim().isEmpty()) {
            return true; // No whitelist, allow all
        }

        String[] allowedApps = sendingApplicationWhitelist.split(",");
        for (String allowedApp : allowedApps) {
            if (allowedApp.trim().equalsIgnoreCase(sendingApplication)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get full HL7 application name (app^facility)
     */
    public String getFullApplicationName() {
        return applicationName + "^" + facilityName;
    }
}
