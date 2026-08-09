# Reasonix 知识体系总览

> 专为 DeepSeek 打造的终端 AI 编程 Agent（GitHub 30k+ star，MIT 开源）——把 DeepSeek 前缀缓存压榨到极致的"缓存优先"架构，长会话成本仅 Claude Code 的 1/4。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [一句话定位](#5-一句话定位)

## 1. 知识体系导图

```
Reasonix（esengine · DeepSeek-Reasonix · MIT 开源）
│
├── 01 产品定位与设计哲学      只为 DeepSeek 打造，放弃通用性
│   ├── DeepSeek 原生 vs 通用 Agent
│   ├── "缓存稳定性 = 架构级不变量"
│   └── 生态位：DeepSeek 生态的 Claude Code
│
├── 02 安装部署与快速上手      三步启动 + 向导 + 三种模式
│   ├── npm / brew / 预编译包 三种安装
│   ├── API Key 三处配置位置
│   └── code / chat / run 三种运行模式
│
├── 03 缓存优先架构（核心）     前缀缓存机制 + 三区上下文划分
│   ├── DeepSeek 前缀缓存原理（字节级匹配）
│   ├── 不可变前缀 / 只追加日志 / 临时草稿区
│   ├── 跨会话缓存持久化（TTL 5-15 分钟）
│   └── 实测：99.82% 命中率，成本 2 折
│
├── 04 配置体系与双模型         reasonix.toml 全声明 + 双模型协作
│   ├── 配置驱动：Providers/工具/插件全声明
│   ├── Executor + Planner 双模型（各自独立缓存）
│   └── planMode / autoCheckpoint / hooks
│
├── 05 工具调用修复与成本控制   四类故障自动修复 + 智能升级
│   ├── Tool-Call Repair：消失/畸形/风暴/截断
│   ├── Cost Control：默认 Flash，/pro 临时升级
│   └── 失败信号触发自动升级策略
│
├── 06 记忆与技能系统           R1 思维链回收 + 持久记忆 + Skills
│   ├── R1 Thought Harvest 思维链回收
│   ├── Persistent Memory（前缀内字节稳定）
│   └── Skills：Markdown frontmatter + subagent
│
├── 07 斜杠命令与 TUI 交互      命令全览 + 三级执行模式
│   ├── /plan /init /stats /compact /memory 等
│   ├── review / AUTO / YOLO 权限三级
│   └── TUI 信息层次与成本仪表
│
├── 08 生态扩展与版本演进        MCP + VS Code + 桌面版 + Kimi K3
│   ├── MCP 插件（--mcp 一行挂载）
│   ├── VS Code 扩展（ACP 协议）/ 桌面版（Tauri）
│   ├── v1.18 Kimi K3 / v1.20 扩展内核（2026-08-05）
│   └── 生态分支：reasonix-legacy 等
│
└── 09 对比选型与面试速记       成本对比 + 选型决策 + 面试题
    ├── vs Claude Code / Aider / Cursor
    ├── "Reasonix 80% 日常 + Claude Code 20% 复杂"
    └── 面试速记
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | 产品定位与设计哲学 | DeepSeek 原生理念、放弃通用性、生态位 | 入门必读 |
| 02 | 安装部署与快速上手 | 安装三选一、API Key、doctor、启动向导、三模式 | 新手第一步 |
| 03 | 缓存优先架构 | 前缀缓存原理、三区划分、TTL、成本实测 | **核心必读** |
| 04 | 配置体系与双模型 | reasonix.toml 全配置、Executor+Planner、hooks | 进阶 |
| 05 | 工具调用修复与成本控制 | 四类故障自修复、/pro 升级、失败升级 | 进阶 |
| 06 | 记忆与技能系统 | R1 思维链回收、持久记忆、Skills | 高阶 |
| 07 | 斜杠命令与 TUI | 命令全览、执行模式三级、成本仪表 | 日常高频 |
| 08 | 生态扩展与版本演进 | MCP、VS Code、桌面版、Kimi K3、v1.20 | 高阶/选型 |
| 09 | 对比选型与面试速记 | vs Claude Code/Aider/Cursor、决策树、面试题 | 面试/落地 |

## 3. 学习路线推荐

**路线一：新手速成（半天上手）**
01 产品定位 → 02 安装上手 → 03 缓存架构（理解为什么省）→ 07 常用命令

**路线二：成本敏感用户（核心玩法）**
03 缓存架构 → 04 配置体系 → 05 成本控制 → 06 记忆与技能

**路线三：深度用户 / 迁移者（Claude Code 用户）**
09 对比选型 → 02 上手 → 04 配置（迁移 hooks/skills/memory 习惯）→ 08 生态

## 4. 核心概念速查

| 概念 | 一句话速记 |
|---|---|
| 前缀缓存 | DeepSeek 默认机制：请求字节前缀与先前一致则命中，命中价仅 1/50-1/120 |
| Cache-First Loop | Reasonix 架构级不变量：上下文只追加不重写，保证前缀字节稳定 |
| 不可变前缀 | system + 工具定义 + 持久记忆 + few-shots，会话内哈希锁定 |
| 只追加日志 | 对话历史 append-only，不压缩不重排 |
| 临时草稿区 | 每轮重置的临时信息，归入日志前经 Tool-Call Repair 提炼 |
| Tool-Call Repair | 四轮工序修复 DeepSeek 工具调用故障（消失/畸形/风暴/截断） |
| Cost Control | 默认 Flash 模型，`/pro` 单轮升级 Pro 后自动切回 |
| Executor + Planner | 执行器 + 规划器双模型协作，各自独立缓存 |
| R1 Thought Harvest | 回收 R1 模型 `<think>` 思维链形成可回放记录 |
| Persistent Memory | 固定前缀内的持久记忆，会话内字节级稳定 |
| Skills | Markdown frontmatter 技能脚本，支持 subagent 隔离运行 |
| reasonix.toml | 配置文件：模型/工具/插件全声明，零硬编码 |
| MCP | 外部工具服务器协议，`--mcp "name=cmd args"` 一行挂载 |
| review / AUTO / YOLO | 执行权限三级：全确认 / 自动 / 完全放手 |
| ACP 协议 | Agent Client Protocol，VS Code 扩展与 CLI 的连接通道 |

## 5. 一句话定位

> Reasonix 是"把 DeepSeek 前缀缓存当作工程核心"的终端编码 Agent：以放弃模型通用性为代价，换来长会话 90%+ 缓存命中率和约 1/4 于 Claude Code 的成本——**它是 DeepSeek 生态的 Claude Code**。

---

## 参考来源

- [DeepSeek-Reasonix GitHub 仓库（esengine）](https://github.com/esengine/deepseek-reasonix)
- [Reasonix npm 包](https://www.npmjs.com/package/reasonix)
- [标星近 15k，这个高颜值 Coding Agent 如何用 DeepSeek 缓存降低会话成本（腾讯云）](https://cloud.tencent.com.cn/developer/article/2680276)
- [DeepSeek-Reasonix：把前缀缓存压榨到极致的终端 AI 编程 Agent（CSDN）](https://blog.csdn.net/design1985/article/details/163433312)
- [一天省 60 刀：DeepSeek-Reasonix 把缓存命中率干到 99.82%（博客园）](https://www.cnblogs.com/itech/p/20224255)
- [DeepSeek-Reasonix crosses 30K GitHub stars（TopAIPRODUCT, 2026-08-04）](https://topaiproduct.com/2026/08/04/deepseek-reasonix-crosses-30k-github-stars-the-claude-code-of-the-deepseek-ecosystem/)
- [Reasonix 完整使用指南：本地部署、AI 模型接入 API 定义、MCP 插件（腾讯云）](https://cloud.tencent.cn/developer/article/2716552)
- [DeepSeek Coding Agent in 2026: Reasonix vs Claude Code, Codex, and Cline（Totalum）](https://www.totalum.app/blog/deepseek-coding-agent-totalum-2026)
- [Reasonix: $12 DeepSeek Coding Agent vs $61 Claude Code（CreativeAINews）](https://www.creativeainews.com/articles/deepseek-reasonix-cache-first-coding-agent-2026/)
- [v1.20.0 发布说明 PR（GitHub）](https://github.com/esengine/DeepSeek-Reasonix/pull/7622)

---

**下一模块**：[01-Reasonix概述-产品定位与设计哲学](01-Reasonix概述-产品定位与设计哲学.md)
