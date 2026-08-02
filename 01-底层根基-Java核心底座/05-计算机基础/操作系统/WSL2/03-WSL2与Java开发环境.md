# WSL2 与 Java 开发环境

> ☕ 在 WSL2 的 Linux 中安装 JDK、Maven、Gradle、Docker —— 利用 SDKMAN 管理多 JDK、IDEA/VS Code 深度集成、Docker Desktop WSL2 Backend 原生容器，构建终极 Java 开发环境

---

## 📚 目录

1. [Java 开发选型：Windows 原生 vs WSL2](#1-java-开发选型windows-原生-vs-wsl2)
2. [SDKMAN：JDK 版本管理神器](#2-sdkmanjdk-版本管理神器)
3. [Maven / Gradle 配置](#3-maven--gradle-配置)
4. [IDE 集成：IDEA + VS Code](#4-ide-集成idea--vs-code)
5. [Docker Desktop WSL2 Backend](#5-docker-desktop-wsl2-backend)
6. [完整开发环境初始化脚本](#6-完整开发环境初始化脚本)

---

## 1. Java 开发选型：Windows 原生 vs WSL2

| 维度 | Windows 原生 | WSL2 |
|------|:---:|:---:|
| **JDK 安装** | 手动下载/配置 PATH | `sdk install java` 一条命令 |
| **版本切换** | 手动改 JAVA_HOME | `sdk use java 21-tem` 秒切 |
| **Maven/Gradle** | 手动配置 | apt/sdkman 安装 + 镜像 |
| **Docker** | Docker Desktop (Windows) | Docker Engine 原生 Linux |
| **编译速度** | 中等（NTFS） | **快**（ext4，SSD 直接读写） |
| **Shell 脚本** | CMD/PowerShell 兼容差 | ✅ 原生 bash/zsh |
| **CI/CD 一致性** | ❌ 与 Linux 服务器不同 | ✅ 与服务器环境一致 |
| **IDE 体验** | ✅ 原生 GUI | ⚠️ 通过 WSLg 或 Windows IDE Remote |
| **推荐方案** | **IDE 在 Windows，代码 + 工具链在 WSL2** | |

> 🎯 **最佳实践**：IDEA/VS Code 用 Windows 原生（GUI 流畅），项目代码放在 WSL2 的 ext4 上（构建快），通过 IDE 的 WSL2 Remote/Integration 打通。

---

## 2. SDKMAN：JDK 版本管理神器

### 2.1 安装 SDKMAN

```bash
# 进入 WSL2 Ubuntu
wsl

# 安装 SDKMAN（类 Unix 上最好的 JDK 版本管理器）
curl -s "https://get.sdkman.io" | bash

# 生效配置
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 验证
sdk version
```

### 2.2 多 JDK 安装与切换

```bash
# 查看可用的 Java 版本
sdk list java | head -30

# 安装常用版本
sdk install java 21.0.5-tem       # Temurin JDK 21 LTS
sdk install java 17.0.12-tem      # Temurin JDK 17 LTS
sdk install java 11.0.24-tem      # Temurin JDK 11 LTS

# 安装特定发行版
sdk install java 21.0.5-amzn      # Amazon Corretto
sdk install java 21.0.5-oracle    # Oracle JDK
sdk install java 22.0.1-graal     # GraalVM

# 设置默认 JDK
sdk default java 21.0.5-tem

# 当前 Shell 临时切换
sdk use java 17.0.12-tem

# 查看当前使用的版本
sdk current java
# → Using java version 21.0.5-tem

# 查看所有已安装
sdk list java | grep installed
```

### 2.3 SDKMAN 管理 Maven / Gradle

```bash
# 安装构建工具
sdk install maven 3.9.9
sdk install gradle 8.11

# 项目级版本锁定（.sdkmanrc）
cat > .sdkmanrc << 'EOF'
java=21.0.5-tem
maven=3.9.9
gradle=8.11
EOF

# 进入项目目录时自动检测 .sdkmanrc
# 在 ~/.zshrc 中添加：
echo 'sdkman_auto_env' >> ~/.zshrc
```

### 2.4 JDK 路径参考

```bash
# SDKMAN 安装的 JDK 路径
~/.sdkman/candidates/java/
├── 21.0.5-tem/
├── 17.0.12-tem/
├── 11.0.24-tem/
└── current -> 21.0.5-tem/    # 当前激活的版本

# JAVA_HOME 已自动设置为 ~/.sdkman/candidates/java/current
echo $JAVA_HOME
```

---

## 3. Maven / Gradle 配置

### 3.1 Maven 配置

```bash
# Maven 配置文件位置（在 WSL2 中）
# SDKMAN 安装的: ~/.sdkman/candidates/maven/current/conf/settings.xml
# 用户级: ~/.m2/settings.xml（建这个）

# 阿里云镜像加速（国内开发者推荐）
mkdir -p ~/.m2
cat > ~/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings>
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <name>Aliyun Maven</name>
            <url>https://maven.aliyun.com/repository/public</url>
            <mirrorOf>central</mirrorOf>
        </mirror>
    </mirrors>
</settings>
EOF

# 环境变量
echo 'export MAVEN_OPTS="-Xmx2g -Dfile.encoding=UTF-8"' >> ~/.zshrc

# 验证
mvn --version
```

### 3.2 Gradle 配置

```bash
# Gradle 全局属性
mkdir -p ~/.gradle
cat > ~/.gradle/gradle.properties << 'EOF'
# JVM 参数
org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC -XX:MaxMetaspaceSize=512m

# 构建优化
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.configuration-cache=true

# 代理（如果需要）
# systemProp.http.proxyHost=127.0.0.1
# systemProp.http.proxyPort=7890
EOF

# 验证
gradle --version
```

### 3.3 依赖下载加速

```bash
# WSL2 中下载依赖慢的原因和解决方案：

# 1. 使用国内镜像（Maven settings.xml / Gradle 仓库镜像）
# 2. 配置代理（如果 Windows 有代理软件）
# 在 ~/.zshrc 中加入：
export http_proxy=http://$(cat /etc/resolv.conf | grep nameserver | awk '{print $2}'):7890
export https_proxy=$http_proxy

# 3. 预下载常用依赖（新项目首次构建前）
# 创建基础 pom.xml 或 build.gradle，执行 mvn dependency:resolve
```

---

## 4. IDE 集成：IDEA + VS Code

### 4.1 VS Code + WSL2 Remote（推荐）

```bash
# 1. Windows 上安装 VS Code + Remote - WSL 扩展
#    扩展 ID: ms-vscode-remote.remote-wsl

# 2. 在 WSL2 终端中打开项目
cd ~/projects/my-java-app
code .
# → 自动启动 WSL2 Remote 模式
# → VS Code Server 运行在 WSL2 中
# → 所有终端、调试、编译都在 Linux 环境

# 3. 安装 Java 扩展包（在 WSL2 中的 VS Code Server）
#    Extension Pack for Java (ms)
#    Spring Boot Extension Pack (vmware)
```

VS Code WSL2 Remote 的优势：

```text
✅ 代码在 WSL2 的 ext4 上（编译极快）
✅ 终端就是 WSL2 bash/zsh（原生 Linux CLI）
✅ 扩展运行在 WSL2 中（Java 语言服务也是 Linux 版）
✅ Git 操作完全在 Linux 环境
✅ 前端界面的丝滑 Windows 渲染
→ 最好的 Windows + Linux 混合开发体验
```

### 4.2 IntelliJ IDEA 集成

```text
IDEA 对 WSL2 的支持有三种方式：

方式一：IDEA 直接打开 \\wsl$\ 路径（不推荐）
  File → Open → \\wsl$\Ubuntu-24.04\home\user\projects
  ❌ 文件 I/O 走 9P 协议，极慢
  ❌ IDE 索引巨慢
  ❌ Maven/Gradle 调用路径混乱

方式二：Gateway Remote Development（推荐）
  IDEA → Remote Development → New Connection → WSL
  → 选择发行版 → 选择项目目录
  ✅ IDEA Backend 运行在 WSL2 中
  ✅ 前端 Thin Client 在 Windows 渲染
  ⚠️ 需要 IDEA Ultimate

方式三：IDEA Windows 版 + Terminal 用 WSL2（折中）
  Settings → Tools → Terminal → Shell path:
  wsl.exe -d Ubuntu-24.04
  → 代码放 Windows 目录，Terminal 用 WSL2
  ⚠️ 编译速度慢（NTFS）
  ✅ 索引用 Windows 路径
```

> 💡 **VS Code + WSL2 Remote** 是目前最丝滑的免费方案。IDEA Ultimate 用户建议 Gateway Remote。

### 4.3 Git 配置

```bash
# 在 WSL2 中配置 Git（与 Windows Git 共享凭据管理器）
git config --global user.name "Your Name"
git config --global user.email "your@email.com"

# WSL2 Git 可以使用 Windows Git 凭据管理器
git config --global credential.helper "/mnt/c/Program\\ Files/Git/mingw64/bin/git-credential-manager.exe"

# 换行符处理（WSL2 是 Linux 环境）
git config --global core.autocrlf input   # 提交时 CRLF→LF
# 或用 .gitattributes 统一团队配置
```

---

## 5. Docker Desktop WSL2 Backend

### 5.1 架构

```text
Docker Desktop with WSL2 Backend：

┌───────────────────────────────────────────┐
│           Windows 宿主机                    │
│  ┌─────────────────┐                      │
│  │ Docker Desktop   │ ← GUI + CLI (docker.exe)│
│  │ (Windows)        │                      │
│  └────────┬────────┘                      │
│           │ 控制                            │
│  ┌────────▼──────────────────────────────┐ │
│  │           WSL2 Backend                 │ │
│  │  ┌──────────────────────────────────┐  │ │
│  │  │  docker-desktop (Docker Engine)  │  │ │
│  │  │  + docker-desktop-data (数据)    │  │ │
│  │  └──────────────────────────────────┘  │ │
│  └───────────────────────────────────────┘ │
└───────────────────────────────────────────┘

优势：
✅ 原生 Linux 容器（无需 Windows 容器）
✅ 容器文件系统在 ext4 上（高性能）
✅ 支持 docker compose、docker buildx
✅ 从 WSL2 和 Windows 都能使用 docker 命令
```

### 5.2 配置 WSL2 Backend

```bash
# 1. Windows 安装 Docker Desktop
# 2. Docker Desktop → Settings → General
#    ✅ Use WSL 2 based engine
# 3. Settings → Resources → WSL Integration
#    ✅ Enable integration with my default WSL distro
#    ✅ 勾选要集成的发行版（Ubuntu-24.04）
# 4. Apply & Restart

# 在 WSL2 中验证
docker --version
docker run hello-world

# docker-compose 也直接可用
docker compose version
```

### 5.3 Docker 数据盘管理

```bash
# WSL2 Docker 数据存放在两个发行版中
wsl -l -v
#   NAME                    STATE           VERSION
# * Ubuntu-24.04            Running         2
#   docker-desktop          Running         2      ← Engine
#   docker-desktop-data     Running         2      ← Images/Containers

# Docker 数据迁移到其他盘
# 1. 导出 docker-desktop-data
wsl --export docker-desktop-data D:\docker-data.tar

# 2. 注销原发行版
wsl --unregister docker-desktop-data

# 3. 导入到新位置
wsl --import docker-desktop-data D:\docker\data D:\docker-data.tar --version 2

# 4. 清理
del D:\docker-data.tar
```

---

## 6. 完整开发环境初始化脚本

```bash
#!/bin/bash
# wsl2-java-dev-setup.sh
# WSL2 Java 开发环境一键初始化

set -e

echo "🔄 Step 1: 更新系统..."
sudo apt update && sudo apt upgrade -y

echo "📦 Step 2: 安装基础工具..."
sudo apt install -y build-essential curl wget git vim zsh \
    software-properties-common apt-transport-https \
    ca-certificates gnupg lsb-release unzip

echo "🐚 Step 3: 配置 zsh + oh-my-zsh..."
sudo chsh -s $(which zsh) $USER
sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)" "" --unattended

echo "☕ Step 4: 安装 SDKMAN..."
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

echo "☕ Step 5: 安装 JDK..."
sdk install java 21.0.5-tem
sdk install java 17.0.12-tem
sdk default java 21.0.5-tem

echo "🔨 Step 6: 安装 Maven + Gradle..."
sdk install maven 3.9.9
sdk install gradle 8.11

echo "📁 Step 7: 配置 Maven 国内镜像..."
mkdir -p ~/.m2
cat > ~/.m2/settings.xml << 'XMLEOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings>
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Maven</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>
</settings>
XMLEOF

echo "✅ Step 8: 生成 SSH Key..."
if [ ! -f ~/.ssh/id_rsa ]; then
    ssh-keygen -t rsa -b 4096 -C "$USER@wsl2" -f ~/.ssh/id_rsa -N ""
fi

echo "📝 Step 9: 配置环境变量..."
cat >> ~/.zshrc << 'EOF'

# === Java 开发环境 ===
export MAVEN_OPTS="-Xmx2g -Dfile.encoding=UTF-8"
export GRADLE_OPTS="-Xmx2g -Dfile.encoding=UTF-8"

# === SDKMAN 自动切换 ===
sdkman_auto_env

# === 别名 ===
alias ll="ls -lh"
alias la="ls -lah"
alias g="git"
alias mci="mvn clean install -DskipTests"
alias mcp="mvn clean package -DskipTests"

# === fzf（模糊搜索）===
[ -f ~/.fzf.zsh ] && source ~/.fzf.zsh
EOF

echo ""
echo "✅ WSL2 Java 开发环境初始化完成！"
echo ""
echo "   JDK:    $(java --version 2>&1 | head -1)"
echo "   Maven:  $(mvn --version 2>&1 | head -1)"
echo "   Gradle: $(gradle --version 2>&1 | head -2 | tail -1)"
echo ""
echo "   SDKMAN:  sdk list java | grep installed"
echo "   工具安装: sdk install <tool>"
echo "   版本切换: sdk use java <version>"
echo ""
```

---

**上一模块**：[02-WSL2安装与配置](./02-WSL2安装与配置.md) ｜ **下一模块**：[04-WSL2与Windows互操作](./04-WSL2与Windows互操作.md) ｜ **返回总览**：[00-WSL2知识体系总览](./00-WSL2知识体系总览.md)

---

*创建于：2026年7月*
