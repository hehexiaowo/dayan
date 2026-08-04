package com.dayan.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.finance.entity.FinanceAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * finance_account 数据访问层。
 */
@Mapper
public interface FinanceAccountMapper extends BaseMapper<FinanceAccount> {
}
