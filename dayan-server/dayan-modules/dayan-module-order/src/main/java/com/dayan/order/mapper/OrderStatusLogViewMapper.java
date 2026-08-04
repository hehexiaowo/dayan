package com.dayan.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.order.entity.OrderStatusLogView;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单状态变更日志 View 数据访问层。
 *
 * <p>映射物理表 {@code system_order_status_log}（跨域共享表），供订单域写日志使用，
 * 不依赖系统域模块。详见 {@link OrderStatusLogView}。
 */
@Mapper
public interface OrderStatusLogViewMapper extends BaseMapper<OrderStatusLogView> {
}
