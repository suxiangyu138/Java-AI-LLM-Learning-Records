# Node.js 57 面试宝典
> 基于《Node.js 开发仿知乎服务端》重点覆盖 Koa 框架 + MongoDB + RESTful API + 社区业务模块设计

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

### 1.1 Koa vs Express 核心区别

| 对比维度 | Koa | Express |
|---------|-----|---------|
| 中间件模型 | 洋葱模型（async/await） | 线性模型（回调） |
| 体积 | 极简核心，约 1.5k 行 | 功能丰富，内置路由/视图 |
| ctx 对象 | 封装 req/res 的 context | req/res 分离 |
| 错误处理 | try/catch 自动冒泡 | 4 参数错误中间件 |
| 异步支持 | 原生支持 async/await | 需要额外中间件 |
| 社区生态 | 较年轻 | 非常成熟 |

> 💡 Koa 由 Express 原班人马打造，主打**轻量、优雅、现代化**。适用于纯 API 服务，Express 更适合全栈 Web 应用。

### 1.2 Koa 洋葱模型执行机制

```javascript
const Koa = require('koa');
const app = new Koa();

app.use(async (ctx, next) => {
  console.log('1-in');
  await next();
  console.log('1-out');
});

app.use(async (ctx, next) => {
  console.log('2-in');
  await next();
  console.log('2-out');
});

app.use(async (ctx) => {
  console.log('3-handler');
  ctx.body = 'Hello Koa';
});

// 输出顺序：
// 1-in -> 2-in -> 3-handler -> 2-out -> 1-out
```

> 🎯 洋葱模型核心优势：中间件可以在请求**前后**都执行逻辑，非常适合日志、事务、计时等场景。

### 1.3 Koa 上下文对象 ctx

```javascript
app.use(async (ctx) => {
  // 请求相关
  ctx.request;      // Koa Request 对象
  ctx.req;          // Node 原生 req
  ctx.method;       // GET/POST/PUT/DELETE
  ctx.url;          // 请求路径
  ctx.headers;      // 请求头
  ctx.query;        // 查询参数
  ctx.params;       // 路由参数（需 koa-router）

  // 响应相关
  ctx.response;     // Koa Response 对象
  ctx.res;          // Node 原生 res
  ctx.status = 200; // 状态码
  ctx.body = {};    // 响应体
  ctx.set('X-Custom', 'value'); // 设置响应头

  // 快捷方式
  ctx.throw(400, '参数错误');
  ctx.assert(user, 401, '未登录');
});
```

### 1.4 RESTful API 六大约束

| 约束 | 说明 | 实践要点 |
|------|------|---------|
| 客户端-服务端分离 | UI 与数据存储分离 | 前端独立部署，后端只提供 API |
| 无状态 | 每个请求包含全部信息 | JWT Token 携带认证信息 |
| 可缓存 | 响应隐式或显式标记可缓存 | `Cache-Control`、`ETag` |
| 统一接口 | 资源操作通过统一接口 | HTTP 方法 + 资源 URI |
| 分层系统 | 中间层可代理/网关/负载均衡 | Nginx 反向代理 |
| 按需代码（可选） | 服务端可以扩展客户端功能 | 极少使用 |

### 1.5 koa-router 路由定义

```javascript
const Router = require('koa-router');
const router = new Router({ prefix: '/api/v1' });

// 定义路由
router.get('/users', listUsers);
router.post('/users', createUser);
router.get('/users/:id', getUserById);
router.put('/users/:id', updateUser);
router.delete('/users/:id', deleteUser);

// 嵌套路由
const topicRouter = new Router({ prefix: '/topics' });
topicRouter.get('/', listTopics);
topicRouter.get('/:id', getTopic);

// 注册到应用
app.use(router.routes());
app.use(router.allowedMethods()); // 自动返回 405 Method Not Allowed
```

### 1.6 Mongoose 基础操作

```javascript
const mongoose = require('mongoose');
mongoose.connect('mongodb://localhost:27017/zhihu');

// 定义 Schema
const userSchema = new mongoose.Schema({
  name: { type: String, required: true },
  age: { type: Number, min: 0, max: 150 },
  email: { type: String, match: /^\S+@\S+\.\S+$/ },
  createdAt: { type: Date, default: Date.now }
});

// 创建 Model
const User = mongoose.model('User', userSchema);

// CRUD
await User.create({ name: 'Alice', age: 25 });
const users = await User.find({ age: { $gte: 18 } }).sort({ createdAt: -1 }).limit(10);
const user = await User.findById('660a1b2c3d4e5f6a7b8c9d0e');
await User.findByIdAndUpdate(id, { age: 26 }, { new: true });
await User.findByIdAndDelete(id);
```

