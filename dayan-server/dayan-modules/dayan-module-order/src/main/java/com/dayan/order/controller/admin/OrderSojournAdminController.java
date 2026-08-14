package com.dayan.order.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.goods.service.GoodsInfoService;
import com.dayan.goods.service.GoodsSojournService;
import com.dayan.goods.vo.GoodsInfoVO;
import com.dayan.goods.vo.GoodsSojournVO;
import com.dayan.order.dto.CreateOrderSojournDTO;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.dto.OrderCompleteDTO;
import com.dayan.order.dto.OrderSojournQueryDTO;
import com.dayan.order.dto.PayCallbackDTO;
import com.dayan.order.dto.RefundApplyDTO;
import com.dayan.order.service.OrderSojournService;
import com.dayan.order.vo.OrderSojournVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端旅居预订订单接口（核心链路）。
 *
 * <p>路径前缀 {@code /order/sojourn}（context-path=/admin-api 拼接为 {@code /admin-api/order/sojourn/*}）。
 * 所有状态变更经 ORDER_SM 状态机，并写 system_order_status_log。
 */
@Tag(name = "旅居预订订单管理（核心链路）")
@Validated
@RestController
@RequestMapping("/order/sojourn")
@RequiredArgsConstructor
public class OrderSojournAdminController {

    private final OrderSojournService orderSojournService;
    private final GoodsInfoService goodsInfoService;
    private final GoodsSojournService goodsSojournService;

    @Operation(summary = "旅居订单分页列表")
    @SaCheckPermission("order:sojourn:list")
    @GetMapping("/page")
    public R<PageResult<OrderSojournVO>> page(OrderSojournQueryDTO query) {
        return R.ok(orderSojournService.page(query));
    }

    @Operation(summary = "旅居订单列表（全量）")
    @SaCheckPermission("order:sojourn:list")
    @GetMapping("/list")
    public R<List<OrderSojournVO>> list(OrderSojournQueryDTO query) {
        return R.ok(orderSojournService.list(query));
    }

    @Operation(summary = "旅居订单详情")
    @SaCheckPermission("order:sojourn:query")
    @GetMapping("/{orderCode}")
    public R<OrderSojournVO> getDetail(@PathVariable @NotBlank String orderCode) {
        return R.ok(orderSojournService.getDetail(orderCode));
    }

    @Operation(summary = "创建旅居订单（生成订单号 + 校验金额 + 置待支付）")
    @OperationLog(module = "旅居订单", action = "新增")
    @SaCheckPermission("order:sojourn:create")
    @PostMapping("/create")
    public R<String> create(@RequestBody @Valid CreateOrderSojournDTO dto) {
        resolveGoodsSnapshot(dto);
        return R.ok(orderSojournService.create(dto));
    }

    /**
     * 按 goodsCode/skuCode 查商品目录，权威覆盖 DTO 中的商品名称/规格名称快照。
     * goodsCode/skuCode 为空时跳过（旅居订单可无商品目录关联）。
     */
    private void resolveGoodsSnapshot(CreateOrderSojournDTO dto) {
        if (dto.getGoodsCode() != null && !dto.getGoodsCode().isBlank()) {
            GoodsInfoVO goods = goodsInfoService.getDetail(dto.getGoodsCode());
            if (goods != null) {
                dto.setGoodsName(goods.getGoodsName());
            }
        }
        if (dto.getSkuCode() != null && !dto.getSkuCode().isBlank()) {
            GoodsSojournVO sku = goodsSojournService.getByCode(dto.getSkuCode());
            if (sku != null) {
                dto.setSkuName(sku.getSkuName());
            }
        }
    }

    @Operation(summary = "支付回调（0→1 已支付）")
    @OperationLog(module = "旅居订单", action = "支付")
    @SaCheckPermission("order:sojourn:pay-callback")
    @PostMapping("/pay-callback")
    public R<Void> payCallback(@RequestBody @Valid PayCallbackDTO dto) {
        orderSojournService.payCallback(dto);
        return R.ok();
    }

    @Operation(summary = "完成订单（3→4 已完成，离店）")
    @OperationLog(module = "旅居订单", action = "完成")
    @SaCheckPermission("order:sojourn:complete")
    @PostMapping("/complete")
    public R<Void> complete(@RequestBody @Valid OrderCompleteDTO dto) {
        orderSojournService.complete(dto);
        return R.ok();
    }

    @Operation(summary = "申请退款（1/2/3→6 退款中）")
    @OperationLog(module = "旅居订单", action = "申请退款")
    @SaCheckPermission("order:sojourn:apply-refund")
    @PostMapping("/apply-refund")
    public R<Void> applyRefund(@RequestBody @Valid RefundApplyDTO dto) {
        orderSojournService.applyRefund(dto);
        return R.ok();
    }

    @Operation(summary = "取消订单（0→5 或 6→5 已取消）")
    @OperationLog(module = "旅居订单", action = "取消")
    @SaCheckPermission("order:sojourn:cancel")
    @PostMapping("/cancel")
    public R<Void> cancel(@RequestBody @Valid OrderCancelDTO dto) {
        orderSojournService.cancel(dto);
        return R.ok();
    }
}
