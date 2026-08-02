# Windows 知识体系

> 🪟 从 Java 开发者的视角重新认识 Windows —— 系统架构、命令行双雄、环境配置、包管理、文件系统、进程安全、实战排错，打造高效的 Windows 开发工作流

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)

---

## 1. 知识体系导图

```
Windows 知识体系 (Java 开发者视角)
│
├── 🏗️ 系统认知层
│   └── 01-Windows系统基础与演进
│       ├── Windows 版本演进 (XP → Win11)
│       ├── 系统架构：内核态 vs 用户态
│       ├── 注册表：Windows 的"配置文件系统"
│       └── Windows 与 Linux/macOS 的本质差异
│
├── ⌨️ 命令行能力层
│   └── 02-Windows命令行全指南
│       ├── CMD：经典命令解释器
│       ├── PowerShell：面向对象的现代 Shell
│       ├── Windows Terminal：新一代终端模拟器
│       └── WSL：Windows 上的原生 Linux
│
├── ☕ Java 开发环境层
│   └── 03-Windows环境配置与Java开发
│       ├── 环境变量体系：用户变量 vs 系统变量
│       ├── JAVA_HOME、Path、CLASSPATH 深度解析
│       ├── 多 JDK 版本管理与切换
│       └── IDE 配置 (IntelliJ / VS Code)
│
├── 📦 软件管理层
│   └── 04-Windows包管理与软件生态
│       ├── winget：微软官方包管理器
│       ├── Chocolatey：社区驱动的包管理
│       ├── Scoop：面向开发者的便携包管理
│       └── 三巨头对比与选型
│
├── 🗄️ 系统运维层
│   └── 05-Windows文件系统进程与安全
│       ├── NTFS 文件系统特性
│       ├── 文件权限与 ACL
│       ├── 进程管理与任务管理器
│       ├── Windows 服务 (services.msc)
│       └── Windows Defender 与防火墙
│
└── 🛠️ 实战排错层
    └── 06-Windows开发者实战手册
        ├── 常见 Java 开发环境问题
        ├── 端口占用、权限拒绝、编码乱码
        ├── 系统优化与清理
        └── 开发者必备快捷键与技巧
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|:-----:|
| 01 | [Windows系统基础与演进](./01-Windows系统基础与演进.md) | OS版本史、内核架构、注册表、Windows vs Linux 对照 | 所有人 |
| 02 | [Windows命令行全指南](./02-Windows命令行全指南.md) | CMD 常用命令、PowerShell 对象管道、Windows Terminal、WSL2 | 所有人 |
| 03 | [Windows环境配置与Java开发](./03-Windows环境配置与Java开发.md) | 环境变量深度解析、JAVA_HOME、多JDK切换、IDE调优 | Java 开发者 |
| 04 | [Windows包管理与软件生态](./04-Windows包管理与软件生态.md) | winget/choco/scoop 三巨头、开发工具一键安装 | 中高级 |
| 05 | [Windows文件系统进程与安全](./05-Windows文件系统进程与安全.md) | NTFS、ACL权限、进程/服务管理、安全机制 | 中高级 |
| 06 | [Windows开发者实战手册](./06-Windows开发者实战手册.md) | 环境排错、端口释放、编码问题、系统优化、效率技巧 | 所有人 |

---

## 3. 学习路线推荐

| 路线 | 路径 | 时长 | 目标 |
|------|------|:--:|------|
| 🟢 **快速上手** | 02 → 03 | 半天 | 能配好 Java 环境、熟练使用命令行 |
| 🟡 **系统掌握** | 01 → 02 → 03 → 06 | 1 天 | 理解 Windows 机制、独立排查环境问题 |
| 🔴 **全栈运维** | 全部 6 模块 | 2-3 天 | 从开发到运维全面掌控 Windows 技术栈 |

---

## 4. 核心概念速查

| 概念 | 一句话解释 | 详见 |
|------|-----------|------|
| **注册表 (Registry)** | Windows 的层次化配置数据库，替代 Linux 的 `/etc` 散文件 | 模块01 |
| **环境变量** | 进程运行时的键值对配置，Path 是最高频使用的变量 | 模块03 |
| **PowerShell 对象管道** | 管道传递的是 `.NET 对象` 而非纯文本，颠覆传统 Shell 思维 | 模块02 |
| **NTFS** | Windows 默认文件系统，支持 ACL 权限、日志、压缩、加密 | 模块05 |
| **Windows 服务** | 后台长期运行的程序，通过 `services.msc` 管理 | 模块05 |
| **WSL2** | Windows Subsystem for Linux 2，在 Windows 上运行完整 Linux 内核 | 模块02 |
| **winget** | 微软官方包管理器，`winget install` 一键装软件 | 模块04 |
| **端口占用** | `netstat -ano | findstr :8080` 定位进程，`taskkill` 释放 | 模块06 |
| **CMD vs PowerShell** | CMD 是文本 Shell（类似 bash），PowerShell 是面向对象 Shell | 模块02 |

---

> 🎯 **开始学习**：[01-Windows系统基础与演进](./01-Windows系统基础与演进.md) ｜ **快速实战**：[03-Windows环境配置与Java开发](./03-Windows环境配置与Java开发.md)

---

*创建于：2026年7月*
