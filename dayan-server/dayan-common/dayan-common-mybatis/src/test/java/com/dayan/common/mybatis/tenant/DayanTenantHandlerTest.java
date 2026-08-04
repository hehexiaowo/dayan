package com.dayan.common.mybatis.tenant;

import com.dayan.common.mybatis.context.ContextHolder;
import net.sf.jsqlparser.expression.StringValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DayanTenantHandler} 单元测试。
 */
class DayanTenantHandlerTest {

    private final DayanTenantHandler handler = new DayanTenantHandler(new String[]{"park_info", "channel_config_content"});

    @AfterEach
    void cleanup() {
        ContextHolder.clear();
    }

    @Test
    void tenantColumn_shouldBeChannelCode() {
        assertThat(handler.getTenantIdColumn()).isEqualTo("channel_code");
    }

    @Test
    void ignoreTable_shouldIgnoreSystemDomainByPrefix() {
        assertThat(handler.ignoreTable("system_dict_common")).isTrue();
        assertThat(handler.ignoreTable("system_menu")).isTrue();
        assertThat(handler.ignoreTable("`system_state_machine`")).isTrue();
    }

    @Test
    void ignoreTable_shouldIgnoreOrganAndButlerDomainByPrefix() {
        assertThat(handler.ignoreTable("organ_account")).isTrue();
        assertThat(handler.ignoreTable("butler_info")).isTrue();
        assertThat(handler.ignoreTable("distributor_info")).isTrue();
    }

    @Test
    void ignoreTable_shouldIgnoreConfiguredTables() {
        assertThat(handler.ignoreTable("park_info")).isTrue();
        assertThat(handler.ignoreTable("channel_config_content")).isTrue();
    }

    @Test
    void ignoreTable_shouldNotIgnoreChannelScopedTables() {
        // 含 channel_code 且需隔离的表不应被忽略
        assertThat(handler.ignoreTable("agent_info")).isFalse();
        assertThat(handler.ignoreTable("client_info")).isFalse();
        assertThat(handler.ignoreTable("equity_depot")).isFalse();
        assertThat(handler.ignoreTable("order_equity")).isFalse();
    }

    @Test
    void ignoreTable_shouldHandleDbPrefixAndBackticks() {
        assertThat(handler.ignoreTable("dayan.system_dict")).isTrue();
        assertThat(handler.ignoreTable("`organ_role`")).isTrue();
    }

    @Test
    void getTenantId_shouldReturnChannelCodeWhenPresent() {
        ContextHolder.setChannelCode("CH00001");
        StringValue tenantId = (StringValue) handler.getTenantId();
        assertThat(tenantId.getValue()).isEqualTo("CH00001");
    }

    @Test
    void getTenantId_shouldReturnZeroWhenNoChannel() {
        Object tenantId = handler.getTenantId();
        // 返回 LongValue(0)，避免未隔离请求意外命中带 channel_code 的数据
        assertThat(tenantId.toString()).isEqualTo("0");
    }
}
