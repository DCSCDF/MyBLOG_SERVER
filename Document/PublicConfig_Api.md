## 公共配置接口文档

---

### 接口说明

公共配置接口用于前台获取网站配置信息，**无需登录即可访问**。

**接口基础路径**: `/api/public/config`

---

### 功能特性

1. **批量获取配置**: 支持通过配置键批量获取配置值
2. **网站基础信息**: 提供网站名称、域名、描述、备案号等基础信息
3. **限流保护**: 使用内存限流防止频繁请求

---

## 接口详情

### 1. 根据配置键获取配置值

批量获取指定配置键的配置值。

- **请求方法**: `POST`
- **请求路径**: `/api/public/config`
- **是否需要登录**: 否
- **限流**: 520次/分钟（按 IP 计数）

#### 请求参数

```json
{
  "keys": ["site.name", "site.logo"]
}
```

| 字段 | 类型     | 必填 | 说明           |
|----|--------|----|--------------|
| keys | List<String> | 是  | 配置键列表，不能为空 |

#### 响应示例

```json
{
  "data": [
    {
      "configKey": "site.name",
      "configValue": "我的博客"
    },
    {
      "configKey": "site.logo",
      "configValue": "https://example.com/logo.png"
    }
  ],
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 响应字段说明

| 字段         | 类型     | 说明     |
|------------|--------|------|
| configKey  | String | 配置键   |
| configValue | String | 配置值   |

---

### 2. 获取网站基础信息

获取网站的基础配置信息，包括名称、域名、描述、备案号等。

- **请求方法**: `GET`
- **请求路径**: `/api/public/config/site-info`
- **是否需要登录**: 否
- **限流**: 500次/分钟（按 IP 计数）

#### 请求参数

无请求参数。

#### 响应示例

```json
{
  "data": {
    "siteName": "我的博客",
    "siteDomain": "https://example.com",
    "siteDescription": "分享技术与生活的博客",
    "recordNumber": "京ICP备XXXXXXXX号"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 响应字段说明

| 字段            | 类型     | 说明     |
|---------------|--------|------|
| siteName      | String | 网站名称   |
| siteDomain    | String | 网站域名   |
| siteDescription | String | 网站描述   |
| recordNumber  | String | 备案号    |

---

## 使用示例

### 示例1: 获取网站基础信息

```bash
curl -X GET "http://localhost:8080/api/public/config/site-info"
```

### 示例2: 批量获取配置值

```bash
curl -X POST http://localhost:8080/api/public/config \
  -H "Content-Type: application/json" \
  -d '{"keys": ["site.name", "site.domain", "site.description"]}'
```

---

## 错误响应

### 配置键列表为空

```json
{
  "data": null,
  "success": false,
  "errorMsg": "配置键列表不能为空",
  "code": 400
}
```

### 服务器内部错误

```json
{
  "data": null,
  "success": false,
  "errorMsg": "获取配置失败",
  "code": 500
}
```

---

## 注意事项

1. **无需鉴权**: 该接口为公开接口，前端无需携带 token 即可访问
2. **配置键格式**: 配置键区分大小写，如 `site.name` 和 `Site.Name` 是不同的键
3. **限流保护**: 接口有频率限制，高并发场景下建议配合 CDN 使用
