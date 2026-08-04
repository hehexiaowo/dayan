package com.dayan.client.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.client.dto.ClientFamilyMemberCreateDTO;
import com.dayan.client.dto.ClientFamilyMemberUpdateDTO;
import com.dayan.client.service.ClientFamilyMemberService;
import com.dayan.client.vo.ClientFamilyMemberVO;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端客户家庭成员接口。
 */
@Tag(name = "客户家庭成员管理")
@RestController
@RequestMapping("/family-members")
@RequiredArgsConstructor
public class ClientFamilyMemberAdminController {

    private final ClientFamilyMemberService clientFamilyMemberService;

    @Operation(summary = "按客户编码列出家庭成员")
    @SaCheckPermission("client:family:list")
    @GetMapping("/by-client/{clientCode}")
    public R<List<ClientFamilyMemberVO>> listByClient(@PathVariable String clientCode) {
        return R.ok(clientFamilyMemberService.listByClient(clientCode));
    }

    @Operation(summary = "新增家庭成员")
    @SaCheckPermission("client:family:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ClientFamilyMemberCreateDTO dto) {
        return R.ok(clientFamilyMemberService.create(dto));
    }

    @Operation(summary = "修改家庭成员")
    @SaCheckPermission("client:family:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid ClientFamilyMemberUpdateDTO dto) {
        clientFamilyMemberService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除家庭成员")
    @SaCheckPermission("client:family:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        clientFamilyMemberService.delete(id);
        return R.ok();
    }
}
