package com.dayan.equity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发起更换权益人（changeHolder）入参。
 *
 * <p>校验 equity_status=2（已激活），状态机 change_holder:2→7，
 * 插 equity_change_holder 记录（change_status=0 待处理），同时校验同一权益无在途记录。
 */
@Data
public class ChangeHolderDTO {

    @NotBlank(message = "权益编码不能为空")
    private String equityCode;

    /** 原权益使用人编码（可空，自动取当前默认使用人） */
    private String oldUsePersonCode;

    @NotBlank(message = "新权益人姓名不能为空")
    private String newPersonName;

    /** 新权益人身份证号（明文，加密存储） */
    private String newPersonIdCard;

    /** 新权益人手机号 */
    private String newPersonPhone;

    @NotBlank(message = "更换原因不能为空")
    private String changeReason;

    /** 操作人编码 */
    private String operatorCode;
}
