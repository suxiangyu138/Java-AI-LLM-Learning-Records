# 01 - pydantic 是什么

> 定位：Python 数据校验的事实标准库——"类型注解即校验器"——Rust 核心 5-50 倍提速，FastAPI/OpenAI SDK/LangChain/Agent 工具定义的共同底座——"2026 年，校验数据用 pydantic，就像读配置用 PyYAML、打日志用 loguru——三个地基件缺一不可"

---

## 📚 目录

1. [pydantic：类型注解即校验器](#1-pydantic类型注解即校验器)
2. [解决什么问题](#2-解决什么问题)
3. [一个画面：校验与转换](#3-一个画面校验与转换)
4. [2026 现状与版本基线](#4-2026-现状与版本基线)
5. [v1 与 v2：一次彻底重写](#5-v1-与-v2一次彻底重写)
6. [生态位置与边界](#6-生态位置与边界)
7. [练习 5 题](#7-练习-5-题)

---

## 1. pydantic：类型注解即校验器

**pydantic** 是 Python 的数据校验（validation）与序列化（serialization）事实标准库：你在类上写类型注解，它自动把注解变成运行时校验器——**"类型注解即校验器"**是它的全部哲学。一个最简单的例子：`class User(BaseModel): name: str; age: int`——构造时传 `{"name": "x", "age": "18"}`，pydantic 会把字符串 `"18"` 转成整数 18（**校验即转换**）；传 `{"age": "abc"}` 则抛 ValidationError 告诉你哪错、为什么错、错在哪一层。**v2 的核心引擎是 Rust 写的 pydantic-core**：校验比 v1 快 5-50 倍（单个字段从微秒级降到亚微秒级），Python 层只是把模型定义翻译成核心 schema 的薄壳——**"定义在 Python、执行在 Rust"**。

## 2. 解决什么问题

**问题一：运行时数据不可信**——HTTP 请求体、数据库查询结果、第三方 API 响应、LLM 的输出——**边界上来的都是字符串/裸 dict，类型是静态承诺，pydantic 把它变成运行时保证**：字段缺失、类型不符、取值越界在入口处全部拦截，业务代码拿到的永远是"已校验的强类型对象"。

**问题二：序列化与交换没有规范**——数据要进 JSON、进数据库、出 OpenAPI 文档——pydantic 一个模型同时产出：**校验器（入口）、dict（出口）、JSON Schema（接口声明）**——"定义一次，三处生效"，FastAPI 的 OpenAPI 文档就是 pydantic 的 model_json_schema 生成的。

**问题三：AI 应用的数据契约**——**2026 年 pydantic 是 AI 栈的隐形底座**：OpenAI SDK 的 structured output（response_format 收 pydantic 模型）、LangChain 的结构化输出、Agent 工具调用的参数 Schema（Function Calling 的 tools 声明本质是 JSON Schema）、llm.yaml 配置校验——**"AI 应用的外来数据（LLM 输出）最不可信，pydantic 是唯一被全栈接受的数据契约工具"**（09 篇全场景）。配置也是 AI 应用的硬需求：llm.yaml、工具参数、评估集 schema——**"配置加载即校验"同样是 pydantic 的活**（09 篇第 5 节）。

## 3. 一个画面：校验与转换

想象一道海关闸口：**进来的货物（输入数据）必须先申报（类型注解），海关按申报检查（校验），不合格的拒收并给回执（ValidationError），合格的按规格改包装（类型转换）**。pydantic 的模型就是这道闸口：`User(name="x", age="18")`——age 申报为 int，进来的是 str "18"，闸口把它转成 int 18 放行；`age="abc"` 拒收，回执写明"age: Input should be a valid integer"。**关键认知**：pydantic 不是"检查通过就完事"，它默认执行**智能类型转换**（str→int、str→bool、dict→模型……）——所以"校验"和"转换"永远一起发生；不想要转换，开严格模式（strict，06 篇）。

**另一个画面：合同**——pydantic 模型像一份合同：字段是条款（类型与约束）、校验器是复核员（语义检查）、ValidationError 是违约通知书（写明违约条款与证据）——"**合同先签后发货，数据先校验后使用**"。

## 4. 2026 现状与版本基线

**2026-08 基线：v2.13.3（2026-04-20 发布，Python 3.10+）**。版本演进关键节点：

| 版本 | 时间 | 关键变化 |
|------|------|---------|
| 2.13.3 | 2026-04-20 | 当前稳定版（锁这个版本） |
| 2.13.0 | 2026-04-13 | 多态序列化 polymorphic_serialization；computed field 的 exclude_if；pydantic.v1 更新至 1.10.26（支持 Py3.14） |
| 2.13.0b1 | 2026-02-23 | **pydantic-core 并入主仓库**（独立仓库 2026-04-11 归档） |
| 2.14.0a1 | 2026-05-22 | **弃 Python 3.9**；Pyodide 浏览器端支持；model_copy 只深拷贝未更新字段 |

**三条 2026 认知**：其一，**v1 已全面退场**——FastAPI 0.126 起要求 pydantic ≥ 2.7.0、0.127 发弃用告警、0.128 完全移除 pydantic.v1 支持——"**老代码里的 v1 写法是 2026 年最大的兼容债**"；其二，**pydantic-core 不再单独发版**——核心并入主仓库后，`pip install pydantic` 一个包搞定一切，排查"core 版本不匹配"的时代结束；其三，**性能路线是 Rust 继续深挖**——正则编译缓存、Literal 校验器优化、datetime 格式化都是 2.13 的提速点——"**越用越快的库，值得学深**"；其四，**AI 工具链深度绑定**——Pydantic AI、LangChain 的 structured output、LiteLLM 的响应校验都以 pydantic 为数据层——"**学 pydantic 一次，全 AI 生态受益**"。

## 5. v1 与 v2：一次彻底重写

v2 不是 v1 的修补，是**核心引擎重写（Python → Rust）**，顺带统一了 API。最常用的对照：

| v1（已退场） | v2（2026 写法） |
|------|------|
| `model.dict()` / `model.json()` | `model.model_dump()` / `model.model_dump_json()` |
| `Model.parse_obj()` / `parse_raw()` | `Model.model_validate()` / `model_validate_json()` |
| `Model.schema()` | `Model.model_json_schema()` |
| `class Config:` | `model_config = ConfigDict(...)` |
| `orm_mode = True` | `from_attributes = True` |
| `@validator`（非 classmethod） | `@field_validator`（**必须 classmethod**） |
| `@root_validator` | `@model_validator(mode="after")` |
| `from pydantic import BaseSettings` | `from pydantic_settings import BaseSettings`（独立包） |

**迁移现实**：老项目用 `uv run bump-pydantic ./src/` 自动改大部分机械替换，再逐模块人工排查——**"v1 写法不会报错，但会在某个依赖升级的早晨突然全部失效——主动迁比被动迁便宜"**（08 篇迁移清单）。

**还有一类隐藏的 v1 债**：第三方库内部还在用 pydantic.v1 命名空间（v2 附带的兼容层）——2.13 里它更新到了 1.10.26（含 Python 3.14 支持），但**新项目别主动依赖它**——"兼容层是过渡，不是目的地"。

## 6. 生态位置与边界

**生态位置**：pydantic 是 Python 的"数据地基件"——**依赖树里出现频率最高的库之一**：FastAPI、OpenAI SDK、LangChain、LiteLLM、Pydantic AI……几乎所有 Web/AI 框架的数据层都建立在它上面。学它的价值：**读懂任何框架的模型定义、写任何应用的数据契约、面试讲数据校验**——与 PyYAML（配置地基件）、loguru（日志地基件）并列的"Python 双地基件+"（00 篇分工）。**一句话定位**："**日志用 loguru、配置用 PyYAML+pydantic-settings、数据校验用 pydantic——三个地基件撑起 Python 应用的日常**"。

**边界要划清楚**：其一，**pydantic 校验数据，不传输数据**——HTTP/网络是 FastAPI/httpx 的活（09 篇分工）；其二，**它不是 ORM**——数据库映射交给 SQLAlchemy 等，pydantic 只在边界上做"DTO"（数据对象）；其三，**它不保证业务逻辑正确**——校验通过 ≠ 业务合理（比如金额为负可以用校验拦，但"该不该扣款"是业务的事）；其四，**JSON Schema 生成 ≠ 可回读**——Rust 正则不支持 look-around，部分 schema 无法 round-trip（00 篇误区七）；其五，**它不决定"要不要校验"**——性能敏感的热路径上是否值得过 pydantic，由你自己权衡（08 篇）。

## 7. 练习 5 题

1. "类型注解即校验器"是什么意思？"校验即转换"指什么？
2. pydantic 解决的三类问题是什么？AI 应用为什么尤其需要它？
3. 2026-08 的版本基线？pydantic-core 并入主仓库意味着什么？
4. 写出 v1 → v2 的五个 API 对照？
5. 与 PyYAML/loguru 的分工一句话是什么？pydantic 的四条边界？

> 🎯 **核心要点**：pydantic v2 = **Rust 核心（5-50 倍提速）+ 类型注解即校验 + 一站式（校验/序列化/Schema）**——2026-08 基线 v2.13.3、v1 全面退场、pydantic-core 已并入主仓库；**"定义一次、三处生效"是它的工程价值，AI 应用的数据契约是它的 2026 主场"**。

---

**下一模块**：[02-安装与快速开始.md](02-安装与快速开始.md) / **返回总览**：[00-pydantic总览.md](00-pydantic总览.md)
