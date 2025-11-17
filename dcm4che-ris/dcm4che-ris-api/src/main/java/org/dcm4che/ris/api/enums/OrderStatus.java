package org.dcm4che.ris.api.enums;

/**
 * Status of an Imaging Service Request (Order).
 * Based on IHE Scheduled Workflow Profile.
 */
public enum OrderStatus {
    /**
     * Order created, not yet scheduled
     */
    PENDING,

    /**
     * At least one SPS has been scheduled
     */
    SCHEDULED,

    /**
     * At least one SPS is in progress
     */
    IN_PROGRESS,

    /**
     * All SPS completed successfully
     */
    COMPLETED,

    /**
     * Order canceled
     */
    CANCELED,

    /**
     * Order discontinued/interrupted
     */
    DISCONTINUED;

    /**
     * Check if transition to new status is valid
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        if (this == newStatus) {
            return true; // Allow re-setting same status
        }

        switch (this) {
            case PENDING:
                return newStatus == SCHEDULED || newStatus == CANCELED;

            case SCHEDULED:
                return newStatus == IN_PROGRESS || newStatus == CANCELED;

            case IN_PROGRESS:
                return newStatus == COMPLETED || newStatus == DISCONTINUED;

            case COMPLETED:
                // Completed orders cannot change status
                return false;

            case CANCELED:
            case DISCONTINUED:
                // Canceled/discontinued orders cannot change status
                return false;

            default:
                return false;
        }
    }
}
