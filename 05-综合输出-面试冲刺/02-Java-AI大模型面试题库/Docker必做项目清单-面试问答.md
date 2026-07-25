# Docker 面试问答清单
> 🎯 基于项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：Docker 的核心概念有哪些？镜像、容器、仓库三者的关系是什么？
**面试官意图：** 考察对 Docker 基础概念的理解，这是面试 Docker 的必问题。

**完美解答：**

**三种核心概念的关系：**

| 概念 | 类比 | 定义 | 生命周期 |
|------|------|------|---------|
| **镜像（Image）** | 类的定义 / ISO 安装盘 | 一个只读的、包含应用程序及其依赖环境的模板 | 不变，可被版本管理 |
| **容器（Container）** | 类的实例 / 安装好的系统 | 镜像的运行实例，是一个隔离的运行时环境 | 可变，有状态 |
| **仓库（Registry）** | GitHub（代码仓库） | 存储和分发镜像的地方，如 Docker Hub | 持久存储 |

**关系示意图：**
```
镜像（只读模板） → docker run → 容器（运行实例）
    ↑                            ↓
 构建 (docker build)        停止/删除
    ↑
Dockerfile（构建脚本）
    ↓
仓库 (Registry) → docker pull → 镜像 → docker run → 容器
```

**核心命令演示：**
```bash
# 从仓库拉取镜像
docker pull mysql:8.0

# 查看本地镜像
docker images

# 从镜像创建并运行容器
docker run -d --name mysql8 \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -v /data/mysql:/var/lib/mysql \
  mysql:8.0

# 查看运行中的容器
docker ps

# 进入容器内部
docker exec -it mysql8 bash
```

> 💡 **核心理解**：镜像是"制作好的披萨面团"（可随时使用），容器是"已经烤好的披萨"（正在提供服务），仓库是"披萨店"（存储和分发）。

---

### Q2：Dockerfile 的核心指令有哪些？多阶段构建是什么？
**面试官意图：** 考察镜像构建的工程能力，特别是多阶段构建（简历高频关键词）。

**完美解答：**

**Dockerfile 核心指令：**

| 指令 | 作用 | 示例 | 注意 |
|------|------|------|------|
| `FROM` | 指定基础镜像 | `FROM openjdk:17-jdk-slim` | 尽量选 slim/alpine 版本 |
| `WORKDIR` | 设置工作目录 | `WORKDIR /app` | 后面的路径都基于此 |
| `COPY` | 复制文件到镜像 | `COPY target/app.jar /app/app.jar` | 保留文件权限 |
| `ADD` | 复制 + 解压缩 | `ADD app.tar.gz /app/` | 比 COPY 多自动解压功能 |
| `RUN` | 构建时执行命令 | `RUN apt-get update && apt-get install -y curl` | 每个 RUN 增加一层 |
| `CMD` | 容器启动时的默认命令 | `CMD ["java", "-jar", "app.jar"]` | 可被 `docker run` 参数覆盖 |
| `ENTRYPOINT` | 容器启动入口 | `ENTRYPOINT ["java", "-jar", "app.jar"]` | 不可被覆盖（除非 --entrypoint） |
| `EXPOSE` | 声明端口 | `EXPOSE 8080` | 仅是文档声明，不实际映射 |
| `ENV` | 环境变量 | `ENV SPRING_PROFILES_ACTIVE=prod` | 运行时可用 `-e` 覆盖 |
| `ARG` | 构建参数 | `ARG JAR_FILE=target/*.jar` | 仅在构建时有效 |

**多阶段构建（Java SpringBoot 示例，面试高频）：**

