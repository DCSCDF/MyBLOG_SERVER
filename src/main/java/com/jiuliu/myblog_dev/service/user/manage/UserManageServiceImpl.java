/*
 * [UserManageServiceImpl.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * license: "MIT"
 */

package com.jiuliu.myblog_dev.service.user.manage;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuliu.myblog_dev.dto.user.manage.PageUserDTO;
import com.jiuliu.myblog_dev.dto.user.manage.PageUserResponseDTO;
import com.jiuliu.myblog_dev.dto.user.manage.UserAdminResponseDTO;
import com.jiuliu.myblog_dev.dto.user.manage.UserUpdateDTO;
import com.jiuliu.myblog_dev.dto.user.role.RoleResponseDTO;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.entity.user.SysUserRole;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserManageServiceImpl implements UserManageService {

    private static final Logger log = LoggerFactory.getLogger(UserManageServiceImpl.class);

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    public UserManageServiceImpl(SysUserMapper sysUserMapper,
                                 SysUserRoleMapper sysUserRoleMapper,
                                 SysRoleMapper sysRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    public SaResult getPageUsers(PageUserDTO pageDto) {
        try {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getIsDeleted, 0)
                    .eq(pageDto.getStatus() != null, SysUser::getStatus, pageDto.getStatus())
                    .orderByDesc(SysUser::getCreateTime);

            if (StringUtils.hasText(pageDto.getKeyword())) {
                String kw = pageDto.getKeyword().trim();
                wrapper.and(w -> w.like(SysUser::getUsername, kw)
                        .or().like(SysUser::getNickname, kw)
                        .or().like(SysUser::getEmail, kw));
            }

            Page<SysUser> page = new Page<>(pageDto.getCurrentPage(), pageDto.getPageSize());
            Page<SysUser> pageResult = sysUserMapper.selectPage(page, wrapper);

            List<UserAdminResponseDTO> records = pageResult.getRecords().stream()
                    .map(this::toUserAdminResponseDTO)
                    .collect(Collectors.toList());

            PageUserResponseDTO resp = new PageUserResponseDTO();
            resp.setRecords(records);
            resp.setTotal(pageResult.getTotal());
            resp.setSize(pageResult.getSize());
            resp.setCurrent(pageResult.getCurrent());
            resp.setPages(pageResult.getPages());
            return SaResult.data(resp);
        } catch (Exception e) {
            log.error("分页获取用户列表异常", e);
            return SaResult.error("获取用户列表失败").setCode(500);
        }
    }

    @Override
    public SaResult getUserById(Long id) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .eq(SysUser::getIsDeleted, 0));
        if (user == null) {
            return SaResult.error("用户不存在").setCode(404);
        }
        return SaResult.data(toUserAdminResponseDTO(user));
    }

    @Override
    public SaResult getUserRoles(Long userId) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .eq(SysUser::getIsDeleted, 0));
        if (user == null) {
            return SaResult.error("用户不存在").setCode(404);
        }

        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(userId);
        List<RoleResponseDTO> dtos = roles.stream().map(this::toRoleResponseDTO).collect(Collectors.toList());
        return SaResult.data(dtos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult updateUser(Long id, UserUpdateDTO dto) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .eq(SysUser::getIsDeleted, 0));
        if (user == null) {
            return SaResult.error("用户不存在").setCode(404);
        }

        // 角色更新：传入 roleId 则覆盖用户现有角色（单一角色）
        if (dto.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getId, dto.getRoleId())
                    .eq(SysRole::getIsDeleted, 0)
                    .eq(SysRole::getStatus, 1));
            if (role == null) {
                return SaResult.error("角色不存在或已禁用").setCode(404);
            }
            sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(id);
            userRole.setRoleId(dto.getRoleId());
            sysUserRoleMapper.insert(userRole);
        }

        // 用户字段更新
        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .eq(SysUser::getIsDeleted, 0)
                .set(StringUtils.hasText(dto.getNickname()), SysUser::getNickname, dto.getNickname())
                .set(dto.getAvatarUrl() != null, SysUser::getAvatarUrl, dto.getAvatarUrl())
                .set(SysUser::getUpdateTime, LocalDateTime.now());

        sysUserMapper.update(null, updateWrapper);

        return getUserById(id);
    }

    @Override
    public SaResult updateUserStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return SaResult.error("status 参数错误").setCode(400);
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .eq(SysUser::getIsDeleted, 0));
        if (user == null) {
            return SaResult.error("用户不存在").setCode(404);
        }

        // 避免禁用超级管理员账号
        if (status == 0 && isSuperAdminUser(id)) {
            return SaResult.error("超级管理员账号不可禁用").setCode(403);
        }

        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .eq(SysUser::getIsDeleted, 0)
                .set(SysUser::getStatus, status)
                .set(SysUser::getUpdateTime, LocalDateTime.now()));

        // 禁用后强制下线
        if (status == 0) {
            StpUtil.logout(id);
        }

        return SaResult.data("更新成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaResult deleteUser(Long id) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .eq(SysUser::getIsDeleted, 0));
        if (user == null) {
            return SaResult.error("用户不存在").setCode(404);
        }

        if (isSuperAdminUser(id)) {
            return SaResult.error("超级管理员账号不可删除").setCode(403);
        }

        // 删除用户-角色关联
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));

        // 逻辑删除用户：用户名追加后缀释放唯一约束，email 置空释放唯一约束
        String username = user.getUsername() == null ? ("user_" + id) : user.getUsername();
        String suffix = "(已删除)_" + id;
        String newUsername = username;
        if (!username.contains("(已删除)")) {
            newUsername = (username.length() + suffix.length() <= 50)
                    ? username + suffix
                    : username.substring(0, 50 - suffix.length()) + suffix;
        }

        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .eq(SysUser::getIsDeleted, 0)
                .set(SysUser::getUsername, newUsername)
                .set(SysUser::getEmail, null)
                .set(SysUser::getStatus, 0)
                .set(SysUser::getIsDeleted, 1)
                .set(SysUser::getUpdateTime, LocalDateTime.now()));

        // 删除后强制下线
        StpUtil.logout(id);
        return SaResult.data("删除成功");
    }

    private boolean isSuperAdminUser(Long userId) {
        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(userId);
        return roles.stream().anyMatch(r ->
                Boolean.TRUE.equals(r.getSuperAdmin()) || "SUPER_ADMIN".equals(r.getCode()));
    }

    private UserAdminResponseDTO toUserAdminResponseDTO(SysUser user) {
        UserAdminResponseDTO dto = new UserAdminResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());
        return dto;
    }

    private RoleResponseDTO toRoleResponseDTO(SysRole role) {
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

