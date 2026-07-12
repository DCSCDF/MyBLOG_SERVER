## 公共管理员信息接口文档

---

### 接口说明

公共管理员信息接口用于前台获取超级管理员用户的公开信息，**无需登录即可访问**。

**接口基础路径**: `/api/public/admin`

---

### 功能特性

1. **获取管理员信息**: 获取超级管理员的昵称、邮箱、头像和简介
2. **限流保护**: 使用内存限流防止频繁请求

---

## 接口详情

### 1. 获取超级管理员公开信息

获取超级管理员用户的公开基本信息。

- **请求方法**: `GET`
- **请求路径**: `/api/public/admin/info`
- **是否需要登录**: 否
- **限流**: 500次/分钟

#### 请求参数

无请求参数。

#### 响应示例

```json
{
  "data": {
    "nickname": "管理员",
    "email": "admin@example.com",
    "avatarUrl": "https://example.com/avatar.png",
    "bio": "热爱技术，分享生活"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 响应字段说明

| 字段        | 类型     | 说明     |
|-----------|--------|------|
| nickname  | String | 昵称    |
| email     | String | 邮箱    |
| avatarUrl | String | 头像URL |
| bio       | String | 用户简介  |

---

## 使用示例

### 示例: 获取管理员信息

```bash
curl -X GET "http://localhost:8080/api/public/admin/info"
```

---

## 错误响应

### 管理员用户不存在

```json
{
  "data": null,
  "success": false,
  "errorMsg": "管理员用户不存在",
  "code": 404
}
```

---

## 注意事项

1. **无需鉴权**: 该接口为公开接口，前端无需携带 token 即可访问
2. **返回信息**: 返回的是超级管理员（admin）的公开信息，不包含敏感字段如密码、ID等
3. **限流保护**: 接口有频率限制，高并发场景下建议配合 CDN 使用