### 1.7 全局错误处理

```javascript
const koaJsonError = require('koa-json-error');
const koaParameter = require('koa-parameter');

// 统一错误格式
app.use(koaJsonError({
  postFormat: (e, obj) => {
    return process.env.NODE_ENV === 'production'
      ? { code: obj.status, message: obj.message }
      : obj; // 开发环境返回完整错误
  }
}));

// 参数校验
app.use(koaParameter(app));
ctx.verifyParams({
  name: { type: 'string', required: true },
  age: { type: 'number', required: false }
});
```

### 1.8 MongoDB 文档关系设计

| 关系类型 | 设计方式 | 示例 |
|---------|---------|------|
| 一对一 | 内嵌文档 | `user.profile` |
| 一对多 | 子文档数组 / 引用 | `post.comments[]` |
| 多对多 | 引用数组 | `user.following[]`、`user.followers[]` |

> 💡 MongoDB 推荐**嵌入优先**，但嵌入文档有 16MB 大小限制。对于评论、粉丝等大量数据，使用引用更为合适。

### 1.9 JWT 认证集成（koa-jwt）

```javascript
const jwt = require('koa-jwt');
const jsonwebtoken = require('jsonwebtoken');

// 保护路由（需认证才可访问）
const auth = jwt({ secret: process.env.JWT_SECRET });
router.get('/api/protected', auth, async (ctx) => {
  ctx.body = { user: ctx.state.user };
});

// 登录接口生成 Token
router.post('/api/login', async (ctx) => {
  const { name, password } = ctx.request.body;
  const user = await User.findOne({ name });
  if (!user || !bcrypt.compareSync(password, user.password)) {
    ctx.throw(401, '用户名或密码错误');
  }
  const token = jsonwebtoken.sign(
    { id: user._id, name: user.name },
    process.env.JWT_SECRET,
    { expiresIn: '7d' }
  );
  ctx.body = { token };
});
```

### 1.10 文件上传（koa-body + koa-static）

```javascript
const koaBody = require('koa-body');
const koaStatic = require('koa-static');
const path = require('path');

// 文件上传配置
app.use(koaBody({
  multipart: true,
  formidable: {
    uploadDir: path.join(__dirname, 'public/uploads'),
    keepExtensions: true,
    maxFileSize: 5 * 1024 * 1024 // 5MB
  }
}));

// 静态文件服务
app.use(koaStatic(path.join(__dirname, 'public')));

// 上传接口
router.post('/api/upload', async (ctx) => {
  const file = ctx.request.files.file;
  ctx.body = {
    url: `/uploads/${path.basename(file.path)}`,
    size: file.size
  };
});
```

### 1.11 PM2 部署配置

```javascript
// ecosystem.config.js
module.exports = {
  apps: [{
    name: 'zhihu-api',
    script: 'app.js',
    instances: 'max',       // 多进程
    exec_mode: 'cluster',   // 集群模式
    env: {
      NODE_ENV: 'production',
      PORT: 3000
    },
    max_memory_restart: '500M',
    log_date_format: 'YYYY-MM-DD HH:mm:ss',
    error_file: './logs/err.log',
    out_file: './logs/out.log'
  }]
};
```

### 1.12 Nginx 反向代理配置

```nginx
upstream zhihu_api {
    server 127.0.0.1:3000;
    server 127.0.0.1:3001;
}

server {
    listen 80;
    server_name api.zhihu.com;

    location / {
        proxy_pass http://zhihu_api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /uploads/ {
        alias /var/www/uploads/;
        expires 30d;
    }
}
```

---

## 二、深度原理剖析

### 2.1 Koa 洋葱模型源码级分析

```javascript
// Koa 中间件组合核心（koa-compose）
function compose(middleware) {
  if (!Array.isArray(middleware)) throw new TypeError('Middleware stack must be an array!');

  return function (context, next) {
    let index = -1;

    return dispatch(0);

    function dispatch(i) {
      if (i <= index) return Promise.reject(new Error('next() called multiple times'));
      index = i;

      let fn = middleware[i];
      if (i === middleware.length) fn = next;
      if (!fn) return Promise.resolve();

      try {
        return Promise.resolve(fn(context, dispatch.bind(null, i + 1)));
      } catch (err) {
        return Promise.reject(err);
      }
    }
  };
}
```

> 🎯 关键点：`next()` 返回 Promise，通过 `dispatch(i+1)` 递归调用下一个中间件。await next() 之后的代码在子中间件执行完毕后恢复执行，形成**洋葱切面**。

### 2.2 Mongoose Schema 设计原则

