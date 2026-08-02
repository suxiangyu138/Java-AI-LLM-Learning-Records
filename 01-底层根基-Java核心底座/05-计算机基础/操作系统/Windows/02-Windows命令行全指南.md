# Windows 命令行全指南

> ⌨️ CMD 是过去，PowerShell 是现在，Windows Terminal 是颜值，WSL 是未来 —— 掌握 Windows 命令行的四重境界

---

## 📚 目录

1. [CMD：经典命令解释器](#1-cmd经典命令解释器)
2. [PowerShell：面向对象的现代 Shell](#2-powershell面向对象的现代-shell)
3. [Windows Terminal：新一代终端](#3-windows-terminal新一代终端)
4. [WSL：Windows 上的 Linux](#4-wslwindows-上的-linux)
5. [四者关系与选择指南](#5-四者关系与选择指南)

---

## 1. CMD：经典命令解释器

### 1.1 CMD 的本质

CMD（`cmd.exe`）是 Windows 的命令行解释器，继承自 MS-DOS 的 `COMMAND.COM`。它是**基于文本**的 Shell，将命令输出视为纯文本流。

### 1.2 CMD 常用命令速查

#### 文件与目录操作

| 命令 | 功能 | 示例 | Linux 对应 |
|------|------|------|-----------|
| `dir` | 列出目录 | `dir /s /b *.java` | `ls` |
| `cd` | 切换目录 | `cd /d D:\project` | `cd` |
| `md` / `mkdir` | 创建目录 | `md new-folder` | `mkdir` |
| `rd` / `rmdir` | 删除目录 | `rd /s /q folder` | `rm -rf` |
| `copy` | 复制文件 | `copy a.txt b.txt` | `cp` |
| `move` | 移动文件 | `move a.txt D:\` | `mv` |
| `del` / `erase` | 删除文件 | `del *.log /s` | `rm` |
| `ren` / `rename` | 重命名 | `ren old.txt new.txt` | `mv` |
| `type` | 查看文件内容 | `type pom.xml` | `cat` |
| `tree` | 目录树 | `tree /F /A` | `tree` |

#### 系统与进程

| 命令 | 功能 | 示例 |
|------|------|------|
| `tasklist` | 列出进程 | `tasklist \| findstr java` |
| `taskkill` | 终止进程 | `taskkill /F /PID 1234` |
| `netstat` | 网络连接 | `netstat -ano \| findstr :8080` |
| `systeminfo` | 系统信息 | `systeminfo` |
| `ipconfig` | 网络配置 | `ipconfig /all` |
| `ping` | 网络连通 | `ping -n 4 127.0.0.1` |
| `tracert` | 路由追踪 | `tracert google.com` |
| `nslookup` | DNS 查询 | `nslookup github.com` |

#### 批处理与脚本

```batch
@echo off
REM 这是 CMD 批处理脚本
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
echo JAVA_HOME=%JAVA_HOME%

java -jar target/app.jar --spring.profiles.active=dev
```

### 1.3 CMD 常用技巧

| 技巧 | 命令/操作 | 说明 |
|------|-----------|------|
| **管道** | `dir \| findstr "test"` | 输出筛选 |
| **重定向** | `java -jar app.jar > log.txt 2>&1` | 输出到文件 |
| **连续命令** | `mvn clean && mvn package` | 成功才执行下一个 |
| **并行命令** | `start java -jar app1.jar & start java -jar app2.jar` | 同时启动 |
| **Tab 补全** | 按 Tab 键 | 文件/目录名补全 |
| **命令历史** | `F7` 查看 / `↑↓` 翻历史 | 历史命令 |
| **清屏** | `cls` | 清空终端 |
| **帮助** | `help dir` / `dir /?` | 查看命令帮助 |

> ⚠️ **CMD 局限**：文本输出难以结构化处理，写复杂脚本非常痛苦。以下场景应考虑用 PowerShell 替代。

---

## 2. PowerShell：面向对象的现代 Shell

### 2.1 PowerShell 的核心哲学

```text
CMD:     dir  →  文本 (字符串列表)
Bash:    ls   →  文本 (字符串列表)
PowerShell: Get-ChildItem → 对象数组 (.NET FileInfo 对象)
                      ↓
         每个对象有 .Name .Length .LastWriteTime .Extension 等属性
```

> 🎯 **核心颠覆**：管道传递的不是文本，而是 **.NET 对象**。这意味着你可以直接在管道中访问对象属性，无需 `awk`/`grep`/`cut` 解析文本。

### 2.2 PowerShell 基础命令

#### 命名规范：动词-名词

```powershell
# PowerShell 所有命令都遵循 动词-名词 格式
Get-Process       # 获取进程
Get-Service       # 获取服务
Get-ChildItem     # 列出文件/目录
Set-Location      # 切换目录
New-Item          # 创建文件/目录
Remove-Item       # 删除文件/目录
Copy-Item         # 复制
Move-Item         # 移动
Invoke-WebRequest # HTTP 请求
Write-Output      # 打印
```

#### 常用命令对照

| 操作 | CMD | PowerShell (全名) | PowerShell (别名) | Linux |
|------|-----|-------------------|-------------------|-------|
| 列出文件 | `dir` | `Get-ChildItem` | `ls`, `dir` | `ls` |
| 切换目录 | `cd` | `Set-Location` | `cd` | `cd` |
| 查看内容 | `type` | `Get-Content` | `cat`, `type` | `cat` |
| 查找文本 | `findstr` | `Select-String` | `sls` | `grep` |
| 清屏 | `cls` | `Clear-Host` | `clear`, `cls` | `clear` |
| 进程列表 | `tasklist` | `Get-Process` | `ps`, `gps` | `ps` |
| 终止进程 | `taskkill` | `Stop-Process` | `kill`, `spps` | `kill` |
| 网络连接 | `netstat` | `Get-NetTCPConnection` | — | `netstat` / `ss` |
| 帮助 | `help` | `Get-Help` | `help` | `man` |

### 2.3 管道的威力：对象操作

```powershell
# 1. 管道传递对象，可直接访问属性
Get-ChildItem *.java | Sort-Object Length -Descending | Select-Object -First 5

# 2. 筛选（类似 grep）
Get-Process | Where-Object { $_.WorkingSet64 -gt 100MB }

# 3. 自定义输出（类似 awk）
Get-ChildItem -Recurse *.java | Select-Object Name, Length, @{Name="KB";Expression={[math]::Round($_.Length/1KB,2)}}

# 4. 导出结构化数据
Get-Process | Select-Object Name, Id, WorkingSet64 | Export-Csv -Path processes.csv

# 5. 统计端口占用（Java 开发者高频命令）
Get-NetTCPConnection | Where-Object LocalPort -eq 8080 | Select-Object LocalAddress, LocalPort, OwningProcess
```

### 2.4 对 Java 开发者实用的 PowerShell 脚本

```powershell
# 一键杀掉占用指定端口的进程
$port = 8080
$pid = (Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1).OwningProcess
if ($pid) {
    Stop-Process -Id $pid -Force
    Write-Host "已终止占用端口 $port 的进程 (PID: $pid)"
} else {
    Write-Host "端口 $port 未被占用"
}

# 批量重命名 .java 文件
Get-ChildItem *.java | ForEach-Object {
    $newName = $_.Name -replace 'OldPrefix', 'NewPrefix'
    Rename-Item -Path $_.FullName -NewName $newName
}

# 查找大文件（>100MB）
Get-ChildItem -Recurse -File | Where-Object { $_.Length -gt 100MB } | Sort-Object Length -Descending | Format-Table Name, @{N="Size(MB)";E={[math]::Round($_.Length/1MB,2)}}
```

### 2.5 PowerShell 配置文件

```powershell
# 查看配置文件路径
$PROFILE
# C:\Users\<你的用户名>\Documents\WindowsPowerShell\Microsoft.PowerShell_profile.ps1

# 编辑配置 (用记事本)
notepad $PROFILE

# 示例配置内容
Set-Alias -Name ll -Value Get-ChildItem
Set-Alias -Name g -Value git
Set-Alias -Name mvn -Value mvn

# 自定义 JAVA_HOME 切换函数
function Use-Java8 { $env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_202"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH" }
function Use-Java17 { $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH" }
function Use-Java21 { $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH" }
```

> ⚠️ **执行策略**：如果运行 `.ps1` 脚本时报错"禁止运行脚本"，用管理员身份运行 `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`

---

## 3. Windows Terminal：新一代终端

### 3.1 特性一览

| 特性 | 说明 |
|------|------|
| **多标签页** | CMD / PowerShell / WSL / Azure Cloud Shell 共处一个窗口 |
| **GPU 加速渲染** | DirectWrite 渲染引擎，比传统 conhost 流畅得多 |
| **分屏窗口** | `Alt+Shift+D` 水平分屏，`Alt+Shift+-(minus)` 垂直分屏 |
| **丰富配色** | 内置多种主题 + 自定义配色方案 |
| **Unicode + Emoji** | 完整支持，再也没乱码 |
| **JSON 配置** | `settings.json` 可版本控制、可分享 |

### 3.2 快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl+Shift+T` | 新建标签页 |
| `Ctrl+Shift+W` | 关闭标签页 |
| `Ctrl+Tab` / `Ctrl+Shift+Tab` | 切换标签页 |
| `Alt+Shift+D` | 水平分屏 |
| `Alt+Shift+-` | 垂直分屏 |
| `Alt+方向键` | 切换分屏窗格 |
| `Ctrl+Shift+F` | 搜索 |
| `Ctrl+,` | 打开 settings.json |
| `Ctrl+Shift+P` | 命令面板 |

### 3.3 安装与设置

```powershell
# winget 安装（推荐，Windows 11 已预装）
winget install Microsoft.WindowsTerminal

# 安装后检查
wt --version
```

> 💡 **推荐**：在 VS Code 中设置 `"terminal.integrated.defaultProfile.windows": "PowerShell"`，获得一致体验。

---

## 4. WSL：Windows 上的 Linux

### 4.1 WSL1 vs WSL2

| 对比维度 | WSL1 | WSL2 |
|---------|------|------|
| **架构** | 系统调用翻译层（无完整内核） | 完整 Linux 内核（Hyper-V 轻量 VM） |
| **文件系统性能** | 跨文件系统快（`/mnt/c/` 直接操作） | 跨文件系统较慢（网络挂载） |
| **Linux 内文件性能** | 一般 | **极快**（原生 ext4） |
| **完整内核** | ❌ | ✅ |
| **Docker 支持** | 需要 Docker Desktop | 原生支持 |
| **GPU 加速** | ❌ | ✅ |
| **内存管理** | 共享 Windows 内存 | 独立分配（可限制 `.wslconfig`） |
| **推荐场景** | 跨系统文件操作 | 纯 Linux 开发环境 |

### 4.2 安装与配置

```powershell
# 1. 安装 WSL（一条命令搞定）
wsl --install
# 默认安装 Ubuntu + WSL2

# 2. 查看已安装的发行版
wsl --list --verbose

# 3. 设置默认版本
wsl --set-default-version 2

# 4. 安装特定发行版
wsl --install -d Ubuntu-24.04
wsl --install -d Debian

# 5. 进入 WSL
wsl
# 或指定发行版
wsl -d Ubuntu-24.04
```

### 4.3 WSL 与 Windows 互操作

```bash
# === 在 WSL 中访问 Windows 文件 ===
cd /mnt/c/Users/你的用户名/Desktop
ls -la

# === 在 WSL 中调用 Windows 程序 ===
notepad.exe file.txt          # 用 Windows 记事本打开
code .                        # 用 Windows VS Code 打开当前目录
explorer.exe .                # 用 Windows 资源管理器打开

# === 在 Windows 中调用 WSL 命令 ===
# 在 PowerShell/CMD 中：
wsl ls
wsl git status
```

### 4.4 内存与资源优化

在 `%USERPROFILE%\.wslconfig` 中配置：

```ini
[wsl2]
memory=8GB              # 限制最大内存
processors=4            # 限制 CPU 核心数
swap=4GB                # 交换空间
localhostForwarding=true  # localhost 端口转发
```

### 4.5 开发场景推荐

| 场景 | 推荐方案 | 原因 |
|------|---------|------|
| Java 后端开发 | **Windows 原生** | IDE 全功能、JDK 生态最好 |
| 前端 / Node.js | WSL2 | npm/yarn 在 Linux 上更快 |
| Docker / K8s | WSL2 | 原生 Linux 容器 |
| Python 数据科学 | WSL2 | Linux 下的 Python 生态更完善 |
| 编译 C/Rust | WSL2 | GCC/Make 等工具链天然 Linux |
| Git 操作 | **Windows Git** 或 WSL Git | 跨文件系统操作 Windows 版更快 |

> 🎯 **核心要点**：Java 开发者日常可以 **Windows 原生做主力**，WSL2 作为 Docker、脚本、Linux 测试的**辅助环境**。

---

## 5. 四者关系与选择指南

```text
┌─────────────────────────────────────────────────┐
│              Windows Terminal (统一界面)           │
│  ┌─────────────┐ ┌──────────────┐ ┌───────────┐ │
│  │  CMD 标签页  │ │PowerShell标签页│ │ WSL 标签页│ │
│  │  cmd.exe    │ │  pwsh.exe    │ │  wsl.exe  │ │
│  └──────┬──────┘ └──────┬───────┘ └─────┬─────┘ │
│         │               │               │        │
│    ConHost          ConHost          Linux VM    │
│    (旧终端)         (或新终端)        (WSL2)      │
└─────────────────────────────────────────────────┘
```

| 场景 | 推荐工具 | 理由 |
|------|---------|------|
| 日常文件操作、简单脚本 | PowerShell | 比 CMD 强大，无需安装 |
| 遗留 .bat 脚本维护 | CMD | 原生兼容 |
| 复杂自动化、系统管理 | PowerShell | 对象管道 + .NET 生态 |
| Docker、K8s、Python | WSL2 | 原生 Linux 体验 |
| 终端窗口管理 | Windows Terminal | 统一四者，颜值与效率 |

---

**上一模块**：[01-Windows系统基础与演进](./01-Windows系统基础与演进.md) ｜ **下一模块**：[03-Windows环境配置与Java开发](./03-Windows环境配置与Java开发.md) ｜ **返回总览**：[00-Windows知识体系总览](./00-Windows知识体系总览.md)

---

*创建于：2026年7月*
