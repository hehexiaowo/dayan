package com.dayan.course.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.course.dto.CourseRecordLearnCreateDTO;
import com.dayan.course.dto.CourseRecordLearnQueryDTO;
import com.dayan.course.dto.CourseRecordLearnUpdateDTO;
import com.dayan.course.service.CourseRecordLearnService;
import com.dayan.course.vo.CourseRecordLearnVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端课程学习记录接口。
 *
 * <p>路径：{@code /course/record-learn/*}。
 */
@Tag(name = "课程学习记录管理")
@RestController
@RequestMapping("/course/record-learn")
@RequiredArgsConstructor
public class CourseRecordLearnAdminController {

    private final CourseRecordLearnService courseRecordLearnService;

    @Operation(summary = "学习记录分页列表")
    @GetMapping("/page")
    public R<PageResult<CourseRecordLearnVO>> page(CourseRecordLearnQueryDTO query) {
        return R.ok(courseRecordLearnService.page(query));
    }

    @Operation(summary = "学习记录列表")
    @GetMapping("/list")
    public R<List<CourseRecordLearnVO>> list(CourseRecordLearnQueryDTO query) {
        return R.ok(courseRecordLearnService.list(query));
    }

    @Operation(summary = "学习记录详情")
    @GetMapping("/{id}")
    public R<CourseRecordLearnVO> getDetail(@PathVariable Long id) {
        return R.ok(courseRecordLearnService.getDetail(id));
    }

    @Operation(summary = "新增学习记录")
    @OperationLog(module = "课程学习记录", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid CourseRecordLearnCreateDTO dto) {
        return R.ok(courseRecordLearnService.create(dto));
    }

    @Operation(summary = "修改学习记录")
    @OperationLog(module = "课程学习记录", action = "修改")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody CourseRecordLearnUpdateDTO dto) {
        courseRecordLearnService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除学习记录")
    @OperationLog(module = "课程学习记录", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        courseRecordLearnService.delete(id);
        return R.ok();
    }
}
