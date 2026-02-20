## SEO配置管理接口文档

---

### 接口鉴权说明

所有SEO配置接口均需要登录，且需在请求 header 中携带 token，格式为：`{tokenName: tokenValue}`（tokenName 默认为 token，可在 application.properties 中修改）。

系统内置的SEO配置（`isSystem=true`）可以编辑，但不可删除。非系统内置的可进行编辑和删除操作。

**页面类型说明**：
- 页面类型（`page_type`）为**类型名称**，不做枚举校验。
- 系统存在默认数据，但**筛选项中的页面类型直接来自数据库**：取表中未删除配置的 `page_type` 去重后作为筛选项（类型名称不重复）。
- **用户可自定义**：新增 SEO 时可从筛选项选择已有类型，也可**直接传入新的类型名称**（如 `home`、`文章页`、`关于我` 等），最大 50 字符。

**唯一性约束**：同一页面类型（`page_type`）和页面ID（`page_id`）的组合必须唯一。对于固定页面，`page_id` 可为 `null`。

---

## SEO配置管理接口

基础路径：`/api/seo`

### 分页获取SEO列表

获取SEO配置列表，支持分页、关键词搜索与页面类型/是否系统内置筛选。响应中附带可用的筛选项（`filterOptions`），供前端渲染筛选控件。

