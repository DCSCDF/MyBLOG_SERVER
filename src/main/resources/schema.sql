-- schema.sql
CREATE DATABASE IF NOT EXISTS myblog_sql CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE myblog_sql;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    nickname    VARCHAR(50)  NOT NULL COMMENT '昵称',
    password    VARCHAR(100) NOT NULL COMMENT '密码（加密后）',
    email       VARCHAR(50) UNIQUE COMMENT '邮箱（唯一）',
    avatar_url  VARCHAR(200) COMMENT '头像URL',
    status      INT        DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    is_deleted  TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    create_time DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    code           VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码（唯一）',
    name           VARCHAR(50) NOT NULL COMMENT '角色名称',
    description    VARCHAR(200) COMMENT '角色描述',
    is_super_admin TINYINT(1) DEFAULT 0 COMMENT '是否超级管理员：0=否，1=是（只能有一个）',
    is_system      TINYINT(1) DEFAULT 0 COMMENT '是否系统内置角色：0=否，1=是（不可删除）',
    sort_order     INT        DEFAULT 0 COMMENT '排序顺序',
    status         INT        DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    is_deleted     TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    create_time    DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
    code        VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码（唯一）',
    name        VARCHAR(50)  NOT NULL COMMENT '权限名称',
    description VARCHAR(200) COMMENT '权限描述',
    sort_order  INT      DEFAULT 0 COMMENT '排序顺序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='权限表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    role_id     BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_user_role (user_id, role_id) COMMENT '防止重复关联',
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE NO ACTION,
    FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE NO ACTION
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户-角色关联表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    role_id       BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_role_permission (role_id, permission_id) COMMENT '防止重复关联',
    FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE NO ACTION,
    FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE NO ACTION
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色-权限关联表';

-- 权限组表
CREATE TABLE IF NOT EXISTS sys_permission_group
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限组ID',
    name        VARCHAR(50) NOT NULL COMMENT '权限组名称',
    description VARCHAR(200) COMMENT '权限组描述',
    sort_order  INT        DEFAULT 0 COMMENT '排序顺序',
    status      INT        DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    is_system   TINYINT(1) DEFAULT 0 COMMENT '是否系统内置：0=否，1=是（不可删除）',
    is_deleted  TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    create_time DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='权限组表';

-- 权限-权限组关联表
CREATE TABLE IF NOT EXISTS sys_permission_group_item
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    group_id      BIGINT NOT NULL COMMENT '权限组ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    sort_order    INT      DEFAULT 0 COMMENT '排序顺序',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_group_permission (group_id, permission_id) COMMENT '防止重复关联',
    FOREIGN KEY (group_id) REFERENCES sys_permission_group (id) ON DELETE NO ACTION,
    FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE NO ACTION
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='权限-权限组关联表';

-- 角色权限组关联表
CREATE TABLE IF NOT EXISTS sys_role_permission_group
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    role_id     BIGINT NOT NULL COMMENT '角色ID',
    group_id    BIGINT NOT NULL COMMENT '权限组ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_role_group (role_id, group_id) COMMENT '防止重复关联',
    FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE NO ACTION,
    FOREIGN KEY (group_id) REFERENCES sys_permission_group (id) ON DELETE NO ACTION
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色-权限组关联表';

-- 分类表
CREATE TABLE IF NOT EXISTS sys_category
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name        VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(200) COMMENT '分类描述',
    sort_order  INT        DEFAULT 0 COMMENT '排序顺序（数字越大越靠前）',
    create_time DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_hidden   TINYINT(1) DEFAULT 0 COMMENT '是否隐藏：0=显示，1=隐藏',
    is_deleted  TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    update_time DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文章分类表';

-- 博客/文章表
CREATE TABLE IF NOT EXISTS sys_blog
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文章ID',
    category_id   BIGINT COMMENT '分类ID',
    title         VARCHAR(200) NOT NULL COMMENT '文章标题',
    summary       VARCHAR(500) COMMENT '文章摘要',
    content       LONGTEXT COMMENT '文章内容',
    cover_image   VARCHAR(200) COMMENT '封面图片URL',
    tags          VARCHAR(200) COMMENT '标签（逗号分隔）',
    author_id     BIGINT COMMENT '作者ID',
    view_count    INT        DEFAULT 0 COMMENT '浏览量',
    comment_count INT        DEFAULT 0 COMMENT '评论数',
    like_count    INT        DEFAULT 0 COMMENT '点赞数',
    is_hidden     TINYINT(1) DEFAULT 0 COMMENT '是否隐藏：0=公开，1=私密',
    is_top        TINYINT(1) DEFAULT 0 COMMENT '是否置顶：0=否，1=是',
    is_recommend  TINYINT(1) DEFAULT 0 COMMENT '是否推荐：0=否，1=是',
    is_deleted    TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    create_time   DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (category_id) REFERENCES sys_category (id) ON DELETE NO ACTION,
    FOREIGN KEY (author_id) REFERENCES sys_user (id) ON DELETE NO ACTION
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='博客文章表';

