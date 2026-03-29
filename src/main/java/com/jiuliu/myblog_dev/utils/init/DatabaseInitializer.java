/*
 * [DatabaseInitializer.java]
 * =======================================
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * =======================================
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/3/29 10:00
 */

package com.jiuliu.myblog_dev.utils.init;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuliu.myblog_dev.entity.config.SysConfig;
import com.jiuliu.myblog_dev.entity.seo.SysSeo;
import com.jiuliu.myblog_dev.entity.user.permission.SysPermission;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroup;
import com.jiuliu.myblog_dev.entity.user.permissiongroup.SysPermissionGroupItem;
import com.jiuliu.myblog_dev.entity.user.role.SysRole;
import com.jiuliu.myblog_dev.entity.user.role.SysRolePermission;
import com.jiuliu.myblog_dev.entity.user.role.SysRolePermissionGroup;
import com.jiuliu.myblog_dev.mapper.config.SysConfigMapper;
import com.jiuliu.myblog_dev.mapper.seo.SysSeoMapper;
import com.jiuliu.myblog_dev.mapper.user.permission.SysPermissionMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupItemMapper;
import com.jiuliu.myblog_dev.mapper.user.permissionGroup.SysPermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRoleMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionGroupMapper;
import com.jiuliu.myblog_dev.mapper.user.role.SysRolePermissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库初始化器
 * 负责在应用启动时初始化必要默认数据
 * 当使用非 root 用户连接数据库时，确保此用户拥有执行 INSERT/UPDATE 的权限
 */
