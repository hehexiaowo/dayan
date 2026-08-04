package com.dayan.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.finance.entity.FinanceReconciliation;
import org.apache.ibatis.annotations.Mapper;

/**
 * finance_reconciliation 数据访问层。
 */
@Mapper
public interface FinanceReconciliationMapper extends BaseMapper<FinanceReconciliation> {
}