```javascript
// 关注/粉丝关系 Schema
const userSchema = new mongoose.Schema({
  name: { type: String, required: true },
  following: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
  followers: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }]
});

// 话题 Schema（含分页和模糊搜索支持）
const topicSchema = new mongoose.Schema({
  name: { type: String, required: true },
  description: { type: String }
});
topicSchema.index({ name: 'text', description: 'text' }); // 全文索引
topicSchema.index({ name: 1 }); // 普通单字段索引

// 答案 Schema（点赞/收藏）
const answerSchema = new mongoose.Schema({
  content: { type: String, required: true },
  questionId: { type: mongoose.Schema.Types.ObjectId, ref: 'Question' },
  answerer: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
  voteCount: { type: Number, default: 0 },
  voters: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }]
});
```

### 2.3 MongoDB 关联查询（populate vs 聚合）

```javascript
// populate：简单关联（多查一次）
const user = await User.findById(userId)
  .populate('following', 'name avatar')
  .populate('followers', 'name avatar');

// 聚合管道：复杂关联
const result = await Question.aggregate([
  { $match: { _id: questionId } },
  {
    $lookup: {
      from: 'answers',
      localField: '_id',
      foreignField: 'questionId',
      as: 'answers'
    }
  },
  {
    $lookup: {
      from: 'users',
      localField: 'author',
      foreignField: '_id',
      as: 'authorInfo'
    }
  },
  { $unwind: '$authorInfo' },
  { $project: { title: 1, content: 1, 'authorInfo.name': 1, answerCount: { $size: '$answers' } } }
]);
```

> 💡 `populate` 适合简单查询（N+1 友好），`aggregate` 适合复杂聚合统计。

### 2.4 三级嵌套评论模型设计

```javascript
// 评论 Schema（支持三级嵌套）
const commentSchema = new mongoose.Schema({
  content: { type: String, required: true },
  answerId: { type: mongoose.Schema.Types.ObjectId, ref: 'Answer', required: true },
  commentator: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  parentId: { type: mongoose.Schema.Types.ObjectId, ref: 'Comment', default: null }, // 父评论
  rootId: { type: mongoose.Schema.Types.ObjectId, ref: 'Comment', default: null },   // 根评论
  replyTo: { type: mongoose.Schema.Types.ObjectId, ref: 'User', default: null }       // 回复对象
}, { timestamps: true });

// 查询评论树
router.get('/answers/:id/comments', async (ctx) => {
  const comments = await Comment.find({ answerId: ctx.params.id })
    .populate('commentator', 'name avatar')
    .populate('replyTo', 'name')
    .sort({ createdAt: 1 });

  // 构建树形结构
  function buildTree(comments, parentId = null) {
    return comments
      .filter(c => String(c.parentId) === String(parentId))
      .map(c => ({
        ...c.toObject(),
        children: buildTree(comments, c._id)
      }));
  }

  ctx.body = buildTree(comments);
});
```

### 2.5 Koa 自定义中间件模式

```javascript
// 认证中间件
async function authMiddleware(ctx, next) {
  const token = ctx.headers.authorization?.replace('Bearer ', '');
  if (!token) {
    ctx.throw(401, '请先登录');
  }

  try {
    const decoded = jsonwebtoken.verify(token, process.env.JWT_SECRET);
    ctx.state.user = decoded; // 挂载到 state
    await next();
  } catch (err) {
    ctx.throw(401, 'Token 无效或已过期');
  }
}

// 资源拥有者检查
async function checkOwner(ctx, next) {
  const resource = await ctx.model.findById(ctx.params.id);
  if (!resource) ctx.throw(404, '资源不存在');
  if (String(resource.author) !== String(ctx.state.user.id)) {
    ctx.throw(403, '无权操作');
  }
  ctx.state.resource = resource;
  await next();
}

// 使用
router.put('/answers/:id', authMiddleware, checkOwner, async (ctx) => {
  const answer = ctx.state.resource;
  answer.content = ctx.request.body.content;
  await answer.save();
  ctx.body = answer;
});
```

### 2.6 分页与模糊搜索实现

```javascript
// 通用分页中间件
async function paginate(ctx, next) {
  const { page = 1, pageSize = 10 } = ctx.query;
  ctx.state.pagination = {
    page: Math.max(1, parseInt(page)),
    pageSize: Math.min(50, Math.max(1, parseInt(pageSize)))
  };
  await next();
}

// 话题模糊搜索
router.get('/topics', paginate, async (ctx) => {
  const { page, pageSize } = ctx.state.pagination;
  const { q } = ctx.query; // 搜索关键字

  const query = {};
  if (q) {
    // 方式1：正则模糊匹配
    query.name = new RegExp(escapeRegex(q), 'i');
    // 方式2：MongoDB 文本索引（需提前建索引）
    // query.$text = { $search: q };
    // 排序：{ score: { $meta: 'textScore' } }
  }

  const total = await Topic.countDocuments(query);
  const list = await Topic.find(query)
    .skip((page - 1) * pageSize)
    .limit(pageSize)
    .sort({ createdAt: -1 });

  ctx.body = { list, total, page, pageSize, totalPages: Math.ceil(total / pageSize) };
});

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
```

