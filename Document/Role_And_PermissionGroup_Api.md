## 角色与权限组管理接口文档

---

### 接口鉴权说明

所有角色与权限组接口均需要登录，且需在请求 header 中携带 token，格式为：`{tokenName: tokenValue}`（tokenName 默认为 token，可在 application.properties 中修改）。

系统内置的角色和权限组（`isSystem=true`）仅支持查看，不可修改和删除。非系统内置的可进行修改和删除操作。

**Sa-Token 鉴权分配**：角色通过关联权限和权限组获得权限。为角色添加权限组时，权限组内的所有权限会自动同步到角色，供 Sa-Token 鉴权使用。删除角色或权限组时，会级联删除所有关联表数据。

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

| 参数 | 类型   | 说明    |
|----|------|-------|
| id | Long | 角色 ID |

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

| 参数 | 类型   | 说明    |
|----|------|-------|
| id | Long | 角色 ID |

#### 请求参数

```json
{
  "name": "自定义角色名称",
  "description": "角色描述（可选）",
  "sortOrder": 50,
  "status": 1
}
```

| 字段          | 类型      | 必填 | 说明             |
|-------------|---------|----|----------------|
| name        | String  | 是  | 角色名称，最大 50 字符  |
| description | String  | 否  | 角色描述，最大 200 字符 |
| sortOrder   | Integer | 否  | 排序顺序，数字越大越靠前   |
| status      | Integer | 否  | 状态：0=禁用，1=启用   |

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

逻辑删除角色。系统内置角色不可删除。**删除角色的同时会级联删除**：用户-角色关联（sys_user_role）、角色-权限关联（sys_role_permission）、角色-权限组关联（sys_role_permission_group），然后对角色执行逻辑删除。

- **请求方法**: `DELETE`
- **请求路径**: `/api/role/{id}`
- **需要权限**: `system:role:delete`

#### 路径参数

| 参数 | 类型   | 说明    |
|----|------|-------|
| id | Long | 角色 ID |

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

#### 默认注册角色不可删除响应

若该角色已通过系统配置 `user_register_default_role` 设为用户注册时的默认角色，则不可删除。需先在系统配置中修改该配置项。

```json
{
    "data": null,
    "success": false,
    "errorMsg": "该角色已设为用户注册默认角色，不可删除。请先在系统配置中修改 user_register_default_role",
    "code": 403
}
```

---

### 获取角色关联的权限和权限组列表

获取指定角色关联的权限列表和权限组列表，用于展示角色的完整权限配置。

- **请求方法**: `GET`
- **请求路径**: `/api/role/{id}/permissions-detail`
- **需要权限**: `system:role:list`

#### 路径参数

| 参数 | 类型   | 说明    |
|----|------|-------|
| id | Long | 角色 ID |

#### 响应示例

