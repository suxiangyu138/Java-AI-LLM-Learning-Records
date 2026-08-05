# 03 DevOps与云原生工具链

> "你了解CI/CD吗？会Docker吗？"——DevOps能力是区分初级和中级后端工程师的关键分水岭

## 📚 目录

1. [容器化 — Docker & Docker Compose](#1-容器化--docker--docker-compose)
2. [容器编排 — Kubernetes (K8s)](#2-容器编排--kubernetes-k8s)
3. [CI/CD — 持续集成与持续部署](#3-cicd--持续集成与持续部署)
4. [代码质量与规范](#4-代码质量与规范)
5. [日志与监控集成](#5-日志与监控集成)
6. [基础设施即代码 (IaC)](#6-基础设施即代码-iac)

---

## 1. 容器化 — Docker & Docker Compose

> 容器化是云原生时代的"基础设施"。面试中至少需要理解Dockerfile优化、镜像分层、容器网络等核心概念。

### 1.1 Docker在企业中的角色

```
开发阶段                    CI/CD阶段                    生产阶段
┌──────────┐              ┌──────────┐              ┌──────────┐
│ 本地开发   │   Docker    │ 代码提交   │  推送镜像    │ K8s集群   │
│ Docker    │ ──────────→ │ Jenkins   │ ──────────→ │ 容器编排   │
│ 环境一致   │   Compose   │ 构建镜像   │   Harbor    │ 自动扩缩容 │
└──────────┘              └──────────┘              └──────────┘
```

> 💡 **核心价值**："It works on my machine"成为历史。Docker确保开发、测试、生产环境完全一致。

**三大角色**：

| 环节 | 角色 | 说明 |
|------|------|------|
| **开发** | 环境一致性 | Docker Compose一键启动MySQL+Redis+应用，告别"本地环境搭半天" |
| **CI/CD** | 构建标准件 | 每次提交都生成不可变镜像，版本可追溯、部署可回滚 |
| **部署** | 轻量运行 | 秒级启动、资源隔离、弹性伸缩 |

---

### 1.2 Dockerfile最佳实践

#### 多阶段构建

```dockerfile
# ============ 第一阶段：编译 ============
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制pom并先下载依赖（利用Docker缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并编译
COPY src ./src
RUN mvn clean package -DskipTests

# ============ 第二阶段：运行 ============
# 使用精简JRE镜像，大幅减小体积
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 从builder阶段复制编译产物
COPY --from=builder /app/target/*.jar app.jar

# 非root用户运行（安全最佳实践）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# 时区设置
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata

# JVM参数通过环境变量传入，支持灵活配置
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
```

#### 分层优化

```
Docker镜像层结构（从下到上）:
  Layer 1: OS基础层 (alpine: 5MB)
  Layer 2: JRE层 (temurin-jre: 50MB)
  Layer 3: 依赖层 (maven依赖: 80MB)  ← 不变时复用
  Layer 4: 应用层 (JAR: 50MB)        ← 经常变
  ──────────────────────────────
  Total: ~185MB (vs 未优化: ~400MB)
```

**优化要点**：

| 优化项 | 做法 | 效果 |
|-------|------|------|
| **多阶段构建** | 编译用Maven镜像，运行用JRE镜像 | 体积减少50%以上 |
| **分层缓存** | 先COPY pom.xml下载依赖，再COPY源码 | 不改依赖时秒级构建 |
| **基础镜像精简** | 使用alpine或distroless | 体积小、攻击面小 |
| **非root用户** | 创建专用用户运行 | 安全 |
| **指令合并** | `RUN &&` 链式命令减少层数 | 减少不必要的层 |
| **.dockerignore** | 排除target/.git/node_modules | 避免无效构建上下文 |

```dockerignore
# .dockerignore
target/
.git/
.gitignore
*.md
node_modules/
.idea/
*.iml
```

---

### 1.3 Docker Compose编排开发环境

```yaml
# docker-compose.yml — 一键启动全套开发环境
version: "3.8"

services:
  # ======== 应用 ========
  app:
    build: .
    image: myapp:latest
    container_name: myapp
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/mydb?useSSL=false
      SPRING_REDIS_HOST: redis
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_ELASTICSEARCH_URIS: http://elasticsearch:9200
    volumes:
      - ./logs:/app/logs
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - backend
    restart: unless-stopped

  # ======== MySQL ========
  mysql:
    image: mysql:8.0
    container_name: mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: mydb
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - backend

  # ======== Redis ========
  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes --requirepass redis123
    networks:
      - backend

  # ======== RabbitMQ ========
  rabbitmq:
    image: rabbitmq:3.12-management
    container_name: rabbitmq
    ports:
      - "5672:5672"   # AMQP
      - "15672:15672" # 管理界面
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin123
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    networks:
      - backend

  # ======== Elasticsearch ========
  elasticsearch:
    image: elasticsearch:8.11
    container_name: elasticsearch
    ports:
      - "9200:9200"
    environment:
      discovery.type: single-node
      ES_JAVA_OPTS: -Xms512m -Xmx512m
      xpack.security.enabled: false
    volumes:
      - es-data:/usr/share/elasticsearch/data
    networks:
      - backend

volumes:
  mysql-data:
  redis-data:
  rabbitmq-data:
  es-data:

networks:
  backend:
    driver: bridge
```

> 💡 **企业实践**：开发阶段使用Docker Compose统一管理所有中间件。每个新成员只需一条命令 `docker-compose up -d`，即可获得完整开发环境。

---

### 1.4 常用Docker命令速查（面试高频）

| 类别 | 命令 | 说明 |
|------|------|------|
| **镜像管理** | `docker images` | 查看本地镜像列表 |
| | `docker pull nginx:alpine` | 拉取镜像 |
| | `docker build -t myapp:1.0 .` | 构建镜像 |
| | `docker rmi $(docker images -q)` | 删除全部镜像 |
| **容器管理** | `docker ps -a` | 查看所有容器 |
| | `docker run -d --name app -p 8080:8080 myapp` | 运行容器 |
| | `docker stop $(docker ps -q)` | 停止所有容器 |
| **进入容器** | `docker exec -it container_id sh` | 进入容器内部（⭐高频） |
| | `docker attach container_id` | 附加到容器 |
| **查看日志** | `docker logs -f --tail 100 container_id` | 跟踪查看最近100行日志（⭐高频） |
| **资源限制** | `docker run --memory=512m --cpus=1.0` | 限制内存和CPU（⭐高频） |
| **网络** | `docker network ls` | 查看网络 |
| | `docker network create --driver bridge my-net` | 创建网络 |
| **数据卷** | `docker volume ls` | 查看数据卷 |
| | `docker run -v /host/path:/container/path` | 挂载宿主机目录 |

```bash
# 面试现场常用命令演示

# 1. 进入容器排查问题
docker exec -it myapp sh
# 然后可以在容器内部查看日志、检查环境变量、测试网络等

# 2. 查看Java应用堆栈
docker exec -it myapp jstack -l 1 > /tmp/thread.dump

# 3. 从宿主机拷贝文件
docker cp myapp:/app/logs/app.log ./app.log

# 4. 查看容器资源占用
docker stats myapp

# 5. 清理所有无用资源（慎用！）
docker system prune -af
```

---

### 1.5 Docker网络与数据卷

#### 网络模式

| 网络模式 | 说明 | 适用场景 |
|---------|------|---------|
| `bridge` | 默认，容器有独立IP，通过网桥通信 | 单机多容器 |
| `host` | 容器直接使用宿主机网络栈 | 性能敏感型（如Nginx） |
| `none` | 无网络 | 安全沙箱 |
| `overlay` | 跨主机容器网络 | Swarm/K8s多节点 |

#### 数据持久化

```yaml
# 三种数据持久化方式对比
volumes:
  # 1. Volume（推荐，由Docker管理）
  - mysql-data:/var/lib/mysql

  # 2. Bind Mount（开发调试方便）
  - ./logs:/app/logs

  # 3. tmpfs（内存中，不持久化）
  - type: tmpfs
    target: /tmp
```

> 🎯 **核心要点**：Docker面试三连问——"Dockerfile怎么优化？"（多阶段构建+分层缓存）、"如何进入运行中的容器？"（`docker exec -it`）、"容器和宿主机怎么传文件？"（`docker cp`）。

---

## 2. 容器编排 — Kubernetes (K8s)

> K8s是容器编排的事实标准，面试中至少需要掌握核心概念、Java应用部署、资源协调。

### 2.1 K8s核心概念面试速成

| 概念 | 类型 | 说明 | 类比 |
|------|------|------|------|
| **Pod** | 最小调度单元 | 一组紧密相关的容器 | 虚拟机中的进程 |
| **Deployment** | 工作负载 | 管理Pod副本、滚动更新、回滚 | 应用管理员 |
| **Service** | 网络抽象 | 为Pod提供稳定访问入口 | 负载均衡器 |
| **Ingress** | 入口网关 | 域名/路径路由到Service | Nginx网关 |
| **ConfigMap** | 配置 | 非敏感配置（环境变量/文件） | 配置文件 |
| **Secret** | 敏感配置 | Base64编码的敏感信息 | 密码本 |
| **Namespace** | 隔离空间 | 逻辑隔离资源 | 项目分组 |
| **PersistentVolume** | 存储 | 独立于Pod生命周期的存储 | 外挂硬盘 |

### 2.2 企业K8s架构图

```
                     ┌─────────────────────────────┐
                     │     Nginx Ingress Controller  │
                     │     (域名 + 路由 + SSL)       │
                     └──────────┬──────────────────┘
                                │
                     ┌──────────▼──────────────────┐
                     │      Service (ClusterIP)      │
                     │    myapp-service:8080          │
                     └──────────┬──────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
  ┌─────▼─────┐          ┌─────▼─────┐          ┌─────▼─────┐
  │   Pod     │          │   Pod     │          │   Pod     │
  │  myapp-1  │          │  myapp-2  │          │  myapp-3  │
  │  app.jar  │          │  app.jar  │          │  app.jar  │
  │ Sidecar   │          │ Sidecar   │          │ Sidecar   │
  │(监控/日志) │          │(监控/日志) │          │(监控/日志) │
  └───────────┘          └───────────┘          └───────────┘
        │                       │                       │
        └───────────────────────┼───────────────────────┘
                                │
                     ┌──────────▼──────────────────┐
                     │    ConfigMap / Secret         │
                     │    (配置 + 密码)              │
                     └─────────────────────────────┘
                     ┌─────────────────────────────┐
                     │    PV / PVC                   │
                     │    (持久化存储)               │
                     └─────────────────────────────┘

Master节点:
  ┌─────────────────────────────────────────────┐
  │ API Server  |  Scheduler  |  Controller      │
  │  etcd (集群状态存储)                          │
  └─────────────────────────────────────────────┘

Node节点:
  ┌─────────────────────────────────────────────┐
  │ Kubelet  |  Kube-Proxy  |  Container Runtime │
  └─────────────────────────────────────────────┘
```

---

### 2.3 一个Java应用的K8s部署YAML详解

```yaml
# ============ 1. Namespace ============
apiVersion: v1
kind: Namespace
metadata:
  name: production
---
# ============ 2. ConfigMap ============
apiVersion: v1
kind: ConfigMap
metadata:
  name: myapp-config
  namespace: production
data:
  application.yml: |
    server:
      port: 8080
    spring:
      datasource:
        url: jdbc:mysql://mysql-service:3306/mydb?useSSL=false
      redis:
        host: redis-service
---
# ============ 3. Secret ============
apiVersion: v1
kind: Secret
metadata:
  name: myapp-secret
  namespace: production
type: Opaque
data:
  # echo -n "root123" | base64
  db-password: cm9vdDEyMw==
---
# ============ 4. Deployment ============
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
  namespace: production
  labels:
    app: myapp
spec:
  replicas: 3                    # 3个副本
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1                # 最大额外Pod数
      maxUnavailable: 0          # 最大不可用Pod数（零宕机部署）
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
        - name: myapp
          image: harbor.example.com/myapp:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
              protocol: TCP
          # 资源请求与限制
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
          # 健康检查
          livenessProbe:           # 存活探针（是否存活，否则重启）
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:          # 就绪探针（是否可服务，否则摘除流量）
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 5
          # 挂载配置
          volumeMounts:
            - name: config
              mountPath: /app/config
            - name: secret
              mountPath: /app/secret
            - name: logs
              mountPath: /app/logs
          # JVM参数
          env:
            - name: JAVA_OPTS
              value: "-Xms512m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"
            - name: SPRING_PROFILES_ACTIVE
              value: "production"
      volumes:
        - name: config
          configMap:
            name: myapp-config
        - name: secret
          secret:
            secretName: myapp-secret
        - name: logs
          emptyDir: {}
---
# ============ 5. Service ============
apiVersion: v1
kind: Service
metadata:
  name: myapp-service
  namespace: production
spec:
  type: ClusterIP                    # 集群内可访问
  selector:
    app: myapp
  ports:
    - port: 8080                     # Service端口
      targetPort: 8080               # Pod端口
---
# ============ 6. Ingress ============
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp-ingress
  namespace: production
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - api.example.com
      secretName: tls-secret
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: myapp-service
                port:
                  number: 8080
```

---

### 2.4 K8s中Java应用的内存/CPU配置

> ⚠️ **关键问题**：K8s中Java应用感知的是容器内存还是宿主机内存？答案是——Java 10+ 通过 `UseContainerSupport` 默认感知Cgroup限制。

```yaml
# JVM与容器资源协调

# Java 8u131+ 需要显式开启
# Java 10+ 默认开启 -XX:+UseContainerSupport

# 推荐配置方式：
# requests 和 limits 保持一致（Guaranteed QoS），避免OOMKill
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "500m"

# 对应的JVM参数
# -Xms 和 -Xmx 推荐设置为 limit 的 70%~80%
# 预留部分内存给JVM自身、堆外内存、线程栈
env:
  - name: JAVA_OPTS
    value: >
      -Xms768m -Xmx768m
      -XX:+UseContainerSupport
      -XX:MaxRAMPercentage=75.0
      -XX:+UseG1GC
      -XX:+HeapDumpOnOutOfMemoryError
      -XX:HeapDumpPath=/app/logs/heapdump.hprof
```

**内存分配模型**：

```text
容器Limits: 1Gi (1024Mi)
  ┌─────────────────────────────────────┐
  │ JVM堆 (Xmx=768m, ~75%)              │
  │  - 新生代 (Eden + Survivor)          │
  │  - 老年代                           │
  ├─────────────────────────────────────┤
  │ JVM堆外 (~256m, ~25%)               │
  │  - Metaspace (默认无上限)            │
  │  - Stack (每个线程 ~1MB)             │
  │  - Direct Buffer                    │
  │  - Code Cache                       │
  ├─────────────────────────────────────┤
  │ 系统预留                             │
  └─────────────────────────────────────┘
```

---

### 2.5 灰度发布/滚动更新/回滚

```yaml
# 滚动更新策略
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  replicas: 5
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1            # 滚动时最多额外启动1个新Pod
      maxUnavailable: 1      # 滚动时最多允许1个旧Pod不可用
  minReadySeconds: 30        # Pod就绪后等待30秒才视为可用
  revisionHistoryLimit: 10   # 保留10个历史版本用于回滚
```

```bash
# 滚动更新
kubectl set image deployment/myapp myapp=harbor.example.com/myapp:2.0.0

# 查看更新状态
kubectl rollout status deployment/myapp

# 查看历史版本
kubectl rollout history deployment/myapp

# 回滚到上一个版本
kubectl rollout undo deployment/myapp

# 回滚到指定版本
kubectl rollout undo deployment/myapp --to-revision=3

# 暂停/恢复滚动（金丝雀发布）
kubectl rollout pause deployment/myapp
# ... 监控一段时间 ...
kubectl rollout resume deployment/myapp
```

> 💡 **金丝雀发布（Canary Deployment）**：先更新少量实例，验证没问题后再全量更新。可以通过 `maxSurge: 0, maxUnavailable: 1` 配合Ingress流量比例实现。

---

### 2.6 K8s vs Docker Swarm vs 传统部署对比

| 对比维度 | 传统部署 | Docker Swarm | Kubernetes |
|---------|---------|-------------|------------|
| **自动化** | 手动 | 中等 | 高度自动化 |
| **服务发现** | Nginx配置 | DNS轮询 | Service + DNS |
| **负载均衡** | Nginx/HAProxy | 内置 | Service + Ingress |
| **自动扩缩容** | 无 | 手动 | HPA (Horizontal Pod Autoscaler) |
| **灰度发布** | 脚本化 | 简单支持 | 完整支持（Rolling/Canary/Blue-Green） |
| **自我修复** | 无 | 容器重启 | 自动重启/调度/健康检查 |
| **存储编排** | 手动挂载 | 简单 | PV/PVC 丰富生态 |
| **学习曲线** | 低 | 中 | 高 |
| **运维成本** | 高 | 中等 | 中低（稳定后） |
| **社区生态** | - | Docker生态 | CNCF生态，最活跃 |
| **推荐场景** | 小项目/传统企业 | 简单容器化 | 中大规模/云原生 |

> 🎯 **核心要点**：K8s面试三连问——"Pod和Deployment的关系？"（Deployment管理Pod副本和更新策略）、"Service的类型？"（ClusterIP/NodePort/LoadBalancer/ExternalName）、"如何实现零宕机部署？"（readinessProbe + rollingUpdate maxUnavailable=0）。

---

## 3. CI/CD — 持续集成与持续部署

> CI/CD是DevOps的核心实践。面试中至少需要理解Pipeline流程、常见工具对比。

### 3.1 CI/CD Pipeline全景

```
代码提交 → 代码扫描 → 单元测试 → 构建 → 集成测试 → 镜像构建 → 镜像推送 → 部署

┌────────┐  ┌────────┐  ┌────────┐  ┌──────┐  ┌────────┐  ┌──────┐  ┌──────┐  ┌──────┐
│ Commit  │→│ 代码    │→│ 单元    │→│ Compile│→│ 集成   │→│ 镜像  │→│ 镜像  │→│ 部署  │
│ & Push  │  │ 扫描    │  │ 测试    │  │ & Build│  │ 测试   │  │ 构建  │  │ 推送  │  │
└────────┘  └────────┘  └────────┘  └──────┘  └────────┘  └──────┘  └──────┘  └──────┘
                                  │          ↑
                                  └──── 失败时阻断 Pipeline，通知开发者 ────┘
```

**各阶段工具选型**：

| 阶段 | 工具选型 | 说明 |
|------|---------|------|
| 代码扫描 | SonarQube / SpotBugs / Alibaba P3C | 静态代码分析 |
| 单元测试 | JUnit 5 + Mockito + JaCoCo | 覆盖率 > 80% |
| 构建 | Maven / Gradle | 编译打包 |
| 集成测试 | Testcontainers / Postman Newman | 集成测试 |
| 镜像构建 | Docker / Jib / Buildpacks | 构建容器镜像 |
| 镜像推送 | Harbor / Nexus / 阿里云ACR | 镜像仓库 |
| 部署 | Jenkins / ArgoCD / Spinnaker | 自动化部署 |

---

### 3.2 Jenkins vs GitHub Actions vs GitLab CI 对比

| 对比维度 | Jenkins | GitHub Actions | GitLab CI |
|---------|---------|---------------|-----------|
| **部署方式** | 自建服务 | SaaS（云端执行） | SaaS + 自建Runner |
| **配置方式** | Jenkinsfile (Groovy) | YAML (.github/workflows) | YAML (.gitlab-ci.yml) |
| **插件生态** | 1500+ 插件，最丰富 | 社区Action市场 | 少 |
| **平台绑定** | 无（任意Git平台） | GitHub专属 | GitLab专属 |
| **可视化管理** | Web UI + Pipeline Blue Ocean | GitHub页面集成 | GitLab页面集成 |
| **维护成本** | 高（需维护Jenkins节点） | 低（托管） | 中 |
| **企业级功能** | Pipeline共享库、审批、权限 | Environment/Environments | 环境管理、审批 |
| **并发构建** | 需配置节点 | 并行Job | 并行Job |
| **选择建议** | 企业自建/遗留系统 | GitHub项目/小团队 | GitLab全栈用户 |

---

### 3.3 Jenkins企业级Pipeline示例

```groovy
// Jenkinsfile — 声明式Pipeline

pipeline {
    agent any

    // ============ 环境变量 ============
    environment {
        APP_NAME = 'myapp'
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
        HARBOR_URL = 'harbor.example.com'
        DOCKER_CREDS = credentials('harbor-credentials')
    }

    // ============ 参数 ============
    parameters {
        choice(
            name: 'ENV',
            choices: ['dev', 'staging', 'production'],
            description: '部署环境'
        )
    }

    // ============ 触发条件 ============
    triggers {
        // 每天凌晨2点构建
        cron('0 2 * * *')
        // 有merge到main时触发
        upstream('myapp-build', 'SUCCESS')
    }

    // ============ 阶段 ============
    stages {
        stage('代码检出') {
            steps {
                checkout scm
            }
        }

        stage('代码质量扫描') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage('单元测试') {
            steps {
                sh 'mvn test -DskipITs'
            }
            post {
                success {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco classPattern: '**/target/classes'
                }
            }
        }

        stage('构建打包') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('构建Docker镜像') {
            steps {
                script {
                    docker.build("${HARBOR_URL}/${APP_NAME}:${IMAGE_TAG}")
                }
            }
        }

        stage('推送镜像到Harbor') {
            steps {
                script {
                    docker.withRegistry("https://${HARBOR_URL}", 'harbor-credentials') {
                        docker.image("${HARBOR_URL}/${APP_NAME}:${IMAGE_TAG}").push()
                        docker.image("${HARBOR_URL}/${APP_NAME}:${IMAGE_TAG}").push('latest')
                    }
                }
            }
        }

        stage('部署到K8s') {
            when {
                expression { params.ENV == 'production' }
            }
            steps {
                script {
                    sh """
                        sed -i 's|IMAGE_PLACEHOLDER|${HARBOR_URL}/${APP_NAME}:${IMAGE_TAG}|g' k8s/deployment.yaml
                        kubectl apply -f k8s/ --namespace=${params.ENV}
                        kubectl rollout status deployment/${APP_NAME} -n ${params.ENV}
                    """
                }
            }
        }
    }

    // ============ 后处理 ============
    post {
        success {
            // 钉钉通知
            dingTalk(
                robot: 'devops-robot',
                type: 'MARKDOWN',
                title: "${APP_NAME} 构建成功",
                text: "✅ 构建成功\n- 环境: ${params.ENV}\n- 镜像: ${IMAGE_TAG}\n- 触发者: ${env.BUILD_USER}"
            )
        }
        failure {
            // 失败告警
            dingTalk(
                robot: 'devops-robot',
                type: 'MARKDOWN',
                title: "${APP_NAME} 构建失败",
                text: "❌ 构建失败\n- 阶段: ${env.STAGE_NAME}\n- 查看: ${env.BUILD_URL}"
            )
        }
    }
}
```

---

### 3.4 GitHub Actions示例

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  APP_NAME: myapp
  HARBOR_URL: harbor.example.com

jobs:
  # ============ 构建与测试 ============
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: 代码质量扫描
        run: mvn sonar:sonar
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

      - name: 单元测试
        run: mvn test

      - name: 构建打包
        run: mvn clean package -DskipTests

      - name: 上传构建产物
        uses: actions/upload-artifact@v4
        with:
          name: app-jar
          path: target/*.jar

  # ============ Docker镜像构建与推送 ============
  docker:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4

      - name: 登录Harbor
        uses: docker/login-action@v3
        with:
          registry: ${{ env.HARBOR_URL }}
          username: ${{ secrets.HARBOR_USERNAME }}
          password: ${{ secrets.HARBOR_PASSWORD }}

      - name: 构建并推送镜像
        uses: docker/build-push-action@v5
        with:
          push: true
          tags: |
            ${{ env.HARBOR_URL }}/${{ env.APP_NAME }}:${{ github.sha }}
            ${{ env.HARBOR_URL }}/${{ env.APP_NAME }}:latest

  # ============ 部署 ============
  deploy:
    needs: docker
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4

      - name: 配置Kubeconfig
        run: |
          mkdir -p $HOME/.kube
          echo "${{ secrets.KUBE_CONFIG }}" > $HOME/.kube/config

      - name: 部署到K8s
        run: |
          sed -i "s|IMAGE_PLACEHOLDER|${{ env.HARBOR_URL }}/${{ env.APP_NAME }}:${{ github.sha }}|g" k8s/deployment.yaml
          kubectl apply -f k8s/ --namespace=production
          kubectl rollout status deployment/${{ env.APP_NAME }} -n production
```

---

### 3.5 自动化测试集成

```xml
<!-- pom.xml — 集成测试配置 -->
<build>
    <plugins>
        <!-- JaCoCo 覆盖率 -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals><goal>prepare-agent</goal></goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>verify</phase>
                    <goals><goal>report</goal></goals>
                </execution>
                <execution>
                    <id>check</id>
                    <phase>verify</phase>
                    <goals><goal>check</goal></goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>BUNDLE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVERED_RATIO</value>
                                        <minimum>0.80</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- Testcontainers 集成测试 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <executions>
                <execution>
                    <goals><goal>integration-test</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

```java
// Testcontainers集成测试示例
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private OrderService orderService;

    @Test
    void testCreateOrder() {
        Order order = orderService.createOrder(new CreateOrderRequest(/*...*/));
        assertNotNull(order.getId());
    }
}
```

---

### 3.6 制品管理（Nexus/Harbor/阿里云ACR）

| 制品类型 | 工具 | 存储内容 | 角色 |
|---------|------|---------|------|
| **JAR包/Maven依赖** | Nexus / Artifactory | 构建产物、第三方依赖 | 构建中间件 |
| **Docker镜像** | Harbor / 阿里云ACR / AWS ECR | 容器镜像 | 镜像仓库 |
| **Helm Chart** | Harbor / ChartMuseum | K8s打包配置 | 云原生部署 |

```yaml
# Maven配置Nexus镜像源
# settings.xml
<mirrors>
  <mirror>
    <id>nexus</id>
    <mirrorOf>*</mirrorOf>
    <url>https://nexus.example.com/repository/maven-public/</url>
  </mirror>
</mirrors>

# Docker客户端配置Harbor
# /etc/docker/daemon.json
{
  "insecure-registries": ["harbor.example.com"],
  "registry-mirrors": ["https://harbor.example.com"]
}
```

> 💡 **企业实践**：Harbor支持镜像复制（跨机房同步）、漏洞扫描（Trivy/Clair）、RBAC权限控制。是企业级镜像仓库的首选。

---

## 4. 代码质量与规范

> 代码质量是团队工程文化的体现。面试中提问"你参与过Code Review吗"考察的是工程素养。

### 4.1 SonarQube代码静态扫描

```bash
# SonarQube核心指标
# - Bugs: 代码缺陷（空指针、资源未关闭等）
# - Vulnerabilities: 安全漏洞（SQL注入、XSS等）
# - Code Smells: 代码坏味道（过长方法、重复代码等）
# - Coverage: 测试覆盖率
# - Duplications: 重复代码比例

# 扫描命令
mvn sonar:sonar \
  -Dsonar.host.url=http://sonarqube.example.com \
  -Dsonar.login=${SONAR_TOKEN} \
  -Dsonar.exclusions=**/test/**
```

**质量阈值（Quality Gate）**：

| 指标 | 阈值 | 含义 |
|------|------|------|
| 覆盖率 | >= 80% | 核心业务代码 |
| 重复率 | < 3% | 避免复制粘贴 |
| 安全评级 | A | 无高危漏洞 |
| 可维护性 | A | 代码坏味道可控 |

### 4.2 Alibaba Java Coding Guidelines

> 阿里Java开发手册是Java后端编码规范的事实标准。面试中常问其中的核心条款。

| 类别 | 规约要点 | 示例 |
|------|---------|------|
| **命名** | 类名UpperCamelCase；方法名lowerCamelCase | `OrderService` / `findById` |
| **常量** | 魔法值必须定义常量类 | `private static final int MAX_RETRY = 3;` |
| **集合** | 指定初始容量 | `new HashMap<>(initialCapacity)` |
| **并发** | 线程池不允许使用Executors创建 | 必须通过ThreadPoolExecutor |
| **SQL** | WHERE条件禁止函数操作索引列 | 见MySQL章节 |
| **异常** | 不要用e.printStackTrace() | 用Logger打印 |

```java
// 禁用Executors创建线程池（阿里规约强制）
// 错误：线程池可能OOM
ExecutorService executor = Executors.newFixedThreadPool(10);

// 正确：显式指定参数
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10,                     // corePoolSize
    20,                     // maximumPoolSize
    60L, TimeUnit.SECONDS,  // keepAliveTime
    new LinkedBlockingQueue<>(2000),  // workQueue（有界队列！）
    new ThreadFactoryBuilder()
        .setNameFormat("order-pool-%d")
        .build(),
    new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);
```

### 4.3 Checkstyle / SpotBugs

```xml
<!-- pom.xml — Checkstyle配置 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <failOnViolation>true</failOnViolation>
    </configuration>
</plugin>

<!-- pom.xml — SpotBugs配置 -->
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <configuration>
        <effort>Max</effort>
        <threshold>Low</threshold>
        <failOnError>true</failOnError>
    </configuration>
</plugin>
```

### 4.4 Git提交规范（Conventional Commits）

```
# 提交格式
<type>(<scope>): <description>

# 类型
feat:      新功能
fix:       Bug修复
docs:      文档变更
style:     代码格式（不影响功能）
refactor:  重构（既不是新增也不是修复）
test:      添加测试
chore:     构建/CI变更
perf:      性能优化

# 示例
feat(order): 新增订单取消功能
fix(payment): 修复微信支付回调验签失败
docs: 更新API接口文档
refactor: 提取公共缓存逻辑到CacheService
```

```bash
# 提交信息示例
git commit -m "feat(order): 新增订单取消功能

- 实现订单取消接口
- 支持退款流程触发
- 新增取消原因枚举

Closes #123"
```

### 4.5 Code Review流程与工具

| 环节 | 工具 | 检查要点 |
|------|------|---------|
| **GitLab MR** | GitLab Merge Request | 代码逻辑、命名规范、测试覆盖 |
| **GitHub PR** | GitHub Pull Request | 同上 + CI状态检查 |
| **Gerrit** | Code Review专门系统 | 逐行审阅、打分机制 |

**Code Review Checklist**：

```markdown
## Code Review 检查清单

### 功能逻辑
- [ ] 是否理解这段代码的业务意图？
- [ ] 边界条件（null、空集合、负数）是否处理？
- [ ] 是否存在并发安全问题？

### 代码质量
- [ ] 命名是否清晰表达意图？
- [ ] 方法是否过长（< 50行）？
- [ ] 是否存在重复代码？
- [ ] 异常处理是否合理？

### 安全
- [ ] SQL注入风险？
- [ ] XSS/CSRF防护？
- [ ] 敏感信息是否泄露（密码/Token）？

### 测试
- [ ] 单元测试是否覆盖核心逻辑？
- [ ] 边界用例是否测试？
```

> 🎯 **核心要点**：Code Review不是"找茬"，而是知识共享和团队成长的机制。好的Review习惯是Java高级工程师的标志之一。

---

## 5. 日志与监控集成

> "你的系统出问题了怎么排查？"——日志和监控能力决定了线上问题的响应速度。

### 5.1 ELK Stack日志收集

```
                    Logstash (数据管道)
                    ┌──────────────────────────┐
                    │ Input → Filter → Output   │
                    │  - File Input             │
                    │  - Grok解析日志格式        │
                    │  - Output→ES              │
                    └──────────┬───────────────┘
                               │
  App(1) ──log文件/API──┐     │
  App(2) ──log文件/API──┤     │     ┌──────────────┐     ┌──────────┐
  App(3) ──log文件/API──┼─────┼────→│ Elasticsearch│ ←── │ Kibana   │
                        │     │     │  (存储+索引)   │     │ (可视化)  │
  App(4) ──Filebeat ───┘     │     └──────────────┘     └──────────┘
                               │
                        Filebeat (轻量采集器)
                        无需Logstash时直接输入ES
```

**Logback配置（JSON格式日志，方便Logstash解析）**：

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- 应用名称 -->
    <springProperty scope="context" name="appName" source="spring.application.name"/>

    <!-- JSON格式Appender -->
    <appender name="JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/app/logs/${appName}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/app/logs/${appName}.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- 添加自定义字段 -->
            <includeMdc>true</includeMdc>
            <customFields>{"app":"${appName}","env":"${ACTIVE_PROFILE}"}</customFields>
        </encoder>
    </appender>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- MDC 添加TraceId -->
    <appender name="JSON" / 配置同上>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

**Filebeat配置**：

```yaml
# filebeat.yml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /app/logs/*.log
    multiline:
      pattern: '^\d{4}-\d{2}-\d{2}'
      negate: true
      match: after

output.elasticsearch:
  hosts: ["http://elasticsearch:9200"]
  index: "app-logs-%{+yyyy.MM.dd}"

# 或输出到Logstash
# output.logstash:
#   hosts: ["logstash:5044"]
```

---

### 5.2 微服务链路追踪

| 工具 | 语言 | 协议 | 侵入性 | 存储 | 界面丰富度 |
|------|------|------|--------|------|-----------|
| **SkyWalking** | Java/Go/.NET | gRPC | 低（Java Agent） | H2/ES | ⭐⭐⭐⭐⭐ |
| **Pinpoint** | Java/PHP | Thrift | 低（Java Agent） | HBase | ⭐⭐⭐⭐ |
| **Jaeger** | 多语言 | OpenTelemetry | 中（SDK） | ES/Badger | ⭐⭐⭐ |
| **Zipkin** | 多语言 | Brave/OpenTelemetry | 中（SDK） | ES/Cassandra | ⭐⭐⭐ |

> 💡 **企业推荐**：Java技术栈首选 **SkyWalking**（无侵入Agent，自动增强Spring Cloud/dubbo/gRPC等框架）。

```yaml
# SkyWalking Agent配置
# skywalking-agent.jar 配置
agent.service_name=myapp
collector.backend_service=skywalking-oap:11800

# JVM参数启动
-javaagent:/path/to/skywalking-agent.jar
-Dskywalking.agent.service_name=myapp
-Dskywalking.collector.backend_service=skywalking-oap:11800
```

**MDC + TraceId 日志关联**：

```java
// 通过SkyWalking Agent自动注入TraceId到MDC
// 日志格式中加入 %X{tid} 即可在日志中看到TraceId

@Component
public class LogPatternConfig {
    @PostConstruct
    public void init() {
        // Spring Cloud Sleuth / Micrometer Tracing 自动添加TraceId
        // 或在logback配置中使用 %X{traceId} / %X{X-B3-TraceId}
    }
}
```

---

### 5.3 Prometheus + Grafana 监控

```yaml
# Prometheus配置（prometheus.yml）
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-apps'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'myapp-1:8080'
          - 'myapp-2:8080'
          - 'myapp-3:8080'
        labels:
          app: myapp
          env: production
```

**Spring Boot Actuator + Micrometer**：

```yaml
# application.yml — 暴露Prometheus指标
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
  endpoint:
    prometheus:
      enabled: true
```

```java
// 自定义业务指标
@Component
public class OrderMetrics {
    private final Counter orderCreateCounter;
    private final Timer orderProcessTimer;
    private final Gauge orderQueueGauge;

    public OrderMetrics(MeterRegistry registry) {
        orderCreateCounter = Counter.builder("order.create.total")
            .description("订单创建总数")
            .register(registry);

        orderProcessTimer = Timer.builder("order.process.duration")
            .description("订单处理耗时")
            .publishPercentileHistogram()
            .register(registry);

        orderQueueGauge = Gauge.builder("order.queue.size", this,
                OrderMetrics::getQueueSize)
            .description("订单队列大小")
            .register(registry);
    }

    public void recordOrderCreate() {
        orderCreateCounter.increment();
    }

    public void recordProcessDuration(Runnable task) {
        orderProcessTimer.record(task);
    }

    private int getQueueSize() {
        return pendingQueue.size();
    }
}
```

```yaml
# Grafana告警规则示例
# 告警渠道: 钉钉 / 企业微信 / PagerDuty

# 常见告警规则:
# - 应用QPS下降 > 50% → 可能宕机
# - 接口P99延迟 > 2000ms → 性能瓶颈
# - JVM堆内存使用率 > 85% → 内存泄漏风险
# - CPU使用率 > 90%持续5分钟 → 需要扩容
# - Error日志率 > 1% → 代码异常
# - OOM Killer → 容器内存不足
```

---

## 6. 基础设施即代码 (IaC)

> IaC将基础设施管理从"手动点击控制台"转变为"代码声明"，是云原生时代的必选项。

### 6.1 Terraform / Ansible 基础概念

| 对比维度 | Terraform | Ansible |
|---------|-----------|---------|
| **模式** | 声明式（Declarative） | 命令式（Procedural） |
| **状态管理** | 状态文件（terraform.tfstate） | 无状态 |
| **资源范围** | 基础设施（云资源/K8s） | 配置管理（软件/包/服务） |
| **配置语言** | HCL | YAML |
| **幂等性** | ✅ 天然 | ✅ 通过模块设计 |
| **适用场景** | 创建/销毁云资源 | 服务器配置和编排 |

```hcl
# Terraform示例 — 创建阿里云ECS + RDS
terraform {
  required_providers {
    alicloud = {
      source = "aliyun/alicloud"
      version = "~> 1.200"
    }
  }
}

provider "alicloud" {
  region = "cn-hangzhou"
}

# 创建VPC
resource "alicloud_vpc" "main" {
  vpc_name   = "production-vpc"
  cidr_block = "10.0.0.0/8"
}

# 创建ECS实例
resource "alicloud_instance" "app" {
  instance_name        = "myapp-server"
  instance_type        = "ecs.g7.large"
  image_id             = "ubuntu_22_04_x64_20G_alibase_20230612.vhd"
  vswitch_id           = alicloud_vswitch.main.id
  security_groups      = [alicloud_security_group.main.id]
  internet_max_bandwidth_out = 100

  user_data = <<-EOF
    #!/bin/bash
    yum install -y docker
    systemctl enable docker
    systemctl start docker
  EOF
}

# 创建RDS MySQL
resource "alicloud_db_instance" "mysql" {
  engine           = "MySQL"
  engine_version   = "8.0"
  instance_class   = "mysql.n2.large.2c"
  instance_storage = 100
  vswitch_id       = alicloud_vswitch.main.id
  security_ips     = [alicloud_instance.app.public_ip]
}
```

```yaml
# Ansible示例 — 部署Java应用
---
- name: 部署Java应用
  hosts: app_servers
  become: yes
  vars:
    app_name: myapp
    app_version: "1.0.0"
    java_home: "/usr/lib/jvm/java-17-openjdk"

  tasks:
    - name: 安装JDK
      apt:
        name: openjdk-17-jdk
        state: present

    - name: 创建应用目录
      file:
        path: "/opt/{{ app_name }}"
        state: directory
        mode: '0755'

    - name: 复制应用JAR
      copy:
        src: "/tmp/{{ app_name }}-{{ app_version }}.jar"
        dest: "/opt/{{ app_name }}/app.jar"
        mode: '0644'

    - name: 配置systemd服务
      template:
        src: app.service.j2
        dest: "/etc/systemd/system/{{ app_name }}.service"
      notify: restart app

    - name: 启动应用
      systemd:
        name: "{{ app_name }}"
        enabled: yes
        state: started

  handlers:
    - name: restart app
      systemd:
        name: "{{ app_name }}"
        state: restarted
```

### 6.2 为什么大厂都在用IaC

| 痛点 | IaC解决方案 |
|------|------------|
| **手动操作易出错** | 代码声明，自动化执行，杜绝人为失误 |
| **环境不一致** | Git版本管理，开发/测试/生产环境完全一致 |
| **扩缩容慢** | 一条命令创建100台服务器 |
| **灾难恢复** | 整个基础设施可"一键重建" |
| **合规审计** | 所有变更记录在Git中，可追溯、可回滚 |
| **知识沉淀** | 基础设施知识从"在某个运维工单里"变为"在代码仓库里" |

> 💡 **IaC黄金法则**：永远不要手动修改生产环境。任何修改都应该通过代码变更 → Code Review → Pipeline 自动化应用。

---

> 🎯 **核心要点**：DevOps与云原生工具链是Java后端工程师从"会写代码"到"会交付"的进阶之路。掌握Docker容器化、K8s编排、CI/CD流水线、日志监控是面试的硬通货。IaC则是拉开"大厂工程师"和"传统开发"距离的关键能力。

---

**下一模块**：[04 监控告警与可观测性](./04-监控告警与可观测性.md) | **返回总览**：[总览](./00-企业技术栈全景总览.md)
