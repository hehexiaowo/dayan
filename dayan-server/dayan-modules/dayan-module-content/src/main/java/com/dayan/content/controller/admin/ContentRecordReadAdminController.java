package com.dayan.content.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.content.dto.ContentRecordReadCreateDTO;
import com.dayan.content.dto.ContentRecordReadQueryDTO;
import com.dayan.content.service.ContentRecordReadService;
import com.dayan.content.vo.ContentReadStatsVO;
import com.dayan.content.vo.ContentRecordReadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端内容阅读记录接口（含 UV/PV 统计）。
 *
 * <p>路径：{@code /content/record-read/*}（由 dayan-admin context-path=/admin-api 拼接为
 * {@code /admin-api/content/record-read/*}）。
 *
 * <p>核心动作：
 * <ul>
 *   <li>{@code POST /content/record-read}：记录阅读事件（前端上报）</li>
 *   <li>{@code GET /content/record-read/stats?contentCode=}：UV/PV 统计</li>
 * </ul>
 */
@Tag(name = "内容阅读记录")
@RestController
@RequestMapping("/content/record-read")
@RequiredArgsConstructor
public class ContentRecordReadAdminController {

    private final ContentRecordReadService contentRecordReadService;

    @Operation(summary = "阅读记录分页列表（按 contentCode）")
    @GetMapping("/page")
    public R<PageResult<ContentRecordReadVO>> page(ContentRecordReadQueryDTO query) {
        return R.ok(contentRecordReadService.page(query));
    }

    @Operation(summary = "记录阅读事件（前端上报）")
    @OperationLog(module = "阅读记录", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ContentRecordReadCreateDTO dto) {
        return R.ok(contentRecordReadService.create(dto));
    }

    @Operation(summary = "删除阅读记录")
    @OperationLog(module = "阅读记录", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contentRecordReadService.delete(id);
        return R.ok();
    }

    @Operation(summary = "按 contentCode 统计阅读 UV/PV")
    @GetMapping("/stats")
    public R<ContentReadStatsVO> stats(@RequestParam String contentCode) {
        return R.ok(contentRecordReadService.stats(contentCode));
    }
}
