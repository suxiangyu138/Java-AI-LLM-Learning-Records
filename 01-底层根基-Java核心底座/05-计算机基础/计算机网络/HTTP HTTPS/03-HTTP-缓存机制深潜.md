# HTTP 缓存机制深潜（RFC 9111）

> 缓存的本质是"用空间换时间"：强缓存让浏览器不发出请求直接用本地副本，协商缓存让服务器用 304 省掉主体传输。`Cache-Control` 的每个参数、`ETag` 的校验强度、`Vary` 的缓存键分裂——是 Web 性能工程师的基本功

---

## 📚 目录

1. [缓存体系全景：浏览器、代理、CDN](#1-缓存体系全景浏览器代理cdn)
2. [强缓存：不走网络的缓存](#2-强缓存不走网络的缓存)
3. [协商缓存：带条件的验证](#3-协商缓存带条件的验证)
4. [Cache-Control 全参数解析](#4-cache-control-全参数解析)
5. [ETag vs Last-Modified](#5-etag-vs-last-modified)
6. [Vary 与缓存键](#6-vary-与缓存键)
7. [缓存更新策略实战](#7-缓存更新策略实战)
8. [核心要点与思考题](#8-核心要点与思考题)

---

## 1. 缓存体系全景：浏览器、代理、CDN

```text
一条完整链路上的缓存层级：
  客户端浏览器缓存（本地）
  中间代理缓存（企业代理/运营商）
  CDN 边缘缓存（Cloudflare/Akamai/阿里云）
  源站（最后兜底）

每层缓存独立判断，响应头对每一层都生效：
  Cache-Control: public → 所有层可缓存
  Cache-Control: private → 仅终端浏览器可缓存（中间层禁存）
```

| 层 | 缓存什么 | 缓存键 |
|----|---------|--------|
| 浏览器 | HTML/JS/CSS/图片 | URL（+ Vary） |
| CDN | 静态资源/页面 | URL（+ Vary + 查询参数策略） |
| 应用层 | 数据（Redis/本地） | 业务键 |

> 🎯 **核心要点**：**缓存策略是头字段驱动的**——同一份资源，源站通过响应头决定"谁能缓存、缓存多久、怎么验证"。改缓存策略 = 改头，不改代码。

## 2. 强缓存：不走网络的缓存

### 2.1 流程

```text
第一次请求：浏览器无缓存 → 发请求 → 200 + Cache-Control: max-age=3600
                        → 浏览器存储资源与过期时间
第二次请求（1 小时内）：本地副本未过期 → 【不发请求】直接用本地
第三次请求（1 小时后）：过期 → 进入协商缓存（如果带校验器）或重新请求
```

### 2.2 两个实现字段

| 字段 | 说明 | 现状 |
|------|------|------|
| `Cache-Control: max-age=3600` | 相对时间（秒） | **现行标准**（HTTP/1.1+） |
| `Expires: Wed, 05 Aug 2026 ...` | 绝对时间 | HTTP/1.0 遗留；时钟偏差问题 |

```text
优先级：Cache-Control 优先于 Expires
⚠️ max-age=0 与 no-cache 语义：强制每次"验证"而非直接使用
```

## 3. 协商缓存：带条件的验证

### 3.1 流程

```text
缓存过期 → 带条件头发请求 → 服务器判断：
  未变化 → 304 Not Modified（无主体）→ 浏览器继续用缓存
  已变化 → 200 + 新资源

条件头（请求侧）：
  If-None-Match: "<etag>"       ← 与响应头 ETag 配对（首选）
  If-Modified-Since: <date>     ← 与响应头 Last-Modified 配对
```

```java
// Spring 中控制缓存（示例）
@GetMapping("/resource")
ResponseEntity<byte[]> get() {
    String etag = md5(content);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofHours(1))
            .cachePrivate())
        .eTag(etag)                    // 生成 ETag
        .body(content);
    // 客户端带 If-None-Match 时 Spring 自动回 304
}
```

### 3.2 完整时序

```text
① 首次：浏览器 → 200（ETag: "abc", Cache-Control: max-age=3600）
② 过期后：浏览器 → If-None-Match: "abc" → 服务器比对
      → 未变：304（无主体）→ 浏览器用缓存，重新计时
      → 已变：200 + 新 ETag
```

> 💡 **理解 304 的价值**：省的不只是响应体传输——304 响应很小（几十字节），而完整资源可能几百 KB。**强缓存省请求，协商缓存省流量**。

## 4. Cache-Control 全参数解析

| 参数 | 语义 | 生产场景 |
|------|------|---------|
| `max-age=<s>` | 缓存有效秒数 | 静态资源 1 年/动态资源 0 |
| `s-maxage=<s>` | **代理/CDN 层**有效秒数（覆盖 max-age） | 源站 CDN 专用策略 |
| `no-cache` | **不直接使用**：每次必须验证（可存但要用前问服务器） | 动态内容 |
| `no-store` | **不存储**（含验证器都不存） | 敏感数据/支付响应 |
| `public` | 任何层可缓存 | CDN 内容 |
| `private` | 仅终端浏览器可缓存（中间层不可） | 用户个性化响应 |
| `must-revalidate` | 过期后必须重新验证（不能 stale） | 强一致内容 |
| `stale-while-revalidate` | 过期后先返回旧内容、后台异步更新 | 体验优先场景 |
| `immutable` | 内容永不变（不用再验证） | 带哈希文件名资源 |

```text
常见组合（生产模板）：
  静态资源（带内容哈希文件名）：
    Cache-Control: public, max-age=31536000, immutable
  动态 API：
    Cache-Control: no-store（或 no-cache 需要校验时）
  页面 HTML（SEO 场景）：
    Cache-Control: public, max-age=60, must-revalidate
  个性化页面：
    Cache-Control: private, no-cache
```

> ⚠️ **最常被误解的参数**：`no-cache` **不是**不缓存——是"可用但必须验证"；`no-store` 才是不存。面试爱考这个。

## 5. ETag vs Last-Modified

| 维度 | ETag（强校验器） | Last-Modified（弱校验器） |
|------|:---:|:---:|
| 精度 | 内容哈希/版本号，**精确** | 秒级时间戳，**不精确** |
| 秒内修改 | ✅ 能识别 | ❌ 识别不了 |
| 内容重组 | ✅ 内容变则变 | ⚠️ 时间可能不变 |
| 代价 | 需要计算哈希 | 零成本（stat） |
| 语义 | 强校验（字节级一致） | 弱校验（表示一致即可） |

```text
两者可共存：服务器先比 ETag，再比 Last-Modified
生产：动态内容用 ETag（精确）；静态文件代理（Nginx）默认
      Last-Modified + ETag（文件哈希）
W/ 前缀表示弱校验：ETag: W/"abc"（允许表示级等价）
```

## 6. Vary 与缓存键

### 6.1 问题：同一 URL 多个表述

```text
同一 URL 按请求头返回不同内容时（内容协商、Cookie、语言）：
  Accept-Language: zh → 中文版
  Accept-Language: en → 英文版
若缓存键只有 URL → 先来者覆盖后者 → 语言错乱
```

### 6.2 解法：Vary

```http
Vary: Accept-Language, Accept-Encoding
# 缓存键 = URL + Accept-Language + Accept-Encoding
```

| Vary 值 | 场景 |
|---------|------|
| `Accept-Encoding` | gzip/br 不同压缩版本（CDN 常见） |
| `Accept-Language` | 多语言 |
| `Cookie` | 登录态差异化内容（**慎用**：缓存碎片化） |
| `User-Agent` | 移动/桌面差异化（慎用） |

> ⚠️ **工程权衡**：Vary 越多缓存命中率越低（键分裂）。**更优解是"URL 即身份"**——语言放路径（`/zh/`、`/en/`）、压缩靠 CDN 自动处理、登录内容用 private/no-cache——用 URL 设计避免 Vary 碎片化。

## 7. 缓存更新策略实战

### 7.1 版本化文件名（缓存更新的金标准）

```text
问题：文件名不变 + max-age 长 → 更新后浏览器仍用旧缓存
解法：文件名带内容哈希（webpack/fingerprint）
  app.a3f2b1.js ← 内容变了哈希就变 → 新 URL = 新缓存
  配合：Cache-Control: public, max-age=31536000, immutable
  HTML 不缓存或短缓存（引用新哈希的文件名）

流程：发版 → HTML 更新引用（引用新哈希文件）→ 旧文件随缓存自然淘汰
```

### 7.2 动态内容的缓存模式

| 模式 | 做法 | 适用 |
|------|------|------|
| no-cache + ETag | 每次验证，变化才传 | 数据频繁变化的 API |
| stale-while-revalidate | 旧内容立即返回 + 后台更新 | 新闻/排行榜等弱一致 |
| CDN 边缘缓存 | s-maxage + 主动 purge | 静态化页面 |
| 后端缓存 + 主动失效 | Redis 缓存 + 写时删除 | 数据层 |

### 7.3 缓存问题排查工具

```bash
# 看缓存决策
curl -sI https://example.com/static/app.js
# 关注：Cache-Control / ETag / Age（CDN 层存活秒数）/ Via（代理链）

# Chrome DevTools → Network → Size 列：
#   "from memory cache" 强缓存命中（未发请求）
#   "304 Not Modified"  协商缓存命中
#   "from disk cache"   磁盘强缓存
```

## 8. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. 强缓存（max-age）省请求、协商缓存（ETag→304）省流量——前者不联网，后者发小请求；
> 2. `no-cache` 是"必须验证"而非"不缓存"，`no-store` 才是；`s-maxage` 单独管 CDN 层；
> 3. 生产金标准：哈希文件名 + immutable 长缓存，HTML 短缓存/不缓存；Vary 是缓存键分裂之源，优先用 URL 设计避免。

**思考题**：

1. `no-cache` 与 `no-store` 的区别？（→ 4）
2. 为什么哈希文件名能"缓存 1 年还即时更新"？（→ 7.1）
3. ETag 比 Last-Modified 强在哪？代价是什么？（→ 5）
4. 多语言站点用 Vary 还是路径拆分？（→ 6.2）

---

**下一模块**：[04-Cookie 与会话机制](04-Cookie 与会话机制.md)｜**返回总览**：[00-HTTP与HTTPS知识体系总览](00-HTTP与HTTPS知识体系总览.md)
