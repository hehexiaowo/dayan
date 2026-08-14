package com.dayan.park.listener;

import com.dayan.common.core.event.FileUploadedEvent;
import com.dayan.park.service.ParkAssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 文件上传 → 素材库登记监听器。
 *
 * <p>同步 @EventListener：registerIfAbsent 幂等（parkCode+assetUrl+sourceType+sourceRef），
 * 登记失败抛异常使上传接口返回失败，杜绝「上传成功但素材库缺失」。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParkAssetRegisterListener {

    private final ParkAssetService parkAssetService;

    @EventListener
    public void onFileUploaded(FileUploadedEvent event) {
        if (!event.isAssetRegister()) {
            return;
        }
        parkAssetService.registerIfAbsent(
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
