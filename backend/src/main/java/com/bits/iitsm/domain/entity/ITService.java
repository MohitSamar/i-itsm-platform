package com.bits.iitsm.domain.entity;

import com.bits.iitsm.domain.enums.CriticalityLevel;
import com.bits.iitsm.domain.enums.IncidentCategory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "it_services")
public class ITService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String serviceCode;

    @Column(nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    private IncidentCategory defaultCategory;

    @Enumerated(EnumType.STRING)
    private CriticalityLevel businessCriticality;

    private String operationalStatus = "OPERATIONAL";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "service_dependencies",
        joinColumns = @JoinColumn(name = "service_id"),
        inverseJoinColumns = @JoinColumn(name = "depends_on_service_id")
    )
    @JsonIgnoreProperties("dependencies")
    private Set<ITService> dependencies = new HashSet<>();

    protected ITService() {}

    public ITService(String serviceCode, String serviceName, IncidentCategory defaultCategory, CriticalityLevel businessCriticality) {
        this.serviceCode = serviceCode;
        this.serviceName = serviceName;
        this.defaultCategory = defaultCategory;
        this.businessCriticality = businessCriticality;
    }

    public void addDependency(ITService upstreamService) {
        this.dependencies.add(upstreamService);
    }

    public boolean hasUpstreamOutage() {
        return dependencies.stream().anyMatch(dep -> !"OPERATIONAL".equals(dep.getOperationalStatus()));
    }

    public UUID getId() { return id; }
    public String getServiceCode() { return serviceCode; }
    public String getServiceName() { return serviceName; }
    public IncidentCategory getDefaultCategory() { return defaultCategory; }
    public CriticalityLevel getBusinessCriticality() { return businessCriticality; }
    public String getOperationalStatus() { return operationalStatus; }
    public void setOperationalStatus(String operationalStatus) { this.operationalStatus = operationalStatus; }
    public Set<ITService> getDependencies() { return dependencies; }
}
