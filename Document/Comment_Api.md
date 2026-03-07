## 评论管理接口文档

---

### 接口鉴权说明

所有评论接口均需要登录，且需在请求 header 中携带 token，格式为：`{tokenName: tokenValue}`（tokenName 默认为 token，可在 application.properties 中修改）。

评论管理使用以下权限码（来源于 `schema.sql` 初始化的 `sys_permission` 表）：

- `comment:list` - 查看评论列表
- `comment:edit` - 编辑评论
- `comment:delete` - 删除评论

评论数据存储在 `sys_comment` 表中，字段包括：关联文章ID、评论者名称、邮箱、头像URL、网站、评论内容、状态、点赞数、设备信息、IP地址等。

**状态说明（status）**：

- `0`：待审核
- `1`：已通过
- `2`：垃圾评论

---

## 用户评论管理接口

基础路径：`/api/comment`

---

### 1. 分页获取当前用户的评论列表

获取当前登录用户的评论列表，支持分页与状态筛选。响应中附带可用的状态筛选项（`filterOptions.status`），供前端渲染筛选控件。

- **请求方法**: `POST`
- **请求路径**: `/api/comment/list`
- **需要权限**: `comment:list`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "status": 1
}
```

| 字段          | 类型      | 必填 | 说明                                        |
|-------------|---------|----|-------------------------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）                              |
| pageSize    | Integer | 是  | 每页数量                                      |
| status      | Integer | 否  | 状态筛选：0=待审核，1=已通过，2=垃圾评论               |

#### 响应示例

```json
{
  "data": {
    "records": [
      {
        "id": 1,
        "blogId": 10,
        "blogTitle": "文章标题示例",
        "parentId": 0,
        "username": "评论者",
        "email": "comment@example.com",
        "avatarUrl": "https://example.com/avatar.png",
        "website": "https://example.com",
        "content": "评论内容",
        "status": 1,
        "likeCount": 5,
        "deviceInfo": "Mozilla/5.0",
        "ipAddress": "127.0.0.1",
        "isAdmin": false,
        "createTime": "2026-03-08T10:00:00",
        "updateTime": "2026-03-08T10:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1,
    "filterOptions": {
      "status": [
        { "value": 0, "label": "待审核" },
        { "value": 1, "label": "已通过" },
        { "value": 2, "label": "垃圾评论" }
      ]
    }
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 响应字段说明

| 字段          | 类型       | 说明                                    |
|-------------|----------|---------------------------------------|
| id          | Long     | 评论ID                                 |
| blogId      | Long     | 关联的文章ID                             |
| blogTitle   | String   | 关联的文章标题（根据文章ID查询得出）              |
| parentId    | Long     | 父评论ID，0表示顶级评论                       |
| username    | String   | 评论者名称                               |
| email       | String   | 邮箱                                    |
| avatarUrl   | String   | 头像URL                                 |
| website     | String   | 个人网站                                 |
| content     | String   | 评论内容                                 |
| status      | Integer  | 状态：0=待审核，1=已通过，2=垃圾评论             |
| likeCount   | Integer  | 点赞数                                   |
| deviceInfo  | String   | 设备信息                                 |
| ipAddress   | String   | IP地址                                  |
| isAdmin     | Boolean  | 是否管理员评论                            |
| createTime  | DateTime | 创建时间                                 |
| updateTime  | DateTime | 更新时间                                 |

---

### 2. 修改自己的评论

根据 ID 修改指定评论，只能修改自己的评论。可修改评论内容和网站。

- **请求方法**: `PUT`
- **请求路径**: `/api/comment/{id}`
- **需要权限**: `comment:edit`

#### 路径参数

| 参数 | 类型   | 说明      |
|----|------|---------|
| id | Long | 评论 ID |

#### 请求参数

```json
{
  "content": "更新后的评论内容",
  "website": "https://new-site.com"
}
```

| 字段     | 类型      | 必填 | 说明                          |
|--------|---------|----|-----------------------------|
| content | String  | 否  | 评论内容                          |
| website | String  | 否  | 个人网站（传空字符串可清空）          |

#### 响应示例

```json
{
  "data": {
    "id": 1,
    "blogId": 10,
    "blogTitle": "文章标题示例",
    "parentId": 0,
    "username": "评论者",
    "email": "comment@example.com",
    "avatarUrl": "https://example.com/avatar.png",
    "website": "https://new-site.com",
    "content": "更新后的评论内容",
    "status": 1,
    "likeCount": 5,
    "deviceInfo": "Mozilla/5.0",
    "ipAddress": "127.0.0.1",
    "isAdmin": false,
    "createTime": "2026-03-08T10:00:00",
    "updateTime": "2026-03-08T11:00:00"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（评论不存在）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "评论不存在",
  "code": 404
}
```

#### 错误响应示例（无权限修改他人评论）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "无权限修改该评论",
  "code": 403
}
```

---

### 3. 删除自己的评论

删除指定 ID 的评论，只能删除自己的评论。删除操作为**逻辑删除**：仅将 `status` 置为 `3`（已删除）。

- **请求方法**: `DELETE`
- **请求路径**: `/api/comment/{id}`
- **需要权限**: `comment:delete`

#### 路径参数

| 参数 | 类型   | 说明      |
|----|------|---------|
| id | Long | 评论 ID |

#### 响应示例

```json
{
  "data": "删除成功",
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（评论不存在）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "评论不存在",
  "code": 404
}
```

#### 错误响应示例（无权限删除他人评论）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "无权限删除该评论",
  "code": 403
}
```

---

## 全局评论管理接口

基础路径：`/api/system/comment`

全局评论管理用于管理员管理所有用户的评论，支持关键词搜索、状态筛选、审核等功能。

---

### 4. 分页获取所有评论列表

获取所有评论列表，支持分页、关键词搜索与状态筛选。响应中附带可用的状态筛选项（`filterOptions.status`）。

- **请求方法**: `POST`
- **请求路径**: `/api/system/comment/list`
- **需要权限**: `system:comment:list`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "评论者",
  "status": 1
}
```

| 字段          | 类型      | 必填 | 说明                                        |
|-------------|---------|----|-------------------------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）                              |
| pageSize    | Integer | 是  | 每页数量                                      |
| keyword     | String  | 否  | 搜索关键词（匹配评论者名称、邮箱、内容）                  |
| status      | Integer | 否  | 状态筛选：0=待审核，1=已通过，2=垃圾评论               |

#### 响应示例

```json
{
  "data": {
    "records": [
      {
        "id": 1,
        "blogId": 10,
        "blogTitle": "文章标题示例",
        "parentId": 0,
        "username": "评论者",
        "email": "comment@example.com",
        "avatarUrl": "https://example.com/avatar.png",
        "website": "https://example.com",
        "content": "评论内容",
        "status": 1,
        "likeCount": 5,
        "deviceInfo": "Mozilla/5.0",
        "ipAddress": "127.0.0.1",
        "isAdmin": false,
        "createTime": "2026-03-08T10:00:00",
        "updateTime": "2026-03-08T10:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1,
    "filterOptions": {
      "status": [
        { "value": 0, "label": "待审核" },
        { "value": 1, "label": "已通过" },
        { "value": 2, "label": "垃圾评论" }
      ]
    }
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

---

### 5. 修改任意评论

根据 ID 修改指定评论，管理员可以修改任意评论。可修改评论内容和网站。

- **请求方法**: `PUT`
- **请求路径**: `/api/system/comment/{id}`
- **需要权限**: `system:comment:edit`

#### 路径参数

| 参数 | 类型   | 说明      |
|----|------|---------|
| id | Long | 评论 ID |

#### 请求参数

```json
{
  "content": "管理员更新的评论内容",
  "website": "https://admin-site.com"
}
```

| 字段     | 类型      | 必填 | 说明                          |
|--------|---------|----|-----------------------------|
| content | String  | 否  | 评论内容                          |
| website | String  | 否  | 个人网站（传空字符串可清空）          |

#### 响应示例

```json
{
  "data": {
    "id": 1,
    "blogId": 10,
    "blogTitle": "文章标题示例",
    "parentId": 0,
    "username": "评论者",
    "email": "comment@example.com",
    "avatarUrl": "https://example.com/avatar.png",
    "website": "https://admin-site.com",
    "content": "管理员更新的评论内容",
    "status": 1,
    "likeCount": 5,
    "deviceInfo": "Mozilla/5.0",
    "ipAddress": "127.0.0.1",
    "isAdmin": false,
    "createTime": "2026-03-08T10:00:00",
    "updateTime": "2026-03-08T11:00:00"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（评论不存在）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "评论不存在",
  "code": 404
}
```

---

### 6. 删除任意评论

删除指定 ID 的评论，管理员可以删除任意评论。删除操作为**逻辑删除**：仅将 `status` 置为 `3`（已删除）。

- **请求方法**: `DELETE`
- **请求路径**: `/api/system/comment/{id}`
- **需要权限**: `system:comment:delete`

#### 路径参数

| 参数 | 类型   | 说明      |
|----|------|---------|
| id | Long | 评论 ID |

#### 响应示例

```json
{
  "data": "删除成功",
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（评论不存在）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "评论不存在",
  "code": 404
}
```

---

### 7. 审核评论（修改状态）

将指定评论的审核状态改为「待审核」「已通过」或「垃圾评论」。

- **请求方法**: `PUT`
- **请求路径**: `/api/system/comment/{id}/approve`
- **需要权限**: `system:comment:approve`

#### 路径参数

| 参数 | 类型   | 说明      |
|----|------|---------|
| id | Long | 评论 ID |

#### 请求参数

```json
{
  "status": 1
}
```

| 字段     | 类型      | 必填 | 说明                              |
|--------|---------|----|---------------------------------|
| status | Integer | 是  | 审核状态：0=待审核，1=已通过，2=垃圾评论 |

#### 响应示例

```json
{
  "data": {
    "id": 1,
    "blogId": 10,
    "blogTitle": "文章标题示例",
    "parentId": 0,
    "username": "评论者",
    "email": "comment@example.com",
    "avatarUrl": "https://example.com/avatar.png",
    "website": "https://example.com",
    "content": "评论内容",
    "status": 1,
    "likeCount": 5,
    "deviceInfo": "Mozilla/5.0",
    "ipAddress": "127.0.0.1",
    "isAdmin": false,
    "createTime": "2026-03-08T10:00:00",
    "updateTime": "2026-03-08T11:00:00"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（评论不存在）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "评论不存在",
  "code": 404
}
```

#### 错误响应示例（状态值无效）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "状态值无效，仅支持 0=待审核，1=已通过，2=垃圾评论",
  "code": 400
}
```

---

## 注意事项

1. **用户权限限制**：普通用户只能查看、修改、删除自己的评论，无法操作他人的评论。
2. **全局管理**：管理员可以通过全局评论接口管理所有用户的评论，包括修改内容、删除评论、审核评论等。
3. **状态筛选**：列表接口支持按 `status` 筛选，前端可使用 `filterOptions.status` 构建下拉框或筛选组件。
4. **文章标题关联**：评论列表响应中会包含关联的文章标题（`blogTitle`），根据 `blogId` 查询得出。
5. **逻辑删除**：删除接口不会物理删除记录，只会将 `status` 置为 `3`（已删除），数据可通过数据库直接恢复。
6. **缓存机制**：评论列表接口实现了缓存机制，提高查询性能。数据变更时会自动清除缓存。
