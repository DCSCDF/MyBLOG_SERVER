## 网站配置管理接口文档

---

### 接口鉴权说明

所有网站配置接口均需要登录，且需在请求 header 中携带 token，格式为：`{tokenName: tokenValue}`（tokenName 默认为 token，可在 application.properties 中修改）。

**配置分类说明**：

- **系统默认配置**（`is_system=1`）：由系统预置，如 `site.name`、`site.domain`、`site.description` 等。仅支持按配置键（key）查询与修改配置值（`config_value`），不可删除。
- **用户自定义配置**（`is_system=0`）：用户自行添加的配置项。支持新增、分页列表、修改配置值、逻辑删除。

**修改限制**：所有配置项仅允许修改 `config_value`，其他字段（如 `config_key`、`data_type`、`validation_rule`、`description`）不可通过本接口修改。

---

## 网站配置管理接口

基础路径：`/api/config`

### 系统默认配置项查询

按前端传入的配置键（如 `site.name`、`site.domain`、`site.description`）查询，仅返回系统内置的配置项。用于获取站点名称、域名、描述等展示或表单回显。

- **请求方法**: `POST`
- **请求路径**: `/api/config/system/list`
- **需要权限**: `system:config:system:list`

#### 请求参数

```json
{
  "keys": ["site.name", "site.domain", "site.description"]
}
```

| 字段   | 类型             | 必填 | 说明                                      |
|------|----------------|----|-----------------------------------------|
| keys | List\<String\> | 是  | 配置键数组，仅支持系统内置项（如 site.name、site.domain） |

#### 响应示例

