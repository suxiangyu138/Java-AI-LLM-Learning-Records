# WASM 与语言级沙箱

> 微秒级启动、无 OS 访问的语言级隔离，适合低风险/低延迟场景；**但 Pyodide 不是安全边界**——这是 2026 最重要的教训。不可信代码的安全边界最终应在 OS 层。

## 1. WASM/WASI 原理

| 特性 | 说明 |
|---|---|
| 无系统调用 | 代码只与 WASM 运行时交互，宿主 syscall 不可达——"secure by construction" |
| 线性内存 | 内存模型受限，无指针逃逸，天然防越界 |
| capability 模型 | 文件/网络需显式授予（WASI preview 2 细粒度权限） |
| 启动耗时 | **微秒级**（vs 容器数百 ms、微VM ~125ms） |
| 密度 | 单节点 500-800 会话（wasmsh 实测）——容器密度的 10-100 倍 |

> 💡 2026 生态：WASM 已进主流 Agent 栈——Claude Agent SDK、LangChain Deep Agents、MCP server（Wassette）均有 WASM 沙箱实现；ClamBot 等安全向项目把全部 LLM 代码跑在 QuickJS-in-Wasmtime。

## 2. 语言运行时选型

| 运行时 | 适用语言 | 特点 |
|---|---|---|
| QuickJS（WASM） | JS/TS | 沙箱化 REPL 标准做法：`@langchain/quickjs`、Simon Willison playground；无 `require`/`import`/`fetch`/文件系统（显式桥接除外） |
| MicroQuickJS（MQuickJS） | JS | Bellard 出品（2026 新锐）：内存/时间限制内建（低至 10kB RAM、~100kB ROM）；**正则回溯也触发中断器**——时间限制对病理回溯有效 |
| Pyodide | Python（CPython→WASM） | **⚠️ 不是安全边界**（见下节） |
| Deno | JS/TS | 权限标志模型（`--allow-*` 白名单），Python 生态羡慕的能力模型；但 Pyodide-in-Deno 组合仍不安全 |
| RestrictedPython | Python | 语言级 AST 限制；`__builtins__` 清洗不全会 RCE（CVE-2026-54769） |
| Pydantic Monty | Python 子集 | Rust 写的 Python 子集→WASM，断网断文件断环境变量，仅暴露开发者控制的函数；2026-02 发布，无 class（迭代快） |

## 3. 必知教训：Pyodide ≠ 安全边界

- **CVE-2026-24002**：Pyodide on Node 无有效沙箱屏障——恶意文档可跑任意进程；Grist 修复为改跑 Deno 下（1.7.9+）
- **Pydantic mcp-run-python 已归档**：官方明确"没有安全方式在 Pyodide 里跑不可信 Python 且保持合理延迟"——Pyodide 内 Python 代码可执行**任意 JavaScript**：污染运行时控制后续调用、读写运行时可访问的任何文件、OOM 宿主（Deno 无法限制内存）
- 结论：**Pyodide 只能当"与 OS 隔离"，不能当"与 JS 运行时隔离"**；LLM 生成的 Python 不可信时，边界必须在 OS 层（gVisor/Firecracker）

## 4. 2026 代表实现

| 项目 | 定位 |
|---|---|
| wasmsh（mayflower） | bash 兼容 WASM shell：88 个 bash 工具 + Python 3.13 + pip + 虚拟文件系统；~300ms 冷启动、~6ms 快照恢复、~1.5ms/热命令；可部署浏览器/Pyodide/K8s 沙箱池 |
| @wasmagent/claude-agent-sdk | 把 WASM 内核（QuickJS/Pyodide/Wasmtime/远程）直接作为 Claude Agent SDK 工具，**替换宿主执行 bash**——同进程、无容器、无远程 shell |
| ClamBot | 全部 LLM 代码跑 QuickJS-in-Wasmtime：工具调用审批门、工作区受限文件系统、SSRF 防护、密钥脱敏 |
| wassette（微软） | MCP server：工具作为 WASM Component 在 Wasmtime 内跑，细粒度安全策略——"浏览器级隔离"给第三方 Agent 工具 |
| Monty（Pydantic） | 沙箱内沙箱：可跑在 Pyodide（浏览器）内或独立 WASM 模块；LLM 靠错误信息迭代 |

## 5. 选型建议

| 场景 | 选择 |
|---|---|
| 低风险 JS 代码 | QuickJS / MicroQuickJS（内建内存+时间限制） |
| 浏览器内执行 | WASM 沙箱（Web Worker 隔离） |
| 高密度轻量执行 | wasmsh 类 WASM 环境（~1.5ms/命令） |
| 不可信 Python | **不要用 Pyodide/RestrictedPython 当边界**，上 gVisor/Firecracker |
| MCP 工具执行 | Wassette 模式（WASM Component 化） |

## 6. WASM vs 容器 vs 微VM

| 维度 | WASM | 容器 | 微VM |
|---|---|---|---|
| 启动 | 微秒级 | <1s | ~125ms |
| 密度/节点 | 500-800 会话 | 10-50 容器 | 10-30 沙箱 |
| 能力边界 | capability 模型 | 共享内核 | 独立内核 |
| 生态兼容 | 受限（语言子集） | 完整 | 完整 |
| 适用 | 低风险/轻量 | 可信代码 | 不可信多租户 |

## 面试速记

1. WASM 沙箱：微秒级启动、无 syscall、capability 模型——密度是容器的 10-100 倍。
2. Pyodide 可执行任意 JS，官方已归档 mcp-run-python（CVE-2026-24002）——语言级沙箱≠OS 级沙箱。
3. MicroQuickJS 内建内存/时间限制，正则回溯也中断，JS 沙箱新优选。
4. RestrictedPython 清洗 `__builtins__` 不全会 RCE（CVE-2026-54769）。
5. 不可信代码的边界应在 OS 层（gVisor/Firecracker），语言级只做性能优化。

---

**下一模块**：[06-Jupyter 与 Code-Interpreter](06-Jupyter与Code-Interpreter.md) ｜ **返回总览**：[00-代码执行沙盒总览](00-代码执行沙盒总览.md)

## 参考来源

- [Running Pydantic's Monty Rust sandboxed Python subset in WebAssembly（Simon Willison）](https://simonwillison.net/2026/Feb/6/pydantic-monty/)
- [wasmsh README（GitHub）](https://github.com/mayflower/wasmsh/blob/main/README.md)
- [CVE-2026-24002（THREATINT）](https://cve.threatint.eu/CVE/CVE-2026-24002)
- [mcp-run-python 归档说明（GitHub）](https://github.com/pydantic/mcp-run-python)
- [Extending AI Agents with WebAssembly（Xebia）](https://tech.xebia.ms/security/videos/extending-ai-agents-with-webassembly)
- [@langchain/quickjs（npm）](https://www.npmjs.com/package/@langchain/quickjs)
