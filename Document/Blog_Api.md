## 文章管理接口文档

---

### 接口鉴权说明

所有文章接口均需要登录，且需在请求 header 中携带 token，格式为：`{tokenName: tokenValue}`（tokenName 默认为 token，可在 application.properties 中修改）。

文章管理使用以下权限码：

- `article:list` - 查看文章列表
- `article:create` - 创建文章
- `article:edit` - 编辑文章
- `article:delete` - 删除文章
- `article:publish` - 发布文章

文章数据存储在 `sys_blog` 表中，字段包括：文章标题、摘要、MD内容、HTML内容、封面图、标签、作者ID、浏览量、评论数、点赞数、是否隐藏、是否置顶、是否推荐等。

---

## 文章管理接口

基础路径：`/api/blogs`

---

### 1. 创建文章

创建一篇新的文章。**作者ID会根据当前登录用户的token自动获取并插入**。

- **请求方法**: `POST`
- **请求路径**: `/api/blogs`
- **需要权限**: `article:create`

#### 请求参数

```json
{
  "title": "Java并发编程实战",
  "categoryId": 1,
  "summary": "本文介绍了Java并发编程的核心知识点",
  "content": "# Java并发编程\n\n## 什么是并发...",
  "coverImage": "https://example.com/images/java-concurrency.jpg",
  "tags": "Java,并发,多线程"
}
```

| 字段          | 类型     | 必填 | 说明                          |
|-------------|--------|----|-----------------------------|
| title       | String | 是  | 文章标题，最大200字符                |
| categoryId  | Long   | 否  | 分类ID，关联 `sys_category` 表    |
| summary     | String | 否  | 文章摘要，最大200字符                |
| content     | String | 否  | MD格式的文章内容                   |
| coverImage  | String | 否  | 封面图片URL，必须为有效的http/https链接  |
| tags        | String | 否  | 标签，多个标签用逗号分隔，如：`前端,后端,Java` |

#### 响应示例

```json
{
  "data": {
    "id": 1,
    "message": "文章创建成功"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（标题为空）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "文章标题不能为空",
  "code": 400
}
```

#### 错误响应示例（摘要超长）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "文章摘要不能超过200字符",
  "code": 400
}
```

#### 错误响应示例（封面图URL格式无效）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "封面图片URL格式无效，请输入有效的http/https链接",
  "code": 400
}
```

---

## 注意事项

1. **作者ID获取**：创建文章时，后台会根据当前登录用户的token自动获取用户ID，并插入到 `author_id` 字段中，无需前端传入。
2. **URL校验**：封面图片URL如果不为空，会进行http/https格式校验，无效格式会返回400错误。
3. **标签格式**：多个标签使用逗号分隔存储，如 `"前端,后端,Java"`，前端展示时需要自行拆分。
4. **逻辑删除**：文章采用逻辑删除，删除后 `is_deleted` 字段置为1，查询时会自动过滤已删除的文章。
5. **限流说明**：创建文章接口有频率限制，每IP每分钟最多20次请求。

---

### 2. 分页获取当前用户的文章列表

获取当前登录用户的文章列表，支持分页和状态筛选。

