# 数据库设计文档

## 数据库概述

| 项目       | 说明                   |
|----------|----------------------|
| **项目**   | 博客系统                 |
| **数据库名** | `myblog_sql`         |
| **字符集**  | `utf8mb4`            |
| **排序规则** | `utf8mb4_unicode_ci` |
| **引擎**   | `InnoDB`             |
| **用途**   | 博客系统数据库              |

---

## 数据库表结构

### 1. 用户表 (`sys_user`)

**用途**：存储系统用户信息

| 字段名         | 类型           | 约束                          | 默认值                         | 说明               |
|-------------|--------------|-----------------------------|-----------------------------|------------------|
| id          | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | -                           | 用户ID             |
| username    | VARCHAR(50)  | UNIQUE, NOT NULL            | -                           | 用户名              |
| nickname    | VARCHAR(50)  | NOT NULL                    | -                           | 昵称               |
| password    | VARCHAR(100) | NOT NULL                    | -                           | 密码（加密后）          |
| email       | VARCHAR(50)  | UNIQUE                      | NULL                        | 邮箱（唯一）           |
| avatar_url  | VARCHAR(200) | -                           | NULL                        | 头像URL            |
| status      | INT          | -                           | 1                           | 状态：0=禁用，1=启用     |
| is_deleted  | TINYINT(1)   | -                           | 0                           | 逻辑删除：0=未删除，1=已删除 |
| create_time | DATETIME     | -                           | CURRENT_TIMESTAMP           | 创建时间             |
| update_time | DATETIME     | -                           | CURRENT_TIMESTAMP ON UPDATE | 更新时间             |

**索引**：

- `idx_user_status(status)` - 状态查询优化

---

### 2. 角色表 (`sys_role`)

**用途**：存储系统角色信息

| 字段名            | 类型           | 约束                          | 默认值                         | 说明               |
|----------------|--------------|-----------------------------|-----------------------------|------------------|
| id             | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | -                           | 角色ID             |
| code           | VARCHAR(50)  | UNIQUE, NOT NULL            | -                           | 角色编码             |
| name           | VARCHAR(50)  | NOT NULL                    | -                           | 角色名称             |
| description    | VARCHAR(200) | -                           | NULL                        | 角色描述             |
| is_super_admin | TINYINT(1)   | -                           | 0                           | 是否超级管理员          |
| is_system      | TINYINT(1)   | -                           | 0                           | 是否系统内置           |
| sort_order     | INT          | -                           | 0                           | 排序顺序             |
| status         | INT          | -                           | 1                           | 状态：0=禁用，1=启用     |
| is_deleted     | TINYINT(1)   | -                           | 0                           | 逻辑删除：0=未删除，1=已删除 |
| create_time    | DATETIME     | -                           | CURRENT_TIMESTAMP           | 创建时间             |
| update_time    | DATETIME     | -                           | CURRENT_TIMESTAMP ON UPDATE | 更新时间             |

**索引**：

- `idx_role_code(code)` - 编码查询优化
- `idx_role_status(status)` - 状态查询优化
- `idx_role_super_admin(is_super_admin)` - 超级管理员查询

**默认角色**：

- `SUPER_ADMIN` - 超级管理员
- `ADMIN` - 普通管理员
- `AUTHOR` - 文章作者
- `USER` - 普通用户

---

### 3. 权限表 (`sys_permission`)

**用途**：存储系统权限信息（权限编码、名称、描述等）

| 字段名         | 类型           | 约束                          | 默认值               | 说明       |
|-------------|--------------|-----------------------------|-------------------|----------|
| id          | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | -                 | 权限ID     |
| code        | VARCHAR(100) | UNIQUE, NOT NULL            | -                 | 权限编码（唯一） |
| name        | VARCHAR(50)  | NOT NULL                    | -                 | 权限名称     |
| description | VARCHAR(200) | -                           | NULL              | 权限描述     |
| sort_order  | INT          | -                           | 0                 | 排序顺序     |
| create_time | DATETIME     | -                           | CURRENT_TIMESTAMP | 创建时间     |

