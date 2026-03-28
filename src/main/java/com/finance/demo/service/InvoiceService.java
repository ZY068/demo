package com.finance.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.finance.demo.dto.InvoiceOcrResult;
import com.finance.demo.entity.Invoice;

public interface InvoiceService extends IService<Invoice> {
    /**
     * 上传发票并识别
     */
    Invoice uploadAndRecognize(byte[] imageData, String fileName, Integer userId);

    /**
     * 识别已有发票
     */
    InvoiceOcrResult recognizeInvoice(Integer invoiceId);

    /**
     * 获取发票详情
     */
    Invoice getInvoiceDetail(Integer invoiceId);
}
