# 数字时钟 (Digital Clock)

> Java Swing 高级质感桌面电子钟，渐变背景 + 霓虹字体 + 圆角边框

## 项目概述

基于 Java Swing 开发的桌面电子时钟程序。采用自定义窗口样式（隐藏原生标题栏），实现渐变背景、霓虹光效字体、圆角边框和阴影效果。支持时间（HH:mm:ss）和日期（yyyy-MM-dd 星期）实时显示，通过 Timer 每秒刷新。

## 技术栈

| 技术 | 说明 |
|------|------|
| Java | JDK 8+ |
| Swing | GUI 框架（JFrame, JLabel, Timer） |
| Java 2D | 渐变绘制、抗锯齿、圆角裁剪 |
| AWT | Graphics2D, GradientPaint, RoundRectangle2D |

## 功能特性

- **渐变背景**：深蓝 → 深紫线性渐变
- **霓虹字体**：大号白色文字 + 浅蓝外发光边框
- **圆角窗口**：RoundRectangle2D 自定义窗口形状
- **无边框设计**：setUndecorated(true) 隐藏原生标题栏
- **实时刷新**：javax.swing.Timer 每秒更新时间和日期
- **抗锯齿渲染**：文字和图形边缘平滑处理

## 项目结构

```
数字时钟/
├── src/
│   └── DigitalClock.java    # 主程序（电子钟核心逻辑）
├── out/                     # 编译输出目录
├── DigitalClock.iml         # IntelliJ IDEA 模块文件
└── README.md
```

## 快速开始

```bash
# 编译
javac -encoding UTF-8 -d out src/DigitalClock.java

# 运行
java -cp out DigitalClock
```

## 核心知识点

| 知识点 | 应用 |
|--------|------|
| Swing JFrame | 窗口创建、自定义样式、事件处理 |
| Graphics2D | 渐变绘制、抗锯齿、自定义形状 |
| GradientPaint | 线性渐变背景实现 |
| RoundRectangle2D | 窗口圆角裁剪 |
| javax.swing.Timer | 定时刷新 UI |
| SimpleDateFormat | 时间/日期格式化 |
| SwingUtilities.invokeLater | EDT 线程安全启动 |
| 反射调用 | AWTUtilities 兼容 Windows 窗口阴影 |

## 注意事项

- 使用 `setUndecorated(true)` 后窗口无法拖拽移动，需自行实现拖拽逻辑
- 反射调用 `AWTUtilities` 在不同 JDK 版本中可能失效，需要做兼容处理
- Timer 的间隔建议 >= 500ms，避免过度刷新消耗 CPU
