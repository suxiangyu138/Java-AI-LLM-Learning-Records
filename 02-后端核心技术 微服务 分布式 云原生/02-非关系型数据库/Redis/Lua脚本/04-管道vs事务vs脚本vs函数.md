# 管道 vs 事务 vs 脚本 vs 函数
> Redis 四个"批量/逻辑"机制的完整对比：Pipeline、MULTI/EXEC、Lua 脚本（EVAL）、Redis Function——什么场景用哪个

## 📚 目录
1. [四机制总览](#1-四机制总览)
2. [Pipeline：省网络，不原子](#2-pipeline省网络不原子)
3. [MULTI/EXEC：原子，无逻辑](#3-multiexec原子无逻辑)
4. [Lua 脚本：原子 + 逻辑](#4-lua-脚本原子--逻辑)
5. [Function：脚本的"产品化"](#5-function脚本的产品化)
6. [选型决策树](#6-选型决策树)
7. [组合使用的正确姿势](#7-组合使用的正确姿势)

## 1. 四机制总览

| 机制 | 原子性 | 逻辑能力 | 持久性 | 网络往返 | 引入版本 |
|------|:---:|:---:|:---:|:---:|:---:|
| **Pipeline** | ❌ | ❌ | - | 1 次 | 客户端能力 |
| **MULTI/EXEC** | ✅ | ❌ | 写命令持久化 | 2 次（排队+执行） | 1.2 |
| **Lua 脚本（EVAL）** | ✅ | ✅（完整语言） | 写效果复制 | 1 次 | 2.6 |
| **Function** | ✅ | ✅ | **脚本库本身持久化** | 1 次 | 7.0 |

> 🎯 记忆框架：**Pipeline 解决"网络往返"，事务/脚本解决"原子性"，脚本/Function 加"逻辑能力"**——四者不是替代关系，是不同维度的工具。

## 2. Pipeline：省网络，不原子

```text
Pipeline（管道）：一次网络往返发送 N 条命令
  客户端 ──[CMD1 CMD2 CMD3...]──> 服务器 ──[R1 R2 R3...]──> 客户端

  ✅ 收益：N 条命令从 N 次 RTT 变成 1 次 RTT（网络 IO 大减）
  ❌ 局限：命令之间可能被其他客户端的命令插入（非原子）
```

| 场景 | 说明 |
|------|------|
| 批量写入 | 大量 SET/INCR（如缓存预热、批量计数） |
| 批量读取 | 大量 GET（省 N-1 次 RTT） |
| 大数据迁移 | SCAN + 批量操作 |

```java
// Spring Data Redis（pipeline 模式）
redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (String key : keys) {
        connection.set(key.getBytes(), value.getBytes());
    }
    return null;
});
```

> ⚠️ 常见误解：**Pipeline ≠ 原子**——管道里的命令在服务器端还是逐条执行，中间可插入其他请求。要原子性必须事务/脚本。

## 3. MULTI/EXEC：原子，无逻辑

```bash
MULTI
SET stock:1001 50
DECR stock:1001
EXEC        # 队列中的命令整体执行（原子）

# 局限一：排队时无法判断（不能"检查再决定"）
# 局限二：执行期错误不回滚（2.6.5+ 默认继续执行其余命令）
```

| 维度 | MULTI/EXEC | Lua 脚本 |
|------|:---:|:---:|
| 条件判断 | ❌ | ✅ |
| 循环 | ❌ | ✅ |
| 返回值处理 | ❌（只拿数组） | ✅ |
| WATCH 配套 | 需要（乐观锁重试） | 不需要（无竞态） |
| 适用 | 简单批量原子 | 复杂逻辑 |

> 🎯 结论：**有逻辑需求就放弃 MULTI 转脚本**——MULTI+WATCH+重试的三件套复杂度远高于一个脚本。

## 4. Lua 脚本：原子 + 逻辑

```text
Lua 脚本 = MULTI 的原子性 + 编程能力（if/循环/函数/cjson）
  适用：分布式锁、限流、防重、扣库存、原子条件更新

代价：
  · 脚本需客户端维护（加载/缓存/NOSCRIPT）
  · 慢脚本阻塞全局（见 03 §7）
  · 无回滚（见 03 §4）
```

```lua
-- 一个"脚本才能做"的例子：条件扣减（MULTI 做不到）
-- 库存充足才扣，不足返回 -1
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if stock < tonumber(ARGV[1]) then
    return -1
end
return redis.call('DECRBY', KEYS[1], ARGV[1])
```

## 5. Function：脚本的"产品化"

```text
Function（Redis 7.0+）＝ 把 Lua 脚本变成"服务器管理的软件组件"：
  EVAL 时代：脚本在客户端，服务器只管执行（缓存易失、无法管理）
  Function：脚本库存在服务器（随 RDB/AOF 持久化、自动复制）

  ┌─ EVAL ──┐      ┌─ Function ──┐
  │ 客户端持有 │      │ 服务器持有    │
  │ 缓存易失   │      │ 持久化+复制   │
  │ SHA1 命名  │      │ 人类可读命名  │
  │ 无版本     │      │ 库版本化     │
  └──────────┘      └─────────────┘
```

| 能力 | EVAL | Function |
|------|:---:|:---:|
| 持久化 | ❌ 重启丢 | ✅ RDB/AOF |
| 复制 | ❌ 需各从库加载 | ✅ 自动 |
| 命名 | SHA1 摘要 | 人类可读 |
| 版本化 | ❌ | ✅ 库级 |
| 代码复用 | ❌ 脚本间不能调用 | ✅ 同库函数互调 |
| 更新 | 改代码重发 | `FUNCTION LOAD REPLACE` 热更新 |
| 2026 官方建议 | 一次性/调试 | **生产首选** |

> 🎯 2026 官方立场（检索核实）：**新生产代码用 Function；EVAL/EVALSHA 只用于一次性/探索/调试**——详见 [05-Redis Function机制](05-Redis-Function机制.md)。

## 6. 选型决策树

```text
需求四问：

① 只是省网络往返？          → Pipeline
   （无原子性要求，批量读写）

② 需要原子但不需逻辑？      → MULTI/EXEC
   （简单批量原子操作）

③ 需要原子 + 条件/循环？    → Lua 脚本（EVAL/EVALSHA）
   （锁/限流/防重/扣库存）

④ 需要长期生产维护/多端共享/热更新？ → Function（7.0+）
   （推荐的生产形态，②③的场景都覆盖）
```

| 场景 | 推荐 |
|------|------|
| 缓存预热/批量写入 | Pipeline |
| 批量简单原子（如多 INCR） | MULTI/EXEC |
| 分布式锁/限流/防重/扣库存 | **Lua 脚本或 Function** |
| 生产持久化服务端逻辑 | **Function** |
| 一次性的数据修复 | EVAL（临时） |

## 7. 组合使用的正确姿势

```text
Pipeline 与脚本不冲突——可以组合：
  Pipeline { EVALSHA 1; EVALSHA 2; ... }   # 一次往返执行多个脚本
  （注意：管道内脚本各自原子，脚本间不原子——需要整体原子就合并成一个脚本）

生产组合套路：
  · 单机高频：Pipeline + EVALSHA（脚本缓存复用）
  · 逻辑原子：单个脚本内完成（别拆多脚本）
  · 生产维护：Function 库 + FCALL
```

> ⚠️ 反模式提醒：**"一个需求拆 3 个脚本再用管道串"是错误设计**——脚本间原子性丢失，且多脚本管理混乱。原子需求 = 一个脚本完成；无原子需求才考虑管道。

---

**下一模块**：[05-Redis-Function机制](05-Redis-Function机制.md) / **返回总览**：[00-Lua脚本总览](00-Lua脚本总览.md)
