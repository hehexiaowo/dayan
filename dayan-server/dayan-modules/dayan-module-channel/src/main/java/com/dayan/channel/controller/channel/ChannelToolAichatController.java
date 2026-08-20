package com.dayan.channel.controller.channel;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dayan.channel.dto.ChannelToolAichatRepoBindDTO;
import com.dayan.channel.entity.ChannelConfigTool;
import com.dayan.channel.service.ChannelConfigToolService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.system.service.SystemKnowledgeRepoService;
import com.dayan.system.vo.SystemKnowledgeRepoVO;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolAichatPersonaVO;
import com.dayan.tool.vo.ToolChannelPersonaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel 渠道端 AI 问答人物接口（渠道补充知识库）。
 *
 * <p>路径 {@code /tools/aichat}（dayan-channel starter context-path 拼接为 {@code /channel-api/tools/aichat/*}）。
 *
 * <p>渠道隔离：channelCode 一律从 {@link ContextHolder} 强制注入，不接收前端参数；
 * 补充范围仅限本渠道 + 后代渠道名下的渠道库（{@code listChannelScopeRepos} 校验）。</p>
 */
@Tag(name = "Channel AI 问答人物")
@RestController
@RequestMapping("/tools/aichat")
@RequiredArgsConstructor
public class ChannelToolAichatController {

    private static final int CONFIG_TYPE_REPO_SUPPLEMENT = 1;

    private final ToolInfoService toolInfoService;
    private final ChannelConfigToolService channelConfigToolService;
    private final SystemKnowledgeRepoService knowledgeRepoService;

    @Operation(summary = "启用中问答人物列表（含 admin 全局库与本渠道补充库）")
    @SaCheckPermission("channel:tool:aichat:view")
    @GetMapping("/personas")
    public R<List<ToolChannelPersonaVO>> personas() {
        String channelCode = ContextHolder.getChannelCode();
        // 从 tool 域获取所有启用的 aichat 人物（不做合并）
        List<ToolAichatPersonaVO> rawPersonas = toolInfoService.listQaPersonasRaw();
        List<ToolChannelPersonaVO> result = rawPersonas.stream().map(p -> {
            ToolChannelPersonaVO vo = new ToolChannelPersonaVO();
            vo.setToolCode(p.getToolCode());
            vo.setPersonaName(p.getPersonaName());
            vo.setToolDesc(p.getToolDesc());
            // globalRepoIds = admin 全局绑定（config_json.repoIds，未合并）
            vo.setGlobalRepoIds(p.getRepoIds() != null ? p.getRepoIds() : List.of());
            // channelRepoIds = 本渠道补充（从 channel_config_tool 读 config_type=1）
            vo.setChannelRepoIds(getChannelRepoIds(channelCode, p.getToolCode()));
            return vo;
        }).collect(Collectors.toList());
        return R.ok(result);
    }

    @Operation(summary = "可补充知识库（本渠道 + 后代渠道名下，不含平台库）")
    @SaCheckPermission("channel:tool:aichat:view")
    @GetMapping("/repos/options")
    public R<List<SystemKnowledgeRepoVO>> repoOptions() {
        return R.ok(knowledgeRepoService.listChannelScopeRepos(ContextHolder.getChannelCode()));
    }

    @Operation(summary = "保存人物补充知识库（全量替换；空数组 = 清空补充）")
    @SaCheckPermission("channel:tool:aichat:update")
    @PutMapping("/personas/{toolCode}/repos")
    public R<Void> saveRepos(@PathVariable String toolCode,
                             @RequestBody @Valid ChannelToolAichatRepoBindDTO dto) {
        String channelCode = ContextHolder.getChannelCode();
        // 人物存在性校验（不存在/非 aichat 类型抛 NOT_FOUND）
        toolInfoService.getQaPersona(toolCode);

        List<Long> repoIds = dto.getRepoIds() == null ? List.of() : dto.getRepoIds().stream().distinct().toList();
        // 归属校验：仅允许本渠道 + 后代渠道名下的渠道库
        java.util.Set<Long> allowed = knowledgeRepoService.listChannelScopeRepos(channelCode).stream()
                .map(SystemKnowledgeRepoVO::getId).collect(java.util.stream.Collectors.toSet());
        for (Long repoId : repoIds) {
            if (repoId == null || !allowed.contains(repoId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "知识库不在可补充范围内: " + repoId);
            }
        }

        // 全量替换：写入 config_type=1 的 config_json
        JSONObject json = new JSONObject();
        json.set("repoIds", repoIds);
        channelConfigToolService.save(channelCode, toolCode, CONFIG_TYPE_REPO_SUPPLEMENT, json.toString());
        return R.ok();
    }

    /**
     * 读取渠道补充知识库 ID 列表（从 channel_config_tool 读 config_type=1 的 config_json）。
     */
    private List<Long> getChannelRepoIds(String channelCode, String toolCode) {
        ChannelConfigTool config = channelConfigToolService.getByChannelToolType(
                channelCode, toolCode, CONFIG_TYPE_REPO_SUPPLEMENT);
        if (config == null || StrUtil.isBlank(config.getConfigJson())) {
            return List.of();
        }
        try {
            JSONObject json = JSONUtil.parseObj(config.getConfigJson());
            JSONArray arr = json.getJSONArray("repoIds");
            if (arr == null) {
                return List.of();
            }
            return arr.toList(Long.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
