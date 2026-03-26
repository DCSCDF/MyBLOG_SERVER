# OSS API Documentation

## 概述

OSS 模块提供阿里云对象存储（OSS）连接测试、图片上传和图片删除功能。

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

### 2. 上传图片

上传图片到阿里云 OSS，支持格式校验和无损压缩。

- **URL**: `POST /api/oss/upload`
- **权限**: `oss:create`
- **Content-Type**: `multipart/form-data`

#### 请求参数

| 参数名 | 类型   | 必填 | 说明                    |
|--------|--------|------|-------------------------|
| file   | File   | 是   | 图片文件（不超过 10MB） |

#### 支持的图片格式

- JPEG / JPG
- PNG
- GIF
- BMP
- WebP

#### 成功响应

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "objectName": "images/2026/03/26/abc123def456.jpg",
    "url": "https://myblog-jiuliu.oss-cn-beijing.aliyuncs.com/images/2026/03/26/abc123def456.jpg",
    "size": 102400
  }
}
```

#### 错误响应

**1. 文件为空**

```json
{
  "code": 400,
  "msg": "请选择要上传的图片",
  "data": null
}
```

**2. 不支持的图片格式**

```json
{
  "code": 400,
  "msg": "不支持的图片格式或文件损坏，支持的格式：jpg, jpeg, png, gif, bmp, webp",
  "data": null
}
```

**3. 文件过大**

```json
{
  "code": 400,
  "msg": "图片大小超过限制",
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

### 3. 删除图片

从阿里云 OSS 删除指定图片。

- **URL**: `DELETE /api/oss/delete`
- **权限**: `oss:delete`

#### 请求参数

| 参数名      | 类型   | 必填 | 说明                          |
|-------------|--------|------|-------------------------------|
| objectName  | String | 是   | OSS 对象名称（文件路径）      |

#### 成功响应

```json
{
  "code": 200,
  "msg": "删除成功",
  "data": null
}
```

#### 错误响应

**1. 对象名为空**

```json
{
  "code": 400,
  "msg": "对象名称不能为空",
  "data": null
}
```

**2. 删除失败**

```json
{
  "code": 500,
  "msg": "删除失败：<具体错误信息>",
  "data": null
}
```

**3. 权限不足**

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
|------------------------|-----------|---------|------------------------------------------|
| aliyun.Access-key      | string    | -       | 阿里云 AccessKey ID                         |
| aliyun.Secret-key      | string    | -       | 阿里云 AccessKey Secret                     |
| aliyun.Bucket          | string    | -       | OSS Bucket 名称                            |
| aliyun.end-point       | string    | -       | OSS 访问域名（如 oss-cn-hangzhou.aliyuncs.com） |
| aliyun.https-enabled    | boolean   | `false` | 是否启用 HTTPS 访问                            |

---

## 权限说明

| 权限码          | 说明       | 所属角色          |
|-----------------|------------|------------------|
| oss:create      | OSS 上传   | 超级管理员        |
| oss:delete      | OSS 删除   | 超级管理员        |
| system:config:edit | OSS 配置测试 | 超级管理员     |

---

## 错误代码说明

| 错误代码 | 说明               |
|----------|------------------|
| 200      | 操作成功，连接正常        |
| 400      | OSS 配置未完成或无法连接   |
| 403      | 权限不足             |
| 500      | 服务器内部错误，客户端初始化失败 |

---

## 使用示例

### cURL 示例

```bash
# 测试 OSS 连接
curl -X GET http://localhost:8080/api/oss/test \
  -H "Authorization: <token>"

# 上传图片
curl -X POST http://localhost:8080/api/oss/upload \
  -H "Authorization: <token>" \
  -F "file=@/path/to/image.jpg"

# 删除图片
curl -X DELETE "http://localhost:8080/api/oss/delete?objectName=images/2026/03/26/abc123.jpg" \
  -H "Authorization: <token>"
```

### JavaScript 示例

```javascript
// 上传图片
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('/api/oss/upload', {
  method: 'POST',
  headers: {
    'Authorization': token
  },
  body: formData
})
  .then(response => response.json())
  .then(data => console.log(data));

// 删除图片
fetch('/api/oss/delete?objectName=images/2026/03/26/abc123.jpg', {
  method: 'DELETE',
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

## 图片处理说明

### 格式校验

上传前会校验：
1. 文件扩展名是否在允许列表中
2. 文件大小是否超过 10MB
3. 文件头魔数是否匹配对应格式（防止伪装的恶意文件）

### 无损压缩

- **JPEG/JPG**: 使用 95% 质量压缩，在保持视觉质量的同时减小文件体积
- **PNG**: 使用渐进式编码，减小文件体积
- **其他格式（GIF, BMP, WebP）**: 保持原样

### 图片存储路径

图片上传后存储在 OSS 的 `images/` 目录下，按日期分组织：
```
images/yyyy/MM/dd/UUID.ext
```

例如：`images/2026/03/26/a1b2c3d4e5f6.jpg`

---
