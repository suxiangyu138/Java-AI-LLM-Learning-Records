# 00 - loguru 总览

> 强烈推荐：loguru（Python 日志库）——做项目必用、简历加分——"Python logging made (stupidly) simple：一个 logger 统治一切，零配置开箱即用，轮转/捕获/JSON 开箱即得——2026 年 Python 日志事实标准"

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
loguru（本体系 11 篇——强烈推荐）
├── 定位层：01 loguru 是什么（Python 日志事实标准/0.7.3 现状）
├── 核心层：02 快速上手与核心 API（add()/sink/remove）
│          03 格式与日志记录（format/颜色/opt/bind）
│          04 文件日志与轮转（rotation/retention/compression）
├── 进阶层：05 异常处理与 traceback（catch/backtrace/diagnose）
│          06 级别控制与过滤器（level/filter/disable-enable）
│          07 并发与性能（enqueue/异步 sink/性能）
├── 工程层：08 与标准库 logging 互操作（InterceptHandler）
│          09 结构化日志与导出（JSON/parse/观测平台对接）
└── 验收层：10 生产实战与自测（FastAPI 日志配置 + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | loguru 是什么 | 定位/标准库痛点/0.7.3 现状 | 认知 |
| 02 | 快速上手与核心 API | add()/sink 家族/remove/自定义 level | 会写日志 |
| 03 | 格式与日志记录 | format/颜色/opt()/bind | 会格式化 |
| 04 | 文件日志与轮转 | rotation/retention/compression | 会落盘 |
| 05 | 异常处理与 traceback | catch/backtrace/diagnose | 会排错 |
| 06 | 级别控制与过滤器 | level/filter/disable/enable | 会治理 |
| 07 | 并发与性能 | enqueue/异步 sink/性能取舍 | 会并发 |
| 08 | 与标准库互操作 | InterceptHandler/第三方库治理 | 会接管 |
| 09 | 结构化日志与导出 | JSON/parse/观测平台 | 会对接 |
| 10 | 实战与自测 | FastAPI 完整配置 + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 Java 日志监控指标体系（`../../../../02-后端核心技术%20微服务%20分布式%20云原生/07-工程运维基础/运维/日志监控指标/00-日志监控指标总览.md`）的分工**：那套体系讲 Java 侧（SLF4J/Logback/Log4j2 + EFK/Loki + OTel）——"**同主题双语言双雄：Java 看那套，Python 看本套；日志格式与采集的通用方法论（模式/滚动/脱敏/告警）两套通用**"（09 篇对接观测平台时复用）。

**与 Python 异步 + FastAPI 体系（`../../../01-Python语言/Python%20异步%20+%20FastAPI/00-Python异步与FastAPI知识体系总览.md`）的分工**：那个体系讲 FastAPI 的日志治理是"中间件 + logging"视角，本体系讲"loguru 怎么用"——**"FastAPI 项目接入 loguru = 本体系的 10 篇实战 + 那个体系的可观测性篇配合"**。

**与爬虫体系（`../../../12-Python爬虫/阶段%201：前置基础（必备）/00-阶段1前置基础总览.md`）的分工**：爬虫是 loguru 的典型落地场景（requests 三件套 + 日志），**"爬虫体系用日志做采集留痕与断点续爬，本体系教你怎么把日志写好"**。

**与 vLLM 体系（`../../了解即可（知道能干什么，不需要深挖源码）/vLLM/00-vLLM总览.md`）、LangChain 体系（`../LangChain/00-LangChain总览.md`）的关系**：vLLM 用 uvicorn 日志、LangChain 的 LCEL 链调试——**"任何 Python 项目（爬虫/FastAPI/vLLM 客户端/LangChain 应用）的日志统一交给 loguru 接管——它是所有 Python 应用的地基件"**。

**2026-08 基线**：loguru **0.7.3**（2024-12-06 发布，纯 Python wheel，MIT，24.1K stars，支持 Python 3.5+ / PyPy / 3.13+）；**关键认知**：0.7.3 是纯修复 + 性能优化版（datetime 格式化提速、启动提速、3.13 兼容），**自 2024-12 起无新功能版本——库已进入稳定维护期，官方路线图是"核心函数用 C 实现，目标比标准库快 10 倍"**；**"老教程与 0.7.x 行为一致，loguru 的 API 五年没变过——学一遍用五年"**。

## 4. 学习路线推荐

**路线一：标准路线（3-5 天）**——01 → 10 逐篇 + 每篇动手跑代码——**毕业标准：给一个 FastAPI 或爬虫项目配上"轮转 + JSON + trace_id + 接管第三方日志"的完整日志体系**。

**路线二：速成路线（1-2 天）**——01 → 02 → 04 → 05 → 08 → 10（跳过 03/06/07/09 精读）——适合已有 logging 经验、只想最快用起来的人。

