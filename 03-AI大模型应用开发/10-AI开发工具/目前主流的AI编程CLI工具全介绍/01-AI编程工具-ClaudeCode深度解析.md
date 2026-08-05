# Claude Code 深度解析

> 🧠 Anthropic 出品的终端 AI Agent —— 三层架构、CLAUDE.md 上下文、Worktree 隔离、Hook 自动化、Skills 扩展

---

## 📚 目录

1. [核心定位与架构](#1-核心定位与架构)
2. [CLAUDE.md 项目记忆](#2-claudemd-项目记忆)
3. [Worktree 隔离](#3-worktree-隔离)
4. [Hook 与 Skills 扩展](#4-hook-与-skills-扩展)
5. [价格与最佳实践](#5-价格与最佳实践)

---

## 1. 核心定位与架构

```text
Claude Code = Anthropic 的终端原生 AI 编程 Agent

定位：项目级多文件重构、遗留代码现代化、复杂全栈任务
类型：Terminal CLI Agent（非 IDE 插件）
模型：仅 Claude 系列（Sonnet/Opus/Haiku）

三层 Agent 架构：
  1. 规划层 → 理解任务 → 拆分步骤 → 制定计划
  2. 工具调用层 → 读文件(Grep/Glob/Read) → 编辑(Edit/Write) → 执行(Bash)
  3. 上下文管理层 → CLAUDE.md + Memory + Git 状态感知

核心工具集：
  Read/Write/Edit   → 文件操作
  Grep/Glob         → 代码搜索
  Bash              → 命令执行
  Agent/Workflow    → 子代理/编排
  Skill             → 加载专项技能
  Task              → 后台任务管理
```

| 维度 | 说明 |
|------|------|
| **最擅长** | 多文件重构、代码迁移、技术方案设计、自动化测试 |
| **不擅长** | 实时代码补全（无 IDE 插件级别的 Tab 补全） |
| **适合人群** | 高级后端/架构师、DevOps、终端重度用户 |
| **真实案例** | Django Auth 模块拆分为微服务：47 分钟完成人工 3-5 天工作量 |

---

## 2. CLAUDE.md 项目记忆

```markdown
# CLAUDE.md — 项目根目录下的 AI 记忆文件

## Repository Purpose
Personal Java backend + AI learning knowledge base.
1,460+ markdown technical documents, 6-layer pyramid.

## Directory Structure
01-底层根基-Java核心底座/
02-后端中间件 微服务工程及相关拓展/
...

## Documentation Standards
- `> 🎯` summary, `> ⚠️` warning, `> 💡` tip
- Tables preferred for comparisons
- Code blocks MUST specify language
```

```text
CLAUDE.md 作用：
  ✅ 每次对话自动加载 → 省去重复说明项目背景
  ✅ 定义文档规范 → 所有输出自动遵循
  ✅ 指定常用命令 → Agent 自动使用正确的构建/测试命令
  ✅ 项目级 Memory → 比每次在对话中说明准确
```

---

## 3. Worktree 隔离

```text
Worktree = 对 Agent 启用独立 Git 工作树

作用：
  → 每个 Agent 在隔离的工作树中操作
  → 未修改的原工作树不受影响
  → 完成任务后自动清理（如无变更）

适用场景：
  ✅ 多 Agent 并行修改同一仓库（避免文件冲突）
  ✅ 验证性任务（Agent 改完但不确定是否正确时）
  ⚠️ 开销：每次创建 200-500ms + 磁盘空间
```

---

## 4. Hook 与 Skills 扩展

### Hook

```text
Hook = 在特定事件触发自动化脚本

事件类型：
  PreToolUse      → 工具调用前（如拦截危险命令）
  PostToolUse     → 工具调用后（如自动格式化）
  Stop            → Agent 停止时
  SessionStart    → 会话开始时
  UserPromptSubmit → 用户发消息时

实战：
  → PreToolUse 拦截 rm -rf / 等危险命令
  → PostToolUse 自动运行代码格式化
  → SessionStart 自动 git pull 最新代码
```

### Skills

```text
Skills = 可加载的专项能力包

内置 Skill（部分）：
  /code-review      → 代码审查
  /security-review  → 安全检查
  /simplify         → 代码简化重构
  /commit-commands  → Git 提交与 PR
  /deep-research    → 深度调研

自定义 Skill：
  → 放在 .claude/skills/ 目录
  → 定义专业领域的操作流程
  → 通过 /skill-name 调用
```

---

## 5. 价格与最佳实践

| 方案 | 价格 | 限制 |
|------|:---:|------|
| **捆绑 Claude Pro** | $20/月 | Rate Limit 严格，轻度使用 |
| **Max 计划** | $100/月 | 中等频率 |
| **Max 加强** | $200/月 | 高频重度使用 |

```text
最佳实践：
  ✅ 项目根目录放 CLAUDE.md（最关键！）
  ✅ 复杂任务拆分为多个小 Agent 调用
  ✅ 利用 Workflow 并行化独立任务
  ✅ Skills 封装重复性操作流程
  ✅ Git 频繁提交，方便回滚 AI 改动
  ❌ 不要让 Agent 一次改太多文件
  ❌ 不要跳过 Review AI 的每次改动
```

---

> 🎯 Claude Code 是 **2026 年 Agent 能力最强的终端 AI 工具**。优势在于复杂多文件重构，劣势在于无实时补全需搭配 IDE 工具。CLAUDE.md 是使用它的第一要务。

---

*创建于：2026年7月*
