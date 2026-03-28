package com.finance.demo.controller;

import com.finance.demo.common.Result;
import com.finance.demo.dto.InvoiceOcrResult;
import com.finance.demo.entity.Invoice;
import com.finance.demo.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @PostMapping("/ocr")
    public Result<InvoiceOcrResult> recognizeInvoice(@RequestParam("file") MultipartFile file) {
        try {
            byte[] imageData = file.getBytes();
            Invoice invoice = invoiceService.uploadAndRecognize(imageData, file.getOriginalFilename(), null);
            InvoiceOcrResult result = new InvoiceOcrResult();
            result.setSuccess(true);
            result.setParsedData(null);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error("上传文件失败: " + e.getMessage());
        } catch (Exception e) {
            return Result.error("OCR识别失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<Invoice> getInvoice(@PathVariable Integer id) {
        Invoice invoice = invoiceService.getInvoiceDetail(id);
        if (invoice == null) {
            return Result.error("发票不存在");
        }
        return Result.success(invoice);
    }

    @PostMapping("/{id}/recognize")
    public Result<InvoiceOcrResult> reRecognize(@PathVariable Integer id) {
        InvoiceOcrResult result = invoiceService.recognizeInvoice(id);
        return result.isSuccess() ? Result.success(result) : Result.error(result.getErrorMessage());
    }

    @GetMapping("/provider")
    public Result<String> getProvider() {
        // This would need to be exposed by the service
        return Result.success("local");
    }
}
