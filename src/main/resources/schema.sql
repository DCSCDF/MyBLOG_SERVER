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
    status      INT      DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
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
    create_time    DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
    parent_id     BIGINT     DEFAULT 0 COMMENT '父权限ID，0表示顶级权限',
    code          VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码（唯一）',
    name          VARCHAR(50)  NOT NULL COMMENT '权限名称',
    type          VARCHAR(20)  NOT NULL COMMENT '权限类型：MENU=菜单，BUTTON=按钮，API=接口，FIELD=字段',
    description   VARCHAR(200) COMMENT '权限描述',
    icon          VARCHAR(50) COMMENT '图标',
    path          VARCHAR(200) COMMENT '路径/URL',
    component     VARCHAR(200) COMMENT '前端组件',
    is_hidden     TINYINT(1) DEFAULT 0 COMMENT '是否隐藏：0=显示，1=隐藏',
    is_affix      TINYINT(1) DEFAULT 0 COMMENT '是否固定页签：0=否，1=是',
    is_keep_alive TINYINT(1) DEFAULT 0 COMMENT '是否缓存：0=否，1=是',
    sort_order    INT        DEFAULT 0 COMMENT '排序顺序',
    status        INT        DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    create_time   DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
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
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
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
    FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色-权限关联表';

-- 权限组表（用于组织权限）
CREATE TABLE IF NOT EXISTS sys_permission_group
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限组ID',
    name        VARCHAR(50) NOT NULL COMMENT '权限组名称',
    description VARCHAR(200) COMMENT '权限组描述',
    sort_order  INT      DEFAULT 0 COMMENT '排序顺序',
    status      INT      DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
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
    FOREIGN KEY (group_id) REFERENCES sys_permission_group (id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
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
    FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES sys_permission_group (id) ON DELETE CASCADE
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
    create_time   DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (category_id) REFERENCES sys_category (id) ON DELETE SET NULL,
    FOREIGN KEY (author_id) REFERENCES sys_user (id) ON DELETE SET NULL
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
    create_time DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (blog_id) REFERENCES sys_blog (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE SET NULL
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
    FOREIGN KEY (comment_id) REFERENCES sys_comment (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='评论点赞表';

-- 标签表
CREATE TABLE IF NOT EXISTS sys_tag
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    name        VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
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
    FOREIGN KEY (blog_id) REFERENCES sys_blog (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES sys_tag (id) ON DELETE CASCADE
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
    FOREIGN KEY (blog_id) REFERENCES sys_blog (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文章点赞表';

-- 创建索引 删除已存在的索引
DROP INDEX IF EXISTS idx_user_status ON sys_user;
DROP INDEX IF EXISTS idx_role_code ON sys_role;
DROP INDEX IF EXISTS idx_role_status ON sys_role;
DROP INDEX IF EXISTS idx_role_super_admin ON sys_role;
DROP INDEX IF EXISTS idx_permission_code ON sys_permission;
DROP INDEX IF EXISTS idx_permission_parent ON sys_permission;
DROP INDEX IF EXISTS idx_permission_type ON sys_permission;
DROP INDEX IF EXISTS idx_permission_status ON sys_permission;
DROP INDEX IF EXISTS idx_user_role_user ON sys_user_role;
DROP INDEX IF EXISTS idx_user_role_role ON sys_user_role;
DROP INDEX IF EXISTS idx_role_permission_role ON sys_role_permission;
DROP INDEX IF EXISTS idx_role_permission_permission ON sys_role_permission;
DROP INDEX IF EXISTS idx_blog_category ON sys_blog;
DROP INDEX IF EXISTS idx_blog_author ON sys_blog;
DROP INDEX IF EXISTS idx_blog_create_time ON sys_blog;
DROP INDEX IF EXISTS idx_blog_hidden ON sys_blog;
DROP INDEX IF EXISTS idx_blog_top ON sys_blog;
DROP INDEX IF EXISTS idx_category_hidden ON sys_category;
DROP INDEX IF EXISTS idx_category_sort ON sys_category;
DROP INDEX IF EXISTS idx_comment_blog ON sys_comment;
DROP INDEX IF EXISTS idx_comment_parent ON sys_comment;
DROP INDEX IF EXISTS idx_comment_user ON sys_comment;
DROP INDEX IF EXISTS idx_comment_status ON sys_comment;
DROP INDEX IF EXISTS idx_comment_create_time ON sys_comment;
DROP INDEX IF EXISTS idx_comment_like_comment ON sys_comment_like;
DROP INDEX IF EXISTS idx_comment_like_user ON sys_comment_like;

-- 创建新索引
CREATE INDEX idx_user_status ON sys_user (status) COMMENT '用户状态索引';
CREATE INDEX idx_role_code ON sys_role (code) COMMENT '角色编码索引';
CREATE INDEX idx_role_status ON sys_role (status) COMMENT '角色状态索引';
CREATE INDEX idx_role_super_admin ON sys_role (is_super_admin) COMMENT '角色超级管理员索引';
CREATE INDEX idx_permission_code ON sys_permission (code) COMMENT '权限编码索引';
CREATE INDEX idx_permission_parent ON sys_permission (parent_id) COMMENT '权限父级索引';
CREATE INDEX idx_permission_type ON sys_permission (type) COMMENT '权限类型索引';
CREATE INDEX idx_permission_status ON sys_permission (status) COMMENT '权限状态索引';
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

-- 插入默认权限
-- 系统管理权限
INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 0, 'system', '系统管理', 'MENU', '系统管理菜单', 100
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 1, 'system:user', '用户管理', 'MENU', '用户管理', 101
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 1, 'system:role', '角色管理', 'MENU', '角色管理', 102
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:role');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 1, 'system:permission', '权限管理', 'MENU', '权限管理', 103
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:permission');

-- 用户管理权限
INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 2, 'system:user:list', '用户列表', 'API', '查看用户列表', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:list');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 2, 'system:user:create', '创建用户', 'API', '创建用户', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:create');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 2, 'system:user:edit', '编辑用户', 'API', '编辑用户', 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:edit');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 2, 'system:user:delete', '删除用户', 'API', '删除用户', 4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:delete');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 2, 'system:user:assignRole', '分配角色', 'API', '为用户分配角色', 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:assignRole');

-- 角色管理权限
INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 3, 'system:role:list', '角色列表', 'API', '查看角色列表', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:role:list');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 3, 'system:role:create', '创建角色', 'API', '创建角色', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:role:create');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 3, 'system:role:edit', '编辑角色', 'API', '编辑角色', 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:role:edit');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 3, 'system:role:delete', '删除角色', 'API', '删除角色', 4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:role:delete');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 3, 'system:role:assignPermission', '分配权限', 'API', '为角色分配权限', 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:role:assignPermission');

-- 文章管理权限
INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 0, 'article', '文章管理', 'MENU', '文章管理菜单', 90
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'article');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 15, 'article:list', '文章列表', 'API', '查看文章列表', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'article:list');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 15, 'article:create', '创建文章', 'API', '创建文章', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'article:create');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 15, 'article:edit', '编辑文章', 'API', '编辑文章', 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'article:edit');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 15, 'article:delete', '删除文章', 'API', '删除文章', 4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'article:delete');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 15, 'article:publish', '发布文章', 'API', '发布文章', 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'article:publish');

