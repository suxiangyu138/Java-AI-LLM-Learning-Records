# 容器隔离与 gVisor 用户态内核

> 容器启动快但共享宿主内核——内核漏洞即逃逸面；gVisor 用"用户态内核"在中间拦截，隔离强于容器、轻于 VM，是 ChatGPT Code Interpreter 的底座。

## 1. 容器为何不够

| 维度 | 容器（runc） | 问题 |
|---|---|---|
| 隔离机制 | namespace + cgroup | 只是"视图 + 资源"隔离，**不限制能力** |
| 内核 | **共享宿主内核** | 一个内核漏洞 = 整台宿主机沦陷 |
| 攻击面 | 全部 syscall + 内核漏洞 | 容器逃逸 CVE 层出不穷 |
| 启动 | <1s | 优点（也是唯一显著优点） |
| 适用 | 可信内部代码 | **不可信 LLM 代码不充分** |

> ⚠️ 2026 共识："每个超大规模厂商都拿最强隔离原语对准 AI，没有一家选容器。"微软 Semantic Kernel 2026-05 披露提示注入在容器内达宿主级 RCE，正是"容器 + Agent"组合的真实风险。

### 容器逃逸的经典路径

1. **内核漏洞**：利用宿主内核任意漏洞（如 CVE-2022-0492 cgroup 逃逸）
2. **挂载滥用**：宿主目录/`/proc` 特殊挂载写入
3. **能力滥用**：容器内保留 `CAP_SYS_ADMIN`/`CAP_NET_ADMIN` 等强能力
4. **setuid/SUID 提权**：未设置 `no-new-privileges`
5. **特权容器**：`--privileged` 等于没有隔离

### 容器安全最佳实践（若只能用容器）

```bash
docker run --rm -it \
  --network none \                              # 无网络
  --cap-drop ALL \                              # 丢弃全部能力
  --security-opt no-new-privileges \            # 禁提权
  --memory 512m --pids-limit 512 \              # 资源配额
  --read-only -v /workspace:/workspace \        # 只读根 + 白名单可写
  --tmpfs /tmp \
  python:3.13 bash
```

> 💡 这条命令本身就是"容器最佳实践模板"：断网、丢能力、禁提权、配额、只读根——面试常问。

## 2. gVisor：用户态内核（L4）

### 2.1 架构三件套

| 组件 | 职责 |
|---|---|
| **Sentry** | 用户态用 Go 重实现 Linux 系统调用接口（~237/350 个），应用 syscall 不触达真实内核——内存管理、文件系统、网络栈、进程管理都在用户态模拟 |
| **Gofer** | 文件系统操作中介，通过 9P 协议与宿主交互——沙箱内文件访问全部经它转发 |
| **seccomp 白名单** | Sentry 自身仅用 53-68 个宿主 syscall（seccomp-bpf 收紧），把 Sentry 的宿主攻击面压到最小 |

### 2.2 三种平台模式

| 模式 | 机制 | 说明 |
|---|---|---|
| Systrap | seccomp-bpf 拦截 | **默认**，无需 KVM，部署门槛最低 |
| KVM | 硬件虚拟化（KVM 平台） | 隔离更强（Sentry 跑在受保护内存），需 `/dev/kvm` |
| ptrace | 追踪拦截 | 遗留模式，最慢，兼容性最好 |

### 2.3 关键数字与权衡

| 指标 | 值 | 说明 |
|---|---|---|
| 启动耗时 | 50-100ms | 接近容器，远快于全 VM |
| syscall 开销 | 2.2-72× | 部分系统调用显著变慢——IO 密集场景需压测 |
| 兼容性 | ~90%（有损） | 部分 syscall 未实现/不完整，运行不兼容程序需先测试 |
| 隔离逻辑 | 攻破 Sentry | 攻破沙箱需 exploit 用户态内核本身，而非打穿宿主内核 |

### 2.4 谁在用

