# 15 - Gradle 构建 Task 与依赖管理

> 🎯 Task 是 Gradle 构建的基本工作单元，依赖管理是日常开发的核心操作 — 掌握 Task 定义与增量构建、依赖解析与冲突处理，才能真正驾驭 Gradle

---

## 目录

1. [Task 定义与生命周期](#1-task-定义与生命周期)
2. [Task 依赖与排序](#2-task-依赖与排序)
3. [增量构建与输入输出](#3-增量构建与输入输出)
4. [依赖管理深入](#4-依赖管理深入)
5. [依赖冲突解决](#5-依赖冲突解决)
6. [Version Catalog（统一版本管理）](#6-version-catalog统一版本管理)
7. [Gradle Daemon 与构建缓存](#7-gradle-daemon-与构建缓存)

---

## 1. Task 定义与生命周期

### 1.1 Task 的两种定义方式

```kotlin
// ⭐ 方式1：register（推荐，懒加载 — 仅在需要时配置）
tasks.register("hello") {
    doLast { println("Hello, Gradle!") }
}

// 方式2：create（立即配置 — 即使不执行也会被配置）
tasks.create("hello2") {
    doLast { println("Hello2!") }
}
```

### 1.2 Task 的 Action：doFirst / doLast

```kotlin
tasks.register("deploy") {
    // 配置阶段执行（总是执行）
    description = "部署应用到服务器"

    doFirst {
        println("1. 停止旧服务...")
    }

    doLast {
        println("3. 启动新服务...")
    }

    doLast {
        println("4. 健康检查...")
    }
}

// 外部追加 Action
tasks.named("deploy") {
    doFirst { println("0. 备份当前版本...") }
}
// 执行顺序：0 → 1 → 3 → 4
```

### 1.3 自定义 Task 类

```kotlin
// 方式1：在 build.gradle.kts 中定义
abstract class GenerateBuildInfo : DefaultTask() {

    @get:Input
    abstract val appVersion: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val content = """
            |app.version=${appVersion.get()}
            |build.time=${java.time.Instant.now()}
        """.trimMargin()
        outputFile.get().asFile.writeText(content)
        println("Generated: ${outputFile.get().asFile}")
    }
}

tasks.register<GenerateBuildInfo>("generateBuildInfo") {
    appVersion.set(project.version.toString())
    outputFile.set(layout.buildDirectory.file("build-info.properties"))
}
```

---

## 2. Task 依赖与排序

### 2.1 显式依赖

```kotlin
// dependsOn：A 执行前必须先执行 B
tasks.register("compile") { doLast { println("编译...") } }
tasks.register("packageApp") {
    dependsOn("compile")
    doLast { println("打包...") }
}

// finalizedBy：A 执行后必定执行 B
tasks.register("deploy") {
    finalizedBy("notify")
    doLast { println("部署...") }
}
tasks.register("notify") { doLast { println("发送通知...") } }

// mustRunAfter：顺序约束但不强制依赖
tasks.register("taskA") { doLast { println("A") } }
tasks.register("taskB") {
    mustRunAfter("taskA")
    doLast { println("B") }
}
```

### 2.2 动态依赖

```kotlin
// 基于条件动态决定依赖
tasks.register("conditionalBuild") {
    doLast { println("条件构建") }
}

tasks.register("build") {
    if (project.hasProperty("skipTests")) {
        dependsOn("compile")
    } else {
        dependsOn("test", "compile")
    }
}
```

---

## 3. 增量构建与输入输出

> 💡 Gradle 增量构建的核心：Task 声明 `@Input` 和 `@Output`，Gradle 自动比较输入是否变化，跳过无变化 Task。

### 3.1 输入输出注解

| 注解 | 说明 | 示例 |
|------|------|------|
| `@Input` | 影响输出的输入值 | 版本号、配置参数 |
| `@InputFile` | 单个输入文件 | 配置文件 |
| `@InputFiles` | 输入文件集合 | 源码目录 |
| `@InputDirectory` | 输入目录 | `src/main/resources` |
| `@OutputFile` | 单个输出文件 | 生成的 JAR |
| `@OutputFiles` | 输出文件集合 | 多个 class 文件 |
| `@OutputDirectory` | 输出目录 | `build/classes` |
| `@Internal` | 不参与增量判断 | 日志/调试信息 |

```kotlin
abstract class ProcessResources : DefaultTask() {

    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:Input
    abstract val environment: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun process() {
        sourceDir.get().asFile.copyRecursively(outputDir.get().asFile, overwrite = true)
        // 替换配置文件中的占位符
        outputDir.get().file("application.yml").asFile
            .writeText(outputDir.get().file("application.yml").asFile
                .readText().replace("@env@", environment.get()))
    }
}
```

### 3.2 跳过 Task

```kotlin
// 条件跳过
tasks.register("heavyTask") {
    onlyIf { project.hasProperty("runHeavy") }
    doLast { println("执行耗时任务...") }
}

// 手动跳过
tasks.register("optional") {
    doLast { println("可选任务") }
}
// gradle build -x optional    ← 跳过 optional Task
```

---

## 4. 依赖管理深入

### 4.1 依赖声明语法

```kotlin
dependencies {
    // ═══ 基本语法 ═══
    implementation("group:artifact:version")

    // ═══ 完整语法 ═══
    implementation(group = "org.springframework.boot",
                   name = "spring-boot-starter-web",
                   version = "3.3.0")

    // ═══ 动态版本 ═══
    implementation("com.google.guava:guava:31.1-jre")      // 固定版本
    implementation("com.google.guava:guava:31.+")           // 31.x 最新
    implementation("com.google.guava:guava:latest.release") // 最新发布版

    // ═══ 文件依赖 ═══
    implementation(files("libs/custom.jar"))
    implementation(fileTree("libs") { include("*.jar") })
}
```

### 4.2 依赖传递控制

```kotlin
dependencies {
    // ═══ 排除传递依赖 ═══
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }

    // 全局排除（对所有依赖生效）
    configurations.all {
        exclude(group = "commons-logging", module = "commons-logging")
    }
}

// ═══ 强制版本（解决冲突） ═══
configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:32.1.3-jre")
        // 或：指定版本优先
        preferProjectModules()
    }
}
```

### 4.3 依赖报告

```bash
# 查看完整依赖树
./gradlew dependencies

# 查看特定配置的依赖树
./gradlew dependencies --configuration implementation

# 查看依赖冲突
./gradlew dependencies --configuration compileClasspath | grep -A 2 "FAILED\|conflict"

# 生成 HTML 依赖报告
./gradlew htmlDependencyReport
```

---

## 5. 依赖冲突解决

### 5.1 依赖冲突场景

```
项目依赖：
├── library-A:1.0 → 依赖 guava:30.0-jre
└── library-B:2.0 → 依赖 guava:32.0-jre
                    → 冲突！Gradle 默认选最高版本 32.0
```

### 5.2 解决策略

```kotlin
configurations.all {
    resolutionStrategy {
        // 策略1：指定默认版本
        force("com.google.guava:guava:32.1.3-jre")

        // 策略2：版本冲突时失败（严格模式，推荐 CI 中启用）
        failOnVersionConflict()

        // 策略3：缓存动态版本的时间
        cacheDynamicVersionsFor(10, "minutes")
        cacheChangingModulesFor(10, "minutes")
    }
}

// ═══ 严格版本声明 ═══
dependencies {
    implementation("com.google.guava:guava") {
        version {
            strictly("32.1.3-jre")   // 严格：只允许这个版本
            prefer("32.0-jre")        // 偏好：优先使用但不强制
            require("[30.0, 33.0[")   // 范围：30.0 ≤ ver < 33.0
        }
    }
}
```

---

## 6. Version Catalog（统一版本管理）

> 💡 Gradle 7.4+ 推荐使用 Version Catalog（`libs.versions.toml`）替代 `dependencyManagement`，提供类型安全的依赖声明。

### 6.1 配置文件

```toml
# gradle/libs.versions.toml
[versions]
spring-boot = "3.3.0"
lombok = "1.18.32"
guava = "32.1.3-jre"

[libraries]
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-boot-starter-test = { module = "org.springframework.boot:spring-boot-starter-test" }
lombok = { module = "org.projectlombok:lombok", version.ref = "lombok" }
guava = { module = "com.google.guava:guava", version.ref = "guava" }
mysql-connector = { module = "com.mysql:mysql-connector-j", version = "8.0.33" }

[bundles]
spring-web = ["spring-boot-starter-web", "spring-boot-starter-validation"]
testing = ["spring-boot-starter-test"]

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
spring-dependency-management = { id = "io.spring.dependency-management", version = "1.1.5" }
```

### 6.2 在 build.gradle.kts 中使用

```kotlin
// build.gradle.kts
plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // 单个依赖
    implementation(libs.spring.boot.starter.web)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    runtimeOnly(libs.mysql.connector)

    // Bundle（一组依赖）
    implementation(libs.bundles.spring.web)

    // 测试
    testImplementation(libs.bundles.testing)
}
```

---

## 7. Gradle Daemon 与构建缓存

### 7.1 Gradle Daemon

```bash
# Daemon 是后台进程，复用 JVM 避免冷启动
./gradlew build --daemon        # 启用 Daemon（默认）

# 查看 Daemon 状态
./gradlew --status

# 停止所有 Daemon
./gradlew --stop
```

### 7.2 构建缓存优化

```properties
# gradle.properties（项目级性能优化）
org.gradle.daemon=true                          # 启用 Daemon
org.gradle.parallel=true                        # 并行构建多模块
org.gradle.caching=true                         # 启用构建缓存
org.gradle.configuration-cache=true             # 启用配置缓存（Gradle 8+）
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m  # Daemon JVM 参数
```

```bash
# ═══ 性能对比 ═══
# 首次构建（无缓存）
./gradlew clean build          # ~30s

# 增量构建（仅修改一个文件）
./gradlew build                # ~3s（仅重新编译修改的类）

# 构建缓存命中（切换分支后切回来）
./gradlew build                # ~1s（FROM-CACHE）
```

### 7.3 Gradle Build Scan（构建分析）

```bash
# 生成构建扫描报告，分析性能瓶颈
./gradlew build --scan

# 或永久启用
# gradle.properties:
# org.gradle.unsafe.build-scan=true
```

> 🎯 **总结**：掌握 Task 的输入输出声明是实现增量构建的关键；Version Catalog 是统一依赖版本的现代方案；合理配置 Daemon + 并行 + 缓存可显著提升大型项目的构建速度。
