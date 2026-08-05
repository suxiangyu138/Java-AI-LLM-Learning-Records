# 20-容器化部署-Docker与K8s
> 🎯 容器化是现代微服务部署的基石 — 掌握Docker镜像构建、容器编排、Kubernetes集群管理，实现从开发到生产的标准化交付流程

---

## 目录
1. [本章总览](#1-本章总览)
2. [Docker核心概念与基础](#2-docker核心概念与基础)
3. [Dockerfile最佳实践](#3-dockerfile最佳实践)
4. [Docker Compose编排微服务](#4-docker-compose编排微服务)
5. [Kubernetes核心概念](#5-kubernetes核心概念)
6. [Spring Boot on Kubernetes部署](#6-spring-boot-on-kubernetes部署)
7. [K8s健康检查与探针](#7-k8s健康检查与探针)
8. [高频踩坑与误区](#8-高频踩坑与误区)
9. [随堂基础练习](#9-随堂基础练习)
10. [章节综合实操案例](#10-章节综合实操案例)
11. [分层综合习题](#11-分层综合习题)
12. [本章复盘速记清单](#12-本章复盘速记清单)
13. [精通拓展补充-P2](#13-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位

- **归属**：Spring全家桶核心组件 → 层级2 P2精通拓展 → 微服务容器化部署
- **前置依赖**：Spring Boot（微服务构建基础）、Linux基础（文件系统/进程/网络概念）、微服务架构理论、基础的YAML/JSON语法
- **难度等级**：⭐⭐⭐⭐（涉及两个大型技术栈Docker+K8s，操作性强）
- **重要性**：⭐⭐⭐⭐⭐（容器化是现代后端开发的必备技能，面试高频考点）

### 1.2 为什么需要容器化部署

> 💡 环境不一致是软件交付最大的痛点。"在我电脑上能跑"不应该成为生产事故的借口。

| 问题 | 传统部署 | 容器化部署 |
|------|---------|-----------|
| **环境一致性** | 开发/测试/生产环境配置差异大 | 镜像打包完整运行环境，处处一致 |
| **资源隔离** | 多个应用共享同一OS进程，冲突风险高 | 每个容器独立进程空间和文件系统 |
| **启动速度** | 部署WAR包需重启服务器，分钟级 | 容器秒级启动，快速扩缩容 |
| **资源利用** | 虚拟机占用大量系统资源 | 共享宿主机内核，轻量级 |
| **灰度发布** | 手动操作，回滚困难 | 滚动更新、一键回滚 |
| **依赖管理** | 手工安装JDK/Tomcat版本 | 镜像锁定所有依赖版本 |

### 1.3 三层学习目标

| 级别 | 目标 |
|------|------|
| **基础** | 理解Docker镜像/容器/仓库三大概念，掌握常用docker命令，能编写Dockerfile构建Spring Boot镜像 |
| **熟练** | 掌握Docker Compose编排多服务，理解K8s Pod/Service/Deployment核心资源，能在K8s上部署Spring Boot应用 |
| **精通** | 深入理解镜像分层和缓存机制，能设计Docker多阶段构建和Jib无Dockerfile构建，掌握K8s健康检查/滚动更新/ConfigMap配置管理 |

### 1.4 学习路径图

```
Docker基础
  ├── 安装与命令 → docker pull/run/ps/exec
  ├── Dockerfile → 多阶段构建 / 最佳实践
  ├── Docker Compose → 多服务编排
  └── 镜像仓库 → 私有Registry / Harbor
      ↓
Kubernetes进阶
  ├── 核心概念 → Pod / Service / Deployment
  ├── 配置管理 → ConfigMap / Secret
  ├── 健康检查 → liveness / readiness / startup探针
  └── 高级话题 → Helm / HPA / Istio
```

---

## 2. Docker核心概念与基础

### 2.1 三大核心概念

| 概念 | 英文 | 定义 | 类比 |
|------|------|------|------|
| **镜像** | Image | 只读模板，包含运行应用所需的文件系统和配置 | 类（Class）— 定义但不运行 |
| **容器** | Container | 镜像的运行实例，可读可写层，独立进程空间 | 对象（Instance）— 具体运行中 |
| **仓库** | Registry | 存储和分发镜像的中心化服务 | Maven中央仓库 / Git仓库 |

> 🎯 **镜像=构建时产物，容器=运行时实例，仓库=分发渠道。**

### 2.2 Docker架构

```
┌─────────────────────────────────────────────────────┐
│                     Docker Client                    │
│              (docker pull/run/ps/build...)           │
└─────────────────────┬───────────────────────────────┘
                      │ REST API
                      ▼
┌─────────────────────────────────────────────────────┐
│                   Docker Daemon                       │
│    ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │
│    │  Image Mgmt  │  │ Container   │  │  Network   │  │
│    │  (build/tag/  │  │  (run/stop/ │  │  (bridge/  │  │
│    │   push)      │  │   exec)     │  │   overlay) │  │
│    └─────────────┘  └──────┬──────┘  └───────────┘  │
└────────────────────────────┼──────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
┌─────────────────────┐ ┌──────────┐ ┌──────────┐
│   Registry (Docker   │ │  Host OS  │ │  Volume   │
│    Hub / Harbor)    │ │  (Linux)  │ │  (持久化)  │
└─────────────────────┘ └──────────┘ └──────────┘
```

### 2.3 Docker常用命令速查表

| 命令 | 用途 | 示例 |
|------|------|------|
| `docker pull` | 从仓库拉取镜像 | `docker pull openjdk:17-jre-slim` |
| `docker images` | 查看本地镜像列表 | `docker images` |
| `docker rmi` | 删除镜像 | `docker rmi openjdk:17-jre-slim` |
| `docker run` | 创建并启动容器 | `docker run -d --name myapp -p 8080:8080 myapp:1.0` |
| `docker ps` | 查看运行中的容器 | `docker ps -a`（查看所有容器） |
| `docker stop` | 停止容器 | `docker stop myapp` |
| `docker rm` | 删除容器 | `docker rm -f myapp`（强制删除） |
| `docker logs` | 查看容器日志 | `docker logs -f myapp`（实时跟踪） |
| `docker exec` | 进入容器执行命令 | `docker exec -it myapp bash` |
| `docker inspect` | 查看容器详细信息 | `docker inspect myapp \| grep IPAddress` |
| `docker build` | 构建镜像 | `docker build -t myapp:1.0 .` |
| `docker tag` | 给镜像打标签 | `docker tag myapp:1.0 myregistry.com/myapp:1.0` |
| `docker push` | 推送镜像到仓库 | `docker push myregistry.com/myapp:1.0` |
| `docker commit` | 将容器保存为新镜像 | `docker commit myapp myapp:backup` |
| `docker cp` | 容器与宿主机间复制文件 | `docker cp myapp:/app/logs ./logs` |
| `docker network` | 管理容器网络 | `docker network create my-net` |
| `docker volume` | 管理数据卷 | `docker volume create data-vol` |
| `docker system prune` | 清理未被使用的资源 | `docker system prune -a` |
| `docker stats` | 查看容器资源使用 | `docker stats` |

### 2.4 docker run常用参数

| 参数 | 说明 | 示例 |
|------|------|------|
| `-d` | 后台运行 | `docker run -d nginx` |
| `--name` | 指定容器名称 | `docker run --name my-nginx nginx` |
| `-p` | 端口映射 宿主机:容器 | `docker run -p 8080:80 nginx` |
| `-v` | 挂载卷 | `docker run -v /host/data:/container/data nginx` |
| `-e` | 设置环境变量 | `docker run -e SPRING_PROFILES_ACTIVE=prod myapp` |
| `--network` | 指定网络 | `docker run --network my-net myapp` |
| `--restart` | 重启策略 | `docker run --restart=always nginx` |
| `--memory` | 内存限制 | `docker run --memory=512m myapp` |
| `--cpus` | CPU限制 | `docker run --cpus=1.5 myapp` |
| `--env-file` | 从文件加载环境变量 | `docker run --env-file .env myapp` |

### 2.5 镜像分层机制

```
┌──────────────────────────────────────────┐
│    可写层 (Container Layer) ← 运行时修改   │
├──────────────────────────────────────────┤
│  myapp:1.0 (APP JAR + dependencies)      │  ← 第4层
├──────────────────────────────────────────┤
│  openjdk:17-jre-slim                     │  ← 第3层
├──────────────────────────────────────────┤
│  debian:bullseye-slim                    │  ← 第2层
├──────────────────────────────────────────┤
│  scratch (Linux Kernel base)             │  ← 第1层
└──────────────────────────────────────────┘
```

> 💡 **分层机制**：每一层都是只读的。当修改文件时，Docker在可写层复制修改（Copy-on-Write）。多个镜像可以共享底层，节省磁盘空间。

**镜像分层的好处**：
- **节省存储**：多个基于openjdk:17的镜像共享同一层
- **加速传输**：拉取镜像时只下载未缓存的层
- **增量构建**：Dockerfile中只重新构建发生变化的层

**分层缓存策略**（影响Dockerfile编写顺序）：
```
# 错误的顺序：每次修改代码都要重新下载Maven依赖
FROM maven:3.8 AS build
COPY . /app           # 整个项目复制，任何文件变化都会使缓存失效
RUN mvn package       # 每次都重新下载依赖

# 正确的顺序：利用缓存层
FROM maven:3.8 AS build
COPY pom.xml /app/    # 单独复制pom.xml
RUN mvn dependency:go-offline  # 只下载依赖
COPY src /app/src/    # 再复制源码 -> 只有修改源码时才重新编译
RUN mvn package
```

### 2.6 容器生命周期

```
pull（拉取镜像）
  │
  ▼
create（docker create → 创建容器，状态：Created）
  │
  ▼
start（docker start → 启动容器，状态：Running）
  │
  ├── pause（暂停）←→ unpause（恢复）
  │
  ├── stop（发送SIGTERM，等待后SIGKILL → 状态：Exited(0)）
  │
  └── restart（先stop再start）
  │
  ▼
kill（直接发送SIGKILL → 状态：Exited(137)）
  │
  ▼
rm（彻底删除容器）
```

> ⚠️ `docker stop`先发送SIGTERM信号，默认等待10秒后再发送SIGKILL。Spring Boot应用应该捕获SIGTERM实现优雅关闭。

---

## 3. Dockerfile最佳实践

### 3.1 多阶段构建Dockerfile（Spring Boot应用）

```dockerfile
# ===== 第一阶段：构建阶段 =====
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

# 设置工作目录
WORKDIR /build

# 第一步：先复制pom文件并下载依赖（利用Docker缓存层）
# 这样做的好处：修改源码不会触发重新下载所有Maven依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 第二步：复制源码并打包
COPY src ./src
RUN mvn package -DskipTests -B

# 最终产物：/build/target/app.jar

# ===== 第二阶段：运行阶段 =====
FROM eclipse-temurin:17-jre-alpine AS runtime

# 添加非root用户（安全性最佳实践）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 从构建阶段复制JAR包
COPY --from=builder /build/target/*.jar app.jar

# 设置时区（解决常见时区问题）
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 切换为非root用户运行
USER appuser

# 暴露端口
EXPOSE 8080

# JVM参数优化（容器化环境下推荐使用-XX:+UseContainerSupport）
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 3.2 .dockerignore文件

> ⚠️ 不配置.dockerignore会导致构建上下文过大，甚至将敏感信息打包进镜像。

```dockerignore
# .dockerignore — 排除不需要进入构建上下文的文件
**/target/
**/.git/
**/.gitignore
**/.idea/
**/*.iml
**/.mvn/
**/mvnw
**/mvnw.cmd
**/node_modules/
**/log/
**/logs/
**/tmp/
**/*.log
**/.DS_Store
**/*.md
**/*.mdx
**/Dockerfile
**/docker-compose*.yml
**/docker-compose*.yaml
**/.env
**/README.*
**/LICENSE*
```

### 3.3 Dockerfile最佳实践清单

| 实践 | 说明 | 反例 |
|------|------|------|
| **多阶段构建** | 第一阶段编译打包、第二阶段只保留JRE和JAR | 直接用`FROM maven`跑JAR，把整个JDK和源码留在镜像中 |
| **.dockerignore** | 排除无关文件，减小构建上下文 | 不配置导致构建上下文包含target目录（几百MB） |
| **利用缓存层** | 将变化频率低的指令放在前面 | 先COPY全部代码再RUN mvn package（每次重新下载依赖） |
| **使用具体标签** | 明确指定基础镜像版本 | `FROM openjdk:latest`（构建不可重现） |
| **非root用户** | 降低安全风险 | `USER root`运行应用 |
| **最小基础镜像** | alpine/jre-slim减小攻击面 | `FROM openjdk:17-jdk`（包含完整JDK和编译器） |
| **合并RUN指令** | 减少分层数量 | 每个apt-get install单独RUN（增加层数） |
| **合理设置WORKDIR** | 避免使用绝对路径混乱 | `RUN cd /app && ...` |
| **HEALTHCHECK** | 定义健康检查，便于编排工具感知 | 不配置健康检查 |
| **环境变量** | 通过ENV暴露配置项 | 硬编码配置在Dockerfile中 |

### 3.4 合并RUN指令的层优化

```dockerfile
# ❌ 坏实践：每行一个RUN指令，生成多个层
RUN apk update
RUN apk add curl
RUN apk add tzdata
RUN rm -rf /var/cache/apk/*

# ✅ 好实践：合并RUN指令，只生成一层
RUN apk update && \
    apk add --no-cache curl tzdata && \
    rm -rf /var/cache/apk/*
```

### 3.5 Jib — 无Dockerfile构建方式

Jib是Google开发的一个Maven/Gradle插件，无需Dockerfile即可构建优化过的Docker镜像。

#### Maven配置

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>3.4.1</version>
    <configuration>
        <from>
            <!-- 基础镜像 -->
            <image>eclipse-temurin:17-jre-alpine</image>
        </from>
        <to>
            <!-- 目标镜像仓库 -->
            <image>registry.example.com/myapp:${project.version}</image>
            <auth>
                <username>${REGISTRY_USER}</username>
                <password>${REGISTRY_PASS}</password>
            </auth>
        </to>
        <container>
            <jvmFlags>
                <jvmFlag>-XX:+UseContainerSupport</jvmFlag>
                <jvmFlag>-XX:MaxRAMPercentage=75.0</jvmFlag>
                <jvmFlag>-Djava.security.egd=file:/dev/./urandom</jvmFlag>
            </jvmFlags>
            <mainClass>com.example.MyApplication</mainClass>
            <ports>
                <port>8080</port>
            </ports>
            <environment>
                <SPRING_PROFILES_ACTIVE>prod</SPRING_PROFILES_ACTIVE>
            </environment>
            <user>nobody:nobody</user>
            <creationTime>USE_CURRENT_TIMESTAMP</creationTime>
        </container>
        <allowInsecureRegistries>false</allowInsecureRegistries>
    </configuration>
</plugin>
```

#### Gradle配置

```groovy
plugins {
    id 'com.google.cloud.tools.jib' version '3.4.1'
}

jib {
    from {
        image = 'eclipse-temurin:17-jre-alpine'
    }
    to {
        image = "registry.example.com/myapp:${project.version}"
        auth {
            username = System.getenv('REGISTRY_USER')
            password = System.getenv('REGISTRY_PASS')
        }
    }
    container {
        jvmFlags = [
            '-XX:+UseContainerSupport',
            '-XX:MaxRAMPercentage=75.0',
            '-Djava.security.egd=file:/dev/./urandom'
        ]
        mainClass = 'com.example.MyApplication'
        ports = ['8080']
        environment = [SPRING_PROFILES_ACTIVE: 'prod']
        user = 'nobody:nobody'
        creationTime = 'USE_CURRENT_TIMESTAMP'
    }
}
```

#### 构建命令

```bash
# 本地构建并推送到Docker daemon
mvn jib:dockerBuild -Dimage=myapp:1.0

# 构建并推送到远程仓库
mvn jib:build -Dimage=registry.example.com/myapp:1.0

# Gradle版本
gradle jibDockerBuild --image=myapp:1.0
```

### 3.6 构建方式对比

| 维度 | Dockerfile | Jib | Buildpacks | Cloud Native Buildpacks |
|------|-----------|-----|-----------|------------------------|
| **是否需要Dockerfile** | 需要 | 不需要 | 不需要 | 不需要 |
| **是否需要Docker daemon** | 需要 | 不需要（直接构建为OCI镜像） | 需要 | 不需要 |
| **多阶段构建** | 原生支持 | 自动多阶段 | 自动 | 自动 |
| **分层优化** | 手动控制 | 自动（依赖层和资源层分离） | 自动 | 自动 |
| **构建速度** | 依赖缓存命中率 | 快（增量构建） | 中等 | 中等 |
| **可定制性** | 完全控制 | 较高（配置参数多） | 较低（依赖buildpack） | 中等 |
| **安全扫描** | 无 | 无 | 无 | 内置 |
| **适用场景** | 需要精细控制镜像 | Java项目快速容器化 | Heroku/PaaS风格 | 企业级标准化构建 |
| **Docker分离（CI友好）** | 需要Docker socket | 无需Docker完全CI友好 | 需要Docker | 无需Docker |

> 💡 **选择建议**：团队对Dockerfile熟悉选Dockerfile；追求构建速度和CI友好选Jib；企业级标准化选Cloud Native Buildpacks。

---

## 4. Docker Compose编排微服务

### 4.1 Docker Compose简介

Docker Compose通过一个YAML文件定义和运行多容器Docker应用。一个Compose文件可以定义多个服务、网络、卷，一次命令启动整个微服务系统。

```bash
# 安装Docker Compose
# Docker Desktop for Windows/Mac 已内置

# 查看版本
docker compose version
```

### 4.2 完整微服务docker-compose.yml

以下是一个包含MySQL 8.0、Redis 7、Nacos、Service-A、Service-B和Gateway的完整微服务系统编排：

```yaml
version: "3.8"

# ============================================
# 全局网络：所有服务通过自定义网络通信
# ============================================
networks:
  micro-network:
    driver: bridge

# ============================================
# 命名卷：数据持久化
# ============================================
volumes:
  mysql-data:
    driver: local
  nacos-data:
    driver: local
  nacos-logs:
    driver: local

# ============================================
# 服务定义
# ============================================
services:
  # ---- 基础设施层 ----

  # MySQL 8.0 数据库
  mysql:
    image: mysql:8.0
    container_name: micro-mysql
    restart: always
    networks:
      - micro-network
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-scripts:/docker-entrypoint-initdb.d  # 初始化SQL脚本
    environment:
      MYSQL_ROOT_PASSWORD: root123456
      MYSQL_DATABASE: micro_service
      MYSQL_USER: service_user
      MYSQL_PASSWORD: service_pass
      TZ: Asia/Shanghai
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-time-zone=+8:00
      - --max-connections=500
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot123456"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    deploy:
      resources:
        limits:
          memory: 1g
          cpus: "1.0"
        reservations:
          memory: 512m

  # Redis 7 缓存
  redis:
    image: redis:7-alpine
    container_name: micro-redis
    restart: always
    networks:
      - micro-network
    ports:
      - "6379:6379"
    volumes:
      - ./redis/redis.conf:/usr/local/etc/redis/redis.conf:ro
      - ./redis/data:/data
    command: redis-server /usr/local/etc/redis/redis.conf --appendonly yes
    environment:
      TZ: Asia/Shanghai
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
      start_period: 10s
    deploy:
      resources:
        limits:
          memory: 512m
          cpus: "0.5"

  # Nacos 注册中心与配置中心（standalone模式 + MySQL存储）
  nacos:
    image: nacos/nacos-server:2.3.0
    container_name: micro-nacos
    restart: always
    networks:
      - micro-network
    ports:
      - "8848:8848"
      - "9848:9848"  # gRPC端口
      - "9849:9849"  # gRPC端口
    volumes:
      - nacos-logs:/home/nacos/logs
    environment:
      MODE: standalone
      TZ: Asia/Shanghai
      # MySQL存储配置
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: mysql
      MYSQL_SERVICE_DB_NAME: nacos_config
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: root123456
      NACOS_AUTH_ENABLE: "true"
      NACOS_AUTH_TOKEN: SecretKey012345678901234567890123456789012345678901234567890123456789
      NACOS_AUTH_IDENTITY_KEY: nacos_server
      NACOS_AUTH_IDENTITY_VALUE: nacos_server
      # JVM参数
      JVM_XMS: 256m
      JVM_XMX: 512m
      JVM_XMN: 128m
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8848/nacos/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s
    deploy:
      resources:
        limits:
          memory: 1g
          cpus: "1.0"

  # ---- 业务服务层 ----

  # Service-A：用户服务
  service-a:
    image: service-a:1.0.0
    container_name: micro-service-a
    build:
      context: ./service-a
      dockerfile: Dockerfile
    restart: always
    networks:
      - micro-network
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      TZ: Asia/Shanghai
      # 服务发现配置
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
      SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR: nacos:8848
      # 数据源配置
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/micro_service?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: service_user
      SPRING_DATASOURCE_PASSWORD: service_pass
      # Redis配置
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      # JVM参数
      JAVA_OPTS: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
    depends_on:
      nacos:
        condition: service_healthy
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s
    deploy:
      resources:
        limits:
          memory: 512m
          cpus: "0.5"
        reservations:
          memory: 256m

  # Service-B：订单服务
  service-b:
    image: service-b:1.0.0
    container_name: micro-service-b
    build:
      context: ./service-b
      dockerfile: Dockerfile
    restart: always
    networks:
      - micro-network
    ports:
      - "8082:8082"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      TZ: Asia/Shanghai
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
      SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR: nacos:8848
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/micro_service?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: service_user
      SPRING_DATASOURCE_PASSWORD: service_pass
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      JAVA_OPTS: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
    depends_on:
      nacos:
        condition: service_healthy
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s
    deploy:
      resources:
        limits:
          memory: 512m
          cpus: "0.5"
        reservations:
          memory: 256m

  # ---- 网关层 ----

  # Spring Cloud Gateway
  gateway:
    image: service-gateway:1.0.0
    container_name: micro-gateway
    build:
      context: ./gateway
      dockerfile: Dockerfile
    restart: always
    networks:
      - micro-network
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      TZ: Asia/Shanghai
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
      SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR: nacos:8848
      JAVA_OPTS: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
    depends_on:
      nacos:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 45s
    deploy:
      resources:
        limits:
          memory: 512m
          cpus: "0.5"
        reservations:
          memory: 256m
```

### 4.3 网络模式对比

| 网络模式 | 说明 | 隔离性 | 跨宿主机通信 | 适用场景 |
|----------|------|--------|-------------|---------|
| **bridge** | 默认模式，容器通过虚拟网卡连接到docker0网桥 | 好 | 需端口映射 | 单机多容器通信 |
| **host** | 容器直接使用宿主机网络栈，无独立IP | 差 | 需宿主机网络配置 | 性能敏感的容器（如Nginx） |
| **overlay** | 跨宿主机覆盖网络，需要Swarm/K8s | 好 | 原生支持 | 多机集群部署 |
| **none** | 无网络，仅loopback | 完全隔离 | 不通信 | 安全敏感场景 |
| **macvlan** | 容器有独立MAC地址，像物理机一样接入网络 | 中等 | 需物理网络支持 | 遗留应用网络兼容 |

> 💡 在Docker Compose中不指定network时，默认创建bridge网络。生产环境推荐自定义bridge网络，因为：
> - 自动DNS解析（可以通过服务名称互相访问）
> - 更好的隔离性
> - 可以连接多个容器而不对外暴露端口

### 4.4 depends_on vs healthcheck对比

| 特性 | depends_on | healthcheck |
|------|-----------|-------------|
| **作用** | 控制服务启动顺序 | 检测服务是否健康运行 |
| **检查级别** | 容器启动状态 | 容器内应用状态 |
| `depends_on` 不加condition | 只检查容器是否已启动（已创建进程） | 不检查应用是否就绪 |
| `depends_on` + `condition: service_healthy` | 等待健康检查通过后才启动依赖服务 | 会持续检查直到状态变为healthy |
| **典型场景** | 先启动数据库再启动应用 | 确保应用真正就绪了才接收请求 |

> ⚠️ **常见误区**：只写`depends_on: - mysql`而不加`condition: service_healthy`，会导致应用在MySQL尚未就绪时就启动，出现数据库连接失败。

### 4.5 .env文件与环境变量管理

```bash
# .env — Docker Compose环境变量文件
# 该文件与docker-compose.yml同级，自动加载

# Docker配置
COMPOSE_PROJECT_NAME=micro-platform

# 镜像版本
IMAGE_TAG=1.0.0

# 基础设施配置
MYSQL_ROOT_PASSWORD=root123456
MYSQL_DATABASE=micro_service
MYSQL_USER=service_user
MYSQL_PASSWORD=service_pass

# Redis配置
REDIS_PASSWORD=

# Nacos配置
NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789

# 时区
TZ=Asia/Shanghai

# 资源限制
MEM_LIMIT_DB=1g
MEM_LIMIT_APP=512m
MEM_LIMIT_GATEWAY=512m
CPU_LIMIT_DB=1.0
CPU_LIMIT_APP=0.5
```

在docker-compose.yml中使用变量替换：

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}

  service-a:
    deploy:
      resources:
        limits:
          memory: ${MEM_LIMIT_APP}
          cpus: ${CPU_LIMIT_APP}
```

### 4.6 docker compose常用命令

| 命令 | 用途 | 示例 |
|------|------|------|
| `docker compose up` | 启动所有服务 | `docker compose up -d`（后台运行） |
| `docker compose down` | 停止并移除所有服务 | `docker compose down -v`（同时删除卷） |
| `docker compose ps` | 查看服务状态 | `docker compose ps` |
| `docker compose logs` | 查看服务日志 | `docker compose logs -f service-a`（实时追踪） |
| `docker compose build` | 重新构建镜像 | `docker compose build service-a` |
| `docker compose restart` | 重启服务 | `docker compose restart service-a` |
| `docker compose exec` | 进入容器执行命令 | `docker compose exec service-a bash` |
| `docker compose stop` | 停止服务 | `docker compose stop service-a` |
| `docker compose start` | 启动已停止的服务 | `docker compose start service-a` |
| `docker compose pull` | 拉取最新镜像 | `docker compose pull mysql` |
| `docker compose config` | 验证并查看合并后的配置 | `docker compose config` |
| `docker compose top` | 查看各服务运行的进程 | `docker compose top` |
| `docker compose images` | 查看各服务使用的镜像 | `docker compose images` |

---

## 5. Kubernetes核心概念

### 5.1 K8s架构总览

```
┌──────────────────────────────────────────────────────────────┐
│                      Control Plane (Master)                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────┐   │
│  │  API Server │  │ Scheduler │  │Controller│  │  etcd      │   │
│  │  (kube-    │  │ (kube-   │  │ Manager  │  │ (分布式键值│   │
│  │   apiserver)│  │  scheduler)│  │          │  │  存储)    │   │
│  └─────┬─────┘  └─────┬────┘  └─────┬────┘  └────────────┘   │
│        │               │             │                         │
└────────┼───────────────┼─────────────┼─────────────────────────┘
         │               │             │
    ┌────┴───────────────┴─────────────┴────────────────────────┐
    │                   Node (Worker) 1                         │
    │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
    │  │ kubelet  │  │ kube-    │  │  Pod A   │  │  Pod B   │   │
    │  │          │  │ proxy    │  │ (container)│  │ (container)│  │
    │  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
    └───────────────────────────────────────────────────────────┘
    ┌───────────────────────────────────────────────────────────┐
    │                   Node (Worker) 2                         │
    │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
    │  │ kubelet  │  │ kube-    │  │  Pod C   │  │  Pod D   │   │
    │  │          │  │ proxy    │  │ (container)│  │ (container)│  │
    │  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
    └───────────────────────────────────────────────────────────┘
```

### 5.2 核心资源对象

| 资源 | 英文 | 定义 | 类比（Docker生态） |
|------|------|------|-------------------|
| **Pod** | Pod | 最小调度单元，包含一个或多个容器，共享网络和存储 | 一组共享网络的容器 |
| **Deployment** | Deployment | 声明式管理Pod的副本、更新策略、回滚 | docker-compose中的service |
| **Service** | Service | 将一组Pod暴露为网络服务，提供负载均衡和稳定IP | Docker Compose的network + 端口映射 |
| **ConfigMap** | ConfigMap | 存储非敏感配置数据，注入到Pod的环境变量或文件 | .env文件 |
| **Secret** | Secret | 存储敏感数据（密码、密钥），Base64编码，etcd加密 | Docker secrets |
| **Ingress** | Ingress | 七层HTTP/HTTPS路由，将外部请求转发到内部Service | Nginx反向代理 |
| **Namespace** | Namespace | 虚拟集群隔离，多环境/多租户隔离 | Docker Compose的project |

### 5.3 Docker Compose vs K8s概念映射

| Docker Compose | Kubernetes | 说明 |
|---------------|------------|------|
| service | Deployment | 定义应用的部署策略 |
| container | Pod中的container | 运行的具体容器 |
| network | Service + NetworkPolicy | 网络通信与策略 |
| volume | PersistentVolume + PersistentVolumeClaim | 数据持久化 |
| depends_on | initContainers + startupProbe | 启动依赖管理 |
| environment | ConfigMap + Secret | 环境变量注入 |
| build | Dockerfile + 镜像仓库 | 镜像构建流程 |
| ports | Service (NodePort/LoadBalancer) + Ingress | 端口暴露 |
| restart: always | Deployment的restartPolicy | 容器重启策略 |
| env_file | ConfigMap (从文件创建) | 批量环境变量 |
| healthcheck | livenessProbe + readinessProbe | 健康检查 |
| deploy.resources | resources.requests/limits | 资源限制 |

### 5.4 Namespace的重要性

```bash
# 查看所有namespace
kubectl get namespaces

# 常见namespace
kubectl get ns
NAME              STATUS   AGE
default           Active   30d     # 默认命名空间（未指定时使用）
kube-system       Active   30d     # K8s系统组件
kube-public       Active   30d     # 公共资源，所有用户可读
kube-node-lease   Active   30d     # 节点心跳
```

> 💡 **Namespace最佳实践**：
> - 环境隔离：`dev` / `staging` / `prod` 分属不同Namespace
> - 业务隔离：每个业务线使用独立Namespace
> - 权限绑定：RBAC绑定到Namespace级别
> - 资源配额：通过ResourceQuota限制每个Namespace的资源使用

```bash
# 创建namespace
kubectl create namespace micro-dev

# 在指定namespace中操作资源
kubectl get pods -n micro-dev
kubectl get all -n micro-dev

# 设置默认namespace（避免每次加-n）
kubectl config set-context --current --namespace=micro-dev

# 删除namespace（会级联删除其下所有资源）
kubectl delete namespace micro-dev
```

---

## 6. Spring Boot on Kubernetes部署

### 6.1 完整部署YAML示例

以下是一个完整的Spring Boot应用在K8s上部署所需的全部YAML资源。

#### 6.1.1 ConfigMap — 应用配置

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: service-a-config
  namespace: micro-dev
  labels:
    app: service-a
data:
  # application.yml 作为整体配置注入
  application.yml: |
    server:
      port: 8081

    spring:
      application:
        name: service-a
      datasource:
        url: jdbc:mysql://mysql-service:3306/micro_service?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
        username: service_user
        password: ${DB_PASSWORD}  # 从Secret引用
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
          idle-timeout: 300000
          connection-timeout: 20000
      redis:
        host: redis-service
        port: 6379
        timeout: 3000ms
        lettuce:
          pool:
            max-active: 16
            max-idle: 8
            min-idle: 4

    # 服务发现
    spring.cloud.nacos:
      discovery:
        server-addr: nacos-service:8848
      config:
        server-addr: nacos-service:8848
        file-extension: yaml

    # 健康检查端点
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          show-details: always
          probes:
            enabled: true  # 启用K8s探针支持（Spring Boot 2.3+）
      metrics:
        tags:
          application: ${spring.application.name}

    # 优雅关闭
    server.shutdown: graceful
    spring.lifecycle.timeout-per-shutdown-phase: 30s

  # 单独的配置项（也可通过环境变量引用）
  app.properties: |
    cache.ttl=600
    cache.max-size=10000
    app.version=1.0.0
```

#### 6.1.2 Secret — 敏感数据

```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: service-a-secret
  namespace: micro-dev
  labels:
    app: service-a
type: Opaque
data:
  # Base64编码的值（echo -n "service_pass" | base64）
  DB_PASSWORD: c2VydmljZV9wYXNz
  # echo -n "redis_pass" | base64
  REDIS_PASSWORD: cmVkaXNfcGFzcw==
  # echo -n "jwt_secret_key_123456" | base64
  JWT_SECRET: and0X3NlY3JldF9rZXlfMTIzNDU2

---
# 通过kubectl命令式创建更安全
# kubectl create secret generic service-a-secret \
#   --from-literal=DB_PASSWORD=service_pass \
#   --from-literal=REDIS_PASSWORD=redis_pass \
#   --from-literal=JWT_SECRET=jwt_secret_key_123456 \
#   -n micro-dev
```

> ⚠️ Secret的Base64编码不是加密！只是编码。生产环境应启用etcd加密（EncryptionConfiguration）或使用外部密钥管理（Vault、AWS Secrets Manager）。

#### 6.1.3 Deployment — 应用部署

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: service-a
  namespace: micro-dev
  labels:
    app: service-a
    version: v1
spec:
  # 副本数
  replicas: 3

  # 滚动更新策略
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # 更新时最多可以多出1个Pod
      maxUnavailable: 0  # 更新时不允许有Pod不可用（保证100%可用）

  # Pod选择器
  selector:
    matchLabels:
      app: service-a

  # Pod模板
  template:
    metadata:
      labels:
        app: service-a
        version: v1
    spec:
      # 优雅关闭宽限期
      terminationGracePeriodSeconds: 60

      # 容器定义
      containers:
        - name: service-a
          image: registry.example.com/service-a:1.0.0
          imagePullPolicy: IfNotPresent

          # 暴露端口
          ports:
            - containerPort: 8081
              name: http
            - containerPort: 8081
              name: actuator

          # 环境变量（从ConfigMap和Secret注入）
          env:
            # 从ConfigMap中获取特定key
            - name: APP_CACHE_TTL
              valueFrom:
                configMapKeyRef:
                  name: service-a-config
                  key: app.properties

            # 从Secret中获取密码
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: service-a-secret
                  key: DB_PASSWORD
            - name: REDIS_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: service-a-secret
                  key: REDIS_PASSWORD
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: service-a-secret
                  key: JWT_SECRET

            # 直接设置环境变量
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: TZ
              value: "Asia/Shanghai"

          # 挂载ConfigMap作为文件
          volumeMounts:
            - name: config-volume
              mountPath: /config/
              readOnly: true

          # 资源限制
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"

          # 健康检查（详见第7章）
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            periodSeconds: 5
            failureThreshold: 2
            successThreshold: 1

      # 卷定义
      volumes:
        - name: config-volume
          configMap:
            name: service-a-config
            items:
              - key: application.yml
                path: application.yml

      # 镜像拉取密钥（私有仓库）
      # imagePullSecrets:
      #   - name: registry-credentials
```

#### 6.1.4 Service — 服务暴露

```yaml
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: service-a
  namespace: micro-dev
  labels:
    app: service-a
spec:
  type: ClusterIP  # 集群内部访问
  selector:
    app: service-a
  ports:
    - name: http
      protocol: TCP
      port: 80          # Service端口
      targetPort: 8081  # 容器端口
```

#### 6.1.5 Ingress — 外部路由

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: micro-ingress
  namespace: micro-dev
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
spec:
  ingressClassName: nginx
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /api/user
            pathType: Prefix
            backend:
              service:
                name: service-a
                port:
                  number: 80
          - path: /api/order
            pathType: Prefix
            backend:
              service:
                name: service-b
                port:
                  number: 80
          - path: /
            pathType: Prefix
            backend:
              service:
                name: gateway
                port:
                  number: 80
  # TLS配置
  tls:
    - hosts:
        - api.example.com
      secretName: example-tls
```

### 6.2 kubectl常用命令

| 命令 | 用途 | 示例 |
|------|------|------|
| `kubectl apply` | 创建/更新资源 | `kubectl apply -f deployment.yaml` |
| `kubectl get` | 查看资源列表 | `kubectl get pods -n micro-dev -w`（实时监听） |
| `kubectl describe` | 查看资源详情 | `kubectl describe pod service-a-xxxxx` |
| `kubectl logs` | 查看Pod日志 | `kubectl logs -f deployment/service-a -c service-a` |
| `kubectl delete` | 删除资源 | `kubectl delete -f deployment.yaml` |
| `kubectl rollout` | 管理滚动更新 | `kubectl rollout status deployment/service-a` |
| `kubectl exec` | 在容器中执行命令 | `kubectl exec -it pod/service-a-xxxxx -- sh` |
| `kubectl port-forward` | 本地端口转发到Pod | `kubectl port-forward svc/service-a 8080:80` |
| `kubectl top` | 查看资源使用 | `kubectl top pod -n micro-dev` |
| `kubectl explain` | 查看资源字段说明 | `kubectl explain deployment.spec` |

```bash
# 完整部署流程
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f ingress.yaml

# 查看部署状态
kubectl rollout status deployment/service-a -n micro-dev

# 查看所有资源
kubectl get all -n micro-dev

# 实时查看Pod事件
kubectl get events -n micro-dev --watch

# 进入Pod调试
kubectl exec -it deployment/service-a -n micro-dev -- sh

# 查看Pod日志
kubectl logs -f -l app=service-a -n micro-dev

# 端口转发（本地调试）
kubectl port-forward svc/service-a 8081:80 -n micro-dev
```

### 6.3 滚动更新策略

```yaml
# 滚动更新是Deployment默认的更新策略
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 25%        # 更新时最多超出期望副本数的比例（向上取整）
      maxUnavailable: 25%  # 更新时最多允许多少比例Pod不可用（向下取整）
```

**滚动更新流程**（replicas=3, maxSurge=1, maxUnavailable=0）：

```
Step 1: 创建新Pod v2 (pending)
  v1(OK)  v1(OK)  v1(OK)  v2(creating)

Step 2: 新Pod v2就绪，删除一个v1
  v1(OK)  v1(OK)  v2(OK)

Step 3: 创建第二个v2
  v1(OK)  v1(OK)  v2(OK)  v2(creating)

Step 4: 第二个v2就绪，删除第二个v1
  v1(OK)  v2(OK)  v2(OK)

Step 5: 创建第三个v2
  v2(OK)  v2(OK)  v2(creating)

Step 6: 所有v2就绪，更新完成
  v2(OK)  v2(OK)  v2(OK)
```

```bash
# 触发滚动更新（更新镜像）
kubectl set image deployment/service-a service-a=registry.example.com/service-a:1.1.0 -n micro-dev

# 或者更新YAML后重新apply
kubectl apply -f deployment.yaml

# 查看更新状态
kubectl rollout status deployment/service-a -n micro-dev

# 查看更新历史
kubectl rollout history deployment/service-a -n micro-dev

# 回滚到上一个版本
kubectl rollout undo deployment/service-a -n micro-dev

# 回滚到指定版本
kubectl rollout undo deployment/service-a --to-revision=2 -n micro-dev

# 暂停/继续滚动更新
kubectl rollout pause deployment/service-a -n micro-dev
kubectl rollout resume deployment/service-a -n micro-dev
```

### 6.4 资源Requests和Limits

> 💡 正确设置资源限制是Spring Boot在K8s上稳定运行的关键。不设置资源限制可能导致节点OOM，Pod被驱逐。

```yaml
resources:
  requests:
    memory: "256Mi"    # 保证至少256MB内存
    cpu: "250m"        # 保证至少0.25核CPU（250 milliCPU）
  limits:
    memory: "512Mi"    # 最多使用512MB内存，超出会被OOMKill
    cpu: "500m"        # 最多使用0.5核CPU
```

| JVM参数 | 说明 | 推荐值 |
|---------|------|--------|
| `-XX:+UseContainerSupport` | 感知容器内存限制（JDK 11+默认） | 开启 |
| `-XX:MaxRAMPercentage=75.0` | JVM使用容器限制内存的75% | 75.0 |
| `-XX:InitialRAMPercentage=50.0` | JVM初始堆大小 | 50.0 |
| `-XX:+UseZGC` | 低延迟GC（大内存、低延迟场景） | 4G以上内存时考虑 |

```yaml
# Spring Boot JVM参数配置（通过JAVA_TOOL_OPTIONS环境变量）
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
        - name: service-a
          env:
            - name: JAVA_TOOL_OPTIONS
              value: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseZGC -XX:+ExitOnOutOfMemoryError"
```

> ⚠️ **关键注意事项**：
> - Pod的memory limit必须大于等于JVM最大堆内存 + 元空间 + 线程栈 + 其他开销
> - 建议Pod内存limit = JVM MaxHeap / 0.75（预留25%给JVM其他开销和OS）
> - 未设置`-XX:+UseContainerSupport`时，JVM会看到宿主机全部内存而不是容器限制

---

## 7. K8s健康检查与探针

### 7.1 三种探针对比

| 探针类型 | 英文 | 用途 | 失败后行为 | 适用场景 |
|----------|------|------|-----------|---------|
| **存活探针** | livenessProbe | 检测容器是否存活（是否卡死/死锁） | 重启容器 | 应用死锁、内存泄漏导致无响应 |
| **就绪探针** | readinessProbe | 检测容器是否准备好接收流量 | 从Service端点中移除 | 启动加载、缓存预热、依赖未就绪 |
| **启动探针** | startupProbe | 检测容器是否已完成启动 | 重启容器（慢启动保护） | 启动慢的应用（避免liveness误杀） |

> 🎯 **三者关系**：startupProbe先运行，成功后移交livenessProbe + readinessProbe。startupProbe保护慢启动应用不被livenessProbe误杀。

### 7.2 Spring Boot Actuator健康端点配置

#### Spring Boot 2.3+ 的K8s探针支持

Spring Boot从2.3版本开始专门提供了对K8s探针的支持，使用独立的健康端点：

```yaml
# application.yml — 启用K8s探针支持
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true   # 启用K8s探针组（Spring Boot 2.3+）
  health:
    readinessstate:
      enabled: true     # 启用就绪状态
    livenessstate:
      enabled: true     # 启用存活状态
```

启用后，Actuator提供以下端点：

| 端点路径 | 用途 | 映射到探针 |
|----------|------|-----------|
| `/actuator/health/liveness` | 存活状态 | livenessProbe |
| `/actuator/health/readiness` | 就绪状态 | readinessProbe |

#### Spring Boot 2.3之前版本（兼容方式）

```yaml
# 低版本Spring Boot只能使用通用health端点
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: always
```

> ⚠️ Spring Boot 2.3之前使用`/actuator/health`作为单一健康端点。所有K8s探针指向同一个路径，无法区分存活和就绪。

### 7.3 完整探针配置示例

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
        - name: service-a
          # ... 其他配置 ...

          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            # 启动探针适用于启动慢的应用
            initialDelaySeconds: 5    # 容器启动后5秒开始探测
            periodSeconds: 5           # 每5秒探测一次
            timeoutSeconds: 3          # 超时时间3秒
            failureThreshold: 30       # 允许失败30次（最多150秒等待启动）
            successThreshold: 1        # 成功1次即认为启动完成

          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            # 应用运行后持续检测
            initialDelaySeconds: 10    # 启动后10秒开始
            periodSeconds: 15          # 每15秒探测一次
            timeoutSeconds: 5
            failureThreshold: 3        # 连续3次失败则重启容器
            successThreshold: 1

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            # 就绪检测影响流量路由
            initialDelaySeconds: 5
            periodSeconds: 10
            timeoutSeconds: 3
            failureThreshold: 2        # 连续2次失败则从Service中移除
            successThreshold: 1
```

### 7.4 三种探针方式对比

| 探针方式 | 工作原理 | 适用场景 | 优点 | 缺点 |
|----------|---------|---------|------|------|
| **HTTP探针** | 请求指定的HTTP端点，2xx/3xx表示健康 | Web应用 | 最直观、最常用 | 需要HTTP端口可达 |
| **TCP探针** | 尝试建立TCP连接，成功表示健康 | 非HTTP服务（数据库、消息队列） | 简单、低开销 | 无法检测应用层状态 |
| **Exec探针** | 在容器内执行命令，退出码0表示健康 | 特定检查脚本 | 灵活、可自定义 | 额外进程开销 |

```yaml
# HTTP探针（最常用，适用于Spring Boot应用）
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
    httpHeaders:
      - name: X-Custom-Header
        value: health-check

# TCP探针（适用于Redis、MySQL等中间件）
readinessProbe:
  tcpSocket:
    port: 6379
  initialDelaySeconds: 5
  periodSeconds: 10

# Exec探针（自定义检查逻辑）
livenessProbe:
  exec:
    command:
      - sh
      - -c
      - "curl -s http://localhost:8081/actuator/health | grep -q '\"status\":\"UP\"'"
  periodSeconds: 15
```

### 7.5 探针参数调优建议

| 参数 | 说明 | 推荐值 | 调整策略 |
|------|------|--------|---------|
| `initialDelaySeconds` | 容器启动后延迟探测时间 | 10-30s | 启动慢的应用适当增大 |
| `periodSeconds` | 探测间隔 | 10-30s | 频繁探测增加负载，可适当调大 |
| `timeoutSeconds` | 单次探测超时 | 3-5s | 网络慢时适当增大 |
| `failureThreshold` | 失败阈值 | liveness:3, readiness:2 | liveness必须大于1防止误杀 |
| `successThreshold` | 成功阈值 | 默认1 | 一般保持默认 |

### 7.6 优雅关闭（Graceful Shutdown）

Spring Boot应用应该优雅地处理K8s发起的Pod终止信号，确保正在处理的请求完成后再关闭。

```yaml
# application.yml — 优雅关闭配置
server:
  shutdown: graceful                   # 优雅关闭（等待请求处理完）

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s    # 优雅关闭最大等待时间（超过则强制关闭）
```

K8s侧配置：

```yaml
spec:
  # Pod模板中设置优雅关闭宽限期（必须大于spring.lifecycle.timeout-per-shutdown-phase）
  template:
    spec:
      terminationGracePeriodSeconds: 60  # 给Pod 60秒完成优雅关闭
```

**Pod关闭流程**：

```
1. kubectl delete pod / 滚动更新触发Pod删除
2. K8s将Pod状态标记为Terminating
3. 从Service端点列表中移除Pod（不再接收新流量）
4. 向Pod内主进程发送SIGTERM信号
5. Spring Boot Actuator捕获SIGTERM
6. Spring Boot执行优雅关闭：
   a. 拒绝新请求（返回503）
   b. 等待正在处理的请求完成（最多30秒）
7. terminationGracePeriodSeconds到期后，发送SIGKILL强制终止
```

> ⚠️ `terminationGracePeriodSeconds`必须大于`spring.lifecycle.timeout-per-shutdown-phase`，否则Spring Boot会被K8s强制杀死。

### 7.7 PreStop钩子

如果应用不支持SIGTERM优雅关闭，可以使用PreStop Hook执行自定义关闭逻辑：

```yaml
spec:
  template:
    spec:
      containers:
        - name: service-a
          lifecycle:
            preStop:
              exec:
                command:
                  - sh
                  - -c
                  - "curl -X POST http://localhost:8081/actuator/shutdown && sleep 10"
```

---

## 8. 高频踩坑与误区

### 8.1 常见问题速查表

| 问题 | 现象 | 根本原因 | 解决方案 |
|------|------|---------|---------|
| **Docker镜像构建失败** | `Error: Unable to access jarfile app.jar` | 多阶段构建时JAR路径不匹配 | 确认`COPY --from=builder`的路径正确，使用`ls -la`调试 |
| **容器时区不对** | 日志时间比系统时间早8小时 | Docker基础镜像默认UTC时间 | 安装tzdata并设置Asia/Shanghai |
| **Docker磁盘占满** | `No space left on device` | 未清理旧镜像和未使用的容器 | `docker system prune -a`定期清理 |
| **Compose网络DNS解析失败** | `Name or service not known` | 容器不在同一自定义网络，或depends_on配置错误 | 所有服务加入同一个network，使用service_healthy |
| **Pod CrashLoopBackOff** | Pod反复重启 | 启动失败或liveness探针配置不当 | `kubectl describe pod`查看事件，`kubectl logs`查看日志 |
| **ImagePullBackOff** | 拉取镜像失败 | 镜像不存在、标签错误、私有仓库未认证 | 检查镜像名和tag，配置imagePullSecrets |
| **OOMKilled** | Pod被终止，状态OOMKilled | 内存limit太小 | 增加memory limit或优化JVM内存配置 |
| **Pod一直Pending** | 调度不成功 | 资源不足、PVC未绑定、NodeSelector不匹配 | `kubectl describe pod`查看事件，检查节点资源 |
| **Service无法访问** | curl超时或连接拒绝 | Service selector不匹配Pod label | 检查selector和Pod的labels是否一致 |
| **滚动更新中断** | 新Pod就绪不了 | readinessProbe失败 | 检查应用启动日志，调整readinessProbe配置 |
| **Secret泄露** | 代码仓库中出现明文密码 | Secret作为YAML提交到了Git | 使用`.gitignore`排除，使用外部密钥管理 |
| **NodePort端口冲突** | Service创建失败 | 端口已被占用 | 使用随机NodePort（不指定port） |

### 8.2 Docker磁盘清理

```bash
# 查看磁盘使用情况
docker system df

# 清理未使用的容器、网络、镜像、构建缓存
docker system prune

# 更彻底——清理所有未被使用的镜像（包括无标签镜像）
docker system prune -a --volumes

# 手动定期清理
cat /etc/cron.daily/docker-clean
#!/bin/bash
docker system prune -af --filter "until=24h"
```

### 8.3 容器时区问题

```dockerfile
# 方案一：直接在Dockerfile中设置（推荐）
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 方案二：通过环境变量（适用于Debian/Ubuntu基础镜像）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 方案三：运行时环境变量（临时）
docker run -e TZ=Asia/Shanghai myapp
```

### 8.4 Docker Compose网络DNS问题

> ⚠️ Docker Compose默认创建的bridge网络提供自动DNS解析。但以下情况会导致DNS解析失败：

```yaml
# ❌ 错误：服务不在同一网络
services:
  service-a:
    networks:
      - net-a
  service-b:
    networks:
      - net-b    # service-a无法通过service-b名称访问service-b

networks:
  net-a:
  net-b:

# ✅ 正确：所有需要通信的服务在同一网络
services:
  service-a:
    networks:
      - micro-network
  service-b:
    networks:
      - micro-network

networks:
  micro-network:
    driver: bridge
```

### 8.5 Pod CrashLoopBackOff排查

```bash
# 步骤1：查看Pod状态
kubectl get pods -n micro-dev

# 步骤2：查看Pod详情（关注Events部分）
kubectl describe pod service-a-6b8f4d7c9d-xxxxx -n micro-dev

# 步骤3：查看应用日志
kubectl logs service-a-6b8f4d7c9d-xxxxx -n micro-dev

# 步骤4：如果有多容器，指定容器
kubectl logs service-a-6b8f4d7c9d-xxxxx -c service-a -n micro-dev

# 步骤5：查看前一次崩溃的日志
kubectl logs service-a-6b8f4d7c9d-xxxxx -p -n micro-dev
```

**常见CrashLoopBackOff原因排查**：

| 日志特征 | 可能原因 | 解决 |
|----------|---------|------|
| `Unable to access jarfile` | JAR路径不对 | 检查Dockerfile中JAR路径 |
| `No datasource found` | 数据库连接失败 | 检查ConfigMap中数据库地址 |
| `Failed to bind to port` | 端口被占用 | 检查是否重复暴露相同端口 |
| `OutOfMemoryError` | 内存不足 | 增大memory limit或优化JVM配置 |
| `UnknownHostException` | DNS解析失败 | 检查Service名称是否正确 |
| `UnsatisfiedDependency` | 依赖组件未就绪 | 检查依赖服务是否正常启动 |

### 8.6 OOMKilled问题

```yaml
# 内存不足导致Pod被终止是K8s上Spring Boot最常见的故障之一

# 诊断：查看Pod内存使用
kubectl top pod -n micro-dev

# 查看Pod终止原因
kubectl describe pod service-a-xxxxx -n micro-dev
# 输出中包含：
# State:          Running
# Last State:     Terminated
#   Reason:       OOMKilled
#   Exit Code:    137

# 解决方案一：增大内存limit
resources:
  limits:
    memory: "1Gi"    # 从512Mi增大到1Gi

# 解决方案二：优化JVM内存
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0"
  # 确保Pod memory limit >= JVM MaxHeap / 0.70

# 解决方案三：减少JVM元空间和线程栈
# -XX:MaxMetaspaceSize=128m -Xss256k
```

### 8.7 ImagePullBackOff排查

```bash
# 步骤1：查看Pod状态
kubectl describe pod service-a-xxxxx -n micro-dev
# Events:
#   Failed to pull image "registry.example.com/service-a:1.0.0": rpc error: ...
#   Back-off pulling image "registry.example.com/service-a:1.0.0"

# 步骤2：常见原因及解决

# 原因1：镜像标签不对 — 确认镜像名和tag
kubectl set image deployment/service-a service-a=registry.example.com/service-a:1.0.0 -n micro-dev

# 原因2：私有仓库未认证 — 创建imagePullSecrets
kubectl create secret docker-registry registry-credentials \
  --docker-server=registry.example.com \
  --docker-username=admin \
  --docker-password=password \
  -n micro-dev

# 将Secret关联到Pod
spec:
  template:
    spec:
      imagePullSecrets:
        - name: registry-credentials

# 原因3：本地未构建镜像 — 手动拉取测试
docker pull registry.example.com/service-a:1.0.0
```

---

## 9. 随堂基础练习

### 练习1：编写Dockerfile

为以下Spring Boot应用编写一个多阶段构建的Dockerfile：
- 使用Maven 3.9 + JDK 17进行编译
- 使用JDK 17 JRE Alpine作为运行环境
- 应用端口为8080
- JVM参数配置 UseContainerSupport + MaxRAMPercentage=75.0
- 设置时区为 Asia/Shanghai
- 添加健康检查（Actuator health端点）
- 使用非root用户运行

### 练习2：Docker Compose编排

编写一个docker-compose.yml，包含以下服务：
- PostgreSQL 15 数据库
- Redis 7
- 一个Spring Boot应用（依赖于PostgreSQL和Redis）
- 使用`.env`文件管理环境变量

要求：
- 所有服务在同一个bridge网络
- 使用命名卷持久化数据
- 配置healthcheck
- 设置资源限制

### 练习3：K8s Deployment转写

将下面docker-compose中的service定义转写为K8s Deployment和Service YAML：

```yaml
services:
  user-service:
    image: user-service:1.0
    ports:
      - "8081:8081"
    environment:
      DB_URL: jdbc:postgresql://db:5432/users
      DB_USERNAME: app_user
      DB_PASSWORD: app_pass_123
    depends_on:
      - db
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
```

### 练习4：探针配置分析

以下是一个Spring Boot应用的探针配置，请指出其中的问题并修正：

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 3
  periodSeconds: 5
  failureThreshold: 2
readinessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 3
  periodSeconds: 5
  failureThreshold: 2
```

### 练习5：kubectl操作

写出完成以下任务的kubectl命令：
1. 在namespace `prod` 中创建一个名为 `myapp` 的Deployment，使用镜像 `myapp:1.0`，replicas=3
2. 将镜像更新为 `myapp:1.1` 并触发滚动更新
3. 查看滚动更新状态
4. 回滚到上一个版本
5. 暴露为ClusterIP Service，端口80映射到容器8080

---

## 10. 章节综合实操案例

### 10.1 场景描述

构建一个三服务订单系统，要求同时提供Docker Compose和Kubernetes两套部署方案。

**系统组成**：
- **order-service**：订单服务（Spring Boot + MySQL），提供订单CRUD API
- **inventory-service**：库存服务（Spring Boot + Redis），提供库存扣减API
- **gateway**：API网关（Spring Cloud Gateway），统一入口

### 10.2 项目结构

```
order-system/
├── order-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── inventory-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── gateway/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── docker-compose/
│   ├── .env
│   ├── docker-compose.yml
│   ├── init-scripts/
│   │   └── init.sql
│   └── redis/
│       └── redis.conf
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── mysql-deployment.yaml
│   ├── redis-deployment.yaml
│   ├── order-deployment.yaml
│   ├── inventory-deployment.yaml
│   ├── gateway-deployment.yaml
│   └── ingress.yaml
└── README.md
```

### 10.3 Docker Compose部署方案

```yaml
# docker-compose/docker-compose.yml
version: "3.8"

networks:
  order-network:
    driver: bridge

volumes:
  mysql-data:
  redis-data:

services:
  mysql:
    image: mysql:8.0
    container_name: order-mysql
    networks:
      - order-network
    ports:
      - "3307:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-scripts:/docker-entrypoint-initdb.d
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: order_system
      MYSQL_USER: order_user
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      TZ: Asia/Shanghai
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 30s
    deploy:
      resources:
        limits:
          memory: 1g
          cpus: "1.0"

  redis:
    image: redis:7-alpine
    container_name: order-redis
    networks:
      - order-network
    ports:
      - "6380:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 15s
      timeout: 3s
      retries: 5
      start_period: 10s
    deploy:
      resources:
        limits:
          memory: 256m
          cpus: "0.5"

  inventory-service:
    build:
      context: ../inventory-service
      dockerfile: Dockerfile
    image: inventory-service:1.0.0
    container_name: order-inventory
    networks:
      - order-network
    ports:
      - "8082:8082"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      TZ: Asia/Shanghai
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
    depends_on:
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 40s
    deploy:
      resources:
        limits:
          memory: 512m
          cpus: "0.5"

  order-service:
    build:
      context: ../order-service
      dockerfile: Dockerfile
    image: order-service:1.0.0
    container_name: order-service-app
    networks:
      - order-network
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      TZ: Asia/Shanghai
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/order_system?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: order_user
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      INVENTORY_SERVICE_URL: http://inventory-service:8082
    depends_on:
      mysql:
        condition: service_healthy
      inventory-service:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 50s
    deploy:
      resources:
        limits:
          memory: 512m
          cpus: "0.5"

  gateway:
    build:
      context: ../gateway
      dockerfile: Dockerfile
    image: order-gateway:1.0.0
    container_name: order-gateway
    networks:
      - order-network
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      TZ: Asia/Shanghai
      ORDER_SERVICE_URL: http://order-service:8081
      INVENTORY_SERVICE_URL: http://inventory-service:8082
    depends_on:
      order-service:
        condition: service_healthy
      inventory-service:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 30s
    deploy:
      resources:
        limits:
          memory: 256m
          cpus: "0.5"
```

```bash
# docker-compose/.env
MYSQL_ROOT_PASSWORD=root123456
MYSQL_PASSWORD=order_pass_123
REDIS_PASSWORD=redis_pass_456
TZ=Asia/Shanghai
```

```sql
-- docker-compose/init-scripts/init.sql
CREATE DATABASE IF NOT EXISTS order_system DEFAULT CHARACTER SET utf8mb4;

USE order_system;

CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity` INT NOT NULL COMMENT '数量',
    `total_amount` DECIMAL(10, 2) NOT NULL COMMENT '总金额',
    `status` VARCHAR(16) NOT NULL DEFAULT 'CREATED' COMMENT '订单状态',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `order_items` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_name` VARCHAR(128) NOT NULL COMMENT '商品名称',
    `price` DECIMAL(10, 2) NOT NULL COMMENT '单价',
    `quantity` INT NOT NULL COMMENT '数量',
    INDEX `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';
```

### 10.4 Kubernetes部署方案

```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: order-system
---
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: order-config
  namespace: order-system
data:
  order-service.yml: |
    server:
      port: 8081
    spring:
      datasource:
        url: jdbc:mysql://mysql-service:3306/order_system?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
        username: order_user
        password: ${DB_PASSWORD}
        hikari:
          maximum-pool-size: 20
      redis:
        host: redis-service
        port: 6379
        password: ${REDIS_PASSWORD}
        timeout: 3000ms
    inventory:
      service:
        url: http://inventory-service:8082
    server:
      shutdown: graceful
    spring:
      lifecycle:
        timeout-per-shutdown-phase: 30s
    management:
      endpoints:
        web:
          exposure:
            include: health,info
      endpoint:
        health:
          probes:
            enabled: true
  inventory-service.yml: |
    server:
      port: 8082
    spring:
      redis:
        host: redis-service
        port: 6379
        password: ${REDIS_PASSWORD}
    server:
      shutdown: graceful
    spring:
      lifecycle:
        timeout-per-shutdown-phase: 30s
    management:
      endpoints:
        web:
          exposure:
            include: health,info
      endpoint:
        health:
          probes:
            enabled: true
  gateway.yml: |
    server:
      port: 8080
    spring:
      cloud:
        gateway:
          routes:
            - id: order-service
              uri: http://order-service:80
              predicates:
                - Path=/api/orders/**
            - id: inventory-service
              uri: http://inventory-service:80
              predicates:
                - Path=/api/inventory/**
    server:
      shutdown: graceful
    spring:
      lifecycle:
        timeout-per-shutdown-phase: 30s
    management:
      endpoints:
        web:
          exposure:
            include: health,info
      endpoint:
        health:
          probes:
            enabled: true
---
# k8s/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: order-secret
  namespace: order-system
type: Opaque
data:
  DB_PASSWORD: b3JkZXJfcGFzc18xMjM=      # echo -n "order_pass_123" | base64
  REDIS_PASSWORD: cmVkaXNfcGFzc180NTY=  # echo -n "redis_pass_456" | base64
---
# k8s/mysql-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql
  namespace: order-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
        - name: mysql
          image: mysql:8.0
          ports:
            - containerPort: 3306
          env:
            - name: MYSQL_ROOT_PASSWORD
              value: "root123456"
            - name: MYSQL_DATABASE
              value: "order_system"
            - name: MYSQL_USER
              value: "order_user"
            - name: MYSQL_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: order-secret
                  key: DB_PASSWORD
            - name: TZ
              value: "Asia/Shanghai"
          volumeMounts:
            - name: mysql-data
              mountPath: /var/lib/mysql
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1Gi"
              cpu: "1"
          startupProbe:
            tcpSocket:
              port: 3306
            initialDelaySeconds: 10
            periodSeconds: 10
            failureThreshold: 30
          livenessProbe:
            tcpSocket:
              port: 3306
            periodSeconds: 30
            failureThreshold: 3
          readinessProbe:
            exec:
              command:
                - mysqladmin
                - ping
                - -h
                - localhost
            periodSeconds: 10
            failureThreshold: 5
            initialDelaySeconds: 10
      volumes:
        - name: mysql-data
          persistentVolumeClaim:
            claimName: mysql-pvc
---
# k8s/mysql-pvc.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pvc
  namespace: order-system
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
---
# k8s/mysql-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql-service
  namespace: order-system
spec:
  selector:
    app: mysql
  ports:
    - port: 3306
      targetPort: 3306
  clusterIP: None  # Headless Service（内部发现用）
---
# k8s/redis-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: order-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          ports:
            - containerPort: 6379
          command:
            - redis-server
            - "--appendonly yes"
            - "--requirepass"
            - "$(REDIS_PASSWORD)"
          env:
            - name: REDIS_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: order-secret
                  key: REDIS_PASSWORD
          resources:
            requests:
              memory: "128Mi"
              cpu: "250m"
            limits:
              memory: "256Mi"
              cpu: "500m"
          livenessProbe:
            tcpSocket:
              port: 6379
            periodSeconds: 30
          readinessProbe:
            exec:
              command:
                - redis-cli
                - ping
            periodSeconds: 10
---
# k8s/redis-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: redis-service
  namespace: order-system
spec:
  selector:
    app: redis
  ports:
    - port: 6379
      targetPort: 6379
---
# k8s/order-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: order-system
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      terminationGracePeriodSeconds: 45
      containers:
        - name: order-service
          image: order-service:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8081
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: order-secret
                  key: DB_PASSWORD
            - name: REDIS_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: order-secret
                  key: REDIS_PASSWORD
            - name: JAVA_TOOL_OPTIONS
              value: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
          volumeMounts:
            - name: config
              mountPath: /config/
              readOnly: true
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            periodSeconds: 15
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            periodSeconds: 10
            failureThreshold: 2
            successThreshold: 1
      volumes:
        - name: config
          configMap:
            name: order-config
            items:
              - key: order-service.yml
                path: application.yml
---
# k8s/order-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: order-service
  namespace: order-system
spec:
  selector:
    app: order-service
  ports:
    - name: http
      port: 80
      targetPort: 8081
  type: ClusterIP
---
# k8s/inventory-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: inventory-service
  namespace: order-system
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: inventory-service
  template:
    metadata:
      labels:
        app: inventory-service
    spec:
      terminationGracePeriodSeconds: 45
      containers:
        - name: inventory-service
          image: inventory-service:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8082
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: REDIS_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: order-secret
                  key: REDIS_PASSWORD
            - name: JAVA_TOOL_OPTIONS
              value: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
          volumeMounts:
            - name: config
              mountPath: /config/
              readOnly: true
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8082
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8082
            periodSeconds: 15
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8082
            periodSeconds: 10
            failureThreshold: 2
      volumes:
        - name: config
          configMap:
            name: order-config
            items:
              - key: inventory-service.yml
                path: application.yml
---
# k8s/inventory-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: inventory-service
  namespace: order-system
spec:
  selector:
    app: inventory-service
  ports:
    - name: http
      port: 80
      targetPort: 8082
  type: ClusterIP
---
# k8s/gateway-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway
  namespace: order-system
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: gateway
  template:
    metadata:
      labels:
        app: gateway
    spec:
      terminationGracePeriodSeconds: 45
      containers:
        - name: gateway
          image: order-gateway:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: JAVA_TOOL_OPTIONS
              value: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
          volumeMounts:
            - name: config
              mountPath: /config/
              readOnly: true
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 15
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            periodSeconds: 10
            failureThreshold: 2
      volumes:
        - name: config
          configMap:
            name: order-config
            items:
              - key: gateway.yml
                path: application.yml
---
# k8s/gateway-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: gateway
  namespace: order-system
spec:
  selector:
    app: gateway
  ports:
    - name: http
      port: 80
      targetPort: 8080
  type: ClusterIP
---
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: order-ingress
  namespace: order-system
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /$1
spec:
  ingressClassName: nginx
  rules:
    - host: order.example.com
      http:
        paths:
          - path: /api/orders/?(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: order-service
                port:
                  number: 80
          - path: /api/inventory/?(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: inventory-service
                port:
                  number: 80
          - path: /?(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: gateway
                port:
                  number: 80
```

### 10.5 部署和验证

```bash
# ===== Docker Compose方式 =====
cd docker-compose
docker compose up -d
docker compose ps
curl http://localhost:8080/api/orders/1
docker compose logs -f order-service

# 清理
docker compose down -v

# ===== Kubernetes方式 =====
cd k8s
kubectl apply -f namespace.yaml
kubectl apply -f mysql-pvc.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f mysql-deployment.yaml
kubectl apply -f mysql-service.yaml
kubectl apply -f redis-deployment.yaml
kubectl apply -f redis-service.yaml
kubectl apply -f order-deployment.yaml
kubectl apply -f order-service.yaml
kubectl apply -f inventory-deployment.yaml
kubectl apply -f inventory-service.yaml
kubectl apply -f gateway-deployment.yaml
kubectl apply -f gateway-service.yaml
kubectl apply -f ingress.yaml

# 查看部署状态
kubectl get pods -n order-system -w
kubectl rollout status deployment/order-service -n order-system

# 端口转发本地测试
kubectl port-forward svc/gateway 8080:80 -n order-system

# 清理
kubectl delete namespace order-system
```

---

## 11. 分层综合习题

### 11.1 基础题

**题目1**：简述Docker镜像和容器的区别。

**题目2**：写出以下场景的docker命令：
- 拉取 `openjdk:17-jre-slim` 镜像
- 基于该镜像运行一个名为 `myapp` 的容器，后台运行，映射8080端口
- 查看容器日志

**题目3**：Dockerfile中 `COPY` 和 `ADD` 指令的区别是什么？

**题目4**：K8s中 Pod 和 Deployment 的关系是什么？

**题目5**：简述 `livenessProbe` 和 `readinessProbe` 的区别。

### 11.2 进阶应用题

**题目6**：以下Dockerfile存在多个问题，请指出并修正：

```dockerfile
FROM openjdk:latest
COPY . /app
RUN apt-get update
RUN apt-get install curl
RUN mvn package -DskipTests
CMD ["java", "-jar", "/app/target/app.jar"]
```

**题目7**：设计一个docker-compose.yml，包含以下服务并满足所有条件：
- MySQL 8.0（持久化数据，健康检查）
- Redis 7（持久化数据）
- Spring Boot应用（连接到MySQL和Redis，从.env文件读取配置）
- 资源限制（MySQL 1G内存，其他512M）

**题目8**：以下K8s Deployment配置中，Spring Boot应用启动后反复重启，请分析可能的原因：

```yaml
spec:
  containers:
    - name: app
      image: myapp:1.0
      env:
        - name: SPRING_PROFILES_ACTIVE
          value: prod
      livenessProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
        initialDelaySeconds: 5
        periodSeconds: 10
        failureThreshold: 3
      resources:
        limits:
          memory: "256Mi"
      # 无readinessProbe和startupProbe
```

**题目9**：如何实现Spring Boot应用在K8s上的零停机滚动更新（至少说出三个关键配置）？

### 11.3 精通拔高题

**题目10**：设计一个完整的CI/CD流水线，从Git提交到K8s部署，包含：
- 代码仓库（GitLab）
- 自动构建（Maven编译 + Docker镜像构建）
- 镜像推送（Harbor仓库）
- 自动部署（K8s滚动更新）
要求：给出.gitlab-ci.yml的核心配置片段，并说明每个阶段的用途。

**题目11**：一个Spring Cloud微服务系统（Nacos + Gateway + 3个业务服务），需要在K8s上部署。请设计：
1. 服务发现方案（Nacos如何与K8s Service配合）
2. 配置中心方案（Nacos Config vs ConfigMap，如何选择）
3. 网关路由方案（Gateway + K8s Ingress，如何分工）

**题目12**：在生产环境中，你的Spring Boot应用突然出现大量的OOMKilled。请设计一个排查和优化方案（从监控告警、JVM调优、资源配比三个角度）。

**题目13**：对比Docker Swarm、Kubernetes、Nomad三种容器编排工具，从以下维度进行分析：
- 架构复杂度
- 功能完备性
- 学习成本
- 社区生态
- 生产环境推荐场景

---

## 12. 本章复盘速记清单

### 12.1 Docker核心速记

| 主题 | 关键点 |
|------|--------|
| **镜像 vs 容器** | 镜像是构建时产物（类），容器是运行时实例（对象） |
| **镜像分层机制** | 只读层 + 可写层，Copy-on-Write，层共享节省空间 |
| **Dockerfile关键原则** | 多阶段构建、利用缓存层（固定层放在前面）、使用具体标签、非root用户、设置时区 |
| **docker run核心参数** | `-d`后台、`-p`端口映射、`-v`挂载卷、`-e`环境变量、`--network`网络、`--restart`重启策略 |
| **.dockerignore** | 排除target/.git/等无关文件，减少构建上下文 |

### 12.2 Docker Compose速记

| 主题 | 关键点 |
|------|--------|
| **depends_on陷阱** | 只保证容器启动，不保证应用就绪；需配合`condition: service_healthy` |
| **healthcheck** | 定义容器内应用的健康检查，是生产环境必须配置的 |
| **环境变量管理** | 使用`.env`文件管理敏感配置，通过`${VAR}`引用 |
| **网络模式选择** | 开发用bridge，性能敏感用host，多机用overlay |
| **资源限制** | 通过`deploy.resources.limits`设置内存和CPU限制 |

### 12.3 K8s核心资源速记

| 资源 | 作用 | 核心字段 |
|------|------|---------|
| **Pod** | 最小调度单元 | `spec.containers[]` |
| **Deployment** | 声明式Pod管理 | `replicas`, `strategy`, `template` |
| **Service** | 网络访问抽象 | `selector`, `ports`, `type` |
| **ConfigMap** | 非敏感配置 | `data`（键值对） |
| **Secret** | 敏感配置 | `data`（Base64编码） |
| **Ingress** | 七层路由 | `rules[].host`, `rules[].http.paths[]` |

### 12.4 三探针配置速记

| 探针 | 失败后果 | 配置关键 | 常见错误 |
|------|---------|---------|---------|
| **startupProbe** | 重启容器 | `failureThreshold`设大（慢启动保护） | 未配置导致liveness误杀 |
| **livenessProbe** | 重启容器 | `failureThreshold>=3`避免误杀 | path指向readiness端点 |
| **readinessProbe** | 移除流量 | `failureThreshold`设小（快速响应） | path指向liveness端点 |

### 12.5 资源限制速记

```
JVM内存计算：Pod memory limit ≈ JVM MaxHeap / 0.75
例：MaxRAMPercentage=75%, Pod limit=512Mi → JVM最大堆 ≈ 384Mi
关键参数：-XX:+UseContainerSupport (JDK 11+默认但建议显式设置)
```

### 12.6 高频错误速查

| 错误 | 快速诊断 | 快速修复 |
|------|---------|---------|
| `CrashLoopBackOff` | `kubectl logs -p pod-name` | 查看前一次崩溃日志 |
| `ImagePullBackOff` | `kubectl describe pod` | 检查镜像名和imagePullSecrets |
| `OOMKilled` | `kubectl describe pod` | 增大memory limit或优化JVM |
| `Pending` | `kubectl describe pod` | 检查节点资源和PVC |
| `ErrImagePull` | 手动docker pull | 确保镜像在仓库中存在 |
| `RunContainerError` | `kubectl logs pod-name` | 检查容器启动命令 |

---

## 13. 精通拓展补充-P2

### 13.1 Helm Chart基础

Helm是K8s的包管理器，将一组K8s资源打包为一个Chart，支持参数化、版本管理和一键部署。

#### Helm核心概念

| 概念 | 说明 |
|------|------|
| **Chart** | K8s资源模板包，包含所有需要的YAML文件 |
| **Repository** | Chart的存储仓库，类似Docker Hub |
| **Release** | Chart在K8s上的一个运行实例 |
| **Values** | 模板参数值，用于定制化部署 |

#### Spring Boot应用的Helm Chart结构

```
myapp-chart/
├── Chart.yaml                  # Chart元数据（名称、版本、依赖）
├── values.yaml                 # 默认配置值
├── values-prod.yaml            # 生产环境覆盖值
├── values-staging.yaml         # 预发布环境覆盖值
├── templates/
│   ├── _helpers.tpl            # 模板辅助函数
│   ├── deployment.yaml         # Deployment模板
│   ├── service.yaml            # Service模板
│   ├── configmap.yaml          # ConfigMap模板
│   ├── secret.yaml             # Secret模板
│   ├── ingress.yaml            # Ingress模板
│   ├── hpa.yaml                # HPA模板（可选）
│   └── _tests/
│       └── test-connection.yaml # 连接测试
└── .helmignore                 # 排除文件
```

#### Chart.yaml

```yaml
apiVersion: v2
name: spring-boot-app
description: A Helm chart for Spring Boot microservice
type: application
version: 1.0.0
appVersion: "1.0.0"
keywords:
  - spring-boot
  - java
  - microservice
maintainers:
  - name: DevOps Team
    email: devops@example.com
```

#### values.yaml

```yaml
# values.yaml — 默认配置（可以被覆盖）
replicaCount: 2

image:
  repository: registry.example.com/myapp
  tag: "1.0.0"
  pullPolicy: IfNotPresent
  pullSecrets: []

service:
  type: ClusterIP
  port: 80
  targetPort: 8080

ingress:
  enabled: true
  className: nginx
  host: api.example.com
  path: /
  pathType: Prefix
  tls:
    enabled: true
    secretName: example-tls

resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"

probes:
  startup:
    enabled: true
    path: /actuator/health/liveness
    initialDelaySeconds: 10
    periodSeconds: 5
    failureThreshold: 30
  liveness:
    enabled: true
    path: /actuator/health/liveness
    initialDelaySeconds: 10
    periodSeconds: 15
    failureThreshold: 3
  readiness:
    enabled: true
    path: /actuator/health/readiness
    periodSeconds: 10
    failureThreshold: 2

env:
  SPRING_PROFILES_ACTIVE: "prod"
  TZ: "Asia/Shanghai"

configMap:
  enabled: true
  applicationYml: |
    server:
      shutdown: graceful
    spring:
      lifecycle:
        timeout-per-shutdown-phase: 30s
    management:
      endpoints:
        web:
          exposure:
            include: health,info,prometheus
      endpoint:
        health:
          probes:
            enabled: true

secret:
  enabled: true
  data:
    DB_PASSWORD: ""
    REDIS_PASSWORD: ""

autoscaling:
  enabled: false
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80
  targetMemoryUtilizationPercentage: 80
```

#### Deployment模板（templates/deployment.yaml）

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      {{- include "myapp.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "myapp.selectorLabels" . | nindent 8 }}
    spec:
      terminationGracePeriodSeconds: {{ .Values.terminationGracePeriodSeconds | default 60 }}
      {{- with .Values.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      containers:
        - name: {{ .Chart.Name }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          ports:
            - name: http
              containerPort: {{ .Values.service.targetPort }}
              protocol: TCP
          env:
            {{- range $key, $value := .Values.env }}
            - name: {{ $key }}
              value: {{ $value | quote }}
            {{- end }}
            {{- if .Values.secret.enabled }}
            {{- range $key, $value := .Values.secret.data }}
            - name: {{ $key }}
              valueFrom:
                secretKeyRef:
                  name: {{ include "myapp.fullname" . }}
                  key: {{ $key }}
            {{- end }}
            {{- end }}
          resources:
            {{- toYaml .Values.resources | nindent 12 }}
          {{- if .Values.probes.startup.enabled }}
          startupProbe:
            httpGet:
              path: {{ .Values.probes.startup.path }}
              port: {{ .Values.service.targetPort }}
            initialDelaySeconds: {{ .Values.probes.startup.initialDelaySeconds }}
            periodSeconds: {{ .Values.probes.startup.periodSeconds }}
            failureThreshold: {{ .Values.probes.startup.failureThreshold }}
          {{- end }}
          {{- if .Values.probes.liveness.enabled }}
          livenessProbe:
            httpGet:
              path: {{ .Values.probes.liveness.path }}
              port: {{ .Values.service.targetPort }}
            initialDelaySeconds: {{ .Values.probes.liveness.initialDelaySeconds }}
            periodSeconds: {{ .Values.probes.liveness.periodSeconds }}
            failureThreshold: {{ .Values.probes.liveness.failureThreshold }}
          {{- end }}
          {{- if .Values.probes.readiness.enabled }}
          readinessProbe:
            httpGet:
              path: {{ .Values.probes.readiness.path }}
              port: {{ .Values.service.targetPort }}
            periodSeconds: {{ .Values.probes.readiness.periodSeconds }}
            failureThreshold: {{ .Values.probes.readiness.failureThreshold }}
          {{- end }}
```

#### Helm常用命令

```bash
# 创建Chart骨架
helm create myapp-chart

# 安装Chart
helm install myapp-release ./myapp-chart --values values-prod.yaml

# 查看已安装的Release
helm list

# 升级Release
helm upgrade myapp-release ./myapp-chart --values values-prod.yaml --set image.tag=1.1.0

# 回滚
helm rollback myapp-release 2

# 卸载
helm uninstall myapp-release

# 渲染模板但不安装（调试用）
helm template ./myapp-chart --values values-prod.yaml

# 查看Release状态
helm status myapp-release

# 查看Release历史
helm history myapp-release
```

> 💡 **Helm最佳实践**：
> - 使用多values文件分层管理配置（values.yaml → values-{env}.yaml）
> - 生产环境中锁定Chart版本（`helm dependency update`）
> - CI/CD中通过`--set image.tag=$CI_COMMIT_SHA`注入构建版本
> - 敏感数据不写在values中，通过外部Secret管理

### 13.2 K8s HPA（Horizontal Pod Autoscaler）

HPA根据CPU/内存使用率或自定义指标自动调整Pod副本数。

```yaml
# hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: service-a-hpa
  namespace: micro-dev
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: service-a
  minReplicas: 2          # 最小副本数
  maxReplicas: 10         # 最大副本数
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70    # CPU使用率超过70%自动扩容
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80    # 内存使用率超过80%自动扩容
```

```bash
# 创建HPA
kubectl apply -f hpa.yaml

# 查看HPA状态
kubectl get hpa -n micro-dev -w

# 手动测试压测扩容（模拟CPU负载）
kubectl run -i --tty load-generator --rm --image=busybox --restart=Never \
  -- /bin/sh -c "while sleep 0.01; do wget -q -O- http://service-a; done"
```

**HPA扩容计算公式**：

```
期望副本数 = ceil[当前副本数 × (当前指标值 / 目标指标值)]

例如：当前副本=3，CPU使用率=140%，目标=70%
期望副本数 = ceil[3 × (140/70)] = ceil[6] = 6
```

### 13.3 K8s Service Mesh — Istio入门

Service Mesh是微服务通信的基础设施层，将服务发现、负载均衡、熔断、限流、可观测性等能力从应用代码中剥离到Sidecar代理。

```
┌─────────────────────────────────────────────┐
│               Service Mesh                    │
│                                               │
│  Service A ──▶ Sidecar(Envoy) ──▶ Service B  │
│  (业务代码)     (代理/治理)       (业务代码)    │
│                                               │
│  Control Plane (Pilot/Mixer/Citadel)          │
│  管理所有Sidecar的策略和配置                    │
└─────────────────────────────────────────────┘
```

#### Istio核心功能

| 功能 | 说明 | 类比 |
|------|------|------|
| **流量管理** | 金丝雀发布、灰度路由、流量镜像 | Nginx高级路由 |
| **安全** | mTLS双向认证、RBAC | 应用层零信任 |
| **可观测性** | 指标、追踪、日志自动采集 | Skywalking + Prometheus |
| **故障注入** | 人为注入延迟/异常测试弹性 | Chaos Engineering |

#### Spring Boot + Istio集成

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: service-a
spec:
  hosts:
    - service-a
  http:
    - match:
        - headers:
            version:
              exact: v2
      route:
        - destination:
            host: service-a
            subset: v2
          weight: 100
    - route:
        - destination:
            host: service-a
            subset: v1
          weight: 90
        - destination:
            host: service-a
            subset: v2
          weight: 10
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: service-a
spec:
  host: service-a
  subsets:
    - name: v1
      labels:
        version: v1
    - name: v2
      labels:
        version: v2
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 10
        http2MaxRequests: 1000
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 30s
```

### 13.4 Docker Compose vs K8s — 如何选择

| 决策维度 | Docker Compose | Kubernetes |
|----------|---------------|------------|
| **集群规模** | 单机（最多几十个容器） | 多机集群（成百上千节点） |
| **高可用** | 基本无（需外部配合） | 原生支持（多Master + etcd集群） |
| **运维复杂度** | 低（一条命令启动） | 高（需要专业运维团队） |
| **学习成本** | 低（1-2天入门） | 高（1-3个月熟练） |
| **功能丰富度** | 基础部署 | 完整平台（监控/日志/CI/CD/安全） |
| **社区生态** | Docker生态 | CNCF生态（Helm/Prometheus/Istio等） |
| **资源开销** | 只需Docker Engine | 需要etcd + 控制平面 + 工作节点 |
| **升级回滚** | 手动 | 原生滚动更新 + 回滚 |
| **服务发现** | Docker DNS（同一网络） | Service + DNS + 多种发现机制 |
| **存储** | Volume绑定 | PV/PVC + StorageClass + CSI |

> 🎯 **选择建议**：
> - **开发/测试环境** → Docker Compose（快速启动、轻量、成本低）
> - **小型生产（单机）** → Docker Compose + 监控 + 备份
> - **中大型生产（多机）** → Kubernetes（功能完备、生态丰富）
> - **云原生转型** → Kubernetes + Istio + 云原生中间件
> - **CI/CD流水线** → Docker Compose用于集成测试，K8s用于部署

### 13.5 CI/CD流水线 — GitLab CI + Docker + K8s

```yaml
# .gitlab-ci.yml — 完整的CI/CD流水线
stages:
  - compile       # 编译
  - test          # 测试
  - build-image   # 构建Docker镜像
  - push-image    # 推送镜像到仓库
  - deploy-dev    # 部署到开发环境
  - deploy-prod   # 部署到生产环境

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=${CI_PROJECT_DIR}/.m2/repository -DskipTests=true"
  DOCKER_REGISTRY: "registry.example.com"
  APP_NAME: "service-a"
  DOCKER_IMAGE: "${DOCKER_REGISTRY}/${APP_NAME}:${CI_COMMIT_SHORT_SHA}"
  K8S_NAMESPACE_DEV: "micro-dev"
  K8S_NAMESPACE_PROD: "micro-prod"

# 缓存Maven依赖，加速构建
cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .m2/repository/

# ===== 阶段1：编译 =====
compile:
  stage: compile
  image: maven:3.9.6-eclipse-temurin-17-alpine
  script:
    - mvn compile -B
  artifacts:
    paths:
      - target/
    expire_in: 1 hour
  only:
    - main
    - develop

# ===== 阶段2：单元测试 =====
test:
  stage: test
  image: maven:3.9.6-eclipse-temurin-17-alpine
  script:
    - mvn test -B
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml
  only:
    - main
    - develop

# ===== 阶段3：构建Docker镜像 =====
build-image:
  stage: build-image
  image: docker:24.0.5
  services:
    - docker:24.0.5-dind
  script:
    - docker build -t ${DOCKER_IMAGE} .
    - docker tag ${DOCKER_IMAGE} ${DOCKER_REGISTRY}/${APP_NAME}:latest
  only:
    - main

# ===== 阶段4：推送镜像到仓库 =====
push-image:
  stage: push-image
  image: docker:24.0.5
  services:
    - docker:24.0.5-dind
  script:
    - echo "${REGISTRY_PASSWORD}" | docker login ${DOCKER_REGISTRY} -u "${REGISTRY_USER}" --password-stdin
    - docker push ${DOCKER_IMAGE}
    - docker push ${DOCKER_REGISTRY}/${APP_NAME}:latest
  only:
    - main

# ===== 阶段5：部署到开发环境 =====
deploy-dev:
  stage: deploy-dev
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/${APP_NAME} ${APP_NAME}=${DOCKER_IMAGE} -n ${K8S_NAMESPACE_DEV}
    - kubectl rollout status deployment/${APP_NAME} -n ${K8S_NAMESPACE_DEV} --timeout=5m
  environment:
    name: dev
  only:
    - develop

# ===== 阶段6：部署到生产环境（需要手动审批） =====
deploy-prod:
  stage: deploy-prod
  image: bitnami/kubectl:latest
  script:
    # 备份当前版本
    - kubectl get deployment ${APP_NAME} -n ${K8S_NAMESPACE_PROD} -o yaml > deploy-backup.yaml
    # 触发滚动更新
    - kubectl set image deployment/${APP_NAME} ${APP_NAME}=${DOCKER_IMAGE} -n ${K8S_NAMESPACE_PROD}
    - kubectl rollout status deployment/${APP_NAME} -n ${K8S_NAMESPACE_PROD} --timeout=5m
    # 验证部署
    - kubectl get pods -n ${K8S_NAMESPACE_PROD} -l app=${APP_NAME}
  environment:
    name: production
  when: manual  # 需要手动点击按钮触发
  only:
    - main
```

### 13.6 容器安全最佳实践

```dockerfile
# 容器安全Dockerfile

# 1. 使用最小基础镜像（减小攻击面）
FROM eclipse-temurin:17-jre-alpine AS runtime

# 2. 定期更新基础镜像
RUN apk update && apk upgrade --no-cache

# 3. 只安装必要工具
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 4. 使用非root用户运行
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# 5. 只复制必要文件
COPY --from=builder /build/target/app.jar app.jar

# 6. 镜像内容不可变（不写入持久数据）
# 7. 不包含调试工具（不使用--no-cache安装bash/openssh）

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# 8. 使用ENTRYPOINT（不易被覆盖）
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**K8s安全上下文**：

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      # Pod级别的安全策略
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        runAsGroup: 3000
        fsGroup: 2000
      containers:
        - name: service-a
          # 容器级别的安全策略
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop:
                - ALL
              add:
                - NET_BIND_SERVICE
            privileged: false
```

### 13.7 K8s故障排除工具箱

```bash
# ===== Pod级别 =====

# 查看Pod详细信息和事件
kubectl describe pod <pod-name> -n <namespace>

# 查看Pod日志（包括前一次崩溃的日志）
kubectl logs <pod-name> -n <namespace> --previous

# 进入Pod调试
kubectl exec -it <pod-name> -n <namespace> -- sh

# 临时启动调试Pod（共享网络命名空间）
kubectl run debug-pod --image=nicolaka/netshoot -it --rm -- /bin/bash

# ===== 节点级别 =====

# 查看节点状态和资源使用
kubectl top nodes
kubectl describe node <node-name>

# 查看节点上运行的Pod
kubectl get pods --all-namespaces --field-selector spec.nodeName=<node-name>

# 查看节点条件
kubectl get nodes -o jsonpath='{.items[*].status.conditions}'

# ===== 资源级别 =====

# 查看所有资源
kubectl get all -n <namespace>

# 查看API资源类型列表
kubectl api-resources

# 查看资源YAML（特别是admission webhook修改后的版本）
kubectl get pod <pod-name> -n <namespace> -o yaml

# ===== 网络调试 =====

# 测试Service DNS解析
kubectl run dns-test --image=busybox --rm -it --restart=Never -- nslookup service-a

# 测试网络连通性
kubectl run net-test --image=nicolaka/netshoot --rm -it --restart=Never -- \
  curl -v http://service-a:80/actuator/health

# ===== 性能排查 =====

# 查看Pod资源使用
kubectl top pod -n <namespace>

# 查看历史资源使用（需要Metrics Server）
kubectl top pod -n <namespace> --sort-by=cpu

# ===== 事件排查 =====

# 查看集群事件（按时间排序）
kubectl get events -n <namespace> --sort-by='.lastTimestamp'

# 查看异常事件
kubectl get events -n <namespace> --field-selector type=Warning
```

### 13.8 生产环境检查清单

| 类别 | 检查项 | 说明 |
|------|--------|------|
| **镜像安全** | 基础镜像是否最小化 | 使用alpine-slim而非完整OS镜像 |
| **镜像安全** | 是否使用非root用户 | 禁止root用户运行应用 |
| **镜像安全** | 是否定期扫描漏洞 | 集成Trivy/Clair扫描 |
| **Pod配置** | 是否设置resource requests/limits | 防止某个Pod抢占全部节点资源 |
| **Pod配置** | 是否配置了三种探针 | startup/liveness/readiness |
| **Pod配置** | 是否设置了优雅关闭 | terminationGracePeriodSeconds + graceful shutdown |
| **Pod配置** | PodDisruptionBudget是否配置 | 保证自愿中断时最低可用Pod数 |
| **网络** | 是否使用NetworkPolicy | 最小权限网络策略 |
| **网络** | Ingress是否配置TLS | 确保外部访问加密 |
| **存储** | 是否使用PVC持久化数据 | 避免容器重启丢失数据 |
| **配置** | 敏感数据是否使用Secret | 禁止在ConfigMap中明文存储密码 |
| **配置** | 是否需要加密etcd中的Secret | 大企业合规要求 |
| **高可用** | Deployment replicas是否>=2 | 避免单点故障 |
| **高可用** | 是否配置HPA | 应对突发流量 |
| **高可用** | Pod是否分布在不同节点 | 使用podAntiAffinity |
| **监控** | Prometheus指标采集是否配置 | 监控应用和集群健康 |
| **监控** | 日志是否集中采集 | 使用EFK/Loki |
| **监控** | 告警规则是否配置 | CPU/内存/OOM/探针失败 |
| **备份** | etcd是否定期备份 | 集群灾难恢复 |
| **备份** | 持久化数据是否备份 | 数据库备份策略 |
| **运维** | 是否使用Helm管理部署 | 标准化部署流程 |
| **运维** | CI/CD是否包含自动化测试 | 减少人为操作失误 |
| **运维** | 是否有回滚方案 | 滚动更新失败时能快速回滚 |

---

> 🎯 **本章总结**：容器化部署是现代微服务架构的基石。Docker解决了环境一致性和标准化交付问题，Kubernetes则解决了大规模容器编排和高可用问题。从Dockerfile编写到Docker Compose编排，再到Kubernetes集群管理，构成了完整的容器化部署技术栈。掌握这些技能，是后端工程师从P1走向P2精通进阶的关键一步。
