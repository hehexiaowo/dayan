package com.dayan.goods.controller.channel;

import com.dayan.channel.entity.ChannelConfigGoods;
import com.dayan.channel.service.ChannelConfigGoodsService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.goods.dto.GoodsInfoQueryDTO;
import com.dayan.goods.service.GoodsInfoService;
import com.dayan.goods.vo.GoodsInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Channel 渠道端商品浏览接口。
 *
 * <p>仅返回当前渠道白名单（{@code channel_config_goods}）内且已上架（{@code goodsStatus = 1}）的商品。
 * 渠道编码强制从 {@link ContextHolder} 获取，依赖 token 命名空间隔离，不做权限注解校验。
 *
 * <p>路径前缀 {@code /goods-infos}（由 dayan-channel 启动模块 context-path=/channel-api 拼接为
 * {@code /channel-api/goods-infos}）。
 */
@Tag(name = "Channel 商品浏览")
@RestController
@RequestMapping("/goods-infos")
@RequiredArgsConstructor
public class ChannelGoodsController {

    private final GoodsInfoService goodsInfoService;
    private final ChannelConfigGoodsService channelConfigGoodsService;

    @Operation(summary = "渠道可购商品列表")
    @GetMapping
    public R<List<GoodsInfoVO>> list(GoodsInfoQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        // 1. 渠道白名单配置
        List<ChannelConfigGoods> configs = channelConfigGoodsService.listByChannel(channelCode);
        Set<String> whitelist = configs.stream()
                .map(ChannelConfigGoods::getGoodsCode)
                .collect(Collectors.toSet());
        // 2. 全量商品按查询条件取出，再在内存中做白名单 + 上架状态过滤
        List<GoodsInfoVO> result = goodsInfoService.list(query).stream()
                .filter(vo -> whitelist.contains(vo.getGoodsCode()))
                .filter(vo -> Integer.valueOf(1).equals(vo.getGoodsStatus()))
                .collect(Collectors.toList());
        return R.ok(result);
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{goodsCode}")
    public R<GoodsInfoVO> getDetail(@PathVariable String goodsCode) {
        String channelCode = ContextHolder.getChannelCode();
        // 校验该商品是否在当前渠道白名单内，防止越权访问
        boolean inWhitelist = channelConfigGoodsService.listByChannel(channelCode).stream()
                .anyMatch(c -> goodsCode.equals(c.getGoodsCode()));
        if (!inWhitelist) {
            throw new BusinessException(ErrorCode.BUSINESS, "商品不在本渠道可购范围");
        }
        return R.ok(goodsInfoService.getDetail(goodsCode));
    }
}
