# AI Agent 工具系统设计（Java 后端 + AI 全栈实战版）

> **文档定位**：AI Agent 核心技术文档 | 工具系统设计最佳实践
> **核心问题**：如何设计 Agent 的工具？如何管理工具调用？如何处理错误？

---

## 一、工具系统架构

```
Agent 大脑（LLM）
    ↓ 生成 Function Call (JSON)
┌───────────────────────┐
│    Tool Registry       │  ← 工具注册中心
│  ┌─────┬─────┬─────┐  │
│  │搜索 │数据库│邮件 │  │
│  └─────┴─────┴─────┘  │
└───────────┬───────────┘
            ↓
┌───────────────────────┐
│   Tool Executor        │  ← 执行引擎
│  - 参数校验            │
│  - 权限检查            │
│  - 超时控制            │
│  - 错误处理            │
└───────────┬───────────┘
            ↓
   实际系统（API / DB / FS）
```

---

## 二、工具定义规范

### 2.1 标准 Tool Schema

```python
from dataclasses import dataclass
from typing import Any, Callable, Optional

@dataclass
class ToolDefinition:
    name: str                          # 工具名（LLM 用于识别）
    description: str                   # 描述（帮助 LLM 判断何时用）
    parameters: dict                   # JSON Schema 参数定义
    handler: Callable                  # 实际执行函数
    dangerous: bool = False            # 是否需要确认
    timeout: int = 30                  # 超时秒数
    max_retries: int = 1               # 失败重试次数
```

### 2.2 示例：搜索工具

```python
search_tool = ToolDefinition(
    name="web_search",
    description="搜索互联网获取最新信息。当需要查询实时数据、新闻或未知知识时使用",
    parameters={
        "type": "object",
        "properties": {
            "query": {
                "type": "string",
                "description": "搜索关键词"
            },
            "max_results": {
                "type": "integer",
                "default": 5,
                "description": "返回结果数量"
            }
        },
        "required": ["query"]
    },
    handler=lambda query, max_results=5: search_engine.search(query, limit=max_results),
    timeout=15
)
```

### 2.3 危险的写操作工具

```python
delete_user_tool = ToolDefinition(
    name="delete_user",
    description="删除指定用户账号，不可逆操作",
    parameters={
        "type": "object",
        "properties": {
            "user_id": {"type": "string"},
            "confirmation": {
                "type": "boolean",
                "description": "必须为 true 才执行删除"
            }
        },
        "required": ["user_id", "confirmation"]
    },
    handler=lambda user_id, confirmation: delete_user(user_id),
    dangerous=True,          # 标记危险
)
```

---

## 三、Tool Registry（工具注册中心）

```python
class ToolRegistry:
    def __init__(self):
        self._tools: dict[str, ToolDefinition] = {}
    
    def register(self, tool: ToolDefinition):
        """注册工具"""
        self._tools[tool.name] = tool
    
    def get(self, name: str) -> Optional[ToolDefinition]:
        """获取工具定义"""
        return self._tools.get(name)
    
    def get_all_schemas(self) -> list[dict]:
        """生成 LLM 的 tools 参数列表"""
        return [
            {
                "type": "function",
                "function": {
                    "name": t.name,
                    "description": t.description,
                    "parameters": t.parameters
                }
            }
            for t in self._tools.values()
        ]
    
    def get_tools_for_context(self) -> str:
        """生成人类可读的工具说明（注入 Prompt）"""
        lines = ["可用工具列表："]
        for t in self._tools.values():
            danger_tag = "⚠️ 危险操作 " if t.dangerous else ""
            lines.append(f"- **{t.name}**: {danger_tag}{t.description}")
        return "\n".join(lines)
```

---

## 四、Tool Executor（执行引擎）

