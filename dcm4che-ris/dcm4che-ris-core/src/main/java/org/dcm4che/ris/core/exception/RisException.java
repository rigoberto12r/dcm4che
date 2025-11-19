package org.dcm4che.ris.core.exception;

/**
 * Base exception for all RIS-related exceptions.
 *
 * @author dcm4che-ris
 */
public class RisException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RisException(String message) {
        super(message);
    }

    public RisException(String message, Throwable cause) {
        super(message, cause);
    }

    public RisException(Throwable cause) {
        super(cause);
    }
}
