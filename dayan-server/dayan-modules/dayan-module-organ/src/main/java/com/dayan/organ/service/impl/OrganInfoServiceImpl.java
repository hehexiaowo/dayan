package com.dayan.organ.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.organ.dto.OrganInfoCreateDTO;
import com.dayan.organ.dto.OrganInfoQueryDTO;
import com.dayan.organ.dto.OrganInfoUpdateDTO;
import com.dayan.organ.entity.OrganInfo;
import com.dayan.organ.mapper.OrganInfoMapper;
import com.dayan.organ.service.OrganInfoService;
import com.dayan.organ.vo.OrganInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 组织信息（公司/分公司）服务实现。
 */
@Service
@RequiredArgsConstructor
public class OrganInfoServiceImpl implements OrganInfoService {

    private final OrganInfoMapper organInfoMapper;

    @Override
    public PageResult<OrganInfoVO> page(OrganInfoQueryDTO query) {
        LambdaQueryWrapper<OrganInfo> wrapper = new LambdaQueryWrapper<OrganInfo>()
                .orderByDesc(OrganInfo::getCreatedAt);
        if (query.getOrganCode() != null && !query.getOrganCode().isEmpty()) {
            wrapper.eq(OrganInfo::getOrganCode, query.getOrganCode());
        }
        if (query.getFullName() != null && !query.getFullName().isEmpty()) {
            wrapper.like(OrganInfo::getFullName, query.getFullName());
        }
        if (query.getOrganType() != null) {
            wrapper.eq(OrganInfo::getOrganType, query.getOrganType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(OrganInfo::getStatus, query.getStatus());
        }
        Page<OrganInfo> page = organInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<OrganInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public OrganInfoVO getDetail(String organCode) {
        OrganInfo organ = selectByCode(organCode);
        return toVO(organ);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(OrganInfoCreateDTO dto) {
        // 信用代码唯一校验（若提供）
        if (dto.getUnifiedCreditCode() != null && !dto.getUnifiedCreditCode().isEmpty()) {
            Long count = organInfoMapper.selectCount(new LambdaQueryWrapper<OrganInfo>()
                    .eq(OrganInfo::getUnifiedCreditCode, dto.getUnifiedCreditCode()));
            if (count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "统一社会信用代码已存在");
            }
        }
        OrganInfo entity = new OrganInfo();
        entity.setOrganCode(generateOrganCode());
        entity.setFullName(dto.getFullName());
        entity.setShortName(dto.getShortName());
        entity.setOrganType(dto.getOrganType() == null ? 1 : dto.getOrganType());
        entity.setUnifiedCreditCode(dto.getUnifiedCreditCode());
        entity.setLegalPerson(dto.getLegalPerson());
        entity.setRegisteredCapital(dto.getRegisteredCapital());
        entity.setBusinessScope(dto.getBusinessScope());
        entity.setProvinceCode(dto.getProvinceCode());
        entity.setCityCode(dto.getCityCode());
        entity.setDistrictCode(dto.getDistrictCode());
        entity.setAddress(dto.getAddress());
        entity.setContactPerson(dto.getContactPerson());
        entity.setContactPhone(dto.getContactPhone());
        entity.setContactEmail(dto.getContactEmail());
        entity.setLogoUrl(dto.getLogoUrl());
        entity.setWebsite(dto.getWebsite());
        entity.setDescription(dto.getDescription());
        entity.setStatus(1);
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setRemark(dto.getRemark());
        organInfoMapper.insert(entity);
        return entity.getOrganCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String organCode, OrganInfoUpdateDTO dto) {
        OrganInfo existing = selectByCode(organCode);
        OrganInfo update = new OrganInfo();
        update.setId(existing.getId());
        if (dto.getFullName() != null) update.setFullName(dto.getFullName());
        if (dto.getShortName() != null) update.setShortName(dto.getShortName());
        if (dto.getOrganType() != null) update.setOrganType(dto.getOrganType());
        if (dto.getUnifiedCreditCode() != null) update.setUnifiedCreditCode(dto.getUnifiedCreditCode());
        if (dto.getLegalPerson() != null) update.setLegalPerson(dto.getLegalPerson());
        if (dto.getRegisteredCapital() != null) update.setRegisteredCapital(dto.getRegisteredCapital());
        if (dto.getBusinessScope() != null) update.setBusinessScope(dto.getBusinessScope());
        if (dto.getProvinceCode() != null) update.setProvinceCode(dto.getProvinceCode());
        if (dto.getCityCode() != null) update.setCityCode(dto.getCityCode());
        if (dto.getDistrictCode() != null) update.setDistrictCode(dto.getDistrictCode());
        if (dto.getAddress() != null) update.setAddress(dto.getAddress());
        if (dto.getContactPerson() != null) update.setContactPerson(dto.getContactPerson());
        if (dto.getContactPhone() != null) update.setContactPhone(dto.getContactPhone());
        if (dto.getContactEmail() != null) update.setContactEmail(dto.getContactEmail());
        if (dto.getLogoUrl() != null) update.setLogoUrl(dto.getLogoUrl());
        if (dto.getWebsite() != null) update.setWebsite(dto.getWebsite());
        if (dto.getDescription() != null) update.setDescription(dto.getDescription());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        organInfoMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String organCode) {
        organInfoMapper.delete(new LambdaQueryWrapper<OrganInfo>()
                .eq(OrganInfo::getOrganCode, organCode));
    }

    private OrganInfo selectByCode(String organCode) {
        OrganInfo organ = organInfoMapper.selectOne(new LambdaQueryWrapper<OrganInfo>()
                .eq(OrganInfo::getOrganCode, organCode).last("LIMIT 1"));
        if (organ == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组织不存在: " + organCode);
        }
        return organ;
    }

    /** 简易编码生成：OR + 时间戳后 5 位 + 随机 3 位（P1 简化，正式用 CodeGenerator） */
    private String generateOrganCode() {
        long ts = System.currentTimeMillis() % 100000L;
        int rand = (int) (Math.random() * 1000);
        return String.format("OR%05d%03d", ts, rand);
    }

    private OrganInfoVO toVO(OrganInfo entity) {
        OrganInfoVO vo = new OrganInfoVO();
        vo.setId(entity.getId());
        vo.setOrganCode(entity.getOrganCode());
        vo.setFullName(entity.getFullName());
        vo.setShortName(entity.getShortName());
        vo.setOrganType(entity.getOrganType());
        vo.setUnifiedCreditCode(entity.getUnifiedCreditCode());
        vo.setLegalPerson(entity.getLegalPerson());
        vo.setRegisteredCapital(entity.getRegisteredCapital());
        vo.setEstablishDate(entity.getEstablishDate());
        vo.setBusinessScope(entity.getBusinessScope());
        vo.setProvinceCode(entity.getProvinceCode());
        vo.setCityCode(entity.getCityCode());
        vo.setDistrictCode(entity.getDistrictCode());
        vo.setAddress(entity.getAddress());
        vo.setContactPerson(entity.getContactPerson());
        vo.setContactPhone(entity.getContactPhone());
        vo.setContactEmail(entity.getContactEmail());
        vo.setLogoUrl(entity.getLogoUrl());
        vo.setWebsite(entity.getWebsite());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setSortOrder(entity.getSortOrder());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
