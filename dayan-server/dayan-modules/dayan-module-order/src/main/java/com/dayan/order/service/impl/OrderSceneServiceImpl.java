package com.dayan.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.statemachine.StateMachineEngine;
import com.dayan.order.dto.CreateOrderSceneDTO;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.dto.OrderCompleteDTO;
import com.dayan.order.dto.OrderSceneQueryDTO;
import com.dayan.order.dto.PayCallbackDTO;
import com.dayan.order.dto.RefundApplyDTO;
import com.dayan.order.entity.OrderScene;
import com.dayan.order.enums.OrderEvent;
import com.dayan.order.enums.OrderType;
import com.dayan.order.mapper.OrderSceneMapper;
import com.dayan.order.service.OrderSceneService;
import com.dayan.order.service.OrderStatusLogHelper;
import com.dayan.order.vo.OrderSceneVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 场景报名订单（order_scene）服务实现 —— 核心链路。
 *
 * <p>核心链路（所有写操作 {@code @Transactional}，状态变更均经 ORDER_SM 状态机）：
 * <ul>
 *   <li>{@link #create}         生成订单号 + 校验金额(支持权益/优惠券抵扣) + 置 0(待支付)</li>
 *   <li>{@link #payCallback}    0→1(已支付) + 写 payTime/payTradeNo/payType</li>
 *   <li>{@link #complete}       3→4(已完成)</li>
 *   <li>{@link #applyRefund}    1|2|3→6(退款中)</li>
 *   <li>{@link #cancel}         0→5 或 6→5 + cancelReason</li>
 * </ul>
 *
 * <p>每次 transition 成功后经 {@link OrderStatusLogHelper} 写一条 system_order_status_log。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSceneServiceImpl implements OrderSceneService {

    // ====== 编码常量 ======
    private static final String OD_PREFIX = "OD";
    private static final String OD_SEQ_KEY = "code:seq:OD:0";
    private static final int OD_SEQ_WIDTH = 4;
    private static final DateTimeFormatter BASIC_ISO_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final OrderSceneMapper orderSceneMapper;
    private final StateMachineEngine stateMachineEngine;
    private final SequenceProvider sequenceProvider;
    private final OrderStatusLogHelper statusLogHelper;

    // ====== 查询 ======

    @Override
    public PageResult<OrderSceneVO> page(OrderSceneQueryDTO query) {
        LambdaQueryWrapper<OrderScene> wrapper = buildQueryWrapper(query);
        Page<OrderScene> page = orderSceneMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<OrderSceneVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<OrderSceneVO> list(OrderSceneQueryDTO query) {
        return orderSceneMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public OrderSceneVO getDetail(String orderCode) {
        return toVO(requireOrder(orderCode));
    }

    @Override
    public OrderScene requireOrder(String orderCode) {
        OrderScene order = orderSceneMapper.selectOne(new LambdaQueryWrapper<OrderScene>()
                .eq(OrderScene::getOrderCode, orderCode)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场景订单不存在: " + orderCode);
        }
        return order;
    }

    // ====== 核心链路：create ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CreateOrderSceneDTO dto) {
        // 金额校验：total = unit_price * participant_count；pay = total - discount - coupon
        BigDecimal unitPrice = dto.getUnitPrice();
        int participantCount = dto.getParticipantCount();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(participantCount));
        BigDecimal discount = dto.getDiscountAmount() == null ? BigDecimal.ZERO : dto.getDiscountAmount();
        BigDecimal coupon = dto.getCouponAmount() == null ? BigDecimal.ZERO : dto.getCouponAmount();
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠金额不能为负");
        }
        if (coupon.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券抵扣金额不能为负");
        }
        BigDecimal totalDeduct = discount.add(coupon);
        if (totalDeduct.compareTo(totalAmount) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠+优惠券抵扣总额不能超过订单总额");
        }
        BigDecimal payAmount = totalAmount.subtract(totalDeduct);

        String orderCode = generateOrderCode();

        OrderScene entity = new OrderScene();
        entity.setOrderCode(orderCode);
        entity.setOrderType(OrderType.SCENE);
        entity.setChannelCode(dto.getChannelCode());
        entity.setChannelFullName(dto.getChannelFullName());
        entity.setAgentCode(dto.getAgentCode());
        entity.setAgentFullName(dto.getAgentFullName());
        entity.setDistributorCode(dto.getDistributorCode());
        entity.setDistributorFullName(dto.getDistributorFullName());
        entity.setClientCode(dto.getClientCode());
        entity.setClientFullName(dto.getClientFullName());
        entity.setGoodsCode(dto.getGoodsCode());
        entity.setSceneCode(dto.getSceneCode());
        entity.setSceneName(dto.getSceneName());
        // sku_code 为 NOT NULL 列，DTO 未提供时用 goodsCode 兜底
        entity.setSkuCode(dto.getSkuCode() != null && !dto.getSkuCode().isEmpty()
                ? dto.getSkuCode() : dto.getGoodsCode());
        entity.setScheduleCode(dto.getScheduleCode());
        entity.setActivityDate(dto.getActivityDate());
        entity.setParticipantCount(participantCount);
        entity.setParticipantNames(dto.getParticipantNames());
        entity.setUnitPrice(unitPrice);
        entity.setTotalAmount(totalAmount);
        entity.setDiscountAmount(discount);
        entity.setPayAmount(payAmount);
        entity.setCouponCode(dto.getCouponCode());
        entity.setCouponAmount(coupon);
        entity.setEquityCode(dto.getEquityCode());
        entity.setContactName(dto.getContactName());
        entity.setContactPhone(dto.getContactPhone());
        entity.setRemark(dto.getRemark());
        entity.setOrderStatus(OrderEvent.STATUS_PENDING_PAY);
        orderSceneMapper.insert(entity);

        statusLogHelper.writeLog(OrderType.SCENE, orderCode,
                OrderEvent.STATUS_PENDING_PAY, OrderEvent.STATUS_PENDING_PAY,
                "订单创建",
                dto.getOperatorCode(), dto.getOperatorName(), "admin",
                "场景报名订单创建");

        log.info("场景订单创建成功: orderCode={}, totalAmount={}, payAmount={}", orderCode, totalAmount, payAmount);
        return orderCode;
    }

    // ====== 核心链路：payCallback ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payCallback(PayCallbackDTO dto) {
        OrderScene order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.PAY);

        OrderScene update = new OrderScene();
        update.setId(order.getId());
        update.setOrderStatus(to);
        update.setPayTime(LocalDateTime.now());
        if (dto.getPayType() != null) {
            update.setPayType(dto.getPayType());
        }
        orderSceneMapper.updateById(update);

        statusLogHelper.writeLog(OrderType.SCENE, order.getOrderCode(),
                from, to, "支付成功",
                dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                "tradeNo=" + dto.getPayTradeNo());

        log.info("场景订单支付成功: orderCode={}, {} --pay--> {}", order.getOrderCode(), from, to);
    }

    // ====== 核心链路：complete ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(OrderCompleteDTO dto) {
        OrderScene order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.COMPLETE);

        OrderScene update = new OrderScene();
        update.setId(order.getId());
        update.setOrderStatus(to);
        orderSceneMapper.updateById(update);

        statusLogHelper.writeLog(OrderType.SCENE, order.getOrderCode(),
                from, to, "业务完结",
                dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                "场景订单完成");
        log.info("场景订单完成: orderCode={}, {} --complete--> {}", order.getOrderCode(), from, to);
    }

    // ====== 核心链路：applyRefund ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(RefundApplyDTO dto) {
        OrderScene order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.REFUND_APPLY);

        OrderScene update = new OrderScene();
        update.setId(order.getId());
        update.setOrderStatus(to);
        orderSceneMapper.updateById(update);

        statusLogHelper.writeLog(OrderType.SCENE, order.getOrderCode(),
                from, to, "申请退款：" + dto.getRefundReason(),
                dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                "场景订单申请退款");
        log.info("场景订单申请退款: orderCode={}, {} --refund_apply--> {}", order.getOrderCode(), from, to);
    }

    // ====== 核心链路：cancel ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(OrderCancelDTO dto) {
        OrderScene order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.CANCEL);

        OrderScene update = new OrderScene();
        update.setId(order.getId());
        update.setOrderStatus(to);
        update.setCancelReason(dto.getCancelReason());
        orderSceneMapper.updateById(update);

        statusLogHelper.writeLog(OrderType.SCENE, order.getOrderCode(),
                from, to, "取消订单：" + dto.getCancelReason(),
                dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                "场景订单取消");
        log.info("场景订单取消: orderCode={}, {} --cancel--> {}", order.getOrderCode(), from, to);
    }

    // ====== 内部方法 ======

    private String generateOrderCode() {
        long seq = sequenceProvider.next(OD_SEQ_KEY);
        String date = LocalDate.now().format(BASIC_ISO_DATE);
        return OD_PREFIX + date + String.format("%0" + OD_SEQ_WIDTH + "d", seq);
    }

    private LambdaQueryWrapper<OrderScene> buildQueryWrapper(OrderSceneQueryDTO query) {
        LambdaQueryWrapper<OrderScene> wrapper = new LambdaQueryWrapper<OrderScene>()
                .orderByDesc(OrderScene::getCreatedAt);
        if (query.getOrderCode() != null && !query.getOrderCode().isEmpty()) {
            wrapper.eq(OrderScene::getOrderCode, query.getOrderCode());
        }
        if (query.getOrderType() != null) {
            wrapper.eq(OrderScene::getOrderType, query.getOrderType());
        }
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(OrderScene::getChannelCode, query.getChannelCode());
        }
        if (query.getAgentCode() != null && !query.getAgentCode().isEmpty()) {
            wrapper.eq(OrderScene::getAgentCode, query.getAgentCode());
        }
        if (query.getDistributorCode() != null && !query.getDistributorCode().isEmpty()) {
            wrapper.eq(OrderScene::getDistributorCode, query.getDistributorCode());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(OrderScene::getClientCode, query.getClientCode());
        }
        if (query.getSceneCode() != null && !query.getSceneCode().isEmpty()) {
            wrapper.eq(OrderScene::getSceneCode, query.getSceneCode());
        }
        if (query.getScheduleCode() != null && !query.getScheduleCode().isEmpty()) {
            wrapper.eq(OrderScene::getScheduleCode, query.getScheduleCode());
        }
        if (query.getCouponCode() != null && !query.getCouponCode().isEmpty()) {
            wrapper.eq(OrderScene::getCouponCode, query.getCouponCode());
        }
        if (query.getEquityCode() != null && !query.getEquityCode().isEmpty()) {
            wrapper.eq(OrderScene::getEquityCode, query.getEquityCode());
        }
        if (query.getActivityDateStart() != null) {
            wrapper.ge(OrderScene::getActivityDate, query.getActivityDateStart());
        }
        if (query.getActivityDateEnd() != null) {
            wrapper.le(OrderScene::getActivityDate, query.getActivityDateEnd());
        }
        if (query.getOrderStatus() != null) {
            wrapper.eq(OrderScene::getOrderStatus, query.getOrderStatus());
        }
        if (query.getPayType() != null) {
            wrapper.eq(OrderScene::getPayType, query.getPayType());
        }
        return wrapper;
    }

    private OrderSceneVO toVO(OrderScene entity) {
        OrderSceneVO vo = new OrderSceneVO();
        vo.setId(entity.getId());
        vo.setOrderCode(entity.getOrderCode());
        vo.setOrderType(entity.getOrderType());
        vo.setChannelCode(entity.getChannelCode());
        vo.setChannelFullName(entity.getChannelFullName());
        vo.setAgentCode(entity.getAgentCode());
        vo.setAgentFullName(entity.getAgentFullName());
        vo.setDistributorCode(entity.getDistributorCode());
        vo.setDistributorFullName(entity.getDistributorFullName());
        vo.setClientCode(entity.getClientCode());
        vo.setClientFullName(entity.getClientFullName());
        vo.setGoodsCode(entity.getGoodsCode());
        vo.setSceneCode(entity.getSceneCode());
        vo.setSceneName(entity.getSceneName());
        vo.setSkuCode(entity.getSkuCode());
        vo.setScheduleCode(entity.getScheduleCode());
        vo.setActivityDate(entity.getActivityDate());
        vo.setParticipantCount(entity.getParticipantCount());
        vo.setParticipantNames(entity.getParticipantNames());
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setDiscountAmount(entity.getDiscountAmount());
        vo.setPayAmount(entity.getPayAmount());
        vo.setCouponCode(entity.getCouponCode());
        vo.setCouponAmount(entity.getCouponAmount());
        vo.setPayType(entity.getPayType());
        vo.setPayTime(entity.getPayTime());
        vo.setEquityCode(entity.getEquityCode());
        vo.setContactName(entity.getContactName());
        vo.setContactPhone(entity.getContactPhone());
        vo.setRemark(entity.getRemark());
        vo.setOrderStatus(entity.getOrderStatus());
        vo.setCancelReason(entity.getCancelReason());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
