# Homebrew 包管理与软件生态

> 🍺 "The Missing Package Manager for macOS" —— 从 `brew install` 到 Brewfile 一键恢复，从 Formulae 到 Casks 到 MAS，macOS 上最不可或缺的开发者工具

---

## 📚 目录

1. [Homebrew 核心概念与原理](#1-homebrew-核心概念与原理)
2. [基础命令完全指南](#2-基础命令完全指南)
3. [Formulae vs Casks vs MAS](#3-formulae-vs-casks-vs-mas)
4. [Taps：扩展软件源](#4-taps扩展软件源)
5. [Brewfile：一键恢复开发环境](#5-brewfile一键恢复开发环境)
6. [Java 开发者完整安装脚本](#6-java-开发者完整安装脚本)

---

## 1. Homebrew 核心概念与原理

### 1.1 设计哲学

```text
Homebrew 的核心理念：

1. 用户级安装，不需要 sudo
   ├── Intel Mac:  /usr/local/
   └── Apple Silicon: /opt/homebrew/

2. 目录隔离
   ├── Cellar/          ← 各软件按版本存档
   ├── opt/             ← 当前激活版本的符号链接
   ├── bin/             ← 可执行文件的符号链接（加入 PATH）
   └── Caskroom/        ← GUI 应用 (.app)
```

### 1.2 安装架构对比

| 项目 | Intel Mac | Apple Silicon Mac |
|------|-----------|-------------------|
| **安装路径** | `/usr/local/` | `/opt/homebrew/` |
| **二进制路径** | `/usr/local/bin/brew` | `/opt/homebrew/bin/brew` |
| **Cellar** | `/usr/local/Cellar/` | `/opt/homebrew/Cellar/` |
| **Caskroom** | `/usr/local/Caskroom/` | `/opt/homebrew/Caskroom/` |

### 1.3 安装 Homebrew

```bash
# 官方安装（需要 xcode-select 或 Command Line Tools）
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安装后配置到 .zprofile（安装脚本会提示）
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"

# 验证
brew doctor       # 检查 Homebrew 健康状态
brew --version
```

---

## 2. 基础命令完全指南

### 2.1 日常操作

```bash
# === 搜索 ===
brew search jdk                   # 模糊搜索
brew search /jdk/                 # 正则搜索

# === 安装 ===
brew install maven                # 安装 Formulae (CLI 工具)
brew install --cask intellij-idea # 安装 Cask (GUI 应用)

# === 查看信息 ===
brew info maven                   # 详细信息
brew info --cask intellij-idea    # Cask 信息
brew deps maven                   # 查看依赖树
brew uses --installed openssl     # 哪些已装包依赖 openssl

# === 列出已装 ===
brew list                         # 所有 Formulae
brew list --cask                  # 所有 Casks
brew list --versions              # 含版本号

# === 更新 ===
brew update                       # 更新 Homebrew 自身 + 源信息
brew outdated                     # 查看可更新的包
brew upgrade                      # 升级所有
brew upgrade maven                # 升级特定包

# === 卸载 ===
brew uninstall maven              # 卸载 Formulae
brew uninstall --cask intellij-idea # 卸载 Cask
brew autoremove                   # 移除无用依赖

# === 清理 ===
brew cleanup                      # 清理旧版本
brew cleanup -n                   # 预览将要清理的（不实际执行）
brew cleanup --prune=all          # 清理所有旧版本

# === 服务管理 ===
brew services list                # 查看所有服务
brew services start mysql         # 启动并设为开机自启
brew services stop mysql          # 停止
brew services restart mysql       # 重启
brew services run mysql           # 只启动（不设开机自启）

# === 诊断 ===
brew doctor                       # 健康检查
brew config                       # 环境信息
```

### 2.2 高级命令

```bash
# 锁定/解锁版本（防止升级）
brew pin maven                    # 锁定
brew unpin maven                  # 解锁

# 查看依赖关系
brew deps --tree --installed      # 树形显示已装依赖关系

# 下载（不安装）
brew fetch maven                  # 只下载

# 重新安装
brew reinstall maven              # 覆盖安装

# 链接/取消链接（切换版本）
brew link openjdk@17
brew unlink openjdk@17

# 查看包的安装目录
brew --prefix maven               # → /opt/homebrew/opt/maven
brew --cellar maven               # → /opt/homebrew/Cellar/maven
```

---

## 3. Formulae vs Casks vs MAS

### 3.1 三者对比

| 维度 | Formula | Cask | MAS |
|------|---------|------|-----|
| **安装内容** | CLI 工具 / 库 | GUI 应用 (.app) | Mac App Store 应用 |
| **格式** | Ruby 脚本 | Ruby 脚本 + .app | 通过 App Store API |
| **安装位置** | `/opt/homebrew/Cellar/` | `/Applications/` | `/Applications/` |
| **更新方式** | `brew upgrade` | `brew upgrade --cask` | `mas upgrade` |
| **版本管理** | ✅ 多版本共存 | ❌ 只保留最新 | ❌ 只保留最新 |
| **示例** | `git`, `maven`, `node`, `python` | `google-chrome`, `intellij-idea`, `docker` | `Xcode`, `Pages`, `Keynote` |

### 3.2 Casks 使用示例

```bash
# 安装 GUI 应用（比去官网下载方便得多）
brew install --cask google-chrome
brew install --cask visual-studio-code
brew install --cask intellij-idea
brew install --cask docker
brew install --cask postman
brew install --cask dbeaver-community
brew install --cask iterm2
brew install --cask warp
brew install --cask rectangle          # 窗口管理
brew install --cask alt-tab             # Windows 式 Alt+Tab
brew install --cask betterdisplay       # 显示器管理
brew install --cask keka                # 压缩解压（替代 The Unarchiver）
brew install --cask iina                # 视频播放器（替代 VLC）
brew install --cask maccy               # 剪贴板历史
brew install --cask raycast             # Alfred 替代品（免费强）
brew install --cask obsidian            # 笔记软件
brew install --cask font-fira-code-nerd-font  # 编程字体
```

### 3.3 MAS (Mac App Store CLI)

```bash
# 安装 mas
brew install mas

# 搜索 App Store 应用
mas search Xcode
mas search "Microsoft Remote Desktop"

# 安装
mas install 497799835   # Xcode (ID=497799835)

# 查看已装
mas list

# 升级
mas upgrade
mas upgrade 497799835   # 升级特定应用

# 常用 App Store 应用 ID
# 497799835  → Xcode
# 1444383602 → Microsoft Remote Desktop
# 409203825  → Numbers
# 409201541  → Pages
# 409183694  → Keynote
# 1176895641 → Spark (邮件客户端)

# 获取应用 ID 的方法：
# 1. 在 App Store 中打开应用页面
# 2. 右键 → 复制链接 → 链接中包含数字 ID
```

---

## 4. Taps：扩展软件源

### 4.1 Tap 概念

```bash
# Homebrew 的软件源叫 Tap（相当于 apt 的 PPA）
# 默认 Tap: homebrew/core (Formulae) + homebrew/cask (Casks)
# 第三方 Tap 扩展了可用软件范围

# 查看已添加 Tap
brew tap

# 添加第三方 Tap
brew tap homebrew/cask-fonts         # 字体
brew tap homebrew/cask-versions      # 多版本 GUI 应用
brew tap microsoft/git               # 微软 Git 版本
brew tap mongodb/brew                # MongoDB 官方
brew tap hashicorp/tap               # HashiCorp (Terraform, Vault...)
brew tap adoptopenjdk/openjdk        # AdoptOpenJDK (已过时，用 temurin)

# 解除 Tap
brew untap hashicorp/tap
```

### 4.2 Java 相关 Tap

```bash
# Temurin JDK（旧版名称，新版已合入 homebrew/core）
brew install temurin

# GraalVM
brew tap graalvm/tap
brew install --cask graalvm-jdk

# 查看可用的 openjdk 版本
brew search openjdk
# openjdk@11  openjdk@17  openjdk@21  openjdk@24
```

---

## 5. Brewfile：一键恢复开发环境

### 5.1 导出 Brewfile

```bash
# 导出当前环境所有 Homebrew 安装的软件
brew bundle dump --file=~/Brewfile --force

# Brewfile 示例内容：
# tap "homebrew/cask"
# tap "homebrew/cask-fonts"
# brew "git"
# brew "maven"
# brew "gradle"
# brew "openjdk@21"
# brew "node"
# brew "python"
# cask "intellij-idea"
# cask "visual-studio-code"
# cask "docker"
# cask "iterm2"
# cask "font-fira-code-nerd-font"
# mas "Xcode", id: 497799835
```

### 5.2 在新 Mac 上一键恢复

```bash
# 1. 先安装 Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. 将 Brewfile 复制到新 Mac

# 3. 一键恢复所有软件
brew bundle install --file=~/Brewfile

# 4. 检查是否有遗漏
brew bundle check --file=~/Brewfile

# 5. 清理 Brewfile 中已卸载的条目
brew bundle cleanup --file=~/Brewfile --force
```

> 🎯 **核心要点**：Brewfile 是 macOS 开发环境迁移的终极方案，配合 iCloud/Dropbox/Git 同步，换新 Mac 后一条命令恢复全部工作环境。

### 5.3 维护 Brewfile 的最佳实践

```bash
# 将 Brewfile 放入 dotfiles 仓库管理
cd ~/.dotfiles
brew bundle dump --file=Brewfile --force
git add Brewfile
git commit -m "Update Brewfile $(date +%Y-%m-%d)"

# 定期清理
brew bundle cleanup --force
# 删除 Brewfile 中不存在但已安装的软件
```

---

## 6. Java 开发者完整安装脚本

### 6.1 新 Mac 一键初始化脚本

```bash
#!/bin/bash
# new-mac-developer-setup.sh
# macOS 开发环境一次性初始化

set -e

echo "🍺 Step 1: 安装 Homebrew..."
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
eval "$(/opt/homebrew/bin/brew shellenv)"

echo "☕ Step 2: 安装 JDK..."
brew install openjdk@17 openjdk@21
# 符号链接到系统 JDK 目录
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk

echo "📦 Step 3: 安装构建工具..."
brew install maven gradle

echo "🛠️ Step 4: 安装通用开发工具..."
brew install git node python
brew install --cask docker visual-studio-code postman dbeaver-community

echo "🖥️ Step 5: 安装终端工具..."
brew install --cask iterm2
brew install fzf
$(brew --prefix)/opt/fzf/install --all

echo "✍️ Step 6: 安装 IDE..."
brew install --cask intellij-idea

echo "🔤 Step 7: 安装编程字体..."
brew install --cask font-fira-code-nerd-font font-jetbrains-mono-nerd-font

echo "⚡ Step 8: 安装效率工具..."
brew install --cask raycast rectangle alt-tab

echo "🧹 Step 9: 清理..."
brew cleanup

echo "✅ 开发环境初始化完成！"
java --version
mvn --version
```

### 6.2 精简版（最少配置）

```bash
# 极简版：只要 Java + Maven + IDEA
brew install openjdk@21 maven
brew install --cask intellij-idea

# SDKMAN 管理多 JDK (推荐)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.5-tem
sdk install maven 3.9.9
```

---

**上一模块**：[03-macOS环境配置与Java开发](./03-macOS环境配置与Java开发.md) ｜ **下一模块**：[05-macOS文件系统进程与安全](./05-macOS文件系统进程与安全.md) ｜ **返回总览**：[00-macOS知识体系总览](./00-macOS知识体系总览.md)

---

*创建于：2026年7月*
