package com.dayan.distributor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.BusinessCode;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.distributor.dto.DistributorInfoCreateDTO;
import com.dayan.distributor.dto.DistributorInfoQueryDTO;
import com.dayan.distributor.dto.DistributorInfoUpdateDTO;
import com.dayan.distributor.entity.DistributorInfo;
import com.dayan.distributor.mapper.DistributorInfoMapper;
import com.dayan.distributor.service.DistributorInfoService;
import com.dayan.distributor.vo.DistributorInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分销商信息（distributor_info）服务实现。
 *
 * <p>平台共享表（{@code DayanTenantHandler} 忽略 distributor_ 前缀），查询/写入不带 channel_code 条件。
 *
 * <p>主体类型差异化校验：
 * <ul>
 *   <li>企业（subjectType=1）：unifiedCreditCode + legalPerson + businessLicenseNo 必填；
 *       unifiedCreditCode 在企业类型间唯一</li>
 *   <li>个人（subjectType=2）：idCard + phone 必填</li>
 * </ul>
 *
 * <p>编码：{@code DS+5 位}，由 {@link CodeGenerator#generate(String)} 生成（{@link BusinessCode#DISTRIBUTOR}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributorInfoServiceImpl implements DistributorInfoService {

    /** 主体类型：企业 */
    private static final int SUBJECT_TYPE_ENTERPRISE = 1;
    /** 主体类型：个人 */
    private static final int SUBJECT_TYPE_PERSONAL = 2;
    /** 默认状态：待审核 */
    private static final int DEFAULT_STATUS = 0;

    private final DistributorInfoMapper distributorInfoMapper;
    private final CodeGenerator codeGenerator;

    @Override
    public PageResult<DistributorInfoVO> page(DistributorInfoQueryDTO query) {
        LambdaQueryWrapper<DistributorInfo> wrapper = buildQueryWrapper(query);
        Page<DistributorInfo> page = distributorInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<DistributorInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<DistributorInfoVO> list(DistributorInfoQueryDTO query) {
        LambdaQueryWrapper<DistributorInfo> wrapper = buildQueryWrapper(query);
        return distributorInfoMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public DistributorInfoVO getDetail(String distributorCode) {
        return toVO(requireDistributor(distributorCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(DistributorInfoCreateDTO dto) {
        validateSubjectRequiredFields(dto.getSubjectType(), dto.getUnifiedCreditCode(),
                dto.getLegalPerson(), dto.getBusinessLicenseNo(), dto.getIdCard(), dto.getPhone());

        // 统一社会信用代码唯一校验（企业类型间）
        if (dto.getSubjectType() == SUBJECT_TYPE_ENTERPRISE
                && dto.getUnifiedCreditCode() != null && !dto.getUnifiedCreditCode().isEmpty()) {
            Long count = distributorInfoMapper.selectCount(new LambdaQueryWrapper<DistributorInfo>()
                    .eq(DistributorInfo::getSubjectType, SUBJECT_TYPE_ENTERPRISE)
                    .eq(DistributorInfo::getUnifiedCreditCode, dto.getUnifiedCreditCode()));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "统一社会信用代码已存在");
            }
        }

        String distributorCode = codeGenerator.generate(BusinessCode.DISTRIBUTOR);

        DistributorInfo entity = new DistributorInfo();
        entity.setDistributorCode(distributorCode);
        entity.setFullName(dto.getFullName());
        entity.setShortName(dto.getShortName());
        entity.setSubjectType(dto.getSubjectType());
        entity.setUnifiedCreditCode(dto.getUnifiedCreditCode());
        entity.setLegalPerson(dto.getLegalPerson());
        entity.setBusinessLicenseNo(dto.getBusinessLicenseNo());
        entity.setRegisteredCapital(dto.getRegisteredCapital());
        entity.setEstablishDate(dto.getEstablishDate());
        entity.setIdCard(dto.getIdCard());
        entity.setGender(dto.getGender() == null ? 0 : dto.getGender());
        entity.setPhone(dto.getPhone());
        entity.setContactPerson(dto.getContactPerson());
        entity.setContactEmail(dto.getContactEmail());
        entity.setProvinceCode(dto.getProvinceCode());
        entity.setCityCode(dto.getCityCode());
        entity.setDistrictCode(dto.getDistrictCode());
        entity.setAddress(dto.getAddress());
        entity.setBankName(dto.getBankName());
        entity.setBankAccount(dto.getBankAccount());
        entity.setBankAccountName(dto.getBankAccountName());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setRemark(dto.getRemark());

        distributorInfoMapper.insert(entity);
        log.info("创建分销商成功: distributorCode={}, subjectType={}", distributorCode, dto.getSubjectType());
        return distributorCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String distributorCode, DistributorInfoUpdateDTO dto) {
        DistributorInfo existing = requireDistributor(distributorCode);
        // 主体类型一经确定不允许变更（资质字段集合不同，避免数据语义混乱）
        if (dto.getSubjectType() != null && !dto.getSubjectType().equals(existing.getSubjectType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "主体类型不允许变更");
        }

        // 统一社会信用代码唯一校验（企业类型间，排除自身）
        if (existing.getSubjectType() == SUBJECT_TYPE_ENTERPRISE
                && dto.getUnifiedCreditCode() != null && !dto.getUnifiedCreditCode().isEmpty()
                && !dto.getUnifiedCreditCode().equals(existing.getUnifiedCreditCode())) {
            Long count = distributorInfoMapper.selectCount(new LambdaQueryWrapper<DistributorInfo>()
                    .eq(DistributorInfo::getSubjectType, SUBJECT_TYPE_ENTERPRISE)
                    .eq(DistributorInfo::getUnifiedCreditCode, dto.getUnifiedCreditCode())
                    .ne(DistributorInfo::getId, existing.getId()));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "统一社会信用代码已存在");
            }
        }

        DistributorInfo update = new DistributorInfo();
        update.setId(existing.getId());
        if (dto.getFullName() != null) update.setFullName(dto.getFullName());
        if (dto.getShortName() != null) update.setShortName(dto.getShortName());
        if (dto.getUnifiedCreditCode() != null) update.setUnifiedCreditCode(dto.getUnifiedCreditCode());
        if (dto.getLegalPerson() != null) update.setLegalPerson(dto.getLegalPerson());
        if (dto.getBusinessLicenseNo() != null) update.setBusinessLicenseNo(dto.getBusinessLicenseNo());
        if (dto.getRegisteredCapital() != null) update.setRegisteredCapital(dto.getRegisteredCapital());
        if (dto.getEstablishDate() != null) update.setEstablishDate(dto.getEstablishDate());
        if (dto.getIdCard() != null) update.setIdCard(dto.getIdCard());
        if (dto.getGender() != null) update.setGender(dto.getGender());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getContactPerson() != null) update.setContactPerson(dto.getContactPerson());
        if (dto.getContactEmail() != null) update.setContactEmail(dto.getContactEmail());
        if (dto.getProvinceCode() != null) update.setProvinceCode(dto.getProvinceCode());
        if (dto.getCityCode() != null) update.setCityCode(dto.getCityCode());
        if (dto.getDistrictCode() != null) update.setDistrictCode(dto.getDistrictCode());
        if (dto.getAddress() != null) update.setAddress(dto.getAddress());
        if (dto.getBankName() != null) update.setBankName(dto.getBankName());
        if (dto.getBankAccount() != null) update.setBankAccount(dto.getBankAccount());
        if (dto.getBankAccountName() != null) update.setBankAccountName(dto.getBankAccountName());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        distributorInfoMapper.updateById(update);
        log.info("更新分销商成功: distributorCode={}", distributorCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String distributorCode) {
        requireDistributor(distributorCode);
        distributorInfoMapper.delete(new LambdaQueryWrapper<DistributorInfo>()
                .eq(DistributorInfo::getDistributorCode, distributorCode));
        log.info("删除分销商成功: distributorCode={}", distributorCode);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<DistributorInfo> buildQueryWrapper(DistributorInfoQueryDTO query) {
        LambdaQueryWrapper<DistributorInfo> wrapper = new LambdaQueryWrapper<DistributorInfo>()
                .orderByAsc(DistributorInfo::getSortOrder)
                .orderByDesc(DistributorInfo::getCreatedAt);
        if (query.getDistributorCode() != null && !query.getDistributorCode().isEmpty()) {
            wrapper.eq(DistributorInfo::getDistributorCode, query.getDistributorCode());
        }
        if (query.getFullName() != null && !query.getFullName().isEmpty()) {
            wrapper.like(DistributorInfo::getFullName, query.getFullName());
        }
        if (query.getSubjectType() != null) {
            wrapper.eq(DistributorInfo::getSubjectType, query.getSubjectType());
        }
        if (query.getUnifiedCreditCode() != null && !query.getUnifiedCreditCode().isEmpty()) {
            wrapper.eq(DistributorInfo::getUnifiedCreditCode, query.getUnifiedCreditCode());
        }
        if (query.getPhone() != null && !query.getPhone().isEmpty()) {
            wrapper.like(DistributorInfo::getPhone, query.getPhone());
        }
        if (query.getStatus() != null) {
            wrapper.eq(DistributorInfo::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private DistributorInfo requireDistributor(String distributorCode) {
        DistributorInfo distributor = distributorInfoMapper.selectOne(new LambdaQueryWrapper<DistributorInfo>()
                .eq(DistributorInfo::getDistributorCode, distributorCode)
                .last("LIMIT 1"));
        if (distributor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分销商不存在: " + distributorCode);
        }
        return distributor;
    }

    /**
     * 按主体类型校验必填字段。
     *
     * <ul>
     *   <li>企业：unifiedCreditCode + legalPerson + businessLicenseNo 必填</li>
     *   <li>个人：idCard + phone 必填（phone 已由 DTO @NotBlank 保证，此处二次防御）</li>
     * </ul>
     */
    private void validateSubjectRequiredFields(Integer subjectType,
                                               String unifiedCreditCode, String legalPerson,
                                               String businessLicenseNo, String idCard, String phone) {
        if (subjectType == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "主体类型不能为空");
        }
        if (subjectType == SUBJECT_TYPE_ENTERPRISE) {
            if (isBlank(unifiedCreditCode)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "企业类型：统一社会信用代码不能为空");
            }
            if (isBlank(legalPerson)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "企业类型：法定代表人不能为空");
            }
            if (isBlank(businessLicenseNo)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "企业类型：营业执照号不能为空");
            }
        } else if (subjectType == SUBJECT_TYPE_PERSONAL) {
            if (isBlank(idCard)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "个人类型：身份证号不能为空");
            }
            if (isBlank(phone)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "个人类型：联系电话不能为空");
            }
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "主体类型取值非法，仅支持 1=企业 / 2=个人");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    private DistributorInfoVO toVO(DistributorInfo entity) {
        DistributorInfoVO vo = new DistributorInfoVO();
        vo.setId(entity.getId());
        vo.setDistributorCode(entity.getDistributorCode());
        vo.setFullName(entity.getFullName());
        vo.setShortName(entity.getShortName());
        vo.setSubjectType(entity.getSubjectType());
        vo.setUnifiedCreditCode(entity.getUnifiedCreditCode());
        vo.setLegalPerson(entity.getLegalPerson());
        vo.setBusinessLicenseNo(entity.getBusinessLicenseNo());
        vo.setRegisteredCapital(entity.getRegisteredCapital());
        vo.setEstablishDate(entity.getEstablishDate());
        vo.setIdCard(entity.getIdCard());
        vo.setGender(entity.getGender());
        vo.setPhone(entity.getPhone());
        vo.setContactPerson(entity.getContactPerson());
        vo.setContactEmail(entity.getContactEmail());
        vo.setProvinceCode(entity.getProvinceCode());
        vo.setCityCode(entity.getCityCode());
        vo.setDistrictCode(entity.getDistrictCode());
        vo.setAddress(entity.getAddress());
        vo.setBankName(entity.getBankName());
        vo.setBankAccount(entity.getBankAccount());
        vo.setBankAccountName(entity.getBankAccountName());
        vo.setStatus(entity.getStatus());
        vo.setSortOrder(entity.getSortOrder());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
