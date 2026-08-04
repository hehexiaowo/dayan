package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkCarePriceCreateDTO;
import com.dayan.park.dto.ParkCarePriceQueryDTO;
import com.dayan.park.dto.ParkCarePriceUpdateDTO;
import com.dayan.park.entity.ParkCarePrice;
import com.dayan.park.mapper.ParkCarePriceMapper;
import com.dayan.park.service.ParkCarePriceService;
import com.dayan.park.vo.ParkCarePriceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 照护价格（park_care_price）服务实现。
 *
 * <p>校验规则：
 * <ul>
 *   <li>effectiveDate &lt; expireDate</li>
 *   <li>isCurrent=1 在同 careTypeCode 下唯一（设当前价时自动置 0 其余）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkCarePriceServiceImpl implements ParkCarePriceService {

    private static final int IS_CURRENT_YES = 1;

    private final ParkCarePriceMapper carePriceMapper;

    @Override
    public PageResult<ParkCarePriceVO> page(ParkCarePriceQueryDTO query) {
        LambdaQueryWrapper<ParkCarePrice> wrapper = buildWrapper(query);
        Page<ParkCarePrice> page = carePriceMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkCarePriceVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkCarePriceVO> listByCareType(String parkCode, String careTypeCode) {
        return carePriceMapper.selectList(new LambdaQueryWrapper<ParkCarePrice>()
                .eq(ParkCarePrice::getParkCode, parkCode)
                .eq(ParkCarePrice::getCareTypeCode, careTypeCode)
                .orderByAsc(ParkCarePrice::getSortOrder)
                .orderByDesc(ParkCarePrice::getIsCurrent)
                .orderByAsc(ParkCarePrice::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkCarePriceVO getDetail(Long id) {
        return toVO(requirePrice(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkCarePriceCreateDTO dto) {
        validateDateRange(dto.getEffectiveDate(), dto.getExpireDate());
        Integer isCurrent = (dto.getIsCurrent() != null && dto.getIsCurrent() == IS_CURRENT_YES)
                ? IS_CURRENT_YES : 0;
        if (isCurrent == IS_CURRENT_YES) {
            clearOtherCurrent(dto.getCareTypeCode(), null);
        }

        ParkCarePrice entity = new ParkCarePrice();
        entity.setParkCode(dto.getParkCode());
        entity.setCareTypeCode(dto.getCareTypeCode());
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
        carePriceMapper.insert(entity);
        log.info("创建照护价格成功: parkCode={}, careTypeCode={}, id={}",
                dto.getParkCode(), dto.getCareTypeCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkCarePriceUpdateDTO dto) {
        ParkCarePrice existing = requirePrice(id);
        LocalDate effective = dto.getEffectiveDate() != null ? dto.getEffectiveDate() : existing.getEffectiveDate();
        LocalDate expire = dto.getExpireDate() != null ? dto.getExpireDate() : existing.getExpireDate();
        validateDateRange(effective, expire);

        ParkCarePrice update = new ParkCarePrice();
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
                clearOtherCurrent(existing.getCareTypeCode(), id);
            }
            update.setIsCurrent(isCurrent);
        }
        carePriceMapper.updateById(update);
        log.info("更新照护价格成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkCarePrice existing = requirePrice(id);
        carePriceMapper.deleteById(existing.getId());
        log.info("删除照护价格成功: id={}", id);
    }

    // ====== 内部方法 ======

    private void validateDateRange(LocalDate effectiveDate, LocalDate expireDate) {
        if (effectiveDate != null && expireDate != null && !effectiveDate.isBefore(expireDate)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "生效日期(effectiveDate)必须早于失效日期(expireDate)");
        }
    }

    private void clearOtherCurrent(String careTypeCode, Long excludeId) {
        LambdaUpdateWrapper<ParkCarePrice> wrapper = new LambdaUpdateWrapper<ParkCarePrice>()
                .eq(ParkCarePrice::getCareTypeCode, careTypeCode)
                .eq(ParkCarePrice::getIsCurrent, IS_CURRENT_YES)
                .set(ParkCarePrice::getIsCurrent, 0);
        if (excludeId != null) {
            wrapper.ne(ParkCarePrice::getId, excludeId);
        }
        carePriceMapper.update(null, wrapper);
    }

    private LambdaQueryWrapper<ParkCarePrice> buildWrapper(ParkCarePriceQueryDTO query) {
        LambdaQueryWrapper<ParkCarePrice> wrapper = new LambdaQueryWrapper<ParkCarePrice>()
                .orderByAsc(ParkCarePrice::getSortOrder)
                .orderByDesc(ParkCarePrice::getIsCurrent)
                .orderByAsc(ParkCarePrice::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkCarePrice::getParkCode, query.getParkCode());
        }
        if (query.getCareTypeCode() != null && !query.getCareTypeCode().isEmpty()) {
            wrapper.eq(ParkCarePrice::getCareTypeCode, query.getCareTypeCode());
        }
        if (query.getPriceType() != null) {
            wrapper.eq(ParkCarePrice::getPriceType, query.getPriceType());
        }
        if (query.getIsCurrent() != null) {
            wrapper.eq(ParkCarePrice::getIsCurrent, query.getIsCurrent());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkCarePrice::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkCarePrice requirePrice(Long id) {
        ParkCarePrice entity = carePriceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "照护价格不存在: id=" + id);
        }
        return entity;
    }

    private ParkCarePriceVO toVO(ParkCarePrice entity) {
        ParkCarePriceVO vo = new ParkCarePriceVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setCareTypeCode(entity.getCareTypeCode());
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
