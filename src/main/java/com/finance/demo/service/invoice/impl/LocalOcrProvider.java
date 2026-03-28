package com.finance.demo.service.invoice.impl;

import com.finance.demo.dto.InvoiceOcrResult;
import com.finance.demo.dto.InvoiceParseResult;
import com.finance.demo.service.invoice.OcrProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component("local")
public class LocalOcrProvider implements OcrProvider {

    @Override
    public InvoiceOcrResult invokeOcr(byte[] imageData, String imageType) {
        try {
            // 模拟OCR识别 - 实际应用中这里会调用真实的OCR服务
            InvoiceParseResult parseResult = new InvoiceParseResult();
            parseResult.setInvoiceNumber("INV" + System.currentTimeMillis());
            parseResult.setInvoiceCode("11" + String.format("%012d", System.currentTimeMillis() % 1000000000000L));
            parseResult.setInvoiceType("VAT_NORMAL");
            parseResult.setAmount(new BigDecimal("100.00"));
            parseResult.setTaxAmount(new BigDecimal("13.00"));
            parseResult.setTotalAmount(new BigDecimal("113.00"));
            parseResult.setIssueDate(LocalDate.now());
            parseResult.setSellerName("示例销售方");
            parseResult.setSellerTaxNo("91110000123456789X");
            parseResult.setBuyerName("示例购买方");
            parseResult.setBuyerTaxNo("91110000123456789Y");

            return InvoiceOcrResult.success(parseResult, new BigDecimal("0.95"), "模拟OCR识别文本");
        } catch (Exception e) {
            return InvoiceOcrResult.fail("模拟OCR识别失败: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "local";
    }
}
