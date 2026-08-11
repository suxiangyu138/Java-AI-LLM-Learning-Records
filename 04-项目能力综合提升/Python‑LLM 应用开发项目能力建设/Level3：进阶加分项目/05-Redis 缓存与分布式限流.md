# 05 Redis 缓存与分布式限流

> 第五个加分项：给项目引入 Redis——响应缓存与语义缓存落地（03 篇的缓存从这里真正实现）、会话共享（多 worker 间对话历史一致）、分布式限流（多 worker 下限流不再失真）。Redis 是 Python 后端项目的标配中间件，与仓库「Redis」主体系分工为"项目落地视角"。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [为什么现在引入 Redis](#2-为什么现在引入-redis)
3. [环境搭建：Redis 8.10 与 redis-py](#3-环境搭建redis-810-与-redis-py)
4. [响应缓存与语义缓存落地](#4-响应缓存与语义缓存落地)
5. [会话共享：多 worker 的一致性](#5-会话共享多-worker-的一致性)
6. [分布式限流：令牌桶与滑动窗口](#6-分布式限流令牌桶与滑动窗口)
7. [常见坑](#7-常见坑)

---

## 1. 目标与验收

本模块的产出：Redis 三件事落地——响应缓存（重复问题 0 模型调用）、会话共享（多 worker 下对话上下文一致）、分布式限流（多 worker 下限流计数正确）；压测验证（04 篇的压测在 Redis 接入前后对比）。验收标准：**同一问题二次请求不产生 LLM 调用**（llm_calls 计数验证）；**多 worker 下会话历史不错乱**；**限流在多 worker 下计数精确**。版本基线（2026-08）：Redis 8.10、redis-py（asyncio 版）。与仓库「Redis」「Redis Lua脚本」体系的分工：那边讲 Redis 原理与命令族，本模块只讲"项目里这三件事怎么做"。

## 2. 为什么现在引入 Redis

Redis 不是 Level2 就该上的——引入时机与引入理由同样重要：**单 worker 阶段不需要**（进程内缓存够用、限流单进程准确、会话在 SQLite）；**多 worker 阶段必须上**（04 篇扩 worker 后三个问题浮出水面：进程内缓存互不共享（每个 worker 缓存一份，命中率砍半）、会话状态在内存里各持一份（错乱）、单进程限流计数失效（每个 worker 各自计数，总限额变 N 倍））。**Redis 解决的正是这三件事的"共享"问题**——它是多进程/多机架构下的共享状态层。**选型边界**：Redis 管"热数据共享"（缓存、计数、会话），SQLite/PostgreSQL 管"事实数据"（对话历史、文档元数据）——**不要把 Redis 当数据库用**（数据持久化与事务交给数据库），这是 2026 年中间件选型的第一常识。

## 3. 环境搭建：Redis 8.10 与 redis-py

开发环境用 Docker 一键起（Level2 09 的 compose 加一个服务），生产环境同样 compose/K8s 托管：

```yaml
# docker-compose.yml 追加
  redis:
    image: redis:8.10
    command: ["redis-server", "--appendonly", "yes"]   # AOF 持久化
    volumes:
      - ./data/redis:/data
    ports: ["6379:6379"]
```

```python
# app/core/redis_client.py
from redis.asyncio import Redis
from app.config import settings

redis = Redis.from_url(settings.redis_url, decode_responses=True)  # 全局单例

# lifespan 里验证连接（Level2 03）：await redis.ping()
```

两个规范：**redis-py 用 asyncio 版**（`redis.asyncio`——同步版会阻塞事件循环，04 篇的坑在这里预设）；**连接用全局单例**（连接池由客户端管理，每请求新建连接是连接泄漏的温床——与 OpenAI client 同一纪律）。**缓存键命名规范**（项目内统一）：`kbqa:cache:qa:{hash}`、`kbqa:session:{id}`、`kbqa:ratelimit:{key}`——前缀 + 冒号分层，一眼看懂归属，避免键冲突（与 08 篇日志字段命名同一纪律）。

## 4. 响应缓存与语义缓存落地

03 篇的响应缓存在这里正式落地：**精确缓存**（最简单——key 是问题哈希）：

```python
async def ask_with_cache(db, session_key, question) -> str:
    cache_key = f"kbqa:cache:qa:{md5(question.encode()).hexdigest()}"
    cached = await redis.get(cache_key)
    if cached:
        return cached                                   # 命中：0 模型调用
    answer = await ask(db, session_key, question)        # 未命中：正常链路
    await redis.set(cache_key, answer, ex=3600)          # TTL 1 小时
    return answer
```

实现要点：**TTL 是正确性的一部分**（知识库更新时主动删除相关缓存——`DELETE kbqa:cache:qa:*` 按文档批量删，与 Level2 04 的删除流程联动）；**缓存穿透防护**（并发同问时用 `SETNX` 锁住一个请求去生成，其余等待——多 worker 下尤其重要，不然高并发重复问会瞬间打爆模型额度）；**语义缓存**（进阶——key 是问题 embedding 的最近邻查询：`redis` 存"问题向量 + 答案"，新问题向量化后在缓存里检索近邻，距离 < 阈值命中——03 篇已说明，Redis 只做存储，检索逻辑复用 06 篇的向量检索姿势）。**缓存的验收实验**：同一问题连问 5 次，llm_calls 表只有 1 条记录（08 篇的成本埋点验证）——**这个实验是"缓存真的生效"的证据**。

## 5. 会话共享：多 worker 的一致性

04 篇扩 worker 后，对话上下文不能靠 worker 内存——虽然 Level2 04 已经把历史存进 SQLite（事实层已共享），但每次请求都要查库重拼上下文（延迟浪费 + SQLite 读放大）。Redis 做**热会话缓存**：会话的最近 N 条消息以 JSON 存 Redis（TTL 30 分钟滑动续期），命中直接取、未命中回源 SQLite 并写缓存：

```python
async def get_history(session_key: str, limit: int = 10) -> list[dict]:
    key = f"kbqa:session:{session_key}"
    raw = await redis.get(key)
    if raw:
        await redis.expire(key, 1800)                    # 滑动续期
        return json.loads(raw)[-limit:]
    messages = await get_recent_messages_db(session_key, limit=limit)  # 回源
    await redis.set(key, json.dumps(messages), ex=1800)
    return messages
```

要点：**写路径双写**（新消息同时写 SQLite（事实）与 Redis（缓存）——Redis 是缓存不是存储，Redis 丢数据（重启/淘汰）后从 SQLite 回源重建，**Redis 缓存永远可以被丢弃**是设计前提）；**一致性窗口**（缓存 TTL 内的旧数据可接受——对话场景 30 分钟窗口无感，写后清缓存或短 TTL 是常见取舍）；**多 worker 验证**（04 篇压测并发对话，断言会话上下文不串——压测脚本加"会话连贯性"断言：每个 session 的问题与答案必须对应）。**缓存一致性的验收实验**：写入新消息后立即查询（强一致路径——应读到最新，走写后清缓存或短 TTL）；等待 TTL 过期后查询（回源路径——应完整重建）；重启 Redis 后查询（重建路径——缓存可丢，SQLite 兜底）。三个路径都验证过，缓存设计才算闭环。

## 6. 分布式限流：令牌桶与滑动窗口

Level2 10 的内存限流在多 worker 下失效（每个 worker 各自计数）——Redis 限流把计数放到共享层。2026 年标准做法：**滑动窗口**（Redis INCR + EXPIRE，简单可靠）与**令牌桶**（Lua 脚本保证原子性——仓库「Redis Lua脚本」体系有完整实现，本模块用现成姿势）：

```python
# 滑动窗口限流：固定窗口计数（简化版；精确滑动窗口用 ZSET）
RATE_LIMIT_LUA = """
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local current = redis.call('INCR', key)
if current == 1 then
    redis.call('EXPIRE', key, window)
end
if current > limit then
    return 0
end
return 1
"""

async def allow(session_key: str, limit: int = 20, window: int = 60) -> bool:
    ok = await redis.eval(RATE_LIMIT_LUA, 1, f"kbqa:ratelimit:{session_key}", limit, window)
    return bool(ok)
```

三个要点：**Lua 脚本保证原子性**（INCR + EXPIRE 两步操作在单脚本内原子执行——多 worker 并发下计数不丢不重，这是 Redis 脚本机制的核心价值，机制详解见「Redis Lua脚本」体系）；**key 按维度隔离**（按会话 ID、按 IP、按用户三档——接口级限流用 IP、对话级用 session，各配各的阈值）；**超限响应走错误契约**（Level2 03 的 429 + code 字段，前端可分支提示"稍后再试"）。**限流参数的校准**（Level2 10 的"先观察再设阈值"在 Redis 版同样适用——04 篇压测数据就是阈值依据：压测得出的容量上限 × 0.8 就是限流阈值）。

## 7. 常见坑

**同步 redis 客户端阻塞异步**：`import redis`（同步版）在 async 路由里调用——统一 `from redis.asyncio import Redis`，阻塞就是 04 篇说的 IO 瓶颈。

**缓存击穿雪崩**：热点 key 过期瞬间大量请求穿透——加锁重建（SETNX）+ 随机过期时间（TTL 加抖动），高并发缓存的三件套（击穿/穿透/雪崩）在这个项目里至少处理前两个。

**Redis 当存储用**：把对话历史只存 Redis——重启全丢——事实数据永远 SQLite/PostgreSQL，Redis 只做可丢缓存，回源路径必须有。

**键污染**：键没有前缀/没有 TTL 的无限增长——命名规范 + 全部带 TTL（无 TTL 的键是事故），定期 SCAN 巡检（仓库「Redis」体系有巡检姿势）。

**限流误伤正常用户**：阈值太紧——压测数据定阈值（第 6 节），限流被触发时日志记录触发原因（08 篇），可审计。

**缓存键污染与泄漏**：键里带了用户输入的完整内容（隐私 + 超长键）——键用哈希（md5/sha256）而非原文，敏感内容不进键也不进缓存值（与 03 篇的缓存纪律联动）。

**多 worker 下缓存一致性玄学**：写了 Redis 但各 worker 还在用进程内缓存——统一走 Redis 封装（一个 get/set 函数），禁止业务代码直接 import 本地 dict 缓存。

**Redis 未设密码暴露公网**：6379 端口裸奔是事故前奏——compose 里设密码（`--requirepass`）与绑定内网访问，Redis 权限最小化与 Level3 09 篇的防火墙纪律同源。

> 🎯 **核心要点**：Redis 在项目里的角色一句话——**"多 worker 时代的共享状态层"**：缓存省重复调用、会话缓存省回源延迟、限流计数全局一致。Redis 管热数据、数据库管事实数据，这个分工边界清晰了，Redis 的引入就从"加了个中间件"变成"架构演进的一部分"。

---

**下一模块**：[06 数据升级：PostgreSQL 与 Milvus](./06-数据升级：PostgreSQL%20与%20Milvus.md) | **返回总览**：[Level3 总览](./00-Level3%20进阶加分项目%20总览.md)

【参考来源】
- [Redis 8 官方文档](https://redis.io/docs/)
- [redis-py（asyncio）文档](https://redis-py.readthedocs.io/)
- [Redis Lua脚本 体系（本仓库）](https://github.com/suxiangyu138/Java-AI-LLM-Learning-Records)
