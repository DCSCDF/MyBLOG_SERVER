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
import com.jiuliu.myblog_dev.dto.link.PagePublicFriendLinkDTO;
import com.jiuliu.myblog_dev.dto.link.PagePublicFriendLinkResponseDTO;
import com.jiuliu.myblog_dev.dto.link.PublicFriendLinkCreateDTO;
import com.jiuliu.myblog_dev.dto.link.PublicFriendLinkResponseDTO;
import com.jiuliu.myblog_dev.entity.link.SysFriendLink;
import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import com.jiuliu.myblog_dev.mapper.link.SysFriendLinkMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionMapper;
import com.jiuliu.myblog_dev.service.mail.MailService;
import com.jiuliu.myblog_dev.utils.cache.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公共外链服务实现类（无需登录）
 */
@Service
public class PublicFriendLinkServiceImpl implements PublicFriendLinkService {

    private static final Logger log = LoggerFactory.getLogger(PublicFriendLinkServiceImpl.class);

    private static final String KEY_SITE_DOMAIN = "site.domain";
    private static final String PERMISSION_LINK_LIST = "links:list";

    private final SysFriendLinkMapper friendLinkMapper;
    private final FriendLinkCacheManager cacheManager;
    private final SysConfigMapper sysConfigMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final MailService mailService;

    public PublicFriendLinkServiceImpl(SysFriendLinkMapper friendLinkMapper,
                                       FriendLinkCacheManager cacheManager,
                                       SysConfigMapper sysConfigMapper,
                                       SysRolePermissionMapper rolePermissionMapper,
                                       MailService mailService) {
        this.friendLinkMapper = friendLinkMapper;
        this.cacheManager = cacheManager;
        this.sysConfigMapper = sysConfigMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.mailService = mailService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult createFriendLink(PublicFriendLinkCreateDTO dto) {
        try {
            SysFriendLink link = new SysFriendLink();
            link.setName(dto.getName());
            link.setUrl(dto.getUrl());
            link.setSummary(dto.getSummary());
            link.setImageUrl(dto.getImageUrl());
            link.setSortOrder(0);
            link.setStatus(0);
            link.setIsDeleted(0);

            friendLinkMapper.insert(link);
            log.info("外链提交成功，id={}, name={}, url={}", link.getId(), link.getName(), link.getUrl());

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cacheManager.clearAllCache();
                    log.debug("事务提交后清除友链缓存");
                    sendNewFriendLinkNotificationToAdmins(dto);
                }
            });

            return SaResult.ok("外链提交成功，待审核后显示");
        } catch (Exception e) {
            log.error("外链提交异常，name={}", dto.getName(), e);
            return SaResult.error("外链提交失败").setCode(500);
        }
    }

    private void sendNewFriendLinkNotificationToAdmins(PublicFriendLinkCreateDTO dto) {
        try {
            if (!mailService.isCommentNotificationEnabled()) {
                log.debug("新友链通知跳过：邮件通知功能未启用");
                return;
            }

            List<String> adminEmails = rolePermissionMapper.selectUserEmailsByPermissionCode(PERMISSION_LINK_LIST);
            if (adminEmails == null || adminEmails.isEmpty()) {
                log.debug("新友链通知跳过：没有友链管理权限的管理员邮箱");
                return;
            }

            List<String> validEmails = new ArrayList<>();
            for (String email : adminEmails) {
                if (StringUtils.hasText(email)) {
                    validEmails.add(email);
                }
            }

            if (validEmails.isEmpty()) {
                return;
            }

            String siteDomain = sysConfigMapper.selectValueByKey(KEY_SITE_DOMAIN);

            SaResult result = mailService.sendNewFriendLinkNotificationToAdmins(
                    validEmails, siteDomain,
                    dto.getName(), dto.getUrl(),
                    dto.getSummary(), dto.getImageUrl());

            if (result.getCode() == 200) {
                log.info("新友链通知发送成功，linkName={}, adminCount={}", dto.getName(), validEmails.size());
            } else {
                log.warn("新友链通知发送失败，linkName={}, error={}", dto.getName(), result.getMsg());
            }
        } catch (Exception e) {
            log.error("新友链通知发送异常，linkName={}", dto.getName(), e);
        }
    }

    @Override
    public SaResult getPagePublicFriendLinks(PagePublicFriendLinkDTO dto) {
        try {
            // 1. 构建缓存键
            String cacheKey = CacheUtil.CACHE_KEY_FRIEND_LINK_LIST + "public:" + dto.getCurrentPage() + "-" + dto.getPageSize();

            PagePublicFriendLinkResponseDTO cached = cacheManager.getPublicFriendLinkListCache().getIfPresent(cacheKey);
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

            cacheManager.getPublicFriendLinkListCache().put(cacheKey, response);
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
    @Override
    public void clearPublicFriendLinkCache() {
        cacheManager.clearPublicFriendLinkCache();
        log.debug("公共外链列表缓存已清除");
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
