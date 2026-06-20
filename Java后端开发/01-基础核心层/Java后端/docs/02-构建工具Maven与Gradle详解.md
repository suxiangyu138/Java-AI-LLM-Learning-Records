# 🔧 构建工具：Maven 与 Gradle 详解

> 掌握依赖管理、构建生命周期、多模块项目，企业级构建实践。

---

## 目录

1. [构建工具的作用与演进](#1-构建工具的作用与演进)
2. [Maven 核心概念](#2-maven-核心概念)
3. [Maven 依赖管理](#3-maven-依赖管理)
4. [Maven 生命周期与插件](#4-maven-生命周期与插件)
5. [Maven 多模块项目](#5-maven-多模块项目)
6. [Gradle 核心概念](#6-gradle-核心概念)
7. [Gradle 依赖管理](#7-gradle-依赖管理)
8. [Maven vs Gradle 对比与选型](#8-maven-vs-gradle-对比与选型)
9. [依赖冲突与解决](#9-依赖冲突与解决)
10. [私有仓库与 Nexus/Artifactory](#10-私有仓库与-nexusartifactory)
11. [BOM 与版本管理最佳实践](#11-bom-与版本管理最佳实践)
12. [常见问题与面试题](#12-常见问题与面试题)

---

## 1. 构建工具的作用与演进

### 1.1 为什么需要构建工具？

```
没有构建工具的时代（javac 手动编译）：
  javac -d bin -cp lib/a.jar:lib/b.jar src/**/*.java
  jar -cf app.jar -C bin .
  # 问题：依赖下载？版本管理？编译顺序？测试？打包？部署？
  # 全是手动管理 → 噩梦

构建工具解决的问题：
  1. 依赖管理：自动下载 + 版本控制 + 传递性依赖
  2. 编译：顺序编译、增量编译
  3. 测试：自动执行测试
  4. 打包：JAR/WAR/Docker Image
  5. 发布：推送到仓库
  6. 多模块：模块间依赖 + 统一版本
```

### 1.2 构建工具演进历程

```
Ant (2000)
  ↓ 灵活但无约定，脚本编写重复
Maven (2004)
  ↓ 约定大于配置，但 XML 繁琐，自定义能力弱
Gradle (2012)
  ↓ 编程式构建，速度快，灵活
Maven Wrapper / Gradle Wrapper
  → 不需要预装 Maven/Gradle，项目自带构建工具
```

---

## 2. Maven 核心概念

### 2.1 坐标 (Coordinates)

```xml
<!-- Maven 用三维坐标唯一确定一个构件 -->
<groupId>com.example</groupId>      <!-- 组织/公司标识，反向域名 -->
<artifactId>my-app</artifactId>     <!-- 项目/模块标识 -->
<version>1.0.0-SNAPSHOT</version>  <!-- 版本号 -->
<packaging>jar</packaging>         <!-- 打包类型：jar/war/pom/ear -->
```

### 2.2 约定优于配置

```
Maven 标准目录结构（约定优于配置的核心体现）：

src/
├── main/
│   ├── java/              ← 源代码
│   ├── resources/         ← 资源文件（配置文件、SQL 文件等）
│   ├── filters/           ← 资源过滤文件（变量替换）
│   └── webapp/            ← Web 资源（web.xml、JSP 等，仅 WAR 项目）
│
├── test/
│   ├── java/              ← 测试源代码
│   ├── resources/         ← 测试资源文件
│   └── filters/           ← 测试资源过滤文件
│
├── target/                ← 构建输出目录（自动生成）
│   ├── classes/           ← 编译后的 class 文件
│   ├── test-classes/      ← 测试 class 文件
│   └── my-app.jar         ← 打包后的 JAR
│
├── pom.xml                ← 项目对象模型
└── LICENSE.txt / README.md
```

### 2.3 POM 最小结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 坐标 -->
    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>

    <!-- 属性（变量集中管理） -->
    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.3.0</spring-boot.version>
    </properties>

    <!-- 父 POM（继承） -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
    </parent>

    <!-- 依赖 -->
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

    <!-- 构建配置 -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2.4 版本号规范

```
版本号格式：<主版本>.<次版本>.<增量版本>[-<里程碑>]

主版本(Major)：不兼容的 API 变更
次版本(Minor)：向下兼容的功能新增
增量版本(Patch)：向下兼容的问题修复
里程碑：SNAPSHOT / RC1 / M1 / RELEASE

示例：
  1.0.0-SNAPSHOT  → 开发中的快照版本
  1.0.0-RC1       → 第一个候选发布版
  1.0.0-RELEASE   → 正式发布版
  2.1.3           → 正式版

SNAPSHOT vs RELEASE：
  SNAPSHOT：每次构建都检查远程仓库拉最新（开发阶段）
  RELEASE：本地仓库有就永不重新下载（稳定阶段）
```

---

## 3. Maven 依赖管理

### 3.1 依赖作用域 (scope)

| Scope | 编译可用 | 测试可用 | 运行时可用 | 打包进 JAR | 典型示例 |
|-------|---------|---------|-----------|-----------|---------|
| **compile** | ✅ | ✅ | ✅ | ✅ | spring-boot-starter-web（默认） |
| **provided** | ✅ | ✅ | ❌ | ❌ | servlet-api, lombok |
| **runtime** | ❌ | ✅ | ✅ | ✅ | mysql-connector-java, 数据库驱动 |
| **test** | ❌ | ✅ | ❌ | ❌ | junit, mockito, h2 |
| **system** | ✅ | ✅ | ✅ | ❌ | 本地 jar（不推荐） |
| **import** | 导入 BOM，不实际引入依赖 | - | - | - | spring-cloud-dependencies |

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>   <!-- 编译时需要，运行时不需要 -->
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>    <!-- 运行时才需要，编译不直接依赖 -->
</dependency>
```

### 3.2 传递性依赖

```
传递性依赖（Transitive Dependency）：
  项目依赖 A → A 依赖 B → 项目自动依赖 B

依赖传递规则：
  scope 传递规则表（左边是 A 的 scope，上边是 B 的 scope）：
              | compile | provided | runtime | test
  compile     | compile | -        | runtime | -
  provided    | provided| -        | provided| -
  runtime     | runtime | -        | runtime | -
  test        | test    | -        | test    | -

举例：
  A (compile) → B (runtime)：传递后 B 的 scope = runtime
  A (test)     → B (compile)：传递后 B 的 scope = test
  A (provided) → B (compile)：不传递！
```

### 3.3 依赖冲突与仲裁

```xml
<!-- 依赖仲裁规则（当存在不同版本的同一依赖时） -->
<!-- 规则 1：最短路径优先 -->
<!-- 规则 2：路径相同 → 先声明者优先 -->

<!-- 示例：项目同时依赖 -->
A (1.0) → C (2.0)     <!-- 路径长度 2 -->
B (1.0) → D → C (1.0) <!-- 路径长度 3 -->
<!-- 结果：选 C (2.0)，因为路径更短 -->

<!-- 强制指定版本（推荐做法） -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.17.1</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 排除传递性依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<!-- 手动引入 Undertow 替代 Tomcat -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

### 3.4 dependencyManagement vs dependencies

```xml
<!-- dependencyManagement（父 POM 中统一管理版本） -->
<!-- 只是声明版本，不实际引入依赖！子模块仍需声明 -->

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>33.2.0-jre</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 子模块中：无需写版本号，由父 POM 管理 -->
<dependencies>
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <!-- version 由父 POM 的 dependencyManagement 统一控制 -->
    </dependency>
</dependencies>
```

---

## 4. Maven 生命周期与插件

### 4.1 三大生命周期

```
clean 生命周期（清理）：
  pre-clean → clean → post-clean

default 生命周期（构建）：
  validate → compile → test → package → verify → install → deploy

site 生命周期（站点）：
  pre-site → site → post-site → site-deploy

关键阶段说明：
  compile   编译源代码（src/main/java → target/classes）
  test      编译测试代码 + 执行测试
  package   打包（生成 JAR/WAR）
  verify    运行集成测试、质量检查
  install   安装到本地仓库（~/.m2/repository）
  deploy    部署到远程仓库（供其他人使用）
```

### 4.2 常用命令

```bash
mvn clean                    # 清理 target 目录
mvn compile                  # 只编译
mvn test                     # 编译 + 运行测试
mvn package                  # 编译 + 测试 + 打包
mvn install                  # 编译 + 测试 + 打包 + 安装到本地仓库
mvn deploy                   # 编译 + 测试 + 打包 + 安装 + 部署到远程仓库

# 常用组合
mvn clean package -DskipTests       # 跳过测试打包
mvn clean package -Dmaven.test.skip=true  # 跳过测试编译和执行
mvn clean install -pl module-a -am  # 只构建子模块（及依赖它的模块）

# 参数
-pl module-name      # 指定模块
-am                  # also-make，同时构建依赖的模块
-amd                 # also-make-dependents，同时构建被依赖的模块
-rf                  # resume-from，从指定模块恢复构建
-T 4                 # 并行构建（4 线程）
```

### 4.3 核心插件

```xml
<build>
    <plugins>
        <!-- 编译器插件：指定 Java 版本 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <source>21</source>
                <target>21</target>
                <compilerArgs>
                    <arg>--enable-preview</arg>
                </compilerArgs>
            </configuration>
        </plugin>

        <!-- JAR 插件：自定义 MANIFEST -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <configuration>
                <archive>
                    <manifest>
                        <addClasspath>true</addClasspath>
                        <mainClass>com.example.Application</mainClass>
                    </manifest>
                </archive>
            </configuration>
        </plugin>

        <!-- 源码打包 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-source-plugin</artifactId>
            <executions>
                <execution>
                    <id>attach-sources</id>
                    <phase>package</phase>
                    <goals>
                        <goal>jar-no-fork</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>

        <!-- Spring Boot 打包（可执行 JAR/原生镜像） -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <mainClass>com.example.Application</mainClass>
                <layout>JAR</layout>
            </configuration>
        </plugin>

        <!-- 依赖分析（检查未使用的依赖、版本冲突） -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-dependency-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

### 4.4 Profile（环境区分）

```xml
<!-- 通过 Profile 区分 dev/test/prod 环境 -->
<profiles>
    <profile>
        <id>dev</id>
        <properties>
            <env>dev</env>
        </properties>
        <activation>
            <activeByDefault>true</activeByDefault>  <!-- 默认激活 -->
        </activation>
    </profile>
    <profile>
        <id>prod</id>
        <properties>
            <env>prod</env>
        </properties>
    </profile>
</profiles>

<!-- 激活方式 -->
<!-- mvn clean package -P dev      命令行 -P -->
<!-- mvn clean package -P prod     -->
<!-- 也可以在 settings.xml 中配置 -->
```

---

## 5. Maven 多模块项目

### 5.1 聚合与继承

```xml
<!-- 根 POM（聚合 + 继承） -->
<!-- packaging 必须是 pom -->
<project>
    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <!-- 聚合模块（一起构建） -->
    <modules>
        <module>my-common</module>     <!-- 公共模块 -->
        <module>my-api</module>        <!-- API 定义模块 -->
        <module>my-service</module>    <!-- 服务模块 -->
        <module>my-web</module>        <!-- Web 模块 -->
    </modules>

    <!-- 统一依赖管理（继承） -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>3.3.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- 统一管理内部模块版本 -->
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>my-common</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 5.2 多模块依赖关系设计

```
典型分层架构对应的模块划分：

my-web（Controller 层）
  ↓ 依赖
my-service（Service 层，含接口定义 + 实现）
  ↓ 依赖
my-api（Feign Client 接口 / DTO 定义，供其他微服务调用）
  ↓ 依赖
my-common（工具类、常量、通用异常）

其他微服务 B → 依赖 my-api → 通过 Feign 调用 my-web

设计原则：
  1. API 模块只包含对外暴露的接口和 DTO，不含实现
  2. Common 模块不含业务逻辑
  3. Service 模块可包含 my-service-api + my-service-impl 两个子模块
  4. 杜绝循环依赖！
```

---

## 6. Gradle 核心概念

### 6.1 基本结构

```groovy
// build.gradle (Groovy DSL)
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.0'
    id 'io.spring.dependency-management' version '1.1.5'
}

// 项目信息
group = 'com.example'
version = '1.0.0'
description = 'My Application'

// 编译配置
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// 仓库
repositories {
    mavenCentral()              // Maven 中央仓库
    maven { url 'https://repo.spring.io/milestone' }  // Spring 里程碑仓库
    maven { url 'https://packages.confluent.io/maven/' }
    mavenLocal()                // 本地 Maven 仓库
}

// 依赖
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    runtimeOnly 'com.mysql:mysql-connector-j'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

// 测试配置
test {
    useJUnitPlatform()
}

// 构建配置
tasks.named('bootJar') {
    archiveFileName = 'my-app.jar'
}
```

### 6.2 Gradle 依赖配置（Configuration）

| Configuration | 作用 | Maven 对应 |
|--------------|------|-----------|
| **implementation** | 编译+运行，不泄露给编译依赖此模块的项目 | compile (更安全) |
| **api** | 编译+运行，泄露给依赖此模块的项目 | compile |
| **compileOnly** | 仅编译时 | provided |
| **runtimeOnly** | 仅运行时 | runtime |
| **annotationProcessor** | 注解处理器 | - |
| **testImplementation** | 仅测试 | test |

```groovy
// implementation vs api（Gradle 的重要设计）
// my-lib 使用 implementation 引入 guava
// → 依赖 my-lib 的模块不会自动看到 guava
// → 编译隔离，减少传递性依赖

// my-lib 使用 api 引入 guava（慎用）
// → 依赖 my-lib 的模块自动看到 guava
// → 类似 Maven 的 compile scope
```

### 6.3 Gradle Wrapper

```bash
# Gradle Wrapper 让项目自带 Gradle 版本
# 不需要开发者预先安装 Gradle

./gradlew build    # Unix
gradlew.bat build  # Windows

# 文件结构：
# gradlew / gradlew.bat    执行脚本
# gradle/wrapper/
#   gradle-wrapper.jar     引导 JAR
#   gradle-wrapper.properties  版本配置

# gradle-wrapper.properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
```

### 6.4 Gradle 多模块

```groovy
// settings.gradle（模块声明）
rootProject.name = 'my-project'
include 'my-common'
include 'my-api'
include 'my-service'
include 'my-web'

// 根 build.gradle
subprojects {
    apply plugin: 'java'

    group = 'com.example'
    version = '1.0.0'

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'
        testImplementation 'org.junit.jupiter:junit-jupiter'
    }
}

// 子模块 build.gradle
dependencies {
    implementation project(':my-common')  // 依赖兄弟模块
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

---

## 7. Gradle 依赖管理

### 7.1 版本目录（Version Catalog）

```toml
# gradle/libs.versions.toml（Gradle 7.0+ 官方推荐的版本管理方式）
[versions]
spring-boot = "3.3.0"
lombok = "1.18.32"
guava = "33.2.0-jre"
mapstruct = "1.5.5.Final"

[libraries]
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-boot-starter-test = { module = "org.springframework.boot:spring-boot-starter-test" }
lombok = { module = "org.projectlombok:lombok", version.ref = "lombok" }
guava = { module = "com.google.guava:guava", version.ref = "guava" }
mapstruct = { module = "org.mapstruct:mapstruct", version.ref = "mapstruct" }
mapstruct-processor = { module = "org.mapstruct:mapstruct-processor", version.ref = "mapstruct" }

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
spring-dependency-management = { id = "io.spring.dependency-management", version = "1.1.5" }

[bundles]
testing = ["spring-boot-starter-test"]
```

```groovy
// build.gradle 使用版本目录
dependencies {
    implementation libs.spring.boot.starter.web
    compileOnly libs.lombok
    annotationProcessor libs.lombok
    implementation libs.guava
    implementation libs.mapstruct
    annotationProcessor libs.mapstruct.processor
    testImplementation libs.bundles.testing
}
```

---

## 8. Maven vs Gradle 对比与选型

### 8.1 全面对比

| 维度 | Maven | Gradle |
|------|-------|--------|
| **配置文件** | pom.xml (XML) | build.gradle (Groovy/Kotlin DSL) |
| **构建模型** | 固定生命周期 | DAG（有向无环图）任务模型 |
| **性能** | 中等 | 快（增量构建 + 构建缓存 + Daemon） |
| **灵活性** | 低（插件扩展） | 高（脚本编程） |
| **约定优于配置** | ✅ 强约束 | ⚠️ 较宽松 |
| **学习成本** | 低 | 中 |
| **IDE 支持** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **依赖冲突解决** | 自动（最短路径） | 自动（最新版本） |
| **生态** | 老牌，市场占有率高 | Android 首选，新项目增多 |

### 8.2 选型建议

```
选 Maven：
  - 团队对 Maven 很熟悉
  - 项目结构简单标准
  - 银行/金融等需要严格规范的项目
  - 不需要高度自定义构建流程

选 Gradle：
  - Android 开发（官方指定）
  - 需要复杂的构建逻辑
  - 大型多模块项目（构建速度优势明显）
  - 团队愿意学习新工具
  - Kotlin 项目

折中方案：
  - 新项目优先考虑 Gradle（Spring Boot 官方生成器默认 Gradle）
  - 存量项目继续 Maven，除非有充分动力迁移
```

---

## 9. 依赖冲突与解决

### 9.1 冲突发现

```bash
# Maven 查看依赖树
mvn dependency:tree -Dverbose   # 展示冲突
mvn dependency:tree -Dincludes=com.google.guava  # 只看特定

# Gradle 查看依赖树
gradle dependencies --configuration runtimeClasspath
```

### 9.2 冲突解决策略

```xml
<!-- 策略 1：dependencyManagement 统一版本（推荐） -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.17.1</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 策略 2：exclusion 排除 -->
<dependency>
    <groupId>some-lib</groupId>
    <artifactId>some-lib</artifactId>
    <exclusions>
        <exclusion>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>  <!-- 排除这个库带的旧 guava -->
        </exclusion>
    </exclusions>
</dependency>
<!-- 手动引入新版本 -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.2.0-jre</version>
</dependency>

<!-- 策略 3：optional（可选依赖，不传递） -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <optional>true</optional>  <!-- 只有明确声明才引入 -->
</dependency>
```

```groovy
// Gradle 冲突解决
configurations.all {
    resolutionStrategy {
        // 强制版本
        force 'com.google.guava:guava:33.2.0-jre'
        // 使用最新版本而非默认的最老版本
        preferProjectModules()
        // 缓存动态版本的时间
        cacheDynamicVersionsFor 10, 'minutes'
    }
}
```

---

## 10. 私有仓库与 Nexus/Artifactory

### 10.1 仓库体系

```
仓库类型：
  1. 本地仓库（~/.m2/repository）
  2. 远程私有仓库（Nexus / Artifactory / 阿里云效仓库）
  3. 远程公共仓库（Maven Central / JCenter(已关闭)）

优先级：
  本地仓库 → 私有仓库 → 中央仓库

公司级架构：
  开发机 → Nexus (公司内部私服，内网)
            ↓ 代理
           Maven Central（外网，只有 Nexus 需要外网访问）
  
  好处：
  - 内网访问快（不用每次都去外网下载）
  - 缓存外网包（某个版本下架也不影响公司项目）
  - 发布内部包（私有 SDK、Common 库）
```

### 10.2 settings.xml 配置

```xml
<!-- ~/.m2/settings.xml -->
<settings>
    <!-- 本地仓库路径 -->
    <localRepository>D:/maven/repository</localRepository>

    <!-- 仓库认证 -->
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>admin</username>
            <password>${env.NEXUS_PASSWORD}</password>
        </server>
    </servers>

    <!-- 镜像（加速下载） -->
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <name>阿里云 Maven 镜像</name>
            <url>https://maven.aliyun.com/repository/public</url>
            <mirrorOf>central</mirrorOf>
        </mirror>
        <mirror>
            <id>tencent</id>
            <name>腾讯云 Maven 镜像</name>
            <url>https://mirrors.cloud.tencent.com/nexus/repository/maven-public/</url>
            <mirrorOf>central</mirrorOf>
        </mirror>
    </mirrors>

    <!-- Profile（仓库配置） -->
    <profiles>
        <profile>
            <id>nexus</id>
            <repositories>
                <repository>
                    <id>nexus</id>
                    <url>http://nexus.company.com/repository/maven-public/</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>true</enabled></snapshots>
                </repository>
            </repositories>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>nexus</activeProfile>
    </activeProfiles>
</settings>
```

---

## 11. BOM 与版本管理最佳实践

### 11.1 BOM 详解

```xml
<!-- BOM (Bill of Materials) -->
<!-- 作用：统一管理一组依赖的版本 -->
<!-- 特点：只声明版本，不实际引入依赖 -->

<!-- Spring Boot BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.3.0</version>
            <type>pom</type>
            <scope>import</scope>    <!-- 关键：import scope 引入 BOM -->
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 然后在 dependencies 中不用写版本号 -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <!-- 版本号由 BOM 管控，无需填写 -->
    </dependency>
</dependencies>

<!-- 可以引入多个 BOM -->
<!-- 优先级：先声明的优先（与依赖仲裁相反！） -->
```

### 11.2 自定义 BOM

```xml
<!-- 公司内部 BOM 项目（统一管控所有内部和第三方依赖版本） -->
<!-- packaging 必须是 pom -->
<project>
    <groupId>com.company</groupId>
    <artifactId>company-bom</artifactId>
    <version>2024.06</version>
    <packaging>pom</packaging>

    <dependencyManagement>
        <dependencies>
            <!-- 导入 Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>3.3.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- 内部模块 -->
            <dependency>
                <groupId>com.company</groupId>
                <artifactId>company-common</artifactId>
                <version>2.1.0</version>
            </dependency>

            <!-- 统一管控第三方版本 -->
            <dependency>
                <groupId>com.google.guava</groupId>
                <artifactId>guava</artifactId>
                <version>33.2.0-jre</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

---

## 12. 常见问题与面试题

### Q1: Maven 的依赖仲裁机制是什么？

```
两个核心原则：
1. 最短路径优先
   A → B → C (2.0)  → 路径长度 2
   A → D → E → C (1.0)  → 路径长度 3
   结果：C (2.0) 生效

2. 路径相同 → 先声明优先
   A → B → C (2.0)  → 路径长度 2（B 在 pom.xml 中先声明）
   A → D → C (1.0)  → 路径长度 2（D 在 pom.xml 中后声明）
   结果：C (2.0) 生效

手动干预：
  - dependencyManagement 强制指定版本（最高优先级）
  - exclusion 排除不想要的
  - 直接在当前项目声明想要的版本
```

### Q2: Maven Wrapper 有什么用？

```
问题：团队成员 Maven 版本不一致或未安装 Maven

解决：
  mvn wrapper:wrapper    生成 .mvn/wrapper/ + mvnw/mvnw.cmd
  ./mvnw clean package   自动下载指定版本的 Maven 来执行构建
  从此不需要预装 Maven！

  同理，Gradle Wrapper：gradle wrapper
```

### Q3: Gradle 的 implementation vs api 的区别？

```groovy
// api：依赖传递（类似 Maven compile）
dependencies {
    api 'com.google.guava:guava:33.0-jre'
}
// 项目 B 依赖当前项目 → B 也能在编译时看到 guava
// 问题：修改 guava 会导致 B 重编译，构建变慢

// implementation：依赖不传递（推荐）
dependencies {
    implementation 'com.google.guava:guava:33.0-jre'
}
// 项目 B 依赖当前项目 → B 编译时看不到 guava（运行时能看到）
// 好处：编译隔离，构建更快，解耦

// 原则：能用 implementation 绝不用 api
```

> **上一篇：** [01-Java基础与核心特性](./01-Java基础与核心特性.md)
>
> **下一篇：** [03-Web基础与RESTful API设计](./03-Web基础与RESTful%20API设计.md)
