package com.dayan.order.service;

import com.dayan.order.entity.OrderStatusChangeRecord;
import com.dayan.order.mapper.OrderStatusChangeRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单状态变更记录写入辅助组件（跨 4 类订单共享）。
 *
 * <p>每次状态机 transition 成功后调用 {@link #writeRecord} 写一条 order_status_change_record。
 * 默认操作人补齐：operatorCode 为空→"system"；operatorType 为空→"system"。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusChangeRecordHelper {

    private final OrderStatusChangeRecordMapper changeRecordMapper;

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
    public void writeRecord(int orderType, String orderCode,
                         Integer fromStatus, Integer toStatus,
                         String changeReason,
                         String operatorCode, String operatorName, String operatorType,
                         String remark) {
        OrderStatusChangeRecord record = new OrderStatusChangeRecord();
        record.setOrderType(orderType);
        record.setOrderCode(orderCode);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setChangeReason(changeReason);
        record.setOperatorCode(operatorCode == null || operatorCode.isBlank() ? "system" : operatorCode);
        record.setOperatorName(operatorName);
        record.setOperatorType(operatorType == null || operatorType.isBlank() ? "system" : operatorType);
        record.setOperateTime(LocalDateTime.now());
        record.setRemark(remark);
        changeRecordMapper.insert(record);
    }
}
