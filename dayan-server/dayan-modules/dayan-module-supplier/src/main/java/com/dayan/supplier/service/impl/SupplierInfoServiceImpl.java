package com.dayan.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.supplier.dto.SupplierAuditDTO;
import com.dayan.supplier.dto.SupplierInfoCreateDTO;
import com.dayan.supplier.dto.SupplierInfoQueryDTO;
import com.dayan.supplier.dto.SupplierInfoUpdateDTO;
import com.dayan.supplier.entity.SupplierInfo;
import com.dayan.supplier.mapper.SupplierInfoMapper;
import com.dayan.supplier.service.SupplierInfoService;
import com.dayan.supplier.vo.SupplierInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 供应商信息服务实现。
 *
 * <p>审核流程：status 1=待审核 / 2=已通过 / 3=已驳回。新建默认 status=1，
 * 审核（{@link #audit}）仅允许当前 status=1 时流转。
 *
 * <p>信用代码唯一校验：{@code unifiedCreditCode} 在同 {@code supplierType} 内唯一。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierInfoServiceImpl implements SupplierInfoService {

    /** 供应商编码前缀 */
    private static final String CODE_PREFIX = "SP";
    /** 初始状态：待审核 */
    private static final int STATUS_PENDING_AUDIT = 1;
    /** 审核状态：通过 */
    private static final int AUDIT_PASS = 2;
    /** 审核状态：驳回 */
    private static final int AUDIT_REJECT = 3;

    private final SupplierInfoMapper supplierInfoMapper;
    private final CodeGenerator codeGenerator;

    @Override
    public PageResult<SupplierInfoVO> page(SupplierInfoQueryDTO query) {
        LambdaQueryWrapper<SupplierInfo> wrapper = new LambdaQueryWrapper<SupplierInfo>()
                .eq(query.getSupplierCode() != null && !query.getSupplierCode().isEmpty(),
                        SupplierInfo::getSupplierCode, query.getSupplierCode())
                .like(query.getFullName() != null && !query.getFullName().isEmpty(),
                        SupplierInfo::getFullName, query.getFullName())
                .like(query.getShortName() != null && !query.getShortName().isEmpty(),
                        SupplierInfo::getShortName, query.getShortName())
                .eq(query.getSupplierType() != null,
                        SupplierInfo::getSupplierType, query.getSupplierType())
                .eq(query.getUnifiedCreditCode() != null && !query.getUnifiedCreditCode().isEmpty(),
                        SupplierInfo::getUnifiedCreditCode, query.getUnifiedCreditCode())
                .eq(query.getStatus() != null, SupplierInfo::getStatus, query.getStatus())
                .eq(query.getAuditStatus() != null, SupplierInfo::getAuditStatus, query.getAuditStatus())
                .orderByDesc(SupplierInfo::getCreatedAt);
        Page<SupplierInfo> page = supplierInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SupplierInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public SupplierInfoVO getDetail(String supplierCode) {
        return toVO(requireSupplier(supplierCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SupplierInfoCreateDTO dto) {
        // 信用代码在同 supplierType 内唯一校验（若提供）
        if (dto.getUnifiedCreditCode() != null && !dto.getUnifiedCreditCode().isEmpty()) {
            checkCreditCodeUnique(dto.getUnifiedCreditCode(), dto.getSupplierType(), null);
        }

        String supplierCode = codeGenerator.generate(CODE_PREFIX);

        SupplierInfo entity = new SupplierInfo();
        entity.setSupplierCode(supplierCode);
        entity.setFullName(dto.getFullName());
        entity.setShortName(dto.getShortName());
        entity.setSupplierType(dto.getSupplierType());
        entity.setUnifiedCreditCode(dto.getUnifiedCreditCode());
        entity.setLegalPerson(dto.getLegalPerson());
        entity.setRegisteredCapital(dto.getRegisteredCapital());
        entity.setEstablishDate(dto.getEstablishDate());
        entity.setBusinessLicenseNo(dto.getBusinessLicenseNo());
        entity.setBusinessScope(dto.getBusinessScope());
        entity.setProvinceCode(dto.getProvinceCode());
        entity.setCityCode(dto.getCityCode());
        entity.setDistrictCode(dto.getDistrictCode());
        entity.setAddress(dto.getAddress());
        entity.setContactPerson(dto.getContactPerson());
        entity.setContactPhone(dto.getContactPhone());
        entity.setContactEmail(dto.getContactEmail());
        entity.setLogoUrl(dto.getLogoUrl());
        entity.setDescription(dto.getDescription());
        entity.setLicenseImage(dto.getLicenseImage());
        entity.setQualificationImage(dto.getQualificationImage());
        entity.setBankName(dto.getBankName());
        entity.setBankAccount(dto.getBankAccount());
        entity.setBankAccountName(dto.getBankAccountName());
        entity.setParkCount(0);
        entity.setCooperationStartDate(dto.getCooperationStartDate());
        entity.setCooperationEndDate(dto.getCooperationEndDate());
        entity.setCommissionRate(dto.getCommissionRate());
        entity.setStatus(STATUS_PENDING_AUDIT);
        entity.setAuditStatus(0);
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setRemark(dto.getRemark());

        supplierInfoMapper.insert(entity);
        log.info("创建供应商成功: supplierCode={}, fullName={}", supplierCode, dto.getFullName());
        return supplierCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String supplierCode, SupplierInfoUpdateDTO dto) {
        SupplierInfo existing = requireSupplier(supplierCode);
        SupplierInfo update = new SupplierInfo();
        update.setId(existing.getId());

        if (dto.getFullName() != null) update.setFullName(dto.getFullName());
        if (dto.getShortName() != null) update.setShortName(dto.getShortName());
        if (dto.getSupplierType() != null) update.setSupplierType(dto.getSupplierType());

        if (dto.getUnifiedCreditCode() != null) {
            // 信用代码在同 supplierType 内唯一校验（排除自身）
            Integer type = dto.getSupplierType() != null ? dto.getSupplierType() : existing.getSupplierType();
            checkCreditCodeUnique(dto.getUnifiedCreditCode(), type, supplierCode);
            update.setUnifiedCreditCode(dto.getUnifiedCreditCode());
        }
        if (dto.getLegalPerson() != null) update.setLegalPerson(dto.getLegalPerson());
        if (dto.getRegisteredCapital() != null) update.setRegisteredCapital(dto.getRegisteredCapital());
        if (dto.getEstablishDate() != null) update.setEstablishDate(dto.getEstablishDate());
        if (dto.getBusinessLicenseNo() != null) update.setBusinessLicenseNo(dto.getBusinessLicenseNo());
        if (dto.getBusinessScope() != null) update.setBusinessScope(dto.getBusinessScope());
        if (dto.getProvinceCode() != null) update.setProvinceCode(dto.getProvinceCode());
        if (dto.getCityCode() != null) update.setCityCode(dto.getCityCode());
        if (dto.getDistrictCode() != null) update.setDistrictCode(dto.getDistrictCode());
        if (dto.getAddress() != null) update.setAddress(dto.getAddress());
        if (dto.getContactPerson() != null) update.setContactPerson(dto.getContactPerson());
        if (dto.getContactPhone() != null) update.setContactPhone(dto.getContactPhone());
        if (dto.getContactEmail() != null) update.setContactEmail(dto.getContactEmail());
        if (dto.getLogoUrl() != null) update.setLogoUrl(dto.getLogoUrl());
        if (dto.getDescription() != null) update.setDescription(dto.getDescription());
        if (dto.getLicenseImage() != null) update.setLicenseImage(dto.getLicenseImage());
        if (dto.getQualificationImage() != null) update.setQualificationImage(dto.getQualificationImage());
        if (dto.getBankName() != null) update.setBankName(dto.getBankName());
        if (dto.getBankAccount() != null) update.setBankAccount(dto.getBankAccount());
        if (dto.getBankAccountName() != null) update.setBankAccountName(dto.getBankAccountName());
        if (dto.getCooperationStartDate() != null) update.setCooperationStartDate(dto.getCooperationStartDate());
        if (dto.getCooperationEndDate() != null) update.setCooperationEndDate(dto.getCooperationEndDate());
        if (dto.getCommissionRate() != null) update.setCommissionRate(dto.getCommissionRate());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        supplierInfoMapper.updateById(update);
        log.info("更新供应商成功: supplierCode={}", supplierCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String supplierCode) {
        requireSupplier(supplierCode);
        supplierInfoMapper.delete(new LambdaQueryWrapper<SupplierInfo>()
                .eq(SupplierInfo::getSupplierCode, supplierCode));
        log.info("删除供应商成功: supplierCode={}", supplierCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(SupplierAuditDTO dto) {
        SupplierInfo existing = requireSupplier(dto.getSupplierCode());
        Integer auditStatus = dto.getAuditStatus();
        if (auditStatus == null || (auditStatus != AUDIT_PASS && auditStatus != AUDIT_REJECT)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核状态非法（仅支持 2=通过 / 3=驳回）");
        }
        // 仅当前 status=1 待审核才能审核
        if (existing.getStatus() == null || existing.getStatus() != STATUS_PENDING_AUDIT) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "供应商当前状态不可审核（需为待审核状态）: supplierCode=" + dto.getSupplierCode());
        }

        SupplierInfo update = new SupplierInfo();
        update.setId(existing.getId());
        update.setStatus(auditStatus);
        update.setAuditStatus(auditStatus);
        update.setAuditRemark(dto.getAuditRemark());
        supplierInfoMapper.updateById(update);
        log.info("审核供应商完成: supplierCode={}, auditStatus={}", dto.getSupplierCode(), auditStatus);
    }

    // ====== 内部方法 ======

    private SupplierInfo requireSupplier(String supplierCode) {
        SupplierInfo entity = supplierInfoMapper.selectOne(new LambdaQueryWrapper<SupplierInfo>()
                .eq(SupplierInfo::getSupplierCode, supplierCode)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商不存在: " + supplierCode);
        }
        return entity;
    }

    /** 信用代码在同 supplierType 内唯一校验；{@code excludeCode} 非空时排除自身 */
    private void checkCreditCodeUnique(String unifiedCreditCode, Integer supplierType, String excludeCode) {
        LambdaQueryWrapper<SupplierInfo> wrapper = new LambdaQueryWrapper<SupplierInfo>()
                .eq(SupplierInfo::getUnifiedCreditCode, unifiedCreditCode);
        if (supplierType != null) {
            wrapper.eq(SupplierInfo::getSupplierType, supplierType);
        }
        if (excludeCode != null && !excludeCode.isEmpty()) {
            wrapper.ne(SupplierInfo::getSupplierCode, excludeCode);
        }
        Long count = supplierInfoMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "统一社会信用代码已存在");
        }
    }

    private SupplierInfoVO toVO(SupplierInfo entity) {
        SupplierInfoVO vo = new SupplierInfoVO();
        vo.setId(entity.getId());
        vo.setSupplierCode(entity.getSupplierCode());
        vo.setFullName(entity.getFullName());
        vo.setShortName(entity.getShortName());
        vo.setSupplierType(entity.getSupplierType());
        vo.setUnifiedCreditCode(entity.getUnifiedCreditCode());
        vo.setLegalPerson(entity.getLegalPerson());
        vo.setRegisteredCapital(entity.getRegisteredCapital());
        vo.setEstablishDate(entity.getEstablishDate());
        vo.setBusinessLicenseNo(entity.getBusinessLicenseNo());
        vo.setBusinessScope(entity.getBusinessScope());
        vo.setProvinceCode(entity.getProvinceCode());
        vo.setCityCode(entity.getCityCode());
        vo.setDistrictCode(entity.getDistrictCode());
        vo.setAddress(entity.getAddress());
        vo.setContactPerson(entity.getContactPerson());
        vo.setContactPhone(entity.getContactPhone());
        vo.setContactEmail(entity.getContactEmail());
        vo.setLogoUrl(entity.getLogoUrl());
        vo.setDescription(entity.getDescription());
        vo.setLicenseImage(entity.getLicenseImage());
        vo.setQualificationImage(entity.getQualificationImage());
        vo.setBankName(entity.getBankName());
        vo.setBankAccount(entity.getBankAccount());
        vo.setBankAccountName(entity.getBankAccountName());
        vo.setParkCount(entity.getParkCount());
        vo.setCooperationStartDate(entity.getCooperationStartDate());
        vo.setCooperationEndDate(entity.getCooperationEndDate());
        vo.setCommissionRate(entity.getCommissionRate());
        vo.setStatus(entity.getStatus());
        vo.setAuditStatus(entity.getAuditStatus());
        vo.setAuditRemark(entity.getAuditRemark());
        vo.setSortOrder(entity.getSortOrder());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
