package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkPricingCreateDTO;
import com.dayan.park.dto.ParkPricingQueryDTO;
import com.dayan.park.dto.ParkPricingUpdateDTO;
import com.dayan.park.entity.ParkPricing;
import com.dayan.park.entity.ParkPricingItem;
import com.dayan.park.mapper.ParkPricingItemMapper;
import com.dayan.park.mapper.ParkPricingMapper;
import com.dayan.park.service.ParkPricingService;
import com.dayan.park.vo.ParkPricingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构统一定价方案服务实现。
 *
 * <p>校验规则：
 * <ul>
 *   <li>effectiveDate &lt; expireDate</li>
 *   <li>isCurrent=1 在同 park+charge_type+ref_code+billing_cycle 下唯一（设当前价时自动置 0 其余）</li>
 * </ul>
 *
 * <p>每条 pricing 创建时自动创建 1 条 pricing_item（主行，冗余 ref_type + ref_code）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkPricingServiceImpl implements ParkPricingService {

    private static final int IS_CURRENT_YES = 1;

    private final ParkPricingMapper pricingMapper;
    private final ParkPricingItemMapper pricingItemMapper;

    @Override
    public PageResult<ParkPricingVO> page(ParkPricingQueryDTO query) {
        LambdaQueryWrapper<ParkPricing> wrapper = buildWrapper(query);
        Page<ParkPricing> page = pricingMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkPricingVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkPricingVO> listByRef(String parkCode, String refType, String refCode) {
        return pricingMapper.selectList(new LambdaQueryWrapper<ParkPricing>()
                        .eq(ParkPricing::getParkCode, parkCode)
                        .eq(ParkPricing::getRefType, refType)
                        .eq(ParkPricing::getRefCode, refCode)
                        .orderByAsc(ParkPricing::getSortOrder)
                        .orderByDesc(ParkPricing::getIsCurrent)
                        .orderByAsc(ParkPricing::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ParkPricingVO> listByChargeType(String parkCode, Integer chargeType) {
        return pricingMapper.selectList(new LambdaQueryWrapper<ParkPricing>()
                        .eq(ParkPricing::getParkCode, parkCode)
                        .eq(ParkPricing::getChargeType, chargeType)
                        .orderByAsc(ParkPricing::getSortOrder)
                        .orderByDesc(ParkPricing::getIsCurrent)
                        .orderByAsc(ParkPricing::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkPricingVO getDetail(Long id) {
        return toVO(requirePricing(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkPricingCreateDTO dto) {
        validateDateRange(dto.getEffectiveDate(), dto.getExpireDate());

        Integer isCurrent = (dto.getIsCurrent() != null && dto.getIsCurrent() == IS_CURRENT_YES)
                ? IS_CURRENT_YES : 0;
        if (isCurrent == IS_CURRENT_YES) {
            clearOtherCurrent(dto.getParkCode(), dto.getChargeType(), dto.getRefCode(),
                    dto.getBillingCycle(), null);
        }

        ParkPricing entity = new ParkPricing();
        entity.setParkCode(dto.getParkCode());
        entity.setPlanName(dto.getPlanName());
        entity.setChargeType(dto.getChargeType());
        entity.setRefType(dto.getRefType());
        entity.setRefCode(dto.getRefCode());
        entity.setRefName(dto.getRefName());
        entity.setBillingCycle(dto.getBillingCycle());
        entity.setPriceUnit(dto.getPriceUnit());
        entity.setOriginalPrice(dto.getOriginalPrice());
        entity.setSalePrice(dto.getSalePrice());
        entity.setDiscountRate(dto.getDiscountRate());
        entity.setPriceDescription(dto.getPriceDescription());
        entity.setIncludesItems(dto.getIncludesItems());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setExpireDate(dto.getExpireDate());
        entity.setIsCurrent(isCurrent);
        entity.setIsPromotion(dto.getIsPromotion() == null ? 0 : dto.getIsPromotion());
        entity.setPromotionDescription(dto.getPromotionDescription());
        entity.setPriceChangeReason(dto.getPriceChangeReason());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        pricingMapper.insert(entity);

        // 自动创建 pricing_item 主行
        ParkPricingItem item = new ParkPricingItem();
        item.setPricingId(entity.getId());
        item.setParkCode(dto.getParkCode());
        item.setItemType(dto.getRefType());
        item.setItemCode(dto.getRefCode());
        item.setItemName(dto.getRefName());
        item.setSortOrder(0);
        pricingItemMapper.insert(item);

        log.info("创建定价方案成功: parkCode={}, chargeType={}, refType={}, refCode={}, id={}",
                dto.getParkCode(), dto.getChargeType(), dto.getRefType(), dto.getRefCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkPricingUpdateDTO dto) {
        ParkPricing existing = requirePricing(id);
        LocalDate effective = dto.getEffectiveDate() != null ? dto.getEffectiveDate() : existing.getEffectiveDate();
        LocalDate expire = dto.getExpireDate() != null ? dto.getExpireDate() : existing.getExpireDate();
        validateDateRange(effective, expire);

        ParkPricing update = new ParkPricing();
        update.setId(existing.getId());
        if (dto.getPlanName() != null) update.setPlanName(dto.getPlanName());
        if (dto.getRefName() != null) update.setRefName(dto.getRefName());
        if (dto.getBillingCycle() != null) update.setBillingCycle(dto.getBillingCycle());
        if (dto.getPriceUnit() != null) update.setPriceUnit(dto.getPriceUnit());
        if (dto.getOriginalPrice() != null) update.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getSalePrice() != null) update.setSalePrice(dto.getSalePrice());
        if (dto.getDiscountRate() != null) update.setDiscountRate(dto.getDiscountRate());
        if (dto.getPriceDescription() != null) update.setPriceDescription(dto.getPriceDescription());
        if (dto.getIncludesItems() != null) update.setIncludesItems(dto.getIncludesItems());
        if (dto.getEffectiveDate() != null) update.setEffectiveDate(dto.getEffectiveDate());
        if (dto.getExpireDate() != null) update.setExpireDate(dto.getExpireDate());
        if (dto.getIsPromotion() != null) update.setIsPromotion(dto.getIsPromotion());
        if (dto.getPromotionDescription() != null) update.setPromotionDescription(dto.getPromotionDescription());
        if (dto.getPriceChangeReason() != null) update.setPriceChangeReason(dto.getPriceChangeReason());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getIsCurrent() != null) {
            int isCurrent = (dto.getIsCurrent() == IS_CURRENT_YES) ? IS_CURRENT_YES : 0;
            if (isCurrent == IS_CURRENT_YES) {
                clearOtherCurrent(existing.getParkCode(), existing.getChargeType(),
                        existing.getRefCode(), existing.getBillingCycle(), id);
            }
            update.setIsCurrent(isCurrent);
        }
        pricingMapper.updateById(update);
        log.info("更新定价方案成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkPricing existing = requirePricing(id);
        // 级联删 pricing_item
        pricingItemMapper.delete(new LambdaQueryWrapper<ParkPricingItem>()
                .eq(ParkPricingItem::getPricingId, id));
        pricingMapper.deleteById(existing.getId());
        log.info("删除定价方案成功: id={}", id);
    }

    // ====== 内部方法 ======

    private void validateDateRange(LocalDate effectiveDate, LocalDate expireDate) {
        if (effectiveDate != null && expireDate != null && !effectiveDate.isBefore(expireDate)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "生效日期(effectiveDate)必须早于失效日期(expireDate)");
        }
    }

    /**
     * 清除同 park+charge_type+ref_code+billing_cycle 下其他定价的当前价标记。
     * billingCycle 为 null 时按 0 处理（一次性费用场景）。
     */
    private void clearOtherCurrent(String parkCode, Integer chargeType, String refCode,
                                   Integer billingCycle, Long excludeId) {
        LambdaUpdateWrapper<ParkPricing> wrapper = new LambdaUpdateWrapper<ParkPricing>()
                .eq(ParkPricing::getParkCode, parkCode)
                .eq(ParkPricing::getChargeType, chargeType)
                .eq(ParkPricing::getRefCode, refCode)
                .eq(ParkPricing::getBillingCycle, billingCycle)
                .eq(ParkPricing::getIsCurrent, IS_CURRENT_YES)
                .set(ParkPricing::getIsCurrent, 0);
        if (excludeId != null) {
            wrapper.ne(ParkPricing::getId, excludeId);
        }
        pricingMapper.update(null, wrapper);
    }

    private LambdaQueryWrapper<ParkPricing> buildWrapper(ParkPricingQueryDTO query) {
        LambdaQueryWrapper<ParkPricing> wrapper = new LambdaQueryWrapper<ParkPricing>()
                .orderByAsc(ParkPricing::getSortOrder)
                .orderByDesc(ParkPricing::getIsCurrent)
                .orderByAsc(ParkPricing::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkPricing::getParkCode, query.getParkCode());
        }
        if (query.getChargeType() != null) {
            wrapper.eq(ParkPricing::getChargeType, query.getChargeType());
        }
        if (query.getRefType() != null && !query.getRefType().isEmpty()) {
            wrapper.eq(ParkPricing::getRefType, query.getRefType());
        }
        if (query.getRefCode() != null && !query.getRefCode().isEmpty()) {
            wrapper.eq(ParkPricing::getRefCode, query.getRefCode());
        }
        if (query.getBillingCycle() != null) {
            wrapper.eq(ParkPricing::getBillingCycle, query.getBillingCycle());
        }
        if (query.getIsCurrent() != null) {
            wrapper.eq(ParkPricing::getIsCurrent, query.getIsCurrent());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkPricing::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkPricing requirePricing(Long id) {
        ParkPricing entity = pricingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "定价方案不存在: id=" + id);
        }
        return entity;
    }

    private ParkPricingVO toVO(ParkPricing entity) {
        ParkPricingVO vo = new ParkPricingVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setPlanName(entity.getPlanName());
        vo.setChargeType(entity.getChargeType());
        vo.setRefType(entity.getRefType());
        vo.setRefCode(entity.getRefCode());
        vo.setRefName(entity.getRefName());
        vo.setBillingCycle(entity.getBillingCycle());
        vo.setPriceUnit(entity.getPriceUnit());
        vo.setOriginalPrice(entity.getOriginalPrice());
        vo.setSalePrice(entity.getSalePrice());
        vo.setDiscountRate(entity.getDiscountRate());
        vo.setPriceDescription(entity.getPriceDescription());
        vo.setIncludesItems(entity.getIncludesItems());
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setExpireDate(entity.getExpireDate());
        vo.setIsCurrent(entity.getIsCurrent());
        vo.setIsPromotion(entity.getIsPromotion());
        vo.setPromotionDescription(entity.getPromotionDescription());
        vo.setPriceChangeReason(entity.getPriceChangeReason());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
