# Linux 隔离原语与沙箱技术栈

> 一切沙箱的底层地基：namespace / cgroup / seccomp / LSM 是内核提供的隔离积木，bubblewrap / NsJail 把它们拼装成可用沙箱。不理解这层，就无法真正评估 gVisor、Firecracker 在"隔离什么"。

## 1. 内核隔离原语总览

Linux 沙箱的全部能力由内核提供的四类原语组合而成。现代沙箱（Chromium、OpenAI Codex、Claude Code、runok）都是"原语组合拳"，**没有单一原语能独立构成安全沙箱**。

| 原语 | 隔离对象 | 内核版本 | 权限要求 | 一句话类比 |
|---|---|---|---|---|
| namespace（7 种） | 进程的"视图"：PID/网络/挂载/UTS/IPC/用户/时间 | 2.4.19 ~ 5.6 | user ns 之外均可非特权 | "看不见" |
| cgroup v2 | 资源配额：CPU/内存/IO/PID 数/swap | 4.15+ | 需 root 配置 | "用不完"（水表+限流阀） |
| seccomp-bpf | 系统调用门禁（白/黑名单） | 3.5+ | 任意进程（配 `NO_NEW_PRIVS`） | "不许调" |
| LSM（AppArmor/SELinux/Landlock） | 路径/标签级强制访问控制 | 2.6.36+ / 5.13+ | 多数需 root；**Landlock 任何用户** | "不许碰" |
| chroot / pivot_root | 根文件系统视图 | 老牌 | chroot 非特权 | 伪隔离——**禁止单独用于安全** |

### 为什么必须组合

| 单原语 | 能挡 | 挡不住 |
|---|---|---|
| 只有 namespace | 看不见 | 看不见≠够不着：沙箱内进程仍可发信号/连 socket/耗资源（namespace 不限制能力） |
| 只有 seccomp | 恶意系统调用 | 合法系统调用对敏感路径的读写、资源耗尽 |
| 只有 Landlock | 路径读写 | 网络访问、资源耗尽、ptrace 横向 |
| 只有 cgroup | 资源风暴 | 恶意读写、网络外泄 |

> 🎯 核心要点：namespace 管"看不见"，cgroup 管"用不完"，seccomp 管"不许调"，LSM 管"不许碰"——四者覆盖不同威胁面，缺一层就多一个逃逸口。

## 2. namespace：进程视图隔离

### 2.1 七种 namespace 明细

| namespace | 隔离内容 | 创建标志 | 沙箱里的典型用途 |
|---|---|---|---|
| mount | 挂载点列表（文件系统视图） | `CLONE_NEWNS` | 只读根、隐藏敏感路径、bwrap 的核心能力 |
| PID | 进程编号视图 | `CLONE_NEWPID` | 沙箱内看不到宿主进程，防 ptrace/信号横向 |
| net | 网络栈（接口、路由、iptables） | `CLONE_NEWNET` | 独立网卡；断网、白名单代理、出口过滤 |
| UTS | hostname / domainname | `CLONE_NEWUTS` | 环境标识隔离 |
| IPC | System V IPC / POSIX mqueue / 共享内存 | `CLONE_NEWIPC` | 防跨沙箱 IPC 通道 |
| user | UID/GID 映射（ns 内 root ≠ 宿主 root） | `CLONE_NEWUSER` | **非特权沙箱的地基** |
| time | 时钟偏移 | `CLONE_NEWTIME` | 防时序侧信道、防许可时间绕过（5.6+） |

### 2.2 user namespace：非特权沙箱的钥匙

- 普通用户 `unshare -Ur` 后在 ns 内成为 root（映射到宿主普通 UID），从而获得创建其他 namespace 的资格——这是 bubblewrap、Firejail、Chromium 沙箱"非特权运行"的根本原因
- 攻击面代价：部分加固发行版关闭 `kernel.unprivileged_userns_clone`，这些工具全部失效；但开放 userns 也被攻击者用于绕过部分 LSM 策略——2026 年仍是未决权衡

```bash
# 非特权创建完整隔离环境（无需 sudo）
unshare --mount --pid --net --user --uts --ipc --fork bash
# 常用调试：unshare -r -n bash  在 user+net ns 内以 root 身份
```

### 2.3 namespace 的局限

- namespace 只是"视图欺骗"，**不限制能力**：有权限时仍可对宿主对象操作（共享 `/proc`、内核对象）
- 必须与 seccomp（能力）、cgroup（资源）、LSM（路径）配合才有安全意义
- 历史教训：早期 Docker 的 `/proc` 视图不完整、`pid 1` 特权问题都曾是逃逸入口（如 CVE-2022-0492 cgroup 逃逸链）

