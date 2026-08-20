# 全局OSS管理 API Documentation

## 概述

全局OSS管理接口用于管理员查看和删除所有用户上传的OSS图片。

> **统一响应格式说明**：以下示例中出现的 `code/msg/data` 为简化写法，实际响应体为项目统一格式：`{ "data": ..., "success": true, "errorMsg": null, "code": 200 }`（错误时 `success=false`，`errorMsg` 为错误信息）。

**基础路径**: `/api/global-oss`

**需要权限**: `system:oss:list`（列表查看）、`system:oss:delete`（删除）

---

## API 接口

### 1. 分页获取OSS图片列表

获取所有用户上传的图片列表，支持分页和关键词搜索。

- **URL**: `POST /api/global-oss/list`
- **权限**: `system:oss:list`

#### 请求参数 (JSON)

| 参数名      | 类型    | 必填 | 说明                     |
|-----------|-------|----|------------------------|
| currentPage | Integer | 是   | 当前页码，从1开始             |
| pageSize   | Integer | 是   | 每页数量                   |
| keyword    | String | 否   | 搜索关键词，匹配文件名、哈希值、用户名 |

#### 请求示例

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "test"
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
        "userId": 1,
        "username": "admin",
        "createTime": "2026-03-26T10:30:00"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
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
| records[].userId | Long    | 上传用户ID        |
| records[].username | String  | 上传用户名         |
| records[].createTime | String  | 创建时间          |
| total           | Long    | 总记录数          |
| size            | Long    | 每页数量          |
| current         | Long    | 当前页码          |
| pages           | Long    | 总页数           |

#### 错误响应

**1. 权限不足**

```json
{
  "code": 403,
  "msg": "无权限访问",
  "data": null
}
```

---

### 2. 删除图片（管理员）

管理员可以删除任意用户的图片。

- **URL**: `DELETE /api/global-oss/{hash}`
- **权限**: `system:oss:delete`

#### 路径参数

| 参数名 | 类型   | 必填 | 说明           |
| ------ | ------ | ---- | -------------- |
| hash  | String | 是   | 图片哈希值（MD5） |

#### 成功响应

```json
{
  "code": 200,
  "msg": "删除成功",
  "data": null
}
```

> **删除流程说明**：先删除数据库记录，OSS 远程对象在**事务提交后**删除。若 OSS 删除失败，数据库记录已删除（不会出现"远程已删、记录残留"的悬空状态），遗留对象由运维对账清理。

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

**3. OSS配置未完成**

```json
{
  "code": 400,
  "msg": "OSS 配置未完成，请先在系统配置中完成阿里云 OSS 相关配置",
  "data": null
}
```

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

| 权限码                | 说明          | 所属角色  |
|--------------------|-------------|-------|
| system:oss:list    | 全局OSS列表查看 | 超级管理员 |
| system:oss:delete  | 全局OSS图片删除 | 超级管理员 |

> **注意**: 需要在系统权限表中添加这两个权限。

---

## 错误代码说明

| 错误代码 | 说明         |
|------|------------|
| 200  | 操作成功       |
| 400  | 参数错误或配置不完整 |
| 401  | 未登录        |
| 403  | 权限不足       |
| 404  | 资源不存在      |
| 500  | 服务器内部错误    |

---

## 缓存说明

全局OSS列表接口使用本地内存缓存：

- **缓存时间**: 10 分钟
- **最大缓存数量**: 100 条
- **缓存清除**: 当有图片上传或删除操作时，会自动清除缓存

---

## 使用示例

### cURL 示例

```bash
# 分页获取图片列表
curl -X POST http://localhost:8080/api/global-oss/list \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"currentPage":1,"pageSize":10}'

# 搜索图片
curl -X POST http://localhost:8080/api/global-oss/list \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"currentPage":1,"pageSize":10,"keyword":"avatar"}'

# 删除图片（管理员可删除任意图片）
curl -X DELETE "http://localhost:8080/api/global-oss/a1b2c3d4e5f6..." \
  -H "Authorization: <token>"
```

### JavaScript 示例

```javascript
// 分页获取图片列表
const response = await fetch('/api/global-oss/list', {
  method: 'POST',
  headers: {
    'Authorization': token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    currentPage: 1,
    pageSize: 10,
    keyword: 'avatar'
  })
});
const result = await response.json();

// 删除图片
await fetch('/api/global-oss/a1b2c3d4e5f6...', {
  method: 'DELETE',
  headers: {
    'Authorization': token
  }
});
```

---
