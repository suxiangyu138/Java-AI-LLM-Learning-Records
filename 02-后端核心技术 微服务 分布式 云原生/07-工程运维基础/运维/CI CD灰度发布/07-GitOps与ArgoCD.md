# 07-GitOps 与 ArgoCD
> Git 为唯一事实来源：GitOps 理念、ArgoCD 架构与同步机制、应用管理、与 Flux 对比——"部署也能走 Code Review"

## 📚 目录
1. [GitOps 理念](#1-gitops-理念)
2. [ArgoCD 架构](#2-argocd-架构)
3. [核心概念：Application 与 Sync](#3-核心概念application-与-sync)
4. [部署工作流（CI → GitOps）](#4-部署工作流ci--gitops)
5. [App of Apps 模式](#5-app-of-apps-模式)
6. [与 Flux 对比](#6-与-flux-对比)
7. [回滚与灾难恢复](#7-回滚与灾难恢复)
8. [高频避坑](#8-高频避坑)
9. [核心要点](#9-核心要点)
10. [参考来源](#10-参考来源)

## 1. GitOps 理念

```text
GitOps 核心：Git 仓库是声明式系统的唯一事实来源（Single Source of Truth）

传统模式：kubectl apply 手动执行（人操作集群，状态不可控）
GitOps 模式：Git 中声明期望状态 → 控制器自动同步到集群

三大原则：
  ① 声明式：期望状态写在 Git（YAML）
  ② 版本化：每次变更走 Git（Code Review）
  ③ 自动同步：控制器持续对比 Git 与集群，收敛到期望状态
```

> 🎯 **GitOps 的本质价值**：**部署也走 Code Review**——改配置和改代码一样有审查、有历史、可回滚。集群漂移（手改的）会被控制器"纠正"回来。

## 2. ArgoCD 架构

```text
开发者提交 Git（应用清单）
  → ArgoCD（控制器）监听 Git 变化
  → 对比 Git 期望状态 vs 集群实际状态
  → 差异 → 同步（Sync）到集群
  → 持续监控（漂移检测）

组件：
  API Server（界面/API）
  Repo Server（读取 Git 仓库）
  Application Controller（对比与同步）
  Redis（缓存/操作状态）
```

```bash
# 安装（K8s 命名空间 argocd）
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 登录
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

## 3. 核心概念：Application 与 Sync

### 3.1 Application（应用声明）

```yaml
# application.yaml（Git 仓库中的声明）
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: order-service
  namespace: argocd
spec:
  project: production          # 项目（权限隔离）
  source:
    repoURL: https://git.example.com/deploy/order-service.git
    targetRevision: main       # Git 分支/Tag
    path: overlays/production  # 清单目录（Kustomize）
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  syncPolicy:
    automated:
      prune: true              # 删除 Git 中已移除的资源
      selfHeal: true           # 漂移自动纠正（回滚手改）
    syncOptions:
      - CreateNamespace=true
```

| 字段 | 说明 |
|------|------|
| `source` | Git 仓库 + 分支 + 路径 |
| `destination` | 目标集群 + 命名空间 |
| `syncPolicy.automated` | 自动同步（prune/selfHeal） |
| `syncPolicy` 手动 | 发布按钮模式（人工确认同步） |

### 3.2 Sync 机制

```text
Sync 流程：
  ① 对比（Diff）：Git 期望状态 vs 集群实际
  ② 显示差异（OutOfSync）
  ③ 执行同步（Auto/Manual）
  ④ 验证（Synced/Healthy）
```

| 状态 | 含义 |
|------|------|
| Synced | Git 与集群一致 |
| OutOfSync | 有差异（Git 变更或集群漂移） |
| Healthy | 应用运行健康（结合健康检查） |
| Progressing | 同步/发布进行中 |

> ⚠️ **自动化策略选择**：`selfHeal: true` 会纠正人工 `kubectl` 修改（漂移防御）；**生产环境建议先手动同步 + 审批**，成熟后再自动化——自动化是目标，不是起点。

## 4. 部署工作流（CI → GitOps）

```text
CI 与 GitOps 的分工：
  CI（构建侧）：代码 → 测试 → 镜像（产物）
  GitOps（部署侧）：镜像 tag 变更 → Git 提交 → ArgoCD 同步

关键：CI 不直接操作集群！CI 只更新 Git 中的镜像版本
```

```yaml
# CI 更新部署清单中的镜像 tag（gitops 仓库）
# 例：overlays/production/kustomization.yaml
images:
  - name: harbor.example.com/prod/order-service
    newTag: 1.0.4        # CI 用 sed/工具更新后提交
```

```bash
# CI 流水线最后一步（更新 Git 并提交）
git clone https://git.example.com/deploy/order-service.git
cd order-service
sed -i "s|newTag: .*|newTag: ${NEW_VERSION}|" overlays/production/kustomization.yaml
git commit -m "release: order-service ${NEW_VERSION}"
git push
# ArgoCD 检测到 Git 变更 → 自动同步部署
```

> 🎯 **"CI 不碰集群"是 GitOps 的安全边界**：CI 只改 Git，集群状态完全由 ArgoCD 管理——**权限最小化（CI 无集群权限）+ 变更全走 Git 审计**。

## 5. App of Apps 模式

```text
多应用管理的模式：一个"根应用"管理所有子应用
```

```yaml
# root-app.yaml（根应用）
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: all-apps
  namespace: argocd
spec:
  source:
    repoURL: https://git.example.com/deploy/apps.git
    path: apps            # 每个子目录一个 Application 声明
  destination:
    server: https://kubernetes.default.svc
  project: default
```

```text
apps/
├── order-service/
│   └── application.yaml      # 子应用声明
├── payment-service/
│   └── application.yaml
└── common/
    └── application.yaml      # 公共组件（监控/ingress）
```

| 优势 | 说明 |
|------|------|
| 单一入口 | 根应用管理全部 |
| 团队自治 | 每个服务自己的 Application |
| 权限分层 | 按项目/应用分权 |

## 6. 与 Flux 对比

| 维度 | ArgoCD | Flux |
|------|--------|------|
| 定位 | GitOps 主流（UI/功能全） | GitOps 轻量（控制器优先） |
| 界面 | 完整 Web UI | CLI/CRD（UI 弱） |
| 渐进交付 | 集成 Argo Rollouts | Kustomize 原生 + 部分支持 |
| 多集群 | ✅ | ✅ |
| 学习曲线 | 中 | 中 |
| 生态 | Argo 家族（Rollouts/Workflow/Events） | Flux 家族（Helm/Kustomize/Image） |
| 适用 | **需要 UI/团队协作** | 极简/与 Kustomize 深度结合 |

> 🎯 **选型结论**：需要完整 UI 与团队协作 → ArgoCD（主流）；追求极简与 Kustomize 原生 → Flux。**两者都是 CNCF 毕业项目，选团队熟悉的**。

## 7. 回滚与灾难恢复

```bash
# 回滚（GitOps 回滚 = Git 回退）
git revert <release-commit>        # 回退镜像版本
git push                           # ArgoCD 自动同步回旧版本

# 或 ArgoCD UI/CLI
argocd app rollback order-service 1    # 回滚到历史部署
```

```text
灾难恢复（集群重建）：
  ① 新集群装 ArgoCD
  ② 指向同一 Git 仓库
  ③ 自动同步全部应用 → 集群恢复

这就是 GitOps 的"备份"：Git 仓库 = 集群的完整状态备份
```

> 🎯 **GitOps 回滚的独特价值**：**回滚 = Git 回退**——与深红发布理念一致（见 [05](05-发布策略全景.md)），且回滚本身也走 Code Review 与审计。

## 8. 高频避坑

| # | 坑 | 规避 |
|---|----|------|
| 1 | CI 直接操作集群 | CI 只改 Git（GitOps 边界） |
| 2 | selfHeal 开太早 | 先手动同步，成熟后自动化 |
| 3 | 密钥进 Git | Sealed Secrets / External Secrets |
| 4 | 忽略 prune | `prune: true` 防"删了 Git 资源但集群还在" |
| 5 | 多环境一个目录 | 环境目录分离（overlays/dev-prod） |
| 6 | 忽略漂移监控 | selfHeal 或告警（OutOfSync 通知） |
| 7 | 大仓库慢 | 拆分仓库/目录（App of Apps） |
| 8 | 无回滚演练 | 定期演练 Git 回退 + 同步 |

## 9. 核心要点

> 🎯 **核心要点**：
> - GitOps 三原则：声明式 + 版本化 + 自动同步（Git = 唯一事实来源）；
> - ArgoCD 核心：Application 声明 + Sync 对比 + selfHeal 漂移纠正；
> - **CI 不碰集群**：CI 只更新 Git 镜像 tag，ArgoCD 负责部署（权限最小化）；
> - App of Apps：根应用管全部子应用（团队自治 + 单一入口）；
> - 与 Flux 对比：要 UI 选 ArgoCD，要极简选 Flux；
> - 回滚 = Git 回退（审计闭环 + 灾难恢复即重新指向 Git）；
> - 演进路径：手动同步 → 自动化同步 → 全自动交付。

## 10. 参考来源

- [GitOps 官方定义（CNCF）](https://opengitops.dev/)
- [ArgoCD 官方文档](https://argo-cd.readthedocs.io/)
- [ArgoCD 与 CI 集成模式](https://argo-cd.readthedocs.io/en/stable/user-guide/ci_automation/)
- [Flux 官方文档](https://fluxcd.io/)
- [GitOps 革命：ArgoCD 与 Argo Rollouts](https://feibaoke.com/blog/20909)

---

**下一模块**：[08-生产实践与面试题](08-生产实践与面试题.md)　/　**返回总览**：[00-总览](00-CI-CD与灰度发布总览.md)
