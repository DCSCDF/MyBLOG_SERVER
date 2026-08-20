## 限流说明文档

---

### 概述

后端对接口按 **IP（或全局）+ 方法** 进行限流，防止暴力请求、恶意刷接口。触发限流时返回 **HTTP 429 Too Many Requests**，并在响应头中设置 **Retry-After**，便于客户端退避重试。

限流基于内存实现，适用于单机或可接受「每实例独立计数」的多实例部署；若需多实例共享同一限流计数，需自行改为 Redis 等集中式存储。

---

### 实现方式

- **注解**：在 Controller 方法上使用 `@RateLimit(count = N, period = M)`，表示在 **M 分钟**内同一限流键最多允许 **N** 次请求。
- **切面**：`RateLimitAspect` 在方法执行前根据限流键生成「时间窗口 + 计数」；超出则抛出 `RateLimitException`。
- **动态限流**：`dynamic=true`（默认）时，`RateLimitAspect` 会真实统计当前活跃请求数，`DynamicRateLimitService` 根据并发压力动态收紧阈值（活跃 >30/50/100 时逐级下调，下限 100 次/分钟）。
- **全局异常处理**：`GlobalExceptionHandler` 将 `RateLimitException` 转为统一响应，状态码 429，并设置响应头 `Retry-After: 60`（建议 60 秒后重试）。

**限流键规则**：`{prefix}:{clientIp}:{Controller类名}.{方法名}`（`ipBased=true` 时）；`ipBased=false` 时客户端部分固定为 `global`，所有用户共享一个计数。

**时间窗口**：从某 key 的**第一次请求**起算，在 `period` 分钟内允许最多 `count` 次；窗口过期后自动重置计数。

---

### 注解参数

| 参数     | 类型     | 默认值          | 说明                                       |
|--------|--------|--------------|------------------------------------------|
| count  | int    | 500          | 时间窗口内允许的请求次数                             |
| period | int    | 1            | 时间窗口长度，**单位：分钟**                         |
| prefix | String | "rate_limit" | 限流键前缀，一般无需修改                             |
| dynamic | boolean | true        | 是否启用动态限流（根据活跃请求数自动收紧阈值）                |
| ipBased | boolean | true        | 是否基于 IP 限流；设为 false 则所有用户共享同一计数（全局限流） |

- `period <= 0` 时按 1 分钟处理；`count <= 0` 时按 1 次处理，避免配置错误导致失效或过于宽松。

---

### 客户端 IP 识别（安全说明）

限流键依赖**客户端 IP**，但不再无条件信任客户端可伪造的 `X-Forwarded-For` / `X-Real-IP` 头：

1. 配置了 `app.security.trusted-proxies`（逗号分隔的可信反向代理 IP）时：仅当请求直接来源（`getRemoteAddr()`）命中该列表，才信任转发头并取 `X-Forwarded-For` 首段（无则取 `X-Real-IP`）；
2. 未配置时：仅当直接来源为回环/内网地址（本机 Nginx、同机 Docker 部署）才信任转发头；
3. 其余情况一律使用 `getRemoteAddr()`，伪造转发头不再生效。

**部署在反向代理/负载均衡后时**：请将代理机 IP 加入 `app.security.trusted-proxies`，否则所有请求会被视为同一 IP（代理地址），导致限流过严。若有多级代理，确保最前端代理写入真实客户端 IP。

---

### 当前应用位置

以下接口已启用限流，生产环境可根据需要调整 `count` / `period`。

#### 认证相关（/api/auth，ipBased=true）

| 接口/方法                              | 限流规则         | 说明         |
|------------------------------------|--------------|------------|
| `GET /api/auth/public-key`         | 30 次 / 1 分钟  | 获取登录公钥，防滥用 |
| `POST /api/auth/login`             | 6 次 / 15 分钟  | 登录         |
| `POST /api/auth/oauth/token`       | 10 次 / 60 分钟 | 授权码换 token |
| `POST /api/auth/register`          | 6 次 / 60 分钟  | 注册         |
| `POST /api/auth/register/code`     | 6 次 / 60 分钟  | 发送注册验证码    |
| `POST /api/auth/register/confirm`  | 6 次 / 60 分钟  | 确认注册       |
| `POST /api/auth/find-password/code` | 6 次 / 60 分钟 | 发送找回密码验证码  |
| `POST /api/auth/find-password/confirm` | 6 次 / 60 分钟 | 重置密码    |
| `POST /api/auth/update-password`   | 6 次 / 15 分钟  | 修改密码       |
| `POST /api/auth/update-nickname`   | 10 次 / 60 分钟 | 修改昵称       |
| `POST /api/auth/update-bio`        | 10 次 / 60 分钟 | 修改简介       |
| `POST /api/auth/update-avatar-url` | 10 次 / 60 分钟 | 修改头像       |
| `POST /api/auth/update-email`      | 6 次 / 60 分钟  | 修改邮箱       |
| `POST /api/auth/change-email/code` | 6 次 / 60 分钟  | 发送换邮箱验证码   |
| `POST /api/auth/change-email/confirm` | 6 次 / 60 分钟 | 确认换邮箱    |
| `POST /api/auth/profile`           | 80 次 / 4 分钟  | 获取当前用户信息   |
| `POST /api/auth/logout`            | 80 次 / 4 分钟  | 登出         |
| `POST /api/auth/permissions`       | 80 次 / 4 分钟  | 获取当前用户权限   |

