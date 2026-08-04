package com.dayan.client.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.client.dto.ClientCareNeedCreateDTO;
import com.dayan.client.dto.ClientCareNeedQueryDTO;
import com.dayan.client.dto.ClientCareNeedUpdateDTO;
import com.dayan.client.service.ClientCareNeedService;
import com.dayan.client.vo.ClientCareNeedVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端客户照护需求评估接口。
 */
@Tag(name = "客户照护需求评估管理")
@RestController
@RequestMapping("/care-needs")
@RequiredArgsConstructor
public class ClientCareNeedAdminController {

    private final ClientCareNeedService clientCareNeedService;

    @Operation(summary = "照护需求评估分页列表")
    @SaCheckPermission("client:care:list")
    @GetMapping
    public R<PageResult<ClientCareNeedVO>> page(ClientCareNeedQueryDTO query) {
        return R.ok(clientCareNeedService.page(query));
    }

    @Operation(summary = "按客户编码列出评估列表")
    @SaCheckPermission("client:care:list")
    @GetMapping("/by-client/{clientCode}")
    public R<List<ClientCareNeedVO>> listByClient(@PathVariable String clientCode) {
        return R.ok(clientCareNeedService.listByClient(clientCode));
    }

    @Operation(summary = "新增照护需求评估")
    @SaCheckPermission("client:care:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ClientCareNeedCreateDTO dto) {
        return R.ok(clientCareNeedService.create(dto));
    }

    @Operation(summary = "修改照护需求评估")
    @SaCheckPermission("client:care:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid ClientCareNeedUpdateDTO dto) {
        clientCareNeedService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除照护需求评估")
    @SaCheckPermission("client:care:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        clientCareNeedService.delete(id);
        return R.ok();
    }
}
