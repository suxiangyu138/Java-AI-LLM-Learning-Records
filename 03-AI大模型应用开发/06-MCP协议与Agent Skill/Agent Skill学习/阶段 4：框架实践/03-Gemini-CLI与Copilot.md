# 03 Gemini CLI 与 Copilot

> 两大"预览期"采用者：Gemini CLI（Google，免费档 + @ 附加 + Claude 技能转换器）、GitHub Copilot（微软，VS Code 零配置 + 多环境）——功能有限但生态入口广。

## 📚 目录

1. [Gemini CLI 支持状态](#1-gemini-cli-支持状态)
2. [Gemini 的技能使用](#2-gemini-的技能使用)
3. [Claude 技能转换器](#3-claude-技能转换器)
4. [GitHub Copilot 支持状态](#4-github-copilot-支持状态)
5. [Copilot 的技能使用](#5-copilot-的技能使用)
6. [两大预览者的对比](#6-两大预览者的对比)
7. [面试高频问法](#7-面试高频问法)

## 1. Gemini CLI 支持状态

| 维度 | 状态 |
|---|---|
| 版本状态 | 预览（Preview） |
| 技能目录 | .gemini/skills/ |
| 开始时间 | 2026-01（预览支持） |
| 免费档 | 有（每日 1,000 次请求） |
| 性能基准 | SWE-bench Verified 约 65% |

```
Google 对标准的采用（预览阶段）：
免费档可用（成本敏感用户的入口）
功能较其他工具有限，仍在收集反馈
```

## 2. Gemini 的技能使用

### 调用方式

```
@ 符号：将技能文件附加到提示词
（区别于 Claude 的自动触发/Codex 的 $）
```

### 使用流程

```
① 技能放 .gemini/skills/
② 对话中用 @ 附加技能文件
③ 技能内容进入上下文
——显式附加（非自动触发优先）
```

### 特点

| 特点 | 说明 |
|---|---|
| 免费档 | 每日 1,000 次请求（试用友好） |
| @ 附加 | 显式引用（简单直接） |
| 1M 上下文 | Google 生态优势 |
| 功能有限 | 预览阶段（收集反馈） |

## 3. Claude 技能转换器

### 是什么

```
Gemini CLI 提供 Claude converter 工具：
转换现有 Claude 技能（自动适配）
——迁移友好（Claude 技能 → Gemini）
```

### 转换的意义

```
① 存量复用：Claude 技能不浪费
② 低门槛迁移：converter 自动处理
③ 生态互通：标准让转换成为可能
```

### 使用注意

```
转换后检查：
① 专属字段是否被处理（allowed-tools 等）
② 正文兼容性（Gemini 支持范围）
③ 实际运行验证（转换 ≠ 完美）
```

## 4. GitHub Copilot 支持状态

| 维度 | 状态 |
|---|---|
| 版本状态 | 预览（Preview） |
| 技能目录 | .github/skills/ |
| 开始时间 | VS Code 1.108（2025-12） |
| 配置 | 需开启 chat.useAgentSkills 设置 |
| 定价 | $10-39/月（付费计划） |

```
微软对标准的采用（预览）：
VS Code 无缝集成（零额外配置）
多环境：VS Code/CLI/coding agent
```

## 5. Copilot 的技能使用

### 使用方式

```
① 技能放 .github/skills/
② 开启 chat.useAgentSkills 设置
③ Copilot Chat 聊天界面使用技能
——VS Code 用户零门槛
```

### 特点

| 特点 | 说明 |
|---|---|
| VS Code 集成 | 编辑器内使用（无缝） |
| 多环境 | VS Code/CLI/cloud agent |
| 不支持 allowed-tools | 降级（跨平台注意） |
| 预览稳定性 | 预览期可能有问题 |

### 跨平台注意

```
Copilot 不支持 allowed-tools：
技能在 Copilot 上运行 = 无工具白名单（降级）
——安全敏感技能在 Copilot 上要额外小心
```

## 6. 两大预览者的对比

| 维度 | Gemini CLI | GitHub Copilot |
|---|---|---|
| 厂商 | Google | 微软 |
| 状态 | 预览 | 预览 |
| 目录 | .gemini/skills/ | .github/skills/ |
| 调用 | @ 附加 | 聊天界面 |
| 免费 | 有（每日 1,000 次） | 无（付费计划） |
| 集成 | 终端 + Google 搜索 | VS Code（零配置） |
| allowed-tools | 有限 | 不支持 |
| 转换工具 | Claude converter | 无 |

### 常见误区

| 误区 | 真相 |
|---|---|
| "预览 = 不能用" | 核心功能可用（生产慎用专属） |
| "@ 附加是唯一方式" | 显式附加为主（自动触发看支持） |
| "converter 完美转换" | 转换后要检查与实测 |
| "Copilot 免费" | 需付费计划（$10-39/月） |
| "预览功能稳定" | 演进中（版本变化） |

### 共性

```
① 都是"采用者"（非原创者）：功能是 Claude Code 的子集
② 都在预览期：功能演进中
③ 跨平台技能都可运行（核心功能）
——写技能以 Claude Code 为准，这两个平台做验证
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| Gemini CLI 技能状态？ | 预览（2026-01）、免费档 |
| Gemini 怎么调用？ | @ 符号附加 |
| Claude converter？ | 转换 Claude 技能（迁移友好） |
| Copilot 状态？ | 预览、VS Code 零配置 |
| Copilot 调用？ | 聊天界面 + chat.useAgentSkills |
| 两大预览者共性？ | 功能子集、演进中、跨平台可用 |

### 面试加分表达

> "Gemini CLI 和 Copilot 都是标准的预览期采用者，功能是 Claude Code 的子集：Gemini 用 @ 附加技能（有免费档 + Claude converter 转换器）、Copilot 在 VS Code 里零配置用（聊天界面，但 chat.useAgentSkills 要开）。跨平台注意：Copilot 不支持 allowed-tools（安全敏感技能会降级）。两个平台适合做'第二验证'——主平台还是功能最全的 Claude Code。"

> 🎯 核心要点：Gemini CLI 预览（@ 附加/免费档/Claude converter/1M 上下文）；Copilot 预览（VS Code 零配置/chat.useAgentSkills/多环境/不支持 allowed-tools）；两者是功能子集（写技能以 Claude Code 为准）；预览期功能演进中；作为跨平台第二验证平台。

---

**下一模块**：[04-Cursor与OpenCode等其他平台](04-Cursor与OpenCode等其他平台.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
