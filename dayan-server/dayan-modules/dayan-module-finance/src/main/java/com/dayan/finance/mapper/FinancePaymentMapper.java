package com.dayan.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.finance.entity.FinancePayment;
import org.apache.ibatis.annotations.Mapper;

/**
 * finance_payment 数据访问层。
 */
@Mapper
public interface FinancePaymentMapper extends BaseMapper<FinancePayment> {
}
