# 01-CI/CD 概念与流水线设计
> CI/CD/DevOps 的概念辨析、流水线阶段设计、Pipeline as Code、质量门禁理念——先理解"为什么"，再谈工具

## 📚 目录
1. [CI / CD / DevOps 概念辨析](#1-ci--cd--devops-概念辨析)
2. [CI/CD 解决的问题](#2-cicd-解决的问题)
3. [流水线阶段设计](#3-流水线阶段设计)
4. [Pipeline as Code](#4-pipeline-as-code)
5. [质量门禁理念](#5-质量门禁理念)
6. [环境与分支策略联动](#6-环境与分支策略联动)
7. [Java 项目流水线示例](#7-java-项目流水线示例)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. CI / CD / DevOps 概念辨析

| 概念 | 含义 | 回答的问题 |
|------|------|-----------|
| **CI（持续集成）** | 频繁合并代码 + 自动构建测试 | 集成问题尽早发现 |
| **CD（持续交付）** | 构建产物随时可发布到生产（人工确认） | 交付物随时可用 |
| **CD（持续部署）** | 自动部署到生产（无需人工） | 上线全自动 |
| **DevOps** | 开发与运维协作的文化+实践 | 打破部门墙 |

```text
CI/CD 流程全景：
代码提交 → CI（构建+测试+扫描）→ 制品（镜像/包）→ CD（部署到环境）
                                    ↓
                          质量门禁（不通过即阻断）
```

> 🎯 **持续交付 vs 持续部署**：交付 = "随时可以发布"（人点按钮）；部署 = "自动发布"（无需人）。大部分企业先做到**持续交付**（发布按钮 + 灰度），再演进到持续部署。

## 2. CI/CD 解决的问题

| 传统痛点 | CI/CD 解法 |
|----------|-----------|
| 集成地狱（月末合并爆炸） | 频繁合并 + 自动测试（每天多次） |
| "在我机器上能跑" | 统一构建环境（容器化构建） |
| 手工部署易错 | 流水线自动化（同一流程每次一致） |
| 上线不可控 | 质量门禁 + 灰度 + 回滚 |
| 无审计 | 每次构建/部署留痕 |

> 💡 **CI/CD 的本质收益**：缩短"代码提交 → 上线"的周期（交付周期），同时**提高上线成功率**——不是更快地犯错，而是更快地安全交付。

## 3. 流水线阶段设计

```text
标准 Java 流水线阶段：
① Checkout    拉取代码
② Build       编译打包（mvn package）
③ Test        单元测试（JUnit）+ 覆盖率
④ Static Scan 静态扫描（SonarQube）+ 代码规范
⑤ Security    依赖漏洞扫描（OWASP DC / Snyk）+ 镜像扫描（Trivy）
⑥ Package     构建镜像 + 推送制品仓库（Harbor）
⑦ Deploy      部署到环境（dev → test → prod 逐级）
⑧ Verify      部署后健康检查 + 冒烟测试
```

| 阶段 | 失败处理 | 门禁意义 |
|------|---------|---------|
| Build | 阻断 | 编译不过不上线 |
| Test | 阻断 | 测试不过不上线 |
| 扫描 | 阻断（高危） | 漏洞不过不上线 |
| Deploy | 阻断 + 回滚 | 部署失败自动回滚 |
| Verify | 告警 | 上线后监控验证 |

> 🎯 **门禁哲学**：流水线的每一道门都在回答"这一关不过，值不值得继续？"——**门禁宁可严格，不可虚设**（虚设的门禁 = 没有门禁）。

## 4. Pipeline as Code

流水线定义进代码仓库（版本化、可审查、可复用）：

| 工具 | 文件 | 特点 |
|------|------|------|
| Jenkins | `Jenkinsfile` | Groovy DSL，最灵活 |
| GitLab CI | `.gitlab-ci.yml` | YAML，与 Git 集成 |
| GitHub Actions | `.github/workflows/*.yml` | YAML，事件驱动 |
| Tekton | K8s CRD | 云原生，K8s 原生 |

```text
Pipeline as Code 的价值：
  版本化：流水线变更走 Code Review
  可审查：谁改了流水线有迹可循
  一致性：分支/环境用同一套模板
  可复用：共享模板/函数
```

> ⚠️ 反模式：在 UI 上"点点点"配置流水线——不可版本化、不可审查、不可复现。**2026 标准做法是 Pipeline as Code**。

## 5. 质量门禁理念

### 5.1 门禁分层

| 层级 | 门禁 | 阻断级别 |
|------|------|---------|
| 提交级 | 编译 + 单测（快） | 阻断合并 |
| PR 级 | 静态扫描 + 覆盖率 | 阻断合并 |
| 发布级 | 镜像扫描 + 安全合规 | 阻断上线 |
| 生产级 | 灰度指标 + 健康检查 | 自动回滚 |

### 5.2 门禁设计原则

```text
① 前置快门禁：编译/单测在提交时跑（分钟级）
② 后置重门禁：扫描/集成测试在发布前跑（完整）
③ 覆盖率红线：核心模块覆盖率 > 80%
④ 漏洞红线：高危漏洞 0 容忍（阻断）
⑤ 门禁结果可追溯：每次构建的检查报告归档
```

> ⚠️ **门禁疲劳**：门禁太多太慢会逼团队绕过——**保持"快反馈"（提交级 < 10 分钟）**，重检查放发布前。

## 6. 环境与分支策略联动

```text
分支 → 环境映射（典型）：
  feature/* → 开发环境（可选触发）
  develop   → 测试环境（每次合并触发）
  release/* → 预发布环境（手工触发）
  main      → 生产环境（发布按钮/标签触发）
```

| 环境 | 触发 | 门禁 | 部署方式 |
|------|------|------|---------|
| dev | push develop | 编译+单测 | 自动 |
| test | push develop | +集成测试 | 自动 |
| pre | 手工（release 分支） | +扫描 | 半自动 |
| prod | 发布按钮/标签 | 全部门禁 | **灰度发布**（见 05/06） |

> 💡 **环境隔离原则**：测试环境随意部署（快速反馈）；生产环境门禁全开 + 灰度（稳定优先）——**不同环境的目的不同，门禁强度也不同**。

## 7. Java 项目流水线示例

```yaml
# .gitlab-ci.yml（Java 项目示例，完整阶段）
stages: [build, test, scan, package, deploy]

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2"

cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths: [.m2/]

build:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  script: [mvn clean compile]
  only: [merge_requests, main, develop]

test:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn test
    - mvn jacoco:report
  coverage: '/Total.*?([0-9]{1,3})%/'
  artifacts:
    paths: [target/site/jacoco/]
  only: [merge_requests, main, develop]

scan:
  stage: scan
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn sonar:sonar -Dsonar.projectKey=$CI_PROJECT_KEY
  only: [main]

package:
  stage: package
  image: docker:20
  services: [docker:dind]
  script:
    - docker build -t $REGISTRY/$APP:$CI_PIPELINE_ID .
    - docker login $REGISTRY -u $USER -p $PASSWORD
    - docker push $REGISTRY/$APP:$CI_PIPELINE_ID
  only: [main]

deploy:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/$APP app=$REGISTRY/$APP:$CI_PIPELINE_ID -n $K8S_NS
    - kubectl rollout status deployment/$APP -n $K8S_NS --timeout=3m
  environment: production
  only: [main]
  when: manual            # 生产部署走发布按钮
```

## 8. 核心要点

> 🎯 **核心要点**：
> - 三概念：CI（集成+测试）、CD（交付=可发布 / 部署=自动上线）、DevOps（文化）；
> - 七阶段流水线：Checkout→Build→Test→Scan→Package→Deploy→Verify；
> - Pipeline as Code 是标准（Jenkinsfile/.gitlab-ci.yml/Actions/Tekton）；
> - 门禁四层：提交（快）→ PR（扫描）→ 发布（安全）→ 生产（灰度指标）；
> - 环境门禁分级：测试宽松快速反馈，生产全门禁+灰度；
> - 生产部署走发布按钮（持续交付），不是自动（持续部署）——大多数团队的稳妥起点。

## 9. 参考来源

- [GitLab CI 官方文档](https://docs.gitlab.com/ee/ci/)
- [Jenkins Pipeline 文档](https://www.jenkins.io/doc/book/pipeline/)
- [GitHub Actions 文档](https://docs.github.com/zh/actions)
- [DevOps 文化与实践（2026）](https://m.zpedu.com/it/ityw/37291.html)

---

**下一模块**：[02-Jenkins与GitLab-CI实践](02-Jenkins与GitLab-CI实践.md)　/　**返回总览**：[00-总览](00-CI-CD与灰度发布总览.md)
