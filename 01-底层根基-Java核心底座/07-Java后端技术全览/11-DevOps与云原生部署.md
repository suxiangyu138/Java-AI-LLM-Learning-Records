# 🐳 DevOps 与云原生部署

> Docker、Kubernetes、CI/CD、可观测性，掌握现代应用的构建与运维。

---

## 目录

1. [Docker 深度实践](#1-docker-深度实践)
2. [Docker Compose 本地编排](#2-docker-compose-本地编排)
3. [Dockerfile 最佳实践](#3-dockerfile-最佳实践)
4. [Kubernetes 核心概念](#4-kubernetes-核心概念)
5. [K8s 部署 Java 应用](#5-k8s-部署-java-应用)
6. [CI/CD 流水线](#6-cicd-流水线)
7. [GraalVM Native Image](#7-graalvm-native-image)
8. [可观测性三支柱](#8-可观测性三支柱)
9. [日志与监控](#9-日志与监控)
10. [常见面试题](#10-常见面试题)

---

## 1. Docker 深度实践

### 1.1 核心概念

```
Docker 三大核心：

Image（镜像）：
  应用及其运行环境的只读模板
  分层存储（Union FS：Overlay2）
  每个 RUN/COPY 指令创建一个新层

Container（容器）：
  镜像的运行实例
  镜像层 + 可写容器层（Copy-on-Write）
  隔离：Namespace（进程/网络/挂载/用户/...）
  资源限制：Cgroups（CPU/内存/磁盘 IO）

Registry（仓库）：
  存储和分发镜像
  公共：Docker Hub
  私有：Harbor / 阿里云 ACR / AWS ECR

Docker 架构：
  Client (docker CLI)
    ↓ REST API
  Docker Daemon (dockerd)
    ├── containerd（容器运行时管理）
    ├── runc（OCI 容器运行时，实际创建容器）
    └── 镜像管理、网络、存储卷
```

### 1.2 常用命令

```bash
# 镜像
docker pull openjdk:21-jdk          # 拉取镜像
docker images                        # 列出本地镜像
docker rmi <image_id>                # 删除镜像
docker build -t my-app:1.0 .         # 构建镜像
docker tag my-app:1.0 registry.example.com/my-app:1.0  # 打标签
docker push registry.example.com/my-app:1.0  # 推送

# 容器生命周期
docker run -d --name my-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /host/path:/container/path \
  --memory=512m --cpus=1 \
  my-app:1.0

docker ps                            # 运行中的容器
docker ps -a                         # 所有容器（含已停止）
docker stop <container>              # 停止容器
docker start <container>             # 启动已停止的容器
docker restart <container>           # 重启容器
docker rm <container>                # 删除容器
docker rm -f <container>             # 强制删除

# 调试
docker logs -f --tail 100 <container>  # 查看日志
docker exec -it <container> /bin/bash  # 进入容器
docker inspect <container>             # 容器配置详情
docker stats                            # 实时资源使用
docker cp <container>:/path/file ./     # 拷贝文件

# 清理
docker system prune -a               # 清理所有未使用的资源
docker image prune                    # 清理悬空镜像
docker volume prune                   # 清理未使用的卷
```

---

## 2. Docker Compose 本地编排

```yaml
# docker-compose.yml — 本地开发环境
version: '3.8'

services:
  # MySQL
  mysql:
    image: mysql:8.0
    container_name: my-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: mydb
      MYSQL_CHARSET: utf8mb4
      MYSQL_COLLATION: utf8mb4_unicode_ci
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-authentication-plugin=mysql_native_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis
  redis:
    image: redis:7-alpine
    container_name: my-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes --requirepass redis123
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  # 应用
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: my-app
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/mydb
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root123
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PASSWORD: redis123
      JAVA_OPTS: "-Xms512m -Xmx512m"

volumes:
  mysql-data:
  redis-data:

networks:
  default:
    name: my-network
```

---

## 3. Dockerfile 最佳实践

```dockerfile
# === 多阶段构建 Dockerfile ===

# Stage 1: 构建
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# 先 COPY 依赖文件，利用 Docker 缓存层
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# 再 COPY 源代码（源代码改动不会使依赖缓存失效）
COPY src src
RUN ./mvnw package -DskipTests -B

# 解压 JAR（为后续分层优化）
RUN java -Djarmode=layertools -jar target/*.jar extract

# Stage 2: 运行
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 创建非 root 用户（安全最佳实践）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 按变更频率从低到高 COPY（充分利用缓存）
COPY --from=builder /workspace/dependencies/ ./
COPY --from=builder /workspace/spring-boot-loader/ ./
COPY --from=builder /workspace/snapshot-dependencies/ ./
COPY --from=builder /workspace/application/ ./

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

# HEALTHCHECK（K8s 可用此判断容器状态）
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
# 或：ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Dockerfile 最佳实践总结

```
1. 使用官方基础镜像（eclipse-temurin / amazoncorretto）
2. 使用特定版本标签（不用 latest）
3. 多阶段构建（分离构建和运行，最终镜像更小）
4. 按变更频率 COPY（先依赖，后代码 → 利用缓存）
5. 非 root 用户运行（USER appuser）
6. .dockerignore 排除不需要的文件
7. 合并 RUN 指令减少层数
8. 使用 COPY 而不是 ADD（除非需要解压 tar）
9. 生产环境使用 JRE 而非 JDK（镜像小 50%+）
10. 设置 HEALTHCHECK
```

---

## 4. Kubernetes 核心概念

### 4.1 架构

```
K8s 集群架构：

Master 节点（Control Plane）：
  ┌─────────────────────────────────┐
  │ kube-apiserver   → 集群 API 入口 │
  │ etcd              → 分布式 KV 存储│
  │ kube-scheduler    → Pod 调度器   │
  │ kube-controller-manager → 控制器 │
  │ cloud-controller-manager(可选)   │
  └─────────────────────────────────┘

Worker 节点：
  ┌─────────────────────────────────┐
  │ kubelet           → 节点代理     │
  │ kube-proxy        → 网络代理     │
  │ Container Runtime → containerd   │
  └─────────────────────────────────┘
```

### 4.2 核心资源

```
Pod（最小部署单元）：
  - 1 个或多个容器共享网络和存储
  - 每个 Pod 有独立 IP
  - 临时性（可以被销毁和重建）

Deployment（无状态应用部署）：
  - 声明期望的 Pod 副本数
  - 滚动更新 / 回滚
  - 自动重启失败容器

Service（稳定的网络入口）：
  - 为 Pod 提供固定的 ClusterIP
  - 自动负载均衡
  - 类型：ClusterIP（默认）/ NodePort / LoadBalancer

ConfigMap（非敏感配置）
Secret（敏感数据，base64 编码）

StatefulSet（有状态应用，如数据库）：
  - 稳定的网络标识（Pod 名称固定）
  - 稳定的持久化存储
  - 有序部署和扩缩容

Ingress（HTTP(S) 路由规则）：
  - 基于域名/路径路由到不同的 Service
  - 需要 Ingress Controller（如 Nginx Ingress）

HPA（水平自动扩容）：
  - 基于 CPU/内存/自定义指标自动增减 Pod 副本

PV / PVC（持久化存储）：
  - PV：集群级别存储资源
  - PVC：用户存储请求
```

---

## 5. K8s 部署 Java 应用

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  labels:
    app: user-service
spec:
  replicas: 3                           # 副本数
  revisionHistoryLimit: 3               # 保留的历史版本数
  strategy:
    type: RollingUpdate                 # 滚动更新
    rollingUpdate:
      maxSurge: 1         # 更新时最多多出几个 Pod
      maxUnavailable: 0   # 更新时最多几个不可用（0 = 不让一个少）
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
        version: "1.0"
    spec:
      containers:
      - name: user-service
        image: registry.example.com/user-service:1.0
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8080
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: JAVA_OPTS
          value: >-
            -Xms512m -Xmx512m
            -XX:+UseG1GC
            -XX:MaxGCPauseMillis=200
            -XX:+ExitOnOutOfMemoryError
            -Djava.security.egd=file:/dev/./urandom
        envFrom:
        - configMapRef:
            name: user-service-config
        - secretRef:
            name: user-service-secret
        resources:
          requests:           # 调度所需资源
            cpu: "500m"       # 0.5 核
            memory: "512Mi"
          limits:             # 资源上限
            cpu: "1000m"      # 1 核
            memory: "1Gi"
        startupProbe:         # 启动探针（给足够时间启动）
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          failureThreshold: 30
          periodSeconds: 10
        livenessProbe:        # 存活探针（失败重启）
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 0
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 3
        readinessProbe:       # 就绪探针（失败摘流）
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
          timeoutSeconds: 2
          failureThreshold: 3
        lifecycle:
          preStop:            # 优雅停机
            exec:
              command: ["/bin/sh", "-c", "sleep 15"]  # 等待流量切完
      terminationGracePeriodSeconds: 30
      affinity:
        podAntiAffinity:      # Pod 反亲和（分散到不同节点）
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchLabels:
                  app: user-service
              topologyKey: kubernetes.io/hostname
---
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  type: ClusterIP
  selector:
    app: user-service
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
    name: http
---
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-service-config
data:
  application-prod.yml: |
    spring:
      datasource:
        url: jdbc:mysql://mysql-service:3306/user_db
      redis:
        host: redis-service
    logging:
      level:
        root: WARN
---
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
spec:
  ingressClassName: nginx
  rules:
  - host: api.example.com
    http:
      paths:
      - path: /users
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 8080
```

---

## 6. CI/CD 流水线

### 6.1 Jenkins Pipeline

```groovy
// Jenkinsfile
pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'registry.example.com'
        IMAGE_NAME = 'user-service'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://gitlab.example.com/microservice/user-service.git',
                    branch: 'main'
            }
        }

        stage('Build & Test') {
            steps {
                sh './mvnw clean verify -B'
            }
            post {
                success {
                    junit '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('Code Quality') {
            steps {
                // SonarQube 代码分析
                sh './mvnw sonar:sonar -Dsonar.host.url=http://sonarqube:9000'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                    docker build -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} .
                    docker tag ${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} \\
                              ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest
                """
            }
        }

        stage('Push Image') {
            steps {
                sh """
                    docker login ${DOCKER_REGISTRY} -u ${DOCKER_USER} -p ${DOCKER_PASS}
                    docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                    docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest
                """
            }
        }

        stage('Deploy to K8s') {
            steps {
                sh """
                    kubectl set image deployment/user-service \\
                        user-service=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} \\
                        --namespace=production
                    kubectl rollout status deployment/user-service \\
                        --namespace=production --timeout=5m
                """
            }
        }

        stage('Health Check') {
            steps {
                sh """
                    sleep 10
                    curl -f http://api.example.com/actuator/health || exit 1
                """
            }
        }
    }

    post {
        failure {
            // 发告警到钉钉/飞书
            dingtalk(
                robot: 'xxx',
                type: 'TEXT',
                text: "构建失败: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
            )
        }
    }
}
```

### 6.2 GitHub Actions

```yaml
# .github/workflows/build-deploy.yml
name: Build and Deploy

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
    - uses: actions/checkout@v4

    - uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Build with Maven
      run: ./mvnw clean verify -B

    - name: Build Docker Image
      run: docker build -t ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }} .

    - name: Push Image
      run: |
        echo "${{ secrets.GITHUB_TOKEN }}" | docker login ${{ env.REGISTRY }} -u ${{ github.actor }} --password-stdin
        docker push ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}

    - name: Deploy to K8s
      run: |
        kubectl set image deployment/user-service \
          user-service=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
```

---

## 7. GraalVM Native Image

```
什么是 GraalVM Native Image？
  将 Java 应用提前（AOT）编译为本地可执行文件
  不依赖 JVM 即可运行

优势：
  1. 启动极快：0.05 秒 vs JVM 3-5 秒
  2. 内存占用小：50MB vs JVM 250MB+
  3. 即时性能（不需要 JIT 预热）
  4. 镜像体积小

代价：
  1. 编译时间长（几分钟）
  2. 不支持所有 Java 特性（动态代理、反射需额外配置）
  3. 需要做兼容性适配

适用场景：
  - Serverless / Function as a Service（冷启动敏感）
  - 容器化环境（小内存、快速弹性）
  - 短生命周期任务

Spring Boot 3.x + GraalVM Native Image：
  ./mvnw -Pnative native:compile
  或：
  ./gradlew nativeCompile
```

---

## 8. 可观测性三支柱

```
可观测性（Observability）三支柱：

Logs（日志）：
  记录离散事件
  回答：发生了什么？
  工具：ELK、Loki

Metrics（指标）：
  数值化数据，时序聚合
  回答：系统表现如何？有什么趋势？
  工具：Prometheus + Grafana

Traces（链路追踪）：
  请求的完整调用链
  回答：这个请求经历了什么？哪里慢了？
  工具：SkyWalking、Jaeger、Zipkin

三者关系：
  日志包含 TraceId → 从日志跳转到链路追踪
  链路追踪有 Span 耗时 → 可以生成指标
  指标异常 → 查看相关 Trace → 查看详细日志
```

---

## 9. 日志与监控

### 9.1 日志规范

```xml
<!-- logback-spring.xml 关键配置 -->
<configuration>
    <!-- JSON 格式输出（方便 ELK 收集） -->
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"application":"${spring.application.name}"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

### 9.2 Prometheus + Grafana 指标

```
关键 JVM 指标：
  jvm_memory_used_bytes{area="heap"}
    → 堆内存使用量，持续增长 → 内存泄漏

  jvm_gc_pause_seconds_sum / jvm_gc_pause_seconds_count
    → GC 平均耗时，> 500ms 需要关注

  jvm_threads_live_threads
    → 存活的线程数，持续增长 → 线程泄漏

  http_server_requests_seconds_count
    → QPS，rate(http_server_requests_seconds_count[1m])

  http_server_requests_seconds{quantile="0.99"}
    → P99 响应时间

告警规则（Prometheus AlertManager）：
  - P99 延迟 > 1s，持续 5 分钟 → 告警
  - 错误率 > 1%，持续 3 分钟 → 告警
  - 堆内存使用 > 80%，持续 10 分钟 → 告警
  - 服务实例不可用 → 立即告警
```

---

## 10. 常见面试题

### Q1: Docker 镜像如何优化大小？

```
1. 使用 Alpine 基础镜像（~5MB）替代完整 Linux（~200MB）
2. 使用多阶段构建（编译一个镜像，运行一个镜像）
3. 用 JRE 代替 JDK（镜像小 50%+）
4. 合并 RUN 指令减少层
5. 清理包管理器缓存（apt clean / apk cache clean）
6. 使用 .dockerignore 排除不需要的文件
7. 使用 Distroless 镜像（最小运行时，无 Shell）
```

### Q2: K8s Deployment 滚动更新过程？

```
Deployment 控制 ReplicaSet，ReplicaSet 控制 Pod

滚动更新（RollingUpdate）过程：
  旧 ReplicaSet: 3 个 Pod
  → 创建新 ReplicaSet → 启动 1 个新 Pod
  → 新 Pod Ready → 删除 1 个旧 Pod
  → 启动第 2 个新 Pod
  → 新 Pod Ready → 删除第 2 个旧 Pod
  → ...直到全部替换

  maxSurge=1（最多比期望多 1 个）
  maxUnavailable=0（更新期间不能少 Pod）

回滚：
  kubectl rollout undo deployment/user-service
  kubectl rollout undo deployment/user-service --to-revision=2
```

### Q3: 如何做到优雅停机？

```yaml
# Spring Boot
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

# K8s
spec:
  terminationGracePeriodSeconds: 30
  containers:
  - lifecycle:
      preStop:
        exec:
          command: ["/bin/sh", "-c", "sleep 5"]
```

> **上一篇：** [10-测试体系与质量保障](./10-测试体系与质量保障.md)
>
> **下一篇：** [12-JVM原理与性能调优](./12-JVM原理与性能调优.md)
