package com.dayan.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
/**
 * 表 course_lecturer 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_lecturer")
public class CourseLecturer extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 讲师编码 */
    private String lecturerCode;

    /** 讲师姓名 */
    private String lecturerName;

    /** 性别 */
    private Integer gender;

    /** 头像URL */
    private String avatar;

    /** 职称/头衔 */
    private String title;

    /** 所属机构 */
    private String organization;

    /** 擅长领域 */
    private String specialty;

    /** 讲师简介 */
    private String introduction;

    /** 资质证书 */
    private String certifications;

    /** 联系电话 */
    private String phone;

    /** 联系邮箱 */
    private String email;

    /** 开课数量 */
    private Integer courseCount;

    /** 学员总数 */
    private Integer studentCount;

    /** 平均评分 */
    private BigDecimal ratingAvg;

    /** 是否平台认证 */
    private Integer isCertified;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
