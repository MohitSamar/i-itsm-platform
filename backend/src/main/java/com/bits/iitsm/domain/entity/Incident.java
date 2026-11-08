package com.bits.iitsm.domain.entity;

import com.bits.iitsm.domain.enums.*;
import com.bits.iitsm.domain.state.IncidentStateValidator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String incidentKey;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Employee reporter;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id", nullable = false)
    private ITService affectedService;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "technician_id")
    private Technician assignedTechnician;

    @Enumerated(EnumType.STRING)
    private IncidentCategory category;

    @Enumerated(EnumType.STRING)
    private ImpactLevel impact;

    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgency;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.NEW;

    private LocalDateTime createdAt;
    private LocalDateTime responseDeadline;
    private LocalDateTime resolutionDeadline;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;

    private boolean responseSlaViolated = false;
    private boolean resolutionSlaViolated = false;
    private boolean slaRiskWarning = false;
    private int escalationLevel = 0;

    private String duplicateOfIncidentKey;
    private String correlatedRootIncidentKey;

    @Column(length = 2000)
    private String investigationNotes = "";

    @Column(length = 2000)
    private String resolutionDetails = "";

    private String reporterFeedback;
    private Integer feedbackRating;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "incident_id")
    @OrderBy("timestamp ASC")
    private List<AuditRecord> auditTrail = new ArrayList<>();

    protected Incident() {}

    public Incident(String incidentKey, String title, String description,
                    Employee reporter, ITService affectedService,
                    ImpactLevel impact, UrgencyLevel urgency) {
        this.incidentKey = incidentKey;
        this.title = title;
        this.description = description;
        this.reporter = reporter;
        this.affectedService = affectedService;
        this.impact = impact;
        this.urgency = urgency;
        this.status = IncidentStatus.NEW;
        this.createdAt = LocalDateTime.now();
    }

    public void transitionState(IncidentStatus targetStatus, IncidentStateValidator validator, String actorName, String reason) {
        validator.validateTransition(this.status, targetStatus);
        String prev = this.status.name();
        this.status = targetStatus;

        if (targetStatus == IncidentStatus.ACKNOWLEDGED) {
            this.acknowledgedAt = LocalDateTime.now();
        } else if (targetStatus == IncidentStatus.RESOLVED) {
            this.resolvedAt = LocalDateTime.now();
            if (this.assignedTechnician != null) {
                this.assignedTechnician.decrementWorkload();
            }
        } else if (targetStatus == IncidentStatus.CLOSED) {
            this.closedAt = LocalDateTime.now();
        }

        addAuditEntry(actorName, "STATE_TRANSITION", prev, targetStatus.name(), reason);
    }

    public void addAuditEntry(String actorName, String action, String prevState, String newState, String description) {
        this.auditTrail.add(new AuditRecord(this.incidentKey, actorName, action, prevState, newState, description));
    }

    public UUID getId() { return id; }
    public String getIncidentKey() { return incidentKey; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Employee getReporter() { return reporter; }
    public ITService getAffectedService() { return affectedService; }
    public Technician getAssignedTechnician() { return assignedTechnician; }
    public void setAssignedTechnician(Technician assignedTechnician) { this.assignedTechnician = assignedTechnician; }
    public IncidentCategory getCategory() { return category; }
    public void setCategory(IncidentCategory category) { this.category = category; }
    public ImpactLevel getImpact() { return impact; }
    public UrgencyLevel getUrgency() { return urgency; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public IncidentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResponseDeadline() { return responseDeadline; }
    public void setResponseDeadline(LocalDateTime responseDeadline) { this.responseDeadline = responseDeadline; }
    public LocalDateTime getResolutionDeadline() { return resolutionDeadline; }
    public void setResolutionDeadline(LocalDateTime resolutionDeadline) { this.resolutionDeadline = resolutionDeadline; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public boolean isResponseSlaViolated() { return responseSlaViolated; }
    public void setResponseSlaViolated(boolean responseSlaViolated) { this.responseSlaViolated = responseSlaViolated; }
    public boolean isResolutionSlaViolated() { return resolutionSlaViolated; }
    public void setResolutionSlaViolated(boolean resolutionSlaViolated) { this.resolutionSlaViolated = resolutionSlaViolated; }
    public boolean isSlaRiskWarning() { return slaRiskWarning; }
    public void setSlaRiskWarning(boolean slaRiskWarning) { this.slaRiskWarning = slaRiskWarning; }
    public int getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(int escalationLevel) { this.escalationLevel = escalationLevel; }
    public String getDuplicateOfIncidentKey() { return duplicateOfIncidentKey; }
    public void setDuplicateOfIncidentKey(String duplicateOfIncidentKey) { this.duplicateOfIncidentKey = duplicateOfIncidentKey; }
    public String getCorrelatedRootIncidentKey() { return correlatedRootIncidentKey; }
    public void setCorrelatedRootIncidentKey(String correlatedRootIncidentKey) { this.correlatedRootIncidentKey = correlatedRootIncidentKey; }
    public String getInvestigationNotes() { return investigationNotes; }
    public void appendInvestigationNotes(String note) {
        this.investigationNotes = (this.investigationNotes == null || this.investigationNotes.isBlank())
            ? note : this.investigationNotes + "\n" + note;
    }
    public String getResolutionDetails() { return resolutionDetails; }
    public void setResolutionDetails(String resolutionDetails) { this.resolutionDetails = resolutionDetails; }
    public String getReporterFeedback() { return reporterFeedback; }
    public Integer getFeedbackRating() { return feedbackRating; }
    public void recordFeedback(int rating, String comment) {
        this.feedbackRating = rating;
        this.reporterFeedback = comment;
    }
    public List<AuditRecord> getAuditTrail() { return auditTrail; }
}
