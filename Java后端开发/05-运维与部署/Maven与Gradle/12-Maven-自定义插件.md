# Maven 自定义插件开发

## 一、概述

Maven 插件本质是一个 Java 项目（打包类型 `maven-plugin`），通过 **MOJO**（Maven Plain Old Java Object）实现具体功能，与 Maven 生命周期绑定。

### 适用场景

| 场景 | 示例 |
|------|------|
| 代码生成 | 根据数据库表生成实体类、Mapper |
| 规范校验 | 强制代码注释、禁止非法依赖 |
| 自定义打包 | 按模块拆分 JAR、注入环境变量 |
| 自动化部署 | SSH 上传 + 远程启动 |

## 二、MOJO 核心概念

- **MOJO**：继承 `AbstractMojo`，实现 `execute()` 方法
- **注解配置**：`@Mojo` 定义目标名和绑定阶段
- **参数配置**：`@Parameter` 定义可配置参数

```java
@Mojo(
    name = "generate",                              // 目标名
    defaultPhase = LifecyclePhase.COMPILE,          // 绑定阶段
    description = "根据数据库表生成实体类"
)
public class CodeGenerateMojo extends AbstractMojo {

    @Parameter(property = "db.url", required = true)
    private String dbUrl;

    @Parameter(property = "entity.package", required = true)
    private String entityPackage;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("开始执行...");
        // 自定义逻辑
    }
}
```

## 三、插件项目结构

```
custom-maven-plugin/
├── pom.xml                              # packaging: maven-plugin
└── src/main/java/com/example/plugin/
    └── CodeGenerateMojo.java            # MOJO 实现类
```

### pom.xml 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project ...>
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>custom-code-generate-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>maven-plugin</packaging>        <!-- 关键 -->

    <dependencies>
        <!-- Maven 插件核心 API -->
        <dependency>
            <groupId>org.apache.maven</groupId>
            <artifactId>maven-plugin-api</artifactId>
            <version>3.8.8</version>
            <scope>provided</scope>
        </dependency>
        <!-- 插件注解 -->
        <dependency>
            <groupId>org.apache.maven.plugin-tools</groupId>
            <artifactId>maven-plugin-annotations</artifactId>
            <version>3.7.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugin-tools</groupId>
                <artifactId>maven-plugin-plugin</artifactId>
                <version>3.7.0</version>
                <configuration>
                    <goalPrefix>code-generate</goalPrefix>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 四、完整示例：代码生成插件

### 4.1 MOJO 实现

```java
package com.example.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import java.sql.*;

@Mojo(
    name = "generate",
    defaultPhase = LifecyclePhase.COMPILE,
    description = "根据数据库表生成 Java 实体类"
)
public class CodeGenerateMojo extends AbstractMojo {

    @Parameter(property = "db.url", required = true)
    private String dbUrl;

    @Parameter(property = "db.username", required = true)
    private String dbUsername;

    @Parameter(property = "db.password", required = true)
    private String dbPassword;

    @Parameter(property = "table.name", required = true)
    private String tableName;

    @Parameter(property = "entity.package", required = true)
    private String entityPackage;

    @Parameter(property = "entity.path",
               defaultValue = "${project.basedir}/src/main/java")
    private String entityPath;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("=== 开始代码生成 ===");
        try {
            // 1. 连接数据库，获取表结构
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("DESCRIBE " + tableName);

            // 2. 处理字段信息（此处简化，实际可用 Freemarker 模板生成完整类）
            StringBuilder fields = new StringBuilder();
            while (rs.next()) {
                String column = rs.getString("Field");
                String type = rs.getString("Type");
                fields.append(String.format("    private %s %s;\n",
                    mapType(type), underlineToCamel(column)));
            }

            // 3. 生成实体类文件
            String className = underlineToCamel(tableName);
            className = className.substring(0, 1).toUpperCase() + className.substring(1);
            String fileContent = String.format(
                "package %s;\n\npublic class %s {\n%s\n}",
                entityPackage, className, fields.toString()
            );

            // 写入文件...

            rs.close(); stmt.close(); conn.close();
            getLog().info("=== 实体类生成成功 ===");
        } catch (Exception e) {
            throw new MojoExecutionException("代码生成失败", e);
        }
    }

    /** 下划线转驼峰 */
    private String underlineToCamel(String str) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : str.toCharArray()) {
            if (c == '_') { upper = true; }
            else {
                sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                upper = false;
            }
        }
        return sb.toString();
    }

    /** MySQL 类型映射为 Java 类型 */
    private String mapType(String mysqlType) {
        if (mysqlType.startsWith("int") || mysqlType.startsWith("tinyint")) return "Integer";
        if (mysqlType.startsWith("bigint")) return "Long";
        if (mysqlType.startsWith("varchar") || mysqlType.startsWith("text")) return "String";
        if (mysqlType.startsWith("datetime") || mysqlType.startsWith("timestamp")) return "Date";
        if (mysqlType.startsWith("decimal")) return "BigDecimal";
        return "Object";
    }
}
```

### 4.2 核心注解速查

| 注解 | 属性 | 说明 |
|------|------|------|
| `@Mojo` | `name` | 目标名，调用时使用 `前缀:name` |
| | `defaultPhase` | 默认绑定生命周期阶段 |
| | `requiresDependencyResolution` | 是否需要依赖解析 |
| `@Parameter` | `property` | 参数名 |
| | `required` | 是否必填 |
| | `defaultValue` | 默认值 |
| `@Execute` | `phase` | 前置阶段 |
| | `goal` | 前置目标 |

## 五、插件调试与部署

### 5.1 安装到本地

```bash
mvn clean install
```

### 5.2 项目引用

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.example</groupId>
            <artifactId>custom-code-generate-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <configuration>
                <dbUrl>jdbc:mysql://localhost:3306/test</dbUrl>
                <dbUsername>root</dbUsername>
                <dbPassword>123456</dbPassword>
                <tableName>user</tableName>
                <entityPackage>com.example.demo.entity</entityPackage>
            </configuration>
            <executions>
                <execution>
                    <phase>compile</phase>
                    <goals><goal>generate</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 5.3 使用方式

```bash
# 自动执行（绑定到 compile）
mvn compile

# 手动调用
mvn code-generate:generate
```

## 六、进阶场景

### 6.1 规范校验插件

```java
@Mojo(name = "check", defaultPhase = LifecyclePhase.VALIDATE)
public class CodeCheckMojo extends AbstractMojo {
    @Override
    public void execute() throws MojoExecutionException {
        // 扫描源码，校验命名规范、注释完整性
        // 不符合规范则抛出 MojoFailureException 终止构建
    }
}
```

### 6.2 自动化部署插件

```java
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class AutoDeployMojo extends AbstractMojo {
    @Override
    public void execute() throws MojoExecutionException {
        // SSH 连接服务器 → 上传 JAR → 执行启动脚本 → 输出日志
    }
}
```

## 七、常见问题

| 问题 | 解决方案 |
|------|---------|
| 项目引用提示"找不到插件" | 确认 `mvn clean install` 成功，GAV 坐标一致 |
| 参数注入失败 | 检查 `@Parameter` 的 `property` 与 `pom.xml` 配置名一致 |
| 资源文件找不到 | 资源放 `src/main/resources`，用 `getClass().getResourceAsStream()` |
| 插件未自动执行 | 确认 `<executions>` 中 `phase` 与执行的 Maven 命令匹配 |
| 不同 Maven 版本兼容 | `maven-plugin-api` 版本设低，避免高版本专有 API |
