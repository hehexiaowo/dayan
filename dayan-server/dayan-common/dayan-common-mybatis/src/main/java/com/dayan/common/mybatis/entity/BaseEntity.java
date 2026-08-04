package com.dayan.common.mybatis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库实体公共字段基类。
 *
 * <p>127 张表均包含以下审计字段，Entity 统一继承本类以复用：
 * <ul>
 *   <li>{@code created_at} / {@code updated_at} - 创建/更新时间，由 {@code MetaObjectHandler} 自动填充</li>
 *   <li>{@code creator} / {@code updater} - 创建/更新人（账号编码），自动填充</li>
 *   <li>{@code deleted} - 逻辑删除标记（1=已删除，0=未删除）</li>
 *   <li>{@code deleted_at} - 删除时间（用于审计追溯）</li>
 * </ul>
 *
 * <p>命名遵循《项目开发规范》v1.1：逻辑删除采用 deleted(1/0) + deleted_at 双字段机制。
 */
@Data
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 创建人（账号编码或系统标识） */
    @TableField(value = "creator", fill = FieldFill.INSERT)
    private String creator;

    /** 更新人（账号编码或系统标识） */
    @TableField(value = "updater", fill = FieldFill.INSERT_UPDATE)
    private String updater;

    /** 逻辑删除标记：1=已删除，0=未删除 */
    @TableLogic
    @TableField(value = "deleted")
    private Integer deleted;

    /** 删除时间（逻辑删除时记录，用于审计追溯） */
    @TableField(value = "deleted_at")
    private LocalDateTime deletedAt;
}
