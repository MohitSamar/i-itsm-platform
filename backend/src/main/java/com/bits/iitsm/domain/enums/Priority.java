package com.bits.iitsm.domain.enums;

public enum Priority {
    P1("Critical", 5, 30),
    P2("High", 15, 120),
    P3("Medium", 60, 480),
    P4("Low", 240, 1440);

    private final String label;
    private final long defaultResponseMinutes;
    private final long defaultResolutionMinutes;

    Priority(String label, long defaultResponseMinutes, long defaultResolutionMinutes) {
        this.label = label;
        this.defaultResponseMinutes = defaultResponseMinutes;
        this.defaultResolutionMinutes = defaultResolutionMinutes;
    }

    public String getLabel() { return label; }
    public long getDefaultResponseMinutes() { return defaultResponseMinutes; }
    public long getDefaultResolutionMinutes() { return defaultResolutionMinutes; }
}
