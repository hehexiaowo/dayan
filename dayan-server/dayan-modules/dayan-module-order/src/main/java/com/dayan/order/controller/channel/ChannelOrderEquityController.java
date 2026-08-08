package com.dayan.order.controller.channel;

import com.dayan.channel.entity.ChannelConfigGoods;
import com.dayan.channel.service.ChannelConfigGoodsService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.goods.service.GoodsInfoService;
import com.dayan.goods.service.GoodsSkuEquityService;
import com.dayan.goods.vo.GoodsInfoVO;
import com.dayan.goods.vo.GoodsSkuEquityVO;
import com.dayan.order.dto.CreateOrderEquityDTO;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.dto.OrderEquityQueryDTO;
import com.dayan.order.dto.PayCallbackDTO;
import com.dayan.order.service.OrderEquityService;
import com.dayan.order.vo.OrderEquityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Channel 渠道端权益订单接口。
 *
 * <p>路径：{@code /channel-api/order-equities/*}（由 dayan-channel 启动模块的 context-path 拼接）。
 *
 * <p>防越权：所有涉及渠道的查询/操作均从 {@link ContextHolder} 强制注入 channelCode，
 * 不接受前端传入。下单时商品名称/单价等服务端权威字段从商品目录解析，禁止客户端传价（防篡改）。
 */
@Tag(name = "Channel 权益订单")
@RestController
@RequestMapping("/order-equities")
@RequiredArgsConstructor
public class ChannelOrderEquityController {

    private final OrderEquityService orderEquityService;
    private final GoodsInfoService goodsInfoService;
    private final GoodsSkuEquityService goodsSkuEquityService;
    private final ChannelConfigGoodsService channelConfigGoodsService;

    @Operation(summary = "本渠道订单列表")
    @GetMapping
    public R<PageResult<OrderEquityVO>> page(OrderEquityQueryDTO query) {
        // 强制注入渠道编码，防止跨渠道越权查询
        query.setChannelCode(ContextHolder.getChannelCode());
        return R.ok(orderEquityService.page(query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderCode}")
    public R<OrderEquityVO> getDetail(@PathVariable String orderCode) {
        OrderEquityVO vo = orderEquityService.getDetail(orderCode);
        // 渠道归属校验：订单 channelCode 必须与当前渠道一致
        if (vo == null || !ContextHolder.getChannelCode().equals(vo.getChannelCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在或无权访问");
        }
        return R.ok(vo);
    }

    @Operation(summary = "渠道下单采购")
    @PostMapping
    public R<String> create(@RequestBody @Valid CreateOrderEquityDTO dto) {
        String channelCode = ContextHolder.getChannelCode();
        // 强制注入渠道信息（防越权）
        dto.setChannelCode(channelCode);
        dto.setOperatorCode(ContextHolder.getAccountCode());
        dto.setOperatorType(ContextHolder.getAccountType());

        // 价格/商品名由服务端从商品目录权威解析，禁止客户端传价（防篡改）
        GoodsInfoVO goods = goodsInfoService.getDetail(dto.getGoodsCode());
        if (goods == null || !Integer.valueOf(1).equals(goods.getGoodsStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS, "商品不存在或已下架");
        }
        // 白名单校验：商品必须在当前渠道可购范围
        boolean inWhitelist = false;
        List<ChannelConfigGoods> configs = channelConfigGoodsService.listByChannel(channelCode);
        for (ChannelConfigGoods c : configs) {
            if (dto.getGoodsCode().equals(c.getGoodsCode())) {
                inWhitelist = true;
                break;
            }
        }
        if (!inWhitelist) {
            throw new BusinessException(ErrorCode.BUSINESS, "商品不在本渠道可购范围");
        }
        // 覆盖客户端传入的价格/名称字段
        BigDecimal unitPrice = goods.getSalePrice() != null ? goods.getSalePrice() : goods.getOriginalPrice();
        if (unitPrice == null) {
            throw new BusinessException(ErrorCode.BUSINESS, "商品未配置价格");
        }
        dto.setUnitPrice(unitPrice);
        dto.setGoodsName(goods.getGoodsName());
        // 规格名称快照：按 skuCode 查目录权威覆盖（防篡改）
        if (dto.getSkuCode() != null && !dto.getSkuCode().isBlank()) {
            GoodsSkuEquityVO sku = goodsSkuEquityService.getByCode(dto.getSkuCode());
            if (sku != null) {
                dto.setSkuName(sku.getSkuName());
            }
        }
        // 权益订单 goodsType 必须为 1（权益类）
        if (!Integer.valueOf(1).equals(goods.getGoodsType())) {
            throw new BusinessException(ErrorCode.BUSINESS, "该商品类型不支持权益订单采购");
        }
        return R.ok(orderEquityService.create(dto));
    }

    @Operation(summary = "支付回调")
    @PostMapping("/{orderCode}/pay-callback")
    public R<Void> payCallback(@PathVariable String orderCode,
                               @RequestBody @Valid PayCallbackDTO dto) {
        // 用 path 参数覆盖 body 中的 orderCode
        dto.setOrderCode(orderCode);
        orderEquityService.payCallback(dto);
        return R.ok();
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{orderCode}/cancel")
    public R<Void> cancel(@PathVariable String orderCode,
                          @RequestBody OrderCancelDTO dto) {
        dto.setOrderCode(orderCode);
        orderEquityService.cancel(dto);
        return R.ok();
    }
}
