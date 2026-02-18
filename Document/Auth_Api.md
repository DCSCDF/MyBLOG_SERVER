## 用户认证模块接口文档

---

### 前端将 token 提交到后端

将 token 塞到请求header里 ，格式为：`{tokenName: tokenValue}`
(tokenName可以在application.properties里面修改，默认为token)。

对于需要登陆的的接口 如果未登录则会有全局拦截器返回401。

---

## 验证码接口 (参考 AJ-Captcha)
验证码服务接口，用于获取、校验和二次验证验证码功能。

基础路径`/api/captcha`

###  获取验证码：
获取验证码信息，用于前端展示验证码图片

- **请求方法**: `POST`
- **请求路径**: `/api/captcha/get`

### 检查验证码：
校验用户输入的验证码是否正确

- **请求方法**: `POST`
- **请求路径**: `/api/captcha/check`

### 二次验证：
进行验证码的二次验证，用于重要操作前的身份确认

- **请求方法**: `POST`
- **请求路径**: `/api/captcha/verify`

#### 参数说明

CaptchaVO 包含验证码相关的验证信息，具体字段参考实现类。
返回统一的响应模型，包含验证结果和相关信息。

---

## 获取 RSA 公钥
获取用 RSA 公钥,主要用来加密密码等敏感信息,同时提供临时token。

- **请求方法**: `GET`
- **请求路径**: `/api/auth/public-key`

#### 响应示例

```json
{
    "data": {
        "tempToken": "TcoMKl.........SJ797Kli3S1Y",
        "publicKey": "MIIBI...........42ztOcMawIDAQAB"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

---

## 账号登录
用于登录账号。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/login`

#### 请求参数

`captchaVerification`为验证码服务验证成功后返回的验证信息。
`password`使用 public-key 接口返回的公钥进行加密后的密码。

```json
{
  "username": "admin",
  "captchaVerification":"6mRZaI......ZZzAbUL8WHw=", 
  "tempToken":"2Em......htZnPmEc79",
  "password": "IaOD3....kqjVi4lvuXry8XaUAq9FtwmE21/0g=="
}
```

#### 响应示例

```json
{
    "data": {
        "token": "J062mk2fxe......82w34W3E9UbagD"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

```json
{
    "data": null,
    "success": false,
    "errorMsg": "验证码已失效，请重新获取",
    "code": 400
}
```

---

## 获取用户信息
获取用户个人资料信息,要求已经登陆状态才能访问。获取的信息比较详细不适用于公开访问。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/profile`

#### 未授权访问响应
```json
{
    "data": null,
    "success": false,
    "errorMsg": "未授权，请先登录",
    "code": 401
}
```

#### 访问成功响应

```json
{
    "data": {
        "id": 1,
        "username": "example_user",
        "nickname": "Example Nickname",
        "email": "user@example.com",
        "createTime": "2023-01-01T00:00:00",
        "updateTime": "2023-01-01T00:00:00",
        "avatarUrl": "https://example.com/avatar.jpg or null"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

---

## 修改密码
用于修改用户密码。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/update-password`
- **需要登录**: 是

#### 请求参数

```json
{
  "old_password": "加密后的原密码",
  "new_password": "加密后的新密码"
}
```

#### 响应示例

```json
{
    "data": {
        "message": "密码修改成功，请重新登录"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

---

## 账号注销
用于退出登录。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/logout`
- **需要登录**: 是

#### 未登录响应

```json
{
    "data": null,
    "success": false,
    "errorMsg": "用户未登录或会话已过期",
    "code": 401
}
```

#### 成功响应

```json
{
    "data": {
        "message": "登出成功",
        "wasLoggedIn": true,
        "logoutTime": 1770731084416
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

---

## 用户注册
用户注册接口。需先调用 `/api/auth/public-key` 获取公钥和 tempToken，验证码流程与登录相同。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/register`
- **限流**: 60 秒内最多 5 次

#### 请求参数

`password` 使用 public-key 接口返回的公钥进行 RSA 加密。`tempToken` 和 `captchaVerification` 获取方式同登录。

```json
{
  "username": "newUser",
  "email": "user@example.com",
  "password": "加密后的密码",
  "tempToken": "从 public-key 接口获取",
  "captchaVerification": "验证码二次验证返回的值"
}
```

| 字段                  | 类型     | 必填 | 说明          |
|---------------------|--------|----|-------------|
| username            | String | 是  | 用户名，4-20 字符 |
| email               | String | 是  | 邮箱          |
| password            | String | 是  | RSA 加密后的密码  |
| tempToken           | String | 是  | 临时凭证        |
| captchaVerification | String | 是  | 验证码校验信息     |

#### 成功响应

```json
{
    "data": {
        "message": "注册成功，请登录",
        "userId": 123
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

- 用户名已存在：`code: 400`
- 邮箱已被注册：`code: 400`
- 验证码/临时凭证无效：`code: 400`

---

## 权限管理接口

### 分页获取权限列表

获取系统权限列表，支持分页与关键词搜索。权限无状态/内置等维度，响应中 `filterOptions` 为空对象，与用户/角色/权限组列表结构保持一致。

- **请求方法**: `POST`
- **请求路径**: `/api/permission/listAll`
- **需要权限**: `system:permission`

#### 请求参数

```json
{
  "currentPage": 1,
  "pageSize": 10,
  "keyword": "system:user"
}
```

| 字段          | 类型      | 必填 | 说明                              |
|-------------|---------|----|---------------------------------|
| currentPage | Integer | 是  | 当前页码（从 1 开始）                    |
| pageSize    | Integer | 是  | 每页数量                            |
| keyword     | String  | 否  | 搜索关键词（匹配 code、name、description） |

#### 响应示例

```json
{
    "data": {
        "records": [
            {
                "id": 1,
                "code": "system:user:list",
                "name": "用户列表",
                "description": "查看用户列表",
                "sortOrder": 1,
                "createTime": "2026-01-01T00:00:00"
            }
        ],
        "total": 100,
        "size": 10,
        "current": 1,
        "pages": 10,
        "filterOptions": {}
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```