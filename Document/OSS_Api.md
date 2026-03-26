# OSS API Documentation

## 概述

OSS 模块提供阿里云对象存储（OSS）连接测试、图片上传、图片删除和图片获取功能。

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

| 参数名 | 类型   | 必填 | 说明             |
|------|------|----|----------------|
| file | File | 是  | 图片文件（不超过 10MB） |

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
    "hash": "5d41402abc4b2a76b9719d911017c592",
    "originalName": "原始名称.jpg",
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
  "msg": "图片大小不能超过 10MB",
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

### 3. 分页获取当前用户的OSS图片列表

获取当前登录用户上传的图片列表，支持分页和关键词搜索。

- **URL**: `POST /api/oss/list`
- **权限**: `oss:list`

#### 请求参数 (JSON)

| 参数名      | 类型    | 必填 | 说明                     |
|-----------|-------|----|------------------------|
| currentPage | Integer | 是   | 当前页码，从1开始             |
| pageSize   | Integer | 是   | 每页数量                   |
| keyword    | String | 否   | 搜索关键词，匹配图片名称、哈希值   |

#### 请求示例

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "avatar"
}
```

#### 成功响应

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "hash": "a1b2c3d4e5f6...",
        "originalName": "avatar.jpg",
        "objectName": "images/2026/03/26/avatar_123_456_xxx.jpg",
        "fileSize": 102400,
        "createTime": "2026-03-26T10:30:00"
      }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "pages": 5
  }
}
```

#### 响应参数说明

| 参数名              | 类型      | 说明            |
|-----------------|---------|---------------|
| records         | Array   | 图片记录列表        |
| records[].id    | Long    | 图片ID          |
| records[].hash  | String  | 图片哈希值（MD5）     |
| records[].originalName | String  | 原始文件名         |
| records[].objectName | String  | OSS对象名称（文件路径） |
| records[].fileSize | Long    | 文件大小（字节）      |
| records[].createTime | String  | 创建时间          |
| total           | Long    | 总记录数          |
| size            | Long    | 每页数量          |
| current         | Long    | 当前页码          |
| pages           | Long    | 总页数           |

---

### 4. 删除图片（通过哈希值）

通过图片哈希值删除图片。**只有上传该图片的用户才能删除，其他用户无法删除。**

- **URL**: `DELETE /api/oss/delete/{hash}`
- **权限**: `oss:delete`

#### 路径参数

| 参数名 | 类型   | 必填 | 说明           |
|------|------|----|--------------|
| hash | String | 是   | 图片哈希值（MD5） |

#### 成功响应

```json
{
  "code": 200,
  "msg": "删除成功",
  "data": null
}
```

#### 错误响应

**1. 哈希值为空**

```json
{
  "code": 400,
  "msg": "哈希值不能为空",
  "data": null
}
```

**2. 图片记录不存在**

```json
{
  "code": 404,
  "msg": "图片记录不存在",
  "data": null
}
```

**3. 无权限删除（用户只能删除自己上传的图片）**

```json
{
  "code": 403,
  "msg": "无权限删除此图片",
  "data": null
}
```

> **权限说明**: 删除时会校验当前登录用户的ID是否与图片记录中的user_id匹配，只有匹配才能删除。

---

### 4. 获取图片（公开接口）

通过哈希值获取 OSS 中的图片，支持 30 分钟本地缓存。

- **URL**: `GET /api/images/{hash}`
- **权限**: 无（公开接口）

#### 路径参数

| 参数名 | 类型   | 必填 | 说明           |
|------|------|----|--------------|
| hash | String | 是   | 图片哈希值（MD5） |

#### 成功响应

返回图片二进制数据，响应头包含：
- `Content-Type`: 图片 MIME 类型
- `Content-Length`: 图片大小
- `Cache-Control`: `public, max-age=1800`（30 分钟浏览器缓存）

#### 错误响应

**1. 图片记录不存在**

```
HTTP 404 Not Found
```

**2. OSS 客户端不可用**

```
HTTP 503 Service Unavailable
```

---

## OSS 配置项说明

在系统配置表 (`sys_config`) 中需要配置以下 OSS 相关配置项：

