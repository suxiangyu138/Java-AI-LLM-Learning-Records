# 11 - Docker 生态工具链详解

> 补充自 01-10 系列文档，深入 Docker 周边核心工具。

## 11.1 终端管理神器：Lazydocker

Lazydocker 是一个 Go 编写的 Docker 终端管理工具，可以在一个界面中管理容器、镜像、卷、网络等。

### 安装

```bash
# Windows (Scoop)
scoop install lazydocker

# macOS
brew install lazydocker

# Linux
curl https://raw.githubusercontent.com/jesseduffield/lazydocker/master/scripts/install_update_linux.sh | bash

# 或通过 Go 安装
go install github.com/jesseduffield/lazydocker@latest
```

### 使用

```bash
# 启动
lazydocker
```

### 快捷键速查

| 按键 | 功能 |
|---|---|
| `tab` | 切换面板（左侧资源列表 ↔ 右侧详情） |
| `↑↓` | 在列表中移动 |
| `enter` | 进入选中项 / 聚焦到某个面板 |
| `d` | 删除选中的容器/镜像/卷 |
| `r` | 重启容器 |
| `s` | 停止容器 |
| `p` | 暂停/恢复容器 |
| `e` | 进入容器 shell |
| `l` | 查看容器日志 |
| `[ / ]` | 切换容器 tab（日志/状态/配置等） |
| `b` | 批量操作模式 |
| `x` | 打开菜单（危险操作确认） |
| `esc` | 返回上一级 |
| `?` | 帮助（显示所有快捷键） |
| `q` | 退出 |

### 实际操作流程

```
lazydocker 主界面布局：

┌────────────┬────────────────────────────────────────┐
│ Containers │  Container: my-nginx                    │
│  my-nginx  │  ┌──────────────────────────────────┐  │
│  my-mysql  │  │ Stats  │ Logs │ Config │ Top     │  │
│  my-redis  │  │────────┼──────┼────────┼─────────│  │
│            │  │ 日志实时滚动...                    │  │
├────────────┤  └──────────────────────────────────┘  │
│ Images     │                                         │
│  nginx     │  环境变量：PATH=/usr/local/...          │
│  mysql     │  端口映射：0.0.0.0:8080 → 80            │
│  redis     │  CPU: 2.3%  MEM: 15.2MB                │
├────────────┤                                         │
│ Volumes    │                                         │
│  db-data   │                                         │
├────────────┤                                         │
│ Networks   │                                         │
│  bridge    │                                         │
└────────────┴────────────────────────────────────────┘
```

---

## 11.2 镜像层分析：Dive

Dive 可以逐层分析 Docker 镜像，查看每层的内容、大小变化，帮助优化镜像体积。

### 安装

```bash
# macOS
brew install dive

# Windows (Scoop)
scoop install dive

# Linux (直接下载)
wget https://github.com/wagoodman/dive/releases/latest/download/dive_linux_amd64.rpm
sudo rpm -ivh dive_linux_amd64.rpm
```

### 使用

```bash
# 分析本地镜像
dive nginx:latest

# 构建并立即分析
dive build -t myapp:latest .

# 使用 CI 模式（非交互，用于流水线）
CI=true dive nginx:latest
```

### 界面与快捷键

```
Dive 界面布局：

┌──────────────────────┬─────────────────────────────┐
│ Layer List (左边)     │ Layer Details (右边)         │
├──────────────────────┼─────────────────────────────┤
│ Layer #1  5MB        │ ┌─────────────────────────┐ │
│ Layer #2  3MB        │ │ 文件变更：               │ │
│ Layer #3  120MB ← !  │ │ A /app/node_modules/... │ │
│ Layer #4  2MB        │ │ M /app/package.json     │ │
│ Layer #5  50MB       │ │ D /tmp/build-cache/     │ │
│                      │ │                         │ │
│ 潜在浪费：180MB      │ │ 当前层大小：120MB       │ │
│ 效率评分：72%        │ │ 文件数：3842            │ │
└──────────────────────┴─────────────────────────────┘
```

