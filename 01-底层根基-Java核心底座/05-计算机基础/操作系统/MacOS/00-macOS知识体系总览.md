# macOS 知识体系

> 🍎 从 Java 开发者的视角驾驭 macOS —— Darwin 内核、zsh 终端、Homebrew 生态、APFS 文件系统、launchd 服务、多 JDK 丝滑切换，打造高效的 macOS 开发工作流

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)

---

## 1. 知识体系导图

```
macOS 知识体系 (Java 开发者视角)
│
├── 🏗️ 系统认知层
│   └── 01-macOS系统基础与演进
│       ├── macOS 版本演进 (Mac OS X → macOS 15 Sequoia)
│       ├── Darwin 内核架构：XNU = Mach + BSD
│       ├── 文件系统演进：HFS+ → APFS
│       └── macOS 与 Linux/Windows 的本质差异
│
├── ⌨️ 终端能力层
│   └── 02-macOS终端与Shell全指南
│       ├── Terminal.app vs iTerm2 vs Warp
│       ├── zsh：macOS 默认 Shell 深度使用
│       ├── bash 兼容与迁移
│       ├── 管道、重定向、脚本
│       └── macOS 特有命令（open、pbcopy、mdfind）
│
├── ☕ Java 开发环境层
│   └── 03-macOS环境配置与Java开发
│       ├── macOS 环境变量体系（zsh profile / plist）
│       ├── Homebrew 安装多 JDK 版本
│       ├── SDKMAN：Java 版本管理神器
│       ├── JAVA_HOME 与 /usr/libexec/java_home
│       └── IDEA / VS Code macOS 专属配置
│
├── 📦 软件管理层
│   └── 04-Homebrew包管理与软件生态
│       ├── Homebrew 核心命令与原理
│       ├── Formulae vs Casks vs MAS
│       ├── Brewfile：一键恢复开发环境
│       └── Mac App Store 命令行（mas）
│
├── 🗄️ 系统运维层
│   └── 05-macOS文件系统进程与安全
│       ├── APFS 特性：快照、克隆、加密、空间共享
│       ├── macOS 权限模型：Sandbox、TCC、Gatekeeper
│       ├── launchd：统一服务管理器
│       └── 网络、防火墙、安全审计
│
└── 🛠️ 实战排错层
    └── 06-macOS开发者实战手册
        ├── M 芯片兼容性（Rosetta 2、原生 ARM）
        ├── 端口占用、权限拒绝、编码乱码
        ├── 系统优化与清理
        └── 开发者必备快捷键与效率工具
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|:-----:|
| 01 | [macOS系统基础与演进](./01-macOS系统基础与演进.md) | OS X→Sequoia、Darwin/XNU 内核、APFS、macOS vs Linux vs Win | 所有人 |
| 02 | [macOS终端与Shell全指南](./02-macOS终端与Shell全指南.md) | iTerm2、zsh 深度使用、管道重定向、macOS 专属命令 | 所有人 |
| 03 | [macOS环境配置与Java开发](./03-macOS环境配置与Java开发.md) | java_home、SDKMAN、JAVA_HOME、IDEA/VS Code 专属优化 | Java 开发者 |
| 04 | [Homebrew包管理与软件生态](./04-Homebrew包管理与软件生态.md) | brew 命令、Formulae/Casks、Brewfile、mas 命令行 | 中高级 |
| 05 | [macOS文件系统进程与安全](./05-macOS文件系统进程与安全.md) | APFS、TCC 沙盒、launchd、防火墙、安全机制 | 中高级 |
| 06 | [macOS开发者实战手册](./06-macOS开发者实战手册.md) | M 芯片兼容、端口排错、系统优化、快捷键、效率工具 | 所有人 |

---

## 3. 学习路线推荐

| 路线 | 路径 | 时长 | 目标 |
|------|------|:--:|------|
| 🟢 **快速上手** | 02 → 03 | 半天 | 能配好 Java 环境、熟练使用终端 |
| 🟡 **系统掌握** | 01 → 02 → 03 → 06 | 1 天 | 理解 macOS 核心机制、独立排查环境问题 |
| 🔴 **全栈运维** | 全部 6 模块 | 2-3 天 | 从开发到运维全面掌控 macOS 技术栈 |

---

## 4. 核心概念速查

| 概念 | 一句话解释 | 详见 |
|------|-----------|------|
| **Darwin / XNU** | macOS 内核，XNU = Mach 微内核 + BSD 层 + I/O Kit | 模块01 |
| **APFS** | Apple 文件系统，支持快照、克隆、加密、空间共享 | 模块05 |
| **zsh** | macOS 默认 Shell，兼容 bash，强大扩展能力 | 模块02 |
| **Homebrew** | macOS 事实标准包管理器，`brew install` 搞定一切 | 模块04 |
| **SDKMAN** | 类 Unix 上最好的 JDK 版本管理器，`sdk use java` 秒切 | 模块03 |
| **launchd** | macOS 的 init + cron + inetd 统一替代品 | 模块05 |
| **TCC** | 隐私保护系统（摄像头/麦克风/文件访问控制） | 模块05 |
| **Gatekeeper** | 代码签名验证 + 公证，阻止未签名应用运行 | 模块05 |
| **/usr/libexec/java_home** | macOS 独有命令，返回当前/指定 JDK 路径 | 模块03 |
| **Rosetta 2** | Apple Silicon 上运行 x86_64 程序的转译层 | 模块06 |

---

> 🎯 **开始学习**：[01-macOS系统基础与演进](./01-macOS系统基础与演进.md) ｜ **快速实战**：[03-macOS环境配置与Java开发](./03-macOS环境配置与Java开发.md)

---

*创建于：2026年7月*
