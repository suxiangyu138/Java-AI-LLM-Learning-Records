# 02 - URL 编码与字符集规范

> 百分号编码是 URL 领域最大的坑区——保留字符、`+` 与空格、`encodeURI` 与 `encodeURIComponent`、`URLEncoder` 的历史包袱、中文乱码、IDN 与 Base64URL，一次讲透

---

## 📚 目录

1. [为什么需要 URL 编码](#1-为什么需要-url-编码)
2. [字符分类保留与非保留](#2-字符分类保留与非保留)
3. [百分号编码机制](#3-百分号编码机制)
4. [空格与加号的世纪之坑](#4-空格与加号的世纪之坑)
5. [JavaScript 编码 API 对比](#5-javascript-编码-api-对比)
6. [Java 编码 API 对比](#6-java-编码-api-对比)
7. [中文与字符集问题排查](#7-中文与字符集问题排查)
8. [IDN 国际化域名与 Punycode](#8-idn-国际化域名与-punycode)
9. [Base64 与 Base64URL](#9-base64-与-base64url)
10. [双重编码与编码陷阱](#10-双重编码与编码陷阱)

---

## 1. 为什么需要 URL 编码

URL 只允许 **ASCII 可打印子集**中的一部分字符。三类字符必须编码：

| 类别 | 原因 | 例子 |
|------|------|------|
| **有语法含义的分隔符** | 出现在数据里会破坏结构 | 参数值含 `&`、`=`、`#`、`?` |
| **不安全 / 会被处理的字符** | 空格被截断、`"` 被引号包裹、`<>` 被 HTML 吃掉 | 空格、`" < > { } | \ ^ ~ [ ]` `` ` `` |
| **非 ASCII 字符** | URL 传输层只认字节 | 中文、emoji、日文 |

**破坏结构的直观例子**：

```text
❌ 期望搜索 "A&B=C" 这个关键词，直接拼接：
   /search?kw=A&B=C&page=1
   服务端解析成 3 个参数：kw=A、B=C、page=1   ← kw 只剩 "A"

✅ 对参数值单独编码：
   /search?kw=A%26B%3DC&page=1
   服务端解析成 2 个参数：kw="A&B=C"、page=1   ← 正确
```

> 🎯 **核心要点**：编码的对象永远是"**单个参数值 / 单个路径段**"，绝不是整条 URL。这是 90% 编码 Bug 的根源。

---

## 2. 字符分类保留与非保留

RFC 3986 把字符分为三大类：

### 2.1 非保留字符 Unreserved（共 66 个，永不需要编码）

```text
A-Z  a-z  0-9  -  .  _  ~
```

> ⚠️ 注意 `~` 是**非保留**字符（RFC 3986 才加入的）。但 Java `URLEncoder` 会把它编成 `%7E`，导致签名不一致——见第 6 节。

### 2.2 保留字符 Reserved（共 18 个，作分隔符时不编码，作数据时必须编码）

| 子类 | 字符 | 用途 |
|------|------|------|
| **gen-delims**（通用分隔符，9 个） | `: / ? # [ ] @` | 划分 URL 主结构 |
| **sub-delims**（子分隔符，11 个） | `! $ & ' ( ) * + , ; =` | 组件内部再细分 |

### 2.3 其他字符（必须编码）

```text
空格  "  <  >  %  {  }  |  \  ^  `      以及所有非 ASCII 字符
```

> 💡 `%` 本身必须编码为 `%25`，否则解码器会把后面两位当成十六进制。

### 2.4 按位置的编码需求矩阵

| 字符 | Path 段 | Query 名/值 | Fragment | 说明 |
|:---:|:---:|:---:|:---:|------|
| `/` | ✅ 必编码 `%2F` | ⭕ 可不编码 | ⭕ | Path 里是层级分隔符 |
| `?` | ✅ `%3F` | ⭕ 可不编码 | ⭕ | 首个 `?` 开始 query |
| `#` | ✅ `%23` | ✅ `%23` | ✅ | 首个 `#` 开始 fragment |
| `&` | ⭕ | ✅ `%26` | ⭕ | query 参数分隔符 |
| `=` | ⭕ | ✅ `%3D` | ⭕ | query 键值分隔符 |
| `+` | ⭕ | ✅ `%2B` | ⭕ | **表单编码下代表空格** |
| 空格 | ✅ `%20` | ✅ `%20` 或 `+` | ✅ `%20` | 见第 4 节 |
| `;` | ⚠️ `%3B` | ⭕ | ⭕ | Spring 矩阵参数分隔符 |
| `:` | ⭕（首段除外） | ⭕ | ⭕ | 相对 URL 首段含 `:` 会被误判成 scheme |
| `%` | ✅ `%25` | ✅ `%25` | ✅ | 永远要编码 |

> ⭕ = 语法上允许原样出现，但编码后也合法且更安全。**保守做法：除非明确是分隔符，全部编码。**

---

## 3. 百分号编码机制

### 3.1 编码算法（三步）

```text
1. 把字符按 UTF-8 编码成字节序列
2. 每个字节 → "%" + 两位大写十六进制
3. 拼接
```

| 字符 | UTF-8 字节 | 编码结果 |
|:---:|-----------|---------|
| 空格 | `20` | `%20` |
| `&` | `26` | `%26` |
| `中` | `E4 B8 AD` | `%E4%B8%AD` |
| `文` | `E6 96 87` | `%E6%96%87` |
| `😀` | `F0 9F 98 80` | `%F0%9F%98%80` |
| `é` | `C3 A9` | `%C3%A9` |

```text
"中文" → %E4%B8%AD%E6%96%87        （UTF-8，现代标准）
"中文" → %D6%D0%CE%C4              （GBK，老系统遗留）
```

> 🎯 **一个汉字 UTF-8 占 3 字节 = 9 个 URL 字符**。中文 URL 极易超长，Nginx 请求行 8 KB 约等于 900 个汉字。

### 3.2 常用字符编码速查表

| 字符 | 编码 | 字符 | 编码 | 字符 | 编码 |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 空格 | `%20` | `!` | `%21` | `"` | `%22` |
| `#` | `%23` | `$` | `%24` | `%` | `%25` |
| `&` | `%26` | `'` | `%27` | `(` | `%28` |
| `)` | `%29` | `*` | `%2A` | `+` | `%2B` |
| `,` | `%2C` | `-` | 不编码 | `.` | 不编码 |
| `/` | `%2F` | `:` | `%3A` | `;` | `%3B` |
| `<` | `%3C` | `=` | `%3D` | `>` | `%3E` |
| `?` | `%3F` | `@` | `%40` | `[` | `%5B` |
| `\` | `%5C` | `]` | `%5D` | `^` | `%5E` |
| `_` | 不编码 | `` ` `` | `%60` | `{` | `%7B` |
| `|` | `%7C` | `}` | `%7D` | `~` | 不编码 |
| 换行 `\n` | `%0A` | 回车 `\r` | `%0D` | Tab | `%09` |

---

## 4. 空格与加号的世纪之坑

**存在两套并行标准**：

| 标准 | 空格编码 | 适用位置 | 出处 |
|------|:---:|---------|------|
| **RFC 3986（URI 标准）** | `%20` | Path、Fragment、通用 URI | RFC 3986 |
| **`application/x-www-form-urlencoded`（表单编码）** | `+` | HTML 表单提交、Query 串 | WHATWG / HTML 规范 |

### 4.1 后果推演

```text
请求：GET /search?kw=a+b
  • 若服务端按表单编码解析 → kw = "a b"    （+ 变空格）
  • 若服务端按 RFC 3986 解析 → kw = "a+b"  （+ 是字面量）

请求：GET /search?kw=a%2Bb
  • 两种解析下都是 kw = "a+b"              ✅ 唯一安全写法
```

### 4.2 各框架的实际行为

| 环境 | Query 中的 `+` | Path 中的 `+` |
|------|---------------|--------------|
| Spring MVC `@RequestParam` | 解成**空格** | 保持 `+` 字面量 |
| Servlet `request.getParameter()` | 解成**空格** | — |
| `java.net.URLDecoder` | 解成**空格** | 解成空格（危险！） |
| JS `URLSearchParams` | 解成**空格** | — |
| JS `decodeURIComponent` | 保持 `+` | 保持 `+` |
| Nginx `$arg_x` | 保持 `+` | 保持 `+` |
| Python `urllib.parse.parse_qs` | 解成**空格** | — |

> ⚠️ **实战踩坑**：Base64 字符串含 `+`，直接放 Query 会被解成空格 → 解码失败。解决：改用 **Base64URL**（第 9 节）或对值做 `encodeURIComponent`。

### 4.3 铁律

> 🎯 **凡是要放进 URL 的数据，一律用"编码 API"处理，把空格编成 `%20`、`+` 编成 `%2B`**。永远不要手写 `+` 表示空格。

---

## 5. JavaScript 编码 API 对比

| API | 不编码的字符 | 用途 |
|-----|-------------|------|
| `encodeURI()` | 非保留 + **保留字符全部** + `#` | 编码**整条 URL**（保留结构） |
| `encodeURIComponent()` | `A-Za-z0-9 - _ . ! ~ * ' ( )` | 编码**单个参数值/路径段** ✅ 常用 |
| `escape()` | — | ❌ **已废弃**，非 UTF-8，不要用 |
| `new URLSearchParams()` | 表单编码规则 | 拼装 Query（空格→`+`） |
| `new URL()` | 自动做最小必要编码 | 解析 + 拼装完整 URL |

### 5.1 对照实验

```javascript
const raw = "a b&c=d/e?f#g中文+x";

encodeURI(raw);
// "a%20b&c=d/e?f#g%E4%B8%AD%E6%96%87+x"   ← & = / ? # + 都没编码 ❌ 参数会被截断

encodeURIComponent(raw);
// "a%20b%26c%3Dd%2Fe%3Ff%23g%E4%B8%AD%E6%96%87%2Bx"   ✅ 全部安全

// 正确姿势：结构自己写，值用 encodeURIComponent
const url = `/search?kw=${encodeURIComponent(raw)}&page=1`;
```

### 5.2 `encodeURIComponent` 的 4 个漏网字符

```javascript
// ! ' ( ) * 属于 sub-delims，encodeURIComponent 不编码
// 在少数严格签名场景（如 OAuth 1.0、AWS SigV4）需要额外补编码
function strictEncode(s) {
  return encodeURIComponent(s)
    .replace(/[!'()*]/g, c => '%' + c.charCodeAt(0).toString(16).toUpperCase());
}
strictEncode("a(b)!*'");  // "a%28b%29%21%2A%27"
```

### 5.3 推荐写法：用 `URL` + `URLSearchParams`

```javascript
const u = new URL("https://api.example.com/v1/search");
u.searchParams.set("kw", "a b&c=d");
u.searchParams.set("page", "2");
u.searchParams.append("tag", "AI");
u.searchParams.append("tag", "URL");

console.log(u.toString());
// https://api.example.com/v1/search?kw=a+b%26c%3Dd&page=2&tag=AI&tag=URL
//                                        ↑ 注意空格是 +（表单编码）

// 读取
u.searchParams.get("kw");       // "a b&c=d"
u.searchParams.getAll("tag");   // ["AI", "URL"]
```

> 💡 `URLSearchParams` 用 `+` 表示空格。若后端严格按 RFC 3986 解析（把 `+` 当字面量），需手动 `.toString().replace(/\+/g, "%20")`。

---

## 6. Java 编码 API 对比

| API | 规则 | 空格 | `~` | 适用 |
|-----|------|:---:|:---:|------|
| `URLEncoder.encode()` | **表单编码**（不是 URI 编码！） | `+` | `%7E` | 只适合 Query 值 |
| `URLDecoder.decode()` | 表单解码 | `+`→空格 | — | 只适合 Query 值 |
| `URI` 多参构造器 | RFC 3986 自动编码 | `%20` | 保留 | 拼装 URI |
| `URI.create()` 单参 | **不编码**，非法字符抛异常 | — | — | 解析已编码 URI |
| `UriUtils.encodePath()`（Spring） | RFC 3986 Path 规则 | `%20` | 保留 | ✅ 推荐 |
| `UriUtils.encodeQueryParam()`（Spring） | RFC 3986 Query 规则 | `%20` | 保留 | ✅ 推荐 |
| `UriComponentsBuilder` | 按组件分别编码 | `%20` | 保留 | ✅ 最推荐 |
| Guava `UrlEscapers` | 按位置分 3 种 escaper | 见下 | 保留 | ✅ 推荐 |
| OkHttp `HttpUrl.Builder` | 自动按位置编码 | `%20` | 保留 | ✅ 推荐 |

### 6.1 `URLEncoder` 的历史包袱

```java
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

String s = "a b+c~d/e";

URLEncoder.encode(s, StandardCharsets.UTF_8);
// "a+b%2Bc%7Ed%2Fe"
//   ↑空格变+   ↑ ~ 被多余编码

// ❌ 错误用法：拿它编码路径
String path = "/files/" + URLEncoder.encode("my file.txt", StandardCharsets.UTF_8);
// "/files/my+file.txt"  ← 服务端拿到的文件名是 "my+file.txt"，找不到文件

// ✅ 修法一：替换 + 为 %20
String fixed = URLEncoder.encode("my file.txt", StandardCharsets.UTF_8)
                         .replace("+", "%20");
// ✅ 修法二（推荐）：用专门的 Path 编码器
String better = org.springframework.web.util.UriUtils
                   .encodePathSegment("my file.txt", StandardCharsets.UTF_8);
// "my%20file.txt"
```

> ⚠️ `URLEncoder` 的类名极具误导性——它实现的是 **HTML 表单编码**（`application/x-www-form-urlencoded`），不是 RFC 3986 URI 编码。**只在编码 Query 参数值时可用**。
>
> 💡 Java 10+ 支持 `URLEncoder.encode(s, StandardCharsets.UTF_8)` 重载，无需再 catch `UnsupportedEncodingException`。

### 6.2 `URI` 双构造器行为差异

```java
// 单参构造：假设输入已编码，遇到非法字符直接抛异常
new URI("https://a.com/my file.txt");
// ❌ URISyntaxException: Illegal character in path at index 20

URI.create("https://a.com/my%20file.txt");   // ✅ OK

// 多参构造：自动帮你编码
URI u = new URI("https", "a.com", "/my file.txt", "kw=a b", "frag ment");
System.out.println(u.toASCIIString());
// https://a.com/my%20file.txt?kw=a%20b#frag%20ment   ✅ 自动编码

// 但多参构造不会编码保留字符 —— 值里的 & 依然会破坏结构
URI bad = new URI("https", "a.com", "/p", "kw=A&B=C", null);
// https://a.com/p?kw=A&B=C   ← 仍然被拆成 2 个参数 ⚠️
```

### 6.3 raw vs decoded 取值

```java
URI u = URI.create("https://a.com/%E4%B8%AD%E6%96%87/a%2Fb?kw=%E4%B8%AD#f%20g");

u.getPath();      // /中文/a/b        ← 解码后，%2F 变成了真 /，层级信息丢失！
u.getRawPath();   // /%E4%B8%AD%E6%96%87/a%2Fb   ← 原始，安全
u.getQuery();     // kw=中
u.getRawQuery();  // kw=%E4%B8%AD
u.getFragment();  // f g
u.getRawFragment; // f%20g
```

> ⚠️ **安全要点**：做路径校验（防穿越）必须用 `getRawPath()` 并自行解码判断。用 `getPath()` 会让 `%2F`、`%2e%2e` 提前解码，绕过你的检查。

### 6.4 Guava 三种 Escaper

```java
import com.google.common.net.UrlEscapers;

UrlEscapers.urlPathSegmentEscaper().escape("my file/a");   // "my%20file%2Fa"
UrlEscapers.urlFormParameterEscaper().escape("my file/a"); // "my+file%2Fa"
UrlEscapers.urlFragmentEscaper().escape("my file/a");      // "my%20file/a"
```

### 6.5 首选方案：`UriComponentsBuilder`

```java
import org.springframework.web.util.UriComponentsBuilder;

URI uri = UriComponentsBuilder
        .fromUriString("https://api.example.com")
        .path("/v1/users/{name}/files/{file}")
        .queryParam("kw", "A&B=C")
        .queryParam("tag", "AI", "URL")          // 重复 key
        .fragment("top")
        .buildAndExpand("张 三", "my file.txt")   // 占位符值自动编码
        .encode(StandardCharsets.UTF_8)
        .toUri();

System.out.println(uri);
// https://api.example.com/v1/users/%E5%BC%A0%20%E4%B8%89/files/my%20file.txt
//   ?kw=A%26B%3DC&tag=AI&tag=URL#top
```

> 🎯 **核心要点**：Java 侧编码，**结论就一句——用 `UriComponentsBuilder`（有 Spring）或 OkHttp `HttpUrl.Builder`（无 Spring），永远别手动 `URLEncoder` + 字符串拼接**。

---

## 7. 中文与字符集问题排查

### 7.1 乱码链路全景

```text
浏览器/客户端                    Web 容器                      应用
  编码 URL  ────────────────►  解析请求行  ────────────────►  取参数
  (UTF-8?)                    (按什么字符集解码?)            (再解一次?)
     │                              │                          │
     └── 环节 A                     └── 环节 B                  └── 环节 C
```

| 环节 | 常见错误 | 排查手段 |
|:---:|---------|---------|
| A 编码端 | 用了 GBK / 完全没编码 | 抓包看 `%XX` 是 3 字节（UTF-8）还是 2 字节（GBK） |
| B 容器解码 | Tomcat `URIEncoding` 设成 ISO-8859-1 | 看 `server.tomcat.uri-encoding` |
| C 应用层 | 又调了一次 `URLDecoder` → 双重解码 | 打印 raw 值确认 |

### 7.2 关键配置

```yaml
# application.yml —— Spring Boot 默认已是 UTF-8，老项目需显式设置
server:
  tomcat:
    uri-encoding: UTF-8          # 影响 Path 与 GET Query 的解码
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true                # 强制请求与响应都用 UTF-8
```

```nginx
# Nginx 不解码 URL，原样透传；但日志里想看中文需要
# （Nginx 默认已按字节透传，一般无需配置）
location / {
    proxy_pass http://backend;
    proxy_set_header Host $host;
}
```

### 7.3 UTF-8 vs GBK 快速判别

```text
"中文" 的 URL 编码：
  UTF-8 → %E4%B8%AD%E6%96%87    （每字 3 段，常以 %E 开头）
  GBK   → %D6%D0%CE%C4          （每字 2 段，常以 %D/%C 开头）
```

```java
// 抢救 GBK 编码的老 URL
String garbled = new String(raw.getBytes(StandardCharsets.ISO_8859_1),
                            Charset.forName("GBK"));
```

### 7.4 中文域名 / 中文路径可行性

| 位置 | 中文支持 | 说明 |
|------|:---:|------|
| Host（中文域名） | ⚠️ 需 Punycode | 见第 8 节 |
| Path | ✅ 需百分号编码 | `/文档/入门` → `/%E6%96%87%E6%A1%A3/%E5%85%A5%E9%97%A8` |
| Query 值 | ✅ 需百分号编码 | 最常见场景 |
| Fragment | ✅ | 浏览器自动处理 |

> 💡 中文 URL 在浏览器地址栏**显示为中文**（美化），复制出来是编码形态。SEO 上中文 URL 对中文搜索引擎有正向作用，但会显著增加 URL 长度、易在第三方系统被截断。**技术型站点建议用英文 slug 或拼音。**

---

## 8. IDN 国际化域名与 Punycode

DNS 协议只认 **LDH 字符集**（Letter、Digit、Hyphen），所以中文域名必须先转成 ASCII。

### 8.1 Punycode（RFC 3492）

```text
münchen.de       →  xn--mnchen-3ya.de
例子.测试         →  xn--fsqu00a.xn--0zwm56d
```

| 特征 | 说明 |
|------|------|
| 前缀 `xn--` | ACE 前缀（ASCII Compatible Encoding），标识这是编码后的标签 |
| **逐标签转换** | 只有含非 ASCII 的标签才转，`.de` 保持原样 |
| 与百分号编码无关 | Host 用 Punycode，Path/Query 用百分号编码，**两套机制** |

### 8.2 Java 处理

```java
import java.net.IDN;

IDN.toASCII("münchen.de");            // "xn--mnchen-3ya.de"
IDN.toUnicode("xn--mnchen-3ya.de");   // "münchen.de"

// 严格模式：拒绝非法字符（推荐用于安全校验）
IDN.toASCII("mü nchen.de", IDN.USE_STD3_ASCII_RULES);  // 抛 IllegalArgumentException
```

```javascript
// 浏览器 URL 对象自动做 Punycode
new URL("https://münchen.de/p").hostname;   // "xn--mnchen-3ya.de"
new URL("https://münchen.de/p").href;       // "https://xn--mnchen-3ya.de/p"
```

### 8.3 同形异义攻击（Homograph Attack）

```text
真域名：apple.com        （全 ASCII）
假域名：аpple.com        （首字母是西里尔字母 а，U+0430）
        → Punycode: xn--pple-43d.com
```

| 防御 | 说明 |
|------|------|
| 浏览器策略 | 混合脚本域名强制显示 Punycode 形态 |
| 服务端校验 | 输入域名先 `IDN.toASCII`，再与白名单比对**编码后**的值 |
| 混合脚本检测 | 同一标签内出现多个 Unicode Script 时告警 |

> ⚠️ 做 SSRF / 重定向白名单时，**必须比对 Punycode 形态**。若直接比对 Unicode 字符串，`аpple.com`（西里尔）会被误判为不在白名单从而放过，或反之绕过检查。

---

## 9. Base64 与 Base64URL

标准 Base64 的字母表含 `+`、`/`、`=`，全都是 URL 敏感字符。

| 编码 | 62/63 号字符 | 填充 | URL 安全 |
|------|-------------|:---:|:---:|
| **Base64**（RFC 4648 §4） | `+` `/` | `=` | ❌ |
| **Base64URL**（RFC 4648 §5） | `-` `_` | 可省略 | ✅ |

```text
标准 Base64：  aGVsbG8+d29ybGQ/PT0=
Base64URL：   aGVsbG8-d29ybGQ_PT0
              替换：+ → -    / → _    去掉尾部 =
```

### 9.1 Java 实现

```java
import java.util.Base64;

byte[] data = "hello>world?".getBytes(StandardCharsets.UTF_8);

// ❌ 标准 Base64 —— 含 + / = 放到 URL 会出问题
String std = Base64.getEncoder().encodeToString(data);
// "aGVsbG8+d29ybGQ/"

// ✅ Base64URL —— 直接可放 URL
String url = Base64.getUrlEncoder().withoutPadding().encodeToString(data);
// "aGVsbG8-d29ybGQ_"

byte[] back = Base64.getUrlDecoder().decode(url);
```

### 9.2 典型应用

| 场景 | 为什么用 Base64URL |
|------|-------------------|
| **JWT** | 三段 `header.payload.signature` 全用 Base64URL，天然可放 URL/Header |
| 分页游标 Cursor | 把 `{"lastId":123}` 编码成不透明字符串放 Query |
| 短链 ID | Base62（`0-9a-zA-Z`）比 Base64URL 更保守，连 `-_` 都不用 |
| 图片 Data URL | `data:image/png;base64,...` 用**标准** Base64（不在 Query 里，`+/` 安全） |
| 签名值 | HMAC 结果转 Base64URL 放 `?sig=` |

```java
// JWT 三段结构示意
String jwt = base64Url(headerJson) + "." + base64Url(payloadJson) + "." + base64Url(hmac);
// eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

> 💡 **速判**：见到含 `-`/`_` 且**无** `+`/`/`/`=` 的 Base64 串，基本就是 Base64URL（JWT、OAuth state、签名）。

---

## 10. 双重编码与编码陷阱

### 10.1 双重编码（Double Encoding）

```text
原文：  中文
一次：  %E4%B8%AD%E6%96%87
两次：  %25E4%25B8%25AD%25E6%2596%2587      （% 本身又被编码成 %25）
```

**产生原因**：

| 场景 | 说明 |
|------|------|
| 前端编码 + 后端转发时又编码 | 网关 / SDK 重复处理 |
| 已编码的 URL 当作参数值再拼接 | 这种情况**应该**双重编码，是正确的 |
| `UriComponentsBuilder` 上重复调 `.encode()` | Spring 常见坑 |

```java
// ⚠️ Spring 常见双重编码坑
UriComponentsBuilder.fromUriString("https://a.com")
        .queryParam("kw", URLEncoder.encode("中文", UTF_8))  // 已编码一次
        .encode()                                            // ❌ 又编一次
        .toUriString();
// https://a.com?kw=%25E4%25B8%25AD%25E6%2596%2587

// ✅ 正确：交给 builder 统一编码，自己不要提前编
UriComponentsBuilder.fromUriString("https://a.com")
        .queryParam("kw", "中文")
        .encode()
        .toUriString();
// https://a.com?kw=%E4%B8%AD%E6%96%87
```

### 10.2 URL 当参数值传递（回调地址 / redirect_uri）

这是**必须**双重编码的场景：

```text
callback = https://app.com/cb?token=abc&from=wx

❌ 直接拼：
/oauth?redirect_uri=https://app.com/cb?token=abc&from=wx
   → from=wx 变成了外层 oauth 的参数，回调地址被截断

✅ 编码后拼：
/oauth?redirect_uri=https%3A%2F%2Fapp.com%2Fcb%3Ftoken%3Dabc%26from%3Dwx
   → 服务端 getParameter("redirect_uri") 得到完整回调地址
```

```java
String callback = "https://app.com/cb?token=abc&from=wx";
String oauthUrl = "https://auth.com/oauth?client_id=X&redirect_uri="
        + URLEncoder.encode(callback, StandardCharsets.UTF_8);
```

### 10.3 编码陷阱清单

| # | 陷阱 | 后果 | 正解 |
|:---:|------|------|------|
| 1 | 对整条 URL 调 `encodeURIComponent` | `://` 被编码，URL 失效 | 只编码值 |
| 2 | 对整条 URL 调 `encodeURI` | 参数值里的 `&=` 未编码，参数截断 | 只编码值 |
| 3 | 用 `URLEncoder` 编码 Path | 空格变 `+`，找不到资源 | `UriUtils.encodePathSegment` |
| 4 | 忘了编码 `%` 本身 | 解码时把后两位吃掉 | 编码器会自动处理 |
| 5 | `getPath()` 而非 `getRawPath()` 做安全校验 | `%2F`/`%2e%2e` 绕过检查 | 用 raw + 自行解码 |
| 6 | 解码次数与编码次数不匹配 | 乱码或 `%25` 残留 | 全链路只编解一次 |
| 7 | 签名前后编码规则不一致（`~`、`*`） | 签名校验失败 | 双方统一用同一 escaper |
| 8 | Base64 直接放 Query | `+` 变空格，解码报错 | Base64URL |
| 9 | 白名单比对 Unicode 域名 | 同形异义绕过 | 先 `IDN.toASCII` |
| 10 | 用正则替换代替编码器 | 漏字符 / 过度编码 | 用标准库 |

---

> 🎯 **本篇核心要点**
> 1. **编码的对象是"值"，不是整条 URL**
> 2. 空格有两套标准：URI 用 `%20`，表单用 `+`；**统一编成 `%20` 最安全**
> 3. JS 用 `encodeURIComponent` / `URLSearchParams`；Java 用 `UriComponentsBuilder`，**别用 `URLEncoder` 编路径**
> 4. 安全校验一律基于 `getRawPath()`，域名白名单一律基于 Punycode
> 5. 放 URL 的二进制/签名用 **Base64URL**，不用标准 Base64

---

**上一模块**：[01-URL基础概念与组成结构.md](01-URL基础概念与组成结构.md) / **下一模块**：[03-URL协议族Scheme全解析.md](03-URL协议族Scheme全解析.md)
