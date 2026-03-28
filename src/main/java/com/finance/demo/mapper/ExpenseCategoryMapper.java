package com.finance.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.demo.entity.ExpenseCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExpenseCategoryMapper extends BaseMapper<ExpenseCategory> {
}
