# DevOps 与 CI/CD 名词剖析

> 🚀 Docker 容器化、Kubernetes 编排、Jenkins/GitLab CI 流水线、监控告警 Prometheus+Grafana —— 现代运维的 35+ 核心概念

---

## 📚 目录

1. [Docker 容器化](#1-docker-容器化)
2. [Kubernetes 编排](#2-kubernetes-编排)
3. [CI/CD 持续集成与交付](#3-cicd-持续集成与交付)
4. [监控与告警](#4-监控与告警)
5. [日志收集](#5-日志收集)
6. [基础设施即代码](#6-基础设施即代码)

---

## 1. Docker 容器化

### 1.1 核心概念

| 概念 | 说明 |
|------|------|
| **Image (镜像)** | 只读模板，包含运行环境和应用 |
| **Container (容器)** | 镜像的运行实例，轻量隔离 |
| **Dockerfile** | 镜像构建定义文件 |
| **Docker Compose** | 多容器编排（单机） |
| **Registry** | 镜像仓库（Docker Hub / Harbor / 私有仓库） |
| **UnionFS** | 联合文件系统，分层构建镜像的基础 |

### 1.2 容器 vs 虚拟机

| 维度 | Docker 容器 | 虚拟机 VM |
|------|:---------:|:-------:|
| **启动速度** | 秒级 | 分钟级 |
| **资源占用** | MB 级 | GB 级 |
| **隔离级别** | 进程级（共享宿主机内核） | 硬件级（完整 OS） |
| **迁移性** | Dockerfile 一键构建 | 镜像导出繁琐 |
| **密度** | 一台机器跑几十上百容器 | 一台机器跑几个 VM |
| **内核** | 共享宿主机内核 | 每个 VM 独立内核 |

### 1.3 常用命令

```bash
# 镜像
docker pull openjdk:21          # 拉取镜像
docker build -t my-app:1.0 .    # 构建镜像
docker images                    # 查看本地镜像
docker rmi my-app:1.0           # 删除镜像

# 容器
docker run -d -p 8080:8080 --name app my-app:1.0  # 启动
docker ps -a                    # 查看所有容器
docker logs -f app              # 查看日志
docker exec -it app /bin/bash   # 进入容器
docker stop/start/restart app   # 停止/启动/重启
docker rm app                   # 删除容器

# 清理
docker system prune -a          # 清理所有未使用的资源
```

### 1.4 Dockerfile 最佳实践

```dockerfile
# 多阶段构建（缩小最终镜像体积）
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# 最佳实践：
# ✅ 多阶段构建 → 最终镜像只有 JRE，没有 Maven 和源码
# ✅ .dockerignore → 排除不必要文件
# ✅ 分层利用缓存 → 先 COPY pom.xml 下载依赖，再 COPY src
# ✅ 用非 root 用户运行
```

---

## 2. Kubernetes 编排

### 2.1 核心概念映射

```text
Kubernetes 核心资源拓扑：

  Cluster
    └── Node（工作节点，物理机/VM）
         └── Pod（最小调度单元，1+ Containers）
              └── Container（Docker 容器）

  管理资源：
    ├── Deployment     → 无状态应用（滚动更新、回滚）
    ├── StatefulSet    → 有状态应用（有序、持久化）
    ├── DaemonSet      → 每个 Node 运行一个（日志采集、监控 Agent）
    ├── Job / CronJob  → 一次性/定时任务
    ├── Service        → 服务发现 + 负载均衡（ClusterIP/NodePort/LoadBalancer）
    ├── Ingress        → 外部 HTTP/HTTPS 路由（类似 Nginx）
    ├── ConfigMap      → 配置管理（非敏感）
    ├── Secret         → 密钥管理（敏感信息）
    ├── PersistentVolume (PV)      → 持久化存储卷
    ├── PersistentVolumeClaim (PVC) → 存储请求
    └── Namespace      → 逻辑隔离（按环境/团队划分）
```

### 2.2 Deployment 核心配置

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 3                     # 3 个 Pod 副本
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: registry.example.com/user-service:1.2.3
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        resources:
          requests:                # 最少资源（调度依据）
            memory: "512Mi"
            cpu: "500m"
          limits:                  # 最大资源（超过可能被 kill）
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:             # 存活探针（失败→重启）
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:            # 就绪探针（失败→摘除流量）
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

### 2.3 Service 类型

| 类型 | 说明 | 使用场景 |
|------|------|---------|
| **ClusterIP** | 集群内部访问（默认） | 微服务间调用 |
| **NodePort** | 通过 Node IP:Port 访问 | 测试/临时暴露 |
| **LoadBalancer** | 云厂商 LB 对外暴露 | 生产环境入口 |
| **ExternalName** | 映射到外部 DNS | 外部服务映射 |

---

## 3. CI/CD 持续集成与交付

### 3.1 概念映射

```text
CI = Continuous Integration（持续集成）
  → 开发者频繁提交代码 → 自动构建 → 自动测试 → 反馈
  → 目标：尽早发现集成问题

CD = Continuous Delivery / Deployment（持续交付/部署）
  → Delivery：代码随时可发布（手动触发发布）
  → Deployment：代码自动发布到生产环境
```

### 3.2 Jenkins Pipeline

```groovy
// Jenkinsfile 声明式流水线
pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'registry.example.com'
        IMAGE_NAME = 'user-service'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {  // 代码质量
            steps {
                sh 'mvn sonar:sonar'
            }
        }

        stage('Docker Build & Push') {
            steps {
                sh "docker build -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER} ."
                sh "docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}"
            }
        }

        stage('Deploy to K8s') {
            steps {
                sh "kubectl set image deployment/${IMAGE_NAME} ${IMAGE_NAME}=${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}"
                sh "kubectl rollout status deployment/${IMAGE_NAME}"
            }
        }
    }
}
```

### 3.3 GitLab CI vs GitHub Actions vs Jenkins

| 维度 | Jenkins | GitLab CI | GitHub Actions |
|------|:------:|:--------:|:------------:|
| **部署方式** | 自托管 | SaaS/自托管 | SaaS/自托管 |
| **配置语言** | Groovy (Jenkinsfile) | YAML (.gitlab-ci.yml) | YAML (.github/workflows/...) |
| **插件生态** | 极丰富 | 中等 | 丰富（Marketplace） |
| **门槛** | 较高 | 中等 | 低 |
| **免费额度** | 完全免费(自托管) | 400分钟/月 | 2000分钟/月(公开) |
| **适合** | 企业定制 | 内源开发 | 开源项目 |

---

## 4. 监控与告警

### 4.1 三大支柱

```text
可观测性三大支柱：

  Metrics (指标)    → Prometheus + Grafana
    "系统当前 CPU 使用率是多少？"

  Logging (日志)    → ELK (Elasticsearch + Logstash + Kibana) / Loki
    "昨天的错误日志里有什么？"

  Tracing (链路追踪) → Jaeger / Zipkin / SkyWalking
    "这个慢请求经过了哪些服务，哪里耗时最长？"
```

### 4.2 Prometheus + Grafana

```yaml
# Spring Boot Actuator + Micrometer 暴露指标
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
```

```text
Prometheus 核心概念：
  → Pull 模式：Prometheus Server 主动抓取 /actuator/prometheus
  → 时序数据库：高效存储 Metric
  → PromQL：灵活查询语言
    rate(http_server_requests_seconds_count[5m])  -- QPS
    histogram_quantile(0.99, ...)                  -- P99 延迟

Grafana：
  → 连接 Prometheus 数据源
  → 可视化面板（Dashboard）
  → 告警规则配置
```

### 4.3 关键监控指标

| 层级 | 指标 | 说明 |
|------|------|------|
| **应用** | QPS, P99/P95 延迟, 错误率 | 黄金信号！ |
| **JVM** | 堆内存使用率, GC 频率/耗时, 线程数 | JVM 健康 |
| **中间件** | 连接池使用率, 慢查询 | 数据库/Redis 健康 |
| **容器** | CPU/Memory/Disk/Network | K8s 调度依据 |
| **业务** | 订单量, 支付成功率 | 业务健康 |

---

## 5. 日志收集

```text
ELK 日志收集架构：

  App Container (Filebeat)  →  Logstash  →  Elasticsearch  →  Kibana
       (收集日志)              (处理+过滤)   (存储+索引)       (可视化)

  Filebeat：轻量日志采集 Agent，部署在每个 Node（DaemonSet）
  Logstash：日志清洗、结构化、过滤
  Elasticsearch：存储日志，支持全文搜索
  Kibana：可视化查询日志、创建 Dashboard

替代方案：
  → Grafana Loki（轻量、成本低、适合 K8s）
  → PLG（Promtail + Loki + Grafana）
```

---

## 6. 基础设施即代码

| 工具 | 用途 | 说明 |
|------|------|------|
| **Terraform** | 多云基础设施管理 | 声明式管理 AWS/Azure/GCP/K8s 资源 |
| **Ansible** | 配置管理 | Agentless、SSH 推送 |
| **Helm** | K8s 包管理 | "K8s 的 apt/yum" |
| **ArgoCD** | GitOps 部署 | 以 Git 仓库为单一真实来源 |
| **Docker Compose** | 单机多容器编排 | 开发/测试环境 |

```yaml
# GitOps 核心思想：
# 1. Git 仓库 = 期望状态（Declarative）
# 2. ArgoCD / Flux 持续对比 Git 与实际集群状态
# 3. 有偏差 → 自动同步（或告警）
# 4. 所有变更通过 PR + 审批 → Git 记录完整审计日志
```

---

> 🎯 **一句话总结**：DevOps 的本质是 **用自动化的方式打通开发到运维的壁垒** —— Docker 统一环境、K8s 编排调度、CI/CD 自动化交付、Prometheus+Grafana 持续监控。

---

**下一模块**：[10-性能调优名词剖析](./10-性能调优名词剖析.md)

---

*创建于：2026年7月*
