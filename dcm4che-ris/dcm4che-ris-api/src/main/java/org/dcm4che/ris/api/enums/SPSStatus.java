package org.dcm4che.ris.api.enums;

/**
 * Status of a Scheduled Procedure Step.
 * Based on DICOM Standard PS 3.4 and IHE Scheduled Workflow.
 */
public enum SPSStatus {
    /**
     * SPS has been scheduled
     */
    SCHEDULED,

    /**
     * Patient has arrived
     */
    ARRIVED,

    /**
     * Ready to perform the procedure
     */
    READY,

    /**
     * Procedure step has started
     */
    STARTED,

    /**
     * Procedure step completed successfully
     */
    COMPLETED,

    /**
     * Procedure step canceled before starting
     */
    CANCELED,

    /**
     * Procedure step discontinued after starting
     */
    DISCONTINUED,

    /**
     * Patient departed (left after completion or cancellation)
     */
    DEPARTED;

    /**
     * Check if transition to new status is valid
     */
    public boolean canTransitionTo(SPSStatus newStatus) {
        if (this == newStatus) {
            return true;
        }

        switch (this) {
            case SCHEDULED:
                return newStatus == ARRIVED || newStatus == READY ||
                       newStatus == STARTED || newStatus == CANCELED;

            case ARRIVED:
                return newStatus == READY || newStatus == STARTED ||
                       newStatus == CANCELED;

            case READY:
                return newStatus == STARTED || newStatus == CANCELED;

            case STARTED:
                return newStatus == COMPLETED || newStatus == DISCONTINUED;

            case COMPLETED:
            case DISCONTINUED:
                return newStatus == DEPARTED;

            case CANCELED:
            case DEPARTED:
                return false; // Terminal states

            default:
                return false;
        }
    }

    /**
     * Check if this status is a terminal state
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELED ||
               this == DISCONTINUED || this == DEPARTED;
    }
}
