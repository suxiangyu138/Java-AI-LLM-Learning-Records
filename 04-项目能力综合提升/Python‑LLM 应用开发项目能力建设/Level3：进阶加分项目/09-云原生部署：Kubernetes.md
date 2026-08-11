# 09 云原生部署：Kubernetes

> 第九个加分项：把 Docker Compose 的单机部署升级为 Kubernetes 云原生——Pod/Deployment/Service 核心概念、Helm 打包、水平伸缩、灰度发布、GitOps 流程。K8s 是后端岗位的"默认技能线"，与仓库「Docker与Kubernetes衔接」「CI CD灰度发布」体系联动。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [为什么上 K8s：Compose 的边界](#2-为什么上-k8scompose-的边界)
3. [核心概念：Pod、Deployment、Service](#3-核心概念poddeploymentservice)
4. [配置与密钥：ConfigMap 与 Secret](#4-配置与密钥configmap-与-secret)
5. [伸缩与自愈：HPA 与探针](#5-伸缩与自愈hpa-与探针)
6. [Helm 打包与发布流程](#6-helm-打包与发布流程)
7. [常见坑](#7-常见坑)

---

## 1. 目标与验收

本模块的产出：项目 K8s 化——Deployment/Service/ConfigMap/Secret 清单、HPA 自动伸缩、Helm chart（可一键 `helm install`）、灰度发布流程（滚动/金丝雀）。验收标准：**集群里 `kubectl get all` 看到服务健康运行**；**压测触发 HPA 自动扩容**（04 篇压测与 K8s 伸缩联动）；**一次发布走完滚动更新**（无中断）。**环境说明**：本地用 Minikube 或 kind（单机 K8s 集群），正式集群概念一致——**"本地单机 K8s 跑通 + 概念可迁移到生产集群"是本模块的合理目标**，不要求真集群（预算与资源限制是现实约束，写进决策记录）。版本基线（2026-08）：Kubernetes 1.3x（2026 稳定线，以官方发布为准）、Helm 4.x、Minikube/kind 本地环境。

## 2. 为什么上 K8s：Compose 的边界

Docker Compose 解决"单机多容器编排"，K8s 解决"集群规模的基础设施"——**边界在哪**：**Compose 够用的场景**（单机、容器 < 10、无伸缩需求——Level2 09 的选择在当时的规模下完全正确）；**K8s 的触发信号**——**多副本**（API 服务要跑 3 个副本，自动伸缩（04 篇压测发现单机 worker 有上限））；**自愈**（节点/容器挂了自动拉起——Compose 的 restart 只是单机重启，K8s 是调度级自愈）；**滚动发布**（金丝雀/蓝绿，发布不中断——「CI CD灰度发布」体系的落地形态）；**多机**（机器故障不宕服务）。**上 K8s 的代价**（决策的另一半）：运维复杂度陡增（集群本身要维护——K3s/Minikube 单机跑也要学调度概念）、资源开销（集群组件吃内存——本地开发机吃紧）。**本模块的决策叙事**："项目从单机走向多副本与滚动发布时，Compose 的边界到了，K8s 是规模化的基础设施——本地用 kind 验证，生产集群概念一致"——**又是"触发信号 + 成本权衡"的标准选型结构**。

## 3. 核心概念：Pod、Deployment、Service

K8s 的三个核心概念（本项目的最小清单，仓库「Docker与Kubernetes衔接」有全景）：**Pod**（最小调度单元——一个或多个容器，共享网络与存储，本项目的 api 与 client 各一个 Pod 或合一个（api 独立 Pod，client 独立 Pod——独立伸缩）；**Deployment**（声明式管理 Pod 副本——"我要 3 个副本"，K8s 保证始终 3 个：滚动更新、回滚、伸缩都在 Deployment 层）；**Service**（稳定的访问入口——Pod 是临时的（重建 IP 变），Service 提供稳定 DNS（api-service:8000 内部访问、NodePort/LoadBalancer 对外暴露））：

```yaml
# api-deployment.yaml（核心清单，client/redis 等类似）
apiVersion: apps/v1
kind: Deployment
metadata: { name: kbqa-api }
spec:
  replicas: 3                                  # 三副本
  selector: { matchLabels: { app: kbqa-api } }
  template:
    metadata: { labels: { app: kbqa-api } }
    spec:
      containers:
        - name: api
          image: registry/kbqa:1.4.2           # 镜像 tag 绑定版本（Level2 09）
          ports: [{ containerPort: 8000 }]
          envFrom: [{ secretRef: { name: kbqa-secrets } }]   # 密钥不进镜像
          resources: { requests: { cpu: "500m" }, limits: { cpu: "1" } }  # 资源声明
```

三个要点：**replicas 是伸缩的声明式起点**（手改或 HPA 自动改——第 5 节）；**镜像 tag 不可变**（`latest` 是反模式——K8s 的镜像拉取策略与 Level2 09 的 tag 绑定延续）；**resources 必写**（不写资源声明的容器是调度灾难——K8s 不知道它能占多少，写 requests/limits 是生产纪律）。

**Service 与 Ingress 的边界**（对外暴露的两种形态）：**Service** 是集群内稳定的访问入口（NodePort 对外临时调试、LoadBalancer 云环境对外）；**Ingress** 是七层路由（域名 + 路径 → Service——`kbqa.example.com/api → kbqa-api-service`，生产环境的标准对外形态，TLS 终止也在 Ingress 层做——**与 Level2 09 的 Nginx 反代角色对应**，K8s 里 Ingress Controller（Nginx Ingress/云厂商 LB）就是那个"反代"）。本项目的对外形态：**本地验证**用 NodePort 或 port-forward（`kubectl port-forward svc/kbqa-api 8000:8000`——调试期的标准姿势）；**生产形态**写清楚 Ingress 清单（域名、TLS、路径路由）——本地不部署 Ingress，概念与清单要完整。

## 4. 配置与密钥：ConfigMap 与 Secret

Level2 09 的"环境变量注入"在 K8s 下的正式形态：**ConfigMap**（非敏感配置——模型名、日志级别、限流阈值）与 **Secret**（敏感配置——API Key、JWT Secret、数据库密码，base64 存储 + RBAC 控制，2026 年生产用外部 Secret 管理（Vault/云厂商 KMS）做加密封存，Demo 级 Secret 对象够用）：

```yaml
apiVersion: v1
kind: Secret
metadata: { name: kbqa-secrets }
type: Opaque
stringData:                                   # stringData 明文写，apply 后自动 base64
  DEEPSEEK_API_KEY: "sk-xxx"
  JWT_SECRET: "change-me"

---
apiVersion: v1
kind: ConfigMap
metadata: { name: kbqa-config }
data:
  MODEL_NAME: "deepseek-v4-flash"
  LOG_LEVEL: "INFO"
```

配置管理的三条纪律（延续 Level2 02）：**ConfigMap 与 Secret 分离**（敏感与否的边界清晰——误把密钥写进 ConfigMap 等于公开）；**配置版本化**（改 ConfigMap 会触发 Pod 滚动（依赖版本号注解）——配置变更与代码变更一样走发布流程）；**Secret 轮换**（API Key 换新——更新 Secret → 滚动重启 Pod，Level1 01 的密钥预案在 K8s 下的自动化形态）。

## 5. 伸缩与自愈：HPA 与探针

K8s 的伸缩与自愈是"云原生"二字的实体：**HPA（水平自动伸缩）**——按 CPU/内存/自定义指标自动调 replicas：04 篇压测出容量数据（100 QPS 时 CPU 70%）→ HPA 配置 `min: 2, max: 10, targetCPUUtilizationPercentage: 70`——压测时观察副本自动从 2 → N；**探针三件套**（仓库「Docker与Kubernetes衔接」有详解）：**liveness**（容器活吗——挂了重启）、**readiness**（能接流量吗——未就绪不进 Service 负载均衡——**启动预热（Level2 03 的 lifespan）与 readiness 配合**：模型加载期间 readiness 返回 503，就绪后才进流量）、**startup**（慢启动容器保护——模型加载 60 秒场景必须配 startup，否则 liveness 误杀）。**自愈的验证**（验收实验）：`kubectl delete pod <api-pod>`——Deployment 自动重建新 Pod，期间 Service 流量无中断（readiness 保证）——**这个演示是"云原生自愈"最直观的证据**。

```yaml
# HPA：自动伸缩
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata: { name: kbqa-api-hpa }
spec:
  scaleTargetRef: { apiVersion: apps/v1, kind: Deployment, name: kbqa-api }
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource: { name: cpu, target: { type: Utilization, averageUtilization: 70 } }
```

## 6. Helm 打包与发布流程

K8s 清单散落文件的管理痛点（10 个清单文件手工 apply——环境差异无法管理），**Helm 解决"清单的模板化与版本化"**：**Chart 结构**（Chart.yaml + values.yaml（环境差异参数——dev/prod 的副本数、镜像 tag、域名）+ templates/（清单模板——`{{ .Values.replicas }}` 引用）；**一键安装**（`helm install kbqa ./chart`，环境差异用 `--values prod.yaml`）；**发布升级**（`helm upgrade kbqa ./chart --set image.tag=1.5.0`——**回滚一条命令**：`helm rollback kbqa 1`——Level2 09 的回滚流程在 Helm 下更简单）。**发布策略（2026 标准）**：**滚动更新**（Deployment 默认——maxSurge/maxUnavailable 控制节奏，发布无中断）；**金丝雀**（新版本 10% 流量验证后全量——Service 与多 Deployment 的权重路由（或 Istio/Argo Rollouts 做精细化——仓库「CI CD灰度发布」体系有全景，本项目滚动 + 手动金丝雀够用）。**GitOps 方向**（预告）：ArgoCD 把"集群状态"变成"Git 仓库的声明"——CI 构建镜像 → Git 改 tag → ArgoCD 自动同步——**"发布即 Git 提交"的完整闭环在「CI CD灰度发布」体系里是另一堂课**，本模块先走通 Helm 手动发布。

## 7. 常见坑

**镜像拉取策略**：`latest` tag 不更新——always/IfNotPresent 策略 + 唯一 tag 绑定（Level2 09 的纪律在 K8s 强制）。

**readiness 没配启动预热**：模型加载 60 秒，期间 liveness 误判重启循环——startupProbe 保护慢启动，readiness 管流量（第 5 节三探针）。

**资源声明缺失**：Pod 无 requests/limits——调度随意 + 节点 OOM 风险，resources 必写。

**本地起真集群**：笔记本跑完整 K8s 组件卡死——Minikube/kind 单机即可，概念与真集群一致。

**Secret 明文进 Git**：Secret YAML 提交仓库——密钥库外管理（Vault/KMS）或至少 .gitignore + 示例模板，Level2 09 的 .dockerignore 教训的 K8s 版。

**HPA 指标没数据**：metrics-server 没装——HPA 需要集群内指标源，Minikube 默认不装，`minikube addons enable metrics-server`。

**日志在 Pod 里看不到**：容器日志随 Pod 消失——集中日志（EFK/Loki——07 篇全家桶的日志侧）或至少 `kubectl logs -f` 排查时意识"这是临时视角"。

**本地集群与生产集群的认知差**：Minikube 单节点练会了"操作"，生产集群还要懂"多节点调度、持久卷（PV/PVC——数据卷在 K8s 的正式形态，本地演示级用 hostPath 即可）、网络插件（CNI）"——**本模块完成后，把"本地练会的操作"与"生产集群才有的概念"分清单列出**，写进 docs/ 的 K8s 笔记：操作是地基，概念清单是生产迁移的路线图——面试被问"K8s 生产要注意什么"时，这份清单就是答案。

**过度追求生产级**：本地 Demo 也堆 PV/PVC/多节点/Ingress——本地验证只做"概念闭环"（Deployment/Service/HPA/Helm），生产级配置写好清单即可，资源与时间花在概念理解上而不是复刻生产环境。

> 🎯 **核心要点**：K8s 化的本质是**"把部署从'跑起来'升级为'声明式运维'"**——Deployment 声明期望状态、Service 提供稳定入口、HPA 自动伸缩、Helm 管理发布、探针保障自愈。清单写对的那一刻，系统从"我手动维护"变成"集群按声明自治"——这是"云原生"在简历上的全部含义。

---

**下一模块**：[10 进阶验收与面试冲刺](./10-进阶验收与面试冲刺.md) | **返回总览**：[Level3 总览](./00-Level3%20进阶加分项目%20总览.md)

【参考来源】
- [Kubernetes 官方文档](https://kubernetes.io/docs/)
- [Helm 官方文档](https://helm.sh/docs/)
- [Docker与Kubernetes衔接 体系（本仓库）](https://github.com/suxiangyu138/Java-AI-LLM-Learning-Records)
- [CI CD灰度发布 体系（本仓库）](https://github.com/suxiangyu138/Java-AI-LLM-Learning-Records)
