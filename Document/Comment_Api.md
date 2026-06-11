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

| 字段          | 类型      | 必填 | 说明                      |
|-------------|---------|----|-------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）            |
| pageSize    | Integer | 是  | 每页数量                    |
| status      | Integer | 否  | 状态筛选：0=待审核，1=已通过，2=垃圾评论 |

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

| 字段         | 类型       | 说明                    |
|------------|----------|-----------------------|
| id         | Long     | 评论ID                  |
| blogId     | Long     | 关联的文章ID               |
| blogTitle  | String   | 关联的文章标题（根据文章ID查询得出）   |
| parentId   | Long     | 父评论ID，0表示顶级评论         |
| username   | String   | 评论者名称                 |
| email      | String   | 邮箱                    |
| avatarUrl  | String   | 头像URL                 |
| website    | String   | 个人网站                  |
| content    | String   | 评论内容                  |
| status     | Integer  | 状态：0=待审核，1=已通过，2=垃圾评论 |
| likeCount  | Integer  | 点赞数                   |
| deviceInfo | String   | 设备信息                  |
| ipAddress  | String   | IP地址                  |
| isAdmin    | Boolean  | 是否管理员评论               |
| createTime | DateTime | 创建时间                  |
| updateTime | DateTime | 更新时间                  |

---

### 2. 修改自己的评论

根据 ID 修改指定评论，只能修改自己的评论。可修改评论内容和网站。

**注意**：如果评论是子评论，且其父评论处于「待审核」或「垃圾评论」状态，则无法修改此回复。

- **请求方法**: `PUT`
- **请求路径**: `/api/comment/{id}`
- **需要权限**: `comment:edit`

#### 路径参数

| 参数 | 类型   | 说明    |
|----|------|-------|
| id | Long | 评论 ID |

#### 请求参数

```json
{
  "content": "更新后的评论内容",
  "website": "https://new-site.com"
}
```

| 字段      | 类型     | 必填 | 说明             |
|---------|--------|----|----------------|
| content | String | 否  | 评论内容           |
| website | String | 否  | 个人网站（传空字符串可清空） |

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

#### 错误响应示例（父评论状态异常）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "父评论尚未通过审核，无法修改此回复",
  "code": 400
}
```

---

### 3. 删除自己的评论

删除指定 ID 的评论，只能删除自己的评论。**级联删除**：删除父评论时，所有子评论也会被一并删除。

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

### 4. 获取当前用户收到的回复评论列表

获取当前登录用户收到的回复评论列表（即其他用户在当前用户的评论下进行的回复）。

- **请求方法**: `GET`
- **请求路径**: `/api/comment/replies`
- **需要权限**: `comment:list`

#### 请求参数

| 参数  | 类型      | 必填 | 说明                  |
|-----|---------|----|---------------------|
| limit | Integer | 否  | 返回条数，最大100条，默认10条 |

#### 请求示例

```
GET /api/comment/replies?limit=20
```

#### 响应示例

```json
{
  "data": [
    {
      "id": 4,
      "parentId": 2,
      "username": "久流",
      "email": "",
      "avatarUrl": "https://api.myblog.icu/api/images/66d8abaee825b7d74238c6bc8a58f4a5",
      "website": null,
      "content": "项目仓库：https://github.com/DCSCDF。目前阶段主要还是打磨优化",
      "isAdmin": true,
      "deviceInfo": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
      "createTime": "2026-03-27T17:17:41",
      "updateTime": "2026-03-27T17:17:41",
      "children": null
    }
  ],
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 响应字段说明

| 字段         | 类型       | 说明                     |
|------------|----------|------------------------|
| id         | Long     | 回复评论ID                 |
| parentId   | Long     | 父评论ID（即当前用户的评论ID）    |
| username   | String   | 回复者名称                  |
| email      | String   | 回复者邮箱                  |
| avatarUrl  | String   | 回复者头像URL               |
| website    | String   | 回复者个人网站               |
| content    | String   | 回复内容                   |
| isAdmin    | Boolean  | 是否管理员回复               |
| deviceInfo | String   | 设备信息                   |
| createTime | DateTime | 创建时间                   |
| updateTime | DateTime | 更新时间                   |
| children   | null     | 子评论（返回null，不包含子评论列表） |

---

## 全局评论管理接口

基础路径：`/api/system/comment`

全局评论管理用于管理员管理所有用户的评论，支持关键词搜索、状态筛选、审核等功能。

---

### 5. 分页获取所有评论列表

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

| 字段          | 类型      | 必填 | 说明                      |
|-------------|---------|----|-------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）            |
| pageSize    | Integer | 是  | 每页数量                    |
| keyword     | String  | 否  | 搜索关键词（匹配评论者名称、邮箱、内容）    |
| status      | Integer | 否  | 状态筛选：0=待审核，1=已通过，2=垃圾评论 |

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

### 6. 修改任意评论

根据 ID 修改指定评论，管理员可以修改任意评论。可修改评论内容和网站。

**注意**：如果评论是子评论，且其父评论处于「待审核」或「垃圾评论」状态，则无法修改此回复。

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

### 7. 删除任意评论

删除指定 ID 的评论，管理员可以删除任意评论。**级联删除**：删除父评论时，所有子评论也会被一并删除。

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

### 8. 审核评论（修改状态）

将指定评论的审核状态改为「待审核」「已通过」或「垃圾评论」。

**注意**：当父评论被设为「待审核」或「垃圾评论」时，所有子评论也会被联动设为「待审核」状态。

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
5. **逻辑删除**：删除接口不会物理删除记录，只会将 `is_deleted` 置为 `1`，数据可通过数据库直接恢复。
6. **缓存机制**：评论列表接口实现了缓存机制，提高查询性能。数据变更时会自动清除缓存。
7. **级联删除**：删除父评论时，所有子评论会被一并删除。
8. **子评论状态限制**：如果父评论处于「待审核」或「垃圾评论」状态，子评论只能是「待审核」状态，且无法修改内容。
9. **审核联动**：当父评论被设为「待审核」或「垃圾评论」时，所有子评论也会被联动设为「待审核」状态。

---

## 邮件通知功能

系统支持评论相关的邮件通知功能，通过 `smtp.comment.enabled` 配置开关控制（需在系统配置中设置为 `true`）。

### 新评论通知（发送给管理员）

当有新评论提交时（无论审核通过还是未通过），系统会自动发送邮件通知给拥有 `system:comment:list` 权限的所有管理员。

**通知场景**：
- **游客评论**：邮件主题为「【待审核】网站有新评论需要处理」，提醒管理员需要进行审核操作
- **已登录用户评论**：邮件主题为「网站有新评论」，通知有新评论提交

**邮件内容包含**：
- 文章标题
- 评论者名称
- 评论内容
- 后台评论管理链接

### 顶级评论通过审核通知（发送给文章作者）

当顶级评论（`parentId=0`，即最顶级的父评论，没有父评论的评论）通过审核时，系统会自动发送邮件通知给文章作者。

**通知场景**：
- 游客提交的顶级评论被管理员审核通过
- 已登录用户提交的顶级评论自动通过（已登录用户评论默认直接通过审核）

**邮件内容包含**：
- 文章标题
- 评论者名称
- 评论内容
- 网站首页链接

### 评论回复通知（发送给被回复者）

当评论收到回复时，系统会发送邮件通知给被回复的评论作者。

### 评论审核结果通知（发送给评论作者）

当评论审核状态发生变化时（通过/未通过），系统会发送邮件通知给评论作者。
