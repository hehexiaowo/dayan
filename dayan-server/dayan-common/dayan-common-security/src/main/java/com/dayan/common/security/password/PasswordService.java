package com.dayan.common.security.password;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码服务（BCrypt 加盐哈希）。
 *
 * <p>所有账号密码经 BCrypt（strength=10）哈希存储，不可逆。
 * 登录时用 {@link #matches} 校验明文与哈希。
 */
@Component
public class PasswordService {

    /** BCrypt 强度：10（约 100ms/次，兼顾安全与性能） */
    private static final int STRENGTH = 10;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(STRENGTH);

    /**
     * 对明文密码哈希。
     */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 校验明文与哈希是否匹配。
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }
}
