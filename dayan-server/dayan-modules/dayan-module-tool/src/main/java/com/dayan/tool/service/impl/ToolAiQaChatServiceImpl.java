package com.dayan.tool.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dayan.common.aliyun.bailian.BailianChatClient;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.knowledge.service.KnowledgeRepoService;
import com.dayan.knowledge.vo.KnowledgeChatVO;
import com.dayan.system.service.SystemConfigService;
import com.dayan.tool.dto.ToolAiQaChatDTO;
import com.dayan.tool.entity.ToolAiQaConfig;
import com.dayan.tool.entity.ToolAiQaMessage;
import com.dayan.tool.entity.ToolAiQaSession;
import com.dayan.tool.mapper.ToolAiQaConfigMapper;
import com.dayan.tool.mapper.ToolAiQaMessageMapper;
import com.dayan.tool.mapper.ToolAiQaSessionMapper;
import com.dayan.tool.service.ToolAiQaChatListener;
import com.dayan.tool.service.ToolAiQaChatService;
import com.dayan.tool.service.ToolAiQaSessionService;
import com.dayan.tool.vo.ToolAiQaChatResultVO;
import com.dayan.tool.vo.ToolAiQaMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 问答核心服务实现。
 *
 * <p>流程：加载人物配置 → 解析绑定知识库 → 跨库检索（每库 try-catch 跳过不可见/异常库）→
 * 引用去重合并后取 top 5 → 无命中兜底 → 组装 system/user prompt → 百炼生成（chat / chatStream）→
 * 落库 user+assistant 两条消息 → 更新 session（lastMessageAt=now，messageCount+=2）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolAiQaChatServiceImpl implements ToolAiQaChatService {

    private static final int TOP_K_PER_REPO = 3;
    private static final int TOP_K_TOTAL = 5;

    private static final double GENERATE_TEMPERATURE = 0.3;

    private static final String NO_HIT_ANSWER = "知识库中未检索到相关内容，请补充资料后重试或换一种问法。";

    private final ToolAiQaConfigMapper configMapper;
    private final ToolAiQaSessionMapper sessionMapper;
    private final ToolAiQaMessageMapper messageMapper;
    private final ToolAiQaSessionService sessionService;
    private final KnowledgeRepoService knowledgeRepoService;
    private final SystemConfigService systemConfigService;
    private final BailianChatClient bailianChatClient = new BailianChatClient();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ToolAiQaChatResultVO chat(ToolAiQaChatDTO dto) {
        String agentCode = ContextHolder.getAccountCode();
        String channelCode = ContextHolder.getChannelCode();
        ToolAiQaConfig config = requireConfig(dto.getConfigId());
        String sessionCode = resolveSession(dto, agentCode, channelCode, config);

        // 1. 跨库检索 + 引用合并去重
        List<String> texts = new ArrayList<>();
        List<ToolAiQaChatResultVO.Citation> citations = retrieveCitations(dto, config, texts);

        // 2. 无命中兜底：落 user + assistant 兜底消息后返回
        if (citations.isEmpty()) {
            saveUserMessage(sessionCode, dto.getQuestion());
            saveAssistantMessage(sessionCode, NO_HIT_ANSWER, List.of());
            touchSession(sessionCode, 2);
            return result(NO_HIT_ANSWER, citations, sessionCode);
        }

        // 3. 组装 prompt + 生成
        String systemPrompt = buildSystemPrompt(config, texts);
        String answer = bailianChatClient.chat(
                getConfig("llm.api-key"), getConfig("llm.api-host"),
                StrUtil.blankToDefault(getConfig("llm.chat-model"), "qwen-plus"),
                systemPrompt, dto.getQuestion());

        // 4. 落库 + 更新 session
        saveUserMessage(sessionCode, dto.getQuestion());
        saveAssistantMessage(sessionCode, answer, citations);
        touchSession(sessionCode, 2);
        return result(answer, citations, sessionCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ToolAiQaChatResultVO chatStreamBlocking(ToolAiQaChatDTO dto, ToolAiQaChatListener listener) {
        String agentCode = ContextHolder.getAccountCode();
        String channelCode = ContextHolder.getChannelCode();
        ToolAiQaConfig config = requireConfig(dto.getConfigId());
        String sessionCode = resolveSession(dto, agentCode, channelCode, config);

        // 1. 跨库检索 + 引用合并去重
        List<String> texts = new ArrayList<>();
        List<ToolAiQaChatResultVO.Citation> citations = retrieveCitations(dto, config, texts);

        // 2. 无命中兜底：落 user + assistant 兜底消息后返回
        if (citations.isEmpty()) {
            if (listener != null) {
                listener.onStage("retrieval", "done");
                listener.onStage("generate", "empty");
            }
            saveUserMessage(sessionCode, dto.getQuestion());
            saveAssistantMessage(sessionCode, NO_HIT_ANSWER, List.of());
            touchSession(sessionCode, 2);
            return result(NO_HIT_ANSWER, citations, sessionCode);
        }

        // 3. 组装 prompt + 流式生成（listener.onDelta 逐段推送）
        if (listener != null) {
            listener.onStage("retrieval", "done");
            listener.onStage("generate", "stream");
        }
        String systemPrompt = buildSystemPrompt(config, texts);
        String answer = bailianChatClient.chatStream(
                getConfig("llm.api-key"), getConfig("llm.api-host"),
                StrUtil.blankToDefault(getConfig("llm.chat-model"), "qwen-plus"),
                systemPrompt, dto.getQuestion(), GENERATE_TEMPERATURE,
                listener == null ? null : text -> listener.onDelta(text));

        // 4. 落库 + 更新 session
        saveUserMessage(sessionCode, dto.getQuestion());
        saveAssistantMessage(sessionCode, answer, citations);
        touchSession(sessionCode, 2);
        return result(answer, citations, sessionCode);
    }

    @Override
    public List<ToolAiQaMessageVO> listMessages(String sessionCode) {
        String agentCode = ContextHolder.getAccountCode();
        // session 归属校验（防越权）
        ToolAiQaSession session = sessionMapper.selectOne(new LambdaQueryWrapper<ToolAiQaSession>()
                .eq(ToolAiQaSession::getSessionCode, sessionCode)
                .eq(ToolAiQaSession::getAgentCode, agentCode)
                .last("LIMIT 1"));
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        List<ToolAiQaMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<ToolAiQaMessage>()
                .eq(ToolAiQaMessage::getSessionCode, sessionCode)
                .orderByAsc(ToolAiQaMessage::getId));
        return messages.stream().map(this::toMessageVO).collect(Collectors.toList());
    }

    // ==================== 内部工具 ====================

    /** 加载人物配置，不存在抛 NOT_FOUND */
    private ToolAiQaConfig requireConfig(Long configId) {
        ToolAiQaConfig config = configMapper.selectById(configId);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "人物配置不存在: " + configId);
        }
        return config;
    }

    /**
     * 解析会话：sessionCode 非空时校验归属 + 人物一致；为空时新建会话（首轮问答）。
     */
    private String resolveSession(ToolAiQaChatDTO dto, String agentCode, String channelCode, ToolAiQaConfig config) {
        if (StrUtil.isBlank(dto.getSessionCode())) {
            return sessionService.create(agentCode, channelCode, config.getId());
        }
        ToolAiQaSession session = sessionMapper.selectOne(new LambdaQueryWrapper<ToolAiQaSession>()
                .eq(ToolAiQaSession::getSessionCode, dto.getSessionCode())
                .eq(ToolAiQaSession::getAgentCode, agentCode)
                .last("LIMIT 1"));
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        if (session.getConfigId() == null || !session.getConfigId().equals(config.getId())) {
            throw new BusinessException(ErrorCode.BUSINESS, "会话人物与请求人物不一致");
        }
        return session.getSessionCode();
    }

    /** 跨库检索：每库 try-catch 跳过不可见/异常库，text 去重，合并后取 top 5 */
    private List<ToolAiQaChatResultVO.Citation> retrieveCitations(ToolAiQaChatDTO dto, ToolAiQaConfig config,
                                                                   List<String> texts) {
        List<ToolAiQaChatResultVO.Citation> citations = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<Long> repoIds = parseLongList(config.getRepoIds());
        for (Long repoId : repoIds) {
            try {
                knowledgeRepoService.requireRepoVisible(repoId);  // 显式渠道可见性校验
                List<KnowledgeChatVO.Citation> cites =
                        knowledgeRepoService.retrieve(repoId, dto.getQuestion(), TOP_K_PER_REPO);
                for (KnowledgeChatVO.Citation c : cites) {
                    String text = StrUtil.cleanBlank(c.getText());
                    if (StrUtil.isBlank(text) || !seen.add(text)) {
                        continue;
                    }
                    ToolAiQaChatResultVO.Citation cl = new ToolAiQaChatResultVO.Citation();
                    cl.setText(text);
                    cl.setScore(c.getScore());
                    cl.setRepoId(repoId);
                    cl.setRepoName(resolveRepoName(repoId));
                    citations.add(cl);
                    texts.add("[" + citations.size() + "] " + text);
                    if (citations.size() >= TOP_K_TOTAL) {
                        return citations;
                    }
                }
            } catch (Exception e) {
                log.warn("会话检索跳过库 repoId={}: {}", repoId, e.getMessage());
            }
        }
        return citations;
    }

    /** 关联仓库名（第一期只到 repo 级，docId/docName 留空）；失败容错为空，不阻断检索 */
    private String resolveRepoName(Long repoId) {
        try {
            return knowledgeRepoService.getDetail(repoId).getRepoName();
        } catch (Exception e) {
            log.warn("关联仓库名失败 repoId={}: {}", repoId, e.getMessage());
            return null;
        }
    }

    /** 组装 system prompt：人设 + 引用约束 + 编号后的检索文本 */
    private String buildSystemPrompt(ToolAiQaConfig config, List<String> texts) {
        return config.getSystemPrompt()
                + "\n\n回答时必须仅依据下方【知识库资料】，不得编造资料外内容；回答用简体中文，条理清晰。\n【知识库资料】\n"
                + String.join("\n", texts);
    }

    private void saveUserMessage(String sessionCode, String content) {
        ToolAiQaMessage msg = new ToolAiQaMessage();
        msg.setSessionCode(sessionCode);
        msg.setRole("user");
        msg.setContent(content);
        messageMapper.insert(msg);
    }

    private void saveAssistantMessage(String sessionCode, String content, List<ToolAiQaChatResultVO.Citation> citations) {
        ToolAiQaMessage msg = new ToolAiQaMessage();
        msg.setSessionCode(sessionCode);
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setCitations(JSONUtil.toJsonStr(citations));
        messageMapper.insert(msg);
    }

    /** 更新会话：lastMessageAt=now，messageCount 原子增量 delta（每次问答 +2，无命中历史兼容 +1） */
    private void touchSession(String sessionCode, int delta) {
        sessionMapper.update(null, new LambdaUpdateWrapper<ToolAiQaSession>()
                .eq(ToolAiQaSession::getSessionCode, sessionCode)
                .setSql("message_count = IFNULL(message_count, 0) + " + delta)
                .set(ToolAiQaSession::getLastMessageAt, LocalDateTime.now()));
    }

    private ToolAiQaChatResultVO result(String answer, List<ToolAiQaChatResultVO.Citation> citations, String sessionCode) {
        ToolAiQaChatResultVO vo = new ToolAiQaChatResultVO();
        vo.setAnswer(answer);
        vo.setCitations(citations);
        vo.setSessionCode(sessionCode);
        return vo;
    }

    private ToolAiQaMessageVO toMessageVO(ToolAiQaMessage m) {
        ToolAiQaMessageVO vo = new ToolAiQaMessageVO();
        vo.setId(m.getId());
        vo.setSessionCode(m.getSessionCode());
        vo.setRole(m.getRole());
        vo.setContent(m.getContent());
        vo.setCitations(StrUtil.isBlank(m.getCitations())
                ? null
                : JSONUtil.toList(m.getCitations(), ToolAiQaChatResultVO.Citation.class));
        return vo;
    }

    /** 读取 llm 分组配置 */
    private String getConfig(String configKey) {
        return systemConfigService.getValue("llm", configKey);
    }

    private List<Long> parseLongList(String json) {
        return StrUtil.isBlank(json) ? List.of() : JSONUtil.toList(json, Long.class);
    }
}