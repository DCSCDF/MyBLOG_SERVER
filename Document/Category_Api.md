## 文章分类管理接口文档

---

### 接口鉴权说明

所有文章分类接口均需要登录，且需在请求 header 中携带 token，格式为：`{tokenName: tokenValue}`（tokenName 默认为 token，可在 application.properties 中修改）。

文章分类管理使用以下权限码（来源于 `schema.sql` 初始化的 `sys_permission` 表）：

- `category:list` - 查看分类列表
- `category:create` - 创建分类
- `category:edit` - 编辑分类
- `category:delete` - 删除分类

分类数据存储在 `sys_category` 表中，字段包括：分类名称、描述、排序数、是否隐藏、作者ID 等。

**状态说明（hidden）**：

- `false`：显示
- `true`：隐藏

---

## 文章分类管理接口

基础路径：`/api/categories`

---

### 1. 分页获取分类列表

获取分类列表，支持分页、关键词搜索与隐藏状态筛选。响应中附带可用的隐藏状态筛选项（`filterOptions.hidden`），供前端渲染筛选控件。**列表只会返回当前登录用户创建的分类（按 `author_id` = 当前用户ID 过滤）**。

- **请求方法**: `POST`
- **请求路径**: `/api/categories/list`
- **需要权限**: `category:list`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "技术",
  "hidden": false
}
```

| 字段          | 类型      | 必填 | 说明                                  |
|-------------|---------|----|-------------------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）                        |
| pageSize    | Integer | 是  | 每页数量                                |
| keyword     | String  | 否  | 搜索关键词（匹配 `name`、`description`）      |
| hidden      | Boolean | 否  | 隐藏状态筛选：`false`=显示，`true`=隐藏，null=全部 |

#### 响应示例

```json
{
  "data": {
    "records": [
      {
        "id": 1,
        "name": "技术分享",
        "description": "技术相关文章分类",
        "sortOrder": 10,
        "hidden": false,
        "createTime": "2026-03-02T10:00:00",
        "updateTime": "2026-03-02T10:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1,
    "filterOptions": {
      "hidden": [
        { "value": 0, "label": "显示" },
        { "value": 1, "label": "隐藏" }
      ]
    }
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

---

### 2. 创建分类

前端提交分类名称、分类描述、排序顺序，创建一条新的分类记录。**创建时后台会自动将 `hidden` 设置为 `false`（不隐藏），并根据当前请求的 token 解析出用户ID，写入 `author_id` 字段**。

- **请求方法**: `POST`
- **请求路径**: `/api/categories`
- **需要权限**: `category:create`

#### 请求参数

```json
{
  "name": "技术分享",
  "description": "技术相关文章分类",
  "sortOrder": 10
}
```

| 字段          | 类型      | 必填 | 说明                  |
|-------------|---------|----|---------------------|
| name        | String  | 是  | 分类名称，前端展示用          |
| description | String  | 否  | 分类描述，用于说明该分类的用途     |
| sortOrder   | Integer | 否  | 排序数，数字越大越靠前；未传则默认 0 |

#### 响应示例

```json
{
  "data": {
    "id": 2,
    "name": "技术分享",
    "description": "技术相关文章分类",
    "sortOrder": 10,
    "hidden": false,
    "createTime": "2026-03-02T10:10:00",
    "updateTime": "2026-03-02T10:10:00"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

---

### 3. 修改分类

根据 ID 修改指定分类，可修改分类名称、分类描述、排序顺序、是否隐藏等字段。**仅允许修改当前登录用户自己创建的分类（`author_id` 必须等于当前用户ID），否则返回 403 无权限错误。**

- **请求方法**: `PUT`
- **请求路径**: `/api/categories/{id}`
- **需要权限**: `category:edit`

#### 路径参数

| 参数 | 类型   | 说明    |
|----|------|-------|
| id | Long | 分类 ID |

#### 请求参数

```json
{
  "name": "更新后的技术分享",
  "description": "更新后的分类描述",
  "sortOrder": 20,
  "hidden": true
}
```

所有字段均为**可选**，仅更新传入的字段。

#### 响应示例

```json
{
  "data": {
    "id": 2,
    "name": "更新后的技术分享",
    "description": "更新后的分类描述",
    "sortOrder": 20,
    "hidden": true,
    "createTime": "2026-03-02T10:10:00",
    "updateTime": "2026-03-02T11:00:00"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（分类不存在）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "分类不存在",
  "code": 404
}
```

---

### 4. 删除分类

删除指定 ID 的分类。目前删除操作为**物理删除**：直接调用 `deleteById` 删除记录。**仅允许删除当前登录用户自己创建的分类（`author_id` 必须等于当前用户ID），否则返回 403 无权限错误。**

- **请求方法**: `DELETE`
- **请求路径**: `/api/categories/{id}`
- **需要权限**: `category:delete`

#### 路径参数

| 参数 | 类型   | 说明    |
|----|------|-------|
| id | Long | 分类 ID |

#### 响应示例

```json
{
  "data": "删除成功",
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（分类不存在）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "分类不存在",
  "code": 404
}
```

---

## 注意事项

1. **排序规则**：列表接口默认按 `sortOrder` 降序、再按 `createTime` 降序排列，同排序数时新建的排在前面。
2. **隐藏状态筛选**：列表接口支持按 `hidden` 筛选，前端可使用 `filterOptions.hidden` 构建下拉框或筛选组件（0=显示，1=隐藏）。
3. **是否隐藏**：通过更新接口传入 `hidden` 字段即可控制分类在前台是否展示。
4. **多作者隔离**：所有分类接口（列表、创建、修改、删除）都会自动根据 token 识别当前用户，仅能操作 `author_id` 等于当前登录用户ID的分类记录，确保不同作者之间的数据隔离。

