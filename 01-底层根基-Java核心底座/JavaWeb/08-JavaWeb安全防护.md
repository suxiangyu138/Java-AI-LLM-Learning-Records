# 08 - JavaWeb 安全防护

> Java Web 安全的三道防线：输入校验（防注入）、会话保护（防劫持）、输出编码（防 XSS）。不理解 Web 安全的开发者，写出的代码都是定时炸弹。

---

## 目录

1. [Web 安全全景威胁](#1-web-安全全景威胁)
2. [SQL 注入防御](#2-sql-注入防御)
3. [XSS 跨站脚本防御](#3-xss-跨站脚本防御)
4. [CSRF 跨站请求伪造防御](#4-csrf-跨站请求伪造防御)
5. [会话安全](#5-会话安全)

---

## 1. Web 安全全景威胁

| 攻击类型 | 目标 | 危害等级 | 防御核心 |
|---------|------|:---:|---------|
| **SQL 注入** | 数据库 | 🔴 极高 | 预编译 + 参数化查询 |
| **XSS** | 用户浏览器 | 🟠 高 | 输出编码 + CSP |
| **CSRF** | 用户操作 | 🟠 高 | Token + SameSite Cookie |
| **会话劫持** | 用户身份 | 🟠 高 | HTTPS + HttpOnly + Secure |
| **文件上传漏洞** | 服务器 | 🔴 极高 | 类型校验 + 重命名 + 隔离 |
| **路径遍历** | 文件系统 | 🔴 极高 | 路径规范化 + 白名单 |

## 2. SQL 注入防御

### 攻击原理

```java
// ❌ 危险：字符串拼接
String username = request.getParameter("username");
String sql = "SELECT * FROM users WHERE username = '" + username + "'";
// 输入: ' OR '1'='1' -- 
// → SELECT * FROM users WHERE username = '' OR '1'='1' --'（返回全部用户）

// ✅ 安全：PreparedStatement
String sql = "SELECT * FROM users WHERE username = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, username);  // 自动转义
```

### MyBatis 中的 SQL 注入

```java
// ❌ ${} 直接拼接 → SQL 注入风险
@Select("SELECT * FROM users WHERE name = '${name}'")

// ✅ #{} 预编译 → 安全
@Select("SELECT * FROM users WHERE name = #{name}")

// 🟡 ${} 的合法用法（ORDER BY / 动态表名 → 必须白名单校验）
private static final Set<String> ALLOWED_COLUMNS = Set.of("id", "name", "create_time");
public List<User> getUsersByOrder(String orderBy) {
    if (!ALLOWED_COLUMNS.contains(orderBy)) {
        throw new IllegalArgumentException("Invalid column: " + orderBy);
    }
    return mapper.getUsersByOrder(orderBy);  // 白名单通过后才用 ${}
}
```

## 3. XSS 跨站脚本防御

### 三种 XSS 类型

| 类型 | 攻击方式 | 场景 |
|------|---------|------|
| **反射型** | URL 参数中嵌入脚本 | 搜索框、错误页面 |
| **存储型** | 脚本存入数据库后被读取 | 评论、用户资料 |
| **DOM 型** | 前端 JS 动态执行 | SPA 页面 |

```java
// 防御 1：输出编码
import org.owasp.encoder.Encode;
String safeOutput = Encode.forHtml(userInput);   // < → &lt;

// 防御 2：输入清洗
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
String clean = Jsoup.clean(userInput, Safelist.basic());
// 保留基本格式，去除 <script>/onclick 等

// 防御 3：HttpOnly Cookie（JS 无法读取）
Cookie cookie = new Cookie("JSESSIONID", sessionId);
cookie.setHttpOnly(true);   // ← 关键！
cookie.setSecure(true);     // 仅 HTTPS 传输
cookie.setAttribute("SameSite", "Strict");
```

## 4. CSRF 跨站请求伪造防御

```text
攻击场景：
  用户登录银行网站 → 浏览器保存 Cookie
  → 用户访问恶意网站（未登出银行）
  → 恶意网站发起 POST 请求到银行（浏览器自动携带 Cookie）
  → 银行认为这是用户的操作 → 转账成功 💸
```

```java
// 防御：同步令牌模式 (Synchronizer Token Pattern)
// Spring Security 已内置，手动实现如下：

// 1. 生成 Token 存入 Session
String token = UUID.randomUUID().toString();
session.setAttribute("csrf_token", token);

// 2. 表单中隐藏字段
// <input type="hidden" name="csrf_token" value="${csrf_token}">

// 3. Filter 校验
@WebFilter("/*")
public class CsrfFilter implements Filter {
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        HttpServletRequest request = (HttpServletRequest) req;
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String sessionToken = (String) request.getSession().getAttribute("csrf_token");
            String formToken = request.getParameter("csrf_token");
            if (sessionToken == null || !sessionToken.equals(formToken)) {
                throw new ServletException("CSRF 攻击被拦截");
            }
        }
        chain.doFilter(req, res);
    }
}
```

## 5. 会话安全

```java
// 安全 Cookie 配置
sessionCookie.setHttpOnly(true);    // 禁止 JS 访问
sessionCookie.setSecure(true);      // 仅 HTTPS
sessionCookie.setAttribute("SameSite", "Lax");  // 跨站限制

// 会话固定防御：登录后更换 Session ID
request.changeSessionId();  // Servlet 3.1+

// 会话超时
session.setMaxInactiveInterval(30 * 60);  // 30 分钟
// web.xml: <session-config><session-timeout>30</session-timeout></session-config>

// 限制并发会话
public class SessionManager {
    private static final Map<String, HttpSession> userSessions = new ConcurrentHashMap<>();

    public static void registerSession(String userId, HttpSession session) {
        HttpSession old = userSessions.put(userId, session);
        if (old != null) old.invalidate();  // 踢掉旧会话
    }
}
```

## 核心要点回顾

- SQL 注入：`#{}` 代替 `${}`，永远不要拼接 SQL
- XSS：输出编码（OWASP Encoder）+ HttpOnly Cookie
- CSRF：Token 校验 + SameSite Cookie
- 文件上传：MIME 校验 + 重命名 + 隔离目录 + 杀毒扫描
- 选择 Spring Security 框架方案（覆盖以上全部），手动实现仅用于理解原理

## 参考资料

1. OWASP Top 10 Web 安全风险
2. OWASP Java Encoder 项目
3. Spring Security 官方文档
