# Docker 快速通关 面试宝典
> 基于《Docker 快速通关教程》课程大纲，全面覆盖面试高频考点与实战场景

## 目录
1. [基础概念速答](#一基础概念速答)
2. [深度原理剖析](#二深度原理剖析)
3. [实战场景题](#三实战场景题)
4. [手写代码/配置文件题](#四手写代码配置文件题)
5. [系统设计题](#五系统设计题)
6. [常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [面试回答模板](#七面试回答模板)
8. [快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答

### Q1: 什么是 Docker？它与传统虚拟机有什么区别？
**Docker** 是一个开源的容器化引擎，基于 Linux 内核的 `cgroup`（资源隔离）和 `namespace`（命名空间）技术实现进程级别的资源隔离。它允许开发者将应用及其依赖打包到一个轻量级、可移植的容器中，在任何 Linux 机器上运行。
- **与 VM 对比**：容器共享宿主机内核，无需 Guest OS，启动毫秒级；VM 包含完整 Guest OS，启动分钟级。
- **资源利用率**：容器更高，虚拟机有额外 Hypervisor 开销。

### Q2: 什么是 Docker 镜像（Image）和容器（Container）？
- **镜像**：一个只读的模板，包含运行应用所需的文件系统、依赖、配置等。可以理解为"类的定义"。
- **容器**：镜像的运行实例，有可写层（Container Layer），可以启动、停止、删除。可以理解为"类的实例"。
- **关系**：`Image -> Container`，一个镜像可以创建多个容器。

### Q3: Docker 的架构包含哪些核心组件？
- **Docker Daemon**（dockerd）：守护进程，管理镜像、容器、网络、存储卷。
- **Docker Client**（docker CLI）：客户端工具，通过 REST API 与 Daemon 通信。
- **Docker Registry**：镜像仓库，默认是 Docker Hub，可搭建私有仓库（Harbor）。
- **Docker Objects**：镜像（Image）、容器（Container）、网络（Network）、数据卷（Volume）。

### Q4: 如何查看 Docker 版本和系统信息？
```bash
docker version          # 查看 Client 和 Server 版本
docker info             # 查看 Docker 系统级配置信息
docker system df        # 查看磁盘使用情况（镜像、容器、卷）
```

### Q5: `docker pull` 和 `docker run` 的区别？
- `docker pull`：从仓库下载镜像到本地，不创建容器。
- `docker run`：从镜像创建并启动一个容器；如果本地没有该镜像，会自动执行 `pull`。

### Q6: 如何列出本地所有镜像和容器？
```bash
docker images                  # 列出所有镜像（等价于 docker image ls）
docker ps                      # 列出运行中的容器
docker ps -a                   # 列出所有容器（包括已停止的）
docker container ls -a         # 等价于 docker ps -a
```

### Q7: `docker start`、`docker stop`、`docker rm` 分别用于什么？
- `docker start <container>`：启动一个已存在的已停止容器。
- `docker stop <container>`：优雅停止容器（发送 SIGTERM，超时后 SIGKILL）。
- `docker rm <container>`：删除一个容器（需先停止）。
- `docker rmi <image>`：删除一个镜像。
- `docker rm -f <container>`：强制删除运行中的容器。

### Q8: 如何查看容器日志？
```bash
docker logs <container>        # 查看日志
docker logs -f <container>     # 实时跟踪日志（tail -f 类似）
docker logs --tail 100 <container>  # 只看最后 100 行
docker logs -t <container>     # 显示时间戳
```

### Q9: 什么是 Docker Compose？解决什么问题？
**Docker Compose** 是一个用于定义和运行多容器 Docker 应用的工具。通过一个 `docker-compose.yml` 文件，可以一次性配置、启动多个关联服务（如 Web + MySQL + Redis），解决微服务架构中多容器编排的复杂度问题。

### Q10: 什么是 Dockerfile？核心作用是什么？
**Dockerfile** 是一个文本文件，包含构建镜像所需的指令序列。核心作用是实现基础设施即代码（Infrastructure as Code），让镜像构建可重复、可版本控制、可自动化。

### Q11: 什么是镜像分层机制（Layer Caching）？
Docker 镜像由多个只读层（Layer）堆叠组成，每一层对应 Dockerfile 中的一条指令（如 `RUN`、`COPY`）。构建时如果某一层未发生变化，Docker 会复用缓存层，大幅加速构建。每层只记录与上一层的差异。

### Q12: 如何保存和加载镜像？
```bash
docker save -o myimage.tar <image>    # 将镜像保存为 tar 文件
docker load -i myimage.tar            # 从 tar 文件加载镜像
docker commit <container> <new-image> # 将容器提交为新镜像（不推荐用于生产，应用 Dockerfile）
```

### Q13: 如何登录 Docker Hub 并推送镜像？
```bash
docker login                        # 登录 Docker Hub，输入用户名密码
docker tag myapp username/myapp:v1  # 标记镜像，格式为 username/repo:tag
docker push username/myapp:v1       # 推送到 Docker Hub
```

### Q14: `docker run` 的常用参数有哪些？
```bash
docker run -d <image>              # 后台运行（detached）
docker run -it <image> bash        # 交互式运行（interactive + tty）
docker run --name myapp <image>    # 指定容器名称
docker run -p 8080:80 <image>      # 端口映射：宿主机:容器
docker run -v /host:/container     # 目录挂载（bind mount）
docker run --rm <image>            # 停止后自动删除容器
docker run --restart=always        # 设置重启策略
```

### Q15: Docker 的存储方式有哪些？
- **Bind Mount**：将宿主机目录挂载到容器中，`docker run -v /host/path:/container/path`。
- **Volume**：由 Docker 管理的持久化数据卷，`docker run -v myvolume:/container/path`，推荐用于生产。
- **tmpfs**：存储在内存中，容器停止后数据消失，适用于临时敏感数据。

### Q16: Docker 的网络模式有哪些？
- **bridge**（默认）：每个容器有独立 IP，通过 Docker 网桥（docker0）通信。
- **host**：容器直接使用宿主机网络栈，无网络隔离，性能最高。
- **none**：无网络，适用于不需要网络的场景。
- **overlay**：跨主机通信，用于 Swarm 集群。
- **macvlan**：为容器分配 MAC 地址，使其像物理设备一样接入网络。

### Q17: 如何创建自定义 Docker 网络？
```bash
docker network create mynetwork          # 创建 bridge 网络
docker run --network mynetwork <image>   # 容器加入自定义网络
docker network connect mynetwork <cont>  # 将运行中的容器连接到网络
docker network ls                        # 列出所有网络
```
自定义网络中的容器可以通过容器名互相通信（内置 DNS 解析）。

### Q18: 什么是多阶段构建（Multistage Build）？
Docker 17.05+ 支持在单一 Dockerfile 中使用多个 `FROM` 指令，每个 FROM 开始一个新的构建阶段，最终只将最后一个阶段的产物复制到最终镜像中，极大减小镜像体积。典型场景：编译阶段用大镜像（含 JDK、Maven），运行阶段用精简镜像（仅 JRE）。

---

## 二、深度原理剖析

### Q19: Docker 底层核心技术是什么？请详细说明 Namespace 和 Cgroup。
Docker 底层依赖 Linux 内核的两大特性：
- **Namespace（命名空间）**：实现资源隔离。Docker 使用 6 种命名空间：
  - `PID Namespace`：进程隔离，容器内无法看到宿主机进程。
  - `Network Namespace`：网络隔离，每个容器有独立的网络栈。
  - `Mount Namespace`：文件系统挂载隔离。
  - `UTS Namespace`：主机名隔离。
  - `IPC Namespace`：进程间通信隔离。
  - `User Namespace`：用户 ID 隔离。
- **Cgroup（Control Groups）**：实现资源限制。可以限制容器对 CPU、内存、磁盘 IO、网络带宽的使用上限，防止一个容器耗尽宿主机资源。

### Q20: 镜像分层机制的工作原理是什么？UnionFS 是什么？
Docker 使用 **UnionFS（联合文件系统）** 实现镜像分层。常见实现包括 `OverlayFS`、`AUFS`、`devicemapper`。
- 镜像由多个只读层（Layer）堆叠，每一层包含文件系统的增量变更。
- 容器启动时，Docker 在镜像层之上添加一个可写层（Container Layer）。
- 当容器修改文件时使用 **写时复制（Copy-on-Write, CoW）** 策略：从只读层复制文件到可写层进行修改，上层覆盖下层同名文件。
- 镜像层的构建缓存：Docker 对每层计算 checksum，如果某层未改变，则复用本地缓存。

### Q21: Cgroup 如何限制容器的 CPU 和内存使用？
```bash
# 限制内存使用为 512MB，不允许使用 Swap
docker run -m 512m --memory-swap 0 <image>

# 限制 CPU 使用权重（默认 1024）
docker run --cpu-shares 512 <image>

# 限制 CPU 核心数
docker run --cpus 1.5 <image>

# 限制 CPU 亲和性
docker run --cpuset-cpus 0,2 <image>
```
底层通过写入 `/sys/fs/cgroup/memory/` 和 `/sys/fs/cgroup/cpu/` 文件实现。

### Q22: Docker 网络通信的数据流向是怎样的（bridge 模式下）？
容器 eth0 -> veth pair（虚拟网卡对）-> docker0 网桥 -> 宿主机网络栈 -> 外部网络。
- 每个容器有一对 veth 接口，一端在容器内（eth0），一端挂在 docker0 网桥上（vethxxx）。
- 容器间通信通过 docker0 网桥转发。
- 端口映射 `-p 8080:80` 通过 iptables NAT 规则实现：宿主机 8080 -> 容器 IP:80。

### Q23: Docker 的存储驱动（Storage Driver）有哪些？各有什么优缺点？
- **OverlayFS / Overlay2**（推荐）：性能好，支持页缓存共享，主流 Linux 发行版默认。
- **AUFS**：稳定但未合入 Linux 主线内核，Ubuntu 早期默认。
- **devicemapper**：RHEL/CentOS 早期默认，性能较差，配置复杂。
- **btrfs/zfs**：利用自身文件系统特性，快照效率高但配置复杂。
- **VFS**：无 CoW 特性，每层完整拷贝，性能差，仅用于测试。

### Q24: Docker 的镜像构建缓存机制是怎样的？如何最大化利用？
Docker 在构建时逐层执行 Dockerfile 指令，并为每层生成缓存键（包含指令内容、父层 ID、构建上下文文件 checksum）。如果缓存命中则直接复用。
- **利用技巧**：将不常变化的指令放在前面，频繁变化的放在后面。经典顺序：
  ```dockerfile
  FROM base
  RUN apt-get update && apt-get install -y packages   # 依赖不变 → 放前面
  COPY requirements.txt .  && pip install             # 依赖列表 → 中间
  COPY . .                                            # 源码变化最频繁 → 放最后
  ```
- **失效场景**：某层未命中后，后续所有层的缓存都失效。
- **强制不使用缓存**：`docker build --no-cache .`

### Q25: `docker commit` 和 Dockerfile 构建的区别与优劣？
| 维度 | docker commit | Dockerfile |
|------|--------------|------------|
| 可重复性 | 手动操作，不可重复 | 代码化，可版本控制 |
| 透明度 | 黑盒，不知道做了什么修改 | 每条指令清晰可见 |
| 镜像大小 | 容易膨胀，包含无关文件 | 精确控制每一层 |
| 调试难度 | 困难 | 逐层构建，易于定位 |
| 生产推荐 | 不推荐 | 强烈推荐 |

---

## 三、实战场景题

### Q26: 部署一个 WordPress 网站，需要关联 MySQL 数据库，请给出完整方案。
**方案**：使用 Docker Compose 定义 WordPress + MySQL 两个服务。
```yaml
# docker-compose.yml
version: '3'
services:
  db:
    image: mysql:5.7
    volumes:
      - db_data:/var/lib/mysql
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: wordpress
      MYSQL_USER: wpuser
      MYSQL_PASSWORD: wppass
    restart: always

  wordpress:
    image: wordpress:latest
    ports:
      - "8080:80"
    environment:
      WORDPRESS_DB_HOST: db
      WORDPRESS_DB_USER: wpuser
      WORDPRESS_DB_PASSWORD: wppass
      WORDPRESS_DB_NAME: wordpress
    volumes:
      - wp_data:/var/www/html
    restart: always

volumes:
  db_data:
  wp_data:
```
启动：`docker compose up -d`

### Q27: 构建一个 Spring Boot 应用的 Docker 镜像，要求镜像尽量小。
**方案**：使用多阶段构建 + 精简基础镜像（如 alpine）。
```dockerfile
# 第一阶段：编译
FROM maven:3.8-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# 第二阶段：运行
FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Q28: 搭建 Redis 主从集群，使用 Docker 自定义网络实现。
```bash
# 1. 创建自定义网络
docker network create redis-net

# 2. 启动 Redis 主节点
docker run -d --name redis-master \
  --network redis-net \
  -p 6379:6379 \
  redis:7 redis-server --appendonly yes

# 3. 启动两个从节点
for i in 1 2; do
  docker run -d --name redis-slave-$i \
    --network redis-net \
    -p 638$i:6379 \
    redis:7 redis-server --appendonly yes \
    --replicaof redis-master 6379
done

# 4. 验证主从状态
docker exec redis-master redis-cli info replication
```

### Q29: 一键启动所有中间件（MySQL、Redis、RabbitMQ、Nginx）的 Compose 配置。
```yaml
version: '3'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - backend

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - backend

  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin
    networks:
      - backend

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    networks:
      - frontend
    depends_on:
      - backend

networks:
  frontend:
  backend:

volumes:
  mysql_data:
  redis_data:
```

### Q30: CI/CD 流水线中如何集成 Docker 镜像构建和推送？
```yaml
# 伪代码（GitLab CI 示例）
stages:
  - build
  - push

variables:
  IMAGE_TAG: $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA

build:
  stage: build
  script:
    - docker build -t $IMAGE_TAG .
    - docker tag $IMAGE_TAG $CI_REGISTRY_IMAGE:latest
  only:
    - main

push:
  stage: push
  script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
    - docker push $IMAGE_TAG
    - docker push $CI_REGISTRY_IMAGE:latest
  only:
    - main
```

### Q31: 如何查看容器的资源使用情况（CPU、内存、网络 IO）？
```bash
docker stats                  # 实时查看所有容器的 CPU、内存、网络、磁盘 IO
docker stats <container>      # 查看指定容器
docker top <container>        # 查看容器内运行的进程
docker inspect <container>    # 查看容器详细信息（JSON 格式，含资源限制配置）
```

### Q32: 生产环境中如何为 Docker 容器设置健康检查？
```dockerfile
# Dockerfile 中定义 Healthcheck
FROM nginx:alpine
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -f http://localhost/ || exit 1
```
或 Compose 中定义：
```yaml
services:
  nginx:
    image: nginx:alpine
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost/"]
      interval: 30s
      timeout: 3s
      retries: 3
```

---

## 四、手写代码/配置文件题

### Q33: 一个完整且优化的 Dockerfile（Spring Boot + Layer 缓存优化）
```dockerfile
# ============================================================
# Dockerfile — Spring Boot 应用, 含层缓存优化
# ============================================================

# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 1. 先复制 pom.xml，利用缓存复用依赖下载层
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. 复制源码（源码变更最频繁，放后面）
COPY src ./src
RUN mvn package -DskipTests -B

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 创建非 root 用户，增强安全性
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 从 builder 阶段复制构建产物
COPY --from=builder /build/target/*.jar app.jar

# 设定时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 暴露端口
EXPOSE 8080

# 非 root 用户运行
USER appuser

# 入口
ENTRYPOINT ["java"]
CMD ["-jar", "app.jar"]
```

### Q34: 多阶段构建 Go 应用示例
```dockerfile
# Stage 1: Build
FROM golang:1.21 AS builder
WORKDIR /src
COPY go.mod go.sum .
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -a -o /app .

# Stage 2: Run (使用 scratch 最小基础镜像)
FROM scratch
COPY --from=builder /app /app
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
EXPOSE 8080
ENTRYPOINT ["/app"]
```

### Q35: 完整的 WordPress + MySQL Compose 配置（含 Healthcheck + Volume）
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: wordpress-db
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:-root123}
      MYSQL_DATABASE: ${DB_NAME:-wordpress}
      MYSQL_USER: ${DB_USER:-wpuser}
      MYSQL_PASSWORD: ${DB_PASSWORD:-wppass}
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - wp-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  wordpress:
    image: wordpress:6.2-php8.2-apache
    container_name: wordpress-app
    restart: unless-stopped
    ports:
      - "8080:80"
    environment:
      WORDPRESS_DB_HOST: mysql:3306
      WORDPRESS_DB_USER: ${DB_USER:-wpuser}
      WORDPRESS_DB_PASSWORD: ${DB_PASSWORD:-wppass}
      WORDPRESS_DB_NAME: ${DB_NAME:-wordpress}
    volumes:
      - wp_data:/var/www/html
    networks:
      - wp-network
    depends_on:
      mysql:
        condition: service_healthy

networks:
  wp-network:
    driver: bridge

volumes:
  mysql_data:
  wp_data:
```

### Q36: 使用 .dockerignore 优化构建
```dockerignore
# .dockerignore — 排除无关文件，加速构建、减小镜像
.git/
.gitignore
node_modules/
target/
*.log
.idea/
.vscode/
*.md
Dockerfile
.dockerignore
```

### Q37: Docker Compose 语法关键字段详解
```yaml
version: '3.8'          # Compose 文件版本，与 Docker 引擎版本对应

services:               # 定义多个服务
  service-name:
    image: ...          # 指定镜像
    build: ./dir        # 指定 Dockerfile 路径
    container_name: ... # 自定义容器名
    ports:              # 端口映射
      - "8080:80"
    expose:             # 仅暴露端口给链接服务
      - "3000"
    volumes:            # 数据卷挂载
      - ./host:/container
      - named_vol:/data
    environment:        # 环境变量
      - KEY=VALUE
    env_file: .env      # 从文件加载环境变量
    networks:           # 网络
      - mynet
    depends_on:         # 依赖关系（控制启动顺序）
      - db
      redis:
        condition: service_healthy
    restart: always     # 重启策略
    healthcheck:        # 健康检查
      test: ["CMD", "curl", "-f", "http://localhost"]
    deploy:             # 部署配置（Swarm 模式）
      replicas: 3
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
    logging:            # 日志驱动
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"

networks:               # 声明网络
  mynet:
    driver: bridge

volumes:                # 声明命名卷
  named_vol:
```

### Q38: Layer 缓存优化示例（用错误顺序示范缓存失效）
```dockerfile
# ❌ 错误写法：源码先复制，导致每次修改源码都需重新下载依赖
FROM maven:3.8-jdk-11
WORKDIR /app
COPY . .                        # 源码变更 → 此层缓存失效
RUN mvn dependency:go-offline   # 每次都要重跑
RUN mvn package

# ✅ 正确写法：分层利用缓存
FROM maven:3.8-jdk-11 AS builder
WORKDIR /app
COPY pom.xml .                  # 依赖变更不频繁
RUN mvn dependency:go-offline   # 复用缓存
COPY src ./src                  # 源码变更只影响这层及之后
RUN mvn package -DskipTests
```

### Q39: Docker Compose 中部署 Nginx 反向代理
```yaml
version: '3.8'
services:
  nginx:
    image: nginx:alpine
    container_name: nginx-proxy
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
      - ./www:/usr/share/nginx/html:ro
    networks:
      - frontend
      - backend
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"

  app:
    image: myapp:latest
    expose:
      - "3000"
    networks:
      - backend
    environment:
      - NODE_ENV=production

networks:
  frontend:
  backend:
```

---

## 五、系统设计题

### Q40: 设计一个微服务架构下的 Docker 部署方案，包含多个服务、数据库、消息队列和反向代理。
**设计方案**：
- **服务划分**：Gateway（API 网关）、UserService、OrderService、ProductService、NotificationService
- **基础设施**：MySQL（主从）、Redis（缓存）、RabbitMQ（消息队列）
- **反向代理**：Nginx（负载均衡 + SSL 终止）
- **技术栈**：Docker Compose 管理单机多服务，Swarm/K8s 管理集群

```yaml
version: '3.8'
services:
  gateway:
    build: ./gateway
    ports:
      - "443:443"
    depends_on: [user-svc, order-svc, product-svc]
    networks: [frontend, backend]

  user-svc:
    build: ./user-service
    depends_on: [mysql-master, redis]
    environment:
      - DB_HOST=mysql-master
      - REDIS_HOST=redis
    networks: [backend]

  order-svc:
    build: ./order-service
    depends_on: [mysql-master, redis, rabbitmq]
    networks: [backend]

  product-svc:
    build: ./product-service
    depends_on: [mysql-master, redis]
    networks: [backend]

  notification-svc:
    build: ./notification-service
    depends_on: [rabbitmq]
    networks: [backend]

  mysql-master:
    image: mysql:8.0
    volumes: [mysql_data:/var/lib/mysql]
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASS}
    networks: [backend]

  mysql-slave:
    image: mysql:8.0
    depends_on: [mysql-master]
    networks: [backend]

  redis:
    image: redis:7-alpine
    volumes: [redis_data:/data]
    networks: [backend]

  rabbitmq:
    image: rabbitmq:3-management
    environment:
      RABBITMQ_DEFAULT_USER: ${MQ_USER}
      RABBITMQ_DEFAULT_PASS: ${MQ_PASS}
    networks: [backend]

networks:
  frontend:
  backend:

volumes:
  mysql_data:
  redis_data:
```

### Q41: 如何设计一个高可用的私有镜像仓库方案？
**方案**：基于 Harbor 搭建企业级私有仓库。
- **高可用架构**：Harbor 多实例 + 共享存储（NFS/MinIO/S3）
- **安全机制**：HTTPS + 访问控制（RBAC）+ 镜像签名（Notary）+ 漏洞扫描（Trivy）
- **同步策略**：主从复制（跨机房同步）、P2P 分发（Dragonfly）
- **GC 策略**：定期清理无标签镜像、设置镜像保留策略
- **部署**：使用 Helm Chart 部署到 Kubernetes，数据库使用外部 PostgreSQL 和 Redis

### Q42: 设计一个 CI/CD 流水线，实现代码提交后自动构建 Docker 镜像并部署到测试环境。
**流水线设计**：
1. **代码提交** -> Git Hook 触发 CI
2. **单元测试** -> `mvn test` / `npm test`
3. **构建镜像** -> `docker build -t app:commit-sha .`
4. **镜像扫描** -> Trivy 扫描漏洞，高危漏洞中断流水线
5. **推送仓库** -> `docker push registry/app:commit-sha`
6. **部署测试环境** -> SSH 登录测试服务器，拉取新镜像重启容器
7. **集成测试** -> 运行自动化测试套件
8. **通知结果** -> 钉钉/Slack 发送构建结果通知

### Q43: 你认为 Docker Swarm 和 Kubernetes 在容器编排上各有什么优劣？如何选择？
| 维度 | Docker Swarm | Kubernetes |
|------|-------------|------------|
| 安装部署 | 极简（Docker 内置） | 复杂，需要大量组件 |
| 学习曲线 | 低 | 高 |
| 功能丰富度 | 基础功能 | 极其丰富（自动扩缩、滚动更新、服务网格等） |
| 生态 | 有限 | 庞大（Helm、Operator、Istio 等） |
| 社区活跃度 | 低 | 极高 |
| 生产案例 | 中小规模 | 大规模、复杂场景 |
| 多云/混合云 | 支持有限 | 原生支持 |

**选择建议**：
- 团队小、节点数 < 50、业务简单 → Swarm
- 大规模、复杂调度需求、多云部署 → K8s
- 云原生优先 → K8s

### Q44: 如何设计 Docker 容器的日志收集方案（ELK/EFK）？
**架构设计**：
```
App Container → 日志文件/stdout → Logstash/Filebeat → Elasticsearch → Kibana
```
推荐方案：每个容器将日志输出到 stdout/stderr，Docker 使用 json-file 驱动收集，Filebeat 采集宿主机日志文件发送到 Elasticsearch，Kibana 可视化。
```yaml
# 使用 Docker Compose 部署 ELK
services:
  app:
    image: myapp
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  filebeat:
    image: docker.elastic.co/beats/filebeat:8.0
    volumes:
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - ./filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
    depends_on: [elasticsearch]

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.0
    environment:
      - discovery.type=single-node

  kibana:
    image: docker.elastic.co/kibana/kibana:8.0
    ports:
      - "5601:5601"
    depends_on: [elasticsearch]
```

---

## 六、常见坑点与最佳实践

| 坑点/问题 | 原因 | 解决方案 | 最佳实践 |
|-----------|------|---------|---------|
| 容器内部无法访问宿主机服务 | 容器使用 bridge 网络，localhost 指向容器自身 | 使用 `host.docker.internal`（Docker Desktop）或宿主机 IP | 多服务通信使用自定义 bridge 网络 + 容器名 |
| `docker build` 缓存不生效 | Dockerfile 指令顺序不合理，频繁变动的文件放在前面 | 将 `COPY` 源码放在 `RUN` 依赖安装之后 | 从不变到变排序：系统依赖 -> 应用依赖 -> 源码 |
| 容器退出后数据丢失 | 容器可写层在删除时被清理 | 使用 Volume 或 Bind Mount 持久化数据 | 生产环境始终使用 Named Volume |
| 镜像体积过大 | 使用完整基础镜像、未清理临时文件 | 使用 Alpine/Slim 基础镜像、多阶段构建、清理 apt 缓存 | `apt-get clean && rm -rf /var/lib/apt/lists/*` |
| 容器以 root 用户运行 | Dockerfile 未指定非 root 用户 | 使用 `USER` 指令创建并切换到非 root 用户 | `RUN addgroup -S app && adduser -S app -G app && USER app` |
| 时区不一致 | 基础镜像默认 UTC 时区 | 通过 ENV 或 RUN 设置时区 | `ENV TZ=Asia/Shanghai` |
| Compose 中依赖服务启动顺序依赖 | `depends_on` 仅等待容器启动，不等待服务就绪 | 使用 `depends_on` + condition: service_healthy + `wait-for-it.sh` | 应用层添加重试机制 |
| 日志占用过多磁盘 | 未限制日志大小和数量 | 限制日志驱动参数 | `logging: options: max-size: "10m" max-file: "3"` |
| 端口映射冲突 | 多个容器映射同一宿主机端口 | 手动分配不同端口或使用动态端口映射 | 生产环境使用反向代理统一入口 |
| 容器内 PID 1 问题 | 直接 `CMD java -jar`，Java 进程是 PID 1 | 使用 `exec` 形式的 ENTRYPOINT/CMD | `ENTRYPOINT ["java", "-jar", "app.jar"]` — exec 形式而非 shell 形式 |
| 构建上下文过大 | Dockerfile 所在目录包含 node_modules、target 等 | 使用 `.dockerignore` 排除无关目录 | `.git/ node_modules/ target/ *.log .idea/` |

---

## 七、面试回答模板

### 模板 1: "请说说 Docker 和虚拟机的区别"
```
Docker 是操作系统级别的虚拟化，共享宿主机内核，而虚拟机是硬件级别的虚拟化。
第一，从架构上看，Docker 包含镜像、容器、仓库三个核心概念，而虚拟机包含宿主机 OS、Hypervisor、Guest OS；
第二，从性能上看，Docker 容器启动是毫秒级，虚拟机是分钟级；
第三，从资源利用率看，Docker 更加轻量，一台物理机可以运行上百个容器，而虚拟机只能运行十几个。
需要注意的一点是，Docker 不能虚拟化不同内核的操作系统，比如不能在 Linux 宿主机上运行 Windows 容器。

举个例子：在 2C4G 的服务器上，能运行 3-4 个虚拟机，但可以运行 20-30 个 Docker 容器。
```

### 模板 2: "Dockerfile 中 CMD 和 ENTRYPOINT 有什么区别？"
```
CMD 和 ENTRYPOINT 都是容器启动时的入口指令，核心区别在于：
第一，CMD 可以被 docker run 命令行参数覆盖，而 ENTRYPOINT 默认不能被覆盖。
第二，当两者同时存在时，CMD 作为 ENTRYPOINT 的默认参数。
第三，推荐用法是：ENTRYPOINT 定义主命令，CMD 定义默认参数。

举例说明：
  ENTRYPOINT ["nginx"]
  CMD ["-g", "daemon off;"]
  
使用 docker run 时，如果带参数如 docker run -it image -v，则 -v 覆盖 CMD，成为 nginx -v。
如果只写 docker run image，则执行 nginx -g daemon off;。

还有 exec 形式和 shell 形式的区别，推荐使用 exec 形式（JSON 数组），不会启动多余的 shell 进程。
```

### 模板 3: "Docker 网络模式有哪些？怎么选择？"
```
Docker 有五种网络模式：

1. bridge（默认）：通过 docker0 网桥实现，容器有独立 IP，适合单机多容器通信。
2. host：容器直接使用宿主机网络，无隔离但性能最高，适合网络性能敏感的场景。
3. none：容器无网络，适合安全敏感或不需要网络的场景。
4. overlay：跨主机通信，用在 Docker Swarm 集群中。
5. macvlan：为容器分配物理网络 MAC 地址，适合需要直接接入物理网络的场景。

生产中最常用的是自定义 bridge 网络。自定义 bridge 相比默认 bridge 有两个关键优势：
一个是内置 DNS 解析，可以通过容器名直接通信；另一个是网络隔离，不同网络间的容器不能互通。

我曾在一个项目中就用自定义 bridge 网络来隔离前端和后端服务，只开放必要的通信端口。
```

### 模板 4: "什么是镜像分层？Docker 镜像为什么能减小体积？"
```
Docker 镜像由多个只读层堆叠组成，每一层本质上是一个文件系统的增量变更。

关键机制是 UnionFS 和写时复制（Copy-on-Write）：
- 构建时，每条 Dockerfile 指令生成一个新层，Docker 会缓存每层的 checksum。
- 运行时，在镜像层上叠加一个可写层，容器对文件的修改都发生在这个可写层。
- 如果多个容器共享同一个镜像，它们复用同一份只读镜像层，只需各自创建极小的可写层。

这就解释了为什么容器启动快、占用空间小。使用多阶段构建还能进一步优化：
第一阶段用大镜像编译（Maven + JDK），第二阶段只把编译产物复制到 JRE 小镜像中。
最终镜像只包含运行所需的最小文件集。

此外，层缓存机制也很重要：把不常变的依赖安装放在前面，经常变的源码复制放在后面，
这样开发迭代时大量利用缓存，构建速度能提升 80% 以上。
```

### 模板 5: "生产环境中使用 Docker 需要注意哪些安全问题？"
```
生产环境中 Docker 安全需要注意以下 7 个方面：

1. 镜像安全：只使用官方或可信基础镜像，定期用 Trivy/Clair 扫描漏洞。
2. 非 root 运行：Dockerfile 中创建专用用户并使用 USER 指令切换，避免容器以 root 运行。
3. 资源限制：通过 -m/--cpus 等参数限制容器资源使用，防止 DoS 攻击。
4. 内核能力限制：使用 --cap-drop=ALL 移除所有内核能力，再按需添加 --cap-add。
5. 只读根文件系统：使用 --read-only 挂载容器根文件系统为只读，结合 volume 写入数据。
6. 镜像签名：使用 Docker Content Trust 验证镜像来源和完整性。
7. 安全审计：定期运行 docker scan 和 docker bench security 进行安全审计。

我个人在项目中还特别关注 .dockerignore 的使用，避免将敏感文件（如密钥）打包进镜像。
同时 Docker daemon 的 API 绝对不能暴露在公网，否则相当于把 root 权限给任何人。
```

---

## 八、快速查漏补缺 Checklist

### 基础概念
- [ ] Docker 是什么，与 VM 的区别（资源隔离级别、启动速度、体积）
- [ ] 镜像（Image）、容器（Container）、仓库（Registry）三者关系
- [ ] Docker 架构：Client / Daemon / Registry / REST API
- [ ] Namespace 六种隔离类型
- [ ] Cgroup 如何限制 CPU、内存、磁盘 IO

### 镜像操作
- [ ] `docker pull` / `docker images` / `docker rmi` / `docker search`
- [ ] `docker build` 和 Dockerfile
- [ ] `docker commit`（了解即可，不推荐生产使用）
- [ ] `docker save` / `docker load`（离线传输镜像）
- [ ] `docker tag` / `docker push` / `docker login`
- [ ] `.dockerignore` 文件配置

### 容器操作
- [ ] `docker run`（-d, -it, -p, -v, --name, --rm, --restart）
- [ ] `docker ps -a` / `docker start` / `docker stop` / `docker rm -f`
- [ ] `docker logs -f` / `docker logs --tail`
- [ ] `docker exec -it <container> bash`（进入运行中的容器）
- [ ] `docker inspect`（查看容器/镜像详细信息）
- [ ] `docker stats`（查看资源使用情况）

### Dockerfile
- [ ] FROM / RUN / COPY / ADD / CMD / ENTRYPOINT / ENV / WORKDIR / EXPOSE / USER / VOLUME / HEALTHCHECK
- [ ] CMD 与 ENTRYPOINT 区别（exec 形式 vs shell 形式）
- [ ] 多阶段构建（Multistage Build）
- [ ] Layer 缓存优化（指令排序）
- [ ] 最小基础镜像选择（alpine / slim / distroless）

### 存储
- [ ] Bind Mount 与 Named Volume 的区别和使用场景
- [ ] tmpfs 挂载（内存存储）
- [ ] `docker volume create/ls/inspect/prune`
- [ ] Volume 的备份和恢复

### 网络
- [ ] bridge / host / none / overlay / macvlan 五种模式
- [ ] 自定义 bridge 网络（DNS 解析容器名）
- [ ] 端口映射原理（iptables NAT）
- [ ] 跨主机网络方案（overlay + Swarm / K8s CNI）

### Docker Compose
- [ ] `docker-compose.yml` 语法：services / networks / volumes
- [ ] environment / env_file 的使用
- [ ] depends_on 和 condition: service_healthy
- [ ] restart 策略（no / always / on-failure / unless-stopped）
- [ ] deploy 配置（replicas / resources / update_config）
- [ ] logging 配置（max-size / max-file）
- [ ] `docker compose up -d` / `docker compose down` / `docker compose logs`

### 安全与最佳实践
- [ ] 非 root 用户运行容器（USER 指令）
- [ ] 镜像漏洞扫描（Trivy / Clair / Docker Scout）
- [ ] 资源限制（--cpus / --memory / --memory-swap）
- [ ] 内核能力管理（--cap-drop=ALL --cap-add=...）
- [ ] 只读根文件系统（--read-only）
- [ ] Docker Content Trust 签名验证
- [ ] 日志限制（max-size / max-file）
- [ ] .dockerignore 使用

### 进阶
- [ ] Docker Swarm 与 Kubernetes 对比
- [ ] 私有仓库搭建（Harbor / Registry）
- [ ] CI/CD 中 Docker 集成
- [ ] ELK/EFK 日志收集方案
- [ ] Docker 监控（cAdvisor / Prometheus）
- [ ] 容器健康检查（HEALTHCHECK / healthcheck）
- [ ] Docker Buildx 多平台构建（ARM + x86）

---

> 本文档基于《Docker 快速通关教程》（08-Docker快速通关.md）编写，覆盖面试高频考点。
> 建议结合课程视频动手实践每个命令，理解原理比背诵命令更重要。
