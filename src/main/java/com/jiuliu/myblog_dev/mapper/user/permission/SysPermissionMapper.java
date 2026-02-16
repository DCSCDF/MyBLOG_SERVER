/*
 * [SysPermissionMapper.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/18 15:14
 */

package com.jiuliu.myblog_dev.mapper.user.permission;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据角色 ID 查询权限列表
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
}