package com.dayan.tool.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.tool.dto.ToolAiQaConfigCreateDTO;
import com.dayan.tool.dto.ToolAiQaConfigQueryDTO;
import com.dayan.tool.dto.ToolAiQaConfigUpdateDTO;
import com.dayan.tool.entity.ToolAiQaConfig;
import com.dayan.tool.mapper.ToolAiQaConfigMapper;
import com.dayan.tool.service.ToolAiQaConfigService;
import com.dayan.tool.vo.ToolAiQaConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 问答人物配置服务：推荐问题、绑定知识库以 JSON 数组字符串存储。
 */
@Service
@RequiredArgsConstructor
public class ToolAiQaConfigServiceImpl implements ToolAiQaConfigService {

    private final ToolAiQaConfigMapper configMapper;
    private final CodeGenerator codeGenerator;

    @Override
    public PageResult<ToolAiQaConfigVO> page(ToolAiQaConfigQueryDTO query) {
        LambdaQueryWrapper<ToolAiQaConfig> wrapper = new LambdaQueryWrapper<ToolAiQaConfig>()
                .like(StrUtil.isNotBlank(query.getPersonaName()), ToolAiQaConfig::getPersonaName, query.getPersonaName())
                .eq(query.getStatus() != null, ToolAiQaConfig::getStatus, query.getStatus())
                .orderByAsc(ToolAiQaConfig::getSortOrder)
                .orderByDesc(ToolAiQaConfig::getId);
        Page<ToolAiQaConfig> page = configMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ToolAiQaConfigVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ToolAiQaConfigVO> listEnabled() {
        return configMapper.selectList(new LambdaQueryWrapper<ToolAiQaConfig>()
                .eq(ToolAiQaConfig::getStatus, 1)
                .orderByAsc(ToolAiQaConfig::getSortOrder)
                .orderByDesc(ToolAiQaConfig::getId)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ToolAiQaConfigVO getDetail(Long id) {
        return toVO(require(id));
    }

    @Override
    public String create(ToolAiQaConfigCreateDTO dto) {
        ToolAiQaConfig config = new ToolAiQaConfig();
        config.setConfigCode(codeGenerator.generate("QAC"));
        config.setPersonaName(dto.getPersonaName());
        config.setIcon(dto.getIcon());
        config.setIconColor(StrUtil.blankToDefault(dto.getIconColor(), "blue"));
        config.setSystemPrompt(dto.getSystemPrompt());
        config.setWelcomeMsg(dto.getWelcomeMsg());
        config.setRecommendQuestions(toJson(dto.getRecommendQuestions()));
        config.setRepoIds(toJson(dto.getRepoIds()));
        config.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        config.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        config.setRemark(dto.getRemark());
        configMapper.insert(config);
        return config.getConfigCode();
    }

    @Override
    public void update(Long id, ToolAiQaConfigUpdateDTO dto) {
        ToolAiQaConfig config = require(id);
        if (dto.getPersonaName() != null) config.setPersonaName(dto.getPersonaName());
        if (dto.getIcon() != null) config.setIcon(dto.getIcon());
        if (dto.getIconColor() != null) config.setIconColor(dto.getIconColor());
        if (dto.getSystemPrompt() != null) config.setSystemPrompt(dto.getSystemPrompt());
        if (dto.getWelcomeMsg() != null) config.setWelcomeMsg(dto.getWelcomeMsg());
        if (dto.getRecommendQuestions() != null) config.setRecommendQuestions(toJson(dto.getRecommendQuestions()));
        if (dto.getRepoIds() != null) config.setRepoIds(toJson(dto.getRepoIds()));
        if (dto.getSortOrder() != null) config.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) config.setStatus(dto.getStatus());
        if (dto.getRemark() != null) config.setRemark(dto.getRemark());
        configMapper.updateById(config);
    }

    @Override
    public void delete(Long id) {
        configMapper.deleteById(id);
    }

    private ToolAiQaConfig require(Long id) {
        ToolAiQaConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "人物配置不存在: " + id);
        }
        return config;
    }

    private ToolAiQaConfigVO toVO(ToolAiQaConfig c) {
        ToolAiQaConfigVO vo = new ToolAiQaConfigVO();
        vo.setId(c.getId());
        vo.setConfigCode(c.getConfigCode());
        vo.setPersonaName(c.getPersonaName());
        vo.setIcon(c.getIcon());
        vo.setIconColor(c.getIconColor());
        vo.setSystemPrompt(c.getSystemPrompt());
        vo.setWelcomeMsg(c.getWelcomeMsg());
        vo.setRecommendQuestions(fromJsonList(c.getRecommendQuestions()));
        vo.setRepoIds(fromJsonLongList(c.getRepoIds()));
        vo.setSortOrder(c.getSortOrder());
        vo.setStatus(c.getStatus());
        vo.setRemark(c.getRemark());
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }

    private String toJson(Object list) {
        return list == null ? null : JSONUtil.toJsonStr(list);
    }

    private List<String> fromJsonList(String json) {
        return StrUtil.isBlank(json) ? null : JSONUtil.toList(json, String.class);
    }

    private List<Long> fromJsonLongList(String json) {
        return StrUtil.isBlank(json) ? null : JSONUtil.toList(json, Long.class);
    }
}
