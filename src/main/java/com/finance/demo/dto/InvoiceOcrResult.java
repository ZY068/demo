package com.finance.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoiceOcrResult {
    private boolean success;
    private String errorMessage;
    private InvoiceParseResult parsedData;
    private BigDecimal confidence;
    private String rawText;

    public static InvoiceOcrResult success(InvoiceParseResult data, BigDecimal confidence, String rawText) {
        InvoiceOcrResult result = new InvoiceOcrResult();
        result.setSuccess(true);
        result.setParsedData(data);
        result.setConfidence(confidence);
        result.setRawText(rawText);
        return result;
    }

    public static InvoiceOcrResult fail(String errorMessage) {
        InvoiceOcrResult result = new InvoiceOcrResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
}
