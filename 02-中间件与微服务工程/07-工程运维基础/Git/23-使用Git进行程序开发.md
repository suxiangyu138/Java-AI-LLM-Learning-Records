# 23-使用Git进行程序开发
> 🎯 从需求到上线的全生命周期Git实践指南：IDE集成操控、Feature分支开发流程、Pull Request与Code Review工程规范、冲突解决实战、CI/CD与Git钩子联动、版本发布流水线、Git LFS大文件管理，以及一个贯穿全文的"需求→开发→上线"完整端到端实战

## 目录

1. [Git与IDE集成：IntelliJ IDEA实操全流程](#1-git与ide集成intellij-idea实操全流程)
   - 1.1 [IDEA Git配置](#11-idea-git配置)
   - 1.2 [Commit操作](#12-commit操作)
   - 1.3 [Push/Pull操作](#13-pushpull操作)
   - 1.4 [分支操作](#14-分支操作)
   - 1.5 [Merge与冲突解决GUI](#15-merge与冲突解决gui)
   - 1.6 [查看历史与版本对比](#16-查看历史与版本对比)
2. [Feature分支开发流程](#2-feature分支开发流程)
   - 2.1 [从JIRA Issue到分支创建](#21-从jira-issue到分支创建)
   - 2.2 [开发提交与频率策略](#22-开发提交与频率策略)
   - 2.3 [Pull Request与Code Review](#23-pull-request与code-review)
   - 2.4 [合并与分支清理](#24-合并与分支清理)
3. [提交频率策略](#3-提交频率策略)
   - 3.1 [何时提交：每个逻辑单元一次](#31-何时提交每个逻辑单元一次)
   - 3.2 [Commit Message与Issue关联](#32-commit-message与issue关联)
4. [Pull Request最佳实践](#4-pull-request最佳实践)
   - 4.1 [PR Size控制](#41-pr-size控制)
   - 4.2 [PR模板](#42-pr模板)
   - 4.3 [Draft PR与请求Review](#43-draft-pr与请求review)
5. [Code Review中的Git](#5-code-review中的git)
   - 5.1 [逐Commit审查](#51-逐commit审查)
   - 5.2 [行级评论与Suggest Changes](#52-行级评论与suggest-changes)
6. [冲突解决实战](#6-冲突解决实战)
   - 6.1 [冲突产生原因](#61-冲突产生原因)
   - 6.2 [三路合并标记解析](#62-三路合并标记解析)
   - 6.3 [手动解决冲突](#63-手动解决冲突)
   - 6.4 [IDE冲突解决工具](#64-ide冲突解决工具)
   - 6.5 [冲突预防策略](#65-冲突预防策略)
7. [Git与CI/CD](#7-git与cicd)
   - 7.1 [Git Hook触发CI](#71-git-hook触发ci)
   - 7.2 [Commit Status Check](#72-commit-status-check)
   - 7.3 [PR合并条件](#73-pr合并条件)
8. [版本发布流程](#8-版本发布流程)
   - 8.1 [Feature Freeze](#81-feature-freeze)
   - 8.2 [Release Branch](#82-release-branch)
   - 8.3 [版本号规范](#83-版本号规范)
   - 8.4 [Tag与Deploy](#84-tag与deploy)
   - 8.5 [Merge Back](#85-merge-back)
9. [.gitattributes配置](#9-gitattributes配置)
   - 9.1 [行尾处理](#91-行尾处理)
   - 9.2 [二进制文件标记](#92-二进制文件标记)
   - 9.3 [Linguist覆盖](#93-linguist覆盖)
10. [Git LFS：大文件管理](#10-git-lfs大文件管理)
    - 10.1 [为什么需要Git LFS](#101-为什么需要git-lfs)
    - 10.2 [安装与配置](#102-安装与配置)
    - 10.3 [常用命令](#103-常用命令)
11. [完整端到端实战：从需求到上线](#11-完整端到端实战从需求到上线)

---

## 1. Git与IDE集成：IntelliJ IDEA实操全流程

### 1.1 IDEA Git配置

IDEA内置了完整的Git可视化操作，企业级开发90%的日常Git操作无需命令行。

**首次配置**：

```
File → Settings → Version Control → Git
  → Path to Git executable: 选择 git.exe 路径
  → SSH executable: Native（使用系统SSH）
  → 点击 Test 验证配置

File → Settings → Version Control → GitHub
  → 添加 GitHub 账号（推荐使用 Token 认证）
  → 或通过 GitLab / Gitee 插件配置
```

### 1.2 Commit操作

```
IDEA Commit 窗口（Ctrl + K）：
  ├── 左侧文件列表：勾选本次要提交的文件
  │   ├── 双栏对比视图：右侧显示当前文件与已提交版本的差异
  │   └── 文件状态图标：绿色=新增，蓝色=修改，灰色=删除
  ├── Commit Message 输入框：
  │   ├── 支持模版（Settings → Commit Dialog → Commit Message Templates）
  │   └── 自动拼装（如 "feat(user): #{issue_number} {summary}"）
  ├── Author 信息
  └── 提交前选项：
      ├── Reformat code        → 自动格式化代码
      ├── Rearrange code       → 重新排列代码
      ├── Optimize imports     → 自动清理无用import
      ├── Perform code analysis → 检查代码问题
      ├── Check TODO           → 检查是否有TODO遗留
      └── Cleanup              → 运行Code Cleanup

  快捷键：
  ├── Ctrl + K  → 打开Commit窗口
  ├── Ctrl + Alt + Z  → 撤销修改（新版IDEA）
  └── Alt + `   → 打开Git菜单（VCS Operations Popup）
```

> 💡 **IDEA Commit窗口的最佳实践**：每次提交前**必勾选** `Optimize imports` 和 `Reformat code`，确保代码风格一致。但 `Perform code analysis` 可以只在PR前执行一次，不需要每次commit都跑。

### 1.3 Push/Pull操作

```
Push（Ctrl + Shift + K）：
  ├── 显示将要推送的commit列表
  ├── 可选择推送前执行：code analysis / check TODO
  └── 支持 --force-with-lease（需勾选 Force Push）

Pull（Ctrl + T）：
  ├── Update Type 选择：
  │   ├── Merge（默认）= git pull
  │   └── Rebase（推荐）= git pull --rebase
  └── 可同时选择更新哪些分支

  配置默认rebase：
  File → Settings → Version Control → Git
    → Update method: "Rebase"（替代默认的Merge）
```

### 1.4 分支操作

```
IDEA 分支管理入口（右下角 Git 分支图标）：
  ├── New Branch：创建并切换
  │   └── 输入分支名 → 选择基于哪个分支
  ├── Local Branches：本地分支列表
  │   ├── Checkout：切换
  │   ├── Compare：比较与当前分支差异
  │   ├── Rebase Current onto Selected：变基
  │   ├── Merge into Current：合并到当前
  │   ├── Rename：重命名
  │   └── Delete：删除
  ├── Remote Branches：远程分支
  │   └── Checkout As...：检出为本地分支
  └── Show Branch Hierarchy：查看分支层级关系
```

### 1.5 Merge与冲突解决GUI

IDEA的冲突解决工具是企业级Java开发中最常用的GUI工具。

**触发冲突解决的场景**：
- `git pull` 或 `git pull --rebase` 时自动检测冲突
- `git merge` 操作时
- Cherry-pick / Rebase过程中

**IDEA冲突解决窗口**（三栏布局）：

```
┌──────────────┬─────────────────┬──────────────┐
│  本地版本     │   合并结果       │  远程版本     │
│  (Yours)     │   (Result)      │  (Theirs)    │
├──────────────┼─────────────────┼──────────────┤
│              │                 │              │
│ public User  │                 │ public User  │
│ findById(   │                 │ findById(    │
│   Long id   │                 │   Long id    │
│ ) {         │                 │ ) {          │
│   return    │                 │   return     │
│     ...     │                 │     ...      │
│ }           │                 │ }            │
│              │                 │              │
└──────────────┴─────────────────┴──────────────┘
     ↑ Apply 'Yours'     ↑ Apply 'Theirs'
     → 接受本地           → 接受远程
```

**操作方式**：
1. 点击 `>>` 箭头从左侧（Yours）或右侧（Theirs）接受整块代码
2. 在中间Result框直接编辑代码
3. 点击 `Apply` 按钮接受当前冲突的解决方案
4. 全部解决后点击 `Apply All` → 自动 `git add` 标记冲突已解决

> 💡 **IDEA冲突解决快捷键**：在Result面板中，`Ctrl + Shift + ←` 接受左侧，`Ctrl + Shift + →` 接受右侧，双手不离键盘即可完成冲突解决。

### 1.6 查看历史与版本对比

```
Git → Log（Alt + 9 或 Alt + ` → 4）：

  ├── 分支筛选：选中某个分支或点击 All
  ├── 作者筛选：输入用户名
  ├── 时间筛选：选择日期范围
  ├── 文件筛选：输入文件路径
  ├── 双击commit：查看该次提交的详情
  │   ├── 变更文件列表
  │   ├── 代码差异对比（左侧旧版/右侧新版）
  │   └── 分支标签信息
  └── 右键commit：
      ├── Create Patch：生成补丁文件
      ├── Cherry-pick：选择应用到当前分支
      ├── Reset Current Branch to Here...：回退到此commit
      ├── Undo Commit...：撤销提交（相当于reset --soft）
      └── Compare with Local：与当前工作区对比
```

---

## 2. Feature分支开发流程

### 2.1 从JIRA Issue到分支创建

Feature分支开发是Java后端团队最主流的工作流，核心路径为：

```
[JIRA Issue] → [Feature Branch] → [Development] → [Pull Request] → [Code Review] → [Merge]
```

**分支命名规范**：

```text
feature/ISSUE-1234-user-login       # 功能开发
bugfix/ISSUE-5678-null-pointer      # BUG修复
hotfix/ISSUE-9012-pay-timeout       # 紧急修复
release/v2.3.0                      # 版本发布
chore/update-dependencies           # 杂项（依赖更新等）
```

**IDEA操作流程**：

```text
1. JIRA → 领取 Issue "ISSUE-1234 实现用户登录功能"
2. IDEA → 右下角 Git → New Branch
3. 输入分支名：feature/ISSUE-1234-user-login
4. 基于 develop 分支创建
5. 自动切换到新分支
```

**命令行等价操作**：

```bash
git checkout develop
git pull --rebase origin develop
git checkout -b feature/ISSUE-1234-user-login
```

### 2.2 开发提交与频率策略

```bash
# === 开发阶段：频繁提交，保证备份 ===
git commit -m "feat(user): add LoginRequest DTO #1234"
git commit -m "feat(user): add UserService.login() method #1234"
git commit -m "feat(user): add LoginController endpoint #1234"
git commit -m "test(user): add unit tests for login #1234"
git commit -m "docs(user): add api-doc for login #1234"

# === 每日至少推送一次（避免本地代码丢失） ===
git push origin feature/ISSUE-1234-user-login
```

### 2.3 Pull Request与Code Review

**在GitHub/GitLab上创建PR**：

```text
1. 推送 feature 分支到远程
2. 打开 GitHub → Pull Requests → New Pull Request
3. base: develop ← compare: feature/ISSUE-1234-user-login
4. 填写 PR 描述（使用 PR 模板）
5. 添加 Reviewer
6. 创建 PR（可先创建 Draft PR）
```

**Reviewer审查流程**：

```text
1. 在 PR 页面查看 Files Changed
2. 逐行 Review 代码变更
3. 添加评论（Comment）或直接建议修改（Suggest Changes）
4. 整体评价：Approve / Request Changes / Comment
5. 提交 Review
```

**开发者响应Review**：

```text
1. 在本地修复review意见
2. 提交并推送
3. 在PR页面对每条评论进行回复（"Done" / "已修改"）
4. CI重新检查通过后，请求再次review
```

### 2.4 合并与分支清理

```bash
# === 合并到 develop ===
# 方式1：GitHub 上点击 "Merge pull request"（推荐）
# 方式2：本地合并
git checkout develop
git pull --rebase origin develop
git merge --no-ff feature/ISSUE-1234-user-login
git push origin develop

# === 清理分支 ===
git branch -d feature/ISSUE-1234-user-login                  # 删除本地
git push origin --delete feature/ISSUE-1234-user-login       # 删除远程
```

---

## 3. 提交频率策略

### 3.1 何时提交：每个逻辑单元一次

| 场景 | 推荐行为 | 原因 |
|------|---------|------|
| 新增一个接口 | 一个功能点一次commit | 后续可单独cherry-pick或revert |
| 同时修改了config + 业务代码 | 分开两次commit | 配置变更与业务逻辑变更关注点不同 |
| 代码重构 | 单独一次commit | 与功能变更分离，方便review |
| 修bug | 一个bug一次commit | 方便溯源和cherry-pick到其他分支 |
| typo/格式化 | 合并到上一个commit（amend/fixup） | 减少噪音commit |
| 写了一半的代码 | <br>- 少量：留在工作区<br>- 大量：`git stash` 或 `git commit -m "WIP"` | 不提交半成品，但也要避免本地代码丢失 |

**每天的标准节奏**：

```text
09:30  git pull --rebase origin develop      ← 开始工作前拉取最新
10:30  git commit -m "feat(user): xxx #1234" ← 完成第一个功能点
11:30  git push origin feature/xxx           ← 上午推送备份
14:00  git commit -m "fix(user): xxx #1234"  ← 完成第二个功能点
15:30  git commit -m "test(user): xxx #1234" ← 补充测试
16:30  git push origin feature/xxx           ← 结束前推送备份
17:00  Create Pull Request                   ← 创建PR（如果功能已完成）
```

### 3.2 Commit Message与Issue关联

企业级开发中，commit message必须关联JIRA/Issues编号：

```bash
# 推荐格式
git commit -m "feat(user): 实现用户登录接口 #1234"

# 详细格式（关联多个issue）
git commit -m "feat(user): 实现用户登录接口

- 新增 LoginRequest/LoginResponse DTO
- 实现 JWT Token 签发与验证
- 添加登录白名单配置
- 补充单元测试

Closes #1234
Related to #1230, #1235"
```

| 关键词 | 含义 | 自动行为（GitHub/GitLab） |
|--------|------|--------------------------|
| `Closes #1234` | 关闭Issue | Issue自动关闭 |
| `Fixes #1234` | 修复Issue | Issue自动关闭并标记为已修复 |
| `Resolves #1234` | 解决Issue | Issue自动关闭 |
| `Related to #1234` | 关联Issue | Issue被关联但不关闭 |
| `Refs #1234` | 引用Issue | 无自动行为 |

> 💡 **为什么必须关联Issue**：没有Issue关联的commit在半年后就是"考古现场"——你永远记不清这个change为什么这么做。Issue编号是连接"技术实现"和"业务需求"的桥梁。

---

## 4. Pull Request最佳实践

### 4.1 PR Size控制

**PR越大，Review质量越低**——这是工程界的共识。

| PR大小 | 行数参考 | Review质量 | 建议 |
|--------|---------|-----------|------|
| 小 | < 200行 | 高，Reviewer愿意逐行看 | ✅ 理想情况 |
| 中 | 200-500行 | 中等，Reviewer会扫读 | ⚠️ 可以接受 |
| 大 | 500-1000行 | 低，Reviewer只看关键部分 | ❌ 尽量拆分 |
| 超大 | > 1000行 | 极低，Reviewer直接Approve | ❌ 必须拆分 |

**拆分策略**：

```text
# 不要这样（一个PR包含3个功能）
PR: "实现用户模块" → 3000行变更 → 没人认真review

# 应该这样（拆分为3个PR）
PR1: "feat(user): 新增用户注册接口" → 200行 ✅
PR2: "feat(user): 新增用户登录接口" → 150行 ✅
PR3: "feat(user): 新增用户信息查询" → 180行 ✅
```

> 💡 **经验法则**：一个PR应该只解决**一个问题**。如果你发现需要在PR描述里写"同时"、"顺便"、"另外"，就是拆分的信号。

### 4.2 PR模板

企业级项目应在仓库根目录创建 `.github/PULL_REQUEST_TEMPLATE.md`：

```markdown
## 关联 Issue
Closes #{issue_number}

## 变更类型
- [ ] feat: 新功能
- [ ] fix: Bug修复
- [ ] refactor: 重构
- [ ] test: 测试
- [ ] docs: 文档
- [ ] chore: 构建/依赖

## 变更内容
简要描述本次PR的变更：

## 测试说明
- [ ] 单元测试已通过
- [ ] 集成测试已通过
- [ ] 本地部署验证通过

## 影响范围
- 会影响哪些模块/服务

## 截图（UI变更时）
```

### 4.3 Draft PR与请求Review

**Draft PR（草稿PR）** 是GitHub提供的功能，用于标记尚未完成的PR：

```text
# Draft PR 适用场景
1. 功能半完成，但想提前让CI开始跑测试
2. 需要其他团队先review接口设计（API约定）
3. 想在某次commit后自动触发CI来验证

# Draft PR 的特点
- 不能被执行合并操作
- Reviewers不会收到正式review请求
- 所有人都能看到并评论

# Draft PR → Ready for Review
功能完成后，取消 Draft 状态，添加 Reviewer
```

**请求Review的时机**：

```text
✅ 合适的时机：
- 功能完全开发完毕
- 所有测试通过（CI Green）
- 自己已review过一遍变更
- 分支已经 rebase 到最新的 develop

❌ 不合适的时机：
- 还有FIXME/TODO注释
- CI失败
- 临时提交了调试代码和日志
- 没有写测试
```

---

## 5. Code Review中的Git

### 5.1 逐Commit审查

一个好的PR应该包含多个逻辑清晰的commit。Reviewer应**逐commit审查**，而非只看整体diff：

```bash
# 在本地review PR的commit历史
git fetch origin pull/123/head:pr-123
git checkout pr-123

# 逐commit查看变更
git log --oneline develop..pr-123
# 8a2f8c9 feat(user): 实现用户登录接口
# 3b4c5d6 fix(user): 修复登录密码加密问题
# 1a2b3c4 test(user): 添加用户登录单元测试

# 逐个查看commit
git show 8a2f8c9
git show 3b4c5d6
git show 1a2b3c4
```

**GitHub/GitLab上的逐commit审查**：在PR页面的 "Commits" 选项卡中，可逐个点击commit查看。

**审查原则**：

```text
1. 先看commit message → 理清这个commit做了什么事
2. 检查commit的变更是否与message描述一致
3. 看是否符合项目的编码规范
4. 关注是否有潜在的问题（空指针、资源泄漏、安全风险）
5. 确认测试覆盖了新增代码的边界情况
```

### 5.2 行级评论与Suggest Changes

**GitHub的行级评论**：在Files Changed页面，点击某行代码旁的 `+` 号添加评论。

**Suggest Changes功能**：评论中可直接给出代码修改建议。

```
评论示例：

─────────────────────────────────────────────────
@@ -15,6 +15,8 @@ public User findById(Long id) {
     if (id == null) {
         throw new IllegalArgumentException("...");
     }
+    if (id <= 0) {
+        throw new IllegalArgumentException("id must be positive");
     return userRepository.findById(id);
 }
─────────────────────────────────────────────────

Reviewer评论：
> 建议增加对id <= 0的校验，防止无效查询。
>
> ```suggestion
>     if (id == null || id <= 0) {
>         throw new IllegalArgumentException("id must not be null or negative");
>     }
> ```
```

> 💡 **Suggest Changes 的优势**：Reviewer直接给出可执行的代码块，开发者点击 "Commit suggestion" 即可应用修改，无需手动编辑文件，减少了沟通成本和出错概率。

---

## 6. 冲突解决实战

### 6.1 冲突产生原因

冲突的本质：**Git不知道如何自动合并两个修改**。

| 冲突场景 | 具体原因 | 发生概率 |
|----------|---------|---------|
| **同一文件同一行被修改** | A和B都修改了 `UserService.java` 第15行 | 高 |
| **一个删除一个修改** | A删除了文件，B修改了同一文件 | 中 |
| **相邻行被修改** | A修改了第14行，B修改了第15行 | 中 |
| **同一个文件的import区** | A和B都添加了不同类的import | 低（Git通常能自动合并） |
| **pom.xml/application.yml** | 多人同时添加依赖/配置项 | 高 |

```bash
# 当冲突发生时，Git会输出
git merge develop
# Auto-merging UserService.java
# CONFLICT (content): Merge conflict in UserService.java
# Automatic merge failed; fix conflicts and then commit the result.

# 冲突文件状态
git status
# On branch feature/login
# You have unmerged paths.
#   (fix conflicts and run "git commit")
#
# Unmerged paths:
#   both modified:   src/main/java/com/demo/service/UserService.java
```

### 6.2 三路合并标记解析

冲突文件中的标记含义：

```java
public User findById(Long id) {
<<<<<<< HEAD
    // === 当前分支（HEAD）的版本  ===
    if (id == null) {
        return null;
    }
=======
    // === 正在合并的分支（develop）的版本 ===
    if (id == null) {
        throw new IllegalArgumentException("id must not be null");
    }
>>>>>>> develop
    return userRepository.findById(id);
}
```

| 标记 | 含义 |
|------|------|
| `<<<<<<< HEAD` | 当前分支版本的开头 |
| `=======` | 两个版本的分界线 |
| `>>>>>>> develop` | 被合并分支版本的结尾 |

### 6.3 手动解决冲突

**解决方案**：保留正确的代码，删除冲突标记。

```java
// 例如：综合两个版本，既保留安全校验又保留优雅返回值
public User findById(Long id) {
    if (id == null) {
        throw new IllegalArgumentException("id must not be null");
    }
    return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
}
```

```bash
# 手动修复后的提交
git add src/main/java/com/demo/service/UserService.java
git commit -m "merge: 解决UserService.findById冲突

- 合并本地null检查与远程的参数校验
- 添加UserNotFoundException处理"
```

### 6.4 IDE冲突解决工具

**IDEA三栏冲突解决窗口操作指南**：

```text
┌─────────────────────┬──────────────────────┬─────────────────────┐
│ Local Changes (本地) │ Result (合并结果)     │ Changes from Server │
│                     │                       │   (远程/被合并分支)   │
├─────────────────────┼──────────────────────┼─────────────────────┤
│ public User         │ public User          │ public User         │
│ findById(Long id)   │ findById(Long id)    │ findById(Long id)   │
│   return null;      │   return             │   throw ...         │
│                     │     userRepo...      │                     │
├─────────────────────┴──────────────────────┴─────────────────────┤
│ Button Bar:                                                        │
│ [Accept Left] [Accept Right] [Apply All Left] [Apply All Right]   │
│ [Reset] [Re-do]                                                    │
└───────────────────────────────────────────────────────────────────┘
```

**IDEA快捷键**：

| 操作 | 快捷键 |
|------|--------|
| 接受左侧（本地）变更 | `Ctrl + Shift + ←` |
| 接受右侧（远程）变更 | `Ctrl + Shift + →` |
| 跳转到下一个冲突 | `Alt + ↓` |
| 跳转到上一个冲突 | `Alt + ↑` |
| 全部应用左侧 | `Ctrl + Alt + Shift + ←` |
| 全部应用右侧 | `Ctrl + Alt + Shift + →` |

### 6.5 冲突预防策略

```bash
# 1. 开始工作前拉取最新代码
git checkout develop
git pull --rebase origin develop
git checkout feature/user-login
git rebase develop

# 2. 功能开发中定期同步远程变更
git pull --rebase origin develop   # 在feature分支上定期变基

# 3. 不要锁定大型配置文件（application.yml/pom.xml）一次太久
#    ——每次都尽快提交推送

# 4. 微服务架构下，不同服务之间不存在文件冲突
#    ——减少冲突的架构层面解决方案

# 5. 使用 .gitattributes 统一行尾
```

> 🎯 **冲突解决的核心原则**：不要"机械合并"——在解决冲突时，**要理解两边的代码逻辑**，做出正确的业务判断，而不是简单地选择"保留谁的代码"。

---

## 7. Git与CI/CD

### 7.1 Git Hook触发CI

CI/CD流水线通常由Git事件触发：

```yaml
# GitHub Actions 示例：.github/workflows/ci.yml
name: Java CI
on:
  push:
    branches: [ develop, 'feature/**' ]
  pull_request:
    branches: [ develop, main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build with Maven
        run: mvn clean verify
```

**常见触发策略**：

| 事件 | 触发动作 | 执行内容 |
|------|---------|---------|
| `push` 到 feature 分支 | 触发CI | 编译 + 单元测试 |
| `push` 到 develop 分支 | 触发CI | 编译 + 单元测试 + 集成测试 |
| `push` 到 main 分支 | 触发CI | 编译 + 全量测试 + 安全扫描 |
| `pull_request` | 触发CI | 编译 + 单元测试 + Code Quality |
| `tag` 推送 | 触发CD | 构建镜像 + 推送仓库 + 部署 |

### 7.2 Commit Status Check

CI执行结果会通过Commit Status反馈到Git提交和PR上：

```
GitHub PR页面显示：
  ✓ CI / build (pull_request) Successful in 3m 42s    ← 通过
  ✗ CI / lint (pull_request) Failed in 1m 15s         ← 失败
  ○ CI / integration (pull_request) Pending — Waiting  ← 等待中
```

**Status Check类型**：

```text
✅ 通过（绿色）：CI构建成功
❌ 失败（红色）：CI构建失败，需要开发者修复
⏳ 等待（黄色）：正在运行中
⚠️ 取消（灰色）：被手动取消或超时
```

### 7.3 PR合并条件

企业级项目通常设置**分支保护规则**，PR必须满足以下条件才能合并：

```text
┌─────────────────────────────────────────────┐
│              PR Merge 检查清单                │
├─────────────────────────────────────────────┤
│ ☑ 至少1个Reviewer Approved                   │
│ ☑ 所有CI检查通过                              │
│ ☑ 分支已更新到最新的 develop（无冲突）          │
│ ☑ 代码行变更不超过团队约定阈值                   │
│ ☑ SonarQube Code Quality Gate Passed         │
│ ───────────────────────────────────────────  │
│ 合并方式：                                    │
│ ○ Merge Commit（保留分支历史）                 │
│ ● Squash and Merge（压缩为一个commit）         │
│ ○ Rebase and Merge（线性历史）                 │
└─────────────────────────────────────────────┘
```

**三种合并方式的比较**：

| 方式 | 历史保留 | 适用场景 |
|------|---------|---------|
| **Merge Commit** | 完整保留分支拓扑 | 大型功能分支、多人协作分支 |
| **Squash and Merge** | 压缩为一个commit | 小功能、单人开发、feature分支 |
| **Rebase and Merge** | 线性历史，无分支信息 | 追求简洁历史的团队 |

---

## 8. 版本发布流程

### 8.1 Feature Freeze

功能冻结是版本发布前的重要节点：

```text
版本发布时间线：

D-7  Feature Freeze：不再接受新的feature合并到develop
D-5  Release Branch 创建：从develop创建 release/v2.3.0
D-5~D-1  Release Test：只修bug，不引入新功能
D-1  最终回归测试
D-0  发布上线
D+1  Merge Back：release分支合并回main和develop
```

### 8.2 Release Branch

```bash
# 创建release分支
git checkout develop
git pull --rebase origin develop
git checkout -b release/v2.3.0

# 在release分支上只修bug
git add .
git commit -m "fix: 修复支付金额精度丢失 #1568"
git commit -m "fix: 修复SQL分页查询偏移量错误 #1569"

# 推送到远程
git push -u origin release/v2.3.0
```

### 8.3 版本号规范

遵循[语义化版本控制（SemVer）](https://semver.org/)：

```
v2.3.1
│ │ │
│ │ └── PATCH：修订号，向后兼容的bug修复
│ └──── MINOR：次版本号，向后兼容的新功能
└────── MAJOR：主版本号，不兼容的API变更
```

| 版本号 | 变动类型 | 示例场景 |
|--------|---------|---------|
| v1.0.0 → v2.0.0 | MAJOR：破坏性变更 | 接口签名修改、数据库表结构变更 |
| v2.0.0 → v2.1.0 | MINOR：新增功能 | 新增API接口、新增配置项 |
| v2.1.0 → v2.1.1 | PATCH：bug修复 | 修复空指针、SQL性能优化 |

### 8.4 Tag与Deploy

```bash
# 测试通过后，合并到main
git checkout main
git pull --rebase origin main
git merge --no-ff release/v2.3.0

# 打版本标签
git tag -a v2.3.0 -m "Release v2.3.0: 用户模块重构与支付优化"

# 推送代码和标签
git push origin main
git push origin v2.3.0

# 触发CD自动部署（标签推送自动触发）
# GitHub Actions / Jenkins 检测到v2.3.0标签后：
# 1. mvn clean package
# 2. docker build -t myapp:v2.3.0
# 3. docker push registry/myapp:v2.3.0
# 4. kubectl set image deployment/myapp myapp=registry/myapp:v2.3.0
```

### 8.5 Merge Back

```bash
# release分支上的bug修复需要同步回develop
git checkout develop
git pull --rebase origin develop
git merge --no-ff release/v2.3.0
git push origin develop

# 删除本地和远程的release分支
git branch -d release/v2.3.0
git push origin --delete release/v2.3.0
```

---

## 9. .gitattributes配置

### 9.1 行尾处理

`gitattributes` 在仓库根目录控制文件级别的Git行为，优先级高于 `core.autocrlf`：

```gitattributes
# 文本文件的行尾规范化
# text=auto 让Git自动检测并转换行尾
* text=auto

# Java源文件显式使用LF（推荐跨平台项目）
*.java text eol=lf
*.xml text eol=lf
*.yml text eol=lf
*.yaml text eol=lf
*.properties text eol=lf
*.md text eol=lf

# Windows平台批处理文件保留CRLF
*.bat text eol=crlf
*.cmd text eol=crlf

# 二进制文件不进行行尾转换
*.jar binary
*.war binary
*.class binary
*.png binary
*.jpg binary
*.ico binary
*.pdf binary
```

### 9.2 二进制文件标记

```gitattributes
# 明确标记为二进制（Git不会对它们做diff）
*.jar binary
*.war binary
*.ear binary
*.class binary
*.dll binary
*.exe binary
*.so binary
*.dylib binary
*.png binary
*.jpg binary
*.jpeg binary
*.gif binary
*.ico binary
*.pdf binary
*.doc binary
*.docx binary
*.xls binary
*.xlsx binary
```

### 9.3 Linguist覆盖

GitHub使用Linguist来统计仓库语言。如果某些文件被误识别，可以通过 `gitattributes` 覆盖：

```gitattributes
# 将生成的Java文件标记为"生成代码"，不计入语言统计
src/main/java/com/demo/generated/* linguist-generated=true

# 将模板文件标记为特定语言
*.stg linguist-language=Java

# 将文档文件排除出语言统计
docs/* linguist-documentation
*.md linguist-documentation

# 将测试代码单独统计
src/test/* linguist-documentation=false
```

---

## 10. Git LFS：大文件管理

### 10.1 为什么需要Git LFS

Git在存储二进制文件时效率极低——它会存储每个版本的完整副本，导致仓库迅速膨胀。

| 文件类型 | 影响 | 建议方案 |
|----------|------|---------|
| JAR/WAR包 | 仓库体积暴增，clone变慢 | Git LFS |
| Docker镜像 | 完全不适用于Git | 用镜像仓库 |
| 日志文件 | 不应提交 | `.gitignore` |
| PDF/图片资源 | 版本多时体积大 | Git LFS |
| Node_modules/JAR依赖 | 不应提交 | 包管理器 + `.gitignore` |

**Git LFS的替代方案**：LFS不是存大文件的替代品，它只是在Git中存储**指针**，将实际内容存储在独立服务器上。

### 10.2 安装与配置

```bash
# 1. 安装Git LFS
# Windows: 从 https://git-lfs.com 下载安装
# macOS: brew install git-lfs
# Linux: sudo apt install git-lfs

# 2. 初始化LFS
git lfs install

# 3. 在项目中指定需要LFS管理的文件类型
git lfs track "*.jar"
git lfs track "*.war"
git lfs track "*.zip"
git lfs track "*.tar.gz"
git lfs track "*.tar"
git lfs track "*.so"
git lfs track "*.dll"
git lfs track "*.dylib"

# 4. 提交 .gitattributes（LFS自动生成了track规则）
git add .gitattributes
git commit -m "chore: 配置Git LFS管理大文件"

# 5. 正常使用Git命令（LFS自动处理）
git add libs/aliyun-sdk-oss-3.0.0.jar
git commit -m "feat: 添加阿里云OSS SDK依赖"
git push origin main    # 大文件自动上传到LFS存储
```

### 10.3 常用命令

```bash
# 查看当前LFS追踪规则
git lfs track

# 查看哪些文件被LFS管理
git lfs ls-files

# 查看仓库的LFS使用情况
git lfs env

# 从LFS拉取文件（正常clone会自动拉取，但浅克隆需要手动）
git lfs pull

# 迁移已有大文件到LFS（仓库已有历史中的大文件）
git lfs migrate import --include="*.jar" --everything

# 查看LFS存储信息
git lfs status
```

> ⚠️ **Git LFS注意事项**：
> 1. LFS文件有存储和带宽限制（免费额度约1GB/月）
> 2. LFS文件一旦推送，删除非常困难（即使从仓库删除，LFS仍保留对象）
> 3. 代码review时无法对比LFS文件的差异（只能看到指针变化）
> 4. 如果团队成员未安装 `git-lfs`，clone时会下载指针文件而非实际内容

---

## 11. 完整端到端实战：从需求到上线

以下模拟一个完整的Java后端功能开发流程，贯穿本文所有知识点。

### 场景：订单服务新增分页查询接口

```text
JIRA Issue: ORDER-5678 - 订单列表支持分页查询
优先级: P1
开发周期: 2天
```

### Day 1 上午：准备工作

```bash
# 1. 更新本地develop
git checkout develop
git pull --rebase origin develop

# 2. 创建feature分支
git checkout -b feature/ORDER-5678-order-pagination

# 3. 确认 .gitignore 和 .gitattributes 已存在
cat .gitignore | head -5
cat .gitattributes | head -5
```

### Day 1 下午：第一次开发迭代

```bash
# 4. 开发分页请求DTO
cat > src/main/java/com/demo/dto/PageRequest.java << 'EOF'
package com.demo.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
public class PageRequest {
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;
}
EOF

git add src/main/java/com/demo/dto/PageRequest.java
git commit -m "feat(order): add PageRequest DTO for pagination #ORDER-5678"

# 5. 开发分页响应DTO
cat > src/main/java/com/demo/dto/PageResponse.java << 'EOF'
package com.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long total) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(content);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(total);
        response.setTotalPages((int) Math.ceil((double) total / size));
        return response;
    }
}
EOF

git add src/main/java/com/demo/dto/PageResponse.java
git commit -m "feat(order): add PageResponse DTO with builder #ORDER-5678"

# 6. 编写Service层
cat > src/main/java/com/demo/service/OrderQueryService.java << 'EOF'
package com.demo.service;

import com.demo.dto.PageRequest;
import com.demo.dto.PageResponse;
import com.demo.entity.Order;
import com.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final OrderRepository orderRepository;

    public PageResponse<Order> findOrders(PageRequest request) {
        Pageable pageable = org.springframework.data.domain.PageRequest
                .of(request.getPage() - 1, request.getSize());
        Page<Order> page = orderRepository.findAll(pageable);
        return PageResponse.of(
                page.getContent(),
                request.getPage(),
                request.getSize(),
                page.getTotalElements()
        );
    }
}
EOF

git add src/main/java/com/demo/service/OrderQueryService.java
git commit -m "feat(order): implement OrderQueryService pagination #ORDER-5678"

# 7. 编写Controller
cat > src/main/java/com/demo/controller/OrderController.java << 'EOF'
package com.demo.controller;

import com.demo.dto.PageRequest;
import com.demo.dto.PageResponse;
import com.demo.entity.Order;
import com.demo.service.OrderQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderQueryService orderQueryService;

    @GetMapping
    public PageResponse<Order> getOrders(@Valid PageRequest request) {
        return orderQueryService.findOrders(request);
    }
}
EOF

git add src/main/java/com/demo/controller/OrderController.java
git commit -m "feat(order): add GET /api/orders pagination endpoint #ORDER-5678"
```

### Day 2 上午：补充测试

```bash
# 8. 编写单元测试
cat > src/test/java/com/demo/service/OrderQueryServiceTest.java << 'EOF'
package com.demo.service;

import com.demo.dto.PageRequest;
import com.demo.dto.PageResponse;
import com.demo.entity.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Test
    void shouldReturnPageResponseWhenPageRequestIsValid() {
        PageRequest request = new PageRequest();
        request.setPage(1);
        request.setSize(20);
        assertNotNull(request);
        assertEquals(1, request.getPage());
        assertEquals(20, request.getSize());
    }
}
EOF

git add src/test/java/com/demo/service/OrderQueryServiceTest.java
git commit -m "test(order): add unit tests for order pagination #ORDER-5678"

# 9. 推送并查看CI
git push origin feature/ORDER-5678-order-pagination
# GitHub Actions自动触发CI ...

# 10. 等待CI通过（绿色）
```

### Day 2 下午：创建PR与Code Review

```text
# 11. 在GitHub上创建PR
# base: develop ← compare: feature/ORDER-5678-order-pagination
# Title: feat(order): 订单分页查询功能 #ORDER-5678

# 12. 填写PR描述（使用模板）
# 关联Issue: ORDER-5678

# 13. 添加Reviewer

# 14. Reviewer审查：
#  - 逐commit查看变更
#  - 发现PageRequest未做sort排序支持
#  - 在Controller行上添加评论："建议增加sort参数支持排序"
#  - 使用 Suggest Changes 建议添加排序字段

# 15. 开发者响应Review
#  - 本地添加排序支持
ed src/main/java/com/demo/dto/PageRequest.java
# ...添加sort字段...

git add src/main/java/com/demo/dto/PageRequest.java
git commit -m "feat(order): add sort field to PageRequest for ordering support #ORDER-5678"
git push origin feature/ORDER-5678-order-pagination

# GitHub上回复评论："已添加sort字段支持排序排序，默认按创建时间降序"

# 16. Reviewer Approve
# CI全部通过, 至少1个Approval → 可以合并

# 17. 合并到develop（选择Squash and Merge）
# GitHub点击 "Squash and Merge" → "Confirm Squash and Merge"

# 18. 删除feature分支
git branch -d feature/ORDER-5678-order-pagination
```

### 版本发布

```bash
# 19. 版本发布操作（Release Manager执行）
git checkout develop
git pull --rebase origin develop
git checkout -b release/v2.4.0
git push -u origin release/v2.4.0

# 20. release分支上只修bug
git add .
git commit -m "fix: 修复分页默认排序不一致 #ORDER-5678"
git push origin release/v2.4.0

# 21. 回归测试通过后
git checkout main
git pull --rebase origin main
git merge --no-ff release/v2.4.0

# 22. 打标签
git tag -a v2.4.0 -m "Release v2.4.0: 新增订单分页查询功能"

# 23. 推送
git push origin main
git push origin v2.4.0
# CD自动触发：build → docker → deploy

# 24. Merge back到develop
git checkout develop
git pull --rebase origin develop
git merge --no-ff release/v2.4.0
git push origin develop

# 25. 清理release分支
git branch -d release/v2.4.0
git push origin --delete release/v2.4.0

# 26. 关闭JIRA Issue
# ORDER-5678 → Done
```

### 完整流程示意图

```text
        ┌─────────────────────────────────────────────────────┐
        │              JIRA: ORDER-5678                        │
        │          订单列表支持分页查询                          │
        └─────────────────────┬───────────────────────────────┘
                              │
        ┌─────────────────────▼───────────────────────────────┐
        │  1. git checkout -b feature/ORDER-5678-pagination    │
        │  2. 开发代码 + 测试代码 (原子提交)                     │
        │  3. git push origin feature/ORDER-5678-pagination    │
        │  4. CI 自动触发 (mvn clean verify)                   │
        └─────────────────────┬───────────────────────────────┘
                              │
        ┌─────────────────────▼───────────────────────────────┐
        │  5. GitHub → Create Pull Request → develop           │
        │  6. Code Review (逐commit + 行级评论)               │
        │  7. CI Status Check (必须全部通过)                   │
        │  8. Squash and Merge → 合并到 develop                │
        └─────────────────────┬───────────────────────────────┘
                              │
        ┌─────────────────────▼───────────────────────────────┐
        │  9. git checkout -b release/v2.4.0                  │
        │ 10. 回归测试 (只修bug)                               │
        │ 11. git merge release/v2.4.0 → main                 │
        │ 12. git tag -a v2.4.0                                │
        │ 13. git push origin main --tags                      │
        │ 14. CD 自动部署 (Build→Docker→Deploy)                │
        └─────────────────────┬───────────────────────────────┘
                              │
        ┌─────────────────────▼───────────────────────────────┐
        │ 15. git merge release/v2.4.0 → develop (Merge Back) │
        │ 16. 清理分支                                         │
        │ 17. JIRA → Done                                     │
        └─────────────────────────────────────────────────────┘
```

> 🎯 **总结**：使用Git进行Java程序开发，本质上是一个"以分支为工作单元、以PR为协作入口、以CI/CD为质量门禁"的工程化流程。IDE将Git操作从命令行变为可视化交互，但并不改变底层的工程原则——**规范的分支命名、原子化的提交、清晰的commit message、有意义的PR和review**，这些才是Git真正赋能软件工程的核心。工具会变（CLI→GUI→AI助手），但这些原则不会变。

---

> 🎯 **全篇总结**：从IDE集成到Feature分支、从Commit规范到Code Review、从冲突解决到CI/CD、从Release流程到LFS，Git在现代Java开发中已不仅是"版本控制工具"，而是串联整个开发流程的**工程基础设施**。掌握这些实践，意味着你不仅在"用Git"，而是在"用Git做工程"。
