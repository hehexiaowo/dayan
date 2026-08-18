package com.dayan.tool.controller.agent;

import com.dayan.common.core.resp.R;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.tool.dto.ToolAichatChatDTO;
import com.dayan.tool.service.ToolAichatChatService;
import com.dayan.tool.service.ToolAichatSessionService;
import com.dayan.tool.service.ToolAichatStreamService;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolAichatChatResultVO;
import com.dayan.tool.vo.ToolAichatMessageVO;
import com.dayan.tool.vo.ToolAichatPersonaVO;
import com.dayan.tool.vo.ToolAichatSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * Agent 端 AI 问答接口。
 *
 * <p>路径：{@code /tools/aichat}（agent starter context-path 拼接为 {@code /agent-api/tools/aichat/*}）。
 * 问答人物 = tool_info 的 aichat 实例，人物标识用 toolCode。
 */
@Tag(name = "Agent AI 问答")
@RestController
@RequestMapping("/tools/aichat")
@RequiredArgsConstructor
public class AgentToolAichatController {

    private final ToolInfoService toolInfoService;
    private final ToolAichatSessionService sessionService;
    private final ToolAichatChatService chatService;
    private final ToolAichatStreamService streamService;

    @Operation(summary = "可用人物列表（tool_type=aichat 且启用）")
    @GetMapping("/configs")
    public R<List<ToolAichatPersonaVO>> configs() {
        return R.ok(toolInfoService.listQaPersonas());
    }

    @Operation(summary = "某人物下我的会话列表")
    @GetMapping("/sessions")
    public R<List<ToolAichatSessionVO>> sessions(@RequestParam String toolCode) {
        if (toolCode == null || toolCode.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "人物编码不能为空");
        }
        String agentCode = ContextHolder.getAccountCode();
        return R.ok(sessionService.listByTool(agentCode, toolCode));
    }

    @Operation(summary = "新建会话")
    @PostMapping("/sessions")
    @OperationLog(module = "AI 问答", action = "新建会话")
    public R<String> createSession(@RequestBody Map<String, Object> body) {
        String agentCode = ContextHolder.getAccountCode();
        String channelCode = ContextHolder.getChannelCode();
        String toolCode = body.get("toolCode") == null ? null : String.valueOf(body.get("toolCode"));
        if (toolCode == null || toolCode.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "人物编码不能为空");
        }
        return R.ok(sessionService.create(agentCode, channelCode, toolCode));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/sessions/{sessionCode}")
    @OperationLog(module = "AI 问答", action = "删除会话")
    public R<Void> deleteSession(@PathVariable String sessionCode) {
        sessionService.delete(ContextHolder.getAccountCode(), sessionCode);
        return R.ok();
    }

    @Operation(summary = "会话消息历史")
    @GetMapping("/messages/{sessionCode}")
    public R<List<ToolAichatMessageVO>> messages(@PathVariable String sessionCode) {
        return R.ok(chatService.listMessages(sessionCode));
    }

    @Operation(summary = "问答（JSON）")
    @PostMapping("/chat")
    public R<ToolAichatChatResultVO> chat(@RequestBody @Valid ToolAichatChatDTO dto) {
        return R.ok(chatService.chat(dto));
    }

    @Operation(summary = "问答（SSE 流式）")
    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@RequestBody @Valid ToolAichatChatDTO dto) {
        return streamService.chatStream(dto);
    }
}
