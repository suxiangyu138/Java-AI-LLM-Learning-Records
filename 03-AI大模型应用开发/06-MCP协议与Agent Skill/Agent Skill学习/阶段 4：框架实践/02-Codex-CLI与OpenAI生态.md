# 02 Codex CLI 与 OpenAI 生态

> OpenAI 对标准的采用（GA）：内置技能安装器（/skills）、$ 符号调用、配置驱动——但正文 8KB 硬上限、allowed-tools 仅部分支持、无 GUI——"简单直接"路线的代表。

## 📚 目录

1. [Codex CLI 的支持状态](#1-codex-cli-的支持状态)
2. [技能目录与调用](#2-技能目录与调用)
3. [内置技能安装器](#3-内置技能安装器)
4. [与 Claude Code 的差异](#4-与-claude-code-的差异)
5. [OpenAI 生态（ChatGPT/IDE）](#5-openai-生态chatgptide)
6. [注意事项](#6-注意事项)
7. [面试高频问法](#7-面试高频问法)

## 1. Codex CLI 的支持状态

| 维度 | 状态 |
|---|---|
| 版本状态 | GA（正式版） |
| 技能目录 | .codex/skills/（+ .agents/skills/ 备援） |
| 定位 | OpenAI 对 Agent Skills 标准的采用 |
| 风格 | 简单直接（纯 CLI，无 GUI） |
| 性能基准 | SWE-bench Verified 77.3%（第二） |

```
OpenAI 采用标准（2026-01）→ Codex CLI 支持技能
定位：终端党、简单直接
Rust 重写后速度与 token 效率大幅提升
```

## 2. 技能目录与调用

### 目录

```
.codex/skills/（项目级）
~/.codex/（配置：config.toml）
.agents/skills/（通用备援路径）
```

### 调用方式

| 方式 | 用法 | 说明 |
|---|---|---|
| 自动触发 | 说触发词 | description 匹配 |
| 符号调用 | $skillname | 显式调用（区别于斜杠） |
| 配置 | config.toml | 模型/技能配置 |

### 调用示例

```
用户："$code-review 帮我审查这段代码"
——$ 符号显式调用技能
```

## 3. 内置技能安装器

### /skills 命令

```
内置技能安装器（Codex 特色）：
/skills → 浏览/安装技能（Marketplace 集成）
——命令行内完成安装（不手动拷目录）
```

### 安装流程

```
① /skills 列出可用技能（市场）
② 选择安装
③ 重启生效（安装后需重启）
```

### 意义

```
① 低门槛：安装技能像安装包（新一代 npm 思想）
② 生态入口：CLI 内完成发现与安装
③ 与 Marketplace 集成（06 篇）
```

## 4. 与 Claude Code 的差异

| 维度 | Claude Code | Codex CLI |
|---|---|---|
| 技能正文上限 | 约 500 行建议 | **8KB 硬上限** |
| allowed-tools | 完整支持 | 仅部分支持 |
| disable-model-invocation | 支持 | 不支持 |
| 调用符号 | 斜杠 / | $ 符号 |
| 安装器 | 手动拷贝 | **内置 /skills** |
| GUI | 有（桌面） | 无（纯 CLI） |
| 子代理/钩子 | 完整 | 有限 |

### 差异的影响

```
跨平台技能要注意：
① 正文控制在 8KB 内（Codex 上限）
② allowed-tools 在 Codex 降级（不报错但可能不生效）
③ $ 符号是 Codex 特色（其他工具不认识）
——写跨平台技能用通用写法（05 篇）
```

## 5. OpenAI 生态（ChatGPT/IDE）

### ChatGPT

```
ChatGPT 也采用 Agent Skills 标准：
技能可在 ChatGPT 使用（入口不同）
```

### 生态组合

```
OpenAI 生态的技能使用：
Codex CLI（终端）+ ChatGPT（对话）+ IDE 集成
——同一份 SKILL.md 在 OpenAI 生态内多入口
```

### 与 Claude 生态的对比

| 生态 | 入口 |
|---|---|
| Anthropic | Claude Code/Claude.ai/API |
| OpenAI | Codex CLI/ChatGPT/IDE |

```
两份生态都支持同一标准（SKILL.md 可移植）
——标准的价值：跨生态
```

## 6. 注意事项

| 注意 | 说明 |
|---|---|
| 正文 8KB 上限 | 超了技能可能被截断/失败 |
| allowed-tools 降级 | 不报错但可能不生效（别依赖） |
| 安装需重启 | /skills 安装后重启生效 |
| 无 GUI | 终端使用（配置全 CLI） |
| 命令名 | $ 符号（与其他工具斜杠不同） |

### 常见误区

| 误区 | 真相 |
|---|---|
| "$ 符号跨平台" | Codex 专属（其他工具不认识） |
| "8KB 只是建议" | 硬上限（超了技能失败/截断） |
| "allowed-tools 全平台" | 仅部分支持（别依赖） |
| "安装即生效" | 需重启 |
| "无 GUI 不方便" | 终端党友好（配置全 CLI） |

### 跨平台提醒

```
在 Codex 上测试技能时：
① 检查正文 ≤8KB（超了要精简）
② 验证 allowed-tools 是否按预期（可能降级）
③ 用自动触发验证（$ 是 Codex 专属）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| Codex CLI 技能状态？ | GA、OpenAI 采用标准 |
| 调用方式？ | 自动触发 + $skillname 符号 |
| /skills 是什么？ | 内置技能安装器（CLI 内安装） |
| 与 Claude Code 差异？ | 8KB 上限/部分 allowed-tools/$ 符号/无 GUI |
| 正文上限？ | 8KB 硬性 |
| 生态？ | Codex CLI + ChatGPT（同一标准多入口） |

### 面试加分表达

> "Codex CLI 是 OpenAI 对标准的采用（GA）：内置 /skills 安装器（CLI 内浏览安装，像装包）、$skillname 符号调用、Rust 重写后性能大幅提升（SWE-bench 77.3%）。与 Claude Code 的关键差异要记住：正文 8KB 硬上限、allowed-tools 仅部分支持（跨平台静默降级）、无 GUI。同一份 SKILL.md 在 OpenAI 生态（Codex + ChatGPT）多入口可用——这就是标准跨生态的价值。"

> 🎯 核心要点：Codex CLI GA（OpenAI 采用，SWE-bench 77.3%）；.codex/skills/ 目录；内置 /skills 安装器 + $ 符号调用；与 Claude 差异（8KB 上限/部分 allowed-tools/无 GUI）；OpenAI 生态多入口（Codex + ChatGPT）；跨平台注意（8KB 控制 + 降级验证）。

---

**下一模块**：[03-Gemini-CLI与Copilot](03-Gemini-CLI与Copilot.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
