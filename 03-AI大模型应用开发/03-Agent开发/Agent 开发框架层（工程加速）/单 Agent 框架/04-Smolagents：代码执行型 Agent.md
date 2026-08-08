# 04 Smolagents：代码执行型 Agent

> 定位：代码执行范式的代表——Agent 写 Python 代码而非 JSON 工具调用，多步任务省 ~30% 步数与调用（1.0 稳定版，2026-08 基准）

## 📚 目录

1. [范式：Code-as-Action](#1-范式code-as-action)
2. [双 API：CodeAgent 与 ToolCallingAgent](#2-双-apicodeagent-与-toolcallingagent)
3. [执行机制与 CodeOutput](#3-执行机制与-codeoutput)
4. [五层沙箱：本地执行安全](#4-五层沙箱本地执行安全)
5. [远程沙箱：生产推荐](#5-远程沙箱生产推荐)
6. [2026 现状与陷阱](#6-2026-现状与陷阱)

## 1. 范式：Code-as-Action

```text
传统工具调用：模型输出 JSON {"tool": "search", "args": {...}} → 框架分发执行
代码执行范式：模型直接写 Python 代码 → 沙箱执行

  Agent 要"查订单再退款"时：
  传统：两次 JSON 工具调用，两次往返
  代码：一次写完 search() + refund() 的 Python 代码，一次执行

收益（HuggingFace 实测）：多步任务步骤与 LLM 调用减少 ~30%
  且代码可读可审计——"模型在想什么"变成"模型写了什么"
```

> 🎯 **生态位**：任务本质是"写代码能解决"的场景（文件处理、数据处理、计算、自动化流水线）。核心逻辑 ~1000 行，是最好 fork 和改造的框架——也意味着会话/部署/扩展都要自己搭。

## 2. 双 API：CodeAgent 与 ToolCallingAgent

| API | 动作空间 | 适用 |
|-----|---------|------|
| `CodeAgent` | 生成 Python 代码执行 | 默认推荐——数据/文件/计算类任务 |
| `ToolCallingAgent` | 生成结构化工具调用（JSON） | 工具返回富数据（图/结构化对象）、代码无优势的场景 |

```python
from smolagents import CodeAgent, HfApiModel, tool

@tool
def search_orders(order_id: str) -> str:
    """查询订单状态。参数：订单号。返回订单状态文本。"""
    return f"订单 {order_id}: 已发货"

agent = CodeAgent(tools=[search_orders], model=HfApiModel("Qwen/Qwen3-32B"))
agent.run("查一下 #8823 的状态")
```

> 2026 状态：两个 API 自 1.0 起稳定（2026-04 确认）；**行为一致性要求 smolagents ≥ 1.0**。

## 3. 执行机制与 CodeOutput

```text
执行器统一实现 PythonExecutor 抽象类：
  execute(code) -> CodeOutput{output, logs(捕获 stdout), is_final_answer}

本地执行（LocalPythonExecutor）：AST 解释执行（不用 eval/exec）
远程执行：E2B / Docker / Blaxel / Modal / Pyodide+Deno

执行前可加扫描器（2026 新评估方法论）：
  CodeInjectionScanner / SecretsScanner / MaliciousURLScanner
  5-10ms/步，gate 模型输出后再进执行器
```

```python
CodeOutput(output="...", logs="...", is_final_answer=True)
# 模型用 final_answer(...) 结束任务——is_final_answer 标记终止条件
```

## 4. 五层沙箱：本地执行安全

> ⚠️ **先读红线**：本地沙箱默认**不隔离文件系统与网络**——工具能自由读写文件、发起网络调用；**沙箱不是自动的**，不可信输入必须传 `custom_tools_executor`（如 RestrictedPython）或用远程沙箱。

| 层 | 防护 | 细节 |
|:---:|------|------|
| 1 | Import 白名单 | 默认禁止 import；需显式 `additional_authorized_imports=["json","re",...]`；`DANGEROUS_MODULES` 默认封 `os/sys/subprocess` |
| 2 | 函数白名单 | 只允许安全函数与已提供工具；`DANGEROUS_FUNCTIONS` 封 `eval/exec/os.system`；禁止访问 `random._os` 等危险子模块 |
| 3 | dunder 保护 | 限制双下划线属性访问（防沙箱逃逸技巧） |
| 4 | 操作数上限 | 总计 10,000,000 次操作（防 DoS/资源耗尽） |
| 5 | 超时与循环上限 | while 循环约 1,000,000 次上限，防死循环 |

附加：`SafeSerializer`（数据传递 JSON-safe，默认禁止 pickle）；安全违规抛 `InterpreterError`。

## 5. 远程沙箱：生产推荐

```text
官方立场（2026）：不可信/对抗性输入 → 用远程执行器
  E2BExecutor     E2B 云沙箱
  DockerExecutor  Docker 容器隔离
  BlaxelExecutor / ModalExecutor  高性能远程平台
  Pyodide+Deno    WebAssembly 沙箱

executor_type 参数选择；远程执行器管理环境全生命周期
（执行前发送工具/变量到远程环境）
```

**生产检查单（2026）**：

```text
□ 不可信输入走 DockerExecutor/E2BExecutor
□ additional_authorized_imports 最小化
□ max_steps 设上限
□ agent.memory.steps 审计日志
□ 锁 smolagents 版本
□ 对抗性 prompt 测试
```

## 6. 2026 现状与陷阱

### 6.1 现状

- **1.0 首个稳定版**（2026-06 评审）：改进沙箱、更可靠的工具调用、托管执行环境成为一等特性
- 模型无关：HuggingFace 模型、OpenAI、Anthropic、本地模型均可
- HF Spaces 集成：`sandbox_create` 等沙箱工具支持持久远程开发环境
- 单线程：本地沙箱不支持并行工具调用（对比工程化模块 07 篇并行策略——需自行编排）

### 6.2 陷阱速查

| 陷阱 | 说明 |
|------|------|
| 沙箱非自动 | 默认代码全权限执行；不可信输入必须显式传执行器 |
| 无文件/网络隔离 | 本地沙箱不隔离 FS 与网络；副作用立即执行、无 dry-run |
| 单线程 | 无并行工具调用 |
| 极简即产品 | 会话管理、tracing、部署、规模扩展全靠自己 |
| 生态不成熟 | 三方集成与框架生态远小于 LangGraph/Pydantic AI |
| 语言单一 | Python only（无 JS 版） |

---

## 【参考来源】

- [futureagi.com: Evaluating smolagents in 2026: Code-as-Action Eval](https://futureagi.com/blog/evaluating-smolagents-2026/)
- [DeepWiki: smolagents Code Execution & Security](https://deepwiki.com/huggingface/smolagents/6-code-execution-and-security)
- [The Neural Base: Code execution sandbox（Smolagents Beginner Course）](https://theneuralbase.com/smolagents/learn/beginner/code-execution-sandbox/)
- [The Neural Base: ToolCallingAgent vs CodeAgent: when each](https://theneuralbase.com/smolagents/learn/beginner/toolcallingagent-vs-codeagent-when-each/)
- [GitHub: smolagents sandboxed_execution.py 示例](https://github.com/huggingface/smolagents/blob/main/examples/sandboxed_execution.py)
- [Ship or Skip: SmolAgents 1.0 评审](https://shiporskip.io/tool/hugging-face-smolagents-1-0-stable-production-ready-framework)

---

**返回总览**：[00-总览：单 Agent 框架知识体系](00-总览：单%20Agent%20框架知识体系.md)

**下一模块**：[05-Pydantic AI：类型安全与 Capabilities](05-Pydantic%20AI：类型安全与%20Capabilities.md)
