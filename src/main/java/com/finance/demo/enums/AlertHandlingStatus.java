package com.finance.demo.enums;

public enum AlertHandlingStatus {
    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    RESOLVED(2, "已处理"),
    FALSE_ALARM(3, "误报");

    private final int code;
    private final String desc;

    AlertHandlingStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
