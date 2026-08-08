# 01-Docker 核心概念与架构
> 容器的本质 = 被 Linux Namespace 隔离 + 被 Cgroups 限制 + 有独立根文件系统的一群进程——不是"轻量级虚拟机"，而是"被隔离的进程"

## 📚 目录
1. [容器 vs 虚拟机](#1-容器-vs-虚拟机)
2. [Docker 三大核心概念](#2-docker-三大核心概念)
3. [镜像分层与 UnionFS](#3-镜像分层与-unionfs)
4. [Namespace 与 Cgroups](#4-namespace-与-cgroups)
5. [Docker 架构与 OCI 标准](#5-docker-架构与-oci-标准)
6. [核心优势与应用场景](#6-核心优势与应用场景)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. 容器 vs 虚拟机

```text
虚拟机 (VM)：                          容器 (Container)：
┌───────────────────┐                  ┌───────────────────┐
│ App A │ App B     │                  │ App A │ App B     │
├────────┼──────────┤                  ├────────┼──────────┤
│ Guest OS A│Guest OS│                  │ Docker Engine     │
├──────────┼────────┤                  ├───────────────────┤
│ Hypervisor        │                  │ Host OS (共享内核)  │
├───────────────────┤                  ├───────────────────┤
│ Host OS           │                  │ Hardware           │
├───────────────────┤                  └───────────────────┘
│ Hardware          │
└───────────────────┘
                          VM启动30s-分钟级    容器启动毫秒级
  每个VM有独立OS内核        所有容器共享Host内核
```

| 维度 | 虚拟机 | 容器 |
|------|--------|------|
| 隔离层级 | Hypervisor 虚拟硬件 | Linux Namespace + Cgroups |
| OS | 每个 VM 有完整 Guest OS | 共享 Host OS 内核 |
| 启动速度 | 30 秒~数分钟 | 毫秒~秒 |
| 镜像大小 | GB 级 | MB 级（Alpine 仅 5MB） |
| 密度 | 一台机器几十个 VM | 一台机器成百上千容器 |
| 性能 | 接近原生但有损耗（>10%） | 几乎等于原生（<2%） |
| 可移植性 | 依赖 Hypervisor 类型 | OCI 标准，跨平台 |
| 安全性 | 强隔离（独立内核） | 弱于 VM（共享内核） |

**核心差异在于内核共享**：

- 容器本质上是一组被 Namespace 隔离、被 CGroup 限制资源、运行在宿主机内核上的进程；
- Linux 容器无法在 Windows 宿主机上直接运行（除非通过 WSL2 或 Hyper-V 虚拟化 Linux 内核）；
- 容器内执行 `uname -r` 看到的是宿主机内核版本；
- 由于没有独立内核，**容器逃逸攻击的后果比 VM 更严重**。

```bash
# 验证：容器里的进程在宿主机上可以看到
docker run -d --name nginx nginx
ps aux | grep nginx
# 输出：可以看到 nginx 进程！它不是虚拟机，就是宿主机上的进程！
```

> 🎯 **容器的本质** = 被 Linux Namespace 隔离 + 被 Cgroups 限制 + 有独立根文件系统的一群进程。关键区别：容器不是"轻量级虚拟机"，而是"被隔离的进程"。

## 2. Docker 三大核心概念

| 概念 | 类比（Java/OOP） | 说明 |
|------|-----------------|------|
| Image 镜像 | 类（Class） | 只读模板，定义了容器运行所需的一切（OS/依赖/代码），**可复用、可分层** |
| Container 容器 | 实例（Instance） | 镜像的运行实例，在镜像层之上添加可写层，**独立网络/进程/文件系统** |
| Registry 仓库 | Maven 仓库 | 存储和分发镜像（Docker Hub ≈ Maven Central），分公有/私有 |

```bash
# 三大概念的协作流程
docker pull nginx:alpine              # 从 Registry 拉取 Image
docker run -d --name web nginx:alpine  # Image 启动为 Container
docker push myrepo/nginx:custom        # Image 推送到 Registry
```

| 仓库类型 | 代表 | 场景 |
|----------|------|------|
| 公有仓库 | Docker Hub、阿里云容器镜像服务（公有版） | 基础镜像（JDK/Tomcat）、开源项目 |
| 私有仓库 | 自建 Registry、Harbor、阿里云 ACR | 企业级应用、含敏感信息的镜像 |

## 3. 镜像分层与 UnionFS

```text
一个 Docker 镜像由多个只读层 (Layer) 组成：
┌─────────────────────────────┐
│ Layer 6: Application Code   │ ← docker build 的 COPY/ADD
├─────────────────────────────┤
│ Layer 5: Dependencies       │ ← RUN npm install / mvn package
├─────────────────────────────┤
│ Layer 4: JDK/JRE            │ ← FROM openjdk:17
├─────────────────────────────┤
│ Layer 3: libc/基础库        │
├─────────────────────────────┤
│ Layer 2: Alpine Linux       │ ← FROM alpine
├─────────────────────────────┤
│ Layer 1: scratch (空)       │
└─────────────────────────────┘
```

### 3.1 写时复制（Copy-on-Write）

```text
容器启动时，在镜像层之上添加一个可写层（Container Layer）：
┌────────────────┐
│ Container R/W  │  ← 每个容器独立的可写层（容器间隔离）
├────────────────┤
│ Image Layer 6  │
├────────────────┤
│ Image Layer 5  │  ← 所有镜像层对容器是只读的
├────────────────┤
│ ...            │
└────────────────┘

读文件：从上层往下找，找到即返回（上层优先）
写文件：文件在 Image 层 → 先 Copy 到 Container 层 → 再修改（CoW）
删文件：在 Container 层创建一个"白 out"文件标记删除
```

### 3.2 分层带来的三大好处

| 好处 | 说明 |
|------|------|
| 空间共享 | 10 个容器都基于 `alpine:3.16`，基础层磁盘只有一份拷贝 |
| 构建缓存 | 某层及其上层指令未变化即可复用缓存层，加速构建 |
| 拉取加速 | 只下载本地没有的层，增量更新 |

### 3.3 存储驱动

| 驱动 | 原理 | 内核要求 | 推荐度 |
|------|------|----------|:---:|
| overlay2 | 多层联合文件系统 | Linux 4.0+ | ✅ 默认推荐 |
| aufs | 老牌联合文件系统 | 需单独编译内核 | ❌ 已淘汰 |
| devicemapper | 块级别 | 需要 thin-provisioning | ❌ 不推荐 |
| btrfs/zfs | 文件系统级 | 特殊文件系统 | ⚠️ 特定场景 |

```bash
docker info | grep "Storage Driver"
# 输出: Storage Driver: overlay2  ← 现代 Linux 默认
```

## 4. Namespace 与 Cgroups

### 4.1 Namespace 六种隔离

| Namespace | 隔离内容 | 效果 |
|-----------|----------|------|
| PID | 进程 ID | 容器内 PID=1 ≠ 宿主机 PID=1 |
| NET | 网络栈 | 容器有独立网卡、IP、端口 |
| IPC | 进程间通信 | 信号量/消息队列隔离 |
| MNT | 文件系统挂载点 | 容器有自己的 / 目录 |
| UTS | 主机名和域名 | `hostname` 在容器内独立 |
| USER | 用户和组 ID | 容器内 root ≠ 宿主机 root（默认） |

```bash
docker inspect <container> | jq '.[0].State.Pid'
ls -la /proc/<pid>/ns/    # 查看该进程的 Namespace 符号链接
```

### 4.2 Cgroups 资源限制

| Cgroup 子系统 | 限制内容 | Docker 参数 |
|-------------|----------|-----------|
| cpu | CPU 使用份额 | `--cpus=2` `--cpu-shares=512` |
| memory | 内存上限 | `--memory=512m` `--memory-swap=1g` |
| blkio | 磁盘 IO | `--blkio-weight=500` `--device-read-bps` |
| pids | 进程数量 | `--pids-limit=100` |

```bash
docker run -d \
  --cpus=1.5 \                    # 最多 1.5 个 CPU
  --memory=512m \                 # 最大 512MB 内存
  --memory-swap=1g \              # 交换分区+内存=1G
  --pids-limit=200 \              # 最多 200 个进程
  nginx

# 查看容器的 Cgroup 设置
cat /sys/fs/cgroup/memory/docker/<container-id>/memory.limit_in_bytes
```

> 🎯 核心认知：Namespace 只隔离"资源可见性"，Cgroups 只限制"资源使用上限"——都是**基于 Linux 内核的有限隔离**，而非虚拟机级别的完全隔离（详见 [08](08-Docker安全与资源限制.md)）。

## 5. Docker 架构与 OCI 标准

### 5.1 架构演进

```text
Docker 最新架构:
  ┌──────────┐
  │docker CLI│ gRPC API
  └────┬─────┘
       ↓
  ┌──────────────┐
  │   dockerd     │  (Docker Daemon: 构建/BuildKit、镜像管理、网络、卷)
  └──────┬───────┘
         ↓ gRPC
  ┌──────────────┐
  │  containerd   │  (容器生命周期管理: 拉取镜像、创建/启动/停止容器)
  └──────┬───────┘
         ↓ shim进程
  ┌──────────────┐
  │     runc      │  (OCI运行时: 实际创建容器—调 Namespace+Cgroups)
  └──────────────┘
```

### 5.2 OCI 规范

| 规范 | 内容 | 作用 |
|------|------|------|
| OCI Image Spec | 镜像格式标准（层结构、配置、manifest） | 任何符合 OCI 的镜像可在任何 OCI 运行时运行 |
| OCI Runtime Spec | 容器运行时标准（如何启动容器） | runc 是参考实现，也有 kata-containers、gVisor |

> 💡 **OCI 的意义**：打破 Docker 公司的垄断 → 可以用 Podman/Buildah 构建镜像、用 containerd/cri-o 运行容器、上传到任何 OCI 兼容的 Registry——这就是 **Kubernetes 1.24+ 移除 dockershim** 的背景。

### 5.3 Docker vs 其他容器工具

| 工具 | 定位 | 特点 |
|------|------|------|
| Docker | 全能型容器平台 | 构建+运行+编排+生态 |
| Podman | Docker 替代（daemonless） | 无守护进程、rootless、兼容 Docker CLI |
| Buildah | 专注于镜像构建 | 比 Docker build 更灵活 |
| containerd | 容器运行时 | K8s 默认 CRI 实现 |
| nerdctl | containerd 的 Docker 兼容 CLI | Docker 用户零成本迁移 |

## 6. 核心优势与应用场景

### 6.1 五大核心优势

| 优势 | 说明 |
|------|------|
| 环境一致性 | 开发/测试/生产环境差异导致"本地能跑，部署就报错"——Docker 打包全部依赖，一键同步 |
| 轻量化 | 容器共享宿主机内核，一台机器可运行数百容器，资源利用率大幅提升 |
| 快速部署与启停 | 镜像快速拉取、容器启动秒级（虚拟机分钟级，效率提升数十倍） |
| 隔离性强 | 每个容器独立隔离，一个容器出问题不影响其他容器和宿主机 |
| 可复用、可扩展 | 镜像可重复使用；与微服务高度契合，每个微服务一个独立容器 |

### 6.2 与 Java 后端的适配底层逻辑

> 🎯 **双重保障**：Java 的跨平台依赖 JVM 虚拟机，Docker 的跨平台依赖宿主机的操作系统内核——二者结合实现"**JVM 隔离 + 系统环境隔离**"的双重保障，彻底解决"环境兼容"问题。

| Java 后端痛点 | Docker 解法 |
|---------------|-------------|
| 环境不一致（JDK 版本/依赖库/系统配置差异） | 打包"Java 应用 + JVM + 依赖库 + 系统配置" |
| 部署繁琐（手动装 JDK/配环境/传 JAR） | 打包为镜像，一条命令启动 |
| 资源隔离（一个应用 OOM 影响其他应用） | "一个应用一个容器"，独立内存/CPU/网络 |
| 微服务落地（技术栈差异难以适配） | 每个微服务单独构建镜像 |

**三个核心适配点**：

```bash
# 适配点 1：JVM 与 Docker 资源匹配
docker run -m 2g --cpus 1 my-java-app
# 关键：JVM 的 -Xms/-Xmx 必须与容器限制匹配（详见 03）

# 适配点 2：Spring Boot 无缝集成（内置 Tomcat，无需额外 Web 容器）
# 只需在 Dockerfile 中指定 JAR 包路径即可启动

# 适配点 3：Maven/Gradle 插件一键自动化
# docker-maven-plugin 实现"编译 → JAR → 镜像 → 推送"全自动（详见 03）
```

## 7. 核心要点

> 🎯 **核心要点**：
> - 容器 = 被隔离的进程（Namespace 提供"错觉"、Cgroups 提供"限制"、UnionFS 提供"文件系统"）；
> - 三要素类比：镜像=类、容器=实例、仓库=Maven 仓库；
> - 镜像分层 + CoW = 空间共享/构建缓存/拉取加速三大收益；
> - OCI 标准化是"任何镜像跑在任何运行时"的基石（K8s 移除 dockershim 的背景）；
> - Java + Docker = "JVM 隔离 + 系统环境隔离"双重保障。

## 8. 参考来源

- [Docker 官方文档：What is Docker](https://docs.docker.com/get-started/)
- [Docker 存储驱动文档（overlay2）](https://docs.docker.com/storage/storagedriver/overlayfs-driver/)
- [Open Container Initiative](https://opencontainers.org/)
- [Docker 架构文档](https://docs.docker.com/get-started/docker_architecture/)

---

**下一模块**：[02-容器生命周期与常用命令](02-容器生命周期与常用命令.md)　/　**返回总览**：[00-总览](00-Docker知识体系总览.md)
