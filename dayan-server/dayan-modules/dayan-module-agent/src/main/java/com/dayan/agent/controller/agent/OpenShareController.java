package com.dayan.agent.controller.agent;

import com.dayan.agent.service.AgentCardService;
import com.dayan.agent.service.AgentInfoService;
import com.dayan.agent.vo.AgentCardVO;
import com.dayan.agent.vo.AgentInfoVO;
import com.dayan.client.service.ClientInfoService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.content.service.ContentInfoService;
import com.dayan.content.vo.ContentInfoVO;
import com.dayan.lead.entity.LeadInfo;
import com.dayan.lead.service.LeadInfoService;
import com.dayan.lead.service.LeadTrackService;
import com.dayan.tool.service.ToolPosterTemplateService;
import com.dayan.tool.vo.ToolPosterTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开分享接口（无需登录）。
 *
 * <p>放在 controller.agent 包（被 agent starter 扫描加载），
 * 路径前缀 /open/share 标识公开性，不加任何 SaToken 注解即无需鉴权
 * （SaTokenConfig 未启用全局拦截，无注解即公开）。
 *
 * <p>客户打开代理人分享的文章链接时，由此接口返回文章内容 + 分享人名片。
 * 访客追踪写入 lead 域：lead_info 建档 + 互动明细表；留资时自动建客户档案并回填关联。
 */
@Slf4j
@Tag(name = "公开分享")
@RestController
@RequestMapping("/open/share")
@RequiredArgsConstructor
public class OpenShareController {

    private final ContentInfoService contentInfoService;
    private final AgentCardService agentCardService;
    private final ToolPosterTemplateService posterTemplateService;
    private final AgentInfoService agentInfoService;
    private final LeadTrackService leadTrackService;
    private final LeadInfoService leadInfoService;
    private final ClientInfoService clientInfoService;

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

        ToolPosterTemplateVO poster = posterTemplateService.getDetail(templateCode);

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

    @Operation(summary = "追踪分享链接打开（公开，自动创建/更新访客线索）")
    @PostMapping("/track")
    public R<Map<String, Object>> trackShare(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String agentCode = (String) body.get("agentCode");
        int shareType = body.get("shareType") != null ? ((Number) body.get("shareType")).intValue() : 1;
        String bizCode = (String) body.get("bizCode");
        String bizTitle = (String) body.get("bizTitle");
        String visitorToken = (String) body.get("visitorToken");
        String visitorSource = (String) body.get("visitorSource");

        // 检测微信环境（User-Agent 含 MicroMessenger）
        if (visitorSource == null) {
            String ua = request.getHeader("User-Agent");
            visitorSource = (ua != null && ua.contains("MicroMessenger")) ? "wechat" : "browser";
        }

        // 渠道归属由分享人（代理人）解析；无分享人的直接访问不建档
        String channelCode = resolveChannelCode(agentCode);
        String token = leadTrackService.track(channelCode, agentCode, shareType,
                bizCode, bizTitle, visitorToken, visitorSource);

        Map<String, Object> result = new HashMap<>();
        result.put("visitorToken", token);
        return R.ok(result);
    }

    @Operation(summary = "客户留资（公开，回填线索手机号/姓名并自动建客户档案）")
    @PostMapping("/contact")
    public R<Void> leaveContact(@RequestBody Map<String, Object> body) {
        String visitorToken = (String) body.get("visitorToken");
        String phone = (String) body.get("phone");
        String name = (String) body.get("name");
        if (!StringUtils.hasText(visitorToken) || !StringUtils.hasText(phone)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "访客令牌和手机号不能为空");
        }

        leadTrackService.leaveContact(visitorToken, phone, name);

        // 留资即建档：find-or-create 客户（档案+无密码账号），并回填线索的客户关联
        try {
            LeadInfo lead = leadInfoService.getByVisitorToken(visitorToken);
            if (lead != null && !StringUtils.hasText(lead.getClientCode())) {
                String clientCode = clientInfoService.findOrCreateByPhone(
                        lead.getChannelCode(), phone, name, null);
                leadTrackService.bindClient(visitorToken, clientCode);
            }
        } catch (Exception e) {
            // 建档失败不阻断留资主流程
            log.warn("[Lead] 留资自动建档客户失败: {}", e.getMessage());
        }
        return R.ok();
    }

    /**
     * 由代理人编码解析其所属渠道编码（代理人不存在时返回 null，追踪降级为不建档）。
     */
    private String resolveChannelCode(String agentCode) {
        if (!StringUtils.hasText(agentCode)) {
            return null;
        }
        try {
            AgentInfoVO agent = agentInfoService.getDetail(agentCode);
            return agent != null ? agent.getChannelCode() : null;
        } catch (BusinessException e) {
            log.warn("[Lead] 解析分享人渠道失败: agentCode={}, {}", agentCode, e.getMessage());
            return null;
        }
    }
}