**索引**：

- `idx_permission_code(code)` - 权限编码索引

**默认权限**：

- 系统管理权限 (`system.*`)、用户管理 (`system:user:*`)、角色管理 (`system:role:*`)、权限组管理 (`system:permission_group:*`)
- 文章管理 (`article:*`)、分类管理 (`category:*`)、评论管理 (`comment:*`)、SEO 管理 (`seo:*`)、网站配置 (`config:*`)

---

### 4. 用户-角色关联表 (`sys_user_role`)

**用途**：存储用户与角色的多对多关系

| 字段名         | 类型       | 约束                          | 默认值               | 说明   |
|-------------|----------|-----------------------------|-------------------|------|
| id          | BIGINT   | PRIMARY KEY, AUTO_INCREMENT | -                 | 关联ID |
| user_id     | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 用户ID |
| role_id     | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 角色ID |
| create_time | DATETIME | -                           | CURRENT_TIMESTAMP | 创建时间 |

**约束**：

- `UNIQUE KEY uk_user_role(user_id, role_id)` - 防止重复关联
- `FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE NO ACTION`
- `FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE NO ACTION`

**索引**：

- `idx_user_role_user(user_id)`
- `idx_user_role_role(role_id)`

---

### 5. 角色-权限关联表 (`sys_role_permission`)

**用途**：存储角色与权限的多对多关系

| 字段名           | 类型       | 约束                          | 默认值               | 说明   |
|---------------|----------|-----------------------------|-------------------|------|
| id            | BIGINT   | PRIMARY KEY, AUTO_INCREMENT | -                 | 关联ID |
| role_id       | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 角色ID |
| permission_id | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 权限ID |
| create_time   | DATETIME | -                           | CURRENT_TIMESTAMP | 创建时间 |

**约束**：

- `UNIQUE KEY uk_role_permission(role_id, permission_id)` - 防止重复关联
- `FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE NO ACTION`
- `FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE NO ACTION`

**索引**：

- `idx_role_permission_role(role_id)`
- `idx_role_permission_permission(permission_id)`

---

### 6. 权限组表 (`sys_permission_group`)

**用途**：用于组织和管理权限组

| 字段名         | 类型           | 约束                          | 默认值                         | 说明                   |
|-------------|--------------|-----------------------------|-----------------------------|----------------------|
| id          | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | -                           | 权限组ID                |
| name        | VARCHAR(50)  | NOT NULL                    | -                           | 权限组名称                |
| description | VARCHAR(200) | -                           | NULL                        | 权限组描述                |
| sort_order  | INT          | -                           | 0                           | 排序顺序                 |
| status      | INT          | -                           | 1                           | 状态：0=禁用，1=启用         |
| is_system   | TINYINT(1)   | -                           | 0                           | 是否系统内置：0=否，1=是（不可删除） |
| is_deleted  | TINYINT(1)   | -                           | 0                           | 逻辑删除：0=未删除，1=已删除     |
| create_time | DATETIME     | -                           | CURRENT_TIMESTAMP           | 创建时间                 |
| update_time | DATETIME     | -                           | CURRENT_TIMESTAMP ON UPDATE | 更新时间                 |

**默认权限组**：

- 系统管理组
- 文章管理组
- 用户管理组

---

### 7. 权限-权限组关联表 (`sys_permission_group_item`)

**用途**：关联权限和权限组

| 字段名           | 类型       | 约束                          | 默认值               | 说明    |
|---------------|----------|-----------------------------|-------------------|-------|
| id            | BIGINT   | PRIMARY KEY, AUTO_INCREMENT | -                 | 关联ID  |
| group_id      | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 权限组ID |
| permission_id | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 权限ID  |
| sort_order    | INT      | -                           | 0                 | 排序顺序  |
| create_time   | DATETIME | -                           | CURRENT_TIMESTAMP | 创建时间  |

