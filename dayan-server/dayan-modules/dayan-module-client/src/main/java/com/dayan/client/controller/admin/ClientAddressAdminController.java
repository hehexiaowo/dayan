package com.dayan.client.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.client.dto.ClientAddressCreateDTO;
import com.dayan.client.dto.ClientAddressUpdateDTO;
import com.dayan.client.service.ClientAddressService;
import com.dayan.client.vo.ClientAddressVO;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端客户收货地址接口。
 */
@Tag(name = "客户收货地址管理")
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class ClientAddressAdminController {

    private final ClientAddressService clientAddressService;

    @Operation(summary = "按客户编码列出收货地址")
    @SaCheckPermission("client:address:list")
    @GetMapping("/by-client/{clientCode}")
    public R<List<ClientAddressVO>> listByClient(@PathVariable String clientCode) {
        return R.ok(clientAddressService.listByClient(clientCode));
    }

    @Operation(summary = "新增收货地址（≤20 条限制）")
    @SaCheckPermission("client:address:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ClientAddressCreateDTO dto) {
        return R.ok(clientAddressService.create(dto));
    }

    @Operation(summary = "修改收货地址")
    @SaCheckPermission("client:address:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid ClientAddressUpdateDTO dto) {
        clientAddressService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "设为默认地址")
    @SaCheckPermission("client:address:update")
    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        clientAddressService.setDefault(id);
        return R.ok();
    }

    @Operation(summary = "删除收货地址")
    @SaCheckPermission("client:address:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        clientAddressService.delete(id);
        return R.ok();
    }
}
