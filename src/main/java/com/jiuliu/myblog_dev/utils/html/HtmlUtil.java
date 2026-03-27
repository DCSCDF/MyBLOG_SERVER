/*
 * [HtmlUtil.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/23
 */

package com.jiuliu.myblog_dev.utils.html;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * HTML工具类
 * 提供Markdown转HTML、HTML净化（防XSS）等操作
 */
public final class HtmlUtil {

    private HtmlUtil() {
    }

    /**
     * 将Markdown转换为HTML并净化XSS
     * <p>
     * 使用 flexmark-java 将 Markdown 转为 HTML，
     * 再通过 jsoup 的 Safelist 白名单过滤，移除所有脚本和危险属性。
     *
     * @param markdown Markdown格式内容
     * @return 净化后的HTML内容，如果输入为null则返回空字符串
     */
    public static String markdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        // Markdown → HTML
        String html = markdownToHtmlRaw(markdown);

        // 净化 XSS：只保留白名单内的标签和属性
        return sanitize(html);
    }

    /**
     * 内部方法：调用 flexmark 将 Markdown 转换为 HTML 字符串
     * <p>
     * 已启用 GFM 扩展，支持以下额外功能：
     * <ul>
     *   <li>GFM 表格（| col1 | col2 |）</li>
     *   <li>删除线（~~text~~）</li>
     *   <li>自动链接</li>
     *   <li>任务列表（- [x] done）</li>
     * </ul>
     */
    private static String markdownToHtmlRaw(String markdown) {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        var document = parser.parse(markdown);
        return renderer.render(document);
    }

    /**
     * 使用 jsoup Safelist 白名单净化 HTML
     * <p>
     * 白名单策略说明：
     * <ul>
     *   <li>保留的标签：p, div, span, h1~h6, ul, ol, li, blockquote,
     *       pre, code, a, img, table, thead, tbody, tr, th, td,
     *       hr, br, strong, em, del, sup, sub, input（只读）</li>
     *   <li>a 标签：只允许 href 属性，且必须是 http/https/mailto 协议</li>
     *   <li>img 标签：只允许 src 属性，且必须是 http/https/data(base64) 协议</li>
     *   <li>input 标签：只保留 type/value/disabled 属性（禁用交互）</li>
     *   <li>所有事件属性（onclick、onerror 等）和 javascript: 协议全部移除</li>
     * </ul>
     *
     * @param html 原始HTML字符串
     * @return 净化后的安全HTML
     */
    public static String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        // 配置白名单
        Safelist safelist = Safelist.relaxed()
                // 表格相关标签（必须先添加标签，再添加属性）
                .addTags("table", "thead", "tbody", "tr", "th", "td")
                // a 标签只允许 href，且限制协议
                .addAttributes("a", "href", "title", "target")
                .addProtocols("a", "href", "http", "https", "mailto")
                // img 标签只允许 src，且限制协议（允许 data: 用于 base64 图片）
                .addAttributes("img", "src", "alt", "title", "width", "height")
                .addProtocols("img", "src", "http", "https", "data")
                // 表格相关属性
                .addAttributes("table", "class", "style")
                .addAttributes("thead", "class", "style")
                .addAttributes("tbody", "class", "style")
                .addAttributes("tr", "class", "style")
                .addAttributes("th", "class", "style", "align", "valign")
                .addAttributes("td", "class", "style", "align", "valign", "colspan", "rowspan")
                // 保留代码块的 class（highlight.js 等可能用到）
                .addAttributes(":all", "class")
                // input 只读属性
                .addAttributes("input", "type", "value", "disabled", "checked")
                // 移除所有可能执行脚本的属性
                .removeAttributes(":all", "onclick", "onload", "onerror",
                        "onmouseover", "onfocus", "onblur", "onchange", "onsubmit");

        Document.OutputSettings outputSettings = new Document.OutputSettings()
                .prettyPrint(false)   // 不格式化，保留原始结构
                .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
                .syntax(Document.OutputSettings.Syntax.html);

        return Jsoup.clean(html, "", safelist, outputSettings);
    }
}