**约束**：

- `UNIQUE KEY uk_group_permission(group_id, permission_id)` - 防止重复关联
- `FOREIGN KEY (group_id) REFERENCES sys_permission_group(id) ON DELETE NO ACTION`
- `FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE NO ACTION`

---

### 8. 角色-权限组关联表 (`sys_role_permission_group`)

**用途**：批量分配权限（通过权限组）

| 字段名         | 类型       | 约束                          | 默认值               | 说明    |
|-------------|----------|-----------------------------|-------------------|-------|
| id          | BIGINT   | PRIMARY KEY, AUTO_INCREMENT | -                 | 关联ID  |
| role_id     | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 角色ID  |
| group_id    | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 权限组ID |
| create_time | DATETIME | -                           | CURRENT_TIMESTAMP | 创建时间  |

**约束**：

- `UNIQUE KEY uk_role_group(role_id, group_id)` - 防止重复关联
- `FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE NO ACTION`
- `FOREIGN KEY (group_id) REFERENCES sys_permission_group(id) ON DELETE NO ACTION`

---

### 9. 分类表 (`sys_category`)

**用途**：文章分类管理

| 字段名         | 类型           | 约束                          | 默认值                         | 说明               |
|-------------|--------------|-----------------------------|-----------------------------|------------------|
| id          | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | -                           | 分类ID             |
| name        | VARCHAR(50)  | NOT NULL                    | -                           | 分类名称             |
| description | VARCHAR(200) | -                           | NULL                        | 分类描述             |
| sort_order  | INT          | -                           | 0                           | 排序顺序（数字越大越靠前）    |
| create_time | DATETIME     | -                           | CURRENT_TIMESTAMP           | 创建时间             |
| is_hidden   | TINYINT(1)   | -                           | 0                           | 是否隐藏：0=显示，1=隐藏   |
| is_deleted  | TINYINT(1)   | -                           | 0                           | 逻辑删除：0=未删除，1=已删除 |
| update_time | DATETIME     | -                           | CURRENT_TIMESTAMP ON UPDATE | 更新时间             |

**索引**：

- `idx_category_hidden(is_hidden)`
- `idx_category_sort(sort_order)`

---

### 10. 博客/文章表 (`sys_blog`)

**用途**：存储博客文章

| 字段名           | 类型           | 约束                          | 默认值                         | 说明               |
|---------------|--------------|-----------------------------|-----------------------------|------------------|
| id            | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | -                           | 文章ID             |
| category_id   | BIGINT       | FOREIGN KEY                 | NULL                        | 分类ID             |
| title         | VARCHAR(200) | NOT NULL                    | -                           | 文章标题             |
| summary       | VARCHAR(500) | -                           | NULL                        | 文章摘要             |
| content       | LONGTEXT     | -                           | NULL                        | 文章内容             |
| cover_image   | VARCHAR(200) | -                           | NULL                        | 封面图片             |
| tags          | VARCHAR(200) | -                           | NULL                        | 标签（逗号分隔）         |
| author_id     | BIGINT       | FOREIGN KEY                 | NULL                        | 作者ID             |
| view_count    | INT          | -                           | 0                           | 浏览量              |
| comment_count | INT          | -                           | 0                           | 评论数              |
| like_count    | INT          | -                           | 0                           | 点赞数              |
| is_hidden     | TINYINT(1)   | -                           | 0                           | 是否隐藏：0=公开，1=私密   |
| is_top        | TINYINT(1)   | -                           | 0                           | 是否置顶：0=否，1=是     |
| is_recommend  | TINYINT(1)   | -                           | 0                           | 是否推荐：0=否，1=是     |
| is_deleted    | TINYINT(1)   | -                           | 0                           | 逻辑删除：0=未删除，1=已删除 |
| create_time   | DATETIME     | -                           | CURRENT_TIMESTAMP           | 创建时间             |
| update_time   | DATETIME     | -                           | CURRENT_TIMESTAMP ON UPDATE | 更新时间             |

