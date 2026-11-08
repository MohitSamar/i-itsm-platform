package com.bits.iitsm.service;

import com.bits.iitsm.domain.entity.ITService;
import com.bits.iitsm.domain.entity.Incident;
import com.bits.iitsm.domain.enums.IncidentCategory;
import com.bits.iitsm.domain.enums.IncidentStatus;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IntelligenceService {

    public IncidentCategory inferCategoryFromText(String title, String description, ITService fallbackService) {
        String combined = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();
        if (combined.contains("password") || combined.contains("login") || combined.contains("ldap") || combined.contains("sso") || combined.contains("auth")) {
            return IncidentCategory.AUTHENTICATION;
        }
        if (combined.contains("database") || combined.contains("sql") || combined.contains("postgres") || combined.contains("query") || combined.contains("connection pool")) {
            return IncidentCategory.DATABASE;
        }
        if (combined.contains("wifi") || combined.contains("network") || combined.contains("dns") || combined.contains("latency") || combined.contains("vpn")) {
            return IncidentCategory.NETWORK;
        }
        if (combined.contains("payment") || combined.contains("transaction") || combined.contains("portal") || combined.contains("app") || combined.contains("500")) {
            return IncidentCategory.APPLICATION;
        }
        if (combined.contains("disk") || combined.contains("storage") || combined.contains("s3") || combined.contains("backup")) {
            return IncidentCategory.STORAGE;
        }
        if (combined.contains("breach") || combined.contains("phishing") || combined.contains("firewall") || combined.contains("unauthorized")) {
            return IncidentCategory.SECURITY;
        }
        if (combined.contains("aws") || combined.contains("cloud") || combined.contains("kubernetes") || combined.contains("container")) {
            return IncidentCategory.CLOUD;
        }
        if (combined.contains("laptop") || combined.contains("printer") || combined.contains("router") || combined.contains("server rack")) {
            return IncidentCategory.HARDWARE;
        }
        return fallbackService != null ? fallbackService.getDefaultCategory() : IncidentCategory.SOFTWARE;
    }

    public Optional<String> findPotentialDuplicate(String title, String description, UUID serviceId, List<Incident> existingIncidents) {
        Set<String> newTokens = tokenize(title + " " + description);
        for (Incident candidate : existingIncidents) {
            if (candidate.getStatus() == IncidentStatus.CLOSED) continue;
            boolean sameService = candidate.getAffectedService() != null
                    && Objects.equals(candidate.getAffectedService().getId(), serviceId);
            Set<String> candidateTokens = tokenize(candidate.getTitle() + " " + candidate.getDescription());
            double similarity = jaccardSimilarity(newTokens, candidateTokens);
            if ((sameService && similarity >= 0.25) || similarity >= 0.45) {
                return Optional.of(candidate.getIncidentKey());
            }
        }
        return Optional.empty();
    }

    public Optional<String> findUpstreamRootCauseIncident(ITService affectedService, List<Incident> existingIncidents) {
        if (affectedService == null || affectedService.getDependencies().isEmpty()) {
            return Optional.empty();
        }
        Set<UUID> upstreamIds = affectedService.getDependencies().stream()
                .map(ITService::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return existingIncidents.stream()
                .filter(inc -> inc.getStatus() != IncidentStatus.CLOSED && inc.getStatus() != IncidentStatus.RESOLVED)
                .filter(inc -> inc.getAffectedService() != null && upstreamIds.contains(inc.getAffectedService().getId()))
                .map(Incident::getIncidentKey)
                .findFirst();
    }

    public List<String> suggestResolutions(Incident target, List<Incident> allIncidents) {
        Set<String> targetTokens = tokenize(target.getTitle() + " " + target.getDescription());
        List<String> suggestions = allIncidents.stream()
                .filter(inc -> !inc.getIncidentKey().equals(target.getIncidentKey()))
                .filter(inc -> inc.getResolutionDetails() != null && !inc.getResolutionDetails().isBlank())
                .filter(inc -> inc.getCategory() == target.getCategory()
                        || jaccardSimilarity(targetTokens, tokenize(inc.getTitle() + " " + inc.getDescription())) >= 0.2)
                .map(inc -> "[" + inc.getIncidentKey() + "] " + inc.getResolutionDetails())
                .distinct()
                .limit(3)
                .collect(Collectors.toList());

        if (suggestions.isEmpty()) {
            suggestions.add("Verify upstream service dependencies (" + target.getAffectedService().getServiceName() + "), restart connection pool, and validate health check endpoints.");
        }
        return suggestions;
    }

    private Set<String> tokenize(String text) {
        if (text == null) return Collections.emptySet();
        Set<String> stopWords = Set.of("the", "and", "unable", "to", "is", "in", "of", "for", "on", "with", "are", "not");
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(w -> w.length() > 2 && !stopWords.contains(w))
                .collect(Collectors.toSet());
    }

    private double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }
}
