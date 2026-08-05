package com.dayan.system.log;

import com.dayan.common.log.operation.OperatorResolver;
import com.dayan.common.mybatis.context.ContextHolder;
import org.springframework.stereotype.Component;

/**
 * 操作人信息解析器实现：从请求上下文（ThreadLocal）读取当前登录账号信息。
 *
 * <p>由 {@code SaTokenContextFilter} 在每次请求时填充 {@link ContextHolder}，
 * 本类供 {@code OperationLogAspect} 在主请求线程调用，把操作人信息注入操作日志。
 *
 * <p>放在 system 模块（该模块的 @Component 始终被 admin 启动类扫描），
 * 实现 common-log 定义的 {@link OperatorResolver} SPI。
 */
@Component
public class ContextOperatorResolver implements OperatorResolver {

    @Override
    public String resolveCode() {
        return ContextHolder.getAccountCode();
    }

    @Override
    public String resolveName() {
        return ContextHolder.getAccountName();
    }

    @Override
    public String resolveType() {
        return ContextHolder.getAccountType();
    }
}
