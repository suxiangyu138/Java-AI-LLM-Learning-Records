# macOS 开发者实战手册

> 🛠️ Apple Silicon 兼容性踩坑？Homebrew 装完找不到命令？端口占用、权限拒绝、IDEA 卡顿 —— Java 开发者在 macOS 上的十大高频问题与最优解法

---

## 📚 目录

1. [Apple Silicon 兼容性全攻略](#1-apple-silicon-兼容性全攻略)
2. [端口与环境排错](#2-端口与环境排错)
3. [Homebrew 常见问题](#3-homebrew-常见问题)
4. [Java 开发专属问题](#4-java-开发专属问题)
5. [系统性能与空间优化](#5-系统性能与空间优化)
6. [开发者必备快捷键](#6-开发者必备快捷键)
7. [效率工具推荐](#7-效率工具推荐)

---

## 1. Apple Silicon 兼容性全攻略

### 1.1 快速诊断

```bash
# 1. 确认架构
uname -m
# arm64 → Apple Silicon (M1/M2/M3/M4)
# x86_64 → Intel Mac

# 2. 确认进程架构
# 活动监视器 → 查看 → 列 → 种类
# Apple (原生 ARM) vs Intel (Rosetta 2 转译)

# 3. 在终端区分
file /opt/homebrew/bin/java           # Mach-O 64-bit executable arm64
file /usr/local/bin/java              # Mach-O 64-bit executable x86_64

# 4. 检查 Java 架构
java -XshowSettings:properties -version 2>&1 | grep "os.arch"
# os.arch = aarch64  → ARM 原生
# os.arch = x86_64   → Rosetta 2 转译
```

### 1.2 ARM / x86 双 Homebrew 共存

```bash
# ARM Homebrew (默认、推荐)
/opt/homebrew/bin/brew                 # 安装 ARM 版本软件

# x86 Homebrew（需要时启用）
# 用 x86_64 模式运行：
arch -x86_64 /bin/zsh
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
# x86 Homebrew 安装在 /usr/local/

# 管理两个 Homebrew
# 在 ~/.zshrc 中创建切换别名
alias brew-arm='eval "$(/opt/homebrew/bin/brew shellenv)"'
alias brew-x86='eval "$(/usr/local/bin/brew shellenv)"'
```

### 1.3 Docker on Apple Silicon 避坑

```bash
# Docker 默认运行 ARM 容器，运行 x86 镜像需要 Rosetta
# Docker Desktop 设置：Settings → General → ✅ Use Rosetta for x86/amd64 emulation

# 拉取镜像时指定架构
docker pull --platform linux/arm64 mysql:8.0    # ARM 原生
docker pull --platform linux/amd64 mysql:8.0    # x86 (启用 Rosetta 后也可运行)

# Java 基础镜像选择
# ARM 原生（推荐）：
FROM eclipse-temurin:21-jre-jammy
# 多架构构建：
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS builder
```

### 1.4 JNI / 本地库兼容性

```bash
# 检查动态库架构
file libnative.dylib
# libnative.dylib: Mach-O 64-bit dynamically linked shared library arm64

# 如果只有 x86_64 的 .dylib，只能通过 Rosetta 2 运行整个 JVM
arch -x86_64 java -Djava.library.path=./ -jar app.jar

# 查看 JVM 加载的本地库
java -XshowSettings:properties -version 2>&1 | grep java.library.path

# Apple Silicon 上原生 JDK 的 .dylib 搜索路径
# /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/lib/
```

---

## 2. 端口与环境排错

### 2.1 端口占用问题

```bash
# === Step 1：找谁占用了端口 ===
lsof -i :8080
# COMMAND   PID   USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
# java    12345   user  128u  IPv6 ...      0t0  TCP *:8080 (LISTEN)

# === Step 2：杀进程 ===
kill -9 12345
# 或
kill $(lsof -t -i:8080)

# === 一键函数（加到 ~/.zshrc）===
function killport() {
    local pid=$(lsof -t -i:$1)
    if [[ -n "$pid" ]]; then
        echo "Killing process on port $1 (PID: $pid)"
        kill -9 $pid
    else
        echo "No process found on port $1"
    fi
}
# 使用：killport 8080
```

### 2.2 环境变量排错

```bash
# === 诊断序列 ===
# 1. 当前 JAVA_HOME
echo $JAVA_HOME

# 2. which java
which java
ls -la $(which java)     # 确认是真实文件还是符号链接

# 3. 查看所有 Java 安装
/usr/libexec/java_home -V

# 4. 查看所有 PATH 条目
echo $PATH | tr ':' '\n'

# 5. 查找所有的 java
mdfind -name java | grep -v "\.jar$" | head -20

# 6. 检查 zsh 配置
cat ~/.zshrc | grep -i java
cat ~/.zshenv | grep -i java
cat ~/.zprofile | grep -i java
```

### 2.3 编码问题

```bash
# macOS 默认 UTF-8（比 Windows 好得多），但仍有边界情况
# 查看当前 locale
locale
# LANG="en_US.UTF-8"
# LC_ALL=

# 设置中文 UTF-8
export LANG=zh_CN.UTF-8

# Maven 构建输出乱码
export MAVEN_OPTS="-Dfile.encoding=UTF-8"

# IDEA 中出现乱码
# Help → Edit Custom VM Options 添加：
# -Dfile.encoding=UTF-8
```

### 2.4 权限拒绝

```bash
# Homebrew 权限问题
sudo chown -R $(whoami) /opt/homebrew
sudo chown -R $(whoami) /opt/homebrew/share/zsh

# 无法打开项目文件
ls -la ~/projects | head -5
sudo chown -R $(whoami) ~/projects

# Java 进程无权限监听低端口（1024 以下）
# 用 8080/8443 代替 80/443

# 应用被 Gatekeeper 阻止
xattr -dr com.apple.quarantine /path/to/app.app
# 或：系统设置 → 隐私与安全性 → 仍要打开

# .ssh 权限过于开放
chmod 700 ~/.ssh
chmod 600 ~/.ssh/id_rsa
chmod 644 ~/.ssh/id_rsa.pub
chmod 644 ~/.ssh/known_hosts
chmod 644 ~/.ssh/config
```

---

## 3. Homebrew 常见问题

### 3.1 command not found after install

```bash
# brew install 后提示 command not found？
# 原因：未将 Homebrew bin 加入 PATH

# 修复
eval "$(/opt/homebrew/bin/brew shellenv)"
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile

# 对于 formula，brew link 可能失败
brew link --overwrite maven      # 强制链接
brew unlink maven && brew link maven
```

### 3.2 更新/升级卡住

```bash
# 更新卡住
brew update-reset                # 重置 Homebrew 源
rm -rf "$(brew --cache)"         # 清理下载缓存

# 升级失败
brew upgrade --force-bottle maven

# 包冲突
brew doctor                      # 诊断
brew cleanup                     # 清理后重试

# 完全重装 Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/uninstall.sh)"
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

---

## 4. Java 开发专属问题

### 4.1 常见错误速查

| 错误 | macOS 特因 | 解决方案 |
|------|-----------|---------|
| `java: command not found` | 未安装或 PATH 未配置 | `brew install openjdk@21` + 符号链接 |
| `Unsupported class file major version 65` | JDK 版本太低 | 切换 JDK：`sdk use java 21.0.5-tem` |
| `Address already in use` | 端口被占用 | `killport 8080` |
| `Could not find or load main class` | 路径含中文/特殊字符 | 避免中文路径 |
| `PermGen / Metaspace error` | JDK 8 → 11+ 参数变化 | `-XX:MaxMetaspaceSize=512m` |
| IDEA `java.lang.OutOfMemoryError` | IDEA 内存不足 | 调大 `idea.vmoptions` 中 `-Xmx` |
| `localhost refused to connect` | 服务未起 / 防火墙 | `sudo pfctl -d` 临时关 PF |
| `JNI error: wrong architecture` | ARM Mac 上用了 x86 JDK | 用 `arch` 检查，下载 ARM 版 JDK |

### 4.2 Maven / Gradle 专属

```bash
# Maven 下载太慢
# ~/.m2/settings.xml 配置阿里云镜像（国内用户）
# 或使用代理
export MAVEN_OPTS="-DsocksProxyHost=127.0.0.1 -DsocksProxyPort=7890"

# Gradle 文件锁冲突
rm -rf ~/.gradle/caches/*/fileHashes
rm -rf ~/.gradle/daemon/
# 或暴力杀干净 Gradle 守护
pkill -f '.*GradleDaemon.*'

# Maven 本地仓库太大
du -sh ~/.m2/repository/
# 清理未用依赖
find ~/.m2/repository -name "*.lastUpdated" -delete

# Gradle 缓存清理
rm -rf ~/.gradle/caches/*/fileHashes/
```

### 4.3 macOS Sequoia 上 Java 注意事项

```text
macOS 15 (Sequoia) 特有：
1. 屏幕录制权限更严格 → IDEA/VS Code 截图插件可能弹授权
2. 本地网络权限 → Spring Boot 应用首次启动弹窗问是否允许网络
3. 辅助功能权限 → 自动化工具（Karabiner, BetterTouchTool）需重新授权
4. 每个 Java 进程首次访问某些目录也可能触发 TCC 弹窗
```

---

## 5. 系统性能与空间优化

### 5.1 内存使用分析

```bash
# 查看内存使用（类似 top）
top -l 1 | head -10 | grep PhysMem
# → PhysMem: 16G used, 16G unused → 内存压力大

# 内存压力查看
memory_pressure
# → System memory pressure is at 25% (normal)

# 按内存排序进程
ps aux --sort=-%mem | head -10

# 杀掉僵尸 Java 进程
ps aux | grep java | grep -v grep | awk '{print $2}' | xargs kill -9
```

### 5.2 空间清理

```bash
# === 查看空间分布 ===
# 推荐用 DaisyDisk 或 GrandPerspective (GUI)
# 命令行：
du -sh ~/Library/* | sort -rh | head -10

# === 可安全删除的目录 ===
# Xcode 相关（如果不做 iOS 开发）
rm -rf ~/Library/Developer/Xcode/DerivedData
rm -rf ~/Library/Developer/Xcode/Archives
rm -rf ~/Library/Developer/Xcode/iOS\ DeviceSupport

# Gradle 缓存
rm -rf ~/.gradle/caches/

# Maven 本地仓库（可选择性删除，会自动重新下载）
du -sh ~/.m2/repository/

# Homebrew 旧版本
brew cleanup --prune=all

# Docker 占用
docker system prune -a --volumes

# Time Machine 本地快照（会释放大量空间！）
sudo tmutil listlocalsnapshots /
sudo tmutil deletelocalsnapshots /

# 系统缓存
sudo rm -rf /Library/Caches/*
rm -rf ~/Library/Caches/*
```

### 5.3 Java 应用性能调优

```bash
# macOS 上 JVM 参数建议
java -Xms2g -Xmx4g \
     -XX:+UseZGC \
     -XX:MaxMetaspaceSize=512m \
     -XX:+AlwaysPreTouch \
     -Dfile.encoding=UTF-8 \
     -jar app.jar

# ZGC 在 macOS 上表现优异（低延迟场景）
# G1GC 更成熟（通用场景）
# ParallelGC 适合高吞吐批处理

# Apple Silicon 上实测 ZGC 比 G1GC 暂停时间减少 70%+
```

---

## 6. 开发者必备快捷键

### 6.1 macOS 系统级

| 快捷键 | 功能 |
|--------|------|
| `⌘ + Space` | Spotlight 搜索（启动应用/计算/搜索） |
| `⌘ + Tab` | 应用切换（配合 `⌘ + Q` 关闭） |
| `⌘ + `` ` ` | 同一应用多窗口切换 |
| `⌘ + W` | 关闭当前窗口/标签页 |
| `⌘ + Option + Esc` | 强制退出应用 |
| `⌘ + Shift + .` | Finder 中显示/隐藏隐藏文件 |
| `⌘ + Shift + G` | Finder 中前往指定路径 |
| `⌘ + Option + Space` | Finder 搜索（非 Spotlight） |
| `⌘ + Control + Q` | 锁屏 |
| `⌘ + Shift + 3` | 全屏截图 |
| `⌘ + Shift + 4` | 区域截图 |
| `⌘ + Shift + 5` | 截图工具面板（含录屏） |
| `⌘ + Option + V` | Finder 中剪切粘贴（移动文件） |
| `Enter` | 文件重命名（不是打开） |
| `⌘ + O` 或 `⌘ + ↓` | 打开文件 |
| `⌘ + ↑` | 回上级目录 |

### 6.2 终端 / iTerm2

| 快捷键 | 功能 |
|--------|------|
| `⌘ + T` | 新建标签页 |
| `⌘ + D` | 垂直分屏 |
| `⌘ + Shift + D` | 水平分屏 |
| `⌘ + W` | 关闭当前窗格/标签 |
| `⌘ + ←/→` | 切换标签 |
| `⌘ + Option + ←/→` | 切换分屏窗格 |
| `⌘ + /` | 高亮鼠标位置 |
| `⌘ + F` | 搜索 |
| `⌘ + ;` | 自动补全提示 |
| `⌘ + Shift + H` | 粘贴历史 |
| `⌘ + Enter` | 全屏切换 |

### 6.3 JetBrains IDEA (macOS Keymap)

| 快捷键 | 功能 |
|--------|------|
| `⌘ + Shift + A` | 万能搜索 (Actions) |
| `⇧⇧` | Search Everywhere |
| `⌘ + Click` | 跳转到定义 |
| `⌘ + E` | 最近文件 |
| `⌘ + Shift + E` | 最近编辑位置 |
| `⌘ + Option + L` | 格式化代码 |
| `⌘ + Shift + F` | 全局搜索（Find in Files） |
| `⌘ + Shift + R` | 全局替换 |
| `⌘ + B` | 跳转到定义 |
| `⌘ + Option + B` | 跳转到实现 |
| `⌘ + F12` | 文件结构弹出窗 |
| `⌘ + Shift + Enter` | 智能补全（自动加分号/括号） |
| `⌃ + Tab` | 切换工具窗口 |
| `⌘ + 1/2/3...` | 打开/关闭工具窗口 |
| `⌘ + Option + T` | 环绕代码块（try/if/for...） |
| `Option + ↑/↓` | 扩大/缩小选区 |
| `⌘ + Shift + U` | 大小写切换 |
| `⌘ + Shift + 8` | 列编辑模式 |

### 6.4 窗口管理

| 快捷键 | 功能 |
|--------|------|
| `⌃ + ←/→` | 左右分屏 (Split View) |
| `⌃ + ↑` | Mission Control |
| `⌃ + ↓` | 当前应用窗口展示 |
| `F11` | 显示桌面 |
| **Rectangle** / **Magnet** 增强 | 1/2 分屏、1/3 分屏、四分之一屏 |

---

## 7. 效率工具推荐

### 7.1 必装工具 Top 10

| 工具 | 用途 | 安装 | 免费 |
|------|------|------|:---:|
| **Raycast** | Spotlight 增强（启动器+剪贴板+窗口+...） | `brew install --cask raycast` | ✅ |
| **Rectangle** | 窗口分屏管理 | `brew install --cask rectangle` | ✅ |
| **iTerm2** | 最强终端模拟器 | `brew install --cask iterm2` | ✅ |
| **Homebrew** | 包管理器 | 官方脚本安装 | ✅ |
| **AltTab** | Windows 式应用窗口切换 | `brew install --cask alt-tab` | ✅ |
| **Maccy / Maccy** | 剪贴板历史管理器 | `brew install --cask maccy` | ✅ |
| **IINA** | 视频播放器 | `brew install --cask iina` | ✅ |
| **Keka** | 压缩解压 | `brew install --cask keka` | ✅ |
| **Visual Studio Code** | 轻量级编辑器 | `brew install --cask visual-studio-code` | ✅ |
| **Docker Desktop** | 容器运行时 | `brew install --cask docker` | ✅ |

### 7.2 开发者增强工具

| 工具 | 用途 | 安装 |
|------|------|------|
| **DevUtils** | 开发者工具箱（JSON/Base64/正则/时间戳...） | 独立下载 |
| **Postman / Bruno** | API 测试 | `brew install --cask postman` |
| **DBeaver** | 数据库管理（支持所有主流 DB） | `brew install --cask dbeaver-community` |
| **Sublime Merge** | Git GUI 客户端 | `brew install --cask sublime-merge` |
| **Charles / Proxyman** | HTTP 抓包调试 | `brew install --cask proxyman` |
| **Wireshark** | 网络抓包分析 | `brew install --cask wireshark` |
| **Obsidian** | Markdown 笔记 | `brew install --cask obsidian` |
| **Stats** | 菜单栏系统监控（CPU/内存/网络/温度） | `brew install --cask stats` |

### 7.3 命令行增强工具

```bash
# 神器组合
brew install bat            # 语法高亮 cat（替代 cat）
brew install fd             # 更快 find（替代 find）
brew install ripgrep        # 更快 grep（替代 grep）
brew install fzf            # 模糊搜索（Ctrl+R 增强）
brew install jq             # JSON 处理
brew install yq             # YAML 处理
brew install httpie         # 更好的 curl（http 命令）
brew install tldr           # 实用命令示例（替代 man 的啰嗦）
brew install thefuck        # 自动纠正错误命令
brew install ncdu           # 磁盘使用分析（比 du 直观）
brew install htop           # 更好的 top
brew install tree           # 目录树
brew install watch          # 重复执行命令
brew install wget           # curl 替代品
brew install diff-so-fancy  # Git diff 美化

# 使用示例
bat pom.xml                          # 语法高亮 XML
fd "Controller\.java$" ~/projects    # 搜 Java Controller
rg "TODO" **/*.java                  # 搜 TODO
cat data.json | jq '.dependencies'   # JSON 解析
http :8080/api/health                # 测试 API
tldr tar                             # 看实用示例
```

---

**上一模块**：[05-macOS文件系统进程与安全](./05-macOS文件系统进程与安全.md) ｜ **返回总览**：[00-macOS知识体系总览](./00-macOS知识体系总览.md)

---

*创建于：2026年7月*
