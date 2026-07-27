# 04 - 结构化输出与 JSON Mode

> 🎯 Hermes 的结构化输出能力让模型严格按照你指定的 JSON Schema 生成结果——告别"格式不对、缺字段、多废话"的烦恼，是构建 API 级可靠 LLM 应用的基石

---

## 目录

1. [JSON Mode 原理](#1-json-mode-原理)
2. [Pydantic Schema 生成](#2-pydantic-schema-生成)
3. [完整使用示例](#3-完整使用示例)
4. [实战场景](#4-实战场景)
5. [与 Function Calling 的区别](#5-与-function-calling-的区别)

---

## 1. JSON Mode 原理

### 1.1 System Prompt 模板

```
<|im_start|>system
You are a helpful assistant that answers in JSON.
Here's the json schema you must adhere to:
<schema>
{JSON_SCHEMA_HERE}
</schema><|im_end|>
```

> 💡 原理很简单：在 system prompt 中用 `<schema>` XML 标签注入完整的 JSON Schema，模型被训练为**严格输出符合该 schema 的 JSON**

### 1.2 工作机制

```text
用户请求
   ↓
System Prompt 包含 <schema> 注入
   ↓
模型生成 → 约束为 JSON → 符合 Schema → 可直接 parse
   ↓
应用层拿到 Python dict → 直接使用
```

对比普通模型的 JSON 输出：

| 维度 | 普通模型 | Hermes JSON Mode |
|------|---------|-----------------|
| 格式保证 | ❌ 可能输出 markdown/废话 | ✅ 纯 JSON |
| Schema 遵循 | ❌ 自己猜结构 | ✅ 严格匹配 schema |
| 缺字段 | ❌ 经常发生 | ✅ 强制填充 |
| 可 parse 率 | ~70-80% | **~99%** |
| 多一层校验 | ❌ 需要 retry logic | ✅ 基本不需要 |

## 2. Pydantic Schema 生成

### 2.1 从 Pydantic Model 到 JSON Schema

```python
from pydantic import BaseModel, Field
from typing import List, Optional

class Product(BaseModel):
    name: str = Field(description="Product name")
    price: float = Field(description="Price in USD", ge=0)
    category: str = Field(description="Product category")
    in_stock: bool = Field(description="Whether currently in stock")
    tags: List[str] = Field(default_factory=list, description="Search tags")

# 生成 JSON Schema
schema = Product.model_json_schema()
print(schema)
```

**输出 Schema：**
```json
{
  "title": "Product",
  "type": "object",
  "properties": {
    "name": {"type": "string", "description": "Product name"},
    "price": {"type": "number", "description": "Price in USD", "minimum": 0},
    "category": {"type": "string", "description": "Product category"},
    "in_stock": {"type": "boolean", "description": "Whether currently in stock"},
    "tags": {
      "type": "array",
      "items": {"type": "string"},
      "description": "Search tags"
    }
  },
  "required": ["name", "price", "category", "in_stock"]
}
```

### 2.2 复杂嵌套 Schema

```python
class OrderItem(BaseModel):
    product: Product
    quantity: int = Field(ge=1, description="Quantity ordered")

class Order(BaseModel):
    order_id: str = Field(description="Unique order identifier")
    customer_email: str = Field(description="Customer email address")
    items: List[OrderItem]
    total: float = Field(ge=0, description="Order total in USD")
    status: str = Field(
        default="pending",
        enum=["pending", "confirmed", "shipped", "delivered"]
    )

# Hermes 能正确处理嵌套对象、数组、枚举等复杂结构
```

## 3. 完整使用示例

### 3.1 Python 推理

```python
from transformers import AutoTokenizer, LlamaForCausalLM
from pydantic import BaseModel
import torch
import json

# ---- Schema 定义 ----
class MovieReview(BaseModel):
    title: str
    year: int
    rating: float
    summary: str
    pros: list[str]
    cons: list[str]
    recommended: bool

schema_json = json.dumps(MovieReview.model_json_schema(), indent=2)

# ---- 模型加载 ----
tokenizer = AutoTokenizer.from_pretrained("NousResearch/Hermes-3-Llama-3.1-8B")
model = LlamaForCausalLM.from_pretrained(
    "NousResearch/Hermes-3-Llama-3.1-8B",
    torch_dtype=torch.float16,
    device_map="auto",
    load_in_4bit=True
)

# ---- 构建 Prompt ----
messages = [
    {
        "role": "system",
        "content": f"""You are a helpful assistant that answers in JSON.
Here's the json schema you must adhere to:
<schema>
{schema_json}
</schema>"""
    },
    {
        "role": "user",
        "content": "Review the movie 'Inception' directed by Christopher Nolan (2010). Rate it 9.0/10."
    }
]

inputs = tokenizer.apply_chat_template(messages, return_tensors="pt",
                                        add_generation_prompt=True).to("cuda")

outputs = model.generate(inputs, max_new_tokens=500, temperature=0.3, do_sample=True)

response = tokenizer.decode(outputs[0][inputs.shape[-1]:], skip_special_tokens=True)

# ---- 解析并验证 ----
review = MovieReview.model_validate_json(response)
print(f"{review.title} ({review.year}): {review.rating}/10")
print(f"推荐: {'是' if review.recommended else '否'}")
```

### 3.2 使用 Hermes 官方工具

```bash
# Hermes-Function-Calling 仓库中的 jsonmode.py
python jsonmode.py \
  --model NousResearch/Hermes-3-Llama-3.1-8B \
  --prompt "Extract all person names and their roles from this text: ..."
```

### 3.3 vLLM 部署 JSON Mode

```bash
# vLLM 服务器端
python -m vllm.entrypoints.openai.api_server \
  --model NousResearch/Hermes-3-Llama-3.1-8B \
  --tool-call-parser hermes \
  --enable-auto-tool-choice

# 客户端调用
curl http://localhost:8000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "NousResearch/Hermes-3-Llama-3.1-8B",
    "messages": [
      {"role": "system", "content": "You are a helpful assistant that answers in JSON.\n<schema>\n...\n</schema>"},
      {"role": "user", "content": "..."}
    ],
    "response_format": {"type": "json_object"}
  }'
```

## 4. 实战场景

| 场景 | Schema 示例 | 应用 |
|------|------------|------|
| 实体抽取 | `{entities: [{name, type, confidence}]}` | NER 替代方案 |
| 文本分类 | `{category, confidence, reasoning}` | 自动打标签 |
| 情感分析 | `{sentiment, score, keywords}` | 舆情监控 |
| 数据提取 | `{invoice: {number, date, items, total}}` | PDF 发票解析 |
| 代码分析 | `{language, functions, dependencies, complexity}` | 代码审查 |
| API 响应标准化 | `{code, data, message}` | 统一 API 格式 |

> 🎯 **最佳场景**：当你需要将 LLM 的输出直接传给下游代码处理，且不允许格式错误时，JSON Mode 是最优解

## 5. 与 Function Calling 的区别

| 维度 | JSON Mode | Function Calling |
|------|-----------|-----------------|
| 目标 | 模型输出结构化数据 | 模型**主动调用**外部工具 |
| 模型角色 | 数据生成器 | 工具编排器 |
| 是否需要执行函数 | ❌ | ✅ |
| 典型用途 | 分类/抽取/摘要结构化 | 查天气/发邮件/操作数据库 |
| System Prompt 标签 | `<schema>` | `<tools>` |
| 适用场景 | 下游代码直接消费 | Agent 式多步任务执行 |

```text
选择指南：
├── 需要模型输出固定格式数据 → JSON Mode
├── 需要模型主动调用外部 API/工具 → Function Calling
└── 两者都需要 → 配合使用（见 03-文档）
```

## 核心要点回顾

- JSON Mode = System Prompt 注入 `<schema>` XML 标签 → 模型强制输出匹配 Schema 的 JSON
- Pydantic `model_json_schema()` 一键生成 Schema
- `temperature=0.3` 推荐值，提高 JSON 稳定性
- vLLM 支持 `response_format: {"type": "json_object"}` 增强约束
- JSON Mode vs Function Calling：前者是"输出格式约束"，后者是"工具调用编排"

## 参考资料

1. Nous Research - Hermes-Function-Calling GitHub 仓库（`jsonmode.py`）
2. Pydantic V2 官方文档 - JSON Schema 生成
3. vLLM 官方文档 - Structured Outputs
