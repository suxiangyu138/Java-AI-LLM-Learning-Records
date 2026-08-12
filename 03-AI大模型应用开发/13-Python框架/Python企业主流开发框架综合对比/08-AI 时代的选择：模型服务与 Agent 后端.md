# AI 时代的选择：模型服务与 Agent 后端

> 2026 年框架选型的最大变量是 AI：FastAPI 拿下模型服务（42% ML 工程师使用，OpenAI/HuggingFace 背书），Django 统治 AI 平台的后台与数据管理。本章给出 AI 场景的完整框架地图——模型服务、RAG 后端、Agent 平台、混合模式与 Django Ninja 桥接

---

## 📚 目录

1. [AI 场景框架地图：三种角色](#1-ai-场景框架地图三种角色)
2. [模型服务：FastAPI 的统治区](#2-模型服务fastapi-的统治区)
3. [AI 平台后台：Django 的价值区](#3-ai-平台后台django-的价值区)
4. [Django + FastAPI sidecar 混合模式](#4-django--fastapi-sidecar-混合模式)
5. [Django Ninja：全栈到 API 的桥接](#5-django-ninja全栈到-api-的桥接)
6. [与 LLM 生态的集成：SDK / LangChain / 流式](#6-与-llm-生态的集成sdk--langchain--流式)
7. [核心要点与思考题](#7-核心要点与思考题)

---

## 1. AI 场景框架地图：三种角色

AI 应用里 Web 框架扮演三种角色，各有归属：

```text
① 模型服务层（FastAPI 统治）
   vLLM/推理引擎包装、OpenAI 兼容 API、流式输出、Embedding 服务
② 应用后端层（FastAPI / Django 可选）
   RAG 管道、Agent 编排、工具调用、对话会话管理
③ 平台与后台层（Django 统治）
   数据管理（知识库/用户/任务）、运营后台、审计、报表
```

- **① 层**：技术形态固定（异步 + 流式 + 协议兼容）→ FastAPI 无悬念
- **② 层**：取决于团队与数据层——纯 API 服务 FastAPI，带管理需求 Django（配合 Ninja）
- **③ 层**：Admin + 认证 + 后台任务 → Django 无悬念

**"AI 应用 = 三层框架分工"是 2026 年的标准答案**——面试讲 AI 项目时按这个地图组织，比"我用 X 框架做了 AI"清晰得多。

## 2. 模型服务：FastAPI 的统治区

模型服务的技术形态与 FastAPI 的能力高度重合：

- **流式输出**：LLM 逐 token 推送（`StreamingResponse`）——体验刚需，FastAPI 原生
- **并发推理**：多请求共享 GPU 推理（vLLM 连续批处理），FastAPI 异步并发天然契合
- **协议兼容**：OpenAI 兼容 API 是事实标准（本仓库「LiteLLM」体系详述），FastAPI 几行实现 `/v1/chat/completions`
- **版本迭代**：模型日更周更，轻量框架的快速重启与无状态设计匹配

```python
# 模型服务的标准形态：OpenAI 兼容 + 流式（FastAPI）
@app.post("/v1/chat/completions")
async def chat(request: ChatRequest):
    async def gen():
        async for chunk in model.stream(request.messages):
            yield sse_format(chunk)
    return StreamingResponse(gen(), media_type="text/event-stream")
```

2026 年事实：**vLLM/Ollama 等推理引擎的官方 API 示例、LangChain 的默认服务形态、各大模型厂商的兼容层，全部以 FastAPI 形态出现**——模型服务场景选 FastAPI 不是偏好，是"生态默认"。

## 3. AI 平台后台：Django 的价值区

AI 平台（RAG 知识库、Agent 工作台、微调任务平台）的"非模型"部分恰好是 Django 甜区：

- **数据管理**：知识库文档、用户、会话、任务——Admin 一键管理，运营人员零开发成本
- **后台任务**：文档切分/向量化/索引是典型的后台任务——Django 6.0 内置任务框架正好承接
- **认证权限**：多租户平台的内置用户/组/权限——开箱即用
- **审计报表**：Admin + 模板出运营报表，比自建前端快一个量级

典型架构：**Django 管平台（数据/任务/后台）+ FastAPI 管服务（推理/检索/对话）**——数据落在同一数据库（Django 模型或 SQLAlchemy 皆可），任务队列打通两边。这是 2026 年"AI 平台"类项目被反复验证的组合，也是本仓库 04 层项目体系的现实形态。

一个具体的平台形态示例：RAG 知识库平台——**Django 侧**：文档管理（Admin 上传/审核）、用户与权限（内置 auth）、切分与向量化任务（6.0 内置后台任务 + Celery 兜底重型任务）、用量统计报表；**FastAPI 侧**：检索接口（向量库查询 + 重排）、对话接口（LLM 调用 + 流式 SSE）、Embedding 批量接口。**两边的数据契约**：文档表、向量索引、会话表共享同一数据库；Django 写、FastAPI 读（只读连接池）或全部经任务队列异步流转。这套形态的好处是"平台能力"与"服务能力"各自独立演进——后台改版不动服务，模型升级不碰后台，**运营与研发两个团队可以并行开工**，这是单一框架做不到的协作红利。

## 4. Django + FastAPI sidecar 混合模式

两大框架并存的成熟模式——**sidecar（边车）**：Django 为主应用，FastAPI 作为独立进程挂载异步接口：

```text
反向代理（Nginx）
 ├── /admin/* /api/v1/*   → Django（业务 + 后台 + 认证）
 └── /v1/chat/* /v1/stream/* → FastAPI（异步 + 流式 + 模型服务）
```

实现要点：

- **反向代理分流**：按路径前缀路由到不同进程（Nginx/Caddy 标准配置）
- **认证打通**：JWT 由 Django 签发，FastAPI 侧校验同一密钥（共享 `SECRET_KEY` 或公钥）
- **数据共享**：同一数据库——Django ORM 管写，FastAPI 侧 SQLAlchemy 读（或 FastAPI 只走消息队列不直连库）
- **部署**：两个进程各自容器化、各自扩容——FastAPI 侧按推理流量扩，Django 侧按业务流量扩

**判断**：sidecar 适合"Django 存量 + 新增异步需求"；**绿地项目且无 Django 存量时，直接用 FastAPI 全栈（加 SQLAlchemy + 第三方后台）更简单**——sidecar 是"融合"不是"默认"。

sidecar 的**运维成本**要说在前面：两个进程 = 两套部署、两套日志、两套监控、两套版本发布——混合架构省了"重写"的成本，却加了"双运维"的成本。2026 年的实践建议是**用一套编排管到底**：容器化后同一 Helm Chart/Compose 定义两个服务，日志统一收进同一平台（按 service 字段区分），发布用同一流水线的两个 job——**把"两个系统"的管理成本压到"一个系统"的体验**，sidecar 才划算。做不到统一编排的团队，评估"单框架 + 低代码后台"路线可能更省。

## 5. Django Ninja：全栈到 API 的桥接

**Django Ninja**（Django 生态的 API 框架，Pydantic 驱动）是 2026 年的重要桥接选项：

```python
# Django Ninja：Django 全栈 + FastAPI 式契约
from ninja import NinjaAPI
api = NinjaAPI()

@api.get("/orders/{oid}", response=OrderOut)   # Pydantic 响应 + 自动 OpenAPI
def get_order(request, oid: int):
    return Order.objects.get(id=oid)            # Django ORM 直接复用
```

它解决的核心矛盾：**Django 团队想要"类型契约 + 自动文档"但不想失去 ORM/Admin**。对比 DRF：Ninja 的 Pydantic 驱动与现代 API 体验（自动 OpenAPI、异步支持）更接近 FastAPI；对比 FastAPI：Ninja 保留 Django 全栈（ORM/Admin/认证）不割裂。**2026 年的选择矩阵：Django 项目 API 化 → 首选 Ninja；绿地 API 项目 → 直接 FastAPI**。

## 6. 与 LLM 生态的集成：SDK / LangChain / 流式

三框架与 LLM 生态的集成成熟度（2026）：

| 集成面 | Django | FastAPI | Flask |
|--------|:---:|:---:|:---:|
| OpenAI 官方 SDK | ✅ 可用 | ✅ 原生异步 | ✅ 可用 |
| LangChain（python 版） | ✅ 可用 | ✅ 推荐形态 | ✅ 可用 |
| 流式响应 | 🧩 异步视图有限 | ✅ 原生 | 🧩 生成器 |
| WebSocket（Agent 对话） | 🧩 Channels | ✅ 原生 | 🧩 Flask-Sock |
| vLLM/Ollama 对接 | ✅ 可用 | ✅ 生态默认 | ✅ 可用 |

结论：**SDK 层三者都能用（Python 语言优势），差异集中在"流式与长连接"**——Agent 对话（WebSocket 双向）、LLM 流式（SSE）在 FastAPI 是原生姿势，Django（Channels）与 Flask（扩展）都要绕路。**"我的 AI 产品需要流式对话"——这一条就足以把 ② 层应用后端推向 FastAPI**（或 Django + FastAPI sidecar）。

补充一个 2026 年正在升温的**Agent 平台模式**：以 FastAPI 为核心构建"Agent 网关"——统一接收对话请求、按意图路由到不同 Agent（检索型/工具型/多模型）、流式回传推理过程（`thought`/`tool_call` 事件走 SSE）、会话记忆与用量计费落库。这套网关用 Django 也能做（Channels + DRF），但**事件流式 + 工具调用的并发编排在 FastAPI 的异步模型里写起来自然一个量级**——OpenAI 兼容的流式协议（`data:` 分块）几乎是为 async 生成器量身定做的。对本仓库「03-Agent开发」与「Function Calling」体系的学习者，这个模式就是"把 Agent 知识装进 Web 框架"的完整落点。

> 🎯 **核心要点**：AI 场景框架地图 = 模型服务 FastAPI（统治区）+ 应用后端 FastAPI/Django（按团队）+ 平台后台 Django（统治区）；sidecar 混合模式用于"Django 存量 + 异步新增"；Django Ninja 是"全栈要契约"的桥接；流式与 WebSocket 需求是 AI 项目选 FastAPI 的决定性理由。

## 7. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. 三层地图：模型服务 FastAPI（vLLM/流式/兼容 API）、平台后台 Django（Admin/任务/认证）、应用后端按团队选；
> 2. sidecar = Django 主 + FastAPI 边车（反向代理分流、JWT 共享、独立扩容），存量融合而非绿地默认；
> 3. Django Ninja 让"全栈 + 现代 API 契约"兼得；流式/WebSocket 需求是 AI 项目选 FastAPI 的决定性理由。

**思考题**：

1. AI 应用的 Web 框架分哪三层？各归谁？（→ 1 节）
2. 模型服务为什么是 FastAPI 的"统治区"？（→ 2 节）
3. sidecar 的认证与数据如何打通？（→ 4 节）
4. Django 项目 API 化选 DRF 还是 Ninja？（→ 5 节）

---

**下一模块**：[09-选型决策：场景剖析与迁移路线](09-选型决策：场景剖析与迁移路线.md)｜**返回总览**：[00-Python企业主流开发框架综合对比知识体系总览](00-Python企业主流开发框架综合对比知识体系总览.md)

---

## 参考来源

- [FastAPI 2026: Why 38% of Python Devs Switched（2026）](https://www.programming-helper.com/tech/fastapi-2026-python-api-framework-ai-ml-adoption-enterprise)——AI 采用数据
- [How FastAPI Became Python's Fastest-Growing Framework（DZone）](https://dzone.com/articles/how-fastapi-became-pythons-fastest-growing-framework)——AI 场景分析
- [Django Ninja 官方文档](https://django-ninja.dev/)——桥接方案权威说明
- [Python 异步 + FastAPI 深潜体系（流式章节）](../../01-Python语言/Python 异步 + FastAPI/00-Python异步与FastAPI知识体系总览.md)——流式实现细节
