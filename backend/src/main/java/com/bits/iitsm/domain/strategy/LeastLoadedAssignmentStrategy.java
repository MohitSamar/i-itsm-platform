package com.bits.iitsm.domain.strategy;

import com.bits.iitsm.domain.entity.Incident;
import com.bits.iitsm.domain.entity.Technician;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class LeastLoadedAssignmentStrategy implements AssignmentStrategy {

    @Override
    public Optional<Technician> selectTechnician(Incident incident, List<Technician> candidates) {
        return candidates.stream()
                .filter(Technician::canAcceptIncident)
                .min(Comparator.comparingInt(Technician::getCurrentWorkload));
    }

    @Override
    public String getStrategyKey() {
        return "LEAST_LOADED";
    }
}
