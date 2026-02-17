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
import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import com.jiuliu.myblog_dev.dto.user.permission.PermissionResponseDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.*;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroup;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroupItem;
import com.jiuliu.myblog_dev.entity.user.role.SysRolePermission;
import com.jiuliu.myblog_dev.entity.user.role.SysRolePermissionGroup;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupItemMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionMapper;
import com.jiuliu.myblog_dev.utils.PermissionOverlapHelper;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jiuliu.myblog_dev.service.user.permission.PermissionServiceImpl.getPermissionResponseDTO;

@Service
public class PermissionGroupServiceImpl implements PermissionGroupService {

    private static final Logger log = LoggerFactory.getLogger(PermissionGroupServiceImpl.class);

    private final SysPermissionGroupMapper sysPermissionGroupMapper;
    private final SysPermissionGroupItemMapper sysPermissionGroupItemMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionGroupMapper sysRolePermissionGroupMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    public PermissionGroupServiceImpl(SysPermissionGroupMapper sysPermissionGroupMapper,
                                      SysPermissionGroupItemMapper sysPermissionGroupItemMapper,
                                      SysPermissionMapper sysPermissionMapper,
                                      SysRolePermissionGroupMapper sysRolePermissionGroupMapper,
                                      SysRolePermissionMapper sysRolePermissionMapper) {
        this.sysPermissionGroupMapper = sysPermissionGroupMapper;
        this.sysPermissionGroupItemMapper = sysPermissionGroupItemMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysRolePermissionGroupMapper = sysRolePermissionGroupMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
    }

    @Override
    public SaResult getPagePermissionGroups(PagePermissionGroupDTO pageDto) {
        try {
            LambdaQueryWrapper<SysPermissionGroup> wrapper = new LambdaQueryWrapper<SysPermissionGroup>()
                    .eq(SysPermissionGroup::getIsDeleted, 0)
                    .eq(pageDto.getStatus() != null, SysPermissionGroup::getStatus, pageDto.getStatus())
                    .eq(pageDto.getIsSystem() != null, SysPermissionGroup::getIsSystem, pageDto.getIsSystem() != null && pageDto.getIsSystem() == 1)
                    .orderByDesc(SysPermissionGroup::getSortOrder);

            if (StringUtils.hasText(pageDto.getKeyword())) {
                String kw = pageDto.getKeyword().trim();
                wrapper.and(w -> w.like(SysPermissionGroup::getName, kw)
                        .or().like(SysPermissionGroup::getDescription, kw));
            }

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
            response.setFilterOptions(buildPermissionGroupListFilterOptions());

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取权限组列表异常", e);
            return SaResult.error("获取权限组列表失败").setCode(500);
        }
    }

