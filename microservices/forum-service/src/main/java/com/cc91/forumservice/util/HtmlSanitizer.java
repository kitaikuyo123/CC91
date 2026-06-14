package com.cc91.forumservice.util;

import org.springframework.web.util.HtmlUtils;

/**
 * HTML 净化工具类，防止存储型 XSS 攻击
 */
public class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    /**
     * 转义 HTML 特殊字符，防止存储型 XSS
     */
    public static String escape(String input) {
        if (input == null) return null;
        return HtmlUtils.htmlEscape(input, "UTF-8");
    }

    /**
     * 对内容进行净化 — 完全转义 HTML 特殊字符，阻止所有 XSS 向量
     */
    public static String sanitizeContent(String content) {
        if (content == null) return null;
        return HtmlUtils.htmlEscape(content, "UTF-8");
    }
}
