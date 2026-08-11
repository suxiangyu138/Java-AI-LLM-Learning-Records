# 04 Python AI 服务实现

> Python 侧的核心产出是"思考引擎"：FastAPI 提供服务外壳，LangGraph 承载 Agent 编排，RAG 供给知识，函数调用连接 Java 工具——四层各自独立、又串成一条决策链。

## 📚 目录

1. [服务骨架](#1-服务骨架)
2. [Agent 编排（LangGraph）](#2-agent-编排langgraph)
3. [RAG 检索](#3-rag-检索)
4. [函数调用：工具循环](#4-函数调用工具循环)
5. [SSE 流式返回](#5-sse-流式返回)
6. [模型接入](#6-模型接入)
7. [核心要点](#7-核心要点)

---

## 1. 服务骨架

Python 侧按职责拆三个子模块：`app/router`（HTTP 入口）、`app/agent`（编排逻辑）、`app/rag`（知识库检索）。入口保持最小化，一个对话接口加两个 RAG 管理接口：

```python
# app/main.py —— 双栈版 FastAPI 骨架
from fastapi import FastAPI
from app.router import chat, rag

app = FastAPI(title="ai-service", version="1.0.0")
app.include_router(chat.router, prefix="/api/v1")
app.include_router(rag.router, prefix="/api/v1/rag")
```

三个接口的边界要清晰：`POST /chat`（对话入口，Java 网关唯一调用点，SSE 流式）、`POST /rag/ingest`（文档入库，Java 上传文件后调用）、`POST /rag/search`（检索调试用）。服务只接受 Java 网关的调用——用网关卡鉴权（校验 `X-Service-Token`），不对外暴露，这是 07 篇安全设计的配合点。

反向调用 Java 的客户端同样要工程化：`httpx.AsyncClient` 做连接池复用（避免每次工具调用都建连，建连开销在流式高并发下会被放大），统一注入 service token 请求头，超时按工具分级（查询类 2s、写操作 3s）。客户端封装成 `java_client.py` 单例模块，所有工具回调走同一出口——超时、重试、错误码解析只维护一份，这是 Python 侧少踩坑的关键工程习惯。

## 2. Agent 编排（LangGraph）

2026-08 基准下 Agent 编排用 LangGraph 1.2.x（LangChain 1.3.x 已稳定，LangGraph 承担一切非平凡工作流）。选 LangGraph 而非 LangChain 原生链式的理由，就是双栈项目最好的面试素材：**有状态、可持久化、支持人工介入**。

- **StateGraph 图编排**：节点（意图识别 → 检索 → 工具决策 → 生成）与条件边（是否调用工具）比链式表达更清晰；
- **Checkpoint 持久化**：对话状态存检查点，服务重启或崩溃后可以从上次检查点恢复——这正是"双栈拆分离线"需要的容错能力；
- **Interrupt 人工介入**：退款等敏感动作可以中断图执行、等待人工确认后恢复（配合 03 篇 Java 侧的人工确认状态位）；
- **子图与并行**：Fan-out/Fan-in 并行检索（向量 + BM25 同时查），汇合后送入生成节点。

最小可用图只有四个节点，先跑通再加分支——初学者最大的坑是一上来画复杂图，调试成本爆炸。

## 3. RAG 检索

知识问答链路：文档入库（分块 → 向量化 → 写 Milvus + ES）→ 查询（向量召回 + BM25 召回 → 融合重排 → 注入 Prompt）。

**混合检索是默认配置**而非可选优化：向量检索抓语义、BM25 抓精确关键词（订单号、型号、政策条款编号），EnsembleRetriever 加权融合（如向量 0.7 + BM25 0.3）。注意点：BM25 检索的字段名必须与 ES 索引映射严格一致，字段名拼写错误会导致静默返回零结果——这是真实线上事故的高发点，排查时先查索引映射。

检索结果注入 Prompt 时要设**注入预算**：top-k 固定 5、每块截断至 512 token，防止长文档把上下文塞满挤掉系统提示——注入预算与分块策略一样，是检索效果调优的日常手段，也是面试能讲出"我调过检索"的证据。

**分块策略**与仓库 RAG 体系一致：按章节结构切分 + 重叠 10-15%，先调分块再调模型，召回率提升的性价比最高。入库接口设计成幂等的：以文档 ID 为唯一键，重复 ingest 覆盖旧向量（先删后插），避免知识库脏数据。

## 4. 函数调用：工具循环

Python 侧不直接操作业务数据，而是通过工具声明回调 Java 的 Tool API，这是双栈的接缝所在：

```python
# app/agent/tools.py —— 工具声明 + 回调 Java
from langchain_core.tools import tool

@tool
def query_order(user_id: int, order_id: str) -> dict:
    """按订单号查询订单状态与物流信息。参数：user_id 用户ID（必填），order_id 订单号（必填）。"""
    resp = java_client.post("/api/v1/tools/order/query",
                            json={"userId": user_id, "orderId": order_id})
    resp.raise_for_status()
    return resp.json()   # {"code":0,"data":{...}} → 模型可读的结构化结果
```

工具声明走 LangChain 的 `@tool` 装饰器（自动生成 JSON Schema），函数 docstring 即模型的工具描述——描述质量直接决定模型何时调用该工具，遵循"做什么、何时用、参数边界"三要素。工具循环由 LangGraph 的 ToolNode 驱动：模型返回 `tool_calls` → 执行 → 结果回传 → 模型继续生成或再次调用。两个工程要点：工具调用要设超时（Java 侧假死不能拖死 Agent 循环）；工具返回结构化错误（`{"code": 4001}`）回传模型，让模型根据错误信息重新规划，能消除八成以上的"模型卡死在错误参数"问题。

## 5. SSE 流式返回

对话接口用 SSE（Server-Sent Events）逐 token 返回，FastAPI 原生支持：

```python
from fastapi.responses import StreamingResponse

@app.post("/chat")
async def chat(req: ChatRequest):
    async def event_stream():
        async for chunk in agent.astream(req.messages):
            yield f"data: {chunk.json()}\n\n"   # 每帧一个 JSON（token/工具状态/终态）
    return StreamingResponse(event_stream(), media_type="text/event-stream")
```

流式帧设计要区分三种事件：`token`（生成内容增量）、`tool_call`（正在调用某工具，前端可展示"正在查询订单…"）、`done`（终态，携带完整会话信息）。这样前端既能逐字展示回答，又能展示工具调用过程——后者是演示与面试的加分点。心跳与断连重连在 05 篇详述。

## 6. 模型接入

模型接入统一走 OpenAI 兼容协议（`base_url` + `api_key` + 模型名），DeepSeek、Qwen 等主流模型全部兼容，**切换模型只改环境变量，Python 代码零改动**——这是双栈架构"敏态"的直接体现。

```python
# .env 示例 —— 模型即配置
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_MODEL=deepseek-chat
# LLM_BASE_URL=http://localhost:11434/v1   # 本地 Ollama 切换只需改这里
# LLM_MODEL=qwen2.5:7b
```

两个必踩的坑提前预警：模型名要写供应商真实支持的 ID（写错会超时或 404，且错误信息常被路由到错误供应商）；带思考链的模型（如 DeepSeek 推理模型）在工具调用循环中必须回传 `reasoning_content`，否则请求被拒。本地开发用 Ollama 起步（零成本），上线切云端 API，这套双轨也是面试的叙事素材。

模型层再补两个生产设计：**fallback 链**——主模型（如 DeepSeek）超时或报错时自动切备用模型（如 Qwen），一次对话最多降级一次，降级事件记入日志与指标（备用模型的回答质量通常有差异，要能事后评估）；**成本感知**——每次对话结束统计 token 消耗并随 `done` 帧回传网关，网关按月汇总成本报表，超预算自动降级到小模型或限流（与 08 篇成本治理衔接）。这两个设计与"模型即配置"合起来，就是完整的模型治理三件套：可切换、可降级、可计量。

## 7. 核心要点

1. LangGraph 选型理由：有状态、检查点持久化、人工介入——三条都是双栈场景刚需。
2. 混合检索默认开：向量 0.7 + BM25 0.3，字段名一致性是排查重点。
3. 工具循环两条铁律：工具调用设超时、错误转 JSON 回传模型。
4. SSE 三帧类型（token / tool_call / done）让前端能展示完整思考过程。
5. 模型即配置：OpenAI 兼容协议 + 环境变量切换，是"敏态大脑"的落地证明。

> 🎯 **核心要点**：Python 侧的面试叙事是"**把不可靠的模型决策变成可控的工程流程**"——图编排确定流程、检查点保证可靠、工具循环限定动作、SSE 暴露过程。每一个组件都对应一个真实故障场景的解决方案，而不是炫技。

---

**下一模块**：[05 双栈通信与接口设计](./05-双栈通信与接口设计.md) | **返回总览**：[双栈联合实战总览](./00-双栈联合实战总览.md)
