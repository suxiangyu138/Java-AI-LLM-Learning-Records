# 00-Docker知识体系总览
> 🎯 Docker是微服务时代的基础设施 — 从核心概念到生产实践，掌握容器化技术的完整知识体系，让"在我机器上能跑"成为历史

---

## 目录
1. [Docker知识全景](#1-docker知识全景)
2. [知识文件导航](#2-知识文件导航)
3. [精通级学习路线](#3-精通级学习路线)
4. [与其他模块的关系](#4-与其他模块的关系)

---

## 1. Docker知识全景

```
Docker 知识体系（精通级）
│
├── 🏗️ 核心概念与架构
│   ├── 容器 vs 虚拟机：本质区别（共享内核 vs 虚拟硬件）
│   ├── 三大核心：Image(只读模板) → Container(运行实例) → Registry(仓库)
│   ├── UnionFS分层文件系统、写时复制(Copy-on-Write)
│   ├── Docker架构：Client → Docker Daemon → containerd → runc
│   └── OCI标准（Open Container Initiative）
│       → 01-Docker核心概念与架构.md + Docker.md
│
├── 📦 镜像构建
│   ├── Dockerfile指令全解（FROM/RUN/COPY/ADD/CMD/ENTRYPOINT/ENV/ARG...）
│   ├── 多阶段构建（Multi-stage Build）：编译与运行分离
│   ├── 镜像优化：层级优化（合理安排指令顺序）、精简基础镜像（alpine/distroless）
│   ├── BuildKit：新一代构建引擎，并行构建+缓存加速
│   └── 镜像安全扫描（Trivy/Docker Scout）
│       → Docker：在镜像中打包软件.md + Docker：构建自动化和高级镜像设置.md
│
├── 🌐 网络
│   ├── 网络模式：bridge/host/none/overlay/macvlan
│   ├── 容器间通信：link（已废弃）→ 自定义网络 + DNS服务发现
│   ├── 端口映射：-p host:container 与 EXPOSE的区别
│   └── Docker网络底层：iptables规则、网络命名空间
│       → Docker网络访问.md
│
├── 💾 存储
│   ├── 三种挂载方式：Volume（推荐） / Bind Mount / tmpfs
│   ├── Volume驱动：local/nfs/云存储
│   ├── 数据卷容器（已废弃）→ 命名卷 + docker-compose volumes
│   └── 持久化最佳实践
│       → Docker 持久化存储与卷间状态共享.md
│
├── 🔒 安全与隔离
│   ├── Linux内核隔离机制：Namespace(6种)+ Cgroups(资源限制)
│   ├── Capabilities权限控制、seccomp/AppArmor安全策略
│   ├── 资源限制：--memory/--cpus/--blkio-weight
│   ├── 非root用户运行（USER指令）
│   └── 镜像签名与内容信任（Docker Content Trust）
│       → Docker隔离-限制危险.md
│
├── 🚀 Compose编排
│   ├── docker-compose.yml全语法（services/networks/volumes/secrets）
│   ├── 环境变量与配置文件分离
│   ├── depends_on + healthcheck 服务依赖管理
│   └── Compose与生产环境的边界（开发用Compose，生产用K8s）
│       → Docker：Docker Compose声明式环境.md
│
├── 📋 镜像仓库与分发
│   ├── Docker Hub / Harbor / AWS ECR
│   ├── Registry API v2 + 垃圾回收
│   ├── 镜像Tag策略（latest是反模式、语义化版本）
│   └── 镜像缓存与代理（Registry Mirror）
│       → Docker：公有和私有软件分发.md + Docker：运行自定义Registry.md
│
├── 🖥️ 多主机与集群
│   ├── Docker Swarm：Service/Task/Stack模型
│   ├── Overlay网络 + 路由Mesh
│   ├── Docker Stack（Compose v3 → Swarm部署）
│   └── Swarm vs Kubernetes：什么时候用Swarm
│       → Docker：Docker Machine和Swarm集群.md + Docker：多容器和多主机环境.md
│
└── 🏭 SpringBoot生产实践
    ├── SpringBoot + Docker完整流程（Dockerfile → Build → Push → Deploy）
    ├── JVM参数在容器中的适配（-XX:+UseContainerSupport）
    ├── Graceful Shutdown与健康检查
    └── CI/CD中的Docker集成（GitHub Actions / Jenkins）
        → 01-Docker与Kubernetes容器化部署.md + 05-SpringBoot自动装配.md
```

---

## 2. 知识文件导航

| # | 文件 | 内容 | 级别 |
|---|------|------|:---:|
| 00 | Docker知识体系总览.md | 全景导航 | — |
| 01 | Docker核心概念与架构.md | 容器原理、架构、vs VM | P0 |
| 02 | Docker镜像构建与Dockerfile.md | Dockerfile精通、多阶段构建、优化 | P0 |
| 03 | Docker网络深度解析.md | 网络模式、通信、iptables | P1 |
| 04 | Docker存储与数据持久化.md | Volume/Bind/tmpfs | P1 |
| 05 | Docker Compose多服务编排.md | Compose语法、多服务、环境管理 | P0 |
| 06 | Docker安全与资源隔离.md | Namespace/Cgroups/安全最佳实践 | P1 |
| 07 | Docker镜像仓库与分发.md | Registry/Harbor/Tag策略 | P1 |
| 08 | Docker多主机与Swarm集群.md | Swarm/Stack/Overlay | P2 |
| 09 | Docker与SpringBoot生产实践.md | 完整部署流程、JVM适配、CI/CD | P0 |

### 📚 完整文件清单（按编号导航）

| # | 文件 | 内容 | 大小 |
|---|------|------|------|
| 00 | 00-Docker知识体系总览.md | 全景导航+学习路线 | 7KB |
| 01 | 01-Docker核心概念与架构.md | Namespace/Cgroups/UnionFS/OCI | 11KB |
| 02 | 02-Docker快速入门.md | 概念入门与第一个容器 | 5KB |
| 03 | 03-Docker综合参考手册.md | 综合命令与概念参考 | 16KB |
| 04 | 04-Docker应用场景与生态.md | Use Cases与工具生态 | 6KB |
| 05 | 05-容器生命周期与运行管理.md | 容器创建/启停/调试 | 6KB |
| 06 | 06-Dockerfile与镜像构建.md | Dockerfile指令与镜像构建 | 22KB |
| 07 | 07-镜像构建进阶与多阶段构建.md | 高级构建/BuildKit/优化 | 24KB |
| 08 | 08-镜像发布与部署流程.md | 镜像发布与部署 | 22KB |
| 09 | 09-Docker网络配置与通信.md | 网络模式/通信/iptables | 13KB |
| 10 | 10-Docker存储卷与数据持久化.md | Volume/Bind/tmpfs | 9KB |
| 11 | 11-Docker-Compose多服务编排.md | Compose全语法+实战 | 28KB |
| 12 | 12-Docker安全加固与资源限制.md | Namespace/Cgroups/安全实践 | 17KB |
| 13 | 13-Docker镜像仓库与分发.md | Registry/Harbor/Tag策略 | 23KB |
| 14 | 14-私有Registry部署与运维.md | 自建Registry+Docker Hub | 22KB |
| 15 | 15-Docker多主机与容器编排.md | 多主机通信与编排 | 25KB |
| 16 | 16-Docker-Swarm集群实战.md | Swarm/Stack/Overlay | 32KB |
| 17 | 17-Docker学习资源索引.md | 原始学习目录参考 | 7KB |
| — | 01-Docker与Kubernetes容器化部署.md | Docker+K8s生产实践(56KB) | 56KB |

---

## 3. 精通级学习路线

### 🟢 L1：能跑起来（1天）
```
Docker 全面介绍 → Docker可以做的事 → 在Docker容器中运行软件
→ Docker镜像发布：如何打包软件（SpringBoot实战）
产出：能将SpringBoot项目打包成Docker镜像并运行
```

### 🔵 L2：生产可用（3天）
```
Docker：在镜像中打包软件（深入） → Docker：构建自动化和高级镜像设置
→ Docker网络访问 → Docker 持久化存储
→ Docker：Docker Compose声明式环境
产出：能用Compose编排SpringBoot+MySQL+Redis多服务
```

### 🟣 L3：架构精通（1周）
```
Docker隔离-限制危险 → Docker：公有和私有软件分发 → Docker：运行自定义Registry
→ Docker：多容器和多主机环境 → Docker：Docker Machine和Swarm集群
→ 01-Docker与Kubernetes容器化部署（K8s部分）
产出：能设计Docker化微服务的完整CI/CD+部署方案
```

---

## 4. 与其他模块的关系

| 模块 | 关系 |
|------|------|
| `Spring全家桶/20-容器化部署-Docker与K8s.md` | SpringBoot视角的Docker+K8s部署 |
| `后端要掌握的前端知识/` | 前端也是容器化的受益者 |
| `软件工程/06-软件配置管理.md` | Docker镜像版本管理是SCM的延伸 |
| `测试工具/Testcontainers.md` | Testcontainers核心依赖Docker |
