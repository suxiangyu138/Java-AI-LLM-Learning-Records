# Shell 效率与 tmux 终端复用

> ⚡ alias 别名、fzf 模糊查找、Ctrl+R 历史、tmux 多窗口 —— 命令行效率 10x

---

## 📚 目录

1. [alias 别名系统](#1-alias-别名系统)
2. [历史记录与搜索](#2-历史记录与搜索)
3. [fzf 模糊查找](#3-fzf-模糊查找)
4. [tmux 终端复用器](#4-tmux-终端复用器)

---

## 1. alias 别名系统

```bash
# ~/.bashrc 或 ~/.zshrc
alias rm='rm -i' && alias cp='cp -i' && alias mv='mv -i'
alias ll='ls -lah' && alias la='ls -A'
alias ..='cd ..' && alias ...='cd ../..'

# Git 极速
alias g='git' && alias gs='git status'
alias ga='git add' && alias gc='git commit -m'
alias gp='git push' && alias gl='git log --oneline --graph'

# Docker
alias d='docker' && alias dc='docker compose'

# 常用
alias myip='curl -s ifconfig.me'
alias ports='ss -tlnp'
```

```powershell
# PowerShell $PROFILE
Set-Alias -Name g -Value git
function .. { Set-Location .. }
```

---

## 2. 历史记录与搜索

```bash
# 配置
export HISTSIZE=10000
export HISTFILESIZE=20000
export HISTCONTROL=ignoredups:erasedups

# 操作
Ctrl+R    → 反向搜索历史（最高频！输入关键词回车执行）
!!        → 重复上条命令
!$        → 上条命令的最后一个参数
!git      → 最近一条 git 开头的命令
history   → 全部历史
```

---

## 3. fzf 模糊查找

```bash
sudo apt install fzf   # 或 sudo dnf install fzf

# 自带快捷键
Ctrl+T    → 粘贴选中文件路径
Ctrl+R    → fzf 增强版历史搜索
Alt+C     → cd 到选中目录

# 实战组合
vim $(fzf)                                    # 选文件→vim 打开
git checkout $(git branch | fzf)              # 选分支切换
kill -9 $(ps aux | fzf | awk '{print $2}')    # 选进程 kill
```

---

## 4. tmux 终端复用器

### 4.1 核心概念

```text
tmux = 终端复用器。三层模型：Server → Session → Window → Pane

核心价值：
  ✅ SSH 断线不丢工作（detach→重连 attach）
  ✅ 多项目多会话管理
  ✅ 分屏：边编辑边 tail -f 日志

命令：
  tmux new -s dev       # 新建会话
  tmux ls               # 列会话
  tmux attach -t dev    # 重连
```

### 4.2 快捷键（Prefix = Ctrl+b）

```text
会话：  d=断开  s=列会话
窗口：  c=新建  ,=重命名  n/p=切换  0-9=跳转
窗格：  %=垂直分  "=水平分  方向键=切换  z=全屏  x=关闭
```

### 4.3 推荐 ~/.tmux.conf

```bash
# 改 Prefix 为 Ctrl+a
unbind C-b && set -g prefix C-a && bind C-a send-prefix
set -g mouse on                     # 鼠标支持
set -g base-index 1                 # 窗口从1编号
bind r source-file ~/.tmux.conf \; display "Reloaded!"  # 重载配置
```

---

> 🎯 **效率三件套**：**alias** 缩短命令、**Ctrl+R** 搜历史、**tmux** 永不丢失工作现场。

---

*创建于：2026年7月*
