# 终端与 Shell 入门

> 🖥️ Terminal vs Shell vs Console 概念辨析、6款终端模拟器、Bash/Zsh/Fish/PowerShell/cmd 对比、配置文件加载流程、提示符定制

---

## 📚 目录

1. [基础概念辨析](#1-基础概念辨析)
2. [终端模拟器选型](#2-终端模拟器选型)
3. [Shell 类型对比](#3-shell-类型对比)
4. [Shell 启动流程与配置](#4-shell-启动流程与配置)
5. [提示符定制](#5-提示符定制)

---

## 1. 基础概念辨析

```text
三层关系（常被混用）：

  Terminal（终端模拟器）→ 你看到的窗口
    → GUI 程序：Windows Terminal, iTerm2, GNOME Terminal
    → 负责：显示、字体、颜色、多标签

  Shell（命令解释器）→ 终端里跑的程序
    → Bash, Zsh, Fish, PowerShell, cmd.exe
    → 负责：解析命令、执行、返回结果

  Console / TTY → 操作系统底层输入输出
    → 用户基本不直接接触

类比：
  浏览器 = Terminal
  JS 引擎 = Shell
  网络栈 = Console
```

```bash
echo $SHELL          # 当前 Shell
cat /etc/shells      # 已安装的 Shell 列表
chsh -s /bin/zsh     # 切换默认 Shell
```

---

## 2. 终端模拟器选型

| 终端 | 平台 | 亮点 |
|------|:--:|------|
| **Windows Terminal** | Win | GPU加速、标签页、分屏、JSON配置 |
| **iTerm2** | macOS | 分屏、热键唤出、Shell Integration |
| **Alacritty** | 跨平台 | GPU加速、YAML配置、极简 |
| **Warp** | macOS | AI 加持、IDE 化 |
| **Tabby** | 跨平台 | 颜值高、SSH管理、插件 |

### 通用终端快捷键

| 键 | 功能 | 键 | 功能 |
|----|------|----|------|
| `Ctrl+C` | 中断 | `Ctrl+D` | EOF/退出 |
| `Ctrl+L` | 清屏 | `Ctrl+R` | 历史搜索 |
| `Ctrl+A` | 行首 | `Ctrl+E` | 行尾 |
| `Ctrl+U` | 删到行首 | `Ctrl+K` | 删到行尾 |
| `Ctrl+W` | 删前一词 | `Ctrl+Shift+C/V` | 复制/粘贴 |

---

## 3. Shell 类型对比

| Shell | 平台 | 后缀 | 配置文件 | 特点 |
|-------|:--:|:---:|---------|------|
| **Bash** | 跨平台 | `.sh` | `~/.bashrc` | 最通用，Linux 默认 |
| **Zsh** | 跨平台 | `.sh` | `~/.zshrc` | Bash 超集，macOS 默认，Oh My Zsh |
| **Fish** | 跨平台 | `.fish` | `config.fish` | 开箱即用，自动建议 |
| **PowerShell** | Win/Linux | `.ps1` | `$PROFILE` | .NET 对象管道 |
| **cmd.exe** | Win | `.bat` | 无配置文件 | 旧式，兼容 DOS |

### 语法差异速查

```bash
# ===== 变量 =====
# Bash/Zsh:    name="hello"
# PowerShell:  $name = "hello"
# cmd:         set name=hello

# ===== 条件 =====
# Bash:        if [ "$a" -eq 1 ]; then ... fi
# PowerShell:  if ($a -eq 1) { ... }
# cmd:         if %a%==1 ( ... )

# ===== 管道 =====
# Bash:        文本流（cat file | grep error）
# PowerShell:  对象流（Get-Content file | Select-String error）
# cmd:         文本流（type file | find "error"）
```

### Oh My Zsh 速装

```bash
sh -c "$(curl -fsSL https://raw.github.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"

# ~/.zshrc 推荐插件
plugins=(git docker zsh-autosuggestions zsh-syntax-highlighting fzf extract)
```

---

## 4. Shell 启动流程与配置

```text
Bash：
  Login Shell (SSH)：  /etc/profile → ~/.bash_profile → ~/.bashrc
  Interactive Shell：  ~/.bashrc

Zsh：
  Login:     /etc/zprofile → ~/.zprofile
  Interactive: /etc/zshrc → ~/.zshrc

PowerShell：
  查看 Profile 路径：$PROFILE
  编辑：notepad $PROFILE

建议：所有自定义配置放 ~/.bashrc 或 ~/.zshrc
```

---

## 5. 提示符定制

```bash
# Bash PS1（~/.bashrc）
export PS1='\u@\h:\w\$ '        # user@host:~/dir$
# \u=用户名 \h=主机名 \w=当前目录

# Zsh 主题（~/.zshrc）
ZSH_THEME="powerlevel10k"        # 推荐：极速、可定制
# 或 "agnoster"（需 Powerline 字体）
```

```powershell
# PowerShell 提示符（$PROFILE）
function prompt {
    "$(Get-Date -Format 'HH:mm') PS $pwd> "
}
```

---

> 🎯 **Terminal 是窗口，Shell 是大脑**。Linux/macOS 用 Bash/Zsh + Oh My Zsh，Windows 用 Windows Terminal + PowerShell 7。`Ctrl+R` 搜历史最高频。

---

*创建于：2026年7月*
