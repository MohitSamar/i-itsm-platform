package com.bits.iitsm.config;

import com.bits.iitsm.domain.entity.*;
import com.bits.iitsm.domain.enums.*;
import com.bits.iitsm.repository.*;
import com.bits.iitsm.service.IncidentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final TechnicianRepository technicianRepository;
    private final ITServiceRepository serviceRepository;
    private final SLAPolicyRepository slaPolicyRepository;
    private final IncidentService incidentService;

    public DataSeeder(UserRepository userRepository,
                      EmployeeRepository employeeRepository,
                      TechnicianRepository technicianRepository,
                      ITServiceRepository serviceRepository,
                      SLAPolicyRepository slaPolicyRepository,
                      IncidentService incidentService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.technicianRepository = technicianRepository;
        this.serviceRepository = serviceRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.incidentService = incidentService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        for (Priority p : Priority.values()) {
            slaPolicyRepository.save(new SLAPolicy(p, p.getDefaultResponseMinutes(), p.getDefaultResolutionMinutes()));
        }

        Employee emp1 = employeeRepository.save(new Employee("Aarav Mehta (Employee E102)", "aarav@campus.bits.edu", "pass123", "Finance & Accounts", "Block A-204"));
        Employee emp2 = employeeRepository.save(new Employee("Priya Nair (Employee E108)", "priya@campus.bits.edu", "pass123", "Academic ERP Division", "Block C-102"));

        Technician tech1 = technicianRepository.save(new Technician(
                "Rohan Verma (Tech T205)", "rohan@campus.bits.edu", "pass123",
                Set.of(IncidentCategory.APPLICATION, IncidentCategory.DATABASE, IncidentCategory.SOFTWARE),
                5, 4, "L1_SUPPORT"
        ));
        Technician tech2 = technicianRepository.save(new Technician(
                "Sneha Iyer (Tech T209 - Team Lead)", "sneha@campus.bits.edu", "pass123",
                Set.of(IncidentCategory.NETWORK, IncidentCategory.SECURITY, IncidentCategory.AUTHENTICATION),
                5, 7, "TEAM_LEAD"
        ));
        Technician tech3 = technicianRepository.save(new Technician(
                "Vikram Desai (Tech T214 - Cloud/Infra)", "vikram@campus.bits.edu", "pass123",
                Set.of(IncidentCategory.CLOUD, IncidentCategory.STORAGE, IncidentCategory.HARDWARE),
                5, 6, "SYS_ADMIN"
        ));

        userRepository.save(new Administrator("Dr. K. R. Rao (IT Manager)", "admin@campus.bits.edu", "admin123", "LEVEL_5_ADMIN"));

        ITService dbService = serviceRepository.save(new ITService("SVC-DB-01", "Enterprise PostgreSQL Cluster", IncidentCategory.DATABASE, CriticalityLevel.MISSION_CRITICAL));
        ITService authService = serviceRepository.save(new ITService("SVC-AUTH-02", "Central SSO & LDAP Authentication", IncidentCategory.AUTHENTICATION, CriticalityLevel.MISSION_CRITICAL));
        ITService netService = serviceRepository.save(new ITService("SVC-NET-03", "Campus Core Backbone Network", IncidentCategory.NETWORK, CriticalityLevel.HIGH));

        ITService paymentService = new ITService("SVC-PAY-04", "Organization Payment Gateway", IncidentCategory.APPLICATION, CriticalityLevel.MISSION_CRITICAL);
        paymentService.addDependency(dbService);
        paymentService.addDependency(authService);
        paymentService = serviceRepository.save(paymentService);

        ITService storageService = new ITService("SVC-STR-05", "Cloud Document Storage Vault", IncidentCategory.STORAGE, CriticalityLevel.MODERATE);
        storageService.addDependency(netService);
        storageService = serviceRepository.save(storageService);

        Incident inc1 = incidentService.reportIncident(
                "Primary database connection pool exhaustion on cluster node-2",
                "PostgreSQL database connections timing out under heavy transaction load.",
                emp2.getId(), dbService.getId(), ImpactLevel.HIGH, UrgencyLevel.HIGH, IncidentCategory.DATABASE
        );
        incidentService.transitionIncidentState(inc1.getIncidentKey(), IncidentStatus.ACKNOWLEDGED, tech1.getName(), "Acknowledged DB pool alert.");
        incidentService.transitionIncidentState(inc1.getIncidentKey(), IncidentStatus.IN_PROGRESS, tech1.getName(), "Inspecting active pg_stat_activity locks.");

        incidentService.reportIncident(
                "Payment application unavailable and users unable to complete transactions",
                "The organization payment application is returning HTTP 503 Gateway Timeout during checkout.",
                emp1.getId(), paymentService.getId(), ImpactLevel.ENTERPRISE_WIDE, UrgencyLevel.IMMEDIATE, IncidentCategory.APPLICATION
        );

        Incident inc3 = incidentService.reportIncident(
                "VPN split-tunnel routing latency in Block C",
                "Employees experiencing packet loss when connecting to internal Git server over campus WiFi.",
                emp2.getId(), netService.getId(), ImpactLevel.MEDIUM, UrgencyLevel.MEDIUM, IncidentCategory.NETWORK
        );
        incidentService.transitionIncidentState(inc3.getIncidentKey(), IncidentStatus.ACKNOWLEDGED, tech2.getName(), "Acknowledged network ticket.");
        incidentService.transitionIncidentState(inc3.getIncidentKey(), IncidentStatus.IN_PROGRESS, tech2.getName(), "Updated BGP routing table on core switch.");
        incidentService.transitionIncidentState(inc3.getIncidentKey(), IncidentStatus.RESOLVED, tech2.getName(), "Flushed stale ARP cache and rebalanced VLAN 40 trunk port.");
    }
}
