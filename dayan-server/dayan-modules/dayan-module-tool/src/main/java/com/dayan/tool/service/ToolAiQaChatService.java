package com.dayan.tool.service;

import com.dayan.tool.dto.ToolAiQaChatDTO;
import com.dayan.tool.vo.ToolAiQaChatResultVO;
import com.dayan.tool.vo.ToolAiQaMessageVO;

import java.util.List;

/**
 * AI 问答核心服务：跨库检索 + 百炼生成 + 引用绑定 + 消息落库。
 */
public interface ToolAiQaChatService {

    /** 非流式问答：检索 + 生成 + 落库，返回答案与引用 */
    ToolAiQaChatResultVO chat(ToolAiQaChatDTO dto);

    /**
     * 流式问答（阻塞式）：内部用 {@code BailianChatClient.chatStream} 逐 delta 拼接，
     * 通过 {@code listener.onDelta} 逐段推送，最后落库返回完整结果。
     */
    ToolAiQaChatResultVO chatStreamBlocking(ToolAiQaChatDTO dto, ToolAiQaChatListener listener);

    /** 查询会话消息历史（含 session 归属校验，防越权） */
    List<ToolAiQaMessageVO> listMessages(String sessionCode);
}