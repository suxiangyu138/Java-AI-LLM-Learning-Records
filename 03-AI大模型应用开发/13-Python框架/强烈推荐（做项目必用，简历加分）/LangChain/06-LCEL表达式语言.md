# 06 - LCEL 表达式语言

> 本体系第六课：LangChain 的组合魔法——LCEL 管道、复杂组合、内置能力——"`prompt | model | parser` 一行拼出链——LCEL 是 1.x 的链标准"

---

## 📚 目录

1. [LCEL 的定位](#1-lcel-的定位)
2. [基础管道组合](#2-基础管道组合)
3. [复杂组合模式](#3-复杂组合模式)
4. [LCEL 的内置能力](#4-lcel-的内置能力)
5. [练习 5 题](#5-练习-5-题)
6. [本节验收](#6-本节验收)

---

## 1. LCEL 的定位

**LCEL（LangChain Expression Language）= LangChain 的表达式语言——用 | 组合组件**：

```text
LCEL 的定位
├── 本质：声明式组合语法（管道符 | 连接 Runnable）
├── 地位：1.x 的链标准（旧 Chain 类 LLMChain/SequentialChain 移 classic）
├── 理念：组合优于继承（不写类——拼积木）
├── 产物：组合结果还是 Runnable（02 篇——可再组合）
└── 示例：chain = prompt | model | parser（一条链 = 一次 invoke）
    ——"LCEL = 搭积木的语言——不写胶水代码，用 | 拼"
```

**定位心智**：**"LCEL 的记忆：'| 管道——声明式拼链——1.x 的链标准'"**——"**为什么取代旧 Chain 类：旧写法（LLMChain 类）——要建类/写回调（重）；LCEL——一行 | 拼出来（轻）——'声明式优于命令式（告诉它'是什么'，不写'怎么做'）'"**（"1.x 的迁移：LLMChain(...) → prompt | model | parser（01 篇 v1.0 变化②）——**'新代码一律 LCEL'"**）；**LCEL 的定位演变**——"v1.0 之后（Agent 优先时代）：LCEL 的价值转向'为 Agent 建工具和子任务链'——复杂流程交给 LangGraph（08 篇）——**'LCEL 管'链'、LangGraph 管'图'——分工清晰'"**（"学习重点（2026 官方建议）：LCEL + RAG + LangGraph 三件事——**'LCEL 是其中第一件（基础）'"**）。

## 2. 基础管道组合

**基础管道——三种常用链型**：

```python
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import StrOutputParser

prompt = ChatPromptTemplate.from_messages([("user", "写一段关于{topic}的介绍")])
model = ChatOpenAI(model="gpt-4o-mini")
parser = StrOutputParser()

# 链型一：线性链（最基础——prompt | model | parser）
chain = prompt | model | parser
result = chain.invoke({"topic": "AI"})       # 一次走完（提示词 → 模型 → 解析）

# 链型二：带业务函数的链（RunnableLambda——02 篇）
from langchain_core.runnables import RunnableLambda

def clean(text: str) -> str:
    return text.strip()                       # 业务函数（进管道）
chain2 = prompt | model | parser | RunnableLambda(clean)
result2 = chain2.invoke({"topic": "AI"})     # 输出经过 clean

# 链型三：带结构化输出的链（05 篇）
# chain3 = prompt | model.with_structured_output(MyModel)
```

**管道心智**：**"管道的记忆：'prompt | model | parser 是标准模板——加业务用 RunnableLambda'"**——"**标准模板的变形：加解析（Str/结构化）、加业务（Lambda）、加工具（08 篇）——'模板 + 变形 = 管道的基本功'"**（"数据流的思考：链 = 数据流（前出后进）——写链前先想'数据长什么样'（02 篇组合规则）——**'链的设计 = 数据流的设计'"**）；**链的复用**——"链是 Runnable（02 篇）——可以被其他链组合（大链套小链）——**'链 = 应用的'函数'（可组合可复用）'"**（"了解即可：链的调试（中间结果查看——langsmith/中间变量）用时查——**'先会拼标准模板，再谈复杂'"**）。

## 3. 复杂组合模式

**复杂组合——并行/条件/透传（LCEL 的进阶）**：

```python
from langchain_core.runnables import RunnableParallel, RunnablePassthrough

# 模式一：并行（多个子链同时跑——互不等待）
# 场景：同一输入要多个视角（总结 + 关键词 + 情感）
summarizer = prompt1 | model | parser          # 总结链
keyworder  = prompt2 | model | parser          # 关键词链
parallel = RunnableParallel(
    summary=summarizer,
    keywords=keyworder,
)
result = parallel.invoke({"topic": "AI 发展"})   # 两个链并行
# result = {"summary": "...", "keywords": "..."}

# 模式二：透传 + 添加（保留原输入 + 加处理结果）
chain = RunnablePassthrough.assign(
    answer=lambda x: (prompt | model | parser).invoke(x)
)
result = chain.invoke({"topic": "AI"})
# result = {"topic": "AI", "answer": "..."}     # 原输入 + 新字段

# 模式三：分支（条件路由——了解即可——复杂分支用 LangGraph）
```

**组合心智**：**"复杂组合的记忆：'Parallel 并行、Passthrough 透传、Branch 分支'"**——"**并行模式的价值：独立任务同时跑（性能——省时间）——'总结 + 关键词 + 情感 = 一次并行拿全'"**（"透传的价值：保留上下文（原输入 + 处理结果——后续要用原输入时）——**'透传 = 链中'不动'的环节'"**）；**复杂组合的边界**——"分支/循环（复杂控制流）LCEL 能做但别扭——**'复杂流程用 LangGraph（08 篇——图编排）——LCEL 管'链'、LangGraph 管'图'"**（"了解即可：知道三种模式 + 边界——**'简单用 LCEL 拼、复杂上 LangGraph（学习重点 2026）'"**）。

## 4. LCEL 的内置能力

**LCEL 的内置能力——白拿的"免费功能"**：

```text
LCEL 内置能力（组合即获得）
├── 流式：chain.stream()——原生流式（打字机效果——05 篇 vLLM 同款体验）
├── 异步：chain.ainvoke()/astream()——高并发场景（a 前缀——02 篇）
├── 批量：chain.batch([...])——并行处理多条输入
├── 错误处理：链中环节出错——可配置重试/兜底（.with_retry/fallback）
└── 中间调试：逐步查看（LangSmith——09 篇）
    ——"内置能力 = 用 LCEL 拼链的'附带福利'——不用自己实现"
```

**内置能力心智**：**"内置能力的记忆：'流式/异步/批量/容错——拼链即获得'"**——"**为什么是白拿：LCEL 的组合层实现了这些（每个 Runnable 都支持）——你拼好链，流式/异步/批量自动可用——'手写的话：每样都要自己实现（流式解析/异步封装/批处理）——LCEL 全内置'"**（"生产场景的用法：chat 应用用 stream（体验）、高并发用 ainvoke（性能）、批处理用 batch——**'内置能力 = 生产应用的'三件套'"**）；**容错的价值**——"链的环节可能失败（模型限流/网络）——with_retry（重试）/fallback（兜底模型）——**'生产链要配容错（不能裸跑）'"**（"了解即可：知道'有容错机制'——具体配置用时查——**'生产级 = 链 + 容错 + 观测（09 篇 LangSmith）'"**）。

**LCEL 的工程实践建议**（写链的好习惯）："**① 链函数化**——每条链封装成函数（def make_chain()——复用/测试）；**② 小链先测**——先拼最小链验证（prompt | model）再扩展（逐步调试）；**③ 命名清晰**——链变量名语义化（summary_chain/qa_chain——别人看得懂）；**④ 版本管理**——链的改动走 Git（与代码同款）——**'四个习惯 = LCEL 的'工程化'"**（"对比脚本式写链（一把梭）：函数化/小链测试 = 可维护——**'链也是代码——按代码的规矩写'"**）。

**LCEL 与 LangGraph 的分工细化**（何时升级到图）："**LCEL 管'链'**——线性/并行/透传（本课——流程确定）；**LangGraph 管'图'**——循环/分支/多 Agent（流程动态——08 篇）——**'判断标准：流程'写死'用 LCEL、流程'看情况'用 LangGraph'"**（"升级的信号：链里出现'复杂条件跳转/循环'（写 LCEL 别扭了）→ 上 LangGraph——**'LCEL 是'简单流程'的答案，LangGraph 是'复杂流程'的答案'"**）。

**LCEL 的面试价值**（为什么是 2026 必学）："**面试高频题：'LCEL 是什么/为什么用它'**——答：'声明式管道组合（|）——prompt | model | parser——组合优于继承——流式/异步/批量内置——1.x 的链标准（旧 Chain 类已移 classic）'——**'LCEL 是 LangChain 面试的第一题（答好它 = 印象分）'"**（"项目里用 LCEL 的讲法：'我的链用 LCEL 拼（prompt | model | parser）——流式输出开箱即用'——**'简历/面试的'LCEL 时刻'——会讲 = 加分'"**）。

## 5. 练习 5 题

1. LCEL 是什么？（| 管道——声明式拼链——1.x 链标准）
2. 标准模板？（prompt | model | parser）
3. 三种复杂模式？（Parallel/Passthrough/Branch）
4. 内置能力四件？（流式/异步/批量/容错）
5. 与 LangGraph 的分工？（链 vs 图）

## 6. 本节验收

**验收动作**：① 拼标准模板链并 invoke；② 加 RunnableLambda 业务函数；③ 用 RunnableParallel 做并行链（总结+关键词）；④ 体验 stream 流式输出——**"标准模板 + 复杂模式 + 内置能力 = LCEL 能力"**——**练习纪律**：写链先想数据流——"链的设计 = 数据流的设计"。

> 🎯 **核心要点**：LCEL = 声明式组合（**| 管道——1.x 链标准——旧 Chain 类已移 classic——组合优于继承**）；标准模板（**prompt | model | parser——加业务 RunnableLambda——加结构化 with_structured_output**）；**复杂组合（Parallel 并行/Passthrough 透传/Branch 分支——简单用 LCEL 复杂上 LangGraph）**；**内置能力（流式/异步/批量/容错——拼链即获得——生产三件套）**——"`prompt | model | parser` 一行拼出链——LCEL 是 1.x 的链标准"。

---

**上一模块**：[05-输出解析.md](./05-输出解析.md) / **下一模块**：[07-记忆与对话.md](./07-记忆与对话.md)
