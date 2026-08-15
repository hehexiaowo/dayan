package com.dayan.common.aliyun;

import lombok.Builder;
import lombok.Data;

/**
 * 百炼凭据载体（由业务模块从 system_config 组装注入，本模块不做任何持久化）。
 * 两套凭据相互独立：
 * - AccessKey 三件套：知识库管理 OpenAPI（RAM 子账号，需 AliyunBailianDataFullAccess 并加入业务空间）；
 * - API-Key + 网关域名：模型推理（OpenAI 兼容专属网关，AI 问答/检索重写用）。
 */
@Data
@Builder
public class BailianProperties {

    /** 知识库管理 AccessKey ID（RAM 子账号） */
    private String accessKeyId;

    /** 知识库管理 AccessKey Secret */
    private String accessKeySecret;

    /** 百炼业务空间 ID（WorkspaceId） */
    private String workspaceId;

    /** 百炼服务地域（默认 cn-beijing，endpoint = bailian.{region}.aliyuncs.com） */
    private String region;

    /** 模型推理 API-Key（sk- 开头） */
    private String apiKey;

    /** 专属网关域名（不含协议与路径，如 llm-xxx.cn-beijing.maas.aliyuncs.com） */
    private String apiHost;

    /** AI 问答模型名（默认 qwen-plus） */
    private String chatModel;
}
