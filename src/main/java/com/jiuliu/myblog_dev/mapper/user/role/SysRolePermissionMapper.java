/*
 * [SysRolePermissionMapper.java]
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

package com.jiuliu.myblog_dev.mapper.user.role;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.entity.user.role.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 根据角色 ID 查询直接分配的权限列表
     *
     * @param roleId 角色 ID
     * @return 权限列表
     */
    @Select("SELECT p.* FROM sys_permission p " +
            "JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId} " +
            "ORDER BY p.sort_order DESC")
    List<SysPermission> selectPermissionsByRoleId(Long roleId);

    /**
     * 根据权限组 ID 查询权限列表
     *
     * @param groupId 权限组 ID
     * @return 权限列表
     */
    @Select("SELECT p.* FROM sys_permission p " +
            "JOIN sys_permission_group_item pgi ON p.id = pgi.permission_id " +
            "WHERE pgi.group_id = #{groupId} " +
            "ORDER BY pgi.sort_order DESC, p.sort_order DESC")
    List<SysPermission> selectPermissionsByGroupId(Long groupId);

    /**
     * 查询拥有指定权限的所有用户邮箱
     * 同时查询直接分配的权限和通过权限组获得的权限
     *
     * @param permissionCode 权限编码（如 "system:comment:list"）
     * @return 拥有该权限的用户邮箱列表
     */
    @Select("SELECT DISTINCT u.email FROM sys_user u " +
            "JOIN sys_user_role ur ON u.id = ur.user_id " +
            "WHERE (" +
            // 方式1: 直接分配的权限
            "  EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = ur.role_id " +
            "          AND EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = rp.permission_id AND p.code = #{permissionCode}))" +
            " OR " +
            // 方式2: 通过权限组获得的权限
            "  EXISTS (SELECT 1 FROM sys_role_permission_group rpg WHERE rpg.role_id = ur.role_id " +
            "          AND EXISTS (SELECT 1 FROM sys_permission_group_item pgi WHERE pgi.group_id = rpg.group_id " +
            "                     AND EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = pgi.permission_id AND p.code = #{permissionCode})))" +
            ") " +
            "AND u.email IS NOT NULL " +
            "AND u.email != '' " +
            "AND u.status = 1")
    List<String> selectUserEmailsByPermissionCode(String permissionCode);
}
