package com.dayan.content.controller.channel;

import com.dayan.channel.entity.ChannelConfigContent;
import com.dayan.channel.service.ChannelConfigContentService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.content.dto.ContentInfoQueryDTO;
import com.dayan.content.service.ContentInfoService;
import com.dayan.content.vo.ContentInfoVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel 渠道端内容接口（养老保典/客户平台共用，appType 区分）。
 *
 * <p>路径：{@code /channel-api/contents}。
 * 防越权：content_info 是平台共享表（TenantHandler 忽略），靠联表 channel_config_content
 * 得本渠道已配置的 contentCode 集合做 IN 过滤，且只展示已审核通过（auditStatus≥2）的内容。
 *
 * <p>appType 查询参数：agent（养老保典）/ client（客户平台），决定查 channel_config_content
 * 中哪个 appType 分组的配置。
 */
@Tag(name = "Channel 内容")
@RestController
@RequestMapping("/contents")
@RequiredArgsConstructor
public class ChannelContentController {

    private final ContentInfoService contentInfoService;
    private final ChannelConfigContentService channelConfigContentService;

    @Operation(summary = "本渠道已配置内容列表")
    @SaCheckPermission("channel:content:list")
    @GetMapping
    public R<PageResult<ContentInfoVO>> page(
            @Parameter(description = "应用类型：agent/client") @RequestParam(defaultValue = "agent") String appType,
            ContentInfoQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        List<String> contentCodes = collectChannelContentCodes(channelCode, appType);
        if (contentCodes.isEmpty()) {
            return R.ok(new PageResult<>(query.getCurrent(), query.getSize(), 0L, Collections.emptyList()));
        }
        query.setContentCodes(contentCodes);
        return R.ok(contentInfoService.page(query));
    }

    @Operation(summary = "内容详情")
    @SaCheckPermission("channel:content:query")
    @GetMapping("/{contentCode}")
    public R<ContentInfoVO> getDetail(@PathVariable String contentCode) {
        String channelCode = ContextHolder.getChannelCode();
        // 校验该内容已被本渠道配置（任一 appType）
        List<String> allCodes = collectChannelContentCodes(channelCode, null);
        if (!allCodes.contains(contentCode)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在或未配置");
        }
        ContentInfoVO vo = contentInfoService.getDetail(contentCode);
        if (vo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在");
        }
        return R.ok(vo);
    }

    /**
     * 收集本渠道已配置的内容编码集合。
     * @param appType agent/client/null（null=不限 appType，全部）
     */
    private List<String> collectChannelContentCodes(String channelCode, String appType) {
        return channelConfigContentService.listByChannel(channelCode).stream()
                .filter(c -> appType == null || appType.equals(c.getAppType()))
                .map(ChannelConfigContent::getContentCode)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toList());
    }
}
