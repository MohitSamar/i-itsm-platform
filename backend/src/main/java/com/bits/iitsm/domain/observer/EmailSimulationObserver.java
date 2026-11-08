package com.bits.iitsm.domain.observer;

import com.bits.iitsm.domain.entity.NotificationLog;
import com.bits.iitsm.repository.NotificationLogRepository;
import org.springframework.stereotype.Component;

@Component
public class EmailSimulationObserver implements IncidentObserver {

    private final NotificationLogRepository notificationRepository;

    public EmailSimulationObserver(NotificationLogRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void onIncidentEvent(IncidentEvent event) {
        String emailBody = "[SMTP SIMULATED EMAIL to " + event.getRecipient() + "] " + event.getMessage();
        notificationRepository.save(new NotificationLog(
                event.getIncident().getIncidentKey(),
                event.getRecipient(),
                "EMAIL_SMTP",
                event.getEventType(),
                emailBody
        ));
    }

    @Override
    public String getChannelName() {
        return "EMAIL_SMTP";
    }
}