---

## 三、实战场景题

### 3.1 用户注册与密码加密

```javascript
const bcrypt = require('bcryptjs');

// 用户注册
router.post('/users', async (ctx) => {
  ctx.verifyParams({
    name: { type: 'string', required: true },
    password: { type: 'string', required: true, min: 6 },
    email: { type: 'string', required: true, format: 'email' }
  });

  const { name, password, email } = ctx.request.body;

  // 检查用户名唯一
  const existingUser = await User.findOne({ name });
  if (existingUser) ctx.throw(409, '用户名已被注册');

  // 密码加密存储
  const salt = await bcrypt.genSalt(10);
  const hashedPassword = await bcrypt.hash(password, salt);

  const user = await User.create({ name, password: hashedPassword, email });
  ctx.status = 201;
  ctx.body = { id: user._id, name: user.name, email: user.email };
});
```

### 3.2 关注/粉丝关系实现

```javascript
// 关注用户
router.put('/users/:id/follow', authMiddleware, async (ctx) => {
  const targetId = ctx.params.id;
  const currentUserId = ctx.state.user.id;

  if (targetId === currentUserId) ctx.throw(400, '不能关注自己');

  const targetUser = await User.findById(targetId);
  if (!targetUser) ctx.throw(404, '用户不存在');

  // 检查是否已关注
  const currentUser = await User.findById(currentUserId);
  if (currentUser.following.includes(targetId)) {
    ctx.throw(409, '已关注该用户');
  }

  // 双向更新
  await User.findByIdAndUpdate(currentUserId, { $push: { following: targetId } });
  await User.findByIdAndUpdate(targetId, { $push: { followers: currentUserId } });

  ctx.status = 204;
});

// 取消关注
router.delete('/users/:id/follow', authMiddleware, async (ctx) => {
  const targetId = ctx.params.id;
  const currentUserId = ctx.state.user.id;

  await User.findByIdAndUpdate(currentUserId, { $pull: { following: targetId } });
  await User.findByIdAndUpdate(targetId, { $pull: { followers: currentUserId } });

  ctx.status = 204;
});

// 获取粉丝列表
router.get('/users/:id/followers', async (ctx) => {
  const user = await User.findById(ctx.params.id)
    .populate('followers', 'name avatar headline');

  if (!user) ctx.throw(404, '用户不存在');
  ctx.body = user.followers;
});
```

### 3.3 嵌套资源路由（问题 -> 答案 -> 评论）

```javascript
// 路由注册：answers 是 questions 的子资源
const answerRouter = new Router({ prefix: '/questions/:questionId/answers' });

answerRouter.get('/', listAnswers);
answerRouter.post('/', authMiddleware, createAnswer);
answerRouter.get('/:id', getAnswerDetail);
answerRouter.put('/:id', authMiddleware, checkAnswerOwner, updateAnswer);
answerRouter.delete('/:id', authMiddleware, checkAnswerOwner, deleteAnswer);

// 创建答案（校验问题存在）
async function createAnswer(ctx) {
  ctx.verifyParams({
    content: { type: 'string', required: true }
  });

  const question = await Question.findById(ctx.params.questionId);
  if (!question) ctx.throw(404, '问题不存在');

  const answer = await Answer.create({
    content: ctx.request.body.content,
    questionId: ctx.params.questionId,
    answerer: ctx.state.user.id
  });

  ctx.status = 201;
  ctx.body = answer;
}

// 评论是 answers 的子资源
const commentRouter = new Router({
  prefix: '/questions/:questionId/answers/:answerId/comments'
});
```

### 3.4 点赞与收藏功能

```javascript
// 点赞答案
router.put('/answers/:id/vote', authMiddleware, async (ctx) => {
  const answer = await Answer.findById(ctx.params.id);
  if (!answer) ctx.throw(404, '答案不存在');

  const userId = ctx.state.user.id;
  const voted = answer.voters.includes(userId);

  if (voted) {
    // 取消赞
    await Answer.findByIdAndUpdate(ctx.params.id, {
      $pull: { voters: userId },
      $inc: { voteCount: -1 }
    });
  } else {
    // 点赞
    await Answer.findByIdAndUpdate(ctx.params.id, {
      $push: { voters: userId },
      $inc: { voteCount: 1 }
    });
  }

  ctx.status = 204;
});

// 收藏问题
router.put('/questions/:id/collect', authMiddleware, async (ctx) => {
  const userId = ctx.state.user.id;
  const questionId = ctx.params.id;
  const collected = ctx.state.user.collections?.includes(questionId);

  if (collected) {
    await User.findByIdAndUpdate(userId, { $pull: { collections: questionId } });
  } else {
    await User.findByIdAndUpdate(userId, { $push: { collections: questionId } });
  }

  ctx.status = 204;
});
```

