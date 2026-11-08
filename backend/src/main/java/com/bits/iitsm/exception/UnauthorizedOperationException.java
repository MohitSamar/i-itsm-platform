package com.bits.iitsm.exception;

public class UnauthorizedOperationException extends IITSMException {
    public UnauthorizedOperationException(String message) {
        super("ERR_UNAUTHORIZED_OPERATION", message);
    }
}