## 3. cgroup v2：资源配额

cgroup 统计并限制进程组对资源的使用。v2（4.15+，主流发行版默认）把 v1 的分层级控制器统一为单一层级树。

| 控制文件 | 作用 | 沙箱生产值参考 |
|---|---|---|
| `memory.max` | 内存硬上限（超限 OOM kill） | 单会话 512MB ~ 4GB |
| `memory.high` | 内存软上限（触发回收而非 kill） | 硬上限的 80% |
| `cpu.max` | `配额/周期`（如 `50000/100000` = 50% CPU） | JupyterHub 常用 `"50%"` |
| `pids.max` | 进程数上限 | OpenSandbox 设 512（防 fork 炸弹） |
| `io.max` | IO 带宽/IOPS 上限 | 按磁盘规格 |
| `memory.swap.max` | swap 上限 | 0（防交换风暴） |

```bash
# 为沙箱会话创建 cgroup 并设限（v2 路径示例）
mkdir -p /sys/fs/cgroup/agent-sandboxes/session-001
echo 536870912 > /sys/fs/cgroup/agent-sandboxes/session-001/memory.max   # 512MB
echo 512 > /sys/fs/cgroup/agent-sandboxes/session-001/pids.max
echo $PID > /sys/fs/cgroup/agent-sandboxes/session-001/cgroup.procs
```

> ⚠️ 生产共识：**内存限制是第一位的**——OOM 只应杀死该会话的进程，绝不波及宿主；`pids.max` 不设，一行 fork 炸弹（`:(){ :|:& };:`）打满全机 PID 表。

## 4. seccomp-bpf：系统调用门禁

seccomp 限制进程可用的系统调用集合，是"能力最小化"的关键一环。

| 模式 | 行为 | 适用 |
|---|---|---|
| strict（Mode 1） | 只允许 `read`/`write`/`exit`/`sigreturn` | 容器 init 等极端最小化 |
| filter（seccomp-bpf） | 每次系统调用执行 BPF 程序裁决（允许/拒绝/追踪） | **主流用法**（Docker 默认 profile、Chromium、Codex） |

### 安全前提：`PR_SET_NO_NEW_PRIVS`

- 必须在加载过滤器前设置，否则过滤规则可被 setuid 二进制提权绕过
- 副作用（正面）：同时阻止沙箱内 setuid 提权——Codex/Claude Code 均依赖此语义

### 典型过滤规则

| 策略 | 实现 | 用途 |
|---|---|---|
| 禁危险 syscall | 拒绝 `ptrace`/`mount`/`kexec_load`/`bpf` | 防逃逸横向、防改挂载 |
| 网络收紧 | `socket(2)` 仅放行 `AF_UNIX`，其余 `EPERM` | ChatGPT Code Interpreter 的"断网"实现 |
| 防终端注入 | 拒绝 `ioctl(TIOCSTI)` | 防沙箱内程序向父终端灌指令 |
| 防跨进程内存 | 拒绝 `process_vm_readv/writev` | 防内存读取 |

```c
// seccomp-bpf 核心逻辑示意（伪代码）
if (nr == __NR_socket && args[0] != AF_UNIX) return EPERM;
if (nr == __NR_ptrace || nr == __NR_mount)   return EPERM;
return ALLOW;
```

### 维护难点

- **白名单必须穷举**沙箱内代码的全部系统调用（glibc 动态链接、JVM 用到的很多）——漏列即运行故障
- 生产做法：黑名单兜底 + 核心白名单，或以 Docker 官方 profile 为起点
- 2026 趋势：内核 seccomp user-notify 允许用户态裁决特定调用，但 gVisor 不支持（Modal 对 gVisor 沙箱改用 ptrace 拦截的原因之一）

## 5. LSM：AppArmor / SELinux / Landlock

LSM 框架提供与 DAC 独立的强制访问控制（MAC）。

| LSM | 控制对象 | 谁能配置 | 复杂度 | 典型用户 |
|---|---|---|---|---|
| AppArmor | 路径 + 网络端口 + syscall | 管理员 | 低（路径即规则） | Ubuntu/SUSE 默认、Docker profile |
| SELinux | 类型/角色/用户标签 | 管理员 | 高（标签体系） | RHEL/CentOS 系 |
| Landlock | 路径访问规则 | **任何进程** | 中（ABI 敏感） | OpenAI Codex、runok、fence |

