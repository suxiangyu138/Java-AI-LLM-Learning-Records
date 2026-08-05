# 00 - OpenCode 知识体系总览

> 🎯 OpenCode 是 GitHub 上星标最高的开源 AI 编程 Agent（18 万+ Star，MIT 协议）— 模型中立、终端原生、Plan/Build 双模式。2026.01 Anthropic 封禁第三方工具后爆发式走红，成为"供应商锁定"反抗运动的旗手

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [2026 发展里程碑](#3-2026-发展里程碑)
4. [与 Claude Code 的定位差异](#4-与-claude-code-的定位差异)
5. [学习路线推荐](#5-学习路线推荐)
6. [核心概念速查](#6-核心概念速查)

---

## 1. 知识全景

```
OpenCode 知识体系（8个文件 — 概述→安装→核心→配置→扩展→CI→运维）
│
├── 🏗️ 概述（01）
│   └── 01-OpenCode概述与核心定位.md      # 是什么/三大形态/Plan-Build双模式/生态对比
│
├── 🚀 上手（02）
│   └── 02-安装与快速上手.md              # 安装方式/登录认证/TUI操作/基础命令
│
├── 🎯 核心（03）
│   └── 03-核心能力与工具链.md            # 工具集/会话管理/多Agent/存储/模型切换
│
├── ⚙️ 配置（04）
│   └── 04-配置体系与AGENTS.md.md         # opencode.json/AGENTS.md/模型配置/全局vs项目
│
├── 🔌 扩展（05）
│   └── 05-五大扩展点详解.md              # Agents/Skills/Plugins/MCP/Commands
│
├── 🤖 CI（06）
│   └── 06-Headless模式与CI集成.md        # opencode run/无头服务器/GitHub Agent/CI安全
│
├── 🔒 运维（07）
│   └── 07-生产实践与安全.md              # 模型选型/成本控制/安全实践/踩坑清单
│
└── 📌 00-OpenCode知识体系总览.md          # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景导航 + 里程碑 + 学习路线 + 速查 | — |
| 01 | 概述与核心定位 | 三大形态/Plan-Build/与同类工具对比 | ⭐⭐ |
| 02 | 安装与快速上手 | 安装/认证/TUI 操作/快捷键 | ⭐⭐ |
| 03 | 核心能力与工具链 | 内置工具/会话管理/多 Agent/存储 | ⭐⭐⭐ |
| 04 | 配置体系与 AGENTS.md | opencode.json/AGENTS.md/模型配置 | ⭐⭐⭐ |
| 05 | 五大扩展点详解 | Agents/Skills/Plugins/MCP/Commands | ⭐⭐⭐ |
| 06 | Headless 与 CI 集成 | run 命令/无头服务器/GitHub Agent/安全 | ⭐⭐⭐ |
| 07 | 生产实践与安全 | 模型选型/成本/安全/坑点 | ⭐⭐ |

---

## 3. 2026 发展里程碑

| 时间 | 事件 |
|------|------|
| 2025 年中 | SST 团队创建 OpenCode |
| 2026.01 | **Anthropic 封禁第三方工具** → OpenCode 作为模型无关替代爆发走红 |
| 2026.06 | Gemini CLI 退役、Roo Code 关停 → 行业整合，OpenCode 逆势增长 |
| 2026.07 | GitHub 星标突破 **18 万+**（星标最高的开源 AI 编程 Agent） |
| 2026.07 | 月活跃用户约 800 万 |
| 2026 持续 | 从 SST 团队移交 Anomaly 维护；推出 Zen 按量付费与 Go 订阅 $10/月 |

---

## 4. 与 Claude Code 的定位差异

| 对比 | OpenCode | Claude Code |
|------|----------|-------------|
| 开源 | ✅ MIT | ❌ 闭源商业 |
| 模型 | **75+ 模型自由切换**（含本地） | 仅 Claude 系列 |
| 供应商锁定 | ❌ 无 | ✅ 绑定 Anthropic |
| 价格 | 工具免费 + 自带 API Key | Pro $20/月 或 API 计费 |
| 界面 | TUI + Desktop + IDE 扩展 | 纯 CLI |
| 隐私 | **本地存储**（SQLite） | 云端/本地混合 |
| Plan/Build | ✅ 双模式 Tab 切换 | 通过 plan mode 实现 |
| 触发背景 | 2026.01 封禁事件受益者 | 封禁事件发起方 |

---

## 5. 学习路线推荐

### 🟢 快速上手（1-2 小时）
```
01-概述 → 02-安装 → opencode 进入 TUI → 跑通第一个任务
产出：终端里用 OpenCode 完成一次代码修改
```

### 🔵 深度使用（半天）
```
03-核心能力 → 04-配置（AGENTS.md）→ 05-扩展点
产出：配置好项目规则，能写自定义 Agent/Skill
```

### 🔴 生产落地（1 天）
```
06-Headless CI → 07-生产实践与安全
产出：CI 流水线里跑自动化 Review + 团队统一配置
```

---

## 6. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| TUI | 基于 Bubble Tea 的终端沉浸式界面（默认） |
| Plan/Build 双模式 | Plan 只读分析、Build 执行修改，Tab 一键切换 |
| AGENTS.md | 项目行为准则（house rules），每次会话自动读取 |
| Agent | 定义角色+权限的代理（plan/build + 自定义） |
| Skill | `.opencode/skills/<name>/SKILL.md` 知识包，模型按描述自动发现 |
| Plugin | `.opencode/plugins/*.{js,ts}` 生命周期钩子插件 |
| MCP | 连接外部工具的协议（local/remote 两种配置） |
| Headless | `opencode run` 无头模式，适合 CI/脚本 |
| BYOK | Bring Your Own Key — 自带 API Key，工具不抽成 |

---

> 🎯 **核心要点**：OpenCode 的三大差异化 — **① 模型中立（75+ 模型自由切换、本地可跑）② Plan/Build 双模式（先规划后执行，复杂重构通过率提升约 40%）③ 本地优先（代码和会话全部本地 SQLite 存储）**。2026 年的定位：Claude Code 的开源替代 + 供应商锁定的反抗旗手。

**下一模块**：[01-OpenCode概述与核心定位](01-OpenCode概述与核心定位.md)
