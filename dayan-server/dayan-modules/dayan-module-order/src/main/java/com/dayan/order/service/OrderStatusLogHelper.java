package com.dayan.order.service;

import com.dayan.order.entity.OrderStatusLogView;
import com.dayan.order.mapper.OrderStatusLogViewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单状态变更日志写入辅助组件（跨 4 类订单共享）。
 *
 * <p>每次状态机 transition 成功后调用 {@link #writeLog} 写一条 system_order_status_log。
 * 默认操作人补齐：operatorCode 为空→"system"；operatorType 为空→"system"。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusLogHelper {

    private final OrderStatusLogViewMapper statusLogMapper;

    /**
     * 写订单状态变更日志。
     *
     * @param orderType     订单类型（{@link com.dayan.order.enums.OrderType}）
     * @param orderCode     订单编号
     * @param fromStatus    原状态
     * @param toStatus      新状态
     * @param changeReason  变更原因（可空）
     * @param operatorCode  操作人编码（可空默认 system）
     * @param operatorName  操作人姓名（可空）
     * @param operatorType  操作人类型（可空默认 system）
     * @param remark        备注（可空）
     */
    public void writeLog(int orderType, String orderCode,
                         Integer fromStatus, Integer toStatus,
                         String changeReason,
                         String operatorCode, String operatorName, String operatorType,
                         String remark) {
        OrderStatusLogView log = new OrderStatusLogView();
        log.setOrderType(orderType);
        log.setOrderCode(orderCode);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setChangeReason(changeReason);
        log.setOperatorCode(operatorCode == null || operatorCode.isBlank() ? "system" : operatorCode);
        log.setOperatorName(operatorName);
        log.setOperatorType(operatorType == null || operatorType.isBlank() ? "system" : operatorType);
        log.setOperateTime(LocalDateTime.now());
        log.setRemark(remark);
        statusLogMapper.insert(log);
    }
}
