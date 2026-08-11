package com.dayan.order.controller.agent;

import com.dayan.channel.entity.ChannelConfigGoods;
import com.dayan.channel.service.ChannelConfigGoodsService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.goods.service.GoodsInfoService;
import com.dayan.goods.vo.GoodsInfoVO;
import com.dayan.order.dto.CreateOrderEquityDTO;
import com.dayan.order.service.OrderEquityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agent 代理人端权益订单接口（大雁商城下单）。
 *
 * <p>路径：{@code /agent-api/order-equities}（由 dayan-agent 启动模块 context-path 拼接）。
 *
 * <p>防越权：channelCode/agentCode 从 {@link ContextHolder} 强制注入，不接受前端传入。
 * 价格/商品名由服务端从商品目录权威解析，禁止客户端传价（防篡改）。
 * orderSource 固定为 2（个人采购）。
 */
@Tag(name = "Agent 权益订单")
@RestController
@RequestMapping("/order-equities")
@RequiredArgsConstructor
public class AgentOrderEquityController {

    private final OrderEquityService orderEquityService;
    private final GoodsInfoService goodsInfoService;
    private final ChannelConfigGoodsService channelConfigGoodsService;

    @Operation(summary = "代理人个人采购下单")
    @PostMapping
    public R<String> create(@RequestBody @Valid CreateOrderEquityDTO dto) {
        String channelCode = ContextHolder.getChannelCode();
        String agentCode = ContextHolder.getAccountCode();

        // 强制注入渠道 + 代理人信息
        dto.setChannelCode(channelCode);
        dto.setAgentCode(agentCode);
        dto.setOperatorCode(agentCode);
        dto.setOperatorType(ContextHolder.getAccountType());
        dto.setOrderSource(2); // 个人采购

        // 商品状态校验
        GoodsInfoVO goods = goodsInfoService.getDetail(dto.getGoodsCode());
        if (goods == null || !Integer.valueOf(1).equals(goods.getGoodsStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS, "商品不存在或已下架");
        }
        // 权益类型校验
        if (!Integer.valueOf(1).equals(goods.getGoodsType())) {
            throw new BusinessException(ErrorCode.BUSINESS, "该商品类型不支持权益订单采购");
        }
        // 白名单校验
        boolean inWhitelist = false;
        List<ChannelConfigGoods> configs = channelConfigGoodsService.listByChannel(channelCode);
        for (ChannelConfigGoods c : configs) {
            if (dto.getGoodsCode().equals(c.getGoodsCode())) {
                inWhitelist = true;
                break;
            }
        }
        if (!inWhitelist) {
            throw new BusinessException(ErrorCode.BUSINESS, "商品不在可购范围");
        }
        // 覆盖客户端传入的价格/名称（防篡改）
        BigDecimal unitPrice = goods.getSalePrice() != null ? goods.getSalePrice() : goods.getOriginalPrice();
        if (unitPrice == null) {
            throw new BusinessException(ErrorCode.BUSINESS, "商品未配置价格");
        }
        dto.setUnitPrice(unitPrice);
        dto.setGoodsName(goods.getGoodsName());

        return R.ok(orderEquityService.create(dto));
    }
}
