# 02 - GitHub Pull Request 深度实战

> PR 是 GitHub 协作的核心机制——从创建到合并的全生命周期、Code Review 文化、最佳实践与进阶技巧。掌握 PR 等于掌握了开源协作的钥匙。

---

## 目录

1. [Pull Request 本质与生命周期](#1-pull-request-本质与生命周期)
2. [创建高质量 PR](#2-创建高质量-pr)
3. [Code Review 最佳实践](#3-code-review-最佳实践)
4. [PR 的合并策略与场景选择](#4-pr-的合并策略与场景选择)
5. [Draft PR 与渐进式开发](#5-draft-pr-与渐进式开发)
6. [跨仓库 PR（Fork 工作流）](#6-跨仓库-prfork-工作流)
7. [PR 冲突解决实战](#7-pr-冲突解决实战)
8. [PR 自动化与工具集成](#8-pr-自动化与工具集成)
9. [常见面试题](#9-常见面试题)

---

## 1. Pull Request 本质与生命周期

### 1.1 PR 是什么

> PR（Pull Request） = "请拉取我的代码"。本质是一个**请求**：把你的分支的变更合并到目标分支，并在此过程中进行代码审查、讨论和 CI 验证。

```
PR 完整生命周期：

  创建 PR  →  CI 自动检查  →  Code Review  →  修改代码  →  通过审查  →  合并  →  删除分支
     │            │               │               │             │           │         │
   New PR    Actions Run    Reviewers    New Commits   Approved    Merged    Deleted
                          Comment/Approve  Push到同一分支
```

### 1.2 PR 页面的关键区域

```
┌─────────────────────────────────────────────────────┐
│ PR Title: feat: add user login with JWT             │ ← 标题
│ #42 由 zhangsan 在 feature/login 分支合并到 main    │
├──────────────────────┬──────────────────────────────┤
│ Conversation 标签     │  Files Changed 标签           │
│                      │                              │
│ ● CI 状态（绿 ✅）    │  修改文件列表 + Diff 视图      │
│ ● Reviewers 审批状态  │  逐行评论 + 代码建议          │
│ ● 讨论线程            │                              │
│ ● 合并按钮            │                              │
├──────────────────────┴──────────────────────────────┤
│ 合并方式选择：                                        │
│   Create a merge commit                             │
│   Squash and merge                                  │
│   Rebase and merge                                  │
└─────────────────────────────────────────────────────┘
```

---

## 2. 创建高质量 PR

### 2.1 PR 标题规范

```bash
# ═══ Conventional Commits 风格（推荐）═══
feat: add user authentication module
fix: resolve NPE in OrderService
docs: update API documentation
refactor: extract validation logic to separate class
test: add integration tests for payment flow
chore: update dependency versions
perf: optimize database query in search endpoint

# ═══ 标题最佳实践 ═══
✅ 包含类型前缀（feat/fix/docs/refactor/test/chore）
✅ 使用英文祈使句（add / fix / update）
✅ 50 字符以内
✅ 首字母小写
❌ 不要用"修复了一个Bug"这种模糊描述
```

### 2.2 PR 描述模板

```markdown
## 📝 变更描述
实现了用户 JWT 登录功能，包括 access token 和 refresh token 的双 token 机制。

## 🎯 关联 Issue
Closes #35

## 📋 变更清单
- [x] 新增 JwtUtil 工具类（生成/验证 token）
- [x] 新增 /api/auth/login 接口
- [x] 新增 /api/auth/refresh 刷新 token 接口
- [x] 添加 RefreshToken 实体与持久化
- [x] 单元测试覆盖（JwtUtil + AuthService）

## 🧪 测试说明
- 运行 `mvn test` 全部通过
- 手动测试步骤：
  1. POST /api/auth/login → 获取 access_token + refresh_token
  2. GET /api/users（Header: Bearer {access_token}）→ 正常返回
  3. POST /api/auth/refresh → 获取新 access_token

## 📸 截图（如涉及 UI）
| 登录页 | 错误提示 |
|--------|---------|
| ![login](url) | ![error](url) |

## ⚠️ 注意事项
- 数据库新增 `refresh_tokens` 表，需要执行迁移脚本
- 环境变量新增 `JWT_SECRET`
```

### 2.3 PR 的大小控制

| PR 大小 | 代码行数 | Review 体验 | 建议 |
|---------|---------|------------|------|
| **XS** | < 50 行 | 秒审 | ✅ 最佳 |
| **S** | 50~200 行 | 几分钟 | ✅ 推荐 |
| **M** | 200~500 行 | 需投入时间 | ✅ 可接受 |
| **L** | 500~1000 行 | 费劲 | ⚠️ 尽量拆分 |
| **XL** | > 1000 行 | 灾难 | ❌ 必须拆分 |

> 💡 大功能拆分为多个小 PR：先发接口定义 → 再发实现 → 再发测试。用 Draft PR 或 Feature Flag 保证未完成功能不影响主分支。

---

## 3. Code Review 最佳实践

### 3.1 作者的责任（提 PR 的人）

```
✅ 提交前自查：
  1. 代码能编译/运行
  2. 所有测试通过
  3. 没有调试代码（console.log/System.out.println）
  4. 代码已格式化（符合团队规范）
  5. 提交信息清晰
  6. PR 描述完整

✅ Review 中：
  1. 对所有评论回复（即使只是 👍）
  2. 修改后标记对话为 Resolved
  3. 不要 force push（破坏 Review 上下文）
  4. 如需要，补充解释设计决策

❌ 不要：
  1. 提交 5000 行的 PR
  2. 忽略 Review 意见
  3. 不写测试
  4. 未经沟通就自行合并
```

### 3.2 Reviewer 的责任（审查代码的人）

```
✅ 审查时关注：
  1. 设计：整体方案是否合理？有没有更简单的方式？
  2. 功能：是否符合需求？边界条件处理了吗？
  3. 复杂度：有没有过度设计？能否更简洁？
  4. 测试：覆盖了关键路径吗？边界用例呢？
  5. 命名：变量/函数/类名是否清晰？
  6. 注释：是否需要额外注释？（代码自解释 > 注释）
  7. 安全：SQL注入、XSS、敏感信息泄漏？
  8. 性能：不必要的循环、N+1查询？

❌ 不要：
  1. 纠结代码风格（交给 Linter）
  2. "这样也行"的主观意见（标记为 nit/suggestion）
  3. 人身攻击/居高临下
  4. 阻塞PR等待非关键的修改

💡 Review 评论规范：
  [blocking] — 必须修改才能合并
  [suggestion] — 建议修改，不阻塞
  [nit] — 小细节，改不改都行
  [question] — 不理解，请解释
  [praise] — 👍 写得好！
```

### 3.3 代码建议（Suggested Change）

```markdown
<!-- GitHub 的代码建议功能 — 一键应用修改 -->

<!-- 在 Files Changed 中点击 +- 按钮，输入修改后代码： -->
```suggestion
public String formatName(String firstName, String lastName) {
    if (firstName == null && lastName == null) {
        return "Anonymous";
    }
    return String.join(" ", firstName, lastName);
}
```
<!-- 作者点击 "Commit suggestion" 即可自动应用此修改 -->
```

---

## 4. PR 的合并策略与场景选择

### 4.1 三种合并方式对比

| 策略 | 按钮文案 | Git 操作 | 历史形态 |
|------|---------|---------|---------|
| **Merge Commit** | Create a merge commit | `git merge --no-ff feature` | 分叉 + 合并节点 |
| **Squash and Merge** | Squash and merge | `git merge --squash feature` | 单 commit 线性 |
| **Rebase and Merge** | Rebase and merge | `git rebase main feature && git merge --ff-only feature` | 多 commit 线性 |

### 4.2 场景选择决策

```
个人项目 → Squash（历史干净，一个 PR = 一个 commit）

团队协作 → Merge Commit（保留完整提交历史，可追溯"谁在什么时候做了什么"）

开源项目 → 遵循社区约定（大多使用 Squash）

需要保留原子提交 → Rebase（每个 commit 都是独立可测试的）

CI/CD auto-deploy 依赖 commit → Rebase 或 Merge Commit（Squash 会丢失触发信息）
```

---

## 5. Draft PR 与渐进式开发

### 5.1 Draft PR 使用场景

```bash
# Draft PR — "我还在开发中，但想提前展示方案"

# ═══ 创建 Draft PR ═══
# 方式1：GitHub Web → Create Pull Request → 选 "Create draft pull request"
# 方式2：gh CLI
gh pr create --draft --title "feat: WIP user dashboard"

# ═══ 转正 Draft → Ready ═══
# GitHub Web → "Ready for review" 按钮
gh pr ready 42

# ═══ Draft PR 的特点 ═══
# ✅ 可以触发 CI（Actions 只跑 build/test，不部署）
# ✅ Reviewers 可以提前看代码给反馈
# ❌ 不能合并（Merge 按钮不可用）
# ❌ CODEOWNERS 不会自动请求 Review
```

### 5.2 何时用 Draft PR

| 场景 | 是否用 Draft | 原因 |
|------|-------------|------|
| 方案不确定，想提前讨论 | ✅ Draft | 明确表示"别合并，先看思路" |
| 功能完成 80%，等依赖 | ✅ Draft | 避免误合并 |
| 拆分为多个小 PR，先发第一个 | ❌ 正常 PR | 每个子 PR 独立完整可合并 |
| 完全完成，只是等 Review | ❌ 正常 PR | Draft 会让 Reviewer 犹豫 |

---

## 6. 跨仓库 PR（Fork 工作流）

### 6.1 给开源项目提 PR

```bash
# ═══ 完整流程 ═══

# 1. Fork 目标仓库（在 GitHub 网页上操作）
#    → https://github.com/original/repo
#    → Fork 到你自己的账号下
#    → https://github.com/yourname/repo

# 2. Clone 你的 Fork
git clone https://github.com/yourname/repo.git
cd repo

# 3. 添加 Upstream（原始仓库）
git remote add upstream https://github.com/original/repo.git
git remote -v
# origin    https://github.com/yourname/repo.git (fetch)
# origin    https://github.com/yourname/repo.git (push)
# upstream  https://github.com/original/repo.git (fetch)
# upstream  https://github.com/original/repo.git (push)

# 4. 同步 Upstream 最新代码⭐
git checkout main
git fetch upstream
git merge upstream/main
git push origin main

# 5. 创建 Feature 分支
git checkout -b fix/typo-in-readme

# 6. 开发 + Commit + Push
git add .
git commit -m "docs: fix typo in README"
git push origin fix/typo-in-readme

# 7. 在 GitHub 上发起 PR
#    从 yourname/repo:fix/typo-in-readme
#    到 original/repo:main
```

### 6.2 Allow Edits from Maintainers

> ⭐ 创建跨仓库 PR 时，勾选 **"Allow edits from maintainers"** —— 允许上游维护者直接修改你的 PR 分支。开源社区的标准做法。

---

## 7. PR 冲突解决实战

### 7.1 冲突的常见原因

```
场景1：两个 PR 修改了同一个文件的同一行
  PR#41: 修改 UserService.java L20 → 合并 → main 更新了
  PR#42: 也是从旧的 main 分支出来的 → 现在 main 和 PR#42 冲突了

场景2：配置文件冲突
  PR#41: application.yml 新增 redis 配置
  PR#42: application.yml 新增 kafka 配置 → 同位置冲突
```

### 7.2 解决冲突的步骤

```bash
# ═══ 方式一：通过命令行 ═══

# 1. 拉取最新 main
git checkout main
git pull origin main

# 2. 切回 feature 分支，rebase 或 merge
git checkout feature/mywork
git rebase main
# 或：git merge main

# 3. 解决冲突
# Git 会标记冲突文件：
# <<<<<<< HEAD
#    你的修改
# =======
#    main 上的修改
# >>>>>>> main

# 4. 标记已解决 + 继续
git add .
git rebase --continue  # 如果用 rebase
# 或 git commit（如果用 merge）

# 5. 推送到远端
git push origin feature/mywork --force-with-lease
# ⚠️ force push 会更新 PR 的提交历史，但 Review 评论会保留
```

```bash
# ═══ 方式二：通过 GitHub Web 编辑器 ═══
# GitHub PR 页面 → "Resolve conflicts" 按钮
# → Web 编辑器解决 → "Mark as resolved" → "Commit merge"
```

---

## 8. PR 自动化与工具集成

### 8.1 GitHub Actions 自动检查

```yaml
# .github/workflows/pr-checks.yml
name: PR Checks

on:
  pull_request:
    types: [opened, synchronize, reopened]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - run: mvn compile

  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - run: mvn test

  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: mvn checkstyle:check

  # 自动给 PR 添加 Label
  labeler:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/labeler@v4
        with:
          repo-token: ${{ secrets.GITHUB_TOKEN }}
```

### 8.2 自动分配 Reviewer

```yaml
# .github/auto_assign.yml
# 配合 Auto Assign Action：新建 PR 时自动分派 Reviewer
addReviewers: true
reviewers:
  - team-lead
  - senior-dev
numberOfReviewers: 2
```

### 8.3 Status Check 集成

```yaml
# PR 合并前必须通过的检查（在 Branch Protection 中配置）
#
# 常见集成（通过 GitHub Marketplace Apps）：
# ✅ GitHub Actions CI/CD
# ✅ Codecov（代码覆盖率）
# ✅ SonarCloud（代码质量门禁）
# ✅ Netlify / Vercel（预览部署）
# ✅ Renovate / Dependabot（依赖更新）
```

---

## 9. 常见面试题

### Q1：描述一下完整的 PR 工作流程？

> Fork → Clone → 创建 Feature 分支 → 开发+测试 → Push → 创建 PR（标题+描述+关联Issue）→ CI 检查 → Code Review → 修改 → Approve → Merge → 删除分支。详见第2节。

### Q2：Merge Commit、Squash、Rebase 合并有什么区别？

> Merge Commit 保留完整分支历史（有合并节点），Squash 将所有 commit 压为 1 个（历史干净），Rebase 保持线性历史（无合并节点但保留所有 commit）。详见第4节。

### Q3：Code Review 时应该关注什么？

> 设计正确性 > 功能完整性 > 测试覆盖 > 代码简洁性 > 命名规范 > 安全性 > 性能。不要纠结代码风格（交给工具），不要主观意见阻塞 PR。详见第3节。

### Q4：Draft PR 和普通 PR 的区别？

> Draft PR 表示"开发中，请勿合并"，可以提前展示代码获得早期反馈。转正后正常走 Review 流程。详见第5节。

### Q5：如何处理 PR 合并冲突？

> 方式一：本地 `rebase main` → 解决冲突 → `force push`。方式二：GitHub Web 编辑器直接解决。推荐方式一（可完整运行测试验证）。详见第7节。
