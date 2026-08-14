package com.dayan.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.order.entity.OrderStatusChangeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单状态变更日志 View 数据访问层。
 *
 * <p>映射物理表 {@code order_status_change_record}（跨域共享表），供订单域写日志使用，
 * 不依赖系统域模块。详见 {@link OrderStatusChangeRecord}。
 */
@Mapper
public interface OrderStatusChangeRecordMapper extends BaseMapper<OrderStatusChangeRecord> {
}
