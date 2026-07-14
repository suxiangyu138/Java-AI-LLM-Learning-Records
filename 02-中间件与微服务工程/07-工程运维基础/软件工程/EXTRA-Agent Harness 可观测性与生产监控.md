# Agent Harness 可观测性与生产监控（实战版）

> **文档定位**：Agent Harness 工程化核心 | 可观测性方案深度解析
> **核心问题**：Agent 跑在生产环境，怎么看它干得好不好？出问题了怎么快速定位？

---

## 一、Agent 可观测性三大支柱

```
Agent Observability = Traces + Metrics + Logs

┌──────────────────────────────────────────────┐
│  Traces（链路追踪）                            │
│  每次 Agent 调用的完整执行链路                  │
│  Thought → Tool Call → Observation → ...      │
├──────────────────────────────────────────────┤
│  Metrics（指标）                               │
│  成功率、延迟、Token 消耗、工具调用分布          │
├──────────────────────────────────────────────┤
│  Logs（日志）                                  │
│  结构化日志：参数、结果、错误、审计              │
└──────────────────────────────────────────────┘
```

---

## 二、Traces — 全链路追踪

### 2.1 Agent Trace 结构

```json
{
  "trace_id": "abc123",
  "session_id": "sess_456",
  "user_id": "user_789",
  "start_time": "2024-06-15T10:30:00Z",
  "end_time": "2024-06-15T10:30:05Z",
  "total_duration_ms": 5200,
  "total_tokens": 1850,
  "steps": [
    {
      "step": 1,
      "type": "thought",
      "content": "需要查询天气信息",
      "duration_ms": 200
    },
    {
      "step": 2,
      "type": "tool_call",
      "tool": "get_weather",
      "args": {"city": "北京"},
      "duration_ms": 450,
      "status": "success"
    },
    {
      "step": 3,
      "type": "observation",
      "result": "北京 25°C 晴",
      "duration_ms": 10
    },
    {
      "step": 4,
      "type": "final_answer",
      "content": "北京今天25°C晴，适合户外",
      "duration_ms": 150
    }
  ],
  "status": "success"
}
```

### 2.2 OpenTelemetry 集成

```python
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.exporter.otlp import OTLPSpanExporter

# 初始化
provider = TracerProvider()
provider.add_span_processor(
    BatchSpanProcessor(OTLPSpanExporter(endpoint="http://jaeger:4317"))
)
trace.set_tracer_provider(provider)
tracer = trace.get_tracer("agent-harness")

# 使用
def run_agent_with_trace(user_query: str, user_id: str):
    with tracer.start_as_current_span("agent_execution") as span:
        span.set_attribute("user_id", user_id)
        span.set_attribute("query", user_query[:200])
        
        try:
            result = agent_loop.run(user_query)
            span.set_attribute("status", "success")
            span.set_attribute("steps", len(result.trajectory))
            span.set_attribute("tokens", result.total_tokens)
            return result
        except Exception as e:
            span.set_attribute("status", "error")
            span.record_exception(e)
            raise
```

---

## 三、Metrics — 核心监控指标

### 3.1 黄金指标

| 指标 | 计算 | 告警阈值 |
|---|---|---|
| **请求量（QPS）** | count / time | 突增 > 200% |
| **成功率** | success / total | < 80% |
| **P95 延迟** | 95th percentile | > 30s |
| **平均步数** | total_steps / total_tasks | > 10 可能陷入循环 |
| **Token 消耗** | sum(tokens) / time | 日环比 > 50% |
| **工具失败率** | tool_errors / tool_calls | > 5% |
| **LLM API 错误率** | api_errors / api_calls | > 1% |

### 3.2 Prometheus 指标暴露

```python
from prometheus_client import Counter, Histogram, Gauge

# 定义指标
agent_requests = Counter('agent_requests_total', 'Total requests', ['status'])
agent_duration = Histogram('agent_duration_seconds', 'Request duration')
agent_steps = Histogram('agent_steps_total', 'Steps per request')
agent_tokens = Counter('agent_tokens_total', 'Tokens consumed', ['model'])
agent_tool_calls = Counter('agent_tool_calls_total', 'Tool calls', ['tool', 'status'])

# 埋点
@agent_duration.time()
def execute_agent(query: str):
    try:
        result = agent.run(query)
        agent_requests.labels(status='success').inc()
        agent_steps.observe(len(result.trajectory))
        agent_tokens.labels(model='claude-sonnet-4-6').inc(result.total_tokens)
        for step in result.trajectory:
            if step.type == 'tool_call':
                agent_tool_calls.labels(
                    tool=step.tool, status='success'
                ).inc()
        return result
    except Exception:
        agent_requests.labels(status='error').inc()
        raise
```

---

