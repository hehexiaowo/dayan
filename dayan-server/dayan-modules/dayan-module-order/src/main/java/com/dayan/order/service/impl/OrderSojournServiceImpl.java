package com.dayan.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.statemachine.StateMachineEngine;
import com.dayan.order.dto.CreateOrderSojournDTO;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.dto.OrderCompleteDTO;
import com.dayan.order.dto.OrderSojournQueryDTO;
import com.dayan.order.dto.PayCallbackDTO;
import com.dayan.order.dto.RefundApplyDTO;
import com.dayan.order.entity.OrderSojourn;
import com.dayan.order.enums.OrderEvent;
import com.dayan.order.enums.OrderType;
import com.dayan.order.mapper.OrderSojournMapper;
import com.dayan.order.service.OrderSojournService;
import com.dayan.order.service.OrderStatusLogHelper;
import com.dayan.order.vo.OrderSojournVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 旅居预订订单（order_sojourn）服务实现 —— 核心链路。
 *
 * <p>核心链路（所有写操作 {@code @Transactional}，状态变更均经 ORDER_SM 状态机）：
 * <ul>
 *   <li>{@link #create}         生成订单号 + 校验金额(room+care+food+other) + 自动计算 stayDays + 置 0(待支付)</li>
 *   <li>{@link #payCallback}    0→1(已支付) + 写 payTime/payTradeNo/payType</li>
 *   <li>{@link #complete}       3→4(已完成，离店)</li>
 *   <li>{@link #applyRefund}    1|2|3→6(退款中)</li>
 *   <li>{@link #cancel}         0→5 或 6→5 + cancelReason</li>
 * </ul>
 *
 * <p>每次 transition 成功后经 {@link OrderStatusLogHelper} 写一条 system_order_status_log。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSojournServiceImpl implements OrderSojournService {

    // ====== 编码常量 ======
    private static final String OD_PREFIX = "OD";
    private static final String OD_SEQ_KEY = "code:seq:OD:0";
    private static final int OD_SEQ_WIDTH = 4;
    private static final DateTimeFormatter BASIC_ISO_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final OrderSojournMapper orderSojournMapper;
    private final StateMachineEngine stateMachineEngine;
    private final SequenceProvider sequenceProvider;
    private final OrderStatusLogHelper statusLogHelper;

    // ====== 查询 ======

    @Override
    public PageResult<OrderSojournVO> page(OrderSojournQueryDTO query) {
        LambdaQueryWrapper<OrderSojourn> wrapper = buildQueryWrapper(query);
        Page<OrderSojourn> page = orderSojournMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<OrderSojournVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<OrderSojournVO> list(OrderSojournQueryDTO query) {
        return orderSojournMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public OrderSojournVO getDetail(String orderCode) {
        return toVO(requireOrder(orderCode));
    }

    @Override
    public OrderSojourn requireOrder(String orderCode) {
        OrderSojourn order = orderSojournMapper.selectOne(new LambdaQueryWrapper<OrderSojourn>()
                .eq(OrderSojourn::getOrderCode, orderCode)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "旅居订单不存在: " + orderCode);
        }
        return order;
    }

    // ====== 核心链路：create ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CreateOrderSojournDTO dto) {
        // 金额校验：total = room + care + food + other；pay = total - discount
        BigDecimal room = dto.getRoomFee() == null ? BigDecimal.ZERO : dto.getRoomFee();
        BigDecimal care = dto.getCareFee() == null ? BigDecimal.ZERO : dto.getCareFee();
        BigDecimal food = dto.getFoodFee() == null ? BigDecimal.ZERO : dto.getFoodFee();
        BigDecimal other = dto.getOtherFee() == null ? BigDecimal.ZERO : dto.getOtherFee();
        BigDecimal totalAmount = room.add(care).add(food).add(other);
        BigDecimal discount = dto.getDiscountAmount() == null ? BigDecimal.ZERO : dto.getDiscountAmount();
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠金额不能为负");
        }
        if (discount.compareTo(totalAmount) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠金额不能超过订单总额");
        }
        BigDecimal payAmount = totalAmount.subtract(discount);

        // 自动计算 stayDays（若未传，由 checkin/checkout 计算）
        Integer stayDays = dto.getStayDays();
        if ((stayDays == null || stayDays <= 0)
                && dto.getCheckinDate() != null && dto.getCheckoutDate() != null) {
            long days = ChronoUnit.DAYS.between(dto.getCheckinDate(), dto.getCheckoutDate());
            stayDays = (int) Math.max(days, 0);
        }

        String orderCode = generateOrderCode();

        OrderSojourn entity = new OrderSojourn();
        entity.setOrderCode(orderCode);
        entity.setOrderType(dto.getOrderType() == null ? OrderType.SOJOURN : dto.getOrderType());
        entity.setChannelCode(dto.getChannelCode());
        entity.setChannelFullName(dto.getChannelFullName());
        entity.setAgentCode(dto.getAgentCode());
        entity.setAgentFullName(dto.getAgentFullName());
        entity.setDistributorCode(dto.getDistributorCode());
        entity.setDistributorFullName(dto.getDistributorFullName());
        entity.setClientCode(dto.getClientCode());
        entity.setClientFullName(dto.getClientFullName());
        entity.setGoodsCode(dto.getGoodsCode());
        entity.setGoodsName(dto.getGoodsName());
        entity.setParkCode(dto.getParkCode());
        entity.setParkFullName(dto.getParkFullName());
        entity.setRoomTypeCode(dto.getRoomTypeCode());
        entity.setSkuCode(dto.getSkuCode());
        entity.setSkuName(dto.getSkuName());
        entity.setCheckinDate(dto.getCheckinDate());
        entity.setCheckoutDate(dto.getCheckoutDate());
        entity.setStayDays(stayDays);
        entity.setResidentCount(dto.getResidentCount());
        entity.setResidentNames(dto.getResidentNames());
        entity.setCareTypeCode(dto.getCareTypeCode());
        entity.setFoodTypeCode(dto.getFoodTypeCode());
        entity.setRoomFee(room);
        entity.setCareFee(care);
        entity.setFoodFee(food);
        entity.setOtherFee(other);
        entity.setTotalAmount(totalAmount);
        entity.setDiscountAmount(discount);
        entity.setPayAmount(payAmount);
        entity.setCouponCode(dto.getCouponCode());
        entity.setDepositAmount(dto.getDepositAmount());
        entity.setEquityCode(dto.getEquityCode());
        entity.setContactName(dto.getContactName());
        entity.setContactPhone(dto.getContactPhone());
        entity.setSpecialNeeds(dto.getSpecialNeeds());
        entity.setRemark(dto.getRemark());
        entity.setOrderStatus(OrderEvent.STATUS_PENDING_PAY);
        orderSojournMapper.insert(entity);

        statusLogHelper.writeLog(OrderType.SOJOURN, orderCode,
                OrderEvent.STATUS_PENDING_PAY, OrderEvent.STATUS_PENDING_PAY,
                "订单创建",
                dto.getOperatorCode(), dto.getOperatorName(), "admin",
                "旅居预订订单创建");

        log.info("旅居订单创建成功: orderCode={}, totalAmount={}, payAmount={}, stayDays={}",
                orderCode, totalAmount, payAmount, stayDays);
        return orderCode;
    }

    // ====== 核心链路：payCallback ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payCallback(PayCallbackDTO dto) {
        OrderSojourn order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.PAY);

        OrderSojourn update = new OrderSojourn();
        update.setId(order.getId());
        update.setOrderStatus(to);
        update.setPayTime(LocalDateTime.now());
        if (dto.getPayType() != null) {
            update.setPayType(dto.getPayType());
        }
        orderSojournMapper.updateById(update);

        statusLogHelper.writeLog(OrderType.SOJOURN, order.getOrderCode(),
                from, to, "支付成功",
                dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                "tradeNo=" + dto.getPayTradeNo());

        log.info("旅居订单支付成功: orderCode={}, {} --pay--> {}", order.getOrderCode(), from, to);
    }

    // ====== 核心链路：complete ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(OrderCompleteDTO dto) {
        OrderSojourn order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.COMPLETE);

        OrderSojourn update = new OrderSojourn();
        update.setId(order.getId());
        update.setOrderStatus(to);
        orderSojournMapper.updateById(update);

        statusLogHelper.writeLog(OrderType.SOJOURN, order.getOrderCode(),
                from, to, "业务完结（离店）",
                dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                "旅居订单完成");
        log.info("旅居订单完成: orderCode={}, {} --complete--> {}", order.getOrderCode(), from, to);
    }

    // ====== 核心链路：applyRefund ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(RefundApplyDTO dto) {
        OrderSojourn order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.REFUND_APPLY);

        OrderSojourn update = new OrderSojourn();
        update.setId(order.getId());
        update.setOrderStatus(to);
        orderSojournMapper.updateById(update);

        statusLogHelper.writeLog(OrderType.SOJOURN, order.getOrderCode(),
                from, to, "申请退款：" + dto.getRefundReason(),
                dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                "旅居订单申请退款");
        log.info("旅居订单申请退款: orderCode={}, {} --refund_apply--> {}", order.getOrderCode(), from, to);
    }

    // ====== 核心链路：cancel ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(OrderCancelDTO dto) {
        OrderSojourn order = requireOrder(dto.getOrderCode());
        int from = order.getOrderStatus() == null ? OrderEvent.STATUS_PENDING_PAY : order.getOrderStatus();
        int to = stateMachineEngine.transition(OrderEvent.DOMAIN, from, OrderEvent.CANCEL);

        OrderSojourn update = new OrderSojourn();
        update.setId(order.getId());
        update.setOrderStatus(to);
        update.setCancelReason(dto.getCancelReason());
        orderSojournMapper.updateById(update);

        statusLogHelper.writeLog(OrderType.SOJOURN, order.getOrderCode(),
                from, to, "取消订单：" + dto.getCancelReason(),
                dto.getOperatorCode(), dto.getOperatorName(), dto.getOperatorType(),
                "旅居订单取消");
        log.info("旅居订单取消: orderCode={}, {} --cancel--> {}", order.getOrderCode(), from, to);
    }

    // ====== 内部方法 ======

    private String generateOrderCode() {
        long seq = sequenceProvider.next(OD_SEQ_KEY);
        String date = LocalDate.now().format(BASIC_ISO_DATE);
        return OD_PREFIX + date + String.format("%0" + OD_SEQ_WIDTH + "d", seq);
    }

    private LambdaQueryWrapper<OrderSojourn> buildQueryWrapper(OrderSojournQueryDTO query) {
        LambdaQueryWrapper<OrderSojourn> wrapper = new LambdaQueryWrapper<OrderSojourn>()
                .orderByDesc(OrderSojourn::getCreatedAt);
        if (query.getOrderCode() != null && !query.getOrderCode().isEmpty()) {
            wrapper.eq(OrderSojourn::getOrderCode, query.getOrderCode());
        }
        if (query.getOrderType() != null) {
            wrapper.eq(OrderSojourn::getOrderType, query.getOrderType());
        }
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(OrderSojourn::getChannelCode, query.getChannelCode());
        }
        if (query.getAgentCode() != null && !query.getAgentCode().isEmpty()) {
            wrapper.eq(OrderSojourn::getAgentCode, query.getAgentCode());
        }
        if (query.getDistributorCode() != null && !query.getDistributorCode().isEmpty()) {
            wrapper.eq(OrderSojourn::getDistributorCode, query.getDistributorCode());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(OrderSojourn::getClientCode, query.getClientCode());
        }
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(OrderSojourn::getParkCode, query.getParkCode());
        }
        if (query.getRoomTypeCode() != null && !query.getRoomTypeCode().isEmpty()) {
            wrapper.eq(OrderSojourn::getRoomTypeCode, query.getRoomTypeCode());
        }
        if (query.getCouponCode() != null && !query.getCouponCode().isEmpty()) {
            wrapper.eq(OrderSojourn::getCouponCode, query.getCouponCode());
        }
        if (query.getEquityCode() != null && !query.getEquityCode().isEmpty()) {
            wrapper.eq(OrderSojourn::getEquityCode, query.getEquityCode());
        }
        if (query.getCheckinDateStart() != null) {
            wrapper.ge(OrderSojourn::getCheckinDate, query.getCheckinDateStart());
        }
        if (query.getCheckinDateEnd() != null) {
            wrapper.le(OrderSojourn::getCheckinDate, query.getCheckinDateEnd());
        }
        if (query.getOrderStatus() != null) {
            wrapper.eq(OrderSojourn::getOrderStatus, query.getOrderStatus());
        }
        if (query.getPayType() != null) {
            wrapper.eq(OrderSojourn::getPayType, query.getPayType());
        }
        return wrapper;
    }

    private OrderSojournVO toVO(OrderSojourn entity) {
        OrderSojournVO vo = new OrderSojournVO();
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
        vo.setGoodsName(entity.getGoodsName());
        vo.setParkCode(entity.getParkCode());
        vo.setParkFullName(entity.getParkFullName());
        vo.setRoomTypeCode(entity.getRoomTypeCode());
        vo.setSkuCode(entity.getSkuCode());
        vo.setSkuName(entity.getSkuName());
        vo.setCheckinDate(entity.getCheckinDate());
        vo.setCheckoutDate(entity.getCheckoutDate());
        vo.setStayDays(entity.getStayDays());
        vo.setResidentCount(entity.getResidentCount());
        vo.setResidentNames(entity.getResidentNames());
        vo.setCareTypeCode(entity.getCareTypeCode());
        vo.setFoodTypeCode(entity.getFoodTypeCode());
        vo.setRoomFee(entity.getRoomFee());
        vo.setCareFee(entity.getCareFee());
        vo.setFoodFee(entity.getFoodFee());
        vo.setOtherFee(entity.getOtherFee());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setDiscountAmount(entity.getDiscountAmount());
        vo.setPayAmount(entity.getPayAmount());
        vo.setCouponCode(entity.getCouponCode());
        vo.setPayType(entity.getPayType());
        vo.setPayTime(entity.getPayTime());
        vo.setDepositAmount(entity.getDepositAmount());
        vo.setEquityCode(entity.getEquityCode());
        vo.setContactName(entity.getContactName());
        vo.setContactPhone(entity.getContactPhone());
        vo.setSpecialNeeds(entity.getSpecialNeeds());
        vo.setOrderStatus(entity.getOrderStatus());
        vo.setCancelReason(entity.getCancelReason());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
