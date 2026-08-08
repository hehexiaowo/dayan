package com.dayan.service.controller.channel;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.service.dto.ServiceSessionQueryDTO;
import com.dayan.service.service.ServiceSessionService;
import com.dayan.service.vo.ServiceSessionVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Channel 渠道端服务记录（服务会话）接口。
 *
 * <p>路径：{@code /channel-api/service-sessions}。
 * 防越权：service_session 表有 channel_code 列（TenantHandler 忽略 service_ 前缀，但显式注入生效），
 * ServiceSessionServiceImpl.buildWrapper 第 403 行已支持 channelCode eq 过滤。
 */
@Tag(name = "Channel 服务记录")
@RestController
@RequestMapping("/service-sessions")
@RequiredArgsConstructor
public class ChannelServiceSessionController {

    private final ServiceSessionService serviceSessionService;

    @Operation(summary = "本渠道服务记录列表")
    @SaCheckPermission("channel:serviceSession:list")
    @GetMapping
    public R<PageResult<ServiceSessionVO>> page(ServiceSessionQueryDTO query) {
        query.setChannelCode(ContextHolder.getChannelCode());
        return R.ok(serviceSessionService.page(query));
    }

    @Operation(summary = "服务记录详情")
    @SaCheckPermission("channel:serviceSession:query")
    @GetMapping("/{sessionCode}")
    public R<ServiceSessionVO> getDetail(@PathVariable String sessionCode) {
        ServiceSessionVO vo = serviceSessionService.getDetail(sessionCode);
        if (vo == null || !ContextHolder.getChannelCode().equals(vo.getChannelCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "服务记录不存在或无权访问");
        }
        return R.ok(vo);
    }
}
