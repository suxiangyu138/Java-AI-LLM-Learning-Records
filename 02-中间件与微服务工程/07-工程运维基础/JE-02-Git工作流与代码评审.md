# 02-Git工作流与代码评审 -- Git Workflow & Code Review

> **从"git push 完事"到"规范的分支策略与工程化评审流程"**

---

## 目录

1. [Git 核心原理回顾](#1-git-核心原理回顾)
2. [分支策略](#2-分支策略)
3. [高级 Git 操作](#3-高级-git-操作)
4. [提交约定（Conventional Commits）](#4-提交约定conventional-commits)
5. [代码评审最佳实践](#5-代码评审最佳实践)
6. [Pull Request 工作流](#6-pull-request-工作流)
7. [Java 项目 .gitignore 最佳实践](#7-java-项目-gitignore-最佳实践)
8. [Monorepo vs Polyrepo](#8-monorepo-vs-polyrepo)
9. [GitOps 简介](#9-gitops-简介)
10. [面试高频题](#10-面试高频题)

---

## 1. Git 核心原理回顾

### 1.1 Git 对象模型

Git 是一个**内容寻址文件系统**，核心是键值对存储：

```
对象类型:
├── blob    — 文件内容（二进制大对象）
├── tree    — 目录结构（包含文件名、权限和指向 blob/tree 的指针）
├── commit  — 快照（指向 tree、parent commit、作者、提交信息）
└── tag     — 指向特定 commit 的命名引用
```

**对象关系图：**

```
commit (sha1: a1b2c3d4)
├── tree (sha1: e5f6g7h8)
│   ├── "README.md" → blob (sha1: i9j0k1l2)
│   ├── "src/" → tree (sha1: m3n4o5p6)
│   │   ├── "main/java/com/company/App.java" → blob (sha1: q7r8s9t0)
│   │   └── "test/java/com/company/AppTest.java" → blob (sha1: u1v2w3x4)
│   └── "pom.xml" → blob (sha1: y5z6a7b8)
├── parent → commit (sha1: c8d9e0f1)
├── author: User <user@company.com>
├── committer: User <user@company.com>
└── message: "feat: add user registration module"
```

**查看对象：**

```bash
# 查看 commit 的详细信息
git show a1b2c3d4

# 查看 tree 对象
git ls-tree e5f6g7h8

# 查看 blob 内容
git show i9j0k1l2

# 查看对象的类型
git cat-file -t a1b2c3d4  # commit / tree / blob / tag
```

### 1.2 Git 三个区域

```
Working Directory         Staging Area               Repository (.git)
   (工作目录)          (暂存区/Index)           (仓库/历史记录)
       │                      │                          │
       │     git add          │       git commit          │
       ├─────────────────────►├─────────────────────────►│
       │                      │                          │
       │◄──── git checkout ───┼────── git checkout ──────┤
       │                      │                          │
       │◄─── git restore ─────┤                          │
       │                      │                          │
       │◄──── git reset ──────┼────── git reset ────────►│
```

**常用操作的工作区变化：**

| 命令 | 工作区 | 暂存区 | 仓库 |
|------|--------|--------|------|
| `git status` | 检查差异 | 检查差异 | - |
| `git add <file>` | - | 更新 | - |
| `git commit` | - | - | 新增 commit |
| `git reset HEAD <file>` | - | 撤销 | - |
| `git checkout -- <file>` | 还原 | - | - |
| `git reset --hard HEAD` | 还原 | 还原 | - |
| `git amend` | - | - | 修改最新 commit |

### 1.3 分支是指向 Commit 的指针

```bash
# 分支本质上是一个指针文件
cat .git/refs/heads/main
# 输出: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t

# HEAD 是一个特殊指针，指向当前分支
cat .git/HEAD
# 输出: ref: refs/heads/main

# 分离 HEAD 状态
git checkout a1b2c3d4  # HEAD 指向 commit 而非分支
```

### 1.4 合并策略

#### Fast-Forward 合并

```bash
# 条件：目标分支是源分支的直接后继
# 结果：直接移动指针，不产生 merge commit
git merge --ff feature/login    # 默认行为，如果能 ff 就 ff
git merge --ff-only feature/login  # 如果不能 ff 就失败

# Before:
# main:     A---B---C
# feature:        \
#                  D---E
#
# After (FF):
# main:     A---B---C---D---E
```

#### Recursive 合并（默认的三路合并）

```bash
# 当分支产生分叉时，使用三路合并
# 结果：产生一个新的 merge commit
git merge feature/login

# Before:
# main:     A---B---C---F
# feature:        \---D---E
#
# After:
# main:     A---B---C---F---G (merge commit)
# feature:        \---D---E /
```

#### 其他合并策略

```bash
# Octopus 合并（合并多个分支）
git merge feature/1 feature/2 feature/3

# Ours 策略（完全保留当前分支内容）
git merge -s ours feature/other

# Subtree 策略
git merge -s subtree feature/subtree
```

**Squash 合并：**

```bash
# 将 feature 分支的所有变更压缩为一个 commit
git merge --squash feature/login
git commit -m "feat: add login functionality"

# Before:
# main:     A---B---C
# feature:        \---D---E---F
#
# After (squash):
# main:     A---B---C---G (squash commit containing D+E+F)
```

**Rebase 合并（线性历史）：**

```bash
git checkout feature/login
git rebase main

# Before:
# main:     A---B---C---F
# feature:        \---D---E
#
# After:
# main:     A---B---C---F
# feature:                  D'---E' (新的 commit hash)
```

---

## 2. 分支策略

### 2.1 GitFlow

> 适用于：有固定发布周期的项目，需要同时维护多个版本

```
master ───────●─────────────●─────────────●──── (发布历史)
             / \           / \           / \
develop ────●───●───●─────●───●───●─────●───●── (开发主线)
          /     \         /     \
feature/ ──●──●── ●──●───  ●──●──        (功能开发)
                        \         \
release/                 ●──●───── ●──●   (发布准备)
                                   \
hotfix/                              ●──●  (紧急修复)
```

**分支说明：**

| 分支 | 命名 | 来源 | 合并到 | 生命周期 |
|------|------|------|--------|---------|
| master | `master` | - | - | 永久 |
| develop | `develop` | master | master | 永久 |
| feature | `feature/*` | develop | develop | 直到功能完成 |
| release | `release/*` | develop | master + develop | 直到发布 |
| hotfix | `hotfix/*` | master | master + develop | 直到修复完成 |

**完整工作流示例：**

```bash
# 1. 初始化 GitFlow
git flow init
# 默认: master 分支用于发布，develop 分支用于开发

# 2. 开始新的功能
git flow feature start user-login
# 相当于: git checkout -b feature/user-login develop

# 3. 在 feature 分支上开发
echo "login code" > login.java
git add login.java
git commit -m "feat: implement user login"
git push origin feature/user-login

# 4. 完成功能（合并到 develop）
git flow feature finish user-login
# 相当于:
#   git checkout develop
#   git merge --no-ff feature/user-login
#   git branch -d feature/user-login

# 5. 准备发布
git flow release start v1.2.0
# 相当于: git checkout -b release/v1.2.0 develop

# 6. 发布分支上的修复
echo "fix" >> release-notes.txt
git commit -m "fix: update release notes"

# 7. 完成发布
git flow release finish v1.2.0
# 相当于:
#   git checkout master
#   git merge --no-ff release/v1.2.0
#   git tag v1.2.0
#   git checkout develop
#   git merge --no-ff release/v1.2.0
#   git branch -d release/v1.2.0

# 8. 紧急修复
git flow hotfix start v1.2.1
# 相当于: git checkout -b hotfix/v1.2.1 master
# ... 修复代码 ...
git flow hotfix finish v1.2.1
```

**GitFlow 的优缺点：**

| 优点 | 缺点 |
|------|------|
| 清晰的发布管理和版本追踪 | 分支过于复杂，学习曲线陡 |
| 支持并行多版本维护 | 频繁的 merge commit 导致历史混乱 |
| 适合固定周期发布 | 不适合持续部署 |
| hotfix 流程明确 | feature 分支生命周期长，集成困难 |

### 2.2 GitHub Flow

> 适用于：持续部署，功能独立，快速迭代的团队

```
main ●─────────●─────────●─────────●─────────●
     \         / \       / \       / \       /
      ●──●──● /   ●──● /   ●──● /   ●──● /
          feature/1     feature/2     feature/3
```

**核心规则：**
1. main 分支始终可部署
2. 每个新功能从 main 拉出 feature 分支
3. 在 feature 分支上开发，频繁 push
4. 创建 Pull Request 请求合并
5. 代码评审和 CI 通过后合并
6. 合并后立即部署

**工作流示例：**

```bash
# 1. 从 main 拉出功能分支
git checkout -b feature/add-payment main

# 2. 开发并频繁提交
git commit -m "feat: add payment model"
git commit -m "feat: add payment service"
git commit -m "test: add payment tests"

# 3. 定期同步 main
git fetch origin
git rebase origin/main

# 4. 推送并创建 PR
git push origin feature/add-payment
# GitHub 上: New Pull Request → feature/add-payment → main

# 5. PR 合并方式
# - 如果希望线性历史: "Rebase and merge"
# - 如果希望保留分支: "Create a merge commit"
# - 如果希望 squash: "Squash and merge"

# 6. 合并后删除远程分支
git push origin --delete feature/add-payment
```

### 2.3 GitLab Flow

> 适用于：需要环境管理（如 staging, pre-production）的场景

```
main ───────●─────────●─────────●──────────
            |         |         |
            ●──●──── ●──●──── ●──●──── (environment branches)
            |         |         |
            ●──●──── ●──●──── ●──●──── (release branches)
            |         |
            ●──●─── ●──●─── (feature branches)
```

**环境分支模式：**

```bash
# 环境分支: 将 main 的稳定版本推送到特定环境
git push origin main:staging     # 部署到 staging
git push origin main:production  # 部署到 production

# 或使用 merge request 合并到环境分支
```

**发布分支模式：**

```bash
# 需要维护旧版本时使用 release 分支
git checkout -b release/2.3 main

# 在 release 分支上做 bug 修复
git cherry-pick bugfix-commit

# 修复合并回 main
git checkout main
git merge --no-ff release/2.3
```

### 2.4 Trunk-Based Development

> 适用于：持续部署，团队成熟度高，功能可快速集成

```
main ───●──●──●──●──●──●──●──●──●──●──●──
        |    |     |    |     |    |     |
        ●    ●     ●    ●     ●    ●     ●
       (short-lived feature branches, hours not days)
```

**核心原则：**

| 原则 | 描述 |
|------|------|
| 短生命周期分支 | Feature 分支不超过 1-2 天，通常几小时 |
| 频繁合并 | 每天至少合并到 main 一次 |
| 小批量提交 | 每次提交改动量小，可独立部署 |
| 特性开关 | 未完成的功能用 Feature Flag 隐藏 |
| 严格 CI | 不通过 CI 的代码不允许合并 |
| 代码评审 | 可选的评审方式（pair programming 代替） |

**Feature Flag 配合：**

```java
// Feature Flag 实现示例
@Component
public class FeatureFlagService {
    private final Map<String, Boolean> features = new ConcurrentHashMap<>();

    public boolean isEnabled(String featureName) {
        return features.getOrDefault(featureName, false);
    }

    // 通过配置中心动态切换
    @EventListener
    public void onFeatureToggle(FeatureToggleEvent event) {
        features.put(event.getFeatureName(), event.isEnabled());
    }
}

// 使用示例
@Service
public class PaymentService {
    @Autowired
    private FeatureFlagService featureFlags;

    public void processPayment(Order order) {
        if (featureFlags.isEnabled("new-payment-flow")) {
            // 新的支付流程（开发中，但已合并到 main）
            processNewPaymentFlow(order);
        } else {
            // 旧的支付流程（稳定版）
            processLegacyPayment(order);
        }
    }
}
```

### 2.5 分支策略选择指南

| 因素 | GitFlow | GitHub Flow | GitLab Flow | Trunk-Based |
|------|---------|-------------|-------------|-------------|
| 发布周期 | 固定周期 | 持续部署 | 环境管理 | 持续部署 |
| 团队规模 | 大团队 | 小团队 | 中型团队 | 中型团队 |
| 团队成熟度 | 低-中 | 中 | 中-高 | 高 |
| 版本维护 | 需要多版本 | 仅最新版 | 有限版本 | 仅最新版 |
| 学习曲线 | 陡 | 平缓 | 中等 | 中等 |
| 分支数量 | 多 | 少 | 中 | 很少 |
| Merge Commit | 很多 | 少数 | 中等 | 很少 |

**推荐：**
- **初创产品 / SaaS**：GitHub Flow 或 Trunk-Based
- **企业级软件**：GitFlow 或 GitLab Flow
- **有多版本维护需求**：GitFlow
- **团队经验不足**：先上 GitHub Flow 再演进

---

## 3. 高级 Git 操作

### 3.1 Interactive Rebase

```bash
# 交互式 rebase 最近 3 个 commit
git rebase -i HEAD~3

# 或基于某个分支
git rebase -i main
```

**交互式界面：**

```
pick a1b2c3d feat: add user login
squash e5f6g7h fix: login validation
reword i9j0k1l feat: add payment
fixup m3n4o5p fix: payment test

# Commands:
# p, pick = 使用该 commit
# r, reword = 使用但修改提交信息
# e, edit = 使用但暂停修改
# s, squash = 合并到上一个 commit
# f, fixup = 合并到上一个但不保留提交信息
# x, exec = 运行 shell 命令
# d, drop = 删除该 commit
```

**常见场景：**

```bash
# 场景1: 把多个 WIP commit 合并成一个
git rebase -i HEAD~5
# 除了第一个都改成 squash

# 场景2: 修改历史 commit 消息
git rebase -i HEAD~3
# 将要修改的 commit 从 pick 改成 reword

# 场景3: 删除错误的 commit
git rebase -i HEAD~10
# 将要删除的 commit 改成 drop

# 场景4: 重新排序 commit
git rebase -i HEAD~5
# 在编辑器中拖动行来重新排序

# 场景5: 拆分一个 commit
git rebase -i HEAD~3
# 将想要拆分的 commit 改成 edit (e)
# 然后: git reset HEAD^
# 然后: 分次 add 和 commit
```

**Rebase 黄金法则：** **永远不要 rebase 已经推送到远程分支（特别是共享分支）的 commit**。

### 3.2 Cherry-pick

```bash
# 将特定 commit 应用到当前分支
git cherry-pick a1b2c3d

# 应用多个 commit
git cherry-pick a1b2c3d e5f6g7h i9j0k1l

# 应用一段连续的 commit
git cherry-pick a1b2c3d..e5f6g7h

# 不自动提交，只应用到工作区
git cherry-pick -n a1b2c3d

# 添加 cherry-pick 标记
git cherry-pick -x a1b2c3d  # 在提交信息中添加来源引用
```

**Cherry-pick 的陷阱：**

| 陷阱 | 说明 | 建议 |
|------|------|------|
| 丢失上下文 | Cherry-pick 的 commit 脱离了原始上下文 | 记录 cherry-pick 来源 |
| 重复 commit | 相同的修改出现在不同分支 | 优先使用 merge |
| 冲突频繁 | 不同基线导致 cherry-pick 冲突 | 确保基线一致 |
| 依赖丢失 | 只 cherry-pick 功能 commit 但缺少基础 commit | 检查依赖关系 |

**Cherry-pick 最佳实践：**
```bash
# 只在以下情况使用 cherry-pick：
# 1. 紧急修复需要应用到多个发布分支
# 2. 从 release 分支挑选特定 bug fix 到 main
# 3. 不需要合并整个分支，只需要特定 commit

# Cherry-pick 后推荐的做法：
git cherry-pick -x a1b2c3d  # -x 会记录 cherry-pick 来源
# 提交信息会追加: (cherry picked from commit a1b2c3d)
```

### 3.3 Reflog

```bash
# 查看 HEAD 的所有历史移动记录
git reflog

# 输出示例：
# a1b2c3d (HEAD -> main) HEAD@{0}: commit: feat: add login
# e5f6g7h HEAD@{1}: reset: moving to HEAD~1
# i9j0k1l HEAD@{2}: commit: fix: validation
# m3n4o5p HEAD@{3}: rebase finished: returning to refs/heads/main
# c8d9e0f HEAD@{4}: rebase: fix: validation  (新的 commit hash)
# q7r8s9t HEAD@{5}: rebase: feat: add login  (新的 commit hash)
# u1v2w3x HEAD@{6}: checkout: moving from feature/login to main

# 恢复丢失的 commit
git reset --hard HEAD@{2}       # 恢复到特定位置
git cherry-pick HEAD@{5}        # 从历史中挑选 commit
```

**Reflog 的救命场景：**

```bash
# 场景1: 误 reset
git reset --hard HEAD~5                     # 哦不，我丢失了 5 个 commit
git reflog                                   # 找到之前的 HEAD
git reset --hard HEAD@{1}                    # 恢复

# 场景2: rebase 搞砸了
git rebase -i HEAD~10                        # rebase 出错
git rebase --abort                           # 放弃 rebase
# 或者：git reset --hard ORIG_HEAD           # 恢复到 rebase 前

# 场景3: 误删分支
git branch -D feature/payment               # 哦不，我删了需要的分支
git reflog                                   # 找到分支上的最后一次 commit
git checkout -b feature/payment a1b2c3d     # 恢复分支
```

### 3.4 Bisect

```bash
# 使用二分法查找引入 bug 的 commit
git bisect start
git bisect good v1.0.0    # 已知的好的版本
git bisect bad HEAD        # 当前版本有问题（坏的）

# Git 会 checkout 一个中间的 commit
# 测试这个 commit，然后标记
git bisect good            # 如果这个 commit 没有 bug
# 或
git bisect bad             # 如果这个 commit 有 bug

# 重复直到找到第一个 bad commit
# 找到后：
git bisect reset           # 结束 bisect

# 自动化 bisect（写一个测试脚本）
git bisect start HEAD v1.0.0
git bisect run ./test-script.sh  # 脚本返回 0 表示 good，非 0 表示 bad
git bisect reset
```

**自动化 Bisect 脚本示例：**

```bash
#!/bin/bash
# test-script.sh: bisect 自动化脚本

# 编译项目
mvn compile -q 2>/dev/null || exit 125  # 编译失败，跳过

# 运行测试
mvn test -Dtest=BugReproductionTest -q 2>/dev/null

# 测试通过 → good (exit 0)
# 测试失败 → bad (exit 1)
exit $?
```

### 3.5 Git Hooks

**pre-commit 钩子（代码格式检查）：**

```bash
#!/bin/bash
# .git/hooks/pre-commit

# 检查是否安装了 Spotless
if ! command -v mvn &> /dev/null; then
    echo "Maven is required"
    exit 1
fi

# 运行代码格式检查
mvn spotless:check -q 2>/dev/null

if [ $? -ne 0 ]; then
    echo "❌ Code formatting check failed. Run 'mvn spotless:apply' to fix."
    exit 1
fi

# 检查是否包含敏感信息
FILES=$(git diff --cached --name-only)
for FILE in $FILES; do
    if grep -q 'password\s*=\|secret\s*=\|api_key\s*=' "$FILE" 2>/dev/null; then
        echo "❌ $FILE contains potential secrets!"
        exit 1
    fi
done

echo "✅ pre-commit check passed"
```

**commit-msg 钩子（Conventional Commits 验证）：**

```bash
#!/bin/bash
# .git/hooks/commit-msg

COMMIT_MSG=$(cat "$1")

# 使用正则验证 Conventional Commits 格式
if [[ ! "$COMMIT_MSG" =~ ^(feat|fix|docs|style|refactor|perf|test|chore|ci|build|revert)(\([a-zA-Z0-9._-]+\))?!?:\ .{1,72}$ ]]; then
    if [[ ! "$COMMIT_MSG" =~ ^BREAKING\ CHANGE:\ .+$ ]]; then
        echo "❌ Invalid commit message format!"
        echo ""
        echo "Expected format: type(scope): description"
        echo ""
        echo "Valid types: feat, fix, docs, style, refactor, perf, test, chore, ci, build, revert"
        echo ""
        echo "Examples:"
        echo "  feat: add user login"
        echo "  fix(api): fix NPE in UserController"
        echo "  feat!: breaking change"
        echo "  feat(api)!: breaking change with scope"
        exit 1
    fi
fi
```

**pre-push 钩子（push 前运行测试）：**

```bash
#!/bin/bash
# .git/hooks/pre-push

echo "🔄 Running tests before push..."

# 运行单元测试
mvn test -q 2>/dev/null

if [ $? -ne 0 ]; then
    echo "❌ Tests failed. Fix before pushing."
    exit 1
fi

echo "✅ All tests passed. Pushing..."
```

**安装 hooks（推荐使用 husky 或手动复制）：**

```bash
# 手动复制到 .git/hooks/
cp scripts/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# 或使用 husky（npm 包）
npx husky init
npx husky add .husky/pre-commit "mvn spotless:check"
npx husky add .husky/commit-msg "scripts/validate-commit.sh"
npx husky add .husky/pre-push "mvn test"
```

### 3.6 Submodules vs Subtrees

| 特性 | Submodules | Subtrees |
|------|------------|----------|
| 子项目引用 | 指针引用（commit hash） | 拷贝源代码 |
| 修改子项目 | 需要在子项目中修改并 push | 可直接在当前仓库修改 |
| 提交原子性 | 需要两步（子项目 + 主项目） | 单次提交 |
| 克隆体验 | 需要 `--recurse-submodules` | 直接 clone 就包含 |
| 版本管理 | 子项目版本锁定为特定 commit | 随主项目版本变化 |
| 上游同步 | `git submodule update --remote` | `git subtree pull` |
| 冲突处理 | 相对简单 | 复杂（需要理解 subtree merge） |
| 适用场景 | 依赖外部库，不常修改 | 需要 fork 并自定义 |

**Submodule 操作：**

```bash
# 添加子模块
git submodule add https://github.com/company/common-utils.git libs/common-utils

# 克隆包含子模块的项目
git clone --recurse-submodules https://github.com/company/main-project.git

# 更新子模块到最新
git submodule update --remote libs/common-utils

# 子模块有更新时
cd libs/common-utils
git checkout v2.0.0
cd ../..
git commit -m "chore: update common-utils to v2.0.0"
```

**Subtree 操作：**

```bash
# 添加 subtree
git subtree add --prefix=libs/common-utils \
    https://github.com/company/common-utils.git main --squash

# 更新 subtree
git subtree pull --prefix=libs/common-utils \
    https://github.com/company/common-utils.git main --squash

# push 变更回上游
git subtree push --prefix=libs/common-utils \
    https://github.com/company/common-utils.git main
```

### 3.7 Git LFS (Large File Storage)

```bash
# 安装 LFS
git lfs install

# 指定需要 LFS 管理的文件类型
git lfs track "*.psd"
git lfs track "*.zip"
git lfs track "*.tar.gz"
git lfs track "*.jar" --filename libs/big-library.jar

# 查看 LFS 跟踪规则
git lfs track
# 输出:
# *.psd filter=lfs diff=lfs merge=lfs -text
# *.zip filter=lfs diff=lfs merge=lfs -text

# .gitattributes 会保存这些规则
cat .gitattributes
# *.psd filter=lfs diff=lfs merge=lfs -text

# 迁移已有文件到 LFS
git lfs migrate import --include="*.psd,*.zip"

# 查看 LFS 使用情况
git lfs ls-files --all
git lfs env
```

**LFS 的最佳实践：**

| 场景 | 建议 | 替代方案 |
|------|------|---------|
| 编译产物 (JAR/WAR) | 不上 Git，放 Nexus | LFS 也不推荐 |
| 大日志文件 | 不上 Git | LFS 也不推荐，用日志系统 |
| 设计稿 (PSD/Sketch) | 用 LFS | 或用专门的资产管理 |
| 大测试数据集 | 用 LFS | 或用对象存储 S3 |
| 二进制依赖 | 用仓库管理器 | LFS 也不推荐 |

---

## 4. 提交约定（Conventional Commits）

### 4.1 规范格式

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

**完整示例：**

```
feat(api): add user registration endpoint

Implement POST /api/v1/users endpoint with email verification.
- Add UserController with registration handler
- Add EmailService for sending verification emails
- Add rate limiting to prevent abuse

Closes #123
BREAKING CHANGE: The UserDTO has been renamed to UserResponse
```

### 4.2 类型定义

| 类型 | 用途 | 是否影响版本 | 说明 |
|------|------|-------------|------|
| `feat` | 新功能 | MINOR | 新增功能 |
| `fix` | Bug 修复 | PATCH | 修复 bug |
| `docs` | 文档变更 | - | 只修改文档 |
| `style` | 代码格式 | - | 不影响代码含义的变化（空格、格式化等） |
| `refactor` | 代码重构 | - | 既不修复 bug 也不增加功能的重构 |
| `perf` | 性能优化 | PATCH | 提高性能的修改 |
| `test` | 测试相关 | - | 添加或修改测试 |
| `chore` | 构建/工具 | - | 构建过程或辅助工具的变动 |
| `ci` | CI 配置 | - | CI 配置文件或脚本的变动 |
| `build` | 构建系统 | - | Maven/Gradle 等构建系统变更 |
| `revert` | 回滚 | - | 回滚之前的 commit |

### 4.3 Breaking Changes

三种标识方式：

```
# 方式 1: type 后加 !
feat!: remove deprecated user API

# 方式 2: type(scope) 后加 !
feat(api)!: change response format

# 方式 3: footer 中标注
feat: upgrade to Spring Boot 3

BREAKING CHANGE: javax.* packages replaced by jakarta.*
```

### 4.4 工具链

**commitlint 配置：**

```javascript
// commitlint.config.js
module.exports = {
    extends: ['@commitlint/config-conventional'],
    rules: {
        'type-enum': [2, 'always', [
            'feat', 'fix', 'docs', 'style',
            'refactor', 'perf', 'test', 'chore',
            'ci', 'build', 'revert'
        ]],
        'scope-case': [2, 'always', 'lower-case'],
        'subject-case': [0], // 不限制大小写
        'subject-empty': [2, 'never'],
        'subject-full-stop': [2, 'never', '.'],
        'header-max-length': [2, 'always', 72],
        'body-leading-blank': [2, 'always'],
        'footer-leading-blank': [2, 'always'],
    },
};
```

**commitizen 交互式提交：**

```bash
# 安装 commitizen
npm install -g commitizen

# 配置适配器
# cz-conventional-changelog 是标准适配器

# 使用交互式提交
git cz
# 会提示选择 type, scope, description 等

# 替代: 直接写规范的提交
git commit -m "feat(api): add pagination support"
```

**semantic release 自动版本管理：**

```javascript
// release.config.js
module.exports = {
    branches: ['main', { name: 'next', prerelease: true }],
    plugins: [
        '@semantic-release/commit-analyzer',
        '@semantic-release/release-notes-generator',
        '@semantic-release/changelog',
        ['@semantic-release/git', {
            assets: ['CHANGELOG.md'],
            message: 'chore(release): ${nextRelease.version}\n\n${nextRelease.notes}'
        }],
        '@semantic-release/maven',
        '@semantic-release/github'
    ]
};
```

**版本自动推导规则：**

| Commit 类型 | 版本变化 |
|-------------|---------|
| `feat!` 或 `BREAKING CHANGE` | MAJOR (x.0.0) |
| `feat` | MINOR (0.x.0) |
| `fix` | PATCH (0.0.x) |
| `perf` | PATCH (0.0.x) |
| 其他类型 | 不触发发布 |

### 4.5 提交信息最佳实践

| 好的提交信息 | 不好的提交信息 |
|-------------|--------------|
| `feat: add user registration endpoint` | `update` |
| `fix(api): handle NPE when user is null` | `fix bug` |
| `refactor: extract EmailService from UserService` | `refactoring code` |
| `test: add unit tests for PaymentService` | `add tests` |
| `docs: update API documentation for v2` | `update docs` |
| `perf: optimize database query with index` | `speed up` |

**提交信息的指导原则：**
1. 使用祈使句（"Add" 而非 "Added" 或 "Adds"）
2. 第一个字母小写
3. description 不超过 72 个字符
4. scope 一般使用模块或组件名
5. body 解释"为什么"而非"做了什么"

---

## 5. 代码评审最佳实践

### 5.1 PR 描述模板

```markdown
## Description

<!-- 简要描述本次 PR 做了哪些改动 -->

## Type of Change

- [ ] feat: 新功能
- [ ] fix: Bug 修复
- [ ] refactor: 代码重构
- [ ] perf: 性能优化
- [ ] test: 测试相关
- [ ] docs: 文档变更
- [ ] chore: 构建/工具
- [ ] ci: CI 配置

## Related Issue

<!-- 关联的 Issue 或 Ticket -->
Closes #123

## How Has This Been Tested?

<!-- 描述测试情况 -->
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 手动测试完成

## Screenshots (if applicable)

<!-- UI 变更请附截图 -->

## Checklist

- [ ] 代码遵循项目编码规范
- [ ] 已添加/更新测试用例
- [ ] 文档已更新
- [ ] 本地构建通过
- [ ] 无新增的 SonarQube 问题
- [ ] 性能无退化

## Additional Context

<!-- 其他需要评审者注意的信息 -->
```

### 5.2 评审检查清单

#### 正确性 (Correctness)

- [ ] 逻辑是否正确处理了所有分支条件？
- [ ] 边界条件是否已处理？（null, empty, max/min values）
- [ ] 并发安全性如何？（是否用到 ConcurrentHashMap？是否需要锁？）
- [ ] 事务边界是否正确？
- [ ] 异常处理是否恰当？（是否吞掉了异常？是否抛出了合适的异常？）

#### 性能 (Performance)

- [ ] 是否存在 N+1 查询问题？
- [ ] 循环中是否做了数据库查询/远程调用？
- [ ] 是否使用了合适的集合类型和初始容量？
- [ ] 缓存策略是否合理？
- [ ] 是否考虑了批量操作？

#### 安全 (Security)

- [ ] 用户输入是否做了校验和转义？
- [ ] SQL 注入风险？（是否使用了参数化查询？）
- [ ] 敏感数据是否做了脱敏？
- [ ] 权限检查是否到位？
- [ ] API 限流是否需要？

#### 可测试性 (Testability)

- [ ] 单元测试是否覆盖了核心逻辑？
- [ ] Mock 是否合适？（是否 over-mocking？）
- [ ] 测试是否覆盖了异常路径？
- [ ] 测试命名是否清晰？

#### 可读性 (Readability)

- [ ] 命名是否表达意图？（变量名、方法名、类名）
- [ ] 方法是否过长？（超过 50 行需要考虑拆分）
- [ ] 注释是否必要且准确？（而不是解释"做了什么"）
- [ ] 是否有不必要的复杂逻辑？
- [ ] 是否遵循了 DRY 原则？

### 5.3 评审规模指南

```
# 最佳实践：每次 PR 的修改量
# 200-400 行代码是最佳评审范围
# 超过 1000 行建议拆分

| 行数                  | 建议                               |
|----------------------|------------------------------------|
| < 100                | 可以快速评审                         |
| 100-400              | 最佳范围，能发现 70%+ 的问题          |
| 400-800              | 会遗漏一些细节问题                    |
| 800-1500             | 明显降低评审质量，建议拆分             |
| > 1500               | 必须拆分，不可评审                    |

# 评审时间：
# 单次评审不超过 60 分钟
# 超过 400 行或需要深度理解的，分多次评审
```

### 5.4 评审评论分级

#### Blocker (阻塞)
```markdown
**Blocker:** This code has a NullPointerException when order is null.
Please ensure null safety here.
// Order order = orderService.getById(orderId);
// order.getAmount()... // NPE when order is null
```

#### Suggestion (建议)
```markdown
**Suggestion:** Consider extracting the discount calculation logic
to a separate method for better testability.

```java
private BigDecimal calculateDiscount(Order order) {
    // current logic here
}
```
```

#### Nitpick (轻微)
```markdown
**Nitpick:** The variable name `list` is too generic. Consider `pendingOrders`.
```

**评论分级对比：**

| 级别 | 含义 | 处理方式 | 如何标记 |
|------|------|---------|---------|
| Blocker | 必须修复才能合并 | 作者必须修改 | `**Blocker:**` |
| Suggestion | 建议修改，但可以不修 | 作者决定，讨论后更新 Issues | `**Suggestion:**` |
| Nitpick | 小问题，风格偏好 | 可以不修，或另开 PR 修改 | `**Nitpick:**` |
| Question | 需要澄清理解 | 作者回答即可 | `**Question:**` |
| Praise | 肯定好的实现 | 回复"谢谢"或+1 | `**Praise:**` |

### 5.5 Reviewer 指南

| 做 | 不做 |
|----|------|
| 保持尊重，对代码不对人 | 使用指责性语言（"你写的什么垃圾代码"） |
| 用提问代替命令（"这里为什么不用 Optional？"） | 用命令式语气（"改成 Optional"） |
| 解释"为什么"（"这样改可以避免将来...的问题"） | 只说"怎么改" |
| 肯定好的实现（"这个设计很优雅"） | 只挑毛病不夸 |
| 关注关键问题，放过小问题（但可以记录） | 在每一行都发表评论 |
| 及时评审（24 小时内回应） | 拖到 PR 堆积 |

### 5.6 Author 指南

| 做 | 不做 |
|----|------|
| 在关键代码加注释，帮助 reviewer 理解 | 不加任何说明就提交 |
| 拆分大型 PR 为多个小 PR | 一个 PR 改上千行代码 |
| 认真回复每一条评论 | 忽略评论或敷衍回复 |
| 收到 blocker 评论立即修改 | 争论"我觉得这样也可以" |
| 使用 Draft PR 提前获取反馈 | 等全部写完才提交评审 |
| 成功后感谢 reviewer 的时间 | 把评审当走过场 |

### 5.7 评审工具

**GitHub PR Review：**

```markdown
<!-- 在 PR 代码行上直接评论 -->
1. 在代码行点击 "+" 号
2. 输入评论
3. 选择 "Start a review" 而不是 "Add single comment"
4. 所有评论完成后，点击 "Finish your review"
5. 选择: Comment / Approve / Request Changes
```

**Gerrit（更严格的评审系统）：**

```bash
# Gerrit 工作流
git push origin HEAD:refs/for/main    # 提交评审

# 评审后修改
git commit --amend --no-edit
git push origin HEAD:refs/for/main    # 新版本会更新同一个 Change

# 下载评审中的代码
git review -d <change-id>
```

---

## 6. Pull Request 工作流

### 6.1 完整 PR 生命周期

```
Draft PR ──→ Open PR ──→ Review ──→ Approved ──→ Merged
  │             │          │           │
  └── Still     │          ├── Changes Requested
      coding    │          │       │
                │          │       └── Author updates
                │          │           │
                │          └────── Re-review ←──┘
                │
                └── CI Fail → Fix → Re-push
```

### 6.2 PR 工作流步骤详解

```bash
# Step 1: 从 main 创建功能分支
git checkout -b feat/add-payment main

# Step 2: 开发并提交
git add src/
git commit -m "feat: add payment service"

# Step 3: 定期同步 main
git fetch origin
git rebase origin/main   # 保持线性历史
# 或: git merge origin/main  # 保留分支历史

# Step 4: 推送并创建 PR
git push origin feat/add-payment

# Step 5: 在 GitHub/GitLab 上创建 PR
# - 选择 base: main, compare: feat/add-payment
# - 填写 PR 描述模板
# - 添加 reviewers, labels, projects

# Step 6: 评审期间修改
# reviewer 提出修改意见后:
git add src/
git commit -m "fix: handle edge case in payment validation"
git push origin feat/add-payment  # 自动更新 PR

# Step 7: Squash 后合并
# 在 GitHub 上选择 "Squash and merge"
# PR 中的所有 commit 会合并为一个

# Step 8: 删除分支
git checkout main
git pull origin main
git branch -d feat/add-payment
```

### 6.3 合并策略选择

| 策略 | Git 命令 | 历史 | 适用场景 |
|------|---------|------|---------|
| Merge commit | `git merge --no-ff` | 保留完整分支历史 | 团队希望看到完整的开发过程 |
| Squash and merge | `git merge --squash` | 压缩为一个 commit | 分支上有大量 WIP commit |
| Rebase and merge | `git rebase` + `git merge --ff` | 线性历史，无分支信息 | 追求清洁的 git 历史 |

---

## 7. Java 项目 .gitignore 最佳实践

```gitignore
# ====== Java 编译产物 ======
*.class
*.jar
*.war
*.ear
target/
build/
!**/*.jar          # 排除某些特殊情况（如自定义镜像）
!gradle-wrapper.jar

# ====== IDE 文件 ======
.idea/
*.iml
*.iws
*.ipr
.settings/
.project
.classpath
*.prefs
.vscode/
*.swp
*.swo
*~

# ====== Maven ======
log/
*.log
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml

# ====== Gradle ======
.gradle/
gradle-app.setting
!gradle/wrapper/gradle-wrapper.jar
build/

# ====== OS 文件 ======
.DS_Store
Thumbs.db
*.sublime-*

# ====== 环境配置 ======
.env
.env.local
*.env
application-local.yml
application-local.properties
secrets.yml

# ====== 日志 ======
logs/
*.log
*.trace
*.out

# ====== 测试报告 ======
test-output/
reports/
*.html
jacoco.exec
*.dump

# ====== 数据库文件 ======
*.db
*.sqlite
*.h2.db

# ====== Node.js (前端构建) ======
node_modules/
npm-debug.log*

# ====== Docker ======
.docker/
docker-compose.override.yml

# ====== 临时文件 ======
*.tmp
*.bak
*.swp
*~

# ====== 项目特定 ======
# 以下根据项目情况配置
# *.local.*
# *.private.*
```

---

## 8. Monorepo vs Polyrepo

### 8.1 定义对比

| 特性 | Monorepo | Polyrepo |
|------|----------|----------|
| 仓库数量 | 单个仓库包含所有项目 | 每个项目独立仓库 |
| 代码共享 | 直接引用内部模块 | 通过包管理器 (Nexus) |
| 构建 | 统一构建工具 | 独立构建系统 |
| CI/CD | 单一流水线 | 多个流水线 |
| 版本管理 | 统一版本或独立版本 | 完全独立版本 |
| 权限管理 | 精细的目录级别权限 | 仓库级别权限 |
| 代码搜索 | 搜索整个代码库 | 需要跨仓库搜索 |

### 8.2 Monorepo 的优势与挑战

**优势：**
- 跨项目重构方便（如统一修改某个接口）
- 共享工具库和配置
- 原子性提交（一次提交影响多个项目）
- 标准化工具链（统一的格式化、lint、构建）
- 历史追溯完整

**挑战：**
- 仓库体积增长快
- 需要强大的构建工具链（增量编译）
- 权限管理复杂（谁可以改哪些模块）
- CI/CD 需要智能地只构建变更的模块
- Git 操作可能变慢（clone, log, blame）

### 8.3 选择指南

| 条件 | 推荐 |
|------|------|
| 小团队 (< 10人)，项目紧密相关 | Monorepo |
| 大团队，多个独立项目 | Polyrepo |
| 微服务架构，独立部署 | Polyrepo |
| 需要共享基础设施库 | Monorepo |
| 不同项目技术栈差异大 | Polyrepo |
| 关注开发体验和原子性变更 | Monorepo |

**折衷方案：**
- 有限 Monorepo：按领域组织，不超过 5-10 个相关项目
- 多仓库 + Git Submodule / Subtree
- Scoped packages (npm) / Multi-module (Maven)

---

## 9. GitOps 简介

### 9.1 什么是 GitOps

GitOps = Git + Infrastructure as Code + CI/CD + Kubernetes

**核心原则：**
1. **声明式配置**：系统状态通过配置文件声明
2. **Git 作为唯一真相源**：所有变更通过 Git 管理
3. **自动同步**：系统实际状态自动跟随 Git 中的配置
4. **自愈**：当实际状态偏离 Git 中的声明时，自动纠正

### 9.2 GitOps 工作流

```text
Developer                                        Kubernetes
   │                                                │
   ├── git push (变更 manifest)                     │
   │                                                │
   ▼                                                │
Git Repository                                      │
   │                                                │
   ├── CI: 验证 manifest (kubeval)                  │
   ├── CI: 构建镜像，推送到仓库                      │
   ├── CI: 更新 manifest 中的镜像 tag               │
   │                                                │
   ▼                                                │
ArgoCD / Flux                                       │
   │                                                │
   ├── 检测到 Git 变更                              │
   ├── 对比当前集群状态和 Git 状态                   │
   ├── 自动同步（或手动批准）                        │
   │                                                │
   ▼                                                ▼
Kubernetes Cluster ◄────── 应用配置 ────────
   │
   └── 如果配置漂移（手动 kubectl edit）→ ArgoCD 自动恢复
```

### 9.3 GitOps 工具

| 工具 | 描述 | 适用场景 |
|------|------|---------|
| ArgoCD | 声明式 GitOps for Kubernetes | K8s 为主的环境 |
| Flux | Weaveworks 的 GitOps 工具 | 自动化程度高的环境 |
| Jenkins X | K8s 原生 CI/CD 平台 | 需要完整 CI/CD 平台的团队 |
| Terraform | IaC 工具但不完全 GitOps | 基础设施管理 |

---

## 10. 面试高频题

### 基础概念类

**Q: Git 中 merge 和 rebase 的区别和适用场景？**
A: merge 保留分支历史（产生 merge commit），适合公共分支；rebase 线性化历史（不产生 merge commit），适合个人分支。黄金法则：永不 rebase 已推送的公共分支。

**Q: GitFlow 和 Trunk-Based Development 如何选择？**
A: GitFlow 适合固定发布周期、需要多版本维护的项目；Trunk-Based 适合持续部署、团队成熟度高的 SaaS 产品。核心权衡是"分支复杂度"vs"发布灵活性"。

**Q: Git bisect 的工作原理？**
A: 二分查找。在 good 和 bad commit 之间进行二分，每次 checkout 中间 commit 供测试。复杂度为 O(log n)，100 个 commit 最多 7 次测试即可找到 bug 引入点。

### 实战类

**Q: 推送后发现最后一个 commit 信息写错了，怎么修改？**
A:
```bash
# 如果还没推送:
git commit --amend -m "correct message"

# 如果已推送:
git commit --amend -m "correct message"
git push --force-with-lease  # 使用 force-with-lease 而不是 force
```

**Q: 开发了一半的 feature 需要切到其他分支修复紧急 bug，怎么办？**
A:
```bash
# 方法1: 暂存
git stash
git checkout main
# ... 修复紧急 bug ...
git checkout feature
git stash pop

# 方法2: 提交到临时分支
git add .
git commit -m "wip: temp save"
git checkout main -b hotfix/urgent
# ... 修复 ...
git checkout feature
git reset HEAD^  # 取消 commit 但保留修改
```

**Q: 一个 PR 太大 reviewer 不肯看，怎么办？**
A:
1. 拆分为多个逻辑独立的 PR（每个不超过 400 行）
2. 提交顺序为：基础重构 → 核心功能 → 测试 → 文档
3. 第一个 PR 先得到批准，后面的基于前面的分支
4. 在 PR 描述中标注"先评审这个 commit，后续 commit 是..."

### 场景类

**Q: 团队提交信息混乱，如何推动规范化？**
A:
1. 引入 commitlint 在 CI 中验证
2. 配置 pre-commit hook 本地验证
3. 提供 commitizen 交互式工具
4. 在 README 中明确提交规范
5. 从核心成员开始示范，逐步推广
6. 不要要求回溯修改历史 commit，从今天开始

**Q: Code Review 时发现逻辑问题但 reviewer 语气不好，如何处理？**
A:
- 作为 reviewer：时刻记住"对代码不对人"；用提问代替指责；给出具体的修改建议
- 作为 author：不要 defensive，理解评论的意图（即使表达方式不好）；在团队文化中倡导友善的评审氛围

**Q: 如何安全地删除已经推送的敏感信息（密码、密钥）？**
A:
1. `git filter-branch` 或 `BFG Repo-Cleaner` 从历史中删除
2. 立即更换所有暴露的密钥
3. 通知团队成员 rebase
4. 使用 `git push --force-with-lease` 推送修复后的历史
5. 注意：如果其他人已经 clone，他们的历史中还包含敏感信息

---

## 总结检查清单

- [ ] 团队是否选择了合适的分支策略
- [ ] Git hooks 是否配置了 pre-commit 和 commit-msg
- [ ] Conventional Commits 是否强制执行
- [ ] Code Review 流程是否明确（模板、Checklist、分级）
- [ ] PR 大小是否控制在合理范围（200-400 行）
- [ ] Reviewer 和 Author 指南是否写入团队文档
- [ ] .gitignore 是否正确配置
- [ ] Monorepo/Polyrepo 的选择是否有合理依据
- [ ] 是否需要 Git LFS
- [ ] Reflog 和 Bisect 是否在团队中推广使用
