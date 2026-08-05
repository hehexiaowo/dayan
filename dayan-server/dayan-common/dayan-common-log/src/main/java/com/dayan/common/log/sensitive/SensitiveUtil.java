package com.dayan.common.log.sensitive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
     * 对 JSON 字符串做字段级深度脱敏：递归遍历 JSON 树，将名字命中 {@code maskFields}
     * 的字段的值替换为 {@code "***"}（不区分大小写）。
     *
     * <p>用于操作日志的入参/出参脱敏。匹配的是 JSON 字段名（即嵌套对象内的属性名），
     * 因此能正确处理 DTO 包裹下的敏感字段（如 {@code {"dto":{"password":"xxx"}}}）。
     *
     * @param json       待脱敏的 JSON 字符串，可为 null
     * @param maskFields 需要脱敏的字段名集合，逗号分隔（如 {@code "password,idCard"}）；
     *                   为 null/空则原样返回
     * @return 脱敏后的 JSON 字符串；解析失败时原样返回（兜底，不抛异常）
     */
    public static String maskJson(String json, String maskFields) {
        if (json == null || json.isEmpty() || maskFields == null || maskFields.isEmpty()) {
            return json;
        }
        Set<String> targets = new HashSet<>();
        for (String f : maskFields.split(",")) {
            String t = f.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) {
                targets.add(t);
            }
        }
        if (targets.isEmpty()) {
            return json;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode masked = maskNode(root, targets);
            return MAPPER.writeValueAsString(masked);
        } catch (Exception e) {
            // 解析失败时原样返回，避免脱敏逻辑影响主流程
            return json;
        }
    }

    /**
     * 递归脱敏：对 Object 节点的字段做名字匹配并替换值；对数组节点递归每个元素。
     */
    private static JsonNode maskNode(JsonNode node, Set<String> targets) {
        if (node == null || node.isNull() || node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                JsonNode child = node.get(i);
                if (child.isContainerNode()) {
                    ((com.fasterxml.jackson.databind.node.ArrayNode) node).set(i, maskNode(child, targets));
                }
            }
            return node;
        }
        // Object
        ObjectNode obj = (ObjectNode) node;
        java.util.Iterator<String> fieldNames = obj.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode child = obj.get(fieldName);
            if (targets.contains(fieldName.toLowerCase(Locale.ROOT))) {
                obj.set(fieldName, TextNode.valueOf("***"));
            } else if (child.isContainerNode()) {
                obj.set(fieldName, maskNode(child, targets));
            }
        }
        return obj;
    }

    private static String repeat(char c, int n) {
        if (n <= 0) return "";
        char[] arr = new char[n];
        java.util.Arrays.fill(arr, c);
        return new String(arr);
    }
}
