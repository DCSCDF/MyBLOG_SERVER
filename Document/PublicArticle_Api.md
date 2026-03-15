## 公共文章列表接口文档

---

### 接口说明

公共文章列表接口用于前台展示博客文章列表，**无需登录即可访问**。该接口会返回所有公开的文章（隐藏的文章不显示），支持分页、关键词搜索、分类筛选和标签筛选。

**接口基础路径**: `/api/public/article`

---

### 功能特性

1. **分页查询**: 支持指定页码和每页数量
2. **关键词搜索**: 支持模糊搜索文章标题和摘要
3. **分类筛选**: 支持按分类名称筛选（模糊搜索）
4. **标签筛选**: 支持按标签名称筛选（模糊搜索）
5. **置顶优先**: 置顶的文章始终排在列表最前面
6. **自动摘要**: 文章摘要为空时，自动从HTML内容中提取前50字
7. **缓存机制**: 使用Guava Cache缓存查询结果，缓存时间30分钟

---

### 筛选项说明

响应中会返回可用的分类筛选项（`categoryOptions`）和标签筛选项（`tagOptions`），供前端渲染筛选控件。

---

## 接口详情

### 1. 分页获取公共文章列表

获取公开的文章列表，支持分页、关键词搜索、分类筛选和标签筛选。

- **请求方法**: `POST`
- **请求路径**: `/api/public/article/list`
- **是否需要登录**: 否

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "Java",
  "categoryName": "技术",
  "tagName": "Spring"
}
```

| 字段           | 类型      | 必填 | 说明                                              |
|--------------|---------|----|-------------------------------------------------|
| currentPage  | Integer | 是  | 当前页码（从 1 开始）                                   |
| pageSize     | Integer | 是  | 每页数量                                           |
| keyword      | String  | 否  | 搜索关键词，同时匹配文章标题和摘要（模糊搜索）                      |
| categoryName | String  | 否  | 分类名称筛选（模糊搜索，匹配分类名称）                        |
| tagName      | String  | 否  | 标签名称筛选（模糊搜索，匹配文章标签字段）                     |

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
        "viewCount": 1234,
        "commentCount": 56,
        "likeCount": 78,
        "isTop": true,
        "authorNickname": "张三",
        "createTime": "2026-03-14T10:00:00"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10,
    "categoryOptions": [
      { "value": 1, "label": "技术" },
      { "value": 2, "label": "生活" },
      { "value": 3, "label": "随笔" }
    ],
    "tagOptions": [
      { "value": "Java", "label": "Java" },
      { "value": "Python", "label": "Python" },
      { "value": "Spring", "label": "Spring" }
    ]
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 响应字段说明

| 字段             | 类型       | 说明                                        |
|----------------|----------|-------------------------------------------|
| records        | Array    | 文章列表                                      |
| total          | Long     | 总记录数                                      |
| size           | Long     | 每页数量                                      |
| current        | Long     | 当前页码                                      |
| pages          | Long     | 总页数                                       |
| categoryOptions | Array   | 可用的分类筛选项                                |
| tagOptions     | Array    | 可用的标签筛选项                                |

#### records 中的字段说明

| 字段             | 类型       | 说明                                        |
|----------------|----------|-------------------------------------------|
| id             | Long     | 文章ID                                      |
| categoryId     | Long     | 分类ID                                      |
| categoryName   | String   | 分类名称                                     |
| title          | String   | 文章标题                                     |
| summary        | String   | 文章摘要（为空时自动从HTML内容提取前50字，去除HTML标签）   |
| coverImage     | String   | 封面图片URL                                  |
| tags           | String   | 标签（逗号分隔）                                |
| viewCount      | Integer  | 浏览量                                      |
| commentCount   | Integer  | 评论数                                      |
| likeCount      | Integer  | 点赞数                                      |
| isTop          | Boolean  | 是否置顶                                     |
| authorNickname  | String   | 作者昵称                                     |
| createTime     | DateTime | 创建时间                                     |

---

### 使用示例

#### 示例1: 获取全部文章（默认第一页）

```bash
curl -X POST http://localhost:8080/api/public/article/list \
  -H "Content-Type: application/json" \
  -d '{"currentPage": 1, "pageSize": 10}'
```

#### 示例2: 搜索包含"Java"的文章

```bash
curl -X POST http://localhost:8080/api/public/article/list \
  -H "Content-Type: application/json" \
  -d '{"currentPage": 1, "pageSize": 10, "keyword": "Java"}'
```

#### 示例3: 筛选"技术"分类的文章

```bash
curl -X POST http://localhost:8080/api/public/article/list \
  -H "Content-Type: application/json" \
  -d '{"currentPage": 1, "pageSize": 10, "categoryName": "技术"}'
```

#### 示例4: 筛选包含"Spring"标签的文章

```bash
curl -X POST http://localhost:8080/api/public/article/list \
  -H "Content-Type: application/json" \
  -d '{"currentPage": 1, "pageSize": 10, "tagName": "Spring"}'
```

#### 示例5: 组合搜索：搜索"教程"且分类为"技术"

```bash
curl -X POST http://localhost:8080/api/public/article/list \
  -H "Content-Type: application/json" \
  -d '{"currentPage": 1, "pageSize": 10, "keyword": "教程", "categoryName": "技术"}'
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
- 缓存键格式: `public_article_list:{currentPage}-{pageSize}-{keyword}-{categoryName}-{tagName}`
- 缓存过期时间: 30分钟
- 缓存最大容量: 100条

#### 数据库查询逻辑

1. 只查询 `is_hidden = false` 的文章（隐藏的文章不显示）
2. 按 `is_top` 降序、`create_time` 降序排序（置顶的文章排在最前）
3. 关键词搜索使用 LIKE 进行模糊匹配，同时匹配标题和摘要
4. 分类筛选先通过分类名称模糊匹配找到分类ID列表，再使用 IN 查询
5. 标签筛选使用 LIKE 进行模糊匹配

#### 摘要自动提取

当文章的 `summary` 字段为空时，系统会自动：
1. 从 HTML 内容中去除所有 HTML 标签
2. 去除 script 和 style 标签及其内容
3. 替换 HTML 实体（如 `&nbsp;`、`&lt;` 等）
4. 截取前50个字符返回

---

### 注意事项

1. **无需鉴权**: 该接口为公开接口，前端无需携带 token 即可访问
2. **隐藏文章**: 任何 `is_hidden = true` 的文章都不会出现在列表中
3. **置顶文章**: 置顶的文章始终显示在最前面，不受分页影响
4. **分类筛选**: 分类名称支持模糊搜索，如传入"技术"可以匹配"技术分享"、"技术教程"等
5. **标签筛选**: 标签搜索支持模糊匹配，如传入"Spring"可以匹配"Spring Boot"、"Spring Cloud"等
6. **筛选项**: 响应中的 `categoryOptions` 和 `tagOptions` 会根据数据库中的实际数据动态生成，供前端构建筛选组件
7. **性能优化**: 高并发场景下建议配合 CDN 和 Nginx 缓存使用

