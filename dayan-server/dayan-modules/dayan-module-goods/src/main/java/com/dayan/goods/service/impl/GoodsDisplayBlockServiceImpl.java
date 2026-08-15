package com.dayan.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.util.HtmlSanitizer;
import com.dayan.goods.dto.GoodsDisplayBlockCreateDTO;
import com.dayan.goods.dto.GoodsDisplayBlockQueryDTO;
import com.dayan.goods.dto.GoodsDisplayBlockUpdateDTO;
import com.dayan.goods.entity.GoodsDisplayBlock;
import com.dayan.goods.mapper.GoodsDisplayBlockMapper;
import com.dayan.goods.service.GoodsDisplayBlockService;
import com.dayan.goods.vo.GoodsDisplayBlockVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品展示板块（goods_display_block）服务实现。
 *
 * <p>同一 goodsCode 下同 block_type 的板块允许有多个，按 sortOrder 升序排列。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsDisplayBlockServiceImpl implements GoodsDisplayBlockService {

    private final GoodsDisplayBlockMapper displayBlockMapper;

    @Override
    public PageResult<GoodsDisplayBlockVO> page(GoodsDisplayBlockQueryDTO query) {
        LambdaQueryWrapper<GoodsDisplayBlock> wrapper = buildWrapper(query);
        Page<GoodsDisplayBlock> page = displayBlockMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<GoodsDisplayBlockVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<GoodsDisplayBlockVO> listByGoods(String goodsCode) {
        return displayBlockMapper.selectList(new LambdaQueryWrapper<GoodsDisplayBlock>()
                .eq(GoodsDisplayBlock::getGoodsCode, goodsCode)
                .eq(GoodsDisplayBlock::getStatus, 1)
                .orderByAsc(GoodsDisplayBlock::getSortOrder)
                .orderByAsc(GoodsDisplayBlock::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public GoodsDisplayBlockVO getDetail(Long id) {
        return toVO(requireBlock(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(GoodsDisplayBlockCreateDTO dto) {
        GoodsDisplayBlock entity = new GoodsDisplayBlock();
        entity.setGoodsCode(dto.getGoodsCode());
        entity.setBlockType(dto.getBlockType());
        entity.setBlockTitle(dto.getBlockTitle());
        entity.setContent(HtmlSanitizer.clean(dto.getContent()));
        entity.setImages(dto.getImages());
        entity.setImageDescriptions(dto.getImageDescriptions());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        displayBlockMapper.insert(entity);
        log.info("创建商品展示板块成功: goodsCode={}, blockType={}, id={}",
                dto.getGoodsCode(), dto.getBlockType(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GoodsDisplayBlockUpdateDTO dto) {
        GoodsDisplayBlock existing = requireBlock(id);
        GoodsDisplayBlock update = new GoodsDisplayBlock();
        update.setId(existing.getId());
        if (dto.getBlockType() != null) update.setBlockType(dto.getBlockType());
        if (dto.getBlockTitle() != null) update.setBlockTitle(dto.getBlockTitle());
        if (dto.getContent() != null) update.setContent(HtmlSanitizer.clean(dto.getContent()));
        if (dto.getImages() != null) update.setImages(dto.getImages());
        if (dto.getImageDescriptions() != null) update.setImageDescriptions(dto.getImageDescriptions());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        displayBlockMapper.updateById(update);
        log.info("更新商品展示板块成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GoodsDisplayBlock existing = requireBlock(id);
        displayBlockMapper.deleteById(existing.getId());
        log.info("删除商品展示板块成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<GoodsDisplayBlock> buildWrapper(GoodsDisplayBlockQueryDTO query) {
        LambdaQueryWrapper<GoodsDisplayBlock> wrapper = new LambdaQueryWrapper<GoodsDisplayBlock>()
                .orderByAsc(GoodsDisplayBlock::getSortOrder)
                .orderByAsc(GoodsDisplayBlock::getId);
        if (query.getGoodsCode() != null && !query.getGoodsCode().isEmpty()) {
            wrapper.eq(GoodsDisplayBlock::getGoodsCode, query.getGoodsCode());
        }
        if (query.getBlockType() != null && !query.getBlockType().isEmpty()) {
            wrapper.eq(GoodsDisplayBlock::getBlockType, query.getBlockType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(GoodsDisplayBlock::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private GoodsDisplayBlock requireBlock(Long id) {
        GoodsDisplayBlock entity = displayBlockMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品展示板块不存在: id=" + id);
        }
        return entity;
    }

    private GoodsDisplayBlockVO toVO(GoodsDisplayBlock entity) {
        GoodsDisplayBlockVO vo = new GoodsDisplayBlockVO();
        vo.setId(entity.getId());
        vo.setGoodsCode(entity.getGoodsCode());
        vo.setBlockType(entity.getBlockType());
        vo.setBlockTitle(entity.getBlockTitle());
        vo.setContent(entity.getContent());
        vo.setImages(entity.getImages());
        vo.setImageDescriptions(entity.getImageDescriptions());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
