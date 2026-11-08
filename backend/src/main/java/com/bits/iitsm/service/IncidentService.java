package com.bits.iitsm.service;

import com.bits.iitsm.domain.entity.*;
import com.bits.iitsm.domain.enums.*;
import com.bits.iitsm.domain.factory.IncidentFactory;
import com.bits.iitsm.domain.observer.IncidentEvent;
import com.bits.iitsm.domain.observer.IncidentObserver;
import com.bits.iitsm.domain.state.IncidentStateValidator;
import com.bits.iitsm.domain.strategy.AssignmentStrategy;
import com.bits.iitsm.exception.ResourceNotFoundException;
import com.bits.iitsm.exception.TechnicianUnavailableException;
import com.bits.iitsm.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EmployeeRepository employeeRepository;
    private final TechnicianRepository technicianRepository;
    private final ITServiceRepository serviceRepository;
    private final SLAPolicyRepository slaPolicyRepository;
    private final IncidentFactory incidentFactory;
    private final IncidentStateValidator stateValidator;
    private final IntelligenceService intelligenceService;
    private final List<AssignmentStrategy> assignmentStrategies;
    private final List<IncidentObserver> observers;

    private String activeAssignmentStrategyKey = "SKILL_AND_EXPERIENCE";

    public IncidentService(IncidentRepository incidentRepository,
                           EmployeeRepository employeeRepository,
                           TechnicianRepository technicianRepository,
                           ITServiceRepository serviceRepository,
                           SLAPolicyRepository slaPolicyRepository,
                           IncidentFactory incidentFactory,
                           IncidentStateValidator stateValidator,
                           IntelligenceService intelligenceService,
                           List<AssignmentStrategy> assignmentStrategies,
                           List<IncidentObserver> observers) {
        this.incidentRepository = incidentRepository;
        this.employeeRepository = employeeRepository;
        this.technicianRepository = technicianRepository;
        this.serviceRepository = serviceRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.incidentFactory = incidentFactory;
        this.stateValidator = stateValidator;
        this.intelligenceService = intelligenceService;
        this.assignmentStrategies = assignmentStrategies;
        this.observers = observers;
    }

    @Transactional
    public Incident reportIncident(String title, String description, UUID reporterId, UUID serviceId,
                                   ImpactLevel impact, UrgencyLevel urgency, IncidentCategory requestedCategory) {
        Employee reporter = employeeRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", String.valueOf(reporterId)));
        ITService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("ITService", String.valueOf(serviceId)));

        List<Incident> existing = incidentRepository.findAllByOrderByCreatedAtDesc();

        IncidentCategory inferredCategory = requestedCategory != null
                ? requestedCategory
                : intelligenceService.inferCategoryFromText(title, description, service);

        SLAPolicy slaPolicy = slaPolicyRepository.findById(Priority.P2).orElse(null);

        Incident incident = incidentFactory.createAndClassifyIncident(
                title, description, reporter, service, impact, urgency, inferredCategory, slaPolicy
        );

        SLAPolicy exactSla = slaPolicyRepository.findById(incident.getPriority()).orElse(null);
        if (exactSla != null) {
            incident.setResponseDeadline(incident.getCreatedAt().plusMinutes(exactSla.getResponseTargetMinutes()));
            incident.setResolutionDeadline(incident.getCreatedAt().plusMinutes(exactSla.getResolutionTargetMinutes()));
        }

        intelligenceService.findPotentialDuplicate(title, description, serviceId, existing)
                .ifPresent(dupKey -> {
                    incident.setDuplicateOfIncidentKey(dupKey);
                    incident.addAuditEntry("INTELLIGENCE_ENGINE", "DUPLICATE_DETECTED",
                            incident.getStatus().name(), incident.getStatus().name(),
                            "Flagged as potential duplicate of active incident " + dupKey);
                });

        intelligenceService.findUpstreamRootCauseIncident(service, existing)
                .ifPresent(rootKey -> {
                    incident.setCorrelatedRootIncidentKey(rootKey);
                    incident.addAuditEntry("DEPENDENCY_GRAPH_ENGINE", "ROOT_CAUSE_CORRELATED",
                            incident.getStatus().name(), incident.getStatus().name(),
                            "Correlated to upstream dependency incident " + rootKey);
                });

        List<Technician> allTechnicians = technicianRepository.findAll();
        AssignmentStrategy strategy = getActiveStrategy();
        Optional<Technician> selectedTech = strategy.selectTechnician(incident, allTechnicians);

        if (selectedTech.isPresent()) {
            Technician tech = selectedTech.get();
            tech.incrementWorkload();
            technicianRepository.save(tech);
            incident.setAssignedTechnician(tech);
            incident.transitionState(IncidentStatus.ASSIGNED, stateValidator, "AUTO_DISPATCHER (" + strategy.getStrategyKey() + ")",
                    "Assigned to Technician " + tech.getName() + " using " + strategy.getStrategyKey() + " strategy");
            notifyAllObservers(incident, "INCIDENT_ASSIGNED",
                    "Incident " + incident.getIncidentKey() + " [" + incident.getPriority() + "] assigned to " + tech.getName(),
                    tech.getEmail());
        } else {
            notifyAllObservers(incident, "INCIDENT_UNASSIGNED_QUEUE",
                    "Incident " + incident.getIncidentKey() + " created but all technicians are at capacity.",
                    "admin@campus.bits.edu");
        }

        if (incident.getPriority() == Priority.P1) {
            service.setOperationalStatus("OUTAGE");
            serviceRepository.save(service);
        } else if (incident.getPriority() == Priority.P2 && "OPERATIONAL".equals(service.getOperationalStatus())) {
            service.setOperationalStatus("DEGRADED");
            serviceRepository.save(service);
        }

        Incident saved = incidentRepository.save(incident);
        notifyAllObservers(saved, "INCIDENT_CREATED",
                "Your incident " + saved.getIncidentKey() + " (" + saved.getTitle() + ") has been logged with Priority " + saved.getPriority(),
                reporter.getEmail());
        return saved;
    }

    @Transactional
    public Incident transitionIncidentState(String incidentKey, IncidentStatus targetStatus, String actorName, String notes) {
        Incident incident = getByKey(incidentKey);

        if (targetStatus == IncidentStatus.RESOLVED && (notes == null || notes.isBlank())) {
            notes = "Issue resolved and service health verified.";
        }
        if (targetStatus == IncidentStatus.RESOLVED) {
            incident.setResolutionDetails(notes);
            ITService svc = incident.getAffectedService();
            svc.setOperationalStatus("OPERATIONAL");
            serviceRepository.save(svc);
        } else if (targetStatus == IncidentStatus.IN_PROGRESS && notes != null && !notes.isBlank()) {
            incident.appendInvestigationNotes(notes);
        }

        incident.transitionState(targetStatus, stateValidator, actorName,
                notes != null && !notes.isBlank() ? notes : "Transitioned to " + targetStatus);

        if (targetStatus == IncidentStatus.RESOLVED && incident.getAssignedTechnician() != null) {
            technicianRepository.save(incident.getAssignedTechnician());
        }

        Incident saved = incidentRepository.save(incident);
        notifyAllObservers(saved, "STATUS_" + targetStatus.name(),
                "Incident " + saved.getIncidentKey() + " transitioned to " + targetStatus + " by " + actorName,
                saved.getReporter().getEmail());
        return saved;
    }

    @Transactional
    public Incident addInvestigationNote(String incidentKey, String actorName, String note) {
        Incident incident = getByKey(incidentKey);
        incident.appendInvestigationNotes("[" + actorName + "]: " + note);
        incident.addAuditEntry(actorName, "NOTE_ADDED", incident.getStatus().name(), incident.getStatus().name(), note);
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident submitFeedback(String incidentKey, int rating, String comment) {
        Incident incident = getByKey(incidentKey);
        incident.recordFeedback(rating, comment);
        incident.addAuditEntry(incident.getReporter().getName(), "FEEDBACK_SUBMITTED",
                incident.getStatus().name(), incident.getStatus().name(),
                "Rating: " + rating + "/5 - " + comment);
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident manualAssign(String incidentKey, UUID technicianId, String adminName) {
        Incident incident = getByKey(incidentKey);
        Technician tech = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", String.valueOf(technicianId)));

        if (!tech.canAcceptIncident()) {
            throw new TechnicianUnavailableException("Technician " + tech.getName() + " is unavailable or at max capacity.");
        }

        if (incident.getAssignedTechnician() != null) {
            incident.getAssignedTechnician().decrementWorkload();
            technicianRepository.save(incident.getAssignedTechnician());
        }

        tech.incrementWorkload();
        technicianRepository.save(tech);
        incident.setAssignedTechnician(tech);

        if (incident.getStatus() == IncidentStatus.CLASSIFIED || incident.getStatus() == IncidentStatus.ASSIGNED || incident.getStatus() == IncidentStatus.IN_PROGRESS) {
            incident.transitionState(IncidentStatus.ASSIGNED, stateValidator, adminName,
                    "Manually assigned to " + tech.getName());
        }
        return incidentRepository.save(incident);
    }

    @Transactional
    public int evaluateSlaAndEscalate(boolean simulateFastForward) {
        List<Incident> activeIncidents = incidentRepository.findAll().stream()
                .filter(i -> i.getStatus() != IncidentStatus.RESOLVED
                          && i.getStatus() != IncidentStatus.VERIFIED
                          && i.getStatus() != IncidentStatus.CLOSED)
                .toList();

        LocalDateTime now = simulateFastForward ? LocalDateTime.now().plusHours(3) : LocalDateTime.now();
        int affectedCount = 0;

        for (Incident inc : activeIncidents) {
            boolean updated = false;

            if (inc.getAcknowledgedAt() == null && inc.getResponseDeadline() != null && now.isAfter(inc.getResponseDeadline()) && !inc.isResponseSlaViolated()) {
                inc.setResponseSlaViolated(true);
                updated = true;
                inc.addAuditEntry("SLA_WATCHDOG", "RESPONSE_SLA_VIOLATED", inc.getStatus().name(), inc.getStatus().name(),
                        "Response target breached (" + inc.getResponseDeadline() + ")");
                notifyAllObservers(inc, "SLA_VIOLATION",
                        "ALERT: Response SLA violated for " + inc.getIncidentKey() + " (" + inc.getPriority() + ")",
                        "admin@campus.bits.edu");
            }

            if (inc.getResolutionDeadline() != null) {
                long totalMinutes = Math.max(1, Duration.between(inc.getCreatedAt(), inc.getResolutionDeadline()).toMinutes());
                long elapsedMinutes = Duration.between(inc.getCreatedAt(), now).toMinutes();

                if (elapsedMinutes >= (totalMinutes * 0.75) && !inc.isSlaRiskWarning() && !inc.isResolutionSlaViolated()) {
                    inc.setSlaRiskWarning(true);
                    updated = true;
                    inc.addAuditEntry("SLA_WATCHDOG", "SLA_RISK_DETECTED", inc.getStatus().name(), inc.getStatus().name(),
                            "Incident has consumed over 75% of allowed resolution SLA window.");
                    notifyAllObservers(inc, "SLA_APPROACHING",
                            "WARNING: SLA deadline approaching for " + inc.getIncidentKey(),
                            inc.getAssignedTechnician() != null ? inc.getAssignedTechnician().getEmail() : "admin@campus.bits.edu");
                }

                if (now.isAfter(inc.getResolutionDeadline()) && !inc.isResolutionSlaViolated()) {
                    inc.setResolutionSlaViolated(true);
                    inc.setSlaRiskWarning(true);
                    updated = true;
                    String[] ladder = {"TECHNICIAN", "TEAM_LEAD", "SYSTEM_ADMINISTRATOR", "IT_MANAGER"};
                    int nextLevel = Math.min(3, inc.getEscalationLevel() + 1);
                    inc.setEscalationLevel(nextLevel);
                    inc.addAuditEntry("AUTO_ESCALATION_ENGINE", "ESCALATED_TO_" + ladder[nextLevel],
                            inc.getStatus().name(), inc.getStatus().name(),
                            "Resolution SLA breached; automatically escalated to " + ladder[nextLevel]);
                    notifyAllObservers(inc, "INCIDENT_ESCALATED",
                            "CRITICAL ESCALATION: " + inc.getIncidentKey() + " escalated to " + ladder[nextLevel],
                            "admin@campus.bits.edu");
                }
            }

            if (updated) {
                incidentRepository.save(inc);
                affectedCount++;
            }
        }
        return affectedCount;
    }

    @Scheduled(fixedRate = 30000)
    public void scheduledSlaSweep() {
        evaluateSlaAndEscalate(false);
    }

    public List<Incident> searchIncidents(String query, IncidentStatus status, Priority priority,
                                          IncidentCategory category, UUID serviceId, UUID technicianId, Boolean slaViolated) {
        return incidentRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(i -> query == null || query.isBlank()
                        || i.getIncidentKey().toLowerCase().contains(query.toLowerCase())
                        || i.getTitle().toLowerCase().contains(query.toLowerCase())
                        || i.getDescription().toLowerCase().contains(query.toLowerCase()))
                .filter(i -> status == null || i.getStatus() == status)
                .filter(i -> priority == null || i.getPriority() == priority)
                .filter(i -> category == null || i.getCategory() == category)
                .filter(i -> serviceId == null || i.getAffectedService().getId().equals(serviceId))
                .filter(i -> technicianId == null || (i.getAssignedTechnician() != null && i.getAssignedTechnician().getId().equals(technicianId)))
                .filter(i -> slaViolated == null || (slaViolated && (i.isResponseSlaViolated() || i.isResolutionSlaViolated()))
                        || (!slaViolated && !i.isResponseSlaViolated() && !i.isResolutionSlaViolated()))
                .toList();
    }

    public Map<String, Object> getDashboardAnalytics() {
        List<Incident> all = incidentRepository.findAll();
        long total = all.size();
        long open = all.stream().filter(i -> i.getStatus() != IncidentStatus.RESOLVED && i.getStatus() != IncidentStatus.VERIFIED && i.getStatus() != IncidentStatus.CLOSED).count();
        long critical = all.stream().filter(i -> i.getPriority() == Priority.P1).count();
        long resolved = all.stream().filter(i -> i.getStatus() == IncidentStatus.RESOLVED || i.getStatus() == IncidentStatus.VERIFIED || i.getStatus() == IncidentStatus.CLOSED).count();
        long slaViolations = all.stream().filter(i -> i.isResponseSlaViolated() || i.isResolutionSlaViolated()).count();

        var workload = technicianRepository.findAll().stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("name", t.getName());
                    m.put("email", t.getEmail());
                    m.put("role", t.getEscalationRole());
                    m.put("activeIncidents", t.getCurrentWorkload());
                    m.put("maxCapacity", t.getMaxCapacity());
                    m.put("skills", t.getSkills());
                    return m;
                }).toList();

        var serviceBreakdown = serviceRepository.findAll().stream()
                .map(s -> {
                    long count = all.stream().filter(i -> i.getAffectedService().getId().equals(s.getId())).count();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("serviceName", s.getServiceName());
                    m.put("serviceCode", s.getServiceCode());
                    m.put("status", s.getOperationalStatus());
                    m.put("criticality", s.getBusinessCriticality());
                    m.put("incidents", count);
                    return m;
                }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalIncidents", total);
        result.put("openIncidents", open);
        result.put("criticalIncidents", critical);
        result.put("resolvedIncidents", resolved);
        result.put("slaViolations", slaViolations);
        result.put("activeAssignmentStrategy", activeAssignmentStrategyKey);
        result.put("availableStrategies", assignmentStrategies.stream().map(AssignmentStrategy::getStrategyKey).toList());
        result.put("technicianWorkload", workload);
        result.put("serviceAnalysis", serviceBreakdown);
        return result;
    }

    public Incident getByKey(String key) {
        return incidentRepository.findByIncidentKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", key));
    }

    public AssignmentStrategy getActiveStrategy() {
        return assignmentStrategies.stream()
                .filter(s -> s.getStrategyKey().equalsIgnoreCase(activeAssignmentStrategyKey))
                .findFirst()
                .orElse(assignmentStrategies.get(0));
    }

    public void setActiveAssignmentStrategyKey(String strategyKey) {
        boolean exists = assignmentStrategies.stream().anyMatch(s -> s.getStrategyKey().equalsIgnoreCase(strategyKey));
        if (!exists) {
            throw new ResourceNotFoundException("AssignmentStrategy", strategyKey);
        }
        this.activeAssignmentStrategyKey = strategyKey.toUpperCase();
    }

    public String getActiveAssignmentStrategyKey() {
        return activeAssignmentStrategyKey;
    }

    private void notifyAllObservers(Incident incident, String eventType, String message, String recipient) {
        IncidentEvent event = new IncidentEvent(incident, eventType, message, recipient);
        for (IncidentObserver observer : observers) {
            observer.onIncidentEvent(event);
        }
    }
}
