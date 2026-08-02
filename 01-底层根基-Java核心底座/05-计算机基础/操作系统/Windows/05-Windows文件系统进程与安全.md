# Windows 文件系统、进程与安全

> 🗄️ NTFS 不只是"存文件的格式"，Windows 进程不只是"任务管理器里的列表" —— 从文件权限到服务管理，从用户账户到防火墙，掌握 Windows 系统管理的核心三件套

---

## 📚 目录

1. [NTFS 文件系统](#1-ntfs-文件系统)
2. [文件权限与 ACL](#2-文件权限与-acl)
3. [进程与线程管理](#3-进程与线程管理)
4. [Windows 服务管理](#4-windows-服务管理)
5. [安全机制与防火墙](#5-安全机制与防火墙)

---

## 1. NTFS 文件系统

### 1.1 Windows 文件系统对比

| 特性 | FAT32 | exFAT | NTFS |
|------|:---:|:---:|:---:|
| **单文件最大** | 4 GB | 理论 16 EB | 理论 16 EB |
| **分区最大** | 2 TB | 128 PB | 256 TB |
| **日志（Journaling）** | ❌ | ❌ | ✅ |
| **权限控制（ACL）** | ❌ | ❌ | ✅ |
| **压缩** | ❌ | ❌ | ✅ |
| **加密（EFS）** | ❌ | ❌ | ✅ |
| **磁盘配额** | ❌ | ❌ | ✅ |
| **硬链接/符号链接** | ❌ | ❌ | ✅ |
| **跨平台兼容** | ✅ 最好 | ✅ 最好 | macOS 只读，Linux 需 ntfs-3g |
| **适用场景** | U 盘（小文件） | 移动存储（大文件） | 系统盘、开发盘 |

### 1.2 NTFS 核心特性

#### 日志（Journaling）

```text
NTFS 的 $LogFile 元文件记录所有元数据更改：

1. 写入前先记日志（Write-Ahead Logging）
2. 写入数据
3. 标记日志完成

→ 断电/崩溃后，通过重放日志恢复到一致状态
→ 类似数据库的 WAL 机制，保证文件系统元数据不损坏
```

#### 硬链接 vs 符号链接 vs 目录链接

| 类型 | 创建命令 | 跨分区 | 目标可以是目录 | 删除目标后 |
|------|---------|:---:|:---:|------|
| **硬链接 (Hard Link)** | `mklink /H` | ❌ | ❌ | 链接仍有效 |
| **符号链接 (Symlink)** | `mklink` | ✅ | ✅ | 链接失效 |
| **目录链接 (Junction)** | `mklink /J` | ❌ | ✅（仅目录） | 链接失效 |

```powershell
# 硬链接：两个"文件名"指向同一个文件数据
mklink /H link.txt original.txt
# → 删除 original.txt，link.txt 仍然能读

# 符号链接：类似 Linux 的 ln -s
mklink link.txt original.txt

# 目录链接：类似 Linux 的目录软链接
mklink /J link-dir target-dir
```

> 💡 **Java 场景**：Maven 本地仓库 `~/.m2/repository` 可以用 `mklink /J` 移到其他分区释放 C 盘空间。

### 1.3 常用文件操作命令

```powershell
# === 磁盘空间分析 ===
# 查看分区信息
Get-PSDrive -PSProvider FileSystem | Format-Table Name, Used, Free, Root

# === 压缩/解压 ===
# 压缩（NTFS 内置压缩，透明使用）
compact /c /s:target-directory
# 解压
compact /u /s:target-directory
# 查看压缩状态
compact /q target-directory

# === 文件属性 ===
# 查看文件属性
Get-Item file.txt | Format-List *
# 修改只读属性
Set-ItemProperty -Path file.txt -Name IsReadOnly -Value $true

# === 磁盘检查与修复 ===
chkdsk C: /f          # 修复文件系统错误
chkdsk C: /r          # 修复 + 检查坏扇区（耗时）
```

---

## 2. 文件权限与 ACL

### 2.1 ACL 模型

```text
Windows ACL 模型（比 Linux 的 rwx 复杂得多）：

安全主体 (Principal)
  ├── 用户 (User)
  ├── 组 (Group)
  └── 特殊身份 (Everyone, SYSTEM, NETWORK SERVICE...)
        │
        ▼
访问控制列表 (ACL)
  ├── DACL (自主访问控制列表) ← 权限设置
  │     └── ACE (访问控制项) × N
  │           ├── 允许/拒绝
  │           ├── 权限：Read, Write, Execute, Delete, Full Control...
  │           └── 继承标志：此文件夹/子文件夹/子文件
  │
  └── SACL (系统访问控制列表) ← 审计日志
```

### 2.2 命令行管理权限

```powershell
# 查看文件/文件夹 ACL
icacls file.txt
icacls D:\project\ /T     # /T 递归

# 输出示例：
# file.txt  BUILTIN\Users:(R)
#           NT AUTHORITY\SYSTEM:(F)
#           BUILTIN\Administrators:(F)

# 权限缩写：
# F  = Full Control (完全控制)
# M  = Modify (修改)
# RX = Read & Execute (读取和执行)
# R  = Read (读取)
# W  = Write (写入)

# 授予权限
icacls file.txt /grant "UserName:(RX)"

# 移除权限
icacls file.txt /remove "UserName"

# 拒绝权限（慎用，Deny 优先级高于 Allow）
icacls file.txt /deny "UserName:(W)"

# 重置继承
icacls file.txt /reset

# 设置所有者
icacls file.txt /setowner "UserName"
```

### 2.3 Java 开发中的权限问题

| 问题 | 现象 | 解决 |
|------|------|------|
| **IDEA 无写入权限** | 无法保存文件、创建项目 | 确保项目目录不在 `C:\Program Files` 下，建议放在用户目录 |
| **Maven 构建报 Access Denied** | target/ 目录无法清理 | 关闭占用文件的进程（idea、java）；`icacls` 重置权限 |
| **Gradle Daemon 无权限** | Gradle 构建失败 | 检查 `~/.gradle/` 目录权限 |
| **跨用户文件访问** | `C:\Users\OtherUser\...` 报错 | 以管理员身份 + 修改 ACL |

> 🎯 **核心要点**：最简单的排错法 —— 在项目根目录右键 → 属性 → 安全 → 确保当前用户有"完全控制"权限。

---

## 3. 进程与线程管理

### 3.1 Windows 进程模型

```text
进程 (Process)
├── 虚拟地址空间 (Virtual Address Space)
├── 可执行程序 (Executable Program)
├── 句柄表 (Handle Table) → 文件、窗口、注册表...
├── 至少一个线程 (Thread)
├── 访问令牌 (Access Token) → 安全上下文
└── 进程 ID (PID)

线程 (Thread)
├── 属于某个进程
├── 有自己的堆栈
├── 与同进程的其他线程共享地址空间
└── 由 NT Kernel 调度器统一调度
```

### 3.2 进程管理命令

```powershell
# === 查看进程 ===
# 所有进程
Get-Process | Sort-Object WorkingSet64 -Descending | Select-Object -First 10

# 特定进程（Java 开发者最爱用的）
Get-Process -Name java
Get-Process -Name java* | Select-Object Id, ProcessName, WorkingSet64, StartTime

# 按端口找进程（超级常用）
Get-NetTCPConnection -LocalPort 8080 | Select-Object LocalAddress, OwningProcess
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess

# === 终止进程 ===
Stop-Process -Name java -Force                          # 终止所有 java 进程
Stop-Process -Id 12345 -Force                           # 按 PID 终止
taskkill /F /PID 12345                                  # CMD 方式
taskkill /F /IM java.exe                                # 按映像名终止

# === 启动进程 ===
Start-Process notepad.exe
Start-Process java -ArgumentList "-jar app.jar --server.port=8080"
```

### 3.3 资源监控

```powershell
# CPU 占用 Top 10
Get-Process | Sort-Object CPU -Descending | Select-Object -First 10 Name, CPU, Id

# 内存占用 Top 10 (单位 MB)
Get-Process | Sort-Object WorkingSet64 -Descending | Select-Object -First 10 `
    @{N="Name";E={$_.ProcessName}}, `
    @{N="Memory(MB)";E={[math]::Round($_.WorkingSet64/1MB,1)}}, `
    Id, StartTime

# 持续监控（类似 top/htop）
# 用图形界面：任务管理器 (Ctrl+Shift+Esc) → 性能
# 命令行实时刷新：
while($true){cls; Get-Process -Name java | Format-Table Id,WorkingSet64,CPU; sleep 2}
```

### 3.4 Java 进程专属技巧

```powershell
# 查看 JVM 进程的启动参数（可以暴露 JAVA_HOME、classpath 等）
Get-WmiObject Win32_Process -Filter "name = 'java.exe'" | Select-Object CommandLine

# 等效命令（PowerShell 5.1+）
Get-CimInstance Win32_Process -Filter "name = 'java.exe'" | Select-Object ProcessId, CommandLine

# 用 jps 查看 Java 进程（JDK 自带工具）
jps -l -v      # -l 显示完整类名，-v 显示 JVM 参数
jps -m          # -m 显示 main 方法参数

# 查看 JVM 堆内存使用
jhsdb jmap --heap --pid <PID>
jcmd <PID> GC.heap_info
```

---

## 4. Windows 服务管理

### 4.1 服务是什么

Windows 服务是在**后台**运行的**长时间存活**的程序，在系统**启动时自动运行**，不依赖用户登录。

```text
Java 开发中常见的 Windows 服务：
├── MySQL / MariaDB 数据库服务
├── Docker Desktop Service
├── Redis (通过第三方工具注册为服务)
├── Jenkins (可注册为服务)
└── WSL 相关服务
```

### 4.2 服务管理命令

```powershell
# === 查看服务 ===
Get-Service                          # 所有服务
Get-Service | Where-Object Status -eq Running   # 运行中的服务
Get-Service -Name mysql*             # 按名称查

# 查看服务详细属性
Get-WmiObject Win32_Service -Filter "Name='MySQL'"

# === 服务操作 ===
Start-Service -Name MySQL            # 启动
Stop-Service -Name MySQL             # 停止
Restart-Service -Name MySQL          # 重启
Set-Service -Name MySQL -StartupType Automatic   # 设为自动启动
Set-Service -Name MySQL -StartupType Manual      # 手动启动
Set-Service -Name MySQL -StartupType Disabled    # 禁用

# === CMD 方式 (sc 命令) ===
sc query MySQL                        # 查询状态
sc start MySQL                        # 启动
sc stop MySQL                         # 停止
sc config MySQL start= auto           # 设为自动启动（注意等号后有空格）
```

### 4.3 将 Java 应用注册为 Windows 服务

方式一：使用 **WinSW** (Windows Service Wrapper) —— 推荐

```xml
<!-- myapp-service.xml -->
<service>
  <id>MyJavaApp</id>
  <name>My Java Application</name>
  <description>Spring Boot 应用服务</description>
  <executable>D:\Programs\Java\jdk-21\bin\java.exe</executable>
  <arguments>-jar D:\apps\myapp.jar --spring.profiles.active=prod</arguments>
  <logpath>D:\apps\logs</logpath>
  <logmode>rotate</logmode>
</service>
```

```powershell
# WinSW 操作
.\WinSW-x64.exe install myapp-service.xml     # 安装服务
.\WinSW-x64.exe start myapp-service.xml       # 启动
.\WinSW-x64.exe stop myapp-service.xml        # 停止
.\WinSW-x64.exe uninstall myapp-service.xml   # 卸载
```

> 💡 **推荐**：WinSW 是 Spring Boot 官方推荐的 Windows 服务化方案，可以将任何 Java 程序包装为 Windows 服务。

方式二：使用 **NSSM** (Non-Sucking Service Manager)

```powershell
# 安装
nssm install MyJavaApp
# → 弹出 GUI 配置：指定 java.exe 路径、jar 路径、参数

# 命令行指定
nssm install MyJavaApp "D:\Programs\Java\jdk-21\bin\java.exe" "-jar D:\apps\myapp.jar"
```

---

## 5. 安全机制与防火墙

### 5.1 用户账户控制 (UAC)

```text
UAC (User Account Control) —— Windows Vista 引入的安全机制：

正常场景：普通用户权限运行
需要提权：弹出"是否允许此应用对你的设备进行更改？"
→ 点击"是" = 获得管理员令牌

开发者影响：
├── idea64.exe 一般不需要管理员
├── 修改 C:\Program Files\ 下的文件 → 需要管理员
├── 监听 1024 以下的端口 → 需要管理员
│      → 解决：使用 1024+ 端口（8080、8443 等）
└── 修改 hosts / 系统环境变量 → 需要管理员
```

### 5.2 Windows 防火墙

```powershell
# === 防火墙状态 ===
Get-NetFirewallProfile | Select-Object Name, Enabled
netsh advfirewall show allprofiles     # netsh 方式

# === 入站规则管理 ===
# 查看所有入站规则
Get-NetFirewallRule -Direction Inbound | Where-Object Enabled -eq True

# 开放端口（Java 开发者常用）
New-NetFirewallRule -DisplayName "Spring Boot 8080" `
    -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow

# 开放端口范围
New-NetFirewallRule -DisplayName "Java Debug 5000-5005" `
    -Direction Inbound -LocalPort 5000-5005 -Protocol TCP -Action Allow

# 删除规则
Remove-NetFirewallRule -DisplayName "Spring Boot 8080"

# === netsh 传统方式 ===
netsh advfirewall firewall add rule name="Allow 8080" dir=in action=allow protocol=TCP localport=8080
netsh advfirewall firewall delete rule name="Allow 8080"
```

### 5.3 Windows Defender（防病毒）

```powershell
# === 临时禁用实时保护（不推荐） ===
Set-MpPreference -DisableRealtimeMonitoring $true

# === 排除目录（强烈推荐，大幅加速开发） ===
# 排除项目目录、Maven 仓库、Node 模块
Add-MpPreference -ExclusionPath "D:\projects"
Add-MpPreference -ExclusionPath "$env:USERPROFILE\.m2\repository"
Add-MpPreference -ExclusionPath "$env:USERPROFILE\.gradle"
Add-MpPreference -ExclusionPath "$env:APPDATA\JetBrains"

# 排除进程
Add-MpPreference -ExclusionProcess "java.exe"
Add-MpPreference -ExclusionProcess "javac.exe"

# 查看排除列表
Get-MpPreference | Select-Object ExclusionPath, ExclusionExtension, ExclusionProcess
```

> ⚠️ **性能影响**：Windows Defender 的实时扫描会显著拖慢 Java 编译和 Maven 构建。建议排除**项目目录、.m2、.gradle、node_modules**，构建速度可提升 30%-50%。

### 5.4 hosts 文件

路径：`C:\Windows\System32\drivers\etc\hosts`

```text
# hosts 文件格式
127.0.0.1       localhost
127.0.0.1       myapp.local
127.0.0.1       api-dev.mydomain.com

# 开发者常用场景：
# 1. 本地开发域名映射
# 2. 屏蔽广告/追踪域名
# 3. 测试多域名路由

# 快捷打开（管理员）
notepad C:\Windows\System32\drivers\etc\hosts
```

---

**上一模块**：[04-Windows包管理与软件生态](./04-Windows包管理与软件生态.md) ｜ **下一模块**：[06-Windows开发者实战手册](./06-Windows开发者实战手册.md) ｜ **返回总览**：[00-Windows知识体系总览](./00-Windows知识体系总览.md)

---

*创建于：2026年7月*
