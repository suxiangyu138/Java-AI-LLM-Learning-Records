# 04 - GitHub Actions CI/CD 实战

> GitHub Actions 是内置于 GitHub 的 CI/CD 平台——从编译、测试到自动部署，从定时任务到事件驱动，无需额外服务器即可实现完整的 DevOps 流水线。

---

## 目录

1. [GitHub Actions 核心概念](#1-github-actions-核心概念)
2. [Workflow 语法全解](#2-workflow-语法全解)
3. [触发事件详解](#3-触发事件详解)
4. [Runner 类型与选择](#4-runner-类型与选择)
5. [Java 后端 CI 实战](#5-java-后端-ci-实战)
6. [自动部署 CD 实战](#6-自动部署-cd-实战)
7. [Actions Marketplace 精选](#7-actions-marketplace-精选)
8. [矩阵构建与并行策略](#8-矩阵构建与并行策略)
9. [Secrets 与环境保护](#9-secrets-与环境保护)
10. [常见面试题](#10-常见面试题)

---

## 1. GitHub Actions 核心概念

### 1.1 六个核心组件

```
┌──────────────────────────────────────────────────────────┐
│                     GitHub Actions                        │
│                                                           │
│  Event (事件)     → 触发 Workflow 运行                     │
│    push, pull_request, schedule, workflow_dispatch, ...    │
│                                                           │
│  Workflow (工作流) → .github/workflows/*.yml 定义的流水线    │
│    一个仓库可以有多个 Workflow                              │
│                                                           │
│  Job (作业)      → Workflow 中的执行单元                   │
│    默认并行运行，可配置依赖关系 needs                         │
│                                                           │
│  Step (步骤)     → Job 中的执行步骤                        │
│    可以是 run (Shell命令) 或 uses (GitHub Action)            │
│                                                           │
│  Runner (运行器)  → 执行 Job 的机器                         │
│    ubuntu-latest / windows-latest / macos-latest / self-hosted│
│                                                           │
│  Action (动作)   → 可复用的执行单元                         │
│    actions/checkout, actions/setup-java, ...               │
└──────────────────────────────────────────────────────────┘
```

### 1.2 层次关系

```
Repository
  └── .github/workflows/
       ├── ci.yml          (Workflow: CI)
       │    ├── Job: build  (runs-on: ubuntu-latest)
       │    │    ├── Step: Checkout     ← uses: actions/checkout@v4
       │    │    ├── Step: Setup Java   ← uses: actions/setup-java@v4
       │    │    ├── Step: Compile      ← run: mvn compile
       │    │    └── Step: Test         ← run: mvn test
       │    └── Job: docker-build (needs: build)
       │
       ├── deploy.yml      (Workflow: Deploy)
       └── nightly.yml     (Workflow: Nightly安全检查)
```

---

## 2. Workflow 语法全解

### 2.1 完整 YAML 结构

```yaml
# .github/workflows/ci.yml
name: Java CI                          # Workflow 名称（显示在 Actions 页面）

on:                                     # ⭐ 触发器
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:                                    # 全局环境变量
  JAVA_VERSION: '17'
  MAVEN_OPTS: '-Xmx1024m'

jobs:                                   # Job 定义
  build:
    name: Build on ${{ matrix.os }}     # Job 显示名称（支持表达式）
    runs-on: ubuntu-latest              # Runner 操作系统

    strategy:                           # 矩阵策略
      matrix:
        os: [ubuntu-latest, windows-latest]
        java: [17, 21]

    timeout-minutes: 30                 # 超时

    steps:                              # 步骤列表
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Java ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Build with Maven
        run: mvn -B package --file pom.xml

  # 第二个 Job（依赖 build）
  docker-build:
    needs: build                        # 依赖关系：build 成功后执行
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'  # 条件：仅在 main 分支
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker image
        run: docker build -t myapp:${{ github.sha }} .
```

### 2.2 表达式语法

```yaml
# ═══ GitHub Actions 表达式 — ${{ }} ═══

# 上下文对象
${{ github.ref }}              # refs/heads/main
${{ github.sha }}              # commit SHA
${{ github.event_name }}       # push / pull_request
${{ github.actor }}            # 触发者用户名
${{ github.repository }}       # owner/repo
${{ github.run_id }}           # 本次运行 ID
${{ github.run_number }}       # 运行序号

# 环境变量
${{ env.JAVA_VERSION }}

# Secrets
${{ secrets.DOCKER_PASSWORD }}

# Job 输出
${{ jobs.build.outputs.artifact_id }}

# 条件判断
if: ${{ github.ref == 'refs/heads/main' }}
if: ${{ success() }}           # 前面的步骤/依赖 Job 都成功
if: ${{ failure() }}           # 前面的步骤/依赖 Job 有失败
if: ${{ always() }}            # 无论成败都执行
if: ${{ cancelled() }}         # 被取消时执行
if: ${{ !startsWith(github.ref, 'refs/tags/') }}

# 函数
${{ startsWith('hello', 'he') }}   # true
${{ contains('hello world', 'world') }}  # true
${{ format('v{0}-{1}', 1, 0) }}    # v1-0
${{ join(array, ', ') }}
```

---

## 3. 触发事件详解

### 3.1 核心事件

```yaml
# ═══ 1. push — 代码推送 ═══
on:
  push:
    branches: [main, develop]
    tags: ['v*']                # 只触发版本标签
    paths:                      # 只有这些文件变更才触发
      - 'src/**'
      - 'pom.xml'
    paths-ignore:               # 忽略这些文件
      - 'docs/**'
      - '**.md'

# ═══ 2. pull_request — PR 活动 ═══
on:
  pull_request:
    types: [opened, synchronize, reopened, closed]
    branches: [main]

# ═══ 3. schedule — 定时执行（UTC 时间）════
on:
  schedule:
    - cron: '0 2 * * *'         # 每天 UTC 2:00（北京时间 10:00）
    - cron: '0 0 * * 1'         # 每周一 UTC 0:00

# ═══ 4. workflow_dispatch — 手动触发 ═══
on:
  workflow_dispatch:
    inputs:
      environment:
        description: '部署环境'
        required: true
        type: choice
        options: [staging, production]
      dry-run:
        description: '预览模式（不实际部署）'
        type: boolean
        default: true

# ═══ 5. workflow_run — 其他 Workflow 完成后触发 ═══
on:
  workflow_run:
    workflows: ['Java CI']
    types: [completed]

# ═══ 6. release — 发布 ═══
on:
  release:
    types: [published, created]
```

### 3.2 事件过滤与组合

```yaml
# 多个事件 OR 组合
on: [push, pull_request]

# 排除特定分支的 push
on:
  push:
    branches-ignore:
      - 'dependabot/**'
      - 'gh-pages'

# push + paths 组合（只有源码变更才跑 CI）
on:
  push:
    paths:
      - 'src/**'
      - 'pom.xml'
      - '.github/workflows/**'
```

---

## 4. Runner 类型与选择

### 4.1 GitHub-hosted Runner

| Runner | OS | 处理器 | 内存 | 存储 | 费用 |
|--------|-----|--------|------|------|------|
| `ubuntu-latest` | Ubuntu 22.04 | 4 vCPU | 16GB | 14GB | Free/分钟 |
| `windows-latest` | Windows Server 2022 | 4 vCPU | 16GB | 14GB | ×2分钟 |
| `macos-latest` | macOS 14 (M1) | 4 vCPU | 14GB | 14GB | ×10分钟 |

### 4.2 Self-hosted Runner

```bash
# 自托管 Runner — 用自己的服务器执行

# 1. 在 GitHub 仓库 Settings → Actions → Runners → New self-hosted runner
# 2. 按指引下载并配置（支持 Linux/macOS/Windows）
# 3. 运行：
./run.sh

# Workflow 中使用：
runs-on: self-hosted

# 或带标签的自建 Runner：
runs-on: [self-hosted, linux, gpu]  # 匹配所有标签

# ⚠️ 注意：公开仓库的自建 Runner 有安全风险（任何人都可在 PR 中执行命令）
# → 务必在 Settings → Actions → Fork pull request workflows 中限制
```

---

## 5. Java 后端 CI 实战

### 5.1 标准 Java CI Pipeline

```yaml
# .github/workflows/java-ci.yml
name: Java CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    services:                              # ⭐ 服务容器（数据库等）
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test123
          MYSQL_DATABASE: testdb
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=3

    env:
      DB_URL: jdbc:mysql://localhost:3306/testdb
      DB_USER: root
      DB_PASSWORD: test123

    steps:
      # 1. 检出代码
      - uses: actions/checkout@v4

      # 2. 设置 JDK
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      # 3. 缓存 Maven 依赖（加速构建）
      - uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-maven-

      # 4. 编译
      - name: Compile
        run: mvn -B compile

      # 5. 代码风格检查
      - name: Checkstyle
        run: mvn checkstyle:check

      # 6. 单元测试 + 集成测试
      - name: Test
        run: mvn -B test

      # 7. 打包
      - name: Package
        run: mvn -B package -DskipTests

      # 8. 上传测试报告
      - name: Upload Test Report
        if: always()         # 即使测试失败也上传
        uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: target/surefire-reports/

      # 9. 上传构建产物
      - name: Upload JAR
        uses: actions/upload-artifact@v4
        with:
          name: app-jar
          path: target/*.jar
```

### 5.2 代码质量门禁集成

```yaml
# SonarCloud 集成
- name: SonarCloud Scan
  uses: sonarsource/sonarcloud-github-action@master
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  with:
    args: >
      -Dsonar.projectKey=myorg_myapp
      -Dsonar.organization=myorg
      -Dsonar.java.coveragePlugin=jacoco
      -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

# Codecov 覆盖率上传
- uses: codecov/codecov-action@v4
  with:
    token: ${{ secrets.CODECOV_TOKEN }}
    files: target/site/jacoco/jacoco.xml
```

---

## 6. 自动部署 CD 实战

### 6.1 Docker 构建与推送

```yaml
name: Docker Build & Push

on:
  push:
    tags: ['v*']       # 只在打版本标签时构建镜像

jobs:
  docker:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # 1. 登录 Docker Hub / GitHub Container Registry
      - name: Login to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      # 2. 提取 metadata（标签、版本号等）
      - name: Docker meta
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository }}
          tags: |
            type=ref,event=tag
            type=sha
            type=raw,value=latest

      # 3. 构建并推送
      - name: Build and Push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

### 6.2 SSH 远程部署

```yaml
name: Deploy to Server

on:
  workflow_run:
    workflows: ['Docker Build & Push']
    types: [completed]
    branches: [main]

jobs:
  deploy:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest
    steps:
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd /opt/app
            docker compose pull
            docker compose up -d --remove-orphans
            docker system prune -f
```

### 6.3 Release 自动发布

```yaml
name: Create Release

on:
  push:
    tags: ['v*']

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # 1. 编译打包
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin', cache: 'maven' }
      - run: mvn -B package -DskipTests

      # 2. 自动生成 Changelog + 创建 Release
      - name: Create Release
        uses: softprops/action-gh-release@v2
        with:
          name: Release ${{ github.ref_name }}
          body_path: CHANGELOG.md
          files: target/*.jar
          generate_release_notes: true
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 7. Actions Marketplace 精选

| Action | 用途 | 使用频率 |
|--------|------|---------|
| `actions/checkout@v4` | 检出代码 | ⭐⭐⭐⭐⭐ |
| `actions/setup-java@v4` | 安装 JDK + 自动缓存 | ⭐⭐⭐⭐⭐ |
| `actions/setup-node@v4` | 安装 Node.js | ⭐⭐⭐⭐⭐ |
| `actions/cache@v4` | 手动缓存依赖 | ⭐⭐⭐⭐ |
| `actions/upload-artifact@v4` | 上传构建产物 | ⭐⭐⭐⭐ |
| `actions/download-artifact@v4` | 下载之前的产物 | ⭐⭐⭐ |
| `docker/login-action@v3` | 登录容器仓库 | ⭐⭐⭐⭐ |
| `docker/build-push-action@v5` | 构建+推送 Docker 镜像 | ⭐⭐⭐⭐ |
| `docker/metadata-action@v5` | 生成 Docker 标签/注解 | ⭐⭐⭐ |
| `softprops/action-gh-release@v2` | 创建 Release | ⭐⭐⭐ |
| `appleboy/ssh-action@v1` | SSH 远程执行命令 | ⭐⭐⭐ |
| `codecov/codecov-action@v4` | 上传覆盖率 | ⭐⭐⭐ |
| `github/codeql-action/*` | 安全代码扫描 | ⭐⭐⭐ |
| `stefanzweifel/git-auto-commit-action` | 自动 commit 变更 | ⭐⭐ |
| `peter-evans/create-pull-request` | 自动创建 PR | ⭐⭐ |
| `reviewdog/action-checkstyle` | Checkstyle PR 评论 | ⭐⭐ |

---

## 8. 矩阵构建与并行策略

### 8.1 矩阵构建

```yaml
jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest]
        java: [17, 21]
      fail-fast: false    # ⭐ 某个组合失败不取消其他组合

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'
      - run: mvn test

    # 运行 2 × 2 = 4 个并行 Job：共 4 个 Runner 同时执行
```

### 8.2 依赖编排

```yaml
jobs:
  build:      # 第一阶段：编译
    runs-on: ubuntu-latest
    steps: [ ... ]

  unit-test:  # 第二阶段：依赖 build（并行）
    needs: build
    runs-on: ubuntu-latest
    steps: [ ... ]

  integration-test:  # 与 unit-test 并行
    needs: build
    runs-on: ubuntu-latest
    steps: [ ... ]

  deploy:     # 第三阶段：依赖测试全部通过
    needs: [unit-test, integration-test]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps: [ ... ]

# 执行顺序：
# build → [unit-test | integration-test] → deploy
```

---

## 9. Secrets 与环境保护

### 9.1 密钥管理

```yaml
# Secrets 层级（优先级从高到低）：

# 1. Environment Secrets（特定环境）
#    → Settings → Environments → production → Secrets

# 2. Repository Secrets（当前仓库）
#    → Settings → Secrets and variables → Actions → Repository secrets

# 3. Organization Secrets（组织级）
#    → Organization Settings → Secrets → Actions

# Workflow 中使用：
env:
  DOCKER_PASSWORD: ${{ secrets.DOCKER_PASSWORD }}
  SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

# ⚠️ Secrets 在日志中自动脱敏（***）
# ⚠️ Secrets 不能直接在 if: 条件中使用（需通过 env 中转）
```

### 9.2 Environment 部署保护

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  workflow_dispatch:

jobs:
  deploy-staging:
    runs-on: ubuntu-latest
    environment: staging          # ⭐ 指定环境
    steps:
      - run: ./deploy.sh staging

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment:                  # ⭐ 生产环境需手动审批
      name: production
      url: https://app.example.com  # 部署后在 Actions 页面显示链接
    steps:
      - run: ./deploy.sh production

# Environment 配置（在 GitHub Settings → Environments 中）：
#   - Required reviewers: 1-6 名审批者
#   - Wait timer: 部署前等待 N 分钟（观察窗口）
#   - Deployment branches: 限制哪些分支可以部署
#   - Environment secrets: 该环境专属密钥
```

---

## 10. 常见面试题

### Q1：GitHub Actions 的核心组件有哪些？

> Event（触发器）→ Workflow（YAML 定义）→ Job（执行单元）→ Step（步骤，run 或 uses）→ Runner（执行环境）。此外还有 Action（可复用组件）和 Artifact（构建产物）。详见第1节。

### Q2：如何在 CI 中运行需要数据库的集成测试？

> 使用 Service Container（`services` 字段）在 Job 中启动 MySQL/PostgreSQL/Redis 等服务容器，通过 `localhost:port` 连接。详见第5节。

### Q3：Secrets 如何安全管理？可以被 PR 读取吗？

> Secrets 通过 `${{ secrets.XXX }}` 引用，日志中自动脱敏。Fork 的 PR **不能**读取上游仓库的 Secrets（安全机制）。详见第9节。

### Q4：矩阵构建和依赖编排分别用于什么场景？

> 矩阵构建：同一套测试在不同 OS/版本组合上运行（fail-fast 控制是否全部完成）。依赖编排（needs）：实现分阶段流水线（编译 → 测试 → 部署）。详见第8节。

### Q5：如何保护生产部署？

> 使用 Environment 保护规则：设置 Required Reviewers（手动审批）+ Wait Timer（观察窗口）+ 限制部署分支 + 环境专属 Secrets。详见第9.2节。
