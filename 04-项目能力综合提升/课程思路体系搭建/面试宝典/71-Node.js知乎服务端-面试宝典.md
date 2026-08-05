# Node.js 71 面试宝典
> 基于《Node.js 知乎服务端开发》重点覆盖服务端架构设计、性能优化、安全防护与生产部署

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

### 1.1 Node.js 服务端架构特点

| 特性 | 说明 | 优势 |
|------|------|------|
| 单线程事件循环 | 基于 libuv 的事件驱动模型 | 高并发 I/O 密集型场景性能出色 |
| 非阻塞 I/O | 异步操作不阻塞主线程 | 资源利用率高，节省内存 |
| V8 引擎 | Google 开源的高性能 JS 引擎 | 执行速度快，JIT 编译优化 |
| npm 生态 | 全球最大的包管理器 | 丰富的第三方模块支持 |
| 前后端同构 | 同一套语言开发前后端 | 降低学习成本，代码复用 |

### 1.2 服务端项目分层架构

```javascript
project/
  ├── app.js                  # 应用入口
  ├── config/
  │   ├── index.js           # 通用配置
  │   ├── db.js              # 数据库配置
  │   └── jwt.js             # JWT 配置
  ├── routes/                 # 路由层（路由定义，参数校验）
  │   ├── index.js           # 路由聚合
  │   ├── users.js
  │   ├── questions.js
  │   └── answers.js
  ├── controllers/            # 控制器层（请求处理，业务调度）
  │   ├── users.js
  │   └── questions.js
  ├── services/               # 服务层（核心业务逻辑）
  │   ├── auth.js
  │   └── topic.js
  ├── models/                 # 数据模型层（Mongoose Schema）
  │   ├── User.js
  │   ├── Question.js
  │   └── Answer.js
  ├── middlewares/             # 中间件层
  │   ├── auth.js
  │   ├── error.js
  │   ├── validator.js
  │   └── pagination.js
  ├── utils/                  # 工具函数
  │   ├── helper.js
  │   └── logger.js
  └── public/                 # 静态资源
      └── uploads/
```

> 🎯 分层架构的核心原则：**每一层只关注自己的职责**，路由层只做路由分发，控制器层处理请求上下文，服务层实现业务逻辑，模型层处理数据持久化。

### 1.3 Koa 应用生命周期

```javascript
const Koa = require('koa');
const app = new Koa();

// 1. 初始化阶段 - 注册中间件
app.use(middleware1);
app.use(middleware2);

// 2. 启动阶段
app.listen(3000, () => {
  console.log('Server started on port 3000');
});

// 优雅关闭
process.on('SIGTERM', async () => {
  console.log('Received SIGTERM, shutting down gracefully...');
  await mongoose.disconnect();   // 关闭数据库连接
  server.close(() => {
    console.log('Server closed');
    process.exit(0);
  });
});
```

### 1.4 环境变量与配置管理

```javascript
// config/index.js
require('dotenv').config();

const config = {
  development: {
    port: 3000,
    dbUrl: 'mongodb://localhost:27017/zhihu-dev',
    jwtSecret: 'dev_secret',
    jwtExpiresIn: '7d'
  },
  production: {
    port: process.env.PORT || 3000,
    dbUrl: process.env.MONGODB_URL,
    jwtSecret: process.env.JWT_SECRET,
    jwtExpiresIn: '1d'
  },
  test: {
    port: 3001,
    dbUrl: 'mongodb://localhost:27017/zhihu-test',
    jwtSecret: 'test_secret',
    jwtExpiresIn: '1h'
  }
};

module.exports = config[process.env.NODE_ENV || 'development'];
```

### 1.5 数据库连接管理

```javascript
// config/db.js
const mongoose = require('mongoose');

async function connectDB() {
  try {
    await mongoose.connect(process.env.MONGODB_URL, {
      maxPoolSize: 10,           // 连接池大小
      serverSelectionTimeoutMS: 5000,
      socketTimeoutMS: 45000,
    });
    console.log('MongoDB connected successfully');
  } catch (err) {
    console.error('MongoDB connection error:', err.message);
    process.exit(1);
  }

  mongoose.connection.on('error', (err) => {
    console.error('MongoDB runtime error:', err);
  });

  mongoose.connection.on('disconnected', () => {
    console.log('MongoDB disconnected');
  });
}

module.exports = { connectDB };
```

### 1.6 日志系统设计

