package com.finance.demo.service.invoice.impl;

import com.finance.demo.dto.InvoiceOcrResult;
import com.finance.demo.dto.InvoiceParseResult;
import com.finance.demo.service.invoice.OcrProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component("aliyun")
public class AliyunOcrProvider implements OcrProvider {

    @Value("${aliyun.ocr.access-key:}")
    private String accessKey;

    @Value("${aliyun.ocr.access-secret:}")
    private String accessSecret;

    @Value("${aliyun.ocr.region:cn-shanghai}")
    private String region;

    @Override
    public InvoiceOcrResult invokeOcr(byte[] imageData, String imageType) {
        // 阿里云OCR集成 - 实际使用时需要配置AK/SK
        if (accessKey == null || accessKey.isEmpty()) {
            return InvoiceOcrResult.fail("阿里云OCR未配置，请设置 aliyun.ocr.access-key 和 aliyun.ocr.access-secret");
        }

        try {
            // 实际实现需要引入阿里云OCR SDK并调用API
            // 这里提供一个模拟实现作为占位
            InvoiceParseResult parseResult = new InvoiceParseResult();
            parseResult.setInvoiceNumber("ALIYUN_INV" + System.currentTimeMillis());
            parseResult.setInvoiceCode("11" + String.format("%012d", System.currentTimeMillis() % 1000000000000L));
            parseResult.setInvoiceType("VAT_NORMAL");
            parseResult.setAmount(new BigDecimal("100.00"));
            parseResult.setTaxAmount(new BigDecimal("13.00"));
            parseResult.setTotalAmount(new BigDecimal("113.00"));
            parseResult.setIssueDate(LocalDate.now());
            parseResult.setSellerName("阿里云测试销售方");
            parseResult.setSellerTaxNo("91310000MA1K3ABCXY");
            parseResult.setBuyerName("阿里云测试购买方");
            parseResult.setBuyerTaxNo("91310000MA1K3ABCXY");

            return InvoiceOcrResult.success(parseResult, new BigDecimal("0.98"), "阿里云OCR原始识别文本");
        } catch (Exception e) {
            return InvoiceOcrResult.fail("阿里云OCR识别失败: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "aliyun";
    }
}
