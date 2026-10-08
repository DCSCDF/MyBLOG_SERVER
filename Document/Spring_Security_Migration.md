# ACL 迁移：Sa-Token → Spring Security

本次迁移将鉴权从 Sa-Token 全量切换到 Spring Security，**数据表、权限码、角色码与 API 响应格式均保持不变**，前端无需改动。

## 映射关系

| 原 Sa-Token | Spring Security 替代 |
| --- | --- |
| `sa-token-spring-boot3-starter` 依赖 | `spring-boot-starter-security` |
| `SaTokenConfig`（过滤器 + 注解拦截器） | `config/system/SecurityConfig` 的 `SecurityFilterChain` + `@EnableMethodSecurity` |
| `StpInterface`（`AuthenticationService`） | `security/BlogUserDetailsService`（`UserDetailsService`），权限展开规则不变 |
| `@SaCheckPermission("article:create")` | `@PreAuthorize("hasAuthority('article:create')")` |
| `@SaCheckRole("SUPER_ADMIN")` | `@PreAuthorize("hasRole('SUPER_ADMIN')")`（权限为 `ROLE_SUPER_ADMIN`） |
| `StpUtil.getLoginIdAsLong()` | `SecurityUtils.getCurrentUserId()` |
| `StpUtil.isLogin()` / `hasPermission()` | `SecurityUtils.isLogin()` / `hasPermission()` |
| `StpUtil.login/logout` | `security/AuthTokenService`（不透明令牌，内存存储） |
| `SaTokenDao`（临时令牌/授权码/验证码存储） | `security/ExpiringValueStore`（带 TTL 的内存存储） |
| `SaFoxUtil.getRandomString` | `utils/security/RandomStringUtil`（SecureRandom，字符集相同） |
| `SaResult` | `dto/ServiceResult`（`code/msg/data` 与静态工厂语义一致） |
| `NotLoginException` / `NotPermissionException` | `AuthenticationException`（401）/ `AccessDeniedException`（403），统一 JSON 格式不变 |

## 保持不变的行为

- 请求头令牌：仍为 `token: <令牌>`（配置 `app.security.token-name`，兼容 `Authorization: Bearer`）。
- 令牌语义：30 天绝对过期（`app.security.token-timeout`）；未勾选记住我时 2 小时活跃超时（滑动续期，`app.login.active-timeout-no-remember`）；单端登录（新登录挤掉旧令牌）；登出、改密、禁用、删除均使令牌失效。
- 权限计算：角色经权限组 + 直接授权获得权限，父权限自动展开子权限，超级管理员拥有全部权限；`sys_*` 表结构与种子数据不变。
- 公开接口划分与原先一致（`/api/public/**`、`/api/captcha/**`、登录/注册/找回密码等），其余接口未登录返回统一 `401 {"success":false,"code":401,"errorMsg":"未授权，请先登录"}`，越权返回 403。

## 配置变更

`sa-token.*` 配置已移除，替换为：

```properties
app.security.token-name=token
app.security.token-timeout=2592000
app.login.active-timeout-no-remember=7200
```

## 已知差异

- 令牌仍为服务端内存存储：应用重启后全部令牌失效（与原 Sa-Token 内存实现一致）。如需多实例部署，可将 `AuthTokenService` / `ExpiringValueStore` 换为 Redis 实现。
- 因活跃超时不再区分“令牌冻结”，原 `登录已过期，请重新登录` 提示统一为 `未授权，请先登录`（HTTP 401 与响应 `code` 不变）。
