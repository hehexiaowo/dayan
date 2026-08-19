package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.system.service.SystemKnowledgeRepoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端数据中心文件接口（百炼数据管理-文件，业务空间级）。
 *
 * <p>路径前缀 {@code /system/knowledge/files}。删除文件不影响已建知识库
 * （库内文档删除走 DeleteIndexDocument，即 repos/{id}/documents/{fileId}）。
 */
@Tag(name = "知识仓库数据中心文件")
@RestController
@RequestMapping("/system/knowledge/files")
@RequiredArgsConstructor
public class SystemKnowledgeFileAdminController {

    private final SystemKnowledgeRepoService knowledgeRepoService;

    @Operation(summary = "删除数据中心文件（不影响已建知识库）")
    @SaCheckPermission("system:knowledge:doc:delete")
    @DeleteMapping("/{fileId}")
    public R<Void> deleteFile(@PathVariable String fileId) {
        knowledgeRepoService.deleteDataCenterFile(fileId);
        return R.ok();
    }
}
