# 03 - Shell 效率与 tmux

> **核心摘要**：命令行效率 10x 的四大武器——alias 别名系统、历史记录与 Ctrl+R、fzf 模糊查找、tmux 终端复用。本文覆盖每个工具的配置、使用与组合玩法，并给出 2026 现代效率工作流。

> **前置阅读**：[[01-终端与Shell入门]]、[[02-跨平台命令对照表]]

---

## 📚 目录

1. [效率四件套总览](#1-效率四件套总览)
2. [alias 别名系统](#2-alias-别名系统)
3. [历史记录与搜索](#3-历史记录与搜索)
4. [fzf：模糊查找一切](#4-fzf模糊查找一切)
5. [tmux 终端复用器](#5-tmux-终端复用器)
6. [tmux 进阶：会话管理与分屏](#6-tmux-进阶会话管理与分屏)
7. [组合工作流（2026 现代实践）](#7-组合工作流2026-现代实践)
8. [核心要点](#8-核心要点)

---

## 1. 效率四件套总览

> **背景**：命令行的效率瓶颈不在「敲得快」，而在「找得准」——找命令、找文件、找历史、找窗口。
> **目的**：用四件套消灭「查找」的时间成本。
> **适用范围**：日常开发的任何命令行操作。

```text
效率四件套
├── ① alias：把常用命令缩短（消灭重复输入）
├── ② 历史搜索：Ctrl+R / fzf 历史（消灭重新输入）
├── ③ fzf：模糊查找一切（文件/历史/进程/命令）
└── ④ tmux：终端复用（消灭窗口管理/会话丢失）

组合效果：5 秒内完成「找到文件 → 打开 → 执行 → 切换」
```

---

## 2. alias 别名系统

### 2.1 基础用法

```bash
# 定义别名（.bashrc/.zshrc 中）
alias ll='ls -alF'
alias gs='git status'
alias ga='git add'
alias gcm='git commit -m'
alias gp='git push'
alias gl='git log --oneline --graph'

# 带参数别名（函数实现）
deploy() {
    ./scripts/build.sh && ssh prod "bash /opt/deploy.sh"
}

# 查看/取消
alias            # 列出所有别名
alias gs         # 查看单个
unalias gs       # 取消
```

### 2.2 安全别名（防误操作）

```bash
# 高危命令的确认保护
alias rm='rm -i'                    # 删除前确认（⚠️ 重要）
alias mv='mv -i'
alias cp='cp -i'

# 覆盖检查
alias mv='mv -i'
# 使用原命令：\rm file（前导反斜杠绕过别名）
```

> 🎯 **别名规范**：短（≤3 字符）、可记忆、团队统一（共享 .bashrc 片段）——`gs`/`gp`/`gcm` 是 Git 操作的事实标准缩写。

---

## 3. 历史记录与搜索

### 3.1 历史基础

```bash
history             # 查看历史
history 20          # 最近 20 条
!!                  # 上一条命令
!123                # 执行历史第 123 条
!gs                 # 执行最近以 gs 开头的命令
!$                  # 上一条命令的最后一个参数
^old^new            # 替换上一条命令的文本并执行

# 历史设置（.bashrc）
export HISTSIZE=10000       # 会话历史条数
export HISTFILESIZE=20000   # 历史文件条数
export HISTTIMEFORMAT="%F %T "   # 显示时间
```

### 3.2 Ctrl+R 反向搜索（核心技能）

```text
Ctrl+R 反向搜索
├── ① 按 Ctrl+R → 输入关键字
├── ② 逐条回溯匹配的历史命令
├── ③ 再按 Ctrl+R 继续回溯 / Enter 执行 / Esc 退出
└── 效率点：不用完整输入命令，几个关键字即可

进阶：fzf 接管历史搜索（更强）
├── 安装 fzf 后配置：Ctrl+R 弹出模糊搜索界面
├── 支持关键字 + 模糊匹配 + 预览
└── 2026 推荐：历史搜索一律 fzf 化
```

```bash
# fzf 接管 Ctrl+R（.bashrc）
eval "$(fzf --bind 'ctrl-r:history')"   # 或使用 fzf 的默认集成
# 更现代：atuin（历史管理工具）
# 安装后历史进入 SQLite：搜索更快 + 支持上下文 + 多机同步
```

---

## 4. fzf：模糊查找一切

### 4.1 安装与基础

> **背景**：fzf（fuzzy finder）是命令行通用模糊查找器——输入关键字，模糊匹配 + 实时预览。
> **目的**：把「找文件/找历史/找进程」统一成同一个交互。
> **适用范围**：文件查找、历史搜索、进程管理、目录跳转、Git 操作。

```bash
# 安装
sudo apt install fzf      # Debian/Ubuntu（自带 bash/zsh 集成）
brew install fzf          # macOS
winget install junegunn.fzf  # Windows

# 基础使用
fzf                       # 交互式文件选择（Enter 输出选中路径）
ls | fzf                  # 管道过滤（任何列表可模糊搜）
```

### 4.2 高频场景

```bash
# ① 打开文件（替代翻目录）
vim $(fzf)                # 模糊选文件用 vim 打开

# ② 目录跳转（配合 zoxide）
cd $(fzf --type d)        # 模糊选目录

# ③ 搜索进程并杀
kill $(ps aux | fzf | awk '{print $2}')

# ④ Git 分支切换
git checkout $(git branch | fzf)

# ⑤ 历史命令
$(fzf --history)          # 或 Ctrl+R 集成

# ⑥ 搜索并预览文件内容
fzf --preview 'bat --color=always {}'   # 预览选中文件
```

### 4.3 fzf 的配置（2026 推荐）

```bash
# .bashrc/.zshrc
export FZF_DEFAULT_COMMAND='rg --files --hidden'   # 默认搜索（rg 加速）
export FZF_DEFAULT_OPTS='--height 40% --preview "bat --color=always {}"'

# 快捷键（默认已配置）
# Ctrl+T  文件选择（粘贴路径）
# Ctrl+R  历史搜索
# Alt+C   目录跳转
```

> 🎯 **fzf 的本质**：**把「精确记忆」变成「模糊识别」**——不用记得文件名/命令全文，几个字符 + 回车即可。配合 rg（搜索）+ bat（预览）是 2026 标准组合。

---

## 5. tmux 终端复用器

### 5.1 核心价值

> **背景**：SSH 断开 = 会话丢失（正在跑的任务被杀）；多任务需要开多个终端窗口。
> **目的**：会话持久化（断开重连不丢）+ 一个终端多窗口/分屏。
> **适用范围**：远程开发（SSH + tmux 是标配）、长时间任务（日志跟随/构建）、多任务并行。
> **不适用场景**：纯本地 GUI 环境（终端窗口够用）。

```text
tmux 三概念（必须理解）
├── Session（会话）：一个工作区（可持久化）
├── Window（窗口）：会话内多个标签页
└── Pane（窗格）：窗口内分屏

关键价值：
├── ① 断开不丢：SSH 断了重连 → tmux attach 恢复
├── ② 后台运行：任务在 tmux 里跑，关终端不中断
├── ③ 多人协作：tmux 共享会话（团队排障）
```

### 5.2 基础命令

```bash
# 创建/进入/退出
tmux                    # 新建会话
tmux new -s work        # 命名会话
tmux detach             # 脱离（Ctrl+b d）——会话在后台继续
tmux attach -t work     # 重新接入
tmux ls                 # 列出会话
tmux kill-session -t work   # 删除会话

# 快捷键前缀：Ctrl+b（然后按命令键）
# Ctrl+b d    脱离会话
# Ctrl+b c    新建窗口
# Ctrl+b n/p  切换窗口
# Ctrl+b %    左右分屏
# Ctrl+b "    上下分屏
# Ctrl+b 方向键  切换窗格
# Ctrl+b z    全屏/还原当前窗格
# Ctrl+b ,    重命名窗口
```

### 5.3 SSH + tmux 标准工作流

```bash
# 远程开发标准流程
ssh server                # ① 连接服务器
tmux new -s dev           # ② 创建会话（或 attach 恢复）
# ...工作（跑服务/看日志/编辑）...
Ctrl+b d                  # ③ 脱离（任务继续跑）
# 断线/回家后：
ssh server && tmux attach # ④ 恢复一切（进程还在！）

# 跑长时间任务（构建/测试）
tmux new -d -s build 'mvn package'   # 后台会话跑构建
tmux attach -t build                 # 查看进度
```

---

## 6. tmux 进阶：会话管理与分屏

### 6.1 分屏布局

```text
开发常用布局（一键恢复）
┌─────────────┬─────────────┐
│  编辑器      │  终端/运行    │
│  (Neovim)   │  (服务日志)   │
├─────────────┼─────────────┤
│  Git/命令    │  测试/辅助    │
└─────────────┴─────────────┘
操作：
Ctrl+b %     左右分屏
Ctrl+b "     上下分屏
Ctrl+b 方向   切换窗格
Ctrl+b z     临时全屏（聚焦单个）
```

### 6.2 配置文件（~/.tmux.conf）

```bash
# ~/.tmux.conf 核心配置（2026 推荐）
# 前缀改为 Ctrl+a（避免与 Ctrl+b 冲突——vim 场景）
set -g prefix C-a
unbind C-b
bind C-a send-prefix

# 鼠标支持（分屏/选择）
set -g mouse on

# 状态栏
set -g status-bg black
set -g status-left '#[fg=green]#S #[fg=white]| '
set -g status-right '#[fg=yellow]%H:%M'

# 窗口编号从 1 开始
set -g base-index 1

# 分屏快捷键（去掉前缀，直接 Ctrl+方向）
bind -n C-Left select-pane -L
bind -n C-Right select-pane -R
bind -n C-Up select-pane -U
bind -n C-Down select-pane -D
```

### 6.3 常见问题

```text
tmux 常见问题
├── ⚠️ 断线重连后窗口大小错乱 → Ctrl+b :resize-window -A（或重设）
├── ⚠️ tmux 里颜色不对 → 设置 TERM=xterm-256color
├── ⚠️ 复制粘贴不方便 → 配置鼠标 + 缓冲区
├── ⚠️ 忘记会话名 → tmux ls 查看
└── ✅ tmux + Neovim 联动：vim-tmux-navigator 统一 Ctrl+hjkl 导航
```

---

## 7. 组合工作流（2026 现代实践）

### 7.1 完整效率栈

```text
2026 现代命令行效率栈
┌─────────────────────────────────────┐
│ 终端：Kitty / Windows Terminal       │
├─────────────────────────────────────┤
│ Shell：zsh / fish（补全 + 高亮）      │
├─────────────────────────────────────┤
│ 提示符：starship（git/环境/耗时）     │
├─────────────────────────────────────┤
│ 查找：fzf（文件/历史/一切）+ rg       │
├─────────────────────────────────────┤
│ 历史：atuin（SQLite + 加密同步）      │
├─────────────────────────────────────┤
│ 目录：zoxide（智能跳转）              │
├─────────────────────────────────────┤
│ 复用：tmux（会话持久化 + 分屏）       │
├─────────────────────────────────────┤
│ 编辑：Neovim（Vim 键位 + LSP）       │
└─────────────────────────────────────┘
```

### 7.2 一天的高效命令行（示例）

```text
9:00   z proj（zoxide 一秒进项目）
       tmux attach（恢复昨天的会话——服务还在跑！）
9:05   Ctrl+R 搜索历史命令（fzf 界面秒选）
       vim $(fzf)（模糊打开要改的文件）
10:00  rg "TODO" src（全项目搜索）
       Ctrl+b % 分屏跑测试
14:00  fzf 选 git 分支切换
       lazygit（Git TUI 可视化操作）
16:00  atuin 搜索上周的部署命令
17:30  Ctrl+b d 脱离（一切保留，明天 attach 恢复）
```

> 🎯 **金句**：现代命令行效率 = **找得到（fzf/atuin）× 记得住（zoxide/starship）× 不丢失（tmux）× 敲得少（alias/补全）**——四者组合，双手不离键盘。

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 效率四件套：alias（缩短）/历史搜索（复用）/fzf（模糊找）/tmux（会话持久）
> 2. alias 规范：短 + 可记忆 + 团队统一；高危命令加 `-i` 确认
> 3. Ctrl+R 反向搜索是基础技能——fzf/atuin 接管后更强（模糊 + 预览 + 同步）
> 4. tmux 三概念（会话/窗口/窗格）：**SSH + tmux 是远程开发标配**——断开重连不丢进程
> 5. 2026 完整栈：Kitty + zsh/fish + starship + fzf/rg + zoxide + atuin + tmux + Neovim

---

**下一模块**：[04-批处理与自动化脚本](04-批处理与自动化脚本.md) | **返回总览**：[00-命令行终端知识体系总览](00-命令行终端知识体系总览.md)
