package com.dayan.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.statemachine.StateMachineEngine;
import com.dayan.order.dto.CreateOrderEquityDTO;
import com.dayan.order.dto.EquityDeliverDTO;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.dto.OrderCompleteDTO;
import com.dayan.order.dto.OrderEquityQueryDTO;
import com.dayan.order.dto.PayCallbackDTO;
import com.dayan.order.dto.RefundApplyDTO;
import com.dayan.order.entity.OrderEquity;
import com.dayan.order.enums.OrderEvent;
import com.dayan.order.enums.OrderType;
import com.dayan.order.mapper.OrderEquityMapper;
import com.dayan.order.service.OrderEquityService;
import com.dayan.order.service.OrderStatusChangeRecordHelper;
import com.dayan.order.vo.OrderEquityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 权益采购订单（order_equity）服务实现 —— 核心链路。
 *
 * <p>核心链路（所有写操作 {@code @Transactional}，状态变更均经 ORDER_SM 状态机）：
 * <ul>
 *   <li>{@link #create}         生成订单号 + 校验金额 + 置 0(待支付) + expire(30min)</li>
 *   <li>{@link #payCallback}    0→1(已支付) + 写 payTime/payTradeNo/payType</li>
 *   <li>{@link #deliver}        部分发放 1→2 / 全部发放 1|2→3 + 维护 deliverCount/deliverTime</li>
 *   <li>{@link #complete}       3→4(已完成)</li>
 *   <li>{@link #applyRefund}    1|2|3→6(退款中)</li>
 *   <li>{@link #cancel}         0→5 或 6→5 + cancelReason</li>
 * </ul>
 *
 * <p>每次 transition 成功后经 {@link OrderStatusChangeRecordHelper} 写一条 order_status_change_record。
 * 本期不强校验 equity 域出库（跨域，仅维护订单侧状态）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEquityServiceImpl implements OrderEquityService {

    // ====== 编码常量 ======
    private static final String OD_PREFIX = "OD";
    private static final String OD_SEQ_KEY = "code:seq:OD:0";
    private static final int OD_SEQ_WIDTH = 4;
    private static final DateTimeFormatter BASIC_ISO_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    /** 权益订单默认过期时长（分钟） */
    private static final int DEFAULT_EXPIRE_MINUTES = 30;

    private final OrderEquityMapper orderEquityMapper;
    private final StateMachineEngine stateMachineEngine;
    private final SequenceProvider sequenceProvider;
    private final OrderStatusChangeRecordHelper changeRecordHelper;

    // ====== 查询 ======

    @Override
    public PageResult<OrderEquityVO> page(OrderEquityQueryDTO query) {
        LambdaQueryWrapper<OrderEquity> wrapper = buildQueryWrapper(query);
        Page<OrderEquity> page = orderEquityMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<OrderEquityVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<OrderEquityVO> list(OrderEquityQueryDTO query) {
        return orderEquityMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public OrderEquityVO getDetail(String orderCode) {
        return toVO(requireOrder(orderCode));
    }

    @Override
    public List<OrderEquityVO> listByOrderCodes(Collection<String> orderCodes) {
        if (orderCodes == null || orderCodes.isEmpty()) {
            return List.of();
        }
        return orderEquityMapper.selectList(new LambdaQueryWrapper<OrderEquity>()
                        .in(OrderEquity::getOrderCode, orderCodes))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public OrderEquity requireOrder(String orderCode) {
        OrderEquity order = orderEquityMapper.selectOne(new LambdaQueryWrapper<OrderEquity>()
                .eq(OrderEquity::getOrderCode, orderCode)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权益订单不存在: " + orderCode);
        }
        return order;
    }

    // ====== 核心链路：create ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CreateOrderEquityDTO dto) {
        // 金额校验：total = unit_price * quantity；pay = total - discount
        BigDecimal unitPrice = dto.getUnitPrice();
        int quantity = dto.getQuantity();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discount = dto.getDiscountAmount() == null ? BigDecimal.ZERO : dto.getDiscountAmount();
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠金额不能为负");
        }
        if (discount.compareTo(totalAmount) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠金额不能超过订单总额");
        }
        BigDecimal payAmount = totalAmount.subtract(discount);

        LocalDateTime now = LocalDateTime.now();
        String orderCode = generateOrderCode();

        OrderEquity entity = new OrderEquity();
        entity.setOrderCode(orderCode);
        entity.setOrderSource(dto.getOrderSource());
        entity.setChannelCode(dto.getChannelCode());
        entity.setChannelFullName(dto.getChannelFullName());
        entity.setAgentCode(dto.getAgentCode());
        entity.setAgentFullName(dto.getAgentFullName());
        entity.setDistributorCode(dto.getDistributorCode());
        entity.setDistributorFullName(dto.getDistributorFullName());
        entity.setGoodsCode(dto.getGoodsCode());
        entity.setGoodsName(dto.getGoodsName());
        // sku_code/sku_name 为 NOT NULL 列，DTO 未提供时用 goodsCode/goodsName 兜底
        entity.setSkuCode(dto.getSkuCode() != null && !dto.getSkuCode().isEmpty()
                ? dto.getSkuCode() : dto.getGoodsCode());
        entity.setSkuName(dto.getSkuName() != null && !dto.getSkuName().isEmpty()
                ? dto.getSkuName() : dto.getGoodsName());
        entity.setQuantity(quantity);
        entity.setUnitPrice(unitPrice);
        entity.setTotalAmount(totalAmount);
        entity.setDiscountAmount(discount);
        entity.setPayAmount(payAmount);
        entity.setDeliverType(dto.getDeliverType() == null ? 1 : dto.getDeliverType());
        entity.setDeliverCount(0);
        entity.setExpireTime(now.plusMinutes(DEFAULT_EXPIRE_MINUTES));
        entity.setOrganCode(dto.getOrganCode());
        entity.setOrderStatus(OrderEvent.STATUS_PENDING_PAY);
        entity.setRemark(dto.getRemark());
        orderEquityMapper.insert(entity);

        // 写初始状态日志（0→0，记录创建）
        String operatorType = dto.getOperatorType() != null && !dto.getOperatorType().isEmpty()
                ? dto.getOperatorType() : "admin";
        changeRecordHelper.writeRecord(OrderType.EQUITY, orderCode,
                OrderEvent.STATUS_PENDING_PAY, OrderEvent.STATUS_PENDING_PAY,
                "订单创建",
                dto.getOperatorCode(), dto.getOperatorName(), operatorType,
                "权益采购订单创建");

        log.info("权益订单创建成功: orderCode={}, totalAmount={}, payAmount={}", orderCode, totalAmount, payAmount);
        return orderCode;
    }

    // ====== 核心链路：payCallback ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payCallback(PayCallbackDTO dto) {
        OrderEquity order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.PAY);

        OrderEquity update = new OrderEquity();
        update.setId(order.getId());
        update.setOrderStatus(to);
        update.setPayTime(LocalDateTime.now());
        update.setPayTradeNo(dto.getPayTradeNo());
        if (dto.getPayType() != null) {
            update.setPayType(dto.getPayType());
        }
        orderEquityMapper.updateById(update);

        changeRecordHelper.writeRecord(OrderType.EQUITY, order.getOrderCode(),
                from, to, "支付成功",
                dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                "tradeNo=" + dto.getPayTradeNo());

        log.info("权益订单支付成功: orderCode={}, {} --pay--> {}", order.getOrderCode(), from, to);
    }

    // ====== 核心链路：deliver / partialDeliver（权益订单特有） ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliver(EquityDeliverDTO dto) {
        OrderEquity order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int quantity = order.getQuantity() == null ? 0 : order.getQuantity();
        int currentDeliver = order.getDeliverCount() == null ? 0 : order.getDeliverCount();
        int thisBatch = dto.getDeliverCount();

        boolean partial = Boolean.TRUE.equals(dto.getPartialDeliver());

        LocalDateTime now = LocalDateTime.now();
        OrderEquity update = new OrderEquity();
        update.setId(order.getId());

        int to;
        if (partial) {
            // 部分发放：1→2
            to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.PARTIAL_DELIVER);
            int newDeliver = currentDeliver + thisBatch;
            if (quantity > 0 && newDeliver > quantity) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "累计发放数量(" + newDeliver + ")超过订单数量(" + quantity + ")");
            }
            update.setDeliverCount(newDeliver);
            update.setOrderStatus(to);
            orderEquityMapper.updateById(update);

            changeRecordHelper.writeRecord(OrderType.EQUITY, order.getOrderCode(),
                    from, to, "部分发放",
                    dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                    "本次=" + thisBatch + "，累计=" + newDeliver);
            log.info("权益订单部分发放: orderCode={}, {} --partial_deliver--> {}, deliverCount={}",
                    order.getOrderCode(), from, to, newDeliver);
        } else {
            // 全部发放完成：1→3 或 2→3
            to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.DELIVER);
            update.setDeliverCount(quantity);
            update.setDeliverTime(now);
            update.setOrderStatus(to);
            orderEquityMapper.updateById(update);

            changeRecordHelper.writeRecord(OrderType.EQUITY, order.getOrderCode(),
                    from, to, "全部发放完成",
                    dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                    "deliverCount=" + quantity);
            log.info("权益订单全部发放: orderCode={}, {} --deliver--> {}", order.getOrderCode(), from, to);
        }
    }

    // ====== 核心链路：complete ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(OrderCompleteDTO dto) {
        OrderEquity order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.COMPLETE);

        OrderEquity update = new OrderEquity();
        update.setId(order.getId());
        update.setOrderStatus(to);
        orderEquityMapper.updateById(update);

        changeRecordHelper.writeRecord(OrderType.EQUITY, order.getOrderCode(),
                from, to, "业务完结",
                dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                "权益订单完成");
        log.info("权益订单完成: orderCode={}, {} --complete--> {}", order.getOrderCode(), from, to);
    }

    // ====== 核心链路：applyRefund ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(RefundApplyDTO dto) {
        OrderEquity order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.REFUND_APPLY);

        OrderEquity update = new OrderEquity();
        update.setId(order.getId());
        update.setOrderStatus(to);
        orderEquityMapper.updateById(update);

        changeRecordHelper.writeRecord(OrderType.EQUITY, order.getOrderCode(),
                from, to, "申请退款：" + dto.getRefundReason(),
                dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                "权益订单申请退款");
        log.info("权益订单申请退款: orderCode={}, {} --refund_apply--> {}", order.getOrderCode(), from, to);
    }

    // ====== 核心链路：cancel ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(OrderCancelDTO dto) {
        OrderEquity order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.CANCEL);

        OrderEquity update = new OrderEquity();
        update.setId(order.getId());
        update.setOrderStatus(to);
        update.setCancelReason(dto.getCancelReason());
        orderEquityMapper.updateById(update);

        changeRecordHelper.writeRecord(OrderType.EQUITY, order.getOrderCode(),
                from, to, "取消订单：" + dto.getCancelReason(),
                dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                "权益订单取消");
        log.info("权益订单取消: orderCode={}, {} --cancel--> {}", order.getOrderCode(), from, to);
    }

    // ====== 内部方法 ======

    private String generateOrderCode() {
        long seq = sequenceProvider.next(OD_SEQ_KEY);
        String date = LocalDate.now().format(BASIC_ISO_DATE);
        return OD_PREFIX + date + String.format("%0" + OD_SEQ_WIDTH + "d", seq);
    }

    private LambdaQueryWrapper<OrderEquity> buildQueryWrapper(OrderEquityQueryDTO query) {
        LambdaQueryWrapper<OrderEquity> wrapper = new LambdaQueryWrapper<OrderEquity>()
                .orderByDesc(OrderEquity::getCreatedAt);
        if (query.getOrderCode() != null && !query.getOrderCode().isEmpty()) {
            wrapper.eq(OrderEquity::getOrderCode, query.getOrderCode());
        }
        if (query.getOrderSource() != null) {
            wrapper.eq(OrderEquity::getOrderSource, query.getOrderSource());
        }
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(OrderEquity::getChannelCode, query.getChannelCode());
        }
        if (query.getAgentCode() != null && !query.getAgentCode().isEmpty()) {
            wrapper.eq(OrderEquity::getAgentCode, query.getAgentCode());
        }
        if (query.getDistributorCode() != null && !query.getDistributorCode().isEmpty()) {
            wrapper.eq(OrderEquity::getDistributorCode, query.getDistributorCode());
        }
        if (query.getGoodsCode() != null && !query.getGoodsCode().isEmpty()) {
            wrapper.eq(OrderEquity::getGoodsCode, query.getGoodsCode());
        }
        if (query.getSkuCode() != null && !query.getSkuCode().isEmpty()) {
            wrapper.eq(OrderEquity::getSkuCode, query.getSkuCode());
        }
        if (query.getOrganCode() != null && !query.getOrganCode().isEmpty()) {
            wrapper.eq(OrderEquity::getOrganCode, query.getOrganCode());
        }
        if (query.getOrderStatus() != null) {
            wrapper.eq(OrderEquity::getOrderStatus, query.getOrderStatus());
        }
        if (query.getPayType() != null) {
            wrapper.eq(OrderEquity::getPayType, query.getPayType());
        }
        return wrapper;
    }

    private OrderEquityVO toVO(OrderEquity entity) {
        OrderEquityVO vo = new OrderEquityVO();
        vo.setId(entity.getId());
        vo.setOrderCode(entity.getOrderCode());
        vo.setOrderSource(entity.getOrderSource());
        vo.setChannelCode(entity.getChannelCode());
        vo.setChannelFullName(entity.getChannelFullName());
        vo.setAgentCode(entity.getAgentCode());
        vo.setAgentFullName(entity.getAgentFullName());
        vo.setDistributorCode(entity.getDistributorCode());
        vo.setDistributorFullName(entity.getDistributorFullName());
        vo.setGoodsCode(entity.getGoodsCode());
        vo.setGoodsName(entity.getGoodsName());
        vo.setSkuCode(entity.getSkuCode());
        vo.setSkuName(entity.getSkuName());
        vo.setQuantity(entity.getQuantity());
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setDiscountAmount(entity.getDiscountAmount());
        vo.setPayAmount(entity.getPayAmount());
        vo.setPayType(entity.getPayType());
        vo.setPayTime(entity.getPayTime());
        vo.setPayTradeNo(entity.getPayTradeNo());
        vo.setDeliverType(entity.getDeliverType());
        vo.setDeliverCount(entity.getDeliverCount());
        vo.setDeliverTime(entity.getDeliverTime());
        vo.setExpireTime(entity.getExpireTime());
        vo.setInvoiceStatus(entity.getInvoiceStatus());
        vo.setOrganCode(entity.getOrganCode());
        vo.setOrderStatus(entity.getOrderStatus());
        vo.setCancelReason(entity.getCancelReason());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
