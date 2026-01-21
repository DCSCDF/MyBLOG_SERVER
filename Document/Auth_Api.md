## 用户模块接口文档

---

![img.png](img/img_2.png)

### 前端将 token 提交到后端

将 token 塞到请求header里 ，格式为：`{tokenName: tokenValue}`(tokenName可以在application.properties里面修改，默认为token)。

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

