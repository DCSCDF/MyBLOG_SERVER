/*
 * [ChineseSegmentUtil.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/26
 */

package com.jiuliu.myblog_dev.utils.segment;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文分词工具类
 * 使用 IKAnalyzer 2012_u6 进行中文分词
 */
public class ChineseSegmentUtil {

    private static final Logger log = LoggerFactory.getLogger(ChineseSegmentUtil.class);

    private ChineseSegmentUtil() {
    }

    /**
     * 分词模式枚举
     */
    public enum SegmentMode {
        /**
         * 细粒度分词：最细粒度拆分，如"登录" -> "登", "录"
         */
        SMART(false),
        /**
         * 智能分词：按语义分词，如"登录" -> "登录"
         */
        MAX_WORD(true);

        private final boolean useSmart;

        SegmentMode(boolean useSmart) {
            this.useSmart = useSmart;
        }
    }

    /**
     * 对文本进行分词，返回分词列表
     *
     * @param text        待分词文本
     * @param segmentMode 分词模式
     * @return 分词列表
     */
    public static List<String> segment(String text, SegmentMode segmentMode) {
//        log.info("【分词输入】text={}, segmentMode={}", text, segmentMode);

        if (text == null || text.trim().isEmpty()) {
            log.warn("【分词输入】输入文本为空或null");
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();

        try (Analyzer analyzer = new IKAnalyzer(segmentMode.useSmart);
             StringReader reader = new StringReader(text);
             TokenStream tokenStream = analyzer.tokenStream(text, reader)) {

            CharTermAttribute termAtt = tokenStream.getAttribute(CharTermAttribute.class);
            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                String token = termAtt.toString();
                if (token != null && !token.isEmpty()) {
                    result.add(token);
                }
            }
        } catch (Exception e) {
            log.error("【分词异常】text={}, error={}", text, e.getMessage());
            return Collections.emptyList();
        }

//        log.info("【分词输出】text={}, result={}", text, result);
        return result;
    }


    /**
     * 对搜索关键词进行分词
     *
     * @param keyword 搜索关键词
     * @return 分词列表（只包含有效词，过滤掉纯数字、纯符号等）
     */
    public static List<String> segmentKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = segment(keyword, SegmentMode.MAX_WORD);

        tokens = tokens.stream()
                .filter(t -> t.length() >= 2 || t.matches("[\\u4e00-\\u9fa5]"))
                .filter(t -> {
                    boolean hasChinese = t.matches(".*[\\u4e00-\\u9fa5].*");
                    boolean hasEnglish = t.matches(".*[a-zA-Z].*");
                    return hasChinese || hasEnglish;
                })
                .collect(Collectors.toList());

        return tokens;
    }


}
