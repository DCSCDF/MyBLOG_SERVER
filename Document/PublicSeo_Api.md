## 公共SEO接口文档

---

### 接口说明

公共SEO接口用于前台页面获取SEO配置信息，**无需登录即可访问**。前端可以根据页面类型获取对应的SEO元数据（title、keywords、description、Open Graph标签等）。

**接口基础路径**: `/api/public/seo`

---

### 功能特性

1. **页面类型查询**: 根据页面类型获取SEO配置
2. **精确匹配**: 支持传入pageId进行精确匹配（如特定文章的SEO配置）
3. **默认回退**: 如果没有精确匹配到pageId，会回退匹配该页面类型的通用配置
4. **缓存机制**: 使用Guava Cache缓存查询结果，缓存时间30分钟
5. **Open Graph支持**: 返回完整的Open Graph标签信息，用于社交媒体分享

---

## 接口详情

### 1. 根据页面类型获取SEO配置

根据页面类型获取SEO配置信息。如果传入pageId，会优先匹配该pageId对应的配置。

- **请求方法**: `GET`
- **请求路径**: `/api/public/seo`
- **是否需要登录**: 否

#### 请求参数

| 参数       | 类型     | 必填 | 说明                                             |
|----------|--------|----|------------------------------------------------|
| pageType | String | 是  | 页面类型（如：`index`、`article`、`category`、`about` 等） |
| pageId   | Long   | 否  | 页面ID（如文章ID、分类ID等），用于精确匹配特定页面的SEO配置             |

#### 匹配规则

1. 如果同时传入 `pageType` 和 `pageId`，优先匹配该组合的配置
2. 如果没有精确匹配到 `pageId`，会回退匹配 `pageType + pageId IS NULL` 的通用配置
3. 如果只传入 `pageType`，只匹配 `pageId` 为空的配置

#### 响应示例

**成功响应**

```json
{
   "data": {
      "id": 1,
      "pageType": "index",
      "pageId": null,
      "title": "我的博客 - 记录技术与生活的点滴",
      "keywords": "博客,技术博客,个人博客,技术分享,编程,开发",
      "description": "我的个人博客，分享技术心得、生活感悟和编程经验",
      "ogTitle": "我的博客 - 记录技术与生活的点滴",
      "ogDescription": "我的个人博客，分享技术心得、生活感悟和编程经验",
      "ogImage": "https://example.com/og-image.jpg",
      "ogType": "website",
      "canonicalUrl": "https://example.com",
      "robots": "index,follow"
   },
   "success": true,
   "errorMsg": null,
   "code": 200
}
```

#### 响应字段说明

| 字段            | 类型     | 说明                                      |
|---------------|--------|-----------------------------------------|
| id            | Long   | SEO配置ID                                 |
| pageType      | String | 页面类型                                    |
| pageId        | Long   | 关联页面ID（无关联时为null）                       |
| title         | String | SEO标题（用于`<title>`标签）                    |
| keywords      | String | SEO关键词（用于`<meta name="keywords">`，逗号分隔） |
| description   | String | SEO描述（用于`<meta name="description">`）    |
| ogTitle       | String | Open Graph标题（用于`og:title`）              |
| ogDescription | String | Open Graph描述（用于`og:description`）        |
| ogImage       | String | Open Graph图片URL（用于`og:image`）           |
| ogType        | String | Open Graph类型（如`website`、`article`）      |
| canonicalUrl  | String | 规范URL（用于`<link rel="canonical">`）       |
| robots        | String | robots指令（如`index,follow`）               |

---

### 2. 获取文章详情页SEO配置（精确匹配）

获取特定文章的SEO配置，传入文章ID进行精确匹配。

- **请求方法**: `GET`
- **请求路径**: `/api/public/seo?pageType=article&pageId=123`
- **是否需要登录**: 否

#### 响应示例

```json
{
   "data": {
      "id": 5,
      "pageType": "article",
      "pageId": 123,
      "title": "Spring Boot 最佳实践 - 我的博客",
      "keywords": "Spring Boot,Java,后端开发,微服务",
      "description": "本文详细介绍Spring Boot的开发最佳实践，包括项目结构、配置管理、异常处理等核心内容。",
      "ogTitle": "Spring Boot 最佳实践 - 我的博客",
      "ogDescription": "本文详细介绍Spring Boot的开发最佳实践，包括项目结构、配置管理、异常处理等核心内容。",
      "ogImage": "https://example.com/images/spring-boot.jpg",
      "ogType": "article",
      "canonicalUrl": "https://example.com/article/123",
      "robots": "index,follow"
   },
   "success": true,
   "errorMsg": null,
   "code": 200
}
```

---

### 3. 获取分类页SEO配置

获取分类页面的SEO配置。

- **请求方法**: `GET`
- **请求路径**: `/api/public/seo?pageType=category&pageId=5`
- **是否需要登录**: 否

#### 响应示例

```json
{
   "data": {
      "id": 3,
      "pageType": "category",
      "pageId": 5,
      "title": "技术文章 - 我的博客",
      "keywords": "技术,编程,开发,教程",
      "description": "技术类文章合集，包含Java、Python、前端等技术文章。",
      "ogTitle": "技术文章 - 我的博客",
      "ogDescription": "技术类文章合集，包含Java、Python、前端等技术文章。",
      "ogImage": null,
      "ogType": "website",
      "canonicalUrl": "https://example.com/category/5",
      "robots": "index,follow"
   },
   "success": true,
   "errorMsg": null,
   "code": 200
}
```

