# CI/CD 持续集成与持续部署 完全指南

> 适用人群：Java后端开发者 / DevOps工程师 / 软件项目经理
> 前置知识：熟悉Git基本操作，了解Maven/Gradle构建流程，对Linux命令行有基本认知

---

## 目录

1. [CI/CD 概念与价值](#1-cicd-概念与价值)
2. [CI/CD 核心流程](#2-cicd-核心流程)
3. [Jenkins 深入详解](#3-jenkins-深入详解)
4. [GitLab CI 实践](#4-gitlab-ci-实践)
5. [GitHub Actions 实践](#5-github-actions-实践)
6. [代码质量与安全](#6-代码质量与安全)
7. [构建工具与制品管理](#7-构建工具与制品管理)
8. [部署策略](#8-部署策略)
9. [监控与告警](#9-监控与告警)
10. [总结与面试要点](#10-总结与面试要点)

---

## 1. CI/CD 概念与价值

### 1.1 什么是 CI（持续集成）

**持续集成（Continuous Integration，CI）** 是一种软件开发实践，要求开发人员每天多次将代码合并到主干分支，每次合并都通过自动化构建（编译 + 测试）来验证，从而尽早发现集成错误。

传统开发模式下，开发者各自在特性分支上工作数天甚至数周，最后合并时才发现冲突不断、测试大面积失败，修复成本极高。CI 的核心思想就是 **"频繁合并，及时验证"**：

- **频繁提交代码**：每天至少合并一次代码到主干，而不是在分支上闭门造车数周。
- **自动构建**：每次 Push 或 Merge Request 都触发自动化构建（Maven/Gradle 编译打包）。
- **自动测试**：运行单元测试、集成测试，快速反馈代码质量。
- **及早发现集成问题**：如果构建失败，团队立即收到通知，第一时间修复，避免问题积压。

CI 的黄金准则：**谁破坏了构建，谁就负责修复，并且优先处理**。

### 1.2 什么是 CD（持续交付与持续部署）

**持续交付（Continuous Delivery）** 是在 CI 的基础上，将通过测试的代码自动部署到类生产环境（Staging），但部署到生产环境需要人工确认。团队可以随时、按需地将任何版本的软件发布到生产环境，部署过程是标准化的、可重复的。

**持续部署（Continuous Deployment）** 是持续交付的更进一步：每次通过 CI 验证的代码变更都会自动部署到生产环境，完全不需要人工干预。这要求团队的自动化测试覆盖足够全面，质量门禁足够严格，才能对生产环境的安全有信心。

两者的核心区别：

| 特性 | 持续交付 | 持续部署 |
|------|---------|---------|
| 部署到测试环境 | 自动化 | 自动化 |
| 部署到生产环境 | 需人工确认（一键部署） | 全自动 |
| 发布频率 | 每天/每周多次 | 每天数十次甚至更多 |
| 适用场景 | 大多数企业级项目 | 互联网高迭代产品 |
| 风险控制 | 人工把关 | 自动化质量门禁 + 快速回滚 |

### 1.3 DevOps：打破开发与运维的壁垒

**DevOps（Development + Operations）** 是一组实践和文化理念，强调开发团队和运维团队的协作、沟通和整合。CI/CD 是 DevOps 的核心技术实践之一。

DevOps 的关键理念 **"You Build It, You Run It"**（你构建，你运维）意味着开发团队不仅要负责编写代码，还要负责代码在生产环境的运行状况，包括监控、告警响应、故障排查等。这带来了一系列变化：

- **消除部门墙**：不再有"开发扔代码过墙，运维接锅"的情况。
- **基础设施即代码（IaC）**：服务器配置、网络策略、中间件配置都用代码管理（Terraform、Ansible）。
- **可观测性内建**：开发阶段就考虑日志、指标、链路追踪，而不是上线后再补。
- **快速反馈闭环**：从代码提交到生产运行的全链路可见，出现问题快速定位。

### 1.4 CI/CD 的核心价值

| 价值 | 说明 |
|------|------|
| **快速反馈** | 提交代码后几分钟内就能知道是否通过编译和测试，不必等数天 |
| **减少手动操作错误** | 部署过程自动化，消除"人肉运维"带来的配置遗漏、命令输错等风险 |
| **可重复的部署流程** | 每次部署流程一致，环境一致，杜绝"在我机器上是好的" |
| **快速迭代** | 从代码提交到上线的时间从数周缩短到数小时甚至数分钟 |
| **团队协作效率** | 代码冲突及早发现，团队沟通成本降低 |

---

## 2. CI/CD 核心流程

一个标准的企业级 CI/CD 流水线通常包含以下阶段：

```
代码提交 → 代码检查 → 编译构建 → 自动化测试 → 代码质量扫描 → 制品打包 → 镜像构建 → 环境部署 → 冒烟测试 → 通知反馈
```

对应到具体的工具链，Java 后端项目典型的 CI/CD 流程如下：

```
Git Push 
    → Webhook 触发 Jenkins/GitLab CI/GitHub Actions
    → Checkout 代码
    → Maven/Gradle 编译（compile）
    → 运行单元测试（test）
    → SonarQube 代码质量扫描（sonar:sonar）
    → 制品打包（package/deploy）
    → Docker 镜像构建并推送至镜像仓库
    → 部署到 Kubernetes 集群（kubectl apply）
    → 健康检查和冒烟测试
    → 发送通知（钉钉/企业微信/Slack）
```

---

## 3. Jenkins 深入详解

Jenkins 是目前最成熟、插件生态最丰富的 CI/CD 引擎，在 Java 企业级开发中占据主导地位，面试高频考点。

### 3.1 Jenkins 架构

Jenkins 采用 **Master/Agent 分布式架构**：

- **Master 节点**：负责调度任务、管理配置、提供 Web UI。Master 本身不执行具体的构建任务。
- **Agent / Slave 节点**：实际执行构建任务的 worker。可以有多台，支持不同操作系统（Linux/Windows/macOS），不同的构建任务可以分配到不同的 Agent 上执行。

这种架构的好处：

- **负载均衡**：多个 Agent 并行执行构建，充分利用硬件资源。
- **环境隔离**：不同的项目可以使用不同的 Agent（一台装 JDK 8，一台装 JDK 17）。
- **水平扩展**：构建任务增多时，只需增加 Agent 节点即可。

Agent 的连接方式有 SSH、JNLP（Java Web Start）和 WebSocket 三种，推荐使用 SSH 方式，配置简单且稳定。

### 3.2 Pipeline as Code

Jenkins Pipeline 的核心思想是 **用代码定义构建流程**，将构建过程保存在 Jenkinsfile 中，随代码一起版本管理。这是 Jenkins 2.x 最重要的特性。

Jenkinsfile 支持两种语法：

#### 声明式 Pipeline（Declarative Pipeline）— **推荐使用**

声明式语法结构清晰，学习成本低，适合大多数场景。基本结构如下：

```groovy
pipeline {
    agent any
    
    environment {
        JAVA_HOME = tool name: 'jdk17', type: 'jdk'
        MAVEN_HOME = tool name: 'maven3', type: 'maven'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                success {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo '构建成功！'
        }
        failure {
            echo '构建失败！'
        }
    }
}
```

#### 脚本式 Pipeline（Scripted Pipeline）

脚本式语法更加灵活，基于 Groovy 脚本，适合复杂流程控制：

```groovy
node {
    stage('Checkout') {
        checkout scm
    }
    stage('Build') {
        sh 'mvn clean compile'
    }
    stage('Test') {
        sh 'mvn test'
    }
}
```

**注意**：实际项目中推荐使用声明式 Pipeline，因为它结构清晰、错误提示友好、内置的 `post` / `when` 等语法大大简化了常见场景的代码量。

### 3.3 声明式 Pipeline 核心语法详解

#### pipeline 块

整个流水线的根节点，包含所有配置。

#### agent

指定构建在哪个节点上执行：

```groovy
agent any                    // 任何可用节点
agent none                   // 全局不指定，各 stage 单独指定
agent { label 'linux' }      // 指定标签为 linux 的节点
agent { docker 'maven:3.8-openjdk-17' }  // 在 Docker 容器中执行
```

#### environment

定义环境变量：

```groovy
environment {
    APP_NAME = 'user-service'
    DOCKER_REGISTRY = 'registry.example.com'
    // 从 Jenkins 凭据中读取
    DOCKER_CREDENTIALS = credentials('docker-hub-credentials')
}
```

#### stages / stage / steps

**stages** 包含一个或多个 **stage**，每个 **stage** 包含具体的 **steps**。每个 stage 代表流水线的一个阶段，在 UI 上可以清晰看到每个阶段的执行状态。

```groovy
stages {
    stage('编译') {
        steps {
            sh 'mvn clean compile'
        }
    }
    stage('测试') {
        steps {
            sh 'mvn test'
        }
    }
}
```

#### post

构建后的处理，根据构建结果执行不同的操作：

```groovy
post {
    always {
        // 无论成功失败都执行
        cleanWs()
    }
    success {
        // 构建成功时
        archiveArtifacts artifacts: 'target/*.jar'
    }
    failure {
        // 构建失败时
        emailext(
            subject: "构建失败: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
            body: "请检查构建日志",
            to: 'team@example.com'
        )
    }
    unstable {
        // 测试结果不稳定时
    }
    aborted {
        // 被手动中止时
    }
}
```

#### when

条件执行，满足条件才执行该 stage：

```groovy
stage('部署到生产') {
    when {
        branch 'main'
        expression { 
            return currentBuild.result == null || currentBuild.result == 'SUCCESS'
        }
    }
    steps {
        sh './deploy-prod.sh'
    }
}

stage('仅对特性分支执行') {
    when {
        not { branch 'main' }
    }
    steps {
        sh 'mvn checkstyle:check'
    }
}

stage('仅当有 tag 时部署') {
    when {
        tag 'v*'
    }
    steps {
        sh './release.sh'
    }
}
```

#### input

人工确认，常用于生产环境部署前的审批：

```groovy
stage('部署到生产') {
    input {
        message '确认部署到生产环境？'
        ok '确认部署'
        submitter 'admin,tech-lead'
        parameters {
            string(name: 'VERSION', defaultValue: '', description: '部署版本号')
        }
    }
    steps {
        sh "kubectl set image deployment/user-service user-service=registry.example.com/user-service:${VERSION}"
    }
}
```

#### tools

配置使用的工具版本，需要在 Jenkins 全局工具配置中预先配置：

```groovy
pipeline {
    agent any
    tools {
        jdk 'jdk17'
        maven 'maven3.9'
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn --version'
                sh 'java -version'
                sh 'mvn clean package'
            }
        }
    }
}
```

### 3.4 Jenkins 触发器

#### Poll SCM（定时轮询）

Jenkins 定期检查代码仓库是否有变更，有变更则触发构建：

```groovy
triggers {
    // H 表示哈希分布，避免整点并发
    pollSCM('H/5 * * * *')  // 每5分钟检查一次
}
```

Cron 表达式格式：`分 时 日 月 周`

#### Webhook（推荐）

代码仓库 Push 或 Merge Request 时主动通知 Jenkins，实时触发构建：

- **GitHub**：在 GitHub 仓库 Settings → Webhooks 中添加 Jenkins 的 Webhook URL（`http://jenkins-url/github-webhook/`）。
- **GitLab**：在 GitLab 项目 Settings → Webhooks 中添加 `http://jenkins-url/project/{project-name}`。

在 Pipeline 中配置：

```groovy
triggers {
    // GitHub Push 触发
    githubPush()
}
```

#### Build after other projects

构建其他项目后触发：

```groovy
triggers {
    upstream(upstreamProjects: 'common-lib-pipeline', threshold: hudson.model.Result.SUCCESS)
}
```

### 3.5 Spring Boot 项目完整 Jenkinsfile 示例

以下是一个企业级 Spring Boot 项目的完整 Jenkinsfile，涵盖代码检出、Maven 构建、SonarQube 扫描、Docker 镜像构建推送、Kubernetes 部署以及钉钉通知：

```groovy
pipeline {
    agent {
        label 'maven-node'
    }

    environment {
        // 项目信息
        APP_NAME = 'user-service'
        PROJECT_NAME = 'user-center'
        
        // 工具路径
        MAVEN_HOME = tool name: 'maven3.9', type: 'maven'
        JAVA_HOME = tool name: 'jdk17', type: 'jdk'
        
        // Docker 镜像仓库
        DOCKER_REGISTRY = 'registry.cn-hangzhou.aliyuncs.com'
        DOCKER_NAMESPACE = 'mycompany'
        DOCKER_IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
        
        // 凭据
        DOCKER_CREDENTIALS_ID = 'docker-hub-credentials'
        K8S_CONFIG_ID = 'kubeconfig-prod'
        
        // SonarQube
        SONAR_HOST_URL = 'http://sonarqube.example.com:9000'
        SONAR_TOKEN_ID = 'sonar-token'
    }

    stages {
        stage('检出代码') {
            steps {
                checkout scm
                script {
                    GIT_COMMIT = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                    GIT_BRANCH = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
                }
            }
        }

        stage('Maven 编译与单元测试') {
            steps {
                sh 'mvn clean test -Dspring.profiles.active=ci'
            }
            post {
                success {
                    // 发布测试报告
                    junit '**/target/surefire-reports/*.xml'
                    // 发布测试覆盖率报告
                    jacoco(
                        classPattern: 'target/classes',
                        execPattern: 'target/jacoco.exec'
                    )
                }
            }
        }

        stage('SonarQube 代码质量扫描') {
            steps {
                withSonarQubeEnv('SonarQube Server') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        sh '''
                            mvn sonar:sonar \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                -Dsonar.login=${SONAR_TOKEN} \
                                -Dsonar.projectKey=${PROJECT_NAME} \
                                -Dsonar.projectName=${PROJECT_NAME} \
                                -Dsonar.java.binaries=target/classes
                        '''
                    }
                }
            }
        }

        stage('Quality Gate 质量门禁') {
            steps {
                timeout(time: 2, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Maven 打包') {
            steps {
                sh 'mvn package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Docker 构建与推送') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'docker-hub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        docker login ${DOCKER_REGISTRY} -u ${DOCKER_USER} -p ${DOCKER_PASS}
                        docker build -t ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${APP_NAME}:${DOCKER_IMAGE_TAG} .
                        docker tag ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${APP_NAME}:${DOCKER_IMAGE_TAG} ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${APP_NAME}:latest
                        docker push ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${APP_NAME}:${DOCKER_IMAGE_TAG}
                        docker push ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${APP_NAME}:latest
                    """
                }
            }
        }

        stage('部署到 Kubernetes') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-prod', variable: 'KUBECONFIG')]) {
                    sh """
                        sed -i 's|image:.*|image: ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${APP_NAME}:${DOCKER_IMAGE_TAG}|' k8s/deployment.yaml
                        kubectl apply -f k8s/
                        kubectl rollout status deployment/${APP_NAME} -n production --timeout=300s
                    """
                }
            }
        }

        stage('冒烟测试') {
            steps {
                sh """
                    curl -f http://prod.example.com/actuator/health || exit 1
                    curl -f http://prod.example.com/api/user/health || exit 1
                """
            }
        }
    }

    post {
        always {
            // 清理工作空间
            cleanWs()
        }
        success {
            // 钉钉通知——构建成功
            dingtalk(
                robot: 'devops-robot',
                type: 'ACTION_CARD',
                title: "✅ ${APP_NAME} 构建成功",
                text: [
                    "**项目**: ${PROJECT_NAME}",
                    "**分支**: ${env.GIT_BRANCH}",
                    "**版本**: ${env.DOCKER_IMAGE_TAG}",
                    "**触发**: ${currentBuild.getBuildCauses()[0].shortDescription}",
                    "**耗时**: ${currentBuild.durationString}"
                ],
                btnUrl: "${env.BUILD_URL}",
                btnOrientation: '1'
            )
        }
        failure {
            // 钉钉通知——构建失败
            dingtalk(
                robot: 'devops-robot',
                type: 'ACTION_CARD',
                title: "❌ ${APP_NAME} 构建失败",
                text: [
                    "**项目**: ${PROJECT_NAME}",
                    "**分支**: ${env.GIT_BRANCH}",
                    "**失败阶段**: ${env.STAGE_NAME}",
                    "**触发**: ${currentBuild.getBuildCauses()[0].shortDescription}"
                ],
                btnUrl: "${env.BUILD_URL}",
                btnOrientation: '1'
            )
        }
    }
}
```

---

## 4. GitLab CI 实践

GitLab CI 是 GitLab 内置的 CI/CD 系统，配置简单、与 GitLab 深度集成，是 Jenkins 之外的又一主流选择。

### 4.1 .gitlab-ci.yml 核心配置

在项目根目录创建 `.gitlab-ci.yml` 文件，定义 CI/CD 流水线：

```yaml
# 定义流水线阶段，按顺序执行
stages:
  - build
  - test
  - sonarqube
  - package
  - docker-build
  - deploy

# 使用 Maven 镜像
image: maven:3.8-openjdk-17

# 缓存 Maven 依赖，加速后续构建
cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .m2/repository/

# 全局变量
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository -DskipTests=false"
  DOCKER_REGISTRY: "registry.cn-hangzhou.aliyuncs.com"
  DOCKER_NAMESPACE: "mycompany"
  APP_NAME: "user-service"

# Job 定义
maven-build:
  stage: build
  script:
    - mvn clean compile
  artifacts:
    paths:
      - target/classes/

maven-test:
  stage: test
  script:
    - mvn test
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml

sonarqube-check:
  stage: sonarqube
  image: sonarsource/sonar-scanner-cli:latest
  variables:
    SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"
  script:
    - sonar-scanner
      -Dsonar.projectKey=${CI_PROJECT_NAME}
      -Dsonar.sources=.
      -Dsonar.java.binaries=target/classes
      -Dsonar.host.url=${SONAR_HOST_URL}
      -Dsonar.login=${SONAR_TOKEN}
  allow_failure: false

maven-package:
  stage: package
  script:
    - mvn package -DskipTests
  artifacts:
    paths:
      - target/*.jar
    expire_in: 30 days

docker-build-push:
  stage: docker-build
  image: docker:20.10.16
  services:
    - docker:20.10.16-dind
  variables:
    DOCKER_TLS_CERTDIR: ""
    DOCKER_IMAGE_TAG: "${CI_PIPELINE_ID}-${CI_COMMIT_SHORT_SHA}"
  script:
    - docker login -u ${DOCKER_USER} -p ${DOCKER_PASS} ${DOCKER_REGISTRY}
    - docker build -t ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${APP_NAME}:${DOCKER_IMAGE_TAG} .
    - docker push ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${APP_NAME}:${DOCKER_IMAGE_TAG}
  only:
    - main
    - tags

deploy-to-k8s:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - sed -i "s|image:.*|image: ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${APP_NAME}:${CI_PIPELINE_ID}-${CI_COMMIT_SHORT_SHA}|" k8s/deployment.yaml
    - kubectl apply -f k8s/
    - kubectl rollout status deployment/${APP_NAME} -n production --timeout=300s
  only:
    - main
  when: manual  # 生产部署需要手动确认
  dependencies:
    - docker-build-push
```

### 4.2 GitLab Runner

**GitLab Runner** 是执行 CI Job 的代理程序，需要单独安装在服务器上。

- **Shared Runner**：所有项目共享，由 GitLab 官方或团队统一管理。
- **Specific Runner**：绑定到特定项目，可以为项目定制环境。

Runner 注册命令：

```bash
gitlab-runner register \
  --url https://gitlab.com \
  --registration-token YOUR_TOKEN \
  --executor docker \
  --description "Docker Runner" \
  --docker-image alpine:latest \
  --docker-volumes /var/run/docker.sock:/var/run/docker.sock
```

### 4.3 GitLab CI 高级特性

**Merge Request 自动触发**：当创建或更新 MR 时自动执行流水线，在 MR 页面直接查看 Pipeline 结果，拦截未通过质量门禁的代码合并。

```yaml
# 仅在 MR 中执行的 Job
check-mr:
  stage: test
  script:
    - mvn checkstyle:check
  only:
    - merge_requests
```

**环境管理（Review Apps）**：每次 MR 自动部署一个临时的 Review 环境，方便 Reviewer 直接预览效果。

```yaml
review-app:
  stage: deploy
  script:
    - helm upgrade --install review-${CI_MERGE_REQUEST_IID} ./chart
      --set image.tag=${CI_COMMIT_SHORT_SHA}
      --set host=review-${CI_MERGE_REQUEST_IID}.example.com
  environment:
    name: review/${CI_MERGE_REQUEST_IID}
    url: http://review-${CI_MERGE_REQUEST_IID}.example.com
    on_stop: stop-review-app
  only:
    - merge_requests

stop-review-app:
  stage: deploy
  script:
    - helm uninstall review-${CI_MERGE_REQUEST_IID}
  when: manual
  environment:
    name: review/${CI_MERGE_REQUEST_IID}
    action: stop
  only:
    - merge_requests
```

**Auto DevOps**：GitLab 提供的开箱即用 CI/CD 方案，自动检测项目类型（Java、Node.js、Python 等），生成默认流水线。适合快速启动的项目。

### 4.4 Jenkins vs GitLab CI 对比

| 对比维度 | Jenkins | GitLab CI |
|---------|---------|-----------|
| 安装配置 | 需要单独部署维护 | GitLab 内置，开箱即用 |
| 配置方式 | Jenkinsfile（Groovy） | .gitlab-ci.yml（YAML） |
| 学习曲线 | 较陡，Groovy 语法 + 丰富插件 | 较平缓，YAML 配置直观 |
| 插件生态 | 极其丰富（1500+ 插件） | 相对有限 |
| 扩展性 | Master/Agent 架构，灵活扩展 | Runner 管理，天然分布式 |
| UI/UX | 经典但略显老旧 | 现代化，与 GitLab 无缝集成 |
| 容器支持 | 通过插件支持 Docker/K8s | 原生 Docker 支持，K8s Executor |
| 适用场景 | 复杂流水线、多工具链整合 | GitLab 生态项目、中小团队 |

---

## 5. GitHub Actions 实践

GitHub Actions 是 GitHub 内置的 CI/CD 功能，凭借与 GitHub 生态的无缝集成和丰富的 Action 市场，在开源社区中广泛使用。

### 5.1 核心概念

- **Workflow（工作流）**：在 `.github/workflows/` 目录下定义的 YAML 文件，一个项目可以有多个 Workflow。
- **Event（事件）**：触发 Workflow 执行的条件，如 `push`、`pull_request`、`schedule`（定时）、`workflow_dispatch`（手动触发）。
- **Job（作业）**：Workflow 中的一个执行单元，多个 Job 可以并行执行或依赖执行。
- **Step（步骤）**：Job 中的具体操作步骤，可以是运行脚本或使用现成的 Action。
- **Action（动作）**：可复用的功能单元，可以从 GitHub Marketplace 获取。

### 5.2 Spring Boot 项目 Workflow 示例

```yaml
name: Spring Boot CI/CD Pipeline

# 触发事件
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 2 * * *'  # 每天凌晨2点定时运行
  workflow_dispatch:      # 支持手动触发

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  # Job 1: 编译和测试
  build-and-test:
    runs-on: ubuntu-latest
    
    services:
      # 启动 MySQL 服务用于集成测试
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test123
          MYSQL_DATABASE: testdb
        ports:
          - 3306:3306
        options: --health-cmd="mysqladmin ping" --health-interval=10s --health-timeout=5s --health-retries=3

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven  # 缓存 Maven 依赖

      - name: Maven 编译
        run: mvn clean compile

      - name: Maven 测试
        run: mvn test
        env:
          SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/testdb

      - name: 上传测试报告
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: target/surefire-reports/

  # Job 2: 代码质量扫描（依赖 Job 1）
  sonarqube:
    needs: build-and-test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # SonarQube 需要完整 Git 历史

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: SonarQube Scan
        uses: sonarsource/sonarqube-scan-action@v2
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}

  # Job 3: Docker 构建和推送
  docker-build-push:
    needs: [build-and-test, sonarqube]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'  # 仅主分支执行
    
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: 设置 Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: 登录容器仓库
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: 提取 Docker 元数据
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha,format=short
            type=raw,value=latest,enable={{is_default_branch}}

      - name: 构建并推送 Docker 镜像
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # Job 4: 部署到服务器
  deploy:
    needs: docker-build-push
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
      - name: 通过 SSH 部署到服务器
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_KEY }}
          script: |
            cd /opt/app/user-service
            docker-compose pull
            docker-compose up -d
            docker image prune -f

      - name: 健康检查
        run: |
          sleep 10
          curl -f http://${{ secrets.DEPLOY_HOST }}/actuator/health || exit 1

      - name: 发送钉钉通知
        if: always()
        uses: yuzhigang0330/dingtalk-notify@v1
        with:
          webhook: ${{ secrets.DINGTALK_WEBHOOK }}
          msgtype: text
          content: |
            【CI/CD通知】${{ github.repository }}
            状态：${{ job.status }}
            分支：${{ github.ref_name }}
            提交：${{ github.sha }}
```

### 5.3 常用 Action 推荐

| Action | 用途 |
|--------|------|
| `actions/checkout` | 检出代码 |
| `actions/setup-java` | 配置 JDK 版本 |
| `actions/cache` | 缓存依赖加速构建 |
| `docker/login-action` | 登录容器镜像仓库 |
| `docker/build-push-action` | 构建并推送 Docker 镜像 |
| `docker/metadata-action` | 提取 Docker 镜像标签/元数据 |
| `sonarsource/sonarqube-scan-action` | SonarQube 代码扫描 |
| `appleboy/ssh-action` | 通过 SSH 连接服务器执行命令 |

---

## 6. 代码质量与安全

### 6.1 SonarQube 代码静态分析

SonarQube 是目前最流行的代码质量平台，提供全面的静态分析能力：

| 维度 | 说明 |
|------|------|
| **Bugs** | 潜在的代码缺陷，可能导致运行时错误 |
| **Vulnerabilities** | 安全漏洞，可能被攻击者利用 |
| **Code Smells** | 代码坏味道，影响可维护性 |
| **Coverage** | 测试覆盖率，需要集成 JaCoCo 等工具 |
| **Duplications** | 重复代码，增加维护成本 |

#### 集成方式

**方式一：Maven 插件集成**

在 `pom.xml` 中配置 SonarQube：

```xml
<properties>
    <sonar.projectKey>my-project</sonar.projectKey>
    <sonar.host.url>http://sonarqube.example.com:9000</sonar.host.url>
    <sonar.login>${env.SONAR_TOKEN}</sonar.login>
    <sonar.coverage.jacoco.xmlReportPaths>target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
</properties>
```

执行扫描：

```bash
mvn clean verify sonar:sonar
```

**方式二：Jenkins + SonarQube 插件**

在 Jenkins 中安装 SonarQube Scanner 插件，配置 SonarQube Server 地址和 Token，然后在 Pipeline 中调用：

```groovy
withSonarQubeEnv('SonarQube Server') {
    sh 'mvn sonar:sonar'
}
```

#### Quality Gate（质量门禁）

Quality Gate 是 SonarQube 的核心特性，定义代码质量的最低标准。常见的质量门禁规则：

- 新代码覆盖率 >= 80%
- 新增 Bugs = 0
- 新增 Vulnerabilities = 0（安全漏洞）
- 新增 Code Smells 不超过 XX 个
- 重复代码比例 <= 3%

当代码不通过 Quality Gate 时，Pipeline 应中断：

```groovy
// Jenkins Pipeline 中等待 SonarQube 检查结果
timeout(time: 2, unit: 'MINUTES') {
    waitForQualityGate abortPipeline: true
}
```

### 6.2 其他代码检查工具

**SpotBugs**（FindBugs 的继任者）：检测 Java 字节码层面的潜在缺陷，如空指针引用、未关闭资源、错误的 equals 实现等。

```bash
mvn spotbugs:spotbugs spotbugs:check
```

**PMD**：检测源代码层面的问题，如未使用的变量、空的 catch 块、过于复杂的表达式等。

```bash
mvn pmd:pmd pmd:check
```

**Checkstyle**：代码风格检查，确保团队代码风格一致（缩进、命名规范、Javadoc 等）。

```bash
mvn checkstyle:check
```

**在 CI 中集成**：将这些检查配置在 Maven POM 中，作为 `verify` 阶段的一部分，确保每次构建都自动执行代码检查。如果检查失败，构建失败，从而阻断低质量代码进入主干。

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.0</version>
    <executions>
        <execution>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

---

## 7. 构建工具与制品管理

### 7.1 Maven 在 CI 中的配置

在 CI 环境中，Maven 的 `settings.xml` 需要配置私有仓库认证信息。通常通过 CI 系统的凭据管理功能注入：

**Jenkins 凭据管理**：

```groovy
withCredentials([string(credentialsId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
    sh 'mvn clean deploy -s $MAVEN_SETTINGS'
}
```

**settings.xml 模板**：

```xml
<settings>
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>${env.NEXUS_USER}</username>
            <password>${env.NEXUS_PASS}</password>
        </server>
        <server>
            <id>nexus-snapshots</id>
            <username>${env.NEXUS_USER}</username>
            <password>${env.NEXUS_PASS}</password>
        </server>
    </servers>
    <mirrors>
        <mirror>
            <id>nexus-mirror</id>
            <mirrorOf>central</mirrorOf>
            <url>https://nexus.example.com/repository/maven-public/</url>
        </mirror>
    </mirrors>
</settings>
```

### 7.2 Gradle 在 CI 中的配置

Gradle Wrapper（`gradlew`）是 CI 环境中的最佳实践，它确保 CI 使用与本地开发完全相同的 Gradle 版本：

```bash
./gradlew clean build test
# 生成依赖锁定文件
./gradlew dependencies --write-locks
```

在 CI 中缓存 Gradle 依赖：

**GitLab CI**：

```yaml
cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .gradle/
    - gradle/wrapper/
```

**GitHub Actions**：

```yaml
- name: Cache Gradle
  uses: actions/cache@v4
  with:
    path: ~/.gradle/caches
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
```

### 7.3 制品管理

**Nexus Repository** 和 **JFrog Artifactory** 是主流的制品管理工具，用于存储和管理：

- **Jar/War 包**：Maven 构建产物
- **Docker 镜像**：容器化应用的镜像
- **npm/PyPI 包**：其他语言构建产物

在 Maven 中配置部署到 Nexus：

```xml
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <url>https://nexus.example.com/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <url>https://nexus.example.com/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

### 7.4 环境变量与凭据管理

CI/CD 系统中涉及大量敏感信息（数据库密码、API Token、SSH 密钥等），绝对不能硬编码在代码中。

**Jenkins 凭据类型**：

| 类型 | 使用场景 |
|------|---------|
| Secret text | API Token、密码 |
| Username with password | 仓库认证、数据库连接 |
| SSH key | Git 仓库访问、服务器连接 |
| Secret file | kubeconfig、证书文件 |
| Certificate | SSL/TLS 证书 |

**GitLab CI Variables**：在 GitLab 项目 Settings → CI/CD → Variables 中配置，支持：

- **Protected**：仅保护分支可用
- **Masked**：在 Job 日志中隐藏值
- **Environment scoped**：按环境区分变量值

**GitHub Actions Secrets**：在 GitHub 仓库 Settings → Secrets and variables → Actions 中配置，每个环境可以有不同的密钥集合。

---

## 8. 部署策略

选择合适的部署策略对于保证服务可用性和降低发布风险至关重要。

### 8.1 滚动部署（Rolling Update）

**原理**：逐个或分批替换旧版本的实例为新版本，整个过程服务不中断。

```
旧版本：[A1] [A2] [A3] [A4] [A5]
步骤1：  [A1-new] [A2] [A3] [A4] [A5]   # 替换1个
步骤2：  [A1-new] [A2-new] [A3] [A4] [A5]  # 再替换1个
...直到全部替换完成
```

**优点**：
- 零停机
- 不需要额外资源（不像蓝绿部署需要两倍资源）
- 可以逐步控制替换节奏

**缺点**：
- 新旧版本短暂共存，可能出现兼容性问题
- 回滚较慢，需要逐步替换回旧版本
- 流量分配不均（旧版本逐渐减少）

**Kubernetes 默认策略**：`kubectl set image` 默认就是滚动更新。

```yaml
# Kubernetes 滚动更新配置
spec:
  replicas: 5
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1       # 最多比期望多启动 1 个 Pod
      maxUnavailable: 0 # 更新期间不允许不可用
```

### 8.2 蓝绿部署（Blue-Green Deployment）

**原理**：维护两套完全相同的生产环境（蓝环境运行当前版本，绿环境部署新版本），流量一次性从蓝环境切换到绿环境。

```
蓝环境 (v1.0) —— 当前运行 —— 负载均衡器 → 用户
绿环境 (v2.0) —— 部署新版本，验证通过

切换后：
蓝环境 (v1.0) —— 闲置作为回滚备份
绿环境 (v2.0) —— 当前运行 —— 负载均衡器 → 用户
```

**优点**：
- 瞬间切换，零停机
- 回滚极快：只需将流量切回蓝环境
- 新版本完全隔离测试，不受旧版本影响

**缺点**：
- 需要双倍资源（两套完整环境）
- 数据库兼容性问题，切换后数据和 Schema 需要前后兼容
- 环境切换可能带来短暂的连接中断

**Kubernetes 实现**：通过 Service Selector 切换：

```bash
# 部署绿环境
kubectl apply -f deployment-green.yaml
# 验证绿环境
kubectl get pods -l version=v2.0
# 切换流量到绿环境
kubectl patch service user-service -p '{"spec":{"selector":{"version":"v2.0"}}}'
```

### 8.3 金丝雀发布（Canary Release）

**原理**：先让新版本服务一小部分用户（如 5% 流量），验证无问题后逐步扩大比例，直到全量替换。

```
v1.0：100% 流量
   ↓
v1.0：95% 流量 + v2.0：5% 流量   # 金丝雀发布开始
   ↓
v1.0：70% 流量 + v2.0：30% 流量  # 逐步扩大
   ↓
v1.0：30% 流量 + v2.0：70% 流量
   ↓
v2.0：100% 流量                    # 全量完成
```

**优点**：
- 影响范围最小，发现问题只有小部分用户受影响
- 可以基于真实用户流量验证新版本
- 可以精细化控制发布节奏

**缺点**：
- 发布过程耗时较长
- 需要流量控制和监控能力
- 多版本共存带来的兼容性挑战

**Kubernetes + Istio 实现**：

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: user-service
spec:
  hosts:
    - user-service
  http:
    - route:
        - destination:
            host: user-service
            subset: v1
          weight: 95
        - destination:
            host: user-service
            subset: v2
          weight: 5
```

### 8.4 回滚策略

无论采用哪种部署策略，都必须有可靠的回滚机制：

**Kubernetes 回滚**：

```bash
# 回滚到上一个版本
kubectl rollout undo deployment/user-service

# 回滚到指定版本
kubectl rollout undo deployment/user-service --to-revision=3

# 查看部署历史
kubectl rollout history deployment/user-service
```

**数据库回滚（Flyway / Liquibase）**：

数据库变更的版本控制和回滚比应用代码更复杂，必须使用专门的工具：

- **Flyway**：通过版本号 SQL 脚本管理迁移，支持 `undo`（需要付费版）。
- **Liquibase**：通过 Changelog 文件管理变更，支持 `rollback` 命令。

最佳实践：**应用代码和数据库变更分开部署**。先部署数据库迁移（向前兼容），再部署应用代码。如果需要回滚，先回滚应用代码，再考虑是否回滚数据库。

```bash
# Flyway 迁移
mvn flyway:migrate

# Liquibase 回滚
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

---

## 9. 监控与告警

CI/CD 的最终目标是快速、可靠地将软件交付到生产环境，而生产环境必须有完善的监控和告警体系来保障服务质量。

### 9.1 Prometheus + Grafana

**Prometheus** 是云原生计算基金会（CNCF）的毕业项目，是 Kubernetes 生态中最主流的监控和告警系统。

核心架构：

```
应用 → 暴露 /metrics 端点 → Prometheus 拉取 → 存储时序数据 → Grafana 可视化
                                                       → AlertManager 告警
```

**Java 应用集成 Prometheus**：

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

配置 `application.yml`：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
```

**Prometheus 配置**：

```yaml
scrape_configs:
  - job_name: 'user-service'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
```

**Grafana 仪表盘**：

- **JVM 监控**：内存使用、GC 情况、线程状态
- **系统指标**：CPU、内存、磁盘、网络
- **业务指标**：QPS、响应时间、错误率、用户在线数
- **CI/CD 指标**：构建频率、部署频率、构建成功率

### 9.2 ELK Stack（Elasticsearch + Logstash + Kibana）

**ELK Stack** 是日志聚合和分析的标准方案：

```
应用日志 → Filebeat 收集 → Logstash 处理/过滤 → Elasticsearch 存储索引 → Kibana 可视化
```

**Filebeat** 是轻量级日志收集器，部署在每个节点上：

```yaml
filebeat.inputs:
  - type: log
    paths:
      - /var/log/apps/*.log
    multiline:
      pattern: '^\d{4}-\d{2}-\d{2}'
      negate: true
      match: after

output.elasticsearch:
  hosts: ["http://elasticsearch:9200"]
  index: "user-service-%{+yyyy.MM.dd}"
```

**Kibana 应用场景**：

- **日志搜索**：按 TraceID 搜索请求全链路日志
- **异常监控**：聚合 ERROR 级别的日志，分析错误趋势
- **告警规则**：当错误日志超过阈值时触发告警
- **Dashboard**：构建日志分析的实时仪表盘

### 9.3 告警与通知

**告警层级**：

| 级别 | 响应时间 | 示例 |
|------|---------|------|
| P0（致命） | 立即处理，<15分钟 | 服务完全不可用、大量 500 |
| P1（严重） | <30分钟 | 响应时间大幅上升、错误率 > 5% |
| P2（警告） | <2小时 | CPU > 80%、磁盘使用率 > 85% |
| P3（通知） | 下个工作日 | 构建失败、测试覆盖率下降 |

**Prometheus AlertManager 配置**：

```yaml
groups:
  - name: user-service
    rules:
      - alert: InstanceDown
        expr: up{job="user-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "用户服务已宕机"
          description: "实例 {{ $labels.instance }} 已不可用超过1分钟"

      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "错误率过高"
          description: "用户服务 5xx 错误率超过 5%"
```

**通知渠道**：

- **钉钉机器人**：通过 Webhook 发送 Markdown 消息
- **企业微信机器人**：通过 Webhook 发送消息
- **飞书机器人**：通过 Webhook 发送卡片消息
- **Slack / PagerDuty**：国际化团队常用

钉钉通知配置示例：

```groovy
// Jenkins Pipeline 中调用
dingtalk(
    robot: 'devops-robot',
    type: 'MARKDOWN',
    title: '【告警】用户服务错误率过高',
    text: [
        "### ⚠️ 用户服务告警",
        "- **告警类型**: 错误率过高",
        "- **当前值**: 12.5%",
        "- **阈值**: >5%",
        "- **时间**: ${new Date().format('yyyy-MM-dd HH:mm:ss')}",
        "- **Pod**: user-service-7d8f9c6b4-abcde",
        "- **操作**: [查看 Grafana](http://grafana.example.com)"
    ]
)
```

---

## 10. 总结与面试要点

### 10.1 CI/CD 核心要点速查表

| 概念 | 关键点 | 面试高频问题 |
|------|-------|-------------|
| **CI 持续集成** | 频繁合并 + 自动构建 + 自动测试 | "CI 解决了什么问题？" |
| **CD 持续交付** | 自动到 Staging，手动确认到生产 | "持续交付和持续部署的区别？" |
| **DevOps** | 开发运维一体化，You Build It You Run It | "DevOps 的核心理念是什么？" |
| **Jenkins Pipeline** | 声明式 > 脚本式，pipeline/stages/steps | "声明式 Pipeline 的核心语法？" |
| **Quality Gate** | 覆盖率 < 80% 阻断合并 | "如何保证代码质量？" |
| **部署策略** | 滚动/蓝绿/金丝雀 | "生产发布如何做到零停机？" |
| **回滚策略** | kubectl rollout undo，数据库版本控制 | "线上出问题如何快速回滚？" |
| **监控告警** | Prometheus + Grafana + ELK | "生产环境如何监控？" |

### 10.2 企业实战 Checklist

以下是一个企业级 CI/CD 体系建设的检查清单，可以作为搭建或评估 CI/CD 体系的参考：

- [ ] **代码管理**：统一分支策略（Git Flow / Trunk Based），保护主干分支
- [ ] **CI 构建**：每次 Push 自动触发编译 + 单元测试，10 分钟内完成
- [ ] **代码质量**：集成 SonarQube + Checkstyle + SpotBugs，Quality Gate 阻断低质量代码
- [ ] **制品管理**：使用 Nexus/Artifactory 管理二进制制品，使用容器镜像仓库管理镜像
- [ ] **自动化测试**：单元测试 + 集成测试 + 接口测试，关键路径覆盖率达到 80%+
- [ ] **环境管理**：开发 / 测试 / 预发 / 生产环境隔离，配置标准化
- [ ] **部署自动化**：使用 K8s / Ansible / Shell 实现一键部署，支持滚动 / 蓝绿 / 金丝雀
- [ ] **回滚能力**：应用回滚 < 5 分钟，数据库变更可逆（Flyway/Liquibase）
- [ ] **监控告警**：Prometheus + Grafana 覆盖系统和业务指标，ELK 集中管理日志
- [ ] **通知机制**：构建结果、部署状态、告警信息通过钉钉/企微/飞书实时通知团队
- [ ] **安全扫描**：依赖漏洞扫描（OWASP Dependency Check）、镜像安全扫描（Trivy）
- [ ] **审计日志**：所有发布操作有记录，谁在什么时间部署了什么版本
- [ ] **容量规划**：定期压测，根据业务增长预留资源

### 10.3 常见面试题

1. **CI 和 CD 分别解决了什么问题？两者的区别是什么？**
   - CI 解决"集成地狱"，频繁合并 + 自动验证，及早发现集成问题。
   - CD 解决"部署恐惧"，自动化部署流程，降低发布风险。
   - 持续交付到生产需人工确认，持续部署全自动。

2. **声明式 Pipeline 和脚本式 Pipeline 的区别？**
   - 声明式：结构清晰，内置 post/when/input 等语法，推荐使用。
   - 脚本式：基于 Groovy，灵活但复杂，适合高级场景。

3. **如何保证 CI/CD 流水线的代码质量？**
   - SonarQube Quality Gate：覆盖率 < 80% 阻断。
   - 集成代码检查工具（Checkstyle/PMD/SpotBugs）。
   - MR 必须通过 CI + Code Review 才能合并。

4. **生产发布如何做到零停机？**
   - 滚动部署（Rolling Update）：逐个替换，零停机但新旧共存。
   - 蓝绿部署（Blue-Green）：两套环境，一次性切换流量。
   - 金丝雀发布（Canary）：小流量验证，逐步扩大到全量。

5. **线上出问题了怎么快速回滚？**
   - 应用层面：kubectl rollout undo、切换负载均衡到旧版本。
   - 数据库层面：Flyway/Liquibase 管理版本，应用先回滚，数据库按需回滚。
   - 关键原则：应用代码和数据库变更解耦，优先保证服务可用。

---

> **参考资源**：
> - [Jenkins 官方文档](https://www.jenkins.io/doc/)
> - [GitLab CI 文档](https://docs.gitlab.com/ee/ci/)
> - [GitHub Actions 文档](https://docs.github.com/en/actions)
> - [SonarQube 文档](https://docs.sonarqube.org/)
> - [Prometheus 文档](https://prometheus.io/docs/)
> - [Kubernetes 部署策略](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
