# 00 - URL 知识体系总览

> 🎯 URL 是互联网的"门牌号"——从 RFC 3986 语法规范到 Spring 路由匹配、从百分号编码到 SSRF 防护、从短链系统设计到 LLM API 的 `base_url`，URL 是后端与 AI 工程师每天都在用却最容易踩坑的基础设施

> 🎯 本系列共 **10 篇**，从语法基础到协议族、从后端路由到编程 API、从安全漏洞到系统设计、从 AI 大模型应用到面试冲刺，覆盖 URL 全链路知识

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [高频踩坑速查](#5-高频踩坑速查)

---

## 1. 知识体系导图

```text
URL 知识体系（10 个文件）
│
├── 🏗️ 语法基础篇（01-03）
│   ├── 01-URL基础概念与组成结构.md      # URI/URL/URN、六大组件、相对URL、归一化
│   ├── 02-URL编码与字符集规范.md        # 百分号编码、保留字符、中文、IDN、Base64URL
│   └── 03-URL协议族Scheme全解析.md      # http/ws/file/data/blob/jdbc/自定义 Scheme
│
├── 🔧 后端工程篇（04-05）
│   ├── 04-URL路由与后端开发实战.md      # Spring 路由、RESTful、Nginx、网关、重定向
│   └── 05-URL解析编程实战-Java与JS.md   # URI/URL/HttpClient/UriComponentsBuilder/WHATWG
│
├── 🛡️ 安全与设计篇（06-07）
│   ├── 06-URL安全与常见漏洞防护.md      # SSRF、开放重定向、XSS、路径穿越、URL 签名
│   └── 07-短链系统设计与URL工程实践.md  # 发号器、Base62、缓存、301/302、SEO、埋点
│
├── 🤖 AI 应用篇（08）
│   └── 08-URL在AI大模型应用中的实战.md  # base_url、RAG 爬取、Agent 工具、MCP、镜像源
│
└── 📌 冲刺篇（09）
    └── 09-URL速查手册与面试高频考点.md  # 语法速查、端口表、编码表、40+ 面试题
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 难度 | 适合人群 |
|:---:|------|---------|:---:|---------|
| 00 | 知识体系总览 | 全景导图 + 学习路线 | — | 全部 |
| 01 | 基础概念与组成结构 | URI/URL/URN、Scheme/Authority/Path/Query/Fragment、相对路径解析、归一化 | ⭐⭐ | 入门必读 |
| 02 | 编码与字符集规范 | 保留/非保留字符、`%XX` 编码、`encodeURIComponent` vs `URLEncoder`、中文乱码、Punycode | ⭐⭐⭐⭐ | 所有踩过乱码坑的人 |
| 03 | 协议族 Scheme 全解析 | 40+ Scheme 分类、`data:`/`blob:`/`file:`、WebSocket、JDBC、移动端 DeepLink | ⭐⭐⭐ | 全栈 / 移动端 |
| 04 | 路由与后端开发实战 | `@PathVariable`、PathPattern、RESTful 设计、Nginx `location`/`proxy_pass`、301/302/307/308 | ⭐⭐⭐⭐⭐ | Java 后端核心 |
| 05 | 解析编程实战 Java 与 JS | `java.net.URI`/`URL` 陷阱、`UriComponentsBuilder`、OkHttp `HttpUrl`、`new URL()`/`URLSearchParams` | ⭐⭐⭐⭐ | 写代码必备 |
| 06 | 安全与常见漏洞防护 | SSRF/IMDS、开放重定向、`javascript:` XSS、解析器差异攻击、路径穿越、签名 URL | ⭐⭐⭐⭐⭐ | 生产环境必修 |
| 07 | 短链系统设计与工程实践 | 发号器 4 方案、Base62、布隆过滤器、301 vs 302、SEO canonical、UTM 埋点 | ⭐⭐⭐⭐⭐ | 面试系统设计高频 |
| 08 | AI 大模型应用实战 | `base_url` 全平台表、多模态 image_url、RAG 网页爬取、Agent WebFetch、MCP、HF 镜像 | ⭐⭐⭐⭐ | AI 应用开发 |
| 09 | 速查手册与面试考点 | 语法速查卡、默认端口表、编码对照表、40+ 面试题精讲 | ⭐⭐⭐ | 面试冲刺 |

---

## 3. 学习路线推荐

```text
🟢 入门路线（60 min）—— 只想把 URL 用对
   01-基础概念（全部）
   → 02-编码规范（第 1、3、5 节：保留字符 / API 选择 / 中文处理）
   → 09-速查手册（语法速查卡 + 端口表）

🔵 后端进阶路线（3 h）—— Java 后端日常开发
   01-基础概念 → 02-编码规范 → 04-路由与后端实战（重点）
   → 05-Java 编程 API（重点：URI vs URL、UriComponentsBuilder）
   → 03-协议族（JDBC / WebSocket 部分）

🟣 安全与架构路线（3 h）—— 生产环境 / 架构设计
   06-安全漏洞防护（重点：SSRF + 开放重定向）
   → 07-短链系统设计（重点：发号器 + 缓存 + 301/302）
   → 04-路由（Nginx 与网关部分）
   → 01-归一化（URL 去重与规范化）

🤖 AI 应用路线（2 h）—— 大模型应用开发
   08-AI 实战（重点：base_url 排错 + RAG URL 治理）
   → 06-安全（Agent SSRF + 间接提示注入）
   → 02-编码（多模态 data URL / Base64URL）

🔴 面试冲刺路线（90 min）
   09-面试高频考点（40+ 题）
   → 07-短链系统设计（系统设计答辩）
   → 06-安全漏洞（安全岗 / 高级岗必问）
   → 02-编码（`encodeURI` vs `encodeURIComponent` 是经典题）
```

---

## 4. 核心概念速查

### 4.1 URL 六大组件一览

```text
 https://user:pwd@api.example.com:8443/v1/users/42?page=2&size=10#profile
 └─┬─┘   └───┬──┘ └──────┬───────┘└┬─┘└─────┬─────┘└──────┬──────┘└──┬──┘
scheme   userinfo       host      port     path         query    fragment
         └────────────── authority ──────────────┘
```

| 组件 | 名称 | 是否必填 | 是否发给服务端 | 大小写敏感 |
|------|------|:---:|:---:|:---:|
| `https` | Scheme 协议 | ✅ 必填 | ✅（决定连接方式） | ❌ 不敏感 |
| `user:pwd` | UserInfo 用户信息 | ❌ | ⚠️ 已废弃，浏览器多数忽略 | ✅ 敏感 |
| `api.example.com` | Host 主机 | ✅（网络类 Scheme） | ✅（Host 头） | ❌ 不敏感 |
| `8443` | Port 端口 | ❌（有默认值） | ✅ | — |
| `/v1/users/42` | Path 路径 | ❌（可为空） | ✅ | ✅ 敏感 |
| `page=2&size=10` | Query 查询串 | ❌ | ✅ | ✅ 敏感 |
| `profile` | Fragment 片段 | ❌ | ❌ **不发送** | ✅ 敏感 |

### 4.2 三个易混术语

| 术语 | 全称 | 定位 | 示例 |
|------|------|------|------|
| **URI** | Uniform Resource **Identifier** | 统一资源标识符，**最大集合** | `urn:isbn:9787111213826`、`https://a.com` |
| **URL** | Uniform Resource **Locator** | 统一资源定位符，URI 的子集，**说明怎么拿到** | `https://a.com/x.png` |
| **URN** | Uniform Resource **Name** | 统一资源名称，URI 的子集，**只说明是什么** | `urn:uuid:6e8bc430-9c3a-...` |

> 🎯 记忆口诀：**URI 是身份证，URL 是家庭地址，URN 是姓名**。日常 99% 场景说的"URI"其实就是 URL。

### 4.3 核心规范文档

| 规范 | 内容 | 适用场景 |
|------|------|---------|
| **RFC 3986** | URI 通用语法（2005，当前主规范） | 后端 / Java `java.net.URI` |
| **RFC 3987** | IRI 国际化资源标识符（允许 Unicode） | 中文域名 / 中文路径 |
| **WHATWG URL Standard** | 浏览器实际实现的活标准（更宽容） | 前端 `new URL()` / Node.js |
| **RFC 7230** | HTTP/1.1 报文语法（request-target 形态） | Nginx / 网关 / 抓包分析 |
| **RFC 6570** | URI Template（`{id}` 占位符） | OpenAPI / Spring `@PathVariable` |
| **RFC 3492** | Punycode（IDN 编码） | `xn--` 域名 |

> ⚠️ **RFC 3986 与 WHATWG 不完全兼容**：同一个字符串，Java `URI` 可能抛异常而浏览器正常解析；这正是 URL 解析器差异攻击的根源（详见 06 篇）。

---

## 5. 高频踩坑速查

| # | 踩坑现象 | 根因 | 速查位置 |
|:---:|---------|------|---------|
| 1 | 参数里的 `+` 变成空格 | `application/x-www-form-urlencoded` 把空格编码成 `+` | 02 篇 |
| 2 | Java `URLEncoder` 编码路径导致 `%20` 变 `+` | `URLEncoder` 是表单编码器，不是 URL 编码器 | 02 / 05 篇 |
| 3 | 参数值含 `&`、`=` 导致参数被截断 | 未对**参数值单独**编码 | 02 篇 |
| 4 | 中文 URL 到后端变问号 | 编解码字符集不一致（UTF-8 vs GBK） | 02 篇 |
| 5 | Spring Boot 3 升级后 `/api/users/` 变 404 | 3.x 默认关闭尾斜杠匹配 | 04 篇 |
| 6 | Nginx `proxy_pass` 加不加 `/` 结果完全不同 | 带 `/` 会替换掉 `location` 前缀 | 04 篇 |
| 7 | `URL.equals()` 卡住 / 超时 | `java.net.URL.equals()` 会做 DNS 解析 | 05 篇 |
| 8 | Fragment 传给后端拿不到 | `#` 后内容浏览器不发送 | 01 篇 |
| 9 | 回调地址被改成钓鱼站 | 开放重定向未做白名单 | 06 篇 |
| 10 | 内网接口被外部读取到云元数据 | SSRF 未过滤内网 IP / 重定向 | 06 篇 |
| 11 | LLM 调用报 404 `/v1/v1/chat/completions` | `base_url` 重复拼了 `/v1` | 08 篇 |
| 12 | 多模态图片 URL 模型读不到 | URL 需公网可达且无鉴权 | 08 篇 |
| 13 | Token 放在 URL 上被日志/Referer 泄露 | 敏感信息不应放 Query | 06 篇 |
| 14 | RAG 库里同一篇文章重复入库多次 | 未做 URL 归一化去重 | 01 / 08 篇 |

---

**下一模块**：[01-URL基础概念与组成结构.md](01-URL基础概念与组成结构.md)
