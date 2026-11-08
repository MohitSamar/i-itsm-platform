package com.bits.iitsm.repository;

import com.bits.iitsm.domain.entity.SLAPolicy;
import com.bits.iitsm.domain.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SLAPolicyRepository extends JpaRepository<SLAPolicy, Priority> {
}
