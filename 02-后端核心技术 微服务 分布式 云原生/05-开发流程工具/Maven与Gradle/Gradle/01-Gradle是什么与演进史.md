# 01 - Gradle 是什么与演进史

> 🎯 Gradle 是 JVM 生态的两大构建工具之一，核心差异在构建模型：Maven 是固定生命周期阶段，Gradle 是自由编排的任务 DAG。DSL 从 Groovy 演进到 Kotlin DSL（2026 默认）再到 Declarative Gradle（下一代），理解这条线就理解了 Gradle 的设计哲学

---

## 目录

1. [Gradle 是什么：任务 DAG 构建器](#1-gradle-是什么任务-dag-构建器)
2. [演进史：2007 到 Declarative Gradle](#2-演进史2007-到-declarative-gradle)
3. [DSL 演进：Groovy → Kotlin → Declarative](#3-dsl-演进groovy--kotlin--declarative)
4. [与 Maven 的本质差异](#4-与-maven-的本质差异)
5. [解决什么问题](#5-解决什么问题)
6. [五个边界](#6-五个边界)
7. [练习](#7-练习)

---

## 1. Gradle 是什么：任务 DAG 构建器

Gradle 是一个基于 **任务依赖图（Task DAG）** 的构建自动化工具：构建脚本定义一组任务（task），任务之间声明依赖关系（dependsOn），Gradle 构建依赖图后按拓扑序执行——这就是它与 Maven 最本质的区别。

```text
Maven： 生命周期阶段（validate → compile → test → package → verify → install）
        阶段顺序固定，行为由插件挂到阶段上

Gradle： 任务图（compileJava → processResources → classes → test → jar → ...）
        任务依赖自由编排，每个任务有独立的 inputs/outputs 与增量检查
```

任务 DAG 的三个推论：**按需执行**（只跑依赖链上的任务，`gradle test` 不跑 `jar`）、**自由编排**（自定义任务任意插入依赖链）、**增量友好**（每个任务独立判 up-to-date）。这让 Gradle 在大型多模块项目中表现出远超 Maven 的灵活性与增量性能——同样跑 `test`，Maven 要过一遍固定生命周期（validate→compile→...），Gradle 只跑 test 依赖链上的任务，配置正确的项目能跳过无关模块的编译。

## 2. 演进史：2007 到 Declarative Gradle

| 阶段 | 时间 | 关键事件 |
|------|------|----------|
| 诞生 | 2007 | 基于 Groovy 的 JVM 构建工具项目启动（借鉴 Ant/Ivy/Maven） |
| 成熟 | 2012 | 1.0 发布，确立"任务 DAG + 约定优于配置"路线 |
| Android 时代 | 2013+ | Android Gradle Plugin（AGP）采用，成为移动构建事实标准 |
| 性能革命 | 2017-2019 | 配置缓存、构建缓存、增量编译逐步落地 |
| Kotlin DSL | 2019+ | Kotlin DSL 稳定，类型安全构建脚本 |
| 9.x | 2025-2026 | Kotlin DSL 默认、可复现归档、配置缓存命中率提升（9.6）、任务溯源（9.5） |
| 下一代 | 2026+ | Declarative Gradle 独立官网，Provider API 迁移至 Gradle 10 |

演进主线的两条腿：**性能**（增量 → 缓存 → 配置缓存，逐层解决"重复构建"）与**语言**（Groovy → Kotlin DSL → Declarative）。Gradle 的竞争力一直是"快 + 灵活"，两条腿缺一不可。

## 3. DSL 演进：Groovy → Kotlin → Declarative

三种 DSL 不是简单的版本升级，而是三种不同的构建描述哲学：

- **Groovy DSL**（.gradle）：动态语言，最灵活但无类型检查，IDE 支持弱；存量项目大量存在，Gradle 9.6 起父项目属性隐式查找已废弃（10 移除）
- **Kotlin DSL**（.gradle.kts）：静态类型 + 自动补全 + 可靠重构，9.0 起默认；**2026 年新项目的推荐选择**
- **Declarative Gradle**：下一代方向——"描述构建是什么"而不是"编写构建如何运行"；处于早期阶段（独立官网 + Kotlin 生态插件演示），官方明确它不会取代 Kotlin DSL 而是演进方向

选型判断：新项目直接 Kotlin DSL；存量 Groovy 项目不强制迁移（除非要 IDE 体验或新特性）；Declarative Gradle 保持关注即可。**不要在 Groovy 上写新逻辑**——它已进入维护模式。

## 4. 与 Maven 的本质差异

| 维度 | Gradle | Maven |
|------|--------|-------|
| 构建模型 | 任务 DAG（自由编排） | 生命周期阶段（固定） |
| 配置 | Kotlin DSL/Groovy（可编程） | XML（声明式但冗长） |
| 增量 | 原生 up-to-date + 增量编译 | 插件级支持 |
| 缓存 | 配置缓存 + 构建缓存 | 无 |
| 灵活性 | 高（自定义任务/钩子） | 低（标准但受限） |
| 学习曲线 | 陡（DSL + 概念多） | 缓（XML 简单） |
| 规范统一 | 靠约定插件自建 | 天生统一 |

**本质差异不是"快慢"而是"模型"**：Maven 的 XML 是"声明结果"，Gradle 的 DSL 是"描述过程"——前者适合团队统一规范（默认行为一致），后者适合复杂定制（多模块变体、自定义构建逻辑）。依赖坐标体系两者兼容，迁移成本主要在构建逻辑本身。一个简单的判断法：构建需求"每个项目都一样"→ Maven 省心；"每个项目都有差异"→ Gradle 值得。

## 5. 解决什么问题

- **构建慢**：增量构建 + 配置缓存 + 构建缓存三层加速，大型项目构建时间从分钟级降到秒级
- **配置繁琐**：Kotlin DSL 类型安全 + 约定插件复用，多模块配置量大幅缩减
- **多模块复杂**：DAG 自由编排 + 复合构建，适合微服务/平台类多项目结构
- **构建不可复现**：Wrapper 固定版本 + 可复现归档（9.0 默认字节级一致）+ 依赖锁定
- **Android 生态**：AGP 的唯一选择（KSP/变体/资源处理深度集成）
- **构建可观测**：Build Scan 记录每一步耗时与缓存命中，性能问题可定位
- **Kotlin 生态**：Kotlin Multiplatform（KMP）官方支持 Gradle 构建，共享业务逻辑跨平台分发

与 Maven 共存的现实：绝大多数团队不会"二选一"——库与中间件常用 Maven 构建（兼容性最广），应用与 Android 用 Gradle；两者坐标互认让混用无痛。选型时不要被"全栈统一工具"诱惑，**按项目类型各选所长**才是工程常态。

## 6. 五个边界

- **不做依赖解析规范**：依赖坐标体系沿用 Maven 中央仓库约定，Gradle 是消费方
- **不做制品仓库**：制品发布到 Maven/Ivy 仓库，Nexus/Artifactory 是仓库服务器
- **不替代 IDE**：IDEA/VS Code 的 Gradle 集成是独立的（04 篇不展开，见 IDE 体系）
- **不管理运行时**：Java 工具链管理是编译/运行环境选择，不是应用部署
- **不是 CI/CD 平台**：Gradle 是 CI 流水线里的构建步骤，流水线编排属于 CI/CD 体系

对 Java 后端开发者的实际意义：2026 年的主流 Java 项目里 Gradle 与 Maven 分布已接近五五开，且新项目（尤其 Kotlin、Android、多模块平台）倾向 Gradle——**会 Maven 是基础，会 Gradle 是差异化**。被问到"为什么用 Gradle"，能答出"任务 DAG + 增量缓存 + Kotlin DSL"三层的人，与只会说"它快"的人是完全不同的水平——把 Gradle 讲成工程能力而非工具命令，是本体系的目标。

## 7. 练习

1. 用一句话解释任务 DAG 与生命周期阶段的区别
2. 画出 Gradle 的演进主线（性能 + 语言两条腿）
3. 三种 DSL 的哲学差异？2026 年新项目选什么？
4. 与 Maven 的本质差异是什么？依赖坐标体系兼容意味着什么？
5. Gradle 解决哪五类问题？边界在哪？

---

> 🎯 **核心要点**：Gradle = 任务 DAG 模型（自由编排 + 增量友好）+ 语言演进（Groovy 维护 → Kotlin DSL 默认 → Declarative 下一代）+ 性能三层加速。与 Maven 的本质差异是构建模型而非快慢。2026 姿势：新项目 Kotlin DSL、Wrapper 固定版本、配置缓存开启、约定插件复用——Maven 迁移不是搬家而是换构建哲学。

**下一模块**：[02-安装与快速开始](02-安装与快速开始.md) / **返回总览**：[00-总览](00-Gradle知识体系总览.md)
