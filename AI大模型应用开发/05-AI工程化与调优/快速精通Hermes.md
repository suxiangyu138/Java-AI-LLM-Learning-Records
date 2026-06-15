# ⚙️ 快速精通 Hermes Agent

> **核心摘要**：Hermes Agent 是 Nous Research 开源的自进化 AI 代理框架，核心卖点是 AI 能从经验中自动学习、创建技能、优化行为，真正越用越聪明。本文涵盖自我进化机制、安装部署、配置详解、Skills 系统、Memory 架构、子代理并行及与 OpenClaw 的对比选型。

> **前置阅读**：[[快速精通-GPT-Gemini-OpenCode-OpenClaw-Hermes]]、[[OpenClaw 核心知识点]]

---

## 目录

1. [Hermes 是什么](#1-hermes-是什么)
2. [自我进化机制](#2-自我进化机制)
3. [安装与部署](#3-安装与部署)
4. [配置详解](#4-配置详解)
5. [Skills 自动学习系统](#5-skills-自动学习系统)
6. [Memory 与跨会话记忆](#6-memory-与跨会话记忆)
7. [子代理与并行](#7-子代理与并行)
8. [Hermes vs OpenClaw](#8-hermes-vs-openclaw)
9. [最佳实践](#9-最佳实践)

---

## 1. Hermes 是什么

Hermes Agent 由 **Nous Research** 团队于 2026 年 2 月开源，是 2026 年上半年增长最快的 AI Agent 项目之一。核心定位：**"The agent that grows with you"**。

### 核心差异

```
普通 AI Agent：用户定义规则 → Agent 执行 → 人工改进 → 再执行
Hermes Agent：用户提需求 → Agent 执行 → 自动发现规律 → 自动创建 Skill → 下次自动优化
```

---

## 2. 自我进化机制

### 2.1 学习循环

```
① 用户提出新任务
  ↓
② Hermes 分解并执行
  ↓
③ 成功 → 提取执行模式 → 自动创建 Skill
   失败 → 分析原因 → 调整策略 → 重试
  ↓
④ Skill 存入技能库
  ↓
⑤ 下次类似任务 → 自动匹配已有 Skill → 继续优化
```

### 2.2 进化范围

| 可自动进化 | 不会自动进化 |
|---|---|
| 执行策略（失败后换方案） | 安全边界 |
| 错误处理（自动重试、降级） | 核心人格（SOUL.md） |
| 格式适配（网站/API 变化） | 成本无上限 |
| 效率优化（发现更快路径） | — |

---

## 3. 安装与部署

### 3.1 环境准备

- Node.js 20+
- 至少一个 LLM API Key
- （可选）Docker / Termux

### 3.2 安装方式

```bash
# npm 全局安装（推荐）
npm install -g hermes-agent
hermes init   # 交互式配置
hermes start

# Docker
docker run -d \
  --name hermes-agent \
  -v ~/.hermes:/app/data \
  -e ANTHROPIC_API_KEY=sk-ant-xxx \
  ghcr.io/nousresearch/hermes-agent:latest

# Termux（Android）
pkg install nodejs git
npm install -g hermes-agent
hermes start --mobile
```

### 3.3 目录结构

```
~/.hermes/
├── config.yaml            # 主配置
├── SOUL.md                # 人格定义（手写）
├── MEMORY.md              # 事件记忆（自动维护）
├── skills/                # 已学技能库
│   ├── auto/              # 自动创建的技能
│   └── custom/            # 手动创建的技能
└── logs/                  # 运行日志
```

---

## 4. 配置详解

### 4.1 config.yaml 核心配置

```yaml
name: "Hermes"
locale: zh-CN
timezone: Asia/Shanghai

persona:
  soul_file: SOUL.md
  language: Chinese
  style: concise-&-technical

default_model: claude-sonnet-4-6

# 模型路由（按任务类型自动切换）
model_routing:
  code:     { model: claude-sonnet-4-6, provider: anthropic }
  review:   { model: gpt-4.1, provider: openai }
  fast:     { model: gemini-2.5-flash, provider: google }
  reasoning:{ model: o4-mini, provider: openai }

# 学习设置
learning:
  auto_create_skills: true
  auto_improve_skills: true
  min_confidence_to_save: 0.7

# 子代理
subagents:
  max_concurrent: 5
  timeout: 300

# 成本控制
budget:
  daily_limit: 30
  monthly_limit: 300
```

### 4.2 SOUL.md 范例

```markdown
# SOUL.md

## 身份
- 我叫 Hermes，是一个 AI 开发助手
- 风格：直接、技术导向
- 语言：中文回复，代码/术语保留英文

## 工作原则
- 代码优先于文字解释
- 涉及安全/钱的决策必须二次确认
- 修改文件前先读，改完显示 diff

## 技术偏好
- Java 17+，日期时间用 java.time
- 测试框架 JUnit 5 + Mockito
```

---

## 5. Skills 自动学习系统

### 5.1 手动创建技能

```yaml
name: java-code-review
version: "1.0"
trigger_keywords: ["审查", "code review", "检查代码"]
execution:
  model: gpt-4.1
  temperature: 0.2
steps:
  - analyze: |
      审查以下 Java 代码，按维度评分（1-10）：
      1. 安全性 2. 正确性 3. 性能 4. 规范
  - suggest: |
      基于问题给出改进后的代码，只改有问题部分。
```

### 5.2 自动创建技能

当 Hermes 成功完成复杂任务后，自动询问是否保存为技能，生成文件到 `~/.hermes/skills/auto/`。系统会持续优化已有技能的执行步骤。

### 5.3 技能管理命令

```bash
/skills list              # 列出所有技能
/skills show 3            # 查看详情
/skills improve 3         # 触发优化
/skills delete 5          # 删除
/skills export 3 > skill.yaml  # 导出分享
/skills stats             # 使用频率/成功率
```

---

## 6. Memory 与跨会话记忆

### 6.1 三层记忆架构

| 层级 | 文件 | 维护方式 | 大小 |
|---|---|---|---|
| 人格层 | SOUL.md | 手动 | ~2-5KB |
| 事件层 | MEMORY.md | 自动（FTS5 全文搜索） | 动态增长 |
| 短期上下文 | 对话层 | 自动（窗口 > 70% 压缩） | N 轮 |

### 6.2 记忆搜索

```bash
/memory "上周讨论的部署方案是什么？"
/memory add "用户偏好：数据库迁移用 Flyway"
/memory search "Flyway"
/memory summary
```

> **重点**：跨平台记忆同步意味着用户在 Telegram 上提及的信息，切换到微信后 Hermes 仍然记得上下文。

---

## 7. 子代理与并行

```bash
# 自动并行：Hermes 自动分解任务
"分析这 3 个微服务各自的性能瓶颈"
# → 自动创建 3 个子代理，并行分析

# 手动创建子代理
/subagent "分析 UserService 的性能瓶颈"
/subagent --model o3 "审查 AuthService 的安全漏洞"

# 管理
/subagent list       # 列出活跃子代理
/subagent result 3   # 查看结果
```

### 实战：审查 + 测试并行

```
"审查 UserService 并生成测试"
→ 子代理 1（claude-sonnet-4-6）：审查代码质量
→ 子代理 2（gpt-4.1）：审查安全性
→ 子代理 3（gemini-2.5-flash）：生成 JUnit 测试
→ 主代理：汇总报告 + 测试代码
```

---

## 8. Hermes vs OpenClaw

| 维度 | Hermes Agent | OpenClaw |
|---|---|---|
| 开源时间 | 2026.02 | 2025.11 |
| Stars | 1.4 万+ | 36 万+ |
| 核心差异 | **自我进化**：自动创建技能 | **稳定性**：可靠的手动工作流 |
| 技能来源 | Agent 自动生成 + 手动 | ClawHub 5700+ 技能市场 |
| 模型支持 | 200+（含国内全系） | 主流模型 |
| 平台支持 | 17+（微信/QQ/钉钉/飞书） | 12+ |
| 适合场景 | 愿意尝试前沿技术 | 需要稳定可靠的生产方案 |

---

## 9. 最佳实践

### 9.1 起步路线

```
第 1 天：安装 + 接 Telegram
第 2 天：写 SOUL.md
第 3 天：做 3 件事，观察自动学习
第 4 天：配置微信/飞书
第 5 天：创建手动 Skill，对比自动 Skill
```

### 9.2 成本管理

```yaml
# 三层降本策略
default_model: gemini-2.5-flash       # 默认最便宜
model_routing:
  code: claude-sonnet-4-6             # 代码用最贵
  fast: gemini-2.5-flash              # 聊天最便宜
budget:
  daily_limit: 20
  monthly_limit: 200
```

### 9.3 安全注意事项

- Hermes 会自己写代码并执行，必须沙箱隔离
- 敏感操作（git push / deploy / rm）设置 `require_confirm`
- 公网部署必须加 HTTPS + 认证
- 定期检查自动创建的 Skill
- 个人微信方案有封号风险，推荐企业微信

---

## 核心要点回顾

- Hermes Agent 的核心差异化是自我进化——从成功经验中自动学习创建技能
- 三层记忆架构（SOUL.md + MEMORY.md + 短期上下文）实现跨平台、跨会话记忆
- 子代理支持并行任务分解，提高复杂任务处理效率
- 支持 200+ 模型和 17+ 平台（含微信/QQ/钉钉/飞书）
- 定位：如果你希望 AI 越用越聪明、长期积累经验，选 Hermes；反之追求稳定可靠选 OpenClaw

## 参考资料

1. Hermes Agent GitHub：https://github.com/NousResearch/hermes-agent
2. Nous Portal：https://portal.nousresearch.com
3. Nous Research：https://nousresearch.com
4. Hermes 模型系列：https://huggingface.co/NousResearch
