# 03-Deployment与滚动更新
> 🎯 Deployment是K8s中管理无状态工作负载的核心控制器 — 它封装了副本管理、滚动更新、回滚、自愈和弹性伸缩，让Java应用发布变成声明式的"我要这个版本跑3个"而非命令式的"先停旧再启新"

---

## 目录
1. [Deployment是什么](#1-deployment是什么)
2. [Deployment三要素](#2-deployment三要素)
3. [ReplicaSet详解](#3-replicaset详解)
4. [滚动更新机制](#4-滚动更新机制)
5. [更新策略对比](#5-更新策略对比)
6. [回滚操作](#6-回滚操作)
7. [更新暂停与恢复与金丝雀发布](#7-更新暂停与恢复与金丝雀发布)
8. [部署策略对比表](#8-部署策略对比表)
9. [K8s工作负载对比](#9-k8s工作负载对比)
10. [完整SpringBoot Deployment YAML](#10-完整springboot-deployment-yaml)

---

## 1. Deployment是什么

### 1.1 一句话定义

**Deployment = 副本管理 + 滚动更新 + 回滚 + 自愈的声明式控制器。**

```text
没有Deployment的时代：
  1. SSH到所有机器
  2. 手动停止旧应用
  3. 启动新应用
  4. 检查是否启动成功
  5. 一台一台重复

有Deployment的时代：
  kubectl apply -f deployment.yaml   ← 搞定

  K8s自动做：
  ├── 保证3个Pod始终运行
  ├── 某个Pod挂了 → 自动新建一个
  ├── Node宕机了 → 在健康Node上重建
  ├── 更新版本 → 滚动替换，零停机
  └── 新版本有问题 → 一键回滚
```

### 1.2 Deployment的工作循环

```text
控制循环（Control Loop）：
  Deployment Controller ←→ API Server

  用户: 创建Deployment（replicas: 3, image: v2）
        │
        ▼
  Deployment Controller: 创建ReplicaSet(rs-v2)
        │
        ▼
  ReplicaSet Controller: 创建3个Pod
        │
        ▼
  Scheduler: 分配Node
        │
        ▼
  kubelet: 在每个Node上启动Pod
        │
        ▼
  持续监控 → 如果Pod挂了 → 重新创建
```

---

## 2. Deployment三要素

一个Deployment定义由三个核心部分组成：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service                    # Deployment名称
  labels:
    app: user-service
spec:
  replicas: 3               # ⭐ 要素1：期望副本数
  selector:                 # ⭐ 要素2：选择Pod的标签
    matchLabels:
      app: user-service
  template:                 # ⭐ 要素3：Pod模板
    metadata:
      labels:
        app: user-service   # 必须与selector.matchLabels匹配
    spec:
      containers:
        - name: user-service
          image: user-service:1.0.0
          ports:
            - containerPort: 8080
```

### 2.1 要素一：replicas（副本数）

| 概念 | 说明 |
|------|------|
| 期望值 | 用户声明的副本数，K8s保证始终是这个数 |
| 自愈 | Pod挂了自动创建新的补上 |
| 伸缩 | 修改replicas即可水平伸缩（或配合HPA自动伸缩） |

```bash
# 手动缩放到5个副本（临时应急）
kubectl scale deployment user-service --replicas=5

# 或修改YAML后apply
kubectl apply -f deployment.yaml
```

### 2.2 要素二：selector（标签选择器）

selector决定了Deployment**管理哪些Pod**。这是Deployment和Pod之间建立关系的桥梁。

```yaml
selector:
  matchLabels:          # 精确匹配
    app: user-service
  # matchExpressions:   # 表达式匹配（可选）
  #   - key: tier
  #     operator: In
  #     values:
  #       - backend
```

> ⚠️ **selector一旦创建不可修改**。如果必须修改，只能删除重建Deployment。所以创建Deployment时selector要规划好。

### 2.3 要素三：template（Pod模板）

template定义了Deployment创建Pod时的"蓝图"。template和普通Pod定义唯一的区别是：template**必须**包含和selector匹配的labels。

```yaml
template:
  metadata:
    labels:
      app: user-service       # 必须匹配selector
      version: "1.0.0"       # 额外标签，不影响选择
  spec:
    containers:
      - ...
```

> 💡 **Pod模板中labels和annotation可以任意添加**，但至少要有与selector匹配的标签。额外标签可以用于区分版本、区分环境等。

---

## 3. ReplicaSet详解

### 3.1 ReplicaSet是什么

**ReplicaSet**是Deployment自动创建和管理的中间层资源。用户不需要手动操作ReplicaSet，它是Deployment的内部实现机制。

```text
Deployment  →  创建/管理  →  ReplicaSet  →  创建/管理  →  Pod

                      ┌─────────────────────────────────┐
                      │     Deployment: user-service     │
                      │     ┌───────────────────────┐   │
                      │     │  RS 1 (v1.0.0): 0 Pods│   │  ← 历史版本，缩到0
                      │     ├───────────────────────┤   │
                      │     │  RS 2 (v1.1.0): 3 Pods│   │  ← 当前版本，运行中
                      │     └───────────────────────┘   │
                      └─────────────────────────────────┘
```

### 3.2 ReplicaSet的命名规则

```text
Deployment:   user-service
ReplicaSet:   user-service-7d9f8c9b6     ← Deployment名 + Pod模板hash
Pod:          user-service-7d9f8c9b6-xk4f3   ← ReplicaSet名 + 随机ID

这样命名的好处：通过Pod名就能知道属于哪个ReplicaSet
```

### 3.3 Deployment滚动更新时的ReplicaSet变化

```bash
# 更新前：1个RS，3个Pod
kubectl get rs
NAME                     DESIRED   CURRENT   READY
user-service-7d9f8c9b6   3         3         3

# 更新镜像到v1.1.0后：2个RS
kubectl get rs
NAME                     DESIRED   CURRENT   READY
user-service-7d9f8c9b6   0         0         0      # 旧版RS缩到0（保留用于回滚）
user-service-8e1a9c7d4   3         3         3      # 新版RS
```

> 💡 **旧版ReplicaSet的Pod缩到0但RS对象不删除**，保留它们是为了回滚时能快速扩容。K8s默认保留10个历史RS，可通过`spec.revisionHistoryLimit`配置。

---

## 4. 滚动更新机制

### 4.1 滚动更新流程

滚动更新（RollingUpdate）是最常用的更新策略，逐个替换旧Pod为新Pod，整个过程服务不中断。

```text
更新前：3个Pod全部是v1
  [v1] [v1] [v1]

开始滚动更新（maxSurge=1, maxUnavailable=1）：
  Step 1: 新建1个v2 Pod（maxSurge=1，总Pod数=4）
  [v1] [v1] [v1] [v2]

  Step 2: 停止1个v1 Pod（maxUnavailable=1，总Pod数=3）
  [v1] [v1] [v2] [v2-pending] → 等待v2 Ready
        ↓
  [v1] [v1] [v2]

  Step 3: 新建1个v2 Pod（总Pod数=4）
  [v1] [v1] [v2] [v2]

  Step 4: 停止1个v1 Pod
  [v1] [v2] [v2]

  Step 5: 新建1个v2 Pod（总Pod数=4）
  [v1] [v2] [v2] [v2]

  Step 6: 停止最后1个v1 Pod
  [v2] [v2] [v2]

更新完成：3个Pod全部是v2
  [v2] [v2] [v2]
```

### 4.2 maxSurge 和 maxUnavailable

这两个参数控制滚动更新的**速度**和**资源消耗**：

| 参数 | 含义 | 可以是 | 默认值 |
|------|------|--------|:------:|
| **maxSurge** | 更新过程中最多可以多出多少个Pod（超出期望副本数） | 绝对值（1）或百分比（25%） | 25% |
| **maxUnavailable** | 更新过程中最多允许多少个Pod不可用 | 绝对值（1）或百分比（25%） | 25% |

```yaml
spec:
  replicas: 10
  strategy:
    rollingUpdate:
      maxSurge: 2           # 最多多出2个Pod → 最大同时12个Pod
      maxUnavailable: 1     # 最多1个Pod不可用 → 至少9个Pod可用
```

```text
maxSurge 和 maxUnavailable 的配合效果：

maxSurge=1, maxUnavailable=1（保守）:
  逐台替换，速度慢但最安全，适合核心服务

maxSurge=3, maxUnavailable=3（激进）:
  一次替换3个，速度快但资源消耗大，适合批量更新

maxSurge=25%, maxUnavailable=25%（默认）:
  replicas=4 → 每次替换1个（25% × 4 = 1）
  replicas=10 → 每次替换3个（25% × 10 = 2.5 → ceil=3）

maxSurge=0（零额外资源）:
  必须maxUnavailable≥1，先停旧再启新
  会导致 Pod 数低于期望值，对高可用有影响
```

> 💡 **生产环境核心服务推荐 `maxSurge=1, maxUnavailable=0`**：每次只新建一个Pod，等它Ready后再停一个旧Pod。虽然慢（3副本需要3轮），但保证服务容量始终不降低。

### 4.3 滚动更新触发条件

修改Deployment中的以下字段会触发滚动更新：

| 字段 | 示例 | 触发更新 |
|------|------|:--------:|
| **image** | 修改container镜像tag | ✅ |
| **resources** | 修改CPU/Memory限制 | ✅ |
| **env** | 增加/修改环境变量 | ✅ |
| **ports** | 修改容器端口 | ✅ |
| **command/args** | 修改启动命令 | ✅ |
| **volumeMounts** | 修改挂载卷 | ✅ |
| **replicas** | 修改副本数 | ❌（仅伸缩） |

```bash
# 触发滚动更新的三种方式
# 方式1：修改YAML
kubectl apply -f deployment.yaml

# 方式2：直接设置镜像（便捷方式）
kubectl set image deployment/user-service user-service=registry.example.com/user-service:1.1.0

# 方式3：编辑Deployment
kubectl edit deployment user-service
```

### 4.4 滚动更新状态查看

```bash
# 查看更新状态
kubectl rollout status deployment/user-service
# 输出：
# Waiting for rollout to finish: 1 out of 3 new replicas have been updated...
# Waiting for rollout to finish: 2 out of 3 new replicas have been updated...
# deployment "user-service" successfully rolled out

# 查看更新历史
kubectl rollout history deployment/user-service
# deployment.apps/user-service
# REVISION  CHANGE-CAUSE
# 1         <none>                     # 第一次部署
# 2         <none>                     # 第一次更新

# 查看指定版本的详细信息
kubectl rollout history deployment/user-service --revision=2

# 给更新添加说明（方便追溯）
kubectl annotate deployment/user-service kubernetes.io/change-cause="升级到1.1.0，修复NPE"
```

---

## 5. 更新策略对比

Deployment支持两种更新策略：

### 5.1 RollingUpdate（滚动更新，默认）

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 25%              # 默认
      maxUnavailable: 25%        # 默认
```

| 特性 | 说明 |
|------|------|
| 停机时间 | 零停机（前提是多个副本） |
| 回滚速度 | 慢（需要逐个替换回旧版本） |
| 资源消耗 | 高峰期可能双倍资源（maxSurge > 0） |
| 默认策略 | 适用于绝大多数无状态服务 |

### 5.2 Recreate（重建）

```yaml
spec:
  strategy:
    type: Recreate
```

```text
Recreate过程：
  Step 1: 删除所有旧Pod
  Step 2: 等待所有Pod终止
  Step 3: 创建所有新Pod

  缺点：更新期间服务不可用
  优点：不会同时运行新旧两个版本（避免数据库schema不兼容）
```

| 特性 | 说明 |
|------|------|
| 停机时间 | 有（所有Pod同时重建） |
| 资源消耗 | 低（不需要额外资源） |
| 适用场景 | 数据库迁移、状态ful应用、schema不兼容的更新 |

### 5.3 策略对比

| 维度 | RollingUpdate | Recreate |
|------|:------------:|:--------:|
| 零停机 | ✅ | ❌ |
| 双版本共存 | ✅（短暂） | ❌ |
| 资源消耗 | 高（可能双倍） | 低 |
| 回滚速度 | 慢 | 快（一次性重建） |
| 适用 | 大部分无状态服务 | 数据库schema变更、有状态服务 |
| 风险 | 新旧版本短暂共存 | 一段时间完全不可用 |

> ⚠️ **如果你的更新包含不兼容的数据库Migration（比如删除字段），使用RollingUpdate可能导致新旧版本同时操作数据库而报错。** 这种场景要么使用Recreate，要么把迁移和代码发布拆成两步。

---

## 6. 回滚操作

### 6.1 为什么需要回滚

```text
发布新版本 → 发现Bug → 需要快速回到旧版本

Deployment的回滚原理：
  旧版本的ReplicaSet一直没有删除（只是缩到0个Pod），
  回滚就是：把新RS缩到0，旧RS扩到期望数
```

### 6.2 回滚命令

```bash
# 回滚到上一个版本
kubectl rollout undo deployment/user-service

# 回滚到指定版本
kubectl rollout undo deployment/user-service --to-revision=2

# 查看版本历史
kubectl rollout history deployment/user-service
```

### 6.3 revisionHistoryLimit

```yaml
spec:
  revisionHistoryLimit: 10      # 保留最近10个版本（默认值）
                                 # 超过的版本会被清理
```

> 💡 **如果设置revisionHistoryLimit=0，则无法回滚！** 生产环境建议保留3~5个版本。

### 6.4 完整回滚示例

```bash
# 场景：user-service v1.1.0 上线后发现内存泄漏

# 1. 查看历史版本
kubectl rollout history deployment/user-service
# REVISION  CHANGE-CAUSE
# 1         Initial deployment v1.0.0
# 2         Update to v1.1.0 (memory opt)

# 2. 查看版本1的详细信息（确认版本1是稳定的）
kubectl rollout history deployment/user-service --revision=1

# 3. 回滚到版本1
kubectl rollout undo deployment/user-service --to-revision=1

# 4. 监控回滚状态
kubectl rollout status deployment/user-service

# 5. 验证回滚结果
kubectl get pods -l app=user-service -o wide
kubectl describe deployment user-service | grep Image
```

```text
回滚后的Revision历史：
  REVISION  CHANGE-CAUSE
  1         Initial deployment v1.0.0
  2         Update to v1.1.0 (memory opt)
  3         Rollback to v1.0.0        ← 回滚产生了一个新版本（实际内容同v1）

注意：回滚不会复用原来的Revision号，而是创建新Revision
这是因为K8s认为回滚也是一次变更，便于审计和追踪
```

---

## 7. 更新暂停与恢复与金丝雀发布

### 7.1 pause / resume

K8s支持在滚动更新过程中**暂停和恢复**，这是实现金丝雀发布的底层能力。

```bash
# 暂停更新（完成当前批次后停止）
kubectl rollout pause deployment/user-service

# 检查更新状态（部分Pod已更新）
kubectl rollout status deployment/user-service

# 验证新版本在部分Pod上的表现
kubectl get pods -l app=user-service -o wide

# 确认没问题后，恢复更新
kubectl rollout resume deployment/user-service
```

### 7.2 金丝雀发布实战

```text
金丝雀发布的本质：
  1. 新创建1个新版本Pod（通过Deployment的暂停功能）
  2. 让这个小比例实例接收真实流量
  3. 监控一段时间（观察错误率、延迟等）
  4. 没问题 → 继续更新剩余Pod
  5. 有问题 → 回滚

实现方式：
  方式A：使用rollout pause/resume（最简单）
  方式B：创建两个Deployment + Service标签切换（蓝绿部署变种）
  方式C：使用Service Mesh（如Istio的流量权重路由）
```

#### 方式A：使用rollout pause/resume

```bash
# Step 1: 发布新版本
kubectl set image deployment/user-service \
  user-service=registry.example.com/user-service:1.2.0

# Step 2: 立即暂停，只更新1个Pod（取决于maxSurge）
kubectl rollout pause deployment/user-service

# Step 3: 查看状态 — 部分Pod已更新，部分还是旧版本
kubectl get pods -l app=user-service
# NAME                            READY   STATUS    RESTARTS
# user-service-6a4b8c9d2-x1k2a   1/1     Running   0     ← 新版本(v1.2.0)
# user-service-7d9f8c9b6-a1b2c   1/1     Running   0     ← 旧版本(v1.0.0)
# user-service-7d9f8c9b6-d3e4f   1/1     Running   0     ← 旧版本(v1.0.0)

# Step 4: 监控新版本Pod的日志和指标
# kubectl logs -f user-service-6a4b8c9d2-x1k2a
# kubectl exec -it user-service-6a4b8c9d2-x1k2a -- curl localhost:8080/actuator/health

# Step 5: 确认正常 → 恢复更新
kubectl rollout resume deployment/user-service

# 或者：发现问题 → 回滚
kubectl rollout undo deployment/user-service
```

#### 方式B：两个Deployment + 标签切换（蓝绿变种）

```yaml
# blue-deployment.yaml — 旧版本（保持运行）
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service-blue
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
      version: blue
  template:
    metadata:
      labels:
        app: user-service
        version: blue
    spec:
      containers:
        - name: user-service
          image: user-service:1.0.0

---
# green-deployment.yaml — 新版本（先部署但不接流量）
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service-green
spec:
  replicas: 0                           # 初始为0
  selector:
    matchLabels:
      app: user-service
      version: green
  template:
    metadata:
      labels:
        app: user-service
        version: green
    spec:
      containers:
        - name: user-service
          image: user-service:1.1.0
```

```yaml
# service.yaml — Service选择哪一版就切到哪版
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  selector:
    app: user-service
    version: blue                       # 当前指向blue
  ports:
    - port: 80
      targetPort: 8080
```

```bash
# 金丝雀发布流程：
# 1. 部署green到1个副本，观察
kubectl scale deployment user-service-green --replicas=1

# 2. 验证没问题后，切换到green
kubectl patch service user-service -p '{"spec":{"selector":{"version":"green"}}}'

# 3. 如果green有问题
kubectl patch service user-service -p '{"spec":{"selector":{"version":"blue"}}}'  # 切回blue

# 4. 清理blue
kubectl scale deployment user-service-blue --replicas=0
```

---

## 8. 部署策略对比表

```text
部署策略全景：
  蓝绿部署     → 切换Service标签，秒级切换和回滚
  滚动更新     → 逐个替换Pod，零停机但回滚慢
  金丝雀发布   → 先让部分流量到新版，逐步放量
  A/B测试     → 基于用户特征的流量分发（不同用户看到不同版本）
```

| 策略 | 方式 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|---------|
| **滚动更新** | 逐个替换Pod | 零停机、资源消耗可控 | 回滚慢（需逐个替换回来） | 大部分场景 |
| **蓝绿部署** | 切换Service指向 | 秒级回滚、快速切换 | 双倍资源成本 | 核心服务、金融系统 |
| **金丝雀发布** | 小部分流量到新版 | 风险最低、可真实验证 | 需要Ingress/Service Mesh配合 | 重要版本发布 |
| **A/B测试** | 按用户特征分流 | 可验证业务效果 | 配置复杂 | 功能试用、版本对比 |
| **Recreate** | 一次性重建 | 无新老共存问题 | 有停机时间 | 数据库迁移、有状态服务 |

> 💡 **策略选择原则**：资源充足+需要极速回滚 → 蓝绿部署；要安全验证新版本 → 金丝雀发布；最常用+最省事 → 滚动更新。

---

## 9. K8s工作负载对比

K8s除了Deployment之外，还有其他几种Workload资源，适用于不同的场景。

### 9.1 五种工作负载总览

| 资源 | 缩写 | 用途 | 有状态？ | 副本管理 | 更新策略 |
|------|:----:|------|:--------:|---------|---------|
| **Deployment** | deploy | 无状态应用 | ❌ | ✅ ReplicaSet | RollingUpdate/Recreate |
| **StatefulSet** | sts | 有状态应用 | ✅ | ✅ 有状态副本 | RollingUpdate/OnDelete |
| **DaemonSet** | ds | 每个Node一个Pod | ❌ | ❌ 按Node数 | RollingUpdate/OnDelete |
| **Job** | job | 一次性任务 | ❌ | ❌ | ❌ 运行完退出 |
| **CronJob** | cj | 定时任务 | ❌ | ❌ 按Cron调度 | ❌ 每次新建 |

### 9.2 详细对比

| 维度 | Deployment | StatefulSet | DaemonSet | Job | CronJob |
|------|:----------:|:-----------:|:---------:|:---:|:-------:|
| **Pod名称** | 随机后缀 | 有序编号（pod-0, pod-1） | 随机后缀 | 随机后缀 | 随机后缀 |
| **存储** | 共享PV | 独立PVC（每个Pod一个） | 共享/hostPath | 临时 | 临时 |
| **网络** | 随机IP | 稳定DNS（pod-0.svc） | 每个Node一个 | 随机IP | 随机IP |
| **伸缩** | ✅ 任意 | ✅ 但需谨慎 | ❌ 按Node | ❌ | ❌ |
| **典型场景** | Web服务、API | 数据库、消息队列、ZooKeeper | 日志收集、监控Agent | 数据迁移 | 定时备份 |

### 9.3 StatefulSet 特点详解

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql
spec:
  serviceName: mysql-h      # 必须关联Headless Service
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
          volumeMounts:
            - name: data
              mountPath: /var/lib/mysql
  volumeClaimTemplates:      # StatefulSet特有的：自动创建PVC
    - metadata:
        name: data
      spec:
        accessModes: [ "ReadWriteOnce" ]
        resources:
          requests:
            storage: 10Gi
```

```text
StatefulSet vs Deployment 关键差异：

1. Pod名称：
   Deployment:   user-service-7d9f8c9b6-xk4f3  (随机)
   StatefulSet:  mysql-0, mysql-1, mysql-2       (有序)

2. Pod创建和删除顺序：
   Deployment:   并行创建/删除（无顺序）
   StatefulSet:  顺序创建（0→1→2），逆序删除（2→1→0）

3. 存储：
   Deployment:   所有Pod共享同一PV（或独立）
   StatefulSet:  Pod-0→PVC-0, Pod-1→PVC-1（每Pod独立磁盘）

4. DNS：
   Deployment:   通过Service统一访问
   StatefulSet:  mysql-0.mysql-h.svc.cluster.local（每Pod独立DNS）
```

### 9.4 DaemonSet 特点

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluentd
spec:
  selector:
    matchLabels:
      name: fluentd
  template:
    metadata:
      labels:
        name: fluentd
    spec:
      tolerations:
        - operator: Exists     # 容忍所有污点，确保在所有Node上运行
      containers:
        - name: fluentd
          image: fluent/fluentd:v1.16
```

| DaemonSet场景 | 工具 |
|--------------|------|
| 日志收集 | fluentd, filebeat, logstash |
| 监控采集 | node-exporter, datadog-agent |
| 网络插件 | Calico, Cilium, Flannel |
| 安全代理 | falco, kube-bench |

---

## 10. 完整SpringBoot Deployment YAML

```yaml
# 生产级SpringBoot应用部署
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: production
  labels:
    app: user-service
    tier: backend
    managed-by: deployment
  annotations:
    kubernetes.io/change-cause: "Initial deployment v1.0.0"
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"

spec:
  # ===== 副本管理 =====
  replicas: 3
  revisionHistoryLimit: 5                 # 保留5个历史版本用于回滚

  # ===== 更新策略 =====
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1                         # 保守更新，每次多1个
      maxUnavailable: 0                   # 保证所有Pod可用（零停机）

  # ===== 选择器 =====
  selector:
    matchLabels:
      app: user-service

  # ===== Pod模板 =====
  template:
    metadata:
      labels:
        app: user-service
        version: "1.0.0"
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"

    spec:
      # 调度策略
      serviceAccountName: user-service-sa

      affinity:
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
                topologyKey: kubernetes.io/hostname    # 尽量分布在不同Node

      # 优雅关闭
      terminationGracePeriodSeconds: 60

      # 容器
      containers:
        - name: user-service
          image: registry.example.com/user-service:1.0.0
          imagePullPolicy: IfNotPresent

          # 端口
          ports:
            - name: http
              containerPort: 8080
              protocol: TCP
            - name: actuator
              containerPort: 8081
              protocol: TCP

          # 环境变量
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "k8s"
            - name: JAVA_OPTS
              value: >-
                -XX:+UseContainerSupport
                -XX:MaxRAMPercentage=75.0
                -XX:InitialRAMPercentage=50.0
                -XX:+HeapDumpOnOutOfMemoryError
                -XX:+UseG1GC
                -Djava.security.egd=file:/dev/./urandom
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

          # 资源限制
          resources:
            requests:
              cpu: "500m"
              memory: "1Gi"
            limits:
              cpu: "1000m"
              memory: "1.5Gi"

          # 探针
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 15
            periodSeconds: 5
            timeoutSeconds: 3
            successThreshold: 1
            failureThreshold: 2

          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 15
            timeoutSeconds: 5
            failureThreshold: 3

          # 优雅关闭
          lifecycle:
            preStop:
              exec:
                command:
                  - sh
                  - -c
                  - |
                    echo "PreStop hook started..."
                    # 可选：从注册中心下架
                    sleep 5
                    echo "Sending SIGTERM to Java process..."
                    kill -SIGTERM 1
                    # Spring Boot的优雅关闭会处理剩余请求

          # 挂载
          volumeMounts:
            - name: config
              mountPath: /app/config
              readOnly: true
            - name: tmp
              mountPath: /tmp
            - name: logs
              mountPath: /var/log/app

      # 卷
      volumes:
        - name: config
          configMap:
            name: app-config
            items:
              - key: application.yml
                path: application.yml
        - name: tmp
          emptyDir: {}
        - name: logs
          emptyDir: {}

---
# Headless Service（用于Pod DNS）
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
    - name: http
      port: 8080
```

> 🎯 **Deployment是K8s中最常用的Workload，但它不是万能的**。理解Deployment和StatefulSet/DaemonSet/Job的差异，选择正确的工作负载类型，才能设计出高可用的生产系统。下一章[04-Service与网络](04-Service与网络.md)将介绍如何让外界访问这些Pod。
