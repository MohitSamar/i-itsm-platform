package com.bits.iitsm.domain.enums;

public enum CriticalityLevel {
    LOW(1), MODERATE(2), HIGH(3), MISSION_CRITICAL(4);

    private final int weight;
    CriticalityLevel(int weight) { this.weight = weight; }
    public int getWeight() { return weight; }
}
