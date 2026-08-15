package com.dayan.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.agent.dto.AgentContentCreateDTO;
import com.dayan.agent.dto.AgentContentQueryDTO;
import com.dayan.agent.dto.AgentContentUpdateDTO;
import com.dayan.agent.entity.AgentContent;
import com.dayan.agent.mapper.AgentContentMapper;
import com.dayan.agent.service.AgentContentService;
import com.dayan.agent.vo.AgentContentVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.mybatis.context.ContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 个人内容服务实现：agentCode 强制取自登录上下文（防越权），
 * channel_code 由租户拦截器自动隔离。
 */
@Service
@RequiredArgsConstructor
public class AgentContentServiceImpl implements AgentContentService {

    private final AgentContentMapper agentContentMapper;

    @Override
    public PageResult<AgentContentVO> page(AgentContentQueryDTO query) {
        String agentCode = requireAgentCode();
        LambdaQueryWrapper<AgentContent> wrapper = new LambdaQueryWrapper<AgentContent>()
                .eq(AgentContent::getAgentCode, agentCode)
                .eq(query.getContentType() != null, AgentContent::getContentType, query.getContentType())
                .like(StrUtil.isNotBlank(query.getKeyword()), AgentContent::getTitle, query.getKeyword())
                .orderByDesc(AgentContent::getId);
        Page<AgentContent> page = agentContentMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<AgentContentVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public AgentContentVO getDetail(Long id) {
        return toVO(requireOwned(id));
    }

    @Override
    public Long create(AgentContentCreateDTO dto) {
        AgentContent entity = new AgentContent();
        entity.setAgentCode(requireAgentCode());
        entity.setChannelCode(ContextHolder.getChannelCode());
        entity.setTitle(dto.getTitle());
        entity.setSummary(dto.getSummary());
        entity.setCoverImage(dto.getCoverImage());
        entity.setContentType(dto.getContentType());
        entity.setContentBody(dto.getContentBody());
        entity.setStyleCode(dto.getStyleCode());
        entity.setRefContentCode(dto.getRefContentCode());
        entity.setRefKbFiles(dto.getRefKbFiles());
        entity.setRefGoodsCodes(dto.getRefGoodsCodes());
        entity.setStatus(1);
        agentContentMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, AgentContentUpdateDTO dto) {
        AgentContent entity = requireOwned(id);
        if (dto.getTitle() != null) {
            entity.setTitle(dto.getTitle());
        }
        if (dto.getSummary() != null) {
            entity.setSummary(dto.getSummary());
        }
        if (dto.getCoverImage() != null) {
            entity.setCoverImage(dto.getCoverImage());
        }
        if (dto.getContentType() != null) {
            entity.setContentType(dto.getContentType());
        }
        if (dto.getContentBody() != null) {
            entity.setContentBody(dto.getContentBody());
        }
        if (dto.getStyleCode() != null) {
            entity.setStyleCode(dto.getStyleCode());
        }
        agentContentMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        requireOwned(id);
        agentContentMapper.deleteById(id);
    }

    /** 本人内容校验：agentCode 不匹配视为不存在（不泄露他人内容存在性） */
    private AgentContent requireOwned(Long id) {
        AgentContent entity = agentContentMapper.selectById(id);
        if (entity == null || !requireAgentCode().equals(entity.getAgentCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在");
        }
        return entity;
    }

    private String requireAgentCode() {
        String agentCode = ContextHolder.getAccountCode();
        if (StrUtil.isBlank(agentCode)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return agentCode;
    }

    private AgentContentVO toVO(AgentContent entity) {
        AgentContentVO vo = new AgentContentVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
