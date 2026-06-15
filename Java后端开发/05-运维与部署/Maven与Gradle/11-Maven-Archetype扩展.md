# Maven Archetype 扩展

## 一、概述

Archetype（原型）是 Maven 的项目模板机制，通过预定义的目录结构、依赖和配置，快速生成符合规范的项目骨架。

### 核心价值

| 场景 | 价值 |
|------|------|
| 团队项目标准化 | 统一包结构、依赖版本、配置文件 |
| 新项目快速启动 | 几分钟生成完整项目骨架 |
| 新人上手 | 生成的项目直接符合团队规范 |

## 二、Archetype 标准目录结构

```
custom-archetype/                        # Archetype 项目自身
├── pom.xml                              # 打包类型：maven-archetype
└── src/main/resources/
    ├── META-INF/maven/
    │   └── archetype-metadata.xml       # 核心描述文件
    └── archetype-resources/             # 模板资源（渲染为实际项目）
        ├── pom.xml                      # 项目 pom 模板
        ├── .gitignore
        └── src/
            ├── main/java/${package}/
            │   ├── controller/
            │   ├── service/
            │   ├── dao/
            │   ├── entity/
            │   └── ${artifactId}Application.java
            ├── main/resources/
            │   └── application.yml
            └── test/java/${package}/
```

> `${package}`、`${artifactId}` 等为动态参数，生成项目时被替换。

## 三、创建自定义 Archetype

### 3.1 创建 Archetype 项目

```bash
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=custom-springboot-archetype \
  -DarchetypeArtifactId=maven-archetype-archetype \
  -DinteractiveMode=false
```

### 3.2 Archetype 自身 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project ...>
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>custom-springboot-archetype</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>maven-archetype</packaging>   <!-- 关键 -->

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-archetype-plugin</artifactId>
                <version>3.2.1</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3.3 核心配置文件：archetype-metadata.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<archetype-descriptor
    xmlns="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.0.0
    http://maven.apache.org/xsd/archetype-descriptor-1.0.0.xsd">

    <!-- 定义参数 -->
    <requiredProperties>
        <requiredProperty key="groupId">
            <defaultValue>com.example</defaultValue>
        </requiredProperty>
        <requiredProperty key="artifactId"/>         <!-- 无默认值，必填 -->
        <requiredProperty key="version">
            <defaultValue>1.0.0-SNAPSHOT</defaultValue>
        </requiredProperty>
        <requiredProperty key="package">
            <defaultValue>${groupId}.${artifactId}</defaultValue>
        </requiredProperty>
        <!-- 自定义参数 -->
        <requiredProperty key="server.port">
            <defaultValue>8080</defaultValue>
        </requiredProperty>
    </requiredProperties>

    <!-- 定义模板文件 -->
    <fileSets>
        <!-- Java 源码（filtered=true 替换参数） -->
        <fileSet filtered="true" encoding="UTF-8">
            <directory>src/main/java</directory>
            <includes><include>**/*.java</include></includes>
        </fileSet>
        <!-- 测试代码 -->
        <fileSet filtered="true" encoding="UTF-8">
            <directory>src/test/java</directory>
            <includes><include>**/*.java</include></includes>
        </fileSet>
        <!-- 配置文件 -->
        <fileSet filtered="true" encoding="UTF-8">
            <directory>src/main/resources</directory>
            <includes>
                <include>application.yml</include>
                <include>logback.xml</include>
            </includes>
        </fileSet>
        <!-- 根目录文件 -->
        <fileSet filtered="true" encoding="UTF-8">
            <directory>.</directory>
            <includes>
                <include>pom.xml</include>
                <include>.gitignore</include>
            </includes>
        </fileSet>
    </fileSets>
</archetype-descriptor>
```

### 3.4 模板文件示例

**pom.xml 模板**（archetype-resources/pom.xml）：

```xml
<groupId>${groupId}</groupId>
<artifactId>${artifactId}</artifactId>
<version>${version}</version>

<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.10</version>
</parent>

<properties>
    <java.version>1.8</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

**application.yml 模板**（支持自定义参数）：

```yaml
server:
  port: ${server.port}
spring:
  application:
    name: ${artifactId}
```

### 3.5 安装与发布

```bash
# 安装到本地仓库
mvn clean install

# 部署到远程仓库（团队共享）
mvn clean deploy
```

### 3.6 使用自定义 Archetype

```bash
# 命令行使用
mvn archetype:generate \
  -DarchetypeGroupId=com.example \
  -DarchetypeArtifactId=custom-springboot-archetype \
  -DarchetypeVersion=1.0.0-SNAPSHOT \
  -DgroupId=com.company \
  -DartifactId=new-project \
  -DinteractiveMode=false

# IDEA 使用：New Project → Create from archetype → Add Archetype
```

## 四、进阶技巧

### 4.1 自定义参数

```xml
<requiredProperty key="db.name">
    <defaultValue>${artifactId}</defaultValue>
</requiredProperty>
```

模板中使用：`url: jdbc:mysql://localhost:3306/${db.name}`

### 4.2 集成自定义插件

在模板 pom.xml 中预配置团队自定义插件，生成的项目自动拥有：

```xml
<plugin>
    <groupId>com.example</groupId>
    <artifactId>custom-code-gen-plugin</artifactId>
    <version>1.0.0</version>
</plugin>
```

## 五、注意事项

| 注意点 | 说明 |
|--------|------|
| **目录结构** | 必须严格遵循 Archetype 标准目录 |
| **编码统一** | 所有模板文件 + fileSet 配置 UTF-8 |
| **参数一致性** | metadata.xml 定义的参数必须与模板中使用的完全一致 |
| **版本管理** | Archetype 自身也需版本管理，模板变更后升级版本 |
| **必测** | 安装后必须先测试生成项目是否能正常编译运行 |

## 六、常见问题

| 问题 | 解决方案 |
|------|---------|
| 参数替换失败 | 检查 metadata.xml 中 `filtered="true"` 和参数定义 |
| 生成项目报错 | 检查模板 Java 文件的 `package` 是否使用 `${package}` |
| 找不到 Archetype | 确认已 `mvn clean install`，GAV 坐标一致 |
| 中文乱码 | 所有文件 + fileSet 配置 `encoding="UTF-8"` |
