# 05 - Chrome DevTools 网络调试实战

> 🎯 Chrome DevTools 是后端工程师的"火眼金睛" — Network 面板抓包分析、重放请求验证接口、查看请求头和响应头、性能瀑布图定位瓶颈

---

## 目录

1. [Network 面板概述](#1-network-面板概述)
2. [请求过滤与搜索](#2-请求过滤与搜索)
3. [请求详情分析](#3-请求详情分析)
4. [重放与修改请求](#4-重放与修改请求)
5. [性能分析](#5-性能分析)

---

## 1. Network 面板概述

```
打开方式：F12 → Network 标签
刷新页面 → 自动记录所有网络请求

面板布局：
┌──────────────────────────────────────────────────────┐
│ [工具栏] 过滤框 | Preserve log | Disable cache | ... │
├──────────────────────────────────────────────────────┤
│ Name          Status  Type    Size    Time  Waterfall │
│ ├─ index.html  200    doc     2.5KB   15ms  ═        │
│ ├─ app.js      200    script  120KB   45ms  ═══      │
│ ├─ style.css   304    css     0B      2ms   ═        │
│ └─ users       500    xhr     0.2KB   850ms ════════ │
├──────────────────────────────────────────────────────┤
│ [详情面板] Headers / Preview / Response / Timing ... │
└──────────────────────────────────────────────────────┘
```

| 列 | 说明 | 后端关注 |
|----|------|:---:|
| **Name** | 请求 URL | ✅ |
| **Status** | HTTP 状态码 | ⭐⭐⭐ |
| **Type** | 资源类型（xhr/doc/script） | API 关注 xhr/fetch |
| **Time** | 总耗时 | ⭐⭐ |
| **Waterfall** | 时间瀑布图 | ⭐⭐ |

---

## 2. 请求过滤与搜索

```
过滤栏（Filter）常用技巧：

  XHR           → 仅显示 AJAX 请求（⭐ 后端最常用）
  method:POST   → 仅 POST 请求
  status-code:5 → 仅 5xx 错误
  domain:api    → 仅 api.example.com 域名
  -domain:cdn   → 排除 CDN 域名
  larger-than:1k → 响应大于 1KB 的请求
```

```text
常用快捷键：
  Ctrl+E          → 开始/停止记录
  Ctrl+F          → 搜索请求（按 URL/Header/Body）
  Preserve log    → ✅ 勾选！页面跳转后保留日志
  Disable cache   → ✅ 勾选！开发时禁用缓存
```

---

## 3. 请求详情分析

### Headers 标签（⭐ 最重要）

```
  General:
    Request URL: http://localhost:8080/api/users/1
    Request Method: GET
    Status Code: 200 OK

  Response Headers:     ← ⭐ 看服务端返回的 Header
    Content-Type: application/json
    Cache-Control: max-age=3600
    Access-Control-Allow-Origin: *

  Request Headers:      ← ⭐ 看浏览器发出的 Header
    Authorization: Bearer eyJhbGci...
    Content-Type: application/json
    Cookie: SESSIONID=abc123
```

### Preview / Response 标签

```
  Preview：格式化展示 JSON（树形可折叠） ⭐ 推荐
  Response：原始响应体（可查看完整 JSON/HTML）
```

### Timing 标签（性能排查）

```text
Queueing:        0.5ms   ← 请求排队时间
Stalled:         0.8ms   ← 连接等待时间
DNS Lookup:      5.2ms   ← DNS 解析
Initial Connection: 12.3ms ← TCP 握手 + TLS 握手
SSL:             8.1ms   ← TLS 握手（HTTP/2 可合并）
Request sent:    0.1ms   ← 发送请求
Waiting (TTFB):  250ms   ← ⭐ 等待服务端处理（排查重点）
Content Download: 3.5ms  ← 下载响应

总耗时 = 各阶段之和
TTFB 高 → 服务端处理慢（查慢 SQL / 接口逻辑 / GC）
Content Download 高 → 响应体太大（分页 / 压缩）
```

---

## 4. 重放与修改请求

### Copy as cURL

```
右键请求 → Copy → Copy as cURL
→ 自动生成完整 curl 命令（含所有 Header/Cookie）

示例输出：
curl 'http://localhost:8080/api/users/1' \
  -H 'Authorization: Bearer eyJhbGci...' \
  -H 'Content-Type: application/json' \
  --compressed

→ 粘贴到终端直接重现请求！后端排查利器
```

### Replay XHR

```
右键 → Replay XHR → 直接重放请求（无需刷新页面）
→ 快速验证接口修复效果
```

### 修改请求重发（Fetch console）

```javascript
// Console 中直接修改请求参数重发
fetch('/api/users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: '李四', age: 30 })
})
.then(r => r.json())
.then(console.log);
```

---

## 5. 性能分析

### 瀑布图解读

```text
Waterfall 颜色含义：
  █ 排队/阻塞
  █ DNS 解析
  █ 连接建立（TCP + TLS）
  █ 等待响应（TTFB — ⭐ 后端关注）
  █ 下载内容

常见问题：
  大量灰色（排队）→ 并发连接数限制（HTTP/1.1 限 6 个/域名）
  大量绿色（TTFB）→ 后端处理瓶颈
  大量蓝色（下载）→ 响应过大 / Gzip 未开启
```

### 性能优化检查点

| 检查项 | 看哪里 | 正常值 |
|--------|--------|:---:|
| 请求是否压缩 | Response Headers → `Content-Encoding: gzip` | ✅ |
| 缓存是否生效 | Status: 304 / Size: (disk cache) | ✅ |
| DNS 是否预解析 | `<link rel="dns-prefetch">` | 可选 |
| 连接是否复用 | Timing → SSL 仅首次出现 | ✅ |
| 静态资源 CDN | domain → cdn.example.com | ✅ |
| 接口 TTFB | Timing → Waiting | < 200ms |

> 🎯 **后端调试三件套**：Network 面板看请求/响应 + Copy as cURL 带回终端重现 + Timing 定位 TTFB 瓶颈。`Preserve log` 和 `Disable cache` 始终开启。
