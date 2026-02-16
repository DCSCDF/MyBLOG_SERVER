## 角色与权限组管理接口文档

---

### 接口鉴权说明

所有角色与权限组接口均需要登录，且需在请求 header 中携带 token，格式为：`{tokenName: tokenValue}`（tokenName 默认为 token，可在 application.properties 中修改）。

系统内置的角色和权限组（`isSystem=true`）仅支持查看，不可修改和删除。非系统内置的可进行修改和删除操作。

---

## 角色管理接口

基础路径：`/api/role`

### 分页获取角色列表

获取系统角色列表，支持分页查询。系统内置角色和自定义角色均会返回。

- **请求方法**: `POST`
- **请求路径**: `/api/role/list`
- **需要权限**: `system:role:list`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10
}
```

#### 响应示例

```json
{
    "data": {
        "records": [
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
        ],
        "total": 4,
        "size": 10,
        "current": 1,
        "pages": 1
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 权限不足响应

```json
{
    "data": null,
    "success": false,
    "errorMsg": "没有权限",
    "code": 403
}
```

---

### 根据 ID 获取角色详情

根据角色 ID 获取角色详细信息。

- **请求方法**: `GET`
- **请求路径**: `/api/role/{id}`
- **需要权限**: `system:role:list`

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id   | Long | 角色 ID |

#### 响应示例

```json
{
    "data": {
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
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 角色不存在响应

```json
{
    "data": null,
    "success": false,
    "errorMsg": "角色不存在",
    "code": 404
}
```

---

### 修改角色

修改角色信息。系统内置角色（`isSystem=true`）不可修改。

- **请求方法**: `PUT`
- **请求路径**: `/api/role/{id}`
- **需要权限**: `system:role:edit`

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id   | Long | 角色 ID |

#### 请求参数

```json
{
  "name": "自定义角色名称",
  "description": "角色描述（可选）",
  "sortOrder": 50,
  "status": 1
}
```

| 字段       | 类型    | 必填 | 说明                              |
|------------|---------|------|-----------------------------------|
| name       | String  | 是   | 角色名称，最大 50 字符            |
| description| String  | 否   | 角色描述，最大 200 字符           |
| sortOrder  | Integer | 否   | 排序顺序，数字越大越靠前          |
| status     | Integer | 否   | 状态：0=禁用，1=启用              |

#### 成功响应

```json
{
    "data": {
        "id": 5,
        "code": "CUSTOM_ROLE",
        "name": "自定义角色名称",
        "description": "角色描述",
        "superAdmin": false,
        "isSystem": false,
        "sortOrder": 50,
        "status": 1,
        "createTime": "2026-01-01T00:00:00",
        "updateTime": "2026-01-01T00:00:00"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 系统内置角色不可修改响应

```json
{
    "data": null,
    "success": false,
    "errorMsg": "系统内置角色不可修改",
    "code": 403
}
```

---

### 删除角色

逻辑删除角色。系统内置角色不可删除。删除后角色在列表中不再显示，但数据库中保留记录。

- **请求方法**: `DELETE`
- **请求路径**: `/api/role/{id}`
- **需要权限**: `system:role:delete`

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id   | Long | 角色 ID |

#### 成功响应

```json
{
    "data": "删除成功",
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 系统内置角色不可删除响应

```json
{
    "data": null,
    "success": false,
    "errorMsg": "系统内置角色不可删除",
    "code": 403
}
```

---

## 权限组管理接口

基础路径：`/api/permission-group`

### 分页获取权限组列表

获取系统权限组列表，支持分页查询。权限组用于将多个权限归类管理，便于为角色批量分配权限。

- **请求方法**: `POST`
- **请求路径**: `/api/permission-group/list`
- **需要权限**: `system:permission_group:list`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10
}
```

#### 响应示例

```json
{
    "data": {
        "records": [
            {
                "id": 1,
                "name": "系统管理组",
                "description": "包含所有系统管理权限",
                "sortOrder": 100,
                "status": 1,
                "isSystem": true,
                "createTime": "2026-01-01T00:00:00",
                "updateTime": "2026-01-01T00:00:00"
            }
        ],
        "total": 3,
        "size": 10,
        "current": 1,
        "pages": 1
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

---

### 根据 ID 获取权限组详情

根据权限组 ID 获取权限组详细信息。

- **请求方法**: `GET`
- **请求路径**: `/api/permission-group/{id}`
- **需要权限**: `system:permission_group:list`

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id   | Long | 权限组 ID |

#### 响应示例

```json
{
    "data": {
        "id": 1,
        "name": "系统管理组",
        "description": "包含所有系统管理权限",
        "sortOrder": 100,
        "status": 1,
        "isSystem": true,
        "createTime": "2026-01-01T00:00:00",
        "updateTime": "2026-01-01T00:00:00"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

---

### 修改权限组

修改权限组信息。系统内置权限组（`isSystem=true`）不可修改。

- **请求方法**: `PUT`
- **请求路径**: `/api/permission-group/{id}`
- **需要权限**: `system:permission_group:edit`

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id   | Long | 权限组 ID |

#### 请求参数

```json
{
  "name": "自定义权限组名称",
  "description": "权限组描述（可选）",
  "sortOrder": 50,
  "status": 1
}
```

| 字段       | 类型    | 必填 | 说明                              |
|------------|---------|------|-----------------------------------|
| name       | String  | 是   | 权限组名称，最大 50 字符          |
| description| String  | 否   | 权限组描述，最大 200 字符         |
| sortOrder  | Integer | 否   | 排序顺序，数字越大越靠前          |
| status     | Integer | 否   | 状态：0=禁用，1=启用              |

#### 成功响应

```json
{
    "data": {
        "id": 4,
        "name": "自定义权限组名称",
        "description": "权限组描述",
        "sortOrder": 50,
        "status": 1,
        "isSystem": false,
        "createTime": "2026-01-01T00:00:00",
        "updateTime": "2026-01-01T00:00:00"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 系统内置权限组不可修改响应

```json
{
    "data": null,
    "success": false,
    "errorMsg": "系统内置权限组不可修改",
    "code": 403
}
```

---

### 删除权限组

逻辑删除权限组。系统内置权限组不可删除。

- **请求方法**: `DELETE`
- **请求路径**: `/api/permission-group/{id}`
- **需要权限**: `system:permission_group:delete`

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id   | Long | 权限组 ID |

#### 成功响应

```json
{
    "data": "删除成功",
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 系统内置权限组不可删除响应

```json
{
    "data": null,
    "success": false,
    "errorMsg": "系统内置权限组不可删除",
    "code": 403
}
```

---

## 权限码对照表

| 权限码                         | 说明           |
|--------------------------------|----------------|
| system:role:list               | 查看角色列表   |
| system:role:edit               | 编辑角色       |
| system:role:delete             | 删除角色       |
| system:permission_group:list   | 查看权限组列表 |
| system:permission_group:edit   | 编辑权限组     |
| system:permission_group:delete | 删除权限组     |
