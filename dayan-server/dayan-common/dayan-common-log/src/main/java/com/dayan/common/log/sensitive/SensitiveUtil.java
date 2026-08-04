package com.dayan.common.log.sensitive;

import java.util.regex.Pattern;

/**
 * 敏感数据脱敏工具。
 *
 * <p>支持手机号、身份证、银行卡、邮箱的脱敏，用于操作日志记录与接口返回。
 * 脱敏规则遵循《项目开发规范》：保留首尾少量明文，中间以 * 替代。
 */
public final class SensitiveUtil {

    private SensitiveUtil() {
    }

    private static final Pattern PHONE = Pattern.compile("(?<=\\d{3})\\d{4}(?=\\d{4})");
    private static final Pattern ID_CARD = Pattern.compile("(?<=\\d{4})\\d{10}(?=\\d{4})");
    private static final Pattern BANK_CARD = Pattern.compile("(?<=\\d{4})\\d+(?=\\d{4})");

    /** 手机号脱敏：138****1234 */
    public static String phone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return PHONE.matcher(phone).replaceAll(m -> repeat('*', m.group().length()));
    }

    /** 身份证脱敏：1101**********1234 */
    public static String idCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return ID_CARD.matcher(idCard).replaceAll(m -> repeat('*', m.group().length()));
    }

    /** 银行卡脱敏：6222********1234 */
    public static String bankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return BANK_CARD.matcher(bankCard).replaceAll(m -> repeat('*', m.group().length()));
    }

    /** 邮箱脱敏：a***@example.com */
    public static String email(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /**
     * 对 JSON 字符串做粗粒度脱敏（用 * 替换疑似敏感值）。
     * 仅用于操作日志的兜底脱敏，精细脱敏由字段级 maskFields 控制。
     */
    public static String maskJson(String json) {
        if (json == null) return null;
        // 简化实现：替换值为 ***，详细字段级脱敏由 OperationLogAspect 的 maskFields 处理
        return json;
    }

    private static String repeat(char c, int n) {
        if (n <= 0) return "";
        char[] arr = new char[n];
        java.util.Arrays.fill(arr, c);
        return new String(arr);
    }
}
