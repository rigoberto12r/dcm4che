package org.dcm4che.ris.dicom.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DICOM configuration properties for RIS.
 * Configures the DICOM Application Entity (AE) for MWL and MPPS services.
 *
 * @author dcm4che-ris
 */
@Configuration
@ConfigurationProperties(prefix = "ris.dicom")
@Getter
@Setter
public class DicomConfiguration {

    /**
     * AE Title of this RIS DICOM server
     * Default: RIS_SCP
     */
    private String aeTitle = "RIS_SCP";

    /**
     * Hostname/IP address to bind DICOM services
     * Default: 0.0.0.0 (all interfaces)
     */
    private String hostname = "0.0.0.0";

    /**
     * Port for DICOM services
     * Default: 11112 (standard DICOM port is 104, but requires root)
     */
    private int port = 11112;

    /**
     * Maximum number of concurrent DICOM associations
     * Default: 50
     */
    private int maxAssociations = 50;

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
     * Send PDU length
     * Default: 16378
     */
    private int sendPduLength = 16378;

    /**
     * Receive PDU length
     * Default: 16378
     */
    private int receivePduLength = 16378;

    /**
     * Enable TLS for DICOM connections
     * Default: false
     */
    private boolean tlsEnabled = false;

    /**
     * TLS protocols (comma-separated)
     * Default: TLSv1.2,TLSv1.3
     */
    private String tlsProtocols = "TLSv1.2,TLSv1.3";

    /**
     * TLS cipher suites (comma-separated)
     * Default: null (use JVM defaults)
     */
    private String tlsCipherSuites;

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
     * Enable MWL Service Class Provider
     * Default: true
     */
    private boolean mwlEnabled = true;

    /**
     * Enable MPPS Service Class Provider
     * Default: true
     */
    private boolean mppsEnabled = true;

    /**
     * MWL date range for queries (days)
     * Queries will return SPSs scheduled within this range
     * Default: 7 days
     */
    private int mwlDateRangeDays = 7;

    /**
     * Maximum MWL query results
     * Limit the number of results returned for C-FIND MWL queries
     * Default: 1000
     */
    private int mwlMaxResults = 1000;

    /**
     * Calling AE Title whitelist (comma-separated)
     * If specified, only these AE Titles will be allowed to connect
     * Default: null (allow all)
     */
    private String callingAeTitleWhitelist;

    /**
     * Enable logging of DICOM messages
     * Default: false (can be verbose)
     */
    private boolean logDicomMessages = false;

    /**
     * Check if calling AE Title is allowed to connect
     */
    public boolean isCallingAeTitleAllowed(String callingAeTitle) {
        if (callingAeTitleWhitelist == null || callingAeTitleWhitelist.trim().isEmpty()) {
            return true; // No whitelist, allow all
        }

        String[] allowedAeTitles = callingAeTitleWhitelist.split(",");
        for (String allowedAe : allowedAeTitles) {
            if (allowedAe.trim().equalsIgnoreCase(callingAeTitle)) {
                return true;
            }
        }
        return false;
    }
}
