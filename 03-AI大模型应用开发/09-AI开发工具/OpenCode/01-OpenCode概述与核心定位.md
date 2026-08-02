# 01 - OpenCode 概述与核心定位

> 🎯 OpenCode 不是聊天窗口 — 是能读取仓库、执行命令、编辑文件、调用任意模型的自主编码 Agent。开源免费、终端原生、模型中立，是 2026 年"供应商锁定反抗"的旗手

---

## 目录

1. [OpenCode 是什么](#1-OpenCode-是什么)
2. [三种使用形态](#2-三种使用形态)
3. [Plan/Build 双模式](#3-planbuild-双模式)
4. [与同类工具全景对比](#4-与同类工具全景对比)
5. [典型适用场景](#5-典型适用场景)

---

## 1. OpenCode 是什么

| 维度 | 详情 |
|------|------|
| 全称 | OpenCode |
| 开发者 | SST（Serverless Stack）团队 → 现由 **Anomaly** 维护 |
| 许可证 | **MIT 开源** |
| GitHub | **18 万+ Star**（2026.07，星标最高的开源 AI 编程 Agent） |
| 月活 | 约 800 万 |
| 技术栈 | Go + Bubble Tea（TUI）/ TypeScript |
| 定位 | **自主编码 Agent**（非聊天、非补全工具） |

```text
OpenCode 是：
  ✅ 能读整个项目仓库的 AI 编程代理
  ✅ 能执行 bash 命令、编辑文件、跑测试的自主 Agent
  ✅ 模型中立（75+ 模型，自带 API Key，工具不抽成）
  ✅ 本地优先（代码和会话全部本地存储）
  ✅ 免费开源（仅付模型 API 费用）

OpenCode 不是：
  ❌ Copilot 式行内自动补全（没有 Tab 补全）
  ❌ 一个聊天窗口（是执行引擎）
  ❌ 绑定某个模型的闭源工具
```

**爆火背景：** 2026.01 Anthropic 封禁第三方工具通过 Claude 订阅账号调用 Claude Code 服务 → 开发者"供应商锁定"集体反弹 → OpenCode 作为模型无关替代快速走红。

---

## 2. 三种使用形态

| 形态 | 命令 | 适用场景 |
|------|------|----------|
| **TUI**（终端界面） | `opencode` | 日常开发、交互式对话、Plan/Build 切换 |
| **Headless CLI** | `opencode run "<prompt>"` | CI/CD、脚本、一次性任务、批量处理 |
| **Server 服务** | `opencode serve` / `opencode web` | 无头 API、Web UI、远程连接、团队协作 |

```bash
# 三种形态的典型用法
opencode                                    # ① 进入 TUI
opencode run "修复 README 中的拼写错误"      # ② 一次性任务
opencode serve --port 4096 --hostname 0.0.0.0   # ③ 服务模式
opencode web --port 4096                    # ③ Web UI
```

---

## 3. Plan/Build 双模式

**OpenCode 最具代表性的设计 — 用 Tab 键一键切换：**

```text
Plan 模式（只读分析）：
├── edit 工具 → 默认 ask（只提示不修改）
├── bash 工具 → 默认 ask（需要确认）
└── 用途：读代码、设计方案、审查 diff、规划重构步骤

Build 模式（执行修改）：
├── edit 工具 → 完整权限（直接修改文件）
├── bash 工具 → 完整权限（直接执行命令）
└── 用途：实现功能、重构、跑测试、提交代码

推荐工作流：
  ① Plan 模式描述需求 → 迭代方案 → 确认实现计划
  ② Tab 切到 Build 模式 → 按计划执行
  ③ 审查 diff → 跑测试 → 完成
```

> 💡 社区测试：**先 Plan 后 Build** 让复杂重构的一次性通过率提升约 **40%**。

---

## 4. 与同类工具全景对比

| 对比 | OpenCode | Claude Code | Codex CLI | Cursor |
|------|----------|-------------|-----------|--------|
| 开源 | ✅ MIT | ❌ | ✅ | ❌ |
| 模型 | **75+ 自由切换** | 仅 Claude | 以 GPT 为主 | 多模型 |
| 供应商锁定 | 无 | **绑定 Anthropic** | 偏 OpenAI | 偏中立 |
| 界面 | TUI/Desktop/IDE | 纯 CLI | CLI | **AI-Native IDE** |
| 行内补全 | ❌ | ❌ | ❌ | ✅ |
| Plan/Build | ✅ Tab 切换 | plan mode | — | Agent Mode |
| 本地存储 | ✅ SQLite | 混合 | ✅ | 云端+本地 |
| 价格 | 免费 + BYOK | Pro $20/月 | Copilot 订阅 | $20/月起 |
| 定位 | **开源中立终端 Agent** | 最强推理终端 Agent | OpenAI 生态 | 全能 IDE |

---

## 5. 典型适用场景

| 场景 | 说明 | 示例 |
|------|------|------|
| **快速调试修复** | 报错信息 → 自主定位 → 修复 | "修复这个编译错误" |
| **大规模代码重构** | Plan 先规划 → Build 执行 | "把项目从 JS 迁移到 TypeScript" |
| **陌生代码库上手** | `/init` 生成项目上下文 | 快速理解遗留项目结构 |
| **隐私敏感项目** | 本地优先 + 离线可用 | 银行/政府/内网项目 |
| **CI/CD 自动化** | Headless 模式嵌入流水线 | GitHub Actions 自动 Review PR |
| **多会话并行** | 同一项目多个独立 AI 会话 | 同时处理多个任务 |

---

> 🎯 **核心要点**：OpenCode 的定位一句话 — **"开源、中立、本地优先的终端 AI 编程 Agent"**。三大差异化：75+ 模型自由切换（BYOK 不抽成）、Plan/Build 双模式（Tab 切换）、三种形态覆盖（TUI/Headless/Server）。与 Claude Code 的核心区别：**开源 vs 闭源、模型中立 vs 绑定 Anthropic**。

**下一模块**：[02-安装与快速上手](02-安装与快速上手.md) / **返回总览**：[00-总览](00-OpenCode知识体系总览.md)
