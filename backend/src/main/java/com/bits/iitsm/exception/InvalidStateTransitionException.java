package com.bits.iitsm.exception;

import com.bits.iitsm.domain.enums.IncidentStatus;

public class InvalidStateTransitionException extends IITSMException {
    public InvalidStateTransitionException(IncidentStatus from, IncidentStatus to) {
        super("ERR_INVALID_STATE_TRANSITION",
              String.format("Illegal lifecycle state transition from %s directly to %s.", from, to));
    }
}
