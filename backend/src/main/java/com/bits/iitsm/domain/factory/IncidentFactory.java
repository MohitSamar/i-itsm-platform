package com.bits.iitsm.domain.factory;

import com.bits.iitsm.domain.entity.*;
import com.bits.iitsm.domain.enums.*;
import com.bits.iitsm.domain.state.IncidentStateValidator;
import com.bits.iitsm.domain.strategy.PriorityCalculationStrategy;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class IncidentFactory {

    private final AtomicInteger sequence = new AtomicInteger(1001);
    private final PriorityCalculationStrategy priorityStrategy;
    private final IncidentStateValidator stateValidator;

    public IncidentFactory(PriorityCalculationStrategy priorityStrategy, IncidentStateValidator stateValidator) {
        this.priorityStrategy = priorityStrategy;
        this.stateValidator = stateValidator;
    }

    public Incident createAndClassifyIncident(String title, String description, Employee reporter,
                                              ITService service, ImpactLevel impact, UrgencyLevel urgency,
                                              IncidentCategory category, SLAPolicy slaPolicy) {
        String key = "INC-" + sequence.getAndIncrement();
        Incident incident = new Incident(key, title, description, reporter, service, impact, urgency);
        incident.addAuditEntry(reporter.getName(), "INCIDENT_CREATED", "NONE", "NEW",
                "Incident reported against service: " + service.getServiceName());

        IncidentCategory resolvedCategory = category != null ? category : service.getDefaultCategory();
        incident.setCategory(resolvedCategory);

        Priority priority = priorityStrategy.calculatePriority(impact, urgency, service.getBusinessCriticality());
        incident.setPriority(priority);

        long respMins = slaPolicy != null ? slaPolicy.getResponseTargetMinutes() : priority.getDefaultResponseMinutes();
        long resMins = slaPolicy != null ? slaPolicy.getResolutionTargetMinutes() : priority.getDefaultResolutionMinutes();
        incident.setResponseDeadline(incident.getCreatedAt().plusMinutes(respMins));
        incident.setResolutionDeadline(incident.getCreatedAt().plusMinutes(resMins));

        incident.transitionState(IncidentStatus.CLASSIFIED, stateValidator, "SYSTEM_CLASSIFIER",
                "Classified as " + resolvedCategory + " with Priority " + priority + " (" + priority.getLabel() + ")");
        return incident;
    }
}