```json
{
    "data": {
        "role": {
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
        },
        "permissions": [
            {
                "id": 1,
                "code": "system:user:list",
                "name": "用户列表",
                "description": "查看用户列表",
                "sortOrder": 1,
                "createTime": "2026-01-01T00:00:00"
            }
        ],
        "permissionGroups": [
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
        ]
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

---

### 为角色添加权限

为角色添加单个权限。仅非系统内置角色可操作。

- **请求方法**: `POST`
- **请求路径**: `/api/role/{id}/permissions`
- **需要权限**: `system:role:addPermission`

#### 路径参数

| 参数 | 类型   | 说明    |
|----|------|-------|
| id | Long | 角色 ID |

#### 请求参数

```json
{
  "permissionId": 5
}
```

| 字段           | 类型   | 必填 | 说明    |
|--------------|------|----|-------|
| permissionId | Long | 是  | 权限 ID |

#### 成功响应

```json
{
    "data": "添加成功",
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

- 该权限已分配给角色：`code: 400`
- 系统内置角色不可修改：`code: 403`
- 角色或权限不存在：`code: 404`

---

### 从角色移除权限

从角色移除单个权限。仅非系统内置角色可操作。

- **请求方法**: `DELETE`
- **请求路径**: `/api/role/{id}/permissions/{permissionId}`
- **需要权限**: `system:role:removePermission`

#### 路径参数

| 参数           | 类型   | 说明    |
|--------------|------|-------|
| id           | Long | 角色 ID |
| permissionId | Long | 权限 ID |

#### 成功响应

```json
{
    "data": "移除成功",
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

- 该权限未分配给角色：`code: 400`
- 系统内置角色不可修改：`code: 403`

---

### 为角色添加权限组

为角色添加权限组。添加后，权限组内的所有权限会自动同步到角色的权限列表中，供 Sa-Token 鉴权使用。仅非系统内置角色可操作。

- **请求方法**: `POST`
- **请求路径**: `/api/role/{id}/permission-groups`
- **需要权限**: `system:role:addPermissionGroup`

#### 路径参数

| 参数 | 类型   | 说明    |
|----|------|-------|
| id | Long | 角色 ID |

#### 请求参数

```json
{
  "groupId": 2
}
```

| 字段      | 类型   | 必填 | 说明     |
|---------|------|----|--------|
| groupId | Long | 是  | 权限组 ID |

#### 成功响应

```json
{
    "data": "添加成功",
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

---

### 从角色移除权限组

从角色移除权限组。移除时会同时从角色权限表中删除该权限组包含的所有权限。仅非系统内置角色可操作。

- **请求方法**: `DELETE`
- **请求路径**: `/api/role/{id}/permission-groups/{groupId}`
- **需要权限**: `system:role:removePermissionGroup`

#### 路径参数

| 参数      | 类型   | 说明     |
|---------|------|--------|
| id      | Long | 角色 ID  |
| groupId | Long | 权限组 ID |

#### 成功响应

```json
{
    "data": "移除成功",
    "success": true,
    "errorMsg": null,
    "code": 200
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

| 参数 | 类型   | 说明     |
|----|------|--------|
| id | Long | 权限组 ID |

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

| 参数 | 类型   | 说明     |
|----|------|--------|
| id | Long | 权限组 ID |

#### 请求参数

```json
{
  "name": "自定义权限组名称",
  "description": "权限组描述（可选）",
  "sortOrder": 50,
  "status": 1
}
```

| 字段          | 类型      | 必填 | 说明              |
|-------------|---------|----|-----------------|
| name        | String  | 是  | 权限组名称，最大 50 字符  |
| description | String  | 否  | 权限组描述，最大 200 字符 |
| sortOrder   | Integer | 否  | 排序顺序，数字越大越靠前    |
| status      | Integer | 否  | 状态：0=禁用，1=启用    |

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

逻辑删除权限组。系统内置权限组不可删除。**删除权限组的同时会级联删除**：权限-权限组关联（sys_permission_group_item）、角色-权限组关联（sys_role_permission_group），然后对权限组执行逻辑删除。

- **请求方法**: `DELETE`
- **请求路径**: `/api/permission-group/{id}`
- **需要权限**: `system:permission_group:delete`

#### 路径参数

| 参数 | 类型   | 说明     |
|----|------|--------|
| id | Long | 权限组 ID |

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

### 获取权限组关联的权限列表

获取指定权限组关联的权限列表。系统内置和非内置权限组均可查看。

- **请求方法**: `GET`
- **请求路径**: `/api/permission-group/{id}/permissions`
- **需要权限**: `system:permission_group:list`

#### 路径参数

| 参数 | 类型   | 说明     |
|----|------|--------|
| id | Long | 权限组 ID |

#### 响应示例

```json
{
    "data": [
        {
            "id": 1,
            "code": "system:user:list",
            "name": "用户列表",
            "description": "查看用户列表",
            "sortOrder": 1,
            "createTime": "2026-01-01T00:00:00"
        }
    ],
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

---

### 为权限组添加权限

为权限组添加单个权限。仅非系统内置权限组可操作。

- **请求方法**: `POST`
- **请求路径**: `/api/permission-group/{id}/permissions`
- **需要权限**: `system:permission_group:addPermission`

#### 路径参数

| 参数 | 类型   | 说明     |
|----|------|--------|
| id | Long | 权限组 ID |

#### 请求参数

```json
{
  "permissionId": 5
}
```

| 字段           | 类型   | 必填 | 说明    |
|--------------|------|----|-------|
| permissionId | Long | 是  | 权限 ID |

#### 成功响应

```json
{
    "data": "添加成功",
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

- 该权限已在权限组中：`code: 400`
- 系统内置权限组不可修改：`code: 403`
- 权限组或权限不存在：`code: 404`

---

### 从权限组移除权限

从权限组移除单个权限。仅非系统内置权限组可操作。

- **请求方法**: `DELETE`
- **请求路径**: `/api/permission-group/{id}/permissions/{permissionId}`
- **需要权限**: `system:permission_group:removePermission`

#### 路径参数

| 参数           | 类型   | 说明     |
|--------------|------|--------|
| id           | Long | 权限组 ID |
| permissionId | Long | 权限 ID  |

#### 成功响应

```json
{
    "data": "移除成功",
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

- 该权限不在权限组中：`code: 400`
- 系统内置权限组不可修改：`code: 403`

---

## 权限码对照表

### 角色管理

| 权限码                               | 说明             |
|-----------------------------------|----------------|
| system:role:list                  | 查看角色列表、详情、权限详情 |
| system:role:edit                  | 编辑角色           |
| system:role:delete                | 删除角色（级联删除关联）   |
| system:role:addPermission         | 为角色添加权限        |
| system:role:removePermission      | 从角色移除权限        |
| system:role:addPermissionGroup    | 为角色添加权限组       |
| system:role:removePermissionGroup | 从角色移除权限组       |

### 权限组管理

| 权限码                                      | 说明              |
|------------------------------------------|-----------------|
| system:permission_group:list             | 查看权限组列表、详情、关联权限 |
| system:permission_group:edit             | 编辑权限组           |
| system:permission_group:delete           | 删除权限组（级联删除关联）   |
| system:permission_group:addPermission    | 为权限组添加权限        |
| system:permission_group:removePermission | 从权限组移除权限        |
