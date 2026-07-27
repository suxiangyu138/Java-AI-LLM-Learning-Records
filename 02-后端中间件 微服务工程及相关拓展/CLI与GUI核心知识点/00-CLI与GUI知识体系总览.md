# 00 - CLI 与 GUI 知识体系总览

> 🎯 CLI 是后端日常操作界面、GUI 是理解交互设计的基础 — Java 后端不仅要会写 API，还要能写命令行工具、理解图形界面原理

---

## 1. 知识全景

```
CLI与GUI（11个文件）
│
├── 🖥️ CLI 命令行（01-05）
│   ├── 01-CLI概述与设计哲学.md      # CLI vs GUI 本质/管道哲学/Unix设计原则
│   ├── 02-Linux常用CLI工具链.md     # grep/awk/sed/find/xargs/jq 组合技
│   ├── 03-Java-CLI框架-Picocli.md   # Picocli/JCommander/Spring Shell
│   ├── 04-构建交互式终端应用.md      # 进度条/彩色输出/表格/交互式选择
│   └── 05-终端美化与效率工具.md      # Zsh/Oh My Zsh/tmux/fzf/alias
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

---

> 🎯 **CLI 是后端的日常界面、GUI 是理解交互设计的功底** — 能写命令行工具的后端更有"工具思维"。
