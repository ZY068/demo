package com.finance.demo.mapper;
import com.finance.demo.entity.ExpenseClaim;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ExpenseClaimMapper extends BaseMapper<ExpenseClaim> {

    // 💡 改用 WEEKDAY 函数：5 是周六，6 是周日
    @Select("SELECT * FROM expense_claim WHERE WEEKDAY(expense_date) >= 5")
    List<ExpenseClaim> selectWeekendClaims();

    @Select("SELECT COUNT(*) FROM expense_claim WHERE user_id = #{userId} AND expense_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)")
    int selectCountInWeek(Integer userId);
}