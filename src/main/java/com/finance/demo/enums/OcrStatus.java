package com.finance.demo.enums;

public enum OcrStatus {
    PENDING(0, "待识别"),
    PROCESSING(1, "识别中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败");

    private final int code;
    private final String desc;

    OcrStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