| 配置键                   | 类型      | 默认值      | 说明                                        |
|------------------------|---------|----------|-------------------------------------------|
| aliyun.Access-key      | string  | -        | 阿里云 AccessKey ID                          |
| aliyun.Secret-key      | string  | -        | 阿里云 AccessKey Secret                      |
| aliyun.Bucket          | string  | -        | OSS Bucket 名称                             |
| aliyun.end-point       | string  | -        | OSS 访问域名（如 oss-cn-hangzhou.aliyuncs.com） |
| aliyun.https-enabled   | boolean | `false`  | 是否启用 HTTPS 访问                            |

---

## 数据库表结构

### sys_oss_image（OSS 图片映射表）

| 字段名            | 类型           | 说明              |
|----------------|--------------|-----------------|
| id             | BIGINT       | 图片ID（主键）        |
| hash           | VARCHAR(128) | 图片哈希值（MD5，唯一）   |
| original_name  | VARCHAR(128) | 原始文件名（截断至128位） |
| object_name    | VARCHAR(500) | OSS 对象名称（文件路径）  |
| file_size      | BIGINT       | 文件大小（字节）        |
| user_id        | BIGINT       | 上传用户ID          |
| create_time    | DATETIME     | 创建时间            |

---

## 权限说明

| 权限码                | 说明       | 所属角色  |
|--------------------|----------|-------|
| oss:create         | OSS 上传   | 超级管理员 |
| oss:delete         | OSS 删除   | 超级管理员 |
| oss:list           | OSS 列表查看 | 超级管理员 |
| system:config:edit | OSS 配置测试 | 超级管理员 |
| system:oss:list    | 全局OSS列表查看 | 超级管理员 |
| system:oss:delete  | 全局OSS图片删除 | 超级管理员 |

---

## 错误代码说明

| 错误代码 | 说明         |
|------|------------|
| 200  | 操作成功       |
| 400  | 参数错误或配置不完整 |
| 403  | 权限不足       |
| 404  | 资源不存在      |
| 500  | 服务器内部错误    |

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

# 删除图片（通过哈希值）
curl -X DELETE "http://localhost:8080/api/oss/delete/5d41402abc4b2a76b9719d911017c592" \
  -H "Authorization: <token>"

# 获取图片（公开接口）
curl -O http://localhost:8080/api/images/5d41402abc4b2a76b9719d911017c592
```

### JavaScript 示例

```javascript
// 上传图片
const formData = new FormData();
formData.append('file', fileInput.files[0]);

const response = await fetch('/api/oss/upload', {
  method: 'POST',
  headers: {
    'Authorization': token
  },
  body: formData
});
const result = await response.json();
// result.data.hash 即为图片哈希值，可用于访问图片

// 删除图片
await fetch('/api/oss/delete/5d41402abc4b2a76b9719d911017c592', {
  method: 'DELETE',
  headers: {
    'Authorization': token
  }
});

// 获取图片（可直接在 img 标签中使用）
// <img src="/api/images/5d41402abc4b2a76b9719d911017c592" />
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

### 图片存储结构

```
images/yyyy/MM/dd/新文件名.扩展名

新文件名格式：原始名称（截断至128位）_时间戳_文件大小_16位随机字符
例如：图片_1743000000000_102400_aBcDeFgHiJkLmNoP.jpg
```

### 防重复上传

上传前会计算图片的 MD5 哈希值：
- 如果哈希值已存在于数据库，说明图片已上传过，直接返回已有记录，不重复上传

---

## 图片缓存说明

### 公开图片获取缓存

公开图片获取接口 (`/api/images/{hash}`) 使用本地内存缓存：

- **缓存时间**: 30 分钟
- **最大缓存数量**: 1000 条
- **缓存命中**: 直接返回缓存数据，减少 OSS 请求
- **缓存未命中**: 从 OSS 下载后存入缓存再返回

浏览器也会对图片进行 30 分钟缓存（`Cache-Control: public, max-age=1800`）。

### 用户OSS列表缓存

用户OSS列表接口 (`/api/oss/list`) 使用本地内存缓存：

- **缓存时间**: 5 分钟
- **最大缓存数量**: 100 条
- **缓存清除**: 当用户上传或删除图片时，会自动清除该用户的OSS列表缓存

### 全局OSS列表缓存

全局OSS管理接口 (`/api/global-oss/list`) 使用本地内存缓存：

- **缓存时间**: 10 分钟
- **最大缓存数量**: 100 条
- **缓存清除**: 当有图片上传或删除操作时，会自动清除缓存

---
