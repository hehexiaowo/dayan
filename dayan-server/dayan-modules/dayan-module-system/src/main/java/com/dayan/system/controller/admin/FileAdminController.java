package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.common.oss.config.StorageProperties;
import com.dayan.common.oss.dto.FileUploadDTO;
import com.dayan.common.oss.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 文件上传/预览 Controller（admin 端）。
 * - POST /v1/files/upload   上传，返回 key（存 DB）。按钮级权限 system:file:upload。
 * - GET  /v1/files/preview/**  代理下载（同源零 CORS）。
 *   该接口经 <img> 直接引用、请求不携带凭证，故不设登录校验；
 *   安全性由「key 不可猜测 + 后缀/内容白名单 + 非白名单类型强制下载」保证。
 */
@Tag(name = "文件管理")
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileAdminController {

    private final StorageService storageService;
    private final StorageProperties storageProperties;

    /** 允许的文件后缀白名单 */
    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "webp",
            "mp4", "webm",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt");

    /** 合法 key 字符集（字母/数字/_-/. /），防止日志注入与异常输入 */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_/\\-.]+$");

    /** preview 允许内联渲染的 contentType（其余强制下载，避免 XSS） */
    private static final Set<String> INLINE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm",
            "application/pdf");

    @Operation(summary = "上传文件")
    @SaCheckPermission("system:file:upload")
    @PostMapping("/upload")
    public R<FileUploadDTO> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "module", required = false) String module) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        long size = file.getSize();
        if (size > storageProperties.getMaxSize()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "文件大小超过限制 " + (storageProperties.getMaxSize() / 1024 / 1024) + "MB");
        }
        String originalName = file.getOriginalFilename();
        String ext = cn.hutool.core.io.FileUtil.extName(originalName);
        if (StrUtil.isBlank(ext) || !ALLOWED_EXT.contains(ext.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的文件类型: " + ext);
        }
        String channelCode = "common";
        try {
            // 从 Sa-Token 获取当前渠道编码（若登录上下文有）
            channelCode = (String) StpUtil.getSession().get("channelCode");
            if (StrUtil.isBlank(channelCode)) {
                channelCode = "admin";
            }
        } catch (Exception ignored) {
            // 未登录或无 session 时用默认值
        }
        String mod = StrUtil.isBlank(module) ? "common" : module;
        try {
            String key = storageService.upload(mod, channelCode,
                    file.getInputStream(), size, file.getContentType(), originalName);
            FileUploadDTO dto = new FileUploadDTO();
            dto.setKey(key);
            dto.setUrl("/admin-api/v1/files/preview/" + key);
            dto.setOriginalName(originalName);
            dto.setSize(size);
            return R.ok(dto);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "预览/下载文件（代理下载）")
    @GetMapping("/preview/**")
    public void preview(HttpServletRequest request, HttpServletResponse response) {
        // key 含斜杠（如 goods/day001/2026/08/08/abc.jpg），从 URI 提取 preview/ 之后的部分
        String uri = request.getRequestURI();
        String prefix = "/admin-api/v1/files/preview/";
        String key = uri.substring(uri.indexOf(prefix) + prefix.length());
        if (StrUtil.isBlank(key) || !KEY_PATTERN.matcher(key).matches()) {
            response.setStatus(404);
            return;
        }
        if (!storageService.exists(key)) {
            response.setStatus(404);
            return;
        }
        // 非图片/视频/PDF 类型强制下载，防止同源 XSS（key 后缀可伪造）
        String contentType = storageService.contentType(key);
        if (!INLINE_CONTENT_TYPES.contains(contentType)) {
            contentType = "application/octet-stream";
            response.setHeader("Content-Disposition", "attachment; filename=\"" + key.substring(key.lastIndexOf('/') + 1) + "\"");
        }
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=86400");
        try (InputStream is = storageService.download(key);
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
            os.flush();
        } catch (Exception e) {
            log.error("文件预览失败 key={}", key, e);
            response.setStatus(500);
        }
    }
}
