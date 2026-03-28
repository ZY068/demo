package com.finance.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.demo.dto.InvoiceOcrResult;
import com.finance.demo.entity.Invoice;
import com.finance.demo.mapper.InvoiceMapper;
import com.finance.demo.service.InvoiceService;
import com.finance.demo.service.invoice.InvoiceOcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements InvoiceService {

    @Autowired
    private InvoiceOcrService ocrService;

    @Override
    @Transactional
    public Invoice uploadAndRecognize(byte[] imageData, String fileName, Integer userId) {
        // 先保存发票记录
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("PENDING_" + System.currentTimeMillis());
        invoice.setInvoiceCode("PENDING");
        invoice.setInvoiceType("UNKNOWN");
        invoice.setOcrStatus(1); // 识别中
        baseMapper.insert(invoice);

        // 调用OCR识别
        String imageType = detectImageType(fileName);
        InvoiceOcrResult ocrResult = ocrService.recognize(imageData, imageType);

        if (ocrResult.isSuccess() && ocrResult.getParsedData() != null) {
            invoice.setInvoiceNumber(ocrResult.getParsedData().getInvoiceNumber());
            invoice.setInvoiceCode(ocrResult.getParsedData().getInvoiceCode());
            invoice.setInvoiceType(ocrResult.getParsedData().getInvoiceType());
            invoice.setAmount(ocrResult.getParsedData().getAmount());
            invoice.setTaxAmount(ocrResult.getParsedData().getTaxAmount());
            invoice.setTotalAmount(ocrResult.getParsedData().getTotalAmount());
            invoice.setIssueDate(ocrResult.getParsedData().getIssueDate());
            invoice.setSellerName(ocrResult.getParsedData().getSellerName());
            invoice.setSellerTaxNo(ocrResult.getParsedData().getSellerTaxNo());
            invoice.setBuyerName(ocrResult.getParsedData().getBuyerName());
            invoice.setBuyerTaxNo(ocrResult.getParsedData().getBuyerTaxNo());
            invoice.setOcrRawText(ocrResult.getRawText());
            invoice.setOcrConfidence(ocrResult.getConfidence());
            invoice.setOcrStatus(2); // 成功
        } else {
            invoice.setOcrStatus(3); // 失败
        }

        baseMapper.updateById(invoice);
        return invoice;
    }

    @Override
    public InvoiceOcrResult recognizeInvoice(Integer invoiceId) {
        Invoice invoice = baseMapper.selectById(invoiceId);
        if (invoice == null) {
            return InvoiceOcrResult.fail("发票不存在");
        }

        if (invoice.getOcrStatus() != null && invoice.getOcrStatus() == 2) {
            return InvoiceOcrResult.fail("发票已完成识别");
        }

        invoice.setOcrStatus(1); // 识别中
        baseMapper.updateById(invoice);

        // 假设有原始图片数据，这里简化为直接调用OCR
        InvoiceOcrResult result = ocrService.recognize(new byte[]{}, "image/jpeg");

        if (result.isSuccess() && result.getParsedData() != null) {
            updateInvoiceFromParseResult(invoice, result);
            invoice.setOcrStatus(2);
        } else {
            invoice.setOcrStatus(3);
        }
        baseMapper.updateById(invoice);

        return result;
    }

    @Override
    public Invoice getInvoiceDetail(Integer invoiceId) {
        return baseMapper.selectById(invoiceId);
    }

    private void updateInvoiceFromParseResult(Invoice invoice, InvoiceOcrResult result) {
        if (result.getParsedData() != null) {
            invoice.setInvoiceNumber(result.getParsedData().getInvoiceNumber());
            invoice.setInvoiceCode(result.getParsedData().getInvoiceCode());
            invoice.setInvoiceType(result.getParsedData().getInvoiceType());
            invoice.setAmount(result.getParsedData().getAmount());
            invoice.setTaxAmount(result.getParsedData().getTaxAmount());
            invoice.setTotalAmount(result.getParsedData().getTotalAmount());
            invoice.setIssueDate(result.getParsedData().getIssueDate());
            invoice.setSellerName(result.getParsedData().getSellerName());
            invoice.setSellerTaxNo(result.getParsedData().getSellerTaxNo());
            invoice.setBuyerName(result.getParsedData().getBuyerName());
            invoice.setBuyerTaxNo(result.getParsedData().getBuyerTaxNo());
            invoice.setOcrRawText(result.getRawText());
            invoice.setOcrConfidence(result.getConfidence());
        }
    }

    private String detectImageType(String fileName) {
        if (fileName == null) return "image/jpeg";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "image/jpeg";
    }
}
