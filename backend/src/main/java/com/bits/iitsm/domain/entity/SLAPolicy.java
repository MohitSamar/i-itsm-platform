package com.bits.iitsm.domain.entity;

import com.bits.iitsm.domain.enums.Priority;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sla_policies")
public class SLAPolicy {

    @Id
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(nullable = false)
    private long responseTargetMinutes;

    @Column(nullable = false)
    private long resolutionTargetMinutes;

    protected SLAPolicy() {}

    public SLAPolicy(Priority priority, long responseTargetMinutes, long resolutionTargetMinutes) {
        this.priority = priority;
        this.responseTargetMinutes = responseTargetMinutes;
        this.resolutionTargetMinutes = resolutionTargetMinutes;
    }

    public LocalDateTime calculateResponseDeadline(LocalDateTime createdAt) {
        return createdAt.plusMinutes(responseTargetMinutes);
    }

    public LocalDateTime calculateResolutionDeadline(LocalDateTime createdAt) {
        return createdAt.plusMinutes(resolutionTargetMinutes);
    }

    public Priority getPriority() { return priority; }
    public long getResponseTargetMinutes() { return responseTargetMinutes; }
    public void setResponseTargetMinutes(long minutes) { this.responseTargetMinutes = minutes; }
    public long getResolutionTargetMinutes() { return resolutionTargetMinutes; }
    public void setResolutionTargetMinutes(long minutes) { this.resolutionTargetMinutes = minutes; }
}
