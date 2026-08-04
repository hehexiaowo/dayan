package com.dayan.common.log.sensitive;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SensitiveUtil} 单元测试。
 */
class SensitiveUtilTest {

    @Test
    void phone_shouldMaskMiddle4Digits() {
        assertThat(SensitiveUtil.phone("13812345678")).isEqualTo("138****5678");
    }

    @Test
    void phone_shouldReturnAsIsWhenTooShort() {
        assertThat(SensitiveUtil.phone("12345")).isEqualTo("12345");
        assertThat(SensitiveUtil.phone(null)).isNull();
    }

    @Test
    void idCard_shouldMaskMiddle() {
        String masked = SensitiveUtil.idCard("110101199001011234");
        assertThat(masked).startsWith("1101");
        assertThat(masked).endsWith("1234");
        assertThat(masked).contains("*");
    }

    @Test
    void bankCard_shouldMaskMiddle() {
        String masked = SensitiveUtil.bankCard("6222021234567890123");
        assertThat(masked).startsWith("6222");
        assertThat(masked).endsWith("0123");
        assertThat(masked).contains("*");
    }

    @Test
    void email_shouldMaskLocalPart() {
        assertThat(SensitiveUtil.email("admin@example.com")).isEqualTo("a***@example.com");
    }

    @Test
    void email_shouldReturnAsIsWhenNoAt() {
        assertThat(SensitiveUtil.email("notanemail")).isEqualTo("notanemail");
    }
}
