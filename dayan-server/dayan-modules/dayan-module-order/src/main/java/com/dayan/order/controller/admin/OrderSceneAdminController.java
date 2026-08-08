package com.dayan.order.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.goods.service.GoodsInfoService;
import com.dayan.goods.service.GoodsSkuSceneService;
import com.dayan.goods.vo.GoodsInfoVO;
import com.dayan.goods.vo.GoodsSkuSceneVO;
import com.dayan.order.dto.CreateOrderSceneDTO;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.dto.OrderCompleteDTO;
import com.dayan.order.dto.OrderSceneQueryDTO;
import com.dayan.order.dto.PayCallbackDTO;
import com.dayan.order.dto.RefundApplyDTO;
import com.dayan.order.dto.SceneDeliverDTO;
import com.dayan.order.service.OrderSceneService;
import com.dayan.order.vo.OrderSceneVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端场景报名订单接口（核心链路）。
 *
 * <p>路径前缀 {@code /order/scene}（context-path=/admin-api 拼接为 {@code /admin-api/order/scene/*}）。
 * 所有状态变更经 ORDER_SM 状态机，并写 system_order_status_log。
 */
@Tag(name = "场景报名订单管理（核心链路）")
@Validated
@RestController
@RequestMapping("/order/scene")
@RequiredArgsConstructor
public class OrderSceneAdminController {

    private final OrderSceneService orderSceneService;
    private final GoodsInfoService goodsInfoService;
    private final GoodsSkuSceneService goodsSkuSceneService;

    @Operation(summary = "场景订单分页列表")
    @SaCheckPermission("order:scene:list")
    @GetMapping("/page")
    public R<PageResult<OrderSceneVO>> page(OrderSceneQueryDTO query) {
        return R.ok(orderSceneService.page(query));
    }

    @Operation(summary = "场景订单列表（全量）")
    @SaCheckPermission("order:scene:list")
    @GetMapping("/list")
    public R<List<OrderSceneVO>> list(OrderSceneQueryDTO query) {
        return R.ok(orderSceneService.list(query));
    }

    @Operation(summary = "场景订单详情")
    @SaCheckPermission("order:scene:query")
    @GetMapping("/{orderCode}")
    public R<OrderSceneVO> getDetail(@PathVariable @NotBlank String orderCode) {
        return R.ok(orderSceneService.getDetail(orderCode));
    }

    @Operation(summary = "创建场景订单（生成订单号 + 校验金额 + 置待支付）")
    @SaCheckPermission("order:scene:create")
    @PostMapping("/create")
    public R<String> create(@RequestBody @Valid CreateOrderSceneDTO dto) {
        resolveGoodsSnapshot(dto);
        return R.ok(orderSceneService.create(dto));
    }

    /**
     * 按 goodsCode/skuCode 查商品目录，权威覆盖 DTO 中的商品名称/规格名称快照。
     * goodsCode/skuCode 为空时跳过（场景订单可无商品目录关联）。
     */
    private void resolveGoodsSnapshot(CreateOrderSceneDTO dto) {
        if (dto.getGoodsCode() != null && !dto.getGoodsCode().isBlank()) {
            GoodsInfoVO goods = goodsInfoService.getDetail(dto.getGoodsCode());
            if (goods != null) {
                dto.setGoodsName(goods.getGoodsName());
            }
        }
        if (dto.getSkuCode() != null && !dto.getSkuCode().isBlank()) {
            GoodsSkuSceneVO sku = goodsSkuSceneService.getByCode(dto.getSkuCode());
            if (sku != null) {
                dto.setSkuName(sku.getSkuName());
            }
        }
    }

    @Operation(summary = "支付回调（0→1 已支付）")
    @SaCheckPermission("order:scene:pay-callback")
    @PostMapping("/pay-callback")
    public R<Void> payCallback(@RequestBody @Valid PayCallbackDTO dto) {
        orderSceneService.payCallback(dto);
        return R.ok();
    }

    @Operation(summary = "核销/发货（1→3 到场核销）")
    @SaCheckPermission("order:scene:deliver")
    @PostMapping("/deliver")
    public R<Void> deliver(@RequestBody @Valid SceneDeliverDTO dto) {
        orderSceneService.deliver(dto);
        return R.ok();
    }

    @Operation(summary = "完成订单（3→4 已完成）")
    @SaCheckPermission("order:scene:complete")
    @PostMapping("/complete")
    public R<Void> complete(@RequestBody @Valid OrderCompleteDTO dto) {
        orderSceneService.complete(dto);
        return R.ok();
    }

    @Operation(summary = "申请退款（1/2/3→6 退款中）")
    @SaCheckPermission("order:scene:apply-refund")
    @PostMapping("/apply-refund")
    public R<Void> applyRefund(@RequestBody @Valid RefundApplyDTO dto) {
        orderSceneService.applyRefund(dto);
        return R.ok();
    }

    @Operation(summary = "取消订单（0→5 或 6→5 已取消）")
    @SaCheckPermission("order:scene:cancel")
    @PostMapping("/cancel")
    public R<Void> cancel(@RequestBody @Valid OrderCancelDTO dto) {
        orderSceneService.cancel(dto);
        return R.ok();
    }
}
