## 公共文章列表接口文档

---

### 接口说明

公共文章列表接口用于前台展示博客文章列表，**无需登录即可访问**。该接口会返回所有公开的文章（隐藏的文章不显示），支持分页和关键词搜索。

**接口基础路径**: `/api/public/article`

---

### 功能特性

1. **分页查询**: 支持指定页码和每页数量
2. **关键词搜索**: 支持模糊搜索文章标题和摘要
3. **分类筛选**: 支持按分类ID筛选文章（传入categoryId后，搜索也只在该分类内进行）
4. **置顶优先**: 置顶的文章始终排在列表最前面
5. **自动摘要**: 文章摘要为空时，自动从HTML内容中提取前50个字
6. **缓存机制**: 使用Guava Cache缓存查询结果，缓存时间30分钟
7. **文章详情**: 支持通过文章ID获取文章全文内容

---

## 接口详情

### 1. 分页获取公共文章列表

获取公开的文章列表，支持分页和关键词搜索。

- **请求方法**: `POST`
- **请求路径**: `/api/public/article/list`
- **是否需要登录**: 否

#### 请求参数

```json
{
   "currentPage": 1,
   "pageSize": 10,
   "keyword": "Java",
   "categoryId": 5
}
```

| 字段          | 类型      | 必填 | 说明                              |
|-------------|---------|----|---------------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）                    |
| pageSize    | Integer | 是  | 每页数量                            |
| keyword     | String  | 否  | 搜索关键词，同时匹配文章标题、摘要、分类名称和标签（模糊搜索） |
| categoryId  | Long    | 否  | 分类ID，用于筛选指定分类的文章；传入后搜索也只在该分类内进行 |

#### 响应示例

```json
{
   "data": {
      "records": [
         {
            "id": 1,
            "categoryId": 5,
            "categoryName": "技术",
            "title": "Spring Boot 最佳实践",
            "summary": "本文介绍了Spring Boot的开发最佳实践，包括项目结构、配置管理...",
            "coverImage": "https://example.com/images/spring-boot.jpg",
            "tags": "Java,Spring,后端",
            "commentCount": 56,
            "isTop": true,
            "authorNickname": "张三",
            "createTime": "2026-03-14T10:00:00"
         }
      ],
      "total": 100,
      "size": 10,
      "current": 1,
      "pages": 10
   },
   "success": true,
   "errorMsg": null,
   "code": 200
}
```

#### 响应字段说明

| 字段      | 类型    | 说明   |
|---------|-------|------|
| records | Array | 文章列表 |
| total   | Long  | 总记录数 |
| size    | Long  | 每页数量 |
| current | Long  | 当前页码 |
| pages   | Long  | 总页数  |

#### records 中的字段说明

| 字段             | 类型       | 说明                                 |
|----------------|----------|------------------------------------|
| id             | Long     | 文章ID                               |
| categoryId     | Long     | 分类ID                               |
| categoryName   | String   | 分类名称                               |
| title          | String   | 文章标题                               |
| summary        | String   | 文章摘要（为空时自动从HTML内容提取前100字，去除HTML标签） |
| coverImage     | String   | 封面图片URL                            |
| tags           | String   | 标签（逗号分隔）                           |
| commentCount   | Integer  | 评论数（仅统计已通过的评论，包括子评论）               |
| isTop          | Boolean  | 是否置顶                               |
| authorNickname | String   | 作者昵称                               |
| createTime     | DateTime | 创建时间                               |

---

### 2. 获取公共文章详情

根据文章ID获取文章的完整详情信息，包括文章全文内容。

- **请求方法**: `GET`
- **请求路径**: `/api/public/article/{id}`
- **是否需要登录**: 否

#### 请求参数

| 参数 | 类型   | 必填 | 说明   |
|----|------|----|------|
| id | Long | 是  | 文章ID |

#### 响应示例

```json
{
   "data": {
      "id": 1,
      "categoryId": 5,
      "categoryName": "技术",
      "title": "Spring Boot 最佳实践",
      "mdContent": "# Spring Boot 最佳实践\n\n## 项目结构\n\n合理的项目结构可以提高代码的可维护性...",
      "tags": "Java,Spring,后端",
      "commentCount": 56,
      "isTop": true,
      "authorNickname": "张三",
      "createTime": "2026-03-14T10:00:00"
   },
   "success": true,
   "errorMsg": null,
   "code": 200
}
```

