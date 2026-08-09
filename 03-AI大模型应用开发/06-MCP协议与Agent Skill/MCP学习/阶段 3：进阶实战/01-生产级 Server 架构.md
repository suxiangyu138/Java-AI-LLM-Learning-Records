# 01 生产级 Server 架构

> 从"函数集合"升级为"可维护服务"：生命周期管理、依赖注入、中间件、组合与代理、配置与密钥、错误处理与日志——FastMCP 3.x 实现。

## 📚 目录

1. [生产级 Server 的六项要求](#1-生产级-server-的六项要求)
2. [生命周期：lifespan](#2-生命周期lifespan)
3. [依赖注入：Depends](#3-依赖注入depends)
4. [中间件系统](#4-中间件系统)
5. [组合与代理](#5-组合与代理)
6. [配置与密钥管理](#6-配置与密钥管理)
7. [错误处理与日志](#7-错误处理与日志)
8. [生产级 Server 骨架](#8-生产级-server-骨架)

## 1. 生产级 Server 的六项要求

阶段 2 的最小 Server 只有"工具注册 + run()"，生产级需要补六块：

| 维度 | 最小 Server | 生产级 Server |
|---|---|---|
| 状态管理 | 模块级全局变量 | lifespan 生命周期（初始化/清理） |
| 依赖 | 函数内直接创建 | 依赖注入（连接、客户端、配置） |
| 横切逻辑 | 无 | 中间件（认证/限流/日志/审计） |
| 错误 | 抛异常（变文本） | ToolResult(is_error) + 结构化错误 |
| 配置 | 硬编码 | 环境变量/密钥管理/校验 |
| 可观测 | 无 | 结构化日志 + OTel trace |

> 🎯 判断标准：你的 Server 能否"在不知道函数细节的同事手里部署、排障、扩容"——能，才算生产级。

## 2. 生命周期：lifespan

FastMCP 3.x 用 `@lifespan` 管理资源生命周期，替代"模块级全局变量"这种隐式状态：

```python
from fastmcp import FastMCP, Context
from contextlib import asynccontextmanager

@asynccontextmanager
async def lifespan(server: FastMCP):
    # 启动：连接数据库、加载配置、初始化客户端
    db = await connect_to_db(...)
    yield {"db": db}        # 注入给所有工具
    # 关闭：释放连接、刷日志、优雅退出
    await db.close()

mcp = FastMCP("prod-server", lifespan=lifespan)
```

**lifespan 的三个价值**：
1. **资源只在需要时创建**——不是所有工具都用的重型连接，别在 import 时建；
2. **启动失败可传播**——数据库挂了 Server 直接拒绝启动，而不是带病运行到第一个调用才报错；
3. **关闭有顺序**——连接池、后台任务、日志刷盘的清理顺序确定。

> ⚠️ 无状态迁移注意（03 篇）：**lifespan 管理的是进程内共享资源，不是"会话状态"**。跨请求状态在 2026-07-28 规范下必须用显式句柄（如 `job_id`）传给工具，绝不能把"上次请求的用户数据"存在全局变量里。

## 3. 依赖注入：Depends

工具函数通过 `Depends` 从 lifespan 上下文取依赖，让"工具逻辑"与"资源获取"解耦：

```python
from fastmcp import Depends

async def get_db() -> Database:
    """从 lifespan_context 取连接池。"""
    return Context().lifespan_context["db"]   # 等价写法见下

@mcp.tool
def query_user(user_id: int, db: Database = Depends(get_db)) -> str:
    """按 ID 查用户信息。"""
    return db.fetch_user(user_id)
```

| 写法 | 适用 | 优点 |
|---|---|---|
| 函数内 `Context()` 取 lifespan | 简单场景 | 少一层函数 |
| `Depends(get_db)` | 多工具共用、要复用工厂 | 依赖显式、可测试（测试注入 mock） |
| 直接参数注入（fastmcp 上下文参数） | 要 Context 本体 | 访问 message/arguments/快照 |

**测试红利**：依赖注入让单测可以不启动真数据库——测试里 override `get_db` 返回内存 mock，工具逻辑与基础设施解耦。

## 4. 中间件系统

FastMCP 3.x 中间件是请求管道的钩子，用于横切逻辑：

```python
from fastmcp.middleware import Middleware, MessageContext

class AuditMiddleware(Middleware):
    async def before(self, context: MessageContext) -> None:
        # context.message.name / context.message.arguments
        log_audit(user=context.auth.user, tool=context.message.name)
    async def after(self, context: MessageContext, result) -> None:
        pass

mcp = FastMCP("audited", middleware=[AuditMiddleware()])
```

**中间件典型用途**：

| 中间件 | 做什么 | 注意 |
|---|---|---|
| 认证校验 | 验证令牌、注入用户身份 | 更完备的做法是 OAuth 层（02 篇） |
| 限流 | 按用户/密钥计数 | 分布式限流要外部存储（Redis） |
| 审计日志 | 谁在何时调了什么工具 | **记键不记值**（令牌/PII 风险） |
| 响应改造 | 统一包装、脱敏 | 注意 resultType 完整性（03 篇） |
| 指标 | 请求计数、耗时直方图 | 配 OTel 出口 |

> ⚠️ 顺序陷阱：FastMCP 3.x **先做参数校验再做中间件**——想"剥离未知参数"必须在工具设计层面解决（用 `**kwargs` 或显式参数），不能在中间件里补。

## 5. 组合与代理

多 Server 治理的两件武器（FastMCP 3.x 内建）：

| 模式 | 机制 | 场景 |
|---|---|---|
| 组合（Composition） | 多个 Server 合成一个表面，客户端只见一个端点 | 公司统一出口：订单+库存+用户三个服务合成一个内部门户 |
| 代理（Proxy） | 包装已有 Server，加中间件/过滤/改写，不改上游代码 | 给第三方 Server 加审计、按租户过滤工具、注入默认参数 |

```python
from fastmcp import FastMCP
from fastmcp.proxy import Proxy

upstream = Proxy("https://orders.example.com/mcp")
upstream.add_middleware(AuditMiddleware())     # 不改上游代码
upstream.filter_tools(lambda t: not t.name.startswith("internal_"))

mcp = FastMCP("gateway")
mcp.mount(upstream)                             # 组合进统一表面
```

**组合/代理的治理价值**：客户端只连一个端点；权限、审计、限流集中在一个网关层；上游 Server 可以按团队独立迭代。这是企业级 MCP 架构的标配（与 08 篇企业网关呼应）。

## 6. 配置与密钥管理

生产级 Server 的配置铁律：

| 铁律 | 做法 | 反例 |
|---|---|---|
| 配置即环境 | 全部走环境变量/配置文件 | 硬编码 API Key |
| 密钥不入库 | 用密钥管理（环境、Secret Manager） | 把 Key 写进 server.py |
| 显式声明 | 注册表发布时声明 `isRequired`/`isSecret`（08 篇） | 密钥静默缺失导致运行期才炸 |
| 启动即校验 | 启动时校验全部必填配置，缺则拒绝启动 | 首个调用才报 KeyError |
| 可注入 | 测试环境注入测试密钥 | 测试连生产 API |

```bash
# 十二要素风格：配置全部来自环境
export MCP_PORT=8080
export DATABASE_URL="postgres://..."
export API_KEY="..."          # 或 Secret Manager 注入
python server.py
```

> 💡 FastMCP 3.x 的 `fastmcp-remote` 支持 `fastmcp_access_token_expiry_seconds` 等环境变量控制令牌生命周期——配置外置的典型示例（07 篇）。

## 7. 错误处理与日志

**工具错误的三种形态**：

| 形态 | 做法 | 适用 |
|---|---|---|
| 预期业务失败 | 返回 `ToolResult(is_error=True)`，带结构化内容 | 用户输入不合法、外部 API 拒绝 |
| 工具自身 bug | 抛异常（FastMCP 会转为文本结果） | 代码缺陷——应被测试抓住 |
| 基础设施故障 | 抛异常 + 日志 + trace | 数据库挂了——必须可见、可告警 |

```python
from fastmcp import ToolResult

@mcp.tool
async def transfer(amount: float) -> ToolResult:
    """发起转账（amount 元）。"""
    if amount <= 0:
        return ToolResult(
            is_error=True,
            content="金额必须大于 0",
            structured_content={"code": "INVALID_AMOUNT", "detail": amount},
        )
    ...
```

**结构化错误优于抛异常**的三个理由：错误码可被客户端/模型解析并决策、不污染正常的文本输出、可与提示注入的防御逻辑共存（05 篇）。**FastMCP 3.4.0+ 支持 `is_error` 映射到 `CallToolResult.isError`**。

**日志规范**：结构化 JSON 日志（时间/级别/请求 ID/工具名/耗时/结果码），**不记令牌、密钥、PII 值**；中间件做审计时同样"记键不记值"；配 OTel 时把 trace_id 注入日志行——排障时"请求 ID 串起所有日志"是生产底线（07 篇）。

## 8. 生产级 Server 骨架

把本节全部要素拼装：

```python
# prod_server.py —— 生产级骨架
from contextlib import asynccontextmanager
from fastmcp import FastMCP, Context, Depends, ToolResult

@asynccontextmanager
async def lifespan(server: FastMCP):
    cfg = load_config()                 # 环境变量 + 校验
    db = await connect_db(cfg.database_url)
    yield {"cfg": cfg, "db": db}
    await db.close()

def get_db():
    return Context().lifespan_context["db"]

mcp = FastMCP("prod-server", lifespan=lifespan,
              middleware=[AuditMiddleware()])

@mcp.tool
def get_user(user_id: int, db=Depends(get_db)) -> ToolResult:
    """查询用户（内部服务）。"""
    try:
        u = db.fetch(user_id)
        return ToolResult(content=str(u))
    except NotFound:
        return ToolResult(is_error=True, content="用户不存在")

if __name__ == "__main__":
    mcp.run(transport="stdio")          # 本地；生产用 http（07 篇）
```

**验收五问**：① 换环境只改环境变量？② 数据库故障时启动即失败？③ 每个请求可被日志/trace 串联？④ 工具错误有错误码？⑤ 测试能注入 mock 依赖？——全"是"，才是生产级。

## 9. 配置校验与多环境

用 pydantic-settings 把"环境变量 → 校验过的配置对象"一次做完：

```python
# config.py —— 启动即校验，缺必填拒绝启动
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="MCP_", env_file=".env")

    port: int = 8080
    database_url: str                        # 必填：缺了直接启动失败
    api_key: str = ""                        # 可空：用于无 key 的开发模式
    read_only: bool = True                   # 默认只读（05 篇安全基线）
    log_level: str = "info"

def load_config() -> Settings:
    s = Settings()
    if not s.api_key and not s.read_only:
        raise RuntimeError("写模式必须提供 API_KEY")   # 安全启动校验
    return s
```

| 环境 | 配置来源 | 特征 |
|---|---|---|
| 开发 | `.env` 文件 | 本地库、测试密钥、debug 日志 |
| 测试 | pytest fixture | 内存库、mock 密钥 |
| 预发布 | 环境变量 | 真实依赖、生产数据副本 |
| 生产 | 环境变量 + Secret Manager | 只读默认、正式密钥、审计全开 |

**配置的安全准则**：`.env` 永不入库（`.gitignore`）；生产密钥从 Secret Manager 注入而非环境文件；**"哪个环境能写"由配置显式表达**（`read_only` 标志）——把安全策略写进配置，而不是写在 README 的注意事项里。

## 10. 并发与阻塞调用

生产级 Server 的并发纪律（FastMCP 异步优先）：

| 场景 | 正确姿势 | 反例 |
|---|---|---|
| IO 密集（DB/HTTP） | 原生异步客户端（asyncpg/httpx） | 同步阻塞事件循环 |
| 遗留同步库 | `await asyncio.to_thread(fn, ...)` | 直接调用卡死整个 Server |
| CPU 密集（计算/加密） | 线程池/进程池或异步委托 | 事件循环内裸跑 |
| 共享状态 | 只读不可变 + 显式锁（极少） | 全局可变 dict 竞态 |

```python
@mcp.tool
async def run_report(report_id: str) -> str:
    """生成报表（CPU 密集，异步委托）。"""
    return await asyncio.to_thread(report_engine.run, report_id)
```

**为什么重要**：MCP Server 是**单进程多请求**并发模型——一个工具阻塞，所有用户的所有请求都卡。**"工具函数必须无阻塞"与"状态必须外置"一样，是生产级的基本纪律**。

## 11. Context 对象全解

FastMCP 的 `Context` 是工具与框架的通信接口，生产代码常用成员：

| 成员 | 内容 | 用途 |
|---|---|---|
| `message.name` | 当前工具名 | 审计/日志 |
| `message.arguments` | 工具参数 | 校验/脱敏记录（记键不记值） |
| `auth.user` | 认证用户（02 篇） | 权限判断、审计主体 |
| `lifespan_context` | lifespan yield 的共享资源 | 取依赖（01 篇 get_db） |
| `fastmcp` | Server 实例引用 | 组合场景访问其他工具 |
| `request_id`/trace 上下文 | 请求标识 | 日志串联（07 篇 OTel） |

```python
@mcp.tool
def delete_record(record_id: str) -> str:
    """删除记录（写操作）。"""
    ctx = Context()
    user = ctx.auth.user if ctx.auth else "anonymous"
    if user == "anonymous":
        return "未认证，拒绝删除"            # 认证 + 审计（05 篇）
    log_audit(user=user, action="delete", target=record_id)
    return f"记录 {record_id} 已删除"
```

## 12. Server 拆分与单体决策

"一个 Server 放多少工具"是架构级决策，与后端微服务拆分类似：

| 方案 | 工具规模 | 优点 | 缺点 | 适用 |
|---|---|---|---|---|
| 单体 Server | 5-15 个同域工具 | 部署简单、上下文共享 | 权限粒度粗、演进互相牵连 | 单业务域、团队小 |
| 按域拆分 | 每域 5-7 个 | 权限隔离、独立迭代、按需挂载 | 多端点运维、跨域组合要网关 | 多业务域、组织大 |
| 网关聚合 | 客户端只见一个端点 | 统一认证/审计/限流 | 网关层复杂度 | 企业级（08 篇） |

**拆分判据三问**：① 工具间的数据/权限是否耦合？（耦合→同体）② 迭代节奏是否一致？（一致→同体）③ 部署爆炸半径是否可接受？（不可→拆分）。**"工具数量治理"（04 篇）与"Server 拆分"是一体两面**：前者管单 Server 内的数量，后者管整体拓扑。

**组合/代理的运维收益**（01 篇第 5 节的展开）：组合让"客户端配置一次"，代理让"第三方 Server 可插桩"——**网关模式下，上游 Server 升级对下游透明，权限变更在网关一处生效**。这是企业 MCP 架构的"治理单点"，也是与后端 API 网关同构的成熟模式。

## 13. 错误处理的进阶模式

**重试与幂等**（工具侧工程）：

| 模式 | 机制 | 适用 |
|---|---|---|
| 幂等键 | 工具接收 `idempotency_key`，重复调用返回首次结果 | 支付、下单（客户端自动重试安全） |
| 显式重试 | 错误码 UPSTREAM_TIMEOUT 时客户端决定重试 | 外部 API 抖动 |
| 降级返回 | 主数据源失败返回缓存/默认值（带标记） | 查询类工具 |

```python
@mcp.tool
def create_order(items: list[str], idempotency_key: str, db=Depends(get_db)) -> ToolResult:
    """创建订单（幂等：同 key 重复调用返回同一订单）。"""
    if existing := db.find_by_key(idempotency_key):
        return ToolResult(content=f"订单已存在：{existing.id}",
                          structured_content={"code": "CONFLICT", "idempotent": True})
    order = db.create(items, key=idempotency_key)
    return ToolResult(content=f"订单创建成功：{order.id}")
```

**为什么幂等键重要**：模型（和网络重试）可能重复调用同一工具——**没有幂等键，一次网络抖动就能产生两笔订单**。生产级工具默认思考"被重复调用会怎样"，这是与普通函数最大的心智差异。

## 14. 生产就绪自查表（落地版）

把 01 篇全部要求压缩成一张表，改造自己的 Server 时逐行对照：

| 维度 | 就绪标准 | 你的状态 |
|---|---|---|
| 生命周期 | 资源在 lifespan 创建/关闭；启动失败可传播 | ☐ |
| 依赖 | 全部走 Depends/lifespan；测试可注入 mock | ☐ |
| 横切 | 认证/审计/限流在中间件；记键不记值 | ☐ |
| 配置 | 环境变量外置；启动校验；read_only 标志 | ☐ |
| 错误 | ToolResult 错误码；幂等键；异常即 bug | ☐ |
| 并发 | 无阻塞工具；to_thread 委托；无共享可变状态 | ☐ |
| 日志 | 结构化 + trace_id；无令牌/PII | ☐ |
| 拆分 | 按三问（耦合/节奏/爆炸半径）决策单体或拆分 | ☐ |

**改造顺序建议**：先配置外置与启动校验（改动小收益大），再补 lifespan/Depends（架构性改造），最后加中间件与幂等（锦上添花）。**一次改一块、改完跑测试**——生产化是渐进工程，不是重写。

**生产化的反模式清单**（对照自查）：把 Server 当脚本写（无 lifespan 靠全局变量）、把密钥写进代码（配置外置的缺失）、所有工具同步阻塞（事件循环卡死）、错误全抛异常（调用方无法解析）、日志全裸奔（令牌进日志）、工具无限增长（不做治理）——**中招三项以上，你的 Server 还是"玩具"**，回到本节逐项修。

> 🎯 **核心要点**：生产级 Server = lifespan 管资源、Depends 解依赖、中间件做横切、组合代理管边界、配置外置（环境变量+启动校验+read_only 安全标志）、错误结构化（错误码+幂等键）、日志可串、异步不阻塞、Context 可审计。架构决策看三问（耦合/节奏/爆炸半径），错误设计多想一步（重试会怎样）。核心思想与后端服务一致：**状态生命周期化、依赖显式化、横切集中化、失败可见化、重复无害化**。无状态规范（03 篇）只改变"跨请求状态放哪"，不改变这些工程原则。

---

**下一模块**：[02-认证与授权：OAuth 2.1 落地](02-认证与授权：OAuth 2.1 落地.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
