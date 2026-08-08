package com.dayan.scene.controller.channel;

import com.dayan.channel.entity.ChannelConfigScene;
import com.dayan.channel.service.ChannelConfigSceneService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.scene.dto.SceneInfoQueryDTO;
import com.dayan.scene.service.SceneInfoService;
import com.dayan.scene.vo.SceneInfoVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel 渠道端场景营销接口。
 *
 * <p>路径：{@code /channel-api/scenes}。
 * 防越权：scene_info 是平台共享表，靠联表 channel_config_scene 得本渠道已配置的 sceneCode 集合过滤。
 */
@Tag(name = "Channel 场景营销")
@RestController
@RequestMapping("/scenes")
@RequiredArgsConstructor
public class ChannelSceneController {

    private final SceneInfoService sceneInfoService;
    private final ChannelConfigSceneService channelConfigSceneService;

    @Operation(summary = "本渠道已配置场景列表")
    @SaCheckPermission("channel:scene:list")
    @GetMapping
    public R<PageResult<SceneInfoVO>> page(SceneInfoQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        List<String> sceneCodes = collectChannelSceneCodes(channelCode);
        if (sceneCodes.isEmpty()) {
            return R.ok(new PageResult<>(query.getCurrent(), query.getSize(), 0L, Collections.emptyList()));
        }
        query.setSceneCodes(sceneCodes);
        return R.ok(sceneInfoService.page(query));
    }

    @Operation(summary = "场景详情")
    @SaCheckPermission("channel:scene:query")
    @GetMapping("/{sceneCode}")
    public R<SceneInfoVO> getDetail(@PathVariable String sceneCode) {
        String channelCode = ContextHolder.getChannelCode();
        List<String> sceneCodes = collectChannelSceneCodes(channelCode);
        if (!sceneCodes.contains(sceneCode)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场景不存在或未配置");
        }
        SceneInfoVO vo = sceneInfoService.getDetail(sceneCode);
        if (vo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场景不存在");
        }
        return R.ok(vo);
    }

    private List<String> collectChannelSceneCodes(String channelCode) {
        return channelConfigSceneService.listByChannel(channelCode).stream()
                .map(ChannelConfigScene::getSceneCode)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toList());
    }
}
