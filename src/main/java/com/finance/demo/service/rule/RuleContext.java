package com.finance.demo.service.rule;

import com.finance.demo.entity.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class RuleContext {
    private ExpenseClaim claim;
    private SysUser applicant;
    private Department department;
    private List<ExpenseClaimDetail> details;
    private List<Invoice> invoices;
    private LocalDate currentDate = LocalDate.now();
    private Map<String, Object> extraData;

    public BigDecimal getTotalAmount() {
        if (claim != null && claim.getAmount() != null) {
            return new BigDecimal(claim.getAmount()).divide(new BigDecimal(100));
        }
        return BigDecimal.ZERO;
    }

    public Integer getDayOfWeek() {
        LocalDate date = claim != null && claim.getExpenseDate() != null
            ? claim.getExpenseDate()
            : null;
        if (date == null) {
            return 0;  // 无日期则不判定
        }
        // Java DayOfWeek: 周一=1, 周日=7
        return date.getDayOfWeek().getValue();
    }
}
