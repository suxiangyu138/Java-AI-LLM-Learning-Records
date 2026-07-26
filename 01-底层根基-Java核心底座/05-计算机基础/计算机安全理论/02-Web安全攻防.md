# Web 安全攻防
> 以 OWASP Top 10 为框架，结合 Java 后端实战，详解 Web 漏洞原理、攻击方式与防御措施，覆盖从漏洞原理到安全编码实践的全链路知识。

## 目录

1. [OWASP Top 10 概览](#1-owasp-top-10-概览)
2. [SQL 注入](#2-sql-注入)
3. [跨站脚本 XSS](#3-跨站脚本-xss)
4. [跨站请求伪造 CSRF](#4-跨站请求伪造-csrf)
5. [服务端请求伪造 SSRF](#5-服务端请求伪造-ssrf)
6. [认证与会话安全](#6-认证与会话安全)
7. [文件上传漏洞](#7-文件上传漏洞)
8. [不安全的反序列化](#8-不安全的反序列化)
9. [其他高频漏洞](#9-其他高频漏洞)
10. [输入安全](#10-输入安全)
11. [输出安全](#11-输出安全)
12. [数据存储安全](#12-数据存储安全)
13. [日志安全](#13-日志安全)
14. [依赖与配置安全](#14-依赖与配置安全)
15. [API 安全清单](#15-api-安全清单)

---

## 1. OWASP Top 10 概览

| 排名 | 漏洞 | 核心风险 |
|------|------|---------|
| A01 | 访问控制失效 | 越权访问 |
| A02 | 加密机制失效 | 数据泄露 |
| A03 | **注入攻击** | 任意命令执行 |
| A04 | 不安全设计 | 架构缺陷 |
| A05 | 安全配置错误 | 默认密码、错误页泄露信息 |
| A06 | 脆弱和过时的组件 | Log4Shell |
| A07 | 认证和授权失败 | 弱密码、Session 劫持 |
| A08 | 软件和数据完整性失效 | CI/CD 投毒 |
| A09 | 安全日志和监控失效 | 被攻击了不知道 |
| A10 | **SSRF** | 内网探测 |

---

## 2. SQL 注入

> 攻击者将恶意 SQL 拼接到查询中，获取/篡改/删除数据。

### 攻击示例

```java
// 危险写法：字符串拼接
String sql = "SELECT * FROM users WHERE name = '" + username + "'";
// username = "' OR '1'='1' --"
// -> SELECT * FROM users WHERE name = '' OR '1'='1' --'
// -> 返回全部用户！
```

### 防御措施

```java
// 参数化查询（PreparedStatement）
PreparedStatement ps = conn.prepareStatement(
    "SELECT * FROM users WHERE name = ?");
ps.setString(1, username);
```

```xml
<!-- MyBatis #{} vs ${} -->
<!-- #{} -> 参数化，安全；${} -> 直接拼接，危险 -->

<!-- 安全 -->
<select id="findUser" parameterType="Long" resultType="User">
    SELECT * FROM user WHERE id = #{id}
</select>

<!-- 危险：仅用于动态表名/排序字段，且必须白名单校验 -->
<select id="findUserByOrder" parameterType="String" resultType="User">
    SELECT * FROM user ORDER BY ${orderBy}
</select>
```

| 防御方式 | 说明 | 优先级 |
|----------|------|--------|
| **参数化查询（PreparedStatement）** | 将用户输入作为参数传入 SQL，驱动自动转义 | 首选 |
| **ORM 框架** | MyBatis、Hibernate 默认参数化查询；注意避免 `${}` 拼接 | 推荐 |
| **输入校验与过滤** | 类型校验、长度限制、特殊字符过滤 | 辅助 |

### 其他注入类漏洞

| 漏洞类型 | 防御措施 |
|----------|----------|
| **命令注入** | 避免使用 `Runtime.getRuntime().exec()` 执行系统命令；必须使用时用 `ProcessBuilder` 传参数列表 |
| **XXE（XML 外部实体）** | 禁用外部实体解析；优先使用 JSON 替代 XML |

```xml
<!-- XXE 攻击 payload：读取服务器文件 -->
<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<root>&xxe;</root>
```

```java
// Java 防御 XXE
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
```

---

## 3. 跨站脚本 XSS

> 恶意脚本注入网页，在受害者浏览器中执行。

### 3.1 三种类型

| 类型 | 攻击方式 | 持久性 | 危害 |
|------|---------|--------|------|
| **反射型** | URL 参数带恶意脚本，服务端反射回页面 | 一次性 | 需要诱导点击 |
| **存储型** | 恶意脚本存入数据库，每次浏览触发 | 持久 | 影响所有用户 |
| **DOM 型** | 纯前端漏洞，JS 动态拼接 HTML | 一次性 | 前端代码 bug |

### 3.2 防御措施

```java
// 输出编码（最重要）
// JSP/Thymeleaf 默认转义 HTML：< -> &lt;  > -> &gt;  " -> &quot;

// 内容安全策略（CSP）HTTP Header
// Content-Security-Policy: default-src 'self'; script-src 'self'

// HttpOnly Cookie -> JS 不可读，防 cookie 窃取
response.setHeader("Set-Cookie", "session=xxx; HttpOnly; Secure; SameSite=Strict");

// 输入校验（白名单）
String clean = Jsoup.clean(userInput, Safelist.basic());
```

| 防御维度 | 实现方式 |
|----------|----------|
| **输入过滤** | Spring 内置 HtmlUtils 转义 HTML 特殊字符；OWASP AntiSamy 过滤富文本恶意脚本 |
| **输出转义** | 后端返回前端的用户输入数据必须 HTML 转义；JSON 确保特殊字符正确转义 |
| **Cookie 安全** | `HttpOnly` 禁止 JS 读 Cookie；`Secure` 仅允许 HTTPS 传输 |

---

## 4. 跨站请求伪造 CSRF

> 诱导用户在已登录的网站上执行非预期的操作。

### 攻击流程

```
1. 用户登录 bank.com（Cookie 有效）
2. 用户访问 attacker.com（钓鱼邮件中的链接）
3. attacker.com 自动提交 form 到 bank.com/transfer
4. 浏览器自动携带 bank.com 的 Cookie -> 转账成功
```

### 防御方案

| 方案 | 做法 | 说明 |
|------|------|------|
| **CSRF Token** | 表单中嵌入随机 token，服务端校验 | 最经典方案 |
| **SameSite Cookie** | `Set-Cookie: SameSite=Strict/Lax` | 现代浏览器支持 |
| **Referer/Origin 校验** | 检查请求来源是否白名单域名 | 可被伪造 |
| **双重 Cookie** | Cookie 和 Header 中各一份相同值 | 跨域请求无法读 Cookie |

```java
// Spring Security 默认开启 CSRF 保护
http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
```

---

## 5. 服务端请求伪造 SSRF

> 攻击者诱导服务器向内网发起请求，绕过防火墙访问内部服务。

### 攻击示例

```
// 图片代理功能：用户传 URL，服务器抓取
// 攻击者传：http://169.254.169.254/latest/meta-data/（AWS 元数据）
// 结果：泄露 AWS 临时凭证！
```

### 防御措施

- 白名单域名/IP + 禁止内网 IP（10.x、172.16-31.x、192.168.x、127.x）
- 禁用不必要协议（`file://`、`gopher://`、`dict://`）
- 限制跳转（30x 重定向后也需检查目标地址）
- 使用独立出口代理，隔离内网

---

## 6. 认证与会话安全

### 6.1 常见问题与防御

| 问题 | 防御 |
|------|------|
| 弱密码 | 密码复杂度要求 + bcrypt/Argon2 哈希存储 |
| 暴力破解 | 登录失败次数限制 + 验证码 + IP 限流 |
| Session 固定 | 登录成功后重新生成 Session ID |
| Cookie 窃取 | HttpOnly + Secure + SameSite |
| 密码明文传输 | 全站 HTTPS（HSTS） |

### 6.2 JWT 安全注意事项

```java
// 错误做法
// 1. 用对称算法 HS256，密钥写死在代码里 -> 谁都能伪造
// 2. 过期时间设为 30 天 -> Token 泄露后可长期利用
// 3. 不校验签名 -> 直接 parse，攻击者可篡改 payload

// 最佳实践
// 1. 用 RS256（非对称）或至少 HS256 + 强密钥
// 2. Access Token 15 min + Refresh Token 7 day
// 3. 必须验证签名 + 过期时间 + issuer
// 4. 敏感操作不走无状态 JWT，回源鉴权
```

---

## 7. 文件上传漏洞

```java
// 危险：不做任何校验
MultipartFile file = request.getFile("avatar");
file.transferTo(new File("/uploads/" + file.getOriginalFilename()));
// 攻击者上传 shell.jsp -> 服务器端执行恶意代码

// 安全：多层校验
// 1. 白名单校验扩展名
String ext = FilenameUtils.getExtension(filename);
if (!List.of("jpg","png","gif","pdf").contains(ext.toLowerCase())) {
    throw new SecurityException("不允许的文件类型");
}
// 2. 校验文件真实类型（Magic Number，非扩展名）
// 3. 随机重命名，不保留原始文件名
String newName = UUID.randomUUID().toString() + "." + ext;
// 4. 限制文件大小
// 5. 上传目录与代码部署目录分离（甚至用 OSS）
// 6. 禁止上传目录执行脚本（Nginx/Apache 配置）
```

---

## 8. 不安全的反序列化

```java
// 危险：Java 原生反序列化未做类型检查
ObjectInputStream ois = new ObjectInputStream(inputStream);
Object obj = ois.readObject(); // 可能触发恶意类的 readObject()

// 方案1：不用 Java 原生序列化，改用 JSON/Protobuf
// 方案2：白名单限制可反序列化的类
// 方案3：使用安全的库（Jackson 的 enableDefaultTyping 也要谨慎）
```

> Log4Shell（CVE-2021-44228）本质上也是反序列化 + JNDI 注入的组合攻击。

---

## 9. 其他高频漏洞

### 9.1 越权漏洞（IDOR）

```java
// 错误：不检查数据归属
@GetMapping("/order/{id}")
public Order getOrder(@PathVariable Long id) {
    return orderService.getById(id); // 用户 A 能查到用户 B 的订单
}

// 正确：校验数据归属
Order order = orderService.getById(id);
if (!order.getUserId().equals(currentUserId)) {
    throw new AccessDeniedException("无权访问");
}
```

### 9.2 敏感信息泄露

| 泄露点 | 防御 |
|--------|------|
| 错误页面暴露堆栈/版本 | 统一错误页面，不显示细节 |
| API 返回多余字段 | DTO 只返回必需字段 |
| Git 泄露 .git 目录 | Nginx 禁止访问 `.git` |
| 日志打印敏感数据 | 日志脱敏（手机号/身份证/密码掩码） |

---

## 10. 输入安全

> **第一原则：永远不要信任用户输入。** 所有从外部来的数据——URL 参数、Header、Cookie、上传文件、第三方 API 返回——都必须校验。

### 10.1 通用校验清单

```java
// 类型校验、长度限制、范围限制、格式校验（正则白名单）、业务逻辑校验

// 示例：用户注册
public class RegisterRequest {
    @NotBlank @Size(min=3, max=20)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")  // 白名单字符
    private String username;

    @NotBlank @Size(min=8, max=128)
    private String password;

    @Email @Size(max=100)
    private String email;

    @Min(0) @Max(150)
    private Integer age;

    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;
}
```

### 10.2 特殊字符处理

```java
// 场景：动态排序字段 -> 必须白名单
private static final Set<String> ALLOWED_SORT_FIELDS =
    Set.of("id", "username", "create_time", "update_time");

public List<User> getUsersByOrder(String orderBy, String direction) {
    if (orderBy == null || !ALLOWED_SORT_FIELDS.contains(orderBy)) {
        throw new IllegalArgumentException("Invalid sort field: " + orderBy);
    }
    if (!"ASC".equalsIgnoreCase(direction) && !"DESC".equalsIgnoreCase(direction)) {
        throw new IllegalArgumentException("Invalid direction: " + direction);
    }
    // 安全：orderBy 和 direction 都经过白名单校验
}
```

---

## 11. 输出安全

### 11.1 API 响应：最小信息原则

```java
// 错误：返回了整个 Entity
@GetMapping("/user/{id}")
public User getUser(@PathVariable Long id) {
    return userMapper.selectById(id);
    // 返回了 password_hash, salt, is_deleted 等字段！
}

// 正确：使用 DTO，只返回必需字段
@GetMapping("/user/{id}")
public UserDTO getUser(@PathVariable Long id) {
    User user = userMapper.selectById(id);
    return UserDTO.from(user);  // 只包含 id, username, avatar, bio
}
```

### 11.2 错误信息脱敏

```java
// 错误：生产环境暴露细节
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handle(Exception e) {
    return ResponseEntity.status(500).body(e.getMessage());
    // "Table 'xxx.users' doesn't exist" -> 暴露表结构
    // "Connection refused to 10.0.1.5:3306" -> 暴露内网 IP
}

// 正确：统一错误响应，隐藏细节
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handle(Exception e) {
    log.error("Internal error, traceId: {}", traceId, e);  // 日志记录详细信息
    return ResponseEntity.status(500)
        .body(new ErrorResponse("INTERNAL_ERROR", "服务内部错误，请稍后重试"));
}
```

### 11.3 防止信息泄露的响应头

```java
response.setHeader("Server", "");            // 不返回 Nginx/Apache 版本
response.setHeader("X-Powered-By", "");      // 不返回框架版本
response.setHeader("X-Application-Context", ""); // Spring Boot 默认返回
```

---

## 12. 数据存储安全

### 12.1 密码存储

```java
// 绝对禁止
String hash = md5(password);           // MD5 太弱
String hash = sha256(password);        // 无 salt，可彩虹表攻击

// 生产标准
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode(password);
// cost factor = 10 -> 2^10 = 1024 次迭代，约 0.1s/次
// 对于攻击者，0.1s x 100亿次试错 = 不可行
```

### 12.2 敏感配置加密

```yaml
# 错误：敏感信息明文写在配置中
spring.datasource.password=MyPassword123
jwt.secret=my-secret-key

# 方案1：环境变量（Docker/K8s）
spring.datasource.password=${DB_PASSWORD}

# 方案2：配置中心加密（Nacos/Apollo 支持加密存储）
spring.datasource.password=ENC(xxxxxx)

# 方案3：Vault / KMS（云服务密钥管理）
```

### 12.3 数据库操作安全

```java
// 最小权限原则：应用 DB 账号只有业务所需权限
// GRANT SELECT, INSERT, UPDATE, DELETE ON mydb.* TO 'app'@'%';
// 不给 DROP, ALTER, CREATE 权限

// 敏感字段加密存储
// 身份证、手机号等用 AES 加密存储（密钥放 KMS）
@TableField(typeHandler = EncryptedTypeHandler.class)
private String idCard;

// 逻辑删除代替物理删除
@TableLogic
private Integer isDeleted;
```

---

## 13. 日志安全

```java
// 危险：日志泄露敏感信息
log.info("用户登录: username={}, password={}", username, password);
log.info("支付请求: cardNo={}, cvv={}", cardNo, cvv);

// 安全：日志脱敏
log.info("用户登录: username={}, password=***", username);
log.info("支付请求: cardNo={}****, cvv=***", cardNo.substring(0, 4));

// 关键操作审计日志
auditLog.info("操作:删除用户, operator:{}, targetId:{}, ip:{}, time:{}",
    currentUser, targetUserId, requestIP, LocalDateTime.now());
```

### 日志级别与安全

| 级别 | 使用场景 | 安全注意 |
|------|---------|---------|
| **ERROR** | 系统错误 | 记录详细信息用于排查，不记录敏感参数 |
| **WARN** | 潜在风险（如频繁登录失败） | 可记录用户名，不记录密码 |
| **INFO** | 关键业务节点（下单、支付） | 审计日志，不可记录敏感字段 |
| **DEBUG** | 开发调试 | 生产环境必须关闭 |

---

## 14. 依赖与配置安全

### 14.1 依赖管理

```xml
<!-- 定期扫描漏洞 -->
<!-- Maven: mvn dependency-check:check -->
<!-- Gradle: gradle dependencyCheckAnalyze -->
<!-- OWASP Dependency-Check 插件 -->

<!-- 锁定版本，避免自动升级引入风险 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>  <!-- 不用版本范围 -->
</dependency>

<!-- 排除不需要的传递依赖 -->
<exclusions>
    <exclusion>
        <groupId>commons-logging</groupId>
        <artifactId>commons-logging</artifactId>
    </exclusion>
</exclusions>
```

### 14.2 Spring Boot 安全配置

```yaml
# application.yml 安全配置

spring:
  # 关闭 actuator 敏感端点
  # management.endpoints.web.exposure.include=health,info

  # Session Cookie 安全
  server:
    servlet:
      session:
        cookie:
          http-only: true     # JS 不可读
          secure: true        # 仅 HTTPS 传输
          same-site: strict   # 防 CSRF

# 自定义错误页面
server:
  error:
    whitelabel:
      enabled: false

# 限制文件上传
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB
```

---

## 15. API 安全清单

### 上线安全检查清单

```
  所有接口都有认证（除非明确公开）
  所有接口都有授权校验（不能越权访问别人的数据）
  输入参数都有校验（@Valid / 类型 / 长度 / 格式 / 范围）
  SQL 查询都用参数化（PreparedStatement / MyBatis #{}）
  错误响应不泄露堆栈/版本/内网信息
  日志中不打印密码/Token/验证码/身份证/银行卡号
  Cookie 设置了 HttpOnly + Secure + SameSite
  全站 HTTPS，配置了 HSTS
  文件上传有类型/大小/内容校验
  敏感配置不在代码/配置文件中（用环境变量/KMS）
  第三方依赖无已知严重漏洞（OWASP Dependency-Check）
  关键操作有审计日志（不可删除）
  CORS 只允许信任域名（不用 *）
  接口有频率限制（防暴力攻击/爬虫）
```

---

## 核心要点速记

1. **SQL 注入**：永远不用字符串拼接 SQL -> PreparedStatement / #{}
2. **XSS**：输出编码 + HttpOnly Cookie + CSP
3. **CSRF**：CSRF Token + SameSite Cookie
4. **SSRF**：禁止内网 IP + 白名单 + 禁用危险协议
5. **文件上传**：白名单扩展名 + 校验真实类型 + 随机文件名 + 非执行目录
6. **越权**：每个接口校验数据归属，不做"信任前端"
7. **输入**：永远不信任 -> 校验（类型/长度/范围/格式/业务逻辑）
8. **输出**：最小信息 -> DTO + 错误脱敏 + 响应头清理
9. **存储**：密码 bcrypt + 配置 KMS + 日志脱敏 + 数据库最小权限
10. **依赖**：定期扫描（OWASP DC）+ 锁定版本 + 排除不安全传递依赖
