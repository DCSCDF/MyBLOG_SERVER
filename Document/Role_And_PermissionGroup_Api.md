## 角色与权限组管理接口文档

---

### 接口鉴权说明

所有角色与权限组接口均需要登录，且需在请求 header 中携带 token，格式为：`{tokenName: tokenValue}`（tokenName 默认为 token，可在 application.properties 中修改）。

系统内置的角色和权限组（`isSystem=true`）仅支持查看，不可修改和删除。非系统内置的可进行修改和删除操作。

**Sa-Token 鉴权分配**：角色通过关联权限和权限组获得权限。为角色添加权限组时，权限组内的所有权限会自动同步到角色，供 Sa-Token 鉴权使用。删除角色或权限组时，会级联删除所有关联表数据。

**父子权限规则**：

- **角色修改**：为角色添加权限或权限组时，若新增权限与角色已有权限存在父子关系或重复（如 `system:user` 与 `system:user:list` 视为父子关系），则不允许添加。已有权限包括：直接分配的权限 + 已关联权限组中的全部权限。
- **权限组管理**：为权限组添加权限时，若待添加权限与权限组中已有权限存在父子关系，则不允许添加（父子权限互斥：关联了父权限就不能关联其子权限，反之亦然）。

**禁用的权限组**：禁用的权限组（`status=0`）无法被添加到角色。当权限组被**禁用**时，仅收回角色通过该组获得的权限（`sys_role_permission`），**保留**角色与权限组的关联（`sys_role_permission_group`）；**重新启用**权限组后，会按保留的关联自动恢复各角色通过该组获得的权限。当权限组被**删除**时，会移除关联及权限，不再保留。

**角色删除与禁用前提**：角色只有在**未被任何用户使用**时才能删除或禁用。若仍有用户关联该角色，将返回错误，需先解除用户与该角色的关联后再操作。

**权限组修改与依赖角色**：为权限组添加权限时，会校验所有依赖该权限组的角色——若角色通过直接分配或其他权限组已拥有该权限（或父子权限），则不允许添加。权限组添加/移除权限后，会同步更新依赖角色的 `sys_role_permission`。

---

## 角色管理接口

基础路径：`/api/role`

### 分页获取角色列表

获取系统角色列表，支持分页、关键词搜索与状态/是否内置筛选。响应中附带可用的筛选项（`filterOptions`），供前端渲染筛选控件。

