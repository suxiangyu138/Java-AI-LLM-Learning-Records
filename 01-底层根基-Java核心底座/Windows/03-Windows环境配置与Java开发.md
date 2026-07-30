# Windows 环境配置与 Java 开发

> ☕ JAVA_HOME 为什么配了却不起作用？Path 搜索优先级到底是什么？多 JDK 如何丝滑切换？IDEA 启动太慢怎么办？—— Java 开发者在 Windows 上必须搞懂的环境配置全攻略

---

## 📚 目录

1. [环境变量深度解析](#1-环境变量深度解析)
2. [JDK 安装与 JAVA_HOME 配置](#2-jdk-安装与-java_home-配置)
3. [多 JDK 版本管理与切换](#3-多-jdk-版本管理与切换)
4. [IDE 配置与优化](#4-ide-配置与优化)
5. [常见环境变量速查](#5-常见环境变量速查)

---

## 1. 环境变量深度解析

### 1.1 环境变量的本质

环境变量是操作系统为每个**进程**维护的键值对集合。进程启动时从父进程**继承**环境变量，之后对变量的修改**只影响当前进程及其子进程**。

```text
系统启动
  │
  ├── 读取：系统环境变量 (HKLM\...\Environment)
  ├── 读取：用户环境变量 (HKCU\Environment)
  └── 合并成初始环境块
        │
        └── 每个新进程 copy 一份，独立修改
```

### 1.2 用户变量 vs 系统变量

| 维度 | 用户变量 | 系统变量 |
|------|---------|---------|
| **作用范围** | 当前用户 | 所有用户 |
| **注册表位置** | `HKCU\Environment` | `HKLM\SYSTEM\...\Environment` |
| **设置需要** | 重新登录生效 | 管理员权限 + 重启 |
| **优先级** | **Path 中用户变量在前面** | 系统变量在后面 |
| **推荐用途** | 开发工具路径、个人偏好 | 系统级通用配置 |

> ⚠️ **关键陷阱**：Path 变量的拼接顺序是 **用户 Path 在前 → 系统 Path 在后**。如果有两个 java.exe，用户 Path 中的那个优先被找到。

### 1.3 环境变量生效机制

```text
修改环境变量后，三种生效方式：

1. 重新打开终端（最简单）
   新 cmd/pwsh 窗口会从 explorer.exe 继承最新的环境变量

2. 刷新当前终端
   cmd:  无需特殊操作（但可能有不一致）
   pwsh: $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" +
         [System.Environment]::GetEnvironmentVariable("Path","User")

3. 重启进程（最彻底的情况）
   某些 GUI 程序（如 IDEA）需要通过任务管理器重启
```

### 1.4 命令行操作环境变量

```powershell
# === PowerShell ===
# 临时设置（仅当前会话）
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 永久设置（用户级）
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21", "User")

# 永久设置（系统级，需管理员）
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21", "Machine")

# 查看所有环境变量
Get-ChildItem Env:
```

```batch
:: === CMD ===
:: 临时设置
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%

:: 永久设置
setx JAVA_HOME "C:\Program Files\Java\jdk-21"
setx JAVA_HOME "C:\Program Files\Java\jdk-21" /M   （系统级，需管理员）

:: 查看所有
set
```

---

## 2. JDK 安装与 JAVA_HOME 配置

### 2.1 获取 JDK

| 发行版 | 获取方式 | 特点 |
|--------|---------|------|
| **Oracle JDK** | oracle.com | 官方，商用需授权（JDK 17+ 免费） |
| **OpenJDK** | jdk.java.net | 开源，需要自己解压配置 |
| **Adoptium (Eclipse Temurin)** | adoptium.net | 社区最流行，一键安装包 |
| **Azul Zulu** | azul.com | 嵌入式/物联网支持好 |
| **Microsoft Build of OpenJDK** | microsoft.com/openjdk | 微软维护，Azure 集成 |
| **GraalVM** | graalvm.org | 高性能 Native Image 编译 |

```powershell
# winget 一键安装（推荐）
winget install EclipseAdoptium.Temurin.21.JDK
winget install Microsoft.OpenJDK.21

# 验证安装
java --version
javac --version
```

### 2.2 JAVA_HOME 配置

```powershell
# === 手动配置（GUI）===
# 1. Win + R → sysdm.cpl → 高级 → 环境变量
# 2. 系统变量 → 新建
#    变量名：JAVA_HOME
#    变量值：C:\Program Files\Java\jdk-21
# 3. 编辑 Path → 添加 %JAVA_HOME%\bin

# === 命令行配置（需管理员） ===
[System.Environment]::SetEnvironmentVariable(
    "JAVA_HOME",
    "C:\Program Files\Java\jdk-21",
    "Machine"
)

# 追加到系统 Path
$currentPath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
[System.Environment]::SetEnvironmentVariable(
    "Path",
    "%JAVA_HOME%\bin;$currentPath",
    "Machine"
)
```

### 2.3 验证配置

```powershell
# 依次执行以下命令验证
echo $env:JAVA_HOME           # 应输出 JDK 路径
java --version                # 应输出 Java 版本
javac --version               # 应输出编译器版本
where java                    # 应显示 JAVA_HOME\bin\java.exe
```

### 2.4 常见陷阱

| 陷阱 | 现象 | 解决方案 |
|------|------|---------|
| **JAVA_HOME 末尾多余 `\`** | `%JAVA_HOME%\bin` 变成 `C:\Java\\bin` | 去掉末尾 `\` |
| **Path 中 JAVA_HOME 展开失败** | `echo %JAVA_HOME%` 正确但 `java` 找不到 | 确认 `%JAVA_HOME%\bin` 确实在 Path 中 |
| **Oracle JDK 自动配的 Path** | `C:\Program Files\Common Files\Oracle\Java\javapath` 中的 java.exe 优先级更高 | 将此路径在 Path 中下移或删除 |
| **32位 vs 64位** | `java -version` 显示 32-bit | 确保 Path 中 64 位 JDK 在前面 |
| **空格路径** | `C:\Program Files\Java\...` 某些脚本出错 | 用引号包裹，或安装到无空格路径 |
| **System32 下残留** | `C:\Windows\System32\java.exe` 被找到 | 将 `%JAVA_HOME%\bin` 移到 Path 最前面 |

---

## 3. 多 JDK 版本管理与切换

### 3.1 方案对比

| 方案 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **手动改 Path** | 修改环境变量指向不同 JDK | 无需额外工具 | 每次改全局，繁琐 |
| **PowerShell 函数** | 临时改当前会话的 `$env:JAVA_HOME` | 轻量、即时生效 | 只影响当前终端 |
| **jabba** | 类似 nvm 的 JDK 版本管理器 | 跨平台、自动下载 | 额外安装 |
| **SDKMAN** (WSL) | Linux 下的 JDK 管理器 | 生态最成熟 | 仅限 WSL 环境 |
| **IDE 内置** | IDEA 为每个项目指定 JDK | 项目级隔离 | 命令行不受影响 |

### 3.2 推荐方案：PowerShell 函数 + IDE 项目级

```powershell
# 添加到 $PROFILE 文件
# 多 JDK 路径定义
$JDK_BASE = "D:\Programs\Java"

function global:Use-Java {
    param([string]$version)
    $jdkPath = "$JDK_BASE\jdk-$version"
    if (Test-Path $jdkPath) {
        $env:JAVA_HOME = $jdkPath
        # 移除已有的 Java bin 路径，插入新的
        $env:PATH = ($env:PATH -split ';' | Where-Object { $_ -notlike "*\Java\*" -and $_ -notlike "*\jdk*" }) -join ';'
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
        Write-Host "✅ 已切换至 JDK $version" -ForegroundColor Green
        java --version
    } else {
        Write-Host "❌ 未找到 JDK $version at $jdkPath" -ForegroundColor Red
    }
}

# 快捷别名
function global:j8  { Use-Java "1.8.0_202" }
function global:j11 { Use-Java "11" }
function global:j17 { Use-Java "17" }
function global:j21 { Use-Java "21" }

# 列出所有已安装 JDK
function global:java-list {
    Get-ChildItem $JDK_BASE -Directory -Filter "jdk*" | ForEach-Object {
        $v = & "$($_.FullName)\bin\java" --version 2>&1 | Select-Object -First 1
        Write-Host "$($_.Name) → $v"
    }
}
```

### 3.3 IDEA 项目级 JDK 配置

```
File → Project Structure → Project → SDK
  → Add SDK → JDK → 选择不同版本的 JDK 目录

每个项目可以独立指定：
  - Project SDK（编译用）
  - Project language level（语法级别，如 Java 8/17/21）
  - Maven/Gradle 的 JRE（Settings → Build Tools → Gradle/Maven）
```

> 💡 **最佳实践**：命令行脚本用 PowerShell 函数切换，IDE 项目用 Project SDK 配置，两者互不干扰。

---

## 4. IDE 配置与优化

### 4.1 IntelliJ IDEA 虚拟机配置

路径：`C:\Users\<用户名>\AppData\Roaming\JetBrains\IntelliJIdea<版本>\idea64.exe.vmoptions`

```properties
# 内存（根据物理内存调整，建议不超过物理内存的一半）
-Xms2048m
-Xmx4096m

# 代码缓存（影响代码补全速度）
-XX:ReservedCodeCacheSize=1024m

# 垃圾回收（推荐 G1 或 ZGC）
-XX:+UseG1GC
-XX:SoftRefLRUPolicyMSPerMB=50

# UI 流畅度
-Dsun.java2d.d3d=false
-Dide.win.file.chooser.native=true

# 控制台编码
-Dfile.encoding=UTF-8
-Dconsole.encoding=UTF-8

# 加速启动
-XX:+AlwaysPreTouch
```

### 4.2 IDEA 常用设置

| 设置项 | 路径 | 建议值 |
|--------|------|--------|
| **自动导包** | Editor → General → Auto Import | 勾选 Add unambiguous imports on the fly |
| **内存指示器** | Appearance → Window Options | 勾选 Show memory indicator |
| **防病毒排除** | Windows Defender | 排除项目目录和 IDE 索引目录 |
| **编译运行时** | Build → Compiler | Build project automatically |
| **排除索引** | Project Structure → Modules | 排除 `node_modules`, `.git`, `target` |
| **共享运行配置** | Run → Edit Configurations | 勾选 Share through VCS |

### 4.3 VS Code Java 配置

```json
// settings.json
{
  "java.jdt.ls.java.home": "D:\\Programs\\Java\\jdk-21",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-1.8",
      "path": "D:\\Programs\\Java\\jdk1.8.0_202"
    },
    {
      "name": "JavaSE-17",
      "path": "D:\\Programs\\Java\\jdk-17"
    },
    {
      "name": "JavaSE-21",
      "path": "D:\\Programs\\Java\\jdk-21",
      "default": true
    }
  ],
  "java.jdt.ls.vmargs": "-XX:+UseG1GC -Xmx4g",
  "java.compile.nullAnalysis.mode": "automatic"
}
```

### 4.4 Maven / Gradle 优化

```properties
# Maven: %USERPROFILE%\.m2\settings.xml
<settings>
    <!-- 阿里云镜像（国内加速） -->
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <name>Aliyun Maven</name>
            <url>https://maven.aliyun.com/repository/public</url>
            <mirrorOf>central</mirrorOf>
        </mirror>
    </mirrors>
</settings>
```

```properties
# Gradle: %USERPROFILE%\.gradle\gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
```

---

## 5. 常见环境变量速查

### 5.1 Java 开发相关

| 变量 | 典型值 | 用途 |
|------|--------|------|
| `JAVA_HOME` | `C:\Program Files\Java\jdk-21` | JDK 根目录 |
| `JRE_HOME` | `%JAVA_HOME%` (JDK 11+ 无需单独设置) | JRE 根目录 |
| `CLASSPATH` | 通常**不设置**（让 IDE/工具管理） | 类路径 |
| `MAVEN_HOME` | `C:\Programs\apache-maven-3.9.x` | Maven 根目录 |
| `GRADLE_HOME` | `C:\Programs\gradle-8.x` | Gradle 根目录 |
| `JAVA_TOOL_OPTIONS` | `-Dfile.encoding=UTF-8` | 全局 JVM 参数（慎用） |

### 5.2 通用效率变量

| 变量 | 典型值 | 用途 |
|------|--------|------|
| `Path` | 追加各类 bin 目录 | 命令行在任何目录找到可执行程序 |
| `TEMP` / `TMP` | `%USERPROFILE%\AppData\Local\Temp` | 临时文件目录 |
| `HOME` / `USERPROFILE` | `C:\Users\xxx` | 用户主目录（Git、SSH 等用到） |

> ⚠️ **CLASSPATH 警告**：现代 Java 开发不建议设置全局 CLASSPATH 环境变量，交由 IDE（IDEA/Eclipse）或构建工具（Maven/Gradle）管理类路径更安全。

---

**上一模块**：[02-Windows命令行全指南](./02-Windows命令行全指南.md) ｜ **下一模块**：[04-Windows包管理与软件生态](./04-Windows包管理与软件生态.md) ｜ **返回总览**：[00-Windows知识体系总览](./00-Windows知识体系总览.md)

---

*创建于：2026年7月*
