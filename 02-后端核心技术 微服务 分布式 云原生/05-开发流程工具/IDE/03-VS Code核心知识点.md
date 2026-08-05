# VS Code 核心知识点（Java 开发）

> **核心摘要**：Visual Studio Code 是微软推出的开源轻量级编辑器，通过插件生态可扩展为功能完整的 Java IDE。相比 IntelliJ IDEA，优势在于启动快、内存占用低、多语言支持统一。

---

## 一、概述

**核心定位**：轻量级/多语言项目首选，**免费**且跨平台。

## 二、Java 开发环境搭建

### 2.1 必装扩展包

| 扩展 | 作用 |
|------|------|
| Extension Pack for Java | 一键安装 6 个核心 Java 扩展 |
| Spring Boot Extension Pack | Spring Boot 开发支持 |
| Lombok Annotations Support | Lombok 注解识别 |
| SonarLint | 实时代码质量检查 |
| GitHub Copilot / 通义灵码 | AI 编程助手 |

### 2.2 JDK 配置

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

| 操作 | 快捷键 |
|------|--------|
| 命令面板 | `Ctrl + Shift + P` |
| 快速打开文件 | `Ctrl + P` |
| 全局搜索 | `Ctrl + Shift + F` |
| 跳转到定义 | `F12` |
| 查找所有引用 | `Shift + F12` |
| 重命名符号 | `F2` |
| 代码格式化 | `Shift + Alt + F` |

## 四、AI 编程助手集成

### GitHub Copilot 配置

```json
{
  "github.copilot.enable": { "*": true, "markdown": false },
  "github.copilot.chat.locale": "zh-CN"
}
```

### 其他 AI 插件

- **通义灵码**：阿里出品，中文场景理解更优，完全免费
- **CodeGeeX**：智谱 AI 出品，支持超百种语言，完全免费

## 五、推荐 settings.json

```json
{
  "files.autoSave": "onFocusChange",
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": { "source.organizeImports": "explicit" },
  "java.compile.nullAnalysis.mode": "automatic",
  "java.configuration.updateBuildConfiguration": "automatic"
}
```

## 六、VS Code vs IntelliJ IDEA

| 维度 | VS Code | IntelliJ IDEA |
|------|---------|---------------|
| 启动速度 | 快 | 较慢 |
| 内存占用 | 低 | 高 |
| Java 重构能力 | 中等 | 强大 |
| 多语言支持 | 优秀 | 一般 |
| 企业级框架 | 需插件补强 | 原生深度支持 |
| 价格 | 免费 | Ultimate 付费 |

## 核心要点回顾

- VS Code + Extension Pack for Java 已可胜任日常 Java 开发
- 轻量级项目、多语言切换、学习阶段推荐 VS Code
- 复杂 Spring 重构、企业级微服务项目建议 IntelliJ IDEA
- 搭配 Copilot/通义灵码，AI 辅助能力不输重量级 IDE

## 参考资料

1. VS Code 官方文档 - code.visualstudio.com/docs
2. Extension Pack for Java - VS Code Marketplace
3. Spring Boot Extension Pack - VS Code Marketplace