**约束**：

- `FOREIGN KEY (category_id) REFERENCES sys_category(id) ON DELETE NO ACTION`
- `FOREIGN KEY (author_id) REFERENCES sys_user(id) ON DELETE NO ACTION`

**索引**：

- `idx_blog_category(category_id)`
- `idx_blog_author(author_id)`
- `idx_blog_create_time(create_time)`
- `idx_blog_hidden(is_hidden)`
- `idx_blog_top(is_top)`

---

### 11. 评论表 (`sys_comment`)

**用途**：文章评论管理

| 字段名         | 类型           | 约束                          | 默认值                         | 说明                          |
|-------------|--------------|-----------------------------|-----------------------------|-----------------------------|
| id          | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | -                           | 评论ID                        |
| blog_id     | BIGINT       | FOREIGN KEY, NOT NULL       | -                           | 文章ID                        |
| parent_id   | BIGINT       | -                           | 0                           | 父评论ID                       |
| user_id     | BIGINT       | FOREIGN KEY                 | NULL                        | 用户ID                        |
| username    | VARCHAR(50)  | NOT NULL                    | -                           | 评论者名称                       |
| email       | VARCHAR(100) | -                           | NULL                        | 邮箱                          |
| avatar_url  | VARCHAR(200) | -                           | NULL                        | 头像URL                       |
| website     | VARCHAR(200) | -                           | NULL                        | 个人网站                        |
| content     | TEXT         | NOT NULL                    | -                           | 评论内容                        |
| status      | TINYINT      | -                           | 0                           | 状态：0=待审核，1=已通过，2=垃圾评论，3=已删除 |
| like_count  | INT          | -                           | 0                           | 点赞数                         |
| device_info | VARCHAR(200) | -                           | NULL                        | 设备信息                        |
| ip_address  | VARCHAR(50)  | -                           | NULL                        | IP地址                        |
| is_admin    | TINYINT(1)   | -                           | 0                           | 是否管理员评论：0=否，1=是             |
| is_deleted  | TINYINT(1)   | -                           | 0                           | 逻辑删除：0=未删除，1=已删除            |
| create_time | DATETIME     | -                           | CURRENT_TIMESTAMP           | 创建时间                        |
| update_time | DATETIME     | -                           | CURRENT_TIMESTAMP ON UPDATE | 更新时间                        |

**约束**：

- `FOREIGN KEY (blog_id) REFERENCES sys_blog(id) ON DELETE NO ACTION`
- `FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE NO ACTION`

**索引**：

- `idx_comment_blog(blog_id)`
- `idx_comment_parent(parent_id)`
- `idx_comment_user(user_id)`
- `idx_comment_status(status)`
- `idx_comment_create_time(create_time)`

---

### 12. 评论点赞表 (`sys_comment_like`)

**用途**：记录评论点赞信息

| 字段名         | 类型       | 约束                          | 默认值               | 说明     |
|-------------|----------|-----------------------------|-------------------|--------|
| id          | BIGINT   | PRIMARY KEY, AUTO_INCREMENT | -                 | 点赞记录ID |
| comment_id  | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 评论ID   |
| user_id     | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 用户ID   |
| create_time | DATETIME | -                           | CURRENT_TIMESTAMP | 创建时间   |

**约束**：

- `UNIQUE KEY uk_comment_user(comment_id, user_id)` - 防止重复点赞
- `FOREIGN KEY (comment_id) REFERENCES sys_comment(id) ON DELETE NO ACTION`
- `FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE NO ACTION`

**索引**：

- `idx_comment_like_comment(comment_id)`
- `idx_comment_like_user(user_id)`

---

### 13. 标签表 (`sys_tag`)

**用途**：文章标签管理

