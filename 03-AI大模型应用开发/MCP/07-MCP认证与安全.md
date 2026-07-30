# 07 - MCP 认证与安全

> 🎯 MCP Server 面向多个 LLM Client 提供服务，安全是生产级部署的底线 — 认证、授权、传输加密、输入净化一个不能少

---

## 目录

1. [安全全景图](#1-安全全景图)
2. [OAuth 2.0 认证](#2-oauth-20-认证)
3. [API Key 认证](#3-api-key-认证)
4. [Authorization 授权模型](#4-authorization-授权模型)
5. [传输安全](#5-传输安全)
6. [输入净化与注入防御](#6-输入净化与注入防御)
7. [审计与合规](#7-审计与合规)

---

## 1. 安全全景图

```text
┌──────────────────────────────────────────────────────────────┐
│                   MCP 安全全景                                │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────────────────────────────────────────┐     │
│  │ ① 认证层 (Authentication)                            │     │
│  │   • OAuth 2.0 (Authorization Code + PKCE)           │     │
│  │   • API Key / Bearer Token                          │     │
│  │   • mTLS (双向 TLS 证书认证)                          │     │
│  ├─────────────────────────────────────────────────────┤     │
│  │ ② 授权层 (Authorization)                             │     │
│  │   • Resource-Level ACL (资源级访问控制)               │     │
│  │   • Tool-Level ACL (工具级访问控制)                   │     │
│  │   • User → Server → Tool 权限链                      │     │
│  ├─────────────────────────────────────────────────────┤     │
│  │ ③ 传输层 (Transport Security)                        │     │
│  │   • TLS 1.3 (HTTPS)                                 │     │
│  │   • mTLS (双向认证)                                   │     │
│  │   • API Gateway (限流/审计)                           │     │
│  ├─────────────────────────────────────────────────────┤     │
│  │ ④ 数据层 (Data Security)                             │     │
│  │   • 输入净化 (防注入)                                  │     │
│  │   • 输出过滤 (防泄露)                                  │     │
│  │   • 日志脱敏 (PII保护)                                │     │
│  │   • 数据加密 (传输 + 静态)                             │     │
│  ├─────────────────────────────────────────────────────┤     │
│  │ ⑤ 合规层 (Compliance)                                │     │
│  │   • 审计日志 (谁、何时、做了什么)                      │     │
│  │   • 速率限制 (防滥用)                                  │     │
│  │   • 沙箱隔离 (高风险操作)                              │     │
│  └─────────────────────────────────────────────────────┘     │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. OAuth 2.0 认证

### 2.1 MCP OAuth 2.0 流程

```text
MCP 推荐使用 OAuth 2.0 Authorization Code Grant + PKCE

流程：
  ① Client 发现 Server 的 OAuth 端点（.well-known/mcp）
  ② Client 重定向用户到 Authorization URL
  ③ 用户登录并授权
  ④ Authorization Server 重定向回 Client，携带 code
  ⑤ Client 用 code 换取 access_token + refresh_token
  ⑥ Client 用 access_token 调用 MCP 接口

  关键：OAuth 2.0 只适用于远程传输（HTTP/SSE）
       stdio 本地传输不需要 OAuth（进程级别隔离）
```

### 2.2 OAuth 配置

```jsonc
// Server 的 .well-known/mcp 返回的 OAuth 配置
{
  "endpoints": [
    {
      "url": "https://api.example.com/mcp",
      "transport": "streamable-http",
      "protocolVersions": ["2025-03-26"]
    }
  ],
  "authentication": {
    "type": "oauth2",
    "authorizationUrl": "https://auth.example.com/authorize",
    "tokenUrl": "https://auth.example.com/token",
    "scopes": [
      {
        "name": "weather:read",
        "description": "读取天气数据"
      },
      {
        "name": "weather:alert",
        "description": "发布天气预警"
      },
      {
        "name": "weather:admin",
        "description": "管理天气服务配置"
      }
    ],
    "pkceRequired": true,
    "clientRegistrationUrl": "https://api.example.com/oauth/register"
  }
}
```

### 2.3 Token 管理

```typescript
/**
 * MCP Client 的 OAuth Token 管理
 */
class McpOAuthTokenManager {
  private accessToken: string | null = null;
  private refreshToken: string | null = null;
  private expiresAt: number = 0;

  /**
   * 获取有效的 Access Token（自动刷新）
   */
  async getAccessToken(): Promise<string> {
    if (this.accessToken && Date.now() < this.expiresAt - 60_000) {
      return this.accessToken;  // Token 还有 1 分钟以上有效期
    }

    if (this.refreshToken) {
      await this.refreshAccessToken();
    } else {
      await this.authorize();
    }

    return this.accessToken!;
  }

  private async authorize(): Promise<void> {
    // PKCE: 生成 code_verifier 和 code_challenge
    const codeVerifier = generateCodeVerifier();
    const codeChallenge = await sha256Base64(codeVerifier);

    // 构建 Authorization URL
    const authUrl = new URL("https://auth.example.com/authorize");
    authUrl.searchParams.set("response_type", "code");
    authUrl.searchParams.set("client_id", this.clientId);
    authUrl.searchParams.set("redirect_uri", this.redirectUri);
    authUrl.searchParams.set("code_challenge", codeChallenge);
    authUrl.searchParams.set("code_challenge_method", "S256");
    authUrl.searchParams.set("scope", "weather:read weather:alert");

    // 打开浏览器让用户授权
    console.log("请在浏览器中打开以下链接进行授权：");
    console.log(authUrl.toString());

    // 监听回调获取 code（本地 HTTP Server）
    const code = await this.waitForCallback();

    // 用 code 换取 token
    const response = await fetch("https://auth.example.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "authorization_code",
        code,
        redirect_uri: this.redirectUri,
        client_id: this.clientId,
        code_verifier: codeVerifier,
      }),
    });

    const data = await response.json();
    this.accessToken = data.access_token;
    this.refreshToken = data.refresh_token;
    this.expiresAt = Date.now() + data.expires_in * 1000;
  }

  private async refreshAccessToken(): Promise<void> {
    const response = await fetch("https://auth.example.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        refresh_token: this.refreshToken!,
        client_id: this.clientId,
      }),
    });

    const data = await response.json();
    this.accessToken = data.access_token;
    this.refreshToken = data.refresh_token ?? this.refreshToken;
    this.expiresAt = Date.now() + data.expires_in * 1000;
  }
}
```

---

## 3. API Key 认证

### 3.1 API Key 方式

```text
适用场景：内部工具、CI/CD Pipeline、服务间调用

配置方式（Server 端）：
  Streamable HTTP Header: Authorization: Bearer <api-key>
  
比 OAuth 简单，但缺少：
  • 用户身份识别（所有调用者共用一个 Key）
  • Token 自动刷新
  • 细粒度 Scope 控制
```

```jsonc
// Client 配置 — API Key
{
  "mcpServers": {
    "internal-tools": {
      "transport": "streamable-http",
      "url": "https://tools.internal.company.com/mcp",
      "headers": {
        "Authorization": "Bearer ${INTERNAL_API_KEY}"
      }
    }
  }
}
```

### 3.2 API Key vs OAuth 决策

| 场景 | 推荐方式 | 原因 |
|------|---------|------|
| 公开 SaaS 产品 | OAuth 2.0 | 需要用户登录、多租户隔离 |
| 企业内部工具 | API Key | 内网环境，简化配置 |
| CI/CD Pipeline | API Key | 无用户交互，自动化 |
| Claude Desktop 用户 | OAuth 2.0 | 多用户、多权限 |
| 开发调试 | API Key 或无认证 | 简便优先 |

---

## 4. Authorization 授权模型

### 4.1 三层权限模型

```text
┌─────────────────────────────────────────────────────────────┐
│                   MCP 三层权限模型                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Layer 1: Server 级                                          │
│    用户能否连接到此 MCP Server？                              │
│    → OAuth Scope / API Key                                  │
│                                                              │
│  Layer 2: Tool/Resource 级                                   │
│    用户能调用哪些工具、读取哪些资源？                          │
│    → Tool-Level ACL / Resource-Level ACL                    │
│                                                              │
│  Layer 3: 参数级                                             │
│    用户在调用工具时，参数受哪些限制？                          │
│    → 参数过滤（如：只能查询自己的数据）                        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Tool-Level ACL 实现

```python
"""
MCP Server 内置的 Tool-Level 权限控制
"""
from functools import wraps
from fastmcp import FastMCP
from fastmcp.exceptions import ToolError

mcp = FastMCP(name="secure-server")

# 权限注册表
TOOL_PERMISSIONS = {
    "get_weather":    {"scope": "weather:read",   "risk": "low"},
    "get_forecast":   {"scope": "weather:read",   "risk": "low"},
    "deploy_alert":   {"scope": "weather:alert",  "risk": "high"},
    "manage_config":  {"scope": "weather:admin",  "risk": "critical"},
}


def require_permission(tool_name: str):
    """装饰器：在 Tool 执行前检查权限"""

    def decorator(func):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            # 获取当前请求上下文
            ctx = mcp.get_context()

            required = TOOL_PERMISSIONS.get(tool_name)
            if not required:
                raise ToolError(f"Tool {tool_name} 未注册权限")

            # 检查用户 Scope
            user_scopes = ctx.user.scopes  # 来自 OAuth Token
            if required["scope"] not in user_scopes:
                raise ToolError(
                    f"权限不足：执行 {tool_name} 需要 {required['scope']} 权限\n"
                    f"当前权限：{', '.join(user_scopes)}"
                )

            # 高风险操作额外确认
            if required["risk"] in ("high", "critical"):
                if not ctx.confirmed:
                    raise ToolError(
                        f"⚠️ 高风险操作：{tool_name}\n"
                        f"请确认是否继续\n"
                        f"建议：检查参数后重新调用"
                    )

            return await func(*args, **kwargs)

        return wrapper

    return decorator


@mcp.tool(name="deploy_alert")
@require_permission("deploy_alert")  # 自动检查权限
async def deploy_alert(city: str, alert_type: str, message: str):
    """发布天气预警（需要 weather:alert 权限）"""
    return f"✅ 预警已发布到 {city}"
```

### 4.3 参数级过滤

```python
"""
参数级授权：限制用户只能访问自己的数据
"""

@mcp.tool(name="query_user_data")
async def query_user_data(user_id: str):
    """查询用户数据 — 自动强制为当前用户"""
    ctx = mcp.get_context()

    # 参数级过滤：强制 user_id 为当前用户（防止越权）
    if user_id != ctx.user.user_id:
        if "admin" not in ctx.user.scopes:
            raise ToolError(
                f"权限不足：只能查询自己的数据 (user_id={ctx.user.user_id})"
            )
        # Admin 可以查询任意用户
        log.warning(f"Admin {ctx.user.user_id} 正在查询用户 {user_id} 的数据")

    return await data_service.query(user_id)
```

---

## 5. 传输安全

### 5.1 TLS 配置

```yaml
# MCP Server 的 TLS 配置（生产环境）
server:
  transport: streamable-http
  port: 443
  tls:
    enabled: true
    cert_file: /etc/certs/mcp-server.crt
    key_file: /etc/certs/mcp-server.key
    min_version: "1.3"          # 只允许 TLS 1.3
    cipher_suites:
      - TLS_AES_256_GCM_SHA384
      - TLS_CHACHA20_POLY1305_SHA256
```

### 5.2 stdio → 本地安全

```text
stdio 传输的安全性：

  天然安全特性：
  ✅ 进程隔离 — Server 在独立进程中运行
  ✅ 无网络暴露 — 不接受外部连接
  ✅ OS 级权限控制 — 依赖操作系统用户权限

  仍需注意：
  ⚠️ Server 进程继承 Client 进程的权限
     → 如果 Client 以 root 运行，Server 也可以做任何事
  ⚠️ 环境变量中的敏感信息（API Key 等）
     → 使用 Secret Manager 而非环境变量
  ⚠️ stdio 无认证 — 任何能启动进程的用户都能调用
     → 依赖 OS 用户隔离
```

---

## 6. 输入净化与注入防御

### 6.1 Tool 输入净化

```python
"""
MCP Tool 的输入安全处理
"""
import re
from pathlib import Path
from fastmcp.exceptions import ToolError


class InputSanitizer:
    """Tool 输入净化器"""

    # 危险模式（命令注入、路径遍历等）
    DANGEROUS_SHELL_PATTERNS = [
        r'[;&|`$(){}]',          # Shell 特殊字符
        r'\.\./',                  # 路径遍历
        r'/etc/passwd',           # 敏感文件
        r'rm\s+-rf',              # 危险命令
        r'sudo\s',                # 提权
        r'>\s*/dev/',             # 设备文件
    ]

    # 敏感信息模式（防止泄露到日志）
    SENSITIVE_PATTERNS = {
        'api_key': r'(?i)(api[_-]?key|token|secret|password)\s*[:=]\s*\S+',
        'email': r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}',
        'phone': r'1[3-9]\d{9}',
    }

    @staticmethod
    def sanitize_shell_input(value: str) -> str:
        """净化 Shell 命令参数"""
        for pattern in InputSanitizer.DANGEROUS_SHELL_PATTERNS:
            if re.search(pattern, value):
                raise ToolError(
                    f"输入包含不安全内容，已被拦截。\n"
                    f"检测到模式：{pattern}"
                )
        return value

    @staticmethod
    def sanitize_file_path(value: str, base_dir: Path) -> Path:
        """净化文件路径 — 防止路径遍历攻击"""
        resolved = (base_dir / value).resolve()

        # 必须在允许的基础目录内
        if not str(resolved).startswith(str(base_dir.resolve())):
            raise ToolError(
                f"路径越权：{value} 不在允许的目录范围内"
            )

        return resolved

    @staticmethod
    def mask_log_output(text: str) -> str:
        """日志输出脱敏"""
        for name, pattern in InputSanitizer.SENSITIVE_PATTERNS.items():
            text = re.sub(pattern, f'[REDACTED_{name.upper()}]', text)
        return text


