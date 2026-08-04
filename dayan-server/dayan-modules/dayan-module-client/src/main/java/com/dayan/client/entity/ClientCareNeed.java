package com.dayan.client.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 client_care_need 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("client_care_need")
public class ClientCareNeed extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 客户编码 */
    private String clientCode;

    /** 评估管家编码 */
    private String butlerCode;

    /** 评估管家姓名(快照) */
    private String butlerFullName;

    /** 评估日期 */
    private LocalDate evalDate;

    /** 建议照护等级 */
    private Integer careLevel;

    /** 偏好照护类型 */
    private String careTypePreference;

    /** 居住偏好 */
    private String livingPreference;

    /** 饮食偏好 */
    private String foodPreference;

    /** 预算下限(元/月) */
    private BigDecimal budgetMin;

    /** 预算上限(元/月) */
    private BigDecimal budgetMax;

    /** 区域偏好 */
    private String areaPreference;

    /** 特殊需求说明 */
    private String specialRequirements;

    /** 期望入住日期 */
    private LocalDate expectedCheckinDate;

    /** 推荐机构列表 */
    private String parkRecommendations;

    /** 评估结论 */
    private String evalResult;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