```javascript
// utils/logger.js
const fs = require('fs');
const path = require('path');

const LOG_LEVELS = { DEBUG: 0, INFO: 1, WARN: 2, ERROR: 3 };
const CURRENT_LEVEL = process.env.LOG_LEVEL || 'INFO';

class Logger {
  constructor() {
    this.logDir = path.join(__dirname, '../../logs');
    if (!fs.existsSync(this.logDir)) {
      fs.mkdirSync(this.logDir, { recursive: true });
    }
  }

  log(level, message, meta = {}) {
    if (LOG_LEVELS[level] < LOG_LEVELS[CURRENT_LEVEL]) return;

    const timestamp = new Date().toISOString();
    const logEntry = JSON.stringify({
      timestamp, level, message, ...meta,
      pid: process.pid
    }) + '\n';

    // 控制台输出
    if (level === 'ERROR') console.error(logEntry);
    else console.log(logEntry);

    // 文件日志
    const fileName = `${level.toLowerCase()}.log`;
    fs.appendFileSync(path.join(this.logDir, fileName), logEntry);
  }

  info(message, meta) { this.log('INFO', message, meta); }
  warn(message, meta) { this.log('WARN', message, meta); }
  error(message, meta) { this.log('ERROR', message, meta); }
  debug(message, meta) { this.log('DEBUG', message, meta); }
}

module.exports = new Logger();
```

### 1.7 安全中间件配置

```javascript
const helmet = require('koa-helmet');
const cors = require('@koa/cors');
const ratelimit = require('koa-ratelimit');

// 安全头
app.use(helmet());

// 跨域配置
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  credentials: true,
  allowMethods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
  allowHeaders: ['Content-Type', 'Authorization']
}));

// 接口限流（Redis 存储）
const rateLimitDb = new Map(); // 生产环境用 Redis
app.use(ratelimit({
  driver: 'memory',
  db: rateLimitDb,
  duration: 60000,       // 1 分钟
  max: 100,              // 最多 100 次
  errorMessage: { code: 429, message: '请求过于频繁，请稍后再试' }
}));
```

### 1.8 请求日志中间件

```javascript
// middlewares/requestLogger.js
async function requestLogger(ctx, next) {
  const start = Date.now();
  const requestId = Date.now().toString(36) + Math.random().toString(36).substr(2, 5);

  ctx.state.requestId = requestId;
  ctx.set('X-Request-Id', requestId);

  try {
    await next();
    const duration = Date.now() - start;
    logger.info('request completed', {
      requestId,
      method: ctx.method,
      url: ctx.url,
      status: ctx.status,
      duration: `${duration}ms`
    });
  } catch (err) {
    // 错误已在错误中间件处理
    const duration = Date.now() - start;
    logger.error('request failed', {
      requestId,
      method: ctx.method,
      url: ctx.url,
      status: err.status || 500,
      duration: `${duration}ms`,
      error: err.message
    });
  }
}

app.use(requestLogger);
```

### 1.9 请求参数校验策略

| 校验层级 | 校验方式 | 作用 |
|---------|---------|------|
| 路由层 | koa-router params | 校验路径参数是否合法 |
| 中间件层 | koa-parameter / Joi | 校验请求体格式 |
| 服务层 | 自定义验证函数 | 校验业务逻辑约束 |
| 数据层 | Mongoose Schema validators | 校验数据持久化约束 |

### 1.10 接口版本管理

```javascript
// routes/index.js - 路由聚合
const Router = require('koa-router');
const apiV1 = new Router({ prefix: '/api/v1' });
const apiV2 = new Router({ prefix: '/api/v2' });

// V1 路由
apiV1.use('/users', require('./users.v1').routes());
apiV1.use('/questions', require('./questions.v1').routes());

// V2 路由（向前兼容）
apiV2.use('/users', require('./users.v2').routes());
apiV2.use('/questions', require('./questions.v2').routes());

// 同时注册两个版本
app.use(apiV1.routes());
app.use(apiV2.routes());
```

---

## 二、深度原理剖析

### 2.1 Node.js Cluster 多进程架构

```javascript
const cluster = require('cluster');
const os = require('os');

if (cluster.isMaster) {
  const numCPUs = os.cpus().length;
  console.log(`Master ${process.pid} is running`);

  // Fork workers
  for (let i = 0; i < numCPUs; i++) {
    cluster.fork();
  }

  // Worker 退出自动重启
  cluster.on('exit', (worker, code, signal) => {
    console.log(`Worker ${worker.process.pid} died (signal: ${signal})`);
    console.log('Starting a new worker...');
    cluster.fork();
  });

  // 优雅退出
  process.on('SIGTERM', () => {
    for (const id in cluster.workers) {
      cluster.workers[id].kill();
    }
    process.exit(0);
  });
} else {
  // Worker 进程
  const Koa = require('koa');
  const app = new Koa();

  // 每个 worker 监听同一个端口
  app.listen(3000);
  console.log(`Worker ${process.pid} started`);
}
```

> 💡 PM2 内部也是基于 cluster 模块实现，但提供了更完善的进程管理、日志、监控能力。PM2 的 cluster 模式会自动处理进程重启、负载均衡等复杂逻辑。

### 2.2 PM2 进程管理进阶

