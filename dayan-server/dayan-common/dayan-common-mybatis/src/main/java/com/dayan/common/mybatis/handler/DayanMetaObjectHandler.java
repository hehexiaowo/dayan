package com.dayan.common.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.dayan.common.mybatis.context.ContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器。
 *
 * <p>填充规则（对应 {@code BaseEntity} 的 {@code @TableField(fill=...)}）：
 * <ul>
 *   <li>INSERT：created_at / updated_at / creator / updater</li>
 *   <li>UPDATE：updated_at / updater</li>
 * </ul>
 *
 * <p>操作人来源 {@link ContextHolder#getAccountCode()}，未登录时填 {@code system}。
 */
@Component
public class DayanMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String operator = currentOperator();

        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "creator", String.class, operator);
        strictInsertFill(metaObject, "updater", String.class, operator);
        // 逻辑删除默认值 0（未删除）
        strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updater", String.class, currentOperator());
    }

    private String currentOperator() {
        String code = ContextHolder.getAccountCode();
        return (code == null || code.isEmpty()) ? ContextHolder.SYSTEM_OPERATOR : code;
    }
}
