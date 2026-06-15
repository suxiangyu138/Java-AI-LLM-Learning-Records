# IntelliJ IDEA 核心知识点

> **核心摘要**：IntelliJ IDEA 是 Java 生态最强大的 IDE，企业级 Java 后端开发首选。本文涵盖核心特性、高效操作、插件推荐、AI 增强工作流等全方位指南。

---

## 一、概述

IntelliJ IDEA 分为 **Ultimate（旗舰版，付费）** 和 **Community（社区版，免费）** 两个版本。Ultimate 版提供 Spring、Java EE、数据库工具等企业级功能支持。

**核心定位**：Java 生态最强大的 IDE，企业级 Java 后端开发首选。

## 二、核心特性

### 2.1 智能代码辅助

| 特性 | 说明 |
|------|------|
| Smart Completion | 基于上下文类型推断的智能补全 |
| Chain Completion | 多步链式补全，自动推断中间变量类型 |
| Dataflow Analysis | 数据流分析，实时检测空指针等问题 |
| Intention Actions | 上下文感知的快速修复建议（灯泡提示） |
| Refactoring | 跨文件安全重构 |

### 2.2 框架与生态集成

- **Spring Boot / Spring Cloud**：原生支持，含 Bean 依赖图、配置提示
- **Maven / Gradle**：内置依赖管理
- **JPA / Hibernate**：JPQL 语法高亮、实体关系图
- **Docker / Kubernetes**：容器编排支持

### 2.3 AI 增强（2026 年）

- **JetBrains AI Assistant**：内置 AI，支持代码解释、生成提交信息
- **GitHub Copilot 插件**：深度集成
- **通义灵码插件**：中文优化

## 三、高效操作

### 3.1 快捷键（Windows/Linux）

| 操作 | 快捷键 |
|------|--------|
| 全局搜索 | `Double Shift` |
| 查找文件 | `Ctrl + Shift + N` |
| 格式化代码 | `Ctrl + Alt + L` |
| 优化导入 | `Ctrl + Alt + O` |
| 重构入口 | `Ctrl + Alt + Shift + T` |
| 跳转到定义 | `Ctrl + B` |
| 查找用法 | `Alt + F7` |

### 3.2 Live Templates

| 缩写 | 展开 |
|------|------|
| `psvm` | `public static void main(String[] args)` |
| `sout` | `System.out.println()` |
| `fori` | `for (int i = 0; i < ...; i++)` |
| `iter` | `for (T item : iterable)` |

### 3.3 调试技巧

- **条件断点**：右键断点，输入条件表达式
- **Evaluate Expression**：`Alt + F8`，运行时动态执行代码
- **Stream Debugger**：可视化 Java Stream 数据变化

## 四、推荐插件

| 插件 | 用途 |
|------|------|
| Lombok | 消除样板代码 |
| MyBatisX | Mapper 与 XML 间跳转 |
| SonarLint | 实时代码质量检查 |
| Rainbow Brackets | 彩虹括号提升可读性 |
| Key Promoter X | 快捷键学习辅助 |

## 五、AI 时代下的 IDEA 工作流

```
需求理解 → Copilot/AI Assistant 生成骨架代码
    ↓
核心逻辑手写 → AI 辅助补全上下文
    ↓
SonarLint 实时检查 → AI 解释代码异味
    ↓
AI 生成 JUnit 测试 → 运行验证
    ↓
AI 优化提交信息 → Git 提交
```

## 核心要点回顾

- Ultimate 版适合企业级 Spring/微服务开发
- Community 版足够应对纯 Java SE 学习和小型项目
- 善用快捷键和 Live Templates 可显著提升编码效率
- 2026 年 AI 插件（Copilot/通义灵码/AI Assistant）已是标配

## 参考资料

1. IntelliJ IDEA 官方文档 - jetbrains.com/idea/documentation
2. JetBrains AI Assistant 文档
