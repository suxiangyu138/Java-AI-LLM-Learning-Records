# PMD 源码静态分析
> PMD 是一款基于源代码级的 Java 静态分析工具，通过扫描源码文件（.java）发现未使用变量、空 catch 块、复杂度过高等代码质量问题，是代码质量门禁的重要组成部分。

## 目录
1. [PMD 概述](#1-pmd-概述)
2. [PMD vs SpotBugs：源码级 vs 字节码级](#2-pmd-vs-spotbugs源码级-vs-字节码级)
3. [Maven/Gradle 插件配置](#3-mavengradle-插件配置)
4. [规则集总览](#4-规则集总览)
5. [核心规则详解与代码示例](#5-核心规则详解与代码示例)
6. [CPD 重复代码检测](#6-cpd-重复代码检测)
7. [报告阅读与阈值控制](#7-报告阅读与阈值控制)
8. [抑制 PMD 警告](#8-抑制-pmd-警告)
9. [自定义 XPath 规则](#9-自定义-xpath-规则)
10. [PMD vs Checkstyle vs SpotBugs 定位表](#10-pmd-vs-checkstyle-vs-spotbugs-定位表)
11. [最佳实践与常用配置模板](#11-最佳实践与常用配置模板)

---

## 1. PMD 概述

### 1.1 什么是 PMD？

PMD 是一个 **源代码级静态分析工具**，它解析 Java 源码生成抽象语法树（AST），然后遍历 AST 匹配预定义的规则模式，发现代码质量问题。

| 属性 | 说明 |
|------|------|
| 全称 | PMD（无官方全称，原名 Pretty Much Done） |
| 分析层次 | 源码（.java 文件） |
| 分析时机 | 编译前/编译后均可 |
| 核心技术 | JavaCC 解析器 → AST → XPath / Java 规则匹配 |
| 语言支持 | Java, JavaScript, TypeScript, PLSQL, Apex, etc. |
| 输出格式 | XML, HTML, CSV, PDF, Text, IDE 插件 |
| 许可证 | Apache 2.0 |
| 当前版本 | 7.x（2024+）|

### 1.2 设计哲学

```
PMD 的核心假设：
如果代码能写得更好（更简洁、更清晰、更安全），就应该写得更好。

它问的不是 "代码能工作吗？"
而是 "代码是否以最佳方式编写？"
```

### 1.3 适用场景

| 场景 | 用途 |
|------|------|
| **死代码检测** | 发现未使用的变量、参数、私有方法 |
| **复杂度控制** | 检测圈复杂度过高的方法、类 |
| **编码规范执行** | 强制执行命名规范、代码结构规则 |
| **潜在 Bug 检测** | 空 catch 块、错误异常处理模式 |
| **重复代码检测** | CPD 模块发现复制粘贴的重复代码 |
| **重构辅助** | 标记需要重构的代码区域 |

---

## 2. PMD vs SpotBugs：源码级 vs 字节码级

### 2.1 核心差异

| 维度 | PMD | SpotBugs |
|------|-----|----------|
| **分析对象** | 源代码（.java） | 字节码（.class） |
| **分析时机** | 编译前或编译后 | 编译后 |
| **需要编译** | 不需要（直接分析源码） | 需要（分析编译产物） |
| **检测方向** | 编码风格、复杂度、死代码 | 运行时 Bug 模式、并发问题 |
| **AST 技术** | JavaCC 解析器生成 AST | ASM 框架解析字节码 |
| **自定义规则** | 容易（XPath/Java） | 困难（需理解字节码） |
| **速度** | 快 | 中等 |
| **误报率** | 较高（风格类规则多） | 中等 |
| **典型发现** | 未用变量、过长方法、命名违规 | 空指针路径、内部暴露、编码问题 |

### 2.2 互补关系

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 代码质量防护                          │
├─────────────────────┬───────────────────────────────────────┤
│    PMD（源码级）      │      SpotBugs（字节码级）              │
│                     │                                       │
│  ✓ 未使用变量         │  ✓ 空指针路径                         │
│  ✓ 空 catch 块       │  ✓ 内部表示暴露                       │
│  ✓ 圈复杂度           │  ✓ 多线程正确性                       │
│  ✓ 命名规范           │  ✓ 序列化问题                         │
│  ✓ 代码风格           │  ✓ 未关闭资源                         │
│  ✓ 重复代码（CPD）    │  ✓ 安全漏洞模式                       │
│  ✓ 过长方法/类        │  ✓ 硬编码问题                         │
└─────────────────────┴───────────────────────────────────────┘
```

> 🎯 PMD 和 SpotBugs 是互补关系而非替代关系。**PMD 管编码规范和质量，SpotBugs 管运行时 Bug**。两者配合使用可构建全面的代码质量防护体系。

---

## 3. Maven/Gradle 插件配置

### 3.1 Maven 配置（pmd-maven-plugin）

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-pmd-plugin</artifactId>
    <version>3.21.2</version>
    <configuration>
        <!-- 目标 JDK 版本 -->
        <targetJdk>17</targetJdk>
        <!-- 是否跳过 -->
        <skip>false</skip>
        <!-- 失败阈值：发现的违规数超过此值时构建失败 -->
        <failurePriority>5</failurePriority>
        <!-- 允许的最大违规数 -->
        <maxAllowedViolations>0</maxAllowedViolations>
        <!-- 规则集文件 -->
        <rulesets>
            <ruleset>config/pmd/pmd-ruleset.xml</ruleset>
        </rulesets>
        <!-- 排除文件 -->
        <excludes>
            <exclude>**/generated/**/*.java</exclude>
        </excludes>
        <!-- 包含文件 -->
        <includes>
            <include>**/*.java</include>
        </includes>
        <!-- 格式 -->
        <format>xml</format>
        <linkXRef>false</linkXRef>
        <outputDirectory>${project.build.directory}/pmd</outputDirectory>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>   <!-- 绑定到 verify 阶段 -->
                <goal>cpd-check</goal> <!-- CPD 重复代码检查 -->
            </goals>
        </execution>
    </executions>
</plugin>
```

执行命令：

```bash
mvn pmd:pmd               # 生成 PMD 报告
mvn pmd:check             # 检查并阻断构建
mvn pmd:cpd               # 运行 CPD 重复代码检测
mvn pmd:cpd-check         # CPD 检查并阻断构建
mvn verify                # 包含 pmd:check 和 cpd-check
```

### 3.2 Gradle 配置

```groovy
plugins {
    id 'java'
    id 'pmd'
}

pmd {
    toolVersion = '7.0.0'
    ignoreFailures = false
    ruleSetFiles = files('config/pmd/pmd-ruleset.xml')
    ruleSets = []  // 清空默认规则集，使用自定义
    sourceSets = [sourceSets.main]
    reportsDir = file("${buildDir}/reports/pmd")
    maxFailures = 0
}

tasks.withType(Pmd) {
    reports {
        xml.required = true
        html.required = true
    }
}
```

### 3.3 配置参数详解

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `targetJdk` | String | 当前 JDK 版本 | 目标 JDK 版本 |
| `failurePriority` | int | 5 | 违规优先级阈值（1-5） |
| `maxAllowedViolations` | int | 0 | 允许的最大违规数 |
| `rulesets` | List | 内置规则集 | 自定义规则集路径 |
| `excludes` | List | 空 | 排除的文件模式 |
| `format` | String | xml | 输出格式（xml/html/csv/pdf/text） |
| `linkXRef` | boolean | true | 是否链接到源码交叉引用 |

---

## 4. 规则集总览

### 4.1 PMD 7.x 规则分类

| 类别 | 缩写 | 说明 | 典型规则数 |
|------|------|------|-----------|
| **Best Practices** | `category/java/bestpractices.xml` | 推荐的最佳编码实践 | 40+ |
| **Code Style** | `category/java/codestyle.xml` | 代码风格规范 | 50+ |
| **Design** | `category/java/design.xml` | 设计问题检测 | 40+ |
| **Documentation** | `category/java/documentation.xml` | 注释与文档相关 | 10+ |
| **Error Prone** | `category/java/errorprone.xml` | 易错模式检测 | 100+ |
| **Multithreading** | `category/java/multithreading.xml` | 多线程正确性 | 20+ |
| **Performance** | `category/java/performance.xml` | 性能问题检测 | 30+ |
| **Security** | `category/java/security.xml` | 安全相关漏洞 | 10+ |

### 4.2 推荐的最小规则集

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ruleset name="custom-pmd-rules"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0
         https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
    <description>自定义 PMD 规则集</description>

    <!-- 最佳实践 -->
    <rule ref="category/java/bestpractices.xml">
        <exclude name="JUnitAssertionsShouldIncludeMessage" />
    </rule>

    <!-- 错误易发模式（最关键） -->
    <rule ref="category/java/errorprone.xml">
        <exclude name="DataflowAnomalyAnalysis" />
        <exclude name="MissingSerialVersionUID" />
    </rule>

    <!-- 设计问题 -->
    <rule ref="category/java/design.xml" />

    <!-- 代码风格（选择性启用） -->
    <rule ref="category/java/codestyle.xml">
        <exclude name="LocalVariableCouldBeFinal" />
        <exclude name="MethodArgumentCouldBeFinal" />
        <exclude name="OnlyOneReturn" />
        <exclude name="AtLeastOneConstructor" />
    </rule>

    <!-- 性能 -->
    <rule ref="category/java/performance.xml" />

    <!-- 多线程 -->
    <rule ref="category/java/multithreading.xml" />

    <!-- 安全 -->
    <rule ref="category/java/security.xml" />
</ruleset>
```

---

## 5. 核心规则详解与代码示例

### 5.1 UnusedLocalVariable（未使用的局部变量）

**描述**：声明了局部变量但从未使用，属于死代码。

**类别**：Best Practices

**检测级别**：Medium

```java
public class ShoppingCart {

    public double calculateTotal(List<Item> items) {
        double total = 0.0;

        for (Item item : items) {
            total += item.getPrice() * item.getQuantity();
        }

        // ❌ UnusedLocalVariable: discountRate 声明了但从未使用
        double discountRate = 0.9;

        return total;
    }
}
```

**修正方案**：

```java
public class ShoppingCart {

    public double calculateTotal(List<Item> items) {
        double total = 0.0;

        for (Item item : items) {
            total += item.getPrice() * item.getQuantity();
        }

        // ✅ 移除未使用变量，或使用它
        return applyDiscount(total);
    }

    private double applyDiscount(double amount) {
        double discountRate = 0.9;
        return amount * discountRate;
    }
}
```

### 5.2 UnusedPrivateMethod（未使用的私有方法）

**描述**：私有的方法从未被调用，属于死代码。

**类别**：Best Practices

```java
public class OrderProcessor {

    public void process(Order order) {
        validateOrder(order);
        saveOrder(order);
    }

    private void validateOrder(Order order) {
        // 校验逻辑
    }

    // ❌ UnusedPrivateMethod: 从未被调用
    private void sendConfirmationEmail(Order order) {
        // 发送邮件
    }

    private void saveOrder(Order order) {
        // 持久化
    }
}
```

**修正方案**：

```java
// ✅ 移除未使用的私有方法，或添加上调用
```

### 5.3 CyclomaticComplexity（圈复杂度）

**描述**：方法的圈复杂度过高，难以理解和测试。圈复杂度衡量方法中独立路径的数量。

**类别**：Design

**检测级别**：Medium

| 复杂度范围 | 评价 |
|-----------|------|
| 1-10 | 简单，良好 |
| 11-20 | 较复杂，需要关注 |
| 21-50 | 非常复杂，建议重构 |
| 50+ | 不可测试，必须重构 |

```java
public class OrderService {

    // ❌ CyclomaticComplexity = 11（默认阈值 10，违规）
    public String getOrderStatus(Order order) {
        if (order == null) {                     // +1
            return "INVALID";
        }

        if (order.isCancelled()) {               // +1
            return "CANCELLED";
        }

        if (order.isRefunded()) {                 // +1
            return "REFUNDED";
        }

        switch (order.getStatus()) {             // +1
            case "PENDING":
                if (order.isPaymentConfirmed()) {  // +1
                    return "READY_TO_SHIP";
                }
                return "AWAITING_PAYMENT";

            case "SHIPPED":
                if (order.isDelivered()) {         // +1
                    return "DELIVERED";
                }
                if (order.isInTransit()) {         // +1
                    return "IN_TRANSIT";
                }
                return "SHIPPED";

            case "ON_HOLD":
                if (order.isFraudCheck()) {        // +1
                    return "FRAUD_REVIEW";
                }
                return "ON_HOLD";

            default:                              // +1
                return "UNKNOWN";
        }
    }
}
```

**修正方案**：

```java
public class OrderService {

    // ✅ 通过策略模式分解，每个方法复杂度 ≤ 5
    public String getOrderStatus(Order order) {
        if (order == null || order.isCancelled() || order.isRefunded()) {
            return getTerminalStatus(order);
        }
        return getActiveStatus(order);
    }

    private String getTerminalStatus(Order order) {
        if (order == null) return "INVALID";
        if (order.isCancelled()) return "CANCELLED";
        if (order.isRefunded()) return "REFUNDED";
        return "UNKNOWN";
    }

    private String getActiveStatus(Order order) {
        return switch (order.getStatus()) {
            case "PENDING" -> getPendingStatus(order);
            case "SHIPPED" -> getShippingStatus(order);
            case "ON_HOLD" -> "ON_HOLD";
            default -> "UNKNOWN";
        };
    }

    private String getPendingStatus(Order order) {
        return order.isPaymentConfirmed() ? "READY_TO_SHIP" : "AWAITING_PAYMENT";
    }

    private String getShippingStatus(Order order) {
        if (order.isDelivered()) return "DELIVERED";
        if (order.isInTransit()) return "IN_TRANSIT";
        return "SHIPPED";
    }
}
```

### 5.4 TooManyMethods（方法过多 — God Class 检测）

**描述**：类中定义的方法过多，可能违反了单一职责原则。

**类别**：Design

```java
// ❌ TooManyMethods: 此类包含了 25+ 个方法
public class UserService {
    // 用户 CRUD
    public void createUser(User user) { /* ... */ }
    public User getUser(Long id) { /* ... */ }
    public void updateUser(User user) { /* ... */ }
    public void deleteUser(Long id) { /* ... */ }

    // 密码管理
    public void changePassword(Long userId, String oldPwd, String newPwd) { /* ... */ }
    public void resetPassword(String email) { /* ... */ }
    public boolean validatePassword(String raw, String encoded) { /* ... */ }

    // 角色权限
    public void assignRole(Long userId, Long roleId) { /* ... */ }
    public void removeRole(Long userId, Long roleId) { /* ... */ }
    public List<Role> getUserRoles(Long userId) { /* ... */ }

    // 邮件通知
    public void sendWelcomeEmail(User user) { /* ... */ }
    public void sendPasswordResetEmail(String email) { /* ... */ }

    // 日志审计
    public void logLogin(Long userId) { /* ... */ }
    public List<AuditLog> getLoginHistory(Long userId) { /* ... */ }

    // ... 更多方法
}
```

**修正方案**：

```java
// ✅ 拆分为多个职责单一的类
public class UserCrudService { /* 用户 CRUD */ }
public class PasswordService { /* 密码管理 */ }
public class RoleAssignmentService { /* 角色权限 */ }
public class NotificationService { /* 邮件通知 */ }
public class AuditLogService { /* 日志审计 */ }
```

### 5.5 UselessParentheses（无用的括号）

**描述**：表达式中的括号是多余的，不影响运算优先级，应移除以提高代码可读性。

**类别**：Code Style

```java
public class MathUtils {

    public double calculate(int a, int b, int c) {
        // ❌ UselessParentheses
        return (a * b) + (c);          // 外层括号多余
    }

    public boolean isValid(int x, int y) {
        // ❌ UselessParentheses
        if ((x > 0) && (y > 0)) {      // 没必要为每个条件加括号
            return true;
        }
        return false;
    }
}
```

**修正方案**：

```java
public double calculate(int a, int b, int c) {
    return a * b + c;                  // ✅ 移除多余括号
}

public boolean isValid(int x, int y) {
    return x > 0 && y > 0;            // ✅ 简洁明了
}
```

### 5.6 UnnecessaryConstructor（不必要的构造器）

**描述**：类中定义了无参数的空构造器，而 Java 会自动生成，此类构造器是多余的。

**类别**：Code Style

```java
public class User {

    private String name;
    private int age;

    // ❌ UnnecessaryConstructor: 编译器会自动生成无参构造器
    public User() {
        // 没有任何操作
    }

    // 必要的：带参数的构造器
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

**修正方案**：

```java
public class User {
    // ✅ 移除空的无参构造器，或添加初始化逻辑
    public User() {
        this.name = "default";
        this.age = 0;
    }
}
```

### 5.7 AvoidCatchingGenericException（捕获泛化异常）

**描述**：捕获过于通用的异常（`Exception`、`Throwable`），会掩盖真正的问题。

**类别**：Best Practices

```java
public class FileService {

    public String readFile(String path) {
        try {
            // ❌ AvoidCatchingGenericException
            // 捕获 Exception 过于宽泛，隐藏了 NullPointerException、
            // ArrayIndexOutOfBoundsException 等非预期异常
            byte[] data = Files.readAllBytes(Paths.get(path));
            return new String(data);
        } catch (Exception e) {
            // 所有异常都被这里捕获
            logger.error("Error reading file: {}", e.getMessage());
            return "";
        }
    }
}
```

**修正方案**：

```java
public class FileService {

    public String readFile(String path) {
        try {
            byte[] data = Files.readAllBytes(Paths.get(path));
            return new String(data);
        } catch (NoSuchFileException e) {
            logger.warn("File not found: {}", path);
            return "";
        } catch (IOException e) {        // ✅ 捕获具体异常
            logger.error("IO error reading file: {}", path, e);
            throw new UncheckedIOException("Failed to read: " + path, e);
        }
    }
}
```

### 5.8 PreserveStackTrace（保留异常堆栈）

**描述**：在捕获异常后抛出新的异常时，没有传入原异常作为 cause，导致堆栈信息丢失。

**类别**：Error Prone

```java
public class DataService {

    // ❌ PreserveStackTrace: 新异常没有包装原异常
    public User findUser(Long id) {
        try {
            return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        } catch (EntityNotFoundException e) {
            // 堆栈信息丢失
            throw new ServiceException("Failed to find user: " + id);
        }
    }
}
```

**修正方案**：

```java
public class DataService {

    // ✅ 保留原始异常堆栈
    public User findUser(Long id) {
        try {
            return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        } catch (EntityNotFoundException e) {
            throw new ServiceException("Failed to find user: " + id, e);  // ✅
        }
    }
}
```

### 5.9 ShortVariable / LongVariable（变量命名规范）

**ShortVariable 描述**：变量名过短（如 `a`、`b`、`x`），无法表达语义。

**LongVariable 描述**：变量名过长（超过阈值），不利于阅读。

**类别**：Code Style

```java
public class OrderProcessor {

    public void process(Order order) {
        // ❌ ShortVariable: 变量名 "o" 无法表达含义
        Order o = order;

        // ❌ ShortVariable: "i" 含义不明确
        for (int i = 0; i < 10; i++) {
            // ...
        }
    }

    // ❌ LongVariable: 变量名过长
    private static final int MAXIMUM_NUMBER_OF_RETRY_ATTEMPTS_BEFORE_ALERTING = 3;
}
```

**修正方案**：

```java
public class OrderProcessor {

    public void process(Order order) {
        final Order currentOrder = order;  // ✅ 命名有意义

        for (int retryCount = 0; retryCount < 10; retryCount++) {  // ✅
            // ...
        }
    }

    private static final int MAX_RETRIES = 3;  // ✅ 适当长度
}
```

### 5.10 更多重要规则

| 规则 | 类别 | 描述 | 示例 |
|------|------|------|------|
| `AvoidDuplicateLiterals` | Performance | 字符串字面量重复出现 | `"error"` 出现多次 |
| `UseStringBufferForStringAppends` | Performance | 循环中使用 `StringBuilder` | `for(){s+=x;}` |
| `NullAssignment` | Error Prone | 对引用类型赋 null 作为默认值 | `String s = null;` |
| `ReturnEmptyArrayRatherThanNull` | Best Practices | 方法返回 null 而非空数组 | `return null;` → `return new int[0];` |
| `AvoidThrowingRawExceptionTypes` | Best Practices | 直接 throw new Exception | `throw new Exception("msg")` |
| `SwitchStmtsShouldHaveDefault` | Best Practices | switch 缺少 default 分支 | `switch(x) { case A: ... }` |
| `SimplifyBooleanReturns` | Code Style | 布尔返回值可简化 | `if(x) return true; else return false;` |
| `JUnitUseExpected` | Best Practices | JUnit 测试使用 expected 属性 | `@Test(expected=...)` |
| `AvoidSynchronizedAtMethodLevel` | Multithreading | 方法级 synchronized 不够精细 | `synchronized void method()` |

### 5.11 规则配置示例：调整参数

```xml
<rule ref="category/java/design.xml/CyclomaticComplexity">
    <properties>
        <!-- 将圈复杂度阈值从 10 调整为 15 -->
        <property name="classReportLevel" value="80" />
        <property name="methodReportLevel" value="15" />
    </properties>
</rule>

<rule ref="category/java/codestyle.xml/ShortVariable">
    <properties>
        <!-- 设置最小变量名长度为 3 -->
        <property name="minimum" value="3" />
    </properties>
</rule>

<rule ref="category/java/codestyle.xml/LongVariable">
    <properties>
        <!-- 设置最大变量名长度为 32 -->
        <property name="minimum" value="32" />
    </properties>
</rule>
```

---

## 6. CPD 重复代码检测

### 6.1 CPD 概述

CPD（Copy-Paste Detector）是 PMD 内置的重复代码检测模块。它通过 **Karp-Rabin 字符串匹配算法** 在源代码中寻找重复的代码块。

### 6.2 检测阈值配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-pmd-plugin</artifactId>
    <configuration>
        <!-- CPD 配置 -->
        <minimumTokens>100</minimumTokens>     <!-- 最小 token 数阈值 -->
        <cpdExcludes>
            <exclude>**/generated/**/*.java</exclude>
        </cpdExcludes>
        <cpdReportFormat>xml</cpdReportFormat>
    </configuration>
</plugin>
```

### 6.3 Gradle CPD 配置

```groovy
pmd {
    cpdThresholdMinimum = 100  // 最小 token 数
}
```

### 6.4 命令行执行

```bash
mvn pmd:cpd                   # 生成 CPD 报告
mvn pmd:cpd-check             # CPD 检查，超标则构建失败
```

### 6.5 CPD 报告示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<pmd-cpd>
    <duplication lines="12" tokens="82">
        <file line="12" path="src/main/java/com/example/service/OrderService.java"/>
        <file line="45" path="src/main/java/com/example/service/PaymentService.java"/>
        <codefragment>
            <![CDATA[
        if (order.isValid()) {
            BigDecimal total = order.getItems().stream()
                .map(Item::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotal(total);
            order.setStatus(OrderStatus.VERIFIED);
        }
            ]]>
        </codefragment>
    </duplication>
</pmd-cpd>
```

### 6.6 哪些应该被认为是重复代码？

| 情况 | 判定 |
|------|------|
| 完全相同的代码块（ctrl+c / ctrl+v） | 必须提取 |
| 仅变量名不同的类似代码 | 必须提取 |
| 相同逻辑但参数化可实现 | 建议提取 |
| 相同结构但类型不同的代码（泛型可解决） | 建议提取 |
| 相同语义但实现略有不同（策略模式场景） | 可能不需要 |

> ⚠️ CPD 基于 token 匹配而非 AST 匹配，因此变量名不同不影响检测结果。例如以下两段代码会被检测为重复：

```java
// 代码块 A
User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
user.setName(name);
user.setEmail(email);
return userRepository.save(user);

// 代码块 B
Product product = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
product.setName(name);
product.setPrice(price);
return productRepository.save(product);
```

---

## 7. 报告阅读与阈值控制

### 7.1 HTML 报告结构

```
PMD Report (target/pmd/pmd.html)
├── Summary
│   ├── Total Files: 120
│   ├── Total Violations: 45
│   ├── Priority 1: 3
│   ├── Priority 2: 8
│   ├── Priority 3: 15
│   └── Priority 4-5: 19
├── Files (按违规数降序排列)
│   ├── OrderService.java: 12 violations
│   │   ├── CyclomaticComplexity (Priority 3)
│   │   ├── AvoidCatchingGenericException (Priority 1)
│   │   └── ...
│   ├── UserController.java: 8 violations
│   └── ...
└── Rules (按触发频次)
    ├── UnusedLocalVariable: 15 times
    ├── ShortVariable: 10 times
    └── ...
```

### 7.2 XML 报告格式

```xml
<pmd version="7.0.0" timestamp="2024-01-15T10:30:00">
    <file name="src/main/java/com/example/service/OrderService.java">
        <violation rule="AvoidCatchingGenericException"
                   ruleset="Best Practices"
                   priority="1"
                   beginline="25"
                   endline="30"
                   class="OrderService"
                   method="processOrder">
            <![CDATA[避免捕获通用的异常类型，如 Exception、Throwable 等]]>
        </violation>
        <violation rule="CyclomaticComplexity"
                   ruleset="Design"
                   priority="3"
                   beginline="15"
                   endline="60"
                   class="OrderService"
                   method="calculateDiscount"
                   externalInfoUrl="https://pmd.github.io/pmd/pmd_rules_java_design.html#cyclomaticcomplexity">
            <![CDATA[方法 calculateDiscount 的圈复杂度为 12，高于允许的阈值 10]]>
        </violation>
    </file>
</pmd>
```

### 7.3 阈值控制策略

```xml
<configuration>
    <!-- 策略 1：优先级阈值 -->
    <!-- 只报告优先级 ≤ 2 的违规 -->
    <failurePriority>2</failurePriority>

    <!-- 策略 2：最大违规数 -->
    <!-- 违规总数超过 50 则构建失败 -->
    <maxAllowedViolations>50</maxAllowedViolations>
</configuration>
```

> 💡 推荐采用**渐进式策略**：新项目从 `maxAllowedViolations=0` 开始，设置 `failurePriority=2`。遗留项目可先放宽阈值，逐步改进后收紧。

### 7.4 自定义报告格式

```xml
<configuration>
    <format>html</format>
    <!-- 自定义 XSLT 转换 -->
    <xslt>
        <file>config/pmd/custom-report.xslt</file>
    </xslt>
</configuration>
```

---

## 8. 抑制 PMD 警告

### 8.1 `@SuppressWarnings("PMD")` 注解

```java
import java.util.Date;

public class EntityBase {

    // 抑制指定规则
    @SuppressWarnings("PMD.ShortVariable")
    public void process(Date dt) {
        // dt 作为参数名可以接受（框架约定）
    }

    // 抑制多个规则
    @SuppressWarnings({"PMD.ShortVariable", "PMD.UnusedPrivateMethod"})
    private void helper() {
        // ...
    }
}
```

### 8.2 NOPMD 注释标记

```java
public class LegacyAdapter {

    // 在行尾添加 NOPMD 标记，抑制该行的所有 PMD 警告
    public void oldApi() {
        int x = 0; // NOPMD - 遗留代码兼容
    }
}
```

### 8.3 suppressions.xml 文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://pmd.sourceforge.io/suppressions_1_0.xsd">
    <!-- 按文件排除 -->
    <suppress files="**/generated/**" />

    <!-- 按规则 + 文件排除 -->
    <suppress files="**/controller/*Controller.java" rule="CyclomaticComplexity" />

    <!-- 按规则 + 类排除 -->
    <suppress
        files="**/domain/*.java"
        rule="AvoidDuplicateLiterals"
        description="领域对象中重复字面量可接受" />

    <!-- 按规则名称排除 -->
    <suppress rule="UnusedPrivateMethod"
        description="保留给扩展点使用" />
</suppressions>
```

### 8.4 Maven 配置 suppressions

```xml
<configuration>
    <suppressFiles>config/pmd/suppressions.xml</suppressFiles>
</configuration>
```

> ⚠️ 使用 `@SuppressWarnings` 应附带注释说明理由。过度使用抑制机制会使静态分析失去意义。

---

## 9. 自定义 XPath 规则

### 9.1 XPath 规则简介

PMD 支持使用 **XPath 表达式** 在 Java AST 上编写自定义规则。只需一个 XML 文件，无需编写 Java 代码。

### 9.2 AST 树示例

```java
public class Hello {
    public void sayHello() {
        System.out.println("Hello");
    }
}
```

对应的 AST 结构（简化）：

```
CompilationUnit
 └── TypeDeclaration
      └── ClassDeclaration
           ├── modifiers: public
           └── body
                └── MethodDeclaration
                     ├── modifiers: public
                     ├── returnType: void
                     ├── name: sayHello
                     └── body
                          └── BlockStatement
                               └── StatementExpression
                                    └── PrimaryExpression ...
```

### 9.3 自定义规则示例：禁止 System.out.println

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ruleset name="custom-rules"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0
         https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
    <description>自定义规则集</description>

    <rule name="NoSystemOutPrintln"
          language="java"
          message="禁止使用 System.out.println，请使用日志框架"
          class="net.sourceforge.pmd.lang.rule.XPathRule">
        <description>
            禁止直接使用 System.out.println 输出，
            应替换为 SLF4J/Logback 日志记录器。
        </description>
        <priority>2</priority>
        <properties>
            <!-- 优先使用 log4j 2.0 的 String.indexOf 等效方法 -->
            <property name="xpath">
                <value>
                    <![CDATA[
//Name[
    (@Image = "println" or @Image = "print" or @Image = "printf")
    and
    ../../PrimaryPrefix/Name[@Image = "System.out"]
]
                    ]]>
                </value>
            </property>
        </properties>
    </rule>
</ruleset>
```

### 9.4 更多自定义规则示例

**检测不正确的日志用法**：

```xml
<rule name="LoggerNotPrivateStaticFinal"
      language="java"
      message="Logger 应声明为 private static final"
      class="net.sourceforge.pmd.lang.rule.XPathRule">
    <description>Logger 必须符合 private static final 模式</description>
    <priority>3</priority>
    <properties>
        <property name="xpath">
            <value>
                <![CDATA[
//FieldDeclaration[
    Type/ReferenceType/ClassOrInterfaceType[@Image = 'Logger']
    and
    not(Modifier[@Image = 'private'])
    and
    not(Modifier[@Image = 'static'])
    and
    not(Modifier[@Image = 'final'])
]
                ]]>
            </value>
        </property>
    </properties>
</rule>
```

**检测事务超时未设置**：

```xml
<rule name="TransactionTimeoutNotSet"
      language="java"
      message="@Transactional 方法应显式指定 timeout"
      class="net.sourceforge.pmd.lang.rule.XPathRule">
    <properties>
        <property name="xpath">
            <value>
                <![CDATA[
//Annotation[Name/@Image = 'Transactional']
    [not(MemberValuePairs/MemberValuePair[
        @Name = 'timeout'
    ])]
                ]]>
            </value>
        </property>
    </properties>
</rule>
```

### 9.5 调试 XPath 规则

PMD 提供了 AST 查看工具：

```bash
# 查看 Java 文件的 AST 树
pmd pmd -dir src/main/java/MyFile.java -format xml -rulesets rulesets/java/quickstart.xml -debug

# 使用 PMD GUI 设计器
pmd-designer
```

> 💡 PMD 7.x 提供了一个基于 Web 的规则设计器，可以在浏览器中编写和测试 XPath 规则，实时查看 AST 结构和匹配结果。

---

## 10. PMD vs Checkstyle vs SpotBugs 定位表

### 10.1 三工具对比

| 维度 | PMD | Checkstyle | SpotBugs |
|------|-----|------------|----------|
| **全称** | PMD | Checkstyle | SpotBugs（FindBugs 继任） |
| **分析层次** | 源码（AST） | 源码（Token/AST） | 字节码（.class） |
| **是否需编译** | 不需要 | 不需要 | 需要 |
| **核心关注** | 编码质量、易错模式 | 代码风格、格式规范 | 运行时缺陷、安全漏洞 |
| **检测能力** | 死代码、复杂度、命名 | 缩进、命名、Javadoc | 空指针、并发、序列化 |
| **自定义规则** | XPath / Java | 扩展 Checks（Java） | 扩展 Detectors（Java+ASM） |
| **误报率** | 中高 | 低 | 中 |
| **风格检查** | ✅ 有限 | ✅ 核心能力 | ❌ |
| **Bug 检测** | ✅ 中等 | ❌ | ✅ 深入 |
| **重复代码** | ✅ CPD 模块 | ❌ | ❌ |
| **报告格式** | XML/HTML/CSV/Text | XML/HTML/Plain | XML/HTML/XSLT |
| **规则数量** | 300+ | 150+ | 400+ |
| **学习曲线** | 中 | 低 | 中高 |
| **IDE 集成** | ✅ IntelliJ/Eclipse | ✅ IntelliJ/Eclipse | ✅ IntelliJ/Eclipse |
| **CI/CD 集成** | ✅ | ✅ | ✅ |

### 10.2 分工定位

```
                 ┌─────────────────────────────────────┐
                 │         代码格式与风格                │
                 │          ┌───────────────┐           │
                 │          │  Checkstyle   │           │
                 │          │  制式检查器    │           │
                 │          └───────────────┘           │
                 ├─────────────────────────────────────┤
                 │         代码质量与规范                │
                 │          ┌───────────────┐           │
                 │          │     PMD       │           │
                 │          │  源码分析器    │           │
                 │          └───────────────┘           │
                 ├─────────────────────────────────────┤
                 │         运行时缺陷检测                │
                 │          ┌───────────────┐           │
                 │          │   SpotBugs    │           │
                 │          │  字节码检测    │           │
                 │          └───────────────┘           │
                 ├─────────────────────────────────────┤
                 │         综合质量平台                  │
                 │          ┌───────────────┐           │
                 │          │   SonarQube   │           │
                 │          │  聚合与展示    │           │
                 │          └───────────────┘           │
                 └─────────────────────────────────────┘
```

### 10.3 工具选择建议

| 项目情况 | 推荐方案 |
|----------|----------|
| **新项目（全栈）** | Checkstyle（风格）+ PMD（质量）+ SpotBugs（缺陷）+ Error Prone（编译期） |
| **小型项目** | PMD 一套够用（含 CPD） |
| **遗留系统维护** | SpotBugs（抓运行时 Bug）+ CPD（去重） |
| **严格格式要求** | Checkstyle（强制执行 Google/Sun 规范） |
| **安全敏感项目** | SpotBugs（安全检测器）+ PMD Security 规则 |
| **团队风格自由** | PMD 够用，无需 Checkstyle |

### 10.4 SonarQube 中的集成

SonarQube 内置了 PMD、Checkstyle、SpotBugs 的规则集，可通过一个平台统一管理：

```
SonarQube
├── PMD 规则        → SonarQube Java Analyzer (SonarJava)
├── Checkstyle 规则  → SonarQube Java Analyzer
└── SpotBugs 规则    → SonarQube Java Analyzer
```

> 🎯 **推荐配置策略**：CI 流水线中同时运行 PMD + SpotBugs + Checkstyle，各自的报告可聚合到 SonarQube 进行统一展示和质量门禁。

---

## 11. 最佳实践与常用配置模板

### 11.1 项目推荐配置

**新项目严格模式（适用于微服务新项目）**：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ruleset name="strict-rules"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0
         https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
    <description>新项目严格规则集</description>

    <!-- 必须修复的 -->
    <rule ref="category/java/errorprone.xml">
        <exclude name="DataflowAnomalyAnalysis" />
    </rule>
    <rule ref="category/java/bestpractices.xml" />

    <!-- 推荐修复的 -->
    <rule ref="category/java/design.xml">
        <properties>
            <property name="CyclomaticComplexity" value="15" />
        </properties>
    </rule>
    <rule ref="category/java/performance.xml" />

    <!-- 风格类（宽松） -->
    <rule ref="category/java/codestyle.xml">
        <exclude name="LocalVariableCouldBeFinal" />
        <exclude name="MethodArgumentCouldBeFinal" />
        <exclude name="OnlyOneReturn" />
        <exclude name="AtLeastOneConstructor" />
        <exclude name="LongVariable" />
        <exclude name="ShortVariable" />
    </rule>
</ruleset>
```

**遗留项目温和模式**：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ruleset name="legacy-rules"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0
         https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
    <description>遗留项目规则集 - 仅报告 Priority 1-2</description>

    <rule ref="category/java/errorprone.xml">
        <priority>1</priority>
    </rule>
    <rule ref="category/java/bestpractices.xml">
        <priority>1</priority>
    </rule>

    <!-- 复杂性规则单独配置 -->
    <rule ref="category/java/design.xml/CyclomaticComplexity">
        <priority>2</priority>
        <properties>
            <property name="methodReportLevel" value="20" />
            <property name="classReportLevel" value="100" />
        </properties>
    </rule>
</ruleset>
```

### 11.2 完整 Maven 配置模板

```xml
<project>
    <properties>
        <pmd.version>3.21.2</pmd.version>
        <pmd.failurePriority>3</pmd.failurePriority>
        <pmd.maxAllowedViolations>50</pmd.maxAllowedViolations>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-pmd-plugin</artifactId>
                <version>${pmd.version}</version>
                <dependencies>
                    <dependency>
                        <groupId>net.sourceforge.pmd</groupId>
                        <artifactId>pmd-java</artifactId>
                        <version>7.0.0</version>
                    </dependency>
                </dependencies>
                <configuration>
                    <targetJdk>17</targetJdk>
                    <failurePriority>${pmd.failurePriority}</failurePriority>
                    <maxAllowedViolations>${pmd.maxAllowedViolations}</maxAllowedViolations>
                    <rulesets>
                        <ruleset>config/pmd/pmd-ruleset.xml</ruleset>
                    </rulesets>
                    <suppressFiles>config/pmd/suppressions.xml</suppressFiles>
                    <excludes>
                        <exclude>**/generated/**</exclude>
                    </excludes>
                    <format>html</format>
                    <linkXRef>false</linkXRef>
                    <minimumTokens>100</minimumTokens>
                </configuration>
                <executions>
                    <execution>
                        <id>pmd-check</id>
                        <goals>
                            <goal>check</goal>
                            <goal>cpd-check</goal>
                        </goals>
                        <phase>verify</phase>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 11.3 CI/CD 集成（GitHub Actions）

```yaml
name: PMD Analysis

on: [push, pull_request]

jobs:
  pmd:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run PMD
        run: mvn clean verify -Dpmd.maxAllowedViolations=0

      - name: Upload PMD Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: pmd-report
          path: target/pmd/

      - name: Check Violations
        if: failure()
        run: |
          echo "PMD 检测到代码违规，请查看报告并修复。"
          exit 1
```

### 11.4 PMD 7.x 重要变化

PMD 7.x 相对于 6.x 的主要变化：

| 变更 | 说明 |
|------|------|
| **规则集格式** | 使用 `category/java/bestpractices.xml` 替代旧格式 |
| **规则名称** | 部分规则已重命名 |
| **Java 支持** | 最低 Java 11，推荐 17+ |
| **XPath 版本** | XPath 3.1 支持 |
| **AST 变化** | Record、Sealed Class、Pattern Matching 支持 |
| **性能提升** | 分析速度显著提升，内存占用降低 |

> 💡 PMD 7.x 的规则引用格式发生变化：旧版 `rulesets/java/basic.xml` 已废弃，应使用 `category/java/bestpractices.xml` 等新格式。

### 11.5 常用命令速查

```bash
# PMD 分析
mvn pmd:pmd
mvn pmd:check

# CPD 重复代码检测
mvn pmd:cpd
mvn pmd:cpd-check

# 使用 PMD 命令行（独立运行）
pmd check -d src/main/java -R category/java/bestpractices.xml -f html > report.html

# AST 查看（调试规则）
pmd ast -d src/main/java/MyClass.java -language java

# 查看可用规则集
pmd rules --list
```

---

> 🎯 PMD 是 Java 源码静态分析的中坚力量，擅长发现死代码、复杂度问题和易错模式。与 Checkstyle（风格）和 SpotBugs（字节码缺陷）配合可构建完整的静态分析防护网。其 CPD 模块是轻量级重复代码检测的绝佳选择。在 SonarQube 普及前，PMD + Checkstyle + SpotBugs 是三件套黄金组合，即使在 SonarQube 时代，它们作为 CI 流水线中的独立检查环节仍有不可替代的价值。
