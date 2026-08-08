# Docker 知识体系总览
> Docker 是微服务时代的基础设施——从核心概念到生产实践，掌握容器化技术的完整知识体系，让"在我机器上能跑"成为历史

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [主题速查索引](#4-主题速查索引)
5. [与其他模块的关系](#5-与其他模块的关系)
6. [核心概念速查](#6-核心概念速查)
7. [参考来源](#7-参考来源)

## 1. 知识体系导图

```text
Docker 精通体系（12 篇，2026-08 重构）
│
├── 🔰 基础与概念 (00-02)
│   ├── 00-Docker知识体系总览.md        # 导航与路线
│   ├── 01-Docker核心概念与架构.md      # 容器vs虚拟机/镜像分层/Namespace/OCI
│   └── 02-容器生命周期与常用命令.md     # 镜像/容器/交互/清理命令全集
│
├── 🏗️ 镜像与分发 (03-04)
│   ├── 03-Dockerfile与镜像构建.md      # 指令详解/多阶段/缓存/安全
│   └── 04-镜像发布与仓库分发.md        # Registry/Harbor/认证/HTTPS
│
├── 🔌 网络与存储 (05-06)
│   ├── 05-Docker网络配置与通信.md      # 端口映射/自定义网络/跨主机
│   └── 06-存储卷与数据持久化.md        # 绑定挂载/管理卷/共享
│
├── 🚀 编排与集群 (07-09)
│   ├── 07-Docker-Compose多服务编排.md  # 声明式环境/多环境/生产配置
│   ├── 08-Docker安全与资源限制.md      # 隔离本质/四大危险/最小权限
│   └── 09-多主机与Swarm集群.md         # Machine/Swarm/Stack/高可用
│
├── ☸️ 云原生衔接 (10)
│   └── 10-Docker与Kubernetes衔接.md    # K8s概念速览/发布策略/CI-CD
│
└── 🧭 场景与索引 (11)
    └── 11-应用场景与学习索引.md        # 八大场景/学习路线/实战清单
```

> 🎯 精通 Docker 的标志：不是会跑 `docker run`，而是理解每个容器本质上就是一个被隔离的进程——Namespace 提供"错觉"、Cgroups 提供"限制"、UnionFS 提供"文件系统"。

## 2. 模块导航

| 序号 | 文件 | 内容 | 级别 |
|:---:|------|------|:---:|
| 00 | [总览（本文）](00-Docker知识体系总览.md) | 导图、路线、速查 | - |
| 01 | [核心概念与架构](01-Docker核心概念与架构.md) | 容器 vs VM、三要素、UnionFS、Namespace/Cgroups、OCI | P0 |
| 02 | [容器生命周期与常用命令](02-容器生命周期与常用命令.md) | 镜像/容器/交互/清理命令全集、避坑 | P0 |
| 03 | [Dockerfile与镜像构建](03-Dockerfile与镜像构建.md) | 指令详解、多阶段构建、缓存优化、安全实践 | P0 |
| 04 | [镜像发布与仓库分发](04-镜像发布与仓库分发.md) | Docker Hub/阿里云、自建 Registry、Harbor、HTTPS | P1 |
| 05 | [网络配置与通信](05-Docker网络配置与通信.md) | 端口映射、自定义网络、容器名通信、跨主机 | P1 |
| 06 | [存储卷与数据持久化](06-存储卷与数据持久化.md) | 绑定挂载、管理卷、卷间共享、避坑 | P1 |
| 07 | [Compose 多服务编排](07-Docker-Compose多服务编排.md) | 声明式环境、服务间通信、多环境、生产配置 | P0 |
| 08 | [安全与资源限制](08-Docker安全与资源限制.md) | 隔离本质、四大危险、非 root、最小权限 | P1 |
| 09 | [多主机与Swarm集群](09-多主机与Swarm集群.md) | Docker Machine、Swarm、Stack、高可用 | P2 |
| 10 | [Docker与Kubernetes衔接](10-Docker与Kubernetes衔接.md) | K8s 概念速览、部署实战、蓝绿/金丝雀、CI/CD | P2 |
| 11 | [应用场景与学习索引](11-应用场景与学习索引.md) | 八大场景、Java 适配、学习路线、实战清单 | - |

## 3. 学习路线推荐

```text
🟢 L1：能跑起来（1天）
02-容器生命周期 → 01-核心概念 → 03-Dockerfile → 04-镜像发布
产出：能将 SpringBoot 项目打包成 Docker 镜像并运行

🔵 L2：生产可用（3天）
05-网络 → 06-存储 → 07-Compose → 08-安全
产出：能用 Compose 编排 SpringBoot+MySQL+Redis 多服务

🟣 L3：架构精通（1周）
09-Swarm → 10-K8s衔接 → 11-场景索引
产出：能设计 Docker 化微服务的完整 CI/CD + 部署方案
```

## 4. 主题速查索引

| 我想... | 看这篇 |
|--------|--------|
| 理解 Docker 是什么 | 01-核心概念与架构 |
| 跑第一个容器 | 02-容器生命周期 |
| 写一个 Dockerfile | 03-Dockerfile与镜像构建 |
| 镜像发布到仓库 | 04-镜像发布与仓库分发 |
| 容器间通信 | 05-网络配置与通信 |
| 数据持久化 | 06-存储卷与数据持久化 |
| 一键起多服务 | 07-Compose 多服务编排 |
| 容器安全加固 | 08-安全与资源限制 |
| 多机集群部署 | 09-多主机与Swarm |
| 从 Docker 到 K8s | 10-Docker与Kubernetes衔接 |
| 学习路线/实战清单 | 11-应用场景与学习索引 |

## 5. 与其他模块的关系

| 模块 | 关系 |
|------|------|
| `11-云原生/Kubernetes/` | K8s 完整体系（12 篇），本体系第 10 篇为其衔接入口 |
| `11-云原生/Prometheus+Grafana 监控告警/` | 容器监控的配套体系 |
| `05-开发流程工具/Git/` | 镜像版本管理与 Git 版本管理联动（CI/CD 流水线） |
| `07-工程运维基础/测试工具/Testcontainers` | Testcontainers 核心依赖 Docker |

## 6. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| **镜像（Image）** | 只读模板（类比 Java 的"类"），分层构建，包含代码/运行时/依赖/配置 |
| **容器（Container）** | 镜像的运行实例（类比"对象"），在镜像层上添加可写层 |
| **仓库（Registry）** | 存储分发镜像（类比 Maven 仓库），Docker Hub ≈ Maven Central |
| **UnionFS** | 联合文件系统，镜像分层叠加，overlay2 为现代默认驱动 |
| **写时复制（CoW）** | 修改只读层文件先复制到可写层，多容器共享底层节省磁盘 |
| **Namespace** | 隔离"资源可见性"（PID/网络/挂载/用户等 6 种） |
| **Cgroups** | 限制"资源使用上限"（CPU/内存/磁盘 IO） |
| **OCI** | 容器标准化组织：Image Spec + Runtime Spec，runc 为参考实现 |
| **端口映射** | `-p 宿主机端口:容器端口`，外部访问入口 |
| **Volume** | 数据持久化：绑定挂载 / Docker 管理卷 / tmpfs |
| **Compose** | 声明式多容器编排（单机） |
| **Swarm** | 官方多主机编排（Manager/Worker、Service/Stack） |
| **Dockerfile** | 镜像构建脚本：FROM/RUN/COPY/CMD/ENTRYPOINT 等指令 |

## 7. 参考来源

- [Docker 官方文档](https://docs.docker.com/)
- [Dockerfile 参考（官方）](https://docs.docker.com/reference/dockerfile/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Docker Swarm 文档](https://docs.docker.com/engine/swarm/)
- [Harbor 官方文档](https://goharbor.io/docs/)
- [Kubernetes 官方文档](https://kubernetes.io/zh-cn/docs/)

---

**下一模块**：[01-Docker核心概念与架构](01-Docker核心概念与架构.md)