| 字段名         | 类型          | 约束                          | 默认值                         | 说明               |
|-------------|-------------|-----------------------------|-----------------------------|------------------|
| id          | BIGINT      | PRIMARY KEY, AUTO_INCREMENT | -                           | 标签ID             |
| name        | VARCHAR(50) | UNIQUE, NOT NULL            | -                           | 标签名称（唯一）         |
| is_deleted  | TINYINT(1)  | -                           | 0                           | 逻辑删除：0=未删除，1=已删除 |
| create_time | DATETIME    | -                           | CURRENT_TIMESTAMP           | 创建时间             |
| update_time | DATETIME    | -                           | CURRENT_TIMESTAMP ON UPDATE | 更新时间             |

---

### 14. 文章-标签关联表 (`sys_blog_tag`)

**用途**：文章与标签的多对多关系

| 字段名         | 类型       | 约束                          | 默认值               | 说明   |
|-------------|----------|-----------------------------|-------------------|------|
| id          | BIGINT   | PRIMARY KEY, AUTO_INCREMENT | -                 | 关联ID |
| blog_id     | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 文章ID |
| tag_id      | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 标签ID |
| create_time | DATETIME | -                           | CURRENT_TIMESTAMP | 创建时间 |

**约束**：

- `UNIQUE KEY uk_blog_tag(blog_id, tag_id)` - 防止重复关联
- `FOREIGN KEY (blog_id) REFERENCES sys_blog(id) ON DELETE NO ACTION`
- `FOREIGN KEY (tag_id) REFERENCES sys_tag(id) ON DELETE NO ACTION`

---

### 15. 文章点赞表 (`sys_blog_like`)

**用途**：记录文章点赞信息

| 字段名         | 类型       | 约束                          | 默认值               | 说明     |
|-------------|----------|-----------------------------|-------------------|--------|
| id          | BIGINT   | PRIMARY KEY, AUTO_INCREMENT | -                 | 点赞记录ID |
| blog_id     | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 文章ID   |
| user_id     | BIGINT   | FOREIGN KEY, NOT NULL       | -                 | 用户ID   |
| create_time | DATETIME | -                           | CURRENT_TIMESTAMP | 创建时间   |

**约束**：

- `UNIQUE KEY uk_blog_user(blog_id, user_id)` - 防止重复点赞
- `FOREIGN KEY (blog_id) REFERENCES sys_blog(id) ON DELETE NO ACTION`
- `FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE NO ACTION`

---

## 新增表（与 schema.sql 一致）

### 16. 网站配置表 (`sys_config`)

**用途**：网站全局配置（键值对），区分系统内置与用户自定义配置

| 字段名             | 类型           | 约束                          | 默认值                         | 说明                                                    |
|-----------------|--------------|-----------------------------|-----------------------------|-------------------------------------------------------|
| id              | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | -                           | 配置ID                                                  |
| config_key      | VARCHAR(100) | UNIQUE, NOT NULL            | -                           | 配置项唯一键，如 site_title, seo_keywords                     |
| config_value    | TEXT         | NOT NULL                    | -                           | 配置值（字符串形式存储，应用层按类型解析）                                 |
| data_type       | VARCHAR(20)  | NOT NULL                    | 'string'                    | 数据类型：string, boolean, integer, json, email, url, text |
| validation_rule | VARCHAR(255) | -                           | NULL                        | 校验规则：如 max_length=100, regex=... 等                    |
| description     | VARCHAR(255) | -                           | NULL                        | 配置项说明，用于后台展示                                          |
| is_system       | TINYINT(1)   | -                           | 0                           | 是否系统内置：0=否，1=是（不可删除）                                  |
| is_deleted      | TINYINT(1)   | -                           | 0                           | 逻辑删除：0=未删除，1=已删除                                      |
| create_time     | DATETIME     | -                           | CURRENT_TIMESTAMP           | 创建时间                                                  |
| update_time     | DATETIME     | -                           | CURRENT_TIMESTAMP ON UPDATE | 更新时间                                                  |

**约束**：

- `UNIQUE KEY uk_config_key(config_key)` - 配置键唯一索引

---

### 17. SEO 配置表 (`sys_seo`)

