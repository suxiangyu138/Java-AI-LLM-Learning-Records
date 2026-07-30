# 01 - Agent Skills 核心概念

> 🎯 Agent Skill 是 Agent 的"可复用能力单元" — 将工具调用、领域知识、执行流程封装为一个标准化的技能模块。理解 Skill 的本质是构建高效 Agent 系统的第一步

---

## 目录

1. [什么是 Agent Skill](#1-什么是-agent-skill)
2. [Skill 的核心三要素](#2-skill-的核心三要素)
3. [Skill vs Tool vs Function vs Plugin](#3-skill-vs-tool-vs-function-vs-plugin)
4. [Skill 的五个维度](#4-skill-的五个维度)
5. [Agent Skills 的设计哲学](#5-agent-skills-的设计哲学)

---

## 1. 什么是 Agent Skill

```text
Agent Skill（智能体技能）= 工具调用 + 领域知识 + 执行流程

类比理解：
  Tool（工具）  = 一把锤子          → 只知道"怎么锤"
  Skill（技能） = 木工技能          → 知道"何时用锤子 + 用什么钉子 + 怎么量尺寸 + 怎么拼接"
  Agent（智能体）= 会木工的机器人    → 有 Skill 的 Agent

核心区别：
  Tool：单点能力——"搜索一下"、"发送HTTP请求"、"读取文件"
  Skill：复合能力——"调研一个技术方案"（含搜索→阅读→对比→总结→输出报告）
```

### 1.1 Skill 的演进历程

```text
阶段一：Hard-coded Rules（2015 前）
  └── if-else 规则引擎，技能完全人工编码

阶段二：LLM + Tool Use（2023）
  └── ChatGPT Plugins、Function Calling：LLM 选择调用哪个工具
      问题：工具粒度太细，LLM 需要反复"试错"组合工具

阶段三：Skill 封装（2024）
  └── 将常用工具组合封装为 Skill，减少 LLM 决策负担
      代表：LangChain Agent、Semantic Kernel Plugin

阶段四：Skill 自治（2025+）
  └── Skill 自带知识库、错误恢复、自适应策略
      代表：Claude Code Skill System、MCP Tool → Skill 模式
```

---

## 2. Skill 的核心三要素

```text
┌──────────────────────────────────────────────────────────┐
│                     Agent Skill                           │
├────────────────────┬─────────────────┬───────────────────┤
│    🛠️ 工具映射     │   📚 领域知识    │   🔄 执行流程      │
│   (Tools)          │   (Knowledge)    │   (Workflow)      │
├────────────────────┼─────────────────┼───────────────────┤
│ • 底层 API/函数    │ • 使用场景判定   │ • 步骤序列        │
│ • 外部服务调用     │ • 前置条件       │ • 条件分支        │
│ • 数据库操作       │ • 参数约束       │ • 错误恢复        │
│ • 文件系统操作     │ • 输出规范       │ • 结果验证        │
│ • 第三方集成       │ • 最佳实践       │ • 进度反馈        │
└────────────────────┴─────────────────┴───────────────────┘

三者缺一不可：
  只有工具映射 → Tool（单点功能）
  工具 + 知识   → Smart Tool（知道怎么用的工具）
  工具 + 知识 + 流程 → Skill ✅（完整的能力单元）
```

### 2.1 工具映射 (Tool Mapping)

```java
// Skill 内部封装的工具集合
public class CodeReviewSkill {
    // 底层工具
    private final Tool readFile;      // 读取源代码
    private final Tool runLinter;     // 运行 Lint 检查
    private final Tool gitDiff;       // 获取变更差异
    private final Tool runTests;      // 运行单元测试

    // Skill 暴露给 Agent 的接口
    public ReviewResult review(String filePath) { ... }
}
```

### 2.2 领域知识 (Domain Knowledge)

```yaml
# Skill 内置的领域知识
skill:
  name: "code_review"
  description: "对代码变更进行系统化审查"
  when_to_use:
    - "用户提交了 PR 或代码变更"
    - "需要检查代码质量、安全、性能"
  when_not_to_use:
    - "仅需格式化代码（用 formatter skill）"
    - "仅需运行测试（用 test_runner skill）"
  preconditions:
    - "目标文件存在"
    - "有 git diff 或变更文件列表"
  expected_output:
    format: "结构化 JSON"
    fields: [severity, file, line, issue, suggestion]
```

### 2.3 执行流程 (Execution Flow)

```text
CodeReviewSkill 执行流程：
  Step 1: 获取变更文件列表（gitDiff）
  Step 2: 对每个文件并行执行：
    2a: 读取文件内容（readFile）
    2b: 运行 Lint 检查（runLinter）
    2c: 运行相关测试（runTests）
  Step 3: 汇总结果 → 结构化输出
  Step 4: 若某步骤失败 → 重试或降级
```

---

## 3. Skill vs Tool vs Function vs Plugin

| 维度 | **Function** | **Tool** | **Skill** | **Plugin** |
|------|:---:|:---:|:---:|:---:|
| **粒度** | 最细 | 细 | 中 | 大 |
| **组成** | 1 个函数 | 1-N 个函数 | N 个 Tool + 知识 + 流程 | N 个 Skill + 配置 |
| **知识内置** | ❌ | ❌ | ✅ | ✅ |
| **流程内置** | ❌ | ❌ | ✅ | ✅（可选） |
| **可组合** | ✅ | ✅ | ✅ | ✅ |
| **独立部署** | ❌ | ❌ | ❌ | ✅ |
| **代表** | `getWeather()` | OpenAI Tool | CodeReviewSkill | ChatGPT Plugin |
| **Agent 视角** | "调用一个函数" | "使用一个工具" | "执行一项技能" | "安装一个扩展" |

```text
层次关系：

  Plugin（插件层）
  ├── Skill A（技能层）
  │   ├── Tool 1（工具层）
  │   │   ├── Function a（函数层）
  │   │   └── Function b
  │   └── Tool 2
  ├── Skill B
  │   ├── Tool 3
  │   └── Tool 4
  └── 配置 + 元数据

实际例子 — Claude Code：
  Plugin：Claude Code 本身
  Skill：/review-pr（代码审查技能）
  Tool：Read、Grep、Bash、Edit
  Function：readFile()、grep()、bash()
```

> 🎯 **核心要点**：Skill 是 Agent 能力封装的最佳粒度 — 不细到让 Agent 迷失方向，不粗到丧失灵活性

---

## 4. Skill 的五个维度

### 4.1 能力维度 (Capability)

```text
Skill 能做什么？

① 数据获取型：搜索、查询DB、爬取网页
② 数据转换型：格式转换、翻译、摘要
③ 分析推理型：代码审查、安全分析、数据洞察
④ 执行操作型：发送邮件、部署服务、创建文件
⑤ 交互协调型：多轮对话、任务分配、进度跟踪
```

### 4.2 自主维度 (Autonomy)

| 级别 | 描述 | 示例 |
|:---:|------|------|
| **L0** | 完全人工指定步骤 | 传统脚本 |
| **L1** | LLM 选择参数，流程固定 | "搜索 {query} 并总结" |
| **L2** | LLM 选择子步骤顺序 | "分析代码" — 先 Lint 还是先 Review |
| **L3** | LLM 动态组合子 Skill | "搭建一个博客系统" → 自主选择技术栈 |
| **L4** | Skill 自我进化 | 根据使用反馈优化自身流程 |

### 4.3 状态维度 (Statefulness)

```text
无状态 Skill：每次调用独立，不依赖历史
  → 翻译、天气查询、代码格式化

有状态 Skill：依赖上下文或会话状态
  → 对话管理、多轮调研、渐进式代码重构
```

### 4.4 响应维度 (Response Mode)

```text
同步 Skill：调用 → 等待 → 返回结果
  → 简单查询、计算

异步 Skill：调用 → 立即返回任务ID → 轮询结果
  → 部署、训练模型、大批量处理

流式 Skill：调用 → 持续输出结果
  → 代码生成、长文本生成、实时分析
```

### 4.5 可见维度 (Visibility)

```text
黑盒 Skill：Agent 只知道输入输出，不知道内部实现
  → 第三方 API Skill、外部服务封装

白盒 Skill：Agent 可以看到并影响内部流程
  → 自定义 Skill、开源 Skill

灰盒 Skill：Agent 知道流程骨架，但部分步骤不透明
  → 企业 Skill 模板
```

---

## 5. Agent Skills 的设计哲学

### 5.1 单一职责原则

```text
✅ 好的设计：
  "CodeReviewSkill" — 只做代码审查
  "TestRunnerSkill" — 只做测试执行
  "DeploySkill"    — 只做部署

❌ 坏的设计：
  "DevOpsSkill" — 审查+测试+部署+监控 一把抓
  → Agent 无法灵活编排，Skill 内部耦合严重
```

### 5.2 声明式优于命令式

```yaml
# ✅ 声明式：描述 Skill 的能力和约束
skill:
  name: web_search
  description: "搜索互联网获取最新信息"
  input: { query: string, max_results: int }
  output: { results: [{ title, url, snippet }] }
  constraints: { rate_limit: "10/min", timeout: "5s" }

# ❌ 命令式：暴露内部实现细节
skill:
  name: web_search
  steps:
    - call_bing_api
    - parse_html
    - filter_results
    - sort_by_relevance
  # Agent 不需要关心这些
```

### 5.3 可发现性优先

```text
Skill 必须能让 LLM 在众多选项中正确选择：
  1. 精确的 description（描述 Skill 做什么、何时用）
  2. 清晰的 when_to_use / when_not_to_use
  3. 明确的输入输出 Schema
  4. 可检索的标签/分类

描述质量直接决定 Agent 的 Skill 选择准确率：
  模糊描述 → LLM 选错 Skill → 任务失败
  精确描述 → LLM 选对 Skill → 一次完成
```

### 5.4 容错内置

```text
Skill 自行处理常见错误，不让 LLM 猜测恢复策略：

  Skill 内置的容错层：
  ├── 参数校验：输入不合法 → 返回明确的错误描述
  ├── 超时重试：网络抖动 → 自动重试 N 次
  ├── 降级策略：主服务不可用 → 切换到备用方案
  ├── 结果验证：输出不符合预期 → 自动修正或报错
  └── 状态回滚：多步骤中途失败 → 回滚已完成步骤
```

> 🎯 **核心要点**：好的 Skill 设计 = 精确的描述 + 单一职责 + 内置容错 + 可组合接口

---

**下一模块**：[02 - Agent Skills 类型与设计模式](./02-Agent%20Skills类型与设计模式.md)  
**返回总览**：[00 - Agent Skills 知识体系总览](./00-Agent%20Skills知识体系总览.md)
