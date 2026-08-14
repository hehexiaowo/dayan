package com.dayan.common.core.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * HTML 富文本净化器：入库前统一过滤 XSS。
 *
 * <p>白名单：段落/标题/列表/表格/引用/图片/视频/链接等富文本常用标签；
 * 禁止 script/iframe/object/embed、一切 on* 事件属性、javascript: 协议；
 * a 标签强制 rel=noopener。输出保持原排版（prettyPrint=false）。
 */
public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("video", "source", "figure", "figcaption")
            .addAttributes("img", "src", "alt", "width", "height")
            .addAttributes("video", "src", "controls", "poster", "width", "height")
            .addAttributes("source", "src", "type")
            .addAttributes(":all", "style")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("video", "src", "http", "https")
            .addProtocols("source", "src", "http", "https")
            .addEnforcedAttribute("a", "rel", "noopener");

    private static final Document.OutputSettings OUTPUT =
            new Document.OutputSettings().prettyPrint(false);

    private HtmlSanitizer() {}

    /** 净化 HTML；null/空白原样返回 */
    public static String clean(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return Jsoup.clean(html, "", SAFELIST, OUTPUT);
    }
}
