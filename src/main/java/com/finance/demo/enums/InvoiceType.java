package com.finance.demo.enums;

public enum InvoiceType {
    VAT_NORMAL("VAT_NORMAL", "增值税普通发票"),
    VAT_SPECIAL("VAT_SPECIAL", "增值税专用发票"),
    RECEIPT("RECEIPT", "收据"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String desc;

    InvoiceType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
