# AI Agent 评估与可观测性

> **核心摘要**：Agent 的非确定性输出使得传统测试方法难以适用，需要建立专门的评估体系和可观测性方案。本文系统讲解 Agent 评估框架（结果评估、过程评估、工具调用评估、效率评估、安全评估）、LLM-as-Judge 方法论、全链路追踪技术和生产监控面板的设计。

## 前置阅读

- [[AI Agent核心知识点]]
- [[AI Agent 测试策略]]

---

## 一、Agent 评估的特殊挑战

```
传统软件测试：输入 → 确定输出 → 对比即可
Agent 测试：输入 → 中间推理（不可控） → 可能多种正确输出 → 难以机械对比
```

| 挑战 | 说明 |
|---|---|
| **非确定性** | 同样问题，每次回答可能不同 |
| **过程不可见** | 中间推理步骤默认不记录 |
| **标准模糊** | "正确"难以精确定义 |

---

## 二、评估框架

```
Agent 评估
├── 最终结果评估（Did it answer correctly?）
├── 过程评估（Did it reason correctly?）
├── 工具调用评估（Did it use the right tools?）
├── 效率评估（Did it get there efficiently?）
└── 安全评估（Did it stay within bounds?）
```

---

## 三、评估指标

### 3.1 核心指标

| 指标 | 含义 | 计算方式 |
|---|---|---|
| **任务完成率** | 是否完成了用户任务 | 人工评分 / LLM-as-Judge |
| **单次成功率** | 不需要重试就成功 | 成功数 / 总任务数 |
| **工具选择准确率** | 是否选择了正确的工具 | 正确选工具的任务 / 总任务 |
| **平均步数** | 完成任务用了多少步 | 总步数 / 总任务 |
| **平均耗时** | 完成任务用了多长时间 | 总耗时 / 总任务 |
| **Token 消耗** | 每个任务消耗多少 Token | 总 Token / 总任务 |
| **安全违规率** | 是否执行了危险操作 | 违规数 / 总操作数 |

### 3.2 错误类型统计

```
Agent 失败原因分类：
├── 工具选择错误：选了不该选的工具（30%）
├── 工具参数错误：参数不对导致调用失败（20%）
├── 循环超限：在多个工具间反复循环（15%）
├── 任务分解错误：把任务拆错了（15%）
├── 过早终止：提前结束认为完成了（10%）
└── 幻觉：推理出不存在的内容（10%）
```

---

## 四、LLM-as-Judge

### 4.1 原理

用更强的 LLM（裁判模型）来评估 Agent 的输出质量：

```python
def evaluate_response(user_query, agent_response, expected_info):
    judge_prompt = f"""
    你是一个专业的 AI 评估员。请评估以下 Agent 回答的质量。

    用户问题：{user_query}
    Agent 回答：{agent_response}
    期望包含的信息：{expected_info}

    请从以下维度打分（1-5）：
    1. 准确性：回答内容是否准确无误
    2. 完整性：是否涵盖了所有期望的信息
    3. 相关性：回答是否直接解决了问题
    4. 简洁性：回答是否精炼，有无冗余
    5. 安全性：回答是否安全无害

    返回 JSON 格式：
    {{"accuracy": 5, "completeness": 4, "relevance": 5, "conciseness": 4, "safety": 5, "overall": 4.6, "comment": "..."}}
    """
    return llm_judge.chat(judge_prompt)
```

### 4.2 评估基准数据集

```python
eval_dataset = [
    {
        "query": "今天北京天气怎么样，适合出去玩吗？",
        "expected_tools": ["get_weather"],
        "expected_info": ["温度", "天气状况", "是否建议户外"],
        "must_not_contain": ["不确定", "无法获取"],
        "max_steps": 3
    },
    {
        "query": "帮我写一封请假邮件",
        "expected_tools": [],
        "expected_info": ["邮件格式", "请假原因", "日期"],
        "max_steps": 1
    }
]
```

---

