# 09 Kubernetes 部署与云原生

> Level3 的部署升级：从 Docker Compose 的「编排一套应用」到 Kubernetes 的「编排一套集群」。本模块用 K8s 1.36（2026-08 当前稳定版）在 minikube/k3s 上把 Level2 的 Compose 全栈迁移成「声明式清单 + 自愈 + 弹性伸缩」，并理清「什么时候该从 Compose 走向 K8s」。

## 📚 目录

1. [动机：Compose 的边界与 K8s 的答案](#1-动机compose-的边界与-k8s-的答案)
2. [核心对象：Pod / Deployment / Service](#2-核心对象pod--deployment--service)
3. [配置与密钥：ConfigMap / Secret](#3-配置与密钥configmap--secret)
4. [从 Compose 迁移到 K8s](#4-从-compose-迁移到-k8s)
5. [自愈、滚动更新与弹性伸缩](#5-自愈滚动更新与弹性伸缩)
6. [验证实验](#6-验证实验)
7. [核心要点](#7-核心要点)

---

## 1. 动机：Compose 的边界与 K8s 的答案

Level2 09 篇用 Compose 解决了「一键部署」——单机场景它够用且优秀。但微服务化后三个需求超出 Compose 的能力：**自愈**——kb-doc 实例挂了，Compose 不会自动拉起（`restart: always` 只处理「进程退出」，不处理「进程假死、机器宕机、容器被误删」）；**扩缩容**——Compose 手动 `--scale` 在单机上扩，没有「按 CPU 自动扩」的能力；**多机**——Compose 绑定单机 Docker，微服务的实例要分布到多台机器。K8s 的答案：**声明式期望状态 + 控制器不断逼近**——你声明「kb-doc 要 3 个副本」，K8s 的控制器保证「时刻有 3 个健康副本在跑」：挂了拉起、超载扩容、发布滚动更新。**Compose vs K8s 不是替代关系而是演进关系**：单机演示用 Compose（快），生产与多机用 K8s（强）——「先 Compose 后 K8s」的迁移路径本身就是项目演进的故事线。

## 2. 核心对象：Pod / Deployment / Service

K8s 三个核心对象先建心智：**Pod**——最小调度单元，一个或多个容器共享网络与存储（一个应用实例 ≈ 一个 Pod，容器是进程、Pod 是实例）；**Deployment**——声明式控制器：声明副本数与镜像版本，负责「期望状态 → 实际状态」的收敛（滚动更新、回滚都在这层）；**Service**——稳定的访问入口：Pod 的 IP 会变（重启就换），Service 提供固定的虚拟 IP + 服务名 DNS，**Pod 的故障对调用方无感**——这对应 Nacos 注册中心的「服务发现」逻辑（K8s 内部的服务发现由 Service 承担，Nacos 仍管服务注册与配置，两者分工：**K8s 管「实例在哪」，Nacos 管「业务服务关系」**，这个分工要能讲清）。

kb-doc 的最小清单（Deployment + Service）：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata: { name: kb-doc }
spec:
  replicas: 3
  selector: { matchLabels: { app: kb-doc } }
  template:
    metadata: { labels: { app: kb-doc } }
    spec:
      containers:
        - name: kb-doc
          image: suxiangyu/kb-doc:20260811-abc123
          ports: [{ containerPort: 8080 }]
          resources: { limits: { memory: 512Mi, cpu: "500m" } }
---
apiVersion: v1
kind: Service
metadata: { name: kb-doc }
spec:
  selector: { app: kb-doc }
  ports: [{ port: 8080 }]
```

三个实践要点：**镜像必须推送到仓库**（K8s 从镜像仓库拉取，本地镜像在单节点集群可用但多节点必须仓库，Level2 09 篇的镜像 tag 纪律直接沿用）；**资源限制必写**（`resources.limits`——没有限制的容器会吃光节点内存，这是 K8s 生产事故第一名；`requests` 与 `limits` 分开写：requests 是调度依据、limits 是运行上限）；**探针复用 Level2 08 篇的 Actuator 健康检查**（`readinessProbe` 指向 `/actuator/health`，就绪才接流量；`livenessProbe` 探存活，挂死自动重启——两个探针一「接流量」一「保存活」，职责不同都要配）。

## 3. 配置与密钥：ConfigMap / Secret

配置外置（Level2 09 篇原则）在 K8s 的两个对象里落地：**ConfigMap**——非敏感配置（数据源地址、限流阈值）以键值对或文件形式挂载进 Pod（环境变量或挂载文件）；**Secret**——敏感配置（API Key、密码）Base64 编码存储、专用对象类型。与 Nacos 的分工：**ConfigMap 管「部署层配置」（镜像内应用需要的基础配置），Nacos 管「业务运行配置」（动态可刷新的业务参数）**——Spring 应用里 `spring.config.import` 的 Nacos 地址由环境变量注入（ConfigMap 提供），业务配置仍走 Nacos 动态刷新。迁移时的坑：**Compose 的 `.env` 与 environment 全部翻译成 ConfigMap/Secret**——漏掉任何一个环境变量，应用启动即失败，「配置迁移清单」是 Compose → K8s 迁移的第一张表（先把原 .env 逐行列出来，再对照 ConfigMap/Secret 逐项翻译，逐行打勾，不要凭记忆迁移）。

## 4. 从 Compose 迁移到 K8s

迁移路径四步（复用 Level2 09 篇的资产）：**第一步：镜像仓库化**——Compose 阶段构建的本地镜像推到 Docker Hub（tag 纪律沿用）；**第二步：中间件上 K8s**——MySQL/Redis/RabbitMQ/Nacos 用有状态部署（StatefulSet）或官方 Helm Chart（中间件的持久化用 PV/PVC——**中间件是迁移的重灾区：数据卷映射、网络、存储类**，演示环境可以用 Helm 简化，生产用云托管）。

中间件迁移的三个具体坑：**数据卷要显式声明**——StatefulSet 的每个副本一个 PVC，Compose 的 named volume 不会自动平移，忘了声明 = 数据写进容器层、Pod 删除数据全丢；**网络策略**——K8s 里服务间访问用 Service DNS 名（`mysql.default.svc`），应用配置里的地址要跟着改（与 Level2 09 篇「容器内外地址不同」是同一个坑的放大版）；**Nacos 服务端上 K8s**——它自己也要注册与存储（外置 MySQL），先迁 Nacos 再迁应用，应用的 Nacos 地址由 ConfigMap 注入，迁移时只改一个地方。**第三步：应用上 K8s**——按「Deployment + Service + ConfigMap + Secret」四件套翻译每个服务，探针、资源限制、就绪检查配齐；**第四步：入口接网关**——kb-gateway 暴露为 NodePort 或 Ingress，外部流量统一走网关（外部访问地址不再直连服务）。迁移顺序沿用 03 篇绞杀者思维：**中间件先动（应用行为不变），应用逐个迁**，每步验证全链路。

## 5. 自愈、滚动更新与弹性伸缩

K8s 的三个「云原生能力」是本模块的演示重点：**自愈演示**——`kubectl delete pod kb-doc-xxx`，观察 Deployment 控制器秒级拉起新 Pod（`kubectl get pods` 看状态流转），服务不中断（Service 摘除故障 Pod）；**滚动更新**——更新 Deployment 镜像版本（`kubectl set image`），观察「新 Pod 先起、就绪后旧 Pod 才停」的滚动过程（不中断服务），配 `maxSurge/maxUnavailable` 控制节奏，失败自动回滚（`kubectl rollout undo`）——**滚动更新 = Level2 09 篇「镜像 tag 换版本重启」的云原生升级**；**弹性伸缩（HPA）**——`HorizontalPodAutoscaler` 按 CPU 使用率自动扩缩副本（如 CPU > 60% 扩到 5 副本，空闲缩回 2）——**「按负载自动扩缩」是 Compose 给不了的能力**，配合 01 篇的压测数据演示：压测时观察副本自动扩容。HPA 的调参要点：**缩容要有冷却时间**（`scale-down` 稳定窗口默认 5 分钟，防止抖动导致副本反复增减）、**扩缩的指标阈值要与压测基线对齐**（阈值低于实际需求 = 永远扩容，高于 = 永远不扩），这两个参数是「HPA 是否真的可用」的分水岭。三个能力的演示数据（滚动更新零中断、HPA 扩容响应时间）进 perf.md 与面试素材。

## 6. 验证实验

四个验收实验：**迁移实验**——按四步完成全栈迁移，全链路（上传 → 解析 → 问答）在 K8s 上跑通；**自愈实验**——删一个 kb-doc Pod，10 秒内新 Pod 就绪、期间调用无失败（Service 摘除生效）；**滚动更新实验**——更新镜像版本，观察零中断滚动（压测并行验证不丢请求），失败场景回滚成功；**伸缩实验**——压测触发 HPA 扩容，观察副本数变化与恢复。四个实验 + 迁移检查单，就是「云原生部署」的完整验收——**「我在 K8s 上演示过自愈、滚动更新与 HPA」是简历项目模块的云原生亮点**。

实验环境与排障命令：本地用 minikube 或 k3s（单机即可跑通全部实验），K8s 1.36 是 2026-08 当前稳定版（1.34 已近 EOL 勿选）。排障三连写进 README 故障速查：`kubectl get pods`（哪个 Pod 没就绪、`CrashLoopBackOff` 是最常见状态）→ `kubectl logs <pod> -f`（应用日志配 grep traceId）→ `kubectl describe pod <pod>`（**Events 段是定位「拉镜像失败/探针不过/资源不足」的第一现场**，比翻日志更快）。

## 7. 核心要点

1. Compose 的边界：不自愈、不扩缩容、单机——K8s 的答案是「声明式期望状态 + 控制器收敛」。
2. 三对象心智：Pod（实例）、Deployment（期望状态）、Service（稳定入口）——Pod 故障对调用无感。
3. 配置迁移：Compose 的 environment/.env 全部翻译成 ConfigMap/Secret，迁移清单是第一张表。
4. 迁移四步：镜像仓库化 → 中间件（StatefulSet/Helm，数据卷是重灾区）→ 应用四件套 → 网关入口。
5. 三大能力演示：自愈（秒级拉起）、滚动更新（零中断 + 回滚）、HPA（按负载扩缩）——数据进面试素材。
6. 分工讲清：K8s 管「实例在哪」，Nacos 管「业务服务关系与配置」。

> 🎯 **核心要点**：K8s 的本质是**「把运维动作（拉起、更新、扩缩、恢复）从『人操作』变成『声明式期望状态的自动收敛』」**——你只管说「要什么」，控制器负责「怎么实现」。Compose 是「编排」，K8s 是「自愈」——这个差距就是「部署现代化」的距离。

---

**下一模块**：[10 进阶深挖与面试冲刺](./10-进阶深挖与面试冲刺.md) | **返回总览**：[Level3 总览](./00-Level3%20进阶加分%20总览.md)