### AppArmor 要点

- 按程序 profile 声明"可读哪些路径、可监听哪些端口、可调哪些 syscall"
- **complain 模式**：只记日志不阻断——调 profile 利器（先 complain 收日志 → 转 enforce）
- 局限：管路径不管网络出站内容，DNS 外泄等信道不在覆盖内

```text
# AppArmor profile 片段：沙箱只读根 + 可写 /workspace
/workspace/** rw,
/usr/**, /bin/**, /etc/** r,
/root/** r,
```

### Landlock 深入（5.13+，沙箱新宠）

- **与 AppArmor/SELinux 的本质区别**：面向"可能不可信的进程"，任何未特权应用都能给自己施加规则——与 seccomp 同哲学
- 规则**加性**：只能不断收紧，不能吊销基线权限——"隐藏/拒绝特定路径"要交给 mount 层（bwrap 只读重挂或空 tmpfs 覆盖）
- **ABI 版本敏感**：5.13=A1 … 5.19=ABI4；生产探测 ABI 按最低内核 best-effort 降级（runok 用 ABI V5；fence 自动探测取最大特性集）

```c
// Landlock 核心流程（伪代码）：/ 只读，/workspace 可写
int rs = landlock_create_ruleset(handled_access: FILE_READ|FILE_WRITE);
landlock_add_rule(rs, PATH_BENEATH, { .path = "/",         .access = FILE_READ });
landlock_add_rule(rs, PATH_BENEATH, { .path = "/workspace", .access = FILE_READ|FILE_WRITE });
landlock_restrict_self(rs, 0);   // 绑定当前进程与所有子进程
```

## 6. 组合工具：bubblewrap / NsJail / Firejail

内核原语过于底层，生产直接用这些封装工具。

| 工具 | 基于 | 特点 | 典型用户 |
|---|---|---|---|
| bubblewrap（bwrap） | mount+user namespace | 极简单二进制、无 SUID、即用即弃、非特权 | Flatpak、Steam、Claude Code、Codex |
| NsJail | namespace+cgroup+seccomp+rlimits | 谷歌开源、配置文件化、**面向服务端** | CTF 评测、在线判题、代码执行服务 |
| Firejail | namespace+seccomp+LSM | 配置丰富、桌面应用沙箱 | 桌面 Linux |

### bwrap 典型用法

```bash
# 只读根 + 仅 /workspace 可写 + 空 tmpfs + 无网络
bwrap --ro-bind / / \
      --bind /workspace /workspace \
      --tmpfs /tmp --tmpfs /home \
      --unshare-all --share-net \
      bash -c "whoami && ls /workspace"
```

### NsJail 配置示例（服务端代码执行）

```text
# nsjail.cfg 片段
name: "py-sandbox"
mount: { src: "/", dst: "/", rw: false }        # 根只读
mount: { src: "/workspace", dst: "/workspace" }  # 工作区可写
cgroup_mem_max: 536870912                        # 512MB 内存
rlimit_nofile: 32                                # 文件描述符上限
seccomp_policy_file: "/etc/nsjail/seccomp.json"
time_limit: 30
```

### 两段式执行模型（沙箱原理的核心细节）

```text
第 1 段：bwrap 建立 mount/user/pid/net namespace（只读根、可写区、隐藏敏感路径）
第 2 段：在 namespace 内部再调用一次自己 → 应用 Landlock 规则 + seccomp 过滤器 → exec 目标命令
```

- 为什么必须两段：bwrap 的 namespace 构建发生在 exec 前；Landlock/seccomp 必须由"将要受限的进程"自己施加——所以工具在沙箱内 re-exec 自己执行后置规则
- **bwrap 不叠 seccomp 的后果**：沙箱内进程仍可 `ptrace`、`socket(AF_UNIX)`、`mount`——namespace 挡不住"有权限的系统调用"

## 7. 生产组合范式（各大厂真实配置）

