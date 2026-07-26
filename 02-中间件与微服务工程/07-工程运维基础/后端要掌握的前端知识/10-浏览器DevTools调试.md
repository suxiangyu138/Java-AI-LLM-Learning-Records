# 10-浏览器DevTools调试
> 🎯 后端开发者的前端调试利器 — 掌握Network面板读懂请求全过程、Console查看前端错误、Application检查存储状态

---

## 目录
1. [F12开发者工具总览](#1-f12开发者工具总览)
2. [Network面板 — 后端最常用](#2-network面板--后端最常用)
3. [Console面板 — 看前端错误](#3-console面板--看前端错误)
4. [Application面板 — 检查存储](#4-application面板--检查存储)
5. [常用调试场景](#5-常用调试场景)

---

## 1. F12开发者工具总览

```
按 F12 打开 DevTools，核心面板：

┌─────────────────────────────────────────┐
│ Elements │ Console │ Sources │ Network │ Application │ ...
├─────────────────────────────────────────┤
│                                         │
│  Elements → 看DOM结构、CSS样式          │
│  Console  → 看JS日志和错误              │
│  Network  → 看所有HTTP请求 ⭐最关键      │
│  Sources  → 看JS源码、打断点            │
│  Application → 看Cookie/localStorage    │
│                                         │
└─────────────────────────────────────────┘
```

| 面板 | 后端使用频率 | 用途 |
|------|:---:|------|
| **Network** | ⭐⭐⭐⭐⭐ | 排查接口问题 |
| **Console** | ⭐⭐⭐⭐ | 看前端报错 |
| **Application** | ⭐⭐⭐ | 检查Cookie/Token存储 |
| **Elements** | ⭐⭐ | 看页面结构 |
| **Sources** | ⭐ | 断点调试JS（调前端才用） |

---

## 2. Network面板 — 后端最常用

### 2.1 请求列表解读

```text
Name          Status  Type    Size    Time
users         200     xhr     2.3KB   45ms
users/1       404     xhr     0.2KB   12ms
styles.css    200     css     15KB    8ms
app.js        200     script  120KB   35ms

关键列：
- Status: 200=成功, 4xx=客户端错, 5xx=服务端错
- Type: xhr/fetch=AJAX请求（你最关心的）
- Size: 响应大小（大响应可能影响性能）
- Time: 请求耗时（排队+发送+等待TTFB+下载）
```

### 2.2 点击请求查看详情

```text
点击某个请求 → 查看子面板：

Headers（请求/响应头）：
  General: Request URL, Request Method, Status Code
  Response Headers: Content-Type, Set-Cookie, ...
  Request Headers: Authorization, Content-Type, ...

Payload（请求体）：
  查看前端发送的JSON参数 ← "参数传对了没？"

Preview / Response（响应体）：
  查看后端返回的JSON数据 ← "返回的数据对不对？"

Timing（时序）：
  Queuing → Stalled → DNS Lookup → Initial connection
  → SSL → Request sent → Waiting(TTFB) → Content Download
  瓶颈通常在 Waiting(TTFB)：服务器处理时间
```

### 2.3 TTFB含义

```text
TTFB (Time To First Byte) = 从发送请求到收到第一个字节的时间

TTFB 长 → 后端处理慢（数据库查询、业务逻辑）
Content Download 长 → 响应体太大（返回数据太多，需要分页）
```

---

## 3. Console面板 — 看前端错误

### 3.1 常见前端错误

```javascript
// ❌ 前端常见的报错（在Console中看到的）

// 1. 接口404
GET http://localhost:8080/api/user 404 (Not Found)

// 2. CORS跨域
Access to fetch at 'http://localhost:8080/api' from origin 
'http://localhost:3000' has been blocked by CORS policy

// 3. JS运行时错误
Uncaught TypeError: Cannot read property 'name' of undefined
// → 前端拿到null/undefined后访问了属性

// 4. JSON解析失败
Uncaught SyntaxError: Unexpected token '<', "<!DOCTYPE "... is not valid JSON
// → 接口返回了HTML（通常是404页面）而不是JSON

// 5. 网络错误
TypeError: Failed to fetch
// → 后端服务未启动或防火墙阻止
```

### 3.2 Console命令

```javascript
// 在Console中直接执行（验证前端数据状态）
console.log(userData)        // 打印变量
console.table(users)         // 表格形式打印数组
console.error("error msg")   // 红色错误
console.warn("warn msg")     // 黄色警告

// 后端可用：在前端Console直接发请求验证接口
fetch('/api/users/1')
  .then(r => r.json())
  .then(console.log)
  .catch(console.error)
```

---

## 4. Application面板 — 检查存储

### 4.1 存储位置速查

| 存储 | 位置 | 后端关注 |
|------|------|----------|
| **Cookies** | Application → Cookies | JWT Token是否存为Cookie、HttpOnly/Secure |
| **Local Storage** | Application → Local Storage | JWT Token是否存localStorage（可被XSS读取） |
| **Session Storage** | Application → Session Storage | 临时数据 |

### 4.2 如何在Application面板检查Token

```text
1. 打开 Application → Local Storage → http://localhost:3000
2. 查找 Key: token / access_token / auth
3. 如果找到了 → 前端把Token存在localStorage（⚠️ 有XSS风险）
4. 如果没找到 → 看Cookies → 看Token是否在HttpOnly Cookie中（✅ 更安全）
```

---

## 5. 常用调试场景

### 5.1 "前端说接口调不通"

```text
后端排查步骤：
1. Network → 确认请求是否发出？Status是什么？
2. Payload → 参数字段名对吗？（前后端字段名不一致是最常见问题）
3. Response → 后端返回了什么？
4. 如果Network列表根本没这个请求 → 前端代码没调用
5. 如果有请求但Status=0或CORS error → 跨域问题
6. 如果Status=500 → 打开后端日志看异常
```

### 5.2 "登录后马上被踢"

```text
1. Application → Cookies → 看Token是否存在
2. Network → 找一个API请求 → Request Headers
   → Authorization 头是否携带了Token？
   → 如果缺失 → 前端拦截器没加Token
3. 如果携带了但返回401 → Token过期或格式不对
```

### 5.3 禁用缓存（联调必备）

```text
Network面板 → 勾选 Disable cache
或 F12 → Network → 右键刷新按钮 → "清空缓存并硬性重新加载"

作用：确保每次请求都是真实请求，不走浏览器缓存
（否则你改了后端，前端看到的还是旧数据！）
```

---

> 🎯 **后端学DevTools的最小集合**：Network面板能看懂请求的完整生命周期 + 能从Console错误信息判断是前端问题还是后端问题。
