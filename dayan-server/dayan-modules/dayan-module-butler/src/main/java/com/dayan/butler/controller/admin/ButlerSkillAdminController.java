package com.dayan.butler.controller.admin;

import com.dayan.butler.dto.ButlerSkillCreateDTO;
import com.dayan.butler.dto.ButlerSkillQueryDTO;
import com.dayan.butler.dto.ButlerSkillUpdateDTO;
import com.dayan.butler.service.ButlerSkillService;
import com.dayan.butler.vo.ButlerSkillVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端管家技能接口。
 *
 * <p>路径：{@code /butler/skill/*}。
 */
@Tag(name = "管家技能管理")
@RestController
@RequestMapping("/butler/skill")
@RequiredArgsConstructor
public class ButlerSkillAdminController {

    private final ButlerSkillService butlerSkillService;

    @Operation(summary = "管家技能分页列表")
    @GetMapping("/page")
    public R<PageResult<ButlerSkillVO>> page(ButlerSkillQueryDTO query) {
        return R.ok(butlerSkillService.page(query));
    }

    @Operation(summary = "管家技能列表")
    @GetMapping("/list")
    public R<List<ButlerSkillVO>> list(ButlerSkillQueryDTO query) {
        return R.ok(butlerSkillService.list(query));
    }

    @Operation(summary = "管家技能详情")
    @GetMapping("/{id}")
    public R<ButlerSkillVO> getDetail(@PathVariable Long id) {
        return R.ok(butlerSkillService.getDetail(id));
    }

    @Operation(summary = "新增管家技能")
    @OperationLog(module = "管家技能", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ButlerSkillCreateDTO dto) {
        return R.ok(butlerSkillService.create(dto));
    }

    @Operation(summary = "修改管家技能")
    @OperationLog(module = "管家技能", action = "修改")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody ButlerSkillUpdateDTO dto) {
        butlerSkillService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除管家技能")
    @OperationLog(module = "管家技能", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        butlerSkillService.delete(id);
        return R.ok();
    }
}
