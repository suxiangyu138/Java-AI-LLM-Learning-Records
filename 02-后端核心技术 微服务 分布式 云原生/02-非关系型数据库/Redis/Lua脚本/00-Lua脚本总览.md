# Redis Lua 脚本总览
> Redis 服务端编程体系：Lua 语言基础、EVAL/SCRIPT 命令族、原子执行语义、Redis 7+ Function 机制、经典脚本实战与 Spring 集成——"把多步原子操作交给 Redis"的完整答案

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [Lua 脚本定位](#4-lua-脚本定位)
5. [核心概念速查](#5-核心概念速查)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
Redis Lua 脚本体系（8 篇，Redis 8.10 / Lua 5.1 / Functions 推荐基准，2026-08）
│
├─ 认知层 ─────────────────────────────
│   ├─ 00 总览（本文）
│   ├─ 01 Lua 语言基础（语法速成，面向 Redis 脚本）
│   └─ 02 脚本机制与命令族（EVAL/EVALSHA/SCRIPT/KEYS/ARGV）
│
├─ 原理层 ─────────────────────────────
│   ├─ 03 原子性与执行语义（单线程/错误/沙箱/2026 加固）
│   └─ 04 管道 vs 事务 vs 脚本 vs 函数（四兄弟选型）
│
├─ 机制层 ─────────────────────────────
│   ├─ 05 Redis Function 机制（7.0+ 官方推荐方案）
│   └─ 06 经典脚本实战（分布式锁/限流/防重/扣库存）
│
└─ 实战层 ─────────────────────────────
│   ├─ 07 Spring Data Redis 集成（DefaultRedisScript/序列化）
│   └─ 08 性能调试与生产实践（避坑/面试）
```

> 🎯 定位一句话：Redis Lua 脚本是 **"把一段业务逻辑送进 Redis 进程内、单线程原子执行"** 的编程接口——它让"检查-修改"两步操作不可分割，是分布式锁、限流、防重、扣库存的底层实现手段。

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-Lua脚本总览.md) | 导图、定位、速查 | 所有人 |
| 01 | [Lua 语言基础](01-Lua语言基础.md) | 类型/table/函数/控制流，Redis 视角 | 入门必读 |
| 02 | [脚本机制与命令族](02-脚本机制与命令族.md) | EVAL/EVALSHA/SCRIPT、KEYS/ARGV、缓存 | 入门必读 |
| 03 | [原子性与执行语义](03-原子性与执行语义.md) | 单线程、错误处理、沙箱与限制 | 重点 |
| 04 | [管道 vs 事务 vs 脚本 vs 函数](04-管道vs事务vs脚本vs函数.md) | 四机制对比与选型 | 重点 |
| 05 | [Redis Function 机制](05-Redis-Function机制.md) | 库/FCALL/持久化/版本化/迁移 | 重点（7.0+） |
| 06 | [经典脚本实战](06-经典脚本实战.md) | 分布式锁/限流/防重/扣库存 | 重点 |
| 07 | [Spring Data Redis 集成](07-Spring-Data-Redis集成.md) | DefaultRedisScript/序列化/NOSCRIPT | 进阶 |
| 08 | [性能调试与生产实践](08-性能调试与生产实践.md) | 性能/调试/避坑/面试 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 快速入门（1 天） | Redis 使用者 | 00 → 01 → 02 → 06 |
| 应用进阶（2 天） | 写生产脚本 | 03 → 04 → 05 → 07 |
| 面试冲刺（1 天） | 备战后端面试 | 00 → 02 → 03 → 06 → 08 |

> 💡 前置与分工：本体系是 Redis 主目录（01-08 篇）的**脚本专项深挖**——其中 [08-Spring Boot集成Redis与Lua](../../Redis/08-Spring%20Boot集成Redis与Lua.md) 已有紧凑的 Lua 章节（6.1-6.6），本体系全面展开且不重复其 Spring 配置基础。

## 4. Lua 脚本定位

```text
为什么需要脚本：
  Redis 单条命令是原子的，但"多条命令的组合"不是——
  例：扣库存 = GET 检查 → DECR 扣减（两条命令，中间可被其他请求插入）

脚本方案：把多步逻辑写进 Lua，整个脚本在 Redis 进程内单线程执行：
  ┌────────────────────────────────────────┐
  │ Redis 进程（单线程执行区）               │
  │  EVAL "if ... then return redis.call(...) end" 0 key arg │
  │  整个脚本执行期间，其他命令全部排队等待    │
  └────────────────────────────────────────┘
  效果：与单条命令相同的原子性保证
```

| 脚本能做的 | 说明 |
|-----------|------|
| 原子多步操作 | 检查-修改不可分割（锁/限流/防重） |
| 减少网络往返 | 3 次命令 → 1 次 EVAL |
| 服务端逻辑复用 | 同一逻辑多端共享 |
| 复杂条件控制 | if/循环/table（命令做不到） |
| 调用任意 Redis 命令 | redis.call/pcall |

> 🎯 一句话理解：**Lua 脚本 = Redis 的"存储过程"**（数据库领域的类比）——把多步逻辑送进数据库进程执行，一次往返、全程原子。

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Lua | Redis 内嵌的脚本语言（**5.1 版本**，2026 仍如此） |
| EVAL | 直接执行脚本（每次都传脚本内容） |
| EVALSHA | 按 SHA1 摘要执行已缓存脚本 |
| SCRIPT LOAD | 缓存脚本返回 SHA1（推荐 EVALSHA 搭配） |
| KEYS[i] | 脚本参数里的键（**必须用 KEYS 传键，不能拼进脚本**） |
| ARGV[i] | 脚本参数里的值 |
| redis.call() | 调用 Redis 命令（出错即抛） |
| redis.pcall() | 调用 Redis 命令（出错返回错误表） |
| 原子性 | 脚本整个执行期间 Redis 单线程，天然原子 |
| 脚本缓存 | 服务器端缓存（LRU 500 条，重启丢失） |
| 沙箱 | 只读库 + 无网络 + 无文件系统（可用库受限） |
| Function | Redis 7.0+ 的**推荐方案**（持久化/命名/版本化） |
| FCALL | 调用 Function |
| 2026 加固 | 禁止全局函数、移除 lua print、内存限制（7.0+ 沙箱变化） |

## 6. 参考来源

- [Redis 官方：Programmability 文档](https://redis.io/docs/latest/develop/programmability/)
- [Redis Functions 介绍（官方）](https://redis.io/docs/latest/develop/programmability/functions-intro/)
- [Redis 官方：Eval 命令文档](https://redis.io/docs/latest/commands/eval/)
- [Redis 源码 deps/lua（Lua 5.1 确认）](https://github.com/redis/redis/tree/unstable/deps/lua)
- [Functions vs EVAL 对比（2026-03）](https://oneuptime.com/blog/post/2026-03-31-redis-functions-differ-lua-eval-scripts/view)
- [Lua 脚本迁移到 Functions 指南（2026-03）](https://OneUptime.com/blog/post/2026-03-31-redis-migrate-from-lua-scripts-to-redis-functions/view)
- [腾讯云：Redis 大版本命令差异（沙箱加固）](https://cloud.tencent.cn/document/product/239/122188)

---

**下一模块**：[01-Lua语言基础](01-Lua语言基础.md)
