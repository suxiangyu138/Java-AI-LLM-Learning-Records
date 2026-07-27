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

## 核心要点回顾

- 三层记忆 = 工作（当前任务）+ 短期（当前会话）+ 长期（永久）
- 长期记忆用向量数据库实现（存语义 → 按相似度检索）
- 滑动窗口 + 摘要压缩 + 向量检索 = 生产级记忆方案
- 重要信息主动存入长期记忆（关键决定、用户偏好、学习经验）