---

## 使用示例

### 示例1: 获取首页SEO配置

```bash
curl -X GET "http://localhost:8080/api/public/seo?pageType=index"
```

### 示例2: 获取文章详情SEO配置

```bash
curl -X GET "http://localhost:8080/api/public/seo?pageType=article&pageId=123"
```

### 示例3: 获取分类SEO配置

```bash
curl -X GET "http://localhost:8080/api/public/seo?pageType=category&pageId=5"
```

### 示例4: 获取关于页面SEO配置

```bash
curl -X GET "http://localhost:8080/api/public/seo?pageType=about"
```

---

## 错误响应

### 示例5: 页面类型为空

```bash
curl -X GET "http://localhost:8080/api/public/seo"
```

```json
{
   "data": null,
   "success": false,
   "errorMsg": "页面类型不能为空",
   "code": 400
}
```

### 示例6: 未找到SEO配置

```bash
curl -X GET "http://localhost:8080/api/public/seo?pageType=nonexistent"
```

```json
{
   "data": null,
   "success": false,
   "errorMsg": "未找到该页面类型的SEO配置",
   "code": 404
}
```

### 示例7: 服务器内部错误

```json
{
   "data": null,
   "success": false,
   "errorMsg": "获取SEO配置失败",
   "code": 500
}
```

---

## 前端使用建议

### 1. 在页面组件中调用SEO接口

```javascript
// 在页面组件的 created 或 useEffect 中调用
async function loadSeoConfig() {
  const pageType = 'article';
  const pageId = 123; // 从路由参数获取

  const response = await fetch(`/api/public/seo?pageType=${pageType}${pageId ? '&pageId=' + pageId : ''}`);
  const result = await response.json();

  if (result.success) {
    const seo = result.data;
    // 更新页面标题
    document.title = seo.title;
    // 更新meta标签
    updateMetaTags(seo);
  }
}

function updateMetaTags(seo) {
  // 更新 keywords
  let keywordsMeta = document.querySelector('meta[name="keywords"]');
  if (!keywordsMeta) {
    keywordsMeta = document.createElement('meta');
    keywordsMeta.name = 'keywords';
    document.head.appendChild(keywordsMeta);
  }
  keywordsMeta.content = seo.keywords;

  // 更新 description
  let descriptionMeta = document.querySelector('meta[name="description"]');
  if (!descriptionMeta) {
    descriptionMeta = document.createElement('meta');
    descriptionMeta.name = 'description';
    document.head.appendChild(descriptionMeta);
  }
  descriptionMeta.content = seo.description;

  // 更新 Open Graph 标签
  updateOrCreateMeta('og:title', seo.ogTitle);
  updateOrCreateMeta('og:description', seo.ogDescription);
  updateOrCreateMeta('og:image', seo.ogImage);
  updateOrCreateMeta('og:type', seo.ogType);
  updateOrCreateMeta('og:url', seo.canonicalUrl);

  // 更新 canonical URL
  let canonicalLink = document.querySelector('link[rel="canonical"]');
  if (!canonicalLink) {
    canonicalLink = document.createElement('link');
    canonicalLink.rel = 'canonical';
    document.head.appendChild(canonicalLink);
  }
  canonicalLink.href = seo.canonicalUrl;

  // 更新 robots
  let robotsMeta = document.querySelector('meta[name="robots"]');
  if (!robotsMeta) {
    robotsMeta = document.createElement('meta');
    robotsMeta.name = 'robots';
    document.head.appendChild(robotsMeta);
  }
  robotsMeta.content = seo.robots;
}

function updateOrCreateMeta(property, content) {
  if (!content) return;
  let meta = document.querySelector(`meta[property="${property}"]`);
  if (!meta) {
    meta = document.createElement('meta');
    meta.setAttribute('property', property);
    document.head.appendChild(meta);
  }
  meta.content = content;
}
```

### 2. 常用页面类型参考

| 页面类型     | 说明     | pageId参数 |
|----------|--------|----------|
| index    | 首页     | 无        |
| article  | 文章详情页  | 文章ID     |
| category | 分类列表页  | 分类ID     |
| tag      | 标签列表页  | 标签ID（可选） |
| about    | 关于页面   | 无        |
| contact  | 联系页面   | 无        |
| archive  | 归档页面   | 无        |
| search   | 搜索结果页面 | 无        |

---

## 实现细节

### 缓存机制

- 使用 Guava Cache 作为内存缓存
- 缓存键格式: `seo:{pageType}:{pageId}`
- 缓存过期时间: 30分钟
- 缓存最大容量: 500条

### 数据库查询逻辑

1. 查询条件：`page_type = ?` AND `is_deleted = 0`
2. 如果传入 `pageId`，优先匹配 `page_id = ?` 的记录
3. 如果没有精确匹配，回退到 `page_id IS NULL` 的通用配置
4. 按 `page_id` 非空优先排序，确保精确匹配的结果排在前面

---

## 注意事项

1. **无需鉴权**: 该接口为公开接口，前端无需携带 token 即可访问
2. **SEO配置**: 确保后台管理系统中已配置各页面类型的SEO数据
3. **性能优化**: 接口内置30分钟缓存，高并发场景下建议配合CDN使用
4. **社交分享**: Open Graph标签用于微信、微博、Facebook等社交平台的分享预览
5. **规范URL**: 建议配置canonicalUrl避免重复内容问题
6. **robots指令**: 通过robots meta标签控制搜索引擎爬取行为

