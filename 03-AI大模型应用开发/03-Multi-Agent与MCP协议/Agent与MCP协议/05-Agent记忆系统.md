# 05 - Agent 记忆系统

> 🎯 记忆决定了 Agent 能走多远 — 没有记忆的 Agent 每次都是"重新认识你"。三层记忆架构是生产级 Agent 的标配

---

## 目录

1. [三层记忆架构](#1-三层记忆架构)
2. [工作记忆实现](#2-工作记忆实现)
3. [短期记忆实现](#3-短期记忆实现)
4. [长期记忆实现](#4-长期记忆实现)
5. [记忆管理策略](#5-记忆管理策略)

---

## 1. 三层记忆架构

```text
Agent 记忆 = 工作记忆 + 短期记忆 + 长期记忆

┌───────────────────────────────────────────┐
│  工作记忆 (Working Memory)                │
│  → 当前任务的核心信息                       │
│  → 容量=LLM Context Window (4K~128K)       │
│  → 生命周期：单次任务执行期间               │
├───────────────────────────────────────────┤
│  短期记忆 (Short-term Memory)              │
│  → 当前会话的对话历史                       │
│  → 容量=最近N轮 (如20轮)                    │
│  → 生命周期：单次会话                       │
├───────────────────────────────────────────┤
│  长期记忆 (Long-term Memory)               │
│  → 跨会话的知识、偏好、经验                 │
│  → 容量=向量数据库/数据库（几乎无限）         │
│  → 生命周期：永久                           │
└───────────────────────────────────────────┘
```

---

## 2. 工作记忆实现

```python
class WorkingMemory:
    """工作记忆 — 当前任务的上下文"""
    
    def __init__(self, max_tokens=8000):
        self.context = []     # [{role, content}, ...]
        self.max_tokens = max_tokens
    
    def add(self, role, content):
        self.context.append({"role": role, "content": content})
        self._trim_if_needed()  # 超出则裁剪
    
    def _trim_if_needed(self):
        """超出长度 → 压缩早期内容"""
        while self._count_tokens() > self.max_tokens:
            # 保留 system prompt + 最新内容
            self.context.pop(1)  # 移除最早的（保留 system prompt）
    
    def get_context(self):
        return self.context
```

---

## 3. 短期记忆实现

```python
class ShortTermMemory:
    """短期记忆 — 对话历史"""
    
    def __init__(self, max_turns=20):
        self.history = []     # [(user_msg, assistant_msg), ...]
        self.max_turns = max_turns
    
    def add_exchange(self, user_msg, assistant_msg):
        self.history.append((user_msg, assistant_msg))
        if len(self.history) > self.max_turns:
            self.history.pop(0)  # 滑动窗口
    
    def get_summary(self):
        """获取最近对话摘要"""
        return "\n".join([
            f"用户: {u}\n助手: {a}" 
            for u, a in self.history[-5:]  # 最近5轮
        ])
```

---

## 4. 长期记忆实现

### 4.1 向量记忆

```python
class LongTermMemory:
    """长期记忆 — 向量数据库"""
    
    def __init__(self, embed_fn, vector_store):
        self.embed = embed_fn
        self.store = vector_store
    
    def remember(self, content, metadata=None):
        """存入长期记忆"""
        vec = self.embed(content)
        self.store.add(
            id=hash(content),
            vector=vec,
            metadata={"content": content, **(metadata or {}), 
                      "timestamp": time.time()}
        )
    
    def recall(self, query, top_k=5):
        """从记忆中检索相关信息"""
        vec = self.embed(query)
        results = self.store.search(vec, top_k)
        return [r["metadata"]["content"] for r in results]
```

### 4.2 摘要记忆

```python
def summarize_and_store(conversation, llm, long_term_memory):
    """重要会话结束时 → LLM 摘要 → 存入长期记忆"""
    summary = llm(f"用 100 字总结以下对话的关键信息和结论：\n{conversation}")
    long_term_memory.remember(
        content=summary,
        metadata={"type": "conversation_summary"}
    )
```

---

## 5. 记忆管理策略

| 策略 | 做法 | 适用 |
|------|------|------|
| **滑动窗口** | 只保留最近 N 轮 | 简单聊天 |
| **摘要压缩** | 定期压缩老对话 | 长会话 |
| **向量检索** | 按相关性取记忆 | 跨会话知识 |
| **反射提取** | 从经历中提取"经验" | 持续改进 |
| **分层存储** | 重要→永久、次要→摘要、琐碎→丢弃 | 生产推荐 |

---

## 6. 记忆系统的生产实践（2026）

**记忆框架的工程选型**（2026 状态）：

| 方案 | 定位 | 特点 |
|------|------|------|
| 自建（向量库 + Redis） | 灵活 | 需要自己管理记忆生命周期 |
| **Mem0** | AI 原生记忆库 | 语义/情景/程序三层内置、提取与更新自动化 |
| **Zep** | 记忆 + 时序 | 长期记忆 + 图记忆、会话分析 |
| LangMem（LangChain） | 记忆 SDK | 与 LangGraph 生态集成 |

**记忆系统的生产三问**：

```text
① 存什么：什么信息值得长期记？
   → 过滤原则：重要性评分（如 >0.8 才入长期记忆）、去重
② 何时更新：对话中/结束后提炼？
   → 异步提炼（对话结束后后台更新）避免阻塞主流程
③ 怎么过期：旧记忆怎么淘汰？
   → TTL（情景记忆 30-90 天）+ 相关性替换 + 用户显式删除

记忆的合规红线：
  敏感信息脱敏后才入库（PII 清洗）
  用户可查看/可删除自己的记忆（可解释性 + 合规）
```

**记忆与 RAG 的分工**（2026 架构共识）：

```text
RAG：静态知识检索（文档/知识库）—— 公共知识
记忆：动态个性化（用户偏好/历史/上下文）—— 私有知识
架构：Router 分流 → RAG（知识）+ Memory（个性）双通道 → 生成
（详见 02-RAG检索增强生成/17-AgenticRAG 的 Memory 范式）
```

> 🎯 **核心要点**：记忆系统 2026 的成熟度 = "**框架化（Mem0/Zep）+ 生命周期管理（存/更/过期）+ 合规内置（脱敏/可删）**"——记忆不再是"向量库存对话历史"的玩具，而是有 SLA 的基础设施组件。

---

## 7. 记忆的实现技术选型

**记忆存储的技术选型**（2026 实践）：

| 存储 | 适用记忆 | 特点 |
|------|---------|------|
| Redis | 短期/会话记忆 | 快、TTL 天然 |
| 向量库（Milvus 等） | 长期语义记忆 | 相似度召回（见 Milvus 系统） |
| 图数据库 | 关系型记忆 | 实体关联（用户↔偏好↔事件） |
| 对象存储 | 原始对话存档 | 成本低、冷数据 |

```text
记忆检索的双通道：
  短期（Redis）：精确 key 查询（session_id）
  长期（向量库）：语义相似召回（"上次聊过类似问题吗"）
  → 与 RAG 的公共知识检索形成"三通道上下文"
  （RAG 知识 + 短期会话 + 长期个性 —— Router 融合）
```

**记忆更新的触发策略**：

```text
① 实时：重要事件立即写（用户明确偏好/关键事实）
② 对话结束异步：总结提炼 → 重要性评分 → 入库（防阻塞主流程）
③ 定期整理：旧记忆压缩合并（摘要化）、删除过期（TTL）
④ 冲突处理：新旧记忆矛盾时——保留新 + 标记旧（可追溯）
```

> 🎯 **核心要点**：记忆技术选型 = "**按记忆类型选存储**"（短期 Redis/长期向量/关系图谱）+ "**四类更新触发**"（实时/异步/定期/冲突）——2026 年框架（Mem0/Zep）已内置这些策略，自建需自行实现。

---

## 核心要点回顾

- 三层记忆 = 工作（当前任务）+ 短期（当前会话）+ 长期（永久）
- 长期记忆用向量数据库实现（存语义 → 按相似度检索）
- 滑动窗口 + 摘要压缩 + 向量检索 = 生产级记忆方案
- 重要信息主动存入长期记忆（关键决定、用户偏好、学习经验）
