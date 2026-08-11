package com.dayan.content.controller.agent;

import com.dayan.channel.entity.ChannelConfigContent;
import com.dayan.channel.service.ChannelConfigContentService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.content.dto.ContentInfoQueryDTO;
import com.dayan.content.service.ContentInfoService;
import com.dayan.content.vo.ContentCategoryOptionVO;
import com.dayan.content.vo.ContentInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 代理人端内容接口（养老保典 · 内容获客）。
 *
 * <p>路径：{@code /agent-api/contents}（由 dayan-agent 启动模块 context-path 拼接）。
 *
 * <p>仅返回当前渠道已配置给 agent 端（{@code channel_config_content.appType='agent'}）
 * 且已发布（{@code contentStatus=2}）的内容。
 * channelCode 从 {@link ContextHolder} 强制注入，防越权。
 */
@Tag(name = "Agent 内容")
@RestController
@RequestMapping("/contents")
@RequiredArgsConstructor
public class AgentContentController {

    private final ContentInfoService contentInfoService;
    private final ChannelConfigContentService channelConfigContentService;

    @Operation(summary = "渠道配置内容列表")
    @GetMapping
    public R<PageResult<ContentInfoVO>> page(ContentInfoQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        List<String> contentCodes = collectContentCodes(channelCode);
        if (contentCodes.isEmpty()) {
            return R.ok(new PageResult<>(query.getCurrent(), query.getSize(), 0L, Collections.emptyList()));
        }
        query.setContentCodes(contentCodes);
        query.setContentStatus(2);  // 仅已发布
        query.setAuditStatus(2);    // 仅审核通过
        return R.ok(contentInfoService.page(query));
    }

    @Operation(summary = "内容详情")
    @GetMapping("/{contentCode}")
    public R<ContentInfoVO> getDetail(@PathVariable String contentCode) {
        String channelCode = ContextHolder.getChannelCode();
        List<String> codes = collectContentCodes(channelCode);
        if (!codes.contains(contentCode)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在或未配置");
        }
        ContentInfoVO vo = contentInfoService.getDetail(contentCode);
        if (vo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在");
        }
        // 浏览量 +1（原子自增，不影响本次返回的展示值）
        contentInfoService.incrementViewCount(contentCode);
        return R.ok(vo);
    }

    @Operation(summary = "内容分类列表（当前渠道已配置内容涉及的分类）")
    @GetMapping("/categories")
    public R<List<ContentCategoryOptionVO>> categories() {
        String channelCode = ContextHolder.getChannelCode();
        List<String> contentCodes = collectContentCodes(channelCode);
        return R.ok(contentInfoService.listCategoriesByContentCodes(contentCodes));
    }

    /**
     * 收集本渠道配置给 agent 端的内容编码。
     */
    private List<String> collectContentCodes(String channelCode) {
        return channelConfigContentService.listByChannel(channelCode).stream()
                .filter(c -> "agent".equals(c.getAppType()))
                .map(ChannelConfigContent::getContentCode)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toList());
    }
}
