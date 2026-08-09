# 01 Claude Code 技能实践

> 标准原创者与功能最全的实现（GA）：自动触发 + 斜杠调用 + 子代理 + 生命周期钩子，独有 allowed-tools/disable-model-invocation——"Claude Code 能跑的，通常其他工具也能跑"。

## 📚 目录

1. [Claude Code 的支持状态](#1-claude-code-的支持状态)
2. [技能目录与调用方式](#2-技能目录与调用方式)
3. [独有能力](#3-独有能力)
4. [子代理与上下文](#4-子代理与上下文)
5. [与 MCP 的组合](#5-与-mcp-的组合)
6. [成本与注意事项](#6-成本与注意事项)
7. [面试高频问法](#7-面试高频问法)

## 1. Claude Code 的支持状态

| 维度 | 状态 |
|---|---|
| 版本状态 | GA（正式版） |
| 技能目录 | .claude/skills/ |
| 定位 | Agent Skills 标准原创者 |
| 功能完整度 | 最全（其他工具的参考实现） |
| 性能基准 | SWE-bench Verified 80.9%（第一） |

```
Claude Code 是标准的事实参考实现：
功能最全 → 其他工具支持的功能它基本都有
"Claude Code 能跑的，通常其他也能跑"
——跨平台测试的主平台
```

## 2. 技能目录与调用方式

### 目录

```
.claude/skills/（项目级）
~/.claude/skills/（用户级）
插件级（随插件分发）
```

### 两种调用

| 方式 | 用法 | 场景 |
|---|---|---|
| 自动触发 | 说触发词（description 匹配） | 日常 |
| 斜杠调用 | /skill-name | 明确指定 |
| 自定义斜杠 | /commandname（命令） | 固定流程 |

### 查看与排查

```
/skills 菜单（可视化 + 启用开关）
"What skills are available?"（对话询问）
/doctor（描述预算检查）
（阶段 2 的 01 篇实操）
```

## 3. 独有能力

| 能力 | 说明 | 跨平台 |
|---|---|---|
| allowed-tools | 工具沙箱（白名单） | 仅部分支持 |
| disable-model-invocation | 禁自动触发（只手动） | 仅 Claude |
| 生命周期钩子 | PreToolUse/PostToolUse 等 | 仅 Claude |
| 子代理执行 | skill 内调用其他 agent | 部分 |
| 动态上下文注入 | 嵌入 shell 命令输出 | 仅 Claude |
| Extended Thinking | ultrathink 深度推理 | 仅 Claude |
| TodoWrite | 任务清单 | 仅 Claude |

### 独有能力的意义

```
① 完整能力在 Claude Code（标准作者）
② 跨平台时独有字段静默降级（05 篇处理）
③ 学习/测试以 Claude Code 为主（功能最全）
```

### 示例：禁自动触发的部署技能

```yaml
---
name: deploy
description: 部署流程。用于用户要求部署/上线时。
disable-model-invocation: true   # 只手动（防误触发）
---
```

## 4. 子代理与上下文

### 子代理执行

```
skill 内可调用其他 agent（子代理）：
context: fork（隔离子上下文运行）
agent: Explore/Plan/general-purpose（指定子代理类型）
```

```yaml
---
name: deep-research
description: 深度调研任务。
context: fork          # 隔离子代理上下文
agent: Explore          # 用只读调研子代理
---
```

### 子代理的价值

```
① 隔离：子代理上下文不污染主会话
② 沙箱：验证/搜集安全执行
③ 分工：调研/规划/实现分离
```

### 动态上下文注入

```
嵌入 shell 命令输出（加载时）：
`!git log --oneline -5`
——技能获得实时上下文（阶段 2 的 07 篇）
```

## 5. 与 MCP 的组合

### 组合模式

```
技能（知识：怎么用）+ MCP 工具（能力：能做什么）：
技能正文指导"调用哪个 MCP 工具、按什么顺序"
```

### Claude Code 的组合优势

```
① MCP 生态最成熟（服务器最多）
② 技能引用 MCP 工具（原生配合）
③ allowed-tools 可限定 MCP 工具范围
——组合的参考实现
```

### 示例

```markdown
## 执行步骤
1. 调用 GitHub MCP 工具获取 PR 信息
2. 按审查规则分析
3. 输出问题清单
```

## 6. 成本与注意事项

### 常见误区

| 误区 | 真相 |
|---|---|
| "Claude 专属扩展跨平台可用" | 会静默降级（allowed-tools 等） |
| "技能越多越好" | 描述预算有限（/doctor 检查） |
| "自动触发永远对" | 有副作用技能要 disable-model-invocation |
| "子代理随便用" | context: fork 有开销（按需） |
| "API 成本不用管" | 按量计费（token 管理） |

### 成本

```
按 API 用量计费（或 $20/月订阅）
多技能/多轮 → token 成本需管理
（对比：Gemini CLI 有免费档）
```

### 注意事项

| 注意 | 说明 |
|---|---|
| 专属扩展不可移植 | allowed-tools 等跨工具降级 |
| API 成本 | 管理 token（技能加载与调用） |
| 技能数量 | 描述预算（/doctor 检查） |
| 实验功能 | 部分能力仍在演进 |

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| Claude Code 技能状态？ | GA、功能最全、标准原创者 |
| 独有能力？ | allowed-tools/钩子/子代理/动态注入 |
| 调用方式？ | 自动触发/斜杠/自定义命令 |
| 为什么以它为主测试？ | 功能最全（其他工具是子集） |
| 与 MCP 组合？ | 技能指导 + MCP 执行（原生配合） |
| 成本注意？ | API 按量（token 管理） |

### 面试加分表达

> "Claude Code 是 Agent Skills 的事实参考实现（GA、功能最全）：allowed-tools 工具沙箱、disable-model-invocation 手动触发、生命周期钩子、子代理执行（context: fork）都是独有或最完善的。跨平台测试以它为主——'Claude Code 能跑的其他通常也能跑'，但注意专属字段在别处会静默降级。与 MCP 组合也是它的原生优势。"

> 🎯 核心要点：Claude Code = 标准参考实现（GA，SWE-bench 80.9% 第一）；目录 .claude/skills/；独有能力（allowed-tools/钩子/子代理/动态注入/ultrathink）；子代理（context: fork + agent 指定）；与 MCP 原生组合；跨平台注意（专属字段降级 + API 成本管理）。

---

**下一模块**：[02-Codex-CLI与OpenAI生态](02-Codex-CLI与OpenAI生态.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
