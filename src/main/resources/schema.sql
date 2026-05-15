-- schema.sql
# CREATE DATABASE IF NOT EXISTS myblog_sql CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
#
# USE myblog_sql;

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
    content_md    LONGTEXT COMMENT 'MD文章内容',
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
    blog_id     BIGINT NOT NULL COMMENT '关联的文章ID',
    parent_id   BIGINT     DEFAULT 0 COMMENT '父评论ID，0表示顶级评论',
    user_id     BIGINT     NULL COMMENT '用户ID（已登录用户）',
    username    VARCHAR(50) COMMENT '评论者名称',
    email       VARCHAR(100) COMMENT '邮箱',
    avatar_url  VARCHAR(200) COMMENT '头像URL',
    website     VARCHAR(200) COMMENT '个人网站',
    content     TEXT   NOT NULL COMMENT '评论内容',
    status      TINYINT    DEFAULT 0 COMMENT '状态：0=待审核，1=已通过，2=垃圾评论',
    like_count  INT        DEFAULT 0 COMMENT '点赞数',
    device_info VARCHAR(200) COMMENT '设备信息',
    ip_address  VARCHAR(50) COMMENT 'IP地址',
    is_admin    TINYINT(1) DEFAULT 0 COMMENT '是否管理员评论：0=否，1=是',
    is_deleted  TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    create_time DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (blog_id) REFERENCES sys_blog (id) ON DELETE NO ACTION
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='评论表';

# -- 评论点赞表
# CREATE TABLE IF NOT EXISTS sys_comment_like
# (
#     id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞记录ID',
#     comment_id  BIGINT NOT NULL COMMENT '评论ID',
#     user_id     BIGINT NOT NULL COMMENT '用户ID',
#     create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
#     UNIQUE KEY uk_comment_user (comment_id, user_id) COMMENT '防止重复点赞',
#     FOREIGN KEY (comment_id) REFERENCES sys_comment (id) ON DELETE NO ACTION,
#     FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE NO ACTION
# ) ENGINE = InnoDB
#   DEFAULT CHARSET = utf8mb4 COMMENT ='评论点赞表';
#
# -- 标签表
# CREATE TABLE IF NOT EXISTS sys_tag
# (
#     id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
#     name        VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
#     is_deleted  TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
#     create_time DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
#     update_time DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
# ) ENGINE = InnoDB
#   DEFAULT CHARSET = utf8mb4 COMMENT ='标签表';
#
# -- 文章-标签关联表
# CREATE TABLE IF NOT EXISTS sys_blog_tag
# (
#     id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
#     blog_id     BIGINT NOT NULL COMMENT '文章ID',
#     tag_id      BIGINT NOT NULL COMMENT '标签ID',
#     create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
#
#     UNIQUE KEY uk_blog_tag (blog_id, tag_id) COMMENT '防止重复关联',
#     FOREIGN KEY (blog_id) REFERENCES sys_blog (id) ON DELETE NO ACTION,
#     FOREIGN KEY (tag_id) REFERENCES sys_tag (id) ON DELETE NO ACTION
# ) ENGINE = InnoDB
#   DEFAULT CHARSET = utf8mb4 COMMENT ='文章-标签关联表';

# -- 文章点赞表
# CREATE TABLE IF NOT EXISTS sys_blog_like
# (
#     id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞记录ID',
#     blog_id     BIGINT NOT NULL COMMENT '文章ID',
#     user_id     BIGINT NOT NULL COMMENT '用户ID',
#     create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
#
#     UNIQUE KEY uk_blog_user (blog_id, user_id) COMMENT '防止重复点赞',
#     FOREIGN KEY (blog_id) REFERENCES sys_blog (id) ON DELETE NO ACTION,
#     FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE NO ACTION
# ) ENGINE = InnoDB
#   DEFAULT CHARSET = utf8mb4 COMMENT ='文章点赞表';

-- 网站配置表
CREATE TABLE IF NOT EXISTS sys_config
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    config_key      VARCHAR(100) NOT NULL UNIQUE COMMENT '配置项唯一键，如 site_title, seo_keywords',
    config_value    TEXT         NOT NULL COMMENT '配置值（字符串形式存储，应用层按类型解析）',
    data_type       VARCHAR(20)  NOT NULL DEFAULT 'string' COMMENT '数据类型：string, boolean, integer, json, email, url, text',
    validation_rule VARCHAR(255) COMMENT '校验规则：如 max_length=100, regex=..., array_of_strings 等',
    description     VARCHAR(255) COMMENT '配置项说明，用于后台展示',
    is_system       TINYINT(1)            DEFAULT 0 COMMENT '是否系统内置：0=否，1=是（不可删除）',
    is_open         TINYINT(1)            DEFAULT 1 COMMENT '是否公开：0=否，1=是（公开接口可查询）',
    is_deleted      TINYINT(1)            DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    create_time     DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_config_key (config_key) COMMENT '配置键唯一索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '网站全局配置表（键值对）';

