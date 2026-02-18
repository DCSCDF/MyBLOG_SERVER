/*
 * [SysPermissionGroupMapper.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/14 04:54
 */

package com.jiuliu.myblog_dev.mapper.user.permissionGroup;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionGroupMapper extends BaseMapper<SysPermissionGroup> {

    /**
     * 根据角色 ID 查询权限组列表
     *
     * @param roleId 角色 ID
     * @return 权限组列表
     */
    @Select("SELECT g.* FROM sys_permission_group g " +
            "JOIN sys_role_permission_group rpg ON g.id = rpg.group_id " +
            "WHERE rpg.role_id = #{roleId} AND (g.is_deleted = 0 OR g.is_deleted IS NULL) " +
            "AND (g.status = 1 OR g.status IS NULL) " +
            "ORDER BY g.sort_order DESC")
    List<SysPermissionGroup> selectGroupsByRoleId(Long roleId);
}