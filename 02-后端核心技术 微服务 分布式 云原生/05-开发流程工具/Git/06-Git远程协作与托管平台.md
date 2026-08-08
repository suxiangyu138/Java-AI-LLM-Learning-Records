# 06-Git 远程协作与托管平台
> 协作的本质：远程仓库只是"约定的同步点"——remote 管理、PR 全流程、Fork 工作流、三大托管平台对比、gh CLI 与 LFS

## 📚 目录
1. [远程仓库管理](#1-远程仓库管理)
2. [Fetch vs Pull vs Push](#2-fetch-vs-pull-vs-push)
3. [Fork + upstream 双远程模式](#3-fork--upstream-双远程模式)
4. [推送冲突处理与跟踪关系](#4-推送冲突处理与跟踪关系)
5. [PR/MR 全流程](#5-prmr-全流程)
6. [协作模型](#6-协作模型)
7. [SSH 配置（免密推送）](#7-ssh-配置免密推送)
8. [托管平台对比与选型](#8-托管平台对比与选型)
9. [GitHub 生态](#9-github-生态)
10. [GitLab 与 Gitee](#10-gitlab-与-gitee)
11. [gh CLI 与多平台同步](#11-gh-cli-与多平台同步)
12. [Git LFS 大文件管理](#12-git-lfs-大文件管理)
13. [核心要点](#13-核心要点)
14. [参考来源](#14-参考来源)

## 1. 远程仓库管理

```text
你的本地仓库 ←→ 远程仓库 (origin) ←→ 同事的本地仓库
     ↕                    ↕                   ↕
  git push           GitHub/GitLab        git fetch
  git fetch           PR / MR             git merge
```

```bash
git remote                    # 列出远程仓库别名
git remote -v                 # 查看 URL（fetch/push 地址）
git remote show origin        # 查看详细信息（含分支追踪关系）
git remote add origin https://github.com/user/repo.git
git remote add upstream https://github.com/original/repo.git   # Fork 场景
git remote set-url origin git@github.com:user/repo.git         # 修改 URL（SSH/HTTPS 切换）
git remote set-url --push origin https://github.com/new-org/project.git  # 分离 fetch/push
git remote remove origin      # 删除远程
git remote rename origin old-origin   # 重命名
```

| 命令 | 场景 |
|------|------|
| `git remote add` | 本地 init 后首次关联远程仓库 |
| `git remote set-url` | 仓库迁移（GitHub→GitLab）、SSH↔HTTPS 切换 |
| `git remote remove` | 项目废弃或重新关联 |
| `git remote rename` | fork 项目的上游仓库管理 |

| 场景 | 推荐 Remote 配置 |
|------|------------------|
| 个人项目 | `origin → github` |
| 企业内部协作 | `origin → gitlab` |
| Fork 开发 / 开源贡献 | `origin → fork` + `upstream → 原项目`（upstream pull-only） |
| 多平台同步 | `origin → github` + `gitee → gitee`（双 push） |

## 2. Fetch vs Pull vs Push

| 命令 | 方向 | 合并？ | 安全？ |
|------|------|--------|--------|
| `git fetch` | 远程→本地（refs） | 否 | ✅ 最安全 |
| `git pull` | 远程→本地（工作区） | 是 | ⚠️ 可能冲突 |
| `git push` | 本地→远程 | — | ⚠️ 可能被拒绝 |

```bash
git fetch origin                    # 只下载不合并
git fetch --all                     # 更新所有远程追踪分支
git fetch --prune                   # 获取 + 删除已不存在的远程追踪分支
git pull origin main                # 下载 + 合并（= fetch + merge）
git pull --rebase origin main       # 用 rebase 代替 merge（推荐，保持线性）
git config --global pull.rebase true   # 全局默认 rebase
git push origin main
git push -u origin feature/login    # 推送并设置上游跟踪
git push origin --delete feature/login
```

| 操作 | 实质 | 是否合并 | 是否产生合并提交 | 推荐场景 |
|------|------|:---:|:---:|------|
| `git fetch` | 仅下载远程对象 | 否 | 否 | 先查看远程变更，再决定如何合并 |
| `git pull` | fetch + merge | 是 | 可能 | 单人分支或无需线性历史 |
| `git pull --rebase` | fetch + rebase | 是（变基合并） | 否 | 多人协作，保持历史线性 |

> 💡 为什么不推荐直接用 `git pull`：① merge 产生额外 merge commit，历史图复杂；② 多人协作时 merge commit 层层嵌套，`git log --graph` 变乱麻；③ `git pull --rebase` 历史线性清晰。

## 3. Fork + upstream 双远程模式

```text
原始仓库 (upstream)          你的 Fork (origin)          你的本地
main ←─────────────────── main ←──── git pull ──── main
  ↑         PR                 ↑       git push →     │
  └──────── feature ───────────┴──────────────── feature
```

```bash
# 1. Fork 仓库（网页操作）
# 2. Clone 自己的 Fork
git clone https://github.com/yourname/repo.git
# 3. 添加上游仓库
git remote add upstream https://github.com/original/repo.git
git remote -v   # origin(fork) + upstream(原仓库)
# 4. 创建功能分支
git checkout -b feature/awesome-feature
# 5. 开发 + 提交
git add . && git commit -m "feat: awesome feature"
# 6. 同步上游（开发期间上游可能有更新）
git fetch upstream
git rebase upstream/main
# 解决冲突 → git rebase --continue
# 7. 推送到自己的 Fork
git push -u origin feature/awesome-feature
# 8. 在 GitHub 上创建 PR：yourname/repo → original/repo
# 9. 根据 Review 修改
git commit -m "fix: address review comments"
git push origin feature/awesome-feature   # PR 自动更新
# 10. PR 合并后，同步 main
git checkout main
git pull upstream main
git push origin main
# 11. 清理本地分支
git branch -d feature/awesome-feature
```

```bash
# 每日同步（在 main 分支执行）
git checkout main
git fetch upstream
git rebase upstream/main          # 用上游代码 rebase 本地 main
git push origin main

# 将上游的指定分支同步到本地
git fetch upstream feature/xxx
git checkout -b feature/xxx upstream/feature/xxx
```

## 4. 推送冲突处理与跟踪关系

### 4.1 Push 被拒绝

```bash
git push origin main
# ! [rejected] main -> main (non-fast-forward)
# 正确做法：
git fetch origin
git rebase origin/main   # 或用 git merge origin/main
# 解决冲突（如果有）
git push origin main
```

### 4.2 分叉分支处理三方案

```bash
# 场景：本地 commit A，远程 commit B
# 方案一：rebase —— 结果线性：... → B → A
git fetch origin && git rebase origin/main
# 方案二：merge —— 结果：... → A → B → Merge Commit
git fetch origin && git merge origin/main
# 方案三：全自动（推荐）
git pull --rebase
```

> 💡 最佳实践组合：`git config --global pull.rebase true` + `git config --global rebase.autoStash true`——pull 自动 rebase 并自动 stash 未提交修改。

### 4.3 跟踪关系与 ahead/behind

```bash
git branch -vv                         # 查看跟踪关系（ahead/behind）
git branch -u origin/main              # 当前分支跟踪 origin/main
git branch -u origin/main mybranch     # 指定分支跟踪
git checkout -b feature/login origin/feature/login   # 基于远程分支创建本地分支
git remote prune origin                # 清理本地"已删除远程分支"引用

git status -sb
# ## feature/login...origin/feature/login [ahead 2, behind 3]
# ahead 2 = 本地有 2 个远程没有的提交 → 先 pull 再 push
```

### 4.4 冲突协作解决流程

```bash
# DevA/DevB 同时改 UserService.java 的 login 方法
# ① 沟通优先；② DevB 先合并：
git fetch origin
git rebase origin/develop
# 解决冲突 → git add → git rebase --continue → git push --force-with-lease
# ③ DevA 后合并（冲突已是 DevB 解决过的版本）；④ 双方 mvn compile && mvn test 验证
```

| 冲突类型 | 典型场景 | 解决策略 |
|----------|----------|----------|
| 逻辑冲突 | 两人改同一方法同一段逻辑 | 沟通确认意图，合并双方修改 |
| 新增文件冲突 | 两人创建同名文件 | 沟通确认是否重复，必要时重命名 |
| POM/配置冲突 | 同时改 pom.xml 添加依赖 | 保留所有依赖，删除冲突标记 |
| 接口签名冲突 | 一人改方法签名，另一人调用原方法 | 以新签名为准，更新调用方 |
| 删除 vs 修改 | A 删文件，B 改同一文件 | 确认删除意图 |

## 5. PR/MR 全流程

```text
创建 feature 分支 → 开发+commit → push 到远程 → 创建 PR/MR
→ CI 自动检查 → Code Review（需要修改则返回）→ Merge → 部署+删除分支
```

### 5.1 PR 合并策略

| 策略 | 效果 | 适用场景 |
|------|------|---------|
| **Create merge commit** | 创建 `Merge pull request #123...` | 保留完整历史 |
| **Squash and merge** | 所有 commit 压成 1 个 | feature 分支 wip commit 太多 |
| **Rebase and merge** | 线性接入，不创建 merge commit | 重视线性历史 |

### 5.2 PR 最佳实践

- 一个 PR 只做一件事（单一职责）；
- 保持 PR 小而专注：**200-400 行变更为佳**，超过 1000 行应当拆分；
- PR 描述写清楚"做了什么、为什么、怎么测试"；
- Review 通过后由 PR 作者自己合并（默认）；合并后立即删除分支。

```markdown
# PR 描述模板（团队统一版）
## 📝 变更概述
新增用户邮箱验证功能
## 🔗 关联Issue
Closes #1234
## 🧪 测试
- [x] 单元测试：验证码生成与校验
- [x] 集成测试：注册→验证→登录完整流程
- [x] 异常测试：过期验证码、错误验证码
## ⚠️ 风险提示
- 依赖邮件服务，需确认测试环境邮件配置
```

### 5.3 本地 Pull 审查（Reviewer）

```bash
git fetch origin pull/123/head:review/PROJ-123
git checkout review/PROJ-123
git log --oneline                     # 逐 commit 审查
git diff develop..review/PROJ-123     # 审查代码变更
mvn compile                           # 编译检查
# 审查完成清理
git checkout develop
git branch -D review/PROJ-123
# 或 gh CLI
gh pr checkout 123
```

## 6. 协作模型

| 模型 | 结构 | 适用 | 特点 |
|------|------|------|------|
| **Centralized**（集中式） | 全员推同一中央仓库同一分支 | 2-5 人小团队、迁移过渡 | 简单但冲突频繁、无审查 |
| **Integration-Manager**（集成管理者） | Maintainer 管 main，贡献者 PR/MR | 开源项目、中大型团队（**90% 企业**） | 代码审查强制 |
| **Dictator-Lieutenant**（独裁者-副手） | 分层维护者管理子系统 | Linux/Apache 超大规模 | 2-3 层维护者，流程复杂 |

## 7. SSH 配置（免密推送）

```bash
# 1. 生成 SSH key（推荐 ed25519，比 RSA 更快更安全）
ssh-keygen -t ed25519 -C "your_email@example.com"
# 2. 复制公钥 → GitHub → Settings → SSH and GPG keys
cat ~/.ssh/id_ed25519.pub
# 3. 测试连接
ssh -T git@github.com        # Hi username! You've successfully authenticated.
# 4. 用 SSH URL 克隆
git clone git@github.com:user/repo.git
# 5. HTTPS 克隆切换为 SSH
git remote set-url origin git@github.com:user/repo.git
# 6. 多账号配置 ~/.ssh/config
# Host github-personal
#     HostName github.com
#     User git
#     IdentityFile ~/.ssh/id_ed25519_personal
# Host github-work
#     HostName github.com
#     User git
#     IdentityFile ~/.ssh/id_ed25519_work
```

## 8. 托管平台对比与选型

| 维度 | GitHub | GitLab | Gitee | Bitbucket |
|------|--------|--------|-------|-----------|
| **免费私有仓库** | ✅ 无限 | ✅ 无限 | ✅ 无限（5 人限制） | ✅ 无限（5 人限制） |
| **内置 CI/CD** | ✅ Actions | ✅ 最强（原生内置） | ⚠️ Gitee Go | ✅ Pipelines |
| **自托管** | ❌（仅 Enterprise） | ✅ CE/EE 均可 | ✅ 企业版 | ❌ |
| **代码审查** | ✅ PR | ✅ MR（更细粒度） | ✅ PR | ✅ PR |
| **中国访问速度** | ⚠️ 慢（需代理） | ⚠️ 自托管可加速 | ✅ 极快 | ⚠️ 不稳定 |
| **开源生态** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **免费 CI 额度** | 2000 分钟/月 | 400 分钟/月 | 有限 | 500 分钟/月 |
| **最大文件限制** | 100 MB | 100 MB | 50 MB | 1 GB（有限制） |
| **LFS 免费额度** | 1 GB | 5 GB | 500 MB | 1 GB |

| 场景 | 推荐平台 | 理由 |
|------|----------|------|
| 🌍 开源项目 | GitHub | 最大开源社区，最高可见度 |
| 🇨🇳 中国团队内部项目 | Gitee | 国内访问快，数据本地化 |
| 🏢 企业私有部署 | GitLab CE | 免费自托管，权限粒度细 |
| 🔄 Atlassian 生态 | Bitbucket | 与 Jira 原生集成 |
| 🚀 需要最强 CI/CD | GitLab | CI 原生内置 |
| 🔐 高安全合规要求 | GitLab EE / GitHub Enterprise | 审计日志、SAML SSO |
| 👤 个人学习/作品集 | GitHub | 简历加分、技术社交 |

## 9. GitHub 生态

### 9.1 Actions / Pages

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - run: mvn clean verify
```

```yaml
# .github/dependabot.yml（依赖安全）
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/"
    schedule: { interval: "weekly" }
    labels: ["dependencies", "security"]
```

- **GitHub Pages**：Settings → Pages → Deploy from a branch → main → `/docs` → 站点 `https://username.github.io/repo/`；
- **GitHub Security 套件**：Dependabot alerts（漏洞告警）、Dependabot updates（自动 PR）、Code scanning（CodeQL）、Secret scanning（密钥检测）、Security advisories（安全公告）。

### 9.2 CODEOWNERS 与 PR 生态

```text
# .github/CODEOWNERS（monorepo 场景）
* @core-team
order-service/** @order-team
payment-service/** @payment-team
.github/workflows/* @devops-team
pom.xml @core-team
```

| CODEOWNERS 语法 | 含义 |
|----------------|------|
| `*` | 匹配所有文件 |
| `*.java` | 匹配所有 Java 文件 |
| `docs/*` | docs 目录下的直接子文件 |
| `docs/**` | docs 目录下所有嵌套文件 |
| `@team` / `@user` | 团队 / 个人（任一批准即可） |

PR 生态 5 大特性：Draft PR（未完成提前展示）、Review required（必须 N 人 approve）、CODEOWNERS（指定审查责任人）、Suggested changes（一键采纳代码建议）、Auto-merge（CI 通过后自动合并）。

### 9.3 Issue 模板与 Labels

```yaml
# .github/ISSUE_TEMPLATE/bug_report.md
---
name: Bug 报告
title: "[BUG] "
labels: bug
assignees: ''
---
## Bug 描述
## 复现步骤
## 期望行为
## 截图（可选）
## 环境（OS/JDK/浏览器）
## 附加信息
```

| Label 类别 | 示例 | 用途 |
|------------|------|------|
| 类型 | `bug`、`feature`、`enhancement` | 区分 Issue 类型 |
| 优先级 | `priority: critical/high/low` | 排期优先级 |
| 状态 | `needs triage`、`in progress`、`stale` | 工作流状态 |
| 模块 | `module: auth`、`module: payment` | 所属模块 |
| 难度 | `good first issue`、`help wanted` | 适合的贡献者 |

## 10. GitLab 与 Gitee

### 10.1 GitLab：DevOps 一体化

```text
GitLab 覆盖的 DevOps 环节:
Plan → Code → Build → Test → Release → Deploy → Monitor
 Issue  MR    CI/CD  Pipeline  Registry  K8s     Metrics
```

```yaml
# .gitlab-ci.yml
stages: [build, test, deploy]
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2"
cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths: [.m2/, target/]
build:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  script: [mvn compile]
  artifacts: { paths: [target/*.jar] }
test:
  stage: test
  script: [mvn test]
deploy_prod:
  stage: deploy
  script: [scp target/*.jar deploy@prod-server:/app/, ssh deploy@prod-server "systemctl restart myapp"]
  only: [tags]
  when: manual
```

| 对比点 | GitHub | GitLab |
|--------|--------|--------|
| 角色粒度 | 5 级 | 8 级 |
| 可见性控制 | 公开/私有 | 公开/内部/私有 |
| Group 嵌套 | ❌ | ✅ 多层 Group |
| 审批规则 | PR Approval | MR Approval Rules（按文件/按人数） |
| 开源 | 核心闭源 | CE 完全开源 |

### 10.2 Gitee（码云）— 国内首选

| 场景 | Gitee 的优势 |
|------|------------|
| 国内访问 | 速度快，不翻墙 |
| 高校教育 | 免费 Gitee 高校版 |
| 企业合规 | 数据在国内服务器 |
| 开源项目 | Gitee GVP（优秀开源项目）扶持 |

特色功能：Gitee Go（国内 CI/CD）、Gitee Pages（需实名）、Gitee Scan（代码质量分析）。

## 11. gh CLI 与多平台同步

### 11.1 gh CLI（GitHub 官方命令行）

```bash
gh auth login                    # 登录
# === PR 全流程 ===
gh pr create --title "feat: 新增订单导出" --body "### 变更内容..." --base develop --assignee @me
gh pr create --draft --title "WIP: 开发中"          # Draft PR
gh pr list / gh pr list --state merged / gh pr list --review-requested @me
gh pr checkout 123
gh pr review 123 --approve
gh pr review 123 --request-changes --body "需要修改"
gh pr merge 123 --squash         # 或 --rebase / --auto
# === Issue 操作 ===
gh issue create --title "Bug: CSV 中文乱码" --label bug
gh issue develop 456             # 创建分支来修复 Issue
# === Release / CI ===
gh release create v1.2.0 --title "v1.2.0" --notes "..." --target main
gh run list / gh run watch / gh run rerun
# === 高级：gh + jq ===
gh search prs --review-requested=@me --state=open \
  --json title,url,repository --jq '.[] | "\(.repository.name): \(.title)"'
```

> 💡 效率技巧：`alias gprc='gh pr create'`、`alias gprv='gh pr view --web'`、`alias gprm='gh pr merge --squash'`。

### 11.2 多平台同步

```bash
# 一键双推（pushurl 方案）
git remote add github https://github.com/user/project.git
git remote add gitee https://gitee.com/user/project.git
git remote set-url --add --push origin https://github.com/user/project.git
git remote set-url --add --push origin https://gitee.com/user/project.git
# 现在 git push origin main 会同时推送到 GitHub 和 Gitee

# Mirror Push 保持同步
git clone --mirror https://github.com/user/project.git
cd project.git
git remote set-url --push origin https://gitee.com/user/project.git
git push --mirror
```

## 12. Git LFS 大文件管理

### 12.1 原理与适用

LFS 用指针文件替换大文件，实际内容存独立服务器，避免仓库膨胀（Git 存二进制效率极低——每个版本存完整副本）。

| 文件类型 | 影响 | 建议方案 |
|----------|------|---------|
| JAR/WAR 包 | 仓库体积暴增 | Git LFS |
| Docker 镜像 | 完全不适用于 Git | 用镜像仓库 |
| 日志文件 | 不应提交 | `.gitignore` |
| PDF/图片资源 | 版本多时体积大 | Git LFS |
| Node_modules/JAR 依赖 | 不应提交 | 包管理器 + `.gitignore` |

### 12.2 配置与命令

```bash
git lfs install
git lfs track "*.jar" "*.zip" "*.tar.gz" "*.psd" "*.mp4"
git lfs track "assets/**"          # 整个目录
git add .gitattributes             # .gitattributes 含 filter=lfs 配置，需提交共享
git commit -m "chore: 配置Git LFS管理大文件"

git lfs track                  # 查看跟踪规则
git lfs ls-files               # 查看哪些文件被 LFS 管理
git lfs pull                   # 拉取所有 LFS 文件
GIT_LFS_SKIP_SMUDGE=1 git clone <url>   # 跳过 LFS 快速克隆
cd project && git lfs pull             # 按需拉取
git lfs prune                  # 删除本地 LFS 缓存
git lfs migrate import --include="*.jar" --everything   # 迁移已有历史大文件
git push --force origin main   # 迁移后强制推送
```

> ⚠️ **LFS 注意 4 条**：① 有存储和带宽限制（免费额度约 1GB/月，GitHub 超出 $5/50GB）；② LFS 文件一旦推送删除非常困难（LFS 仍保留对象）；③ review 时无法对比 LFS 文件差异（只能看指针变化）；④ 成员未装 `git-lfs` 时 clone 会下载指针文件而非实际内容。

## 13. 核心要点

> 🎯 **核心要点**：
> - 远程 = 同步点：`fetch`（只读安全）→ `pull --rebase`（线性同步）→ `push -u`（建跟踪）；
> - Fork 工作流三远程关系：origin（自己）、upstream（上游）、PR（回上游）；
> - PR 是协作核心机制：小而专注（200-400 行）、描述"做什么/为什么/怎么测"、合并即删分支；
> - 平台选型：开源→GitHub、国内→Gitee、私有化→GitLab CE、最强 CI→GitLab；
> - gh CLI 把 PR/Issue/Release 全流程命令化，配合 jq 可批量治理；
> - LFS 管大文件（jar/图片/媒体），但注意额度、删除困难、review 不可见三点。

## 14. 参考来源

- [Pro Git Book：远程仓库](https://git-scm.com/book/zh/v2/Git-基础-远程仓库的使用)
- [GitHub Flow 官方文档](https://docs.github.com/zh/get-started/using-github/github-flow)
- [GitHub CLI 文档](https://cli.github.com/manual/)
- [Git LFS 官方文档](https://git-lfs.com/)
- [GitLab CI 文档](https://docs.gitlab.com/ee/ci/)

---

**下一模块**：[07-Git日常管理与工作区实践](07-Git日常管理与工作区实践.md)　/　**返回总览**：[00-总览](00-Git知识体系总览.md)
