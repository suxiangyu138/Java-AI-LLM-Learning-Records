# 10-Docker 与 Kubernetes 衔接
> 从 Docker 到 K8s 的桥：K8s 解决了 Compose 无法覆盖的多机集群/自动扩缩/故障自愈/滚动更新——概念速览、部署实战、蓝绿/金丝雀发布、CI/CD 流水线

## 📚 目录
1. [为什么需要 Kubernetes](#1-为什么需要-kubernetes)
2. [K8s 核心概念速览](#2-k8s-核心概念速览)
3. [探针（Probe）与 Spring Boot Actuator](#3-探针probe与-spring-boot-actuator)
4. [部署 Spring Boot 完整流程](#4-部署-spring-boot-完整流程)
5. [容器化 CI/CD 流水线](#5-容器化-cicd-流水线)
6. [蓝绿部署](#6-蓝绿部署)
7. [金丝雀发布](#7-金丝雀发布)
8. [回滚策略总结](#8-回滚策略总结)
9. [常见误区与学习路线](#9-常见误区与学习路线)
10. [核心要点](#10-核心要点)
11. [参考来源](#11-参考来源)

> 💡 **体系衔接**：K8s 完整知识体系见同级 `Kubernetes/` 目录（12 篇：核心概念/Pod/Deployment/Service/ConfigMap/Ingress/HPA/存储/部署实战等）。本篇聚焦"Docker → K8s"的桥接视角与发布策略。

## 1. 为什么需要 Kubernetes

K8s 解决了 Docker Compose 无法覆盖的问题：**多机集群、自动扩缩、故障自愈、服务发现、滚动更新**。

```text
Docker Compose（单机）      →    Kubernetes（多机集群）
  services                      Pod / Deployment / Service
  单主机编排                     跨主机编排 + 自动扩缩 + 自愈
```

**集群架构**（控制平面 + 工作节点）：

| 组件 | 职责 |
|------|------|
| **API Server** | 集群入口，所有操作（kubectl/仪表盘/其他组件）都通过它，提供 REST API 和认证鉴权 |
| **Scheduler** | 调度 Pod 到合适的工作节点（资源/亲和性/污点） |
| **Controller Manager** | 运行各种控制器，确保实际状态收敛到期望状态 |
| **etcd** | 分布式键值存储，保存集群所有状态数据（唯一有状态组件，需备份） |
| **kubelet** | 节点代理，管理 Pod 和容器的生命周期 |
| **kube-proxy** | 维护节点网络规则，实现 Service 负载均衡 |
| **Container Runtime** | 容器运行时（containerd、CRI-O；Docker 已弃用） |

## 2. K8s 核心概念速览

### 2.1 Pod — 最小调度单元

- **单容器 Pod**：最常见的模式，一个 Pod 只有一个应用容器；
- **多容器 Pod（Sidecar 模式）**：主容器 + 辅助容器共享 Pod（日志收集 Filebeat/Fluentd、服务网格代理 Istio Envoy、配置热更新、反向代理）。

| Pod 关键特性 | 说明 |
|--------------|------|
| 共享网络 | Pod 内所有容器共享同一网络命名空间（IP 和端口空间），可通过 localhost 互相访问 |
| 共享存储 | 容器可挂载相同 Volume |
| **临时性** | IP 随 Pod 重建而变化——这就是为什么需要 Service 做稳定访问入口 |
| 原子调度 | Pod 内所有容器保证被调度到同一节点 |

### 2.2 Deployment — 声明式管理 Pod

提供**声明式更新**能力——你描述"想要什么状态"，Deployment Controller 确保实际状态匹配。

```yaml
spec:
  replicas: 3                              # 期望副本数
  selector:
    matchLabels: { app: my-app }
  template:
    metadata:
      labels: { app: my-app, version: v1 }
    spec:
      containers:
      - name: my-app
        image: registry/my-app:v1
        resources:
          requests: { memory: "256Mi", cpu: "250m" }
          limits:   { memory: "512Mi", cpu: "500m" }
  strategy:
    type: RollingUpdate                     # 滚动更新策略
    rollingUpdate:
      maxSurge: 1                           # 最多超出期望副本数 1 个
      maxUnavailable: 0                     # 不允许不可用 Pod
```

```bash
# 滚动更新与回滚
kubectl set image deployment/my-app-deployment my-app=registry/my-app:v2
kubectl rollout status deployment/my-app-deployment    # 查看更新状态
kubectl rollout history deployment/my-app-deployment   # 查看更新历史
kubectl rollout undo deployment/my-app-deployment      # 回滚上一个版本
kubectl rollout undo deployment/my-app-deployment --to-revision=2   # 回滚指定版本
kubectl rollout pause deployment/my-app-deployment     # 暂停更新（金丝雀用）
kubectl rollout resume deployment/my-app-deployment    # 恢复更新
```

**滚动更新机制**：更新前 3 个 v1 → 创建 1 个 v2 新 Pod（maxSurge=1）→ 等新 Pod 就绪（Readiness Probe）→ 删除 1 个 v1 → 循环直到全部替换为 v2。

### 2.3 Service — 稳定的网络入口

通过**标签选择器**找到匹配 Pod，并将请求负载均衡到这些 Pod 上。每个 Service 背后由 **Endpoints**（或 EndpointSlice）维护实际 Pod IP 列表。

| Service 类型 | 访问方式 | 适用场景 |
|--------------|---------|---------|
| **ClusterIP** | 集群内部虚拟 IP，仅集群内可访问 | 内部微服务通信 |
| **NodePort** | 每个节点 IP + 固定端口（30000-32767） | 开发测试、外部简单访问 |
| **LoadBalancer** | 云厂商提供公网负载均衡器 | 生产环境对外暴露 |
| **ExternalName** | DNS CNAME 映射 | 外部服务映射为集群内服务名 |

### 2.4 ConfigMap / Secret / Ingress / Namespace / HPA

| 对象 | 一句话 | 关键点 |
|------|--------|--------|
| ConfigMap | 非敏感配置，与镜像解耦 | **挂载为文件支持热更新（约 60 秒）**；环境变量方式不支持热更新 |
| Secret | 敏感配置 | 默认仅 base64 编码，**不是加密**；生产用 Sealed Secrets / External Secrets Operator / Vault |
| Ingress | HTTP/HTTPS 七层路由 | 需配合 Ingress Controller（Nginx/Traefik）；支持域名/路径路由 + TLS |
| Namespace | 逻辑隔离 | 多环境（dev/staging/prod）或多团队；配 ResourceQuota/LimitRange 控制资源 |
| HPA | 水平自动扩缩 | `desired = ceil[currentReplicas * (currentValue / targetValue)]`，如 CPU 平均利用率 70% |

## 3. 探针（Probe）与 Spring Boot Actuator

```yaml
spec:
  containers:
  - name: my-app
    # === 存活探针：容器是否活着，失败则重启 ===
    livenessProbe:
      httpGet: { path: /actuator/health/liveness, port: 8080 }
      initialDelaySeconds: 30
      periodSeconds: 10
      timeoutSeconds: 3
      failureThreshold: 3          # 连续失败 3 次重启

    # === 就绪探针：容器是否就绪，失败则移出 Service ===
    readinessProbe:
      httpGet: { path: /actuator/health/readiness, port: 8080 }
      initialDelaySeconds: 10
      periodSeconds: 5
      failureThreshold: 2

    # === 启动探针：保护慢启动容器（K8s 1.18+） ===
    startupProbe:
      httpGet: { path: /actuator/health, port: 8080 }
      initialDelaySeconds: 5
      periodSeconds: 5
      failureThreshold: 30          # 最多 30 次 * 5 秒 = 150 秒
```

| 探针 | 失败后果 | 适用场景 |
|------|---------|---------|
| **Liveness** | 重启容器 | 检测死锁、内存泄漏、进程挂起等不可恢复错误 |
| **Readiness** | 从 Service Endpoints 中移除 | 启动加载依赖、缓存预热、短暂高负载 |
| **Startup** | 重启容器（通过前禁用 Liveness） | 启动时间很长的应用（Spring Boot 加载大量 Bean） |

> ⚠️ **重要原则**：不要在 Liveness Probe 中检查外部依赖（数据库、Redis 等）——外部依赖故障会导致应用被重启，而重启通常不能解决外部依赖问题。Readiness Probe 适合检查依赖就绪状态。

```yaml
# Spring Boot Actuator 配合
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
# 此时 /actuator/health/liveness 和 /actuator/health/readiness 自动可用
```

## 4. 部署 Spring Boot 完整流程

```bash
# 步骤 1：构建并推送镜像（多阶段构建 Dockerfile 见 03）
docker build -t registry/my-app:v1.0.0 .
docker push registry/my-app:v1.0.0
```

```yaml
# 步骤 2：ConfigMap（configmap.yaml）—— application.yml 作为外部配置
apiVersion: v1
kind: ConfigMap
metadata:
  name: my-app-config
data:
  application.yml: |
    server:
      port: 8080
    spring:
      datasource:
        url: jdbc:mysql://mysql-service:3306/myapp?useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: ${DB_PASSWORD}          # 密码通过 Secret 注入
      redis:
        host: redis-service
        port: 6379
        password: ${REDIS_PASSWORD}
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
```

```yaml
# 步骤 3：Secret（secret.yaml）—— echo -n "root123" | base64
apiVersion: v1
kind: Secret
metadata:
  name: my-app-secret
type: Opaque
data:
  db-password: cm9vdDEyMw==
  redis-password: cmVkaXMxMjM=
```

```yaml
# 步骤 4：Deployment（deployment.yaml）—— 核心要点
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: my-app
        image: registry/my-app:v1.0.0
        imagePullPolicy: Always
        env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef: { name: my-app-secret, key: db-password }
        - name: SPRING_CONFIG_ADDITIONAL_LOCATION
          value: /app/config/          # 让 Spring Boot 从 ConfigMap 挂载目录加载外部配置
        resources:
          requests: { memory: "256Mi", cpu: "250m" }
          limits:   { memory: "512Mi", cpu: "1" }
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: tmp-volume
          mountPath: /tmp              # 配合只读文件系统
        # 三探针配置见第 3 节
        securityContext:
          runAsNonRoot: true
          runAsUser: 1000
      volumes:
      - name: config-volume
        configMap: { name: my-app-config }
      - name: tmp-volume
        emptyDir: {}
  strategy:
    type: RollingUpdate
    rollingUpdate: { maxSurge: 1, maxUnavailable: 0 }
```

```yaml
# 步骤 5：Service（service.yaml）
apiVersion: v1
kind: Service
metadata:
  name: my-app-service
spec:
  selector: { app: my-app }
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
```

```yaml
# 步骤 6：HPA（hpa.yaml，可选）
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: my-app-hpa
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
      target: { type: Utilization, averageUtilization: 70 }
```

```bash
# 步骤 7：按顺序应用所有配置
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f hpa.yaml

# 查看状态与测试访问
kubectl get pods -w
kubectl get deployments && kubectl get services && kubectl get hpa
kubectl port-forward svc/my-app-service 8080:80    # 端口转发到本地
kubectl logs -f -l app=my-app                      # 跟踪所有 Pod 日志
```

## 5. 容器化 CI/CD 流水线

```text
代码提交 → 自动构建 → 镜像推送 → 自动部署 → 健康检查 → 完成/回滚
  │          │           │           │          │
 git push   mvn package  docker push kubectl     kubectl rollout
 触发流水线  +docker build           set image   status
```

```yaml
# .gitlab-ci.yml
stages: [build, dockerize, deploy]

variables:
  DOCKER_REGISTRY: "registry.cn-hangzhou.aliyuncs.com"
  APP_NAME: "my-app"
  K8S_NAMESPACE: "production"

maven-build:
  stage: build
  image: maven:3.8-eclipse-temurin-17
  script: [mvn clean package -DskipTests]
  artifacts:
    paths: [target/*.jar]
    expire_in: 1 hour

docker-build:
  stage: dockerize
  image: docker:20
  services: [docker:dind]
  script:
    - docker build -t ${DOCKER_REGISTRY}/my-project/${APP_NAME}:${CI_PIPELINE_ID} .
    - docker tag ${DOCKER_REGISTRY}/my-project/${APP_NAME}:${CI_PIPELINE_ID} ${DOCKER_REGISTRY}/my-project/${APP_NAME}:latest
    - docker login -u ${DOCKER_USER} -p ${DOCKER_PASSWORD} ${DOCKER_REGISTRY}
    - docker push ${DOCKER_REGISTRY}/my-project/${APP_NAME}:${CI_PIPELINE_ID}
    - docker push ${DOCKER_REGISTRY}/my-project/${APP_NAME}:latest

deploy-k8s:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/${APP_NAME} ${APP_NAME}=${DOCKER_REGISTRY}/my-project/${APP_NAME}:${CI_PIPELINE_ID} -n ${K8S_NAMESPACE}
    - kubectl rollout status deployment/${APP_NAME} -n ${K8S_NAMESPACE} --timeout=5m
  only: [main]
```

> 💡 版本标签用 `${CI_PIPELINE_ID}` 保证唯一；构建阶段用 `docker:dind` 服务；`only: main` 分支触发。

## 6. 蓝绿部署

核心思想：维护两套完全独立的环境（蓝=当前生产，绿=新版本），通过切换 Service 选择器实现零停机发布。

```text
                ┌──────────┐
                │ Service  │  ← 指向蓝色或绿色
                │ selector │
                └────┬─────┘
          ┌──────────┴──────────┐
          ▼                     ▼
   ┌──────────┐          ┌──────────┐
   │  Blue v1 │          │ Green v2 │
   │ (当前生产)│          │ (新版本)  │
   └──────────┘          └──────────┘
     标签: version: blue   标签: version: green
```

```bash
# 1. 当前 Service 指向蓝色：selector 含 version: blue
# 2. 部署绿色环境（my-app-green Deployment，image v2.0.0）
# 3. 验证绿色正常后，切换 Service 指向绿色
kubectl patch service my-app-service -p '{"spec":{"selector":{"version":"green"}}}'
# 4. 确认绿色就绪后，删除蓝色环境
kubectl delete deployment my-app-blue
```

| 优点 | 缺点 |
|------|------|
| 瞬间切换，回滚极快（再次切回蓝色即可） | 需要双倍资源，成本较高 |

## 7. 金丝雀发布

逐步将小部分流量引入新版本，监控确认正常后逐步增加比例直到全量切换。

```text
第一阶段：100% v1   0% v2   ← 金丝雀开始
第二阶段： 90% v1  10% v2   ← 观察 10% 流量
第三阶段： 50% v1  50% v2   ← 逐步放大
第四阶段：  0% v1 100% v2   ← 全量完成
```

**方案一：Istio VirtualService 权重路由**：

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: my-app-vs
spec:
  hosts: [my-app-service]
  http:
  - route:
    - destination: { host: my-app-service, subset: v1 }
      weight: 90                        # 90% 流量到 v1
    - destination: { host: my-app-service, subset: v2 }
      weight: 10                        # 10% 流量到 v2
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: my-app-dr
spec:
  host: my-app-service
  subsets:
  - name: v1
    labels: { version: v1 }
  - name: v2
    labels: { version: v2 }
```

**方案二：副本数比例 + Service selector（简化方案）**：

```bash
# 1. 部署 v1：3 个副本（100% 流量）；部署 v2：1 个副本
kubectl scale deployment my-app-v2 --replicas=1
# 2. Service selector 为 app=my-app，同时匹配 v1 和 v2 的 Pod
# v1: 3 个 → 占 75% 流量；v2: 1 个 → 占 25% 流量
# 3. 逐步 scale up v2、scale down v1 实现流量切换
kubectl scale deployment my-app-v2 --replicas=2   # v2 占 40%
kubectl scale deployment my-app-v1 --replicas=1   # v2 占 50%
kubectl scale deployment my-app-v2 --replicas=3   # v2 占 75%
kubectl scale deployment my-app-v1 --replicas=0   # 100% v2
```

## 8. 回滚策略总结

| 发布方式 | 回滚方式 | 回滚速度 | 影响范围 |
|---------|---------|---------|---------|
| 滚动更新（RollingUpdate） | `kubectl rollout undo` | 分钟级（逐个替换 Pod） | 部分流量短暂影响 |
| 蓝绿部署 | 切换 Service selector | 秒级 | 无影响（瞬时切换） |
| 金丝雀发布 | 将权重调回 0% | 秒级（DNS/配置生效） | 仅影响金丝雀用户 |

## 9. 常见误区与学习路线

**8 条常见误区**：

| # | 误区 | 正确做法 |
|---|------|---------|
| 1 | 镜像太大 | 总是使用 Alpine 或 Slim 版本，多阶段构建是必须的 |
| 2 | 以 root 运行容器 | 生产环境务必非 root 用户 + securityContext |
| 3 | 依赖 IP 直连 Pod | Pod IP 是临时的，永远通过 Service 访问 |
| 4 | 没有配置资源限制 | 不设 limits 会导致单个 Pod 耗尽节点资源 |
| 5 | 探针配置不当 | Liveness 不查外部依赖，Readiness 查依赖状态 |
| 6 | ConfigMap 热更新误区 | 挂载为文件才支持热更新，环境变量方式不支持 |
| 7 | Secret 误以为加密 | Secret 只是 base64 编码，敏感环境需配合外部密钥管理 |
| 8 | 忽略 Pod 反亲和性 | 同一 Deployment 的 Pod 应分散在不同节点，提高可用性 |

**学习路线**：

```text
初学者阶段：Docker 基础 → Dockerfile 构建 → Docker Compose 编排 → Swarm（了解即可）
进阶阶段：K8s 核心概念 → kubectl 命令 → Deployment/Service/ConfigMap → 部署实战
高级阶段：Ingress/Helm/Operator → Service Mesh（Istio）→ GitOps（ArgoCD）→ 安全加固
生产实践：监控（Prometheus + Grafana）· 日志（EFK/ELK）· 链路追踪（Jaeger/Zipkin）· CI/CD
```

> 🎯 **最后建议**：不要试图一次性掌握所有概念。从 Docker 入手——先能让 Spring Boot 应用在容器中运行，然后通过 Compose 将 MySQL+Redis+应用一键拉起。理解了容器化便利后，再引入 K8s——先部署一个无状态应用，再逐步添加 ConfigMap、Secret、Probe、HPA。**先跑起来，再优化**。

## 10. 核心要点

> 🎯 **核心要点**：
> - K8s 解决 Compose 的四大问题：多机集群、自动扩缩、故障自愈、滚动更新；
> - 映射关系：镜像→Pod、容器→Pod 内容器、Compose 服务→Deployment+Service；
> - 三探针配合：Liveness（重启）/ Readiness（摘流量）/ Startup（慢启动保护）；
> - 发布三策略：滚动（默认）、蓝绿（双倍资源换秒级切换）、金丝雀（权重渐进）；
> - 回滚选择：滚动用 `rollout undo`，蓝绿/金丝雀切回 selector/权重（秒级）；
> - 完整知识见同级 `Kubernetes/` 体系（12 篇）。

## 11. 参考来源

- [Kubernetes 官方文档](https://kubernetes.io/zh-cn/docs/)
- [Kubernetes Deployment 文档](https://kubernetes.io/zh-cn/docs/concepts/workloads/controllers/deployment/)
- [Kubernetes 探针文档](https://kubernetes.io/zh-cn/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
- [Istio 流量管理文档](https://istio.io/latest/zh/docs/tasks/traffic-management/)

---

**下一模块**：[11-应用场景与学习索引](11-应用场景与学习索引.md)　/　**返回总览**：[00-总览](00-Docker知识体系总览.md)
