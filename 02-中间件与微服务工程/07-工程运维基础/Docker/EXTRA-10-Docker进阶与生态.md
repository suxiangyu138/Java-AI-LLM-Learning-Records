# 10 - Docker 进阶与生态

## 10.1 Docker Compose 生产部署

### 零停机部署 (Zero-downtime Deployment)

```bash
# docker-compose.yml 中配置多个副本
services:
  web:
    image: myapp:${TAG}
    deploy:
      replicas: 3
      update_config:
        parallelism: 1        # 每次更新 1 个副本
        delay: 10s            # 间隔 10 秒
        order: start-first    # 先启动新的，再停旧的
        failure_action: rollback
```

```bash
# 滚动更新
TAG=v2.0 docker compose up -d --scale web=4
sleep 10
TAG=v2.0 docker compose up -d --scale web=3
```

### 使用 Watchtower 自动更新

```yaml
services:
  watchtower:
    image: containrrr/watchtower
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    command: --interval 300 --cleanup my-app my-db
    restart: unless-stopped
```

## 10.2 Docker Swarm 原生集群

Docker Swarm 是 Docker 内置的集群管理工具，适合中小规模部署。

### 核心概念

| 概念 | 说明 |
|---|---|
| Node | Swarm 集群中的一台 Docker 主机 |
| Manager Node | 管理节点，负责调度和集群状态维护 |
| Worker Node | 工作节点，运行服务容器 |
| Service | 在集群中运行的服务定义（类似 `docker run`） |
| Task | 单个容器实例，由 Service 创建 |
| Stack | 一组相关的 Service（类似 `docker compose`） |

### 快速上手

```bash
# ====== 初始化 Swarm ======
# 在 Manager 节点上
docker swarm init --advertise-addr 192.168.1.100

# 输出包含 worker 加入命令，类似：
# docker swarm join --token SWMTKN-1-xxx 192.168.1.100:2377

# ====== Worker 节点加入 ======
# 在其他机器上执行上面输出的 join 命令

# ====== 查看集群状态 ======
docker node ls

# ====== 部署服务 ======
docker service create \
  --name web \
  --replicas 3 \
  --publish 80:3000 \
  --update-parallelism 1 \
  --update-delay 10s \
  myapp:latest

# ====== 查看服务 ======
docker service ls
docker service ps web     # 查看每个副本的运行节点

# ====== 缩放 ======
docker service scale web=5

# ====== 滚动更新 ======
docker service update --image myapp:v2.0 web

# ====== 使用 Stack 部署 ======
# docker-stack.yml 与 docker-compose.yml 语法兼容
docker stack deploy -c docker-stack.yml my-stack

# ====== 离开 Swarm ======
docker swarm leave --force   # Manager 需要 --force
```

### docker-stack.yml 示例

```yaml
version: "3.8"

services:
  web:
    image: myapp:${TAG:-latest}
    ports:
      - "80:3000"
    environment:
      - NODE_ENV=production
    deploy:
      replicas: 3
      update_config:
        parallelism: 1
        delay: 10s
        failure_action: rollback
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
      resources:
        limits:
          cpus: "0.5"
          memory: 256M

  redis:
    image: redis:7-alpine
    deploy:
      replicas: 1
      placement:
        constraints:
          - node.role == worker
```

### Swarm vs Kubernetes

| 特性 | Docker Swarm | Kubernetes |
|---|---|---|
| 复杂度 | 低，5 分钟上手 | 高，学习曲线陡峭 |
| 安装 | Docker 内置 | 需要额外安装配置 |
| 规模 | 中小规模 | 大规模 |
| 自动伸缩 | 手动 | 内置 HPA |
| 服务发现 | 内置 DNS | CoreDNS |
| 负载均衡 | 内置 Mesh Routing | Ingress / Service |
| 生态 | 小 | 极大 |
| 适用场景 | 简单部署、小团队 | 复杂微服务、大企业 |

## 10.3 Docker 安全最佳实践

### 镜像安全

