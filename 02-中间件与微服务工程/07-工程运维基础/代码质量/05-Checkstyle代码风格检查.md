# Checkstyle 代码风格检查
> Checkstyle 是 Java 代码风格的"风格警察"，通过解析源代码检查缩进、命名、Javadoc、导入等格式规范，确保团队代码风格一致，是代码审查自动化的第一道防线。

## 目录
1. [Checkstyle 概述](#1-checkstyle-概述)
2. [Google Java Style vs Sun Code Conventions vs 自定义规则](#2-google-java-style-vs-sun-code-conventions-vs-自定义规则)
3. [Maven/Gradle 插件配置](#3-mavengradle-插件配置)
4. [checkstyle.xml 配置文件结构](#4-checkstylexml-配置文件结构)
5. [核心检查项详解与代码示例](#5-核心检查项详解与代码示例)
6. [Suppressions：抑制误报](#6-suppressions抑制误报)
7. [自定义 Checks 开发简介](#7-自定义-checks-开发简介)
8. [IDE 集成（IntelliJ CheckStyle-IDEA）](#8-ide-集成intellij-checkstyle-idea)
9. [Google Java Format vs Checkstyle 自动修复](#9-google-java-format-vs-checkstyle-自动修复)
10. [CI/CD 集成与失败控制](#10-cicd-集成与失败控制)
11. [完整实战：SpringBoot 项目 Google Java Style 强制](#11-完整实战springboot-项目-google-java-style-强制)
12. [常见问题与最佳实践](#12-常见问题与最佳实践)

---

## 1. Checkstyle 概述

### 1.1 什么是 Checkstyle？

Checkstyle 是一个 **源代码级静态分析工具**，专注于检查和强制执行 Java 代码的 **编码风格和格式规范**。

| 属性 | 说明 |
|------|------|
| 全称 | Checkstyle |
| 分析层次 | 源码（.java 文件） |
| 分析技术 | Token 流 + AST（抽象语法树） |
| 创始人 | Oliver Burn |
| 当前版本 | 10.x（2024+） |
| 许可证 | LGPL-2.1+ |
| 内建检查 | 170+ |
| 内置规范 | Google Java Style, Sun Code Conventions |

### 1.2 Checkstyle 的核心价值

```
Checkstyle 解决的核心问题：
"为什么同样的代码风格，不同人写出来差异这么大？"

通过自动化工具强制执行风格规范，让团队：
  ✓ Code Review 聚焦逻辑而非格式
  ✓ 代码差异显示真实变更而非格式调整
  ✓ 新人快速融入团队编码风格
  ✓ 降低代码维护的认知成本
```

### 1.3 适用场景

| 场景 | 说明 |
|------|------|
| **统一团队编码风格** | 强制执行一致的格式规范 |
| **Code Review 预处理** | 自动过滤格式问题，Review 聚焦逻辑 |
| **CI 质量门禁** | PR 合并前检查风格合规 |
| **开源项目合规** | 满足开源项目的贡献风格要求 |
| **遗留系统规范化** | 逐步统一老代码格式 |

---

## 2. Google Java Style vs Sun Code Conventions vs 自定义规则

### 2.1 内置规范对比

| 维度 | Google Java Style | Sun Code Conventions |
|------|------------------|---------------------|
| **起源** | Google 内部规范 | Sun Microsystems 官方规范 |
| **版本** | 2024（持续更新） | 1999（已停止更新） |
| **缩进** | 2 空格 | 4 空格 |
| **行宽** | 100 字符 | 80 字符 |
| **花括号** | 行尾风格（Egyptian 括号） | 行尾风格 |
| **空行规范** | 严格的垂直空白规则 | 较宽松 |
| **包名** | 全小写，允许分段 | 全小写 |
| **注解位置** | 独立行（类/方法），同行（字段/参数） | 未明确定义 |
| **文件编码** | UTF-8 | 未强制 |
| **当前状态** | 推荐、持续维护 | 主流不再使用 |

### 2.2 Google Java Style 核心规则

| 规则 | Google Java Style | 说明 |
|------|------------------|------|
| 缩进 | 2 空格 | 不使用 tab |
| 行宽 | 100 字符 | 超过需换行 |
| 花括号 | `if () {` 行尾 | Egyptian 风格 |
| 空块 | `{}` 或 `// empty` | 需注释 |
| 一行一声明 | 强制 | 不允许多个变量在同一行 |
| switch | 必须包含 break | fall-through 需注释 |
| 注解 | 类/方法前独立行 | 字段/参数同行 |
| 导入 | 无通配符 | 禁止 `import *` |
| 排序 | static → 非 static | 按字母排序 |
| 空格 | 运算符两侧 | 保留空格规则 |
| 文件头 | 可选 | - |

### 2.3 Sun Code Conventions 核心规则

| 规则 | Sun Code Conventions |
|------|---------------------|
| 缩进 | 4 空格 |
| 行宽 | 80 字符 |
| 花括号 | `if () {` 行尾风格 |
| 注释 | 强制 Javadoc（公有成员） |

### 2.4 自定义规则：选择与取舍

```xml
<!-- 在 Google Style 基础上自定义 -->
<module name="Checker">
    <!-- 从 Google Style 配置开始 -->
    <module name="TreeWalker">
        <!-- 继承 Google 的命名规范 -->
        <module name="com.puppycrawl.tools.checkstyle.checks.naming
            .ConstantNameCheck">
            <property name="format"
                value="^log(ger)?|[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$"/>
        </module>

        <!-- 调整行宽为 120（Google 默认 100） -->
        <module name="LineLength">
            <property name="max" value="120"/>
            <property name="ignorePattern"
                value="^package.*|^import.*|a href|href|http://|https://|ftp://"/>
        </module>

        <!-- 添加自定义团队规则 -->
        <module name="com.puppycrawl.tools.checkstyle.checks.design
            .HideUtilityClassConstructorCheck"/>
    </module>
</module>
```

---

## 3. Maven/Gradle 插件配置

### 3.1 Maven 配置（maven-checkstyle-plugin）

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.1</version>
    <configuration>
        <!-- 使用 Google Java Style -->
        <configLocation>config/checkstyle/checkstyle.xml</configLocation>
        <!-- 或使用内置规范 -->
        <!-- <configLocation>google_checks.xml</configLocation> -->
        <!-- <configLocation>sun_checks.xml</configLocation> -->

        <!-- 违规严重级别：error/warning/info -->
        <maxAllowedViolations>0</maxAllowedViolations>
        <violationSeverity>warning</violationSeverity>

        <!-- 编码 -->
        <charset>UTF-8</charset>

        <!-- 排除文件 -->
        <excludes>**/generated/**/*.java</excludes>

        <!-- 输出格式 -->
        <format>xml</format>
        <outputFile>${project.build.directory}/checkstyle/checkstyle-result.xml</outputFile>
    </configuration>
    <executions>
        <execution>
            <id>checkstyle-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <phase>verify</phase>
        </execution>
    </executions>
</plugin>
```

执行命令：

```bash
mvn checkstyle:check          # 检查风格（阻断构建）
mvn checkstyle:checkstyle     # 生成报告（不阻断）
mvn checkstyle:gui            # 启动 GUI
mvn verify                    # 包含 checkstyle:check
```

### 3.2 Gradle 配置

```groovy
plugins {
    id 'java'
    id 'checkstyle'
}

checkstyle {
    toolVersion = '10.13.0'
    configFile = file('config/checkstyle/checkstyle.xml')
    configDirectory = file('config/checkstyle')
    maxErrors = 0
    maxWarnings = 0
    ignoreFailures = false
    showViolations = true
}

tasks.withType(Checkstyle) {
    reports {
        xml.required = true
        html.required = true
    }
}

// 配置 sourceSets
checkstyleMain {
    source = 'src/main/java'
    excludes = ['**/generated/**']
}
checkstyleTest {
    source = 'src/test/java'
}
```

### 3.3 配置参数详解

| Maven 参数 | Gradle 对应 | 默认值 | 说明 |
|-----------|-------------|--------|------|
| `configLocation` | `configFile` | `sun_checks.xml` | 规则配置文件路径 |
| `maxAllowedViolations` | `maxErrors` | 0 | 允许的最大违规数 |
| `violationSeverity` | - | `error` | 违规级别阈值 |
| `excludes` | `exclude` | 空 | 排除的文件模式 |
| `suppressionsLocation` | - | 空 | 抑制配置文件路径 |
| `charset` | - | UTF-8 | 文件编码 |
| `format` | `reports` | xml | 输出格式 |

---

## 4. checkstyle.xml 配置文件结构

### 4.1 基本结构

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">

<module name="Checker">
    <!-- 文件级别的检查 -->
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="warning"/>
    <property name="fileExtensions" value="java"/>

    <!-- 文件长度检查 -->
    <module name="FileLength">
        <property name="max" value="2000"/>
    </module>

    <!-- 文件 tab 字符检查 -->
    <module name="FileTabCharacter"/>

    <!-- TreeWalker：类/方法级别的检查 -->
    <module name="TreeWalker">
        <!-- 导入检查 -->
        <module name="AvoidStarImport"/>
        <module name="UnusedImports"/>

        <!-- 命名检查 -->
        <module name="ConstantName"/>
        <module name="LocalFinalVariableName"/>
        <module name="LocalVariableName"/>
        <module name="MemberName"/>
        <module name="MethodName"/>
        <module name="PackageName"/>
        <module name="ParameterName"/>
        <module name="StaticVariableName"/>
        <module name="TypeName"/>

        <!-- 大小检查 -->
        <module name="MethodLength"/>
        <module name="ParameterNumber"/>

        <!-- 空格检查 -->
        <module name="WhitespaceAround"/>
        <module name="NoWhitespaceBefore"/>
        <module name="WhitespaceAfter"/>

        <!-- Javadoc 检查 -->
        <module name="JavadocMethod"/>
        <module name="JavadocType"/>
        <module name="JavadocVariable"/>

        <!-- 编码问题 -->
        <module name="HiddenField"/>
        <module name="MagicNumber"/>

        <!-- 设计 -->
        <module name="DesignForExtension"/>
        <module name="FinalClass"/>
        <module name="HideUtilityClassConstructor"/>
        <module name="VisibilityModifier"/>
    </module>
</module>
```

### 4.2 配置结构层次

```
Checker（全局配置）
├── 文件范围检查
│   ├── FileLength
│   ├── FileTabCharacter
│   ├── NewlineAtEndOfFile
│   └── RegexpHeader（文件头检查）
│
├── TreeWalker（语法树检查器）
│   ├── 命名规范
│   ├── 导入管理
│   ├── 代码大小
│   ├── 空白与格式
│   ├── Javadoc
│   ├── 编码问题
│   └── 设计问题
│
├── Suppressions（抑制配置）
└── Filters（过滤配置）
```

### 4.3 Google Java Style 配置文件下载

Checkstyle 官方提供了可直接使用的 Google Java Style 配置：

```bash
# 从 Maven 中央仓库下载
# google_checks.xml 已包含在 checkstyle jar 中

# 或直接使用 URL
# https://raw.githubusercontent.com/checkstyle/checkstyle/master/src/main/resources/google_checks.xml
```

```xml
<!-- 直接使用内置的 Google Style -->
<configuration>
    <configLocation>google_checks.xml</configLocation>
</configuration>
```

> 💡 推荐以 `google_checks.xml` 为基础，在此基础上做少量团队定制，而不是从零编写所有配置。

---

## 5. 核心检查项详解与代码示例

### 5.1 FileLength（文件长度）

**描述**：检查源文件的总行数，避免文件过长。

**类别**：文件检查（Checker 级别）

```java
// ❌ FileLength: 文件超过 2000 行
// ... 大量代码 ...

// 修正方案：拆分为多个类
```

### 5.2 LineLength（行长度）

**描述**：检查单行字符数，避免水平滚动。

**类别**：TreeWalker

```java
public class LineLengthExample {

    // ❌ LineLength: 第 5 行超过 100 字符
    public Result processOrderAndGenerateInvoice(
            Order order, Customer customer, Address shippingAddress,
            PaymentMethod paymentMethod, DiscountCalculator discountCalc) {
        // ✅ 拆分为多行
    }

    // ❌ 太长的方法链
    public void badStyle() {
        String result = orderService.findOrder(orderId).map(Order::getCustomer)
            .flatMap(customerService::getCustomerProfile).orElseThrow(() -> new NotFoundException("Order not found"));
        // 应换行
    }
}
```

**配置**：

```xml
<module name="LineLength">
    <property name="max" value="100"/>
    <property name="ignorePattern"
        value="^package.*|^import.*|a href|href|http://|https://|ftp://"/>
</module>
```

### 5.3 MethodLength（方法长度）

**描述**：检查方法体的行数，过长方法通常职责过多。

**类别**：大小检查

```java
public class OrderManager {

    // ❌ MethodLength: 150 行的方法，远超阈值
    public void processOrder(Order order) {
        // 第 1-10 行：参数校验
        if (order == null) throw new IllegalArgumentException();
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalStateException("Order has no items");
        }

        // 第 11-50 行：计算价格
        // ...

        // 第 51-80 行：检查库存
        // ...

        // 第 81-100 行：生成发票
        // ...

        // 第 101-120 行：发送通知
        // ...

        // 第 121-150 行：保存记录
        // ...
    }
}
```

**修正方案**：

```java
public class OrderManager {

    // ✅ 拆分为多个方法，每个方法不超过 30 行
    public void processOrder(Order order) {
        validateOrder(order);
        BigDecimal total = calculateTotal(order);
        checkInventory(order);
        Invoice invoice = generateInvoice(order, total);
        sendNotification(order, invoice);
        saveOrder(order, invoice);
    }

    private void validateOrder(Order order) { /* ... */ }
    private BigDecimal calculateTotal(Order order) { /* ... */ }
    private void checkInventory(Order order) { /* ... */ }
    private Invoice generateInvoice(Order order, BigDecimal total) { /* ... */ }
    private void sendNotification(Order order, Invoice invoice) { /* ... */ }
    private void saveOrder(Order order, Invoice invoice) { /* ... */ }
}
```

**配置**：

```xml
<module name="MethodLength">
    <property name="max" value="30"/>        <!-- 最大行数 -->
    <property name="countEmpty" value="false"/> <!-- 不计空行 -->
</module>
```

### 5.4 WhitespaceAround（环绕空格）

**描述**：检查二元运算符、关键字两侧是否有空格。属于最常触发的规则之一。

**类别**：空白检查

```java
public class WhitespaceExample {

    // ❌ WhitespaceAround: 运算符两侧缺少空格
    public void badStyle() {
        int x=1+2;                 // 应为 `int x = 1 + 2;`
        if(x>0) {                  // 应为 `if (x > 0) {`
            System.out.println(">"+x);  // 应为 `"> " + x`
        }

        List<String> list=new ArrayList<>();  // generic 运算符缺少空格
    }

    // ✅ 正确风格
    public void goodStyle() {
        int x = 1 + 2;
        if (x > 0) {
            System.out.println("> " + x);
        }

        List<String> list = new ArrayList<>();
        List<String> list2 = new ArrayList<>();  // 花括号前无空格（Java 惯例）
    }
}
```

### 5.5 NoWhitespaceBefore（前置空格禁止）

**描述**：某些 token 之前不应有空格。

```java
public class NoWhitespaceBeforeExample {

    // ❌ NoWhitespaceBefore: 分号前不应有空格
    public void badStyle() {
        for (int i = 0 ; i < 10 ; i++) {  // 分号前空格
            // ...
        }
    }

    // ✅ correct
    public void goodStyle() {
        for (int i = 0; i < 10; i++) {
            // ...
        }
    }
}
```

### 5.6 命名规范检查

**ConstantName**（常量命名）：

```java
public class NamingConventions {

    // ❌ ConstantName: 常量应全大写
    public static final int maxRetryCount = 3;       // 应为 MAX_RETRY_COUNT
    public static final String default_name = "default"; // 应全大写+下划线

    // ✅
    public static final int MAX_RETRY_COUNT = 3;
    public static final String DEFAULT_NAME = "default";

    // ❌ LocalVariableName: 局部变量应小驼峰
    public void process() {
        int MAX_VALUE = 100;  // 应为 maxValue
        String user_name = "test";  // 应为 userName
    }

    // ✅
    public void processCorrect() {
        int maxValue = 100;
        String userName = "test";
    }
}
```

**MethodName**（方法命名）：

```java
public class MethodNamingExample {

    // ❌ MethodName: 方法名应小驼峰
    public void Process_Order() { }           // ❌
    public void get_User_Info() { }           // ❌
    private void DoSomething() { }            // ❌

    // ✅
    public void processOrder() { }            // ✅
    public void getUserInfo() { }             // ✅
    private void doSomething() { }            // ✅
}
```

**TypeName**（类名）：

```java
// ❌ TypeName: 类名应大驼峰
class userService { }          // ❌ 应为 UserService
class order_processor { }      // ❌ 应为 OrderProcessor

// ✅
class UserService { }          // ✅
class OrderProcessor { }       // ✅
```

**配置示例**：

```xml
<module name="ConstantName">
    <property name="format"
        value="^log(ger)?|[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$"/>
</module>
<module name="LocalVariableName">
    <property name="format" value="^[a-z][a-zA-Z0-9]*$"/>
    <property name="allowOneCharVarInForLoop" value="true"/>
</module>
<module name="MethodName">
    <property name="format" value="^[a-z][a-zA-Z0-9]*$"/>
</module>
<module name="TypeName">
    <property name="format" value="^[A-Z][a-zA-Z0-9]*$"/>
</module>
```

### 5.7 Import 检查

**AvoidStarImport**（禁止通配符导入）：

```java
// ❌ AvoidStarImport
import java.util.*;              // 应具体导入
import java.io.*;                // 应具体导入

// ✅
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
```

**UnusedImports**（未使用的导入）：

```java
package com.example;

import java.util.List;            // ✅ 使用
import java.util.ArrayList;       // ✅ 使用
import java.util.Date;            // ❌ UnusedImports: 未使用
import java.sql.Connection;       // ❌ UnusedImports: 未使用
import java.io.Serializable;      // ❌ 未实现接口

public class UserService {
    private List<String> names = new ArrayList<>();
}
```

### 5.8 Javadoc 检查

**JavadocMethod**（方法 Javadoc）：

```java
public class UserController {

    // ❌ MissingJavadocMethod (若配置): 公有方法缺少 Javadoc
    public User getUser(Long id) {
        return userService.findById(id);
    }

    /**
     * 根据用户 ID 获取用户信息。
     *
     * @param id 用户 ID，不能为 null
     * @return 用户对象，未找到时返回 null
     * @throws IllegalArgumentException 如果 id 为 null
     */
    // ✅ 完整 Javadoc
    public User findUserById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return userService.findById(id);
    }
}
```

**JavadocType**（类 Javadoc）：

```java
// ❌ MissingJavadocType
public class OrderService {
    // ...
}

/**
 * 订单服务，负责订单的创建、查询和状态管理。
 *
 * @author dev-team
 * @since 1.0.0
 */
// ✅ 完整类注释
public class OrderProcessingService {
    // ...
}
```

### 5.9 Coding 检查

**HiddenField**（字段隐藏）：

```java
public class User {

    private String name;
    private String email;

    // ❌ HiddenField: 参数 name 隐藏了成员变量
    public User(String name, String email) {
        name = name;     // 实际是参数自赋值，成员变量未更新
        email = email;   // 同上
    }

    public void setName(String name) {
        name = name;     // ❌ HiddenField: 参数隐藏了字段
    }
}
```

**修正方案**：

```java
    // ✅ 使用 this 关键字区分
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;  // ✅
    }
```

**MagicNumber**（魔术数字）：

```java
public class DiscountCalculator {

    // ❌ MagicNumber: 使用字面量数字
    public double calculate(double price) {
        if (price > 1000) {          // 1000 是什么？
            return price * 0.9;      // 0.9 是什么？
        }
        if (price > 500) {
            return price * 0.95;     // 0.95 是什么？
        }
        return price;
    }
}
```

**修正方案**：

```java
public class DiscountCalculator {

    // ✅ 使用命名常量
    private static final double PREMIUM_THRESHOLD = 1000.0;
    private static final double STANDARD_THRESHOLD = 500.0;
    private static final double PREMIUM_DISCOUNT = 0.9;
    private static final double STANDARD_DISCOUNT = 0.95;

    public double calculate(double price) {
        if (price > PREMIUM_THRESHOLD) {
            return price * PREMIUM_DISCOUNT;
        }
        if (price > STANDARD_THRESHOLD) {
            return price * STANDARD_DISCOUNT;
        }
        return price;
    }
}
```

> 💡 并非所有数字都是魔术数字。`0`、`1`、`-1` 在初始化场景中是允许的。可以通过 `ignoreNumbers` 属性配置。

```xml
<module name="MagicNumber">
    <property name="ignoreNumbers" value="0, 1, -1, 100"/>
    <property name="ignoreAnnotation" value="true"/>
</module>
```

**MissingSwitchDefault**（switch 缺失 default）：

```java
public class StatusHandler {

    // ❌ MissingSwitchDefault: switch 没有 default 分支
    public String handleStatus(String status) {
        switch (status) {
            case "ACTIVE":
                return "activated";
            case "INACTIVE":
                return "deactivated";
            // 缺少 default 分支
        }
        return "unknown";
    }
}
```

**修正方案**：

```java
    // ✅
    public String handleStatus(String status) {
        switch (status) {
            case "ACTIVE":
                return "activated";
            case "INACTIVE":
                return "deactivated";
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
        }
    }
```

### 5.10 Design 检查

**DesignForExtension**（设计为扩展）：

```java
// ❌ DesignForExtension: 非 final/abstract 的公有/保护方法
// 暗示该方法设计为可被重写，但未提供文档说明如何重写
public class BaseService {

    public void save(Entity entity) {
        // 非 final 非 abstract 的公有方法
        validate(entity);
        persist(entity);
    }

    protected void validate(Entity entity) {
        // 子类可重写
    }
}
```

**修正方案**：

```java
// ✅ 方案一：声明为 final，禁止重写
public class BaseService {
    public final void save(Entity entity) {
        validate(entity);
        persist(entity);
    }
}

// ✅ 方案二：抽象方法 + 文档说明重写契约
public abstract class BaseService {
    public final void save(Entity entity) {
        Entity validated = validate(entity);
        persist(validated);
    }

    /**
     * 子类需实现的校验逻辑。
     * 返回校验后的实体，不应对原始实体做修改。
     */
    protected abstract Entity validate(Entity entity);
}
```

**FinalLocalVariable**（局部变量声明为 final）：

```java
public class FinalVariableExample {

    public void process(Order order) {
        // ❌ FinalLocalVariable (如果启用): 局部变量不可变但未声明 final
        Long orderId = order.getId();
        Customer customer = order.getCustomer();
        BigDecimal total = calculateTotal(order);
    }
}
```

```java
    // ✅ 声明为 final，明确不可变性
    public void process(Order order) {
        final Long orderId = order.getId();
        final Customer customer = order.getCustomer();
        final BigDecimal total = calculateTotal(order);
    }
```

### 5.11 检查项速查表

| 检查项 | 类别 | 描述 | 默认严重度 |
|--------|------|------|-----------|
| `FileLength` | 文件大小 | 文件行数上限 | warning |
| `LineLength` | 大小 | 单行字符上限 | warning |
| `MethodLength` | 大小 | 方法行数上限 | warning |
| `ParameterNumber` | 大小 | 参数个数上限 | warning |
| `WhitespaceAround` | 空白 | 运算符/关键字两侧空格 | error |
| `NoWhitespaceBefore` | 空白 | 禁止前置空格 | error |
| `WhitespaceAfter` | 空白 | 逗号/分号后空格 | error |
| `ConstantName` | 命名 | 常量全大写+下划线 | error |
| `MethodName` | 命名 | 方法小驼峰 | error |
| `TypeName` | 命名 | 类名大驼峰 | error |
| `PackageName` | 命名 | 包名全小写 | error |
| `AvoidStarImport` | 导入 | 禁止通配符导入 | info |
| `UnusedImports` | 导入 | 未使用的导入 | warning |
| `JavadocMethod` | Javadoc | 方法需 Javadoc | info |
| `JavadocType` | Javadoc | 类需 Javadoc | info |
| `HiddenField` | 编码 | 参数隐藏字段 | warning |
| `MagicNumber` | 编码 | 禁止魔术数字 | warning |
| `MissingSwitchDefault` | 编码 | switch 需 default | warning |
| `DesignForExtension` | 设计 | 扩展性设计合规 | warning |
| `FinalLocalVariable` | 设计 | 局部变量 final | info |
| `VisibilityModifier` | 设计 | 成员变量访问控制 | error |

---

## 6. Suppressions：抑制误报

### 6.1 `@SuppressWarnings("checkstyle:xxx")` 注解

从 Java 9 + Checkstyle 8.x 起支持：

```java
public class UserService {

    // 抑制特定检查
    @SuppressWarnings("checkstyle:magicnumber")
    public static final double PI_APPROX = 3.14;

    // 抑制多个检查
    @SuppressWarnings({"checkstyle:hiddenfield", "checkstyle:magicnumber"})
    public UserService(String name, int port) {
        this.name = name;
        this.port = port;
    }

    // 抑制所有 checkstyle 检查
    @SuppressWarnings("checkstyle")
    public void legacyMethod() {
        // 兼容代码
    }
}
```

### 6.2 suppressions.xml 文件

```xml
<?xml version="1.0"?>
<!DOCTYPE suppressions PUBLIC
    "-//Checkstyle//DTD SuppressionFilter Configuration 1.2//EN"
    "https://checkstyle.org/dtds/suppressions_1_2.dtd">

<suppressions>
    <!-- 排除生成的代码 -->
    <suppress files="[/\\]generated[/\\]" checks=".*"/>

    <!-- 排除测试代码的 magic number -->
    <suppress
        files="[/\\]test[/\\].*"
        checks="MagicNumber" />

    <!-- 排除特定文件的特定检查 -->
    <suppress
        files="UserController.java"
        checks="MissingJavadocMethod"
        message="公有方法需 Javadoc" />

    <!-- 按行号排除 -->
    <suppress
        files="ConfigConstants.java"
        checks="MagicNumber"
        lines="15-30" />

    <!-- 排除特定检查在所有文件上 -->
    <suppress
        checks="DesignForExtension"
        message="设计为扩展" />
</suppressions>
```

### 6.3 配置 suppressions

```xml
<module name="SuppressionFilter">
    <property name="file" value="config/checkstyle/suppressions.xml"/>
    <property name="optional" value="false"/>
</module>
```

Maven 配置：

```xml
<configuration>
    <suppressionsLocation>config/checkstyle/suppressions.xml</suppressionsLocation>
</configuration>
```

### 6.4 行内注释抑制（Checkstyle 7.x 版本）

```java
public class LegacyCode {

    // CHECKSTYLE:OFF - 以下代码块跳过检查
    @Override
    public void legacyApi() {
        // ... 不需要检查的老代码
    }
    // CHECKSTYLE:ON
}
```

需要启用 `SuppressWithPlainTextCommentFilter`：

```xml
<module name="SuppressWithPlainTextCommentFilter">
    <property name="offCommentFormat" value="CHECKSTYLE:OFF"/>
    <property name="onCommentFormat" value="CHECKSTYLE:ON"/>
</module>
```

> ⚠️ 注释抑制应谨慎使用，优先使用 `@SuppressWarnings` 注解或 `suppressions.xml`。注释抑制没有类型安全检查，更容易出错。

---

## 7. 自定义 Checks 开发简介

### 7.1 自定义 Check 的基本步骤

开发一个自定义 Check 需要：

```
1. 继承 AbstractCheck 或特定 Check 基类
2. 实现 getDefaultTokens() / getAcceptableTokens()
3. 在 visitToken() 方法中实现检查逻辑
4. 通过 log() 方法报告违规
5. 编译并打包
6. 在 checkstyle.xml 中引用
```

### 7.2 示例：禁止使用 System.out 的 Check

```java
package com.example.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * 自定义 Check：禁止使用 System.out.println 和 System.err.println。
 */
public class NoSystemOutCheck extends AbstractCheck {

    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.METHOD_CALL};
    }

    @Override
    public int[] getAcceptableTokens() {
        return getDefaultTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return getDefaultTokens();
    }

    @Override
    public void visitToken(DetailAST ast) {
        // 查找方法名
        DetailAST methodName = ast.findFirstToken(TokenTypes.IDENT);
        if (methodName == null) return;

        String name = methodName.getText();
        if (!"println".equals(name) && !"print".equals(name) && !"printf".equals(name)) {
            return;
        }

        // 查找调用者是否是 System.out 或 System.err
        DetailAST dot = ast.getFirstChild().getFirstChild();
        if (dot == null || dot.getType() != TokenTypes.DOT) return;

        DetailAST object = dot.getFirstChild();
        DetailAST field = dot.getLastChild();

        if (object == null || field == null) return;

        String objectText = object.getText();
        String fieldText = field.getText();

        if ("System".equals(objectText)
                && ("out".equals(fieldText) || "err".equals(fieldText))) {
            log(ast, "禁止使用 System.out/err，请使用 SLF4J 日志框架");
        }
    }
}
```

### 7.3 打包与使用

```xml
<!-- 自定义 Check 的 pom.xml -->
<project>
    <dependencies>
        <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>10.13.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

```bash
# 打包
mvn clean package

# 安装到本地仓库
mvn install
```

```xml
<!-- 在使用方项目中引用自定义 Check -->
<module name="Checker">
    <module name="TreeWalker">
        <!-- 引入自定义 Check -->
        <module name="com.example.checkstyle.checks.NoSystemOutCheck"/>
    </module>
</module>
```

```xml
<!-- Maven 插件中引入自定义 Check 的依赖 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>custom-checkstyle-checks</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</plugin>
```

---

## 8. IDE 集成（IntelliJ CheckStyle-IDEA）

### 8.1 安装插件

```
IntelliJ IDEA → Settings → Plugins
搜索 "CheckStyle-IDEA" → Install → Restart
```

### 8.2 配置插件

```
IntelliJ IDEA → Settings → Tools → Checkstyle
├── Checkstyle Version: 10.x
├── Scan Scope: Java source (including tests)
└── Configuration File:
    ├── + (添加自定义 checkstyle.xml)
    │   ├── Description: 项目规则
    │   └── File: config/checkstyle/checkstyle.xml
    └── Active: 项目规则
```

### 8.3 使用方式

| 操作 | 快捷键/方式 | 说明 |
|------|-----------|------|
| 检查当前文件 | `Alt + Shift + C` | 仅检查当前打开的文件 |
| 检查整个模块 | 右键模块 → Checkstyle → Check Module | 检查整个模块 |
| 实时检查 | 勾选 "Scanning scope" 中的 "Editor" | 输入时即时高亮 |
| 查看结果 | Checkstyle Tool Window | 底部分页栏 |
| 快速跳转 | 双击违规项 | 跳转到对应代码行 |
| 抑制警告 | 右键违规行 → Suppress | 自动生成注解 |

### 8.4 与 IntelliJ Formatter 联动

IntelliJ 可以导入 Checkstyle 配置，使代码格式化器自动满足 Checkstyle 要求：

```
Settings → Editor → Code Style → Java
├── Scheme: Project
└── ⚙ (齿轮图标) → Import Scheme → Checkstyle Configuration
    └── 选择 checkstyle.xml
```

这样 `Ctrl + Alt + L`（格式化代码）后就能通过大部分的 Checkstyle 检查。

> 💡 将 Checkstyle 配置文件与 IntelliJ Code Style 联动是最佳实践。开发者在编写代码时 `Ctrl+Alt+L` 格式化，即可满足大部分风格检查，减少 CI 失败。

---

## 9. Google Java Format vs Checkstyle 自动修复

### 9.1 Google Java Format

Google Java Format 是一个 **代码格式化工具**，能自动将代码重排为 Google Java Style。

| 属性 | 说明 |
|------|------|
| 工具 | `google-java-format` |
| 作用 | 自动格式化代码（格式化器） |
| 行为 | 重写 .java 文件 |
| 配置 | 几乎没有可配置项（采用 Google 标准不可定制） |
| 插件 | IntelliJ / Eclipse / Maven / Gradle |

### 9.2 Checkstyle

| 属性 | 说明 |
|------|------|
| 工具 | Checkstyle |
| 作用 | 检查代码格式（检查器） |
| 行为 | 只报告违规，不自动修复 |
| 配置 | 高度可定制的配置 |
| 插件 | IntelliJ / Eclipse / Maven / Gradle |

### 9.3 Checkstyle 与 Google Java Format 的关系

```
                    ┌──────────────────┐
                    │   编写代码        │
                    └────────┬─────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │  Google Java Format           │
              │  自动格式化（一键修复格式）    │ ← 格式化器
              └────────────┬─────────────────┘
                           │
                           ▼
              ┌──────────────────────────────┐
              │  Checkstyle                  │
              │  检查剩余违规（命名/注释等）   │ ← 检查器
              └────────────┬─────────────────┘
                           │
                           ▼
              ┌──────────────────────────────┐
              │  CI 门禁                     │
              │  格式化通过 + 检查通过 → 绿   │
              └──────────────────────────────┘
```

### 9.4 推荐组合策略

| 工具 | 时机 | 作用 |
|------|------|------|
| IntelliJ Formatter + Checkstyle 配置 | 开发阶段 `Ctrl+Alt+L` | 编写时即时格式化 |
| Google Java Format | Pre-commit 钩子 | 确保提交前格式化 |
| Checkstyle | CI verify 阶段 | 检查未通过的风格问题 |
| Spotless Maven Plugin | Maven 生命周期 | 自动格式化 + 检查 |

### 9.5 Spotless Maven 插件（自动修复）

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.43.0</version>
    <configuration>
        <java>
            <!-- 使用 Google Java Format 自动格式化 -->
            <googleJavaFormat>
                <version>1.19.1</version>
                <style>GOOGLE</style>
            </googleJavaFormat>
            <!-- 覆盖导入顺序 -->
            <importOrder>
                <order>java|javax|org|com</order>
            </importOrder>
            <!-- 移除多余的空格 -->
            <trimTrailingWhitespace/>
            <endWithNewline/>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>    <!-- 检查格式 -->
                <goal>apply</goal>    <!-- 自动修复 -->
            </goals>
        </execution>
    </executions>
</plugin>
```

```bash
mvn spotless:check    # 检查格式违规
mvn spotless:apply    # 自动修复格式
```

> 🎯 **最佳实践**：用 Google Java Format（Spotless）做**自动格式化**，用 Checkstyle 做**风格检查（超越格式的命名/注释/设计）**。两者解决不同的问题，互补使用。

---

## 10. CI/CD 集成与失败控制

### 10.1 GitHub Actions 集成

```yaml
name: Checkstyle CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  checkstyle:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run Checkstyle
        run: mvn clean compile checkstyle:check

      - name: Upload Checkstyle Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: checkstyle-report
          path: target/checkstyle/
```

### 10.2 Jenkins Pipeline 集成

```groovy
pipeline {
    agent any
    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('Checkstyle') {
            steps {
                sh 'mvn checkstyle:check'
            }
            post {
                always {
                    // 发布 Checkstyle 报告
                    checkstyle(
                        canComputeNew: false,
                        canResolveRelativePaths: false,
                        defaultEncoding: '',
                        healthy: '',
                        pattern: 'target/checkstyle/checkstyle-result.xml',
                        unstable: ''
                    )
                }
                failure {
                    echo "Checkstyle 检测到代码风格违规，请检查！"
                }
            }
        }
    }
}
```

### 10.3 违规计数控制

```xml
<configuration>
    <!-- 最多允许 10 个 warning，0 个 error -->
    <maxAllowedViolations>10</maxAllowedViolations>
    <violationSeverity>warning</violationSeverity>
</configuration>
```

### 10.4 Maven 项目多模块配置

```xml
<!-- 父 pom.xml -->
<pluginManagement>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-checkstyle-plugin</artifactId>
            <version>3.3.1</version>
            <configuration>
                <configLocation>${project.parent.basedir}/config/checkstyle/checkstyle.xml</configLocation>
            </configuration>
            <executions>
                <execution>
                    <id>checkstyle-check</id>
                    <goals><goal>check</goal></goals>
                    <phase>verify</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</pluginManagement>

<!-- 子模块继承父 pom 的配置即可，无需重复 -->
```

---

## 11. 完整实战：SpringBoot 项目 Google Java Style 强制

### 11.1 项目结构

```
springboot-checkstyle-demo/
├── config/checkstyle/
│   ├── checkstyle.xml          # Google Java Style + 自定义
│   └── suppressions.xml        # 抑制配置
├── pom.xml
├── src/main/java/com/example/
│   ├── Application.java
│   ├── controller/UserController.java
│   └── service/UserService.java
└── src/test/java/
```

### 11.2 checkstyle.xml

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">

<!--
    基于 Google Java Style 的团队自定义配置
    Google Java Style: https://google.github.io/styleguide/javaguide.html
-->
<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="warning"/>
    <property name="fileExtensions" value="java"/>

    <!-- 文件头（可选） -->
    <module name="RegexpHeader">
        <property name="header"
            value="^/\*\*$\n^ \* Copyright \(c\) \d{4} Company\.$\n^ \* All rights reserved\.$\n^ \*/$"/>
        <property name="multiLines" value="5"/>
    </module>

    <!-- 文件长度 -->
    <module name="FileLength">
        <property name="max" value="1000"/>
    </module>

    <!-- 不允许 tab -->
    <module name="FileTabCharacter"/>

    <!-- 抑制 -->
    <module name="SuppressionFilter">
        <property name="file" value="config/checkstyle/suppressions.xml"/>
        <property name="optional" value="true"/>
    </module>

    <!-- ============================================ -->
    <!-- TreeWalker                                  -->
    <!-- ============================================ -->
    <module name="TreeWalker">

        <!-- --- 命名规范 --- -->
        <module name="ConstantName"/>
        <module name="LocalFinalVariableName"/>
        <module name="LocalVariableName">
            <property name="allowOneCharVarInForLoop" value="true"/>
        </module>
        <module name="MemberName"/>
        <module name="MethodName">
            <property name="format" value="^[a-z][a-zA-Z0-9]*$"/>
        </module>
        <module name="PackageName"/>
        <module name="ParameterName"/>
        <module name="StaticVariableName"/>
        <module name="TypeName"/>

        <!-- --- 导入管理 --- -->
        <module name="AvoidStarImport"/>
        <module name="UnusedImports"/>
        <module name="RedundantImport"/>
        <module name="ImportOrder">
            <property name="groups" value="java,javax,org,com"/>
            <property name="separated" value="true"/>
            <property name="option" value="top"/>
        </module>

        <!-- --- 代码大小 --- -->
        <module name="MethodLength">
            <property name="max" value="30"/>
        </module>
        <module name="ParameterNumber">
            <property name="max" value="5"/>
        </module>
        <module name="LineLength">
            <property name="max" value="100"/>
        </module>

        <!-- --- 空白 --- -->
        <module name="WhitespaceAround"/>
        <module name="NoWhitespaceBefore"/>
        <module name="WhitespaceAfter"/>
        <module name="GenericWhitespace"/>
        <module name="MethodParamPad"/>
        <module name="ParenPad"/>
        <module name="TypecastParenPad"/>
        <module name="NoWhitespaceAfter"/>

        <!-- --- Javadoc --- -->
        <module name="JavadocMethod">
            <property name="accessModifiers" value="public"/>
            <property name="allowMissingParamTags" value="false"/>
            <property name="allowMissingReturnTag" value="false"/>
        </module>
        <module name="JavadocType">
            <property name="scope" value="public"/>
        </module>
        <module name="JavadocStyle">
            <property name="checkFirstSentence" value="false"/>
        </module>
        <property name="summaryJavaDoc" value="required"/>

        <!-- --- 编码 --- -->
        <module name="HiddenField"/>
        <module name="MagicNumber">
            <property name="ignoreNumbers" value="0,1,-1,100"/>
            <property name="ignoreAnnotation" value="true"/>
        </module>
        <module name="MissingSwitchDefault"/>
        <module name="DefaultComesLast"/>
        <module name="FallThrough"/>
        <module name="EmptyStatement"/>
        <module name="EqualsHashCode"/>
        <module name="StringLiteralEquality"/>
        <module name="SimplifyBooleanExpression"/>
        <module name="SimplifyBooleanReturn"/>

        <!-- --- 设计 --- -->
        <module name="DesignForExtension"/>
        <module name="FinalClass"/>
        <module name="HideUtilityClassConstructor"/>
        <module name="VisibilityModifier"/>
        <module name="InterfaceIsType"/>

        <!-- --- 修饰符 --- -->
        <module name="ModifierOrder"/>
        <module name="RedundantModifier"/>

        <!-- --- 其他 --- -->
        <module name="ArrayTypeStyle"/>
        <module name="UpperEll"/>
        <module name="OuterTypeFilename"/>
        <module name="OneTopLevelClass"/>
        <module name="UpperEll"/>

    </module>
</module>
```

### 11.3 suppressions.xml

```xml
<?xml version="1.0"?>
<!DOCTYPE suppressions PUBLIC
    "-//Checkstyle//DTD SuppressionFilter Configuration 1.2//EN"
    "https://checkstyle.org/dtds/suppressions_1_2.dtd">

<suppressions>
    <!-- 测试类放宽要求 -->
    <suppress files="[/\\]test[/\\]" checks="MagicNumber|JavadocMethod|JavadocType"/>
    <suppress files="[/\\]test[/\\]" checks="MethodLength" message="方法超过 30 行"/>

    <!-- 配置类允许魔术数字 -->
    <suppress files=".*Config\.java" checks="MagicNumber"/>

    <!-- 生成的代码跳过 -->
    <suppress files="[/\\]generated[/\\]" checks=".*"/>

    <!-- 常量接口中允许常量命名模式 -->
    <suppress files=".*Constants\.java" checks="ConstantName"/>
</suppressions>
```

### 11.4 pom.xml

```xml
<project>
    <build>
        <plugins>
            <!-- Checkstyle -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-checkstyle-plugin</artifactId>
                <version>3.3.1</version>
                <configuration>
                    <configLocation>config/checkstyle/checkstyle.xml</configLocation>
                    <suppressionsLocation>config/checkstyle/suppressions.xml</suppressionsLocation>
                    <maxAllowedViolations>0</maxAllowedViolations>
                    <violationSeverity>warning</violationSeverity>
                    <excludes>**/generated/**/*.java</excludes>
                    <encoding>UTF-8</encoding>
                </configuration>
                <executions>
                    <execution>
                        <id>checkstyle-verify</id>
                        <goals><goal>check</goal></goals>
                        <phase>verify</phase>
                    </execution>
                </executions>
            </plugin>

            <!-- Google Java Format (自动格式化) -->
            <plugin>
                <groupId>com.diffplug.spotless</groupId>
                <artifactId>spotless-maven-plugin</artifactId>
                <version>2.43.0</version>
                <configuration>
                    <java>
                        <googleJavaFormat>
                            <version>1.19.1</version>
                            <style>GOOGLE</style>
                        </googleJavaFormat>
                        <importOrder>
                            <order>java|javax|org|com</order>
                        </importOrder>
                        <trimTrailingWhitespace/>
                        <endWithNewline/>
                    </java>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>check</goal>
                        </goals>
                        <phase>verify</phase>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 11.5 故意包含违规的示例代码

```java
package com.example.controller;

import java.util.*;
import java.io.*;

// ❌ MissingJavadocType
public class UserController {
// ❌ TypeName: 类名应大驼峰（此处故意正确）
    private String name_;
    // ❌ MemberName: 不应以下划线结尾

    // ❌ LineLength: 超长行
    private static final String VERY_LONG_CONSTANT_NAME = "这是非常长的字符串，超过 100 个字符的限制，Checkstyle 应该能检测到并报告违规问题";

    // ❌ MissingJavadocMethod
    public User getUser(Long user_id) {  // ❌ ParameterName: 下划线命名
        return userService.findById(user_id);
    }

    public void process(String name, String email, String phone, String address, String zipCode) {
    // ❌ ParameterNumber: 参数超过 5 个

        // ❌ UnusedImports: java.io.* 未使用
        // ❌ AvoidStarImport: import java.util.* 通配符导入

        // ❌ MagicNumber
        double price = 3.14159 * 2;

        // ❌ WhitespaceAround
        int x=1+2;

        // ❌ HiddenField
        name_=name;
    }

    // ❌ MagicNumber
    public double calculate(double value) {
        return value > 1000 ? value * 0.9 : value * 0.95;
    }
}
```

### 11.6 运行与修复流程

```bash
# 1. 自动格式化（修复 80% 的格式问题）
mvn spotless:apply

# 2. 运行 Checkstyle
mvn checkstyle:checkstyle

# 3. 查看报告
# target/checkstyle/checkstyle-result.xml
# target/checkstyle/checkstyle.html

# 4. 修复剩余违规
# - 添加 Javadoc
# - 修改变量命名
# - 提取魔术常量为命名常量

# 5. 最终验证
mvn clean verify
```

### 11.7 预期 Checkstyle 报告结果

| 违规类型 | 位置 | 说明 |
|----------|------|------|
| `MissingJavadocType` | UserController.java:1 | 类缺少 Javadoc |
| `AvoidStarImport` | UserController.java:3 | 通配符导入 `java.util.*` |
| `UnusedImports` | UserController.java:4 | 未使用的导入 `java.io.*` |
| `LineLength` | UserController.java:9 | 行超过 100 字符 |
| `MemberName` | UserController.java:8 | 成员变量以下划线结尾 |
| `ParameterName` | UserController.java:12 | 参数使用下划线 |
| `MissingJavadocMethod` | UserController.java:11 | 方法缺少 Javadoc |
| `ParameterNumber` | UserController.java:16 | 参数超过 5 个 |
| `MagicNumber` | UserController.java:21,27 | 使用魔术数字 |
| `WhitespaceAround` | UserController.java:24 | 运算符两侧缺少空格 |
| `HiddenField` | UserController.java:25 | 参数隐藏了字段 |

---

## 12. 常见问题与最佳实践

### 12.1 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| **Checkstyle 与 IDE 格式化不一致** | IDE 格式化器没有加载 Checkstyle 配置 | 导入 checkstyle.xml 到 IntelliJ Code Style |
| **使用 Lombok 导致大量违规** | Lombok 生成的代码被 Checkstyle 检查 | 在 suppressions.xml 中排除 `@Generated` 代码 |
| **行内注释抑制失效** | 未配置 `SuppressWithPlainTextCommentFilter` | 添加 Filter 配置 |
| **Maven 多模块配置重复** | 每个子模块需要相同配置 | 使用 parent pom 的 `pluginManagement` |
| **CI 和本地结果不一致** | 本地使用不同版本的插件或配置 | 锁定 checkstyle 版本，使用同一配置文件 |
| **Javadoc 检查过于严格** | 配置了过严的 Javadoc 检查项 | 调整 scope 为 `public`，或降低 severity |

### 12.2 最佳实践

| 实践 | 说明 |
|------|------|
| **从 Google Java Style 开始** | Google Style 是行业标准，不要从零开始配置 |
| **格式化器 + 检查器组合** | Spotless/Google Java Format 自动格式化 + Checkstyle 风格检查 |
| **Pre-commit 格式化钩子** | 提交前自动格式化，减少 CI 失败 |
| **CI 中强制门禁** | 设置 `maxAllowedViolations=0`，严格阻断 |
| **逐步收紧** | 遗留项目先 warning，逐步过渡到 error |
| **团队共识** | 规则配置需要团队共同决策，而非个人决定 |
| **定期评审规则** | 每季度评审一次 checkstyle.xml，移除无效规则 |

### 12.3 Lombok 兼容处理

```xml
<!-- suppressions.xml -->
<suppress files=".*" checks="HideUtilityClassConstructor"/>
<suppress files=".*" checks="MissingJavadocMethod"/>
<suppress files=".*Builder\.java" checks=".*"/>
```

```xml
<!-- checkstyle.xml 中忽略 Lombok 注解 -->
<module name="SuppressWarningsFilter"/>
<module name="TreeWalker">
    <module name="SuppressWarningsHolder"/>
    <!-- ... -->
</module>
```

### 12.4 推荐配置演进路线

```
Phase 1: 仅报告（遗留项目）
→ severity=info, no build failure
→ 让团队看到违规情况

Phase 2: warning 级别（适应期）
→ severity=warning, maxAllowedViolations=200
→ 逐步减少违规数

Phase 3: error 级别（严格期）
→ severity=error, maxAllowedViolations=0
→ CI 严格门禁，新代码必须合规

Phase 4: 自动化修复
→ Spotless apply + pre-commit hook
→ 开发者无需手动关注格式
```

### 12.5 常用命令速查

```bash
# Checkstyle 检查
mvn checkstyle:check          # 检查并阻断
mvn checkstyle:checkstyle     # 生成报告

# Spotless 自动格式化
mvn spotless:check            # 检查格式
mvn spotless:apply            # 自动修复格式

# 完整构建 + 检查
mvn clean verify

# 跳过检查（紧急场景）
mvn clean verify -Dcheckstyle.skip=true

# 使用命令行直接运行
java -jar checkstyle-10.13.0-all.jar \
    -c config/checkstyle/checkstyle.xml \
    -f xml \
    -o checkstyle-result.xml \
    src/main/java
```

---

> 🎯 Checkstyle 是代码风格自动化的基石工具。它的核心价值不在于"发现错误"，而在于**消除团队中因代码风格差异而产生的认知摩擦**。当团队统一使用 Google Java Style + Checkstyle 强制执行后，Code Review 将从"这里少了个空格"转向真正重要的逻辑评审。配合 Spotless/Google Java Format 自动格式化，可以实现"提交即合规"的流畅开发体验。
