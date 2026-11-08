package com.bits.iitsm.controller;

import com.bits.iitsm.domain.entity.*;
import com.bits.iitsm.domain.enums.*;
import com.bits.iitsm.exception.ResourceNotFoundException;
import com.bits.iitsm.exception.UnauthorizedOperationException;
import com.bits.iitsm.repository.*;
import com.bits.iitsm.service.IncidentService;
import com.bits.iitsm.service.IntelligenceService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class IitsmController {

    private final IncidentService incidentService;
    private final IntelligenceService intelligenceService;
    private final UserRepository userRepository;
    private final ITServiceRepository serviceRepository;
    private final SLAPolicyRepository slaPolicyRepository;
    private final NotificationLogRepository notificationRepository;
    private final IncidentRepository incidentRepository;

    public IitsmController(IncidentService incidentService,
                           IntelligenceService intelligenceService,
                           UserRepository userRepository,
                           ITServiceRepository serviceRepository,
                           SLAPolicyRepository slaPolicyRepository,
                           NotificationLogRepository notificationRepository,
                           IncidentRepository incidentRepository) {
        this.incidentService = incidentService;
        this.intelligenceService = intelligenceService;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.notificationRepository = notificationRepository;
        this.incidentRepository = incidentRepository;
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedOperationException("Invalid login credentials for email: " + email));
        if (password != null && !password.equals(user.getPasswordHash())) {
            throw new UnauthorizedOperationException("Invalid password for user: " + email);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("user", user);
        res.put("dashboardRoute", user.getDashboardRoute());
        return res;
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/services")
    public List<ITService> getAllServices() {
        return serviceRepository.findAll();
    }

    @GetMapping("/sla-policies")
    public List<SLAPolicy> getSlaPolicies() {
        return slaPolicyRepository.findAll();
    }

    @PutMapping("/sla-policies/{priority}")
    public SLAPolicy updateSlaPolicy(@PathVariable Priority priority, @RequestBody Map<String, Long> body) {
        SLAPolicy policy = slaPolicyRepository.findById(priority)
                .orElse(new SLAPolicy(priority, priority.getDefaultResponseMinutes(), priority.getDefaultResolutionMinutes()));
        if (body.containsKey("responseTargetMinutes")) {
            policy.setResponseTargetMinutes(body.get("responseTargetMinutes"));
        }
        if (body.containsKey("resolutionTargetMinutes")) {
            policy.setResolutionTargetMinutes(body.get("resolutionTargetMinutes"));
        }
        return slaPolicyRepository.save(policy);
    }

    @GetMapping("/incidents")
    public List<Incident> searchIncidents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) IncidentCategory category,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) UUID technicianId,
            @RequestParam(required = false) Boolean slaViolated) {
        return incidentService.searchIncidents(query, status, priority, category, serviceId, technicianId, slaViolated);
    }

    @GetMapping("/incidents/{incidentKey}")
    public Map<String, Object> getIncidentDetails(@PathVariable String incidentKey) {
        Incident inc = incidentService.getByKey(incidentKey);
        List<String> recommendations = intelligenceService.suggestResolutions(inc, incidentRepository.findAll());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("incident", inc);
        res.put("suggestedResolutions", recommendations);
        return res;
    }

    @PostMapping("/incidents")
    public Incident createIncident(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String description = body.get("description");
        UUID reporterId = UUID.fromString(body.get("reporterId"));
        UUID serviceId = UUID.fromString(body.get("serviceId"));
        ImpactLevel impact = ImpactLevel.valueOf(body.getOrDefault("impact", "HIGH"));
        UrgencyLevel urgency = UrgencyLevel.valueOf(body.getOrDefault("urgency", "HIGH"));
        IncidentCategory category = body.get("category") != null && !body.get("category").isBlank()
                ? IncidentCategory.valueOf(body.get("category")) : null;

        return incidentService.reportIncident(title, description, reporterId, serviceId, impact, urgency, category);
    }

    @PostMapping("/incidents/suggest-category")
    public Map<String, Object> suggestCategory(@RequestBody Map<String, String> body) {
        IncidentCategory suggested = intelligenceService.inferCategoryFromText(
                body.get("title"), body.get("description"), null);
        return Map.of("suggestedCategory", suggested);
    }

    @PatchMapping("/incidents/{incidentKey}/status")
    public Incident transitionStatus(@PathVariable String incidentKey, @RequestBody Map<String, String> body) {
        IncidentStatus targetStatus = IncidentStatus.valueOf(body.get("targetStatus"));
        String actorName = body.getOrDefault("actorName", "Technician");
        String notes = body.getOrDefault("notes", "");
        return incidentService.transitionIncidentState(incidentKey, targetStatus, actorName, notes);
    }

    @PostMapping("/incidents/{incidentKey}/notes")
    public Incident addNote(@PathVariable String incidentKey, @RequestBody Map<String, String> body) {
        return incidentService.addInvestigationNote(incidentKey,
                body.getOrDefault("actorName", "Support Engineer"),
                body.getOrDefault("note", ""));
    }

    @PostMapping("/incidents/{incidentKey}/feedback")
    public Incident submitFeedback(@PathVariable String incidentKey, @RequestBody Map<String, Object> body) {
        int rating = Integer.parseInt(String.valueOf(body.getOrDefault("rating", 5)));
        String comment = String.valueOf(body.getOrDefault("comment", "Resolved satisfactorily."));
        return incidentService.submitFeedback(incidentKey, rating, comment);
    }

    @PostMapping("/incidents/{incidentKey}/assign")
    public Incident manualAssign(@PathVariable String incidentKey, @RequestBody Map<String, String> body) {
        UUID techId = UUID.fromString(body.get("technicianId"));
        String actor = body.getOrDefault("actorName", "Administrator");
        return incidentService.manualAssign(incidentKey, techId, actor);
    }

    @PutMapping("/admin/assignment-strategy")
    public Map<String, String> switchStrategy(@RequestBody Map<String, String> body) {
        String key = body.get("strategyKey");
        incidentService.setActiveAssignmentStrategyKey(key);
        return Map.of("activeStrategy", incidentService.getActiveAssignmentStrategyKey());
    }

    @PostMapping("/admin/sla-simulate")
    public Map<String, Object> simulateSlaBreach() {
        int count = incidentService.evaluateSlaAndEscalate(true);
        return Map.of("escalatedOrFlaggedCount", count);
    }

    @GetMapping("/dashboard/analytics")
    public Map<String, Object> getAnalytics() {
        return incidentService.getDashboardAnalytics();
    }

    @GetMapping("/notifications")
    public List<NotificationLog> getNotifications() {
        return notificationRepository.findTop50ByOrderByTimestampDesc();
    }
}
