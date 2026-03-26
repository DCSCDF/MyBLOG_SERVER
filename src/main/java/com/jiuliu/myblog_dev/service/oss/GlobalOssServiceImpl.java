/*
 * [GlobalOssServiceImpl.java]
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

package com.jiuliu.myblog_dev.service.oss;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.config.business.OSSConfig;
import com.jiuliu.myblog_dev.dto.oss.global.GlobalOssResponseDTO;
import com.jiuliu.myblog_dev.dto.oss.global.PageGlobalOssDTO;
import com.jiuliu.myblog_dev.dto.oss.global.PageGlobalOssResponseDTO;
import com.jiuliu.myblog_dev.entity.oss.SysOssImage;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.mapper.oss.SysOssImageMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 全局OSS管理 Service 实现类
 */
@Service
public class GlobalOssServiceImpl implements GlobalOssService {

    private static final Logger log = LoggerFactory.getLogger(GlobalOssServiceImpl.class);

    /**
     * 全局OSS列表缓存
     * 缓存时间：10分钟
     */
    private final Cache<String, PageGlobalOssResponseDTO> globalOssListCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private final SysOssImageMapper sysOssImageMapper;
    private final SysUserMapper sysUserMapper;
    private final OSSConfig ossConfig;

    public GlobalOssServiceImpl(SysOssImageMapper sysOssImageMapper,
                                SysUserMapper sysUserMapper,
                                OSSConfig ossConfig) {
        this.sysOssImageMapper = sysOssImageMapper;
        this.sysUserMapper = sysUserMapper;
        this.ossConfig = ossConfig;
    }

    @Override
    public SaResult getPageGlobalOssImages(PageGlobalOssDTO dto) {
        try {
            // 构建缓存键
            String cacheKey = "global_oss_list_" + dto.getCurrentPage() + "_" + dto.getPageSize() + "_" +
                    (dto.getKeyword() != null ? dto.getKeyword() : "");

            // 尝试从缓存获取
            PageGlobalOssResponseDTO cached = globalOssListCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("从缓存获取全局OSS图片列表，key={}", cacheKey);
                return SaResult.data(cached);
            }

            // 构建查询条件
            LambdaQueryWrapper<SysOssImage> queryWrapper = new LambdaQueryWrapper<>();

            // 关键词搜索：文件名、哈希值、用户名
            if (StringUtils.hasText(dto.getKeyword())) {
                // 先查询匹配用户名的用户ID
                List<SysUser> matchedUsers = sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().like(SysUser::getNickname, dto.getKeyword())
                );
                List<Long> matchedUserIds = matchedUsers.stream()
                        .map(SysUser::getId)
                        .collect(Collectors.toList());

                // 构建搜索条件：文件名匹配 OR 哈希匹配 OR 用户ID匹配
                queryWrapper.and(wrapper -> wrapper
                        .like(SysOssImage::getOriginalName, dto.getKeyword())
                        .or()
                        .like(SysOssImage::getHash, dto.getKeyword())
                        .or(matchedUserIds.isEmpty() ? null : w -> w.in(SysOssImage::getUserId, matchedUserIds))
                );
            }

            // 按创建时间倒序
            queryWrapper.orderByDesc(SysOssImage::getCreateTime);

            // 分页查询
            Page<SysOssImage> page = new Page<>(dto.getCurrentPage(), dto.getPageSize());
            IPage<SysOssImage> pageResult = sysOssImageMapper.selectPage(page, queryWrapper);

            // 收集所有用户 ID
            List<Long> userIds = pageResult.getRecords().stream()
                    .map(SysOssImage::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            // 批量查询用户信息
            Map<Long, String> userNameMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<SysUser> users = sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds)
                );
                for (SysUser user : users) {
                    userNameMap.put(user.getId(), user.getNickname());
                }
            }

            // 转换为响应DTO
            List<GlobalOssResponseDTO> records = pageResult.getRecords().stream()
                    .map(image -> convertToGlobalOssResponseDTO(image, userNameMap))
                    .collect(Collectors.toList());

            // 构建响应
            PageGlobalOssResponseDTO response = new PageGlobalOssResponseDTO();
            response.setRecords(records);
            response.setTotal(pageResult.getTotal());
            response.setSize(pageResult.getSize());
            response.setCurrent(pageResult.getCurrent());
            response.setPages(pageResult.getPages());

            // 存入缓存
            globalOssListCache.put(cacheKey, response);

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取OSS图片列表异常", e);
            return SaResult.error("获取图片列表失败").setCode(500);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult deleteImage(String hash) {
        log.debug("开始删除图片（管理员），hash=[{}]", hash);

        if (hash == null || hash.isBlank()) {
            log.warn("图片删除失败：哈希值为空");
            return SaResult.error("哈希值不能为空").setCode(400);
        }

        if (!ossConfig.isConfigured()) {
            log.warn("图片删除失败：OSS 配置未完成");
            return SaResult.error("OSS 配置未完成，请先在系统配置中完成阿里云 OSS 相关配置").setCode(400);
        }

        // 1. 从数据库查询图片记录
        SysOssImage imageRecord = sysOssImageMapper.selectByHash(hash);
        if (imageRecord == null) {
            log.warn("图片记录不存在，hash=[{}]", hash);
            return SaResult.error("图片记录不存在").setCode(404);
        }

        String objectName = imageRecord.getObjectName();

        // 2. 删除 OSS 中的图片
        com.aliyun.oss.OSS ossClient = ossConfig.getOssClient();
        if (ossClient == null) {
            log.error("图片删除失败：无法获取 OSS 客户端");
            return SaResult.error("OSS 客户端初始化失败，请检查配置").setCode(500);
        }

        try {
            ossClient.deleteObject(ossConfig.getBucket(), objectName);
        } catch (Exception e) {
            log.error("OSS 图片删除失败：{}", e.getMessage(), e);
            return SaResult.error("删除 OSS 图片失败：" + e.getMessage()).setCode(500);
        }

        // 3. 删除数据库记录
        try {
            sysOssImageMapper.deleteById(imageRecord.getId());
        } catch (Exception e) {
            log.error("数据库图片记录删除失败：{}", e.getMessage(), e);
            return SaResult.error("OSS 图片已删除，但数据库记录删除失败").setCode(500);
        }

        // 4. 清除缓存
        clearCache();

        log.info("管理员删除图片成功，hash=[{}]，objectName=[{}]", hash, objectName);
        return SaResult.ok().setMsg("删除成功");
    }

    @Override
    public void clearCache() {
        globalOssListCache.invalidateAll();
        log.debug("全局OSS列表缓存已清除");
    }

    /**
     * 将实体转换为全局OSS响应DTO
     */
    private GlobalOssResponseDTO convertToGlobalOssResponseDTO(SysOssImage image, Map<Long, String> userNameMap) {
        GlobalOssResponseDTO dto = new GlobalOssResponseDTO();
        dto.setId(image.getId());
        dto.setHash(image.getHash());
        dto.setOriginalName(image.getOriginalName());
        dto.setObjectName(image.getObjectName());
        dto.setFileSize(image.getFileSize());
        dto.setUserId(image.getUserId());
        dto.setUsername(userNameMap.get(image.getUserId()));
        dto.setCreateTime(image.getCreateTime());
        return dto;
    }
}
