package com.dayan.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.supplier.dto.SupplierContractCreateDTO;
import com.dayan.supplier.dto.SupplierContractQueryDTO;
import com.dayan.supplier.dto.SupplierContractUpdateDTO;
import com.dayan.supplier.entity.SupplierContract;
import com.dayan.supplier.mapper.SupplierContractMapper;
import com.dayan.supplier.service.SupplierContractService;
import com.dayan.supplier.vo.SupplierContractVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 供应商合同服务实现。
 *
 * <p>续约链：创建时若 {@code parentContractCode} 非空，视为续约：
 * <ul>
 *   <li>原合同必须存在</li>
 *   <li>原合同 {@code renewCount} +1</li>
 *   <li>新合同 {@code renewCount} 初始化为 0，{@code parentContractCode} 指向原合同</li>
 * </ul>
 *
 * <p>日期校验：{@code effectiveDate < expireDate}（两者都提供时校验）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierContractServiceImpl implements SupplierContractService {

    /** 合同编码前缀 */
    private static final String CODE_PREFIX = "HT";

    private final SupplierContractMapper contractMapper;
    private final CodeGenerator codeGenerator;

    @Override
    public PageResult<SupplierContractVO> page(SupplierContractQueryDTO query) {
        LambdaQueryWrapper<SupplierContract> wrapper = new LambdaQueryWrapper<SupplierContract>()
                .eq(query.getContractCode() != null && !query.getContractCode().isEmpty(),
                        SupplierContract::getContractCode, query.getContractCode())
                .like(query.getContractName() != null && !query.getContractName().isEmpty(),
                        SupplierContract::getContractName, query.getContractName())
                .eq(query.getSupplierCode() != null && !query.getSupplierCode().isEmpty(),
                        SupplierContract::getSupplierCode, query.getSupplierCode())
                .eq(query.getOrganCode() != null && !query.getOrganCode().isEmpty(),
                        SupplierContract::getOrganCode, query.getOrganCode())
                .eq(query.getContractType() != null,
                        SupplierContract::getContractType, query.getContractType())
                .eq(query.getSettlementCycle() != null,
                        SupplierContract::getSettlementCycle, query.getSettlementCycle())
                .eq(query.getStatus() != null, SupplierContract::getStatus, query.getStatus())
                .eq(query.getParentContractCode() != null && !query.getParentContractCode().isEmpty(),
                        SupplierContract::getParentContractCode, query.getParentContractCode())
                .orderByDesc(SupplierContract::getCreatedAt);
        Page<SupplierContract> page = contractMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SupplierContractVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public SupplierContractVO getDetail(String contractCode) {
        return toVO(requireContract(contractCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SupplierContractCreateDTO dto) {
        // 日期校验：生效 < 到期
        validateDateRange(dto.getEffectiveDate(), dto.getExpireDate());

        String contractCode = codeGenerator.generate(CODE_PREFIX);

        SupplierContract entity = new SupplierContract();
        entity.setContractCode(contractCode);
        entity.setContractName(dto.getContractName());
        entity.setSupplierCode(dto.getSupplierCode());
        entity.setOrganCode(dto.getOrganCode());
        entity.setContractType(dto.getContractType());
        entity.setSignDate(dto.getSignDate());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setExpireDate(dto.getExpireDate());
        entity.setContractAmount(dto.getContractAmount());
        entity.setCommissionRate(dto.getCommissionRate());
        entity.setSettlementCycle(dto.getSettlementCycle());
        entity.setTerms(dto.getTerms());
        entity.setAttachmentUrls(dto.getAttachmentUrls());
        entity.setSignPerson(dto.getSignPerson());
        entity.setSignSealImage(dto.getSignSealImage());
        entity.setIsAutoRenew(dto.getIsAutoRenew() == null ? 0 : dto.getIsAutoRenew());
        entity.setRenewCount(0);
        entity.setAuditRemark(dto.getAuditRemark());
        entity.setRemark(dto.getRemark());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());

        // 续约链处理：parentContractCode 指向原合同
        String parentCode = dto.getParentContractCode();
        if (parentCode != null && !parentCode.isEmpty()) {
            SupplierContract parent = requireContract(parentCode);
            entity.setParentContractCode(parentCode);
            // 原合同 renewCount +1
            SupplierContract parentUpdate = new SupplierContract();
            parentUpdate.setId(parent.getId());
            parentUpdate.setRenewCount((parent.getRenewCount() == null ? 0 : parent.getRenewCount()) + 1);
            contractMapper.updateById(parentUpdate);
            log.info("合同续约: parentContractCode={}, renewCount={}",
                    parentCode, parentUpdate.getRenewCount());
        }

        contractMapper.insert(entity);
        log.info("创建供应商合同成功: contractCode={}, supplierCode={}, parentContractCode={}",
                contractCode, dto.getSupplierCode(), parentCode);
        return contractCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String contractCode, SupplierContractUpdateDTO dto) {
        SupplierContract existing = requireContract(contractCode);

        // 日期校验：以"现有 + 入参覆盖"后的最终值为准
        LocalDate effective = dto.getEffectiveDate() != null ? dto.getEffectiveDate() : existing.getEffectiveDate();
        LocalDate expire = dto.getExpireDate() != null ? dto.getExpireDate() : existing.getExpireDate();
        validateDateRange(effective, expire);

        SupplierContract update = new SupplierContract();
        update.setId(existing.getId());
        if (dto.getContractName() != null) update.setContractName(dto.getContractName());
        if (dto.getOrganCode() != null) update.setOrganCode(dto.getOrganCode());
        if (dto.getContractType() != null) update.setContractType(dto.getContractType());
        if (dto.getSignDate() != null) update.setSignDate(dto.getSignDate());
        if (dto.getEffectiveDate() != null) update.setEffectiveDate(dto.getEffectiveDate());
        if (dto.getExpireDate() != null) update.setExpireDate(dto.getExpireDate());
        if (dto.getContractAmount() != null) update.setContractAmount(dto.getContractAmount());
        if (dto.getCommissionRate() != null) update.setCommissionRate(dto.getCommissionRate());
        if (dto.getSettlementCycle() != null) update.setSettlementCycle(dto.getSettlementCycle());
        if (dto.getTerms() != null) update.setTerms(dto.getTerms());
        if (dto.getAttachmentUrls() != null) update.setAttachmentUrls(dto.getAttachmentUrls());
        if (dto.getSignPerson() != null) update.setSignPerson(dto.getSignPerson());
        if (dto.getSignSealImage() != null) update.setSignSealImage(dto.getSignSealImage());
        if (dto.getIsAutoRenew() != null) update.setIsAutoRenew(dto.getIsAutoRenew());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getAuditRemark() != null) update.setAuditRemark(dto.getAuditRemark());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        contractMapper.updateById(update);
        log.info("更新供应商合同成功: contractCode={}", contractCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String contractCode) {
        requireContract(contractCode);
        // 校验是否存在续约子合同
        Long childCount = contractMapper.selectCount(new LambdaQueryWrapper<SupplierContract>()
                .eq(SupplierContract::getParentContractCode, contractCode));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "存在续约子合同，无法删除");
        }
        contractMapper.delete(new LambdaQueryWrapper<SupplierContract>()
                .eq(SupplierContract::getContractCode, contractCode));
        log.info("删除供应商合同成功: contractCode={}", contractCode);
    }

    // ====== 内部方法 ======

    private SupplierContract requireContract(String contractCode) {
        SupplierContract entity = contractMapper.selectOne(new LambdaQueryWrapper<SupplierContract>()
                .eq(SupplierContract::getContractCode, contractCode)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商合同不存在: " + contractCode);
        }
        return entity;
    }

    /** 校验 effectiveDate < expireDate（两者都提供时） */
    private void validateDateRange(LocalDate effectiveDate, LocalDate expireDate) {
        if (effectiveDate != null && expireDate != null && !effectiveDate.isBefore(expireDate)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "生效日期必须早于到期日期");
        }
    }

    private SupplierContractVO toVO(SupplierContract entity) {
        SupplierContractVO vo = new SupplierContractVO();
        vo.setId(entity.getId());
        vo.setContractCode(entity.getContractCode());
        vo.setContractName(entity.getContractName());
        vo.setSupplierCode(entity.getSupplierCode());
        vo.setOrganCode(entity.getOrganCode());
        vo.setContractType(entity.getContractType());
        vo.setSignDate(entity.getSignDate());
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setExpireDate(entity.getExpireDate());
        vo.setContractAmount(entity.getContractAmount());
        vo.setCommissionRate(entity.getCommissionRate());
        vo.setSettlementCycle(entity.getSettlementCycle());
        vo.setTerms(entity.getTerms());
        vo.setAttachmentUrls(entity.getAttachmentUrls());
        vo.setSignPerson(entity.getSignPerson());
        vo.setSignSealImage(entity.getSignSealImage());
        vo.setIsAutoRenew(entity.getIsAutoRenew());
        vo.setRenewCount(entity.getRenewCount());
        vo.setParentContractCode(entity.getParentContractCode());
        vo.setStatus(entity.getStatus());
        vo.setAuditRemark(entity.getAuditRemark());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
