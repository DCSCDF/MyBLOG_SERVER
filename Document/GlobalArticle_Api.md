## 全局文章管理接口文档

---

### 接口鉴权说明

所有全局文章管理接口均需要登录，且需在请求 header 中携带 token，格式为：`{tokenName: tokenValue}`（tokenName 默认为 token，可在 application.properties 中修改）。

全局文章管理使用以下权限码：

- `system:article:list` - 查看全局文章列表
- `system:article:edit` - 编辑文章状态（隐藏、置顶、推荐）
- `system:article:delete` - 删除文章

**重要说明**：`system:article` 为父权限，拥有此权限将自动包含所有 `system:article:*` 子权限。

全局文章管理接口可以管理所有用户的文章，不受作者限制。

---

## 全局文章管理接口

基础路径：`/api/global-article`

---

### 1. 分页获取所有文章列表

获取所有用户的文章列表，支持分页和状态筛选。

- **请求方法**: `POST`
- **请求路径**: `/api/global-article/list`
- **需要权限**: `system:article:list`

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
        "isRecommend": true,
        "authorId": 1,
        "authorNickname": "张三"
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

| 字段             | 类型      | 说明                       |
|----------------|---------|--------------------------|
| records        | List    | 文章列表                     |
| id             | Long    | 文章ID                     |
| categoryId     | Long    | 分类ID                     |
| title          | String  | 文章标题                     |
| summary        | String  | 文章摘要（为空时自动从HTML内容提取前50字） |
| coverImage     | String  | 封面图片URL                  |
| tags           | String  | 标签（逗号分隔）                 |
| viewCount      | Integer | 浏览量                      |
| commentCount   | Integer | 评论数                      |
| likeCount      | Integer | 点赞数                      |
| isHidden       | Boolean | 是否隐藏                     |
| isTop          | Boolean | 是否置顶                     |
| isRecommend    | Boolean | 是否推荐                     |
| authorId       | Long    | 作者用户ID                   |
| authorNickname | String  | 作者昵称（用户已注销时显示"用户已注销"）    |
| total          | Long    | 总记录数                     |
| size           | Long    | 每页数量                     |
| current        | Long    | 当前页码                     |
| pages          | Long    | 总页数                      |
| filterOptions  | Map     | 筛选项，供前端下拉框使用             |

#### 摘要处理说明

- 如果文章的 `summary` 字段不为空，则直接返回摘要
- 如果 `summary` 为空，则从 `htmlContent` 中提取纯文本（去除所有HTML标签），并截取前50个字符

---

### 2. 更新文章状态

更新文章的隐藏、置顶、推荐状态。

- **请求方法**: `PUT`
- **请求路径**: `/api/global-article/{id}/status`
- **需要权限**: `system:article:edit`

#### 请求参数

| 参数          | 类型      | 必填 | 说明                                   |
|-------------|---------|----|--------------------------------------|
| id          | Long    | 是  | 文章ID                                 |
| isHidden    | Boolean | 否  | 是否隐藏：null=不修改, true=隐藏, false=显示     |
| isTop       | Boolean | 否  | 是否置顶：null=不修改, true=置顶, false=不置顶    |
| isRecommend | Boolean | 否  | 是否推荐：null=不修改, true=推荐, false=不推荐    |

```json
{
  "isHidden": false,
  "isTop": true,
  "isRecommend": true
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

#### 错误响应示例（文章不存在）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "文章不存在",
  "code": 404
}
```

---

### 3. 删除文章

删除指定的文章（逻辑删除）。

- **请求方法**: `DELETE`
- **请求路径**: `/api/global-article/{id}`
- **需要权限**: `system:article:delete`

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

---

## 通用说明

1. **权限说明**：全局文章管理接口可以管理所有用户的文章，无需检查文章作者
2. **逻辑删除**：所有查询接口会自动过滤 `is_deleted=1` 的文章，删除操作会将 `is_deleted` 置为1
3. **摘要提取**：如果文章摘要为空，会自动从HTML内容中提取纯文本并截取前50个字返回
4. **排序规则**：文章列表默认按置顶状态降序，再按创建时间降序排列
