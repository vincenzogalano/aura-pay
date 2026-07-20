package com.aurapay.core.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, String id) {
        super("RESOURCE_NOT_FOUND", String.format("%s with id '%s' was not found", resourceName, id));
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
}
