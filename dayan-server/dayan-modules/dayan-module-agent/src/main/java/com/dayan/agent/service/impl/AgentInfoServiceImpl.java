package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.agent.dto.AgentInfoCreateDTO;
import com.dayan.agent.dto.AgentInfoQueryDTO;
import com.dayan.agent.dto.AgentInfoUpdateDTO;
import com.dayan.agent.entity.AgentInfo;
import com.dayan.agent.mapper.AgentInfoMapper;
import com.dayan.agent.service.AgentInfoService;
import com.dayan.agent.vo.AgentInfoVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.mybatis.context.ContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 代理人信息（agent_info）服务实现。
 *
 * <p>渠道隔离：agent_info 表含 channel_code 字段，由 {@code TenantLineInnerInterceptor} 自动追加查询/更新条件；
 * 新增时显式写入 channel_code（取入参或上下文）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentInfoServiceImpl implements AgentInfoService {

    private final AgentInfoMapper agentInfoMapper;

    @Override
    public PageResult<AgentInfoVO> page(AgentInfoQueryDTO query) {
        LambdaQueryWrapper<AgentInfo> wrapper = new LambdaQueryWrapper<AgentInfo>()
                .orderByDesc(AgentInfo::getCreatedAt);
        if (query.getAgentCode() != null && !query.getAgentCode().isEmpty()) {
            wrapper.eq(AgentInfo::getAgentCode, query.getAgentCode());
        }
        // channel_code 优先取入参，否则由租户拦截器从上下文自动注入
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(AgentInfo::getChannelCode, query.getChannelCode());
        }
        if (query.getFullName() != null && !query.getFullName().isEmpty()) {
            wrapper.like(AgentInfo::getFullName, query.getFullName());
        }
        if (query.getPhone() != null && !query.getPhone().isEmpty()) {
            wrapper.like(AgentInfo::getPhone, query.getPhone());
        }
        if (query.getAgentLevel() != null) {
            wrapper.eq(AgentInfo::getAgentLevel, query.getAgentLevel());
        }
        if (query.getIsCertified() != null) {
            wrapper.eq(AgentInfo::getIsCertified, query.getIsCertified());
        }
        if (query.getStatus() != null) {
            wrapper.eq(AgentInfo::getStatus, query.getStatus());
        }
        Page<AgentInfo> page = agentInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<AgentInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public AgentInfoVO getDetail(String agentCode) {
        AgentInfo agent = selectByCode(agentCode);
        return toVO(agent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(AgentInfoCreateDTO dto) {
        // channel_code 取入参或上下文
        String channelCode = pickChannelCode(dto.getChannelCode());

        // employee_no 渠道内唯一校验（若提供）
        if (dto.getEmployeeNo() != null && !dto.getEmployeeNo().isEmpty()) {
            Long count = agentInfoMapper.selectCount(new LambdaQueryWrapper<AgentInfo>()
                    .eq(AgentInfo::getChannelCode, channelCode)
                    .eq(AgentInfo::getEmployeeNo, dto.getEmployeeNo()));
            if (count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "工号已存在: " + dto.getEmployeeNo());
            }
        }

        AgentInfo entity = new AgentInfo();
        entity.setAgentCode(generateAgentCode());
        entity.setFullName(dto.getFullName());
        entity.setGender(dto.getGender() == null ? 0 : dto.getGender());
        entity.setAvatar(dto.getAvatar());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setIdCard(dto.getIdCard());
        entity.setChannelCode(channelCode);
        entity.setCompanyName(dto.getCompanyName());
        entity.setBranchName(dto.getBranchName());
        entity.setDepartment(dto.getDepartment());
        entity.setPosition(dto.getPosition());
        entity.setEmployeeNo(dto.getEmployeeNo());
        entity.setLicenseNo(dto.getLicenseNo());
        entity.setProvinceCode(dto.getProvinceCode());
        entity.setCityCode(dto.getCityCode());
        entity.setDistrictCode(dto.getDistrictCode());
        entity.setAddress(dto.getAddress());
        entity.setServiceIntro(dto.getServiceIntro());
        entity.setClientCount(0);
        entity.setTotalOrderCount(0);
        entity.setTotalOrderAmount(java.math.BigDecimal.ZERO);
        entity.setAgentLevel(dto.getAgentLevel() == null ? 1 : dto.getAgentLevel());
        entity.setIsCertified(dto.getIsCertified() == null ? 0 : dto.getIsCertified());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        agentInfoMapper.insert(entity);
        log.info("创建代理人成功: agentCode={}, channelCode={}", entity.getAgentCode(), channelCode);
        return entity.getAgentCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String agentCode, AgentInfoUpdateDTO dto) {
        AgentInfo existing = selectByCode(agentCode);
        // employee_no 渠道内唯一校验（若变更）
        if (dto.getEmployeeNo() != null && !dto.getEmployeeNo().isEmpty()
                && !dto.getEmployeeNo().equals(existing.getEmployeeNo())) {
            Long count = agentInfoMapper.selectCount(new LambdaQueryWrapper<AgentInfo>()
                    .eq(AgentInfo::getChannelCode, existing.getChannelCode())
                    .eq(AgentInfo::getEmployeeNo, dto.getEmployeeNo())
                    .ne(AgentInfo::getId, existing.getId()));
            if (count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "工号已存在: " + dto.getEmployeeNo());
            }
        }
        AgentInfo update = new AgentInfo();
        update.setId(existing.getId());
        if (dto.getFullName() != null) update.setFullName(dto.getFullName());
        if (dto.getGender() != null) update.setGender(dto.getGender());
        if (dto.getAvatar() != null) update.setAvatar(dto.getAvatar());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getEmail() != null) update.setEmail(dto.getEmail());
        if (dto.getIdCard() != null) update.setIdCard(dto.getIdCard());
        if (dto.getCompanyName() != null) update.setCompanyName(dto.getCompanyName());
        if (dto.getBranchName() != null) update.setBranchName(dto.getBranchName());
        if (dto.getDepartment() != null) update.setDepartment(dto.getDepartment());
        if (dto.getPosition() != null) update.setPosition(dto.getPosition());
        if (dto.getEmployeeNo() != null) update.setEmployeeNo(dto.getEmployeeNo());
        if (dto.getLicenseNo() != null) update.setLicenseNo(dto.getLicenseNo());
        if (dto.getProvinceCode() != null) update.setProvinceCode(dto.getProvinceCode());
        if (dto.getCityCode() != null) update.setCityCode(dto.getCityCode());
        if (dto.getDistrictCode() != null) update.setDistrictCode(dto.getDistrictCode());
        if (dto.getAddress() != null) update.setAddress(dto.getAddress());
        if (dto.getServiceIntro() != null) update.setServiceIntro(dto.getServiceIntro());
        if (dto.getAgentLevel() != null) update.setAgentLevel(dto.getAgentLevel());
        if (dto.getIsCertified() != null) update.setIsCertified(dto.getIsCertified());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        agentInfoMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String agentCode) {
        selectByCode(agentCode);
        agentInfoMapper.delete(new LambdaQueryWrapper<AgentInfo>()
                .eq(AgentInfo::getAgentCode, agentCode));
    }

    private AgentInfo selectByCode(String agentCode) {
        AgentInfo agent = agentInfoMapper.selectOne(new LambdaQueryWrapper<AgentInfo>()
                .eq(AgentInfo::getAgentCode, agentCode).last("LIMIT 1"));
        if (agent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "代理人不存在: " + agentCode);
        }
        return agent;
    }

    /** channel_code 取入参或上下文，仍未取到则抛业务异常 */
    private String pickChannelCode(String fromDto) {
        if (fromDto != null && !fromDto.isEmpty()) {
            return fromDto;
        }
        String ctx = ContextHolder.getChannelCode();
        if (ctx == null || ctx.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "渠道编码不能为空");
        }
        return ctx;
    }

    /** agent_code 生成：AG + 时间戳后 5 位 + 随机 3 位（渠道内唯一，依赖 uk_channel_agent_code 兜底） */
    private String generateAgentCode() {
        long ts = System.currentTimeMillis() % 100000L;
        int rand = (int) (Math.random() * 1000);
        return String.format("AG%05d%03d", ts, rand);
    }

    private AgentInfoVO toVO(AgentInfo entity) {
        AgentInfoVO vo = new AgentInfoVO();
        vo.setId(entity.getId());
        vo.setAgentCode(entity.getAgentCode());
        vo.setFullName(entity.getFullName());
        vo.setGender(entity.getGender());
        vo.setAvatar(entity.getAvatar());
        vo.setPhone(entity.getPhone());
        vo.setEmail(entity.getEmail());
        vo.setIdCard(entity.getIdCard());
        vo.setChannelCode(entity.getChannelCode());
        vo.setCompanyName(entity.getCompanyName());
        vo.setBranchName(entity.getBranchName());
        vo.setDepartment(entity.getDepartment());
        vo.setPosition(entity.getPosition());
        vo.setEmployeeNo(entity.getEmployeeNo());
        vo.setLicenseNo(entity.getLicenseNo());
        vo.setProvinceCode(entity.getProvinceCode());
        vo.setCityCode(entity.getCityCode());
        vo.setDistrictCode(entity.getDistrictCode());
        vo.setAddress(entity.getAddress());
        vo.setServiceIntro(entity.getServiceIntro());
        vo.setClientCount(entity.getClientCount());
        vo.setTotalOrderCount(entity.getTotalOrderCount());
        vo.setTotalOrderAmount(entity.getTotalOrderAmount());
        vo.setAgentLevel(entity.getAgentLevel());
        vo.setIsCertified(entity.getIsCertified());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