```javascript
// ecosystem.config.js
module.exports = {
  apps: [
    {
      name: 'zhihu-api',
      script: './app.js',
      instances: 'max',              // 自动使用所有 CPU
      exec_mode: 'cluster',          // 集群模式
      watch: false,                  // 生产环境关闭文件监听
      max_memory_restart: '500M',    // 内存超过 500M 自动重启
      merge_logs: true,              // 合并日志
      log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
      error_file: './logs/pm2-error.log',
      out_file: './logs/pm2-out.log',
      env: {
        NODE_ENV: 'production',
        PORT: 3000
      },
      // 健康检查
      min_uptime: '10s',
      max_restarts: 10,
      // 零停机部署
      listen_timeout: 3000,
      kill_timeout: 5000,
      // 灰度发布
      increment_var: 'PORT'
    }
  ],
  // 部署配置
  deploy: {
    production: {
      user: 'node',
      host: 'your-server.com',
      ref: 'origin/main',
      repo: 'git@github.com:org/zhihu-api.git',
      path: '/var/www/zhihu-api',
      'post-deploy': 'npm install --production && pm2 reload ecosystem.config.js --env production'
    }
  }
};
```

### 2.3 内存泄漏检测与分析

```javascript
// 使用 heapdump 生成堆快照
const heapdump = require('heapdump');

// 手动触发快照（生产环境谨慎使用）
heapdump.writeSnapshot('./heap-' + Date.now() + '.heapsnapshot');

// 或者通过信号触发（推荐）
process.on('SIGUSR2', () => {
  heapdump.writeSnapshot();
});

// 使用 clinic 进行诊断
// clinic doctor -- node app.js
// clinic flame -- node app.js

// 常见内存泄漏场景
// 1. 全局缓存未限制大小
const globalCache = new Map();
// 解决：使用 lru-cache 限制大小
const LRU = require('lru-cache');
const cache = new LRU({ max: 500, maxAge: 1000 * 60 * 60 });

// 2. 闭包引用导致无法回收
// 3. 事件监听器未移除
// 4. 大量定时器未清理
// 5. Stream 未销毁
```

### 2.4 数据库查询性能优化

```javascript
// 1. 使用 lean() 加速只读查询
// 默认 Mongoose 返回全功能文档对象，包含大量方法
// lean() 返回纯 JSON，性能提升 5-10 倍
const topics = await Topic.find(query)
  .lean()
  .limit(pageSize);

// 2. 使用 select 限制返回字段
const user = await User.findById(id)
  .select('name avatar email')  // 只返回需要的字段
  .lean();

// 3. 批量操作（避免循环单条操作）
const userIds = [id1, id2, id3];
await User.updateMany(
  { _id: { $in: userIds } },
  { $set: { status: 'active' } }
);

// 4. 索引覆盖查询
// 创建复合索引让查询完全在索引中完成
topicSchema.index({ name: 1, createdAt: -1 });

// 5. 分页优化（避免 skip 大偏移）
// 传统方式（skip 越大越慢）：
.find().skip(10000).limit(20)
// 优化方式（使用游标）：
.find({ _id: { $gt: lastId } }).limit(20).sort({ _id: 1 })
```

### 2.5 中间件执行流程与错误传播

```javascript
// Koa 错误冒泡机制
app.use(async (ctx, next) => {
  try {
    await next();
  } catch (err) {
    // 捕获所有下游中间件抛出的错误
    ctx.status = err.status || 500;
    ctx.body = {
      code: ctx.status,
      message: err.expose ? err.message : '服务器内部错误'
    };
    ctx.app.emit('error', err, ctx); // 触发全局错误事件
  }
});

app.on('error', (err, ctx) => {
  // 全局错误日志记录
  logger.error('Unhandled error', {
    error: err.message,
    stack: err.stack,
    url: ctx?.url
  });
});

// 错误类型区分
// 1. 操作错误（预期错误）：参数校验失败等
//    需友好返回，不崩溃进程
// 2. 编程错误（Bug）：未定义变量等
//    让进程崩溃，自动重启恢复
if (err.isOperational) {
  // 操作错误，正常返回
} else {
  // 编程错误，记录日志并崩溃
  process.exit(1);
}
```

### 2.6 接口缓存策略

```javascript
// 服务端缓存（基于 lru-cache）
const LRU = require('lru-cache');

const topicCache = new LRU({
  max: 100,                    // 最多缓存 100 个话题
  maxAge: 1000 * 60 * 5,      // 5 分钟过期
  updateAgeOnGet: true         // 访问时刷新过期时间
});

// 缓存中间件
function cacheMiddleware(duration) {
  return async (ctx, next) => {
    const key = ctx.url;
    const cached = topicCache.get(key);
    if (cached) {
      ctx.body = cached;
      return;
    }

    await next();

    // 只缓存成功响应
    if (ctx.status === 200) {
      topicCache.set(key, ctx.body, duration);
    }
  };
}

// 使用
router.get('/topics/hot', cacheMiddleware(300000), async (ctx) => {
  const hotTopics = await Topic.find()
    .sort({ questionCount: -1 })
    .limit(10)
    .lean();
  ctx.body = hotTopics;
});

// 手动清除缓存
router.post('/topics', authMiddleware, async (ctx) => {
  const topic = await Topic.create(ctx.request.body);
  topicCache.reset(); // 话题变化时清空缓存
  ctx.status = 201;
  ctx.body = topic;
});
```

---

## 三、实战场景题

### 3.1 服务端优雅关闭实现

