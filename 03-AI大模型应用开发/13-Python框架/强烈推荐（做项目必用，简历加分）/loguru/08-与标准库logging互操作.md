# 08 - 与标准库 logging 互操作

> 定位：生产日志的"大一统"篇——第三方库全用标准库打日志，用 `InterceptHandler` 把它们全部接管进 loguru——从"两套日志系统混战"到"单一口径"

---

## 📚 目录

1. [为什么必须互操作](#1-为什么必须互操作)
2. [两个方向的对接](#2-两个方向的对接)
3. [InterceptHandler：官方接管方案](#3-intercepthandler官方接管方案)
4. [propagate 与重复日志](#4-propagate-与重复日志)
5. [第三方库日志治理实战](#5-第三方库日志治理实战)
6. [迁移三步走](#6-迁移三步走)
7. [常见坑](#7-常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. 为什么必须互操作

现实是：**你的项目里同时存在两套日志系统**。你用 loguru 打业务日志，但 `requests`、`urllib3`、`httpx`、`uvicorn`、`pymysql`、`aiohttp`……几乎所有第三方库都用标准库 `logging` 打日志。后果：

- **格式不统一**——一半彩色一半灰扑扑，格式对不上；
- **级别不统一**——requests 的 DEBUG 请求日志可能直接刷屏，而你的 INFO 级别压不住它；
- **输出不统一**——第三方库日志可能根本没进你的文件轮转，故障时"日志不全"。

**单一口径原则**：生产日志系统只能有一个主出口（你配的 sink），所有来源都必须汇聚到它。loguru 为这个提供了两个方向的桥。

## 2. 两个方向的对接

**方向一：loguru 的日志进标准库**——`logger.add(StandardHandler())`，把标准库的日志系统当 sink。适合"你的 loguru 只是配角、项目以标准库生态为主"的存量项目。

**方向二：标准库的日志进 loguru**——`InterceptHandler` 挂到标准库 logger 上，**第三方库的每条日志被转成 loguru 消息**。这是主流方向——你的应用是 loguru 主导，第三方库全部"投降"过来。

两个方向不必互斥，但**生产项目只需要方向二**：loguru 作为唯一出口，第三方库全部截流。方向一的代码形态也给出，遇到"存量项目以标准库为主、只想局部用 loguru"时可以直接抄：

```python
from logging import Handler

class StandardHandler(Handler):
    """把 loguru 的消息桥接成标准库记录——loguru 当配角时用"""
    def emit(self, record):
        import logging
        std = logging.getLogger(record["name"])
        std.log(record["level"].no, record["message"])

logger.add(StandardHandler(), level="INFO")
```

**桥接的对称性**：两座桥都成立、代码都短，说明 loguru 对标准库生态的态度是"开放兼容"而非"取而代之"——**它把选择权留给项目：以谁为主，另一方便是 sink 或被接管者**。

## 3. InterceptHandler：官方接管方案

官方文档给出的 InterceptHandler 就是标准库 Handler 子类 + 一条 `basicConfig` 挂载——**全局接管一行搞定**：

```python
import logging
import sys
from loguru import logger

class InterceptHandler(logging.Handler):
    def emit(self, record):
        # 找到 loguru 对应级别，防止未知级别报错
        try:
            level = logger.level(record.levelname).name
        except ValueError:
            level = record.levelno
        frame, depth = logging.currentframe(), 2
        while frame and frame.f_code.co_filename == logging.__file__:
            frame = frame.f_back
            depth += 1
        logger.opt(depth=depth, exception=record.exc_info).log(level, record.getMessage())

def setup_logging():
    logger.remove()                      # 清默认
    logger.add("logs/app.log", rotation="500 MB", enqueue=True)
    logger.add(sys.stderr, level="INFO")
    logging.basicConfig(handlers=[InterceptHandler()], level=0, force=True)
    # 第三方库统一收敛级别，避免刷屏
    for name in ("uvicorn", "uvicorn.access", "urllib3", "requests",
                 "httpx", "aiohttp"):
        logging.getLogger(name).handlers = [InterceptHandler()]
        logging.getLogger(name).propagate = False
```

逐行理解关键点：`logger.opt(depth=depth)` 修正栈深度，让日志显示的调用位置是**第三方库的原始位置**而不是 InterceptHandler 内部；`exception=record.exc_info` 把标准库的异常信息带给 loguru 的 traceback 体系；`force=True` 覆盖任何既有 basicConfig；`propagate = False` 切断标准库继续向上传播（否则日志被记录两次——一次 loguru 一次标准库 root）。

## 4. propagate 与重复日志

**重复日志是接管后第一高频问题**：同一消息出现两遍——loguru 一遍 + 标准库 root 一遍。根因：`logging` 的 handler 会沿 logger 层级传播，`logging.basicConfig` 挂到 root 后，被 InterceptHandler 处理的记录**继续 propagate 到 root 又打了一遍**。解法就是第 3 节代码里的 `propagate = False`——**接管一个就断一个的传播**。

> 💡 **提示**：如果还有漏网的重复，检查是否 `logging.root.addHandler(...)` 被别的库偷偷加过 handler；统一收口的原则是"**标准库侧只留 InterceptHandler 一个挂载点，root 清空其他 handler**"。

## 5. 第三方库日志治理实战

接管之后的治理靠 06 篇的 filter 字典，对**噪音大户**精准调级：

```python
logger.add("logs/app.log", filter={
    "": "INFO",
    "uvicorn": "WARNING",        # uvicorn 启动横幅，INFO 就够吵了
    "uvicorn.access": "INFO",    # 访问日志保留（埋点线索）
    "urllib3": "WARNING",        # 连接池重试刷屏大户
    "requests": "WARNING",
    "pymysql": "WARNING",        # 每条 SQL 都是 DEBUG 级别的库
})
```

**FastAPI 场景的完整链路**：uvicorn（服务器）→ uvicorn.access（访问日志）→ uvicorn.error → 你的业务 loguru——全部收进 InterceptHandler 后，级别表一配，一套格式、一个轮转文件、统一 JSON（09 篇）——**这才是"日志大一统"的完整形态**（10 篇有 FastAPI 完整实战代码）。

**uvicorn.access 里到底有什么**：它是一条包含客户端 IP、请求方法、路径、状态码、耗时的 INFO 记录——是"谁在什么时候调了什么接口"的埋点线索，业务告警排查（"某个 IP 大量 4xx"）全靠它；**access 与 error 是两个独立的 logger，必须分别接管**，漏掉哪个都会出现"启动横幅有、访问记录没有"的怪异形态。启动参数还能进一步控制：`uvicorn app:app --no-access-log` 关掉访问日志（合规场景不需要时），`--log-level warning` 降噪——**接管之后这些参数与 loguru 的 filter 字典形成双保险，谁生效以实际输出为准**（10 篇验收第 3 条就是查这个）。

## 6. 迁移三步走

把存量项目从标准库迁到 loguru，按此顺序，每步可独立上线：

**第一步：并行双写**——保留原 logging 配置，加 `logger.add(...)` 新写一份 loguru 日志，两套并存对比格式与完整性（发现"loguru 少了哪类日志"的窗口期）。

**第二步：接管第三方**——挂 InterceptHandler + propagate=False + filter 字典，第三方库日志进 loguru，此时两套系统各自完整但内容重叠（业务日志可能双份——先接受，格式统一更重要）。

**第三步：业务代码替换**——逐文件把 `logging.getLogger(__name__)` 替换为 `from loguru import logger`，消息格式 `%` 改 `{}`，`logging.exception` 改 `logger.exception`，替换完成后删掉旧配置——**验收标准：日志文件里没有任何一行来自标准库输出，所有来源格式一致**。

## 7. 常见坑

**坑一：propagate 忘了设**——重复日志，每条两遍（第 4 节）。

**坑二：depth 没修正**——日志里显示的"函数名/行号"全是 InterceptHandler 内部的，排查定位全错（第 3 节代码的 `depth` 循环不可省）。

**坑三：basicConfig 被覆盖**——项目里别处调用了 `logging.basicConfig(...)` 又建了 root handler，InterceptHandler 被挤掉——**统一用 `logging.basicConfig(handlers=[...], force=True)` 且全项目只允许一次**。

**坑四：只接管不调级**——uvicorn/requests 全量 DEBUG 进来，日志量爆炸（第 5 节 filter 字典是标配）。

## 8. 练习 5 题

1. 为什么第三方库的日志必须统一接管？"单一口径"指什么？
2. 两个对接方向各自的适用场景？生产为什么主用方向二？
3. InterceptHandler 里 depth 修正和 exception 传递各解决什么问题？
4. 重复日志的根因和两种解法？
5. 迁移三步走每步的验收标准是什么？

> 🎯 **核心要点**：日志大一统 = **InterceptHandler 接管标准库 + propagate=False 防重复 + filter 字典调级 + depth 修正定位**；迁移走"并行双写 → 接管第三方 → 替换业务代码"三步，每步独立可上线——**生产日志只有一套格式、一个出口、一个轮转文件**。

---

**下一模块**：[09-结构化日志与导出.md](09-结构化日志与导出.md) / **返回总览**：[00-loguru总览.md](00-loguru总览.md)
