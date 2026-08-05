# 09 - 类型提示与 Pydantic

> 🎯 类型提示是 Python 3.5+ 最重要的工程化进步——从"全凭记忆"到"IDE 自动补全"，从"运行时才知道类型错误"到"静态检查"。Pydantic 将类型提示变成了数据校验引擎，是 FastAPI、LangChain 的基础

---

## 目录

1. [类型提示基础](#1-类型提示基础)
2. [高级类型](#2-高级类型)
3. [Pydantic 数据校验](#3-pydantic-数据校验)

---

## 1. 类型提示基础

```python
# 基本类型
def greet(name: str, age: int) -> str:
    return f"{name} is {age} years old"

# 容器类型 (Python 3.9+)
def process(items: list[str], scores: dict[str, float]) -> tuple[int, str]:
    return len(items), items[0]

# Optional = 可以是 None
from typing import Optional
def find_user(id: int) -> Optional[User]:
    return db.get(id)  # 可能返回 None

# Union = 多种类型之一
from typing import Union
def parse(value: Union[str, int]) -> float:
    return float(value)

# 现代写法 (Python 3.10+): str | int 代替 Union[str, int]
def parse(value: str | int) -> float:
    return float(value)
```

## 2. 高级类型

```python
from typing import Protocol, TypeVar, Generic, Literal, TypedDict

# Protocol: 结构化子类型（类似 Go interface）
class Embeddable(Protocol):
    def embed(self) -> list[float]: ...

def compute_similarity(a: Embeddable, b: Embeddable) -> float: ...

# TypeVar: 泛型
T = TypeVar('T')
def first(items: list[T]) -> T | None:
    return items[0] if items else None

# Literal: 限定值的范围
def set_mode(mode: Literal["train", "eval", "inference"]) -> None: ...

# TypedDict: 类型化的 dict
class ModelConfig(TypedDict):
    hidden_size: int
    num_layers: int
    dropout: float
```

## 3. Pydantic 数据校验

```python
from pydantic import BaseModel, Field, validator

class LLMRequest(BaseModel):
    prompt: str = Field(..., min_length=1, max_length=8000)
    model: Literal["gpt-4o", "claude-sonnet"] = "gpt-4o"
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)
    max_tokens: int = Field(default=1024, gt=0, le=32768)

    @validator("prompt")
    def prompt_not_empty(cls, v):
        if not v.strip():
            raise ValueError("prompt cannot be blank")
        return v

# 自动校验 + JSON Schema 生成
request = LLMRequest(prompt="Hello", temperature=1.5)
print(request.model_dump())            # dict
print(request.model_dump_json())       # JSON string
print(LLMRequest.model_json_schema())  # JSON Schema（给 LLM 的 Function Calling！）

# 从 LLM 响应解析（自动校验）
response_text = '{"prompt": "Hi", "model": "gpt-4o"}'
parsed = LLMRequest.model_validate_json(response_text)  # 自动类型转换+校验
```

### AI 开发中的应用

```python
# 1. Function Calling Schema → Pydantic 自动生成
class WeatherQuery(BaseModel):
    city: str = Field(description="城市名")
    date: str | None = Field(default=None, description="日期 YYYY-MM-DD")

# .model_json_schema() → 直接喂给 LLM 的 tools 参数！

# 2. 配置文件校验
class AppConfig(BaseModel):
    openai_api_key: str
    model_name: str = "gpt-4o"
    max_concurrency: int = Field(default=10, ge=1, le=100)

config = AppConfig(**yaml.safe_load(open("config.yaml")))

# 3. API 响应校验（FastAPI + Pydantic）
from fastapi import FastAPI
app = FastAPI()

@app.post("/chat")
async def chat(request: LLMRequest) -> ChatResponse:  # 自动校验！
    result = await llm.generate(request.prompt)
    return ChatResponse(content=result)
```

## 核心要点回顾

- 类型提示 = Python 3.5+ 标配，提高可读性 + IDE 支持
- `Protocol` = Go-style 接口，`Literal` = 限定值集合
- Pydantic = 运行时数据校验 + JSON Schema 自动生成
- AI 最常用：Pydantic Model → `model_json_schema()` → Function Calling
- Pydantic v2 用 `model_validate_json`（不是 `parse_raw`）

## 参考资料

1. Python typing 官方文档
2. Pydantic v2 文档 — docs.pydantic.dev
