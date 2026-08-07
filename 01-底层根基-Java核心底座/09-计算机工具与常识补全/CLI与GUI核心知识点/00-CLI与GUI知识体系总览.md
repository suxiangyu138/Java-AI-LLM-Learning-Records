# 00 - CLI 与 GUI 知识体系总览

> 🎯 CLI 是后端日常操作界面、GUI 是理解交互设计的基础、TUI 是终端里的应用开发 — Java 后端不仅要会写 API，还要能写命令行工具、理解图形界面原理，2026 年 AI 工具浪潮让 TUI 成为终端开发的新主场

---

## 1. 知识全景

```
CLI与GUI（13个文件）
│
├── 🖥️ CLI 命令行（01-05）
│   ├── 01-CLI概述与设计哲学.md      # CLI vs GUI 本质/管道哲学/Unix设计原则
│   ├── 02-Linux常用CLI工具链.md     # grep/awk/sed/find/xargs/jq 组合技
│   ├── 03-Java-CLI框架-Picocli.md   # Picocli/JCommander/Spring Shell
│   ├── 04-构建交互式终端应用.md      # 进度条/彩色输出/表格/交互式选择
│   └── 05-终端美化与效率工具.md      # Zsh/Oh My Zsh/tmux/fzf/alias
│
├── 🧩 TUI 终端用户界面（11-12）
│   ├── 11-TUI终端用户界面：概念与终端原理.md  # 终端协议/渲染模型/事件循环/陷阱
│   └── 12-TUI框架生态与工程实践.md           # Textual/Bubble Tea/Ratatui/Ink/TamboUI
│
├── 🪟 GUI 图形界面（06-09）
│   ├── 06-GUI概述与Java桌面生态.md   # AWT/Swing/JavaFX/SWT 演进与定位
│   ├── 07-Swing核心组件实战.md       # JFrame/JPanel/布局/事件/多线程
│   ├── 08-JavaFX现代桌面应用.md      # FXML/SceneBuilder/CSS/数据绑定
│   └── 09-GUI事件模型与MVC架构.md    # 事件分发/EDT线程/MVC/MVP模式
│
├── ⚖️ 对比与实战（10）
│   └── 10-CLI与GUI选型与工程化.md     # 场景对比/安装包/跨平台/自动化测试
│
└── 📌 00-CLI与GUI知识体系总览.md       # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景导航 | — |
| 01 | CLI概述与设计哲学 | CLI本质/管道/Unix哲学/标志性CLI工具 | ⭐⭐ |
| 02 | Linux常用CLI工具链 | grep/awk/sed/find/xargs/jq组合实战 | ⭐⭐⭐ |
| 03 | Java CLI框架-Picocli | Picocli注解/子命令/参数校验/打包 | ⭐⭐ |
| 04 | 构建交互式终端应用 | 进度条/彩色输出/表格/选择器/Spinner | ⭐⭐ |
| 05 | 终端美化与效率工具 | Zsh/OhMyZsh/tmux/fzf/zoxide/alias | ⭐ |
| 06 | GUI概述与Java桌面生态 | AWT→Swing→JavaFX→SWT/选型对比 | ⭐⭐ |
| 07 | Swing核心组件实战 | JFrame/JPanel/布局管理器/事件处理 | ⭐⭐ |
| 08 | JavaFX现代桌面应用 | FXML/SceneBuilder/CSS/Property绑定 | ⭐⭐ |
| 09 | GUI事件模型与MVC架构 | EDT线程/SwingWorker/MVC模式 | ⭐⭐⭐ |
| 10 | CLI与GUI选型与工程化 | 场景选型/打包exe/jpackage/自动测试 | ⭐⭐ |
| 11 | TUI终端用户界面：概念与终端原理 | TUI定位/终端协议/渲染模型/事件循环/七陷阱 | ⭐⭐ |
| 12 | TUI框架生态与工程实践 | 四大框架/TamboUI/JLine/选型决策/工程纪律 | ⭐⭐⭐ |

---

## 3. 三界面形态定位

| 形态 | 一句话 | 典型 | 本体系位置 |
|------|------|------|------|
| CLI | 命令 → 结果，一次性 | grep、git | 01-05 |
| TUI | 终端里的交互应用，全屏 + 事件循环 | vim、htop、lazygit、Claude Code | 11-12 |
| GUI | 窗口 + 鼠标的图形界面 | 桌面应用 | 06-10 |

> 💡 学习顺序建议：CLI（01-05）→ TUI（11-12）→ GUI（06-10）。TUI 放在 CLI 与 GUI 之间学，能自然理解「交互状态」从无到有的演进；Java 后端重点看 11 篇的终端原理与 12 篇的 JLine/TamboUI 实战。

---

> 🎯 **CLI 是后端的日常界面、TUI 是终端里的应用开发、GUI 是理解交互设计的功底** — 能写命令行工具的后端更有"工具思维"，2026 年 AI 终端工具（Claude Code/Copilot CLI）全部基于 TUI，理解 TUI 就是理解下一波开发者工具的基础设施。
