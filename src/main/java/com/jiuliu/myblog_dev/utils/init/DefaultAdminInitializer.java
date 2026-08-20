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

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Component
@Order(2)  // 设置较低的优先级，确保在数据库初始化之后运行
public class DefaultAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminInitializer.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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
        // 依赖 @Order(1)/@Order(2) 保证 DatabaseInitializer 先执行，无需 sleep hack

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
            // 创建管理员：密码优先取环境变量 DEFAULT_ADMIN_PASSWORD，未配置时随机生成并仅打印一次
            String initialPassword = resolveInitialPassword();

            SysUser sysUser = new SysUser();
            sysUser.setUsername("admin");
            sysUser.setNickname("管理员");
            sysUser.setPassword(passwordEncoder.encode(initialPassword));
            sysUser.setEmail("admin@example.com");
            sysUser.setStatus(1); // 1表示启用
            sysUser.setCreateTime(LocalDateTime.now());
            sysUser.setUpdateTime(LocalDateTime.now());

            sysUserMapper.insert(sysUser);
            if (initialPasswordFromEnv) {
                log.info("默认管理员已创建: username: admin（密码来自环境变量 DEFAULT_ADMIN_PASSWORD，请妥善保管）");
            } else {
                log.info("默认管理员已创建: username: admin（未配置 DEFAULT_ADMIN_PASSWORD，本次随机生成的初始密码为 [{}]，请立即登录后修改）", initialPassword);
            }

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
     * 是否从环境变量读取了初始密码（决定日志中是否打印随机密码）
     */
    private boolean initialPasswordFromEnv = false;

    /**
     * 解析初始密码：
     * 1. 环境变量 DEFAULT_ADMIN_PASSWORD（或系统属性 app.init.admin-password）
     * 2. 未配置时随机生成 12 位强密码（仅在创建时打印一次，提示立即修改）
     */
    private String resolveInitialPassword() {
        String envPassword = System.getenv("DEFAULT_ADMIN_PASSWORD");
        if (envPassword == null || envPassword.isBlank()) {
            envPassword = System.getProperty("app.init.admin-password");
        }
        if (envPassword != null && !envPassword.isBlank()) {
            initialPasswordFromEnv = true;
            return envPassword;
        }
        initialPasswordFromEnv = false;
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
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