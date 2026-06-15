# Claude Code 工具系统详解

> **核心摘要**：Claude Code 内置了约 30 个工具，分为文件工具、搜索工具、执行工具、网络工具、Agent 工具、Plan 工具等。本文深度解析各工具的用法和最佳实践。

---

## 一、工具全景图

```
Claude Code 工具集
├── 文件工具：Read / Write / Edit / Glob / NotebookEdit
├── 搜索工具：Grep（内容搜索）
├── 执行工具：Bash / PowerShell
├── 网络工具：WebSearch / WebFetch
├── Agent 工具：Agent（子代理）/ Task（任务管理）
├── Plan 工具：EnterPlanMode / ExitPlanMode
├── Git 工具：通过 Bash 调用 gh CLI
├── 交互工具：AskUserQuestion / PushNotification
├── 高级工具：Skill / EnterWorktree / ExitWorktree
└── 定时工具：CronCreate / CronDelete / CronList / ScheduleWakeup
```

## 二、文件操作三剑客

| 工具 | 用途 | 适用场景 |
|------|------|---------|
| **Read** | 读取文件内容，支持分页和 PDF/图片/Notebook | 查看配置文件 |
| **Write** | 创建新文件或完全重写已有文件 | 新建一个类、完全重构 |
| **Edit** | 基于字符串替换的精确编辑 | 加一个方法、变量重命名 |

> **重点**：Edit 的 `old_string` 必须唯一，`replace_all` 可替换全部出现。

## 三、搜索与发现

| 工具 | 能力 | 使用场景 |
|------|------|---------|
| **Grep** | 正则搜索文件内容（底层 ripgrep） | 查找代码中的特定模式 |
| **Glob** | 按 glob 模式匹配文件路径 | 查找文件位置 |

**最佳实践**：
1. 先 Grep 找代码 → Read 读文件 → Edit 改
2. 模糊搜索用 Agent Explore → 精确搜索用 Grep
3. 查文件在哪用 Glob → 查内容在哪用 Grep

## 四、Agent 工具

### 4.1 Agent（子代理）

启动一个独立的子代理处理复杂任务。支持 30+ 种子代理类型：

| 子代理类型 | 用途 |
|-----------|------|
| general-purpose | 通用代理 |
| Explore | 代码探索（只读） |
| Plan | 架构设计 |
| code-review | 代码审查 |
| feature-dev | 特性开发 |

### 4.2 Task（任务管理）

任务状态流转：`pending → in_progress → completed`

## 五、Plan 模式

```
EnterPlanMode → 探索代码 → 设计方案 → 写计划 → ExitPlanMode → 用户审核 → 批准后实现
```

## 六、工具选择决策树

```
要做什么？
├── 读文件 → Read
├── 新建文件 → Write
├── 修改部分 → Edit
├── 搜内容 → Grep
├── 找文件 → Glob
├── 跑命令 → Bash
├── 复杂多步任务 → Agent
├── 需要联网 → WebSearch / WebFetch
├── 架构设计 → EnterPlanMode
└── 审查代码 → /review
```

## 核心要点回顾

- Read + Write + Edit = 文件三剑客
- Grep + Glob = 搜索双星
- Bash = 命令行万能接口
- Agent = 多任务并行/上下文隔离
- Plan = 先想后做，降低返工
- Cron/Loop = 自动化定时/循环

## 参考资料

1. Anthropic 官方文档 - Claude Code Tools
2. Claude Code 工具参考 - Tool Schema 文档
