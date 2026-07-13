/*
 * [SysUserRoleMapper.java]
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

package com.jiuliu.myblog_dev.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuliu.myblog_dev.entity.user.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-角色关联表 Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @org.apache.ibatis.annotations.Insert("INSERT IGNORE INTO sys_user_role (user_id, role_id, create_time) VALUES (#{userId}, #{roleId}, NOW())")
    int insertIgnore(SysUserRole userRole);
}