```javascript
const Koa = require('koa');
const app = new Koa();

const server = app.listen(3000);
logger.info('Server started on port 3000');

// 优雅关闭处理
async function gracefulShutdown(signal) {
  logger.info(`Received ${signal}, starting graceful shutdown...`);

  // 1. 停止接受新请求
  server.close(() => {
    logger.info('HTTP server closed');
  });

  // 2. 等待正在处理的请求完成
  //    Koa 内置等待机制，但可以设置超时
  const forceExit = setTimeout(() => {
    logger.error('Forced shutdown after timeout');
    process.exit(1);
  }, 30000);

  // 3. 关闭数据库连接
  await mongoose.disconnect();
  logger.info('MongoDB disconnected');

  // 4. 关闭其他资源（Redis、消息队列等）
  // await redisClient.quit();

  clearTimeout(forceExit);
  logger.info('Graceful shutdown completed');
  process.exit(0);
}

// 注册信号处理
process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));
```

### 3.2 数据库读写分离策略

```javascript
// config/db.js - 读写分离
const mongoose = require('mongoose');

let readConn, writeConn;

async function setupDatabase() {
  // 主库（写操作）
  writeConn = await mongoose.createConnection(process.env.MONGODB_PRIMARY_URL, {
    maxPoolSize: 10
  });

  // 从库（读操作，多个）
  readConn = await mongoose.createConnection(process.env.MONGODB_SECONDARY_URL, {
    maxPoolSize: 20,
    readPreference: 'secondaryPreferred' // 优先从从库读取
  });

  // 注册 Schema 时区分读写
  const UserSchema = new mongoose.Schema({ ... });
  writeConn.model('User', UserSchema);
  readConn.model('User', UserSchema);

  logger.info('Database read/write separation configured');
}

// 使用示例
const UserRead = readConn.model('User');
const UserWrite = writeConn.model('User');

// 读操作
const users = await UserRead.find({ status: 'active' });

// 写操作
const user = await UserWrite.create({ name: 'Alice' });
```

### 3.3 批量接口性能优化

```javascript
// 问题：N+1 查询（循环查数据库）
// 反例：循环内查数据库
const questions = await Question.find({});
for (const question of questions) {
  const answerCount = await Answer.countDocuments({ questionId: question._id });
  question.answerCount = answerCount; // ❌ 每次循环都查一次数据库
}

// 正例：批量聚合查询
const questions = await Question.aggregate([
  {
    $lookup: {
      from: 'answers',
      localField: '_id',
      foreignField: 'questionId',
      as: 'answers'
    }
  },
  {
    $project: {
      title: 1,
      content: 1,
      answerCount: { $size: '$answers' },
      latestAnswer: { $arrayElemAt: ['$answers', -1] }
    }
  },
  { $sort: { createdAt: -1 } },
  { $limit: 20 }
]);

// 批量 populate
const questions = await Question.find({})
  .populate({
    path: 'author',
    select: 'name avatar',
    options: { lean: true }
  })
  .lean();
```

### 3.4 健康检查端点

```javascript
// 健康检查路由（用于负载均衡器）
router.get('/health', async (ctx) => {
  const health = {
    status: 'UP',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    memoryUsage: process.memoryUsage(),
    checks: {}
  };

  // 检查数据库连接
  try {
    await mongoose.connection.db.admin().ping();
    health.checks.database = { status: 'UP', latency: '...' };
  } catch (err) {
    health.checks.database = { status: 'DOWN', error: err.message };
    health.status = 'DEGRADED';
  }

  // 检查 Redis（如有）
  // try { await redisClient.ping(); } catch {}

  ctx.status = health.status === 'UP' ? 200 : 503;
  ctx.body = health;
});
```

### 3.5 错误追踪与报警

```javascript
// 使用 Sentry 进行错误追踪
const Sentry = require('@sentry/node');

Sentry.init({
  dsn: process.env.SENTRY_DSN,
  environment: process.env.NODE_ENV,
  tracesSampleRate: 0.1, // 10% 采样率
  beforeSend(event) {
    // 过滤敏感信息
    if (event.request?.data?.password) {
      event.request.data.password = '***';
    }
    return event;
  }
});

// Koa 集成
app.on('error', (err, ctx) => {
  Sentry.withScope((scope) => {
    scope.setTag('request_id', ctx.state.requestId);
    scope.setExtra('url', ctx.url);
    scope.setExtra('method', ctx.method);
    Sentry.captureException(err);
  });
});

// API 响应时间报警
const slowApiThreshold = 2000; // 2秒
app.use(async (ctx, next) => {
  const start = Date.now();
  await next();
  const duration = Date.now() - start;

  if (duration > slowApiThreshold) {
    logger.warn('slow API detected', {
      url: ctx.url,
      method: ctx.method,
      duration: `${duration}ms`
    });
    // 可以发送报警通知
  }
});
```

### 3.6 灰度发布与版本兼容

