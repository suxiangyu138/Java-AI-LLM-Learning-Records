# 01 - Chrome DevTools 快捷键

> **核心摘要**：DevTools 是前端调试的主战场——F12 打开、Ctrl+Shift+P 命令菜单、Ctrl+[ / ] 面板切换。本文覆盖 DevTools 的打开方式、面板导航、命令菜单（上帝模式）与全局快捷键。

> **前置阅读**：[[00-Web开发常用快捷键知识体系总览]]

---

## 📚 目录

1. [DevTools 的打开方式](#1-devtools-的打开方式)
2. [面板体系与导航](#2-面板体系与导航)
3. [命令菜单（上帝模式）](#3-命令菜单上帝模式)
4. [全局快捷键](#4-全局快捷键)
5. [面板专属快捷键预告](#5-面板专属快捷键预告)
6. [自定义快捷键](#6-自定义快捷键)
7. [核心要点](#7-核心要点)

---

## 1. DevTools 的打开方式

> **背景**：DevTools（开发者工具）是 Chrome/Edge 内置的调试套件——前端开发的第一工具。
> **目的**：掌握多种打开方式，任何场景秒开。
> **适用范围**：Chrome/Edge（内核相同，快捷键一致）。
> **不适用场景**：Firefox（快捷键大部分相同，少量差异）。

```text
DevTools 的四种打开方式
├── ① F12（最常用）——开/关切换
├── ② Ctrl+Shift+I（等价 F12）
├── ③ 右键 → 检查（鼠标场景）
├── ④ Ctrl+Shift+J ——直接打开 Console 面板！
└── macOS：Cmd+Option+I / Cmd+Option+J

常见困惑
├── ⚠️ F12 被系统占用（笔记本 Fn 键）→ 按 Fn+F12 或 Ctrl+Shift+I
├── ⚠️ 打开的是 Edge DevTools（默认浏览器）→ 与 Chrome 相同
└── ✅ DevTools 打开后：F12 关闭/Ctrl+Shift+I 重新打开同一窗口
```

---

## 2. 面板体系与导航

### 2.1 DevTools 面板总览

| 面板 | 功能 | 前端场景 |
|------|------|---------|
| **Elements** | DOM 与样式 | 改样式/看结构 |
| **Console** | 控制台 | 看报错/调试输出 |
| **Sources** | 源码与断点 | 断点调试核心 |
| **Network** | 网络请求 | 接口排查 |
| **Performance** | 性能分析 | 性能优化 |
| **Application** | 存储/Cookie | 本地存储排查 |
| **Memory** | 内存快照 | 内存泄漏 |

### 2.2 面板切换快捷键

```text
面板切换（高效核心）
├── Ctrl+[ / Ctrl+]：左右循环切换面板
├── Ctrl+Shift+P：命令菜单跳转（输入面板名）
├── Esc：打开/关闭底部抽屉（Drawer——Console 常驻）
└── 悬停图标：面板图标在顶部工具栏

开发者习惯
├── 排样式 → Elements
├── 查报错 → Ctrl+Shift+J（Console 直达）
├── 断点调试 → Sources
├── 接口排查 → Network
└── 金句：Ctrl+[ / ] 是面板间切换的「高频键」
```

---

## 3. 命令菜单（上帝模式）

### 3.1 什么是命令菜单

> 🎯 **Ctrl+Shift+P 命令菜单**——DevTools 的「上帝模式」：输入关键词执行任何菜单功能。**记不住菜单在哪？直接命令菜单搜**。

### 3.2 高频命令（2026 推荐）

```text
命令菜单高频命令（Ctrl+Shift+P 后输入）
├── Capture node screenshot   截取节点图（元素截图）
├── Show Coverage            代码覆盖率（哪些代码被执行）
├── Disable JavaScript       禁用 JS（测试无 JS 页面）
├── Show Network conditions  网络条件（模拟离线/慢速）
├── Show rendering           渲染设置（布局/滚动检查）
├── Dock to right/bottom     面板停靠位置切换
├── 切换设备模拟             设备模式（或 Ctrl+Shift+M）
├── Search all files         全局搜索（等价 Ctrl+Shift+F）
└── Reset zoom               重置缩放（误缩后）

用法示例（截取某个元素的图）
① Ctrl+Shift+C 选中元素
② Ctrl+Shift+P → 输入 "node screenshot" → Enter
③ 元素 PNG 自动下载！
```

> 🎯 **命令菜单的使用哲学**：**记不住的位置用命令菜单搜**——比翻菜单快 10 倍。常用命令自然记住，冷门命令搜索直达。

---

## 4. 全局快捷键

### 4.1 全局快捷键表

| 快捷键 | 功能 |
|:---:|------|
| `F12` / `Ctrl+Shift+I` | 打开/关闭 DevTools |
| `Ctrl+Shift+J` | 打开 Console |
| `Ctrl+Shift+C` | 元素选择器（检查模式） |
| `Ctrl+Shift+M` | 设备模拟模式（移动端） |
| `Ctrl+Shift+P` | 命令菜单 |
| `Ctrl+[` / `Ctrl+]` | 面板切换 |
| `Esc` | 底部抽屉（Console）开关 |
| `Ctrl+R` | 刷新页面 |
| `Ctrl+Shift+R` | **强制刷新**（绕过缓存） |
| `Ctrl+F` | 当前面板内搜索 |
| `Ctrl+Shift+F` | 所有文件全局搜索 |

### 4.2 高频组合场景

```text
组合场景 1：页面元素排查
├── F12 → Ctrl+Shift+C 点击元素 → Elements 定位

组合场景 2：JS 报错排查
├── Ctrl+Shift+J → 看 Console 报错 → 点击报错定位源码

组合场景 3：缓存问题
├── Ctrl+Shift+R 强制刷新（改代码不生效先试这个！）

组合场景 4：移动端问题
├── Ctrl+Shift+M 设备模拟 → 切换设备尺寸

组合场景 5：性能问题
├── Ctrl+Shift+P → Performance → 录制分析
```

---

## 5. 面板专属快捷键预告

### 5.1 各面板核心快捷键

| 面板 | 核心快捷键 | 详见 |
|------|-----------|------|
| Elements | ↑↓ 调整样式值 / Enter 编辑 | 02 篇 |
| Sources | F8/F10/F11 断点控制 / Ctrl+P 文件 | 03 篇 |
| Console | ↑↓ 历史 / Shift+Enter 多行 | 04 篇 |
| Network | Ctrl+E 录制 / / 过滤 | 04 篇 |

> 💡 **学习策略**：先掌握本系列 01 篇（打开与导航），再按需深挖面板专属快捷键（02-04 篇）——**每个面板的核心快捷键只有 5-8 个，不必全背**。

---

## 6. 自定义快捷键

```text
DevTools 自定义快捷键（2026）
├── ① Settings（齿轮）→ Shortcuts
├── ② 搜索操作 → 点击快捷键 → 录制新键
├── ③ 支持命令菜单中所有操作的自定义
├── ④ 快捷键冲突提示（与浏览器/系统冲突）
└── ⑤ 重置：Shortcuts 面板 Restore defaults

使用场景
├── 高频面板自定义直达键
├── 命令菜单常用命令绑定快捷键
└── macOS/Windows 键位差异调整
```

---

## 7. 核心要点

> 🎯 **核心要点**：
> 1. 打开三键：F12（切换）/Ctrl+Shift+I（等价）/Ctrl+Shift+J（Console 直达）
> 2. 面板切换：Ctrl+[ / ] 左右循环 + Esc 抽屉开关
> 3. **Ctrl+Shift+P 命令菜单是「上帝模式」**——记不住的位置搜索直达（截元素图/覆盖率/禁用 JS）
> 4. 全局三键：Ctrl+Shift+C 选元素 / Ctrl+Shift+M 设备模拟 / Ctrl+Shift+R 强制刷新
> 5. 自定义：Settings → Shortcuts 绑定高频操作
> 6. 学习策略：先掌握打开与导航，面板专属键按需深挖（每面板 5-8 个）

---

**下一模块**：[02-Elements与样式调试](02-Elements与样式调试.md) | **返回总览**：[00-Web开发常用快捷键知识体系总览](00-Web开发常用快捷键知识体系总览.md)
