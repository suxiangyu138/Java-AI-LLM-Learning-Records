# Windows 开发者实战手册

> 🛠️ 端口被占？环境变量不生效？IDEA 启动卡成 PPT？编码乱码？Git 换行符冲突？—— Java 开发者在 Windows 上的十大高频问题，一次性全部解决

---

## 📚 目录

1. [端口相关排错](#1-端口相关排错)
2. [环境变量疑难杂症](#2-环境变量疑难杂症)
3. [编码与字符集问题](#3-编码与字符集问题)
4. [Git 相关配置与排错](#4-git-相关配置与排错)
5. [系统性能优化](#5-系统性能优化)
6. [开发者必备快捷键](#6-开发者必备快捷键)
7. [实用工具推荐](#7-实用工具推荐)
8. [常见 Java 错误的 Windows 特因](#8-常见-java-错误的-windows-特因)

---

## 1. 端口相关排错

### 1.1 端口占用问题（Java 开发者 No.1 高频问题）

```text
错误信息：
  Web server failed to start. Port 8080 was already in use.
  java.net.BindException: Address already in use: bind
```

```powershell
# === Step 1：找谁占用了端口 ===
netstat -ano | findstr :8080
# 输出：TCP  0.0.0.0:8080  0.0.0.0:0  LISTENING  12345

# PowerShell 更优雅的方式
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue |
    Select-Object LocalAddress, LocalPort, OwningProcess, State

# === Step 2：查进程名 ===
Get-Process -Id 12345 | Select-Object Id, ProcessName, Path, StartTime

# === Step 3：杀掉进程 ===
Stop-Process -Id 12345 -Force
# 或 CMD：
taskkill /F /PID 12345

# === 一键脚本（加到 $PROFILE）===
function Kill-Port($port) {
    $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($conn) {
        $conn | ForEach-Object {
            $proc = Get-Process -Id $_.OwningProcess
            Write-Host "终止进程 $($proc.ProcessName) (PID: $($proc.Id)) 占用端口 $port"
            Stop-Process -Id $_.OwningProcess -Force
        }
    } else {
        Write-Host "端口 $port 未被占用"
    }
}
# 使用：Kill-Port 8080
```

### 1.2 端口 TIME_WAIT 释放慢

```powershell
# 查看当前 TIME_WAIT 连接数
netstat -ano | findstr TIME_WAIT | measure

# 解决方案：修改注册表调整 TCP 参数（需管理员）
# HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters
# 添加 DWORD：
# TcpTimedWaitDelay = 30 (十进制，默认 120 秒，最小 30)
```

### 1.3 Hyper-V 端口预留冲突

```powershell
# Windows 上 Docker/WSL 有时会预留大量端口
# 查看 Hyper-V 预留的端口范围
netsh interface ipv4 show excludedportrange protocol=tcp

# 如果 8080 被 Hyper-V 预留，可以：
# 1. 暂时关闭 winnat 然后重启
net stop winnat
net start winnat

# 2. 或者改用其他端口（更推荐）
```

---

## 2. 环境变量疑难杂症

### 2.1 常见症状与诊断

```powershell
# === 诊断命令序列 ===
# 1. 检查当前会话
echo $env:JAVA_HOME           # PowerShell
echo %JAVA_HOME%              # CMD

# 2. 检查系统注册表
Get-ItemProperty "HKLM:\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" | Select-Object JAVA_HOME

# 3. 检查用户注册表
Get-ItemProperty "HKCU:\Environment" | Select-Object JAVA_HOME

# 4. which：到底用的是哪个 java.exe？
where java           # CMD
Get-Command java     # PowerShell

# 5. 输出所有 Path 条目，逐条检查
$env:Path -split ';' | Select-String -Pattern '(?i)java'

# 6. 测试变量展开是否正确
[System.Environment]::ExpandEnvironmentVariables("%JAVA_HOME%\bin")
```

### 2.2 经典问题：改了环境变量但不生效

```text
原因分析：

修改环境变量后，已打开的进程不会自动刷新：
├── 已打开的 cmd/PowerShell → 重新打开
├── 已打开的 IDEA/VSCode → 完全退出重启
├── 已打开的文件资源管理器 → 用 Process Explorer 刷新
│                              或重启 explorer.exe
└── 系统服务 → 重启服务或重启系统

终极方案（需管理员）：
1. 修改后打开 cmd → 输入 set → 看不到新变量 → 重新打开 cmd
2. GUI 设置的环境变量需要点"确定"关闭所有对话框
3. 用 setx 设置后立即在当前终端生效：
   setx JAVA_HOME "C:\path\to\jdk"
   # 但需新终端才能看到
```

### 2.3 Path 太长被截断

```powershell
# 查看 Path 总长度
($env:Path).Length

# Windows 环境变量长度限制：
# 旧版：2047 字符
# Win10 1803+：可更长但有最大 1024 字符的路径名限制

# 解决方案：
# 1. 用短路径替代长路径
# C:\PROGRA~1\Java\jdk-21 代替 C:\Program Files\Java\jdk-21
cmd /c for %A in ("C:\Program Files\Java\jdk-21") do @echo %~sA

# 2. 创建符号链接缩短路径
mklink /J C:\jdk21 "C:\Program Files\Java\jdk-21"
# Path 中直接用 C:\jdk21\bin

# 3. 清理不必要的 Path 条目
# 删除重复的、失效的路径
```

---

## 3. 编码与字符集问题

### 3.1 Windows 编码体系

```text
Windows 编码分层：

系统代码页（ANSI）→ 中文 Windows 默认 GBK (CP936)
控制台代码页        → 默认 GBK，可改为 UTF-8 (chcp 65001)
Java 默认编码      → 取决于系统区域设置，通常 GBK
IDE 控制台编码     → IDEA 可独立设置 UTF-8
```

### 3.2 Java 控制台乱码

```text
常见乱码场景：
├── Maven 构建输出中文乱码
├── IDEA 控制台 Spring Boot 日志乱码
├── logback/log4j 日志文件乱码
└── System.out.println("中文") 输出 ?
```

```powershell
# === 修改控制台代码页 ===
chcp 65001                           # 临时切换为 UTF-8
chcp                                 # 查看当前代码页

# === 永久将控制台设为 UTF-8（Win10 1903+）===
# 设置 → 时间和语言 → 语言和区域 → 管理语言设置
# → 更改系统区域设置 → Beta：使用 Unicode UTF-8 提供全球语言支持
# ⚠️ 会影响部分老软件（不推荐轻易开启）

# === Java 应用强制 UTF-8（推荐方案）===
# 启动时加 JVM 参数
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -jar app.jar

# 或者设置环境变量
$env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
```

### 3.3 IDEA 编码配置

```
IntelliJ IDEA 全链路 UTF-8 设置：

1. File → Settings → Editor → File Encodings
   - Global Encoding: UTF-8
   - Project Encoding: UTF-8
   - Default encoding for properties files: UTF-8
   ✅ Transparent native-to-ascii conversion

2. Help → Edit Custom VM Options
   追加：-Dfile.encoding=UTF-8
        -Dconsole.encoding=UTF-8

3. Run Configuration → 每个启动配置
   VM options: -Dfile.encoding=UTF-8

4. Maven Settings → Runner
   VM Options: -Dfile.encoding=UTF-8

5. idea.properties（idea安装目录/bin/）
   -Dfile.encoding=UTF-8
```

### 3.4 文件编码转换

```powershell
# PowerShell 读取不同编码文件
Get-Content file.txt -Encoding UTF8
Get-Content file.txt -Encoding Default  # 系统默认（GBK on 中文 Win）
Get-Content file.txt -Encoding UTF8NoBOM

# 转换编码
Get-Content input.txt -Encoding Default | Set-Content output.txt -Encoding UTF8

# 批量转换目录下所有 .java 文件
Get-ChildItem -Recurse *.java | ForEach-Object {
    $content = Get-Content $_.FullName -Encoding Default
    [System.IO.File]::WriteAllLines($_.FullName, $content, [System.Text.UTF8Encoding]::new($false))
}
```

---

## 4. Git 相关配置与排错

### 4.1 换行符问题（CRLF vs LF）

```powershell
# === 推荐配置（Windows 开发者）===
git config --global core.autocrlf true
# 工作：检出时 LF→CRLF，提交时 CRLF→LF
# 适合：纯 Windows 开发或 Windows + Linux 混合

# === 其他选项 ===
# 保持原样不转换（不推荐，容易混乱）
git config --global core.autocrlf false

# 提交时转 LF，检出时不转换（Linux 开发用）
git config --global core.autocrlf input

# === 项目级 .gitattributes（推荐团队统一配置）===
# 在项目根目录创建 .gitattributes：
*.java text eol=lf
*.xml text eol=lf
*.properties text eol=lf
*.yaml text eol=lf
*.yml text eol=lf
*.md text eol=lf
*.bat text eol=crlf
*.ps1 text eol=crlf
*.png binary
*.jar binary
```

### 4.2 Git 加速配置

```powershell
# SSH 方式替代 HTTPS（避免每次输密码）
git config --global url."git@github.com:".insteadOf "https://github.com/"
git config --global url."git@gitee.com:".insteadOf "https://gitee.com/"

# 代理配置（科学上网）
git config --global http.proxy http://127.0.0.1:7890
git config --global https.proxy http://127.0.0.1:7890
# 取消代理
git config --global --unset http.proxy
git config --global --unset https.proxy

# 大小写敏感（Windows 文件系统不区分大小写，但 Git 默认区分）
git config --global core.ignorecase false
```

### 4.3 文件权限问题

```powershell
# Windows 下 Git 可能会把所有文件标记为 mode 100755
git config --global core.filemode false
```

---

## 5. 系统性能优化

### 5.1 开发性能黄金配置

| 优化项 | 操作 | 效果 |
|--------|------|------|
| **电源模式** | 设置 → 电源 → **最佳性能** | CPU 不再降频 |
| **防病毒排除** | 排除项目目录 + `.m2` + `.gradle` | 编译速度 ↑30-50% |
| **Windows Search 排除** | 排除开发目录 | 减少磁盘 IO 争抢 |
| **虚拟内存** | 固定大小 = 物理内存 1.5× | 避免动态扩展开销 |
| **视觉效果** | 调整为最佳性能 | 减少 GPU/CPU 占用 |
| **启动程序** | 任务管理器 → 启动 → 禁用不必要的 | 开机更快 |
| **磁盘清理** | `cleanmgr` + 删除 temp 文件 | 释放 C 盘空间 |

### 5.2 内存优化

```powershell
# 查看内存使用
Get-CimInstance Win32_OperatingSystem | Select-Object TotalVisibleMemorySize, FreePhysicalMemory

# 计算可分配给 Java 的内存
# 留 4-8GB 给系统，剩余可分配给 IDE + JVM
# 例：32GB 系统 → 给 IDEA 4GB + 应用 JVM 8GB = 12GB

# IDEA 设置：Help → Edit Custom VM Options
# -Xms2g -Xmx4g
```

### 5.3 磁盘空间管理

```powershell
# === 大目录查找 ===
# 找出 C 盘 Top 10 大文件夹
Get-ChildItem C:\ -Directory -ErrorAction SilentlyContinue |
    ForEach-Object {
        $size = (Get-ChildItem $_.FullName -Recurse -File -ErrorAction SilentlyContinue |
            Measure-Object Length -Sum).Sum
        [PSCustomObject]@{Name=$_.Name; "Size(GB)"=[math]::Round($size/1GB,2)}
    } | Sort-Object "Size(GB)" -Descending | Select-Object -First 10

# === 可安全清理的目录 ===
# %TEMP% (临时文件)
# %USERPROFILE%\.m2\repository (Maven 旧版本)
# %USERPROFILE%\.gradle\caches (Gradle 旧版本)
# %USERPROFILE%\AppData\Local\JetBrains\ (IDE 旧版本)
```

---

## 6. 开发者必备快捷键

### 6.1 Windows 系统级

| 快捷键 | 功能 |
|--------|------|
| `Win + E` | 打开文件资源管理器 |
| `Win + R` | 运行对话框 |
| `Win + X` | 高级用户菜单（设备管理器/磁盘管理/终端） |
| `Win + V` | **剪贴板历史**（比 Ctrl+V 强太多了） |
| `Win + Shift + S` | 截图工具（区域截图） |
| `Win + .` | Emoji + 特殊符号面板 |
| `Win + Tab` | 任务视图 / 虚拟桌面 |
| `Win + Ctrl + D` | 新建虚拟桌面 |
| `Win + Ctrl + ←/→` | 切换虚拟桌面 |
| `Win + Ctrl + F4` | 关闭当前虚拟桌面 |
| `Ctrl + Shift + Esc` | 任务管理器（不用 Ctrl+Alt+Del） |
| `Alt + Tab` | 切换窗口 |
| `Win + 数字键` | 启动/切换任务栏第 N 个程序 |

### 6.2 终端快捷键（Windows Terminal / PowerShell）

| 快捷键 | 功能 |
|--------|------|
| `Ctrl + Shift + T` | 新建标签页 |
| `Ctrl + Shift + W` | 关闭标签页 |
| `Alt + Shift + D` | 水平分屏 |
| `Alt + Shift + -` | 垂直分屏 |
| `Ctrl + Shift + F` | 搜索 |
| `Ctrl + Shift + P` | 命令面板 |
| `Ctrl + Shift + N` | 新窗口 |
| `Shift + ↑/↓` | 向上/向下选择文本 |
| `Ctrl + Shift + ↑/↓` | 向上/向下滚动 |

### 6.3 文件资源管理器

| 快捷键 | 功能 |
|--------|------|
| `F2` | 重命名 |
| `F3 / Ctrl + F` | 搜索 |
| `Alt + Enter` | 查看属性 |
| `Alt + ↑` | 回到上级目录 |
| `Alt + ←/→` | 前进/后退 |
| `Ctrl + Shift + N` | 新建文件夹 |
| `Ctrl + L` | 聚焦地址栏（可直接输路径或 cmd） |
| 地址栏输入 `cmd` 回车 | 当前目录打开 CMD |
| 地址栏输入 `powershell` 回车 | 当前目录打开 PowerShell |
| 地址栏输入 `wt -d .` 回车 | 当前目录打开 Windows Terminal |

---

## 7. 实用工具推荐

### 7.1 微软出品

| 工具 | 用途 | 安装 |
|------|------|------|
| **PowerToys** | 效率工具箱（FancyZones、Color Picker、Text Extractor...） | `winget install Microsoft.PowerToys` |
| **Sysinternals Suite** | 系统底层工具集（Process Explorer、Autoruns...） | `winget install Microsoft.Sysinternals` |
| **Windows Terminal** | 现代终端 | `winget install Microsoft.WindowsTerminal` |
| **Dev Home** | 开发环境管理中心 | `winget install Microsoft.DevHome` |

### 7.2 开发相关

| 工具 | 用途 | 安装 |
|------|------|------|
| **Everything** | 秒级文件搜索 | `winget install voidtools.Everything` |
| **Ditto** | 剪贴板管理增强 | `winget install Ditto.Ditto` |
| **7-Zip** | 压缩解压 | `winget install 7zip.7zip` |
| **QuickLook** | 空格预览文件（macOS 风格） | `winget install QL-Win.QuickLook` |
| **Oh My Posh** | PowerShell 主题美化 | `winget install JanDeDobbeleer.OhMyPosh` |
| **fzf** | 模糊搜索（命令行神器） | `scoop install fzf` |

### 7.3 Oh My Posh 快速配置

```powershell
# 安装
winget install JanDeDobbeleer.OhMyPosh

# 安装字体
oh-my-posh font install FiraCode

# 编辑 $PROFILE，添加：
oh-my-posh init pwsh --config "$env:POSH_THEMES_PATH\jandedobbeleer.omp.json" | Invoke-Expression

# 重新打开终端即可看到美化效果
```

---

## 8. 常见 Java 错误的 Windows 特因

| 错误 | Windows 特有原因 | 解决方案 |
|------|-----------------|---------|
| `Address already in use: bind` | 端口被占用（含 Hyper-V 预留） | [端口排错](#1-端口相关排错) |
| `Could not find or load main class` | CLASSPATH 含中文路径或空格 | 避免中文/空格路径 |
| `Access is denied` (文件创建) | 目录无写入权限 | 放用户目录下；`icacls` 授权 |
| `java.nio.charset.MalformedInputException` | GBK/UTF-8 编码冲突 | 统一 UTF-8 |
| `java.io.FileNotFoundException` | 路径分隔符 `\` vs `/` 问题 | 用 `File.separator` 或 `/` |
| `java.lang.OutOfMemoryError: unable to create new native thread` | Windows 线程数限制（默认约 2000） | 减少线程数或调整系统限制 |
| `Could not reserve enough space for object heap` | 虚拟内存不足 / 32位 Java 限制 | 用 64 位 JDK，增加虚拟内存 |
| IDEA `Cannot resolve symbol` | Windows Defender 锁文件 | 排除项目目录 |
| Gradle `lock timeout` | 文件锁冲突（杀进程不干净） | `taskkill /F /IM java.exe`；删除 `.gradle` 下的 lock 文件 |

---

**上一模块**：[05-Windows文件系统进程与安全](./05-Windows文件系统进程与安全.md) ｜ **返回总览**：[00-Windows知识体系总览](./00-Windows知识体系总览.md)

---

*创建于：2026年7月*
