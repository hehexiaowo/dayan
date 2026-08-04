package com.dayan.client.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.client.dto.ClientInfoCreateDTO;
import com.dayan.client.dto.ClientInfoQueryDTO;
import com.dayan.client.dto.ClientInfoUpdateDTO;
import com.dayan.client.service.ClientInfoService;
import com.dayan.client.vo.ClientInfoVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端客户信息接口。
 */
@Tag(name = "客户信息管理")
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientInfoAdminController {

    private final ClientInfoService clientInfoService;

    @Operation(summary = "客户分页列表（按渠道隔离）")
    @SaCheckPermission("client:info:list")
    @GetMapping
    public R<PageResult<ClientInfoVO>> page(ClientInfoQueryDTO query) {
        return R.ok(clientInfoService.page(query));
    }

    @Operation(summary = "客户详情")
    @SaCheckPermission("client:info:query")
    @GetMapping("/{clientCode}")
    public R<ClientInfoVO> getDetail(@PathVariable String clientCode) {
        return R.ok(clientInfoService.getDetail(clientCode));
    }

    @Operation(summary = "新增客户")
    @SaCheckPermission("client:info:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ClientInfoCreateDTO dto) {
        return R.ok(clientInfoService.create(dto));
    }

    @Operation(summary = "修改客户")
    @SaCheckPermission("client:info:update")
    @PutMapping("/{clientCode}")
    public R<Void> update(@PathVariable String clientCode, @RequestBody @Valid ClientInfoUpdateDTO dto) {
        clientInfoService.update(clientCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除客户")
    @SaCheckPermission("client:info:delete")
    @DeleteMapping("/{clientCode}")
    public R<Void> delete(@PathVariable String clientCode) {
        clientInfoService.delete(clientCode);
        return R.ok();
    }
}
