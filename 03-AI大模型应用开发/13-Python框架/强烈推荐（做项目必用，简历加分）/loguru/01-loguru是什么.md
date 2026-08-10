# 01 - loguru 是什么

> 定位：Python 日志库的事实标准——"Python logging made (stupidly) simple"——把标准库 logging 的 Handler/Formatter/Filter 三件套压成一个 `add()`，让打日志变成零成本的自觉习惯

---

## 📚 目录

1. [一个日志库，解决四类痛点](#1-一个日志库解决四类痛点)
2. [2026 现状与版本基线](#2-2026-现状与版本基线)
3. [核心设计哲学](#3-核心设计哲学)
4. [与标准库 logging 对比](#4-与标准库-logging-对比)
5. [生态位置与边界](#5-生态位置与边界)
6. [练习 5 题](#6-练习-5-题)

---

## 1. 一个日志库，解决四类痛点

loguru 的官方口号是"Python logging made (stupidly) simple"——它瞄准的正是标准库 logging 让新手劝退、让老手嫌弃的四个痛点。

**痛点一：配置样板代码太多。** 标准库打一个带文件轮转的日志，要同时摆弄 `getLogger`、`FileHandler`、`RotatingFileHandler`、`Formatter`、`setLevel`、`addHandler` 六七个环节，写错一个环节日志就静默丢失。loguru 把这套全部压进一个 `logger.add()`：sink（输出目标）、级别、格式、轮转、保留、压缩、队列、序列化全部是这一个函数的参数——"**No Handler, no Formatter, no Filter: one function to rule them all**"。

**痛点二：每个文件都要样板。** 标准库惯例是每个模块 `logger = logging.getLogger(__name__)`，然后祈祷配置在入口处设置好了；一旦入口没配，日志直接消失或重复。loguru 反过来：**全局只有一个预配置好的 logger**，`from loguru import logger` 拿来就打，默认输出到 stderr 且带颜色——不存在"没配置导致没日志"的可能。

**痛点三：格式不可读。** 标准库默认格式是一行挤满时间戳、模块、级别的灰色文本，没有颜色、没有友好的时间格式。loguru 默认输出带 `<level>` 自动配色的级别标签和精确时间，`{time:YYYY-MM-DD HH:mm:ss}` 这类模板直接可用。

**痛点四：异常信息不直观。** 标准库的 `traceback` 只给堆栈帧，不给你"崩溃那一刻变量是什么"。loguru 的 `backtrace=True` + `diagnose=True` 能把异常调用链和局部变量值一起打出来，排错效率完全不同（05 篇详讲）。

## 2. 2026 现状与版本基线

**2026-08 基线：0.7.3（2024-12-06 发布）**，自那以后没有新功能版本——不是项目死了，而是**进入了成熟维护期**：0.7.3 全部是修复与性能优化（Cython 栈帧兼容、`logger.remove()` 线程安全、Python 3.13 下 `diagnose` 兼容、datetime 格式化提速、启动提速），0.7.x 系列在 Debian/Ubuntu/Arch 等发行版持续打包维护。

项目基本面：**24.1K stars、MIT 许可证、纯 Python wheel（`py3-none-any`，无需编译）、支持 Python 3.5+ 与 PyPy、PyPI 标记 Production/Stable**。官方路线图上明确写着"**在即将到来的版本中，关键函数将用 C 实现以获得最大速度**"——目标是把"比标准库快 10 倍"（官方宣称的当前性能口号）变成硬指标。

**对学习者的含义**：loguru 的 API 五年基本没变——**"学一遍用五年"**；网上任何时代的 loguru 教程几乎都不过时，这与其他 Python 框架（LangChain 1.x 重写、FastAPI 频繁破坏性变更）形成鲜明对比，是"低折旧率技术"的典型。

## 3. 核心设计哲学

loguru 的设计可以用三句话概括。

**第一句：只有一个 logger。** 不是每个模块建一个，而是全局一个单例，`logger.add()` 注册多个 sink（可以同时输出到 stderr、文件、JSON 队列），每个 sink 独立配级别/格式/过滤器。模块只负责"打"，输出策略全部集中在入口。

**第二句：消息即模板。** `logger.info("用户 {} 登录成功", user_id)` 用大括号占位符，参数惰性拼接——日志级别不满足时连参数都不计算（对性能敏感场景是免费优化）。这比 f-string 提前求值、比 `%` 风格更清晰（03 篇详讲）。

**第三句：捕获即装饰。** `@logger.catch` 装饰一个函数，异常自动带着完整上下文进日志，不用在每个 except 里手写 `logger.exception()`。线程里的异常也能兜住（05 篇详讲）。

## 4. 与标准库 logging 对比

| 维度 | 标准库 logging | loguru |
|------|--------------|--------|
| 初始化 | getLogger + Handler + Formatter + setLevel 多步 | `logger.add(sink, ...)` 一步 |
| 输出目标 | Handler 类体系（File/Stream/Rotating…） | sink 参数（路径/流/函数/类通吃） |
| 文件轮转 | 手写 RotatingHandler + 自己管备份 | `rotation="500 MB"` 一行 |
| 清理保留 | 无内置，需自写 | `retention="10 days"` |
| 压缩 | 无内置 | `compression="zip"` |
| 默认行为 | 无配置则 WARNING 以上才可见 | stderr + 彩色 + 全级别 |
| 异常详情 | 无变量值 | backtrace/diagnose 可选 |
| 多进程安全 | 需 queue 手搭 | `enqueue=True` |
| JSON 输出 | 自写 Formatter | `serialize=True` |
| 灵活性 | 极高（全部可定制） | 足够（常见场景全覆盖） |
| 生态 | 标准库自带、三方 Handler 多 | 单包即可、`apprise` 告警插件 |

**选型判断**：追求极致定制（自研日志框架、需要和特定平台深度整合）用标准库；**绝大多数项目（脚本、爬虫、FastAPI、AI 应用）用 loguru——覆盖 95% 的日志需求，剩下 5% 通过 08 篇的互操作机制也能接住**。

## 5. 生态位置与边界

**loguru 在 Python 生态的位置**：它是"应用层日志地基件"——爬虫项目的采集留痕、FastAPI 的请求日志、AI 应用的调用链日志、库开发者的内部日志，全都可以统一到 loguru。同类的替代品主要是 **structlog**（更极致的结构化日志，与 loguru 的"简单优先"路线不同；loguru 的 JSON 序列化在 09 篇覆盖，大多数项目不必上 structlog）。

**边界要划清楚**：loguru 只负责"**产日志**"——它不做指标采集、不做分布式追踪（trace 的 ID 可以由应用生成并 bind 进日志，但采集与聚合是 OTel/Loki/ELK 的事）、不做日志聚合与告警（可以靠 `apprise` 发通知，但生产告警体系用专用平台）。**一句话：loguru 是观测链路的最上游，不是观测平台本身**（09 篇讲对接）。

**落地场景速览**：脚本/CLI（零配置彩色输出）、爬虫（断点续爬的留痕）、FastAPI（请求日志 + trace_id）、AI 应用（LLM 调用参数与耗时）、Python 库（`disable()` 默认静默 + 应用 `enable()`）。

**什么场景不建议用 loguru**：追求极端吞吐且对日志性能敏感到纳秒级的场景（标准库可完全自定义、C 实现更可控，loguru 的 C 化还在路线图上）；需要与某云厂商日志 SDK 深度绑定的存量项目（迁移成本可能大于收益）；日志量到达"需要自研日志平台"量级的巨型系统——**一句话：常规项目无脑 loguru，到了"日志本身成为架构问题"的规模再谈自研**。

**为什么它是 2026 年必学项**：几乎所有主流 Python 开源项目的日志都用了 loguru（FastAPI 系模板、爬虫框架生态、AI 应用脚手架中 loguru 是默认日志件）——**会 loguru = 能读懂别人的项目 + 自己的项目从第一天就有规范日志**；这也是它排在"强烈推荐"目录（与 LangChain、LlamaIndex 同级）的原因：不是某个领域的专项技能，而是所有 Python 项目的通用地基。

## 6. 练习 5 题

1. 用标准库实现"彩色 + 轮转 + 保留 10 天"的日志配置，再改用 loguru 一行实现——对比代码量。
2. 为什么 loguru 的"全局单例"设计能消灭"日志丢失"问题？
3. 0.7.3 版本的性质是什么？为什么说 loguru "学一遍用五年"？
4. loguru 与 structlog 的分工边界是什么？什么项目才需要升级到 structlog？
5. 为什么说"loguru 是观测链路的最上游"？它不负责什么？

> 🎯 **核心要点**：loguru = "一个 logger + 一个 add() + 消息模板 + catch 装饰器"四板斧，把标准库的配置负担压缩到接近零；2026 年它是 Python 日志事实标准（0.7.3 稳定维护期，API 五年不变）——**学它 = 给所有 Python 项目装上统一的地基件**。

---

**下一模块**：[02-快速上手与核心API.md](02-快速上手与核心API.md) / **返回总览**：[00-loguru总览.md](00-loguru总览.md)
