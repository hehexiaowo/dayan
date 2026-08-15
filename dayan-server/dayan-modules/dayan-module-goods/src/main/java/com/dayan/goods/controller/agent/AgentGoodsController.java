package com.dayan.goods.controller.agent;

import com.dayan.channel.entity.ChannelConfigGoods;
import com.dayan.channel.service.ChannelConfigGoodsService;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.goods.dto.GoodsInfoQueryDTO;
import com.dayan.goods.entity.ServiceItem;
import com.dayan.goods.mapper.ServiceItemMapper;
import com.dayan.goods.model.NetworkScope;
import com.dayan.goods.model.RightsJson;
import com.dayan.goods.service.GoodsDisplayBlockService;
import com.dayan.goods.service.GoodsEquityService;
import com.dayan.goods.service.GoodsInfoService;
import com.dayan.goods.vo.AgentGoodsVO;
import com.dayan.goods.vo.GoodsDisplayBlockVO;
import com.dayan.goods.vo.GoodsEquityVO;
import com.dayan.goods.vo.GoodsInfoVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent 代理人端商品浏览接口（大雁商城）。
 *
 * <p>仅返回当前渠道白名单（{@code channel_config_goods}）内、已上架（上架态）
 * 且为权益类（{@code goodsType=1}）的商品；每个商品聚合权益配置
 * （构成/期限/共享/转让 + 服务项目的次数/入住权/折扣/随心住规则/网络范围），
 * 供代理人在商城列表与详情页向客户讲解权益内容。
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
    private final GoodsEquityService goodsEquityService;
    private final GoodsDisplayBlockService goodsDisplayBlockService;
    private final ChannelConfigGoodsService channelConfigGoodsService;
    private final ServiceItemMapper serviceItemMapper;

    @Operation(summary = "商城可购权益商品列表（含权益配置）")
    @GetMapping
    public R<List<AgentGoodsVO>> list(GoodsInfoQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        // 渠道白名单
        List<ChannelConfigGoods> configs = channelConfigGoodsService.listByChannel(channelCode);
        Set<String> whitelist = configs.stream()
                .map(ChannelConfigGoods::getGoodsCode)
                .collect(Collectors.toSet());
        // 白名单 + 已上架(goodsStatus=2，5态枚举中 1=待上架) + 权益类型 三重过滤，
        // 逐个聚合权益配置（商城商品量小，可接受）
        List<AgentGoodsVO> result = goodsInfoService.list(query).stream()
                .filter(vo -> whitelist.contains(vo.getGoodsCode()))
                .filter(vo -> Integer.valueOf(2).equals(vo.getGoodsStatus()))
                .filter(vo -> Integer.valueOf(1).equals(vo.getGoodsType()))
                .map(this::toAgentVO)
                .collect(Collectors.toList());
        return R.ok(result);
    }

    @Operation(summary = "商品详情（含权益配置）")
    @GetMapping("/{goodsCode}")
    public R<AgentGoodsVO> getDetail(@PathVariable String goodsCode) {
        String channelCode = ContextHolder.getChannelCode();
        boolean inWhitelist = channelConfigGoodsService.listByChannel(channelCode).stream()
                .anyMatch(c -> goodsCode.equals(c.getGoodsCode()));
        if (!inWhitelist) {
            throw new com.dayan.common.core.exception.BusinessException(
                    com.dayan.common.core.exception.ErrorCode.BUSINESS, "商品不在可购范围");
        }
        // 详情额外聚合展示板块（列表不携带，避免列表响应膨胀）
        AgentGoodsVO vo = toAgentVO(goodsInfoService.getDetail(goodsCode));
        vo.setDisplayBlocks(goodsDisplayBlockService.listByGoods(goodsCode));
        return R.ok(vo);
    }

    /** 基础 VO + 权益配置 → Agent 聚合 VO（未配置权益的商品 equity=null，前端按无权益内容展示） */
    private AgentGoodsVO toAgentVO(GoodsInfoVO goods) {
        AgentGoodsVO vo = new AgentGoodsVO();
        BeanUtils.copyProperties(goods, vo);
        try {
            GoodsEquityVO equity = goodsEquityService.getByGoodsCode(goods.getGoodsCode());
            fillEffectiveNetworkScope(equity);
            vo.setEquity(equity);
        } catch (Exception e) {
            // 无权益配置（404 抛业务异常）不阻断商品展示
            vo.setEquity(null);
        }
        return vo;
    }

    /**
     * 网络范围对齐为「生效范围」：rel 未收窄（null）时回填服务项目级范围
     * （如随心住在服务项目上配置的机构+房型），代理人端展示口径即客户实际可用范围。
     */
    private void fillEffectiveNetworkScope(GoodsEquityVO equity) {
        if (equity == null || equity.getServiceItems() == null || equity.getServiceItems().isEmpty()) {
            return;
        }
        Set<String> itemCodes = equity.getServiceItems().stream()
                .map(GoodsEquityVO.ServiceItemRelVO::getItemCode)
                .collect(Collectors.toSet());
        Map<String, ServiceItem> items = serviceItemMapper.selectList(
                        new LambdaQueryWrapper<ServiceItem>().in(ServiceItem::getItemCode, itemCodes))
                .stream().collect(Collectors.toMap(ServiceItem::getItemCode, i -> i));
        for (GoodsEquityVO.ServiceItemRelVO rel : equity.getServiceItems()) {
            if (rel.getNetworkScope() != null && rel.getNetworkScope().isCustom()) {
                continue; // 商品级已收窄，生效范围即商品级
            }
            ServiceItem item = items.get(rel.getItemCode());
            if (item != null) {
                NetworkScope effective = RightsJson.readNetwork(item.getServiceNetwork());
                rel.setNetworkScope(effective); // 项目级也未配置时为 null=业态全部
            }
        }
    }
}
