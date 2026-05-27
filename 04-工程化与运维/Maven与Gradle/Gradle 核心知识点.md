# Gradle 核心知识点

## 一、概述

Gradle 是基于 Groovy/Kotlin DSL 的现代化构建工具，相比 Maven，其核心差异在于 **灵活性** 和 **构建性能**。Gradle 使用有向无环图（DAG）管理任务依赖，支持增量构建和构建缓存。

**核心定位：** 灵活、高性能的 JVM 构建工具，**Android 开发官方构建系统**，大型 Java 项目推荐。

**官网：** https://gradle.org

## 二、核心概念

### 2.1 构建生命周期

```
初始化（Initialization）
  → 确定哪些 Project 参与构建
  → 创建 Project 对象

配置（Configuration）
  → 解析 build.gradle(.kts)
  → 构建 Task 依赖图（DAG）

执行（Execution）
  → 按依赖顺序执行 Task
  → 增量构建：只执行输入有变化的 Task
```

### 2.2 核心三要素

| 概念 | 说明 |
|------|------|
| **Project** | 一个构建单元（JAR/WAR），对应一个 `build.gradle` |
| **Task** | 一个构建操作（编译、打包、测试等），Gradle 内置 + 可自定义 |
| **Plugin** | 可复用的构建逻辑集合（`java`、`spring-boot`、`application`） |

## 三、Maven 对比

| 维度 | Gradle | Maven |
|------|--------|-------|
| 构建脚本 | `build.gradle(.kts)` | `pom.xml` |
| DSL 语言 | Groovy / Kotlin | XML |
| 灵活性 | **高**（可编程扩展） | 低（插件 + XML 配置） |
| 构建性能 | **快**（增量+缓存+并行） | 较慢 |
| 依赖管理 | 短语法 + 动态版本 | XML + 固定版本 |
| 学习曲线 | 高 | 低 |
| 社区规模 | 中等 | **大** |
| Android | **官方** | 需插件 |

## 四、快速上手

### 4.1 基本结构（Kotlin DSL）

```kotlin
// settings.gradle.kts
rootProject.name = "my-app"
include("lib", "web")

// build.gradle.kts (根项目)
plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### 4.2 依赖类型

| 配置 | 作用 | 类比 Maven Scope |
|------|------|------------------|
| `implementation` | 编译+运行，不传递 | compile（推荐） |
| `api` | 编译+运行，可传递 | compile |
| `compileOnly` | 仅编译 | provided |
| `runtimeOnly` | 仅运行 | runtime |
| `testImplementation` | 仅测试编译 | test |
| `annotationProcessor` | 注解处理器 | annotationProcessor |

### 4.3 常用命令

```bash
gradle build              # 编译+测试+打包
gradle bootRun            # 启动 Spring Boot（需插件）
gradle test               # 运行测试
gradle clean              # 清理构建产物
gradle dependencies       # 查看依赖树
gradle tasks              # 列出所有 Task

# 性能相关
gradle build --parallel   # 并行构建
gradle build --build-cache # 启用构建缓存
```

## 五、Gradle Wrapper

```bash
# 项目根目录生成 wrapper
gradle wrapper --gradle-version 8.7

# 生成的文件
# ├── gradlew         (Unix 执行脚本)
# ├── gradlew.bat     (Windows 执行脚本)
# └── gradle/wrapper/
#     ├── gradle-wrapper.jar
#     └── gradle-wrapper.properties
```

**好处：** 不需要全局安装 Gradle，`gradlew` 自动下载指定版本。

## 六、多模块项目

```kotlin
// settings.gradle.kts
rootProject.name = "my-platform"
include("common", "service-user", "service-order", "web-api")

// 子模块 build.gradle.kts
dependencies {
    implementation(project(":common"))
}
```

## 七、AI 辅助

```kotlin
// 用 AI 生成 Gradle 构建脚本
// Prompt: "Gradle Kotlin DSL，Spring Boot 3.3, Java 21,
//          依赖 JPA、Flyway、Redis、Testcontainers"
```

## 八、选型建议

| 场景 | 推荐 |
|------|------|
| 新项目 / Android | Gradle（Kotlin DSL） |
| 成熟企业项目 | Maven（团队熟悉度优先） |
| 构建速度敏感 | Gradle |
| CI/CD 标准化 | Maven（简单可靠） |

## 九、总结

Gradle 的核心价值在于 **构建性能**（增量+缓存+并行）和 **灵活性**（可编程 DSL）。Kotlin DSL 是 2026 年的推荐选择（类型安全 + IDE 支持好）。但 Maven 的简单和标准化在企业中仍有广泛基础。
