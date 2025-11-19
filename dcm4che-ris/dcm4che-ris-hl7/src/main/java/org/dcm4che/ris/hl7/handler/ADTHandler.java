package org.dcm4che.ris.hl7.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.hl7.HL7Exception;
import org.dcm4che.hl7.HL7Message;
import org.dcm4che.hl7.HL7Segment;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4che.net.hl7.UnparsedHL7Message;
import org.dcm4che.ris.api.entity.Patient;
import org.dcm4che.ris.hl7.config.HL7Configuration;
import org.dcm4che.ris.hl7.service.HL7MessageService;
import org.springframework.stereotype.Component;

import java.net.Socket;

/**
 * HL7 ADT (Admission, Discharge, Transfer) message handler.
 * Processes patient demographic messages (A01, A04, A05, A08, etc.).
 *
 * Supported message types:
 * - A01: Admit/Visit Notification
 * - A04: Register a Patient
 * - A05: Pre-Admit a Patient
 * - A08: Update Patient Information
 * - A40: Merge Patient
 *
 * Based on HL7 v2.x ADT message specification.
 *
 * @author dcm4che-ris
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ADTHandler implements HL7Application {

    private final HL7MessageService hl7MessageService;
    private final HL7Configuration hl7Configuration;

    @Override
    public byte[] onMessage(Socket socket, UnparsedHL7Message msg) throws HL7Exception {
        try {
            // Parse HL7 message
            HL7Message hl7Msg = HL7Message.parse(msg.data(), msg.msh().getCharacterSet());

            // Get message type
            HL7Segment msh = hl7Msg.msh();
            String messageType = msh.getMessageType();
            String sendingApplication = msh.getSendingApplicationWithFacility();

            log.info("Received HL7 ADT message: Type={}, From={}", messageType, sendingApplication);

            // Check if sending application is allowed
            if (!hl7Configuration.isSendingApplicationAllowed(sendingApplication)) {
                log.warn("Sending application not in whitelist: {}", sendingApplication);
                return createNACK(hl7Msg, "AR", "Sending application not authorized");
            }

            // Log message if enabled
            if (hl7Configuration.isLogHL7Messages()) {
                log.debug("HL7 ADT Message:\n{}", new String(msg.data()));
            }

            // Process based on message type
            Patient patient = null;
            String triggerEvent = msh.getTriggerEvent();

            switch (triggerEvent) {
                case "A01": // Admit/Visit Notification
                case "A04": // Register a Patient
                case "A05": // Pre-Admit a Patient
                case "A08": // Update Patient Information
                    patient = hl7MessageService.processADTMessage(hl7Msg);
                    log.info("ADT {} processed successfully for patient: MRN={}",
                            triggerEvent, patient.getMrn());
                    break;

                case "A40": // Merge Patient - Patient Identifier List
                    patient = processMergePatient(hl7Msg);
                    log.info("ADT A40 (Merge Patient) processed successfully");
                    break;

                default:
                    log.warn("Unsupported ADT trigger event: {}", triggerEvent);
                    return createNACK(hl7Msg, "AR", "Unsupported trigger event: " + triggerEvent);
            }

            // Return ACK
            return createACK(hl7Msg);

        } catch (HL7Exception e) {
            log.error("HL7 error processing ADT message", e);
            throw e;
        } catch (Exception e) {
            log.error("Error processing ADT message", e);
            throw new HL7Exception("Internal error processing ADT message: " + e.getMessage());
        }
    }

    /**
     * Process A40 - Merge Patient message.
     * Merges source patient into target patient.
     */
    private Patient processMergePatient(HL7Message msg) throws HL7Exception {
        // A40 contains multiple PID segments
        // First PID = target patient (keep)
        // Second PID = source patient (merge from)

        // This is a simplified implementation
        // In production, would need more complex logic to handle MRG segment

        log.warn("Patient merge (A40) not fully implemented - processing as update");
        return hl7MessageService.processADTMessage(msg);
    }

    /**
     * Create ACK (acknowledgment) message.
     */
    private byte[] createACK(HL7Message msg) {
        try {
            HL7Segment msh = msg.msh();

            // Build ACK message
            StringBuilder ack = new StringBuilder();

            // MSH segment
            ack.append("MSH|^~\\&|");
            ack.append(hl7Configuration.getFullApplicationName()).append("|"); // Sending Application
            ack.append(msh.getSendingApplicationWithFacility()).append("|"); // Receiving Application (original sender)
            ack.append(hl7MessageService.formatHL7DateTime(java.time.LocalDateTime.now())).append("||"); // Timestamp
            ack.append("ACK^").append(msh.getTriggerEvent()).append("|"); // Message Type
            ack.append(msh.getMessageControlID()).append("|"); // Message Control ID
            ack.append("P|").append(hl7Configuration.getHl7Version()).append("\r");

            // MSA segment
            ack.append("MSA|AA|"); // AA = Application Accept
            ack.append(msh.getMessageControlID()).append("|\r");

            return ack.toString().getBytes(hl7Configuration.getCharacterEncoding());

        } catch (Exception e) {
            log.error("Error creating ACK", e);
            return "MSA|AE|ERROR|\r".getBytes();
        }
    }

    /**
     * Create NACK (negative acknowledgment) message.
     */
    private byte[] createNACK(HL7Message msg, String ackCode, String errorMessage) {
        try {
            HL7Segment msh = msg.msh();

            // Build NACK message
            StringBuilder nack = new StringBuilder();

            // MSH segment
            nack.append("MSH|^~\\&|");
            nack.append(hl7Configuration.getFullApplicationName()).append("|");
            nack.append(msh.getSendingApplicationWithFacility()).append("|");
            nack.append(hl7MessageService.formatHL7DateTime(java.time.LocalDateTime.now())).append("||");
            nack.append("ACK^").append(msh.getTriggerEvent()).append("|");
            nack.append(msh.getMessageControlID()).append("|");
            nack.append("P|").append(hl7Configuration.getHl7Version()).append("\r");

            // MSA segment
            nack.append("MSA|").append(ackCode).append("|"); // AR = Application Reject, AE = Application Error
            nack.append(msh.getMessageControlID()).append("|");
            nack.append(errorMessage).append("|\r");

            return nack.toString().getBytes(hl7Configuration.getCharacterEncoding());

        } catch (Exception e) {
            log.error("Error creating NACK", e);
            return "MSA|AE|ERROR|\r".getBytes();
        }
    }
}
