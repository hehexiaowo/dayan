package com.dayan.order.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.order.dto.CreateOrderCourseDTO;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.dto.OrderCompleteDTO;
import com.dayan.order.dto.OrderCourseQueryDTO;
import com.dayan.order.dto.PayCallbackDTO;
import com.dayan.order.dto.RefundApplyDTO;
import com.dayan.order.entity.OrderCourse;
import com.dayan.order.vo.OrderCourseVO;

import java.util.List;

/**
 * 课程购买订单（order_course）服务 —— 核心链路。
 *
 * <p>所有 order_status 变更必须经 {@code StateMachineEngine.transition("ORDER_SM", from, event)}。
 * 每次状态流转后写一条 system_order_status_log（经 OrderStatusLogHelper）。
 */
public interface OrderCourseService {

    PageResult<OrderCourseVO> page(OrderCourseQueryDTO query);

    List<OrderCourseVO> list(OrderCourseQueryDTO query);

    OrderCourseVO getDetail(String orderCode);

    /** 查询实体（不存在抛业务异常） */
    OrderCourse requireOrder(String orderCode);

    /** 创建订单：生成订单号 + 校验金额 + 置 order_status=0 → 返回 orderCode */
    String create(CreateOrderCourseDTO dto);

    /** 支付回调：0→1 + 写 payTime/payTradeNo/payType */
    void payCallback(PayCallbackDTO dto);

    /** 申请退款：1/2/3→6 */
    void applyRefund(RefundApplyDTO dto);

    /** 取消订单：0→5 或 6→5 + cancelReason */
    void cancel(OrderCancelDTO dto);

    /** 完成订单：3→4 */
    void complete(OrderCompleteDTO dto);
}
