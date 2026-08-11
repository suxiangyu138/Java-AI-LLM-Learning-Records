# JUnit 知识体系总览

> Java 测试框架事实标准：从 JUnit 4 到 JUnit 6（2025-09 GA），Jupiter 模型 + 三平台架构 + 扩展机制的全景知识体系。基于 **JUnit 6.1.1 / JUnit 5.14.4（2026-08）** 编写。

## 📚 目录

1. [知识体系导图](#1)
2. [模块导航](#2)
3. [学习路线推荐](#3)
4. [核心概念速查](#4)

## 1. 知识体系导图

> 💡 先说结论：**2026 年新项目直接用 JUnit 6.1.1**（Java 17+）；存量 5.x 项目迁移成本极低；JUnit 4 项目必须规划迁移（Vintage 已弃用、Spring Boot 4 不再兜底）。

## 1. 知识体系导图

```text
JUnit 知识体系（10 篇）
│
├── 01-演进史与版本全景 ──── JUnit 3→4→5→6 演进 · 版本时间线 · Spring Boot 4 默认 JUnit 6
│
├── 02-三平台架构与核心 API ── Platform/Jupiter/Vintage · 注解族 · 断言/假设 · JSpecify
│
├── 03-测试生命周期与嵌套 ──── 生命周期回调 · @Nested · 执行顺序 · @TestInstance
│
├── 04-参数化测试全解析 ────── @ValueSource/@CsvSource/@MethodSource · FastCSV · 聚合器
│
├── 05-扩展模型 Extension ──── 扩展点接口 · @ExtendWith/@RegisterExtension · 条件 · 并行执行
│
├── 06-Mock 与依赖隔离 ────── Mockito 5.x · stub/verify · 静态/构造 Mock · 隔离策略
│
├── 07-Spring 生态集成测试 ─── SpringExtension · @SpringBootTest · Testcontainers · Boot 4 与 JUnit 6
│
├── 08-构建工具与 CI 集成 ──── Maven Surefire/Failsafe · Gradle · ConsoleLauncher · JFR · 覆盖率
│
└── 09-测试策略与最佳实践 ──── 测试金字塔 · FIRST · 命名规范 · 5→6 迁移 · 故障排查
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | 演进史与版本全景 | 三大时代（框架类→平台类→扩展生态）、5.x/6.x 双线版本、Java 17 基线、Vintage 弃用 | 所有开发者（先读） |
| 02 | 三平台架构与核心 API | JUnit Platform 作为引擎中立平台、Jupiter API 注解族、断言/假设、@Tag | 初级→中级 |
| 03 | 测试生命周期与嵌套 | @BeforeEach/@AfterEach 语义、@Nested 内聚分组、MethodOrderer、PER_CLASS | 中级 |
| 04 | 参数化测试全解析 | 六类数据源、类型转换、参数聚合、FastCSV（JUnit 6） | 中级 |
| 05 | 扩展模型 Extension | 声明式扩展、生命周期回调接口、@TempDir/@Timeout、并行执行配置 | 中级→高级 |
| 06 | Mock 与依赖隔离 | Mockito 5.x 核心用法、Mock/Spy、静态与构造 Mock、隔离边界 | 中级 |
| 07 | Spring 生态集成测试 | SpringExtension、@SpringBootTest 分层、Testcontainers 一等公民、切片测试 | 中级→高级 |
| 08 | 构建工具与 CI 集成 | Surefire/Failsafe 3.x 配置、Gradle、ConsoleLauncher、CI 流水线、JFR 分析 | 中级→高级 |
| 09 | 测试策略与最佳实践 | 测试金字塔、FIRST 原则、命名、5→6 平滑迁移、常见故障排查 | 全员 |

## 3. 学习路线推荐

**路线 A：快速上手（1 天）**
01 演进史 → 02 核心 API → 03 生命周期 → 09 最佳实践。看完即可在项目中写出高质量单测。

**路线 B：中级工程化（1 周）**
路线 A 全部 + 04 参数化 → 06 Mock → 08 构建集成。覆盖日常开发 90% 的测试场景（含 CI 配置）。

**路线 C：高级专家（1-2 周）**
路线 B 全部 + 05 扩展模型 → 07 Spring 生态 → 09 迁移实战。掌握自研扩展、并行执行、Boot 4 测试体系与 JUnit 5→6 升级。

## 4. 核心概念速查

### 4.1 版本速览（2026-08）

| 版本线 | 最新版 | 状态 | 定位 |
|--------|--------|------|------|
| JUnit 6.x | **6.1.1**（2026-06-28） | 主推 | 新项目默认：Java 17 基线、统一版本号、Vintage 弃用 |
| JUnit 5.x | 5.14.4（2026-04-26） | 维护 | 存量系统维护线，Java 8+ 兼容 |
| JUnit 4.x | 4.13.2 | 终止 | 仅经 Vintage 桥接运行，Spring Boot 4 已移除 |

### 4.2 生态横向对比

| 框架 | 定位 | 与 JUnit 的关系 |
|------|------|----------------|
| TestNG | 早期平台化框架（分组/并行/依赖） | 可作 Platform 第三方引擎共存 |
| Spock | Groovy 行为驱动测试（Given/When/Then） | 基于 JUnit Platform 运行（JUnit 5 起） |
| AssertJ | 流式断言库（`assertThat(x).isEqualTo(y)`） | 与 Jupiter 断言互补，Boot 4 starter-test 自带 |
| Mockito | Mock 框架（5.x） | MockitoExtension 原生集成 Jupiter |
| Testcontainers | 真实依赖容器化 | Boot 4 通过 `@ServiceConnection` 一等公民 |
| Pitest | 变异测试（代码质量强化） | 与覆盖率互补，验证测试有效性 |

### 4.3 学习验收清单

- [ ] 能说出 Platform/Jupiter/Vintage 三层的职责边界
- [ ] 能写出五段式生命周期并解释 PER_METHOD vs PER_CLASS 取舍
- [ ] 能独立配置 Maven/Gradle + JUnit 6 并跑通 `mvn test`
- [ ] 能用 @ParameterizedTest 覆盖边界、@Nested 组织分组、Extension 封装横切逻辑
- [ ] 能配置并行执行并说出三条隔离性陷阱
- [ ] 能完成一个模块的 JUnit 5 → 6 迁移并做回归验证

| 概念 | 一句话说明 | 所在模块 |
|------|-----------|---------|
| JUnit Platform | 引擎中立测试平台（Launcher API + 引擎 SPI），Jupiter/Vintage 皆为其引擎 | 02 |
| Jupiter | JUnit 5/6 的测试引擎与 API 模型，`org.junit.jupiter` 包 | 02 |
| Vintage | 运行 JUnit 3/4 旧测试的兼容引擎，**JUnit 6 起弃用**（仅迁移桥） | 02/09 |
| 生命周期回调 | `@BeforeAll/@BeforeEach/@Test/@AfterEach/@AfterAll` 五段式执行模型 | 03 |
| `@Nested` | 非静态内部类，按树形组织关联测试并继承外层状态 | 03 |
| 参数化测试 | 同一测试方法以多组数据循环执行，`@ParameterizedTest` | 04 |
| Extension 扩展 | JUnit 的核心扩展机制，替代 JUnit 4 的 Runner/Rule | 05 |
| `@TempDir` | 内置扩展，为每个测试创建可自动清理的临时目录 | 05 |
| 并行执行 | 配置 `junit.jupiter.execution.parallel.enabled=true` 开启，6.1 可选线程池实现 | 05 |
| Mockito | 最主流的 Java Mock 框架（5.x），与 Jupiter 原生集成 | 06 |
| SpringExtension | Spring TestContext 与 Jupiter 的桥接扩展，`@SpringBootTest` 的底层 | 07 |
| Testcontainers | 真实依赖（DB/Redis/Kafka）容器化测试，Boot 4 通过 `@ServiceConnection` 一等支持 | 07 |
| Surefire/Failsafe | Maven 单测/集成测试执行插件，3.x 支持 JUnit 6 | 08 |
| JFR 集成 | JUnit 6 起 JFR 事件内置于 `junit-platform-launcher`，免额外依赖记录测试事件 | 08 |
| `org.junit.start` | JUnit 6.1 新增模块，Java 25 单文件源码模式跑测试 | 08/01 |

> 🎯 **核心要点**：JUnit 5 是"平台化"的架构革命（引擎中立、扩展驱动、函数式断言）；JUnit 6 是"现代化"的版本革命（Java 17 基线、统一版本号、Vintage 弃用）。2026 年新项目应直接用 JUnit 6，存量 5.x 项目迁移成本极低（两年弃用期 API 之外基本零改动）。

---

**下一模块**：[01-演进史与版本全景](01-演进史与版本全景.md)

**【参考来源】**
- JUnit 5.14.4 Release Notes：https://docs.junit.org/5.14.4/release-notes.html
- JUnit 6.1.1 Release Notes：https://docs.junit.org/6.1.1/release-notes.html
- Upgrading to JUnit 6.0（官方 Wiki）：https://github.com/junit-team/junit-framework/wiki/Upgrading-to-JUnit-6.0
