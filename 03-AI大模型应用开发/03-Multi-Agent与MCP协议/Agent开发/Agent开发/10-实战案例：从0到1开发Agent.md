# 10 - 实战案例：从 0 到 1 开发 Agent

> **核心摘要**：理论走完，动手实战。本文以「客户售后处理 Agent」为完整案例，走一遍从需求定义到生产部署的全流程——包含完整代码骨架、评估集示例、多 Agent 编排与部署配置，可直接复用为项目模板。

> **前置阅读**：本系列全部 9 篇（01-09）

---

## 📚 目录

1. [项目背景与需求定义](#1-项目背景与需求定义)
2. [系统设计](#2-系统设计)
3. [核心代码骨架](#3-核心代码骨架)
4. [评估集建立](#4-评估集建立)
5. [多 Agent 编排](#5-多-agent-编排)
6. [安全与治理](#6-安全与治理)
7. [部署与运维](#7-部署与运维)
8. [复盘与迭代](#8-复盘与迭代)
9. [核心要点](#9-核心要点)

---

## 1. 项目背景与需求定义

### 1.1 项目背景

> **需求**：为某电商平台构建「售后处理 Agent」——自动处理客户售后请求（退款/换货/物流查询），复杂情况升级人工。

### 1.2 需求定义（意图工程）

```yaml
业务意图:
  用户: 电商客户
  场景: 提交售后请求 → 自动分类 → 自动处理/升级人工
  功能:
    - 售后请求自动分类（退款/换货/物流/其他）
    - 退款：校验订单状态与资格 → 自动退款
    - 换货/物流：查询物流 → 答复/安排
    - 复杂情况升级人工
  边界: 不做支付链路、不做商品推荐

技术约束:
  框架: LangGraph + FastAPI + PostgreSQL + Redis
  模型: 路由用小模型（haiku-4.5）+ 主流程大模型（sonnet-5）
  工具: MCP 服务器（订单/支付/物流/工单）

验收标准:
  性能: 简单任务 P95 < 10s
  质量: 任务成功率 > 90%，退款准确率 100%（人工复核）
  安全: 高危操作（退款）必须 HITL 审批
```

---

## 2. 系统设计

### 2.1 架构总览

```text
售后处理 Agent 架构
┌──────────┐    ┌──────────┐    ┌──────────────┐
│ API 网关   │ → │ 消息队列   │ → │ Worker 集群   │
│ (FastAPI) │    │ (Redis)  │    │ (LangGraph)  │
└──────────┘    └──────────┘    └──────┬───────┘
                                       │
                        ┌──────────────┼──────────────┐
                        ▼              ▼              ▼
                  ┌──────────┐  ┌──────────┐  ┌──────────┐
                  │ 分类 Agent │  │ 处理 Agent │  │ 退款 Agent │
                  │ (小模型)   │  │ (主循环)   │  │ (HITL)   │
                  └──────────┘  └──────────┘  └──────────┘
                        │              │              │
                        ▼              ▼              ▼
                  ┌──────────────────────────────────────┐
                  │ MCP 工具服务器：订单/支付/物流/工单     │
                  └──────────────────────────────────────┘
```

### 2.2 状态设计

```python
# 任务状态（PostgreSQL 持久化）
class TaskState(TypedDict):
    task_id: str
    request_text: str            # 原始请求
    category: str                # 分类结果
    order_info: dict             # 订单信息
    decisions: list              # 决策记录
    approval: dict               # HITL 审批记录
    result: dict                 # 最终结果
    trace_id: str                # 因果链追踪
```

---

## 3. 核心代码骨架

### 3.1 主流程：LangGraph 图

```python
# agent_graph.py — 售后处理 Agent 主图
from langgraph.graph import StateGraph, END
from langgraph.checkpoint.postgres import PostgresSaver
from .tools import tool_executor, MCP_CLIENT

# 节点定义
def classify(state):
    """分类节点：小模型快速分类"""
    category = router_model.classify(
        state["request_text"], categories=["refund", "exchange", "logistics", "other"])
    return {"category": category, "decisions": [{"step": "classify", "result": category}]}

def query_order(state):
    """订单查询节点：MCP 工具"""
    result = tool_executor("query_order", {"order_id": extract_order_id(state["request_text"])})
    return {"order_info": result}

def handle_refund(state):
    """退款处理节点：校验 + 申请审批"""
    eligibility = tool_executor("check_refund_eligibility", {"order_id": state["order_info"]["id"]})
    if not eligibility["ok"]:
        return {"result": {"status": "rejected", "reason": eligibility["reason"]}}
    # HITL：退款必须人工审批
    approval = request_approval(
        action="refund",
        context={"order": state["order_info"], "amount": eligibility["refund_amount"]},
        timeout=300)
    if approval:
        result = tool_executor("execute_refund", {"order_id": state["order_info"]["id"]})
        return {"result": {"status": "refunded", "refund_id": result["refund_id"]}}
    return {"result": {"status": "escalated_human", "reason": "审批未通过"}}

def escalate(state):
    """升级人工节点：创建工单"""
    ticket = tool_executor("create_ticket", {
        "request": state["request_text"], "category": state["category"]})
    return {"result": {"status": "escalated", "ticket_id": ticket["id"]}}

def router(state):
    """路由边：按分类分派"""
    mapping = {"refund": "handle_refund", "exchange": "handle_exchange",
               "logistics": "handle_logistics", "other": "escalate"}
    return mapping.get(state["category"], "escalate")

# 构图
graph = StateGraph(TaskState)
graph.add_node("classify", classify)
graph.add_node("query_order", query_order)
graph.add_node("handle_refund", handle_refund)
graph.add_node("handle_exchange", handle_exchange)
graph.add_node("handle_logistics", handle_logistics)
graph.add_node("escalate", escalate)
graph.set_entry_point("classify")
graph.add_edge("classify", "query_order")
graph.add_conditional_edges("query_order", router)
for node in ["handle_refund", "handle_exchange", "handle_logistics", "escalate"]:
    graph.add_edge(node, END)

# 生产：PostgreSQL 检查点（断点恢复）
checkpointer = PostgresSaver.from_conn_string("postgresql://...")
app = graph.compile(checkpointer=checkpointer)
```

### 3.2 工具执行器（统一入口）

```python
# tools.py — 工具统一执行（校验 + 留痕 + 审计）
import time
from .registry import TOOL_REGISTRY

def tool_executor(name: str, args: dict, trace_id: str = None):
    tool = TOOL_REGISTRY[name]
    validate_args(args, tool["schema"])            # 参数校验
    if tool["requires_approval"]:                  # 高危审批
        wait_human_approval(name, args, trace_id)
    t0 = time.time()
    result = MCP_CLIENT.call(name, args)           # MCP 调用
    emit_span(trace_id=trace_id, tool=name,        # 因果链追踪
              args_summary=redact(args),
              duration_ms=(time.time() - t0) * 1000,
              status="ok")
    return result
```

---

## 4. 评估集建立

### 4.1 评估集示例（先建评估再开发）

```json
// evals/refund_cases.json — 退款类评估用例
[
  {
    "task": "用户申请退款，订单已签收 3 天",
    "setup": {"order_status": "delivered", "return_window": "7d"},
    "success": {"status": "refunded", "approval_used": true,
                "tools": ["query_order", "check_refund_eligibility", "execute_refund"]}
  },
  {
    "task": "用户申请退款，订单已超过退货期",
    "setup": {"order_status": "delivered", "days_since_delivery": 30},
    "success": {"status": "rejected", "reason_mentions": ["退货期"]}
  },
  {
    "task": "用户询问退款进度，无退款申请记录",
    "success": {"status": "escalated", "no_fabrication": true}
  }
]
```

### 4.2 评估运行

```bash
# 评估命令（CI 中运行）
python eval_run.py --suite evals/refund_cases.json --gate 90

# 输出
# refund_cases: 12/12 通过（正确性 100%）
# 轨迹对齐度: 0.87（达标 >0.8）
# 平均轮次: 4.2（预算内）
# 门禁判定: ✅ PASS
```

---

## 5. 多 Agent 编排

### 5.1 Planner 模式（复杂售后）

```python
# planner.py — 复杂场景：Planner + Worker
planner_agent = Agent(
    name="planner",
    instructions="""拆解复杂售后请求为子任务列表。
    输出 JSON 计划：[{step, tool, inputs, acceptance}]""",
)

worker_agent = Agent(
    name="worker",
    tools=[tool_executor],
    instructions="按计划执行单个子任务，完成后报告结果与验证证据",
)

def run_planner_mode(request: str, plan: list) -> dict:
    results = {}
    for step in plan:                          # 按计划逐步执行
        result = worker_agent.run(step)        # Worker 独立执行
        results[step["id"]] = result
        if not verify_acceptance(result, step):  # Evaluator 验收
            return retry_step(step, results)   # 只重做失败步
    return results                             # 可中断、可恢复
```

### 5.2 协作要点落地

```text
本案例的多 Agent 实践
├── 分类 Agent（小模型路由）：意图分流
├── 处理 Agent（主循环）：主流程执行
├── 退款 Agent（HITL）：高危操作人机协同
├── 计划持久化：每步结果入 Redis（可恢复）
└── 因果链：所有 Agent 共享 trace_id
```

---

## 6. 安全与治理

### 6.1 安全设计落地

```python
# security.py — 安全三件套
def security_gate(request_text, action, trace_id):
    # ① Guardrails：输入守卫（注入检测）
    input_check = guardrails.check_input(request_text)
    if not input_check["safe"]:
        return BLOCK_AND_ALERT(trace_id, input_check["risk"])

    # ② 最小权限：动作白名单
    if action["tool"] not in ALLOWED_TOOLS:
        return PERMISSION_DENIED(action)

    # ③ HITL：高危操作审批（退款/删除）
    if action["tool"] in HIGH_RISK_TOOLS:
        approval = request_human_approval(action, trace_id, timeout=300)
        if not approval:
            return APPROVAL_REJECTED
    return OK
```

### 6.2 治理清单

```text
上线前治理检查
[ ] 任务状态持久化（PostgreSQL Checkpointer）
[ ] 因果链追踪全链路（trace_id 贯穿）
[ ] 退款/删除类操作 HITL 审批门
[ ] 敏感信息脱敏（日志/追踪/记忆）
[ ] token 预算与告警配置
[ ] 评估集覆盖主流程 + 失败场景 + 注入用例
[ ] 灰度发布计划（影子 → 金丝雀 → 全量）
```

---

## 7. 部署与运维

### 7.1 Docker 部署骨架

```yaml
# docker-compose.yml 片段
services:
  worker:
    build: ./agent-service
    environment:
      MODEL_ROUTER: claude-haiku-4-5-20251001
      MODEL_MAIN: claude-sonnet-5
      TOKEN_BUDGET_PER_TASK: "50000"
    depends_on: [redis, postgres]
    deploy:
      replicas: 3              # 水平扩展
  redis:
    image: redis:7
  postgres:
    image: postgres:16
```

### 7.2 可观测性接入

```yaml
# 指标 + 告警配置
metrics:
  task_success_rate: {alert_when: "< 0.85", action: pager}
  avg_steps_per_task: {alert_when: "> 15", action: warning}
  token_usage_per_task: {alert_when: "> budget*0.8", action: warning}
  tool_failure_rate: {alert_when: "> 0.10", action: warning}
  refund_error: {alert_when: "> 0", action: pager}   # 退款错误 = 0 容忍
```

### 7.3 运营流程

```text
上线后运营
├── 每日：监控面板巡检（成功率/延迟/token）
├── 每周：评估回归（模型/提示词变更）
├── 事故：trace 定位 → 修复 → 转评估用例
└── 迭代：评估分上升才允许上线
```

---

## 8. 复盘与迭代

### 8.1 项目复盘（对照本系列知识）

| 环节 | 应用的知识 | 效果 |
|------|-----------|------|
| 需求定义 | 意图工程（01 篇） | 边界清晰、验收量化 |
| 架构 | P-A-M-E + 图编程（01/03） | 状态可恢复 |
| 评估 | 先建评估（07 篇） | 每版改动有分数 |
| 安全 | Guardrails + HITL（08 篇） | 退款 100% 人工复核 |
| 部署 | 事件驱动（09 篇） | P95 < 10s 达标 |
| 协作 | Planner 模式（06 篇） | 失败只重做单步 |

### 8.2 迭代方向

```text
V2 迭代候选
├── ① 评估集扩充（真实事故转用例）
├── ② 记忆接入（用户历史偏好）
├── ③ 模型路由调优（更多步骤走小模型）
├── ④ 去中心化（多 Worker 共识）
└── ⑤ Harness 完善（AIBOM/红队常态化）
→ 原则：先埋点数据，再决定优化方向
```

---

## 9. 核心要点

> 🎯 **核心要点**：
> 1. 从 0 到 1 的顺序：**需求（意图工程）→ 设计 → 评估集（先建）→ 骨架 → 安全 → 部署**——评估先行是成败关键
> 2. 代码骨架五件套：图编排（LangGraph）+ 统一工具执行器 + Checkpointer + 因果链 + 评估门禁
> 3. 高危操作（退款）HITL 审批 + 最小权限 + Guardrails 是生产底线
> 4. 部署三件套：事件驱动（异步）+ 灰度发布 + 监控告警；迭代靠数据（评估分数）而非感觉

---

**下一模块**：[11-面试高频考点与总结](11-面试高频考点与总结.md) | **返回总览**：[00-Agent开发知识体系总览](00-Agent开发知识体系总览.md)
