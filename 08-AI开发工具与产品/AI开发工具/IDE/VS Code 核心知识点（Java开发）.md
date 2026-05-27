# VS Code 核心知识点（Java 开发）

## 一、概述

Visual Studio Code（VS Code）是微软推出的开源轻量级编辑器，通过插件生态可扩展为功能完整的 Java IDE。相比 IntelliJ IDEA，其优势在于 **启动快、内存占用低、多语言支持统一体验**。

**核心定位：** 轻量级/多语言项目首选，**免费**且跨平台。

## 二、Java 开发环境搭建

### 2.1 必装扩展包

| 扩展 | 作用 |
|------|------|
| **Extension Pack for Java** | 一键安装 6 个核心 Java 扩展的集合包 |
| **Spring Boot Extension Pack** | Spring Boot 开发支持（含 Initializr、Dashboard） |
| **Lombok Annotations Support** | Lombok 注解识别 |
| **SonarLint** | 实时代码质量检查 |
| **GitHub Copilot / 通义灵码** | AI 编程助手 |

### 2.2 Extension Pack for Java 包含的核心组件

```
Extension Pack for Java
  ├── Language Support for Java (Red Hat) —— 语法解析、智能补全
  ├── Debugger for Java —— 调试支持
  ├── Test Runner for Java —— 运行 JUnit/TestNG
  ├── Maven for Java —— Maven 项目管理
  ├── Gradle for Java —— Gradle 项目管理
  └── Project Manager for Java —— 多项目切换
```

### 2.3 JDK 配置

在 `settings.json` 中指定 JDK 路径：

```json
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-17",
      "path": "C:\\Program Files\\Java\\jdk-17"
    }
  ],
  "java.jdt.ls.java.home": "C:\\Program Files\\Java\\jdk-17"
}
```

## 三、核心功能与快捷键

### 3.1 命令面板

| 操作 | 快捷键 |
|------|--------|
| 命令面板 | `Ctrl + Shift + P` |
| 快速打开文件 | `Ctrl + P` |
| 全局搜索 | `Ctrl + Shift + F` |
| 文件内搜索 | `Ctrl + F` |
| 跳转到定义 | `F12` |
| 查找所有引用 | `Shift + F12` |
| 重命名符号 | `F2` |
| 代码格式化 | `Shift + Alt + F` |
| 优化导入 | `Shift + Alt + O` |

### 3.2 Java 专属功能

| 功能 | 操作方式 |
|------|----------|
| **Spring Boot Dashboard** | 侧边栏一键启动/停止/调试 Spring Boot 应用 |
| **Maven/Gradle 面板** | 侧边栏可视化执行 Goal/Task |
| **智能补全** | 输入触发，支持类型推断链式补全 |
| **生成代码** | 右键 → Source Action → 生成 Getter/Setter/Constructor |
| **JUnit 测试** | 代码行左侧 ▶ 按钮直接运行/调试单个测试 |

### 3.3 调试配置

`.vscode/launch.json` 调试配置示例：

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Launch Spring Boot",
      "request": "launch",
      "mainClass": "com.example.DemoApplication",
      "projectName": "my-spring-app"
    }
  ]
}
```

## 四、AI 编程助手集成

### 4.1 GitHub Copilot

```json
// settings.json 推荐配置
{
  "github.copilot.enable": {
    "*": true,
    "markdown": false
  },
  "github.copilot.chat.locale": "zh-CN"
}
```

### 4.2 通义灵码

- 阿里出品，中文场景理解更优
- 支持代码生成、解释、注释翻译、单元测试生成
- 完全免费，国内访问低延迟

### 4.3 CodeGeeX

- 智谱 AI 出品，支持超百种语言
- 完全免费，支持代码翻译和注释生成

## 五、实用设置与技巧

### 5.1 推荐 settings.json

```json
{
  "files.autoSave": "onFocusChange",
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.organizeImports": "explicit"
  },
  "java.compile.nullAnalysis.mode": "automatic",
  "java.configuration.updateBuildConfiguration": "automatic",
  "terminal.integrated.defaultProfile.windows": "PowerShell"
}
```

### 5.2 多项目工作区

- `.code-workspace` 文件可同时管理多个 Java 项目
- 每个文件夹独立配置，互不干扰

### 5.3 Remote Development

| 扩展 | 场景 |
|------|------|
| **Remote - SSH** | 远程 Linux 服务器开发 |
| **Remote - Containers** | 在 Docker 容器内开发 |
| **Remote - WSL** | 在 WSL 子系统中开发 |

## 六、VS Code vs IntelliJ IDEA

| 维度 | VS Code | IntelliJ IDEA |
|------|---------|---------------|
| 启动速度 | 快 | 较慢 |
| 内存占用 | 低 | 高 |
| Java 重构能力 | 中等 | 强大 |
| 多语言支持 | 优秀 | 一般 |
| 企业级框架支持 | 需插件补强 | 原生深度支持 |
| 价格 | 免费 | Ultimate 付费 |
| 适合场景 | 轻量开发/多语言/前端 | Spring/企业级后端 |

## 七、总结

- VS Code + Extension Pack for Java 已可胜任日常 Java 开发
- **轻量级项目、多语言切换、学习阶段**推荐 VS Code
- 复杂 Spring 重构、企业级微服务项目建议 IntelliJ IDEA
- 搭配 Copilot/通义灵码，AI 辅助能力不输重量级 IDE