- **请求方法**: `POST`
- **请求路径**: `/api/blogs/list`
- **需要权限**: `article:list`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "Java",
  "isHidden": false,
  "isTop": null,
  "isRecommend": null
}
```

| 字段          | 类型      | 必填 | 说明                                 |
|-------------|---------|----|------------------------------------|
| currentPage | Integer | 是  | 当前页码，从1开始                          |
| pageSize    | Integer | 是  | 每页数量                               |
| keyword     | String  | 否  | 搜索关键词，匹配文章标题                       |
| isHidden    | Boolean | 否  | 是否隐藏筛选：null=全部, false=显示, true=隐藏  |
| isTop       | Boolean | 否  | 是否置顶筛选：null=全部, false=不置顶, true=置顶 |
| isRecommend | Boolean | 否  | 是否推荐筛选：null=全部, false=不推荐, true=推荐 |

#### 响应示例

```json
{
  "data": {
    "records": [
      {
        "id": 1,
        "categoryId": 1,
        "title": "Java并发编程实战",
        "summary": "本文介绍了Java并发编程的核心知识点",
        "coverImage": "https://example.com/images/java.jpg",
        "tags": "Java,并发",
        "viewCount": 100,
        "commentCount": 10,
        "likeCount": 50,
        "isHidden": false,
        "isTop": true,
        "isRecommend": true
      }
    ],
    "total": 20,
    "size": 10,
    "current": 1,
    "pages": 2,
    "filterOptions": {
      "isHidden": [
        { "value": null, "label": "全部" },
        { "value": false, "label": "显示" },
        { "value": true, "label": "隐藏" }
      ],
      "isTop": [
        { "value": null, "label": "全部" },
        { "value": false, "label": "不置顶" },
        { "value": true, "label": "置顶" }
      ],
      "isRecommend": [
        { "value": null, "label": "全部" },
        { "value": false, "label": "不推荐" },
        { "value": true, "label": "推荐" }
      ]
    }
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 响应字段说明

| 字段            | 类型      | 说明                       |
|---------------|---------|--------------------------|
| records       | List    | 文章列表                     |
| id            | Long    | 文章ID                     |
| categoryId    | Long    | 分类ID                     |
| title         | String  | 文章标题                     |
| summary       | String  | 文章摘要（为空时自动从HTML内容提取前50字） |
| coverImage    | String  | 封面图片URL                  |
| tags          | String  | 标签（逗号分隔）                 |
| viewCount     | Integer | 浏览量                      |
| commentCount  | Integer | 评论数                      |
| likeCount     | Integer | 点赞数                      |
| isHidden      | Boolean | 是否隐藏                     |
| isTop         | Boolean | 是否置顶                     |
| isRecommend   | Boolean | 是否推荐                     |
| total         | Long    | 总记录数                     |
| size          | Long    | 每页数量                     |
| current       | Long    | 当前页码                     |
| pages         | Long    | 总页数                      |
| filterOptions | Map     | 筛选项，供前端下拉框使用             |

#### 摘要处理说明

- 如果文章的 `summary` 字段不为空，则直接返回摘要
- 如果 `summary` 为空，则从 `content`（MD格式）中提取纯文本（去除所有MD格式标记），并截取前100个字符

---

### 3. 获取文章详情

根据文章ID获取文章的完整详情信息，只能查看自己的文章。

- **请求方法**: `GET`
- **请求路径**: `/api/blogs/{id}`
- **需要权限**: `article:detail`

#### 请求参数

| 参数 | 类型   | 必填 | 说明   |
|----|------|----|------|
| id | Long | 是  | 文章ID |

#### 响应示例

```json
{
  "data": {
    "id": 1,
    "categoryId": 1,
    "title": "Java并发编程实战",
    "summary": "本文介绍了Java并发编程的核心知识点",
    "content": "# Java并发编程\n\n## 什么是并发...",
    "coverImage": "https://example.com/images/java.jpg",
    "tags": "Java,并发,多线程",
    "authorId": 1,
    "viewCount": 100,
    "commentCount": 10,
    "likeCount": 50,
    "isHidden": false,
    "isTop": true,
    "isRecommend": true,
    "createTime": "2026-01-01T10:00:00",
    "updateTime": "2026-01-02T15:30:00"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 响应字段说明

| 字段           | 类型            | 说明         |
|--------------|---------------|------------|
| id           | Long          | 文章ID       |
| categoryId   | Long          | 分类ID       |
| title        | String        | 文章标题       |
| summary      | String        | 文章摘要       |
| content      | String        | MD格式文章内容   |
| coverImage   | String        | 封面图片URL    |
| tags         | String        | 标签（逗号分隔）   |
| authorId     | Long          | 作者ID       |
| viewCount    | Integer       | 浏览量        |
| commentCount | Integer       | 评论数        |
| likeCount    | Integer       | 点赞数        |
| isHidden     | Boolean       | 是否隐藏       |
| isTop        | Boolean       | 是否置顶       |
| isRecommend  | Boolean       | 是否推荐       |
| createTime   | LocalDateTime | 创建时间       |
| updateTime   | LocalDateTime | 更新时间       |

#### 错误响应示例（文章不存在）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "文章不存在",
  "code": 404
}
```

#### 错误响应示例（无权限）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "无权限访问该文章",
  "code": 403
}
```

---

### 4. 更新文章状态

更新文章的隐藏状态。

- **请求方法**: `PUT`
- **请求路径**: `/api/blogs/{id}/status`
- **需要权限**: `article:edit`

#### 请求参数

| 参数       | 类型      | 必填 | 说明                                |
|-----------|---------|----|-----------------------------------|
| id        | Long    | 是  | 文章ID                              |
| isHidden  | Boolean | 否  | 是否隐藏：null=不修改, true=隐藏, false=显示  |

```json
{
  "isHidden": true
}
```

#### 响应示例

```json
{
  "data": {
    "id": 1,
    "message": "文章状态更新成功"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例

```json
{
  "data": null,
  "success": false,
  "errorMsg": "无权限修改该文章",
  "code": 403
}
```

---

### 5. 更新文章内容

更新文章的标题、摘要、内容、封面图、标签等信息。

- **请求方法**: `PUT`
- **请求路径**: `/api/blogs/{id}`
- **需要权限**: `article:edit`

#### 请求参数

| 字段          | 类型     | 必填 | 说明                         |
|-------------|--------|----|----------------------------|
| id          | Long   | 是  | 文章ID                       |
| title       | String | 否  | 文章标题，最大200字符               |
| summary     | String | 否  | 文章摘要，最大200字符               |
| content     | String | 否  | MD格式的文章内容                  |
| coverImage  | String | 否  | 封面图片URL，必须为有效的http/https链接 |
| tags        | String | 否  | 标签，多个标签用逗号分隔               |
| categoryId  | Long   | 否  | 分类ID                       |

```json
{
  "title": "Java并发编程实战（更新版）",
  "summary": "本文全面介绍了Java并发编程的核心知识点",
  "content": "# Java并发编程\n\n## 线程池...",
  "coverImage": "https://example.com/images/java-v2.jpg",
  "tags": "Java,并发,多线程,线程池",
  "categoryId": 2
}
```

#### 响应示例

```json
{
  "data": {
    "id": 1,
    "message": "文章更新成功"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（标题超长）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "文章标题不能超过200字符",
  "code": 400
}
```

#### 错误响应示例（URL格式无效）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "封面图片URL格式无效，请输入有效的http/https链接",
  "code": 400
}
```

---

### 6. 删除文章

删除指定的文章（逻辑删除）。

- **请求方法**: `DELETE`
- **请求路径**: `/api/blogs/{id}`
- **需要权限**: `article:delete`

#### 请求参数

| 参数 | 类型   | 必填 | 说明   |
|----|------|----|------|
| id | Long | 是  | 文章ID |

#### 响应示例

```json
{
  "data": {
    "id": 1,
    "message": "文章删除成功"
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（文章不存在）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "文章不存在",
  "code": 404
}
```

#### 错误响应示例（无权限）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "无权限删除该文章",
  "code": 403
}
```

---

## 通用说明

1. **权限控制**：所有接口只能操作当前登录用户自己的文章，无法操作其他用户的文章。
2. **逻辑删除**：所有查询接口会自动过滤 `is_deleted=1` 的文章，删除操作会将 `is_deleted` 置为1。
3. **摘要提取**：如果文章摘要为空，会自动从HTML内容中提取纯文本并截取前50字返回。
4. **排序规则**：文章列表默认按置顶状态降序，再按创建时间降序排列。
