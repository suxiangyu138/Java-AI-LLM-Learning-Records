# 02 - 快速上手与核心 API

> 定位：loguru 的"五分钟上手"篇——`add()` 一函数统治一切——sink 家族、remove、级别体系与自定义 level，掌握这些就能给任何 Python 项目接上日志

---

## 📚 目录

1. [安装与第一个日志](#1-安装与第一个日志)
2. [add()：一函数统治一切](#2-add一函数统治一切)
3. [sink 家族：日志去哪](#3-sink-家族日志去哪)
4. [remove 与 clear：掌控生命周期](#4-remove-与-clear掌控生命周期)
5. [级别体系与自定义 level](#5-级别体系与自定义-level)
6. [与标准库概念映射](#6-与标准库概念映射)
7. [练习 5 题](#7-练习-5-题)

---

## 1. 安装与第一个日志

安装零成本：`pip install loguru`（纯 Python wheel，无编译依赖）。然后第一行日志长这样：

```python
from loguru import logger

logger.info("应用启动，端口 {}", 8080)
logger.warning("配置缺失，使用默认值")
```

什么都不用配置，输出直接打到 stderr，带精确时间戳和绿色 INFO/黄色 WARNING 配色。**这就是 loguru 的核心承诺：`import` 即可用，不存在"忘了配置导致日志消失"**。和标准库对比：标准库不调用 `basicConfig()` 的话，`logger.info()` 默认静默——loguru 把这个"最坑的默认值"直接反转过来了。

## 2. add()：一函数统治一切

`add()` 是 loguru 的绝对核心：它同时替代标准库的 Handler、Formatter、Filter、级别设置四样东西。完整形态：

```python
logger.add(
    "app.log",                    # sink：输出目标
    level="INFO",                 # 该 sink 的最低级别
    format="{time:YYYY-MM-DD HH:mm:ss} | {level} | {message}",
    filter="my_module",           # 只收哪些模块的日志
    rotation="500 MB",            # 文件轮转
    retention="10 days",          # 保留期
    compression="zip",            # 切分后压缩
    enqueue=True,                 # 队列写入（多进程安全）
    backtrace=True,               # 异常跨调用捕获
    diagnose=True,                # 异常时显示变量值（生产慎用）
    serialize=True,               # JSON 行输出
    encoding="utf-8",             # 文件编码
)
```

参数分四组理解：**去哪里**（sink + encoding + compression）、**收什么**（level + filter）、**长什么样**（format + serialize + colorize）、**怎么抗住生产**（rotation + retention + enqueue + backtrace）。后续 04-09 篇逐组展开，这一篇先把每个参数"认识一遍"，知道它存在即可。

## 3. sink 家族：日志去哪

sink 是 loguru 里"日志输出目标"的统称，比标准库的 Handler 更宽泛，五类都收：

**字符串路径**——最常用，直接给文件路径就自动建文件：

```python
logger.add("logs/app.log")                       # 单文件追加
logger.add("logs/app_{time:YYYY-MM-DD}.log")     # 带 {time} 自动分文件
```

**文件对象/流**——`sys.stderr`、`sys.stdout`、打开的文件对象都行，灵活接管子进程输出。

**函数**——任何接受 message 参数的可调用对象，比如把日志转成 HTTP 推送：

```python
def webhook(msg):
    requests.post("http://alert.example.com", json={"msg": str(msg)})

logger.add(webhook, level="ERROR")   # 只有 ERROR 级才触发推送
```

**自定义类**——实现 `write()` 方法即成为 sink，适合对接队列、数据库等复杂目标（`logger.add(MyQueueSink())`）。

**标准库 Handler**——`logging.Handler` 子类可直接作为 sink，这是与标准库互操作的桥梁（08 篇专讲）。

**特殊值**——`logger.add("sys.stderr")` 传字符串也能识别；不传 sink 默认 stderr。一个 logger 可以同时注册多个 sink，同一消息按各 sink 自己的级别/格式独立输出——**开发看彩色 stderr、生产写轮转文件、告警走函数推送，三种目标一次配齐**。

## 4. remove 与 clear：掌控生命周期

默认 logger 自带一个 stderr sink（`logger.add` 返回值是 handler id）。要接管默认行为、让输出只剩自己配的 sink：

```python
logger.remove()          # 移除全部 handler（含默认的 stderr）
logger.remove(handler_id)  # 按 add() 返回值精准移除某个
logger.clear()           # 等价 remove()，清空所有
```

典型用法是应用入口处"重置后重配"：

```python
logger.remove()   # 干掉默认 stderr
logger.add("logs/app.log", rotation="500 MB")
logger.add(sys.stderr, level="DEBUG")   # 自己想要什么配什么
```

> 💡 **提示**：`logger.remove()` 曾在线程并发下偶发 `RuntimeError`（0.7.3 已修复），但生产上仍建议在应用启动时一次性配好 handler，不要在运行中频繁增删。

## 5. 级别体系与自定义 level

loguru 内置七个级别，数字越小越详细：**TRACE(5) < DEBUG(10) < INFO(20) < SUCCESS(25) < WARNING(30) < ERROR(40) < CRITICAL(50)**。比标准库多出 TRACE 和 SUCCESS 两个——TRACE 给极细诊断用，SUCCESS 是 loguru 特色（成功路径单独一种绿色，爬虫"抓取成功"、批处理"任务完成"这类消息用它语义最清晰）。

日志方法就是级别名：`logger.trace(...)` 到 `logger.critical(...)`，方法名即级别，不用再传 level 参数。自定义级别也很直接——给"审计"、"安全告警"这类业务级打上专属标签：

```python
from loguru import logger

logger.level("AUDIT", no=35, color="<magenta>", icon="🔒")
logger.log("AUDIT", "管理员 {} 删除了订单 {}", user_id, order_id)
```

`no=35` 指定数字位置（避开内置值），`color` 决定该级别日志的默认配色，`icon` 在部分 sink 前展示。自定义级别配合 `filter` 可以做"AUDIT 单独进审计文件"的合规场景（06 篇）。

`logger.level()` 还有两个实用姿势：**查询**——`logger.level("AUDIT")` 返回该级别的 no/color/icon 等全部属性（写过滤器时校验级别存在与否）；**修改**——对已存在的级别重新调用 `logger.level("AUDIT", no=38, ...)` 可更新其颜色/图标（审计日志想换配色，一行搞定）。**数字取值建议**：自定义级别夹在相邻内置级别之间（如 35 在 WARNING 与 ERROR 之间），`level="INFO"` 的 sink 会自动放行 35 级日志而拦截 30 级以下的——**级别数字即过滤语义，别和内置值冲突**（`no` 冲突会报错而不是静默覆盖）。

## 6. 与标准库概念映射

| 标准库 logging | loguru 对应 | 差异点 |
|---------------|------------|--------|
| `logging.getLogger()` | `from loguru import logger` | 单例 vs 多 logger |
| `Handler` | `sink`（add 参数） | 类型更宽：路径/流/函数/类通吃 |
| `Formatter` | `format` 参数 | 大括号模板 + 颜色 markup |
| `Filter` | `filter` 参数 | 字符串/函数/字典 |
| `setLevel()` | `level` 参数（per-sink） | 级别是 sink 属性不是 logger 属性 |
| `RotatingHandler` | `rotation` | 内置大小/时间/周期三种模式 |
| `QueueHandler` | `enqueue=True` | 一行开队列 |
| `logging.exception()` | `logger.exception()` | 语义一致，输出更漂亮 |

**映射表的使用方式**：如果你有标准库经验，读这张表就能把知识平移过来；如果你没有，忘掉标准库直接按 loguru 的思维学——**sink 是目标的组合、级别在 add 里配、格式是模板字符串**。

**add() 的返回值是 handler id**——整数句柄，配合 `logger.remove(id)` 精准移除、`logger.levels()` 查看当前全部已配置的 sink 与级别。调试"日志去哪了"的第一工具就是 `logger.levels()`：列出每个 handler 的 sink/级别/格式，一眼看出"哪个 handler 把日志吞了"。

**关于"先配后用"的启动惯例**：loguru 没有"配置必须先行"的强约束——模块代码 import 后直接打日志，默认进 stderr；应用入口配好文件 sink 后，后续日志自动双写。**这种"迟到配置也能生效"的模型意味着业务代码完全不用关心日志系统何时就绪**——与标准库"未 basicConfig 则 INFO 静默"的模型是设计哲学层面的对立，也是 loguru 零样板的根基。

## 7. 练习 5 题

1. `import loguru 后什么都不配，日志打到哪里？为什么说这个设计消灭了"日志丢失"？`
2. add() 的参数分哪四组？各举一个参数例子。
3. 五种 sink 类型各适合什么场景？函数 sink 典型用在哪？
4. 自定义 level 的 `no` 参数为什么不能乱选数字？
5. remove() 和 clear() 的关系？为什么建议启动时一次配好？

> 🎯 **核心要点**：loguru 的全部配置收敛在一个 `add()`——**sink 决定去哪、level/filter 决定收什么、format 决定长什么样、rotation/enqueue 决定扛不扛得住生产**；配合 remove() 重置与自定义 level，五分钟就能给项目装上"开发看彩色、生产写轮转文件、告警走推送"的三路日志。

---

**下一模块**：[03-格式与日志记录.md](03-格式与日志记录.md) / **返回总览**：[00-loguru总览.md](00-loguru总览.md)
