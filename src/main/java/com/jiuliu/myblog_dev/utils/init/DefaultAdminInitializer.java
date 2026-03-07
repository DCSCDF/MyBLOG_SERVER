/*
 * [DefaultAdminInitializer.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/8 04:37
 */

package com.jiuliu.myblog_dev.utils.init;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuliu.myblog_dev.entity.user.SysUser;
import com.jiuliu.myblog_dev.entity.user.SysUserRole;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.mapper.user.SysUserMapper;
import com.jiuliu.myblog_dev.mapper.user.SysUserRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Order(2)  // 设置较低的优先级，确保在数据库初始化之后运行
public class DefaultAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminInitializer.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    // 构造器注入
    public DefaultAdminInitializer(SysUserMapper sysUserMapper,
                                   SysUserRoleMapper sysUserRoleMapper,
                                   SysRoleMapper sysRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 延迟执行，等待数据库初始化完成
        try {
            Thread.sleep(1000);  // 等待1秒，确保数据库初始化完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 检查数据库表是否存在
        if (!isDatabaseInitialized()) {
            log.error("数据库表未初始化，跳过管理员创建");
            return;
        }

        // 检查是否已经存在管理员
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", DEFAULT_ADMIN_USERNAME);
        SysUser adminUser = sysUserMapper.selectOne(queryWrapper);

        if (adminUser == null) {
            // 创建管理员
            SysUser sysUser = new SysUser();
            sysUser.setUsername("admin");
            sysUser.setNickname("管理员");
            sysUser.setPassword(passwordEncoder.encode("a123456"));
            sysUser.setEmail("admin@example.com");
            sysUser.setStatus(1); // 1表示启用
            sysUser.setCreateTime(LocalDateTime.now());
            sysUser.setUpdateTime(LocalDateTime.now());

            sysUserMapper.insert(sysUser);
            log.info("默认管理员已创建: username: admin (密码: a123456, 请首次登录后修改)");

            // 为管理员分配超级管理员角色
            assignSuperAdminRole(sysUser.getId());
        } else {
            log.info("管理员已存在，跳过初始化");

            // 检查管理员是否已关联超级管理员角色
            if (!hasSuperAdminRole(adminUser.getId())) {
                log.info("管理员未关联超级管理员角色，正在分配...");
                assignSuperAdminRole(adminUser.getId());
            }
        }
    }

    /**
     * 默认管理员用户名（唯一可拥有超级管理员角色的账号）
     */
    private static final String DEFAULT_ADMIN_USERNAME = "admin";

    /**
     * 为用户分配超级管理员角色。
     * 超级管理员只能有一个：先移除其他用户对该角色的关联，再为当前用户分配。
     */
    private void assignSuperAdminRole(Long userId) {
        try {
            // 查询 超级管理员角色的ID
            QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
            roleQuery.eq("code", "SUPER_ADMIN");
            SysRole superAdminRole = sysRoleMapper.selectOne(roleQuery);

            if (superAdminRole == null) {
                log.error("超级管理员角色不存在，请确保数据库初始化完成");
                return;
            }

            // 超级管理员只能有一个：移除其他用户对该角色的关联
            QueryWrapper<SysUserRole> removeOthers = new QueryWrapper<>();
            removeOthers.eq("role_id", superAdminRole.getId()).ne("user_id", userId);
            int removed = sysUserRoleMapper.delete(removeOthers);
            if (removed > 0) {
                log.info("已从其他 {} 个用户移除超级管理员角色，保证仅默认管理员拥有", removed);
            }

            // 检查是否已存在关联
            QueryWrapper<SysUserRole> userRoleQuery = new QueryWrapper<>();
            userRoleQuery.eq("user_id", userId)
                    .eq("role_id", superAdminRole.getId());
            SysUserRole existingUserRole = sysUserRoleMapper.selectOne(userRoleQuery);

            if (existingUserRole != null) {
                log.info("用户已关联超级管理员角色，无需重复分配");
                return;
            }

            // 创建用户-角色关联
            SysUserRole sysUserRole = new SysUserRole();
            sysUserRole.setUserId(userId);
            sysUserRole.setRoleId(superAdminRole.getId());
            sysUserRole.setCreateTime(LocalDateTime.now());

            sysUserRoleMapper.insert(sysUserRole);
            log.info("成功为用户分配超级管理员角色");

        } catch (Exception e) {
            log.error("分配超级管理员角色失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查用户是否已关联超级管理员角色
     */
    private boolean hasSuperAdminRole(Long userId) {
        try {
            // 查询 超级管理员角色的ID
            QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
            roleQuery.eq("code", "SUPER_ADMIN");
            SysRole superAdminRole = sysRoleMapper.selectOne(roleQuery);

            if (superAdminRole == null) {
                return false;
            }

            // 检查关联
            QueryWrapper<SysUserRole> userRoleQuery = new QueryWrapper<>();
            userRoleQuery.eq("user_id", userId)
                    .eq("role_id", superAdminRole.getId());
            SysUserRole userRole = sysUserRoleMapper.selectOne(userRoleQuery);

            return userRole != null;

        } catch (Exception e) {
            log.error("检查用户角色失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查数据库是否已初始化
     */
    private boolean isDatabaseInitialized() {
        try {
            // 尝试查询sys_user表，如果表不存在会抛出异常
            QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
            queryWrapper.last("LIMIT 1");
            sysUserMapper.selectOne(queryWrapper);
            return true;
        } catch (Exception e) {
            log.warn("数据库表尚未初始化: {}", e.getMessage());
            return false;
        }
    }
}