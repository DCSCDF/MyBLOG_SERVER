/*
 * [RoleServiceImpl.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/2/18 11:52
 */

/*
 * [RoleServiceImpl.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.service.user.role;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuliu.myblog_dev.dto.common.FilterOptionItem;
import com.jiuliu.myblog_dev.dto.user.permission.PermissionResponseDTO;
import com.jiuliu.myblog_dev.dto.user.permissiongroup.PermissionGroupResponseDTO;
import com.jiuliu.myblog_dev.dto.user.role.*;
import com.jiuliu.myblog_dev.entity.user.SysUserRole;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroup;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.entity.user.role.SysRolePermission;
import com.jiuliu.myblog_dev.entity.user.role.SysRolePermissionGroup;
import com.jiuliu.myblog_dev.exception.BusinessException;
import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionMapper;
import com.jiuliu.myblog_dev.utils.PermissionOverlapHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jiuliu.myblog_dev.service.user.permission.PermissionServiceImpl.getPermissionResponseDTO;
import static com.jiuliu.myblog_dev.service.user.permissiongroup.PermissionGroupServiceImpl.getPermissionGroupResponseDTO;

@Service
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);
    private static final String CONFIG_KEY_REGISTER_DEFAULT_ROLE = "user_register_default_role";

    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysRolePermissionGroupMapper sysRolePermissionGroupMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysPermissionGroupMapper sysPermissionGroupMapper;
    private final SysConfigMapper sysConfigMapper;

    public RoleServiceImpl(SysRoleMapper sysRoleMapper,
                           SysRolePermissionMapper sysRolePermissionMapper,
                           SysRolePermissionGroupMapper sysRolePermissionGroupMapper,
                           SysUserRoleMapper sysUserRoleMapper,
                           SysPermissionMapper sysPermissionMapper,
                           SysPermissionGroupMapper sysPermissionGroupMapper,
                           SysConfigMapper sysConfigMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysRolePermissionGroupMapper = sysRolePermissionGroupMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysPermissionGroupMapper = sysPermissionGroupMapper;
        this.sysConfigMapper = sysConfigMapper;
    }

    @Override
    public SaResult getPageRoles(PageRoleDTO pageDto) {
        try {
            LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getIsDeleted, 0)
                    .eq(pageDto.getStatus() != null, SysRole::getStatus, pageDto.getStatus())
                    .eq(pageDto.getIsSystem() != null, SysRole::getIsSystem, pageDto.getIsSystem() != null && pageDto.getIsSystem() == 1)
                    .orderByDesc(SysRole::getSortOrder);

            if (StringUtils.hasText(pageDto.getKeyword())) {
                String kw = pageDto.getKeyword().trim();
                wrapper.and(w -> w.like(SysRole::getCode, kw)
                        .or().like(SysRole::getName, kw)
                        .or().like(SysRole::getDescription, kw));
            }

            Page<SysRole> page = new Page<>(pageDto.getCurrentPage(), pageDto.getPageSize());
            Page<SysRole> pageResult = sysRoleMapper.selectPage(page, wrapper);

            List<RoleResponseDTO> dtos = pageResult.getRecords().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());

            PageRoleResponseDTO response = new PageRoleResponseDTO();
            response.setRecords(dtos);
            response.setTotal(pageResult.getTotal());
            response.setSize(pageResult.getSize());
            response.setCurrent(pageResult.getCurrent());
            response.setPages(pageResult.getPages());
            response.setFilterOptions(buildRoleListFilterOptions());

            return SaResult.data(response);
        } catch (Exception e) {
            log.error("分页获取角色列表异常", e);
            return SaResult.error("获取角色列表失败").setCode(500);
        }
    }

    private static Map<String, List<FilterOptionItem>> buildRoleListFilterOptions() {
        return Map.of(
                "status", List.of(new FilterOptionItem(0, "禁用"), new FilterOptionItem(1, "启用")),
                "isSystem", List.of(new FilterOptionItem(0, "否"), new FilterOptionItem(1, "是")));
    }

    @Override
    public SaResult getRoleById(Long id) {
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getId, id)
                        .eq(SysRole::getIsDeleted, 0));
        if (role == null) {
            log.warn("获取角色详情失败：角色不存在，id={}", id);
            return SaResult.error("角色不存在").setCode(404);
        }
        return SaResult.data(toResponseDTO(role));
    }

    @Override
    public SaResult createRole(RoleCreateDTO dto) {
        // 检查角色编码是否已存在（含逻辑删除记录，因数据库 code 唯一约束对全表生效）
        SysRole existingRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, dto.getCode()));
        if (existingRole != null) {
            log.warn("创建角色失败：角色编码已存在，code={}", dto.getCode());
            return SaResult.error("角色编码已存在").setCode(400);
        }

        SysRole role = new SysRole();
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        role.setSuperAdmin(false);
        role.setIsSystem(false);
        role.setIsDeleted(0);

        sysRoleMapper.insert(role);
        log.info("角色创建成功，id={}, code={}", role.getId(), role.getCode());
        return SaResult.data(toResponseDTO(role));
    }

    @Override
    public SaResult updateRole(RoleUpdateDTO dto) {
        SysRole role = sysRoleMapper.selectById(dto.getId());
        if (role == null) {
            log.warn("更新角色失败：角色不存在，id={}", dto.getId());
            return SaResult.error("角色不存在").setCode(404);
        }
        if (Boolean.TRUE.equals(role.getSuperAdmin()) || "SUPER_ADMIN".equals(role.getCode())) {
            log.warn("更新角色失败：超级管理员角色不可修改，id={}", dto.getId());
            return SaResult.error("超级管理员角色不可修改").setCode(403);
        }
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            log.warn("更新角色失败：系统内置角色不可修改，id={}", dto.getId());
            return SaResult.error("系统内置角色不可修改").setCode(403);
        }

        if (dto.getStatus() != null && dto.getStatus() == 0) {
            long userCount = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, dto.getId()));
            if (userCount > 0) {
                log.warn("更新角色失败：该角色正在被用户使用无法禁用，roleId={}, 关联用户数={}", dto.getId(), userCount);
                return SaResult.error("该角色正在被用户使用，无法禁用。请先解除用户与该角色的关联").setCode(403);
            }
        }

        LambdaUpdateWrapper<SysRole> wrapper = new LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getId, dto.getId())
                .set(SysRole::getName, dto.getName())
                .set(dto.getDescription() != null, SysRole::getDescription, dto.getDescription())
                .set(dto.getSortOrder() != null, SysRole::getSortOrder, dto.getSortOrder())
                .set(dto.getStatus() != null, SysRole::getStatus, dto.getStatus())
                .set(SysRole::getUpdateTime, LocalDateTime.now());

        sysRoleMapper.update(null, wrapper);
        log.info("角色更新成功，id={}", dto.getId());
        return SaResult.data(toResponseDTO(sysRoleMapper.selectById(dto.getId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult deleteRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            log.warn("删除角色失败：角色不存在，id={}", id);
            return SaResult.error("角色不存在").setCode(404);
        }
        if (Boolean.TRUE.equals(role.getSuperAdmin()) || "SUPER_ADMIN".equals(role.getCode())) {
            log.warn("删除角色失败：超级管理员角色不可删除，id={}", id);
            return SaResult.error("超级管理员角色不可删除").setCode(403);
        }
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            log.warn("删除角色失败：系统内置角色不可删除，id={}", id);
            return SaResult.error("系统内置角色不可删除").setCode(403);
        }

        String defaultRoleCode = sysConfigMapper.selectValueByKey(CONFIG_KEY_REGISTER_DEFAULT_ROLE);
        if (StringUtils.hasText(defaultRoleCode) && defaultRoleCode.equals(role.getCode())) {
            log.warn("删除角色失败：该角色已设为注册默认角色不可删除，id={}, code={}", id, role.getCode());
            return SaResult.error("该角色已设为用户注册默认角色，不可删除。请先在系统配置中修改 user_register_default_role").setCode(403);
        }

        long userCount = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        if (userCount > 0) {
            log.warn("删除角色失败：该角色正在被用户使用，id={}, 关联用户数={}", id, userCount);
            return SaResult.error("该角色正在被用户使用，无法删除。请先解除用户与该角色的关联").setCode(403);
        }

        // 1. 删除用户-角色关联
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        // 2. 删除角色-权限关联
        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        // 3. 删除角色-权限组关联
        sysRolePermissionGroupMapper.delete(new LambdaQueryWrapper<SysRolePermissionGroup>().eq(SysRolePermissionGroup::getRoleId, id));
        // 4. 逻辑删除角色：code 追加「(已删除)_id」后缀，释放唯一约束以便复用编码
        String newCode = role.getCode();
        if (newCode != null && !newCode.contains("(已删除)")) {
            String suffix = "(已删除)_" + id;
            newCode = (newCode.length() + suffix.length() <= 50) ? newCode + suffix : newCode.substring(0, 50 - suffix.length()) + suffix;
        }
        sysRoleMapper.update(null, new LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getId, id)
                .set(SysRole::getCode, newCode)
                .set(SysRole::getIsDeleted, 1)
                .set(SysRole::getUpdateTime, LocalDateTime.now()));
        log.info("角色删除成功，已级联删除关联数据，id={}", id);
        return SaResult.data("删除成功");
    }

    @Override
    public SaResult getRolePermissionsDetail(Long roleId) {
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getId, roleId)
                        .and(w -> w.eq(SysRole::getIsDeleted, 0).or().isNull(SysRole::getIsDeleted)));
        if (role == null) {
            log.warn("获取角色权限详情失败：角色不存在，roleId={}", roleId);
            return SaResult.error("角色不存在").setCode(404);
        }

        List<SysPermission> permissions = sysPermissionMapper.selectPermissionsByRoleId(roleId);
        List<SysPermissionGroup> groups = sysPermissionGroupMapper.selectGroupsByRoleId(roleId);

        RolePermissionsDetailDTO detail = new RolePermissionsDetailDTO();
        detail.setRole(toResponseDTO(role));
        detail.setPermissions(permissions.stream().map(this::toPermissionDTO).collect(Collectors.toList()));
        detail.setPermissionGroups(groups.stream().map(this::toPermissionGroupDTO).collect(Collectors.toList()));
        return SaResult.data(detail);
    }

    @Override
    public SaResult addPermissionToRole(Long roleId, Long permissionId) {
        validateRoleForModification(roleId);
        SysPermission newPerm = sysPermissionMapper.selectById(permissionId);
        if (newPerm == null) {
            log.warn("角色添加权限失败：权限不存在，roleId={}, permissionId={}", roleId, permissionId);
            return SaResult.error("权限不存在").setCode(404);
        }

        // 父子权限重叠校验：与角色已有权限（直接分配+权限组）任一重叠则不通过
        List<String> rolePermissionCodes = getRoleAllPermissionCodes(roleId);
        for (String existingCode : rolePermissionCodes) {
            if (PermissionOverlapHelper.overlaps(newPerm.getCode(), existingCode)) {
                return SaResult.error("该权限与角色已有权限重叠（存在父子关系或重复），请勿重复添加").setCode(400);
            }
        }

        SysRolePermission rp = new SysRolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        sysRolePermissionMapper.insert(rp);
        log.info("角色添加权限成功，roleId={}, permissionId={}", roleId, permissionId);
        return SaResult.data("添加成功");
    }

    @Override
    public SaResult removePermissionFromRole(Long roleId, Long permissionId) {
        validateRoleForModification(roleId);

        int deleted = sysRolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId)
                        .eq(SysRolePermission::getPermissionId, permissionId));
        if (deleted == 0) {
            log.warn("角色移除权限失败：该权限未分配给角色，roleId={}, permissionId={}", roleId, permissionId);
            return SaResult.error("该权限未分配给角色").setCode(400);
        }
        log.info("角色移除权限成功，roleId={}, permissionId={}", roleId, permissionId);
        return SaResult.data("移除成功");
    }

    @Override
    public SaResult addPermissionGroupToRole(Long roleId, Long groupId) {
        validateRoleForModification(roleId);
        SysPermissionGroup group = sysPermissionGroupMapper.selectById(groupId);
        if (group == null || (group.getIsDeleted() != null && group.getIsDeleted() == 1)) {
            log.warn("角色添加权限组失败：权限组不存在，roleId={}, groupId={}", roleId, groupId);
            return SaResult.error("权限组不存在").setCode(404);
        }
        if (group.getStatus() != null && group.getStatus() == 0) {
            log.warn("角色添加权限组失败：禁用的权限组无法添加，roleId={}, groupId={}", roleId, groupId);
            return SaResult.error("禁用的权限组无法添加到角色").setCode(400);
        }

        long count = sysRolePermissionGroupMapper.selectCount(
                new LambdaQueryWrapper<SysRolePermissionGroup>()
                        .eq(SysRolePermissionGroup::getRoleId, roleId)
                        .eq(SysRolePermissionGroup::getGroupId, groupId));
        if (count > 0) {
            log.warn("角色添加权限组失败：该权限组已分配给角色，roleId={}, groupId={}", roleId, groupId);
            return SaResult.error("该权限组已分配给角色").setCode(400);
        }

        // 父子权限重叠校验：权限组中的权限与角色已有权限任一重叠则不通过
        List<String> rolePermissionCodes = getRoleAllPermissionCodes(roleId);
        List<SysPermission> groupPermissions = sysPermissionMapper.selectPermissionsByGroupId(groupId);
        for (SysPermission gp : groupPermissions) {
            for (String existingCode : rolePermissionCodes) {
                if (PermissionOverlapHelper.overlaps(gp.getCode(), existingCode)) {
                    return SaResult.error("权限组中的权限「" + gp.getCode() + "」与角色已有权限重叠（存在父子关系或重复），请勿重复添加").setCode(400);
                }
            }
        }

        SysRolePermissionGroup rpg = new SysRolePermissionGroup();
        rpg.setRoleId(roleId);
        rpg.setGroupId(groupId);
        sysRolePermissionGroupMapper.insert(rpg);

        // 同步权限组中的权限到角色权限表，供 Sa-Token 鉴权使用
        List<Long> permissionIds = sysPermissionMapper.selectPermissionsByGroupId(groupId).stream()
                .map(SysPermission::getId)
                .toList();
        for (Long pid : permissionIds) {
            long existCount = sysRolePermissionMapper.selectCount(
                    new LambdaQueryWrapper<SysRolePermission>()
                            .eq(SysRolePermission::getRoleId, roleId)
                            .eq(SysRolePermission::getPermissionId, pid));
            if (existCount == 0) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                sysRolePermissionMapper.insert(rp);
            }
        }
        log.info("角色添加权限组成功，roleId={}, groupId={}", roleId, groupId);
        return SaResult.data("添加成功");
    }

    @Override
    public SaResult removePermissionGroupFromRole(Long roleId, Long groupId) {
        validateRoleForModification(roleId);

        int deleted = sysRolePermissionGroupMapper.delete(
                new LambdaQueryWrapper<SysRolePermissionGroup>()
                        .eq(SysRolePermissionGroup::getRoleId, roleId)
                        .eq(SysRolePermissionGroup::getGroupId, groupId));
        if (deleted == 0) {
            log.warn("角色移除权限组失败：该权限组未分配给角色，roleId={}, groupId={}", roleId, groupId);
            return SaResult.error("该权限组未分配给角色").setCode(400);
        }

        // 从角色权限表中移除该权限组包含的权限（这些权限仅通过该组获得，直接分配的权限在删除组时保留）
        // 注意：若某权限同时通过直接分配获得，此处删除会一并移除，需重新添加
        List<Long> permissionIds = sysPermissionMapper.selectPermissionsByGroupId(groupId).stream()
                .map(SysPermission::getId)
                .toList();
        for (Long pid : permissionIds) {
            sysRolePermissionMapper.delete(
                    new LambdaQueryWrapper<SysRolePermission>()
                            .eq(SysRolePermission::getRoleId, roleId)
                            .eq(SysRolePermission::getPermissionId, pid));
        }
        log.info("角色移除权限组成功，roleId={}, groupId={}", roleId, groupId);
        return SaResult.data("移除成功");
    }

    private PermissionResponseDTO toPermissionDTO(SysPermission p) {
        return getPermissionResponseDTO(p);
    }

    private PermissionGroupResponseDTO toPermissionGroupDTO(SysPermissionGroup g) {
        PermissionGroupResponseDTO dto = new PermissionGroupResponseDTO();
        return getPermissionGroupResponseDTO(g, dto);
    }

    /**
     * 获取角色拥有的所有权限编码（直接分配的权限 + 权限组中的权限）
     */
    private List<String> getRoleAllPermissionCodes(Long roleId) {
        List<SysPermission> directPerms = sysPermissionMapper.selectPermissionsByRoleId(roleId);
        List<String> codes = directPerms.stream().map(SysPermission::getCode).collect(Collectors.toList());
        List<SysPermissionGroup> groups = sysPermissionGroupMapper.selectGroupsByRoleId(roleId);
        for (SysPermissionGroup g : groups) {
            List<SysPermission> groupPerms = sysPermissionMapper.selectPermissionsByGroupId(g.getId());
            for (SysPermission p : groupPerms) {
                if (!codes.contains(p.getCode())) {
                    codes.add(p.getCode());
                }
            }
        }
        return codes;
    }

    /**
     * 验证角色是否存在且可修改
     *
     * @param roleId 角色ID
     */
    private void validateRoleForModification(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null || (role.getIsDeleted() != null && role.getIsDeleted() == 1)) {
            throw new BusinessException("角色不存在", 404);
        }
        if (Boolean.TRUE.equals(role.getSuperAdmin()) || "SUPER_ADMIN".equals(role.getCode())) {
            throw new BusinessException("超级管理员角色不可修改", 403);
        }
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new BusinessException("系统内置角色不可修改", 403);
        }
    }

    private RoleResponseDTO toResponseDTO(SysRole role) {
        RoleResponseDTO dto = new RoleResponseDTO();
        dto.setId(role.getId());
        dto.setCode(role.getCode());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setSuperAdmin(role.getSuperAdmin());
        dto.setIsSystem(role.getIsSystem());
        dto.setSortOrder(role.getSortOrder());
        dto.setStatus(role.getStatus());
        dto.setCreateTime(role.getCreateTime());
        dto.setUpdateTime(role.getUpdateTime());
        return dto;
    }
}
