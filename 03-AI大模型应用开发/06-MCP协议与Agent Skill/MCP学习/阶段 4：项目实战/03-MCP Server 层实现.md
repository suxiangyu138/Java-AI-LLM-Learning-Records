# 03 MCP Server 层实现

> 执行层的"库"：从 01 篇映射矩阵落地的业务工具集——订单/物流/退款/通知四个工具，全部按阶段 3 生产级标准实现（认证/无状态/错误码/幂等/测试）。

## 📚 目录

1. [工具集总览](#1-工具集总览)
2. [读工具：get_order 与 get_shipping](#2-读工具get_order-与-get_shipping)
3. [写工具：update_ship_date 与 refund_order](#3-写工具update_ship_date-与-refund_order)
4. [通知工具：notify](#4-通知工具notify)
5. [MRTR 确认：写操作的协议级设计](#5-mrtr-确认写操作的协议级设计)
6. [数据层与桩服务](#6-数据层与桩服务)
7. [复用阶段 3 的生产级配置](#7-复用阶段-3-的生产级配置)

## 1. 工具集总览

按 01 篇映射矩阵实现的工具清单：

| 工具 | 类型 | 参数 | 副作用 | 确认 |
|---|---|---|---|---|
| get_order | 读 | order_id, include_items | 无 | — |
| get_shipping | 读 | order_id | 无 | — |
| update_ship_date | 写 | order_id, new_date | 改库 | MRTR |
| refund_order | 写 | order_id, reason | 改库+账务 | MRTR（大额/超期） |
| get_refund_stats | 读 | days | 无 | — |
| notify | 写 | user_id, message | 发通知（桩） | — |

**设计检查**（对照阶段 3/04 篇规范）：6 个工具在 5-7 最优区间；读写分离（写工具打 `write` 标签）；描述含"何时用/何时不用/边界"；错误码体系统一（NOT_FOUND/PERMISSION_DENIED/CONFLICT）。

## 2. 读工具：get_order 与 get_shipping

```python
# mcp_servers/order_server/tools/query.py
from fastmcp import FastMCP, ToolResult, Depends

@mcp.tool
def get_order(order_id: str, include_items: bool = False,
              db=Depends(get_db)) -> ToolResult:
    """按订单号查询订单信息。

    何时用：用户询问订单状态、金额、商品时。
    何时不用：物流轨迹查 get_shipping。
    边界：order_id 为平台内部单号，不含第三方单号。
    """
    order = db.get_order(order_id)
    if order is None:
        return ToolResult(is_error=True, content="订单不存在",
                          structured_content={"code": "NOT_FOUND"})
    return ToolResult(content=order.summary(include_items),
                      structured_content={"code": "OK", "order": order.to_dict()})

@mcp.tool
def get_shipping(order_id: str, db=Depends(get_db)) -> ToolResult:
    """查询订单物流轨迹。

    何时用：用户问"到哪了/几天能到"时。
    注意：仅已发货订单有轨迹；未发货返回 CONFLICT 提示。
    """
    order = db.get_order(order_id)
    if order is None:
        return ToolResult(is_error=True, content="订单不存在",
                          structured_content={"code": "NOT_FOUND"})
    if order.status != "shipped":
        return ToolResult(is_error=True, content=f"订单{order.status}，暂无物流轨迹",
                          structured_content={"code": "CONFLICT"})
    return ToolResult(content=db.get_tracking(order_id))
```

**读工具的工程要点**：① 返回 `structured_content` 让编排层直接取结构化数据（05 篇 Skill 步骤用）；② 业务状态冲突用 CONFLICT 错误码（"未发货查物流"是高频错问）；③ **查询即校验**——所有写工具的第一步都调 get_order（防改不存在的单）。

## 3. 写工具：update_ship_date 与 refund_order

```python
# mcp_servers/order_server/tools/write.py
from fastmcp import FastMCP, ToolResult, Context

@mcp.tool(tags={"write"})
def update_ship_date(order_id: str, new_date: str, reason: str = "",
                     db=Depends(get_db)) -> ToolResult:
    """修改订单发货日期。

    何时用：用户要求调整发货时间。
    校验：仅未发货订单可改；新日期不得早于今天。
    边界：改单属写操作，需要授权与确认。
    """
    order = db.get_order(order_id)
    if order is None:
        return ToolResult(is_error=True, content="订单不存在",
                          structured_content={"code": "NOT_FOUND"})
    if order.status != "pending":
        return ToolResult(is_error=True,
                          content=f"订单已{order.status}，无法修改发货日期",
                          structured_content={"code": "CONFLICT"})
    db.update_ship_date(order_id, new_date, reason)
    return ToolResult(content=f"订单{order_id}发货日期已改为 {new_date}",
                      structured_content={"code": "OK"})

@mcp.tool(tags={"write"})
def refund_order(order_id: str, reason: str, db=Depends(get_db)) -> ToolResult:
    """发起订单退款。

    何时用：用户申请退款且符合条件。
    校验：仅已支付订单；超 7 天或金额超 500 元需人工确认。
    边界：退款金额以订单实付为准，不按用户报价。
    """
    order = db.get_order(order_id)
    if order is None:
        return ToolResult(is_error=True, content="订单不存在",
                          structured_content={"code": "NOT_FOUND"})
    if order.status != "paid":
        return ToolResult(is_error=True, content=f"订单{order.status}，不可退款",
                          structured_content={"code": "CONFLICT"})
    needs_human = order.days_since_paid > 7 or order.total > 500
    refund_id = db.create_refund(order_id, reason, needs_human)
    return ToolResult(
        content=f"退款申请已受理（单号{refund_id}）" +
                ("，因超期/大额需人工审核" if needs_human else "，预计 3-5 个工作日到账"),
        structured_content={"code": "OK", "refund_id": refund_id,
                            "needs_human": needs_human})

# 只读模式默认禁用写工具（阶段 3 安全基线）
if cfg.read_only:
    mcp.disable(tags={"write"})
```

**写工具的工程要点**：① `write` 标签 + read_only 默认禁用（阶段 3 五件套）；② 业务校验前置（存在性/状态/条件）；③ 退款返回 `needs_human` 标志——编排层据此走 MRTR（05 篇 Skill 步骤判断）；④ 退款单号即无状态句柄（阶段 3 模式）。

## 4. 通知工具：notify

```python
@mcp.tool(tags={"write"})
def notify(user_id: str, message: str, channel: str = "sms") -> ToolResult:
    """发送通知给用户（短信/站内信）。

    何时用：退款受理回执、投诉升级通知等需要主动触达时。
    注意：消息内容由 Skill 模板生成，工具不加工内容。
    """
    # 桩实现：真实项目接短信/邮件网关（07 篇可替换）
    ok = notifier_stub.send(user_id, message, channel)
    if not ok:
        return ToolResult(is_error=True, content="通知发送失败",
                          structured_content={"code": "UPSTREAM_TIMEOUT"})
    return ToolResult(content=f"已通知用户{user_id}（{channel}）",
                      structured_content={"code": "OK"})
```

**通知工具的定位**：它是"回执闭环"的关键——退款/升级场景的终点是"用户知道结果"。**内容生成在 Skill 层（模板），发送在执行层（工具）**——工具不碰话术，Skill 不碰渠道（04 篇职责分离）。

## 5. MRTR 确认：写操作的协议级设计

阶段 3 的 MRTR（SEP-2322）在项目里如何落地——编排层是"客户端"角色：

```python
# orchestrator/agent.py —— MRTR 确认片段（05 篇完整循环）
async def call_with_confirmation(agent, tool, arguments):
    result = await agent.call_mcp(tool, arguments)      # 第一次调用
    if result.get("resultType") == "input_required":   # 服务器要输入
        prompt = result["inputRequests"][0]["prompt"]
        decision = await ask_user(prompt)              # 交互层确认
        # 重发原调用 + inputResponses + requestState
        return await agent.call_mcp(tool, arguments,
            input_responses=[{"id": result["inputRequests"][0]["id"],
                              "response": decision}],
            request_state=result["requestState"])
    return result
```

**确认策略表**（01 篇映射矩阵的"人工"列落地）：

| 操作 | 确认触发 | 确认呈现 |
|---|---|---|
| update_ship_date | 总是确认 | "确认把 A1002 发货日期改为 8-15？" |
| refund_order | 超 7 天或 >500 元 | "退款需人工审核，确认提交？" |
| notify | 不确认（低风险） | — |

**设计原则**：**确认触发条件写进工具返回（needs_human），呈现文案写进 Skill（04 篇）**——工具决定"要不要确认"，Skill 决定"怎么问"。

## 6. 数据层与桩服务

```python
# data/seed.py —— 造数据（可复现是测试前提）
orders = [
    {"id": "A1001", "status": "shipped", "total": 199.0,
     "paid_days_ago": 3, "ship_date": "2026-08-05"},
    {"id": "A1002", "status": "pending", "total": 88.0,
     "paid_days_ago": 1},
    {"id": "A1003", "status": "paid", "total": 600.0,      # 超 500 → 需人工
     "paid_days_ago": 9},                                   # 超 7 天 → 需人工
    {"id": "A1004", "status": "paid", "total": 299.0, "paid_days_ago": 2},
]
```

**数据设计要点**：种子数据**故意覆盖边界**（超期退款、未发货查物流、大额退款、不存在订单）——06 篇场景剧本直接消费这些边界样本；真实外部 API（短信/物流）用桩服务模拟（`httpx` 拦截或本地 Flask 桩），07 篇部署时替换。

## 7. 复用阶段 3 的生产级配置

| 阶段 3 资产 | 本项目的复用 |
|---|---|
| lifespan + Depends | 数据库连接生命周期与注入（01 篇） |
| 中间件审计 | 每工具调用留痕（07 篇监控数据源） |
| 认证（JWT 校验） | 编排层作为客户端带令牌调 Server |
| read_only 默认 | 写工具默认禁用，配置开启 |
| 错误码体系 | NOT_FOUND/CONFLICT 全工具统一 |
| ToolResult(is_error) | 全部工具结构化返回 |
| 进程内 Client(mcp) 测试 | 06 篇工具测试直接复用 |
| Docker 镜像 | 07 篇容器化直接复用 |

## 8. 工具 Schema 速查表

六个工具的完整接口（03 篇实现 + 04 篇 Skill 引用的依据）：

| 工具 | inputSchema 要点 | outputSchema | 错误码 |
|---|---|---|---|
| get_order | order_id: str(必填), include_items: bool(默认 false) | {code, order} | NOT_FOUND |
| get_shipping | order_id: str(必填) | {code, tracking} | NOT_FOUND/CONFLICT |
| update_ship_date | order_id, new_date(ISO 日期), reason(可选) | {code, message} | NOT_FOUND/CONFLICT |
| refund_order | order_id, reason(必填) | {code, refund_id, needs_human} | NOT_FOUND/CONFLICT |
| get_refund_stats | days: int(默认 7) | {code, stats} | — |
| notify | user_id, message, channel(默认 sms) | {code} | UPSTREAM_TIMEOUT |

**Schema 与 Skill 的契约**：Skill 里写"调用 get_order(order_id=<用户提供的订单号>)"——**工具 Schema 是 Skill 与工具之间的接口文档**；Schema 变更 = Skill 必须同步检查（06 篇技能层测试兜底）。

## 9. 桩服务实现与切换

```python
# mcp_servers/order_server/stubs.py —— 真实外部 API 的替身（01 篇风险表）
import httpx

class NotifierStub:
    """短信/站内信桩：记录发送请求，测试可断言。"""
    sent: list[dict] = []

    def send(self, user_id: str, message: str, channel: str) -> bool:
        self.sent.append({"user_id": user_id, "message": message, "channel": channel})
        return True          # 真实网关切换点：这里换成 httpx.post(网关)

class TrackingStub:
    """物流轨迹桩：按订单状态生成轨迹。"""
    def get(self, order_id: str) -> str:
        return f"2026-08-06 08:00 已从上海分拨中心发出；08-07 预计到达北京"
```

**桩的工程价值**：① 测试确定性（06 篇断言"通知发了什么"）；② 演示可复现（不依赖外部服务）；③ **切换点单一**（换真实网关只改桩内部一行）——07 篇上线时替换。

## 10. 常见错误与排查

| 错误 | 原因 | 解决 |
|---|---|---|
| 写工具不在列表 | read_only 默认开启（阶段 3 安全基线） | 配置开启 + 确认机制就位 |
| refund 返回 CONFLICT | 订单状态不符（未支付/已退款） | 检查业务校验顺序（03 篇第 2 节） |
| get_shipping 无轨迹 | 订单未发货（设计如此） | 返回文案引导用户（Skill 话术） |
| notify 失败 | 桩未初始化 | 检查 lifespan（阶段 3） |
| 工具 Schema 与 Skill 不一致 | 改了工具没改 Skill | 技能层测试兜底（06 篇） |

## 11. 认证接入与权限设计

Server 层的认证（阶段 3 全套复用，本项目简化落地）：

| 场景 | 认证策略 | 说明 |
|---|---|---|
| 本机开发（stdio） | 无认证（进程内隔离） | stdio 天然受限于本机 |
| 团队共享（HTTP） | Bearer + JWT 校验 | 编排器持令牌调用 |
| 写操作 | scope 检查 + MRTR | tools.write scope + 人工确认 |

```python
# 团队共享模式的认证配置（阶段 3 第 2 篇复用）
auth = JWTVerifier(
    issuer="https://auth.myorg.com",
    jwks_url=cfg.jwks_url,
    required_audience="order-server",
    required_scopes=["tools.read"],
)
mcp = FastMCP("order-server", auth=auth)
# 写工具需额外 scope（工具内检查 ctx.auth.scopes）
```

**权限矩阵**（映射矩阵"人工"列的认证侧）：

| 工具 | scope | 确认 |
|---|---|---|
| get_order/get_shipping/get_refund_stats | tools.read | 无 |
| update_ship_date | tools.write | MRTR |
| refund_order | tools.write | MRTR（needs_human 时） |
| notify | tools.write | 无（低风险） |

**权限设计原则**：读工具只需 read scope；写工具 read+write；危险写（退款）read+write+MRTR——**三层递进，越危险门槛越高**（阶段 3 安全模型的项目化）。

## 12. 种子数据与测试数据的分离

数据设计的一个工程细节（测试可复现的关键）：

| 数据集 | 用途 | 特征 |
|---|---|---|
| data/seed.py | 开发/演示 | 4 个订单（含全部边界样本） |
| tests/fixtures/ | 测试专用 | 每个测试独立造数（不共享可变状态） |

```python
# tests/fixtures/orders.py —— 测试隔离（阶段 3 测试纪律）
@pytest.fixture
def fresh_db(tmp_path):
    db = OrderRepo(f"{tmp_path}/test.db")     # 每测试独立库
    db.seed([...])                            # 按用例造数
    return db
```

**分离的价值**：演示数据可以造得"好看"（完整轨迹），测试数据必须"刁钻"（边界全覆盖）——**混用会导致演示卡在测试数据上，或测试漏掉真实边界**。

## 13. 工具层测试要点清单

03 篇 Server 的工具测试（06 篇第一层）直接照此清单写：

| 工具 | 必测用例 | 断言 |
|---|---|---|
| get_order | 存在/不存在/含 items/空 ID | 返回码、结构化数据 |
| get_shipping | 已发货/未发货/不存在 | CONFLICT 文案 |
| update_ship_date | 正常/未发货改单/不存在/非法日期 | 状态变更、错误码 |
| refund_order | 正常/超期/大额/未支付/不存在/重复 | needs_human、CONFLICT |
| get_refund_stats | 正常/0 数据/负数 days | 统计正确 |
| notify | 成功/桩失败 | 发送记录、UPSTREAM_TIMEOUT |

**清单使用方式**：每工具 2-4 个用例（正常+边界+错误）——**"没有用例清单的工具是没写完的工具"**（阶段 4 测试纪律）。

## 14. 性能与并发考虑

Server 层性能（客服场景流量小，但工程习惯要有）：

| 维度 | 本项目要求 | 手段 |
|---|---|---|
| 延迟 | 单工具 <50ms（本地库） | sqlite 索引 + 异步 |
| 并发 | 支持多对话同时调工具 | 无共享可变状态（03 篇） |
| 长任务 | 无需（工具都是秒级） | 有需要时上 tasks 扩展（阶段 3） |
| 资源 | 内存 <512MB | 连接池 + 关闭流式大结果 |

**性能原则**：**先用正确的架构，再优化热点**——本项目流量小，架构正确（无阻塞/无共享状态）已满足；真实压测留给 07 篇上线后的监控（P99 观测）。**工具返回的"体积纪律"**：客服工具返回结构化摘要而非全量数据（订单摘要含关键字段即可）——返回体积决定上下文 token（04 篇预算表），**"少而准"的返回是成本控制的第一环**。**工具的错误文案原则**：错误信息给"用户可行动"的提示（"订单未发货，暂无可查询的物流轨迹"优于"CONFLICT"）——工具的错误码给编排层决策，文案给用户理解，两者都不可省。**测试先行提示**：工具写完立刻补测试（03 篇第 13 节清单）再写下一个——"写一个测一个"的成本远低于"全写完再补测"（错误定位成本随距离线性上升，阶段 4 测试纪律第一条）。

## 15. Server 层面试速记

1. 工具与 Skill 的契约：Schema 是接口文档，Skill 步骤引用它——Schema 变更必须同步 Skill（06 篇兜底）。
2. 写工具三校验：存在性（NOT_FOUND）→ 状态（CONFLICT）→ 条件（政策/权限）。
3. 确认设计：触发条件在工具返回（needs_human），文案在 Skill——触发与呈现分离。
4. 权限三层递进：read scope → write scope → MRTR 人工确认，越危险门槛越高。
5. 数据纪律：种子数据覆盖边界（演示用），测试数据独立隔离（测试用）。
6. 桩服务即切换点：换真实 API 只改桩内部一行，测试与演示不受影响。

## 16. Server 层自查清单

| 检查项 | 通过标准 |
|---|---|
| Schema 质量 | 描述含"何时用/边界"；枚举收窄；默认值减少模型负担 |
| 错误码 | NOT_FOUND/CONFLICT/UPSTREAM_TIMEOUT 全工具统一 |
| 写工具 | write 标签 + read_only 默认禁用 + MRTR 确认 |
| 校验顺序 | 存在性→状态→条件（03 篇第 2 节） |
| 桩服务 | 切换点单一；测试断言发送记录 |
| 种子数据 | 覆盖全部边界样本（超期/大额/不存在/未发货） |
| 测试 | 每工具 2-4 用例（正常+边界+错误） |

**自查时机**：写完 Server 立即自查（06 篇测试前）——**自查清单是"阶段 3 生产级标准"与"本项目业务需求"的交集**，两边都过才进入编排层开发。**Server 层的完成标志**：自查清单全过 + 工具测试全绿 + 5 剧本所需数据在种子数据中可复现——三项齐备，执行层的"库"才算交付，可以开始"程序"（Skill）与"大脑"（编排器）的开发。**面试补充**：本层是"阶段 3 能力"的复用证明——面试被问"阶段 3 学的怎么用上"，答案就是本层的每一个工具（认证/无状态/错误码/确认机制全在 03 篇里落地）。

> 🎯 **核心要点**：Server 层 = 映射矩阵的代码化（6 个工具）+ 读写分离（write 标签/read_only）+ 业务校验前置（存在性/状态/条件）+ 确认触发写进工具返回（needs_human）+ MRTR 客户端实现在编排层 + 认证三层递进（read→write→MRTR）+ 边界种子数据 + 测试数据隔离 + 桩服务（切换点单一）+ Schema 速查表（工具-Skill 契约）+ 工具测试清单 + 性能原则（正确架构优先）+ 自查清单 + 阶段 3 全套生产级资产复用。**验收标准：任一工具能被"不知道业务的调用方"按 Schema 正确使用**（阶段 3 的 Schema 规范），且 5 个剧本所需数据在种子数据里全部可复现。

---

**下一模块**：[04-Agent Skill 层开发](04-Agent Skill 层开发.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
