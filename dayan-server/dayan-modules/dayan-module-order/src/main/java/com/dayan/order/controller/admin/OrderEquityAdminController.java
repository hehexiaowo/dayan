package com.dayan.order.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.order.dto.CreateOrderEquityDTO;
import com.dayan.order.dto.EquityDeliverDTO;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.dto.OrderCompleteDTO;
import com.dayan.order.dto.OrderEquityQueryDTO;
import com.dayan.order.dto.PayCallbackDTO;
import com.dayan.order.dto.RefundApplyDTO;
import com.dayan.order.service.OrderEquityService;
import com.dayan.order.vo.OrderEquityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端权益采购订单接口（核心链路）。
 *
 * <p>路径前缀 {@code /order/equity}（context-path=/admin-api 拼接为 {@code /admin-api/order/equity/*}）。
 * 所有状态变更经 ORDER_SM 状态机，并写 system_order_status_log。
 */
@Tag(name = "权益采购订单管理（核心链路）")
@Validated
@RestController
@RequestMapping("/order/equity")
@RequiredArgsConstructor
public class OrderEquityAdminController {

    private final OrderEquityService orderEquityService;

    @Operation(summary = "权益订单分页列表")
    @SaCheckPermission("order:equity:list")
    @GetMapping("/page")
    public R<PageResult<OrderEquityVO>> page(OrderEquityQueryDTO query) {
        return R.ok(orderEquityService.page(query));
    }

    @Operation(summary = "权益订单列表（全量）")
    @SaCheckPermission("order:equity:list")
    @GetMapping("/list")
    public R<List<OrderEquityVO>> list(OrderEquityQueryDTO query) {
        return R.ok(orderEquityService.list(query));
    }

    @Operation(summary = "权益订单详情")
    @SaCheckPermission("order:equity:query")
    @GetMapping("/{orderCode}")
    public R<OrderEquityVO> getDetail(@PathVariable @NotBlank String orderCode) {
        return R.ok(orderEquityService.getDetail(orderCode));
    }

    @Operation(summary = "创建权益订单（生成订单号 + 校验金额 + 置待支付）")
    @OperationLog(module = "权益订单", action = "新增")
    @SaCheckPermission("order:equity:create")
    @PostMapping("/create")
    public R<String> create(@RequestBody @Valid CreateOrderEquityDTO dto) {
        return R.ok(orderEquityService.create(dto));
    }

    @Operation(summary = "支付回调（0→1 已支付）")
    @OperationLog(module = "权益订单", action = "支付")
    @SaCheckPermission("order:equity:pay-callback")
    @PostMapping("/pay-callback")
    public R<Void> payCallback(@RequestBody @Valid PayCallbackDTO dto) {
        orderEquityService.payCallback(dto);
        return R.ok();
    }

    @Operation(summary = "权益发货（部分发放 1→2 / 全部发放 1|2→3）")
    @OperationLog(module = "权益订单", action = "发货")
    @SaCheckPermission("order:equity:deliver")
    @PostMapping("/deliver")
    public R<Void> deliver(@RequestBody @Valid EquityDeliverDTO dto) {
        orderEquityService.deliver(dto);
        return R.ok();
    }

    @Operation(summary = "完成订单（3→4 已完成）")
    @OperationLog(module = "权益订单", action = "完成")
    @SaCheckPermission("order:equity:complete")
    @PostMapping("/complete")
    public R<Void> complete(@RequestBody @Valid OrderCompleteDTO dto) {
        orderEquityService.complete(dto);
        return R.ok();
    }

    @Operation(summary = "申请退款（1/2/3→6 退款中）")
    @OperationLog(module = "权益订单", action = "申请退款")
    @SaCheckPermission("order:equity:apply-refund")
    @PostMapping("/apply-refund")
    public R<Void> applyRefund(@RequestBody @Valid RefundApplyDTO dto) {
        orderEquityService.applyRefund(dto);
        return R.ok();
    }

    @Operation(summary = "取消订单（0→5 或 6→5 已取消）")
    @OperationLog(module = "权益订单", action = "取消")
    @SaCheckPermission("order:equity:cancel")
    @PostMapping("/cancel")
    public R<Void> cancel(@RequestBody @Valid OrderCancelDTO dto) {
        orderEquityService.cancel(dto);
        return R.ok();
    }
}
