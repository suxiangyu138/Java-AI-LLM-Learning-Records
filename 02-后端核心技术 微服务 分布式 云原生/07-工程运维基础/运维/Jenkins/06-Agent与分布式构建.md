# Agent 与分布式构建
> Master/Agent 架构实战：SSH Agent、标签分组、Docker Agent、K8s 动态 Pod——从"单机 Jenkins"到"构建农场"

## 📚 目录
1. [为什么要分布式构建](#1-为什么要分布式构建)
2. [Agent 接入三种方式](#2-agent-接入三种方式)
3. [标签：构建能力的分组](#3-标签构建能力的分组)
4. [Docker Agent：环境隔离](#4-docker-agent环境隔离)
5. [K8s 动态 Agent：按需弹性](#5-k8s-动态-agent按需弹性)
6. [高可用与架构演进](#6-高可用与架构演进)

## 1. 为什么要分布式构建

```text
单机瓶颈：
  · Master 同时干调度 + 构建 → 构建排队
  · 构建环境混在一起（Java/Node/部署互相污染）
  · 一台机器扛不住多个项目的并发

分布式价值：
  · 调度与执行分离（Master 只调度）
  · 按项目/环境分 Agent（隔离）
  · 加机器 = 构建能力线性扩展
  · 动态 Agent（用完即焚）解决环境一致性
```

| 规模 | 架构 |
|------|------|
| 小团队（<10 人） | 单机 Master + 本地构建 |
| 中型（10-50 人） | Master + 2-5 台 SSH Agent（按标签分组） |
| 大型/云原生 | Master(HA) + K8s 动态 Pod Agent |

> 🎯 架构演进主线：**单机 → 静态 Agent 集群 → 动态 Agent（Docker/K8s）**——动态化解决的是"环境一致性与资源利用率"两个老问题。

## 2. Agent 接入三种方式

| 接入方式 | 原理 | 适用 |
|---------|------|------|
| **SSH Agent** | Master SSH 连到 Agent 机器启动代理 | 传统虚拟机（推荐静态场景） |
| JNLP（Inbound） | Agent 主动连 Master（50000 端口） | 防火墙后/容器 Agent |
| **Docker/K8s** | 动态生成容器/Pod 当 Agent | 云原生（推荐新环境） |

```text
SSH Agent 配置（经典姿势）：
  Manage Jenkins → Nodes → New Node → Permanent Agent
  配置：远程根目录、标签、执行器数、SSH 凭证（私钥）
  注意：Agent 机器需装 Java 21 + 构建工具链

JNLP 场景：
  Agent 容器启动时带 -url http://jenkins:8080 -secret xxx
  适合 Agent 在防火墙后（Master 不可达但 Agent 可出网）
```

| Agent 配置项 | 说明 |
|-------------|------|
| 远程根目录 | Agent 上 workspace 的根（如 /data/jenkins-agent） |
| 标签 | 能力分组（见 §3） |
| 执行器数 | 并发槽位（默认 2，IO 密集可多） |
| 启动方式 | SSH / JNLP / Docker |
| 闲置回收 | 动态 Agent 空闲自动销毁 |

> ⚠️ Agent 环境一致性陷阱：**SSH Agent 是"静态机器"，环境靠人维护**——JDK 版本漂移、依赖缺失是"换台 Agent 就构建失败"的根源；要么严格环境管理，要么上 Docker/K8s 动态 Agent（环境代码化）。

## 3. 标签：构建能力的分组

```groovy
// 标签选择（流水线声明要什么样的 Agent）
pipeline {
    agent { label 'java && linux' }        // 同时满足 java 和 linux
    stages {
        stage('Build') {
            agent { label 'java' }         // 阶段级：换环境
            steps { sh 'mvn package' }
        }
        stage('Deploy') {
            agent { label 'deploy-prod' }  // 部署专用（有生产网络）
            steps { sh 'deploy.sh' }
        }
    }
}
```

| 标签设计 | 示例 | 用途 |
|---------|------|------|
| 工具能力 | `java` / `node` / `docker` | 环境需求匹配 |
| 平台 | `linux` / `windows` | 跨平台构建 |
| 环境 | `deploy-prod` / `test-env` | 部署目标隔离 |
| 项目 | `project-a` | 专属资源（抢占场景） |

> 🎯 标签 = **"Agent 的能力声明"**：流水线按标签"点菜"，Master 按标签"配菜"。设计规范：**组合语义**（`java && linux`）优于单一大标签——细粒度标签让资源利用率最高。

## 4. Docker Agent：环境隔离

```groovy
pipeline {
    // 全局：整个流水线在 Docker 容器里跑
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-21'   // 构建环境镜像
            args '-v /data/m2:/root/.m2'           // 挂载依赖缓存（提速关键！）
        }
    }
    stages {
        stage('Build') {
            steps { sh 'mvn -B clean package' }
        }
    }
}
```

```text
Docker Agent 原理：
  每次构建 → 拉取镜像 → 启动容器（作为 Agent）→ 执行流水线 → 销毁
  收益：
  · 环境 100% 一致（镜像即环境声明）
  · 构建隔离（互相不污染）
  · 环境升级 = 改镜像（一处生效）
  · 用完即焚（无环境漂移）

前提：Jenkins 能访问 Docker（docker.sock 挂载或 DIND）
```

| Docker Agent 注意 | 说明 |
|------------------|------|
| 镜像体积 | 构建镜像别贪大（基础工具 + 构建工具链） |
| 依赖缓存 | **挂载 m2/npm 缓存卷**（否则每次全量下载） |
| 嵌套 Docker | 流水线里要 docker build → DinD（挂载 sock） |
| 镜像仓库 | 私有仓库需登录（凭证注入） |
| 网络 | 容器网络需访问 Git/制品库 |

> 🎯 面试点：**"Docker Agent 解决了什么？"**——环境一致性（镜像即环境）+ 隔离（构建互不污染）+ 弹性（按需起停）。它是"环境代码化"在 CI 里的落地（[Docker 体系](../../../11-云原生/Docker/00-Docker知识体系总览.md) 呼应）。

## 5. K8s 动态 Agent：按需弹性

```groovy
pipeline {
    agent {
        kubernetes {
            label 'k8s-agent'                    // Pod 模板名
            defaultContainer 'jnlp'
            yaml '''
                apiVersion: v1
                kind: Pod
                spec:
                  containers:
                  - name: jnlp
                    image: jenkins/inbound-agent:jdk21
                  - name: maven
                    image: maven:3.9-eclipse-temurin-21
                    command: ["cat"]
                    tty: true
                    volumeMounts:
                    - name: m2
                      mountPath: /root/.m2
                  volumes:
                  - name: m2
                    persistentVolumeClaim:
                      claimName: jenkins-m2-pvc
            '''
        }
    }
    stages {
        stage('Build') {
            steps {
                container('maven') {              // 在 maven 容器里执行
                    sh 'mvn -B clean package'
                }
            }
        }
    }
}
```

```text
K8s 动态 Agent 架构：
  Jenkins(HA) → K8s API → 按需创建 Pod（agent 容器 + 工具容器）
  → 流水线在 Pod 容器里执行 → 结束销毁 Pod

价值：
  · 弹性：并发高峰自动扩 Pod，空闲归零（省资源）
  · 一致性：Pod 模板 = 环境声明（同 Docker，但更细粒度）
  · 成本：不建静态构建机群，K8s 集群复用

注意：
  · 需安装 Kubernetes 插件 + 配置 K8s 集群凭证
  · Pod 模板用 YAML 声明（多容器：jnlp + 工具容器）
```

> 🎯 演进结论：**SSH Agent（传统静态）→ Docker Agent（容器化隔离）→ K8s 动态（弹性 + 多容器）**——2026 云原生团队的标准形态是 K8s 动态 Agent；内网/虚拟化环境保留 SSH Agent 也是完全合理的（看基础设施）。

## 6. 高可用与架构演进

```text
高可用（HA）演进路径：
  ① 单机：Jenkins + 数据卷定期备份（小团队够用）
  ② 主备：数据卷同步 + 备机（冷备/温备）
  ③ 云原生 HA：K8s 部署 + PVC + 多副本（2026 生产标准）

K8s HA 部署要点：
  · StatefulSet/PVC 持久化（JENKINS_HOME 在共享存储）
  · 多副本（Active 单写 + 读副本有限支持）
  · 前置负载均衡（Ingress）+ HTTPS
  · Agent 全动态（Pod）
  · 备份自动化（CronJob 快照 PVC）
```

| HA 级别 | 手段 | 适用 |
|---------|------|------|
| 备份兜底 | 每日备份 + 定期演练 | 中小团队（最实用！） |
| 主备 | 数据卷同步 | 中大型 |
| K8s 多副本 | 共享 PVC + Ingress | 云原生生产 |

> ⚠️ Jenkins HA 的现实：**Jenkins 是"单写者"架构**（多个 Master 同时写 JENKINS_HOME 会损坏）——真正的 HA 是"共享存储 + 单活跃 + 快速故障切换"，不是普通多副本。**对 90% 的团队，可靠的备份 + 快速恢复演练比复杂 HA 更实际**（[08](08-生产运维与故障排查.md) 备份篇）。

---

**下一模块**：[07-共享库与工程化](07-共享库与工程化.md) / **返回总览**：[00-Jenkins总览](00-Jenkins总览.md)