```javascript
// 功能开关中间件
const featureFlags = {
  newCommentSystem: false,
  recommendationV2: true
};

function featureFlag(flagName) {
  return async (ctx, next) => {
    ctx.state.features = { ...featureFlags };
    // 允许特定用户开启新功能
    if (ctx.headers['x-beta-user']) {
      ctx.state.features.newCommentSystem = true;
    }
    await next();
  };
}

// 请求 header 版本控制
function apiVersion(versionMap) {
  return async (ctx, next) => {
    const version = ctx.headers['accept-version'] || 'v1';

    if (versionMap[version]) {
      ctx.state.apiVersion = version;
      await versionMap[version](ctx, next);
    } else {
      ctx.throw(400, `Unsupported API version: ${version}`);
    }
  };
}

// 使用
router.get('/questions/:id', apiVersion({
  v1: getQuestionV1,
  v2: getQuestionV2  // 新版本增加推荐答案
}));
```

---

## 四、手写代码题

### 4.1 手写 Promise 版数据库重连机制

```javascript
async function connectWithRetry(maxRetries = 5, delay = 2000) {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      await mongoose.connect(process.env.MONGODB_URL);
      logger.info('Database connected successfully');
      return;
    } catch (err) {
      logger.error(`Connection attempt ${attempt}/${maxRetries} failed: ${err.message}`);

      if (attempt === maxRetries) {
        logger.error('All connection attempts failed');
        throw err;
      }

      // 指数退避
      const waitTime = delay * Math.pow(2, attempt - 1);
      logger.info(`Retrying in ${waitTime}ms...`);
      await new Promise(resolve => setTimeout(resolve, waitTime));
    }
  }
}

// 定期保活
setInterval(async () => {
  try {
    await mongoose.connection.db.admin().ping();
  } catch {
    logger.error('Database ping failed, attempting reconnect');
    connectWithRetry().catch(err => {
      logger.error('Reconnection failed:', err.message);
    });
  }
}, 60000); // 每分钟 ping 一次
```

### 4.2 手写限流中间件

```javascript
// 简易滑动窗口限流
function slidingWindowRateLimit(options = {}) {
  const { windowMs = 60000, max = 100 } = options;
  const hits = new Map();

  return async (ctx, next) => {
    const ip = ctx.ip;
    const now = Date.now();
    const windowStart = now - windowMs;

    if (!hits.has(ip)) {
      hits.set(ip, []);
    }

    // 清理过期的请求记录
    const requests = hits.get(ip).filter(t => t > windowStart);

    if (requests.length >= max) {
      ctx.set('Retry-After', Math.ceil(windowMs / 1000));
      ctx.status = 429;
      ctx.body = { code: 429, message: 'Too Many Requests' };
      return;
    }

    requests.push(now);
    hits.set(ip, requests);

    // 设置响应头
    ctx.set('X-RateLimit-Limit', max);
    ctx.set('X-RateLimit-Remaining', max - requests.length);
    ctx.set('X-RateLimit-Reset', Math.ceil((windowStart + windowMs) / 1000));

    await next();
  };
}
```

### 4.3 手写简易日志收集器

```javascript
class LogCollector {
  constructor(options = {}) {
    this.buffer = [];
    this.bufferSize = options.bufferSize || 100;
    this.flushInterval = options.flushInterval || 5000;
    this.transport = options.transport || this.defaultTransport;

    this._startFlushTimer();
  }

  log(level, message, meta = {}) {
    const entry = {
      timestamp: new Date().toISOString(),
      level,
      message,
      meta,
      pid: process.pid,
      hostname: require('os').hostname()
    };

    this.buffer.push(entry);

    if (this.buffer.length >= this.bufferSize) {
      this.flush();
    }
  }

  async flush() {
    if (this.buffer.length === 0) return;

    const batch = this.buffer.splice(0);
    try {
      await this.transport(batch);
    } catch (err) {
      console.error('Failed to flush logs:', err);
      // 失败时重新入队
      this.buffer.unshift(...batch);
    }
  }

  async defaultTransport(batch) {
    // 默认输出到 stdout
    batch.forEach(entry => {
      const line = JSON.stringify(entry);
      if (entry.level === 'ERROR') {
        process.stderr.write(line + '\n');
      } else {
        process.stdout.write(line + '\n');
      }
    });
  }

  _startFlushTimer() {
    setInterval(() => this.flush(), this.flushInterval);
  }
}
```

### 4.4 手写简易连接池

