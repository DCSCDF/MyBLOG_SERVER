/*
 * [SysConfigServiceImpl.java]
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

package com.jiuliu.myblog_dev.service.config;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jiuliu.myblog_dev.config.MailConfig;
import com.jiuliu.myblog_dev.dto.config.*;
import com.jiuliu.myblog_dev.entity.config.SysConfig;
import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import com.jiuliu.myblog_dev.utils.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SysConfigServiceImpl implements SysConfigService {

    private static final Logger log = LoggerFactory.getLogger(SysConfigServiceImpl.class);

    /**
     * 系统配置缓存 - 缓存系统配置项，key为configKey，value为SysConfig对象
     * 缓存时间：60分钟
     */
    private final Cache<String, SysConfig> configCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build();


    private final SysConfigMapper sysConfigMapper;
    private final MailConfig mailConfig;

    public SysConfigServiceImpl(SysConfigMapper sysConfigMapper, MailConfig mailConfig) {
        this.sysConfigMapper = sysConfigMapper;
        this.mailConfig = mailConfig;
    }

    @Override
    public SaResult getSystemConfigByKeys(ConfigSystemKeysDTO dto) {
        if (dto.getKeys() == null || dto.getKeys().isEmpty()) {
            log.warn("系统配置查询失败：配置键列表为空");
            return SaResult.error("配置键列表不能为空").setCode(400);
        }
        List<String> keys = dto.getKeys().stream().filter(StringUtils::hasText).distinct().toList();
        if (keys.isEmpty()) {
            log.warn("系统配置查询失败：过滤后配置键列表为空");
            return SaResult.error("配置键列表不能为空").setCode(400);
        }

        // 从缓存中获取配置，先尝试从缓存读取每个key
        List<SysConfig> resultList = new java.util.ArrayList<>();
        List<String> missingKeys = new java.util.ArrayList<>();

        for (String key : keys) {
            String cacheKey = CacheUtil.CACHE_KEY_SYS_CONFIG + key;
            SysConfig cached = configCache.getIfPresent(cacheKey);
            if (cached != null) {
                resultList.add(cached);
                log.debug("从缓存获取系统配置，key={}", key);
            } else {
                missingKeys.add(key);
            }
        }

        // 缓存未命中，从数据库查询
        if (!missingKeys.isEmpty()) {
            LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
                    .in(SysConfig::getConfigKey, missingKeys)
                    .eq(SysConfig::getIsSystem, 1)
                    .eq(SysConfig::getIsDeleted, 0);
            List<SysConfig> dbConfigs = sysConfigMapper.selectList(wrapper);

            // 将数据库查询结果放入缓存
            for (SysConfig config : dbConfigs) {
                String cacheKey = CacheUtil.CACHE_KEY_SYS_CONFIG + config.getConfigKey();
                configCache.put(cacheKey, config);
                resultList.add(config);
            }

            log.debug("从数据库查询系统配置，keys={}", missingKeys);
        }

        List<ConfigItemResponseDTO> result = resultList.stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
        return SaResult.data(result);
    }

    @Override
    public SaResult getPageCustomConfigs(PageConfigCustomDTO pageDto) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getIsSystem, 0)
                .eq(SysConfig::getIsDeleted, 0)
                .orderByDesc(SysConfig::getCreateTime);
        if (StringUtils.hasText(pageDto.getKeyword())) {
            String kw = pageDto.getKeyword().trim();
            wrapper.and(w -> w.like(SysConfig::getConfigKey, kw).or().like(SysConfig::getDescription, kw));
        }
        Page<SysConfig> page = new Page<>(pageDto.getCurrentPage(), pageDto.getPageSize());
        Page<SysConfig> pageResult = sysConfigMapper.selectPage(page, wrapper);
        List<ConfigItemResponseDTO> records = pageResult.getRecords().stream()
                .map(this::toItemResponseWithId)
                .collect(Collectors.toList());
        PageConfigCustomResponseDTO response = new PageConfigCustomResponseDTO();
        response.setRecords(records);
        response.setTotal(pageResult.getTotal());
        response.setSize(pageResult.getSize());
        response.setCurrent(pageResult.getCurrent());
        response.setPages(pageResult.getPages());
        return SaResult.data(response);
    }

    @Override
    public SaResult createCustomConfig(ConfigCreateDTO dto) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, dto.getConfigKey())
                .eq(SysConfig::getIsDeleted, 0);
        SysConfig existing = sysConfigMapper.selectOne(wrapper);
        if (existing != null) {
            log.warn("创建自定义配置失败：配置键已存在，configKey={}", dto.getConfigKey());
            return SaResult.error("配置键已存在").setCode(400);
        }
        SysConfig config = new SysConfig();
        config.setConfigKey(dto.getConfigKey());
        config.setConfigValue(dto.getConfigValue());
        config.setDataType(StringUtils.hasText(dto.getDataType()) ? dto.getDataType() : "string");
        config.setValidationRule(dto.getValidationRule());
        config.setDescription(dto.getDescription());
        config.setIsSystem(0);
        config.setIsDeleted(0);
        sysConfigMapper.insert(config);
        log.info("自定义配置项创建成功，id={}, configKey={}", config.getId(), config.getConfigKey());
        // 清除相关缓存
        clearConfigCache(dto.getConfigKey());
        return SaResult.data(toItemResponseWithId(config));
    }

    @Override
    public SaResult updateConfigValue(ConfigUpdateDTO dto) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, dto.getConfigKey())
                .eq(SysConfig::getIsDeleted, 0);
        SysConfig config = sysConfigMapper.selectOne(wrapper);
        if (config == null) {
            log.warn("修改配置失败：配置项不存在，configKey={}", dto.getConfigKey());
            return SaResult.error("配置项不存在").setCode(404);
        }
        LambdaUpdateWrapper<SysConfig> updateWrapper = new LambdaUpdateWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, dto.getConfigKey())
                .set(SysConfig::getConfigValue, dto.getConfigValue())
                .set(SysConfig::getUpdateTime, LocalDateTime.now());
        sysConfigMapper.update(null, updateWrapper);

        // 清除相关缓存
        clearConfigCache(dto.getConfigKey());

        // 如果是邮件相关配置，立即刷新邮件发送器
        if (isMailRelatedConfig(dto.getConfigKey())) {
            mailConfig.refreshMailSender();
            log.info("邮件配置已更新，已触发邮件发送器立即刷新");
        }

        log.info("网站配置更新成功，configKey={}", dto.getConfigKey());
        SysConfig updated = sysConfigMapper.selectOne(wrapper);
        return SaResult.data(toItemResponse(updated));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult deleteCustomConfig(Long id) {
        SysConfig config = sysConfigMapper.selectById(id);
        if (config == null) {
            log.warn("删除自定义配置失败：配置项不存在，id={}", id);
            return SaResult.error("配置项不存在").setCode(404);
        }
        if (config.getIsDeleted() != null && config.getIsDeleted() == 1) {
            log.warn("删除自定义配置失败：配置项已被删除，id={}", id);
            return SaResult.error("配置项已被删除").setCode(404);
        }
        if (config.getIsSystem() != null && config.getIsSystem() == 1) {
            log.warn("删除自定义配置失败：系统内置配置项不可删除，id={}, configKey={}", id, config.getConfigKey());
            return SaResult.error("系统内置配置项不可删除").setCode(403);
        }
        // 为被删除的配置项添加"_已删除"后缀，防止与未删除的配置项产生键名冲突
        String deletedConfigKey = config.getConfigKey() + "_已删除";
        LambdaUpdateWrapper<SysConfig> updateWrapper = new LambdaUpdateWrapper<SysConfig>()
                .eq(SysConfig::getId, id)
                .set(SysConfig::getConfigKey, deletedConfigKey)
                .set(SysConfig::getIsDeleted, 1)
                .set(SysConfig::getUpdateTime, LocalDateTime.now());
        sysConfigMapper.update(null, updateWrapper);

        // 清除相关缓存
        clearConfigCache(config.getConfigKey());

        log.info("自定义配置项删除成功，id={}, configKey={}", id, config.getConfigKey());
        return SaResult.data("删除成功");
    }

    /**
     * 清除配置缓存
     * 清除包含该配置键的所有相关缓存
     *
     * @param configKey 配置键
     */
    private void clearConfigCache(String configKey) {
        if (configKey == null) {
            return;
        }
        // 清除精确匹配的缓存项
        configCache.invalidate(CacheUtil.CACHE_KEY_SYS_CONFIG + configKey);
        // 清除所有系统配置缓存（因为缓存key是由多个键组合而成的）
        configCache.invalidateAll();
        log.debug("配置缓存已清除，configKey={}", configKey);
    }

    private ConfigItemResponseDTO toItemResponse(SysConfig c) {
        ConfigItemResponseDTO dto = new ConfigItemResponseDTO();
        dto.setConfigKey(c.getConfigKey());
        dto.setConfigValue(c.getConfigValue());
        dto.setDataType(c.getDataType());
        dto.setValidationRule(c.getValidationRule());
        dto.setDescription(c.getDescription());
        dto.setCreateTime(c.getCreateTime());
        dto.setUpdateTime(c.getUpdateTime());
        return dto;
    }

    private ConfigItemResponseDTO toItemResponseWithId(SysConfig c) {
        ConfigItemResponseDTO dto = toItemResponse(c);
        dto.setId(c.getId());
        return dto;
    }

    /**
     * 判断配置键是否与邮件相关
     */
    private boolean isMailRelatedConfig(String configKey) {
        return configKey != null && configKey.startsWith("smtp.");
    }
}
