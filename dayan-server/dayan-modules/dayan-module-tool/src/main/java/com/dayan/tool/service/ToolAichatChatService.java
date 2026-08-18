package com.dayan.tool.service;

import com.dayan.tool.dto.ToolAichatChatDTO;
import com.dayan.tool.vo.ToolAichatChatResultVO;
import com.dayan.tool.vo.ToolAichatMessageVO;

import java.util.List;

/**
 * AI 问答核心服务：跨库检索 + 百炼生成 + 引用绑定 + 消息落库。
 */
public interface ToolAichatChatService {

    /** 非流式问答：检索 + 生成 + 落库，返回答案与引用 */
    ToolAichatChatResultVO chat(ToolAichatChatDTO dto);

    /**
     * 流式问答（阻塞式）：内部用 {@code BailianChatClient.chatStream} 逐 delta 拼接，
     * 通过 {@code listener.onDelta} 逐段推送，最后落库返回完整结果。
     */
    ToolAichatChatResultVO chatStreamBlocking(ToolAichatChatDTO dto, ToolAichatChatListener listener);

    /** 查询会话消息历史（含 session 归属校验，防越权） */
    List<ToolAichatMessageVO> listMessages(String sessionCode);
}