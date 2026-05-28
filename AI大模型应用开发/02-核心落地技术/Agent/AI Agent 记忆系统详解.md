# AI Agent 记忆系统详解（Java 后端 + AI 全栈实战版）

> **文档定位**：AI Agent 核心技术文档 | 记忆系统设计深度解析
> **核心问题**：Agent 如何记住上下文？如何跨会话持久化？如何动态检索知识？

---

## 一、Agent 记忆分层模型

```
Agent 记忆系统
├── 工作记忆（Working Memory）
│   └── 当前推理链路的临时状态（变量、中间结果、工具返回）
├── 短期记忆（Short-Term Memory）
│   └── 当前会话的对话历史（滑动窗口管理）
├── 长期记忆（Long-Term Memory）
│   ├── 用户画像（偏好、习惯、历史行为）
│   ├── 知识记忆（业务知识、文档、FAQ）
│   └── 经验记忆（成功/失败的任务轨迹）
└── 感知记忆（Sensory Memory）
    └── 当前输入的原始信息（用户消息、图片、语音）
```

---

## 二、三层记忆对比

| 维度 | 工作记忆 | 短期记忆 | 长期记忆 |
|---|---|---|---|
| **存储位置** | LLM 上下文窗口 | 会话缓存（Redis/内存） | 向量库 + 结构化 DB |
| **生命周期** | 单次推理周期 | 单次会话 | 跨会话持久化 |
| **容量** | KB~MB 级 | 几十条消息 | GB~TB 级 |
| **检索方式** | 直接注入 Prompt | 滑动窗口取最近 N 条 | 向量相似度检索 |
| **技术** | Prompt 变量 | 内存/Redis List | Vector DB + RAG |

---

## 三、短期记忆实现

### 3.1 滑动窗口（最简单）

```python
class ShortTermMemory:
    def __init__(self, max_size: int = 20):
        self.messages = []
        self.max_size = max_size
    
    def add(self, role: str, content: str):
        self.messages.append({"role": role, "content": content})
        # 超过窗口大小，丢弃最早的
        if len(self.messages) > self.max_size:
            self.messages = self.messages[-self.max_size:]
    
    def get_context(self) -> list:
        return self.messages
```

### 3.2 摘要压缩

```
原始消息（50 条）→ LLM 生成摘要(500 tokens) → 旧消息可丢弃

技巧：
- 保留最近 N 条原始消息（保证细节）
- 更早的消息压缩为摘要（节省 Token）
```

```python
def compress_history(messages, keep_last=10):
    old = messages[:-keep_last]
    recent = messages[-keep_last:]
    
    if old:
        summary_prompt = "请将以下对话历史压缩为 200 字以内的摘要：\n"
        summary_prompt += "\n".join([m["content"] for m in old])
        summary = llm.chat(summary_prompt)
        return [{"role": "system", "content": f"对话历史摘要：{summary}"}] + recent
    return recent
```

### 3.3 Token 感知的窗口管理

```python
import tiktoken

class TokenAwareMemory:
    def __init__(self, max_tokens: int = 4000):
        self.messages = []
        self.max_tokens = max_tokens
        self.encoder = tiktoken.get_encoding("cl100k_base")
    
    def add(self, role, content):
        self.messages.append({"role": role, "content": content})
        self._trim()
    
    def _trim(self):
        while self._total_tokens() > self.max_tokens:
            self.messages.pop(0)  # 丢掉最早的
    
    def _total_tokens(self):
        return sum(len(self.encoder.encode(m["content"])) for m in self.messages)
```

---

## 四、长期记忆实现

### 4.1 架构

```
写入：
新信息 → Embedding → 向量库存储 + 结构化元数据存储

读取：
当前上下文 → 生成检索 Query → 向量搜索 → 排序 → 注入 Prompt
```

### 4.2 记忆写入

```python
from openai import OpenAI

class LongTermMemory:
    def __init__(self, vector_db, embedding_client):
        self.vector_db = vector_db      # Milvus / Pinecone / pgvector
        self.embedder = embedding_client
    
    def store(self, content: str, metadata: dict, memory_type: str):
        # 1. 生成向量
        vector = self.embedder.embed(content)
        
        # 2. 存入向量库
        self.vector_db.insert(
            collection=f"memory_{memory_type}",
            vector=vector,
            metadata={
                "content": content,
                "timestamp": datetime.now().isoformat(),
                **metadata
            }
        )
    
    def store_user_preference(self, user_id: str, preference: str):
        self.store(
            content=preference,
            metadata={"user_id": user_id},
            memory_type="user_profile"
        )
    
    def store_experience(self, task: str, result: str, success: bool):
        self.store(
            content=f"Task: {task}\nResult: {result}",
            metadata={"success": success},
            memory_type="experience"
        )
```

