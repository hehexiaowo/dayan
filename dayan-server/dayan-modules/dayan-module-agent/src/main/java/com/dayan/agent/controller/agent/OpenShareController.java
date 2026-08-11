package com.dayan.agent.controller.agent;

import com.dayan.agent.service.AgentCardService;
import com.dayan.agent.service.PosterTemplateService;
import com.dayan.agent.vo.AgentCardVO;
import com.dayan.agent.vo.PosterTemplateVO;
import com.dayan.common.core.resp.R;
import com.dayan.content.service.ContentInfoService;
import com.dayan.content.vo.ContentInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 公开分享接口（无需登录）。
 *
 * <p>放在 controller.agent 包（被 agent starter 扫描加载），
 * 路径前缀 /open/share 标识公开性，不加任何 SaToken 注解即无需鉴权
 * （SaTokenConfig 未启用全局拦截，无注解即公开）。
 *
 * <p>客户打开代理人分享的文章链接时，由此接口返回文章内容 + 分享人名片。
 */
@Tag(name = "公开分享")
@RestController
@RequestMapping("/open/share")
@RequiredArgsConstructor
public class OpenShareController {

    private final ContentInfoService contentInfoService;
    private final AgentCardService agentCardService;
    private final PosterTemplateService posterTemplateService;

    @Operation(summary = "分享内容详情（公开，含分享人名片）")
    @GetMapping("/content/{contentCode}")
    public R<Map<String, Object>> shareContent(
            @PathVariable String contentCode,
            @RequestParam(required = false) String agent) {

        ContentInfoVO content = contentInfoService.getDetail(contentCode);

        AgentCardVO card = null;
        if (agent != null && !agent.isEmpty()) {
            card = agentCardService.getFirstByAgent(agent);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("card", card);
        return R.ok(result);
    }

    @Operation(summary = "分享营销海报（公开，含分享人名片）")
    @GetMapping("/poster/{templateCode}")
    public R<Map<String, Object>> sharePoster(
            @PathVariable String templateCode,
            @RequestParam(required = false) String agent) {

        PosterTemplateVO poster = posterTemplateService.getDetail(templateCode);

        AgentCardVO card = (agent != null && !agent.isEmpty())
                ? agentCardService.getFirstByAgent(agent) : null;

        Map<String, Object> result = new HashMap<>();
        result.put("poster", poster);
        result.put("card", card);
        return R.ok(result);
    }

    @Operation(summary = "获取代理人名片（公开，工具分享用）")
    @GetMapping("/agent-card")
    public R<AgentCardVO> getAgentCard(@RequestParam String agent) {
        return R.ok(agentCardService.getFirstByAgent(agent));
    }
}
