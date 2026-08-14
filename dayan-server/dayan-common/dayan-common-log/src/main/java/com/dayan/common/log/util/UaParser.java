package com.dayan.common.log.util;

/**
 * User-Agent 简单解析工具。
 *
 * <p>仅覆盖主流 UA，边缘 UA 解析失败时字段留 null（不影响主流程）。
 * 供 {@code OperationLogAspect} 与 {@code AuthLogRecorder} 实现共用。
 */
public final class UaParser {

    private UaParser() {
    }

    /** 设备类型：pc / mobile / tablet */
    public static String deviceType(String ua) {
        if (ua == null || ua.isEmpty()) {
            return null;
        }
        String uaLower = ua.toLowerCase();
        if (uaLower.contains("mobile") || uaLower.contains("android") || uaLower.contains("iphone")) {
            if (uaLower.contains("tablet") || uaLower.contains("ipad")) {
                return "tablet";
            }
            return "mobile";
        }
        return "pc";
    }

    /** 操作系统：Windows / macOS / Linux / Android / iOS */
    public static String os(String ua) {
        if (ua == null || ua.isEmpty()) {
            return null;
        }
        String uaLower = ua.toLowerCase();
        if (uaLower.contains("windows")) {
            return "Windows";
        }
        if (uaLower.contains("mac os") || uaLower.contains("macintosh")) {
            return "macOS";
        }
        if (uaLower.contains("linux")) {
            return "Linux";
        }
        if (uaLower.contains("android")) {
            return "Android";
        }
        if (uaLower.contains("iphone") || uaLower.contains("ipad") || uaLower.contains("ios")) {
            return "iOS";
        }
        return null;
    }

    /** 浏览器：Edge / Firefox / Chrome / Safari（Edge 要先于 Chrome 判断，否则会被 Chrome 吞掉） */
    public static String browser(String ua) {
        if (ua == null || ua.isEmpty()) {
            return null;
        }
        String uaLower = ua.toLowerCase();
        if (uaLower.contains("edg/") || uaLower.contains("edge")) {
            return "Edge";
        }
        if (uaLower.contains("firefox")) {
            return "Firefox";
        }
        if (uaLower.contains("chrome")) {
            return "Chrome";
        }
        if (uaLower.contains("safari")) {
            return "Safari";
        }
        return null;
    }
}
