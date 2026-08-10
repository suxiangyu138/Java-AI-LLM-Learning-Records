# 00 - pydantic 总览

> 定位：Python 数据校验的事实标准库——"类型注解即校验器"——Rust 核心（pydantic-core）5-50 倍提速；FastAPI/OpenAI SDK/LangChain/Agent 工具定义全栈底座；2026 年 v2.13.3 稳定维护，v1 已全面退场

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [与主体系的分工](#3-与主体系的分工)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [常见误区](#6-常见误区)
7. [一周学习计划示例](#7-一周学习计划示例)
8. [快速自测 10 题](#8-快速自测-10-题)
9. [参考来源](#9-参考来源)

---

## 1. 知识体系导图

```text
pydantic v2（本体系 11 篇——Web & 异步基座模块）
├── 定位层：01 pydantic 是什么（数据校验事实标准/Rust 核心/2026 基线）
│          02 安装与快速开始（Model 三段式/最小闭环）
├── 核心层：03 类型系统与字段（内置类型/约束/嵌套模型）
│          04 校验器体系（field_validator/model_validator/三种 mode）
│          05 序列化与模型方法（model_dump/model_copy/computed_field）
│          06 ConfigDict 与模型配置（extra/from_attributes/strict/frozen）
├── 应用层：07 高级类型与 TypeAdapter（判别联合/Settings/严格模式）
│          08 性能与生产实践（Rust 核心/编译缓存/JSON 直验）
│          09 与 AI 生态集成（structured output/工具 Schema/设置校验）
└── 验收层：10 生产实战与自测（FastAPI+结构化输出全链路 + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | pydantic 是什么 | 定位/2026 基线/v1 vs v2 | 认知 |
| 02 | 安装与快速开始 | 模型定义/校验/序列化闭环 | 会建模 |
| 03 | 类型系统与字段 | 类型全家/Field 约束/嵌套 | 会声明 |
| 04 | 校验器体系 | field/model_validator/mode | 会校验 |
| 05 | 序列化与模型方法 | dump/copy/computed_field | 会导出 |
| 06 | ConfigDict | extra/from_attributes/strict | 会配置 |
| 07 | 高级类型 | TypeAdapter/判别联合/Settings | 会进阶 |
| 08 | 性能与生产 | Rust 核心/JSON 直验/清单 | 会生产 |
| 09 | AI 生态集成 | structured output/工具 Schema | 会集成 |
| 10 | 实战与自测 | 全链路实战 + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 FastAPI 主体系（`../../../01-Python语言/Python 异步 + FastAPI/00-Python异步与FastAPI知识体系总览.md`）的分工**：FastAPI 体系 05 篇讲"请求/响应校验在 Web 栈里的用法"，**本体系专讲 pydantic 库本身**——模型定义、校验器、序列化、类型系统、配置、性能——**"FastAPI 是 Web 框架，pydantic 是它的校验引擎——先学引擎再学框架"**。

**与 pyyaml（`../../强烈推荐（做项目必用，简历加分）/pyyaml/00-pyyaml总览.md`）的分工**：配置文件场景两条路线——**"pyyaml 管读取，pydantic 管校验"**：yaml 读进来是裸 dict，pydantic 把 dict 校验成强类型配置对象（llm.yaml + Settings 是 2026 标准组合，09 篇）。

**与 loguru（`../../强烈推荐（做项目必用，简历加分）/loguru/00-loguru总览.md`）的分工**：日志结构化场景——**loguru 负责输出，pydantic 负责定义"日志事件的 schema"**；同为"Web & 异步基座模块"的地基件。

**与同级待建体系的分工**：FastAPI（`../FastAPI/00-FastAPI总览.md`）与 openai python-sdk（`../openai%20python-sdk/00-openai-python-sdk总览.md`）都重度依赖 pydantic——**"pydantic 是这两个框架的地基，本体系先建，框架篇引用它"**；Function Calling 体系（`../../../02-大模型基础与Prompt工程/Function Calling 函数调用【Agent 基石】/00-FunctionCalling知识体系总览.md`）的工具声明也是 pydantic 生成 JSON Schema（09 篇）。

**2026-08 基线**：pydantic **v2.13.3**（2026-04-20 发布——v2.13.0 于 2026-04-13 为主特性版：多态序列化 `polymorphic_serialization`、computed field 的 `exclude_if`、私有属性默认工厂可用验证后数据、pydantic.v1 命名空间更新至 1.10.26 并支持 Python 3.14）；**pydantic-core（Rust 核心）已并入主仓库**（v2.13.0b1 起，独立仓库 2026-04-11 归档）；v2.14.0a1（2026-05-22）弃 Python 3.9、支持 Pyodide 浏览器端——**"v2 完全体、Python 3.10+、新项目锁 2.13.x"**；**v1 已全面退场**：FastAPI 0.126 起弃 v1（要求 pydantic ≥ 2.7.0）、0.128 完全移除 pydantic.v1。

## 4. 学习路线推荐

**路线一：标准路线（3-5 天）**——01 → 10 逐篇 + 每篇跑代码（**练习 5 题要动手写，代码别复制粘贴**）——**毕业标准：独立完成"FastAPI 接口校验 + Settings 配置 + LLM 结构化输出"三合一项目**。

**路线二：速成路线（1-2 天）**——01 → 02 → 03 → 06 → 10（跳过 04/05/07/08/09 精读）——适合已在用 FastAPI、只想把 pydantic 用法补齐的人——**"会用 FastAPI 的校验注解 ≠ 懂 pydantic，把模型/配置/序列化三块补上就够日常"**。

**路线三：AI 项目驱动路线**——做 Agent/RAG 项目遇到"结构化输出""工具参数校验"按需查篇——**"pydantic 是 AI 应用的隐形底座：structured output 靠它、工具 Schema 靠它、配置校验靠它——遇到就查 09 篇"**；同级的 openai python-sdk（`../openai%20python-sdk/`）建成后与 09 篇互为表里。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| BaseModel | 模型基类：类型注解即字段声明与校验器 | 02 |
| ValidationError | 校验失败错误（含所有字段错误路径） | 02 |
| Field | 字段级约束与元数据（min_length/gt/别名…） | 03 |
| Annotated | 组合类型与约束的现代写法 | 03 |
| field_validator | 字段级校验器（before/after/wrap） | 04 |
| model_validator | 模型级校验器（跨字段逻辑） | 04 |
| model_dump | 序列化为 dict（v1 的 .dict()） | 05 |
| model_validate | 从 dict/对象校验构造（v1 的 parse_obj） | 05 |
| ConfigDict | 模型配置（extra/from_attributes/strict/frozen） | 06 |
| TypeAdapter | 无模型类型校验（list[dict] 等任意类型） | 07 |
| Discriminated Union | 判别联合：按 tag 字段分派的 Union | 07 |
| pydantic-settings | 独立配置包：环境变量/文件 → 强类型 Settings | 07 |

## 6. 常见误区

**误区一：pydantic 只是"参数校验工具"**——它是**数据层事实标准**：校验、序列化、JSON Schema 生成、配置管理一站式——FastAPI/OpenAI SDK/LangChain 全栈地基（01/09 篇）。

**误区二：还在用 v1 写法**——`.dict()`/`.parse_obj()`/`@validator`/`class Config` 全部是 v1 API——**2026 年 FastAPI 已完全移除 pydantic.v1，新代码写 v2 API（model_dump/model_validate/@field_validator）**（01 篇对照表）。

**误区三：校验器随便写函数就行**——**v2 的 @field_validator 必须 classmethod、必须返回校验后的值**——不写 @classmethod 直接报错，忘了返回值等于"校验了个寂寞"（04 篇）。

**误区四：Union 的类型顺序无所谓**——**v2 按声明顺序尝试，str 在前会把数字也吞掉**——宽松类型放后面，或显式用 discriminator（03/07 篇）。

**误区五：配置靠 pydantic 里的 BaseSettings**——**v2 起 BaseSettings 移到了独立的 pydantic-settings 包**——从 pydantic 导入直接 ImportError（07 篇）。

**误区六：校验慢无所谓**——**pydantic-core 是 Rust 实现，校验比 v1 快 5-50 倍**——但大量小模型反复实例化仍有开销，**批量场景用 TypeAdapter 预热编译**（08 篇）。

**误区七：JSON Schema 生成后可以 round-trip 回 pydantic**——**Rust 正则库不支持 look-around，Decimal 等字段生成的 schema 无法被 pydantic 自己重新校验**（issue #13017）——schema 只做"对外声明"，别拿它重建模型（09 篇）；同理，**模型定义才是唯一事实来源，schema 是它的投影**。

## 7. 一周学习计划示例

| 天 | 内容 | 动手任务 |
|:---:|------|---------|
| 1 | 01 + 02 | 装 pydantic==2.13.3，定义一个模型校验与序列化 |
| 2 | 03 + 04 | 写 5 种字段类型 + before/after 校验器 |
| 3 | 05 + 06 | 试 model_dump 各参数与 extra/strict 配置 |
| 4 | 07 | 写一个 Discriminated Union + TypeAdapter 批量校验 |
| 5 | 08 | 对比 TypeAdapter vs 模型实例化性能 |
| 6 | 09 | 用 pydantic 定义 LLM 结构化输出 schema |
| 7 | 10 自测 + 面试 | 三合一项目 + 20 题 |

## 8. 快速自测 10 题

1. pydantic v2 的核心引擎是什么？比 v1 快多少？
2. v1 → v2 的五个 API 对照（dict/parse_obj/schema/Config/validator）？
3. Field 与 Annotated 各是什么？现代写法推荐哪个？
4. field_validator 的三种 mode 语义？为什么必须 classmethod？
5. model_validator 解决什么问题？
6. model_dump 的常用参数？model_copy 在 2.14 的行为变化？
7. ConfigDict 里 extra/strict/frozen 各做什么？
8. Discriminated Union 与普通 Union 的区别？
9. BaseSettings 现在在哪？为什么单独成包？
10. 2026-08 的版本基线？pydantic-core 为什么并入主仓库？

## 9. 参考来源

- [Pydantic 官方文档（v2 完整 API 参考）](https://docs.pydantic.dev/)
- [Pydantic GitHub 官方仓库（Release 记录/HISTORY）](https://github.com/pydantic/pydantic)
- [Pydantic v2.13 发布公告（polymorphic_serialization/exclude_if）](https://pydantic.dev/articles/pydantic-v2-13-release)
- [Pydantic PyPI 页面（2.13.3 版本信息）](https://pypi.org/project/pydantic/)
- [FastAPI 0.126-0.128 Pydantic v1 移除时间线（GitHub PR #14862）](https://github.com/fastapi/fastapi/pull/14862)
- [pydantic issue #13017：Decimal JSON Schema 与 Rust 正则 round-trip 限制](https://github.com/pydantic/pydantic/issues/13017)

---

**下一模块**：[01-pydantic是什么.md](01-pydantic是什么.md)
