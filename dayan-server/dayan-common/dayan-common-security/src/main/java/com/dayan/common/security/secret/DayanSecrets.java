package com.dayan.common.security.secret;

import com.dayan.common.core.crypto.AesGcmUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 平台敏感凭据单点收口：默认重置密码、AES-GCM 派生密钥口令。
 *
 * <p>历史上 {@code "dayan@123"} 与 {@code "dayan-default-key"} 散落在 6/5 个
 * ServiceImpl 中逐字重复，此处统一为配置项：
 * <ul>
 *   <li>{@code dayan.security.default-reset-password} —— 账号重置后的默认密码</li>
 *   <li>{@code dayan.aes.key} —— AES-GCM 密钥派生口令（沿用既有配置键）</li>
 * </ul>
 *
 * <p>生产强制：当激活 profile 含 {@code prod}，或显式设置
 * {@code dayan.security.require-configured-secrets=true} 时，上述两项必须显式配置，
 * 否则启动直接失败（fail-fast），杜绝生产环境静默回退到公开默认值。
 * 本地开发未配置时保留默认值并输出 WARN 日志，零成本。
 */
@Slf4j
@Component
public class DayanSecrets {

    /** 开发环境兜底值（仅本地使用，生产强制覆盖） */
    private static final String DEV_RESET_PASSWORD = "dayan@123";
    private static final String DEV_KEY_PASSWORD = "dayan-default-key";

    @Getter
    @Value("${dayan.security.default-reset-password:" + DEV_RESET_PASSWORD + "}")
    private String defaultResetPassword;

    @Value("${dayan.aes.key:}")
    private String aesKeyPassword;

    /** 显式开关：true 时无论 profile 均强制要求配置 */
    @Value("${dayan.security.require-configured-secrets:false}")
    private boolean requireConfiguredSecrets;

    private final Environment environment;

    /** AES 密钥 hex（派生后缓存） */
    private volatile String aesKeyHex;

    public DayanSecrets(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        boolean prod = requireConfiguredSecrets
                || Arrays.asList(environment.getActiveProfiles()).contains("prod");
        boolean aesMissing = aesKeyPassword == null || aesKeyPassword.isBlank();
        boolean resetIsDefault = DEV_RESET_PASSWORD.equals(defaultResetPassword);
        if (prod && (aesMissing || resetIsDefault)) {
            throw new IllegalStateException(
                    "[安全启动校验] 生产环境必须显式配置敏感凭据，禁止回退默认值："
                            + (aesMissing ? " dayan.aes.key(缺失)" : "")
                            + (resetIsDefault ? " dayan.security.default-reset-password(仍为默认值)" : ""));
        }
        if (aesMissing) {
            log.warn("[安全] 未配置 dayan.aes.key，使用开发默认派生密钥（生产环境将启动失败）");
        }
        if (resetIsDefault) {
            log.warn("[安全] 未配置 dayan.security.default-reset-password，使用开发默认重置密码（生产环境将启动失败）");
        }
    }

    /**
     * AES-GCM 密钥 hex：由配置的口令派生；未配置时回退开发默认口令派生。
     */
    public String aesKeyHex() {
        if (aesKeyHex == null) {
            synchronized (this) {
                if (aesKeyHex == null) {
                    String password = (aesKeyPassword == null || aesKeyPassword.isBlank())
                            ? DEV_KEY_PASSWORD : aesKeyPassword;
                    aesKeyHex = AesGcmUtil.deriveKey(password);
                }
            }
        }
        return aesKeyHex;
    }
}
