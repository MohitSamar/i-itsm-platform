package com.bits.iitsm.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String incidentKey;
    private String recipient;
    private String channel;
    private String eventType;

    @Column(length = 1000)
    private String message;

    private LocalDateTime timestamp;

    protected NotificationLog() {}

    public NotificationLog(String incidentKey, String recipient, String channel, String eventType, String message) {
        this.incidentKey = incidentKey;
        this.recipient = recipient;
        this.channel = channel;
        this.eventType = eventType;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getIncidentKey() { return incidentKey; }
    public String getRecipient() { return recipient; }
    public String getChannel() { return channel; }
    public String getEventType() { return eventType; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
