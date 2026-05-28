# AI Agent 成本优化（实战版）

> **文档定位**：AI Agent 核心技术 | Token 与 API 成本控制详解
> **核心问题**：Agent 烧钱太快怎么办？Token 消耗怎么压？不同模型怎么选？

---

## 一、Agent 成本构成

```
单次 Agent 调用的成本 = 

  输入 Token 费用
  + 输出 Token 费用
  + 工具调用开销
  + 基础设施成本
```

| 成本项 | 占比 | 优化空间 |
|---|---|---|
| **System Prompt** | 10-15% | ⭐⭐⭐ 压缩精简 |
| **对话历史** | 30-50% | ⭐⭐⭐⭐⭐ 最大优化空间 |
| **工具描述** | 5-10% | ⭐⭐ 按需注入 |
| **工具返回值** | 10-15% | ⭐⭐⭐ 截断/结构化 |
| **LLM 生成** | 15-25% | ⭐⭐ 限制输出长度 |

---

## 二、成本对比（不同模型的差距巨大）

### 2.1 API 定价对比（2026 参考）

| 模型 | 输入 $/1M tokens | 输出 $/1M tokens | 适合 |
|---|---|---|---|
| Claude Opus 4.7 | $15 | $75 | 复杂推理 |
| Claude Sonnet 4.6 | $3 | $15 | 日常开发（推荐） |
| Claude Haiku 4.5 | $0.80 | $4 | 简单任务 |
| GPT-4o | $2.50 | $10 | 通用 |
| GPT-4o-mini | $0.15 | $0.60 | 简单任务 |
| DeepSeek-V3 | $0.50 | $2.00 | 性价比 |
| Qwen 2.5-Max | $0.50 | $1.50 | 中文优化 |

### 2.2 同任务不同模型花费

```
任务：审查 10 个 Java 文件（约 15K tokens）

Opus 4.7:  输入 $0.23 + 输出 $1.13 = $1.36/次
Sonnet 4.6: 输入 $0.05 + 输出 $0.23 = $0.28/次  ← 5 倍价差
Haiku 4.5:  输入 $0.01 + 输出 $0.06 = $0.07/次  ← 20 倍价差

每天 100 次审查：
  Opus:  $136/天 = $4,080/月
  Sonnet: $28/天 = $840/月    ← 通常够用
```

---

## 三、模型选择矩阵

| 任务类型 | 推荐模型 | 原因 |
|---|---|---|
| 简单问答/格式转换 | **Haiku** | 便宜够用 |
| CRUD 代码生成 | **Sonnet** | 性价比最优 |
| 代码审查/重构 | **Sonnet** | 能力足够 |
| 架构设计/复杂调试 | **Opus** | 推理最强 |
| 批量数据提取 | **Haiku** | 量大省钱 |
| 安全审计 | **Opus** | 不能出错 |
| 前端样式调整 | **Sonnet** | 实用 |
| 日常对话/答疑 | **Haiku** | 即时响应 + 便宜 |

---

## 四、Token 压缩技术

### 4.1 对话历史压缩

```python
class ContextCompressor:
    def compress(self, messages: list, keep_last: int = 10) -> list:
        """将长对话压缩为摘要 + 最近消息"""
        if len(messages) <= keep_last:
            return messages
        
        old = messages[:-keep_last]
        recent = messages[-keep_last:]
        
        # 压缩旧消息
        summary = llm.chat(
            "将以下对话历史压缩为 200 字以内的摘要：\n" +
            "\n".join([f"{m['role']}: {m['content'][:200]}" for m in old]),
            model="haiku"  # 用便宜模型压缩
        )
        
        return [
            {"role": "system", "content": f"历史摘要：{summary}"}
        ] + recent
```

### 4.2 工具描述精简

```python
# ❌ 太啰嗦
TOOL_DESC_BAD = "这个工具用于搜索互联网上的最新信息。当你需要获取实时数据或者你不确定的事实信息时，你应该调用这个工具。它会返回搜索结果的标题、摘要和链接。"

# ✅ 精简有效（LLM 完全能理解）
TOOL_DESC_GOOD = "搜索互联网获取最新信息。返回标题、摘要、链接。"

# 可进一步压缩
TOOL_DESC_MIN = "搜索互联网"
```

### 4.3 工具返回值截断