-- SEO表
CREATE TABLE IF NOT EXISTS sys_seo
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'SEO配置ID',
    page_type      VARCHAR(50) NOT NULL COMMENT '页面类型：home=首页，article=文章页，category=分类页，tag=标签页，about=关于页，contact=联系页',
    page_id        BIGINT COMMENT '关联页面ID',
    title          VARCHAR(200) COMMENT 'SEO标题（title标签）',
    keywords       VARCHAR(500) COMMENT 'SEO关键词（keywords meta标签，逗号分隔）',
    description    VARCHAR(1500) COMMENT 'SEO描述（description meta标签）',
    og_title       VARCHAR(200) COMMENT 'Open Graph标题（og:title）',
    og_description VARCHAR(1500) COMMENT 'Open Graph描述（og:description）',
    og_image       VARCHAR(500) COMMENT 'Open Graph图片URL（og:image）',
    og_type        VARCHAR(50)  DEFAULT 'website' COMMENT 'Open Graph类型（og:type，如website、article）',
    canonical_url  VARCHAR(500) COMMENT '规范URL（canonical link）',
    robots         VARCHAR(100) DEFAULT 'index,follow' COMMENT 'robots meta标签（如index,follow、noIndex,noFollow）',
    is_deleted     TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    is_system      TINYINT(1)   DEFAULT 0 COMMENT '是否系统内置：0=否，1=是（不可删除）',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_page_type_id (page_type, page_id) COMMENT '页面类型和页面ID唯一索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='SEO配置表';

-- 外链/友情链接表
CREATE TABLE IF NOT EXISTS sys_friend_link
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '外链ID',
    name        VARCHAR(100) NOT NULL COMMENT '链接名称',
    url         VARCHAR(500) NOT NULL COMMENT 'URL地址',
    summary     VARCHAR(500) COMMENT '简介',
    remark      VARCHAR(500) COMMENT '备注',
    image_url   VARCHAR(500) COMMENT '图片URL',
    sort_order  INT        DEFAULT 0 COMMENT '排序顺序（数字越大越靠前）',
    status      TINYINT    DEFAULT 0 COMMENT '状态：0=待审核，1=已通过，2=已拒绝，3=已删除',
    is_deleted  TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    create_time DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='外链/友情链接表';

-- OSS 图片映射表
CREATE TABLE IF NOT EXISTS sys_oss_image
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '图片ID',
    hash          VARCHAR(128) NOT NULL UNIQUE COMMENT '图片哈希值（MD5）',
    original_name VARCHAR(128) COMMENT '原始文件名（截断至128位）',
    object_name   VARCHAR(500) NOT NULL COMMENT 'OSS 对象名称（文件路径）',
    file_size     BIGINT       NOT NULL COMMENT '文件大小（字节）',
    user_id       BIGINT COMMENT '上传用户ID',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_hash (hash) COMMENT '哈希值索引',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    INDEX idx_create_time (create_time) COMMENT '创建时间索引',
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='OSS 图片映射表';

-- 创建索引

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
# CREATE INDEX idx_comment_like_comment ON sys_comment_like (comment_id) COMMENT '点赞评论索引';
# CREATE INDEX idx_comment_like_user ON sys_comment_like (user_id) COMMENT '点赞用户索引';
CREATE INDEX idx_seo_page_type ON sys_seo (page_type) COMMENT 'SEO页面类型索引';
CREATE INDEX idx_seo_page_id ON sys_seo (page_id) COMMENT 'SEO页面ID索引';
CREATE INDEX idx_friend_link_status ON sys_friend_link (status) COMMENT '外链审核状态索引';
CREATE INDEX idx_friend_link_sort ON sys_friend_link (sort_order) COMMENT '外链排序索引';
CREATE INDEX idx_friend_link_create_time ON sys_friend_link (create_time) COMMENT '外链创建时间索引';

/*
 * [schema.sql]
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
 *
 * =======================================
 * 注意：默认数据（角色、权限、配置、SEO等）现在由 Java 代码初始化
 * 请参考: com.jiuliu.myblog_dev.utils.init.DatabaseInitializer
 * =======================================
 */
