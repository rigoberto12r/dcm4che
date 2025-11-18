package org.dcm4che.ris.hl7;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4che.net.hl7.HL7DeviceExtension;
import org.dcm4che.net.hl7.HL7MessageListener;
import org.dcm4che.ris.hl7.config.HL7Configuration;
import org.dcm4che.ris.hl7.handler.ADTHandler;
import org.dcm4che.ris.hl7.handler.ORMHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Main HL7 server for RIS.
 * Initializes and manages HL7 message handlers (ADT, ORM).
 *
 * @author dcm4che-ris
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HL7Server {

    private final HL7Configuration hl7Configuration;
    private final ADTHandler adtHandler;
    private final ORMHandler ormHandler;

    private Device device;
    private Connection connection;
    private HL7DeviceExtension hl7Extension;

    @PostConstruct
    public void start() throws IOException {
        log.info("Starting RIS HL7 Server...");

        // Create device
        device = new Device("RIS_HL7");

        // Create HL7 device extension
        hl7Extension = new HL7DeviceExtension();
        device.addDeviceExtension(hl7Extension);

        // Create connection
        connection = new Connection();
        connection.setHostname(hl7Configuration.getHostname());
        connection.setPort(hl7Configuration.getPort());
        connection.setProtocol(Connection.Protocol.HL7);
        connection.setRequestTimeout(hl7Configuration.getRequestTimeout());
        connection.setIdleTimeout(hl7Configuration.getIdleTimeout());
        connection.setAcceptTimeout(hl7Configuration.getAcceptTimeout());
        connection.setReleaseTimeout(hl7Configuration.getReleaseTimeout());
        connection.setSocketCloseDelay(50);
        connection.setSendBufferSize(hl7Configuration.getSocketBufferSize());
        connection.setReceiveBufferSize(hl7Configuration.getSocketBufferSize());

        device.addConnection(connection);

        // Create HL7 Application
        org.dcm4che.net.hl7.HL7Application hl7App =
                new org.dcm4che.net.hl7.HL7Application(hl7Configuration.getApplicationName());
        hl7Extension.addHL7Application(hl7App);
        hl7App.addConnection(connection);

        // Register message handlers
        if (hl7Configuration.isAdtEnabled()) {
            log.info("Enabling ADT Handler (Admission/Discharge/Transfer)");
            hl7App.setHL7MessageListener(new HL7MessageListener() {
                @Override
                public byte[] onMessage(java.net.Socket socket, org.dcm4che.net.hl7.UnparsedHL7Message msg)
                        throws org.dcm4che.hl7.HL7Exception {
                    String messageType = msg.msh().getMessageType();

                    // Route to appropriate handler based on message type
                    if (messageType.startsWith("ADT^")) {
                        return adtHandler.onMessage(socket, msg);
                    } else if (messageType.startsWith("ORM^") && hl7Configuration.isOrmEnabled()) {
                        return ormHandler.onMessage(socket, msg);
                    } else {
                        log.warn("Unsupported HL7 message type: {}", messageType);
                        return createErrorResponse("Unsupported message type: " + messageType);
                    }
                }
            });
        }

        // Set executor services
        ExecutorService executorService = Executors.newCachedThreadPool();
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        device.setExecutor(executorService);
        device.setScheduledExecutor(scheduledExecutorService);

        // Bind and start listening
        device.bindConnections();

        log.info("RIS HL7 Server started successfully");
        log.info("Application: {} ({})", hl7Configuration.getApplicationName(), hl7Configuration.getFacilityName());
        log.info("Listening on: {}:{}", hl7Configuration.getHostname(), hl7Configuration.getPort());
        log.info("ADT Enabled: {}", hl7Configuration.isAdtEnabled());
        log.info("ORM Enabled: {}", hl7Configuration.isOrmEnabled());
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping RIS HL7 Server...");

        if (device != null) {
            try {
                device.unbindConnections();
                log.info("RIS HL7 Server stopped");
            } catch (Exception e) {
                log.error("Error stopping HL7 server", e);
            }
        }
    }

    /**
     * Get the device
     */
    public Device getDevice() {
        return device;
    }

    /**
     * Get connection status
     */
    public boolean isRunning() {
        return device != null && device.isInstalled();
    }

    /**
     * Get connection info
     */
    public String getConnectionInfo() {
        if (connection != null) {
            return String.format("%s:%d (App: %s)",
                    connection.getHostname(),
                    connection.getPort(),
                    hl7Configuration.getApplicationName());
        }
        return "Not connected";
    }

    /**
     * Create error response message
     */
    private byte[] createErrorResponse(String errorMessage) {
        StringBuilder response = new StringBuilder();
        response.append("MSH|^~\\&|");
        response.append(hl7Configuration.getFullApplicationName()).append("||");
        response.append("||ACK|ERROR|P|").append(hl7Configuration.getHl7Version()).append("\r");
        response.append("MSA|AE|ERROR|").append(errorMessage).append("|\r");

        try {
            return response.toString().getBytes(hl7Configuration.getCharacterEncoding());
        } catch (Exception e) {
            return response.toString().getBytes();
        }
    }
}
