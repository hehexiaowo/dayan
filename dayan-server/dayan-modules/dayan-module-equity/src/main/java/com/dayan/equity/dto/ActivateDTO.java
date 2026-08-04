package com.dayan.equity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 权益激活入参（按激活码或绑定码定位权益）。
 *
 * <p>核心链路：
 * <ol>
 *   <li>carrierType=1（卡）→ 按 activateCode 查 equity_depot；carrierType=2（函）→ 按 bindCode 查</li>
 *   <li>校验 equity_status=1（已出库），状态机 activate:1→2</li>
 *   <li>写 activate_time、expire_time=activate_time+template.valid_days、client_code</li>
 *   <li>插 equity_activate 记录（activate_code=AC+序列）</li>
 *   <li>联动 batch.activatedCount += 1</li>
 *   <li>自动创建默认使用人 equity_use_person（relation=本人，is_default_holder=1）</li>
 * </ol>
 */
@Data
public class ActivateDTO {

    /** 载体类型：1=权益卡（按 activateCode）/ 2=权益函（按 bindCode） */
    private Integer carrierType;

    /** 权益卡激活码（DY-8位）；carrierType=1 时必填 */
    private String activateCode;

    /** 权益函绑定码（BF-12位）；carrierType=2 时必填 */
    private String bindCode;

    @NotBlank(message = "激活客户编码不能为空")
    private String clientCode;

    /** 激活客户姓名（快照，同时作为默认使用人姓名） */
    private String clientFullName;

    /** 激活客户手机号 */
    private String clientPhone;

    /** 激活渠道：1=APP / 2=小程序 / 3=H5 / 4=管家代激活 / 5=代理人代激活 */
    private Integer activateChannel;

    /** 激活来源编码（如代理人编码） */
    private String activateSourceCode;

    /** 激活 IP */
    private String ipAddress;

    /** 设备信息 */
    private String deviceInfo;
}
