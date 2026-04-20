## 用户认证模块接口文档

---

### 前端将 token 提交到后端

将 token 塞到请求header里 ，格式为：`{tokenName: tokenValue}`
(tokenName可以在application.properties里面修改，默认为token)。

对于需要登陆的的接口 如果未登录则会有全局拦截器返回401。

---

## 验证码接口 (TianAi-Captcha)
验证码服务接口，用于获取、校验和二次验证行为验证码功能。

基础路径`/api/captcha`

###  获取验证码：
获取验证码信息，用于前端展示滑块 / 旋转 / 滑动还原 / 文字点选验证码图片。

- **请求方法**: `Post`
- **请求路径**: `/api/captcha/get`
- **可选参数**: `type`（不传默认为 `SLIDER`）

### 检查验证码：
校验用户行为轨迹是否满足验证码要求。

- **请求方法**: `POST`
- **请求路径**: `/api/captcha/check`

### 二次验证：
用于在关键操作前再次校验验证码是否有效（需在配置中开启 `captcha.secondary.enabled`）。

- **请求方法**: `GET`
- **请求路径**: `/api/captcha/verify`

#### 参数说明

请求与响应仍然通过项目统一的 `Response` 包装返回，内部使用 Tianai-Captcha 的数据结构。前端只需根据 `success`、`code`、`errorMsg` 判断结果，不需要直接依赖底层库类型。 

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
`oauthEnabled`为可选参数，用于控制是否启用外部授权模式：
- `false`或不传：正常登录，直接返回 token
- `true`：外部授权模式，登录成功返回一次性授权码(code)，需通过 `/api/auth/oauth/token` 接口换取 token

```json
{
  "username": "admin",
  "captchaVerification":"6mRZaI......ZZzAbUL8WHw=",
  "tempToken":"2Em......htZnPmEc79",
  "password": "IaOD3....kqjVi4lvuXry8XaUAq9FtwmE21/0g==",
  "oauthEnabled": false
}
```

#### 响应示例（正常模式）

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

#### 响应示例（外部授权模式 oauthEnabled=true）

```json
{
    "data": {
        "code": "AbCdEfGhIjKlMnOpQrStUvWxYz012345",
        "expiresIn": 300,
        "token": "J062mk2fxe......82w34W3E9UbagD"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

> 注意：外部授权模式下 code 有效期 5 分钟，只能使用一次。token 可直接使用，code 可用于外部系统换取新 token。

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

## OAuth 授权码换取 Token
用于外部授权模式下，使用授权码换取正式的登录 token。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/oauth/token`
- **限流**: 60 秒内最多 10 次

#### 请求参数

`code` 为登录接口返回的一次性授权码，有效期 5 分钟，只能使用一次。

```json
{
  "code": "AbCdEfGhIjKlMnOpQrStUvWxYz012345"
}
```

| 字段   | 类型     | 必填 | 说明                |
|------|--------|----|-------------------|
| code | String | 是  | 授权码，登录接口返回，有效期5分钟 |

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

- 授权码为空：`code: 400`
- 授权码无效或已过期：`code: 400`

```json
{
    "data": null,
    "success": false,
    "errorMsg": "授权码无效或已过期",
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

> **注意**：注册方式由系统配置 `reg.use-email` 控制：
> - `false`（默认）：直接注册，无需邮箱验证
> - `true`：启用邮箱验证码注册，需先调用 `/api/auth/register/code` 获取验证码，再调用 `/api/auth/register/confirm` 完成注册

- **请求方法**: `POST`
- **请求路径**: `/api/auth/register`
- **限流**: 60 秒内最多 6 次

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
- 已启用邮箱验证注册：`code: 400`（提示使用 `/api/auth/register/code` 接口）

---

## 请求发送注册验证码
当系统配置 `reg.use-email=true` 时使用此接口发送注册验证码到邮箱。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/register/code`
- **限流**: 60 秒内最多 6 次

#### 请求参数

`password` 使用 public-key 接口返回的公钥进行 RSA 加密。

```json
{
  "username": "newUser",
  "email": "user@example.com",
  "password": "加密后的密码",
  "tempToken": "从 public-key 接口获取",
  "captchaVerification": "验证码二次验证返回的值"
}
```

| 字段                  | 类型     | 必填 | 说明              |
|---------------------|--------|----|-----------------|
| username            | String | 是  | 用户名，4-20 字符   |
| email               | String | 是  | 邮箱              |
| password            | String | 是  | RSA 加密后的密码     |
| tempToken           | String | 是  | 临时凭证            |
| captchaVerification | String | 是  | 验证码校验信息       |

#### 成功响应

