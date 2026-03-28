package com.finance.demo.enums;

public enum ActionType {
    SUBMIT("SUBMIT", "提交"),
    APPROVE("APPROVE", "审批通过"),
    REJECT("REJECT", "审批驳回"),
    AUDIT("AUDIT", "审计");

    private final String code;
    private final String desc;

    ActionType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
