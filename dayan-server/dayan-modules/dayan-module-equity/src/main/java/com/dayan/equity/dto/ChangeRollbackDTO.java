package com.dayan.equity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 回滚更换权益人（changeRollback）入参。
 *
 * <p>change_status 0→2，equity_status 7→2（复用 change_done 事件回退权益状态，
 * 语义上回滚=权益恢复原持有人，权益本身仍处于已激活 2）。
 */
@Data
public class ChangeRollbackDTO {

    @NotBlank(message = "权益编码不能为空")
    private String equityCode;

    @NotNull(message = "更换记录 id 不能为空")
    private Long changeId;

    /** 操作人编码 */
    private String operatorCode;
}