```json
{
    "data": {
        "message": "验证码已发送到您的邮箱，请查收",
        "email": "u***r@example.com",
        "expiresIn": 300
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

| 字段      | 类型    | 说明              |
|---------|-------|-----------------|
| message | String | 提示信息          |
| email  | String | 掩码后的邮箱地址     |
| expiresIn | Integer | 验证码有效期（秒）   |

#### 错误响应

- 用户名已存在：`code: 400`
- 邮箱已被注册：`code: 400`
- 验证码尚在有效期内：`code: 400`（提示剩余等待秒数）
- SMTP 配置未完成：`code: 400`
- 验证码/临时凭证无效：`code: 400`

---

## 确认注册（验证邮箱验证码）
当系统配置 `reg.use-email=true` 时使用此接口完成注册。用户需提供邮箱和收到的验证码。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/register/confirm`
- **限流**: 60 秒内最多 6 次

#### 请求参数

```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

| 字段   | 类型     | 必填 | 说明      |
|------|--------|----|---------|
| email | String | 是  | 注册邮箱   |
| code  | String | 是  | 6位验证码 |

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

- 验证码为空：`code: 400`
- 验证码无效：`code: 400`（提示重新获取）
- 验证码已过期：`code: 400`
- 验证码错误：`code: 400`

> **注册流程说明**：
> 1. 调用 `/api/auth/public-key` 获取公钥和 tempToken
> 2. 调用 `/api/captcha/get` 获取验证码图片
> 3. 调用 `/api/captcha/check` 校验行为轨迹
> 4. 调用 `/api/auth/register/code` 发送注册验证码（需通过 `/api/captcha/verify` 二次验证）
> 5. 检查邮箱收取验证码
> 6. 调用 `/api/auth/register/confirm` 完成注册

> **注意**：验证码有效期为 5 分钟，同一邮箱在验证码失效前无法重复获取。

---

## 修改昵称
用于修改用户昵称。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/update-nickname`
- **需要登录**: 是
- **限流**: 60 秒内最多 10 次

#### 请求参数

```json
{
  "nickname": "新昵称"
}
```

| 字段       | 类型     | 必填 | 说明          |
|----------|--------|----|-------------|
| nickname | String | 是  | 昵称，长度1-50字符 |

#### 响应示例

```json
{
    "data": {
        "message": "昵称修改成功"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

- 昵称为空：`code: 400`
- 昵称长度超过限制：`code: 400`
- 用户不存在：`code: 400`

---

## 修改头像URL
用于修改用户头像URL。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/update-avatar-url`
- **需要登录**: 是
- **限流**: 60 秒内最多 10 次

#### 请求参数