| 方案 | 组合 | 覆盖威胁 |
|---|---|---|
| Chromium | namespace + seccomp-bpf | 渲染进程漏洞利用（浏览器 20 年打磨的参考） |
| OpenAI Codex（Linux 新版） | bwrap（`--ro-bind / /` 只读根 + 可写工作区 + `.git`/`.codex` 保护路径重挂只读 + 禁符号链接）+ seccomp（禁新 socket）+ `NO_NEW_PRIVS` + 网络代理桥（net ns 隔离 + unix socket TCP 桥） | 文件越界、网络外泄、提权 |
| OpenAI Codex（macOS） | Seatbelt（`/usr/bin/sandbox-exec`）运行时生成 profile | 文件/网络/系统能力 |
| OpenAI Codex（Windows） | 受限 token + SID/ACL + Job Object + 防火墙规则（专用账户 `CodexSandboxOffline/Online`） | 文件、网络、资源 |
| runok | bwrap + Landlock（ABI V5）+ seccomp（仅网络） | 分层防御参考实现 |
| Claude Code（Linux/WSL2） | bubblewrap 沙箱化 Bash 工具（仅 Bash 及子进程） | 命令级隔离 |
| ChatGPT Code Interpreter | gVisor 用户态内核（见 [03-容器与 gVisor](03-容器与gVisor.md)） | 完整内核级隔离 |

### 设计清单（从范式提炼）

1. **先只读根，再开可写白名单**——默认拒绝哲学（而非先全开再禁）
2. **保护路径递归生效**：`.git`、凭据目录在可写根内也必须重挂只读（Codex 对 `.git`/`.codex` 的强制）
3. **网络一律默认拒绝**，需要时白名单 + 代理桥接
4. **符号链接是常见逃逸载体**：可写区禁创建指向区外的 symlink（Claude Code CVE-2026-39861 正是此例）
5. **资源配额与沙箱生命周期绑定**：cgroup 随会话创建/销毁
6. **凭据不落盘**：密钥经网络边界注入（Vercel/Cloudflare 零信任 secrets 模式）

## 8. 易错点

- **chroot 当安全边界**：chroot 可被 chdir 逃逸（内核对象可达），现代沙箱一律用 pivot_root 或 mount ns
- **只用 seccomp 黑名单**：`io_uring` 等新 syscall/变体悄然扩大攻击面，黑名单永远追不完
- **忘了 `NO_NEW_PRIVS`**：seccomp 形同虚设且 setuid 提权路径敞开
- **bwrap 不叠 seccomp**：`ptrace`/`socket`/`mount` 仍可用
- **Landlock 规则只增不减**：想"隐藏"路径必须靠 mount 层，别指望 Landlock 吊销
- **cgroup 不限制进程数**：`pids.max` 不设，一行 fork 炸弹打满 PID 表
- **user namespace 被关闭的发行版**：bwrap/Firejail 全部失效，需换 root 启动或特权模式
- **单层工具当完整沙箱**：只有 bwrap、只有 NsJail 默认配置，都留大口子

## 面试速记

1. 七种 namespace 管"视图"，cgroup 管"资源"，seccomp 管"系统调用"，LSM 管"路径访问"——四类原语组合才是沙箱。
2. user namespace 是"非特权沙箱"的钥匙；namespace 只是视图欺骗，不限制能力。
3. seccomp 必须配 `NO_NEW_PRIVS`；白名单穷举是维护痛点，生产用黑名单兜底+白名单核心。
4. Landlock 是 5.13+ 唯一非特权路径级 MAC，规则加性、不能吊销、ABI 敏感。
5. 大厂范式六条：默认拒绝、保护路径递归只读、网络代理桥、禁符号链接、配额绑定会话、凭据不落盘。

---

**下一模块**：[03-容器隔离与 gVisor](03-容器与gVisor.md) ｜ **返回总览**：[00-代码执行沙盒总览](00-代码执行沙盒总览.md)

## 参考来源

- [Linux 沙箱技术详解：从原理到实践（geek-blogs）](https://geek-blogs.com/blog/sandboxing-in-linux/)
- [The Linux Sandbox（Chromium Docs）](https://chromium.cpp.hybrid-analysis.googlesource.com/ios-chromium-mirror/+/8afe74e67cf4f8b004695e8d0bfce179c7305220/sandbox/linux/)
- [Landlock, Seccomp, and eBPF（fence DeepWiki）](https://deepwiki.com/fencesandbox/fence/4.3-landlock-seccomp-and-ebpf)
- [OpenAI Codex linux-sandbox landlock.rs（GitHub 源码）](https://raw.githubusercontent.com/openai/codex/f802f0a3911655ac0e2876fceedf8ad833431df3/codex-rs/linux-sandbox/src/landlock.rs)
- [runok Architecture: Linux Sandbox](https://runok.fohte.net/architecture/sandbox/linux/)
- [Complete tutorial on sandboxing in Linux with Firejail（mundobytes）](https://mundobytes.com/en/Complete-tutorial-on-sandboxing-in-Linux-with-Firejail-and-more/)
