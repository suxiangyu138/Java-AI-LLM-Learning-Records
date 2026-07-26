# Docker 容器技术 — 学习目录

## 学习路线图

```
第1-2天        第3-4天        第5-6天        第7-8天        第9-10天+
基础概念        镜像与容器      数据与网络      Dockerfile    生态与实战
    │               │               │               │               │
01 概述    ──▶  03 镜像    ──▶  05 数据卷  ──▶  07 Dockerfile ──▶  09 实战项目
02 安装         04 容器         06 网络         08 Compose       10 进阶生态
                                                              11 生态工具
                                                              12 开发工作流
```

## 文档清单

### 基础篇

| # | 文档 | 核心内容 |
|---|---|---|
| 01 | [Docker概述与核心概念](01-Docker概述与核心概念.md) | 容器 vs 虚拟机、镜像/容器/仓库、Docker 架构 |
| 02 | [Docker安装与配置](02-Docker安装与配置.md) | Win/Linux 安装、镜像加速、daemon.json 配置 |

### 核心篇

| # | 文档 | 核心内容 |
|---|---|---|
| 03 | [Docker镜像管理](03-Docker镜像管理.md) | UnionFS 分层原理、镜像命令、Docker Hub、tag 管理 |
| 04 | [Docker容器管理](04-Docker容器管理.md) | 生命周期、docker run 速查、exec/debug/logs |
| 05 | [Docker数据卷与持久化](05-Docker数据卷与持久化.md) | Volume/Bind Mount/tmpfs 对比、备份迁移 |
| 06 | [Docker网络管理](06-Docker网络管理.md) | bridge/host/overlay、DNS、WordPress 实战 |

### 进阶篇

| # | 文档 | 核心内容 |
|---|---|---|
| 07 | [Dockerfile最佳实践](07-Dockerfile最佳实践.md) | 全指令详解、多阶段构建、层缓存、生产级模板 |
| 08 | [Docker-Compose多容器编排](08-Docker-Compose多容器编排.md) | yml 语法、多环境、profile、健康检查、Web 全栈 |
| 09 | [Docker实战项目](09-Docker实战项目.md) | Flask+MySQL+Redis、MERN 全栈、CI/CD、多语言配方 |
| 10 | [Docker进阶与生态](10-Docker进阶与生态.md) | Swarm 集群、安全加固、日志监控、Harbor、性能优化 |

### 生态扩展篇

| # | 文档 | 核心内容 |
|---|---|---|
| 11 | [Docker生态工具链详解](11-Docker生态工具链详解.md) | Lazydocker、Dive、Harbor、Prometheus+Grafana、CI/CD 流水线、K8s 入门映射 |
| 12 | [Docker日常开发工作流](12-Docker日常开发工作流.md) | 热重载、netshoot 调试、环境共享、数据库管理、维护清单 |

## Docker 技术全景图

```
                        ┌─────────────────────┐
                        │    Docker Compose    │
                        │    (多容器编排)       │
                        └──────────┬──────────┘
                                   │
┌─────────────┐    ┌───────────────┼───────────────┐    ┌─────────────┐
│  Dockerfile  │    │         Docker CLI          │    │ Docker Hub  │
│  (镜像构建)   │    │    (build/pull/run/push)    │    │ Harbor      │
└──────┬───────┘    └───────────────┼───────────────┘    │ (镜像仓库)   │
       │                            │                    └─────────────┘
       ▼                            ▼
┌──────────────┐           ┌───────────────────┐
│   Image      │── run ──▶│    Container       │
│   (镜像)     │           │    (容器)           │
│              │           │                    │
│  只读分层     │           │  ┌──────────────┐  │
│  UnionFS     │           │  │ 可写容器层     │  │
│              │           │  ├──────────────┤  │
│              │           │  │ Image 层 N    │  │
│              │           │  │    ...        │  │
│              │           │  │ Image 层 1    │  │
│              │           │  └──────────────┘  │
└──────────────┘           └─────────┬─────────┘
                                     │
                          ┌──────────┴──────────┐
                          │         │           │
                     Volume      Network     tmpfs
                    (持久化)    (通信隔离)   (敏感数据)
```

## 快速索引（按场景）

| 我想... | 看这篇 |
|---|---|
| 理解 Docker 是什么 | [01-概述](01-Docker概述与核心概念.md) |
| 安装 Docker | [02-安装](02-Docker安装与配置.md) |
| 学会镜像相关操作 | [03-镜像管理](03-Docker镜像管理.md) |
| 学会容器相关操作 | [04-容器管理](04-Docker容器管理.md) |
| 数据如何持久化 | [05-数据卷](05-Docker数据卷与持久化.md) |
| 容器间如何通信 | [06-网络管理](06-Docker网络管理.md) |
| 写一个 Dockerfile | [07-Dockerfile](07-Dockerfile最佳实践.md) |
| 管理多容器应用 | [08-Compose](08-Docker-Compose多容器编排.md) |
| 看一个完整项目 | [09-实战](09-Docker实战项目.md) |
| 了解 Docker 生态 | [10-进阶生态](10-Docker进阶与生态.md) + [11-生态工具](11-Docker生态工具链详解.md) |
| 融入日常开发 | [12-开发工作流](12-Docker日常开发工作流.md) |
| 使用 Lazydocker/dive | [11-生态工具](11-Docker生态工具链详解.md) |
| 搭建 CI/CD | [11-生态工具§11.5](11-Docker生态工具链详解.md) |
| 搭建监控 | [11-生态工具§11.4](11-Docker生态工具链详解.md) |
| 学习 Kubernetes | [11-生态工具§11.6](11-Docker生态工具链详解.md) |

## 实战清单（建议按顺序完成）

- [ ] 安装 Docker，跑通 `docker run hello-world`
- [ ] 用 Docker 跑一个 Nginx，修改首页内容
- [ ] 拉取 MySQL 镜像，挂载 Volume 持久化数据
- [ ] 写一个 Dockerfile 容器化你的项目
- [ ] 用 Dive 分析你的镜像，优化体积
- [ ] 用 Compose 编排多容器应用（如 Web + DB + Redis）
- [ ] 配置健康检查和 depends_on 条件等待
- [ ] 用 Lazydocker 管理容器（熟练使用快捷键）
- [ ] 搭建一个 Harbor 私有仓库
- [ ] 写一个 GitHub Actions CI/CD 流水线
- [ ] 学习 Prometheus + Grafana 监控容器
- [ ] 尝试 Docker Swarm 或 Kubernetes 集群部署
