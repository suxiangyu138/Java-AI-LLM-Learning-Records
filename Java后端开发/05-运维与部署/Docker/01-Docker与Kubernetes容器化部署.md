# Docker与Kubernetes容器化部署深度指南

> 本文面向Java/后端开发者，从容器化基础到Kubernetes生产实践，涵盖Docker核心概念、Dockerfile最佳实践、Docker Compose本地编排、K8s集群架构与实战，以及CI/CD流水线集成。全文以Spring Boot应用为主线，贯穿完整示例。

---

## 目录

1. [容器化基础与Docker核心概念](#1-容器化基础与docker核心概念)
2. [Dockerfile最佳实践](#2-dockerfile最佳实践)
3. [Docker Compose多容器编排](#3-docker-compose多容器编排)
4. [Kubernetes核心概念](#4-kubernetes核心概念)
5. [Kubernetes实战：部署Spring Boot应用](#5-kubernetes实战部署spring-boot应用)
6. [CI/CD与容器化发布策略](#6-cicd与容器化发布策略)
7. [总结与进阶路线](#7-总结与进阶路线)

---

## 1. 容器化基础与Docker核心概念

### 1.1 容器 vs 虚拟机

理解容器的最佳方式是将其与虚拟机（VM）进行对比。这两者虽然都用于资源隔离和应用打包，但底层原理截然不同。

| 对比维度 | 容器 (Container) | 虚拟机 (VM) |
|---------|----------------|-------------|
| 内核 | 共享宿主机内核 | 每个VM包含独立Guest OS |
| 启动速度 | 秒级（毫秒~秒） | 分钟级 |
| 镜像大小 | MB级（Alpine仅5MB） | GB级（含完整OS） |
| 资源占用 | 轻量，仅进程级隔离 | 重量级，Hypervisor开销 |
| 隔离性 | 较弱（共享内核，Namespace+CGroup隔离） | 强（完全虚拟化硬件） |
| 密度 | 单机可运行数百容器 | 单机通常数十个VM |

**核心差异在于内核共享**：容器本质上是一组被Namespace隔离、被CGroup限制资源、运行在宿主机内核上的进程。这意味着：

- Linux容器无法在Windows宿主机上直接运行（除非通过WSL2或Hyper-V虚拟化Linux内核）。
- 容器内执行 `uname -r` 看到的是宿主机内核版本。
- 由于没有独立内核，容器逃逸攻击的后果比VM更严重。

对于Java应用而言，容器带来的好处尤为明显：**环境一致性**（开发环境=测试环境=生产环境）、**快速弹性伸缩**、**高效资源利用**（一台4C8G的服务器可以轻松运行10+个Java微服务实例）。

### 1.2 Docker三要素：镜像、容器、仓库

Docker生态围绕三个核心概念构建：

#### 镜像（Image）

镜像是容器的只读模板，类似于面向对象编程中的"类"。它包含运行应用所需的一切——代码、运行时、系统工具、库和配置。镜像是分层构建的（后文详述），可以从Dockerfile构建，也可以从仓库下载。

```bash
# 本地镜像管理
docker images                    # 列出所有镜像
docker image ls                 # 同上
docker rmi nginx:latest         # 删除镜像
docker image prune              # 清理悬空镜像（dangling images）
```

#### 容器（Container）

容器是镜像的运行实例，类似"对象"。它是拥有独立文件系统、网络栈和进程空间的隔离环境。同一镜像可以启动多个容器，彼此互不影响。

```bash
# 从nginx镜像创建并启动容器
docker run -d --name my-nginx -p 8080:80 nginx:alpine
```

#### 仓库（Registry）

仓库用于存储和分发镜像。官方仓库是Docker Hub，但国内访问较慢，生产环境常用：

- **阿里云容器镜像服务（ACR）**：国内速度快，支持私有仓库
- **Harbor**：企业级私有镜像仓库，支持RBAC、漏洞扫描
- **AWS ECR / Azure CR / Google GCR**：云厂商托管服务

```bash
# 登录阿里云镜像仓库
docker login --username=你的阿里云账号 registry.cn-hangzhou.aliyuncs.com

# 标记并推送镜像
docker tag my-app:v1 registry.cn-hangzhou.aliyuncs.com/namespace/my-app:v1
docker push registry.cn-hangzhou.aliyuncs.com/namespace/my-app:v1

# 拉取镜像
docker pull registry.cn-hangzhou.aliyuncs.com/namespace/my-app:v1
```

### 1.3 镜像分层与Union FS

Docker镜像的核心设计思想是**分层（Layer）**。每个Dockerfile指令（FROM、RUN、COPY等）都会创建一个新层，这些层以只读方式堆叠，最终构成完整的文件系统。

**分层存储的技术基础**：

- **Union File System（联合文件系统）**：Docker默认使用Overlay2驱动。它将多个目录（层）挂载到同一个挂载点，呈现为一个统一文件系统。
- **写时复制（Copy-on-Write, CoW）**：当容器需要修改某个只读层的文件时，不会直接修改原层，而是将该文件复制到容器可写层再修改。这使得多个容器可以共享相同的底层镜像，大幅节省磁盘空间。

```
镜像层（只读）:
  Layer 6: CMD ["java", "-jar", "app.jar"]        ← Dockerfile最后指令
  Layer 5: COPY target/app.jar /app/app.jar        ← 应用JAR
  Layer 4: RUN apk add --no-cache openjdk11-jre   ← 安装JRE
  Layer 3: WORKDIR /app                            ← 工作目录
  Layer 2: FROM alpine:3.16                        ← 基础OS
  Layer 1: 基础层（来自alpine镜像的rootfs）         ← 最底层

容器可写层（读写）:
  运行时修改的文件会通过CoW机制复制到这里
```

这种分层结构带来的好处：

1. **空间共享**：如果10个容器都基于 `alpine:3.16`，alpine的基础层在磁盘上只有一份拷贝。
2. **构建缓存**：构建时只要某层及其上层的指令未变化，就可以复用缓存层，大幅加速构建。
3. **拉取加速**：拉取镜像时只下载本地没有的层，增量更新。

### 1.4 Docker常用命令速查

命令是基本功，这里按使用场景分类整理：

#### 镜像管理

```bash
docker pull nginx:alpine          # 拉取镜像
docker push my-app:v1             # 推送镜像
docker build -t my-app:v1 .       # 构建镜像，-t指定标签
docker build --no-cache -t my-app:v1 .   # 禁用缓存构建
docker image ls                   # 列出镜像
docker rmi nginx:alpine           # 删除镜像
docker image prune                # 清理未使用的镜像
docker tag src dst                # 给镜像打标签
```

#### 容器生命周期

```bash
docker run -d \                    # -d 后台运行（detach）
  -p 8080:8080 \                   # -p 宿主机端口:容器端口
  -v /host/data:/app/data \        # -v 挂载卷
  -e SPRING_PROFILES_ACTIVE=prod \ # -e 环境变量
  --name my-app \                  # --name 容器名
  --restart unless-stopped \       # --restart 重启策略
  -m 512m \                        # -m 内存限制
  --cpus 1.5 \                     # --cpus CPU核数限制
  my-app:v1

docker ps                          # 运行中的容器
docker ps -a                       # 所有容器（含已停止）
docker stop my-app                 # 停止
docker start my-app                # 启动已停止的
docker restart my-app              # 重启
docker rm my-app                   # 删除容器
docker rm -f my-app                # 强制删除运行中的容器
```

#### 容器交互

```bash
docker logs my-app                 # 查看日志
docker logs -f my-app              # 实时跟踪日志（tail -f效果）
docker logs --tail 100 my-app      # 只显示最后100行
docker exec -it my-app /bin/bash   # 进入容器内部（交互式）
docker exec my-app ls /app         # 在容器内执行命令（非交互）
docker cp /host/file my-app:/app   # 从宿主机复制文件到容器
docker cp my-app:/app/log.log .    # 从容器复制文件到宿主机
docker inspect my-app              # 查看容器详细信息（JSON）
```

#### 资源清理

```bash
docker system df                   # 查看Docker磁盘使用情况
docker system prune                # 清理所有未使用的资源
docker system prune -a --volumes   # 彻底清理（包含镜像和卷）
```

---

## 2. Dockerfile最佳实践

### 2.1 基础指令详解

#### FROM — 选择基础镜像

基础镜像的选择直接影响构建速度、安全性和最终镜像大小：

```dockerfile
# 推荐：使用官方Alpine版本，体积极小
FROM openjdk:17-jdk-alpine

# 也可选Slim变体（基于Debian，比Alpine大但兼容性更好）
FROM openjdk:17-jdk-slim

# 不推荐：完整版包含大量无用工具
# FROM openjdk:17
```

**Alpine镜像**基于musl libc而非glibc，体积只有5MB左右。对于某些依赖glibc特定行为的Java应用（尤其是涉及JNI/Native库的），应选择 `slim` 版本。

#### WORKDIR — 设置工作目录

```dockerfile
WORKDIR /app
```
等价于连续执行 `mkdir -p /app && cd /app`。后续的RUN、CMD、ENTRYPOINT、COPY、ADD指令都会在此目录下执行。

#### RUN — 执行构建命令

```dockerfile
# 最佳实践：合并命令减少层数，&& 确保全部成功
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Alpine版本
RUN apk add --no-cache curl bash
```

`--no-install-recommends`（apt）和 `--no-cache`（apk）可以防止安装不必要的推荐包，减小镜像体积。最后的 `rm -rf /var/lib/apt/lists/*` 清理包管理器缓存——这一步必须与 `apt-get update` 在同一RUN指令中，否则缓存清理会单独成层且无法被移除（因为上层保留着包含缓存的层）。

#### COPY vs ADD

```dockerfile
# 推荐：COPY，语义清晰
COPY target/my-app.jar app.jar

# 不推荐：ADD有隐含行为，容易造成意外
ADD target/my-app.tar.gz /app/    # 自动解压tar
ADD https://example.com/file /app/ # 支持URL下载（不推荐用于生产）
```

**规则**：复制本地文件用 `COPY`；只有需要自动解压tar包时才用 `ADD`（且解压行为会破坏构建缓存）。绝不使用ADD下载远程文件——这应该在RUN中用 `curl` 或 `wget` 完成，以便利用缓存和错误处理。

#### ENV vs ARG

- **ENV**：设置环境变量，构建时和运行时都生效（容器启动后仍可访问）。
- **ARG**：仅构建时参数，容器运行时不保留（除非用ENV引用）。

```dockerfile
ARG APP_VERSION=1.0.0              # 构建参数：docker build --build-arg APP_VERSION=2.0.0
ENV SPRING_PROFILES_ACTIVE=prod    # 运行时环境变量
ENV APP_VERSION=$APP_VERSION       # 将ARG的值持久化为ENV
```

#### EXPOSE

```dockerfile
EXPOSE 8080
```
仅声明容器监听的端口，**不会自动映射端口**。它是一个文档作用，运行时仍需 `-p 8080:8080` 才能真正映射。但在Kubernetes/Docker Compose编排中，EXPOSE的信息会被自动发现。

#### CMD vs ENTRYPOINT

这是最容易混淆的一组指令。核心区别：

```dockerfile
# ENTRYPOINT：定义容器启动时的可执行文件（难以被覆盖）
ENTRYPOINT ["java", "-jar", "app.jar"]

# CMD：为ENTRYPOINT提供默认参数（可以被docker run命令覆盖）
CMD ["--spring.profiles.active=prod"]

# 常见用法组合
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=dev"]
```

用Shell形式（不带方括号）会通过 `/bin/sh -c` 执行，PID为1的不是应用进程，导致信号无法正确传递（`docker stop` 可能无法优雅关闭Java进程）。**始终使用Exec形式（JSON数组语法）**。

```dockerfile
# 正确：Exec形式，PID 1 是Java进程
ENTRYPOINT ["java", "-jar", "app.jar"]

# 错误：Shell形式，PID 1 是sh，Java成为子进程
ENTRYPOINT java -jar app.jar
```

### 2.2 多阶段构建（Multi-Stage Build）

多阶段构建是减小最终镜像体积的最有效手段。核心思想：**第一阶段用完整环境编译，第二阶段只复制编译产物到精简镜像**。

典型场景：Spring Boot应用的Maven构建。

```dockerfile
# === 第一阶段：编译阶段 ===
FROM maven:3.8.6-eclipse-temurin-17 AS builder
WORKDIR /build

# 先复制pom.xml，利用缓存安装依赖（只要pom不变化就命中缓存）
COPY pom.xml .
RUN mvn dependency:resolve -pl . -q

# 复制源码并编译
COPY src ./src
RUN mvn package -DskipTests -q

# === 第二阶段：运行阶段 ===
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 创建一个非root用户（安全最佳实践）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 只从builder阶段复制jar文件
COPY --from=builder /build/target/*.jar app.jar

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

最终镜像只包含JRE + JAR文件，没有JDK、Maven、源代码等编译时依赖。对比：

- 单阶段构建（`FROM maven + COPY + RUN mvn package`）：约 500~700MB
- 多阶段构建（`FROM jre-alpine`）：约 120~180MB

### 2.3 构建缓存优化技巧

Docker构建时逐层执行，如果某层指令和上下文都没有变化，就使用缓存。合理利用缓存可以显著加速构建：

```dockerfile
# 不推荐：每次修改源码都会重新安装依赖
COPY . .
RUN mvn package

# 推荐：分离pom和源码，先装依赖
COPY pom.xml .
RUN mvn dependency:resolve           # pom不变则命中缓存
COPY src ./src
RUN mvn package                      # 只编译改动的代码
```

**构建缓存失效规则**：
- 某层指令发生变化（包括命令文本和上下文文件变化），该层及后续层全部失效。
- 使用 `--no-cache` 强制全部重跑。
- 频繁变化的指令（如 `COPY src`）放在Dockerfile靠后的位置。

### 2.4 .dockerignore

类似 `.gitignore`，在构建时排除不需要发送到Docker守护进程的文件，减少构建上下文大小，提升构建速度：

```dockerignore
.git/
.gitignore
target/
*.md
.DS_Store
docker-compose*.yml
.idea/
*.iml
node_modules/
```

注意：`docker build` 会将当前目录（构建上下文）整个发送给Docker守护进程。如果忘了忽略 `target/`，几百MB的编译输出会被传到守护进程，即使最终不会打包进镜像，构建第一步也慢得令人崩溃。

### 2.5 生产环境安全实践

```dockerfile
# 1. 使用非root用户运行
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# 2. 不要以root运行Java进程——如果被攻破，攻击者有完全权限

# 3. 使用只读根文件系统（与K8s securityContext配合）
# docker run --read-only --tmpfs /tmp ...

# 4. 设置明确的资源限制（非必须，建议在编排层控制）

# 5. 移除setuid/setbit权限
RUN find / -perm /6000 -type f -exec chmod a-s {} \; || true

# 6. 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
```

---

## 3. Docker Compose多容器编排

Docker Compose用于在**单机**上定义和运行多容器应用。它通过YAML文件描述服务的依赖关系、网络、存储卷等，一条命令即可启动整个应用栈。

### 3.1 docker-compose.yml 核心配置

以典型Spring Boot项目为例：应用 + MySQL + Redis。

```yaml
version: '3.8'

services:
  # === Spring Boot 应用 ===
  app:
    build:
      context: .
      dockerfile: Dockerfile
    image: my-app:latest
    container_name: spring-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/myapp?useSSL=false&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root123
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      TZ: Asia/Shanghai
    volumes:
      - app-logs:/app/logs
    depends_on:
      - db
      - redis
    networks:
      - backend
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 40s
    mem_limit: 512m
    cpus: 1.0

  # === MySQL 数据库 ===
  db:
    image: mysql:8.0
    container_name: mysql-db
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: myapp
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql  # 初始化脚本
    networks:
      - backend
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 3s
      retries: 5

  # === Redis 缓存 ===
  redis:
    image: redis:7-alpine
    container_name: redis-cache
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - backend
    restart: unless-stopped
    command: redis-server --appendonly yes --requirepass redis123
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

volumes:
  mysql-data:
    driver: local
  redis-data:
    driver: local
  app-logs:
    driver: local

networks:
  backend:
    driver: bridge
```

**关键配置说明**：

| 配置项 | 说明 |
|--------|------|
| `build` | 指定从Dockerfile构建（context是构建上下文路径） |
| `image` | 指定使用的镜像（构建后自动打标签） |
| `ports` | `"宿主机端口:容器端口"` |
| `volumes` | 命名卷（`mysql-data:/var/lib/mysql`）或绑定挂载（`./sql/init.sql:...`） |
| `environment` | 环境变量，支持 `${VARIABLE}` 引用 `.env` 文件 |
| `depends_on` | 控制启动顺序（不等服务就绪，只确保容器启动） |
| `networks` | 自定义网络，服务间通过服务名通信（如`jdbc:mysql://db:3306`） |
| `restart` | 重启策略，同Docker `--restart` |
| `healthcheck` | 健康检查 |

### 3.2 服务间通信

在自定义网络 `backend` 中，容器可以通过**服务名**互相访问：

- 应用访问数据库：`jdbc:mysql://db:3306/myapp`
- 应用访问Redis：`spring.redis.host=redis`
- 数据库初始化脚本通过 `depends_on` 等待启动

**注意**：`depends_on` 只保证上游容器**已启动**，不保证其就绪。例如 `db` 容器启动了，但MySQL可能还在初始化。更好的方案：

```yaml
# 方案1：应用启动时加入等待脚本（wait-for-it.sh）
# 方案2：在应用代码中配置重试连接
# 方案3：使用healthcheck + depends_on的condition（Docker Compose 3.8+实验特性）
```

### 3.3 Compose常用命令

```bash
# 启动所有服务
docker compose up -d                     # -d：后台运行

# 重新构建并启动
docker compose up -d --build

# 查看日志
docker compose logs -f                  # 跟踪所有服务的日志
docker compose logs -f app             # 只跟踪app服务的日志

# 查看运行状态
docker compose ps

# 进入容器
docker compose exec app /bin/sh
docker compose exec db mysql -uroot -p

# 停止所有服务（不删除）
docker compose stop

# 停止并删除容器和网络
docker compose down

# 停止并删除容器、网络和数据卷（⚠ 会删除数据库数据）
docker compose down -v

# 查看配置
docker compose config

# 重启服务
docker compose restart app
```

### 3.4 开发环境与生产环境的差异

```yaml
# 开发环境：挂载源码实现热重载
services:
  app:
    build:
      context: .
      dockerfile: Dockerfile.dev    # 开发专用Dockerfile
    volumes:
      - ./src:/app/src              # 挂载源码实现即时修改
      - ~/.m2:/root/.m2             # 共享Maven本地仓库
    ports:
      - "5005:5005"                 # 开启远程调试端口
    environment:
      SPRING_PROFILES_ACTIVE: dev
      JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

生产环境不应挂载源码，不应开启调试端口，应使用固定版本镜像而非 `latest` 标签。

---

## 4. Kubernetes核心概念

Kubernetes（K8s）是容器编排的事实标准。它解决了Docker Compose无法覆盖的问题：**多机集群、自动扩缩、故障自愈、服务发现、滚动更新**。本节从架构开始，逐一剖析核心概念。

### 4.1 集群架构

一个K8s集群由**控制平面**（Master）和**工作节点**（Worker Node）组成：

```
┌─────────────────────────────────────────────────┐
│                  Master 节点                       │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │API Server│  │Scheduler │  │Controller Mgr │  │
│  └─────┬────┘  └──────────┘  └───────┬───────┘  │
│        │                             │          │
│  ┌─────▼──────────────────────────┐  │          │
│  │           etcd                 │  │          │
│  └────────────────────────────────┘  │          │
└──────────────────────────────────────┘──────────┘
                     │ API Server（HTTPS 6443）
                     ▼
┌─────────────────────────────────────────────────┐
│              Worker 节点 1                        │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ kubelet  │  │kube-proxy│  │   Container   │  │
│  │          │  │          │  │   Runtime     │  │
│  └─────┬────┘  └─────┬────┘  │ (containerd)  │  │
│        │             │       └───────────────┘  │
│  ┌─────▼─────────────▼──────────────────────┐   │
│  │          Pods (多个)                       │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

**Master组件**：

| 组件 | 职责 |
|------|------|
| **API Server** | 集群入口，所有操作（kubectl、仪表盘、其他组件）都通过API Server，提供REST API和认证鉴权 |
| **Scheduler** | 调度Pod到合适的工作节点（考虑资源、亲和性、污点等） |
| **Controller Manager** | 运行各种控制器（Deployment Controller、Node Controller等），确保实际状态收敛到期望状态 |
| **etcd** | 分布式键值存储，保存集群所有状态数据（唯一有状态组件），需做备份 |

**Worker组件**：

| 组件 | 职责 |
|------|------|
| **kubelet** | 节点代理，负责管理Pod和容器的生命周期，与API Server通信 |
| **kube-proxy** | 维护节点上的网络规则，实现Service的负载均衡 |
| **Container Runtime** | 容器运行时（containerd、CRI-O、Docker已弃用） |

### 4.2 Pod — 最小调度单元

Pod是Kubernetes中最小的部署和调度单元，它封装了一个或多个容器、共享存储和网络资源。

#### 单容器Pod vs 多容器Pod

```yaml
# 单容器Pod：最常见的模式，一个Pod只有一个应用容器
apiVersion: v1
kind: Pod
metadata:
  name: my-app-pod
  labels:
    app: my-app
spec:
  containers:
  - name: my-app
    image: my-app:v1
    ports:
    - containerPort: 8080
```

```yaml
# 多容器Pod：Sidecar模式，主容器 + 辅助容器共享Pod
apiVersion: v1
kind: Pod
metadata:
  name: web-app-with-sidecar
spec:
  containers:
  - name: web-app                    # 主容器
    image: my-app:v1
    ports:
    - containerPort: 8080
    volumeMounts:
    - name: logs
      mountPath: /app/logs
  - name: log-collector              # Sidecar：收集日志到中心化服务
    image: fluent-bit:latest
    volumeMounts:
    - name: logs
      mountPath: /var/log/app
    env:
    - name: FLUENT_ELASTICSEARCH_HOST
      value: "elasticsearch.logging"
  volumes:
  - name: logs
    emptyDir: {}                     # 临时存储，Pod销毁时清除
```

**Pod关键特性**：

- **共享网络**：Pod内所有容器共享同一个网络命名空间（IP和端口空间），可以通过localhost互相访问。
- **共享存储**：Pod内的容器可以挂载相同的Volume。
- **临时性**：Pod是最小调度单元，IP会随Pod重建而变化——这就是为什么需要Service来做稳定访问入口。
- **原子调度**：Pod内的所有容器保证被调度到同一节点。

#### Sidecar模式

Sidecar是K8s中重要的设计模式：在Pod中额外启动一个辅助容器，增强或扩展主容器的功能，而不修改主容器的代码。常见场景：

- 日志收集（Filebeat/Fluentd + 应用）
- 服务网格代理（Istio Envoy + 应用）
- 配置文件热更新（ConfigMap Reloader）
- 反向代理（Nginx + Web应用）

### 4.3 Deployment — 声明式管理Pod

Deployment是K8s中最常用的工作负载资源，它提供了**声明式更新**能力——你描述"想要什么状态"，Deployment Controller会确保实际状态与之匹配。

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app-deployment
  labels:
    app: my-app
spec:
  replicas: 3                              # 期望副本数
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
        version: v1
    spec:
      containers:
      - name: my-app
        image: registry/my-app:v1
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
      restartPolicy: Always
  strategy:
    type: RollingUpdate                     # 滚动更新策略
    rollingUpdate:
      maxSurge: 1                           # 更新时最多超出期望副本数1个
      maxUnavailable: 0                     # 更新时允许不可用Pod数为0
```

**核心字段说明**：

- **replicas**：期望运行的Pod副本数。
- **selector.matchLabels**：Deployment管理的Pod标签选择器（必须与Pod模板标签匹配）。
- **template**：Pod模板，定义Pod的规格。
- **strategy**：更新策略（`RollingUpdate` 或 `Recreate`）。

#### 滚动更新与回滚

```bash
# 更新镜像版本
kubectl set image deployment/my-app-deployment my-app=registry/my-app:v2

# 查看更新状态
kubectl rollout status deployment/my-app-deployment

# 查看更新历史
kubectl rollout history deployment/my-app-deployment

# 回滚到上一个版本
kubectl rollout undo deployment/my-app-deployment

# 回滚到指定版本
kubectl rollout undo deployment/my-app-deployment --to-revision=2

# 暂停更新（用于金丝雀发布）
kubectl rollout pause deployment/my-app-deployment

# 恢复更新
kubectl rollout resume deployment/my-app-deployment
```

**滚动更新机制**：

```
更新前：3个Pod全部运行v1版本
→ 创建1个v2版本的新Pod（maxSurge=1）
→ 等待新Pod就绪（Readiness Probe通过）
→ 删除1个v1版本的旧Pod（maxUnavailable=0）
→ 再创建1个v2新Pod
→ ... 循环直到全部替换为v2
```

### 4.4 Service — 稳定的网络入口

Pod是临时性的（IP会变），Service提供了一层稳定的抽象——通过**标签选择器**找到匹配的Pod，并将请求负载均衡到这些Pod上。

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-app-service
spec:
  selector:
    app: my-app                        # 选择带有 app=my-app 标签的Pod
  ports:
  - protocol: TCP
    port: 80                           # Service端口
    targetPort: 8080                   # Pod容器端口
  type: ClusterIP                      # 默认类型，集群内可访问
```

#### Service类型

| 类型 | 访问方式 | 适用场景 |
|------|---------|---------|
| **ClusterIP** | 集群内部虚拟IP（10.x.x.x），仅集群内可访问 | 内部微服务通信 |
| **NodePort** | 每个节点的IP + 固定端口（30000-32767） | 开发测试，外部简单访问 |
| **LoadBalancer** | 云厂商提供公网负载均衡器 | 生产环境对外暴露 |
| **ExternalName** | DNS CNAME映射 | 将外部服务映射为集群内服务名 |

```yaml
# NodePort 示例
apiVersion: v1
kind: Service
metadata:
  name: my-app-nodeport
spec:
  type: NodePort
  selector:
    app: my-app
  ports:
  - port: 80
    targetPort: 8080
    nodePort: 30080                # 可选，不指定则随机分配

# LoadBalancer 示例（云环境）
apiVersion: v1
kind: Service
metadata:
  name: my-app-lb
spec:
  type: LoadBalancer
  selector:
    app: my-app
  ports:
  - port: 80
    targetPort: 8080
```

**Service与Pod的关联机制**：Service通过 `spec.selector` 中的标签匹配Pod。当Pod标签改变时，连接会自动更新。每个Service背后由**Endpoints**（或EndpointSlice）维护实际Pod IP列表。

```bash
# 查看Service的Endpoints
kubectl get endpoints my-app-service
```

### 4.5 ConfigMap — 非敏感配置

ConfigMap用于将配置信息从容器镜像中解耦，使应用配置可以在不重建镜像的情况下修改。

```yaml
# application.yml 作为ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  application.yml: |
    server:
      port: 8080

    spring:
      datasource:
        url: jdbc:mysql://mysql-service:3306/myapp
        username: root
        password: ${DB_PASSWORD}    # 密码通过Secret注入
      redis:
        host: redis-service
        port: 6379

    logging:
      level:
        com.myapp: INFO
```

**挂载方式**：

```yaml
# 方式1：挂载为文件（推荐，支持热更新）
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: my-app
        volumeMounts:
        - name: config
          mountPath: /app/config     # 挂载后 /app/config/application.yml
      volumes:
      - name: config
        configMap:
          name: app-config

# 方式2：注入为环境变量
# 不适用于复杂YAML结构，适合简单键值对
spec:
  template:
    spec:
      containers:
      - name: my-app
        envFrom:
        - configMapRef:
            name: app-config
```

**注意**：挂载为文件的ConfigMap支持**自动热更新**（kubelet定期同步，最长约60秒），但环境变量方式**不支持热更新**（注入时即固定）。

### 4.6 Secret — 敏感配置

Secret用于存储敏感信息（密码、Token、证书），但默认仅base64编码，**不是加密**！

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
type: Opaque
data:
  db-password: cm9vdDEyMw==          # echo -n "root123" | base64
  redis-password: cmVkaXMxMjM=
```

**使用方式**：

```yaml
spec:
  template:
    spec:
      containers:
      - name: my-app
        env:
        - name: DB_PASSWORD            # 应用读取的环境变量名
          valueFrom:
            secretKeyRef:
              name: app-secret
              key: db-password
        volumeMounts:
        - name: secret-volume
          mountPath: /etc/secret
      volumes:
      - name: secret-volume
        secret:
          secretName: app-secret
```

**生产环境Secret管理方案**：

- **Sealed Secrets**：Bitnami开源工具，SealedSecret CRD在集群内解密为Secret，Git中可以安全存储加密后的SealedSecret YAML。
- **External Secrets Operator**：从外部Vault/AWS Secrets Manager/Azure Key Vault同步Secret到K8s。
- **HashiCorp Vault**：成熟的密钥管理方案，支持动态凭据、租约和审计。

### 4.7 Ingress — HTTP/HTTPS路由

Ingress是K8s中的API对象，管理外部HTTP/HTTPS流量到集群内Service的路由。它需要配合**Ingress Controller**才能工作（如Nginx Ingress Controller、Traefik、AWS ALB Ingress Controller）。

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: my-app-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.myapp.com
    secretName: myapp-tls-secret        # TLS证书Secret
  rules:
  - host: api.myapp.com                 # 域名路由
    http:
      paths:
      - path: /api/v1
        pathType: Prefix
        backend:
          service:
            name: my-app-service
            port:
              number: 80
      - path: /                        # 兜底路由
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 80
  - host: admin.myapp.com              # 不同域名路由到不同Service
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: admin-service
            port:
              number: 80
```

**流量路径**：

```
用户 → DNS解析 → 负载均衡器 → Ingress Controller (Pod) → 根据Ingress规则 → Service → Pod
```

### 4.8 Namespace — 逻辑隔离

Namespace将集群内资源划分为逻辑分组，适用于多环境（dev/staging/prod）或多团队共享集群的场景。

```bash
# 操作特定Namespace的资源
kubectl get pods -n dev
kubectl get pods -n production

# 创建Namespace
kubectl create namespace dev

# 设置当前上下文默认Namespace
kubectl config set-context --current --namespace=dev
```

```yaml
# 在YAML中指定Namespace
apiVersion: v1
kind: Service
metadata:
  name: my-app-service
  namespace: production
```

**ResourceQuota和LimitRange**：Namespace级别的资源控制手段。

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: dev-quota
  namespace: dev
spec:
  hard:
    requests.cpu: "4"
    requests.memory: 8Gi
    limits.cpu: "8"
    limits.memory: 16Gi
    pods: "20"
```

### 4.9 水平自动扩缩（HPA）

HorizontalPodAutoscaler（HPA）根据CPU、内存或自定义指标自动调整Deployment的副本数。

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: my-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-app-deployment
  minReplicas: 2                    # 最小副本数
  maxReplicas: 10                   # 最大副本数
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70       # 目标CPU平均利用率70%
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

**HPA工作流程**：

```
监控指标收集（Metrics Server / Prometheus）
    ↓
HPA Controller 计算期望副本数：desired = ceil[currentReplicas * (currentValue / targetValue)]
    ↓
更新Deployment的replicas字段
    ↓
Deployment Controller创建/删除Pod
```

### 4.10 探针（Probe）

K8s提供了三种探针来感知容器状态，是保证应用高可用的关键：

```yaml
spec:
  containers:
  - name: my-app
    # === 存活探针：容器是否活着，失败则重启 ===
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      initialDelaySeconds: 30        # 容器启动后等待30秒开始检查
      periodSeconds: 10              # 每10秒检查一次
      timeoutSeconds: 3             # 超时3秒算失败
      failureThreshold: 3           # 连续失败3次重启

    # === 就绪探针：容器是否就绪，失败则移出Service ===
    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 5
      timeoutSeconds: 3
      failureThreshold: 2

    # === 启动探针：保护慢启动容器（K8s 1.18+） ===
    startupProbe:
      httpGet:
        path: /actuator/health
        port: 8080
      initialDelaySeconds: 5
      periodSeconds: 5
      failureThreshold: 30          # 最多检查30次*5秒=150秒
```

**三种探针的区别与配合**：

| 探针 | 失败后果 | 适用场景 |
|------|---------|---------|
| **Liveness** | 重启容器 | 检测死锁、内存泄漏、进程挂起等不可恢复的错误 |
| **Readiness** | 从Service Endpoints中移除 | 启动加载依赖、缓存预热、短暂高负载 |
| **Startup** | 重启容器（如果配置了Liveness，在Startup通过前禁用Liveness） | 启动时间很长的应用（如Spring Boot加载大量Bean） |

**Spring Boot Actuator配合**：

```java
// application.yml 配置
// management.endpoint.health.probes.enabled=true
// management.health.livenessstate.enabled=true
// management.health.readinessstate.enabled=true

// 此时 /actuator/health/liveness 和 /actuator/health/readiness 自动可用
// Liveness检查应用是否内部死锁
// Readiness检查外部依赖（DB、Redis等）是否就绪
```

**重要原则**：不要在Liveness Probe中检查外部依赖（数据库、Redis等），因为外部依赖故障会导致应用被重启，而重启通常不能解决外部依赖问题。Readiness Probe适合检查依赖就绪状态。

---

## 5. Kubernetes实战：部署Spring Boot应用

本节从零到一完成一个Spring Boot应用在K8s上的完整部署。

### 5.1 Kubernetes常用命令速查

```bash
# ===== 集群信息 =====
kubectl cluster-info                  # 集群信息
kubectl get nodes                     # 查看所有节点
kubectl describe node <node-name>     # 查看节点详情（资源使用、Pod分布）

# ===== 资源操作 =====
kubectl get pods                      # 查看Pod
kubectl get pods -o wide              # 查看Pod及所在节点和IP
kubectl get pods -w                   # 实时watch Pod状态变化
kubectl get all                       # 查看命名空间下所有资源
kubectl get pods --all-namespaces     # 查看所有命名空间的Pod

kubectl describe pod <pod-name>       # 查看Pod详情（事件、状态、日志）
kubectl logs <pod-name>               # 查看Pod日志
kubectl logs -f <pod-name>            # 跟踪日志
kubectl logs <pod-name> -c <container> # 多容器Pod指定容器

kubectl exec -it <pod-name> -- /bin/sh    # 进入Pod
kubectl exec <pod-name> -- ls /app        # 在Pod内执行命令

kubectl get deployments               # 查看Deployment
kubectl describe deployment <name>    # 查看详情
kubectl get services                  # 查看Service
kubectl get ingress                   # 查看Ingress
kubectl get configmap                 # 查看ConfigMap
kubectl get secret                    # 查看Secret
kubectl get hpa                       # 查看HPA
kubectl get events                    # 查看集群事件

# ===== 资源管理 =====
kubectl apply -f deployment.yaml      # 创建/更新资源（声明式）
kubectl delete -f deployment.yaml     # 删除资源
kubectl delete pod <pod-name>         # 删除Pod（Deployment会自动重建）
kubectl delete deployment <name>      # 删除Deployment

# ===== 调试 =====
kubectl top pods                      # 查看Pod资源使用
kubectl top nodes                     # 查看节点资源使用
kubectl port-forward pod/my-app 8080:8080  # 端口转发到本地调试
```

### 5.2 部署完整流程

#### 步骤1：构建并推送镜像

```bash
# 多阶段构建Dockerfile（见第2节）
docker build -t registry/my-app:v1.0.0 .
docker push registry/my-app:v1.0.0
```

#### 步骤2：创建ConfigMap

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: my-app-config
  namespace: default
data:
  application.yml: |
    server:
      port: 8080

    spring:
      datasource:
        url: jdbc:mysql://mysql-service:3306/myapp?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
        username: root
        password: ${DB_PASSWORD}
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
          idle-timeout: 300000
      redis:
        host: redis-service
        port: 6379
        password: ${REDIS_PASSWORD}
        timeout: 3000ms

    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          probes:
            enabled: true
      health:
        livenessstate:
          enabled: true
        readinessstate:
          enabled: true
```

#### 步骤3：创建Secret

```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: my-app-secret
  namespace: default
type: Opaque
data:
  db-password: cm9vdDEyMw==              # root123
  redis-password: cmVkaXMxMjM=            # redis123
```

#### 步骤4：创建Deployment

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
  namespace: default
  labels:
    app: my-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
      - name: my-app
        image: registry/my-app:v1.0.0
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: my-app-secret
              key: db-password
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: my-app-secret
              key: redis-password
        - name: SPRING_CONFIG_ADDITIONAL_LOCATION
          value: /app/config/
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "1"
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: tmp-volume
          mountPath: /tmp
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 15
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 2
        startupProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
          failureThreshold: 30
        securityContext:
          runAsNonRoot: true
          runAsUser: 1000
      volumes:
      - name: config-volume
        configMap:
          name: my-app-config
      - name: tmp-volume
        emptyDir: {}
      restartPolicy: Always
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

#### 步骤5：创建Service

```yaml
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: my-app-service
  namespace: default
spec:
  selector:
    app: my-app
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
    name: http
  type: ClusterIP
```

#### 步骤6：创建HPA（可选）

```yaml
# hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: my-app-hpa
  namespace: default
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-app
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

#### 步骤7：应用所有配置

```bash
# 按顺序应用
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f hpa.yaml

# 查看状态
kubectl get pods -w
kubectl get deployments
kubectl get services
kubectl get hpa

# 测试访问（端口转发到本地）
kubectl port-forward svc/my-app-service 8080:80

# 查看日志
kubectl logs -f -l app=my-app          # 跟踪所有app=my-app的Pod日志
```

---

## 6. CI/CD与容器化发布策略

### 6.1 容器化CI/CD流水线

典型的容器化CI/CD流水线包含以下阶段：

```
代码提交 → 自动构建 → 镜像推送 → 自动部署 → 健康检查 → 完成/回滚
  │          │           │           │          │
 git push   mvn package  docker push kubectl     kubectl rollout
 触发流水线  +docker build           set image   status
```

#### Jenkins Pipeline示例

```groovy
pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'registry.cn-hangzhou.aliyuncs.com'
        NAMESPACE = 'my-project'
        APP_NAME = 'my-app'
        VERSION = "${BUILD_NUMBER}"
        K8S_NAMESPACE = 'production'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean package -DskipTests=false'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                    docker build -t ${DOCKER_REGISTRY}/${NAMESPACE}/${APP_NAME}:${VERSION} .
                    docker tag ${DOCKER_REGISTRY}/${NAMESPACE}/${APP_NAME}:${VERSION} \
                               ${DOCKER_REGISTRY}/${NAMESPACE}/${APP_NAME}:latest
                """
            }
        }

        stage('Push Image') {
            steps {
                withDockerRegistry([credentialsId: 'docker-registry-cred', url: "https://${DOCKER_REGISTRY}"]) {
                    sh """
                        docker push ${DOCKER_REGISTRY}/${NAMESPACE}/${APP_NAME}:${VERSION}
                        docker push ${DOCKER_REGISTRY}/${NAMESPACE}/${APP_NAME}:latest
                    """
                }
            }
        }

        stage('Deploy to K8s') {
            steps {
                sh """
                    kubectl set image deployment/${APP_NAME} \
                        ${APP_NAME}=${DOCKER_REGISTRY}/${NAMESPACE}/${APP_NAME}:${VERSION} \
                        -n ${K8S_NAMESPACE}
                """
            }
        }

        stage('Verify Deployment') {
            steps {
                sh 'kubectl rollout status deployment/my-app -n production --timeout=5m'
            }
        }
    }

    post {
        failure {
            // 部署失败自动回滚
            sh 'kubectl rollout undo deployment/my-app -n production'
        }
    }
}
```

#### GitLab CI示例

```yaml
# .gitlab-ci.yml
stages:
  - build
  - dockerize
  - deploy

variables:
  DOCKER_REGISTRY: "registry.cn-hangzhou.aliyuncs.com"
  APP_NAME: "my-app"
  K8S_NAMESPACE: "production"

maven-build:
  stage: build
  image: maven:3.8-eclipse-temurin-17
  script:
    - mvn clean package -DskipTests
  artifacts:
    paths:
      - target/*.jar
    expire_in: 1 hour

docker-build:
  stage: dockerize
  image: docker:20
  services:
    - docker:dind
  script:
    - docker build -t ${DOCKER_REGISTRY}/my-project/${APP_NAME}:${CI_PIPELINE_ID} .
    - docker tag ${DOCKER_REGISTRY}/my-project/${APP_NAME}:${CI_PIPELINE_ID} \
                 ${DOCKER_REGISTRY}/my-project/${APP_NAME}:latest
    - docker login -u ${DOCKER_USER} -p ${DOCKER_PASSWORD} ${DOCKER_REGISTRY}
    - docker push ${DOCKER_REGISTRY}/my-project/${APP_NAME}:${CI_PIPELINE_ID}
    - docker push ${DOCKER_REGISTRY}/my-project/${APP_NAME}:latest

deploy-k8s:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/${APP_NAME} \
        ${APP_NAME}=${DOCKER_REGISTRY}/my-project/${APP_NAME}:${CI_PIPELINE_ID} \
        -n ${K8S_NAMESPACE}
    - kubectl rollout status deployment/${APP_NAME} -n ${K8S_NAMESPACE} --timeout=5m
  only:
    - main
```

### 6.2 蓝绿部署（Blue-Green Deployment）

蓝绿部署的核心思想是维护两套完全独立的环境（蓝=当前生产，绿=新版本），通过切换Service的选择器实现零停机发布。

```
                ┌──────────┐
                │ Service  │  ← 指向蓝色或绿色
                │ selector │
                └────┬─────┘
                     │
          ┌──────────┴──────────┐
          ▼                     ▼
   ┌──────────┐          ┌──────────┐
   │  Blue v1 │          │ Green v2 │
   │ (当前生产)│          │ (新版本)  │
   └──────────┘          └──────────┘
     标签:                  标签:
   app: my-app           app: my-app
   version: blue         version: green
```

**实现方式**：

```yaml
# 当前Service指向蓝色
apiVersion: v1
kind: Service
metadata:
  name: my-app-service
spec:
  selector:
    app: my-app
    version: blue          # 指向蓝色环境
  ports:
  - port: 80
    targetPort: 8080
```

```yaml
# 部署绿色环境
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app-green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: my-app
      version: green
  template:
    metadata:
      labels:
        app: my-app
        version: green
    spec:
      containers:
      - name: my-app
        image: registry/my-app:v2.0.0   # 新版本
        # ... 其余配置
```

```bash
# 验证绿色环境运行正常后，切换Service指向绿色
kubectl patch service my-app-service -p '{"spec":{"selector":{"version":"green"}}}'

# 确认绿色就绪后，删除蓝色环境
kubectl delete deployment my-app-blue
```

**优缺点**：
- 优点：瞬间切换，回滚极快（再次切回蓝色即可）
- 缺点：需要双倍资源，成本较高

### 6.3 金丝雀发布（Canary Release）

金丝雀发布逐步将小部分流量引入新版本，在监控确认正常后逐步增加比例，直到全量切换。

```
第一阶段： 100% v1（旧）   0% v2（新）  ← 金丝雀开始
第二阶段：  90% v1        10% v2       ← 观察10%流量
第三阶段：  50% v1        50% v2       ← 逐步放大
第四阶段：   0% v1       100% v2       ← 全量完成
```

#### 基于Service weight的实现（使用Istio）

```yaml
# Istio VirtualService 实现金丝雀
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: my-app-vs
spec:
  hosts:
  - my-app-service
  http:
  - route:
    - destination:
        host: my-app-service
        subset: v1
      weight: 90                        # 90%流量到v1
    - destination:
        host: my-app-service
        subset: v2
      weight: 10                        # 10%流量到v2
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: my-app-dr
spec:
  host: my-app-service
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
```

#### 基于Deployment副本数比例的简化方案

```bash
# 部署v1：3个副本（100%流量）
# 部署v2：1个副本 + 修改Service选择器

# 第一步：先部署1个v2副本
kubectl scale deployment my-app-v2 --replicas=1

# 第二步：调整Service selector匹配两个版本
# Service的selector为 app=my-app，同时匹配v1和v2的Pod

# v1副本：3个 → 占75%流量
# v2副本：1个 → 占25%流量
# 逐步scale up v2、scale down v1实现流量切换
kubectl scale deployment my-app-v2 --replicas=2   # v2占40%
kubectl scale deployment my-app-v1 --replicas=1   # v2占50%
kubectl scale deployment my-app-v2 --replicas=3   # v2占75%
kubectl scale deployment my-app-v1 --replicas=0   # 100% v2
```

### 6.4 回滚策略总结

| 发布方式 | 回滚方式 | 回滚速度 | 影响范围 |
|---------|---------|---------|---------|
| 滚动更新（RollingUpdate） | `kubectl rollout undo` | 分钟级（逐个替换Pod） | 部分流量短暂影响 |
| 蓝绿部署 | 切换Service selector | 秒级 | 无影响（瞬时切换） |
| 金丝雀发布 | 将权重调回0% | 秒级（DNS/配置生效） | 仅影响金丝雀用户 |

---

## 7. 总结与进阶路线

### 学习路线总结

```
初学者阶段：
  Docker基础 → Dockerfile构建 → Docker Compose编排 → Docker Swarm（了解即可）

进阶阶段：
  K8s核心概念 → kubectl命令 → Deployment/Service/ConfigMap → 部署实战

高级阶段：
  Ingress/Helm/Operator → Service Mesh（Istio） → GitOps（ArgoCD） → 安全加固

生产实践：
  监控（Prometheus + Grafana）
  日志（EFK/ELK）
  链路追踪（Jaeger/Zipkin）
  CI/CD（Jenkins/GitLab CI/ArgoCD）
```

### 常见误区与建议

1. **镜像太大**：总是使用Alpine或Slim版本，多阶段构建是必须的。
2. **以root运行容器**：生产环境务必使用非root用户运行，配合securityContext。
3. **依赖IP直连Pod**：Pod IP是临时的，永远通过Service访问。
4. **没有配置资源限制**：不设limits会导致单个Pod耗尽节点资源。
5. **探针配置不当**：Liveness不要检查外部依赖，Readiness检查依赖状态。
6. **ConfigMap热更新**：挂载为文件才支持热更新，环境变量方式不支持。
7. **Secret误以为加密**：Secret只是base64编码，敏感环境需配合外部密钥管理。
8. **忽略Pod反亲和性**：同一Deployment的Pod应分散在不同节点，提高可用性。

### 总结清单

| 内容 | 掌握要求 | 关键要点 |
|------|---------|---------|
| Docker基础命令 | 熟练使用 | pull/push/run/ps/exec/logs/build |
| Dockerfile编写 | 熟练掌握 | 多阶段构建、层优化、alpine、非root |
| Docker Compose | 熟练掌握 | services定义、volumes、networks、depends_on |
| K8s架构 | 理解 | Master/Worker、API Server、etcd、kubelet |
| Pod & Deployment | 熟练掌握 | 声明式管理、滚动更新、回滚 |
| Service & Ingress | 熟练使用 | ClusterIP/NodePort/LoadBalancer、域名路由 |
| ConfigMap & Secret | 熟练掌握 | 文件挂载、环境变量注入 |
| HPA & Probe | 理解并配置 | CPU/memory自动扩缩、三种探针配合 |
| 发布策略 | 理解 | 滚动更新、蓝绿部署、金丝雀发布 |
| CI/CD集成 | 能搭建 | Jenkins/GitLab → docker build → kubectl set image |

### 推荐学习资源

- **官方文档**：kubernetes.io（无可替代的最权威来源）
- **认证**：CKA（Certified Kubernetes Administrator）、CKAD（Certified Kubernetes Application Developer）
- **练习环境**：Minikube（本地单节点）、Kind（K8s in Docker，CI友好）、K3s（轻量级边缘集群）
- **工具链**：Helm（包管理）、Skaffold（开发迭代）、ArgoCD（GitOps）、Prometheus Operator（监控）

---

> **最后建议**：不要试图一次性掌握所有概念。从Docker入手——先能让Spring Boot应用在容器中运行，然后通过Docker Compose将MySQL+Redis+应用一键拉起。当理解了容器化带来的便利后，再引入K8s——先部署一个无状态应用，再逐步添加ConfigMap、Secret、Probe、HPA等功能。**先跑起来，再优化**，这是学习容器化最有效的路径。
