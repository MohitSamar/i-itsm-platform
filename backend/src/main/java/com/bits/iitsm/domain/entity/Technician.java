package com.bits.iitsm.domain.entity;

import com.bits.iitsm.domain.enums.IncidentCategory;
import com.bits.iitsm.domain.enums.Role;
import com.bits.iitsm.exception.WorkloadExceededException;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("TECHNICIAN")
public class Technician extends User {

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "technician_skills", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "skill")
    private Set<IncidentCategory> skills = new HashSet<>();

    private int currentWorkload = 0;
    private int maxCapacity = 5;
    private boolean available = true;
    private int experienceYears = 1;
    private String escalationRole = "L1_SUPPORT";

    protected Technician() {}

    public Technician(String name, String email, String passwordHash,
                      Set<IncidentCategory> skills, int maxCapacity, int experienceYears, String escalationRole) {
        super(name, email, passwordHash, Role.TECHNICIAN);
        this.skills = skills != null ? new HashSet<>(skills) : new HashSet<>();
        this.maxCapacity = maxCapacity;
        this.experienceYears = experienceYears;
        this.escalationRole = escalationRole;
    }

    @Override
    public String getDashboardRoute() {
        return "/technician/workbench";
    }

    public boolean canAcceptIncident() {
        return available && currentWorkload < maxCapacity;
    }

    public boolean hasSkill(IncidentCategory category) {
        return skills.contains(category);
    }

    public void incrementWorkload() {
        if (currentWorkload >= maxCapacity) {
            throw new WorkloadExceededException(getName(), maxCapacity);
        }
        this.currentWorkload++;
    }

    public void decrementWorkload() {
        if (this.currentWorkload > 0) {
            this.currentWorkload--;
        }
    }

    public Set<IncidentCategory> getSkills() { return skills; }
    public int getCurrentWorkload() { return currentWorkload; }
    public int getMaxCapacity() { return maxCapacity; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public int getExperienceYears() { return experienceYears; }
    public String getEscalationRole() { return escalationRole; }
}
