# 04 - Gradle 深度剖析

> **核心摘要**：Gradle 是 Java 生态的编程式构建之王（2026.06 已到 **9.3**，IDEA 2026.1 全面支持）——**Task 图 + 增量构建 + 构建缓存**是性能三件套，Groovy/Kotlin DSL 提供可编程性。理解三件事：① Task 模型（构建 = 依赖图遍历）② 配置阶段 vs 执行阶段 ③ 增量/缓存机制。

> **前置阅读**：[[01-核心概念与原理]] | [[02-Maven深度剖析]]

---

## 📚 目录

1. [Gradle 的定位与哲学](#1-gradle-的定位与哲学)
2. [构建脚本：DSL 与配置](#2-构建脚本dsl-与配置)
3. [Task 模型：构建的核心抽象](#3-task-模型构建的核心抽象)
4. [配置阶段 vs 执行阶段](#4-配置阶段-vs-执行阶段)
5. [增量构建机制](#5-增量构建机制)
6. [构建缓存](#6-构建缓存)
7. [依赖管理](#7-依赖管理)
8. [多模块与 Version Catalog](#8-多模块与-version-catalog)
9. [性能调优与常见坑](#9-性能调优与常见坑)
10. [核心要点](#10-核心要点)

---

## 1. Gradle 的定位与哲学

> **背景**：2007 年诞生，2012 年 Android 采用后爆发；以「快 + 可编程」挑战 Maven 的「约定 + 声明」。
> **目的**：理解 Task 图驱动构建的核心心智模型。
> **适用范围**：新项目、性能敏感、需要自定义构建逻辑的团队（2026 主流选择之一）。

```text
Gradle 的核心哲学：构建是「代码」不是「配置」
├── Maven：POM 是数据文件（你描述结果，插件决定过程）
├── Gradle：build.gradle 是代码（你控制过程，DSL 提供语义）
├── 性能：增量构建 + 缓存（只重跑变化部分）
└── 生态：Android 官方 / Spring Boot 官方 / 大部分新开源项目

Gradle 能做什么（覆盖 Maven 全部能力）
├── 依赖管理：Maven 坐标兼容（从 Maven Central/私有仓拉依赖）
├── 生命周期：自建任务图（不再有固定阶段，一切皆 Task）
├── 多模块：构建即图——天然支持并行/部分构建
├── 可扩展：自写 Task / 插件（几百行代码实现自定义构建逻辑）
└── 部署：War/Jar/Docker 镜像（配合插件）……

Gradle 9.3（2026.06）新看点
├── IDEA 2026.1 全面支持（本地依赖智能完成）
├── WSL/Docker 环境修复（跨平台构建一致性）
├── 复合构建 Version Catalog 逻辑修复
└── 快速修复「禁用离线模式并重新运行构建」
```

---

## 2. 构建脚本：DSL 与配置

> 🎯 **Gradle 脚本两种 DSL**——Groovy（历史默认）和 Kotlin（官方推荐新项目用）：

```kotlin
// build.gradle.kts（Kotlin DSL，2026 推荐写法）
plugins {
    java
    application
    id("org.springframework.boot") version "3.4.0"
}

group = "com.example"
version = "1.2.0"

repositories {
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public")  // 国内镜像
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

application {
    mainClass.set("com.example.MainKt")
}
```

```groovy
// build.gradle（Groovy DSL，老项目常见）
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.0'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
}
```

**Groovy vs Kotlin DSL 对比**：

| 维度 | Groovy DSL | Kotlin DSL（推荐） |
|------|:---:|:---:|
| 类型安全 | ❌ 运行时才报错 | ✅ 编译期检查 |
| IDE 支持 | 一般 | ✅ 补全/导航/重构 |
| 性能 | 快 | 稍慢（Kotlin 编译开销） |
| 学习成本 | 低（脚本友好） | 中（Kotlin 语法） |
| 新项目建议 | 存量沿用 | ✅ 新项目首选 |

---

## 3. Task 模型：构建的核心抽象

> 🎯 **Gradle 没有「生命周期阶段」，只有 Task（任务）**——构建 = Task 依赖图的一次遍历执行。

```text
Task 模型
├── Task = 构建中的最小工作单元（编译/测试/打包……）
├── 每个 Task 有：输入（inputs）+ 动作（action）+ 输出（outputs）
├── Task 依赖：A dependsOn B → 执行 A 前先执行 B
└── 构建 = 「从你要的 Task 出发，按依赖图逆推执行」

常用内置 Task（Java 插件提供）
├── compileJava / compileTestJava   编译
├── processResources                资源处理
├── test                            测试
├── jar / bootJar                   打包
├── clean                           清理
├── build                           组装 + 检查
└── publish / publishToMavenLocal   发布
```

```kotlin
// 自定义 Task（Gradle 可编程性的体现）
tasks.register("printEnv") {
    doLast {
        println("环境: ${System.getenv("ENV") ?: "dev"}")
    }
}

// 自定义 Task 接入依赖图
tasks.named("build") {
    dependsOn("printEnv")   // build 前先跑 printEnv
}

// Task 输入输出声明（增量构建的前提）
tasks.register("genConfig") {
    inputs.property("env", env)                 // 输入声明
    outputs.file(layout.buildDirectory.file("config.properties"))  // 输出声明
    doLast { ... }
}
```

> 💡 **Task 的三个阶段**：注册（register，惰性）→ 配置（configuration）→ 执行（action）——只有**执行阶段**才真正干活，配置阶段只建图。

---

## 4. 配置阶段 vs 执行阶段

> 🎯 **Gradle 构建分两个阶段**——配置阶段建图（慢但只跑一次），执行阶段跑任务（快）。理解这个才能优化构建速度：

```text
构建执行流程
├── ① 初始化：定位 settings.gradle.kts（多模块项目入口）
├── ② 配置阶段（Configuration）：执行所有构建脚本
│   ├── 创建全部 Task（无论本次要不要跑）
│   ├── 解析依赖图
│   └── 慢的元凶：脚本里写了耗时逻辑（网络/IO）
├── ③ 执行阶段（Execution）：按依赖图执行选中 Task 的 action
└── 优化核心：让配置阶段「只建图不干活」

常见性能误区
├── ❌ 在配置阶段做网络请求/文件 IO → 每次构建都做
├── ❌ 用 println 排查（配置阶段会打印多次）
├── ❌ 配置阶段执行耗时逻辑 → 用 doLast/doFirst 包进 action
└── ✅ 惰性配置：tasks.register（注册不配置，用到才配置）
```

```text
Gradle 阶段记忆
├── 「配置阶段决定跑什么，执行阶段决定跑多快」
├── 看构建报告：--profile 生成配置/执行耗时分布
└── 金句：把「计划」和「执行」分开——计划再复杂也不该耗时
```

---

## 5. 增量构建机制

> 🎯 **Gradle 的招牌性能能力**——只重跑「输入变了」的 Task，没变的直接跳过：

```text
增量构建原理
├── 每个 Task 声明 inputs + outputs
├── 执行前：对比上次的 input 快照（文件内容哈希/属性值）
├── 输入没变 + 输出存在 → UP-TO-DATE（跳过，秒完成）
├── 输入变了 → 重跑 + 记录新快照
└── 关键：Task 必须正确声明 inputs/outputs（声明不全 = 每次全量跑）

执行状态
├── UP-TO-DATE   输入输出都没变（跳过）
├── FROM-CACHE   命中构建缓存（远程/本地产物复用）
├── NO-SOURCE    无输入源（跳过）
├── SUCCESS      正常执行
└── FAILED       失败（首次执行没有缓存时才是真失败）

对比 Maven 4
├── Maven 4：树形生命周期避免冗余（模块级）
├── Gradle：Task 级增量（比模块级更细）
└── 结论：增量粒度 Gradle 更细——大项目收益更明显
```

> ⚠️ **增量失效的常见原因**：Task 未声明 inputs/outputs；输入包含不确定值（时间戳/随机数）；多 Task 共享同一输出文件（冲突导致永远重跑）。

---

## 6. 构建缓存

> 🎯 **增量是「本次构建省」，缓存是「跨构建/跨机器省」**——远端缓存让 CI 与其他机器共享产物：

```text
构建缓存层次
├── 本地缓存（默认开启）：~/.gradle/caches/build-cache-1
├── 远程缓存（需配置）：企业级共享（同一仓库代码 → 任意机器复用产物）
└── 触发条件：Task 输入哈希一致（含依赖版本/源码/编译参数）

远程缓存配置（settings.gradle.kts）
buildCache {
    local { enabled = true }
    remote(HttpBuildCache) {
        url = uri("https://build-cache.example.com/cache/")
        allowUntrustedServer = false
    }
}

使用场景
├── CI 构建结果 → 开发者本地复用（首次构建速度提升明显）
├── 不同 CI Job 复用（多平台测试）
└── 需注意：缓存命中 = 复用旧编译产物 → 依赖完全可复现（锁依赖版本）

对比 Maven 4 现状
├── Maven 4 构建缓存处于起步阶段
├── Gradle 远程缓存成熟（2018 年即引入）
└── 多机器团队：Gradle 缓存优势明显
```

---

## 7. 依赖管理

> 🎯 **Gradle 兼容 Maven 仓库生态**（从 Maven Central/私有 Nexus 拉包），但配置更精细：

```kotlin
dependencies {
    // 配置组（Configuration）：类似 Maven scope 但更细分
    implementation("org.slf4j:slf4j-api:2.0.16")       // 编译+运行，传递
    compileOnly("org.projectlombok:lombok:1.18.34")     // 编译期（≈provided）
    runtimeOnly("com.mysql:mysql-connector-j:9.1.0")    // 运行期（≈runtime）
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")  // 测试
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    annotationProcessor("org.projectlombok:lombok:1.18.34")  // 注解处理
}

// 依赖约束（强制版本：类似 dependencyManagement）
dependencies {
    constraints {
        implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    }
}
```

**Gradle 冲突策略（与 Maven 的差异）**：

| 维度 | Maven | Gradle |
|------|-------|--------|
| 裁决规则 | 就近原则 | **最高版本**（默认） |
| 冲突提示 | 静默 | 报告冲突路径 |
| 强制控制 | enforcer 插件 | resolutionStrategy / constraints |
| 排查命令 | `mvn dependency:tree` | `gradle dependencies` / `dependencyInsight` |

```kotlin
// 强制版本 + 排除
configurations.all {
    resolutionStrategy {
        force("com.fasterxml.jackson.core:jackson-databind:2.17.2")  // 强制
    }
    exclude(group = "commons-logging", module = "commons-logging")   // 排除
}

// 排查命令
./gradlew dependencies                       // 全量依赖树
./gradlew dependencyInsight --dependency jackson-databind  // 查某包的引入路径
```

> ⚠️ **最高版本策略的风险**：默认选最高版本可能导致「新版破坏旧 API」——生产建议用 Version Catalog + constraints 显式锁定关键版本，而不是依赖默认策略。

---

## 8. 多模块与 Version Catalog

> 🎯 **Version Catalog（版本目录）是 Gradle 9 的官方推荐**——统一管理版本号，Maven 的「dependencyManagement + 父 POM」在 Gradle 的对应物：

```toml
# gradle/libs.versions.toml（Version Catalog 文件）
[versions]
spring-boot = "3.4.0"
jackson = "2.17.2"
junit = "5.11.0"

[libraries]
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web", version.ref = "spring-boot" }
jackson-databind = { module = "com.fasterxml.jackson.core:jackson-databind", version.ref = "jackson" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
```

```kotlin
// 使用目录（settings.gradle.kts 自动加载 libs.versions.toml）
dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.databind)
    testImplementation(libs.junit.jupiter)
}
```

**多模块结构**：

```text
项目根
├── settings.gradle.kts（模块注册 + 目录引用）
├── gradle/libs.versions.toml（版本目录）
├── common/（build.gradle.kts）
├── order-service/
└── user-service/

# settings.gradle.kts
rootProject.name = "my-project"
include("common", "order-service", "user-service")

# 子模块依赖兄弟模块（免版本号）
dependencies {
    implementation(project(":common"))
}
```

> 💡 **Version Catalog vs Maven 对比**：Catalog 是「文件级」统一（所有模块引用同一 toml），比 Maven 的 parent 继承更简洁、IDE 支持更好（跳转/补全）。

---

## 9. 性能调优与常见坑

**性能调优三板斧**：

```text
① 配置阶段瘦身
├── 惰性注册 tasks.register（替代 tasks.create）
├── 耗时逻辑移入 doLast/doFirst
└── 避免配置阶段网络/文件 IO

② 增量 + 缓存
├── 自定义 Task 正确声明 inputs/outputs
├── 开启远程缓存（settings 配置 HttpBuildCache）
└── 看构建报告：./gradlew build --profile

③ 并行与守护进程
├── 多模块并行：org.gradle.parallel=true（gradle.properties）
├── 构建守护进程：默认开启（复用 JVM，避免重复启动）
└── 配置缓存：org.gradle.configuration-cache=true（Gradle 7+ 特性）
```

**常见坑**：

| 坑 | 现象 | 对策 |
|----|------|------|
| 依赖下载慢 | 首次构建卡很久 | 配阿里云 maven 镜像 |
| Gradle 版本不一致 | 团队构建结果不同 | gradle-wrapper（推荐）+ 锁定版本 |
| Kotlin DSL 编译慢 | 每次改脚本重编译 | 保持脚本简洁；必要时降 Groovy |
| 配置阶段耗时 | --profile 显示 configuration 高 | 惰性注册 + 移出耗时逻辑 |
| 增量失效 | 每次全量重跑 | 检查 inputs/outputs 声明 |
| 缓存命中率低 | 输入含时间戳等不稳定值 | 移除不确定输入 |
| 内存不足 | 大项目 OOM | gradle.properties 调 org.gradle.jvmargs |

```properties
# gradle.properties（性能基线配置）
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
```

---

## 10. 核心要点

> 🎯 **核心要点**：
> 1. **构建 = 代码**：Task 图驱动，可编程性远超 Maven 的声明式
> 2. **Task 三要素**：inputs/outputs/action——正确声明才有增量
> 3. **两阶段**：配置阶段建图（不干活）、执行阶段跑 Task（干正事）
> 4. **增量 + 缓存**：UP-TO-DATE 跳过 + 远程缓存跨机器复用
> 5. **依赖管理**：兼容 Maven 仓库，最高版本策略 + constraints 显式锁定
> 6. **Version Catalog**：libs.versions.toml 统一版本（Gradle 9 推荐）
> 7. **性能三板斧**：惰性注册 + 增量声明 + 并行/配置缓存
> 8. **Gradle 9.3（2026）**：IDEA 全面支持、跨平台构建修复——与 Maven 4 的选型见 [[05-Maven与Gradle选型对比]]

---

**下一模块**：[05-Maven与Gradle选型对比](05-Maven与Gradle选型对比.md) | **返回总览**：[00-各种包管理器知识体系总览](00-各种包管理器知识体系总览.md)
