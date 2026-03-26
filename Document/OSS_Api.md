# OSS API Documentation

## 概述

OSS 模块提供阿里云对象存储（OSS）连接测试功能，用于验证 OSS 配置是否正确。

---

## API 接口

### 1. 测试 OSS 连接

测试阿里云 OSS 配置是否正确并验证连接状态。

- **URL**: `GET /api/oss/test`
- **权限**: `system:config:edit`

#### 成功响应

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "OSS 配置已完成且连接正常"
}
```

#### 错误响应

**1. OSS 配置未完成**

```json
{
  "code": 400,
  "msg": "OSS 配置未完成，请先在系统配置中完成阿里云 OSS 相关配置",
  "data": null
}
```

**2. OSS 客户端初始化失败**

```json
{
  "code": 500,
  "msg": "OSS 客户端初始化失败，请检查配置",
  "data": null
}
```

**3. 无法连接到 OSS 服务器**

```json
{
  "code": 400,
  "msg": "OSS 配置已完成，但无法连接到服务器：<具体错误信息>",
  "data": null
}
```

**4. 权限不足**

```json
{
  "code": 403,
  "msg": "无权限访问",
  "data": null
}
```

---

## OSS 配置项说明

在系统配置表 (`sys_config`) 中需要配置以下 OSS 相关配置项：

| 配置键                  | 类型      | 默认值     | 说明                                       |
|----------------------|---------|---------|------------------------------------------|
| aliyun.Access-key    | string  | -       | 阿里云 AccessKey ID                         |
| aliyun.Secret-key    | string  | -       | 阿里云 AccessKey Secret                     |
| aliyun.Bucket        | string  | -       | OSS Bucket 名称                            |
| aliyun.end-point     | string  | -       | OSS 访问域名（如 oss-cn-hangzhou.aliyuncs.com） |
| aliyun.https-enabled | boolean | `false` | 是否启用 HTTPS 访问                            |

---

## 错误代码说明

| 错误代码 | 说明               |
|------|------------------|
| 200  | 操作成功，连接正常        |
| 400  | OSS 配置未完成或无法连接   |
| 403  | 权限不足             |
| 500  | 服务器内部错误，客户端初始化失败 |

---

## 使用示例

### cURL 示例

```bash
# 测试 OSS 连接
curl -X GET http://localhost:8080/api/oss/test \
  -H "Authorization: <token>"
```

### JavaScript 示例

```javascript
// 测试 OSS 连接
fetch('/api/oss/test', {
  method: 'GET',
  headers: {
    'Authorization': token
  }
})
  .then(response => response.json())
  .then(data => console.log(data));
```

---

## 常见问题排查

### 1. AccessKey 无效

**错误信息**: `OSS 配置已完成，但无法连接到服务器：InvalidAccessKeyId`

- 检查 `aliyun.Access-key` 是否正确
- 确认 AccessKey 未过期或被禁用

### 2. 签名不匹配

**错误信息**: `OSS 配置已完成，但无法连接到服务器：SignatureDoesNotMatch`

- 检查 `aliyun.Secret-key` 是否正确
- 确认 Bucket 名称和 Endpoint 是否匹配

### 3. Bucket 不存在

**错误信息**: `OSS 配置已完成，但无法连接到服务器：NoSuchBucket`

- 检查 `aliyun.Bucket` 是否存在
- 确认 Bucket 所在区域与 Endpoint 一致

### 4. 网络连接问题

**错误信息**: `OSS 配置已完成，但无法连接到服务器：ConnectionTimeout`

- 检查网络连接是否正常
- 确认防火墙未阻止 OSS 端口（443）

---

## 配置刷新

OSS 配置从数据库动态加载，修改配置后系统会自动刷新，无需重启服务。

---
