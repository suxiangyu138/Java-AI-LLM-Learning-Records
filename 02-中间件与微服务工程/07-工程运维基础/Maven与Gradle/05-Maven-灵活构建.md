# Maven 灵活构建

## 一、单模块项目构建

### 标准 pom.xml（Spring Boot）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 继承 Spring Boot 父项目 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.10</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>demo-single</artifactId>
    <version>1.0.0-RELEASE</version>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
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
                    <mainClass>com.example.demo.DemoApplication</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 二、多模块项目构建

### 2.1 标准多模块结构

```
demo-parent/                        # 父项目（pom 类型）
├── pom.xml
├── demo-common/                    # 公共模块（jar）
│   └── pom.xml
├── demo-dao/                       # 数据访问层（jar）
│   └── pom.xml
├── demo-service/                   # 业务逻辑层（jar）
│   └── pom.xml
└── demo-api/                       # 接口层（jar/war）
    └── pom.xml
```

### 2.2 父项目 POM 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>demo-parent</artifactId>
    <version>1.0.0-RELEASE</version>
    <packaging>pom</packaging>   <!-- 父项目必须为 pom -->

    <!-- 声明所有子模块 -->
    <modules>
        <module>demo-common</module>
        <module>demo-dao</module>
        <module>demo-service</module>
        <module>demo-api</module>
    </modules>

    <!-- 全局属性 -->
    <properties>
        <java.version>1.8</java.version>
        <spring-boot.version>2.7.10</spring-boot.version>
        <mysql.version>8.0.33</mysql.version>
    </properties>

    <!-- 统一依赖版本管理（仅声明版本，不引入依赖）-->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>mysql</groupId>
                <artifactId>mysql-connector-java</artifactId>
                <version>${mysql.version}</version>
            </dependency>
            <!-- 子模块间依赖版本管理 -->
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>demo-common</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 统一插件配置 -->
    <build>
        <pluginManagement>
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
        </pluginManagement>
    </build>
</project>
```

### 2.3 子模块 POM 配置

```xml
<!-- demo-service/pom.xml -->
<project ...>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>demo-parent</artifactId>
        <version>1.0.0-RELEASE</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>demo-service</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- 依赖 common 模块（版本继承父项目） -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>demo-common</artifactId>
        </dependency>
        <!-- 依赖 dao 模块 -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>demo-dao</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 2.4 多模块构建命令

```bash
# 构建所有模块
mvn clean install

# 仅构建指定模块及其依赖
mvn clean package -pl demo-api -am
# -pl: 指定模块
# -am: also-make，同时构建依赖模块
# -amd: also-make-dependents，同时构建下游依赖模块
```

## 三、多环境构建（Profile）

### 3.1 配置文件结构

```
src/main/resources/
├── application.yml           # 主配置
├── application-dev.yml       # 开发环境
├── application-test.yml      # 测试环境
└── application-prod.yml      # 生产环境
```

### 3.2 pom.xml Profile 配置

```xml
<profiles>
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <env>dev</env>
        </properties>
    </profile>
    <profile>
        <id>test</id>
        <properties>
            <env>test</env>
        </properties>
    </profile>
    <profile>
        <id>prod</id>
        <properties>
            <env>prod</env>
        </properties>
    </profile>
</profiles>

<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
            <filtering>true</filtering>    <!-- 开启资源过滤，替换 ${} 占位符 -->
            <includes>
                <include>application.yml</include>
                <include>application-${env}.yml</include>
            </includes>
        </resource>
    </resources>
</build>
```

### 3.3 多环境打包命令

```bash
mvn clean package -Pdev      # 开发环境
mvn clean package -Ptest     # 测试环境
mvn clean package -Pprod     # 生产环境
mvn clean package -Pprod -DskipTests   # 生产环境 + 跳过测试
```

## 四、依赖优化

### 4.1 常用命令

```bash
# 查看依赖树（排查冲突）
mvn dependency:tree

# 分析未使用/未声明的依赖
mvn dependency:analyze

# 查看重复依赖
mvn dependency:analyze-duplicate
```

### 4.2 优化策略

| 策略 | 说明 |
|------|------|
| **dependencyManagement** | 父 POM 统一版本，子模块不写 version |
| **exclusions** | 排除传递依赖中的冲突版本 |
| **依赖范围优化** | test 范围依赖不打包，runtime 范围不参与编译 |
| **瘦包** | Spring Boot 可通过配置排除内置依赖，减小 JAR 体积 |

## 五、常见构建问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 依赖冲突 | 多版本传递依赖 | `mvn dependency:tree` → 排除或统一版本 |
| 子模块找不到依赖 | 父模块未 install | 先执行 `mvn clean install` |
| 打包失败 | 编译错误/依赖缺失 | 检查代码 + pom 依赖完整性 |
| 下载缓慢 | 中央仓库在国外 | 配置阿里云镜像 |
| 多环境配置不生效 | 未开启 filtering | 设置 `<filtering>true</filtering>` |
