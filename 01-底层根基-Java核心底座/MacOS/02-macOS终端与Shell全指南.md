# macOS 终端与 Shell 全指南

> ⌨️ 从 Terminal.app 到 iTerm2 再到 Warp，从 bash 到 zsh 再到 oh-my-zsh —— Java 开发者在 macOS 上必须搞懂的终端与 Shell 全栈

---

## 📚 目录

1. [终端模拟器：Terminal.app vs iTerm2 vs Warp](#1-终端模拟器terminalapp-vs-iterm2-vs-warp)
2. [zsh：macOS 默认 Shell 深度使用](#2-zshmacos-默认-shell-深度使用)
3. [管道、重定向与进程控制](#3-管道重定向与进程控制)
4. [macOS 专属命令](#4-macos-专属命令)
5. [oh-my-zsh 与插件生态](#5-oh-my-zsh-与插件生态)

---

## 1. 终端模拟器：Terminal.app vs iTerm2 vs Warp

### 1.1 三者对比

| 特性 | Terminal.app | iTerm2 | Warp |
|------|:---:|:---:|:---:|
| **推出** | 2001 (随 Mac OS X) | 2010 | 2022 |
| **价格** | 系统内置 | 免费开源 | 免费 (团队版付费) |
| **标签页** | ✅ | ✅ (更灵活) | ✅ |
| **分屏** | ❌ | ✅ (无限分割) | ✅ |
| **GPU 渲染** | ❌ | ✅ Metal 渲染 | ✅ 自研引擎 |
| **搜索** | 基础 | 强大（正则、大小写） | 命令块智能搜索 |
| **AI 辅助** | ❌ | ❌ | ✅ Warp AI (集成 LLM) |
| **Python API** | ❌ | ✅ 脚本化控制 | ❌ |
| **Profile** | 基础 | ✅ 丰富（可自动化） | 基础 |
| **即时重播** | ❌ | ✅ 回放终端历史 | ❌ |
| **热键窗口** | ❌ | ✅ 全局快捷键呼出 | ❌ |
| **推荐人群** | 临时使用 | **开发者首选** | AI 时代探索者 |

### 1.2 iTerm2 推荐配置

```bash
# iTerm2 个性化设置
Preferences:
├── General → Closing → ✅ Quit when all windows are closed
├── Appearance → Theme → Minimal
├── Profiles → Window → Transparency → 轻微透明（10-15%）
├── Profiles → Keys → Key Mappings
│   ├── ⌘← / ⌘→  → Send Escape [H / [F  (行首/行尾)
│   ├── ⌥← / ⌥→  → Send Escape b / f    (单词跳转)
│   └── ⌫ / ⌥⌫   → Send Hex 0x7f / Escape [127;5~ (删单词)
├── Profiles → Terminal → Unlimited scrollback
└── Keys → Hotkey → ✅ Show/hide with hotkey (⌃Space)
```

### 1.3 macOS 终端字体推荐

```bash
# 编程字体（Nerd Font 版本含图标）
brew install --cask font-fira-code-nerd-font
brew install --cask font-cascadia-code-nf
brew install --cask font-jetbrains-mono-nerd-font
brew install --cask font-meslo-lg-nerd-font

# 在终端/iTerm2 设置中选择 Nerd Font
```

---

## 2. zsh：macOS 默认 Shell 深度使用

### 2.1 zsh vs bash 核心差异

| 特性 | bash | zsh |
|------|:---:|:---:|
| **macOS 默认** | 旧版 3.2 (不再更新) | 5.x (2019 起为默认) |
| **GPL 版本** | GPLv3 (新版) | MIT-like |
| **自动补全** | 基础 | ✅ 菜单式、大小写不敏感、模糊匹配 |
| **路径展开** | `cd /u/l/b` ❌ | `cd /u/l/b → /usr/local/bin` ✅ |
| **Glob 通配** | 基础 | ✅ 递归 `**/*.java`、限定符 `*(.)` |
| **拼写纠正** | ❌ | ✅ `sl → ls` |
| **右提示符** | ❌ | ✅ `RPROMPT` |
| **主题系统** | ❌ | ✅ 内置 prompt 主题 |
| **浮点运算** | ❌ 需 bc | ✅ 内建支持 |
| **关联数组** | 4.0+ | ✅ 原生 |
| **语法高亮** | ❌ | ✅ 需要 fish-like 插件 |

> 🎯 **核心要点**：zsh 对 bash 脚本 99% 兼容，日常交互体验远胜 bash。写脚本建议用 `#!/bin/bash` 保持跨平台兼容，日常交互用 zsh。

### 2.2 zsh 常用快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl + A / E` | 光标跳到行首 / 行尾 |
| `Ctrl + U / K` | 删除光标前 / 后全部 |
| `Ctrl + W` | 删除前一个单词 |
| `Ctrl + R` | 历史命令搜索（fzf 增强） |
| `Ctrl + L` | 清屏 |
| `Ctrl + D` | 退出当前 Shell (EOF) |
| `Esc + .` | 插入上一条命令的最后一个参数 |
| `!!` | 重复上一条命令 |
| `!$` | 上一条命令最后一个参数 |
| `!java` | 执行最近一条以 java 开头的命令 |

### 2.3 配置文件加载顺序

```text
登录 Shell 启动顺序:
  1. /etc/zshenv          → 所有 zsh 都加载（极少使用）
  2. ~/.zshenv             → 环境变量放这里
  3. /etc/zprofile         → 登录时（系统级）
  4. ~/.zprofile           → 登录时（用户级）← 环境变量更常放这
  5. /etc/zshrc            → 交互 Shell（系统级）
  6. ~/.zshrc              → 交互 Shell（用户级）← 别名/插件放这
  7. /etc/zlogin           → 登录时（最后）
  8. ~/.zlogin             → 登录时（最后）

实用原则：
  ~/.zshenv → 环境变量
  ~/.zshrc  → 别名、函数、插件、prompt
  ~/.zprofile → PATH（登录时设置一次就够了）
```

---

## 3. 管道、重定向与进程控制

### 3.1 标准 Unix 管道与重定向

```bash
# === 管道：串联命令 ===
mvn dependency:tree | grep "spring-boot" | wc -l

# === 重定向 ===
# 标准输出重定向
mvn clean compile > build.log        # 覆盖
mvn clean compile >> build.log       # 追加

# 标准错误重定向
mvn clean compile 2> errors.log

# 合并标准输出和错误
mvn clean compile > all.log 2>&1     # 都到文件
mvn clean compile 2>&1 | grep ERROR  # 都进管道

# === /dev/null：丢弃输出 ===
mvn clean compile > /dev/null 2>&1   # 完全静默

# === tee：同时输出到文件和屏幕 ===
java -jar app.jar | tee app.log      # 看日志同时保存

# === 进程控制 ===
java -jar app.jar &                  # 后台运行
Ctrl + Z                             # 暂停当前进程
bg                                   # 将暂停进程放后台
fg                                   # 将后台进程拉前台
jobs                                 # 查看后台作业列表
```

### 3.2 zsh 独有：Glob 通配符高级用法

```bash
# 递归通配符
ls **/*.java                          # 递归搜索所有 .java 文件
grep -r "TODO" **/*.java              # 等价但更灵活

# 限定符（Qualifiers）
ls -l **/*.java(.Lm+10)               # .java 中 >10MB 的文件
ls **/*(/)                            # 只列目录
ls **/*(@)                            # 只列符号链接
ls -l **/pom.xml(.)                   # 只列普通文件
rm **/*.class(.)                      # 删除所有 .class 文件

# 批量重命名（zmv）
autoload -Uz zmv
zmv '(*).java' '$1_backup.java'       # 批量 .java → _backup.java
zmv '(*)_old.java' '$1.java'          # 批量去掉 _old 后缀

# Brace Expansion (花括号展开)
mkdir -p project/{src/main/java,src/main/resources,src/test/java}
# → project/src/main/java, project/src/main/resources, project/src/test/java
```

> 💡 **zmv 是杀手级功能**：`autoload -Uz zmv` 加载后即可批量重命名，比写循环脚本快 10 倍。

---

## 4. macOS 专属命令

### 4.1 文件与系统操作

| 命令 | 功能 | 示例 |
|------|------|------|
| `open` | 用默认程序打开文件/URL/目录 | `open .` (Finder打开当前目录) |
| `open -a` | 指定应用打开 | `open -a IntelliJ\ IDEA pom.xml` |
| `open -e` | 用 TextEdit 打开 | `open -e ~/.zshrc` |
| `open -R` | 在 Finder 中显示 | `open -R pom.xml` |
| `pbcopy` / `pbpaste` | 剪贴板复制/粘贴 | `cat key.pub \| pbcopy` |
| `mdfind` | Spotlight 命令行搜索 | `mdfind -name "pom.xml"` |
| `defaults` | 读写系统偏好设置 | `defaults read com.apple.dock` |
| `say` | 文字转语音 | `say "build complete"` |
| `screencapture` | 命令行截图 | `screencapture -i ~/Desktop/shot.png` |

### 4.2 开发者常用组合

```bash
# 复制 SSH 公钥到剪贴板（极其常用）
cat ~/.ssh/id_rsa.pub | pbcopy

# 在 Finder 中打开项目
open -R pom.xml

# Spotlight 全局搜索文件（比 find 快很多）
mdfind "kMDItemKind == 'Java Source'"    # 搜索所有 .java 文件
mdfind -onlyin ~/project "spring boot"    # 内容搜索

# 用默认浏览器打开URL
open "http://localhost:8080/swagger-ui.html"

# 打开多个项目
open -a "IntelliJ IDEA" ~/project1
open -a "IntelliJ IDEA" ~/project2

# 设置系统隐藏文件可见
defaults write com.apple.finder AppleShowAllFiles -bool true
killall Finder

# 修改 Dock 自动隐藏速度
defaults write com.apple.dock autohide-delay -float 0
defaults write com.apple.dock autohide-time-modifier -float 0.3
killall Dock
```

### 4.3 launchctl：管理后台服务

```bash
# 查看所有服务
launchctl list

# 查看特定服务
launchctl list | grep com.openssh.sshd

# 加载/卸载服务配置
launchctl load ~/Library/LaunchAgents/com.user.myapp.plist
launchctl unload ~/Library/LaunchAgents/com.user.myapp.plist

# 启动/停止服务
launchctl start com.user.myapp
launchctl stop com.user.myapp
```

---

## 5. oh-my-zsh 与插件生态

### 5.1 安装 oh-my-zsh

```bash
# 安装（二选一）
# curl 方式
sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"

# wget 方式
sh -c "$(wget -O- https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"

# 安装后配置文件在 ~/.zshrc
```

### 5.2 推荐插件（~/.zshrc）

```bash
# === 内置插件（oh-my-zsh plugins 目录下）===
plugins=(
    git                           # git 别名和补全
    docker                        # docker 命令补全
    docker-compose                # docker-compose 补全
    brew                          # Homebrew 补全
    mvn                           # Maven 补全
    gradle                        # Gradle 补全
    macos                         # macOS 专属命令增强
    colored-man-pages             # man 页面彩色
    extract                       # 万能解压（x 任意压缩包）
    z                             # 智能目录跳转（z 目录片段）
    sudo                          # 按两下 Esc 加 sudo
    copyfile                      # 复制文件内容到剪贴板
    copypath                      # 复制当前路径到剪贴板
    web-search                    # 终端内一键搜索
    history                       # 历史命令增强
    command-not-found             # 命令不存在时提示 brew install
)
```

### 5.3 外部插件（安装后启用）

```bash
# === zsh-autosuggestions：灰色提示（必装）===
# 根据历史自动给出灰色建议，→ 键接受
git clone https://github.com/zsh-users/zsh-autosuggestions \
    ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-autosuggestions
# 加入 plugins: ... zsh-autosuggestions

# === zsh-syntax-highlighting：语法高亮（必装）===
# 输入命令时实时高亮：有效命令绿色，无效红色
git clone https://github.com/zsh-users/zsh-syntax-highlighting.git \
    ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-syntax-highlighting
# 加入 plugins: ... zsh-syntax-highlighting

# === fzf：模糊搜索（强烈推荐）===
# Ctrl+R 增强搜索、Ctrl+T 模糊找文件
brew install fzf
$(brew --prefix)/opt/fzf/install

# === powerlevel10k：最强主题 ===
git clone --depth=1 https://github.com/romkatv/powerlevel10k.git \
    ${ZSH_CUSTOM:-$HOME/.oh-my-zsh/custom}/themes/powerlevel10k
# 在 .zshrc 中设置 ZSH_THEME="powerlevel10k/powerlevel10k"
# 然后运行 p10k configure 进行交互式配置
```

### 5.4 完整的 ~/.zshrc 示例

```bash
# === .zshrc ===
export ZSH="$HOME/.oh-my-zsh"
ZSH_THEME="powerlevel10k/powerlevel10k"

# 插件（顺序有讲究）
plugins=(
    git docker docker-compose brew
    mvn gradle
    macos colored-man-pages
    extract z sudo
    copyfile copypath
    web-search history
    command-not-found
    zsh-autosuggestions
    zsh-syntax-highlighting
)

source $ZSH/oh-my-zsh.sh

# === 别名（Java 开发高频）===
alias ll="ls -lh"
alias la="ls -lah"
alias g="git"
alias mci="mvn clean install -DskipTests"
alias mcp="mvn clean package -DskipTests"
alias msb="mvn spring-boot:run"
alias idea="open -a 'IntelliJ IDEA'"

# === 环境变量 ===
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export MAVEN_HOME=$(brew --prefix)/opt/maven

# === fzf 增强 ===
[ -f ~/.fzf.zsh ] && source ~/.fzf.zsh

# === SDKMAN ===
export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]] && source "$SDKMAN_DIR/bin/sdkman-init.sh"

# === 启动时自动加载 powerlevel10k 配置 ===
[[ ! -f ~/.p10k.zsh ]] || source ~/.p10k.zsh
```

---

**上一模块**：[01-macOS系统基础与演进](./01-macOS系统基础与演进.md) ｜ **下一模块**：[03-macOS环境配置与Java开发](./03-macOS环境配置与Java开发.md) ｜ **返回总览**：[00-macOS知识体系总览](./00-macOS知识体系总览.md)

---

*创建于：2026年7月*
