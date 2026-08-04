package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkRoomTypeCreateDTO;
import com.dayan.park.dto.ParkRoomTypeQueryDTO;
import com.dayan.park.dto.ParkRoomTypeUpdateDTO;
import com.dayan.park.entity.ParkRoomType;
import com.dayan.park.mapper.ParkRoomTypeMapper;
import com.dayan.park.service.ParkRoomTypeService;
import com.dayan.park.vo.ParkRoomTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 房型（park_room_type）服务实现。
 *
 * <p>校验规则：
 * <ul>
 *   <li>roomTypeCode 同 parkCode 下唯一</li>
 *   <li>totalRooms &gt;= availableRooms（总数必须不小于可入住数）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkRoomTypeServiceImpl implements ParkRoomTypeService {

    private final ParkRoomTypeMapper roomTypeMapper;

    @Override
    public PageResult<ParkRoomTypeVO> page(ParkRoomTypeQueryDTO query) {
        LambdaQueryWrapper<ParkRoomType> wrapper = buildWrapper(query);
        Page<ParkRoomType> page = roomTypeMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkRoomTypeVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkRoomTypeVO> listByPark(String parkCode) {
        return roomTypeMapper.selectList(new LambdaQueryWrapper<ParkRoomType>()
                .eq(ParkRoomType::getParkCode, parkCode)
                .orderByAsc(ParkRoomType::getSortOrder)
                .orderByAsc(ParkRoomType::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkRoomTypeVO getDetail(Long id) {
        return toVO(requireRoomType(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkRoomTypeCreateDTO dto) {
        Long count = roomTypeMapper.selectCount(new LambdaQueryWrapper<ParkRoomType>()
                .eq(ParkRoomType::getParkCode, dto.getParkCode())
                .eq(ParkRoomType::getRoomTypeCode, dto.getRoomTypeCode()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "房型编码已存在: " + dto.getRoomTypeCode());
        }
        validateRoomTotal(dto.getTotalRooms(), dto.getAvailableRooms());

        ParkRoomType entity = new ParkRoomType();
        entity.setParkCode(dto.getParkCode());
        entity.setRoomTypeCode(dto.getRoomTypeCode());
        entity.setRoomTypeName(dto.getRoomTypeName());
        entity.setStayType(dto.getStayType());
        entity.setBuildingName(dto.getBuildingName());
        entity.setFloor(dto.getFloor());
        entity.setRoomCategory(dto.getRoomCategory());
        entity.setArea(dto.getArea());
        entity.setOrientation(dto.getOrientation());
        entity.setBedCount(dto.getBedCount());
        entity.setTotalRooms(dto.getTotalRooms());
        entity.setAvailableRooms(dto.getAvailableRooms());
        entity.setHasBathroom(dto.getHasBathroom());
        entity.setHasKitchen(dto.getHasKitchen());
        entity.setHasBalcony(dto.getHasBalcony());
        entity.setHasTv(dto.getHasTv());
        entity.setHasAircon(dto.getHasAircon());
        entity.setHasFridge(dto.getHasFridge());
        entity.setHasWasher(dto.getHasWasher());
        entity.setHasWifi(dto.getHasWifi());
        entity.setHasEmergency(dto.getHasEmergency());
        entity.setHasMonitor(dto.getHasMonitor());
        entity.setFacilities(dto.getFacilities());
        entity.setDescription(dto.getDescription());
        entity.setCoverImage(dto.getCoverImage());
        entity.setImages(dto.getImages());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setDesignDescription(dto.getDesignDescription());
        entity.setDesignImage(dto.getDesignImage());
        entity.setAdditionalImages(dto.getAdditionalImages());
        roomTypeMapper.insert(entity);
        log.info("创建房型成功: parkCode={}, roomTypeCode={}, id={}",
                dto.getParkCode(), dto.getRoomTypeCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkRoomTypeUpdateDTO dto) {
        ParkRoomType existing = requireRoomType(id);
        // 总数校验：合并已有值与新值
        Integer total = dto.getTotalRooms() != null ? dto.getTotalRooms() : existing.getTotalRooms();
        Integer available = dto.getAvailableRooms() != null ? dto.getAvailableRooms() : existing.getAvailableRooms();
        validateRoomTotal(total, available);

        ParkRoomType update = new ParkRoomType();
        update.setId(existing.getId());
        if (dto.getRoomTypeName() != null) update.setRoomTypeName(dto.getRoomTypeName());
        if (dto.getStayType() != null) update.setStayType(dto.getStayType());
        if (dto.getBuildingName() != null) update.setBuildingName(dto.getBuildingName());
        if (dto.getFloor() != null) update.setFloor(dto.getFloor());
        if (dto.getRoomCategory() != null) update.setRoomCategory(dto.getRoomCategory());
        if (dto.getArea() != null) update.setArea(dto.getArea());
        if (dto.getOrientation() != null) update.setOrientation(dto.getOrientation());
        if (dto.getBedCount() != null) update.setBedCount(dto.getBedCount());
        if (dto.getTotalRooms() != null) update.setTotalRooms(dto.getTotalRooms());
        if (dto.getAvailableRooms() != null) update.setAvailableRooms(dto.getAvailableRooms());
        if (dto.getHasBathroom() != null) update.setHasBathroom(dto.getHasBathroom());
        if (dto.getHasKitchen() != null) update.setHasKitchen(dto.getHasKitchen());
        if (dto.getHasBalcony() != null) update.setHasBalcony(dto.getHasBalcony());
        if (dto.getHasTv() != null) update.setHasTv(dto.getHasTv());
        if (dto.getHasAircon() != null) update.setHasAircon(dto.getHasAircon());
        if (dto.getHasFridge() != null) update.setHasFridge(dto.getHasFridge());
        if (dto.getHasWasher() != null) update.setHasWasher(dto.getHasWasher());
        if (dto.getHasWifi() != null) update.setHasWifi(dto.getHasWifi());
        if (dto.getHasEmergency() != null) update.setHasEmergency(dto.getHasEmergency());
        if (dto.getHasMonitor() != null) update.setHasMonitor(dto.getHasMonitor());
        if (dto.getFacilities() != null) update.setFacilities(dto.getFacilities());
        if (dto.getDescription() != null) update.setDescription(dto.getDescription());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getImages() != null) update.setImages(dto.getImages());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getDesignDescription() != null) update.setDesignDescription(dto.getDesignDescription());
        if (dto.getDesignImage() != null) update.setDesignImage(dto.getDesignImage());
        if (dto.getAdditionalImages() != null) update.setAdditionalImages(dto.getAdditionalImages());
        roomTypeMapper.updateById(update);
        log.info("更新房型成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkRoomType existing = requireRoomType(id);
        roomTypeMapper.deleteById(existing.getId());
        log.info("删除房型成功: id={}", id);
    }

    // ====== 内部方法 ======

    /** 校验：总数必须不小于可入住数 */
    private void validateRoomTotal(Integer totalRooms, Integer availableRooms) {
        if (totalRooms != null && availableRooms != null && totalRooms < availableRooms) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "房间总数(totalRooms=" + totalRooms + ")不能小于可入住数(availableRooms=" + availableRooms + ")");
        }
    }

    private LambdaQueryWrapper<ParkRoomType> buildWrapper(ParkRoomTypeQueryDTO query) {
        LambdaQueryWrapper<ParkRoomType> wrapper = new LambdaQueryWrapper<ParkRoomType>()
                .orderByAsc(ParkRoomType::getSortOrder)
                .orderByAsc(ParkRoomType::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkRoomType::getParkCode, query.getParkCode());
        }
        if (query.getRoomTypeCode() != null && !query.getRoomTypeCode().isEmpty()) {
            wrapper.eq(ParkRoomType::getRoomTypeCode, query.getRoomTypeCode());
        }
        if (query.getRoomTypeName() != null && !query.getRoomTypeName().isEmpty()) {
            wrapper.like(ParkRoomType::getRoomTypeName, query.getRoomTypeName());
        }
        if (query.getRoomCategory() != null) {
            wrapper.eq(ParkRoomType::getRoomCategory, query.getRoomCategory());
        }
        if (query.getStayType() != null) {
            wrapper.eq(ParkRoomType::getStayType, query.getStayType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkRoomType::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkRoomType requireRoomType(Long id) {
        ParkRoomType entity = roomTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "房型不存在: id=" + id);
        }
        return entity;
    }

    private ParkRoomTypeVO toVO(ParkRoomType entity) {
        ParkRoomTypeVO vo = new ParkRoomTypeVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setRoomTypeCode(entity.getRoomTypeCode());
        vo.setRoomTypeName(entity.getRoomTypeName());
        vo.setStayType(entity.getStayType());
        vo.setBuildingName(entity.getBuildingName());
        vo.setFloor(entity.getFloor());
        vo.setRoomCategory(entity.getRoomCategory());
        vo.setArea(entity.getArea());
        vo.setOrientation(entity.getOrientation());
        vo.setBedCount(entity.getBedCount());
        vo.setTotalRooms(entity.getTotalRooms());
        vo.setAvailableRooms(entity.getAvailableRooms());
        vo.setHasBathroom(entity.getHasBathroom());
        vo.setHasKitchen(entity.getHasKitchen());
        vo.setHasBalcony(entity.getHasBalcony());
        vo.setHasTv(entity.getHasTv());
        vo.setHasAircon(entity.getHasAircon());
        vo.setHasFridge(entity.getHasFridge());
        vo.setHasWasher(entity.getHasWasher());
        vo.setHasWifi(entity.getHasWifi());
        vo.setHasEmergency(entity.getHasEmergency());
        vo.setHasMonitor(entity.getHasMonitor());
        vo.setFacilities(entity.getFacilities());
        vo.setDescription(entity.getDescription());
        vo.setCoverImage(entity.getCoverImage());
        vo.setImages(entity.getImages());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setDesignDescription(entity.getDesignDescription());
        vo.setDesignImage(entity.getDesignImage());
        vo.setAdditionalImages(entity.getAdditionalImages());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
