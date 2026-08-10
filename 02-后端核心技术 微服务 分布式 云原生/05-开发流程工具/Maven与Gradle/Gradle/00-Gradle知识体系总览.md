# 00 - Gradle 知识体系总览

> 🎯 Gradle 是 JVM 构建工具的事实标准之二（与 Maven 并列）——基于任务 DAG 的构建模型、Kotlin DSL 类型安全脚本、增量构建 + 配置缓存 + 构建缓存三层加速。2026 年 Gradle 9.7 时代：Kotlin DSL 默认、Declarative Gradle 成为下一代方向

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [2026 版本与演进](#3-2026-版本与演进)
4. [与 Maven 体系的定位分工](#4-与-maven-体系的定位分工)
5. [学习路线推荐](#5-学习路线推荐)
6. [核心概念速查](#6-核心概念速查)
7. [七大常见误区](#7-七大常见误区)
8. [参考来源](#8-参考来源)

---

## 1. 知识全景

```
Gradle 知识体系（11 个文件 — 定位→安装→生命周期→依赖→多模块→插件→DSL→性能→生产→面试）
│
├── 🏗️ 概述（01）
│   └── 01-Gradle是什么与演进史.md      # 定位/任务DAG模型/DSL演进/Groovy→Kotlin→Declarative
│
├── 🚀 入门（02）
│   └── 02-安装与快速开始.md           # Wrapper/Java工具链/daemon/第一个Kotlin DSL构建
│
├── ⚙️ 生命周期（03）
│   └── 03-构建生命周期与Task.md       # 三阶段/任务图/增量构建/up-to-date/Task provenance
│
├── 📦 依赖（04）
│   └── 04-依赖管理.md                 # 仓库/坐标/冲突仲裁/BOM/版本目录/依赖锁定
│
├── 🏢 多模块（05）
│   └── 05-多模块与复合构建.md          # settings/约定插件/buildSrc/复合构建/Isolated Projects
│
├── 🔌 插件（06）
│   └── 06-插件体系与自定义开发.md      # 核心/社区插件/插件开发/pluginManagement
│
├── 🧩 DSL（07）
│   └── 07-KotlinDSL与惰性配置.md      # 类型安全/Provider惰性/配置避免/gradle.properties
│
├── ⚡ 性能（08）
│   └── 08-构建缓存与性能优化.md        # 增量/配置缓存/本地远程缓存/kapt→KSP/CI优化
│
├── 🏭 生产（09）
│   └── 09-生产实践与CI-CD集成.md      # Wrapper纪律/工具链/发布制品/CI集成/Develocity
│
├── 🎓 面试（10）
│   └── 10-与Maven对比与面试自测.md     # 对比选型/迁移路径/面试三范式/20自测
│
└── 📌 00-Gradle知识体系总览.md         # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景导航 + 2026 版本 + 分工 + 速查 | — |
| 01 | Gradle 是什么与演进史 | 任务 DAG/DSL 演进/五边界 | ⭐⭐ |
| 02 | 安装与快速开始 | Wrapper/工具链/daemon/首个构建 | ⭐ |
| 03 | 构建生命周期与 Task | 三阶段/任务图/增量构建 | ⭐⭐⭐ |
| 04 | 依赖管理 | 仓库/仲裁/BOM/版本目录/锁定 | ⭐⭐⭐ |
| 05 | 多模块与复合构建 | settings/约定插件/复合构建 | ⭐⭐⭐ |
| 06 | 插件体系与自定义开发 | 插件类型/开发/发布/管理 | ⭐⭐ |
| 07 | Kotlin DSL 与惰性配置 | 类型安全/Provider/配置避免 | ⭐⭐ |
| 08 | 构建缓存与性能优化 | 三层加速/配置缓存/CI 优化 | ⭐⭐⭐ |
| 09 | 生产实践与 CI/CD 集成 | Wrapper 纪律/工具链/Develocity | ⭐⭐ |
| 10 | 与 Maven 对比与面试自测 | 对比选型/迁移/20 自测 | ⭐⭐ |

---

## 3. 2026 版本与演进

| 版本 | 时间 | 关键内容 |
|------|------|----------|
| 9.0.0 | 2025 | Kotlin DSL 默认、可复现归档、最低 Java 版本提升、Kotlin 2/Groovy 4 |
| 9.3.0 | 2026.01 | HTML 测试报告嵌套视图、bash/zsh 补全 |
| 9.5.0 | 2026.04 | 任务溯源（Task provenance）、Settings 约定插件类型安全、Wrapper 下载重试 |
| 9.6.0 | 2026 | 配置缓存命中率大幅提升（属性精确追踪）、--non-interactive、NO_COLOR |
| 9.7.0 | 2026 | 已发布（细节见官方 release notes） |

学习本体系的前置建议：先有 Git 与 Java 基础（构建工具消费 Git 仓库与 JDK 编译产物），如已掌握 Maven（上级目录 01-13 篇）则对比理解更快——本体系每篇都给出与 Maven 的对照点，10 篇做系统对比。

2026 方向：Provider API 迁移推迟到 Gradle 10（需更好 IDE 支持）、Isolated Projects 在配置缓存产品化后推进、**Declarative Gradle 上线独立官网**——"描述构建是什么"取代"编写构建如何运行"，是下一代构建描述方式。DSL 演进主线：Groovy → Kotlin DSL → Declarative Gradle。

## 4. 与 Maven 体系的定位分工

| 维度 | Gradle | Maven |
|------|--------|-------|
| 构建模型 | 任务 DAG（Task 图，可自由编排） | 生命周期阶段（固定三生命周期） |
| 配置语言 | Kotlin DSL（类型安全）/Groovy | XML（pom.xml） |
| 增量构建 | 原生支持（up-to-date 检查） | 有限（插件实现） |
| 构建缓存 | 本地 + 远程 + 配置缓存 | 无原生 |
| 依赖管理 | 同 Maven 坐标体系 + 版本目录 | 坐标 + BOM |
| 多模块 | 约定插件 + 复合构建 | 聚合 + 继承 |
| 适用 | 大型/多模块/Android/性能敏感 | 传统 Java/企业规范 |

**分工结论**：两者坐标体系兼容（依赖都能互认），差异在构建模型与性能。Maven 简单规范适合中小项目与团队统一；Gradle 灵活强大适合多模块大项目与性能敏感场景。上级目录有 Maven 旧体系（01-13 篇），本体系是 Gradle 深潜，10 篇做对比。

## 5. 学习路线推荐

### 🟢 快速上手（半天）
```
02-安装与快速开始 → 03-构建生命周期 → 04-依赖管理
产出：能用 Gradle 构建 Java 项目，理解任务与依赖
```

### 🔵 工程化（1 天）
```
05-多模块 → 06-插件 → 07-KotlinDSL → 08-构建缓存
产出：能搭建多模块项目、写约定插件、优化构建速度
```

### 🔴 专家（1 天）
```
08-性能深潜 → 09-生产CI → 10-对比迁移
产出：能诊断构建性能、配置 CI 缓存、主导 Maven 迁移
```

---

## 6. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| 任务 DAG | 构建 = 任务依赖图，任务按依赖关系执行，可自由编排 |
| Wrapper | 固定 Gradle 版本的启动器（gradlew），团队版本一致的基石 |
| Kotlin DSL | 默认构建脚本语言，类型安全 + IDE 自动补全 |
| Configuration Cache | 缓存配置阶段输出，复用跨构建配置（9.6 命中率大增） |
| 增量构建 | inputs/outputs 未变的任务标记 up-to-date，跳过执行 |
| 本地/远程缓存 | 复用任务输出：本地跨工作区、远程跨团队/CI |
| 版本目录 | libs.versions.toml 集中管理依赖版本 |
| 约定插件 | 共享构建逻辑的插件（convention plugins），多模块标配 |
| 复合构建 | 多个独立构建组合为一个（composite builds） |
| Task provenance | 9.5 任务溯源：错误信息标明任务注册来源 |
| Declarative Gradle | 下一代 DSL：描述"构建是什么"而非"怎么运行" |
| Build Scan | 构建可观测性报告，性能诊断的核心工具 |

---

## 7. 七大常见误区

1. **"Gradle 只是快一点的 Maven"**——构建模型本质不同：Maven 是固定生命周期阶段，Gradle 是自由任务 DAG
2. **"Groovy 和 Kotlin DSL 随便选"**——2026 新项目应直接用 Kotlin DSL（9.0 起默认），Groovy 只处理存量
3. **"Wrapper 是多余的"**——Wrapper 固定版本，团队/CI 与本地一致，是构建可复现的基石
4. **"配置缓存开了就完事"**——9.6 前命中率受属性读取影响，要遵守"配置阶段不读属性"的纪律
5. **"依赖冲突靠 exclude 全删"**——先升级传递依赖版本或用依赖约束，exclude 是最后手段
6. **"kapt 还能用就不换"**——KSP 快 2 倍以上且支持增量，新注解处理项目直接 KSP
7. **"远程缓存=构建缓存"**——Develocity 的 Universal Cache 含 Setup/Artifact/Build 三层，各解决一段

---

## 8. 参考来源

- [Gradle 9.6.0 Release Notes - 官方](https://docs.gradle.org/9.6.0/release-notes.html)
- [What's new in Gradle 9.0.0 - gradle.org](https://gradle.org/whats-new/gradle-9/)
- [Gradle Build Tool Newsletter 2026.04](https://newsletter.gradle.org/html/2026-04-28-April2026/index.html)
- [Three Gradle Talks from KotlinConf 2026 - Gradle Blog](https://blog.gradle.org/gradle-at-kotlinconf-2026)
- [Build Acceleration 2026.1 - Develocity Docs](https://docs.gradle.com/develocity/2026.1/administration/build-acceleration/)
- [Develocity 2026.2 发布 - Develocity](https://develocity.ai/releases/2026.2)
- [Essential Tips for Gradle on Ephemeral CI Environments - Gradle Blog](https://blog.gradle.org/gradle-on-ephemeral-ci-2)
- [Gradle 9.7.0 - Gradle 论坛](https://discuss.gradle.org/t/9-7-0/52164)

---

> 🎯 **核心要点**：Gradle = 任务 DAG 构建模型 + Kotlin DSL 类型安全 + 三层加速（增量/配置缓存/构建缓存）。2026 版本线 9.7，配置缓存命中率与任务溯源是亮点，Declarative Gradle 是下一代方向。选型速记：多模块大项目/Android/性能敏感 → Gradle；传统 Java/规范统一 → Maven。最佳实践：Kotlin DSL + 版本目录 + 约定插件 + Wrapper 固定版本。

**下一模块**：[01-Gradle是什么与演进史](01-Gradle是什么与演进史.md)