```dockerfile
# ====== 第一阶段：构建 ======
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
# 下载依赖（利用 Docker Layer 缓存）
RUN mvn dependency:go-offline -B
COPY src ./src
# 编译打包
RUN mvn package -DskipTests

# ====== 第二阶段：运行（只保留运行所需的最小内容） ======
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 从 builder 阶段复制构建产物
COPY --from=builder /build/target/*.jar app.jar

# 安全配置：创建非 root 用户运行
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**多阶段构建的优势：**

| 对比 | 单阶段构建 | 多阶段构建 |
|------|-----------|-----------|
| **镜像大小** | 500MB+（包含 JDK + Maven + 源码 + 依赖包） | 100MB-200MB（仅 JRE + jar） |
| **安全风险** | 包含构建工具（可能被利用） | 只有运行环境 |
| **构建速度** | 每次都要编译 | 利用缓存层加速 |

> 🎯 **结论**：多阶段构建是现代 Java 容器化的标准实践。核心思想是"构建和运行分离"，第一阶段有完整的构建工具链，第二阶段只拷贝最终产物，做到镜像最小化。

---

### Q3：Docker Compose 有什么作用？docker-compose.yml 的核心结构是什么？
**面试官意图：** 考察多服务编排能力，这是微服务部署的基础技能。

**完美解答：**

**Docker Compose 的作用：** 通过一个 YAML 文件定义和管理多个 Docker 容器，实现"一键启动整个应用栈"。

**核心结构：**

```yaml
version: '3.8'

services:
  # ====== 业务服务 ======
  app:
    build:
      context: .
      dockerfile: Dockerfile
    image: myapp:latest
    container_name: spring-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://mysql:3306/db?useSSL=false
      REDIS_HOST: redis
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    volumes:
      - ./logs:/app/logs
    restart: unless-stopped
    networks:
      - backend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # ====== 数据库 ======
  mysql:
    image: mysql:8.0
    container_name: mysql-db
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: db
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

  # ====== 缓存 ======
  redis:
    image: redis:7-alpine
    container_name: redis-cache
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}
    networks:
      - backend

volumes:
  mysql-data:
  redis-data:

networks:
  backend:
    driver: bridge
```

**常用 Compose 命令：**
```bash
# 启动所有服务
docker compose up -d

# 查看日志
docker compose logs -f

# 重启单个服务
docker compose restart app

# 扩缩容（服务开启多个实例）
docker compose up -d --scale app=3

# 停止并删除所有容器
docker compose down

# 只重新构建并启动修改过的服务
docker compose up -d --build app
```

> 💡 **环境变量管理**：用 `.env` 文件管理不同环境的变量，`docker-compose.yml` 中通过 `${VARIABLE}` 引用。不要硬编码密码和配置。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你如何将一个 SpringBoot 项目完整的容器化部署到生产环境？
**面试官意图：** 考察从开发到部署的完整 CI/CD 经验，这是后端工程师的核心技能之一。

**完美解答：**

**项目容器化部署全流程：**

**第一阶段：镜像构建（Maven + 多阶段构建）**

使用多阶段构建的 Dockerfile（上面 Q2 已展示），通过 Maven 插件集成到构建流程：

```xml
<!-- pom.xml 集成 Docker 构建 -->
<plugin>
    <groupId>com.spotify</groupId>
    <artifactId>dockerfile-maven-plugin</artifactId>
    <version>1.4.13</version>
    <configuration>
        <repository>${docker.registry}/myapp</repository>
        <tag>${project.version}</tag>
        <buildArgs>
            <JAR_FILE>target/${project.build.finalName}.jar</JAR_FILE>
        </buildArgs>
    </configuration>
</plugin>
```

**第二阶段：Docker Compose 多服务编排**

```yaml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    image: myapp:1.0.0
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://mysql:3306/db
      DB_USERNAME: root
      DB_PASSWORD: ${DB_PASSWORD}
    depends_on:
      mysql:
        condition: service_healthy
    volumes:
      - ./logs:/app/logs
    restart: unless-stopped
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: db
    volumes:
      - mysql-data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
```

**第三阶段：生产部署 + Nginx 反向代理**

```yaml
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d
      - ./nginx/ssl:/etc/nginx/ssl
      - ./static:/usr/share/nginx/html
    depends_on:
      - app
