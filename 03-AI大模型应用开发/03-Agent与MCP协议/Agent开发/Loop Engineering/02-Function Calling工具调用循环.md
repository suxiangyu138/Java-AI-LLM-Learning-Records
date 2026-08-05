# 02 - Function Calling 工具调用循环

> 🎯 Function Calling 循环是 ReAct 模式中"Act"环节的工程实现。本章聚焦工具注册、调用、回填、递归的全流程，以及多工具并行、错误回退的生产级实践

---

## 目录

1. [工具调用循环全流程](#1-工具调用循环全流程)
2. [单工具 vs 多工具调用](#2-单工具-vs-多工具调用)
3. [递归与根因分析](#3-递归与根因分析)
4. [生产级错误处理](#4-生产级错误处理)

---

## 1. 工具调用循环全流程

```text
完整的工具调用生命周期：

1. 注册 → 2. 选择 → 3. 调用 → 4. 执行 → 5. 回填 → 6. 验证 → 7. (递归)
──┬───   ──┬───   ──┬───   ──┬───   ──┬───   ──┬───   ──┬───
  系统      LLM      LLM      执行器    系统      系统      LLM
```

### 1.1 工具注册

```python
class ToolRegistry:
    def __init__(self):
        self._tools: dict[str, Callable] = {}

    def register(self, name: str, description: str,
                 parameters: dict, handler: Callable):
        self._tools[name] = {
            "handler": handler,
            "schema": {
                "type": "function",
                "function": {
                    "name": name,
                    "description": description,
                    "parameters": parameters
                }
            }
        }

    def get_schemas(self) -> list:
        return [t["schema"] for t in self._tools.values()]

    def execute(self, name: str, args: dict) -> dict:
        if name not in self._tools:
            return {"error": f"Unknown tool: {name}"}
        try:
            return {"result": self._tools[name]["handler"](**args)}
        except Exception as e:
            return {"error": str(e), "retryable": isinstance(e, TimeoutError)}
```

### 1.2 工具 Schema 设计原则

```text
好的 Schema：
✅ description 写清楚"做什么"和"什么时候用"
✅ parameters 完整列出所有参数 + 类型 + 约束
✅ required 字段明确标注

差的 Schema：
❌ "查询数据"（太模糊，LLM 不知道该不该用）
❌ 参数只有名字没描述（LLM 会填错）
❌ 不标注 required（LLM 会遗漏必需参数）
```

## 2. 单工具 vs 多工具调用

### 2.1 单工具串行（默认模式）

```text
最简单的工具循环：一次只调用一个工具

User: "北京天气怎么样？"
  Step 1: get_weather("北京") → 22°C, 晴
  Answer: "北京今天晴，22°C"
```

### 2.2 多工具并行

```text
工具间无依赖 → 并行调用

User: "比较北京和上海的天气"
  Step 1:
    ├── get_weather("北京") ─┐
    └── get_weather("上海") ─┤ 并行
                              ↓
  Answer: "北京晴22°C，上海多云25°C"

关键判断：
  → 工具 A 的输出 ≠ 工具 B 的输入 → 可并行 ✅
  → 工具 A 的输出 = 工具 B 的输入 → 必须串行 ❌
```

### 2.3 选择矩阵

| 场景 | 策略 | 延迟 |
|------|------|:---:|
| 无依赖 | 并行 | T_max(single) |
| 有依赖 | 串行 | ΣT_all |
| 不确定 | 乐观并行 → 失败回退串行 | 自适应 |

## 3. 递归与根因分析

### 3.1 递归深度控制

```python
class RecursiveToolLoop:
    def __init__(self, llm, registry, max_depth=5):
        self.llm = llm
        self.registry = registry
        self.max_depth = max_depth

    def execute(self, query: str) -> str:
        messages = [{"role": "user", "content": query}]

        for depth in range(self.max_depth):
            response = self.llm.chat(
                messages,
                tools=self.registry.get_schemas()
            )

            # 终止条件
            if not response.tool_calls:
                return response.content

            # 深度警告
            if depth >= 3:
                messages.append({
                    "role": "system",
                    "content": "注意：已调用 {} 次工具。如果信息足够，请直接给出答案。".format(depth + 1)
                })

            # 执行工具
            for tc in response.tool_calls:
                result = self.registry.execute(tc.name, tc.arguments)
                messages = self._append(messages, tc, result)

        return "达到最大递归深度 {}".format(self.max_depth)
```

### 3.2 递归模式分类

| 模式 | 特征 | 示例 |
|------|------|------|
| **链式** | A → B → C（严格顺序） | 查用户ID → 查订单 → 查物流 |
| **树形** | A → (B, C) → D（分支汇聚） | 同时查不同数据源 → 综合 |
| **迭代** | A → A → A（重复调用） | 分页获取所有数据 |
| **条件** | A → if X then B else C | 查价格 → 如果有折扣则计算 |

## 4. 生产级错误处理

### 4.1 工具执行失败处理

```python
class ResilientToolExecutor:
    def execute_with_retry(self, name, args, max_retries=3):
        last_error = None

        for attempt in range(max_retries):
            try:
                result = self.registry.execute(name, args)

                # 检查业务错误
                if "error" in result:
                    if result.get("retryable", False):
                        time.sleep(2 ** attempt)
                        continue
                    return result  # 不可重试，直接返回

                return result

            except TimeoutError:
                time.sleep(2 ** attempt)
                last_error = "Timeout"

        return {"error": f"Failed after {max_retries} retries: {last_error}"}
```

### 4.2 降级策略

```text
工具调用失败的降级链：

主工具 get_order_detail(id)
  ↓ 失败
备用工具 get_order_summary(id)    ← 返回更少字段
  ↓ 失败
兜底查询 search_orders("id:" + id)  ← 最基础查询
  ↓ 失败
Answer: "抱歉，暂时无法查询该订单，请稍后重试"
```

### 4.3 并行调用的部分失败

```python
def execute_parallel_with_fallback(tool_calls):
    results = []
    failed = []

    # 并行执行
    with ThreadPoolExecutor() as executor:
        futures = {
            executor.submit(execute_tool, tc): tc
            for tc in tool_calls
        }
        for future in as_completed(futures):
            tc = futures[future]
            try:
                results.append((tc, future.result()))
            except Exception:
                failed.append(tc)

    # 失败的工具串行重试
    for tc in failed:
        try:
            results.append((tc, execute_tool(tc)))
        except Exception:
            results.append((tc, {"error": "unavailable"}))

    return results
```

## 核心要点回顾

- 工具循环 = 注册 → 选择(LLM) → 执行(代码) → 回填 → 递归
- 并行条件：工具间无依赖；串行条件：B 需要 A 的输出
- 递归深度控制：max_depth=5 + 深度警告 + Token 预算
- 错误处理：重试(3次 指数退避) → 降级(备选工具) → 兜底(抱歉)
- 部分失败不要整体放弃——并行执行中 1 个失败不影响其他

## 参考资料

1. OpenAI Function Calling 文档
2. Anthropic Tool Use 文档
3. Google Gemini Function Calling
