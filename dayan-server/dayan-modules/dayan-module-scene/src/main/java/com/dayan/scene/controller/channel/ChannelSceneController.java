package com.dayan.scene.controller.channel;

import com.dayan.channel.entity.ChannelConfigScene;
import com.dayan.channel.service.ChannelConfigSceneService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.scene.dto.SceneInfoQueryDTO;
import com.dayan.scene.dto.SceneScheduleQueryDTO;
import com.dayan.scene.entity.SceneInfo;
import com.dayan.scene.mapper.SceneInfoMapper;
import com.dayan.scene.service.SceneInfoService;
import com.dayan.scene.service.SceneScheduleService;
import com.dayan.scene.vo.SceneInfoVO;
import com.dayan.scene.vo.SceneScheduleVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Channel 渠道端场景营销接口。
 *
 * <p>路径：{@code /channel-api/scenes}。
 *
 * <p>防越权：scene_info / scene_schedule 都是平台共享表（无 channel_code 列），
 * 靠联表 channel_config_scene 得本渠道已配置的 sceneCode 集合过滤。
 */
@Tag(name = "Channel 场景营销")
@RestController
@RequestMapping("/scenes")
@RequiredArgsConstructor
public class ChannelSceneController {

    private final SceneInfoService sceneInfoService;
    private final SceneScheduleService sceneScheduleService;
    private final ChannelConfigSceneService channelConfigSceneService;
    private final SceneInfoMapper sceneInfoMapper;

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

    // ==================== 场景活动日程（scene_schedule）====================

    @Operation(summary = "本渠道场景活动日程分页")
    @SaCheckPermission("channel:scene:list")
    @GetMapping("/schedules")
    public R<PageResult<SceneScheduleVO>> pageSchedules(SceneScheduleQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        List<String> sceneCodes = collectChannelSceneCodes(channelCode);
        if (sceneCodes.isEmpty()) {
            return R.ok(new PageResult<>(query.getCurrent(), query.getSize(), 0L, Collections.emptyList()));
        }
        query.setSceneCodes(sceneCodes);
        PageResult<SceneScheduleVO> result = sceneScheduleService.page(query);
        fillSceneName(result.getRecords());
        return R.ok(result);
    }

    @Operation(summary = "场景活动日程详情")
    @SaCheckPermission("channel:scene:query")
    @GetMapping("/schedules/{id}")
    public R<SceneScheduleVO> getScheduleDetail(@PathVariable Long id) {
        String channelCode = ContextHolder.getChannelCode();
        List<String> sceneCodes = collectChannelSceneCodes(channelCode);
        SceneScheduleVO vo = sceneScheduleService.getDetail(id);
        if (vo == null || !sceneCodes.contains(vo.getSceneCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场景日程不存在或未配置");
        }
        fillSceneName(List.of(vo));
        return R.ok(vo);
    }

    private List<String> collectChannelSceneCodes(String channelCode) {
        return channelConfigSceneService.listByChannel(channelCode).stream()
                .map(ChannelConfigScene::getSceneCode)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 批量回填 sceneName：按结果里出现的 sceneCode 集合一次性查 scene_info，
     * 组装 Map&lt;sceneCode, sceneName&gt; 后回填到每个 VO，避免 N+1。
     */
    private void fillSceneName(List<SceneScheduleVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<String> codes = records.stream()
                .map(SceneScheduleVO::getSceneCode)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toSet());
        if (codes.isEmpty()) {
            return;
        }
        List<SceneInfo> scenes = sceneInfoMapper.selectList(
                new LambdaQueryWrapper<SceneInfo>()
                        .in(SceneInfo::getSceneCode, codes)
                        .select(SceneInfo::getSceneCode, SceneInfo::getSceneName));
        Map<String, String> nameMap = scenes.stream()
                .collect(Collectors.toMap(SceneInfo::getSceneCode, SceneInfo::getSceneName, (a, b) -> a));
        records.forEach(vo -> vo.setSceneName(nameMap.get(vo.getSceneCode())));
    }
}