-- 评论表
CREATE TABLE IF NOT EXISTS sys_comment
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    blog_id     BIGINT      NOT NULL COMMENT '关联的文章ID',
    parent_id   BIGINT     DEFAULT 0 COMMENT '父评论ID，0表示顶级评论',
    user_id     BIGINT COMMENT '用户ID（已登录用户）',
    username    VARCHAR(50) NOT NULL COMMENT '评论者名称',
    email       VARCHAR(100) COMMENT '邮箱',
    avatar_url  VARCHAR(200) COMMENT '头像URL',
    website     VARCHAR(200) COMMENT '个人网站',
    content     TEXT        NOT NULL COMMENT '评论内容',
    status      TINYINT    DEFAULT 0 COMMENT '状态：0=待审核，1=已通过，2=垃圾评论，3=已删除',
    like_count  INT        DEFAULT 0 COMMENT '点赞数',
    device_info VARCHAR(200) COMMENT '设备信息',
    ip_address  VARCHAR(50) COMMENT 'IP地址',
    is_admin    TINYINT(1) DEFAULT 0 COMMENT '是否管理员评论：0=否，1=是',
    is_deleted  TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    create_time DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (blog_id) REFERENCES sys_blog (id) ON DELETE NO ACTION,
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE NO ACTION
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='评论表';

-- 评论点赞表
CREATE TABLE IF NOT EXISTS sys_comment_like
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞记录ID',
    comment_id  BIGINT NOT NULL COMMENT '评论ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_comment_user (comment_id, user_id) COMMENT '防止重复点赞',
    FOREIGN KEY (comment_id) REFERENCES sys_comment (id) ON DELETE NO ACTION,
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE NO ACTION
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='评论点赞表';

-- 标签表
CREATE TABLE IF NOT EXISTS sys_tag
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    name        VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    is_deleted  TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    create_time DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='标签表';

-- 文章-标签关联表
CREATE TABLE IF NOT EXISTS sys_blog_tag
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    blog_id     BIGINT NOT NULL COMMENT '文章ID',
    tag_id      BIGINT NOT NULL COMMENT '标签ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_blog_tag (blog_id, tag_id) COMMENT '防止重复关联',
    FOREIGN KEY (blog_id) REFERENCES sys_blog (id) ON DELETE NO ACTION,
    FOREIGN KEY (tag_id) REFERENCES sys_tag (id) ON DELETE NO ACTION
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文章-标签关联表';

-- 文章点赞表
CREATE TABLE IF NOT EXISTS sys_blog_like
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞记录ID',
    blog_id     BIGINT NOT NULL COMMENT '文章ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_blog_user (blog_id, user_id) COMMENT '防止重复点赞',
    FOREIGN KEY (blog_id) REFERENCES sys_blog (id) ON DELETE NO ACTION,
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE NO ACTION
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文章点赞表';

