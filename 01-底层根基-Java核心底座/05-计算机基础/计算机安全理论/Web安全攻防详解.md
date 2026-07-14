# Web安全攻防详解

> Web安全是后端开发的底线。本文以 OWASP Top 10 为框架，结合 Java 后端开发实战，详解最常见的 Web 漏洞原理、攻击方式与防御措施。

## 📑 目录

- [一、OWASP Top 10 概览](#一owasp-top-10-概览)
- [二、注入攻击（Injection）](#二注入攻击injection)
- [三、跨站脚本（XSS）](#三跨站脚本xss)
- [四、跨站请求伪造（CSRF）](#四跨站请求伪造csrf)
- [五、服务端请求伪造（SSRF）](#五服务端请求伪造ssrf)
- [六、认证与会话安全](#六认证与会话安全)
- [七、文件上传漏洞](#七文件上传漏洞)
- [八、不安全的反序列化](#八不安全的反序列化)
- [九、其他高频漏洞](#九其他高频漏洞)
- [十、面试高频问题](#十面试高频问题)

---

## 一、OWASP Top 10 概览

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

## 二、注入攻击（Injection）

### 2.1 SQL 注入

> 攻击者将恶意 SQL 拼接到查询中，获取/篡改/删除数据。

**攻击示例**：
```java
// ❌ 危险写法：字符串拼接
String sql = "SELECT * FROM users WHERE name = '" + username + "'";
// username = "' OR '1'='1' --"
// → SELECT * FROM users WHERE name = '' OR '1'='1' --'
// → 返回全部用户！
```

**防御**：
```java
// ✅ 参数化查询（PreparedStatement）
PreparedStatement ps = conn.prepareStatement(
    "SELECT * FROM users WHERE name = ?");
ps.setString(1, username);

// ✅ ORM 框架（MyBatis #{} vs ${}）
// #{username} → 参数化，安全
// ${username} → 直接拼接，危险！（除非白名单校验）
@Select("SELECT * FROM users WHERE name = #{username}")
User findByName(String username);
```

### 2.2 XXE（XML 外部实体注入）

```xml
<!-- 攻击 payload：读取服务器文件 -->
<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<root>&xxe;</root>
```

**Java 防御**：
```java
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
```

### 2.3 命令注入

```java
// ❌ 危险
Runtime.getRuntime().exec("ping " + userInput);
// userInput = "127.0.0.1; rm -rf /"

// ✅ 安全：用 ProcessBuilder，参数用列表传，不用字符串拼接
new ProcessBuilder("ping", userInput).start();
```

---

## 三、跨站脚本（XSS）

> 恶意脚本注入网页，在受害者浏览器中执行。

### 3.1 三种类型

| 类型 | 攻击方式 | 持久性 | 危害 |
|------|---------|--------|------|
| **反射型** | URL 参数带恶意脚本，服务端反射回页面 | 一次性 | 需要诱导点击 |
| **存储型** | 恶意脚本存入数据库，每次浏览触发 | 持久 | 影响所有用户 |
| **DOM 型** | 纯前端漏洞，JS 动态拼接 HTML | 一次性 | 前端代码 bug |

### 3.2 防御措施

```java
// ① 输出编码（最重要）
// JSP/Thymeleaf 默认转义 HTML：
// < → &lt;  > → &gt;  " → &quot;

// ② 内容安全策略（CSP）HTTP Header
// Content-Security-Policy: default-src 'self'; script-src 'self'

// ③ HttpOnly Cookie → JS 不可读，防 cookie 窃取
response.setHeader("Set-Cookie", "session=xxx; HttpOnly; Secure; SameSite=Strict");

// ④ 输入校验（白名单）
String clean = Jsoup.clean(userInput, Safelist.basic());
```

---

## 四、跨站请求伪造（CSRF）

> 诱导用户在已登录的网站上执行非预期的操作。

**攻击流程**：
```
1. 用户登录 bank.com（Cookie 有效）
2. 用户访问 attacker.com（钓鱼邮件中的链接）
3. attacker.com 的页面自动提交 form 到 bank.com/transfer
4. 浏览器自动携带 bank.com 的 Cookie → 转账成功
```

**防御**：

| 方案 | 做法 | 说明 |
|------|------|------|
| **CSRF Token** | 表单中嵌入随机 token，服务端校验 | 最经典方案 |
| **SameSite Cookie** | `Set-Cookie: SameSite=Strict/Lax` | 现代浏览器支持 |
| **Referer/Origin 校验** | 检查请求来源是否白名单域名 | 可被伪造（旧浏览器） |
| **双重 Cookie** | Cookie 和 Header 中各一份相同值 | 跨域请求无法读 Cookie |

```java
// Spring Security 默认开启 CSRF 保护
http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
```

---

## 五、服务端请求伪造（SSRF）

> 攻击者诱导服务器向内网发起请求，绕过防火墙访问内部服务。

**攻击示例**：
```
// 图片代理功能：用户传 URL，服务器抓取
// 攻击者传：http://169.254.169.254/latest/meta-data/ （AWS 元数据）
// 结果：泄露 AWS 临时凭证！
```

**防御**：
- 白名单域名/IP + 禁止内网 IP（10.x、172.16-31.x、192.168.x、127.x）
- 禁用不必要协议（`file://`、`gopher://`、`dict://`）
- 限制跳转（30x 重定向后也需检查）
- 使用独立出口代理，隔离内网

---

## 六、认证与会话安全

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
// ❌ 常见错误
// 1. 用对称算法 HS256，密钥写死在代码里 → 谁都能伪造
// 2. 过期时间设为 30 天 → Token 泄露后可长期利用
// 3. 不校验签名 → 直接 parse，攻击者可篡改 payload

// ✅ 最佳实践
// 1. 用 RS256（非对称）或至少 HS256 + 强密钥
// 2. Access Token 15 min + Refresh Token 7 day
// 3. 必须验证签名 + 过期时间 + issuer
// 4. 敏感操作不走无状态 JWT，回源鉴权
```

---

## 七、文件上传漏洞

```java
// ❌ 危险：不做任何校验
MultipartFile file = request.getFile("avatar");
file.transferTo(new File("/uploads/" + file.getOriginalFilename()));
// 攻击者上传 shell.jsp → 服务器端执行恶意代码

// ✅ 安全：多重校验
// ① 白名单校验扩展名
String ext = FilenameUtils.getExtension(filename);
if (!List.of("jpg","png","gif","pdf").contains(ext.toLowerCase())) {
    throw new SecurityException("不允许的文件类型");
}
// ② 校验文件真实类型（Magic Number，非扩展名）
// ③ 随机重命名，不保留原始文件名
String newName = UUID.randomUUID().toString() + "." + ext;
// ④ 限制文件大小
// ⑤ 上传目录与代码部署目录分离（甚至用 OSS）
// ⑥ 禁止上传目录执行脚本（Nginx/Apache 配置）
```

---

## 八、不安全的反序列化

```java
// ❌ 危险：Java 原生反序列化未做类型检查
ObjectInputStream ois = new ObjectInputStream(inputStream);
Object obj = ois.readObject(); // 可能触发恶意类的 readObject()

// ✅ 方案1：不用 Java 原生序列化，改用 JSON/Protobuf
// ✅ 方案2：白名单限制可反序列化的类
// ✅ 方案3：使用安全的库（Jackson 的 enableDefaultTyping 也要谨慎）
```

> Log4Shell（CVE-2021-44228）本质上也是反序列化 + JNDI 注入的组合攻击。

---

## 九、其他高频漏洞

### 9.1 越权漏洞（IDOR）

```java
// ❌ 不检查数据归属
@GetMapping("/order/{id}")
public Order getOrder(@PathVariable Long id) {
    return orderService.getById(id); // 用户 A 能查到用户 B 的订单
}

// ✅ 校验数据归属
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

## 十、面试高频问题

**Q1：XSS 和 CSRF 的区别？**
> XSS：在受害者浏览器执行恶意脚本，利用**用户对网站的信任**。CSRF：诱导用户执行非预期操作，利用**网站对用户浏览器的信任**。XSS 可以绕过 CSRF 防护（因为能读到 CSRF Token）。

**Q2：SQL 注入怎么防御？做的项目里怎么处理？**
> 参数化查询（PreparedStatement）是第一道防线，ORM 框架（MyBatis 用 #{}）是第二道防线。输入校验（长度/类型/格式）做辅助。不用 ${}（除非做 order by / group by 等动态字段时用白名单）。

**Q3：HTTPS 能防什么？不能防什么？**
> 能防：中间人窃听、篡改（加密+认证）。不能防：应用层漏洞（SQL 注入、XSS）、CSRF、恶意软件。HTTPS 只保护传输层，不管应用层安全。

**Q4：JWT 安全吗？有什么坑？**
> 安全的前提：正确实现。坑：① 无法主动失效（退出登录后 Token 仍然有效直到过期）② Payload 只是 Base64 编码，可读不要放敏感信息 ③ 密钥泄露后所有 Token 可伪造。

---

## 📖 核心要点速记

1. **SQL 注入**：永远不用字符串拼接 SQL → PreparedStatement / #{}
2. **XSS**：输出编码（HTML 转义） + HttpOnly Cookie + CSP
3. **CSRF**：CSRF Token + SameSite Cookie
4. **SSRF**：禁止内网 IP + 白名单 + 禁用危险协议
5. **文件上传**：白名单扩展名 + 校验真实类型 + 随机文件名 + 非执行目录
6. **越权**：每个接口都校验数据归属，不做"信任前端"
7. **安全思维**：永远不信任用户输入 → 输入校验 + 输出编码 + 最小权限

---

## 📖 相关阅读

- [计算机安全-核心知识点](./计算机安全-核心知识点.md)
- [密码学基础核心知识点](./密码学基础核心知识点.md)
- [认证与授权详解](./认证与授权详解.md)
- [安全编码最佳实践](./安全编码最佳实践.md)
