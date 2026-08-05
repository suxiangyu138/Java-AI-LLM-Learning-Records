# 06 多版本 JDK 管理与 SDKMAN

> 一台机器跑多个 JDK 是后端日常——SDKMAN 是最优雅的管理器，配合 CI 矩阵与容器化，让"JDK 环境"成为可复现的工程资产

---

## 📚 目录

1. [为什么需要多版本 JDK](#1-为什么需要多版本-jdk)
2. [SDKMAN 全命令解析](#2-sdkman-全命令解析)
3. [管理工具横向对比](#3-管理工具横向对比)
4. [CI/CD 中的 JDK 矩阵](#4-cicd-中的-jdk-矩阵)
5. [生产环境 JDK：容器与运行时](#5-生产环境-jdk容器与运行时)
6. [环境管理的工程规范](#6-环境管理的工程规范)

---

## 1. 为什么需要多版本 JDK

**多版本是常态而非特例**：

| 场景 | 需求 |
|------|------|
| 多个项目基线不同 | 项目 A 用 JDK 17（Spring Boot 3）、项目 B 用 JDK 21、遗留项目 JDK 8 |
| 新特性尝鲜与回退 | JDK 25 预览特性测试失败 → 一键切回 21 |
| 构建环境 vs 运行环境 | CI 用 LTS 编译，本地可跑最新版体验新语法 |
| 工具链版本差异 | Gradle 版本与 JDK 版本有兼容矩阵 |

**核心原则**：**环境变量（JAVA_HOME/PATH）是"全局默认"，版本切换要"按项目隔离"**——这是多版本管理的本质。

> 🎯 **核心要点**：多版本管理的目标不是"装很多 JDK"，而是"**每个项目有确定的 JDK，切换零摩擦**"——工具的价值在于把"改环境变量"这种易错操作变成一条命令。

---

## 2. SDKMAN 全命令解析

**SDKMAN**（Linux/macOS/WSL/Cygwin/Git Bash 可用）——管理 JDK 及其他 JVM 生态工具（Maven/Gradle/Kotlin/Scala/Spring Boot CLI）：

```bash
# 安装 SDKMAN
curl -s "https://get.sdkman.io" | bash

# 核心命令（以 JDK 为例）
sdk list java                    # 列出所有可用 JDK 发行版与版本（含 Temurin/Zulu/GraalVM...）
sdk install java 21.0.5-tem      # 安装指定版本（发行版后缀：-tem/-zulu/-corretto/-graal）
sdk install java 25-tem          # 不指定补丁号：装该发行版最新

sdk use java 21.0.5-tem          # 当前 shell 临时切换（只影响当前终端）
sdk default java 21.0.5-tem      # 设置全局默认（写入 ~/.sdkman/candidates/java/current）
sdk current java                 # 查看当前生效版本
sdk upgrade java                 # 升级到最新
sdk uninstall java 17.0.2-zulu   # 卸载

sdk list maven                   # 其他工具同理：maven/gradle/kotlin/springboot...
sdk install maven 3.9.9
```

**SDKMAN 的机制**：

```text
~/.sdkman/candidates/java/          ← 所有安装的 JDK 版本
    ├── 17.0.2-zulu/                 ← 每个版本独立目录
    ├── 21.0.5-tem/
    ├── 25-tem/
    └── current → 21.0.5-tem         ← 软链接：指向 default 版本
# PATH 里加 ~/.sdkman/candidates/java/current/bin —— 切换 = 改软链接，零配置
```

```bash
# 按项目锁定版本的规范做法：项目内 .sdkmanrc
# 内容：java=21.0.5-tem
# 进入项目目录自动加载（需开启 shell 集成）
sdk env init     # 生成 .sdkmanrc
sdk env          # 应用 .sdkmanrc 的版本
```

> 🎯 **核心要点**：SDKMAN 的价值 = **"发行版 + 版本 + 切换"三维管理**——一条命令装任何发行版（Temurin/Zulu/Corretto…）、切换零摩擦、`.sdkmanrc` 把 JDK 版本变成项目资产（可提交到仓库）。

---

## 3. 管理工具横向对比

| 工具 | 平台 | 机制 | 特点 |
|------|------|------|------|
| **SDKMAN** | Unix 系（含 WSL/Git Bash） | 软链接切换 | 多语言、发行版全、`.sdkmanrc` 项目级 |
| jabba | 跨平台 | JAVA_HOME 切换脚本 | 老牌跨平台方案，生态冷清 |
| jenv | macOS/Linux | 目录级配置（`.java-version`） | 与 ruby 的 rbenv 同思路 |
| scoop（Windows） | Windows | 包管理 + shim | Windows 首选之一 |
| Chocolatey | Windows | 包管理 | 系统级安装，版本切换弱 |
| 手动环境变量 | 全平台 | 改 JAVA_HOME/PATH | 零依赖但易错、无版本管理 |
| 容器化（最终方案） | 全平台 | 每项目一个 JDK 镜像 | 隔离最彻底（见第 5 节） |

**选择建议**：

```text
macOS/Linux/WSL（推荐）→ SDKMAN
Windows 原生 → scoop（配 java bucket）或 SDKMAN（Git Bash）
追求环境完全隔离 → 容器（Docker 每项目 JDK 镜像）
```

```powershell
# Windows scoop 示例
scoop bucket add java
scoop install temurin21           # 安装 Temurin 21
scoop install temurin-lts-jdk     # 当前 LTS
# 版本切换：scoop reset temurin17
```

> 💡 Windows 环境变量陷阱：`JAVA_HOME` 与 `PATH` 中多个 JDK 顺序冲突时，"先出现的生效"——排查"java -version 不是我要的版本"时先看 `where java` 指向哪里（详见 `JDK/02` 环境变量详解）。

---

## 4. CI/CD 中的 JDK 矩阵

**CI 的标准姿势：构建矩阵 + 版本固定**（GitHub Actions 示例）：

```yaml
# .github/workflows/build.yml —— 矩阵测试多个 JDK 版本
name: build
on: [push, pull_request]
jobs:
  test:
    strategy:
      matrix:
        java: ['17', '21', '25']        # 构建矩阵：一次提交测 3 个版本
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin         # 发行版固定
          java-version: ${{ matrix.java }}
      - run: mvn verify
```

**CI 的 JDK 纪律**：

| 纪律 | 说明 |
|------|------|
| 版本固定 | 构建脚本明确 JDK 版本（不要依赖 CI 镜像默认） |
| 发行版固定 | `distribution: temurin` 显式声明 |
| 矩阵覆盖 | 目标基线（17/21）必测，最新 LTS（25）加测 |
| 与本地一致 | 本地 `.sdkmanrc` 与 CI 配置同一版本——**"本地能跑 CI 不能跑"的第一排查项就是 JDK 版本** |

> 🎯 **核心要点**：CI 是环境一致性的守门员——**构建矩阵 + 版本固定**让"JDK 版本漂移"这类问题在提交时暴露，而不是上线后。

---

## 5. 生产环境 JDK：容器与运行时

**生产环境 JDK 的三种形态**（从重到轻）：

| 形态 | 说明 | 适用 |
|------|------|------|
| 完整 JDK 镜像 | `eclipse-temurin:21-jdk` | 开发调试、构建阶段 |
| **JRE 裁剪镜像** | jlink 定制运行时（见 03 模块） | **生产运行阶段（推荐）** |
| 原生镜像 | GraalVM AOT（Spring Boot 4 原生支持） | 极致启动/内存 |

```dockerfile
# 生产镜像最佳实践：构建与运行分离（多阶段）
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN mvn -q package

FROM eclipse-temurin:21-jre           # 运行阶段：只用 JRE（或 jlink 定制运行时）
COPY --from=build /app/target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

**容器环境的内存纪律**：

```text
容器内不要用 -Xmx 硬编码 —— 用 -XX:MaxRAMPercentage=75（按容器配额自适应）
否则：容器限 1G，JVM 按宿主机内存算堆 → 容器 OOM Kill
```

> 🎯 **核心要点**：生产 JDK 的工程规范 = **多阶段构建（构建用 JDK、运行用 JRE/定制运行时）+ 内存配额自适应（MaxRAMPercentage）**——镜像小、OOM 少、可复现。

---

## 6. 环境管理的工程规范

**JDK 环境管理的完整规范清单**：

| 环节 | 规范 |
|------|------|
| 本地开发 | SDKMAN + `.sdkmanrc` 项目级锁定；Windows 用 scoop |
| 构建 | CI 矩阵（17/21/25）+ 发行版固定 |
| 运行时 | 多阶段容器镜像，JRE/jlink 运行时 |
| 工具链 | Maven/Gradle 的 `toolchains` 指定 JDK（多版本编译） |
| 文档 | 项目 README 写明"JDK 版本 + 获取方式"（新同事零问号） |

```xml
<!-- Maven Toolchains：一个构建可同时用多个 JDK（如主构建 21、编译目标 17） -->
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides><version>21</version></provides>
    <configuration><jdkHome>~/.sdkman/candidates/java/21-tem</jdkHome></configuration>
  </toolchain>
</toolchains>
```

> 🎯 **核心要点**：环境管理的终点是"**环境即代码**"——`.sdkmanrc`（本地）、CI 配置（构建）、Dockerfile（运行）三处声明同一 JDK 版本，环境问题从"玄学"变成"可复现的配置差异"。

---

**下一模块**：[07-面试高频考点与总结](./07-面试高频考点与总结.md) / **返回总览**：[00-SDK知识体系总览](./00-SDK知识体系总览.md)