**用途**：各页面类型的 SEO 配置（title、keywords、description、Open Graph 等）

| 字段名            | 类型            | 约束                          | 默认值                         | 说明                                                   |
|----------------|---------------|-----------------------------|-----------------------------|------------------------------------------------------|
| id             | BIGINT        | PRIMARY KEY, AUTO_INCREMENT | -                           | SEO 配置ID                                             |
| page_type      | VARCHAR(50)   | NOT NULL                    | -                           | 页面类型：home/article/category/tag/about/contact/links 等 |
| page_id        | BIGINT        | -                           | NULL                        | 关联页面ID                                               |
| title          | VARCHAR(200)  | -                           | NULL                        | SEO 标题（title 标签）                                     |
| keywords       | VARCHAR(500)  | -                           | NULL                        | SEO 关键词（keywords meta，逗号分隔）                          |
| description    | VARCHAR(1500) | -                           | NULL                        | SEO 描述（description meta）                             |
| og_title       | VARCHAR(200)  | -                           | NULL                        | Open Graph 标题（og:title）                              |
| og_description | VARCHAR(1500) | -                           | NULL                        | Open Graph 描述（og:description）                        |
| og_image       | VARCHAR(500)  | -                           | NULL                        | Open Graph 图片 URL（og:image）                          |
| og_type        | VARCHAR(50)   | -                           | 'website'                   | Open Graph 类型（og:type，如 website、article）             |
| canonical_url  | VARCHAR(500)  | -                           | NULL                        | 规范 URL（canonical link）                               |
| robots         | VARCHAR(100)  | -                           | 'index,follow'              | robots meta（如 index,follow、noIndex,noFollow）         |
| is_deleted     | TINYINT(1)    | -                           | 0                           | 逻辑删除：0=未删除，1=已删除                                     |
| is_system      | TINYINT(1)    | -                           | 0                           | 是否系统内置：0=否，1=是（不可删除）                                 |
| create_time    | DATETIME      | -                           | CURRENT_TIMESTAMP           | 创建时间                                                 |
| update_time    | DATETIME      | -                           | CURRENT_TIMESTAMP ON UPDATE | 更新时间                                                 |

**约束**：

- `UNIQUE KEY uk_page_type_id(page_type, page_id)` - 页面类型与页面ID 唯一索引

**索引**：

- `idx_seo_page_type(page_type)`
- `idx_seo_page_id(page_id)`

---

### 18. 外链/友情链接表 (`sys_friend_link`)

**用途**：存储外链/友情链接，支持审核与排序

| 字段名         | 类型           | 约束                          | 默认值                         | 说明                         |
|-------------|--------------|-----------------------------|-----------------------------|----------------------------|
| id          | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | -                           | 外链ID                       |
| name        | VARCHAR(100) | NOT NULL                    | -                           | 链接名称                       |
| url         | VARCHAR(500) | NOT NULL                    | -                           | URL 地址                     |
| summary     | VARCHAR(500) | -                           | NULL                        | 简介                         |
| remark      | VARCHAR(500) | -                           | NULL                        | 备注                         |
| image_url   | VARCHAR(500) | -                           | NULL                        | 图片 URL                     |
| sort_order  | INT          | -                           | 0                           | 排序顺序（数字越大越靠前）              |
| status      | TINYINT      | -                           | 0                           | 状态：0=待审核，1=已通过，2=已拒绝，3=已删除 |
| is_deleted  | TINYINT(1)   | -                           | 0                           | 逻辑删除：0=未删除，1=已删除           |
| create_time | DATETIME     | -                           | CURRENT_TIMESTAMP           | 创建时间                       |
| update_time | DATETIME     | -                           | CURRENT_TIMESTAMP ON UPDATE | 更新时间                       |

**索引**：

- `idx_friend_link_status(status)` - 外链审核状态索引
- `idx_friend_link_sort(sort_order)` - 外链排序索引
- `idx_friend_link_create_time(create_time)` - 外链创建时间索引  