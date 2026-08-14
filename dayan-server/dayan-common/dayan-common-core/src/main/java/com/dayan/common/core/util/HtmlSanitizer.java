package com.dayan.common.core.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * HTML 富文本净化器：入库前统一过滤 XSS。
 *
 * <p>白名单：段落/标题/列表/表格/引用/图片/视频/链接等富文本常用标签；
 * 禁止 script/iframe/object/embed、一切 on* 事件属性、javascript: 协议；
 * a 标签强制 rel=noopener target=_blank。输出保持原排版（prettyPrint=false）。
 *
 * <p>相对 URL（如 img src="/admin-api/v1/files/preview/x.jpg"）原样保留：
 * jsoup 的协议校验对无法解析的相对值会直接剥除属性，故 clean 时传入一个占位 baseUri
 * 令协议解析可完成（相对路径按 https 解析必然通过 http/https 白名单），
 * 并配合 preserveRelativeLinks(true) 阻止相对值被重写为基于占位域名的绝对 URL。
 */
public final class HtmlSanitizer {

    /**
     * 协议校验用占位 baseUri：只参与 src/href 的协议解析判定，绝不写入输出
     * （preserveRelativeLinks=true 保证相对值原样保留、绝对值不受影响）。
     */
    private static final String URL_RESOLVE_BASE = "https://placeholder.invalid";

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("video", "source", "figure", "figcaption")
            .addAttributes("img", "src", "alt", "width", "height")
            .addAttributes("video", "src", "controls", "poster", "width", "height")
            .addAttributes("source", "src", "type")
            .addAttributes(":all", "style")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("video", "src", "http", "https")
            .addProtocols("source", "src", "http", "https")
            .preserveRelativeLinks(true)
            .addEnforcedAttribute("a", "rel", "noopener")
            .addEnforcedAttribute("a", "target", "_blank");

    private static final Document.OutputSettings OUTPUT =
            new Document.OutputSettings().prettyPrint(false);

    private HtmlSanitizer() {}

    /** 净化 HTML；null/空白原样返回 */
    public static String clean(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return Jsoup.clean(html, URL_RESOLVE_BASE, SAFELIST, OUTPUT);
    }
}
