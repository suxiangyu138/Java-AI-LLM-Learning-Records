# WSL2 与 Windows 互操作

> 🔗 文件系统互访（/mnt/c/ vs \\\\wsl$\\）、路径映射、跨 OS 程序调用、性能陷阱与最佳实践 —— WSL2 与 Windows 无缝协作的完整指南

---

## 📚 目录

1. [文件系统互访](#1-文件系统互访)
2. [性能陷阱：跨文件系统 I/O](#2-性能陷阱跨文件系统-io)
3. [跨 OS 程序调用](#3-跨-os-程序调用)
4. [环境变量与 PATH 互通](#4-环境变量与-path-互通)
5. [最佳实践：文件放哪里](#5-最佳实践文件放哪里)

---

## 1. 文件系统互访

### 1.1 从 WSL2 访问 Windows 文件

```bash
# === /mnt/ 自动挂载 ===
# Windows 所有驱动器自动挂载到 /mnt/ 下
ls /mnt/
# c/  d/  e/  ...

# 访问 Windows 文件
cd /mnt/c/Users/用户名/Desktop
ls /mnt/d/projects/

# === drvfs 挂载 ===
# Windows 文件通过 drvfs 文件系统驱动挂载
mount | grep drvfs
# C:\ on /mnt/c type 9p (rw,noatime,dirsync,...)

# === 手动挂载 Windows 目录到 Linux ===
sudo mkdir -p /mnt/projects
sudo mount -t drvfs D:\\projects /mnt/projects

# === /etc/fstab 自动挂载 ===
# 编辑 /etc/fstab，添加：
# D:\projects /mnt/projects drvfs defaults 0 0
# ⚠️ 注意：WSL2 中推荐在 /etc/wsl.conf 的 [automount] 配置
```

### 1.2 从 Windows 访问 WSL2 文件

```text
方式一：文件资源管理器 → \\wsl$\<发行版名>\
  地址栏输入：\\wsl$\Ubuntu-24.04\home\用户名\projects

方式二：wslview / wslpath 命令
  wslview .          → 在 Windows 资源管理器打开当前 WSL2 目录
  wslpath -w ~       → 将 WSL2 路径转为 Windows 路径
  wslpath -m 'C:\'   → 将 Windows 路径转为 WSL2 路径

方式三：在 WSL2 终端中直接打开
  explorer.exe .     → 当前目录在文件资源管理器中打开
```

```powershell
# === Windows PowerShell 中操作 WSL2 文件 ===

# 列出 WSL2 用户目录
ls \\wsl$\Ubuntu-24.04\home\用户名\

# 复制文件到 WSL2
copy .\config.json \\wsl$\Ubuntu-24.04\home\用户名\

# 从 WSL2 复制文件
copy \\wsl$\Ubuntu-24.04\home\用户名\output.txt .
```

### 1.3 路径转换工具

```bash
# === wslpath：WSL 内置路径转换 ===
# WSL2 → Windows
wslpath -w /home/username/projects
# → \\wsl$\Ubuntu-24.04\home\username\projects

wslpath -m /home/username/projects
# → //wsl$/Ubuntu-24.04/home/username/projects

# Windows → WSL2
wslpath -u 'C:\\Users\\username\\Desktop'
# → /mnt/c/Users/username/Desktop

# === 在脚本中使用 ===
# 获取 Windows 的用户目录
WIN_HOME=$(wslpath -u "$(cmd.exe /c echo %USERPROFILE% 2>/dev/null | tr -d '\r')")
```

---

## 2. 性能陷阱：跨文件系统 I/O

### 2.1 性能对比表

| 操作 | WSL2 Linux 内 (ext4) | WSL2 访问 /mnt/c/ (9P) | 差距 |
|------|:---:|:---:|:---:|
| **小文件读取** | ✅ 极快 | ⚠️ 慢 3-5x | 网络协议开销 |
| **大量小文件（node_modules）** | ✅ 快 | ❌ 慢 10-20x | 每个文件需网络往返 |
| **Git 操作** | ✅ 快 | ❌ 极慢（尤其是 `git status`） | 需要遍历大量文件 |
| **Java 编译（Maven/Gradle）** | ✅ 快 | ❌ 慢 5-10x | 读取 + 写入大量 .class |
| **数据库文件（SQLite）** | ✅ 极快 | ❌ 不适合 | 频繁随机读写 |
| **大文件顺序读写** | ✅ 极快 | ⚠️ 慢 1.5-2x | 带宽瓶颈 |
| **IDE 索引（IntelliJ）** | ✅ 快 | ❌ 极慢 | 读取整个项目文件 |

### 2.2 跨文件系统慢的根本原因

```text
WSL2 的文件系统架构：

Linux ext4 文件:
  Java 进程 → ext4 驱动 → 虚拟磁盘 vhdx → SSD
  → 直通，极快

Windows NTFS 文件 (/mnt/c/):
  Java 进程 → 9P 客户端 → Hyper-V 网络 → 9P 服务端 → NTFS → SSD
  → 经过虚拟网络 + 协议转换，有显著开销

结论：跨文件系统本质上是走网络协议！
```

### 2.3 性能验证

```bash
# 简单基准测试
# 在 Linux ext4 上
dd if=/dev/zero of=~/test.dat bs=1M count=1024 conv=fdatasync
# → ~ 2000 MB/s (NVMe SSD)

# 在 /mnt/c/ 上
dd if=/dev/zero of=/mnt/c/Users/用户名/test.dat bs=1M count=1024 conv=fdatasync
# → ~ 300 MB/s (相同的 SSD！)

# Git 速度对比
cd ~/projects               # WSL2 ext4 内
time git status              # → ~0.05s

cd /mnt/c/same-project       # NTFS 上同一项目
time git status              # → ~3s (慢 60 倍！)
```

> 🎯 **核心原则**：**项目代码永远放在 WSL2 的 ext4 文件系统内**（`~/projects/`），不要放在 `/mnt/c/` 下。

---

## 3. 跨 OS 程序调用

### 3.1 WSL2 中调用 Windows 程序

```bash
# === Windows 程序在 WSL2 中直接可用 ===
# （因为 [interop] appendWindowsPath=true）

# 浏览器和文件管理器
explorer.exe .                    # 在文件资源管理器中打开当前目录
start .                           # 同上（PowerShell 别名）

# 编辑器
notepad.exe pom.xml               # 用 Windows 记事本打开
code .                            # 用 Windows VS Code 打开当前目录
code --remote wsl+Ubuntu-24.04 .  # 强制 WSL2 Remote 模式

# 剪贴板
echo "hello" | clip.exe           # 复制到 Windows 剪贴板
powershell.exe Get-Clipboard      # 读取 Windows 剪贴板

# 其他
cmd.exe /c dir                    # 执行 CMD 命令
powershell.exe -Command "Get-Process | Select-Object -First 5"
ipconfig.exe                      # Windows 网络配置
```

### 3.2 Windows 中调用 WSL2 命令

```powershell
# === PowerShell 中调用 WSL2 ===

# 在默认发行版中执行命令
wsl ls -la
wsl git status
wsl mvn clean package

# 在指定发行版中执行
wsl -d Ubuntu-24.04 -- bash -c "java -jar app.jar"

# 管道互通
echo "data from windows" | wsl md5sum
wsl cat /etc/os-release | Select-String "VERSION"

# 以 root 执行
wsl -u root systemctl status docker

# 批量操作
wsl bash -c '
  cd ~/projects/myapp
  git pull
  mvn clean package -DskipTests
'
```

### 3.3 结合脚本实现跨 OS 工作流

```powershell
# build-and-run.ps1 —— 在 Windows 中一键构建 + 运行 WSL2 项目
param(
    [string]$ProjectPath = "~/projects/myapp"
)

Write-Host "🔄 Pulling latest code..." -ForegroundColor Yellow
wsl bash -c "cd $ProjectPath && git pull"

Write-Host "🔨 Building..." -ForegroundColor Yellow
wsl bash -c "cd $ProjectPath && mvn clean package -DskipTests"

Write-Host "🚀 Starting application..." -ForegroundColor Green
wsl bash -c "cd $ProjectPath && java -jar target/*.jar"

# 运行后自动打开浏览器
Start-Process "http://localhost:8080"
```

```bash
#!/bin/bash
# deploy-to-server.sh —— 在 WSL2 中构建，通过 Windows 工具部署

# 构建（在 WSL2 ext4 上，速度极快）
mvn clean package -DskipTests

# 复制到 Windows 可访问目录
cp target/*.jar /mnt/c/Users/用户名/Desktop/deploy/

# 通过 Windows SSH 客户端部署（如果有特殊配置）
powershell.exe -Command "
    scp C:\Users\用户名\Desktop\deploy\*.jar server:~/
    ssh server 'sudo systemctl restart myapp'
"
```

---

## 4. 环境变量与 PATH 互通

### 4.1 PATH 互通机制

```bash
# WSL2 默认会追加 Windows PATH
echo $PATH
# /usr/local/sbin:/usr/local/bin:...:/mnt/c/Windows/system32:...

# 这意味着可以在 WSL2 中直接运行 Windows 可执行文件！
# 例如：code, notepad.exe, ipconfig.exe...

# 如果不想混入 Windows PATH：
# /etc/wsl.conf
# [interop]
# appendWindowsPath=false

# 手动获取 Windows 环境变量
WIN_USER=$(cmd.exe /c "echo %USERNAME%" 2>/dev/null | tr -d '\r')
echo "Windows user: $WIN_USER"

# 获取 Windows JAVA_HOME
WIN_JAVA=$(cmd.exe /c "echo %JAVA_HOME%" 2>/dev/null | tr -d '\r')
echo "Windows JAVA_HOME: $WIN_JAVA"
```

### 4.2 WSL2 特有的环境变量

```bash
# WSL 注入的环境变量
echo $WSL_DISTRO_NAME      # 当前发行版名称
echo $WSL_INTEROP          # Socket 路径（用于与 Windows 通信）
echo $WSLENV               # Windows→WSL 共享的环境变量列表

# WSLENV 用法（Windows 端设置，传入 WSL2）
# 在 PowerShell 中：
# $env:WSLENV = "MY_VAR/p:OTHER_VAR/u"
# /p = 双向共享, /u = Windows→WSL2 单向

# 场景：让 WSL2 使用 Windows 的 HTTP 代理
# PowerShell:
# $env:HTTP_PROXY = "http://127.0.0.1:7890"
# $env:WSLENV = "HTTP_PROXY/p:HTTPS_PROXY/p:NO_PROXY/p"
# wsl
```

### 4.3 WSLg：共享环境变量与剪贴板

```bash
# WSLg 自动同步以下内容：
# ✅ DISPLAY (Wayland 显示)
# ✅ WAYLAND_DISPLAY
# ✅ PULSE_SERVER (音频)
# ✅ XDG_RUNTIME_DIR

# 手动设置
export DISPLAY=$(cat /etc/resolv.conf | grep nameserver | awk '{print $2}'):0.0

# 剪贴板互通（利用 PowerShell）
alias pbcopy='powershell.exe -Command "Set-Clipboard -Value \$input"'
alias pbpaste='powershell.exe -Command "Get-Clipboard"'

echo "Hello from WSL2" | pbcopy
pbpaste
```

---

## 5. 最佳实践：文件放哪里

### 5.1 决策矩阵

| 文件类型 | 放哪里 | 原因 |
|---------|--------|------|
| **Java 项目代码** | ✅ WSL2 `~/projects/` (ext4) | 编译/构建/I/O 全在高速 ext4 |
| **前端项目 (Node.js)** | ✅ WSL2 `~/projects/` (ext4) | node_modules 小文件多，NTFS 极慢 |
| **Python 项目** | ✅ WSL2 `~/projects/` (ext4) | pip/conda 在 Linux 原生更好 |
| **Docker 数据** | ✅ WSL2 ext4 | Docker Engine 原生 Linux |
| **Maven ~/.m2** | ✅ WSL2 ext4 | 依赖下载在 ext4，速度快 |
| **数据库文件 (SQLite)** | ✅ WSL2 ext4 | 随机读写多，NTFS 不适合 |
| **IDE 配置** | Windows 默认 | IDEA/VS Code 设置不涉及 I/O 性能 |
| **文档/笔记** | Windows 或 WSL2 均可 | 无 I/O 敏感 |
| **照片/视频/大文件** | Windows (NTFS) | 不涉及编译，空间管理方便 |
| **BitTorrent 下载** | Windows (NTFS) | 不涉及开发 |
| **游戏** | Windows (NTFS) | 不涉及开发 |
| **虚拟机镜像** | Windows (NTFS) | 大文件，NTFS 足够 |

### 5.2 推荐的目录结构

```bash
# WSL2 中 (ext4)
~/
├── projects/               ← 所有代码放这里
│   ├── my-java-app/
│   ├── spring-microservices/
│   └── frontend-react/
├── .m2/                    ← Maven 本地仓库
├── .gradle/                ← Gradle 缓存
└── .ssh/                   ← SSH 密钥

# Windows 中 (NTFS)
D:\
├── Documents/              ← 文档、笔记
├── Downloads/              ← 下载
├── Media/                  ← 照片、视频
└── Backup/                 ← wsl --export 备份
```

### 5.3 Git 项目跨系统同步的坑

```text
❌ 错误做法：
  在 /mnt/c/Users/xxx/projects/ 下 git clone
  → git status 慢 60 倍
  → Maven 编译慢 10 倍
  → IDE 索引慢 10 倍

✅ 正确做法：
  cd ~/projects
  git clone git@github.com:user/repo.git
  → 一切操作在 ext4 上，速度正常

✅ 偶尔需要从 Windows 访问代码时：
  explorer.exe ~/projects/myapp
  → 仅在需要时通过 \\wsl$\ 访问
```

> 🎯 **黄金法则**：**WSL2 内的代码用 WSL2 的工具构建，Windows 的代码用 Windows 的工具构建。永远不要交叉。**

---

**上一模块**：[03-WSL2与Java开发环境](./03-WSL2与Java开发环境.md) ｜ **下一模块**：[05-WSL2网络与安全](./05-WSL2网络与安全.md) ｜ **返回总览**：[00-WSL2知识体系总览](./00-WSL2知识体系总览.md)

---

*创建于：2026年7月*
