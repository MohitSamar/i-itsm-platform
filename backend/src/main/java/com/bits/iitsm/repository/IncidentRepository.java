package com.bits.iitsm.repository;

import com.bits.iitsm.domain.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    Optional<Incident> findByIncidentKey(String incidentKey);
    List<Incident> findAllByOrderByCreatedAtDesc();
}
