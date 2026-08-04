package com.dayan.butler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
/**
 * 表 butler_service_record 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("butler_service_record")
public class ButlerServiceRecord extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 管家编码 */
    private String butlerCode;

    /** 客户编码 */
    private String clientCode;

    /** 服务类型 */
    private Integer serviceType;

    /** 服务标题 */
    private String serviceTitle;

    /** 服务日期 */
    private LocalDate serviceDate;

    /** 状态 */
    private Integer status;

    /** 沟通方式 */
    private Integer communicateWay;

    /** 备注 */
    private String remark;
}
