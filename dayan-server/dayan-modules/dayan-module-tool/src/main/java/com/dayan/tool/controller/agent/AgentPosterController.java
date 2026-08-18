package com.dayan.tool.controller.agent;

import com.dayan.common.core.resp.R;
import com.dayan.tool.service.ToolPosterTemplateService;
import com.dayan.tool.vo.ToolPosterTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代理人端 — 营销海报模板浏览（工具域）。
 * 模板由后台定义，代理人只能浏览和分享，不能编辑。
 */
@Tag(name = "营销海报")
@RestController
@RequestMapping("/poster-templates")
@RequiredArgsConstructor
public class AgentPosterController {

    private final ToolPosterTemplateService posterTemplateService;

    @Operation(summary = "海报模板列表")
    @GetMapping
    public R<List<ToolPosterTemplateVO>> list(
            @RequestParam(required = false) String category) {
        return R.ok(posterTemplateService.listActive(category));
    }

    @Operation(summary = "海报模板详情")
    @GetMapping("/{templateCode}")
    public R<ToolPosterTemplateVO> detail(@PathVariable String templateCode) {
        return R.ok(posterTemplateService.getDetail(templateCode));
    }
}
