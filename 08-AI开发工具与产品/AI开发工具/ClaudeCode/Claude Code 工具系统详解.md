# Claude Code 工具系统详解（实战版）

> **文档定位**：Claude Code CLI 工具集深度解析
> **核心问题**：Claude Code 有哪些内置工具？各工具怎么用？如何优化工具选择？

---

## 一、工具全景图

Claude Code 内置了约 30 个工具，分为以下几类：

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

---

## 二、文件操作三剑客

### 2.1 Read — 读取文件

```
用途：读取文件内容，支持分页
何时用：需要查看文件内容时
特点：
  - 支持 PDF（分页读取）
  - 支持图片（多模态）
  - 支持 Jupyter Notebook
  - 从 Cat -n 格式输出（带行号）
```

### 2.2 Write — 创建/重写文件

```
用途：创建新文件或完全重写已有文件
何时用：新建文件、完全替换内容
特点：
  - 必须先 Read 过才能重写（安全机制）
  - 自动覆盖（有确认机制）
```

### 2.3 Edit — 精确编辑

```
用途：基于字符串替换的精确编辑
何时用：修改文件中的部分内容
特点：
  - old_string 必须唯一（否则报错）
  - replace_all 可替换全部出现
  - 保留精确缩进
```

| 场景 | 工具选择 |
|---|---|
| 看配置文件 | Read |
| 新建一个类 | Write |
| 加一个方法 | Edit |
| 变量重命名 | Edit (replace_all) |
| 完全重构一个文件 | Write |

---

## 三、搜索与发现

### 3.1 Grep — 内容搜索（最常用）

```
底层：ripgrep
能力：正则搜索文件内容
输出：files_with_matches / content / count
高级：-A/-B/-C 上下文行、multiline、head_limit
```

### 3.2 Glob — 文件名搜索

```
能力：按 glob 模式匹配文件路径
高级：按修改时间排序
```

### 3.3 搜索最佳实践

```
1. 先 Grep 找代码 → Read 读文件 → Edit 改
2. 模糊搜索用 Agent Explore → 精确搜索用 Grep
3. 查文件在哪用 Glob → 查内容在哪用 Grep
```

---

## 四、执行工具

### 4.1 Bash — Shell 命令执行

```
用途：运行任何 Shell 命令
安全：默认需要用户确认
最佳实践：
  - Git 操作用 Bash("git ...")
  - 构建/测试用 Bash("npm test")
  - 避免 Bash("rm -rf") 在权限中 deny
```

### 4.2 PowerShell — Windows 命令

```
用途：Windows PowerShell 命令
说明：与 Bash 互斥使用
```

---

## 五、Agent 工具

### 5.1 Agent（子代理）

```
用途：启动一个独立的子代理处理复杂任务
子代理类型：
  - general-purpose：通用代理
  - Explore：代码探索（只读）
  - Plan：架构设计
  - claude-code-guide：Claude Code 文档问答
  - code-review：代码审查
  - feature-dev:code-explorer：特性探索
  - ...30+ 种专门代理

使用场景：
  - 并行任务（多个 Agent 同时跑）
  - 上下文隔离（Agent 独立上下文窗口）
  - 专门能力（用对应类型的 Agent）
```

### 5.2 Task — 任务管理

```
TaskCreate：创建任务
TaskUpdate：更新任务状态
TaskList：列出所有任务
TaskGet：获取任务详情

任务状态流转：
pending → in_progress → completed
```

---

## 六、Plan 模式工具

```
EnterPlanMode → 进入规划模式
ExitPlanMode → 提交计划待审批

流程：
1. 用户说"帮我做一个功能"
2. AI 调用 EnterPlanMode
3. 探索代码 → 设计方案 → 写计划
4. 调用 ExitPlanMode → 用户审核
5. 用户批准 → 开始实现
```

---

## 七、Web 工具

### 7.1 WebSearch

```
能力：联网搜索最新信息
用途：查文档、最新技术、bug 解决方案
注意：结果提供 Sources 链接
```

### 7.2 WebFetch

```
能力：抓取指定 URL 内容并分析
用途：查看官方文档、技术博客
限制：无法访问需要认证的页面
```

---

## 八、交互工具

### 8.1 AskUserQuestion

```
用途：向用户提问（多选/单选）
场景：
  - 不确定技术选型时
  - 需要用户偏好时
  - 多个方案需要决策时
```

### 8.2 PushNotification

```
用途：推送桌面通知
场景：后台任务完成、重要事件提醒
```

---

## 九、定时与循环

### 9.1 Cron — 定时任务

```
CronCreate：创建定时任务
CronDelete：删除定时任务
CronList：列出所有定时任务

支持：
  - 一次性任务（remind me at 3pm）
  - 重复任务（every 5 minutes）
  - 续存任务（durable: true 跨 session）
```

### 9.2 Loop — 循环执行

```
/loop 5m /check-deploy    → 每 5 分钟检查部署状态
/loop                      → 自定节奏循环
```

---

## 十、高级工具

### 10.1 Skill

```
调用已注册的 Skill（技能/插件）

示例：
  /review → 代码审查
  /commit → 创建提交
  /init → 初始化项目
```

### 10.2 Worktree

```
EnterWorktree：创建 Git 工作树（隔离环境）
ExitWorktree：退出工作树（保留或清理）

用途：并行开发、PR 审查、实验性修改
```

### 10.3 MCP 集成工具

```
通过 MCP 插件获得的外部工具：
  - mcp__ide__executeCode：执行 Jupyter 代码
  - mcp__ide__getDiagnostics：获取 IDE 诊断
  - mcp__plugin_context7__query-docs：查询框架文档
  - ...（取决于安装了哪些 MCP Server）
```

---

## 十一、工具选择决策树

```
要做什么？
├── 读文件 → Read
├── 新建文件 → Write
├── 修改部分 → Edit（优于 Bash sed）
├── 搜内容 → Grep
├── 找文件 → Glob
├── 跑命令 → Bash
├── 复杂多步任务 → Agent(subagent_type=Explore)
├── 需要联网 → WebSearch / WebFetch
├── 架构设计 → EnterPlanMode
├── 审查代码 → Bash("git diff") + Agent(subagent_type=code-review)
└── 提交代码 → Bash("git commit ...")
```

---

## 十二、极简总结

```
Read + Write + Edit = 文件三剑客
Grep + Glob = 搜索双星
Bash = 命令行万能接口
Agent = 多任务并行/上下文隔离
Plan = 先想后做，降低返工
Cron/Loop = 自动化定时/循环
MCP Tools = 生态扩展入口
```
