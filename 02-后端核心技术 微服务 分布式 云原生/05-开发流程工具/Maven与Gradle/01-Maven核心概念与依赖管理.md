# 01 - Maven 核心概念与依赖管理

> 🎯 Maven 的三大核心能力：GAV 坐标体系实现唯一标识、依赖传递机制解决 Jar 包地狱、标准生命周期统一构建流程 — 这是 Java 后端工程化的基石

---

## 目录

1. [Maven 核心概念](#一maven核心概念)
2. [生命周期与插件](#二生命周期与插件)
3. [Maven + Git CI/CD 集成](#三maven--git-cicd-集成)

---

## 一、Maven核心概念

> 💡 Maven 与 Git 是 Java 项目两大基石。Maven 负责构建管理，Git 负责版本控制。Git 详细内容参见 [Git 学习文件夹](../Git/)。

### 1.1 约定优于配置

Maven最核心的设计哲学是**约定优于配置**。它规定了标准项目结构，开发者无需为每个项目单独配置目录布局。

```
my-app/
├── src/
│   ├── main/
│   │   ├── java/          # 项目Java源码
│   │   └── resources/     # 资源文件(配置文件、静态资源等)
│   └── test/
│       ├── java/          # 测试Java源码
│       └── resources/     # 测试资源文件
├── target/                # 构建产物输出目录(编译后的class、jar包等)
├── pom.xml                # Maven项目核心配置文件
└── .gitignore             # Git忽略文件
```

这种约定使团队内部所有项目保持一致的代码布局，降低新成员的上手成本。任何熟悉Maven的开发者，无论进入哪个项目，都能立刻定位到源码、测试和配置文件。

### 1.2 坐标系统

Maven通过**坐标**唯一标识每一个构建产物。坐标由三要素组成：

```xml
<groupId>com.example</groupId>
<artifactId>user-service</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

| 要素 | 说明 | 命名规范 |
|------|------|----------|
| `groupId` | 组织标识，通常为反向域名 | `com.company.project` |
| `artifactId` | 项目/模块名称 | `user-service`、`order-api` |
| `version` | 版本号 | `1.0.0-RELEASE`、`2.1.0-SNAPSHOT` |

**版本号规则**：
- **SNAPSHOT（快照版本）**：表示开发中的不稳定版本，每次构建都会从远程仓库拉取最新快照，适用于团队内部联调阶段。
- **RELEASE（正式版本）**：表示稳定的发布版本，构建后不会变更，适用于生产环境。
- 版本号推荐遵循**语义化版本**：`主版本.次版本.修订号`（如 `2.5.1`），主版本号变更表示不兼容的API修改，次版本号变更表示向下兼容的功能新增，修订号变更表示向下兼容的问题修正。

### 1.3 依赖管理

依赖管理是Maven最核心、最强大的功能之一，它解决了Java项目中"jar包地狱"的问题。

#### 依赖范围（Scope）

依赖范围决定了依赖在不同的构建阶段是否可见：

| Scope | 编译期 | 运行期 | 测试期 | 典型示例 |
|-------|--------|--------|--------|----------|
| `compile`（默认） | 是 | 是 | 是 | 项目核心依赖，如Spring框架 |
| `provided` | 是 | 否 | 是 | Servlet API（容器已提供）、Lombok |
| `runtime` | 否 | 是 | 是 | JDBC驱动实现类 |
| `test` | 否 | 否 | 是 | JUnit、Mockito |
| `system` | 是 | 是 | 是 | **已废弃**，不推荐使用 |

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <scope>compile</scope>  <!-- 默认即为compile，可省略 -->
</dependency>

<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>  <!-- 容器提供，打包时排除 -->
</dependency>

<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
    <scope>runtime</scope>  <!-- 编译期不需要，运行时才加载 -->
</dependency>

<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>  <!-- 仅测试期间需要 -->
</dependency>
```

#### 依赖传递与调解

Maven支持**依赖传递**：如果A依赖B、B依赖C，则A自动获得C的依赖。传递行为受scope影响：

- `compile`范围的依赖会传递
- `provided`和`test`范围的依赖不会传递
- `runtime`范围的依赖会传递（仅在运行期）

当出现**依赖冲突**（同一个依赖的不同版本同时存在）时，Maven使用以下规则进行**依赖调解**：

1. **最短路径优先**：路径越短，优先级越高。A → B → C:v2（路径长2），A → D → E → C:v1（路径长3），则选择C:v2。
2. **先声明优先**：相同路径长度下，在pom.xml中先声明的依赖胜出。

#### dependencyManagement

在父POM或项目入口POM中使用`<dependencyManagement>`统一管理版本号，子模块引用时无需指定版本，有效避免版本冲突：

```xml
<!-- 父POM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>32.1.3-jre</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 子模块POM，无需指定版本 -->
<dependencies>
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <!-- 版本从父POM继承 -->
    </dependency>
</dependencies>
```

#### 依赖排除与可选依赖

**依赖排除**用于解决传递依赖带来的冲突：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>some-library</artifactId>
    <version>1.0</version>
    <exclusions>
        <exclusion>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**可选依赖**标记为optional后不会传递，需要消费方自行声明：

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
    <optional>true</optional>
</dependency>
```

#### 查看依赖树

使用`mvn dependency:tree`命令可视化依赖关系，快速发现冲突：

```bash
# 查看完整依赖树
mvn dependency:tree

# 查找特定依赖
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core:jackson-databind

# 输出到文件
mvn dependency:tree > deps.txt
```

### 1.4 BOM（Bill of Materials）

BOM是一种特殊的POM，其唯一目的是集中管理一组依赖的版本号。最经典的案例是Spring Boot的`spring-boot-dependencies`：

```xml
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
```

引入BOM后，项目中引用Spring Boot生态内的依赖都无需指定版本号，所有版本由BOM统一管控。团队也可以自行编写BOM来管理内部公共库的版本。

### 1.5 生命周期

Maven定义了三大独立的生命周期：

| 生命周期 | 用途 | 核心阶段 |
|----------|------|----------|
| **clean** | 清理项目 | pre-clean → clean → post-clean |
| **default** | 构建项目 | validate → compile → test → package → verify → install → deploy |
| **site** | 生成站点文档 | pre-site → site → post-site → site-deploy |

最常用的是**default生命周期**的核心阶段：

```bash
mvn clean          # 清理target目录
mvn compile        # 编译src/main/java
mvn test           # 运行测试代码（依赖compile已完成）
mvn package        # 打包为jar/war（依赖test已完成）
mvn install        # 安装到本地~/.m2/repository
mvn deploy         # 部署到远程私服
```

执行后面的阶段时，前面的阶段会自动执行。例如`mvn package`会依次执行compile、test、package。

### 1.6 核心插件

Maven本身是一个插件执行框架，实际工作由插件完成：

| 插件 | 功能 | 关键配置 |
|------|------|----------|
| `maven-compiler-plugin` | 编译Java源码 | 指定JDK版本、编码 |
| `maven-surefire-plugin` | 执行单元测试 | 包含/排除测试类 |
| `maven-jar-plugin` | 打包为jar | 指定Main-Class |
| `spring-boot-maven-plugin` | 构建可执行fat jar | 打包所有依赖 |
| `maven-assembly-plugin` | 自定义打包 | 制作分发包 |
| `maven-source-plugin` | 打包源码 | 发布源码jar |
| `maven-deploy-plugin` | 部署到私服 | 配置仓库地址 |

**配置示例——指定JDK版本并跳过测试**：

```xml
<build>
    <plugins>
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
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.1.2</version>
            <configuration>
                <skipTests>true</skipTests>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 1.7 多模块构建

中大型项目通常采用多模块架构，实现模块化开发和独立部署：

```
parent-project/
├── pom.xml                  # 父POM（聚合 + 继承）
├── common/                  # 公共工具模块
│   └── pom.xml
├── user-api/                # 用户服务API定义
│   └── pom.xml
├── user-service/            # 用户服务实现
│   └── pom.xml
└── gateway/                 # API网关
    └── pom.xml
```

**父POM配置**：

```xml
<!-- parent-project/pom.xml -->
<groupId>com.example</groupId>
<artifactId>parent-project</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>   <!-- 父POM打包类型必须为pom -->

<modules>
    <module>common</module>
    <module>user-api</module>
    <module>user-service</module>
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
```

**子模块POM配置**：

```xml
<!-- user-service/pom.xml -->
<parent>
    <groupId>com.example</groupId>
    <artifactId>parent-project</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>

<artifactId>user-service</artifactId>

<dependencies>
    <dependency>
        <groupId>${project.groupId}</groupId>
        <artifactId>common</artifactId>
    </dependency>
</dependencies>
```

**多模块构建命令**：

```bash
# 构建所有模块
mvn clean install

# 仅构建某个模块及其依赖模块
mvn clean install -pl user-service -am

# 跳过指定模块
mvn clean install -pl '!common'
```

### 1.8 私服（Nexus / Artifactory）

私服是团队内部托管构件（jar包、插件、POM）的仓库服务器，解决了以下问题：

1. **加速构建**：缓存公网依赖，避免重复下载
2. **内部共享**：发布团队内部库和SNAPSHOT版本
3. **访问控制**：管理第三方依赖的合规性

**settings.xml配置**：

```xml
<settings>
    <!-- 本地仓库路径 -->
    <localRepository>D:/maven/repository</localRepository>

    <!-- 镜像配置：使用阿里云镜像加速 -->
    <mirrors>
        <mirror>
            <id>aliyun-maven</id>
            <mirrorOf>central</mirrorOf>
            <name>阿里云公共仓库</name>
            <url>https://maven.aliyun.com/repository/central</url>
        </mirror>
    </mirrors>

    <!-- 私服认证 -->
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>deployer</username>
            <password>deployer-password</password>
        </server>
        <server>
            <id>nexus-snapshots</id>
            <username>deployer</username>
            <password>deployer-password</password>
        </server>
    </servers>
</settings>
```

**POM中配置部署地址**：

```xml
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <name>Releases Repository</name>
        <url>https://nexus.example.com/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <name>Snapshots Repository</name>
        <url>https://nexus.example.com/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

---

## 二、Maven实战

### 2.1 settings.xml 深入

`settings.xml`是Maven的全局/用户级配置文件，位于`~/.m2/settings.xml`。它包含以下核心配置：

**localRepository**：指定本地依赖缓存目录，默认为`~/.m2/repository`。建议将其移动到非系统盘空间充足的路径：

```xml
<localRepository>D:/maven/repository</localRepository>
```

**mirror**：为中央仓库配置镜像，通常使用阿里云或华为云镜像加速国内构建：

```xml
<mirror>
    <id>aliyun-public</id>
    <mirrorOf>*</mirrorOf>  <!-- 所有仓库都走该镜像 -->
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

`mirrorOf`的取值策略：
- `central`：仅镜像中央仓库（推荐，其他私有仓库仍直连）
- `*`：所有仓库都走镜像（简单粗暴）
- `external:*`：除本地仓库外的所有外部仓库
- `repo1,repo2`：逗号分隔的仓库ID列表

**server**：配置私服认证信息。`id`必须与POM中`distributionManagement`的`id`一致。

**profile**：定义一组配置（仓库列表、插件仓库、属性等），在新版Maven中也可以在此配置JDK版本、环境变量等。

### 2.2 常用命令详解

```bash
# 清理——删除target目录
mvn clean

# 编译——将src/main/java编译为class文件到target/classes
mvn compile

# 测试——运行src/test/java中的单元测试
mvn test

# 打包——生成jar/war到target目录
mvn package

# 安装——将包安装到本地仓库供其他项目使用
mvn install

# 部署——将包上传到私服
mvn deploy

# 验证——检查项目是否正确且所有必要信息可用
mvn verify
```

### 2.3 常用参数

```bash
# 跳过测试（编译测试类但不执行）
mvn package -DskipTests

# 完全跳过测试（连测试类都不编译）
mvn package -Dmaven.test.skip=true

# 指定profile
mvn package -P prod

# 指定模块构建（-pl：指定模块列表，-am：同时构建依赖模块）
mvn clean install -pl user-api,user-service -am

# 并行构建（-T：指定线程数，或使用乘法因子）
mvn clean install -T 4
mvn clean install -T 2.0C  # CPU核心数 * 2.0

# 离线模式（不从远程仓库下载）
mvn package -o

# 排除test代码编译
mvn compile -Dmaven.test.skip=true

# 调试输出
mvn package -X

# 从指定文件引入外部依赖版本属性
mvn package -Drevision=2.0.0
```

### 2.4 Profile 环境切换

Profile允许为不同环境（开发、测试、生产）指定不同的配置：

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
            <db.url>jdbc:mysql://localhost:3306/dev_db</db.url>
        </properties>
    </profile>

    <!-- 测试环境 -->
    <profile>
        <id>test</id>
        <properties>
            <env>test</env>
            <db.url>jdbc:mysql://test-db:3306/test_db</db.url>
        </properties>
    </profile>

    <!-- 生产环境 -->
    <profile>
        <id>prod</id>
        <properties>
            <env>prod</env>
            <db.url>jdbc:mysql://prod-db:3306/prod_db</db.url>
        </properties>
    </profile>
</profiles>
```

配合Maven资源过滤，可以在不同环境下自动替换配置文件中的占位符：

```xml
<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
            <filtering>true</filtering>  <!-- 启用资源过滤 -->
        </resource>
    </resources>
</build>
```

在`application.properties`中使用占位符：

```properties
db.url=${db.url}
env=${env}
```

构建时通过 `-P` 激活对应profile：

```bash
mvn clean package -P prod
```

---

---

## 三、Maven + Git CI/CD 集成

> 💡 Git 详细命令、分支策略、工作流等内容请参见 **[Git 学习文件夹](../Git/)**。本节聚焦 Maven 与 Git 在 CI/CD 中的协同要点。

### 3.1 协同核心要点

| 维度 | 实践 | 说明 |
|------|------|------|
| 版本联动 | Maven 版本号 ↔ Git Tag | 发布时版本号与 Tag 必须一致 |
| CI 流水线 | `git push` → `mvn clean verify` | 每次推送自动触发构建+测试 |
| 多环境 | Profile ↔ 分支 | dev 分支→dev profile，main→prod profile |
| 自动化发布 | `maven-release-plugin` + Git Tag | 一键打 Tag 并部署 |
| 依赖缓存 | CI 缓存 `~/.m2/repository` | 避免每次从远程下载 |

### 3.2 GitHub Actions CI 示例

```yaml
# .github/workflows/maven.yml
name: Java CI with Maven

on:
  push:
    branches: [ "main", "develop" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    - name: Build & Test
      run: mvn -B clean verify
    - name: Dependency Tree
      run: mvn dependency:tree -Dverbose
```

### 3.3 标准开发流程

```
git checkout -b feature/xxx
  → 开发 → mvn test
  → git commit -m "feat: xxx"
  → git pull --rebase origin main
  → git push origin feature/xxx
  → 创建 PR → Code Review
  → mvn clean verify -P prod
  → Merge → CI 自动部署
```

### 3.4 实践 Checklist

| 分类 | 检查项 |
|------|--------|
| Maven | `settings.xml` 配置镜像 + 私服 |
| Maven | `<dependencyManagement>` 统一版本 |
| Maven | Profile 管理多环境（dev/test/prod） |
| Maven | CI 中 `mvn dependency:tree` 检查冲突 |
| Git | `.gitignore` 排除 target/、.idea/、*.iml |
| Git | 提交遵循 Conventional Commits |
| Git | 分支保护 + PR Code Review |
| CI/CD | push/PR 自动触发 `mvn clean verify` |
| CI/CD | 缓存 `~/.m2/repository` 加速构建 |

---

## 参考资源

- Maven 官方文档：https://maven.apache.org/guides/
- Spring 官方 BOM：https://spring.io/projects/spring-boot
- 阿里云 Maven 仓库指南：https://developer.aliyun.com/mvn/guide
- Git 学习文件夹：[../Git/](../Git/)
- Conventional Commits：https://www.conventionalcommits.org/
