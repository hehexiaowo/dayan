package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkAdviserCreateDTO;
import com.dayan.park.dto.ParkAdviserQueryDTO;
import com.dayan.park.dto.ParkAdviserUpdateDTO;
import com.dayan.park.service.ParkAdviserService;
import com.dayan.park.vo.ParkAdviserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构顾问接口。
 *
 * <p>路径前缀 {@code /park/adviser}。
 */
@Tag(name = "机构顾问管理")
@RestController
@RequestMapping("/park/adviser")
@RequiredArgsConstructor
public class ParkAdviserAdminController {

    private final ParkAdviserService parkAdviserService;

    @Operation(summary = "机构顾问分页列表")
    @GetMapping("/page")
    public R<PageResult<ParkAdviserVO>> page(ParkAdviserQueryDTO query) {
        return R.ok(parkAdviserService.page(query));
    }

    @Operation(summary = "机构顾问列表（按机构）")
    @GetMapping("/list")
    public R<List<ParkAdviserVO>> list(@RequestParam String parkCode) {
        return R.ok(parkAdviserService.listByPark(parkCode));
    }

    @Operation(summary = "机构顾问详情")
    @GetMapping("/{id}")
    public R<ParkAdviserVO> getDetail(@PathVariable Long id) {
        return R.ok(parkAdviserService.getDetail(id));
    }

    @Operation(summary = "新增机构顾问")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkAdviserCreateDTO dto) {
        return R.ok(parkAdviserService.create(dto));
    }

    @Operation(summary = "修改机构顾问")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkAdviserUpdateDTO dto) {
        parkAdviserService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除机构顾问")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkAdviserService.delete(id);
        return R.ok();
    }
}
