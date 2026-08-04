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
import com.dayan.goods.entity.GoodsSkuCourse;
import com.dayan.goods.entity.GoodsSkuEquity;
import com.dayan.goods.entity.GoodsSkuScene;
import com.dayan.goods.entity.GoodsSkuSojourn;
import com.dayan.goods.mapper.GoodsInfoMapper;
import com.dayan.goods.mapper.GoodsSkuCourseMapper;
import com.dayan.goods.mapper.GoodsSkuEquityMapper;
import com.dayan.goods.mapper.GoodsSkuSceneMapper;
import com.dayan.goods.mapper.GoodsSkuSojournMapper;
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
 * <p>商品类型 goodsType（1权益/2场景/3课程/4旅居）创建后不可变更；
 * 删除时按类型校验对应 SKU 子表无关联记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsInfoServiceImpl implements GoodsInfoService {

    /** 商品编码前缀 */
    private static final String CODE_PREFIX = BusinessCode.GOODS;
    /** 序列键（全局共享计数，channelCode=0） */
    private static final String SEQ_KEY = "code:seq:" + CODE_PREFIX + ":0";

    /** 默认商品状态：0=下架 */
    private static final int DEFAULT_GOODS_STATUS = 0;
    /** 默认审核状态：0=待审 */
    private static final int DEFAULT_AUDIT_STATUS = 0;
    /** 上架 */
    private static final int STATUS_ON = 1;
    /** 下架 */
    private static final int STATUS_OFF = 0;

    private final GoodsInfoMapper goodsInfoMapper;
    private final GoodsSkuEquityMapper skuEquityMapper;
    private final GoodsSkuSceneMapper skuSceneMapper;
    private final GoodsSkuCourseMapper skuCourseMapper;
    private final GoodsSkuSojournMapper skuSojournMapper;
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
        Integer target = dto.getGoodsStatus();
        if (target == null || (target != STATUS_ON && target != STATUS_OFF)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "goodsStatus 仅支持 0=下架 / 1=上架");
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
                case 1 -> checkSkuEmpty(goodsCode, "权益", skuEquityMapper.selectCount(
                        new LambdaQueryWrapper<GoodsSkuEquity>().eq(GoodsSkuEquity::getGoodsCode, goodsCode)));
                case 2 -> checkSkuEmpty(goodsCode, "场景", skuSceneMapper.selectCount(
                        new LambdaQueryWrapper<GoodsSkuScene>().eq(GoodsSkuScene::getGoodsCode, goodsCode)));
                case 3 -> checkSkuEmpty(goodsCode, "课程", skuCourseMapper.selectCount(
                        new LambdaQueryWrapper<GoodsSkuCourse>().eq(GoodsSkuCourse::getGoodsCode, goodsCode)));
                case 4 -> checkSkuEmpty(goodsCode, "旅居", skuSojournMapper.selectCount(
                        new LambdaQueryWrapper<GoodsSkuSojourn>().eq(GoodsSkuSojourn::getGoodsCode, goodsCode)));
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

    private GoodsInfo requireGoods(String goodsCode) {
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
