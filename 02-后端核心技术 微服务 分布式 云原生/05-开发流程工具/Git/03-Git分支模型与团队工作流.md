# 03-Git 分支模型与团队工作流
> 分支本质只是指针，几乎零成本；但**怎么用分支**决定团队开发效率和发布质量——四模型对比、命名规范、发布与热修复流程

## 📚 目录
1. [分支底层原理](#1-分支底层原理)
2. [四种主流分支模型速览](#2-四种主流分支模型速览)
3. [Git Flow — 经典重型模型](#3-git-flow--经典重型模型)
4. [GitHub Flow — 轻量敏捷模型](#4-github-flow--轻量敏捷模型)
5. [GitLab Flow — 环境驱动模型](#5-gitlab-flow--环境驱动模型)
6. [Trunk-Based Development — 主干开发](#6-trunk-based-development--主干开发)
7. [分支模型决策树](#7-分支模型决策树)
8. [分支命名规范](#8-分支命名规范)
9. [版本发布流程与 hotfix 双合并](#9-版本发布流程与-hotfix-双合并)
10. [worktree：多分支并行开发](#10-worktree多分支并行开发)
11. [核心要点](#11-核心要点)
12. [参考来源](#12-参考来源)

## 1. 分支底层原理

```text
分支 = 指向某个 commit 的指针（一个 40 字节的文件，内容是 40 位 SHA-1 哈希）

创建分支 → 在 .git/refs/heads/ 下新建一个文件
切换分支 → 移动 HEAD 指针 + 更新工作区文件
删除分支 → 删除指针文件（commit 数据不丢失）
```

```bash
cat .git/refs/heads/main          # 输出 40 位 SHA-1 哈希
git cat-file -t 8f3a2c9b1d4       # 输出: commit（确认对象类型）
ls -la .git/refs/heads/           # 每个分支文件 = 41 字节（40 位哈希 + 换行符）
```

| HEAD 状态 | 指向 | 场景 | 风险 |
|-----------|------|------|:---:|
| 正常 | 某个分支引用 | 日常开发 | 无 |
| detached HEAD | 直接指向某个 commit | `git checkout <commit>` 查看历史 | ⚠️ 此状态提交可能丢失 |

```bash
git checkout 8f3a2c9b               # 进入 detached HEAD
git switch -c temp-branch           # 已有提交时：创建分支保住代码
git switch main                     # 只是查看：直接切回分支
```

> 🎯 **核心要点**：分支 = 环境隔离 + 需求边界 + 版本管理 + 安全防线。没有分支模型的团队痛点：所有人往 main 直接提交 → 频繁冲突；不知道哪些代码在生产 → 回滚困难；紧急修复没地方放。

## 2. 四种主流分支模型速览

| 模型 | 分支数量 | 适用团队 | 发布节奏 |
|------|---------|---------|---------|
| Git Flow | 多（main+develop+feature+release+hotfix） | 传统软件，有明确版本 | 几周~几月 |
| GitHub Flow | 少（main + feature） | Web 应用，SaaS | 按天/小时 |
| GitLab Flow | 中（main + feature + 环境分支） | 需要多环境 | 按需 |
| Trunk-Based | 极少（trunk + 短期 feature） | 精英团队，大厂 | 按小时 |

| 维度 | Git Flow | GitHub Flow | GitLab Flow |
|------|----------|-------------|-------------|
| 长期分支 | main + develop | main | main + 环境分支 |
| 分支数量 | 5 种 | 2 种 | 3-4 种（按环境） |
| 发布节奏 | 固定周期（周/月） | 随时发布 | 按环境推进 |
| 版本管理 | 每个 release 独立分支 | main 即最新 | 环境分支即版本 |
| 复杂度 | ⚠️ 高 | ✅ 低 | ⚠️ 中 |
| CI/CD 友好度 | ⚠️ 中 | ✅ 高 | ✅ 高 |
| 适用团队 | 10+ 人传统企业 | 2-10 人互联网 | 中型多环境团队 |

## 3. Git Flow — 经典重型模型

### 3.1 分支角色

| 分支 | 来源 | 合并到 | 生命周期 | 命名 |
|------|------|--------|---------|------|
| main | — | — | 永久 | `main` |
| develop | main | — | 永久 | `develop` |
| feature | develop | develop | 开发完成后删除 | `feature/xxx` |
| release | develop | main + develop | 发布完成后删除 | `release/x.y.z` |
| hotfix | main | main + develop | 修复完成后删除 | `hotfix/x.y.z` |

```text
Git Flow 分支工作流（时间从左到右）

main        init ────────────────── 合并 release/1.0 ── 合并 hotfix/1.0.1
                                    │                    │
develop     ── 合并 A ── 合并 B ──── 合并 release ─────── 合并 hotfix
            │          │
feature/A   A-1 A-2 ───┘
feature/B   B-1 ───────┘
release/1.0            fix-rc1 ─────┘
hotfix/1.0.1                          bugfix ────────────┘
```

### 3.2 操作流程

```bash
# 1. 初始化仓库（设置 main + develop）
git init
git checkout -b develop

# 2. 开发新功能
git checkout -b feature/user-login develop
# ... 开发 ...
git add . && git commit -m "feat: user login module"
git checkout develop
git merge --no-ff feature/user-login   # --no-ff 保留分支痕迹
git branch -d feature/user-login

# 3. 准备发布
git checkout -b release/1.0.0 develop
# 修复bug、更新版本号、文档
git commit -m "chore: bump version to 1.0.0"
git checkout main
git merge --no-ff release/1.0.0
git tag -a v1.0.0 -m "Release v1.0.0"
git checkout develop
git merge --no-ff release/1.0.0       # release 的修复合并回 develop
git branch -d release/1.0.0

# 4. 紧急修复
git checkout -b hotfix/1.0.1 main
# 修复bug
git commit -m "fix: critical login bug"
git checkout main
git merge --no-ff hotfix/1.0.1
git tag -a v1.0.1 -m "Hotfix v1.0.1"
git checkout develop
git merge --no-ff hotfix/1.0.1        # hotfix 必须合并回 develop！
git branch -d hotfix/1.0.1
```

| 优点 | 缺点 |
|------|------|
| 结构清晰，角色分明 | **太重**，日常维护成本高 |
| 适合有明确版本周期的软件 | merge 太多，历史复杂 |
| 发布内容可控 | 不适合频繁部署的 Web 应用 |

> 💡 适用：Java 传统项目、金融、政务等版本发布周期长的场景。

## 4. GitHub Flow — 轻量敏捷模型

### 4.1 核心原则（5 条）

1. **main 分支永远可部署**（铁律）
2. 从 main 创建 feature 分支，用描述性命名
3. 随时推送 feature 分支（早开 PR）
4. 通过 **Pull Request** 讨论和审查
5. 合并到 main 后**立即部署**

### 4.2 操作流程

```bash
# 1. 从 main 拉出 feature 分支
git checkout main && git pull origin main
git checkout -b feature/oauth-integration

# 2. 频繁提交，随时 push（备份+协作可见）
git commit -m "feat: add oauth redirect"
git push origin feature/oauth-integration

# 3. 在 GitHub 上创建 PR，讨论、Review、CI 检查

# 4. 合并方式选择（GitHub 三种按钮）
#   - Create a merge commit  → 保留完整历史
#   - Squash and merge       → 压缩所有 commit 为一个
#   - Rebase and merge       → 线性历史

# 5. 部署 main（合并后自动/手动触发）
# 6. 删除 feature 分支
git branch -d feature/oauth-integration
git push origin --delete feature/oauth-integration
```

```bash
# 保持 feature 分支与 main 同步
git checkout feature/xxx
git merge main          # 或者 git rebase main（看团队约定）
```

## 5. GitLab Flow — 环境驱动模型

```text
main ──────────> pre-production ──────────> production
  │                  │                        │
  └── feature/A      └── 自动部署到staging     └── 手动部署到生产
```

```bash
# main → pre-production → production 的逐级合并
git checkout pre-production && git merge main
git checkout production && git merge pre-production
git tag -a v2.1.0 -m "Deploy to production"
```

核心概念：**环境分支 = 环境的"当前版本"**；代码流动方向 `main → test → pre → prod`；每个环境合并前自动运行对应级别的测试。

## 6. Trunk-Based Development — 主干开发

大厂精英团队最爱（Google、Facebook）。

```text
trunk (main)
  ├── 超短期 feature 分支（< 1天）
  ├── Feature Flag 控制未完成功能
  └── 分支切换评审（Branch by Abstraction）
```

| 核心规则 | 说明 |
|----------|------|
| 短分支 | 所有人直接往 trunk 提交（或 <24h 超短分支） |
| Feature Flag | 用开关隐藏未完成功能 |
| 频繁集成 | 每天多次 |
| 强测试 | 必须有强测试覆盖 + CI |

```python
# Feature Flag 示例：用 flag 控制未完成的功能
if feature_flag_enabled("new-search"):
    return new_search_handler()
else:
    return old_search_handler()
```

## 7. 分支模型决策树

```text
                        多久发布一次？
                       /      |        \
        几周/几月      /       |         \ 每天/几小时
        (移动App/桌面) │        |          \
              Git Flow │        |        团队规模？
                       │        |        /        \
                       │        |    1-20人     50+人(有专人维护CI)
                       │        |      |            |
                       │        |  GitHub Flow     CI和测试完善吗?
                       │        |    (推荐)        /          \
                       │        |               完善          一般
                       │        |                |             |
                       │        |          Trunk-Based    GitHub Flow
                       │        |
                       │   需要多环境 (staging/production)?
                       │            |
                       │       GitLab Flow
```

| 场景 | 推荐模型 | 原因 |
|------|---------|------|
| 个人项目 / 小团队 | GitHub Flow | 简单够用 |
| 开源项目 | GitHub Flow + Fork | PR 是协作核心 |
| 移动 App 开发 | Git Flow | 版本发布有节奏，需要 hotfix |
| SaaS 产品 | GitHub Flow 或 Trunk-Based | 持续部署 |
| 企业级多环境 | GitLab Flow | 环境分支匹配发布流水线 |
| 大型基础设施 | Trunk-Based | 避免分支地狱 |

> 💡 建议路径：先用 GitHub Flow 入门 → 团队大了考虑 GitLab Flow → 工程能力强了探索 Trunk-Based。

## 8. 分支命名规范

| 前缀 | 用途 | 拉取来源 | 合并目标 | 示例 |
|------|------|----------|----------|------|
| `feature/` | 新功能开发 | develop | develop | `feature/user-auth` |
| `fix/` | 非生产 Bug 修复 | develop | develop | `fix/order-total` |
| `hotfix/` | 生产紧急修复 | main | main + develop | `hotfix/pay-timeout` |
| `release/` | 发布准备 | develop | main | `release/v2.1.0` |
| `refactor/` | 代码重构 | develop | develop | `refactor/extract-service` |
| `chore/` | 构建/依赖/工具 | develop | develop | `chore/upgrade-spring` |

```bash
feature/user-center-auth        # ✅ 小写+连字符
feature/UserAuth                # ❌ 驼峰命名
feature/user_auth               # ❌ 下划线
feature/user-auth-and-profile   # ❌ 太长
```

> 🎯 **命名原则**：小写字母 + 连字符分隔，前缀明确意图，名称简短（≤4 个单词）。团队规范可加 JIRA 号：`feature/PROJ-123-user-login`。

## 9. 版本发布流程与 hotfix 双合并

### 9.1 完整发布流程（Git Flow 版）

```bash
# 1. 从 develop 拉 release 分支
git switch develop && git pull
git switch -c release/v2.3.0

# 2. 在 release 上做发布准备（更新版本号、补文档、最终测试）

# 3. 合并到 main
git switch main && git pull
git merge --no-ff release/v2.3.0

# 4. 打标签
git tag -a v2.3.0 -m "Release v2.3.0: 新增订单导出、修复支付超时"

# 5. 推送到远程
git push origin main --tags

# 6. 合并回 develop（保证 develop 有 release 上的最终修复）
git switch develop
git merge --no-ff release/v2.3.0
git push origin develop

# 7. 清理
git branch -d release/v2.3.0
```

```text
semver: MAJOR.MINOR.PATCH
  MAJOR: 不兼容的API变更
  MINOR: 向后兼容的新功能
  PATCH: 向后兼容的Bug修复

标签 = 版本的不可变标记
  git tag -a v2.3.0 → 附注标签（推荐，含发布说明）
  git tag -s v2.3.0 → GPG签名标签（安全要求高的企业）
```

### 9.2 hotfix 双合并（main + develop）

```bash
# hotfix 需要同时合并到 main 和 develop
git switch main
git merge --no-ff hotfix/critical-fix
git push origin main

git switch develop
git merge --no-ff hotfix/critical-fix
# 如果有冲突（develop 比 main 多代码）→ 手动解决
git push origin develop

git branch -d hotfix/critical-fix
```

> ⚠️ **hotfix 未合并回 develop 的后果**：下次发布重复踩坑——hotfix 修复必须在 develop 上重放（合并或 cherry-pick）。

## 10. worktree：多分支并行开发

```text
传统方式：一个工作区，频繁 stash + switch
  → 切换分支耗时、stash 容易搞混、IDE 重新索引

worktree 方式：多个工作区目录，每个对应一个分支
  → 同时打开两个 IDE 窗口、互不干扰
```

```bash
git worktree add ../project-hotfix hotfix/critical   # 创建 worktree
git worktree list
# /path/to/project        a1b2c3d [main]
# /path/to/project-hotfix d4e5f6g [hotfix/critical]
git worktree remove ../project-hotfix
git worktree prune                                    # 清理记录
```

```bash
# 场景：正在 feature 分支开发，突然需要紧急 hotfix
git worktree add ../project-hotfix main
cd ../project-hotfix
git switch -c hotfix/critical-payment
# 修复、提交、合并...
cd ../project                      # 原工作区完全没受影响，连 stash 都不需要
git worktree remove ../project-hotfix
```

> 💡 Java 后端实战：一个 worktree 跑 `feature/user-module`，另一个跑 `feature/order-module`，两个 IDEA 窗口同时开发互不干扰。

## 11. 核心要点

> 🎯 **核心要点**：
> - 分支 = 41 字节指针文件，创建近乎零成本——成本在"怎么用"；
> - 四模型坐标：发布节奏（周/月 → 小时）× 团队规模（小 → 大）× 环境数（1 → N）；
> - 命名规范：小写 + 连字符 + 前缀表（feature/fix/hotfix/release/refactor/chore）；
> - 发布铁律：release 合并 main + 打 tag + 合并回 develop 三步缺一不可；
> - hotfix 双合并（main + develop）是最高频遗漏点；
> - 并行任务用 worktree，别用 stash 硬切。

## 12. 参考来源

- [Pro Git Book：分支章节](https://git-scm.com/book/zh/v2/Git-分支-分支简介)
- [Git Flow 官方说明（nvie）](https://nvie.com/posts/a-successful-git-branching-model/)
- [GitHub Flow 官方文档](https://docs.github.com/zh/get-started/using-github/github-flow)
- [Trunk-Based Development 官网](https://trunkbaseddevelopment.com/)

---

**下一模块**：[04-Git合并与变基](04-Git合并与变基.md)　/　**返回总览**：[00-总览](00-Git知识体系总览.md)
