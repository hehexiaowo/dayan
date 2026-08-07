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
 *
 * <p><b>为何不像 admin 端那样按角色过滤（treeByRole）：</b>当前 channel 端无"不同角色看不同菜单"
 * 的真实需求（YAGNI），接口/按钮级权限才是真需求，登录即见本端全量菜单。详见
 * .superpowers/specs/2026-08-07-p9-channel-full-design.md §3.7 选项 B。
 * 若后续需要按角色过滤，需新增 channel_role_menu_rel 表与对应查询链路
 * （SystemMenuService.treeByRole 当前仅查 organ_*_rel 表）。
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
