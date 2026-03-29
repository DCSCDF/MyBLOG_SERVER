## RSS Feed 接口文档

---

### 接口说明

RSS Feed 接口用于获取博客的最新文章列表，采用 Atom 1.0 标准格式输出，**无需登录即可访问**。可通过 RSS 阅读器订阅博客更新。

**接口基础路径**: `/api/public/rss`

---

### 功能特性

1. **Atom 1.0 标准**: 遵循 Atom 1.0  syndication format
2. **最新文章**: 自动返回最近发布的 10 篇文章
3. **时间排序**: 按发布时间倒序排列（最新的在前）
4. **完整元数据**: 包含标题、链接、摘要、作者、标签等信息
5. **缓存优化**: HTTP 缓存头设置为 30 分钟

---

## 接口详情

### 获取 RSS Feed

获取博客的最新文章 RSS Feed（Atom 格式）。

- **请求方法**: `GET`
- **请求路径**: `/api/public/rss`
- **是否需要登录**: 否
- **返回格式**: `application/atom+xml; charset=UTF-8`

#### 请求参数

无请求参数。

#### 响应示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>我的博客</title>
  <subtitle>分享技术与生活的博客</subtitle>
  <link href="https://example.com" rel="alternate" type="text/html"/>
  <link href="https://example.com/rss" rel="self" type="application/atom+xml"/>
  <id>urn:uuid:xxxxx</id>
  <updated>2026-03-29T10:30:00+08:00</updated>
  <entry>
    <title>Spring Boot 最佳实践</title>
    <link href="https://example.com/article/1" rel="alternate" type="text/html"/>
    <id>urn:uuid:xxxxx</id>
    <updated>2026-03-29T10:00:00+08:00</updated>
    <summary type="html">本文介绍了Spring Boot的开发最佳实践，包括项目结构、配置管理...</summary>
    <content type="html"><div style='margin-bottom: 20px; color: #666;'>
<span>作者：张三</span> &nbsp;|&nbsp; <span>发布时间：2026-03-29T10:00</span> &nbsp;|&nbsp; <span>标签：Java,Spring</span>
</div>
<h1>Spring Boot 最佳实践</h1>
<h2>项目结构</h2>
<p>合理的项目结构可以提高代码的可维护性...</p>
<hr/>
<p style='color: #888; font-size: 12px;'>
原文链接：<a href='https://example.com/article/1'>https://example.com/article/1</a>
</p></content>
    <category term="Java"/>
    <category term="Spring"/>
    <author>
      <name>张三</name>
    </author>
  </entry>
  <entry>
    <title>Docker 容器化部署指南</title>
    <link href="https://example.com/article/2" rel="alternate" type="text/html"/>
    <id>urn:uuid:xxxxx</id>
    <updated>2026-03-28T15:30:00+08:00</updated>
    <summary type="html">本文详细介绍了如何使用Docker进行应用容器化部署...</summary>
    <category term="DevOps"/>
    <category term="Docker"/>
    <author>
      <name>张三</name>
    </author>
  </entry>
</feed>
```

#### 响应字段说明

| 字段              | 类型      | 说明                                       |
|-----------------|---------|------------------------------------------|
| feed            | Element | Atom Feed 根元素                            |
| title           | Element | 网站名称                                   |
| subtitle        | Element | 网站描述                                   |
| link[@rel=alternate] | Element | 网站首页链接                               |
| link[@rel=self] | Element | RSS Feed 自身链接                          |
| id              | Element | Feed 唯一标识符                             |
| updated         | Element | Feed 最后更新时间                           |
| entry           | Element | 文章条目，可包含多个                         |
| entry.title     | Element | 文章标题                                   |
| entry.link      | Element | 文章详情页链接                              |
| entry.id        | Element | 文章唯一标识符（UUID 格式）                    |
| entry.updated    | Element | 文章发布时间                                |
| entry.summary   | Element | 文章摘要（HTML 格式，最多 200 字符，用于 Feed 阅读器预览） |
| entry.content   | Element | 文章完整内容（HTML 格式，已将 Markdown 转换为 HTML）      |
| entry.category  | Element | 文章标签，可包含多个                                    |
| entry.author    | Element | 作者信息                                               |
| entry.author.name | Element | 作者昵称                                             |

---

### 使用示例

#### 示例1: 在浏览器中查看 RSS Feed

```
http://localhost:8080/api/public/rss
```

#### 示例2: 使用 curl 获取 RSS Feed

```bash
curl -X GET "http://localhost:8080/api/public/rss"
```

#### 示例3: 保存 RSS Feed 到文件

```bash
curl -X GET "http://localhost:8080/api/public/rss" -o feed.xml
```

#### 示例4: 添加 HTTP 头获取

```bash
curl -X GET "http://localhost:8080/api/public/rss" \
  -H "Accept: application/atom+xml"
