package com.bits.iitsm.domain.observer;

public interface IncidentObserver {
    void onIncidentEvent(IncidentEvent event);
    String getChannelName();
}