```json
{
    "data": [
        {
            "configKey": "site.name",
            "configValue": "我的博客",
            "dataType": "string",
            "validationRule": "max_length=100",
            "description": "网站名称",
            "createTime": "2026-02-18T00:00:00",
            "updateTime": "2026-02-22T00:00:00"
        },
        {
            "configKey": "site.domain",
            "configValue": "localhost:8080",
            "dataType": "string",
            "validationRule": "max_length=100",
            "description": "网站域名",
            "createTime": "2026-02-18T00:00:00",
            "updateTime": "2026-02-22T00:00:00"
        }
    ],
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

说明：仅返回传入的 `keys` 中且为系统内置（`is_system=1`）且未删除的配置项；非系统内置或已删除的 key 不会出现在结果中。

#### 错误响应

**配置键列表不能为空**

```json
{
    "data": null,
    "success": false,
    "errorMsg": "配置键列表不能为空",
    "code": 400
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

### 用户自定义配置项分页列表

获取所有非系统内置的配置项列表，仅支持分页；支持按关键词搜索（匹配 `config_key`、`description`）。每条记录包含 `id`，用于删除接口。

- **请求方法**: `POST`
- **请求路径**: `/api/config/custom/list`
- **需要权限**: `system:config:custom:list`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "自定义"
}
```

| 字段          | 类型      | 必填 | 说明                               |
|-------------|---------|----|----------------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）                     |
| pageSize    | Integer | 是  | 每页数量                             |
| keyword     | String  | 否  | 搜索关键词（匹配 config_key、description） |

#### 响应示例

```json
{
    "data": {
        "records": [
            {
                "id": 10,
                "configKey": "custom.theme",
                "configValue": "dark",
                "dataType": "string",
                "validationRule": null,
                "description": "自定义主题",
                "createTime": "2026-02-22T10:00:00",
                "updateTime": "2026-02-22T10:00:00"
            }
        ],
        "total": 1,
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

### 创建用户自定义配置项

添加一条用户自定义配置项。`config_key` 全局唯一，不可与已有（未删除）配置键重复。创建后 `is_system=0`，可在此后通过「修改」与「删除」接口管理。

- **请求方法**: `POST`
- **请求路径**: `/api/config/custom`
- **需要权限**: `system:config:create`

#### 请求参数

```json
{
  "configKey": "custom.theme",
  "configValue": "dark",
  "dataType": "string",
  "validationRule": "max_length=50",
  "description": "自定义主题"
}
```

| 字段             | 类型     | 必填 | 说明                                                        |
|----------------|--------|----|-----------------------------------------------------------|
| configKey      | String | 是  | 配置键（全局唯一，不可与已有未删除项重复）                                     |
| configValue    | String | 是  | 配置值                                                       |
| dataType       | String | 否  | 数据类型：string、boolean、integer、json、email、url、text，默认 string |
| validationRule | String | 否  | 校验规则，如 max_length=100、regex=...                           |
| description    | String | 否  | 配置项说明，用于后台展示                                              |

#### 响应示例

```json
{
    "data": {
        "id": 10,
        "configKey": "custom.theme",
        "configValue": "dark",
        "dataType": "string",
        "validationRule": "max_length=50",
        "description": "自定义主题",
        "createTime": "2026-02-22T10:00:00",
        "updateTime": "2026-02-22T10:00:00"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

**配置键已存在**

```json
{
    "data": null,
    "success": false,
    "errorMsg": "配置键已存在",
    "code": 400
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

### 修改网站配置项

根据 `config_key` 修改该配置项的 `config_value`，其他字段（如 `data_type`、`validation_rule`、`description`）不可修改。系统默认配置与用户自定义配置均支持修改值。

- **请求方法**: `PUT`
- **请求路径**: `/api/config`
- **需要权限**: `system:config:edit`

#### 请求参数

```json
{
  "configKey": "site.name",
  "configValue": "我的技术博客"
}
```

| 字段          | 类型     | 必填 | 说明      |
|-------------|--------|----|---------|
| configKey   | String | 是  | 配置键（唯一） |
| configValue | String | 是  | 新的配置值   |

#### 响应示例

```json
{
    "data": {
        "configKey": "site.name",
        "configValue": "我的技术博客",
        "dataType": "string",
        "validationRule": "max_length=100",
        "description": "网站名称",
        "createTime": "2026-02-18T00:00:00",
        "updateTime": "2026-02-22T12:00:00"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

**配置项不存在**

```json
{
    "data": null,
    "success": false,
    "errorMsg": "配置项不存在",
    "code": 404
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

### 删除用户自定义配置项

按主键 `id` 逻辑删除非系统内置的配置项。系统内置配置（`is_system=1`）不可删除。`id` 来自「用户自定义配置项分页列表」接口返回的 `records[].id`。

- **请求方法**: `DELETE`
- **请求路径**: `/api/config/custom/{id}`
- **需要权限**: `system:config:delete`

#### 路径参数

| 参数 | 类型   | 说明       |
|----|------|----------|
| id | Long | 配置项主键 ID |

#### 响应示例

```json
{
    "data": "删除成功",
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

**配置项不存在**

```json
{
    "data": null,
    "success": false,
    "errorMsg": "配置项不存在",
    "code": 404
}
```

**系统内置配置项不可删除**

```json
{
    "data": null,
    "success": false,
    "errorMsg": "系统内置配置项不可删除",
    "code": 403
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

## 权限说明

网站配置管理功能需要以下权限：

- `system:config:system:list` - 按 key 查询系统默认配置项
- `system:config:custom:list` - 分页查询用户自定义配置项
- `system:config:create` - 创建用户自定义配置项
- `system:config:edit` - 修改配置项的值（仅 `config_value`）
- `system:config:delete` - 删除非系统内置的配置项

---

## 注意事项

1. **系统默认配置**：仅支持「按 key 查询」和「修改 config_value」，不可删除。系统内置项由 SQL 或初始化脚本插入（如 `site.name`、`site.domain`、`site.description`、`site.icp`、SMTP 相关等）。

2. **用户自定义配置**：支持新增、分页列表、修改值、逻辑删除。新增时 `config_key` 不可与已有（未删除）配置键重复。删除为逻辑删除，不会物理删除数据。

3. **修改范围**：所有修改接口仅更新 `config_value`，不修改 `config_key`、`data_type`、`validation_rule`、`description` 等字段。

4. **删除标识**：删除自定义配置时需使用列表接口返回的 `id`，确保仅删除非系统内置项；若误传系统内置配置的 id，将返回「系统内置配置项不可删除」。