```python
import asyncio
import signal

class ToolExecutor:
    def __init__(self, registry: ToolRegistry):
        self.registry = registry
    
    async def execute(self, name: str, args: dict, 
                      confirm_callback=None) -> dict:
        tool = self.registry.get(name)
        if not tool:
            return {"error": f"未知工具: {name}"}
        
        # 1. 危险操作确认
        if tool.dangerous and confirm_callback:
            confirmed = await confirm_callback(tool, args)
            if not confirmed:
                return {"error": "用户取消了该危险操作"}
        
        # 2. 参数校验
        validated = self._validate_args(tool, args)
        if "error" in validated:
            return validated
        
        # 3. 执行（带超时）
        try:
            result = await asyncio.wait_for(
                self._run_with_retry(tool, validated["args"]),
                timeout=tool.timeout
            )
            return {"success": True, "result": result}
        
        except asyncio.TimeoutError:
            return {"error": f"工具 {name} 执行超时（{tool.timeout}s）"}
        
        except Exception as e:
            return {"error": f"工具 {name} 执行失败: {str(e)}"}
    
    async def _run_with_retry(self, tool, args):
        last_error = None
        for attempt in range(tool.max_retries + 1):
            try:
                return await tool.handler(**args)
            except Exception as e:
                last_error = e
                if attempt < tool.max_retries:
                    await asyncio.sleep(1)  # 重试前等待
        raise last_error
    
    def _validate_args(self, tool, args):
        # JSON Schema 校验
        try:
            jsonschema.validate(args, tool.parameters)
            return {"args": args}
        except jsonschema.ValidationError as e:
            return {"error": f"参数校验失败: {e.message}"}
```

---

## 五、工具设计原则

| 原则 | 说明 | 坏例 | 好例 |
|---|---|---|---|
| **单一职责** | 一个工具只做一件事 | `do_everything()` | `get_weather(city)` |
| **好描述** | LLM 靠描述判断何时用 | `查询` | `搜索互联网获取最新信息` |
| **明确参数** | 参数含义清晰、类型严格 | `data: string` | `city: string, limit: int` |
| **可重试** | 失败可安全重试 | 非幂等写入 | 查询/搜索 |
| **返回结构化** | 返回 JSON 而非纯文本 | `"找到了 xxx"` | `{"items": [...], "total": 10}` |

### 5.1 返回格式最佳实践

```python
# ✅ 结构化返回（LLM 能理解）
def search_products(query, min_price=None, max_price=None):
    results = db.search(query, min_price, max_price)
    return {
        "items": [
            {"id": r.id, "name": r.name, "price": r.price}
            for r in results
        ],
        "total": len(results),
        "query": query
    }

# ❌ 纯文本返回（LLM 难解析）
def search_products(query, **kwargs):
    results = db.search(query, **kwargs)
    return f"找到了 {len(results)} 个产品：" + ", ".join(r.name for r in results)
```

---

## 六、工具错误处理体系

```python
# 标准化错误响应
def tool_error(code: str, message: str, retryable: bool = False) -> dict:
    return {
        "success": False,
        "error": {
            "code": code,
            "message": message,
            "retryable": retryable
        }
    }

# 错误码定义
class ErrorCode:
    TIMEOUT = "TIMEOUT"         # 超时，可重试
    RATE_LIMITED = "RATE_LIMIT"  # 限流，等一会可重试
    UNAUTHORIZED = "AUTH"       # 未授权，不可重试
    INVALID_PARAMS = "INVALID"  # 参数错误，调参数后重试
    INTERNAL = "INTERNAL"       # 内部错误，可能重试
```

---

## 七、工具组合模式

```
1. 流水线（Pipeline）：A → B → C
   get_user_id(email) → get_user_orders(user_id) → format_report(orders)

2. 并行（Parallel）：[A, B, C] 同时执行
   [get_weather("北京"), get_weather("上海"), get_weather("广州")]

3. 条件（Conditional）：if A → B else → C
   查库存 → 有货 → 创建订单
          → 缺货 → 通知补货

4. 回退（Fallback）：A → 失败 → B
   查 Redis 缓存 → Miss → 查 MySQL
```

---

## 八、面试核心要点

1. **工具定义包含什么？** name + description + parameters(JSON Schema) + handler
2. **工具描述为什么重要？** LLM 靠描述判断何时调用这个工具
3. **危险操作怎么处理？** `dangerous=true` 标记 + 用户确认回调 + 二次确认参数
4. **工具执行失败怎么办？** 返回结构化错误（code + message + retryable），LLM 根据错误重试或换方案
5. **为什么返回要结构化？** LLM 解析 JSON 比解析自然语言准确得多

---

## 九、极简总结

```
Tool = name + description + JSON Schema + handler
Registry = 工具注册中心，生成 LLM tools 参数
Executor = 参数校验 + 权限检查 + 超时控制 + 错误处理
好工具 = 单一职责 + 描述清晰 + 结构化返回 + 幂等可重试
```
