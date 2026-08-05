# 05 - HTTP 服务与 Express 框架

> 🎯 Express 是 Node.js 的 Spring Boot——最流行的 Web 框架，AI 工具的 API 层首选。本章覆盖路由、中间件、REST API 的完整实践

---

## 目录

1. [原生 http 模块](#1-原生-http-模块)
2. [Express 核心概念](#2-express-核心概念)
3. [REST API 完整示例](#3-rest-api-完整示例)

---

## 1. 原生 http 模块

```javascript
import http from 'http';

// 最简 HTTP 服务（Spring Boot 的 @RestController）
const server = http.createServer((req, res) => {
    // req = HttpServletRequest
    // res = HttpServletResponse
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok' }));
});

server.listen(3000, () => console.log('http://localhost:3000'));
```

## 2. Express 核心概念

### 2.1 Express vs Spring Boot

| 概念 | Spring Boot | Express |
|------|------------|---------|
| 框架 | Spring Boot Starter | Express |
| 路由 | `@GetMapping("/users")` | `app.get('/users', handler)` |
| 中间件 | `Filter` / `Interceptor` | `app.use(middleware)` |
| 请求体 | `@RequestBody` | `req.body` (需 `express.json()`) |
| 路径参数 | `@PathVariable` | `req.params.id` |
| 查询参数 | `@RequestParam` | `req.query.page` |
| 异常处理 | `@ExceptionHandler` | `app.use((err, req, res, next) => ...)` |

### 2.2 安装与启动

```bash
npm install express
npm install -D @types/express  # TypeScript 类型
```

```javascript
import express from 'express';

const app = express();
const PORT = 3000;

// 中间件：JSON 解析（类似 @RequestBody）
app.use(express.json());

// 路由
app.get('/', (req, res) => {
    res.json({ message: 'Hello from Express' });
});

app.listen(PORT, () => {
    console.log(`Server running at http://localhost:${PORT}`);
});
```

## 3. REST API 完整示例

```javascript
import express from 'express';

const app = express();
app.use(express.json());

// 内存数据库
let users = [
    { id: 1, name: 'Alice', email: 'alice@example.com' },
    { id: 2, name: 'Bob', email: 'bob@example.com' },
];

// ====== CRUD 路由 ======

// GET /users?page=1&limit=10
app.get('/users', (req, res) => {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const start = (page - 1) * limit;
    res.json({
        data: users.slice(start, start + limit),
        total: users.length,
        page,
    });
});

// GET /users/:id
app.get('/users/:id', (req, res) => {
    const user = users.find(u => u.id === parseInt(req.params.id));
    if (!user) return res.status(404).json({ error: 'User not found' });
    res.json(user);
});

// POST /users
app.post('/users', (req, res) => {
    const { name, email } = req.body;
    if (!name || !email) {
        return res.status(400).json({ error: 'name and email required' });
    }
    const user = { id: users.length + 1, name, email };
    users.push(user);
    res.status(201).json(user);
});

// PUT /users/:id
app.put('/users/:id', (req, res) => {
    const user = users.find(u => u.id === parseInt(req.params.id));
    if (!user) return res.status(404).json({ error: 'Not found' });
    Object.assign(user, req.body);
    res.json(user);
});

// DELETE /users/:id
app.delete('/users/:id', (req, res) => {
    const index = users.findIndex(u => u.id === parseInt(req.params.id));
    if (index === -1) return res.status(404).json({ error: 'Not found' });
    users.splice(index, 1);
    res.status(204).send();
});

// ====== 中间件 ======

// 日志中间件（类似 Filter）
app.use((req, res, next) => {
    console.log(`${req.method} ${req.path}`);
    next();
});

// 认证中间件
function auth(req, res, next) {
    const token = req.headers.authorization;
    if (token === 'Bearer secret-token') {
        next();
    } else {
        res.status(401).json({ error: 'Unauthorized' });
    }
}
app.use('/admin', auth);  // 只对 /admin 路径生效

// 错误处理中间件（4 个参数的中间件是错误处理器！）
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Internal Server Error' });
});

app.listen(3000);
```

### 中间件模式

```text
Express 中间件执行顺序：

Request → [JSON解析] → [日志] → [认证] → [路由处理器] → Response
              │          │        │           │
              └──────────┴────────┴───────────┘
                     next() 链式传递

每个中间件：
  → 可以修改 req/res
  → 调用 next() 传递给下一个
  → 或者直接 res.send() 终止请求
```

## 核心要点回顾

- Express = Spring Boot 的 Node.js 版
- 四种路由参数：`req.params`(路径) / `req.query`(查询) / `req.body`(请求体) / `req.headers`
- 中间件 = Java Filter/Interceptor，用 `next()` 传递
- 错误处理 = 4 参数中间件 `(err, req, res, next)`
- `express.json()` 是必加的中间件（解析 JSON body）

## 参考资料

1. Express 官方文档 — expressjs.com
2. Fastify（性能更好的替代品）— fastify.io