| 快捷键 | 功能 |
|---|---|
| `tab` | 切换左右面板 |
| `↑↓` | 在层列表中移动 |
| `ctrl+u` | 仅显示未修改的文件 |
| `ctrl+a` | 显示所有文件 |
| `ctrl+l` | 仅显示当前层的变更 |
| `space` | 折叠/展开目录 |
| `ctrl+space` | 全部折叠 |
| `q` | 退出 |

### CI 集成

```yaml
# .github/workflows/dive.yml
name: Image Size Check

on: [pull_request]

jobs:
  dive:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build image
        run: docker build -t myapp:${{ github.sha }} .

      - name: Dive analysis
        uses: wagoodman/dive-action@v1
        with:
          image: myapp:${{ github.sha }}
          config: |
            rules:
              lowestEfficiency: 0.85
              highestWastedBytes: 50MB
```

### 常见优化场景

```bash
# 场景1：发现某个 COPY . . 层引入了几百MB
# 解决：添加 .dockerignore

# 场景2：发现 RUN apt install 后没有清理缓存
# 解决：加上 rm -rf /var/lib/apt/lists/*

# 场景3：发现 node_modules 里有 devDependencies
# 解决：RUN npm ci --only=production，或多阶段构建

# 场景4：发现废弃的大文件残留在镜像层里
# 解决：同一层 RUN 里完成下载-使用-删除
```

---

## 11.3 企业级镜像仓库：Harbor

Harbor 是 CNCF 毕业项目，提供比 Docker Registry 更丰富的企业功能。

### 核心功能

| 功能 | 说明 |
|---|---|
| **RBAC** | 基于角色的访问控制（项目、用户、角色） |
| **漏洞扫描** | 集成 Trivy，自动扫描镜像 CVE |
| **签名验证** | 支持 Notary，镜像来源可信 |
| **镜像复制** | 跨数据中心同步镜像 |
| **垃圾回收** | 自动清理未引用的镜像层 |
| **审计日志** | 记录所有操作日志 |
| **WebHook** | 镜像推送/删除事件通知 |
| **OCI 兼容** | 支持 Helm Chart、CNAB 等制品 |

### Docker Compose 快速部署

```yaml
# harbor-docker-compose.yml
services:
  harbor:
    image: goharbor/harbor:v2.10.0
    container_name: harbor
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./harbor-data:/data
      - ./harbor.yml:/harbor.yml
```

### 项目与权限模型

```
Harbor 权限模型：

Organization
  └── Project "backend"          ← 项目 = 镜像集合 + 权限边界
        ├── 成员：dev-team (开发者，可 push/pull)
        ├── 成员：ops-team  (维护者，可管理配置)
        ├── 成员：ci-bot     (访客，仅 pull)
        │
        ├── Repository: backend/api        ← 镜像仓库
        ├── Repository: backend/worker
        └── Repository: backend/cronjob

  └── Project "frontend"
        ├── Repository: frontend/web
        └── Repository: frontend/nginx
```

### 镜像漏洞扫描

```bash
# 手动触发扫描
docker login harbor.company.com
docker tag myapp:v1 harbor.company.com/project/myapp:v1
docker push harbor.company.com/project/myapp:v1

# Harbor Web UI → 项目 → 仓库 → 扫描
# 自动展示 CVE 列表和严重等级

# 设置策略：存在高危漏洞时阻止拉取
# 项目 → 配置 → 部署安全性 → 阻止高危漏洞镜像运行
```

### 镜像复制（异地容灾）

```yaml
# Harbor 复制规则
# Web UI → 仓库管理 → 复制 → 新建规则
# 将 project/myapp 自动同步到异地 Harbor 实例
```

---

## 11.4 监控体系：Prometheus + Grafana + cAdvisor

### 架构

```
┌──────────────┐     ┌──────────────┐     ┌──────────┐
│  cAdvisor     │────▶│  Prometheus  │────▶│  Grafana │
│  (容器指标)    │     │  (采集+存储)  │     │  (可视化)  │
└──────────────┘     └──────────────┘     └──────────┘
        │                    │
┌───────┴───────┐    ┌───────┴───────┐
│ Node Exporter │    │ AlertManager  │
│ (主机指标)     │    │ (告警管理)     │
└───────────────┘    └───────────────┘
```

