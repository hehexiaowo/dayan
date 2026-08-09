package com.dayan.service.controller.client;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.security.StpKit;
import com.dayan.service.dto.ServiceSessionQueryDTO;
import com.dayan.service.service.ServiceSessionService;
import com.dayan.service.vo.ServiceSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 服务会话 - client 端接口（持卡人视角，只读）。
 *
 * <p>路径：{@code /client-api/service-sessions/*}。
 * 持卡人可查看自己的服务会话列表和详情，发起请求请用
 * {@code POST /client-api/equity/service-request}（在 equity controller）。
 */
@Tag(name = "Client 服务会话")
@RestController
@RequestMapping("/service-sessions")
@RequiredArgsConstructor
public class ClientServiceSessionController {

    private final ServiceSessionService serviceSessionService;

    /** 获取当前登录 clientCode */
    private String currentClientCode() {
        Object code = StpKit.CLIENT.getSession().get("clientCode");
        if (code == null) {
            throw new IllegalStateException("未获取到登录身份");
        }
        return code.toString();
    }

    @Operation(summary = "我的服务会话列表")
    @GetMapping("/my")
    public R<PageResult<ServiceSessionVO>> mySessions(ServiceSessionQueryDTO query) {
        query.setClientCode(currentClientCode());
        return R.ok(serviceSessionService.page(query));
    }

    @Operation(summary = "服务会话详情")
    @GetMapping("/{sessionCode}")
    public R<ServiceSessionVO> detail(@PathVariable String sessionCode) {
        ServiceSessionVO vo = serviceSessionService.getDetail(sessionCode);
        // 越权防护：只能看自己的会话
        if (vo == null || !currentClientCode().equals(vo.getClientCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在或无权访问");
        }
        return R.ok(vo);
    }
}
