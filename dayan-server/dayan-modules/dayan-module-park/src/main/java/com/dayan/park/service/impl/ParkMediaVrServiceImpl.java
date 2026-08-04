package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkMediaVrCreateDTO;
import com.dayan.park.dto.ParkMediaVrQueryDTO;
import com.dayan.park.dto.ParkMediaVrUpdateDTO;
import com.dayan.park.entity.ParkMediaVr;
import com.dayan.park.mapper.ParkMediaVrMapper;
import com.dayan.park.service.ParkMediaVrService;
import com.dayan.park.vo.ParkMediaVrVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构 VR（park_media_vr）服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkMediaVrServiceImpl implements ParkMediaVrService {

    private final ParkMediaVrMapper mediaVrMapper;

    @Override
    public PageResult<ParkMediaVrVO> page(ParkMediaVrQueryDTO query) {
        LambdaQueryWrapper<ParkMediaVr> wrapper = buildWrapper(query);
        Page<ParkMediaVr> page = mediaVrMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkMediaVrVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkMediaVrVO> listByPark(String parkCode) {
        return mediaVrMapper.selectList(new LambdaQueryWrapper<ParkMediaVr>()
                .eq(ParkMediaVr::getParkCode, parkCode)
                .orderByAsc(ParkMediaVr::getSortOrder)
                .orderByAsc(ParkMediaVr::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkMediaVrVO getDetail(Long id) {
        return toVO(requireVr(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkMediaVrCreateDTO dto) {
        Long count = mediaVrMapper.selectCount(new LambdaQueryWrapper<ParkMediaVr>()
                .eq(ParkMediaVr::getParkCode, dto.getParkCode())
                .eq(ParkMediaVr::getVrUrl, dto.getVrUrl()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "VR链接已存在: " + dto.getVrUrl());
        }

        ParkMediaVr entity = new ParkMediaVr();
        entity.setParkCode(dto.getParkCode());
        entity.setVrUrl(dto.getVrUrl());
        entity.setVrProvider(dto.getVrProvider());
        entity.setVrName(dto.getVrName());
        entity.setVrType(dto.getVrType());
        entity.setThumbnailUrl(dto.getThumbnailUrl());
        entity.setVrDescription(dto.getVrDescription());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        mediaVrMapper.insert(entity);
        log.info("创建机构VR成功: parkCode={}, id={}", dto.getParkCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkMediaVrUpdateDTO dto) {
        ParkMediaVr existing = requireVr(id);
        ParkMediaVr update = new ParkMediaVr();
        update.setId(existing.getId());
        if (dto.getVrUrl() != null && !dto.getVrUrl().equals(existing.getVrUrl())) {
            Long count = mediaVrMapper.selectCount(new LambdaQueryWrapper<ParkMediaVr>()
                    .eq(ParkMediaVr::getParkCode, existing.getParkCode())
                    .eq(ParkMediaVr::getVrUrl, dto.getVrUrl())
                    .ne(ParkMediaVr::getId, id));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "VR链接已存在: " + dto.getVrUrl());
            }
            update.setVrUrl(dto.getVrUrl());
        }
        if (dto.getVrProvider() != null) update.setVrProvider(dto.getVrProvider());
        if (dto.getVrName() != null) update.setVrName(dto.getVrName());
        if (dto.getVrType() != null) update.setVrType(dto.getVrType());
        if (dto.getThumbnailUrl() != null) update.setThumbnailUrl(dto.getThumbnailUrl());
        if (dto.getVrDescription() != null) update.setVrDescription(dto.getVrDescription());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        mediaVrMapper.updateById(update);
        log.info("更新机构VR成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkMediaVr existing = requireVr(id);
        mediaVrMapper.deleteById(existing.getId());
        log.info("删除机构VR成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkMediaVr> buildWrapper(ParkMediaVrQueryDTO query) {
        LambdaQueryWrapper<ParkMediaVr> wrapper = new LambdaQueryWrapper<ParkMediaVr>()
                .orderByAsc(ParkMediaVr::getSortOrder)
                .orderByAsc(ParkMediaVr::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkMediaVr::getParkCode, query.getParkCode());
        }
        if (query.getVrType() != null) {
            wrapper.eq(ParkMediaVr::getVrType, query.getVrType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkMediaVr::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkMediaVr requireVr(Long id) {
        ParkMediaVr entity = mediaVrMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构VR不存在: id=" + id);
        }
        return entity;
    }

    private ParkMediaVrVO toVO(ParkMediaVr entity) {
        ParkMediaVrVO vo = new ParkMediaVrVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setVrUrl(entity.getVrUrl());
        vo.setVrProvider(entity.getVrProvider());
        vo.setVrName(entity.getVrName());
        vo.setVrType(entity.getVrType());
        vo.setThumbnailUrl(entity.getThumbnailUrl());
        vo.setVrDescription(entity.getVrDescription());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
