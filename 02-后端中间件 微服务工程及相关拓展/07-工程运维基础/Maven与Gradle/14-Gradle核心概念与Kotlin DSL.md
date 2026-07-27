# 14 - Gradle 核心概念与 Kotlin DSL

> 🎯 Gradle 是 JVM 生态的现代化构建工具，基于 DAG 任务图和可编程 DSL，在构建性能和灵活性上全面超越 Maven — Spring Boot、Android 均已官方推荐

---

## 目录

1. [Gradle 概述与核心优势](#1-gradle-概述与核心优势)
2. [构建生命周期](#2-构建生命周期)
3. [核心三要素：Project / Task / Plugin](#3-核心三要素project--task--plugin)
4. [Kotlin DSL 完整项目结构](#4-kotlin-dsl-完整项目结构)
5. [依赖配置类型详解](#5-依赖配置类型详解)
6. [Gradle Wrapper](#6-gradle-wrapper)
7. [常用命令速查](#7-常用命令速查)
8. [与 Maven 关键差异](#8-与-maven-关键差异)
9. [Spring Boot + Gradle 实战模板](#9-spring-boot--gradle-实战模板)

---

## 1. Gradle 概述与核心优势

Gradle 是基于 Groovy/Kotlin DSL 的现代化 JVM 构建工具，核心特点：

| 特性 | 说明 | 对比 Maven |
|------|------|-----------|
| **构建模型** | 有向无环图（DAG）管理 Task 依赖 | 固定生命周期 |
| **增量构建** | 仅重新构建输入变化的 Task | 全量或部分 |
| **构建缓存** | 跨项目共享 Task 输出 | 无内置 |
| **并行执行** | 多模块并行构建 | `-T` 参数有限支持 |
| **DSL 灵活性** | Groovy/Kotlin 可编程脚本 | XML 声明式 |

- **官网**：https://gradle.org
- **Kotlin DSL 指南**：https://docs.gradle.org/current/userguide/kotlin_dsl.html
- **官方地位**：Android 官方构建系统，Spring Boot 推荐使用

---

## 2. 构建生命周期

```
┌─────────────────────────────────────────────────────────┐
│ 1. 初始化 (Initialization)                              │
│    解析 settings.gradle(.kts)，确定参与构建的 Project     │
│    → 创建 Project 对象，构建多项目树                      │
├─────────────────────────────────────────────────────────┤
│ 2. 配置 (Configuration)                                 │
│    解析每个 Project 的 build.gradle(.kts)                │
│    → 构建 Task 依赖图 (DAG)                             │
│    → ⚠️ 所有配置代码都在这阶段执行                         │
├─────────────────────────────────────────────────────────┤
│ 3. 执行 (Execution)                                     │
│    按 DAG 依赖顺序执行 Task                              │
│    → 增量构建：跳过输入未变化的 Task                      │
│    → 构建缓存：复用之前缓存的 Task 输出                   │
└─────────────────────────────────────────────────────────┘
```

> ⚠️ **关键理解**：配置阶段会执行 `build.gradle` 中除 Task Action 外的所有代码。`doLast`/`doFirst` 中的代码在**执行阶段**运行，其他代码在**配置阶段**运行。

---

## 3. 核心三要素：Project / Task / Plugin

| 概念 | 说明 | 类比 Maven |
|------|------|-----------|
| **Project** | 构建单元（JAR/WAR），对应一个 `build.gradle(.kts)` | `<artifactId>` 模块 |
| **Task** | 构建操作的原子单元（编译、打包、测试等） | Maven Goal |
| **Plugin** | 可复用的构建逻辑集合 | Maven Plugin |

```kotlin
// Task 定义示例
tasks.register("hello") {
    doLast {
        println("Hello, Gradle!")
    }
}
```

---

## 4. Kotlin DSL 完整项目结构

### 4.1 标准目录布局

```
my-app/
├── settings.gradle.kts          # 项目名 + 子模块声明
├── build.gradle.kts             # 根项目构建脚本
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew                      # Unix Wrapper 脚本
├── gradlew.bat                  # Windows Wrapper 脚本
└── src/
    ├── main/java/               # 源码（与 Maven 相同）
    ├── main/resources/          # 资源文件
    └── test/java/               # 测试代码
```

### 4.2 settings.gradle.kts

```kotlin
// settings.gradle.kts
rootProject.name = "my-platform"

// 多模块
include("common", "service-user", "service-order")
```

### 4.3 build.gradle.kts（Spring Boot 3.x 完整模板）

```kotlin
// build.gradle.kts
plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    // 阿里云镜像：maven { url = uri("https://maven.aliyun.com/repository/public") }
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // 数据库
    runtimeOnly("com.mysql:mysql-connector-j")

    // 工具库
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 测试
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 自定义 Task
tasks.register("printVersion") {
    doLast {
        println("Building ${project.group}:${project.name}:${project.version}")
    }
}
```

---

## 5. 依赖配置类型详解

Gradle 的依赖配置比 Maven Scope 更精细：

| 配置 | 编译期 | 运行期 | 传递性 | 类比 Maven Scope | 使用场景 |
|------|:---:|:---:|:---:|------|------|
| `implementation` | ✅ | ✅ | ❌ | compile（推荐） | ⭐ 最常用，内部实现依赖 |
| `api` | ✅ | ✅ | ✅ | compile | 对外暴露的 API 模块 |
| `compileOnly` | ✅ | ❌ | ❌ | provided | Lombok、Servlet API |
| `runtimeOnly` | ❌ | ✅ | ✅ | runtime | JDBC 驱动 |
| `testImplementation` | ❌ | ❌ | ❌ | test | JUnit、Mockito |
| `annotationProcessor` | ✅ | ❌ | ❌ | — | Lombok、MapStruct |

```kotlin
dependencies {
    // ⭐ 最常用：implementation
    implementation("org.springframework.boot:spring-boot-starter-web")

    // 模块间依赖（api vs implementation）
    api(project(":common"))                   // 对外暴露
    implementation(project(":internal-util")) // 仅内部使用

    // 编译期注解
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 运行时
    runtimeOnly("com.mysql:mysql-connector-j")

    // 统一版本管理（BOM）
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.0"))
}
```

---

## 6. Gradle Wrapper

Wrapper 是 Gradle 的"自举"机制，无需全局安装：

```bash
# 生成 Wrapper（锁定版本）
gradle wrapper --gradle-version 8.7

# 之后使用 gradlew
./gradlew build     # 自动下载指定版本 Gradle
./gradlew test
```

生成文件：

| 文件 | 作用 |
|------|------|
| `gradlew` / `gradlew.bat` | 跨平台执行脚本 |
| `gradle/wrapper/gradle-wrapper.jar` | Wrapper JAR（~60KB） |
| `gradle/wrapper/gradle-wrapper.properties` | 版本配置 |

---

## 7. 常用命令速查

```bash
# ═══ 基础构建 ═══
./gradlew build              # 编译 + 测试 + 打包
./gradlew clean              # 清理
./gradlew test               # 运行测试

# ═══ 信息查看 ═══
./gradlew tasks --all        # 列出所有 Task
./gradlew dependencies       # 查看依赖树
./gradlew projects           # 列出子项目

# ═══ Spring Boot ═══
./gradlew bootRun            # 启动 Spring Boot
./gradlew bootJar            # 打包可执行 JAR

# ═══ 性能优化 ═══
./gradlew build --parallel              # 并行
./gradlew build --build-cache           # 构建缓存
./gradlew build --configuration-cache   # 配置缓存（Gradle 8+）
```

---

## 8. 与 Maven 关键差异

| 维度 | Gradle | Maven |
|------|--------|-------|
| 构建脚本 | `build.gradle(.kts)` | `pom.xml` |
| DSL 语言 | Groovy/Kotlin（可编程） | XML（声明式） |
| 灵活性 | ⭐⭐⭐ 高度可编程 | ⭐ 受限于插件 |
| 构建性能 | ⭐⭐⭐ 增量+缓存+并行 | ⭐⭐ `-T` 并行 |
| 学习曲线 | 陡峭 | 平缓 |
| Android | 官方 | 需插件 |
| 约定优于配置 | 灵活但需显式声明 | ⭐⭐⭐ 强约定 |

---

## 9. Spring Boot + Gradle 实战模板

### 9.1 最小可运行项目

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "com.example"
version = "1.0.0"
java { sourceCompatibility = JavaVersion.VERSION_21 }
repositories { mavenCentral() }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> { useJUnitPlatform() }
```

### 9.2 选型建议

| 场景 | 推荐 |
|------|------|
| 新项目 / Android / Kotlin | ⭐ **Gradle (Kotlin DSL)** |
| 成熟企业 Java 项目 | Maven（团队熟悉度优先） |
| 构建速度敏感 | ⭐ **Gradle** |
| CI/CD 标准化 | Maven（简单可靠） |

> 🎯 Gradle 核心价值：构建性能（增量+缓存+并行）+ 灵活性（可编程 DSL）。Kotlin DSL 是推荐选择（类型安全+IDE 支持）。更详细的对比请参见 `17-Maven与Gradle对比选型指南.md`。
