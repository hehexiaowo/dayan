package com.dayan.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.finance.entity.FinanceFlow;
import org.apache.ibatis.annotations.Mapper;

/**
 * finance_flow 数据访问层。
 */
@Mapper
public interface FinanceFlowMapper extends BaseMapper<FinanceFlow> {
}
