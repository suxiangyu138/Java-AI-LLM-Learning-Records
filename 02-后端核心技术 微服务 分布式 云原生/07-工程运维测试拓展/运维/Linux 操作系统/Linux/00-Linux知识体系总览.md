# 00 - Linux 知识体系总览

> Linux 是 Java 后端的生产运行环境——从文件系统到进程管理、从网络配置到 JVM 线上排查，12 篇文档覆盖服务端生存必备技能（基于 2026-08 主流发行版：RHEL 10 / Rocky 10 / Ubuntu 26.04 LTS）。Shell 脚本编程为独立体系，见[Shell 知识体系总览](../Shell/00-Shell知识体系总览.md)。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)

## 1. 知识体系导图

```text
Linux 知识体系（12 篇）
│
├── 🐧 核心基础（01-05）
│   ├── 01-Linux操作系统基础与文件系统   # 内核架构/FHS/inode/链接/启动流程
│   ├── 02-Linux常用命令                # 文件/文本/权限/进程/网络 + 后端场景
│   ├── 03-Linux系统管理                # 磁盘/进程/systemd/包管理/用户/环境变量
│   ├── 04-Linux网络与安全              # 网络配置/防火墙/SSH/入侵排查/SELinux
│   └── 05-Linux服务管理                # systemd/cron/FTP/NFS/Samba/Apache
│
├── 📋 运维实战（06-11）
│   ├── 06-命令行速查手册               # 高频命令快速索引（按场景分类）
│   ├── 07-服务器运维基础               # 硬件/发行版选型/初始化/SSH/FinalShell
│   ├── 08-Linux权限与用户管理深度       # rwx/SUID/ACL/sudo/capabilities
│   ├── 09-JVM排查工具详解             # jps/jstack/jstat/jmap/Arthas
│   ├── 10-线上问题排查流程与速查表      # CPU/内存/死锁/慢接口/Full GC 方法论
│   └── 11-日志管理                    # 日志框架/Logback/最佳实践/切割策略
│
└── 📌 00-Linux知识体系总览             # ← 本文件
```

> 🔗 **关联体系**：[Shell 脚本编程](../Shell/00-Shell知识体系总览.md)（独立体系，6 篇）——自动化运维脚本、grep/sed/awk 文本处理。

## 2. 模块导航

| # | 模块 | 核心内容 | 级别 |
|:---:|------|---------|:---:|
| 01 | [Linux 操作系统基础与文件系统](01-Linux操作系统基础与文件系统.md) | 内核架构、FHS、inode、链接、启动流程 | ⭐⭐⭐ |
| 02 | [Linux 常用命令](02-Linux-常用命令.md) | 文件/文本/权限/进程/网络/打包 + 后端场景 | ⭐ |
| 03 | [Linux 系统管理](03-Linux-系统管理.md) | 磁盘/进程/systemd/包管理/用户/内核参数 | ⭐⭐ |
| 04 | [Linux 网络与安全](04-Linux-网络与安全.md) | 网络配置/防火墙/SSH/隧道/入侵排查/SELinux | ⭐⭐⭐ |
| 05 | [Linux 服务管理](05-Linux-服务管理.md) | systemd service/cron/FTP/NFS/Samba/Apache | ⭐⭐ |
| 06 | [命令行速查手册](06-命令行速查手册.md) | 按场景分类的高频命令快速索引 | ⭐ |
| 07 | [服务器运维基础](07-服务器运维基础.md) | 硬件选型/发行版选型/系统初始化/SSH/FinalShell | ⭐⭐ |
| 08 | [Linux 权限与用户管理深度](08-Linux权限与用户管理深度.md) | rwx/SUID/SGID/Sticky/ACL/sudo/capabilities | ⭐⭐⭐⭐ |
| 09 | [JVM 排查工具详解](09-JVM排查工具详解.md) | jps/jstack/jstat/jmap/MAT/jinfo/Arthas | ⭐⭐⭐⭐ |
| 10 | [线上问题排查流程与速查表](10-线上问题排查流程与速查表.md) | CPU/内存/死锁/慢接口/Full GC 排查方法论 | ⭐⭐⭐⭐ |
| 11 | [日志管理](11-日志管理.md) | 日志框架/Logback 配置/最佳实践/切割策略 | ⭐⭐ |

