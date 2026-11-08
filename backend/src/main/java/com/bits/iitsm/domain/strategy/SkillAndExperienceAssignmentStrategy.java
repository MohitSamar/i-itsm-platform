package com.bits.iitsm.domain.strategy;

import com.bits.iitsm.domain.entity.Incident;
import com.bits.iitsm.domain.entity.Technician;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class SkillAndExperienceAssignmentStrategy implements AssignmentStrategy {

    @Override
    public Optional<Technician> selectTechnician(Incident incident, List<Technician> candidates) {
        return candidates.stream()
                .filter(Technician::canAcceptIncident)
                .max(Comparator.comparingDouble(tech -> scoreTechnician(tech, incident)));
    }

    private double scoreTechnician(Technician tech, Incident incident) {
        double skillBonus = tech.hasSkill(incident.getCategory()) ? 50.0 : 0.0;
        double experienceBonus = tech.getExperienceYears() * 5.0;
        double workloadPenalty = tech.getCurrentWorkload() * 8.0;
        return skillBonus + experienceBonus - workloadPenalty;
    }

    @Override
    public String getStrategyKey() {
        return "SKILL_AND_EXPERIENCE";
    }
}
