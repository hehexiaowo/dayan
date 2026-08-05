package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkPeripheryCreateDTO;
import com.dayan.park.dto.ParkPeripheryQueryDTO;
import com.dayan.park.dto.ParkPeripheryUpdateDTO;
import com.dayan.park.service.ParkPeripheryService;
import com.dayan.park.vo.ParkPeripheryVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构周边信息接口。
 *
 * <p>路径前缀 {@code /park/periphery}。
 */
@Tag(name = "机构周边信息管理")
@RestController
@RequestMapping("/park/periphery")
@RequiredArgsConstructor
public class ParkPeripheryAdminController {

    private final ParkPeripheryService parkPeripheryService;

    @Operation(summary = "机构周边信息分页列表")
    @SaCheckPermission("park:periphery:list")
    @GetMapping("/page")
    public R<PageResult<ParkPeripheryVO>> page(ParkPeripheryQueryDTO query) {
        return R.ok(parkPeripheryService.page(query));
    }

    @Operation(summary = "机构周边信息列表（按机构）")
    @SaCheckPermission("park:periphery:list")
    @GetMapping("/list")
    public R<List<ParkPeripheryVO>> list(@RequestParam String parkCode) {
        return R.ok(parkPeripheryService.listByPark(parkCode));
    }

    @Operation(summary = "机构周边信息详情")
    @SaCheckPermission("park:periphery:query")
    @GetMapping("/{id}")
    public R<ParkPeripheryVO> getDetail(@PathVariable Long id) {
        return R.ok(parkPeripheryService.getDetail(id));
    }

    @Operation(summary = "新增机构周边信息")
    @SaCheckPermission("park:periphery:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkPeripheryCreateDTO dto) {
        return R.ok(parkPeripheryService.create(dto));
    }

    @Operation(summary = "修改机构周边信息")
    @SaCheckPermission("park:periphery:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkPeripheryUpdateDTO dto) {
        parkPeripheryService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除机构周边信息")
    @SaCheckPermission("park:periphery:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkPeripheryService.delete(id);
        return R.ok();
    }
}
