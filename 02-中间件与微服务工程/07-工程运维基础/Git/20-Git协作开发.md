# 20-Git协作开发
> 从单人开发到多团队协作的 Git 实操手册，覆盖分布式协作模型、PR/MR 全流程、冲突解决策略、Git Hooks 自动化，以及完整的团队协作 SOP。

## 目录
1. [Distributed Collaboration Models](#1-distributed-collaboration-models)
2. [Team Roles & Git Operations](#2-team-roles--git-operations)
3. [Branch Protection Rules](#3-branch-protection-rules)
4. [PR/MR Full Lifecycle](#4-prmr-full-lifecycle)
5. [Code Review with Git](#5-code-review-with-git)
6. [Conflict Resolution Collaboration Scenarios](#6-conflict-resolution-collaboration-scenarios)
7. [Parallel Development Strategies](#7-parallel-development-strategies)
8. [Commit Standards Enforcement](#8-commit-standards-enforcement)
9. [Git Hooks for Teams](#9-git-hooks-for-teams)
10. [Monorepo Collaboration](#10-monorepo-collaboration)
11. [Cross-Team Collaboration (Forking Workflow)](#11-cross-team-collaboration-forking-workflow)
12. [Emergency Fix Collaboration Flow](#12-emergency-fix-collaboration-flow)
13. [Complete Team Collaboration SOP](#13-complete-team-collaboration-sop)

---

## 1. Distributed Collaboration Models

Git 的分布式本质衍生出三种主流的协作模型，团队根据规模和场景选择适合的模型。

### 1.1 Centralized Workflow（SVN-Style via Git）

SVN 用户迁移到 Git 最自然的模型，所有开发者将代码推送到同一个中央仓库的同一个分支（通常是 `main`）。

```
          ┌─────────┐
          │  Remote  │
          │  origin  │
          │   main   │
          └────┬─────┘
         ┌─────┼─────┐
         │     │     │
    ┌────┴┐ ┌──┴──┐ ┌┴────┐
    │ DevA│ │DevB │ │DevC │
    └─────┘ └─────┘ └─────┘
```

| 特点 | 说明 |
|------|------|
| 适用场景 | 小型团队（2-5人）、快速原型、迁移过渡期 |
| 优点 | 简单，学习成本低 |
| 缺点 | 无隔离开发，冲突频繁，无代码审查环节 |
| 分支策略 | 所有人在 `main` 分支上工作 |
| 冲突频率 | 高，尤其文件密集修改时 |

```bash
# Centralized workflow 典型操作链
git pull                        # 拉取最新代码
# 修改代码...
git add .
git commit -m "feat: ..."
git pull                        # 推送前再拉取（解决潜在冲突）
git push                        # 推送
```

> ⚠️ Centralized workflow 不适合中大型项目。如果你在写 `git add . && git commit -m "update" && git push` 三连击，请尽快切换到 feature branch 工作流。

### 1.2 Integration-Manager Workflow（最主流，GitHub / GitLab 默认模式）

项目有一个"官方"仓库，维护者（Integration Manager）拥有该仓库的写入权限，贡献者通过 fork 或 feature branch 提交 PR/MR。

```
        Official Repo (upstream)
        ┌────────────────────┐
        │  Maintainer 管理    │
        │  main / develop     │
        └──────┬─────────────┘
               │ PR approved
        ┌──────┴─────────────┐
        │  Developer's Fork   │
        │  (or feature bran) │
        │  feature/xxx        │
        └────────────────────┘
               │
          ┌────┴────┐
          │ Developer│
          └─────────┘
```

| 特点 | 说明 |
|------|------|
| 适用场景 | 开源项目、中大型团队、需要代码审查的组织 |
| 优点 | 强代码质量管控、支持大规模协作 |
| 贡献方式 | Fork + PR（外部贡献）或 Feature Branch + MR（内部协作） |
| 代码审查 | 强制，所有合并请求需经过评审 |
| 权限控制 | 细粒度，仅 maintainer 有写权限 |

### 1.3 Dictator-Lieutenant Workflow（Linus Torvalds 的 Linux 内核模型）

大型项目（如 Linux Kernel）使用多层级维护者架构，每个 Lieutenant 负责一个子系统。

```
            Dictator (Linus)
                 │
        ┌────────┴────────┐
        │                 │
   Lieutenant A      Lieutenant B
   (networking)      (drivers)
        │                 │
    ┌───┤             ┌───┤
  DevA DevB          DevC DevD
```

| 特点 | 说明 |
|------|------|
| 适用场景 | 超大规模项目（Linux、Apache 基金项目） |
| 优点 | 分层管控，各子系统独立演进 |
| 缺点 | 流程复杂，合并周期长 |
| 层级 | 2-3 层维护者 |

> 🎯 **团队选型建议**: 小型团队（<5人）用 Centralized 过渡；中型团队（5-20人）用 Integration-Manager；大型项目（>50人）考虑多层维护者模型。90% 的企业团队应选择 Integration-Manager 模型。

---

## 2. Team Roles & Git Operations

一个健康的 Git 协作团队至少包含三个角色，每个角色有明确的操作职责。

### 2.1 Role Responsibility Matrix

| 角色 | Git 核心操作 | 职责范围 | 所需权限 |
|------|-------------|----------|----------|
| **Developer** | 创建 feature/hotfix 分支、提交代码、发起 PR/MR、解决冲突 | 功能开发、Bug 修复、单元测试 | Push 到 feature 分支 |
| **Reviewer** | 拉取 PR 分支到本地审查、添加行内评论、Approve/Request Changes | 代码质量、逻辑正确性、安全审计 | 读取仓库、评论权限 |
| **Maintainer** | 审核并合并 PR、管理分支保护规则、创建 Release 和 Tag | 分支权限、版本发布、合规审计 | 仓库管理权限 |

### 2.2 Developer 日常操作链

```bash
# 1. 从最新的 develop 创建功能分支
git checkout develop
git pull --rebase origin develop
git checkout -b feature/PROJ-123-user-profile

# 2. 开发过程中定期同步主分支
git fetch origin develop
git rebase origin/develop

# 3. 提交规范示例
git add src/main/java/com/xxx/service/ProfileService.java
git commit -m "feat(user-service): 新增用户资料修改接口 #PROJ-123"
git add src/test/java/com/xxx/service/ProfileServiceTest.java
git commit -m "test(user-service): 补充用户资料修改单元测试 #PROJ-123"

# 4. 推送并创建 PR
git push -u origin feature/PROJ-123-user-profile

# 5. 根据 Review 意见修改
git commit -m "fix: Review 意见-修复空指针检查 #PROJ-123"
git push
```

### 2.3 Reviewer 审查模式

```bash
# 方式一：在 GitHub/GitLab 网页审查（推荐初学者）

# 方式二：拉取 PR 分支到本地深入审查
git fetch origin pull/123/head:review/PROJ-123
git checkout review/PROJ-123

# 逐 commit 审查
git log --oneline
git diff commit1..commit2

# 审查代码变更
git diff develop..review/PROJ-123
git diff develop..review/PROJ-123 -- src/main/java/  # 只看业务代码

# 编译检查
mvn compile

# 审查完成后清理
git checkout develop
git branch -D review/PROJ-123
```

### 2.4 Maintainer 合并决策

```bash
# 合并方式选择
# 1. Create merge commit - 保留完整历史（适用于公共分支）
# 2. Squash and merge - 压缩为单个提交（适用于 feature 分支）
# 3. Rebase and merge - 线形历史（适用于需要干净历史的项目）

# 手动合并（不在平台操作时）
git checkout develop
git pull --rebase origin develop
git merge --no-ff feature/PROJ-123-user-profile  # --no-ff 强制保留分支历史
git push origin develop

# 或 squash merge
git merge --squash feature/PROJ-123-user-profile
git commit -m "feat(user-service): 新增用户资料功能 #PROJ-123"

# 推送标签
git tag -a v1.2.0 -m "Release v1.2.0: 新增用户资料功能"
git push origin v1.2.0
```

---

## 3. Branch Protection Rules

分支保护是 Git 协作中最重要的安全屏障，防止未经验证的代码进入核心分支。

### 3.1 GitHub / GitLab 保护规则配置

```text
Settings → Branches → Branch protection rules → Add rule

Branch name pattern: main (或 develop)
```

### 3.2 保护规则全景表

| 规则 | 说明 | 推荐级别 |
|------|------|----------|
| **Require a pull request before merging** | 禁止直接推送，所有修改必须通过 PR | ✅ 必选 |
| **Require approvals (>=1)** | 至少 N 人审查通过才可合并 | ✅ 必选（≥1） |
| **Dismiss stale pull request approvals** | 新推送后自动撤销旧审批 | ⚠️ 推荐 |
| **Require review from Code Owners** | 指定 CODEOWNERS 必须审查 | ✅ 推荐 |
| **Require status checks** | CI 必须通过（编译、测试、代码扫描） | ✅ 必选 |
| **Require branches to be up to date** | 合并前 feature 分支必须基于最新目标分支 | ✅ 推荐 |
| **Require conversation resolution** | 所有评论 resolved 后才能合并 | ⚠️ 推荐 |
| **Require signed commits** | 提交必须 GPG 签名 | ⚠️ 可选（高安全要求） |
| **Require linear history** | 禁止 merge commit，只允许 rebase | ⚠️ 可选 |
| **Include administrators** | 管理员也受保护规则约束 | ✅ 推荐 |
| **Restrict push access** | 仅指定角色可推送 | ✅ 可选（特定分支） |
| **Lock branch** | 完全禁止推送（冻结期使用） | ⚠️ 紧急/发布期间 |

### 3.3 推荐分支保护策略

```yaml
# 多分支保护配置推荐
main:
  require_pr: true
  require_approvals: 2                  # 生产分支需要 2 人审查
  require_status_checks: true
  include_administrators: true
  require_linear_history: true
  dismiss_stale_approvals: true

develop:
  require_pr: true
  require_approvals: 1                  # 开发分支 1 人审查
  require_status_checks: true
  require_branches_up_to_date: true

release/*:
  require_pr: true
  require_approvals: 2
  lock_branch_on_create: false

hotfix/*:
  require_pr: true
  require_approvals: 1                  # 紧急修复可放宽
  allow_force_push: false
```

> 💡 **GitHub 规则冲突优先级**: 更具体模式名的规则优先级高于通用模式。`release/v2.*` > `release/*` > `*`。

---

## 4. PR/MR Full Lifecycle

Pull Request（GitHub） / Merge Request（GitLab）是团队协作的核心载体，完整生命周期包括 7 个阶段。

### 4.1 完整流程时序图

```
Developer                  Remote Repo                 Reviewer
    │                         │                          │
    │── push feature branch ─→│                          │
    │                         │                          │
    │── Create Draft PR ─────→│                          │
    │                         │                          │
    │── Mark as Ready ───────→│── Request Reviewers ────→│
    │                         │                          │── Review Code
    │                         │                          │── Add Comments
    │                         │                          │
    │←── Address Comments ────│←── Request Changes ─────│
    │                         │                          │
    │── Push Updated Code ───→│                          │── Approve
    │                         │                          │
    │                         │←── Approve ─────────────│
    │                         │                          │
    │── Merge PR ────────────→│                          │
    │                         │── Delete Feature Branch  │
    │                         │                          │
    v                         v                          v
```

### 4.2 各阶段详细操作

**Stage 1: Draft PR（草稿阶段）**

```markdown
# 适用场景：功能开发中，但希望给团队预览方向

## GitHub 创建 Draft PR
gh pr create --draft --title "feat: 新增订单导出（开发中）" --body "初步实现，剩余单元测试待补充"

## 或在 GitHub Web 页面创建 PR 时选择 "Create draft pull request"

## Draft PR 特点
- 不会自动请求 Reviewers
- 不可合并
- CI 仍会触发（可在早期发现问题）
- 开发完成后点击 "Ready for review" 转为正式 PR
```

**Stage 2: Ready for Review / Request Reviewers**

```markdown
## PR 描述模板（团队统一）
---
### 关联需求
- JIRA: PROJ-456

### 变更内容
- 新增订单导出 CSV 接口（GET /api/orders/export）
- 支持按时间范围、订单状态过滤
- 导出文件限流（单用户 1 次/分钟）

### 改动清单
| 文件 | 变更类型 | 说明 |
|------|----------|------|
| OrderExportController.java | 新增 | 导出接口 |
| OrderExportService.java | 新增 | 导出核心逻辑 |
| OrderExportServiceTest.java | 新增 | 单元测试 |

### 测试情况
- [x] 单元测试通过
- [x] 接口自测通过
- [x] 边界情况（空数据、大数据量）已覆盖
- [ ] 集成测试（由 QA 验证）

### 自检清单
- [x] 代码规范（阿里巴巴 Java 开发手册）
- [x] SQL 性能（有索引、无 N+1）
- [x] 异常处理完整
- [ ] 是否需要更新 API 文档？
---

## Reviewers 分配
- 模块负责人: @zhang3
- 架构师: @li4 （涉及架构变更时）
```

**Stage 3: Address Comments**

```bash
# Developer 处理 review 意见
# 1. 根据评论修改代码
# 2. 对每条评论回复处理方式（Done / 说明原因不修改）

# 3. 提交修改
git commit -m "fix: 处理 Review 意见-优化 SQL 查询添加索引"
git push

# 4. 在 PR 中标记已解决的对话
# GitHub: "Resolve conversation"
```

**Stage 4: Approve & Merge**

```bash
# 合并前检查
# 1. 所有 conversation 已 resolved
# 2. 至少 N 人 approved（N 根据保护规则）
# 3. CI 全部通过
# 4. 分支无冲突

# 合并策略选择
# 小型功能（<5 commits）→ Squash and merge
# 中型功能（独立提交有意义）→ Rebase and merge
# 大型功能（需保留分支历史）→ Create a merge commit --no-ff
```

**Stage 5: Cleanup**

```bash
# 合并后（自动或手动）删除 feature 分支
git push origin --delete feature/PROJ-456-order-export  # 远程
git branch -d feature/PROJ-456-order-export             # 本地

# 同步目标分支
git checkout develop
git pull --rebase origin develop
```

> 💡 **PR 最佳实践**: 保持 PR 小而专注（200-400 行变更为佳）。过大的 PR 难以审查，容易遗漏问题。超过 1000 行的 PR 应当拆分。

---

## 5. Code Review with Git

代码审查是质量把控的核心环节，有多种审查方式可选。

### 5.1 审查方式对比

| 方式 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| Web UI 审查 | 快速审查、简单变更 | 方便、直观、无需切换环境 | 无法本地编译运行 |
| 本地 Pull 审查 | 复杂变更、需要运行验证 | 可编译运行测试、IDE 辅助 | 操作步骤较多 |
| 逐 Commit 审查 | 提交历史有意义的 PR | 理解开发演进过程 | 需要 commit 拆分合理 |
| 总 Diff 审查 | 功能简单、提交混乱的 PR | 一次性看到全部变更 | 无法理解变更顺序 |

### 5.2 本地 Pull 审查进阶

```bash
# 设置 checkpr alias
git config --global alias.checkpr '!f() { git fetch origin pull/$1/head:pr/$1 && git checkout pr/$1; }; f'

# 使用
git checkpr 123          # 拉取并切换到 PR #123

# 或使用 gh CLI
gh pr checkout 123

# 高级审查命令
# 查看 PR 中所有新增文件
git diff --diff-filter=A --name-only develop...HEAD

# 查看 PR 中所有修改文件
git diff --diff-filter=M --name-only develop...HEAD

# 查看特定模块的变化
git log develop..HEAD -- src/main/java/com/xxx/service/

# 逐文件审查
git diff develop..HEAD -- src/main/java/com/xxx/service/ExportService.java

# 审查 TODO / FIXME / DEBUG 残留
git diff develop..HEAD | grep -E '(TODO|FIXME|DEBUG|System\.out|print)'
```

### 5.3 Review Checklist

```markdown
## Java 后端 Review Checklist

### 业务逻辑
- [ ] 是否覆盖了所有需求场景？
- [ ] 边界条件处理完整？（null、空集合、超大值）
- [ ] 异常分支处理正确？（throw 或 fallback）

### 代码规范
- [ ] 遵循阿里巴巴 Java 开发手册？
- [ ] 命名规范（类名 PascalCase、方法 camelCase）
- [ ] 无魔法数字（使用常量/枚举）
- [ ] 日志级别使用正确？（debug/info/warn/error）

### 安全性
- [ ] 接口权限控制？（@PreAuthorize / 拦截器）
- [ ] 参数校验完整？（@Valid / 自定义校验）
- [ ] SQL 注入风险？（禁止拼接 SQL，使用 MyBatis/JPQL）
- [ ] 敏感信息脱敏？（密码、手机号、身份证）

### 性能
- [ ] SQL 有合适索引？
- [ ] 无 N+1 查询问题？
- [ ] 循环内无数据库调用？
- [ ] 批量操作使用了批量 API？

### 测试
- [ ] 单元测试覆盖核心逻辑？
- [ ] 测试包含正常路径和异常路径？
- [ ] 测试不依赖外部服务？
```

---

## 6. Conflict Resolution Collaboration Scenarios

冲突在团队协作中不可避免，关键在于建立规范的冲突解决流程。

### 6.1 冲突协作解决流程

```
场景：DevA 和 DevB 同时修改了 UserService.java 的 login 方法

步骤 1：沟通优先
  DevA 联系 DevB: "我们同时改了这个方法，我先合并你的改动？"

步骤 2：DevB 先合并（merge/rebase），解决冲突
  git checkout feature/devB-login
  git fetch origin develop
  git rebase origin/develop
  # 解决冲突...
  git add UserService.java
  git rebase --continue
  git push --force-with-lease

步骤 3：DevA 后合并（rebase），解决冲突
  git checkout feature/devA-login
  git fetch origin develop
  git rebase origin/develop
  # 此时 DevA 看到的冲突是 DevB 已经解决过一版的代码
  # 解决冲突...
  git add UserService.java
  git rebase --continue

步骤 4：双方验证
  mvn compile && mvn test
```

### 6.2 各类冲突处理策略

| 冲突类型 | 典型场景 | 解决策略 |
|----------|----------|----------|
| **逻辑冲突** | 两人同时修改同一方法的同一段逻辑 | 沟通确认意图，合并双方修改 |
| **新增文件冲突** | 两人创建了同名文件 | 沟通确认是否重复，必要时重命名 |
| **POM/配置冲突** | 同时修改 pom.xml 添加依赖 | 保留所有依赖，删除冲突标记 |
| **接口签名冲突** | 一人修改方法签名，另一人调用原方法 | 以新签名为准，更新调用方 |
| **删除 vs 修改** | A 删除了文件，B 修改了同一文件 | 确认删除意图，B 的修改可放弃 |

### 6.3 高级冲突解决工具

```bash
# 使用 git mergetool（配置可视化冲突解决）
git config --global merge.tool vscode
git config --global mergetool.vscode.cmd 'code --wait $MERGED'

# 或使用 IDEA 内置合并工具
git config --global merge.tool idea
git config --global mergetool.idea.cmd 'idea merge $LOCAL $REMOTE $BASE $MERGED'

# 当冲突过大时，选择直接使用一方
git checkout --ours UserService.java     # 保留本地版本
git checkout --theirs UserService.java   # 保留远程版本

# 查看冲突文件列表（按文件大小排序）
git diff --name-only --diff-filter=U | xargs wc -l | sort -n
```

---

## 7. Parallel Development Strategies

多人并行开发是 Git 协作的核心优势，但需要策略性地管理。

### 7.1 Feature Branching + Short-Lived Branches

```bash
# 核心原则：分支越小越好，生命周期越短越好

# 分支生命周期
git checkout -b feature/PROJ-456-user-export develop
# ... 1-3 天开发 ...
git push -u origin feature/PROJ-456-user-export
# 创建 PR → Review → Merge
git branch -d feature/PROJ-456-user-export

# 分支大小参考
# 小功能：1-2 天，<500 行变更
# 中功能：3-5 天，500-2000 行变更
# 大功能：>5 天，需要拆分为子功能分支
```

### 7.2 Frequent Rebase on Main

```bash
# 关键习惯：每日至少 rebase 一次 main/develop

# 早晨 start of day
git checkout feature/PROJ-456-user-export
git fetch origin develop
git rebase origin/develop

# 午饭后 midday
git fetch origin develop
git rebase origin/develop

# 创建 PR 前
git fetch origin develop
git rebase origin/develop
git push --force-with-lease
```

### 7.3 模块拆分策略（微服务场景）

| 策略 | 适用场景 | 冲突概率 | 说明 |
|------|----------|----------|------|
| 按服务拆分 | 微服务架构 | 低 | 每个服务独立仓库，天然隔离 |
| 按模块拆分 | 单体应用 | 中 | controller/service/dao 各司其职 |
| 按功能拆分 | 同一模块多人 | 高 | 需要频繁沟通协调 |
| 接口契约先行 | 前后端协作 | 低 | 先定接口 API，再并行开发 |

---

## 8. Commit Standards Enforcement

规范不落地等于没有规范。通过 Git Hooks 和 CI 实现自动化强制执行。

### 8.1 Conventional Commits 规范

```text
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

| Type | 含义 | 是否出现在 CHANGELOG |
|------|------|---------------------|
| `feat` | 新功能 | 是 |
| `fix` | Bug 修复 | 是 |
| `refactor` | 重构（非 fix 非 feat） | 否 |
| `perf` | 性能优化 | 否 |
| `test` | 测试相关 | 否 |
| `docs` | 文档修改 | 否 |
| `chore` | 构建、CI、依赖 | 否 |
| `style` | 格式修改（非逻辑改动） | 否 |

### 8.2 Pre-commit Hooks（Checkstyle / Lint）

```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/pre-commit/mirrors-eslint
    rev: v8.56.0
    hooks:
      - id: eslint
        args: ['--fix']
  - repo: local
    hooks:
      - id: maven-checkstyle
        name: Maven Checkstyle
        entry: mvn checkstyle:check
        language: system
        files: \.java$
        pass_filenames: false
      - id: no-debug-code
        name: No Debug Code
        entry: grep -rn 'System\.out\|\.printStackTrace\|e\.print()' --include='*.java' .
        language: system
        stages: [commit]
```

### 8.3 commit-msg Hook（Conventional Commits 校验）

```bash
#!/bin/sh
# .git/hooks/commit-msg

commit_msg_file="$1"
commit_msg=$(cat "$commit_msg_file")

# 正则校验：type(scope): description
pattern="^(feat|fix|refactor|perf|test|docs|chore|style|ci|build|revert)(\(.+\))?: .{1,100}$"

if ! echo "$commit_msg" | grep -qE "$pattern"; then
    echo "ERROR: Commit message must follow Conventional Commits format:"
    echo "  <type>(<scope>): <description>"
    echo "  Example: feat(user-service): 新增用户登录接口"
    exit 1
fi

# 校验 type 小写
type=$(echo "$commit_msg" | cut -d'(' -f1)
if [ "$type" != "$(echo "$type" | tr '[:upper:]' '[:lower:]')" ]; then
    echo "ERROR: Commit type must be lowercase"
    exit 1
fi

exit 0
```

### 8.4 CI Checks 全流程

```yaml
# .github/workflows/ci.yml (GitHub Actions)
name: CI
on: [pull_request]

jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Checkstyle
        run: mvn checkstyle:check

      - name: Unit Tests
        run: mvn test

      - name: SonarQube Scan
        run: mvn sonar:sonar

      - name: Check commit messages
        uses: wagoid/commitlint-github-action@v5
```

---

## 9. Git Hooks for Teams

Git Hooks 是团队自动化的基石，但需要仓库级共享才能发挥团队价值。

### 9.1 团队 Hook 管理方案

```bash
# 方案一：在项目中维护 hooks 目录（推荐）
project-root/
├── .githooks/                    # 存放共享 hooks
│   ├── pre-commit
│   ├── commit-msg
│   ├── pre-push
│   └── post-merge
├── .gitignore
└── ...

# 设置 core.hooksPath 指向共享 hooks 目录
git config core.hooksPath .githooks

# 或将此设置加入项目级配置
# 创建 setup.sh 脚本供新成员运行
#!/bin/bash
git config core.hooksPath .githooks
echo "Git hooks configured successfully!"
```

### 9.2 Hook 全景表

| Hook | 触发时机 | 用途 | 返回值影响 |
|------|----------|------|-----------|
| `pre-commit` | `git commit` 前 | 代码风格检查、语法校验、禁止调试代码 | 非零 → 终止提交 |
| `prepare-commit-msg` | 提交信息编辑前 | 自动填充模板 | — |
| `commit-msg` | 提交信息编辑后 | 校验提交格式（Conventional Commits） | 非零 → 终止提交 |
| `post-commit` | `git commit` 后 | 通知 CI、发送消息 | 不影响结果 |
| `pre-push` | `git push` 前 | 运行单元测试、安全检查 | 非零 → 终止推送 |
| `pre-receive` | 服务端接收推送前 | 校验分支权限、拒绝敏感文件 | 非零 → 拒绝推送 |
| `update` | 服务端更新分支前 | 分支粒度权限控制 | 非零 → 拒绝更新 |
| `post-receive` | 服务端接收推送后 | 触发部署、发送通知 | 不影响结果 |
| `post-merge` | `git merge` 后 | 自动安装依赖、数据库迁移 | — |
| `pre-auto-gc` | `git gc --auto` 前 | 阻止 gc 在特定时段执行 | 非零 → 跳过 gc |

### 9.3 团队 Hook 精选

**pre-commit: 禁止提交调试代码**

```bash
#!/bin/bash
# .githooks/pre-commit

FILES=$(git diff --cached --name-only --diff-filter=ACM)

# 检查 TODO / FIXME / DEBUG
if echo "$FILES" | xargs grep -ln 'TODO\|FIXME\|XXX' --include='*.java' 2>/dev/null; then
    echo "⚠️  WARNING: Files contain TODO/FIXME markers"
    echo "Please resolve them before committing, or use --no-verify to bypass"
    exit 1
fi

# 检查 System.out / printStackTrace
if echo "$FILES" | xargs grep -ln 'System\.out\.print\|\.printStackTrace()' --include='*.java' 2>/dev/null; then
    echo "❌  ERROR: Debug code detected (System.out / printStackTrace)"
    exit 1
fi

# Maven 编译（只 checkstyle，不运行全量测试）
mvn compile -q || { echo "❌  Compilation failed"; exit 1; }
```

**pre-push: 运行关键测试**

```bash
#!/bin/bash
# .githooks/pre-push

BRANCH=$(git rev-parse --abbrev-ref HEAD)

# 保护 main 和 develop 分支
if [ "$BRANCH" = "main" ] || [ "$BRANCH" = "develop" ]; then
    echo "❌  Cannot push directly to $BRANCH"
    echo "Please create a pull request instead."
    exit 1
fi

# 运行受影响模块的单元测试
echo "🔍  Running unit tests for changed modules..."
mvn test -pl $(git diff --name-only origin/develop...HEAD | cut -d'/' -f1-2 | sort -u | tr '\n' ',')
if [ $? -ne 0 ]; then
    echo "❌  Tests failed. Push aborted."
    exit 1
fi
```

**post-receive: 自动部署触发（服务端 Hook）**

```bash
#!/bin/bash
# 服务端 .git/hooks/post-receive

while read oldrev newrev refname; do
    BRANCH=$(git rev-parse --symbolic --abbrev-ref $refname)

    case $BRANCH in
        main)
            echo "🚀  Deploying to production..."
            ssh deploy@prod-server "cd /app && git pull && docker-compose up -d --build"
            ;;
        develop)
            echo "🧪  Deploying to staging..."
            ssh deploy@staging-server "cd /app && git pull && docker-compose up -d --build"
            ;;
        *)
            echo "ℹ️   Ignoring push to $BRANCH"
            ;;
    esac
done
```

---

## 10. Monorepo Collaboration

Monorepo（单体仓库）在大型项目中日益流行，多个团队共享一个仓库。

### 10.1 CODEOWNERS 文件

CODEOWNERS 定义文件或目录的所有者，PR 修改对应文件时会自动邀请所有者审查。

```text
# .github/CODEOWNERS

# 默认所有者
* @core-team

# 订单服务
order-service/** @order-team

# 支付服务
payment-service/** @payment-team

# 用户服务
user-service/** @user-team

# CI/CD 配置
.github/workflows/* @devops-team
.gitlab-ci.yml @devops-team

# 根目录配置文件（所有人都要经过 core-team 审查）
pom.xml @core-team
.gitignore @core-team
```

**CODEOWNERS 语法规则**:

| 语法 | 含义 |
|------|------|
| `*` | 匹配所有文件 |
| `*.java` | 匹配所有 Java 文件 |
| `docs/*` | 匹配 docs 目录下的直接子文件 |
| `docs/**` | 匹配 docs 目录下所有嵌套文件 |
| `@team` | 团队（GitHub Team） |
| `@user` | 个人 |
| `user-service/ @team @user` | 多个所有者（任一批准即可） |

> 💡 **CODEOWNERS 高级用法**: 在 GitHub 中开启 "Require review from Code Owners" 保护规则，可强制特定文件必须由其所有者审查。

### 10.2 Monorepo 分支策略

```
main
  └── develop
       ├── feature/order-service/PROJ-456
       ├── feature/payment-service/PROJ-457
       └── feature/user-service/PROJ-458
```

| 实践 | 说明 |
|------|------|
| **命名空间分支** | `feature/<service>/<ticket>` 避免分支名冲突 |
| **按目录限制 CI** | 只运行修改模块的测试和构建 |
| **独立部署** | 每个服务可独立脱离主分支发布 |
| **Owner 自动审查** | CODEOWNERS 确保正确团队审查变更 |

### 10.3 Monorepo 工具选型

| 工具 | 用途 | 适用场景 |
|------|------|----------|
| **Nx** | 构建系统 + 依赖图 | 前端 + 部分后端项目 |
| **Bazel** | 构建系统 | 超大型项目（Google 风格） |
| **Gradle** | 构建系统 | Java/Kotlin 多模块项目 |
| **Maven** | 构建系统 | Java 多模块项目 |
| **git-subrepo** | 子仓库管理 | 需要部分克隆的场景 |

---

## 11. Cross-Team Collaboration (Forking Workflow)

跨组织协作（如开源贡献、外包团队）时，Forking Workflow 是标准方案。

### 11.1 Forking 工作流

```bash
# 贡献者（Contributor）侧

# Step 1: Fork 上游仓库到自己的 GitHub 账号

# Step 2: Clone 自己的 fork
git clone https://github.com/contributor/project.git
cd project

# Step 3: 添加上游仓库
git remote add upstream https://github.com/original/project.git

# Step 4: 创建功能分支
git checkout -b feature/new-feature

# Step 5: 开发并提交
git add .
git commit -m "feat: 新增X功能"
git push -u origin feature/new-feature

# Step 6: 在自己的 fork 仓库创建 PR → 指向 upstream main

# Step 7: 同步上游更新（Review 期间）
git fetch upstream
git rebase upstream/main
git push --force-with-lease

# Step 8: PR 合并后清理
git checkout main
git fetch upstream
git rebase upstream/main
git push origin main
git branch -d feature/new-feature
```

### 11.2 Fork 同步自动化

```bash
# 通过 GitHub Actions 自动同步 fork
# .github/workflows/sync-fork.yml
name: Sync Fork
on:
  schedule:
    - cron: '0 6 * * *'  # 每天 UTC 6:00
  workflow_dispatch:

jobs:
  sync:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Sync upstream
        run: |
          git remote add upstream https://github.com/original/project.git
          git fetch upstream
          git checkout main
          git rebase upstream/main
          git push origin main
```

### 11.3 CLA（Contributor License Agreement）

大型开源项目要求贡献者签署 CLA，常见工具:

| 工具 | 集成平台 | 特点 |
|------|----------|------|
| **CLA Assistant** | GitHub | 自动检测 + 签名 UI |
| **EasyCLA** | Linux Foundation | 企业级 CLA 管理 |
| **DCO** | GitHub | 通过 `Signed-off-by` 行声明 |

```bash
# DCO (Developer Certificate of Origin)
# 每个提交末尾加上 Signed-off-by 行
git commit -s -m "feat: 新增功能

Signed-off-by: Zhang San <zhangsan@example.com>"

# 批量补签
git rebase -i HEAD~5 -x "git commit --amend --signoff --no-edit"
```

---

## 12. Emergency Fix Collaboration Flow

线上事故的修复需要快速响应，但质量管控不能完全丢弃。

### 12.1 Hotfix 协作流程

```bash
# Step 1: Maintainer 从 main 创建 hotfix 分支
git checkout main
git pull origin main
git checkout -b hotfix/v1.2.1-payment-timeout

# Step 2: 修复 Bug
# ... 修改代码 ...

git add payment-service/src/main/java/PaymentService.java
git commit -m "fix(payment): 修复支付超时未释放锁 Bug #BUG456"

# Step 3: 创建 Hotfix PR
# 目标分支：main
# 标签：hotfix
# 描述：紧急修复，需加速审批

# Step 4: 快速通道审批
# - Approvals 要求降为 1 人（紧急时可免）
# - 自动跳过非必要 CI 检查
# - Maintainer 直接合并

# Step 5: 部署
git tag -a v1.2.1 -m "Hotfix: 修复支付超时锁释放问题"
git push origin v1.2.1

# Step 6: Cherry-pick 回 develop / 其他活跃分支
git checkout develop
git pull origin develop
git cherry-pick a1b2c3d     # hotfix 提交的哈希
git push origin develop

# 如果有多个活跃分支
git checkout release/v1.1.x
git cherry-pick a1b2c3d
git push origin release/v1.1.x
```

### 12.2 Hotfix vs Feature Branch 区别

| 维度 | Feature Branch | Hotfix Branch |
|------|---------------|---------------|
| 来源分支 | develop | main（生产标签） |
| 目标分支 | develop | main → develop（需 cherry-pick） |
| 审批速度 | 常规（1-2天） | 加速（0.5-4小时） |
| Approvals 要求 | ≥2 | ≥1（或免审） |
| 上线窗口 | 按版本计划 | 立即上线 |
| 标签创建 | 版本发布时 | Hotfix 版本（修订号+1） |

---

## 13. Complete Team Collaboration SOP

### 13.1 New Member Onboarding（新成员入职流程）

```text
Day 1 - 环境准备
├── 安装 Git（v2.40+）
├── 配置 user.name / user.email
├── 生成 SSH Key 并添加到 Git 平台
├── 配置 GPG 签名（如有要求）
└── 克隆项目仓库

Day 2 - 规范学习
├── 阅读团队 Git 协作规范文档
├── 学习分支命名规范
├── 学习 Conventional Commits 格式
├── 配置 pre-commit hooks
└── 创建第一个 demo PR（由 Mentor 审查）

Day 3 - 独立贡献
├── 领取第一个小任务（Bug fix 或小功能）
├── 创建 feature 分支 → 开发 → 提交
├── 发起 PR → 请求审查
├── 根据 Review 意见修改
└── PR 合并 → 删除分支
```

### 13.2 Daily Collaboration Routine（日常协作节律）

```text
每日 Git 协作节律（推荐）

09:00 - 晨间同步
├── git checkout develop
├── git pull --rebase origin develop
└── 查看是否有待处理的 PR Review

09:30-11:30 - 上午开发
├── 在 feature 分支上开发
├── 每完成一个微任务：git commit
└── 复杂改动使用 git add -p 精细化提交

11:30-12:00 - 午间推送
└── git push（推送上午成果到远程）

14:00-14:15 - 午后同步
├── git fetch origin develop
├── git rebase origin/develop
└── 解决可能出现的冲突

14:15-16:30 - 下午开发
├── 继续开发
└── 提交新的改动

16:30-17:00 - 提交 PR
├── git rebase -i 整理提交历史
├── git push --force-with-lease
└── 在平台创建 PR / 将 Draft PR 转为 Ready

17:00-17:30 - Review 时间
├── 审查他人的 PR（至少审查 1 个）
├── 回复自己 PR 上的评论
└── 下班前确认所有分支已推送
```

### 13.3 Release Day Collaboration Flow（发布日协作流程）

```text
发布日 SOP

T-2 Days - 代码冻结
├── 所有 feature 分支合并到 develop
├── 创建 release/vX.Y.Z 分支
└── 仅允许 Bug fix 提交到 release 分支

T-1 Day - 测试验证
├── QA 在 release 分支上验证
├── 修复发现的 Bug（在 release 分支直接修复）
└── Prepare release notes

T Day - 发布日
├── 09:00 - Final QA sign-off
├── 10:00 - Merge release 到 main
├── 10:15 - 创建版本标签 vX.Y.Z
├── 10:30 - 部署到生产环境
├── 11:00 - 生产验证（线上 smoke test）
├── 11:30 - Merge release 回 develop（如有修复）
├── 12:00 - 删除 release 分支
└── 14:00 - Release retrospective meeting

发布后
├── 监控线上指标（1小时黄金观察期）
├── 准备 hotfix 分支（如有紧急问题）
└── 在 CHANGELOG.md 中记录本次发布
```

### 13.4 SOP 检查清单

```markdown
## 每日 Checklist
- [ ] 晨间拉取了 develop 最新代码
- [ ] feature 分支基于最新 develop
- [ ] 提交信息遵循 Conventional Commits
- [ ] 下班前推送了所有代码
- [ ] 处理了待审查的 PR

## PR Checklist
- [ ] 分支名符合规范
- [ ] 提交历史清晰（无 WIP commit、无 merge commit）
- [ ] CI 全部通过
- [ ] 至少 1 人已审查通过
- [ ] 所有 conversation 已 resolved
- [ ] 分支无冲突

## 发布 Checklist
- [ ] Release notes 已准备
- [ ] Tag 已创建
- [ ] CHANGELOG 已更新
- [ ] release 分支已合并到 main 和 develop
- [ ] 无用分支已清理
- [ ] 部署后 smoke test 通过
```

> 🎯 **协作的本质**: Git 协作开发并非关于复杂的命令，而是关于**约定、流程和沟通**。最好的 Git 协作流程是让团队感觉不到 Git 的存在 —— 规范内化为习惯，自动化消除了人为错误，PR 对话聚焦于代码而非流程问题。当一个团队做到"提交规范、审查高效、发布流畅"时，Git 协作就真正成为了团队效率的倍增器而非负担。
