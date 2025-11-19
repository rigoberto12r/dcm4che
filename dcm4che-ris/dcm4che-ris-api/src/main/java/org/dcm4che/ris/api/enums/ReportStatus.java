package org.dcm4che.ris.api.enums;

/**
 * Status of a Radiology Report.
 */
public enum ReportStatus {
    /**
     * Report not yet started
     */
    PENDING,

    /**
     * Draft report in progress
     */
    DRAFT,

    /**
     * Preliminary report issued
     */
    PRELIMINARY,

    /**
     * Final report signed
     */
    FINAL,

    /**
     * Report amended after being final
     */
    AMENDED,

    /**
     * Report corrected
     */
    CORRECTED,

    /**
     * Addendum added to final report
     */
    ADDENDUM;

    /**
     * Check if transition to new status is valid
     */
    public boolean canTransitionTo(ReportStatus newStatus) {
        if (this == newStatus) {
            return true;
        }

        switch (this) {
            case PENDING:
                return newStatus == DRAFT;

            case DRAFT:
                return newStatus == PRELIMINARY || newStatus == FINAL;

            case PRELIMINARY:
                return newStatus == FINAL || newStatus == AMENDED;

            case FINAL:
                return newStatus == AMENDED || newStatus == CORRECTED || newStatus == ADDENDUM;

            case AMENDED:
            case CORRECTED:
                return newStatus == ADDENDUM;

            case ADDENDUM:
                return false; // Terminal state

            default:
                return false;
        }
    }

    /**
     * Check if report can be edited
     */
    public boolean isEditable() {
        return this == PENDING || this == DRAFT;
    }

    /**
     * Check if report is finalized
     */
    public boolean isFinal() {
        return this == FINAL || this == AMENDED ||
               this == CORRECTED || this == ADDENDUM;
    }
}
