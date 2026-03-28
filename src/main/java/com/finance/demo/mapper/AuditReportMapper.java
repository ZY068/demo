package com.finance.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.demo.entity.AuditReport;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditReportMapper extends BaseMapper<AuditReport> {
}
