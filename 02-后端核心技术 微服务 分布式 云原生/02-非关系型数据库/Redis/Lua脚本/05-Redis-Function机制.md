# Redis Function 机制
> Redis 7.0+ 官方推荐的编程形态：命名库、持久化与复制、版本化热更新、FCALL 调用——以及从 EVAL 迁移的完整路线

## 📚 目录
1. [为什么要有 Function](#1-为什么要有-function)
2. [库（Library）：Function 的载体](#2-库libraryfunction-的载体)
3. [注册与调用：FUNCTION/FCALL 命令族](#3-注册与调用functionfcall-命令族)
4. [Function vs EVAL 全面对比](#4-function-vs-eval-全面对比)
5. [版本化与热更新](#5-版本化与热更新)
6. [只读函数与 flags](#6-只读函数与-flags)
7. [从 EVAL 迁移到 Function](#7-从-eval-迁移到-function)
8. [Function 的局限与适用边界](#8-function-的局限与适用边界)

## 1. 为什么要有 Function

```text
EVAL 时代的问题清单（官方文档明确列出）：
  · 每个客户端都要维护脚本副本（版本不同步）
  · 脚本缓存易失（重启/FLUSH/故障转移 → NOSCRIPT）
  · SHA1 摘要无法阅读（MONITOR/排障看不懂）
  · 无法版本管理（改了脚本只能全客户端重新发）
  · 脚本之间不能互相调用（无代码复用）
  · 鼓励"客户端拼脚本"反模式

Function 的答案：把脚本变成"数据集的一部分"
  · 存在服务器（RDB/AOF 持久化、自动复制到从库）
  · 人类可读命名 + 版本化库
  · 一次注册，所有客户端按名调用
```

> 🎯 定位一句话：**Function 是 Lua 脚本的"产品化"**——从"客户端临时递给服务器的代码"升级为"服务器管理的、持久化的、可版本化的服务端函数库"。

## 2. 库（Library）：Function 的载体

```lua
-- 一个 Function 库文件（#! 声明 + 注册函数）
#!lua name=mylib                       -- 库名（必填）
#!lua version=1                        -- 库版本（可选，语义化）

-- 库内可定义公共辅助函数（库内复用）
local function buildKey(userId)
    return "user:" .. userId
end

-- 注册函数：redis.register_function(name, callback, flags)
redis.register_function('getUserScore', function(keys, args)
    local key = buildKey(args[1])      -- 复用辅助函数 ✅（EVAL 做不到）
    return redis.call('HGET', key, 'score')
end, { 'no-writes' })                  -- flags：只读

redis.register_function('incrUserScore', function(keys, args)
    local key = buildKey(args[1])
    return redis.call('HINCRBY', key, 'score', args[2])
end)
```

| 库要素 | 说明 |
|--------|------|
| `#!lua name=...` | 库名（唯一标识，必填） |
| `#!lua version=...` | 库版本（`FUNCTION LOAD REPLACE` 时用） |
| `redis.register_function(name, cb, flags)` | 注册函数（可注册多个） |
| `function(keys, args)` | 回调签名：**keys 和 args 是数组参数**（不再是全局 KEYS/ARGV） |
| 库内复用 | 库内函数/变量可互相调用（EVAL 无此能力） |

> ⚠️ 签名差异：**Function 回调是 `function(keys, args)` 形式**（参数数组），不再是 EVAL 的全局 `KEYS[i]`/`ARGV[i]`——但依然**从 1 开始索引**：`keys[1]`、`args[1]`。

## 3. 注册与调用：FUNCTION/FCALL 命令族

```bash
# ① 加载库（redis-cli -x 从文件加载）
redis-cli -x FUNCTION LOAD < mylib.lua
# 返回库名：mylib

# ② 调用函数
FCALL getUserScore 0 42
#              ↑ numkeys（Function 通常把键作为 args 传，numkeys=0）
FCALL incrUserScore 1 "user:42" 10      # keys 走 keys 数组

# ③ 只读调用（可上从库执行）
FCALL_RO getUserScore 0 42

# ④ 管理命令
FUNCTION LIST        # 列出已加载库
FUNCTION DUMP        # 备份全部库（二进制）
FUNCTION RESTORE     # 恢复备份
FUNCTION FLUSH       # 清空全部库
FUNCTION STATS       # 运行状态
FUNCTION KILL        # 终止运行中的函数
```

| 命令 | 用途 |
|------|------|
| `FUNCTION LOAD` | 加载库（`REPLACE` 参数可覆盖同库名） |
| `FCALL name numkeys keys args` | 调用函数 |
| `FCALL_RO` | 只读调用（从库可执行） |
| `FUNCTION LIST/DUMP/RESTORE` | 管理/备份/恢复 |
| `FUNCTION FLUSH` | 清空（同步/异步模式） |
| `FUNCTION KILL` | 终止卡住函数（对应 SCRIPT KILL） |

> 💡 运维视角：**FUNCTION DUMP/RESTORE 是脚本的"备份"**——迁移集群/灾备时脚本库随数据一起走，不再有"新集群没脚本"的 NOSCRIPT 灾难。

## 4. Function vs EVAL 全面对比

| 维度 | EVAL / EVALSHA | Function |
|------|:---:|:---:|
| 代码存放 | 客户端（每次传/缓存） | **服务器（持久化）** |
| 重启后 | 缓存丢（NOSCRIPT） | **存活（RDB/AOF）** |
| 从库 | 需各从库加载 | **自动复制** |
| 命名 | SHA1 | **人类可读** |
| 版本管理 | ❌ | ✅（库级 version） |
| 热更新 | 客户端重发 | `FUNCTION LOAD REPLACE` |
| 代码复用 | ❌ 脚本间不可调用 | ✅ 库内函数互调 |
| 备份迁移 | 手工脚本管理 | ✅ FUNCTION DUMP |
| 调试可读性 | SHA1 难读 | 名称即文档 |
| 2026 定位 | 一次性/探索/调试 | **生产首选** |

> 🎯 2026 检索结论（OneUptime/官方文档一致）：**新项目在 Redis 7.0+ 上直接用 Function**；EVAL 保留给"临时验证、一次性修复、Redis < 7 环境"。

## 5. 版本化与热更新

```lua
#!lua name=mylib
#!lua version=2                    -- 版本号语义化
local function buildKey(userId) ... end

redis.register_function('incrUserScore', function(keys, args)
    -- v2 新逻辑：加防超限判断
    local newScore = redis.call('HINCRBY', 'user:' .. args[1], 'score', args[2])
    if newScore > 10000 then
        redis.call('HSET', 'user:' .. args[1], 'score', 10000)   -- 封顶
        return 10000
    end
    return newScore
end)
```

```bash
# 热更新：不重启、不影响调用方（名称不变，逻辑升级）
redis-cli -x FUNCTION LOAD REPLACE < mylib-v2.lua
# 已有客户端继续 FCALL incrUserScore —— 自动走新逻辑
```

| 版本化能力 | 说明 |
|-----------|------|
| 库版本 | 元数据（`FUNCTION LIST` 可见） |
| REPLACE | 同库名覆盖（平滑升级） |
| 灰度 | 发布前可先在预发环境 FUNCTION LOAD 验证 |
| 回滚 | 重新加载旧版本库即可 |
| CI/CD | 脚本库文件入库 → 发布流水线 FUNCTION LOAD |

> 🎯 这是 Function 相对 EVAL 的**杀手级价值**：业务脚本升级 = 一条 FUNCTION LOAD 命令，**无需改任何客户端代码、无需等所有服务重启**——EVAL 时代改脚本意味着全链路发版。

## 6. 只读函数与 flags

```lua
-- 注册时声明 flags：
redis.register_function('readOnlyFn', function(keys, args)
    return redis.call('GET', keys[1])
end, { 'no-writes' })       -- 声明只读

-- 效果：
-- ① 可被 FCALL_RO 调用（从库执行，读写分离）
-- ② 主库调用时也走只读快照（一致性）
-- ③ 忘写 no-writes 但函数只读 → FCALL_RO 也能调（运行时检查）
```

| flag | 含义 |
|------|------|
| `no-writes` | 函数只读（可在从库执行、可 FCALL_RO） |
| `allow-oom` | 允许在内存超限（OOM）时仍执行（默认 OOM 时写函数拒绝） |
| 缺省 | 可读写（默认） |

> 💡 与 EVAL_RO 对应：**FCALL_RO 是 Function 的只读入口**（7.0+）；配合 `no-writes` 标记，只读函数能安全地在从库/副本执行——读写分离架构下的推送查询场景友好。

## 7. 从 EVAL 迁移到 Function

```text
迁移四步（2026 官方推荐路径）：

① 包装：把 EVAL 脚本包进库文件
   #!lua name=mylib
   redis.register_function('myLogic', function(keys, args)
       ...原脚本逻辑，KEYS[i]→keys[i]、ARGV[i]→args[i]...
   end)

② 加载：redis-cli -x FUNCTION LOAD REPLACE < mylib.lua

③ 替换调用：客户端 EVAL/EVALSHA → FCALL
   EVALSHA sha 1 key arg   →   FCALL myLogic 1 key arg

④ 双跑过渡：新旧并行一段时间 → 验证一致 → 下线 EVAL
```

| 迁移注意 | 说明 |
|---------|------|
| 索引不变 | keys[1]/args[1] 仍从 1 开始（仅去掉了全局 KEYS） |
| 沙箱一致 | 与 EVAL 相同的沙箱规则（local 强制等） |
| 库内重构 | 相同逻辑可抽公共函数（顺便收益） |
| 只读函数 | 加 `no-writes` flag + 客户端切 FCALL_RO |
| 版本规划 | 库 version 从 1 开始语义化 |

## 8. Function 的局限与适用边界

| 局限 | 说明 |
|------|------|
| 版本要求 | Redis 7.0+（老环境只能用 EVAL） |
| 生态 | 客户端库支持参差（部分库对 Function 封装晚，可用原生命令） |
| 排查 | 逻辑藏在服务器 → 调试依赖 FUNCTION LIST/MONITOR（见 08） |
| 滥用风险 | 服务器存逻辑 = 发布入口变化（必须纳入 CI/CD 管理） |

```text
适用边界速记：
  ✅ 生产/多端共享/热更新/长期维护 → Function
  ✅ 一次性调试、数据修复 → EVAL
  ✅ 老版本 Redis（<7.0）→ EVAL
  ❌ 临时脚本别注册 Function（会持久化+复制，成为长期包袱）
```

> 🎯 面试金句：**"Function 是 Redis 7.0 把 Lua 脚本产品化的机制——命名库、RDB 持久化、自动复制、版本化热更新；2026 年官方建议新生产代码用 Function，EVAL 退居一次性场景"**。

---

**下一模块**：[06-经典脚本实战](06-经典脚本实战.md) / **返回总览**：[00-Lua脚本总览](00-Lua脚本总览.md)
