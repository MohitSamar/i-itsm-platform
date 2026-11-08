package com.bits.iitsm.domain.strategy;

import com.bits.iitsm.domain.entity.Incident;
import com.bits.iitsm.domain.entity.Technician;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinAssignmentStrategy implements AssignmentStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Optional<Technician> selectTechnician(Incident incident, List<Technician> candidates) {
        List<Technician> available = candidates.stream()
                .filter(Technician::canAcceptIncident)
                .toList();
        if (available.isEmpty()) {
            return Optional.empty();
        }
        int idx = Math.floorMod(counter.getAndIncrement(), available.size());
        return Optional.of(available.get(idx));
    }

    @Override
    public String getStrategyKey() {
        return "ROUND_ROBIN";
    }
}