```

```nginx
# nginx/conf.d/app.conf
upstream app_servers {
    server app:8080 weight=1;
    server app:8081 weight=1; # 第二个实例
}

server {
    listen 80;
    server_name api.myapp.com;

    location / {
        proxy_pass http://app_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /static/ {
        alias /usr/share/nginx/html/;
        expires 7d;
    }
}
```

**生产部署最佳实践：**

| 实践 | 说明 |
|------|------|
| **健康检查** | 使用 `HEALTHCHECK` 或 SpringBoot Actuator，让 Docker 自动恢复异常容器 |
| **日志管理** | 配置 `json-file` 日志驱动，限制日志文件大小防止磁盘爆满 |
| **资源限制** | `deploy.resources.limits.memory: 1g`，防止单个服务耗尽主机内存 |
| **重启策略** | `restart: unless-stopped`，容器异常退出时自动重启 |
| **安全配置** | 使用非 root 用户运行、不暴露不必要的端口、使用 `.env` 管理密钥 |

> 💡 **一句话总结**：容器化不是简单的 `docker run`，而是包括镜像构建、编排配置、健康检查、日志管理、资源控制等一整套体系。

---

### Q5：Docker 的网络模式有哪些？微服务架构中如何选择网络方案？
**面试官意图：** 考察 Docker 网络原理的理解，这是多服务通信的基础。

**完美解答：**

**Docker 网络模式对比：**

| 网络模式 | 网络隔离 | 外部访问 | 服务间通信 | 适用场景 |
|----------|:--------:|:--------:|:----------:|---------|
| **bridge（默认）** | 容器之间可通过 IP 通信 | 需要端口映射 `-p` | 需要 Link 或自定义网络 | **单机多容器**（最常用） |
| **host** | 容器直接使用宿主机网络 | 直接使用宿主机端口 | 无隔离 | 高性能场景，端口冲突风险 |
| **none** | 完全隔离 | 无网络 | 无 | 安全敏感场景 |
| **overlay** | 跨主机容器通信 | 需要 ingress 网络 | 通过 Docker Swarm 或 K8s | **多主机集群**（生产环境） |

**常用自定义 bridge 网络：**
```yaml
# docker-compose 中定义多个网络实现访问控制
services:
  app:
    networks:
      - frontend    # 暴露给 Nginx
      - backend     # 连接 MySQL、Redis

  mysql:
    networks:
      - backend     # 只和后端服务通信，不对外暴露

  nginx:
    networks:
      - frontend    # 暴露到外部
    ports:
      - "80:80"

networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true  # backend 网络无法访问外部，增强安全性
```

> ⚠️ **面试关键点**：容器间通信使用**服务名**而不是 IP 地址。Docker Compose 会自动为每个 service 创建 DNS 解析，服务名就是主机名。例如 app 服务可以通过 `jdbc:mysql://mysql:3306/db` 连接数据库（而不是 IP）。

---

### Q6：你使用 Docker 部署过哪些中间件？说一个你最熟悉的中间件部署过程
**面试官意图：** 考察 Docker 部署中间件的实际经验。

**完美解答：**

**常用中间件一键部署（Docker 一键启动集群）：**

```yaml
# 完整中间件栈
version: '3.8'

services:
  # MySQL 8.0
  mysql:
    image: mysql:8.0
    container_name: middleware-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root123
    volumes:
      - mysql-data:/var/lib/mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5

  # Redis 7
  redis:
    image: redis:7-alpine
    container_name: middleware-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes --requirepass redis123

  # RabbitMQ 3.x (带管理后台)
  rabbitmq:
    image: rabbitmq:3.12-management
    container_name: middleware-rabbitmq
    ports:
      - "5672:5672"    # AMQP 协议
      - "15672:15672"  # 管理后台
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin123
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq

  # ElasticSearch 8.x
  elasticsearch:
    image: elasticsearch:8.10.2
    container_name: middleware-es
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data

  # Kibana
  kibana:
    image: kibana:8.10.2
    container_name: middleware-kibana
    ports:
      - "5601:5601"
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    depends_on:
      - elasticsearch

volumes:
  mysql-data:
  redis-data:
  rabbitmq-data:
  es-data:
```

