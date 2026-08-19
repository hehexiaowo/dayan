package com.dayan.tool.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dayan.common.aliyun.bailian.BailianChatClient;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.system.service.SystemKnowledgeRepoService;
import com.dayan.system.vo.SystemKnowledgeChatVO;
import com.dayan.tool.dto.ToolAichatChatDTO;
import com.dayan.tool.entity.ToolAichatMessage;
import com.dayan.tool.entity.ToolAichatSession;
import com.dayan.tool.mapper.ToolAichatMessageMapper;
import com.dayan.tool.mapper.ToolAichatSessionMapper;
import com.dayan.tool.service.AiClientHolder;
import com.dayan.tool.service.ToolAichatChatListener;
import com.dayan.tool.service.ToolAichatChatService;
import com.dayan.tool.service.ToolAichatSessionService;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolAichatChatResultVO;
import com.dayan.tool.vo.ToolAichatMessageVO;
import com.dayan.tool.vo.ToolAichatPersonaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 问答核心服务实现。
 *
 * <p>流程：加载问答人物（tool_info 的 aichat 实例）→ 解析绑定知识库 → 跨库检索（每库 try-catch 跳过
 * 不可见/异常库）→ 引用去重合并后取 top 5 → 无命中兜底 → 组装 system/user prompt → 百炼生成
 * （chat / chatStream）→ 落库 user+assistant 两条消息 → 更新 session（lastMessageAt=now，messageCount+=2）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolAichatChatServiceImpl implements ToolAichatChatService {

    private static final int TOP_K_PER_REPO = 3;
    private static final int TOP_K_TOTAL = 5;

    private static final double GENERATE_TEMPERATURE = 0.3;

    private static final String NO_HIT_ANSWER = "知识库中未检索到相关内容，请补充资料后重试或换一种问法。";
    private static final int HISTORY_LIMIT = 12;
    private static final int HISTORY_CONTENT_LIMIT = 4000;

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_SYSTEM = "system";

    private final ToolAichatSessionMapper sessionMapper;
    private final ToolAichatMessageMapper messageMapper;
    private final ToolAichatSessionService sessionService;
    private final ToolInfoService toolInfoService;
    private final SystemKnowledgeRepoService knowledgeRepoService;
    private final AiClientHolder aiClientHolder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ToolAichatChatResultVO chat(ToolAichatChatDTO dto) {
        String agentCode = ContextHolder.getAccountCode();
        String channelCode = ContextHolder.getChannelCode();
        ToolAichatPersonaVO persona = requirePersona(dto.getToolCode());
        String sessionCode = resolveSession(dto, agentCode, channelCode, persona);

        // 1. 跨库检索 + 引用合并去重
        List<String> texts = new ArrayList<>();
        List<ToolAichatChatResultVO.Citation> citations = retrieveCitations(dto, persona, texts);

        // 2. 无命中兜底：落 user + assistant 兜底消息后返回
        if (citations.isEmpty()) {
            saveUserMessage(sessionCode, dto.getQuestion());
            saveAssistantMessage(sessionCode, NO_HIT_ANSWER, List.of());
            touchSession(sessionCode, 2);
            return result(NO_HIT_ANSWER, citations, sessionCode);
        }

        // 3. 组装 prompt + 生成
        String systemPrompt = buildSystemPrompt(persona, texts);
        String answer = aiClientHolder.getChatClient().chat(
                aiClientHolder.requireConfig("llm.api-key", "AI 问答 API-Key 未配置"),
                aiClientHolder.requireConfig("llm.api-host", "AI 问答网关域名未配置"),
                aiClientHolder.chatModel(),
                buildConversationMessages(sessionCode, dto.getQuestion(), systemPrompt), GENERATE_TEMPERATURE);

        // 4. 落库 + 更新 session
        saveUserMessage(sessionCode, dto.getQuestion());
        saveAssistantMessage(sessionCode, answer, citations);
        touchSession(sessionCode, 2);
        return result(answer, citations, sessionCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ToolAichatChatResultVO chatStreamBlocking(ToolAichatChatDTO dto, ToolAichatChatListener listener) {
        String agentCode = ContextHolder.getAccountCode();
        String channelCode = ContextHolder.getChannelCode();
        ToolAichatPersonaVO persona = requirePersona(dto.getToolCode());
        String sessionCode = resolveSession(dto, agentCode, channelCode, persona);

        // 1. 跨库检索 + 引用合并去重
        List<String> texts = new ArrayList<>();
        List<ToolAichatChatResultVO.Citation> citations = retrieveCitations(dto, persona, texts);

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
        String systemPrompt = buildSystemPrompt(persona, texts);
        String answer = aiClientHolder.getChatClient().chatStream(
                aiClientHolder.requireConfig("llm.api-key", "AI 问答 API-Key 未配置"),
                aiClientHolder.requireConfig("llm.api-host", "AI 问答网关域名未配置"),
                aiClientHolder.chatModel(),
                systemPrompt, buildConversationPrompt(sessionCode, dto.getQuestion()), GENERATE_TEMPERATURE,
                listener == null ? null : text -> listener.onDelta(text));

        // 4. 落库 + 更新 session
        saveUserMessage(sessionCode, dto.getQuestion());
        saveAssistantMessage(sessionCode, answer, citations);
        touchSession(sessionCode, 2);
        return result(answer, citations, sessionCode);
    }

    @Override
    public List<ToolAichatMessageVO> listMessages(String sessionCode) {
        String agentCode = ContextHolder.getAccountCode();
        // session 归属校验（防越权）
        ToolAichatSession session = sessionMapper.selectOne(new LambdaQueryWrapper<ToolAichatSession>()
                .eq(ToolAichatSession::getSessionCode, sessionCode)
                .eq(ToolAichatSession::getAgentCode, agentCode)
                .last("LIMIT 1"));
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        List<ToolAichatMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<ToolAichatMessage>()
                .eq(ToolAichatMessage::getSessionCode, sessionCode)
                .orderByAsc(ToolAichatMessage::getId));
        return messages.stream().map(this::toMessageVO).collect(Collectors.toList());
    }

    // ==================== 内部工具 ====================

    /** 加载问答人物（tool_info 的 aichat 实例），不存在抛 NOT_FOUND */
    private ToolAichatPersonaVO requirePersona(String toolCode) {
        ToolAichatPersonaVO persona = toolInfoService.getQaPersona(toolCode);
        if (persona == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "问答人物不存在: " + toolCode);
        }
        return persona;
    }

    /**
     * 解析会话：sessionCode 非空时校验归属 + 人物一致；为空时新建会话（首轮问答）。
     */
    private String resolveSession(ToolAichatChatDTO dto, String agentCode, String channelCode,
                                  ToolAichatPersonaVO persona) {
        if (StrUtil.isBlank(dto.getSessionCode())) {
            return sessionService.create(agentCode, channelCode, persona.getToolCode());
        }
        ToolAichatSession session = sessionMapper.selectOne(new LambdaQueryWrapper<ToolAichatSession>()
                .eq(ToolAichatSession::getSessionCode, dto.getSessionCode())
                .eq(ToolAichatSession::getAgentCode, agentCode)
                .last("LIMIT 1"));
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        if (session.getToolCode() == null || !session.getToolCode().equals(persona.getToolCode())) {
            throw new BusinessException(ErrorCode.BUSINESS, "会话人物与请求人物不一致");
        }
        return session.getSessionCode();
    }

    /** 跨库检索：每库 try-catch 跳过不可见/异常库，text 去重，合并后取 top 5 */
    private List<ToolAichatChatResultVO.Citation> retrieveCitations(ToolAichatChatDTO dto, ToolAichatPersonaVO persona,
                                                                   List<String> texts) {
        List<ToolAichatChatResultVO.Citation> citations = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<Long> repoIds = persona.getRepoIds() == null ? List.of() : persona.getRepoIds();
        for (Long repoId : repoIds) {
            try {
                knowledgeRepoService.requireRepoVisible(repoId);  // 显式渠道可见性校验
                List<SystemKnowledgeChatVO.Citation> cites =
                        knowledgeRepoService.retrieve(repoId, dto.getQuestion(), TOP_K_PER_REPO);
                for (SystemKnowledgeChatVO.Citation c : cites) {
                    String text = StrUtil.cleanBlank(c.getText());
                    if (StrUtil.isBlank(text) || !seen.add(text)) {
                        continue;
                    }
                    ToolAichatChatResultVO.Citation cl = new ToolAichatChatResultVO.Citation();
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
    private String buildSystemPrompt(ToolAichatPersonaVO persona, List<String> texts) {
        return persona.getSystemPrompt()
                + "\n\n回答时必须仅依据下方【知识库资料】，不得编造资料外内容；回答用简体中文，条理清晰。\n【知识库资料】\n"
                + String.join("\n", texts);
    }

    private List<BailianChatClient.Message> buildConversationMessages(String sessionCode, String question,
                                                                        String systemPrompt) {
        List<BailianChatClient.Message> messages = new ArrayList<>();
        messages.add(new BailianChatClient.Message(ROLE_SYSTEM, systemPrompt));
        for (ToolAichatMessage message : loadHistory(sessionCode, question)) {
            messages.add(new BailianChatClient.Message(message.getRole(), limitContent(message.getContent())));
        }
        messages.add(new BailianChatClient.Message(ROLE_USER, question));
        return messages;
    }

    private String buildConversationPrompt(String sessionCode, String question) {
        StringBuilder prompt = new StringBuilder();
        for (ToolAichatMessage message : loadHistory(sessionCode, question)) {
            prompt.append(ROLE_USER.equals(message.getRole()) ? "用户" : "助手")
                    .append("：").append(limitContent(message.getContent())).append("\n");
        }
        prompt.append("用户：").append(question);
        return prompt.toString();
    }

    private List<ToolAichatMessage> loadHistory(String sessionCode, String question) {
        List<ToolAichatMessage> history = messageMapper.selectList(new LambdaQueryWrapper<ToolAichatMessage>()
                .eq(ToolAichatMessage::getSessionCode, sessionCode)
                .in(ToolAichatMessage::getRole, List.of(ROLE_USER, ROLE_ASSISTANT))
                .orderByDesc(ToolAichatMessage::getId)
                .last("LIMIT " + HISTORY_LIMIT));
        Collections.reverse(history);
        return history;
    }

    private String limitContent(String content) {
        return content == null ? "" : StrUtil.maxLength(content, HISTORY_CONTENT_LIMIT);
    }

    private void saveUserMessage(String sessionCode, String content) {
        ToolAichatMessage msg = new ToolAichatMessage();
        msg.setSessionCode(sessionCode);
        msg.setRole(ROLE_USER);
        msg.setContent(content);
        messageMapper.insert(msg);
    }

    private void saveAssistantMessage(String sessionCode, String content, List<ToolAichatChatResultVO.Citation> citations) {
        ToolAichatMessage msg = new ToolAichatMessage();
        msg.setSessionCode(sessionCode);
        msg.setRole(ROLE_ASSISTANT);
        msg.setContent(content);
        msg.setCitations(JSONUtil.toJsonStr(citations));
        messageMapper.insert(msg);
    }

    /**
     * 更新会话：lastMessageAt=now，messageCount 原子增量 delta（每次问答 +2）。
     * 首次交互时用第一条用户消息作为标题（单条条件 UPDATE，无竞态）。
     */
    private void touchSession(String sessionCode, int delta) {
        // 尝试获取首条用户消息作为标题（仅 message_count=0 时生效）
        String titleCandidate = null;
        ToolAichatMessage firstQuestion = messageMapper.selectOne(new LambdaQueryWrapper<ToolAichatMessage>()
                .eq(ToolAichatMessage::getSessionCode, sessionCode)
                .eq(ToolAichatMessage::getRole, ROLE_USER)
                .orderByAsc(ToolAichatMessage::getId).last("LIMIT 1"));
        if (firstQuestion != null && StrUtil.isNotBlank(firstQuestion.getContent())) {
            titleCandidate = limitTitle(firstQuestion.getContent());
        }

        // 单条原子 UPDATE：message_count 增量 + 条件设置标题 + 更新 lastMessageAt
        LambdaUpdateWrapper<ToolAichatSession> wrapper = new LambdaUpdateWrapper<ToolAichatSession>()
                .eq(ToolAichatSession::getSessionCode, sessionCode)
                .setSql("message_count = IFNULL(message_count, 0) + {0}", delta)
                .set(ToolAichatSession::getLastMessageAt, LocalDateTime.now());
        if (titleCandidate != null) {
            // 仅当 message_count 为 0（首条消息）时设置标题，后续消息不覆盖
            wrapper.setSql("title = CASE WHEN IFNULL(message_count, 0) = 0 THEN {0} ELSE title END",
                    titleCandidate);
        }
        sessionMapper.update(null, wrapper);
    }

    private String limitTitle(String title) {
        int count = title.codePointCount(0, title.length());
        return count <= 30 ? title : title.substring(0, title.offsetByCodePoints(0, 30));
    }

    private ToolAichatChatResultVO result(String answer, List<ToolAichatChatResultVO.Citation> citations, String sessionCode) {
        ToolAichatChatResultVO vo = new ToolAichatChatResultVO();
        vo.setAnswer(answer);
        vo.setCitations(citations);
        vo.setSessionCode(sessionCode);
        return vo;
    }

    private ToolAichatMessageVO toMessageVO(ToolAichatMessage m) {
        ToolAichatMessageVO vo = new ToolAichatMessageVO();
        vo.setId(m.getId());
        vo.setSessionCode(m.getSessionCode());
        vo.setRole(m.getRole());
        vo.setContent(m.getContent());
        vo.setCitations(StrUtil.isBlank(m.getCitations())
                ? null
                : JSONUtil.toList(m.getCitations(), ToolAichatChatResultVO.Citation.class));
        return vo;
    }

}