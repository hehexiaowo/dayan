package com.dayan.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 持卡人发起服务请求入参（client 端）。
 *
 * <p>持卡人为权益人选择一个服务项目（旅游短居/长居/照护）发起一次服务请求，
 * 后端校验权益归属 + 配额剩余后创建 service_session（状态=1 待分配）。
 *
 * <p>此 DTO 定义在 service 模块以便 controller 复用，但发起端点位于 equity 模块的
 * client controller（因需跨 equity+goods+service 三域校验）。
 */
@Data
public class ClientServiceRequestDTO {

    /** 权益编码 */
    @NotBlank(message = "权益编码不能为空")
    private String equityCode;

    /** 服务项目编码（如 SI00001=旅游短居） */
    @NotBlank(message = "服务项目不能为空")
    private String itemCode;

    /** 权益使用人 ID（为哪个使用人发起，从 equity_use_person 选） */
    @NotBlank(message = "权益人不能为空")
    private String usePersonId;

    /** 需求描述（选填） */
    @Size(max = 500, message = "需求描述不能超过500字")
    private String demandDesc;
}