```bash
# 一条命令启动所有中间件！
docker compose up -d

# 查看所有中间件状态
docker compose ps
```

> 💡 **面试加分**：能说出 ElasticSearch 需要配置 `vm.max_map_count=262144`（否则启动报错），以及 MySQL 8.0 需要使用 `native` 认证插件兼容旧版客户端——这些细节能体现你真的部署过。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：Docker 镜像体积优化有哪些方法？如何从 500MB 优化到 100MB？
**面试官意图：** 考察镜像优化的工程经验，这是简历上"镜像瘦身"的高频考点。

**完美解答：**

**镜像体积优化全攻略：**

| 优化策略 | 优化前 | 优化后 | 原理 |
|----------|--------|--------|------|
| **选择小基础镜像** | `openjdk:17-jdk` (350MB) | `eclipse-temurin:17-jre-alpine` (80MB) | JDK 换 JRE，Linux 换 Alpine |
| **多阶段构建** | 500MB (含 Maven/源码) | 120MB (仅 JRE + jar) | 分离构建环境与运行环境 |
| **清理缓存** | 200MB (apt/package 缓存) | 20MB | `rm -rf /var/lib/apt/lists/*` |
| **合并 RUN 指令** | 3 层 (每层 50MB) | 1 层 (50MB) | 每个 RUN 指令增加一层 |
| **减少不必要的文件** | 100MB (包含测试/文档) | 20MB | `.dockerignore` 排除无用文件 |

**Dockerfile 对比：**

```dockerfile
// ❌ 优化前
FROM openjdk:17-jdk
COPY . /app
WORKDIR /app
RUN ./mvnw package
EXPOSE 8080
CMD ["java", "-jar", "target/app.jar"]

// ✅ 优化后（核心：小镜像 + 多阶段 + 合并指令）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder target/app.jar .
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

```dockerfile
// .dockerignore（排除无用文件，减少构建上下文）
**/.git
**/.gitignore
**/node_modules
**/target
**/*.md
**/test/
**/docs/
Dockerfile
.dockerignore
```

**优化效果数据：**

| 优化措施 | 镜像大小 | 启动时间 | 传输时间（公网） |
|----------|:--------:|:--------:|:--------------:|
| 原始（JDK + Maven） | 520MB | 8s | 约 60s |
| + Alpine + JRE | 180MB | 4s | 约 20s |
| + 多阶段构建 | 120MB | 3s | 约 12s |
| + .dockerignore | 105MB | 3s | 约 10s |

> 🎯 **目标**：生产环境的 Java SpringBoot 镜像，**120MB 以内**是合格，**80MB 以内**是优秀。

---

### Q8：Docker 和 K8s 的关系是什么？什么时候需要从 Docker Compose 升级到 K8s？
**面试官意图：** 考察对容器编排体系的宏观理解，以及在不同规模下的技术选型能力。

**完美解答：**

**Docker vs K8s 定位对比：**

| 维度 | Docker Compose | Kubernetes |
|------|:-------------:|:----------:|
| **定位** | 单机容器编排 | 生产级容器集群管理 |
| **规模** | 单台机器，几个服务 | 成百上千台机器，上万个 Pod |
| **高可用** | 手动或 restart | 自动故障恢复、自愈 |
| **服务发现** | 基本（通过服务名） | 完善（Service + DNS + LoadBalancer） |
| **自动扩缩** | 手动 --scale | 自动（HPA 基于 CPU/内存/自定义指标） |
| **滚动更新** | 手动控制 | 自动化滚动更新 + 回滚 |
| **存储管理** | 简单的 Volume | PV/PVC 持久化存储体系 |
| **学习成本** | 低（1-2 天） | 高（至少 1-2 个月） |

**什么时候需要升级到 K8s：**

```
应用规模发展阶段：
├── 单机/开发环境 → Docker Compose ✅
├── 几台机器、几十个服务 → Docker Swarm 或轻量 K8s
├── 多团队协作、上百服务、要求高可用 → K8s ✅
└── 云原生、CI/CD、DevOps 成熟团队 → K8s + ServiceMesh ✅
```

**K8s 核心概念映射：**

```yaml
# K8s Deployment（代替 docker-compose 的服务定义）
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-app
spec:
  replicas: 3  # 3 个副本（代替 --scale）
  selector:
    matchLabels:
      app: spring-app
  template:
    metadata:
      labels:
        app: spring-app
    spec:
      containers:
      - name: app
        image: myapp:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        readinessProbe:     # 就绪检查
          httpGet:
            path: /actuator/health
            port: 8080
