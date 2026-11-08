package com.bits.iitsm.exception;

public class WorkloadExceededException extends IITSMException {
    public WorkloadExceededException(String techName, int maxCapacity) {
        super("ERR_WORKLOAD_EXCEEDED",
              String.format("Technician %s has reached maximum workload capacity (%d).", techName, maxCapacity));
    }
}
