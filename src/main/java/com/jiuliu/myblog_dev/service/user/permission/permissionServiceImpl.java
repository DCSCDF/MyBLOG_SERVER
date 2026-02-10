/*
 * [permissionServiceImpl.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/24 23:24
 */

package com.jiuliu.myblog_dev.service.user.permission;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class permissionServiceImpl implements permissionService {

    private static final Logger log = LoggerFactory.getLogger(permissionServiceImpl.class);

    private final SysPermissionMapper sysPermissionMapper;

    // 构造函数注入
    public permissionServiceImpl(SysPermissionMapper sysPermissionMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
    }
    @Override
    public SaResult getAllPermissions() {
        log.info("获取所有权限列表");

        try {
            // 使用MyBatis-Plus的QueryWrapper按sort_order排序查询所有权限
            QueryWrapper<SysPermission> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByAsc("sort_order");

            List<SysPermission> permissions = sysPermissionMapper.selectList(queryWrapper);

            log.info("成功获取权限列表，共{}条记录", permissions.size());
            return SaResult.data(permissions);
        } catch (Exception e) {
            log.error("获取权限列表异常", e);
            return SaResult.error("获取权限列表失败").setCode(500);
        }
    }
}