---
# K8s Service（代替 docker-compose 的端口映射）
apiVersion: v1
kind: Service
metadata:
  name: spring-app-service
spec:
  selector:
    app: spring-app
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

> 💡 **务实建议**：很多团队在只有几个微服务的情况下就上了 K8s，运维成本远高于收益。我的判断标准是——如果 Docker Compose + 健康检查 + 手动重启就能满足可用性要求，就不需要 K8s。**K8s 是解决大规模问题的工具，不是必须品。**

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：容器启动后马上就退出了，怎么排查？
**面试官意图：** 考察容器化部署的日常排障经验。

**完美解答：**

**排障流程：**

```bash
# Step 1: 查看容器状态和日志
docker ps -a                   # 查看所有容器（包括已退出的）
docker logs <container_id>     # 查看退出前的日志

# Step 2: 检查退出码
# 退出码 0    → 正常退出（可能是 CMD/ENTRYPOINT 执行完就退出了）
# 退出码 1    → 应用错误（Java 异常、配置错误等）
# 退出码 137  → 被 OOM Kill（内存不足）
# 退出码 139  → 段错误（内存访问越界）
# 退出码 143  → 被 SIGTERM 终止（正常停止）

# Step 3: 交互式运行，保持容器存活以便排查
docker run -it --entrypoint bash myimage:latest
# 进入容器后手动启动应用：
# java -jar app.jar

# Step 4: 检查资源限制
docker inspect <container_id> | grep -i memory
# 查看是否内存设置太小导致 OOM
```

**常见问题和解决方案：**

| 现象 | 退出码 | 原因 | 解决方案 |
|------|:------:|------|---------|
| **应用配置错误** | 1 | 数据库连接失败、端口被占用 | 检查环境变量和配置文件 |
| **OOM 被杀** | 137 | 容器内存限制太小 | `--memory=512m` 调大 |
| **启动脚本错误** | 127 | ENTRYPOINT 脚本找不到 | `docker run --entrypoint sh` 调试 |
| **前台进程缺失** | 0 | 启动命令是后台进程（`&`） | 使用前台命令或 `tail -f /dev/null` |
| **健康检查失败** | 非 0 | 健康检查返回非 200（K8s 场景） | 调整健康检查路径 |

> 💡 **核心排障口诀**："先看日志，再查配置，然后交互式运行，最后检查资源限制。"

---

### Q10：Docker 数据持久化的方式有哪些？bind mount 和 volume 有什么区别？
**面试官意图：** 考察对 Docker 数据管理的理解，这是容器化落地必须掌握的基础。

**完美解答：**

**三种数据持久化方式对比：**

| 方式 | 存储位置 | 生命周期 | 管理方式 | 适用场景 |
|------|---------|:--------:|---------|---------|
| **bind mount** | 宿主机指定目录 | 与宿主机目录一致 | 手动管理 | 开发热更新、配置文件挂载 |
| **volume** | Docker 管理的目录（`/var/lib/docker/volumes/`） | Docker 管理 | `docker volume` 命令 | **生产推荐** |
| **tmpfs mount** | 内存（不会写磁盘） | 容器停止即消失 | 临时存储 | 敏感数据、临时缓存 |

