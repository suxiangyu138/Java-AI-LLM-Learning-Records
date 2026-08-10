# 03 - 构建生命周期与 Task

> 🎯 构建 = 任务 DAG：初始化 → 配置 → 执行三阶段。Task 有类型、依赖、inputs/outputs 与 up-to-date 检查——增量构建的底层机制。9.5 的 Task provenance 让"任务从哪来"可追溯

---

## 目录

1. [生命周期三阶段](#1-生命周期三阶段)
2. [Task 的定义与类型](#2-task-的定义与类型)
3. [任务依赖：DAG 的构建](#3-任务依赖dag-的构建)
4. [增量构建：inputs/outputs 与 up-to-date](#4-增量构建inputsoutputs-与-up-to-date)
5. [Task provenance：任务溯源](#5-task-provenance任务溯源)
6. [生命周期钩子与任务挂接](#6-生命周期钩子与任务挂接)
7. [常见内置任务速查](#7-常见内置任务速查)
8. [练习](#8-练习)

---

## 1. 生命周期三阶段

Gradle 每次构建执行三个阶段：

```text
① 初始化（Initialization）：定位 settings 文件，创建项目层级
② 配置（Configuration）：执行构建脚本，构建任务图（注册任务、声明依赖）
③ 执行（Execution）：按 DAG 拓扑序执行任务
```

两个关键推论：

- **配置阶段执行构建脚本**：脚本里的代码（包括任务定义块）在配置阶段运行，任务体（doLast 等）在执行阶段运行——"任务定义"与"任务行为"分离
- **配置成本是大头**：大型项目配置阶段可能占构建时间 50%+，所以配置缓存（08 篇）的价值巨大；"配置避免"（只配置被请求的任务）是 9.x 的优化方向

调试入口：`./gradlew help --task <name>` 查看任务详情；`./gradlew tasks` 列出可用任务。

## 2. Task 的定义与类型

```kotlin
// 简单任务（DefaultTask：无输入输出，仅执行动作）
tasks.register("hello") {
    doLast { println("Hello Gradle!") }
}

// 类型化任务（继承 Task 类型，自带 inputs/outputs 语义）
tasks.register<Copy>("copyResources") {
    from("src/main/resources")
    into(layout.buildDirectory.dir("resources"))
}
```

两种定义方式：**`register`（懒注册）**——任务配置延迟到需要时执行，配置避免的关键 API，9.x 推荐；**`create`**——立即创建配置，旧风格。类型化任务（Copy/Delete/Jar/Test 等）比默认任务更优：自带增量检查语义。

**任务命名**：kebab-case（`copy-resources`），避免与内置任务重名；任务可以有别名（`alias`）与描述（`description`）。

## 3. 任务依赖：DAG 的构建

```kotlin
tasks.register("deploy") {
    dependsOn("test", "jar")          // 声明依赖
    doLast { println("Deploying...") }
}

// 或按类型引用
tasks.register("deploy") {
    dependsOn(tasks.test, tasks.jar)
}
```

依赖语义：执行 deploy 前先执行 test 与 jar（按拓扑序）；`mustRunAfter`/`shouldRunAfter` 表达排序而非依赖（两者无依赖关系但需要顺序）。**DAG 的威力**：`./gradlew deploy` 自动拉入整条依赖链；`./gradlew test` 只跑 test 链——不执行无关任务。循环依赖（A 依赖 B、B 依赖 A）在配置阶段直接报错。

## 4. 增量构建：inputs/outputs 与 up-to-date

增量构建是 Gradle 性能的根基：任务声明 inputs 与 outputs，下次构建时对比哈希，未变化的任务标记 **UP-TO-DATE** 跳过执行。

```kotlin
tasks.register<JavaCompile>("compileJava") {
    source = fileTree("src/main/java")        // inputs
    destinationDirectory = layout.buildDirectory.dir("classes/java/main")  // outputs
}
```

```text
执行状态：
├── UP-TO-DATE：输入输出未变，跳过
├── FROM-CACHE：命中构建缓存，从缓存还原输出
├── NO-SOURCE：无输入源，跳过
└── 执行：输入变了，真正执行
```

增量构建的三条纪律：**正确声明 inputs/outputs**（漏声明 → 结果过期却不重跑）；**inputs 不要用绝对路径**（换机器/CI 就失效）；**任务不要写未声明的文件**（污染输出破坏 up-to-date）。Java 增量编译：9.x 下源码或 classpath 变化只重编译受影响的类。

## 5. Task provenance：任务溯源

Gradle 9.5 引入的任务溯源（Task provenance）：**任务失败时，错误信息标明该任务由谁注册**——构建脚本、settings 脚本还是某个插件。

```text
失败信息示例：
> Task :app:compileJava FAILED
  Task ':app:compileJava' is registered from: 'org.jetbrains.kotlin.jvm' plugin
```

价值：大型项目的任务大多来自插件（java/kotlin/AGP），溯源直接告诉你"这个任务是谁带来的、去哪个插件文档查"，省去在构建脚本与插件里排查来源的时间。排查任务来源的另一个入口：`./gradlew help --task <name>` 的输出含注册来源。

## 6. 生命周期钩子与任务挂接

```kotlin
// 配置阶段钩子（执行构建脚本时触发）
plugins {
    java
}
// 所有项目配置完成后
allprojects {
    tasks.withType<Test>().configureEach {
        maxHeapSize = "1g"
    }
}

// 执行阶段钩子
tasks.named("build") {
    doFirst { println("build 开始前") }     // 动作前
    doLast { println("build 结束后") }      // 动作后
}
```

**挂接纪律**：优先 `tasks.named()`/`withType().configureEach()`（按名/按类型精确挂接），不要直接改插件任务内部——约定插件的复用思路（05 篇）就是"通过类型安全访问器配置别人的任务"。

三个容易写错的钩子用法：**`doFirst` 与 `doLast` 的顺序**——多个 doFirst 按注册逆序执行（后注册先执行），doLast 按注册顺序，混用时要理清动作顺序；**`onlyIf` 条件跳过**——`tasks.test { onlyIf { !project.hasProperty("skipTests") } }` 用条件控制任务执行而非写死在脚本里；**`finalizedBy` 清理链**——`test.finalizedBy("report")` 保证即使 test 失败报告任务也执行（失败场景的数据收集常用）。这三个钩子覆盖了"任务行为定制"的绝大多数需求，比复制插件任务重写更优雅。

任务模型与 Maven 的最后一个对照：**`clean` 是任务不是阶段**——`./gradlew clean build` 是两个任务按依赖（build dependsOn clean？不，Gradle 里 clean 与 build 默认无依赖）执行，顺序靠命令行传入；Maven 的 clean 是生命周期的一部分。这个差异常导致新人困惑"为什么 Gradle 的 clean build 有时不干净"——Gradle 里要保证清理顺序需显式 `clean.dependsOn` 或使用 `buildDir` 约定（clean 默认删 build 目录，插件任务输出都在其下，通常够用）。同理 `check` 与 `assemble` 是聚合任务（依赖多条链）而非阶段——`build` 依赖两者，这就是"构建骨架"与"Maven 生命周期"在概念上的对应与区别。理解这一点后，`gradle build` 与 `mvn package` 的行为差异就有了理论解释：Gradle 的 build 内容由插件决定（java 插件挂 test/check），你可以精确控制 build 的依赖链，Maven 的 package 是写死的阶段序列。

## 7. 常见内置任务速查

| 任务 | 插件 | 作用 |
|------|------|------|
| build | base | 完整构建（依赖 check + assemble） |
| check | base | 所有验证（test/lint） |
| test | java | 运行单元测试 |
| jar / bootJar | java / Spring Boot | 打包 |
| clean | base | 清理 build 目录 |
| dependencies | 内置 | 依赖树分析 |
| help --task | 内置 | 任务详情 |
| init / wrapper | 内置 | 初始化/生成 Wrapper |

## 8. 练习

1. 生命周期三阶段各做什么？"任务定义"与"任务行为"为什么分离？
2. register 与 create 的差异？类型化任务比默认任务好在哪里？
3. 任务依赖声明方式？mustRunAfter 与 dependsOn 的区别？
4. UP-TO-DATE / FROM-CACHE / NO-SOURCE 各是什么状态？
5. 增量构建的三条纪律？Java 增量编译的原理？

**任务调试实战**：`./gradlew tasks --all` 看全量任务与依赖；`./gradlew help --task test` 看 test 任务的类型、依赖与来源（9.5 provenance）；写一个自定义 Copy 任务并故意漏声明 output 目录，观察 up-to-date 检查的行为——**"故意做错再修正"是理解任务模型最快的方式**，比背概念记忆深十倍。条件允许时再用 `--dry-run` 模拟一次构建（只打印任务计划不执行），对照 DAG 理解"哪些任务会被拉入执行链"。

---

> 🎯 **核心要点**：生命周期 = 初始化/配置/执行三阶段，配置阶段跑脚本、执行阶段跑任务体（分离理解是排错前提）。Task = 定义（register 懒注册）+ 依赖（DAG）+ 增量（inputs/outputs → UP-TO-DATE）。三条纪律：正确声明 IO、不用绝对路径、不写未声明文件。9.5 Task provenance 让"任务从哪来"一目了然。

**下一模块**：[04-依赖管理](04-依赖管理.md) / **返回总览**：[00-总览](00-Gradle知识体系总览.md)
