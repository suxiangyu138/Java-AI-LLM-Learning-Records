# 00 - Claude Code 知识体系总览

> 🎯 Claude Code 是 Anthropic 推出的终端级 AI 编程 Agent — 可以自主编辑文件、执行命令、操作 Git，是 Cursor/Copilot 之外最强大的 AI 编码工具

---

## 1. 知识全景

```
Claude Code 体系（11个文件）
│
├── 🏗️ 入门篇（01-03）
│   ├── 01-快速入门.md              # 5分钟上手/核心命令/使用场景
│   ├── 02-核心概念与架构.md         # Agent循环/上下文管理/权限模型
│   └── 03-安装与配置详解.md         # 安装/API Key/CLAUDE.md/项目配置
│
├── 🔧 核心篇（04-06）
│   ├── 04-工具系统详解.md           # 文件编辑/Bash/Grep/WebFetch工具
│   ├── 05-记忆系统详解.md           # CLAUDE.md/MEMORY.md/项目记忆
│   └── 06-MCP集成实战.md            # MCP Server配置/工具扩展
│
├── 🚀 进阶篇（07-08）
│   ├── 07-Skill系统与斜杠命令.md     # 自定义Skill/斜杠命令/工作流
│   └── 08-Hooks与自动化.md          # PreToolUse/PostToolUse/Stop Hooks
│
├── 📋 实战篇（09-10）
│   ├── 09-实战技巧与最佳实践.md      # Java项目实战/Code Review/重构
│   └── 10-vs竞品深度对比.md          # vs Cursor/Copilot/ChatGPT
│
└── 📌 00-ClaudeCode知识体系总览.md    # ← 本文件
```

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景+路线 | — |
| 01 | 快速入门 | 5分钟上手/核心命令 | ⭐⭐⭐⭐ |
| 02 | 核心概念与架构 | Agent循环/权限模型 | ⭐⭐⭐⭐ |
| 03 | 安装与配置详解 | CLI安装/CLAUDE.md | ⭐⭐⭐ |
| 04 | 工具系统详解 | 文件/Bash/Grep工具 | ⭐⭐⭐⭐ |
| 05 | 记忆系统详解 | CLAUDE.md/MEMORY.md | ⭐⭐⭐⭐ |
| 06 | MCP集成实战 | MCP Server/工具扩展 | ⭐⭐⭐ |
| 07 | Skill系统与斜杠命令 | 自定义Skill/工作流 | ⭐⭐⭐ |
| 08 | Hooks与自动化 | 钩子/自动化流水线 | ⭐⭐⭐ |
| 09 | 实战技巧与最佳实践 | Java项目/CR/重构 | ⭐⭐⭐⭐ |
| 10 | vs竞品深度对比 | vs Cursor/Copilot选型 | ⭐⭐⭐ |

## 3. 学习路线

```text
🟢 上手（30min）：01-快速入门 → 03-安装配置
🔵 理解（1h）：02-架构 → 04-工具 → 05-记忆
🟣 进阶（1h）：06-MCP → 07-Skill → 08-Hooks
🟡 实战（30min）：09-最佳实践 → 10-对比选型
```