**Volume vs Bind Mount 深度对比：**

```bash
# bind mount：将宿主机目录挂载到容器
docker run -v /host/path:/container/path nginx
# 优点：方便查看修改
# 缺点：依赖宿主机路径，不方便迁移

# volume：使用 Docker 管理的存储
docker volume create nginx-data
docker run -v nginx-data:/usr/share/nginx/html nginx
# 优点：跨主机可移植、Docker 自动管理
# 缺点：不能直接查看内容
```

**生产实践示例：**
```yaml
services:
  mysql:
    image: mysql:8.0
    volumes:
      # 1. Volume：持久化数据（生产推荐）
      - mysql-data:/var/lib/mysql
      # 2. Bind mount：挂载初始化脚本（开发调试用）
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
      # 3. Bind mount：挂载配置文件（环境区分）
      - ./conf/my.cnf:/etc/mysql/conf.d/my.cnf

volumes:
  mysql-data:    # Docker 自动管理
```

> ⚠️ **生产环境不要用 bind mount！** Volume 是 Docker 官方推荐的生产数据持久化方案，原因有三：① 跨宿主机可移植 ② Docker 自动管理备份和恢复 ③ 安全（bind mount 可能绕过 SELinux 等权限控制）。

---

### Q11：容器中 Java 应用如何配置 JVM 参数？如何在容器内合理分配内存？
**面试官意图：** 考察 Java 容器化的内存管理经验，这是很多 Java 工程师初次上容器时容易踩的坑。

**完美解答：**

**容器内 JVM 内存配置：**

```dockerfile
# Dockerfile 中的 JVM 配置
FROM eclipse-temurin:17-jre-alpine

ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod

# 建议：通过环境变量传递 JVM 参数，不用硬编码
COPY target/app.jar app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

```yaml
# docker-compose.yml
services:
  app:
    image: myapp:1.0.0
    deploy:
      resources:
        limits:
          memory: 1g        # 容器总内存限制
          cpus: '0.5'
        reservations:
          memory: 512m
    environment:
      JAVA_OPTS: >
        -Xms512m                  # 初始堆大小
        -Xmx512m                  # 最大堆大小（关键！）
        -XX:+UseContainerSupport  # 容器感知（JDK 10+ 默认开启）
        -XX:MaxRAMPercentage=75.0 # 堆最大使用容器内存的 75%
        -XX:+PrintGCDetails
        -Djava.security.egd=file:/dev/./urandom  # 加速 Tomcat 启动
```

**Java 容器化内存分配黄金法则：**

```yaml
# 容器内存 1G 时的分配方案
deploy:
  resources:
    limits:
      memory: 1g

# JVM 堆大小：容器内存的 50%-75%
# -XX:MaxRAMPercentage=75.0 → JVM 堆最大使用 768MB（1G * 75%）
# 剩余 25% 用于：JVM 本身（元空间/栈）、操作系统缓存、其他进程
```

**常见错误和解决方案：**

| 错误配置 | 后果 | 正确配置 |
|----------|------|---------|
| `-Xmx1g` 但容器只有 1g 内存 | OOM（JVM + 系统内存 > 1g） | `-XX:MaxRAMPercentage=75.0` |
| 未设置 `-Xmx` | JVM 默认使用宿主机 1/4 内存，大容器超配 | 显式设置或使用 MaxRAMPercentage |
| 容器内存设太小 | OOM Kill（退出码 137） | 使用 `docker stats` 观察实际使用量后调整 |
| JDK 8 未开启 `UseContainerSupport` | JVM 不认识容器限制，使用宿主机内存 | 升级到 JDK 11+ 或加 `-XX:+UseContainerSupport` |

> 💡 **最安全的方式**：使用 JDK 17 + `-XX:MaxRAMPercentage=75.0`。JVM 会自动感知容器内存限制，不需要手动计算 `-Xmx`。但如果容器内存低于 256MB，手动设置 `-Xmx` 更精确。

---

### Q12：Docker 容器日志占用磁盘过多怎么办？如何管理日志？
**面试官意图：** 考察容器运维经验，日志管理是生产中常见的痛点。

**完美解答：**

**日志过大导致的问题：**
- 不限制日志 → 日志文件无限增长 → 磁盘爆满 → 服务不可用
- 默认 Docker 会将日志保存为 JSON 文件，不限制大小

**解决方案：**

**1. 全局限制（所有容器生效）：**
```bash
# 创建或修改 /etc/docker/daemon.json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}

