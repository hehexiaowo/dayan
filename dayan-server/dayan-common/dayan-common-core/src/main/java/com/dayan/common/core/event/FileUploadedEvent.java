package com.dayan.common.core.event;

import lombok.Data;

/**
 * 文件上传成功事件。
 *
 * <p>由 FileAdminController 在上传成功后同步发布；park 模块 ParkAssetRegisterListener
 * 监听并幂等登记素材库。同步事件：监听器抛异常会使上传接口整体返回失败
 * （OSS 对象可能残留，与「对象只增不减」策略一致，可接受）。
 */
@Data
public class FileUploadedEvent {

    /** 对象 key（DB 持久化值） */
    private String key;
    /** 原始文件名 */
    private String originalName;
    /** 文件大小（字节） */
    private long size;
    /** MIME 类型 */
    private String contentType;
    /** 上传模块（oss key 前缀） */
    private String module;

    /** 是否登记素材库 */
    private boolean assetRegister;
    /** 归属机构编码（空=平台素材） */
    private String assetParkCode;
    /** 素材类型（1图/2视频/3文件/4VR，缺省按 contentType 推断） */
    private Integer assetType;
    /** 来源类型（如 room_type/display_block） */
    private String assetSourceType;
    /** 来源编码 */
    private String assetSourceRef;
}