## 四、Logs — 结构化日志

```python
import structlog

logger = structlog.get_logger()

def log_agent_event(event_type: str, **kwargs):
    """统一的结构化日志格式"""
    logger.info(
        event_type,
        timestamp=datetime.now().isoformat(),
        **kwargs
    )

# 使用
log_agent_event("agent.start", user_id="u1", query="...")
log_agent_event("tool.call", tool="get_weather", args={"city": "北京"}, duration_ms=450)
log_agent_event("tool.result", tool="get_weather", status="success")
log_agent_event("agent.end", status="success", steps=4, tokens=1850, duration_ms=5200)
```

---

## 五、Grafana 监控面板

```yaml
# Dashboard 布局
rows:
  - title: "Agent 概览"
    panels:
      - stat: "今日请求量"
      - stat: "成功率"
      - stat: "P95 延迟"
      - stat: "活跃会话数"
      
  - title: "执行效率"
    panels:
      - timeseries: "QPS 趋势"
      - timeseries: "P50/P95/P99 延迟"
      - timeseries: "平均步数趋势"
      - heatmap: "延迟分布"
      
  - title: "Token 消耗"
    panels:
      - timeseries: "Token/小时"
      - stat: "今日总 Token"
      - stat: "预估今日费用"
      
  - title: "工具调用"
    panels:
      - pie: "工具调用分布"
      - timeseries: "工具失败率"
      - table: "Top 慢工具"
      
  - title: "错误分析"
    panels:
      - timeseries: "错误率趋势"
      - table: "Top 错误类型"
      - log: "最近错误日志"
```

---

## 六、告警规则

```yaml
# Prometheus AlertManager 规则
groups:
  - name: agent_alerts
    rules:
      - alert: AgentSuccessRateLow
        expr: rate(agent_requests_total{status="success"}[5m]) / rate(agent_requests_total[5m]) < 0.8
        for: 5m
        labels: { severity: critical }
        annotations: { summary: "Agent 成功率低于 80%" }
      
      - alert: AgentLatencyHigh
        expr: histogram_quantile(0.95, agent_duration_seconds) > 30
        for: 5m
        labels: { severity: warning }
      
      - alert: AgentLoopDetected
        expr: rate(agent_steps_total[1m]) > 20
        for: 2m
        labels: { severity: critical }
        annotations: { summary: "Agent 可能陷入循环" }
      
      - alert: ToolFailureSpike
        expr: rate(agent_tool_calls_total{status="error"}[5m]) > 5
        labels: { severity: warning }
      
      - alert: LLMApiDown
        expr: rate(agent_requests_total{status="error"}[1m]) > 0.5
        labels: { severity: critical }
        annotations: { summary: "LLM API 不可用，切换到备用模型" }
```

---

## 七、失败分析自动化

```python
class FailureAnalyzer:
    """自动分类 Agent 失败原因"""
    
    def analyze(self, trace: dict) -> dict:
        errors = []
        
        for step in trace["steps"]:
            if step.get("status") == "error":
                error_type = self._classify_error(step)
                errors.append({
                    "step": step["step"],
                    "type": error_type,
                    "detail": step.get("error", "")
                })
        
        return {
            "trace_id": trace["trace_id"],
            "status": trace["status"],
            "error_count": len(errors),
            "errors": errors,
            "root_cause": self._find_root_cause(errors)
        }
    
    def _classify_error(self, step: dict) -> str:
        error_msg = step.get("error", "")
        if "timeout" in error_msg.lower():
            return "TOOL_TIMEOUT"
        if "rate limit" in error_msg.lower():
            return "RATE_LIMITED"
        if "unauthorized" in error_msg.lower():
            return "AUTH_ERROR"
        if "invalid" in error_msg.lower():
            return "INVALID_PARAMS"
        return "UNKNOWN"
```

---

## 八、面试核心要点

1. **Agent 可观测性三大支柱？** Traces（链路）、Metrics（指标）、Logs（日志）
2. **黄金指标有哪些？** QPS、成功率、P95 延迟、Token 消耗、工具失败率
3. **怎么发现 Agent 陷入循环？** 监控平均步数 + 单次步数阈值告警
4. **OpenTelemetry 怎么接入？** TracerProvider + OTLP Exporter + span 埋点
5. **失败怎么自动分类？** 错误分类器 + root cause 分析

---

## 九、极简总结

```
Traces = OpenTelemetry + Jaeger（每次 Agent 调用的完整链路）
Metrics = Prometheus + Grafana（QPS/延迟/成功率/Token）
Logs = 结构化 JSON（可搜索、可追踪）
告警 = 成功率 < 80% + P95 > 30s + 步数激增
分析 = 自动分类错误 → 找到 root cause → 针对性优化
```