### Prometheus 配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # Docker 宿主机指标
  - job_name: "node"
    static_configs:
      - targets: ["node-exporter:9100"]

  # 容器指标
  - job_name: "cadvisor"
    static_configs:
      - targets: ["cadvisor:8080"]

  # Docker Engine 指标（需开启 metrics-addr）
  - job_name: "docker"
    static_configs:
      - targets: ["host.docker.internal:9323"]
```

### Grafana Dashboard 导入

```bash
# 常用 Dashboard ID（导入方法：Grafana → + → Import）
# 193   Docker 宿主机监控（综合）
# 14282 Docker 容器详细监控
# 11600 Docker Swarm 监控
# 1860  Node Exporter 主机监控
# 893   Docker Engine 指标
```

### 告警规则示例

```yaml
# prometheus-alerts.yml
groups:
  - name: docker
    rules:
      - alert: ContainerDown
        expr: time() - container_last_seen{name!=""} > 60
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "容器 {{ $labels.name }} 已停止超过 1 分钟"

      - alert: ContainerHighCPU
        expr: rate(container_cpu_usage_seconds_total[5m]) * 100 > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "容器 {{ $labels.name }} CPU 使用率 > 80%"

      - alert: ContainerHighMemory
        expr: container_memory_usage_bytes / container_spec_memory_limit_bytes > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "容器 {{ $labels.name }} 内存使用率 > 90%"
```

---

## 11.5 CI/CD 集成实战

### Jenkins + Docker Pipeline

```groovy
// Jenkinsfile
pipeline {
    agent any

    environment {
        REGISTRY = 'harbor.company.com/project'
        IMAGE_NAME = 'myapp'
        IMAGE_TAG = "${BUILD_NUMBER}-${GIT_COMMIT.take(7)}"
    }

    stages {
        stage('Checkout') {
            steps { git url: 'https://github.com/company/myapp.git' }
        }

        stage('Test') {
            steps {
                sh 'docker build --target test -t myapp:test .'
                sh 'docker run --rm myapp:test npm test'
            }
        }

        stage('Build & Push') {
            steps {
                sh "docker build -t ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} ."
                sh "docker tag ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} \
                    ${REGISTRY}/${IMAGE_NAME}:latest"
                sh "docker push ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
                sh "docker push ${REGISTRY}/${IMAGE_NAME}:latest"
            }
        }

        stage('Deploy') {
            steps {
                sshagent(['deploy-key']) {
                    sh """
                        ssh deploy@server "
                            cd /opt/myapp &&
                            docker compose pull &&
                            docker compose up -d --remove-orphans
                        "
                    """
                }
            }
        }

        stage('Cleanup') {
            steps {
                sh "docker rmi ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
                sh "docker image prune -f"
            }
        }
    }
}
```

### GitHub Actions 完整流水线

```yaml
# .github/workflows/full-ci.yml
name: Docker Full CI/CD

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: hadolint/hadolint-action@v3
        with:
          dockerfile: Dockerfile

  test:
    needs: lint
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build test image
        run: docker build --target test -t app:test .
      - name: Run tests
        run: docker run --rm app:test

  security-scan:
    needs: lint
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build image
        run: docker build -t app:scan .
      - name: Trivy scan
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: app:scan
          format: sarif
          output: trivy-results.sarif
          severity: HIGH,CRITICAL
      - name: Upload SARIF
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: trivy-results.sarif

  build-and-push:
    needs: [test, security-scan]
    if: github.event_name == 'push'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Docker meta
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha,format=short
            type=ref,event=branch
            type=semver,pattern={{version}}
            type=raw,value=latest,enable=${{ github.ref == 'refs/heads/main' }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Dive size check
        run: |
          CI=true dive ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:sha-${{ github.sha }}

  deploy-staging:
    needs: build-and-push
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - name: Deploy to staging
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.STAGING_HOST }}
          username: deploy
          key: ${{ secrets.DEPLOY_SSH_KEY }}
          script: |
            cd /opt/myapp-staging
            docker compose pull
            docker compose up -d --remove-orphans

  deploy-production:
    needs: build-and-push
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Deploy to production
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.PROD_HOST }}
          username: deploy
          key: ${{ secrets.DEPLOY_SSH_KEY }}
          script: |
            cd /opt/myapp
            docker compose pull
            docker compose up -d --remove-orphans
            docker image prune -a -f
