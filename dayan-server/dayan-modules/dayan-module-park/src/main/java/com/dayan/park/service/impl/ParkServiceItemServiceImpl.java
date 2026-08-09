package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkServiceItemCreateDTO;
import com.dayan.park.dto.ParkServiceItemQueryDTO;
import com.dayan.park.dto.ParkServiceItemUpdateDTO;
import com.dayan.park.entity.ParkServiceItem;
import com.dayan.park.mapper.ParkServiceItemMapper;
import com.dayan.park.service.ParkServiceItemService;
import com.dayan.park.vo.ParkServiceItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构服务项（park_service_item）服务实现。
 *
 * <p>serviceCode 唯一校验（同 parkCode 下）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkServiceItemServiceImpl implements ParkServiceItemService {

    private final ParkServiceItemMapper serviceItemMapper;

    @Override
    public PageResult<ParkServiceItemVO> page(ParkServiceItemQueryDTO query) {
        LambdaQueryWrapper<ParkServiceItem> wrapper = buildWrapper(query);
        Page<ParkServiceItem> page = serviceItemMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkServiceItemVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkServiceItemVO> listByPark(String parkCode) {
        return serviceItemMapper.selectList(new LambdaQueryWrapper<ParkServiceItem>()
                .eq(ParkServiceItem::getParkCode, parkCode)
                .orderByAsc(ParkServiceItem::getSortOrder)
                .orderByAsc(ParkServiceItem::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkServiceItemVO getDetail(Long id) {
        return toVO(requireItem(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkServiceItemCreateDTO dto) {
        Long count = serviceItemMapper.selectCount(new LambdaQueryWrapper<ParkServiceItem>()
                .eq(ParkServiceItem::getParkCode, dto.getParkCode())
                .eq(ParkServiceItem::getServiceCode, dto.getServiceCode()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "服务编码已存在: " + dto.getServiceCode());
        }

        ParkServiceItem entity = new ParkServiceItem();
        entity.setParkCode(dto.getParkCode());
        entity.setServiceCode(dto.getServiceCode());
        entity.setServiceName(dto.getServiceName());
        entity.setServiceCategory(dto.getServiceCategory());
        entity.setServiceDescription(dto.getServiceDescription());
        entity.setServiceFrequency(dto.getServiceFrequency());
        entity.setServiceDuration(dto.getServiceDuration());
        entity.setCoverImage(dto.getCoverImage());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        serviceItemMapper.insert(entity);
        log.info("创建机构服务项成功: parkCode={}, serviceCode={}, id={}",
                dto.getParkCode(), dto.getServiceCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkServiceItemUpdateDTO dto) {
        ParkServiceItem existing = requireItem(id);
        ParkServiceItem update = new ParkServiceItem();
        update.setId(existing.getId());
        if (dto.getServiceName() != null) update.setServiceName(dto.getServiceName());
        if (dto.getServiceCategory() != null) update.setServiceCategory(dto.getServiceCategory());
        if (dto.getServiceDescription() != null) update.setServiceDescription(dto.getServiceDescription());
        if (dto.getServiceFrequency() != null) update.setServiceFrequency(dto.getServiceFrequency());
        if (dto.getServiceDuration() != null) update.setServiceDuration(dto.getServiceDuration());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        serviceItemMapper.updateById(update);
        log.info("更新机构服务项成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkServiceItem existing = requireItem(id);
        serviceItemMapper.deleteById(existing.getId());
        log.info("删除机构服务项成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkServiceItem> buildWrapper(ParkServiceItemQueryDTO query) {
        LambdaQueryWrapper<ParkServiceItem> wrapper = new LambdaQueryWrapper<ParkServiceItem>()
                .orderByAsc(ParkServiceItem::getSortOrder)
                .orderByAsc(ParkServiceItem::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkServiceItem::getParkCode, query.getParkCode());
        }
        if (query.getServiceCode() != null && !query.getServiceCode().isEmpty()) {
            wrapper.eq(ParkServiceItem::getServiceCode, query.getServiceCode());
        }
        if (query.getServiceName() != null && !query.getServiceName().isEmpty()) {
            wrapper.like(ParkServiceItem::getServiceName, query.getServiceName());
        }
        if (query.getServiceCategory() != null) {
            wrapper.eq(ParkServiceItem::getServiceCategory, query.getServiceCategory());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkServiceItem::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkServiceItem requireItem(Long id) {
        ParkServiceItem entity = serviceItemMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构服务项不存在: id=" + id);
        }
        return entity;
    }

    private ParkServiceItemVO toVO(ParkServiceItem entity) {
        ParkServiceItemVO vo = new ParkServiceItemVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setServiceCode(entity.getServiceCode());
        vo.setServiceName(entity.getServiceName());
        vo.setServiceCategory(entity.getServiceCategory());
        vo.setServiceDescription(entity.getServiceDescription());
        vo.setServiceFrequency(entity.getServiceFrequency());
        vo.setServiceDuration(entity.getServiceDuration());
        vo.setCoverImage(entity.getCoverImage());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
