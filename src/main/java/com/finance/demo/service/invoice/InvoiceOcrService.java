package com.finance.demo.service.invoice;

import com.finance.demo.dto.InvoiceOcrResult;

public interface InvoiceOcrService {
    /**
     * 识别发票
     * @param imageData 图片字节数据
     * @param imageType 图片类型（image/jpeg, image/png, application/pdf）
     * @return 识别结果
     */
    InvoiceOcrResult recognize(byte[] imageData, String imageType);

    /**
     * 识别发票（通过文件路径）
     * @param filePath 文件路径
     * @return 识别结果
     */
    InvoiceOcrResult recognizeFromFile(String filePath);

    /**
     * 获取当前OCR提供商
     */
    String getCurrentProvider();
}
