# 09 - Function Calling 与 Tool Use 深度实战

> 🎯 Function Calling 是 LLM API 开发中最重要但也最容易出错的部分。不同平台的格式、限制、最佳实践各不相同。本文提供跨平台（OpenAI/Claude/Hermes/国产）的统一 Function Calling 封装方案、递归调用模式和 Agent 基础架构

> **前置阅读**：[[04-快速精通GPT]]、[[06-快速精通Claude API]]、[[02-大模型API调用实践]]

---

## 目录

1. [各平台 FC 格式对比](#1-各平台-fc-格式对比)
2. [统一 FC 封装层](#2-统一-fc-封装层)
3. [递归调用模式](#3-递归调用模式)
4. [并行工具调用](#4-并行工具调用)
5. [错误处理与降级](#5-错误处理与降级)
6. [生产级 Agent 骨架](#6-生产级-agent-骨架)

---

## 1. 各平台 FC 格式对比

| 维度 | OpenAI | Claude | Hermes 3 | DeepSeek | Qwen/GLM |
|------|--------|--------|----------|----------|----------|
| 工具定义 | `tools` 参数 | `tools` 参数 | `<tools>` XML | `tools` 参数 | `tools` 参数 |
| 调用格式 | `tool_calls` JSON | `tool_use` block | `<tool_call>` XML | `tool_calls` JSON | `tool_calls` JSON |
| 结果回填 | `role: "tool"` | `tool_result` | `<tool_response>` | `role: "tool"` | `role: "tool"` |
| 并行调用 | ✅ 原生 | ✅ 原生 | ✅ 数组 | ✅ 原生 | ⚠️ 有限 |
| 强制调用 | `tool_choice` | ❌ 仅建议 | ❌ 仅建议 | `tool_choice` | `tool_choice` |
| 兼容 OpenAI | ✅ 原生 | ❌ 需适配 | ⚠️ vLLM 解析 | ✅ 完全兼容 | ✅ 完全兼容 |

> 🎯 **核心思路**：以 OpenAI 格式为统一标准，非标准平台（Claude/Hermes）在适配层做转换

## 2. 统一 FC 封装层

### 2.1 架构设计

```text
┌─────────────────────────────────────────┐
│         应用层 (Agent / Service)          │
│    tool_registry.call("get_weather", {}) │
└───────────────────┬─────────────────────┘
                    │
┌───────────────────▼─────────────────────┐
│         Unified FC Adapter               │
│   ┌─────────────────────────────────┐   │
│   │  OpenAI 格式 (内部标准)           │   │
│   └────────┬────────┬────────┬──────┘   │
│   ┌────────▼──┐ ┌───▼────┐ ┌─▼───────┐  │
│   │ OpenAI    │ │Claude  │ │Hermes   │  │
│   │ Adapter   │ │Adapter │ │Adapter  │  │
│   └───────────┘ └────────┘ └─────────┘  │
└─────────────────────────────────────────┘
```

### 2.2 核心实现

```python
from abc import ABC, abstractmethod
from typing import Any, Callable
import json

# ====== 工具注册表 ======
class ToolRegistry:
    """统一的工具注册表——所有平台共用"""

    def __init__(self):
        self._tools: dict[str, Callable] = {}
        self._schemas: list[dict] = []

    def register(self, name: str, description: str,
                 parameters: dict, handler: Callable):
        self._tools[name] = handler
        self._schemas.append({
            "type": "function",
            "function": {
                "name": name,
                "description": description,
                "parameters": parameters
            }
        })

    def execute(self, name: str, arguments: dict) -> Any:
        if name not in self._tools:
            return {"error": f"Unknown tool: {name}"}
        try:
            return self._tools[name](**arguments)
        except Exception as e:
            return {"error": str(e)}

    def get_openai_schemas(self) -> list[dict]:
        return self._schemas


# ====== 平台适配器 ======
class BaseFCAdapter(ABC):
    """FC 适配器基类"""

    @abstractmethod
    def format_request(self, messages, tools):
        """将 OpenAI 格式的消息+工具 → 平台特定格式"""
        pass

    @abstractmethod
    def parse_response(self, response) -> list[dict]:
        """将平台响应 → 统一格式: [{"name": "", "arguments": {}}, ...]"""
        pass

    @abstractmethod
    def append_tool_results(self, messages, tool_results):
        """追加工具执行结果到消息列表"""
        pass


class OpenAIFCAdapter(BaseFCAdapter):
    """OpenAI 兼容平台适配器 (GPT/DeepSeek/Qwen/GLM)"""

    def format_request(self, messages, tools):
        return {"messages": messages, "tools": tools}

    def parse_response(self, response) -> list[dict]:
        tool_calls = response.choices[0].message.tool_calls
        if not tool_calls:
            return []
        return [
            {"name": tc.function.name, "arguments": json.loads(tc.function.arguments)}
            for tc in tool_calls
        ]

    def append_tool_results(self, messages, tool_results):
        for tr in tool_results:
            messages.append({
                "role": "tool",
                "tool_call_id": tr["id"],
                "content": json.dumps(tr["result"], ensure_ascii=False)
            })
        return messages


class ClaudeFCAdapter(BaseFCAdapter):
    """Claude Tool Use 适配器"""

    def format_request(self, messages, tools):
        # Claude 的 tools 格式与 OpenAI 不同
        claude_tools = []
        for t in tools:
            claude_tools.append({
                "name": t["function"]["name"],
                "description": t["function"]["description"],
                "input_schema": t["function"]["parameters"]
            })
        return {"messages": messages, "tools": claude_tools}

    def parse_response(self, response) -> list[dict]:
        tool_uses = []
        for block in response.content:
            if block.type == "tool_use":
                tool_uses.append({
                    "name": block.name,
                    "arguments": block.input,
                    "id": block.id
                })
        return tool_uses

    def append_tool_results(self, messages, tool_results):
        for tr in tool_results:
            messages.append({
                "role": "user",
                "content": [{
                    "type": "tool_result",
                    "tool_use_id": tr["id"],
                    "content": json.dumps(tr["result"], ensure_ascii=False)
                }]
            })
        return messages
```

## 3. 递归调用模式

### 3.1 核心循环

```python
class FCAgent:
    """FC Agent — 自动递归调用直到任务完成"""

    def __init__(self, llm_client, adapter: BaseFCAdapter,
                 registry: ToolRegistry, max_rounds=5):
        self.client = llm_client
        self.adapter = adapter
        self.registry = registry
        self.max_rounds = max_rounds

    def run(self, user_query: str, system_prompt: str = "") -> str:
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": user_query})

        for round_num in range(self.max_rounds):
            # 1. 准备请求
            request = self.adapter.format_request(
                messages,
                self.registry.get_openai_schemas()
            )

            # 2. 调用 LLM
            response = self._call_llm(request)

            # 3. 解析工具调用
            tool_calls = self.adapter.parse_response(response)
            if not tool_calls:
                return self._extract_text(response)  # 最终答案

            # 4. 执行工具
            tool_results = []
            for tc in tool_calls:
                result = self.registry.execute(tc["name"], tc["arguments"])
                tool_results.append({
                    "name": tc["name"],
                    "id": tc.get("id", str(round_num)),
                    "result": result
                })

            # 5. 回填结果 → 继续循环
            messages = self.adapter.append_tool_results(messages, tool_results)

        return "Max tool call rounds exceeded"

    def _call_llm(self, request):
        # 子类实现：调用具体的 LLM
        raise NotImplementedError

    def _extract_text(self, response) -> str:
        # 子类实现：提取文本内容
        raise NotImplementedError
```

### 3.2 使用示例

```python
# 注册工具
registry = ToolRegistry()
registry.register(
    name="get_weather",
    description="Get current weather for a city",
    parameters={
        "type": "object",
        "properties": {
            "city": {"type": "string", "description": "City name"}
        },
        "required": ["city"]
    },
    handler=lambda city: {"city": city, "temp": 22, "condition": "Sunny"}
)

# 创建 Agent
agent = FCGPTAgent(
    api_key="sk-xxx",
    model="gpt-4o",
    registry=registry
)

# 一行调用
answer = agent.run("What's the weather in Beijing?")
print(answer)  # → "北京当前天气晴朗，气温22°C"
```

## 4. 并行工具调用

```python
# 支持并行的 Agent（OpenAI/Claude 原生支持）

def execute_parallel(tool_calls: list[dict], registry: ToolRegistry):
    """并行执行多个独立的工具调用"""
    import concurrent.futures

    with concurrent.futures.ThreadPoolExecutor() as executor:
        futures = {
            executor.submit(registry.execute, tc["name"], tc["arguments"]): tc
            for tc in tool_calls
        }
        results = []
        for future in concurrent.futures.as_completed(futures):
            tc = futures[future]
            results.append({
                "name": tc["name"],
                "id": tc.get("id"),
                "result": future.result()
            })
    return results
```

## 5. 错误处理与降级

```python
class RobustFCAgent(FCAgent):
    """带错误处理的 FC Agent"""

    def run(self, user_query: str, system_prompt: str = "") -> str:
        try:
            return super().run(user_query, system_prompt)
        except Exception as e:
            # 3 次重试
            for attempt in range(3):
                try:
                    return super().run(user_query, system_prompt)
                except Exception:
                    time.sleep(2 ** attempt)  # 指数退避
            # 最终降级：不用工具，直接回答
            return self._fallback_chat(user_query)

    def _fallback_chat(self, query: str) -> str:
        """降级方案：直接 LLM 回答（不使用工具）"""
        return self.client.chat.completions.create(
            model=self.model,
            messages=[{"role": "user", "content": query}]
        ).choices[0].message.content
```

## 6. 生产级 Agent 骨架

```java
// Java Spring Boot 生产级 Agent 骨架
@Service
public class ProductionFCAgent {

    private final Map<String, Function<Map<String,Object>, Object>> tools = new HashMap<>();
    private final RestClient llmClient;

    public ProductionFCAgent(RestClient llmClient) {
        this.llmClient = llmClient;
    }

    // 工具注册
    public void registerTool(String name, String description,
                              String schema, Function<Map<String,Object>, Object> handler) {
        tools.put(name, handler);
        // schema 存储到工具注册表...
    }

    // Agent 执行
    public AgentResponse execute(String userQuery, int maxRounds) {
        var messages = new ArrayList<>(List.of(
            Map.of("role", "user", "content", userQuery)
        ));

        for (int round = 0; round < maxRounds; round++) {
            var response = callLLM(messages);
            var toolCalls = extractToolCalls(response);

            if (toolCalls.isEmpty()) {
                return new AgentResponse(response, round + 1);
            }

            for (var tc : toolCalls) {
                var result = tools.get(tc.name()).apply(tc.arguments());
                messages.add(Map.of("role", "tool",
                    "tool_call_id", tc.id(),
                    "content", toJson(result)));
            }
        }
        return new AgentResponse("Max rounds exceeded", maxRounds);
    }

    record AgentResponse(String content, int roundsUsed) {}
}
```

## 核心要点回顾

- 统一 FC 适配层：以 OpenAI 格式为内部标准，各平台做转换
- ToolRegistry = 工具注册 + Schema 生成 + 执行调度
- 递归调用 = 调用 → 解析 → 执行 → 回填 → 循环（直到无 tool_call）
- 并行执行：`ThreadPoolExecutor` 并行调用独立工具
- 错误处理：重试 3 次 → 降级到无工具对话
- 生产级：工具注册表 + 超时控制 + 监控埋点 + 降级方案

## 参考资料

1. OpenAI Function Calling 文档
2. Anthropic Tool Use 文档
3. Nous Research Hermes Function Calling
4. Spring AI Function Calling 源码