-- 网站配置表
CREATE TABLE IF NOT EXISTS sys_config
(
    config_key      VARCHAR(100) NOT NULL PRIMARY KEY COMMENT '配置项唯一键，如 site_title, seo_keywords',
    config_value    TEXT         NOT NULL COMMENT '配置值（字符串形式存储，应用层按类型解析）',
    data_type       VARCHAR(20)  NOT NULL DEFAULT 'string' COMMENT '数据类型：string, boolean, integer, json, email, url, text',
    validation_rule VARCHAR(255) COMMENT '校验规则：如 max_length=100, regex=..., array_of_strings 等',
    description     VARCHAR(255) COMMENT '配置项说明，用于后台展示',
    is_system       TINYINT(1)            DEFAULT 0 COMMENT '是否系统内置：0=否，1=是（不可删除）',
    update_time     DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '网站全局配置表（键值对）';


-- 创建索引 删除已存在的索引（仅删除下方会重建的索引）
ALTER TABLE sys_user DROP INDEX idx_user_status;
ALTER TABLE sys_role DROP INDEX idx_role_code;
ALTER TABLE sys_role DROP INDEX idx_role_status;
ALTER TABLE sys_role DROP INDEX idx_role_super_admin;
ALTER TABLE sys_permission DROP INDEX idx_permission_code;
ALTER TABLE sys_user_role DROP INDEX idx_user_role_user;
ALTER TABLE sys_user_role DROP INDEX idx_user_role_role;
ALTER TABLE sys_role_permission DROP INDEX idx_role_permission_role;
ALTER TABLE sys_role_permission DROP INDEX idx_role_permission_permission;
ALTER TABLE sys_blog DROP INDEX idx_blog_category;
ALTER TABLE sys_blog DROP INDEX idx_blog_author;
ALTER TABLE sys_blog DROP INDEX idx_blog_create_time;
ALTER TABLE sys_blog DROP INDEX idx_blog_hidden;
ALTER TABLE sys_blog DROP INDEX idx_blog_top;
ALTER TABLE sys_category DROP INDEX idx_category_hidden;
ALTER TABLE sys_category DROP INDEX idx_category_sort;
ALTER TABLE sys_comment DROP INDEX idx_comment_blog;
ALTER TABLE sys_comment DROP INDEX idx_comment_parent;
ALTER TABLE sys_comment DROP INDEX idx_comment_user;
ALTER TABLE sys_comment DROP INDEX idx_comment_status;
ALTER TABLE sys_comment DROP INDEX idx_comment_create_time;
ALTER TABLE sys_comment_like DROP INDEX idx_comment_like_comment;
ALTER TABLE sys_comment_like DROP INDEX idx_comment_like_user;

-- 创建新索引
CREATE INDEX idx_user_status ON sys_user (status) COMMENT '用户状态索引';
CREATE INDEX idx_role_code ON sys_role (code) COMMENT '角色编码索引';
CREATE INDEX idx_role_status ON sys_role (status) COMMENT '角色状态索引';
CREATE INDEX idx_role_super_admin ON sys_role (is_super_admin) COMMENT '角色超级管理员索引';
CREATE INDEX idx_permission_code ON sys_permission (code) COMMENT '权限编码索引';
# CREATE INDEX idx_permission_parent ON sys_permission (parent_id) COMMENT '权限父级索引';
# CREATE INDEX idx_permission_type ON sys_permission (type) COMMENT '权限类型索引';
# CREATE INDEX idx_permission_status ON sys_permission (status) COMMENT '权限状态索引';
CREATE INDEX idx_user_role_user ON sys_user_role (user_id) COMMENT '用户角色-用户索引';
CREATE INDEX idx_user_role_role ON sys_user_role (role_id) COMMENT '用户角色-角色索引';
CREATE INDEX idx_role_permission_role ON sys_role_permission (role_id) COMMENT '角色权限-角色索引';
CREATE INDEX idx_role_permission_permission ON sys_role_permission (permission_id) COMMENT '角色权限-权限索引';
CREATE INDEX idx_blog_category ON sys_blog (category_id) COMMENT '文章分类索引';
CREATE INDEX idx_blog_author ON sys_blog (author_id) COMMENT '文章作者索引';
CREATE INDEX idx_blog_create_time ON sys_blog (create_time) COMMENT '文章创建时间索引';
CREATE INDEX idx_blog_hidden ON sys_blog (is_hidden) COMMENT '文章隐藏状态索引';
CREATE INDEX idx_blog_top ON sys_blog (is_top) COMMENT '文章置顶状态索引';
CREATE INDEX idx_category_hidden ON sys_category (is_hidden) COMMENT '分类隐藏状态索引';
CREATE INDEX idx_category_sort ON sys_category (sort_order) COMMENT '分类排序索引';
CREATE INDEX idx_comment_blog ON sys_comment (blog_id) COMMENT '评论文章索引';
CREATE INDEX idx_comment_parent ON sys_comment (parent_id) COMMENT '评论父评论索引';
CREATE INDEX idx_comment_user ON sys_comment (user_id) COMMENT '评论用户索引';
CREATE INDEX idx_comment_status ON sys_comment (status) COMMENT '评论状态索引';
CREATE INDEX idx_comment_create_time ON sys_comment (create_time) COMMENT '评论创建时间索引';
CREATE INDEX idx_comment_like_comment ON sys_comment_like (comment_id) COMMENT '点赞评论索引';
CREATE INDEX idx_comment_like_user ON sys_comment_like (user_id) COMMENT '点赞用户索引';


-- 插入默认角色
INSERT IGNORE INTO sys_role (code, name, description, is_super_admin, is_system, sort_order, status)
SELECT 'SUPER_ADMIN', '超级管理员', '拥有系统所有权限，只能有一个', 1, 1, 100, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'SUPER_ADMIN');

