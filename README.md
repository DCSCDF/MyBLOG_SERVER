<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">MyBlog 后端应用</h1>
<h4 align="center">使用 SpringBoot 框架，方法实现采用更安全的方式。</h4>
<div align="center">



[![My Skills](https://skillicons.dev/icons?i=java,spring,mysql,git&theme=light)](https://skillicons.dev)
</div>

---
## 部署改项目：

如果想部署该项目，请跳转到下方链接根据文档安装发行版。
> https://github.com/DCSCDF/MYBLOG-Distribution
---

![MySQL](https://img.shields.io/badge/MySQL-8.4.0-00758F?logo=mysql&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.9-6DB33F?logo=springboot&logoColor=white)

> ### API对接 与数据库结构等文档
> - [用户认证API文档 `Auth_Api`](Document/Auth_Api.md)
> - [角色与权限组API文档 `Role_And_PermissionGroup_Api`](Document/Role_And_PermissionGroup_Api.md)
> - [数据库结构 `Myblog_Sql`](Document/Myblog_Sql.md)
> - [SEO配置接口文档 `Seo_Api`(已禁用 启用需要移除@Disabled注解)](Document/Seo_Api.md)
> - [SEO配置接口文档 `PublicSeo_Api`(已禁用 启用需要移除@Disabled注解)](Document/PublicSeo_Api.md)
> - [账号管理（用户管理）接口文档 `User_Manage_Api`](Document/User_Manage_Api.md)
> - [站点配置接口文档 `Config_Api`](Document/Config_Api.md)
> - [限流逻辑说明文档 `RateLimit`](Document/RateLimit.md)
> - [权限层级与父子关系说明 `Permission_Hierarchy`](Document/Permission_Hierarchy.md)
> - [分类API文档 `Category_Api`](Document/Category_Api.md)
> - [网站友链API文档 `FriendLink_Api`](Document/FriendLink_Api.md)
> - [文章管理文档 `Blog_Api`](Document/Blog_Api.md)
> - [公共文章API文档 `PublicArticle_Api`](Document/PublicArticle_Api.md)
> - [评论与全局评论API文档 `Comment_Api`](Document/Comment_Api.md)
> - [公共评论API文档 `PublicComment_Api`](Document/PublicComment_Api.md)
> - [全局文章API文档 `GlobalArticle_Api`](Document/GlobalArticle_Api.md)
> - [公共分类API文档 `PublicCategory_Api`](Document/PublicCategory_Api.md)
> - [OSS对象存储API文档 `OSS_Api`](Document/OSS_Api.md)
> - [全局OSS管理API文档 `GlobalOss_Api`](Document/GlobalOss_Api.md)
> - [邮箱接口文档 `Mail_Api`](Document/Mail_Api.md)

---

## 项目开发参考

## 一、模块架构图

```mermaid
graph TD
    subgraph 用户管理模块["用户管理模块"]
        SysUser[用户 SysUser]
        SysRole[角色 SysRole]
        SysPermission[权限 SysPermission]
        SysPermissionGroup[权限组 SysPermissionGroup]
        SysUserRole[(用户角色关联)]
        SysRolePermission[(角色权限关联)]
        SysRolePermissionGroup[(角色权限组关联)]
        SysPermissionGroupItem[(权限组项关联)]
    end
    
    subgraph 博客模块["博客模块"]
        SysBlog[博客文章 SysBlog]
        SysCategory[分类 SysCategory]
        SysComment[评论 SysComment]
        SysOssImage[OSS图片 SysOssImage]
    end
    
    subgraph 配置模块["配置模块"]
        SysConfig[系统配置 SysConfig]
        SysSeo[SEO配置 SysSeo 已禁用]
        SysFriendLink[友情链接 SysFriendLink]
    end
    
    %% 用户管理内部关系
    SysUser -->|拥有| SysUserRole
    SysRole -->|分配给| SysUserRole
    
    SysRole -->|拥有| SysRolePermission
    SysPermission -->|被分配| SysRolePermission
    
    SysRole -->|拥有| SysRolePermissionGroup
    SysPermissionGroup -->|被分配| SysRolePermissionGroup
    
    SysPermission -->|属于| SysPermissionGroupItem
    SysPermissionGroup -->|包含| SysPermissionGroupItem
    
    %% 博客模块内部关系
    SysCategory -->|包含| SysBlog
    SysBlog -->|拥有| SysComment
    SysComment -->|回复| SysComment
    
    %% 跨模块关系
    SysUser -->|作者| SysBlog
    SysUser -->|评论者| SysComment
    SysUser -->|上传者| SysOssImage
```

## 二、详细实体关系图

```mermaid
erDiagram
    %% 实体定义 - 用户管理
    SysUser {
        bigint id PK "主键"
        varchar username UK "用户名"
        varchar nickname "昵称"
        varchar password "密码"
        varchar email UK "邮箱"
        varchar avatar_url "头像URL"
        tinyint status "状态"
        tinyint is_deleted "删除标记"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
    }
    
    SysRole {
        bigint id PK
        varchar code UK
        varchar name
        varchar description
        tinyint is_super_admin
        tinyint is_system
        tinyint is_deleted
        int sort_order
        tinyint status
        datetime create_time
        datetime update_time
    }
    
    SysPermission {
        bigint id PK
        varchar code UK
        varchar name
        varchar description
        int sort_order
        datetime create_time
    }
    
    SysPermissionGroup {
        bigint id PK
        varchar name
        varchar description
        int sort_order
        tinyint status
        tinyint is_system
        tinyint is_deleted
        datetime create_time
        datetime update_time
    }
    
    %% 关联表
    SysUserRole {
        bigint id PK
        bigint user_id FK
        bigint role_id FK
        datetime create_time
    }
    
    SysRolePermission {
        bigint id PK
        bigint role_id FK
        bigint permission_id FK
        datetime create_time
    }
    
    SysRolePermissionGroup {
        bigint id PK
        bigint role_id FK
        bigint group_id FK
        datetime create_time
    }
    
    SysPermissionGroupItem {
        bigint id PK
        bigint group_id FK
        bigint permission_id FK
        int sort_order
        datetime create_time
    }
    
    %% 实体定义 - 博客模块
    SysBlog {
        bigint id PK
        bigint category_id FK
        varchar title
        varchar summary
        longtext content_md
        varchar cover_image
        varchar tags
        bigint author_id FK
        int comment_count
        tinyint is_hidden
        tinyint is_top
        tinyint is_recommend
        tinyint is_deleted
        datetime create_time
        datetime update_time
    }
    
    SysCategory {
        bigint id PK
        varchar name
        varchar description
        int sort_order
        datetime create_time
        tinyint is_hidden
        datetime update_time
    }
    
    SysComment {
        bigint id PK
        bigint blog_id FK
        bigint parent_id FK
        bigint user_id FK
        varchar username
        varchar email
        varchar avatar_url
        varchar website
        text content
        tinyint status
        int like_count
        varchar device_info
        varchar ip_address
        tinyint is_admin
        tinyint is_deleted
        datetime create_time
        datetime update_time
    }
    
    SysOssImage {
        bigint id PK
        varchar hash
        varchar original_name
        varchar object_name
        bigint file_size
        bigint user_id FK
        datetime create_time
    }
    
    %% 实体定义 - 配置模块
    SysFriendLink {
        bigint id PK
        varchar name
        varchar url
        varchar summary
        varchar remark
        varchar image_url
        int sort_order
        tinyint status
        tinyint is_deleted
        datetime create_time
        datetime update_time
    }
    
    SysSeo {
        bigint id PK
        varchar page_type
        bigint page_id
        varchar title
        varchar keywords
        varchar description
        varchar og_title
        varchar og_description
        varchar og_image
        varchar og_type
        varchar canonical_url
        varchar robots
        tinyint is_deleted
        tinyint is_system
        datetime create_time
        datetime update_time
    }
    
    SysConfig {
        bigint id PK
        varchar config_key
        varchar config_value
        varchar data_type
        varchar validation_rule
        varchar description
        tinyint is_system
        tinyint is_open
        tinyint is_deleted
        datetime create_time
        datetime update_time
    }
    
    %% 关系定义
    SysUser ||--o{ SysUserRole : "拥有"
    SysRole ||--o{ SysUserRole : "分配给"
    
    SysRole ||--o{ SysRolePermission : "拥有"
    SysPermission ||--o{ SysRolePermission : "被分配"
    
    SysRole ||--o{ SysRolePermissionGroup : "拥有"
    SysPermissionGroup ||--o{ SysRolePermissionGroup : "被分配"
    
    SysPermission ||--o{ SysPermissionGroupItem : "属于"
    SysPermissionGroup ||--o{ SysPermissionGroupItem : "包含"
    
    SysUser ||--o{ SysBlog : "发布"
    SysCategory ||--o{ SysBlog : "分类"
    SysBlog ||--o{ SysComment : "包含"
    SysUser ||--o{ SysComment : "发表"
    SysComment ||--o{ SysComment : "回复"
    SysUser ||--o{ SysOssImage : "上传"
```

## 三、关系矩阵表

### 用户管理模块关系

| 关系类型 | 主实体 | 关联实体 | 关系说明 | 基数 |
|---------|-------|---------|---------|-----|
| 用户角色 | SysUser | SysUserRole | 用户拥有角色 | 1:N |
| 角色分配 | SysRole | SysUserRole | 角色分配给用户 | 1:N |
| 角色权限 | SysRole | SysRolePermission | 角色拥有权限 | 1:N |
| 权限分配 | SysPermission | SysRolePermission | 权限分配给角色 | 1:N |
| 角色权限组 | SysRole | SysRolePermissionGroup | 角色拥有权限组 | 1:N |
| 权限组分配 | SysPermissionGroup | SysRolePermissionGroup | 权限组分配给角色 | 1:N |
| 权限归属 | SysPermission | SysPermissionGroupItem | 权限属于权限组 | 1:N |
| 权限组包含 | SysPermissionGroup | SysPermissionGroupItem | 权限组包含权限 | 1:N |

### 博客模块关系

| 关系类型 | 主实体 | 关联实体 | 关系说明 | 基数 |
|---------|-------|---------|---------|-----|
| 作者关系 | SysUser | SysBlog | 用户发布文章 | 1:N |
| 分类关系 | SysCategory | SysBlog | 分类包含文章 | 1:N |
| 文章评论 | SysBlog | SysComment | 文章包含评论 | 1:N |
| 用户评论 | SysUser | SysComment | 用户发表评论 | 1:N |
| 评论回复 | SysComment | SysComment | 评论回复评论 | 1:N |
| 用户上传 | SysUser | SysOssImage | 用户上传图片 | 1:N |

## 四、实体分类汇总

| 模块 | 实体名称 | 表名 | 说明 |
|-----|---------|-----|------|
| **核心业务** | 用户 | sys_user | 系统用户信息 |
| | 博客文章 | sys_blog | 博客文章内容 |
| | 分类 | sys_category | 文章分类 |
| | 评论 | sys_comment | 文章评论 |
| **权限管理** | 角色 | sys_role | 用户角色定义 |
| | 权限 | sys_permission | 系统权限定义 |
| | 权限组 | sys_permission_group | 权限分组 |
| | 用户角色关联 | sys_user_role | 用户-角色关系 |
| | 角色权限关联 | sys_role_permission | 角色-权限关系 |
| | 角色权限组关联 | sys_role_permission_group | 角色-权限组关系 |
| | 权限组项关联 | sys_permission_group_item | 权限-权限组关系 |
| **配置管理** | 系统配置 | sys_config | 系统参数配置 |
| | SEO配置 | sys_seo | 页面SEO设置 |
| **其他** | 友情链接 | sys_friend_link | 友情链接管理 |
| | OSS图片 | sys_oss_image | 图片存储记录 |

## 五、图表说明

- **矩形框**：表示实体（数据库表）
- **圆角矩形框**：表示模块分组
- **菱形框**：表示关系（由Mermaid自动生成）
- **PK**：主键（Primary Key）
- **FK**：外键（Foreign Key）
- **UK**：唯一键（Unique Key）
- **1:N**：一对多关系
- **N:M**：多对多关系（通过中间表实现）

## 六、实体关系流程图

```mermaid
flowchart LR
    A[用户登录] --> B{验证权限}
    B -->|通过| C[访问资源]
    B -->|拒绝| D[返回错误]
    
    subgraph 权限验证流程
        B --> E[检查用户角色]
        E --> F[检查角色权限]
        F --> G[检查权限组]
    end
    
    subgraph 内容发布流程
        H[创建文章] --> I[选择分类]
        I --> J[上传图片]
        J --> K[保存文章]
    end
    
    subgraph 评论流程
        L[发表评论] --> M[审核状态]
        M -->|通过| N[显示评论]
        M -->|拒绝| O[隐藏评论]
    end
```

---

## 项目结构
```
src/main/java/com/jiuliu/myblog_dev/
├── config/                 # 配置类
│   ├── CorsConfig
│   ├── GlobalExceptionHandler
│   ├── RsaKeyConfig
│   ├── Captcha/
│   ├── rsa/
│   ├── satoken/
│   ├── security/
│   └── validation/
├── controller/user/        # 控制器（按业务域划分）
│   ├── auth/
│   └── permission/
├── dto/                    # 数据传输对象
├── entity/                 # 实体类
├── mapper/                 # MyBatis Mapper
├── service/user/           # 服务层（接口 + 实现分离）
│   ├── auth/
│   └── permission/
└── utils/                  # 工具类
```


### 鉴权 API 说明

您可以这样使用鉴权：

``` java

// 获取权限列表
List<String> permissionList = StpUtil.getPermissionList();

// 判断权限
boolean hasPermission = StpUtil.hasPermission("system:user:list");

// 检查权限（未通过则抛出异常）
StpUtil.checkPermission("system:user:list");

// 检查多个权限（全部通过）
StpUtil.checkPermissionAnd("system:user:list","system:user:create");

// 检查多个权限（任一通过）
StpUtil.checkPermissionOr("system:user:list","system:user:delete");

// 获取角色列表
List<String> roleList = StpUtil.getRoleList();

// 判断角色
boolean hasRole = StpUtil.hasRole("ADMIN");

// 检查角色（未通过则抛出异常）
StpUtil.checkRole("ADMIN");

// 检查多个角色（全部通过）
StpUtil.checkRoleAnd("ADMIN","SUPER_ADMIN");

// 检查多个角色（任一通过）
StpUtil.checkRoleOr("ADMIN","SUPER_ADMIN");
```

控制器中使用权限验证：

``` java
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // 参考数据库的 sys_permission 表中的 system:user:list 权限
    @SaCheckPermission("system:user:list")
    @GetMapping("/users")
    public SaResult getUserList() {
        // 业务逻辑
        return SaResult.ok("用户列表");
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/users")
    public SaResult createUser() {
        // 业务逻辑
        return SaResult.ok("用户创建成功");
    }
}
```

### 行为验证码（Tianai-Captcha，基于 /api/captcha 前缀）：

当前项目使用 **Tianai-Captcha** 作为行为验证码组件，接口说明如下：

| 接口                    | 方法  | 说明                                       |
|-----------------------|-----|------------------------------------------|
| `/api/captcha/gen`    | GET | 生成验证码（默认滑块，可通过 `type` 指定类型）      |
| `/api/captcha/check`  | POST| 校验用户行为轨迹（滑动 / 旋转 / 文字点选等）           |
| `/api/captcha/verify` | GET | 二次验证（需在配置中开启 `captcha.secondary.enabled`） |

验证码背景图片从 `resources/images` 目录加载（例如 `a.png`、`b.png`、`c.png`、`48.png`），跨域策略统一复用项目的 `CorsConfig` 配置。 

