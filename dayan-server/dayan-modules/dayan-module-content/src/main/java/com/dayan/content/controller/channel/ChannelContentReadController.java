package com.dayan.content.controller.channel;

import com.dayan.channel.entity.ChannelConfigContent;
import com.dayan.channel.service.ChannelConfigContentService;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.content.dto.ContentRecordReadQueryDTO;
import com.dayan.content.service.ContentRecordReadService;
import com.dayan.content.vo.ContentRecordReadVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel 渠道端阅读记录接口。
 *
 * <p>路径：{@code /channel-api/content-read-records}。
 * 防越权：content_record_read 是平台共享表，按本渠道已配置内容的 contentCode 集合过滤。
 */
@Tag(name = "Channel 阅读记录")
@RestController
@RequestMapping("/content-read-records")
@RequiredArgsConstructor
public class ChannelContentReadController {

    private final ContentRecordReadService contentRecordReadService;
    private final ChannelConfigContentService channelConfigContentService;

    @Operation(summary = "本渠道内容阅读记录列表")
    @SaCheckPermission("channel:readRecord:list")
    @GetMapping
    public R<PageResult<ContentRecordReadVO>> page(ContentRecordReadQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        List<String> contentCodes = collectChannelContentCodes(channelCode);
        if (contentCodes.isEmpty()) {
            return R.ok(new PageResult<>(query.getCurrent(), query.getSize(), 0L, Collections.emptyList()));
        }
        query.setContentCodes(contentCodes);
        return R.ok(contentRecordReadService.page(query));
    }

    private List<String> collectChannelContentCodes(String channelCode) {
        return channelConfigContentService.listByChannel(channelCode).stream()
                .map(ChannelConfigContent::getContentCode)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toList());
    }
}
