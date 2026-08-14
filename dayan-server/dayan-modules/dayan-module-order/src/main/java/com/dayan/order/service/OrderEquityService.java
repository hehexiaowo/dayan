package com.dayan.order.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.order.dto.CreateOrderEquityDTO;
import com.dayan.order.dto.EquityDeliverDTO;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.dto.OrderCompleteDTO;
import com.dayan.order.dto.OrderEquityQueryDTO;
import com.dayan.order.dto.PayCallbackDTO;
import com.dayan.order.dto.RefundApplyDTO;
import com.dayan.order.entity.OrderEquity;
import com.dayan.order.vo.OrderEquityVO;

import java.util.Collection;
import java.util.List;

/**
 * 权益采购订单（order_equity）服务 —— 核心链路。
 *
 * <p>承载权益订单全生命周期：创建 → 支付 → (部分)发放 → 完成 / 取消 / 退款。
 * 所有 order_status 变更必须经 {@code StateMachineEngine.transition("ORDER_SM", from, event)}。
 * 每次状态流转后写一条 order_status_change_record（经 OrderStatusChangeRecordMapper）。
 */
public interface OrderEquityService {

    PageResult<OrderEquityVO> page(OrderEquityQueryDTO query);

    List<OrderEquityVO> list(OrderEquityQueryDTO query);

    OrderEquityVO getDetail(String orderCode);

    /** 按订单编码批量查询（orderCode 唯一，返回结果与入参一一对应；空集合返回空列表）。 */
    List<OrderEquityVO> listByOrderCodes(Collection<String> orderCodes);

    /** 查询实体（不存在抛业务异常） */
    OrderEquity requireOrder(String orderCode);

    // ====== 核心链路 ======

    /** 创建订单：生成订单号 + 校验金额 + 置 order_status=0 + expire_time(now+30min) → 返回 orderCode */
    String create(CreateOrderEquityDTO dto);

    /** 支付回调：0→1 + 写 payTime/payTradeNo/payType */
    void payCallback(PayCallbackDTO dto);

    /** 申请退款：1/2/3→6 */
    void applyRefund(RefundApplyDTO dto);

    /** 取消订单：0→5 或 6→5 + cancelReason */
    void cancel(OrderCancelDTO dto);

    /** 完成订单：3→4 */
    void complete(OrderCompleteDTO dto);

    /** 权益发货：partialDeliver=true 时 1→2（累加 deliverCount）；false 时 1/2→3（deliverCount=quantity） */
    void deliver(EquityDeliverDTO dto);
}
