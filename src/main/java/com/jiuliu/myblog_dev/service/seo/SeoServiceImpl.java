/*
 * [SeoServiceImpl.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/21
 */

package com.jiuliu.myblog_dev.service.seo;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import com.jiuliu.myblog_dev.dto.seo.*;
import com.jiuliu.myblog_dev.entity.seo.SysSeo;
import com.jiuliu.myblog_dev.mapper.seo.SysSeoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SeoServiceImpl implements SeoService {

    private static final Logger log = LoggerFactory.getLogger(SeoServiceImpl.class);

    private final SysSeoMapper sysSeoMapper;

    public SeoServiceImpl(SysSeoMapper sysSeoMapper) {
        this.sysSeoMapper = sysSeoMapper;
    }

    @Override
    public SaResult getPageSeos(PageSeoDTO pageDto) {
        try {
            LambdaQueryWrapper<SysSeo> wrapper = new LambdaQueryWrapper<SysSeo>()
                    .eq(SysSeo::getIsDeleted, 0)
                    .eq(StringUtils.hasText(pageDto.getPageType()), SysSeo::getPageType, pageDto.getPageType())
                    .eq(pageDto.getIsSystem() != null, SysSeo::getIsSystem, pageDto.getIsSystem() != null && pageDto.getIsSystem() == 1)
                    .orderByDesc(SysSeo::getCreateTime);

            if (StringUtils.hasText(pageDto.getKeyword())) {
                String kw = pageDto.getKeyword().trim();
                wrapper.and(w -> w.like(SysSeo::getPageType, kw)
                        .or().like(SysSeo::getTitle, kw)
                        .or().like(SysSeo::getKeywords, kw)
                        .or().like(SysSeo::getDescription, kw));
            }

            Page<SysSeo> page = new Page<>(pageDto.getCurrentPage(), pageDto.getPageSize());
            Page<SysSeo> pageResult = sysSeoMapper.selectPage(page, wrapper);

            List<SeoResponseDTO> dtos = pageResult.getRecords().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());

            PageSeoResponseDTO response = new PageSeoResponseDTO();
            response.setRecords(dtos);
            response.setTotal(pageResult.getTotal());
            response.setSize(pageResult.getSize());
            response.setCurrent(pageResult.getCurrent());
            response.setPages(pageResult.getPages());
            // 页面类型筛选项来自数据库中的不重复类型名称，用户也可在新增时自定义传入类型名称
            List<String> distinctPageTypes = sysSeoMapper.selectDistinctPageTypes();
            response.setFilterOptions(buildSeoListFilterOptions(distinctPageTypes));

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取SEO列表异常", e);
            return SaResult.error("获取SEO列表失败").setCode(500);
        }
    }

    /**
     * 构建列表筛选项：页面类型来自数据库不重复值（类型名称不重复），支持用户自定义添加时传入新类型名称
     */
    private static Map<String, List<FilterOptionItem>> buildSeoListFilterOptions(List<String> distinctPageTypes) {
        List<FilterOptionItem> pageTypeOptions = distinctPageTypes == null
                ? new ArrayList<>()
                : distinctPageTypes.stream()
                .map(t -> new FilterOptionItem(t, t))
                .collect(Collectors.toList());
        return Map.of(
                "pageType", pageTypeOptions,
                "isSystem", List.of(
                        new FilterOptionItem(0, "否"),
                        new FilterOptionItem(1, "是")
                ));
    }

    @Override
    public SaResult getSeoById(Long id) {
        SysSeo seo = sysSeoMapper.selectOne(
                new LambdaQueryWrapper<SysSeo>()
                        .eq(SysSeo::getId, id)
                        .eq(SysSeo::getIsDeleted, 0));
        if (seo == null) {
            return SaResult.error("SEO配置不存在").setCode(404);
        }
        return SaResult.data(toResponseDTO(seo));
    }

    @Override
    public SaResult createSeo(SeoCreateDTO dto) {
        // 检查页面类型和页面ID的组合是否已存在
        LambdaQueryWrapper<SysSeo> wrapper = new LambdaQueryWrapper<SysSeo>()
                .eq(SysSeo::getPageType, dto.getPageType())
                .eq(dto.getPageId() != null, SysSeo::getPageId, dto.getPageId())
                .isNull(dto.getPageId() == null, SysSeo::getPageId)
                .eq(SysSeo::getIsDeleted, 0);

        SysSeo existingSeo = sysSeoMapper.selectOne(wrapper);
        if (existingSeo != null) {
            return SaResult.error("该页面类型的SEO配置已存在").setCode(400);
        }

        SysSeo seo = getSysSeo(dto);

        sysSeoMapper.insert(seo);
        log.info("SEO配置创建成功，id={}, pageType={}, pageId={}", seo.getId(), seo.getPageType(), seo.getPageId());
        return SaResult.data(toResponseDTO(seo));
    }

    @NonNull
    private static SysSeo getSysSeo(SeoCreateDTO dto) {
        SysSeo seo = new SysSeo();
        seo.setPageType(dto.getPageType());
        seo.setPageId(dto.getPageId());
        seo.setTitle(dto.getTitle());
        seo.setKeywords(dto.getKeywords());
        seo.setDescription(dto.getDescription());
        seo.setOgTitle(dto.getOgTitle());
        seo.setOgDescription(dto.getOgDescription());
        seo.setOgImage(dto.getOgImage());
        seo.setOgType(dto.getOgType() != null ? dto.getOgType() : "website");
        seo.setCanonicalUrl(dto.getCanonicalUrl());
        seo.setRobots(dto.getRobots() != null ? dto.getRobots() : "index,follow");
        seo.setIsDeleted(0);
        seo.setIsSystem(false);
        return seo;
    }

    @Override
    public SaResult updateSeo(SeoUpdateDTO dto) {
        SysSeo seo = sysSeoMapper.selectById(dto.getId());
        if (seo == null) {
            return SaResult.error("SEO配置不存在").setCode(404);
        }
        if (seo.getIsDeleted() != null && seo.getIsDeleted() == 1) {
            return SaResult.error("SEO配置已被删除").setCode(404);
        }

        // 如果修改了pageId，需要检查新的组合是否已存在
        if (dto.getPageId() != null && !dto.getPageId().equals(seo.getPageId())) {
            LambdaQueryWrapper<SysSeo> wrapper = new LambdaQueryWrapper<SysSeo>()
                    .eq(SysSeo::getPageType, seo.getPageType())
                    .eq(SysSeo::getPageId, dto.getPageId())
                    .ne(SysSeo::getId, dto.getId())
                    .eq(SysSeo::getIsDeleted, 0);

            SysSeo existingSeo = sysSeoMapper.selectOne(wrapper);
            if (existingSeo != null) {
                return SaResult.error("该页面类型的SEO配置已存在").setCode(400);
            }
        }

        LambdaUpdateWrapper<SysSeo> updateWrapper = new LambdaUpdateWrapper<SysSeo>()
                .eq(SysSeo::getId, dto.getId())
                .set(dto.getPageId() != null, SysSeo::getPageId, dto.getPageId())
                .set(dto.getTitle() != null, SysSeo::getTitle, dto.getTitle())
                .set(dto.getKeywords() != null, SysSeo::getKeywords, dto.getKeywords())
                .set(dto.getDescription() != null, SysSeo::getDescription, dto.getDescription())
                .set(dto.getOgTitle() != null, SysSeo::getOgTitle, dto.getOgTitle())
                .set(dto.getOgDescription() != null, SysSeo::getOgDescription, dto.getOgDescription())
                .set(dto.getOgImage() != null, SysSeo::getOgImage, dto.getOgImage())
                .set(dto.getOgType() != null, SysSeo::getOgType, dto.getOgType())
                .set(dto.getCanonicalUrl() != null, SysSeo::getCanonicalUrl, dto.getCanonicalUrl())
                .set(dto.getRobots() != null, SysSeo::getRobots, dto.getRobots())
                .set(SysSeo::getUpdateTime, LocalDateTime.now());

        sysSeoMapper.update(null, updateWrapper);
        log.info("SEO配置更新成功，id={}", dto.getId());
        return SaResult.data(toResponseDTO(sysSeoMapper.selectById(dto.getId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult deleteSeo(Long id) {
        SysSeo seo = sysSeoMapper.selectById(id);
        if (seo == null) {
            return SaResult.error("SEO配置不存在").setCode(404);
        }
        if (seo.getIsDeleted() != null && seo.getIsDeleted() == 1) {
            return SaResult.error("SEO配置已被删除").setCode(404);
        }
        if (Boolean.TRUE.equals(seo.getIsSystem())) {
            return SaResult.error("系统内置SEO配置不可删除").setCode(403);
        }

        // 逻辑删除
        sysSeoMapper.update(null, new LambdaUpdateWrapper<SysSeo>()
                .eq(SysSeo::getId, id)
                .set(SysSeo::getIsDeleted, 1)
                .set(SysSeo::getUpdateTime, LocalDateTime.now()));

        log.info("SEO配置删除成功，id={}", id);
        return SaResult.data("删除成功");
    }

    private SeoResponseDTO toResponseDTO(SysSeo seo) {
        SeoResponseDTO dto = new SeoResponseDTO();
        dto.setId(seo.getId());
        dto.setPageType(seo.getPageType());
        dto.setPageId(seo.getPageId());
        dto.setTitle(seo.getTitle());
        dto.setKeywords(seo.getKeywords());
        dto.setDescription(seo.getDescription());
        dto.setOgTitle(seo.getOgTitle());
        dto.setOgDescription(seo.getOgDescription());
        dto.setOgImage(seo.getOgImage());
        dto.setOgType(seo.getOgType());
        dto.setCanonicalUrl(seo.getCanonicalUrl());
        dto.setRobots(seo.getRobots());
        dto.setIsSystem(seo.getIsSystem());
        dto.setCreateTime(seo.getCreateTime());
        dto.setUpdateTime(seo.getUpdateTime());
        return dto;
    }
}
