# 00 - Node.js 知识体系总览（Java 开发者视角）

> 🎯 Node.js 是 AI 工具链的核心语言——MCP Server、LangChain.js、Vite/Next.js 前端、VS Code 插件，全都跑在 Node.js 上。Java 开发者不需要成为 Node.js 专家，但必须能"读懂、会改、能写"

> 🎯 共 **12 篇**，从 Java 映射到 Node.js、从 npm 到 Express、从 TypeScript 到 MCP

---

## 1. 知识全景

```
Node.js 体系（12个文件）
│
├── 🏗️ 基础篇（01-03）
│   ├── 01-Java开发者视角的Node.js入门.md   # 语法映射/运行时/生态对比
│   ├── 02-npm包管理与模块系统.md          # npm/package.json/CommonJS/ESM
│   └── 03-异步编程Promise与async-await.md  # 事件循环/回调/Promise
│
├── 🔧 核心篇（04-06）
│   ├── 04-文件系统与Stream操作.md         # fs/readFile/writeFile/Stream
│   ├── 05-HTTP服务与Express框架.md        # http模块/Express/REST API
│   └── 06-子进程与系统交互.md             # child_process/exec/spawn
│
├── 🚀 进阶篇（07-09）
│   ├── 07-TypeScript基础.md              # 类型/接口/泛型/tsconfig
│   ├── 08-Node.js测试与调试.md            # Jest/Supertest/debugger
│   └── 09-MCP Server开发实战.md           # Model Context Protocol Node.js实现
│
├── 📋 AI工具链篇（10）
│   └── 10-AI前端工具链Vite与Next.js.md    # 现代前端/SSR/API Routes
│
└── 📌 冲刺篇（11）
    └── 11-面试高频考点与总结.md            # Java-Node.js对比/事件循环
```

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景+路线 | — |
| 01 | Java视角入门 | 语法映射/运行时对比 | ⭐⭐⭐⭐⭐ |
| 02 | npm与模块 | package.json/CommonJS/ESM | ⭐⭐⭐⭐⭐ |
| 03 | 异步编程 | Promise/async-await/事件循环 | ⭐⭐⭐⭐⭐ |
| 04 | 文件系统 | fs/Stream/路径处理 | ⭐⭐⭐⭐ |
| 05 | HTTP与Express | REST API/中间件/路由 | ⭐⭐⭐⭐⭐ |
| 06 | 子进程 | exec/spawn/与Java交互 | ⭐⭐⭐⭐ |
| 07 | TypeScript | 类型系统/接口/泛型 | ⭐⭐⭐⭐ |
| 08 | 测试与调试 | Jest/debugger/inspect | ⭐⭐⭐ |
| 09 | MCP Server | Model Context Protocol实战 | ⭐⭐⭐⭐⭐ |
| 10 | AI前端工具链 | Vite/Next.js/SSR | ⭐⭐⭐⭐ |
| 11 | 面试考点 | Java对比/事件循环/模块 | ⭐⭐⭐⭐⭐ |

## 3. 学习路线

```text
🟢 上手（45min）：01-Java视角 → 02-npm → 03-异步
🔵 核心（1h）：04-文件 → 05-Express → 06-子进程
🟣 进阶（1h）：07-TypeScript → 08-测试 → 09-MCP Server
🟡 AI链（30min）：10-前端工具链
🔴 冲刺（20min）：11-面试
```

## 4. Java ↔ Node.js 速查

| Java | Node.js |
|------|---------|
| `javac` + `java` | `node script.js` |
| Maven/Gradle | npm / yarn / pnpm |
| `pom.xml` | `package.json` |
| Spring Boot | Express / Fastify |
| `Thread` / `ExecutorService` | `Promise` / `async-await` |
| `InputStream` / `OutputStream` | `Stream` |
| `Runtime.exec()` | `child_process.exec()` |
| JUnit | Jest |
| Type annotation in code | TypeScript |
| JAR | npm package |
