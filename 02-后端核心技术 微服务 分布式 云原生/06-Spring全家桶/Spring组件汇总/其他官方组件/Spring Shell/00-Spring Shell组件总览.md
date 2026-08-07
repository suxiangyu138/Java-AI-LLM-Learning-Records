# 00 Spring Shell 组件总览

> 组件卡片：Spring Shell 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档；Spring Shell 是 Spring 生态的交互式命令行（REPL）框架，暂无独立深度体系，深挖见官方参考文档

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Spring Shell 是 Spring 生态的交互式命令行（REPL/CLI）框架**——基于 JLine 提供命令注册、参数解析、自动补全、历史记录，把 Spring 应用变成"可交互的终端工具"，也是 AI Agent 型 CLI 工具的首选基座。

```text
核心心智模型：
  命令（@Command 注解方法 / CommandRegistration DSL）
    ├── 参数：@Option 具名参数 / 位置参数 / 多值（arity）
    ├── 分组：@ShellCommandGroup（类级/方法级分组）
    └── 执行：方法体 = 命令逻辑
  三种运行模式（ShellRunner）
    ├── InteractiveShellRunner   交互 REPL（JLine 终端）
    ├── NonInteractiveShellRunner 单条命令（脚本/CI：-c "cmd"）
    └── ScriptShellRunner        脚本文件批量执行
  JLine：补全 / 历史 / 终端控制（3.30.x）
```

> 🎯 **一句话**：Spring Shell = "把 Spring 容器能力暴露给终端"——命令方法里能用一切 Bean（服务/仓库/AI 组件），是运维工具、开发脚手架、AI CLI 的标准姿势。

## 2. 版本现状（2026-08）

| 版本线 | 最新 | 发布时间 | 配套 | 状态 |
|--------|------|---------|------|------|
| **4.0.x** | 4.0.3 | 2026-06-11 | Spring Boot 4.0.7 / SF 7.0.8 / JLine 3.30.13 | **当前主线** |
| 3.4.x | 3.4.3 | 2026-06-11 | Boot 3.x | 维护线（迁移 4.0 的跳板） |

4.0 是多年来的**首个大版本重构**（2025-12 底 ~ 2026-01 发布）：

- **Boot 下零注解开关**：`@EnableCommand` / `@CommandScan` 不再需要（自动注册）；`@Command` 行为修正；
- **命令 DSL 重建**：`CommandRegistration.Builder` 新构建器（对齐 Spring Security 的 FilterChain 式 builder 风格）；
- **模块化架构**：`spring-shell-core` 不再依赖 Spring Boot 与 JLine；`standard` / `standard-commands` / `table` 模块并入 core；`shell-component` 相关旧注解（`@ShellComponent`、`@ShellMethod`、`@ShellOption`）**全部移除**；
- **JSpecify 空安全**：API 全面标注可空性；
- **版本对齐**：SF 7.0 / Boot 4.0；官方要求**先升到最新 3.4.x 再迁 4.0**（迁移指南见项目 Wiki）。

4.0.2（2026-04）新增：类级命令分组、测试"需要输入的命令"、多值参数 arity、help 命令排序、非交互模式退出码修复（不再恒为 0）、子命令布尔标志、Spring profile 生效、GraalVM 原生命令注册。4.0.3（2026-06）修复：JDK 25 下 tab 补全（JLine 3.30.9+）、自定义 Converter 配 @Command、原生编译 stg 资源 runtime hints 等。

> 💡 面试/写代码要点：**4.0 语法 = @Command + @Option + CommandRegistration DSL**；看到 `@ShellComponent`/`@ShellMethod` 的博客都是 3.x 旧文。

## 3. 能力地图

| 能力域 | 能力 | 代表组件 |
|--------|------|---------|
| 命令定义 | 注解式 / DSL 式双模型 | @Command、@Option、CommandRegistration.Builder |
| 交互体验 | 补全 / 历史 / 分组 / 帮助 | JLine Completion、CommandCatalog、help 命令 |
| 参数 | 具名/位置/多值/转换/必填 | @Option、Converter、arity |
| 运行模式 | 交互 / 单命令 / 脚本 | Interactive/NonInteractive/Script ShellRunner |
| 退出码 | 非交互模式退出状态 | ExitCodeExceptionProvider、CommandRegistration exitCode |
| 测试 | 命令级测试 | spring-shell-test、@ShellTest、ShellAssertions |
| 集成 | Spring 容器全量能力 | 任意 Bean 注入命令方法 |
| 原生 | GraalVM Native Image | runtime hints、命令自动注册（4.0.2+） |
| 扩展 | 自定义终端/补全/参数解析 | Terminal 定制、CompletionProposal |