```json
{
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

| 字段        | 类型     | 必填 | 说明                                     |
|-----------|--------|----|----------------------------------------|
| avatarUrl | String | 否  | 头像URL，必须是有效的 http/https 链接，传空字符串表示清空头像 |

#### 响应示例

```json
{
    "data": {
        "message": "头像URL修改成功"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 清空头像响应

```json
{
    "data": {
        "message": "头像已清空"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

- 头像URL格式无效：`code: 400`
- 用户不存在：`code: 400`

---

## 修改邮箱（直接模式）
当系统配置 `reg.use-email=false` 时使用此接口直接修改邮箱，无需验证码。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/update-email`
- **需要登录**: 是
- **限流**: 60 秒内最多 6 次

#### 请求参数

```json
{
  "email": "newemail@example.com"
}
```

| 字段  | 类型     | 必填 | 说明      |
|-----|--------|----|---------|
| email | String | 是  | 新邮箱地址 |

#### 响应示例

```json
{
    "data": {
        "message": "邮箱修改成功"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

- 邮箱为空：`code: 400`
- 邮箱格式不正确：`code: 400`
- 邮箱已被注册：`code: 400`
- 用户不存在：`code: 400`
- 已启用邮箱验证模式：`code: 400`（提示使用 `/api/auth/change-email/code` 接口）

---

## 请求发送邮箱变更验证码
当系统配置 `reg.use-email=true` 时使用此接口发送邮箱变更验证码到新邮箱。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/change-email/code`
- **需要登录**: 是
- **限流**: 60 秒内最多 6 次

#### 请求参数

```json
{
  "email": "newemail@example.com"
}
```

| 字段  | 类型     | 必填 | 说明      |
|-----|--------|----|---------|
| email | String | 是  | 新邮箱地址 |

#### 成功响应

```json
{
    "data": {
        "message": "验证码已发送到您的新邮箱，请查收",
        "email": "n***w@example.com",
        "expiresIn": 300
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

| 字段      | 类型    | 说明              |
|---------|-------|-----------------|
| message | String | 提示信息          |
| email  | String | 掩码后的邮箱地址     |
| expiresIn | Integer | 验证码有效期（秒）   |

#### 错误响应

- 邮箱已被注册：`code: 400`
- 验证码尚在有效期内：`code: 400`（提示剩余等待秒数）
- SMTP 配置未完成：`code: 400`

---

## 确认邮箱变更（验证邮箱验证码）
当系统配置 `reg.use-email=true` 时使用此接口完成邮箱更换。用户需提供新邮箱和收到的验证码。

- **请求方法**: `POST`
- **请求路径**: `/api/auth/change-email/confirm`
- **需要登录**: 是
- **限流**: 60 秒内最多 6 次

#### 请求参数

```json
{
  "email": "newemail@example.com",
  "code": "123456"
}
```

| 字段   | 类型     | 必填 | 说明      |
|------|--------|----|---------|
| email | String | 是  | 新邮箱   |
| code  | String | 是  | 6位验证码 |

#### 成功响应

```json
{
    "data": {
        "message": "邮箱修改成功"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

- 验证码为空：`code: 400`
- 验证码无效：`code: 400`（提示重新获取）
- 验证码已过期：`code: 400`
- 验证码错误：`code: 400`
- 邮箱不匹配：`code: 400`

> **邮箱变更流程说明（reg.use-email=true 时）**：
> 1. 调用 `/api/auth/change-email/code` 发送验证码到新邮箱
> 2. 检查新邮箱收取验证码
> 3. 调用 `/api/auth/change-email/confirm` 完成邮箱更换

> **注意**：验证码有效期为 5 分钟，同一用户在新验证码失效前无法重复获取。

---

## 找回密码

用户找回密码接口。当系统配置 `reg.use-email=true` 时可用。

> **功能说明**：
> - 用户可以通过用户名或邮箱匹配到对应的账户
> - 如果匹配到账户且账户绑定了邮箱，则向该邮箱发送6位验证码
> - 用户收到验证码后，通过验证码和新密码完成密码重置
> - 未找到用户或用户未绑定邮箱时返回统一的错误信息（防止用户枚举攻击）

- **请求方法**: `POST`
- **请求路径**: `/api/auth/find-password/code`
- **限流**: 60 秒内最多 6 次

### 请求发送找回密码验证码

#### 请求参数

```json
{
  "usernameOrEmail": "用户名或邮箱",
  "tempToken": "从 public-key 接口获取",
  "captchaVerification": "验证码二次验证返回的值"
}
```

| 字段                  | 类型     | 必填 | 说明              |
|---------------------|--------|----|-----------------|
| usernameOrEmail     | String | 是  | 用户名或邮箱地址     |
| tempToken           | String | 是  | 临时凭证           |
| captchaVerification | String | 是  | 验证码校验信息       |

#### 成功响应

```json
{
    "data": {
        "message": "验证码已发送到您的邮箱，请查收",
        "email": "u***r@example.com",
        "expiresIn": 300
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

| 字段      | 类型    | 说明              |
|---------|-------|-----------------|
| message | String | 提示信息          |
| email  | String | 掩码后的邮箱地址     |
| expiresIn | Integer | 验证码有效期（秒）   |

#### 错误响应

- 未找到对应的用户或用户未绑定邮箱：`code: 400`（统一错误信息，防止枚举攻击）
- 验证码尚在有效期内：`code: 400`（提示剩余等待秒数）
- SMTP 配置未完成：`code: 400`
- 验证码/临时凭证无效：`code: 400`
- 该功能未启用：`code: 400`（当 `reg.use-email=false` 时）

---

### 确认找回密码（重置密码）

- **请求方法**: `POST`
- **请求路径**: `/api/auth/find-password/confirm`
- **限流**: 60 秒内最多 6 次

#### 请求参数

`newPassword` 使用 public-key 接口返回的公钥进行 RSA 加密。

```json
{
  "usernameOrEmail": "用户名或邮箱",
  "code": "123456",
  "newPassword": "加密后的新密码"
}
```

| 字段              | 类型     | 必填 | 说明              |
|-----------------|--------|----|-----------------|
| usernameOrEmail | String | 是  | 用户名或邮箱地址     |
| code            | String | 是  | 6位验证码          |
| newPassword     | String | 是  | RSA 加密后的新密码   |

#### 成功响应

```json
{
    "data": {
        "message": "密码重置成功，请使用新密码登录"
    },
    "success": true,
    "errorMsg": null,
    "code": 200
}
```

#### 错误响应

- 未找到对应的用户或用户未绑定邮箱：`code: 400`
- 验证码为空：`code: 400`
- 验证码无效：`code: 400`（提示重新获取）
- 验证码已过期：`code: 400`
- 验证码错误：`code: 400`
- 密码格式错误：`code: 400`
- 密码格式不符合要求：`code: 400`
- 该功能未启用：`code: 400`（当 `reg.use-email=false` 时）

> **找回密码流程说明**：
> 1. 调用 `/api/auth/public-key` 获取公钥和 tempToken
> 2. 调用 `/api/captcha/get` 获取验证码图片
> 3. 调用 `/api/captcha/check` 校验行为轨迹
> 4. 调用 `/api/auth/find-password/code` 发送找回密码验证码（需通过 `/api/captcha/verify` 二次验证）
> 5. 检查邮箱收取验证码
> 6. 调用 `/api/auth/find-password/confirm` 完成密码重置

> **注意**：
> - 验证码有效期为 5 分钟，同一邮箱在验证码失效前无法重复获取
> - 密码格式要求：6-20位，必须包含字母和数字，不能包含空格、引号、分号等特殊字符

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