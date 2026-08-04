package com.dayan.course.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.course.dto.CourseInfoCreateDTO;
import com.dayan.course.dto.CourseInfoQueryDTO;
import com.dayan.course.dto.CourseInfoUpdateDTO;
import com.dayan.course.service.CourseInfoService;
import com.dayan.course.vo.CourseInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端课程信息接口。
 *
 * <p>路径：{@code /course/info/*}。
 */
@Tag(name = "课程信息管理")
@RestController
@RequestMapping("/course/info")
@RequiredArgsConstructor
public class CourseInfoAdminController {

    private final CourseInfoService courseInfoService;

    @Operation(summary = "课程信息分页列表")
    @GetMapping("/page")
    public R<PageResult<CourseInfoVO>> page(CourseInfoQueryDTO query) {
        return R.ok(courseInfoService.page(query));
    }

    @Operation(summary = "课程信息列表")
    @GetMapping("/list")
    public R<List<CourseInfoVO>> list(CourseInfoQueryDTO query) {
        return R.ok(courseInfoService.list(query));
    }

    @Operation(summary = "课程信息详情")
    @GetMapping("/{courseCode}")
    public R<CourseInfoVO> getDetail(@PathVariable String courseCode) {
        return R.ok(courseInfoService.getDetail(courseCode));
    }

    @Operation(summary = "新增课程信息")
    @OperationLog(module = "课程信息", action = "新增")
    @PostMapping
    public R<String> create(@RequestBody @Valid CourseInfoCreateDTO dto) {
        return R.ok(courseInfoService.create(dto));
    }

    @Operation(summary = "修改课程信息")
    @OperationLog(module = "课程信息", action = "修改")
    @PutMapping("/{courseCode}")
    public R<Void> update(@PathVariable String courseCode,
                          @RequestBody CourseInfoUpdateDTO dto) {
        courseInfoService.update(courseCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除课程信息")
    @OperationLog(module = "课程信息", action = "删除")
    @DeleteMapping("/{courseCode}")
    public R<Void> delete(@PathVariable String courseCode) {
        courseInfoService.delete(courseCode);
        return R.ok();
    }

    @Operation(summary = "课程上架")
    @OperationLog(module = "课程信息", action = "上架")
    @PutMapping("/{courseCode}/publish")
    public R<Void> publish(@PathVariable String courseCode) {
        courseInfoService.publish(courseCode);
        return R.ok();
    }

    @Operation(summary = "课程下架")
    @OperationLog(module = "课程信息", action = "下架")
    @PutMapping("/{courseCode}/offline")
    public R<Void> offline(@PathVariable String courseCode) {
        courseInfoService.offline(courseCode);
        return R.ok();
    }
}
