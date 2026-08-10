# 09 - 生产实践与 CI/CD 集成

> 🎯 生产级 Gradle 工程 = Wrapper 版本纪律 + Java 工具链 + 制品发布 + CI 缓存持久化 + 构建可观测。本章覆盖 CI/CD 全链路集成（GitHub Actions/Gitee Go/Jenkins）、制品发布到私有仓库、多版本 JDK 与常见生产坑

---

## 目录

1. [生产纪律七条](#1-生产纪律七条)
2. [CI/CD 集成：三大平台接入](#2-cicd-集成三大平台接入)
3. [CI 缓存持久化配置](#3-ci-缓存持久化配置)
4. [制品发布：maven-publish 实战](#4-制品发布maven-publish-实战)
5. [多版本 JDK 与工具链矩阵](#5-多版本-jdk-与工具链矩阵)
6. [构建可观测性](#6-构建可观测性)
7. [常见生产坑清单](#7-常见生产坑清单)
8. [与 Maven 混用的团队实践](#8-与-maven-混用的团队实践)
9. [练习](#9-练习)

---

## 1. 生产纪律七条

1. **Wrapper 固定版本**：gradlew + wrapper 文件全提交，distributionUrl 精确版本，不用动态版本
2. **gradle.properties 进版本库**：jvmargs/缓存/并行等配置随项目走，团队一致
3. **版本目录统一**：依赖与插件版本集中在 libs.versions.toml，升级单点
4. **配置缓存常开**：org.gradle.configuration-cache=true 默认开启，维护惰性纪律
5. **构建缓存 + 远程缓存**：本地缓存必开，企业用 Develocity/私有缓存服务
6. **依赖锁定**：生产项目锁依赖版本（lockfile 提交），杜绝构建漂移
7. **可复现优先**：9.0 可复现归档默认，配合锁定实现字节级可复现构建

## 2. CI/CD 集成：三大平台接入

**GitHub Actions**（.github/workflows/build.yml）：

```yaml
name: Build
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
      - name: Build
        run: ./gradlew build --no-daemon
```

**Gitee Go**（.workflow/gitee.yml）：语法与 Actions 兼容，`uses: checkout@v1` + `uses: maven@v3` 换成 Gradle 命令步骤即可；国内节点对 Gradle 依赖下载更友好（配合镜像仓库加速）。

**Jenkins**：pipeline 里 `sh './gradlew build'` + 持久化 `~/.gradle` 目录（凭据绑定 GRADLE_USER_HOME 卷）；Wrapper 保证 Jenkins 与本地同版本。

**三平台共同要点**：`--no-daemon`（容器环境不留守护进程）、`--stacktrace`（CI 日志定位）、失败时上传测试报告与 Build Scan 链接。

## 3. CI 缓存持久化配置

CI 缓存的核心：**持久化 GRADLE_USER_HOME**（依赖 + 构建缓存 + 配置缓存一次搞定）。

**GitHub Actions 官方方案**（gradle/actions/setup-gradle 内置缓存管理）：

```yaml
- uses: gradle/actions/setup-gradle@v4
  # 自动缓存 ~/.gradle/caches 与 ~/.gradle/wrapper
```

**自定义缓存**（旧版或特殊需求）：

```yaml
- uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', 'gradle/libs.versions.toml') }}
    restore-keys: ${{ runner.os }}-gradle
```

缓存键设计：构建脚本变了才失效（hashFiles 构建文件），否则复用——依赖不变时命中率最高。**Ephemeral CI 专项**（08 篇）：modules-2 缓存省 28%、GRADLE_RO_DEP_CACHE 只读共享免锁竞争。

## 4. 制品发布：maven-publish 实战

```kotlin
plugins {
    `java-library`
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "My Library"
                description = "Library description"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer { id = "suxiangyu"; name = "Su Xiangyu" }
                }
            }
        }
    }
    repositories {
        maven {
            name = "private"
            url = uri("https://repo.example.com/maven")
            credentials(PasswordCredentials::class) {
                username = providers.gradleProperty("repoUser")
                password = providers.gradleProperty("repoPass")
            }
        }
    }
}
```

发布要点：**pom 元数据**（name/description/license/developers——发布到中央仓库的必需项，私有仓库也建议规范）；**凭据不硬编码**（gradle.properties 里 `repoUser=` 留空，CI 用环境变量注入）；**发布到中央仓库**（Maven Central）需要 sonatype 账号 + GPG 签名（signing 插件）；本地验证 `./gradlew publishToMavenLocal`。

## 5. 多版本 JDK 与工具链矩阵

```yaml
# CI 矩阵：多 JDK 版本验证
strategy:
  matrix:
    java: ['17', '21', '25']
steps:
  - uses: actions/setup-java@v4
    with:
      distribution: temurin
      java-version: ${{ matrix.java }}
  - run: ./gradlew test --no-daemon
```

工具链（02 篇）与 CI 矩阵的配合：**项目声明 toolchain 目标版本，CI 矩阵验证跨版本兼容**；工具链自动下载在 CI 里要确认（offline 环境需预装或缓存 toolchain 下载目录）；`org.gradle.java.installations.auto-download` 在受控环境关闭。多版本策略：开发用 LTS（21）、CI 矩阵覆盖 LTS + 最新、运行时与编译目标分离（编译 21、运行 25 兼容验证）。

## 6. 构建可观测性

- **Build Scan**：每次 CI 构建生成 scan 链接（gradle/actions 自动上传或 Develocity 插件），失败构建附 scan 到 PR 评论——排错从"看日志"升级为"看数据"
- **--profile / --info**：本地诊断
- **Develocity Analytics**：跨团队缓存命中率/构建时间/失败率趋势；MCP Server 自然语言查询（"哪些模块拖慢构建"）
- **告警基线**：构建时间回归、缓存命中率下降、失败率上升——三个指标进监控

## 7. 常见生产坑清单

| # | 坑 | 解法 |
|---|-----|------|
| 1 | 本机能过 CI 挂 | Wrapper 版本不一致 → 全用 gradlew |
| 2 | 依赖下载慢/超时 | 国内镜像仓库 + 持久化 GRADLE_USER_HOME |
| 3 | 配置缓存报错 | 配置阶段读外部状态 → Provider 化/声明输入 |
| 4 | 缓存命中率 0 | Build Scan 查根因（易变输入/绝对路径） |
| 5 | 凭据泄露 | 硬编码在 gradle.properties 被提交 → 环境变量注入 |
| 6 | daemon 残留容器 | CI 用 --no-daemon |
| 7 | 动态版本漂移 | 依赖锁定 + 固定版本 |
| 8 | 增量编译失效 | 编译 flag 关闭了增量 → 检查 kotlin 插件配置 |
| 9 | 插件与 Gradle 版本不兼容 | 插件市场查兼容矩阵，随 Gradle 升级插件 |
| 10 | 可复现构建失败 | 时间戳/路径进归档 → 9.0 可复现归档默认 + 检查配置 |

## 8. 与 Maven 混用的团队实践

大团队常见"Gradle 与 Maven 并存"，三条协作纪律：

- **制品互认**：Gradle 发布到 Maven 仓库的制品，Maven 项目正常消费（坐标兼容）——发布侧统一走私有 Nexus/Artifactory
- **CI 分离**：各自构建的流水线独立，产物汇入同一制品仓库；**不要在同一条流水线混用 mvn 与 gradlew 构建同一制品**（构建产物不一致排查困难）
- **团队培训**：双工具团队最怕"各写各的"——统一约定（Wrapper 版本、仓库规范、发布规范）写入团队文档，模板仓库各建一份

混用不是问题，**规范不一致才是问题**：版本目录（Gradle 侧）与 pom properties（Maven 侧）维护同一份版本清单会漂移——选一个作为"版本事实来源"，另一个引用或同步。

从入门到生产的最后一步检查：**你的项目能否在空环境一键构建**——clone 到新机器/新 CI 容器，`./gradlew build` 一次通过且产物可复现（依赖锁定 + Wrapper 固定 + 工具链声明三者齐备即满足）。做不到一键构建的工程，所有优化都是空中楼阁——这是本体系所有概念的验收标准。

最后给一个实战模板：**新 Java 服务的 Gradle 基线工程**——Kotlin DSL + Wrapper 9.6 + 版本目录 + 约定插件（java-conventions）+ 配置缓存/构建缓存开启 + 依赖锁定 + maven-publish 配置 + CI 流水线（缓存持久化 + --no-daemon + Build Scan 上传）。照这个清单搭一次骨架，等于把本体系 10 篇的知识串成可交付物——比刷任何教程都值。

验收该骨架的三问：**空环境一键构建**（clone → ./gradlew build 通过）；**二次构建增量**（改一行代码重跑，只有受影响任务执行）；**CI 与本地一致**（同版本、同缓存、同产物哈希）。三问全过，这个骨架就是可复制的团队资产——以后新服务从它复制而不是从零写，是"生产实践"这四个字的真正含义。

## 9. 练习

1. 生产纪律七条里哪三条最容易忽略？为什么？
2. 三大 CI 平台的 Gradle 接入共同要点？
3. CI 缓存持久化的缓存键怎么设计？为什么 hashFiles 构建文件？
4. maven-publish 的 pom 元数据为什么重要？凭据怎么管理？
5. 工具链 + CI 矩阵怎么配合？构建可观测的三指标是什么？

---

> 🎯 **核心要点**：生产 Gradle = 七条纪律（Wrapper 固定/版本目录/配置缓存/依赖锁定/可复现）+ CI 接入（--no-daemon、持久化 GRADLE_USER_HOME、--stacktrace）+ 制品发布（pom 元数据、凭据环境变量、GPG 签名）+ 可观测（Build Scan 三指标）。排错顺序：先 Build Scan 数据，再针对性修——不猜。

**下一模块**：[10-与Maven对比与面试自测](10-与Maven对比与面试自测.md) / **返回总览**：[00-总览](00-Gradle知识体系总览.md)
