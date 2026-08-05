# 03 - Agent 实现原理与 ReAct 循环

> 🎯 OpenClaw Agent = ReAct 循环 + 工具调用 + Skills 注入 + Memory 管理 + 主子 Agent 协作。本章覆盖从消息入站到回复发出的完整执行链路

---

## 目录

1. [Agent Loop 异步流水线](#1-agent-loop-异步流水线)
2. [ReAct 循环内部分解](#2-react-循环内部分解)
3. [Hook 生命周期扩展点](#3-hook-生命周期扩展点)
4. [Memory 四层记忆架构](#4-memory-四层记忆架构)
5. [主子 Agent 并行架构](#5-主子-agent-并行架构)
6. [容错与回退机制](#6-容错与回退机制)

---

## 1. Agent Loop 异步流水线

```text
消息入站（RPC 接收）：
  → 立即返回 { runId, acceptedAt }（不阻塞入站通道）
  → 真正执行在异步 agentCommand 中：

agentCommand 执行流程：
① 解析 model（用户指定 > Channel 默认 > 全局默认）
② 加载 Skills snapshot（当前可用 Skill 摘要注入 System Prompt）
③ 调用 runEmbeddedPiAgent() → 进入 ReAct 循环

并发控制：两级队列串行化
├── Per-session Queue：同一会话消息按序执行
└── Global Queue：跨 Session 排队（防资源争抢）
```

---

## 2. ReAct 循环内部分解

```text
核心源码位置：src/agents/pi-embedded-runner/run.ts

每次迭代（iteration）包含：
┌─────────────────────────────────────────────┐
│ ① 准备 Workspace                            │
│    检查工作目录、加载文件清单、注入上下文     │
├─────────────────────────────────────────────┤
│ ② 加载 Skill Entries                        │
│    扫描可用 SKILL.md → 生成 XML 摘要          │
│    注入 System Prompt（<available_skills>）  │
├─────────────────────────────────────────────┤
│ ③ 构建 System Prompt                        │
│    包含：行为规范 + Safety Rails + Tools Schema│
│        + Memory 摘要 + Skills + Workspace    │
├─────────────────────────────────────────────┤
│ ④ 创建工具集 → 调用 pi-coding-agent          │
│    Tools: shell/read/write/edit/browser/     │
│           cron/skills/memory/subagent...    │
├─────────────────────────────────────────────┤
│ ⑤ LLM 返回 → 检查需要执行工具？               │
│    ├─ 是 → 执行工具 → 结果注入上下文 → goto ②  │
│    └─ 否 → ⑥ 生成最终回复                     │
├─────────────────────────────────────────────┤
│ ⑦ 处理流式输出 / 上下文压缩 / NO_REPLY 过滤   │
└─────────────────────────────────────────────┘

循环上限：MAX_RUN_LOOP_ITERATIONS（随 Auth Profile 数量动态缩放）
```

### 关键设计细节

**NO_REPLY 约定：** 模型输出 `NO_REPLY` token → 框架过滤不下发。实现"群聊中不是每条消息都需要回复"的沉默语义。

**System Prompt 工程：** 所有行为规范全部写死在 Prompt 里（文本约束、非代码拦截），包括：
- Safety Rails：Prompt Injection 防御、Skill 投毒防御、敏感操作需确认
- Tool Call Style：工具调用格式和优先级
- URL 自动 Fallback 链：`skill → web_fetch → browser → 搜索 skills`（不询问用户）
- Runtime 元信息注入：当前时间、OS 类型、可用磁盘空间等

---

## 3. Hook 生命周期扩展点

```text
五个 Hook 扩展点（插件注册，无需 fork 核心代码）：

① before_model_resolve    → 模型解析前（可动态替换模型）
② before_prompt_build     → Prompt 构建前（可注入自定义上下文）
③ before_tool_call        → 工具调用前（可拦截/改写工具调用参数）
④ after_tool_call         → 工具调用后（可修改工具返回结果）
⑤ agent_end               → Agent 执行结束（清理/审计日志）
```

---

## 4. Memory 四层记忆架构

```text
四层记忆（由近到远）：

Layer 1：Session Context（JSONL）
  → 当前会话的完整对话流
  → 存储在 ~/.openclaw/sessions/<sessionKey>.jsonl

Layer 2：Daily Logs（日记忆）
  → 每天自动归档 → memory/YYYY-MM-DD.md
  → Agent 完成会话后自动写入关键信息

Layer 3：MEMORY.md（长期记忆）
  → 用户手动编辑或 Agent 自动总结的持久化记忆
  → 只在主 Session 加载（群聊不加载，防隐私泄露）

Layer 4：Vector Search（语义检索）
  → 历史数据向量化 → 相似性检索
  → 用于补全遥远的记忆碎片

上下文压缩：
  接近上限时 → 先跑静默 memory flush turn → 再 compaction（最多 3 次）
```

```markdown
# MEMORY.md 示例
---
name: user-preferences
description: 用户的编码偏好和常用工具
---

## 代码风格
- 使用 2 空格缩进
- 函数命名用 camelCase
- 偏好 TypeScript 而非 JavaScript

## 常用工具
- IDE: VS Code
- 终端: iTerm2 + zsh
- 笔记: Obsidian
```

---

## 5. 主子 Agent 并行架构

```text
主 Agent 通过 sessions_spawn 工具创建子 Agent：

特性：
├── 并行执行：同时 spawn 多个子 Agent
├── 上下文隔离：每个子 Agent 独立 Context
├── 独立模型配置：子 Agent 可用不同于主 Agent 的模型
├── 工具权限两级拒绝：
│   ├── DENY_ALWAYS：敏感工具全局禁止
│   └── DENY_LEAF：子 Agent 不可再 spawn 孙子 Agent
├── 推送式结果：子 Agent 完成后主动推送给主 Agent
└── steer 操作：主 Agent 可中断并重新指挥偏离方向的子 Agent

subagents 工具：
  ├── list：列出所有活跃子 Agent
  ├── kill：终止指定子 Agent
  └── steer：中断+重新指挥子 Agent
```

---

## 6. 容错与回退机制

| 机制 | 说明 |
|------|------|
| **错误分类** | rate_limit / overloaded / auth / billing / timeout 标准化分类 |
| **Auth Profile 熔断轮换** | 一个模型故障 → 自动切换到备用 Auth Profile |
| **模型回退链** | Claude → GPT → DeepSeek → 本地 Ollama（跨提供商降级） |
| **指数退避+抖动** | 网络错误重试时加随机抖动防惊群 |
| **上下文压缩** | 接近 Token 上限时自动压缩（最多 3 次） |

---

> 🎯 **核心要点**：Agent 的三个核心机制 — **① ReAct 循环（Reasoning→Acting→Observation，最多 N 次迭代）② Hook 扩展点（5 个生命周期钩子，插件式注入）③ Memory 四层架构（Session→Daily→MEMORY.md→Vector）**。关键设计：NO_REPLY 实现沉默语义、Prompt 文本约束替代代码拦截、主子 Agent 并行 + 权限隔离。

**下一模块**：[04-Skills技能系统与开发](04-Skills技能系统与开发.md) / **返回总览**：[00-总览](00-OpenClaw知识体系总览.md)
