package com.finance.demo.mapper;

import com.finance.demo.entity.AuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
    // 继承 BaseMapper 即可，MP 会自动处理 insert 逻辑
}