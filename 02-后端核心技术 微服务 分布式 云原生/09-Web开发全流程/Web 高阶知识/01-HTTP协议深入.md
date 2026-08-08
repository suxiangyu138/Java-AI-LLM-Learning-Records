# HTTP 协议深入
> 报文三段结构、方法语义与幂等、状态码全语义、头字段体系、连接管理——HTTP 是 Web 的"语法"，读透它才能读懂一切框架

## 📚 目录
1. [报文结构：三段式](#1-报文结构三段式)
2. [URL 与 URI 语义](#2-url-与-uri-语义)
3. [方法语义与幂等](#3-方法语义与幂等)
4. [状态码语义全表](#4-状态码语义全表)
5. [头字段体系](#5-头字段体系)
6. [连接管理](#6-连接管理)
7. [HTTP 语义如何塑造 REST](#7-http-语义如何塑造-rest)

## 1. 报文结构：三段式

```http
# 请求报文
POST /api/users HTTP/1.1          ← 请求行：方法 目标 版本
Host: api.example.com             ← 请求头（键值对）
Content-Type: application/json
Authorization: Bearer xxx

{"name": "Alice"}                ← 请求体（可空）

# 响应报文
HTTP/1.1 201 Created              ← 状态行：版本 状态码 短语
Content-Type: application/json    ← 响应头
Cache-Control: no-store
Location: /api/users/42

{"id": 42, "name": "Alice"}      ← 响应体（可空）
```

| 段 | 请求 | 响应 | 说明 |
|----|------|------|------|
| 起始行 | 方法 + 目标 + 版本 | 版本 + 状态码 + 短语 | 报文的第一行 |
| 头字段 | 请求头 | 响应头 | 键值对，`\r\n` 分隔，以空行结束 |
| body | 请求体 | 响应体 | 可空；有 body 时须有 `Content-Length` 或 `Transfer-Encoding: chunked` |

> 🎯 关键认知：**HTTP 是无状态文本协议**（现代传输层可能是二进制帧，但语义仍是这三段）——一切框架（Servlet/Spring MVC）都是在帮你解析/构造这三段。报文边界由 `Content-Length`/`chunked` 决定，这是 [网络问题排查](../../07-工程运维基础/运维/网络问题排查/00-网络问题排查总览.md) 抓包分析的基础。

## 2. URL 与 URI 语义

```text
https://user:pass@api.example.com:8443/app/users?page=1&size=20#top
│      │    │      │              │   │    │          │         │
scheme userinfo host            port path  │   query    fragment
                                          └── 仅请求行里出现，不发到服务器
```

| 组件 | 说明 | 常见坑 |
|------|------|--------|
| scheme | http/https | 反代后需 X-Forwarded-Proto 还原 |
| host:port | 目标 | 反代后取 X-Forwarded-Host |
| path | 资源路径 | 需 URL 编码（中文/特殊字符） |
| query | 查询参数 | 敏感信息别放 query（进日志！） |
| fragment | 片段（#） | **只属于浏览器**，不发送给服务器 |

> ⚠️ 编码规则：path 段用百分号编码，query 参数键值需 `encodeURIComponent`（`&`/`=`/`+` 是保留字符）。中文乱码/参数截断 90% 是编码不一致——服务端解码字符集必须与编码一致（见 [Servlet 编码体系](../Servlet/04-请求处理与路径映射.md) §7）。

## 3. 方法语义与幂等

| 方法 | 语义 | 幂等 | 安全（不修改资源） | 典型状态码 |
|------|------|:---:|:---:|------|
| GET | 获取资源 | ✅ | ✅ | 200 |
| HEAD | 只取响应头 | ✅ | ✅ | 200（无 body） |
| POST | 创建/提交 | ❌ | ❌ | 201/200 |
| PUT | 整体替换 | ✅ | ❌ | 200/204 |
| PATCH | 部分修改 | ❌（语义上可幂等） | ❌ | 200 |
| DELETE | 删除 | ✅ | ❌ | 204/404 |
| OPTIONS | 查询支持的方法 | ✅ | ✅ | 204（CORS 预检用） |
| TRACE | 回显请求 | ✅ | ✅ | 200（安全风险，禁用） |

```text
幂等定义：客户端重复发送同一请求，服务器状态与第一次执行后一致。
  · PUT /users/42 {name:B} 执行两次 = 执行一次（结果相同）
  · POST /orders（下单）执行两次 = 两笔订单 ❌ 不幂等
  · 幂等 ≠ 安全：PUT 会改数据，但重复执行不产生额外影响
```

> 🎯 面试必答：**"POST 不幂等、GET/PUT/DELETE 幂等"**——这是接口设计的第一原则。幂等性落实的三板斧：唯一键（幂等键）、数据库唯一约束、乐观锁版本号，见 [09-生产实践与面试题](09-生产实践与面试题.md) 接口设计部分。

## 4. 状态码语义全表

```text
1xx 信息（100-199）      2xx 成功（200-299）     3xx 重定向（300-399）
4xx 客户端错误（400-499） 5xx 服务器错误（500-599）
```

### 4.1 高频状态码精解

| 码 | 含义 | 语义要点 | 常见场景 |
|:---:|------|---------|---------|
| 200 | OK | 请求成功 | GET 成功 |
| 201 | Created | 资源已创建，响应含 Location | POST 创建 |
| 204 | No Content | 成功但无 body | DELETE 成功 |
| 206 | Partial Content | Range 分片返回 | 断点续传/视频流 |
| 301 | Moved Permanently | 永久重定向（SEO 会更新） | 域名迁移 |
| 302 | Found | 临时重定向 | 登录跳转（默认） |
| 303 | See Other | GET 去取结果（PRG 模式） | 表单提交后 |
| 304 | Not Modified | 协商缓存命中（无 body） | 缓存验证 |
| 307 | Temporary Redirect | 保留方法重定向 | POST 不能变 GET 的跳转 |
| 308 | Permanent Redirect | 永久 + 保留方法 | 与 301 的区别在这 |
| 400 | Bad Request | 语法/语义错误 | 参数校验失败 |
| 401 | Unauthorized | 未认证（无凭据） | 未登录 |
| 403 | Forbidden | 已认证但无权限 | 越权访问 |
| 404 | Not Found | 资源不存在 | 路由未命中 |
| 405 | Method Not Allowed | 方法不支持 | GET 打 POST 接口 |
| 406 | Not Acceptable | Accept 无法满足 | 消息转换器缺失 |
| 408 | Request Timeout | 请求超时 | 慢客户端 |
| 409 | Conflict | 资源冲突 | 唯一键冲突/版本冲突 |
| 410 | Gone | 资源已永久移除 | 下线接口 |
| 415 | Unsupported Media Type | Content-Type 不支持 | 传错格式 |
| 422 | Unprocessable Entity | 语义错误（RFC 4918） | 业务校验失败 |
| 429 | Too Many Requests | 限流触发 | 频率限制 |
| 500 | Internal Server Error | 服务器未捕获异常 | 兜底错误 |
| 502 | Bad Gateway | 上游无响应/协议错 | 反代到死掉的后端 |
| 503 | Service Unavailable | 过载/维护中 | 熔断/停机 |
| 504 | Gateway Timeout | 上游超时 | 反代超时设置 |

> ⚠️ 高频考点：**401 vs 403**（没登录 vs 没权限）、**301 vs 308**（改不改方法）、**502 vs 504**（连接失败 vs 超时）、**404 vs 410**（临时不在 vs 永久删除）——区分表达的是"语义"，面试官爱考。

### 4.2 业务状态码设计

```text
两套体系并存是常态：
  HTTP 状态码：表达传输层语义（4xx/5xx）——协议层判定
  业务码：    表达业务层语义（1001 用户不存在）——body 里的 code
  原则：HTTP 码必须正确（4xx 就 4xx），业务码负责细分
```

> 💡 常见反模式：**"所有请求都返回 200 + 业务码"**——HTTP 层全是 200 会让监控/网关/浏览器语义全失效（缓存、重试、告警判断错乱）。正确姿势见 [SpringBoot Web 错误处理](../SpringBoot%20Web/05-错误处理机制深潜.md)。

## 5. 头字段体系

### 5.1 分类与高频头

| 类别 | 特点 | 高频成员 |
|------|------|---------|
| 通用头 | 请求响应都有 | Cache-Control、Connection、Date |
| 请求头 | 请求专用 | Host、User-Agent、Accept、Cookie、Authorization、Referer、Origin |
| 响应头 | 响应专用 | Set-Cookie、Server、Location、Allow |
| 实体头 | 描述 body | Content-Type、Content-Length、Content-Encoding、ETag、Last-Modified |

```http
# 一次典型请求的完整头
GET /api/users HTTP/1.1
Host: api.example.com                  # HTTP/1.1 必填（虚拟主机判定依据）
User-Agent: Mozilla/5.0 ...            # 客户端标识（别信，可伪造）
Accept: application/json               # 期望的响应类型（406 判定依据）
Accept-Encoding: gzip, br              # 支持的压缩算法（br = Brotli）
Accept-Language: zh-CN,en;q=0.9        # 语言偏好（q 是权重）
Authorization: Bearer eyJ...           # 认证凭据
Cookie: SID=abc123                     # 会话 Cookie
Origin: https://admin.example.com      # 跨域来源（CORS 判定）
Referer: https://app.example.com/      # 来源页（隐私，可禁）
```

| 高频响应头 | 用途 |
|-----------|------|
| `Content-Type: application/json; charset=utf-8` | body 媒体类型（415 判定依据） |
| `Content-Length` | body 字节数（无则 chunked） |
| `Cache-Control` / `ETag` | 缓存控制（见 02） |
| `Set-Cookie` | 下发 Cookie（可多值） |
| `Location` | 重定向目标（3xx 必须） |
| `Allow` | 405 时返回支持的方法 |
| `Retry-After` | 429/503 时建议重试时间 |

> 🎯 理解头 = 理解框架配置：Nginx 的 proxy_set_header、Spring 的 @RequestHeader、CORS 的 Access-Control-*——全是这套头体系的应用，详见 [05-跨域与浏览器安全机制](05-跨域与浏览器安全机制.md)。

## 6. 连接管理

| 时代 | 模型 | 问题 |
|------|------|------|
| HTTP/1.0 | 每请求一连接 | 三次握手 × N 请求，慢 |
| HTTP/1.1 | **Keep-Alive 长连接** | 队头阻塞（同一连接串行） |
| HTTP/1.1 + 管线化 | 请求不等待响应 | 中间代理难实现，实际废弃 |
| HTTP/1.1 + 多连接 | 浏览器开 6 个并行连接 | 缓解队头阻塞，连接上限 |
| HTTP/2 | 单连接多路复用 | 解决应用层队头阻塞 |
| HTTP/3 | QUIC | 再解决传输层队头阻塞（见 04） |

> 🎯 **队头阻塞（Head-of-Line Blocking）是 HTTP 演进的发动机**：HTTP/1.1 的队头阻塞 → HTTP/2 多路复用；TCP 的队头阻塞 → HTTP/3 换 QUIC。理解了这一条线，HTTP 演进史全部串起来（见 [04-HTTP2与HTTP3](04-HTTP2与HTTP3.md)）。

## 7. HTTP 语义如何塑造 REST

| HTTP 语义 | REST 应用 |
|-----------|----------|
| 资源（URL） | `/users/42` 即资源标识 |
| 方法 | GET 查 / POST 建 / PUT 改 / DELETE 删 |
| 状态码 | 201 创建成功、404 不存在、409 冲突 |
| 幂等 | PUT/DELETE 可安全重试 |
| 缓存 | GET 可缓存（见 02） |
| Content-Type | 表示（JSON/XML）与资源分离 |

> 💡 关联阅读：RESTful 设计的完整实践（DTO/VO、错误码、分页规范）在 [后端名词体系](../后端%20分布式常用名词通俗解释/00-后端分布式名词知识体系总览.md) 与 [SpringBoot Web 体系](../SpringBoot%20Web/00-SpringBootWeb总览.md) 有展开；本体系专注协议语义本身。

---

**下一模块**：[02-HTTP缓存体系](02-HTTP缓存体系.md) / **返回总览**：[00-Web高阶知识总览](00-Web高阶知识总览.md)