# 重启 Docker 生效
systemctl restart docker
```

**2. 单容器限制：**
```yaml
services:
  app:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"   # 每个日志文件最大 10MB
        max-file: "3"      # 保留 3 个文件（共 30MB）
```

**3. 生产环境日志方案选择：**

| 方案 | 说明 | 优点 | 缺点 |
|------|------|------|------|
| **json-file + max-size** | Docker 自带，滚动日志 | 最简单 | 不便于集中查看 |
| **ELK + Filebeat** | 采集到 ES 统一存储 | 集中检索、可视化 | 架构复杂 |
| **syslog** | 发送到系统日志 | 标准化 | 功能有限 |
| **fluentd** | 日志采集和转发 | 灵活、插件丰富 | 需额外部署 |

**4. 紧急清理：**
```bash
# 查看日志占用大小
du -sh /var/lib/docker/containers/*/*-json.log

# 不停止容器清空日志（谨慎）
truncate -s 0 /var/lib/docker/containers/<container_id>/*-json.log

# 或者用 docker-compose 管理
docker compose logs --tail=100 app
```

> ⚠️ **务必在部署前就配置好日志上限！** 我见过因为日志没有限制，一周内写满 200GB 磁盘导致系统崩溃的案例。

---

## 💎 面试加分金句

- "Docker 的核心价值不在于容器本身，而在于构建-分发-部署的标准化流程——让应用在任何环境中都以相同方式运行。"
- "多阶段构建是 Java 容器化的必选项，它把镜像从 500MB 瘦身到 120MB 的同时还提升了安全性。"
- "容器不是虚拟机——一个容器只运行一个进程，不要在一个容器中同时跑 Java 和 Nginx。"
- "生产环境不要用 `:latest` 标签，始终使用语义化版本号（如 `1.2.3`）或 Git commit SHA。确保可追溯、可回滚。"
- "从 Docker Compose 到 K8s 的升级是一场架构变革，不是简单地换个编排工具——你需要投入至少 2 个月的学习成本。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| Docker 和虚拟机的区别？ | 共享宿主机内核 vs 独立内核，容器是进程级隔离 |
| docker run -d 和 -it 有什么区别？ | -d 后台运行，-it 交互式运行（保持终端打开） |
| 容器中 PID 1 进程有什么特殊？ | 负责处理 SIGTERM 信号，子进程回收 |
| 如何优雅停止容器？ | `docker stop` 发送 SIGTERM → 等待优雅退出 → 超时后 SIGKILL |
| Docker 的隔离性用什么实现？ | Namespace（隔离）+ Cgroups（资源限制）+ UnionFS（文件系统） |
| COPY 和 ADD 的区别？ | ADD 支持自动解压缩和 URL 下载，但行为不透明，推荐用 COPY |
| docker-compose 中 depends_on 一定等 MySQL 就绪吗？ | 不保证，需要 healthcheck |
| 如何 Debug 一个构建失败的 Dockerfile？ | 用 --target 停在某一层，通过中间产物排查 |

## 🔗 关联知识点

- [Redis必做项目清单-面试问答](#) — Redis 容器化部署
- [MySQL必做项目清单-面试问答](#) — MySQL 容器化主从复制
- [RabbitMQ必做项目清单-面试问答](#) — RabbitMQ 容器化部署
- [ElasticSearch必做项目清单-面试问答](#) — ELK 容器化部署
