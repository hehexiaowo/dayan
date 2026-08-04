package com.dayan.client.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.client.dto.ClientFavoriteAddDTO;
import com.dayan.client.dto.ClientFavoriteQueryDTO;
import com.dayan.client.service.ClientFavoriteService;
import com.dayan.client.vo.ClientFavoriteVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端客户收藏接口。
 */
@Tag(name = "客户收藏管理")
@RestController
@RequestMapping("/client-favorites")
@RequiredArgsConstructor
public class ClientFavoriteAdminController {

    private final ClientFavoriteService clientFavoriteService;

    @Operation(summary = "客户收藏分页列表")
    @SaCheckPermission("client:favorite:list")
    @GetMapping
    public R<PageResult<ClientFavoriteVO>> page(ClientFavoriteQueryDTO query) {
        return R.ok(clientFavoriteService.page(query));
    }

    @Operation(summary = "按客户编码列出收藏")
    @SaCheckPermission("client:favorite:list")
    @GetMapping("/by-client/{clientCode}")
    public R<List<ClientFavoriteVO>> listByClient(@PathVariable String clientCode) {
        return R.ok(clientFavoriteService.listByClient(clientCode));
    }

    @Operation(summary = "添加收藏")
    @SaCheckPermission("client:favorite:add")
    @PostMapping
    public R<Long> add(@RequestBody @Valid ClientFavoriteAddDTO dto) {
        return R.ok(clientFavoriteService.add(dto));
    }

    @Operation(summary = "移除收藏")
    @SaCheckPermission("client:favorite:remove")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        clientFavoriteService.remove(id);
        return R.ok();
    }
}
