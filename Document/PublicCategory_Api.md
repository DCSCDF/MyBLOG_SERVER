## 公共分类接口文档

---

### 接口说明

公共分类接口用于前台展示博客分类列表，**无需登录即可访问**。该接口只返回未隐藏的分类，支持按排序顺序展示。

**接口基础路径**: `/api/public/category`

---

### 功能特性

1. **获取分类列表**: 返回所有未隐藏的分类
2. **获取分类详情**: 根据ID获取指定分类的信息（仅限未隐藏分类）
3. **缓存机制**: 使用Guava Cache缓存查询结果，缓存时间30分钟

---

## 接口详情

### 1. 获取可显示的分类列表

获取所有未隐藏的分类列表。

- **请求方法**: `GET`
- **请求路径**: `/api/public/category/list`
- **是否需要登录**: 否

#### 请求参数

无请求参数。

#### 响应示例

```json
{
  "data": [
    {
      "id": 1,
      "name": "技术分享",
      "description": "技术相关文章分类",
      "sortOrder": 10
    },
    {
      "id": 2,
      "name": "生活随笔",
      "description": "生活感悟和随笔文章",
      "sortOrder": 5
    }
  ],
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 响应字段说明

| 字段          | 类型      | 说明           |
|-------------|---------|--------------|
| id          | Long    | 分类ID         |
| name        | String  | 分类名称         |
| description | String  | 分类描述         |
| sortOrder   | Integer | 排序顺序，数字越大越靠前 |

---

### 2. 根据ID获取分类详情

根据ID获取指定分类的信息（仅限未隐藏的分类）。

- **请求方法**: `GET`
- **请求路径**: `/api/public/category/{id}`
- **是否需要登录**: 否

#### 路径参数

| 参数 | 类型   | 说明    |
|----|------|-------|
| id | Long | 分类 ID |

#### 响应示例

```json
{
  "data": {
    "id": 1,
    "name": "技术分享",
    "description": "技术相关文章分类",
    "sortOrder": 10
  },
  "success": true,
  "errorMsg": null,
  "code": 200
}
```

#### 错误响应示例（分类不存在或已隐藏）

```json
{
  "data": null,
  "success": false,
  "errorMsg": "分类不存在或已隐藏",
  "code": 404
}
```

---

### 使用示例

#### 示例1: 获取所有可显示的分类

```bash
curl -X GET http://localhost:8080/api/public/category/list
```

#### 示例2: 获取指定分类详情

```bash
curl -X GET http://localhost:8080/api/public/category/1
```

---

### 错误响应

#### 服务器内部错误

```json
{
  "data": null,
  "success": false,
  "errorMsg": "获取分类列表失败",
  "code": 500
}
```

---

### 实现细节

#### 缓存机制

- 使用 Guava Cache 作为内存缓存
- 分类列表缓存键格式: `all_visible_categories`
- 单个分类缓存键: `{categoryId}`
- 缓存过期时间: 30分钟

#### 数据库查询逻辑

1. 只查询 `is_hidden = false` 的分类（隐藏的分类不显示）
2. 按 `sort_order` 降序、`create_time` 升序排序（排序数大的排在前面）

---

### 注意事项

1. **无需鉴权**: 该接口为公开接口，前端无需携带 token 即可访问
2. **隐藏分类**: 任何 `is_hidden = true` 的分类都不会出现在列表中
3. **排序说明**: 返回的分类按 `sortOrder` 降序排列，可用于前台分类导航的排序展示
