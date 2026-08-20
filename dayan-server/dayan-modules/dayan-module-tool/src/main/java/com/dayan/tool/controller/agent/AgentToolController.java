package com.dayan.tool.controller.agent;

import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 端工具接口。
 *
 * <p>路径：{@code /tools}（agent starter context-path 拼接为 {@code /agent-api/tools}）。
 * 返回全部启用工具，按 id 升序。
 */
@Tag(name = "Agent 工具")
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
public class AgentToolController {

    private final ToolInfoService toolInfoService;

    @Operation(summary = "启用工具列表（按渠道过滤）")
    @GetMapping
    public R<List<ToolInfoVO>> list() {
        String channelCode = ContextHolder.getChannelCode();
        return R.ok(toolInfoService.listForAgent(channelCode));
    }
}