```

---

## 11.6 Kubernetes 入门衔接

Docker 学完后，Kubernetes 是自然的下一步。以下是从 Docker 视角看 K8s 的对照表：

### 概念映射

| Docker | Kubernetes | 说明 |
|---|---|---|
| Container | Pod | K8s 最小调度单元，可包含多个容器 |
| `docker run` | Deployment | 声明式管理 Pod 副本 |
| `docker network` | Service | 服务发现与负载均衡 |
| `docker volume` | PersistentVolume | 持久化存储抽象 |
| `docker-compose.yml` | Helm Chart | 应用打包部署 |
| Docker Hub | Container Registry | 镜像仓库 |
| Docker Compose | Kustomize / Helm | 多环境配置管理 |
| `docker ps` | `kubectl get pods` | 查看运行实例 |

### 最小化 K8s 配置示例

```yaml
# deployment.yaml — 类似 docker run -d --name web -p 80:3000 myapp
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web
spec:
  replicas: 3                           # 类似 --scale web=3
  selector:
    matchLabels:
      app: web
  template:
    metadata:
      labels:
        app: web
    spec:
      containers:
        - name: web
          image: myapp:v1
          ports:
            - containerPort: 3000
          env:
            - name: DB_HOST
              value: postgres-service    # 类似 Compose 中的服务名解析
          resources:
            limits:
              memory: "256Mi"
              cpu: "500m"
---
# service.yaml — 类似端口映射 + 负载均衡
apiVersion: v1
kind: Service
metadata:
  name: web-service
spec:
  type: LoadBalancer
  selector:
    app: web
  ports:
    - port: 80
      targetPort: 3000
```

### 本地 K8s 学习环境

```bash
# Docker Desktop 自带 K8s
# Settings → Kubernetes → Enable Kubernetes

# 验证
kubectl cluster-info
kubectl get nodes

# 或者用 Minikube
minikube start --driver=docker

# 或者用 Kind (Kubernetes in Docker)
kind create cluster --name learning
```

### K8s 学习路径（Docker 之后）

```
Docker 熟练
   │
   ├── kubectl 基本命令（类比 docker CLI）
   ├── Pod / Deployment / Service 三大核心资源
   ├── ConfigMap / Secret 配置管理
   ├── Ingress 流量入口
   ├── PersistentVolume / PersistentVolumeClaim
   ├── Helm 包管理工具
   └── K8s 集群搭建（kubeadm / EKS / GKE / AKS）
```

---

## 11.7 常用生态工具速查

### 开发调试

| 工具 | 用途 | 一行命令 |
|---|---|---|
| **ctop** | 终端容器监控 | `ctop` |
| **dry** | Docker 容器管理 UI | `dry` |
| **dockly** | Node.js 写的 TUI | `npx dockly` |
| **sen** | Python 写的 TUI | `pip install sen && sen` |

### 构建加速

| 工具 | 用途 |
|---|---|
| **BuildKit** | Docker 新一代构建引擎（`DOCKER_BUILDKIT=1`） |
| **Buildx** | 多平台构建（`docker buildx build --platform linux/amd64,linux/arm64`） |
| **Kaniko** | 无需 Docker daemon 的镜像构建（常用于 K8s CI） |
| **Buildah** | RedHat 的替代构建工具（无 daemon） |

### 安全

| 工具 | 用途 |
|---|---|
| **Trivy** | 镜像漏洞扫描 (`trivy image myapp`) |
| **Falco** | 容器运行时威胁检测 |
| **Clair** | 静态漏洞分析（Harbor 内置） |
| **Cosign** | 镜像签名与验证 |

### 镜像优化

| 工具 | 用途 |
|---|---|
| **Dive** | 逐层分析镜像 |
| **docker-slim** | 自动缩小镜像（`docker-slim build myapp`） |
| **SlimToolkit** | 只保留运行时必需文件 |

---

> **相关文档**：[08-Docker-Compose多容器编排](08-Docker-Compose多容器编排.md) | [09-Docker实战项目](09-Docker实战项目.md) | [10-Docker进阶与生态](10-Docker进阶与生态.md)
