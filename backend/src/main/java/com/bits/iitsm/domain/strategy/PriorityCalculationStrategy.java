package com.bits.iitsm.domain.strategy;

import com.bits.iitsm.domain.enums.CriticalityLevel;
import com.bits.iitsm.domain.enums.ImpactLevel;
import com.bits.iitsm.domain.enums.Priority;
import com.bits.iitsm.domain.enums.UrgencyLevel;

public interface PriorityCalculationStrategy {
    Priority calculatePriority(ImpactLevel impact, UrgencyLevel urgency, CriticalityLevel serviceCriticality);
    String getStrategyName();
}