INSERT IGNORE INTO sys_role (code, name, description, is_super_admin, is_system, sort_order, status)
SELECT 'ADMIN', '普通管理员', '拥有系统大部分管理权限', 0, 1, 90, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'ADMIN');

INSERT IGNORE INTO sys_role (code, name, description, is_super_admin, is_system, sort_order, status)
SELECT 'AUTHOR', '文章作者', '可以发布和管理自己的文章', 0, 1, 80, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'AUTHOR');

INSERT IGNORE INTO sys_role (code, name, description, is_super_admin, is_system, sort_order, status)
SELECT 'USER', '普通用户', '可以评论、点赞、收藏文章', 0, 1, 70, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'USER');

-- 修复默认角色标记（兼容旧数据：历史库可能已存在但标记不正确）
UPDATE sys_role
SET is_super_admin = 1,
    is_system      = 1,
    status         = 1,
    is_deleted     = 0
WHERE code = 'SUPER_ADMIN';

UPDATE sys_role
SET is_system  = 1,
    status     = 1,
    is_deleted = 0
WHERE code IN ('ADMIN', 'AUTHOR', 'USER');

-- 系统管理权限
INSERT IGNORE INTO sys_permission (code, name, description, sort_order)
VALUES ('system', '系统管理', '系统管理菜单', 100),
       ('system:user', '用户管理', '用户管理', 101),
       ('system:role', '角色管理', '角色管理', 102),
       ('system:permission', '权限管理', '权限管理', 103),
       ('system:permission_group', '权限组管理', '权限组管理', 104),

-- 用户管理 API 权限
       ('system:user:list', '用户列表', '查看用户列表', 1),
       ('system:user:create', '创建用户', '创建用户', 2),
       ('system:user:edit', '编辑用户', '编辑用户', 3),
       ('system:user:delete', '删除用户', '删除用户', 4),
       ('system:user:assignRole', '分配角色', '为用户分配角色', 5),

-- 角色管理 API 权限
       ('system:role:list', '角色列表', '查看角色列表', 1),
       ('system:role:create', '创建角色', '创建角色', 2),
       ('system:role:edit', '编辑角色', '编辑角色', 3),
       ('system:role:delete', '删除角色', '删除角色', 4),
       ('system:role:assignPermission', '分配权限', '为角色分配权限', 5),
       ('system:role:addPermission', '角色添加权限', '为角色添加权限', 6),
       ('system:role:removePermission', '角色移除权限', '从角色移除权限', 7),
       ('system:role:addPermissionGroup', '角色添加权限组', '为角色添加权限组', 8),
       ('system:role:removePermissionGroup', '角色移除权限组', '从角色移除权限组', 9),

-- 权限组管理 API 权限
       ('system:permission_group:list', '权限组列表', '查看权限组列表', 1),
       ('system:permission_group:create', '创建权限组', '创建权限组', 2),
       ('system:permission_group:edit', '编辑权限组', '编辑权限组', 3),
       ('system:permission_group:delete', '删除权限组', '删除权限组', 4),
       ('system:permission_group:addPermission', '权限组添加权限', '为权限组添加权限', 4),
       ('system:permission_group:removePermission', '权限组移除权限', '从权限组移除权限', 5),

-- 文章管理
       ('article', '文章管理', '文章管理菜单', 90),
       ('article:list', '文章列表', '查看文章列表', 1),
       ('article:create', '创建文章', '创建文章', 2),
       ('article:edit', '编辑文章', '编辑文章', 3),
       ('article:delete', '删除文章', '删除文章', 4),
       ('article:publish', '发布文章', '发布文章', 5),

-- 分类管理
       ('category', '分类管理', '分类管理菜单', 80),
       ('category:list', '分类列表', '查看分类列表', 1),
       ('category:create', '创建分类', '创建分类', 2),
       ('category:edit', '编辑分类', '编辑分类', 3),
       ('category:delete', '删除分类', '删除分类', 4),

-- 评论管理
       ('comment', '评论管理', '评论管理菜单', 70),
       ('comment:list', '评论列表', '查看评论列表', 1),
       ('comment:create', '创建评论', '创建评论', 2),
       ('comment:edit', '编辑评论', '编辑评论', 3),
       ('comment:delete', '删除评论', '删除评论', 4),
       ('comment:approve', '审核评论', '审核评论', 5);

-- 为超级管理员角色分配所有权限（使用NOT EXISTS检查）
-- 重新分配默认角色权限（避免父子权限同时分配导致后续“权限重叠”问题）
DELETE rp
FROM sys_role_permission rp
         JOIN sys_role r ON rp.role_id = r.id
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN', 'AUTHOR', 'USER');