### 3.5 koa-parameter 参数校验

```javascript
// 启用参数校验
app.use(koaParameter(app));

// 在控制器中使用
router.post('/questions', authMiddleware, async (ctx) => {
  ctx.verifyParams({
    title: { type: 'string', required: true, max: 200 },
    description: { type: 'string', required: false },
    topics: {
      type: 'array',
      itemType: 'string',
      required: false
    }
  });
  // ...处理逻辑
});

// 自定义错误消息
ctx.verifyParams({
  email: {
    type: 'string',
    required: true,
    format: 'email',
    message: '请输入有效的邮箱地址'
  },
  age: {
    type: 'number',
    required: true,
    min: 0,
    max: 150,
    message: '年龄必须在0-150之间'
  }
});
```

### 3.6 文件类型与尺寸校验

```javascript
// 自定义文件校验中间件
async function validateUpload(ctx, next) {
  const file = ctx.request.files?.file;
  if (!file) ctx.throw(400, '请选择文件');

  // 检查文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif'];
  if (!allowedTypes.includes(file.type)) {
    // 删除已上传的文件
    await fs.unlink(file.path);
    ctx.throw(400, '仅支持 JPG/PNG/GIF 格式');
  }

  // 检查文件大小
  if (file.size > 5 * 1024 * 1024) {
    await fs.unlink(file.path);
    ctx.throw(400, '文件不能超过 5MB');
  }

  await next();
}

router.post('/api/upload', authMiddleware, validateUpload, async (ctx) => {
  const file = ctx.request.files.file;
  ctx.body = { url: `/uploads/${path.basename(file.path)}` };
});
```

---

## 四、手写代码题

### 4.1 手写 Koa 中间件组合函数（koa-compose）

```javascript
function compose(middlewares) {
  return function (ctx) {
    let index = -1;

    function dispatch(i) {
      if (i <= index) {
        return Promise.reject(new Error('next() called multiple times'));
      }
      index = i;

      const fn = middlewares[i];
      if (!fn) return Promise.resolve();

      try {
        return Promise.resolve(fn(ctx, () => dispatch(i + 1)));
      } catch (err) {
        return Promise.reject(err);
      }
    }

    return dispatch(0);
  };
}

// 测试
const middleware = compose([
  async (ctx, next) => {
    ctx.log = [];
    ctx.log.push('1-start');
    await next();
    ctx.log.push('1-end');
  },
  async (ctx, next) => {
    ctx.log.push('2-start');
    await next();
    ctx.log.push('2-end');
  }
]);

const ctx = {};
middleware(ctx).then(() => console.log(ctx.log));
// ['1-start', '2-start', '2-end', '1-end']
```

### 4.2 手写简易 JWT 认证中间件

```javascript
const jwt = require('jsonwebtoken');

function koaJwt(options) {
  const { secret, getToken } = Object.assign({
    getToken: (ctx) => ctx.headers.authorization?.replace('Bearer ', '')
  }, options);

  return async (ctx, next) => {
    const token = getToken(ctx);

    if (!token) {
      ctx.throw(401, 'Authentication Error');
    }

    try {
      const decoded = jwt.verify(token, secret);
      ctx.state.user = decoded;
      await next();
    } catch (err) {
      if (err.name === 'TokenExpiredError') {
        ctx.throw(401, 'Token Expired');
      }
      ctx.throw(401, 'Invalid Token');
    }
  };
}

module.exports = koaJwt;
```

### 4.3 手写 RESTful API 资源控制器工厂

