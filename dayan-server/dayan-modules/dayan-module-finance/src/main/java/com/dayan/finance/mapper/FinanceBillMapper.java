package com.dayan.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.finance.entity.FinanceBill;
import org.apache.ibatis.annotations.Mapper;

/**
 * finance_bill 数据访问层。
 */
@Mapper
public interface FinanceBillMapper extends BaseMapper<FinanceBill> {
}
