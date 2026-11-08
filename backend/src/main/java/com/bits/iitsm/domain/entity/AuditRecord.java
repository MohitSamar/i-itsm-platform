package com.bits.iitsm.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_records")
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String incidentKey;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String actorName;

    @Column(nullable = false)
    private String action;

    private String previousState;
    private String newState;

    @Column(length = 1000)
    private String description;

    protected AuditRecord() {}

    public AuditRecord(String incidentKey, String actorName, String action,
                       String previousState, String newState, String description) {
        this.incidentKey = incidentKey;
        this.timestamp = LocalDateTime.now();
        this.actorName = actorName;
        this.action = action;
        this.previousState = previousState;
        this.newState = newState;
        this.description = description;
    }

    public UUID getId() { return id; }
    public String getIncidentKey() { return incidentKey; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getActorName() { return actorName; }
    public String getAction() { return action; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public String getDescription() { return description; }
}