```javascript
function createResourceController(Model, options = {}) {
  const {
    listPopulate = '',
    detailPopulate = '',
    permissionCheck = null
  } = options;

  return {
    // 列表
    async list(ctx) {
      const { page = 1, pageSize = 10 } = ctx.query;
      const total = await Model.countDocuments();
      const list = await Model.find()
        .populate(listPopulate)
        .skip((page - 1) * pageSize)
        .limit(Number(pageSize))
        .sort({ createdAt: -1 });

      ctx.body = { list, total, page: Number(page), pageSize: Number(pageSize) };
    },

    // 详情
    async getById(ctx) {
      const resource = await Model.findById(ctx.params.id)
        .populate(detailPopulate);
      if (!resource) ctx.throw(404, '资源不存在');
      ctx.body = resource;
    },

    // 创建
    async create(ctx) {
      const resource = await Model.create({
        ...ctx.request.body,
        ...(options.creatorField ? { [options.creatorField]: ctx.state.user.id } : {})
      });
      ctx.status = 201;
      ctx.body = resource;
    },

    // 更新
    async update(ctx) {
      const resource = await Model.findByIdAndUpdate(
        ctx.params.id,
        ctx.request.body,
        { new: true, runValidators: true }
      );
      if (!resource) ctx.throw(404, '资源不存在');
      ctx.body = resource;
    },

    // 删除
    async delete(ctx) {
      const resource = await Model.findByIdAndDelete(ctx.params.id);
      if (!resource) ctx.throw(404, '资源不存在');
      ctx.status = 204;
    }
  };
}

// 使用
const topicController = createResourceController(Topic, {
  listPopulate: 'author',
  permissionCheck: 'admin'
});
router.get('/topics', topicController.list);
router.post('/topics', authMiddleware, topicController.create);
```

### 4.4 手写 MongoDB 事务处理（Mongoose Session）

```javascript
// MongoDB 4.0+ 支持多文档事务
async function transferMoney(fromId, toId, amount) {
  const session = await mongoose.startSession();
  session.startTransaction();

  try {
    // 扣减
    const fromUser = await User.findById(fromId).session(session);
    if (fromUser.balance < amount) {
      throw new Error('余额不足');
    }
    fromUser.balance -= amount;
    await fromUser.save({ session });

    // 增加
    const toUser = await User.findById(toId).session(session);
    toUser.balance += amount;
    await toUser.save({ session });

    // 提交事务
    await session.commitTransaction();
    console.log('转账成功');
  } catch (err) {
    // 回滚
    await session.abortTransaction();
    console.error('转账失败:', err.message);
    throw err;
  } finally {
    session.endSession();
  }
}
```

### 4.5 手写简易 Mongoose Schema 验证

```javascript
// 自定义邮箱格式校验
function validateEmail(email) {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(email);
}

const userSchema = new mongoose.Schema({
  email: {
    type: String,
    required: [true, '邮箱是必填项'],
    validate: {
      validator: validateEmail,
      message: '邮箱格式不正确'
    },
    unique: true
  },
  phone: {
    type: String,
    match: [/^1[3-9]\d{9}$/, '手机号格式不正确']
  },
  status: {
    type: String,
    enum: ['active', 'inactive', 'banned'],
    default: 'active'
  },
  age: {
    type: Number,
    min: [0, '年龄不能为负'],
    max: [150, '年龄超出范围']
  }
});

// 自定义预保存钩子
userSchema.pre('save', function(next) {
  if (this.isModified('password')) {
    // 密码加密已在外部处理
    this.updatedAt = new Date();
  }
  next();
});
```

### 4.6 手写分页中间件

```javascript
function paginationMiddleware(options = {}) {
  const { maxSize = 50, defaultSize = 10 } = options;

  return async (ctx, next) => {
    const page = Math.max(1, parseInt(ctx.query.page) || 1);
    const pageSize = Math.min(maxSize, Math.max(1, parseInt(ctx.query.pageSize) || defaultSize));

    ctx.state.pagination = {
      page,
      pageSize,
      skip: (page - 1) * pageSize
    };

    await next();
  };
}

// 使用
router.get('/answers', paginationMiddleware({ maxSize: 30 }), async (ctx) => {
  const { page, pageSize, skip } = ctx.state.pagination;
  const query = Answer.find().skip(skip).limit(pageSize).sort({ createdAt: -1 });
  const [list, total] = await Promise.all([
    query,
    Answer.countDocuments()
  ]);

  ctx.body = {
    list,
    pagination: {
      page,
      pageSize,
      total,
      totalPages: Math.ceil(total / pageSize)
    }
  };
});
```

---

## 五、系统设计题

### 5.1 仿知乎问答系统数据库设计

**核心集合与 Schema**：

```
Users:    _id, name, password, email, avatar, headline, following[], followers[], collections[]
Questions: _id, title, description, topics[], author, createdAt, updatedAt
Answers:   _id, content, questionId, answerer, voteCount, voters[], createdAt
Comments:  _id, content, answerId, commentator, parentId, rootId, replyTo, createdAt
Topics:    _id, name, description, questions[]
```

**关键索引设计**：

```javascript
// 答案按问题查询
answerSchema.index({ questionId: 1, createdAt: -1 });

// 评论按答案查询
commentSchema.index({ answerId: 1, createdAt: 1 });

// 话题全文搜索
topicSchema.index({ name: 'text', description: 'text' });

// 用户登录
userSchema.index({ name: 1 }, { unique: true });

// 时间排序
questionSchema.index({ createdAt: -1 });
```

