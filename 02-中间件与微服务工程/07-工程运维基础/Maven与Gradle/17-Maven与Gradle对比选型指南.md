# 17 - Maven 与 Gradle 对比选型指南

> 🎯 选 Maven 还是 Gradle？这不是信仰之争，而是工程决策 — 从团队能力、项目规模、构建性能、生态兼容四个维度做出理性选择

---

## 目录

1. [全方位对比矩阵](#1-全方位对比矩阵)
2. [核心哲学差异](#2-核心哲学差异)
3. [语法对比速查](#3-语法对比速查)
4. [性能对比](#4-性能对比)
5. [场景化选型决策](#5-场景化选型决策)
6. [混合使用策略](#6-混合使用策略)

---

## 1. 全方位对比矩阵

| 维度 | Maven | Gradle | 胜出 |
|------|-------|--------|:---:|
| **构建脚本** | `pom.xml` (XML) | `build.gradle(.kts)` (Groovy/Kotlin) | Gradle |
| **学习曲线** | ⭐⭐ 平缓，XML 人人可读 | ⭐⭐⭐⭐ 陡峭，DSL 需学习 | Maven |
| **灵活性** | 低（插件 + XML 配置） | 高（可编程 DSL，直接写逻辑） | Gradle |
| **构建速度** | 中等（无内置增量） | 快（增量+缓存+并行+Daemon） | Gradle |
| **约定优于配置** | ⭐⭐⭐ 强约定 | ⭐⭐ 可自定义，需显式声明 | Maven |
| **IDE 支持** | ⭐⭐⭐ 成熟（IDEA/Eclipse） | ⭐⭐⭐ 良好（IDEA 原生支持） | 平 |
| **依赖管理** | GAV 坐标 + XML Scope | 短语法 + Configuration | Gradle |
| **多模块** | `<modules>` + 父 POM 继承 | `include()` + `allprojects{}` | 平 |
| **插件生态** | ⭐⭐⭐ 极其丰富 | ⭐⭐⭐ 丰富（兼容 Maven 仓库） | Maven |
| **Android** | 需插件，非官方 | ⭐⭐⭐ 官方构建系统 | Gradle |
| **CI/CD** | ⭐⭐⭐ 简单可靠 | ⭐⭐ 稍复杂（Wrapper + Daemon） | Maven |
| **社区规模** | ⭐⭐⭐ 广泛，文档多 | ⭐⭐⭐ 快速增长 | Maven |
| **Spring Boot** | ⭐⭐⭐ 广泛使用 | ⭐⭐⭐ 官方推荐 | 平 |

---

## 2. 核心哲学差异

### 2.1 构建模型

```
Maven：固定生命周期模型
  validate → compile → test → package → verify → install → deploy
  → 所有项目走同样的路径，插件填充各阶段

Gradle：有向无环图 (DAG) 模型
  Task A → Task B → Task C
         ↘ Task D ↗
  → Task 自由组合，按依赖关系执行
```

### 2.2 配置模型

| 维度 | Maven | Gradle |
|------|-------|--------|
| **配置语言** | XML（声明式） | Kotlin/Groovy（编程式） |
| **逻辑能力** | 极有限（需插件） | 完整编程能力 |
| **复用方式** | 父 POM 继承 | `allprojects{}` / `buildSrc` / Convention Plugin |
| **版本管理** | `<dependencyManagement>` / BOM | `platform()` / `libs.versions.toml` |
| **多环境** | `<profiles>` | 需手动实现或用条件逻辑 |

---

## 3. 语法对比速查

### 3.1 基本结构

```xml
<!-- Maven: pom.xml -->
<project>
    <groupId>com.example</groupId>
    <artifactId>demo</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
</project>
```

```kotlin
// Gradle: build.gradle.kts
group = "com.example"
version = "1.0.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

### 3.2 常用操作对比

| 操作 | Maven | Gradle |
|------|-------|--------|
| 添加依赖 | `<dependency>` XML 块 | `implementation("g:a:v")` |
| 指定 JDK 版本 | `maven-compiler-plugin` 配置 | `java { sourceCompatibility = ... }` |
| 多模块 | `<modules>` + `<parent>` | `include()` + `project()` |
| 排除传递依赖 | `<exclusions>` XML 块 | `exclude()` 一行 |
| 跳过测试 | `-DskipTests` | `-x test` |
| 查看依赖树 | `mvn dependency:tree` | `gradle dependencies` |
| Profile | `<profiles>` XML 块 | 需自定义（无内置等价） |
| 自定义构建逻辑 | 编写 Maven 插件（Java） | 直接在 build.gradle 中写 |

### 3.3 依赖范围映射

| Maven Scope | Gradle Configuration |
|-------------|---------------------|
| compile | `implementation` / `api` |
| provided | `compileOnly` |
| runtime | `runtimeOnly` |
| test | `testImplementation` |
| system | 不推荐使用 |
| import | `platform()` |

---

## 4. 性能对比

### 4.1 典型大型项目构建时间（参考数据）

| 场景 | Maven | Gradle（首次） | Gradle（增量） | Gradle（缓存命中） |
|------|:---:|:---:|:---:|:---:|
| 小型（1模块，100类） | 5s | 6s | 1s | 0.5s |
| 中型（10模块，500类） | 45s | 40s | 8s | 3s |
| 大型（50模块，2000类） | 180s | 150s | 25s | 8s |

### 4.2 Gradle 性能优势来源

```
1. 增量构建    → 仅重编译修改的源文件（Maven 需插件实现）
2. 构建缓存    → 跨项目/跨分支复用 Task 输出
3. Daemon      → 复用 JVM，避免每次冷启动
4. 并行构建    → 多模块独立 Task 并发执行
5. 配置缓存    → 跳过重复的配置阶段（Gradle 8+）
```

---

## 5. 场景化选型决策

### 5.1 决策树

```
是否 Android 项目？
├── YES → Gradle（唯一选择）
└── NO
    ├── 团队主要技术栈是 Kotlin？
    │   └── YES → Gradle Kotlin DSL
    ├── 团队是否有 Gradle 经验？
    │   ├── YES + 构建性能敏感 → Gradle
    │   └── NO → Maven（降低学习成本）
    ├── 需要大量自定义构建逻辑？
    │   └── YES → Gradle（Maven 插件开发成本高）
    ├── 已有成熟 Maven 项目？
    │   └── YES → 继续 Maven（迁移成本 > 收益）
    └── 新项目？
        └── Gradle（拥抱未来）
```

### 5.2 场景推荐表

| 场景 | 推荐 | 理由 |
|------|:---:|------|
| **Android 项目** | Gradle | 官方唯一支持 |
| **Kotlin 项目** | Gradle | Kotlin DSL 类型安全 |
| **Spring Boot 新项目** | 两者均可 | Gradle 略优（官方推荐+性能） |
| **大型多模块企业项目** | Gradle | 增量构建+并行大幅提速 |
| **团队以 Java 为主/无 Gradle 经验** | Maven | 降低学习成本，XML 易理解 |
| **CI/CD 简单可靠优先** | Maven | 无 Daemon 问题，标准化命令 |
| **需要大量自定义构建** | Gradle | 直接在脚本中编写逻辑 |
| **已有成熟 Maven 项目** | Maven | 不折腾，迁移 ROI 不高 |
| **微服务（多独立仓库）** | Gradle | 复合构建 + 增量编译 |
| **开源项目** | Maven | 用户更广泛，降低贡献门槛 |

---

## 6. 混合使用策略

### 6.1 新老共存

```
企业常见策略：
├── 存量 Maven 项目  → 保持不动，维护成本低
├── 新的 Java 项目   → 评估后可选 Gradle
├── 新的 Kotlin 项目 → Gradle（自然选择）
└── 公共库           → 同时发布到 Maven 仓库（两方可共用）
```

### 6.2 Gradle 项目兼容 Maven

```kotlin
// build.gradle.kts — 发布到 Maven 本地仓库（供 Maven 项目使用）
plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
```

```bash
./gradlew publishToMavenLocal    # 发布到 ~/.m2/repository
# 之后 Maven 项目可以正常依赖此库
```

### 6.3 Maven 项目渐进迁移

```
阶段1：用 gradle init 自动生成 → 验证构建结果一致
阶段2：手动调优多模块配置 → 本地开发使用 Gradle
阶段3：CI 并行跑 Maven + Gradle（过渡期）
阶段4：确认稳定后废弃 Maven 构建
```

> 🎯 **最终建议**：工具服务于人，不让人服务于工具。选型优先考虑团队能力和项目需求，新项目推荐 Gradle Kotlin DSL，成熟 Maven 项目不折腾。
