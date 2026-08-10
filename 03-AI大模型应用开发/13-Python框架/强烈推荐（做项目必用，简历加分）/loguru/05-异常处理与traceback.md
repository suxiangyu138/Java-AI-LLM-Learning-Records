# 05 - 异常处理与 traceback

> 定位：loguru 最亮眼的卖点——`catch` 装饰器 + `backtrace/diagnose` 把异常从"一堆堆栈帧"变成"带变量值的完整事故现场"——同时讲清 diagnose 的敏感数据泄露风险

---

## 📚 目录

1. [标准库的异常日志差在哪](#1-标准库的异常日志差在哪)
2. [logger.exception 与 catch 装饰器](#2-loggerexception-与-catch-装饰器)
3. [backtrace 与 diagnose：事故现场还原](#3-backtrace-与-diagnose事故现场还原)
4. [安全警告：diagnose 会泄露什么](#4-安全警告diagnose-会泄露什么)
5. [线程与全局异常捕获](#5-线程与全局异常捕获)
6. [异常级别与 onerror 定制](#6-异常级别与-onerror-定制)
7. [常见坑](#7-常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. 标准库的异常日志差在哪

标准库 `logging.exception()` 输出的 traceback 只包含**调用链与文件行号**——你知道崩在第几行，但不知道**为什么崩**：那个除法为什么是零？请求参数是什么？变量值是多少？都得靠"复现 + 加 print"慢慢猜。

**异常日志的三种来源，先分清各自捕获什么**：`logger.exception()` 捕获"当前 except 块里的异常"（显式、局部）；`@logger.catch` 捕获"被装饰函数内任何未处理异常"（声明式、函数级）；`threads=True` 捕获"线程的未捕获异常"（兜底、全局）。**三种来源叠加覆盖了"显式处理、函数边界、后台线程"三个层级**——排查"这条异常日志是哪来的"时，先对号入座是哪种来源。

loguru 的 `backtrace=True` + `diagnose=True` 把这两块补上：backtrace 展示**完整的调用链**（包括异常被中间层捕获又抛出的过程），diagnose 在每个堆栈帧旁**标注局部变量值**。排错效率的差距是数量级的——这也是 loguru 宣称"fully descriptive exceptions"的来源。

## 2. logger.exception 与 catch 装饰器

**在 except 块里用**：

```python
try:
    result = call_api(order_id)
except Exception:
    logger.exception("调用支付接口失败，订单 {}", order_id)
```

`logger.exception()` 自动带上当前异常的完整 traceback，级别是 ERROR。比标准库更进一步的是 `@logger.catch` 装饰器——**函数抛出异常时自动记录并吞掉（返回 None），不用写 try/except**：

```python
@logger.catch
def process_task(task_id):
    payload = fetch(task_id)      # 这里崩了会被自动记录
    return parse(payload)
```

**上下文管理器形态**同样可用：

```python
with logger.catch():
    do_dangerous_thing()
```

`catch` 的默认行为是"记录异常并返回 None"，适合任务队列、爬虫单页抓取这类"单条失败不能拖垮整体"的场景——**一条任务崩了，日志留痕，循环继续**。注意语义：`catch` 不会重新抛出，如果你希望"记录后继续向上抛"，改用 `try/except + logger.exception()` 或手动 `raise`。

## 3. backtrace 与 diagnose：事故现场还原

两个参数在 add() 里配置，决定 sink 输出异常时的详细程度：

```python
logger.add("logs/app.log", backtrace=True, diagnose=True)
```

**backtrace=True**：默认只显示"异常发生位置往下的调用链"（异常传播路径）；开启后显示**完整调用链**，包括异常被捕获、包装、再抛出的所有环节——排查"异常在中间层被吞又冒出来"的诡异问题时是神器。

**diagnose=True**：每个堆栈帧**附上局部变量及其值**。事故现场直接可见：

```text
> File "app.py", line 42, in process_task
    | result = order.amount / order.count
    |         └ 0 / 0
```

变量表在 0.7.3 已修复 Python 3.13 兼容性（此前 3.13 下该功能异常）。**组合认知**：backtrace 管"调用了什么"，diagnose 管"变量是什么"——两者一起开才是完整的事故现场。

## 4. 安全警告：diagnose 会泄露什么

**这是 loguru 文档里写得最醒目的警告：`diagnose=True` 是默认值，且可能泄露敏感数据，生产环境必须评估**。它会把每个帧的局部变量值打印进日志——如果函数里有 `password`、`token`、`api_key`、`request.headers`，这些值会原样进日志文件：

```python
logger.add("app.log", backtrace=True, diagnose=True)   # 危险：生产也开？
```

**生产治理铁律**：`diagnose` 按环境开关——开发环境全开（排错体验拉满），生产环境关闭（`diagnose=False`，保留 backtrace 即可），需要时用 `opt(exception=...)` 对单条异常临时开。示例：

```python
import os

logger.add("app.log", backtrace=True,
           diagnose=os.getenv("ENV") != "production")
```

如果生产必须开 diagnose 排查疑难问题，配合 09 篇的 JSON 脱敏思路（过滤敏感字段后再落盘）。**面试点**：能主动说出"diagnose 默认开、生产要关、泄露风险"的人，说明真正读过文档。

## 5. 线程与全局异常捕获

**线程里的异常不会自动进日志**——这是 Python 的著名坑：线程崩了，主线程毫不知情，日志里什么都没有。`catch` 的 `threads=True` 参数处理这个：

```python
logger.add("app.log", catch=True, threads=True)
```

`threads=True` 让所有线程的未捕获异常都进入 loguru 的记录体系（包括后台线程、`ThreadPoolExecutor` 的 worker）。**全局兜底**：`@logger.catch` 装饰主函数 + `threads=True` 覆盖子线程，一套配置全项目异常留痕。注意：`threads=True` 只接管"未捕获"的异常——`except Exception:` 里被吞掉的异常它管不了，那要靠代码规范（别空 except）。

**asyncio 任务的异常也要单独处理**：`asyncio.create_task()` 的任务异常默认只会打印到 stderr，不进任何日志系统——**在任务内部用 catch 装饰，或在取结果处显式 `task.exception()`**；loguru 的 `threads=True` 只管线程，不管协程任务，这是两个独立的坑（FastAPI 的 BackgroundTasks 同理，回调里要自己 try/except + `logger.exception`）。

## 6. 异常级别与 onerror 定制

`catch` 默认把异常记成 ERROR 并返回 None，两个参数可定制：`level`（记录级别，比如"预期内的小失败"记成 WARNING）和 `onerror`（自定义回调，比如失败时发告警）：

```python
@logger.catch(level="WARNING", onerror=lambda: notify_admin())
def fetch_maybe_fail():
    ...
```

组合用法：**catch + onerror = "失败留痕 + 触发告警"**，一个装饰器完成"记录与通知"两件事；`reraise=True` 参数则让 catch 记录后继续抛出（装饰器形态下配合调用方处理）。异常对象的 `__repr__` 里再抛异常会导致递归（0.7.3 修复了 `__repr__` 中抛异常引发无限递归的问题），异常信息本身报错时 loguru 会降级输出简化信息而不是崩溃。

## 7. 常见坑

**坑一：catch 吞异常导致任务假成功**——`@logger.catch` 返回 None，调用方以为"处理完了"；需要感知失败就用 `reraise=True` 或手写 try/except。

**坑二：生产开着 diagnose 裸奔**——泄露密码/token 到日志文件，日志文件再进 ELK，等于把密钥交给全公司（第 4 节铁律）。

**坑三：线程异常查不到**——没配 `threads=True`，后台线程崩了日志里一片空白，误以为服务稳定。

**坑四：只在 add() 里配了 backtrace 但 catch 没开**——`backtrace/diagnose` 是 sink 级参数，`logger.exception()` 与 `catch` 都会生效；但**普通 `logger.info()` 手动附带的 exception 需要 `opt(exception=...)`**，否则异常信息不进日志。

## 8. 练习 5 题

1. 标准库异常日志缺什么？backtrace 和 diagnose 各自补哪块？
2. `@logger.catch` 的默认返回值和 reraise 语义？什么场景必须 reraise？
3. 为什么 diagnose=True 在 0.7.3 修了 3.13 兼容还要强调生产关闭？泄露路径是什么？
4. `threads=True` 解决什么？它管不了什么类型的异常？
5. catch 的 onerror 参数适合什么场景？

> 🎯 **核心要点**：loguru 异常三件套 = **exception()（except 块内留痕）+ catch（装饰器自动捕获，任务级容错）+ backtrace/diagnose（完整调用链 + 变量值还原现场）**；安全底线：**diagnose 开发全开、生产关闭**，配合 threads=True 做到全项目异常零遗漏。

---

**下一模块**：[06-级别控制与过滤器.md](06-级别控制与过滤器.md) / **返回总览**：[00-loguru总览.md](00-loguru总览.md)
