package com.dayan.common.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    @Test
    void 空白原样返回() {
        assertThat(HtmlSanitizer.clean(null)).isNull();
        assertThat(HtmlSanitizer.clean("  ")).isEqualTo("  ");
    }

    @Test
    void 剥离script标签() {
        String out = HtmlSanitizer.clean("<p>ok</p><script>alert(1)</script>");
        assertThat(out).contains("<p>ok</p>").doesNotContain("script").doesNotContain("alert");
    }

    @Test
    void 剥离iframe标签() {
        String out = HtmlSanitizer.clean("<iframe src=\"https://evil.com\"></iframe><p>ok</p>");
        assertThat(out).doesNotContain("iframe").contains("<p>ok</p>");
    }

    @Test
    void 剥离事件属性() {
        String out = HtmlSanitizer.clean("<img src=\"https://a.com/b.jpg\" onerror=\"alert(1)\">");
        assertThat(out).contains("https://a.com/b.jpg").doesNotContain("onerror");
    }

    @Test
    void 剥离javascript协议() {
        String out = HtmlSanitizer.clean("<a href=\"javascript:alert(1)\">x</a>");
        assertThat(out).doesNotContain("javascript:");
    }

    @Test
    void 保留video与图片() {
        String out = HtmlSanitizer.clean(
                "<video src=\"https://a.com/v.mp4\" controls poster=\"https://a.com/p.jpg\"></video>"
                        + "<img src=\"https://a.com/b.jpg\" alt=\"图\">");
        assertThat(out).contains("<video").contains("v.mp4").contains("b.jpg").contains("alt=\"图\"");
    }

    @Test
    void 保留常用排版标签与style() {
        String out = HtmlSanitizer.clean("<p style=\"text-align:center\">a<b>b</b></p><ul><li>c</li></ul>");
        assertThat(out).contains("text-align:center").contains("<b>b</b>").contains("<li>c</li>");
    }

    @Test
    void 链接强制noopener() {
        String out = HtmlSanitizer.clean("<a href=\"https://a.com\">x</a>");
        assertThat(out).contains("rel=\"noopener\"");
    }
}
