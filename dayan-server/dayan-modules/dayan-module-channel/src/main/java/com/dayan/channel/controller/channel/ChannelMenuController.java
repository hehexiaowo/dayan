package com.dayan.channel.controller.channel;

import com.dayan.common.core.resp.R;
import com.dayan.system.entity.SystemMenu;
import com.dayan.system.service.SystemMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Channel 渠道端菜单接口。
 *
 * <p>提供当前端菜单树（domain_type=channel 全量，不过滤角色）。
 * 与 Admin 端 SystemMenuAdminController 的 /menus/mine 路径形态一致，
 * 前端 getMyMenuTree 复用同一接口名。
 *
 * <p>channel-api 天然就是 channel 端，domainType 在后端写死，不接收前端参数。
 */
@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class ChannelMenuController {

    private final SystemMenuService menuService;

    /**
     * 当前账号可见菜单树（domain_type=channel 全量）。
     *
     * <p>GET /channel-api/menus/mine
     * 登录即可调用，无需 @SaCheckPermission（与 admin 端 /menus/mine 一致）。
     */
    @GetMapping("/mine")
    public R<List<SystemMenu>> mine() {
        return R.ok(menuService.tree("channel"));
    }
}
