package com.jiuliu.myblog_dev.utils;

import com.jiuliu.myblog_dev.utils.html.HtmlUtil;
import org.junit.jupiter.api.Test;

/**
 * Markdown表格渲染测试
 */
public class MarkdownTableTest {

    @Test
    public void testTableRendering() {
        String markdown = """
| 类型 |描述 |举例 |
| - | - | - |
|number |任意数字 |1 , -33 , 2.5 |
| string| 任意字符串| 'hello' , 'ok' , '你好' |
|boolean | 布尔值|true 或 false |
""";

        System.out.println("=== 原始 Markdown ===");
        System.out.println(markdown);

        String html = HtmlUtil.markdownToHtml(markdown);

        System.out.println("\n=== 转换后的 HTML ===");
        System.out.println(html);

        // 检查是否包含表格标签
        boolean hasTable = html.contains("<table");
        boolean hasTr = html.contains("<tr");
        boolean hasTd = html.contains("<td");

        System.out.println("\n=== 检查结果 ===");
        System.out.println("包含 <table>: " + hasTable);
        System.out.println("包含 <tr>: " + hasTr);
        System.out.println("包含 <td>: " + hasTd);

        if (!hasTable || !hasTr || !hasTd) {
            System.out.println("\n❌ 表格未正确渲染!");
        } else {
            System.out.println("\n✅ 表格渲染正常!");
        }
    }

    @Test
    public void testSimpleTable() {
        String markdown = "| 列1 | 列2 |\n| --- | --- |\n| A | B |";

        System.out.println("=== 简单表格测试 ===");
        System.out.println("原始: " + markdown);

        String html = HtmlUtil.markdownToHtml(markdown);
        System.out.println("HTML: " + html);
    }
}
