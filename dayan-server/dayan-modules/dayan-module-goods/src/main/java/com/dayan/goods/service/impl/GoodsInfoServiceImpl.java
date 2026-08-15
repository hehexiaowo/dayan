package com.dayan.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.BusinessCode;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsInfoCreateDTO;
import com.dayan.goods.dto.GoodsInfoQueryDTO;
import com.dayan.goods.dto.GoodsInfoShelfDTO;
import com.dayan.goods.dto.GoodsInfoUpdateDTO;
import com.dayan.goods.entity.GoodsInfo;
import com.dayan.goods.entity.GoodsCourse;
import com.dayan.goods.entity.GoodsEquity;
import com.dayan.goods.entity.GoodsScene;
import com.dayan.goods.entity.GoodsSojourn;
import com.dayan.goods.mapper.GoodsInfoMapper;
import com.dayan.goods.mapper.GoodsCourseMapper;
import com.dayan.goods.mapper.GoodsEquityMapper;
import com.dayan.goods.mapper.GoodsSceneMapper;
import com.dayan.goods.mapper.GoodsSojournMapper;
import com.dayan.goods.service.GoodsInfoService;
import com.dayan.goods.vo.GoodsInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品 SPU（goods_info）服务实现。
 *
 * <p>编码规则：{@code "GD" + format(%05d, sequenceProvider.next("code:seq:GD:0"))}。
 * 平台共享表（AUTO_INCREMENT + DayanTenantHandler 忽略），主键为 id，业务键为 goodsCode。
 *
 * <p>商品类型 goodsType（1权益/2场景/3课程/4旅游短居）变更需 4 张 SKU 子表全部为空
 * （类型决定关联子表）；删除时按类型校验对应 SKU 子表无关联记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsInfoServiceImpl implements GoodsInfoService {

    /** 商品编码前缀 */
    private static final String CODE_PREFIX = BusinessCode.GOODS;
    /** 序列键（全局共享计数，channelCode=0） */
    private static final String SEQ_KEY = "code:seq:" + CODE_PREFIX + ":0";

    /** 默认商品状态：0=草稿 */
    private static final int DEFAULT_GOODS_STATUS = 0;
    /** 默认审核状态：0=待审 */
    private static final int DEFAULT_AUDIT_STATUS = 0;
    /** 上架（DDL 5 态：2=已上架） */
    private static final int STATUS_ON_SHELF = 2;
    /** 下架（DDL 5 态：3=已下架） */
    private static final int STATUS_OFF_SHELF = 3;
    /** 售罄（DDL 5 态：4=已售罄） */
    private static final int STATUS_SOLD_OUT = 4;

    private final GoodsInfoMapper goodsInfoMapper;
    private final GoodsSceneMapper sceneMapper;
    private final GoodsCourseMapper courseMapper;
    private final GoodsSojournMapper sojournMapper;
    private final GoodsEquityMapper equityMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<GoodsInfoVO> page(GoodsInfoQueryDTO query) {
        LambdaQueryWrapper<GoodsInfo> wrapper = buildWrapper(query);
        Page<GoodsInfo> page = goodsInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<GoodsInfoVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<GoodsInfoVO> list(GoodsInfoQueryDTO query) {
        return goodsInfoMapper.selectList(buildWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public GoodsInfoVO getDetail(String goodsCode) {
        return toVO(requireGoods(goodsCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(GoodsInfoCreateDTO dto) {
        String goodsCode = nextGoodsCode();

        GoodsInfo entity = new GoodsInfo();
        entity.setGoodsCode(goodsCode);
        entity.setGoodsName(dto.getGoodsName());
        entity.setGoodsShortName(dto.getGoodsShortName());
        entity.setGoodsType(dto.getGoodsType());
        entity.setCategoryCode(dto.getCategoryCode());
        entity.setBrandName(dto.getBrandName());
        entity.setCoverImage(dto.getCoverImage());
        entity.setImageUrls(dto.getImageUrls());
        entity.setVideoUrl(dto.getVideoUrl());
        entity.setGoodsDescription(dto.getGoodsDescription());
        entity.setSummary(dto.getSummary());
        entity.setOriginalPrice(dto.getOriginalPrice());
        entity.setSalePrice(dto.getSalePrice());
        entity.setCostPrice(dto.getCostPrice());
        entity.setPriceUnit(dto.getPriceUnit());
        entity.setStock(dto.getStock());
        entity.setSalesCount(0);
        entity.setViewCount(0);
        entity.setCollectCount(0);
        entity.setSaleStartTime(dto.getSaleStartTime());
        entity.setSaleEndTime(dto.getSaleEndTime());
        entity.setIsHot(dto.getIsHot() == null ? 0 : dto.getIsHot());
        entity.setIsNew(dto.getIsNew() == null ? 0 : dto.getIsNew());
        entity.setIsRecommend(dto.getIsRecommend() == null ? 0 : dto.getIsRecommend());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setGoodsStatus(DEFAULT_GOODS_STATUS);
        entity.setAuditStatus(DEFAULT_AUDIT_STATUS);
        entity.setRemark(dto.getRemark());

        goodsInfoMapper.insert(entity);
        log.info("创建商品成功: goodsCode={}, goodsType={}", goodsCode, dto.getGoodsType());
        return goodsCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String goodsCode, GoodsInfoUpdateDTO dto) {
        GoodsInfo existing = requireGoods(goodsCode);
        GoodsInfo update = new GoodsInfo();
        update.setId(existing.getId());

        // 商品类型变更：类型决定关联的 SKU 子表，仅在 4 张子表全部为空时放行，
        // 已建任何 SKU 配置则拒绝（否则旧子表数据成为孤儿、详情页 tab 错乱）
        if (dto.getGoodsType() != null && !dto.getGoodsType().equals(existing.getGoodsType())) {
            if (dto.getGoodsType() < 1 || dto.getGoodsType() > 4) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "goodsType 仅支持 1权益/2场景/3课程/4旅游短居");
            }
            long skuCount = equityMapper.selectCount(new LambdaQueryWrapper<GoodsEquity>()
                            .eq(GoodsEquity::getGoodsCode, goodsCode))
                    + sceneMapper.selectCount(new LambdaQueryWrapper<GoodsScene>()
                            .eq(GoodsScene::getGoodsCode, goodsCode))
                    + courseMapper.selectCount(new LambdaQueryWrapper<GoodsCourse>()
                            .eq(GoodsCourse::getGoodsCode, goodsCode))
                    + sojournMapper.selectCount(new LambdaQueryWrapper<GoodsSojourn>()
                            .eq(GoodsSojourn::getGoodsCode, goodsCode));
            if (skuCount > 0) {
                throw new BusinessException(ErrorCode.BUSINESS,
                        "商品已存在 SKU 配置（共 " + skuCount + " 条），不能修改商品类型；请先清空对应配置");
            }
            update.setGoodsType(dto.getGoodsType());
            log.info("商品类型变更: goodsCode={}, {} -> {}", goodsCode, existing.getGoodsType(), dto.getGoodsType());
        }

        if (dto.getGoodsName() != null) update.setGoodsName(dto.getGoodsName());
        if (dto.getGoodsShortName() != null) update.setGoodsShortName(dto.getGoodsShortName());
        if (dto.getCategoryCode() != null) update.setCategoryCode(dto.getCategoryCode());
        if (dto.getBrandName() != null) update.setBrandName(dto.getBrandName());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getImageUrls() != null) update.setImageUrls(dto.getImageUrls());
        if (dto.getVideoUrl() != null) update.setVideoUrl(dto.getVideoUrl());
        if (dto.getGoodsDescription() != null) update.setGoodsDescription(dto.getGoodsDescription());
        if (dto.getSummary() != null) update.setSummary(dto.getSummary());
        if (dto.getOriginalPrice() != null) update.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getSalePrice() != null) update.setSalePrice(dto.getSalePrice());
        if (dto.getCostPrice() != null) update.setCostPrice(dto.getCostPrice());
        if (dto.getPriceUnit() != null) update.setPriceUnit(dto.getPriceUnit());
        if (dto.getStock() != null) update.setStock(dto.getStock());
        if (dto.getSaleStartTime() != null) update.setSaleStartTime(dto.getSaleStartTime());
        if (dto.getSaleEndTime() != null) update.setSaleEndTime(dto.getSaleEndTime());
        if (dto.getIsHot() != null) update.setIsHot(dto.getIsHot());
        if (dto.getIsNew() != null) update.setIsNew(dto.getIsNew());
        if (dto.getIsRecommend() != null) update.setIsRecommend(dto.getIsRecommend());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getAuditStatus() != null) update.setAuditStatus(dto.getAuditStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        goodsInfoMapper.updateById(update);
        log.info("更新商品成功: goodsCode={}", goodsCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shelf(GoodsInfoShelfDTO dto) {
        GoodsInfo existing = requireGoods(dto.getGoodsCode());
        Integer input = dto.getGoodsStatus();
        // 入参约定 0=下架 / 1=上架 / 4=售罄（保持前端兼容），映射到 DDL 5 态落库，避免与 goods_status 5 态语义错位
        if (input == null || (input != 0 && input != 1 && input != 4)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "goodsStatus 仅支持 0=下架 / 1=上架 / 4=售罄");
        }
        int target;
        if (input == 1) {
            target = STATUS_ON_SHELF;
        } else if (input == 4) {
            target = STATUS_SOLD_OUT;
        } else {
            target = STATUS_OFF_SHELF;
        }
        GoodsInfo update = new GoodsInfo();
        update.setId(existing.getId());
        update.setGoodsStatus(target);
        goodsInfoMapper.updateById(update);
        log.info("商品上下架成功: goodsCode={}, goodsStatus={}", dto.getGoodsCode(), target);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String goodsCode) {
        GoodsInfo existing = requireGoods(goodsCode);
        // 按商品类型校验对应 SKU 子表无关联记录
        Integer goodsType = existing.getGoodsType();
        if (goodsType != null) {
            switch (goodsType) {
                case 2 -> checkSkuEmpty(goodsCode, "场景", sceneMapper.selectCount(
                        new LambdaQueryWrapper<GoodsScene>().eq(GoodsScene::getGoodsCode, goodsCode)));
                case 3 -> checkSkuEmpty(goodsCode, "课程", courseMapper.selectCount(
                        new LambdaQueryWrapper<GoodsCourse>().eq(GoodsCourse::getGoodsCode, goodsCode)));
                case 4 -> checkSkuEmpty(goodsCode, "旅游短居", sojournMapper.selectCount(
                        new LambdaQueryWrapper<GoodsSojourn>().eq(GoodsSojourn::getGoodsCode, goodsCode)));
                default -> { /* 未知类型不阻塞删除 */ }
            }
        }
        goodsInfoMapper.delete(new LambdaQueryWrapper<GoodsInfo>()
                .eq(GoodsInfo::getGoodsCode, goodsCode));
        log.info("删除商品成功: goodsCode={}", goodsCode);
    }

    // ====== 内部方法 ======

    private void checkSkuEmpty(String goodsCode, String skuLabel, Long count) {
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "商品存在关联的" + skuLabel + " SKU，无法删除: " + goodsCode);
        }
    }

    private LambdaQueryWrapper<GoodsInfo> buildWrapper(GoodsInfoQueryDTO query) {
        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<GoodsInfo>()
                .orderByDesc(GoodsInfo::getId);
        if (query.getGoodsCode() != null && !query.getGoodsCode().isEmpty()) {
            wrapper.eq(GoodsInfo::getGoodsCode, query.getGoodsCode());
        }
        if (query.getGoodsName() != null && !query.getGoodsName().isEmpty()) {
            wrapper.like(GoodsInfo::getGoodsName, query.getGoodsName());
        }
        if (query.getGoodsType() != null) {
            wrapper.eq(GoodsInfo::getGoodsType, query.getGoodsType());
        }
        if (query.getCategoryCode() != null && !query.getCategoryCode().isEmpty()) {
            wrapper.eq(GoodsInfo::getCategoryCode, query.getCategoryCode());
        }
        if (query.getGoodsStatus() != null) {
            wrapper.eq(GoodsInfo::getGoodsStatus, query.getGoodsStatus());
        }
        if (query.getAuditStatus() != null) {
            wrapper.eq(GoodsInfo::getAuditStatus, query.getAuditStatus());
        }
        return wrapper;
    }

    @Override
    public GoodsInfo requireGoods(String goodsCode) {
        GoodsInfo entity = goodsInfoMapper.selectOne(new LambdaQueryWrapper<GoodsInfo>()
                .eq(GoodsInfo::getGoodsCode, goodsCode)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在: " + goodsCode);
        }
        return entity;
    }

    /** 生成商品编码：GD + 5 位序列 */
    private String nextGoodsCode() {
        return CODE_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private GoodsInfoVO toVO(GoodsInfo entity) {
        GoodsInfoVO vo = new GoodsInfoVO();
        vo.setId(entity.getId());
        vo.setGoodsCode(entity.getGoodsCode());
        vo.setGoodsName(entity.getGoodsName());
        vo.setGoodsShortName(entity.getGoodsShortName());
        vo.setGoodsType(entity.getGoodsType());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setBrandName(entity.getBrandName());
        vo.setCoverImage(entity.getCoverImage());
        vo.setImageUrls(entity.getImageUrls());
        vo.setVideoUrl(entity.getVideoUrl());
        vo.setGoodsDescription(entity.getGoodsDescription());
        vo.setSummary(entity.getSummary());
        vo.setOriginalPrice(entity.getOriginalPrice());
        vo.setSalePrice(entity.getSalePrice());
        vo.setCostPrice(entity.getCostPrice());
        vo.setPriceUnit(entity.getPriceUnit());
        vo.setStock(entity.getStock());
        vo.setSalesCount(entity.getSalesCount());
        vo.setViewCount(entity.getViewCount());
        vo.setCollectCount(entity.getCollectCount());
        vo.setSaleStartTime(entity.getSaleStartTime());
        vo.setSaleEndTime(entity.getSaleEndTime());
        vo.setIsHot(entity.getIsHot());
        vo.setIsNew(entity.getIsNew());
        vo.setIsRecommend(entity.getIsRecommend());
        vo.setSortOrder(entity.getSortOrder());
        vo.setGoodsStatus(entity.getGoodsStatus());
        vo.setAuditStatus(entity.getAuditStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
