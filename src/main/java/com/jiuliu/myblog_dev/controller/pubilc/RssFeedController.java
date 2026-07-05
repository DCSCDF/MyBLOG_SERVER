/*
 * [RssFeedController.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/29
 */

package com.jiuliu.myblog_dev.controller.pubilc;

import com.jiuliu.myblog_dev.dto.rss.RssFeedResponseDTO;
import com.jiuliu.myblog_dev.service.rss.RssFeedService;
import com.jiuliu.myblog_dev.utils.rateLimit.RateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RSS Feed 公共接口 - 无需登录即可访问
 */
@RestController
@RequestMapping("/api/public/rss")
public class RssFeedController {

    private static final Logger log = LoggerFactory.getLogger(RssFeedController.class);

    private final RssFeedService rssFeedService;

    public RssFeedController(RssFeedService rssFeedService) {
        this.rssFeedService = rssFeedService;
    }

    /**
     * 获取 RSS Feed (Atom 格式)
     * GET /api/public/rss?username=xxx
     * 无需登录，所有用户均可访问
     * 返回最新10篇文章的Atom格式RSS Feed
     * 
     * @param username 可选参数，指定用户名，只检索该用户的文章
     */
    @RateLimit(count = 500, period = 1, prefix = "public_rss_feed", ipBased = false)
    @GetMapping(produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    public ResponseEntity<String> getRssFeed(@RequestParam(required = false) String username) {
        try {
            RssFeedResponseDTO response = rssFeedService.generateRssFeed(username);
            log.debug("RSS Feed 请求成功，username={}，共 {} 篇文章", username, response.getArticleCount());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/atom+xml; charset=UTF-8"))
                    .header("Cache-Control", "public, max-age=1800")
                    .body(response.getFeedXml());
        } catch (Exception e) {
            log.error("RSS Feed 生成失败", e);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<error>RSS Feed 生成失败</error>");
        }
    }
}