### 5.2 RESTful API 路由架构

```
/api
├── /users                    # 用户模块
│   ├── POST /login          # 登录
│   ├── POST /register       # 注册
│   ├── GET /:id             # 获取用户
│   ├── PUT /:id             # 更新用户
│   ├── GET /:id/following   # 关注列表
│   ├── GET /:id/followers   # 粉丝列表
│   ├── PUT /:id/follow      # 关注
│   └── DELETE /:id/follow   # 取消关注
├── /questions                # 问题模块
│   ├── GET /                # 问题列表（分页+搜索）
│   ├── POST /               # 提问
│   ├── GET /:id             # 问题详情
│   ├── PUT /:id             # 更新问题
│   └── DELETE /:id          # 删除问题
├── /questions/:id/answers    # 答案模块（嵌套资源）
│   ├── GET /                # 答案列表
│   ├── POST /               # 回答
│   ├── GET /:answerId       # 答案详情
│   ├── PUT /:answerId       # 更新答案
│   └── DELETE /:answerId    # 删除答案
├── /answers/:id/comment      # 评论模块（三级嵌套）
│   ├── GET /                # 评论列表
│   ├── POST /               # 评论
│   └── DELETE /:commentId   # 删除评论
├── /topics                   # 话题模块
│   ├── GET /                # 话题列表（分页+模糊搜索）
│   ├── GET /:id             # 话题详情
│   └── POST /               # 创建话题
└── /upload                   # 文件上传
    └── POST /               # 上传文件
```

### 5.3 性能优化与部署方案

| 优化方向 | 方案 | 具体措施 |
|---------|------|---------|
| 数据库 | 索引优化 | 常用查询字段建立索引，用 `explain()` 分析查询计划 |
| 数据库 | 连接池 | Mongoose 默认连接池 5，按需调整 |
| 缓存 | Redis | 热点话题缓存，用户 session 缓存 |
| 网络 | gzip 压缩 | koa-compress 中间件 |
| 静态资源 | CDN | 用户头像、图片文件走 CDN |
| 进程 | PM2 集群 | 多进程充分利用多核 CPU |
| 反向代理 | Nginx | 负载均衡、静态文件直接服务 |
| 监控 | 日志+告警 | PM2 日志、Sentry 错误监控 |

### 5.4 生产环境部署全流程

```bash
# 1. 服务器环境配置
apt install nginx nodejs npm
npm install -g pm2

# 2. 克隆代码
git clone https://github.com/zhihu-api.git
cd zhihu-api
npm install --production

# 3. 环境变量配置
cat > .env << EOF
NODE_ENV=production
PORT=3000
JWT_SECRET=your_jwt_secret
MONGODB_URL=mongodb://localhost:27017/zhihu
EOF

# 4. 构建（如有需要）
npm run build

# 5. 启动服务
pm2 start ecosystem.config.js

# 6. 配置 Nginx 反向代理
# 参考上一节的 Nginx 配置

# 7. HTTPS 配置
certbot --nginx -d api.zhihu.com

# 8. 验证
curl https://api.zhihu.com/api/v1/topics
```

---

## 六、常见坑点与最佳实践

| 坑点 | 说明 | 最佳实践 |
|------|------|---------|
| `next()` 不加 await | 中间件执行顺序混乱 | 始终 `await next()` |
| `next()` 多次调用 | koa-compose 会抛 Error | 确保只调用一次 |
| Mongoose 未连接 | 操作数据库时未连接成功 | 使用 `mongoose.connect()` 的 Promise |
| populate 过多字段 | 造成 N+1 查询 | 限制 populate 字段，使用 select |
| 文件上传未限制大小 | 耗尽服务器内存 | koa-body 设置 maxFileSize |
| JWT 存 localStorage | 易被 XSS 窃取 | 使用 httpOnly Cookie |
| 密码明文存储 | 数据库泄露风险 | bcrypt 加盐哈希存储 |
| 请求体未校验 | 非法数据入库 | 使用 koa-parameter 严格校验 |
| 未处理 404 路由 | 请求不存在的路径返回 404 | 在路由后添加 404 处理 |
| Mongoose lean() 未用 | 返回 Mongoose 文档(重) | 只读查询使用 `.lean()` |
| 未捕获 rejected promise | 进程可能崩溃 | 全局 `process.on('unhandledRejection')` |
| 部署依赖 devDependencies | 生产环境打包过大 | `npm install --production` |
| 忽略 MongoDB 索引 | 慢查询导致性能问题 | 定期检查慢查询日志 |

---

## 七、面试回答模板

### 7.1 "Koa 的洋葱模型是什么？"

