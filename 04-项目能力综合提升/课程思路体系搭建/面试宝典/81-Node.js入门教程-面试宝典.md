# Node.js 81 面试宝典
> 基于《黑马程序员Node.js全套入门教程》覆盖 Event loop、CommonJS、Express、MySQL、JWT 与项目实战高频考点

## 目录
1. [基础概念速答](#一基础概念速答)
2. [深度原理剖析](#二深度原理剖析)
3. [实战场景题](#三实战场景题)
4. [手写代码题](#四手写代码题)
5. [系统设计题](#五系统设计题)
6. [常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [面试回答模板](#七面试回答模板)
8. [快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答

### 1.1 什么是 Node.js？它和浏览器 JS 有何区别？

| 维度 | Node.js | 浏览器 JS |
|------|---------|-----------|
| 运行时 | V8 + libuv | V8 + Web API |
| 全局对象 | `global` | `window` |
| 模块系统 | CommonJS / ES Module | ES Module（`<script type="module">`） |
| 文件系统 | `fs` 模块 | 不可用 |
| 网络 | `http`、`net` 模块 | `fetch`、`XMLHttpRequest` |
| 包管理 | npm / yarn / pnpm | CDN 引入 |

> 💡 Node.js 是 **服务端 JavaScript 运行时**，基于 Chrome V8 引擎和 libuv 事件循环库，提供非阻塞 I/O 能力。

### 1.2 Node.js 事件循环（Event Loop）的执行顺序

```
   ┌───────────┐
┌─>│   timers   │ (setTimeout, setInterval)
│  └───────────┘
│  ┌───────────┐
│  │   pending  │ (I/O callbacks 延迟到下一轮)
│  │ callbacks  │
│  └───────────┘
│  ┌───────────┐
│  │   idle,    │ (内部使用)
│  │  prepare   │
│  └───────────┘
│  ┌───────────┐
│  │    poll    │ (轮询 I/O 事件)
│  └───────────┘
│  ┌───────────┐
│  │   check   │ (setImmediate)
│  └───────────┘
│  ┌───────────┐
│  │  close    │ (close callbacks)
│  │ callbacks │
│  └───────────┘
└─────────────────┘
```

> ⚠️ **关键区别**：Node.js 将 `process.nextTick()` 和微任务（Promise.then）放在每个阶段之间执行，**先清空 nextTick 队列，再清空微任务队列**。

### 1.3 CommonJS 模块化核心

```javascript
// a.js
const name = 'Node.js';
const version = '20';
module.exports = { name, version };
// 或 exports.name = name;

// b.js
const mod = require('./a.js');
console.log(mod.name); // 'Node.js'
```

| 特性 | CommonJS | ES Module |
|------|----------|-----------|
| 加载方式 | 同步加载 | 异步加载 |
| 语法 | `require()` / `module.exports` | `import` / `export` |
| 运行时 | 运行时加载 | 编译时静态解析 |
| 文件后缀 | `.js` / `.cjs` | `.mjs` / `.js`（`"type": "module"`） |
| 循环依赖 | 返回未完成 exports | 报错或返回 undefined |

### 1.4 npm 包管理器

```bash
npm init -y            # 快速初始化 package.json
npm install express    # 安装并写入 dependencies
npm install -D nodemon # 安装开发依赖
npm ci                 # 基于 lockfile 精确安装（CI 环境）
npx create-react-app   # 无需全局安装直接运行
```

> 💡 `package-lock.json` 锁定依赖树版本，保证生产环境一致性。

### 1.5 Express 路由与中间件

```javascript
const express = require('express');
const app = express();

// 路由级中间件
app.get('/users/:id', (req, res, next) => {
  const { id } = req.params;
  res.json({ id, name: 'Alice' });
});

// 应用级中间件
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 错误处理中间件（4 个参数）
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: err.message });
});

app.listen(3000);
```

### 1.6 身份认证：Session vs JWT

| 对比维度 | Session | JWT |
|---------|---------|-----|
| 存储位置 | 服务端内存/Redis | 客户端（token 字符串） |
| 扩展性 | 需要共享 session 存储 | 天然无状态，水平扩展友好 |
| 安全性 | CSRF 风险 | XSS 风险（需存 httpOnly Cookie） |
| 性能 | 每次查询存储 | 只需验证签名 |

### 1.7 JWT 实现示例

```javascript
const jwt = require('jsonwebtoken');
const SECRET = 'my_secret_key';

// 生成 Token
const token = jwt.sign(
  { id: 1, username: 'admin' },
  SECRET,
  { expiresIn: '7d' }
);

// 验证 Token
try {
  const decoded = jwt.verify(token, SECRET);
  console.log(decoded); // { id: 1, username: 'admin', iat: ..., exp: ... }
} catch (err) {
  // Token 过期或无效
}
```

### 1.8 MySQL 数据库操作（mysql2 模块）

```javascript
const mysql = require('mysql2/promise');

async function query() {
  const conn = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: '123456',
    database: 'test'
  });
  const [rows] = await conn.execute(
    'SELECT * FROM users WHERE id = ?', [1]
  );
  console.log(rows);
  await conn.end();
}
```

> 💡 使用 `?` 占位符防止 SQL 注入，mysql2 默认返回 Promise 支持 async/await。

### 1.9 Buffer 与 Stream

```javascript
// Buffer：处理二进制数据
const buf = Buffer.from('Hello');
console.log(buf.toString('base64')); // SGVsbG8=

// Stream：处理大文件
const fs = require('fs');
const readStream = fs.createReadStream('large.txt');
const writeStream = fs.createWriteStream('copy.txt');
readStream.pipe(writeStream);
```

### 1.10 process.env 与环境变量

```bash
# .env 文件
DB_HOST=localhost
DB_PORT=3306
JWT_SECRET=my_jwt_secret
```

```javascript
// 推荐使用 dotenv 库
require('dotenv').config();
const dbHost = process.env.DB_HOST || 'localhost';

// 区分环境
if (process.env.NODE_ENV === 'production') {
  // 生产环境配置
}
```

### 1.11 fs 模块核心 API

```javascript
const fs = require('fs/promises'); // Promise 版本

// 读取文件
const data = await fs.readFile('./file.txt', 'utf-8');

// 写入文件
await fs.writeFile('./output.txt', 'Hello Node.js');

// 检查文件是否存在
try {
  await fs.access('./file.txt', fs.constants.F_OK);
  console.log('文件存在');
} catch {
  console.log('文件不存在');
}
```

### 1.12 http 模块创建服务器

```javascript
const http = require('http');
const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ message: 'Hello Node.js' }));
});
server.listen(3000, () => console.log('Server running on port 3000'));
```

### 1.13 RESTful API 规范

| HTTP 方法 | 操作 | 路径示例 |
|-----------|------|---------|
| GET | 查询资源 | `/api/users`、`/api/users/:id` |
| POST | 创建资源 | `/api/users` |
| PUT | 完整更新 | `/api/users/:id` |
| PATCH | 部分更新 | `/api/users/:id` |
| DELETE | 删除资源 | `/api/users/:id` |

### 1.14 跨域解决（CORS）

```javascript
// 使用 cors 中间件
const cors = require('cors');
app.use(cors({
  origin: ['http://localhost:8080'],
  credentials: true
}));

// 手动设置响应头
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  if (req.method === 'OPTIONS') return res.sendStatus(204);
  next();
});
```

### 1.15 PM2 进程管理

```bash
npm install -g pm2
pm2 start app.js -i max          # 以最大 CPU 数启动集群
pm2 list                          # 查看进程列表
pm2 logs app                      # 查看日志
pm2 restart app                   # 重启
pm2 save && pm2 startup           # 设置开机自启
```

### 1.16 child_process 模块

```javascript
const { exec, spawn } = require('child_process');

// exec：执行命令，缓冲输出
exec('ls -la', (err, stdout, stderr) => {
  console.log(stdout);
});

// spawn：流式输出，适合大量数据
const ls = spawn('ls', ['-la']);
ls.stdout.on('data', (data) => console.log(data.toString()));
```

---

## 二、深度原理剖析

### 2.1 Node.js 事件循环完整机制

```javascript
// 执行顺序演示
setTimeout(() => console.log('timeout'), 0);
setImmediate(() => console.log('immediate'));
process.nextTick(() => console.log('nextTick'));
Promise.resolve().then(() => console.log('promise'));

// 输出结果（每次可能不一致）：
// nextTick
// promise
// timeout 或 immediate（取决于系统性能）
```

> 💡 `process.nextTick()` 优先级最高，在当前操作结束后立即执行，**可能造成 I/O 饥饿**。Node.js 文档建议多数场景使用 `setImmediate()`。

### 2.2 CommonJS 模块加载机制

```javascript
// require 伪代码逻辑：
function require(filePath) {
  // 1. 检查缓存
  if (require.cache[modulePath]) return require.cache[modulePath].exports;

  // 2. 创建模块对象
  const module = { exports: {} };

  // 3. 包装函数
  (function(exports, require, module, __filename, __dirname) {
    // 模块代码被包装在此
    eval(moduleCode);
  })(module.exports, require, module, filePath, path.dirname(filePath));

  // 4. 缓存并返回
  require.cache[modulePath] = module;
  return module.exports;
}
```

> ⚠️ **循环依赖陷阱**：当 A -> B -> A 时，B 拿到的 A.exports 是**未完成的 partial 对象**，可能导致 undefined 引用。

### 2.3 Express 中间件执行机制

```javascript
// 中间件洋葱模型（Koa 风格）vs Express 线性模型
const express = require('express');
const app = express();

app.use((req, res, next) => {
  console.log('中间件 A 开始');
  next();
  console.log('中间件 A 结束');
});

app.use((req, res, next) => {
  console.log('中间件 B 开始');
  next();
  console.log('中间件 B 结束');
});

app.get('/', (req, res) => {
  res.send('Hello');
});
```

> 💡 Express 是**线性请求-响应模型**，中间件按 `use()` 顺序依次执行。Koa 才是真正的**洋葱模型**，支持 async/await 中间件。

### 2.4 Stream 背压机制

```javascript
const { Readable, Writable, Transform } = require('stream');

// 可读流
const readable = new Readable({
  highWaterMark: 16 * 1024, // 16KB
  read() {
    this.push(Math.random() > 0.5 ? null : 'data');
  }
});

// 可写流
const writable = new Writable({
  highWaterMark: 16 * 1024,
  write(chunk, encoding, callback) {
    process.stdout.write(chunk);
    callback();
  }
});

// 自动处理背压（当写入速度跟不上时，暂停读取）
readable.pipe(writable);

// 手动处理背压
readable.on('data', (chunk) => {
  const canContinue = writable.write(chunk);
  if (!canContinue) {
    readable.pause();
    writable.once('drain', () => readable.resume());
  }
});
```

### 2.5 Buffer 内存分配机制

```javascript
// Buffer 内部使用 Slab 分配器（8KB 为单位）
// 小于 8KB 的小 Buffer 使用预分配池
// 大于 8KB 的大 Buffer 直接分配独立内存

const buf1 = Buffer.alloc(10);           // 安全分配，会清零
const buf2 = Buffer.allocUnsafe(10);     // 快速分配，可能含旧数据
const buf3 = Buffer.from([0x62, 0x75]);  // 从数组创建
const buf4 = Buffer.from('hello', 'utf8'); // 从字符串创建
```

### 2.6 JWT 无状态认证原理

```javascript
// JWT 结构：Header.Payload.Signature
// Header:  {"alg": "HS256", "typ": "JWT"}
// Payload: {"sub": "123", "name": "Alice", "iat": 1516239022}
// Signature: HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)

// 为什么无状态？服务端不存储 session，只验证签名
// 缺点：无法主动失效（需维护黑名单或缩短过期时间）
```

### 2.7 cluster 模块多进程架构

```javascript
const cluster = require('cluster');
const http = require('http');
const numCPUs = require('os').cpus().length;

if (cluster.isMaster) {
  console.log(`Master ${process.pid} is running`);

  // Fork workers
  for (let i = 0; i < numCPUs; i++) {
    cluster.fork();
  }

  cluster.on('exit', (worker, code, signal) => {
    console.log(`Worker ${worker.process.pid} died`);
    cluster.fork(); // 自动重启
  });
} else {
  // Workers can share TCP connection
  http.createServer((req, res) => {
    res.writeHead(200);
    res.end(`Hello from worker ${process.pid}`);
  }).listen(8000);
}
```

> 💡 cluster 模式使用**轮询策略**分发请求到 worker 进程，适合 CPU 密集型场景。I/O 密集型场景用 PM2 的 cluster 模式更便捷。

### 2.8 EventEmitter 自定义事件

```javascript
const EventEmitter = require('events');

class MyEmitter extends EventEmitter {
  constructor() {
    super();
  }
}

const myEmitter = new MyEmitter();
myEmitter.on('event', (data) => {
  console.log('事件触发:', data);
});
myEmitter.emit('event', { key: 'value' });

// 事件最大监听数
console.log(EventEmitter.defaultMaxListeners); // 10
myEmitter.setMaxListeners(20);

// 一次性监听
myEmitter.once('oneTime', () => console.log('只执行一次'));
```

---

## 三、实战场景题

### 3.1 文件上传处理

```javascript
const multer = require('multer');
const path = require('path');

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, 'uploads/'),
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, file.fieldname + '-' + uniqueSuffix + path.extname(file.originalname));
  }
});

const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB
  fileFilter: (req, file, cb) => {
    if (file.mimetype.startsWith('image/')) cb(null, true);
    else cb(new Error('仅支持图片格式'));
  }
});

app.post('/upload', upload.single('avatar'), (req, res) => {
  res.json({ url: `/uploads/${req.file.filename}` });
});
```

### 3.2 日志中间件设计

```javascript
const fs = require('fs');
const path = require('path');

function logger(req, res, next) {
  const start = Date.now();
  const originalEnd = res.end;

  res.end = function(...args) {
    const duration = Date.now() - start;
    const log = `[${new Date().toISOString()}] ${req.method} ${req.originalUrl} ${res.statusCode} ${duration}ms\n`;
    fs.appendFileSync(path.join(__dirname, 'access.log'), log);
    originalEnd.apply(res, args);
  };

  next();
}

app.use(logger);
```

### 3.3 统一错误处理

```javascript
// 自定义错误类
class AppError extends Error {
  constructor(message, statusCode) {
    super(message);
    this.statusCode = statusCode;
    this.isOperational = true;
    Error.captureStackTrace(this, this.constructor);
  }
}

// 404 处理
app.use((req, res, next) => {
  next(new AppError('接口不存在', 404));
});

// 全局错误处理
app.use((err, req, res, next) => {
  const statusCode = err.statusCode || 500;
  const message = err.isOperational ? err.message : '服务器内部错误';

  console.error(`[ERROR] ${err.stack}`);
  res.status(statusCode).json({
    code: statusCode,
    message,
    ...(process.env.NODE_ENV === 'development' && { stack: err.stack })
  });
});
```

### 3.4 接口限流

```javascript
const rateLimit = require('express-rate-limit');

const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 分钟
  max: 100, // 每个 IP 最多 100 次
  message: { code: 429, message: '请求过于频繁，请稍后再试' },
  standardHeaders: true,
  legacyHeaders: false
});

const authLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,
  max: 5,
  message: { code: 429, message: '登录尝试过多，账户已临时锁定' }
});

app.use('/api', limiter);
app.use('/api/auth/login', authLimiter);
```

### 3.5 JWT 中间件实现

```javascript
function auth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ code: 401, message: '未提供认证令牌' });
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.user = decoded;
    next();
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      return res.status(401).json({ code: 401, message: '令牌已过期' });
    }
    return res.status(401).json({ code: 401, message: '无效令牌' });
  }
}

app.get('/api/profile', auth, (req, res) => {
  res.json({ user: req.user });
});
```

### 3.6 MySQL 事务处理

```javascript
const conn = await mysql2.createConnection(config);

try {
  await conn.beginTransaction();

  // 扣减库存
  const [result] = await conn.execute(
    'UPDATE products SET stock = stock - 1 WHERE id = ? AND stock > 0', [productId]
  );
  if (result.affectedRows === 0) {
    throw new Error('库存不足');
  }

  // 创建订单
  await conn.execute(
    'INSERT INTO orders (user_id, product_id, amount) VALUES (?, ?, ?)',
    [userId, productId, price]
  );

  await conn.commit();
  console.log('事务提交成功');
} catch (err) {
  await conn.rollback();
  console.error('事务回滚:', err.message);
} finally {
  conn.release();
}
```

### 3.7 数据库连接池

```javascript
const mysql = require('mysql2/promise');

const pool = mysql.createPool({
  host: 'localhost',
  user: 'root',
  password: '123456',
  database: 'test',
  waitForConnections: true,  // 无可用连接时等待
  connectionLimit: 10,        // 最大连接数
  queueLimit: 0               // 等待队列无上限
});

// 使用连接池查询
const [rows] = await pool.execute('SELECT * FROM users WHERE id = ?', [1]);
```

---

## 四、手写代码题

### 4.1 手写 Promise 封装 fs.readFile

```javascript
const fs = require('fs');

function readFilePromise(filePath, encoding = 'utf-8') {
  return new Promise((resolve, reject) => {
    fs.readFile(filePath, encoding, (err, data) => {
      if (err) reject(err);
      else resolve(data);
    });
  });
}

// 使用
async function main() {
  try {
    const data = await readFilePromise('./config.json');
    console.log(JSON.parse(data));
  } catch (err) {
    console.error('读取失败:', err.message);
  }
}
```

### 4.2 手写简易 Express 中间件机制

```javascript
function createApp() {
  const middlewares = [];

  const app = (req, res) => {
    let idx = 0;
    const next = () => {
      const middleware = middlewares[idx++];
      if (middleware) middleware(req, res, next);
    };
    next();
  };

  app.use = (fn) => {
    middlewares.push(fn);
  };

  return app;
}

// 测试
const app = createApp();
app.use((req, res, next) => {
  console.log('1');
  next();
  console.log('1-end');
});
app.use((req, res, next) => {
  console.log('2');
  next();
  console.log('2-end');
});
```

### 4.3 手写简易 EventEmitter

```javascript
class EventEmitter {
  constructor() {
    this._events = {};
  }

  on(event, listener) {
    if (!this._events[event]) this._events[event] = [];
    this._events[event].push(listener);
    return this;
  }

  emit(event, ...args) {
    const listeners = this._events[event];
    if (listeners) {
      listeners.forEach(listener => listener(...args));
    }
    return this;
  }

  off(event, listener) {
    const listeners = this._events[event];
    if (listeners) {
      this._events[event] = listeners.filter(l => l !== listener);
    }
    return this;
  }

  once(event, listener) {
    const wrapper = (...args) => {
      listener(...args);
      this.off(event, wrapper);
    };
    this.on(event, wrapper);
    return this;
  }
}
```

### 4.4 手写简易 Promise 队列

```javascript
function promiseQueue(tasks, concurrency = 2) {
  let running = 0;
  let index = 0;
  const results = [];

  return new Promise((resolve) => {
    function run() {
      if (index >= tasks.length && running === 0) {
        resolve(results);
        return;
      }

      while (running < concurrency && index < tasks.length) {
        const taskIndex = index++;
        const task = tasks[taskIndex];

        running++;
        Promise.resolve(task())
          .then(result => {
            results[taskIndex] = result;
          })
          .catch(err => {
            results[taskIndex] = err;
          })
          .finally(() => {
            running--;
            run();
          });
      }
    }

    run();
  });
}

// 测试
const tasks = [1, 2, 3, 4, 5].map(n => () =>
  new Promise(resolve => setTimeout(() => resolve(n), 1000))
);
promiseQueue(tasks, 2).then(console.log); // [1,2,3,4,5] 每两个一组执行
```

### 4.5 手写简易 JWT 验证中间件

```javascript
const jwt = require('jsonwebtoken');

function authMiddleware(options = {}) {
  const { secret = process.env.JWT_SECRET, required = true } = options;

  return (req, res, next) => {
    const token = req.cookies?.token
      || req.headers.authorization?.replace('Bearer ', '')
      || req.query?.token;

    if (!token) {
      if (required) return res.status(401).json({ message: '请先登录' });
      req.user = null;
      return next();
    }

    try {
      req.user = jwt.verify(token, secret);
      next();
    } catch (err) {
      if (err.name === 'TokenExpiredError') {
        return res.status(401).json({ message: '登录已过期，请重新登录' });
      }
      return res.status(401).json({ message: '无效的令牌' });
    }
  };
}
```

### 4.6 手写简易 MySQL CRUD 封装

```javascript
class BaseModel {
  constructor(pool, tableName) {
    this.pool = pool;
    this.table = tableName;
  }

  async findAll(condition = {}) {
    const keys = Object.keys(condition);
    let sql = `SELECT * FROM ${this.table}`;
    const params = [];

    if (keys.length > 0) {
      sql += ' WHERE ' + keys.map(k => `${k} = ?`).join(' AND ');
      params.push(...Object.values(condition));
    }

    const [rows] = await this.pool.execute(sql, params);
    return rows;
  }

  async findById(id) {
    const [rows] = await this.pool.execute(
      `SELECT * FROM ${this.table} WHERE id = ?`, [id]
    );
    return rows[0];
  }

  async create(data) {
    const keys = Object.keys(data);
    const values = Object.values(data);
    const placeholders = keys.map(() => '?').join(', ');

    const [result] = await this.pool.execute(
      `INSERT INTO ${this.table} (${keys.join(', ')}) VALUES (${placeholders})`, values
    );
    return result.insertId;
  }

  async update(id, data) {
    const keys = Object.keys(data);
    const values = Object.values(data);

    const [result] = await this.pool.execute(
      `UPDATE ${this.table} SET ${keys.map(k => `${k} = ?`).join(', ')} WHERE id = ?`,
      [...values, id]
    );
    return result.affectedRows > 0;
  }

  async delete(id) {
    const [result] = await this.pool.execute(
      `DELETE FROM ${this.table} WHERE id = ?`, [id]
    );
    return result.affectedRows > 0;
  }
}
```

---

## 五、系统设计题

### 5.1 设计用户管理后台 API

**需求**：用户注册、登录、CRUD、角色权限

**设计方案**：

```javascript
// API 路由设计
// POST   /api/users/register    注册
// POST   /api/users/login       登录
// GET    /api/users             用户列表（需 admin 角色）
// GET    /api/users/:id         用户详情
// PUT    /api/users/:id         更新用户信息
// DELETE /api/users/:id         删除用户（需 admin）

// 权限中间件
function requireRole(role) {
  return (req, res, next) => {
    if (req.user.role !== role) {
      return res.status(403).json({ message: '权限不足' });
    }
    next();
  };
}

// 注册时密码加密
const bcrypt = require('bcryptjs');
const salt = await bcrypt.genSalt(10);
const hashedPassword = await bcrypt.hash(password, salt);
```

### 5.2 设计文章发布系统

| 模块 | 表名 | 核心字段 |
|------|------|---------|
| 文章分类 | `categories` | id, name, slug, description |
| 文章 | `articles` | id, title, content, category_id, author_id, status, created_at |
| 评论 | `comments` | id, article_id, user_id, content, parent_id |
| 标签 | `tags` | id, name |
| 文章标签 | `article_tags` | article_id, tag_id |

```javascript
// 文章分页查询
router.get('/articles', async (req, res) => {
  const { page = 1, pageSize = 10, categoryId, status } = req.query;
  const offset = (page - 1) * pageSize;

  let where = '1=1';
  const params = [];

  if (categoryId) { where += ' AND a.category_id = ?'; params.push(categoryId); }
  if (status) { where += ' AND a.status = ?'; params.push(status); }

  const [rows] = await pool.execute(
    `SELECT a.*, c.name AS category_name, u.username AS author_name
     FROM articles a
     LEFT JOIN categories c ON a.category_id = c.id
     LEFT JOIN users u ON a.author_id = u.id
     WHERE ${where}
     ORDER BY a.created_at DESC
     LIMIT ? OFFSET ?`,
    [...params, parseInt(pageSize), offset]
  );

  const [[{ total }]] = await pool.execute(
    `SELECT COUNT(*) as total FROM articles a WHERE ${where}`, params
  );

  res.json({ list: rows, total, page: parseInt(page), pageSize: parseInt(pageSize) });
});
```

### 5.3 Session 与 Redis 集成方案

```javascript
const session = require('express-session');
const RedisStore = require('connect-redis').default;
const { createClient } = require('redis');

const redisClient = createClient({
  url: process.env.REDIS_URL || 'redis://localhost:6379'
});
redisClient.connect().catch(console.error);

app.use(session({
  store: new RedisStore({ client: redisClient }),
  secret: process.env.SESSION_SECRET,
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    maxAge: 24 * 60 * 60 * 1000 // 24h
  }
}));
```

### 5.4 内存泄漏排查方案

```bash
# 1. 使用 heapdump 生成堆快照
npm install heapdump
node -r heapdump app.js
# kill -USR2 <pid> 触发快照

# 2. 使用 clinic 诊断
npm install -g clinic
clinic doctor -- node app.js
clinic flame -- node app.js

# 3. 使用 Chrome DevTools Memory 面板分析
node --inspect app.js
```

```javascript
// 常见内存泄漏来源
// 1. 全局变量未释放
global.cache = new Map(); // 需要限制大小

// 2. 闭包引用
function leak() {
  const largeData = new Array(1000000).fill('x');
  return function() {
    console.log(largeData.length); // largeData 永远无法回收
  };
}

// 3. 事件监听未移除
process.on('message', handler); // 需要 process.off('message', handler)

// 4. 定时器未清除
const timer = setInterval(() => {}, 1000); // 需要 clearInterval(timer)
```

---

## 六、常见坑点与最佳实践

| 坑点 | 说明 | 最佳实践 |
|------|------|---------|
| `res.send()` 后继续执行 | Express 不会自动 return，后续代码仍会执行 | `return res.json(...)` 或加 `else` |
| `forEach` 中 await 无效 | `forEach` 不支持 async，需用 `for...of` | 使用 `for...of` 或 `Promise.all` |
| `req.body` 为 undefined | 未配置 body-parser | `app.use(express.json())` |
| 回调地狱 | 多层嵌套回调难以维护 | 使用 async/await 或 Promise |
| 未捕获的 Promise 异常 | Node 14 以下会静默失败 | 全局 `process.on('unhandledRejection', ...)` |
| 生产环境使用 `--inspect` | 暴露调试端口有安全风险 | 仅用于本地开发，使用 `NODE_ENV=production` |
| MySQL 注入 | SQL 字符串拼接 | 始终使用 `?` 占位符 |
| JWT 泄漏 | Token 存 localStorage 易被 XSS 窃取 | 存 httpOnly Cookie + CSRF Token |
| 文件描述符泄漏 | 读取文件后未关闭 | 使用 `fs.promises` + `try/finally` |
| `npm install` 无 lockfile | 不同环境依赖版本不一致 | 提交 `package-lock.json` 到版本控制 |
| 中间件顺序错误 | 路由应放在错误处理之前 | 404 中间件放最后 |
| `require` 路径过长 | 使用 `../../` 难以维护 | 配置 `module-alias` 或用 `app-module-path` |

---

## 七、面试回答模板

### 7.1 "请解释 Node.js 的事件循环"

```text
Node.js 事件循环基于 libuv 实现，共有 6 个阶段：
1. timers - 执行 setTimeout/setInterval 回调
2. pending callbacks - 执行延迟到下一轮的 I/O 回调
3. idle, prepare - 内部使用
4. poll - 轮询 I/O 事件（核心阶段，阻塞等待）
5. check - 执行 setImmediate 回调
6. close callbacks - 执行 close 事件回调

每个阶段之间，会先清空 process.nextTick 队列，再清空微任务队列。
理解事件循环对写好异步代码、避免性能问题至关重要。
```

### 7.2 "Express 和 Koa 有什么区别？"

```text
1. 中间件模型：Express 是线性请求-响应模型，Koa 是洋葱模型（支持 async/await）
2. 内置功能：Express 内置路由、静态文件、视图引擎；Koa 极简，需通过中间件扩展
3. 错误处理：Express 用 4 参数错误中间件；Koa 用 try/catch，错误自动冒泡
4. 社区生态：Express 社区更成熟，插件更多；Koa 更现代化，适合 API 服务
```

### 7.3 "如何处理 Node.js 中的错误？"

```text
- 同步代码：try/catch 捕获异常
- 异步回调：错误优先回调（err, result）模式
- Promise：.catch() 或 async/await try/catch
- 全局未捕获：process.on('uncaughtException') 和 process.on('unhandledRejection')
- Express 错误中间件：4 参数中间件统一处理
- 最佳实践：区分操作错误（预期错误）和编程错误（bug），操作错误优雅处理，编程错误崩溃重启
```

### 7.4 "JWT 和 Session 怎么选？"

```text
- JWT 适合：无状态API、移动端、微服务、跨域单点登录
- Session 适合：传统Web应用、需要服务端主动失效、敏感操作场景
- 混合方案：短时 JWT + refresh token + Redis 黑名单
- 安全建议：JWT 存 httpOnly Cookie，设置合理过期时间，使用强签名密钥
```

### 7.5 "如何优化 Node.js 应用性能？"

```text
1. 使用 cluster 或 PM2 多进程发挥多核 CPU 优势
2. 数据库查询加索引，使用连接池
3. 使用 Redis 缓存热点数据减少数据库查询
4. 静态文件用 Nginx 反向代理，不用 Node 直接服务
5. 使用 Stream 处理大文件，避免内存溢出
6. 启用 gzip 压缩（compression 中间件）
7. 使用 async/await 避免阻塞事件循环
8. 定期检查内存泄漏（heapdump/clinic）
```

---

## 八、快速查漏补缺 Checklist

- [ ] 能完整描述事件循环 6 个阶段
- [ ] 理解微任务与宏任务的执行顺序
- [ ] 掌握 CommonJS 和 ES Module 区别
- [ ] 能手写简易中间件机制
- [ ] 掌握 Express 路由和中间件使用
- [ ] 理解 JWT 结构（Header.Payload.Signature）
- [ ] 会用 mysql2 连接池操作数据库
- [ ] 掌握 Stream 背压处理方式
- [ ] 理解 Buffer 与二进制数据处理
- [ ] 掌握 PM2 基本命令与 cluster 模式
- [ ] 了解 child_process（spawn/exec/fork 区别）
- [ ] 知道如何处理内存泄漏
- [ ] 掌握 CORS 跨域配置
- [ ] 了解 RESTful API 设计规范
- [ ] 掌握 async/await 错误处理
- [ ] 了解中间件执行顺序与错误处理流程
- [ ] 理解 npm lockfile 的作用
- [ ] 掌握环境变量管理（dotenv）
- [ ] 了解 helmet、cors、express-rate-limit 等安全中间件
- [ ] 会使用 bcryptjs 进行密码加密存储
