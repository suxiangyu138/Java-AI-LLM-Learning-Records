# 第5章 MyBatis 代码生成器

## 5.1 基本介绍

### 5.1.1 概念
**MyBatis Generator**，简称 **MBG**，是 MyBatis 官方提供的**代码自动生成工具**。

### 5.1.2 作用
根据数据库表，自动逆向生成：
1. 实体类（Entity）
2. Mapper 接口
3. Mapper XML 映射文件
4. 基础 CRUD 全套方法

### 5.1.3 适用场景
单表基础增删改查，重复、模板化代码一键生成，**大幅提升开发效率**，企业开发高频使用。

---

## 5.2 环境准备

### 5.2.1 引入依赖
`pom.xml` 新增 MBG 核心依赖与插件：
```xml
<!-- MyBatis 代码生成器核心依赖 -->
<dependency>
    <groupId>org.mybatis.generator</groupId>
    <artifactId>mybatis-generator-core</artifactId>
    <version>1.4.2</version>
</dependency>
```

同时配置 Maven 插件：
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.mybatis.generator</groupId>
            <artifactId>mybatis-generator-maven-plugin</artifactId>
            <version>1.4.2</version>
            <configuration>
                <!-- 配置文件路径 -->
                <configurationFile>src/main/resources/generator-config.xml</configurationFile>
                <overwrite>true</overwrite>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## 5.3 编写 MBG 核心配置文件
在 `resources` 下创建 **generator-config.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE generatorConfiguration
        PUBLIC "-//mybatis.org//DTD MyBatis Generator Configuration 1.0//EN"
        "http://mybatis.org/dtd/mybatis-generator-config_1_0.dtd">

<generatorConfiguration>
    <!-- 1. 数据库驱动路径（本地可不配，maven自动加载） -->
    <!-- 2. 环境配置 -->
    <context id="mysqlContext" targetRuntime="MyBatis3">

        <!-- 去除自动生成的注释 -->
        <commentGenerator>
            <property name="suppressAllComments" value="true"/>
        </commentGenerator>

        <!-- 3. 数据库连接信息 -->
        <jdbcConnection
                driverClass="com.mysql.cj.jdbc.Driver"
                connectionURL="jdbc:mysql://localhost:3306/mybatis_test?serverTimezone=UTC&amp;useSSL=false"
                userId="root"
                password="root">
        </jdbcConnection>

        <!-- 4. 类型转换 -->
        <javaTypeResolver>
            <property name="forceBigDecimals" value="false"/>
        </javaTypeResolver>

        <!-- 5. 生成实体类：指定存放包与路径 -->
        <javaModelGenerator targetPackage="com.pojo"
                            targetProject="src/main/java">
            <property name="enableSubPackages" value="true"/>
            <property name="trimStrings" value="true"/>
        </javaModelGenerator>

        <!-- 6. 生成 Mapper XML 文件 -->
        <sqlMapGenerator targetPackage="mapper"
                         targetProject="src/main/resources">
            <property name="enableSubPackages" value="true"/>
        </sqlMapGenerator>

        <!-- 7. 生成 Mapper 接口 -->
        <javaClientGenerator type="XMLMAPPER"
                             targetPackage="com.mapper"
                             targetProject="src/main/java">
            <property name="enableSubPackages" value="true"/>
        </javaClientGenerator>

        <!-- 8. 指定要生成代码的表
             tableName：数据库表名
             domainObjectName：对应实体类名
        -->
        <table tableName="user" domainObjectName="User"/>

    </context>
</generatorConfiguration>
```

---

## 5.4 运行代码生成器

### 方式一：Maven 插件运行
IDEA 右侧 → Maven → 当前项目 → Plugins → `mybatis-generator` → 双击运行：
```
mybatis-generator:generate
```

### 方式二：Java 代码运行（备选）
```java
public class GeneratorRun {
    public static void main(String[] args) throws Exception{
        List<String> argsList = new ArrayList<>();
        argsList.add("src/main/resources/generator-config.xml");
        MyBatisGenerator.main(argsList.toArray(new String[0]));
    }
}
```

---

## 5.5 自动生成的内容说明

### 1. 实体类（com.pojo）
- 包含数据表全部字段
- 无参/有参构造、get/set、toString
- 自动适配数据库字段类型

### 2. Mapper 接口（com.mapper）
内置常用单表方法：
- `selectByPrimaryKey` 根据主键查询
- `deleteByPrimaryKey` 根据主键删除
- `insert` 全字段新增
- `insertSelective` 选择性新增（非空字段）
- `updateByPrimaryKey` 全字段修改
- `updateByPrimaryKeySelective` 选择性修改

### 3. Mapper XML
- 对应接口所有方法的 SQL
- 自带基础动态 SQL 判断
- 字段与属性自动映射

---

## 5.6 拓展：条件查询 Example
MBG 默认生成 **Example** 条件对象，用于多条件动态查询：
```java
// 示例：条件查询
UserExample example = new UserExample();
example.createCriteria()
       .andAgeEqualTo(20)
       .andUsernameLike("%张%");
List<User> userList = userMapper.selectByExample(example);
```
> 作用：无需手写 XML，Java 链式代码实现动态多条件查询。

---

## 5.7 优缺点总结

### 优点
1. 单表 CRUD 零手写，开发效率极高
2. 自动处理字段映射、类型适配
3. 内置选择性增删改，避免空值覆盖
4. 支持 Example 条件查询，简化动态 SQL

### 缺点
1. 仅适用于**单表操作**
2. 多表联查、复杂业务仍需手动写 SQL
3. 生成代码冗余，部分方法用不到

---

## 5.8 企业使用规范
1. 基础单表 CRUD **直接使用 MBG 生成**
2. 复杂查询、多表关联、自定义业务 SQL **手动编写 XML**
3. 禁止直接修改生成的基础文件，自定义方法**追加编写**，避免重新生成覆盖代码

---
