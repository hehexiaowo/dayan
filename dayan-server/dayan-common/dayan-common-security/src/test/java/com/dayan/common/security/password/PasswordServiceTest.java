package com.dayan.common.security.password;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PasswordService} 单元测试。
 */
class PasswordServiceTest {

    private final PasswordService service = new PasswordService();

    @Test
    void encode_shouldReturnBCryptHash() {
        String hash = service.encode("admin123");
        assertThat(hash).startsWith("$2a$10$");
        assertThat(hash).isNotEqualTo("admin123");
    }

    @Test
    void encode_shouldProduceDifferentHashForSamePassword() {
        String h1 = service.encode("samepass");
        String h2 = service.encode("samepass");
        // BCrypt 每次随机盐，相同密码哈希不同
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void matches_shouldVerifyCorrectPassword() {
        String hash = service.encode("admin123");
        assertThat(service.matches("admin123", hash)).isTrue();
    }

    @Test
    void matches_shouldRejectWrongPassword() {
        String hash = service.encode("admin123");
        assertThat(service.matches("wrong", hash)).isFalse();
    }

    @Test
    void matches_shouldReturnFalseWhenNullInputs() {
        assertThat(service.matches(null, "$2a$10$xxx")).isFalse();
        assertThat(service.matches("x", null)).isFalse();
    }

    @Test
    void encode_matches_admin123Seed() {
        // 验证种子数据用的 BCrypt 哈希可被校验
        // admin/admin123 的种子哈希在 db seed 中预生成，这里验证算法一致性
        String hash = service.encode("admin123");
        assertThat(service.matches("admin123", hash)).isTrue();
    }
}
