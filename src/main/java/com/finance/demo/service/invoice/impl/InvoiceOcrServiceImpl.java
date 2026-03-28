package com.finance.demo.service.invoice.impl;

import com.finance.demo.dto.InvoiceOcrResult;
import com.finance.demo.service.invoice.InvoiceOcrService;
import com.finance.demo.service.invoice.OcrProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class InvoiceOcrServiceImpl implements InvoiceOcrService {

    @Value("${ocr.provider:local}")
    private String activeProvider;

    @Autowired
    private Map<String, OcrProvider> ocrProviders;

    private OcrProvider provider;

    @PostConstruct
    public void init() {
        this.provider = ocrProviders.get(activeProvider);
        if (this.provider == null) {
            this.provider = ocrProviders.get("local");
        }
    }

    @Override
    public InvoiceOcrResult recognize(byte[] imageData, String imageType) {
        if (provider == null) {
            return InvoiceOcrResult.fail("OCR提供商未配置");
        }
        return provider.invokeOcr(imageData, imageType);
    }

    @Override
    public InvoiceOcrResult recognizeFromFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            byte[] imageData = Files.readAllBytes(path);
            String imageType = detectImageType(filePath);
            return recognize(imageData, imageType);
        } catch (IOException e) {
            return InvoiceOcrResult.fail("读取文件失败: " + e.getMessage());
        }
    }

    @Override
    public String getCurrentProvider() {
        return provider != null ? provider.getProviderName() : "none";
    }

    private String detectImageType(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else {
            return "image/jpeg";
        }
    }
}
