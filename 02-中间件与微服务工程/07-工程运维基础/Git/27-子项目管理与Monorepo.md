# 27-子项目管理与Monorepo
> 🎯 多仓库管理是微服务时代的基础工程能力 — 掌握Git Submodule、Git Subtree、Monorepo三种方案的原理、对比与选型，解决公共组件复用和多服务协同的版本管理难题

---

## 目录
1. [问题场景：多仓库管理的核心痛点](#1-问题场景多仓库管理的核心痛点)
2. [三大方案全景对比](#2-三大方案全景对比)
3. [Git Submodule：独立仓库引用](#3-git-submodule独立仓库引用)
4. [Git Subtree：仓库内嵌合并](#4-git-subtree仓库内嵌合并)
5. [Monorepo：单一仓库管理](#5-monorepo单一仓库管理)
6. [Java后端选型决策](#6-java后端选型决策)
7. [实战：公共组件库管理](#7-实战公共组件库管理)

---

## 1. 问题场景：多仓库管理的核心痛点

```text
典型Java微服务项目结构：
  ├── user-service        (独立Git仓库)
  ├── order-service       (独立Git仓库)
  ├── product-service     (独立Git仓库)
  ├── gateway-service     (独立Git仓库)
  └── common-lib          (公共组件：DTO、工具类、异常定义)

痛点：
  ❌ common-lib更新后，需要手动在每个服务中更新依赖
  ❌ 版本不一致：user-service用common v1.2，order-service用v1.0
  ❌ 跨服务修改：一个需求涉及3个服务，要在3个仓库分别PR
  ❌ CI/CD协调：发布时要确保所有服务使用正确的common-lib版本
```

---

## 2. 三大方案全景对比

| 维度 | Git Submodule | Git Subtree | Monorepo |
|------|:---:|:---:|:---:|
| **原理** | 主仓库记录子仓库的commit引用 | 子仓库历史合并到主仓库 | 所有代码在同一个仓库 |
| **子仓库独立性** | ✅ 完全独立 | ❌ 合并到主仓库 | ❌ 不独立 |
| **子仓库历史** | ✅ 保留 | ✅ 保留（可选择squash） | — |
| **更新子仓库** | ⚠️ 手动2步 | 双向同步命令 | — |
| **clone复杂度** | ⚠️ 需额外步骤 | ✅ 普通clone | ✅ 普通clone |
| **跨项目原子提交** | ❌ 不支持 | ✅ 支持 | ✅ 支持 |
| **仓库大小** | ✅ 小 | ⚠️ 中等 | ❌ 巨大 |
| **权限控制** | ✅ 细粒度 | ⚠️ 粗粒度 | ❌ 单一 |
| **学习曲线** | ⚠️ 中高 | ⚠️ 中 | ✅ 低 |
| **适合团队规模** | 中大型 | 小型 | 任何 |

---

## 3. Git Submodule：独立仓库引用

### 3.1 工作原理

```text
主仓库只记录子仓库的"指针"（某个commit SHA-1），不存储子仓库的实际代码。

.gitmodules 文件:
  [submodule "libs/common"]
    path = libs/common
    url = https://github.com/company/common-lib.git

主仓库.git/modules/ 存储子仓库的实际代码和历史
主仓库中 libs/common/ 是子仓库的working directory
```

### 3.2 完整操作流程

```bash
# === 添加子模块 ===
git submodule add https://github.com/company/common-lib.git libs/common
git commit -m "chore: 添加common-lib子模块"

# === clone含子模块的项目 ===
git clone --recurse-submodules https://github.com/company/main-project.git
# 或先clone再初始化
git clone <url>
cd main-project
git submodule init          # 初始化本地配置
git submodule update        # 拉取子模块代码

# === 更新子模块到最新 ===
cd libs/common
git pull origin main        # 子模块内拉取最新
cd ../..
git add libs/common          # 主仓库记录新的commit引用
git commit -m "chore: 更新common-lib到v1.2.0"

# === 批量操作所有子模块 ===
git submodule foreach 'git checkout main && git pull'
git submodule foreach 'mvn clean install -DskipTests'
```

### 3.3 Submodule生命周期管理

```bash
# 查看子模块状态
git submodule status
# +a1b2c3d libs/common (v1.1.0)     ← +表示有本地修改
#  a1b2c3d libs/common (v1.1.0)     ← 无本地修改

# 删除子模块
git submodule deinit libs/common      # 取消注册
git rm libs/common                    # 删除文件+记录
rm -rf .git/modules/libs/common       # 删除缓存
git commit -m "chore: 移除common-lib子模块"
```

### 3.4 Submodule 常见陷阱

| 陷阱 | 现象 | 解决 |
|------|------|------|
| clone后子模块目录为空 | 忘记`--recurse-submodules` | `git submodule update --init --recursive` |
| 子模块处于detached HEAD | 切到commit而非分支 | `cd libs/common && git checkout main` |
| 忘记推送子模块变更 | 别人的主仓库指向不存在的commit | 先push子模块，再push主仓库 |
| 合并冲突于`.gitmodules` | 两人同时改了子模块配置 | 手动解决冲突，选择正确的URL |

---

## 4. Git Subtree：仓库内嵌合并

### 4.1 Subtree vs Submodule

```text
Submodule: 主仓库"引用"子仓库 → 指针
Subtree: 主仓库"包含"子仓库 → 代码实打实在主仓库里

Subtree的核心思想：
  把外部仓库的历史合并到主仓库的子目录中
  → 看起来就像一直都是主仓库的一部分
```

### 4.2 核心操作

```bash
# === 添加子仓库到主仓库 ===
git subtree add --prefix=libs/common \
  https://github.com/company/common-lib.git main --squash

# --squash: 把子仓库的所有历史压缩为1个commit
# --prefix: 子仓库内容放在哪个目录

# === 从子仓库拉取更新 ===
git subtree pull --prefix=libs/common \
  https://github.com/company/common-lib.git main --squash

# === 推送修改回子仓库 ===
git subtree push --prefix=libs/common \
  https://github.com/company/common-lib.git main
```

### 4.3 Submodule vs Subtree 决策

| 你的需求 | 选Submodule | 选Subtree |
|----------|:---:|:---:|
| 子仓库需要独立开发和CI/CD | ✅ | — |
| 不想团队成员学习新命令 | — | ✅ |
| 需要原子提交（主仓库+子仓库一起commit） | ❌ | ✅ |
| 子仓库有大量独立历史 | ✅ | — |
| 项目比较小、依赖关系简单 | — | ✅ |

---

## 5. Monorepo：单一仓库管理

### 5.1 Monorepo是什么

> Monorepo = 将多个项目/服务的代码放在同一个Git仓库中管理。

```text
Monorepo目录结构：
  /
  ├── services/
  │   ├── user-service/        (含 pom.xml)
  │   ├── order-service/       (含 pom.xml)
  │   └── gateway-service/     (含 pom.xml)
  ├── libs/
  │   └── common/              (公共组件)
  ├── build/                   (统一构建脚本)
  └── pom.xml                  (父POM，管理所有子模块)
```

### 5.2 Monorepo 优势与挑战

| 优势 | 挑战 |
|------|------|
| ✅ **原子提交**：一次commit修改多个服务 | ❌ **仓库巨大**：clone时间长（可用shallow clone解决） |
| ✅ **统一版本**：所有服务用相同的common版本 | ❌ **权限粗**：无法按目录控制读写（GitHub支持CODEOWNERS） |
| ✅ **跨服务重构**：IDE全局搜索+替换 | ❌ **CI复杂**：需智能检测哪些服务变更了 |
| ✅ **简化依赖**：不需要Maven发布common版本 | ❌ **构建时间长**：需增量构建 |
| ✅ **统一规范**：一套CI/代码风格/commit规范 | ❌ **需工具支持**：Bazel/Nx/Turborepo |

### 5.3 Monorepo CI优化

```yaml
# GitHub Actions: 只构建变更的服务
name: CI
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
            order-service: 'services/order-service/**'
            common-lib: 'libs/common/**'

  build-user-service:
    needs: changes
    if: ${{ needs.changes.outputs.user-service == 'true' || needs.changes.outputs.common-lib == 'true' }}
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - run: cd services/user-service && mvn verify
```

### 5.4 Monorepo 工具选择

| 工具 | 定位 | 适合 |
|------|------|------|
| **Maven/Gradle 多模块** | Java生态原生 | ✅ Java Monorepo首选 |
| **Nx** | 前端+全栈 | 前端为主，后端为辅 |
| **Turborepo** | 前端Monorepo | JS/TS项目 |
| **Bazel** | Google级构建工具 | 超大型Monorepo |

---

## 6. Java后端选型决策

```text
决策流程图：

你的团队规模？
  ├── <5人 → Monorepo（Maven多模块）← 最简单
  │
  ├── 5-20人 → 看公共组件复用情况
  │   ├── 公共组件稳定、更新少 → Submodule
  │   └── 公共组件频繁变动 → Monorepo（Maven多模块）
  │
  └── >20人 → 多仓库 + Submodule
      ├── 每个微服务独立仓库
      ├── common-lib用Submodule引用
      └── 或内部Maven仓库发布common-lib的jar包（推荐！）

大多数Java团队的实际情况：
  → 公共组件发布为jar包（内部Nexus/Artifactory）← 最成熟的做法
  → 只有在需要"源码级引用"时才用Submodule/Subtree
```

---

## 7. 实战：公共组件库管理

### 7.1 推荐方案：内部Maven仓库

```xml
<!-- 各微服务pom.xml中引用公共组件 -->
<dependency>
    <groupId>com.company</groupId>
    <artifactId>common-lib</artifactId>
    <version>1.2.0</version>
</dependency>
```

```bash
# common-lib发布流程：
cd common-lib
# 1. 更新版本号
mvn versions:set -DnewVersion=1.2.1
# 2. 本地验证
mvn clean verify
# 3. 发布到内部仓库
mvn deploy
# 4. 各微服务更新依赖版本
```

### 7.2 Submodule方案：适合源码级协作

```bash
# 当多个团队需要频繁修改common-lib时，用Submodule
git submodule add git@github.com:company/common-lib.git libs/common

# 日常使用：
# 开发common-lib时，直接在libs/common/下修改
# 修改完push到common-lib仓库
# 其他项目git submodule update即可同步
```

### 7.3 Monorepo方案：小团队首选

```xml
<!-- 父POM -->
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
# 一键构建所有模块
mvn clean verify

# 只构建user-service
mvn -pl services/user-service clean verify

# 构建user-service及其依赖的common
mvn -pl services/user-service -am clean verify
```

---

> 🎯 **选型第一原则**：能用jar包依赖解决的，不要用Submodule/Subtree。源码级引用只在频繁修改公共组件且需要原子提交时才用。小团队优先Monorepo（Maven多模块），大团队用多仓库+内部Maven仓库。
