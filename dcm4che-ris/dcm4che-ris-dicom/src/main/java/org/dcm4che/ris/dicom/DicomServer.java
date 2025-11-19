package org.dcm4che.ris.dicom;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.data.UID;
import org.dcm4che.net.*;
import org.dcm4che.net.service.DicomServiceRegistry;
import org.dcm4che.ris.dicom.config.DicomConfiguration;
import org.dcm4che.ris.dicom.scp.MWLQuerySCP;
import org.dcm4che.ris.dicom.scp.MPPSServiceSCP;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Main DICOM server for RIS.
 * Initializes and manages DICOM services (MWL, MPPS).
 *
 * @author dcm4che-ris
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DicomServer {

    private final DicomConfiguration dicomConfiguration;
    private final MWLQuerySCP mwlQuerySCP;
    private final MPPSServiceSCP mppsServiceSCP;

    private Device device;
    private Connection connection;

    @PostConstruct
    public void start() throws IOException {
        log.info("Starting RIS DICOM Server...");

        // Create DICOM device
        device = new Device(dicomConfiguration.getAeTitle());

        // Create connection
        connection = new Connection();
        connection.setHostname(dicomConfiguration.getHostname());
        connection.setPort(dicomConfiguration.getPort());
        connection.setMaxOpsInvoked(dicomConfiguration.getMaxAssociations());
        connection.setMaxOpsPerformed(dicomConfiguration.getMaxAssociations());
        connection.setRequestTimeout(dicomConfiguration.getRequestTimeout());
        connection.setIdleTimeout(dicomConfiguration.getIdleTimeout());
        connection.setAcceptTimeout(dicomConfiguration.getAcceptTimeout());
        connection.setReleaseTimeout(dicomConfiguration.getReleaseTimeout());
        connection.setSendPDULength(dicomConfiguration.getSendPduLength());
        connection.setReceivePDULength(dicomConfiguration.getReceivePduLength());

        device.addConnection(connection);

        // Create Application Entity
        ApplicationEntity ae = new ApplicationEntity(dicomConfiguration.getAeTitle());
        ae.addConnection(connection);
        device.addApplicationEntity(ae);

        // Create service registry
        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();

        // Register MWL Service
        if (dicomConfiguration.isMwlEnabled()) {
            log.info("Enabling MWL Service (Modality Worklist)");
            serviceRegistry.addDicomService(mwlQuerySCP);

            // Add MWL presentation context
            ae.addTransferCapability(
                    new TransferCapability(null,
                            UID.ModalityWorklistInformationModelFind,
                            TransferCapability.Role.SCP,
                            UID.ImplicitVRLittleEndian,
                            UID.ExplicitVRLittleEndian,
                            UID.ExplicitVRBigEndianRetired)
            );
        }

        // Register MPPS Service
        if (dicomConfiguration.isMppsEnabled()) {
            log.info("Enabling MPPS Service (Modality Performed Procedure Step)");
            serviceRegistry.addDicomService(mppsServiceSCP);

            // Add MPPS presentation context
            ae.addTransferCapability(
                    new TransferCapability(null,
                            UID.ModalityPerformedProcedureStep,
                            TransferCapability.Role.SCP,
                            UID.ImplicitVRLittleEndian,
                            UID.ExplicitVRLittleEndian,
                            UID.ExplicitVRBigEndianRetired)
            );
        }

        ae.setDimseRQHandler(serviceRegistry);

        // Set executor services
        ExecutorService executorService = Executors.newCachedThreadPool();
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        device.setExecutor(executorService);
        device.setScheduledExecutor(scheduledExecutorService);

        // Bind and start listening
        device.bindConnections();

        log.info("RIS DICOM Server started successfully");
        log.info("AE Title: {}", dicomConfiguration.getAeTitle());
        log.info("Listening on: {}:{}", dicomConfiguration.getHostname(), dicomConfiguration.getPort());
        log.info("MWL Enabled: {}", dicomConfiguration.isMwlEnabled());
        log.info("MPPS Enabled: {}", dicomConfiguration.isMppsEnabled());
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping RIS DICOM Server...");

        if (device != null) {
            try {
                device.unbindConnections();
                log.info("RIS DICOM Server stopped");
            } catch (Exception e) {
                log.error("Error stopping DICOM server", e);
            }
        }
    }

    /**
     * Get the DICOM device
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
            return String.format("%s:%d (AE: %s)",
                    connection.getHostname(),
                    connection.getPort(),
                    dicomConfiguration.getAeTitle());
        }
        return "Not connected";
    }
}