```text
Koa 的洋葱模型是一种中间件执行机制。当请求进入时，按注册顺序依次执行中间件，每个中间件通过 await next() 将控制权传递给下一个中间件；响应返回时，按相反顺序执行 next() 之后的代码。

这种模型最核心的优势是：我可以在请求前后都插入逻辑。比如日志中间件，进入时记录时间戳，返回时计算耗时写入日志，所有逻辑写在一个中间件里，非常优雅。

实现原理是 koa-compose 函数，它将中间件数组组合成一个嵌套的 Promise 链，通过递归 dispatch 函数调度执行。
```

### 7.2 "RESTful API 的设计要点是什么？"

```text
1. 使用名词复数作为资源路径（/users、/articles）
2. 用 HTTP 方法表达操作（GET 查、POST 增、PUT 改、DELETE 删）
3. 资源间关系用嵌套 URI（/questions/:id/answers）
4. 使用查询参数过滤排序（?page=1&pageSize=10&sort=-createdAt）
5. 返回适当的 HTTP 状态码（201 Created、204 No Content、400 Bad Request）
6. 无状态，每个请求包含足够信息
7. 版本控制（/api/v1/）
```

### 7.3 "MongoDB 和 MySQL 怎么选？"

```text
MongoDB 适合：数据结构灵活（文档模型）、快速迭代开发、存储 JSON-like 数据、需要横向扩展的场景。比如社区问答系统，用户 profile、答案嵌套评论这种结构很适合用文档存储。

MySQL 适合：数据结构固定、需要复杂多表关联查询、事务一致性要求高、报表统计场景。

仿知乎项目中用 MongoDB 很合适，因为：
1. 用户关注关系的数组字段（following/followers）天然适合文档模型
2. 问题-答案-评论的嵌套结构用 populate 查询很方便
3. 开发迭代速度快，无需频繁改表结构
4. 不需要复杂的跨文档事务（大部分操作是单文档）
```

### 7.4 "如何处理 MongoDB 的多表关联？"

```text
在 MongoDB 中，我常用三种方式处理关联：

1. populate：最简单，适合一对一/一对多简单关联。比如 User 的 following 字段用 ref 引用，通过 populate 填充。缺点是 N+1 查询。

2. 聚合管道（aggregate + lookup）：适合复杂关联和聚合统计。比如查询问题时同时统计回答数、浏览次数。性能比 populate 好，但代码更复杂。

3. 反范式化（冗余存储）：对读多写少的场景，直接嵌入数据。比如在回答文档中存储用户名的副本，避免每次查用户。缺点是数据冗余，更新需同步。

选择策略：读多写少用反范式，关联简单用 populate，统计复杂用 aggregate。
```

### 7.5 "仿知乎项目的核心难点是什么？"

```text
核心难点是设计好资源间的关联关系：

1. 嵌套资源路由：问题 -> 答案 -> 评论（三级嵌套），Koa Router 怎么组织层次结构才能避免路由冲突

2. 关注关系的双向同步：A 关注 B 需要同时更新 A.following 和 B.followers，要考虑幂等性和并发问题

3. 三级评论的树形结构：每条评论可能有父评论、根评论、回复对象，查询时要构建树

4. 分页 + 模糊搜索：话题和问题的搜索需要同时支持分页和文本搜索，索引设计很重要

5. 认证与权限：JWT 认证 + 资源拥有者校验，每个受保护的操作都需要验证

解决方案是模块化设计，每个资源独立路由控制器，使用中间件复用认证和权限逻辑，Mongoose 的 populate 和 aggregate 处理关联查询。
```

---

## 八、快速查漏补缺 Checklist

- [ ] 理解 Koa 洋葱模型与 koa-compose 原理
- [ ] 能手动实现 compose 函数
- [ ] 掌握 koa-router 路由组织与参数传递
- [ ] 理解 ctx 对象的完整生命周期
- [ ] 掌握 Mongoose Schema 设计与索引优化
- [ ] 理解 populate 与 aggregate 的区别与适用场景
- [ ] 掌握嵌套资源路由设计模式
- [ ] 理解关注/粉丝关系的双向更新机制
- [ ] 掌握三级评论的树形结构设计
- [ ] 理解 JWT 认证与 koa-jwt 的使用
- [ ] 掌握 koa-body 文件上传配置
- [ ] 理解 koa-parameter 参数校验
- [ ] 掌握全局错误处理配置
- [ ] 理解 RESTful API 六大约束
- [ ] 掌握 PM2 + Nginx 部署流程
- [ ] 理解 MongoDB 文档关系设计原则
- [ ] 掌握分页 + 模糊搜索实现
- [ ] 理解 $push/$pull/$inc 等原子操作
- [ ] 掌握 MVC 分层架构设计
- [ ] 了解生产环境性能优化方案
