## 账号管理（用户管理）接口文档

---

### 接口鉴权说明

所有账号管理接口均需要登录，且需在请求 header 中携带 token，格式为：`{tokenName: tokenValue}`（tokenName 默认为 token，可在 `application.properties` 中修改）。

接口使用 Sa-Token 权限码控制访问，权限码来源于 `schema.sql` 初始化的 `sys_permission`。

---

## 用户管理接口

基础路径：`/api/user`

### 分页获取用户列表

查看程序中的所有用户（仅返回未删除用户），支持分页、关键词搜索与状态筛选。响应中附带可用的筛选项（`filterOptions`），供前端渲染筛选控件。

- **请求方法**: `POST`
- **请求路径**: `/api/user/list`
- **需要权限**: `system:user:list`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "admin",
  "status": 1
}
```

| 字段          | 类型      | 必填 | 说明                                |
|-------------|---------|----|-----------------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）                      |
| pageSize    | Integer | 是  | 每页数量                              |
| keyword     | String  | 否  | 搜索关键词（匹配 username、nickname、email） |
| status      | Integer | 否  | 状态筛选：0=禁用，1=启用                    |

#### 响应示例

```json
{
  "data": {
    "records": [
      {
        "id": 1,
        "username": "admin",
        "nickname": "管理员",
        "email": "admin@example.com",
        "avatarUrl": "https://example.com/avatar.png",
        "status": 1,
        "createTime": "2026-01-01T00:00:00",
        "updateTime": "2026-01-01T00:00:00",
        "roles": [
          {
            "id": 1,
            "code": "SUPER_ADMIN",
            "name": "超级管理员",
            "description": "拥有系统所有权限，只能有一个",
            "superAdmin": true,
            "isSystem": true,
            "sortOrder": 100,
            "status": 1,
            "createTime": "2026-01-01T00:00:00",
            "updateTime": "2026-01-01T00:00:00"
          }
        ]
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1,
    "filterOptions": {
      "status": [
        { "value": 0, "label": "禁用" },
        { "value": 1, "label": "启用" }
      ]
    }
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

| 响应字段                           | 说明                                                                  |
|--------------------------------|---------------------------------------------------------------------|
| records                        | 当前页用户列表，每项含 `roles` 当前角色列表                                          |
| total / size / current / pages | 分页信息                                                                |
| filterOptions                  | 可用筛选项，key 为筛选项名称（如 `status`），value 为 `{ value, label }` 数组，供前端下拉等使用 |

---

### 获取用户详情

获取指定用户的基础信息。

- **请求方法**: `GET`
- **请求路径**: `/api/user/{id}`
- **需要权限**: `system:user:list`

#### 响应示例

```json
{
  "data": {
    "id": 2,
    "username": "test",
    "nickname": "测试用户",
    "email": "test@example.com",
    "avatarUrl": null,
    "status": 1,
    "createTime": "2026-01-01T00:00:00",
    "updateTime": "2026-01-01T00:00:00",
    "roles": [
      {
        "id": 2,
        "code": "ADMIN",
        "name": "普通管理员",
        "description": "拥有系统大部分管理权限",
        "superAdmin": false,
        "isSystem": true,
        "sortOrder": 90,
        "status": 1,
        "createTime": "2026-01-01T00:00:00",
        "updateTime": "2026-01-01T00:00:00"
      }
    ]
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 用户不存在响应

```json
{
  "data": null,
  "success": false,
  "errorMsg": "用户不存在",
  "code": 404
}
```

---

### 获取用户角色列表

查看指定用户的角色列表。

- **请求方法**: `GET`
- **请求路径**: `/api/user/{id}/roles`
- **需要权限**: `system:user:assignRole`

#### 响应示例

```json
{
  "data": [
    {
      "id": 2,
      "code": "ADMIN",
      "name": "普通管理员",
      "description": "拥有系统大部分管理权限",
      "superAdmin": false,
      "isSystem": true,
      "sortOrder": 90,
      "status": 1,
      "createTime": "2026-01-01T00:00:00",
      "updateTime": "2026-01-01T00:00:00"
    }
  ],
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

---

### 修改用户信息（昵称/头像/角色）

可修改：昵称、头像 URL、角色（传入 `roleId` 时将 **覆盖** 用户现有角色为单一角色）。

- **请求方法**: `PUT`
- **请求路径**: `/api/user/{id}`
- **需要权限**: `system:user:edit`

#### 请求参数

```json
{
  "nickname": "新昵称",
  "avatarUrl": "https://example.com/new-avatar.png",
  "roleId": 3
}
```

| 字段        | 类型     | 必填 | 说明                        |
|-----------|--------|----|---------------------------|
| nickname  | String | 否  | 昵称（最大 50）                 |
| avatarUrl | String | 否  | 头像 URL（最大 200，可传 null 清空） |
| roleId    | Long   | 否  | 角色 ID（传入则替换现有角色）          |

#### 响应示例

```json
{
  "data": {
    "id": 2,
    "username": "test",
    "nickname": "新昵称",
    "email": "test@example.com",
    "avatarUrl": "https://example.com/new-avatar.png",
    "status": 1,
    "createTime": "2026-01-01T00:00:00",
    "updateTime": "2026-01-01T00:00:00"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

---

### 启用/禁用用户

禁用用户后：该用户将无法登录，并会被强制下线。

- **请求方法**: `PUT`
- **请求路径**: `/api/user/{id}/status`
- **需要权限**: `system:user:edit`

#### 请求参数

```json
{
  "status": 0
}
```

| 字段     | 类型      | 必填 | 说明           |
|--------|---------|----|--------------|
| status | Integer | 是  | 状态：0=禁用，1=启用 |

#### 成功响应

```json
{
  "data": "更新成功",
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 超级管理员不可禁用响应

```json
{
  "data": null,
  "success": false,
  "errorMsg": "超级管理员账号不可禁用",
  "code": 403
}
```

---

### 删除用户（逻辑删除）

逻辑删除用户：将 `is_deleted` 置为 1，并同时禁用账号（`status=0`），且会释放用户名/邮箱唯一约束以便后续复用。
删除后用户无法登录，并会被强制下线。

- **请求方法**: `DELETE`
- **请求路径**: `/api/user/{id}`
- **需要权限**: `system:user:delete`

#### 成功响应

```json
{
  "data": "删除成功",
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 超级管理员不可删除响应

```json
{
  "data": null,
  "success": false,
  "errorMsg": "超级管理员账号不可删除",
  "code": 403
}
```

---

## 权限码对照表（用户管理）

| 权限码                    | 说明        |
|------------------------|-----------|
| system:user:list       | 查看用户列表/详情 |
| system:user:edit       | 编辑用户信息/状态 |
| system:user:delete     | 删除用户      |
| system:user:assignRole | 查看用户角色    |

