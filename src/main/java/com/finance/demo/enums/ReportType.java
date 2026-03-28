package com.finance.demo.enums;

public enum ReportType {
    COMPLIANCE("COMPLIANCE", "合规审计报告"),
    PROCESS("PROCESS", "流程审计报告"),
    DATA_ANALYSIS("DATA_ANALYSIS", "数据分析报告");

    private final String code;
    private final String desc;

    ReportType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
