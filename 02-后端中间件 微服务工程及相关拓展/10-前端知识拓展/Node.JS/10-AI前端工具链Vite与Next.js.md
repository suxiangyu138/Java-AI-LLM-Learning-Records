# 10 - AI 前端工具链：Vite 与 Next.js

> 🎯 AI 应用的前端离不开现代构建工具。Vite 是新一代构建工具（替代 Webpack），Next.js 是全栈 React 框架。Java 后端开发者不需要成为前端专家，但要能"跑起来、改得动"

---

## 目录

1. [Vite：现代构建工具](#1-vite现代构建工具)
2. [Next.js：全栈 React 框架](#2-nextjs全栈-react-框架)
3. [AI 前端项目模板](#3-ai-前端项目模板)

---

## 1. Vite：现代构建工具

### 1.1 为什么 Vite 替代 Webpack

| 维度 | Webpack | Vite |
|------|:---:|:---:|
| 冷启动 | 30s-2min | **<1s** |
| HMR 热更新 | 1-5s | **<100ms** |
| 原理 | Bundle 全部再服务 | 原生 ESM，按需编译 |
| 配置复杂度 | 高 | 低 |

```bash
# 创建 Vite 项目
npm create vite@latest my-ai-chat -- --template react-ts
cd my-ai-chat
npm install
npm run dev  # → http://localhost:5173

# 构建生产版本
npm run build  # → dist/ 目录
```

### 1.2 关键文件

```typescript
// vite.config.ts — 最简配置
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    server: {
        port: 3000,
        proxy: {
            '/api': 'http://localhost:8080'  // 代理到 Java 后端！
        }
    }
});
```

## 2. Next.js：全栈 React 框架

### 2.1 与 Spring Boot 的对应

| Next.js | Spring Boot |
|---------|------------|
| `app/page.tsx` | `@Controller` + 模板 |
| `app/api/route.ts` | `@RestController` |
| `app/layout.tsx` | 模板布局 |
| `middleware.ts` | `Filter` / `Interceptor` |
| Server Components | 服务端渲染 |
| `next.config.js` | `application.yml` |

### 2.2 API Routes（替代 Express！）

```typescript
// app/api/chat/route.ts — Next.js 的 API 路由（= @RestController）
import { NextRequest, NextResponse } from 'next/server';

export async function POST(req: NextRequest) {
    const { message } = await req.json();

    // 调用 Java 后端
    const response = await fetch('http://java-backend:8080/ai/chat', {
        method: 'POST',
        body: JSON.stringify({ message }),
        headers: { 'Content-Type': 'application/json' }
    });
    const data = await response.json();

    return NextResponse.json(data);
}
```

### 2.3 流式 AI 响应

```typescript
// app/api/chat-stream/route.ts — SSE 流式 AI 输出
export async function POST(req: NextRequest) {
    const { message } = await req.json();

    // 调用 Java 后端（流式）
    const response = await fetch('http://java-backend:8080/ai/chat/stream', {
        method: 'POST',
        body: JSON.stringify({ message }),
        headers: { 'Content-Type': 'application/json' }
    });

    // 直接转发 SSE 流
    return new Response(response.body, {
        headers: {
            'Content-Type': 'text/event-stream',
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive',
        }
    });
}
```

## 3. AI 前端项目模板

```text
ai-chat-app/
├── app/
│   ├── api/
│   │   └── chat/route.ts        ← Next.js API Route
│   ├── layout.tsx               ← 根布局
│   └── page.tsx                 ← 聊天页面
├── components/
│   ├── ChatInput.tsx            ← 输入框组件
│   ├── MessageList.tsx          ← 消息列表
│   └── StreamingText.tsx        ← 流式文本渲染
├── lib/
│   └── api.ts                   ← API 调用工具函数
├── package.json
├── next.config.js
└── tailwind.config.js           ← CSS 框架
```

```bash
# 一键创建 AI 聊天前端
npx create-next-app@latest ai-chat --typescript --tailwind --app
cd ai-chat
npm run dev
```

## 核心要点回顾

- Vite = 现代构建工具（替代 Webpack），秒级启动
- Next.js = 全栈 React（替代 Express + 前端框架）
- Next.js API Routes = `@RestController`（可以直接写后端逻辑）
- Server Components = 服务端渲染（SEO 友好 + 性能好）
- Java 后端 + Next.js 前端 = AI 应用最通用架构

## 参考资料

1. Vite 官方文档 — vitejs.dev
2. Next.js 官方文档 — nextjs.org