- **请求方法**: `POST`
- **请求路径**: `/api/seo/list`
- **需要权限**: `seo:list`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "首页",
  "pageType": "home",
  "isSystem": 1
}
```

| 字段          | 类型      | 必填 | 说明                                             |
|-------------|---------|----|------------------------------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）                                   |
| pageSize    | Integer | 是  | 每页数量                                           |
| keyword     | String  | 否  | 搜索关键词（匹配 page_type、title、keywords、description） |
| pageType    | String  | 否  | 页面类型筛选（与列表中某条记录的 pageType 一致即可）                |
| isSystem    | Integer | 否  | 是否系统内置：0=否，1=是                                 |

#### 响应示例

```json
{
    "data": {
        "records": [
            {
                "id": 1,
                "pageType": "home",
                "pageId": null,
                "title": "我的博客 - 记录技术与生活的点滴",
                "keywords": "博客,技术博客,个人博客,技术分享,编程,开发",
                "description": "我的个人博客，分享技术心得、生活感悟和编程经验",
                "ogTitle": "我的博客 - 记录技术与生活的点滴",
                "ogDescription": "我的个人博客，分享技术心得、生活感悟和编程经验",
                "ogImage": null,
                "ogType": "website",
                "canonicalUrl": null,
                "robots": "index,follow",
                "isSystem": true,
                "createTime": "2026-02-21T00:00:00",
                "updateTime": "2026-02-21T00:00:00"
            }
        ],
        "total": 1,
        "size": 10,
        "current": 1,
        "pages": 1,
        "filterOptions": {
            "pageType": [
                { "value": "home", "label": "home" },
                { "value": "about", "label": "about" }
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

### 根据 ID 获取SEO详情

根据SEO配置 ID 获取详细信息。

- **请求方法**: `GET`
- **请求路径**: `/api/seo/{id}`
- **需要权限**: `seo:list`

#### 路径参数

| 参数 | 类型   | 说明       |
|----|------|----------|
| id | Long | SEO配置 ID |

#### 响应示例

```json
{
    "data": {
        "id": 1,
        "pageType": "home",
        "pageId": null,
        "title": "我的博客 - 记录技术与生活的点滴",
        "keywords": "博客,技术博客,个人博客,技术分享,编程,开发",
        "description": "我的个人博客，分享技术心得、生活感悟和编程经验",
        "ogTitle": "我的博客 - 记录技术与生活的点滴",
        "ogDescription": "我的个人博客，分享技术心得、生活感悟和编程经验",
        "ogImage": null,
        "ogType": "website",
        "canonicalUrl": null,
        "robots": "index,follow",
        "isSystem": true,
        "createTime": "2026-02-21T00:00:00",
        "updateTime": "2026-02-21T00:00:00"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### SEO配置不存在响应

```json
{
    "data": null,
    "success": false,
    "errorMsg": "SEO配置不存在",
    "code": 404
}
```

---

### 创建SEO配置

创建新的SEO配置。页面类型和页面ID的组合必须唯一。

- **请求方法**: `POST`
- **请求路径**: `/api/seo`
- **需要权限**: `seo:create`

#### 请求参数

```json
{
  "pageType": "article",
  "pageId": 1,
  "title": "文章标题 - SEO优化",
  "keywords": "文章,技术,编程",
  "description": "这是一篇关于技术的文章",
  "ogTitle": "文章标题 - SEO优化",
  "ogDescription": "这是一篇关于技术的文章",
  "ogImage": "https://example.com/image.jpg",
  "ogType": "article",
  "canonicalUrl": "https://example.com/article/1",
  "robots": "index,follow"
}
```

| 字段            | 类型     | 必填 | 说明                                     |
|---------------|--------|----|----------------------------------------|
| pageType      | String | 是  | 页面类型名称（可从筛选项选择已有类型或自定义输入，最大50字符）       |
| pageId        | Long   | 否  | 关联页面ID（文章ID、分类ID等，首页等固定页面可为空）          |
| title         | String | 否  | SEO标题（最大200字符）                         |
| keywords      | String | 否  | SEO关键词（最大500字符，逗号分隔）                   |
| description   | String | 否  | SEO描述（最大1500字符）                        |
| ogTitle       | String | 否  | Open Graph标题（最大200字符）                  |
| ogDescription | String | 否  | Open Graph描述（最大1500字符）                 |
| ogImage       | String | 否  | Open Graph图片URL（最大500字符）               |
| ogType        | String | 否  | Open Graph类型（最大50字符，默认：website）        |
| canonicalUrl  | String | 否  | 规范URL（最大500字符）                         |
| robots        | String | 否  | robots meta标签（最大100字符，默认：index,follow） |

#### 响应示例

```json
{
    "data": {
        "id": 2,
        "pageType": "article",
        "pageId": 1,
        "title": "文章标题 - SEO优化",
        "keywords": "文章,技术,编程",
        "description": "这是一篇关于技术的文章",
        "ogTitle": "文章标题 - SEO优化",
        "ogDescription": "这是一篇关于技术的文章",
        "ogImage": "https://example.com/image.jpg",
        "ogType": "article",
        "canonicalUrl": "https://example.com/article/1",
        "robots": "index,follow",
        "isSystem": false,
        "createTime": "2026-02-21T10:00:00",
        "updateTime": "2026-02-21T10:00:00"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

**该页面类型的SEO配置已存在**

```json
{
    "data": null,
    "success": false,
    "errorMsg": "该页面类型的SEO配置已存在",
    "code": 400
}
```

---

### 修改SEO配置

修改SEO配置信息。系统内置的SEO配置也可以编辑。

- **请求方法**: `PUT`
- **请求路径**: `/api/seo/{id}`
- **需要权限**: `seo:edit`

#### 路径参数

| 参数 | 类型   | 说明       |
|----|------|----------|
| id | Long | SEO配置 ID |

#### 请求参数

```json
{
  "pageId": 1,
  "title": "更新后的文章标题 - SEO优化",
  "keywords": "文章,技术,编程,更新",
  "description": "这是一篇更新后的关于技术的文章",
  "ogTitle": "更新后的文章标题 - SEO优化",
  "ogDescription": "这是一篇更新后的关于技术的文章",
  "ogImage": "https://example.com/new-image.jpg",
  "ogType": "article",
  "canonicalUrl": "https://example.com/article/1",
  "robots": "index,follow"
}
```

| 字段            | 类型     | 必填 | 说明                            |
|---------------|--------|----|-------------------------------|
| pageId        | Long   | 否  | 关联页面ID（文章ID、分类ID等，首页等固定页面可为空） |
| title         | String | 否  | SEO标题（最大200字符）                |
| keywords      | String | 否  | SEO关键词（最大500字符，逗号分隔）          |
| description   | String | 否  | SEO描述（最大1500字符）               |
| ogTitle       | String | 否  | Open Graph标题（最大200字符）         |
| ogDescription | String | 否  | Open Graph描述（最大1500字符）        |
| ogImage       | String | 否  | Open Graph图片URL（最大500字符）      |
| ogType        | String | 否  | Open Graph类型（最大50字符）          |
| canonicalUrl  | String | 否  | 规范URL（最大500字符）                |
| robots        | String | 否  | robots meta标签（最大100字符）        |

#### 响应示例

```json
{
    "data": {
        "id": 2,
        "pageType": "article",
        "pageId": 1,
        "title": "更新后的文章标题 - SEO优化",
        "keywords": "文章,技术,编程,更新",
        "description": "这是一篇更新后的关于技术的文章",
        "ogTitle": "更新后的文章标题 - SEO优化",
        "ogDescription": "这是一篇更新后的关于技术的文章",
        "ogImage": "https://example.com/new-image.jpg",
        "ogType": "article",
        "canonicalUrl": "https://example.com/article/1",
        "robots": "index,follow",
        "isSystem": false,
        "createTime": "2026-02-21T10:00:00",
        "updateTime": "2026-02-21T11:00:00"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

**SEO配置不存在**

```json
{
    "data": null,
    "success": false,
    "errorMsg": "SEO配置不存在",
    "code": 404
}
```

**该页面类型的SEO配置已存在**（修改pageId时，如果新组合已存在）

```json
{
    "data": null,
    "success": false,
    "errorMsg": "该页面类型的SEO配置已存在",
    "code": 400
}
```

---

### 删除SEO配置

删除SEO配置。系统内置的SEO配置不可删除。

- **请求方法**: `DELETE`
- **请求路径**: `/api/seo/{id}`
- **需要权限**: `seo:delete`

#### 路径参数

| 参数 | 类型   | 说明       |
|----|------|----------|
| id | Long | SEO配置 ID |

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

**SEO配置不存在**

```json
{
    "data": null,
    "success": false,
    "errorMsg": "SEO配置不存在",
    "code": 404
}
```

**系统内置SEO配置不可删除**

```json
{
    "data": null,
    "success": false,
    "errorMsg": "系统内置SEO配置不可删除",
    "code": 403
}
```

---

## 权限说明

SEO配置管理功能需要以下权限：

- `seo:list` - 查看SEO配置列表和详情
- `seo:create` - 创建SEO配置
- `seo:edit` - 编辑SEO配置（系统内置的也可以编辑）
- `seo:delete` - 删除SEO配置（系统内置的不可删除）

---

## 注意事项

1. **唯一性约束**：同一页面类型（`page_type`）和页面ID（`page_id`）的组合必须唯一。创建或修改时如果违反此约束，将返回错误。

2. **系统内置配置**：系统内置的SEO配置（`isSystem=true`）可以编辑，但不可删除。这是为了保护系统默认配置不被误删。

3. **逻辑删除**：删除操作采用逻辑删除，不会物理删除数据，只是将 `isDeleted` 字段设置为 1。

4. **默认值**：
   - `ogType` 默认为 `"website"`
   - `robots` 默认为 `"index,follow"`
   - `isSystem` 默认为 `false`（创建时）

5. **页面类型**：不做枚举校验。筛选项中的 `pageType` 来自数据库中已有配置的去重结果（类型名称不重复）；新增时可选择已有类型或自定义输入新类型名称（最大50个字符）。
