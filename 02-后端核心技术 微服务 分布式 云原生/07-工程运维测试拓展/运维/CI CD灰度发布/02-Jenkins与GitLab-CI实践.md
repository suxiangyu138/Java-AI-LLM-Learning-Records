# 02-Jenkins 与 GitLab CI 实践
> 两大 CI 引擎的实战：Jenkins Pipeline 全流程、GitLab CI 配置、对比选型——"重而强"与"轻而简"

## 📚 目录
1. [对比总览](#1-对比总览)
2. [Jenkins 架构与部署](#2-jenkins-架构与部署)
3. [Jenkins Pipeline 实战](#3-jenkins-pipeline-实战)
4. [GitLab CI 架构与配置](#4-gitlab-ci-架构与配置)
5. [GitLab CI 实战](#5-gitlab-ci-实战)
6. [选型决策](#6-选型决策)
7. [高频避坑](#7-高频避坑)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. 对比总览

| 维度 | Jenkins | GitLab CI |
|------|---------|-----------|
| 形态 | 独立 CI 服务器 | GitLab 内置 |
| 配置 | Jenkinsfile（Groovy） | `.gitlab-ci.yml`（YAML） |
| 学习成本 | 高（Groovy/插件） | 低（YAML） |
| 插件生态 | 极丰富（1500+） | 中等 |
| 与 Git 集成 | 需配置 Webhook | 原生 |
| 资源占用 | 高（重） | 轻 |
| 适合 | 传统企业/复杂需求 | GitLab 全家桶用户 |
| 2026 定位 | 成熟但"重"，向 Tekton/云原生迁移中 | 简洁高效的主流选择 |

## 2. Jenkins 架构与部署

```text
Jenkins 架构：
Master（调度/管理）→ Agent（执行构建，可多台）
```

```bash
# Docker 部署（官方推荐）
docker run -d --name jenkins \
  -p 8080:8080 -p 50000:50000 \
  -v jenkins-home:/var/jenkins_home \
  jenkins/jenkins:lts

# 初始化
docker logs jenkins | grep -A2 'initialAdminPassword'   # 初始密码
```

| 组件 | 说明 |
|------|------|
| Master | 任务调度、配置管理、界面 |
| Agent | 执行构建（Java 构建用 maven 镜像的 Agent） |
| 凭证管理 | 凭据（Token/SSH/镜像仓库账号）安全存储 |
| 插件 | 构建工具/通知/凭证集成 |

> ⚠️ **Jenkins 常见坑**：内存占用高（2G+ 起步）；插件升级破坏兼容；Agent 环境不一致——**用容器化 Agent（maven 镜像）保证构建环境一致**。

## 3. Jenkins Pipeline 实战

```groovy
// Jenkinsfile（Java 项目完整示例）
pipeline {
    agent {
        docker { image 'maven:3.9-eclipse-temurin-17' }   // 容器化 Agent
    }
    environment {
        REGISTRY = 'harbor.example.com'
        APP = 'order-service'
        K8S_NS = 'production'
        // 凭证引用（不硬编码）
        REGISTRY_CRED = credentials('harbor-cred')
    }
    stages {
        stage('Checkout') { steps { checkout scm } }

        stage('Build & Test') {
            steps {
                sh 'mvn clean package'
                junit 'target/surefire-reports/*.xml'      // 测试报告
            }
        }

        stage('Static Scan') {
            steps {
                sh 'mvn sonar:sonar -Dsonar.projectKey=order-service'
            }
        }

        stage('Build Image') {
            steps {
                sh """
                    docker build -t ${REGISTRY}/${APP}:${BUILD_NUMBER} .
                    docker tag ${REGISTRY}/${APP}:${BUILD_NUMBER} ${REGISTRY}/${APP}:latest
                """
            }
        }

        stage('Push Image') {
            steps {
                sh """
                    docker login ${REGISTRY} -u ${REGISTRY_CRED_USR} -p ${REGISTRY_CRED_PSW}
                    docker push ${REGISTRY}/${APP}:${BUILD_NUMBER}
                """
            }
        }

        stage('Deploy') {
            when { branch 'main' }
            input { message '确认部署到生产？' }            // 发布按钮
            steps {
                sh "kubectl set image deployment/${APP} app=${REGISTRY}/${APP}:${BUILD_NUMBER} -n ${K8S_NS}"
                sh "kubectl rollout status deployment/${APP} -n ${K8S_NS} --timeout=3m"
            }
        }
    }
    post {
        failure { sh 'kubectl rollout undo deployment/order-service -n production || true' }
        always { junit 'target/surefire-reports/*.xml' }
    }
}
```

| 要素 | 说明 |
|------|------|
| `agent docker` | 容器化 Agent（环境一致性） |
| `credentials()` | 凭证管理（不硬编码密码） |
| `when` | 分支条件（main 才部署） |
| `input` | 发布确认按钮（持续交付） |
| `post` | 失败自动回滚 + 报告归档 |

> 💡 **Jenkins 最佳实践**：容器化 Agent、凭证管理、`input` 发布门、post 失败回滚——四件套是生产级 Jenkins 的标配。

## 4. GitLab CI 架构与配置

```text
GitLab CI 组件：
  GitLab（仓库 + CI 调度）
  → Runner（执行器，可 Docker/K8s 部署）
  → 作业（Job）按 stages 顺序执行
```

```bash
# Runner 注册（Docker 方式）
docker run -d --name gitlab-runner \
  -v /var/run/docker.sock:/var/run/docker.sock \
  gitlab/gitlab-runner:latest

gitlab-runner register \
  --url https://gitlab.example.com \
  --token <RUNNER_TOKEN> \
  --executor docker \
  --docker-image maven:3.9-eclipse-temurin-17
```

| 概念 | 说明 |
|------|------|
| Runner | 执行器（shared/group/project 三种范围） |
| Job | 最小执行单元（script + image + stage） |
| Stage | 作业分组（build/test/deploy 顺序执行） |
| Cache/Artifact | 依赖缓存 / 制品传递 |
| Rules/Only | 触发条件（分支/标签/变量） |

## 5. GitLab CI 实战

```yaml
# .gitlab-ci.yml（多阶段 + 多环境）
stages: [build, test, scan, package, deploy-dev, deploy-prod]

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2"
  IMAGE: $CI_REGISTRY_IMAGE:$CI_PIPELINE_ID

cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths: [.m2/]

build:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  script: [mvn clean compile -DskipTests]

test:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script: [mvn test]
  artifacts:
    when: always
    reports:
      junit: target/surefire-reports/*.xml

scan:
  stage: scan
  image: maven:3.9-eclipse-temurin-17
  script: [mvn sonar:sonar]
  only: [main]

package:
  stage: package
  image: docker:20
  services: [docker:dind]
  before_script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
  script:
    - docker build -t $IMAGE .
    - docker push $IMAGE

deploy-dev:
  stage: deploy-dev
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/order-service app=$IMAGE -n dev
    - kubectl rollout status deployment/order-service -n dev --timeout=3m
  environment: dev
  only: [develop]

deploy-prod:
  stage: deploy-prod
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/order-service app=$IMAGE -n production
    - kubectl rollout status deployment/order-service -n production --timeout=3m
  environment:
    name: production
    url: https://api.example.com
  only: [main]
  when: manual                # 发布按钮
```

| 特性 | 说明 |
|------|------|
| `environment` | 环境管理（部署记录/回滚到历史部署） |
| `when: manual` | 手动触发（生产发布按钮） |
| `only/rules` | 分支触发控制 |
| `artifacts.reports.junit` | 测试报告集成到 MR |
| CI 变量 | 内置变量（CI_PIPELINE_ID/CI_REGISTRY） |

## 6. 选型决策

| 场景 | 推荐 |
|------|------|
| 已用 GitLab | **GitLab CI**（原生集成，零额外部署） |
| 传统企业/复杂集成需求 | Jenkins（插件生态） |
| 需要 Jenkins 但嫌重 | Jenkins 容器化 + 精简插件 |
| 云原生新项目 | GitHub Actions 或 Tekton（见 03） |
| 纯 K8s 团队 | Tekton + ArgoCD |

> 🎯 **2026 选型结论**：新项目优先 GitLab CI 或 GitHub Actions（Pipeline as Code + 轻量）；Jenkins 用于存量与复杂场景；纯云原生用 Tekton——**别为"功能多"选重工具，为"够用且简单"选轻工具**。

## 7. 高频避坑

| # | 坑 | 规避 |
|---|----|------|
| 1 | Jenkins 内存占用过高 | 容器化 + 合理 JVM 参数 |
| 2 | Agent 环境不一致 | 容器化 Agent（镜像固定版本） |
| 3 | 密码硬编码 | 凭证管理（credentials/CI 变量） |
| 4 | 生产自动部署 | `when: manual` 发布按钮（先持续交付） |
| 5 | 缓存失效 | cache key 按分支 + 依赖锁文件 |
| 6 | 测试报告不展示 | artifacts/junit 报告集成 |
| 7 | 镜像 tag 用 latest | 用 BUILD_NUMBER/CI_PIPELINE_ID（可回滚） |
| 8 | 失败不回滚 | post/rollout status 失败处理 |

## 8. 核心要点

> 🎯 **核心要点**：
> - Jenkins：重而强（Groovy Pipeline + 1500 插件 + 容器化 Agent）；
> - GitLab CI：轻而简（YAML + Git 原生 + Runner）；
> - 生产级四件套：容器化构建、凭证管理、发布按钮（manual/input）、失败回滚；
> - 镜像 tag 用流水线 ID（可回滚的唯一标识）；
> - 选型：够用且简单 > 功能多；存量 Jenkins 可容器化续命；
> - 2026 趋势：Pipeline as Code 是底线，云原生向 Tekton/GitHub Actions 演进。

## 9. 参考来源

- [Jenkins 官方文档](https://www.jenkins.io/doc/)
- [Jenkins Pipeline 语法](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [GitLab CI 官方文档](https://docs.gitlab.com/ee/ci/)
- [GitLab CI 环境管理](https://docs.gitlab.com/ee/ci/environments/)

---

**下一模块**：[03-GitHub-Actions与制品管理](03-GitHub-Actions与制品管理.md)　/　**返回总览**：[00-总览](00-CI-CD与灰度发布总览.md)
