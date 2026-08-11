# Pipeline 语法
> 声明式 vs 脚本式、pipeline 块结构、stage/steps/agent/post、环境变量与参数化——Jenkins 代码化流水线的完整语法

## 📚 目录
1. [两种语法：声明式 vs 脚本式](#1-两种语法声明式-vs-脚本式)
2. [声明式结构总览](#2-声明式结构总览)
3. [agent：在哪执行](#3-agent在哪执行)
4. [stage/steps：流程骨架](#4-stagesteps流程骨架)
5. [post：收尾处理](#5-post收尾处理)
6. [环境变量与全局变量](#6-环境变量与全局变量)
7. [when 条件与 parallel 并行](#7-when-条件与-parallel-并行)
8. [脚本块与 Groovy 能力](#8-脚本块与-groovy-能力)

## 1. 两种语法：声明式 vs 脚本式

| 维度 | 声明式（Declarative） | 脚本式（Scripted） |
|------|:---:|:---:|
| 风格 | **结构化 YAML 风格**（pipeline {}） | Groovy 脚本（node {}） |
| 学习曲线 | 低 | 高 |
| 语法校验 | ✅ 内置校验 | 运行时才知道 |
| 复杂逻辑 | 有限（script 块转义） | 无限（完整 Groovy） |
| 官方推荐 | **✅ 首选** | 特殊需求才用 |

> 🎯 2026 标准：**一律声明式**——结构化、可校验、好审查；复杂逻辑用 `script {}` 块局部转义 Groovy，而不是整条流水线用脚本式。

## 2. 声明式结构总览

```groovy
// Jenkinsfile（声明式标准骨架）
pipeline {                                    // 顶层：pipeline 块
    agent any                                 // ① 在哪执行
    options {                                 // ② 全局选项
        timeout(time: 30, unit: 'MINUTES')    // 超时
        buildDiscarder(logRotator(numToKeepStr: '100'))  // 构建保留
        disableConcurrentBuilds()             // 禁止并发（共享环境）
    }
    parameters {                              // ③ 参数化（见 02 §6）
        string(name: 'BRANCH', defaultValue: 'main')
    }
    environment {                             // ④ 环境变量
        APP_NAME = 'demo'
        VERSION = "${params.BRANCH}-${BUILD_NUMBER}"
    }
    stages {                                  // ⑤ 阶段序列（核心）
        stage('Checkout') { steps { checkout scm } }
        stage('Build') {
            steps { sh 'mvn -B clean package' }
        }
    }
    post {                                    // ⑥ 收尾（恒执行）
        success { echo '构建成功' }
        failure { echo '构建失败' }
        always { cleanWs() }
    }
}
```

| 块 | 职责 | 必填 |
|----|------|:---:|
| `pipeline` | 顶层容器 | ✅ |
| `agent` | 执行位置 | ✅ |
| `stages` | 阶段序列 | ✅ |
| `options` | 全局选项 | - |
| `parameters` | 参数化 | - |
| `environment` | 环境变量 | - |
| `post` | 收尾处理 | - |

## 3. agent：在哪执行

```groovy
pipeline {
    agent any                                 // 任意可用 Agent
    // agent { label 'java' }                 // 指定标签（见 06）
    // agent { docker { image 'maven:3.9-eclipse-temurin-21' } }  // Docker 容器执行
    // agent { kubernetes { ... } }           // K8s 动态 Pod（见 06）

    stages {
        stage('Build') {
            agent { label 'java' }            // 阶段级 agent（覆盖全局）
            steps { sh 'mvn package' }
        }
        stage('Deploy') {
            agent { label 'deploy-server' }   // 部署阶段换机器
            steps { sh 'deploy.sh' }
        }
    }
}
```

| agent 形式 | 说明 |
|-----------|------|
| `any` | 任意空闲 Agent |
| `label 'xxx'` | 指定标签（按能力分组） |
| `docker { image }` | 容器内执行（构建环境隔离） |
| `kubernetes {}` | K8s 动态 Pod |
| 阶段级 agent | 不同阶段不同环境 |

> 🎯 设计模式：**"构建用 Docker 容器（环境一致），部署用专用 Agent（有凭证/网络）"**——阶段级 agent 让流水线天然支持"构建环境"与"部署环境"分离。

## 4. stage/steps：流程骨架

```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'     // shell
                echo "构建产物: ${env.WORKSPACE}"          // 日志
            }
        }
        stage('Test') {
            steps {
                sh 'mvn -B test'
                junit 'target/surefire-reports/*.xml'     // 测试报告集成
            }
        }
        stage('Package') {
            steps {
                sh 'docker build -t demo:${VERSION} .'
                withCredentials([string(credentialsId: 'harbor-pass',
                        variable: 'HPASS')]) {            // 凭证注入（见 04）
                    sh 'docker login -u admin -p ${HPASS} harbor.local'
                    sh 'docker push harbor.local/demo:${VERSION}'
                }
            }
        }
    }
}
```

| steps 常用指令 | 用途 |
|---------------|------|
| `sh 'cmd'` | 执行 shell（最常用） |
| `echo` | 输出日志 |
| `checkout scm` | 检出代码 |
| `junit` | 集成 JUnit 报告 |
| `archiveArtifacts` | 归档制品 |
| `withCredentials` | 安全注入凭证（别拼进命令！） |
| `script {}` | 转义 Groovy 逻辑 |
| `timeout` / `retry` | 步骤级控制 |

> ⚠️ **凭证铁律**：`docker login -p ${密码}` 这种把密码暴露进构建日志的做法是反模式——必须 `withCredentials` 注入（[04-凭证与安全](04-凭证与安全.md) 专章）。

## 5. post：收尾处理

```groovy
pipeline {
    agent any
    stages { stage('Build') { steps { sh 'mvn package' } } }
    post {
        always {                       // 恒执行（成功失败都跑）
            cleanWs()                  // 清理 workspace
        }
        success {
            archiveArtifacts artifacts: 'target/*.jar'
            junit 'target/surefire-reports/*.xml'
        }
        failure {
            mail to: 'team@example.com',
                 subject: "构建失败: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
        }
        unstable {
            echo '测试失败（黄色）'
        }
        aborted { echo '手动终止' }
    }
}
```

| post 条件 | 触发时机 |
|-----------|---------|
| `always` | 任何结果（清理/通知） |
| `success` | 成功 |
| `failure` | 失败 |
| `unstable` | 测试失败/门禁不过 |
| `aborted` | 手动终止 |
| `cleanup` | 最后（7.2+ 可嵌套条件） |

> 🎯 post 是**"finally 语义"**：失败也要清理 workspace、发通知——比"每个 stage 里 try-catch"优雅得多。面试答"流水线怎么保证失败也清理"→ `post { always { cleanWs() } }`。

## 6. 环境变量与全局变量

```groovy
pipeline {
    environment {
        APP = 'demo'
        VERSION = "${params.BRANCH}-${BUILD_NUMBER}"     // 引用参数/内置变量
        REGION = "${env.NODE_NAME}"                       // env 前缀访问内置
    }
    stages {
        stage('Show') {
            steps {
                sh 'echo $APP $VERSION'                   // shell 里直接用
                echo "Workspace: ${env.WORKSPACE}"        // Groovy 里 env. 访问
            }
        }
    }
}
```

| 内置变量（常用） | 含义 |
|----------------|------|
| `BUILD_NUMBER` | 构建号 |
| `JOB_NAME` | Job 名 |
| `WORKSPACE` | 工作目录 |
| `BRANCH_NAME` | 分支名（多分支流水线） |
| `GIT_COMMIT` | 提交 SHA |
| `NODE_NAME` | 执行 Agent 名 |
| `params.xxx` | 参数化变量（02 §6） |
| `currentBuild.result` | 当前构建结果 |

> ⚠️ **env 访问两种写法**：Groovy 表达式里 `env.VAR` 或 `${env.VAR}`；**shell 里直接 `$VAR`**（环境变量自动注入）。在双引号字符串里用 `${VAR}`，单引号不解析——Groovy 语法细节是 Pipeline 报错高频区。

## 7. when 条件与 parallel 并行

```groovy
pipeline {
    agent any
    stages {
        stage('单元测试') {
            when { branch 'main' }                        // 仅 main 分支
            steps { sh 'mvn test' }
        }
        stage('部署') {
            when {
                expression { params.ENV == 'prod' }        // 仅生产参数
                environment name: 'RUN_DEPLOY', value: 'true'
            }
            steps { sh 'deploy.sh' }
        }
        stage('并行验证') {
            parallel {                                     // ⚠️ parallel 是 stage 内
                stage('SonarQube') { steps { sh 'sonar-scan.sh' } }
                stage('安全扫描') { steps { sh 'trivy image demo' } }
                stage('压测') { steps { sh 'jmeter-run.sh' } }
            }
        }
    }
}
```

| when 条件 | 用途 |
|----------|------|
| `branch 'main'` | 分支过滤 |
| `expression {}` | Groovy 表达式 |
| `environment name/value` | 环境变量判断 |
| `not { }` | 取反 |
| `allOf / anyOf` | 组合条件 |

> 🎯 **parallel 的正确位置**：并行必须放在 `stage('X') { parallel { ... } }` 结构里——多个独立验证（代码扫描/安全/测试）并行执行，总耗时 = 最慢那个。这是流水线提速的第一手段（构建时间从"串行和"变"最大值"）。

## 8. 脚本块与 Groovy 能力

```groovy
pipeline {
    agent any
    stages {
        stage('动态逻辑') {
            steps {
                script {                                   // 转义 Groovy
                    def version = readMavenPom().getVersion()   // 读 POM 版本
                    def isRelease = version.endsWith('-SNAPSHOT') ? false : true
                    if (isRelease) {
                        echo "发布版本: ${version}"
                        env.RELEASE_FLAG = 'true'           // 写环境变量
                    } else {
                        echo "快照版本，跳过发布"
                    }
                }
            }
        }
    }
}
```

| Groovy 能力（script 块内） | 场景 |
|---------------------------|------|
| 变量/条件/循环 | 动态逻辑 |
| `readMavenPom` / `readJSON` | 读取构建上下文 |
| `fileExists` / `dir` | 文件操作 |
| 调用其他 Job | `build 'deploy-job', parameters: [...]` |
| 异常处理 | try-catch（配合 post 兜底） |

> 💡 边界把握：**"声明式是骨架，script 是补丁"**——80% 流水线用纯声明式表达；复杂逻辑收敛进 script 块或共享库函数（[07-共享库与工程化](07-共享库与工程化.md)），别让整条流水线变成 Groovy 泥潭。

---

**下一模块**：[04-凭证与安全](04-凭证与安全.md) / **返回总览**：[00-Jenkins总览](00-Jenkins总览.md)
