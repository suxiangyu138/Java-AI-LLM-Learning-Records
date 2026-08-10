# 02 - 核心抽象：Runnable

> 本体系第二课：LangChain 的设计核心——Runnable 统一接口——"一切皆为 Runnable，一切皆可组合——记住这一句，LangChain 的骨架就懂了"

---

## 📚 目录

1. [设计哲学](#1-设计哲学)
2. [Runnable 接口](#2-runnable-接口)
3. [Runnable 家族](#3-runnable-家族)
4. [组合的基础](#4-组合的基础)
5. [练习 5 题](#5-练习-5-题)
6. [本节验收](#6-本节验收)

---

## 1. 设计哲学

**LangChain 的设计哲学：'一切皆为 Runnable，一切皆可组合'**：

```text
设计哲学
├── 一切皆 Runnable：模型/提示词/解析器/检索器/Agent——都是 Runnable
│    —— 统一接口：invoke/stream/batch（调用方式一致）
├── 一切皆可组合：Runnable 之间用 | 连接（LCEL——06 篇）
│    —— 组合的结果还是 Runnable（可再组合）
└── 类比：Runnable 像"积木的接口"——所有积木的凸点凹槽统一——
    任意两块能拼——拼完的还是一块"大积木"
    ——"统一接口 = 自由组合的前提——这是 LangChain 的骨架"
```

**哲学心智**：**"设计哲学的记忆：'统一接口 + 自由组合'"**——"**为什么这么设计：LLM 应用 = 多组件协作——统一接口让'换组件'和'加组件'都简单——'模型换成另一个（接口一样）、链里加一个解析器（接口一样）——不用改其他部分'"**（"对比：每个组件自己一套 API（模型 API 一套/解析器一套）→ 组合要写胶水；统一接口 → 组合用 |——**'接口统一 = 胶水代码消失'"**）；**哲学的价值**——"理解了 Runnable，LangChain 的一切都好理解（提示词是 Runnable、模型是 Runnable、链是 Runnable）——**'Runnable 是理解 LangChain 的'钥匙'"**（"本体系 03-09 篇都会提'它是 Runnable'——**'一把钥匙开所有门'"**）。

## 2. Runnable 接口

**Runnable 接口 = 统一的调用方式（四个方法 + 异步版）**：

```python
from langchain_core.runnables import Runnable

# Runnable 统一接口（所有组件都实现）
# 同步：invoke（单次调用）/ batch（批量）/ stream（流式）
# 异步：ainvoke / abatch / astream（加 a 前缀）
# 组合：pipe（| 管道——06 篇）/ bind（绑定参数）

# 示例：模型是 Runnable（统一调用）
from langchain_openai import ChatOpenAI

model = ChatOpenAI(model="gpt-4o-mini")          # 模型 = Runnable
resp = model.invoke("你好")                       # invoke 调用
# stream 流式 / batch 批量——同一个接口

# 组合的产物也是 Runnable（06 篇详讲）
# chain = prompt | model | parser
# chain.invoke(...) —— 链也这样调用
```

**接口心智**：**"接口的记忆：'invoke/stream/batch 三兄弟 + a 前缀异步'"**——"**为什么重要：会一个组件的调用 = 会所有组件（接口统一）——'模型用 invoke、提示词也用 invoke、链还用 invoke——一套姿势走天下'"**（"异步的姿势：async 场景用 ainvoke/astream（`Python 异步 + FastAPI` 体系的配合）——**'高并发应用用 a 前缀（09 篇集成）'"**）；**接口的返回**——"各组件返回自己的类型（模型返回消息、解析器返回字符串/对象）——**'输入输出类型各异，调用姿势统一——这就是接口的价值'"**（"了解即可：接口细节（参数/返回）查文档——**'记住'四方法 + 统一姿势'就够'"**）。

## 3. Runnable 家族

**Runnable 家族——常用组件一览**：

```python
from langchain_core.runnables import (
    RunnableLambda,          # 包装普通函数（把 Python 函数变 Runnable）
    RunnablePassthrough,     # 透传（原样传——不处理）
    RunnableParallel,        # 并行（多个子链并行跑）
)

# RunnableLambda：任何函数都能"变"Runnable（组合的万能胶）
def double(x): return x * 2
runnable_fn = RunnableLambda(double)          # 函数 → Runnable
runnable_fn.invoke(21)                        # 42

# RunnablePassthrough：透传/添加字段
# 输入 {"a": 1} → passthrough 原样输出（或 assign 添加）
# RunnableParallel：并行
# {"x": chain1, "y": chain2} → 两个链并行跑（互不等待）
```

**家族心智**：**"家族的记忆：'Lambda 包函数、Passthrough 透传、Parallel 并行'——三个工具件"**——"**RunnableLambda 的价值：'你的业务函数也能进管道'——业务逻辑（计算/查库/加工）包一层就能和模型组合——'管道里不只能有 LangChain 组件，还能有你的函数'"**（"这是 06 篇 LCEL 组合的关键——**'Lambda 是'业务代码'进入'框架管道'的桥'"**）；**Passthrough 与 Parallel**——"Passthrough：原样传（管道里"不动"的环节）；Parallel：并行（多个独立子任务同时跑——性能）——**'两个工具件的场景：透传（不动）/并行（同时）'"**（"了解即可：具体用法用时查文档——**'记住'三个工具件存在 + 用途'就够'"**）。

## 4. 组合的基础

**Runnable 的组合 = LCEL 的地基（06 篇的预告）**：

```python
# 组合的基础：|（管道——Runnable 之间）
# prompt（提示词）| model（模型）| parser（解析器）
# 前一个的输出 = 后一个的输入（类型要匹配——LangChain 自动适配大部分）

from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

prompt = ChatPromptTemplate.from_messages([("user", "用一句话介绍：{topic}")])
model = ChatOpenAI(model="gpt-4o-mini")
parser = StrOutputParser()                    # 消息 → 字符串

chain = prompt | model | parser               # 组合（核心！）
result = chain.invoke({"topic": "量子计算"})    # 链 = 一个 Runnable
print(result)                                 # 纯字符串结果
```

**组合心智**：**"组合的记忆：'| 管道——前出后进——组合结果还是 Runnable'"**——"**为什么这是核心：一次 invoke 走完整条链（提示词 → 模型 → 解析）——'应用的最小单元 = 一条链（chain）'"**（"组合的规则：前一个输出是后一个输入（类型适配——LangChain 自动处理大部分）——**'先想清楚'数据流'（什么进什么出），再拼链'"**）；**组合的深化**——"组合可以是复杂结构（并行/分支/循环——06 篇）——**'本课打地基（| 是基本），06 篇盖楼（复杂组合）'"**（"组合与 Runnable 的关系：组合的结果也是 Runnable（还能再组合）——**'万物皆 Runnable 的完整闭环（本课 1 节的落地）'"**）。

**Runnable 的实际使用流**（从创建到调用的完整姿势）："**① 创建**——模型（ChatOpenAI）/模板（ChatPromptTemplate）/解析器（StrOutputParser）都是 Runnable；**② 组合**——用 | 拼（prompt | model | parser）；**③ 调用**——invoke/stream/batch（或 a 前缀异步）；**④ 复用**——链可再组合（大链套小链）——**'四步 = Runnable 的完整生命周期——会走一遍 = 会 LangChain'"**（"Runnable 的调试——中间结果（stream 逐步看/langsmith 追踪——09 篇）——**'组合的调试 = 逐步验证（先小链后大链）'"**）。

**Runnable 与异步**（高并发应用的姿势）："**a 前缀全家桶**——ainvoke/astream/abatch（async 环境用——`Python 异步 + FastAPI` 体系的配合）——'FastAPI 接口里调用 LangChain 链 → 用 ainvoke（不阻塞事件循环）'"（"异步的坑——async 链里不能有同步阻塞（模型调用要异步版——与 uvicorn 06 篇'同步阻塞是性能杀手'同款）——**'异步场景：链 + a 前缀 + 无同步阻塞'"**）。

**Runnable 的生态意义**（为什么它定义了 LangChain）："**所有组件/集成都实现 Runnable**（模型/模板/解析器/检索器/Agent——02 篇）——'学会 Runnable = 学会 LangChain 的'通用语'——新组件到手就知道怎么调（invoke/stream/batch）'"（"生态的价值：1000+ 集成（09 篇）都是 Runnable——**'Runnable 是 LangChain 生态的'接口标准'——会一个会用全部'"**）。

## 5. 练习 5 题

1. 设计哲学一句话？（一切皆 Runnable，一切皆可组合）
2. Runnable 四方法？（invoke/stream/batch + a 前缀）
3. RunnableLambda 的价值？（业务函数进管道）
4. 组合的规则？（前出后进——结果还是 Runnable）
5. 一把钥匙是什么？（Runnable 接口——理解 LangChain 的钥匙）

## 6. 本节验收

**验收动作**：① 用 invoke/stream/batch 三种方式调模型；② 用 RunnableLambda 包一个业务函数并调用；③ 用 | 拼一条最小链（prompt | model | parser）并 invoke；④ 默写设计哲学——**"接口 + 家族 + 组合 = Runnable 核心"**——**练习纪律**：每个组件先问"它是 Runnable 吗"——"一把钥匙开所有门"。

> 🎯 **核心要点**：设计哲学（**一切皆 Runnable 一切皆可组合——统一接口 = 自由组合的前提——胶水代码消失**）；**接口（invoke/stream/batch + a 前缀——一套姿势走天下）**；**家族（Lambda 包函数/Passthrough 透传/Parallel 并行——Lambda 是业务代码进管道的桥）**；**组合基础（| 管道——前出后进——组合结果还是 Runnable——本课打地基 06 篇盖楼）**——"记住这一句，LangChain 的骨架就懂了"。

---

**上一模块**：[01-LangChain是什么.md](./01-LangChain是什么.md) / **下一模块**：[03-模型封装ChatModel.md](./03-模型封装ChatModel.md)
