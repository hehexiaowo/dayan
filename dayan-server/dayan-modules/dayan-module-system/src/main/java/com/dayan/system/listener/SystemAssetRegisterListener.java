package com.dayan.system.listener;

import com.dayan.common.core.event.FileUploadedEvent;
import com.dayan.system.service.SystemAssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 文件上传 → 素材仓库登记监听器。
 *
 * <p>同步 @EventListener：registerIfAbsent 幂等（parkCode+assetUrl+sourceType+sourceRef），
 * 登记失败抛异常使上传接口返回失败，杜绝「上传成功但素材仓库缺失」。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemAssetRegisterListener {

    private final SystemAssetService systemAssetService;

    @EventListener
    public void onFileUploaded(FileUploadedEvent event) {
        if (!event.isAssetRegister()) {
            return;
        }
        systemAssetService.registerIfAbsent(
                blankToNull(event.getAssetParkCode()),
                event.getAssetType() != null ? event.getAssetType() : inferAssetType(event.getContentType()),
                event.getKey(),
                blankToNull(event.getAssetSourceType()),
                blankToNull(event.getAssetSourceRef()),
                event.getOriginalName(),
                event.getSize());
        log.info("素材登记完成: key={}, parkCode={}", event.getKey(), event.getAssetParkCode());
    }

    private Integer inferAssetType(String contentType) {
        if (contentType != null && contentType.startsWith("image/")) {
            return 1;
        }
        if (contentType != null && contentType.startsWith("video/")) {
            return 2;
        }
        return 3;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
