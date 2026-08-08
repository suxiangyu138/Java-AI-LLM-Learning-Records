# 06 CLI Agent 生态：百炼 CLI / Qoder / OpenClaw / Hermes

> 定位：CLI 生态篇——终端 Agent 的 2026 格局：阿里云百炼 CLI（平台能力 CLI 化）、Qoder、OpenClaw、Hermes Agent，以及"平台 ↔ 本地 Agent"双向打通趋势（2026-08 基准）

## 📚 目录

1. [CLI Agent 生态格局](#1-cli-agent-生态格局)
2. [阿里云百炼 CLI：平台能力 CLI 化](#2-阿里云百炼-cli平台能力-cli-化)
3. [百炼 CLI 核心能力](#3-百炼-cli-核心能力)
4. [Qoder / OpenClaw / Hermes Agent](#4-qoder--openclaw--hermes-agent)
5. [双向打通趋势：平台 ↔ 本地 Agent](#5-双向打通趋势平台--本地-agent)
6. [CLI Agent 选型](#6-cli-agent-选型)
7. [核心要点](#7-核心要点)

## 1. CLI Agent 生态格局

```text
2026 CLI Agent 生态分层：
├─ 编码 Agent（本地）：Claude Code / Codex CLI / Qoder / OpenClaw
├─ 平台 CLI 化：阿里云百炼 CLI（modelstudioai/cli）
│   └─ 一行命令接入 150+ 模型 + 全套平台能力
├─ 协议接入：MCP（工具）/ A2A（Agent 间，Hermes 已支持 v1.0）
└─ 平台对接：Coze 3.0 可一键接入本地 Agent（Claude Code/Codex/OpenClaw）

关键事实：百炼 CLI 原生支持 Claude Code、Qoder、OpenClaw、Hermes Agent
         → 平台把"模型/知识库/记忆/搜索"开放为 CLI 工具，任何 Agent 可调
```

> 🎯 **一句话**：2026 年 CLI Agent 生态的核心叙事 = **"平台能力工具化"**——云平台把全套能力变成 Agent 可调用的 CLI/工具，本地 Agent 生态（Claude Code 系）作为消费方。

## 2. 阿里云百炼 CLI：平台能力 CLI 化

| 维度 | 事实 |
|------|------|
| 发布 | 2026-05-29 开源（GitHub：modelstudioai/cli） |
| 定位 | 专为 Agent 设计：一行命令接入百炼全套能力 |
| 模型 | 150+ 款（Qwen/GLM/Kimi/DeepSeek/Wan 等多模态） |
| 能力 | 应用/Workflow 调用、知识库检索、记忆库与用户画像、联网搜索、本地文件上传与多模态处理 |
| 兼容 | 原生支持 Claude Code、Qoder、OpenClaw、Hermes Agent 等主流 Agent 框架 |
| 设计 | 把面向人的平台交互（鉴权/参数/协议差异）封装为统一轻量命令行入口 |
| 配套 | 吞吐弹性调度（并发波峰波谷）、Agentic RL（基于执行反馈迭代） |

> 🎯 **设计洞察**：百炼 CLI = "**把平台做成 Agent 的工具**"——过去是人调用平台 API，现在是 Agent 一行命令调用平台能力；鉴权/参数/协议差异全部封装。

## 3. 百炼 CLI 核心能力

| 能力 | 说明 |
|------|------|
| 模型调用 | 文本/图像/视频/语音/视觉理解多模态 |
| 应用编排 | 调用百炼应用与 Workflow |
| 知识库 | 检索（RAG 能力 CLI 化） |
| 记忆 | 记忆库管理与用户画像 |
| 联网搜索 | 内置搜索能力 |
| 文件处理 | 本地文件上传与多模态处理 |
| 结构化输出 | 便于 Agent 工具调用与自动化编排 |

```text
示例（官方口径）：Agent 工具中输入提示词
"搜索今天关于 AI 新闻，给我生成一段相声，并且生成音频"
→ Agent 自动：联网搜索 → 调语言模型写脚本 → 调语音模型生成音频
全流程一行提示词驱动，无需拼接多个 API
```

> 💡 **与 Claude Code 的关系**：百炼 CLI 不替代 Claude Code——它把自己变成 Claude Code 可调用的"平台工具"；Claude Code 管编排，百炼 CLI 供能力（模型/知识库/记忆/搜索）。

## 4. Qoder / OpenClaw / Hermes Agent

| Agent | 归属 | 定位 | 2026 要点 |
|-------|------|------|----------|
| Qoder | 阿里 | 编码 Agent | 百炼 CLI 原生支持对象之一；与通义生态联动 |
| OpenClaw | 开源 | 本地 CLI Agent | Coze 3.0 可一键接入；百炼 CLI 支持 |
| Hermes Agent | NousResearch | 开源 Agent 框架 | 2026 增加 **A2A v1.0 协议插件**（Agent 间互操作） |
| Claude Code | Anthropic | 编码 Agent | 生态事实标准；多平台接入（百炼/Coze） |
| Codex CLI | OpenAI | 编码 Agent | Coze 3.0 可接入 |

> 💡 **Hermes 的 A2A 意义**：CLI Agent 从"单机工具"走向"协议化 Agent"——A2A 端点让 Hermes 可被其他 Agent 发现与委派（07 篇协议详述）。

## 5. 双向打通趋势：平台 ↔ 本地 Agent

```text
2026 双向打通（多平台共同动作）：

方向一：本地 Agent 接入平台协作
  Coze 3.0：Claude Code / Codex / OpenClaw 一键接入项目空间协作
  百炼 CLI：本地 Agent 调用平台能力

方向二：平台能力开放给本地 Agent
  百炼 CLI：150+ 模型 + 全套能力 CLI 化
  Coze：开源（Coze Studio + Loop）

结果：平台 = 能力供给方，本地 CLI Agent = 编排消费方，
      MCP/A2A = 连接协议
```

> 🎯 **生态判断**：本地 CLI Agent（Claude Code 系）成为 2026 年 Agent 生态的"客户端"，云平台成为"能力服务器"——**学会用 CLI Agent + 平台能力组合，比纠结单框架更有生产力**。

## 6. CLI Agent 选型

| 场景 | 推荐 | 理由 |
|------|------|------|
| 通用编码/终端 Agent | Claude Code | 生态最成熟（工具/hooks/技能） |
| 阿里云/Qwen 生态 | Qoder + 百炼 CLI | 平台能力一站式 |
| 多平台能力接入 | 任意 CLI Agent + 百炼 CLI | 能力与编排解耦 |
| Agent 间协议化协作 | Hermes（A2A v1.0） | 协议化互操作 |
| 平台内协作（含业务人员） | Coze 3.0 接入本地 Agent | 团队空间协作 |

## 7. 核心要点

> 🎯 **核心要点**：
> 1. 2026 CLI 生态核心叙事：**平台能力工具化**——百炼 CLI（2026-05-29 开源）让 150+ 模型/知识库/记忆/搜索成为 Agent 一行命令可调的工具
> 2. 百炼 CLI 原生兼容 Claude Code/Qoder/OpenClaw/Hermes——不替代 CLI Agent，而是做"能力供给方"
> 3. 双向打通：本地 Agent 接平台（Coze 3.0）↔ 平台能力开放（百炼 CLI）——MCP/A2A 为连接协议
> 4. 生态判断：本地 CLI Agent = 客户端，云平台 = 能力服务器——组合使用是 2026 生产力路径

---

**上一模块**：[05 自进化前沿](05-自进化前沿：Prime%20Agent%20与%20RLM%20harness.md)　**下一模块**：[07 协议新进展](07-协议新进展：A2A%20v1.0%20UCP%20OSSA%20三协议栈.md)　**返回总览**：[00 总览](00-总览：新兴%20SDK%20知识体系.md)

## 【参考来源】

- [阿里云开源百炼CLI，Agent可调用全套模型和应用能力（财联社）](https://www.cls.cn/detail/2385243)
- [阿里云开源百炼 CLI，AI Agent 即刻接入（钛媒体）](https://www.tmtpost.com/nictation/8007252.html)
- [阿里云百炼核心能力CLI化（C114）](https://www.c114.net.cn/industry/85706.html)
- [阿里雲百煉CLI開源（觀點網）](https://www.guandian.cn/article/20260529/563921.html)
- [字节跳动AI Agent平台扣子Coze上线3.0（C114）](https://www.c114.net.cn/ainews/86334.html)
- [Hermes Agent A2A v1.0 protocol plugin (GitHub PR)](https://github.com/NousResearch/hermes-agent/pull/77109)
