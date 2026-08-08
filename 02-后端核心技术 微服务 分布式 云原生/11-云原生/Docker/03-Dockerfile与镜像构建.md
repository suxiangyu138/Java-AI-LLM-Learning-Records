# 03-Dockerfile 与镜像构建
> 镜像中打包软件的本质 = 将 Java 软件运行所需全量依赖资源按 Docker 规范整合为独立镜像——指令详解、多阶段构建、缓存优化、安全实践

## 📚 目录
1. [核心认知：Java 视角的镜像打包](#1-核心认知java-视角的镜像打包)
2. [前置准备：Java 软件自身打包](#2-前置准备java-软件自身打包)
3. [Dockerfile 基础指令详解](#3-dockerfile-基础指令详解)
4. [三大场景标准 Dockerfile](#4-三大场景标准-dockerfile)
5. [多阶段构建](#5-多阶段构建)
6. [构建缓存与 .dockerignore](#6-构建缓存与-dockerignore)
7. [构建自动化：Maven 插件与 Jenkins](#7-构建自动化maven-插件与-jenkins)
8. [生产环境安全与优化实践](#8-生产环境安全与优化实践)
9. [镜像体积与启动速度优化](#9-镜像体积与启动速度优化)
10. [高频避坑清单](#10-高频避坑清单)
11. [核心要点](#11-核心要点)
12. [参考来源](#12-参考来源)

## 1. 核心认知：Java 视角的镜像打包

- **本质**：镜像中打包软件 = 将 Java 软件运行所需**全量依赖资源**整合为独立镜像：编译后的 JAR/WAR 包 + 运行环境（JRE/JDK）+ 第三方依赖库 + 配置文件 + 启动脚本。
- **与传统 Java 打包的核心差异**：「包含运行环境」——这是实现"一次构建，到处运行"、解决"本地能跑、部署报错"的关键。
- **两级打包关系**：Java 应用打包（JAR/WAR 生成，编译层面）是镜像打包的基础；**必须先通过 Maven/Gradle 生成可运行 JAR/WAR，再写 Dockerfile**，否则镜像打包必然失败。
- **关键前提**：本地无法运行的 JAR 包，镜像中也必然无法运行（必须本地 `java -jar` 验证）。

## 2. 前置准备：Java 软件自身打包

### 2.1 依赖管理与精简（减少镜像体积关键）

| 操作 | 说明 |
|------|------|
| 删除无用依赖 | pom.xml 剔除 test 依赖（`<scope>test</scope>`）与开发环境依赖 |
| 固定依赖版本 | 用 `<dependencyManagement>` 统一管理，避免版本冲突 |
| 处理本地依赖 | 未上传 Maven 仓库的本地 JAR 需在 pom 正确配置依赖路径 |

### 2.2 Spring Boot 应用打包（JAR，最常用）

Spring Boot 内置 Tomcat，默认打包为可运行 JAR，是镜像打包首选场景：

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
      <version>2.7.10</version>          <!-- 与 Spring Boot 版本一致 -->
      <executions>
        <execution>
          <goals>
            <goal>repackage</goal>      <!-- 关键：生成可运行 JAR 包 -->
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

```bash
mvn clean package -DskipTests    # 打包（跳过测试加快速度）
java -jar target/app.jar         # 本地验证可运行
```

> 💡 需动态修改的配置（application-prod.yml 等）建议从 JAR 分离，后续用数据卷挂载，避免每次改配置都重新打包。

### 2.3 传统 SSM 应用打包（WAR 包）

- pom.xml 设置 `<packaging>war</packaging>`；
- 排除内置 Tomcat、引入外部 Tomcat 依赖（scope=provided）。

## 3. Dockerfile 基础指令详解

### 3.1 FROM — 选择基础镜像

```dockerfile
# 推荐：官方 Alpine 版本，体积极小
FROM openjdk:17-jdk-alpine

# 也可选 Slim 变体（基于 Debian，比 Alpine 大但兼容性更好）
FROM openjdk:17-jdk-slim

# 不推荐：完整版包含大量无用工具
# FROM openjdk:17
```

> ⚠️ **musl vs glibc**：Alpine 基于 musl libc 而非 glibc，体积只有 5MB 左右。对于依赖 glibc 特定行为的 Java 应用（尤其涉及 JNI/Native 库的），应选择 `slim` 版本。

### 3.2 RUN — 执行构建命令

```dockerfile
# 最佳实践：合并命令减少层数，&& 确保全部成功
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Alpine 版本
RUN apk add --no-cache curl bash
```

> ⚠️ **缓存清理必须在同一 RUN 中**：`rm -rf /var/lib/apt/lists/*` 必须与 `apt-get update` 在同一 RUN 指令中，否则缓存清理单独成层且无法被移除。

### 3.3 COPY vs ADD

```dockerfile
# 推荐：COPY，语义清晰
COPY target/my-app.jar app.jar

# 不推荐：ADD 有隐含行为，容易造成意外
ADD target/my-app.tar.gz /app/    # 自动解压 tar
ADD https://example.com/file /app/ # 支持 URL 下载（不推荐生产）
```

**规则**：复制本地文件用 `COPY`；只有需要自动解压 tar 包时才用 `ADD`（且解压行为会破坏构建缓存）；**绝不使用 ADD 下载远程文件**——用 `RUN curl`/`wget` 完成，便于利用缓存和错误处理。

### 3.4 ENV vs ARG

- **ENV**：设置环境变量，构建时和运行时都生效；
- **ARG**：仅构建时参数，容器运行时不保留（除非用 ENV 引用）。

```dockerfile
ARG APP_VERSION=1.0.0              # 构建参数：docker build --build-arg APP_VERSION=2.0.0
ENV SPRING_PROFILES_ACTIVE=prod    # 运行时环境变量
ENV APP_VERSION=$APP_VERSION       # 将 ARG 的值持久化为 ENV
```

### 3.5 EXPOSE

```dockerfile
EXPOSE 8080
```

> 💡 仅声明容器监听的端口，**不会自动映射端口**——是文档作用，运行时仍需 `-p 8080:8080` 才能真正映射。但 Kubernetes/Docker Compose 编排中 EXPOSE 信息会被自动发现。

### 3.6 CMD vs ENTRYPOINT（最容易混淆）

```dockerfile
# ENTRYPOINT：定义容器启动时的可执行文件（难以被覆盖）
ENTRYPOINT ["java", "-jar", "app.jar"]

# CMD：为 ENTRYPOINT 提供默认参数（可以被 docker run 命令覆盖）
CMD ["--spring.profiles.active=prod"]

# 常见用法组合
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=dev"]
```

> ⚠️ **Exec vs Shell 形式与 PID 1 问题**：Shell 形式（不带方括号）会通过 `/bin/sh -c` 执行，PID 为 1 的不是应用进程，导致信号无法正确传递（`docker stop` 可能无法优雅关闭 Java 进程）。**始终使用 Exec 形式（JSON 数组语法）**：
> - 正确：`ENTRYPOINT ["java", "-jar", "app.jar"]`（PID 1 是 Java 进程）
> - 错误：`ENTRYPOINT java -jar app.jar`（PID 1 是 sh，Java 成为子进程）

## 4. 三大场景标准 Dockerfile

### 场景 1：Spring Boot JAR 包（单体应用，最常用）

```dockerfile
# 1. 基础镜像选择：轻量 JRE 镜像（生产优先，减少体积）
# JDK8 用 openjdk:8-jre-slim，JDK11 用 openjdk:11-jre-slim
FROM openjdk:11-jre-slim

# 2. 工作目录：规范应用存储路径
WORKDIR /app

# 3. 复制 JAR 包：通配符适配带版本号的 JAR 包
COPY target/*.jar /app/app.jar

# 4. 暴露端口：与 Spring Boot 配置的 server.port 一致
EXPOSE 8080

# 5. 启动命令：配置 JVM 参数，避免 OOM，适配 Docker 资源限制
# 建议 Xmx 不超过容器内存的 80%，如容器限制 2G，可配置 -Xms512m -Xmx1.6g
ENTRYPOINT ["java", "-Xms512m", "-Xmx1g", "-jar", "/app/app.jar"]
```

### 场景 2：传统 SSM WAR 包（需外部 Tomcat）

```dockerfile
# 1. 基础镜像：官方 Tomcat 镜像（Tomcat9 适配 JDK8/11）
FROM tomcat:9-jre11-slim

# 2. 清理 Tomcat 默认项目，避免干扰
RUN rm -rf /usr/local/tomcat/webapps/*

# 3. 复制 WAR 包到部署目录，重命名为 ROOT.war，无需加项目名访问
COPY target/ssm-web-1.0.0.war /usr/local/tomcat/webapps/ROOT.war

# 4. 暴露 Tomcat 端口（默认 8080）
EXPOSE 8080

# 5. 启动 Tomcat（默认启动命令）
ENTRYPOINT ["catalina.sh", "run"]
```

### 场景 3：Java 微服务多模块

- 根项目 pom.xml 统一管理所有微服务的依赖版本、打包插件；
- 每个微服务模块单独编写 Dockerfile（放模块根目录，与 src 同级）；
- 根目录执行 `mvn clean package -DskipTests` 批量生成各模块 JAR；
- 通过 docker-maven-plugin 一键批量构建镜像（见第 7 节）。

## 5. 多阶段构建

核心思想：**第一阶段用完整环境编译，第二阶段只复制编译产物到精简镜像**。最终镜像只包含 JRE + JAR，没有 JDK、Maven、源代码等编译时依赖。

```dockerfile
# === 第一阶段：编译阶段 ===
FROM maven:3.8.6-eclipse-temurin-17 AS builder
WORKDIR /build

# 先复制 pom.xml，利用缓存安装依赖（只要 pom 不变化就命中缓存）
COPY pom.xml .
RUN mvn dependency:resolve -pl . -q

# 复制源码并编译
COPY src ./src
RUN mvn package -DskipTests -q

# === 第二阶段：运行阶段 ===
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 创建一个非 root 用户（安全最佳实践）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 只从 builder 阶段复制 jar 文件
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

| 构建方式 | 镜像体积 |
|----------|---------|
| 单阶段构建（FROM maven + COPY + RUN mvn package） | 约 500~700MB |
| 多阶段构建（FROM jre-alpine） | 约 120~180MB |

## 6. 构建缓存与 .dockerignore

### 6.1 构建缓存优化

```dockerfile
# 不推荐：每次修改源码都会重新安装依赖
COPY . .
RUN mvn package

# 推荐：分离 pom 和源码，先装依赖
COPY pom.xml .
RUN mvn dependency:go-offline      # pom 不变则命中缓存
COPY src ./src
RUN mvn package                     # 只编译改动的代码
```

**构建缓存失效规则**：

- 某层指令发生变化（命令文本或上下文文件变化），该层及后续层全部失效；
- 使用 `--no-cache` 强制全部重跑；
- 频繁变化的指令（如 `COPY src`）放在 Dockerfile **靠后**的位置；
- 不变的操作放前面（基础镜像、依赖下载、创建用户、WORKDIR）。

### 6.2 .dockerignore

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

> ⚠️ `docker build` 会将当前目录（构建上下文）**整个发送给 Docker 守护进程**。如果忘了忽略 `target/`，几百 MB 的编译输出会被传到守护进程，即使最终不会打包进镜像，构建第一步也会慢得令人崩溃。

## 7. 构建自动化：Maven 插件与 Jenkins

### 7.1 docker-maven-plugin（本地一键）

```xml
<build>
  <finalName>springboot-automated-build</finalName>
  <plugins>
    <!-- Spring Boot 打包插件（生成可运行 JAR） -->
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
      <version>2.7.10</version>
      <executions>
        <execution><goals><goal>repackage</goal></goals></execution>
      </executions>
    </plugin>
    <!-- Docker Maven 插件（自动化构建与推送） -->
    <plugin>
      <groupId>com.spotify</groupId>
      <artifactId>docker-maven-plugin</artifactId>
      <version>1.2.2</version>
      <configuration>
        <imageName>docker.io/your-username/springboot-automated:1.0.0</imageName>
        <dockerDirectory>./</dockerDirectory>
        <resources>
          <resource>
            <targetPath>/</targetPath>
            <directory>${project.build.directory}</directory>
            <include>${project.build.finalName}.jar</include>
          </resource>
        </resources>
        <pushImage>true</pushImage>
        <pushImageTag>true</pushImageTag>
        <dockerHost>unix:///var/run/docker.sock</dockerHost>
      </configuration>
    </plugin>
  </plugins>
</build>
```

```bash
mvn clean package docker:build     # "Java 打包 → 镜像构建 → 推送"全流程
```

> 💡 微服务多模块在每个模块配置该插件，根目录执行可批量构建推送；版本管理通过改 pom 中镜像版本号实现，无需手动改 Dockerfile。

### 7.2 Jenkins 流水线

```groovy
pipeline {
    agent any
    environment {
        IMAGE_NAME = "springboot-automated"
        IMAGE_VERSION = "1.0.0"
        DOCKER_REPO = "docker.io/your-username"
    }
    stages {
        stage('Pull Code') {
            steps { git url: 'https://github.com/your-username/your-java-project.git', branch: 'master' }
        }
        stage('Build & Package') {
            steps { sh 'mvn clean package docker:build' }
        }
        stage('Push Image') {
            steps { sh "docker push ${DOCKER_REPO}/${IMAGE_NAME}:${IMAGE_VERSION}" }
        }
        stage('Deploy') {
            steps {
                sh 'ssh root@your-server-ip "sh /root/deploy/deploy.sh"'
            }
        }
    }
    post {
        failure { echo '构建失败，请检查代码或配置！' }
    }
}
```

## 8. 生产环境安全与优化实践

### 8.1 非 root 用户运行（核心）

```dockerfile
FROM openjdk:11-jre-slim
# 创建非 root 用户和用户组
RUN addgroup --system java-group && adduser --system --group java-user
# 切换到非 root 用户
USER java-user
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms512m", "-Xmx1g", "-jar", "/app/app.jar"]
```

> ⚠️ 不要以 root 运行 Java 进程——如果被攻破，攻击者有完全权限。

### 8.2 生产环境安全实践 6 条

```dockerfile
# 1. 使用非 root 用户运行（见 8.1）
# 2. 使用只读根文件系统（与 K8s securityContext 配合）
# docker run --read-only --tmpfs /tmp ...
# 3. 设置明确的资源限制（建议在编排层控制）
# 4. 移除 setuid/setbit 权限
RUN find / -perm /6000 -type f -exec chmod a-s {} \; || true
# 5. 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
# 6. 限制容器权限（运行时）
# docker run -d -p 80:8080 --cap-add=NET_BIND_SERVICE springboot-app:1.0.0
```

### 8.3 JVM 参数生产级配置

```text
-XX:+UseContainerSupport           # 适配 Docker 容器，自动感知容器资源限制（Java 10+ 默认开启）
-Xmx1.6g                           # 限制 JVM 最大内存，不超过容器内存的 80%
-Xms1.6g                           # 初始内存与最大内存一致，避免频繁 GC
-XX:+PrintGCDetails -XX:+PrintGCTimeStamps   # GC 日志
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/heapdump.hprof   # OOM 堆转储
```

> ⚠️ **Java 8 及以下**：JVM 无法识别 Docker 容器内存限制，需添加 `-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap`。

### 8.4 多环境适配

```dockerfile
# 方案 1：环境变量注入（推荐）
ENV SPRING_PROFILES_ACTIVE=dev \
    DB_URL=jdbc:mysql://localhost:3306/db_dev
ENTRYPOINT ["java", "-Xms512m", "-Xmx1g", "-jar", "/app/app.jar", "--spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
```

```bash
docker run -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod -e DB_URL=jdbc:mysql://prod-db:3306/db_prod springboot-app:1.0.0
```

```bash
# 方案 2：配置文件挂载（配置复杂场景）
docker run -d -p 8080:8080 -v /host/config/application-prod.yml:/app/application.yml springboot-app:1.0.0
```

### 8.5 镜像版本管理

```text
版本规范（必做）：应用名称:主版本.次版本.修订号-环境
  springboot-app:1.0.0-prod    # 生产
  springboot-app:1.0.1-test    # 测试
禁止使用 latest 标签！—— 便于版本回滚和问题排查
```

### 8.6 多架构适配（Buildx）

```bash
docker buildx create --use
docker buildx build --platform linux/amd64,linux/arm64 -t your-repo/springboot-app:1.0.0 --push .
```

## 9. 镜像体积与启动速度优化

| 手段 | 说明 |
|------|------|
| 多阶段构建 | 压缩 50% 以上，拉取速度提升 50% 以上 |
| 轻量基础镜像 | `jre-slim`（常规兼容）；`jre-alpine`（极致轻量，musl 需测试）；避免 `java:xx` 非官方镜像 |
| 清理临时文件 | `RUN rm -rf /root/.m2`、`RUN apt-get clean`、`RUN rm -rf /tmp/*` |
| JVM 参数 | `-Xms` 与 `-Xmx` 相等避免频繁 GC；`-XX:+UseContainerSupport` |
| ENTRYPOINT 精简 | 仅保留应用启动命令，避免无关初始化脚本增加启动耗时 |
| 构建缓存 | 分层缓存利用（pom 分离） |

## 10. 高频避坑清单

| # | 报错/现象 | 成因 | 解决方案 |
|---|----------|------|----------|
| 1 | `COPY failed: no such file or directory` | COPY 源路径错误（JAR 未生成）或执行目录不对 | 确认 target 下 JAR 已生成；进入 Dockerfile 目录执行（末尾加 `.`） |
| 2 | `no main manifest attribute` | 未生成可运行 JAR（缺 repackage 目标） | pom 含 `<goal>repackage</goal>`；重新打包并本地 java -jar 验证 |
| 3 | 镜像体积过大（几百 MB~1GB） | 臃肿基础镜像、未多阶段构建、依赖冗余 | 换 jre-slim/alpine；启用多阶段构建；精简 pom 依赖 |
| 4 | 容器启动后 Java 应用无法访问 | `server.address=127.0.0.1`（仅容器内访问） | 确保 `server.address=0.0.0.0` |
| 5 | 微服务多模块镜像版本混乱 | 未统一版本管理、用 latest | 根 pom 定义版本变量；镜像名"微服务名:版本号" |
| 6 | Jenkins 报 `docker: command not found` | Jenkins 服务器未装 Docker 或用户无权限 | 安装 Docker；`usermod -aG docker jenkins` 后重启 |
| 7 | 多阶段构建后镜像启动报"JAR 包不存在" | COPY 路径错误 | 检查编译阶段 JAR 生成路径与 `COPY --from=build` 一致 |
| 8 | 环境变量注入后应用无法读取 | Spring Boot 未正确配置读取方式 | `${环境变量名}` 读取；保证变量名一致 |
| 9 | 非 root 用户运行报"权限不足" | 目录/文件无非 root 用户读写权限 | `RUN mkdir -p /app && chown -R java-user:java-group /app` |
| 10 | Alpine 镜像启动失败 | musl libc 与 glibc 不兼容 | `RUN apk add --no-cache libc6-compat`；无法解决换 slim |

## 11. 核心要点

> 🎯 **核心要点**：
> - Dockerfile 铁律：Exec 形式（PID 1）、合并 RUN（减层）、COPY 优先（语义清晰）、`--no-install-recommends` + 缓存清理同 RUN；
> - 多阶段构建是**必做**（几百 MB → 一百多 MB）；
> - 缓存优化：不变在前、变化在后，pom 分离先行；
> - JVM 与容器内存匹配（Xmx ≤ 容器 80%），Java 8 需显式参数；
> - 安全六件套：非 root、只读根文件系统、资源限制、去 setuid、健康检查、禁 privileged；
> - 版本规范：`应用名:版本-环境`，禁止 latest。

## 12. 参考来源

- [Dockerfile 官方参考](https://docs.docker.com/reference/dockerfile/)
- [Docker 多阶段构建文档](https://docs.docker.com/build/building/multi-stage/)
- [Docker 构建缓存文档](https://docs.docker.com/build/cache/)
- [docker-maven-plugin（Spotify）](https://github.com/spotify/docker-maven-plugin)

---

**下一模块**：[04-镜像发布与仓库分发](04-镜像发布与仓库分发.md)　/　**返回总览**：[00-总览](00-Docker知识体系总览.md)
