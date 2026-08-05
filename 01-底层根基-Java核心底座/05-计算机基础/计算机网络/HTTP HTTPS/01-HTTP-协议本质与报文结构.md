# HTTP 协议本质与报文结构

> HTTP 是"用纯文本表达资源操作"的应用层协议：无状态、请求-响应、方法+URI+头部+可选主体。理解它的本质（无状态与文本化），才能理解为什么需要 Cookie、为什么有缓存、为什么演进到 HTTP/2 的二进制帧

---

## 📚 目录

1. [HTTP 的三个本质特征](#1-http-的三个本质特征)
2. [请求报文结构](#2-请求报文结构)
3. [响应报文结构](#3-响应报文结构)
4. [方法语义与幂等性（RFC 9110）](#4-方法语义与幂等性rfc-9110)
5. [头部字段体系](#5-头部字段体系)
6. [URI / URL / URN](#6-uri--url--urn)
7. [媒体类型与内容协商](#7-媒体类型与内容协商)
8. [核心要点与思考题](#8-核心要点与思考题)

---

## 1. HTTP 的三个本质特征

### 1.1 无状态（stateless）

```text
服务器不保存"客户端上下文"：
  每个请求都是独立的，服务器不知道"你是谁、你之前做了什么"

无状态的代价 → 需要状态时：
  Cookie（客户端携带标识，04 章）
  Token（无服务器存储的标识）
  Session 表（服务器侧存储 + Cookie 关联）

无状态的好处 → 水平扩展容易：
  任何服务器都能处理任何请求（无粘性会话）
  → 负载均衡天然友好（计算机网络高并发体系 08 章）
```

### 1.2 文本协议

```text
请求行/状态行与头部都是 ASCII 文本（可读、可调试）
  GET /index.html HTTP/1.1\r\n
  Host: example.com\r\n
  \r\n
  <body>

文本化的代价：冗余大（头部重复）、解析成本高
→ HTTP/2 用二进制分帧解决（06 章）；HTTP/3 彻底重做传输（网络高并发体系 07 章）
```

### 1.3 请求-响应模型

```text
客户端发起请求 → 服务器返回响应
服务器不能主动推送（除非 WebSocket/SSE 建立后）
资源的"表述"（representation）：同一资源可有多种表示（JSON/XML/HTML）
→ 内容协商（Accept 头）让服务器挑一种
```

> 🎯 **核心要点**：无状态是 HTTP 的"设计原点"——它让 Web 可水平扩展，也让"会话"成为所有 Web 框架必须自己解决的问题（Cookie/Token/Session）。

## 2. 请求报文结构

```text
GET /users/42?page=2 HTTP/1.1\r\n        ← 请求行：方法 SP URI SP 版本
Host: api.example.com\r\n                ← 头部（每个 头部名: 值）
Authorization: Bearer xxx\r\n
Accept: application/json\r\n
\r\n                                      ← 空行（分隔头部与主体）
{"query":"..."}                           ← 请求主体（GET 通常无）
```

| 组成 | 例子 | 说明 |
|------|------|------|
| 请求行 | `GET /path?query HTTP/1.1` | 方法、URI、版本 |
| 头部 | `Host:` `User-Agent:` `Accept:` | 键值对，大小写不敏感（惯例小写） |
| 空行 | `\r\n` | 标志头部结束 |
| 主体 | JSON/表单/文件 | 可选（GET/HEAD/DELETE 一般无） |

```bash
# 用 curl 观察原始报文
curl -v http://example.com
# > GET / HTTP/1.1            （发送：请求行+头部）
# > Host: example.com
# > User-Agent: curl/...
# < HTTP/1.1 200 OK           （接收：状态行+头部）
# < Content-Type: text/html
```

## 3. 响应报文结构

```text
HTTP/1.1 200 OK\r\n                       ← 状态行：版本 SP 状态码 SP 原因短语
Content-Type: application/json\r\n
Cache-Control: max-age=3600\r\n
\r\n
{"id":42,"name":"alice"}                  ← 响应主体
```

| 组成 | 说明 |
|------|------|
| 状态行 | `HTTP/1.1 200 OK`——版本、状态码、原因短语（原因短语是给人看的，程序只看码） |
| 头部 | `Content-Type` / `Content-Length` / `Cache-Control` / `Set-Cookie` 等 |
| 主体 | 表述数据；`Content-Length` 或 `Transfer-Encoding: chunked` 决定边界 |

> ⚠️ **注意**：`Content-Length` 与 `Transfer-Encoding` **互斥**——两者同时出现是走私攻击的温床（08 章）；响应主体大小由两者之一界定。

## 4. 方法语义与幂等性（RFC 9110）

### 4.1 方法全表

| 方法 | 语义 | 幂等 | 安全 | 主体 |
|------|------|:---:|:---:|------|
| GET | 获取资源表述 | ✅ | ✅ | 无（一般） |
| HEAD | 只取头部（同 GET 无主体） | ✅ | ✅ | 无 |
| POST | 提交资源（创建/动作） | ❌ | ❌ | 有 |
| PUT | 整体替换资源 | ✅ | ❌ | 有 |
| PATCH | 部分修改资源 | ❌（语义上） | ❌ | 有 |
| DELETE | 删除资源 | ✅ | ❌ | 一般无 |
| OPTIONS | 询问支持的方法/CORS 预检 | ✅ | ✅ | 无 |
| TRACE | 回显请求（调试，安全风险） | ✅ | ✅ | 无 |

### 4.2 幂等性的工程意义

```text
幂等（idempotent）：执行 1 次与执行 N 次效果相同

为什么重要：
  网络超时 → 客户端重试 → 重试会不会产生副作用？
  GET 重试安全（浏览器刷新就是重试）
  POST 重试可能重复下单/重复扣款 → 需要幂等键（Idempotency-Key）或唯一约束

生产套路：
  ① 自然幂等：PUT 全量替换（重复 PUT 结果一致）
  ② 人工幂等：POST 请求带 Idempotency-Key，服务端按 key 去重
  ③ 数据库兜底：唯一索引（订单号/流水号）
```

> 🎯 **核心要点**：面试高频——"GET 和 POST 的区别"要答出**语义层面**（幂等/安全/缓存/历史记录），而非表面（"GET 在 URL 传参 POST 在 body"）。

## 5. 头部字段体系

### 5.1 四类头部

| 类别 | 位置 | 例子 | 说明 |
|------|:---:|------|------|
| 通用头 | 双向 | `Cache-Control`、`Connection`、`Date` | 请求响应通用 |
| 请求头 | 请求 | `Host`、`User-Agent`、`Accept`、`Authorization`、`Cookie` | 客户端上下文 |
| 响应头 | 响应 | `Set-Cookie`、`Location`、`Server`、`WWW-Authenticate` | 服务器上下文 |
| 实体头 | 双向往 | `Content-Type`、`Content-Length`、`ETag`、`Last-Modified` | 描述主体 |

### 5.2 高频头部速查

| 头部 | 方向 | 作用 |
|------|:---:|------|
| `Host` | 请求 | **HTTP/1.1 必须**；虚拟主机路由依据 |
| `Accept` / `Accept-Encoding` | 请求 | 内容协商（类型/压缩算法） |
| `Authorization` | 请求 | 凭证（Bearer Token / Basic） |
| `Cookie` / `Set-Cookie` | 请求/响应 | 会话状态 |
| `Content-Type` | 双向 | 主体媒体类型 |
| `Location` | 响应 | 重定向目标（配合 3xx） |
| `ETag` / `Last-Modified` | 响应 | 缓存校验（03 章） |
| `Cache-Control` | 双向 | 缓存策略（03 章） |

## 6. URI / URL / URN

```text
URI（统一资源标识符）：标识资源的总称
├── URL（定位符）：说明"在哪找"——http://example.com/path?q=1
└── URN（名称符）：说明"叫什么"——urn:isbn:9780141036144

URL 结构分解：
  http://user:pass@example.com:8080/path/to/page?name=alice&age=30#section
  └scheme┘ └──userinfo──┘ └─host──┘ └port┘ └───path───┘ └──query───┘ └fragment┘
```

| 部分 | 编码注意 | 说明 |
|------|---------|------|
| path | 路径段需 URL 编码（空格→%20） | 语义路径 |
| query | 键值对 `&` 分隔，需编码 | 查询参数 |
| fragment | **不发给服务器**（`#` 后浏览器本地处理） | 锚点 |

> ⚠️ **易错点**：`#fragment` 永不发送到服务器——用它做前端路由（SPA）因此不会产生请求；后端拿不到 fragment 参数。

## 7. 媒体类型与内容协商

### 7.1 Content-Type 体系

```text
格式：type/subtype
  application/json         JSON
  application/x-www-form-urlencoded   表单（a=1&b=2）
  multipart/form-data     文件上传（边界分隔）
  text/html                HTML
  text/plain               纯文本
  application/octet-stream 二进制流
  image/png / video/mp4    媒体
```

### 7.2 内容协商流程

```text
客户端声明偏好 → 服务器选择 → 响应中标注实际类型
  Accept: text/html,application/json;q=0.9     （q=质量因子，0-1 偏好度）
  服务器挑 application/json
  Content-Type: application/json               （实际返回的）
```

> 💡 **工程意义**：同一 URL 返回 HTML（浏览器）或 JSON（API 客户端）的"双通道"实现，靠的就是 Accept 协商——但**优先用路径/子域区分**（如 `/api/`），协商只做兜底（缓存键会因此分裂，见 03 章 Vary）。

## 8. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. HTTP 的本质：无状态（水平扩展友好，会话靠 Cookie/Token 补充）+ 文本（可读但冗余，HTTP/2 改二进制）+ 请求-响应（推送靠 WebSocket/SSE）；
> 2. 方法三原则：GET 安全幂等、PUT/DELETE 幂等、POST 都不保证——重试安全由此而来；
> 3. 报文结构：请求行/状态行 + 头部（键值对）+ 空行 + 主体，`Content-Length` 与 `Transfer-Encoding` 互斥。

**思考题**：

1. 无状态为什么对水平扩展有利？（→ 1.1）
2. POST 重试怎么防重复？（→ 4.2 三件套）
3. `#fragment` 会发给服务器吗？（→ 6）
4. 内容协商会带来什么缓存问题？（→ 7.2 + 03 章 Vary）

---

**下一模块**：[02-HTTP 状态码与错误处理](02-HTTP 状态码与错误处理.md)｜**返回总览**：[00-HTTP与HTTPS知识体系总览](00-HTTP与HTTPS知识体系总览.md)
