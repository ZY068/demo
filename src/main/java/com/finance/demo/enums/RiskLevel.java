package com.finance.demo.enums;

public enum RiskLevel {
    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高"),
    SEVERE(4, "严重");

    private final int code;
    private final String desc;

    RiskLevel(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static RiskLevel fromCode(int code) {
        for (RiskLevel level : values()) {
            if (level.code == code) return level;
        }
        return LOW;
    }
}
