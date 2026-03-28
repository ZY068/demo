package com.finance.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.demo.entity.Invoice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {
}
