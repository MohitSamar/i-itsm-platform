package com.bits.iitsm.exception;

public abstract class IITSMException extends RuntimeException {
    private final String errorCode;

    protected IITSMException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
