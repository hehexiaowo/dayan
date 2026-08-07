package com.dayan.finance.controller.channel;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.finance.dto.ApplyInvoiceDTO;
import com.dayan.finance.dto.FinanceInvoiceQueryDTO;
import com.dayan.finance.service.FinanceInvoiceService;
import com.dayan.finance.vo.FinanceInvoiceVO;
import com.dayan.order.service.OrderCourseService;
import com.dayan.order.service.OrderEquityService;
import com.dayan.order.service.OrderSceneService;
import com.dayan.order.service.OrderSojournService;
import com.dayan.order.vo.OrderCourseVO;
import com.dayan.order.vo.OrderEquityVO;
import com.dayan.order.vo.OrderSceneVO;
import com.dayan.order.vo.OrderSojournVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Channel 渠道端发票申请接口。
 *
 * <p>路径：{@code /channel-api/finance-invoices/*}（由 dayan-channel 启动模块的 context-path 拼接）。
 *
 * <p>读接口（列表/详情）：finance_invoice 表无 channel_code 字段，靠
 * {@code applicantCode}(=channelCode) + {@code applicantType}("channel") 做归属过滤。
 */
@Tag(name = "Channel 发票申请")
@RestController
@RequestMapping("/finance-invoices")
@RequiredArgsConstructor
public class ChannelInvoiceController {

    private final FinanceInvoiceService financeInvoiceService;
    private final OrderEquityService orderEquityService;
    private final OrderSceneService orderSceneService;
    private final OrderCourseService orderCourseService;
    private final OrderSojournService orderSojournService;

    @Operation(summary = "渠道申请发票")
    @PostMapping("/apply")
    public R<String> apply(@RequestBody @Valid ApplyInvoiceDTO dto) {
        String channelCode = ContextHolder.getChannelCode();
        // 强制注入申请方信息（防越权），覆盖任何前端传入
        dto.setApplicantCode(channelCode);
        dto.setApplicantType("channel");
        dto.setApplicantName(ContextHolder.getAccountName());
        // 发票金额对账：若关联订单，发票金额不得超过该订单已支付金额（防篡改/防超额开票）
        // 无订单开票场景（orderCode 为空，如结算单维度）本期不加金额上限（产品上金额由其他流程约束）
        if (dto.getOrderCode() != null && !dto.getOrderCode().isEmpty()) {
            BigDecimal orderPayAmount = resolveOrderPayAmountForInvoice(dto.getOrderCode(), channelCode);
            if (orderPayAmount == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "关联订单不存在或无权访问");
            }
            if (dto.getInvoiceAmount() != null
                    && dto.getInvoiceAmount().compareTo(orderPayAmount) > 0) {
                throw new BusinessException(ErrorCode.BUSINESS,
                        "发票金额不得超过订单实付金额（" + orderPayAmount + "）");
            }
        }
        return R.ok(financeInvoiceService.apply(dto));
    }

    @Operation(summary = "本渠道发票列表")
    @SaCheckPermission("channel:invoice:list")
    @GetMapping
    public R<PageResult<FinanceInvoiceVO>> page(FinanceInvoiceQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        // 强制注入申请方信息（防越权），覆盖任何前端传入
        query.setApplicantCode(channelCode);
        query.setApplicantType("channel");
        return R.ok(financeInvoiceService.page(query));
    }

    @Operation(summary = "发票详情")
    @SaCheckPermission("channel:invoice:query")
    @GetMapping("/{invoiceCode}")
    public R<FinanceInvoiceVO> getDetail(@PathVariable String invoiceCode) {
        FinanceInvoiceVO vo = financeInvoiceService.getDetail(invoiceCode);
        String channelCode = ContextHolder.getChannelCode();
        if (vo == null
                || !channelCode.equals(vo.getApplicantCode())
                || !"channel".equals(vo.getApplicantType())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "发票不存在或无权访问");
        }
        return R.ok(vo);
    }

    // ====== 内部方法：发票金额对账 ======

    /**
     * 按 orderCode 顺序探测 4 类订单，返回首个命中且属于本渠道的订单实付金额。
     *
     * <p>发票 DTO 不携带 orderType（与支付单不同），无法按类型直接路由，只能顺序探测。
     * {@code OrderXxxService.getDetail} 在订单不存在时<b>抛 BusinessException 而非返回 null</b>
     * （见 OrderEquityServiceImpl.requireOrder），故对每一类 try/catch 尝试。
     *
     * <p>探测顺序：权益→场景→课程→旅居。orderCode 在 4 类订单间理论上唯一（生成规则隔离），
     * 首个命中的即为目标；若全部不存在或存在但 channelCode 不匹配，返回 null（调用方按无权访问处理）。
     *
     * @param orderCode   关联订单编码
     * @param channelCode 当前渠道编码
     * @return 订单实付金额（订单存在且属于本渠道）；null=订单不存在/不属于本渠道
     */
    private BigDecimal resolveOrderPayAmountForInvoice(String orderCode, String channelCode) {
        if (orderCode == null || orderCode.isEmpty() || channelCode == null) {
            return null;
        }
        // 权益
        try {
            OrderEquityVO o = orderEquityService.getDetail(orderCode);
            if (o != null && channelCode.equals(o.getChannelCode())) {
                return o.getPayAmount();
            }
        } catch (BusinessException ignore) {
            // 该类型不存在，继续探测下一类型
        }
        // 场景
        try {
            OrderSceneVO o = orderSceneService.getDetail(orderCode);
            if (o != null && channelCode.equals(o.getChannelCode())) {
                return o.getPayAmount();
            }
        } catch (BusinessException ignore) {
        }
        // 课程
        try {
            OrderCourseVO o = orderCourseService.getDetail(orderCode);
            if (o != null && channelCode.equals(o.getChannelCode())) {
                return o.getPayAmount();
            }
        } catch (BusinessException ignore) {
        }
        // 旅居
        try {
            OrderSojournVO o = orderSojournService.getDetail(orderCode);
            if (o != null && channelCode.equals(o.getChannelCode())) {
                return o.getPayAmount();
            }
        } catch (BusinessException ignore) {
        }
        return null;
    }
}
