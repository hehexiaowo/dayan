package com.dayan.order.controller.channel;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.dto.OrderSceneQueryDTO;
import com.dayan.order.service.OrderSceneService;
import com.dayan.order.vo.OrderSceneVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Channel 渠道端场景订单接口（订单管理）。
 *
 * <p>路径：{@code /channel-api/order-scenes/*}（由 dayan-channel 启动模块的 context-path 拼接）。
 *
 * <p>防越权：所有涉及渠道的查询/操作均从 {@link ContextHolder} 强制注入 channelCode，
 * 不接受前端传入。order_* 表在租户忽略清单，渠道隔离全靠业务层手动注入。
 * 详情/取消接口做渠道归属二次校验（订单 channelCode 必须与当前渠道一致）。
 *
 * <p>本接口为纯读接口（列表 + 详情 + 取消），不做下单。
 */
@Tag(name = "Channel 场景订单")
@RestController
@RequestMapping("/order-scenes")
@RequiredArgsConstructor
public class ChannelOrderSceneController {

    private final OrderSceneService orderSceneService;

    @Operation(summary = "本渠道场景订单分页列表")
    @SaCheckPermission("channel:order:list")
    @GetMapping
    public R<PageResult<OrderSceneVO>> page(OrderSceneQueryDTO query) {
        // 强制注入渠道编码，防止跨渠道越权查询
        query.setChannelCode(ContextHolder.getChannelCode());
        return R.ok(orderSceneService.page(query));
    }

    @Operation(summary = "场景订单详情")
    @SaCheckPermission("channel:order:query")
    @GetMapping("/{orderCode}")
    public R<OrderSceneVO> getDetail(@PathVariable String orderCode) {
        OrderSceneVO vo = orderSceneService.getDetail(orderCode);
        // 渠道归属校验：订单 channelCode 必须与当前渠道一致
        if (vo == null || !ContextHolder.getChannelCode().equals(vo.getChannelCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在或无权访问");
        }
        return R.ok(vo);
    }

    @Operation(summary = "取消场景订单")
    @SaCheckPermission("channel:order:cancel")
    @PostMapping("/{orderCode}/cancel")
    public R<Void> cancel(@PathVariable String orderCode, @RequestBody @Valid OrderCancelDTO dto) {
        OrderSceneVO vo = orderSceneService.getDetail(orderCode);
        // 渠道归属校验：订单 channelCode 必须与当前渠道一致
        if (vo == null || !ContextHolder.getChannelCode().equals(vo.getChannelCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在或无权访问");
        }
        // 用 path 参数覆盖 body 中的 orderCode
        dto.setOrderCode(orderCode);
        orderSceneService.cancel(dto);
        return R.ok();
    }
}
