# AI Agent 测试策略

> **核心摘要**：Agent 作为非确定性系统，其测试方法论与传统软件有本质区别。本文基于测试金字塔模型（静态分析 → 单元测试 → 集成测试 → E2E 测试），深入讲解各层级的测试方法、Mock LLM 策略、LLM-as-Judge 评估方式以及 CI/CD 集成方案。

## 前置阅读

- [[AI Agent核心知识点]]
- [[AI Agent 评估与可观测性]]

---

## 一、Agent 测试的特殊挑战

| 挑战 | 传统软件 | Agent |
|---|---|---|
| **确定性** | 同样输入 = 同样输出 | 同样输入 = 不同输出 |
| **边界** | 明确的输入输出 | 中间过程不确定 |
| **测试预言** | 精确匹配期望值 | "好"是个模糊概念 |
| **外部依赖** | 可 Mock | LLM API 不能完全 Mock |
| **副作用** | 可控 | 可能修改文件 / 调 API |

---

## 二、测试金字塔

```
              /\
             /E2E\        端到端：完整任务执行
            /------\
           /集成测试 \     Agent + 真实工具 + Mock LLM
          /----------\
         /  单元测试  \    每个组件独立测试
        /--------------\
       /   静态分析     \  Prompt 模板检查 / Schema 校验
      /------------------\
```

---

## 三、单元测试

### 3.1 工具函数测试

```python
def test_get_weather_tool():
    """工具函数是确定性的，正常单测"""
    result = get_weather("北京")
    assert "temp" in result
    assert "weather" in result

def test_permission_guard():
    """权限检查逻辑可精确测试"""
    guard = PermissionGuard(mode="deny")
    ok, msg = guard.check("rm -rf /")
    assert ok == False
    assert "禁止" in msg
```

### 3.2 Prompt 生成测试

```python
def test_build_system_prompt():
    tools = [search_tool, weather_tool]
    prompt = build_system_prompt(tools)
    assert "你是 AI 助手" in prompt
    assert "web_search" in prompt
    assert "get_weather" in prompt
    assert "JSON 格式" in prompt

def test_prompt_includes_user_context():
    user_context = {"name": "张三", "role": "Java 开发"}
    prompt = build_prompt_with_context("查询天气", user_context)
    assert "张三" in prompt
    assert "Java 开发" in prompt
```

### 3.3 Schema 校验测试

```python
def test_tool_response_schema():
    response = {"tool": "get_weather", "args": {"city": "北京"}}
    schema = {
        "type": "object",
        "required": ["tool", "args"],
        "properties": {
            "tool": {"type": "string"},
            "args": {"type": "object"}
        }
    }
    jsonschema.validate(response, schema)
```

---

## 四、集成测试（Mock LLM）

### 4.1 Mock LLM 策略

```python
class MockLLM:
    """可控的 Mock LLM，返回预设响应"""
    def __init__(self, responses: list):
        self.responses = responses
        self.call_count = 0
        self.call_history = []

    def chat(self, messages, tools=None):
        self.call_history.append({"messages": messages, "tools": tools})
        response = self.responses[self.call_count]
        self.call_count += 1
        return response

def test_agent_calls_correct_tool():
    """测试 Agent 收到天气查询时是否调用 get_weather"""
    mock_llm = MockLLM(responses=[
        LLMResponse(
            thought="需要查天气",
            tool_call={"name": "get_weather", "args": {"city": "北京"}}
        ),
        LLMResponse(
            thought="已经拿到天气数据",
            final_answer="北京今天 25°C 晴"
        )
    ])
    agent = Agent(llm=mock_llm, tools=[weather_tool])
    result, trajectory = agent.run("今天北京天气怎么样")
    assert trajectory[0].tool_name == "get_weather"
    assert trajectory[0].tool_args == {"city": "北京"}
    assert "25°C" in result
```

### 4.2 循环退出测试

```python
def test_agent_stops_after_max_iterations():
    """测试 Agent 达到最大迭代次数时退出"""
    mock_llm = MockLLM(responses=[
        LLMResponse(tool_call={"name": "get_weather", "args": {"city": "北京"}})
    ] * 20)
    agent = Agent(llm=mock_llm, tools=[weather_tool], max_iterations=5)
    with pytest.raises(MaxIterationsExceeded):
        agent.run("查询天气")
```

### 4.3 工具错误处理测试

```python
def test_agent_handles_tool_error():
    """测试工具调用失败时 Agent 是否正确处理"""
    mock_llm = MockLLM(responses=[
        LLMResponse(tool_call={"name": "nonexistent_tool", "args": {}}),
        LLMResponse(tool_call={"name": "get_weather", "args": {"city": "北京"}}),
        LLMResponse(final_answer="北京今天晴")
    ])
    agent = Agent(llm=mock_llm, tools=[weather_tool])
    result, trajectory = agent.run("天气")
    assert trajectory[0].status == "error"
    assert trajectory[1].status == "success"
```

---

## 五、E2E 测试（真实 LLM）

### 5.1 测试数据集

```python
E2E_TEST_CASES = [
    {
        "id": "E2E-001",
        "query": "北京今天天气怎么样",
        "expected_tools": ["get_weather"],
        "must_contain": ["温度", "天气"],
        "must_not_contain": ["不确定", "无法获取"],
        "max_steps": 3,
        "min_score": 0.7
    },
    {
        "id": "E2E-002",
        "query": "帮我写一个 Hello World",
        "expected_tools": [],
        "must_contain": ["Hello", "World"],
        "max_steps": 1,
        "min_score": 0.9
    }
]
```

### 5.2 E2E 评估执行

```python
def run_e2e_tests(agent, test_cases):
    results = []
    for tc in test_cases:
        result, trajectory = agent.run(tc["query"])

        tools_called = [s.tool_name for s in trajectory if s.type == "tool_call"]
        tools_ok = set(tools_called) == set(tc["expected_tools"])
        content_ok = all(kw in result for kw in tc["must_contain"])
        content_bad = any(kw in result for kw in tc["must_not_contain"])
        steps_ok = len(trajectory) <= tc["max_steps"]
        score = evaluate_with_judge(tc["query"], result)
        score_ok = score >= tc["min_score"]

        passed = all([tools_ok, content_ok, not content_bad, steps_ok, score_ok])
        results.append({**tc, "passed": passed, "score": score, "trajectory": trajectory})
    return results
```

---

## 六、自动化 CI 集成

```yaml
name: Agent Tests
on: [push, pull_request]

jobs:
  unit-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: pip install -r requirements.txt
      - run: pytest tests/unit/ -v

  integration-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: pytest tests/integration/ -v --mock-llm

  e2e-test:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - run: pytest tests/e2e/ -v
        env:
          ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}

  eval-gate:
    runs-on: ubuntu-latest
    steps:
      - run: python eval/run_eval.py --pass-threshold 0.80
```

---

## 核心要点回顾

- Agent 测试最难的是非确定性输出 + "好"难以量化 + 外部依赖多
- 单元测试覆盖：工具函数、Prompt 模板、Schema 校验、权限逻辑
- 集成测试：Mock LLM 返回预设响应，验证 Agent 的工具选择和执行逻辑
- E2E 评估：LLM-as-Judge 打分 + 人工抽样 + 统计指标
- CI 策略：单测 + 集成每次提交跑，E2E 只在 main 分支跑，通过率低于 80% 不通过

---

## 参考资料

1. pytest 官方文档. 单元测试框架
2. LangSmith 官方文档. Agent 评估与追踪
3. Anthropic 官方文档. LLM-as-Judge 评估方法论
4. GitHub Actions 官方文档. CI/CD 工作流配置
