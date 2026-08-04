package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkPeripheryCreateDTO;
import com.dayan.park.dto.ParkPeripheryQueryDTO;
import com.dayan.park.dto.ParkPeripheryUpdateDTO;
import com.dayan.park.entity.ParkPeriphery;
import com.dayan.park.mapper.ParkPeripheryMapper;
import com.dayan.park.service.ParkPeripheryService;
import com.dayan.park.vo.ParkPeripheryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构周边信息（park_periphery）服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkPeripheryServiceImpl implements ParkPeripheryService {

    private final ParkPeripheryMapper peripheryMapper;

    @Override
    public PageResult<ParkPeripheryVO> page(ParkPeripheryQueryDTO query) {
        LambdaQueryWrapper<ParkPeriphery> wrapper = buildWrapper(query);
        Page<ParkPeriphery> page = peripheryMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkPeripheryVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkPeripheryVO> listByPark(String parkCode) {
        return peripheryMapper.selectList(new LambdaQueryWrapper<ParkPeriphery>()
                .eq(ParkPeriphery::getParkCode, parkCode)
                .orderByAsc(ParkPeriphery::getSortOrder)
                .orderByAsc(ParkPeriphery::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkPeripheryVO getDetail(Long id) {
        return toVO(requirePeriphery(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkPeripheryCreateDTO dto) {
        ParkPeriphery entity = new ParkPeriphery();
        entity.setParkCode(dto.getParkCode());
        entity.setPeripheryType(dto.getPeripheryType());
        entity.setPlaceName(dto.getPlaceName());
        entity.setPlaceAddress(dto.getPlaceAddress());
        entity.setDistance(dto.getDistance());
        entity.setDetailDescription(dto.getDetailDescription());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        peripheryMapper.insert(entity);
        log.info("创建机构周边信息成功: parkCode={}, id={}", dto.getParkCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkPeripheryUpdateDTO dto) {
        ParkPeriphery existing = requirePeriphery(id);
        ParkPeriphery update = new ParkPeriphery();
        update.setId(existing.getId());
        if (dto.getPeripheryType() != null) update.setPeripheryType(dto.getPeripheryType());
        if (dto.getPlaceName() != null) update.setPlaceName(dto.getPlaceName());
        if (dto.getPlaceAddress() != null) update.setPlaceAddress(dto.getPlaceAddress());
        if (dto.getDistance() != null) update.setDistance(dto.getDistance());
        if (dto.getDetailDescription() != null) update.setDetailDescription(dto.getDetailDescription());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        peripheryMapper.updateById(update);
        log.info("更新机构周边信息成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkPeriphery existing = requirePeriphery(id);
        peripheryMapper.deleteById(existing.getId());
        log.info("删除机构周边信息成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkPeriphery> buildWrapper(ParkPeripheryQueryDTO query) {
        LambdaQueryWrapper<ParkPeriphery> wrapper = new LambdaQueryWrapper<ParkPeriphery>()
                .orderByAsc(ParkPeriphery::getSortOrder)
                .orderByAsc(ParkPeriphery::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkPeriphery::getParkCode, query.getParkCode());
        }
        if (query.getPeripheryType() != null) {
            wrapper.eq(ParkPeriphery::getPeripheryType, query.getPeripheryType());
        }
        if (query.getPlaceName() != null && !query.getPlaceName().isEmpty()) {
            wrapper.like(ParkPeriphery::getPlaceName, query.getPlaceName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkPeriphery::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkPeriphery requirePeriphery(Long id) {
        ParkPeriphery entity = peripheryMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构周边信息不存在: id=" + id);
        }
        return entity;
    }

    private ParkPeripheryVO toVO(ParkPeriphery entity) {
        ParkPeripheryVO vo = new ParkPeripheryVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setPeripheryType(entity.getPeripheryType());
        vo.setPlaceName(entity.getPlaceName());
        vo.setPlaceAddress(entity.getPlaceAddress());
        vo.setDistance(entity.getDistance());
        vo.setDetailDescription(entity.getDetailDescription());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
