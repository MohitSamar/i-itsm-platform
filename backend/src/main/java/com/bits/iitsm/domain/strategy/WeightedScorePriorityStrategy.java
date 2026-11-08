package com.bits.iitsm.domain.strategy;

import com.bits.iitsm.domain.enums.CriticalityLevel;
import com.bits.iitsm.domain.enums.ImpactLevel;
import com.bits.iitsm.domain.enums.Priority;
import com.bits.iitsm.domain.enums.UrgencyLevel;
import org.springframework.stereotype.Component;

@Component
public class WeightedScorePriorityStrategy implements PriorityCalculationStrategy {

    @Override
    public Priority calculatePriority(ImpactLevel impact, UrgencyLevel urgency, CriticalityLevel criticality) {
        double score = (impact.getWeight() * 0.40)
                     + (urgency.getWeight() * 0.35)
                     + (criticality.getWeight() * 0.25);

        if (score >= 3.4) return Priority.P1;
        if (score >= 2.6) return Priority.P2;
        if (score >= 1.8) return Priority.P3;
        return Priority.P4;
    }

    @Override
    public String getStrategyName() {
        return "WEIGHTED_SCORE";
    }
}
