# Windows 系统基础与演进

> 🏗️ 从 Windows 1.0 到 Windows 11，从 MS-DOS 内核到 NT 架构 —— 理解你每天都在用的操作系统是如何运转的

---

## 📚 目录

1. [Windows 版本演进简史](#1-windows-版本演进简史)
2. [Windows 系统架构概览](#2-windows-系统架构概览)
3. [注册表 Registry](#3-注册表-registry)
4. [Windows 与 Linux/macOS 本质差异](#4-windows-与-linuxmacos-本质差异)
5. [Windows 版本选择建议](#5-windows-版本选择建议)

---

## 1. Windows 版本演进简史

### 1.1 消费级 Windows 演进

| 版本 | 年份 | 内核 | 里程碑意义 |
|------|:--:|------|------|
| Windows 1.0 | 1985 | MS-DOS | 图形界面初尝试 |
| Windows 95 | 1995 | MS-DOS + 9x | 开始菜单、任务栏、即插即用 |
| Windows XP | 2001 | NT 5.1 | 消费/企业统一内核，史上最长寿（13年支持） |
| Windows Vista | 2007 | NT 6.0 | Aero 毛玻璃、UAC 安全机制 |
| Windows 7 | 2009 | NT 6.1 | 被誉为"第二个 XP"，稳定高效 |
| Windows 8/8.1 | 2012 | NT 6.2/6.3 | Metro 界面，触屏优先，争议最大 |
| Windows 10 | 2015 | NT 10.0 | 回归传统+现代融合，WSL 引入 |
| Windows 11 | 2021 | NT 10.0 | 全新 UI、Android 子系统、TPM 2.0 强制要求 |

### 1.2 服务器版本

| 版本 | 对应消费版 | 关键特性 |
|------|:-----:|------|
| Windows Server 2008 R2 | Win7 | IIS 7.5、Hyper-V |
| Windows Server 2012 R2 | Win8.1 | PowerShell DSC |
| Windows Server 2016 | Win10 1607 | Nano Server、容器 |
| Windows Server 2019 | Win10 1809 | Kubernetes 支持 |
| Windows Server 2022 | Win10 21H2 | 安全核心服务器 |

> 💡 **关键认知**：自 Windows 10 起，微软转向"Windows as a Service"（服务化），不再发布大版本号，而是持续通过半年频道的功能更新迭代。

---

## 2. Windows 系统架构概览

### 2.1 内核态 vs 用户态

```text
┌─────────────────────────────────────────────────────┐
│                    用户态 (User Mode)                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────────┐ │
│  │ 普通进程  │ │ 系统服务  │ │ 环境子系统 (Win32/WSL) │ │
│  │ java.exe │ │ svchost  │ │    POSIX/Linux       │ │
│  └──────────┘ └──────────┘ └──────────────────────┘ │
│  ┌──────────────────────────────────────────────────┐│
│  │          子系统 DLL (kernel32.dll)                ││
│  └──────────────────────────────────────────────────┘│
├────────────────────── 系统调用 ──────────────────────┤
│                    内核态 (Kernel Mode)               │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────────┐ │
│  │ 执行体    │ │  内核    │ │   设备驱动程序          │ │
│  │ Executive│ │  Kernel  │ │   Device Drivers      │ │
│  └──────────┘ └──────────┘ └──────────────────────┘ │
│  ┌──────────────────────────────────────────────────┐│
│  │         硬件抽象层 (HAL)                          ││
│  └──────────────────────────────────────────────────┘│
│  ┌──────────────────────────────────────────────────┐│
│  │              硬件 (CPU/内存/磁盘/网络)            ││
│  └──────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

### 2.2 核心组件说明

| 组件 | 层级 | 职责 |
|------|:--:|------|
| **HAL** (硬件抽象层) | 内核态底层 | 屏蔽硬件差异，向上提供统一接口 |
| **Kernel** (内核) | 内核态 | 线程调度、中断处理、多处理器同步 |
| **Executive** (执行体) | 内核态 | 进程管理、内存管理、I/O管理、安全管理 |
| **Win32 子系统** | 用户态 | 传统 Windows 程序的运行环境 |
| **WSL 子系统** | 用户态 | Linux 二进制兼容层 |
| **NTDLL.dll** | 用户态底层 | 用户态到内核态的入口（系统调用网关） |

> 🎯 **核心要点**：Java 程序（java.exe）运行在用户态，JVM 的线程最终映射到内核线程，由 NT Kernel 统一调度。

---

## 3. 注册表 Registry

### 3.1 注册表是什么

注册表是 Windows 的**层次化配置数据库**，相当于 Linux 的 `/etc/` 目录 + `~/.config/` 的集合体，但以树形结构集中存储。

```text
注册表 (regedit.exe)
│
├── HKEY_CLASSES_ROOT (HKCR)     → 文件关联、COM 注册
├── HKEY_CURRENT_USER (HKCU)     → 当前用户配置
├── HKEY_LOCAL_MACHINE (HKLM)    → 本机全局配置 ← 最重要
├── HKEY_USERS (HKU)             → 所有用户配置
└── HKEY_CURRENT_CONFIG (HKCC)   → 当前硬件配置
```

### 3.2 开发者常用注册表路径

| 路径 | 用途 |
|------|------|
| `HKLM\SOFTWARE\JavaSoft\JDK` | 已安装的 JDK 版本信息 |
| `HKLM\SOFTWARE\JavaSoft\Java Runtime Environment` | 当前 JRE 版本 |
| `HKLM\SYSTEM\CurrentControlSet\Services` | 所有 Windows 服务配置 |
| `HKCU\Environment` | 当前用户的环境变量 |
| `HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment` | 系统级环境变量 |
| `HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Run` | 开机自启程序 |

### 3.3 注册表操作方式

```powershell
# PowerShell 读取注册表
Get-ItemProperty -Path "HKLM:\SOFTWARE\JavaSoft\JDK"

# CMD 读取注册表
reg query "HKLM\SOFTWARE\JavaSoft\JDK"

# 命令行设置环境变量（推荐方式，而非直接改注册表）
setx JAVA_HOME "C:\Program Files\Java\jdk-21" /M

# 导入/导出 .reg 文件
reg export "HKLM\SOFTWARE\JavaSoft" C:\backup\java.reg
reg import C:\backup\java.reg
```

> ⚠️ **警告**：直接编辑注册表有风险，修改前建议先导出备份。环境变量优先用 `setx` 或系统设置 GUI 操作。

---

## 4. Windows 与 Linux/macOS 本质差异

### 4.1 核心差异对照表

| 维度 | Windows | Linux | macOS |
|------|---------|-------|-------|
| **内核** | NT 内核 | Linux 内核 | XNU (Mach + BSD) |
| **文件路径** | `C:\Users\xxx\` 反斜杠 | `/home/xxx/` 正斜杠 | `/Users/xxx/` 正斜杠 |
| **路径分隔符** | `;` | `:` | `:` |
| **换行符** | `\r\n` (CRLF) | `\n` (LF) | `\n` (LF) 旧版 `\r` |
| **文件系统** | NTFS / FAT32 / exFAT | ext4 / XFS / Btrfs | APFS / HFS+ |
| **可执行文件** | `.exe` `.bat` `.ps1` | ELF (无扩展名) | Mach-O (无扩展名) |
| **大小写** | 不敏感（保留大小写） | 敏感 | 默认不敏感（APFS 可选） |
| **包管理** | winget / choco / scoop | apt / yum / pacman | Homebrew |
| **Shell** | cmd / PowerShell | bash / zsh / fish | zsh (默认) / bash |
| **配置存储** | 注册表 (集中) | 文件 (分散 `/etc/`) | 文件 + plist |
| **进程/线程** | 进程=容器, 线程=执行单元 | 同样 | 同样 |
| **动态库** | `.dll` | `.so` | `.dylib` |
| **服务管理** | `services.msc` / `sc` | `systemd` / `init.d` | `launchd` |
| **日志系统** | 事件查看器 (EventLog) | syslog / journald | unified logging |

### 4.2 对 Java 开发者的影响

| 场景 | 注意事项 |
|------|------|
| **跨平台路径** | 用 `File.separator` 或 `Paths.get()` 替代硬编码 `\` 或 `/` |
| **换行符问题** | Git 配置 `core.autocrlf=true` (Windows) / `input` (Linux) |
| **编码** | Windows 默认 GBK/GB2312，Java 默认 UTF-8，注意 `-Dfile.encoding=UTF-8` |
| **端口绑定** | Windows 端口释放有 TIME_WAIT 延迟，重启 `net stop winnat && net start winnat` |
| **进程管理** | Windows 没有 `kill -9`，用 `taskkill /F /PID` |
| **文件锁** | Windows 上文件被占用时无法删除，Linux 上可以删除正在使用的文件 |

> 🎯 **核心要点**：Windows 和 Linux 最大的思维差异在于 —— Windows 是 **API 驱动**（Win32 API），Linux 是 **文件驱动**（一切皆文件）。

---

## 5. Windows 版本选择建议

### 5.1 Java 开发者推荐

| 场景 | 推荐版本 | 理由 |
|------|---------|------|
| 主力开发机 | Windows 11 Pro | WSL2 增强、最新 PowerShell、虚拟化支持 |
| 公司办公 | Windows 10/11 Enterprise | 域控管理、组策略、BitLocker |
| 旧硬件 | Windows 10 LTSC | 长周期支持、无强制更新、轻量化 |
| 服务器 | Windows Server 2022 | 长期服务频道、无桌面可选 |

### 5.2 Java 版本与 Windows 兼容性

| JDK 版本 | 最低 Windows 版本 | 状态 |
|:-------:|-------------------|:----:|
| JDK 8 | Windows Vista | LTS，广泛使用 |
| JDK 11 | Windows 8.1 | LTS |
| JDK 17 | Windows 10 | LTS，推荐 |
| JDK 21 | Windows 10 / Server 2019 | 当前 LTS |
| JDK 24 | Windows 10 / Server 2019 | 最新特性版 |

> 💡 **建议**：2026 年新项目优先选择 JDK 21 LTS，老旧项目维护 JDK 8/11。

---

**下一模块**：[02-Windows命令行全指南](./02-Windows命令行全指南.md) ｜ **返回总览**：[00-Windows知识体系总览](./00-Windows知识体系总览.md)

---

*创建于：2026年7月*
