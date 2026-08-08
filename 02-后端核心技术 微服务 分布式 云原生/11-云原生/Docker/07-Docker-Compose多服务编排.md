# 07-Docker Compose 多服务编排
> 通过 YAML 文件"声明"多容器的最终运行状态——MySQL/Redis/Spring Boot 一键启动，核心价值"一致性、高效性、可维护性"

## 📚 目录
1. [核心认知：声明式环境](#1-核心认知声明式环境)
2. [compose 文件结构](#2-compose-文件结构)
3. [核心命令全集](#3-核心命令全集)
4. [服务间通信与健康检查](#4-服务间通信与健康检查)
5. [完整示例：Spring Boot + MySQL + Redis](#5-完整示例spring-boot--mysql--redis)
6. [多环境适配](#6-多环境适配)
7. [生产环境配置](#7-生产环境配置)
8. [CI/CD 联动](#8-cicd-联动)
9. [高频避坑点](#9-高频避坑点)
10. [核心要点](#10-核心要点)
11. [参考来源](#11-参考来源)

## 1. 核心认知：声明式环境

**本质**：通过 YAML 配置文件（docker-compose.yml）"声明"多容器的最终运行状态——无需手动执行 `docker run` 逐一启动容器，Compose 自动解析配置、调度容器启动，确保最终运行状态与声明一致。

**声明式 vs 命令式对比**：

| 维度 | 命令式（手动 docker run） | 声明式（Docker Compose YAML） |
|------|--------------------------|------------------------------|
| 核心逻辑 | 手动执行每一步命令，告诉 Docker"如何做" | 声明最终运行状态，告诉 Docker"要什么" |
| 配置管理 | 命令分散，无统一管理，易遗漏 | 配置集中 YAML，可版本控制（提交 Git） |
| 环境一致性 | 不同环境命令可能不一致 | 统一 YAML，所有环境复用 |
| 维护成本 | 修改配置需重新执行所有命令 | 修改 YAML 后一键重启 |
| Java 场景 | 适合单容器调试 | **完美适配微服务多组件联动** |

**四大核心价值（Java 后端视角）**：① 简化多容器部署、提升开发效率；② 保障环境一致性（YAML 提交 Git 全团队复用）；③ 贴合微服务解耦理念；④ 可扩展性强，适配本地开发/功能测试/小型生产。

## 2. compose 文件结构

| 声明 | 说明 |
|------|------|
| `version: '3.8'` | 版本声明，v3.8 适配 Docker 19.03+（推荐 Compose v2.0+） |
| `services` | 服务声明：每个服务对应一个容器（image/container_name/restart/environment/ports/volumes/networks/healthcheck/depends_on） |
| `networks` | 网络声明：自定义桥接网络（driver: bridge），服务间通信 |
| `volumes` | 数据卷声明：集中管理所有数据卷（mysql-data/redis-data/springboot-logs） |

**关键配置项**：

| 配置项 | 说明 |
|--------|------|
| `build` | 从 Dockerfile 构建（context 是构建上下文路径） |
| `image` | 指定使用的镜像（构建后自动打标签） |
| `ports` | `"宿主机端口:容器端口"` |
| `volumes` | 命名卷（`mysql-data:/var/lib/mysql`）或绑定挂载（`./sql/init.sql:...`） |
| `environment` | 环境变量，支持 `${VARIABLE}` 引用 `.env` 文件 |
| `depends_on` | 控制启动顺序（**不等服务就绪，只确保容器启动**） |
| `networks` | 自定义网络，服务间通过服务名通信 |
| `restart` | 重启策略，同 Docker `--restart` |
| `healthcheck` | 健康检查 |

## 3. 核心命令全集

```bash
# 在 docker-compose.yml 所在目录执行

docker-compose up -d                     # 1. 启动所有服务（后台运行，核心命令）
docker-compose ps                        # 2. 查看所有服务的运行状态
docker-compose logs -f springboot-service # 3. 查看 Java 服务日志（实时跟踪）
docker-compose restart springboot-service # 4. 重启某个服务（无需重启整个环境）
docker-compose stop                      # 5. 停止整个环境（保留容器和数据）
docker-compose down                      # 6. 停止并删除环境（容器/网络删除，数据卷保留）
docker-compose down -v                   # 7. 停止并删除容器、网络和数据卷（⚠️ 会删数据库数据）
docker-compose up -d --build             # 8. 重新构建镜像并启动（Java 代码修改后）
docker-compose exec springboot-service bash   # 9. 进入服务容器（调试）
docker-compose config                    # 10. 查看最终配置
docker-compose ps --filter "health=healthy"  # 11. 查看健康检查通过的服务
```

> 💡 修改了 Spring Boot 代码后只需 `docker-compose up -d --build` 即可重新构建镜像并重启 Java 服务，无需手动删旧容器、重启整个环境。

```bash
# Compose 安装（二进制方式，CentOS/Ubuntu 通用）
curl -L "https://github.com/docker/compose/releases/download/v2.20.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
docker-compose --version
```

## 4. 服务间通信与健康检查

- 所有服务加入同一自定义网络，通过**服务名称**直接通信（Java 服务配置 MySQL 地址为 `mysql-service`、Redis 地址为 `redis-service`），无需写 IP；
- **depends_on + healthcheck 组合**：仅 depends_on 只保证启动顺序，无法确保依赖组件"正常可用"；结合 healthcheck 可让 Java 服务等依赖组件健康检查通过后再启动。

```yaml
depends_on:
  mysql-service:
    condition: service_healthy    # 等待 MySQL 健康检查通过
  redis-service:
    condition: service_healthy
```

**验证联动三步**：

```bash
# 1. 状态验证
docker-compose ps                  # 全部 Up + healthy
# 2. 应用验证
curl http://服务器IP:8080/actuator/health   # 返回 UP
# 3. 数据验证
docker-compose exec mysql-service mysql -u root -p123456
use java_demo_db; select * from user;
docker-compose exec redis-service redis-cli
keys *                            # 查看缓存 key
```

## 5. 完整示例：Spring Boot + MySQL + Redis

```yaml
# 版本声明：v3.8 适配 Docker 19.03+
version: '3.8'

# 服务声明：每个服务对应一个容器
services:

  # 1. MySQL 服务（Java 服务依赖的数据库）
  mysql-service:
    image: 192.168.1.10:5000/mysql:8.0      # 优先使用自定义 Registry 镜像
    container_name: mysql-container
    restart: always
    environment:
      - MYSQL_ROOT_PASSWORD=123456          # 数据库 root 密码
      - MYSQL_DATABASE=java_demo_db         # Java 服务使用的数据库
      - MYSQL_USER=java_dev
      - MYSQL_PASSWORD=123456
      - TZ=Asia/Shanghai                    # 时区一致，避免时间错乱
    ports:
      - "3306:3306"
    volumes:
      - /data/mysql/data:/var/lib/mysql     # 数据存储目录
      - /data/mysql/conf:/etc/mysql/conf.d  # 配置文件目录
    networks:
      - java-demo-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p123456"]
      interval: 10s
      timeout: 5s
      retries: 3

  # 2. Redis 服务（Java 服务依赖的缓存）
  redis-service:
    image: 192.168.1.10:5000/redis:6.2
    container_name: redis-container
    restart: always
    environment:
      - TZ=Asia/Shanghai
    ports:
      - "6379:6379"
    volumes:
      - /data/redis/data:/data
    networks:
      - java-demo-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

  # 3. Spring Boot 服务（核心业务服务）
  springboot-service:
    image: 192.168.1.10:5000/springboot-demo:1.0.0
    container_name: springboot-container
    restart: always
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-service:3306/java_demo_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=java_dev
      - SPRING_DATASOURCE_PASSWORD=123456
      - SPRING_REDIS_HOST=redis-service    # 引用服务名称，无需写 IP
      - SPRING_REDIS_PORT=6379
      - TZ=Asia/Shanghai
    ports:
      - "8080:8080"
    depends_on:
      mysql-service:
        condition: service_healthy
      redis-service:
        condition: service_healthy
    networks:
      - java-demo-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 3

# 网络声明：自定义桥接网络
networks:
  java-demo-network:
    driver: bridge

# 数据卷声明：集中管理
volumes:
  mysql-data:
  redis-data:
  springboot-logs:
```

## 6. 多环境适配

**多配置文件方案**：核心文件 + 环境覆盖文件，`-f` 参数叠加启动，无需改核心 YAML：

```text
docker-compose.yml          # 核心配置（所有环境共用）
docker-compose.dev.yml      # 开发环境覆盖（镜像版本、环境变量）
docker-compose.test.yml     # 测试环境覆盖
docker-compose.prod.yml     # 生产环境覆盖（安全、资源限制）
```

```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

**开发环境 vs 生产环境差异**：

| 维度 | 开发环境 | 生产环境 |
|------|---------|---------|
| 源码 | 挂载源码实现热重载（`./src:/app/src`） | 不挂载源码 |
| 调试 | 开启远程调试端口（5005 + JAVA_TOOL_OPTIONS） | 关闭调试端口 |
| 镜像 | Dockerfile.dev | 固定版本镜像（禁止 latest） |
| 配置 | dev profile | prod profile + 环境变量注入密码 |

## 7. 生产环境配置

```yaml
# docker-compose.prod.yml（要点版）
version: '3.8'
services:
  springboot-service:
    image: 192.168.1.10:5000/springboot-demo:1.0.0
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    deploy:
      resources:
        limits: { cpus: '1', memory: 1G }        # 限制 CPU/内存
        reservations: { cpus: '0.5', memory: 512M }
    ports:
      - "80:8080"                                # 生产用 80 端口
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 3s
      retries: 5
  mysql-service:
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}   # 从环境变量读取，避免硬编码
    volumes:
      - /data/prod/mysql/data:/var/lib/mysql
```

```yaml
# 网络隔离（生产）
networks:
  java-prod-network:
    driver: bridge
    internal: true            # 内部网络，禁止外部访问
    ipam:
      config:
        - subnet: 172.18.0.0/16
```

```yaml
# 日志挂载（供 ELK 收集）
services:
  springboot-service:
    volumes:
      - /data/prod/springboot/logs:/app/logs
```

> ⚠️ 安全加固三要点：① 敏感信息不写 YAML（环境变量或 Docker Secrets）；② 网络隔离（internal 内部网络）；③ 镜像必须来自自定义 Registry 且经漏洞扫描。

## 8. CI/CD 联动

```groovy
// Jenkinsfile（更新 Compose 环境）
pipeline {
    agent any
    environment {
        REGISTRY_URL = "192.168.1.10:5000"
        IMAGE_NAME = "springboot-demo"
        IMAGE_VERSION = "1.0.0"
    }
    stages {
        stage('Pull Code') {
            steps { git url: 'https://github.com/your-username/your-java-project.git', branch: 'master' }
        }
        stage('Build & Push Image') {
            steps {
                sh 'mvn clean package docker:build'
                sh "docker tag ${IMAGE_NAME}:${IMAGE_VERSION} ${REGISTRY_URL}/${IMAGE_NAME}:${IMAGE_VERSION}"
                sh "docker login ${REGISTRY_URL} -u java-dev -p 123456"
                sh "docker push ${REGISTRY_URL}/${IMAGE_NAME}:${IMAGE_VERSION}"
            }
        }
        stage('Update Compose Environment') {
            steps {
                sh 'cd /data/compose && docker-compose pull springboot-service'
                sh 'cd /data/compose && docker-compose up -d springboot-service'
            }
        }
    }
}
```

## 9. 高频避坑点

| # | 现象 | 成因 | 方案 |
|---|------|------|------|
| 1 | Java 服务启动失败"无法连接 MySQL/Redis" | 未配 depends_on/healthcheck；服务名不一致；未入同一网络 | 配 depends_on+healthcheck；核对服务名；同一网络 |
| 2 | 多环境切换配置不生效 | 启动命令未指定环境文件；环境文件未覆盖核心配置 | `-f` 指定核心+环境文件；核对环境变量 key |
| 3 | 日志提示"时区不一致" | 各容器时区配置不一致影响订单时间 | 所有容器加 `- TZ=Asia/Shanghai`，JVM 时区与容器一致 |
| 4 | 数据卷挂载失败 "permission denied" | 本地目录权限不足、路径不存在 | `chmod 777 /data/mysql/data`；`mkdir -p` |
| 5 | CI/CD 更新失败"镜像拉取失败" | Registry 未开放权限；标签未含地址；未配信任列表 | 开放权限；标签含"地址:端口/镜像名:版本号"；加信任列表 |

## 10. 核心要点

> 🎯 **核心要点**：
> - 声明式 vs 命令式：告诉 Docker"要什么"而非"如何做"；
> - `depends_on` 只管启动顺序，**`condition: service_healthy` 才管"就绪"**；
> - 服务间通信用**服务名**（`jdbc:mysql://mysql-service:3306`），与微服务容器名通信同理；
> - 日常三命令：`up -d`（启动）、`logs -f`（日志）、`restart <服务>`（单服务重启）；
> - 多环境用 `-f` 叠加文件；生产必配资源限制、密码环境变量化、internal 网络；
> - 可逐步过渡 K8s，但声明式配置、多容器协同的核心逻辑不变。

## 11. 参考来源

- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [Compose 文件参考（services/networks/volumes）](https://docs.docker.com/compose/compose-file/)
- [Compose 健康检查（depends_on condition）](https://docs.docker.com/compose/compose-file/05-services/)

---

**下一模块**：[08-Docker安全与资源限制](08-Docker安全与资源限制.md)　/　**返回总览**：[00-总览](00-Docker知识体系总览.md)
