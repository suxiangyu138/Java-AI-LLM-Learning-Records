# 03 - GitHub Issues 与项目管理

> Issues 不止是 Bug 追踪——结合 Labels、Milestones、Projects 和 Discussions，GitHub 提供了完整的项目管理能力。学会用 GitHub 管理软件开发全流程。

---

## 目录

1. [Issue 核心概念与最佳实践](#1-issue-核心概念与最佳实践)
2. [Labels 分类体系设计](#2-labels-分类体系设计)
3. [Milestones 里程碑管理](#3-milestones-里程碑管理)
4. [GitHub Projects 项目管理](#4-github-projects-项目管理)
5. [Issue 模板与自动化](#5-issue-模板与自动化)
6. [Discussions 社区讨论](#6-discussions-社区讨论)
7. [Issue 驱动的开发流程](#7-issue-驱动的开发流程)
8. [常见面试题](#8-常见面试题)

---

## 1. Issue 核心概念与最佳实践

### 1.1 Issue 的本质

> Issue = 任务单元。不限于 Bug 报告，也可以是功能请求、技术讨论、重构计划、文档需求——一切需要跟踪的工作。

```
Issue 的 5 个核心属性：
┌────────────────────────────────────────────┐
│ Title: 数据库连接池泄漏导致服务重启          │
│ Assignee: @zhangsan（负责人）               │
│ Labels: bug, high-priority, backend         │
│ Milestone: v2.1.0                           │
│ Project: Sprint 12 - Backlog               │
├────────────────────────────────────────────┤
│ Description: ...                           │
│ Comments: 讨论线程                          │
│ Linked PRs: #145 (修复此问题的 PR)          │
└────────────────────────────────────────────┘
```

### 1.2 写好一个 Issue

```markdown
<!-- ═══ Bug Report 最佳模板 ═══ -->

### 🐛 问题描述
用户列表接口 /api/users 在高并发下偶发返回 500 错误

### 🔄 复现步骤
1. 使用 JMeter 创建 100 线程并发
2. 持续请求 GET /api/users?page=1&size=50
3. 约 30 秒后开始出现 500 错误
4. 停止压测后错误消失

### ✅ 期望行为
所有请求正常返回 200 和用户列表数据

### 📊 环境信息
- OS: Ubuntu 22.04
- Java: 17.0.5
- DB: MySQL 8.0.32
- 分支: main @ commit abc123

### 📎 相关日志
```
2024-01-15 10:23:45 ERROR HikariPool - Connection is not available,
request timed out after 30000ms
```

### 🔗 关联
- 可能是 #34 的同样根因
```

---

## 2. Labels 分类体系设计

### 2.1 推荐的 Label 体系

```markdown
# ═══ 类型（Type）— 用颜色分类 ═══

🐛 bug              #FC2929 红色 — Bug 报告
✨ enhancement      #84B6EB 蓝色 — 功能增强
📚 documentation    #FEF2C0 黄色 — 文档
🧹 refactor         #D4C5F9 紫色 — 重构
🧪 test             #C5DEF5 浅蓝 — 测试
🔧 chore            #BFDADC 青色 — 工程杂务
❓ question         #D876E3 品红 — 疑问/讨论

# ═══ 优先级（Priority）═══

🔥 critical        #E11D21 深红 — 阻塞，立即修复
⚠️ high             #EB6420 橙色 — 本里程碑必须修
📌 medium           #FB135B0 黄绿 — 下个里程碑
⬇️ low              #0E8A16 绿色 — Nice to have

# ═══ 状态（Status）═══

📋 triage           #5319E7 紫色 — 待评估
✅ confirmed        #006B75 深青 — 已确认
🚧 in-progress      #E99695 粉色 — 进行中
🧊 blocked          #000000 黑色 — 被阻塞
🚫 wontfix          #FFFFFF 白色 — 不修复

# ═══ 模块（Module）═══

📦 frontend         #FFA07A 浅橙
🗄️ backend           #87CEEB 天蓝
🗃️ database          #FFD700 金色
🚀 devops            #D3D3D3 浅灰
📱 mobile            #98FB98 浅绿
🔒 security          #2F4F4F 深灰
```

### 2.2 Issue 的典型标签组合

```
Bug 报告：     🐛 bug + 🔥 critical + 🗄️ backend
功能请求：     ✨ enhancement + 📌 medium + 📦 frontend
文档更新：     📚 documentation + ⬇️ low
安全漏洞：     🐛 bug + 🔥 critical + 🔒 security
线上故障：     🐛 bug + 🔥 critical + 🧊 blocked（等修复环境）
```

---

## 3. Milestones 里程碑管理

### 3.1 Milestone 本质

> Milestone = 时间/目标边界。将一组 Issues/PRs 归入同一个交付目标，跟踪进度（完成百分比）。

```
基于版本的里程碑：
  v1.0.0 — MVP 最小可用版本
  v1.1.0 — 性能优化版本
  v2.0.0 — 重构版本

基于 Sprint 的里程碑：
  Sprint 1 — 用户系统
  Sprint 2 — 订单系统

Milestone 信息：
  Title: v1.0.0 - MVP Release
  Due Date: 2024-02-01
  Description: 第一个对外发布版本，包含核心功能
  Progress: 18 open / 12 closed = 40% 完成
```

### 3.2 Milestone 进度追踪

```bash
# 通过 gh CLI 查看 Milestone 进度
gh issue list --milestone "v1.0.0" --state open
gh issue list --milestone "v1.0.0" --state closed

# 通过 API 获取统计
curl -H "Authorization: token $GITHUB_TOKEN" \
  https://api.github.com/repos/user/repo/milestones/1
# 返回：{ "open_issues": 18, "closed_issues": 12, ... }
```

---

## 4. GitHub Projects 项目管理

### 4.1 Projects 三种视图

```
1. Board（看板视图）— 类似 Trello/Jira
   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
   │  Todo    │  │In Progress│ │ In Review│  │  Done    │
   │ □ #12    │  │ □ #15  👤 │  │ □ #18    │  │ ✅ #10   │
   │ □ #13 🔥 │  │ □ #16     │  │          │  │ ✅ #11   │
   └──────────┘  └──────────┘  └──────────┘  └──────────┘

2. Table（表格视图）— 类似 Excel
   | Title | Status     | Assignee | Priority | Milestone |
   |-------|-----------|----------|----------|-----------|
   | #12   | Todo      | zhangsan | High     | v1.0      |
   | #13   | In Progress| lisi    | Critical  | v1.0     |

3. Roadmap（路线图视图）— 按日期排列
   可设置 Start date / End date 字段，用甘特图展示
```

### 4.2 自定义字段

```yaml
# 除了默认的 Status，Projects 支持自定义字段：
Fields:
  - Status        # 单选项（Todo / In Progress / Done）
  - Priority      # 单选项（Critical / High / Medium / Low）
  - Size          # 单选项（XS / S / M / L / XL）
  - Effort        # 数字（估计工时）
  - Sprint        # 迭代（Sprint 1 / Sprint 2）
  - Start Date    # 日期
  - Target Date   # 日期
```

### 4.3 自动化规则

```yaml
# Projects 内置自动化（Workflows）：

# 1. Item added to project → Set field
#    Issue 添加到 Project 时 → Status 自动设为 "Todo"

# 2. Item closed → Archive / Move to "Done"
#    Issue 关闭时 → Status 自动设为 "Done"

# 3. Pull Request merged → Auto-close linked Issue
#    PR 合并后 → 关联的 Issue 自动关闭

# 4. Auto-add to project → 按 Label 过滤
#    打上 "bug" 标签 → 自动加入 Bug Triage 项目
```

---

## 5. Issue 模板与自动化

### 5.1 配置 Issue 模板

```yaml
# .github/ISSUE_TEMPLATE/config.yml — 模板配置
blank_issues_enabled: false  # 禁止空白 Issue，强制使用模板
contact_links:
  - name: 💬 社区讨论
    url: https://github.com/user/repo/discussions
    about: 使用问题请到 Discussions 提问
  - name: 📖 文档
    url: https://docs.example.com
    about: 请先查阅文档
```

```markdown
<!-- .github/ISSUE_TEMPLATE/bug_report.yml — YAML 表单模板 -->
name: 🐛 Bug 报告
description: 报告一个 Bug
title: "[Bug]: "
labels: ["bug", "triage"]
body:
  - type: textarea
    attributes:
      label: 🐛 问题描述
      description: 清晰描述你遇到的问题
    validations:
      required: true
  - type: textarea
    attributes:
      label: 🔄 复现步骤
      placeholder: |
        1. 打开...
        2. 点击...
        3. 看到错误...
  - type: dropdown
    attributes:
      label: 严重程度
      options:
        - 崩溃/无法使用
        - 功能受阻（有 workaround）
        - 体验不佳
        - 仅影响美观
  - type: input
    attributes:
      label: 版本号
      placeholder: e.g. v1.2.0
```

### 5.2 Issue 表单与 Markdown 模板选择

| 特性 | Issue Form (YAML) | Issue Template (Markdown) |
|------|------------------|--------------------------|
| 输入验证 | ✅ required 字段 | ❌ |
| 下拉选项 | ✅ dropdown | ❌ |
| 自动标签 | ✅ labels 字段 | ❌ |
| 自定义性 | 较高 | 最高（完全自由） |
| 学习成本 | 中 | 低 |
| **推荐** | 结构化项目 | 灵活的自由格式需求 |

---

## 6. Discussions 社区讨论

### 6.1 Discussions vs Issues

| 维度 | Issues | Discussions |
|------|--------|-------------|
| **目的** | 跟踪任务/缺陷 | 开放讨论 |
| **结构化** | 高（Labels/Milestones） | 低（Categories） |
| **关闭** | 任务完成后关闭 | 可标记为 Answered |
| **搜索** | 按 Labels 等过滤 | 按 Categories 过滤 |
| **典型场景** | Bug、Feature、Task | Q&A、公告、Ideas |

```
使用原则：
  ✅ 你知道"要做什么" → Issue
  ✅ 你在探索"怎么做/该不该做" → Discussion
  ✅ Discussion 讨论出结论 → 转为 Issue 执行
```

### 6.2 推荐的 Discussion 分类

```yaml
Categories:
  📢 Announcements    # 公告（仅维护者可发）
  💡 Ideas            # 功能想法和提案
  🙏 Q&A              # 问答（支持标记最佳答案）
  👏 Show and tell     # 社区展示
  💬 General          # 一般讨论
  📊 Polls            # 投票
```

---

## 7. Issue 驱动的开发流程

### 7.1 完整的开发闭环

```
        Idea / Bug 发现
              │
              ▼
    ┌── Create Issue ──┐
    │  添加 Labels/     │
    │  Milestone/Assign │
    └────────┬──────────┘
             │
             ▼
    ┌── Planning ──────┐
    │  加入 Project/    │
    │  Sprint Backlog   │
    └────────┬──────────┘
             │
             ▼
    ┌── Development ───┐
    │  Create Branch    │
    │  feat/42-login    │ ← Issue #42
    │  Commit + Push    │
    └────────┬──────────┘
             │
             ▼
    ┌── PR ────────────┐
    │  "Closes #42"     │
    │  Review → Merge   │
    └────────┬──────────┘
             │
             ▼
    ┌── Auto Close ────┐
    │  Issue #42 自动关闭│
    │  移到 Done 列     │
    └──────────────────┘
```

### 7.2 Issue 与 PR 的自动关联

```bash
# ═══ 关键字自动关闭 Issue ═══
# 在 PR 描述或 commit message 中使用：

Closes #42        # 合并后自动关闭
Fixes #42         # 同上
Resolves #42      # 同上

# 也可以用"所属仓库"语法：
Closes user/repo#42

# ═══ 仅关联（不关闭） ═══
Related to #42
See also #42
Ref #42
```

### 7.3 分支命名与 Issue 关联

```bash
# 推荐的分支命名（含 Issue 编号）
feat/42-user-login        # Issue #42
fix/99-npe-order-service   # Issue #99
docs/15-api-documentation   # Issue #15

# 好处：
# 1. 从分支名直接知道对应的 Issue
# 2. GitHub 自动关联分支和 Issue
# 3. PR 合并且分支删除后，Issue 上会留下交叉引用
```

---

## 8. 常见面试题

### Q1：GitHub Issues 和 Project 的区别？

> Issue 是单个任务/Bug/需求的原子单元；Project 是管理这些单元的看板/表格，提供进度可视化。一个 Project 可以关联多个仓库的 Issues。详见第4节。

### Q2：如何设计一套好的 Labels 体系？

> 按三个维度：类型（bug/enhancement/documentation）、优先级（critical/high/medium/low）、模块（frontend/backend/database）。保持简洁，通常 15-25 个 Label 即可。详见第2节。

### Q3：Issue 和 Discussion 各自适用什么场景？

> Issue 适用已明确的任务（Bug、Feature、Task），Discussion 适用开放讨论（Q&A、Ideas、公告）。讨论出结论后转为 Issue 执行。详见第6节。

### Q4：如何实现 Issue → PR → 自动关闭的闭环？

> 创建 Issue → 基于 Issue 编号创建分支 → 开发 Push → 创建 PR（描述中包含 `Closes #42`）→ PR 合并后 Issue 自动关闭。详见第7节。
