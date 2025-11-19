package org.dcm4che.ris.core.exception;

/**
 * Exception thrown when authentication fails.
 *
 * @author dcm4che-ris
 */
public class AuthenticationException extends RisException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
