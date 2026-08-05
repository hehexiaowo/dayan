package com.dayan.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.CreatePaymentDTO;
import com.dayan.finance.dto.FinancePaymentQueryDTO;
import com.dayan.finance.dto.PaymentMarkFailedDTO;
import com.dayan.finance.dto.PaymentMarkSuccessDTO;
import com.dayan.finance.dto.RecordFlowDTO;
import com.dayan.finance.entity.FinancePayment;
import com.dayan.finance.enums.FinanceEvent;
import com.dayan.finance.mapper.FinancePaymentMapper;
import com.dayan.finance.service.FinanceFlowService;
import com.dayan.finance.service.FinancePaymentService;
import com.dayan.finance.vo.FinancePaymentVO;
import com.dayan.order.dto.PayCallbackDTO;
import com.dayan.order.service.OrderCourseService;
import com.dayan.order.service.OrderEquityService;
import com.dayan.order.service.OrderSceneService;
import com.dayan.order.service.OrderSojournService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单支付记录（finance_payment）服务实现。
 *
 * <p>状态机：create(0 待支付) → markSuccess 0→1 / markFailed 0→2。
 * 编码：PAY + 10 位序号。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancePaymentServiceImpl implements FinancePaymentService {

    private final FinancePaymentMapper paymentMapper;
    private final SequenceProvider sequenceProvider;
    // 订单域 4 类 Service，用于支付成功后按 orderType 自动联动订单状态机
    private final OrderEquityService orderEquityService;
    private final OrderSceneService orderSceneService;
    private final OrderCourseService orderCourseService;
    private final OrderSojournService orderSojournService;
    // 财务流水 Service，用于支付成功后自动生成收入流水（G-1 修复）
    private final FinanceFlowService financeFlowService;

    // ====== 查询 ======

    @Override
    public PageResult<FinancePaymentVO> page(FinancePaymentQueryDTO query) {
        LambdaQueryWrapper<FinancePayment> wrapper = buildQueryWrapper(query);
        Page<FinancePayment> page = paymentMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<FinancePaymentVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<FinancePaymentVO> list(FinancePaymentQueryDTO query) {
        return paymentMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public FinancePaymentVO getDetail(String paymentCode) {
        return toVO(requirePayment(paymentCode));
    }

    // ====== 写入 ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CreatePaymentDTO dto) {
        if (dto.getPayAmount() == null || dto.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付金额必须 > 0");
        }

        FinancePayment entity = new FinancePayment();
        String paymentCode = generatePaymentCode();
        entity.setPaymentCode(paymentCode);
        entity.setOrderType(dto.getOrderType());
        entity.setOrderCode(dto.getOrderCode());
        entity.setPayType(dto.getPayType());
        entity.setPayAmount(dto.getPayAmount());
        entity.setPayerAccount(dto.getPayerAccount());
        entity.setPayeeAccount(dto.getPayeeAccount());
        entity.setPayStatus(FinanceEvent.PAY_STATUS_PENDING);
        entity.setPayDescription(dto.getPayDescription());
        entity.setExtraData(dto.getExtraData());
        paymentMapper.insert(entity);

        log.info("创建支付记录: paymentCode={}, orderCode={}, amount={}",
                paymentCode, dto.getOrderCode(), dto.getPayAmount());
        return paymentCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSuccess(PaymentMarkSuccessDTO dto) {
        FinancePayment payment = requirePayment(dto.getPaymentCode());
        int from = payment.getPayStatus() == null ? FinanceEvent.PAY_STATUS_PENDING : payment.getPayStatus();
        if (from != FinanceEvent.PAY_STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "支付记录非待支付状态，无法标记成功（当前状态=" + from + "）");
        }
        LocalDateTime now = LocalDateTime.now();
        FinancePayment update = new FinancePayment();
        update.setId(payment.getId());
        update.setPayStatus(FinanceEvent.PAY_STATUS_SUCCESS);
        update.setTradeNo(dto.getTradeNo());
        update.setPayTime(dto.getPayTime() != null ? dto.getPayTime() : now);
        update.setNotifyTime(dto.getNotifyTime() != null ? dto.getNotifyTime() : now);
        if (dto.getPayerAccount() != null && !dto.getPayerAccount().isEmpty()) {
            update.setPayerAccount(dto.getPayerAccount());
        }
        if (dto.getPayeeAccount() != null && !dto.getPayeeAccount().isEmpty()) {
            update.setPayeeAccount(dto.getPayeeAccount());
        }
        if (dto.getPayDescription() != null && !dto.getPayDescription().isEmpty()) {
            update.setPayDescription(dto.getPayDescription());
        }
        paymentMapper.updateById(update);
        log.info("支付成功: paymentCode={}, tradeNo={}", dto.getPaymentCode(), dto.getTradeNo());

        // 支付成功后自动联动订单状态机（0→1 已支付）
        // 按 payment.orderType 分发到对应 OrderXxxService.payCallback
        triggerOrderPayCallback(payment, dto);

        // 支付成功后自动生成财务收入流水（G-1 修复：跨域联动，try-catch 不阻断支付主流程）
        recordIncomeFlow(payment, dto);
    }

    /**
     * 支付成功后自动生成 type=1 收入流水。
     *
     * <p>跨域解耦：流水写入失败不影响支付主流程（支付记录已成功更新），仅记录警告。
     * 这与 {@link #triggerOrderPayCallback} 的容错策略一致。
     *
     * <p>accountCode 用 paymentCode 作为占位（FinancePayment 实体不含 channelCode），
     * 后续如需按渠道维度对账，可扩展 FinancePayment 增加 channelCode 冗余字段。
     *
     * @param payment 支付记录（含 orderCode/payAmount）
     * @param dto     标记成功的入参（含 tradeNo）
     */
    private void recordIncomeFlow(FinancePayment payment, PaymentMarkSuccessDTO dto) {
        try {
            RecordFlowDTO flowDTO = new RecordFlowDTO();
            flowDTO.setFlowType(1); // 1=收入
            flowDTO.setBizType(payment.getOrderType() == 1 ? "equity_order"
                    : payment.getOrderType() == 2 ? "scene_order"
                    : payment.getOrderType() == 3 ? "course_order"
                    : payment.getOrderType() == 4 ? "sojourn_order" : "order");
            flowDTO.setBizCode(payment.getOrderCode());
            flowDTO.setAccountType("payment");
            flowDTO.setAccountCode(payment.getPaymentCode());
            flowDTO.setFlowAmount(payment.getPayAmount());
            flowDTO.setPayType(payment.getPayType());
            flowDTO.setTradeNo(dto.getTradeNo());
            flowDTO.setFlowDescription("订单 " + payment.getOrderCode() + " 支付到账");
            flowDTO.setRemark("支付成功自动生成");
            financeFlowService.record(flowDTO);
            log.info("支付成功自动生成收入流水: paymentCode={}, orderCode={}, amount={}",
                    payment.getPaymentCode(), payment.getOrderCode(), payment.getPayAmount());
        } catch (Exception e) {
            log.warn("支付成功后生成收入流水失败（忽略）: paymentCode={}, err={}",
                    payment.getPaymentCode(), e.getMessage());
        }
    }

    /**
     * 支付成功后按 orderType 路由到对应订单 Service 的 payCallback。
     *
     * <p>幂等保护：订单状态机自身有状态守卫（只有 0→1 合法），若订单已经是已支付状态，
     * transition 会抛 BusinessException，此处捕获并记录警告日志，不中断支付流程
     * （支付记录本身已成功更新，不能因订单侧重复回调而回滚）。
     *
     * @param payment 支付记录（含 orderType/orderCode）
     * @param dto     标记成功的入参（含 tradeNo）
     */
    private void triggerOrderPayCallback(FinancePayment payment, PaymentMarkSuccessDTO dto) {
        PayCallbackDTO payCallback = new PayCallbackDTO();
        payCallback.setOrderCode(payment.getOrderCode());
        payCallback.setPayTradeNo(dto.getTradeNo());
        payCallback.setPayType(payment.getPayType());
        payCallback.setOperatorType("system");

        Integer orderType = payment.getOrderType();
        try {
            if (orderType == null) {
                log.warn("支付记录无 orderType，跳过订单联动: paymentCode={}", dto.getPaymentCode());
                return;
            }
            switch (orderType) {
                case 1: // 权益
                    orderEquityService.payCallback(payCallback);
                    break;
                case 2: // 场景
                    orderSceneService.payCallback(payCallback);
                    break;
                case 3: // 课程
                    orderCourseService.payCallback(payCallback);
                    break;
                case 4: // 旅居
                    orderSojournService.payCallback(payCallback);
                    break;
                default:
                    log.warn("未知 orderType={}，跳过订单联动: paymentCode={}", orderType, dto.getPaymentCode());
            }
        } catch (Exception e) {
            // 订单侧可能因重复回调（订单已是已支付状态）抛状态机异常，记录警告但不回滚支付
            log.warn("订单 payCallback 联动失败（可能重复回调）: orderCode={}, orderType={}, err={}",
                    payment.getOrderCode(), orderType, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(PaymentMarkFailedDTO dto) {
        FinancePayment payment = requirePayment(dto.getPaymentCode());
        int from = payment.getPayStatus() == null ? FinanceEvent.PAY_STATUS_PENDING : payment.getPayStatus();
        if (from != FinanceEvent.PAY_STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "支付记录非待支付状态，无法标记失败（当前状态=" + from + "）");
        }
        LocalDateTime now = LocalDateTime.now();
        FinancePayment update = new FinancePayment();
        update.setId(payment.getId());
        update.setPayStatus(FinanceEvent.PAY_STATUS_FAILED);
        update.setNotifyTime(dto.getNotifyTime() != null ? dto.getNotifyTime() : now);
        if (dto.getPayDescription() != null && !dto.getPayDescription().isEmpty()) {
            update.setPayDescription(dto.getPayDescription());
        }
        paymentMapper.updateById(update);
        log.info("支付失败: paymentCode={}", dto.getPaymentCode());
    }

    // ====== 内部方法 ======

    @Override
    public FinancePayment requirePayment(String paymentCode) {
        FinancePayment payment = paymentMapper.selectOne(new LambdaQueryWrapper<FinancePayment>()
                .eq(FinancePayment::getPaymentCode, paymentCode)
                .last("LIMIT 1"));
        if (payment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付记录不存在: " + paymentCode);
        }
        return payment;
    }

    private String generatePaymentCode() {
        long seq = sequenceProvider.next(FinanceEvent.PAYMENT_SEQ_KEY);
        return FinanceEvent.PAYMENT_PREFIX + String.format("%0" + FinanceEvent.PAYMENT_SEQ_WIDTH + "d", seq);
    }

    private LambdaQueryWrapper<FinancePayment> buildQueryWrapper(FinancePaymentQueryDTO query) {
        LambdaQueryWrapper<FinancePayment> wrapper = new LambdaQueryWrapper<FinancePayment>()
                .orderByDesc(FinancePayment::getCreatedAt);
        if (query.getPaymentCode() != null && !query.getPaymentCode().isEmpty()) {
            wrapper.eq(FinancePayment::getPaymentCode, query.getPaymentCode());
        }
        if (query.getOrderType() != null) {
            wrapper.eq(FinancePayment::getOrderType, query.getOrderType());
        }
        if (query.getOrderCode() != null && !query.getOrderCode().isEmpty()) {
            wrapper.eq(FinancePayment::getOrderCode, query.getOrderCode());
        }
        if (query.getTradeNo() != null && !query.getTradeNo().isEmpty()) {
            wrapper.eq(FinancePayment::getTradeNo, query.getTradeNo());
        }
        if (query.getPayType() != null) {
            wrapper.eq(FinancePayment::getPayType, query.getPayType());
        }
        if (query.getPayStatus() != null) {
            wrapper.eq(FinancePayment::getPayStatus, query.getPayStatus());
        }
        if (query.getPayTimeFrom() != null) {
            wrapper.ge(FinancePayment::getPayTime, query.getPayTimeFrom());
        }
        if (query.getPayTimeTo() != null) {
            wrapper.le(FinancePayment::getPayTime, query.getPayTimeTo());
        }
        return wrapper;
    }

    private FinancePaymentVO toVO(FinancePayment entity) {
        FinancePaymentVO vo = new FinancePaymentVO();
        vo.setId(entity.getId());
        vo.setPaymentCode(entity.getPaymentCode());
        vo.setOrderType(entity.getOrderType());
        vo.setOrderCode(entity.getOrderCode());
        vo.setPayType(entity.getPayType());
        vo.setPayAmount(entity.getPayAmount());
        vo.setTradeNo(entity.getTradeNo());
        vo.setPayerAccount(entity.getPayerAccount());
        vo.setPayeeAccount(entity.getPayeeAccount());
        vo.setPayTime(entity.getPayTime());
        vo.setNotifyTime(entity.getNotifyTime());
        vo.setPayStatus(entity.getPayStatus());
        vo.setPayDescription(entity.getPayDescription());
        vo.setExtraData(entity.getExtraData());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
