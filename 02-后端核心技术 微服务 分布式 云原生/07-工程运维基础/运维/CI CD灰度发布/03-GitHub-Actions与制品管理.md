# 03-GitHub Actions 与制品管理
> Actions 工作流实战、制品仓库选型（Harbor/Nexus/GitHub Packages）、镜像与依赖管理——"构建产物去哪、怎么管、怎么用"

## 📚 目录
1. [GitHub Actions 核心概念](#1-github-actions-核心概念)
2. [Java 项目 Actions 实战](#2-java-项目-actions-实战)
3. [Actions 高级模式](#3-actions-高级模式)
4. [制品管理全景](#4-制品管理全景)
5. [Harbor：镜像仓库实践](#5-harbor镜像仓库实践)
6. [Nexus：依赖与制品仓库](#6-nexus依赖与制品仓库)
7. [制品版本与回滚](#7-制品版本与回滚)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. GitHub Actions 核心概念

```text
触发（Event）→ 工作流（Workflow）→ 作业（Job）→ 步骤（Step）
  push/PR/定时        .yml 文件        并行执行      命令/Action
```

| 概念 | 说明 |
|------|------|
| Workflow | `.github/workflows/*.yml`（Pipeline as Code） |
| Event | 触发条件（push/pull_request/schedule/tag） |
| Job | 一组步骤（可并行、可依赖 `needs`） |
| Step | 单条命令或复用 Action（`uses:`） |
| Runner | 执行环境（ubuntu-latest/自托管） |
| Secrets | 敏感信息（仓库级/环境级） |

## 2. Java 项目 Actions 实战

```yaml
# .github/workflows/ci.yml
name: Java CI/CD
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven                    # Maven 依赖缓存
      - name: Build & Test
        run: mvn clean verify
      - name: Upload Test Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: target/surefire-reports/

  scan:
    needs: build-test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: SonarQube Scan
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn sonar:sonar -Dsonar.token=$SONAR_TOKEN

  deploy:
    needs: [build-test, scan]
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
    environment: production               # 环境（含审批/保护规则）
    steps:
      - uses: actions/checkout@v4
      - name: Login to Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Build & Push Image
        run: |
          docker build -t ghcr.io/${{ github.repository }}:${{ github.sha }} .
          docker push ghcr.io/${{ github.repository }}:${{ github.sha }}
```

| 要点 | 说明 |
|------|------|
| `setup-java` + `cache: maven` | 依赖缓存（构建提速） |
| `secrets` | Token/密码（不入仓库） |
| `environment` | 环境保护（审批规则/变量隔离） |
| `needs` | 作业依赖（门禁链） |
| tag 用 SHA | 镜像版本可追溯 |

## 3. Actions 高级模式

### 3.1 环境审批（生产门禁）

```yaml
jobs:
  deploy-prod:
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://api.example.com
    # 环境配置了"Required reviewers"时，部署前需人工审批
```

```text
Environment 保护能力：
  Required reviewers（审批）
  Wait timer（等待窗口）
  Environment secrets（环境级密钥隔离）
  部署历史（可回滚到历史部署）
```

### 3.2 矩阵构建（多 JDK/多平台）

```yaml
strategy:
  matrix:
    java: ['17', '21']            # 多 JDK 兼容验证
    os: [ubuntu-latest, windows-latest]
```

### 3.3 定时与手动触发

```yaml
on:
  schedule:
    - cron: '0 2 * * *'           # 每日 2 点（安全扫描）
  workflow_dispatch: {}           # 手动触发按钮
```

## 4. 制品管理全景

```text
制品（Artifact）= 构建产物：
  镜像（Docker Image）→ 镜像仓库（Harbor/GHCR）
  依赖包（JAR）→ 依赖仓库（Nexus/Maven Central）
  构建报告 → 流水线归档
```

| 仓库类型 | 工具 | 管理内容 |
|----------|------|---------|
| 镜像仓库 | **Harbor** / GitHub Container Registry / 阿里云 ACR | Docker 镜像（漏洞扫描/保留策略） |
| 依赖仓库 | **Nexus** / Artifactory | Maven 依赖（私有库/代理中央库） |
| 构建产物 | CI 系统内置 | 报告/日志/归档 |

> 🎯 **制品管理的核心价值**：**每个可部署物都有唯一版本、可追溯来源、可安全存储**——"线上跑的镜像"能反查到"哪个 commit 构建的"。

## 5. Harbor：镜像仓库实践

```bash
# 推送镜像
docker login harbor.example.com
docker tag order-service:1.0.3 harbor.example.com/prod/order-service:1.0.3
docker push harbor.example.com/prod/order-service:1.0.3
```

| Harbor 能力 | 说明 |
|-------------|------|
| 项目隔离 | 按团队/环境分项目（dev/prod 权限隔离） |
| 漏洞扫描 | 推送即扫描（Trivy 引擎） |
| 保留策略 | 自动清理旧版本（保留最近 N 个） |
| 复制 | 跨站点镜像同步（容灾） |
| 签名 | 镜像签名验证（防篡改） |

```yaml
# CI 集成：扫描通过才部署
# harbor 扫描结果作为部署门禁（API 查询或 Webhook）
```

> 💡 镜像 tag 规范：`仓库/项目/应用:版本-环境`（`prod/order-service:1.0.3`）；**禁止 latest**——回滚需要精确版本。

## 6. Nexus：依赖与制品仓库

```text
Nexus 三大角色：
  私有仓库（内部分发的 JAR）
  代理仓库（缓存 Maven Central，加速 + 内网可用）
  制品仓库（部署发布产物）
```

```xml
<!-- Maven settings.xml 配置私有仓库 -->
<settings>
  <mirrors>
    <mirror>
      <id>nexus</id>
      <mirrorOf>*</mirrorOf>
      <url>https://nexus.example.com/repository/maven-public/</url>
    </mirror>
  </mirrors>
  <servers>
    <server>
      <id>nexus-releases</id>
      <username>deploy</username>
      <password>${env.NEXUS_PASSWORD}</password>
    </server>
  </servers>
</settings>
```

```bash
# 发布私有依赖
mvn deploy   # 推送到 Nexus（releases/snapshots 仓库）
```

| Nexus 场景 | 价值 |
|------------|------|
| 内部公共组件（common-lib） | 多服务共享（见 Git 体系 Monorepo 章节） |
| 中央库代理 | 内网构建加速 + 离线可用 |
| 制品审计 | 依赖来源可追溯 |

## 7. 制品版本与回滚

```text
版本规范：
  镜像：应用名:主.次.修订-环境（order-service:1.0.3-prod）
  CI tag：应用名:流水线ID（可追溯到 commit）
  依赖：SemVer（1.0.3）

回滚三要素：
  ① 镜像还在（保留策略防误删）
  ② 版本可定位（tag 规范 + 部署记录）
  ③ 回滚动作标准化（kubectl rollout undo / 切回旧镜像）
```

| 回滚场景 | 动作 |
|----------|------|
| K8s 滚动更新失败 | `kubectl rollout undo deployment/order-service` |
| 灰度异常 | 流量切回 100% 旧版本（见 06） |
| 蓝绿异常 | Service selector 切回蓝环境 |
| 配置回滚 | Nacos 配置历史版本 |

> 🎯 **可回滚的前提是制品可追溯**：镜像 tag 含版本/流水线 ID + 保留策略不删旧版 + 部署记录留痕——三件套缺一不可。

## 8. 核心要点

> 🎯 **核心要点**：
> - Actions：事件驱动 + needs 门禁链 + environment 审批 + secrets；
> - 制品双仓库：Harbor（镜像，扫描/保留/复制）+ Nexus（依赖，私有/代理）；
> - 镜像 tag 规范：版本-环境，禁止 latest，CI 用 SHA/流水线 ID；
> - 回滚三要素：镜像在（保留策略）、版本可定位（tag+记录）、动作标准化（undo/切流）；
> - Harbor 扫描作为部署门禁（安全左移，见 04）；
> - 制品管理 = "线上跑的每一行代码都有据可查"。

## 9. 参考来源

- [GitHub Actions 官方文档](https://docs.github.com/zh/actions)
- [Harbor 官方文档](https://goharbor.io/docs/)
- [Nexus Repository 文档](https://help.sonatype.com/en/sonatype-nexus-repository.html)
- [GitHub Container Registry 文档](https://docs.github.com/zh/packages)

---

**下一模块**：[04-质量门禁与安全左移](04-质量门禁与安全左移.md)　/　**返回总览**：[00-总览](00-CI-CD与灰度发布总览.md)
