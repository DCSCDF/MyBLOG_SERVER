# Myblog 项目部署与配置指南

## 一、打包

```bash
.\mvnw.cmd package -DskipTests
```

打包完成后，JAR 文件位于 `target/Myblog_dev-0.0.1-SNAPSHOT.jar`

---

## 二、运行

### 方式一：直接运行（使用内置配置）

```bash
java -jar target/Myblog_dev-0.0.1-SNAPSHOT.jar
```

### 方式二：命令行参数覆盖配置

```bash
java -jar target/Myblog_dev-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://新地址:3306/myblog_sql?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai \
  --spring.datasource.username=用户名 \
  --spring.datasource.password=密码 \
  --server.port=8088
```

### 方式三：环境变量

```bash
# Windows
set SPRING_DATASOURCE_URL=jdbc:mysql://新地址:3306/myblog_sql?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai
set SPRING_DATASOURCE_USERNAME=用户名
set SPRING_DATASOURCE_PASSWORD=密码
java -jar target/Myblog_dev-0.0.1-SNAPSHOT.jar
```

### 方式四：外部配置文件（推荐）

在 JAR 包**同目录**下创建 `application.yml`，内容如下：

```yaml
spring:
  datasource:
    url: jdbc:mysql://新地址:3306/myblog_sql?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai
    username: 用户名
    password: 密码
  sql:
    init:
      mode: always
      continue-on-error: true
      platform: mysql

server:
  port: 8088

# Sa-Token
sa-token:
  is-concurrent: false
  timeout: 2592000
  is-share: true
  token-name: token
  is-read-cookie: false
  is-read-header: true
  is-log: false
  token-style: random-128

# CORS
app:
  cors:
    allowed-origins: http://localhost:5173,http://localhost:3000

# 文件上传
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 10MB
```

> **提示**：外部配置文件会覆盖 JAR 内置配置，不需要重新打包即可切换数据库。

---

## 三、配置说明

### 数据库配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.datasource.url` | JDBC 连接地址 | `jdbc:mysql://localhost:3306/myblog_sql` |
| `spring.datasource.username` | 数据库用户名 | `root` |
| `spring.datasource.password` | 数据库密码 | - |
| `spring.datasource.driver-class-name` | 驱动类 | `com.mysql.cj.jdbc.Driver` |

### 服务配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | HTTP 服务端口 | `8088` |
| `spring.application.name` | 应用名称 | `Myblog_dev` |

### Sa-Token 配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `sa-token.is-concurrent` | 是否允许同一账号多处登录 | `false` |
| `sa-token.timeout` | Token 有效期（秒） | `2592000`（30天） |
| `sa-token.token-style` | Token 样式 | `random-128` |

### 文件上传配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.servlet.multipart.max-file-size` | 单文件最大大小 | `10MB` |
| `spring.servlet.multipart.max-request-size` | 单次请求最大大小 | `10MB` |

### 跨域配置

```yaml
app:
  cors:
    allowed-origins: http://localhost:5173,http://localhost:3000
```

多个域名用逗号分隔。

---

## 四、配置文件优先级

Spring Boot 加载配置的优先级（从高到低）：

1. **命令行参数** `--spring.datasource.xxx=xxx`
2. **环境变量** `SPRING_DATASOURCE_XXX=xxx`
3. **JAR 同目录 `config/application.yml`**
4. **JAR 同目录 `application.yml`**
5. **JAR 内置配置**（`BOOT-INF/classes/application.properties`）

---

## 五、后台运行（Windows）

```bash
start /B javaw -jar target/Myblog_dev-0.0.1-SNAPSHOT.jar
```

或使用 [winsw](https://github.com/winsw/winsw)、[NSSM](https://nssm.cc/) 将 JAR 注册为 Windows 服务。

---

## 六、部署检查清单

- [ ] MySQL 数据库已创建（会自动创建 `myblog_sql`）
- [ ] 数据库连接信息正确
- [ ] JAR 文件权限正确（可读可执行）
- [ ] 防火墙开放 `8088` 端口
- [ ] 外部 `application.yml` 放在 JAR 同目录（可选）
