package com.bits.iitsm.exception;

public class ResourceNotFoundException extends IITSMException {
    public ResourceNotFoundException(String resource, String identifier) {
        super("ERR_RESOURCE_NOT_FOUND", String.format("%s not found with identifier: %s", resource, identifier));
    }
}
