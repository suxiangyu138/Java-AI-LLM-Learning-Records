# Agent 集成与 SDK 实战

> 教程：把代码执行沙盒接入 Agent（E2B 为主线，含模板管理、MCP 集成、生产规范清单）。写完可直接照抄落地。

## 1. E2B 快速接入（Python）

```bash
pip install e2b-code-interpreter
export E2B_API_KEY=sk-xxx   # console.e2b.dev 获取
```

```python
from e2b_code_interpreter import CodeInterpreter

with CodeInterpreter() as sandbox:
    result = sandbox.notebook.exec_cell(
        "import pandas as pd\n"
        "df = pd.DataFrame({'a': [1,2,3], 'b': [4,5,6]})\n"
        "df.mean()",
        timeout=60,          # 秒，默认 300
    )
    print(result.text)        # 文本输出
    for img in result.results:  # matplotlib 图表等
        img.base64  # 转给 LLM 分析
```

### 通用 Sandbox API（细粒度控制）

```python
from e2b import Sandbox

sandbox = Sandbox.create(template="code-interpreter-v1")  # 显式模板

# 文件操作
sandbox.files.write("/workspace/data.csv", csv_bytes)
content = sandbox.files.read("/workspace/result.json")

# 终端命令（PTY）
proc = sandbox.process.start("pip install requests", timeout=120)

# 生命周期——必须显式终止，否则计费
sandbox.kill()
```

### TypeScript

```typescript
import { Sandbox } from "e2b";
const sandbox = await Sandbox.create("code-interpreter-v1");
await sandbox.runCode("print('hi')", { timeoutMs: 60000 });
await sandbox.files.read("/workspace/out.txt");
await sandbox.kill();
```

## 2. 模板与版本管理

| 模板 | 适用 |
|---|---|
| `base` | 基础命令/文件，无数据科学库 |
| `code-interpreter-v1` | 代码执行/数据分析（默认；run_code/runCode 执行、跨调用上下文持久、PTY 终端） |
| 自定义 | `Template().from_template("code-interpreter-v1")` 预装依赖，版本化缓存（0.4.x） |

> ⚠️ 生产必须**按 `template_id` 固定版本**（如 0.4.3），不要用 `latest` 别名——模板变更导致行为漂移、回归难查。

## 3. 集成模式

### 3.1 作为 Tool 接入 LangChain

```python
from langchain_community.tools import E2BCodeInterpreterTool
# 或经 MCP：E2B 提供 code-interpreter MCP server，工具注册即用
```

### 3.2 MCP 模式

- E2B / OpenSandbox / jupyter-interpreter-mcp 均提供 MCP server——沙箱能力直接注册为 MCP 工具，LLM 侧零胶水
- **Claude Agent SDK 方案**：`@wasmagent/claude-agent-sdk` 把 WASM 沙箱内核（QuickJS/Pyodide/Wasmtime/远程）直接替换宿主 bash 工具——同进程、无容器、无远程 shell
- ⚠️ 教训：**MCP server 自身必须沙箱化**（Wassette 模式：工具作为 WASM Component 跑在 Wasmtime），否则"工具调用 = 宿主调用"（见 [09-安全攻防案例与加固](09-安全攻防案例与加固.md) 的 MCP loopback 提权）

### 3.3 自建 K8s 沙箱池

- kubernetes-sigs/agent-sandbox：SandboxClaim 按需申请 + WarmPool 预创建消除冷启动
- OpenSandbox：BatchSandbox 一次对象写批量创建 100 沙箱（0.92s），侧车任务编排 + `shardTaskPatches` 异构参数

## 4. 生产规范清单

| 项 | 规范 |
|---|---|
| 超时 | 单次执行 30-60s 默认，长任务显式放宽；**三层超时**：执行级 / 会话级 / 总任务级 |
| 会话 | 任务结束必须 `kill()`；复用会话前检查状态；限制单会话时长（E2B 24h 上限） |
| 文件 | 白名单路径（`/workspace`）；敏感文件硬拦截（`.env`/`.ssh`）；大文件流式传输 |
| 密钥 | **零信任注入**：凭据经网络边界注入（Vercel/Cloudflare 模式），沙箱内不落盘凭据、短时化 |
| 重试 | 沙箱创建失败指数退避重试；**执行失败转错误 JSON 回传模型**（消除 80% 卡死），不静默重跑 |
| 资源 | 内存/CPU/PID 配额随会话绑定（OpenSandbox pids=512 防 fork 炸弹） |
| 可观测 | 生命周期 webhook、执行日志（stdout/stderr）、成本追踪；OTel 接入 |
| 地域 | 数据驻留：EU 数据用 EU 区域，BYOC 企业自控 |

## 5. 易错点

- 不 `kill()` 沙箱 → 计费泄漏/资源泄漏（僵尸沙箱池）
- `latest` 模板 → 行为漂移
- 沙箱内放密钥 → 逃逸即失守（凭据必须短时化 + 网络边界注入）
- Pyodide 当安全边界 → RCE（见 [05](05-WASM与语言级沙箱.md)）
- 只设执行超时不设会话超时 → 僵尸会话累积
- MCP server 无沙箱 → 工具调用即宿主调用

## 面试速记

1. E2B 三件套：`Sandbox.create` / `run_code`（或 `notebook.exec_cell`）/ `kill()`——**显式终止**。
2. 模板固定 `template_id`，不用 `latest`；自定义模板版本化缓存。
3. 密钥零信任注入：网络边界注入、不落盘、短时化。
4. MCP server 自身也要沙箱化（Wassette 模式）；Claude Agent SDK 可用 WASM 内核替换宿主 bash。
5. 三层超时 + 失败转 JSON 回传模型，是 Agent 代码执行的稳定性基石。

---

**下一模块**：[09-安全攻防案例与加固](09-安全攻防案例与加固.md) ｜ **返回总览**：[00-代码执行沙盒总览](00-代码执行沙盒总览.md)

## 参考来源

- [E2B SDK 文档与 Cookbook（e2b.dev）](https://e2b.dev/docs)
- [E2B vs Modal（Northflank）](https://northflank.com/blog/e2b-vs-modal)
- [OpenSandbox（GitHub）](https://github.com/opensandbox-group/OpenSandbox)
- [mcp-run-python 归档说明（GitHub）](https://github.com/pydantic/mcp-run-python)
- [@wasmagent/claude-agent-sdk（npm）](https://www.npmjs.com/package/@wasmagent/claude-agent-sdk)
