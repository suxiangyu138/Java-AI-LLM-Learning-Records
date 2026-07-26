# 01-Docker核心概念与架构
> 🎯 Docker底层不是魔法 — 掌握容器vs虚拟机的本质区别、UnionFS分层文件系统、Namespace+Cgroups隔离机制、OCI标准，理解Docker为什么"轻量"

---

## 目录
1. [容器 vs 虚拟机](#1-容器-vs-虚拟机)
2. [Docker三大核心概念](#2-docker三大核心概念)
3. [UnionFS与镜像分层](#3-unionfs与镜像分层)
4. [Linux内核隔离机制](#4-linux内核隔离机制)
5. [Docker架构与OCI标准](#5-docker架构与oci标准)
6. [Docker引擎与运行时](#6-docker引擎与运行时)

---

## 1. 容器 vs 虚拟机

### 1.1 本质区别

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

关键区别：容器不是"轻量级虚拟机"，而是"被隔离的进程"。
```

| 维度 | 虚拟机 | 容器 |
|------|--------|------|
| **隔离层级** | Hypervisor虚拟硬件 | Linux Namespace + Cgroups |
| **OS** | 每个VM有完整Guest OS | 共享Host OS内核 |
| **启动速度** | 30秒~数分钟 | 毫秒~秒 |
| **镜像大小** | GB级 | MB级（可小至5MB） |
| **密度** | 一台机器几十个VM | 一台机器成百上千容器 |
| **性能** | 接近原生但有损耗(>10%) | 几乎等于原生(<2%) |
| **可移植性** | 依赖Hypervisor类型 | OCI标准，跨平台 |
| **安全性** | 强隔离（独立内核） | 弱于VM（共享内核） |

### 1.2 容器的本质

> 🎯 **容器的本质 = 被Linux Namespace隔离 + 被Cgroups限制 + 有独立根文件系统的一群进程**。

```bash
# 验证：容器里的进程在宿主机上可以看到
docker run -d --name nginx nginx
ps aux | grep nginx
# 输出：可以看到nginx进程！它不是虚拟机，就是宿主机上的进程！
```

---

## 2. Docker三大核心概念

| 概念 | 类比（Java/OOP） | 说明 |
|------|-----------------|------|
| **Image 镜像** | 类(Class) | 只读模板，定义了容器运行所需的一切（OS/依赖/代码） |
| **Container 容器** | 实例(Instance) | 镜像的运行实例，在镜像层之上添加可写层 |
| **Registry 仓库** | Maven仓库 | 存储和分发镜像（Docker Hub ≈ Maven Central） |

```bash
# 三大概念的协作流程
docker pull nginx:alpine              # 从Registry拉取Image
docker run -d --name web nginx:alpine  # Image启动为Container
docker push myrepo/nginx:custom        # Image推送到Registry
```

### 2.1 Image镜像的组成

```text
一个Docker镜像由多个只读层(Layer)组成：

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

每层是一个增量（diff），层与层之间通过UnionFS叠加。
相同的层可以在不同镜像间共享 → 节省磁盘 + 加速拉取。
```

---

## 3. UnionFS与镜像分层

### 3.1 写时复制 Copy-on-Write

```text
容器启动时，在镜像层之上添加一个可写层（Container Layer）：
┌────────────────┐
│ Container R/W  │  ← 每个容器独立的可写层（容器间隔离）
├────────────────┤
│ Image Layer 6  │
├────────────────┤
│ Image Layer 5  │  ← 所有镜像层对容器是只读的
├────────────────┤
│ Image Layer 4  │
├────────────────┤
│ ...            │
└────────────────┘

读文件：从上层往下找，找到即返回（上层优先）
写文件：文件在Image层 → 先Copy到Container层 → 再修改（CoW）
删文件：在Container层创建一个"白out"文件标记删除
```

### 3.2 存储驱动对比

| 驱动 | 原理 | 内核要求 | 推荐度 |
|------|------|----------|:---:|
| **overlay2** | 多层联合文件系统 | Linux 4.0+ | ✅ 默认推荐 |
| aufs | 老牌联合文件系统 | 需单独编译内核 | ❌ 已淘汰 |
| devicemapper | 块级别 | 需要thin-provisioning | ❌ 不推荐 |
| btrfs/zfs | 文件系统级 | 特殊的文件系统 | ⚠️ 特定场景 |

```bash
# 查看当前存储驱动
docker info | grep "Storage Driver"
# 输出: Storage Driver: overlay2  ← 现代Linux默认
```

---

## 4. Linux内核隔离机制

### 4.1 Namespace（六种隔离）

| Namespace | 隔离内容 | 效果 |
|-----------|----------|------|
| **PID** | 进程ID | 容器内PID=1 ≠ 宿主机PID=1 |
| **NET** | 网络栈 | 容器有独立网卡、IP、端口 |
| **IPC** | 进程间通信 | 信号量/消息队列隔离 |
| **MNT** | 文件系统挂载点 | 容器有自己的/目录 |
| **UTS** | 主机名和域名 | `hostname`在容器内独立 |
| **USER** | 用户和组ID | 容器内root ≠ 宿主机root（默认） |

```bash
# 查看某个容器的Namespace
docker inspect <container> | jq '.[0].State.Pid'
ls -la /proc/<pid>/ns/    # 查看该进程的Namespace符号链接
```

### 4.2 Cgroups（资源限制）

| Cgroup子系统 | 限制内容 | Docker参数 |
|-------------|----------|-----------|
| **cpu** | CPU使用份额 | `--cpus=2` `--cpu-shares=512` |
| **memory** | 内存上限 | `--memory=512m` `--memory-swap=1g` |
| **blkio** | 磁盘IO | `--blkio-weight=500` `--device-read-bps` |
| **pids** | 进程数量 | `--pids-limit=100` |

```bash
# 限制容器资源
docker run -d \
  --cpus=1.5 \                    # 最多1.5个CPU
  --memory=512m \                 # 最大512MB内存
  --memory-swap=1g \              # 交换分区+内存=1G
  --pids-limit=200 \              # 最多200个进程
  nginx

# 查看容器的Cgroup设置
cat /sys/fs/cgroup/memory/docker/<container-id>/memory.limit_in_bytes
```

---

## 5. Docker架构与OCI标准

### 5.1 Docker架构演进

```text
Docker 1.x (2013-2016):
  Docker Client → Docker Daemon (单体，包含所有功能)

Docker 1.11+ (2016+):
  Docker CLI → dockerd → containerd → containerd-shim → runc
  
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
  │     runc      │  (OCI运行时: 实际创建容器—调Namespace+Cgroups)
  └──────────────┘
```

### 5.2 OCI标准

> OCI (Open Container Initiative) = 容器标准化组织。定义了两个核心规范：

| 规范 | 内容 | 作用 |
|------|------|------|
| **OCI Image Spec** | 镜像格式标准（层结构、配置文件、manifest） | 任何符合OCI的镜像可在任何OCI运行时运行 |
| **OCI Runtime Spec** | 容器运行时标准（如何启动容器） | runc是参考实现，也有kata-containers、gVisor |

```text
OCI的意义：
  打破了Docker公司的垄断 → 
  你可以用Podman/Buildah构建镜像、
  用containerd/cri-o运行容器、
  上传到任何OCI兼容的Registry
  → 这就是Kubernetes 1.24+移除dockershim的背景
```

---

## 6. Docker引擎与运行时

### 6.1 运行时层级

| 层级 | 组件 | 说明 |
|------|------|------|
| **High-Level Runtime** | containerd, CRI-O | 管理镜像传输、解压、容器生命周期 |
| **Low-Level Runtime** | runc, crun, kata, gVisor | 实际创建容器（调Namespace/Cgroups） |

```bash
# 直接用runc运行OCI容器（不用Docker）
mkdir rootfs
docker export $(docker create alpine) | tar -C rootfs -xf -
runc spec
runc run mycontainer
```

### 6.2 Docker vs 其他容器工具

| 工具 | 定位 | 特点 |
|------|------|------|
| **Docker** | 全能型容器平台 | 构建+运行+编排+生态 |
| **Podman** | Docker替代（daemonless） | 无守护进程、rootless、兼容Docker CLI |
| **Buildah** | 专注于镜像构建 | 比Docker build更灵活 |
| **containerd** | 容器运行时 | K8s默认CRI实现 |
| **nerdctl** | containerd的Docker兼容CLI | Docker用户零成本迁移 |

---

> 🎯 **精通Docker的标志**：不是会跑`docker run`，而是理解每个容器本质上就是一个被隔离的进程 — Namespace提供"错觉"、Cgroups提供"限制"、UnionFS提供"文件系统"。
