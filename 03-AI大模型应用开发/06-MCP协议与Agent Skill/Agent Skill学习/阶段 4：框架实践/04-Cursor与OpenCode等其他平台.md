# 04 Cursor 与 OpenCode 等其他平台

> 支持矩阵的剩余拼图：Cursor（Nightly 尚不稳定）、OpenCode（开源原生）、Snowflake/Kiro/Junie 等——约 40 平台的生态全景与完整支持矩阵。

## 📚 目录

1. [平台全景](#1-平台全景)
2. [Cursor](#2-cursor)
3. [OpenCode](#3-opencode)
4. [其他平台](#4-其他平台)
5. [完整支持矩阵](#5-完整支持矩阵)
6. [平台选择建议](#6-平台选择建议)
7. [面试高频问法](#7-面试高频问法)

## 1. 平台全景

```
约 40 个平台支持 Agent Skills（2026 初）：
主流六家：Claude Code/Codex/Copilot/Gemini/Cursor/OpenCode
其他：Snowflake Cortex Code/Kiro/JetBrains Junie/VS Code 等
```

| 梯队 | 平台 | 状态 |
|---|---|---|
| 成熟 | Claude Code / Codex CLI | GA |
| 预览 | Copilot / Gemini CLI | Preview |
| 早期 | Cursor | Nightly |
| 原生 | OpenCode | 开源原生 |

## 2. Cursor

| 维度 | 状态 |
|---|---|
| 版本状态 | Nightly（早期） |
| 技能目录 | .cursor/skills/ |
| 定位 | AI 编辑器（底层模型可选 Claude 最佳） |
| 性能基准 | SWE-bench Verified 约 73% |

### 特点

```
① Nightly：支持尚不稳定（等稳定版）
② 编辑器内使用（非 CLI）
③ 底层模型可选（选 Claude 效果最佳）
```

### 使用注意

```
① Nightly 阶段：生产慎用（等稳定）
② 目录 .cursor/skills/
③ 跨平台技能可放（核心功能支持）
```

## 3. OpenCode

| 维度 | 状态 |
|---|---|
| 版本状态 | 原生支持 |
| 技能目录 | .opencode/skills/ |
| 定位 | 开源 Agent（可自托管） |
| 特点 | 开源、配置自由 |

### 特点

```
① 开源：可自托管/定制（私有化场景）
② 原生支持：技能是一等公民
③ 目录：.opencode/skills/
```

### 适用

```
① 开源/自托管需求
② 定制化场景（改源码）
③ 跨平台验证（原生支持）
```

## 4. 其他平台

| 平台 | 说明 |
|---|---|
| Snowflake Cortex Code | 数据云生态 |
| Kiro | Agent 框架 |
| JetBrains Junie | JetBrains IDE |
| VS Code | 编辑器级支持 |
| GitHub Copilot cloud | 云 agent |

```
约 40 平台的生态全景：
从 CLI 到 IDE 到云 agent 全覆盖
——标准已经是"事实生态"（阶段 1 的 04 篇）
```

## 5. 完整支持矩阵

| 工具 | 状态 | 目录 | 调用 | 特色 |
|---|---|---|---|---|
| Claude Code | GA | .claude/skills/ | 自动/斜杠 | 功能最全（参考实现） |
| Codex CLI | GA | .codex/skills/ | 自动/$ | /skills 安装器、8KB 上限 |
| Copilot | 预览 | .github/skills/ | 聊天界面 | VS Code 零配置 |
| Gemini CLI | 预览 | .gemini/skills/ | @ 附加 | 免费档、converter |
| Cursor | Nightly | .cursor/skills/ | 编辑器 | 尚不稳定 |
| OpenCode | 原生 | .opencode/skills/ | 自动 | 开源自托管 |

### 通用总线（回顾）

```
所有 agent 都读 .agents/skills/（备援路径）
→ 跨平台技能的"主目录"（05 篇详述）
```

## 6. 平台选择建议

| 需求 | 选择 |
|---|---|
| 功能最全/标准参考 | Claude Code |
| 终端党/简单直接 | Codex CLI |
| VS Code 用户 | Copilot |
| 成本敏感/免费 | Gemini CLI |
| 开源自托管 | OpenCode |
| 编辑器内 | Cursor（等稳定） |

### 常见误区

| 误区 | 真相 |
|---|---|
| "Nightly 也能生产" | 不稳定（等稳定版） |
| "开源 = 免费完整" | OpenCode 开源但需自托管运维 |
| "40 平台全成熟" | 分梯队（GA/预览/Nightly） |
| "目录都放一份" | 用 .agents/skills/ 总线 |
| "选平台只看基准" | 还要看生态与需求匹配 |

### 生态观察（阶段 1 的 04 篇呼应）

```
约 40 平台的生态意义：
① 标准成为事实（不是某一家私有）
② 技能资产跨生态流通（不锁定）
③ 竞争推动实现完善（GA 梯队）
——生态规模 = 标准价值的证明
```

### 跨平台策略

```
① 主平台：Claude Code（功能全，测试基准）
② 第二验证：Codex（GA）或 Gemini（免费）
③ 技能放 .agents/skills/（通用总线）
——"主开发 + 多验证"策略（05 篇深化）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| 约多少平台支持？ | 约 40 个 |
| Cursor 状态？ | Nightly（早期不稳定） |
| OpenCode 特点？ | 开源原生（可自托管） |
| 完整矩阵？ | 六家状态/目录/调用/特色 |
| 通用总线？ | .agents/skills/（所有 agent 读） |
| 选择建议？ | 按需求（功能/终端/VS Code/免费/开源） |

### 面试加分表达

> "Agent Skills 的生态全景是约 40 平台：成熟梯队（Claude Code/Codex GA）、预览梯队（Copilot/Gemini）、早期（Cursor Nightly）、开源原生（OpenCode）。跨平台策略是'主开发 + 多验证'：以功能最全的 Claude Code 为主平台开发测试，Codex 或 Gemini 做第二验证，技能放 .agents/skills/ 通用总线——一份 SKILL.md 六家可跑。"

> 🎯 核心要点：约 40 平台全景（四梯队）；Cursor Nightly（等稳定）；OpenCode 开源原生（自托管）；完整六家矩阵（状态/目录/调用/特色）；通用总线 .agents/skills/；选择按需求（功能/终端/VS Code/免费/开源）；跨平台策略"主开发 + 多验证"。

---

**下一模块**：[05-跨平台技能设计](05-跨平台技能设计.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
