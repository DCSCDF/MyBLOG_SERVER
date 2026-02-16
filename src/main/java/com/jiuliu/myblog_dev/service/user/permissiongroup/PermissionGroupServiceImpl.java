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
import com.jiuliu.myblog_dev.dto.user.permission.PermissionResponseDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.*;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroup;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroupItem;
import com.jiuliu.myblog_dev.entity.user.role.SysRolePermissionGroup;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupItemMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionGroupMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionGroupServiceImpl implements PermissionGroupService {

    private static final Logger log = LoggerFactory.getLogger(PermissionGroupServiceImpl.class);

    private final SysPermissionGroupMapper sysPermissionGroupMapper;
    private final SysPermissionGroupItemMapper sysPermissionGroupItemMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionGroupMapper sysRolePermissionGroupMapper;

    public PermissionGroupServiceImpl(SysPermissionGroupMapper sysPermissionGroupMapper,
                                      SysPermissionGroupItemMapper sysPermissionGroupItemMapper,
                                      SysPermissionMapper sysPermissionMapper,
                                      SysRolePermissionGroupMapper sysRolePermissionGroupMapper) {
        this.sysPermissionGroupMapper = sysPermissionGroupMapper;
        this.sysPermissionGroupItemMapper = sysPermissionGroupItemMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysRolePermissionGroupMapper = sysRolePermissionGroupMapper;
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
    @Transactional(rollbackFor = Exception.class)
    public SaResult deletePermissionGroup(Long id) {
        SysPermissionGroup group = sysPermissionGroupMapper.selectById(id);
        if (group == null) {
            return SaResult.error("权限组不存在").setCode(404);
        }
        if (Boolean.TRUE.equals(group.getIsSystem())) {
            return SaResult.error("系统内置权限组不可删除").setCode(403);
        }

        // 1. 删除权限组-权限关联
        sysPermissionGroupItemMapper.delete(new LambdaQueryWrapper<SysPermissionGroupItem>()
                .eq(SysPermissionGroupItem::getGroupId, id));
        // 2. 删除角色-权限组关联
        sysRolePermissionGroupMapper.delete(new LambdaQueryWrapper<SysRolePermissionGroup>()
                .eq(SysRolePermissionGroup::getGroupId, id));
        // 3. 逻辑删除权限组
        sysPermissionGroupMapper.update(null, new LambdaUpdateWrapper<SysPermissionGroup>()
                .eq(SysPermissionGroup::getId, id)
                .set(SysPermissionGroup::getIsDeleted, 1));
        log.info("权限组删除成功，已级联删除关联数据，id={}", id);
        return SaResult.data("删除成功");
    }

    @Override
    public SaResult getPermissionsByGroupId(Long groupId) {
        SysPermissionGroup group = sysPermissionGroupMapper.selectOne(
                new LambdaQueryWrapper<SysPermissionGroup>()
                        .eq(SysPermissionGroup::getId, groupId)
                        .eq(SysPermissionGroup::getIsDeleted, 0));
        if (group == null) {
            return SaResult.error("权限组不存在").setCode(404);
        }
        List<SysPermission> permissions = sysPermissionMapper.selectPermissionsByGroupId(groupId);
        List<PermissionResponseDTO> dtos = permissions.stream().map(this::toPermissionDTO).collect(Collectors.toList());
        return SaResult.data(dtos);
    }

    @Override
    public SaResult addPermissionToGroup(Long groupId, Long permissionId) {
        SysPermissionGroup group = sysPermissionGroupMapper.selectById(groupId);
        if (group == null || (group.getIsDeleted() != null && group.getIsDeleted() == 1)) {
            return SaResult.error("权限组不存在").setCode(404);
        }
        if (Boolean.TRUE.equals(group.getIsSystem())) {
            return SaResult.error("系统内置权限组不可修改").setCode(403);
        }
        if (sysPermissionMapper.selectById(permissionId) == null) {
            return SaResult.error("权限不存在").setCode(404);
        }

        long count = sysPermissionGroupItemMapper.selectCount(
                new LambdaQueryWrapper<SysPermissionGroupItem>()
                        .eq(SysPermissionGroupItem::getGroupId, groupId)
                        .eq(SysPermissionGroupItem::getPermissionId, permissionId));
        if (count > 0) {
            return SaResult.error("该权限已在权限组中").setCode(400);
        }

        SysPermissionGroupItem item = new SysPermissionGroupItem();
        item.setGroupId(groupId);
        item.setPermissionId(permissionId);
        item.setSortOrder(0);
        sysPermissionGroupItemMapper.insert(item);
        log.info("权限组添加权限成功，groupId={}, permissionId={}", groupId, permissionId);
        return SaResult.data("添加成功");
    }

    @Override
    public SaResult removePermissionFromGroup(Long groupId, Long permissionId) {
        SysPermissionGroup group = sysPermissionGroupMapper.selectById(groupId);
        if (group == null || (group.getIsDeleted() != null && group.getIsDeleted() == 1)) {
            return SaResult.error("权限组不存在").setCode(404);
        }
        if (Boolean.TRUE.equals(group.getIsSystem())) {
            return SaResult.error("系统内置权限组不可修改").setCode(403);
        }

        int deleted = sysPermissionGroupItemMapper.delete(
                new LambdaQueryWrapper<SysPermissionGroupItem>()
                        .eq(SysPermissionGroupItem::getGroupId, groupId)
                        .eq(SysPermissionGroupItem::getPermissionId, permissionId));
        if (deleted == 0) {
            return SaResult.error("该权限不在权限组中").setCode(400);
        }
        log.info("权限组移除权限成功，groupId={}, permissionId={}", groupId, permissionId);
        return SaResult.data("移除成功");
    }

    private PermissionResponseDTO toPermissionDTO(SysPermission p) {
        PermissionResponseDTO dto = new PermissionResponseDTO();
        dto.setId(p.getId());
        dto.setCode(p.getCode());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setSortOrder(p.getSortOrder());
        dto.setCreateTime(p.getCreateTime());
        return dto;
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
