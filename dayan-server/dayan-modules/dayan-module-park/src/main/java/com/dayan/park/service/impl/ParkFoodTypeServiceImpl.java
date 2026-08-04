package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkFoodTypeCreateDTO;
import com.dayan.park.dto.ParkFoodTypeQueryDTO;
import com.dayan.park.dto.ParkFoodTypeUpdateDTO;
import com.dayan.park.entity.ParkFoodType;
import com.dayan.park.mapper.ParkFoodTypeMapper;
import com.dayan.park.service.ParkFoodTypeService;
import com.dayan.park.vo.ParkFoodTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 餐饮类型（park_food_type）服务实现。
 *
 * <p>foodTypeCode 同 parkCode 下唯一。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkFoodTypeServiceImpl implements ParkFoodTypeService {

    private final ParkFoodTypeMapper foodTypeMapper;

    @Override
    public PageResult<ParkFoodTypeVO> page(ParkFoodTypeQueryDTO query) {
        LambdaQueryWrapper<ParkFoodType> wrapper = buildWrapper(query);
        Page<ParkFoodType> page = foodTypeMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkFoodTypeVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkFoodTypeVO> listByPark(String parkCode) {
        return foodTypeMapper.selectList(new LambdaQueryWrapper<ParkFoodType>()
                .eq(ParkFoodType::getParkCode, parkCode)
                .orderByAsc(ParkFoodType::getSortOrder)
                .orderByAsc(ParkFoodType::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkFoodTypeVO getDetail(Long id) {
        return toVO(requireFoodType(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkFoodTypeCreateDTO dto) {
        Long count = foodTypeMapper.selectCount(new LambdaQueryWrapper<ParkFoodType>()
                .eq(ParkFoodType::getParkCode, dto.getParkCode())
                .eq(ParkFoodType::getFoodTypeCode, dto.getFoodTypeCode()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "餐饮类型编码已存在: " + dto.getFoodTypeCode());
        }

        ParkFoodType entity = new ParkFoodType();
        entity.setParkCode(dto.getParkCode());
        entity.setFoodTypeCode(dto.getFoodTypeCode());
        entity.setFoodTypeName(dto.getFoodTypeName());
        entity.setMealPlan(dto.getMealPlan());
        entity.setDietFeatures(dto.getDietFeatures());
        entity.setSampleMenu(dto.getSampleMenu());
        entity.setSpecialDiet(dto.getSpecialDiet() == null ? 0 : dto.getSpecialDiet());
        entity.setSpecialDietDescription(dto.getSpecialDietDescription());
        entity.setDescription(dto.getDescription());
        entity.setCoverImage(dto.getCoverImage());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        foodTypeMapper.insert(entity);
        log.info("创建餐饮类型成功: parkCode={}, foodTypeCode={}, id={}",
                dto.getParkCode(), dto.getFoodTypeCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkFoodTypeUpdateDTO dto) {
        ParkFoodType existing = requireFoodType(id);
        ParkFoodType update = new ParkFoodType();
        update.setId(existing.getId());
        if (dto.getFoodTypeName() != null) update.setFoodTypeName(dto.getFoodTypeName());
        if (dto.getMealPlan() != null) update.setMealPlan(dto.getMealPlan());
        if (dto.getDietFeatures() != null) update.setDietFeatures(dto.getDietFeatures());
        if (dto.getSampleMenu() != null) update.setSampleMenu(dto.getSampleMenu());
        if (dto.getSpecialDiet() != null) update.setSpecialDiet(dto.getSpecialDiet());
        if (dto.getSpecialDietDescription() != null) update.setSpecialDietDescription(dto.getSpecialDietDescription());
        if (dto.getDescription() != null) update.setDescription(dto.getDescription());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        foodTypeMapper.updateById(update);
        log.info("更新餐饮类型成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkFoodType existing = requireFoodType(id);
        foodTypeMapper.deleteById(existing.getId());
        log.info("删除餐饮类型成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkFoodType> buildWrapper(ParkFoodTypeQueryDTO query) {
        LambdaQueryWrapper<ParkFoodType> wrapper = new LambdaQueryWrapper<ParkFoodType>()
                .orderByAsc(ParkFoodType::getSortOrder)
                .orderByAsc(ParkFoodType::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkFoodType::getParkCode, query.getParkCode());
        }
        if (query.getFoodTypeCode() != null && !query.getFoodTypeCode().isEmpty()) {
            wrapper.eq(ParkFoodType::getFoodTypeCode, query.getFoodTypeCode());
        }
        if (query.getFoodTypeName() != null && !query.getFoodTypeName().isEmpty()) {
            wrapper.like(ParkFoodType::getFoodTypeName, query.getFoodTypeName());
        }
        if (query.getMealPlan() != null) {
            wrapper.eq(ParkFoodType::getMealPlan, query.getMealPlan());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkFoodType::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkFoodType requireFoodType(Long id) {
        ParkFoodType entity = foodTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "餐饮类型不存在: id=" + id);
        }
        return entity;
    }

    private ParkFoodTypeVO toVO(ParkFoodType entity) {
        ParkFoodTypeVO vo = new ParkFoodTypeVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setFoodTypeCode(entity.getFoodTypeCode());
        vo.setFoodTypeName(entity.getFoodTypeName());
        vo.setMealPlan(entity.getMealPlan());
        vo.setDietFeatures(entity.getDietFeatures());
        vo.setSampleMenu(entity.getSampleMenu());
        vo.setSpecialDiet(entity.getSpecialDiet());
        vo.setSpecialDietDescription(entity.getSpecialDietDescription());
        vo.setDescription(entity.getDescription());
        vo.setCoverImage(entity.getCoverImage());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
