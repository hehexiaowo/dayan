package com.dayan.goods.controller.agent;

import com.dayan.channel.entity.ChannelConfigGoods;
import com.dayan.channel.service.ChannelConfigGoodsService;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.goods.dto.GoodsInfoQueryDTO;
import com.dayan.goods.service.GoodsInfoService;
import com.dayan.goods.vo.GoodsInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent 代理人端商品浏览接口（大雁商城）。
 *
 * <p>仅返回当前渠道白名单（{@code channel_config_goods}）内、已上架（{@code goodsStatus=1}）
 * 且为权益类（{@code goodsType=1}）的商品。
 *
 * <p>路径前缀 {@code /goods-infos}（由 dayan-agent 启动模块 context-path=/agent-api 拼接为
 * {@code /agent-api/goods-infos}）。
 */
@Tag(name = "Agent 商品浏览")
@RestController
@RequestMapping("/goods-infos")
@RequiredArgsConstructor
public class AgentGoodsController {

    private final GoodsInfoService goodsInfoService;
    private final ChannelConfigGoodsService channelConfigGoodsService;

    @Operation(summary = "商城可购权益商品列表")
    @GetMapping
    public R<List<GoodsInfoVO>> list(GoodsInfoQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        // 渠道白名单
        List<ChannelConfigGoods> configs = channelConfigGoodsService.listByChannel(channelCode);
        Set<String> whitelist = configs.stream()
                .map(ChannelConfigGoods::getGoodsCode)
                .collect(Collectors.toSet());
        // 白名单 + 上架 + 权益类型 三重过滤
        List<GoodsInfoVO> result = goodsInfoService.list(query).stream()
                .filter(vo -> whitelist.contains(vo.getGoodsCode()))
                .filter(vo -> Integer.valueOf(1).equals(vo.getGoodsStatus()))
                .filter(vo -> Integer.valueOf(1).equals(vo.getGoodsType()))
                .collect(Collectors.toList());
        return R.ok(result);
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{goodsCode}")
    public R<GoodsInfoVO> getDetail(@PathVariable String goodsCode) {
        String channelCode = ContextHolder.getChannelCode();
        boolean inWhitelist = channelConfigGoodsService.listByChannel(channelCode).stream()
                .anyMatch(c -> goodsCode.equals(c.getGoodsCode()));
        if (!inWhitelist) {
            throw new com.dayan.common.core.exception.BusinessException(
                    com.dayan.common.core.exception.ErrorCode.BUSINESS, "商品不在可购范围");
        }
        return R.ok(goodsInfoService.getDetail(goodsCode));
    }
}
