# 01 - 终端与 Shell 入门

> **核心摘要**：终端（Terminal）与 Shell 是两个常被混淆的概念——终端是「屏幕」，Shell 是「大脑」。本文厘清概念、给出 2026 终端模拟器选型、对比五大 Shell、拆解启动流程与提示符定制。

> **前置阅读**：[[00-命令行终端知识体系总览]]

---

## 📚 目录

1. [基础概念辨析](#1-基础概念辨析)
2. [终端模拟器选型](#2-终端模拟器选型)
3. [Shell 类型对比](#3-shell-类型对比)
4. [Shell 启动流程与配置](#4-shell-启动流程与配置)
5. [提示符定制](#5-提示符定制)
6. [环境变量与 PATH](#6-环境变量与-path)
7. [常见陷阱](#7-常见陷阱)
8. [核心要点](#8-核心要点)

---

## 1. 基础概念辨析

> **背景**：Terminal、Console、Shell、命令提示符——四个词天天用，但大多数人分不清。
> **目的**：厘清概念，理解「输入命令后发生了什么」。
> **适用范围**：任何命令行使用者。

```text
四个概念的层级
┌─────────────────────────────────────┐
│ 终端模拟器（Terminal）——「屏幕」      │
│ 例：Windows Terminal / iTerm2 / Kitty │
│ 作用：显示输出、接收键盘输入           │
├─────────────────────────────────────┤
│ Shell（Shell）——「大脑」             │
│ 例：bash / zsh / fish / PowerShell   │
│ 作用：解析命令、执行程序、管理进程     │
├─────────────────────────────────────┤
│ 命令（Command）——「动作」            │
│ 例：ls / cd / git / java             │
│ 作用：被 Shell 解析并执行的程序       │
└─────────────────────────────────────┘
```

| 概念 | 类比 | 说明 |
|------|------|------|
| **终端模拟器** | 电视屏幕 | 只负责「显示与输入」——换 Shell 不影响 |
| **Shell** | 大脑/翻译官 | 解析你的命令，调用程序执行 |
| **Console** | 老式终端 | 历史概念（物理终端），现在泛指命令行窗口 |
| **命令提示符（Prompt）** | 界面提示 | `user@host:~$` 等待输入的状态 |

> 🎯 **关键认知**：**终端和 Shell 是「分离」的**——同一个终端可以切换不同 Shell（`chsh`/`exec zsh`），同一个 Shell 可以在不同终端运行。排障时先分清「是终端问题还是 Shell 问题」。

---

## 2. 终端模拟器选型

### 2.1 2026 终端模拟器对比

| 终端 | 平台 | 特点 | 适用 |
|------|:---:|------|------|
| **Windows Terminal** | Windows | 微软官方、多标签、GPU 加速 | **Windows 默认首选** |
| **Kitty** | Linux/macOS | GPU 加速、配置文本化、内置 ssh wrapper | 性能控 |
| **Ghostty** | 全平台 | 2024 发布、快速、原生 | 新锐（2026 流行） |
| **WezTerm** | 全平台 | 模块化、Vi 风格导航 | 可定制控 |
| **iTerm2** | macOS | macOS 老牌、功能全 | macOS 首选 |
| **tmux** | 终端内 | 复用器（不是模拟器） | 会话管理 |

### 2.2 选型要点（2026）

```text
现代终端的必备能力
├── ① 真彩色（true color）支持——现代配色必需
├── ② Unicode/Emoji 渲染（Nerd Fonts 图标）
├── ③ GPU 加速（大输出不卡）
├── ④ 多标签/分屏
├── ⑤ 字体渲染质量（等宽字体 + 连字）

选型建议
├── Windows → Windows Terminal（官方 + 最成熟）
├── macOS → iTerm2（老牌）或 Ghostty（新锐）
├── Linux → Kitty（性能）或系统默认
└── 全平台一致 → Ghostty / WezTerm
```

> 💡 **字体与图标**：现代终端搭配 **Nerd Fonts**（图标字体）——文件类型图标、Git 状态符号在终端中显示为图形，体验提升明显。

---

## 3. Shell 类型对比

### 3.1 五大 Shell 对比

| Shell | 平台 | 特点 | 2026 定位 |
|-------|:---:|------|---------|
| **bash** | Linux/macOS 默认 | 事实标准、脚本兼容 | **服务器标准** |
| **zsh** | macOS 默认 | bash 兼容 + 增强（补全/主题） | 开发机主流 |
| **fish** | 全平台 | 开箱即用（自动补全/高亮） | 新手指友好 |
| **PowerShell** | Windows 默认 | 对象管道、模块化 | **Windows 标准** |
| **cmd** | Windows 遗留 | 老式批处理 | 仅兼容遗留 |

### 3.2 选择建议

```text
Shell 选择
├── 服务器/生产 → bash（不可动摇的标准）
├── 个人开发机（Linux/macOS）→ zsh（bash 兼容 + 体验好）
├── 想开箱即用 → fish（内置补全/高亮，零配置）
├── Windows 自动化 → PowerShell（对象管道强大）
└── 学习路径：先 bash（服务器必须）→ 再 zsh/fish（体验升级）

fish 的特色（2026 吸引力）
├── 自动补全（基于历史）
├── 语法高亮（内置）
├── 默认配置合理（免折腾）
└── Vi 模式支持
⚠️ fish 不兼容 bash 脚本——写脚本仍用 bash
```

---

## 4. Shell 启动流程与配置

### 4.1 bash 启动流程（配置加载顺序）

```text
bash 启动流程（关键：登录 Shell vs 非登录 Shell）
├── 登录 Shell（ssh/终端首启）：
│   /etc/profile → ~/.bash_profile → ~/.bashrc
├── 非登录 Shell（子 Shell/新标签）：
│   ~/.bashrc（直接）
└── ⚠️ 常见困惑：改了 .bashrc 不生效
    → 因为当前会话没重新加载：source ~/.bashrc

zsh 对应文件
├── ~/.zshrc（主配置，zsh 每次都加载）
├── ~/.zprofile（登录时）
└── ~/.zshenv（所有实例）

fish
├── ~/.config/fish/config.fish
└── 自动补全/函数目录
```

### 4.2 配置优先级与易错点

```text
配置文件的常见坑
├── ⚠️ .bash_profile 与 .bashrc 职责混淆
│   → 惯例：.bash_profile 里 source .bashrc（统一入口）
├── ⚠️ 改配置不生效
│   → source ~/.bashrc 重新加载（或新开终端）
├── ⚠️ 服务器 vs 本地配置差异
│   → 服务器改配置前先备份（.bashrc.bak）
└── ⚠️ 团队环境：公司别名写在 /etc/profile.d/（全局）
```

### 4.3 最小配置模板

```bash
# ~/.bashrc 最小实用配置
# 别名（高频命令）
alias ll='ls -alF'
alias la='ls -A'
alias gs='git status'
alias gp='git pull'
alias ..='cd ..'

# 历史
export HISTSIZE=10000
export HISTFILESIZE=20000

# 提示符（简单版）
PS1='\[\033[01;32m\]\u@\h\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '

# 快捷键（部分终端）
bind '"\C-p": history-search-backward'
```

---

## 5. 提示符定制

### 5.1 提示符基础

```text
提示符（PS1）的组成
├── \u 用户名 / \h 主机名 / \w 工作目录
├── \$ 权限符（$ 普通用户 / # root）
├── \t 时间 / \n 换行
└── 转义序列控制颜色：\[\033[32m\]绿色

示例：user@host:~/project$ 
```

### 5.2 starship：2026 现代提示符

> 🎯 **starship**：跨 Shell 提示符（bash/zsh/fish/PowerShell 通用）——显示 git 分支、命令耗时、Python 环境等上下文信息：

```bash
# 安装
curl -sS https://starship.rs/install.sh | sh

# 启用（bash 在 .bashrc 加）
eval "$(starship init bash)"

# 配置（~/.config/starship.toml）
[character]
success_symbol = "[❯](purple)"
error_symbol = "[❯](red)"

[git_branch]
symbol = " "
```

```text
starship 的价值
├── ① git 状态可视化（分支/变更/冲突）
├── ② 语言环境提示（Python venv/Node 版本）
├── ③ 命令耗时显示（发现慢命令）
├── ④ 跨 Shell 一致（换 Shell 提示符不变）
└── ⑤ 配置简单（TOML）
```

---

## 6. 环境变量与 PATH

### 6.1 环境变量的本质

```text
环境变量 = 进程的「全局配置」
├── 查看：env / echo $PATH
├── 设置（当前会话）：export MY_VAR=value
├── 永久（配置文件）：写入 .bashrc/.zshrc
├── 系统级：/etc/environment（登录时加载）
└── 传递：子进程继承父进程的环境变量

常用变量：
├── PATH：命令搜索路径（哪个目录找命令）
├── HOME：用户主目录
├── LANG/LC_ALL：语言与编码
├── JAVA_HOME / M2_HOME：Java 生态
└── EDITOR：默认编辑器
```

### 6.2 PATH 的陷阱

```text
PATH 常见坑
├── ⚠️ 命令找不到（command not found）→ PATH 没包含该目录
├── ⚠️ 版本冲突：/usr/bin/java vs 自定义 java → PATH 顺序决定
├── ⚠️ Windows 分号 vs Linux 冒号（路径列表分隔符！）
├── ⚠️ 改 PATH 不生效：需要 source 或重开终端
└── 排查：which java / type java（看实际使用的版本）

设置示例：
export PATH="$HOME/.local/bin:$PATH"     # 前置 = 优先
export JAVA_HOME=/opt/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"
```

---

## 7. 常见陷阱

| # | 陷阱 | 表现 | 解法 |
|---|------|------|------|
| 1 | **配置不生效** | 改了 .bashrc 没反应 | `source ~/.bashrc` |
| 2 | **命令找不到** | command not found | 检查 PATH + which |
| 3 | **脚本权限** | Permission denied | `chmod +x script.sh` |
| 4 | **换行符错误** | 脚本报 $'\r' | dos2unix（Windows 编辑过） |
| 5 | **PATH 顺序错** | 用了错误的版本 | 前置自定义目录 |
| 6 | **终端乱码** | 中文乱码 | LANG/编码设置 |
| 7 | **别名遮蔽** | alias 覆盖了原命令 | `\ls` 用原命令 / unalias |
| 8 | **rm 误删** | 无回收站 | 谨慎 + alias rm='rm -i' |

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 终端是「屏幕」、Shell 是「大脑」——排障先分清是哪层的问题
> 2. 2026 终端选型：Windows Terminal / Kitty / Ghostty + Nerd Fonts 图标
> 3. Shell 选择：服务器 bash、开发机 zsh、Windows PowerShell、尝鲜 fish
> 4. 启动流程：登录 Shell 加载 .bash_profile→.bashrc；改配置 `source` 生效
> 5. starship 让提示符显示 git/耗时/环境——2026 效率标配
> 6. PATH 顺序决定命令版本——`which` 排查第一招

---

**下一模块**：[02-跨平台命令对照表](02-跨平台命令对照表.md) | **返回总览**：[00-命令行终端知识体系总览](00-命令行终端知识体系总览.md)
