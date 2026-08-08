# Agent 结构化记忆：会话与状态持久化

> 数据库是 Agent 记忆的默认载体：聊天历史（有界行）、短期状态（KV/检查点）、长期事实（claim 生命周期表）。2026 年共识——分层记忆 + 记忆管理器，原始对话 RAG 已证明不够用。

## 1. 记忆分层架构（2026 共识）

| 层 | 存什么 | 典型存储 | 读取时机 |
|---|---|---|---|
| 近期对话 | 当前会话消息 | 聊天历史表（有界读取） | 每轮 |
| 滚动摘要 | 历史对话压缩 | 表/缓存 | 超窗时 |
| 结构化事实 | 稳定事实/偏好/决策 | KV 或 claim 表 | 按需召回 |
| 情景记忆 | 带时间戳的事件 | 事件表/向量片段 | 相关任务 |
| 向量片段 | 可检索的文档化记忆 | 向量列/向量库 | 语义召回 |
| 归档 | 已废弃/过时记忆 | 同库标记废弃 | 永不（SQL 过滤） |

**记忆管理器（Memory Manager）**：策略层，决定存什么、更新什么、检索什么、按什么优先级组装"最小有用上下文包"：

```text
优先级：当前消息 → 近期轮次 → 活跃结构化决策 → 情景事件 → 滚动摘要 → 向量片段
```

> 🎯 核心要点：**记忆不是"把历史全塞进上下文"，而是"挑最小的有用包"**——记忆管理器负责挑，数据库负责存。

## 2. 为什么"对话历史 RAG"不够

| 缺陷 | 表现 |
|---|---|
| 信噪比崩塌 | 检索到的是"段落"而非"事实"，噪声淹没信号 |
| 无关系感知 | 语义相似的未必结构相关（"张三"与"张总的订单"） |
| 无矛盾检测 | 新旧事实同时召回，互相打架 |
| 无时效概念 | 过期记忆无法自动失效 |

→ 结论：**结构化事实（KV + 关系表）优于原始对话 RAG**，这是 2026 年共识。

## 3. 聊天历史：有界行存储

```sql
CREATE TABLE chat_messages (
    id        BIGSERIAL PRIMARY KEY,
    thread_id TEXT NOT NULL,          -- 会话 id（对应用户+会话）
    role      TEXT NOT NULL,          -- user / assistant / tool
    content   TEXT NOT NULL,
    meta      JSONB,                  -- token 数、工具调用等
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_chat_thread ON chat_messages (thread_id, id);
```

| 策略 | 做法 | 目的 |
|---|---|---|
| 有界读取 | 读取只取最近 N 条（`history_size=N`） | 控 token，**不删行**保审计 |
| 滚动摘要 | 超出窗口的历史先摘要入库 | 长会话不丢信息 |
| 索引 | (thread_id, id) 复合索引 | 分页/恢复 O(log n) |
| 并发 | 数千会话同时读写无冲突 | 行级隔离 |

## 4. 短期状态：检查点与 KV

### 4.1 检查点（Checkpoint）：框架级状态保存

```python
# LangGraph 示例：每一步图状态存 Redis，可断点恢复/回放
from langgraph.checkpoint.redis import RedisSaver

checkpointer = RedisSaver.from_conn_info(host="redis", port=6379)
graph = build_agent().compile(checkpointer=checkpointer)

config = {"configurable": {"thread_id": "user-42"}}
result = graph.invoke({"messages": [...]}, config)   # 中途崩溃可从该步恢复
```

| 检查点能力 | 存什么 | 价值 |
|---|---|---|
| 图状态 | 消息、工具结果、推理产物 | 崩溃恢复 |
| 线程续跑 | thread_id 维度 | 跨设备续聊 |
| 回放/分支 | 历史状态快照 | 调试与多方案 |

### 4.2 KV Store：跨会话事实

```text
键：namespace + key（如 user:42 / pref:region）
值：JSON（偏好、决策、状态）
特性：可选向量索引做语义搜索；TTL 实现记忆衰减
```

Redis 适合延迟敏感（亚毫秒）+ 向量检索 + TTL 衰减；关系表适合需要事务与审计的场景。

## 5. 长期事实：claim 生命周期（关系表即知识图谱）

2026 主流：**不用专用图数据库，用关系表建模实体与事实**：

```sql
CREATE TABLE claims (
    id           BIGSERIAL PRIMARY KEY,
    entity       TEXT NOT NULL,          -- 实体：user:42
    claim        TEXT NOT NULL,          -- 事实："偏好简洁回答"
    state        TEXT DEFAULT 'PROPOSED' -- PROPOSED→ACCEPTED→DEPRECATED
    confidence   REAL,
    valid_from   DATE,
    valid_until  DATE,
    evidence     JSONB,                  -- 溯源：来源消息/文档
    supersedes   BIGINT REFERENCES claims(id)   -- 矛盾处理
);
CREATE INDEX idx_claims_entity ON claims (entity) WHERE state = 'ACCEPTED';
```

