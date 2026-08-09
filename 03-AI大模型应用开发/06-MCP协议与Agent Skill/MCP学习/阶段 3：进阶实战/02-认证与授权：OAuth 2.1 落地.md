# 02 认证与授权：OAuth 2.1 落地

> 2026 年 MCP 认证主线：六 SEP 对齐 OAuth 2.1、RFC 9728 受保护资源元数据、CIMD 取代 DCR、FastMCP 四种落地策略——从"裸跑裸奔"到"可对外发布"。

## 📚 目录

1. [认证模型：资源服务器与授权服务器分离](#1-认证模型资源服务器与授权服务器分离)
2. [六 SEP：2026 认证加固](#2-六-sep2026-认证加固)
3. [RFC 9728：受保护资源元数据](#3-rfc-9728受保护资源元数据)
4. [CIMD 取代 DCR](#4-cimd-取代-dcr)
5. [令牌生命周期：访问/刷新/受众](#5-令牌生命周期访问刷新受众)
6. [FastMCP 四种认证策略](#6-fastmcp-四种认证策略)
7. [常见落地形态与选型](#7-常见落地形态与选型)
8. [认证排错速查](#8-认证排错速查)

## 1. 认证模型：资源服务器与授权服务器分离

MCP 的 OAuth 2.1 模型把"提供工具"与"发令牌"拆成两个角色：

```text
┌────────────┐  1. 发现：/.well-known/oauth-protected-resource
│ MCP 客户端  │ ──────────────────────────────────────────▶
└─────┬──────┘
      │ 2. 401 + WWW-Authenticate 质询
      ▼
┌────────────┐  3. 指向授权服务器（OAuth 2.1 / OIDC）
│ 授权服务器   │ 4. 登录/授权/发令牌（aud 绑定 MCP Server）
│(AuthKit 等) │ 5. JWKS 公钥端点
└────────────┘
      │ 6. 拿到 Bearer Token
      ▼
┌────────────┐  7. 每次请求 Authorization: Bearer <token>
│ MCP Server  │ 8. 验签 + 校验 aud/iss/过期 —— 通过才执行工具
└────────────┘
```

**角色表**：

| 角色 | 职责 | 说明 |
|---|---|---|
| 资源服务器 | MCP Server 本体：每请求验证 Bearer Token | 只验签，不发令牌 |
| 授权服务器 | 登录、同意、发令牌、JWKS | 独立服务（WorkOS AuthKit、Auth0、自建） |
| 客户端 | 拿令牌、随请求携带 | 支持 OAuth 流程即可 |

**为什么要分离**：MCP Server 是"工具出口"，把登录 UI、同意页、令牌存储堆进去既危险又难维护；分离后 Server 只做一件事——**用授权服务器的公钥验证每个请求的令牌**。

## 2. 六 SEP：2026 认证加固

2026-07-28 规范"Authorization Hardening"一节六个 SEP，全部对齐 OAuth 2.1/OIDC 实践：

| SEP | 内容 | 防什么 |
|---|---|---|
| SEP-2468 | 客户端必须校验授权响应的 `iss`（RFC 9207） | **Mix-up 攻击**：恶意服务器把客户端引导到自己的授权服务器，偷走用户凭据；MCP 单客户端多服务器场景下尤其普遍；未来版本将强制拒绝缺失 iss |
| SEP-2352 | 客户端把注册凭据绑定到授权服务器 `issuer`；资源迁移到新服务器时重新注册 | **凭据重放**：令牌换个目标服务器复用以绕过权限 |
| SEP-837 | 客户端声明 OIDC `application_type`（DCR 时） | 授权服务器默认把桌面/CLI 客户端当"web"、拒绝 localhost 重定向 URI |
| SEP-2207 | 规定从 OIDC 风格授权服务器请求刷新令牌的方式 | 令牌过期即死，客户端无法续期 |
| SEP-2350 | **step-up scope 累加**而非替换 | 高权限操作（如"删除"）的增量授权不清掉已授的低权限 scope |
| SEP-2351 | 统一 `.well-known` 发现后缀 | 各厂商自定义发现路径，客户端无法互操作 |

**理解要点**：六 SEP 几乎全是**客户端侧义务**——2026 认证规范把"安全责任"从服务器单方承担，改为客户端必须配合（验 iss、绑 issuer、声明类型）。**你的客户端不实现 SEP-2468，你的用户就可能被 mix-up 钓鱼**。

## 3. RFC 9728：受保护资源元数据

规范的两条硬性要求：

1. **MCP Server 必须实现 OAuth 2.0 Protected Resource Metadata（RFC 9728）**；
2. **MCP 客户端必须用该元数据做授权服务器发现**。

落地形态：Server 在 `/.well-known/oauth-protected-resource` 发布 JSON，声明授权服务器地址：

```json
{
  "resource": "https://mcp.example.com/",
  "authorization_servers": [
    "https://auth.example.com/"
  ],
  "audience": "https://mcp.example.com/",
  "scopes_supported": ["tools.read", "tools.write"]
}
```

**401 流程**：客户端调用无令牌 → Server 返回 `401` + `WWW-Authenticate` 质询头 → 客户端读取元数据 → 跳转授权服务器 → 拿令牌重试。**没有元数据的 Server 就像没有说明书的 API**——客户端只能猜，猜错就重试地狱。

## 4. CIMD 取代 DCR

2026-07-28 规范最重要的认证变化：**OAuth Client ID Metadata Documents（CIMD）取代 Dynamic Client Registration（DCR）**，DCR 进入 12 个月弃用窗口（仅向后兼容）。

| 维度 | DCR（旧） | CIMD（新） |
|---|---|---|
| 客户端 ID 形态 | 授权服务器注册后下发 | **HTTPS URL 本身就是客户端 ID** |
| 元数据存放 | 授权服务器数据库 | 客户端自己的 URL 文档 |
| 注册步骤 | 动态注册 API 往返 | 无——静态文档 |
| 信任 | 授权服务器信任注册表 | 授权服务器信任域名/托管方 |
| 适合 | 历史兼容 | 新开发（推荐） |

**CIMD 文档示例**（放在 `https://client.example.com/mcp-client.json`）：

```json
{
  "client_id": "https://client.example.com/mcp-client.json",
  "client_name": "My Agent",
  "redirect_uris": ["https://client.example.com/callback"],
  "grant_types": ["authorization_code", "refresh_token"],
  "token_endpoint_auth_method": "private_key_jwt"
}
```

**为什么转向 CIMD**：DCR 在"MCP 客户端多、授权服务器杂"的现实里是最常故障的环节（动态注册被拒、类型误判、凭据泄露）；CIMD 让客户端自描述，授权服务器只需按 URL 取文档——**多客户端时代的工程解耦**。

## 5. 令牌生命周期：访问/刷新/受众

| 令牌 | 生命周期 | 用途 | 关键字段 |
|---|---|---|---|
| Access Token | 短（分钟-小时） | 每请求 Bearer | `aud`（受众绑定）、`iss`、`exp`、scope |
| Refresh Token | 长（天-周） | 换新访问令牌 | 只发给授权服务器，永不外泄 |

**受众绑定（RFC 8707）**：令牌 `aud` 必须等于 MCP Server 的资源标识——防止"A 服务器的令牌去调 B 服务器的工具"。FastMCP 3.2.4 起强化了受众校验（v3.4.2 又恢复了兼容部分含私有 JWS 头参数的提供商）。

**step-up 授权**（SEP-2350）：工具按风险分级——`tools.read` 基础授权即得；`tools.write`（改数据）触发二次授权；**增量累加**：用户已授 read，请求 write 时只补 write，不重新要求 read。

**令牌校验清单**（Server 侧每请求）：签名（JWKS 公钥）→ 过期 → `aud`=本资源 → `iss`=可信授权服务器 → scope 覆盖本工具所需。

## 6. FastMCP 四种认证策略

FastMCP 3.x 内建 OAuth，四档策略（`FastMCP(name=..., auth=auth)`）：

| 策略 | 机制 | 适用 | 实现 |
|---|---|---|---|
| JWTVerifier | 校验已知签发方 JWT | 已有 IdP（Auth0/自建） | `JWTVerifier(issuer=..., jwks_url=...)` |
| 远程 OAuth 提供商 | 走 DCR 的授权服务器 | WorkOS AuthKit、Keycloak（3.2.4+） | `WorkOSProvider(client_id, client_secret)` |
| **OAuth Proxy** | 桥接不支持 DCR 的提供商（GitHub/Google/Azure） | "分享给团队用" | `GitHubProvider(client_id, ...)` 等 |
| 完整 OAuth 实现 | 自己当授权服务器 | 不推荐（除非必须） | — |

**FastMCP 自带端点**：配置后自动暴露 `/.well-known/oauth-authorization-server`、`/authorize`、`/token`，并在**每个调用上验证令牌**；认证用户通过 `ctx.auth` 读取（01 篇审计中间件用它）。

**验证元数据**：

```bash
curl http://localhost:8000/.well-known/oauth-authorization-server
# {"issuer": ..., "authorization_endpoint": ..., "token_endpoint": ...}
```

## 7. 常见落地形态与选型

| 场景 | 推荐形态 | 说明 |
|---|---|---|
| 个人本机使用 | 无认证或简单令牌 | stdio 传输天然受限于本机进程 |
| 团队内部共享 | OAuth Proxy + 团队 IdP | fastmcp-remote + 上游认证；bridge 失败大声报错 |
| 对外 SaaS 服务 | 授权服务器（AuthKit/Auth0）+ RFC 9728 + CIMD | 完整合规：六 SEP 全上 |
| 企业内网 | 自建网关（Keycloak/Entra）+ 审计 | 见 08 篇企业网关 |
| 存量 Legacy | DCR 兼容 | 12 个月弃用窗内尽快迁 CIMD |

**选型金句**：认证不是"有就有"——**没有 `/.well-known` 元数据、没有受众绑定、没有刷新机制的"认证"只是伪装**。对外发布前用 08 篇的消费者检查单自测。

## 8. 认证排错速查

| 现象 | 首查 | 根因示例 |
|---|---|---|
| 401 循环 | WWW-Authenticate 头是否带正确元数据 URL | 元数据路径 404、aud 不匹配 |
| 令牌拒绝 | 校验 `aud`/`iss`/`exp` | 服务器改了域名，客户端令牌仍绑旧 aud |
| localhost 重定向被拒 | 客户端 `application_type` 声明 | 授权服务器把 CLI 当 web 应用（SEP-837） |
| 刷新失败 | Refresh Token 是否发给授权服务器 | 客户端把刷新令牌当 Bearer 用 |
| 间歇 401 | 时钟偏差（验签 `exp`） | NTP 未同步；JWT 校验允许 leeway 处理 |
| 代理链 401 | 上游认证是否转发 | fastmcp-remote 令牌过期未透明刷新（3.4.0 修复） |

## 9. FastMCP 认证代码实战

**JWT 校验（已有 IdP 的最简形态）**：

```python
from fastmcp import FastMCP, Context
from fastmcp.security import JWTVerifier

auth = JWTVerifier(
    issuer="https://auth.myorg.com/",
    jwks_url="https://auth.myorg.com/.well-known/jwks.json",
    required_audience="https://mcp.myorg.com/mcp",   # RFC 8707 受众绑定
    required_scopes=["tools.read"],                  # 基础 scope
)

mcp = FastMCP("order-server", auth=auth)

@mcp.tool
def delete_order(order_id: str) -> str:
    """删除订单（高权限：需要 tools.write scope）。"""
    ctx = Context()
    if "tools.write" not in (ctx.auth.scopes or []):
        return "权限不足：需要 tools.write"          # step-up scope（SEP-2350）
    ...
```

**step-up 授权的两种实现**：

| 方式 | 机制 | 适用 |
|---|---|---|
| scope 检查（上例） | 工具内检查 scope 集合 | 简单分级（read/write） |
| MRTR 确认（03 篇） | 高权限操作返回 input_required 要人工确认 | 危险操作（删除/转账/发布） |

**生产实践两者叠加**：scope 挡住"无授权者"，MRTR 挡住"被注入的模型"——第一道防越权，第二道防欺骗（05 篇六层防御的第 2 层）。

**验证清单（curl 三连）**：

```bash
# 1. 元数据可达（RFC 9728）
curl -s http://localhost:8080/.well-known/oauth-protected-resource
# 2. 无令牌调用 → 401 + WWW-Authenticate
curl -s -i http://localhost:8080/mcp -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
# 3. 带令牌调用 → 200
curl -s http://localhost:8080/mcp -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

## 10. WWW-Authenticate 与 401 协议细节

**质询头的标准形态**（客户端据此发现授权服务器）：

```text
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer realm="mcp", 
  resource="https://mcp.myorg.com/mcp", 
  authorization_servers="https://auth.myorg.com"
```

**客户端处理流程**（02 篇模型图的协议版）：

1. 收到 401 + 质询头；
2. 读取质询中的 `authorization_servers`（或回源取 `/.well-known/oauth-protected-resource`）；
3. 发起授权：请求 `authorization_endpoint`（带 `application_type` 声明，SEP-837）；
4. 回调拿到授权码 → 换令牌（`aud` 绑定 MCP 资源，RFC 8707）；
5. 带 `Authorization: Bearer` 重试原请求。

**常见 401 形态对照**：`401 + 空质询` = 服务器没配元数据（2 篇第 7 节排错表头号）；`401 + WWW-Authenticate` = 正常质询流程；`403` = 已认证但 scope 不足（不是认证问题，是授权问题）。

## 11. Scope 设计：授权的最小单元

Scope 是授权的核心粒度，设计时遵循"最小但够用"：

| 设计原则 | 示例 |
|---|---|
| 按操作方向分 | `tools.read`（查询）/ `tools.write`（修改） |
| 按数据域分 | `orders.read`、`inventory.read`（多域 Server 用） |
| 按危险度分 | `tools.write` 基础写；`tools.admin` 删除/批量 |
| 增量累加 | 授权新 scope 不收回已授 scope（SEP-2350） |
| 每工具最小集 | 工具内声明"需要哪些 scope"，与调用方核对 |

**scope 与工具的关系**：scope 是"能力令牌"，工具是"能力实现"——**一个工具可以要求多个 scope（如 orders.write + orders.admin），一个 scope 可以覆盖多个工具**。工具内检查（02 篇第 9 节的 `ctx.auth.scopes` 检查）就是"授权决定执行"的落点。

**常见设计错误**：① 一个"全权限"scope 让所有工具共享（等于没有粒度）；② scope 名与工具一一对应（数量爆炸）；③ 把"认证"与"授权"混为一谈（能登录≠能用删除工具）。

## 12. 三个落地案例

**案例 A：个人开源 Server（最简单）**。GitHub 上的 sqlite 查询 Server：无认证直接发布（工具只读、无敏感数据）。**判断**：只读 + 无 PII 才允许裸奔；一旦工具能写或查业务数据，至少加 Bearer 校验。

**案例 B：SaaS 公司内部工具（最常见）**。订单查询 Server 给客服团队用：WorkOS/Auth0 授权服务器 + JWT 校验 + `orders.read` scope；写操作（改状态）要求 `orders.write` 并走 MRTR 人工确认。**判断**：这是 90% 场景的标准形态——外部授权服务器 + 服务器端验签 + scope 分级。

**案例 C：受监管金融产品（最严格）**。交易执行 Server：完整 OAuth 2.1（六 SEP 全上）+ CIMD 客户端注册 + step-up scope（小额授信自动、大额二次授权）+ 全量审计 + 企业网关接入。**判断**：监管场景的"认证"从技术组件变成合规证据——**审计与可撤销性与"能登录"同等重要**。

**案例对比启示**：认证方案不是越重越好，而是**与工具的危险度匹配**——"只读个人工具"与"交易执行工具"的认证成本差一个数量级是合理的，但"交易工具裸奔"在任何场景都是事故。

## 13. 认证常见误区

| 误区 | 真相 |
|---|---|
| "加了 OAuth 就安全了" | OAuth 只解决认证；授权粒度（scope）、审计、可撤销同样关键 |
| "JWT 验签就够了" | 验签只是第一步——aud/iss/exp/scope 四查缺一不可 |
| "Bearer 令牌放 URL" | 令牌只走 Authorization 头；URL 会进日志与历史记录 |
| "刷新令牌当访问令牌用" | 刷新令牌只发给授权服务器；泄露刷新令牌=长期会话泄露 |
| "元数据随便配" | `.well-known` 是客户端信任的入口，配错=客户端无法认证 |
| "客户端的事与我无关" | 六 SEP 全是客户端义务——你的客户端不实现，用户就被钓鱼（mix-up） |
| "只读 Server 不需要认证" | 只读+公开数据可以裸奔；只读+业务数据仍需至少 Bearer（防滥用与审计） |

**自测三问**（每部署一个带认证的 Server）：① 换域名后令牌还合法吗？（aud/iss 绑定）② 令牌泄露后能秒撤销吗？（可撤销性）③ 审计日志能回答"谁在何时用了什么"吗？（可追溯）——三问全"是"，认证才算落地。

**认证与无状态的协同**（02 篇与 03 篇的关系）：无状态规范移除了会话，认证正是"每请求身份"的载体——**身份随令牌走、不随会话留**，这让认证本身也变成无状态的：任意实例验签同一套 JWKS、轮询 LB 无需会话亲和、令牌可撤销可轮换。**认证是无状态架构的信任底座**：没有认证，无状态的每请求都是匿名请求；有了认证，无状态的每请求都是可归属的请求——两篇合起来才是"生产级"的完整含义。

> 🎯 **核心要点**：2026 认证 = 资源服务器/授权服务器分离 + 六 SEP 客户端义务 + RFC 9728 元数据 + CIMD 客户端自描述 + 受众绑定 + scope 最小粒度。落地顺序：**先上元数据与 Bearer 校验（单点），再上完整授权服务器与刷新（体验），最后补 CIMD 与 step-up（合规）**。认证方案与工具危险度匹配：只读裸奔可接受，写操作必须授权，危险操作必须确认+审计。认证的价值不是"挡住了坏人"而是"让每个请求可归属、可审计、可撤销"——而这一切从一条规范的 401 质询头开始。

---

**下一模块**：[03-无状态迁移与双代兼容](03-无状态迁移与双代兼容.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
