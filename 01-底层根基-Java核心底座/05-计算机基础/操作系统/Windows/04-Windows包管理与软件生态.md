# Windows 包管理与软件生态

> 📦 winget、Chocolatey、Scoop 三足鼎立 —— 从"下载→下一步→下一步→完成"到一行命令装好全套开发环境

---

## 📚 目录

1. [为什么需要包管理器](#1-为什么需要包管理器)
2. [winget：微软官方](#2-winget微软官方)
3. [Chocolatey：社区先驱](#3-chocolatey社区先驱)
4. [Scoop：开发者的选择](#4-scoop开发者的选择)
5. [三巨头全面对比与选型](#5-三巨头全面对比与选型)
6. [Java 开发者软件栈一键部署](#6-java-开发者软件栈一键部署)

---

## 1. 为什么需要包管理器

```text
传统方式：打开浏览器 → 搜索软件 → 找到官网 → 下载 .exe/.msi
         → 双击运行 → 下一步×5 → 取消勾选捆绑软件 → 完成
         → 重复以上 × 需要的工具数量

包管理器：winget install OpenJDK.21
         # 一条命令，下载+安装+配置+Path，全部自动搞定
```

| 优势 | 说明 |
|------|------|
| **一键安装** | 无需手动下载、双击、下一步 |
| **自动 PATH** | 安装即配置好环境变量，立即可用 |
| **版本管理** | `upgrade` 一键升级，`list` 查看所有已装 |
| **脚本化** | 新机器/重装系统，一个脚本恢复全部工具 |
| **来源可信** | 从官方仓库/源下载，避免下载站捆绑 |

---

## 2. winget：微软官方

### 2.1 简介

**Windows Package Manager (winget)** 是微软 2020 年推出的官方包管理器，Windows 11 已内置，Windows 10 需安装 [App Installer](https://apps.microsoft.com/detail/9nblggh4nns1)。

```powershell
# 检查是否已安装
winget --version
```

### 2.2 核心命令

```powershell
# 搜索软件
winget search "jdk 21"
winget search "intellij"

# 安装软件
winget install EclipseAdoptium.Temurin.21.JDK
winget install JetBrains.IntelliJIDEA.Community

# 查看已安装
winget list

# 升级单个/全部
winget upgrade EclipseAdoptium.Temurin.21.JDK
winget upgrade --all

# 卸载
winget uninstall JetBrains.IntelliJIDEA.Community

# 导出/导入（迁移神器）
winget export -o packages.json     # 导出已装列表
winget import -i packages.json     # 在新机器恢复所有软件
```

### 2.3 winget 的 Java 生态覆盖

```powershell
# JDK 发行版
winget install EclipseAdoptium.Temurin.21.JDK    # Adoptium JDK 21
winget install EclipseAdoptium.Temurin.17.JDK
winget install Microsoft.OpenJDK.21
winget install Oracle.JDK.21                      # Oracle JDK (需登录)

# 开发工具
winget install Git.Git
winget install Microsoft.VisualStudioCode
winget install JetBrains.IntelliJIDEA.Community
winget install Apache.Maven.3.9.9
winget install Docker.DockerDesktop

# 数据库工具
winget install DBeaverCommunity.DBeaverCommunity
winget install HeidiSQL.HeidiSQL

# 效率工具
winget install 7zip.7zip
winget install Notepad++.Notepad++
winget install Microsoft.PowerToys
winget install Microsoft.WindowsTerminal
```

> ⚠️ **winget 局限**：部分包版本滞后（依赖社区维护 manifest），安装路径通常不可自定义，且大多是系统级安装。

---

## 3. Chocolatey：社区先驱

### 3.1 安装 Chocolatey

```powershell
# 以管理员身份运行 PowerShell，执行：
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

### 3.2 核心命令

```powershell
# 搜索
choco search jdk

# 安装
choco install jdk8 -y      # -y 跳过确认
choco install intellijidea-community -y

# 查看
choco list --local-only     # 本地已装
choco outdated              # 可升级的

# 升级
choco upgrade all -y
choco upgrade jdk8 -y

# 卸载
choco uninstall jdk8 -y
```

### 3.3 Chocolatey 特色

| 特色 | 说明 |
|------|------|
| **社区最大** | 1 万+ 包，生态最丰富 |
| **GUI 可选** | Chocolatey GUI：`choco install chocolateygui` |
| **企业版** | C4B：集中管理、自动更新、审计日志 |
| **自动 PATH** | 安装后立即可用，无需手动配环境变量 |
| **安装脚本** | `choco install packages.config` 批量安装 |

### 3.4 经典 packages.config

```xml
<?xml version="1.0" encoding="utf-8"?>
<packages>
  <!-- JDK -->
  <package id="jdk8" />
  <package id="temurin21" />

  <!-- 构建工具 -->
  <package id="maven" />
  <package id="gradle" />

  <!-- IDE -->
  <package id="intellijidea-community" />
  <package id="vscode" />

  <!-- 运行时 -->
  <package id="git" />
  <package id="nodejs" />
  <package id="python" />

  <!-- 数据库 -->
  <package id="dbeaver" />
  <package id="mysql" />

  <!-- 工具 -->
  <package id="7zip" />
  <package id="notepadplusplus" />
  <package id="postman" />
  <package id="docker-desktop" />
</packages>
```

```powershell
# 一键安装全部
choco install packages.config -y
```

> ⚠️ **Chocolatey 局限**：部分包需要管理员权限，系统级安装较"脏"，卸载可能有残留。

---

## 4. Scoop：开发者的选择

### 4.1 Scoop 的设计哲学

```text
Scoop 的核心原则：
├── 📁 所有软件装在 ~/scoop/ 下（不需要管理员权限）
├── 🔗 通过 ~/scoop/shims/ 软链接暴露命令
├── 🎯 用户级安装（不污染系统）
├── 📦 便携式软件优先（解压即用）
└── 🪣 bucket 机制组织软件源
```

### 4.2 安装与配置

```powershell
# 安装 Scoop（普通用户权限即可）
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# 添加常用 bucket（软件源）
scoop bucket add extras       # GUI 软件
scoop bucket add java         # JDK 各版本
scoop bucket add versions     # 多版本软件
scoop bucket add nerd-fonts   # 编程字体
```

### 4.3 核心命令

```powershell
# 搜索
scoop search jdk

# 安装
scoop install temurin21-jdk
scoop install maven
scoop install intellij-idea

# 查看
scoop list               # 已装列表
scoop status             # 可升级的

# 升级
scoop update             # 更新 Scoop 自身 + buckets
scoop update *           # 升级所有软件
scoop update temurin21-jdk

# 卸载
scoop uninstall maven

# 清理旧版本
scoop cleanup *
```

### 4.4 Scoop 目录结构

```text
~/scoop/
├── apps/           # 已安装软件（每个软件有独立目录 + current 软链接）
│   ├── temurin21-jdk/
│   │   ├── 21.0.5-11/        # 具体版本
│   │   └── current → 21.0.5-11/
│   ├── maven/
│   └── git/
├── buckets/        # 软件源 manifest
│   ├── main/
│   ├── extras/
│   └── java/
├── shims/          # 命令代理（自动加入 Path）
│   ├── java.exe    → 软链接到 current 版本
│   ├── javac.exe
│   └── mvn
└── workspace/
```

### 4.5 Java 生态 Scoop 安装

```powershell
# 添加 java bucket
scoop bucket add java

# JDK 全家桶
scoop install temurin21-jdk     # Adoptium JDK 21
scoop install temurin17-jdk     # Adoptium JDK 17
scoop install temurin11-jdk     # Adoptium JDK 11
scoop install microsoft21-jdk   # Microsoft OpenJDK 21
scoop install oraclejdk         # Oracle JDK
scoop install graalvm-jdk       # GraalVM

# 切换默认 Java 版本
scoop reset temurin21-jdk       # 切换当前版本
scoop reset temurin17-jdk       # 切换到 17

# 开发工具
scoop install maven             # Maven 最新版
scoop install gradle            # Gradle 最新版
scoop install ant               # Ant

# IDE
scoop install intellij-idea     # IntelliJ IDEA Community
scoop install vscode            # VS Code

# 数据库
scoop install mysql             # MySQL
scoop install dbeaver           # DBeaver

# 通用工具
scoop install git
scoop install 7zip
scoop install aria2             # 多线程下载（自动加速）scoop install sudo              # Windows 上的 sudo
scoop install curl
scoop install jq                # JSON 处理
```

> 🎯 **Scoop 最大优势**：`scoop reset <jdk>` 一键切换 Java 版本，比改环境变量优雅得多。

---

## 5. 三巨头全面对比与选型

### 5.1 功能对比表

| 维度 | winget | Chocolatey | Scoop |
|------|:---:|:---:|:---:|
| **开发者** | 微软官方 | 社区 | 社区 |
| **首次发布** | 2020 | 2011 | 2018 |
| **内置于** | Win11 内置 | 需安装 | 需安装 |
| **包数量** | ~5000+ | ~10000+ | ~2000+ |
| **管理员权限** | 部分需要 | 大多数需要 | **不需要** |
| **安装位置** | 系统 `Program Files` | 系统 `Program Files` | **用户目录** `~/scoop` |
| **版本切换** | ❌ 不支持 | ❌ 不支持 | ✅ `scoop reset` |
| **多版本共存** | ❌ | ❌ | ✅ `versions` bucket |
| **便携软件** | ❌ | ❌ | ✅ 核心设计 |
| **卸载干净度** | 中等 | 中等 | **极高** |
| **GUI 客户端** | ❌ | ✅ Chocolatey GUI | ❌ |
| **JSON 导入导出** | ✅ | ✅ (packages.config) | ✅ `scoop export/import` |
| **社区活跃度** | 高（微软推动） | 高（历史最久） | 中（开发者最爱） |

### 5.2 选型建议

| 用户类型 | 推荐 | 理由 |
|---------|:--:|------|
| **普通用户** | winget | Windows 11 内置，开箱即用 |
| **企业环境** | Chocolatey | 最多包，企业版支持 |scoop
| **Java 开发者** | **Scoop** | 无需管理员、版本切换、干净卸载 |
| **组合使用** | winget + Scoop | winget 装大软件（IDEA、VS）、Scoop 装开发工具 |

---

## 6. Java 开发者软件栈一键部署

### 6.1 Scoop 版部署脚本

```powershell
# new-machine-setup.ps1
# 新机器/重装系统后一键恢复开发环境

# 1. 安装 Scoop
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser -Force
irm get.scoop.sh | iex

# 2. 安装加速工具
scoop install aria2 git 7zip

# 3. 添加 buckets
scoop bucket add extras
scoop bucket add java
scoop bucket add versions

# 4. 安装 JDK
scoop install temurin21-jdk
scoop install temurin17-jdk
scoop install temurin11-jdk

# 5. 构建工具
scoop install maven
scoop install gradle

# 6. IDE 与编辑器
scoop install vscode
scoop install intellij-idea

# 7. 运行时
scoop install nodejs-lts
scoop install python

# 8. 数据库与工具
scoop install dbeaver
scoop install postman

# 9. 辅助工具
scoop install everything      # 文件搜索
scoop install powertoys       # 微软效率工具集

# 10. 编程字体
scoop install FiraCode-NF
scoop install CascadiaCode-NF

# 完成
Write-Host "✅ 开发环境部署完成！" -ForegroundColor Green
java --version
```

### 6.2 winget 版部署脚本

```powershell
# winget 版（Win11 无需安装包管理器）
$packages = @(
    "EclipseAdoptium.Temurin.21.JDK",
    "Git.Git",
    "Apache.Maven.3.9.9",
    "Microsoft.VisualStudioCode",
    "JetBrains.IntelliJIDEA.Community",
    "Docker.DockerDesktop",
    "DBeaverCommunity.DBeaverCommunity",
    "Microsoft.WindowsTerminal",
    "Microsoft.PowerToys",
    "7zip.7zip"
)

foreach ($pkg in $packages) {
    Write-Host "Installing $pkg..." -ForegroundColor Yellow
    winget install --id $pkg --silent --accept-package-agreements --accept-source-agreements
}
```

---

**上一模块**：[03-Windows环境配置与Java开发](./03-Windows环境配置与Java开发.md) ｜ **下一模块**：[05-Windows文件系统进程与安全](./05-Windows文件系统进程与安全.md) ｜ **返回总览**：[00-Windows知识体系总览](./00-Windows知识体系总览.md)

---

*创建于：2026年7月*
