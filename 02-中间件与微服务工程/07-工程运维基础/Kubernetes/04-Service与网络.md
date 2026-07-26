# 04-Service与网络
> 🎯 Service是K8s中连接Pod与外部世界的桥梁 — 它为动态变化的Pod提供稳定的虚拟IP、DNS名称和负载均衡，是K8s网络模型中最核心的抽象之一

---

## 目录
1. [Service的本质](#1-service的本质)
2. [四种Service类型详解](#2-四种service类型详解)
3. [Service选择Pod的机制](#3-service选择pod的机制)
4. [kube-proxy工作模式](#4-kube-proxy工作模式)
5. [会话亲和性](#5-会话亲和性)
6. [无头Service (Headless)](#6-无头service-headless)
7. [CoreDNS服务发现](#7-coredns服务发现)
8. [Endpoint与EndpointSlice](#8-endpoint与endpointslice)
9. [NetworkPolicy网络隔离](#9-networkpolicy网络隔离)
10. [完整YAML示例](#10-完整yaml示例)

---

## 1. Service的本质

### 1.1 为什么需要Service

Pod的生命周期是短暂的：Pod可能因为Node故障、资源压力、应用崩溃或滚动更新而被销毁重建。每次重建后Pod的IP都会变化。

```text
问题：
  Pod A（IP: 10.244.1.5）→ 调用 Pod B（IP: 10.244.2.7）

  突然 Pod B 挂了，ReplicaSet重建一个新 Pod B'
  新 Pod B' 的 IP 变成了 10.244.2.15

  Pod A 无法访问 Pod B，因为 IP 变了！

Service解决的三个核心问题：
  1. ✅ 稳定的虚拟IP：无论Pod怎么变，Service的IP不变
  2. ✅ 服务发现：通过DNS名（user-service）而不是硬编码IP
  3. ✅ 负载均衡：多个副本间自动分发请求
```

### 1.2 Service的抽象模型

```text
┌────────────────────────────────────────────────────────┐
│                      Service                            │
│                    IP: 10.96.0.10                       │
│                 DNS: user-service                       │
│                   Port: 80                              │
│  ┌──────────────────────────────────────────────────┐  │
│  │            Label Selector: app=user-service       │  │
│  └──────────────────────────────────────────────────┘  │
│                         │                               │
│                         ▼                               │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐   │
│  │  Pod (v1)    │ │  Pod (v2)    │ │  Pod (v3)    │   │
│  │  10.244.1.5  │ │  10.244.2.7  │ │  10.244.3.9  │   │
│  │  8080        │ │  8080        │ │  8080        │   │
│  └──────────────┘ └──────────────┘ └──────────────┘   │
└────────────────────────────────────────────────────────┘
```

> 💡 **把Service想象成一个"反向代理"**：外界请求到达Service的虚拟IP:Port，Service把请求转发到后端某个Pod的IP:containerPort。这个转发过程对客户端完全透明。

### 1.3 Service的YAML骨架

```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service              # Service名称 → 也用作DNS名
  labels:
    app: user-service
spec:
  type: ClusterIP                  # Service类型（默认）
  selector:                        # 标签选择器 → 选择后端Pod
    app: user-service
  ports:                           # 端口映射
    - name: http                   # 端口名称（用于Service引用）
      protocol: TCP
      port: 80                     # Service暴露的端口
      targetPort: 8080             # Pod中容器的端口（可以是数字或名称）
```

---

## 2. 四种Service类型详解

### 2.1 类型总览

```text
                           外部用户
                               │
                               ▼
                    ┌──────────────────────┐
                    │    Internet / 用户     │
                    └──────────────────────┘
                               │
            ┌──────────────────┼──────────────────┐
            ▼                  ▼                  ▼
      ┌──────────┐      ┌──────────┐      ┌──────────┐
      │ NodePort │      │LoadBalancer│    │ExternalName│
      │30000-32767│    │  云LB→Node  │    │ DNS CNAME  │
      └────┬─────┘      └─────┬────┘      └──────────┘
           │                  │
           ▼                  ▼
      ┌──────────────────────────────┐
      │         ClusterIP            │
      │      10.96.x.x (虚拟IP)       │
      └──────────────┬───────────────┘
                     │
                     ▼
      ┌──────────────────────────────┐
      │       Pod (10.244.x.x)       │
      └──────────────────────────────┘
```

### 2.2 详细对比表

| 类型 | 访问方式 | 可访问范围 | 实现机制 | 典型场景 |
|------|---------|-----------|---------|---------|
| **ClusterIP** | `service-name:port` | 集群内部 | 分配虚拟IP，kube-proxy转发 | 内部微服务调用 |
| **NodePort** | `NodeIP:NodePort` | 集群内外均可 | 每个Node开放一个端口 → ClusterIP | 调试、无LB的环境 |
| **LoadBalancer** | `LB-DNS:port` | 公网 | 云厂商LB → NodePort → ClusterIP → Pod | 生产环境对外暴露 |
| **ExternalName** | DNS CNAME | 集群内部 | DNS返回外部域名CNAME | 访问外部服务 |

### 2.3 ClusterIP（默认）

```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  type: ClusterIP                    # 可省略（默认值）
  clusterIP: 10.96.0.50             # 可选：手动指定IP（一般不指定，让K8s自动分配）
  selector:
    app: user-service
  ports:
    - name: http
      protocol: TCP
      port: 80
      targetPort: 8080              # 对应Pod的containerPort
```

```text
ClusterIP工作原理：
  1. K8s分配一个虚拟IP（10.96.0.50）
  2. 这个IP只在集群内部可达（通过kube-proxy的iptables/IPVS规则）
  3. 集群内任何Pod都可以通过以下方式访问：
     └── http://user-service:80               （DNS名）
     └── http://user-service.default.svc:80   （完整DNS）
     └── http://10.96.0.50:80                 （ClusterIP直连，不推荐）
```

> 💡 **ClusterIP是K8s网络的基础**。所有内部微服务调用都应该使用ClusterIP。在SpringBoot中配置`SPRING_REDIS_HOST=redis-service`（而不是IP地址），K8s会自动解析到Redis Service。

### 2.4 NodePort

```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  type: NodePort
  selector:
    app: user-service
  ports:
    - port: 80                        # ClusterIP端口
      targetPort: 8080                # Pod端口
      nodePort: 30080                 # Node端口（可选，不指定则随机分配30000-32767）
```

```text
NodePort访问流程：
  外部请求 → http://192.168.1.10:30080
                                │
                                ▼
                         Node的iptables规则
                                │
                                ▼
                         Service ClusterIP:80
                                │
                                ▼
                         Pod:10.244.1.5:8080
```

| NodePort特性 | 说明 |
|-------------|------|
| 端口范围 | 30000-32767（可通过kube-apiserver参数配置） |
| 每个Node | 集群中每个Node都开放相同的NodePort |
| 安全性 | 建议配合防火墙使用，限制来源IP |
| 性能 | 比ClusterIP多一层NAT，多一次转发 |

> ⚠️ **NodePort不是生产环境的首选方案**。每个服务都需要一个端口，端口管理混乱。生产环境应该使用LoadBalancer或Ingress。

### 2.5 LoadBalancer

```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  type: LoadBalancer
  selector:
    app: user-service
  ports:
    - port: 80
      targetPort: 8080
  # 云厂商相关配置
  loadBalancerIP: 203.0.113.10       # 可选：指定LB的IP
  loadBalancerSourceRanges:           # 可选：白名单
    - 0.0.0.0/0                      # 允许所有来源
  externalTrafficPolicy: Local       # 保留客户端真实IP
```

```text
LoadBalancer 访问流程：
  用户 → http://myapp.example.com:80
                    │
                    ▼
          Cloud LB (阿里云SLB/AWS ELB/GCP LB)
                    │
                    ▼
          Node 1:NodePort → Service ClusterIP → Pod
          Node 2:NodePort → Service ClusterIP → Pod
          Node 3:NodePort → Service ClusterIP → Pod
```

| 云厂商 | LB产品 | LoadBalancer对应实现 |
|-------|--------|---------------------|
| 阿里云 | SLB | cloud-provider-alibaba |
| AWS | ELB/NLB | aws-load-balancer-controller |
| Azure | Azure LB | cloud-provider-azure |
| GCP | GCLB | cloud-provider-gcp |

#### externalTrafficPolicy 的作用

```yaml
# 默认：Cluster 模式
externalTrafficPolicy: Cluster
# 请求可能转发到其他Node上的Pod（SNAT后丢失客户端IP）
# 用户IP → Node1 → Node2上的Pod → 响应
# Node1的iptables做了SNAT，Pod看到的是Node1的IP而非用户IP

# Local 模式
externalTrafficPolicy: Local
# 请求只转发到本Node上的Pod（保留客户端IP）
# 用户IP → Node1 → Node1上的Pod → 响应
# Pod能看到客户端真实IP
# 缺点：如果本Node没有Pod，请求被丢弃
```

### 2.6 ExternalName

```yaml
apiVersion: v1
kind: Service
metadata:
  name: external-mysql             # 对内暴露为 external-mysql
spec:
  type: ExternalName
  externalName: rds.aliyuncs.com   # 外部真实服务DNS
  # 没有selector和ports，仅做DNS CNAME映射
```

```text
ExternalName 工作方式：
  在Pod内访问 external-mysql:3306
              │
              ▼  DNS解析
  CoreDNS返回 rds.aliyuncs.com 的CNAME记录
              │
              ▼
  客户端直接连接 rds.aliyuncs.com:3306（阿里云RDS）
```

> 💡 **ExternalName适合将外部服务（云数据库、第三方API）映射为集群内部服务名**。应用配置不需要知道是内网还是外网，统一使用Service名即可。切换内部/外部时只需修改Service的type，不改应用代码。

---

## 3. Service选择Pod的机制

### 3.1 Label Selector 原理

Service通过**标签选择器（Label Selector）**动态查找匹配的Pod。

```yaml
# Service
spec:
  selector:
    app: user-service
    tier: backend

# Pod必须有这两个标签才能被选中
metadata:
  labels:
    app: user-service     # ✅ 匹配
    tier: backend         # ✅ 匹配
    version: "1.0.0"      # ✅ 额外标签不影响
```

```text
Selector匹配逻辑：
  - Service要求的所有标签，Pod都必须有（AND逻辑）
  - Pod可以有额外标签，不影响
  - 标签值必须完全匹配

  类似于 SQL:  SELECT * FROM pods WHERE app='user-service' AND tier='backend'
```

### 3.2 动态变化的Pod集合

```text
时间线：
  t0: Pod[v1,v1,v1] → Service发现3个Pod → 添加到endpoints列表
  t1: 滚动更新开始 → 新Pod[v2]创建
                    → 新Pod标签匹配 → 自动加入Service
                    → 旧Pod被删 → 自动从Service移除
  t2: Pod[v2,v2,v2] → Service无缝切换完成

整个过程：应用无需重启，Service自动适应
```

### 3.3 验证Service选中的Pod

```bash
# 查看Service的Endpoints（选中的Pod IP列表）
kubectl get endpoints user-service
# NAME           ENDPOINTS                            AGE
# user-service   10.244.1.5:8080,10.244.2.7:8080      24h

# 查看Service详情
kubectl describe svc user-service
# Name:              user-service
# Namespace:         default
# Type:              ClusterIP
# IP:                10.96.0.50
# Port:              http  80/TCP
# TargetPort:        8080/TCP
# Endpoints:         10.244.1.5:8080,10.244.2.7:8080,10.244.3.9:8080
# Session Affinity:  None
```

---

## 4. kube-proxy工作模式

### 4.1 kube-proxy 是什么

kube-proxy是运行在每个Node上的网络代理，负责实现Service的负载均衡。

```text
kube-proxy 的职责：
  1. 监听API Server上Service和Endpoint的变化
  2. 在Node上创建和维护网络规则
  3. 将发往Service VIP的流量转发到后端Pod
```

### 4.2 三种工作模式

| 模式 | 原理 | 负载均衡算法 | 性能 | 适用版本 |
|------|------|:----------:|:----:|:--------:|
| **userspace** | 用户态代理（已废弃） | 轮询 | ❌ 慢 | v1.0前 |
| **iptables** | Linux内核Netfilter规则 | 随机（DNAT） | ⚠️ 中 | v1.1+（默认） |
| **IPVS** | Linux内核LVS | 多种算法 | ✅ 高 | v1.11+ |

### 4.3 iptables 模式（默认）

```text
iptables 工作流程：

  Pod → 访问 10.96.0.50:80  (Service VIP)
              │
              ▼
        PREROUTING Chain
              │
              ▼
        KUBE-SERVICES Chain (所有Service规则)
              │
              ▼
        匹配 Service: 10.96.0.50:80
              │
              ▼
        KUBE-SVC-XXXXX Chain (负载均衡)
         │      │      │
         ▼      ▼      ▼
    KUBE-SEP-1 KUBE-SEP-2 KUBE-SEP-3
    (随机DNAT) (随机DNAT) (随机DNAT)
         │
         ▼
       10.244.1.5:8080 (Pod IP)
```

```text
iptables 模式的优缺点：

✅ 优点：
  - 纯内核态处理（比userspace快）
  - 不需要用户态进程转发
  - 成熟稳定，所有Linux版本都支持

❌ 缺点：
  - 规则数量与Service+Pod数量成正比
  - 5K+条规则时查表延迟明显
  - 负载均衡只能随机（不能加权、不能一致性哈希）
  - 更新规则时需要全量刷新（可能导致短暂连接中断）
```

### 4.4 IPVS 模式（高性能）

```bash
# 检查当前kube-proxy模式
kubectl get configmap kube-proxy -n kube-system -o yaml | grep mode

# 切换到IPVS模式（修改ConfigMap后重启kube-proxy）
# configmap中设置 mode: "ipvs"
```

```text
IPVS 模式的优缺点：

✅ 优点：
  - 基于LVS内核模块，性能极高（O(1)查找）
  - 支持多种负载均衡算法
  - 支持大规模集群（上万条规则）
  - 规则更新是增量式的（不会中断连接）

❌ 缺点：
  - 需要加载ip_vs内核模块
  - 部分K8s发行版默认未开启
  - 需要额外安装ipvsadm工具
```

| IPVS算法 | 名称 | 说明 |
|:--------:|------|------|
| rr | Round Robin | 轮询（默认） |
| wrr | Weighted Round Robin | 加权轮询 |
| lc | Least Connection | 最少连接数 |
| wlc | Weighted Least Connection | 加权最少连接 |
| sh | Source Hashing | 源地址哈希（类似会话亲和性） |
| dh | Destination Hashing | 目标地址哈希 |

### 4.5 模式选择建议

| 集群规模 | 推荐模式 | 理由 |
|---------|:-------:|------|
| < 50个Service | iptables | 简单、零配置、足够用 |
| 50-500个Service | IPVS | 性能更优、算法可选 |
| > 500个Service | IPVS | iptables查表成为瓶颈 |
| 所有规模（推荐） | IPVS | 性能更好，功能更丰富 |

---

## 5. 会话亲和性

### 5.1 问题场景

```text
问题：无会话亲和性时
  请求1 → 客户端A → Service → Pod-1（登录成功，session存于Pod-1内存）
  请求2 → 客户端A → Service → Pod-2（未登录，需要重新登录）

解决方案：会话亲和性（Session Affinity）
  让同一个客户端的请求始终转发到同一个Pod
```

### 5.2 配置方式

```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  selector:
    app: user-service
  ports:
    - port: 80
      targetPort: 8080
  sessionAffinity: ClientIP           # 基于客户端IP的会话亲和性
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 10800           # 会话保持时间（默认10800=3小时）
```

| 模式 | 值 | 说明 |
|------|:---:|------|
| 关闭 | `None` | 默认，请求随机分配 |
| 开启 | `ClientIP` | 同一IP的请求到同一Pod |

### 5.3 注意事项

> ⚠️ **会话亲和性的两大问题：**
> 1. **流量不均**：如果某个客户端IP发请求特别多，会导致某个Pod压力特别大
> 2. **NAT导致失效**：多个真实用户经过同一个NAT出口，会被认为是同一个ClientIP，全部转发到同一个Pod
>
> **最佳实践**：不要依赖Session亲和性，而应该使用**中央化Session存储**（Redis）或**JWT无状态token**。这样每个Pod都可以处理任意用户请求，才真正实现水平伸缩。

---

## 6. 无头Service (Headless)

### 6.1 什么是Headless Service

Headless Service不分配ClusterIP，而是直接返回后端所有Pod的IP列表。

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql-headless
spec:
  clusterIP: None              # ⭐ 关键：ClusterIP设为None
  selector:
    app: mysql
  ports:
    - port: 3306
      targetPort: 3306
```

```text
普通Service vs Headless Service：

普通Service（ClusterIP=10.96.0.50）：
  DNS解析 user-service.default.svc.cluster.local
  → 返回 10.96.0.50（虚拟IP，由kube-proxy转发）

Headless Service（clusterIP=None）：
  DNS解析 mysql-headless.default.svc.cluster.local
  → 返回 Pod IP列表 [10.244.1.5, 10.244.2.7, 10.244.3.9]
  → 也可以查单个Pod：mysql-0.mysql-headless.default.svc.cluster.local
```

### 6.2 配合StatefulSet使用

Headless Service和StatefulSet是最佳搭档：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql-h
spec:
  clusterIP: None
  selector:
    app: mysql
  ports:
    - port: 3306
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql
spec:
  serviceName: mysql-h              # ⭐ 关联Headless Service
  replicas: 3
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
```

```text
StatefulSet + Headless Service 的DNS：

  Pod 0 的 DNS：mysql-0.mysql-h.default.svc.cluster.local → 10.244.1.5
  Pod 1 的 DNS：mysql-1.mysql-h.default.svc.cluster.local → 10.244.2.7
  Pod 2 的 DNS：mysql-2.mysql-h.default.svc.cluster.local → 10.244.3.9

  MySQL主从复制：
    主库：mysql-0.mysql-h.default.svc.cluster.local:3306
    从库：mysql-1.mysql-h.default.svc.cluster.local:3306
```

### 6.3 使用场景

| 场景 | 说明 | 示例 |
|------|------|------|
| **StatefulSet** | 每个Pod需要稳定的网络标识 | MySQL主从、Redis集群、ZooKeeper |
| **自定义负载均衡** | 客户端自己做负载均衡 | gRPC客户端侧负载均衡 |
| **查找所有Pod** | 需要知道集群中所有Pod实例 | 服务发现、集群管理 |

---

## 7. CoreDNS服务发现

### 7.1 CoreDNS在K8s中的角色

K8s 1.13+使用CoreDNS作为默认的集群DNS服务器，替代了原来的kube-dns。

```text
CoreDNS的部署形式：
  ┌────────────────────────────────────────┐
  │  kube-system 命名空间                    │
  │  ┌──────────────────────────────────┐  │
  │  │  Deployment: coredns (2个副本)     │  │
  │  │  配置: ConfigMap: coredns         │  │
  │  │  Service: kube-dns (ClusterIP)    │  │
  │  │  Pod内的 /etc/resolv.conf:        │  │
  │  │    nameserver 10.96.0.10          │  │
  │  │    search default.svc.cluster.local │  │
  │  │    search svc.cluster.local         │  │
  │  └──────────────────────────────────┘  │
  └────────────────────────────────────────┘
```

### 7.2 DNS域名格式

```text
完整的DNS域名：
  <service>.<namespace>.svc.cluster.local

示例：
  ┌──────────────────┬─────────────────────────────────────┐
  │ 缩写形式          │ 完整形式                             │
  ├──────────────────┼─────────────────────────────────────┤
  │ user-service     │ user-service.default.svc.cluster.local │
  │ redis-service    │ redis-service.default.svc.cluster.local│
  │ mysql-h          │ mysql-h.default.svc.cluster.local     │
  │ mysql-0.mysql-h  │ mysql-0.mysql-h.default.svc.cluster.local │
  └──────────────────┴─────────────────────────────────────┘

跨命名空间访问：
  user-service.production.svc.cluster.local
  redis-service.cache.svc.cluster.local
  mysql-h.database.svc.cluster.local
```

### 7.3 Pod内的DNS解析

```bash
# 在Pod内测试DNS解析
kubectl exec -it user-service-7d9f8c9b6-xk4f3 -- sh

# 查看DNS配置
cat /etc/resolv.conf
# nameserver 10.96.0.10
# search default.svc.cluster.local svc.cluster.local cluster.local
# options ndots:5

# 测试DNS解析
nslookup user-service
# Server:    10.96.0.10
# Address:   10.96.0.10#53
# Name:      user-service.default.svc.cluster.local
# Address:   10.96.0.50

# 测试Headless Service
nslookup mysql-h
# Server:    10.96.0.10
# Address:   10.96.0.10#53
# Name:      mysql-h.default.svc.cluster.local
# Address:   10.244.1.5
# Name:      mysql-h.default.svc.cluster.local
# Address:   10.244.2.7
# Name:      mysql-h.default.svc.cluster.local
# Address:   10.244.3.9
```

> 💡 **SpringBoot不需要额外配置DNS**，使用Service名作为数据库、Redis等地址即可。例如`spring.redis.host=redis-service`，K8s的CoreDNS会自动解析为Service的ClusterIP。

### 7.4 Pod DNS 策略

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: custom-dns-pod
spec:
  dnsPolicy: ClusterFirst          # 默认：先查集群DNS，失败再查上游
  # dnsPolicy: None                 # 自定义DNS，需指定dnsConfig
  # dnsPolicy: Default              # 继承Node的DNS（即宿主机的/etc/resolv.conf）
  # dnsPolicy: ClusterFirstWithHostNet # 使用hostNetwork时仍需集群DNS

  dnsConfig:                        # dnsPolicy=None时必须
    nameservers:
      - 8.8.8.8
      - 114.114.114.114
    searches:
      - default.svc.cluster.local
    options:
      - name: ndots
        value: "5"
  containers:
    - name: app
      image: app
```

---

## 8. Endpoint与EndpointSlice

### 8.1 Endpoints

当Service通过selector匹配到Pod时，K8s自动创建同名的Endpoints对象，记录所有选中Pod的IP:Port。

```yaml
# Service
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  selector:
    app: user-service
  ports:
    - port: 80
      targetPort: 8080
---
# Endpoints（自动创建，无需手动编写）
apiVersion: v1
kind: Endpoints
metadata:
  name: user-service                    # 与Service同名
subsets:
  - addresses:
      - ip: 10.244.1.5                  # Pod IP
        nodeName: node-01
        targetRef:
          kind: Pod
          name: user-service-7d9f8c9b6-xk4f3
      - ip: 10.244.2.7
        nodeName: node-02
      - ip: 10.244.3.9
        nodeName: node-03
    ports:
      - port: 8080
        name: http
        protocol: TCP
```

### 8.2 手动创建Endpoint（不通过Selector）

```yaml
# 场景：Service想代理集群外部的服务（不通过Pod）
apiVersion: v1
kind: Service
metadata:
  name: external-mysql
spec:
  # 没有selector — K8s不会自动创建Endpoints
  ports:
    - port: 3306
      targetPort: 3306
---
apiVersion: v1
kind: Endpoints
metadata:
  name: external-mysql        # 必须与Service同名
subsets:
  - addresses:
      - ip: 192.168.1.100     # 外部MySQL的内网IP
    ports:
      - port: 3306
```

> 💡 **手动创建Endpoints可以实现"Service代理外部服务"的效果**，比ExternalName更灵活（支持IP而不是只能DNS）。

### 8.3 EndpointSlice（大规模集群优化）

EndpointSlice是Endpoints的新一代API（K8s v1.21+），解决了单个Endpoints对象过大时的问题。

```text
Endpoints vs EndpointSlice：

Endpoints（旧）：
  一个Service → 一个Endpoints对象
  最大支持1000个端点（否则需要切分）

EndpointSlice（新）：
  一个Service → 多个EndpointSlice对象
  每个Slice最多100个端点
  支持拓扑感知路由（将流量优先路由到同区域Pod）
```

| 特性 | Endpoints | EndpointSlice |
|------|:---------:|:-------------:|
| API版本 | v1 | discovery.k8s.io/v1 |
| 最大端点/对象 | 1000 | 100 |
| 拓扑感知路由 | ❌ | ✅ |
| K8s版本 | 一直支持 | v1.21+ 默认启用 |

---

## 9. NetworkPolicy网络隔离

### 9.1 什么是NetworkPolicy

默认情况下，K8s集群中所有Pod可以互相通信（全连通网络）。NetworkPolicy用于实现**网络隔离**，定义哪些Pod可以访问哪些Pod。

```text
没有NetworkPolicy：
  Pod-A ────────────→ Pod-B
  Pod-A ────────────→ Pod-C
  Pod-B ────────────→ Pod-C
  ... 所有Pod可以任意互访

有NetworkPolicy（默认拒绝+白名单）：
  默认：所有入站流量被拒绝
  允许：来自特定Pod或特定IP段的流量
```

> ⚠️ **NetworkPolicy需要CNI网络插件支持**。Flannel不支持NetworkPolicy，Calico和Cilium支持。如果CNI不支持，NetworkPolicy定义不会生效（所有Pod仍可互访）。

| CNI插件 | NetworkPolicy支持 |
|---------|:----------------:|
| Flannel | ❌ |
| Calico | ✅ |
| Cilium | ✅（eBPF实现，性能最高） |
| Weave Net | ✅ |
| Kube-router | ✅ |

### 9.2 NetworkPolicy语法

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: api-network-policy
  namespace: production
spec:
  # 1. 选择策略作用的Pod
  podSelector:
    matchLabels:
      app: user-service

  # 2. 策略类型（双向控制）
  policyTypes:
    - Ingress              # 入站流量控制
    - Egress               # 出站流量控制

  # 3. 入站规则：允许谁访问user-service
  ingress:
    - from:
        # 允许来自相同命名空间、相同app标签的Pod
        - podSelector:
            matchLabels:
              app: api-gateway
        # 允许来自指定命名空间的所有Pod
        - namespaceSelector:
            matchLabels:
              name: monitoring
        # 允许来自指定IP段
        - ipBlock:
            cidr: 10.0.0.0/8
            except:
              - 10.96.0.0/12      # 排除Service CIDR
      ports:
        - protocol: TCP
          port: 8080               # 只允许访问8080端口

  # 4. 出站规则：允许user-service访问谁
  egress:
    - to:
        - podSelector:             # 允许访问数据库
            matchLabels:
              app: mysql
      ports:
        - protocol: TCP
          port: 3306
    - to:
        - namespaceSelector:       # 允许访问CoreDNS
            matchLabels:
              k8s-app: kube-dns
      ports:
        - protocol: UDP
          port: 53
    - to:                          # 允许访问外部HTTPS
        - ipBlock:
            cidr: 0.0.0.0/0
      ports:
        - protocol: TCP
          port: 443
```

### 9.3 默认隔离策略

```yaml
# 默认拒绝所有入站流量
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-ingress
spec:
  podSelector: {}                  # 选择命名空间下所有Pod
  policyTypes:
    - Ingress

---
# 默认拒绝所有出站流量
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-egress
spec:
  podSelector: {}
  policyTypes:
    - Egress

---
# 默认允许所有入站流量（相当于没有策略）
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-allow-ingress
spec:
  podSelector: {}
  ingress:
    - {}
  policyTypes:
    - Ingress
```

### 9.4 零信任网络模型

```text
推荐的安全模型：

  基础策略（Default-Deny）:
    ┌────────────────────────────────┐
    │  production命名空间             │
    │  默认拒绝所有入站和出站           │
    └────────────────────────────────┘

  白名单规则:
    ┌──────────────┐    ┌──────────────┐
    │  api-gateway  │───▶│ user-service │
    │  (允许入站)    │    │              │
    └──────────────┘    └──────┬───────┘
                               │
                     ┌─────────▼────────┐
                     │     mysql         │
                     │    (允许出站3306)  │
                     └──────────────────┘

  实现效果：
    api-gateway → user-service:8080  ✅
    api-gateway → mysql:3306         ❌  （隔离）
    user-service → mysql:3306        ✅
    user-service → Internet          ❌  （隔离）
```

> 💡 **零信任网络的核心原则**：显式允许，隐式拒绝。不因为两个服务在同一命名空间就默认互通。每个连通路径都需要显式声明在NetworkPolicy中。

---

## 10. 完整YAML示例

### 10.1 user-service ClusterIP + 内部Service

```yaml
# 1. user-service的ClusterIP Service
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: production
  labels:
    app: user-service
    tier: backend
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
spec:
  type: ClusterIP
  selector:
    app: user-service
  ports:
    - name: http
      protocol: TCP
      port: 80                          # 对外暴露80端口
      targetPort: 8080                  # 转发到Pod的8080
    - name: actuator
      protocol: TCP
      port: 8081
      targetPort: 8081

---
# 2. order-service（调用user-service）
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: production
spec:
  replicas: 2
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: order-service:1.0.0
          env:
            # 使用Service名调用user-service（不用IP）
            - name: USER_SERVICE_URL
              value: "http://user-service:80/api/users"
            - name: SPRING_PROFILES_ACTIVE
              value: "k8s"

---
# 3. redis-service
apiVersion: v1
kind: Service
metadata:
  name: redis-service
  namespace: production
spec:
  type: ClusterIP
  selector:
    app: redis
  ports:
    - name: redis
      protocol: TCP
      port: 6379
      targetPort: 6379
```

### 10.2 外部MySQL的ExternalName

```yaml
# 4. 外部MySQL Service（ExternalName）
apiVersion: v1
kind: Service
metadata:
  name: mysql-service
  namespace: production
  labels:
    app: mysql
spec:
  type: ExternalName
  externalName: mydb.1234567890.rds.cn-chengdu.aliyuncs.com
  ports:
    - port: 3306
      targetPort: 3306
```

### 10.3 完整的网络策略

```yaml
# 5. 生产环境默认拒绝策略
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
  namespace: production
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress

---
# 6. 允许user-service被API Gateway访问
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-ingress-to-user-service
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: user-service
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: api-gateway
      ports:
        - protocol: TCP
          port: 8080
    - from:
        - namespaceSelector:
            matchLabels:
              name: monitoring
      ports:
        - protocol: TCP
          port: 8081

---
# 7. 允许user-service出站访问MySQL和DNS
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-egress-from-user-service
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: user-service
  policyTypes:
    - Egress
  egress:
    - to:
        - ipBlock:
            cidr: 100.64.0.0/10          # 阿里云RDS内网网段
      ports:
        - protocol: TCP
          port: 3306
    - to:
        - namespaceSelector: {}           # 所有命名空间的DNS
          podSelector:
            matchLabels:
              k8s-app: kube-dns
      ports:
        - protocol: UDP
          port: 53
        - protocol: TCP
          port: 53
    - to:
        - ipBlock:
            cidr: 0.0.0.0/0             # 允许访问互联网（仅443）
      ports:
        - protocol: TCP
          port: 443

---
# 8. user-service的Headless Service（gRPC使用）
apiVersion: v1
kind: Service
metadata:
  name: user-service-headless
  namespace: production
spec:
  clusterIP: None
  selector:
    app: user-service
  ports:
    - name: grpc
      port: 9090
      targetPort: 9090
```

### 10.4 SpringBoot配置示例

```yaml
# application-k8s.yml
spring:
  datasource:
    # 使用Service名而不是IP地址
    url: jdbc:mysql://mysql-service:3306/user_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  redis:
    host: redis-service
    port: 6379

# 调用其他Service
app:
  services:
    order-service: http://order-service:80/api/orders
    inventory-service: http://inventory-service:80/api/inventory
```

---

> 🎯 **Service是K8s网络的灵魂**。理解Service的四种类型（ClusterIP/NodePort/LoadBalancer/ExternalName）、kube-proxy的转发模式（iptables vs IPVS）、Headless Service与StatefulSet的配合、CoreDNS的服务发现机制，以及NetworkPolicy的零信任网络隔离，才能真正掌握K8s网络模型。下一章将介绍如何通过Ingress实现HTTP七层路由。