    private static Map<String, List<FilterOptionItem>> buildPermissionGroupListFilterOptions() {
        return Map.of(
                "status", List.of(new FilterOptionItem(0, "禁用"), new FilterOptionItem(1, "启用")),
                "isSystem", List.of(new FilterOptionItem(0, "否"), new FilterOptionItem(1, "是")));
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
    public SaResult createPermissionGroup(PermissionGroupCreateDTO dto) {
        // 检查权限组名称是否已存在（未删除的）
        SysPermissionGroup existingGroup = sysPermissionGroupMapper.selectOne(
                new LambdaQueryWrapper<SysPermissionGroup>()
                        .eq(SysPermissionGroup::getName, dto.getName())
                        .and(w -> w.eq(SysPermissionGroup::getIsDeleted, 0).or().isNull(SysPermissionGroup::getIsDeleted)));
        if (existingGroup != null) {
            return SaResult.error("权限组名称已存在").setCode(400);
        }

        SysPermissionGroup group = new SysPermissionGroup();
        group.setName(dto.getName());
        group.setDescription(dto.getDescription());
        group.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        group.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        group.setIsSystem(false);
        group.setIsDeleted(0);

        sysPermissionGroupMapper.insert(group);
        log.info("权限组创建成功，id={}, name={}", group.getId(), group.getName());
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
                .set(dto.getStatus() != null, SysPermissionGroup::getStatus, dto.getStatus())
                .set(SysPermissionGroup::getUpdateTime, LocalDateTime.now());

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
        // 3. 逻辑删除权限组：name 追加「(已删除)_id」后缀，避免名称占用便于复用
        String newName = group.getName();
        if (newName != null && !newName.contains("(已删除)")) {
            String suffix = "(已删除)_" + id;
            newName = (newName.length() + suffix.length() <= 50) ? newName + suffix : newName.substring(0, 50 - suffix.length()) + suffix;
        }
        sysPermissionGroupMapper.update(null, new LambdaUpdateWrapper<SysPermissionGroup>()
                .eq(SysPermissionGroup::getId, id)
                .set(SysPermissionGroup::getName, newName)
                .set(SysPermissionGroup::getIsDeleted, 1)
                .set(SysPermissionGroup::getUpdateTime, LocalDateTime.now()));
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
        SaResult validationResult = validatePermissionGroup(groupId);
        if (validationResult != null) {
            return validationResult;
        }
        sysPermissionGroupMapper.selectById(groupId);
        SysPermission newPerm = sysPermissionMapper.selectById(permissionId);
        if (newPerm == null) {
            return SaResult.error("权限不存在").setCode(404);
        }

        long count = sysPermissionGroupItemMapper.selectCount(
                new LambdaQueryWrapper<SysPermissionGroupItem>()
                        .eq(SysPermissionGroupItem::getGroupId, groupId)
                        .eq(SysPermissionGroupItem::getPermissionId, permissionId));
        if (count > 0) {
            return SaResult.error("该权限已在权限组中").setCode(400);
        }

        // 父子权限互斥：关联了父权限就不能关联其子权限，关联了子权限就不能关联其父权限
        List<SysPermission> existingPerms = sysPermissionMapper.selectPermissionsByGroupId(groupId);
        for (SysPermission existing : existingPerms) {
            if (PermissionOverlapHelper.overlaps(newPerm.getCode(), existing.getCode())) {
                return SaResult.error("该权限与权限组中已有权限存在父子关系，不能同时关联父权限和子权限").setCode(400);
            }
        }

        // 依赖该权限组的所有角色：新增权限不能与角色已有权限（直接+其他权限组）重叠
        List<Long> roleIdsWithGroup = sysRolePermissionGroupMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermissionGroup>().eq(SysRolePermissionGroup::getGroupId, groupId))
                .stream().map(SysRolePermissionGroup::getRoleId).toList();
        for (Long roleId : roleIdsWithGroup) {
            List<String> rolePermCodesExcludingThisGroup = getRolePermissionCodesExcludingGroup(roleId, groupId);
            for (String code : rolePermCodesExcludingThisGroup) {
                if (PermissionOverlapHelper.overlaps(newPerm.getCode(), code)) {
                    return SaResult.error("该权限与依赖此权限组的角色已有权限重叠（角色通过直接分配或其他权限组已拥有此权限或父子权限）").setCode(400);
                }
            }
        }

        SysPermissionGroupItem item = new SysPermissionGroupItem();
        item.setGroupId(groupId);
        item.setPermissionId(permissionId);
        item.setSortOrder(0);
        sysPermissionGroupItemMapper.insert(item);

        // 将新增权限同步到依赖此权限组的所有角色的 sys_role_permission
        syncPermissionToRoles(roleIdsWithGroup, permissionId, true);
        log.info("权限组添加权限成功，groupId={}, permissionId={}", groupId, permissionId);
        return SaResult.data("添加成功");
    }

