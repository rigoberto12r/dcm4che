package org.dcm4che.ris.core.exception;

/**
 * Exception thrown when a requested resource is not found.
 *
 * @author dcm4che-ris
 */
public class ResourceNotFoundException extends RisException {

    private static final long serialVersionUID = 1L;

    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s not found with id: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
