package com.finance.demo.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.finance.demo.common.Result;
import com.finance.demo.entity.Department;
import com.finance.demo.entity.DepartmentBudget;
import com.finance.demo.service.DepartmentService;
import com.finance.demo.service.DepartmentBudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentBudgetService budgetService;

    @GetMapping
    public Result<List<Department>> getAllDepartments() {
        return Result.success(departmentService.list());
    }

    @GetMapping("/{id}")
    public Result<Department> getDepartment(@PathVariable Integer id) {
        Department dept = departmentService.getById(id);
        return dept != null ? Result.success(dept) : Result.error("部门不存在");
    }

    @GetMapping("/{id}/budget")
    public Result<DepartmentBudget> getDepartmentBudget(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer year) {
        int budgetYear = year != null ? year : LocalDate.now().getYear();
        DepartmentBudget budget = budgetService.lambdaQuery()
            .eq(DepartmentBudget::getDeptId, id)
            .eq(DepartmentBudget::getBudgetYear, budgetYear)
            .one();
        return Result.success(budget);
    }

    @PutMapping("/{id}/budget")
    public Result<Void> updateBudget(@PathVariable Integer id, @RequestBody DepartmentBudget budget) {
        budget.setDeptId(id);
        if (budget.getBudgetYear() == null) {
            budget.setBudgetYear(LocalDate.now().getYear());
        }

        // 先查询是否已存在该部门的预算
        DepartmentBudget existing = budgetService.lambdaQuery()
            .eq(DepartmentBudget::getDeptId, id)
            .eq(DepartmentBudget::getBudgetYear, budget.getBudgetYear())
            .one();

        if (existing != null) {
            // 存在则通过 UpdateWrapper 更新，避免乐观锁版本问题
            LambdaUpdateWrapper<DepartmentBudget> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(DepartmentBudget::getId, existing.getId())
                   .set(existing.getBudgetAmount() != null, DepartmentBudget::getBudgetAmount, budget.getBudgetAmount());
            boolean success = budgetService.update(wrapper);
            return success ? Result.success(null) : Result.error("更新失败");
        } else {
            // 不存在则新增
            budget.setUsedAmount(BigDecimal.ZERO);
            budget.setFrozenAmount(BigDecimal.ZERO);
            return budgetService.save(budget) ? Result.success(null) : Result.error("保存失败");
        }
    }
}
