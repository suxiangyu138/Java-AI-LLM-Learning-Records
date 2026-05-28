# Swing 界面设计 (Swing GUI Design)

> Java Swing 学生信息管理系统，涵盖核心 GUI 组件、布局管理、事件处理

## 项目概述

基于 Java Swing 构建的学生信息管理桌面应用。综合练习 Swing 核心组件（JTextField、JComboBox、JTable、JMenuBar），实现学生信息的增删改查功能。包含表单输入、表格展示、菜单栏、对话框、窗口关闭确认等常见 GUI 交互模式。

## 技术栈

| 技术 | 说明 |
|------|------|
| Java | JDK 8+ |
| Swing | JFrame, JPanel, JMenuBar, JTable, JOptionPane |
| AWT | LayoutManager（BorderLayout, GridLayout, FlowLayout） |
| DefaultTableModel | 表格数据模型管理 |

## 功能特性

- **菜单栏**：文件菜单 → 退出（带快捷键提示）
- **表单输入**：姓名（JTextField）、年龄（JTextField）、性别（JComboBox）
- **数据表格**：JTable + DefaultTableModel 展示学生列表
- **增删改查**：添加学生、选中删除、清空表单
- **退出确认**：JOptionPane 确认对话框，防止误关闭
- **多布局组合**：BorderLayout + GridLayout + FlowLayout 嵌套

## 项目结构

```
Swing界面/
├── src/
│   ├── Main.java                # 程序入口
│   └── SwingGuiDesign.java      # 主界面类（GUI 构建 + 事件处理）
├── out/                         # 编译输出目录
├── SwingGuiDesign.iml           # IntelliJ IDEA 模块文件
└── README.md
```

## 快速开始

```bash
# 编译
javac -encoding UTF-8 -d out src/Main.java src/SwingGuiDesign.java

# 运行
java -cp out Main
```

## 核心知识点

| 知识点 | 应用 |
|--------|------|
| JFrame | 窗口创建、大小、居中、关闭策略 |
| JMenuBar / JMenu / JMenuItem | 菜单系统构建 |
| JTextField / JComboBox | 表单输入组件 |
| JTable + DefaultTableModel | 表格数据绑定与动态更新 |
| JOptionPane | 确认对话框、消息弹窗 |
| WindowListener | 窗口关闭事件拦截 |
| LayoutManager | BorderLayout / GridLayout / FlowLayout 布局 |
| ActionListener | 按钮点击事件处理 |

## 注意事项

- `setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)` 配合 WindowListener 实现自定义关闭逻辑
- DefaultTableModel 的 addRow / removeRow 是操作表格数据的核心方法
- Swing 所有 UI 操作应在 Event Dispatch Thread (EDT) 中执行
