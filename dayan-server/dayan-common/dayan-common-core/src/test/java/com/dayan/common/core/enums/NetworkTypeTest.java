package com.dayan.common.core.enums;

import com.dayan.common.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NetworkTypeTest {

    @Test
    void of_合法code返回枚举() {
        assertThat(NetworkType.of("vital")).isEqualTo(NetworkType.VITAL);
        assertThat(NetworkType.of("care")).isEqualTo(NetworkType.CARE);
        assertThat(NetworkType.of("sojourn")).isEqualTo(NetworkType.SOJOURN);
        assertThat(NetworkType.of("VITAL")).isNull();
        assertThat(NetworkType.of("xxx")).isNull();
    }

    @Test
    void normalizeTags_空值返回null() {
        assertThat(NetworkType.normalizeTags(null)).isNull();
        assertThat(NetworkType.normalizeTags("  ")).isNull();
        assertThat(NetworkType.normalizeTags(",")).isNull();
    }

    @Test
    void normalizeTags_合法串去重保序() {
        assertThat(NetworkType.normalizeTags("vital,care")).isEqualTo("vital,care");
        assertThat(NetworkType.normalizeTags(" care ,vital,care ")).isEqualTo("care,vital");
    }

    @Test
    void normalizeTags_非法值抛业务异常() {
        assertThatThrownBy(() -> NetworkType.normalizeTags("vital,bad"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bad");
    }
}
