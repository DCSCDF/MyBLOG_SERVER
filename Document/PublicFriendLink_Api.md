# 公共外链接口文档

---

## 接口说明

公共外链接口用于前台用户提交外链申请和查看已通过审核的外链列表，**无需登录即可访问**。

**接口基础路径**: `/api/public/friend-link`

---

## 功能特性

1. **外链申请提交**: 用户可提交外链申请，包含名称、URL地址、简介、站点图片URL
2. **URL验证**: URL地址和站点图片URL必须为有效的 http/https 协议，否则返回错误
3. **审核机制**: 提交的外链默认状态为待审核(status=0)，需管理员审核后才能显示
4. **排序规则**: 外链列表按 `sort_order` 降序排序（数字越大越靠前），相同则按创建时间降序
5. **缓存机制**: 外链列表使用30分钟缓存，提高访问速度
6. **限流机制**: 提交接口每个IP每分钟最多提交4次，防止刷屏

---

## 接口详情

### 1. 提交外链申请

提交新的外链申请，提交后状态为待审核。

- **请求方法**: `POST`
- **请求路径**: `/api/public/friend-link`
- **是否需要登录**: 否
- **限流**: 每个IP每分钟4次

#### 请求参数

```json
{
  "name": "示例博客",
  "url": "https://example.com",
  "summary": "一个示例技术博客站点",
  "imageUrl": "https://example.com/logo.png"
}
```

| 字段       | 类型     | 必填 | 说明                             |
|----------|--------|----|--------------------------------|
| name     | String | 是  | 链接名称                           |
| url      | String | 是  | URL地址（必须为有效的 http/https URL）   |
| summary  | String | 否  | 简介                             |
| imageUrl | String | 否  | 站点图片URL（必须为有效的 http/https URL） |

#### 响应示例（成功）

```json
{
  "data": "外链提交成功，待审核后显示",
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 响应示例（URL格式无效）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "URL地址格式无效，请输入有效的网址",
  "code": 400
}
```

```json
{
  "data": null,
  "success": false,
  "errorMsg": "站点图片URL格式无效，请输入有效的网址",
  "code": 400
}
```

#### 响应示例（参数校验失败）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "链接名称不能为空",
  "code": 400
}
```

---

### 2. 获取已通过的外链列表

获取已通过审核的外链列表，支持分页。只返回 status=1（已通过）且未删除的外链。

- **请求方法**: `POST`
- **请求路径**: `/api/public/friend-link/list`
- **是否需要登录**: 否
- **缓存**: 30分钟

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10
}
```

| 字段          | 类型      | 必填 | 说明         |
|-------------|---------|----|------------|
| currentPage | Integer | 是  | 当前页码（从1开始） |
| pageSize    | Integer | 是  | 每页数量       |

#### 响应示例（成功）

```json
{
  "data": {
    "records": [
      {
        "name": "示例博客",
        "url": "https://example.com",
        "summary": "一个示例技术博客站点",
        "imageUrl": "https://example.com/logo.png",
        "createTime": "2026-03-27T10:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 响应字段说明

| 字段      | 类型         | 说明   |
|---------|------------|------|
| records | List<外链对象> | 外链列表 |
| total   | Long       | 总记录数 |
| size    | Long       | 每页数量 |
| current | Long       | 当前页码 |
| pages   | Long       | 总页数  |

#### records 字段说明

| 字段         | 类型            | 说明      |
|------------|---------------|---------|
| name       | String        | 链接名称    |
| url        | String        | URL地址   |
| summary    | String        | 简介      |
| imageUrl   | String        | 站点图片URL |
| createTime | LocalDateTime | 创建时间    |

---

## 错误响应

### URL 格式无效

```json
{
  "data": null,
  "success": false,
  "errorMsg": "URL地址格式无效，请输入有效的网址",
  "code": 400
}
```

```json
{
  "data": null,
  "success": false,
  "errorMsg": "站点图片URL格式无效，请输入有效的网址",
  "code": 400
}
```

### 参数校验失败

```json
{
  "data": null,
  "success": false,
  "errorMsg": "链接名称不能为空",
  "code": 400
}
```

### 服务器内部错误

```json
{
  "data": null,
  "success": false,
  "errorMsg": "外链提交失败",
  "code": 500
}
```

### 请求过于频繁（限流）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "请求过于频繁，请稍后再试",
  "code": 429
}
```

---

## 使用示例

### 示例1: 提交外链申请

```bash
curl -X POST http://localhost:8080/api/public/friend-link \
  -H "Content-Type: application/json" \
  -d '{
    "name": "示例博客",
    "url": "https://example.com",
    "summary": "一个示例技术博客站点",
    "imageUrl": "https://example.com/logo.png"
  }'
```

### 示例2: 获取外链列表

```bash
curl -X POST http://localhost:8080/api/public/friend-link/list \
  -H "Content-Type: application/json" \
  -d '{
    "currentPage": 1,
    "pageSize": 10
  }'
```

---

## 实现细节

### URL 验证规则

1. URL地址和站点图片URL必须为有效的 http/https 协议
2. 使用 `java.net.URL` 类解析并验证协议
3. 非 http/https 协议的URL将被拒绝

### 缓存机制

1. 外链列表使用 Guava Cache 缓存
2. 缓存时间：30分钟
3. 当后台修改、删除、新增外链时，自动清除缓存
4. 缓存键格式：`friend_link_list:public:{page}-{pageSize}`

### 限流机制

1. 使用 `@RateLimit` 注解实现限流
2. 限流规则：每个IP每分钟最多提交4次
3. 限流键前缀：`public_friend_link_create`
4. 超出限制返回 HTTP 429 状态码

### 排序规则

1. 主排序：`sort_order` 降序（数字越大越靠前）
2. 次排序：`create_time` 降序（sort_order 相同时，新建的外链排在前面）

### 外链状态说明

| 状态码 | 说明   |
|-----|------|
| 0   | 待审核  |
| 1   | 已通过  |
| 2   | 已拒绝  |
| 3   | 已删除  |

---

## 注意事项

1. **无需鉴权**: 该接口为公开接口，前端无需携带 token 即可访问
2. **URL验证**: 请确保输入的URL地址和站点图片URL为有效的 http/https 协议，否则会被拒绝
3. **审核机制**: 提交的外链需要管理员审核后才能在前台显示
4. **限流保护**: 提交接口有频率限制，防止恶意刷屏
5. **缓存说明**: 外链列表有30分钟缓存，审核通过后可能需要等待缓存过期才能看到更新