### 4.3 记忆检索

```python
def retrieve(self, query: str, user_id: str = None, top_k: int = 5):
    query_vector = self.embedder.embed(query)
    
    results = self.vector_db.search(
        collection="memory_user_profile",
        vector=query_vector,
        filter={"user_id": user_id} if user_id else None,
        top_k=top_k
    )
    
    memories = [r["metadata"]["content"] for r in results]
    return memories

# 注入 Prompt
def build_prompt_with_memory(user_query, user_id):
    relevant_memories = retrieve(user_query, user_id)
    
    prompt = "以下是与该用户相关的历史记忆：\n"
    for i, m in enumerate(relevant_memories):
        prompt += f"{i+1}. {m}\n"
    prompt += f"\n用户当前问题：{user_query}\n"
    
    return prompt
```

---

## 五、记忆混合检索（Hybrid Search）

```python
def hybrid_retrieve(query: str, user_id: str, alpha=0.7):
    """
    alpha=0.7: 70% 语义相关 + 30% 时间衰减
    """
    query_vector = embedder.embed(query)
    
    results = vector_db.search(query_vector, top_k=20)
    
    scored = []
    for r in results:
        semantic_score = r["score"]                          # 语义相似度
        age_days = (now - r["timestamp"]).days
        time_score = 1.0 / (1.0 + age_days / 30)             # 时间衰减
        
        final_score = alpha * semantic_score + (1 - alpha) * time_score
        scored.append((r, final_score))
    
    scored.sort(key=lambda x: x[1], reverse=True)
    return [s[0] for s in scored[:5]]
```

---

## 六、记忆管理策略

| 策略 | 方法 | 适用场景 |
|---|---|---|
| **索引衰减** | 越旧记忆权重越低 | 时效性敏感 |
| **使用频率加权** | 被频繁检索的记忆加分 | 用户偏好 |
| **重要性标记** | LLM 标记记忆重要性 | 关键信息 |
| **冲突解决** | 新旧记忆冲突 → 以新为准或询问用户 | 偏好变化 |
| **定期清理** | 删除长期未访问的低分记忆 | 控制存储成本 |

---

## 七、实战模板：Agent Memory 完整类

```python
class AgentMemory:
    def __init__(self):
        self.short_term = ShortTermMemory(max_messages=20)    # 会话上下文
        self.long_term = LongTermMemory()                     # 向量长期存储
        self.working = {}                                     # 当前任务临时变量
    
    def build_context(self, user_query: str, user_id: str) -> str:
        """构建注入 LLM 的完整上下文"""
        parts = []
        
        # 1. 长期记忆（从向量库检索）
        memories = self.long_term.retrieve(user_query, user_id)
        if memories:
            parts.append("【相关历史信息】\n" + "\n".join(f"- {m}" for m in memories))
        
        # 2. 短期记忆（当前会话）
        dialog = self.short_term.get_context()
        if dialog:
            parts.append("【当前会话】\n" + "\n".join(f"{m['role']}: {m['content']}" for m in dialog))
        
        # 3. 工作记忆（当前任务状态）
        if self.working:
            parts.append("【当前任务状态】\n" + json.dumps(self.working, ensure_ascii=False))
        
        parts.append(f"\n【用户最新消息】\n{user_query}")
        return "\n\n".join(parts)
```

---

## 八、面试核心要点

1. **Agent 三层记忆分别是什么？** 工作记忆（推理中）、短期记忆（当前会话）、长期记忆（跨会话）
2. **短期记忆怎么管理 Token？** 滑动窗口 + 摘要压缩 + Token 计数
3. **长期记忆怎么检索？** 向量相似度搜索 + 混合检索（语义 + 时间衰减）
4. **什么时候写长期记忆？** 任务成功时保存经验、用户明示偏好时保存画像
5. **RAG 和 Agent 记忆什么关系？** RAG 是外部只读知识库，记忆是 Agent 自己积累的经验和用户画像

---

## 九、极简总结

```
短期记忆 = 当前对话的滑动窗口（内存/Redis）
长期记忆 = 向量库存储（相似度检索）
工作记忆 = 当前任务临时变量（Prompt 注入）
管理 = Token 计数 + 摘要压缩 + 时间衰减 + 定期清理
关键 = Agent 能"记住"用户 + 能从"过去"学习
```
