/*
 * [PublicFriendLinkServiceImpl.java]
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

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.dto.link.PagePublicFriendLinkDTO;
import com.jiuliu.myblog_dev.dto.link.PagePublicFriendLinkResponseDTO;
import com.jiuliu.myblog_dev.dto.link.PublicFriendLinkCreateDTO;
import com.jiuliu.myblog_dev.dto.link.PublicFriendLinkResponseDTO;
import com.jiuliu.myblog_dev.entity.link.SysFriendLink;
import com.jiuliu.myblog_dev.mapper.link.SysFriendLinkMapper;
import com.jiuliu.myblog_dev.utils.cache.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 公共外链服务实现类（无需登录）
 */
@Service
public class PublicFriendLinkServiceImpl implements PublicFriendLinkService {

    private static final Logger log = LoggerFactory.getLogger(PublicFriendLinkServiceImpl.class);

    /**
     * 公共外链列表缓存 - 只缓存已通过审核的外链列表（前台展示用）
     * 缓存时间：30分钟
     */
    private final Cache<String, PagePublicFriendLinkResponseDTO> publicFriendLinkListCache = CacheBuilder.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private final SysFriendLinkMapper friendLinkMapper;

    public PublicFriendLinkServiceImpl(SysFriendLinkMapper friendLinkMapper) {
        this.friendLinkMapper = friendLinkMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult createFriendLink(PublicFriendLinkCreateDTO dto) {
        try {
            // 1. 验证 URL 地址格式
            if (!StringUtils.hasText(dto.getUrl()) || isUrlInvalid(dto.getUrl())) {
                log.warn("外链提交失败：URL地址格式无效，url={}", dto.getUrl());
                return SaResult.error("URL地址格式无效，请输入有效的网址").setCode(400);
            }

            // 2. 验证站点图片 URL 格式（如果提供了）
            if (StringUtils.hasText(dto.getImageUrl()) && isUrlInvalid(dto.getImageUrl())) {
                log.warn("外链提交失败：站点图片URL格式无效，imageUrl={}", dto.getImageUrl());
                return SaResult.error("站点图片URL格式无效，请输入有效的网址").setCode(400);
            }

            // 3. 构建外链实体
            SysFriendLink link = new SysFriendLink();
            link.setName(dto.getName());
            link.setUrl(dto.getUrl());
            link.setSummary(dto.getSummary());
            link.setImageUrl(dto.getImageUrl());
            link.setSortOrder(0); // 默认排序为0
            link.setStatus(0);    // 待审核
            link.setIsDeleted(0);

            // 4. 保存外链
            friendLinkMapper.insert(link);
            log.info("外链提交成功，id={}, name={}, url={}", link.getId(), link.getName(), link.getUrl());

            // 5. 清除公共外链列表缓存
            clearPublicFriendLinkCache();

            // 6. 返回结果
            return SaResult.ok("外链提交成功，待审核后显示");
        } catch (Exception e) {
            log.error("外链提交异常，name={}", dto.getName(), e);
            return SaResult.error("外链提交失败").setCode(500);
        }
    }

    @Override
    public SaResult getPagePublicFriendLinks(PagePublicFriendLinkDTO dto) {
        try {
            // 1. 构建缓存键
            String cacheKey = CacheUtil.CACHE_KEY_FRIEND_LINK_LIST + "public:" + dto.getCurrentPage() + "-" + dto.getPageSize();

            // 2. 尝试从缓存获取
            PagePublicFriendLinkResponseDTO cached = publicFriendLinkListCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("从缓存获取公共外链列表，key={}", cacheKey);
                return SaResult.data(cached);
            }

            // 3. 构建查询条件：只查询已通过审核且未删除的外链
            LambdaQueryWrapper<SysFriendLink> wrapper = new LambdaQueryWrapper<SysFriendLink>()
                    .eq(SysFriendLink::getIsDeleted, 0)
                    .eq(SysFriendLink::getStatus, 1)
                    .orderByDesc(SysFriendLink::getSortOrder)  // 按 sort_order 降序排序
                    .orderByDesc(SysFriendLink::getCreateTime); // sort_order 相同时按创建时间降序

            // 4. 分页查询
            Page<SysFriendLink> page = new Page<>(dto.getCurrentPage(), dto.getPageSize());
            Page<SysFriendLink> pageResult = friendLinkMapper.selectPage(page, wrapper);

            // 5. 转换为响应DTO
            List<PublicFriendLinkResponseDTO> records = pageResult.getRecords().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());

            // 6. 构建响应
            PagePublicFriendLinkResponseDTO response = new PagePublicFriendLinkResponseDTO();
            response.setRecords(records);
            response.setTotal(pageResult.getTotal());
            response.setSize(pageResult.getSize());
            response.setCurrent(pageResult.getCurrent());
            response.setPages(pageResult.getPages());

            // 7. 存入缓存
            publicFriendLinkListCache.put(cacheKey, response);
            log.debug("公共外链列表已缓存，key={}", cacheKey);

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取公共外链列表异常", e);
            return SaResult.error("获取外链列表失败").setCode(500);
        }
    }

    /**
     * 清除公共外链列表缓存
     */
    public void clearPublicFriendLinkCache() {
        publicFriendLinkListCache.invalidateAll();
        log.debug("公共外链列表缓存已清除");
    }

    /**
     * 验证 URL 格式是否有效
     * 支持 http:// 和 https:// 协议
     */
    private boolean isUrlInvalid(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true;
        }
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String protocol = parsedUrl.getProtocol();
            return !"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 将 SysFriendLink 转换为 PublicFriendLinkResponseDTO
     */
    private PublicFriendLinkResponseDTO toResponseDTO(SysFriendLink link) {
        PublicFriendLinkResponseDTO dto = new PublicFriendLinkResponseDTO();
        dto.setName(link.getName());
        dto.setUrl(link.getUrl());
        dto.setSummary(link.getSummary());
        dto.setImageUrl(link.getImageUrl());
        dto.setCreateTime(link.getCreateTime());
        return dto;
    }
}
