# 02 JDK 安装与多版本管理

> 一台机器装5个JDK版本是后端开发的日常——掌握多版本管理，告别"在我电脑上能跑"

## 📚 目录

1. [Windows 安装详解](#1-windows-安装详解)
2. [macOS 安装详解](#2-macos-安装详解)
3. [Linux 安装详解](#3-linux-安装详解)
4. [多版本JDK管理工具](#4-多版本jdk管理工具)
5. [Maven/Gradle 中指定JDK版本](#5-mavengradle-中指定jdk版本)
6. [环境变量详解](#6-环境变量详解)
7. [常见安装问题排错](#7-常见安装问题排错)

---

## 1. Windows 安装详解

### 1.1 手动安装（官方方式）

**下载路径：**

| 版本类型 | 下载地址 | 说明 |
|---------|---------|------|
| Oracle JDK | https://www.oracle.com/java/technologies/downloads/ | 商用需付费，个人开发免费 |
| OpenJDK | https://jdk.java.net/ | 官方开源版本，无商业限制 |
| Adoptium (Eclipse Temurin) | https://adoptium.net/ | 社区推荐，最流行的OpenJDK构建 |
| Amazon Corretto | https://aws.amazon.com/corretto/ | AWS 长期支持版本 |
| Azul Zulu | https://www.azul.com/downloads/ | 支持平台最多 |

> 💡 **推荐选择**：开发环境优先使用 **Eclipse Temurin (Adoptium)** 或 **Amazon Corretto**，生产环境使用对应云厂商的JDK构建。

**安装步骤：**

```bash
# 1. 下载 .msi 或 .zip 安装包（推荐 .msi 自动配置环境变量）

# 2. 安装路径规范（避免中文和空格）
#    推荐：C:\Java\jdk-17
#    推荐：D:\Java\jdk-21
#    避免：C:\Program Files\Java\jdk 1.8.0_321（含空格）
#    避免：C:\Users\张三\Java\jdk-17（含中文）

# 3. 配置系统环境变量
#    新建 JAVA_HOME → C:\Java\jdk-17
#    编辑 Path → 添加 %JAVA_HOME%\bin
#    新建 CLASSPATH → .;%JAVA_HOME%\lib\dt.jar;%JAVA_HOME%\lib\tools.jar

# 4. 验证安装
java -version
javac -version
echo %JAVA_HOME%
```

**手动配置环境变量（GUI 方式）：**

```
1. 右键"此电脑" → 属性 → 高级系统设置 → 环境变量
2. 系统变量 → 新建 → 变量名: JAVA_HOME → 变量值: C:\Java\jdk-17
3. 系统变量 → 找到 Path → 编辑 → 新建 → %JAVA_HOME%\bin
   （注意：将 %JAVA_HOME%\bin 移到最顶部，避免被其他Java路径覆盖）
4. 系统变量 → 新建 → 变量名: CLASSPATH → 变量值: .;%JAVA_HOME%\lib\dt.jar;%JAVA_HOME%\lib\tools.jar
```

> ⚠️ **权限问题**：如果遇到 "拒绝访问" 或无法保存环境变量，请以管理员身份运行。修改系统变量需要管理员权限，修改用户变量则不需要。

### 1.2 使用 Chocolatey 包管理器安装

```bash
# 1. 安装 Chocolatey（以管理员身份运行 PowerShell）
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# 2. 安装 JDK
choco install temurin17         # Eclipse Temurin JDK 17
choco install temurin21         # Eclipse Temurin JDK 21
choco install corretto11        # Amazon Corretto 11
choco install zulu8             # Azul Zulu JDK 8

# 3. 查看已安装
choco list --local-only

# 4. 卸载
choco uninstall temurin17
```

> 💡 **Chocolatey 优势**：自动配置环境变量、自动处理依赖、版本切换方便。适合团队统一开发环境标准化。

### 1.3 Windows 安装常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `java` 不是内部或外部命令 | Path 未配置或配置错误 | 检查 `%JAVA_HOME%\bin` 是否在 Path 中 |
| java -version 版本不对 | 多个 JDK 路径冲突 | 检查 Path 中所有 Java 相关路径，将目标版本移到最前 |
| 安装路径含空格报错 | 路径中含 `Program Files` | 重新安装到无空格路径，如 `C:\Java\jdk-17` |
| 中文路径导致编码问题 | Windows 用户名含中文 | 安装到纯英文路径，如 `D:\Java\` |
| 无法安装 .msi | 权限不足 | 右键 → 以管理员身份运行 |
| javac 找不到但 java 可以 | 只配置了 JRE 而非 JDK | 安装完整 JDK，重新配置 JAVA_HOME |

> ⚠️ **Windows 路径优先级**：系统变量 Path > 用户变量 Path。如果系统变量中有 Oracle Java 路径（如 `C:\Program Files\Common Files\Oracle\Java\javapath`），它会被最先搜索到。解决方法：将 `%JAVA_HOME%\bin` 移到 Path 最顶部，或删除 Oracle 自动添加的路径。

---

## 2. macOS 安装详解

### 2.1 使用 Homebrew 安装（最推荐）

```bash
# 1. 更新 Homebrew
brew update

# 2. 安装 JDK
brew install openjdk@17        # OpenJDK 17
brew install openjdk@21        # OpenJDK 21

# 3. 对于 Adoptium Temurin（更稳定）
brew tap adoptium/tap
brew install temurin8           # JDK 8
brew install temurin11          # JDK 11
brew install temurin17          # JDK 17
brew install temurin21          # JDK 21

# 4. 查看已安装的 JDK
/usr/libexec/java_home -V
```

### 2.2 手动配置 .zshrc

```bash
# 编辑配置文件
vi ~/.zshrc

# 添加 JDK 环境变量（以 JDK 17 为例）
export JAVA_HOME_17=$(/usr/libexec/java_home -v 17)
export JAVA_HOME_21=$(/usr/libexec/java_home -v 21)
export JAVA_HOME=$JAVA_HOME_17
export PATH=$JAVA_HOME/bin:$PATH

# 添加快捷切换别名
alias jdk8='export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); java -version'
alias jdk11='export JAVA_HOME=$(/usr/libexec/java_home -v 11); java -version'
alias jdk17='export JAVA_HOME=$(/usr/libexec/java_home -v 17); java -version'
alias jdk21='export JAVA_HOME=$(/usr/libexec/java_home -v 21); java -version'

# 生效配置
source ~/.zshrc
```

### 2.3 `/usr/libexec/java_home` 详解

```bash
# 列出所有 JDK
/usr/libexec/java_home -V

# 获取特定版本路径
/usr/libexec/java_home -v 17
# 输出：/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# 获取特定架构的 JDK
/usr/libexec/java_home -v 17 --arch arm64

# 在脚本中使用
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

> 💡 `java_home` 是 macOS 独有的 JDK 路径查询工具，它会扫描 `/Library/Java/JavaVirtualMachines/` 目录下的所有 JDK，按版本号排序返回最匹配的路径。

### 2.4 macOS 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `brew install` 后 `java` 仍找不到 | Homebrew 安装的是 JDK 但未链接 | 执行 `sudo ln -sfn /usr/local/opt/openjdk/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk.jdk` |
| `JAVA_HOME` 未生效 | shell 配置文件未正确加载 | 检查 `.zshrc` 或 `.bash_profile` 配置，执行 `source` |
| macOS 升级后 Java 不可用 | 系统更新删除了 JDK 符号链接 | 重新安装 JDK 或重建符号链接 |
| Apple Silicon (M1/M2/M3) 兼容性 | ARM 架构 JDK 支持不足 | 使用 Azul Zulu 或 Adoptium 的 ARM 版本；Rosetta 2 可运行 x86 JDK |

> ⚠️ **macOS 安全提示**：如果安装后提示 "无法验证开发者"，请前往 **系统设置 → 隐私与安全性 → 仍要打开**，或使用 `sudo spctl --master-disable` 临时关闭 Gatekeeper（不推荐长期关闭）。

---

## 3. Linux 安装详解

### 3.1 使用包管理器安装

```bash
# === Ubuntu / Debian (apt) ===
# 更新源
sudo apt update

# 安装 OpenJDK
sudo apt install openjdk-17-jdk    # JDK 17
sudo apt install openjdk-21-jdk    # JDK 21

# 安装完成后自动配置，默认安装路径：/usr/lib/jvm/java-17-openjdk-amd64

# === CentOS / RHEL / Fedora (dnf/yum) ===
# CentOS 7/8 使用 yum
sudo yum install java-17-openjdk-devel

# CentOS 8+ / Fedora 使用 dnf
sudo dnf install java-17-openjdk-devel
sudo dnf install java-21-openjdk-devel

# 默认安装路径：/usr/lib/jvm/java-17-openjdk-<arch>/
```

### 3.2 手动安装（tar.gz 方式）

当需要特定版本（如 Oracle JDK）或服务器无外网时使用：

```bash
# 1. 下载 JDK tar.gz
wget https://download.java.net/java/GA/jdk17/.../openjdk-17_linux-x64_bin.tar.gz

# 2. 解压到指定目录
sudo mkdir -p /usr/local/java
sudo tar -xzf openjdk-17_linux-x64_bin.tar.gz -C /usr/local/java/

# 3. 配置环境变量（/etc/profile 或 ~/.bashrc）
cat >> ~/.bashrc << 'EOF'
export JAVA_HOME=/usr/local/java/jdk-17
export PATH=$JAVA_HOME/bin:$PATH
export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
EOF

# 4. 生效
source ~/.bashrc
```

### 3.3 `update-alternatives` 管理多版本

```bash
# 1. 安装多个 JDK 后，注册到 alternatives
sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-17-openjdk-amd64/bin/java 171
sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-11-openjdk-amd64/bin/java 111
sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-17-openjdk-amd64/bin/javac 171
sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-11-openjdk-amd64/bin/javac 111

# 2. 交互式切换版本
sudo update-alternatives --config java
sudo update-alternatives --config javac

# 3. 非交互式设置版本
sudo update-alternatives --set java /usr/lib/jvm/java-17-openjdk-amd64/bin/java
sudo update-alternatives --set javac /usr/lib/jvm/java-17-openjdk-amd64/bin/javac

# 4. 查看当前版本
update-alternatives --display java
```

> ⚠️ **重要提示**：`update-alternatives` 管理的只是符号链接。如果需要全面切换 JDK，还需要同步更新 `JAVA_HOME` 环境变量。建议配合 `~/.bashrc` 中的别名使用。

### 3.4 服务器无 GUI 安装最佳实践

```bash
# 1. 使用 minimal JDK（不含 JavaFX、演示等）
sudo apt install openjdk-17-jdk-headless

# 2. 验证安装（无图形环境）
java -version
javac -version

# 3. 设置系统级 JAVA_HOME
echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64" | sudo tee /etc/profile.d/java.sh
echo "export PATH=\$PATH:\$JAVA_HOME/bin" | sudo tee -a /etc/profile.d/java.sh
sudo chmod +x /etc/profile.d/java.sh

# 4.  Docker 容器中的 JDK（推荐使用 slim 镜像）
FROM eclipse-temurin:17-jre-alpine
# 或
FROM amazoncorretto:17-alpine
```

| JDK 版本 | Ubuntu 包名 | CentOS 包名 | 镜像体积（约） |
|---------|------------|------------|-------------|
| JDK 8 | openjdk-8-jdk | java-1.8.0-openjdk-devel | 200 MB |
| JDK 11 LTS | openjdk-11-jdk | java-11-openjdk-devel | 250 MB |
| JDK 17 LTS | openjdk-17-jdk | java-17-openjdk-devel | 280 MB |
| JDK 21 LTS | openjdk-21-jdk | java-21-openjdk-devel | 300 MB |
| JDK 8 Headless | openjdk-8-jdk-headless | java-1.8.0-openjdk-headless | 120 MB |
| JDK 17 Headless | openjdk-17-jdk-headless | java-17-openjdk-headless | 150 MB |

> 💡 **服务器推荐**：生产环境使用 **Headless** 版本（无 GUI 依赖），配合 **Docker** 容器化部署。使用 `jlink` 定制最小 JRE 可将镜像体积进一步缩减至 40-60 MB。

---

## 4. 多版本 JDK 管理工具

### 4.1 SDKMAN!（Mac/Linux 最推荐）

SDKMAN! 是目前最流行的 JDK 版本管理工具，支持 Linux 和 macOS，可管理 Java、Groovy、Kotlin、Scala、Maven、Gradle 等 SDK。

**安装：**

```bash
# 1. 安装 SDKMAN!
curl -s "https://get.sdkman.io" | bash

# 2. 安装完成后执行
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 3. 验证安装
sdk version
```

**常用命令：**

| 命令 | 说明 | 示例 |
|------|------|------|
| `sdk list java` | 列出所有可安装的 JDK 发行版 | 显示 40+ 种 JDK 版本 |
| `sdk install java 17.0.9-tem` | 安装指定版本的 Temurin JDK | 自动下载并配置 |
| `sdk install java 21.0.1-zulu` | 安装 Zulu JDK 21 | 多发行版支持 |
| `sdk use java 17.0.9-tem` | 当前 shell 临时切换 | 仅当前终端生效 |
| `sdk default java 21.0.1-zulu` | 设置全局默认版本 | 新终端默认使用 |
| `sdk current java` | 查看当前使用的版本 | 实时显示 |
| `sdk uninstall java 11.0.21-tem` | 卸载指定版本 | 清理磁盘空间 |
| `sdk upgrade java` | 升级到当前发行版的最新小版本 | 保持更新 |

**发行版标识符速查：**

```bash
# 查看所有可用发行版
sdk list java | grep "vendor"

# 常见发行版标识
tem    # Eclipse Temurin（推荐）
zulu   # Azul Zulu
cor    # Amazon Corretto
graal  # GraalVM
oracle # Oracle JDK
open   # OpenJDK
```

**实战：管理多个 JDK 版本**

```bash
# 安装多个版本
sdk install java 8.0.392-tem
sdk install java 11.0.21-tem
sdk install java 17.0.9-tem
sdk install java 21.0.1-tem

# 项目目录自动切换（.sdkmanrc）
cd my-project
sdk env init    # 创建 .sdkmanrc 文件

# .sdkmanrc 内容示例：
# Enable auto-env through sdkman_auto_env=true
# java=17.0.9-tem

# 进入目录自动切换版本（需开启 auto_env）
sdk config  # 设置 sdkman_auto_env=true
```

> 💡 **SDKMAN! 优势**：自动管理 PATH 和 JAVA_HOME，无需手动配置；支持 40+ 种 JDK 发行版；支持项目级版本自动切换；还可管理 Maven/Gradle/Grails 等工具。

### 4.2 jEnv（按项目自动切换 JDK 版本）

jEnv 是一个轻量级的 Java 环境管理器，通过目录层级自动切换 JDK 版本，适合 Mac/Linux。

```bash
# 安装
brew install jenv

# 初始化
echo 'export PATH="$HOME/.jenv/bin:$PATH"' >> ~/.zshrc
echo 'eval "$(jenv init -)"' >> ~/.zshrc
source ~/.zshrc

# 添加已安装的 JDK
jenv add /Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
jenv add /Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home

# 查看已添加的版本
jenv versions

# 设置全局版本
jenv global 17.0

# 设置当前 shell 版本
jenv shell 11.0

# 设置目录级别的版本（核心功能）
cd /path/to/my-project
jenv local 17.0    # 创建 .java-version 文件
cat .java-version  # 内容：17.0

# 之后的 Maven/Gradle 命令自动使用 JDK 17
```

| 场景 | 命令 | 优先级 |
|------|------|--------|
| 全局默认 | `jenv global 17.0` | 最低 |
| 当前 shell | `jenv shell 11.0` | 中 |
| 项目目录 | `jenv local 17.0` | 最高 |

> ⚠️ **jEnv 注意点**：jEnv 管理的仅仅是 JAVA_HOME，不会安装 JDK 本身，需要提前通过 brew 或其他方式安装 JDK。某些场景下需要启用 `jenv enable-plugin export` 来确保 JAVA_HOME 变量被正确设置。

### 4.3 Windows 手动环境变量切换 + 批处理脚本

Windows 没有 SDKMAN! 的原生支持，可以使用批处理脚本实现快速切换：

**方案一：批处理脚本（`jdk-switch.bat`）**

```bat
@echo off
chcp 65001 >nul
echo ========================================
echo          JDK 版本切换工具
echo ========================================
echo  1) JDK 8  (C:\Java\jdk-8)
echo  2) JDK 11 (C:\Java\jdk-11)
echo  3) JDK 17 (C:\Java\jdk-17)
echo  4) JDK 21 (C:\Java\jdk-21)
echo ========================================
set /p choice="请选择 JDK 版本 (1-4): "

if "%choice%"=="1" set JAVA_HOME=C:\Java\jdk-8
if "%choice%"=="2" set JAVA_HOME=C:\Java\jdk-11
if "%choice%"=="3" set JAVA_HOME=C:\Java\jdk-17
if "%choice%"=="4" set JAVA_HOME=C:\Java\jdk-21

if "%JAVA_HOME%"=="" (
    echo 无效选择！
    pause
    exit /b 1
)

:: 设置系统环境变量（需管理员权限）
setx JAVA_HOME "%JAVA_HOME%" /M

echo.
echo JDK 已切换至：%JAVA_HOME%
java -version
pause
```

**方案二：PowerShell 脚本（`switch-jdk.ps1`）**

```powershell
# 以管理员身份运行
param([string]$version = "17")

$jdkPaths = @{
    "8"  = "C:\Java\jdk-8"
    "11" = "C:\Java\jdk-11"
    "17" = "C:\Java\jdk-17"
    "21" = "C:\Java\jdk-21"
}

if (-not $jdkPaths.ContainsKey($version)) {
    Write-Error "支持的版本：8, 11, 17, 21"
    exit 1
}

$newHome = $jdkPaths[$version]
[Environment]::SetEnvironmentVariable("JAVA_HOME", $newHome, "Machine")

# 刷新 Path
$oldPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
$newPath = ($oldPath -split ";" | Where-Object {$_ -notmatch "jdk"}) -join ";"
$newPath = "$newHome\bin;$newPath"
[Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")

Write-Host "JDK 已切换至：$newHome" -ForegroundColor Green
java -version
```

> 💡 **Windows 最佳实践**：保留 JDK 安装包在特定目录（如 `C:\Java\`），通过修改 JAVA_HOME 和 Path 实现切换。配合 IDEA 的 Project SDK 功能，每个项目可独立指定 JDK。

### 4.4 IntelliJ IDEA 多 JDK 配置

**添加 JDK：**

```
File → Project Structure (Ctrl+Shift+Alt+S) → SDKs → +
  → Add JDK → 选择 JDK 根目录 (如 C:\Java\jdk-17)
```

**Project SDK（项目级）：**

```text
File → Project Structure → Project → SDK
  → 选择项目使用的 JDK 版本

File → Project Structure → Project → Language level
  → 选择 Java 语言级别（如 17 - Sealed types, preview）
```

**Global SDK（全局默认）：**

```text
File → New Projects Setup → Structure → Global SDK
  → 设置新建项目的默认 JDK
```

**Gradle JVM：**

```text
File → Settings → Build, Execution, Deployment → Build Tools → Gradle
  → Gradle JVM → 选择 Gradle 运行使用的 JDK
```

**Maven JVM：**

```text
File → Settings → Build, Execution, Deployment → Build Tools → Maven
  → Runner → JRE
```

> 💡 **多版本同时开发技巧**：在 IDEA 中同时打开多个项目，每个项目配置不同的 JDK。使用 `File → Settings → Build Tools → Gradle → Gradle JVM` 为 Gradle 守护进程指定独立的 JDK，避免与项目 JDK 冲突。

---

## 5. Maven/Gradle 中指定 JDK 版本

### 5.1 Maven `maven-compiler-plugin` 配置

```xml
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <maven.compiler.release>17</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<!-- 或通过 plugin 显式配置 -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.12.1</version>
            <configuration>
                <source>17</source>
                <target>17</target>
                <release>17</release>
                <encoding>UTF-8</encoding>
                <showWarnings>true</showWarnings>
                <showDeprecation>true</showDeprecation>
                <!-- 注解处理器 -->
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.30</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**编译版本 vs 运行版本 区别：**

| 概念 | 说明 | 配置参数 |
|------|------|---------|
| `source` | 源码兼容的 Java 版本 | 决定允许使用的语言特性 |
| `target` | 生成的 class 文件版本 | 决定最低兼容的 JVM 版本 |
| `release` | Java 9+ 新参数 | 同时控制 source + target + 编译时的 API 限制 |
| `--release` | 编译时绑定平台 API | 防止使用了高版本的 API |

```bash
# 使用不同 JDK 编译（指定 JDK 路径）
mvn clean compile -Dmaven.compiler.release=17 -Djava.home=C:\Java\jdk-17

# 查看编译详细输出
mvn clean compile -X
```

> ⚠️ **source/target 陷阱**：设置 `source=8` 但运行时 JDK 版本包含了高版本 API，编译不会报错但运行时可能出错。推荐使用 `release` 参数（Java 9+），它同时限制了 API 调用。

### 5.2 Gradle Java Toolchain

Gradle 7.0+ 提供了 **Java Toolchain** 机制，可以显式指定编译、测试、运行使用的 JDK 版本，无需依赖 `JAVA_HOME`。

```groovy
// build.gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.ADOPTIUM  // 可选：指定 JDK 发行商
    }
}

// 也可以配置多个 JVM 任务
tasks.withType(JavaCompile).configureEach {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType(Test).configureEach {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType(Javadoc).configureEach {
    javadocTool = javaToolchains.javadocToolFor {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
```

```kotlin
// build.gradle.kts
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}
```

**Gradle 自动检测 JDK：**

```bash
# 查看可用的 JDK 工具链
gradle -q javaToolchains

# 输出示例：
# + Options
#     | Auto-detected: true
#     | Auto-downloaded: false
# + JDK 17 (17.0.9)
#     | Location: /Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
#     | Language Version: 17
#     | Vendor: Adoptium
#     | Is JDK: true
# + JDK 21 (21.0.1)
#     | Location: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
#     | Language Version: 21
#     | Vendor: Oracle
#     | Is JDK: true
```

> 💡 **Toolchain 优势**：项目内部声明式指定 JDK 版本，CI/CD 环境无需前置安装 JDK（Gradle 可以自动下载），团队成员无需手动切换本地 JDK。

### 5.3 编译版本 vs 运行版本全景对比

| 层面 | 编译时 | 运行时 |
|------|--------|--------|
| JDK 版本 | `javac` 的版本 | `java` 的版本 |
| Java 语言特性 | 由 `--source` 或 `--release` 控制 | 与 JVM 无关 |
| class 文件格式 | 由 `--target` 或 `--release` 控制 | JVM 必须兼容此 class 版本 |
| API 可用性 | 由 `--release` 或 `--class-path` 控制 | 运行时实际 classpath 中的类 |
| 典型场景 | JDK 17 编译 target 8 | JRE 8 上运行 |

```bash
# 典型交叉编译场景：JDK 17 编译可在 JDK 8 上运行的程序
javac -source 8 -target 8 -bootclasspath /path/to/jdk8/rt.jar MyApp.java

# Java 9+ 推荐方式
javac --release 8 MyApp.java
```

> 🎯 **核心要点**：开发环境使用 JDK 17+，生产环境使用 JDK 17 LTS 或 21 LTS。编译时指定 target 版本不高于运行时的 JVM 版本。推荐使用 Gradle Toolchain 或 Maven Toolchain 实现构建工具与 JDK 解耦。

---

## 6. 环境变量详解

### 6.1 `JAVA_HOME` vs `PATH` vs `CLASSPATH`

| 环境变量 | 作用 | 配置示例 | 是否必须 |
|---------|------|---------|---------|
| `JAVA_HOME` | JDK 安装根目录，供其他工具（Maven、Tomcat、IDEA）查找 JDK | `C:\Java\jdk-17` | **是** |
| `PATH` | 操作系统搜索可执行文件的路径 | `%JAVA_HOME%\bin` | **是** |
| `CLASSPATH` | JVM 搜索用户类文件的路径 | `.;%JAVA_HOME%\lib\dt.jar` | **否**（Java 5+ 推荐不设置） |

### 6.2 详细说明与最佳实践

**JAVA_HOME：**

- 被 Tomcat、Maven、Gradle、Jenkins、Eclipse 等工具读取
- 命名必须完全大写，下划线连接
- 值指向 JDK 根目录（包含 bin、lib、jre 等子目录）
- 建议不要以 `\`（Windows）或 `/`（Linux）结尾

```bash
# Windows
JAVA_HOME=C:\Java\jdk-17

# macOS / Linux
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

**PATH 配置：**

```bash
# Windows
%JAVA_HOME%\bin    # 推荐：引用 JAVA_HOME，只需修改一处

# macOS / Linux
$JAVA_HOME/bin     # 推荐放在 PATH 最前面：export PATH=$JAVA_HOME/bin:$PATH
```

> ⚠️ **PATH 顺序陷阱**：`PATH` 中 `%JAVA_HOME%\bin` 的位置决定了哪个 Java 被优先使用。如果系统自带 Java 的路径在你的配置之前，`java -version` 可能显示不期望的版本。**解决方案**：将 `%JAVA_HOME%\bin` 或 `$JAVA_HOME/bin` 置于 PATH 最前面。

**CLASSPATH：**

```bash
# 经典配置（JDK 5 之前必须）
CLASSPATH=.;%JAVA_HOME%\lib\dt.jar;%JAVA_HOME%\lib\tools.jar

# 现代最佳实践（JDK 5+）：不设置 CLASSPATH
# 使用 -cp 或 -classpath 参数替代
javac -cp lib/*:. MyApp.java
java -cp lib/*:. MyApp
```

> 💡 **为什么现代 Java 不推荐 CLASSPATH？** Java 5+ 支持 `-cp` 和 `-classpath` 参数，IDE 和构建工具（Maven/Gradle）自动管理 classpath。全局 CLASSPATH 反而容易导致版本冲突和类加载问题。**最佳实践**：不设置 `CLASSPATH` 系统变量，在项目中用构建工具管理依赖。

### 6.3 各操作系统环境变量配置对比

| 操作 | Windows | macOS | Linux |
|------|---------|-------|-------|
| 配置文件 | 系统属性 GUI | `~/.zshrc` / `~/.bash_profile` | `~/.bashrc` / `/etc/profile` |
| 设置 JAVA_HOME | GUI 新建系统变量 | `export JAVA_HOME=/path` | `export JAVA_HOME=/path` |
| 设置 PATH | GUI 编辑 Path | `export PATH=$JAVA_HOME/bin:$PATH` | 同 macOS |
| 立即生效 | 重新打开 CMD/PowerShell | `source ~/.zshrc` | `source ~/.bashrc` |
| 系统范围 | 系统变量对所有用户生效 | 修改 `/etc/profile` | 修改 `/etc/profile` |
| 验证命令 | `echo %JAVA_HOME%` | `echo $JAVA_HOME` | `echo $JAVA_HOME` |

---

## 7. 常见安装问题排错

### 7.1 版本冲突

**现象：** `java -version` 显示 A 版本，但 `javac -version` 显示 B 版本。

**原因：** `java` 和 `javac` 来自不同的 JDK 安装。

**排查步骤：**

```bash
# Windows
where java
where javac

# macOS / Linux
which java
which javac
# 输出示例：
# /usr/bin/java          → 系统自带的 Java
# /usr/local/bin/javac   → 手动安装的 JDK
```

**解决方案：**

1. 确保 `JAVA_HOME` 指向正确的 JDK
2. 确保 `PATH` 中 `%JAVA_HOME%\bin` 在所有其他 Java 路径之前
3. 删除第三方软件自动添加的 Java 路径（如 Oracle Java 自动更新路径、Adobe 产品自带的 JRE）

### 7.2 `java -version` 显示的不是我安装的版本

**排查清单：**

```bash
# 1. 检查 JAVA_HOME 是否正确
echo %JAVA_HOME%        # Windows
echo $JAVA_HOME         # macOS/Linux

# 2. 检查 PATH 中所有 Java 相关路径
echo %PATH:;=&echo.% | findstr java    # Windows
echo $PATH | tr ':' '\n' | grep -i java  # macOS/Linux

# 3. 检查是否有其他软件自带的 JRE 在 PATH 前面
# Windows: C:\Program Files\Common Files\Oracle\Java\javapath
# macOS: /usr/bin/java (系统自带/符号链接)
# Linux: /usr/bin/java (alternatives 链接)

# 4. 检查符号链接指向
ls -la /usr/bin/java*   # macOS/Linux
```

### 7.3 `java` 命令找不到

| 系统 | 可能原因 | 解决方案 |
|------|---------|---------|
| Windows | Path 未配置 `%JAVA_HOME%\bin` | 重新配置 Path |
| Windows | 环境变量修改后未重启终端 | 重新打开 CMD/PowerShell |
| macOS | `~/.zshrc` 未正确配置 | 检查配置并 `source` |
| macOS | Homebrew 安装后未创建符号链接 | `sudo ln -sfn ...` 创建链接 |
| Linux | `update-alternatives` 未注册 | 使用 `update-alternatives --install` |
| Linux | 安装的是 JRE 而非 JDK | 安装 `java-17-openjdk-devel` |

### 7.4 IDE 中 JDK 选择问题

**IntelliJ IDEA 常见问题：**

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 项目报 "Java: invalid source release" | Language level 不对 | Project Structure → Project → Language level |
| Maven 编译 JDK 版本错 | Maven 运行 JDK 与项目 JDK 不一致 | Settings → Maven → Runner → JRE |
| Gradle 构建版本不对 | Gradle JVM 未指定 | Settings → Gradle → Gradle JVM |
| IDE 找不到我的 JDK | JDK 路径不被 IDEA 自动检测 | File → Project Structure → SDKs → + Add JDK |
| Lombok 不生效 | 注解处理器未配置 | File → Settings → Annotation Processors → Enable |

**统一排错流程：**

```text
1. 检查终端 java -version → 确认系统 JDK
2. 检查 IDE 中 Project SDK → 确认 IDE JDK
3. 检查 Maven/Gradle 的 JDK 配置 → 确认构建工具 JDK
4. 检查 pom.xml / build.gradle 中的 source/target → 确认编译参数
5. 运行 mvn -version / gradle -v → 确认构建工具自身 JDK
```

> 🎯 **核心要点**：一台机器管理多个 JDK 版本的标准流程：
> 1. 使用 SDKMAN!（Mac/Linux）或手动批处理脚本（Windows）管理 JDK 安装和切换
> 2. 构建工具使用 Gradle Toolchain 或 Maven Toolchain 声明式指定 JDK 版本
> 3. IDE 中为每个项目独立配置 Project SDK
> 4. 遇到版本问题时，按"终端 → IDE → 构建工具 → 编译参数"逐层排查
> 5. 团队统一使用 `.sdkmanrc` 或 `.java-version` 文件锁定 JDK 版本

---

**下一模块**：[03 JDK核心工具链实战](./03-JDK核心工具链实战.md) | **返回总览**：[总览](./00-JDK知识体系总览.md)
