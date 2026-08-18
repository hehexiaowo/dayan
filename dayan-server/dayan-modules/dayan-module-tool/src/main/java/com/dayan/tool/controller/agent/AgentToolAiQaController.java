package com.dayan.tool.controller.agent;

import com.dayan.common.core.resp.R;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.tool.dto.ToolAiQaChatDTO;
import com.dayan.tool.service.ToolAiQaChatService;
import com.dayan.tool.service.ToolAiQaConfigService;
import com.dayan.tool.service.ToolAiQaSessionService;
import com.dayan.tool.service.ToolAiQaStreamService;
import com.dayan.tool.vo.ToolAiQaChatResultVO;
import com.dayan.tool.vo.ToolAiQaConfigVO;
import com.dayan.tool.vo.ToolAiQaMessageVO;
import com.dayan.tool.vo.ToolAiQaSessionVO;
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

@Tag(name = "Agent AI 问答")
@RestController
@RequestMapping("/tools/qa")
@RequiredArgsConstructor
public class AgentToolAiQaController {

    private final ToolAiQaConfigService configService;
    private final ToolAiQaSessionService sessionService;
    private final ToolAiQaChatService chatService;
    private final ToolAiQaStreamService streamService;

    @Operation(summary = "可用人物列表（平台启用）")
    @GetMapping("/configs")
    public R<List<ToolAiQaConfigVO>> configs() {
        return R.ok(configService.listEnabled());
    }

    @Operation(summary = "某人物下我的会话列表")
    @GetMapping("/sessions")
    public R<List<ToolAiQaSessionVO>> sessions(@RequestParam Long configId) {
        if (configId == null || configId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "人物配置 ID 必须大于 0");
        }
        String agentCode = ContextHolder.getAccountCode();
        return R.ok(sessionService.listByPersona(agentCode, configId));
    }

    @Operation(summary = "新建会话")
    @PostMapping("/sessions")
    @OperationLog(module = "AI 问答", action = "新建会话")
    public R<String> createSession(@RequestBody Map<String, Long> body) {
        String agentCode = ContextHolder.getAccountCode();
        String channelCode = ContextHolder.getChannelCode();
        return R.ok(sessionService.create(agentCode, channelCode, body.get("configId")));
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
    public R<List<ToolAiQaMessageVO>> messages(@PathVariable String sessionCode) {
        return R.ok(chatService.listMessages(sessionCode));
    }

    @Operation(summary = "问答（JSON）")
    @PostMapping("/chat")
    public R<ToolAiQaChatResultVO> chat(@RequestBody @Valid ToolAiQaChatDTO dto) {
        return R.ok(chatService.chat(dto));
    }

    @Operation(summary = "问答（SSE 流式）")
    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@RequestBody @Valid ToolAiQaChatDTO dto) {
        return streamService.chatStream(dto);
    }
}