-- 分类管理权限
INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 0, 'category', '分类管理', 'MENU', '分类管理菜单', 80
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'category');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 22, 'category:list', '分类列表', 'API', '查看分类列表', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'category:list');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 22, 'category:create', '创建分类', 'API', '创建分类', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'category:create');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 22, 'category:edit', '编辑分类', 'API', '编辑分类', 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'category:edit');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 22, 'category:delete', '删除分类', 'API', '删除分类', 4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'category:delete');

-- 评论管理权限
INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 0, 'comment', '评论管理', 'MENU', '评论管理菜单', 70
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'comment');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 27, 'comment:list', '评论列表', 'API', '查看评论列表', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'comment:list');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 27, 'comment:create', '创建评论', 'API', '创建评论', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'comment:create');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 27, 'comment:edit', '编辑评论', 'API', '编辑评论', 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'comment:edit');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 27, 'comment:delete', '删除评论', 'API', '删除评论', 4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'comment:delete');

INSERT IGNORE INTO sys_permission (parent_id, code, name, type, description, sort_order)
SELECT 27, 'comment:approve', '审核评论', 'API', '审核评论', 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'comment:approve');

-- 为超级管理员角色分配所有权限（使用NOT EXISTS检查）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         CROSS JOIN sys_permission p
WHERE r.code = 'SUPER_ADMIN'
  AND NOT EXISTS (SELECT 1
                  FROM sys_role_permission rp
                  WHERE rp.role_id = r.id
                    AND rp.permission_id = p.id);

