package com.bits.iitsm.domain.observer;

import com.bits.iitsm.domain.entity.NotificationLog;
import com.bits.iitsm.repository.NotificationLogRepository;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationObserver implements IncidentObserver {

    private final NotificationLogRepository notificationRepository;

    public InAppNotificationObserver(NotificationLogRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void onIncidentEvent(IncidentEvent event) {
        notificationRepository.save(new NotificationLog(
                event.getIncident().getIncidentKey(),
                event.getRecipient(),
                "IN_APP_DASHBOARD",
                event.getEventType(),
                event.getMessage()
        ));
    }

    @Override
    public String getChannelName() {
        return "IN_APP_DASHBOARD";
    }
}
