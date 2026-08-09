package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkDisplayBlockCreateDTO;
import com.dayan.park.dto.ParkDisplayBlockQueryDTO;
import com.dayan.park.dto.ParkDisplayBlockUpdateDTO;
import com.dayan.park.service.ParkDisplayBlockService;
import com.dayan.park.vo.ParkDisplayBlockVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构展示板块接口。
 *
 * <p>路径前缀 {@code /park/display-block}。
 */
@Tag(name = "机构展示板块管理")
@RestController
@RequestMapping("/park/display-block")
@RequiredArgsConstructor
public class ParkDisplayBlockAdminController {

    private final ParkDisplayBlockService parkDisplayBlockService;

    @Operation(summary = "展示板块分页列表")
    @SaCheckPermission("park:display-block:list")
    @GetMapping("/page")
    public R<PageResult<ParkDisplayBlockVO>> page(ParkDisplayBlockQueryDTO query) {
        return R.ok(parkDisplayBlockService.page(query));
    }

    @Operation(summary = "展示板块列表（按机构）")
    @SaCheckPermission("park:display-block:list")
    @GetMapping("/list")
    public R<List<ParkDisplayBlockVO>> list(@RequestParam String parkCode) {
        return R.ok(parkDisplayBlockService.listByPark(parkCode));
    }

    @Operation(summary = "展示板块详情")
    @SaCheckPermission("park:display-block:query")
    @GetMapping("/{id}")
    public R<ParkDisplayBlockVO> getDetail(@PathVariable Long id) {
        return R.ok(parkDisplayBlockService.getDetail(id));
    }

    @Operation(summary = "新增展示板块")
    @SaCheckPermission("park:display-block:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkDisplayBlockCreateDTO dto) {
        return R.ok(parkDisplayBlockService.create(dto));
    }

    @Operation(summary = "修改展示板块")
    @SaCheckPermission("park:display-block:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkDisplayBlockUpdateDTO dto) {
        parkDisplayBlockService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除展示板块")
    @SaCheckPermission("park:display-block:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkDisplayBlockService.delete(id);
        return R.ok();
    }
}
