# 09-Git 标签、子模块与 Monorepo
> 版本里程碑（tag/SemVer/Releases）、多仓库共享代码（submodule/subtree）、大仓管理（Monorepo）——发布与多仓治理全景

## 📚 目录
1. [标签：轻量 vs 注解](#1-标签轻量-vs-注解)
2. [tag 命令全集](#2-tag-命令全集)
3. [SemVer 语义化版本](#3-semver-语义化版本)
4. [GitHub Releases 与 Changelog 自动生成](#4-github-releases-与-changelog-自动生成)
5. [多仓库共享代码三种策略](#5-多仓库共享代码三种策略)
6. [Git Submodule 完整命令](#6-git-submodule-完整命令)
7. [Git Subtree](#7-git-subtree)
8. [Monorepo](#8-monorepo)
9. [Java 后端选型决策](#9-java-后端选型决策)
10. [多仓库管理工具](#10-多仓库管理工具)
11. [核心要点](#11-核心要点)
12. [参考来源](#12-参考来源)

## 1. 标签：轻量 vs 注解

标签 = 固定指针（分支可移动，tag 永远钉在某个 commit）。

| 特性 | 轻量标签 (Lightweight) | 注解标签 (Annotated) |
|------|----------------------|---------------------|
| 本质 | 只是一个 ref | 独立的 tag 对象 |
| 元数据 | 只有 commit SHA | tagger、日期、message、GPG 签名 |
| 是否可签名 | 否 | 是 |
| 推荐 | 临时标记、个人使用 | **生产发布唯一推荐** |

> ⚠️ **标签与分支的区别**：标签**不可移动**，一旦创建永远指向同一个 commit；分支可移动。不要用标签做"当前版本"概念——用分支。标签只用于**标记历史中的里程碑**。

## 2. tag 命令全集

```bash
# === 创建 ===
git tag v1.0.0                                    # 轻量标签
git tag -a v1.0.0 -m "Release version 1.0.0"      # 注解标签（⭐ 发布用）
git tag -a v0.9.0 a1b2c3d -m "Beta release"       # 给过去的 commit 打标签
git tag -s v1.0.0 -m "已签名发布"                  # GPG 签名标签（企业级安全）

# === 列出/查看 ===
git tag                                          # 列出所有标签
git tag -l "v1.*"                                # 按模式过滤
git show v1.0.0                                  # 查看标签详情
git tag -v v1.0.0                                # 验证签名

# === 推送 ===
git push origin v1.0.0                           # 推送单个
git push origin --tags                           # 推送所有未推送的标签

# === 删除 ===
git tag -d v1.0.0                                # 删除本地
git push origin --delete v1.0.0                  # 删除远程（方式一）
git push origin :refs/tags/v1.0.0                # 删除远程（方式二）

# === 检出 ===
git checkout v1.0.0                              # 检出标签（进入 detached HEAD）
git checkout -b hotfix-from-v1.0.0 v1.0.0        # 基于标签创建分支
git fetch --tags                                 # 拉取远程标签
```

## 3. SemVer 语义化版本

```text
v2.3.1
 │ │ │
 │ │ └── PATCH: 向后兼容的 bug 修复
 │ └──── MINOR: 向后兼容的新功能
 └────── MAJOR: 不兼容的 API 变更
```

| 版本号变动 | 触发场景 | 示例 |
|-----------|---------|------|
| MAJOR +1 | 不兼容的 API 变更、数据库表结构变更 | v1.0.0 → v2.0.0 |
| MINOR +1, PATCH=0 | 向后兼容的新功能 | v2.0.0 → v2.1.0 |
| PATCH +1 | 向后兼容的 Bug 修复 | v2.1.0 → v2.1.1 |

先行版本与构建元数据：`v1.0.0-alpha.1`、`v1.0.0-beta.2`、`v2.0.0-rc.1`、`v1.5.0+build.20250101`（构建元数据不参与比较）。

版本优先级：`1.0.0 < 2.0.0 < 2.1.0 < 2.1.1`；`1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-beta < 1.0.0`。

## 4. GitHub Releases 与 Changelog 自动生成

### 4.1 gh CLI 创建 Release

```bash
gh release create v1.0.0 \
  --title "Release v1.0.0" \
  --notes "## What's new
- feat: add dark mode support
- fix: resolve login timeout issue" \
  --target main

gh release create v1.0.0 --title "v1.0.0" dist/app-v1.0.0.exe   # 附带二进制
gh release create v2.0.0-beta.1 --prerelease --title "v2.0.0 Beta 1"   # 预发布
```

### 4.2 Changelog 自动生成（三种方案）

```bash
# 方案 1：standard-version（Node）
npm install --save-dev standard-version
# package.json scripts: "release": "standard-version"
npm run release
# 自动：bump 版本号 → 更新 CHANGELOG.md → commit "chore(release): 1.2.0" → tag v1.2.0
npx standard-version --release-as minor    # 0.1.0 → 0.2.0
npx standard-version --dry-run             # 预览不执行
git push --follow-tags origin main

# 方案 2：git-cliff（Rust，极快）
cargo install git-cliff
git cliff -o CHANGELOG.md
git cliff --latest --prepend CHANGELOG.md
```

```yaml
# 方案 3：GitHub Actions 自动发布
# .github/workflows/release.yml
on:
  push:
    tags: ['v*']
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - name: Generate Changelog
        run: |
          PREV_TAG=$(git describe --tags --abbrev=0 HEAD^ 2>/dev/null || echo "")
          if [ -z "$PREV_TAG" ]; then LOG=$(git log --oneline --no-decorate)
          else LOG=$(git log --oneline --no-decorate ${PREV_TAG}..HEAD); fi
          echo "CHANGES<<EOF" >> $GITHUB_OUTPUT
          echo "$LOG" >> $GITHUB_OUTPUT
          echo "EOF" >> $GITHUB_OUTPUT
      - uses: softprops/action-gh-release@v1
        with:
          body: ${{ steps.changelog.outputs.CHANGES }}
          generate_release_notes: true
```

## 5. 多仓库共享代码三种策略

| 维度 | Git Submodule | Git Subtree | Monorepo |
|------|:---:|:---:|:---:|
| 原理 | 主仓库记录子仓库的 commit 引用 | 子仓库历史合并到主仓库 | 所有代码在同一个仓库 |
| 子仓库独立性 | ✅ 完全独立 | ❌ 合并到主仓库 | ❌ 不独立 |
| 子仓库历史 | ✅ 保留 | ✅ 保留（可 squash） | — |
| 更新子仓库 | ⚠️ 手动 2 步 | 双向同步命令 | — |
| clone 复杂度 | ⚠️ 需额外步骤 | ✅ 普通 clone | ✅ 普通 clone |
| 跨项目原子提交 | ❌ | ✅ | ✅ |
| 权限控制 | ✅ 细粒度 | ⚠️ 粗粒度 | ❌ 单一 |
| 适合团队规模 | 中大型 | 小型 | 任何 |

| 场景 | 推荐 |
|------|------|
| 子仓库需独立开发/CI/CD | Submodule |
| 不想团队学新命令 | Subtree |
| 需要原子提交 | Subtree / Monorepo |
| 子仓库有大量独立历史 | Submodule |
| 项目小、依赖简单 | Subtree |

## 6. Git Submodule 完整命令

### 6.1 核心概念

```text
主仓库 (main-repo)
├── src/
├── lib/
│   └── shared-utils/    ← 子模块，指向 shared-utils 仓库的 commit a1b2c3d
├── .gitmodules          ← 子模块配置
└── package.json
```

### 6.2 添加子模块

```bash
git submodule add https://github.com/team/shared-utils.git lib/shared-utils
# 发生三件事：克隆子仓库到目录、创建 .gitmodules、在 index 记录 commit SHA

cat .gitmodules
# [submodule "lib/shared-utils"]
#     path = lib/shared-utils
#     url = https://github.com/team/shared-utils.git

git add .gitmodules lib/shared-utils
git commit -m "chore: add shared-utils as submodule"
```

> 💡 27 独有细节：主仓库 `.git/modules/` 存储子仓库的实际代码和历史；主仓库中 `libs/common/` 是子仓库的 working directory。

### 6.3 克隆 / 更新 / 状态

```bash
# 克隆含子模块的仓库
git clone --recurse-submodules https://github.com/team/main-repo.git
# 或先 clone 再初始化
git submodule init
git submodule update --init --recursive

# 更新子模块到最新
cd lib/shared-utils
git fetch && git checkout main
cd ../..
git add lib/shared-utils
git commit -m "chore: update shared-utils to v2.1.0"
# 或一行命令
git submodule update --remote lib/shared-utils

# 状态与批量操作
git submodule status
#  a1b2c3d lib/shared-utils (v2.0.0)
# +e4f5g6h lib/shared-utils (v2.0.0-1-g...)  ← + 表示有本地未提交修改
git submodule foreach 'git checkout main && git pull'
git submodule foreach 'mvn clean install -DskipTests'
git submodule update --remote --recursive
```

### 6.4 删除子模块

```bash
git submodule deinit lib/shared-utils      # 取消注册
git rm lib/shared-utils                    # 删除文件+记录
rm -rf .git/modules/libs/common            # 删除缓存
git commit -m "chore: remove shared-utils submodule"
```

### 6.5 子模块的坑

```text
❌ 忘记 --recurse-submodules，克隆后代码不完整
❌ 子模块改了没提交，主仓库记录了新 SHA 但子模块没 push
❌ git pull 不会自动更新子模块（需要 git submodule update）
❌ 切换分支后子模块可能处于旧 commit
```

```bash
# 自动更新配置（推荐）
git config --global submodule.recurse true
git config --global status.submoduleSummary true
git config --global diff.submodule log
```

## 7. Git Subtree

| 维度 | Submodule | Subtree |
|------|----------|---------|
| 代码存储 | "指针"，代码在独立仓库 | "拷贝"，代码在主仓库中 |
| 克隆 | 需要额外步骤 | `git clone` 直接获得完整代码 |
| 更新 | 手动 + 子仓库推送 | `git subtree pull/push` |
| 适合 | 明确边界、独立发布 | 紧密耦合、不需要独立仓库 |

```bash
# === 添加子仓库到主仓库 ===
git subtree add --prefix=libs/common \
  https://github.com/company/common-lib.git main --squash
# --squash: 把子仓库的所有历史压缩为 1 个 commit

# === 从子仓库拉取更新 ===
git subtree pull --prefix=libs/common \
  https://github.com/company/common-lib.git main --squash

# === 推送修改回子仓库 ===
git subtree push --prefix=libs/common \
  https://github.com/company/common-lib.git main
```

## 8. Monorepo

### 8.1 概念与结构

所有项目/库/服务放在一个 Git 仓库：

```text
monorepo/
├── apps/
│   ├── web/             # 前端应用
│   ├── api/             # 后端服务
│   └── mobile/          # 移动端
├── packages/
│   ├── shared-utils/    # 共享工具库
│   └── ui-components/   # UI 组件库
├── services/            # Java 微服务（含各 pom.xml）
├── libs/                # 共享库
└── pom.xml              # 父 POM（Maven 多模块）
```

### 8.2 优势 vs 挑战

| 优势 | 挑战 |
|------|------|
| 统一版本控制，一次 commit 跨所有项目 | 仓库体积巨大（浅克隆/部分检出解决） |
| 原子化重构（改 shared → 所有 app 同步） | CI 时间长（需增量构建） |
| 统一的 lint/test/build 配置 | 权限管理困难（CODEOWNERS 解决） |
| 简化依赖（不需要发布 common 版本） | 工具链要求高（Bazel/Nx/Turborepo） |

### 8.3 工具对比与关键实践

| 工具 | 生态 | 作用 |
|------|------|------|
| **Turborepo** | JS/TS | 增量构建、并行任务、缓存 |
| **Nx** | JS/TS | 构建系统 + 依赖图 + 代码生成 |
| **Bazel** | 多语言 | Google 的构建系统，极致增量 |
| **Maven/Gradle 多模块** | Java | **Java Monorepo 首选** |

```bash
# 部分克隆（只克隆需要的部分）
git clone --filter=blob:none https://github.com/org/monorepo.git
# Sparse Checkout（只检出部分目录）
git sparse-checkout init --cone
git sparse-checkout set apps/web packages/shared-utils
```

```yaml
# Monorepo CI 优化：只构建变更的服务（dorny/paths-filter）
on: [push]
jobs:
  changes:
    runs-on: ubuntu-latest
    outputs:
      user-service: ${{ steps.filter.outputs.user-service }}
    steps:
      - uses: dorny/paths-filter@v2
        id: filter
        with:
          filters: |
            user-service: 'services/user-service/**'
            common-lib: 'libs/common/**'
  build-user-service:
    needs: changes
    if: ${{ needs.changes.outputs.user-service == 'true' || needs.changes.outputs.common-lib == 'true' }}
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - run: cd services/user-service && mvn verify
```

## 9. Java 后端选型决策

```text
你的团队规模？
  ├── <5人 → Monorepo（Maven多模块）← 最简单
  ├── 5-20人 → 看公共组件复用情况
  │   ├── 公共组件稳定、更新少 → Submodule
  │   └── 公共组件频繁变动 → Monorepo（Maven多模块）
  └── >20人 → 多仓库 + Submodule
      ├── 每个微服务独立仓库
      ├── common-lib 用 Submodule 引用
      └── 或内部 Maven 仓库发布 common-lib 的 jar 包（推荐！）

大多数 Java 团队的实际情况：
  → 公共组件发布为 jar 包（内部 Nexus/Artifactory）← 最成熟的做法
  → 只有在需要"源码级引用"时才用 Submodule/Subtree
```

### 方案 A：内部 Maven 仓库（最推荐）

```xml
<!-- 各微服务 pom.xml 中引用公共组件 -->
<dependency>
    <groupId>com.company</groupId>
    <artifactId>common-lib</artifactId>
    <version>1.2.0</version>
</dependency>
```

```bash
cd common-lib
mvn versions:set -DnewVersion=1.2.1   # 1. 更新版本号
mvn clean verify                       # 2. 本地验证
mvn deploy                             # 3. 发布到内部仓库
# 4. 各微服务更新依赖版本
```

### 方案 C：Monorepo 父 POM（小团队首选）

```xml
<groupId>com.company</groupId>
<artifactId>platform</artifactId>
<version>1.0.0</version>
<packaging>pom</packaging>

<modules>
    <module>libs/common</module>
    <module>services/user-service</module>
    <module>services/order-service</module>
</modules>
```

```bash
mvn clean verify                                   # 一键构建所有模块
mvn -pl services/user-service clean verify         # 只构建 user-service
mvn -pl services/user-service -am clean verify     # 构建 user-service 及其依赖的 common
```

> 🎯 **选型第一原则**：能用 jar 包依赖解决的不要用 Submodule/Subtree；源码级引用只在频繁修改公共组件且需要原子提交时才用；小团队优先 Monorepo（Maven 多模块），大团队用多仓库 + 内部 Maven 仓库。

## 10. 多仓库管理工具

### 10.1 git-repo（Google Android 工具）

```bash
repo init -u https://github.com/org/manifest.git   # 从 manifest 仓库获取所有子仓库定义
repo sync                                           # 同步所有仓库
repo start feature/upgrade --all                    # 在所有仓库创建分支
repo status                                         # 查看所有仓库状态
```

### 10.2 myrepos (mr)

```bash
sudo apt install myrepos    # Debian/Ubuntu；brew install myrepos（Mac）
mr register ~/projects/repo1
mr fetch       # 所有仓库 git fetch
mr status      # 所有仓库 git status
mr push        # 所有仓库 git push
```

### 10.3 批量仓库操作脚本（微服务场景）

```bash
#!/bin/bash
# batch-git.sh — 对所有微服务仓库执行同一 Git 操作

REPOS=("user-service" "order-service" "product-service" "gateway-service" "common-lib")
ACTION=${1:-status}

for repo in "${REPOS[@]}"; do
    echo "=== $repo ==="
    cd "$repo" || continue
    case $ACTION in
        status)  git status --short ;;
        pull)    git fetch --all --prune && git pull --rebase ;;
        branch)  git branch --all ;;
        *)       git "$@" ;;
    esac
    cd ..
done
```

## 11. 核心要点

> 🎯 **核心要点**：
> - 标签是"不可移动的里程碑"：注解标签 + SemVer + GitHub Release 是发布三件套；
> - Changelog 自动生成：standard-version（JS）/ git-cliff（Rust）/ Actions 模板三选一；
> - 共享代码三策略：Submodule（指针/独立）、Subtree（拷贝/简单）、Monorepo（原子/统一）；
> - Submodule 五坑：clone 缺 `--recurse`、子模块未 push、pull 不更新、detached HEAD、删除留缓存；
> - Java 首选：内部 Maven 仓库发 jar 包 > Monorepo（小团队）> Submodule（大团队）；
> - 多仓库治理：git-repo / mr / 批量脚本三件套按规模选。

## 12. 参考来源

- [Pro Git Book：子模块](https://git-scm.com/book/zh/v2/Git-工具-子模块)
- [SemVer 语义化版本规范](https://semver.org/lang/zh-CN/)
- [standard-version 文档](https://github.com/conventional-changelog/standard-version)
- [git-cliff 文档](https://git-cliff.org/)
- [Trunk-Based Development：Monorepo](https://trunkbaseddevelopment.com/monorepos/)

---

**下一模块**：[10-Git调试取证与排错](10-Git调试取证与排错.md)　/　**返回总览**：[00-总览](00-Git知识体系总览.md)
