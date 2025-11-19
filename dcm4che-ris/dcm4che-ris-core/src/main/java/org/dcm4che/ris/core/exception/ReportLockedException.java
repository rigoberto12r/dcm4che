package org.dcm4che.ris.core.exception;

/**
 * Exception thrown when attempting to edit a report that is locked by another user.
 *
 * @author dcm4che-ris
 */
public class ReportLockedException extends RisException {

    private static final long serialVersionUID = 1L;

    private final Long reportId;
    private final String lockedByUsername;

    public ReportLockedException(Long reportId, String lockedByUsername) {
        super(String.format("Report %d is currently locked by user: %s", reportId, lockedByUsername));
        this.reportId = reportId;
        this.lockedByUsername = lockedByUsername;
    }

    public Long getReportId() {
        return reportId;
    }

    public String getLockedByUsername() {
        return lockedByUsername;
    }
}
