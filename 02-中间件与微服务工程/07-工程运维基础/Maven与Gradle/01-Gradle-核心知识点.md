# Gradle 核心知识点

## 一、概述

Gradle 是基于 Groovy/Kotlin DSL 的现代化 JVM 构建工具，使用**有向无环图（DAG）**管理任务依赖，支持增量构建和构建缓存。

- **官网**：https://gradle.org
- **核心优势**：灵活的 DSL 脚本 + 高性能增量构建
- **官方地位**：Android 官方构建系统，Spring Boot 也推荐使用

## 二、构建生命周期

```
初始化（Initialization）
  → 确定参与构建的 Project，创建 Project 对象

配置（Configuration）
  → 解析 build.gradle(.kts)，构建 Task 依赖图（DAG）

执行（Execution）
  → 按依赖顺序执行 Task，增量构建仅执行输入变化的 Task
```

## 三、核心三要素

| 概念 | 说明 |
|------|------|
| **Project** | 构建单元（JAR/WAR），对应一个 `build.gradle` |
| **Task** | 构建操作（编译、打包、测试等），Gradle 内置 + 可自定义 |
| **Plugin** | 可复用的构建逻辑集合，如 `java`、`spring-boot`、`application` |

## 四、与 Maven 对比

| 维度 | Gradle | Maven |
|------|--------|-------|
| 构建脚本 | `build.gradle(.kts)` | `pom.xml` |
| DSL 语言 | Groovy / Kotlin | XML |
| 灵活性 | **高**（可编程扩展） | 低（插件 + XML 配置） |
| 构建性能 | **快**（增量 + 缓存 + 并行） | 较慢 |
| 依赖管理 | 短语法 + 动态版本 | XML + 固定版本 |
| 学习曲线 | 高 | 低 |
| 生态规模 | 中等 | **大** |
| Android | **官方** | 需插件 |

## 五、Kotlin DSL 基本结构

```kotlin
// settings.gradle.kts
rootProject.name = "my-app"
include("lib", "web")

// build.gradle.kts（根项目）
plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
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

## 六、依赖配置类型

| 配置 | 作用 | 类比 Maven Scope |
|------|------|------------------|
| `implementation` | 编译 + 运行，不传递 | compile（推荐） |
| `api` | 编译 + 运行，可传递 | compile |
| `compileOnly` | 仅编译 | provided |
| `runtimeOnly` | 仅运行 | runtime |
| `testImplementation` | 仅测试编译 | test |
| `annotationProcessor` | 注解处理器 | annotationProcessor |

## 七、常用命令

```bash
# 基础
gradle build              # 编译 + 测试 + 打包
gradle test               # 运行测试
gradle clean              # 清理构建产物
gradle dependencies       # 查看依赖树
gradle tasks              # 列出所有 Task

# Spring Boot
gradle bootRun            # 启动 Spring Boot 应用

# 性能优化
gradle build --parallel   # 并行构建
gradle build --build-cache # 启用构建缓存
```

## 八、Gradle Wrapper

```bash
# 项目根目录生成 wrapper，锁定 Gradle 版本
gradle wrapper --gradle-version 8.7
```

生成的文件：
- `gradlew` / `gradlew.bat`：跨平台执行脚本
- `gradle/wrapper/gradle-wrapper.jar`：Wrapper JAR
- `gradle/wrapper/gradle-wrapper.properties`：版本配置

**优势**：无需全局安装 Gradle，`gradlew` 自动下载指定版本，保证团队环境一致。

## 九、多模块项目

```kotlin
// settings.gradle.kts
rootProject.name = "my-platform"
include("common", "service-user", "service-order", "web-api")

// 子模块 build.gradle.kts
dependencies {
    implementation(project(":common"))
}
```

## 十、选型建议

| 场景 | 推荐 |
|------|------|
| 新项目 / Android | **Gradle（Kotlin DSL）** |
| 成熟企业项目 | Maven（团队熟悉度优先） |
| 构建速度敏感 | **Gradle** |
| CI/CD 标准化 | Maven（简单可靠） |

## 十一、总结

Gradle 的核心价值在于**构建性能**（增量 + 缓存 + 并行）和**灵活性**（可编程 DSL）。Kotlin DSL 是推荐选择（类型安全 + IDE 支持完善）。Maven 在简单性和标准化方面仍有广泛基础，选型应结合团队实际情况。
