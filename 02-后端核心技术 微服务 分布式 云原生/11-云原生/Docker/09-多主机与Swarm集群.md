# 09-多主机与 Swarm 集群
> Docker Machine + Swarm 是 Java 微服务生产级多主机部署的"轻量化最优方案"（比 K8s 简单易维护，贴合中小型 Java 团队）——核心价值"简化部署、保障稳定、提升效率"

## 📚 目录
1. [多主机编排全景](#1-多主机编排全景)
2. [Docker Machine：多主机统一管家](#2-docker-machine多主机统一管家)
3. [Swarm 集群初始化与节点管理](#3-swarm-集群初始化与节点管理)
4. [镜像分发与编排文件](#4-镜像分发与编排文件)
5. [服务部署与负载均衡验证](#5-服务部署与负载均衡验证)
6. [Swarm 常用命令全集](#6-swarm-常用命令全集)
7. [高可用与安全加固](#7-高可用与安全加固)
8. [CI/CD 联动](#8-cicd-联动)
9. [高频避坑清单](#9-高频避坑清单)
10. [核心要点](#10-核心要点)
11. [参考来源](#11-参考来源)

## 1. 多主机编排全景

**核心认知**：多容器 = "一个应用 = 多个容器"部署模式（组件解耦适配微服务、按需分配资源、独立扩容）；多主机 = 多容器部署到多台服务器（负载均衡提升可用性、突破单主机资源限制）。多容器是多主机的基础，多主机是规模化的保障。

**核心工具对比**：

| 工具 | 用途 |
|------|------|
| **Docker Swarm** | Docker 官方容器编排，轻量易维护，适合中小型 Java 团队，多主机容器的部署/调度/负载均衡 |
| **自定义 Registry** | 多主机间镜像同步，所有主机从私有仓库拉取镜像确保版本一致 |

**协同关系**：

```text
Docker Machine（前置环境搭建）→ 自定义 Registry（镜像分发）
→ Docker Compose（容器配置声明）→ Swarm 集群（多主机编排）
→ 构成"镜像构建 → 分发 → 环境搭建 → 容器编排 → 运维"全流程闭环
```

**环境准备（3 台 Linux 服务器）**：

| 主机角色 | IP 地址 | 核心配置 |
|----------|---------|----------|
| Swarm 管理节点（Manager） | 192.168.1.10 | Docker + Compose + Swarm + 自定义 Registry |
| Swarm 工作节点（Worker1） | 192.168.1.11 | Docker，加入 Swarm，能访问 Registry |
| Swarm 工作节点（Worker2） | 192.168.1.12 | Docker，加入 Swarm，能访问 Registry |

> ⚠️ 前置要求：所有主机网络互通；开放 Swarm 所需端口 **2377、7946、4789**；所有主机配置自定义 Registry 信任列表（避免 HTTP 推送/拉取失败）。

## 2. Docker Machine：多主机统一管家

**定位**：通过 SSH 连接多台主机自动安装、统一配置 Docker 引擎（镜像加速器、Registry 信任列表）。本质并非替代 Docker，而是管理工具。

```bash
# 安装（Linux 管理机，v0.16.2）
curl -L https://github.com/docker/machine/releases/download/v0.16.2/docker-machine-$(uname -s)-$(uname -m) -o /usr/local/bin/docker-machine
chmod +x /usr/local/bin/docker-machine
docker-machine version

# 前置：SSH 无密码登录
ssh-keygen -t rsa -P "" -f ~/.ssh/id_rsa
ssh-copy-id root@192.168.1.11
ssh-copy-id root@192.168.1.12

# 创建主机节点（核心命令）
docker-machine create \
  --driver generic \                  # generic 适用于物理机、虚拟机
  --generic-ip-address=192.168.1.11 \ # 目标主机 IP
  --generic-ssh-user=root \
  --generic-ssh-key ~/.ssh/id_rsa \
  --engine-registry-mirror=https://docker.mirrors.aliyun.com \  # 镜像加速器
  --engine-insecure-registries=192.168.1.10:5000 \  # 信任自定义 Registry
  worker1

# 管理命令全集
docker-machine ls                      # 查看所有主机节点状态
eval $(docker-machine env worker1)     # 进入 worker1 的 Docker 环境
docker ps                              # 查看 worker1 上的容器
docker-machine restart worker2         # 重启某台主机的 Docker 服务
docker-machine stop worker1            # 停止某台主机的 Docker 服务
docker-machine inspect worker1 | grep -E "RegistryMirror|InsecureRegistry"   # 查看配置
docker-machine rm worker2              # 移除主机节点
```

> 💡 **Java 场景专属技巧**：批量创建可写 Shell 脚本循环执行；Machine 配置的环境（Registry 信任、镜像加速器）会同步到主机 Docker 配置文件，**无需手动修改每台主机的 daemon.json**；创建主机务必配 `--engine-insecure-registries`，避免 Swarm 部署时镜像拉取失败。

## 3. Swarm 集群初始化与节点管理

```bash
# 步骤 1：初始化 Swarm 管理节点
eval $(docker-machine env manager)
docker swarm init --advertise-addr 192.168.1.10
# 执行成功后生成工作节点加入命令（需复制保存）：
# docker swarm join --token SWMTKN-1-xxx 192.168.1.10:2377

# 步骤 2：添加工作节点
eval $(docker-machine env worker1)
docker swarm join --token SWMTKN-1-xxx 192.168.1.10:2377
eval $(docker-machine env worker2)
docker swarm join --token SWMTKN-1-xxx 192.168.1.10:2377

# 步骤 3：验证集群状态（管理节点执行）
docker node ls
# 成功返回：3 个节点（1 个 Manager，2 个 Worker），状态均为 Ready
```

## 4. 镜像分发与编排文件

### 4.1 推送镜像到自定义 Registry

```bash
eval $(docker-machine env manager)

# 拉取基础镜像并推送
docker pull mysql:8.0
docker tag mysql:8.0 192.168.1.10:5000/mysql:8.0
docker push 192.168.1.10:5000/mysql:8.0
docker pull redis:6.2
docker tag redis:6.2 192.168.1.10:5000/redis:6.2
docker push 192.168.1.10:5000/redis:6.2

# 推送 Java 微服务镜像
docker tag springboot-demo:1.0.0 192.168.1.10:5000/springboot-demo:1.0.0
docker push 192.168.1.10:5000/springboot-demo:1.0.0
```

### 4.2 Swarm 适配的编排文件（docker-compose-swarm.yml）

```yaml
version: '3.8'
services:

  # 1. MySQL 服务（主从复制，高可用，部署在管理节点）
  mysql-master:
    image: 192.168.1.10:5000/mysql:8.0
    deploy:
      replicas: 1
      placement:
        constraints: [node.role == manager]   # 强制部署在管理节点
    environment:
      - MYSQL_ROOT_PASSWORD=123456
      - MYSQL_DATABASE=java_cluster_db
      - MYSQL_USER=java_dev
      - MYSQL_PASSWORD=123456
      - TZ=Asia/Shanghai
    volumes:
      - mysql-master-data:/var/lib/mysql
    networks:
      - java-swarm-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p123456"]
      interval: 10s
      timeout: 5s
      retries: 3

  # 2. Redis 服务（集群，3 个副本，分布在所有节点）
  redis:
    image: 192.168.1.10:5000/redis:6.2
    deploy:
      replicas: 3                          # 3 个副本，实现负载均衡
    volumes:
      - redis-data:/data
    networks:
      - java-swarm-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

  # 3. Spring Boot 微服务（多实例，负载均衡，部署在工作节点）
  springboot-app:
    image: 192.168.1.10:5000/springboot-demo:1.0.0
    deploy:
      replicas: 2                          # 2 个实例，分布在 2 个工作节点
      placement:
        constraints: [node.role == worker]
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
      restart_policy:
        condition: on-failure
      ports:
        - "8080:8080"                      # 所有节点的 8080 端口映射
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-master:3306/java_cluster_db?useSSL=true&serverTimezone=UTC
      - SPRING_DATASOURCE_USERNAME=java_dev
      - SPRING_DATASOURCE_PASSWORD=123456
      - SPRING_REDIS_CLUSTER_NODES=redis:6379
      - TZ=Asia/Shanghai
    depends_on:
      mysql-master:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - java-swarm-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 3

# 自定义 overlay 网络，实现多主机容器通信（Swarm 集群专用）
networks:
  java-swarm-network:
    driver: overlay
    attachable: true          # 允许手动连接到该网络

# 数据卷，实现数据持久化（集群内共享）
volumes:
  mysql-master-data:
  redis-data:
```

> 💡 **Java 后端专属要点**：`deploy` 为 Swarm 专属配置（副本数量、节点调度、资源限制）；`placement.constraints` 指定节点角色（Java 服务不占管理节点资源、MySQL 主节点部署管理节点确保稳定）；镜像全部从自定义 Registry 拉取；overlay 网络实现多主机通信，Java 服务通过服务名（mysql-master、redis）直连无需写 IP。

## 5. 服务部署与负载均衡验证

```bash
# 部署服务栈（stack 是 Swarm 中多容器的集合）
eval $(docker-machine env manager)
docker stack deploy -c docker-compose-swarm.yml java-cluster-stack

# 查看服务栈部署状态
docker stack ls
docker stack ps java-cluster-stack       # 查看各服务实例分布

# 查看 Java 服务日志，验证启动成功
docker service logs -f java-cluster-stack_springboot-app
```

**验证集群与负载均衡**：

| 验证项 | 方法 | 预期 |
|--------|------|------|
| 容器分布 | `docker stack ps java-cluster-stack` | Java 2 实例分布 worker1/worker2、Redis 3 实例分布 3 节点、MySQL 在 manager |
| 负载均衡 | 多次访问 `http://任意节点IP:8080/xxx` | 请求分发到不同实例（worker1/worker2 轮流处理） |
| 多容器联动 | 写 MySQL/Redis 后进容器验证 | 数据正常 |
| **故障恢复** | 手动停止 worker1 的 Java 容器 | Swarm 自动在 worker2 启动新实例，保持 2 实例（高可用验证） |

## 6. Swarm 常用命令全集

```bash
# ---- 集群管理 ----
docker swarm init --advertise-addr 192.168.1.10          # 初始化集群
docker swarm join --token SWMTKN-1-xxx 192.168.1.10:2377 # 节点加入集群
docker swarm join-token manager                          # 获取管理节点加入令牌
docker node ls                                           # 查看集群节点状态

# ---- 服务栈管理 ----
docker stack deploy -c docker-compose-swarm.yml java-cluster-stack  # 部署服务栈
docker stack ls                                         # 查看服务栈状态
docker stack ps java-cluster-stack                      # 查看各服务实例分布

# ---- 服务级操作 ----
docker service logs -f java-cluster-stack_springboot-app          # 查看服务日志
docker service scale java-cluster-stack_springboot-app=4          # 服务扩容（2→4）
docker service ps --no-trunc java-cluster-stack_springboot-app    # 查看版本历史
docker service rollback java-cluster-stack_springboot-app         # 回滚到上一个版本
docker service update --image 192.168.1.10:5000/springboot-demo:1.0.0 java-cluster-stack_springboot-app  # 滚动更新镜像

# ---- 网络 ----
docker network create -d overlay --opt encrypted java-swarm-network-encrypted  # 加密 overlay 网络
```

## 7. 高可用与安全加固

### 7.1 管理节点高可用（推荐 3 个管理节点，防单点故障）

```bash
# 在 manager 上获取管理节点加入令牌
docker swarm join-token manager
# 在新管理节点执行加入命令
docker swarm join --token SWMTKN-1-xxx 192.168.1.10:2377
# 验证
docker node ls    # 可看到多个 Manager 节点，状态为 Ready
```

### 7.2 敏感信息加密：Docker Secrets（Swarm 专属）

```bash
# 创建 Secrets（存储 MySQL 密码）
echo "123456" | docker secret create mysql-root-password -
```

```yaml
# 在 Compose 文件中引用 Secrets
services:
  mysql-master:
    environment:
      - MYSQL_ROOT_PASSWORD_FILE=/run/secrets/mysql-root-password
    secrets:
      - mysql-root-password
secrets:
  mysql-root-password:
    external: true
```

### 7.3 网络隔离与节点权限

```yaml
networks:
  java-swarm-network:
    driver: overlay
    internal: true      # 内部网络，禁止外部访问
```

- overlay 网络默认**不加密**，多主机间通信明文传输，需开启加密（`--opt encrypted`）；
- 按环境创建不同 overlay 网络实现环境隔离；
- 不同节点分配不同角色（管理节点仅调度、工作节点仅运行容器），限制操作权限避免误操作。

## 8. CI/CD 联动

```groovy
// Jenkinsfile（更新 Swarm 服务）
pipeline {
    agent any
    environment {
        REGISTRY_URL = "192.168.1.10:5000"
        IMAGE_NAME = "springboot-demo"
        IMAGE_VERSION = "1.0.0"
        SWARM_STACK_NAME = "java-cluster-stack"
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
        stage('Update Swarm Cluster') {
            steps {
                sh 'eval $(docker-machine env manager)'
                sh "docker service update --image ${REGISTRY_URL}/${IMAGE_NAME}:${IMAGE_VERSION} ${SWARM_STACK_NAME}_springboot-app"
            }
        }
    }
}
```

## 9. 高频避坑清单

| # | 现象 | 成因 | 解决方案 |
|---|------|------|----------|
| 1 | Machine 创建主机失败 "SSH connection failed" | 目标主机未开 SSH；无法无密码登录；防火墙未开 22 端口 | 开启 SSH；验证无密码登录；开放 22 端口 |
| 2 | 多容器通信失败 "connection refused" | 未入同一网络；启动顺序错误；端口映射错误 | 同一网络；depends_on 定义顺序；检查端口映射 |
| 3 | Swarm 节点加入失败 "timeout" | 网络不通；2377/7946/4789 未开放；管理节点 IP 错误 | 互相 ping 通；开放 Swarm 端口；核对 IP |
| 4 | 镜像拉取失败 "no such image" | 镜像未推送；节点未配信任列表；标签缺地址 | 推送镜像；Machine 重新配置；标签含"地址:端口/镜像名" |
| 5 | Java 服务无法连接依赖 "unknown host" | 未入同一 overlay；服务名不一致；依赖未就绪 | 同一 overlay；核对服务名；查依赖日志 |
| 6 | Swarm 扩容失败 "no resources available" | 工作节点资源不足；placement 约束错误；节点异常 | 检查资源；修改约束；查看节点状态 |

## 10. 核心要点

> 🎯 **核心要点**：
> - 四件套闭环：Machine（搭环境）→ Registry（分镜像）→ Compose（写配置）→ Swarm（做编排）；
> - Machine 一键同步环境：镜像加速器 + Registry 信任列表自动下发，无需手改 daemon.json；
> - Swarm 核心：`swarm init/join`（建群）→ `stack deploy`（部署）→ `service scale/update/rollback`（运维）；
> - `placement.constraints` 控制节点角色（MySQL 在 manager、Java 在工作节点）；
> - 高可用三板斧：3 管理节点、加密 overlay、Docker Secrets；
> - 故障自愈验证：杀掉容器 → Swarm 自动重建，这是集群的价值证明。

## 11. 参考来源

- [Docker Swarm 官方文档](https://docs.docker.com/engine/swarm/)
- [Docker Machine 官方文档](https://docs.docker.com/machine/)
- [Docker Secrets 文档](https://docs.docker.com/engine/swarm/secrets/)
- [Docker Stack 部署文档](https://docs.docker.com/engine/swarm/stack-deploy/)

---

**下一模块**：[10-Docker与Kubernetes衔接](10-Docker与Kubernetes衔接.md)　/　**返回总览**：[00-总览](00-Docker知识体系总览.md)