```

---

### 错误响应

#### 服务器内部错误

```xml
<?xml version="1.0" encoding="UTF-8"?>
<error>RSS Feed 生成失败</error>
```

---

### 实现细节

#### 数据来源

1. **文章查询条件**:
   - `is_hidden = false`（只返回公开文章）
   - 按 `create_time` 降序排序
   - 限制返回 10 条记录

2. **元数据获取**:
   - 网站名称、域名、描述：从 `sys_config` 表获取
   - 作者昵称：从 `sys_user` 表批量查询

#### 内容生成规则

每个 entry 包含以下内容：

1. **摘要 (summary)**:
   - 优先使用文章的 `summary` 字段
   - 如果为空，从 Markdown 内容提取纯文本
   - 最多 200 字符，用于阅读器列表预览

2. **完整内容 (content)**:
   - 文章元信息：作者、发布时间、标签
   - Markdown 转换为 HTML 的完整正文
   - 底部添加原文链接

| 字段        | 规则                                              |
|-----------|-------------------------------------------------|
| title     | 使用 `site.name` 配置，如无则使用 "My Blog"             |
| link      | 使用 `site.domain` 配置                              |
| entry.title | 文章标题，原文输出                                  |
| entry.link | 格式为 `{site.domain}/article/{articleId}`         |
| entry.id  | UUID 格式: `urn:uuid:{hash}`                      |
| entry.updated | 文章创建时间，RFC 3339 格式                          |
| entry.summary | 优先使用文章摘要字段，其次从 Markdown 内容提取纯文本，最多 200 字符 |
| entry.content | 文章完整 HTML 内容，包括作者、发布时间、标签、Markdown 转换后的正文、原文链接 |
| entry.category | 按逗号分隔的标签，每个标签生成一个 `<category term="...">` |
| entry.author | 文章作者昵称                                      |

#### 缓存策略

- HTTP 响应头: `Cache-Control: public, max-age=1800`
- 浏览器/CDN 缓存时间: 30 分钟

---

### 订阅方式

#### RSS 阅读器订阅

将以下 URL 添加到任何支持 Atom/RSS 的阅读器中：

```
https://your-domain.com/api/public/rss
```

#### 博客主题添加 RSS 链接

在博客 HTML 的 `<head>` 中添加自动发现链接：

```html
<link rel="alternate" type="application/atom+xml" title="RSS Feed" href="/api/public/rss">
```

---

### 注意事项

1. **无需鉴权**: 该接口为公开接口，前端无需携带 token 即可访问
2. **仅公开文章**: 只有 `is_hidden = false` 的文章会出现在 Feed 中
3. **时间排序**: 文章按创建时间倒序，最新的 10 篇
4. **完整内容**: 每篇文章包含完整 HTML 内容，可在 Feed 阅读器中直接阅读
5. **摘要预览**: summary 字段用于阅读器列表预览，最多 200 字符
6. **字符编码**: 返回内容使用 UTF-8 编码，确保特殊字符正确显示
7. **HTML 安全**: 文章内容经过 XSS 净化处理

