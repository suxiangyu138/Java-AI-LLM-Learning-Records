# IntelliJ IDEA 核心知识点

## 一、概述

IntelliJ IDEA 是 JetBrains 公司推出的 Java/Kotlin 集成开发环境（IDE），分为 **Ultimate（旗舰版，付费）** 和 **Community（社区版，免费开源）** 两个版本。Ultimate 版提供 Spring、Java EE、数据库工具等企业级功能支持。

**核心定位：** Java 生态最强大的 IDE，企业级 Java 后端开发首选。

## 二、核心特性

### 2.1 智能代码辅助

| 特性 | 说明 |
|------|------|
| **Smart Completion** | 基于上下文类型推断的智能补全，比普通前缀匹配更精准 |
| **Chain Completion** | 多步链式补全，自动推断中间变量类型 |
| **Dataflow Analysis** | 数据流分析，实时检测空指针、类型不匹配等问题 |
| **Intention Actions** | 上下文感知的快速修复建议（灯泡提示） |
| **Refactoring** | 跨文件安全重构：重命名、提取方法、移动类、修改签名等 |

### 2.2 框架与生态集成

- **Spring Boot / Spring Cloud**：原生支持，含 Bean 依赖图、端点导航、配置提示
- **Maven / Gradle**：内置依赖管理，POM/build.gradle 可视化编辑
- **JPA / Hibernate**：JPQL 语法高亮、实体关系图
- **Docker**：Dockerfile 编辑、Compose 服务管理、容器内调试
- **Kubernetes**：k8s 资源文件智能编辑

### 2.3 AI 增强（2026年）

- **JetBrains AI Assistant**：内置 AI，支持代码解释、生成提交信息、自然语言转代码
- **GitHub Copilot 插件**：深度集成，行内补全 + 聊天面板
- **通义灵码插件**：中文优化，适合国内开发者

### 2.4 数据库工具（Ultimate）

- 内置 DataGrip 级数据库客户端
- SQL 智能补全、可视化查询构建器
- 支持 MySQL、PostgreSQL、Oracle、MongoDB 等

## 三、常用高效操作

### 3.1 快捷键（Windows/Linux）

| 操作 | 快捷键 |
|------|--------|
| 全局搜索（文件/类/符号/操作） | `Double Shift` |
| 查找文件 | `Ctrl + Shift + N` |
| 查找类 | `Ctrl + N` |
| 查找操作 | `Ctrl + Shift + A` |
| 格式化代码 | `Ctrl + Alt + L` |
| 优化导入 | `Ctrl + Alt + O` |
| 重构入口 | `Ctrl + Alt + Shift + T` |
| 最近文件 | `Ctrl + E` |
| 跳转到定义 | `Ctrl + B` |
| 查找用法 | `Alt + F7` |

### 3.2 Live Templates（代码模板）

常用缩写：
- `psvm` → `public static void main(String[] args)`
- `sout` → `System.out.println()`
- `psfs` → `public static final String`
- `fori` → `for (int i = 0; i < ...; i++)`
- `iter` → `for (T item : iterable)`

### 3.3 调试技巧

- **条件断点**：右键断点，输入条件表达式
- **Evaluate Expression**：`Alt + F8`，运行时动态执行代码片段
- **Stream Debugger**：可视化 Java Stream 各步骤的数据变化
- **Remote Debug**：连接远程 JVM 进行调试

## 四、插件推荐

| 插件 | 用途 |
|------|------|
| **Lombok** | 消除样板代码（需配合 Annotation Processor） |
| **MyBatisX** | MyBatis Mapper 与 XML 间跳转 |
| **SonarLint** | 实时代码质量检查 |
| **Rainbow Brackets** | 彩虹括号，提升嵌套代码可读性 |
| **Key Promoter X** | 快捷键学习辅助 |
| **String Manipulation** | 字符串大小写、格式批量转换 |
| **.env files support** | .env 文件语法支持 |

## 五、项目配置要点

### 5.1 SDK 与模块管理

```
File → Project Structure (Ctrl + Alt + Shift + S)
  ├── Project → 选择 JDK 版本
  ├── Modules → 模块源码、依赖、输出路径
  └── Libraries → 外部库管理
```

### 5.2 推荐设置

- **Build Tools → Maven**：设置 `Maven home path`、`User settings file`
- **Editor → Code Style → Java**：导入团队代码风格 XML
- **Appearance → Theme**：推荐 `Darcula`（护眼暗色主题）
- **Editor → Inspections**：自定义检查规则严格程度

## 六、AI 时代下的 IDEA 工作流

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

## 七、总结

- **Ultimate 版**适合企业级 Spring/微服务开发，内置数据库工具和 AI 能力
- **Community 版**足够应对纯 Java SE 学习、小型项目
- 善用快捷键和 Live Templates 能显著提升编码效率
- 2026 年 AI 插件（Copilot/通义灵码/AI Assistant）已是标配，建议至少装一个
