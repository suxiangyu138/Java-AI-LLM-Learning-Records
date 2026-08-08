# 04 CrewAI：角色化 Crew 与 Flows

> 定位：框架深潜第二站——角色化多 Agent 框架代表：四大原语（Agent/Task/Crew/Process）、生产化 Flows、2026 checkpointing 与 A2A 增强；原型最快（2026-08 基准）

## 📚 目录

1. [定位与版本窗口](#1-定位与版本窗口)
2. [四大原语：Agent / Task / Crew / Process](#2-四大原语agent--task--crew--process)
3. [Process 两种模式：顺序与层级](#3-process-两种模式顺序与层级)
4. [Flows：2026 生产推荐](#4-flows2026-生产推荐)
5. [Checkpointing 与 Forking](#5-checkpointing-与-forking)
6. [A2A 与协议支持](#6-a2a-与协议支持)
7. [记忆系统](#7-记忆系统)
8. [安全与认证](#8-安全与认证)
9. [Crew Studio 与 Discovery](#9-crew-studio-与-discovery)
10. [局限与适用边界](#10-局限与适用边界)
11. [核心要点](#11-核心要点)

## 1. 定位与版本窗口

| 维度 | 2026 事实 |
|------|----------|
| 定位 | 角色化多 Agent：Researcher/Writer/Reviewer 协作，直觉化 Crew 抽象 |
| 版本 | v1.15.2（2026 年中）；v1.14.2/v1.14.4（2026-04） |
| 规模 | ~52.4k GitHub stars（2026-05） |
| 市场 | 宣称 60% Fortune 500 采用、6000 万+ 月执行（第三方，方向性参考） |
| 依赖 | 独立于 LangChain；Python 3.10-3.13 |
| 性能 | 第三方宣称 5.76x 快于 LangGraph（AWS 基准，方向性参考） |

> 🎯 **一句话**：CrewAI = "给每个 Agent 一个角色，让角色们协作"——研究/写作/审查这类**内容流水线**场景的直觉化首选；2026 年补上 checkpointing、A2A、Flows 生产化后，从"原型工具"走向"生产可用"。

## 2. 四大原语：Agent / Task / Crew / Process

| 原语 | 作用 | 示例 |
|------|------|------|
| Agent | 角色（角色/目标/背景 + 工具 + LLM） | Researcher（调研员） |
| Task | 任务（描述 + 预期输出 + 指定 Agent） | "调研竞品定价" |
| Crew | 一组 Agent + 任务 + 协作流程 | 研究+写作+审查三员小组 |
| Process | 协作方式（sequential / hierarchical） | 顺序执行 / 主管分发 |

```python
from crewai import Agent, Task, Crew, Process

researcher = Agent(role="调研员", goal="调研竞品", backstory="资深分析师",
                   tools=[search_tool], llm="gpt-4o")
writer = Agent(role="写手", goal="撰写报告", backstory="技术编辑")

research_task = Task(description="调研竞品定价策略", expected_output="结构化调研笔记",
                     agent=researcher)
write_task = Task(description="基于调研撰写报告", expected_output="Markdown 报告",
                  agent=writer)

crew = Crew(agents=[researcher, writer],
            tasks=[research_task, write_task],
            process=Process.sequential)   # 或 Process.hierarchical
result = crew.kickoff()
```

## 3. Process 两种模式：顺序与层级

| 模式 | 机制 | 适合 | 对应范式 |
|------|------|------|---------|
| sequential | 任务按声明顺序依次执行，前任务输出传后任务 | 流水线（Handoff 链） | 02 篇 Handoff 链 |
| hierarchical | 自动创建 Manager Agent 分发任务给成员 | 需要主管协调 | 02 篇 Supervisor |

> 💡 hierarchical 模式下 CrewAI 自动生成 Manager——省去手写编排者，但也意味着编排提示词由框架控制，可定制性弱于 LangGraph 显式图。

## 4. Flows：2026 生产推荐

2026 年 CrewAI 的核心架构主张：**生产用 Flows，探索用 Crews**。

```text
Crews  = LLM 驱动的自主角色协作（探索期：灵活但不可预测）
Flows  = 事件驱动、确定性、代码所有的工作流（生产期：可控可测）
```

```python
from crewai.flow.flow import Flow, start, listen
from pydantic import BaseModel

class ReportState(BaseModel):
    topic: str
    report: str = ""

class ReportFlow(Flow[ReportState]):
    @start()
    def research(self):
        result = crew_research.kickoff()
        self.state.report = result.raw
        return result.raw

    @listen(research)
    def write(self, research_output):
        return writer_task.kickoff(inputs={"research": research_output})

flow = ReportFlow()
flow.kickoff()
```

| Flows 能力 | 2026 事实 |
|-----------|----------|
| 事件驱动 | `@start()` / `@listen()` 装饰器链 |
| 生产就绪 | "production-ready Flows"、全局 HITL 配置、流式帧协议 |
| 增强 | 模板化 Flow 动作输入、Flow 定义生成技能 |
| 检查点 | 与 checkpointing 体系打通 |

## 5. Checkpointing 与 Forking

v1.14.2+ 加入（对齐 LangGraph 的持久化能力）：

| 能力 | 说明 |
|------|------|
| 检查点命令 | resume / diff / prune（CLI） |
| 恢复 | `Agent.kickoff(from_checkpoint=...)` |
| Forking | 检查点分叉 + lineage 追踪；树视图 TUI 可编辑输入输出 |
| 版本 | checkpoint 内嵌 `crewai_version`，带迁移框架 |
| 范围 | 扩展至 standalone agents + checkpoint 生命周期事件 |

> 💡 **对齐信号**：CrewAI 2026 补 checkpointing 说明"持久化/恢复"已从 LangGraph 的差异化能力变成多 Agent 框架的**基线能力**。

## 6. A2A 与协议支持

| 能力 | 2026 事实 |
|------|----------|
| A2A | 原生支持：async 链、poll/stream/push 三种更新机制、企业级 A2A 文档 |
| MCP | 原生支持（`crewai-tools[mcp]`） |
| 意义 | Crew 可被跨厂商编排者调用（如 LangGraph 编排者委派 CrewAI Crew）——协议层互操作（07 篇） |

## 7. 记忆系统

| 记忆类型 | 说明 |
|---------|------|
| 短期记忆 | 会话内 |
| 长期记忆 | 跨会话持久化 |
| 实体记忆 | 实体属性提取与存储 |
| 上下文记忆 | 任务上下文汇总 |

- LLM 驱动的统一记忆；第一方向量库集成
- 2026 新增：LLM token 追踪增强（reasoning tokens + cache creation tokens）

> ⚠️ 行业共识（2026）：**没有任何框架的长期记忆达到生产级**（含语义检索与剪枝）——跨框架的未解问题，生产上需自行设计记忆策略。

## 8. 安全与认证

| 项 | 2026 事实 |
|----|----------|
| plan-execute 模式 | 规划与执行分离，第三方宣称多步推理幻觉循环 -40-60%（方向性） |
| Plus API tokens | 凭证作用域、可过期 token（按端点/Agent ID/时间窗），替代裸 API Key |
| 安全修复 | 代码解释器沙箱逃逸（路径穿越）、NL2SQLTool 加固（只读默认/参数化） |
| 漏洞治理 | pypdf/requests/cryptography/transformers 等依赖链修复 |

> ⚠️ 使用代码解释器/Crew Studio 时确认沙箱版本——2026 年修复过沙箱逃逸 CVE 类问题，升级到最新版。

## 9. Crew Studio 与 Discovery

| 产品 | 说明 |
|------|------|
| Crew Studio | 自动化 Agent 构建器：自然语言描述工作流即生成；基于 70 万+ 社区用例模式；1000+ 连接器；三种节点（单 Agent/Crew/Router）；组织级 Agent 仓库 |
| CrewAI Discovery | 多信号匹配 + 群体分析 + 模式识别（数十亿生产执行数据），推荐适合自动化的业务场景 |

## 10. 局限与适用边界

| 局限 | 表现 | 应对 |
|------|------|------|
| 可观测弱 | 日志基础、错误恢复追踪弱于 LangGraph | 生产接 LangSmith/自建追踪 |
| 抽象固执 | 角色化抽象与非常规生产需求冲突 | 复杂场景迁移 LangGraph |
| token 开销 | 角色扮演（role/backstory）增加提示词开销 | 精简角色描述 |
| 性能数据 | 5.76x 等为第三方基准 | 用真实工作流实测 |
| 层级 Manager | 编排提示词由框架控制，可定制弱 | 需要显式编排时用 LangGraph |

> 🎯 **适用结论**：CrewAI = **快速原型 + 内容流水线**（数小时出活）；验证通过后若进入长流程/复杂状态/强可观测需求，按 09 篇迁移路线评估是否迁 LangGraph——CrewAI 自身也提供"Flows 生产化"路径作为留在原框架的选项。

## 11. 核心要点

> 🎯 **核心要点**：
> 1. 四大原语：Agent / Task / Crew / Process（sequential / hierarchical）
> 2. 2026 主张：**生产用 Flows（确定性事件流），探索用 Crews（自主协作）**
> 3. v1.14.2+ checkpointing/forking 补齐持久化；A2A/MCP 原生支持跨厂商互操作
> 4. 原型最快（数小时），但可观测与编排可控性弱于 LangGraph——验证后评估迁移

---

**上一模块**：[03 LangGraph](03-LangGraph：多%20Agent%20生产编排标杆.md)　**下一模块**：[05 AG2 与 Agent Framework](05-AG2%20与%20Microsoft%20Agent%20Framework：AutoGen%20遗产的继承者.md)　**返回总览**：[00 总览](00-总览：多%20Agent（Multi‑Agent）框架知识体系.md)

## 【参考来源】

- [CrewAI Changelog (v1.15.2)](https://docs.crewai.com/v1.15.2/en/changelog)
- [CrewAI v1.11.0rc1: Plan-Execute Pattern and Enterprise Auth (AIDevSetup)](https://aidevsetup.com/insider/crewai-v1-11-0rc1-plan-execute-pattern-and-enterprise-auth)
- [CrewAI Studio: The Automated Agent Builder](https://crewai.com/blog/crew-studio-automated-agent-builder)
- [Introducing CrewAI Discovery](https://crewai.com/blog/crewai-discovery)
- [The open-source multi-agent framework cements its position (The Agent Times)](https://theagenttimes.com/articles/the-open-source-multi-agent-framework-cements-its-position-as-a-coordination-bac)
- [Multi-Agent Orchestration Frameworks 2026 (Presenc AI)](https://presenc.ai/research/multi-agent-orchestration-frameworks-2026)
- [CrewAI 1.15.2 on PyPI (newreleases.io)](https://newreleases.io/project/pypi/crewai/release/1.15.2)
