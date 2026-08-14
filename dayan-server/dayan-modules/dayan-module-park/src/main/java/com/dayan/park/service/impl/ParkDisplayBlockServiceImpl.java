package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.enums.NetworkType;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.util.HtmlSanitizer;
import com.dayan.park.dto.ParkDisplayBlockCreateDTO;
import com.dayan.park.dto.ParkDisplayBlockQueryDTO;
import com.dayan.park.dto.ParkDisplayBlockUpdateDTO;
import com.dayan.park.entity.ParkDisplayBlock;
import com.dayan.park.mapper.ParkDisplayBlockMapper;
import com.dayan.park.service.ParkDisplayBlockService;
import com.dayan.park.vo.ParkDisplayBlockVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构展示板块（park_display_block）服务实现。
 *
 * <p>同一 parkCode 下同 block_type 的板块允许有多个（如多个"文娱活动"板块），
 * 按 sortOrder 升序排列。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkDisplayBlockServiceImpl implements ParkDisplayBlockService {

    private final ParkDisplayBlockMapper displayBlockMapper;

    @Override
    public PageResult<ParkDisplayBlockVO> page(ParkDisplayBlockQueryDTO query) {
        LambdaQueryWrapper<ParkDisplayBlock> wrapper = buildWrapper(query);
        Page<ParkDisplayBlock> page = displayBlockMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkDisplayBlockVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkDisplayBlockVO> listByPark(String parkCode) {
        return displayBlockMapper.selectList(new LambdaQueryWrapper<ParkDisplayBlock>()
                .eq(ParkDisplayBlock::getParkCode, parkCode)
                .eq(ParkDisplayBlock::getStatus, 1)
                .orderByAsc(ParkDisplayBlock::getSortOrder)
                .orderByAsc(ParkDisplayBlock::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkDisplayBlockVO getDetail(Long id) {
        return toVO(requireBlock(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkDisplayBlockCreateDTO dto) {
        ParkDisplayBlock entity = new ParkDisplayBlock();
        entity.setParkCode(dto.getParkCode());
        entity.setBlockType(dto.getBlockType());
        entity.setBlockTitle(dto.getBlockTitle());
        entity.setContent(HtmlSanitizer.clean(dto.getContent()));
        entity.setImages(dto.getImages());
        entity.setImageDescriptions(dto.getImageDescriptions());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setNetworkTags(NetworkType.normalizeTags(dto.getNetworkTags()));
        displayBlockMapper.insert(entity);
        log.info("创建展示板块成功: parkCode={}, blockType={}, id={}",
                dto.getParkCode(), dto.getBlockType(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkDisplayBlockUpdateDTO dto) {
        ParkDisplayBlock existing = requireBlock(id);
        ParkDisplayBlock update = new ParkDisplayBlock();
        update.setId(existing.getId());
        if (dto.getBlockTitle() != null) update.setBlockTitle(dto.getBlockTitle());
        if (dto.getContent() != null) update.setContent(HtmlSanitizer.clean(dto.getContent()));
        if (dto.getImages() != null) update.setImages(dto.getImages());
        if (dto.getImageDescriptions() != null) update.setImageDescriptions(dto.getImageDescriptions());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getNetworkTags() != null) {
            String normalized = NetworkType.normalizeTags(dto.getNetworkTags());
            if (normalized == null) {
                // 空串=恢复全部业态：updateById 的 NOT_NULL 策略会忽略 null 字段，需显式 set NULL
                displayBlockMapper.update(null, new LambdaUpdateWrapper<ParkDisplayBlock>()
                        .eq(ParkDisplayBlock::getId, id)
                        .set(ParkDisplayBlock::getNetworkTags, null));
            } else {
                update.setNetworkTags(normalized);
            }
        }
        displayBlockMapper.updateById(update);
        log.info("更新展示板块成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkDisplayBlock existing = requireBlock(id);
        displayBlockMapper.deleteById(existing.getId());
        log.info("删除展示板块成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkDisplayBlock> buildWrapper(ParkDisplayBlockQueryDTO query) {
        LambdaQueryWrapper<ParkDisplayBlock> wrapper = new LambdaQueryWrapper<ParkDisplayBlock>()
                .orderByAsc(ParkDisplayBlock::getSortOrder)
                .orderByAsc(ParkDisplayBlock::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkDisplayBlock::getParkCode, query.getParkCode());
        }
        if (query.getBlockType() != null && !query.getBlockType().isEmpty()) {
            wrapper.eq(ParkDisplayBlock::getBlockType, query.getBlockType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkDisplayBlock::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkDisplayBlock requireBlock(Long id) {
        ParkDisplayBlock entity = displayBlockMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "展示板块不存在: id=" + id);
        }
        return entity;
    }

    private ParkDisplayBlockVO toVO(ParkDisplayBlock entity) {
        ParkDisplayBlockVO vo = new ParkDisplayBlockVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setBlockType(entity.getBlockType());
        vo.setBlockTitle(entity.getBlockTitle());
        vo.setContent(entity.getContent());
        vo.setImages(entity.getImages());
        vo.setImageDescriptions(entity.getImageDescriptions());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setNetworkTags(entity.getNetworkTags() == null || entity.getNetworkTags().isEmpty()
                ? Collections.emptyList()
                : Arrays.asList(entity.getNetworkTags().split(",")));
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
