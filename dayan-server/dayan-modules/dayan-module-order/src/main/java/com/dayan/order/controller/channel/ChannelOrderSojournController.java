package com.dayan.order.controller.channel;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.dto.OrderSojournQueryDTO;
import com.dayan.order.service.OrderSojournService;
import com.dayan.order.vo.OrderSojournVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Channel 渠道端旅游短居订单接口（订单管理）。
 *
 * <p>路径：{@code /channel-api/order-sojourns/*}（由 dayan-channel 启动模块的 context-path 拼接）。
 *
 * <p>防越权：所有涉及渠道的查询/操作均从 {@link ContextHolder} 强制注入 channelCode，
 * 不接受前端传入。order_* 表在租户忽略清单，渠道隔离全靠业务层手动注入。
 * 详情/取消接口做渠道归属二次校验（订单 channelCode 必须与当前渠道一致）。
 *
 * <p>本接口为纯读接口（列表 + 详情 + 取消），不做下单。
 */
@Tag(name = "Channel 旅游短居订单")
@RestController
@RequestMapping("/order-sojourns")
@RequiredArgsConstructor
public class ChannelOrderSojournController {

    private final OrderSojournService orderSojournService;

    @Operation(summary = "本渠道旅游短居订单分页列表")
    @SaCheckPermission("channel:order:list")
    @GetMapping
    public R<PageResult<OrderSojournVO>> page(OrderSojournQueryDTO query) {
        // 强制注入渠道编码，防止跨渠道越权查询
        query.setChannelCode(ContextHolder.getChannelCode());
        return R.ok(orderSojournService.page(query));
    }

    @Operation(summary = "旅游短居订单详情")
    @SaCheckPermission("channel:order:query")
    @GetMapping("/{orderCode}")
    public R<OrderSojournVO> getDetail(@PathVariable String orderCode) {
        OrderSojournVO vo = orderSojournService.getDetail(orderCode);
        // 渠道归属校验：订单 channelCode 必须与当前渠道一致
        if (vo == null || !ContextHolder.getChannelCode().equals(vo.getChannelCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在或无权访问");
        }
        return R.ok(vo);
    }

    @Operation(summary = "取消旅游短居订单")
    @SaCheckPermission("channel:order:cancel")
    @PostMapping("/{orderCode}/cancel")
    public R<Void> cancel(@PathVariable String orderCode, @RequestBody @Valid OrderCancelDTO dto) {
        OrderSojournVO vo = orderSojournService.getDetail(orderCode);
        // 渠道归属校验：订单 channelCode 必须与当前渠道一致
        if (vo == null || !ContextHolder.getChannelCode().equals(vo.getChannelCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在或无权访问");
        }
        // 用 path 参数覆盖 body 中的 orderCode
        dto.setOrderCode(orderCode);
        orderSojournService.cancel(dto);
        return R.ok();
    }
}
