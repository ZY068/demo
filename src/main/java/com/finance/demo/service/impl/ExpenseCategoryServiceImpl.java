package com.finance.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.demo.entity.ExpenseCategory;
import com.finance.demo.mapper.ExpenseCategoryMapper;
import com.finance.demo.service.ExpenseCategoryService;
import org.springframework.stereotype.Service;

@Service
public class ExpenseCategoryServiceImpl extends ServiceImpl<ExpenseCategoryMapper, ExpenseCategory> implements ExpenseCategoryService {
}
