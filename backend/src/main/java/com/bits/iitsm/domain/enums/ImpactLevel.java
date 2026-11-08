package com.bits.iitsm.domain.enums;

public enum ImpactLevel {
    LOW(1), MEDIUM(2), HIGH(3), ENTERPRISE_WIDE(4);

    private final int weight;
    ImpactLevel(int weight) { this.weight = weight; }
    public int getWeight() { return weight; }
}
