<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">MyBlog 后端应用</h1>
<h4 align="center">使用 SpringBoot 框架，方法实现采用更安全的方式。</h4>
<div align="center">

[![My Skills](https://skillicons.dev/icons?i=java,spring,mysql,git&theme=light)](https://skillicons.dev)
</div>

---

![MySQL](https://img.shields.io/badge/MySQL-8.4.0-00758F?logo=mysql&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.9-6DB33F?logo=springboot&logoColor=white)

> ### API 与数据库等文档
> - [用户API文档（UserAPI）](Document/USER_API.md)
> - [数据库结构（UserAPI）](Document/SQL.md)

---

### 关系参考图：
![用户关系图](Document/img/img.png)
![数据库ER图](Document/img/img_1.png)

### 鉴权 API 说明
您可以这样使用鉴权：

```java
// 获取权限列表
List<String> permissionList = StpUtil.getPermissionList();

// 判断权限
boolean hasPermission = StpUtil.hasPermission("system:user:list");

// 检查权限（未通过则抛出异常）
StpUtil.checkPermission("system:user:list");

// 检查多个权限（全部通过）
StpUtil.checkPermissionAnd("system:user:list", "system:user:create");

// 检查多个权限（任一通过）
StpUtil.checkPermissionOr("system:user:list", "system:user:delete");

// 获取角色列表
List<String> roleList = StpUtil.getRoleList();

// 判断角色
boolean hasRole = StpUtil.hasRole("ADMIN");

// 检查角色（未通过则抛出异常）
StpUtil.checkRole("ADMIN");

// 检查多个角色（全部通过）
StpUtil.checkRoleAnd("ADMIN", "SUPER_ADMIN");

// 检查多个角色（任一通过）
StpUtil.checkRoleOr("ADMIN", "SUPER_ADMIN");
```

控制器中使用权限验证：

```java
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