- **ChatGPT Code Interpreter**（K8s 上的 gVisor，见 [06-Jupyter 与 Code-Interpreter](06-Jupyter与Code-Interpreter.md)）
- **Modal**（自定义规则防恶意 syscall）
- **Google Cloud Agent Sandbox**
- 开源：kubernetes-sigs/agent-sandbox、OpenSandbox（gVisor/Kata/Firecracker 多 runtime 可选）

## 3. Kata Containers（L4 的另一支）

- 思路：**容器接口（OCI/Docker 兼容）+ 轻量 VM 内核**——每个容器跑在独立 VM 里
- 与 gVisor 对比：

| 维度 | gVisor | Kata |
|---|---|---|
| 隔离机制 | 用户态 syscall 拦截 | 硬件虚拟化（独立内核） |
| 需 KVM | 否（Systrap 默认） | **是** |
| 隔离强度 | 中强 | 强（更接近微VM） |
| 资源开销 | 低 | 中（每容器一个 VM） |
| 兼容性 | 有损（syscall 模拟） | 高（完整内核） |
| 部署 | 简单（runtime 替换） | 需虚拟化环境 |

- 2026 定位：gVisor 兼容性/运维更亲民；Kata 在"强隔离 + 容器生态兼容"场景（且宿主机有 KVM 时）

## 4. L3-L5 对比总表

| 方案 | 隔离机制 | 启动 | syscall 兼容 | 需 KVM | 代表用户 |
|---|---|---|---|---|---|
| Docker/runc | 共享内核容器 | <1s | 100% | 否 | 可信内部代码 |
| gVisor | 用户态内核 | 50-100ms | ~90% 有损 | 否（Systrap） | ChatGPT、Modal、GCP |
| Kata | 轻量 VM（容器接口） | ~1s | 高 | 是 | 强隔离容器场景 |
| Firecracker | 微VM | ~125ms | 100%（独立内核） | 是 | E2B、AWS Lambda（见 [04](04-Firecracker微虚拟机.md)） |

## 5. 集成姿势（runtime 替换）

```bash
# 把容器运行时切换为 gVisor（现有镜像零改动）
# containerd 配置中加入 runsc 运行时，或：
docker run --runtime=runsc --rm -it python:3.13 bash
```

## 面试速记

1. 容器共享内核 → 内核漏洞即逃逸面；2026 共识不可信代码不用容器。
2. gVisor = Sentry（用户态内核，237/350 syscall）+ Gofer（9P 文件中介）+ seccomp 白名单（53-68 syscall）。
3. Systrap 默认无 KVM 依赖——ChatGPT 选 gVisor 而非 Firecracker：K8s 节点本身是 VM，无 /dev/kvm。
4. gVisor 有 syscall 兼容性损耗（2.2-72× 开销），必须压测；攻破它需打穿用户态内核。
5. Kata = 容器接口 + 轻量 VM；容器最佳实践：断网+丢能力+禁提权+配额+只读根。

---

**下一模块**：[04-Firecracker 微虚拟机](04-Firecracker微虚拟机.md) ｜ **返回总览**：[00-代码执行沙盒总览](00-代码执行沙盒总览.md)

## 参考来源

- [Poking Around ChatGPT's Sandbox（mkarots 逆向分析）](https://mkarots.github.io/blog/chatgpt-sandbox-exploration/)
- [Best platforms for untrusted code execution in 2026（Northflank）](https://northflank.com/blog/best-platforms-for-untrusted-code-execution)
- [AI Agent Sandboxing in 2026（amux）](https://amux.io/guides/ai-agent-sandboxing/)
- [How to sandbox AI agents in 2026（dev.to）](https://dev.to/manveerchawla/how-to-sandbox-ai-agents-in-2026-firecracker-gvisor-runtimes-isolation-strategies-14pk)
- [OpenSandbox: Secure, Fast, and Extensible Sandbox runtime（GitHub）](https://github.com/opensandbox-group/OpenSandbox)
