package com.bits.iitsm.repository;

import com.bits.iitsm.domain.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    List<NotificationLog> findTop50ByOrderByTimestampDesc();
}
