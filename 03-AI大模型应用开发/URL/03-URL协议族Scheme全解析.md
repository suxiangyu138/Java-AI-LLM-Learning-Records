# 03 - URL 协议族 Scheme 全解析

> 40+ 常见 Scheme 分类速查——从 `http/ws` 网络协议到 `data:/blob:` 客户端协议、从 `jdbc:/redis:` 中间件连接串到移动端 DeepLink 与 IDE 唤起

---

## 📚 目录

1. [Scheme 全景分类](#1-scheme-全景分类)
2. [Web 网络协议族](#2-web-网络协议族)
3. [WebSocket 与实时通信](#3-websocket-与实时通信)
4. [本地与内嵌资源协议](#4-本地与内嵌资源协议)
5. [中间件与数据库连接串](#5-中间件与数据库连接串)
6. [通信与设备唤起协议](#6-通信与设备唤起协议)
7. [自定义 Scheme 与 DeepLink](#7-自定义-scheme-与-deeplink)
8. [Java 中的特殊 URL 协议](#8-java-中的特殊-url-协议)

---

## 1. Scheme 全景分类

```text
Scheme 协议族
│
├── 🌐 Web 网络类       http、https、ftp、ftps、sftp、gopher、dict
├── 🔌 实时通信类       ws、wss、mqtt、mqtts、stomp、sse（非独立scheme）
├── 💾 本地资源类       file、data、blob、about、chrome、jar、classpath
├── 🗄️ 数据存储类       jdbc、mongodb、redis、mysql、postgresql、s3、hdfs
├── 📨 消息中间件类     amqp、amqps、kafka（非标准）、nats、pulsar
├── ☎️ 设备唤起类       mailto、tel、sms、facetime、geo、maps
├── 🔗 P2P/下载类       magnet、ed2k、thunder、bittorrent
├── 📦 版本控制类       git、git+ssh、svn、svn+ssh
├── 📱 应用唤起类       myapp、weixin、alipay、taobao、vscode、idea
└── 🛠️ 开发工具类       npm、pip、docker、oci、vscode-remote
```

| Scheme 注册状态 | 说明 | 例子 |
|---------------|------|------|
| **Permanent**（永久注册） | IANA 正式登记，规范明确 | `http`、`https`、`mailto`、`file`、`ws` |
| **Provisional**（临时注册） | IANA 已登记但规范较松 | `redis`、`mongodb`、`magnet` |
| **Private/Unregistered** | 厂商自用，无 IANA 登记 | `weixin`、`vscode`、`jdbc`、`myapp` |

> 💡 IANA 官方注册表：`https://www.iana.org/assignments/uri-schemes/uri-schemes.xhtml`（约 380 个已注册 Scheme）

---

## 2. Web 网络协议族

### 2.1 http / https

| 维度 | `http` | `https` |
|------|--------|---------|
| 默认端口 | 80 | 443 |
| 传输 | 明文 | TLS 加密 |
| 同源判定 | 与 https 版**不同源** | — |
| 浏览器特性限制 | 大量 API 被禁 | 完整可用 |

**HTTPS-only 才能用的浏览器 API（Secure Context 要求）**：

```text
Service Worker、Web Push、Geolocation、getUserMedia（摄像头/麦克风）、
Web Crypto subtle、Clipboard API、WebAuthn/Passkey、HTTP/2、HTTP/3、
Web Bluetooth、Payment Request、Storage 持久化
```

> ⚠️ **例外**：`http://localhost` 与 `http://127.0.0.1` 被浏览器视为 Secure Context，本地开发无需 HTTPS。但 `http://192.168.1.5`（局域网 IP）**不是**——手机调试摄像头功能会失败，需自签证书或用 `ngrok`/`localtunnel` 反代。

**HSTS 强制升级**：

```http
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

| 效果 | 说明 |
|------|------|
| 浏览器内部改写 | 用户输 `http://a.com` 会在**发请求前**改成 `https://` |
| 不可点击忽略 | 证书错误无法"继续访问" |
| `preload` | 提交到 Chrome 预置列表，首次访问即生效 |

### 2.2 ftp / ftps / sftp

| Scheme | 端口 | 加密 | 浏览器支持 |
|--------|:---:|------|:---:|
| `ftp` | 21 | 无 | ❌ Chrome 88+ / Firefox 90+ **已移除** |
| `ftps` | 990 / 21 | TLS | ❌ |
| `sftp` | 22 | SSH | ❌（非 IANA 注册，仅客户端工具认） |

```text
ftp://user:pass@files.example.com:21/pub/docs/readme.txt
sftp://deploy@10.0.0.5:22/var/www/html/          ← FileZilla / WinSCP 可识别
```

> 💡 `sftp` 与 `ftps` 完全不同：`sftp` 是 SSH 子系统（单端口 22），`ftps` 是 FTP + TLS（需要多端口，NAT 不友好）。生产环境一律选 `sftp`。

### 2.3 已淘汰但在安全领域重要的 Scheme

| Scheme | 端口 | 为何要知道 |
|--------|:---:|-----------|
| `gopher://` | 70 | **SSRF 万能利用协议**——可构造任意 TCP 字节流，打 Redis/MySQL/FastCGI |
| `dict://` | 2628 | SSRF 探测内网端口与服务指纹 |
| `ldap://` | 389 | JNDI 注入（Log4Shell `${jndi:ldap://...}`） |
| `rmi://` | 1099 | JNDI 注入另一路径 |

```text
# SSRF 利用示例（仅用于理解防御，需授权环境）
gopher://127.0.0.1:6379/_SET%20key%20value%0d%0a
   → 通过 SSRF 向内网 Redis 写数据
```

> ⚠️ **防御结论**：任何接受用户输入 URL 的功能，**Scheme 必须白名单**，只允许 `http`/`https`。详见 06 篇。

---

## 3. WebSocket 与实时通信

### 3.1 ws / wss

```text
wss://gateway.example.com:443/socket?token=abc&room=42
└┬┘  └────────┬────────────┘└┬┘└──┬──┘└───────┬──────┘
scheme      host           port  path       query
```

| 特性 | 说明 |
|------|------|
| 默认端口 | `ws` 80、`wss` 443（与 http/https 相同） |
| **无 Fragment** | RFC 6455 规定 WebSocket URI **不允许** `#fragment` |
| 握手 | 先发 HTTP `GET` + `Upgrade: websocket`，101 后切协议 |
| 鉴权 | 浏览器 WebSocket API **不能自定义 Header** → token 只能放 Query 或用子协议 |

```java
// Java 11+ 原生 WebSocket 客户端（可以设 Header，比浏览器灵活）
HttpClient.newHttpClient()
    .newWebSocketBuilder()
    .header("Authorization", "Bearer " + token)     // ✅ Java 可以
    .buildAsync(URI.create("wss://gw.example.com/socket"), listener);
```

```javascript
// 浏览器只能把 token 放 Query（⚠️ 会进服务端日志）
const ws = new WebSocket(`wss://gw.example.com/socket?token=${token}`);

// 折中方案：用 Sec-WebSocket-Protocol 携带（服务端需配合）
const ws2 = new WebSocket("wss://gw.example.com/socket", ["bearer", token]);
```

> ⚠️ **WebSocket 鉴权坑**：token 放 Query 会被 Nginx access_log 记录。生产做法：连接后**首帧发送鉴权消息**，服务端未鉴权前不推数据、超时断开。

### 3.2 SSE 与 MQTT

| 协议 | Scheme | 说明 |
|------|--------|------|
| **SSE**（Server-Sent Events） | 无独立 scheme，用 `https://` | `Content-Type: text/event-stream`，单向推送，LLM 流式输出主力 |
| **MQTT** | `mqtt://` 1883、`mqtts://` 8883 | IoT 标准 |
| **MQTT over WS** | `ws://host/mqtt`、`wss://host/mqtt` | 浏览器接 MQTT 唯一方式 |
| **STOMP over WS** | `ws://host/stomp` | Spring `@MessageMapping` 常用 |

```text
# LLM 流式输出就是 SSE —— 普通 HTTPS URL + 特殊响应头
POST https://api.deepseek.com/v1/chat/completions
Body: {"model":"deepseek-chat","stream":true,...}
Resp: Content-Type: text/event-stream
      data: {"choices":[{"delta":{"content":"你"}}]}
      data: {"choices":[{"delta":{"content":"好"}}]}
      data: [DONE]
```

---

## 4. 本地与内嵌资源协议

### 4.1 file:// —— 三斜杠之谜

```text
file:///C:/Users/tom/a.txt        ← Windows（空 host + /C:/...）
file:///home/tom/a.txt            ← Linux/macOS
file://server/share/a.txt         ← UNC 网络共享（host = server）
file:///C:/dir/my%20file.txt      ← 空格必须编码
```

| 语法 | host | 含义 |
|------|------|------|
| `file:///path` | 空 | 本机（**标准写法，三斜杠**） |
| `file://localhost/path` | localhost | 本机（等价） |
| `file://host/path` | host | 远程 SMB/UNC 共享 |
| `file:/path` | 无 authority | 部分实现容忍，不规范 |

```java
// Java 中 Path 与 file URI 互转
Path p = Paths.get("C:\\Users\\tom\\a b.txt");
URI  u = p.toUri();          // file:///C:/Users/tom/a%20b.txt
Path back = Paths.get(u);    // 还原

// ⚠️ 不要手拼字符串，Windows 反斜杠与空格必踩坑
```

> ⚠️ **安全**：`file://` 是 SSRF 读取任意文件的经典通道（`file:///etc/passwd`）。所有用户可控 URL 必须禁掉。

### 4.2 data:// —— 内嵌数据

```text
data:[<mediatype>][;charset=xxx][;base64],<data>
```

| 示例 | 说明 |
|------|------|
| `data:,Hello` | 默认 `text/plain;charset=US-ASCII` |
| `data:text/plain;charset=UTF-8,%E4%B8%AD%E6%96%87` | 百分号编码文本 |
| `data:text/html,<h1>Hi</h1>` | ⚠️ XSS 载体 |
| `data:image/png;base64,iVBORw0KGgo...` | 内嵌图片（最常见） |
| `data:application/json;base64,eyJhIjoxfQ==` | 内嵌 JSON |

```html
<!-- 小图标内嵌，省一次 HTTP 请求 -->
<img src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0i...">

<!-- 前端下载文件，无需后端 -->
<a href="data:text/csv;charset=UTF-8,name%2Cage%0Atom%2C18" download="a.csv">下载</a>
```

| 优点 | 缺点 |
|------|------|
| 省 HTTP 请求，无跨域 | Base64 体积 **+33%** |
| 离线可用 | 不能被浏览器/CDN 单独缓存 |
| 适合 < 4 KB 小图标 | 大文件会拖慢首屏解析 |
| — | ⚠️ `data:text/html` 可执行脚本 → XSS |

```javascript
// JS 生成 data URL
const dataUrl = "data:application/json;base64," + btoa(JSON.stringify({a:1}));

// 中文需先转 UTF-8 再 btoa（btoa 只吃 Latin-1）
const utf8DataUrl = "data:text/plain;charset=UTF-8;base64," +
  btoa(String.fromCharCode(...new TextEncoder().encode("中文")));
```

> ⚠️ 现代浏览器**禁止顶层导航到 `data:` URL**（防钓鱼），只能用于 `<img>`、`<script>`、CSS 等子资源。

### 4.3 blob:// —— 内存对象引用

```text
blob:https://example.com/550e8400-e29b-41d4-a716-446655440000
     └──────── origin ────────┘└──────────── UUID ────────────┘
```

| 特性 | 说明 |
|------|------|
| 仅在**当前文档生命周期**内有效 | 刷新即失效 |
| 绑定 origin | 跨标签页/跨域无法访问 |
| **不含数据本体** | 只是指向内存/磁盘 Blob 的句柄 |
| 必须手动释放 | `URL.revokeObjectURL()`，否则内存泄漏 |

```javascript
// 大文件预览 / 前端导出（比 data: 高效得多，无 Base64 膨胀）
const blob = new Blob([csvText], { type: "text/csv;charset=utf-8" });
const url  = URL.createObjectURL(blob);

const a = document.createElement("a");
a.href = url;
a.download = "report.csv";
a.click();

URL.revokeObjectURL(url);   // ✅ 必须释放

// 视频/音频流播放
videoEl.src = URL.createObjectURL(mediaStream);
```

| 场景 | 选 `data:` | 选 `blob:` |
|------|:---:|:---:|
| 小图标（< 4 KB） | ✅ | — |
| 需要持久化到 CSS/HTML | ✅ | ❌ |
| 大文件下载 / 视频流 | ❌ 内存爆 | ✅ |
| Canvas 导出图片 | 小图 ✅ | 大图 ✅ |

### 4.4 浏览器内部 Scheme

| Scheme | 用途 |
|--------|------|
| `about:blank` | 空白页（`window.open` 默认值） |
| `about:srcdoc` | iframe `srcdoc` 属性文档的 origin |
| `chrome://settings` | Chrome 内部页（`edge://`、`brave://` 类似） |
| `devtools://` | DevTools 自身 |
| `view-source:https://a.com` | 查看源码 |
| `javascript:void(0)` | ⚠️ 伪协议，XSS 载体，现代代码不应使用 |

---

## 5. 中间件与数据库连接串

这些不是标准 URI，但形态借用了 URL 语法，是后端每天都在写的。

### 5.1 JDBC 连接串

```text
jdbc:<subprotocol>://<host>:<port>/<database>?<params>
```

| 数据库 | 连接串 |
|--------|--------|
| MySQL 8 | `jdbc:mysql://127.0.0.1:3306/mall?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false` |
| PostgreSQL | `jdbc:postgresql://127.0.0.1:5432/mall?currentSchema=public` |
| Oracle（SID） | `jdbc:oracle:thin:@127.0.0.1:1521:orcl` |
| Oracle（Service） | `jdbc:oracle:thin:@//127.0.0.1:1521/orclpdb` |
| SQL Server | `jdbc:sqlserver://127.0.0.1:1433;databaseName=mall;encrypt=true` ⚠️ 分号分隔 |
| H2 内存 | `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1` |
| SQLite | `jdbc:sqlite:/data/app.db` |
| ClickHouse | `jdbc:clickhouse://127.0.0.1:8123/default` |
| Doris / StarRocks | `jdbc:mysql://127.0.0.1:9030/db`（兼容 MySQL 协议） |

> ⚠️ **`jdbc:` 不是合法 URI 的 hier-part**——`jdbc:mysql://...` 中 scheme 是 `jdbc`，剩下 `mysql://...` 是 opaque part。所以 `URI.create(jdbcUrl).getHost()` 返回 `null`，必须先剥掉 `jdbc:` 前缀再解析。

```java
// 解析 JDBC URL 拿 host/port/db
String jdbcUrl = "jdbc:mysql://10.0.0.5:3306/mall?useSSL=false";
URI u = URI.create(jdbcUrl.substring(5));   // 剥掉 "jdbc:"
u.getScheme();  // mysql
u.getHost();    // 10.0.0.5
u.getPort();    // 3306
u.getPath();    // /mall
```

**MySQL 连接串关键参数**：

| 参数 | 推荐值 | 作用 |
|------|-------|------|
| `characterEncoding` | `utf8` | 避免中文乱码（配合库表 utf8mb4） |
| `serverTimezone` | `Asia/Shanghai` | 否则时间差 8 小时（MySQL 8 必填） |
| `useSSL` | 内网 `false` | 省握手开销；公网必须 `true` |
| `rewriteBatchedStatements` | `true` | 批量插入性能提升数十倍 |
| `allowMultiQueries` | **`false`** | ⚠️ 开了会放大 SQL 注入危害 |
| `allowPublicKeyRetrieval` | 按需 | MySQL 8 `caching_sha2_password` 报错时用 |
| `connectTimeout` / `socketTimeout` | `3000` / `60000` | 防连接悬挂 |

### 5.2 Redis / MongoDB / 消息队列

| 中间件 | 连接串 |
|--------|--------|
| Redis 单机 | `redis://:password@127.0.0.1:6379/0` |
| Redis TLS | `rediss://:password@r.example.com:6380/0` |
| Redis Sentinel | `redis-sentinel://host1:26379,host2:26379/mymaster/0` |
| MongoDB | `mongodb://user:pwd@127.0.0.1:27017/mall?authSource=admin` |
| MongoDB Atlas | `mongodb+srv://user:pwd@cluster0.xxx.mongodb.net/mall` ⚠️ 走 DNS SRV，无端口 |
| RabbitMQ | `amqp://guest:guest@127.0.0.1:5672/%2F` ⚠️ 默认 vhost `/` 要编码成 `%2F` |
| RabbitMQ TLS | `amqps://user:pwd@mq.example.com:5671/prod` |
| Elasticsearch | `http://elastic:pwd@127.0.0.1:9200` |
| Kafka | `kafka-1:9092,kafka-2:9092` ⚠️ **不是 URL**，是逗号分隔的 host:port 列表 |
| Nacos | `nacos://127.0.0.1:8848`（Dubbo 注册中心写法） |
| ZooKeeper | `zookeeper://127.0.0.1:2181` |
| MinIO / S3 | `s3://my-bucket/path/to/obj`（SDK 内部 URI） |
| HDFS | `hdfs://namenode:8020/user/hive/warehouse` |

> ⚠️ **RabbitMQ 默认 vhost 坑**：vhost 就是 `/`，作为 path 时必须编码为 `%2F`，写成 `amqp://host:5672/` 表示 vhost 为**空串**，连接会失败。

### 5.3 密码里的特殊字符必须编码

```text
密码：p@ss:w/rd#1

❌ redis://:p@ss:w/rd#1@127.0.0.1:6379/0
   → 解析器在第一个 @ 就断开，host 变成 "ss:w"，彻底解析错

✅ redis://:p%40ss%3Aw%2Frd%231@127.0.0.1:6379/0
```

| 密码中的字符 | 必须编码为 |
|:---:|:---:|
| `@` | `%40` |
| `:` | `%3A` |
| `/` | `%2F` |
| `#` | `%23` |
| `?` | `%3F` |
| `%` | `%25` |

> 💡 **最佳实践**：连接串里的密码交给配置中心/环境变量，用 `spring.datasource.password` 单独字段，**不要塞进 URL**，从根上避免编码问题。

---

## 6. 通信与设备唤起协议

| Scheme | 语法 | 效果 |
|--------|------|------|
| `mailto:` | `mailto:a@b.com?cc=c@d.com&subject=标题&body=正文` | 唤起邮件客户端 |
| `tel:` | `tel:+8613800138000` | 唤起拨号盘 |
| `sms:` | `sms:+8613800138000?body=你好` | 唤起短信（iOS 用 `&body=`） |
| `geo:` | `geo:31.2304,121.4737?z=16` | 唤起地图 |
| `maps:` | `maps://?q=上海外滩` | iOS 地图 |
| `facetime:` | `facetime:a@b.com` | iOS 视频通话 |
| `magnet:` | `magnet:?xt=urn:btih:<hash>&dn=name&tr=<tracker>` | 唤起 BT 客户端 |

```html
<!-- mailto 参数需 encodeURIComponent -->
<a href="mailto:support@example.com?subject=%E5%B7%A5%E5%8D%95%23123&body=%E9%97%AE%E9%A2%98%3A">
  联系客服
</a>

<!-- tel 用国际格式最稳，不要带空格/横线 -->
<a href="tel:+8613800138000">拨打</a>
```

> ⚠️ `mailto:` 的 `subject`/`body` 必须 `encodeURIComponent`，换行用 `%0D%0A`。未编码的 `&` 会截断后续参数。

---

## 7. 自定义 Scheme 与 DeepLink

### 7.1 三代唤起方案对比

| 方案 | 形态 | 平台 | 优点 | 缺点 |
|------|------|:---:|------|------|
| **URL Scheme** | `myapp://order/42` | iOS / Android | 简单，兼容老系统 | 未装 App 报错；可被抢注（钓鱼） |
| **Universal Links** | `https://a.com/order/42` | iOS 9+ | 未装 App 自动落网页；不可抢注 | 需部署 `apple-app-site-association` |
| **App Links** | `https://a.com/order/42` | Android 6+ | 同上 | 需部署 `assetlinks.json` + `android:autoVerify` |

### 7.2 Universal Links / App Links 配置

```json
// https://a.com/.well-known/apple-app-site-association   （无 .json 后缀！Content-Type: application/json）
{
  "applinks": {
    "details": [{
      "appIDs": ["ABCDE12345.com.example.myapp"],
      "components": [
        { "/": "/order/*", "comment": "订单详情" },
        { "/": "/promo/*", "exclude": true }
      ]
    }]
  }
}
```

```json
// https://a.com/.well-known/assetlinks.json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.example.myapp",
    "sha256_cert_fingerprints": ["14:6D:E9:83:C5:73:06:50:..."]
  }
}]
```

```xml
<!-- AndroidManifest.xml -->
<activity android:name=".MainActivity">
  <!-- 传统 Scheme -->
  <intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="myapp" android:host="order" />
  </intent-filter>
  <!-- App Links -->
  <intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="a.com" android:pathPrefix="/order" />
  </intent-filter>
</activity>
```

### 7.3 常见 App 的 Scheme

| App | Scheme 示例 |
|-----|------------|
| 微信 | `weixin://dl/scan`、`weixin://dl/moments` |
| 支付宝 | `alipays://platformapi/startapp?appId=xxx`、`alipayqr://...` |
| 淘宝 | `taobao://item.taobao.com/item.htm?id=xxx` |
| 抖音 | `snssdk1128://user/profile/xxx` |
| 高德 | `iosamap://path?dlat=31.2&dlon=121.4` |
| VS Code | `vscode://file/d:/project/a.java:42:8` |
| IDEA | `idea://open?file=/path/A.java&line=42` |
| JetBrains Toolbox | `jetbrains://idea/navigate/reference?project=X&path=A.java` |

```text
# VS Code 唤起并跳到指定行 —— 日志/异常栈可点击的原理
vscode://file/d:/Desktop/project/src/main/java/App.java:128:15
```

> ⚠️ **自定义 Scheme 的安全风险**：任何 App 都能注册 `myapp://`，恶意 App 抢注后可劫持 DeepLink 窃取 URL 里的 token。**结论：涉及凭证的跳转必须用 Universal Links / App Links（有域名归属验证），不要用裸 Scheme。**

---

## 8. Java 中的特殊 URL 协议

| Scheme | 用途 | 示例 |
|--------|------|------|
| `jar:` | 访问 JAR 内部条目 | `jar:file:/app/lib/a.jar!/META-INF/MANIFEST.MF` |
| `classpath:` | Spring 资源（非 JDK 标准） | `classpath:application.yml` |
| `classpath*:` | Spring 通配所有 classpath | `classpath*:META-INF/spring.factories` |
| `file:` | 文件系统 | `file:/opt/app/config.yml` |
| `http:` / `https:` | 网络资源 | — |

```java
// jar: 协议 —— ! 分隔归档与内部路径
URL u = new URI("jar:file:/app/lib/mysql.jar!/META-INF/services/java.sql.Driver").toURL();
try (InputStream in = u.openStream()) { /* 读 JAR 内文件 */ }

// Spring Resource 抽象统一各种 scheme
@Autowired ResourceLoader loader;
Resource r1 = loader.getResource("classpath:prompt/system.txt");
Resource r2 = loader.getResource("file:/data/knowledge.md");
Resource r3 = loader.getResource("https://cdn.example.com/schema.json");
String text = r1.getContentAsString(StandardCharsets.UTF_8);   // Spring 6+
```

**Java 20+ 弃用提醒**：

```java
// ❌ Java 20 起 URL 的所有 String 构造器被标记 @Deprecated
URL bad = new URL("https://a.com/p");

// ✅ 推荐写法
URL good = URI.create("https://a.com/p").toURL();
```

**自定义协议处理器（了解即可）**：

```java
// 注册自定义 scheme，如 s3://、oss://
public class MyHandler extends URLStreamHandler {
    protected URLConnection openConnection(URL u) { return new MyConnection(u); }
}
// 通过 -Djava.protocol.handler.pkgs=com.example.protocol 注册
// 或 Java 9+ 用 URLStreamHandlerProvider SPI（推荐）
```

---

> 🎯 **本篇核心要点**
> 1. 用户可控的 URL，**Scheme 必须白名单只留 `http`/`https`**——`file:`/`gopher:`/`dict:` 都是 SSRF 利器
> 2. `data:` 适合小图标（+33% 体积），`blob:` 适合大文件/流（需手动 `revokeObjectURL`）
> 3. `jdbc:` 的 host 用 `URI` 直接取是 `null`，要先剥前缀；**密码里的 `@:/#%` 必须编码**（或干脆别放 URL）
> 4. RabbitMQ 默认 vhost `/` 要写成 `%2F`；MySQL 8 必配 `serverTimezone`
> 5. 移动端唤起用 **Universal Links / App Links**，裸 Scheme 会被恶意 App 抢注

---

**上一模块**：[02-URL编码与字符集规范.md](02-URL编码与字符集规范.md) / **下一模块**：[04-URL路由与后端开发实战.md](04-URL路由与后端开发实战.md)
