# 06 Sessions：状态管理

> 对话记忆的关键：Session 负责跨运行的历史持久化——没有 Session，每次 Runner.run() 都是冷启动；InMemory/SQLite/Redis 三档实现，从测试到生产。

## 📚 目录

1. [为什么需要 Sessions](#1-为什么需要-sessions)
2. [Session 机制](#2-session-机制)
3. [三种内置实现](#3-三种内置实现)
4. [多轮对话落地](#4-多轮对话落地)
5. [RunState 与恢复](#5-runstate-与恢复)
6. [上下文与状态的区分](#6-上下文与状态的区分)
7. [面试高频问法](#7-面试高频问法)
8. [会话管理的工程实践](#8-会话管理的工程实践)
9. [常见误区](#9-常见误区)

## 1. 为什么需要 Sessions

### 冷启动问题

```
没有 Session：每次 Runner.run() 都是冷启动
（模型不知道之前的对话——多轮能力缺失）
```

| 场景 | 无 Session | 有 Session |
|---|---|---|
| 多轮客服 | 每轮失忆 | 记住上下文 |
| 长任务续接 | 从头再来 | 从断点继续 |
| 多用户 | 互相串扰 | 按 Session 隔离 |

### 对应关系

```
Session = Agent 版的"对话历史持久化"
（类比 RAG 阶段 2 的多轮设计 / 传统 Web 的 Session）
```

## 2. Session 机制

### 核心概念

```
Session 保存：
对话消息历史（跨运行）
运行状态（RunState）
按 session_id 隔离（多用户/多会话）
```

### 用法

```python
from agents import Agent, Runner
from agents.sessions import InMemorySession

# 1. 创建/加载 Session
session = InMemorySession()          # 新会话
# 或加载已存在的（多轮续接）
# session = InMemorySession(id="user-123")

# 2. 运行并携带 Session
result = Runner.run_sync(
    agent, "第一轮问题",
    session=session,                 # 关键：传入 Session
)

# 3. 第二轮（Session 记住历史）
result2 = Runner.run_sync(
    agent, "还记得刚才的问题吗？",
    session=session,                 # 同一 Session → 有记忆
)
```

### 运行流程

```
Runner.run(session=...) →
① 从 Session 恢复历史
② 追加本轮消息
③ 运行循环
④ 更新 Session（保存新历史）
```

## 3. 三种内置实现

| 实现 | 持久化 | 适用 |
|---|---|---|
| InMemorySession | 内存（进程内） | 测试/单进程 |
| SqliteSession | SQLite 文件 | 中小规模 |
| RedisSession | Redis | 高规模/多实例 |

### 选型逻辑

```
测试 → InMemorySession（零依赖）
单机生产 → SqliteSession（文件持久化）
多实例/高并发 → RedisSession（共享）
自定义需求 → Session 子类
```

### Redis 示例

```python
from agents.sessions import RedisSession
import redis

r = redis.Redis(host="localhost", port=6379)
session = RedisSession(redis_client=r, id="user-123")
```

## 4. 多轮对话落地

### 完整多轮模式

```python
class ChatService:
    def __init__(self):
        # 生产用 RedisSession（多实例共享）
        self.sessions = {}      # user_id → session

    def ask(self, user_id: str, query: str) -> str:
        # 按用户取/建 Session
        session = self.sessions.setdefault(
            user_id, RedisSession(redis_client=r, id=user_id),
        )
        result = Runner.run_sync(
            self.agent, query,
            session=session,
            max_turns=10,
        )
        return result.final_output
```

### 多轮设计要点

| 要点 | 说明 |
|---|---|
| Session 按用户隔离 | user_id → session（防串扰） |
| 历史自动增长 | Session 管理（无需手动传历史） |
| 会话清理 | 过期会话清理（Redis TTL） |
| 上下文窗口 | 超长历史需摘要（Session 不解决窗口） |

## 5. RunState 与恢复

### RunState 是什么

```
运行状态（RunState）：
保存到 Session → 中断后可恢复
（对应"检查点"思想，MAF/AutoGen 都有）
```

### 恢复流程

```
① 运行中断（崩溃/超时/手动停止）
② Session 中保存了运行状态
③ 恢复：从上次状态继续（不从头）
```

### 使用场景

```
长任务（多轮工具调用）中断恢复
流式对话中断续接
成本控制（超预算暂停 → 恢复）
```

## 6. 上下文与状态的区分

### 2026 文档澄清的关键区分

| 概念 | 内容 | 生命周期 |
|---|---|---|
| 本地应用上下文（RunContextWrapper） | 应用数据（用户 ID/权限/配置） | 单次运行 |
| 对话状态（Session） | 消息历史/运行状态 | 跨运行持久 |

### 区分的重要性

```
RunContextWrapper：每次运行传入（应用级数据）
Session：跨运行保存（对话级状态）
混淆的后果：应用数据被持久化（泄漏/过期）
```

### 使用建议

```
应用数据（当前请求相关）→ context（不持久）
对话数据（需要记住的）→ session（持久化）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| Session 解决什么？ | 跨运行对话历史持久化（免冷启动） |
| 三种实现？ | InMemory/SQLite/Redis（测试到生产） |
| 多轮怎么做？ | Runner.run(session=...) 同一 Session |
| RunState？ | 运行状态保存（中断恢复） |
| context 与 session 区别？ | 应用数据 vs 对话状态（生命周期不同） |
| 多用户怎么隔离？ | user_id → session |

### 面试加分表达

> "Session 是 SDK 的记忆层：Runner.run 传 session，历史自动恢复与保存——没有它每次运行都是冷启动。选型按规模：测试 InMemory、单机 SQLite、多实例 Redis。2026 文档还澄清了关键区分：RunContextWrapper 是应用级上下文（不持久），Session 是对话状态（持久化）——混用会导致应用数据泄漏。"

## 8. 会话管理的工程实践

### 会话生命周期

```
创建 → 使用（多轮）→ 过期清理
生命周期设计：
① 会话 TTL（Redis 过期/定期清理）
② 上限（每用户最多 N 会话）
③ 归档（长会话摘要归档）
```

### 会话与缓存的关系

```
会话 = 对话状态（必须持久）
缓存 = 结果复用（可丢弃）
别把缓存当会话（结果过期 ≠ 对话结束）
```

### 多实例部署（RedisSession）

```
多实例共享会话：
实例 A 写入 → 实例 B 读取（Redis 共享）
负载均衡下对话不丢（关键生产需求）
```

### 会话安全

```
① 会话隔离：user_id → session（防串扰）
② 权限：会话归属校验（防越权访问他人会话）
③ 敏感信息：会话内容脱敏存储
```

## 9. 常见误区

| 误区 | 真相 |
|---|---|
| "Session 可选" | 多轮没有它 = 冷启动失忆 |
| "InMemory 够生产" | 仅测试——生产 SQLite/Redis |
| "context 会持久化" | 应用级上下文不持久（区分！） |
| "会话无限保留" | 要 TTL/清理（成本与隐私） |
| "单实例够" | 多实例必须 Redis 共享 |

> 🎯 核心要点：Session = 跨运行对话持久化（免冷启动）；三档实现（InMemory 测试/SQLite 单机/Redis 多实例）；多轮 = 同一 Session 传入 Runner；RunState 支持中断恢复；RunContextWrapper（应用数据）vs Session（对话状态）生命周期不同；工程四件事（TTL/上限/归档/多实例共享）；多用户按 user_id 隔离 + 权限校验。

---

**下一模块**：[07-生产化-追踪-HITL-Sandbox](07-生产化-追踪-HITL-Sandbox.md) / **返回总览**：[00-OpenAI-Agents-SDK知识体系总览](00-OpenAI-Agents-SDK知识体系总览.md)