```javascript
class ConnectionPool {
  constructor(createConnection, options = {}) {
    this.createConnection = createConnection;
    this.maxSize = options.maxSize || 10;
    this.minSize = options.minSize || 2;
    this.idleTimeout = options.idleTimeout || 30000;

    this.pool = [];
    this.active = new Set();
    this.waiters = [];
    this._initialized = false;
  }

  async initialize() {
    for (let i = 0; i < this.minSize; i++) {
      const conn = await this.createConnection();
      this.pool.push(conn);
    }
    this._initialized = true;
  }

  async acquire() {
    // 有空闲连接，直接返回
    if (this.pool.length > 0) {
      const conn = this.pool.pop();
      this.active.add(conn);
      return conn;
    }

    // 还可以创建新连接
    if (this.active.size < this.maxSize) {
      const conn = await this.createConnection();
      this.active.add(conn);
      return conn;
    }

    // 连接池已满，等待释放
    return new Promise(resolve => {
      this.waiters.push(resolve);
    });
  }

  release(conn) {
    this.active.delete(conn);

    // 优先分配给等待者
    if (this.waiters.length > 0) {
      const waiter = this.waiters.shift();
      this.active.add(conn);
      waiter(conn);
      return;
    }

    // 放回池中
    if (this.pool.length < this.minSize) {
      this.pool.push(conn);
    } else {
      // 超时后关闭空闲连接
      const timer = setTimeout(() => {
        const idx = this.pool.indexOf(conn);
        if (idx !== -1) {
          this.pool.splice(idx, 1);
          conn.close();
        }
      }, this.idleTimeout);
      conn._idleTimer = timer;
      this.pool.push(conn);
    }
  }

  async close() {
    for (const conn of [...this.pool, ...this.active]) {
      if (conn._idleTimer) clearTimeout(conn._idleTimer);
      await conn.close();
    }
    this.pool = [];
    this.active.clear();
  }
}
```

### 4.5 手写 API 响应包装器

```javascript
// controllers/base.js
class BaseController {
  success(ctx, data, message = 'success', status = 200) {
    ctx.status = status;
    ctx.body = {
      code: status,
      message,
      data,
      timestamp: new Date().toISOString()
    };
  }

  created(ctx, data, message = 'created') {
    this.success(ctx, data, message, 201);
  }

  paginated(ctx, { list, total, page, pageSize }) {
    ctx.body = {
      code: 200,
      message: 'success',
      data: {
        list,
        pagination: {
          page: Number(page),
          pageSize: Number(pageSize),
          total,
          totalPages: Math.ceil(total / pageSize)
        }
      },
      timestamp: new Date().toISOString()
    };
  }

  fail(ctx, message = 'error', status = 400, details = null) {
    ctx.status = status;
    ctx.body = {
      code: status,
      message,
      ...(details && { details }),
      timestamp: new Date().toISOString()
    };
  }
}

module.exports = BaseController;

// 使用示例
class UserController extends BaseController {
  async list(ctx) {
    const { page, pageSize } = ctx.state.pagination;
    const [list, total] = await Promise.all([
      User.find().skip((page - 1) * pageSize).limit(pageSize).lean(),
      User.countDocuments()
    ]);
    this.paginated(ctx, { list, total, page, pageSize });
  }
}
```

### 4.6 手写自定义 Mongoose 插件

```javascript
// 软删除插件
function softDeletePlugin(schema) {
  schema.add({
    deletedAt: { type: Date, default: null },
    isDeleted: { type: Boolean, default: false }
  });

  // 查询时自动过滤已删除
  schema.pre('find', function() {
    this.where({ isDeleted: false });
  });

  schema.pre('findOne', function() {
    this.where({ isDeleted: false });
  });

  schema.pre('countDocuments', function() {
    this.where({ isDeleted: false });
  });

  // 自定义方法：软删除
  schema.methods.softDelete = async function() {
    this.isDeleted = true;
    this.deletedAt = new Date();
    return this.save();
  };

  // 自定义方法：恢复
  schema.methods.restore = async function() {
    this.isDeleted = false;
    this.deletedAt = null;
    return this.save();
  };

  // 静态方法：查询所有（含已删除）
  schema.statics.findAllWithDeleted = function() {
    return this.find({});
  };
}

// 使用插件
const questionSchema = new mongoose.Schema({ ... });
questionSchema.plugin(softDeletePlugin);

const Question = mongoose.model('Question', questionSchema);
await question.softDelete(); // 软删除
await question.restore();     // 恢复
```

---

## 五、系统设计题

### 5.1 知乎服务端整体架构设计

```text
                     ┌──────────────┐
                     │   DNS/LB     │
                     │ (Nginx/ALB)  │
                     └──────┬───────┘
                            │
              ┌─────────────┼─────────────┐
              │             │             │
         ┌────▼───┐   ┌────▼───┐   ┌────▼───┐
         │ PM2    │   │ PM2    │   │ PM2    │
         │ Worker1│   │Worker2 │   │Worker N│
         │ :3001  │   │ :3002  │   │ :300N  │
         └───┬────┘   └───┬────┘   └───┬────┘
             │            │            │
    ┌────────┴────────────┴────────────┴────────┐
    │                 Redis Cache               │
    │           (Session / Hot Topics)          │
    └────────────────┬──────────────────────────┘
                     │
    ┌────────────────┴──────────────────────────┐
    │              MongoDB Primary              │
    │           (Write Operations)              │
    └────────────────┬──────────────────────────┘
                     │ Replica Set
    ┌────────────────┼──────────────────────────┐
    │         ┌──────┴──────┐                   │
    │         │ MongoDB     │                   │
    │         │ Secondary 1 │ ... Secondary N    │
    │         │ (Read Ops)  │                   │
    │         └─────────────┘                   │
    └───────────────────────────────────────────┘

分层职责：
- 负载均衡层：TLS 终止、路由分发、限流
- 应用层：业务逻辑处理、认证授权
- 缓存层：热点数据缓存、session 存储
- 数据层：数据持久化、读写分离、备份
```

