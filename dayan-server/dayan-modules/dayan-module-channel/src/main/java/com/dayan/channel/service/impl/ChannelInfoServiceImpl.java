package com.dayan.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.channel.dto.ChannelAuditDTO;
import com.dayan.channel.dto.ChannelInfoCreateDTO;
import com.dayan.channel.dto.ChannelInfoQueryDTO;
import com.dayan.channel.dto.ChannelInfoUpdateDTO;
import com.dayan.channel.entity.ChannelInfo;
import com.dayan.channel.mapper.ChannelInfoMapper;
import com.dayan.channel.service.ChannelInfoService;
import com.dayan.channel.vo.ChannelInfoVO;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.mybatis.context.ContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 渠道信息（树形）服务实现。
 *
 * <p>{@code channel_info} 平台共享表（{@link com.dayan.common.mybatis.tenant.DayanTenantHandler} 忽略），
 * 不受渠道字段隔离约束，可直接跨渠道查询。
 *
 * <p>树形维护：
 * <ul>
 *   <li>{@code ancestors}：祖级链，逗号分隔的 channelCode（如 "CH00001,CH00002"）</li>
 *   <li>{@code level}：层级深度，顶级为 1，每深一级 +1</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelInfoServiceImpl implements ChannelInfoService {

    /** 渠道编码前缀 */
    private static final String CODE_PREFIX = "CH";
    /** 默认渠道类型 */
    private static final int DEFAULT_CHANNEL_TYPE = 2;

    private final ChannelInfoMapper channelInfoMapper;
    private final CodeGenerator codeGenerator;

    @Override
    public List<ChannelInfoVO> listByParent(ChannelInfoQueryDTO query) {
        LambdaQueryWrapper<ChannelInfo> wrapper = new LambdaQueryWrapper<ChannelInfo>()
                .orderByAsc(ChannelInfo::getSortOrder)
                .orderByAsc(ChannelInfo::getId);
        if (query.getParentCode() != null && !query.getParentCode().isEmpty()) {
            wrapper.eq(ChannelInfo::getParentCode, query.getParentCode());
        }
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(ChannelInfo::getChannelCode, query.getChannelCode());
        }
        if (query.getFullName() != null && !query.getFullName().isEmpty()) {
            wrapper.like(ChannelInfo::getFullName, query.getFullName());
        }
        if (query.getChannelType() != null) {
            wrapper.eq(ChannelInfo::getChannelType, query.getChannelType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ChannelInfo::getStatus, query.getStatus());
        }
        if (query.getAuditStatus() != null) {
            wrapper.eq(ChannelInfo::getAuditStatus, query.getAuditStatus());
        }
        if (query.getDistributorCode() != null && !query.getDistributorCode().isEmpty()) {
            wrapper.eq(ChannelInfo::getDistributorCode, query.getDistributorCode());
        }
        return channelInfoMapper.selectList(wrapper).stream()
                .sorted(Comparator
                        .comparingInt((ChannelInfo c) -> c.getSortOrder() == null ? 0 : c.getSortOrder())
                        .thenComparingLong(c -> c.getId() == null ? 0L : c.getId()))
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelInfoVO> tree() {
        // 全量加载后内存构建（渠道数量级小）
        List<ChannelInfo> all = channelInfoMapper.selectList(new LambdaQueryWrapper<ChannelInfo>()
                .orderByAsc(ChannelInfo::getSortOrder)
                .orderByAsc(ChannelInfo::getId));
        List<ChannelInfoVO> nodes = all.stream()
                .sorted(Comparator
                        .comparingInt((ChannelInfo c) -> c.getSortOrder() == null ? 0 : c.getSortOrder())
                        .thenComparingLong(c -> c.getId() == null ? 0L : c.getId()))
                .map(this::toVO)
                .collect(Collectors.toList());

        Map<String, ChannelInfoVO> codeMap = new LinkedHashMap<>();
        for (ChannelInfoVO node : nodes) {
            codeMap.put(node.getChannelCode(), node);
        }

        List<ChannelInfoVO> roots = new java.util.ArrayList<>();
        for (ChannelInfoVO node : nodes) {
            String parentCode = node.getParentCode();
            if (parentCode == null || parentCode.isEmpty() || !codeMap.containsKey(parentCode)) {
                roots.add(node);
            } else {
                codeMap.get(parentCode).getChildren().add(node);
            }
        }
        return roots;
    }

    @Override
    public ChannelInfoVO getDetail(String channelCode) {
        return toVO(requireChannel(channelCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ChannelInfoCreateDTO dto) {
        // 统一社会信用代码唯一校验（若提供）
        if (dto.getUnifiedCreditCode() != null && !dto.getUnifiedCreditCode().isEmpty()) {
            Long count = channelInfoMapper.selectCount(new LambdaQueryWrapper<ChannelInfo>()
                    .eq(ChannelInfo::getUnifiedCreditCode, dto.getUnifiedCreditCode()));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "统一社会信用代码已存在");
            }
        }

        String channelCode = codeGenerator.generate(CODE_PREFIX);

        ChannelInfo entity = new ChannelInfo();
        entity.setChannelCode(channelCode);
        entity.setFullName(dto.getFullName());
        entity.setShortName(dto.getShortName());
        entity.setChannelType(dto.getChannelType() == null ? DEFAULT_CHANNEL_TYPE : dto.getChannelType());

        // 维护层级
        String parentCode = dto.getParentCode();
        if (parentCode == null || parentCode.isEmpty()) {
            entity.setParentCode("");
            entity.setAncestors("");
            entity.setLevel(1);
        } else {
            ChannelInfo parent = requireChannel(parentCode);
            entity.setParentCode(parentCode);
            entity.setAncestors(buildAncestors(parent.getAncestors(), parentCode));
            entity.setLevel((parent.getLevel() == null ? 1 : parent.getLevel()) + 1);
        }

        entity.setUnifiedCreditCode(dto.getUnifiedCreditCode());
        entity.setLegalPerson(dto.getLegalPerson());
        entity.setProvinceCode(dto.getProvinceCode());
        entity.setCityCode(dto.getCityCode());
        entity.setDistrictCode(dto.getDistrictCode());
        entity.setAddress(dto.getAddress());
        entity.setContactPerson(dto.getContactPerson());
        entity.setContactPhone(dto.getContactPhone());
        entity.setContactEmail(dto.getContactEmail());
        entity.setLogoUrl(dto.getLogoUrl());
        entity.setDescription(dto.getDescription());
        entity.setDistributorCode(dto.getDistributorCode());
        entity.setCooperationStartDate(dto.getCooperationStartDate());
        entity.setSettlementCycle(dto.getSettlementCycle());
        entity.setFeatureConfig(dto.getFeatureConfig());
        entity.setAgentCount(0);
        entity.setTotalOrderAmount(java.math.BigDecimal.ZERO);
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(1);
        entity.setAuditStatus(0);
        entity.setRemark(dto.getRemark());

        channelInfoMapper.insert(entity);
        log.info("创建渠道成功: channelCode={}, parentCode={}", channelCode, parentCode);
        return channelCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String channelCode, ChannelInfoUpdateDTO dto) {
        ChannelInfo existing = requireChannel(channelCode);
        ChannelInfo update = new ChannelInfo();
        update.setId(existing.getId());

        if (dto.getFullName() != null) update.setFullName(dto.getFullName());
        if (dto.getShortName() != null) update.setShortName(dto.getShortName());
        if (dto.getChannelType() != null) update.setChannelType(dto.getChannelType());

        // 移动层级：变更 parentCode 时重算 ancestors/level
        if (dto.getParentCode() != null) {
            String newParent = dto.getParentCode();
            if (newParent.isEmpty()) {
                // 移到顶级
                update.setParentCode("");
                update.setAncestors("");
                update.setLevel(1);
            } else {
                if (newParent.equals(channelCode)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "上级渠道不能是自身");
                }
                ChannelInfo parent = requireChannel(newParent);
                // 防止把渠道挂到自己的子孙下（形成环）
                if (isDescendant(existing, newParent)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "不能将渠道移动到其子渠道下");
                }
                update.setParentCode(newParent);
                update.setAncestors(buildAncestors(parent.getAncestors(), newParent));
                update.setLevel((parent.getLevel() == null ? 1 : parent.getLevel()) + 1);
            }
        }

        if (dto.getUnifiedCreditCode() != null) {
            // 信用代码唯一校验（排除自身）
            Long count = channelInfoMapper.selectCount(new LambdaQueryWrapper<ChannelInfo>()
                    .eq(ChannelInfo::getUnifiedCreditCode, dto.getUnifiedCreditCode())
                    .ne(ChannelInfo::getChannelCode, channelCode));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "统一社会信用代码已存在");
            }
            update.setUnifiedCreditCode(dto.getUnifiedCreditCode());
        }
        if (dto.getLegalPerson() != null) update.setLegalPerson(dto.getLegalPerson());
        if (dto.getProvinceCode() != null) update.setProvinceCode(dto.getProvinceCode());
        if (dto.getCityCode() != null) update.setCityCode(dto.getCityCode());
        if (dto.getDistrictCode() != null) update.setDistrictCode(dto.getDistrictCode());
        if (dto.getAddress() != null) update.setAddress(dto.getAddress());
        if (dto.getContactPerson() != null) update.setContactPerson(dto.getContactPerson());
        if (dto.getContactPhone() != null) update.setContactPhone(dto.getContactPhone());
        if (dto.getContactEmail() != null) update.setContactEmail(dto.getContactEmail());
        if (dto.getLogoUrl() != null) update.setLogoUrl(dto.getLogoUrl());
        if (dto.getDescription() != null) update.setDescription(dto.getDescription());
        if (dto.getDistributorCode() != null) update.setDistributorCode(dto.getDistributorCode());
        if (dto.getCooperationStartDate() != null) update.setCooperationStartDate(dto.getCooperationStartDate());
        if (dto.getSettlementCycle() != null) update.setSettlementCycle(dto.getSettlementCycle());
        if (dto.getFeatureConfig() != null) update.setFeatureConfig(dto.getFeatureConfig());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getAuditStatus() != null) update.setAuditStatus(dto.getAuditStatus());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        channelInfoMapper.updateById(update);
        log.info("更新渠道成功: channelCode={}", channelCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String channelCode) {
        requireChannel(channelCode);
        // 校验是否存在子渠道
        Long childCount = channelInfoMapper.selectCount(new LambdaQueryWrapper<ChannelInfo>()
                .eq(ChannelInfo::getParentCode, channelCode));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "存在子渠道，无法删除");
        }
        channelInfoMapper.delete(new LambdaQueryWrapper<ChannelInfo>()
                .eq(ChannelInfo::getChannelCode, channelCode));
        log.info("删除渠道成功: channelCode={}", channelCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(ChannelAuditDTO dto) {
        ChannelInfo existing = requireChannel(dto.getChannelCode());
        Integer auditStatus = dto.getAuditStatus();
        // 业务校验：auditStatus 仅允许 1=通过 / 2=驳回
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核状态非法（仅支持 1=通过 / 2=驳回）");
        }
        ChannelInfo update = new ChannelInfo();
        update.setId(existing.getId());
        update.setAuditStatus(auditStatus);
        channelInfoMapper.updateById(update);
        log.info("渠道审核完成: channelCode={}, auditStatus={}", dto.getChannelCode(), auditStatus);
    }

    // ====== 内部方法 ======

    private ChannelInfo requireChannel(String channelCode) {
        ChannelInfo channel = channelInfoMapper.selectOne(new LambdaQueryWrapper<ChannelInfo>()
                .eq(ChannelInfo::getChannelCode, channelCode)
                .last("LIMIT 1"));
        if (channel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "渠道不存在: " + channelCode);
        }
        return channel;
    }

    @Override
    public void requireManageCapability() {
        String channelCode = ContextHolder.getChannelCode();
        ChannelInfo current = requireChannel(channelCode);
        if (current.getCanManage() == null || current.getCanManage() != 1) {
            throw new BusinessException(ErrorCode.BUSINESS, "当前渠道无配置权限");
        }
    }

    @Override
    public void requireDescendant(String targetChannelCode) {
        if (targetChannelCode == null || targetChannelCode.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标渠道编码不能为空");
        }
        String currentCode = ContextHolder.getChannelCode();
        if (targetChannelCode.equals(currentCode)) {
            return; // 自身允许（账号管理本渠道自身）
        }
        ChannelInfo target = requireChannel(targetChannelCode);
        String ancestors = target.getAncestors();
        if (ancestors == null || !java.util.Arrays.asList(ancestors.split(",")).contains(currentCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作非本渠道子树的资源");
        }
    }

    /** 判断 targetCode 是否为 current 的子孙（依据 ancestors 链） */
    private boolean isDescendant(ChannelInfo current, String targetCode) {
        String ancestors = current.getAncestors();
        if (ancestors == null || ancestors.isEmpty()) {
            return false;
        }
        for (String code : ancestors.split(",")) {
            if (targetCode.equals(code)) {
                return true;
            }
        }
        return false;
    }

    private String buildAncestors(String parentAncestors, String parentCode) {
        if (parentAncestors == null || parentAncestors.isEmpty()) {
            return parentCode;
        }
        return parentAncestors + "," + parentCode;
    }

    private ChannelInfoVO toVO(ChannelInfo entity) {
        ChannelInfoVO vo = new ChannelInfoVO();
        vo.setId(entity.getId());
        vo.setChannelCode(entity.getChannelCode());
        vo.setFullName(entity.getFullName());
        vo.setShortName(entity.getShortName());
        vo.setChannelType(entity.getChannelType());
        vo.setParentCode(entity.getParentCode());
        vo.setAncestors(entity.getAncestors());
        vo.setLevel(entity.getLevel());
        vo.setUnifiedCreditCode(entity.getUnifiedCreditCode());
        vo.setLegalPerson(entity.getLegalPerson());
        vo.setProvinceCode(entity.getProvinceCode());
        vo.setCityCode(entity.getCityCode());
        vo.setDistrictCode(entity.getDistrictCode());
        vo.setAddress(entity.getAddress());
        vo.setContactPerson(entity.getContactPerson());
        vo.setContactPhone(entity.getContactPhone());
        vo.setContactEmail(entity.getContactEmail());
        vo.setLogoUrl(entity.getLogoUrl());
        vo.setDescription(entity.getDescription());
        vo.setAgentCount(entity.getAgentCount());
        vo.setTotalOrderAmount(entity.getTotalOrderAmount());
        vo.setCooperationStartDate(entity.getCooperationStartDate());
        vo.setDistributorCode(entity.getDistributorCode());
        vo.setSettlementCycle(entity.getSettlementCycle());
        vo.setFeatureConfig(entity.getFeatureConfig());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setAuditStatus(entity.getAuditStatus());
        vo.setCanManage(entity.getCanManage());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
