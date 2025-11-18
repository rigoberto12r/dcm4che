package org.dcm4che.ris.hl7.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che.hl7.HL7Exception;
import org.dcm4che.hl7.HL7Message;
import org.dcm4che.hl7.HL7Segment;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4che.net.hl7.UnparsedHL7Message;
import org.dcm4che.ris.api.entity.ImagingServiceRequest;
import org.dcm4che.ris.hl7.config.HL7Configuration;
import org.dcm4che.ris.hl7.service.HL7MessageService;
import org.springframework.stereotype.Component;

import java.net.Socket;

/**
 * HL7 ORM (Order Message) handler.
 * Processes radiology order messages (O01).
 *
 * Supported message types:
 * - ORM^O01: General Order Message
 *
 * Supported order controls (ORC-1):
 * - NW: New Order
 * - CA: Cancel Order
 * - OC: Order Canceled
 * - SC: Status Changed
 *
 * Based on HL7 v2.x ORM message specification.
 *
 * @author dcm4che-ris
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ORMHandler implements HL7Application {

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

            log.info("Received HL7 ORM message: Type={}, From={}", messageType, sendingApplication);

            // Check if sending application is allowed
            if (!hl7Configuration.isSendingApplicationAllowed(sendingApplication)) {
                log.warn("Sending application not in whitelist: {}", sendingApplication);
                return createNACK(hl7Msg, "AR", "Sending application not authorized");
            }

            // Log message if enabled
            if (hl7Configuration.isLogHL7Messages()) {
                log.debug("HL7 ORM Message:\n{}", new String(msg.data()));
            }

            // Validate trigger event
            String triggerEvent = msh.getTriggerEvent();
            if (!"O01".equals(triggerEvent)) {
                log.warn("Unsupported ORM trigger event: {}", triggerEvent);
                return createNACK(hl7Msg, "AR", "Unsupported trigger event: " + triggerEvent);
            }

            // Process ORM message
            ImagingServiceRequest order = hl7MessageService.processORMMessage(hl7Msg);

            log.info("ORM message processed successfully: OrderID={}, PlacerOrderNumber={}",
                    order.getOrderId(), order.getPlacerOrderNumber());

            // Return ACK
            return createACK(hl7Msg);

        } catch (HL7Exception e) {
            log.error("HL7 error processing ORM message", e);
            return createNACK(hl7Msg, "AE", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing ORM message", e);
            return createNACK(hl7Msg, "AE", "Internal error: " + e.getMessage());
        }
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
            ack.append(msh.getSendingApplicationWithFacility()).append("|"); // Receiving Application
            ack.append(hl7MessageService.formatHL7DateTime(java.time.LocalDateTime.now())).append("||");
            ack.append("ACK^").append(msh.getTriggerEvent()).append("|");
            ack.append(msh.getMessageControlID()).append("|");
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
            HL7Segment msh = msg == null ? null : msg.msh();
            String messageControlId = msh != null ? msh.getMessageControlID() : "UNKNOWN";
            String triggerEvent = msh != null ? msh.getTriggerEvent() : "O01";
            String sendingApp = msh != null ? msh.getSendingApplicationWithFacility() : "UNKNOWN";

            // Build NACK message
            StringBuilder nack = new StringBuilder();

            // MSH segment
            nack.append("MSH|^~\\&|");
            nack.append(hl7Configuration.getFullApplicationName()).append("|");
            nack.append(sendingApp).append("|");
            nack.append(hl7MessageService.formatHL7DateTime(java.time.LocalDateTime.now())).append("||");
            nack.append("ACK^").append(triggerEvent).append("|");
            nack.append(messageControlId).append("|");
            nack.append("P|").append(hl7Configuration.getHl7Version()).append("\r");

            // MSA segment
            nack.append("MSA|").append(ackCode).append("|");
            nack.append(messageControlId).append("|");
            nack.append(errorMessage).append("|\r");

            return nack.toString().getBytes(hl7Configuration.getCharacterEncoding());

        } catch (Exception e) {
            log.error("Error creating NACK", e);
            return "MSA|AE|ERROR|\r".getBytes();
        }
    }
}
