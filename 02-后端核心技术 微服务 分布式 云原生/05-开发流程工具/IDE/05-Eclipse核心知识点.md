# Eclipse 核心知识点（Java 开发）

> **核心摘要**：Eclipse 是 IBM 于 2001 年开源的 Java IDE，由 Eclipse 基金会维护。在 2026 年虽不再是 Java 新项目的首选 IDE，但在传统企业应用维护领域仍有不可替代的价值。

---

## 一、概述

**核心定位**：老牌开源 IDE，适合维护传统 Java/Jakarta EE 项目。

## 二、核心特性

### 2.1 工作区与项目管理

- **Workspace**：顶级容器，管理一组项目的 IDE 状态
- **Project**：源码、类路径、编译器设置的独立单元
- **Working Set**：在大型工作区中按逻辑分组项目

### 2.2 代码辅助

| 功能 | 快捷键 |
|------|--------|
| 内容辅助 | `Ctrl + Space` |
| 快速修复 | `Ctrl + 1` |
| 跳转到定义 | `F3` |
| 查找引用 | `Ctrl + Shift + G` |
| 重构 | `Alt + Shift + T` |
| 格式化代码 | `Ctrl + Shift + F` |
| 优化导入 | `Ctrl + Shift + O` |

### 2.3 构建与依赖

- **构建路径（Build Path）**：手动管理 JAR 依赖和源码目录
- **Maven 集成（m2e）**：自动识别 pom.xml
- **Gradle（Buildship）**：Eclipse 基金会维护的 Gradle 插件

## 三、常用插件

| 插件 | 用途 |
|------|------|
| Spring Tools 4 | Spring Boot/Cloud 开发支持 |
| m2e (Maven Integration) | Maven 项目依赖解析与构建 |
| Buildship | Gradle 项目集成 |
| MyBatis Generator | MyBatis 代码自动生成 |
| SonarLint | 实时代码质量分析 |
| SpotBugs | 静态字节码缺陷检测 |

## 四、调试功能

- **条件断点**：右键断点 → Breakpoint Properties → Conditional
- **异常断点**：Run → Add Java Exception Breakpoint
- **热替换（Hot Code Replace）**：调试时修改代码，部分场景实时生效
- **远程调试**：连接远程 JVM，端口默认 5005

## 五、Eclipse vs IntelliJ IDEA

| 维度 | Eclipse | IntelliJ IDEA |
|------|---------|---------------|
| 许可证 | 完全免费开源 | Community 免费 / Ultimate 付费 |
| 智能补全 | 较弱（基于前缀匹配） | 强大（类型推断 + 数据流分析） |
| 重构能力 | 基础 | 全面且安全 |
| 内存占用 | 较低 | 较高 |
| 传统企业支持 | 原生支持 Jakarta EE/WebSphere | 需 Ultimate 版 |

## 六、AI 时代的 Eclipse

Eclipse 的 AI 能力相对较弱，可通过以下方式补充：
1. 安装 Copilot 插件：Eclipse 版 GitHub Copilot
2. 使用 VS Code 或 IDEA 辅助：核心代码在 Eclipse 维护，新功能在 IDEA/VS Code 开发

## 核心要点回顾

- Eclipse 适合维护传统 Jakarta EE / WebSphere / WebLogic 应用
- 新项目推荐 IntelliJ IDEA，轻量开发推荐 VS Code
- Eclipse 在 2026 年的核心价值在于传统企业项目维护
- 可通过 Copilot 插件补充 AI 能力

## 参考资料

1. Eclipse 官方文档 - eclipse.org/documentation
2. Eclipse 插件市场 - marketplace.eclipse.org
