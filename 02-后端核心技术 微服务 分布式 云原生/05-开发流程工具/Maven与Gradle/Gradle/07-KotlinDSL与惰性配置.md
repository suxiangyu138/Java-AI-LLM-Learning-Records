# 07 - Kotlin DSL 与惰性配置

> 🎯 Kotlin DSL（9.0 起默认）带来类型安全、自动补全与可靠重构；惰性配置（Provider/Property API）是 Gradle 配置阶段性能的核心——"配置避免"让只配置被请求的任务。理解这两者，构建脚本从"能跑"到"专业"

---

## 目录

1. [Kotlin DSL 的价值：类型安全与补全](#1-kotlin-dsl-的价值类型安全与补全)
2. [脚本结构：build.gradle.kts 解剖](#2-脚本结构buildgradlekts-解剖)
3. [Provider 与惰性配置](#3-provider-与惰性配置)
4. [Property：扩展属性的惰性化](#4-property扩展属性的惰性化)
5. [配置避免：register vs create](#5-配置避免register-vs-create)
6. [gradle.properties 与脚本性能](#6-gradleproperties-与脚本性能)
7. [Groovy 存量迁移](#7-groovy-存量迁移)
8. [常见 Kotlin DSL 陷阱](#8-常见-kotlin-dsl-陷阱)
9. [练习](#9-练习)

---

## 1. Kotlin DSL 的价值：类型安全与补全

Kotlin DSL（.gradle.kts）是 Gradle 9.0 起的默认构建语言，价值三件套：

- **类型安全**：`dependencies { implementation("...") }` 的配置名、参数类型在编译期检查——拼错配置名/参数类型直接编译失败，而非运行时报错
- **自动补全**：IDE 内补全配置块、任务名、依赖坐标（配合版本目录的类型安全访问器体验最佳）
- **可靠重构**：重命名任务/扩展/配置时 IDE 全局重构，Groovy 动态脚本做不到

```kotlin
// Kotlin DSL：编译期检查
tasks.register<Test>("unitTest") {
    useJUnitPlatform()                    // 补全 + 类型检查
    testLogging { events("passed", "failed") }
}
```

代价与边界：Kotlin DSL 编译构建脚本有初始成本（首次构建更慢，被 daemon 缓存抵消）；复杂脚本需要 Kotlin 语法知识（lambda、扩展函数、委托）。

## 2. 脚本结构：build.gradle.kts 解剖

```kotlin
plugins {                                // ① 插件声明（最前）
    java
    id("org.springframework.boot") version "3.4.0"
}

group = "com.example"                    // ② 项目坐标
version = "1.0.0"

repositories { mavenCentral() }          // ③ 仓库

dependencies {                           // ④ 依赖（配置块）
    implementation("org.springframework.boot:spring-boot-starter-web")
}

java { toolchain { ... } }               // ⑤ 扩展配置

tasks.test {                             // ⑥ 任务配置（named 访问）
    useJUnitPlatform()
}

val myExt by extra { "value" }           // ⑦ extra 自定义属性
```

Kotlin DSL 的脚本本质：每个块都是对扩展（Extension）的配置——`java {}` 配置 JavaPluginExtension、`tasks.test {}` 配置 Test 任务。**理解"块 = 扩展配置"就理解了一切**：看文档找"扩展名"即可。

## 3. Provider 与惰性配置

惰性配置（Lazy Configuration）是 Gradle 配置阶段的性能核心：

```kotlin
val message = providers.provider {
    // 这个 lambda 只在被消费时执行（真实工作延迟）
    file("message.txt").readText()
}

tasks.register("printMessage") {
    doLast { println(message.get()) }    // 执行阶段才真正读取
}
```

Provider API 三件套：**Provider**（延迟计算的值）、**Property**（可设置的 Provider）、**providers 工厂**（provider {}、provider { file.contentsText } 等）。核心语义：**声明与计算分离**——脚本里声明依赖关系，实际计算推迟到消费时刻。

价值：配置阶段只建依赖图不做真实工作 → 配置变快、配置缓存可序列化（存的是"如何计算"而不是计算结果）、跨配置复用。**惰性配置是配置缓存的前提**——非惰性的脚本（配置阶段就读文件/算哈希）无法被配置缓存复用。

## 4. Property：扩展属性的惰性化

自定义扩展的属性必须用 Property 类型才能惰性：

```kotlin
abstract class GreetingExtension {
    abstract val name: Property<String>        // 抽象属性：Gradle 自动实现
    abstract val target: Property<String>
}

class GreetingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("greeting", GreetingExtension::class.java)
        ext.name.convention("world")            // 默认值（惰性）
        ext.target.convention(project.layout.buildDirectory)

        project.tasks.register("greet") {
            doLast {
                println("Hello ${ext.name.get()}!")
            }
        }
    }
}
```

Property 规则：**抽象 Property 由 Gradle 自动生成实现**（不用手动 new）；`convention()` 设默认值（用户可覆盖）；`get()` 消费、`set()` 覆盖、`finalizeValueOnRead()` 冻结。**extension 里不要用普通 String/File 字段**——那会让属性在配置阶段求值，破坏惰性。

## 5. 配置避免：register vs create

配置避免（Configuration Avoidance）的 API 选择：

```kotlin
// ❌ create：立即创建并配置任务（配置阶段就执行配置块）
tasks.create("heavy") { doLast { ... } }

// ✅ register：懒注册，配置延迟到任务被请求时
tasks.register("heavy") { doLast { ... } }

// 条件配置：只有执行相关任务时才配置
tasks.named("build") { dependsOn("heavy") }
```

原则：**register 优先于 create、named 优先于 getByName、configureEach 优先于 all**。收益：不执行的任务不配置（`gradle help` 不配置 build 链的任务）；配置缓存命中率提升；大型项目配置时间显著下降。**存量脚本的 create → register 改造是 9.x 性能优化第一步**。

## 6. gradle.properties 与脚本性能

```properties
# gradle.properties（项目根，提交到 Git）
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.parallel=true
kotlin.daemon.jvmargs=-Xmx2g
```

脚本性能的三板斧：**开启配置缓存**（9.6 属性精确追踪后命中率大增）；**并行构建**（多模块并行，CPU 核数以内收益明显）；**避免配置阶段 IO**（文件读取、网络请求都搬进 Provider 或任务执行阶段）。诊断入口：`./gradlew --scan` 生成 Build Scan，配置/执行耗时分段可见；`--profile` 输出构建阶段报告。

## 7. Groovy 存量迁移

Groovy 脚本（.gradle）迁移到 Kotlin DSL（.gradle.kts）的路径：

1. **逐步迁移**：buildSrc 或约定插件先转 Kotlin（模块脚本保留 Groovy），Kotlin 与 Groovy 脚本可共存
2. **自动转换工具**：`gradle init` 支持 Groovy → Kotlin 转换（复杂脚本需手工修）
3. **高频差异**：`tasks.withType(Test)` → `tasks.withType<Test>()`；`project.ext` → `extra`；Groovy 动态属性访问 → Kotlin 显式类型
4. **9.6 提醒**：Groovy 隐式查找父项目属性已废弃（10 移除），存量脚本要显式引用

迁移原则：**不迁移就不迁移**（能跑的 Groovy 继续跑），但要**新代码不再写 Groovy**——混合状态是过渡常态，约定插件层先转 Kotlin 收益最大。

## 8. 常见 Kotlin DSL 陷阱

| 陷阱 | 现象 | 解法 |
|------|------|------|
| 配置名拼写 | `implemntation` 编译报错 | 类型安全下编译期暴露，看错误提示 |
| 类型不匹配 | Groovy 动态类型代码迁移后报错 | 显式类型转换（`as String`、`toInt()`） |
| 块内 this 混淆 | lambda 里的 this 指向错误对象 | 用 `this@xxx` 或命名参数 |
| 字符串模板 | `"$"` 误当模板 | Kotlin 里 `$` 需要转义（`\$`） |
| 惰性缺失 | 扩展属性用普通类型（String 而非 Property） | 抽象 Property + convention |
| 配置阶段 IO | 脚本里读文件/网络 | 搬进 Provider 或任务体 |

Kotlin DSL 的错误信息比 Groovy 友好（编译期暴露 + IDE 高亮），但 Kotlin 语法本身（lambda/委托/泛型）是学习成本——**先用标准模板改，再理解语法**，不要从零写。

一个判断你"是否真的会用 Kotlin DSL"的自测：能否解释 `tasks.named("build") { dependsOn("lint") }` 与 `tasks.named("build") { doLast { ... } }` 里 `named` 的返回类型差异（TaskProvider vs Task），以及为什么 `dependsOn` 接受字符串而 `doLast` 需要配置块——能讲清"提供者（Provider）与配置块"两套语义，说明真正理解了惰性模型；只会抄模板的写不出这句话。

再补一个实战细节：**`tasks.named("build") { ... }` 的配置块在什么时机执行**——不是脚本执行时立即执行，而是构建图计算后、任务执行前（若任务被请求）。这意味着块内引用的属性（如 `project.version`）在配置块执行时才取值——想"读取配置阶段的最终值"就得依赖这个时机，反过来，想在块外读任务配置结果（如 `tasks.test.get().maxHeapSize`）就会拿到默认值而非配置后的值。理解执行时机，是排 Kotlin DSL 疑难 bug 的关键。

## 9. 练习

1. Kotlin DSL 的三件套价值？"块 = 扩展配置"怎么理解？
2. Provider 的核心语义？为什么惰性是配置缓存的前提？
3. Property 的规则？convention 与 set 的区别？
4. register vs create 的差异？配置避免的 API 选择原则？
5. 脚本性能三板斧？Groovy 迁移的逐步路径？

---

> 🎯 **核心要点**：Kotlin DSL = 类型安全 + 补全 + 重构；惰性配置 = Provider/Property（声明与计算分离），是配置缓存的前提；配置避免 = register/named/configureEach 优先。性能三板斧：配置缓存 + 并行 + 避免配置阶段 IO。Groovy 存量不强制迁移，新代码一律 Kotlin DSL。

**下一模块**：[08-构建缓存与性能优化](08-构建缓存与性能优化.md) / **返回总览**：[00-总览](00-Gradle知识体系总览.md)