-- SUPER_ADMIN：分配全部“叶子权限”（排除菜单/父级权限，避免父子冲突）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         CROSS JOIN sys_permission p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code NOT IN ('system', 'system:user', 'system:role', 'system:permission_group', 'article', 'category', 'comment');

-- ADMIN：用户管理 + 内容管理（文章/分类/评论）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         CROSS JOIN sys_permission p
WHERE r.code = 'ADMIN'
  AND (p.code LIKE 'system:user:%'
    OR p.code LIKE 'article:%'
    OR p.code LIKE 'category:%'
    OR p.code LIKE 'comment:%');

-- AUTHOR：文章管理 + 分类列表 + 基础评论权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         CROSS JOIN sys_permission p
WHERE r.code = 'AUTHOR'
  AND (p.code LIKE 'article:%'
    OR p.code IN ('category:list', 'comment:create', 'comment:list'));

-- USER：文章列表 + 评论（创建/列表）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         CROSS JOIN sys_permission p
WHERE r.code = 'USER'
  AND p.code IN ('article:list', 'comment:create', 'comment:list');

-- 插入默认权限组
INSERT IGNORE INTO sys_permission_group (name, description, sort_order, status, is_system)
SELECT '系统管理组', '包含所有系统管理权限', 100, 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission_group WHERE name = '系统管理组');

INSERT IGNORE INTO sys_permission_group (name, description, sort_order, status, is_system)
SELECT '文章管理组', '包含所有文章管理权限', 90, 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission_group WHERE name = '文章管理组');

INSERT IGNORE INTO sys_permission_group (name, description, sort_order, status, is_system)
SELECT '用户管理组', '包含用户管理相关权限', 80, 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission_group WHERE name = '用户管理组');

-- 修复默认权限组标记（兼容旧数据）
UPDATE sys_permission_group
SET is_system  = 1,
    status     = 1,
    is_deleted = 0
WHERE name IN ('系统管理组', '文章管理组', '用户管理组');

-- 为权限组添加权限
-- 重建系统内置权限组的默认分配（避免父子权限混入同一组）
DELETE pgi
FROM sys_permission_group_item pgi
         JOIN sys_permission_group g ON pgi.group_id = g.id
WHERE g.name IN ('系统管理组', '文章管理组', '用户管理组');

-- 系统管理组权限
INSERT IGNORE INTO sys_permission_group_item (group_id, permission_id, sort_order)
SELECT g.id, p.id, 1
FROM sys_permission_group g
         CROSS JOIN sys_permission p
WHERE g.name = '系统管理组'
  AND (p.code = 'system:permission'
    OR p.code LIKE 'system:role:%'
    OR p.code LIKE 'system:permission_group:%')
  AND NOT EXISTS (SELECT 1
                  FROM sys_permission_group_item pgi
                  WHERE pgi.group_id = g.id
                    AND pgi.permission_id = p.id);

-- 文章管理组权限
INSERT IGNORE INTO sys_permission_group_item (group_id, permission_id, sort_order)
SELECT g.id, p.id, 1
FROM sys_permission_group g
         CROSS JOIN sys_permission p
WHERE g.name = '文章管理组'
  AND (p.code LIKE 'article:%'
    OR p.code LIKE 'category:%'
    OR p.code LIKE 'comment:%')
  AND NOT EXISTS (SELECT 1
                  FROM sys_permission_group_item pgi
                  WHERE pgi.group_id = g.id
                    AND pgi.permission_id = p.id);

-- 用户管理组权限
INSERT IGNORE INTO sys_permission_group_item (group_id, permission_id, sort_order)
SELECT g.id, p.id, 1
FROM sys_permission_group g
         CROSS JOIN sys_permission p
WHERE g.name = '用户管理组'
  AND p.code LIKE 'system:user:%'
  AND NOT EXISTS (SELECT 1
                  FROM sys_permission_group_item pgi
                  WHERE pgi.group_id = g.id
                    AND pgi.permission_id = p.id);

-- 插入默认网站配置
INSERT IGNORE INTO sys_config (config_key, config_value, data_type, validation_rule, description, is_system)
VALUES ('site_title', '我的博客', 'string', 'max_length=100', '网站主标题', 1),
       ('site_subtitle', '记录技术与生活的点滴', 'string', 'max_length=200', '网站副标题', 1),
       ('user_register_default_role', 'USER', 'string', 'required', '用户注册时默认分配的角色编码（如 USER、AUTHOR）', 1);

