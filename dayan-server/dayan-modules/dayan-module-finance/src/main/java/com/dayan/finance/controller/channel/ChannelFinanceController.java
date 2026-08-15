package com.dayan.finance.controller.channel;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.finance.dto.CreatePaymentDTO;
import com.dayan.finance.dto.FinancePaymentQueryDTO;
import com.dayan.finance.dto.PaymentMarkSuccessDTO;
import com.dayan.finance.service.FinancePaymentService;
import com.dayan.finance.vo.FinancePaymentVO;
import com.dayan.order.dto.OrderCourseQueryDTO;
import com.dayan.order.dto.OrderEquityQueryDTO;
import com.dayan.order.dto.OrderSceneQueryDTO;
import com.dayan.order.dto.OrderSojournQueryDTO;
import com.dayan.order.service.OrderCourseService;
import com.dayan.order.service.OrderEquityService;
import com.dayan.order.service.OrderSceneService;
import com.dayan.order.service.OrderSojournService;
import com.dayan.order.vo.OrderCourseVO;
import com.dayan.order.vo.OrderEquityVO;
import com.dayan.order.vo.OrderSceneVO;
import com.dayan.order.vo.OrderSojournVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Channel 渠道端支付接口。
 *
 * <p>路径：{@code /channel-api/finance-payments/*}（由 dayan-channel 启动模块的 context-path 拼接）。
 *
 * <p>防越权：支付金额从订单权威解析（{@code order_equity.pay_amount}），禁止客户端传金额（防篡改）。
 *
 * <p>读接口（列表/详情）：finance_payment 表无 channel_code 字段，靠反查本渠道 4 类订单（权益/场景/课程/旅游短居）
 * 的 orderCode 集合做归属过滤。
 */
@Tag(name = "Channel 支付")
@RestController
@RequestMapping("/finance-payments")
@RequiredArgsConstructor
public class ChannelFinanceController {

    private final FinancePaymentService financePaymentService;
    private final OrderEquityService orderEquityService;
    private final OrderSceneService orderSceneService;
    private final OrderCourseService orderCourseService;
    private final OrderSojournService orderSojournService;

    @Operation(summary = "创建支付单")
    @PostMapping
    public R<String> create(@RequestBody @Valid CreatePaymentDTO dto) {
        String channelCode = ContextHolder.getChannelCode();
        // 全类型归属校验 + 权威金额覆盖（防越权 + 防篡改），与 getDetail 的 isOrderOwnedByChannel 同源
        Integer orderType = dto.getOrderType();
        String orderCode = dto.getOrderCode();
        if (orderType == null || orderCode == null || orderCode.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单类型/订单编码不能为空");
        }
        BigDecimal authoritativePayAmount = resolveOrderPayAmount(orderCode, orderType, channelCode);
        if (authoritativePayAmount == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在或无权访问");
        }
        // 覆盖客户端传入的金额（以订单表为准）
        dto.setPayAmount(authoritativePayAmount);
        return R.ok(financePaymentService.create(dto));
    }

    @Operation(summary = "标记支付成功")
    @PostMapping("/{paymentCode}/mark-success")
    public R<Void> markSuccess(@PathVariable String paymentCode,
                               @RequestBody @Valid PaymentMarkSuccessDTO dto) {
        // 用 path 参数覆盖 body 中的 paymentCode
        dto.setPaymentCode(paymentCode);
        financePaymentService.markSuccess(dto);
        return R.ok();
    }

    @Operation(summary = "本渠道支付单列表")
    @SaCheckPermission("channel:payment:list")
    @GetMapping
    public R<PageResult<FinancePaymentVO>> page(FinancePaymentQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        // finance_payment 无 channel_code，靠反查本渠道 4 类订单的 orderCode 过滤
        List<String> orderCodes = collectChannelOrderCodes(channelCode);
        if (orderCodes.isEmpty()) {
            // 本渠道无任何订单，返回空分页（避免 SQL in() 报错）
            return R.ok(new PageResult<>(query.getCurrent(), query.getSize(), 0L, Collections.emptyList()));
        }
        query.setOrderCodes(orderCodes);
        return R.ok(financePaymentService.page(query));
    }

    @Operation(summary = "支付单详情")
    @SaCheckPermission("channel:payment:query")
    @GetMapping("/{paymentCode}")
    public R<FinancePaymentVO> getDetail(@PathVariable String paymentCode) {
        FinancePaymentVO vo = financePaymentService.getDetail(paymentCode);
        if (vo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付单不存在");
        }
        // 校验该支付单关联的订单属于本渠道（按 orderType 直接分发，避免顺序探测）
        String channelCode = ContextHolder.getChannelCode();
        if (!isOrderOwnedByChannel(vo.getOrderCode(), vo.getOrderType(), channelCode)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付单不存在或无权访问");
        }
        return R.ok(vo);
    }

    // ====== 内部方法：订单归属反查 ======

    /**
     * 收集本渠道全部订单码（4 类订单），用于支付单/其他无 channel_code 表的反查过滤。
     *
     * <p>对 4 类订单各查一次（按 channelCode 过滤），合并 orderCode 集合。
     */
    private List<String> collectChannelOrderCodes(String channelCode) {
        List<String> codes = new ArrayList<>();

        OrderEquityQueryDTO eq = new OrderEquityQueryDTO();
        eq.setChannelCode(channelCode);
        for (OrderEquityVO o : orderEquityService.list(eq)) {
            if (o.getOrderCode() != null) codes.add(o.getOrderCode());
        }

        OrderSceneQueryDTO sq = new OrderSceneQueryDTO();
        sq.setChannelCode(channelCode);
        for (OrderSceneVO o : orderSceneService.list(sq)) {
            if (o.getOrderCode() != null) codes.add(o.getOrderCode());
        }

        OrderCourseQueryDTO cq = new OrderCourseQueryDTO();
        cq.setChannelCode(channelCode);
        for (OrderCourseVO o : orderCourseService.list(cq)) {
            if (o.getOrderCode() != null) codes.add(o.getOrderCode());
        }

        OrderSojournQueryDTO jq = new OrderSojournQueryDTO();
        jq.setChannelCode(channelCode);
        for (OrderSojournVO o : orderSojournService.list(jq)) {
            if (o.getOrderCode() != null) codes.add(o.getOrderCode());
        }

        return codes;
    }

    /**
     * 校验订单是否属于本渠道（按 orderType 直接分发查询）。
     *
     * <p>注意：{@code OrderXxxService.getDetail} 在订单不存在时<b>抛 BusinessException 而非返回 null</b>
     * （见 OrderEquityServiceImpl.requireOrder），故采用支付单自身携带的 orderType 直接路由到
     * 对应类型的订单 Service，不做顺序探测——避免误命中类型的"订单不存在"异常中断流程。
     *
     * @param orderCode   支付单关联的订单编码
     * @param orderType   订单类型：1=权益/2=场景/3=课程/4=旅游短居
     * @param channelCode 当前渠道编码
     * @return true=属于本渠道；false=不属于或 orderType 非法
     */
    private boolean isOrderOwnedByChannel(String orderCode, Integer orderType, String channelCode) {
        if (orderCode == null || orderCode.isEmpty() || orderType == null || channelCode == null) {
            return false;
        }
        try {
            switch (orderType) {
                case 1: { // 权益
                    OrderEquityVO eq = orderEquityService.getDetail(orderCode);
                    return eq != null && channelCode.equals(eq.getChannelCode());
                }
                case 2: { // 场景
                    OrderSceneVO sq = orderSceneService.getDetail(orderCode);
                    return sq != null && channelCode.equals(sq.getChannelCode());
                }
                case 3: { // 课程
                    OrderCourseVO cq = orderCourseService.getDetail(orderCode);
                    return cq != null && channelCode.equals(cq.getChannelCode());
                }
                case 4: { // 旅游短居
                    OrderSojournVO jq = orderSojournService.getDetail(orderCode);
                    return jq != null && channelCode.equals(jq.getChannelCode());
                }
                default:
                    return false;
            }
        } catch (BusinessException e) {
            // 订单不存在（requireOrder 抛 NOT_FOUND），视为不属于本渠道
            return false;
        }
    }

    /**
     * 按订单类型解析订单的权威实付金额，并校验订单属于本渠道。
     *
     * <p>用于创建支付单时的防越权（订单必须属于本渠道）+ 防篡改（金额以订单表为准，忽略前端传入）。
     * 与 {@link #isOrderOwnedByChannel} 同源，但返回金额而非 boolean，供 create 覆盖 dto.payAmount。
     * 两者结构相似但职责不同（getDetail 不需要金额，create 需要金额），保留并存优于过度抽象。
     *
     * @param orderCode   订单编码
     * @param orderType   订单类型：1=权益/2=场景/3=课程/4=旅游短居
     * @param channelCode 当前渠道编码
     * @return 订单实付金额（订单存在且属于本渠道）；null=订单不存在/不属于本渠道/orderType 非法
     */
    private BigDecimal resolveOrderPayAmount(String orderCode, Integer orderType, String channelCode) {
        if (orderCode == null || orderCode.isEmpty() || orderType == null || channelCode == null) {
            return null;
        }
        try {
            switch (orderType) {
                case 1: { // 权益
                    OrderEquityVO o = orderEquityService.getDetail(orderCode);
                    return (o != null && channelCode.equals(o.getChannelCode())) ? o.getPayAmount() : null;
                }
                case 2: { // 场景
                    OrderSceneVO o = orderSceneService.getDetail(orderCode);
                    return (o != null && channelCode.equals(o.getChannelCode())) ? o.getPayAmount() : null;
                }
                case 3: { // 课程
                    OrderCourseVO o = orderCourseService.getDetail(orderCode);
                    return (o != null && channelCode.equals(o.getChannelCode())) ? o.getPayAmount() : null;
                }
                case 4: { // 旅游短居
                    OrderSojournVO o = orderSojournService.getDetail(orderCode);
                    return (o != null && channelCode.equals(o.getChannelCode())) ? o.getPayAmount() : null;
                }
                default:
                    return null;
            }
        } catch (BusinessException e) {
            // 订单不存在（requireOrder 抛 NOT_FOUND），视为不属于本渠道
            return null;
        }
    }
}
