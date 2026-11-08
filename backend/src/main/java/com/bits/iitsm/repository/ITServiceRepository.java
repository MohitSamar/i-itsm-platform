package com.bits.iitsm.repository;

import com.bits.iitsm.domain.entity.ITService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ITServiceRepository extends JpaRepository<ITService, UUID> {
    Optional<ITService> findByServiceCode(String serviceCode);
}
