# macOS 文件系统、进程与安全

> 🗄️ APFS 不只是"新一代文件系统"，时光机器快照、空间克隆、原生加密 —— 理解 macOS 底层存储、launchd 服务管理和多层安全机制

---

## 📚 目录

1. [APFS 文件系统](#1-apfs-文件系统)
2. [Bundle 与文件结构](#2-bundle-与文件结构)
3. [macOS 权限模型](#3-macos-权限模型)
4. [launchd：统一服务管理器](#4-launchd统一服务管理器)
5. [安全分层体系](#5-安全分层体系)

---

## 1. APFS 文件系统

### 1.1 macOS 文件系统演进

| 文件系统 | 使用时期 | 关键特性 |
|---------|:-----:|------|
| **HFS** (Mac OS Standard) | 1985-1998 | 最早的 Mac 文件系统 |
| **HFS+** (Mac OS Extended) | 1998-2017 | 日志、别名、压缩 |
| **APFS** (Apple File System) | 2017-现在 | 快照、克隆、加密、空间共享 |

> 💡 **关键节点**：macOS 10.13 High Sierra 开始迁移到 APFS，SSD 必须使用 APFS；macOS 10.15 Catalina 后系统盘强制 APFS。

### 1.2 APFS 核心特性

| 特性 | 说明 | 开发者影响 |
|------|------|-----------|
| **快照 (Snapshot)** | 文件系统状态的时间点冻结副本 | Time Machine 的基础 |
| **克隆 (Clone)** | 复制文件不占额外空间，写时复制 (COW) | `cp` 瞬间完成 |
| **空间共享 (Space Sharing)** | 同一容器内多个卷共享空间 | 无需分区，按需动态分配 |
| **加密** | 原生 AES 加密，多密钥体系 | FileVault 全盘加密 |
| **崩溃保护** | 元数据修改是原子操作 | 减少数据损坏 |
| **稀疏文件** | 实际存储小于逻辑大小 | 节省空间 |
| **快目录大小统计** | 极速获取目录实际大小 | Finder 实时显示大小 |

### 1.3 APFS 容器与卷结构

```text
物理磁盘 (SSD)
└── APFS 容器 (Container)
    ├── Macintosh HD (系统卷) ← 只读系统文件
    ├── Macintosh HD - Data (数据卷) ← 用户数据、应用
    ├── Preboot (引导)
    ├── Recovery (恢复)
    └── VM (虚拟内存交换)
    
所有卷共享同一容器内的空闲空间，无需手动分区！
```

```bash
# 查看 APFS 卷
diskutil list
diskutil apfs list

# 查看磁盘使用情况
df -h
diskutil info / | grep "APFS"

# 创建 APFS 快照
tmutil localsnapshot    # Time Machine 本地快照
tmutil listlocalsnapshots /
```

### 1.4 常用文件操作

```bash
# === 查找大文件 ===
# 当前目录下大于 100MB 的文件
find . -type f -size +100M -exec ls -lh {} \;

# 目录大小排序 Top 10 (macOS 风格)
du -sh * | sort -rh | head -10

# === 文件属性 ===
# 查看完整信息
stat pom.xml

# 查看扩展属性 (xattr)
xattr -l pom.xml
xattr -d com.apple.quarantine pom.xml   # 删除隔离属性

# 查看元数据 (Spotlight)
mdls pom.xml

# === 权限修复 ===
# 修复 Homebrew 权限
sudo chown -R $(whoami) /opt/homebrew

# 批量修改所有者
sudo chown -R $(whoami):staff ~/projects
```

### 1.5 macOS 特有目录

```bash
# /tmp vs /private/tmp
# /tmp → /private/tmp（符号链接），重启清空

# 获取真正的临时目录（推荐用于脚本）
mktemp -d

# macOS 隐藏目录
~/Library/
├── Application Support/  # 应用数据（类似 Windows AppData）
├── Caches/               # 缓存（可清理）
├── Preferences/          # plist 配置文件
├── LaunchAgents/         # 用户级 launchd 服务
├── Logs/                 # 应用日志
└── Containers/           # 沙盒应用数据
```

---

## 2. Bundle 与文件结构

### 2.1 Bundle 是什么

Bundle 是 macOS/iOS 的核心设计模式：**一个看起来是文件的目录**。

```bash
# .app 应用 → 实际是目录
ls ~/Applications/IntelliJ\ IDEA.app/Contents/
# Info.plist  MacOS/  Resources/  bin/  lib/

# .jdk 也是 Bundle 目录
ls /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/
# Home/  Info.plist  MacOS/

# 查看 Bundle 内部结构
ls /Applications/Visual\ Studio\ Code.app/Contents/
```

### 2.2 常见 Bundle 类型

| 扩展名 | 类型 | 说明 |
|--------|------|------|
| `.app` | 应用程序 | 可执行文件 + 资源 + plist |
| `.framework` | 框架（库） | 动态库 + 头文件 + 资源 |
| `.bundle` | 插件/资源包 | 可加载的代码或资源 |
| `.jdk` | JDK Bundle | Java 运行环境 |
| `.kext` | 内核扩展（已弃用） | 设备驱动 |
| `.systemextension` | 系统扩展（新） | 替代 kext |
| `.pkg` | 安装包 | Flat Package 格式 |
| `.dmg` | 磁盘映像 | 分发格式 |
| `.workflow` | Automator 工作流 | 自动化脚本 |
| `.savedSearch` | 智能文件夹 | Spotlight 搜索条件 |

```bash
# 查看 Bundle 的 Info.plist（元数据核心）
plutil -p /Applications/IntelliJ\ IDEA.app/Contents/Info.plist
# 查看版本号
defaults read /Applications/IntelliJ\ IDEA.app/Contents/Info.plist CFBundleShortVersionString
```

---

## 3. macOS 权限模型

### 3.1 Unix 基础权限 + ACL

```bash
# macOS 在标准 Unix 权限之上叠加 ACL
ls -le          # -e 显示 ACL
# drwxr-xr-x +  ← + 表示有 ACL
# 0: group:everyone deny delete

# 查看
ls -lO          # -O 显示文件标志（schg, hidden...）

# 修改 ACL
chmod +a "user:${USER} allow delete" file.txt     # 添加
chmod -a "user:${USER} allow delete" file.txt     # 删除
```

### 3.2 TCC（透明、同意和控制）

TCC 是 macOS 的隐私权限管理系统，Java 开发者可能遇到：

```bash
# TCC 数据库位置（SQLite）
# 用户级: ~/Library/Application Support/com.apple.TCC/TCC.db
# 系统级: /Library/Application Support/com.apple.TCC/TCC.db

# 开发中会遇到 TCC 弹窗的权限：
├── 文件与文件夹：访问桌面/文档/下载
├── 全盘访问（Full Disk Access）
├── 辅助功能（Accessibility）→ 快捷键/自动化
├── 屏幕录制 → 截图工具
├── 摄像头 / 麦克风 → 视频会议
├── 开发者工具
└── 自动化（Apple Events）→ 控制其他应用

# 命令行重置 TCC 授权（需先关闭 SIP）
tccutil reset All com.jetbrains.intellij
tccutil reset Camera
```

### 3.3 SIP（系统完整性保护）

```bash
# SIP 保护的关键目录（即使 root 也无法修改）
# /System/
# /usr/ (除了 /usr/local/)
# /bin/
# /sbin/
# /var/db/ConfigurationProfiles

# 查看 SIP 状态
csrutil status

# SIP 通常在以下场景影响开发者：
# 1. 无法修改 /usr/bin 下的系统文件
# 2. 无法加载未签名的内核扩展
# 3. 某些调试器功能受限

# 不建议永久关闭 SIP，如必需可在 Recovery 中操作
# 重启按住 Cmd+R → 终端 → csrutil disable
```

> ⚠️ **强烈建议不要关闭 SIP**。大部分开发需求可以通过权限授权或使用 `/usr/local/` / `/opt/homebrew/` 替代。

---

## 4. launchd：统一服务管理器

### 4.1 launchd 是什么

launchd (PID 1) 是 macOS 的**第一个进程**，统一了传统 Unix 中多个服务：

```text
launchd = systemd + cron + inetd + xinetd
        ├── 系统/用户服务管理
        ├── 定时任务（替代 cron）
        ├── 按需启动（替代 inetd）
        └── 资源限制、标准输出重定向
```

### 4.2 launchd 配置位置

| 路径 | 作用域 | 运行身份 | 时机 |
|------|:---:|------|------|
| `/System/Library/LaunchDaemons/` | 系统级守护 | root | 系统启动 |
| `/Library/LaunchDaemons/` | 全局守护 | root | 系统启动 |
| `/Library/LaunchAgents/` | 全局代理 | 当前用户 | 用户登录 |
| `~/Library/LaunchAgents/` | 用户代理 | 当前用户 | 用户登录 |

### 4.3 plist 配置详解

将 Java 应用注册为 launchd 服务：

```xml
<!-- ~/Library/LaunchAgents/com.user.springboot.plist -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- 唯一标识 -->
    <key>Label</key>
    <string>com.user.springboot</string>

    <!-- 启动程序 -->
    <key>ProgramArguments</key>
    <array>
        <string>/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin/java</string>
        <string>-jar</string>
        <string>/Users/user/projects/myapp/target/app.jar</string>
        <string>--spring.profiles.active=prod</string>
    </array>

    <!-- 工作目录 -->
    <key>WorkingDirectory</key>
    <string>/Users/user/projects/myapp</string>

    <!-- 环境变量 -->
    <key>EnvironmentVariables</key>
    <dict>
        <key>JAVA_HOME</key>
        <string>/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home</string>
    </dict>

    <!-- 运行时机：用户登录时 -->
    <key>RunAtLoad</key>
    <true/>

    <!-- 崩溃自动重启 -->
    <key>KeepAlive</key>
    <true/>

    <!-- 日志输出 -->
    <key>StandardOutPath</key>
    <string>/Users/user/projects/myapp/logs/stdout.log</string>
    <key>StandardErrorPath</key>
    <string>/Users/user/projects/myapp/logs/stderr.log</string>
</dict>
</plist>
```

```bash
# 管理 launchd 服务
launchctl load ~/Library/LaunchAgents/com.user.springboot.plist   # 加载
launchctl unload ~/Library/LaunchAgents/com.user.springboot.plist # 卸载

# 查看服务
launchctl list | grep springboot

# 启动/停止
launchctl start com.user.springboot
launchctl stop com.user.springboot

# 定时任务（替代 cron）
# 每天凌晨 2 点清理日志
<key>StartCalendarInterval</key>
<dict>
    <key>Hour</key><integer>2</integer>
    <key>Minute</key><integer>0</integer>
</dict>
```

### 4.4 launchctl 常用命令

```bash
launchctl list                          # 查看所有已加载服务
launchctl list | grep -v "com.apple"    # 只看第三方服务
launchctl print gui/$(id -u)            # 查看当前用户域详情
launchctl print system                  # 查看系统域详情
launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/xxx.plist   # 引导加载
launchctl bootout gui/$(id -u)/com.user.xxx  # 卸载
```

---

## 5. 安全分层体系

### 5.1 防火墙 (pf + ALF)

```bash
# === 应用层防火墙 (ALF) ===
# GUI 配置：系统设置 → 网络 → 防火墙
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate

# 命令行管理
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate on
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off

# 开放特定端口（开发用）
# ALF 不擅长端口规则，用 pf (Packet Filter) 更灵活

# === PF 防火墙（BSD 包过滤器）===
# 查看默认 PF 配置
cat /etc/pf.conf

# 查看当前规则
sudo pfctl -s rules
sudo pfctl -s all

# 临时启用/禁用
sudo pfctl -e     # 启用
sudo pfctl -d     # 禁用
```

### 5.2 Gatekeeper + 公证

```bash
# 查看配置
spctl --status
# → assessments enabled  (Gatekeeper 已启用)

# 查看允许的应用来源
spctl --list
# 可选值：Anywhere / Mac App Store / App Store and identified developers

# 允许来自任何来源（不推荐，安全风险）
sudo spctl --master-disable

# 恢复默认
sudo spctl --master-enable

# 强制打开被阻止的应用
xattr -dr com.apple.quarantine /path/to/app.app

# 检查应用的公证状态
spctl -a -vvv -t execute /Applications/IntelliJ\ IDEA.app
```

### 5.3 开发者常用安全检查

```bash
# 1. 检查哪些应用有全盘访问权限
sudo sqlite3 /Library/Application\ Support/com.apple.TCC/TCC.db \
  "SELECT client FROM access WHERE service='kTCCServiceSystemPolicyAllFiles' AND auth_value=2"

# 2. 检查开机自启项
# GUI: 系统设置 → 通用 → 登录项
# CLI:
osascript -e 'tell application "System Events" to get the name of every login item'
ls ~/Library/LaunchAgents/

# 3. 检查 SSH 密钥权限
ls -la ~/.ssh/
# 私钥必须是 600
chmod 600 ~/.ssh/id_rsa
# 公钥可以是 644
chmod 644 ~/.ssh/id_rsa.pub

# 4. 检查 .zshrc 中是否有可疑代码
cat ~/.zshrc | grep -E "curl|wget|eval|bash -c"
```

---

**上一模块**：[04-Homebrew包管理与软件生态](./04-Homebrew包管理与软件生态.md) ｜ **下一模块**：[06-macOS开发者实战手册](./06-macOS开发者实战手册.md) ｜ **返回总览**：[00-macOS知识体系总览](./00-macOS知识体系总览.md)

---

*创建于：2026年7月*