- **请求方法**: `POST`
- **请求路径**: `/api/role/list`
- **需要权限**: `system:role:list`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "ADMIN",
  "status": 1,
  "isSystem": 1
}
```

| 字段          | 类型      | 必填 | 说明                                |
|-------------|---------|----|-----------------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）                     |
| pageSize    | Integer | 是  | 每页数量                             |
| keyword     | String  | 否  | 搜索关键词（匹配 code、name、description）     |
| status      | Integer | 否  | 状态筛选：0=禁用，1=启用                    |
| isSystem    | Integer | 否  | 是否系统内置：0=否，1=是                   |

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
        "pages": 1,
        "filterOptions": {
            "status": [
                { "value": 0, "label": "禁用" },
                { "value": 1, "label": "启用" }
            ],
            "isSystem": [
                { "value": 0, "label": "否" },
                { "value": 1, "label": "是" }
            ]
        }
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

### 创建角色

创建新的角色。角色编码必须唯一，且只能包含大写字母和下划线。

- **请求方法**: `POST`
- **请求路径**: `/api/role`
- **需要权限**: `system:role:create`

#### 请求参数

```json
{
  "code": "CUSTOM_ROLE",
  "name": "自定义角色",
  "description": "角色描述（可选）",
  "sortOrder": 50,
  "status": 1
}
```

| 字段          | 类型      | 必填 | 说明                       |
|-------------|---------|----|--------------------------|
| code        | String  | 是  | 角色编码，2-50字符，只能包含大写字母和下划线 |
| name        | String  | 是  | 角色名称，最大50字符              |
| description | String  | 否  | 角色描述，最大200字符             |
| sortOrder   | Integer | 否  | 排序顺序，数字越大越靠前，默认0         |
| status      | Integer | 否  | 状态：0=禁用，1=启用，默认1         |

#### 成功响应

```json
{
    "data": {
        "id": 5,
        "code": "CUSTOM_ROLE",
        "name": "自定义角色",
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

#### 错误响应

- 角色编码已存在：`code: 400`
- 角色编码格式错误：`code: 400`

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

#### 角色正在被用户使用无法禁用响应

当角色仍被至少一个用户使用时，不可将状态改为禁用。

```json
{
    "data": null,
    "success": false,
    "errorMsg": "该角色正在被用户使用，无法禁用。请先解除用户与该角色的关联",
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

#### 角色正在被用户使用无法删除响应

当角色仍被至少一个用户使用时，不可删除。需先解除所有用户与该角色的关联后再删除。

```json
{
    "data": null,
    "success": false,
    "errorMsg": "该角色正在被用户使用，无法删除。请先解除用户与该角色的关联",
    "code": 403
}
```

---

### 获取角色关联的权限和权限组列表

获取指定角色关联的权限列表和权限组列表，用于展示角色的完整权限配置。仅返回**未删除且已启用**（`status=1`）的权限组；已禁用或已删除的权限组不会出现在列表中（禁用/删除时已同步移除角色与权限组的关联）。

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
- 该权限与角色已有权限存在父子关系或重复（父子权限去重：若新增权限与已有权限编码相同，或存在父子层级关系如 `system:user` 与 `system:user:list`，则不允许添加）：`code: 400`
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

#### 错误响应

- 该权限组已分配给角色：`code: 400`
- 禁用的权限组无法添加到角色：`code: 400`
- 该权限组中的权限与角色已有权限存在父子关系或重复（父子权限去重：若权限组中任意权限与角色已有权限编码相同，或存在父子层级关系，则不允许添加）：`code: 400`
- 系统内置角色不可修改：`code: 403`
- 角色或权限组不存在：`code: 404`

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

获取系统权限组列表，支持分页、关键词搜索与状态/是否内置筛选。响应中附带可用的筛选项（`filterOptions`），供前端渲染筛选控件。

- **请求方法**: `POST`
- **请求路径**: `/api/permission-group/list`
- **需要权限**: `system:permission_group:list`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "系统",
  "status": 1,
  "isSystem": 1
}
```

| 字段          | 类型      | 必填 | 说明                            |
|-------------|---------|----|-------------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）                 |
| pageSize    | Integer | 是  | 每页数量                         |
| keyword     | String  | 否  | 搜索关键词（匹配 name、description）   |
| status      | Integer | 否  | 状态筛选：0=禁用，1=启用                |
| isSystem    | Integer | 否  | 是否系统内置：0=否，1=是               |

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
        "pages": 1,
        "filterOptions": {
            "status": [
                { "value": 0, "label": "禁用" },
                { "value": 1, "label": "启用" }
            ],
            "isSystem": [
                { "value": 0, "label": "否" },
                { "value": 1, "label": "是" }
            ]
        }
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

### 创建权限组

创建新的权限组。权限组名称必须唯一（在未删除的权限组中）。

- **请求方法**: `POST`
- **请求路径**: `/api/permission-group`
- **需要权限**: `system:permission_group:create`

#### 请求参数

```json
{
  "name": "自定义权限组",
  "description": "权限组描述（可选）",
  "sortOrder": 50,
  "status": 1
}
```

| 字段          | 类型      | 必填 | 说明               |
|-------------|---------|----|------------------|
| name        | String  | 是  | 权限组名称，最大50字符     |
| description | String  | 否  | 权限组描述，最大200字符    |
| sortOrder   | Integer | 否  | 排序顺序，数字越大越靠前，默认0 |
| status      | Integer | 否  | 状态：0=禁用，1=启用，默认1 |

#### 成功响应

```json
{
    "data": {
        "id": 4,
        "name": "自定义权限组",
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

#### 错误响应

- 权限组名称已存在：`code: 400`

---

### 修改权限组

修改权限组信息。系统内置权限组（`isSystem=true`）不可修改。将权限组**禁用**（`status=0`）时，仅收回角色通过该组获得的权限，保留角色与权限组的关联；将权限组**重新启用**（`status=1`）时，会按保留的关联自动恢复各角色通过该组获得的权限。

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

逻辑删除权限组。系统内置权限组不可删除。**删除权限组时**：会先同步移除所有角色与该权限组的关联及通过该组获得的权限，再级联删除权限-权限组关联（sys_permission_group_item）、角色-权限组关联（sys_role_permission_group），然后对权限组执行逻辑删除。重新启用（若有恢复逻辑）或新建同名权限组后，需重新为角色添加才会恢复关联。

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
- 该权限与权限组中已有权限存在父子关系（父子权限互斥：关联了父权限就不能关联其子权限，关联了子权限就不能关联其父权限。如 `system:user` 与 `system:user:list` 不能同时存在）：`code: 400`
- 该权限与依赖此权限组的角色已有权限重叠（角色通过直接分配或其他权限组已拥有此权限或父子权限）：`code: 400`
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
| system:role:create                | 创建角色           |
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
| system:permission_group:create           | 创建权限组           |
| system:permission_group:edit             | 编辑权限组           |
| system:permission_group:delete           | 删除权限组（级联删除关联）   |
| system:permission_group:addPermission    | 为权限组添加权限        |
| system:permission_group:removePermission | 从权限组移除权限        |
