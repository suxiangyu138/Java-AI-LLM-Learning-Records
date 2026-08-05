# 尚硅谷 Maven 面试宝典
> 基于课程大纲全面覆盖面试高频考点，从基础概念到深度原理、实战场景、系统设计一网打尽

## 目录
1. [一、基础概念速答](#一基础概念速答12-18题)
2. [二、深度原理剖析](#二深度原理剖析8-12题)
3. [三、实战场景题](#三实战场景题6-10题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（12-18题）

### 1.1 什么是 Maven？
Maven 是一个基于 **Project Object Model (POM)** 的项目管理和构建工具，核心功能包括**依赖管理**、**项目构建**和**项目信息管理**。它使用 `pom.xml` 文件描述项目配置，通过坐标系统唯一标识每个构件。

### 1.2 Maven 作为依赖管理工具的核心优势是什么？
- **自动下载**：从中央仓库自动下载依赖及其传递依赖
- **版本统一**：通过 `properties` 标签和 `dependencyManagement` 统一管理版本
- **依赖冲突解决**：通过最短路径优先和第一声明优先原则自动解决冲突
- **仓库链**：本地仓库 -> 私服 -> 中央仓库的依次查找机制

### 1.3 Maven 作为构建工具解决了什么问题？
传统项目依赖手动配置 `classpath`，效率低且容易遗漏。Maven 提供标准化的**构建生命周期**（compile -> test -> package -> install -> deploy），通过插件自动执行编译、测试、打包、部署等环节，无需人工介入。

### 1.4 Maven 的工作原理是什么？
Maven 根据 `pom.xml` 中的坐标信息（GAVP），在**本地仓库**查找依赖，若未找到则依次查询**私服/远程仓库**和**中央仓库**，下载后缓存到本地。构建时，通过生命周期+插件机制执行各阶段任务。

### 1.5 什么是 GAVP 坐标？
GAVP 是 Maven 坐标的核心四要素：

| 要素 | 说明 | 示例 |
|------|------|------|
| `groupId` | 组织/公司标识，通常为倒置域名 | `com.example` |
| `artifactId` | 项目唯一标识 | `user-service` |
| `version` | 项目版本号 | `1.0.0-SNAPSHOT` |
| `packaging` | 打包方式（jar/war/pom） | `jar` |

```xml
<groupId>com.example</groupId>
<artifactId>user-service</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>jar</packaging>
```

### 1.6 Maven 的六大依赖范围（Scope）对比

| Scope | 编译期 | 测试期 | 运行期 | 是否打入包中 | 典型场景 |
|-------|--------|--------|--------|-------------|---------|
| `compile` | Y | Y | Y | Y | 核心业务依赖 |
| `test` | N | Y | N | N | JUnit、Mockito |
| `provided` | Y | Y | N | N | Servlet API、Lombok |
| `runtime` | N | Y | Y | Y | JDBC 驱动 |
| `system` | Y | Y | N | N | 本地系统 jar（需配合 systemPath） |
| `import` | - | - | - | - | 仅用于 `dependencyManagement` 的 BOM 导入 |

**详细说明**：
- `compile`：默认 scope，所有阶段都可用，会传递依赖
- `test`：仅测试代码可见，如 `junit`、`mockito`
- `provided`：编译期需要但运行期由容器提供，如 `javax.servlet-api`（Tomcat 已包含）
- `runtime`：编译不需要但运行时需要，如 `mysql-connector-java`
- `system`：类似 provided，但显式指定本地 jar 路径，**不推荐使用**，破坏可移植性
- `import`：仅在 `dependencyManagement` 中使用，将 BOM 中的依赖版本引入当前项目

```xml
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>
```

### 1.7 settings.xml 中有哪些核心配置？
- **localRepository**：本地仓库路径
- **mirrors**：镜像配置，可拦截中央仓库请求到国内镜像（如阿里云）
- **servers**：认证信息（用户名/密码），访问私服时需要
- **profiles**：激活特定配置组，如 JDK 版本、仓库地址
- **activeProfiles**：默认激活的 profile

```xml
<mirror>
    <id>aliyun-maven</id>
    <mirrorOf>central</mirrorOf>
    <name>阿里云公共仓库</name>
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

### 1.8 Maven 聚合与继承的区别是什么？

| 特性 | 聚合 | 继承 |
|------|------|------|
| 目的 | 一次构建多个模块 | 统一管理依赖和配置 |
| 标签 | `<modules><module>` | `<parent>` |
| packaging | `pom` | `pom` |
| 方向 | 父模块引用子模块 | 子模块引用父模块 |
| 关系 | 整体与部分 | 父子继承 |

### 1.9 什么是 Maven 私服 Nexus？有哪些仓库类型？
Nexus 是 Maven 的私有仓库服务器，支持三种仓库类型：

| 仓库类型 | 说明 | 用途 |
|----------|------|------|
| **Hosted** | 宿主仓库 | 存放公司内部私有的 jar 包 |
| **Proxy** | 代理仓库 | 代理中央仓库或阿里云仓库，缓存下载的 jar |
| **Group** | 仓库组 | 将多个仓库合并为一个地址对外暴露 |

---

## 二、深度原理剖析（8-12题）

### 2.1 依赖传递（Transitive Dependency）的原理是什么？

当项目 A 依赖 B，B 依赖 C 时，A 会自动获得 C 的依赖（称为**传递依赖**）。传递性使得开发者无需手动声明所有间接依赖。

```text
A -> B -> C (A 会自动获得 C 的依赖)
```

但是传递依赖受 scope 影响，**scope 传递规则**如下：

| 第一依赖\第二依赖 | compile | test | provided | runtime |
|------------------|---------|------|----------|---------|
| compile | compile | - | - | runtime |
| test | test | - | - | test |
| provided | provided | - | provided | provided |
| runtime | runtime | - | - | runtime |

> 关键规律：`test` 和 `provided` 范围的依赖**不会传递**。

### 2.2 依赖冲突的解决机制是什么？

Maven 采用以下两原则自动解决依赖冲突：

**原则一：最短路径优先（Shortest Path First）**

```text
A -> B -> C -> logback 1.2.0      (路径深度=3)
A -> D -> logback 1.4.0            (路径深度=2，胜出)
```

最终 A 使用 logback 1.4.0，因为路径更短。

**原则二：第一声明优先（First Declaration Wins）**

当路径深度相同时，在 `pom.xml` 中先声明的依赖胜出：

```text
A -> B -> logback 1.2.0 (深度=2，先声明)
A -> C -> logback 1.4.0 (深度=2，后声明，被排除)
```

### 2.3 如何手动排除传递依赖？

使用 `<exclusions>` 标签手动排除不需要的传递依赖：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>module-a</artifactId>
    <version>1.0.0</version>
    <exclusions>
        <exclusion>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

也可使用 **optional** 标记，让依赖不传递：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>internal-utils</artifactId>
    <version>1.0.0</version>
    <optional>true</optional>
</dependency>
```

### 2.4 dependencyManagement 与 dependencies 的区别是什么？

| 特性 | dependencyManagement | dependencies |
|------|---------------------|--------------|
| 作用 | 声明版本号，统一管理 | 实际引入依赖 |
| 子模块是否自动继承 | 声明但不引入，子模块继承版本号 | 直接引入依赖 |
| 使用场景 | 父 POM 或 BOM 中 | 各模块的 pom.xml |
| 覆盖规则 | 子模块可覆盖版本号 | 不可被子模块覆盖 |

```xml
<!-- 父 POM：统一管理版本，但不引入 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <version>5.3.30</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 子模块：只需声明 groupId 和 artifactId，version 继承自父 POM -->
<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-core</artifactId>
        <!-- 无需指定 version -->
    </dependency>
</dependencies>
```

### 2.5 BOM（Bill of Materials）模式是什么？

BOM 是一种特殊的 POM，其 `packaging=pom`，专门用于管理一组依赖的版本号。BOM 通过 `dependencyManagement` 统一声明版本，其他项目通过 `scope=import` 导入使用。

```xml
<!-- BOM 定义 -->
<project>
    <groupId>com.example</groupId>
    <artifactId>spring-boot-dependencies</artifactId>
    <version>3.2.0</version>
    <packaging>pom</packaging>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
                <version>3.2.0</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

```xml
<!-- 项目导入 BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Spring Boot 就是典型的 BOM 模式**：`spring-boot-dependencies` 管理了数百个依赖版本，用户项目导入后只需声明 `artifactId` 即可。

### 2.6 Maven 构建生命周期详解

Maven 有三套独立的生命周期：

| 生命周期 | 包含阶段 | 作用 |
|----------|---------|------|
| **clean** | pre-clean, clean, post-clean | 清理构建产物 |
| **default** | 23 个阶段（见下方） | 核心构建流程 |
| **site** | pre-site, site, post-site, site-deploy | 生成项目文档 |

**Default 生命周期的核心阶段**：

| 阶段 | 执行内容 | 对应命令 |
|------|---------|---------|
| `validate` | 验证项目配置正确性 | - |
| `compile` | 编译源代码 | `mvn compile` |
| `test` | 运行单元测试 | `mvn test` |
| `package` | 打包为 jar/war | `mvn package` |
| `verify` | 运行集成测试验证包 | `mvn verify` |
| `install` | 安装到本地仓库 | `mvn install` |
| `deploy` | 部署到远程私服 | `mvn deploy` |

**执行规则**：执行某个阶段时，其之前的所有阶段也会依次执行。例如 `mvn install` 会自动执行 compile -> test -> package -> install。

### 2.7 Maven 插件机制的核心原理

Maven 本质是一个**插件执行框架**，生命周期的每个阶段都由对应的插件完成：

| 插件 | 功能 | 绑定阶段 |
|------|------|---------|
| `maven-compiler-plugin` | 编译 Java 源码 | compile |
| `maven-surefire-plugin` | 运行单元测试 | test |
| `maven-failsafe-plugin` | 运行集成测试 | verify |
| `maven-jar-plugin` | 打包为 jar | package |
| `maven-war-plugin` | 打包为 war | package |
| `maven-assembly-plugin` | 定制化打包（含依赖） | package |
| `maven-shade-plugin` | 创建 uber-jar（可执行 fat jar） | package |
| `maven-install-plugin` | 安装到本地仓库 | install |
| `maven-deploy-plugin` | 部署到远程仓库 | deploy |

**自定义插件配置示例**：

```xml
<build>
    <plugins>
        <!-- 指定 JDK 编译版本 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>17</source>
                <target>17</target>
                <encoding>UTF-8</encoding>
            </configuration>
        </plugin>
        <!-- 打包时跳过测试 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <skipTests>true</skipTests>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 2.8 依赖下载失败的常见原因及解决方案

| 原因 | 解决方案 |
|------|---------|
| 网络问题无法访问中央仓库 | 配置阿里云镜像 |
| 仓库中不存在该坐标的版本 | 检查版本号是否正确 |
| 依赖被删除或私有仓库未发布 | 确认仓库中已部署 |
| 本地 `.lastUpdated` 缓存文件 | 删除缓存后重新下载 |

**批量删除 `.lastUpdated` 缓存脚本**（Windows）：

```bash
# 在本地仓库目录下执行
cd /d %USERPROFILE%\.m2\repository
for /r %i in (*.lastUpdated) do del %i
```

### 2.9 Profiles 多环境配置

Profiles 允许根据不同环境（开发/测试/生产）使用不同的配置：

```xml
<profiles>
    <!-- 开发环境 -->
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <env>dev</env>
            <db.url>jdbc:mysql://localhost:3306/dev</db.url>
        </properties>
    </profile>
    <!-- 生产环境 -->
    <profile>
        <id>prod</id>
        <properties>
            <env>prod</env>
            <db.url>jdbc:mysql://prod-host:3306/prod</db.url>
        </properties>
    </profile>
</profiles>
```

激活方式：
- `mvn clean install -P prod`（命令行激活）
- `<activeByDefault>true</activeByDefault>`（默认激活）
- 通过 JDK 版本、系统属性等自动激活

---

## 三、实战场景题（6-10题）

### 3.1 使用 `mvn dependency:tree` 分析依赖冲突

```bash
# 查看完整依赖树
mvn dependency:tree

# 搜索特定依赖
mvn dependency:tree -Dincludes=org.springframework:spring-core

# 查看依赖冲突详情
mvn dependency:tree -Dverbose
```

**输出示例**：
```text
com.example:my-app:jar:1.0.0
+- org.springframework:spring-core:5.3.30 (compile)
   \- commons-logging:commons-logging:1.2 (compile)
\- org.slf4j:slf4j-api:2.0.9 (compile)
```

### 3.2 如何排查 Maven 构建速度慢的问题？

| 排查方向 | 检查项 | 优化方案 |
|---------|--------|---------|
| 仓库网络 | 是否使用了国内镜像 | 配置阿里云 `https://maven.aliyun.com/repository/public` |
| 依赖下载 | 是否每次都重新下载 | 开启 `-o` 离线模式，或确保本地缓存 |
| 测试执行 | 测试是否过多或过慢 | `-DskipTests` 跳过测试（非生产构建） |
| 多模块构建 | 是否重复构建不变模块 | 使用 `-pl` 指定模块，`-am` 同时构建依赖模块 |
| 并行构建 | 是否启用多线程 | `mvn -T 4 clean install`（4 线程并行） |

**常用优化命令**：
```bash
# 跳过测试 + 多线程 + 指定模块
mvn clean install -DskipTests -T 4 -pl user-service,order-service -am
```

### 3.3 如何搭建多模块项目？

```xml
<!-- 父 POM：聚合 + 继承 -->
<project>
    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>common</module>
        <module>user-service</module>
        <module>order-service</module>
        <module>gateway</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>3.2.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

```xml
<!-- 子模块 user-service/pom.xml -->
<project>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-project</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>user-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 3.4 如何将项目部署到 Nexus 私服？

**第一步**：在 `settings.xml` 中配置私服认证信息：

```xml
<servers>
    <server>
        <id>nexus-releases</id>
        <username>admin</username>
        <password>admin123</password>
    </server>
    <server>
        <id>nexus-snapshots</id>
        <username>admin</username>
        <password>admin123</password>
    </server>
</servers>
```

**第二步**：在 `pom.xml` 中配置部署地址：

```xml
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <url>http://nexus.example.com/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <url>http://nexus.example.com/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

**第三步**：执行部署命令：

```bash
mvn deploy
```

> 版本号含 `-SNAPSHOT` 的自动发布到 `snapshots` 仓库，正式版本发布到 `releases` 仓库。

### 3.5 Maven 项目依赖版本统一维护的几种方式

| 方式 | 适用场景 | 示例 |
|------|---------|------|
| `<properties>` 标签 | 单模块或简单项目 | `<spring.version>5.3.30</spring.version>` |
| `dependencyManagement` | 多模块父 POM | 父 POM 统一管理，子模块继承 |
| BOM `import` | 外部依赖版本管理 | Spring Boot、JUnit 5 BOM |
| `pluginManagement` | 统一管理插件版本 | 在父 POM 中统一定义插件配置 |

```xml
<properties>
    <java.version>17</java.version>
    <spring.boot.version>3.2.0</spring.boot.version>
    <mybatis.version>3.5.15</mybatis.version>
    <lombok.version>1.18.30</lombok.version>
</properties>
```

### 3.6 如何解决本地 jar 包依赖问题？

> 阿里/美团面试常问：项目无法从中央仓库获取某个 jar 怎么办？

```bash
# 方案一：手动安装到本地仓库
mvn install:install-file \
    -Dfile=my-local-lib.jar \
    -DgroupId=com.example \
    -DartifactId=my-local-lib \
    -Dversion=1.0.0 \
    -Dpackaging=jar

# 方案二：使用 system scope（不推荐）
```

```xml
<!-- 方案二：使用 system scope（不推荐，破坏可移植性） -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-local-lib</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/my-local-lib.jar</systemPath>
</dependency>
```

---

## 四、手写代码题（5-8题）

### 4.1 完整 POM 模版

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- GAVP 坐标 -->
    <groupId>com.example</groupId>
    <artifactId>demo-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>demo-service</name>
    <description>Demo microservice project</description>

    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring.boot.version>3.2.0</spring.boot.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 4.2 多模块父 POM（聚合 + 继承）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>parent-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <!-- 聚合子模块 -->
    <modules>
        <module>common-utils</module>
        <module>core-service</module>
        <module>web-api</module>
    </modules>

    <!-- 统一版本管理 -->
    <properties>
        <java.version>17</java.version>
        <spring.version>5.3.30</spring.version>
        <junit.version>5.10.1</junit.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>

    <!-- 统一依赖管理（不直接引入，子模块按需引用） -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-context</artifactId>
                <version>${spring.version}</version>
            </dependency>
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter</artifactId>
                <version>${junit.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
                <scope>provided</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 统一插件管理 -->
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.11.0</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 4.3 使用 Assembly 插件打包可执行 JAR

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <configuration>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
        <archive>
            <manifest>
                <mainClass>com.example.MainApplication</mainClass>
            </manifest>
        </archive>
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
```

### 4.4 使用 Shade 插件创建 Uber-JAR

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>com.example.MainApplication</mainClass>
                    </transformer>
                </transformers>
                <!-- 排除冲突文件 -->
                <filters>
                    <filter>
                        <artifact>*:*</artifact>
                        <excludes>
                            <exclude>META-INF/*.SF</exclude>
                            <exclude>META-INF/*.DSA</exclude>
                            <exclude>META-INF/*.RSA</exclude>
                        </excludes>
                    </filter>
                </filters>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 4.5 使用 Surefire 插件配置单元测试

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.3</version>
    <configuration>
        <!-- 跳过测试 -->
        <skipTests>false</skipTests>
        <!-- 包含/排除测试类 -->
        <includes>
            <include>**/*Test.java</include>
            <include>**/*Tests.java</include>
        </includes>
        <excludes>
            <exclude>**/*IntegrationTest.java</exclude>
        </excludes>
        <!-- 测试报告格式 -->
        <reportFormat>brief</reportFormat>
        <!-- 并行执行测试 -->
        <parallel>methods</parallel>
        <threadCount>4</threadCount>
    </configuration>
</plugin>
```

---

## 五、系统设计题（3-5题）

### 5.1 如果你来设计一个企业级 Maven 私服架构，你会如何设计？

**设计要点**：

1. **仓库分层**：搭建 Nexus 集群，分为 3 层
   - 代理层：代理阿里云/中央仓库，配置定时同步策略
   - 宿主层：存放公司内部 jar，按部门/项目拆分仓库
   - 聚合层：通过 Group 仓库对外暴露统一地址

2. **权限控制**：基于角色的访问控制
   - 管理员：管理仓库、用户、权限
   - 开发者：可 deploy 到 snapshots，不可覆盖 releases
   - 只读用户：仅可下载依赖

3. **高可用**：Nexus 集群 + 共享存储（NAS/S3），避免单点故障

4. **CI/CD 集成**：Jenkins/GitLab CI 构建后自动 `mvn deploy`

### 5.2 如何处理微服务架构中的依赖版本管理？

在微服务架构中，不同服务可能使用同一框架的不同版本，极易产生冲突。推荐方案：

```
父 POM（my-platform-dependencies）
    ├── service-a  (pom)
    │   ├── service-a-api
    │   └── service-a-impl
    ├── service-b  (pom)
    │   ├── service-b-api
    │   └── service-b-impl
    └── common (统一依赖管理)
```

**最佳实践**：
1. 顶层 BOM 管理所有三方依赖版本
2. 每个微服务继承父 BOM 的版本控制
3. 内部模块间通过 `<version>${project.version}</version>` 保持同步
4. 使用 `mvn versions:display-dependency-updates` 定期检查版本更新

### 5.3 Maven vs Gradle 对比

| 对比维度 | Maven | Gradle |
|----------|-------|--------|
| 构建文件 | `pom.xml`（XML） | `build.gradle`（Groovy/Kotlin DSL） |
| 构建速度 | 较慢，不支持增量编译 | 快 2-10 倍，基于 DAG 任务图 + 增量编译 |
| 依赖管理 | GAVP 坐标，成熟稳定 | 同样支持 Maven 坐标，更灵活的依赖配置 |
| 灵活性 | 固定生命周期模型，扩展受限 | 基于 Task 图，高度可定制 |
| 学习曲线 | 较低，XML 易读 | 较高，需要学习 DSL |
| 项目结构 | 约定优于配置，标准化 | 约定优于配置，支持自定义 |
| 缓存机制 | 本地仓库缓存 | Build Cache + 远程缓存 |
| 社区生态 | 极成熟，插件丰富 | 增长迅速，Android 标准 |
| 多模块 | 聚合 + 继承 | 多 project 配置，更灵活 |

> **面试建议**：大部分企业使用 Maven（尤其是传统 Java 项目），新项目和 Android 项目偏向 Gradle。两者不是替代关系，根据团队和项目特点选择。

### 5.4 Maven 项目从 Java 8 迁移到 Java 17 需要修改哪些配置？

```xml
<properties>
    <!-- 1. 更新 JDK 版本 -->
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>

<build>
    <plugins>
        <!-- 2. 更新 compiler 插件 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <release>17</release>
                <!-- 启用 Java 17 新特性 -->
                <parameters>true</parameters>
            </configuration>
        </plugin>
        <!-- 3. 更新 Surefire 插件（兼容 JPMS） -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.3</version>
        </plugin>
    </plugins>
</build>
```

---

## 六、常见坑点与最佳实践

| 坑点 | 原因 | 解决方案 |
|------|------|---------|
| `.lastUpdated` 缓存导致依赖永远下载失败 | 上次下载中断产生错误缓存 | 删除 `~/.m2/repository` 下所有 `.lastUpdated` 文件后重试 |
| `IntelliJ IDEA` 中的 Maven 依赖未自动刷新 | IDE 缓存未更新 | 点击右侧 Maven 面板的 `Reload All Projects` 按钮 |
| 子模块版本号不统一 | 手动在各子模块中写死版本 | 父 POM 使用 `dependencyManagement` 统一管理 |
| 运行时 `ClassNotFoundException` | scope 配置错误导致 jar 未打包 | 检查 scope 是否应为 `compile` 或 `runtime` 而不是 `provided` |
| 多模块构建时 `Could not resolve dependencies` | 子模块版本与父 POM 不一致 | 统一使用 `${project.version}` 引用内部模块版本 |
| 打包后 jar 运行报 `No main manifest attribute` | 未指定 mainClass | 配置 `maven-jar-plugin` 的 `mainClass` 或使用 `spring-boot-maven-plugin` |
| 依赖冲突导致运行时方法异常 | 不同版本的相同类被加载 | `mvn dependency:tree` 定位冲突，使用 `<exclusions>` 排除 |
| `-SNAPSHOT` 快照版本未更新 | 本地缓存了旧快照 | 使用 `-U` 参数强制更新：`mvn clean install -U` |
| 私服下载 401 Unauthorized | 缺少认证配置 | 在 `settings.xml` 的 `<servers>` 中配置私服用户名密码 |

---

## 七、面试回答模板（Top 5）

### 7.1 请解释 Maven 的依赖传递机制

"Maven 的依赖传递是指当项目 A 依赖 B、B 依赖 C 时，C 会自动传递到 A 的 classpath 中。但是传递范围受 scope 限制，例如 `test` 和 `provided` 范围的依赖不会传递。如果出现依赖冲突，Maven 通过**最短路径优先**和**第一声明优先**两个原则自动解决。如果需要手动控制，可以使用 `<exclusions>` 排除特定传递依赖，或使用 `<optional>true</optional>` 阻止依赖外传。建议开发中经常使用 `mvn dependency:tree` 命令来查看完整的依赖树并排查问题。"

### 7.2 Maven 生命周期包含哪些核心阶段？

"Maven 有三套生命周期：clean、default 和 site。最核心的是 default 生命周期，包含 validate、compile、test、package、verify、install、deploy 七个关键阶段。这些阶段有严格的顺序关系，执行后面的阶段会自动触发前面所有阶段。每个阶段实际由对应的 Maven 插件执行，例如 compile 由 `maven-compiler-plugin` 执行，test 由 `maven-surefire-plugin` 执行。开发者可以通过绑定自定义插件到特定阶段来扩展构建流程。"

### 7.3 dependencyManagement 和 dependencies 有什么区别？

"`dependencyManagement` 在父 POM 中统一声明依赖的版本号和 scope，但不会在父模块中实际引入这些依赖。子模块引入依赖时无需指定版本号，自动继承父 POM 中声明的版本。而 `dependencies` 是实际引入依赖，子模块会直接继承父模块的所有 `dependencies`。`dependencyManagement` 的核心优势是统一版本管理，便于在多模块项目中保持一致性，子模块也能通过显式指定版本号覆盖父 POM 的声明。Spring Boot 的 `spring-boot-dependencies` 就是最典型的 `dependencyManagement` + BOM 模式应用。"

### 7.4 多模块项目中如何处理模块间的依赖？

"多模块项目通常采用父 POM 同时承担**聚合**和**继承**两种角色。聚合通过 `<modules><module>` 将子模块组织在一起，实现一条命令构建所有模块。继承通过 `<parent>` 让子模块继承父 POM 的公共配置。模块间依赖直接通过 GAV 坐标引用，版本使用 `${project.version}` 保持同步。关键是在父 POM 中通过 `dependencyManagement` 统一管理所有依赖版本，子模块仅声明自己需要的依赖。构建时推荐使用 `mvn clean install -pl module-a -am` 只构建指定的模块及其依赖模块，提高构建效率。"

### 7.5 Maven 和 Gradle 的优缺点对比？

"Maven 的最大优势是**约定优于配置**，项目结构标准化、学习成本低、插件生态成熟，适用于大多数企业级项目。缺点是 XML 配置冗长，构建速度不如 Gradle。Gradle 基于 DAG 任务图，支持增量编译和构建缓存，构建速度比 Maven 快 2-10 倍，且 Kotlin DSL 配置更简洁。但 Gradle 学习曲线较陡，生态在某些领域不如 Maven 成熟。在实际工作中，传统 Java 项目和微服务架构仍以 Maven 为主，Android 开发和追求构建性能的新项目更多采用 Gradle。两者可以共存，选择时需要考虑团队技术栈和项目特点。"

---

## 八、快速查漏补缺Checklist

- [ ] 掌握 GAVP 坐标的四种元素含义
- [ ] 理解六大 scope（compile/test/provided/runtime/system/import）的区别
- [ ] 能在 `pom.xml` 中编写依赖声明
- [ ] 理解依赖冲突解决机制：最短路径优先 + 第一声明优先
- [ ] 理解 scope 传递规则表
- [ ] 掌握 `mvn dependency:tree` 的使用
- [ ] 熟悉 default 生命周期各核心阶段（compile -> test -> package -> install -> deploy）
- [ ] 掌握 `maven-compiler-plugin` 的配置（JDK 版本）
- [ ] 掌握 `maven-surefire-plugin` 的配置（跳过测试、包含排除模式）
- [ ] 理解 `dependencyManagement` 与 `dependencies` 的区别
- [ ] 掌握 BOM 模式原理及 `scope=import` 的使用
- [ ] 能编写多模块父 POM（聚合 + 继承）
- [ ] 理解 `<exclusions>` 和 `<optional>` 的区别和使用场景
- [ ] 掌握 `settings.xml` 中的 mirror、server、profile 配置
- [ ] 理解 Nexus 三种仓库类型：Hosted、Proxy、Group
- [ ] 掌握 `mvn deploy` 部署到私服的完整配置
- [ ] 理解 Profiles 多环境配置
- [ ] 理解 `maven-assembly-plugin` 和 `maven-shade-plugin` 的区别
- [ ] 能讨论 Maven vs Gradle 的优劣
- [ ] 掌握 `.lastUpdated` 缓存问题的排查和解决
- [ ] 掌握 `-U`、`-o`、`-T`、`-pl`、`-am` 等常用命令行参数
- [ ] 能解释传递依赖的 scope 传递规则
- [ ] 了解 Java 8 -> Java 17 迁移所需的 Maven 配置变更
