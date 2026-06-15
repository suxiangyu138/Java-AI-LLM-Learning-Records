# Agent Harness 安全沙箱与工具隔离（实战版）

> **文档定位**：Agent Harness 工程化核心 | 安全管控深度解析
> **核心问题**：如何让 Agent 安全地执行任意代码？如何防止越权操作？沙箱怎么设计？

---

## 一、安全沙箱的三层防护

```
┌─────────────────────────────────────────┐
│           第 0 层：输入验证               │
│   用户输入 → 规则过滤 → Prompt 注入检测    │
└──────────────────┬──────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│           第 1 层：工具级权限             │
│   每个工具声明能力边界 + 参数校验 + 审批    │
└──────────────────┬──────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│           第 2 层：执行沙箱               │
│   Docker 容器 / Firejail / gVisor / seccomp│
└─────────────────────────────────────────┘
```

---

## 二、沙箱技术选型对比

| 方案 | 隔离级别 | 性能损耗 | 启动速度 | 适用场景 |
|---|---|---|---|---|
| **Docker** | 容器级（内核共享） | 5-10% | 秒级 | 群组聊天、多租户 |
| **Firejail** | 应用级（namespace） | <5% | 毫秒级 | 单用户本地执行 |
| **gVisor** | 系统调用拦截 | 10-20% | 秒级 | 高安全要求 |
| **seccomp** | 系统调用过滤 | 忽略不计 | 无启动开销 | 最小权限限制 |
| **WebAssembly** | 字节码沙箱 | <5% | 毫秒级 | 浏览器/Edge 环境 |

### 2.1 Docker 沙箱（OpenClaw 方案）

```yaml
# docker-compose sandbox 配置
services:
  agent-sandbox:
    image: node:22-alpine
    read_only: true              # 只读根文件系统
    tmpfs: /tmp                   # 临时写入
    network_mode: none            # 无网络
    cap_drop: ALL                 # 移除所有能力
    security_opt:
      - no-new-privileges:true    # 禁止提权
    mem_limit: 512m               # 内存限制
    cpu_shares: 512               # CPU 限制
    volumes:
      - ./workspace:/workspace:ro # 只读挂载项目
```

---

## 三、工具三级权限模型

```python
class ToolPermission:
    READ = "read"           # 只读：可自动执行
    WRITE = "write"         # 写入：需确认
    DANGEROUS = "dangerous"  # 危险：需审批 + 审计

class PermissionGuard:
    def __init__(self, mode: str):
        # mode: "full" | "allowlist" | "deny"
        self.mode = mode
        self.allowlist = set()
        self.denylist = {"rm -rf", "chmod 777", "> /dev/sda"}
    
    def check(self, command: str) -> tuple[bool, str]:
        # 1. deny 列表
        for pattern in self.denylist:
            if pattern in command:
                return False, f"命令在禁止列表中: {pattern}"
        
        # 2. allowlist 模式
        if self.mode == "allowlist":
            for allowed in self.allowlist:
                if command.startswith(allowed):
                    return True, ""
            return False, "命令不在白名单中"
        
        # 3. full 模式（危险操作需确认）
        if any(kw in command for kw in ["rm ", "delete", "drop", "truncate"]):
            return True, "⚠️ 危险操作，需用户确认"
        
        return True, ""
```

---

## 四、文件系统隔离

```python
class FileSystemGuard:
    """限制 Agent 的文件访问范围"""
    
    def __init__(self, allowed_paths: list[str]):
        self.allowed_paths = [Path(p).resolve() for p in allowed_paths]
    
    def is_allowed(self, target_path: str, operation: str) -> bool:
        resolved = Path(target_path).resolve()
        
        # 必须在允许的目录下
        if not any(resolved.is_relative_to(allowed) for allowed in self.allowed_paths):
            return False
        
        # 禁止写入敏感文件
        sensitive_patterns = [".env", ".git/config", "id_rsa", ".pem"]
        if operation == "write":
            if any(p in str(resolved) for p in sensitive_patterns):
                return False
        
        return True

# 示例：只允许在 /workspace 目录下操作
fs_guard = FileSystemGuard(["/workspace", "/tmp/sandbox"])
```

---

## 五、网络隔离

```python
class NetworkGuard:
    """控制 Agent 的网络访问"""
    
    def __init__(self, allowed_domains: list[str] = None):
        self.allowed_domains = allowed_domains or []
        self.blocked_domains = [
            "localhost", "127.0.0.1", "0.0.0.0",
            "169.254.", "10.", "172.16.", "192.168."
        ]
    
    def check_url(self, url: str) -> bool:
        parsed = urlparse(url)
        
        # 禁止内网地址
        for blocked in self.blocked_domains:
            if parsed.hostname.startswith(blocked):
                return False
        
        # 允许列表模式
        if self.allowed_domains:
            return any(parsed.hostname.endswith(d) for d in self.allowed_domains)
        
        return True
```

---

## 六、命令执行审批流程

```
Agent 请求执行命令
       ↓
┌──────────────────┐
│  模式匹配过滤     │ → 匹配 deny 列表 → 拒绝
└──────┬───────────┘
       ↓
┌──────────────────┐
│  危险级别判定     │
│  read → 自动通过  │
│  write → 一次确认 │
│  dangerous → 二次 │
└──────┬───────────┘
       ↓
┌──────────────────┐
│  用户审批         │
│  allow/deny/always│
└──────┬───────────┘
       ↓
┌──────────────────┐
│  Sandbox 执行     │
│  超时 + 审计日志  │
└──────────────────┘
```

---

## 七、审计日志

```python
import logging
from datetime import datetime

class AuditLogger:
    def log_execution(self, user_id: str, command: str, 
                       result: str, duration_ms: int):
        entry = {
            "timestamp": datetime.now().isoformat(),
            "user_id": user_id,
            "command": command,
            "result": result,         # "allowed" / "denied" / "error"
            "duration_ms": duration_ms,
            "sandbox_id": os.environ.get("SANDBOX_ID")
        }
        logging.info(f"AUDIT: {json.dumps(entry)}")
        
        # 危险操作额外记录到专用日志
        if any(kw in command for kw in ["rm", "delete", "DROP"]):
            logging.warning(f"DANGEROUS_OP: {json.dumps(entry)}")
```

---

## 八、面试核心要点

1. **Harness 安全三层？** 输入验证 → 工具级权限 → 执行沙箱
2. **沙箱方案怎么选？** 单用户用 Firejail/进程隔离，多租户用 Docker
3. **工具权限怎么分级？** read（自动）/ write（确认）/ dangerous（审批）
4. **文件隔离怎么做？** 只允许在指定目录下操作，禁止操作 .env/密钥文件
5. **网络隔离怎么做？** 禁止内网地址，允许列表模式控制外网访问

---

## 九、极简总结

```
Harness 安全 = 输入校验 + 权限分级 + 沙箱执行 + 审计日志
沙箱 = Docker（多租户）/ Firejail（单用户）/ seccomp（最小权限）
工具分级 = read(自动) / write(确认) / dangerous(审批+审计)
文件 = 白名单目录 + 禁止敏感文件
网络 = 禁止内网 + 允许列表外网
```
