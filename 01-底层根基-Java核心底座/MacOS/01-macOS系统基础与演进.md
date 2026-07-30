# macOS 系统基础与演进

> 🏗️ 从 Mac OS X 10.0 到 macOS 15 Sequoia，从 PowerPC 到 Apple Silicon —— 理解 Darwin 内核、APFS 文件系统和 macOS 独特的架构哲学

---

## 📚 目录

1. [macOS 版本演进简史](#1-macos-版本演进简史)
2. [Darwin 内核架构](#2-darwin-内核架构)
3. [硬件架构迁移：PowerPC → Intel → Apple Silicon](#3-硬件架构迁移powerpc--intel--apple-silicon)
4. [macOS 与 Linux / Windows 本质差异](#4-macos-与-linux--windows-本质差异)
5. [macOS 核心设计哲学](#5-macos-核心设计哲学)

---

## 1. macOS 版本演进简史

### 1.1 完整版本时间线

| 版本 | 代号 | 年份 | 内核 | 里程碑意义 |
|------|------|:--:|------|------|
| Mac OS X 10.0 | Cheetah | 2001 | Darwin 1.3 | 首个正式版，Aqua 界面诞生 |
| Mac OS X 10.4 | Tiger | 2005 | Darwin 8.x | Spotlight 搜索、Rosetta v1 (PPC→Intel) |
| Mac OS X 10.6 | Snow Leopard | 2009 | Darwin 10.x | "零新功能"，纯优化；Mac App Store 引入 |
| OS X 10.8 | Mountain Lion | 2012 | Darwin 12.x | Gatekeeper 引入、通知中心 |
| OS X 10.10 | Yosemite | 2014 | Darwin 14.x | 扁平化设计，Handoff/Continuity |
| macOS 10.12 | Sierra | 2016 | Darwin 16.x | Siri 引入 Mac，APFS 预览 |
| macOS 10.14 | Mojave | 2018 | Darwin 18.x | 暗色模式，最后支持 32 位应用 |
| macOS 10.15 | Catalina | 2019 | Darwin 19.x | **32 位彻底弃用**，APFS 默认 |
| macOS 11 | Big Sur | 2020 | Darwin 20.x | **大版本号 11**，Apple Silicon 原生支持 |
| macOS 12 | Monterey | 2021 | Darwin 21.x | Shortcuts 移植、Universal Control |
| macOS 13 | Ventura | 2022 | Darwin 22.x | Stage Manager、Continuity Camera |
| macOS 14 | Sonoma | 2023 | Darwin 23.x | 桌面小组件、游戏模式 |
| macOS 15 | Sequoia | 2024 | Darwin 24.x | iPhone Mirroring、Apple Intelligence |

> 💡 **命名变化**：Mac OS X → OS X (2012) → macOS (2016)。版本号从 10.x 跨越到 11 (2020)，标志着 Apple Silicon 时代的到来。

### 1.2 企业/服务器相关

| 组件 | 定位 | 说明 |
|------|------|------|
| macOS Server | 服务器套件（已退役） | 2022 年停止更新，功能分散到各 macOS 中 |
| Xcode Server | CI/CD | Xcode 内置持续集成 |
| Apple Business Manager | MDM 设备管理 | 企业级 Mac 部署与管理 |

---

## 2. Darwin 内核架构

### 2.1 XNU 内核全景

```text
┌─────────────────────────────────────────────────────────┐
│                      用户态 (User Space)                  │
│  ┌───────────┐ ┌───────────┐ ┌──────────────────────┐  │
│  │ Aqua GUI  │ │ 守护进程   │ │  BSD 用户空间工具     │  │
│  │ (SpringBoard-like)│ (launchd管理)│ (ls, ps, bash...)  │  │
│  └───────────┘ └───────────┘ └──────────────────────┘  │
│  ┌────────────────────────────────────────────────────┐  │
│  │            系统框架 (Foundation, AppKit, SwiftUI...) │  │
│  └────────────────────────────────────────────────────┘  │
├──────────────────── 系统调用 (syscall) ──────────────────┤
│                      内核态 (Kernel Space)                │
│  ┌────────────────────────────────────────────────────┐  │
│  │                    BSD 层 (POSIX 接口)               │  │
│  │    进程模型、文件系统、网络栈、用户/权限管理          │  │
│  └────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────┐  │
│  │                   Mach 微内核层                      │  │
│  │   IPC、线程调度、虚拟内存、实时时钟、硬件抽象         │  │
│  └────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────┐  │
│  │                   I/O Kit (设备驱动框架)              │  │
│  │     面向对象 C++ 设备驱动运行时                      │  │
│  └────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                   硬件 (Apple Silicon / Intel)            │
└─────────────────────────────────────────────────────────┘
```

### 2.2 XNU 的三层结构

| 层级 | 来源 | 职责 | 特点 |
|------|------|------|------|
| **Mach** | CMU 大学 | 微内核核心：IPC、线程、虚拟内存、任务调度 | 消息传递机制，所有服务间通信走 Port |
| **BSD** | FreeBSD | POSIX 兼容层：进程模型、VFS、网络栈、权限 | 提供标准 Unix 编程接口 |
| **I/O Kit** | Apple 自研 | 设备驱动框架 | C++ 面向对象驱动模型，动态加载 |

> 🎯 **核心认知**：XNU = **Mach (微内核)** + **BSD (单体内核层)** 的混合内核，兼具微内核的模块化和单体内核的性能。

### 2.3 与 Linux 内核的关键差异

| 维度 | Linux | XNU (macOS) |
|------|-------|-------------|
| **内核类型** | 单体内核（Monolithic） | 混合内核（Hybrid） |
| **设备驱动** | 在内核空间，C 语言 | I/O Kit，C++ 面向对象 |
| **文件系统** | VFS 层 + ext4/XFS/Btrfs | VFS 层 + APFS/HFS+ |
| **进程通信** | System V IPC / Unix Socket | Mach Port（消息传递） |
| **线程模型** | 1:1 线程模型 | Mach 线程（pthread 包装） |
| **可加载模块** | `.ko` (kernel object) | `.kext` (kernel extension) → System Extension (新) |
| **调度器** | CFS (完全公平调度器) | Mach 调度器 |
| **源码** | 完全开源 | Darwin 开源（部分驱动闭源） |

---

## 3. 硬件架构迁移：PowerPC → Intel → Apple Silicon

### 3.1 三次架构迁移

```text
1994-2006: PowerPC (IBM/Motorola)
    │
    ├── 2005: 宣布转向 Intel x86
    │   工具：Rosetta v1 (PPC → x86 转译)
    │
    ▼
2006-2020: Intel x86_64 (Core 2 Duo → Xeon)
    │
    ├── 2020: 宣布转向 Apple Silicon (ARM64)
    │   工具：Rosetta 2 (x86_64 → ARM64 转译)
    │   Universal 2 Binary：同时包含 x86_64 + ARM64 代码
    │
    ▼
2020-现在: Apple Silicon (M1 → M2 → M3 → M4)
```

### 3.2 对 Java 开发者的影响

| 影响 | Intel Mac | Apple Silicon Mac |
|------|-----------|-------------------|
| **JDK 架构** | x86_64 (amd64) | **aarch64** (ARM 64) |
| **原生 JDK 支持** | 所有发行版 | Azul / Temurin / Oracle / GraalVM 支持 ARM |
| **Docker** | Docker Desktop (x86_64) | Docker Desktop (ARM 原生) + 可模拟 x86 |
| **Maven/Gradle** | 原生性能 | **显著更快**（单核强 30%+） |
| **IDE (IDEA)** | 原生 | 原生 ARM 版（2021+ 支持） |
| **JNI / 本地库** | x86_64 .dylib | 需要 ARM 版本 .dylib |

```bash
# 检测当前 Mac 架构
uname -m
# x86_64 → Intel Mac
# arm64  → Apple Silicon

# 检测 JDK 架构
java -XshowSettings:properties -version 2>&1 | grep "os.arch"
# os.arch = x86_64 (Intel JDK)
# os.arch = aarch64 (ARM JDK)

# Apple Silicon 上运行 x86 JDK（通过 Rosetta 2）
arch -x86_64 java -version
```

### 3.3 Rosetta 2 使用场景

```bash
# 需要 Rosetta 2 的情况：
# 1. 某些 JDK 发行版尚未提供 ARM 版本
# 2. 老旧的 JNI 本地库（.dylib）只有 x86_64 版本
# 3. Docker 中运行 x86_64 镜像

# 安装 Rosetta 2
softwareupdate --install-rosetta

# 用 x86_64 模式启动终端
arch -x86_64 zsh

# 在 x86_64 模式下安装 x86 版 Homebrew
arch -x86_64 /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
# 安装在 /usr/local/（ARM Homebrew 在 /opt/homebrew/）
```

---

## 4. macOS 与 Linux / Windows 本质差异

### 4.1 核心差异对照

| 维度 | macOS | Linux | Windows |
|------|-------|-------|---------|
| **内核** | XNU (Mach+BSD) | Linux | NT |
| **默认 Shell** | zsh | bash | cmd / PowerShell |
| **文件路径** | `/Users/xxx/` | `/home/xxx/` | `C:\Users\xxx\` |
| **路径分隔符** | `:` | `:` | `;` |
| **换行符** | `\n` (LF) | `\n` (LF) | `\r\n` (CRLF) |
| **文件系统** | APFS | ext4 / XFS / Btrfs | NTFS |
| **大小写** | **默认不敏感** (APFS 可选敏感) | 敏感 | 不敏感 |
| **包管理** | **Homebrew** (事实标准) | apt / yum / pacman | winget / choco / scoop |
| **服务管理** | **launchd** | systemd | services.msc |
| **应用格式** | `.app` Bundle + `.dmg` 磁盘映像 | ELF + 包管理器 | `.exe` + `.msi` |
| **动态库** | `.dylib` | `.so` | `.dll` |
| **桌面环境** | Aqua (唯一官方) | GNOME / KDE / XFCE... | Windows Shell |
| **窗口管理** | 全局菜单栏 + Dock | 各桌面环境不同 | 任务栏 + 开始菜单 |
| **GUI 技术** | SwiftUI / AppKit | GTK / Qt | WinUI / WPF / Win32 |
| **包格式** | `.app` (Bundle 目录) | ELF 二进制 | `.exe` PE 格式 |
| **剪切板** | `pbcopy` / `pbpaste` | `xclip` / `wl-copy` | `clip` |

### 4.2 macOS 特有概念速览

| 概念 | 说明 | Linux 类比 |
|------|------|-----------|
| **Bundle** | `.app` 是目录，包含所有资源，看起来像单个文件 | ❌ 无直接类比 |
| **plist** | XML/Binary 格式的配置文件 | 部分类似 systemd unit 文件 |
| **launchd** | 统一进程/服务/定时任务管理器 | systemd + cron + inetd |
| **TCC** | 隐私权限控制（相机/麦克风/文件） | ❌ 无直接类比 |
| **Gatekeeper** | 代码签名验证 + 公证 | ❌ 无直接类比 |
| **Keychain** | 密码/证书/密钥安全存储 | gnome-keyring / kwallet |
| **Spotlight** | 系统级实时搜索索引 | locate + GNOME Tracker |
| **.DS_Store** | 每个目录的元数据文件 | ❌ 无（常出现在 Git 中） |

> 🎯 **核心要点**：macOS 的本质是一个**经过深度定制的 Unix 系统**，它既有 Linux 的终端体验，又有封闭生态的优雅体验。

---

## 5. macOS 核心设计哲学

### 5.1 设计原则

| 原则 | 说明 | 开发者影响 |
|------|------|-----------|
| **一致性** | 全局菜单栏、统一快捷键（⌘Q 永远退出） | 学习成本低 |
| **隐喻** | 桌面、文件夹、废纸篓... | 直观 |
| **直接操作** | 拖拽为主、多点触控 | 交互自然 |
| **所见即所得** | 打印/导出效果与屏幕一致 | PDF 生态好 |
| **用户控制** | 细粒度权限弹窗（TCC） | 开发需申请权限 |

### 5.2 安全架构一览

```text
macOS 安全分层：

第一层：Gatekeeper（门禁）
  └── 禁止未签名/未公证的 App 运行

第二层：Notarization（公证）
  └── Apple 扫描恶意代码后才放行

第三层：Sandbox（沙盒）
  └── App Store 应用运行在受限环境中

第四层：TCC（透明、同意和控制）
  └── 摄像头、麦克风、文件、屏幕录制…需用户授权

第五层：SIP（系统完整性保护）
  └── 即使 root 也无法修改系统关键目录

第六层：XProtect + MRT
  └── 内建防病毒 + 恶意软件移除工具
```

```bash
# 查看 SIP 状态
csrutil status

# 查看 Gatekeeper 设置
spctl --status

# 强制打开被 Gatekeeper 阻止的应用
xattr -d com.apple.quarantine /path/to/app.app
# 或在系统设置中允许
```

---

**下一模块**：[02-macOS终端与Shell全指南](./02-macOS终端与Shell全指南.md) ｜ **返回总览**：[00-macOS知识体系总览](./00-macOS知识体系总览.md)

---

*创建于：2026年7月*
