# 09 - URL 速查手册与面试高频考点

> 面试前的最后一站——URL 语法速查卡、常见端口表、编码对照表、HTTP 状态码精华、50 道面试高频题精讲，A4 打印即用

---

## 📚 目录

1. [URL 语法速查卡](#1-url-语法速查卡)
2. [常见默认端口全表](#2-常见默认端口全表)
3. [URL 编码速查表](#3-url-编码速查表)
4. [HTTP 状态码 URL 相关精选](#4-http-状态码-url-相关精选)
5. [常见 Content-Type](#5-常见-content-type)
6. [面试 50 题精讲](#6-面试-50-题精讲)

---

## 1. URL 语法速查卡

### 1.1 完整结构

```text
 https://user:pass@api.example.com:8443/v1/users/42?page=2&size=10#profile
 └─┬─┘   └───┬────┘ └──────┬───────┘└┬─┘└─────┬─────┘└──────┬──────┘└──┬──┘
scheme   userinfo        host       port    path       query     fragment
         └──────────────── authority ──────────────────┘
└─────────────────────────── URL ───────────────────────────────┘
```

### 1.2 六大组件速查

| 组件 | 必填 | 发送到服务端 | 大小写 | 分隔符 |
|------|:---:|:---:|:---:|------|
| Scheme | ✅ | ✅（决定连接方式） | ❌ 不敏感 | `:` |
| UserInfo | ❌ | ⚠️ 已废弃 | ✅ | `@` |
| Host | ✅ | ✅（Host 头） | ❌ 不敏感 | — |
| Port | ❌ | ✅ | — | `:` |
| Path | ❌ | ✅ | ✅ 敏感 | `/` |
| Query | ❌ | ✅ | ✅ 敏感 | `?` `&` `=` |
| Fragment | ❌ | ❌ 不发送 | ✅ 敏感 | `#` |

### 1.3 RFC 3986 ABNF 骨架

```text
URI    = scheme ":" hier-part [ "?" query ] [ "#" fragment ]
scheme = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )    ← 字母开头
authority = [ userinfo "@" ] host [ ":" port ]
path   = path-abempty / path-absolute / path-rootless / path-empty
query  = *( pchar / "/" / "?" )                         ← 无固定结构
fragment = *( pchar / "/" / "?" )
```

### 1.4 相对 URL 解析速查

| 相对写法 | 含义 | 示例（Base = `https://a.com/dir/page`） |
|---------|------|---------------------------------------|
| `//cdn.com/x` | 协议相对 | `https://cdn.com/x` |
| `/api/x` | 根相对 | `https://a.com/api/x` |
| `x.html` | 目录相对 | `https://a.com/dir/x.html` |
| `../x` | 上级相对 | `https://a.com/x` |
| `?q=1` | 仅 Query | `https://a.com/dir/page?q=1` |
| `#top` | 仅 Fragment | `https://a.com/dir/page#top` |

### 1.5 URI vs URL vs URN

```text
URI（身份证）⊃ URL（家庭地址）+ URN（姓名）
```

| 维度 | URI | URL | URN |
|------|-----|-----|-----|
| 作用 | 标识 | 标识 + 定位（怎么拿） | 标识（永久名） |
| 示例 | URL、URN | `https://a.com/x.png` | `urn:isbn:978-3-16-148410-0` |
| 迁移后 | — | ❌ 失效 | ✅ 仍有效 |

---

## 2. 常见默认端口全表

### 2.1 Web 与网络

| 协议 | 端口 | 说明 |
|------|:---:|------|
| HTTP | 80 | — |
| HTTPS | 443 | TLS |
| HTTP/3 (QUIC) | 443 | UDP |
| HTTP 代理 | 3128 / 8080 | Squid / 正向代理 |
| SOCKS 代理 | 1080 | — |

### 2.2 数据库与缓存

| 服务 | 端口 | 协议/说明 |
|------|:---:|------|
| MySQL | 3306 | TCP |
| PostgreSQL | 5432 | TCP |
| Redis | 6379 | TCP |
| MongoDB | 27017 | TCP |
| Elasticsearch | 9200 (HTTP) / 9300 (Transport) | TCP |
| ClickHouse | 8123 (HTTP) / 9000 (Native) | TCP |
| Neo4j | 7474 (HTTP) / 7687 (Bolt) | TCP |
| Cassandra | 9042 | TCP |
| H2 | 9092 (Web Console) / 动态 (TCP) | — |

### 2.3 消息队列

| 服务 | 端口 | 说明 |
|------|:---:|------|
| RabbitMQ | 5672 (AMQP) / 15672 (管理) | TCP |
| Kafka | 9092 | TCP |
| ZooKeeper | 2181 | TCP |
| Nacos | 8848 | TCP |
| Pulsar | 6650 | TCP |

### 2.4 实时通信

| 协议 | 端口 | 说明 |
|------|:---:|------|
| WebSocket (ws) | 80 | 同 HTTP |
| WebSocket Secure (wss) | 443 | 同 HTTPS |
| MQTT | 1883 | IoT |
| MQTT over TLS | 8883 | — |
| STOMP | 61613 | — |

### 2.5 基础设施

| 服务 | 端口 | 说明 |
|------|:---:|------|
| DNS | 53 | TCP/UDP |
| SSH | 22 | — |
| FTP | 21 (控制) / 20 (数据) | — |
| SFTP | 22 | SSH 子系统 |
| SMTP | 25 / 587 (TLS) | 邮件发送 |
| IMAP | 143 / 993 (TLS) | 邮件收取 |
| LDAP | 389 / 636 (LDAPS) | 目录服务 |
| NTP | 123 | 时间同步 |
| RDP | 3389 | 远程桌面 |

### 2.6 AI / LLM 本地服务

| 服务 | 端口 | 说明 |
|------|:---:|------|
| Ollama | 11434 | 本地 LLM |
| vLLM | 8000 | 本地推理引擎 |
| text-generation-webui | 7860 | Gradio 默认 |
| Stable Diffusion WebUI | 7860 | 同上 |
| Weaviate | 8080 | 向量数据库 |
| Milvus | 19530 | 向量数据库 |
| Qdrant | 6333 | 向量数据库 |
| Chroma | 8000 | 向量数据库 |
| LangFuse | 3000 | LLM 可观测 |
| Open WebUI | 3000 | ChatGPT 风格前端 |

---

## 3. URL 编码速查表

### 3.1 必须编码的字符

| 字符 | 编码 | 原因 |
|:---:|------|------|
| 空格 | `%20` | 非 ASCII 可打印 |
| `!` | `%21` | sub-delim，但 Query 中可能冲突 |
| `"` | `%22` | 不安全 |
| `#` | `%23` | Fragment 分隔符 |
| `$` | `%24` | sub-delim |
| `%` | `%25` | 编码前缀，必须自身编码 |
| `&` | `%26` | Query 参数分隔符 |
| `'` | `%27` | sub-delim |
| `(` `)` | `%28` `%29` | sub-delim |
| `*` | `%2A` | sub-delim（`encodeURIComponent` 不编码） |
| `+` | `%2B` | 表单编码代表空格 |
| `,` | `%2C` | sub-delim |
| `/` | `%2F` | Path 层级分隔符（Path 段中必须编码） |
| `:` | `%3A` | Scheme/Port 分隔符 |
| `;` | `%3B` | 矩阵参数分隔符 |
| `<` `>` | `%3C` `%3E` | 不安全（HTML 语境） |
| `=` | `%3D` | Query 键值分隔符 |
| `?` | `%3F` | Query 分隔符 |
| `@` | `%40` | UserInfo 分隔符 |
| `[` `]` | `%5B` `%5D` | IPv6 / 不安全 |
| `\` | `%5C` | 不安全 |
| `{` `}` `|` | `%7B` `%7D` `%7C` | 不安全 |

### 3.2 不需要编码的字符

| 类别 | 字符 | 数量 |
|------|------|:---:|
| 大写字母 | `A-Z` | 26 |
| 小写字母 | `a-z` | 26 |
| 数字 | `0-9` | 10 |
| 非保留 | `-` `.` `_` `~` | 4 |

### 3.3 JS/Java 编码 API 对照

| 操作 | JavaScript | Java（Spring） |
|------|-----------|---------------|
| 编码 Query 值 | `encodeURIComponent(v)` | `UriUtils.encodeQueryParam(v, UTF_8)` |
| 编码 Path 段 | `encodeURIComponent(v)` | `UriUtils.encodePathSegment(v, UTF_8)` |
| 编码整条 URL（不推荐） | `encodeURI(url)` | — |
| 解码 | `decodeURIComponent(v)` | `UriUtils.decode(v, UTF_8)` |
| 拼接 Query | `new URLSearchParams()` | `UriComponentsBuilder` |
| 解析 URL 各组件 | `new URL(s)` | `URI.create(s)` |
| Base64URL | `btoa` + replace | `Base64.getUrlEncoder()` |

### 3.4 空格处理三个标准

| 标准 | 空格编码 | 适用 |
|------|:---:|------|
| RFC 3986 | `%20` | URI/URL 标准 |
| `application/x-www-form-urlencoded` | `+` | HTML 表单、Query 串 |
| `multipart/form-data` | 原始空格 | 文件上传 |

---

## 4. HTTP 状态码 URL 相关精选

### 4.1 重定向类 (3xx)

| 码 | 名称 | 方法保留 | 缓存 | 典型用途 |
|:---:|------|:---:|:---:|---------|
| 301 | Moved Permanently | ❌ POST→GET | ✅ 强缓存 | 域名迁移、HTTP→HTTPS |
| 302 | Found | ❌ POST→GET | ⚠️ | 临时跳转 |
| 303 | See Other | ✅→GET | ❌ | POST 后跳详情(PRG) |
| 304 | Not Modified | — | — | 缓存协商 |
| 307 | Temporary Redirect | ✅ 保留 | ❌ | POST 临时跳转 |
| 308 | Permanent Redirect | ✅ 保留 | ✅ | 永久跳转+保留POST |

### 4.2 客户端错误类 (4xx) —— URL 相关

| 码 | 含义 | 常见原因 |
|:---:|------|---------|
| 400 | Bad Request | URL 语法非法、请求行过长、编码错误 |
| 403 | Forbidden | 路径遍历被拦截、IP 黑名单 |
| 404 | Not Found | 路由匹配不到、资源不存在 |
| 405 | Method Not Allowed | `POST /users/42` 但只有 GET |
| 410 | Gone | 短链已删除/过期（比 404 更精确） |
| 414 | URI Too Long | URL 超过服务器限制 |
| 415 | Unsupported Media Type | Accept/Content-Type 不匹配 |

### 4.3 服务端错误类 (5xx)

| 码 | 含义 | URL 相关可能原因 |
|:---:|------|----------------|
| 502 | Bad Gateway | 后端服务不可达、Nginx `proxy_pass` 配置错误 |
| 504 | Gateway Timeout | `proxy_read_timeout` 太小、LLM 推理超时 |

---

## 5. 常见 Content-Type

| Content-Type | 场景 |
|-------------|------|
| `application/json` | REST API 请求/响应（最常用） |
| `application/x-www-form-urlencoded` | HTML 表单提交 |
| `multipart/form-data` | 文件上传 |
| `text/event-stream` | **SSE 流式输出（LLM 必备）** |
| `text/html` | 网页 |
| `text/plain` | 纯文本 |
| `application/octet-stream` | 二进制下载 |
| `application/xml` / `text/xml` | XML / SOAP |
| `application/graphql` | GraphQL |
| `application/pdf` | PDF |
| `image/png` / `image/jpeg` / `image/webp` | 图片（多模态 API） |
| `audio/wav` / `audio/mp3` | 音频（Whisper 等） |

---

## 6. 面试 50 题精讲

### 基础概念（题 1-10）

**Q1：URI 和 URL 有什么区别？**

> URI 是统一资源标识符（超集），URL 是统一资源定位符（子集），URN 是统一资源名称（子集）。URL 不仅标识资源还说明怎么获取（含 Scheme），URN 只永 久命名。URL 和 URN 都是 URI。日常说的"URI"基本就是 URL。

**Q2：URL 由哪几个部分组成？**

> `scheme://userinfo@host:port/path?query#fragment`。其中 authority = userinfo@host:port。只有 Fragment 不发给服务端。

**Q3：Fragment（`#`）有什么用？为什么不发给服务端？**

> Fragment 是客户端锚点：浏览器滚动定位、SPA hash 路由、PDF `#page=3`。不发给服务端是因为它是纯客户端交互信息。任何服务端需要的信息不能放 Fragment。

**Q4：`https://a.com` 和 `https://a.com/` 等价吗？**

> 在 HTTP 协议层面等价（`GET /` vs `GET /`），但在 URL 语法上不同：一个 path 为空，一个 path 为 `/`。在相对 URL 解析和 Spring 路由匹配中，它们行为一致（`URI.resolve()` 都会补 `/`）。

**Q5：什么是协议相对 URL（Protocol-Relative URL）？为什么不再推荐？**

> `//cdn.com/lib.js`，自动继承当前页面的协议。在 HTTPS 全面普及后已无必要，且在 `file://` 页面下会失效，有解析歧义。直接写 `https://`。

**Q6：相对 URL 的解析算法是怎样的？**

> RFC 3986 §5.2 规定的标准算法：取 Base URL 的各组件，用相对 URL 的各组件逐层覆盖（Scheme→1，Authority→2...），最后对合并后的 Path 做点段移除（`. `→删，`..`→删上一段）。

**Q7：URL 最大多长？**

> RFC 没规定上限。实际限制：IE 2083 字符（最严），Chrome 约 2 MB，Nginx 请求行 8 KB（默认），Tomcat 请求行+头 8 KB。工程上控制在 **2000 字符以内**最安全。

**Q8：URL 中哪些部分大小写敏感？**

> Scheme：不敏感。Host：不敏感（DNS 不区分）。Path：**敏感**（服务端文件系统/路由决定）。Query：**敏感**（应用解释）。Fragment：**敏感**（客户端解释）。

**Q9：`URL` 和 `URI` 类用哪个？为什么？**

> Java 中优先用 `URI`。`URL.equals()` 会做 DNS 解析 + IP 比对（性能问题且不确定），`URI.equals()` 是纯字符串比较。Java 20+ 已废弃 `URL` 的 String 构造器。

**Q10：什么是 URL 归一化？**

> 消除同一资源的不同 URL 表示，使其映射到同一规范形式。安全规则：Scheme/Host 小写、去默认端口、百分号 hex 大写、解码非保留字符、移除点段。风险规则：去尾斜杠、排序 Query、去追踪参数。

---

### 编码（题 11-18）

**Q11：`encodeURI` vs `encodeURIComponent` 的区别？**

> `encodeURI` 不编码 `A-Za-z0-9 - _ . ! ~ * ' ( ) ; , / ? : @ & = + $ #`（保留 URL 结构），适合编码整条 URL。`encodeURIComponent` 只不编码 `A-Za-z0-9 - _ . ! ~ * ' ( )`，所有分隔符（`& = / ? #`）都编码，**适合编码单个参数值**。

**Q12：Java `URLEncoder` 为什么有坑？**

> `URLEncoder` 实现的是 `application/x-www-form-urlencoded`（HTML 表单编码），不是 RFC 3986 URI 编码。空格变 `+`（不是 `%20`），`~` 被多余编码成 `%7E`。**只能用于编码 Query 参数值，不能用来编码 Path**。

**Q13：`+` 和 `%20` 在 URL 中有什么区别？**

> `%20` 是 RFC 3986 URI 标准（空格编码），`+` 是 HTML 表单编码标准（空格编码）。Query 中 `+` 通常被解成空格，Path 中 `+` 是字面量。**统一用 `%20` 最安全**。

**Q14：中文 URL 乱码怎么排查？**

> 三步排查：① 抓包看 `%XX` 是 3 字节（UTF-8）还是 2 字节（GBK）；② 确认 Tomcat `URIEncoding=UTF-8`；③ 确认应用没有二次调用 `URLDecoder.decode()`。UTF-8 "中文"→`%E4%B8%AD%E6%96%87`，GBK→`%D6%D0%CE%C4`。

**Q15：Base64 和 Base64URL 有什么区别？**

> Base64 含 `+`、`/`、`=`（URL 不安全）。Base64URL 把 `+`→`-`、`/`→`_`、去掉末尾 `=`。JWT、签名值放 URL 必须用 Base64URL。

**Q16：IDN（国际化域名）是什么？Punycode 怎么工作？**

> IDN 允许域名含非 ASCII 字符（如中文域名、ü）。Punycode 把非 ASCII 标签转成 `xn--` 前缀 + ASCII 编码。`münchen.de` → `xn--mnchen-3ya.de`。Java 用 `java.net.IDN.toASCII()`。

**Q17：什么是同形异义攻击（Homograph Attack）？**

> 用看起来一样的 Unicode 字符伪造域名。`аpple.com`（首字母是西里尔 `а` U+0430）≠ `apple.com`（ASCII `a`）。Punycode 后不同：`xn--pple-43d.com` vs `apple.com`。防御：域名白名单比对 Punycode 形态。

**Q18：`%2F` 和 `/` 在 URL Path 中有何不同？**

> `/` 是 Path 层级分隔符。`%2F` 是字面量斜杠，不代表层级。Tomcat 默认拒绝 `%2F`（返回 400），需 `-D...ALLOW_ENCODED_SLASH=true` 才放行——但这可能打开路径穿越漏洞。

---

### 后端路由（题 19-25）

**Q19：`@PathVariable` vs `@RequestParam` 的区别？**

> `@PathVariable`：从 URL Path 占位符取值（`/users/{id}`）。`@RequestParam`：从 Query 参数或表单取值（`?id=1`）。RESTful：资源标识用 PathVariable，筛选/分页用 RequestParam。

**Q20：Spring Boot 3 尾斜杠发生了什么？**

> Spring Boot 2 默认 `useTrailingSlashMatch=true`，`/api/users/` 能匹配到 `/api/users`。Spring Boot 3 **默认关闭**（已弃用该配置），请求 `/api/users/` 返回 404。迁移方案：Nginx 层用 308 统一去掉尾斜杠。

**Q21：AntPathMatcher vs PathPattern 的区别？**

> AntPathMatcher：旧实现，每次扫描字符串，支持 `**` 在中间。PathPattern：Spring 5.3+ 默认，预解析成树，快 6~8 倍，`**` 只能在末尾，新增 `{*name}` 匹配剩余全部。Spring Boot 3 默认 PathPattern。

**Q22：Nginx `proxy_pass` 加不加 `/` 有什么区别？**

> 末尾有 `/`：剥掉 location 前缀再拼（如 `location /api/` + `proxy_pass http://backend/;` → `/api/users` 变成 `/users`）。末尾无 `/`：原始 URI 不变透传。

**Q23：301 vs 302 vs 307 vs 308 怎么选？**

> 301：永久 + 可能改方法(GET)。302：临时 + 可能改方法(GET)。307：临时 + **保留方法**。308：永久 + **保留方法**。短链用 302。去尾斜杠用 308（保留 POST）。

**Q24：RESTful API 的 URL 设计有哪些铁律？**

> ① 名词复数（`/users`）。② HTTP 方法表达动作（`GET /users`、`POST /users`）。③ 层级不超过 2-3 层。④ 筛选/分页用 Query。⑤ 全小写 + 中划线。⑥ 不带文件后缀（用 Accept 头协商）。

**Q25：Spring Security 的路径匹配有什么坑？**

> Security 与 MVC 的路径匹配必须一致，否则出权限绕过。典型案例：`/api/admin/**` 不覆盖 `/api/admin`（无尾斜杠）→ 需同时配置两个模式。Security 匹配**区分大小写**→ `/API/Admin` 可能被绕过。

---

### 安全（题 26-34）

**Q26：什么是 SSRF？怎么防御？**

> 服务端请求伪造——攻击者让服务端发起恶意 HTTP 请求（打内网、读云元数据）。防御四层：① Scheme 白名单(http/https)。② DNS 解析后 IP 黑名单(阻止 127.0.0.1/10.0.0.0/8/169.254.0.0/16 等)。③ 手动控制重定向，每步校验。④ 禁用 file:/gopher:/dict: 等协议。

**Q27：为什么 IP 不能做字符串匹配来判断 SSRF？**

> 127.0.0.1 的等价写法太多了：`0x7f.0.0.1`、`0177.0.0.1`、`2130706433`（十进制）、`0x7f000001`、`[::1]`、`[::ffff:127.0.0.1]`、`localhost`、`127.0.0.1.nip.io`。只有 DNS 解析后的 InetAddress 判断才可靠。

**Q28：什么是开放重定向？怎么防御？**

> 攻击者构造恶意跳转 URL，利用合法网站的跳转功能把用户引到钓鱼站。防御：① 不允许绝对 URL（只接受相对路径）。② 如果必须接受绝对 URL，只允许白名单域名。③ 防止 `//evil.com`（协议相对）和 `\evil.com`（反斜杠）绕过。

**Q29：什么是 URL 解析器差异攻击？**

> 不同解析器（Java `URI`、WHATWG `URL`、curl）对同一字符串解析结果不同。过滤用 A 解析认为是 evil.com（拒绝），执行用 B 解析拿到 safe.com（放行）。防御：过滤层和执行层用同一解析逻辑 + 统一先归一化。

**Q30：路径穿越怎么防？**

> ① URL 解码后检查是否含 `..`。② 拒绝以 `/` 或 `\` 开头。③ `resolve()` + `normalize()`。④ `toRealPath()` 后检查 `startsWith(baseDir)`。⑤ 文件名白名单（只允许 `[a-zA-Z0-9._-]`）。

**Q31：`javascript:` 伪协议有什么 XSS 风险？**

> `<a href="javascript:alert(document.cookie)">` 用户可控的链接可能注入可执行脚本。防御：所有 UGC 内容的链接限制 Scheme 白名单（只允许 `http`/`https`/`mailto`/`tel`）。

**Q32：URL 签名解决了什么问题？怎么实现？**

> 防 URL 参数篡改（如改 `email=` 参数）。实现：① 对 Query 参数按 key 排序。② 拼接 `path?sorted_query`。③ HMAC-SHA256 生成签名。④ 附在 URL 末尾 `&sig=xxx`。必须包含过期时间。

**Q33：为什么 Token 不能放 URL？**

> 五种泄露路径：地址栏截图、浏览器历史、服务端 access_log、Referer 头（带到第三方）、分享时直接发出。生产环境 Token 一律放 Header（`Authorization: Bearer xxx`）或 Body。

**Q34：云元数据服务 IP 是多少？**

> AWS：`169.254.169.254`。阿里云：`100.100.100.200`。腾讯云：`metadata.tencentyun.com`。Azure：`169.254.169.254`。GCP：`metadata.google.internal`。这些是 SSRF 攻击的首要目标。

---

### 系统设计（题 35-40）

**Q35：短链系统怎么生成短码？**

> 核心方案：① Hash（`SHA256(longUrl)` → 取前 42bit → Base62），优点天然去重，缺点冲突需处理。② 发号器（Snowflake/Redis INCR）→ Base62，无冲突，高性能。推荐生产环境用 Snowflake + Base62。

**Q36：Base62 是什么？为什么不直接用 Base64？**

> Base62 字符集：`0-9A-Za-z`（62 个字符），无 URL 特殊字符。Base64 含 `+`、`/`、`=` → 放 URL 需要额外编码。Base62URL（`0-9A-Za-z-_`）也是好选择。7 位 Base62 = 3.5 万亿空间，足够短链使用。

**Q37：短链跳转用 301 还是 302？为什么？**

> **302**。原因：① 302 每次请求都到短链服务器，能统计点击量。② 301 被浏览器强缓存，后续点击不到服务器，无法统计和修改目标。商业短链（广告/社交分享）必须用 302。唯一例外：个人永久迁移链接用 301。

**Q38：短链系统的缓存穿透怎么处理？**

> 三层防御：① **布隆过滤器**：快速拒绝一定不存在的短码（减少 99% 无效查询）。② **缓存空值**：不存在的短码缓存 `NULL` 占位符，过期时间短（5 分钟）。③ **分布式锁**：热点短码过期瞬间，只让一个线程查 DB。

**Q39：短链系统容量估算是怎样的？**

> 假设日生成 1000 万条、日访问 10 亿次。存储 5 年 → 182.5 亿条 × 500B = ~850GB MySQL。热数据缓存（30 天）→ ~60 GB Redis。读 QPS 峰值 → 约 5 万 QPS → 1 主 2 从 Redis 集群。分库策略：范围分片（新数据自然写新库）。

**Q40：短链系统怎么防刷？**

> ① 令牌桶限流：同一 IP 每秒最多 N 次生成请求。② 验证码：同一 IP 短时间大量请求触发。③ 内容安全：异步扫描长链是否恶意。④ 禁封：恶意短链实时标记，访问时返回 410。

---

### AI 大模型相关（题 41-46）

**Q41：LLM 的 `base_url` 填错了会怎样？**

> 最常见的 Bug：`base_url` 末尾 `/v1` 与代码中路径 `/v1/chat/completions` 拼成 `/v1/v1/chat/completions` → **404 Not Found**。反过来，如果 `base_url` 不含 `/v1`，直接拼 `/chat/completions` → 也是 404。解法：写一个 `LlmBaseUrl.normalize()` 统一处理。

**Q42：流式输出的 Nginx 需要什么特殊配置？**

> 三个关键：① `proxy_buffering off`（否则 SSE 全缓存压到结束才吐）。② `proxy_read_timeout 600s`（LLM 长推理可能几分钟）。③ `proxy_set_header Connection ""`（支持 HTTP/1.1 长连接）。少一个都会"前端卡住不动"或"504 Gateway Timeout"。

**Q43：多模态 API 的图片 URL 有哪些坑？**

> ① URL 必须公网可达且无需鉴权→内网图片转 Data URL。② 图片太大可能超 Token 限制→先压缩。③ 图片 URL 含签名 Token→送模型提供商会泄露在日志里。④ Data URL 体积 +33%→小图(<4KB)用它，大图用公网 URL。

**Q44：RAG 系统中 URL 归一化为什么重要？**

> 同一篇文章可能被多个 URL 变体收录（`https://a.com/p` / `https://A.COM/p?utm_source=wx` / `https://a.com/p/`...），不做归一化 → 同一内容重复入库 → 检索结果重复 → 浪费 Token 和存储。归一化四步：小写 host、去追踪参数、列化 Query、去 Fragment。

**Q45：Agent 的 WebFetch 工具有什么安全风险？**

> 三大风险：① SSRF：用户给内网 URL → Agent 读内网敏感数据并返回。② 间接 Prompt Injection：网页内容含恶意指令劫持 Agent 行为。③ 资源耗尽：指向超长内容的 URL → 内存溢出。防御：沙箱化（独立 LLM 调用 + 内容标记 + URL 安全校验 + 大小限制）。

**Q46：前端能直接调 LLM API 吗？**

> **不能**（生产环境）。原因：前端会暴露 API Key（任何人 F12 就能看到），导致 Key 被盗刷。正确做法：后端代理转发→前端只和后端通信，API Key 只在服务端。

---

### 综合 + 陷阱（题 47-50）

**Q47：`URL.equals()` 在 Java 中有什么问题？**

> `java.net.URL.equals()` 会做 DNS 解析 + IP 比对。后果：① 性能问题（每次比较都是网络 I/O）。② 不确定结果（DNS 轮询返回不同 IP → 相同逻辑 URL 被判不相等）。③ 阻塞风险（DNS 不可达时卡住）。**永远用 `URI.equals()` 或归一化后的 String 比较。**

**Q48：`new URL(str)` vs `URI.create(str).toURL()` ？**

> `new URL(str)` 在 Java 20+ 已被标记 `@Deprecated`。推荐 `URI.create(str).toURL()`。原因：① URI 解析不联网。② toURL() 验证协议处理器可用性。③ 语法更清晰。

**Q49：`$uri` vs `$request_uri` 在 Nginx 中的区别？**

> `$uri`：规范化 + **解码**后的 Path（不含 Query）。`$request_uri`：原始 URI（含 Query，未解码）。**做安全过滤必须用 `$request_uri`**。`$uri` 已被解码 `%2e%2e%2f`→`../`，可能绕过 WAF 规则。

**Q50：如果让你设计一个 API 网关的 URL 安全模块，你会考虑哪些点？**

> ① Scheme 白名单（只允许 http/https）。② 内网 IP 拦截（DNS 解析后判断）。③ 路径穿越检测（解码后检查 `..`）。④ 危险 Header 过滤（`X-Forwarded-For` 伪造）。⑤ URL 超长拒绝（> 8KB 直接 414）。⑥ 重定向目标校验（防止开放重定向）。⑦ 请求方法限制（只允许 GET/POST/PUT/PATCH/DELETE）。⑧ 速率限制（URI + IP 维度）。⑨ URL 解码后二次检查（防双重编码绕过）。⑩ 日志脱敏（Token/密码参数打码）。

---

> 🎯 **本篇核心要点**
> 1. 基础题（Q1-10）——必会，所有岗位都会问
> 2. 编码题（Q11-18）——前端/全栈高频，`encodeURIComponent` vs `URLEncoder` 是经典题
> 3. 路由题（Q19-25）——Java 后端必问，Spring Boot 3 尾斜杠 + Nginx `proxy_pass` 是实战重点
> 4. 安全题（Q26-34）——高级/安全岗必问，SSRF + 开放重定向 + 路径穿越 三件套
> 5. 系统设计（Q35-40）——短链系统是经典题，必会容量估算和缓存策略
> 6. AI 题（Q41-46）——AI 应用岗新增考点，`base_url` 排错 + 流式 Nginx + Agent 安全
> 7. 陷阱题（Q47-50）——区分优秀和卓越的分水岭

---

**上一模块**：[08-URL在AI大模型应用中的实战.md](08-URL在AI大模型应用中的实战.md) / **返回总览**：[00-URL知识体系总览.md](00-URL知识体系总览.md)
