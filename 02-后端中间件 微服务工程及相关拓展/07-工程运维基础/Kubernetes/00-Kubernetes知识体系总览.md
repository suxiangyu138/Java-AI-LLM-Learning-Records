# 00-Kubernetes知识体系总览
> 🎯 K8s是云原生时代的基础设施标准 — 从核心概念到SpringBoot生产部署，掌握容器编排的完整知识体系，让Java应用在云端自动伸缩、自愈、零宕机

---

## 目录
1. [K8s知识全景](#1-k8s知识全景)
2. [知识文件导航](#2-知识文件导航)
3. [精通级学习路线](#3-精通级学习路线)
4. [与Docker/SpringBoot的关系](#4-与dockerspringboot的关系)

---

## 1. K8s知识全景

```
Kubernetes 精通体系（12个文件）
│
├── 🏗️ 核心概念与架构
│   ├── K8s是什么：容器编排平台、声明式API、自愈系统
│   ├── 架构组件：Master(Node/API Server/etcd/Scheduler/Controller Manager)
│   │             vs Worker(kubelet/kube-proxy/Container Runtime)
│   └── 核心抽象：Pod/Service/Deployment/ConfigMap/Secret/Volume/Ingress
│       → 01-Kubernetes核心概念与架构.md
│
├── 📦 Pod — 最小部署单元
│   ├── Pod生命周期：Pending→Running→Succeeded/Failed
│   ├── 多容器Pod模式：Sidecar/Ambassador/Adapter
│   └── Init Container：初始化任务
│       → 02-Pod详解.md
│
├── 🔄 Deployment — 无状态工作负载
│   ├── 声明式副本管理、ReplicaSet
│   ├── 滚动更新：RollingUpdate vs Recreate
│   └── 回滚：kubectl rollout undo
│       → 03-Deployment与滚动更新.md
│
├── 🌐 Service — 服务发现与负载均衡
│   ├── ClusterIP/NodePort/LoadBalancer/ExternalName
│   ├── kube-proxy：iptables vs IPVS
│   └── DNS服务发现：<service>.<namespace>.svc.cluster.local
│       → 04-Service与网络.md
│
├── ⚙️ 配置管理
│   ├── ConfigMap：非敏感配置（application.yml）
│   ├── Secret：敏感配置（数据库密码）
│   └── 热更新：挂载卷自动更新 vs subPath限制
│       → 05-ConfigMap与Secret.md
│
├── 💾 存储
│   ├── Volume类型：emptyDir/hostPath/nfs/cloud
│   ├── PV/PVC：持久卷与声明
│   └── StorageClass：动态存储供应
│       → 06-存储与持久化.md
│
├── 🚪 Ingress — HTTP路由
│   ├── Ingress Controller：Nginx/Traefik
│   ├── 路由规则：Host/Path/TLS
│   └── Ingress vs Gateway API
│       → 07-Ingress与七层路由.md
│
├── 📈 弹性伸缩
│   ├── HPA：基于CPU/Memory/自定义指标
│   ├── VPA：垂直扩缩容
│   └── Cluster Autoscaler：节点级弹性
│       → 08-HPA弹性伸缩.md
│
├── 🏭 SpringBoot K8s实战
│   ├── 完整部署：Dockerfile→Deployment→Service→Ingress
│   ├── JVM容器适配：-XX:+UseContainerSupport
│   └── CI/CD：GitHub Actions→Build→Push→Deploy
│       → 09-SpringBoot应用K8s部署实战.md
│
├── 🩺 健康检查
│   ├── Liveness Probe(存活)/Readiness Probe(就绪)/Startup Probe
│   ├── SpringBoot Actuator + K8s健康检查
│   └── Graceful Shutdown：preStop Hook + Spring优雅停机
│       → 10-K8s健康检查与零宕机.md
│
└── 📊 可观测性
    ├── 日志：EFK(Elasticsearch+Fluentd+Kibana)
    ├── 监控：Prometheus + Grafana
    └── 链路追踪：Jaeger/SkyWalking
        → 11-K8s日志与监控.md
```

---

## 2. 知识文件导航

| # | 文件 | 级别 | 时间 |
|---|------|:---:|:---:|
| 00 | K8s知识体系总览 | — | 10min |
| 01 | K8s核心概念与架构 | ⭐⭐ | 2h |
| 02 | Pod详解 | ⭐⭐ | 1.5h |
| 03 | Deployment与滚动更新 | ⭐⭐⭐ | 1.5h |
| 04 | Service与网络 | ⭐⭐⭐ | 2h |
| 05 | ConfigMap与Secret | ⭐⭐ | 1h |
| 06 | 存储与持久化 | ⭐⭐ | 1.5h |
| 07 | Ingress与七层路由 | ⭐⭐⭐ | 1h |
| 08 | HPA弹性伸缩 | ⭐⭐⭐ | 1h |
| 09 | SpringBoot K8s部署实战 | ⭐⭐⭐⭐ | 3h |
| 10 | 健康检查与零宕机 | ⭐⭐⭐⭐ | 1.5h |
| 11 | K8s日志与监控 | ⭐⭐⭐ | 1.5h |

---

## 3. 精通级学习路线

### 🟢 L1：理解概念（半天）
```
00总览 → 01核心概念与架构 → 02-Pod → 03-Deployment
产出：理解K8s是做什么的、核心抽象有哪些
```

### 🔵 L2：能部署应用（1天）
```
04-Service → 05-ConfigMap/Secret → 07-Ingress → 09-SpringBoot实战
产出：能把SpringBoot应用完整部署到K8s集群
```

### 🟣 L3：生产运维（2天）
```
06-存储 → 08-HPA → 10-健康检查 → 11-日志监控
产出：能在生产环境运维K8s中的SpringBoot应用
```

---

## 4. 与Docker/SpringBoot的关系

```
Docker       → 打包应用为容器镜像（单机运行）
Docker Compose → 单机编排多容器
Kubernetes   → 集群级容器编排（多机、自动伸缩、自愈）

SpringBoot + Docker + K8s = 云原生Java应用标准栈：
  代码 → Dockerfile → 镜像 → K8s Deployment → Service暴露 → Ingress路由 → 生产运行
```

> 🎯 **K8s不是替代Docker，而是编排Docker容器**。Docker负责"打包"，K8s负责"在哪跑、跑几个、挂了怎么办"。
