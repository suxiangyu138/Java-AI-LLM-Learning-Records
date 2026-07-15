# MyBatis 代码生成器（MBG）使用教程

> **定位**：MBG（MyBatis Generator）根据数据库表结构自动生成实体类、Mapper 接口、XML 映射文件，消除基础 CRUD 的重复编码。

---

## 目录

1. [核心作用](#1-核心作用)
2. [环境准备](#2-环境准备)
3. [Maven 插件配置](#3-maven-插件配置)
4. [执行代码生成](#4-执行代码生成)
5. [生成后优化](#5-生成后优化)
6. [常见问题与避坑](#6-常见问题与避坑)

---

## 1. 核心作用

| 生成内容 | 说明 |
|----------|------|
| **实体类（POJO）** | 与表字段对应，getter/setter/toString/构造方法 |
| **Mapper 接口** | `selectByPrimaryKey`、`insert`、`updateByPrimaryKey`、`deleteByPrimaryKey`、`selectAll` |
| **Mapper XML** | 对应接口方法的 SQL（静态 SQL，可扩展为动态 SQL） |
| Example 类（可选） | 多条件查询辅助类 |

> ⚠️ MBG 生成的是**基础代码**，复杂 SQL（多表关联、复杂动态 SQL）仍需手动编写。

---

## 2. 环境准备

| 条件 | 说明 |
|------|------|
| 开发环境 | IDEA + Maven |
| 数据库 | MySQL（需有可访问的数据库和表） |
| 项目结构 | `pojo` / `mapper` 包 + `resources` 目录 |
| 核心依赖 | MyBatis + MySQL 驱动（MBG Maven 插件自动引入） |

---

## 3. Maven 插件配置

### 3.1 pom.xml 添加 MBG 插件

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.mybatis.generator</groupId>
            <artifactId>mybatis-generator-maven-plugin</artifactId>
            <version>1.4.2</version>
            <configuration>
                <configurationFile>src/main/resources/generatorConfig.xml</configurationFile>
                <overwrite>true</overwrite>   <!-- 允许覆盖已生成代码 -->
                <verbose>true</verbose>
            </configuration>
            <dependencies>
                <dependency>
                    <groupId>mysql</groupId>
                    <artifactId>mysql-connector-java</artifactId>
                    <version>8.0.30</version>
                </dependency>
                <dependency>
                    <groupId>org.mybatis</groupId>
                    <artifactId>mybatis</artifactId>
                    <version>3.5.10</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```

### 3.2 编写 generatorConfig.xml

> 放在 `src/main/resources/` 目录下。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE generatorConfiguration
    PUBLIC "-//mybatis.org//DTD MyBatis Generator Configuration 1.0//EN"
    "https://mybatis.org/dtd/mybatis-generator-config_1_0.dtd">
<generatorConfiguration>
    <context id="DB2Tables" targetRuntime="MyBatis3">
        <!-- 关闭注释生成 -->
        <commentGenerator>
            <property name="suppressAllComments" value="true"/>
        </commentGenerator>

        <!-- 数据库连接 -->
        <jdbcConnection driverClass="com.mysql.cj.jdbc.Driver"
            connectionURL="jdbc:mysql://localhost:3306/mybatis_db?serverTimezone=UTC"
            userId="root" password="123456"/>

        <!-- 实体类 -->
        <javaModelGenerator targetPackage="com.example.pojo"
            targetProject="src/main/java">
            <property name="trimStrings" value="true"/>
        </javaModelGenerator>

        <!-- Mapper XML -->
        <sqlMapGenerator targetPackage="com.example.mapper"
            targetProject="src/main/resources"/>

        <!-- Mapper 接口 -->
        <javaClientGenerator type="XMLMAPPER"
            targetPackage="com.example.mapper"
            targetProject="src/main/java"/>

        <!-- 数据库表配置 -->
        <table tableName="user" domainObjectName="User">
            <generatedKey column="id" sqlStatement="MySQL" identity="true"/>
        </table>
    </context>
</generatorConfiguration>
```

**关键配置说明**：

| 配置项 | 说明 |
|--------|------|
| `jdbcConnection` | 替换为实际数据库 URL、用户名、密码 |
| `javaModelGenerator.targetPackage` | 实体类包路径 |
| `sqlMapGenerator.targetPackage` | XML 文件包路径 |
| `javaClientGenerator.targetPackage` | Mapper 接口包路径 |
| `table.tableName` | 数据库表名（区分大小写） |
| `generatedKey` | 自增主键配置 |

---

## 4. 执行代码生成

```text
IDEA → Maven 面板 → Plugins → mybatis-generator → mybatis-generator:generate → 双击运行
```

**生成文件**：

| 位置 | 文件 |
|------|------|
| `src/main/java/com/example/pojo/` | `User.java` |
| `src/main/java/com/example/mapper/` | `UserMapper.java` |
| `src/main/resources/com/example/mapper/` | `UserMapper.xml` |

---

## 5. 生成后优化

| 优化项 | 做法 |
|--------|------|
| 实体类 | 添加 `@Data`（Lombok）、删除多余构造方法 |
| Mapper 接口 | 删除不需要的方法、添加自定义方法 |
| Mapper XML | 将静态 SQL 扩展为动态 SQL |
| 关闭 Example | 添加 `enableSelectByExample="false"` 等配置 |

---

## 6. 常见问题与避坑

| 问题 | 解决 |
|------|------|
| **无文件生成** | 检查 `generatorConfig.xml` 路径、数据库连接、表名大小写 |
| **Mapper 与 XML 不匹配** | 检查 `namespace`、`targetPackage`、方法名与 SQL id 一致 |
| **无 getter/setter** | 升级 MBG 到 1.4.2+ 或手动添加 / Lombok |
| **自增主键未忽略** | 添加 `<generatedKey column="id" sqlStatement="MySQL" identity="true"/>` |
| **覆盖误删** | 生成前备份已有代码 |

> 🎯 **核心流程**：配置 Maven 插件 → 编写 generatorConfig.xml → 执行 generate → 优化生成代码。
