package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkMediaVideoCreateDTO;
import com.dayan.park.dto.ParkMediaVideoQueryDTO;
import com.dayan.park.dto.ParkMediaVideoUpdateDTO;
import com.dayan.park.service.ParkMediaVideoService;
import com.dayan.park.vo.ParkMediaVideoVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构视频接口。
 *
 * <p>路径前缀 {@code /park/media-video}。
 */
@Tag(name = "机构视频管理")
@RestController
@RequestMapping("/park/media-video")
@RequiredArgsConstructor
public class ParkMediaVideoAdminController {

    private final ParkMediaVideoService parkMediaVideoService;

    @Operation(summary = "机构视频分页列表")
    @SaCheckPermission("park:media-video:list")
    @GetMapping("/page")
    public R<PageResult<ParkMediaVideoVO>> page(ParkMediaVideoQueryDTO query) {
        return R.ok(parkMediaVideoService.page(query));
    }

    @Operation(summary = "机构视频列表（按机构）")
    @SaCheckPermission("park:media-video:list")
    @GetMapping("/list")
    public R<List<ParkMediaVideoVO>> list(@RequestParam String parkCode) {
        return R.ok(parkMediaVideoService.listByPark(parkCode));
    }

    @Operation(summary = "机构视频详情")
    @SaCheckPermission("park:media-video:query")
    @GetMapping("/{id}")
    public R<ParkMediaVideoVO> getDetail(@PathVariable Long id) {
        return R.ok(parkMediaVideoService.getDetail(id));
    }

    @Operation(summary = "新增机构视频")
    @SaCheckPermission("park:media-video:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkMediaVideoCreateDTO dto) {
        return R.ok(parkMediaVideoService.create(dto));
    }

    @Operation(summary = "修改机构视频")
    @SaCheckPermission("park:media-video:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkMediaVideoUpdateDTO dto) {
        parkMediaVideoService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除机构视频")
    @SaCheckPermission("park:media-video:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkMediaVideoService.delete(id);
        return R.ok();
    }
}
