package com.dayan.client.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.client.dto.ClientHealthProfileSaveDTO;
import com.dayan.client.service.ClientHealthProfileService;
import com.dayan.client.vo.ClientHealthProfileVO;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端客户健康档案接口（一客户一档案）。
 */
@Tag(name = "客户健康档案管理")
@RestController
@RequestMapping("/health-profiles")
@RequiredArgsConstructor
public class ClientHealthProfileAdminController {

    private final ClientHealthProfileService clientHealthProfileService;

    @Operation(summary = "按客户编码获取健康档案")
    @SaCheckPermission("client:health:query")
    @GetMapping("/{clientCode}")
    public R<ClientHealthProfileVO> getByClient(@PathVariable String clientCode) {
        return R.ok(clientHealthProfileService.getByClient(clientCode));
    }

    @Operation(summary = "保存或更新健康档案（upsert：一客户一档案）")
    @SaCheckPermission("client:health:save")
    @PostMapping
    public R<Long> saveOrUpdate(@RequestBody @Valid ClientHealthProfileSaveDTO dto) {
        return R.ok(clientHealthProfileService.saveOrUpdate(dto));
    }

    @Operation(summary = "删除健康档案")
    @SaCheckPermission("client:health:delete")
    @DeleteMapping("/{clientCode}")
    public R<Void> delete(@PathVariable String clientCode) {
        clientHealthProfileService.delete(clientCode);
        return R.ok();
    }
}
