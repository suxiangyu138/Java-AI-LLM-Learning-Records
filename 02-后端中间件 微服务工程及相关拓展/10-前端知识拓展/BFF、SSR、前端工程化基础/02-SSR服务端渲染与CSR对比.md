# 02 - SSR 服务端渲染与 CSR 对比

> 🎯 SSR vs CSR 不是信念之争而是场景之选 — 理解两者的渲染流程和 SEO/TTFB 权衡，后端才能理解为什么有时候"页面是服务端拼好的"

---

## 目录

1. [CSR vs SSR 渲染流程](#1-csr-vs-ssr-渲染流程)
2. [SSR 框架概览](#2-ssr-框架概览)
3. [对 Java 后端的影响](#3-对-java-后端的影响)

---

## 1. CSR vs SSR 渲染流程

```text
CSR（Client-Side Rendering — Vue/React 默认）：
  1. 浏览器请求 → 返回空壳 HTML（<div id="app"></div>）
  2. 浏览器下载 JS Bundle（app.js）
  3. JS 执行 → 渲染页面 → 调 API 获取数据
  4. 数据回来 → 更新 DOM

  问题：白屏时间长、SEO 不友好（爬虫看到空壳）

SSR（Server-Side Rendering — Next.js/Nuxt）：
  1. 浏览器请求 → Node.js 服务端执行 Vue/React → 返回完整 HTML
  2. 浏览器直接渲染（已有内容）
  3. JS 加载后 Hydration（接管交互）

  优势：首屏快、SEO 友好
  代价：需要 Node.js 服务端、服务端资源消耗
```

| 维度 | CSR | SSR |
|------|:---:|:---:|
| 首屏速度 | 慢（等 JS 加载+执行） | ⭐ 快（直出 HTML） |
| SEO | ❌ 差（爬虫看空壳） | ✅ 好（HTML 有内容） |
| 服务端压力 | 低（静态文件） | 高（渲染吃 CPU） |
| 开发体验 | 简单 | 复杂（区分服务端/客户端代码） |
| **适用** | 后台管理系统、工具 | 官网、电商、内容站 |

### 混合方案：SSG + CSR

```text
SSG（Static Site Generation）：
  构建时生成静态 HTML → 直接返回 HTML
  适合：博客、文档站、不频繁更新的页面

  Next.js: getStaticProps() — 构建时获取数据
  Nuxt:    nuxt generate

ISR（Incremental Static Regeneration）：
  SSG + 定时重新生成 → 兼具静态的速度 + 动态的时效
```

---

## 2. SSR 框架概览

| 框架 | 定位 | 渲染方式 | API 层 |
|------|------|:---:|------|
| **Next.js** | React 全栈框架 | SSR/SSG/ISR | API Routes / Server Actions |
| **Nuxt** | Vue 全栈框架 | SSR/SSG | Server Routes |
| **Remix** | React 全栈框架 | SSR | Loader + Action |
| **Astro** | 内容站框架 | SSG/SSR | — |

```javascript
// Next.js SSR 示例 — 服务端获取数据
export async function getServerSideProps(context) {
  // ⭐ 这段代码在服务端执行！
  const res = await fetch(`https://api.example.com/users/${context.params.id}`);
  const user = await res.json();
  return { props: { user } };    // 数据注入组件
}

export default function UserPage({ user }) {
  return <div>{user.name}</div>;   // HTML 直接包含内容
}
```

---

## 3. 对 Java 后端的影响

```text
CSR 场景（SPA — 后台管理系统）：
  Nginx → 静态文件（index.html + app.js）
  Java → 纯 API（/api/*）
  → 前后端完全分离，后端只需提供 REST API

SSR 场景（Next.js/Nuxt — 官网/电商）：
  Node.js Server → 服务端渲染 → 调 Java API 获取数据
  → 后端不变！依然是纯 API + 可能需更高性能（SSR 放大 QPS）
```

```text
SSR 场景下后端的注意事项：
  1. API 性能要更好（每次页面请求都调 API）
  2. 接口缓存很重要（SSR 会放大请求量）
  3. Cookie/Token 需要从 SSR 服务端透传到 Java API
  4. 健康检查要完备（SSR 依赖 API 可用性）
```

### SSR 中的 API 调用链

```text
浏览器 → Next.js Server → Java API Server → DB
         ↑ UUID 是重点！
         在这里调用 Java API，返回完整 HTML

优点：后端不需要变，继续提供纯 JSON REST API
缺点：每次渲染都调 API → 接口 QPS 放大
```

> 🎯 **后端记住**：CSR = 前端静态文件 + 后端纯 API（默认模式）；SSR = Node.js 服务端渲染 + 后端纯 API（后端不变！）。当看到前端需求文档上出现 Next.js/Nuxt 时，意味着接口性能要更好、缓存更重要。
