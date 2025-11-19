package org.dcm4che.ris.core.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 *
 * @author dcm4che-ris
 */
public class DuplicateResourceException extends RisException {

    private static final long serialVersionUID = 1L;

    private final String resourceType;
    private final String fieldName;
    private final Object fieldValue;

    public DuplicateResourceException(String resourceType, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: %s", resourceType, fieldName, fieldValue));
        this.resourceType = resourceType;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public DuplicateResourceException(String message) {
        super(message);
        this.resourceType = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}
