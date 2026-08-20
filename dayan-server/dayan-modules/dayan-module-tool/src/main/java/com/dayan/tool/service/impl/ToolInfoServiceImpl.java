package com.dayan.tool.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.tool.dto.ToolInfoCreateDTO;
import com.dayan.tool.dto.ToolInfoQueryDTO;
import com.dayan.tool.dto.ToolInfoUpdateDTO;
import com.dayan.tool.entity.ToolInfo;
import com.dayan.tool.mapper.ToolInfoMapper;
import com.dayan.tool.model.ToolAiartistPipelineConfig;
import com.dayan.tool.model.ToolType;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolAichatPersonaVO;
import com.dayan.tool.vo.ToolAiartistConfigVO;
import com.dayan.tool.vo.ToolInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工具服务实现。
 *
 * <p>工具编码生成：{@code "TL" + String.format("%05d", sequenceProvider.next("code:seq:TL:0"))}，
 * 全表唯一。新建默认 status=1（启用）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolInfoServiceImpl implements ToolInfoService {

    /** 工具编码前缀 */
    private static final String CODE_PREFIX = "TL";
    /** 序列键 */
    private static final String SEQ_KEY = "code:seq:TL:0";
    /** 默认状态：启用 */
    private static final int DEFAULT_STATUS = 1;

    private final ToolInfoMapper toolInfoMapper;
    private final SequenceProvider sequenceProvider;
    private final ChannelConfigToolBridge channelConfigToolBridge;

    @Override
    public PageResult<ToolInfoVO> page(ToolInfoQueryDTO query) {
        LambdaQueryWrapper<ToolInfo> wrapper = buildQueryWrapper(query);
        Page<ToolInfo> page = toolInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ToolInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ToolInfoVO> list(ToolInfoQueryDTO query) {
        return toolInfoMapper.selectList(buildQueryWrapper(query)).stream().map(this::toVO).toList();
    }

    @Override
    public ToolInfoVO getDetail(String toolCode) {
        return toVO(requireTool(toolCode));
    }

    @Override
    public List<ToolInfoVO> listEnabled() {
        return toolInfoMapper.selectList(new LambdaQueryWrapper<ToolInfo>()
                        .eq(ToolInfo::getStatus, 1)
                        .orderByAsc(ToolInfo::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    public List<ToolInfoVO> listForAgent(String channelCode) {
        // 全部启用工具
        List<ToolInfoVO> allTools = toolInfoMapper.selectList(new LambdaQueryWrapper<ToolInfo>()
                        .eq(ToolInfo::getStatus, 1)
                        .orderByAsc(ToolInfo::getId))
                .stream().map(this::toVO).toList();

        // 渠道配置的工具（可能包含额外工具，需要补充）
        List<String> configuredCodes = channelConfigToolBridge.listConfiguredToolCodes(channelCode);
        if (configuredCodes.isEmpty()) {
            return allTools;
        }
        // 补充配置工具（去重）
        Set<String> existingCodes = allTools.stream().map(ToolInfoVO::getToolCode).collect(Collectors.toSet());
        List<String> newCodes = configuredCodes.stream().filter(c -> !existingCodes.contains(c)).toList();
        if (!newCodes.isEmpty()) {
            List<ToolInfoVO> configuredTools = toolInfoMapper.selectList(new LambdaQueryWrapper<ToolInfo>()
                            .in(ToolInfo::getToolCode, newCodes)
                            .eq(ToolInfo::getStatus, 1))
                    .stream().map(this::toVO).toList();
            allTools.addAll(configuredTools);
        }
        return allTools;
    }

    @Override
    public List<ToolAichatPersonaVO> listQaPersonas() {
        return toolInfoMapper.selectList(new LambdaQueryWrapper<ToolInfo>()
                        .eq(ToolInfo::getToolType, ToolType.AI_QA)
                        .eq(ToolInfo::getStatus, 1)
                        .orderByAsc(ToolInfo::getId))
                .stream().map(this::toPersona)
                .map(p -> {
                    p.setRepoIds(mergeRepoIds(p.getToolCode(), p.getRepoIds()));
                    return p;
                })
                .toList();
    }

    @Override
    public List<ToolAichatPersonaVO> listQaPersonasRaw() {
        return toolInfoMapper.selectList(new LambdaQueryWrapper<ToolInfo>()
                        .eq(ToolInfo::getToolType, ToolType.AI_QA)
                        .eq(ToolInfo::getStatus, 1)
                        .orderByAsc(ToolInfo::getId))
                .stream().map(this::toPersona)
                .toList();
    }

    @Override
    public ToolAichatPersonaVO getQaPersona(String toolCode) {
        ToolInfo tool = requireTool(toolCode);
        if (!ToolType.AI_QA.equals(tool.getToolType())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "问答人物不存在: " + toolCode);
        }
        return toPersona(tool);
    }

    @Override
    public List<ToolAiartistConfigVO> listAiartistConfigs() {
        return toolInfoMapper.selectList(new LambdaQueryWrapper<ToolInfo>()
                        .eq(ToolInfo::getToolType, ToolType.AI_CREATOR)
                        .eq(ToolInfo::getStatus, 1)
                        .orderByAsc(ToolInfo::getId))
                .stream().map(this::toAiartistConfig).toList();
    }

    @Override
    public ToolAiartistConfigVO getAiartistConfig(String toolCode) {
        ToolInfo tool = requireTool(toolCode);
        if (!ToolType.AI_CREATOR.equals(tool.getToolType())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI 创作分类不存在: " + toolCode);
        }
        return toAiartistConfig(tool);
    }

    @Override
    public ToolAiartistPipelineConfig getAiartistPipelineConfig(String toolCode) {
        ToolInfo tool = requireTool(toolCode);
        if (!ToolType.AI_CREATOR.equals(tool.getToolType())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI 创作分类不存在: " + toolCode);
        }
        return ToolAiartistPipelineConfig.parse(StrUtil.isBlank(tool.getConfigJson())
                ? null : JSONUtil.parseObj(tool.getConfigJson()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ToolInfoCreateDTO dto) {
        String toolCode = generateToolCode();

        ToolType.requireValid(dto.getToolType());
        validateConfigJson(dto.getConfigJson());
        validateQaPersona(dto.getToolType(), dto.getConfigJson());
        validateAiartistConfig(dto.getToolType(), dto.getConfigJson());

        ToolInfo entity = new ToolInfo();
        entity.setToolCode(toolCode);
        entity.setToolName(dto.getToolName());
        entity.setToolType(dto.getToolType());
        entity.setToolDesc(dto.getToolDesc());
        entity.setConfigJson(dto.getConfigJson());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());
        entity.setRemark(dto.getRemark());

        toolInfoMapper.insert(entity);
        log.info("创建工具成功: toolCode={}, toolName={}", toolCode, dto.getToolName());
        return toolCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String toolCode, ToolInfoUpdateDTO dto) {
        ToolInfo existing = requireTool(toolCode);
        ToolInfo update = new ToolInfo();
        update.setId(existing.getId());

        if (dto.getToolName() != null) update.setToolName(dto.getToolName());
        if (dto.getToolType() != null) {
            ToolType.requireValid(dto.getToolType());
            update.setToolType(dto.getToolType());
        }
        if (dto.getToolDesc() != null) update.setToolDesc(dto.getToolDesc());
        if (dto.getConfigJson() != null) {
            validateConfigJson(dto.getConfigJson());
            // 类型可能同步变更，校验用变更后的类型
            String effectiveType = dto.getToolType() != null ? dto.getToolType() : existing.getToolType();
            validateQaPersona(effectiveType, dto.getConfigJson());
            validateAiartistConfig(effectiveType, dto.getConfigJson());
            update.setConfigJson(dto.getConfigJson());
        }
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        toolInfoMapper.updateById(update);
        log.info("更新工具成功: toolCode={}", toolCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String toolCode) {
        ToolInfo existing = requireTool(toolCode);
        toolInfoMapper.deleteById(existing.getId());
        log.info("删除工具成功: toolCode={}", toolCode);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ToolInfo> buildQueryWrapper(ToolInfoQueryDTO query) {
        return new LambdaQueryWrapper<ToolInfo>()
                .eq(query.getToolCode() != null && !query.getToolCode().isEmpty(),
                        ToolInfo::getToolCode, query.getToolCode())
                .like(query.getToolName() != null && !query.getToolName().isEmpty(),
                        ToolInfo::getToolName, query.getToolName())
                .eq(query.getToolType() != null && !query.getToolType().isEmpty(),
                        ToolInfo::getToolType, query.getToolType())
                .eq(query.getStatus() != null, ToolInfo::getStatus, query.getStatus())
                .orderByDesc(ToolInfo::getCreatedAt);
    }

    private ToolInfo requireTool(String toolCode) {
        ToolInfo tool = toolInfoMapper.selectOne(new LambdaQueryWrapper<ToolInfo>()
                .eq(ToolInfo::getToolCode, toolCode)
                .last("LIMIT 1"));
        if (tool == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工具不存在: " + toolCode);
        }
        return tool;
    }

    /** 生成工具编码：TL + 5 位序列 */
    private String generateToolCode() {
        return CODE_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private void validateConfigJson(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return;
        }
        try {
            cn.hutool.json.JSONUtil.parse(configJson);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工具配置不是合法 JSON");
        }
    }

    /** aichat 类型必须配置人设描述（config_json.systemPrompt） */
    private void validateQaPersona(String toolType, String configJson) {
        if (!ToolType.AI_QA.equals(toolType)) {
            return;
        }
        String systemPrompt = StrUtil.isBlank(configJson)
                ? null : JSONUtil.parseObj(configJson).getStr("systemPrompt");
        if (StrUtil.isBlank(systemPrompt)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "你问我答类型必须配置人设描述（config_json.systemPrompt）");
        }
    }

    /** aiartist 类型必须配置创作目的与人设描述（config_json.purpose / systemPrompt） */
    private void validateAiartistConfig(String toolType, String configJson) {
        if (!ToolType.AI_CREATOR.equals(toolType)) {
            return;
        }
        JSONObject cfg = StrUtil.isBlank(configJson) ? null : JSONUtil.parseObj(configJson);
        if (cfg == null || StrUtil.isBlank(cfg.getStr("purpose"))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "AI 创作类型必须配置创作目的（config_json.purpose：science/park/product）");
        }
        if (StrUtil.isBlank(cfg.getStr("systemPrompt"))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "AI 创作类型必须配置人设描述（config_json.systemPrompt）");
        }
    }

    /** 组装问答人物：personaName 取 toolName，其余属性来自 config_json */
    private ToolAichatPersonaVO toPersona(ToolInfo tool) {
        ToolAichatPersonaVO vo = new ToolAichatPersonaVO();
        vo.setToolCode(tool.getToolCode());
        vo.setPersonaName(tool.getToolName());
        vo.setToolDesc(tool.getToolDesc());
        if (StrUtil.isNotBlank(tool.getConfigJson())) {
            JSONObject cfg = JSONUtil.parseObj(tool.getConfigJson());
            vo.setIcon(cfg.getStr("icon"));
            vo.setIconColor(cfg.getStr("iconColor"));
            vo.setSystemPrompt(cfg.getStr("systemPrompt"));
            vo.setWelcomeMsg(cfg.getStr("welcomeMsg"));
            if (cfg.getJSONArray("recommendQuestions") != null) {
                vo.setRecommendQuestions(cfg.getJSONArray("recommendQuestions").toList(String.class));
            }
            vo.setRepoIds(parseRepoIds(tool));
        }
        return vo;
    }

    /** 从 config_json 解析全局绑定的知识库 ID（缺失/非法按空处理） */
    private List<Long> parseRepoIds(ToolInfo tool) {
        if (StrUtil.isNotBlank(tool.getConfigJson())) {
            JSONObject cfg = JSONUtil.parseObj(tool.getConfigJson());
            if (cfg.getJSONArray("repoIds") != null) {
                return cfg.getJSONArray("repoIds").toList(Long.class);
            }
        }
        return List.of();
    }

    /** 组装 AI 创作分类：名称取 toolName，purpose/图标/人设来自 config_json */
    private ToolAiartistConfigVO toAiartistConfig(ToolInfo tool) {
        ToolAiartistConfigVO vo = new ToolAiartistConfigVO();
        vo.setToolCode(tool.getToolCode());
        vo.setToolName(tool.getToolName());
        vo.setToolDesc(tool.getToolDesc());
        if (StrUtil.isNotBlank(tool.getConfigJson())) {
            JSONObject cfg = JSONUtil.parseObj(tool.getConfigJson());
            vo.setPurpose(cfg.getStr("purpose"));
            vo.setIcon(cfg.getStr("icon"));
            vo.setIconColor(cfg.getStr("iconColor"));
            vo.setSystemPrompt(cfg.getStr("systemPrompt"));
        }
        return vo;
    }

    private ToolInfoVO toVO(ToolInfo entity) {
        ToolInfoVO vo = new ToolInfoVO();
        vo.setId(entity.getId());
        vo.setToolCode(entity.getToolCode());
        vo.setToolName(entity.getToolName());
        vo.setToolType(entity.getToolType());
        vo.setToolDesc(entity.getToolDesc());
        vo.setConfigJson(entity.getConfigJson());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /**
     * 合并有效知识库：全局 repoIds + 当前渠道补充 repoIds（去重保序；无渠道上下文退化为仅全局）。
     * 渠道补充从 channel_config_tool(config_type=1) 读取 config_json 中的 repoIds。
     */
    private List<Long> mergeRepoIds(String toolCode, List<Long> globalRepoIds) {
        java.util.Set<Long> merged = new java.util.LinkedHashSet<>();
        if (globalRepoIds != null && !globalRepoIds.isEmpty()) {
            merged.addAll(globalRepoIds);
        }
        String channelCode = com.dayan.common.mybatis.context.ContextHolder.getChannelCode();
        if (StrUtil.isNotBlank(channelCode)) {
            merged.addAll(channelConfigToolBridge.listChannelRepoIds(channelCode, toolCode));
        }
        return List.copyOf(merged);
    }
}
