# macOS 环境配置与 Java 开发

> ☕ `/usr/libexec/java_home` 为什么是 macOS 专属神器？SDKMAN 如何秒切 8 个 JDK 版本？Homebrew JDK 和手动安装有什么不同？—— Java 开发者在 macOS 上的环境配置全攻略

---

## 📚 目录

1. [macOS 环境变量体系](#1-macos-环境变量体系)
2. [JDK 安装方案对比](#2-jdk-安装方案对比)
3. [SDKMAN：Java 版本管理神器](#3-sdkmanjava-版本管理神器)
4. [JAVA_HOME 与 /usr/libexec/java_home](#4-java_home-与-usrlibexecjava_home)
5. [IDE 配置与 macOS 专属优化](#5-ide-配置与-macos-专属优化)

---

## 1. macOS 环境变量体系

### 1.1 环境变量的生命周期

```text
macOS 上环境变量生效的完整链路：

1. 系统启动
   └── launchd (PID 1) 初始化系统环境

2. 用户登录
   └── loginwindow → 读取 ~/.zprofile、~/.zshenv

3. 打开终端 (Terminal.app / iTerm2)
   ├── 登录 Shell → 加载 ~/.zprofile → ~/.zshrc → ~/.zlogin
   └── 非登录 Shell (新标签页) → 只加载 ~/.zshrc

4. 从 Dock/Finder 启动 GUI 应用
   └── 不走 Shell 配置！需要特殊方式设置
```

> ⚠️ **关键陷阱**：从 Dock 或 Spotlight 启动的 IDEA 不会读取 `~/.zshrc` 中的环境变量。需要通过 `launchctl setenv` 或在 `~/.zshenv` 中设置。

### 1.2 配置方式对比

| 方式 | 作用范围 | 持久性 | GUI 应用可见 | 推荐场景 |
|------|:---:|:---:|:---:|------|
| `export` in `.zshrc` | 终端会话 | 永久 | ❌ | 命令行工具 |
| `export` in `.zshenv` | 所有 zsh | 永久 | ❌ | 环境变量 |
| `launchctl setenv` | 整个用户会话 | 重启后失效 | ✅ | GUI 应用需用 |
| `~/Library/LaunchAgents/*.plist` | 用户服务 | 永久 | ✅ | 服务/守护进程 |
| `/etc/paths` / `/etc/paths.d/` | 全局 PATH | 永久 | ✅ | 系统级 PATH |
| `/etc/launchd.conf` | 全局环境 | 永久 | ✅ | 所有进程（macOS 10.9 已弃用） |

### 1.3 推荐配置位置

```bash
# ~/.zshenv —— 环境变量（所有 zsh 实例都加载）
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo "/opt/homebrew/opt/openjdk@21")
export PATH="$JAVA_HOME/bin:$PATH"

# ~/.zshrc —— 交互式 Shell 配置（别名、插件、主题、补全）
alias ll="ls -lh"
alias g="git"

# ~/.zprofile —— 登录 Shell 配置（PATH、登录时执行一次的命令）
eval "$(/opt/homebrew/bin/brew shellenv)"
```

### 1.4 让 GUI 应用（IDEA）也能读取环境变量

```bash
# 方式一：launchctl setenv（在 .zshrc 中自动执行）
# 每次打开终端时同步环境变量到 launchctl
launchctl setenv JAVA_HOME "$JAVA_HOME"
launchctl setenv PATH "$PATH"

# 方式二：创建 LaunchAgent（推荐）
# 创建 ~/Library/LaunchAgents/environment.plist
cat > ~/Library/LaunchAgents/environment.plist << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>setenv.environment</string>
  <key>ProgramArguments</key>
  <array>
    <string>/bin/launchctl</string>
    <string>setenv</string>
    <string>JAVA_HOME</string>
    <string>/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home</string>
  </array>
  <key>RunAtLoad</key>
  <true/>
</dict>
</plist>
EOF

launchctl load ~/Library/LaunchAgents/environment.plist
```

---

## 2. JDK 安装方案对比

### 2.1 方案一览

| 方案 | 安装命令 | JDK 路径 | 版本切换 | 推荐度 |
|------|---------|---------|:---:|:---:|
| **Homebrew** | `brew install openjdk@21` | `/opt/homebrew/opt/openjdk@21/` | 手动 | ⭐⭐⭐ |
| **SDKMAN** | `sdk install java 21.0.5-tem` | `~/.sdkman/candidates/java/` | ✅ 秒切 | ⭐⭐⭐⭐⭐ |
| **手动 .tar.gz** | 下载 → 解压 | 自定义 | 手动 | ⭐⭐ |
| **DMG 安装包** | 双击安装 | `/Library/Java/JavaVirtualMachines/` | 手动 | ⭐⭐⭐ |
| **jEnv** | `brew install jenv` | 管理 `/Library/Java/` 下的 JDK | ✅ | ⭐⭐⭐ |

### 2.2 Homebrew 安装

```bash
# 搜索可用 JDK
brew search openjdk

# 安装特定版本
brew install openjdk@17
brew install openjdk@21
brew install openjdk@24    # 最新

# Homebrew 安装的 JDK 位置
# Intel Mac:    /usr/local/opt/openjdk@21/
# Apple Silicon: /opt/homebrew/opt/openjdk@21/
# 实际文件:     /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/

# 创建符号链接（让系统识别）
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
    /Library/Java/JavaVirtualMachines/openjdk-21.jdk

# 验证安装
java --version
/usr/libexec/java_home -V
```

### 2.3 DMG 安装（传统方式）

```bash
# 下载 .dmg → 双击安装 → JDK 出现在：
/Library/Java/JavaVirtualMachines/
├── jdk-11.0.24.jdk/
├── jdk-17.0.12.jdk/
├── jdk-21.0.5.jdk/
└── jdk-1.8.0_422.jdk/

# 每个 .jdk 是一个 Bundle 目录，真实 Home 在：
# /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/
```

> 💡 **DMG 安装的系统级 JDK** 会被 `/usr/libexec/java_home` 自动发现，而 Homebrew 的需要手动符号链接。

---

## 3. SDKMAN：Java 版本管理神器

### 3.1 安装与基础命令

```bash
# 安装 SDKMAN
curl -s "https://get.sdkman.io" | bash

# 安装后重启终端，或立即生效
source "$HOME/.sdkman/bin/sdkman-init.sh"

# === 核心命令 ===

# 列出可安装的 Java 版本
sdk list java

# 安装特定版本
sdk install java 21.0.5-tem          # Temurin JDK 21
sdk install java 17.0.12-tem         # Temurin JDK 17
sdk install java 11.0.24-tem         # Temurin JDK 11
sdk install java 8.0.422-tem         # Temurin JDK 8
sdk install java 22.0.1-graal        # GraalVM 22

# 查看已安装
sdk list java | grep installed

# 切换默认版本（全局生效）
sdk default java 21.0.5-tem

# 当前 Shell 临时切换
sdk use java 17.0.12-tem

# 查看当前版本
sdk current java
```

### 3.2 JDK 发行版选择

```bash
# SDKMAN 支持的 Java 发行版标识
sdk list java

# 常用发行版：
# -tem       → Eclipse Temurin (Adoptium) ← 推荐
# -amzn      → Amazon Corretto
# -ms        → Microsoft OpenJDK
# -oracle    → Oracle JDK
# -graal     → GraalVM
# -zulu      → Azul Zulu
# -librca    → BellSoft Liberica
# -sapmchn   → SAP SapMachine

# 安装示例
sdk install java 21.0.5-tem       # Temurin
sdk install java 21.0.5-amzn      # Corretto
sdk install java 21.0.5-oracle    # Oracle
```

### 3.3 SDKMAN 管理 Maven / Gradle

```bash
# SDKMAN 不只是 Java 版本管理器！

# Maven
sdk install maven 3.9.9
sdk use maven 3.9.9

# Gradle
sdk install gradle 8.11
sdk use gradle 8.11

# 其他 SDK
sdk install ant 1.10.15
sdk install kotlin 2.1.0
sdk install scala 3.5.0
sdk install groovy 4.0.23

# 查看所有可管理 SDK 类型
sdk list
```

### 3.4 SDKMAN 高级技巧

```bash
# .sdkmanrc：项目级 SDK 版本锁定
# 在项目根目录创建 .sdkmanrc
cat > .sdkmanrc << 'EOF'
java=21.0.5-tem
maven=3.9.9
gradle=8.11
EOF

# 进入项目时自动切换到指定版本（需在 ~/.zshrc 中启用）
echo 'sdk_auto_env' >> ~/.zshrc

# 升级 SDKMAN 和所有已装 SDK
sdk selfupdate
sdk upgrade

# 清理旧版本（释放空间）
sdk flush archives    # 清理下载缓存
sdk flush temp        # 清理临时文件

# 离线模式
sdk offline enable
sdk offline disable

# 查看 SDKMAN 的 JAVA_HOME
echo $JAVA_HOME
# → /Users/xxx/.sdkman/candidates/java/current
```

> 🎯 **核心要点**：SDKMAN 的 `sdk use` 和项目级 `.sdkmanrc` 是 Java 版本管理的最佳实践，配合 IDEA 的 Project SDK 设置可做到命令行和 IDE 完全一致。

---

## 4. JAVA_HOME 与 /usr/libexec/java_home

### 4.1 /usr/libexec/java_home 详解

这是 macOS **独有**的系统工具，返回当前默认 JDK 的 Home 路径。

```bash
# 基本用法
/usr/libexec/java_home
# → /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home

# 查看所有已安装 JDK
/usr/libexec/java_home -V
# → Matching Java Virtual Machines (3):
#     21.0.5 (arm64) "Oracle" - "OpenJDK 21"
#     17.0.12 (arm64) "Oracle" - "OpenJDK 17"
#     1.8.0_422 (x86_64) "Oracle" - "Java SE 8"

# 请求特定版本
/usr/libexec/java_home -v 21      # JDK 21
/usr/libexec/java_home -v 17      # JDK 17
/usr/libexec/java_home -v 1.8     # JDK 8

# 请求特定架构
/usr/libexec/java_home -a arm64   # ARM 原生 JDK
/usr/libexec/java_home -a x86_64  # Intel JDK (通过 Rosetta 2)

# 常见用法：动态设置 JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

> ⚠️ **注意**：`/usr/libexec/java_home` 只能发现 `/Library/Java/JavaVirtualMachines/` 下的 JDK。Homebrew 安装的需要手动符号链接到该目录。

### 4.2 多版本自动切换脚本

```bash
# 在 ~/.zshrc 中添加
# 基于当前目录的 .java-version 文件自动切换 JDK
function java_version_auto() {
    if [[ -f .java-version ]]; then
        local version=$(cat .java-version)
        export JAVA_HOME=$(/usr/libexec/java_home -v "$version" 2>/dev/null || \
                           sdk home java "$version" 2>/dev/null)
        echo "☕ JDK switched to $version → $JAVA_HOME"
    fi
}

# 进入目录时自动触发
autoload -Uz add-zsh-hook
add-zsh-hook chpwd java_version_auto
java_version_auto

# 使用方式：
# 项目 A 目录下：echo "21" > .java-version
# 项目 B 目录下：echo "17" > .java-version
# cd 进去自动切换！
```

---

## 5. IDE 配置与 macOS 专属优化

### 5.1 IntelliJ IDEA macOS 优化

```properties
# ~/Library/Application Support/JetBrains/IntelliJIdea<版本>/idea.vmoptions
# 或 Help → Edit Custom VM Options

# === 内存配置（Apple Silicon 优化）===
-Xms2g
-Xmx4g
# M4 Pro/Max 可适度调高

# === GC 调优 ===
-XX:+UseZGC                       # ZGC 低延迟，适合大内存
# 或 -XX:+UseG1GC（老牌稳定）

# === macOS 特定优化 ===
-Dapple.awt.graphics.UseQuartz=true       # Quartz 2D 渲染
-Dapple.laf.useScreenMenuBar=true         # 使用全局菜单栏
-Dapple.awt.antialiasing=true             # 字体抗锯齿
-Dsun.java2d.opengl=true                  # OpenGL 加速

# === 代码缓存 ===
-XX:ReservedCodeCacheSize=1024m
```

### 5.2 IDEA macOS 快捷键（Keymap: macOS）

| 快捷键 | 功能 |
|--------|------|
| `⌘ + Shift + A` | 万能搜索（Actions） |
| `⇧⇧` (双击 Shift) | Search Everywhere |
| `⌘ + E` | 最近文件 |
| `⌘ + ⌥ + L` | 格式化代码 |
| `⌘ + ⌥ + O` | 清理无用 import |
| `⌘ + Shift + F` | 全局搜索 |
| `⌘ + Shift + R` | 全局替换 |
| `⌘ + Shift + Enter` | 自动补全分号/括号 |
| `⌃ + ⌥ + O` | Optimize Imports |
| `⌘ + B` | 跳转到定义 |
| `⌘ + ⌥ + B` | 跳转到实现 |
| `⌘ + F12` | 文件结构弹出窗 |
| `⌃ + Tab` | 切换工具窗口 |
| `⌘ + 1/2/3...` | 打开/关闭工具窗口 |

### 5.3 VS Code macOS 配置

```json
// settings.json
{
  "java.jdt.ls.java.home": "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-1.8",
      "path": "/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home"
    },
    {
      "name": "JavaSE-17",
      "path": "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"
    },
    {
      "name": "JavaSE-21",
      "path": "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home",
      "default": true
    }
  ],
  "java.jdt.ls.vmargs": "-XX:+UseZGC -Xmx4g",
  "java.compile.nullAnalysis.mode": "automatic"
}
```

### 5.4 Maven / Gradle 在 macOS 上的优化

```bash
# Maven: ~/.m2/settings.xml 与 Windows 相同
# 但建议使用 SDKMAN 管理 Maven 版本

# Gradle: ~/.gradle/gradle.properties
# macOS 特有优化
org.gradle.jvmargs=-Xmx4g -XX:+UseZGC -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true

# Gradle 守护进程（macOS 下文件系统快）
org.gradle.daemon.idletimeout=3600000

# 全局 Gradle 配置
org.gradle.java.home=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

---

**上一模块**：[02-macOS终端与Shell全指南](./02-macOS终端与Shell全指南.md) ｜ **下一模块**：[04-Homebrew包管理与软件生态](./04-Homebrew包管理与软件生态.md) ｜ **返回总览**：[00-macOS知识体系总览](./00-macOS知识体系总览.md)

---

*创建于：2026年7月*
