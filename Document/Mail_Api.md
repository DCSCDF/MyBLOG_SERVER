# Mail API Documentation

## 概述

邮件模块提供 SMTP 邮件发送功能，支持发送测试邮件验证配置是否正确，以及评论、友链相关的自动通知邮件。

---

## API 接口

### 1. 发送测试邮件

发送一封测试邮件以验证 SMTP 配置是否正确。

- **URL**: `POST /api/mail/test`
- **权限**: `system:config:edit`
- **Content-Type**: `application/json`

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| to | string | 是 | 收件人邮箱地址 |
| subject | string | 否 | 邮件主题，默认值：`测试邮件` |
| content | string | 否 | 邮件内容，默认值：`这是一封来自博客系统的测试邮件。如果收到此邮件，说明邮件配置正确。` |

#### 请求示例

```json
{
  "to": "test@example.com",
  "subject": "测试邮件主题",
  "content": "这是测试邮件的内容"
}
```

#### 成功响应

```json
{
  "code": 200,
  "msg": "邮件发送成功",
  "data": null
}
```

#### 错误响应

**1. 参数校验失败**

```json
{
  "code": 400,
  "msg": "收件人邮箱不能为空",
  "data": null
}
```

```json
{
  "code": 400,
  "msg": "邮箱格式不正确",
  "data": null
}
```

**2. SMTP 配置未完成**

```json
{
  "code": 400,
  "msg": "SMTP 配置未完成，请先在系统配置中完成 SMTP 相关配置",
  "data": null
}
```

**3. 邮件发送失败**

```json
{
  "code": 500,
  "msg": "邮件发送失败：用户名或密码错误，请检查 SMTP 用户名和密码配置",
  "data": null
}
```

```json
{
  "code": 500,
  "msg": "邮件发送失败：无法连接到 SMTP 服务器，请检查 SMTP 主机和端口配置",
  "data": null
}
```

```json
{
  "code": 500,
  "msg": "邮件发送失败：连接 SMTP 服务器超时，请检查网络或 SMTP 主机配置",
  "data": null
}
```

```json
{
  "code": 500,
  "msg": "邮件发送失败：SSL/TLS 连接失败，请检查 SSL 配置是否正确",
  "data": null
}
```

---

### 2. 检查 SMTP 配置状态

检查 SMTP 配置是否已完成。

- **URL**: `GET /api/mail/status`
- **权限**: `system:config:edit`

#### 成功响应

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "SMTP 配置已完成"
}
```

#### 未配置响应

```json
{
  "code": 400,
  "msg": "SMTP 配置未完成",
  "data": null
}
```

---

## SMTP 配置项说明

在系统配置表 (`sys_config`) 中需要配置以下 SMTP 相关配置项：

| 配置键 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| smtp.host | string | `smtp.example.com` | SMTP 服务器地址，如 `smtp.gmail.com`、`smtp.qq.com` |
| smtp.port | integer | `587` | SMTP 端口号，常用端口：25（无加密）、587（TLS）、465（SSL） |
| smtp.username | string | - | SMTP 认证用户名，通常为邮箱地址 |
| smtp.password | string | - | SMTP 认证密码，部分邮箱需要使用应用专用密码 |
| smtp.from | string | - | 发件人邮箱地址 |
| smtp.ssl.enabled | boolean | `false` | 是否启用 SSL/TLS 加密 |
| smtp.comment.enabled | boolean | `false` | 是否启用评论及友链邮件通知功能 |

---

## 自动通知功能

系统支持评论、友链相关的自动邮件通知功能，通过 `smtp.comment.enabled` 配置开关控制。

### 通知类型

| 通知类型 | 触发条件 | 收件人 | 说明 |
|----------|----------|--------|------|
| 新评论通知 | 有新评论提交时 | 拥有 `system:comment:list` 权限的管理员 | 游客评论提示"待审核" |
| 顶级评论通过通知 | 顶级评论通过审核时 | 文章作者 | 通知有新评论 |
| 评论回复通知 | 评论收到回复时 | 被回复的评论作者 | 通知有回复 |
| 审核结果通知 | 评论审核状态变更时 | 评论作者 | 通知审核结果 |
| 新友链通知 | 有用户提交友链申请时 | 拥有 `links:list` 权限的管理员 | 通知有待审核的友链申请 |
| 友链审核结果通知 | 友链审核状态变更时（预留） | 友链提交者 | 预留接口，通知审核结果 |

## 错误代码说明

| 错误代码 | 说明 |
|----------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误或 SMTP 配置未完成 |
| 500 | 服务器内部错误，邮件发送失败 |

---

## 常见问题排查

### 1. 认证失败

**错误信息**: `邮件发送失败：用户名或密码错误`

- 检查 `smtp.username` 是否为正确的邮箱地址
- 检查 `smtp.password` 是否正确，部分邮箱（如 Gmail、QQ邮箱）需要使用**应用专用密码**
- Gmail 需要启用"低安全性应用访问"或使用应用专用密码
- QQ 邮箱需要在设置中开启 SMTP 服务并获取授权码

### 2. 连接失败

**错误信息**: `邮件发送失败：无法连接到 SMTP 服务器`

- 检查 `smtp.host` 是否正确（如 `smtp.gmail.com`）
- 检查 `smtp.port` 是否正确（常用：587 for TLS，465 for SSL）
- 检查防火墙是否阻止了 SMTP 端口

### 3. 连接超时

**错误信息**: `邮件发送失败：连接 SMTP 服务器超时`

- 检查网络连接是否正常
- 检查 SMTP 服务器是否可访问
- 尝试更换 `smtp.host` 或 `smtp.port`

### 4. SSL/TLS 错误

**错误信息**: `邮件发送失败：SSL/TLS 连接失败`

- 如果使用 465 端口，确保 `smtp.ssl.enabled` 设置为 `true`
- 如果使用 587 端口，确保 `smtp.ssl.enabled` 设置为 `false`（STARTTLS）
- 检查服务器是否支持 SSL/TLS 协议

---

## 使用示例

### cURL 示例

```bash
# 发送测试邮件
curl -X POST http://localhost:8080/api/mail/test \
  -H "Content-Type: application/json" \
  -H "Authorization: <token>" \
  -d '{"to": "test@example.com", "subject": "测试", "content": "测试内容"}'

# 检查 SMTP 状态
curl -X GET http://localhost:8080/api/mail/status \
  -H "Authorization: <token>"
```

### JavaScript 示例

```javascript
// 发送测试邮件
fetch('/api/mail/test', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': token
  },
  body: JSON.stringify({
    to: 'test@example.com',
    subject: '测试邮件',
    content: '这是一封测试邮件'
  })
})
```
