package com.github.fburato.justone.services.errors;

public class EntityIdMismatchException extends RuntimeException {

    private final String resourceId;
    private final String entityId;

    public EntityIdMismatchException(String resourceId, String entityId) {
        super(String.format("resourceId=%s does not match entityId=%s", resourceId, entityId));
        this.resourceId = resourceId;
        this.entityId = entityId;
    }

    public String resourceId() {
        return resourceId;
    }

    public String entityId() {
        return entityId;
    }
}
