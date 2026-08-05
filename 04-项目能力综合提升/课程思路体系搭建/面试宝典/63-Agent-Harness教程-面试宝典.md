# Agent Harness 面试宝典
> 基于课程大纲全面覆盖面试高频考点 — Harness Engineering 六大核心组件深度解析，面向架构师与高级工程师

## 目录
1. [一、基础概念速答](#一基础概念速答12-18题)
2. [二、深度原理剖析](#二深度原理剖析8-12题)
3. [三、实战场景题](#三实战场景题6-10题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（12-18题）

### 1.1 什么是 Agent Harness？
Agent Harness 是 AI Agent 的**运行时基础设施层**，它为 Agent 提供文件系统、脚本执行、沙箱隔离、工具集成、记忆搜索、上下文工程等核心能力。类比而言：LLM 是 Agent 的大脑，Harness 是 Agent 的身体和神经系统。

> 💡 Harness 的核心价值在于让 LLM 从"聊天机器人"进化为"可执行任务的数字员工"。

### 1.2 AI 开发的三层进化路线是什么？
| 层级 | 名称 | 核心关注点 | 代表技术 |
|------|------|-----------|---------|
| 第一层 | Prompt Engineering（提示工程） | 如何写好 prompt、few-shot、CoT | Prompt 模板、思维链 |
| 第二层 | Context Engineering（上下文工程） | 如何构建和注入高质量上下文 | RAG、向量数据库、Context 窗口管理 |
| 第三层 | Harness Engineering（运行时工程） | 如何让 Agent 安全可靠地执行任务 | Filesystem、Sandbox、Tool Integration |

> 🎯 2026 年是 Harness Engineering 的爆发之年，OpenAI、Anthropic 等顶级 AI 公司都在疯狂投入这一领域。

### 1.3 Harness 与 Framework 的核心区别是什么？
| 维度 | Framework | Harness |
|------|-----------|---------|
| 定位 | 开发框架，提供编程抽象 | 运行时环境，提供执行保障 |
| 关注点 | 如何开发 Agent | 如何让 Agent 安全稳定运行 |
| 核心能力 | 编排、链式调用、Agent 定义 | 文件系统、沙箱、内存、工具集成 |
| 安全 | 开发者自行负责 | 内置沙箱隔离、权限校验 |
| 生产就绪 | 需要额外部署 | 开箱即用，包含监控、限流、日志 |
| 举例 | LangChain, AutoGen, CrewAI | Hermes Agent, OpenAI Code Interpreter |

### 1.4 Hermes Agent 是什么？
Hermes Agent 是一个**生产级 Agent Harness 实现**，提供了六大核心组件的完整方案。它解决了 Agent 从原型到生产部署面临的文件管理、代码执行、安全沙箱、工具集成等关键问题。

### 1.5 六大核心组件是哪六个？
| 编号 | 组件 | 英文 | 核心职责 |
|------|------|------|---------|
| 1 | 文件系统 | Filesystem | 持久化存储、工作目录管理、文件读写 |
| 2 | 脚本执行 | Script Execution | 多语言代码运行、通用问题求解 |
| 3 | 沙箱环境 | Sandbox | 安全隔离、资源限制、防恶意行为 |
| 4 | 工具集成层 | Tool Integration | 连接外部 API、数据库、服务 |
| 5 | 内存与搜索 | Memory & Search | 短期/长期记忆、语义搜索、持续学习 |
| 6 | 上下文工程与验证防护 | Context Engineering & Verification | 上下文窗口管理、防 Context Drift |

### 1.6 什么是 Context Drift（上下文漂移）？
Context Drift 是指 Agent 在执行过程中，上下文逐渐偏离原始目标的现象。常见原因包括：
- 长对话中注意力逐渐衰减
- 工具调用返回大量噪音信息
- 多步推理中中间结论被错误累积
- 上下文窗口溢出导致关键信息被截断

> ⚠️ Context Drift 是生产环境 Agent 最主要的失败模式之一。

### 1.7 三种主流 Harness 设计范式是什么？
| 范式 | 特点 | 适用场景 |
|------|------|---------|
| 沙箱优先（Sandbox-First） | 安全第一，所有代码执行在隔离环境中进行 | 开放平台、多租户场景 |
| 工具优先（Tool-First） | 扩展性第一，丰富的工具集成生态 | 企业内部自动化、工作流编排 |
| 记忆优先（Memory-First） | 连续性第一，强调长期记忆和上下文维持 | 个人助手、长期对话场景 |

### 1.8 模型中心（Model-Centric）与基础设施中心（Infrastructure-Centric）的区别
| 维度 | Model-Centric | Infrastructure-Centric |
|------|--------------|----------------------|
| 核心假设 | 模型越强 = Agent 越强 | 基础设施越强 = Agent 越可靠 |
| 投入方向 | 训练更大更强的模型 | 构建更完善的运行时体系 |
| 瓶颈 | 模型的推理能力 | 系统的安全性和可靠性 |
| 代表公司 | 预训练模型厂商 | Harness 平台厂商 |

### 1.9 Harness Engineering 的两大核心实践是什么？
1. **防护性设计（Protective Design）**：围绕错误处理做系统设计，包括 Retry 机制、Graceful Degradation、Fail-Safe 默认行为
2. **可观测性（Observability）**：日志、链路追踪、性能指标，让 Agent 行为可理解、可调试

### 1.10 什么是文件系统组件在 Harness 中的作用？
FileSystem 组件为 Agent 提供了**持久化存储能力**，包括：
- 工作目录管理：每个 Agent 会话拥有独立的工作目录
- 文件读写：支持多种格式的读写操作
- 快照与恢复：支持会话状态持久化和恢复
- 配额管理：限制磁盘使用空间防止滥用

### 1.11 什么是沙箱组件？为什么需要它？
Sandbox 是 Agent 执行代码的安全隔离环境。必要性：
- **安全隔离**：防止恶意代码访问主机系统
- **资源限制**：CPU、内存、网络的配额管控
- **状态清理**：每次执行后环境的完全清理
- **审计日志**：所有执行操作均可追溯

### 1.12 Hugging Face 在 Harness 生态中的角色是什么？
Hugging Face 提供模型托管、推理 API、数据集管理和 Tokenizer 工具链，是 Harness 获取和调用 AI 模型的核心基础设施。Transformers 库提供了模型加载/推理接口，datasets 库提供了训练数据管道，Tokenizers 库提供了高效的文本分词能力。

### 1.13 脚本执行组件如何工作？
Script Execution 组件允许 Agent 动态生成并运行代码（Python、Bash、JavaScript 等），是 Agent"动手能力"的核心。执行流程：Agent 生成代码 -> 代码校验（语法检查 + 安全审查）-> 沙箱内执行 -> 结果捕获 -> 输出返回给 Agent。

### 1.14 什么是 Tool Integration 层？
Tool Integration 是 Agent 连接外部世界的桥梁，将 API、数据库、Web 服务等封装为 Agent 可调用的工具函数。每个工具需要提供：名称、描述、输入输出 Schema、执行逻辑、错误处理。

### 1.15 Hermes Agent 与 OpenClaw 的核心区别？
| 维度 | Hermes Agent | OpenClaw |
|------|-------------|----------|
| 定位 | 通用 Agent Harness | 代码专用 Agent |
| 文件系统 | 通用工作目录 | Git 仓库集成 |
| 脚本执行 | 多语言支持 | 以 Python 为主 |
| 工具生态 | 通用 API 集成 | 开发工具链为主 |
| 部署模型 | 云原生 + 本地 | 本地优先 |

---

## 二、深度原理剖析（8-12题）

### 2.1 Harness Engineering 如何解决 LLM 的先天缺陷？
LLM 存在三大先天缺陷，Harness 分别对应解决：

| LLM 缺陷 | 具体表现 | Harness 解决方案 |
|---------|---------|-----------------|
| 无持久化状态 | 每次对话都是新的开始 | FileSystem + Memory 组件提供持久化 |
| 无法执行代码 | 只能生成文本，不能实际操作 | Script Execution + Sandbox 组件 |
| 无外部感知 | 知识截止于训练数据 | Tool Integration 连接外部 API/DB |

### 2.2 Context Engineering 与 Harness Engineering 有什么区别和联系？
```
Prompt Engineering (怎么写)
      ↓
Context Engineering (给什么上下文)
      ↓
Harness Engineering (怎么执行)
```

| 维度 | Context Engineering | Harness Engineering |
|------|-------------------|-------------------|
| 关注点 | 上下文的质量和相关性 | 任务的执行和保障 |
| 核心技术 | RAG、Chunking、Embedding、重排序 | 沙箱、文件系统、工具编排 |
| 输入 | 用户查询 + 知识库 | Agent 决策 + 上下文 |
| 输出 | 优化的 Prompt + 上下文 | 任务执行结果 |
| 典型组件 | 向量数据库、检索器 | Sandbox、Executor |

### 2.3 六大核心组件之间的协作关系是怎样的？
```
用户输入
    │
    ▼
┌──────────────────────────────────────────┐
│         Context Engineering               │  ← 构建初始上下文
│         构建 & 压缩上下文                   │
└────────────────┬─────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────┐
│         Tool Integration                  │  ← Agent 决策调用哪些工具
│         工具选择 & 参数填充                 │
└────────────────┬─────────────────────────┘
                 │
    ┌────────────┼────────────┐
    ▼            ▼            ▼
┌─────────┐ ┌─────────┐ ┌─────────┐
│ Filesys │ │ Script  │ │ Memory  │  ← 并行执行
│ 文件操作│ │ 代码执行 │ │ 记忆检索│
└─────────┘ └────┬────┘ └─────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│          Sandbox (安全隔离层)               │  ← 所有执行在沙箱内
└────────────────┬─────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────┐
│    Verification & Protection              │  ← 验证结果、防 Context Drift
│         验证 & 防护                        │
└──────────────────────────────────────────┘
                 │
                 ▼
             输出结果
```

### 2.4 Context Drift 的防护机制有哪些？
多层防护体系：

| 层级 | 防护机制 | 说明 |
|------|---------|------|
| 1. 输入层 | Context 压缩与摘要 | 定期对长上下文做 Compress，保留核心信息 |
| 2. 决策层 | Goal Refocus | 每 N 步回顾原始目标，校准方向 |
| 3. 执行层 | 中间结果验证 | 每个工具调用结果做 Schema 校验和合理性检查 |
| 4. 输出层 | 输出与目标一致性检查 | 最终结果与原始目标进行语义匹配校验 |
| 5. 系统层 | 窗口管理 | 上下文窗口溢出预警、滑动窗口策略 |

```python
# Context Drift 防护伪代码示例
class ContextDriftProtector:
    def __init__(self, original_goal: str):
        self.original_goal = original_goal
        self.step_count = 0
        self.context_window = []

    def check_drift(self, current_context: str) -> bool:
        """检查当前上下文是否发生漂移"""
        self.step_count += 1
        # 每 5 步做一次 Goal Refocus
        if self.step_count % 5 == 0:
            similarity = self._compute_semantic_similarity(
                self.original_goal, current_context
            )
            if similarity < 0.6:  # 相似度阈值
                return True  # 发生漂移
        return False

    def refocus(self, current_context: str) -> str:
        """重新聚焦到原始目标"""
        compressed = self._compress_context(current_context)
        return f"[Goal Refocus] 原始目标: {self.original_goal}\n当前进度: {compressed}"
```

### 2.5 如何理解 Harness 从 Model-Centric 到 Infrastructure-Centric 的转变？
行业演进路径：

1. **Phase 1 — 模型为王（2022-2024）**：GPT-4/Claude 等模型能力快速提升，大家认为模型是 Agent 的一切
2. **Phase 2 — 基础设施觉醒（2024-2025）**：发现模型虽强，但在生产环境中频繁失败——文件存取失败、代码执行溢出、上下文丢失
3. **Phase 3 — 基础设施中心化（2025-2026+）**：认识到 Agent 的生产力 = 模型能力 × 基础设施质量，Harness 成为核心投资方向

> 🎯 面试核心点：要有"模型能力是乘法因子，基础设施是底数"的认知。

### 2.6 Hermes Agent 的工作原理是什么？
```python
# Hermes Agent 核心工作流程（概念性代码）
class HermesAgent:
    def __init__(self, harness_config: dict):
        self.filesystem = FileSystemComponent(config=harness_config.get("fs"))
        self.sandbox = SandboxComponent(config=harness_config.get("sandbox"))
        self.tools = ToolRegistry(config=harness_config.get("tools"))
        self.memory = MemoryComponent(config=harness_config.get("memory"))
        self.context_engine = ContextEngine(config=harness_config.get("context"))
        self.verifier = VerificationComponent()

    async def run(self, task: str):
        # Step 1: Context Engineering — 构建上下文
        context = await self.context_engine.build_context(task)

        # Step 2: Agent 推理决策
        action = await self.llm.decide(context)  # LLM 决定下一步动作

        # Step 3: 在 Sandbox 中执行
        async with self.sandbox.isolate() as session:
            if action.type == "code":
                result = await session.execute_code(action.payload)
            elif action.type == "tool":
                result = await self.tools.invoke(action.tool_name, action.params)

        # Step 4: 验证与防护
        verified = await self.verifier.validate(result)

        # Step 5: 更新记忆
        await self.memory.store(task, action, verified)

        return verified
```

### 2.7 Harness 如何保证代码执行的安全性？
多层安全架构：

| 安全层 | 措施 | 实现方式 |
|-------|------|---------|
| 代码静态分析 | AST 扫描、危险函数检测 | 阻止调用 `os.system()`、`exec()` 等危险函数 |
| 沙箱隔离 | Docker/KVM/Wasmer 容器 | 限制文件系统、网络访问、系统调用 |
| 资源配额 | CPU/内存/磁盘/网络限制 | CGroup、CFS 调度器配置 |
| 超时控制 | 执行超时自动终止 | SIGKILL 强制终止 + 资源回收 |
| 审计日志 | 所有操作全量记录 | 操作时间、执行用户、调用链 |

```python
# 沙箱执行安全配置示例
SANDBOX_CONFIG = {
    "type": "docker",              # 基于 Docker 的沙箱
    "image": "harness-sandbox:latest",
    "resource_limits": {
        "cpu": "1.0",               # 最多 1 核
        "memory": "512m",           # 最多 512MB 内存
        "disk": "1g",               # 最多 1GB 磁盘
        "network": "none",          # 禁止网络访问（默认）
        "timeout": 30               # 单次执行超时 30 秒
    },
    "allowed_syscalls": [           # 白名单系统调用
        "read", "write", "open", "close",
        "mmap", "munmap", "brk"
    ],
    "forbidden_modules": [          # 禁止导入的 Python 模块
        "os", "subprocess", "ctypes",
        "socket", "requests"
    ]
}
```

### 2.8 模型微调与 Harness Engineering 的关系是什么？
| 维度 | 模型微调 | Harness Engineering |
|------|---------|-------------------|
| 目标 | 提升模型特定能力 | 提升 Agent 执行可靠性 |
| 方法 | 权重更新、LoRA、QLoRA | 基础设施层组件优化 |
| 关注点 | 模型输出质量 | 执行过程的安全与正确 |
| 投入产出比 | 每次微调成本高 | 一次构建，多次复用 |
| 关系 | 互补关系 | 互补关系 |

> 💡 一个高效的 Agent = 微调后的专业模型 + 完善的 Harness 基础设施。

---

## 三、实战场景题（6-10题）

### 3.1 如何设计一个 Code Agent Harness？
需求：Agent 需要能阅读仓库代码、修改文件、运行测试、提交 PR。

```python
class CodeAgentHarness:
    """生产级 Coding Agent Harness 设计"""
    def __init__(self, repo_url: str):
        self.fs = GitFilesystem(repo_url)  # 基于 Git 的文件系统
        self.executor = CodeExecutor(languages=["python", "javascript", "go"])
        self.sandbox = TestSandbox()       # 专为代码测试优化的沙箱
        self.reviewer = CodeReviewer()     # 代码审查组件

    async def implement_feature(self, description: str):
        # 1. 读取仓库上下文
        repo_structure = await self.fs.read_tree()
        # 2. Agent 生成代码
        code = await self.llm.generate_code(description, repo_structure)
        # 3. 沙箱内测试
        test_result = await self.sandbox.run_tests(code)
        if not test_result.passed:
            return test_result.errors
        # 4. 代码审查
        review = await self.reviewer.review(code)
        # 5. 写入文件
        await self.fs.write_file(code)
        return review
```

**面试加分点**：强调 Git 集成、Test Sandbox、Code Review 三层质量保障。

### 3.2 如何设计一个多租户 Agent Harness 平台？
核心挑战：多租户隔离、资源公平调度、数据安全。

| 设计维度 | 方案 |
|---------|------|
| 租户隔离 | 每个租户独立 Sandbox + 独立 FileSystem Namespace |
| 资源调度 | 基于加权公平队列（WFQ）分配 GPU/CPU 配额 |
| 数据安全 | 租户数据加密存储，内存隔离，网络隔离 |
| 计费 | Token 消耗 + 执行时长 + 存储用量 |

### 3.3 如何将现有 LangChain/Framework 应用迁移到 Harness？
迁移策略三步走：

1. **基础设施层替换**：将纯 Python 执行替换为 Sandboxed 执行
2. **工具层改造**：为每个 Tool 添加 Schema 校验和错误处理
3. **添加防护层**：引入 Context Drift 检测和超时控制

```python
# 迁移示例：从 LangChain Tool 到 Harness Tool
# Before: LangChain 风格
@tool
def search_web(query: str):
    return requests.get(f"https://api.search.com?q={query}").json()

# After: Harness 风格（带沙箱和校验）
@harness_tool(
    name="search_web",
    input_schema={"query": str},
    output_schema={"results": list, "total": int},
    timeout=10,
    sandbox="network-isolated"
)
async def search_web(ctx: ExecutionContext, query: str):
    async with ctx.sandbox.allowed_network(["api.search.com"]):
        result = await ctx.http.get(f"https://api.search.com?q={query}")
        return result.json()
```

### 3.4 Agent 在处理长任务时上下文溢出怎么办？
多级上下文管理策略：

```python
class ContextManager:
    def __init__(self, max_tokens: int = 128000):
        self.max_tokens = max_tokens
        self.short_term = []    # 近期上下文（保留完整）
        self.long_term = []     # 长期上下文（压缩后）
        self.summary = ""

    def add(self, content: str):
        token_count = self._count_tokens(content)
        # 当前上下文超出阈值时触发压缩
        if self._total_tokens() + token_count > self.max_tokens * 0.8:
            self._compress()
        self.short_term.append(content)

    def _compress(self):
        """将最早的 Short-term 压缩存入 Long-term"""
        oldest = self.short_term.pop(0)
        compressed = self._llm_summarize(oldest)  # LLM 摘要压缩
        self.long_term.append(compressed)
        # 重新构建全局摘要
        self.summary = self._llm_summarize("\n".join(self.long_term[-5:]))

    def get_context(self) -> str:
        return f"[全局摘要]: {self.summary}\n[最近内容]: {''.join(self.short_term)}"
```

### 3.5 工具调用失败时 Agent 应该怎么处理？
Graceful Degradation 模式：

| 失败类型 | 处理策略 | 示例 |
|---------|---------|------|
| 网络超时 | Retry with Backoff（指数退避重试） | 重试 3 次，间隔 1s/2s/4s |
| 参数错误 | Schema 校验失败后让 LLM 重新生成参数 | "参数 xxx 不合法，请提供 yyy 格式" |
| 权限不足 | Fallback 到只读操作 | 无写权限时返回可读数据的子集 |
| 服务不可用 | 返回缓存结果或报错提示 | "搜索服务暂时不可用，这是上次的缓存结果" |

### 3.6 如何设计 Agent 的 Memory 系统？
```python
class AgentMemory:
    """层级记忆系统"""
    def __init__(self):
        self.episodic = EpisodicMemory()    # 情节记忆：对话历史 + 关键事件
        self.semantic = SemanticMemory()    # 语义记忆：知识图谱 + 概念
        self.procedural = ProceduralMemory() # 过程记忆：工具调用模式 + 工作流

    async def remember(self, query: str):
        results = await asyncio.gather(
            self.episodic.search(query),    # 向量相似度搜索
            self.semantic.search(query),    # 知识图谱查询
            self.procedural.match(query)    # 模式匹配
        )
        return self._rerank_and_merge(results)
```

---

## 四、手写代码题（5-8题）

### 4.1 实现一个简单的 Sandbox 执行器
```python
import subprocess
import tempfile
import os
from typing import Optional

class SimpleSandbox:
    """轻量级 Python 代码沙箱执行器"""

    FORBIDDEN_KEYWORDS = ["__import__", "eval", "exec", "open",
                          "os.", "subprocess", "sys.", "socket"]

    def __init__(self, timeout: int = 10, memory_limit: str = "256m"):
        self.timeout = timeout

    def _validate_code(self, code: str) -> bool:
        """静态代码安全检查"""
        for keyword in self.FORBIDDEN_KEYWORDS:
            if keyword in code:
                return False
        return True

    def execute(self, code: str) -> dict:
        """在隔离环境中执行代码"""
        if not self._validate_code(code):
            return {"success": False, "error": "Code validation failed",
                    "output": ""}

        with tempfile.TemporaryDirectory() as tmpdir:
            script_path = os.path.join(tmpdir, "script.py")
            with open(script_path, "w", encoding="utf-8") as f:
                f.write(code)

            try:
                result = subprocess.run(
                    ["python", script_path],
                    capture_output=True, text=True,
                    timeout=self.timeout,
                    cwd=tmpdir
                )
                return {
                    "success": result.returncode == 0,
                    "output": result.stdout,
                    "error": result.stderr,
                    "return_code": result.returncode
                }
            except subprocess.TimeoutExpired:
                return {"success": False, "error": "Execution timeout",
                        "output": ""}

# 使用示例
sandbox = SimpleSandbox(timeout=5)
result = sandbox.execute("print('Hello from Sandbox!')")
print(result["output"])  # Hello from Sandbox!
```

### 4.2 实现一个 Tool Registry（工具注册中心）
```python
from typing import Any, Callable, Dict
from dataclasses import dataclass
import json

@dataclass
class ToolSpec:
    name: str
    description: str
    input_schema: Dict[str, type]
    output_schema: Dict[str, type]
    handler: Callable
    timeout: int = 30

class ToolRegistry:
    """工具注册中心，管理所有 Agent 可调用工具"""

    def __init__(self):
        self._tools: Dict[str, ToolSpec] = {}

    def register(self, spec: ToolSpec):
        """注册工具"""
        self._tools[spec.name] = spec
        print(f"[Registry] Tool '{spec.name}' registered")

    def get_spec(self, name: str) -> ToolSpec:
        """获取工具定义"""
        if name not in self._tools:
            raise KeyError(f"Tool '{name}' not found")
        return self._tools[name]

    def list_tools(self) -> list:
        """列出所有工具（用于 LLM 函数调用）"""
        return [
            {
                "name": spec.name,
                "description": spec.description,
                "parameters": {
                    name: str(dtype.__name__)
                    for name, dtype in spec.input_schema.items()
                }
            }
            for spec in self._tools.values()
        ]

    async def invoke(self, name: str, params: Dict[str, Any]) -> Any:
        """调用工具"""
        spec = self.get_spec(name)
        # 输入参数校验
        for param_name, param_type in spec.input_schema.items():
            if param_name not in params:
                raise ValueError(f"Missing required param: {param_name}")
            if not isinstance(params[param_name], param_type):
                raise TypeError(
                    f"Param '{param_name}' should be {param_type.__name__}"
                )
        # 执行
        result = await spec.handler(**params)
        return result
```

### 4.3 实现 Context Drift 检测器
```python
from sentence_transformers import SentenceTransformer
import numpy as np
from typing import List

class DriftDetector:
    """基于语义相似度的 Context Drift 检测器"""

    def __init__(self, threshold: float = 0.65):
        self.model = SentenceTransformer('all-MiniLM-L6-v2')
        self.threshold = threshold
        self.original_goal_embedding = None

    def set_goal(self, goal: str):
        """设置原始目标"""
        self.original_goal_embedding = self.model.encode(goal)

    def check(self, current_context: str) -> dict:
        """检测上下文漂移"""
        if self.original_goal_embedding is None:
            return {"drifted": False, "similarity": 1.0}

        current_embedding = self.model.encode(current_context)
        similarity = float(np.dot(self.original_goal_embedding, current_embedding)
                          / (np.linalg.norm(self.original_goal_embedding)
                             * np.linalg.norm(current_embedding)))

        return {
            "drifted": similarity < self.threshold,
            "similarity": round(similarity, 4),
            "threshold": self.threshold
        }

# 使用示例
detector = DriftDetector(threshold=0.65)
detector.set_goal("帮我查一下 2026 年的 GDP 数据并生成图表")
result = detector.check("我们来写个贪吃蛇游戏吧")  # 偏离了目标
print(result["drifted"])  # True
```

### 4.4 实现一个基础的 Memory 组件
```python
from collections import deque
from typing import Optional

class SlidingWindowMemory:
    """滑动窗口记忆组件"""

    def __init__(self, window_size: int = 20):
        self.history = deque(maxlen=window_size)
        self.summary = ""

    def add(self, role: str, content: str):
        self.history.append({"role": role, "content": content})

    def get_recent(self, n: int = 5) -> list:
        return list(self.history)[-n:]

    def summarize(self, llm_summarize_fn) -> str:
        """对历史记忆进行摘要"""
        full_history = "\n".join(
            f"{h['role']}: {h['content']}" for h in self.history
        )
        self.summary = llm_summarize_fn(full_history)
        return self.summary

    def get_context(self) -> str:
        if self.summary:
            return f"[历史摘要]: {self.summary}\n[最近消息]: {self.get_recent(3)}"
        return self.get_recent()
```

### 4.5 实现一个文件系统组件
```python
import os
import shutil
from pathlib import Path

class HarnessFileSystem:
    """Agent Harness 文件系统组件"""

    def __init__(self, workspace_root: str = "./workspace"):
        self.root = Path(workspace_root).absolute()
        self.root.mkdir(parents=True, exist_ok=True)
        self.current_dir = self.root

    def read_file(self, path: str) -> str:
        """安全读取文件（防止路径穿越）"""
        full_path = self._resolve_path(path)
        self._validate_path(full_path)
        return full_path.read_text(encoding="utf-8")

    def write_file(self, path: str, content: str):
        """安全写入文件"""
        full_path = self._resolve_path(path)
        self._validate_path(full_path)
        full_path.parent.mkdir(parents=True, exist_ok=True)
        full_path.write_text(content, encoding="utf-8")

    def list_dir(self, path: str = ".") -> list:
        full_path = self._resolve_path(path)
        return [str(p.relative_to(self.root)) for p in full_path.iterdir()]

    def _resolve_path(self, path: str) -> Path:
        """解析路径（防止目录穿越攻击）"""
        full_path = (self.current_dir / path).resolve()
        return full_path

    def _validate_path(self, path: Path):
        """验证路径在 workspace 内"""
        if not str(path).startswith(str(self.root)):
            raise PermissionError(f"Path traversal detected: {path}")

    def cleanup(self):
        """清理工作空间"""
        shutil.rmtree(self.root)
```

### 4.6 实现 Agent 执行循环（主循环）
```python
class AgentLoop:
    """Agent 主执行循环"""

    def __init__(self, llm, harness):
        self.llm = llm
        self.harness = harness
        self.max_steps = 20

    async def run(self, task: str):
        context = [{"role": "user", "content": task}]
        results = []

        for step in range(self.max_steps):
            # LLM 决策
            response = await self.llm.generate(context)

            if response.get("type") == "final_answer":
                results.append(response["content"])
                break

            # Harness 执行
            if response["type"] == "tool_call":
                tool_result = await self.harness.invoke_tool(
                    response["tool_name"],
                    response["parameters"]
                )
                context.append({"role": "assistant",
                                "content": f"调用 {response['tool_name']}"})
                context.append({"role": "tool", "content": str(tool_result)})
                results.append(tool_result)

            elif response["type"] == "code_exec":
                code_result = await self.harness.execute_code(
                    response["code"]
                )
                context.append({"role": "tool", "content": str(code_result)})

            # Context Drift 检测
            drift = self.harness.detect_drift(task, context)
            if drift:
                context = self.harness.refocus_context(task)

        return results
```

---

## 五、系统设计题（3-5题）

### 5.1 设计一个企业级 Agent Harness 平台
**需求**：支持多团队使用、千人并发、安全合规、可观测性。

```
┌─────────────────────────────────────────────────┐
│                   接入层                          │
│     API Gateway | Rate Limiter | Auth               │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│                  编排层                           │
│  Task Scheduler | Orchestrator | State Machine     │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│                Harness 核心层                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │ Sandbox  │ │ Tool Hub │ │ Context Engine    │  │
│  │ 集群管理  │ │ 工具市场  │ │ 窗口管理 + RAG    │  │
│  └──────────┘ └──────────┘ └──────────────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │ Filesys  │ │ Memory   │ │ Verification      │  │
│  │ 分布式存储│ │ 记忆服务   │ │ 校验 + 审计       │  │
│  └──────────┘ └──────────┘ └──────────────────┘  │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│                基础设施层                          │
│  K8s | Docker | GPU Pool | Object Storage         │
└─────────────────────────────────────────────────┘
```

**架构要点**：
- 所有组件无状态水平扩展（Sandbox 除外，使用 Sandbox Pool）
- 工具市场支持插件化注册
- 审计日志全链路追踪，满足 SOC2 合规

### 5.2 设计一个高可用的 Sandbox 集群
```python
# Sandbox 集群设计概念
SANDBOX_CLUSTER = {
    "pool_size": 100,           # 预热 100 个沙箱实例
    "max_concurrent": 1000,     # 最大并发执行数
    "isolation": {
        "type": "gVisor",       # 使用 gVisor 实现内核级隔离
        "network_policy": "default-deny",
        "filesystem": "tmpfs"   # 内存文件系统，用完即焚
    },
    "auto_scaling": {
        "min": 10,
        "max": 500,
        "target_cpu": 70,       # CPU 利用率超过 70% 时扩容
        "cooldown": 60          # 扩容冷却时间 60 秒
    },
    "security": {
        "seccomp_profile": "default.json",
        "capabilities": ["NET_BIND_SERVICE"],
        "read_only_rootfs": True
    }
}
```

### 5.3 设计跨 Agent 协作的 Harness 架构
多 Agent 协作场景（如 AutoGPT/Multi-Agent 模式）：

| 通信模式 | 适用场景 | Harness 支持方式 |
|---------|---------|-----------------|
| 广播 | 任务分发 | Message Bus（Redis Pub/Sub） |
| 点对点 | 任务委派 | Direct Channel（gRPC Stream） |
| 主从 | 领导-协作者 | Coordinator Pattern |
| 黑板模式 | 共享状态 | Shared Memory + 锁机制 |

```python
class MultiAgentCoordinator:
    """多 Agent 协作协调器"""

    def __init__(self):
        self.agents = {}           # Agent 注册表
        self.shared_workspace = SharedFileSystem()  # 共享文件系统
        self.message_queue = MessageQueue()         # 消息队列

    async def broadcast(self, sender: str, task: str, target_agents: list):
        """向指定 Agent 列表广播任务"""
        for agent_id in target_agents:
            await self.message_queue.send(agent_id, {
                "from": sender,
                "type": "task",
                "payload": task,
                "context": self.shared_workspace.snapshot()
            })

    async def collect(self, task_id: str, expected: int, timeout: int = 60):
        """收集所有 Agent 的结果"""
        results = []
        async for result in self.message_queue.collect(task_id, timeout):
            results.append(result)
            if len(results) >= expected:
                break
        return results
```

### 5.4 设计一个 Harness 的可观测性体系
```yaml
# OpenTelemetry 驱动的可观测性配置
observability:
  traces:
    exporter: otlp
    sampling: 0.1  # 10% 采样率（高并发环境）
    attributes:
      - harness.component  # 组件名（sandbox/fs/memory/…）
      - agent.id           # Agent 实例 ID
      - session.id         # 会话 ID

  metrics:
    - name: harness.execution.duration
      type: histogram
      buckets: [0.1, 0.5, 1, 2, 5, 10, 30, 60]
    - name: harness.tool.invocations
      type: counter
      labels: [tool_name, status]
    - name: harness.sandbox.utilization
      type: gauge

  logging:
    level: info
    structured: true
    fields:
      - timestamp
      - level
      - agent_id
      - component
      - message
```

**面试加分点**：提到 OpenTelemetry、自定义 Metrics、链路追踪、基于采样的 Trace 采集策略。

---

## 六、常见坑点与最佳实践

### 6.1 常见坑点对照表

| 坑点 | 现象 | 原因 | 最佳实践 |
|------|------|------|---------|
| **上下文溢出** | Agent 突然"失忆"，行为异常 | 上下文窗口超限，早期内容被丢弃 | 压缩 + 滑动窗口 + 自动摘要 |
| **工具调用风暴** | Token 迅速耗尽，Agent 陷入循环 | 工具返回结果被原样追加到上下文 | 限制调用次数 + 结果截断 + 缓存 |
| **沙箱逃逸** | Agent 访问了主机系统 | 沙箱配置过于宽松 | 默认拒绝策略 + seccomp + 只读根文件系统 |
| **状态泄漏** | Agent A 看到 Agent B 的数据 | 文件系统/内存隔离不彻底 | 租户级 Namespace + 加密 |
| **无限循环** | Agent 反复执行同一操作 | 缺乏步数限制和去重机制 | 最大步数限制 + 操作哈希去重 |
| **幻觉累积** | Agent 基于错误假设继续推理 | 中间错误被当作事实传入后续步骤 | 每一步结果独立校验 + 置信度评分 |
| **冷启动慢** | 首次请求延迟过高 | Sandbox 镜像拉取、模型加载耗时长 | Sandbox 预热池 + 模型缓存 |
| **资源饥饿** | 某个 Agent 占用全部资源 | 缺乏配额管理和公平调度 | CGroup 配额 + 加权公平队列 |

### 6.2 Harness 设计最佳实践

1. **默认拒绝（Default-Deny）**：Sandbox 网络、文件访问默认拒绝，按需开放
2. **无状态设计**：Harness 核心组件无状态，方便水平扩展
3. **优雅降级（Graceful Degradation）**：工具失败时提供合理 Fallback
4. **可观测性优先（Observability-First）**：上线前接入全链路追踪
5. **渐进式安全**：开发环境宽松、预发布中等、生产环境严格
6. **熔断机制（Circuit Breaker）**：工具连续失败 N 次后自动熔断
7. **幂等性设计**：同一任务多次执行结果一致

---

## 七、面试回答模板（Top 5）

### 7.1 "请解释一下什么是 Agent Harness"
> **T = 技术定义 + A = 三层进化 + R = 六大组件 + A = 行业趋势**

**T** — Agent Harness 是 AI Agent 的运行时基础设施层，提供文件系统、沙箱、工具集成等核心能力，让 LLM 从"聊天机器人"进化为可执行任务的"数字员工"。

**A** — 它代表了 AI 应用开发的第三层进化：第一层是 Prompt Engineering（怎么写提示词），第二层是 Context Engineering（给什么上下文），第三层就是 Harness Engineering（怎么让它稳定执行）。

**R** — 核心包括六大组件：FileSystem 持久化、Script Execution 执行能力、Sandbox 安全隔离、Tool Integration 外部连接、Memory & Search 记忆系统、Verification & Protection 防护体系。

**A** — 2026 年是 Harness Engineering 的爆发之年，OpenAI 的 Code Interpreter、Anthropic 的 Tool Use 本质上都是 Harness 的实践。行业正从 Model-Centric 转向 Infrastructure-Centric。

### 7.2 "Harness 和 Framework 有什么区别？"
> **T = 定位差异 + A = 关注点对比 + R = 举例佐证**

**T** — Framework（如 LangChain、AutoGen）是给**开发者**的**开发框架**，关注如何**编写** Agent；Harness 是给**Agent**的**运行环境**，关注如何**执行**任务。

**A** — Framework 提供 Chain、Agent、Tool 的编程抽象，但不管执行安全和生产运维；Harness 内置沙箱隔离、资源限制、审计日志、容错机制等生产级能力。

**R** — 拿一个具体例子：LangChain 的 Agent 可以直接 `exec()` 用户代码，这在开发环境没问题，但生产环境这就是巨大安全隐患。Harness 强制所有代码在 Sandbox 中执行，彻底杜绝了这个问题。

### 7.3 "如何防止 Agent 的 Context Drift？"
> **T = 定义问题 + A = 五层防护 + R = Goal Refocus 机制**

**T** — Context Drift 是 Agent 在执行长任务时，上下文逐渐偏离原始目标的现象，是生产环境 Agent 最主要的失败原因。

**A** — 我们构建了五层防护：第一层是输入层的 Compress & Summary，定期压缩上下文；第二层是决策层的 Goal Refocus，每 N 步回顾原始目标；第三层是执行层的中间结果 Schema 校验；第四层是输出层的一致性检查；第五层是系统层的窗口溢出预警。

**R** — 最核心的是 Goal Refocus 机制：我们维护原始目标的 Embedding，每 5 步计算当前上下文与目标的语义相似度，低于阈值时自动触发 Re-focus。这能把长任务的成功率提升约 40%。

### 7.4 "为什么需要 Sandbox？只做静态代码检查不够吗？"
> **T = 不够 + A = 静态检查的局限性 + R = 沙箱的必要性**

**T** — 静态代码检查（AST 扫描、危险函数检测等）是必要但不充分的。

**A** — 静态检查存在三个盲区：一是动态导入（`__import__("os")`）难以静态识别；二是运行时漏洞（如内存溢出、死循环）无法通过静态分析发现；三是行为边界问题（比如代码本身合法，但恶意利用了宿主环境资源）。

**R** — 沙箱提供的是执行时隔离：Docker 容器限制文件系统访问、CGroup 限制资源使用、seccomp 限制系统调用。像 Hermes Agent 就使用 gVisor 实现内核级隔离，是真正意义上的"安全执行"。

### 7.5 "设计一个生产级 Agent Harness 时最重要的是什么？"
> **T = 安全 + A = 可靠 + R = 可观测 = A = 架构权衡**

**T** — 第一是**安全**，没有安全的生产级系统毫无意义。默认拒绝、沙箱隔离、最小权限原则是铁律。

**A** — 第二是**可靠**，Graceful Degradation（优雅降级）、Circuit Breaker（熔断）、Retry with Backoff（指数退避重试），每个工具调用都要有容错设计。

**R** — 第三是**可观测性**，你没法 debug 一个你不理解的 Agent。全链路追踪、Metrics 监控、结构化日志，三者缺一不可。

**A** — 最后是架构层面的权衡：安全与性能的权衡（太严格的沙箱影响响应速度）、隔离与共享的权衡（完全隔离的 Sandbox 无法利用缓存）。一个好的架构师要找到这个平衡点。

---

## 八、快速查漏补缺 Checklist

### 核心概念
- [ ] 能一句话说清楚什么是 Agent Harness
- [ ] 能清晰解释 Prompt Engineering → Context Engineering → Harness Engineering 三层进化
- [ ] 能列出并解释 Harness 与 Framework 的 3 个以上核心区别
- [ ] 能说出三种主流 Harness 设计范式（Sandbox-First / Tool-First / Memory-First）
- [ ] 能解释 Model-Centric → Infrastructure-Centric 的行业转变

### 六大核心组件
- [ ] 理解 FileSystem 在 Agent 中的角色（持久化、快照、配额管理）
- [ ] 理解 Script Execution 的工作原理（生成 → 校验 → 执行 → 返回）
- [ ] 理解 Sandbox 的安全隔离机制（Docker/gVisor/CGroup/seccomp）
- [ ] 理解 Tool Integration 的注册/发现/调用模式
- [ ] 理解 Memory & Search 的层级设计（短期/长期/语义搜索）
- [ ] 理解 Context Engineering 的窗口管理和压缩策略

### Hermes Agent
- [ ] 知道 Hermes Agent 的核心功能
- [ ] 了解 Hermes Agent 的价格构成和使用场景
- [ ] 了解 Hermes Agent 的局限性
- [ ] 知道 Hermes Agent vs OpenClaw 的差异化对比

### Context Drift
- [ ] 能定义 Context Drift
- [ ] 能说出五层防护机制
- [ ] 能手写语义相似度检测代码
- [ ] 理解 Goal Refocus 机制

### 实战与代码
- [ ] 能实现一个简单的 Sandbox 执行器
- [ ] 能实现 Tool Registry 的注册和调用
- [ ] 能实现 Context Drift 检测器
- [ ] 能实现 Agent 主执行循环
- [ ] 能设计多 Agent 协作架构

### 系统设计
- [ ] 能画出企业级 Harness 平台架构图
- [ ] 能设计高可用 Sandbox 集群
- [ ] 能设计 Harness 可观测性体系
- [ ] 能应对"如何迁移现有 Framework 应用到 Harness"的问题

### 最佳实践
- [ ] 知道沙箱配置的 Default-Deny 原则
- [ ] 知道工具调用的 Graceful Degradation 策略
- [ ] 知道 Context 压缩的滑动窗口策略
- [ ] 知道幂等性设计的重要性
- [ ] 知道熔断机制在工具调用中的应用

### Hugging Face 补充知识
- [ ] 了解 Transformers 库的基本使用
- [ ] 了解 datasets 库的数据加载流程
- [ ] 了解 Tokenizers 的分词原理
- [ ] 理解模型微调的基本概念
- [ ] 了解超长文本训练存在的问题

---

> 🎯 **面试总结**：Agent Harness 面试的核心在于展现**系统思维**——不是零散地背组件名，而是能够从架构视角阐述 LLM 的缺陷、Harness 如何弥补这些缺陷、以及如何构建安全可靠的 Agent 生产系统。2026 年 Harness Engineering 是 AI 领域最热的方向之一，扎实的理解会让你在面试中脱颖而出。
