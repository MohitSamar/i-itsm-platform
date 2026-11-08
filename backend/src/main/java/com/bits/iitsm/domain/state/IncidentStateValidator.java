package com.bits.iitsm.domain.state;

import com.bits.iitsm.domain.enums.IncidentStatus;
import com.bits.iitsm.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class IncidentStateValidator {

    private final Map<IncidentStatus, Set<IncidentStatus>> allowedTransitions = new EnumMap<>(IncidentStatus.class);

    public IncidentStateValidator() {
        allowedTransitions.put(IncidentStatus.NEW, EnumSet.of(IncidentStatus.CLASSIFIED));
        allowedTransitions.put(IncidentStatus.CLASSIFIED, EnumSet.of(IncidentStatus.ASSIGNED));
        allowedTransitions.put(IncidentStatus.ASSIGNED, EnumSet.of(IncidentStatus.ACKNOWLEDGED, IncidentStatus.ASSIGNED));
        allowedTransitions.put(IncidentStatus.ACKNOWLEDGED, EnumSet.of(IncidentStatus.IN_PROGRESS));
        allowedTransitions.put(IncidentStatus.IN_PROGRESS, EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.ASSIGNED));
        allowedTransitions.put(IncidentStatus.RESOLVED, EnumSet.of(IncidentStatus.VERIFIED, IncidentStatus.IN_PROGRESS));
        allowedTransitions.put(IncidentStatus.VERIFIED, EnumSet.of(IncidentStatus.CLOSED));
        allowedTransitions.put(IncidentStatus.CLOSED, EnumSet.noneOf(IncidentStatus.class));
    }

    public void validateTransition(IncidentStatus current, IncidentStatus target) {
        Set<IncidentStatus> validNextStates = allowedTransitions.getOrDefault(current, EnumSet.noneOf(IncidentStatus.class));
        if (!validNextStates.contains(target)) {
            throw new InvalidStateTransitionException(current, target);
        }
    }
}
