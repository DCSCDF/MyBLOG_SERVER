/*
 * [CategoryServiceImpl.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/2
 */

package com.jiuliu.myblog_dev.service.blog.category;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuliu.myblog_dev.dto.blog.category.*;
import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import com.jiuliu.myblog_dev.entity.blog.category.SysCategory;
import com.jiuliu.myblog_dev.mapper.blog.category.SysCategoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final SysCategoryMapper categoryMapper;

    public CategoryServiceImpl(SysCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public SaResult getPageCategories(PageCategoryDTO pageDto) {
        try {
            LambdaQueryWrapper<SysCategory> wrapper = new LambdaQueryWrapper<SysCategory>()
                    .orderByDesc(SysCategory::getSortOrder)
                    .orderByDesc(SysCategory::getCreateTime);

            if (pageDto.getHidden() != null) {
                wrapper.eq(SysCategory::getHidden, pageDto.getHidden());
            }

            if (StringUtils.hasText(pageDto.getKeyword())) {
                String kw = pageDto.getKeyword().trim();
                wrapper.and(w -> w.like(SysCategory::getName, kw)
                        .or().like(SysCategory::getDescription, kw));
            }

            Page<SysCategory> page = new Page<>(pageDto.getCurrentPage(), pageDto.getPageSize());
            Page<SysCategory> pageResult = categoryMapper.selectPage(page, wrapper);

            List<CategoryResponseDTO> records = pageResult.getRecords().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());

            PageCategoryResponseDTO response = new PageCategoryResponseDTO();
            response.setRecords(records);
            response.setTotal(pageResult.getTotal());
            response.setSize(pageResult.getSize());
            response.setCurrent(pageResult.getCurrent());
            response.setPages(pageResult.getPages());
            response.setFilterOptions(buildFilterOptions());

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取分类列表异常", e);
            return SaResult.error("获取分类列表失败").setCode(500);
        }
    }

    private Map<String, List<FilterOptionItem>> buildFilterOptions() {
        List<FilterOptionItem> hiddenOptions = List.of(
                new FilterOptionItem(0, "显示"),
                new FilterOptionItem(1, "隐藏")
        );
        return Map.of("hidden", hiddenOptions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult createCategory(CategoryCreateDTO dto) {
        SysCategory category = new SysCategory();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        // 默认不隐藏
        category.setHidden(false);

        categoryMapper.insert(category);
        log.info("分类创建成功，id={}, name={}", category.getId(), category.getName());
        return SaResult.data(toResponseDTO(categoryMapper.selectById(category.getId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult updateCategory(CategoryUpdateDTO dto) {
        SysCategory existing = categoryMapper.selectById(dto.getId());
        if (existing == null) {
            log.warn("更新分类失败：记录不存在，id={}", dto.getId());
            return SaResult.error("分类不存在").setCode(404);
        }

        LambdaUpdateWrapper<SysCategory> updateWrapper = new LambdaUpdateWrapper<SysCategory>()
                .eq(SysCategory::getId, dto.getId())
                .set(dto.getName() != null, SysCategory::getName, dto.getName())
                .set(dto.getDescription() != null, SysCategory::getDescription, dto.getDescription())
                .set(dto.getSortOrder() != null, SysCategory::getSortOrder, dto.getSortOrder())
                .set(dto.getHidden() != null, SysCategory::getHidden, dto.getHidden())
                .set(SysCategory::getUpdateTime, LocalDateTime.now());

        categoryMapper.update(null, updateWrapper);
        log.info("分类更新成功，id={}", dto.getId());
        SysCategory updated = categoryMapper.selectById(dto.getId());
        return SaResult.data(toResponseDTO(updated));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult deleteCategory(Long id) {
        SysCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            log.warn("删除分类失败：记录不存在，id={}", id);
            return SaResult.error("分类不存在").setCode(404);
        }

        categoryMapper.deleteById(id);
        log.info("分类删除成功，id={}", id);
        return SaResult.data("删除成功");
    }

    private CategoryResponseDTO toResponseDTO(SysCategory category) {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setSortOrder(category.getSortOrder());
        dto.setHidden(category.getHidden());
        dto.setCreateTime(category.getCreateTime());
        dto.setUpdateTime(category.getUpdateTime());
        return dto;
    }
}