    @Override
    public SaResult removePermissionFromGroup(Long groupId, Long permissionId) {
        SaResult validationResult = validatePermissionGroup(groupId);
        if (validationResult != null) {
            return validationResult;
        }
        sysPermissionGroupMapper.selectById(groupId);

        int deleted = sysPermissionGroupItemMapper.delete(
                new LambdaQueryWrapper<SysPermissionGroupItem>()
                        .eq(SysPermissionGroupItem::getGroupId, groupId)
                        .eq(SysPermissionGroupItem::getPermissionId, permissionId));
        if (deleted == 0) {
            return SaResult.error("该权限不在权限组中").setCode(400);
        }

        // 同步到依赖此权限组的所有角色：移除该权限，若角色通过其他权限组或直接分配仍拥有则需保留
        List<Long> roleIdsWithGroup = sysRolePermissionGroupMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermissionGroup>().eq(SysRolePermissionGroup::getGroupId, groupId))
                .stream().map(SysRolePermissionGroup::getRoleId).toList();
        syncPermissionToRoles(roleIdsWithGroup, permissionId, false);
        log.info("权限组移除权限成功，groupId={}, permissionId={}", groupId, permissionId);
        return SaResult.data("移除成功");
    }

    /**
     * 获取角色拥有的权限编码（排除指定权限组）
     */
    private List<String> getRolePermissionCodesExcludingGroup(Long roleId, Long excludeGroupId) {
        List<String> codes = new ArrayList<>();
        // 直接分配的权限：sys_role_permission 无法区分来源，需通过「角色权限 = 直接 + 所有权限组」反推
        // 使用：角色所有权限组（含excludeGroupId）的权限并集，再减去 excludeGroupId 的权限 = 直接 + 其它组
        List<SysPermissionGroup> groups = sysPermissionGroupMapper.selectGroupsByRoleId(roleId);
        for (SysPermissionGroup g : groups) {
            if (g.getId().equals(excludeGroupId)) continue;
            List<SysPermission> perms = sysPermissionMapper.selectPermissionsByGroupId(g.getId());
            for (SysPermission p : perms) {
                if (!codes.contains(p.getCode())) codes.add(p.getCode());
            }
        }
        // 直接权限：sys_role_permission 中减去所有权限组的权限
        List<SysPermission> rolePerms = sysPermissionMapper.selectPermissionsByRoleId(roleId);
        List<Long> fromGroups = new ArrayList<>();
        for (SysPermissionGroup g : groups) {
            fromGroups.addAll(sysPermissionMapper.selectPermissionsByGroupId(g.getId()).stream()
                    .map(SysPermission::getId).toList());
        }
        for (SysPermission p : rolePerms) {
            if (!fromGroups.contains(p.getId()) && !codes.contains(p.getCode())) {
                codes.add(p.getCode());
            }
        }
        return codes;
    }

    /**
     * 角色是否通过「其它权限组（不含指定组）」拥有指定权限。
     * 若存在则不应从 sys_role_permission 中删除（因添加时禁止重复，直接分配与权限组不会重叠）。
     */
    private boolean hasPermissionFromOtherSource(Long roleId, Long permissionId) {
        List<SysPermissionGroup> groups = sysPermissionGroupMapper.selectGroupsByRoleId(roleId);
        for (SysPermissionGroup g : groups) {
            if (g.getId() == null) continue;
            List<SysPermission> perms = sysPermissionMapper.selectPermissionsByGroupId(g.getId());
            if (perms.stream().anyMatch(p -> p.getId().equals(permissionId))) return true;
        }
        return false;
    }

    private PermissionResponseDTO toPermissionDTO(SysPermission p) {
        return getPermissionResponseDTO(p);
    }

    /**
     * 同步权限到角色
     *
     * @param roleIds      角色ID列表
     * @param permissionId 权限ID
     * @param isAdd        是否为添加操作（true为添加，false为删除）
     */
    private void syncPermissionToRoles(List<Long> roleIds, Long permissionId, boolean isAdd) {
        for (Long roleId : roleIds) {
            if (isAdd) {
                // 添加权限：只有当角色不拥有该权限时才添加
                long existCount;
                existCount = sysRolePermissionMapper.selectCount(
                        new LambdaQueryWrapper<SysRolePermission>()
                                .eq(SysRolePermission::getRoleId, roleId)
                                .eq(SysRolePermission::getPermissionId, permissionId));
                if (existCount == 0) {
                    SysRolePermission rp = new SysRolePermission();
                    rp.setRoleId(roleId);
                    rp.setPermissionId(permissionId);
                    sysRolePermissionMapper.insert(rp);
                }
            } else {
                // 删除权限：只有当角色不通过其他来源拥有该权限时才删除
                boolean hasFromOtherSource = hasPermissionFromOtherSource(roleId, permissionId);
                if (!hasFromOtherSource) {
                    sysRolePermissionMapper.delete(
                            new LambdaQueryWrapper<SysRolePermission>()
                                    .eq(SysRolePermission::getRoleId, roleId)
                                    .eq(SysRolePermission::getPermissionId, permissionId));
                }
            }
        }
    }

    /**
     * 验证权限组是否存在且不是系统权限组
     *
     * @param groupId 权限组ID
     * @return 验证失败返回错误结果，验证成功返回null
     */
    private SaResult validatePermissionGroup(Long groupId) {
        SysPermissionGroup group = sysPermissionGroupMapper.selectById(groupId);
        if (group == null || (group.getIsDeleted() != null && group.getIsDeleted() == 1)) {
            return SaResult.error("权限组不存在").setCode(404);
        }
        if (Boolean.TRUE.equals(group.getIsSystem())) {
            return SaResult.error("系统内置权限组不可修改").setCode(403);
        }
        return null;
    }

    private PermissionGroupResponseDTO toResponseDTO(SysPermissionGroup group) {
        PermissionGroupResponseDTO dto;
        dto = new PermissionGroupResponseDTO();
        return getPermissionGroupResponseDTO(group, dto);
    }

    @NonNull
    public static PermissionGroupResponseDTO getPermissionGroupResponseDTO(SysPermissionGroup group, PermissionGroupResponseDTO dto) {
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