### 5.2 服务端 API 权限体系设计

```javascript
// 基于角色的访问控制（RBAC）
const roles = {
  guest:     { permissions: ['read:topic', 'read:question'] },
  user:      { permissions: ['read:*', 'create:answer', 'create:question', 'update:self'] },
  moderator: { permissions: ['read:*', 'create:*', 'update:*', 'delete:content'] },
  admin:     { permissions: ['read:*', 'create:*', 'update:*', 'delete:*', 'manage:user'] }
};

// 权限检查中间件
function requirePermission(permission) {
  return async (ctx, next) => {
    const userRole = ctx.state.user?.role || 'guest';
    const userPermissions = roles[userRole]?.permissions || [];

    // 检查是否匹配
    const hasPermission = userPermissions.some(p => {
      // 支持通配符：read:* 匹配 read:topic
      const [action, resource] = permission.split(':');
      return p === `${action}:*` || p === `*:*` || p === permission;
    });

    if (!hasPermission) {
      ctx.throw(403, 'Insufficient permissions');
    }

    // 资源拥有者检查（update:self 可更新自己的资源）
    if (permission === 'update:self' && ctx.params.id !== ctx.state.user.id) {
      ctx.throw(403, '只能操作自己的资源');
    }

    await next();
  };
}

// 使用
router.post('/admin/users', auth, requirePermission('manage:user'), adminHandler);
router.put('/users/:id', auth, requirePermission('update:self'), updateUser);
```

### 5.3 高可用部署架构

| 层级 | 组件 | 方案 | 容灾策略 |
|------|------|------|---------|
| DNS | 域名解析 | 多区域 DNS | DNS 故障转移 |
| 负载均衡 | Nginx / ALB | 反向代理 + 健康检查 | 多节点部署 |
| 应用 | Node.js + PM2 | Cluster 模式，多 worker | 自动重启，无状态设计 |
| 缓存 | Redis Sentinel | 主从复制 | Sentinel 自动故障转移 |
| 数据库 | MongoDB Replica Set | 1 Primary + 2 Secondary | 自动选举新主节点 |
| 文件 | 对象存储 | 阿里云 OSS / AWS S3 | 多区域冗余 |
| 监控 | 日志 + 指标 | Sentry + Prometheus | 多级告警 |

### 5.4 性能压测与容量规划

```bash
# 使用 autocannon 进行压测
npm install -g autocannon

# 测试 GET 接口
autocannon -c 100 -d 30 http://localhost:3000/api/v1/topics

# 测试 POST 接口
autocannon -c 50 -d 30 \
  -m POST \
  -H "Content-Type: application/json" \
  -b '{"name":"Node.js","description":"test"}' \
  http://localhost:3000/api/v1/topics

# 关键指标解读
# Latency: 平均/最大延迟
# Requests/sec: 每秒请求数
# Throughput: 吞吐量（MB/s）
# 1xx/2xx/3xx/4xx/5xx: 状态码分布
# Non 2xx: 异常请求数

# 容量规划公式
# 单个 worker 最大 QPS × worker 数量 × 冗余系数 = 系统总容量
# 冗余系数通常取 0.7（预留 30% 的 buffer）
```

---

## 六、常见坑点与最佳实践

| 坑点 | 说明 | 最佳实践 |
|------|------|---------|
| 生产环境使用 console.log | 性能差，无日志级别 | 使用 winston/pino 等专业日志库 |
| 未设置 NODE_ENV | 无法区分环境 | 启动时设置 `NODE_ENV=production` |
| 未捕获的 Promise 异常 | Node 14 以下静默失败 | 全局 `process.on('unhandledRejection')` |
| 进程崩溃后未重启 | 服务不可用 | 使用 PM2 cluster 模式，自动重启 |
| 数据库连接单点 | 数据库宕机导致全站不可用 | MongoDB Replica Set + 读写分离 |
| 未限制请求体大小 | 大请求耗尽服务器内存 | koa-body 设置 maxFileSize |
| 未设置超时时间 | 慢请求积压 | 设置 socket 和请求超时 |
| 接口无版本控制 | 前端更新困难 | URL 路径或 Header 版本管理 |
| 缺少健康检查 | 负载均衡器无法感知服务状态 | 添加 `/health` 端点 |
| 敏感信息硬编码 | 密码/密钥泄露风险 | 使用环境变量或密钥管理服务 |
| 未开启 gzip | 响应体过大，网络传输慢 | koa-compress 中间件 |
| 数据库缺少索引 | 数据量大时查询极慢 | 使用 `explain()` 分析查询计划 |
| 死锁/活锁 | 进程未响应 | 设置进程看门狗，超时自动重启 |
| 跨域配置过于宽松 | 被恶意网站调用 API | 明确指定允许的 origin |

---

## 七、面试回答模板

### 7.1 "如何保证 Node.js 服务的高可用？"

