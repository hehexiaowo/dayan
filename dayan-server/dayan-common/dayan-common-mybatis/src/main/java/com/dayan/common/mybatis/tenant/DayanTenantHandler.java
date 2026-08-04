package com.dayan.common.mybatis.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.dayan.common.mybatis.context.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashSet;
import java.util.Set;

/**
 * 多租户（渠道隔离）字段处理器。
 *
 * <p>渠道隔离机制：分片候选表（含 channel_code 字段，共 44 张）在查询/更新时自动追加
 * {@code channel_code = ?} 条件，由 MyBatis-Plus {@code TenantLineInnerInterceptor} 调用本类。
 *
 * <p>租户值来源：{@link ContextHolder#getChannelCode()}，由 dayan-common-security 的
 * SaToken 过滤器从当前登录会话写入。未登录或系统域操作时返回 0，配合忽略表清单放行。
 *
 * <p>忽略表清单（平台共享表，无 channel_code 字段）：
 * <ul>
 *   <li>系统域全部 18 张表（system_*）</li>
 *   <li>核心域全部 9 张表（organ_*）</li>
 *   <li>管家域 butler_* 8 张（平台共享）</li>
 *   <li>分销商 distributor_info 1 张</li>
 *   <li>权益域 equity_* 6 张（P4：Admin 全局管理，渠道归属通过 channel_code 字段显式查询）</li>
 *   <li>服务域 service_* 7 张（P5：Admin 全局视图，服务跨渠道分配管家）</li>
 *   <li>商品域 goods_* / 场景域 scene_* / 内容域 content_* / 课程域 course_*（P6：平台共享资源，Admin 全局管理）</li>
 *   <li>订单域 order_* / 结算域 finance_*（P7：平台全局资金链路，Admin 全局对账与结算）</li>
 *   <li>channel_info（渠道本身定义，非渠道内数据）</li>
 * </ul>
 * 具体忽略表名可通过配置项 {@code dayan.tenant.ignore-tables} 覆盖/追加。
 */
@Slf4j
public class DayanTenantHandler implements TenantLineHandler {

    /** 租户字段名 */
    public static final String TENANT_COLUMN = "channel_code";

    /** 平台共享表前缀（默认忽略） */
    private static final String[] DEFAULT_IGNORE_PREFIXES = {
            "system_", "organ_", "butler_", "distributor_", "equity_", "service_",
            "goods_", "scene_", "content_", "course_", "order_", "finance_"
    };

    /** 单独忽略的表（平台共享但不匹配前缀） */
    private static final String[] DEFAULT_IGNORE_TABLES = {
            "channel_info"
    };

    private final Set<String> ignoreTables;

    public DayanTenantHandler(
            @Value("${dayan.tenant.ignore-tables:}") String[] configuredIgnoreTables) {
        this.ignoreTables = new HashSet<>();
        // 默认忽略表
        for (String t : DEFAULT_IGNORE_TABLES) {
            ignoreTables.add(normalize(t));
        }
        // 配置追加的忽略表
        if (configuredIgnoreTables != null) {
            for (String t : configuredIgnoreTables) {
                if (t != null && !t.isBlank()) {
                    ignoreTables.add(normalize(t.trim()));
                }
            }
        }
    }

    @Override
    public Expression getTenantId() {
        String channelCode = ContextHolder.getChannelCode();
        if (channelCode == null || channelCode.isEmpty()) {
            // 未绑定渠道：返回一个不可能匹配的值，避免泄露跨渠道数据
            // （对忽略表不生效；对非忽略表返回 0，正常情况下系统域操作不走带 channel_code 的表）
            return new LongValue(0L);
        }
        return new StringValue(channelCode);
    }

    @Override
    public String getTenantIdColumn() {
        return TENANT_COLUMN;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (tableName == null) {
            return true;
        }
        String normalized = normalize(tableName);
        // 显式忽略清单
        if (ignoreTables.contains(normalized)) {
            return true;
        }
        // 默认前缀忽略
        for (String prefix : DEFAULT_IGNORE_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** 统一表名：去除反引号/引号/库名前缀，转小写 */
    private String normalize(String tableName) {
        String t = tableName;
        // 去除 `dbname`.`table` 的库前缀
        int dot = t.lastIndexOf('.');
        if (dot >= 0) {
            t = t.substring(dot + 1);
        }
        // 去除反引号
        t = t.replace("`", "").replace("\"", "");
        return t.toLowerCase();
    }
}