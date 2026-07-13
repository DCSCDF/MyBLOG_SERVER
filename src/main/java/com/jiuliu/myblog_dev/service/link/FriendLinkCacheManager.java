/*
 * [FriendLinkCacheManager.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/27
 */

package com.jiuliu.myblog_dev.service.link;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.dto.link.PageFriendLinkResponseDTO;
import com.jiuliu.myblog_dev.dto.link.PagePublicFriendLinkResponseDTO;
import com.jiuliu.myblog_dev.entity.link.SysFriendLink;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Getter
@Component
public class FriendLinkCacheManager {

    private static final Logger log = LoggerFactory.getLogger(FriendLinkCacheManager.class);

    private final Cache<Long, SysFriendLink> friendLinkCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private final Cache<String, PageFriendLinkResponseDTO> friendLinkListCache = CacheBuilder.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private final Cache<String, PagePublicFriendLinkResponseDTO> publicFriendLinkListCache = CacheBuilder.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    public void clearAllCache() {
        friendLinkCache.invalidateAll();
        friendLinkListCache.invalidateAll();
        publicFriendLinkListCache.invalidateAll();
        log.debug("友链缓存已清除（包括后台和前台）");
    }

    public void clearPublicFriendLinkCache() {
        publicFriendLinkListCache.invalidateAll();
        log.debug("公共外链列表缓存已清除");
    }
}