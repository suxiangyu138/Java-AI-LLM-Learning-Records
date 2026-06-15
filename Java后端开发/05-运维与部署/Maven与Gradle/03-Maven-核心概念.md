# Maven 核心概念

## 一、Maven 核心定位

Maven 是一套**标准化的项目构建与依赖管理框架**，解决 Java 后端开发三大痛点：

| 痛点 | Maven 解决方案 |
|------|---------------|
| 依赖管理混乱 | 通过 GAV 坐标 + 中央仓库自动化管理依赖 |
| 构建流程不统一 | 标准化生命周期，统一编译、测试、打包、部署 |
| 项目结构不规范 | 约定优于配置，强制标准目录结构 |

## 二、POM（Project Object Model）

POM 是 Maven 的核心，通过 `pom.xml` 描述项目的所有配置信息。

### 核心配置结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- === 项目坐标（GAV）=== -->
    <groupId>com.company.project</groupId>
    <artifactId>demo-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <!-- === 依赖管理 === -->
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>2.7.10</version>
        </dependency>
    </dependencies>

    <!-- === 构建配置 === -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### POM 继承与聚合

多模块项目中，父 POM 统一管理版本，子模块继承父 POM 配置：

```xml
<!-- 父 POM（packaging 必须为 pom）-->
<packaging>pom</packaging>
<modules>
    <module>common</module>
    <module>user-service</module>
    <module>order-service</module>
</modules>

<!-- 子 POM 继承 -->
<parent>
    <groupId>com.company.project</groupId>
    <artifactId>project-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
<artifactId>user-service</artifactId>
```

## 三、GAV 坐标体系

项目的唯一标识，三个属性缺一不可：

| 属性 | 说明 | 示例 |
|------|------|------|
| **groupId** | 组织/团队标识（反向域名） | `org.springframework.boot` |
| **artifactId** | 项目/模块名称 | `spring-boot-starter-web` |
| **version** | 项目版本号 | `2.7.10` |

## 四、依赖管理（Dependency）

### 依赖范围（Scope）

| Scope | 编译 | 测试 | 运行 | 打包 | 典型场景 |
|-------|:----:|:----:|:----:|:----:|---------|
| `compile`（默认）| ✅ | ✅ | ✅ | ✅ | Spring Core、MyBatis |
| `test` | ❌ | ✅ | ❌ | ❌ | JUnit、Mockito |
| `provided` | ✅ | ✅ | ❌ | ❌ | Servlet API（Tomcat 提供） |
| `runtime` | ❌ | ✅ | ✅ | ✅ | MySQL 驱动 |
| `system` | ✅ | ✅ | ❌ | ❌ | 本地 jar（不推荐） |

### 依赖传递规则

当 A → B → C 时，Maven 自动下载 A、B、C：

- **最短路径优先**：路径短的版本优先
- **声明优先**：同层级先声明的优先
- **dependencyManagement 优先**：强制指定版本，覆盖所有传递依赖

### 依赖冲突解决

```bash
# 查看依赖树
mvn dependency:tree
```

```xml
<!-- 方式 1：排除冲突依赖 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 方式 2：dependencyManagement 统一版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <version>5.3.20</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 五、构建生命周期

### Default 生命周期（核心）

| 阶段 | 作用 | 命令 |
|------|------|------|
| `validate` | 验证项目配置 | `mvn validate` |
| `compile` | 编译源码 | `mvn compile` |
| `test` | 执行单元测试 | `mvn test` |
| `package` | 打包（jar/war） | `mvn package` |
| `verify` | 验证包质量 | `mvn verify` |
| `install` | 安装到本地仓库 | `mvn install` |
| `deploy` | 部署到远程仓库 | `mvn deploy` |

### Clean 生命周期

```bash
mvn clean    # 删除 target 目录
```

**规律**：执行后续阶段会自动执行前置阶段。如 `mvn package` 自动执行 validate → compile → test → package。

### 常用组合命令

```bash
mvn clean compile        # 清理 + 编译
mvn clean test           # 清理 + 编译 + 测试
mvn clean package        # 清理 + 编译 + 测试 + 打包（最常用）
mvn clean install        # + 安装到本地仓库
mvn clean deploy         # + 部署到远程仓库
mvn dependency:tree      # 查看依赖树（排查冲突）
```

## 六、插件（Plugin）

Maven 所有构建功能均由插件实现：

| 插件 | 作用 | 必配 |
|------|------|:----:|
| `maven-compiler-plugin` | 编译 Java 源码，指定 JDK 版本 | ✅ |
| `maven-surefire-plugin` | 执行单元测试 | ✅ |
| `maven-jar-plugin` | 打包为 jar | ✅ |
| `maven-war-plugin` | 打包为 war | Web 项目必配 |
| `spring-boot-maven-plugin` | Spring Boot 打包 + 运行 | Spring Boot |
| `maven-surefire-report-plugin` | 生成测试报告 | 可选 |
| `jacoco-maven-plugin` | 代码覆盖率 | 推荐 |

## 七、标准目录结构

```
project-root/
├── pom.xml                          # Maven 核心配置
├── src/
│   ├── main/
│   │   ├── java/                    # Java 源代码
│   │   │   └── com/company/project/
│   │   │       ├── controller/      # 控制层
│   │   │       ├── service/         # 业务层
│   │   │       ├── dao/             # 数据访问层
│   │   │       └── entity/          # 实体层
│   │   └── resources/               # 配置文件
│   │       ├── application.yml
│   │       └── mapper/
│   └── test/
│       ├── java/                    # 测试代码
│       └── resources/               # 测试资源
└── target/                          # 构建产物（自动生成）
    ├── classes/                     # 编译后的 class
    ├── surefire-reports/            # 测试报告
    └── *.jar                        # 打包产物
```

## 八、概念串联

```
pom.xml（配置入口）
  ↓ 定义坐标 + 依赖
仓库（Repository） ← 按 GAV 定位并下载
  ↓ 依赖就绪
插件（Plugin） → 执行生命周期（Lifecycle）各阶段
  ↓
compile → test → package → install → deploy
```
