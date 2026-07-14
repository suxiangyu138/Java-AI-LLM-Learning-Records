# 构建工具: Maven & Gradle

> **"Build tools are the assembly line of software production — invisible when perfect, catastrophic when broken."**  
> 版本: Maven 3.9+ / Gradle 8.x / JDK 17+  
> 适用: 企业级 Java 项目构建

---

## Table of Contents

1. [构建工具演进史](#1-构建工具演进史)
2. [Maven 核心概念](#2-maven-核心概念)
3. [Maven 依赖管理](#3-maven-依赖管理)
4. [Maven 生命周期与插件](#4-maven-生命周期与插件)
5. [Maven 多模块项目](#5-maven-多模块项目)
6. [Maven Profile 与仓库](#6-maven-profile-与仓库)
7. [Maven 最佳实践](#7-maven-最佳实践)
8. [Gradle 核心概念](#8-gradle-核心概念)
9. [Gradle 依赖管理](#9-gradle-依赖管理)
10. [Gradle 多项目与性能](#10-gradle-多项目与性能)
11. [Maven vs Gradle](#11-maven-vs-gradle)
12. [企业级构建流水线](#12-企业级构建流水线)
13. [语义化版本与发布](#13-语义化版本与发布)
14. [依赖安全与漏洞扫描](#14-依赖安全与漏洞扫描)
15. [面试题精选](#15-面试题精选)

---

## 1. 构建工具演进史

### 1.1 Java 构建工具发展时间线

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Java 构建工具演进史                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  2000: Ant                                                          │
│  ├── XML 配置文件 (build.xml)                                       │
│  ├── 声明式任务 (target, task)                                     │
│  ├── 依赖管理: ❌ 需要手动管理 jar                                  │
│  ├── 标准化: ❌ 每个项目 build.xml 完全不同                         │
│  └── 特点: 灵活但无约定                                             │
│                                                                     │
│  2004: Maven                                                        │
│  ├── XML 配置文件 (pom.xml)                                         │
│  ├── 约定优于配置 (Convention over Configuration)                    │
│  ├── 依赖管理: ✅ 自动下载 + 传递性依赖                             │
│  ├── 标准化: ✅ 统一的项目结构 + 生命周期                           │
│  ├── 仓库: ✅ Central Repository (中央仓库)                         │
│  └── 特点: 标准化但冗长                                             │
│                                                                     │
│  2012: Gradle                                                        │
│  ├── Groovy DSL / Kotlin DSL 配置                                   │
│  ├── 基于任务图 (Task Graph)                                         │
│  ├── 依赖管理: ✅ 更灵活的作用域                                    │
│  ├── 性能: ✅ 增量编译 + 构建缓存 + 并行执行                        │
│  ├── 兼容: ✅ 兼容 Maven 仓库                                       │
│  └── 特点: 快速、灵活、表达力强                                      │
│                                                                     │
│  市场占有率趋势:                                                     │
│  2010: Ant 60% | Maven 35% | Gradle 5%                              │
│  2015: Ant 20% | Maven 60% | Gradle 20%                             │
│  2020: Ant 5%  | Maven 50% | Gradle 45%                             │
│  2024: Ant 2%  | Maven 45% | Gradle 53% (新项目 Gradle 占优)        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 为什么需要构建工具

```java
// ========== 没有构建工具的 Java 项目 ==========
// 手动编译: javac -cp lib/* -d build src/**/*.java
// 手动打包: jar cf app.jar -C build .
// 手动运行: java -cp app.jar:lib/* com.example.Main
// 手动测试: java -cp test.jar:lib/*:build org.junit.runner.JUnitCore ...
// 
// 痛点:
// 1. 依赖管理: 手动下载jar, 管理版本, 处理冲突
// 2. 构建步骤: 每次都要执行一长串命令
// 3. 可重复性: 不同机器效果不同
// 4. 团队协作: 每个人需要手动配置环境

// ========== 使用 Maven/Gradle ==========
// mvn clean package         → 编译 + 测试 + 打包
// mvn install               → 发布到本地仓库
// mvn deploy                → 发布到远程仓库
// gradle build               → 编译 + 测试 + 打包
// gradle publish             → 发布到仓库
```

---

## 2. Maven 核心概念

### 2.1 POM (Project Object Model)

```xml
<!-- ========== pom.xml 基本结构 ========== -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <!-- POM 版本 (固定 4.0.0) -->
    <modelVersion>4.0.0</modelVersion>
    
    <!-- ========== GAV 坐标 (项目唯一标识) ========== -->
    <groupId>com.example</groupId>           <!-- 组织/公司标识 -->
    <artifactId>my-app</artifactId>          <!-- 项目/模块名称 -->
    <version>1.0.0-SNAPSHOT</version>        <!-- 版本号 -->
    <packaging>jar</packaging>               <!-- 打包类型: jar, war, pom, ear -->
    
    <!-- ========== 项目信息 ========== -->
    <name>My Application</name>
    <description>Enterprise-grade Spring Boot application</description>
    <url>https://example.com</url>
    
    <inceptionYear>2024</inceptionYear>
    
    <organization>
        <name>Example Corp</name>
        <url>https://example.com</url>
    </organization>
    
    <licenses>
        <license>
            <name>Apache License, Version 2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0</url>
        </license>
    </licenses>
    
    <!-- ========== 属性 (集中管理版本号) ========== -->
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- 依赖版本 -->
        <spring-boot.version>3.2.0</spring-boot.version>
        <mybatis.version>3.5.14</mybatis.version>
        <guava.version>33.0.0-jre</guava.version>
    </properties>
    
    <!-- ========== 依赖管理 (继承时子模块可省略版本) ========== -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <!-- ========== 依赖声明 ========== -->
    <dependencies>
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <!-- 版本从 dependencyManagement 继承 -->
        </dependency>
        
        <!-- 单元测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <!-- ========== 构建配置 ========== -->
    <build>
        <finalName>${project.artifactId}-${project.version}</finalName>
        
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2.2 GAV 坐标详解

```xml
<!-- ========== GAV = GroupId:ArtifactId:Version ========== -->

<!-- GroupId — 组织标识 (通常为公司域名倒写) -->
<groupId>com.example</groupId>
<groupId>org.springframework.boot</groupId>
<groupId>com.baomidou</groupId>
<groupId>org.mybatis.spring.boot</groupId>

<!-- ArtifactId — 项目/模块名 -->
<artifactId>my-app</artifactId>
<artifactId>spring-boot-starter-web</artifactId>
<artifactId>mybatis-plus-spring-boot3-starter</artifactId>

<!-- Version — 版本号 -->
<version>1.0.0-SNAPSHOT</version>  <!-- SNAPSHOT: 开发版 -->
<version>1.0.0-M1</version>        <!-- M: Milestone 里程碑 -->
<version>1.0.0-RC1</version>       <!-- RC: Release Candidate 候选版 -->
<version>1.0.0-RELEASE</version>   <!-- 正式版 -->
<version>1.0.0</version>           <!-- 正式版简写 -->

<!-- 版本号规范:
    主版本.次版本.修订版本-限定符
    
    主版本 (Major): 不兼容的 API 修改
    次版本 (Minor): 向下兼容的功能增加
    修订版本 (Patch): 向下兼容的问题修复
    限定符: SNAPSHOT, Alpha, Beta, RC, RELEASE -->
```

### 2.3 标准目录布局

```
# ========== Maven 标准目录结构 (Convention over Configuration) ==========

my-app/
├── pom.xml                              # Maven 项目描述
│
├── src/
│   ├── main/                            # 主代码
│   │   ├── java/                        # Java 源码
│   │   │   └── com/example/
│   │   │       ├── Application.java
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── entity/
│   │   │       └── config/
│   │   │
│   │   ├── resources/                   # 资源文件
│   │   │   ├── application.yml
│   │   │   ├── logback-spring.xml
│   │   │   ├── mapper/                  # MyBatis XML
│   │   │   ├── i18n/                    # 国际化
│   │   │   └── static/                  # 静态资源
│   │   │       ├── css/
│   │   │       ├── js/
│   │   │       └── images/
│   │   │
│   │   └── webapp/                      # Web 应用 (war 包)
│   │       └── WEB-INF/
│   │
│   └── test/                            # 测试代码
│       ├── java/                        # 测试源码
│       │   └── com/example/
│       │       ├── controller/
│       │       └── service/
│       └── resources/                   # 测试资源
│           ├── application-test.yml
│           └── test-data.sql
│
├── target/                              # 构建输出 (自动生成)
│   ├── classes/
│   ├── test-classes/
│   ├── generated-sources/
│   └── *.jar / *.war
│
├── .mvn/                                # Maven Wrapper
│   └── wrapper/
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties
├── mvnw                                 # Maven Wrapper (Unix)
├── mvnw.cmd                             # Maven Wrapper (Windows)
│
└── README.md
```

---

## 3. Maven 依赖管理

### 3.1 依赖作用域 (Scope)

```xml
<!-- ========== 6 种依赖作用域 ========== -->

<!-- 1. compile (默认): 所有阶段都可用, 会包含在包中 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <scope>compile</scope>  <!-- 默认, 可省略 -->
</dependency>

<!-- 2. provided: 编译时需要, 运行时由 JDK/容器提供 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>  <!-- IDE 在编译时使用, 打包时排除 -->
</dependency>

<!-- Tomcat 由 Spring Boot 内嵌, 但如果部署到外部 Tomcat: -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>  <!-- 外部 Tomcat 提供 -->
</dependency>

<!-- 3. runtime: 运行/测试时需要, 编译时不需要 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>  <!-- 编译时用 JDBC 接口, 运行时需要驱动 -->
</dependency>

<!-- 4. test: 仅在测试时使用 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>

<!-- 5. system: 本地 jar 包 (不推荐, 破坏可重复性) -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>local-lib</artifactId>
    <version>1.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/local-lib.jar</systemPath>
</dependency>

<!-- 6. import: 仅在 dependencyManagement 中使用 (BOM) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-dependencies</artifactId>
    <version>2023.0.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

### 3.2 传递性依赖

```
# ========== 传递性依赖 ==========
# Maven 会自动下载依赖的依赖 (传递性依赖)

# 示例: 添加 spring-boot-starter-web
# 会自动依赖:
#   spring-boot-starter-web
#     ├── spring-boot-starter            (核心)
#     │    ├── spring-boot               (自动配置)
#     │    ├── spring-boot-autoconfigure
#     │    ├── snakeyaml                 (YAML 解析)
#     │    └── jakarta.annotation-api
#     ├── spring-boot-starter-tomcat     (内嵌 Tomcat)
#     │    ├── tomcat-embed-core
#     │    ├── tomcat-embed-el
#     │    └── tomcat-embed-websocket
#     ├── spring-web                     (Spring MVC)
#     ├── spring-webmvc
#     └── spring-boot-starter-json       (Jackson)
#          ├── jackson-databind
#          ├── jackson-datatype-jdk8
#          └── jackson-datatype-jsr310

# ========== 依赖仲裁 (Dependency Mediation) ==========
# 当同一依赖出现不同版本时, Maven 处理规则:
# 1. 最短路径优先 (nearest wins)
# 2. 第一声明优先 (路径相同时, 先声明的版本胜出)

# 示例: A → B → C → D 2.0
#       A → E → D 1.0
# 结果: D 1.0 (路径更短)

# 示例: A → B → D 1.0
#       A → C → D 2.0
# 结果: D 1.0 (B 先声明)

# ========== 查看依赖树 ==========
# mvn dependency:tree
# mvn dependency:tree -Dincludes=com.fasterxml.jackson*
# mvn dependency:tree -Dverbose  (详细模式, 含冲突)
```

### 3.3 排除与可选依赖

```xml
<!-- ========== 排除传递性依赖 ========== -->
<!-- 当传递性依赖引入不需要的 jar 时, 手动排除 -->

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <!-- 排除内嵌 Tomcat (使用 Undertow) -->
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
        
        <!-- 排除 Logback (使用 Log4j2) -->
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
        
        <!-- 排除特定传递性依赖 -->
        <exclusion>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- ========== 可选依赖 ========== -->
<!-- 依赖方声明 optional=true, 使用者需要显式添加才能使用 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-lib</artifactId>
    <version>1.0.0</version>
    <optional>true</optional>
    <!-- 声明为可选: 使用者不会自动引入此依赖 -->
</dependency>

<!-- 使用方需要显式添加才能使用: -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-lib</artifactId>
    <version>1.0.0</version>
    <!-- 必须显式声明 -->
</dependency>
```

### 3.4 BOM (Bill of Materials)

```xml
<!-- ========== BOM 统一版本管理 ========== -->
<!-- BOM 是集中管理版本号的标准方式 -->

<!-- 方式 1: 继承 spring-boot-starter-parent -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>
<!-- 子 pom 中不需要写任何版本号 -->

<!-- 方式 2: 使用 BOM import (没有 parent 时) -->
<dependencyManagement>
    <dependencies>
        <!-- Spring Boot BOM -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- Spring Cloud BOM -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- 其他 BOM -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-bom</artifactId>
            <version>3.5.5</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 方式 3: 自定义 BOM (适用于公司内部) -->
<!-- 公司级 BOM 项目: company-bom/pom.xml -->
<project>
    <groupId>com.example</groupId>
    <artifactId>company-bom</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>3.2.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- 公司内部库版本管理 -->
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>company-common</artifactId>
                <version>2.1.0</version>
            </dependency>
            
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>company-logging</artifactId>
                <version>1.3.0</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>

<!-- 使用 BOM: -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>company-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>company-common</artifactId>
        <!-- 版本从 BOM 继承 -->
    </dependency>
</dependencies>
```

### 3.5 依赖冲突解决

```xml
<!-- ========== 依赖冲突排查流程 ========== -->

<!-- 步骤 1: 查看依赖树 -->
<!-- mvn dependency:tree > deps.txt -->

<!-- 步骤 2: 找到冲突 -->
<!-- 输出中会显示: (version managed from 2.0.0 to 1.0.0) 等提示 -->

<!-- 步骤 3: 解决方案 -->

<!-- 方案 A: 在 dependencyManagement 中显式指定版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.16.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 方案 B: 排除传递性依赖 -->
<dependency>
    <groupId>some.library</groupId>
    <artifactId>some-artifact</artifactId>
    <exclusions>
        <exclusion>
            <groupId>conflicting.group</groupId>
            <artifactId>conflicting-artifact</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 方案 C: 使用 enforcer 插件强制版本收敛 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>3.4.1</version>
    <executions>
        <execution>
            <id>enforce-dependency-convergence</id>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <dependencyConvergence/>
                    <!-- 版本冲突时构建失败 -->
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 4. Maven 生命周期与插件

### 4.1 三套生命周期

```
# ========== Maven 三套生命周期 ==========

# 1. clean 生命周期 (清理项目)
#    pre-clean → clean → post-clean
#    mvn clean          # 删除 target 目录

# 2. default 生命周期 (主要生命周期)
#    validate          # 验证项目配置是否正确
#    initialize        # 初始化构建状态
#    generate-sources  # 生成源代码
#    process-sources   # 处理源代码
#    generate-resources# 生成资源文件
#    process-resources # 复制资源到输出目录
#    compile           # 编译源代码
#    process-classes   # 处理编译后的文件
#    generate-test-sources
#    process-test-sources
#    generate-test-resources
#    process-test-resources
#    test-compile      # 编译测试代码
#    process-test-classes
#    test              # 运行测试
#    prepare-package   # 打包前准备
#    package           # 打包 (jar/war)
#    pre-integration-test
#    integration-test  # 集成测试
#    post-integration-test
#    verify            # 验证包是否有效
#    install           # 安装到本地仓库
#    deploy            # 部署到远程仓库

# 3. site 生命周期 (生成站点文档)
#    pre-site → site → post-site → site-deploy
#    mvn site           # 生成项目文档

# ========== 常用命令组合 ==========
mvn clean                # 清理
mvn compile              # 编译
mvn test                 # 测试
mvn package              # 打包
mvn install              # 安装到本地仓库
mvn deploy               # 部署到远程仓库
mvn clean package        # 清理 + 打包
mvn clean install        # 清理 + 安装
mvn clean verify         # 清理 + 验证 (含集成测试)
mvn dependency:tree      # 查看依赖树
mvn versions:display-dependency-updates  # 检查依赖更新

# ========== 跳过测试 ==========
mvn package -DskipTests        # 跳过编译测试代码
mvn package -Dmaven.test.skip=true  # 跳过测试全流程 (不推荐)
mvn verify -Dintegration.test.skip=true  # 跳过集成测试
```

### 4.2 Phases and Goals

```
# ========== Phase vs Goal ==========
#
# Phase (阶段): 生命周期中的步骤, 如 compile, test, package
# Goal (目标): 插件中的具体任务, 如 compiler:compile, surefire:test
#
# Phase 绑定到 Plugin Goal:
# compile     → compiler:compile
# test        → surefire:test
# package     → jar:jar 或 spring-boot:repackage 等

# ========== 查看 Phase-Goal 绑定 ==========
# mvn help:describe -Dcmd=compile
# mvn help:describe -Dcmd=spring-boot:run
```

### 4.3 核心插件

```xml
<!-- ========== maven-compiler-plugin — 编译配置 ========== -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>17</source>          <!-- JDK 版本 -->
        <target>17</target>
        <encoding>UTF-8</encoding>
        <parameters>true</parameters> <!-- 编译时保留方法参数名 (Spring 需要) -->
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>1.18.30</version>
            </path>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.5.5.Final</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>

<!-- ========== maven-surefire-plugin — 测试执行 ========== -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.2</version>
    <configuration>
        <!-- 包含/排除测试类 -->
        <includes>
            <include>**/*Test.java</include>
            <include>**/*Tests.java</include>
        </includes>
        <excludes>
            <exclude>**/*IntegrationTest.java</exclude>
        </excludes>
        <!-- 并行测试 -->
        <parallel>classes</parallel>
        <threadCount>4</threadCount>
        <!-- 测试报告 -->
        <reportsDirectory>${project.build.directory}/test-reports</reportsDirectory>
    </configuration>
</plugin>

<!-- ========== maven-failsafe-plugin — 集成测试 ========== -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.2.2</version>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <includes>
            <include>**/*IntegrationTest.java</include>
            <include>**/*IT.java</include>
        </includes>
    </configuration>
</plugin>

<!-- ========== spring-boot-maven-plugin — Spring Boot 打包 ========== -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <!-- 可执行 jar -->
        <executable>true</executable>
        <!-- 排除 devtools -->
        <excludes>
            <exclude>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-devtools</artifactId>
            </exclude>
        </excludes>
        <!-- 指定主类 -->
        <mainClass>com.example.Application</mainClass>
        <!-- Docker 镜像配置 -->
        <image>
            <name>${project.groupId}/${project.artifactId}:${project.version}</name>
        </image>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>repackage</goal>      <!-- 将 jar 重新打包为可执行 jar -->
                <goal>build-info</goal>     <!-- 生成 build-info.properties -->
            </goals>
        </execution>
    </executions>
</plugin>

<!-- ========== maven-jar-plugin — JAR 包配置 ========== -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>3.3.0</version>
    <configuration>
        <archive>
            <manifest>
                <addClasspath>true</addClasspath>
                <mainClass>com.example.Application</mainClass>
            </manifest>
            <manifestEntries>
                <Implementation-Title>${project.name}</Implementation-Title>
                <Implementation-Version>${project.version}</Implementation-Version>
                <Build-Time>${maven.build.timestamp}</Build-Time>
            </manifestEntries>
        </archive>
        <excludes>
            <exclude>**/*.xml</exclude>  <!-- 排除某些文件 -->
        </excludes>
    </configuration>
</plugin>

<!-- ========== maven-assembly-plugin — 自定义打包 ========== -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <version>3.6.0</version>
    <configuration>
        <descriptors>
            <descriptor>src/assembly/dist.xml</descriptor>
        </descriptors>
        <finalName>${project.artifactId}-${project.version}</finalName>
        <appendAssemblyId>false</appendAssemblyId>
    </configuration>
    <executions>
        <execution>
            <id>make-assembly</id>
            <phase>package</phase>
            <goals>
                <goal>single</goal>
            </goals>
        </execution>
    </executions>
</plugin>

<!-- assembly 描述符: src/assembly/dist.xml -->
<!-- 打包为包含 bin, conf, lib 目录的分发包 -->
<assembly>
    <id>dist</id>
    <formats>
        <format>tar.gz</format>
        <format>zip</format>
    </formats>
    <fileSets>
        <fileSet>
            <directory>${project.basedir}/scripts</directory>
            <outputDirectory>bin</outputDirectory>
            <fileMode>0755</fileMode>
        </fileSet>
        <fileSet>
            <directory>${project.basedir}/src/main/resources</directory>
            <outputDirectory>conf</outputDirectory>
            <excludes>
                <exclude>application.yml</exclude>
            </excludes>
        </fileSet>
        <fileSet>
            <directory>${project.build.directory}</directory>
            <outputDirectory>lib</outputDirectory>
            <includes>
                <include>*.jar</include>
            </includes>
        </fileSet>
    </fileSets>
</assembly>
```

---

## 5. Maven 多模块项目

### 5.1 多模块结构

```
# ========== 企业级多模块项目结构 ==========

my-platform/
├── pom.xml                      # 父 POM (聚合 + 依赖管理)
│
├── platform-common/             # 公共模块 (工具类, 通用实体)
│   └── pom.xml
│
├── platform-dao/                # 数据访问层
│   └── pom.xml
│
├── platform-service/            # 业务逻辑层
│   └── pom.xml
│
├── platform-web/                # Web 接口层 (Spring Boot)
│   └── pom.xml
│
├── platform-api/                # 对外 API 接口 (Feign 客户端)
│   └── pom.xml
│
├── platform-batch/              # 批处理模块 (Spring Batch)
│   └── pom.xml
│
└── platform-starters/           # 自定义 Starter 模块
    ├── pom.xml
    ├── redis-starter/
    ├── security-starter/
    └── logging-starter/
```

### 5.2 父 POM 配置

```xml
<!-- ========== 父 POM (my-platform/pom.xml) ========== -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    
    <!-- 父 POM 坐标 -->
    <groupId>com.example</groupId>
    <artifactId>my-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>  <!-- 父模块必须是 pom 类型 -->
    
    <name>My Platform Parent</name>
    
    <!-- ========== 聚合子模块 ========== -->
    <!-- 定义哪些子模块属于这个项目 -->
    <modules>
        <module>platform-common</module>
        <module>platform-dao</module>
        <module>platform-service</module>
        <module>platform-web</module>
        <module>platform-api</module>
        <module>platform-batch</module>
    </modules>
    
    <!-- ========== 继承自 Spring Boot Parent ========== -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    
    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- 内部模块版本 -->
        <platform.version>${project.version}</platform.version>
        
        <!-- 三方库版本 -->
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <jjwt.version>0.12.3</jjwt.version>
        <hutool.version>5.8.25</hutool.version>
    </properties>
    
    <!-- ========== 依赖管理 (子模块不需要写版本) ========== -->
    <dependencyManagement>
        <dependencies>
            <!-- 内部模块 -->
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>platform-common</artifactId>
                <version>${platform.version}</version>
            </dependency>
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>platform-dao</artifactId>
                <version>${platform.version}</version>
            </dependency>
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>platform-service</artifactId>
                <version>${platform.version}</version>
            </dependency>
            
            <!-- 第三方库 -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <!-- ========== 子模块共享依赖 ========== -->
    <!-- 所有子模块都自动继承这些依赖 -->
    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 5.3 子模块 POM

```xml
<!-- ========== platform-common/pom.xml ========== -->
<project>
    <modelVersion>4.0.0</modelVersion>
    
    <!-- 父 POM 引用 -->
    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    
    <!-- 当前模块坐标 (无 groupId, 继承父 POM) -->
    <artifactId>platform-common</artifactId>
    <packaging>jar</packaging>
    
    <dependencies>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    </dependencies>
</project>

<!-- ========== platform-web/pom.xml (Spring Boot 入口) ========== -->
<project>
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    
    <artifactId>platform-web</artifactId>
    <packaging>jar</packaging>
    
    <dependencies>
        <!-- 内部依赖 -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>platform-service</artifactId>
        </dependency>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>platform-common</artifactId>
        </dependency>
        
        <!-- 外部依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
    
    <!-- web 模块负责打包为可执行 jar -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>

<!-- ========== 模块依赖关系 ========== -->
<!-- platform-web → platform-service → platform-dao → platform-common -->
<!--                                                       ↑
<!--                                           (实体类, 工具类) -->
```

### 5.4 构建顺序与依赖

```xml
<!-- ========== 构建顺序 ========== -->
<!-- Maven 会自动计算模块的构建顺序:

1. 分析模块间的依赖关系
   platform-common → (无依赖)                                  → 1st
   platform-dao    → platform-common                            → 2nd
   platform-service → platform-common, platform-dao            → 3rd
   platform-web     → platform-common, platform-service        → 4th

2. 执行命令:
   mvn clean install                    # 所有模块按顺序构建
   mvn clean install -pl platform-web   # 只构建 web 模块 (及其依赖)
   mvn clean install -am -pl platform-web  # 构建 web 及依赖模块
   
   参数:
   -pl, --projects        : 指定要构建的模块
   -am, --also-make       : 同时构建依赖的模块
   -amd, --also-make-dependents : 同时构建依赖当前模块的模块
-->

<!-- ========== 可用构建命令 ========== -->
# 全量构建
mvn clean package                    # 构建所有模块
mvn clean install                    # 构建并安装到本地仓库

# 选择性构建
mvn -pl platform-web package         # 只构建 web 模块
mvn -pl platform-web -am package     # 构建 web 及其依赖

# 跳过测试
mvn clean package -DskipTests

# 离线模式
mvn clean package -o                 # 离线 (不检查远程仓库)

# 多线程
mvn clean package -T 4               # 4 线程并行
mvn clean package -T 1C              # 每个 CPU 核心 1 线程

# 指定 profile
mvn clean package -P dev

# Debug 模式
mvn clean package -X                 # 调试输出
```

---

## 6. Maven Profile 与仓库

### 6.1 Profiles

```xml
<!-- ========== Maven Profile 环境配置 ========== -->
<profiles>
    <!-- 开发环境 (默认) -->
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <environment>dev</environment>
            <db.url>jdbc:mysql://localhost:3306/dev_db</db.url>
        </properties>
    </profile>
    
    <!-- 测试环境 -->
    <profile>
        <id>test</id>
        <activation>
            <property>
                <name>env</name>
                <value>test</value>
            </property>
        </activation>
        <properties>
            <environment>test</environment>
            <db.url>jdbc:mysql://test-db:3306/test_db</db.url>
        </properties>
    </profile>
    
    <!-- 生产环境 -->
    <profile>
        <id>prod</id>
        <activation>
            <activeByDefault>false</activeByDefault>
        </activation>
        <properties>
            <environment>prod</environment>
            <db.url>jdbc:mysql://prod-db:3306/prod_db</db.url>
        </properties>
        <!-- 生产环境特殊构建配置 -->
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                        <skip>true</skip>  <!-- 生产构建跳过测试 -->
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
    
    <!-- 集成测试 (需要额外的测试数据库) -->
    <profile>
        <id>integration</id>
        <dependencies>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>mysql</artifactId>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </profile>

    <!-- 触发方式 (多种) -->
    <!-- activation.activeByDefault — 默认激活 -->
    <!-- activation.jdk — 匹配 JDK 版本 -->
    <!-- activation.os — 匹配 OS -->
    <!-- activation.property — 匹配系统属性或环境变量 -->
</profiles>

<!-- 使用方式: -->
# mvn clean package -P dev           # 激活 dev profile
# mvn clean package -P test          # 激活 test profile
# mvn clean package -P prod          # 激活 prod profile
# mvn clean package -D env=test      # 通过属性激活
# mvn clean package -P dev,integration  # 同时激活多个
```

### 6.2 仓库配置

```xml
<!-- ========== settings.xml — Maven 全局配置 ========== -->
<!-- 位置: ~/.m2/settings.xml -->

<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <!-- 本地仓库位置 (默认 ~/.m2/repository) -->
    <localRepository>D:/maven/repository</localRepository>
    
    <!-- 镜像配置 (加速下载) -->
    <mirrors>
        <!-- 阿里云镜像 (国内必配) -->
        <mirror>
            <id>aliyun-maven</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Maven Mirror</name>
            <url>https://maven.aliyun.com/repository/central</url>
        </mirror>
        
        <!-- 华为云镜像 -->
        <mirror>
            <id>huawei-maven</id>
            <mirrorOf>central</mirrorOf>
            <name>Huawei Maven Mirror</name>
            <url>https://repo.huaweicloud.com/repository/maven</url>
        </mirror>
        
        <!-- 腾讯云镜像 -->
        <mirror>
            <id>tencent-maven</id>
            <mirrorOf>central</mirrorOf>
            <name>Tencent Maven Mirror</name>
            <url>https://mirrors.tencent.com/nexus/repository/maven-public</url>
        </mirror>
    </mirrors>
    
    <!-- 服务器认证 (部署到私有仓库时需要) -->
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>admin</username>
            <password>password</password>
        </server>
        <server>
            <id>nexus-snapshots</id>
            <username>admin</username>
            <password>password</password>
        </server>
        
        <!-- 使用加密密码 -->
        <!-- mvn --encrypt-password password -->
        <!-- <password>{jBCrypt}$2a$10$...</password> -->
    </servers>
    
    <!-- 全局 profile (包含私有仓库地址) -->
    <profiles>
        <profile>
            <id>nexus</id>
            <repositories>
                <repository>
                    <id>nexus-releases</id>
                    <url>https://nexus.example.com/repository/maven-releases</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>false</enabled></snapshots>
                </repository>
                <repository>
                    <id>nexus-snapshots</id>
                    <url>https://nexus.example.com/repository/maven-snapshots</url>
                    <releases><enabled>false</enabled></releases>
                    <snapshots><enabled>true</enabled></snapshots>
                </repository>
            </repositories>
            
            <!-- 插件仓库 -->
            <pluginRepositories>
                <pluginRepository>
                    <id>nexus-releases</id>
                    <url>https://nexus.example.com/repository/maven-releases</url>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>
    
    <!-- 激活 profile -->
    <activeProfiles>
        <activeProfile>nexus</activeProfile>
    </activeProfiles>
</settings>

<!-- ========== pom.xml 中的仓库配置 ========== -->
<repositories>
    <repository>
        <id>aliyun</id>
        <name>Alibaba Cloud</name>
        <url>https://maven.aliyun.com/repository/public</url>
        <releases><enabled>true</enabled></releases>
        <snapshots><enabled>false</enabled></snapshots>
    </repository>
    
    <!-- 私有 Nexus 仓库 -->
    <repository>
        <id>company-nexus</id>
        <name>Company Nexus</name>
        <url>https://nexus.company.com/repository/maven-public/</url>
        <releases><enabled>true</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>

<!-- ========== 发布配置 ========== -->
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <name>Releases</name>
        <url>https://nexus.example.com/repository/maven-releases</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <name>Snapshots</name>
        <url>https://nexus.example.com/repository/maven-snapshots</url>
    </snapshotRepository>
</distributionManagement>
```

### 6.3 资源过滤

```xml
<!-- ========== 资源过滤 (Profile 替换变量) ========== -->
<!-- 把 application.yml 中的 @db.url@ 替换为 profile 中的值 -->

<build>
    <!-- 启用资源过滤的目录 -->
    <resources>
        <resource>
            <directory>src/main/resources</directory>
            <filtering>true</filtering>  <!-- 启用过滤 (变量替换) -->
            <includes>
                <include>application*.yml</include>
                <include>**/*.properties</include>
            </includes>
            <excludes>
                <exclude>static/**</exclude>
                <exclude>templates/**</exclude>
            </excludes>
        </resource>
        <!-- 不进行过滤的资源 -->
        <resource>
            <directory>src/main/resources</directory>
            <filtering>false</filtering>
            <includes>
                <include>static/**</include>
                <include>templates/**</include>
            </includes>
        </resource>
    </resources>
</build>

<!-- application.yml 中使用 Maven 变量: -->
# @environment@  会被替换为 dev/test/prod
# @project.version@  会被替换为版本号
# @db.url@  会被替换为 profile 中定义的 url

# mvn clean package -P dev → application.yml 中的 @environment@ 变为 dev
```

---

## 7. Maven 最佳实践

### 7.1 Maven Wrapper

```xml
<!-- ========== Maven Wrapper ========== -->
<!-- 让项目自带 Maven 版本, 不需要全局安装 -->
<!-- 生成方式: mvn -N wrapper:wrapper -Dmaven=3.9.6 -->

<!-- 项目根目录会生成: -->
<!-- mvnw             — Unix Shell 脚本 -->
<!-- mvnw.cmd         — Windows CMD 脚本 -->
<!-- .mvn/wrapper/    — Maven Wrapper jar 和配置 -->

<!-- 使用方式 (无需安装 Maven): -->
./mvnw clean package        # Unix/Mac
mvnw.cmd clean package      # Windows

# .mvn/wrapper/maven-wrapper.properties:
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
```

### 7.2 Spring Boot 项目典型配置

```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <!-- 推荐: 继承 Spring Boot Parent -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>My Spring Boot Application</name>

    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- 额外三方库版本 -->
        <hutool.version>5.8.25</hutool.version>
    </properties>

    <dependencies>
        <!-- Core -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Data -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Utilities -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>
        
        <!-- Monitoring -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>build-info</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 7.3 常用 Troubleshooting

```bash
# ========== 常见问题解决 ==========

# 1. 依赖下载失败: 清理本地缓存后重试
rm -rf ~/.m2/repository/com/example/
mvn clean install -U    # -U: 强制更新快照

# 2. 依赖冲突
mvn dependency:tree -Dincludes=com.fasterxml.jackson*
# 在 pom.xml 中添加排除或直接声明版本

# 3. jar 包找不到 (NoClassDefFoundError)
mvn dependency:tree | grep problematic-lib
# 检查 scope 是否正确, 是否是 provided

# 4. 编译错误 (符号找不到)
mvn clean compile
# 检查依赖是否正确, 是否缺少 dependency

# 5. 测试失败
mvn test -Dtest=UserServiceTest      # 运行特定测试
mvn test -Dtest=*ServiceTest         # 运行 Service 测试
mvn test -DfailIfNoTests=false       # 没有测试不报错

# 6. 跳过 checkstyle / pmd
mvn clean package -Dcheckstyle.skip=true

# 7. OOM (OutOfMemory)
# export MAVEN_OPTS="-Xmx1024m -XX:MaxMetaspaceSize=256m"
# 或在 .mvn/jvm.config 中配置:
# -Xmx1024m -XX:MaxMetaspaceSize=256m
```

---

## 8. Gradle 核心概念

### 8.1 Groovy DSL vs Kotlin DSL

```kotlin
// ========== build.gradle (Groovy DSL) ==========
// 传统 Gradle 配置语言

plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '1.0.0-SNAPSHOT'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven { url 'https://maven.aliyun.com/repository/public' }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    runtimeOnly 'com.mysql:mysql-connector-j'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

// ========== build.gradle.kts (Kotlin DSL) — 推荐 ==========
// 类型安全, IDE 智能提示更好

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.aliyun.com/repository/public")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### 8.2 settings.gradle.kts

```kotlin
// ========== settings.gradle.kts ==========
// 项目名称和多模块配置

rootProject.name = "my-platform"

// 子模块声明
include(
    "platform-common",
    "platform-dao",
    "platform-service",
    "platform-web",
    "platform-api"
)

// 构建缓存配置
buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, ".build-cache")
        removeUnusedEntriesAfterDays = 30
    }
}

// 插件管理 (集中管理插件版本)
pluginManagement {
    plugins {
        val springBootVersion: String by settings
        val dependencyManagementVersion: String by settings
        
        id("org.springframework.boot") version springBootVersion
        id("io.spring.dependency-management") version dependencyManagementVersion
    }
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

### 8.3 Gradle 任务模型

```kotlin
// ========== Task 定义 ==========
// Gradle 的核心是任务图 (Task Graph)

tasks.register("hello") {
    doLast {
        println("Hello, Gradle!")
    }
}

tasks.register("printVersion") {
    dependsOn("hello")  // 任务依赖
    doLast {
        println("Version: ${project.version}")
    }
}

// ========== 自定义 Task 类 ==========
abstract class DatabaseMigrationTask : DefaultTask() {
    
    @Input
    var migrationDir: String = "db/migration"
    
    @Input
    var targetUrl: String = "jdbc:mysql://localhost:3306/db"
    
    @OutputFile
    var outputFile: File = File("${project.buildDir}/migration-result.txt")
    
    @TaskAction
    fun migrate() {
        println("Running migrations from $migrationDir")
        println("Target: $targetUrl")
        outputFile.writeText("Migration completed at ${System.currentTimeMillis()}")
    }
}

tasks.register<DatabaseMigrationTask>("dbMigrate") {
    migrationDir = "db/migrations"
    targetUrl = "jdbc:mysql://prod-db:3306/prod"
}

// ========== 任务依赖关系 ==========
// clean → (依赖) → cleanXxx
// build → (依赖) → compile → processResources
// build → (依赖) → test
// build → (依赖) → check

// ========== 常用任务 ==========
// gradle tasks                    # 列出所有任务
// gradle build                    # 构建
// gradle clean build              # 清理并构建
// gradle bootRun                  # Spring Boot 启动
// gradle test --tests *UserTest   # 特定测试
// gradle dependencies             # 查看依赖树
// gradle bootJar                  # 打包可执行 jar
// gradle --build-cache build      # 使用构建缓存
// gradle build --parallel         # 并行构建
```

---

## 9. Gradle 依赖管理

### 9.1 依赖配置

```kotlin
// ========== Gradle 依赖配置 (作用域) ==========

dependencies {
    // ─── compile (已废弃) ───
    // Gradle 3.x 前: compile 'group:artifact:version'
    // Gradle 4.x+ 划分为 implementation 和 api
    
    // 1. implementation (推荐)
    //    ● 模块内部使用
    //    ● 依赖不会暴露给消费者 (编译更快, 封装更好)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // 2. api (慎用 — 暴露给消费者)
    //    ● 消费者也会获得此依赖
    //    ● 类似于 Maven 的 compile scope
    //    ● 通常只在 library 项目中使用
    api("org.springframework.boot:spring-boot-starter-web")
    
    // 3. compileOnly
    //    ● 编译时需要, 运行时不需要
    //    ● 类似于 Maven 的 provided
    compileOnly("org.projectlombok:lombok")
    compileOnly("org.mapstruct:mapstruct")
    
    // 4. runtimeOnly
    //    ● 运行时需要, 编译时不需要
    //    ● 类似于 Maven 的 runtime
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")
    
    // 5. annotationProcessor
    //    ● 注解处理器 (编译时生成代码)
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor")
    
    // 6. testImplementation
    //    ● 测试时使用
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    
    // 7. testCompileOnly
    testCompileOnly("org.projectlombok:lombok")
    
    // 8. testRuntimeOnly
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // 9. developmentOnly
    //    ● Spring Boot 的 DevTools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    
    // 10. constraints (版本约束)
    constraints {
        implementation("com.fasterxml.jackson.core:jackson-databind") {
            version {
                strictly("2.16.0")  // 强制使用此版本
                prefer("2.16.0")
            }
        }
    }
}
```

### 9.2 依赖声明方式

```kotlin
// ========== 多种声明方式 ==========

dependencies {
    // 方式 1: 字符串 (简洁, 不推荐 — 无法约束版本)
    implementation("com.google.guava:guava:33.0.0-jre")
    
    // 方式 2: Map (在 Kotlin DSL 中不常用)
    implementation(group = "com.google.guava", name = "guava", version = "33.0.0-jre")
    
    // 方式 3: 使用变量
    val guavaVersion = "33.0.0-jre"
    implementation("com.google.guava:guava:$guavaVersion")
    
    // 方式 4: 使用 libs.version.toml (推荐 — 版本目录)
    // 详见 section 9.3
}

// ========== 版本目录 (Version Catalog) — 推荐 ==========
// 文件: gradle/libs.versions.toml

// [versions]
// spring-boot = "3.2.0"
// mybatis-plus = "3.5.5"
// guava = "33.0.0-jre"
// lombok = "1.18.30"
//
// [libraries]
// spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
// mybatis-plus-spring-boot3-starter = { module = "com.baomidou:mybatis-plus-spring-boot3-starter", version.ref = "mybatis-plus" }
// guava = { module = "com.google.guava:guava", version.ref = "guava" }
// lombok = { module = "org.projectlombok:lombok", version.ref = "lombok" }
//
// [bundles]
// spring-web = ["spring-boot-starter-web", "spring-boot-starter-validation"]
//
// [plugins]
// spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }

// 在 build.gradle.kts 中使用:
dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.mybatis.plus.spring.boot3.starter)
    implementation(libs.guava)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    
    // 使用 bundle
    implementation(libs.bundles.spring.web)
}
```

### 9.3 依赖排除与传递性

```kotlin
// ========== 排除传递性依赖 ==========

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web") {
        // 排除内嵌 Tomcat (改用 Undertow)
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
        // 排除 Jackson (改用 Gson)
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-json")
    }
    
    implementation("org.springframework.boot:spring-boot-starter-undertow")
    implementation("com.google.code.gson:gson")
    
    // 排除特定传递性依赖
    implementation("com.example:some-library:1.0") {
        exclude(group = "commons-logging")
        // exclude(group = "commons-logging", module = "commons-logging")
    }
}

// ========== 全局排除 ==========
configurations.all {
    // 排除所有模块中的 commons-logging
    exclude(group = "commons-logging", module = "commons-logging")
    
    // 避免重复的 SLF4J 绑定
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    
    // 策略: 强制使用最新版本
    resolutionStrategy {
        // 强制版本
        force("com.fasterxml.jackson.core:jackson-databind:2.16.0")
        
        // 版本冲突时的策略
        failOnVersionConflict()  // 版本冲突时构建失败
        
        // 或: 选择最新版本
        // preferProjectModules()
    }
}

// ========== 查看依赖树 ==========
// gradle dependencies
// gradle dependencies --configuration implementation
// gradle dependencies --configuration runtimeClasspath
// gradle build --scan        # 生成构建扫描报告
```

---

## 10. Gradle 多项目与性能

### 10.1 多项目配置

```kotlin
// ========== 根项目 build.gradle.kts ==========

plugins {
    java
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

// 公共配置 (所有子项目共享)
subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    
    group = "com.example"
    version = "1.0.0-SNAPSHOT"
    
    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
    
    the<io.spring.gradle.dependencymanagement.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.0")
        }
    }
    
    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// ========== platform-common/build.gradle.kts ==========
// 公共模块 — 不应用 Spring Boot 插件

plugins {
    java
}

dependencies {
    implementation("cn.hutool:hutool-all:5.8.25")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}

// ========== platform-dao/build.gradle.kts ==========
plugins {
    java
}

dependencies {
    implementation(project(":platform-common"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")
}

// ========== platform-web/build.gradle.kts ==========
// Web 模块 — Spring Boot 可执行入口

plugins {
    java
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":platform-service"))
    implementation(project(":platform-common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}
```

### 10.2 性能优化

```kotlin
// ========== gradle.properties — 性能配置 ==========
# 内存配置
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8

# 守护进程 (默认开启, 大幅提升速度)
org.gradle.daemon=true

# 并行构建
org.gradle.parallel=true

# 构建缓存
org.gradle.caching=true

# 配置缓存 (Gradle 5.1+)
org.gradle.configuration-cache=true

# 按需配置 (只配置需要的项目)
org.gradle.configureondemand=true

# 控制台输出类型
org.gradle.console=plain

# Kotlin DSL 脚本缓存
kotlin.daemon.jvmargs=-Xmx512m

# ========== 构建缓存配置 ==========
// settings.gradle.kts
buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, ".gradle-build-cache")
        removeUnusedEntriesAfterDays = 7
    }
    
    // 远程缓存 (使用 Redis 或 S3)
    // remote<HttpBuildCache> {
    //     url = uri("https://build-cache.example.com/cache/")
    //     isPush = true
    //     credentials {
    //         username = "cache-user"
    //         password = "cache-pass"
    //     }
    // }
}

// ========== 增量编译 ==========
// 默认开启, 可以通过以下方式验证:
// gradle clean compileJava --info
// 输出会显示: Incremental compilation is enabled

// ========== 文件监控 (持续构建) ==========
// gradle build --continuous
// 文件变化时自动重新构建
```

### 10.3 Gradle Wrapper

```properties
# ========== gradle/wrapper/gradle-wrapper.properties ==========
# 项目自带的 Gradle 版本, 无需全局安装

distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists

# 使用方式:
./gradlew build        # 自动下载对应版本 Gradle
gradlew.bat build      # Windows

# 升级 Gradle Wrapper:
./gradlew wrapper --gradle-version=8.6
```

---

## 11. Maven vs Gradle

### 11.1 对比表格

```
┌─────────────────────────────────────────────────────────────────────┐
│              Maven vs Gradle 详细对比                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  对比维度       | Maven                     | Gradle                │
│  ───────────────|───────────────────────────|────────────────────────│
│  配置文件       | XML (pom.xml)             | Groovy/Kotlin DSL     │
│  性能           | 慢 (无缓存)               | 快 (增量+缓存+并行)   │
│  增量编译       | ❌ 不支持 (全量)          | ✅ 增量编译           │
│  构建缓存       | ❌ 不支持                 | ✅ 本地+远程缓存      │
│  并行构建       | ✅ -T 参数                | ✅ 默认               │
│  学习曲线       | 低 (XML 简单但冗长)       | 高 (DSL 灵活但复杂)   │
│  灵活性         | 低 (约定严格)             | 高 (任务图自定义)     │
│  代码可读性     | 中 (XML 结构清晰但冗长)   | 好 (DSL 简洁)         │
│  多模块         | ✅ 父 POM + 聚合          | ✅ 多 project         │
│  依赖管理       | scope 6 种                | configuration 10+ 种  |
│  版本管理       | BOM + properties          | Version Catalog       │
│  插件生态       | 丰富                       | 丰富 + 自定义方便     │
│  市场占有率     | 45% (遗留项目多)           | 53% (新项目占优)      │
│  使用场景       | 企业传统项目               | 新项目, Android       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 11.2 选择指南

```
选择 Maven 还是 Gradle?

你的项目类型?
│
├── 企业遗留项目 (已有 Maven 配置)
│   └──→ Maven (保持一致性)
│
├── 新项目
│   ├── 团队熟悉 Groovy/Kotlin?
│   │   ├── 是 → Gradle (推荐)
│   │   └── 否 → Maven
│   ├── 构建性能是关键?
│   │   ├── 是 → Gradle (增量编译+缓存)
│   │   └── 否 → Maven (够用)
│   ├── 需要自定义构建逻辑?
│   │   ├── 是 → Gradle (自定义 Task)
│   │   └── 否 → Maven (插件即可)
│   └── Android 项目?
│       └──→ Gradle (Android 官方支持)
│
└── 综合建议
    ├── Spring Boot 项目: Maven 或 Gradle 均可
    ├── 大型多模块项目: Gradle (构建速度优势明显)
    └── 新手学习: Maven (更低门槛, 更多现成文档)
```

---

## 12. 企业级构建流水线

### 12.1 GitLab CI / Jenkins Pipelines

```yaml
# ========== GitLab CI 流水线 (.gitlab-ci.yml) ==========
stages:
  - build
  - test
  - package
  - docker
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  DOCKER_IMAGE: $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA

cache:
  paths:
    - .m2/repository/
    - .gradle/

# Stage 1: Build
build:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn clean compile -DskipTests
  artifacts:
    paths:
      - target/classes/

# Stage 2: Test
unit-test:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn test
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml

integration-test:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  services:
    - mysql:8.0
  variables:
    SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/testdb"
  script:
    - mvn verify -P integration
  only:
    - main
    - develop

# Stage 3: Package
package:
  stage: package
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn clean package -DskipTests -P prod
  artifacts:
    paths:
      - target/*.jar
  only:
    - main
    - tags

# Stage 4: Build & Push Docker Image
docker-build:
  stage: docker
  image: docker:20.10.16
  services:
    - docker:20.10.16-dind
  script:
    - docker build -t $DOCKER_IMAGE .
    - docker push $DOCKER_IMAGE
  only:
    - main
    - tags

# Stage 5: Deploy
deploy-prod:
  stage: deploy
  image: alpine:latest
  before_script:
    - apk add --no-cache curl
  script:
    - curl -X POST https://deploy.example.com/api/deploy \
        -H "Authorization: Bearer $DEPLOY_TOKEN" \
        -d "image=$DOCKER_IMAGE&env=prod"
  environment:
    name: production
  only:
    - tags
  when: manual

# ========== Jenkins Pipeline (Jenkinsfile) ==========
pipeline {
    agent any
    
    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Code Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Package') {
            steps {
                sh 'mvn clean package -DskipTests -P prod'
            }
        }
        
        stage('Docker Build') {
            steps {
                script {
                    docker.build("myapp:${env.BUILD_NUMBER}")
                }
            }
        }
        
        stage('Deploy') {
            steps {
                script {
                    sh "kubectl set image deployment/myapp app=myapp:${env.BUILD_NUMBER}"
                }
            }
        }
    }
    
    post {
        failure {
            emailext(
                subject: "Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "The build has failed. Check ${env.BUILD_URL}"
            )
        }
    }
}
```

### 12.2 Docker 多阶段构建

```dockerfile
# ========== Dockerfile (Maven + Spring Boot) ==========
# 阶段 1: 构建
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# 构建 (跳过测试)
RUN mvn clean package -DskipTests

# 阶段 2: 运行 (最小化镜像)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 从构建阶段复制 jar
COPY --from=builder /app/target/*.jar app.jar

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080

# 启动参数
ENV JAVA_OPTS="-Xmx512m -Xms512m"
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

```dockerfile
# ========== Dockerfile (Gradle + Spring Boot) ==========
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN gradle build --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 13. 语义化版本与发布

### 13.1 语义化版本规范 (SemVer)

```
# ========== 语义化版本 2.0.0 ==========
# 格式: MAJOR.MINOR.PATCH
# 示例: 1.4.2

# MAJOR (主版本): 不兼容的 API 修改
#   1.0.0 → 2.0.0 (有破坏性变更)

# MINOR (次版本): 向下兼容的功能增加
#   1.0.0 → 1.1.0 (增加新功能)

# PATCH (修订版本): 向下兼容的问题修复
#   1.0.0 → 1.0.1 (修复 Bug)

# 先行版本号: 主版本.次版本.修订版本-先行版本.构建元数据
#   1.0.0-alpha.1
#   1.0.0-beta.2
#   1.0.0-rc.1
#   1.0.0

# Maven 版本号约定:
#   ${major}.${minor}.${patch}-${qualifier}
#   SNAPSHOT: 开发快照 (1.0.0-SNAPSHOT)
#   ALPHA/BETA: 内部测试 (1.0.0-ALPHA)
#   RC: 候选发布 (1.0.0-RC1)
#   RELEASE: 正式发布 (1.0.0-RELEASE 或 1.0.0)

# Maven 版本比较:
#   1.0.0-ALPHA < 1.0.0-BETA < 1.0.0-RC1 < 1.0.0-RC2 < 1.0.0 < 1.0.1
```

### 13.2 Maven 发布流程

```xml
<!-- ========== Maven Release Plugin ========== -->
<project>
    <scm>
        <connection>scm:git:git@github.com:example/my-app.git</connection>
        <developerConnection>scm:git:git@github.com:example/my-app.git</developerConnection>
        <url>https://github.com/example/my-app</url>
        <tag>HEAD</tag>
    </scm>
    
    <distributionManagement>
        <repository>
            <id>nexus-releases</id>
            <url>https://nexus.example.com/repository/maven-releases</url>
        </repository>
        <snapshotRepository>
            <id>nexus-snapshots</id>
            <url>https://nexus.example.com/repository/maven-snapshots</url>
        </snapshotRepository>
    </distributionManagement>
</project>

<!-- 发布命令: -->
# 开发期: mvn deploy           # 部署 SNAPSHOT 版本
# 发布前: mvn release:prepare   # 自动: 去除 SNAPSHOT → tag → 版本递增
# 发布:   mvn release:perform   # checkout tag → deploy

# 手动发布 (不使用 release plugin):
# mvn versions:set -DnewVersion=1.0.0          # 修改版本号
# mvn clean deploy -P prod                      # 发布
# mvn versions:set -DnewVersion=1.1.0-SNAPSHOT  # 回到开发版本
```

---

## 14. 依赖安全与漏洞扫描

### 14.1 OWASP Dependency Check

```xml
<!-- ========== Maven OWASP 插件 ========== -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.9</version>
    <configuration>
        <!-- CVSS 评分阈值 (>= 7.0 才报告) -->
        <failBuildOnCVSS>7</failBuildOnCVSS>
        
        <!-- 漏洞数据库更新 -->
        <nvdApiKey>${nvd.api.key}</nvdApiKey>
        
        <!-- 报告格式 -->
        <formats>
            <format>HTML</format>
            <format>JSON</format>
        </formats>
        
        <!-- 排除误报 -->
        <suppressionFile>dependency-check-suppressions.xml</suppressionFile>
        
        <!-- 跳过某些依赖 -->
        <skipTestScope>false</skipTestScope>
        <skipProvidedScope>false</skipProvidedScope>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>

# 使用:
# mvn dependency-check:check          # 检查依赖漏洞
# mvn dependency-check:aggregate      # 聚合多模块报告
# target/dependency-check-report.html # 查看报告

# 建议: 在 CI 流水线中集成
# 每次构建自动检查已知漏洞
```

```kotlin
// ========== Gradle OWASP 插件 ==========
plugins {
    id("org.owasp.dependencycheck") version "9.0.9"
}

dependencyCheck {
    failBuildOnCVSS = 7.0f
    formats = listOf("HTML", "JSON")
    suppressionFile = "dependency-check-suppressions.xml"
}

// 使用:
// gradle dependencyCheckAnalyze
// gradle dependencyCheckAggregate
```

### 14.2 Snyk / Trivy / Grype

```yaml
# ========== Snyk CLI (商业/免费) ==========
# snyk test              # 检查依赖漏洞
# snyk monitor           # 持续监控
# snyk code test         # 代码安全扫描

# ========== Trivy (开源, 推荐) ==========
# trivy fs .             # 扫描文件系统
# trivy image myapp:1.0  # 扫描 Docker 镜像
# trivy repo https://github.com/example/my-app

# ========== GitHub Dependabot (GitHub 内置) ==========
# .github/dependabot.yml:
# version: 2
# updates:
#   - package-ecosystem: "maven"
#     directory: "/"
#     schedule:
#       interval: "weekly"
#     open-pull-requests-limit: 10
#     labels:
#       - "dependencies"
#       - "security"
#     reviewers:
#       - "team-lead"
```

---

## 15. 面试题精选

### 15.1 Maven 面试题

**Q1: Maven 的依赖作用域 (scope) 有哪些？**

A: compile (默认, 所有阶段), provided (编译时需要, 运行时不打包), runtime (运行时需要, 编译时不需要), test (仅测试), system (本地 jar, 不推荐), import (仅 BOM 中使用)。

**Q2: Maven 如何处理依赖冲突？**

A: 最短路径优先 (nearest wins), 路径相同则第一声明优先。解决方式: 在 dependencyManagement 中指定版本, 使用 exclusion, 或 enforcer 插件强制版本收敛。

**Q3: Maven 的三种生命周期？**

A: clean (清理), default (默认, 含 compile, test, package, install, deploy), site (生成文档)。Phase 绑定 Plugin Goal 执行具体任务。

**Q4: 如何创建多模块 Maven 项目？**

A: 父 POM packaging=pom, 用 modules 声明子模块。子模块通过 parent 指向父 POM。父 POM 用 dependencyManagement 管理版本, 子模块无需声明版本。

### 15.2 Gradle 面试题

**Q5: Gradle 中 implementation 和 api 的区别？**

A: implementation 不暴露依赖给消费者, 编译速度更快, 封装性更好。api 暴露依赖给消费者, 类似 Maven 的 compile scope。推荐优先使用 implementation。

**Q6: Gradle 的性能优化措施？**

A: 开启守护进程 (daemon)、并行构建 (parallel)、构建缓存 (caching)、配置缓存 (configuration-cache)、增量编译、按需配置 (configureondemand)。

**Q7: Gradle 的 Task 是什么？如何创建自定义 Task？**

A: Task 是 Gradle 构建的最小执行单元。通过 tasks.register 创建, 可以用 dependsOn 建立依赖关系, 用 @TaskAction 定义执行逻辑。

### 15.3 综合面试题

**Q8: Maven vs Gradle 如何选择？**

A: 项目角度: 遗留 Maven 项目保持 Maven, 新项目推荐 Gradle。团队角度: 熟悉 Groovy/Kotlin 选 Gradle, 否则 Maven。性能角度: 大型项目 Gradle 优势明显 (增量+缓存+并行)。生态角度: 两者都支持 Spring Boot, Gradle 对 Android 是唯一选择。

**Q9: 如何发布 Java 库到 Maven 中央仓库？**

A: 注册 Sonatype JIRA 账号 → 创建 Issue 申请 namespace → 配置 GPG 签名 → 配置 Maven 的 Javadoc 和 Source 插件 → mvn deploy → 在 Sonatype 中 Release。

**Q10: 如何保证项目依赖的安全性？**

A: 使用 OWASP Dependency-Check 自动扫描已知漏洞, 定期更新依赖版本, 使用 Snyk/Dependabot 持续监控, 制定依赖版本升级策略, 定期审计。

---

> **构建工具是开发者的隐形助手。选择 Maven 获得稳定性, 选择 Gradle 获得速度。无论选择哪个, 理解依赖管理、生命周期和流水线集成, 是 Java 后端工程师进阶的必备技能。**
