package com.dayan.equity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 完成更换权益人（changeDone）入参。
 *
 * <p>校验 change 记录存在且 change_status=0，状态机 change_done:7→2，更新 change_status=1。
 * 切换 equity_use_person.is_default_holder（旧的置 0，新的置 1）；
 * 若新使用人不存在则按入参创建。
 */
@Data
public class ChangeDoneDTO {

    @NotBlank(message = "权益编码不能为空")
    private String equityCode;

    /** change_holder 记录 id 或 equity_code 唯一在途记录定位 */
    @NotNull(message = "更换记录 id 不能为空")
    private Long changeId;

    /** 新权益使用人编码（已存在则切换默认；为空则按 newPersonName/newPersonIdCard 新建） */
    private String newUsePersonCode;

    private String newPersonName;
    private String newPersonIdCard;
    private String newPersonPhone;

    /** 操作人编码 */
    private String operatorCode;
}