```dockerfile
# 1. 使用特定版本标签，不用 latest
FROM python:3.11.7-slim-bookworm

# 2. 使用非 root 用户
RUN groupadd -r app && useradd -r -g app app
USER app

# 3. 使用 COPY 而非 ADD
COPY . /app

# 4. 最小化攻击面
# - 使用 slim/alpine 镜像
# - 只安装必需的包
# - 清理临时文件
```

### 运行时安全

```bash
# 1. 以只读模式运行
docker run --read-only --tmpfs /tmp nginx:alpine

# 2. 限制资源
docker run -m 256m --cpus 0.5 --pids-limit 100 nginx:alpine

# 3. 禁止提权
docker run --security-opt=no-new-privileges nginx:alpine

# 4. 限制系统调用
docker run --cap-drop=ALL --cap-add=NET_BIND_SERVICE nginx:alpine

# 5. 使用自定义 seccomp/AppArmor
docker run --security-opt seccomp=/path/to/seccomp.json nginx:alpine
```

### 扫描镜像漏洞

```bash
# Docker Scout（Docker 内置）
docker scout quickview nginx:latest
docker scout cves nginx:latest

# Trivy（开源）
trivy image nginx:latest
trivy image --severity HIGH,CRITICAL myapp:latest

# Snyk
snyk container test myapp:latest
```

### Secrets 管理

```bash
# Docker Swarm Secrets
echo "MySecretPassword" | docker secret create db_password -
docker service create --secret db_password --name db mysql:8.0

# Compose 中使用 .env
# docker-compose.yml
services:
  db:
    image: mysql:8.0
    secrets:
      - db_password

secrets:
  db_password:
    file: ./secrets/db_password.txt   # 不要提交到 Git！
```

## 10.4 日志管理

### 集中式日志

```yaml
# docker-compose.yml - ELK 技术栈
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"

  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    ports:
      - "5000:5000"

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    ports:
      - "5601:5601"

  # 使用 Fluentd 收集 Docker 日志
  fluentd:
    image: fluent/fluentd:v1.16
    volumes:
      - ./fluentd.conf:/fluentd/etc/fluent.conf
    ports:
      - "24224:24224"
```

### Docker Logging Driver

```bash
# 使用 json-file + 限制大小（默认驱动）
docker run --log-opt max-size=10m --log-opt max-file=3 nginx

# 使用 Fluentd 驱动
docker run \
  --log-driver=fluentd \
  --log-opt fluentd-address=localhost:24224 \
  --log-opt tag=docker.{{.Name}} \
  nginx
```

## 10.5 监控与健康检查

### 内置健康检查

```dockerfile
# Dockerfile 中定义健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:3000/health || exit 1
```

### 监控工具链

```yaml
# Prometheus + Grafana + cAdvisor + Node Exporter
services:
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    volumes:
      - grafana-data:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin

  cadvisor:
    image: gcr.io/cadvisor/cadvisor:latest
    volumes:
      - /:/rootfs:ro
      - /var/run:/var/run:ro
      - /sys:/sys:ro
      - /var/lib/docker/:/var/lib/docker:ro
    ports:
      - "8080:8080"

  node-exporter:
    image: prom/node-exporter:latest
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
    ports:
      - "9100:9100"

volumes:
  prometheus-data:
  grafana-data:
```

## 10.6 私有镜像仓库

### 使用 Registry 官方镜像

```bash
# 启动私有仓库
docker run -d \
  --name registry \
  --restart unless-stopped \
  -p 5000:5000 \
  -v registry-data:/var/lib/registry \
  registry:2

# 推送镜像到私有仓库
docker tag myapp:latest localhost:5000/myapp:latest
docker push localhost:5000/myapp:latest

# 拉取
docker pull localhost:5000/myapp:latest
```

### 使用 Harbor（企业级）

```bash
# 下载 Harbor
wget https://github.com/goharbor/harbor/releases/latest/download/harbor-online-installer.tgz
tar xzf harbor-online-installer.tgz
cd harbor

# 配置 harbor.yml（修改 hostname、密码等）
cp harbor.yml.tmpl harbor.yml

# 安装
./install.sh

# 访问 https://your-host
# Harbor 提供：镜像管理、漏洞扫描、签名、RBAC、镜像复制等
```

## 10.7 性能优化清单