**路线三：查字典式路线**——项目里遇到日志问题按需查篇——**"loguru 上手 5 分钟，难点全在生产细节（轮转/并发/互操作/安全）——遇到再查"**。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| logger | 全局单例日志器，开箱即用输出 stderr | 02 |
| add() | 一个函数替代 Handler+Formatter+Filter | 02 |
| sink | 日志输出目标（文件/流/函数/类/Handler） | 02 |
| rotation | 按大小/时间自动切分文件 | 04 |
| retention | 自动清理过期日志文件 | 04 |
| compression | 切分后自动压缩（zip/gz） | 04 |
| format | 大括号模板 + record 字段 + 颜色 markup | 03 |
| opt() | 逐条日志微调（lazy/colors/raw/depth） | 03 |
| bind() | 给日志绑定 extra 上下文字段 | 03 |
| catch | 装饰器/上下文管理器捕获异常 | 05 |
| backtrace/diagnose | 跨调用捕获 + 变量值显示（安全警告） | 05 |
| filter | 按模块/规则过滤日志 | 06 |
| disable/enable | 库开发模式：默认静默、显式启用 | 06 |
| enqueue | 队列化写入，多进程安全 + 异步支持 | 07 |
| serialize/parse | JSON 输出 / 正则反解析 | 09 |

## 6. 常见误区

**误区一：loguru 只是"好看的 print"**——实际它具备完整生产能力：轮转、保留、压缩、JSON、多进程安全，都是标准库要手写一堆代码才能凑齐的。

**误区二：项目里 logger 和 logging 混用没关系**——一半日志带颜色一半不带、格式不统一、级别不一致——**生产日志必须"单一口径"：用 08 篇的 InterceptHandler 把标准库全部接管**。

**误区三：diagnose=True 直接上生产**——0.7.3 里 diagnose 在 Python 3.13 有兼容修复，但它会把局部变量值打进日志——**生产环境可能泄露密码/token——必须按环境开关**（05 篇详讲）。

**误区四：rotation="500 MB" 就万事大吉**——轮转的触发时机、跨天切分、多进程轮转冲突、retention 与备份的关系都是坑（04 篇）。

**误区五：日志随便打，量大再说**——**结构化日志（JSON）要从第一天就定好 schema，事后改格式 = 全链路解析重做**（09 篇）。

**误区六：loguru 能替代观测平台**——loguru 只负责"产日志"，不采集指标、不做 tracing、不聚合——**它是 ELK/Loki/Grafana 的数据源之一，不是替代品**。

**误区七：异步函数里不能打日志**——`enqueue=True` + 异步 sink + `complete()` 是官方支持姿势，不是玄学（07 篇）。

## 7. 一周学习计划示例

| 天 | 内容 | 动手任务 |
|:---:|------|---------|
| 1 | 01 + 02 | 装 loguru，跑通 add/remove/level |
| 2 | 03 + 04 | 配一个"轮转 + 压缩 + 颜色"的文件日志 |
| 3 | 05 + 06 | 用 catch 包住一个会崩的函数，写过滤器 |
| 4 | 07 + 08 | 开 enqueue，用 InterceptHandler 接管 requests 日志 |
| 5 | 09 + 10 | FastAPI 项目接 JSON 日志 + trace_id |
| 6-7 | 10 自测 + 面试 | 跑 20 题，写简历项目描述 |

## 8. 快速自测 10 题

1. loguru 的核心设计哲学是什么？与标准库 logging 的最大差异在哪？
2. add() 有哪些常用参数？分别解决什么问题？
3. rotation 有哪几种写法？各自适合什么场景？
4. retention 和 compression 的区别？
5. format 里如何输出时间、级别、函数名？如何上色？
6. bind() 和 contextualize() 的区别？
7. diagnose=True 有什么安全风险？生产怎么处理？
8. enqueue=True 解决什么问题？有什么代价？
9. 如何让第三方库（requests/uvicorn）的日志统一走 loguru？
10. serialize=True 输出什么？配合什么手段喂给 Loki/ELK？

## 9. 参考来源

- [loguru GitHub 官方仓库（README/文档/CHANGELOG）](https://github.com/Delgan/loguru)
- [loguru PyPI 页面（0.7.3 版本信息/兼容性/许可证）](https://pypi.org/project/loguru/)
- [loguru 官方文档：Sinks 与 Rotation](https://loguru.readthedocs.io/en/stable/api/logger.html)
- [loguru 官方文档：互操作与迁移指南（InterceptHandler）](https://loguru.readthedocs.io/en/stable/resources/migration.html)
- [loguru 0.7.3 Release 记录（newreleases.io）](https://newreleases.io/project/pypi/loguru/release/0.7.3)
- [DEV.co 对 loguru 的评估（2026 现状复核）](https://dev.co/observability/open-source/loguru)

---

**下一模块**：[01-loguru是什么.md](01-loguru是什么.md)
