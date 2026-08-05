# 02-Pod详解
> 🎯 Pod是K8s中最小的部署和调度单元 — 理解Pod的本质（共享网络/存储/IPC命名空间）、生命周期、多容器协作模式、资源QoS与调度策略，是掌握K8s的基石

---

## 目录
1. [Pod的本质](#1-pod的本质)
2. [Pod生命周期](#2-pod生命周期)
3. [Pod状态与条件](#3-pod状态与条件)
4. [多容器Pod模式](#4-多容器pod模式)
5. [Init Container](#5-init-container)
6. [Pod资源限制与QoS](#6-pod资源限制与qos)
7. [Pod调度策略](#7-pod调度策略)
8. [完整Java应用Pod YAML](#8-完整java应用pod-yaml)

---

## 1. Pod的本质

### 1.1 为什么需要Pod？

K8s没有直接调度单个容器，而是调度Pod这一层抽象。Pod是**一组容器的集合**，这些容器共享相同的运行环境。

```text
容器设计原则：一个进程一个容器
  ┌─────────────────┐
  │     Pod 边界     │
  │  ┌─────┐ ┌─────┐ │
  │  │ App │ │ Side│ │
  │  │容器  │ │car  │ │
  │  └─────┘ └─────┘ │
  │  共享网络/IPC/Vol  │
  └─────────────────┘
```

> 💡 **Pod不是一组进程"捆在一起"的产物，而是"这些进程协同工作，需要共享同一环境"的产物。** 类比：Pod相当于一台"逻辑主机"，其内容器相当于这台主机上的进程。

### 1.2 共享资源

| 共享维度 | 详细说明 | 容器间如何访问 |
|----------|----------|---------------|
| **网络命名空间** | 所有容器共享同一个IP、端口范围 | localhost:port 互相访问 |
| **网络栈** | 共享网络接口、路由表、iptables规则 | 无需NAT直接通信 |
| **存储卷** | Pod级别声明的Volume可被所有容器挂载 | 挂载到不同路径即可共享文件 |
| **IPC命名空间** | SystemV IPC / POSIX 消息队列 | 通过共享内存通信 |
| **UTS命名空间** | 共享主机名 | hostname命令返回相同值 |

```yaml
# Pod网络模型示意
apiVersion: v1
kind: Pod
metadata:
  name: multi-container-pod
spec:
  containers:
    - name: app
      image: nginx
      ports:
        - containerPort: 80
    - name: sidecar
      image: busybox
      command: ["sh", "-c", "while true; do wget -q -O- http://localhost:80; sleep 10; done"]
      # ↑ sidecar通过localhost访问app容器的80端口
```

> 💡 同一Pod内容器间使用localhost通信，不同Pod间必须使用Service或Pod IP。这是K8s网络模型的基本原则。

### 1.3 Pod vs Docker Compose Service 对比

| 维度 | Pod | Docker Compose Service |
|------|-----|----------------------|
| 规模 | 通常1~3个容器 | 任意数量容器 |
| 网络 | 共享localhost | 独立网络，通过DNS通信 |
| 存储 | 共享Volume | 独立/共享Volume均可 |
| 生命周期 | 一起调度、一起销毁 | 独立管理 |
| 伸缩单元 | 以Pod为单位整体伸缩 | 以Service为单位伸缩 |
| 适用场景 | 紧耦合辅助进程 | 松耦合服务 |

---

## 2. Pod生命周期

### 2.1 生命周期的五个阶段

```text
          ┌──────────────┐
          │   Pending    │
          └──────┬───────┘
                 │ 调度成功，镜像拉取完成
                 ▼
          ┌──────────────┐
          │   Running    │◄────────┐
          └──────┬───────┘         │
                 │                  │
          ┌──────┴───────┐    ┌────┴───────┐
          │   Succeeded  │    │  Failed    │
          └──────────────┘    └────────────┘

          ┌──────────────┐
          │   Unknown    │ ← 状态不可达
          └──────────────┘
```

### 2.2 Phase 详细说明

| Phase | 状态码 | 含义 | 对应用场景 |
|-------|:------:|------|-----------|
| **Pending** | 0 | Pod已创建但容器未全部启动 | 正在调度、拉取镜像、InitContainer执行中 |
| **Running** | 1 | 所有容器已创建，至少一个Running | 正常运行的应用 |
| **Succeeded** | 2 | 所有容器正常退出（exit 0） | Job任务执行完成 |
| **Failed** | 3 | 至少一个容器非正常退出（exit ≠ 0） | 应用崩溃、配置错误 |
| **Unknown** | 4 | API Server无法获取Pod状态 | Master和Node断连、网络故障 |

### 2.3 从创建到销毁的完整流程

```text
1. kubectl apply -f pod.yaml
         │
         ▼
2. API Server验证 → 写入etcd
         │
         ▼
3. Scheduler调度 → 分配到合适Node
         │
         ▼
4. kubelet在Node上启动Pod
   ├── 4.1 拉取Init Container镜像（如有）
   ├── 4.2 执行Init Container（串行）
   ├── 4.3 拉取主容器镜像
   ├── 4.4 创建Sandbox容器（pause容器）
   ├── 4.5 创建业务容器
   └── 4.6 启动Liveness/Readiness/Startup Probe
         │
         ▼
5. Pod进入Running状态 → 提供服务
         │
         ▼
6. 收到删除请求 → 进入Terminating
   ├── preStop Hook执行（最多30s）
   ├── SIGTERM发送到主进程（1号进程）
   ├── 优雅退出等待（terminationGracePeriodSeconds，默认30s）
   └── SIGKILL强制终止
         │
         ▼
7. Pod从etcd中删除
```

> ⚠️ **Pod的优雅关闭是生产环境的必选项**。如果应用不处理SIGTERM信号，默认30s后会被SIGKILL强杀，可能导致请求丢失或数据不一致。

### 2.4 Pod的restartPolicy

| 策略 | 行为 | 适用场景 |
|------|------|----------|
| **Always** | 无论exit code，总是重启 | 长期运行服务（默认值） |
| **OnFailure** | exit code != 0时重启 | Job、批处理任务 |
| **Never** | 永不重启 | 一次性的数据迁移任务 |

> 💡 restartPolicy作用于Pod内的所有容器。Deployment管理的Pod默认`restartPolicy: Always`。

---

## 3. Pod状态与条件

### 3.1 PodStatus vs PodCondition

```yaml
# kubectl get pod <name> -o yaml 看到的完整状态
status:
  phase: Running                          # 生命周期阶段
  conditions:                             # 更细粒度的条件数组
    - type: PodScheduled
      status: "True"
      lastTransitionTime: "..."
    - type: Initialized
      status: "True"
    - type: ContainersReady
      status: "True"
    - type: Ready
      status: "True"
  hostIP: 10.0.0.1                        # 所在Node IP
  podIP: 10.244.1.5                       # Pod的集群IP
  podIPs:                                 # 双栈支持
    - ip: 10.244.1.5
  startTime: "2024-01-01T00:00:00Z"
  containerStatuses:
    - name: user-service
      containerID: docker://abc123
      image: user-service:1.0.0
      imageID: docker-pullable://...
      state:                              # waiting / running / terminated
        running:
          startedAt: "2024-01-01T00:00:01Z"
      lastState:                          # 前一次的状态（用于排查OOMKilled等）
        terminated:
          reason: OOMKilled
          exitCode: 137
      ready: true
      restartCount: 1
```

### 3.2 PodCondition 详细说明

| Condition Type | 描述 | True表示 | 排查方向 |
|---------------|------|---------|---------|
| **PodScheduled** | Pod已被调度到Node | 已分配Node | 资源不足、污点、Affinity冲突 |
| **Initialized** | Init Container执行完成 | Init容器已退出 | Init容器镜像拉取失败、错误退出 |
| **ContainersReady** | 所有容器Readiness Probe通过 | 容器已就绪 | Probe配置错误、应用启动慢 |
| **Ready** | Pod可以被Service转发流量 | 可作为后端 | 与ContainersReady联动 |

```bash
# 排查Pod问题的常用命令
kubectl describe pod <name>              # 查看Events（最常用）
kubectl logs <name> [-c container-name]  # 查看容器日志
kubectl logs <name> --previous           # 查看前一次运行的日志（CrashLoopBackOff）
kubectl exec -it <name> -- sh            # 进入容器排查
```

### 3.3 常见Pod问题诊断表

| 现象 | Phase | 原因 | 解决方案 |
|------|:-----:|------|---------|
| Pending | Pending | 节点资源不足/污点/PVC未绑定 | `kubectl describe`看Events，加节点或删污点 |
| ImagePullBackOff | Pending | 镜像拉取失败 | 检查镜像名、tag、仓库认证 |
| CrashLoopBackOff | Running | 容器启动后立即崩溃 | `kubectl logs --previous`查看错误日志 |
| OOMKilled | Running | 容器超过memory limit | 增大limit或优化内存 |
| RunContainerError | Running | 容器运行时错误 | 检查entrypoint、挂载卷权限 |
| Unknown | Unknown | Node失联 | 排查网络、kubelet健康状态 |

> 💡 **CrashLoopBackOff** = 容器启动失败后K8s自动重试，但每次重试时间指数增长（10s→20s→40s→80s→...→300s max），这是保护机制防止反复重启打垮系统。

---

## 4. 多容器Pod模式

### 4.1 三种经典设计模式

多容器Pod不是把任意容器随意塞在一起。业界总结了三种成熟模式：

```text
┌──────────────────────────────────────────────────────────┐
│                      Pod边界                               │
│                                                           │
│  Sidecar模式:                                             │
│  ┌───────────────┐  ┌──────────────────┐                 │
│  │  Main Container│  │  Sidecar          │                 │
│  │  user-service  │  │  filebeat/logstash│                 │
│  │  (提供API)     │  │  (收集日志→ES)    │                 │
│  └───────┬───────┘  └──────────────────┘                 │
│          │ 共享emptyDir卷                                  │
│          ▼                                                 │
│  /var/log/app/*.log   →   filebeat tail 同步             │
│                                                           │
│  Ambassador模式:                                           │
│  ┌───────────────┐  ┌──────────────────┐                 │
│  │  Main Container│  │  Ambassador       │                 │
│  │  Java App      │  │  redis-proxy/     │                 │
│  │  → localhost:6379│  │  twemproxy       │                 │
│  └───────────────┘  │  → 真实Redis集群   │                 │
│                      └──────────────────┘                 │
│                                                           │
│  Adapter模式:                                             │
│  ┌───────────────┐  ┌──────────────────┐                 │
│  │  Main Container│  │  Adapter          │                 │
│  │  旧版App       │  │  格式转换器       │                 │
│  │  输出独特格式   │  │  → 统一监控指标   │                 │
│  └───────────────┘  └──────────────────┘                 │
└──────────────────────────────────────────────────────────┘
```

### 4.2 Sidecar模式

**定义**：主容器旁边附加一个辅助容器，增强主容器功能而不修改主容器代码。

| 典型场景 | Sidecar做的事情 | 常见镜像 |
|----------|----------------|---------|
| **日志收集** | 从共享卷tail日志文件，发送到ES/Loki | filebeat, fluentd, logstash |
| **配置刷新** | 监听配置变化，动态reload主容器 | consul-template, configmap-reload |
| **服务网格** | 拦截所有进出流量，实现限流/熔断/观测 | Envoy (Istio sidecar) |
| **健康上报** | 收集应用指标，暴露给监控系统 | prometheus-adapter |

```yaml
# Sidecar模式：主容器 + 日志收集Sidecar
apiVersion: v1
kind: Pod
metadata:
  name: app-with-sidecar
  labels:
    app: user-service
spec:
  volumes:
    - name: shared-logs
      emptyDir: {}

  containers:
    # 主容器：Java应用
    - name: user-service
      image: user-service:1.0.0
      ports:
        - containerPort: 8080
      volumeMounts:
        - name: shared-logs
          mountPath: /var/log/app

    # Sidecar容器：Filebeat日志收集
    - name: filebeat
      image: docker.elastic.co/beats/filebeat:8.11.0
      volumeMounts:
        - name: shared-logs
          mountPath: /var/log/app
          readOnly: true
      env:
        - name: ELASTICSEARCH_HOST
          value: "elasticsearch.logging:9200"
```

> 💡 **Sidecar是"零侵入增强"的最佳实践**。不修改主容器的代码或镜像，通过附加容器实现日志、监控、网络治理等横切关注点。Istio的Envoy代理是Sidecar模式最成功的工业实践。

### 4.3 Ambassador模式

**定义**：用代理容器封装对外部服务的访问，主容器只连接localhost。

| 场景 | Ambassador做的事情 | 优点 |
|------|------------------|------|
| **Redis集群代理** | 客户端连接localhost:6379，代理转发到真实集群 | 主容器不需要感知集群拓扑 |
| **数据库读写分离** | localhost:3306 → 写主库/读从库 | 主容器零配置 |
| **服务发现封装** | 屏蔽后端服务变化，提供稳定连接 | 主容器不需要DNS |

```yaml
# Ambassador模式：Java App → localhost:6379 → 真实Redis集群
apiVersion: v1
kind: Pod
metadata:
  name: app-with-ambassador
  labels:
    app: order-service
spec:
  containers:
    - name: order-service
      image: order-service:1.0.0
      env:
        # 应用只配置localhost，不知晓外部Redis拓扑
        - name: SPRING_REDIS_HOST
          value: "localhost"
        - name: SPRING_REDIS_PORT
          value: "6379"

    - name: redis-ambassador
      image: twemproxy:0.4.1
      command:
        - nutcracker
        - -c
        - /etc/nutcracker/conf.yml
      ports:
        - containerPort: 6379
      volumeMounts:
        - name: ambassador-config
          mountPath: /etc/nutcracker
```

### 4.4 Adapter模式

**定义**：将主容器非标准的输出格式转换为统一标准。

| 场景 | Adapter做的事情 |
|------|----------------|
| **日志格式统一** | 应用输出纯文本 → Adapter转JSON → 日志系统 |
| **监控指标统一** | 应用特殊指标 → Prometheus格式 |
| **协议转换** | HTTP → gRPC 或 其他协议 |

```yaml
# Adapter模式：将应用日志转换为JSON格式
apiVersion: v1
kind: Pod
metadata:
  name: app-with-adapter
spec:
  volumes:
    - name: shared-logs
      emptyDir: {}
  containers:
    - name: legacy-app
      image: legacy-app
      volumeMounts:
        - name: shared-logs
          mountPath: /var/log/app

    - name: log-adapter
      image: log-adapter:1.0
      volumeMounts:
        - name: shared-logs
          mountPath: /var/log/app
          readOnly: true
      # 读取 /var/log/app/*.log，转换为JSON后输出到stdout
      # K8s收集stdout作为容器日志
```

### 4.5 何时使用多容器Pod vs 独立Pod

| 场景 | 建议 | 理由 |
|------|------|------|
| 日志收集 | Sidecar Pattern | 日志是"本地"的，与主容器生命周期绑定 |
| 服务网格 | Sidecar Pattern | 拦截流量必须在同一个网络命名空间 |
| 数据库代理 | Ambassador Pattern | 屏蔽后端拓扑 |
| 格式转换 | Adapter Pattern | 本地文件读取更高效 |
| 独立微服务 | 独立Pod | 不同的生命周期、独立伸缩、独立版本 |

> ⚠️ **不要把所有相关的容器都塞到一个Pod里**。多容器Pod适用于"紧密协作、共享同一生命周期"的场景。如果两个服务可以独立升级、独立伸缩，就应该用独立Pod + Service。

---

## 5. Init Container

### 5.1 什么是Init Container

Init Container是Pod中**在主容器启动之前执行**的特殊容器，负责初始化任务。与主容器的核心区别：

```text
Pod启动流程：
  ┌──────────┐   ┌─────────────────┐   ┌──────────┐
  │  Sandbox  │──▶│ Init Container  │──▶│ Main     │
  │  (pause)  │   │ (串行执行,       │   │ Container│
  │           │   │  全部成功后才继续)  │   │          │
  └──────────┘   └─────────────────┘   └──────────┘
```

| 特性 | Init Container | 主容器 |
|------|:--------------:|:------:|
| 执行顺序 | 串行，一个接一个 | 并行启动 |
| 生命周期 | 执行完就退出 | 持续运行 |
| restartPolicy | Pod的重启策略 | 同左 |
| 资源限制 | 支持requests/limits | 支持 |
| Readiness Probe | 不支持 | 支持 |
| 镜像更新 | 随Pod更新 | 随Pod更新 |

### 5.2 Init Container 典型场景

| 场景 | Init Container做的事情 |
|------|----------------------|
| **数据库初始化** | 执行Flyway/SQL迁移脚本，数据库Ready后才启动应用 |
| **权限检查** | 检查需要的外部资源是否可访问（数据库、Redis等） |
| **数据预热** | 从远程下载缓存数据或模型文件到共享卷 |
| **配置生成** | 从Secret/ConfigMap生成配置文件到共享卷 |
| **等待依赖服务** | 循环检测依赖Service是否Ready |

```yaml
# Init Container 完整示例：数据库迁移 + 等待依赖
apiVersion: v1
kind: Pod
metadata:
  name: app-with-init
spec:
  volumes:
    - name: app-config
      emptyDir: {}
    - name: app-data
      emptyDir: {}

  initContainers:
    # Init 1：等待MySQL就绪
    - name: wait-for-mysql
      image: busybox:1.28
      command:
        - sh
        - -c
        - |
          echo "Waiting for MySQL..."
          until nc -z mysql-service 3306; do
            echo "MySQL not ready, retrying..."
            sleep 2
          done
          echo "MySQL is ready!"

    # Init 2：执行数据库迁移
    - name: db-migration
      image: flyway/flyway:10
      command:
        - flyway
        - migrate
      env:
        - name: FLYWAY_URL
          value: "jdbc:mysql://mysql-service:3306/userdb"
        - name: FLYWAY_USER
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: FLYWAY_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password

    # Init 3：下载模型文件
    - name: download-model
      image: curlimages/curl:8.5.0
      command:
        - sh
        - -c
        - |
          curl -o /data/model.bin https://models.example.com/latest.bin
      volumeMounts:
        - name: app-data
          mountPath: /data

  containers:
    # 主容器：所有Init Container执行完毕后才启动
    - name: app
      image: user-service:1.0.0
      volumeMounts:
        - name: app-data
          mountPath: /data
```

> 💡 **Init Container是串行执行的**，所以如果Init1耗时30s，Init2耗时10s，Pod的Pending时间至少40s。如果Pending时间异常长，要排查Init Container的日志。

### 5.3 Init Container 的高级用法

```yaml
# Init Container支持resource限制和securityContext
initContainers:
  - name: heavy-init
    image: init-tool:latest
    resources:
      requests:
        cpu: "500m"
        memory: "512Mi"
      limits:
        cpu: "1000m"
        memory: "1Gi"
    securityContext:
      runAsUser: 1000
      runAsNonRoot: true
```

> ⚠️ **如果任何一个Init Container失败，K8s会根据restartPolicy决定是否重启整个Pod**。如果restartPolicy=Always，Init容器失败会重启Pod（重新从头执行所有Init容器）。这是常见陷阱：认为Always只影响主容器。

---

## 6. Pod资源限制与QoS

### 6.1 requests vs limits

K8s通过资源限制保障Pod的服务质量和集群的公平调度：

| 维度 | requests | limits |
|------|----------|--------|
| **含义** | 最低资源保证 | 硬性资源上限 |
| **调度依据** | Scheduler根据Node上已分配requests总和决定是否调度 | 不影响调度 |
| **CPU超卖** | 保证值 | 可burst，但超过会被throttle（限流） |
| **Memory超卖** | 保证值 | 超过会OOMKilled |
| **类比** | 订酒店：你保证至少要这个房间大小 | 最大不能超过这个面积 |

```yaml
# 资源定义的最佳实践
resources:
  requests:          # 调度时需要Node至少有这些资源
    cpu: "250m"      # 0.25核
    memory: "256Mi"  # 256兆内存
  limits:            # 运行时不能超过的资源上限
    cpu: "500m"      # 0.5核（超过会被throttle）
    memory: "512Mi"  # 512兆（超过会被OOM Kill）
```

### 6.2 CPU单位和Memory单位

```text
CPU:
  1 = 1核 = 1 vCPU
  1000m = 1000 millicores = 1核
  500m = 0.5核 = 半核
  100m = 0.1核

Memory:
  1G = 1GB = 10^9 bytes
  1Gi = 1GiB = 2^30 = 1073741824 bytes
  1M = 1MB = 10^6 bytes
  1Mi = 1MiB = 2^20 = 1048576 bytes

  常见写法：256Mi, 512Mi, 1Gi, 2Gi
```

> 💡 **生产环境建议使用Mi/Gi（二进制单位）**，因为操作系统内存分配以2的幂为单位。1G = 1000MB，而1Gi = 1024Mi，差~7%，在集群规模下是大量资源。

### 6.3 QoS三等级

K8s根据Pod的resources定义自动分配QoS等级。QoS等级决定了**资源紧缺时Pod被驱逐的优先顺序**。

| QoS等级 | 条件 | 特性 | 驱逐优先级 |
|:-------:|------|------|:----------:|
| **Guaranteed** | 所有容器的requests=limits（CPU和Memory都必须相等） | 最高保证，绝不轻易驱逐 | 3（最后一个） |
| **Burstable** | 至少一个容器的requests \< limits 或只设置requests | 中级保证，部分可弹性 | 2 |
| **BestEffort** | 没有设置任何resources | 无保证，系统压力大时最先被驱逐 | 1（第一个） |

```yaml
# Guaranteed: requests == limits（所有资源都需要相等）
apiVersion: v1
kind: Pod
metadata:
  name: guaranteed-pod
spec:
  containers:
    - name: app
      image: app
      resources:
        requests:
          cpu: "500m"
          memory: "512Mi"
        limits:
          cpu: "500m"       # 相等
          memory: "512Mi"    # 相等

---
# Burstable: requests < limits
apiVersion: v1
kind: Pod
metadata:
  name: burstable-pod
spec:
  containers:
    - name: app
      image: app
      resources:
        requests:
          cpu: "200m"
          memory: "256Mi"
        limits:
          cpu: "500m"       # 大于requests
          memory: "512Mi"    # 大于requests

---
# BestEffort: 无资源限制
apiVersion: v1
kind: Pod
metadata:
  name: besteffort-pod
spec:
  containers:
    - name: app
      image: app
      # 没有任何resources字段
```

```text
驱逐优先级链：
  内存压力 → BestEffort Pod立刻被驱逐
           → Burstable Pod（超过requests的）被驱逐
           → Burstable Pod（未超过requests的）被驱逐
           → Guaranteed Pod最后被驱逐
```

> ⚠️ **BestEffort Pod在内存压力下会直接被OOM Kill，且不保证任何资源。生产环境的Java应用至少设置为Burstable，最好设置为Guaranteed。** Java的JVM Heap设置要考虑容器limits，使用`-XX:MaxRAMPercentage=70.0`而非-Xmx。

### 6.4 JVM容器适配

```yaml
# Java应用在Pod中的资源感知配置
apiVersion: v1
kind: Pod
metadata:
  name: java-app
spec:
  containers:
    - name: java-app
      image: openjdk:17
      resources:
        requests:
          memory: "512Mi"
          cpu: "500m"
        limits:
          memory: "1Gi"
          cpu: "1000m"
      env:
        # Java 8u131+ 和 Java 10+ 自动适配容器内存
        # 但最好明确指定
        - name: JAVA_OPTS
          value: >-
            -XX:+UseContainerSupport
            -XX:MaxRAMPercentage=75.0
            -XX:InitialRAMPercentage=50.0
            -XX:+PrintGCDetails
            -XX:+PrintGCDateStamps
      # 或直接传入JVM参数
      # command: ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
```

| JVM版本 | 容器内存感知 |
|---------|-------------|
| Java 8u131- | ❌ 不感知，看到的是宿主机内存 |
| Java 8u131+ | ✅ 实验性：-XX:+UseCGroupMemoryLimitForHeap |
| Java 8u191+ | ✅ 默认开启：-XX:+UseContainerSupport |
| Java 10+ | ✅ 默认支持UseContainerSupport |
| Java 17+ | ✅ 最佳支持，建议使用 |

---

## 7. Pod调度策略

### 7.1 调度三阶段

```text
Scheduler工作流程：
  1. 过滤（Predicates/Filtering）: 选出满足条件的Node
     排除：资源不足、端口冲突、taint未匹配、Affinity不满足

  2. 评分（Priorities/Scoring）: 对候选Node打分
     打分维度：资源剩余量、Pod分布均匀度、Node亲和度

  3. 绑定（Binding）: 选择最高分Node，绑定调度
```

### 7.2 三种调度方式对比

```text
调度能力递进：
  nodeSelector(简单标签匹配) 
       ↓
  nodeAffinity(表达式匹配，灵活) 
       ↓
  taint/toleration(排斥/容忍，高级)
```

### 7.3 nodeSelector

最简单的调度方式：要求Pod运行在拥有特定标签的Node上。

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: gpu-pod
spec:
  nodeSelector:
    gpu: "true"           # 只调度到有 gpu=true 标签的Node
    disk: "ssd"           # 所有条件必须同时满足（AND）
  containers:
    - name: gpu-app
      image: gpu-app
```

```bash
# 给Node打标签
kubectl label node node-01 gpu=true
kubectl label node node-02 disk=ssd
```

> 💡 nodeSelector是**精确匹配**，不能做"或"、"非"、"in"等逻辑。需要更复杂逻辑时使用nodeAffinity。

### 7.4 nodeAffinity

比nodeSelector更灵活的节点亲和性调度，支持复杂表达式。

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: affinity-pod
spec:
  affinity:
    nodeAffinity:
      # 硬要求：调度时必须满足（对应requiredDuringScheduling）
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
          - matchExpressions:
              - key: topology.kubernetes.io/zone
                operator: In
                values:
                  - us-east-1a
                  - us-east-1b
              - key: gpu
                operator: Exists    # 存在即有gpu标签

      # 软偏好：尽可能满足（对应preferredDuringScheduling）
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 80                # 权重1~100，越高越优先
          preference:
            matchExpressions:
              - key: disk
                operator: In
                values:
                  - ssd
        - weight: 20
          preference:
            matchExpressions:
              - key: spot-instance
                operator: NotIn
                values:
                  - "true"          # 避免被调度到spot节点
```

| operator | 含义 | 示例 |
|----------|------|------|
| **In** | 值在列表中 | `key in [v1, v2]` |
| **NotIn** | 值不在列表中 | `key notin [v1]` |
| **Exists** | key存在（忽略值） | `key exists` |
| **DoesNotExist** | key不存在 | `key doesnotexist` |
| **Gt** | 值大于（数值比较） | `key Gt 100` |
| **Lt** | 值小于（数值比较） | `key Lt 50` |

> 💡 **Remember后缀**：`requiredDuringScheduling` = 硬要求，`preferredDuringScheduling` = 软偏好。后半段`IgnoredDuringExecution` = 调度后即使标签变化也不重新调度，这是K8s 1.x的默认行为。`RequiredDuringExecution`还在alpha阶段。

### 7.5 Taint和Toleration

Taint（污点）是Node的排斥标签，Toleration（容忍）是Pod对污点的容忍。

```bash
# 给Node打污点（NoSchedule：阻止调度）
kubectl taint node node-01 gpu=true:NoSchedule

# 去除污点
kubectl taint node node-01 gpu=true:NoSchedule-

# 查看Node污点
kubectl describe node node-01 | grep Taints
```

| Taint Effect | 行为 |
|-------------|------|
| **NoSchedule** | 不匹配Toleration的Pod不能调度到该Node |
| **PreferNoSchedule** | 尽量不调度，但非强制 |
| **NoExecute** | 不匹配Toleration的Pod会被驱离，已在运行的也会被驱逐 |

```yaml
# Pod容忍污点
apiVersion: v1
kind: Pod
metadata:
  name: tainted-pod
spec:
  tolerations:
    - key: gpu
      operator: Equal        # key+value精确匹配
      value: "true"
      effect: NoSchedule
    - key: spot-instance
      operator: Exists       # 仅匹配key，忽略value
      effect: NoSchedule
    - key: node.kubernetes.io/unreachable
      operator: Exists
      effect: NoExecute
      tolerationSeconds: 60  # 60秒后驱逐，给时间优雅关闭
  containers:
    - name: app
      image: app
```

### 7.6 Pod间亲和/反亲和

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: with-pod-affinity
spec:
  affinity:
    podAffinity:                     # Pod亲和：和某个Pod调度到同一拓扑域
      requiredDuringSchedulingIgnoredDuringExecution:
        - labelSelector:
            matchExpressions:
              - key: app
                operator: In
                values:
                  - cache
          topologyKey: topology.kubernetes.io/hostname

    podAntiAffinity:                  # Pod反亲和：避免和某个Pod在同一Node
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          podAffinityTerm:
            labelSelector:
              matchExpressions:
                - key: app
                  operator: In
                  values:
                    - user-service
            topologyKey: kubernetes.io/hostname
```

| 场景 | 配置 | 效果 |
|------|------|------|
| **高可用部署** | podAntiAffinity同一服务 | 不让两个副本在同一个Node |
| **缓存就近** | podAffinity应用+Redis | 应用和Redis在同一个可用区 |
| **资源隔离** | podAntiAffinity大服务 | 不让多个大服务挤在一个Node |

---

## 8. 完整Java应用Pod YAML

```yaml
# 生产级Java应用Pod定义
apiVersion: v1
kind: Pod
metadata:
  name: user-service-0
  namespace: production
  labels:
    app: user-service
    version: "1.0.0"
    tier: backend
    managed-by: pod-direct
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
    prometheus.io/path: "/actuator/prometheus"
    sidecar.istio.io/inject: "true"       # 注入Istio Sidecar

spec:
  # 调度策略
  nodeSelector:
    workload: backend                     # 只调度到backend节点

  affinity:
    nodeAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 80
          preference:
            matchExpressions:
              - key: instance-type
                operator: NotIn
                values:
                  - spot                    # 尽量不使用spot实例
    podAntiAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          podAffinityTerm:
            labelSelector:
              matchExpressions:
                - key: app
                  operator: In
                  values:
                    - user-service
            topologyKey: kubernetes.io/hostname  # 尽量分布在不同的Node

  tolerations:
    - key: node.kubernetes.io/unreachable
      operator: Exists
      effect: NoExecute
      tolerationSeconds: 30

  # Service Account
  serviceAccountName: user-service-sa

  # Init Container：等待依赖
  initContainers:
    - name: wait-for-mysql
      image: busybox:1.36
      command:
        - sh
        - -c
        - |
          echo "Waiting for MySQL at mysql-service:3306..."
          for i in $(seq 1 30); do
            if nc -z mysql-service 3306 2>/dev/null; then
              echo "MySQL is ready!"
              exit 0
            fi
            echo "Attempt $i/30, retrying in 2s..."
            sleep 2
          done
          echo "Failed to connect to MySQL"
          exit 1

    - name: db-migration
      image: flyway/flyway:10.0
      command:
        - flyway
        - migrate
      env:
        - name: FLYWAY_URL
          value: "jdbc:mysql://mysql-service:3306/user_db"
        - name: FLYWAY_USER
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: username
        - name: FLYWAY_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password

  # 主容器
  containers:
    - name: user-service
      image: registry.example.com/user-service:1.0.0
      imagePullPolicy: IfNotPresent

      ports:
        - name: http
          containerPort: 8080
          protocol: TCP

      # JVM环境变量
      env:
        - name: JAVA_OPTS
          value: >-
            -XX:+UseContainerSupport
            -XX:MaxRAMPercentage=75.0
            -XX:InitialRAMPercentage=50.0
            -XX:+HeapDumpOnOutOfMemoryError
            -XX:HeapDumpPath=/tmp/heapdump.hprof
            -XX:+UseG1GC
            -XX:MaxGCPauseMillis=200
            -Djava.security.egd=file:/dev/./urandom
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: DB_HOST
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: db.host
        - name: DB_PORT
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: db.port
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
        - name: REDIS_HOST
          value: "redis-service"
        - name: REDIS_PORT
          value: "6379"

      # 资源限制：Guaranteed QoS
      resources:
        requests:
          cpu: "500m"
          memory: "1Gi"
        limits:
          cpu: "500m"
          memory: "1Gi"

      # 存活探针：容器是否活着（挂了就重启）
      livenessProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
        initialDelaySeconds: 30          # 给JVM启动时间
        periodSeconds: 10                # 每10s检查一次
        timeoutSeconds: 5                # 5s超时
        failureThreshold: 3              # 连续3次失败才重启

      # 就绪探针：是否可以接收流量
      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8080
        initialDelaySeconds: 20
        periodSeconds: 5
        timeoutSeconds: 3
        successThreshold: 1
        failureThreshold: 2

      # 启动探针：应用是否已启动（防止慢启动被误杀）
      startupProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
        initialDelaySeconds: 10
        periodSeconds: 5
        failureThreshold: 30             # 给最多150s用于启动

      # 停止前钩子：优雅关闭
      lifecycle:
        preStop:
          exec:
            command:
              - sh
              - -c
              - |
                echo "Shutting down gracefully..."
                # 通知注册中心下架（可选）
                # curl -X POST http://eureka:8761/eureka/apps/USER-SERVICE/$(HOSTNAME)/status?value=OUT_OF_SERVICE
                sleep 5                  # 等kube-proxy更新iptables
                kill -SIGTERM 1          # 向Java进程发SIGTERM

      # 挂载卷
      volumeMounts:
        - name: config-volume
          mountPath: /app/config
          readOnly: true
        - name: tmp
          mountPath: /tmp
        - name: logs
          mountPath: /var/log/app
        - name: timezone
          mountPath: /etc/localtime
          readOnly: true

    # Sidecar：日志收集
    - name: filebeat
      image: docker.elastic.co/beats/filebeat:8.11.0
      volumeMounts:
        - name: logs
          mountPath: /var/log/app
          readOnly: true
      env:
        - name: ELASTICSEARCH_HOST
          value: "elasticsearch.logging:9200"

  # 卷定义
  volumes:
    - name: config-volume
      configMap:
        name: app-config
        items:
          - key: application.yml
            path: application.yml
    - name: tmp
      emptyDir: {}
    - name: logs
      emptyDir: {}
    - name: timezone
      hostPath:
        path: /usr/share/zoneinfo/Asia/Shanghai

  # 重启策略
  restartPolicy: Always

  # 优雅关闭期限
  terminationGracePeriodSeconds: 60

  # Pod安全策略
  securityContext:
    runAsUser: 1000
    runAsNonRoot: true
    fsGroup: 1000
    seccompProfile:
      type: RuntimeDefault
```

---

> 🎯 **掌握Pod就是掌握K8s的原子单元**。理解Pod的共享资源模型（为什么需要pause容器）、生命周期阶段（Pending里面包含多少事情）、QoS等级（Guaranteed不等于永远不会被杀死）、以及多容器模式（Sidecar是服务网格的基础），才能在生产环境中写出健壮的Pod定义。下一章：[03-Deployment与滚动更新](03-Deployment与滚动更新.md)将介绍如何通过Deployment管理Pod的期望状态和发布策略。
