package org.dcm4che.ris.api.enums;

/**
 * Status of a Performed Procedure Step (PPS).
 * Based on DICOM Standard PS 3.4 F.7 - Modality Performed Procedure Step SOP Class.
 */
public enum PPSStatus {
    /**
     * PPS is in progress (from MPPS N-CREATE)
     */
    IN_PROGRESS,

    /**
     * PPS completed successfully (from MPPS N-SET)
     */
    COMPLETED,

    /**
     * PPS discontinued/interrupted (from MPPS N-SET)
     */
    DISCONTINUED;

    /**
     * Check if transition to new status is valid
     */
    public boolean canTransitionTo(PPSStatus newStatus) {
        if (this == newStatus) {
            return true;
        }

        switch (this) {
            case IN_PROGRESS:
                return newStatus == COMPLETED || newStatus == DISCONTINUED;

            case COMPLETED:
            case DISCONTINUED:
                // Terminal states, no transitions allowed
                return false;

            default:
                return false;
        }
    }

    /**
     * Check if this is a terminal status
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == DISCONTINUED;
    }
}