| 机制 | 作用 |
|---|---|
| 状态机 | PROPOSED→ACCEPTED→DEPRECATED；查询只读 ACCEPTED（SQL 级过滤，过期永不到达模型） |
| 冲突检测 | 保存时与邻近事实交叉核对，矛盾则 supersedes 旧 claim（MOSAIC 2026 前沿） |
| 图遍历 | 实体-claim 关系用递归 CTE 做多跳查询 |
| 混合检索 | 向量（语义）+ 关键词（BM25）+ 图（2 跳）+ 时效 四路召回 → RRF 融合 |
| ACID 写路径 | 提取+落库+废弃旧事实在**一个事务**完成 |

## 6. 存储选型：一个库 vs 多个库

| 方案 | 结构 | 适合 |
|---|---|---|
| 一库化（2026 趋势） | 一个 PG/Oracle/Redis 同时装：向量列 + 历史表 + claim 表 + 语义缓存 | 中小项目、合规简单 |
| 多库分工 | 业务库 + Redis（状态）+ 向量库（RAG） | 大项目、既有架构 |

> 💡 一库化收益：一套权限、一套备份、一套审计（合规成本骤降）；代价：单库压力与耦合。Oracle AI Database / Snowflake / Redis 2026 都官方支持"一个库全装"。

**本地开发的最小一库化**（OpenFang 模式）：单 SQLite 三个表搞定全部记忆——`kv_store`（Agent 作用域键值状态）、`memory_fragments`（记忆片段 + 向量列 + FTS5 兜底全文检索）、`entities/relations`（知识图谱表）——外加一个整合引擎做指数衰减、片段合并与用量统计。个人项目/教学演示完全够用，生产再平滑迁移 PG。

## 7. 记忆治理与合规

| 治理项 | 做法 |
|---|---|
| 溯源 | 每条记忆带 source 时间戳/提取方式/置信度 |
| 作用域 | 记忆分级：private/team/org/customer |
| 保留期 | 按层设保留策略（会话 90 天、事实按 valid_until） |
| 权限 | RBAC 最小权限 + 记忆按作用域隔离 |
| PII | 保存前脱敏（邮箱/手机哈希化） |
| 回滚 | 版本历史支持恢复 |
| 可观测 | 提取质量、置信度校准、检索归因（答案→claim→证据→来源） |

## 8. 落地顺序建议

| 阶段 | 建什么 | 何时需要 |
|---|---|---|
| 1 | 聊天历史表（有界读取） | 演示界面刷新不丢、多轮续聊 |
| 2 | 检查点 + KV（Redis） | 任务中断恢复、跨设备 |
| 3 | claim 表 + 混合检索 | 长期助手、个性化、多用户 |
| 4 | 记忆治理全套 | 合规监管环境 |

> 🎯 核心要点：记忆的成熟度是**从"能存"到"能挑"到"能弃"**——能存（历史表）→ 能挑（记忆管理器+混合检索）→ 能弃（claim 生命周期+保留策略）。只做到"能存"的 Agent 记忆，是失控的缓存。

---

**下一模块**：[06-业务查询场景：只读数据分析](06-业务查询场景：只读数据分析.md)　**返回总览**：[00-数据库交互总览](00-数据库交互总览.md)

## 参考来源

- [Which Agent Memory Approach Is Best for Long Conversations?（Oracle）](https://blogs.oracle.com/developers/which-agent-memory-approach-is-best-for-long-conversations)
- [Accurate and Efficient Long-Term Memory for LLM Agents（arXiv 2607.16211）](https://arxiv.org/abs/2607.16211)
- [Scaling Agent Context with Knowledge Graphs（Capital One）](https://capitalonesoftware.com/blog/scaling-agent-context-snowflake-knowledge-graphs)
- [Adding Long-Term Memory to LangGraph and LangChain Agents（Hindsight）](https://hindsight.vectorize.io/blog/2026/03/24/langgraph-longterm-memory)
- [One Database for the Whole LangChain Ecosystem（Oracle）](https://blogs.oracle.com/developers/one-database-for-the-whole-langchain-ecosystem-memory-persistence-and-deep-agents-on-oracle-ai-database)
- [Build Smarter AI Agents: Manage Short-Term and Long-Term Memory with Redis（Redis Blog）](https://redis.io/blog/build-smarter-ai-agents-manage-short-term-and-long-term-memory-with-redis.md)
