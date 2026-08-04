package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkFoodPriceCreateDTO;
import com.dayan.park.dto.ParkFoodPriceQueryDTO;
import com.dayan.park.dto.ParkFoodPriceUpdateDTO;
import com.dayan.park.entity.ParkFoodPrice;
import com.dayan.park.mapper.ParkFoodPriceMapper;
import com.dayan.park.service.ParkFoodPriceService;
import com.dayan.park.vo.ParkFoodPriceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 餐饮价格（park_food_price）服务实现。
 *
 * <p>校验规则：
 * <ul>
 *   <li>effectiveDate &lt; expireDate</li>
 *   <li>isCurrent=1 在同 foodTypeCode 下唯一（设当前价时自动置 0 其余）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkFoodPriceServiceImpl implements ParkFoodPriceService {

    private static final int IS_CURRENT_YES = 1;

    private final ParkFoodPriceMapper foodPriceMapper;

    @Override
    public PageResult<ParkFoodPriceVO> page(ParkFoodPriceQueryDTO query) {
        LambdaQueryWrapper<ParkFoodPrice> wrapper = buildWrapper(query);
        Page<ParkFoodPrice> page = foodPriceMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkFoodPriceVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkFoodPriceVO> listByFoodType(String parkCode, String foodTypeCode) {
        return foodPriceMapper.selectList(new LambdaQueryWrapper<ParkFoodPrice>()
                .eq(ParkFoodPrice::getParkCode, parkCode)
                .eq(ParkFoodPrice::getFoodTypeCode, foodTypeCode)
                .orderByAsc(ParkFoodPrice::getSortOrder)
                .orderByDesc(ParkFoodPrice::getIsCurrent)
                .orderByAsc(ParkFoodPrice::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkFoodPriceVO getDetail(Long id) {
        return toVO(requirePrice(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkFoodPriceCreateDTO dto) {
        validateDateRange(dto.getEffectiveDate(), dto.getExpireDate());
        Integer isCurrent = (dto.getIsCurrent() != null && dto.getIsCurrent() == IS_CURRENT_YES)
                ? IS_CURRENT_YES : 0;
        if (isCurrent == IS_CURRENT_YES) {
            clearOtherCurrent(dto.getFoodTypeCode(), null);
        }

        ParkFoodPrice entity = new ParkFoodPrice();
        entity.setParkCode(dto.getParkCode());
        entity.setFoodTypeCode(dto.getFoodTypeCode());
        entity.setPriceType(dto.getPriceType());
        entity.setOriginalPrice(dto.getOriginalPrice());
        entity.setSalePrice(dto.getSalePrice());
        entity.setDiscountRate(dto.getDiscountRate());
        entity.setPriceDescription(dto.getPriceDescription());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setExpireDate(dto.getExpireDate());
        entity.setIsCurrent(isCurrent);
        entity.setIsPromotion(dto.getIsPromotion() == null ? 0 : dto.getIsPromotion());
        entity.setPromotionDescription(dto.getPromotionDescription());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        foodPriceMapper.insert(entity);
        log.info("创建餐饮价格成功: parkCode={}, foodTypeCode={}, id={}",
                dto.getParkCode(), dto.getFoodTypeCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkFoodPriceUpdateDTO dto) {
        ParkFoodPrice existing = requirePrice(id);
        LocalDate effective = dto.getEffectiveDate() != null ? dto.getEffectiveDate() : existing.getEffectiveDate();
        LocalDate expire = dto.getExpireDate() != null ? dto.getExpireDate() : existing.getExpireDate();
        validateDateRange(effective, expire);

        ParkFoodPrice update = new ParkFoodPrice();
        update.setId(existing.getId());
        if (dto.getPriceType() != null) update.setPriceType(dto.getPriceType());
        if (dto.getOriginalPrice() != null) update.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getSalePrice() != null) update.setSalePrice(dto.getSalePrice());
        if (dto.getDiscountRate() != null) update.setDiscountRate(dto.getDiscountRate());
        if (dto.getPriceDescription() != null) update.setPriceDescription(dto.getPriceDescription());
        if (dto.getEffectiveDate() != null) update.setEffectiveDate(dto.getEffectiveDate());
        if (dto.getExpireDate() != null) update.setExpireDate(dto.getExpireDate());
        if (dto.getIsPromotion() != null) update.setIsPromotion(dto.getIsPromotion());
        if (dto.getPromotionDescription() != null) update.setPromotionDescription(dto.getPromotionDescription());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getIsCurrent() != null) {
            int isCurrent = (dto.getIsCurrent() == IS_CURRENT_YES) ? IS_CURRENT_YES : 0;
            if (isCurrent == IS_CURRENT_YES) {
                clearOtherCurrent(existing.getFoodTypeCode(), id);
            }
            update.setIsCurrent(isCurrent);
        }
        foodPriceMapper.updateById(update);
        log.info("更新餐饮价格成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkFoodPrice existing = requirePrice(id);
        foodPriceMapper.deleteById(existing.getId());
        log.info("删除餐饮价格成功: id={}", id);
    }

    // ====== 内部方法 ======

    private void validateDateRange(LocalDate effectiveDate, LocalDate expireDate) {
        if (effectiveDate != null && expireDate != null && !effectiveDate.isBefore(expireDate)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "生效日期(effectiveDate)必须早于失效日期(expireDate)");
        }
    }

    private void clearOtherCurrent(String foodTypeCode, Long excludeId) {
        LambdaUpdateWrapper<ParkFoodPrice> wrapper = new LambdaUpdateWrapper<ParkFoodPrice>()
                .eq(ParkFoodPrice::getFoodTypeCode, foodTypeCode)
                .eq(ParkFoodPrice::getIsCurrent, IS_CURRENT_YES)
                .set(ParkFoodPrice::getIsCurrent, 0);
        if (excludeId != null) {
            wrapper.ne(ParkFoodPrice::getId, excludeId);
        }
        foodPriceMapper.update(null, wrapper);
    }

    private LambdaQueryWrapper<ParkFoodPrice> buildWrapper(ParkFoodPriceQueryDTO query) {
        LambdaQueryWrapper<ParkFoodPrice> wrapper = new LambdaQueryWrapper<ParkFoodPrice>()
                .orderByAsc(ParkFoodPrice::getSortOrder)
                .orderByDesc(ParkFoodPrice::getIsCurrent)
                .orderByAsc(ParkFoodPrice::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkFoodPrice::getParkCode, query.getParkCode());
        }
        if (query.getFoodTypeCode() != null && !query.getFoodTypeCode().isEmpty()) {
            wrapper.eq(ParkFoodPrice::getFoodTypeCode, query.getFoodTypeCode());
        }
        if (query.getPriceType() != null) {
            wrapper.eq(ParkFoodPrice::getPriceType, query.getPriceType());
        }
        if (query.getIsCurrent() != null) {
            wrapper.eq(ParkFoodPrice::getIsCurrent, query.getIsCurrent());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkFoodPrice::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkFoodPrice requirePrice(Long id) {
        ParkFoodPrice entity = foodPriceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "餐饮价格不存在: id=" + id);
        }
        return entity;
    }

    private ParkFoodPriceVO toVO(ParkFoodPrice entity) {
        ParkFoodPriceVO vo = new ParkFoodPriceVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setFoodTypeCode(entity.getFoodTypeCode());
        vo.setPriceType(entity.getPriceType());
        vo.setOriginalPrice(entity.getOriginalPrice());
        vo.setSalePrice(entity.getSalePrice());
        vo.setDiscountRate(entity.getDiscountRate());
        vo.setPriceDescription(entity.getPriceDescription());
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setExpireDate(entity.getExpireDate());
        vo.setIsCurrent(entity.getIsCurrent());
        vo.setIsPromotion(entity.getIsPromotion());
        vo.setPromotionDescription(entity.getPromotionDescription());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
