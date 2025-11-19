package org.dcm4che.ris.core.exception;

/**
 * Exception thrown when an invalid status transition is attempted.
 *
 * @author dcm4che-ris
 */
public class InvalidStatusTransitionException extends RisException {

    private static final long serialVersionUID = 1L;

    private final String currentStatus;
    private final String targetStatus;

    public InvalidStatusTransitionException(String currentStatus, String targetStatus) {
        super(String.format("Invalid status transition from %s to %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public InvalidStatusTransitionException(String message, String currentStatus, String targetStatus) {
        super(message);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getTargetStatus() {
        return targetStatus;
    }
}
