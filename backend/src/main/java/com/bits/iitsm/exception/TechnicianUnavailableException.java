package com.bits.iitsm.exception;

public class TechnicianUnavailableException extends IITSMException {
    public TechnicianUnavailableException(String message) {
        super("ERR_TECHNICIAN_UNAVAILABLE", message);
    }
}
