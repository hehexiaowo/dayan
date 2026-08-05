package com.dayan.order.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.order.dto.CreateOrderCourseDTO;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.dto.OrderCompleteDTO;
import com.dayan.order.dto.OrderCourseQueryDTO;
import com.dayan.order.dto.PayCallbackDTO;
import com.dayan.order.dto.RefundApplyDTO;
import com.dayan.order.service.OrderCourseService;
import com.dayan.order.vo.OrderCourseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端课程购买订单接口（核心链路）。
 *
 * <p>路径前缀 {@code /order/course}（context-path=/admin-api 拼接为 {@code /admin-api/order/course/*}）。
 * 所有状态变更经 ORDER_SM 状态机，并写 system_order_status_log。
 */
@Tag(name = "课程购买订单管理（核心链路）")
@Validated
@RestController
@RequestMapping("/order/course")
@RequiredArgsConstructor
public class OrderCourseAdminController {

    private final OrderCourseService orderCourseService;

    @Operation(summary = "课程订单分页列表")
    @SaCheckPermission("order:course:list")
    @GetMapping("/page")
    public R<PageResult<OrderCourseVO>> page(OrderCourseQueryDTO query) {
        return R.ok(orderCourseService.page(query));
    }

    @Operation(summary = "课程订单列表（全量）")
    @SaCheckPermission("order:course:list")
    @GetMapping("/list")
    public R<List<OrderCourseVO>> list(OrderCourseQueryDTO query) {
        return R.ok(orderCourseService.list(query));
    }

    @Operation(summary = "课程订单详情")
    @SaCheckPermission("order:course:query")
    @GetMapping("/{orderCode}")
    public R<OrderCourseVO> getDetail(@PathVariable @NotBlank String orderCode) {
        return R.ok(orderCourseService.getDetail(orderCode));
    }

    @Operation(summary = "创建课程订单（生成订单号 + 校验金额 + 置待支付）")
    @SaCheckPermission("order:course:create")
    @PostMapping("/create")
    public R<String> create(@RequestBody @Valid CreateOrderCourseDTO dto) {
        return R.ok(orderCourseService.create(dto));
    }

    @Operation(summary = "支付回调（0→1 已支付）")
    @SaCheckPermission("order:course:pay-callback")
    @PostMapping("/pay-callback")
    public R<Void> payCallback(@RequestBody @Valid PayCallbackDTO dto) {
        orderCourseService.payCallback(dto);
        return R.ok();
    }

    @Operation(summary = "完成订单（3→4 已完成）")
    @SaCheckPermission("order:course:complete")
    @PostMapping("/complete")
    public R<Void> complete(@RequestBody @Valid OrderCompleteDTO dto) {
        orderCourseService.complete(dto);
        return R.ok();
    }

    @Operation(summary = "申请退款（1/2/3→6 退款中）")
    @SaCheckPermission("order:course:apply-refund")
    @PostMapping("/apply-refund")
    public R<Void> applyRefund(@RequestBody @Valid RefundApplyDTO dto) {
        orderCourseService.applyRefund(dto);
        return R.ok();
    }

    @Operation(summary = "取消订单（0→5 或 6→5 已取消）")
    @SaCheckPermission("order:course:cancel")
    @PostMapping("/cancel")
    public R<Void> cancel(@RequestBody @Valid OrderCancelDTO dto) {
        orderCourseService.cancel(dto);
        return R.ok();
    }
}
