/*
 * [MarkdownUtil.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/20
 */

package com.jiuliu.myblog_dev.utils.markdown;

import org.springframework.lang.NonNull;

import java.util.regex.Pattern;

/**
 * Markdown工具类
 * 提供Markdown格式标签去除等常用操作
 */
public final class MarkdownUtil {

    private MarkdownUtil() {
    }

    /**
     * 去除MD格式标签，获取纯文本内容
     *
     * @param mdContent Markdown格式的内容
     * @return 去除标签后的纯文本
     */

    @NonNull
    public static String stripMdTags(String mdContent) {
        if (mdContent == null || mdContent.isEmpty()) {
            return "";
        }
        String text = mdContent;

        // 去除代码块（行内代码和代码块）
        text = text.replaceAll("```[\\s\\S]*?```", "");
        text = text.replaceAll("`[^`]+`", "");

        // 去除标题标记（# ## ### 等）
        text = Pattern.compile("^#+ ", Pattern.MULTILINE).matcher(text).replaceAll("");

        // 去除加粗、斜体等标记
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        text = text.replaceAll("\\*(.+?)\\*", "$1");
        text = text.replaceAll("__(.+?)__", "$1");
        text = text.replaceAll("_(.+?)_", "$1");

        // 去除链接 [text](url)
        text = text.replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1");

        // 去除图片 ![alt](url)
        text = text.replaceAll("!\\[([^]]*)]\\([^)]+\\)", "");

        // 去除引用标记 >
        text = Pattern.compile("^> ", Pattern.MULTILINE).matcher(text).replaceAll("");

        // 去除列表标记（- * 1. 2. 等）
        text = Pattern.compile("^[-*+] ", Pattern.MULTILINE).matcher(text).replaceAll("");
        text = Pattern.compile("^\\d+\\. ", Pattern.MULTILINE).matcher(text).replaceAll("");

        // 去除表格分隔行（如 |---|---|）
        text = Pattern.compile("^\\|[:\\-]+\\|.*$", Pattern.MULTILINE).matcher(text).replaceAll("");

        // 去除表格单元格管道符（| cell | cell | -> cell）
        text = text.replaceAll("\\|\\s*", " ").replaceAll("\\s*\\|", "");

        // 去除水平线标记
        text = Pattern.compile("^[-*_]{3,}$", Pattern.MULTILINE).matcher(text).replaceAll("");

        // 去除表格单元格管道符（| cell | -> cell）
        text = text.replaceAll("\\|\\s*", " ");

        // 去除HTML标签
        text = text.replaceAll("<[^>]+>", "");

        // 替换HTML实体
        text = text.replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'");

        // 合并多余空白
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }
}
