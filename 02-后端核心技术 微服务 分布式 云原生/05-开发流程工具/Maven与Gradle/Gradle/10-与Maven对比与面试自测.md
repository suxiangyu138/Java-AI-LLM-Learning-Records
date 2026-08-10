# 10 - 与 Maven 对比与面试自测

> 🎯 收尾篇：Gradle vs Maven 全维对比、Maven → Gradle 迁移路径、迁移避坑清单，以及 20 道自测题与面试三范式。核心结论：**模型不同（任务 DAG vs 生命周期）、坐标兼容、迁移是换构建哲学不是换工具**

---

## 目录

1. [全维对比：Gradle vs Maven](#1-全维对比gradle-vs-maven)
2. [选型决策树](#2-选型决策树)
3. [Maven → Gradle 迁移路径](#3-maven--gradle-迁移路径)
4. [迁移避坑清单](#4-迁移避坑清单)
5. [面试三范式](#5-面试三范式)
6. [简历呈现写法](#6-简历呈现写法)
7. [毕业检查单](#7-毕业检查单)
8. [自测 20 题](#8-自测-20-题)

---

## 1. 全维对比：Gradle vs Maven

| 维度 | Gradle | Maven |
|------|--------|-------|
| 构建模型 | 任务 DAG（自由编排） | 生命周期阶段（固定） |
| 配置语言 | Kotlin DSL（类型安全） | XML（声明式） |
| 增量构建 | 原生 up-to-date + 增量编译 | 插件级 |
| 构建缓存 | 本地/远程/配置缓存 | 无原生 |
| 依赖仲裁 | 最新者胜（newest） | 最先声明者胜 |
| 依赖管理 | 版本目录 + 约束 + 锁定 | BOM + 属性 |
| 多模块 | 约定插件 + 复合构建 | 聚合 + 继承 |
| 灵活性 | 高（脚本可编程） | 低（标准但受限） |
| 学习曲线 | 陡 | 缓 |
| Android | 唯一官方支持 | 无 |
| 性能 | 大型项目显著更快 | 中小项目差距小 |
| 可观测 | Build Scan 生态 | 有限 |

**结论**：不是"谁更好"而是"谁匹配"。Gradle 胜在模型灵活 + 性能 + Android；Maven 胜在简单规范 + 生态成熟。**坐标与制品互认**——两个体系可以共存于同一技术栈（库用 Maven 发布、应用用 Gradle 构建）。

## 2. 选型决策树

```text
Android/多平台项目？ ── 是 → Gradle（唯一选择）
        │
        否 → 大型多模块/性能敏感？ ── 是 → Gradle（增量+缓存收益大）
        │
        否 → 团队规范统一优先？ ── 是 → Maven（简单规范）
        │
        └ 微服务多仓库/复合构建需求？ ── 是 → Gradle（复合构建）
```

三个判断维度：**平台绑定**（Android 必须 Gradle）、**规模与性能**（大型多模块 Gradle 优势显著）、**团队能力**（新手团队 Maven 上手快，有 Kotlin 经验的团队 Gradle 无障碍）。迁移本身有成本，没有明确收益不动——**"别人都在用"不是迁移理由**。

## 3. Maven → Gradle 迁移路径

```text
迁移五步：
① 生成骨架：gradle init --type java-library（自动识别 pom.xml）
   → 或 gradle init 交互式（选择 Kotlin DSL）
② 转换依赖：pom.xml 的 <dependencies> → build.gradle.kts 的 dependencies 块
   → gradle init 可自动转换大部分
③ 转换插件：maven-compiler/surefire/jar 等 → java/java-library 插件
   → 自定义插件（如 shade 打包）→ 生态替代（shadow 插件）
④ 验证一致性：对比 mvn build 与 gradle build 的产物
   → 运行全量测试，逐模块验证
⑤ 切换 CI：CI 从 mvn 换 ./gradlew，持久化 GRADLE_USER_HOME 缓存
```

**转换核心映射**：`maven-compiler-plugin` → java 插件 toolchain；`maven-surefire-plugin` → test 任务（useJUnitPlatform）；`maven-shade-plugin` → shadow 插件；`maven-assembly-plugin` → distribution；`spring-boot-maven-plugin` → org.springframework.boot 插件。版本属性（`<properties>`）→ 版本目录；BOM 导入 → `implementation(platform(...))`。

**迁移纪律**：先在一个模块试点（验证流程与性能收益），再全量迁移；迁移期间双构建并行（mvn 与 gradlew 都跑），产物对比一致后切默认；**不要在迁移同时重构业务**——一次只做一件事。

## 4. 迁移避坑清单

| # | 坑 | 解法 |
|---|-----|------|
| 1 | 依赖仲裁差异 | Maven 最先声明 vs Gradle 最新——迁移后冲突版本可能变，跑全量测试验证 |
| 2 | 传递依赖爆炸 | Maven 全传递 vs Gradle 默认——用 implementation 而非 api 控制暴露 |
| 3 | 自定义 Maven 插件 | 没有 Gradle 等价物 → 重写为 Gradle 任务/插件 |
| 4 | profile 多环境 | Maven profile → Gradle 用属性/自定义任务变体 |
| 5 | 资源过滤 | Maven 资源过滤 → processResources 配置 |
| 6 | 增量行为差异 | Maven 全量 vs Gradle 增量——测试依赖顺序敏感？用 --rerun-tasks 排查 |
| 7 | 版本目录缺失 | 迁移时顺手建 libs.versions.toml，避免散落硬编码版本 |
| 8 | CI 缓存没配 | 不持久化 GRADLE_USER_HOME，CI 每天全量下载依赖 |
| 9 | 文档还写 mvn | 项目文档、贡献指南同步更新 |
| 10 | 迁移中重构 | 迁移与业务改动隔离，一次一件事 |

## 5. 面试三范式

**价值式**：Gradle 解决什么问题？——Maven 时代构建的三大痛点：配置冗长（XML）、增量差（全量重编）、扩展受限（固定生命周期）。Gradle 用任务 DAG + Kotlin DSL + 增量/缓存三层加速解决：大型多模块项目构建时间从分钟级到秒级、配置可编程可复用、Android 生态唯一选择。

**技术式**：核心机制？——任务 DAG（register 懒注册 + dependsOn + 增量 up-to-date）；配置缓存（配置阶段输出序列化复用，惰性配置是前提）；构建缓存（本地/远程复用任务输出）；Kotlin DSL 类型安全 + 版本目录 + 约定插件；依赖仲裁最新者胜 + constraints/BOM/锁定。

**生产式**：你踩过什么坑？——本机过 CI 挂（Wrapper 版本不一致）；配置缓存报错（配置阶段读环境变量，Provider 化解决）；缓存命中率 0（Build Scan 查出绝对路径输入）；CI 慢（持久化 GRADLE_USER_HOME 省 28%）；kapt 换 KSP 构建时间减半。

**追问四连**：register vs create？配置缓存前提？api vs implementation？Maven 迁移的三个坑？

## 6. 简历呈现写法

```text
主导团队 Maven → Gradle 迁移与构建性能优化
- 迁移 30+ 模块至 Kotlin DSL + 版本目录 + 约定插件，配置量减少 60%
- 落地配置缓存与本地/远程构建缓存，全量构建 8 分钟 → 2.5 分钟（-69%）
- 接入 Build Scan 可观测性，缓存命中率从 30% 提升至 80%+
```

要点：量化（构建时间/命中率/配置量）、点名机制（配置缓存/约定插件/Build Scan）、体现工程决策（迁移策略而非"换工具"）。

## 7. 毕业检查单

1. 能说出任务 DAG 与生命周期阶段的本质差异
2. 能解释 Wrapper 为什么是工程基石
3. 能画出生命周期三阶段与执行状态机
4. 能写出 Java 项目的完整 build.gradle.kts
5. 能解释 api vs implementation 与依赖仲裁
6. 会用版本目录与依赖锁定
7. 能搭建多模块 + 约定插件工程
8. 能解释配置缓存机制与惰性纪律
9. 能设计 CI 缓存持久化方案
10. 能答出自测 20 题中的 15 题以上

## 8. 自测 20 题

1. 任务 DAG 与生命周期阶段的本质差异？
2. Wrapper 四文件？为什么 .jar 必须提交？
3. 生命周期三阶段？配置阶段跑什么？
4. register vs create？配置避免原则？
5. inputs/outputs 与 UP-TO-DATE 的关系？
6. Task provenance（9.5）解决什么问题？
7. implementation 与 api 的差异？
8. 冲突仲裁规则与 Maven 的差异？
9. 版本目录的三个价值？
10. 依赖锁定是什么？动态版本为什么危险？
11. 约定插件 vs buildSrc vs 复合构建？
12. pluginManagement 的三个职责？
13. Provider 惰性的核心语义？为什么是配置缓存前提？
14. Property 的 convention 与 set 区别？
15. 配置缓存的纪律？9.6 属性精确追踪？
16. 四层复用的分工？
17. Ephemeral CI 的最大单项优化？
18. kapt 为什么换 KSP？
19. maven-publish 的 pom 元数据为什么重要？
20. Maven 迁移的三个核心坑？

**加试 5 题**：配置缓存报错怎么排查；远程缓存的两种来源（Develocity/HTTP）差异；复合构建与多模块的适用边界；可复现归档（9.0）是什么；Declarative Gradle 与 Kotlin DSL 的关系。

---

> 🎯 **核心要点**：Gradle vs Maven = 模型差异（DAG vs 生命周期）+ 坐标兼容 + 性能分层。选型看平台/规模/团队三因素，迁移有明确收益才动。面试主线：任务 DAG + 配置缓存 + 构建缓存 + 约定插件，四件套讲清楚即达生产水平。自测 20 题全过即毕业。

**返回总览**：[00-总览](00-Gradle知识体系总览.md)