```bash
# 1. 使用 overlay2 存储驱动
docker info | grep "Storage Driver"

# 2. 限制容器日志大小
# /etc/docker/daemon.json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}

# 3. 定期清理
docker system prune -a -f --volumes

# 4. 使用 buildkit 加速构建
export DOCKER_BUILDKIT=1
docker build -t myapp .

# 5. 拉取时使用 --platform 匹配架构
docker pull --platform linux/amd64 nginx

# 6. 避免将大型文件放入镜像（使用 Volume）
# 7. 使用 .dockerignore 减少构建上下文
# 8. 合并 RUN 指令减少镜像层数
# 9. 使用多阶段构建减小镜像体积
```

## 10.8 常用调试命令速查

```bash
# 查看 Docker 磁盘使用情况
docker system df
docker system df -v    # 详细

# 查看实时事件流
docker events

# 查看容器内的文件变化（与镜像对比）
docker diff my-container

# 导出容器文件系统
docker export my-container > container.tar

# 从 tar 导入为镜像
docker import container.tar my-image:v1

# 查看端口映射
docker port my-container

# 跟踪容器内系统调用
docker run --rm -it --cap-add=SYS_PTRACE --pid=host \
  --net=host --privileged \
  strace -p $(docker inspect -f '{{.State.Pid}}' my-container)

# 性能分析
docker stats --no-stream --format \
  "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"
```

## 10.9 Docker 生态地图

```
┌──────────────────────────────────────────────────────────────┐
│                     Docker 技术生态                           │
├───────────────┬──────────────────┬───────────────────────────┤
│   容器运行时   │     编排调度       │       镜像管理             │
│   Docker      │   Docker Swarm    │   Docker Hub              │
│   containerd  │   Kubernetes      │   Harbor                  │
│   CRI-O       │   Nomad           │   Quay                    │
├───────────────┼──────────────────┼───────────────────────────┤
│   监控告警     │     日志收集       │       CI/CD              │
│   Prometheus  │   ELK Stack       │   GitHub Actions          │
│   Grafana     │   Loki            │   GitLab CI               │
│   cAdvisor    │   Fluentd         │   Jenkins                 │
│   Datadog     │   Vector          │   Drone CI                │
├───────────────┼──────────────────┼───────────────────────────┤
│   网络         │     存储          │       安全                │
│   Traefik     │   Ceph            │   Trivy                   │
│   Cilium      │   Longhorn        │   Falco                   │
│   Calico      │   Portworx        │   Snyk                    │
│   Nginx       │   Rook            │   Aqua                    │
└───────────────┴──────────────────┴───────────────────────────┘
```

## 10.10 学习路径建议

```
第1周：Docker 基础
  ├── 安装 Docker，跑通 hello-world
  ├── 理解镜像、容器、仓库概念
  ├── 练习 docker run / ps / exec / logs
  └── 用 Docker 跑一个 Nginx + 静态页面

第2周：构建镜像
  ├── 学习 Dockerfile 指令
  ├── 为你的项目写第一个 Dockerfile
  ├── 理解镜像分层和缓存优化
  └── 练习多阶段构建

第3周：数据与网络
  ├── Volume 和 Bind Mount
  ├── 自定义网络，容器间通信
  ├── 数据库容器化 + 数据持久化
  └── 实战：WordPress + MySQL

第4周：Docker Compose
  ├── 编写 docker-compose.yml
  ├── 多服务编排
  ├── 环境变量管理
  └── 实战：完整 Web 应用栈

第5周：进阶
  ├── Docker Swarm 入门
  ├── 镜像安全扫描
  ├── 日志与监控
  └── CI/CD 集成
```

---

> **恭喜！** 核心 10 个模块已完成。建议继续阅读以下生态扩展文档：
> - [11-Docker生态工具链详解](11-Docker生态工具链详解.md) — Lazydocker、Dive、Harbor、Prometheus+Grafana、CI/CD
> - [12-Docker日常开发工作流](12-Docker日常开发工作流.md) — 热重载、调试、数据库管理、环境统一

Docker 是一门**实践性极强**的技术，多动手是最好的学习方式。
