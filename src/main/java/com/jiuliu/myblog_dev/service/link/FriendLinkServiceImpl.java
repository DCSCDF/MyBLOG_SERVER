/*
 * [FriendLinkServiceImpl.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/8
 */

package com.jiuliu.myblog_dev.service.link;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import com.jiuliu.myblog_dev.dto.link.*;
import com.jiuliu.myblog_dev.entity.link.SysFriendLink;
import com.jiuliu.myblog_dev.mapper.link.SysFriendLinkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class FriendLinkServiceImpl implements FriendLinkService {

    private static final Logger log = LoggerFactory.getLogger(FriendLinkServiceImpl.class);

    /**
     * 友链缓存 - 缓存单个友链，key为友链ID，value为SysFriendLink对象
     * 缓存时间：30分钟
     */
    private final Cache<Long, SysFriendLink> friendLinkCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    /**
     * 友链列表缓存 - 缓存通过审核的友链列表（前台展示用）
     * 缓存时间：30分钟
     */
    private final Cache<String, List<SysFriendLink>> friendLinkListCache = CacheBuilder.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private final SysFriendLinkMapper friendLinkMapper;

    public FriendLinkServiceImpl(SysFriendLinkMapper friendLinkMapper) {
        this.friendLinkMapper = friendLinkMapper;
    }

    @Override
    public SaResult getPageFriendLinks(PageFriendLinkDTO pageDto) {
        try {
            LambdaQueryWrapper<SysFriendLink> wrapper = new LambdaQueryWrapper<SysFriendLink>()
                    .eq(SysFriendLink::getIsDeleted, 0)
                    .orderByDesc(SysFriendLink::getSortOrder)
                    .orderByDesc(SysFriendLink::getCreateTime);

            if (pageDto.getStatus() != null) {
                wrapper.eq(SysFriendLink::getStatus, pageDto.getStatus());
            }

            if (StringUtils.hasText(pageDto.getKeyword())) {
                String kw = pageDto.getKeyword().trim();
                wrapper.and(w -> w.like(SysFriendLink::getName, kw)
                        .or().like(SysFriendLink::getUrl, kw)
                        .or().like(SysFriendLink::getSummary, kw)
                        .or().like(SysFriendLink::getRemark, kw));
            }

            Page<SysFriendLink> page = new Page<>(pageDto.getCurrentPage(), pageDto.getPageSize());
            Page<SysFriendLink> pageResult = friendLinkMapper.selectPage(page, wrapper);

            List<FriendLinkResponseDTO> records = pageResult.getRecords().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());

            PageFriendLinkResponseDTO response = new PageFriendLinkResponseDTO();
            response.setRecords(records);
            response.setTotal(pageResult.getTotal());
            response.setSize(pageResult.getSize());
            response.setCurrent(pageResult.getCurrent());
            response.setPages(pageResult.getPages());
            response.setFilterOptions(buildStatusFilterOptions());

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取外链列表异常", e);
            return SaResult.error("获取外链列表失败").setCode(500);
        }
    }

    private Map<String, List<FilterOptionItem>> buildStatusFilterOptions() {
        List<FilterOptionItem> statusOptions = List.of(
                new FilterOptionItem(0, "待审核"),
                new FilterOptionItem(1, "已通过"),
                new FilterOptionItem(2, "已拒绝")
//                new FilterOptionItem(3, "已删除")
        );
        return Map.of("status", statusOptions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult createFriendLink(FriendLinkCreateDTO dto) {
        SysFriendLink link = new SysFriendLink();
        link.setName(dto.getName());
        link.setUrl(dto.getUrl());
        link.setSummary(dto.getSummary());
        link.setRemark(dto.getRemark());
        link.setImageUrl(dto.getImageUrl());
        link.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        // 默认审核通过
        link.setStatus(1);
        link.setIsDeleted(0);

        friendLinkMapper.insert(link);
        log.info("外链创建成功，id={}, name={}", link.getId(), link.getName());
        // 清除友链缓存
        clearFriendLinkCache();
        return SaResult.data(toResponseDTO(friendLinkMapper.selectById(link.getId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult updateFriendLink(FriendLinkUpdateDTO dto) {
        SysFriendLink existing = friendLinkMapper.selectById(dto.getId());
        if (existing == null || existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
            log.warn("更新外链失败：记录不存在或已删除，id={}", dto.getId());
            return SaResult.error("外链不存在").setCode(404);
        }

        LambdaUpdateWrapper<SysFriendLink> updateWrapper = new LambdaUpdateWrapper<SysFriendLink>()
                .eq(SysFriendLink::getId, dto.getId())
                .set(dto.getName() != null, SysFriendLink::getName, dto.getName())
                .set(dto.getUrl() != null, SysFriendLink::getUrl, dto.getUrl())
                .set(dto.getSummary() != null, SysFriendLink::getSummary, dto.getSummary())
                .set(dto.getRemark() != null, SysFriendLink::getRemark, dto.getRemark())
                .set(dto.getImageUrl() != null, SysFriendLink::getImageUrl, dto.getImageUrl())
                .set(dto.getSortOrder() != null, SysFriendLink::getSortOrder, dto.getSortOrder())
                .set(SysFriendLink::getUpdateTime, LocalDateTime.now());

        friendLinkMapper.update(null, updateWrapper);
        log.info("外链更新成功，id={}", dto.getId());
        // 清除友链缓存
        clearFriendLinkCache();
        SysFriendLink updated = friendLinkMapper.selectById(dto.getId());
        return SaResult.data(toResponseDTO(updated));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult updateFriendLinkStatus(Long id, FriendLinkStatusUpdateDTO dto) {
        SysFriendLink existing = friendLinkMapper.selectById(id);
        if (existing == null) {
            log.warn("变更友链审核状态失败：记录不存在，id={}", id);
            return SaResult.error("友链不存在").setCode(404);
        }
        if (existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
            log.warn("变更友链审核状态失败：记录已删除，id={}", id);
            return SaResult.error("友链已删除").setCode(404);
        }
        Integer newStatus = dto.getStatus();
        friendLinkMapper.update(null, new LambdaUpdateWrapper<SysFriendLink>()
                .eq(SysFriendLink::getId, id)
                .set(SysFriendLink::getStatus, newStatus)
                .set(SysFriendLink::getUpdateTime, LocalDateTime.now()));
        log.info("友链审核状态变更成功，id={}, 新状态={}", id, newStatus);
        // 清除友链缓存
        clearFriendLinkCache();
        SysFriendLink updated = friendLinkMapper.selectById(id);
        return SaResult.data(toResponseDTO(updated));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult deleteFriendLink(Long id) {
        SysFriendLink existing = friendLinkMapper.selectById(id);
        if (existing == null) {
            log.warn("删除外链失败：记录不存在，id={}", id);
            return SaResult.error("外链不存在").setCode(404);
        }
        if (existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
            log.warn("删除外链失败：记录已被删除，id={}", id);
            return SaResult.error("外链已被删除").setCode(404);
        }

        friendLinkMapper.update(null, new LambdaUpdateWrapper<SysFriendLink>()
                .eq(SysFriendLink::getId, id)
                .set(SysFriendLink::getIsDeleted, 1)
                .set(SysFriendLink::getStatus, 3) // 标记为已删除
                .set(SysFriendLink::getUpdateTime, LocalDateTime.now()));

        log.info("外链删除成功，id={}", id);
        // 清除友链缓存
        clearFriendLinkCache();
        return SaResult.data("删除成功");
    }

    /**
     * 清除友链缓存
     */
    private void clearFriendLinkCache() {
        friendLinkCache.invalidateAll();
        friendLinkListCache.invalidateAll();
        log.debug("友链缓存已清除");
    }

    private FriendLinkResponseDTO toResponseDTO(SysFriendLink link) {
        FriendLinkResponseDTO dto = new FriendLinkResponseDTO();
        dto.setId(link.getId());
        dto.setName(link.getName());
        dto.setUrl(link.getUrl());
        dto.setSummary(link.getSummary());
        dto.setRemark(link.getRemark());
        dto.setImageUrl(link.getImageUrl());
        dto.setSortOrder(link.getSortOrder());
        dto.setStatus(link.getStatus());
        dto.setCreateTime(link.getCreateTime());
        dto.setUpdateTime(link.getUpdateTime());
        return dto;
    }
}