@Component
@Order(1)
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysPermissionGroupMapper sysPermissionGroupMapper;
    private final SysPermissionGroupItemMapper sysPermissionGroupItemMapper;
    private final SysConfigMapper sysConfigMapper;
    private final SysSeoMapper sysSeoMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysRolePermissionGroupMapper sysRolePermissionGroupMapper;

    public DatabaseInitializer(SysRoleMapper sysRoleMapper,
                               SysPermissionMapper sysPermissionMapper,
                               SysPermissionGroupMapper sysPermissionGroupMapper,
                               SysPermissionGroupItemMapper sysPermissionGroupItemMapper,
                               SysConfigMapper sysConfigMapper,
                               SysSeoMapper sysSeoMapper,
                               SysRolePermissionMapper sysRolePermissionMapper,
                               SysRolePermissionGroupMapper sysRolePermissionGroupMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysPermissionGroupMapper = sysPermissionGroupMapper;
        this.sysPermissionGroupItemMapper = sysPermissionGroupItemMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.sysSeoMapper = sysSeoMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysRolePermissionGroupMapper = sysRolePermissionGroupMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========== 开始数据库初始化 ==========");

        try {
            // 检查数据库表是否存在
            if (!isDatabaseInitialized()) {
                log.error("数据库表未初始化！请确保：");
                log.error("1. schema.sql 已成功执行");
                log.error("2. 数据库用户有 CREATE TABLE 权限");
                log.error("3. 表已正确创建");
                return;
            }

            log.info("数据库连接正常，开始初始化默认数据...");

            // 初始化默认角色
            initDefaultRoles();

            // 初始化默认权限
            initDefaultPermissions();

            // 初始化默认权限组
            initDefaultPermissionGroups();

            // 初始化权限-权限组关联
            initPermissionGroupItems();

            // 初始化角色-权限组关联
            initRolePermissionGroupRelations();

            // 初始化角色-权限关联
            initRolePermissionRelations();

            // 初始化默认配置
            initDefaultConfigs();

            // 初始化默认SEO配置
            initDefaultSeoConfigs();

            log.info("========== 数据库初始化完成 ==========");

        } catch (Exception e) {
            log.error("========== 数据库初始化失败 ==========", e);
            log.error("可能的解决方案：");
            log.error("1. 确认数据库用户有足够的权限 (SELECT, INSERT, UPDATE, DELETE)");
            log.error("2. 检查数据库连接配置");
            log.error("3. 查看 schema.sql 是否已成功执行");
        }
    }

    private boolean isDatabaseInitialized() {
        try {
            QueryWrapper<SysRole> queryWrapper = new QueryWrapper<>();
            queryWrapper.last("LIMIT 1");
            sysRoleMapper.selectOne(queryWrapper);
            return true;
        } catch (Exception e) {
            log.warn("数据库表尚未初始化: {}", e.getMessage());
            return false;
        }
    }

    private void initDefaultRoles() {
        log.info("初始化默认角色...");

        // 超级管理员角色
        SysRole superAdminRole = new SysRole();
        superAdminRole.setCode("SUPER_ADMIN");
        superAdminRole.setName("超级管理员");
        superAdminRole.setDescription("拥有系统所有权限，只能有一个");
        superAdminRole.setSuperAdmin(true);
        superAdminRole.setIsSystem(true);
        superAdminRole.setSortOrder(100);
        superAdminRole.setStatus(1);
        insertOrUpdateRole(superAdminRole);

        // 普通管理员角色
        SysRole adminRole = new SysRole();
        adminRole.setCode("ADMIN");
        adminRole.setName("普通管理员");
        adminRole.setDescription("拥有系统大部分管理权限");
        adminRole.setSuperAdmin(false);
        adminRole.setIsSystem(true);
        adminRole.setSortOrder(90);
        adminRole.setStatus(1);
        insertOrUpdateRole(adminRole);

        // 文章作者角色
        SysRole authorRole = new SysRole();
        authorRole.setCode("AUTHOR");
        authorRole.setName("文章作者");
        authorRole.setDescription("可以发布和管理自己的文章");
        authorRole.setSuperAdmin(false);
        authorRole.setIsSystem(true);
        authorRole.setSortOrder(80);
        authorRole.setStatus(1);
        insertOrUpdateRole(authorRole);

        // 普通用户角色
        SysRole userRole = new SysRole();
        userRole.setCode("USER");
        userRole.setName("普通用户");
        userRole.setDescription("可以评论、点赞、收藏文章");
        userRole.setSuperAdmin(false);
        userRole.setIsSystem(true);
        userRole.setSortOrder(70);
        userRole.setStatus(1);
        insertOrUpdateRole(userRole);

        log.info("默认角色初始化完成");
    }

    private void insertOrUpdateRole(SysRole role) {
        try {
            QueryWrapper<SysRole> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("code", role.getCode());
            SysRole existingRole = sysRoleMapper.selectOne(queryWrapper);

            if (existingRole == null) {
                sysRoleMapper.insert(role);
                log.info("  创建角色: {}", role.getName());
            } else {
                role.setId(existingRole.getId());
                role.setIsDeleted(0);
                sysRoleMapper.updateById(role);
                log.info("  更新角色: {}", role.getName());
            }
        } catch (Exception e) {
            log.error("  初始化角色失败 [{}]: {}", role.getName(), e.getMessage());
        }
    }

    private void initDefaultPermissions() {
        log.info("初始化默认权限...");

        insertOrUpdatePermission("system", "系统管理", "系统管理菜单", 100);
        insertOrUpdatePermission("system:user", "用户管理", "用户管理", 101);
        insertOrUpdatePermission("system:role", "角色管理", "角色管理", 102);
        insertOrUpdatePermission("system:permission", "权限管理", "权限管理", 103);
        insertOrUpdatePermission("system:permission:permission_group", "权限组管理", "权限组管理", 104);

        // 用户管理 API 权限
        insertOrUpdatePermission("system:user:list", "用户列表", "查看用户列表", 1);
        insertOrUpdatePermission("system:user:create", "创建用户", "创建用户", 2);
        insertOrUpdatePermission("system:user:edit", "编辑用户", "编辑用户", 3);
        insertOrUpdatePermission("system:user:delete", "删除用户", "删除用户", 4);
        insertOrUpdatePermission("system:user:assignRole", "分配角色", "为用户分配角色", 5);

        // 角色管理 API 权限
        insertOrUpdatePermission("system:role:list", "角色列表", "查看角色列表", 1);
        insertOrUpdatePermission("system:role:create", "创建角色", "创建角色", 2);
        insertOrUpdatePermission("system:role:edit", "编辑角色", "编辑角色", 3);
        insertOrUpdatePermission("system:role:delete", "删除角色", "删除角色", 4);
        insertOrUpdatePermission("system:role:assignPermission", "分配权限", "为角色分配权限", 5);
        insertOrUpdatePermission("system:role:addPermission", "角色添加权限", "为角色添加权限", 6);
        insertOrUpdatePermission("system:role:removePermission", "角色移除权限", "从角色移除权限", 7);
        insertOrUpdatePermission("system:role:addPermissionGroup", "角色添加权限组", "为角色添加权限组", 8);
        insertOrUpdatePermission("system:role:removePermissionGroup", "角色移除权限组", "从角色移除权限组", 9);

        // 权限组管理 API 权限
        insertOrUpdatePermission("system:permission:permission_group:list", "权限组列表", "查看权限组列表", 1);
        insertOrUpdatePermission("system:permission:permission_group:create", "创建权限组", "创建权限组", 2);
        insertOrUpdatePermission("system:permission:permission_group:edit", "编辑权限组", "编辑权限组", 3);
        insertOrUpdatePermission("system:permission:permission_group:delete", "删除权限组", "删除权限组", 4);
        insertOrUpdatePermission("system:permission:permission_group:addPermission", "权限组添加权限", "为权限组添加权限", 4);
        insertOrUpdatePermission("system:permission:permission_group:removePermission", "权限组移除权限", "从权限组移除权限", 5);

        // OSS管理
        insertOrUpdatePermission("system:oss", "OSS管理", "OSS管理菜单", 60);
        insertOrUpdatePermission("system:oss:list", "OSS图片列表", "OSS图片列表", 1);
        insertOrUpdatePermission("system:oss:delete", "删除OSS的图片", "删除OSS的图片", 4);

        // 网站配置管理
        insertOrUpdatePermission("system:config", "网站配置", "网站配置菜单", 55);
        insertOrUpdatePermission("system:config:systemlist", "系统配置查询", "按 key 查询系统默认配置项", 1);
        insertOrUpdatePermission("system:config:customlist", "自定义配置列表", "分页查询用户自定义配置项", 2);
        insertOrUpdatePermission("system:config:create", "创建自定义配置", "添加用户自定义配置项", 3);
        insertOrUpdatePermission("system:config:edit", "修改配置", "修改网站配置项的值", 4);
        insertOrUpdatePermission("system:config:delete", "删除自定义配置", "删除非系统内置的配置项", 5);

        // SEO管理
        insertOrUpdatePermission("system:seo", "SEO管理", "SEO管理菜单", 54);
        insertOrUpdatePermission("system:seo:list", "SEO列表", "查看SEO列表", 1);
        insertOrUpdatePermission("system:seo:create", "创建SEO", "创建SEO配置", 2);
        insertOrUpdatePermission("system:seo:edit", "编辑SEO", "编辑SEO配置", 3);
        insertOrUpdatePermission("system:seo:delete", "删除SEO", "删除SEO配置", 4);

        // 全局文章、分类、评论等管理
        insertOrUpdatePermission("system:article", "全局文章管理", "全局文章管理菜单", 56);
        insertOrUpdatePermission("system:article:list", "全局文章列表", "查看全局文章列表", 56);
        insertOrUpdatePermission("system:article:delete", "全局删除文章", "全局删除文章", 56);
        insertOrUpdatePermission("system:article:edit", "全局编辑文章", "全局编辑文章", 56);

        insertOrUpdatePermission("system:comment", "全局评论管理", "全局评论管理菜单", 2);
        insertOrUpdatePermission("system:comment:list", "全局评论列表", "全局评论列表", 2);
        insertOrUpdatePermission("system:comment:delete", "全局删除评论", "全局删除评论", 2);
        insertOrUpdatePermission("system:comment:edit", "全局编辑评论", "全局编辑评论", 2);
        insertOrUpdatePermission("system:comment:approve", "全局审核评论", "全局审核评论", 5);

        // 文章管理
        insertOrUpdatePermission("article", "文章管理", "文章管理菜单", 90);
        insertOrUpdatePermission("article:list", "文章列表", "查看文章列表", 1);
        insertOrUpdatePermission("article:create", "创建文章", "创建文章", 2);
        insertOrUpdatePermission("article:edit", "编辑文章", "编辑文章", 3);
        insertOrUpdatePermission("article:delete", "删除文章", "删除文章", 4);

        // OSS管理
        insertOrUpdatePermission("oss", "OSS管理", "OSS管理菜单", 91);
        insertOrUpdatePermission("oss:list", "OSS列表", "查看OSS列表", 2);
        insertOrUpdatePermission("oss:create", "OSS上传", "OSS上传", 2);
        insertOrUpdatePermission("oss:delete", "OSS删除", "OSS删除", 4);

        // 分类管理
        insertOrUpdatePermission("category", "分类管理", "分类管理菜单", 80);
        insertOrUpdatePermission("category:list", "分类列表", "查看分类列表", 1);
        insertOrUpdatePermission("category:create", "创建分类", "创建分类", 2);
        insertOrUpdatePermission("category:edit", "编辑分类", "编辑分类", 3);
        insertOrUpdatePermission("category:delete", "删除分类", "删除分类", 4);

        // 评论管理
        insertOrUpdatePermission("comment", "评论管理", "评论管理菜单", 70);
        insertOrUpdatePermission("comment:list", "评论列表", "查看评论列表", 1);
        insertOrUpdatePermission("comment:create", "创建评论", "创建评论", 2);
        insertOrUpdatePermission("comment:edit", "编辑评论", "编辑评论", 3);
        insertOrUpdatePermission("comment:delete", "删除评论", "删除评论", 4);

        // 友情链接管理
        insertOrUpdatePermission("links", "友情链接管理", "友情链接管理菜单", 50);
        insertOrUpdatePermission("links:list", "友情链接列表", "查看友情链接列表", 1);
        insertOrUpdatePermission("links:create", "创建友情链接", "创建友情链接", 2);
        insertOrUpdatePermission("links:edit", "编辑友情链接", "编辑友情链接", 3);
        insertOrUpdatePermission("links:delete", "删除友情链接", "删除友情链接", 4);

        log.info("默认权限初始化完成");
    }

    private void insertOrUpdatePermission(String code, String name, String description, int sortOrder) {
        try {
            QueryWrapper<SysPermission> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("code", code);
            SysPermission existing = sysPermissionMapper.selectOne(queryWrapper);

            if (existing == null) {
                SysPermission permission = new SysPermission();
                permission.setCode(code);
                permission.setName(name);
                permission.setDescription(description);
                permission.setSortOrder(sortOrder);
                sysPermissionMapper.insert(permission);
            }
        } catch (Exception e) {
            log.error("  初始化权限失败 [{}]: {}", code, e.getMessage());
        }
    }

    private void initDefaultPermissionGroups() {
        log.info("初始化默认权限组...");

        insertOrUpdatePermissionGroup("系统管理组", "包含所有系统管理权限", 100);
        insertOrUpdatePermissionGroup("文章管理组", "包含所有文章管理权限", 90);
        insertOrUpdatePermissionGroup("用户管理组", "包含用户管理相关权限", 80);

        log.info("默认权限组初始化完成");
    }

    private void insertOrUpdatePermissionGroup(String name, String description, int sortOrder) {
        try {
            QueryWrapper<SysPermissionGroup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", name);
            SysPermissionGroup existing = sysPermissionGroupMapper.selectOne(queryWrapper);

            if (existing == null) {
                SysPermissionGroup group = new SysPermissionGroup();
                group.setName(name);
                group.setDescription(description);
                group.setSortOrder(sortOrder);
                group.setStatus(1);
                group.setIsSystem(true);
                group.setIsDeleted(0);
                sysPermissionGroupMapper.insert(group);
            } else {
                existing.setDescription(description);
                existing.setIsSystem(true);
                existing.setStatus(1);
                existing.setIsDeleted(0);
                sysPermissionGroupMapper.updateById(existing);
            }
        } catch (Exception e) {
            log.error("  初始化权限组失败 [{}]: {}", name, e.getMessage());
        }
    }

    private void initDefaultConfigs() {
        log.info("初始化默认配置...");

        insertOrUpdateConfig("site.name", "我的博客", "string", "max_length=100", "网站名称", 1);
        insertOrUpdateConfig("site.domain", "localhost:8080", "string", "max_length=100", "网站域名", 1);
        insertOrUpdateConfig("site.description", "一个简洁的个人博客系统", "text", null, "网站描述", 1);
        insertOrUpdateConfig("site.icp", "", "string", "max_length=50", "网站备案号", 1);
        insertOrUpdateConfig("user_register_default_role", "USER", "string", "required", "用户注册时默认分配的角色编码", 0);

        insertOrUpdateConfig("smtp.host", "smtp.example.com", "string", "max_length=100", "SMTP服务器地址", 0);
        insertOrUpdateConfig("smtp.port", "587", "integer", "range=1-65535", "SMTP端口号", 0);
        insertOrUpdateConfig("smtp.username", "your-email@example.com", "email", null, "SMTP用户名", 0);
        insertOrUpdateConfig("smtp.password", "your-password", "string", null, "SMTP密码", 0);
        insertOrUpdateConfig("smtp.fromName", "your-email@example.com", "email", null, "SMTP发件人邮箱", 0);
        insertOrUpdateConfig("smtp.ssl.enabled", "false", "boolean", null, "SMTP是否启用SSL", 0);
        insertOrUpdateConfig("smtp.comment.enabled", "false", "boolean", null, "SMTP是否启用评论通知", 0);

        insertOrUpdateConfig("aliyun.Secret-key", "Secret", "string", "max_length=200", "aliyun Secret Key", 0);
        insertOrUpdateConfig("aliyun.Access-key", "Access", "string", "max_length=200", "aliyun Access Key", 0);
        insertOrUpdateConfig("aliyun.Bucket", "Bucket", "string", "max_length=200", "aliyun Bucket", 0);
        insertOrUpdateConfig("aliyun.end-point", "end-point", "string", "max_length=200", "aliyun end-point", 0);
        insertOrUpdateConfig("aliyun.https-enabled", "false", "boolean", null, "阿里云是否启用https", 0);

        insertOrUpdateConfig("comment-show-email-enabled", "false", "boolean", null, "是否显示完整的评论者邮箱", 0);
        insertOrUpdateConfig("site.redirect_url", "", "string", null, "重定向URL", 1);

        log.info("默认配置初始化完成");
    }

    private void insertOrUpdateConfig(String key, String value, String dataType, String validationRule,
                                      String description, int isOpen) {
        try {
            QueryWrapper<SysConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("config_key", key);
            SysConfig existing = sysConfigMapper.selectOne(queryWrapper);

            if (existing == null) {
                SysConfig config = new SysConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                config.setDataType(dataType);
                config.setValidationRule(validationRule);
                config.setDescription(description);
                config.setIsSystem(1);
                config.setIsOpen(isOpen);
                config.setIsDeleted(0);
                sysConfigMapper.insert(config);
            } else {
                existing.setConfigValue(value);
                existing.setDescription(description);
                existing.setIsDeleted(0);
                sysConfigMapper.updateById(existing);
            }
        } catch (Exception e) {
            log.error("  初始化配置失败 [{}]: {}", key, e.getMessage());
        }
    }

    private void initDefaultSeoConfigs() {
        log.info("初始化默认SEO配置...");

        insertOrUpdateSeoConfig("home",
                "myblog - 记录技术与生活的点滴",
                "博客,技术博客,个人博客,技术分享,编程,开发",
                "我的个人博客，分享技术心得、生活感悟和编程经验",
                "我的博客 - 记录技术与生活的点滴",
                "我的个人博客，分享技术心得、生活感悟和编程经验",
                "website");

        insertOrUpdateSeoConfig("article",
                "myblog - {文章标题}",
                "{文章标签},{文章分类},技术博客,编程分享",
                "{文章摘要}",
                "我的博客 - {文章标题}",
                "{文章摘要}",
                "article");

        insertOrUpdateSeoConfig("links",
                "myblog - 友情链接",
                "友情链接,合作伙伴,技术博客,网站推荐",
                "我的博客友情链接页面，推荐优质的技术博客和网站",
                " - 友情链接",
                "我的博客友情链接页面，推荐优质的技术博客和网站",
                "website");

        log.info("默认SEO配置初始化完成");
    }

    private void insertOrUpdateSeoConfig(String pageType, String title, String keywords,
                                         String description, String ogTitle, String ogDescription,
                                         String ogType) {
        try {
            QueryWrapper<SysSeo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("page_type", pageType);
            queryWrapper.isNull("page_id");
            SysSeo existing = sysSeoMapper.selectOne(queryWrapper);

            if (existing == null) {
                SysSeo seo = new SysSeo();
                seo.setPageType(pageType);
                seo.setPageId(null);
                seo.setTitle(title);
                seo.setKeywords(keywords);
                seo.setDescription(description);
                seo.setOgTitle(ogTitle);
                seo.setOgDescription(ogDescription);
                seo.setOgImage(null);
                seo.setOgType(ogType);
                seo.setCanonicalUrl(null);
                seo.setRobots("index,follow");
                seo.setIsSystem(true);
                seo.setIsDeleted(0);
                sysSeoMapper.insert(seo);
            } else {
                existing.setTitle(title);
                existing.setKeywords(keywords);
                existing.setDescription(description);
                existing.setOgTitle(ogTitle);
                existing.setOgDescription(ogDescription);
                existing.setIsDeleted(0);
                sysSeoMapper.updateById(existing);
            }
        } catch (Exception e) {
            log.error("  初始化SEO配置失败 [{}]: {}", pageType, e.getMessage());
        }
    }

    /**
     * 初始化角色-权限关联关系
     */
    private void initRolePermissionRelations() {
        log.info("初始化角色-权限关联...");

        // 获取所有角色
        QueryWrapper<SysRole> roleWrapper = new QueryWrapper<>();
        roleWrapper.eq("is_deleted", 0);
        List<SysRole> roles = sysRoleMapper.selectList(roleWrapper);

        // 获取所有权限
        List<SysPermission> allPermissions = sysPermissionMapper.selectList(null);
        if (roles.isEmpty() || allPermissions.isEmpty()) {
            log.warn("角色或权限表为空，跳过关联初始化");
            return;
        }

        // 创建权限代码到ID的映射
        Map<String, Long> permissionCodeToId = new HashMap<>();
        for (SysPermission perm : allPermissions) {
            permissionCodeToId.put(perm.getCode(), perm.getId());
        }

        // 创建角色代码到ID的映射
        Map<String, Long> roleCodeToId = new HashMap<>();
        for (SysRole role : roles) {
            roleCodeToId.put(role.getCode(), role.getId());
        }

        // 定义角色-权限关联关系
        Map<String, List<String>> rolePermissionMap = buildRolePermissionRelations();

        // 为每个角色分配权限
        int totalRelations = 0;
        for (Map.Entry<String, List<String>> entry : rolePermissionMap.entrySet()) {
            String roleCode = entry.getKey();
            List<String> permissionCodes = entry.getValue();

            Long roleId = roleCodeToId.get(roleCode);
            if (roleId == null) {
                log.warn("  角色不存在: {}", roleCode);
                continue;
            }

            for (String permCode : permissionCodes) {
                Long permId = permissionCodeToId.get(permCode);
                if (permId == null) {
                    log.warn("  权限不存在: {}", permCode);
                    continue;
                }

                if (insertOrUpdateRolePermission(roleId, permId)) {
                    totalRelations++;
                }
            }
        }

        log.info("角色-权限关联初始化完成，共建立 {} 条关联关系", totalRelations);
    }

    /**
     * 定义角色-权限关联关系
     * 键：角色代码
     * 值：该角色拥有的权限代码列表
     */
    private Map<String, List<String>> buildRolePermissionRelations() {
        Map<String, List<String>> relations = new HashMap<>();

        // 超级管理员：拥有所有权限
        relations.put("SUPER_ADMIN", Arrays.asList(
                // 用户管理
                "system:user:list", "system:user:edit", "system:user:delete", "system:user:assignRole",
                // 角色管理
                "system:role:list", "system:role:create", "system:role:edit", "system:role:delete",
                "system:role:addPermission", "system:role:removePermission",
                "system:role:addPermissionGroup", "system:role:removePermissionGroup",
                // 权限管理
                "system:permission",
                // 权限组管理
                "system:permission:permission_group:list", "system:permission:permission_group:create",
                "system:permission:permission_group:edit", "system:permission:permission_group:delete",
                "system:permission:permission_group:addPermission", "system:permission:permission_group:removePermission",
                // 配置管理
                "system:config:systemlist", "system:config:customlist", "system:config:create", "system:config:edit", "system:config:delete",
                // SEO管理
                "system:seo:list", "system:seo:create", "system:seo:edit", "system:seo:delete",
                // OSS管理
                "system:oss:list", "system:oss:delete",
                // 全局文章管理
                "system:article:list", "system:article:edit", "system:article:delete",
                // 全局评论管理
                "system:comment:list", "system:comment:edit", "system:comment:delete", "system:comment:approve",
                // 文章管理
                "article:list", "article:create", "article:edit", "article:delete",
                // 分类管理
                "category:list", "category:create", "category:edit", "category:delete",
                // 评论管理
                "comment:list", "comment:edit", "comment:delete",
                // 链接管理
                "links:list", "links:create", "links:edit", "links:delete",
                // OSS
                "oss:list", "oss:create", "oss:delete"
        ));

        // 管理员：大部分管理权限
        relations.put("ADMIN", Arrays.asList(
                // 用户管理
                "system:user:list", "system:user:edit", "system:user:delete", "system:user:assignRole",
                // 角色管理
                "system:role:list",
                // 配置管理
                "system:config:systemlist", "system:config:customlist", "system:config:create", "system:config:edit", "system:config:delete",
                // OSS管理
                "system:oss:list", "system:oss:delete",
                // 全局文章管理
                "system:article:list", "system:article:edit", "system:article:delete",
                // 全局评论管理
                "system:comment:list", "system:comment:edit", "system:comment:delete", "system:comment:approve",
                // 文章管理
                "article:list", "article:create", "article:edit", "article:delete",
                // 分类管理
                "category:list", "category:create", "category:edit", "category:delete",
                // 评论管理
                "comment:list", "comment:edit", "comment:delete",
                // 链接管理
                "links:list", "links:create", "links:edit", "links:delete",
                // OSS
                "oss:list", "oss:create", "oss:delete"
        ));

        // 作者：文章相关权限
        relations.put("AUTHOR", Arrays.asList(
                // 文章管理
                "article:list", "article:create", "article:edit", "article:delete",
                // 分类管理
                "category:list", "category:create", "category:edit",
                // 评论管理
                "comment:list", "comment:edit",
                // 链接管理
                "links:list",
                // OSS
                "oss:list", "oss:create"
        ));

        // 普通用户：基础权限（无直接权限，通过角色组关联）
        relations.put("USER", List.of());

        return relations;
    }

    /**
     * 插入或更新角色-权限关联
     */
    private boolean insertOrUpdateRolePermission(Long roleId, Long permissionId) {
        try {
            QueryWrapper<SysRolePermission> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("role_id", roleId);
            queryWrapper.eq("permission_id", permissionId);
            SysRolePermission existing = sysRolePermissionMapper.selectOne(queryWrapper);

            if (existing == null) {
                SysRolePermission relation = new SysRolePermission();
                relation.setRoleId(roleId);
                relation.setPermissionId(permissionId);
                sysRolePermissionMapper.insert(relation);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("  初始化角色权限关联失败 [role={}, perm={}]: {}", roleId, permissionId, e.getMessage());
            return false;
        }
    }

    /**
     * 初始化权限-权限组关联关系
     */
    private void initPermissionGroupItems() {
        log.info("初始化权限-权限组关联...");

        // 获取所有权限组
        QueryWrapper<SysPermissionGroup> groupWrapper = new QueryWrapper<>();
        groupWrapper.eq("is_deleted", 0);
        List<SysPermissionGroup> groups = sysPermissionGroupMapper.selectList(groupWrapper);

        // 获取所有权限
        List<SysPermission> allPermissions = sysPermissionMapper.selectList(null);

        if (groups.isEmpty() || allPermissions.isEmpty()) {
            log.warn("权限组或权限表为空，跳过关联初始化");
            return;
        }

        // 创建权限组名称到ID的映射
        Map<String, Long> groupNameToId = new HashMap<>();
        for (SysPermissionGroup group : groups) {
            groupNameToId.put(group.getName(), group.getId());
        }

        // 创建权限代码到ID的映射
        Map<String, Long> permissionCodeToId = new HashMap<>();
        for (SysPermission perm : allPermissions) {
            permissionCodeToId.put(perm.getCode(), perm.getId());
        }

        // 定义权限-权限组关联关系
        Map<String, List<String>> permissionGroupMap = buildPermissionGroupRelations();

        // 为每个权限组分配权限
        int totalRelations = 0;
        for (Map.Entry<String, List<String>> entry : permissionGroupMap.entrySet()) {
            String groupName = entry.getKey();
            List<String> permissionCodes = entry.getValue();

            Long groupId = groupNameToId.get(groupName);
            if (groupId == null) {
                log.warn("  权限组不存在: {}", groupName);
                continue;
            }

            for (String permCode : permissionCodes) {
                Long permId = permissionCodeToId.get(permCode);
                if (permId == null) {
                    log.warn("  权限不存在: {}", permCode);
                    continue;
                }

                if (insertOrUpdatePermissionGroupItem(groupId, permId, totalRelations)) {
                    totalRelations++;
                }
            }
        }

        log.info("权限-权限组关联初始化完成，共建立 {} 条关联关系", totalRelations);
    }

    /**
     * 定义权限-权限组关联关系
     */
    private Map<String, List<String>> buildPermissionGroupRelations() {
        Map<String, List<String>> relations = new HashMap<>();

        // 系统管理组
        relations.put("系统管理组", Arrays.asList(
                // 用户管理
                "system:user:list", "system:user:edit", "system:user:delete", "system:user:assignRole",
                // 角色管理
                "system:role:list", "system:role:create", "system:role:edit", "system:role:delete",
                "system:role:addPermission", "system:role:removePermission",
                "system:role:addPermissionGroup", "system:role:removePermissionGroup",
                // 权限管理
                "system:permission",
                // 权限组管理
                "system:permission:permission_group:list", "system:permission:permission_group:create",
                "system:permission:permission_group:edit", "system:permission:permission_group:delete",
                "system:permission:permission_group:addPermission", "system:permission:permission_group:removePermission",
                // 配置管理
                "system:config:systemlist", "system:config:customlist", "system:config:create", "system:config:edit", "system:config:delete",
                // SEO管理
                "system:seo:list", "system:seo:create", "system:seo:edit", "system:seo:delete",
                // OSS管理
                "system:oss:list", "system:oss:delete"
        ));

        // 文章管理组
        relations.put("文章管理组", Arrays.asList(
                // 全局文章管理
                "system:article:list", "system:article:edit", "system:article:delete",
                // 全局评论管理
                "system:comment:list", "system:comment:edit", "system:comment:delete", "system:comment:approve",
                // 文章管理
                "article:list", "article:create", "article:edit", "article:delete",
                // 分类管理
                "category:list", "category:create", "category:edit", "category:delete",
                // 评论管理
                "comment:list", "comment:edit", "comment:delete",
                // 链接管理
                "links:list", "links:create", "links:edit", "links:delete",
                // OSS
                "oss:list", "oss:create", "oss:delete"
        ));

        // 用户管理组
        relations.put("用户管理组", Arrays.asList(
                "system:user:list", "system:user:edit", "system:user:delete", "system:user:assignRole"
        ));

        return relations;
    }

    /**
     * 插入或更新权限-权限组关联
     */
    private boolean insertOrUpdatePermissionGroupItem(Long groupId, Long permissionId, int sortOrder) {
        try {
            QueryWrapper<SysPermissionGroupItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("group_id", groupId);
            queryWrapper.eq("permission_id", permissionId);
            SysPermissionGroupItem existing = sysPermissionGroupItemMapper.selectOne(queryWrapper);

            if (existing == null) {
                SysPermissionGroupItem item = new SysPermissionGroupItem();
                item.setGroupId(groupId);
                item.setPermissionId(permissionId);
                item.setSortOrder(sortOrder);
                sysPermissionGroupItemMapper.insert(item);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("  初始化权限组权限关联失败 [group={}, perm={}]: {}", groupId, permissionId, e.getMessage());
            return false;
        }
    }

    /**
     * 初始化角色-权限组关联关系
     */
    private void initRolePermissionGroupRelations() {
        log.info("初始化角色-权限组关联...");

        // 获取所有角色
        QueryWrapper<SysRole> roleWrapper = new QueryWrapper<>();
        roleWrapper.eq("is_deleted", 0);
        List<SysRole> roles = sysRoleMapper.selectList(roleWrapper);

        // 获取所有权限组
        QueryWrapper<SysPermissionGroup> groupWrapper = new QueryWrapper<>();
        groupWrapper.eq("is_deleted", 0);
        List<SysPermissionGroup> groups = sysPermissionGroupMapper.selectList(groupWrapper);

        if (roles.isEmpty() || groups.isEmpty()) {
            log.warn("角色或权限组表为空，跳过关联初始化");
            return;
        }

        // 创建权限组名称到ID的映射
        Map<String, Long> groupNameToId = new HashMap<>();
        for (SysPermissionGroup group : groups) {
            groupNameToId.put(group.getName(), group.getId());
        }

        // 创建角色代码到ID的映射
        Map<String, Long> roleCodeToId = new HashMap<>();
        for (SysRole role : roles) {
            roleCodeToId.put(role.getCode(), role.getId());
        }

        // 定义角色-权限组关联关系
        Map<String, List<String>> roleGroupMap = buildRolePermissionGroupRelations();

        // 为每个角色分配权限组
        int totalRelations = 0;
        for (Map.Entry<String, List<String>> entry : roleGroupMap.entrySet()) {
            String roleCode = entry.getKey();
            List<String> groupNames = entry.getValue();

            Long roleId = roleCodeToId.get(roleCode);
            if (roleId == null) {
                log.warn("  角色不存在: {}", roleCode);
                continue;
            }

            for (String groupName : groupNames) {
                Long groupId = groupNameToId.get(groupName);
                if (groupId == null) {
                    log.warn("  权限组不存在: {}", groupName);
                    continue;
                }

                if (insertOrUpdateRolePermissionGroup(roleId, groupId)) {
                    totalRelations++;
                }
            }
        }

        log.info("角色-权限组关联初始化完成，共建立 {} 条关联关系", totalRelations);
    }

    /**
     * 定义角色-权限组关联关系
     */
    private Map<String, List<String>> buildRolePermissionGroupRelations() {
        Map<String, List<String>> relations = new HashMap<>();

        // 超级管理员：所有权限组
        relations.put("SUPER_ADMIN", Arrays.asList(
                "系统管理组", "文章管理组", "用户管理组"
        ));

        // 管理员：系统管理和文章管理
        relations.put("ADMIN", Arrays.asList(
                "系统管理组", "文章管理组"
        ));

        // 作者：文章管理
        relations.put("AUTHOR", List.of(
                "文章管理组"
        ));

        // 普通用户：无权限组关联（只有直接权限）
        relations.put("USER", List.of());

        return relations;
    }

    /**
     * 插入或更新角色-权限组关联
     */
    private boolean insertOrUpdateRolePermissionGroup(Long roleId, Long groupId) {
        try {
            QueryWrapper<SysRolePermissionGroup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("role_id", roleId);
            queryWrapper.eq("group_id", groupId);
            SysRolePermissionGroup existing = sysRolePermissionGroupMapper.selectOne(queryWrapper);

            if (existing == null) {
                SysRolePermissionGroup relation = new SysRolePermissionGroup();
                relation.setRoleId(roleId);
                relation.setGroupId(groupId);
                sysRolePermissionGroupMapper.insert(relation);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("  初始化角色权限组关联失败 [role={}, group={}]: {}", roleId, groupId, e.getMessage());
            return false;
        }
    }
}