## 3. 学习路线推荐

### 🟢 L1：能在 Linux 上部署 Java 应用（半天）

```
01-操作系统基础 → 02-常用命令 → 05-服务管理 → 07-服务器运维基础
产出：能在 Linux 上部署 SpringBoot jar 包、查看日志、启停服务
```

### 🔵 L2：运维与排错（1-2 天）

```
03-系统管理 → 04-网络与安全 → 06-命令行速查 → 09-JVM排查工具 → 10-排查流程
产出：能独立排查 CPU 飙升、内存溢出、磁盘满、网络不通、接口变慢等问题
```

### 🟣 L3：权限深度与安全加固（半天）

```
08-权限与用户管理深度 + 04-网络与安全（安全部分）
产出：能设计生产环境的权限模型，配置最小权限原则
```

### 🟠 L4：日志治理与监控体系（半天）

```
11-日志管理 + 10-排查流程（速查表）
产出：能设计日志规范、配置 Logback、建立线上排查 SOP
```

> 💡 需要编写自动化运维脚本（部署/备份/日志分析）？前往关联体系 [Shell 脚本编程](../Shell/00-Shell知识体系总览.md)。

## 4. 核心概念速查

### 4.1 发行版现状（2026-08）

| 发行版 | 现状 | 定位 |
|--------|------|------|
| RHEL 10.2（2026-05） | 企业级旗舰，内核 6.12 / systemd 257，需订阅 | 金融/合规场景 |
| Rocky Linux 10.2（2026-05） | RHEL 1:1 重建，免费，10 年支持 | **企业生产默认推荐** |
| AlmaLinux 10.2（2026-05） | ABI 兼容 + ELevate 原地升级工具 | 迁移友好的替代 |
| Ubuntu 26.04 LTS（2026-04） | 云原生/AI 工作负载首选，5 年 LTS | 云实例/AI 推理 |
| CentOS 7/8 | **已 EOL**（2024-06 / 2021-12） | 必须迁移至 Rocky/Alma 10 |

### 4.2 高频概念速查

| 概念 | 一句话说明 | 所在模块 |
|------|-----------|---------|
| FHS | 文件系统层次标准，规定目录职责（/etc 配置、/var 数据、/opt 应用） | 01 |
| inode | 文件元数据索引节点，ls -i 查看；df -i 监控 inode 耗尽 | 01 |
| 软链接 vs 硬链接 | 软链是指针可跨文件系统；硬链共享 inode 不可跨分区 | 01 |
| systemd | PID=1 的初始化系统，systemctl 管理服务，Type=simple/forking | 03/05 |
| 权限模型 | rwx 位 + umask 掩码 + SUID/SGID/Sticky 特殊位 + ACL 扩展 | 08 |
| sudo | 授权提权机制（visudo 配置），比直接 root 安全 | 08 |
| firewalld | RHEL 系默认防火墙（zone 模型），nftables 为后端 | 04 |
| SELinux | 强制访问控制（MAC），RHEL 系默认 Enforcing | 04 |
| jstack 线程状态 | RUNNABLE/BLOCKED/WAITING/TIMED_WAITING 对应不同问题 | 09 |
| jmap -dump | 导出堆快照（会 STW！线上慎用），配 MAT 分析泄漏 | 09 |
| Arthas | 在线诊断神器：watch/trace/jad/thread 免重启定位 | 09 |
| Logback | Spring Boot 默认日志框架，AsyncAppender 提升性能 | 11 |

> 🎯 **核心要点**：本体系按「能用 → 能查 → 能排 → 能治」四级能力组织——01-05 打底、06-11 上生产；2026 年生产环境首选 **Rocky Linux 10**（或 Ubuntu 26.04 LTS），线上问题排查直接跳到 09/10 按速查表走流程。Shell 脚本自动化见[关联体系](../Shell/00-Shell知识体系总览.md)。

---

**下一模块**：[01-Linux 操作系统基础与文件系统](01-Linux操作系统基础与文件系统.md)

**【参考来源】**
- Rocky Linux 10.2 / AlmaLinux 10.2 / RHEL 10.2 发布信息（2026-05）：https://computingforgeeks.com/rocky-almalinux-rhel-comparison/
- Ubuntu 26.04 LTS：https://distrowatch.com/
