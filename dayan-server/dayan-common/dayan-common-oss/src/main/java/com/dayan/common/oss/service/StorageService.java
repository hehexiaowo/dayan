package com.dayan.common.oss.service;

import java.io.InputStream;

/**
 * 文件存储抽象接口。当前实现 MinioStorageService；
 * 预留未来 OSS/COS 实现，以及预签名直传扩展（接口不变，加 presign 方法即可）。
 */
public interface StorageService {

    /**
     * 上传文件，返回生成的 objectKey。
     *
     * @param module        业务模块（goods/scene/park 等）
     * @param channelCode   渠道编码
     * @param is            文件输入流
     * @param size          文件大小（字节）
     * @param contentType   MIME 类型
     * @param originalName  原始文件名（用于提取后缀，不保留原名）
     * @return objectKey，如 goods/day001/2026/08/08/abc.jpg
     */
    String upload(String module, String channelCode, InputStream is, long size,
                  String contentType, String originalName);

    /** 下载文件，返回输入流（调用方负责 close）。 */
    InputStream download(String key);

    /** 删除文件。 */
    void delete(String key);

    /** 判断文件是否存在。 */
    boolean exists(String key);

    /** 根据 key 推断 Content-Type。 */
    String contentType(String key);
}
