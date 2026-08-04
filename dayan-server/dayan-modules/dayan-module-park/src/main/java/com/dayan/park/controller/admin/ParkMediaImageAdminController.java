package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkMediaImageCreateDTO;
import com.dayan.park.dto.ParkMediaImageQueryDTO;
import com.dayan.park.dto.ParkMediaImageUpdateDTO;
import com.dayan.park.service.ParkMediaImageService;
import com.dayan.park.vo.ParkMediaImageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构图片接口。
 *
 * <p>路径前缀 {@code /park/media-image}，拼接为 {@code /admin-api/park/media-image/*}。
 */
@Tag(name = "机构图片管理")
@RestController
@RequestMapping("/park/media-image")
@RequiredArgsConstructor
public class ParkMediaImageAdminController {

    private final ParkMediaImageService parkMediaImageService;

    @Operation(summary = "机构图片分页列表")
    @GetMapping("/page")
    public R<PageResult<ParkMediaImageVO>> page(ParkMediaImageQueryDTO query) {
        return R.ok(parkMediaImageService.page(query));
    }

    @Operation(summary = "机构图片列表（按机构）")
    @GetMapping("/list")
    public R<List<ParkMediaImageVO>> list(@RequestParam String parkCode) {
        return R.ok(parkMediaImageService.listByPark(parkCode));
    }

    @Operation(summary = "机构图片详情")
    @GetMapping("/{id}")
    public R<ParkMediaImageVO> getDetail(@PathVariable Long id) {
        return R.ok(parkMediaImageService.getDetail(id));
    }

    @Operation(summary = "新增机构图片")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkMediaImageCreateDTO dto) {
        return R.ok(parkMediaImageService.create(dto));
    }

    @Operation(summary = "修改机构图片")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkMediaImageUpdateDTO dto) {
        parkMediaImageService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除机构图片")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkMediaImageService.delete(id);
        return R.ok();
    }
}
