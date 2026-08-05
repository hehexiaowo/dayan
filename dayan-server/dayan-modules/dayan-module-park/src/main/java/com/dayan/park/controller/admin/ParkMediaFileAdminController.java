package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkMediaFileCreateDTO;
import com.dayan.park.dto.ParkMediaFileQueryDTO;
import com.dayan.park.dto.ParkMediaFileUpdateDTO;
import com.dayan.park.service.ParkMediaFileService;
import com.dayan.park.vo.ParkMediaFileVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构文件接口。
 *
 * <p>路径前缀 {@code /park/media-file}。
 */
@Tag(name = "机构文件管理")
@RestController
@RequestMapping("/park/media-file")
@RequiredArgsConstructor
public class ParkMediaFileAdminController {

    private final ParkMediaFileService parkMediaFileService;

    @Operation(summary = "机构文件分页列表")
    @SaCheckPermission("park:media-file:list")
    @GetMapping("/page")
    public R<PageResult<ParkMediaFileVO>> page(ParkMediaFileQueryDTO query) {
        return R.ok(parkMediaFileService.page(query));
    }

    @Operation(summary = "机构文件列表（按机构）")
    @SaCheckPermission("park:media-file:list")
    @GetMapping("/list")
    public R<List<ParkMediaFileVO>> list(@RequestParam String parkCode) {
        return R.ok(parkMediaFileService.listByPark(parkCode));
    }

    @Operation(summary = "机构文件详情")
    @SaCheckPermission("park:media-file:query")
    @GetMapping("/{id}")
    public R<ParkMediaFileVO> getDetail(@PathVariable Long id) {
        return R.ok(parkMediaFileService.getDetail(id));
    }

    @Operation(summary = "新增机构文件")
    @SaCheckPermission("park:media-file:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkMediaFileCreateDTO dto) {
        return R.ok(parkMediaFileService.create(dto));
    }

    @Operation(summary = "修改机构文件")
    @SaCheckPermission("park:media-file:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkMediaFileUpdateDTO dto) {
        parkMediaFileService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除机构文件")
    @SaCheckPermission("park:media-file:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkMediaFileService.delete(id);
        return R.ok();
    }
}
