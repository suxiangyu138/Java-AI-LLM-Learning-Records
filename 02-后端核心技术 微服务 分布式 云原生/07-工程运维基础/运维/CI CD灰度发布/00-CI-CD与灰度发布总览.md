# CI/CD 与灰度发布总览
> Java 后端的交付流水线：CI/CD 工具链、质量门禁、发布策略（蓝绿/金丝雀/滚动）、灰度落地、GitOps——从"代码提交"到"安全上线"

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [2026 工具链全景](#4-2026-工具链全景)
5. [核心概念速查](#5-核心概念速查)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
CI/CD 与灰度发布体系（8 篇，2026-08 基准）
│
├─ 基础层 ─────────────────────────────
│   ├─ 00 总览（本文）
│   └─ 01 CI/CD 概念与流水线设计（CI/CD/DevOps/流水线阶段）
│
├─ 工具层 ─────────────────────────────
│   ├─ 02 Jenkins 与 GitLab CI 实践
│   ├─ 03 GitHub Actions 与制品管理（Harbor/Nexus）
│   └─ 04 质量门禁与安全左移（测试/扫描/镜像安全）
│
├─ 发布层 ─────────────────────────────
│   ├─ 05 发布策略全景（蓝绿/金丝雀/滚动/深红/回滚）
│   ├─ 06 灰度发布落地（网关/Nacos/K8s Rollouts/Istio）
│   └─ 07 GitOps 与 ArgoCD（声明式交付）
│
└─ 实战层 ─────────────────────────────
│   └─ 08 生产实践与面试题（平台四维度/选型/避坑/面试）
```

> 🎯 核心认知：**CI/CD 解决"怎么把代码变成可交付物"，灰度发布解决"怎么安全地把可交付物变成线上服务"**——两者合起来才是完整的交付闭环。

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-CI-CD与灰度发布总览.md) | 导图、工具链、速查 | 所有人 |
| 01 | [CI/CD 概念与流水线设计](01-CI-CD概念与流水线设计.md) | CI/CD/DevOps、流水线阶段、Pipeline as Code、门禁理念 | 入门必读 |
| 02 | [Jenkins 与 GitLab CI 实践](02-Jenkins与GitLab-CI实践.md) | Jenkins Pipeline、GitLab CI、对比选型 | 进阶 |
| 03 | [GitHub Actions 与制品管理](03-GitHub-Actions与制品管理.md) | Actions 工作流、制品仓库（Harbor/Nexus）、镜像管理 | 进阶 |
| 04 | [质量门禁与安全左移](04-质量门禁与安全左移.md) | 测试门禁、静态扫描、Trivy 镜像扫描、密钥管理 | 重点 |
| 05 | [发布策略全景](05-发布策略全景.md) | 蓝绿/金丝雀/滚动/深红、回滚策略 | 重点 |
| 06 | [灰度发布落地](06-灰度发布落地.md) | 网关灰度、Nacos 配置灰度、Argo Rollouts、Istio | 重点 |
| 07 | [GitOps 与 ArgoCD](07-GitOps与ArgoCD.md) | GitOps 理念、ArgoCD 架构、同步机制、Flux 对比 | 进阶 |
| 08 | [生产实践与面试题](08-生产实践与面试题.md) | 发布平台四维度、工具选型、避坑、面试 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 概念入门（半天） | 了解 CI/CD 全貌 | 00 → 01 → 05 |
| 落地实践（2 天） | 要搭流水线 | 01 → 02/03 → 04 → 06 |
| GitOps 进阶（3 天） | 云原生交付 | 全部 + 07 → 08 |

## 4. 2026 工具链全景

| 环节 | 工具 | 定位 |
|------|------|------|
| CI | Jenkins / GitLab CI / GitHub Actions / Tekton | 构建+测试+制品 |
| 制品 | Harbor / Nexus / GitHub Packages | 镜像/依赖仓库 |
| CD | ArgoCD / Flux / 自研发布平台 | 部署到 K8s |
| 渐进交付 | Argo Rollouts / Istio / Nacos 灰度 | 蓝绿/金丝雀/灰度 |
| 安全 | Trivy / SonarQube / Sealed Secrets / cert-manager | 安全左移 |
| 监控 | Prometheus + Grafana（详见日志监控指标体系） | 发布效果验证 |

```text
典型组合（按团队形态）：
  传统企业过渡：Jenkins Pipeline + Harbor + ArgoCD
  GitLab 全家桶：GitLab CI + Harbor + ArgoCD
  纯云原生：Tekton + ArgoCD + Argo Rollouts
```

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| CI（持续集成） | 频繁合并代码 + 自动构建测试（尽早发现集成问题） |
| CD（持续交付/部署） | 自动部署到环境（交付=随时可发布；部署=自动上线） |
| 流水线（Pipeline） | 构建→测试→制品→部署的阶段链 |
| Pipeline as Code | 流水线写进代码仓库（Jenkinsfile/.gitlab-ci.yml） |
| 质量门禁 | 测试/扫描不通过则阻断流水线 |
| 制品（Artifact） | 构建产物（JAR/镜像），版本化管理 |
| 蓝绿部署 | 双环境切换（秒级回滚，双倍资源） |
| 金丝雀/灰度 | 小流量渐进放量（10%→30%→60%→100%） |
| 滚动更新 | 逐步替换实例（默认策略） |
| 深红发布 | 按请求头/用户属性精确路由（2026 新理念） |
| 回滚 | 恢复到上一版本（镜像/配置/数据联动） |
| GitOps | Git 为唯一事实来源，自动同步集群状态 |
| ArgoCD | GitOps 主流控制器 |
| Argo Rollouts | K8s 渐进交付（蓝绿/金丝雀 + 自动回滚） |
| 安全左移 | 扫描/检查前置到流水线早期 |

## 6. 参考来源

- [GitOps 官方文档（CNCF）](https://opengitops.dev/)
- [ArgoCD 官方文档](https://argo-cd.readthedocs.io/)
- [Argo Rollouts 文档](https://argoproj.github.io/argo-rollouts/)
- [Jenkins 官方文档](https://www.jenkins.io/doc/)
- [GitLab CI 文档](https://docs.gitlab.com/ee/ci/)
- [GitHub Actions 文档](https://docs.github.com/zh/actions)

---

**下一模块**：[01-CI-CD概念与流水线设计](01-CI-CD概念与流水线设计.md)
