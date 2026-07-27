# 01-Kubernetes核心概念与架构
> 🎯 K8s不是"超级Docker Compose" — 理解Master/Worker架构、声明式API、核心抽象(Pod/Service/Deployment)、以及控制循环(Control Loop)的设计哲学

---

## 目录
1. [Kubernetes是什么](#1-kubernetes是什么)
2. [Master-Worker架构](#2-master-worker架构)
3. [声明式API与控制循环](#3-声明式api与控制循环)
4. [六大核心抽象](#4-六大核心抽象)
5. [K8s网络模型](#5-k8s网络模型)
6. [K8s vs Docker Compose vs VM](#6-k8s-vs-docker-compose-vs-vm)

---

## 1. Kubernetes是什么

### 1.1 一句话定义

> K8s是**容器编排平台** — 你告诉它"我想要什么状态"，它自动把系统调整为那个状态并保持。

```text
你声明（Declarative）:
  "我要3个user-service实例，每个用512MB内存，挂在80端口后面"

K8s做的事（Control Loop）:
  ✅ 启动3个Pod
  ✅ 监控Pod健康状态
  ✅ Pod挂了自动重启
  ✅ 流量自动负载均衡到3个Pod
  ✅ 节点宕机自动迁移Pod到健康节点
  ✅ 流量增大自动扩容（配置HPA后）
```

### 1.2 K8s解决的核心问题

| 问题 | 没有K8s | 有K8s |
|------|---------|-------|
| 服务部署 | SSH到每台机器手动启动 | `kubectl apply -f deployment.yaml` |
| 负载均衡 | 手动配置Nginx upstream | Service自动负载均衡 |
| 故障恢复 | 告警→人工重启 | 自动检测+重启+迁移 |
| 弹性伸缩 | 人工评估+扩容 | HPA根据CPU/内存自动缩放 |
| 配置管理 | 配置文件散落各机器 | ConfigMap/Secret统一管理 |
| 滚动更新 | 手动停旧启新 | Deployment自动分批替换 |
| 回滚 | 手动恢复旧版本 | `kubectl rollout undo` 一键回滚 |

---

## 2. Master-Worker架构

### 2.1 全景架构图

```text
┌─────────────────────────────────────────────────────────────┐
│                      Control Plane (Master)                  │
│  ┌──────────┐ ┌───────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ API      │ │ Scheduler │ │Controller│ │ etcd          │  │
│  │ Server   │ │           │ │ Manager  │ │ (分布式KV存储) │  │
│  │ (集群入口)│ │ (调度Pod)  │ │ (状态控制)│ │              │  │
│  └──────────┘ └───────────┘ └──────────┘ └──────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
┌─────────┴─────────┐ ┌───────┴─────────┐ ┌───────┴─────────┐
│    Worker Node 1  │ │  Worker Node 2  │ │  Worker Node 3  │
│ ┌───────────────┐ │ │ ┌─────────────┐ │ │ ┌─────────────┐ │
│ │   kubelet     │ │ │ │   kubelet   │ │ │ │   kubelet   │ │
│ │ (节点代理)     │ │ │ │             │ │ │ │             │ │
│ ├───────────────┤ │ │ ├─────────────┤ │ │ ├─────────────┤ │
│ │ kube-proxy    │ │ │ │ kube-proxy  │ │ │ │ kube-proxy  │ │
│ │ (网络代理)     │ │ │ │             │ │ │ │             │ │
│ ├───────────────┤ │ │ ├─────────────┤ │ │ ├─────────────┤ │
│ │ Container     │ │ │ │ Container   │ │ │ │ Container   │ │
│ │ Runtime       │ │ │ │ Runtime     │ │ │ │ Runtime     │ │
│ │ (containerd)  │ │ │ │             │ │ │ │             │ │
│ ├───────────────┤ │ │ ├─────────────┤ │ │ ├─────────────┤ │
│ │ Pod Pod Pod   │ │ │ │ Pod Pod     │ │ │ │ Pod Pod Pod │ │
│ └───────────────┘ │ │ └─────────────┘ │ │ └─────────────┘ │
└───────────────────┘ └─────────────────┘ └─────────────────┘
```

### 2.2 各组件职责

| 组件 | 位置 | 职责 | 类比 |
|------|:---:|------|------|
| **API Server** | Master | 集群唯一入口，所有操作通过它 | 前台的"柜台" |
| **etcd** | Master | 存储集群所有状态数据 | 数据库 |
| **Scheduler** | Master | 决定Pod运行在哪个Node上 | 调度员 |
| **Controller Manager** | Master | 运行各种Controller，维持期望状态 | 自动化运维 |
| **kubelet** | 每个Node | 管理本Node上的Pod生命周期 | 工头 |
| **kube-proxy** | 每个Node | 维护网络规则，实现Service负载均衡 | 路由器 |
| **Container Runtime** | 每个Node | 实际运行容器（containerd/cri-o） | 工人 |

---

## 3. 声明式API与控制循环

### 3.1 声明式 vs 命令式

```bash
# 命令式（Imperative）：告诉系统"做什么"
kubectl run nginx --image=nginx      # 具体的操作步骤
kubectl scale --replicas=3 deploy/nginx

# 声明式（Declarative）：告诉系统"我想要什么状态"
kubectl apply -f deployment.yaml     # 描述期望状态，K8s自动达到
```

```yaml
# deployment.yaml — 声明式配置
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 3                    # 期望3个副本
  selector:
    matchLabels:
      app: user-service
  template:
    spec:
      containers:
        - name: user-service
          image: registry.example.com/user-service:1.0
          ports:
            - containerPort: 8080
          resources:
            requests:
              memory: "256Mi"
              cpu: "200m"
```

### 3.2 控制循环 (Control Loop)

```text
K8s的工作原理 = 无限循环的控制循环：

  observe → diff → act
     ↑                │
     └────────────────┘

1. observe: Controller观察当前状态（如Pod实际运行2个）
2. diff: 与期望状态对比（期望3个，实际2个 → 差1个）
3. act: 执行操作消除差异（启动一个新的Pod）
4. 重复...
```

> 🎯 **这是K8s最核心的设计哲学**：你负责声明"期望状态"，K8s负责保证系统始终处于那个状态。这不是一次性命令，而是持续的自动控制。

---

## 4. 六大核心抽象

| 资源 | Kind | 作用 | 类比 |
|------|------|------|------|
| **Pod** | Pod | 最小部署单元，包含1+个容器 | 一个"进程组" |
| **Service** | Service | 为Pod提供稳定访问入口+负载均衡 | 内部负载均衡器 |
| **Deployment** | Deployment | 管理Pod副本数+滚动更新+回滚 | 应用管理器 |
| **ConfigMap** | ConfigMap | 非敏感配置 | 配置文件 |
| **Secret** | Secret | 敏感配置（密码/证书） | 加密配置 |
| **Ingress** | Ingress | HTTP七层路由 | Nginx反向代理 |

```text
典型关系链：
  Deployment → 创建和管理 ReplicaSet → 创建和管理 Pod
  Service → 选择 Pod（通过label）→ 提供稳定IP和DNS
  Ingress → 路由到 Service → Service负载均衡到 Pod
  ConfigMap/Secret → 挂载到Pod → 容器内读取配置
```

---

## 5. K8s网络模型

### 5.1 四个网络层次

| 层次 | 问题 | 方案 |
|------|------|------|
| **Pod内通信** | 同一Pod内容器间如何通信？ | 共享localhost（共享网络命名空间） |
| **Pod间通信** | 不同Node上的Pod如何通信？ | CNI插件（Flannel/Calico/Cilium） |
| **Pod到Service** | Pod怎么找到Service？ | kube-proxy + DNS |
| **外部到Service** | 外部用户怎么访问？ | NodePort/LoadBalancer/Ingress |

### 5.2 CNI网络插件

| 插件 | 网络模型 | 性能 | 网络策略 | 推荐场景 |
|------|----------|:---:|:---:|------|
| **Flannel** | VXLAN/host-gw | ⚠️ 中 | ❌ | 简单、入门 |
| **Calico** | BGP/IPIP | ✅ 高 | ✅ | 高性能+网络策略 |
| **Cilium** | eBPF | ✅✅ 最高 | ✅✅ | 云原生、可观测 |

---

## 6. K8s vs Docker Compose vs VM

| 维度 | Docker Compose | Kubernetes | 虚拟机部署 |
|------|:---:|:---:|:---:|
| 编排规模 | 单机 | 集群（数百台） | 手动/脚本 |
| 自动伸缩 | ❌ | ✅ HPA | ❌ |
| 故障自愈 | ⚠️ restart: always | ✅ 自动重启+迁移 | ❌ |
| 滚动更新 | ❌ | ✅ Deployment | ❌ |
| 服务发现 | ⚠️ DNS | ✅ CoreDNS | 手动 |
| 配置管理 | .env文件 | ConfigMap/Secret | 文件/脚本 |
| 学习曲线 | ✅ 低 | ❌ 高 | ✅ 中 |
| 适用 | 开发/测试/小项目 | 生产/中大型系统 | 传统/非容器化 |

---

> 🎯 **入门K8s的核心障碍不是记住API，而是理解"声明式"思维**：从"我告诉机器做什么"转变为"我描述想要什么，机器自己决定怎么做"。