```python
def truncate_tool_result(result: str, max_chars: int = 2000) -> str:
    """截断工具返回，只保留关键信息"""
    if len(result) <= max_chars:
        return result
    
    # 前 1500 + 后 500
    return result[:1500] + f"\n...(共 {len(result)} 字符，已截断)...\n" + result[-500:]
```

### 4.4 System Prompt 瘦身

```python
# ❌ 冗长
SYSTEM_PROMPT_BAD = """
你是一个智能助手。你需要非常认真、仔细、负责任地处理每一个用户的问题。
在回答时，请确保你的回答是准确的、完整的、有帮助的。
如果你不知道答案，请不要编造，诚实地告诉用户你不知道。
在调用工具之前，请先仔细思考是否真的需要调用工具...
"""
# 约 150 tokens 的废话

# ✅ 精简
SYSTEM_PROMPT_GOOD = "你是智能助手。准确回答，不知道就说不知道，必要时调用工具。"
# 约 25 tokens
```

---

## 五、缓存策略

### 5.1 Prompt Cache（Anthropic 特有）

```python
# Prompt Cache 可以缓存重复的 System Prompt + 工具定义
# 对反复调用的 Agent 场景，缓存命中率可达 70-80%

# 确保重复部分完全一致（不会被缓存破坏）：
# ✅ System Prompt 不变
# ✅ 工具定义不变
# ✅ 前几条消息不变
```

```
缓存命中：输入成本降低 90%
缓存 Miss：正常计费
最佳实践：System Prompt + 工具定义固定在最前面
```

### 5.2 业务层缓存

```python
import hashlib
from functools import lru_cache

class AgentCache:
    def __init__(self):
        self.cache = {}
    
    def get_or_run(self, query: str, agent_func):
        # 对查询做语义哈希（相似问题命中缓存）
        key = hashlib.md5(query.encode()).hexdigest()
        
        if key in self.cache:
            return self.cache[key] + "\n[来自缓存]"
        
        result = agent_func(query)
        self.cache[key] = result
        return result
```

---

## 六、成本监控

```python
class CostTracker:
    def __init__(self):
        self.daily_total = 0
        self.by_model = defaultdict(float)
        self.by_user = defaultdict(float)
    
    def record(self, model: str, input_tokens: int, output_tokens: int, user_id: str):
        pricing = {
            "claude-sonnet-4-6": (3.0, 15.0),
            "claude-haiku-4-5": (0.8, 4.0),
        }
        input_price, output_price = pricing.get(model, (0, 0))
        cost = (input_tokens/1e6 * input_price) + (output_tokens/1e6 * output_price)
        
        self.daily_total += cost
        self.by_model[model] += cost
        self.by_user[user_id] += cost
        
        # 超预算告警
        if self.daily_total > 50:  # $50/天
            send_alert(f"Agent API 日消费超 $50，当前 ${self.daily_total:.2f}")
```

---

## 七、极致省钱方案

```
第 1 层：能用 Haiku 绝不用 Sonnet
第 2 层：System Prompt 精简到 50 tokens 内
第 3 层：工具描述每条不超过 30 字
第 4 层：对话历史压缩为摘要
第 5 层：工具返回值截断到 2000 字符
第 6 层：开 Prompt Cache
第 7 层：语义缓存重复查询
第 8 层：本地模型（Ollama）处理内部/低敏任务

效果：从 $840/月 降到 $100-150/月
```

---

## 八、面试核心要点

1. **Agent 最大成本项是什么？** 对话历史（30-50%），压缩空间最大
2. **怎么选模型？** 简单用 Haiku，日常用 Sonnet，复杂用 Opus
3. **Prompt Cache 省多少？** 命中后输入成本降 90%
4. **怎么压 Token？** 精简 System Prompt + 压缩历史 + 截断工具返回
5. **本地模型有什么作用？** 内部/低敏/高频任务省钱

---

## 九、极简总结

```
选模型 = 简单→Haiku，日常→Sonnet，复杂→Opus
压 Token = 精简 System Prompt + 历史压缩 + 结果截断
缓存 = Prompt Cache（命中省 90%输入）+ 业务层语义缓存
监控 = 按模型/用户/天追踪费用 + 超预算告警
极致省钱 = Haiku + 全开压缩 + 本地模型 = $100/月
```
