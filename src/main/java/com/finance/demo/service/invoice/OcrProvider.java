package com.finance.demo.service.invoice;

import com.finance.demo.dto.InvoiceOcrResult;

public interface OcrProvider {
    /**
     * 识别发票
     * @param imageData 图片字节数据
     * @param imageType 图片类型（jpg/png/pdf）
     * @return 识别结果
     */
    InvoiceOcrResult invokeOcr(byte[] imageData, String imageType);

    /**
     * 获取提供商名称
     */
    String getProviderName();
}
