package com.finance.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.demo.entity.Department;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
