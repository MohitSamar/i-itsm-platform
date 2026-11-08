package com.bits.iitsm.domain.strategy;

import com.bits.iitsm.domain.entity.Incident;
import com.bits.iitsm.domain.entity.Technician;
import java.util.List;
import java.util.Optional;

public interface AssignmentStrategy {
    Optional<Technician> selectTechnician(Incident incident, List<Technician> candidates);
    String getStrategyKey();
}
