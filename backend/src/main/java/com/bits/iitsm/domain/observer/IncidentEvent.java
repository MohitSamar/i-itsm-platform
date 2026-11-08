package com.bits.iitsm.domain.observer;

import com.bits.iitsm.domain.entity.Incident;

public class IncidentEvent {
    private final Incident incident;
    private final String eventType;
    private final String message;
    private final String recipient;

    public IncidentEvent(Incident incident, String eventType, String message, String recipient) {
        this.incident = incident;
        this.eventType = eventType;
        this.message = message;
        this.recipient = recipient;
    }

    public Incident getIncident() { return incident; }
    public String getEventType() { return eventType; }
    public String getMessage() { return message; }
    public String getRecipient() { return recipient; }
}
