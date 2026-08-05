# 01 - URL 基础概念与组成结构

> 从 RFC 3986 语法出发，彻底讲清 URI/URL/URN 关系、六大组件规则、相对 URL 解析算法与 URL 归一化

---

## 📚 目录

1. [URI URL URN 三者关系](#1-uri-url-urn-三者关系)
2. [RFC 3986 通用语法](#2-rfc-3986-通用语法)
3. [六大组件逐个拆解](#3-六大组件逐个拆解)
4. [绝对 URL 与相对 URL](#4-绝对-url-与相对-url)
5. [Query 查询串的多种约定](#5-query-查询串的多种约定)
6. [URL 归一化与规范化](#6-url-归一化与规范化)
7. [长度限制与大小写敏感性](#7-长度限制与大小写敏感性)

---

## 1. URI URL URN 三者关系

```text
                    ┌──────────────────────────────┐
                    │            URI               │
                    │   统一资源标识符（超集）        │
                    │  ┌────────────┐ ┌──────────┐ │
                    │  │    URL     │ │   URN    │ │
                    │  │  怎么找到它  │ │ 它叫什么  │ │
                    │  └────────────┘ └──────────┘ │
                    └──────────────────────────────┘
```

| 维度 | URI | URL | URN |
|------|-----|-----|-----|
| 全称 | Uniform Resource Identifier | Uniform Resource Locator | Uniform Resource Name |
| 作用 | **标识**资源 | **定位**资源（含访问方式） | **命名**资源（持久唯一） |
| 是否含协议 | 不一定 | ✅ 必含 Scheme 且可访问 | `urn:` 固定前缀 |
| 资源迁移后 | — | ❌ 失效 | ✅ 仍有效 |
| 示例 | 两者皆是 | `https://a.com/b.png` | `urn:isbn:9787111213826` |

**实际工程中的用法差异**：

```java
// Java 中的语义区分非常明显
java.net.URI  uri = URI.create("urn:uuid:6e8bc430-9c3a-11d9-9669-0800200c9a66"); // ✅ OK
java.net.URL  url = URI.create("urn:uuid:...").toURL();  // ❌ MalformedURLException
// 原因：URL 必须有可用的协议处理器（http/https/file/jar/ftp...），URN 没有
```

> 🎯 **核心要点**：`URI` 是纯语法层面的字符串标识；`URL` 额外要求"能按协议访问到"。写代码优先用 `URI` 做解析和拼装，只在真正要发起连接时转成 `URL`。

---

## 2. RFC 3986 通用语法

### 2.1 完整 ABNF 骨架

```text
URI = scheme ":" hier-part [ "?" query ] [ "#" fragment ]

hier-part = "//" authority path-abempty
          / path-absolute
          / path-rootless
          / path-empty

authority = [ userinfo "@" ] host [ ":" port ]
```

### 2.2 分隔符记忆图

```text
  scheme  ://   userinfo   @    host    :  port   /path   ?query   #fragment
    ↑      ↑        ↑      ↑      ↑     ↑    ↑      ↑        ↑        ↑
    │      │        │      │      │     │    │      │        │        └ 客户端锚点
    │      │        │      │      │     │    │      │        └────────── 参数区
    │      │        │      │      │     │    └─────────────────────────── 层级路径
    │      │        │      │      │     └────────────────────────────────  端口分隔
    │      │        │      │      └──────────────────────────────────────  主机
    │      │        │      └─────────────────────────────────────────────  凭证分隔
    │      │        └────────────────────────────────────────────────────  用户名:密码
    │      └─────────────────────────────────────────────────────────────  有 authority 的标志
    └────────────────────────────────────────────────────────────────────  协议
```

### 2.3 各类形态实例

| 形态 | 示例 | 说明 |
|------|------|------|
| 完整网络 URL | `https://a.com:8443/p?q=1#f` | 最常见 |
| 无 authority | `mailto:tom@a.com` | `path-rootless` |
| 无 authority 绝对路径 | `urn:isbn:123` | URN 形态 |
| 空 host | `file:///C:/tmp/a.txt` | 三斜杠 = 空 host + 绝对路径 |
| 仅 authority | `https://a.com` | path 为空串 |
| Query 为空 | `https://a.com/p?` | `?` 存在但 query 是空串（≠ 无 query） |
| Fragment 为空 | `https://a.com/p#` | 同理 |

> ⚠️ `https://a.com/p` 与 `https://a.com/p?` 在 RFC 层面**不等价**（一个 query 为 `null`，一个为 `""`），Java `URI.getQuery()` 分别返回 `null` 与 `""`。

---

## 3. 六大组件逐个拆解

### 3.1 Scheme 协议

```text
scheme = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
```

| 规则 | 说明 |
|------|------|
| 必须字母开头 | `2http://` 非法 |
| 允许 `+ - .` | `git+ssh://`、`view-source:`、`coap+tcp://` |
| **大小写不敏感** | `HTTP://A.COM` ≡ `http://a.com`，规范化时统一转小写 |
| 不含 `//` | `//` 属于 hier-part，不是 scheme 的一部分 |

### 3.2 Authority = UserInfo + Host + Port

**UserInfo（已被废弃）**

```text
https://admin:123456@internal.corp.com/panel
```

| 风险 | 说明 |
|------|------|
| 明文凭证 | 会进浏览器历史、日志、Referer |
| 钓鱼利用 | `https://www.bank.com@evil.com` —— 真实主机是 `evil.com` |
| 浏览器策略 | Chrome/Firefox 对子资源请求直接忽略甚至阻止 |

> ⚠️ RFC 3986 明确"**不推荐**"在 URI 中使用 `user:password@` 形式。生产环境凭证一律走 Header 或 Body。

**Host 三种形态**

| 形态 | 示例 | 备注 |
|------|------|------|
| 注册名（域名） | `api.example.com` | 大小写不敏感 |
| IPv4 | `192.168.1.10` | 点分十进制 |
| IPv6 字面量 | `http://[2001:db8::1]:8080/` | **必须**用方括号包裹 |

```java
// IPv6 必须带方括号，否则 : 会被当成端口分隔符
URI ok  = URI.create("http://[::1]:8080/health");   // ✅ host = [::1]
URI bad = URI.create("http://::1:8080/health");     // ❌ 解析异常
```

**Port 与默认端口**

| Scheme | 默认端口 | Scheme | 默认端口 |
|--------|:---:|--------|:---:|
| `http` | 80 | `ws` | 80 |
| `https` | 443 | `wss` | 443 |
| `ftp` | 21 | `ssh` / `sftp` | 22 |
| `smtp` | 25 | `dns` | 53 |
| `ldap` | 389 | `ldaps` | 636 |

> 💡 归一化规则：**显式写出默认端口应被移除**。`https://a.com:443/p` ≡ `https://a.com/p`，但 `https://a.com:8443/p` 不可省略。这也是 CORS 同源判断的关键——`http://a.com` 与 `http://a.com:80` 同源，与 `http://a.com:8080` **不同源**。

### 3.3 Path 路径

| 规则 | 说明 |
|------|------|
| 以 `/` 分隔为多个 segment | `/v1/users/42` → `["v1","users","42"]` |
| **大小写敏感** | Linux 服务器 `/User` ≠ `/user`（Windows/macOS 文件系统可能不敏感 → 跨平台坑） |
| 支持 `.` `..` 点段 | 解析时必须移除（见 4.3） |
| 空 segment 有意义 | `/a//b` 中间是一个空 segment，不等价于 `/a/b` |
| 尾斜杠有语义差异 | `/docs` vs `/docs/` 在相对路径解析、Spring 3.x 路由中结果不同 |

**Matrix Variable（矩阵参数，冷门但 Spring 支持）**

```text
GET /cars/color=red;year=2026/detail
        └─────── 分号挂在 segment 上 ───────┘
```

```java
@GetMapping("/cars/{filter}/detail")
public String detail(@MatrixVariable String color,
                     @MatrixVariable int year) { ... }
// 需开启：UrlPathHelper.setRemoveSemicolonContent(false)
```

### 3.4 Query 查询串

```text
?key1=value1&key2=value2
```

| 事实 | 说明 |
|------|------|
| RFC **未规定**内部结构 | `&`/`=` 只是**约定**，历史上 `;` 也曾合法 |
| 顺序在语法上有意义 | `?a=1&b=2` 与 `?b=2&a=1` 是不同字符串（业务上通常等价） |
| 重复 key 合法 | `?id=1&id=2` → 后端可取数组（Spring `List<String>`） |
| 空值形态多样 | `?a=`（空串）、`?a`（无 `=`，值为 `null` 或 `""` 取决于框架） |

### 3.5 Fragment 片段

| 特性 | 说明 |
|------|------|
| **不发送到服务端** | 抓包看不到 `#` 及其后内容 |
| 由客户端解释 | 浏览器滚动定位、SPA hash 路由、PDF `#page=3` |
| OAuth 隐式模式曾用它传 token | 避免 token 进服务端日志（现已不推荐该模式） |
| 文本片段（Chrome） | `#:~:text=关键词` 直接高亮页面文字 |

```text
https://a.com/doc.pdf#page=12&zoom=150     ← PDF 阅读器解析
https://a.com/app#/user/42/profile         ← SPA hash 路由
https://a.com/x#:~:text=URL%20编码          ← 滚动并高亮
```

> 🎯 **核心要点**：任何需要服务端感知的信息，绝不能放在 Fragment 里；反之，任何不想让服务端看到的客户端状态（如 hash 路由），放 Fragment 最安全。

---

## 4. 绝对 URL 与相对 URL

### 4.1 四种相对形态

假设当前页面 Base = `https://a.com/docs/guide/intro.html`

| 类型 | 写法 | 解析结果 |
|------|------|---------|
| 绝对 URL | `https://b.com/x` | `https://b.com/x` |
| 协议相对（Scheme-relative） | `//cdn.com/lib.js` | `https://cdn.com/lib.js` |
| 根相对（Absolute-path） | `/api/v1/users` | `https://a.com/api/v1/users` |
| 目录相对（Relative-path） | `next.html` | `https://a.com/docs/guide/next.html` |
| 父级相对 | `../index.html` | `https://a.com/docs/index.html` |
| 仅 Query | `?page=2` | `https://a.com/docs/guide/intro.html?page=2` |
| 仅 Fragment | `#top` | `https://a.com/docs/guide/intro.html#top` |
| 空串 | `` | 当前 URL（去掉 fragment） |

> ⚠️ **协议相对 URL `//cdn.com/x` 已不推荐**：在 HTTPS 全面普及后它只带来解析歧义，且在 `file://` 页面下会失效。直接写 `https://`。

### 4.2 尾斜杠对相对解析的致命影响

```text
Base: https://a.com/docs/guide     （无尾斜杠 → "guide" 被当成文件名）
  相对 "api.html"  →  https://a.com/docs/api.html      ⚠️ guide 被替换

Base: https://a.com/docs/guide/    （有尾斜杠 → "guide" 是目录）
  相对 "api.html"  →  https://a.com/docs/guide/api.html  ✅
```

### 4.3 点段移除算法（Remove Dot Segments）

RFC 3986 §5.2.4 规定的标准算法：

| 输入路径 | 输出 |
|---------|------|
| `/a/b/c/./../../g` | `/a/g` |
| `/a/../../b` | `/b`（越界的 `..` 被丢弃，不会跑到根之上） |
| `mid/content=5/../6` | `mid/6` |
| `/./a` | `/a` |
| `/a/b/.` | `/a/b/` |

```java
// Java 内置实现
URI base = URI.create("https://a.com/docs/guide/intro.html");
System.out.println(base.resolve("../api/v2.html"));
// → https://a.com/docs/api/v2.html

System.out.println(base.resolve("//cdn.com/lib.js"));
// → https://cdn.com/lib.js

System.out.println(URI.create("https://a.com/a/b/c/./../../g").normalize());
// → https://a.com/a/g
```

> ⚠️ **安全提醒**：`..` 的移除必须在**解码之后**再做一次校验。攻击者用 `%2e%2e%2f` 绕过第一次检查，服务器解码后才变成 `../` —— 这是路径穿越的经典手法（详见 06 篇）。

---

## 5. Query 查询串的多种约定

同一份"数组 + 嵌套对象"数据，不同框架序列化结果完全不同：

| 风格 | 数组 `ids=[1,2]` | 嵌套 `filter.name=tom` | 代表框架 |
|------|------------------|------------------------|---------|
| 重复 key（标准） | `ids=1&ids=2` | — | Spring、Go、WHATWG |
| 方括号空 | `ids[]=1&ids[]=2` | — | PHP、Rails |
| 方括号索引 | `ids[0]=1&ids[1]=2` | — | qs（Node）、Rails |
| 逗号分隔 | `ids=1,2` | — | OpenAPI `style=form,explode=false` |
| 点号嵌套 | — | `filter.name=tom` | Spring（对象绑定） |
| 方括号嵌套 | — | `filter[name]=tom` | qs、PHP |
| 管道/空格 | `ids=1|2`、`ids=1 2` | — | OpenAPI `pipeDelimited` |

**Spring 后端对应写法**

```java
@GetMapping("/search")
public Result search(
    @RequestParam List<Integer> ids,        // ids=1&ids=2  或  ids=1,2 都能收
    @RequestParam(required = false) String kw,
    Filter filter,                          // filter.name=tom → 对象绑定
    Pageable pageable                       // page=0&size=20&sort=id,desc
) { ... }
```

> 💡 **对外 API 建议**：统一用**重复 key**（`ids=1&ids=2`）或**逗号分隔**（`ids=1,2`），并在 OpenAPI 文档中显式声明 `style`/`explode`，避免前后端各写一套。

---

## 6. URL 归一化与规范化

在**去重、缓存 Key、URL 签名、RAG 知识库入库**场景里，归一化是必做步骤。

### 6.1 语义保留型归一化（安全，一律该做）

| # | 规则 | 归一化前 | 归一化后 |
|:---:|------|---------|---------|
| 1 | Scheme 转小写 | `HTTPS://a.com` | `https://a.com` |
| 2 | Host 转小写 | `https://API.A.COM/P` | `https://api.a.com/P`（Path 不变！） |
| 3 | 移除默认端口 | `https://a.com:443/p` | `https://a.com/p` |
| 4 | 百分号十六进制转大写 | `%3a` | `%3A` |
| 5 | 解码非保留字符 | `%7Euser` | `~user` |
| 6 | 移除点段 | `/a/./b/../c` | `/a/c` |
| 7 | 空路径补 `/` | `https://a.com` | `https://a.com/` |
| 8 | IDN 转 Punycode | `münchen.de` | `xn--mnchen-3ya.de` |

### 6.2 语义可能改变型（需业务确认）

| 规则 | 风险 |
|------|------|
| 移除尾斜杠 `/a/` → `/a` | 部分服务视为不同资源 |
| 移除 `#fragment` | SPA hash 路由会丢页面状态 |
| 排序 Query 参数 | 签名场景需要，但服务端若依赖顺序会出错 |
| 移除空参数 `?a=&b=1` → `?b=1` | 后端可能靠"存在与否"判断 |
| 移除追踪参数 `utm_*`/`fbclid`/`gclid` | RAG 去重强烈推荐；广告归因场景不能删 |
| 强制 `http` → `https` | 目标站未支持 HTTPS 会挂 |
| 移除 `index.html`/`default.aspx` | 依赖静态路径的站点会 404 |

### 6.3 Java 归一化工具函数

```java
import java.net.URI;
import java.net.IDN;
import java.util.*;
import java.util.stream.Collectors;

public final class UrlNormalizer {

    private static final Set<String> TRACKING_PREFIX = Set.of("utm_");
    private static final Set<String> TRACKING_EXACT  =
            Set.of("fbclid", "gclid", "msclkid", "spm", "from", "share_source");

    /** 用于 RAG 知识库 / 爬虫去重的规范化 URL */
    public static String canonicalize(String raw) {
        URI u = URI.create(raw.trim()).normalize();          // 规则 4/5/6

        String scheme = u.getScheme() == null ? "https" : u.getScheme().toLowerCase();
        String host   = IDN.toASCII(u.getHost().toLowerCase());   // 规则 2/8
        int    port   = u.getPort();
        if (isDefaultPort(scheme, port)) port = -1;               // 规则 3

        String path = (u.getRawPath() == null || u.getRawPath().isEmpty())
                ? "/" : u.getRawPath();                           // 规则 7

        String query = cleanAndSortQuery(u.getRawQuery());

        StringBuilder sb = new StringBuilder(scheme).append("://").append(host);
        if (port != -1) sb.append(':').append(port);
        sb.append(path);
        if (query != null && !query.isEmpty()) sb.append('?').append(query);
        return sb.toString();                                    // 丢弃 fragment
    }

    private static boolean isDefaultPort(String scheme, int port) {
        return ("http".equals(scheme) && port == 80)
            || ("https".equals(scheme) && port == 443);
    }

    private static String cleanAndSortQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) return rawQuery;
        return Arrays.stream(rawQuery.split("&"))
                .filter(kv -> !kv.isEmpty())
                .filter(kv -> {
                    String k = kv.split("=", 2)[0].toLowerCase();
                    return TRACKING_PREFIX.stream().noneMatch(k::startsWith)
                        && !TRACKING_EXACT.contains(k);
                })
                .sorted()                                         // 排序保证幂等
                .collect(Collectors.joining("&"));
    }
}
```

```java
// 以下 5 个 URL 归一化后完全相同 → 只入库一次
UrlNormalizer.canonicalize("HTTPS://Blog.Example.COM:443/post/./42/?utm_source=wx&id=7#comment");
UrlNormalizer.canonicalize("https://blog.example.com/post/42/?id=7&utm_medium=cpc");
UrlNormalizer.canonicalize("https://blog.example.com/post/43/../42/?id=7");
// → https://blog.example.com/post/42/?id=7
```

> 🎯 **核心要点**：归一化的目标不是"最短"，而是"**幂等且一致**"。同一份规则要同时用在写入端和查询端，否则去重会失效。

---

## 7. 长度限制与大小写敏感性

### 7.1 URL 长度限制真相

**RFC 没有规定上限**，限制全部来自实现：

| 环节 | 限制 | 配置项 |
|------|------|-------|
| IE 11 | 2083 字符（**最严**） | 不可改 |
| Edge / Chrome | 约 2 MB（地址栏显示截断 32 KB） | — |
| Safari / Firefox | 约 64 KB+ | — |
| **Nginx** | 请求行 8 KB | `large_client_header_buffers 4 8k` |
| **Tomcat** | 请求行 + 头 8 KB | `server.max-http-request-header-size` |
| **Undertow** | 1 MB 总头 | `max-header-size` |
| **Spring Cloud Gateway** | 依赖 Netty | `max-initial-line-length` |
| CDN（Cloudflare 等） | 常见 8~16 KB | 各家不同 |
| 搜索引擎抓取 | 建议 < 2048 | SEO 最佳实践 |

> 💡 **工程结论**：对外 URL 控制在 **2000 字符以内**最安全。超长参数（如批量 ID、长 Prompt、Base64 图片）改用 `POST` + Body。

### 7.2 大小写敏感性对照表

| 组件 | 是否敏感 | 依据 |
|------|:---:|------|
| Scheme | ❌ | RFC 3986 明确不敏感 |
| Host | ❌ | DNS 不区分大小写 |
| Port | — | 纯数字 |
| **Path** | ✅ | 由服务端文件系统/路由决定 |
| **Query** | ✅ | 由应用解释 |
| **Fragment** | ✅ | 由客户端解释 |
| 百分号编码的 hex | ❌ | `%3A` ≡ `%3a`，规范写法用大写 |

> ⚠️ **跨平台经典事故**：Windows 本地开发 `/Static/Logo.PNG` 能访问，部署到 Linux 直接 404。约定**全小写 + 中划线**的 URL 路径（`/static/logo.png`、`/user-profile`）可彻底规避。

---

> 🎯 **本篇核心要点**
> 1. URI ⊃ URL / URN；写代码用 `URI` 解析，发请求时才转 `URL`
> 2. 六大组件里只有 **Fragment 不发给服务端**
> 3. Scheme 与 Host 大小写不敏感，**Path/Query 敏感**——统一小写路径
> 4. 尾斜杠改变相对 URL 解析结果，也改变 Spring 3.x 的路由匹配
> 5. 归一化要**幂等且写读一致**，是去重与签名的地基

---

**上一模块**：[00-URL知识体系总览.md](00-URL知识体系总览.md) / **下一模块**：[02-URL编码与字符集规范.md](02-URL编码与字符集规范.md)
