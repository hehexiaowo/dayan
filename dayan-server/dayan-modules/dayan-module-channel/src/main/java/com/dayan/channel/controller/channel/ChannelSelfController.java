package com.dayan.channel.controller.channel;

import com.dayan.channel.service.ChannelInfoService;
import com.dayan.channel.vo.ChannelInfoVO;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel 渠道端自身信息接口。
 *
 * <p>路径：{@code /channel-api/channel-infos/*}（由 dayan-channel 启动模块的 context-path 拼接）。
 */
@Tag(name = "Channel 渠道信息")
@RestController
@RequestMapping("/channel-infos")
@RequiredArgsConstructor
public class ChannelSelfController {

    private final ChannelInfoService channelInfoService;

    @Operation(summary = "本渠道信息")
    @GetMapping("/current")
    public R<ChannelInfoVO> current() {
        String channelCode = ContextHolder.getChannelCode();
        return R.ok(channelInfoService.getDetail(channelCode));
    }

    @Operation(summary = "本渠道组织架构树")
    @GetMapping("/tree")
    public R<List<ChannelInfoVO>> tree() {
        String channelCode = ContextHolder.getChannelCode();
        List<ChannelInfoVO> fullTree = channelInfoService.tree();
        // 找到当前渠道节点及其所有后代（ancestors 包含 channelCode）
        List<ChannelInfoVO> subTree = fullTree.stream()
                .filter(vo -> channelCode.equals(vo.getChannelCode())
                        || (vo.getAncestors() != null && vo.getAncestors().contains(channelCode)))
                .collect(Collectors.toList());
        return R.ok(subTree);
    }
}