#### 响应字段说明

| 字段             | 类型       | 说明                                |
|----------------|----------|-----------------------------------|
| id             | Long     | 文章ID                              |
| categoryId     | Long     | 分类ID                              |
| categoryName   | String   | 分类名称（如果分类已隐藏则返回null）              |
| title          | String   | 文章标题                              |
| mdContent      | String   | 文章内容（Markdown格式原文，前端负责渲染） |
| tags           | String   | 标签（逗号分隔）                          |
| commentCount   | Integer  | 评论数（仅统计已通过的评论，包括子评论）              |
| isTop          | Boolean  | 是否置顶                              |
| authorNickname | String   | 作者昵称                              |
| createTime     | DateTime | 创建时间                              |

#### 错误响应

**文章不存在或已下架**

```json
{
   "data": null,
   "success": false,
   "errorMsg": "文章不存在或已下架",
   "code": 404
}
```

---

### 使用示例

#### 示例1: 获取文章详情

```bash
curl -X GET "http://localhost:8080/api/public/article/1"
```

#### 示例2: 获取全部文章（默认第一页）

```bash
curl -X POST http://localhost:8080/api/public/article/list \
  -H "Content-Type: application/json" \
  -d '{"currentPage": 1, "pageSize": 10}'
```

#### 示例3: 搜索包含"Java"的文章

```bash
curl -X POST http://localhost:8080/api/public/article/list \
  -H "Content-Type: application/json" \
  -d '{"currentPage": 1, "pageSize": 10, "keyword": "Java"}'
```

#### 示例4: 获取指定分类下的文章

```bash
curl -X POST http://localhost:8080/api/public/article/list \
  -H "Content-Type: application/json" \
  -d '{"currentPage": 1, "pageSize": 10, "categoryId": 5}'
```

#### 示例5: 在指定分类内搜索文章

```bash
curl -X POST http://localhost:8080/api/public/article/list \
  -H "Content-Type: application/json" \
  -d '{"currentPage": 1, "pageSize": 10, "categoryId": 5, "keyword": "Spring"}'
```

---

### 错误响应

#### 示例6: 获取不存在的文章

```bash
curl -X GET "http://localhost:8080/api/public/article/999"
```

```json
{
   "data": null,
   "success": false,
   "errorMsg": "文章不存在或已下架",
   "code": 404
}
```

---

### 错误响应

#### 参数校验失败

```json
{
   "data": null,
   "success": false,
   "errorMsg": "当前页码不能为空",
   "code": 400
}
```

#### 服务器内部错误

```json
{
   "data": null,
   "success": false,
   "errorMsg": "获取文章列表失败",
   "code": 500
}
```

---

### 实现细节

#### 缓存机制

- 使用 Guava Cache 作为内存缓存
- 缓存键格式: `public_article_list:{currentPage}-{pageSize}-{categoryId}-{keyword}`
- 缓存过期时间: 30分钟
- 缓存最大容量: 100条

#### 数据库查询逻辑

1. 只查询 `is_hidden = false` 的文章（隐藏的文章不显示）
2. 如果传入了 `categoryId`，只查询该分类下的文章
3. 按 `is_top` 降序、`create_time` 降序排序（置顶的文章排在最前）
4. 关键词搜索逻辑：
   - 如果传入了 `categoryId`，只在该分类内搜索（标题、摘要、标签）
   - 如果未传入 `categoryId`，搜索所有分类，匹配标题、摘要、分类名称和标签

#### 摘要自动提取

当文章的 `summary` 字段为空时，系统会自动：
1. 从 HTML 内容中去除所有 HTML 标签
2. 去除 script 和 style 标签及其内容
3. 替换 HTML 实体（如 `&nbsp;`、`&lt;` 等）
4. 截取前100个字符返回

---

### 注意事项

1. **无需鉴权**: 该接口为公开接口，前端无需携带 token 即可访问
2. **隐藏文章**: 任何 `is_hidden = true` 的文章都不会出现在列表中
3. **置顶文章**: 置顶的文章始终显示在最前面，不受分页影响
4. **分类筛选**: 传入 `categoryId` 后，只返回该分类下的文章，搜索也只在该分类内进行
5. **性能优化**: 高并发场景下建议配合 CDN 和 Nginx 缓存使用