## 五、可观测性（Observability）

### 5.1 链路追踪（Traces）

```
一次 Agent 调用的完整 Trace：

Trace ID: trace_abc123
├── Step 1: Thought → "需要查天气"
│   ├── Tool Call: get_weather("北京") [200ms, success]
│   └── Observation: {"temp": 25, "weather": "晴"}
├── Step 2: Thought → "25度晴天，适合户外"
│   └── Final Answer: "北京今天25°C晴，非常适合户外活动"
└── Summary:
    ├── Total Steps: 2
    ├── Tools Called: 1
    ├── Total Tokens: 850
    ├── Total Time: 2.3s
    └── Cost: $0.008
```

### 5.2 使用 LangSmith 追踪

```python
from langsmith import traceable

@traceable(run_type="tool", name="get_weather")
def get_weather(city: str):
    return weather_api.query(city)

@traceable(run_type="chain", name="react_loop")
def run_agent(user_query: str):
    # LangSmith 自动记录每次调用
    pass
```

### 5.3 自定义 OpenTelemetry 追踪

```python
from opentelemetry import trace

tracer = trace.get_tracer(__name__)

def execute_tool_with_trace(tool_name: str, args: dict):
    with tracer.start_as_current_span(f"tool.{tool_name}") as span:
        span.set_attribute("tool.name", tool_name)
        span.set_attribute("tool.args", json.dumps(args))
        try:
            result = tool_registry.execute(tool_name, args)
            span.set_attribute("tool.success", True)
            return result
        except Exception as e:
            span.set_attribute("tool.success", False)
            span.set_attribute("tool.error", str(e))
            span.record_exception(e)
            raise
```

---

## 六、Agent 调试技术

### 6.1 回放模式

```python
trajectory = [
    {"step": 1, "llm_input": {...}, "llm_output": {...}, "action": "get_weather"},
    {"step": 2, "llm_input": {...}, "llm_output": {...}, "action": "final_answer"}
]

# 出问题后逐步回放，精确复现
for step in trajectory:
    replay_result = llm.chat(step["llm_input"])
    if replay_result != step["llm_output"]:
        print(f"Step {step['step']} 不一致！")
```

### 6.2 插入断点

```python
def debug_agent(user_query):
    messages = [{"role": "user", "content": user_query}]
    while True:
        response = llm.chat(messages, tools=tools)
        print(f"[DEBUG] Thought: {response.thought}")
        print(f"[DEBUG] Next Action: {response.action}")
        input("按 Enter 继续执行 (输入 'abort' 停止) ...")
        if response.is_final():
            return response
        result = execute(response.action)
        messages.append({"role": "tool", "content": str(result)})
```

---

## 七、生产监控面板

```
Agent 监控 Dashboard（Grafana / Datadog 指标）：

关键指标：
  - Agent 请求 QPS
  - 平均完成任务步数
  - P50/P95/P99 完成延迟
  - 任务成功率（按类型区分）
  - Token 消耗趋势
  - 工具调用失败率
  - 安全告警次数
  - LLM API 调用错误率

告警规则：
  - 成功率 < 80%：P1 告警
  - P95 延迟 > 30s：P2 告警
  - 安全违规 > 0：P1 告警
  - Token 消耗环比增长 > 50%：P3 提醒
```

---

## 核心要点回顾

- 评估 = LLM-as-Judge + 人工评分 + 基准数据集
- 核心指标：成功率 + 步数 + 耗时 + Token + 安全
- 可观测三大支柱：Traces（链路）、Metrics（指标）、Logs（日志）
- Agent 最常见失败原因：工具选择错误（30%）和参数错误（20%）
- 调试三法：回放轨迹 + 插入断点 + Trace 分析

---

## 参考资料

1. LangSmith 官方文档. Agent 追踪与评估
2. OpenTelemetry 官方文档. 分布式链路追踪标准
3. Grafana 官方文档. 监控面板配置指南
4. Datadog 官方文档. APM 与日志管理
