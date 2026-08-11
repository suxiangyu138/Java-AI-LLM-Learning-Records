# 05 LangChain/LangGraph 框架化

> 把 Level1 手写的编排循环升级为 LangGraph 状态图：检索、生成、工具调用成为显式节点，流程可打断、可观测、可复用。手写是为了懂原理（Level1），框架化是为了工程效率与演进空间（本模块）。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [框架化决策：为什么是 LangGraph](#2-框架化决策为什么是-langgraph)
3. [核心概念：State、Node、Edge](#3-核心概念statenodeedge)
4. [迁移：手写循环 → 状态图](#4-迁移手写循环--状态图)
5. [流式、条件分支与工具调用](#5-流式条件分支与工具调用)
6. [不用框架的边界](#6-不用框架的边界)
7. [常见坑](#7-常见坑)

---

## 1. 目标与验收

本模块的产出：用 LangGraph 重写编排层——一个三节点图（检索 → 生成 → 工具回调分支），替换 04 篇 service 里的手写 ask()。验收标准：**能画出本项目的图（节点、边、条件分支）**；**能说清 LangGraph 与手写循环的本质区别**；**graph 能被流式调用且单步可观测**。版本基线（2026-08）：LangGraph 1.x（2026-05 发布 1.0 Alpha，编排引擎非破坏性升级；与 LangChain v0.8+ 生态配合使用）。

## 2. 框架化决策：为什么是 LangGraph

Level1 的 while 循环已经能跑通 RAG + 工具调用，为什么还要框架？三个理由：**流程显式化**——手写循环的"下一步去哪"藏在代码分支里，图的节点与边一目了然（面试画图两分钟讲清全流程）；**工程能力内置**——重试、超时、中断（human-in-the-loop）、状态检查点（断点续跑）框架原生支持，手写要几百行；**生态演进**——2026 年 LangChain 生态已经全面图化（Legacy LLMChain 等旧 API 淘汰），Agent/RAG 的标准实现都在 LangGraph 上，跟随生态才能读得懂开源项目。代价也明确：**抽象层增加**（调试要会读框架栈）——所以本模块的迁移路径是"先画图，再落码"，每一步都能对照 Level1 的代码。

决策边界：**线性流程（无分支无循环）不需要 LangGraph**——简单 chain 用函数组合即可；**有分支、有循环、有人工介入、要检查点**——上 LangGraph。本项目的"检索 → 生成 → 工具回调 → 重试"正是图场景。

## 3. 核心概念：State、Node、Edge

LangGraph 三个核心概念，与手写代码一一对应：**State**（状态——图内共享的数据容器，类似手写循环里的 messages/context 变量）；**Node**（节点——一个函数：输入 State，输出 State 的更新；对应手写循环里的一个步骤）；**Edge**（边——节点间的连接，条件边决定分支走向；对应 if 分支）：

```python
from typing import TypedDict
from langgraph.graph import StateGraph, END

class RagState(TypedDict):           # State：图内共享数据
    question: str
    history: list[dict]
    context: list[str]
    tool_calls: list[dict]
    answer: str | None

def retrieve_node(state: RagState) -> dict:   # Node 1：检索（06 篇）
    context = hybrid_search(state["question"], top_k=50)
    return {"context": rerank(context, top_k=5)}

def generate_node(state: RagState) -> dict:   # Node 2：生成（含工具回调判断）
    answer = call_model(state["history"], state["context"], state["question"])
    return {"answer": answer}
```

理解 State 的关键：**图不共享全局变量，一切数据走 State 流转**——这让图可以随时暂停/恢复（State 序列化即检查点）、可以单步调试、可以并行分支。对比手写循环：变量在函数内传递（隐式），State 是显式的"数据总线"。

## 4. 迁移：手写循环 → 状态图

把 04 篇的 ask() 迁成图，迁移的步骤就是"识别手写代码里的步骤与分支"：

```python
from langgraph.graph import StateGraph, END

def build_graph():
    g = StateGraph(RagState)

    g.add_node("retrieve", retrieve_node)
    g.add_node("generate", generate_node)
    g.add_node("tool", tool_node)            # 工具执行节点（Level1 07 的循环体）

    g.add_edge("retrieve", "generate")       # 顺序边：先检索后生成
    g.add_conditional_edges(                 # 条件边：生成结果决定下一步
        "generate",
        route_after_generate,                # 路由函数：返回 "tool" 或 END
        {"tool": "tool", "end": END},
    )
    g.add_edge("tool", "generate")           # 工具执行后回到生成节点（循环！）
    g.set_entry_point("retrieve")            # 图入口
    return g.compile()

app = build_graph()

async def ask(session_key: str, question: str, history: list[dict]) -> str:
    result = await app.ainvoke(
        {"question": question, "history": history, "context": [], "tool_calls": []})
    return result["answer"]
```

对照迁移：**顺序部分**（检索→生成）→ 顺序边；**while 循环**（工具调用→回传→再生成）→ generate→tool→generate 的循环边；**终止条件**（不再调工具）→ 条件边路由到 END。**看懂这张图就理解了这个项目 80% 的 AI 逻辑**——面试时在纸上画它，胜过背十行代码。生成的 answer 走 State 落库（04 篇的 save_message 移到调用侧）。

## 5. 流式、条件分支与工具调用

图带来的三个工程能力直接解决 Level2 痛点：**流式**——`app.astream(...)` 按节点产出（mode="updates" 输出每个节点的 State 更新，前端可做"正在检索…"的节点级提示，体验远超纯 token 流）；**条件分支**——`route_after_generate` 是路由函数（读 State 判断是否要调工具，返回边名），工具调用从手写 for 循环变成图的一个节点（node 里循环执行并行 tool_calls）；**检查点**——`compile(checkpointer=...)` 后支持中断与断点续跑（"检索结果太差，请用户确认是否继续"这类人工介入场景），Level3 的多 Agent 场景会重度使用。

```python
async for chunk in app.astream(state_input, stream_mode="updates"):
    for node_name, update in chunk.items():
        log.info("节点 %s 完成: %s", node_name, list(update.keys()))   # 节点级可观测
```

Stream 模式的选型：**updates（节点粒度）**给界面做"步骤提示"，**messages/tokens（token 粒度）**给打字机——两者组合是本项目流式的完整形态（03 篇 SSE 接口消费 updates 流，前端展示步骤 + 打字机）。

**检查点与人机协同**：`compile(checkpointer=SqliteSaver(...))` 后，State 在每一步落盘（SQLite 检查点）——中断恢复（`interrupt_before=["generate"]` 在生成前暂停，供人工确认检索结果）、断点续跑（崩溃后从上次节点继续，不重复调用模型）。本项目的两个实用场景：**检索结果确认**（检索质量差时暂停让用户调整问题，而不是硬生成——对应 06 篇的"检索是 73% 故障根源"，人机协同是第一道兜底）；**成本护栏**（工具调用循环加 interrupt 上限，避免失控循环烧钱——Level1 07 的 max_iterations 的图化实现）。

**调试可视化**：LangGraph 自带可视化——`from IPython.display import Image; Image(app.get_graph().draw_mermaid_png())` 直接画出节点-边图（Mermaid 格式），或者用 LangGraph Studio（桌面调试器：单步、查看 State、编辑输入重放）。调试三板斧：**单步跑**（`app.astream(..., stream_mode="debug")` 逐步看 State 变化）；**断点看 State**（在节点函数里打日志，08 篇的节点日志带上 trace_id）；**重放**（检查点让同一个输入可重复执行——模型调用可 mock 后重放是调试 Agent 的黄金姿势）。

## 6. 不用框架的边界

框架化不等于"处处 LangGraph"，明确三条边界：**纯函数逻辑不图化**（分块、清洗、格式转换——普通函数即可，图上全是节点反而难读）；**单步调用不图化**（一次问答无分支——直接调模型函数，图只承载有循环/分支的流程）；**引入框架前先手写一遍**（本层顺序是刻意的：Level1 手写懂原理，Level2 框架化懂工程——直接上框架的人调不动 bug，因为不理解底层发生了什么）。边界问题的本质是**抽象的代价**：图的调试比函数难（要跟 State），所以图只用在"图的收益 > 调试成本"的流程上。**框架化的验收对照**：迁移前后各跑一遍 06 篇的评估集——框架化本身不该改变检索质量（质量由检索组件决定），指标应持平或更好（工具节点带来的重试可能改善）；**如果指标明显下降，查 State 传递**（上下文没进生成节点——State 字段漏传是最常见的新手 bug），而不是怀疑框架。这个对照让"框架化"变成可验证的工程决策，而非"跟风换技术"。

## 7. 常见坑

**State 类型不严谨**：TypedDict 字段缺失导致节点 KeyError——所有 State 字段在图上定义完整，节点返回的键必须是 State 已有键。

**条件边返回不存在的边名**：route 函数返回 "tool" 但图里没注册——路由函数与 add_conditional_edges 的映射表同步维护。

**工具节点忘记回传**：tool node 改了 State 但没把结果拼进 messages——工具结果必须进入 State 再交给生成节点（对照 Level1 07 的 messages.append 配对）。

**图只 invoke 不流式**：async 接口里用同步 invoke 阻塞事件循环——统一 astream/ainvoke（async 入口），同步调用只留给测试。

**版本线混乱**：LangChain/LangGraph 包家族版本联动——uv.lock 锁定后整体升级（`uv add langgraph@latest langchain-core@latest` 一起升），混搭老新版本是 2026 年最常见的框架类报错来源。

**框架报错看不懂**：框架栈的 Traceback 很深——先找"你的文件"的帧（过滤框架内部帧），再把 State 关键字段打印出来（08 篇的节点日志），**框架类问题 80% 是 State 传参问题**（字段漏传/类型不对），不是框架 bug。

> 🎯 **核心要点**：框架化的本质是**把流程从"代码分支"升级为"显式状态图"**——State 是数据总线、Node 是步骤、Edge 是路由。图跑通的那一刻，你的编排层从"我写的循环"变成"可画、可断、可观测的系统"，这是简历里"使用 LangGraph 编排 RAG 流程"这句话的全部底气。

---

**下一模块**：[06 检索质量升级](./06-检索质量升级.md) | **返回总览**：[Level2 总览](./00-Level2%20工程化完整项目%20总览.md)

【参考来源】
- [LangGraph 官方文档](https://langchain-ai.github.io/langgraph/)
- [LangChain vs LangGraph 2026: Which to Use for Enterprise Agents](https://superml.dev/langchain-vs-langgraph-2026-enterprise-agents)
- [LangChain/LangGraph 1.0 Alpha 发布：双语言支持与架构革新](https://developer.baidu.com/article/detail.html?id=6997561)
- [Building Real-World Agentic RAG Systems](https://learned-memory.kit.com/posts/building-real-world-agentic-rag-systems)
