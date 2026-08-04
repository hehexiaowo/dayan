package com.dayan.common.core.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 加解密工具。
 *
 * <p>规范要求（《项目开发规范》v1.1）：敏感数据使用 AES-256-GCM（随机 IV + Tag 认证加密），
 * 替代不安全的 ECB 模式。
 *
 * <p>密文格式（Base64）：{@code IV(12 byte) || ciphertext || GCM-Tag(16 byte)}。
 * 每次加密 IV 随机生成，相同明文产生不同密文；解密时 Tag 校验失败即抛异常，保证完整性。
 */
public final class AesGcmUtil {

    /** GCM 推荐 IV 长度：12 字节 */
    private static final int IV_LENGTH = 12;
    /** GCM 认证 Tag 长度：128 bit = 16 字节 */
    private static final int TAG_LENGTH_BIT = 128;

    private AesGcmUtil() {
    }

    /**
     * 加密。
     *
     * @param plaintext 明文（UTF-8）
     * @param keyHex    256-bit 密钥的十六进制字符串（64 字符）。可用 {@link #deriveKey(String)} 生成。
     * @return Base64(IV || ciphertext+tag)
     */
    public static String encrypt(String plaintext, String keyHex) {
        try {
            byte[] keyBytes = hexToBytes(keyHex);
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM 加密失败", e);
        }
    }

    /**
     * 解密。
     *
     * @param cipherBase64 {@link #encrypt} 的输出
     * @param keyHex       与加密相同的 256-bit 密钥 hex
     * @return 明文（UTF-8）
     */
    public static String decrypt(String cipherBase64, String keyHex) {
        try {
            byte[] combined = Base64.getDecoder().decode(cipherBase64);
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);

            byte[] keyBytes = hexToBytes(keyHex);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM 解密失败（密钥错误或密文被篡改）", e);
        }
    }

    /**
     * 由任意长度密码派生 256-bit 密钥（hex，64 字符）。
     * 使用 SHA-256。适用于将配置中的可读密钥转为 AES 密钥。
     */
    public static String deriveKey(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("派生密钥失败", e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
