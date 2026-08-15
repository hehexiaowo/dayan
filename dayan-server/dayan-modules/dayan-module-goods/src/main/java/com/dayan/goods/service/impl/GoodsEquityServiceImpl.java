package com.dayan.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.goods.dto.GoodsEquitySaveDTO;
import com.dayan.goods.entity.GoodsEquity;
import com.dayan.goods.entity.GoodsServiceItemRel;
import com.dayan.goods.entity.ServiceItem;
import com.dayan.goods.mapper.GoodsEquityMapper;
import com.dayan.goods.mapper.GoodsServiceItemRelMapper;
import com.dayan.goods.mapper.ServiceItemMapper;
import com.dayan.goods.model.HolderRule;
import com.dayan.goods.model.NetworkScope;
import com.dayan.goods.model.RightsJson;
import com.dayan.goods.service.GoodsEquityService;
import com.dayan.goods.vo.GoodsEquityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsEquityServiceImpl implements GoodsEquityService {

    private final GoodsEquityMapper goodsEquityMapper;
    private final GoodsServiceItemRelMapper relMapper;
    private final ServiceItemMapper serviceItemMapper;

    @Override
    public GoodsEquityVO getByGoodsCode(String goodsCode) {
        LambdaQueryWrapper<GoodsEquity> wrapper = new LambdaQueryWrapper<GoodsEquity>()
                .eq(GoodsEquity::getGoodsCode, goodsCode);
        GoodsEquity entity = goodsEquityMapper.selectOne(wrapper);
        if (entity == null) {
            return null;
        }
        return toVO(entity, listRelsByGoodsCode(goodsCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(GoodsEquitySaveDTO dto) {
        // 0. 权益内容校验（构成规则 / 折扣率 / 使用规则）
        validateHolderRule(dto.getPersonCount(), dto.getHolderRule());
        if (dto.getServiceItems() != null) {
            for (GoodsEquitySaveDTO.ServiceItemRelDTO relDto : dto.getServiceItems()) {
                validateRel(relDto);
            }
        }

        // 1. UPSERT goods_equity（1:1 by goodsCode）
        LambdaQueryWrapper<GoodsEquity> wrapper = new LambdaQueryWrapper<GoodsEquity>()
                .eq(GoodsEquity::getGoodsCode, dto.getGoodsCode());
        GoodsEquity existing = goodsEquityMapper.selectOne(wrapper);

        if (existing == null) {
            existing = new GoodsEquity();
            existing.setGoodsCode(dto.getGoodsCode());
            existing.setPersonCount(dto.getPersonCount() != null ? dto.getPersonCount() : 1);
            existing.setValidityType(dto.getValidityType() != null ? dto.getValidityType() : 1);
            existing.setHolderRule(RightsJson.write(dto.getHolderRule()));
            existing.setShareMode(dto.getShareMode() != null ? dto.getShareMode() : 1);
            existing.setValidDays(dto.getValidDays() != null ? dto.getValidDays() : 365);
            existing.setShelfLifeDays(dto.getShelfLifeDays() != null ? dto.getShelfLifeDays() : 730);
            existing.setMaxTransferable(dto.getMaxTransferable() != null ? dto.getMaxTransferable() : 0);
            existing.setDescription(dto.getDescription());
            existing.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
            existing.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
            goodsEquityMapper.insert(existing);
        } else {
            if (dto.getPersonCount() != null) existing.setPersonCount(dto.getPersonCount());
            if (dto.getValidityType() != null) existing.setValidityType(dto.getValidityType());
            existing.setHolderRule(RightsJson.write(dto.getHolderRule()));
            if (dto.getShareMode() != null) existing.setShareMode(dto.getShareMode());
            if (dto.getValidDays() != null) existing.setValidDays(dto.getValidDays());
            if (dto.getShelfLifeDays() != null) existing.setShelfLifeDays(dto.getShelfLifeDays());
            if (dto.getMaxTransferable() != null) existing.setMaxTransferable(dto.getMaxTransferable());
            if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
            if (dto.getSortOrder() != null) existing.setSortOrder(dto.getSortOrder());
            if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
            goodsEquityMapper.updateById(existing);
        }

        // 2. 先删后插 goods_service_item_rel（N:M，物理删除避免逻辑删除+唯一键冲突）
        relMapper.physicalDeleteByGoodsCode(dto.getGoodsCode());

        if (dto.getServiceItems() != null && !dto.getServiceItems().isEmpty()) {
            // 批量校验 itemCode 存在性 + 批量查 itemName
            Set<String> itemCodes = dto.getServiceItems().stream()
                    .map(GoodsEquitySaveDTO.ServiceItemRelDTO::getItemCode)
                    .collect(Collectors.toSet());
            LambdaQueryWrapper<ServiceItem> itemWrapper = new LambdaQueryWrapper<ServiceItem>()
                    .in(ServiceItem::getItemCode, itemCodes);
            List<ServiceItem> items = serviceItemMapper.selectList(itemWrapper);
            Set<String> foundCodes = items.stream()
                    .map(ServiceItem::getItemCode)
                    .collect(Collectors.toSet());
            for (String code : itemCodes) {
                if (!foundCodes.contains(code)) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "服务项目不存在：" + code);
                }
            }

            for (GoodsEquitySaveDTO.ServiceItemRelDTO relDto : dto.getServiceItems()) {
                GoodsServiceItemRel rel = new GoodsServiceItemRel();
                rel.setGoodsCode(dto.getGoodsCode());
                rel.setItemCode(relDto.getItemCode());
                rel.setQuantity(relDto.getQuantity());
                rel.setQuotaType(relDto.getQuotaType() != null ? relDto.getQuotaType() : 2);
                rel.setNetworkScope(relDto.getNetworkScope() != null && relDto.getNetworkScope().isCustom()
                        ? RightsJson.write(relDto.getNetworkScope()) : null);
                rel.setAdmissionGuaranteed(nz(relDto.getAdmissionGuaranteed()));
                rel.setAdmissionPriority(nz(relDto.getAdmissionPriority()));
                rel.setAdmissionDiscount(nz(relDto.getAdmissionDiscount()));
                rel.setDiscountRate(relDto.getDiscountRate());
                rel.setUsageRule(RightsJson.write(relDto.getUsageRule()));
                rel.setSortOrder(relDto.getSortOrder() != null ? relDto.getSortOrder() : 0);
                relMapper.insert(rel);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String goodsCode) {
        // 物理删除 rel + equity（避免逻辑删除+唯一键冲突）
        relMapper.physicalDeleteByGoodsCode(goodsCode);
        goodsEquityMapper.physicalDeleteByGoodsCode(goodsCode);
    }

    @Override
    public GoodsEquity requireByGoodsCode(String goodsCode) {
        LambdaQueryWrapper<GoodsEquity> wrapper = new LambdaQueryWrapper<GoodsEquity>()
                .eq(GoodsEquity::getGoodsCode, goodsCode);
        GoodsEquity entity = goodsEquityMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权益商品配置不存在：" + goodsCode);
        }
        return entity;
    }

    @Override
    public List<GoodsEquityVO.ServiceItemRelVO> listRelsByGoodsCode(String goodsCode) {
        // 查 rel
        LambdaQueryWrapper<GoodsServiceItemRel> relWrapper = new LambdaQueryWrapper<GoodsServiceItemRel>()
                .eq(GoodsServiceItemRel::getGoodsCode, goodsCode)
                .orderByAsc(GoodsServiceItemRel::getSortOrder)
                .orderByAsc(GoodsServiceItemRel::getCreatedAt);
        List<GoodsServiceItemRel> rels = relMapper.selectList(relWrapper);
        if (rels.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查 service_item 防止 N+1
        Set<String> itemCodes = rels.stream()
                .map(GoodsServiceItemRel::getItemCode)
                .collect(Collectors.toSet());
        LambdaQueryWrapper<ServiceItem> itemWrapper = new LambdaQueryWrapper<ServiceItem>()
                .in(ServiceItem::getItemCode, itemCodes);
        List<ServiceItem> items = serviceItemMapper.selectList(itemWrapper);
        Map<String, ServiceItem> itemMap = items.stream()
                .collect(Collectors.toMap(ServiceItem::getItemCode, i -> i));

        return rels.stream().map(rel -> {
            GoodsEquityVO.ServiceItemRelVO vo = new GoodsEquityVO.ServiceItemRelVO();
            vo.setId(rel.getId());
            vo.setGoodsCode(rel.getGoodsCode());
            vo.setItemCode(rel.getItemCode());
            ServiceItem item = itemMap.get(rel.getItemCode());
            if (item != null) {
                vo.setItemName(item.getItemName());
                vo.setItemCategory(item.getItemCategory());
                vo.setItemSubtype(item.getItemSubtype());
            }
            vo.setQuantity(rel.getQuantity());
            vo.setQuotaType(rel.getQuotaType());
            vo.setNetworkScope(RightsJson.read(rel.getNetworkScope(), NetworkScope.class));
            vo.setAdmissionGuaranteed(rel.getAdmissionGuaranteed());
            vo.setAdmissionPriority(rel.getAdmissionPriority());
            vo.setAdmissionDiscount(rel.getAdmissionDiscount());
            vo.setDiscountRate(rel.getDiscountRate());
            vo.setUsageRule(RightsJson.read(rel.getUsageRule(), com.dayan.goods.model.UsageRule.class));
            vo.setSortOrder(rel.getSortOrder());
            vo.setCreatedAt(rel.getCreatedAt());
            return vo;
        }).toList();
    }

    private GoodsEquityVO toVO(GoodsEquity entity, List<GoodsEquityVO.ServiceItemRelVO> serviceItems) {
        GoodsEquityVO vo = new GoodsEquityVO();
        vo.setId(entity.getId());
        vo.setGoodsCode(entity.getGoodsCode());
        vo.setPersonCount(entity.getPersonCount());
        vo.setValidityType(entity.getValidityType());
        vo.setHolderRule(RightsJson.read(entity.getHolderRule(), HolderRule.class));
        vo.setShareMode(entity.getShareMode());
        vo.setValidDays(entity.getValidDays());
        vo.setShelfLifeDays(entity.getShelfLifeDays());
        vo.setMaxTransferable(entity.getMaxTransferable());
        vo.setDescription(entity.getDescription());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setServiceItems(serviceItems);
        return vo;
    }

    // ====== 权益内容校验 ======

    /** 权益人构成规则校验：self 固定1，spouse 0/1，parent 0~4，构成之和=personCount */
    private void validateHolderRule(Integer personCount, HolderRule rule) {
        if (rule == null) {
            return; // 未配置构成规则（旧数据/极简商品），仅按 personCount 上限控制
        }
        int self = rule.getSelf() == null ? 1 : rule.getSelf();
        int spouse = rule.getSpouse() == null ? 0 : rule.getSpouse();
        int parent = rule.getParent() == null ? 0 : rule.getParent();
        if (self != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "权益人构成：本人席位固定为 1");
        }
        if (spouse < 0 || spouse > 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "权益人构成：配偶席位只能为 0 或 1");
        }
        if (parent < 0 || parent > 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "权益人构成：父母席位只能在 0~4 之间（双方父母最多4人）");
        }
        if (personCount != null && personCount != self + spouse + parent) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "权益人构成之和（" + (self + spouse + parent) + "）须等于使用人人数（" + personCount + "）");
        }
    }

    /** 服务项目关联行校验：折扣率范围 + 折扣率联动优惠权标记 + 使用规则基本值域 */
    private void validateRel(GoodsEquitySaveDTO.ServiceItemRelDTO relDto) {
        BigDecimal rate = relDto.getDiscountRate();
        if (rate != null && (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "折扣率须在 (0,100] 之间（90.00=门市价9折）: " + relDto.getItemCode());
        }
        if (rate != null && nz(relDto.getAdmissionDiscount()) == 0) {
            // 填了折扣率却没勾优惠权 → 自动补勾，避免展示口径不一致
            relDto.setAdmissionDiscount(1);
        }
        com.dayan.goods.model.UsageRule usage = relDto.getUsageRule();
        if (usage != null) {
            if (usage.getMaxNightsPerUse() != null && usage.getMaxNightsPerUse() < 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "使用规则：每次最多晚数须≥1: " + relDto.getItemCode());
            }
            if (usage.getMaxRoomsPerUse() != null && usage.getMaxRoomsPerUse() < 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "使用规则：每次房间数须≥1: " + relDto.getItemCode());
            }
            if (usage.getMaxGuestsPerUse() != null && usage.getMaxGuestsPerUse() < 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "使用规则：每间可住人数须≥1: " + relDto.getItemCode());
            }
            if (usage.getAdvanceBookDays() != null && usage.getAdvanceBookDays() < 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "使用规则：提前预订天数不能为负: " + relDto.getItemCode());
            }
        }
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