## 4. 与深度体系的映射

| 本卡片模块 | 深度体系对应 | 衔接说明 |
|-----------|-------------|---------|
| 02 核心类 | [Spring Task 知识体系-06 虚拟线程与 Spring Boot 4](../../../Spring Task/06-虚拟线程与Spring Boot 4的调度变革.md) | Shell 命令方法天然跑在 Spring 容器内，可调用 @Async/调度能力 |
| 04 集成地图 | [Spring Batch 深度体系-08 调度、运维与可观测性](../../../Spring Batch/08-调度、运维与可观测性.md) | Shell 是触发批处理（JobOperator）的轻量运维终端 |
| 04 集成地图 | [DeepSeek 知识体系](../../../../../03-AI大模型应用开发/10-AI开发工具/主流大模型API调用/DeepSeek/00-DeepSeek知识体系总览.md) | AI CLI 工具：Shell 命令方法内调 LLM/Agent |

> 💡 Spring Shell 生态位：**它是"操作层"组件**——把后端能力暴露为终端命令，常与 Batch（运维触发）、Spring AI（智能助手 CLI）、Spring Security（命令鉴权）组合；本仓库暂无独立深度体系，深入以 [官方参考文档](https://docs.spring.io/spring-shell/reference/) 为准。

## 5. 快速上手 3 步

```text
① 引入 spring-boot-starter-shell（Boot 4 管理版本，无需 @EnableCommand）
② 用 @Command + @Option 声明命令类（或 CommandRegistration DSL）
③ 运行应用：交互模式直接敲命令；脚本/CI 用 -c 或脚本文件
```

```java
// 最小命令：hello 命令（4.0 语法）
@Component
class HelloCommand {
    @Command(command = "hello", description = "say hello")
    String hello(@Option(defaultValue = "world") String name) {
        return "hello " + name;
    }
}
// 运行：java -jar app.jar → hello → hello world（可 tab 补全参数）
```

> 💡 三种模式即得：交互（默认）、`-c "hello"` 单命令（返回退出码，CI 友好）、`--script file.shell` 脚本批量。

## 6. 速查导航

| 卡片 | 内容 | 何时翻 |
|------|------|--------|
| [01-模块清单](01-模块清单.md) | artifact 与模块边界（4.0 合并后更简） | 加依赖、理解 4.0 结构 |
| [02-核心类与注解速查](02-核心类与注解速查.md) | 命令/参数/运行器/测试 API | 写命令时随手查 |
| [03-配置属性速查](03-配置属性速查.md) | spring.shell.* 属性字典 | 写 application.yml 时 |
| [04-集成地图与常见问题](04-集成地图与常见问题.md) | 生态集成 + 高频坑 + 3.x→4.0 迁移 | 集成/迁移/排障 |

## 7. 学习路线推荐

```text
快速上手：00 → 01 → 05（三步）→ 02 核心类
生产 CLI：00 → 02（运行器/退出码）→ 03 配置 → 04（测试/原生）
AI CLI / 运维工具：00 → 04 集成地图 → 结合 Spring AI 与 Batch 深度体系
```

## 8. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| @Command | 4.0 唯一命令注解（方法/类级，可分组） |
| @Option | 命令参数（具名/默认值/必填/多值） |
| CommandRegistration.Builder | DSL 式命令定义（复杂命令） |
| ShellRunner | 三种运行模式（交互/单命令/脚本） |
| CommandCatalog | 命令注册表（命令名 → 注册信息） |
| Converter | 参数类型转换（自定义类型解析） |
| ExitCodeExceptionProvider | 非交互模式退出码控制 |
| spring-shell-test | 命令测试工具（@ShellTest + ShellAssertions） |

---

> 🎯 **核心要点**：Spring Shell 4.0 = "零注解开关 + DSL 重建 + 模块合并 + JSpecify"的现代化重写；三件事记住——**语法看 4.0（@Command/@Option）、CI 看退出码、交互体验靠 JLine**；AI CLI 工具是 2026 年最热的使用场景。

**下一模块**：[01-模块清单](01-模块清单.md)
