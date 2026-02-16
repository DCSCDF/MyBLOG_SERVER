/*
 * [PermissionGroupServiceImpl.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.service.user.permissiongroup;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.*;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroup;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionGroupServiceImpl implements PermissionGroupService {

    private static final Logger log = LoggerFactory.getLogger(PermissionGroupServiceImpl.class);

    private final SysPermissionGroupMapper sysPermissionGroupMapper;

    public PermissionGroupServiceImpl(SysPermissionGroupMapper sysPermissionGroupMapper) {
        this.sysPermissionGroupMapper = sysPermissionGroupMapper;
    }

    @Override
    public SaResult getPagePermissionGroups(PagePermissionGroupDTO pageDto) {
        try {
            LambdaQueryWrapper<SysPermissionGroup> wrapper = new LambdaQueryWrapper<SysPermissionGroup>()
                    .eq(SysPermissionGroup::getIsDeleted, 0)
                    .orderByDesc(SysPermissionGroup::getSortOrder);

            Page<SysPermissionGroup> page = new Page<>(pageDto.getCurrentPage(), pageDto.getPageSize());
            Page<SysPermissionGroup> pageResult = sysPermissionGroupMapper.selectPage(page, wrapper);

            List<PermissionGroupResponseDTO> dtos = pageResult.getRecords().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());

            PagePermissionGroupResponseDTO response = new PagePermissionGroupResponseDTO();
            response.setRecords(dtos);
            response.setTotal(pageResult.getTotal());
            response.setSize(pageResult.getSize());
            response.setCurrent(pageResult.getCurrent());
            response.setPages(pageResult.getPages());

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取权限组列表异常", e);
            return SaResult.error("获取权限组列表失败").setCode(500);
        }
    }

    @Override
    public SaResult getPermissionGroupById(Long id) {
        SysPermissionGroup group = sysPermissionGroupMapper.selectOne(
                new LambdaQueryWrapper<SysPermissionGroup>()
                        .eq(SysPermissionGroup::getId, id)
                        .eq(SysPermissionGroup::getIsDeleted, 0));
        if (group == null) {
            return SaResult.error("权限组不存在").setCode(404);
        }
        return SaResult.data(toResponseDTO(group));
    }

    @Override
    public SaResult updatePermissionGroup(PermissionGroupUpdateDTO dto) {
        SysPermissionGroup group = sysPermissionGroupMapper.selectById(dto.getId());
        if (group == null) {
            return SaResult.error("权限组不存在").setCode(404);
        }
        if (Boolean.TRUE.equals(group.getIsSystem())) {
            return SaResult.error("系统内置权限组不可修改").setCode(403);
        }

        LambdaUpdateWrapper<SysPermissionGroup> wrapper = new LambdaUpdateWrapper<SysPermissionGroup>()
                .eq(SysPermissionGroup::getId, dto.getId())
                .set(SysPermissionGroup::getName, dto.getName())
                .set(dto.getDescription() != null, SysPermissionGroup::getDescription, dto.getDescription())
                .set(dto.getSortOrder() != null, SysPermissionGroup::getSortOrder, dto.getSortOrder())
                .set(dto.getStatus() != null, SysPermissionGroup::getStatus, dto.getStatus());

        sysPermissionGroupMapper.update(null, wrapper);
        log.info("权限组更新成功，id={}", dto.getId());
        return SaResult.data(toResponseDTO(sysPermissionGroupMapper.selectById(dto.getId())));
    }

    @Override
    public SaResult deletePermissionGroup(Long id) {
        SysPermissionGroup group = sysPermissionGroupMapper.selectById(id);
        if (group == null) {
            return SaResult.error("权限组不存在").setCode(404);
        }
        if (Boolean.TRUE.equals(group.getIsSystem())) {
            return SaResult.error("系统内置权限组不可删除").setCode(403);
        }

        sysPermissionGroupMapper.update(null, new LambdaUpdateWrapper<SysPermissionGroup>()
                .eq(SysPermissionGroup::getId, id)
                .set(SysPermissionGroup::getIsDeleted, 1));
        log.info("权限组逻辑删除成功，id={}", id);
        return SaResult.data("删除成功");
    }

    private PermissionGroupResponseDTO toResponseDTO(SysPermissionGroup group) {
        PermissionGroupResponseDTO dto = new PermissionGroupResponseDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setSortOrder(group.getSortOrder());
        dto.setStatus(group.getStatus());
        dto.setIsSystem(group.getIsSystem());
        dto.setCreateTime(group.getCreateTime());
        dto.setUpdateTime(group.getUpdateTime());
        return dto;
    }
}
