# 公共评论接口文档

---

## 接口说明

公共评论接口用于前台用户提交评论，**无需登录即可访问**。已登录用户提交评论使用用户表信息，游客使用传入的信息。隐藏或已删除的文章无法评论。

**接口基础路径**: `/api/public/comment`

---

## 功能特性

1. **已登录用户**: 自动使用用户表的昵称、邮箱、头像信息，无需传入
2. **游客评论**: 支持传入用户名、邮箱、头像、个人网站
3. **回复评论**: 支持回复已审核通过的评论（回复需要传入父评论ID）
4. **审核机制**: 已登录用户评论直接通过，游客评论需审核后显示
5. **IP记录**: 自动记录评论者的IP地址和设备信息
6. **评论数更新**: 提交评论后自动更新文章的评论数

---

## 接口详情

### 1. 提交评论

提交评论或回复，支持已登录用户和游客两种模式。

- **请求方法**: `POST`
- **请求路径**: `/api/public/comment`
- **是否需要登录**: 否

#### 请求参数

```json
{
  "blogId": 1,
  "parentId": 0,
  "username": "游客张三",
  "email": "zhangsan@example.com",
  "avatarUrl": "https://example.com/avatar.jpg",
  "website": "https://myblog.com",
  "content": "这是一条评论内容"
}
```

| 字段      | 类型     | 必填   | 说明                                       |
|---------|--------|------|------------------------------------------|
| blogId  | Long   | 是    | 文章ID                                    |
| parentId| Long   | 是    | 父评论ID，0表示顶级评论                              |
| username| String | 游客必填 | 评论者名称，已登录用户可不填                     |
| email   | String | 否    | 邮箱，已登录用户可不填                           |
| avatarUrl| String | 否    | 头像URL，已登录用户可不填                       |
| website | String | 否    | 个人网站                                   |
| content | String | 是    | 评论内容                                   |

#### 已登录用户请求示例

```json
{
  "blogId": 1,
  "parentId": 0,
  "content": "这是一条已登录用户的评论"
}
```

#### 响应示例（成功）

```json
{
  "data": {
    "id": 123,
    "message": "评论提交成功"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 游客响应示例

```json
{
  "data": {
    "id": 124,
    "message": "评论提交成功，待审核后显示"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 响应字段说明

| 字段     | 类型   | 说明    |
|--------|------|-------|
| id     | Long | 评论ID  |
| message| String | 状态提示 |

---

## 错误响应

### 文章不存在或已下架

```json
{
  "data": null,
  "success": false,
  "errorMsg": "文章不存在或已下架",
  "code": 404
}
```

### 父评论不存在

```json
{
  "data": null,
  "success": false,
  "errorMsg": "父评论不存在或不属于该文章",
  "code": 400
}
```

### 参数校验失败

```json
{
  "data": null,
  "success": false,
  "errorMsg": "评论内容不能为空",
  "code": 400
}
```

### 服务器内部错误

```json
{
  "data": null,
  "success": false,
  "errorMsg": "评论提交失败",
  "code": 500
}
```

---

## 使用示例

### 示例1: 游客提交顶级评论

```bash
curl -X POST http://localhost:8080/api/public/comment \
  -H "Content-Type: application/json" \
  -d '{
    "blogId": 1,
    "parentId": 0,
    "username": "游客张三",
    "email": "zhangsan@example.com",
    "content": "这是一条游客评论"
  }'
```

### 示例2: 已登录用户提交评论

```bash
curl -X POST http://localhost:8080/api/public/comment \
  -H "Content-Type: application/json" \
  -H "token: your_token_here" \
  -d '{
    "blogId": 1,
    "parentId": 0,
    "content": "这是一条已登录用户的评论"
  }'
```

### 示例3: 回复评论

```bash
curl -X POST http://localhost:8080/api/public/comment \
  -H "Content-Type: application/json" \
  -d '{
    "blogId": 1,
    "parentId": 123,
    "username": "回复者",
    "content": "这是一条回复评论"
  }'
```

### 示例4: 携带设备信息（前端自动处理）

前端请求会自动携带 `User-Agent` 头，服务器会记录评论者的设备信息。

---

## 实现细节

### 用户识别逻辑

1. 首先检查用户是否已登录（通过 Sa-Token 的 `StpUtil.isLogin()`）
2. **已登录用户**:
   - 使用用户表的 `nickname` 作为评论者名称
   - 使用用户表的 `email` 作为邮箱
   - 使用用户表的 `avatar` 作为头像
   - 直接通过审核（`status = 1`）
   - 如果用户有系统管理权限（如 `system:user:list`），则 `is_admin = true`
3. **游客用户**:
   - 使用传入的 `username`、`email`、`avatarUrl`
   - 进入待审核状态（`status = 0`）

### IP 和设备信息

1. IP 地址获取优先级：`X-Forwarded-For` > `X-Real-IP` > `request.getRemoteAddr()`
2. 设备信息直接从请求头 `User-Agent` 获取

### 评论状态说明

| 状态码 | 说明   |
|-----|------|
| 0   | 待审核  |
| 1   | 通过   |
| 2   | 垃圾评论 |
| 3   | 删除   |

### 回复验证

1. 回复的父评论必须存在且属于同一篇文章
2. 父评论必须是已审核通过的状态（`status = 1`）
3. 只有顶级评论和一级回复会被显示（根据业务需求可调整）

---

## 注意事项

1. **无需鉴权**: 该接口为公开接口，前端无需携带 token 即可访问
2. **已登录用户**: Token 有效时自动使用用户信息，无需传入个人信息
3. **游客审核**: 游客提交的评论需要管理员审核后才能显示
4. **文章限制**: 隐藏或已删除的文章无法评论
5. **回复限制**: 回复只能回复顶级评论或一级回复，不能嵌套太深
6. **敏感词**: 建议前端或后端增加敏感词过滤机制
7. **频率限制**: 建议配置评论提交频率限制，防止刷屏

