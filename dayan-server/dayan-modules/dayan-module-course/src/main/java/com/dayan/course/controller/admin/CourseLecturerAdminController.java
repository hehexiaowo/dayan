package com.dayan.course.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.course.dto.CourseLecturerCreateDTO;
import com.dayan.course.dto.CourseLecturerQueryDTO;
import com.dayan.course.dto.CourseLecturerUpdateDTO;
import com.dayan.course.service.CourseLecturerService;
import com.dayan.course.vo.CourseLecturerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端课程讲师接口。
 *
 * <p>路径：{@code /course/lecturer/*}。
 */
@Tag(name = "课程讲师管理")
@RestController
@RequestMapping("/course/lecturer")
@RequiredArgsConstructor
public class CourseLecturerAdminController {

    private final CourseLecturerService courseLecturerService;

    @Operation(summary = "课程讲师分页列表")
    @GetMapping("/page")
    public R<PageResult<CourseLecturerVO>> page(CourseLecturerQueryDTO query) {
        return R.ok(courseLecturerService.page(query));
    }

    @Operation(summary = "课程讲师列表")
    @GetMapping("/list")
    public R<List<CourseLecturerVO>> list(CourseLecturerQueryDTO query) {
        return R.ok(courseLecturerService.list(query));
    }

    @Operation(summary = "课程讲师详情")
    @GetMapping("/{id}")
    public R<CourseLecturerVO> getDetail(@PathVariable Long id) {
        return R.ok(courseLecturerService.getDetail(id));
    }

    @Operation(summary = "新增课程讲师")
    @OperationLog(module = "课程讲师", action = "新增")
    @PostMapping
    public R<String> create(@RequestBody @Valid CourseLecturerCreateDTO dto) {
        return R.ok(courseLecturerService.create(dto));
    }

    @Operation(summary = "修改课程讲师")
    @OperationLog(module = "课程讲师", action = "修改")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody CourseLecturerUpdateDTO dto) {
        courseLecturerService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除课程讲师")
    @OperationLog(module = "课程讲师", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        courseLecturerService.delete(id);
        return R.ok();
    }
}
