# AI Agent 测试策略（实战版）

> **文档定位**：AI Agent 核心技术 | 测试方法论详解
> **核心问题**：Agent 这种非确定性系统怎么测？单元测试、集成测试、E2E 测试怎么做？

---

## 一、Agent 测试特殊挑战

| 挑战 | 传统软件 | Agent |
|---|---|---|
| **确定性** | 同样输入=同样输出 | 同样输入=不同输出 |
| **边界** | 明确的输入输出 | 中间过程不确定 |
| **测试预言** | 精确匹配期望值 | "好"是个模糊概念 |
| **外部依赖** | Mock 掉 | LLM API 不能完全 Mock |
| **副作用** | 可控 | 可能修改文件/调 API |

---

## 二、Agent 测试金字塔

```
              /\
             /E2E\        端到端：完整任务执行
            /------\
           /集成测试 \      Agent + 真实工具 + Mock LLM
          /----------\
         /  单元测试   \   每个组件独立测试
        /--------------\
       /   静态分析      \  Prompt 模板检查 / Schema 校验
      /------------------\
```

---

## 三、单元测试

### 3.1 工具函数测试（常规单测）

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
    
    # 检查关键内容
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

### 3.3 JSON Schema 校验测试

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
        # 第 1 次调用：返回 tool call
        LLMResponse(
            thought="需要查天气",
            tool_call={"name": "get_weather", "args": {"city": "北京"}}
        ),
        # 第 2 次调用：返回 final answer
        LLMResponse(
            thought="已经拿到天气数据",
            final_answer="北京今天 25°C 晴"
        )
    ])
    
    agent = Agent(llm=mock_llm, tools=[weather_tool])
    result, trajectory = agent.run("今天北京天气怎么样")
    
    # 验证
    assert trajectory[0].tool_name == "get_weather"
    assert trajectory[0].tool_args == {"city": "北京"}
    assert "25°C" in result
```

### 4.2 Agent 循环退出测试

```python
def test_agent_stops_after_max_iterations():
    """测试 Agent 在达到最大迭代次数时退出"""
    mock_llm = MockLLM(responses=[
        LLMResponse(tool_call={"name": "get_weather", "args": {"city": "北京"}})
    ] * 20)  # 永远返回 tool call
    
    agent = Agent(llm=mock_llm, tools=[weather_tool], max_iterations=5)
    
    with pytest.raises(MaxIterationsExceeded):
        agent.run("查询天气")
```

### 4.3 工具错误处理测试

```python
def test_agent_handles_tool_error():
    """测试工具调用失败时 Agent 是否正确处理"""
    mock_llm = MockLLM(responses=[
        # 第 1 次：调用不存在的工具
        LLMResponse(tool_call={"name": "nonexistent_tool", "args": {}}),
        # 第 2 次：收到错误后重试正确的工具
        LLMResponse(tool_call={"name": "get_weather", "args": {"city": "北京"}}),
        # 第 3 次：final answer
        LLMResponse(final_answer="北京今天晴")
    ])
    
    agent = Agent(llm=mock_llm, tools=[weather_tool])
    result, trajectory = agent.run("天气")
    
    assert trajectory[0].status == "error"
    assert trajectory[1].status == "success"
```

---

## 五、E2E 测试（真实 LLM）

### 5.1 E2E 测试数据集

```python
E2E_TEST_CASES = [
    {
        "id": "E2E-001",
        "query": "北京今天天气怎么样",
        "expected_tools": ["get_weather"],
        "must_contain": ["温度", "天气"],
        "must_not_contain": ["不确定", "无法获取"],
        "max_steps": 3,
        "min_score": 0.7  # LLM-as-Judge 最低分
    },
    {
        "id": "E2E-002", 
        "query": "帮我写一个 Hello World",
        "expected_tools": [],  # 不需要工具
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
        
        # 1. 工具正确性
        tools_called = [s.tool_name for s in trajectory if s.type == "tool_call"]
        tools_ok = set(tools_called) == set(tc["expected_tools"])
        
        # 2. 内容检查
        content_ok = all(kw in result for kw in tc["must_contain"])
        content_bad = any(kw in result for kw in tc["must_not_contain"])
        
        # 3. 步数检查
        steps_ok = len(trajectory) <= tc["max_steps"]
        
        # 4. LLM-as-Judge 评分
        score = evaluate_with_judge(tc["query"], result)
        score_ok = score >= tc["min_score"]
        
        passed = all([tools_ok, content_ok, not content_bad, steps_ok, score_ok])
        results.append({**tc, "passed": passed, "score": score, "trajectory": trajectory})
    
    return results
```

---

## 六、自动化测试 CI 集成

```yaml
# .github/workflows/agent-test.yml
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
    if: github.ref == 'refs/heads/main'  # 只在 main 分支跑 E2E
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

## 七、面试核心要点

1. **Agent 测试最难的是什么？** 非确定性输出 + "好"难以量化 + 外部依赖多
2. **单元测试测什么？** 工具函数、Prompt 模板、Schema 校验、权限逻辑
3. **集成测试怎么测？** Mock LLM 返回预设响应，验证 Agent 的工具选择和执行逻辑
4. **E2E 怎么评？** LLM-as-Judge 打分 + 人工抽样 + 指标统计
5. **CI 里怎么跑？** 单测+集成每次提交跑，E2E 只在 main 分支跑

---

## 八、极简总结

```
单测 = 工具函数 + Prompt 模板 + Schema + 权限（确定性的）
集成 = Mock LLM 返回预设 → 测 Agent 逻辑（工具选择/循环/错误处理）
E2E = 真实 LLM + 评估数据集 + LLM-as-Judge
CI = 单测/集成每次提交 → E2E 只在 main → < 80% 不通过
```