# 在 Tool 中使用
@mcp.tool(name="read_file")
async def read_file(path: str):
    """安全地读取文件"""
    safe_path = InputSanitizer.sanitize_file_path(path, BASE_DIR)

    content = safe_path.read_text()

    # 日志中脱敏
    logger.info(f"读取文件: {safe_path.name} ({len(content)} bytes)")

    return content
```

---

## 7. 审计与合规

### 7.1 审计日志格式

```jsonc
// 每条 MCP 调用产生的审计日志
{
  "timestamp": "2025-07-29T10:30:00.123Z",
  "audit_id": "audit_abc123",

  // 谁（认证信息）
  "principal": {
    "user_id": "user_456",
    "client_id": "claude-code-v2",
    "scopes": ["weather:read", "weather:alert"],
    "ip": "192.168.1.100"
  },

  // 做了什么
  "action": {
    "type": "tool_call",             // tool_call | resource_read | prompt_get
    "server": "weather-server@2.1.0",
    "tool": "deploy_alert",
    "input_summary": "city=北京, alert_type=storm"
    // 注意：不记录完整的敏感参数
  },

  // 结果
  "result": {
    "status": "success",             // success | error | denied
    "duration_ms": 234,
    "error_code": null
  },

  // 上下文
  "context": {
    "session_id": "sess_xyz789",
    "trace_id": "trace_def456",
    "risk_level": "high"
  }
}
```

### 7.2 速率限制

```text
多级速率限制策略：

  User 级：单个用户每分钟最多 N 次工具调用
    → 防止单用户滥用

  Tool 级：高风险工具（如 deploy）每分钟最多 M 次
    → 防止批量破坏性操作

  Server 级：单个 Server 的总 QPS 上限
    → 保护后端服务

  IP 级：单个 IP 的连接数上限
    → 防 DDoS
```

> 🎯 **核心要点**：公开服务必须 OAuth 2.0、所有远程传输必须 TLS、高风险 Tool 必须 Tool-Level ACL、所有输入必须净化、每次调用必须记录审计日志

---

**上一模块**：[06 - MCP Client 开发实战](./06-MCP%20Client开发实战.md)  
**下一模块**：[08 - MCP 高级特性与生态](./08-MCP高级特性与生态.md)  
**返回总览**：[00 - MCP 知识体系总览](./00-MCP知识体系总览.md)
