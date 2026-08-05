# 16 - Gradle 多模块构建与插件开发

> 🎯 多模块构建是 Gradle 的强项，自定义插件封装复用逻辑 — 掌握这两项能力，才能在大中型项目中将构建效率发挥到极致

---

## 目录

1. [多模块项目结构](#1-多模块项目结构)
2. [子模块依赖与共享配置](#2-子模块依赖与共享配置)
3. [复合构建 (Composite Build)](#3-复合构建-composite-build)
4. [自定义 Gradle 插件](#4-自定义-gradle-插件)
5. [发布插件到仓库](#5-发布插件到仓库)
6. [性能优化实战](#6-性能优化实战)
7. [从 Maven 迁移到 Gradle](#7-从-maven-迁移到-gradle)

---

## 1. 多模块项目结构

### 1.1 典型微服务多模块

```
my-platform/
├── settings.gradle.kts              # 声明所有子模块
├── build.gradle.kts                 # 根项目：公共配置
├── gradle/
│   ├── libs.versions.toml           # Version Catalog
│   └── wrapper/
├── buildSrc/                        # 构建逻辑复用（可选）
│   └── src/main/kotlin/
│       └── my-platform.java-conventions.gradle.kts
│
├── common/                          # 公共模块（API、工具类、实体）
│   ├── build.gradle.kts
│   └── src/main/java/...
├── service-user/                    # 用户服务
│   ├── build.gradle.kts
│   └── src/main/java/...
├── service-order/                   # 订单服务
│   ├── build.gradle.kts
│   └── src/main/java/...
└── gateway/                         # 网关（Spring Cloud Gateway）
    ├── build.gradle.kts
    └── src/main/java/...
```

### 1.2 settings.gradle.kts

```kotlin
// settings.gradle.kts
rootProject.name = "my-platform"

include(
    "common",
    "service-user",
    "service-order",
    "gateway"
)

// 可选：自定义子模块目录名（如目录不同）
// include(":service-user")
// project(":service-user").projectDir = file("services/user-service")
```

### 1.3 根项目 build.gradle.kts（公共配置）

```kotlin
// 根 build.gradle.kts — 对所有子项目生效的公共配置
plugins {
    java
    id("org.springframework.boot") version "3.3.0" apply false
    id("io.spring.dependency-management") version "1.1.5" apply false
}

// ⭐ allprojects: 根项目 + 所有子项目
allprojects {
    group = "com.example"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// ⭐ subprojects: 仅子项目（不含根）
subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
```

---

## 2. 子模块依赖与共享配置

### 2.1 模块间依赖

```kotlin
// service-user/build.gradle.kts
plugins {
    id("org.springframework.boot")          // 可启动的应用模块
    id("io.spring.dependency-management")
}

dependencies {
    // ⭐ 模块间依赖（使用 project()）
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}

// common/build.gradle.kts（纯库模块，不需要 boot 插件）
plugins {
    java
}

dependencies {
    // 公共模块声明 api，让消费者可以传递依赖
    api("com.google.guava:guava:32.1.3-jre")
    implementation("org.slf4j:slf4j-api")
}
```

### 2.2 buildSrc 共享构建逻辑

```kotlin
// buildSrc/build.gradle.kts
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

// buildSrc/src/main/kotlin/my-platform.java-conventions.gradle.kts
plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### 2.3 共享插件版本（Version Catalog）

```toml
# gradle/libs.versions.toml
[versions]
spring-boot = "3.3.0"

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

```kotlin
// 子模块中引用
plugins {
    alias(libs.plugins.spring.boot)
}
```

---

## 3. 复合构建 (Composite Build)

> 💡 复合构建允许在构建时临时引入外部项目，无需发布到仓库即可调试依赖。

```kotlin
// settings.gradle.kts
rootProject.name = "my-app"

// ⭐ includeBuild：引入外部 Gradle 项目参与本次构建
includeBuild("../my-common-lib") {
    dependencySubstitution {
        substitute(module("com.example:my-common")).using(project(":"))
    }
}
```

适用场景：
- 同时开发应用和公共库，不需要先 `publishToMavenLocal`
- 多个独立 Git 仓库的项目联合调试
- 插件开发时的即时测试

---

## 4. 自定义 Gradle 插件

### 4.1 独立插件项目结构

```
my-plugin/
├── build.gradle.kts
├── settings.gradle.kts
└── src/main/kotlin/com/example/MyPlugin.kt
```

```kotlin
// build.gradle.kts
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        register("myPlugin") {
            id = "com.example.my-plugin"
            implementationClass = "com.example.MyPlugin"
        }
    }
}
```

### 4.2 插件实现

```kotlin
// src/main/kotlin/com/example/MyPlugin.kt
package com.example

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 1. 注册扩展属性（DSL 配置块）
        val extension = project.extensions.create("myConfig", MyPluginExtension::class.java)

        // 2. 注册 Task
        project.tasks.register("generateConfig", GenerateConfigTask::class.java) {
            group = "my-plugin"
            description = "生成配置文件"
            environment.set(extension.environment)
        }

        // 3. 修改已有配置
        project.tasks.withType(Test::class.java) {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
    }
}

// 扩展属性（让用户通过 DSL 配置插件）
open class MyPluginExtension {
    var environment: String = "dev"
    var outputDir: String = "build/generated"
}

// Task 实现
abstract class GenerateConfigTask : DefaultTask() {
    @get:Input
    abstract val environment: Property<String>

    @TaskAction
    fun generate() {
        val configDir = project.layout.buildDirectory.dir("generated").get().asFile
        configDir.mkdirs()
        configDir.resolve("app-config.yml").writeText("""
            |environment: ${environment.get()}
            |timestamp: ${System.currentTimeMillis()}
        """.trimMargin())
        println("Generated config for environment: ${environment.get()}")
    }
}
```

### 4.3 使用自定义插件

```kotlin
// 应用项目 build.gradle.kts
plugins {
    id("com.example.my-plugin") version "1.0.0"
}

// 配置插件扩展
myConfig {
    environment = "prod"
    outputDir = "custom/output"
}
```

---

## 5. 发布插件到仓库

```kotlin
// build.gradle.kts
plugins {
    `maven-publish`
    `java-gradle-plugin`
}

group = "com.example"
version = "1.0.0"

publishing {
    repositories {
        maven {
            name = "nexusReleases"
            url = uri("https://nexus.example.com/repository/maven-releases/")
            credentials {
                username = project.findProperty("nexusUsername") as String?
                password = project.findProperty("nexusPassword") as String?
            }
        }
    }
}
```

```bash
# 发布
./gradlew publish
```

---

## 6. 性能优化实战

### 6.1 优化配置 Checklist

```properties
# gradle.properties
# ═══ 基础性能 ═══
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true          # Gradle 8+ 配置缓存

# ═══ JVM 调优 ═══
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError

# ═══ 增量编译 ═══
org.gradle.unsafe.configuration-cache=true   # 跳过重复的配置阶段
```

### 6.2 模块化并行

```bash
# 指定并行 Worker 数
./gradlew build --parallel --max-workers=4

# 查看构建详情
./gradlew build --profile        # 生成 build/reports/profile 报告
./gradlew build --scan            # 生成 Gradle Build Scan
```

### 6.3 常用优化技巧

| 优化 | 说明 | 效果 |
|------|------|:---:|
| `--parallel` | 多模块并行构建 | ⭐⭐⭐ |
| `--build-cache` | 跨构建共享 Task 输出 | ⭐⭐⭐ |
| `configuration-cache` | 跳过重复配置阶段 | ⭐⭐⭐ |
| `--daemon` | 复用 JVM 进程 | ⭐⭐ |
| `api` vs `implementation` | 减少不必要的重新编译 | ⭐⭐ |
| Task 增量声明 | 声明 @Input/@Output | ⭐⭐⭐ |

---

## 7. 从 Maven 迁移到 Gradle

### 7.1 自动迁移

```bash
# 在 Maven 项目根目录执行
gradle init
# 选择：2 (application) → 3 (Java) → 1 (Groovy/Kotlin DSL) → yes (使用新 API)
# Gradle 自动解析 pom.xml 生成 build.gradle
```

### 7.2 概念映射速查

| Maven | Gradle |
|-------|--------|
| `pom.xml` | `build.gradle(.kts)` |
| `<groupId>` | `group` |
| `<artifactId>` | `name` (默认取目录名) |
| `<version>` | `version` |
| `<parent>` | —（无直接等价，用 `dependencyManagement` BOM） |
| `<dependencyManagement>` | `platform()` 或 `libs.versions.toml` |
| `<dependencies>` | `dependencies {}` |
| `<scope>compile</scope>` | `implementation()` |
| `<scope>provided</scope>` | `compileOnly()` |
| `<scope>runtime</scope>` | `runtimeOnly()` |
| `<scope>test</scope>` | `testImplementation()` |
| `<modules>` | `include()` in `settings.gradle.kts` |
| `mvn clean install` | `./gradlew build publishToMavenLocal` |
| `mvn dependency:tree` | `./gradlew dependencies` |

> 🎯 **迁移建议**：使用 `gradle init` 自动生成初始脚本，然后根据项目特点手动调优。多模块项目重点调整模块间依赖和 Version Catalog 配置。
