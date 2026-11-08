package com.bits.iitsm.domain.enums;

public enum UrgencyLevel {
    LOW(1), MEDIUM(2), HIGH(3), IMMEDIATE(4);

    private final int weight;
    UrgencyLevel(int weight) { this.weight = weight; }
    public int getWeight() { return weight; }
}
