package com.finance.demo.mapper;

import com.finance.demo.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    // 继承 BaseMapper 后，MyBatis-Plus 会自动帮你实现 selectOne 等基础功能
}