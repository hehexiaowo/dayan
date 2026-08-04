package com.dayan.common.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.dayan.common.mybatis.tenant.DayanTenantHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置。
 *
 * <p>注册三个 InnerInterceptor，顺序重要：
 * <ol>
 *   <li>{@link TenantLineInnerInterceptor} - 渠道字段级隔离（最先执行，确保所有 SQL 都带渠道条件）</li>
 *   <li>{@link PaginationInnerInterceptor} - 分页</li>
 *   <li>{@link OptimisticLockerInnerInterceptor} - 乐观锁（基于 version 字段）</li>
 * </ol>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public DayanTenantHandler dayanTenantHandler(
            @Value("${dayan.tenant.ignore-tables:}") String[] ignoreTables) {
        return new DayanTenantHandler(ignoreTables);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DayanTenantHandler tenantHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 租户（渠道）隔离
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantHandler));
        // 2. 分页
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L); // 单页最大 500 条，防止超大查询
        pagination.setOverflow(true); // 页码越界自动归 1
        interceptor.addInnerInterceptor(pagination);
        // 3. 乐观锁
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
