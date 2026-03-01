## 友情链接管理接口文档

---

### 接口鉴权说明

所有友情链接接口均需要登录，且需在请求 header 中携带 token，格式为：`{tokenName: tokenValue}`（tokenName 默认为 token，可在 application.properties 中修改）。

友情链接管理使用以下权限码（来源于 `schema.sql` 初始化的 `sys_permission` 表）：

- `links:list` - 查看友情链接列表
- `links:create` - 创建友情链接
- `links:edit` - 编辑友情链接
- `links:delete` - 删除友情链接

外链数据存储在 `sys_friend_link` 表中，字段包括：链接名称、URL地址、简介、备注、图片URL、排序数、审核状态等。

**状态说明（status）**：

- `0`：待审核
- `1`：已通过
- `2`：已拒绝


---

## 友情链接管理接口

基础路径：`/api/friend-link`

---

### 1. 分页获取友情链接列表

获取友情链接列表，支持分页、关键词搜索与状态筛选。响应中附带可用的状态筛选项（`filterOptions.status`），供前端渲染筛选控件。

- **请求方法**: `POST`
- **请求路径**: `/api/friend-link/list`
- **需要权限**: `links:list`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "博客",
  "status": 1
}
```

| 字段          | 类型      | 必填 | 说明                                        |
|-------------|---------|----|-------------------------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）                              |
| pageSize    | Integer | 是  | 每页数量                                      |
| keyword     | String  | 否  | 搜索关键词（匹配 `name`、`url`、`summary`、`remark`） |
| status      | Integer | 否  | 状态筛选：0=待审核，1=已通过，2=已拒绝，3=已删除              |

#### 响应示例

```json
{
  "data": {
    "records": [
      {
        "id": 1,
        "name": "示例博客",
        "url": "https://example.com",
        "summary": "一个示例技术博客站点",
        "remark": "长期合作友链",
        "imageUrl": "https://example.com/logo.png",
        "sortOrder": 10,
        "status": 1,
        "createTime": "2026-02-23T10:00:00",
        "updateTime": "2026-02-23T10:00:00"
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
        { "value": 2, "label": "已拒绝" }
      ]
    }
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

---

### 2. 创建友情链接（默认审核通过）

前端提交链接名称、URL地址、简介、备注、图片URL、排序数，创建一条新的友情链接记录。**创建时后台会自动将 `status` 设置为 `1`（已通过）**。

- **请求方法**: `POST`
- **请求路径**: `/api/friend-link`
- **需要权限**: `links:create`

#### 请求参数

```json
{
  "name": "示例博客",
  "url": "https://example.com",
  "summary": "一个示例技术博客站点",
  "remark": "长期合作友链",
  "imageUrl": "https://example.com/logo.png",
  "sortOrder": 10
}
```

| 字段        | 类型      | 必填 | 说明                         |
|-----------|---------|----|----------------------------|
| name      | String  | 是  | 链接名称，前端展示用                 |
| url       | String  | 是  | URL地址，需为有效 http/https 链接   |
| summary   | String  | 否  | 简介，简要介绍该站点或链接用途            |
| remark    | String  | 否  | 备注，仅后台使用的备注说明              |
| imageUrl  | String  | 否  | 图片URL（如站点Logo），用于前端展示头像或图标 |
| sortOrder | Integer | 否  | 排序数，数字越大越靠前；未传则默认 0        |

#### 响应示例

```json
{
  "data": {
    "id": 2,
    "name": "示例博客",
    "url": "https://example.com",
    "summary": "一个示例技术博客站点",
    "remark": "长期合作友链",
    "imageUrl": "https://example.com/logo.png",
    "sortOrder": 10,
    "status": 1,
    "createTime": "2026-02-23T10:10:00",
    "updateTime": "2026-02-23T10:10:00"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

---

### 3. 修改友情链接

根据 ID 修改指定友情链接，可修改链接名称、URL地址、简介、备注、图片URL、排序数等字段。

- **请求方法**: `PUT`
- **请求路径**: `/api/friend-link/{id}`
- **需要权限**: `links:edit`

#### 路径参数

| 参数 | 类型   | 说明      |
|----|------|---------|
| id | Long | 友情链接 ID |

#### 请求参数

```json
{
  "name": "更新后的示例博客",
  "url": "https://example.com",
  "summary": "更新后的简介",
  "remark": "备注信息已更新",
  "imageUrl": "https://example.com/new-logo.png",
  "sortOrder": 20
}
```

所有字段均为**可选**，仅更新传入的字段。

#### 响应示例

```json
{
  "data": {
    "id": 2,
    "name": "更新后的示例博客",
    "url": "https://example.com",
    "summary": "更新后的简介",
    "remark": "备注信息已更新",
    "imageUrl": "https://example.com/new-logo.png",
    "sortOrder": 20,
    "status": 1,
    "createTime": "2026-02-23T10:10:00",
    "updateTime": "2026-02-23T11:00:00"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（友情链接不存在或已删除）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "外链不存在",
  "code": 404
}
```

---

### 4. 变更友链审核状态

将指定友链的审核状态改为「待审核」「已通过」或「已拒绝」。支持：待审核→通过/拒绝、已通过→拒绝、已拒绝→通过等任意在 0/1/2 之间的切换。

- **请求方法**: `PUT`
- **请求路径**: `/api/friend-link/{id}/status`
- **需要权限**: `links:edit`

#### 路径参数

| 参数 | 类型   | 说明      |
|----|------|---------|
| id | Long | 友情链接 ID |

#### 请求参数

```json
{
  "status": 1
}
```

| 字段     | 类型      | 必填 | 说明                     |
|--------|---------|----|------------------------|
| status | Integer | 是  | 审核状态：0=待审核，1=已通过，2=已拒绝 |

#### 响应示例

返回更新后的完整友链对象（同「修改友情链接」的 data 结构）。

```json
{
  "data": {
    "id": 2,
    "name": "示例博客",
    "url": "https://example.com",
    "summary": "一个示例技术博客站点",
    "remark": "长期合作友链",
    "imageUrl": "https://example.com/logo.png",
    "sortOrder": 10,
    "status": 1,
    "createTime": "2026-02-23T10:10:00",
    "updateTime": "2026-02-23T11:00:00"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（友链不存在或已删除）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "友链不存在",
  "code": 404
}
```

```json
{
  "data": null,
  "success": false,
  "errorMsg": "友链已删除",
  "code": 404
}
```

---

### 5. 删除友情链接

删除指定 ID 的友情链接。删除操作为**逻辑删除**：仅将 `isDeleted` 置为 `1`，并将 `status` 标记为 `3`（已删除），数据不会被物理删除。

- **请求方法**: `DELETE`
- **请求路径**: `/api/friend-link/{id}`
- **需要权限**: `links:delete`

#### 路径参数

| 参数 | 类型   | 说明      |
|----|------|---------|
| id | Long | 友情链接 ID |

#### 响应示例

```json
{
  "data": "删除成功",
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（友情链接不存在或已删除）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "外链不存在",
  "code": 404
}
```

---

## 注意事项

1. **默认审核通过**：通过创建接口新增的友情链接，后台会自动将 `status` 设为 `1`（已通过），可直接在前台展示。
2. **状态筛选**：列表接口支持按 `status` 筛选，前端可使用 `filterOptions.status` 构建下拉框或筛选组件。
3. **排序规则**：默认按 `sortOrder` 降序、再按 `createTime` 降序排列，同排序数时新建的排在前面。
4. **审核状态变更**：通过「变更友链审核状态」接口可将待审核改为通过/拒绝，或将已通过改为已拒绝、已拒绝改为已通过（或改回待审核），状态仅允许 0/1/2。
5. **逻辑删除**：删除接口不会物理删除记录，只会将 `isDeleted` 置为 1，并把 `status` 置为 3（已删除），便于后续审计或恢复。

