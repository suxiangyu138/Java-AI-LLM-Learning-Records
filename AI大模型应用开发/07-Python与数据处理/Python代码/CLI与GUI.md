# CLI 与 GUI 基础概念

> **核心摘要**：CLI（命令行界面）和 GUI（图形用户界面）是两种与计算机交互的方式。CLI 高效精准、可自动化，适合专业人员；GUI 直观易学，适合日常使用。

---

## 一、CLI vs GUI 对比

| 维度 | CLI（命令行） | GUI（图形界面） |
|------|-------------|---------------|
| 交互方式 | 文字命令（敲键盘） | 窗口、图标、按钮（鼠标点击） |
| 示例 | Windows cmd、PowerShell、Linux Terminal | Windows 桌面、macOS、手机 App |
| 特点 | 高效、可脚本自动化、省资源 | 直观易学、操作简单 |
| 缺点 | 需要记命令 | 复杂/批量操作慢、难自动化 |

## 二、同一件事两种做法

**目标**：把桌面上的 `a.txt` 移到 `Documents` 文件夹

**GUI 做法**：
1. 打开文件资源管理器
2. 进桌面 → 选中 `a.txt`
3. 鼠标拖到 Documents 文件夹

**CLI 做法（一行命令）**：
```bash
mv ~/Desktop/a.txt ~/Documents/
```

## 三、CLI 的主要应用场景

- 服务器 / 云主机管理（Linux 基本全用 CLI）
- 开发 / 编译 / 部署（npm、git、docker、mvn 等）
- 批量处理 / 自动化脚本（备份、日志分析、爬虫）
- 资源受限设备（路由器、嵌入式系统）

## 核心要点回顾

- CLI 是"敲命令操作电脑"的黑框界面，用于高效、精准、自动化地干活
- GUI 是"点图标"的图形界面，直观易学但难以自动化
- CLU + GUI 互补使用：日常操作用 GUI，批量/复杂任务用 CLI

## 参考资料

1. Windows PowerShell 文档 - Microsoft Learn
2. Linux 命令行大全 - Linux Command Library
