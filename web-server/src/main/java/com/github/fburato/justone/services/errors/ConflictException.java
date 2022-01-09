package com.github.fburato.justone.services.errors;

public class ConflictException extends RuntimeException {

    private final String entityType;
    private final String entityId;

    public ConflictException(String entityType, String entityId) {
        super(String.format("entityType=%s with entityId=%s already exists", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String entityType() {
        return entityType;
    }

    public String entityId() {
        return entityId;
    }
}