-- 为普通管理员角色分配基本管理权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         CROSS JOIN sys_permission p
WHERE r.code = 'ADMIN'
  AND p.code NOT LIKE 'system:permission%'
  AND p.code NOT LIKE 'system:role:assignPermission%'
  AND NOT EXISTS (SELECT 1
                  FROM sys_role_permission rp
                  WHERE rp.role_id = r.id
                    AND rp.permission_id = p.id);

-- 为作者角色分配文章相关权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         CROSS JOIN sys_permission p
WHERE r.code = 'AUTHOR'
  AND (p.code LIKE 'article:%' OR p.code LIKE 'category:list%')
  AND NOT EXISTS (SELECT 1
                  FROM sys_role_permission rp
                  WHERE rp.role_id = r.id
                    AND rp.permission_id = p.id);

-- 为普通用户角色分配基本权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         CROSS JOIN sys_permission p
WHERE r.code = 'USER'
  AND (p.code IN ('article:list', 'comment:create', 'comment:list'))
  AND NOT EXISTS (SELECT 1
                  FROM sys_role_permission rp
                  WHERE rp.role_id = r.id
                    AND rp.permission_id = p.id);

-- 插入默认权限组
INSERT IGNORE INTO sys_permission_group (name, description, sort_order, status)
SELECT '系统管理组', '包含所有系统管理权限', 100, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission_group WHERE name = '系统管理组');

INSERT IGNORE INTO sys_permission_group (name, description, sort_order, status)
SELECT '文章管理组', '包含所有文章管理权限', 90, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission_group WHERE name = '文章管理组');

INSERT IGNORE INTO sys_permission_group (name, description, sort_order, status)
SELECT '用户管理组', '包含用户管理相关权限', 80, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission_group WHERE name = '用户管理组');

-- 为权限组添加权限
-- 系统管理组权限
INSERT IGNORE INTO sys_permission_group_item (group_id, permission_id, sort_order)
SELECT g.id, p.id, 1
FROM sys_permission_group g
         CROSS JOIN sys_permission p
WHERE g.name = '系统管理组'
  AND p.code LIKE 'system:%'
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
  AND p.code LIKE 'article:%'
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

-- 为角色分配权限组（ADMIN角色）
INSERT IGNORE INTO sys_role_permission_group (role_id, group_id)
SELECT r.id, g.id
FROM sys_role r
         CROSS JOIN sys_permission_group g
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (SELECT 1
                  FROM sys_role_permission_group rpg
                  WHERE rpg.role_id = r.id
                    AND rpg.group_id = g.id);

-- 自动同步权限组中的权限到角色权限表
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT rpg.role_id, pgi.permission_id
FROM sys_role_permission_group rpg
         JOIN sys_permission_group_item pgi ON rpg.group_id = pgi.group_id
WHERE NOT EXISTS (SELECT 1
                  FROM sys_role_permission rp
                  WHERE rp.role_id = rpg.role_id
                    AND rp.permission_id = pgi.permission_id);

