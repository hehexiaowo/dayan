package com.dayan.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.finance.entity.FinanceInvoice;
import org.apache.ibatis.annotations.Mapper;

/**
 * finance_invoice 数据访问层。
 */
@Mapper
public interface FinanceInvoiceMapper extends BaseMapper<FinanceInvoice> {
}
