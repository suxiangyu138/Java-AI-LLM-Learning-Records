# CentOS 知识体系（服务端实战版）

> 🔴 RHEL 生态、yum/dnf 包管理、SELinux 安全、firewalld 防火墙、生产环境部署 —— 面向 Java 服务端的 CentOS 完整知识体系

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [CentOS vs Ubuntu 速查](#5-centos-vs-ubuntu-速查)

---

## 1. 知识体系导图

```
CentOS 知识体系全景
│
├── 🔴 基础认知层
│   └── 01-入门与包管理          → RHEL版图、dnf/rpm、EPEL、镜像加速、Java工具链
│
├── 🔒 安全核心层
│   ├── 02-SELinux与安全加固     → MAC强制访问、安全上下文、audit2allow、5大Java实战
│   └── 03-firewalld防火墙       → Zone体系、四层规则、Rich Rule、NAT转发
│
└── 🚀 生产实践层
    └── 04-生产环境部署实战       → 内核调优、systemd深度定制、部署SOP、故障排查
```

---

## 2. 模块导航

| 序号 | 模块名称 | 核心内容 | 适合人群 |
|:---:|---------|---------|:-----:|
| 01 | [入门与包管理](./01-CentOS入门与包管理.md) | RHEL版图、dnf全手册、rpm底层、EPEL/第三方源、JDK安装 | 初学者 |
| 02 | [SELinux与安全加固](./02-CentOS-SELinux与安全加固.md) | MAC原理、安全上下文、排障三板斧、audit2allow、Java5大场景 | 中高级运维 |
| 03 | [firewalld 防火墙](./03-CentOS-firewalld防火墙.md) | Zone区域、service/port/rich-rule/direct四层、NAT、生产配置 | 中高级运维 |
| 04 | [生产环境部署实战](./04-CentOS生产环境部署实战.md) | 内核sysctl、systemd深度定制、Spring Boot部署、基线检查、故障排查 | 高级运维/架构师 |

---

## 3. 学习路线推荐

### 🟢 快速上手（半天）

```
01-入门与包管理  →  会装软件、配镜像源、日常运维
```

> 💡 **目标**：能在 CentOS 上安装 JDK、Docker、Nginx，会用 dnf 管理软件包。

### 🟡 安全加固（1 天）

```
01-入门  →  02-SELinux  →  03-firewalld
```

> 💡 **目标**：理解 SELinux 排障流程，能用 firewalld 配置生产级防火墙规则。不关 SELinux，不用 iptables。

### 🔴 生产部署（2 天）

```
01 → 02 → 03 → 04-生产环境部署
```

> 💡 **目标**：从零搭建 Java 微服务生产环境，内核调优 + systemd 定制 + 部署脚本 + 监控基线。

---

## 4. 核心概念速查

| 概念 | 全称/说明 | 用途 | 详见模块 |
|------|---------|------|:-----:|
| **yum** | Yellowdog Updater Modified | CentOS 7 包管理器（已过时） | 01 |
| **dnf** | Dandified YUM | CentOS 8+ 包管理器（替代 yum） | 01 |
| **rpm** | Red Hat Package Manager | 底层包工具（查询/离线安装） | 01 |
| **EPEL** | Extra Packages for Enterprise Linux | RHEL/CentOS 扩展软件源（必装） | 01 |
| **SELinux** | Security-Enhanced Linux | MAC 强制访问控制 | 02 |
| **Enforcing** | SELinux 强制执行模式 | 生产环境必须开启 | 02 |
| **Permissive** | SELinux 警告模式 | 仅调试用，不要永久设 | 02 |
| **安全上下文** | Security Context (User:Role:Type) | SELinux 的权限标签 | 02 |
| **audit2allow** | - | SELinux 日志 → 生成放行策略 | 02 |
| **restorecon** | - | 恢复文件默认 SELinux 上下文 | 02 |
| **firewalld** | - | CentOS 7+ 默认防火墙守护进程 | 03 |
| **Zone** | firewalld 信任区域 | 按网络环境分级管控 | 03 |
| **Rich Rule** | firewalld 高级规则 | 限制来源IP、限频、日志 | 03 |
| **Direct Rule** | firewalld 底层直连规则 | 直接操作 nftables/iptables | 03 |
| **sysctl** | System Control | 内核参数调优 | 04 |
| **systemd** | System Daemon | 服务管理、开机启动 | 04 |

---

## 5. CentOS vs Ubuntu 速查

| 操作 | CentOS | Ubuntu |
|------|--------|--------|
| 更新索引 | `dnf check-update` | `apt update` |
| 安装软件 | `dnf install pkg` | `apt install pkg` |
| 搜索软件 | `dnf search keyword` | `apt search keyword` |
| 删除软件 | `dnf remove pkg` | `apt remove pkg` |
| 已安装列表 | `rpm -qa` | `apt list --installed` |
| 安装 JDK 21 | `dnf install java-21-openjdk-devel` | `apt install openjdk-21-jdk` |
| 防火墙 | `firewall-cmd --add-port=8080/tcp` | `ufw allow 8080` |
| 安全机制 | SELinux (Enforcing) | AppArmor |
| SELinux 检查 | `getenforce` | `aa-status` |
| 服务管理 | `systemctl` (两者相同) | `systemctl` |
| 查看版本 | `cat /etc/redhat-release` | `lsb_release -a` |

---

> 🎯 **开始学习**：从 [01-入门与包管理](./01-CentOS入门与包管理.md) 开始，先搞定软件安装和镜像源配置，再深入 SELinux 和防火墙。

---

*创建于：2026年7月*
