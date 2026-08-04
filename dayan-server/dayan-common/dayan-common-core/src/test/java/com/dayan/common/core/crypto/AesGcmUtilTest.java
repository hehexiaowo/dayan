package com.dayan.common.core.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link AesGcmUtil} 单元测试。
 */
class AesGcmUtilTest {

    private static final String KEY_256 = "0123456789abcdef0123456789abcdef";
    private static final String KEY_256_HEX = "3031323334353637383961626364656630313233343536373839616263646566";

    @Test
    void encryptThenDecrypt_shouldRoundTrip() {
        String plaintext = "hello 大雁养老";
        String cipher = AesGcmUtil.encrypt(plaintext, KEY_256_HEX);
        String decrypted = AesGcmUtil.decrypt(cipher, KEY_256_HEX);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_shouldProduceDifferentCiphertextForSamePlaintext() {
        String plaintext = "same content";
        String c1 = AesGcmUtil.encrypt(plaintext, KEY_256_HEX);
        String c2 = AesGcmUtil.encrypt(plaintext, KEY_256_HEX);
        // GCM 每次随机 IV，密文应不同
        assertThat(c1).isNotEqualTo(c2);
    }

    @Test
    void decrypt_shouldFailOnTamperedCiphertext() {
        String cipher = AesGcmUtil.encrypt("secret", KEY_256_HEX);
        // 篡改密文最后一位
        char last = cipher.charAt(cipher.length() - 1);
        char tampered = last == '0' ? '1' : '0';
        String tamperedCipher = cipher.substring(0, cipher.length() - 1) + tampered;
        assertThatThrownBy(() -> AesGcmUtil.decrypt(tamperedCipher, KEY_256_HEX))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void decrypt_shouldFailOnWrongKey() {
        String cipher = AesGcmUtil.encrypt("secret", KEY_256_HEX);
        String wrongKey = "ff".repeat(32);
        assertThatThrownBy(() -> AesGcmUtil.decrypt(cipher, wrongKey))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void encryptKey_shouldDerive256BitKeyFromString() {
        String derived = AesGcmUtil.deriveKey(KEY_256);
        assertThat(derived).hasSize(64); // 32 bytes = 64 hex chars
    }
}