#### 验证码（/api/captcha，ipBased=true）

| 接口/方法              | 限流规则        | 说明      |
|--------------------|-------------|---------|
| `POST /api/captcha/get`    | 10 次 / 1 分钟 | 获取验证码   |
| `POST /api/captcha/check`  | 20 次 / 1 分钟 | 检查验证码   |
| `GET /api/captcha/verify`  | 10 次 / 1 分钟 | 二次验证    |

#### OSS / 图片（ipBased=true）

| 接口/方法                      | 限流规则         | 说明          |
|----------------------------|--------------|-------------|
| `POST /api/oss/upload`     | 20 次 / 1 分钟  | 上传图片（防解压炸弹刷接口） |
| `GET /api/images/{hash}`   | 120 次 / 1 分钟 | 公开图片流（防 OSS 流量/计费攻击） |

#### 公开读接口（ipBased=true，防单攻击者耗尽全站预算）

| 接口/方法                              | 限流规则         |
|------------------------------------|--------------|
| `POST /api/public/article/list`    | 500 次 / 1 分钟 |
| `POST /api/public/article/admin/list` | 500 次 / 1 分钟 |
| `POST /api/public/article/user/list` | 500 次 / 1 分钟 |
| `GET /api/public/article/{id}`     | 500 次 / 1 分钟 |
| `GET /api/public/rss`              | 500 次 / 1 分钟 |
| `POST /api/public/config`          | 520 次 / 1 分钟 |
| `GET /api/public/config/site-info` | 500 次 / 1 分钟 |
| `GET /api/public/admin/info`       | 500 次 / 1 分钟 |

#### 全局限流接口（ipBased=false，全站共享计数）

| 接口/方法                          | 限流规则         | 说明        |
|--------------------------------|--------------|-----------|
| `GET /api/public/category/list` | 500 次 / 1 分钟 | 全站共享，按需调整 |
| `GET /api/public/category/{id}` | 500 次 / 1 分钟 | 全站共享，按需调整 |

#### 其它

| 接口/方法                        | 限流规则         | 说明           |
|------------------------------|--------------|--------------|
| `POST /api/public/comment`   | 4 次 / 1 分钟  | 提交评论（匿名接口）   |
| `POST /api/public/friend-link` | 4 次 / 1 分钟 | 提交外链申请（匿名接口） |
| `POST /api/blogs`            | 20 次 / 60 分钟 | 创建文章         |

---

### 429 响应格式

触发限流时：

- **HTTP 状态码**：429
- **响应头**：`Retry-After: 60`（建议 60 秒后再试）
- **响应体**：与项目统一格式一致，例如：

```json
{
    "data": null,
    "success": false,
    "errorMsg": "请求过于频繁，请稍后再试",
    "code": 429
}
```

前端可根据 `code === 429` 或 `Retry-After` 做提示或定时重试。

---

### 生产环境注意事项

1. **单机内存**  
   限流数据存于 JVM 内存（ConcurrentHashMap + 定时清理，每 30 秒清理一次、保留 5 分钟宽限期；上限 10000 个 key，超出触发紧急清理）。内存占用与「活跃 IP × 被限流接口数 × 最大 period 分钟」相关，一般可接受。

2. **多实例部署**  
   各实例独立计数，不做跨实例汇总。例如 2 台机器时，同一 IP 对同一接口在每台机器上各有 6 次/15 分钟，实际可请求约 12 次/15 分钟。若需全局限流，需自行改为 Redis 等集中式计数。

3. **代理与 IP**  
   见上文「客户端 IP 识别」，务必在代理/负载均衡上配置 `app.security.trusted-proxies`。

4. **调参建议**  
   - 登录/注册/改密等敏感接口：建议保持较严（如 5–10 次/10–15 分钟）。  
   - 验证码获取/校验：按需设置（如 10–20 次/分钟），避免单 IP 刷验证码。  
   - 公钥、个人资料等：可适当放宽（如 30–80 次/1–4 分钟），避免正常用户被误拦。

5. **仅 Web 请求**  
   `@RateLimit` 仅用于在 Web 请求上下文中调用的 Controller 方法；在非 Web 上下文使用会抛出异常并由全局异常处理返回 500。

6. **日志**  
   触发限流时会有 WARN 日志（包含 IP、限流键、方法签名），便于排查与审计。
