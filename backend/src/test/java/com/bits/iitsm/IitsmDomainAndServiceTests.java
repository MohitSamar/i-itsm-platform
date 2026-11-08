package com.bits.iitsm;

import com.bits.iitsm.domain.entity.*;
import com.bits.iitsm.domain.enums.*;
import com.bits.iitsm.domain.factory.IncidentFactory;
import com.bits.iitsm.domain.state.IncidentStateValidator;
import com.bits.iitsm.domain.strategy.*;
import com.bits.iitsm.exception.InvalidStateTransitionException;
import com.bits.iitsm.exception.WorkloadExceededException;
import com.bits.iitsm.service.IntelligenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IitsmDomainAndServiceTests {

    private IncidentStateValidator stateValidator;
    private WeightedScorePriorityStrategy priorityStrategy;
    private IncidentFactory incidentFactory;
    private IntelligenceService intelligenceService;
    private Employee sampleEmployee;
    private ITService paymentService;
    private ITService databaseService;

    @BeforeEach
    void setUp() {
        stateValidator = new IncidentStateValidator();
        priorityStrategy = new WeightedScorePriorityStrategy();
        incidentFactory = new IncidentFactory(priorityStrategy, stateValidator);
        intelligenceService = new IntelligenceService();

        sampleEmployee = new Employee("Aarav Mehta", "aarav@campus.bits.edu", "pass123", "Finance", "Block A");
        databaseService = new ITService("SVC-DB", "Core Database", IncidentCategory.DATABASE, CriticalityLevel.MISSION_CRITICAL);
        paymentService = new ITService("SVC-PAY", "Payment Application", IncidentCategory.APPLICATION, CriticalityLevel.MISSION_CRITICAL);
        paymentService.addDependency(databaseService);
    }

    @Test
    @DisplayName("TC01 [OOP Inheritance & Polymorphism]: User subclasses return distinct role dashboard routes")
    void tc01_userInheritanceAndPolymorphism() {
        User emp = sampleEmployee;
        User tech = new Technician("Rohan", "rohan@bits.edu", "pass", Set.of(IncidentCategory.APPLICATION), 5, 3, "L1");
        User admin = new Administrator("Dr. Rao", "admin@bits.edu", "pass", "L5");

        assertEquals("/employee/portal", emp.getDashboardRoute());
        assertEquals("/technician/workbench", tech.getDashboardRoute());
        assertEquals("/admin/command-center", admin.getDashboardRoute());
    }

    @Test
    @DisplayName("TC02 [Business Rule]: Weighted Priority Strategy calculates P1 for Enterprise-Wide Immediate incidents")
    void tc02_priorityStrategyCalculatesP1() {
        Priority result = priorityStrategy.calculatePriority(
                ImpactLevel.ENTERPRISE_WIDE, UrgencyLevel.IMMEDIATE, CriticalityLevel.MISSION_CRITICAL);
        assertEquals(Priority.P1, result);
    }

    @Test
    @DisplayName("TC03 [Boundary Condition]: Weighted Priority Strategy calculates P4 for Low Impact and Low Urgency")
    void tc03_priorityStrategyCalculatesP4Boundary() {
        Priority result = priorityStrategy.calculatePriority(
                ImpactLevel.LOW, UrgencyLevel.LOW, CriticalityLevel.LOW);
        assertEquals(Priority.P4, result);
    }

    @Test
    @DisplayName("TC04 [Boundary Condition]: Weighted Priority Strategy distinguishes P2 and P3 thresholds")
    void tc04_priorityStrategyP2AndP3Boundaries() {
        Priority p2 = priorityStrategy.calculatePriority(ImpactLevel.HIGH, UrgencyLevel.HIGH, CriticalityLevel.HIGH);
        Priority p3 = priorityStrategy.calculatePriority(ImpactLevel.MEDIUM, UrgencyLevel.MEDIUM, CriticalityLevel.MODERATE);
        assertEquals(Priority.P2, p2);
        assertEquals(Priority.P3, p3);
    }

    @Test
    @DisplayName("TC05 [Normal Operation]: State Pattern allows valid end-to-end Incident Lifecycle progression")
    void tc05_statePatternValidLifecycle() {
        SLAPolicy p1Sla = new SLAPolicy(Priority.P1, 5, 30);
        Incident inc = incidentFactory.createAndClassifyIncident(
                "Payment outage", "Checkout failing", sampleEmployee, paymentService,
                ImpactLevel.ENTERPRISE_WIDE, UrgencyLevel.IMMEDIATE, IncidentCategory.APPLICATION, p1Sla);

        assertEquals(IncidentStatus.CLASSIFIED, inc.getStatus());

        inc.transitionState(IncidentStatus.ASSIGNED, stateValidator, "Dispatcher", "Assigned to Rohan");
        inc.transitionState(IncidentStatus.ACKNOWLEDGED, stateValidator, "Rohan", "Acknowledged");
        assertNotNull(inc.getAcknowledgedAt());

        inc.transitionState(IncidentStatus.IN_PROGRESS, stateValidator, "Rohan", "Investigating logs");
        inc.transitionState(IncidentStatus.RESOLVED, stateValidator, "Rohan", "Restarted gateway");
        assertNotNull(inc.getResolvedAt());

        inc.transitionState(IncidentStatus.VERIFIED, stateValidator, "Aarav", "Verified working");
        inc.transitionState(IncidentStatus.CLOSED, stateValidator, "Aarav", "Closed ticket");
        assertEquals(IncidentStatus.CLOSED, inc.getStatus());
        assertNotNull(inc.getClosedAt());
        assertEquals(8, inc.getAuditTrail().size());
    }

    @Test
    @DisplayName("TC06 [Exception Handling]: State Pattern blocks illegal direct transition from NEW to CLOSED")
    void tc06_statePatternBlocksNewToClosed() {
        Incident inc = new Incident("INC-9001", "Test", "Desc", sampleEmployee, paymentService, ImpactLevel.LOW, UrgencyLevel.LOW);
        assertThrows(InvalidStateTransitionException.class, () ->
                inc.transitionState(IncidentStatus.CLOSED, stateValidator, "Attacker", "Bypass lifecycle"));
    }

    @Test
    @DisplayName("TC07 [Exception Handling]: State Pattern blocks transition from ASSIGNED directly to RESOLVED")
    void tc07_statePatternBlocksAssignedToResolved() {
        SLAPolicy sla = new SLAPolicy(Priority.P2, 15, 120);
        Incident inc = incidentFactory.createAndClassifyIncident(
                "DB slow", "Queries timing out", sampleEmployee, databaseService,
                ImpactLevel.HIGH, UrgencyLevel.HIGH, IncidentCategory.DATABASE, sla);
        inc.transitionState(IncidentStatus.ASSIGNED, stateValidator, "System", "Assigned");

        assertThrows(InvalidStateTransitionException.class, () ->
                inc.transitionState(IncidentStatus.RESOLVED, stateValidator, "Tech", "Skipped investigation"));
    }

    @Test
    @DisplayName("TC08 [Boundary & Exception]: Technician workload enforces maximum capacity and throws WorkloadExceededException")
    void tc08_technicianWorkloadBoundaryAndException() {
        Technician tech = new Technician("Vikram", "vikram@bits.edu", "pass", Set.of(IncidentCategory.CLOUD), 2, 5, "L1");
        assertTrue(tech.canAcceptIncident());
        tech.incrementWorkload();
        tech.incrementWorkload();
        assertFalse(tech.canAcceptIncident());

        assertThrows(WorkloadExceededException.class, tech::incrementWorkload);
    }

    @Test
    @DisplayName("TC09 [Strategy Pattern]: SkillAndExperienceAssignmentStrategy selects technician with matching category skill")
    void tc09_skillStrategySelectsSkilledTechnician() {
        Technician netTech = new Technician("Sneha", "sneha@bits.edu", "pass", Set.of(IncidentCategory.NETWORK), 5, 3, "L1");
        Technician dbTech = new Technician("Rohan", "rohan@bits.edu", "pass", Set.of(IncidentCategory.DATABASE), 5, 4, "L1");

        Incident dbIncident = incidentFactory.createAndClassifyIncident(
                "Postgres lock", "Deadlock detected", sampleEmployee, databaseService,
                ImpactLevel.HIGH, UrgencyLevel.HIGH, IncidentCategory.DATABASE, null);

        SkillAndExperienceAssignmentStrategy strategy = new SkillAndExperienceAssignmentStrategy();
        Optional<Technician> chosen = strategy.selectTechnician(dbIncident, List.of(netTech, dbTech));

        assertTrue(chosen.isPresent());
        assertEquals("Rohan", chosen.get().getName());
    }

    @Test
    @DisplayName("TC10 [Strategy Pattern]: LeastLoadedAssignmentStrategy selects technician with lowest active workload")
    void tc10_leastLoadedStrategySelectsLowestWorkload() {
        Technician busyTech = new Technician("BusyTech", "b@bits.edu", "pass", Set.of(IncidentCategory.APPLICATION), 5, 5, "L1");
        busyTech.incrementWorkload();
        busyTech.incrementWorkload();

        Technician freeTech = new Technician("FreeTech", "f@bits.edu", "pass", Set.of(IncidentCategory.APPLICATION), 5, 1, "L1");

        Incident inc = incidentFactory.createAndClassifyIncident(
                "App error", "HTTP 500", sampleEmployee, paymentService,
                ImpactLevel.MEDIUM, UrgencyLevel.MEDIUM, IncidentCategory.APPLICATION, null);

        LeastLoadedAssignmentStrategy strategy = new LeastLoadedAssignmentStrategy();
        Optional<Technician> chosen = strategy.selectTechnician(inc, List.of(busyTech, freeTech));

        assertTrue(chosen.isPresent());
        assertEquals("FreeTech", chosen.get().getName());
    }

    @Test
    @DisplayName("TC11 [Strategy Pattern]: RoundRobinAssignmentStrategy distributes assignments evenly across technicians")
    void tc11_roundRobinStrategyCyclesTechnicians() {
        Technician t1 = new Technician("Tech1", "t1@bits.edu", "pass", Set.of(IncidentCategory.APPLICATION), 5, 2, "L1");
        Technician t2 = new Technician("Tech2", "t2@bits.edu", "pass", Set.of(IncidentCategory.APPLICATION), 5, 2, "L1");
        Incident inc = incidentFactory.createAndClassifyIncident(
                "App issue", "Desc", sampleEmployee, paymentService,
                ImpactLevel.LOW, UrgencyLevel.LOW, IncidentCategory.APPLICATION, null);

        RoundRobinAssignmentStrategy rr = new RoundRobinAssignmentStrategy();
        String first = rr.selectTechnician(inc, List.of(t1, t2)).orElseThrow().getName();
        String second = rr.selectTechnician(inc, List.of(t1, t2)).orElseThrow().getName();

        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("TC12 [Boundary Condition]: AssignmentStrategy returns empty when all technicians are unavailable")
    void tc12_strategyReturnsEmptyWhenNoTechniciansAvailable() {
        Technician offlineTech = new Technician("Offline", "off@bits.edu", "pass", Set.of(IncidentCategory.APPLICATION), 5, 3, "L1");
        offlineTech.setAvailable(false);

        Incident inc = incidentFactory.createAndClassifyIncident(
                "Outage", "Desc", sampleEmployee, paymentService,
                ImpactLevel.HIGH, UrgencyLevel.HIGH, IncidentCategory.APPLICATION, null);

        SkillAndExperienceAssignmentStrategy strategy = new SkillAndExperienceAssignmentStrategy();
        assertTrue(strategy.selectTechnician(inc, List.of(offlineTech)).isEmpty());
    }

    @Test
    @DisplayName("TC13 [SLA Management]: SLAPolicy calculates exact response and resolution deadlines for P1 and P4")
    void tc13_slaPolicyDeadlineCalculation() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 9, 10, 0);
        SLAPolicy p1Policy = new SLAPolicy(Priority.P1, 5, 30);
        SLAPolicy p4Policy = new SLAPolicy(Priority.P4, 240, 1440);

        assertEquals(base.plusMinutes(5), p1Policy.calculateResponseDeadline(base));
        assertEquals(base.plusMinutes(30), p1Policy.calculateResolutionDeadline(base));
        assertEquals(base.plusHours(4), p4Policy.calculateResponseDeadline(base));
        assertEquals(base.plusHours(24), p4Policy.calculateResolutionDeadline(base));
    }

    @Test
    @DisplayName("TC14 [Intelligent Feature]: Auto-categorization engine infers category from symptom keywords")
    void tc14_intelligentCategoryInference() {
        assertEquals(IncidentCategory.AUTHENTICATION,
                intelligenceService.inferCategoryFromText("SSO Login failure", "LDAP password rejected", null));
        assertEquals(IncidentCategory.DATABASE,
                intelligenceService.inferCategoryFromText("Postgres timeout", "SQL query connection pool full", null));
        assertEquals(IncidentCategory.NETWORK,
                intelligenceService.inferCategoryFromText("Campus WiFi drop", "VPN latency and DNS failure", null));
    }

    @Test
    @DisplayName("TC15 [Intelligent Feature]: Duplicate detection identifies semantically similar active incidents")
    void tc15_duplicateIncidentDetection() {
        Incident existing = incidentFactory.createAndClassifyIncident(
                "Payment application unavailable and checkout failing",
                "Users unable to complete payment transaction on portal",
                sampleEmployee, paymentService, ImpactLevel.HIGH, UrgencyLevel.HIGH, IncidentCategory.APPLICATION, null);

        Optional<String> dupKey = intelligenceService.findPotentialDuplicate(
                "Unable to make payment transaction",
                "Payment application checkout failing for users",
                existing.getAffectedService().getId(),
                List.of(existing));

        assertTrue(dupKey.isPresent());
        assertEquals(existing.getIncidentKey(), dupKey.get());
    }

    @Test
    @DisplayName("TC16 [Innovation Feature]: Service Dependency Graph detects upstream root-cause outages")
    void tc16_serviceDependencyGraphUpstreamDetection() {
        databaseService.setOperationalStatus("OUTAGE");
        assertTrue(paymentService.hasUpstreamOutage());

        databaseService.setOperationalStatus("OPERATIONAL");
        assertFalse(paymentService.hasUpstreamOutage());
    }
}