```text
1. 多进程架构：PM2 cluster 模式利用多核 CPU，worker 崩溃自动重启
2. 优雅关闭：收到 SIGTERM 信号后停止接受新请求，等待正在处理的请求完成，再关闭数据库连接
3. 无状态设计：不把状态存在进程内存中，用 Redis/MongoDB 存储 session，任一 worker 宕机不影响全局
4. 健康检查：提供 /health 端点包含数据库、缓存等依赖的状态检查，供负载均衡器判断
5. 数据库高可用：MongoDB Replica Set + 读写分离，主节点故障自动选举
6. 监控告警：使用 Sentry 追踪错误，PM2 监控进程状态，Prometheus 采集性能指标
7. 容量规划：通过压测确定单机瓶颈，预留 30% 的 buffer
```

### 7.2 "生产环境 Node.js 内存泄漏怎么排查？"

```text
按照这个流程排查：
1. 现象观察：用 PM2 监控内存曲线，如果持续上涨不回落，大概率有泄漏
2. 生成堆快照：用 heapdump 模块在内存上涨前后分别生成堆快照（kill -USR2 <pid>）
3. 对比分析：在 Chrome DevTools Memory 面板加载两个快照，用 Comparison 视图看哪些对象增长最多
4. 常见源头：
   - 全局缓存没有大小限制（用 lru-cache 替代 Map）
   - 事件监听器注册未移除
   - 闭包引用了大对象
   - 定时器/Stream 未正确清理
5. 工具推荐：clinic doctor 可以自动诊断 leak 问题，生成流程图
```

### 7.3 "Node.js 应用的性能优化怎么做？"

```text
从这几个层面逐步优化：

1. 数据库层：索引覆盖查询、限制返回字段（select + lean）、批量操作代替循环单条

2. 应用层：使用连接池、开启 gzip 压缩、缓存热点数据（lru-cache/Redis）、避免阻塞事件循环

3. 架构层：PM2 cluster 多进程、Nginx 反向代理、静态文件 CDN

4. 代码层：使用 Stream 处理大文件、用 for...of 替代 forEach + await、避免内存泄漏

5. 网络层：HTTP/2、Keep-Alive、压缩响应体

优化顺序：先定位瓶颈（用 clinic/autocannon 压测），再针对性优化，不要过早优化。
```

### 7.4 "MongoDB 查询慢怎么排查？"

```text
1. 开启慢查询日志：设置 `db.setProfilingLevel(1, { slowms: 100 })`
2. 分析查询计划：`db.collection.find(...).explain('executionStats')`
   查看 totalDocsExamined、nReturned、IXSCAN vs COLLSCAN
3. 优化策略：
   - 确保查询条件有索引覆盖
   - 复合索引遵循 ESR（相等-排序-范围）原则
   - 使用 lean() 减少 Mongoose 文档转换开销
   - 避免使用 $ne/$nin 等无法有效利用索引的操作符
   - 大量分页用游标代替 skip（_id > lastId）
4. 硬件层面：增加内存让热点数据驻留内存、SSD 加速

5. Mongoose 层面：限制 populate 深度、只查询需要的字段
```

### 7.5 "如何设计一个可扩展的 Node.js 项目结构？"

```text
我推荐分层架构 + 模块化的组织方式：

分层架构（关注点分离）：
- routes：只做路由注册和参数校验
- controllers：处理请求上下文，调用 service
- services：实现核心业务逻辑
- models：数据模型定义
- middlewares：跨切面的通用逻辑
- utils：工具函数

模块化：每个业务模块（user、question、answer）有自己独立的 routes、controllers、services 文件，遵循单一职责原则。

扩展性要点：
1. 接口版本控制（/api/v1/、/api/v2/）
2. 配置与环境分离（config 目录 + dotenv）
3. 依赖注入（构造函数注入代替直接 require）
4. 插件化中间件（松耦合、可插拔）
```

---

## 八、快速查漏补缺 Checklist

- [ ] 掌握 Koa 洋葱模型与中间件执行顺序
- [ ] 理解服务端分层架构设计
- [ ] 掌握 PM2 cluster 模式配置与进程管理
- [ ] 理解优雅关闭的实现流程
- [ ] 掌握 MongoDB 索引优化与 explain 分析
- [ ] 理解读写分离和数据库高可用方案
- [ ] 掌握 JWT 认证与权限体系设计（RBAC）
- [ ] 理解内存泄漏排查方法与工具
- [ ] 掌握日志系统设计与分级
- [ ] 理解限流、熔断、降级策略
- [ ] 掌握健康检查与监控告警方案
- [ ] 理解接口版本管理策略
- [ ] 掌握请求参数校验体系
- [ ] 理解缓存策略与缓存失效处理
- [ ] 掌握安全中间件（helmet, cors, ratelimit）
- [ ] 理解 Nginx 反向代理配置
- [ ] 掌握性能压测方法与工具（autocannon）
- [ ] 理解 502/504 等常见生产环境错误排查
- [ ] 掌握环境变量管理与配置分离
- [ ] 理解错误分类（操作错误 vs 编程错误）
