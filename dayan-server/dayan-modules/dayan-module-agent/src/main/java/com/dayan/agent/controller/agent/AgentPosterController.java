package com.dayan.agent.controller.agent;

import com.dayan.agent.service.PosterTemplateService;
import com.dayan.agent.vo.PosterTemplateVO;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代理人端 — 营销海报模板浏览。
 * 模板由后台定义，代理人只能浏览和分享，不能编辑。
 */
@Tag(name = "营销海报")
@RestController
@RequestMapping("/poster-templates")
@RequiredArgsConstructor
public class AgentPosterController {

    private final PosterTemplateService posterTemplateService;

    @Operation(summary = "海报模板列表")
    @GetMapping
    public R<List<PosterTemplateVO>> list(
            @RequestParam(required = false) String category) {
        return R.ok(posterTemplateService.listActive(category));
    }

    @Operation(summary = "海报模板详情")
    @GetMapping("/{templateCode}")
    public R<PosterTemplateVO> detail(@PathVariable String templateCode) {
        return R.ok(posterTemplateService.getDetail(templateCode));
    }
}
