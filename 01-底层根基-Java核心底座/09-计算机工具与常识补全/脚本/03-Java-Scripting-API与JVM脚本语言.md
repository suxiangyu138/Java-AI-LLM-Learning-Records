# Java Scripting API 与 JVM 脚本语言

> JVM 不只是 Java 的家——JSR 223 打开多语言之门，Groovy 写 DSL，Kotlin 写脚本，JBang 让 Java 脚本飞

## 📚 目录

1. [JSR 223 / javax.script 详解](#1)
2. [Nashorn 到 GraalVM 的迁移之路](#2)
3. [Groovy 脚本生态](#3)
4. [Kotlin 脚本 (.kts)](#4)
5. [JBang —— Java 脚本新范式](#5)
6. [JVM 脚本语言全景对比](#6)

---

## 1. JSR 223 / javax.script 详解 {#1}

### 1.1 架构概览

JSR 223（Scripting for the Java Platform）定义了一套标准 API，让 Java 应用可以嵌入和执行任何 JVM 脚本语言。

```
┌──────────────────────────────────────────────┐
│              Java 应用程序                     │
│                                              │
│  ┌──────────────────────────────────────┐    │
│  │     javax.script (JSR 223)           │    │
│  │                                      │    │
│  │  ScriptEngineManager                 │    │
│  │  ├── discoverEngines()  发现可用引擎  │    │
│  │  └── getEngineByName()  按名称获取    │    │
│  │                                      │    │
│  │  ScriptEngine                        │    │
│  │  ├── eval(String)       执行脚本     │    │
│  │  ├── eval(Reader)       从文件执行   │    │
│  │  ├── put(key, value)    设置变量     │    │
│  │  └── get(key)           获取变量     │    │
│  │                                      │    │
│  │  Compilable (可选)                    │    │
│  │  └── compile(String) → CompiledScript│    │
│  │                                      │    │
│  │  Invocable (可选)                     │    │
│  │  ├── invokeFunction()  调用脚本函数   │    │
│  │  └── getInterface()    脚本对象代理   │    │
│  │                                      │    │
│  │  Bindings                            │    │
│  │  ├── ENGINE_SCOPE      引擎级变量    │    │
│  │  └── GLOBAL_SCOPE      全局级变量    │    │
│  └──────────────────────────────────────┘    │
│                    │                          │
│     ┌──────────────┼──────────────┐          │
│     ▼              ▼              ▼          │
│  ┌──────┐   ┌──────────┐   ┌────────┐       │
│  │ JS   │   │  Groovy  │   │ Python │  ...  │
│  │Engine│   │  Engine  │   │ Engine │       │
│  └──────┘   └──────────┘   └────────┘       │
└──────────────────────────────────────────────┘
```

### 1.2 基础用法

```java
import javax.script.*;

public class ScriptEngineDemo {
    public static void main(String[] args) throws Exception {
        // ── 1. 获取脚本引擎管理器 ──
        ScriptEngineManager manager = new ScriptEngineManager();

        // ── 2. 列出所有可用引擎 ──
        System.out.println("可用引擎:");
        manager.getEngineFactories().forEach(f ->
            System.out.printf("  %s (%s) → 扩展名: %s%n",
                f.getEngineName(),
                f.getLanguageName(),
                f.getExtensions()));

        // ── 3. 按名称获取引擎 ──
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        // 或按扩展名：manager.getEngineByExtension("js")
        // 或按 MIME：manager.getEngineByMimeType("text/javascript")

        // ── 4. 执行简单脚本 ──
        engine.eval("print('Hello from JS!')");

        // ── 5. 设置变量并执行 ──
        engine.put("name", "Java Developer");
        engine.put("score", 95);
        engine.eval("print(name + ' scored ' + score)");

        // ── 6. 获取脚本执行结果 ──
        Object result = engine.eval("1 + 2 + 3");
        System.out.println("计算结果: " + result);  // 6.0
    }
}
```

### 1.3 编译缓存（Compilable）

```java
import javax.script.*;

ScriptEngineManager manager = new ScriptEngineManager();
ScriptEngine engine = manager.getEngineByName("JavaScript");

// 检查是否支持编译
if (engine instanceof Compilable compilable) {

    // 编译脚本（可重复执行，避免每次解析）
    CompiledScript script = compilable.compile("""
        function calculate(x, y) {
            return x * y + Math.max(x, y);
        }
        calculate(10, 20);
        """);

    // 执行 10 万次
    for (int i = 0; i < 100_000; i++) {
        Bindings bindings = engine.createBindings();
        // 每次执行可以绑定不同的变量
        Object result = script.eval(bindings);
    }
}
```

> 💡 **提示**：编译型脚本引擎（如 GraalVM JS、Groovy）支持 `Compilable`，`CompiledScript.eval()` 性能显著优于 `ScriptEngine.eval()`。

### 1.4 函数调用（Invocable）

```java
// 在 Java 中调用脚本定义的函数
if (engine instanceof Invocable invocable) {

    // 定义脚本函数
    engine.eval("""
        function discount(price, rate) {
            return price * (1 - rate);
        }
        """);

    // 直接调用脚本函数
    Object result = invocable.invokeFunction("discount", 100.0, 0.2);
    System.out.println("折扣价: " + result);  // 80.0

    // 将脚本对象映射为 Java 接口（类型安全调用）
    engine.eval("""
        var calculator = {
            add: function(a, b) { return a + b; },
            multiply: function(a, b) { return a * b; }
        };
        """);
    Object calcObj = engine.get("calculator");

    Calculator calc = invocable.getInterface(calcObj, Calculator.class);
    System.out.println(calc.add(3, 4));       // 7.0
    System.out.println(calc.multiply(3, 4));  // 12.0
}

// 配合接口定义
interface Calculator {
    double add(double a, double b);
    double multiply(double a, double b);
}
```

### 1.5 Bindings 作用域

```java
ScriptEngine engine = manager.getEngineByName("JavaScript");

// ── ENGINE_SCOPE：仅当前引擎可见 ──
Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
engineScope.put("local", "引擎局部变量");

// ── GLOBAL_SCOPE：所有引擎实例共享 ──
Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
globalScope.put("shared", "全局共享变量");

// 执行时优先查找 ENGINE_SCOPE，再查 GLOBAL_SCOPE
engine.eval("print(local); print(shared);");
```

| 作用域 | 可见性 | 生命周期 | 典型用途 |
|--------|------|---------|---------|
| `ENGINE_SCOPE` | 当前 ScriptEngine 实例 | 引擎生命周期 | 请求级变量 |
| `GLOBAL_SCOPE` | 同一 ScriptEngineManager 创建的所有引擎 | Manager 生命周期 | 全局配置、共享数据 |

> 🎯 **核心要点**：ScriptEngine 的核心价值在于**动态可配置性**——规则引擎、定价公式、数据转换等经常变化的逻辑，用脚本维护比硬编码灵活得多。

---

## 2. Nashorn 到 GraalVM 的迁移之路 {#2}

### 2.1 Nashorn 时间线

| JDK 版本 | Nashorn 状态 | 说明 |
|---------|:---:|------|
| JDK 8 | ✅ 内置 | Oracle Nashorn，ECMAScript 5.1 + 扩展 |
| JDK 9-10 | ✅ 内置 | 继续可用 |
| JDK 11 | ⚠️ 标记废弃 | `@deprecated forRemoval=true` |
| JDK 15+ | ❌ 已移除 | 彻底从 JDK 中移除 |

### 2.2 如果项目仍在使用 Nashorn

```xml
<!-- 方案1：引入独立 Nashorn 依赖（短期过渡） -->
<dependency>
    <groupId>org.openjdk.nashorn</groupId>
    <artifactId>nashorn-core</artifactId>
    <version>15.4</version>
</dependency>
```

```java
// 方案2：迁移到 GraalVM JavaScript
// 引入依赖
// <dependency>
//     <groupId>org.graalvm.polyglot</groupId>
//     <artifactId>polyglot</artifactId>
//     <version>24.0.0</version>
// </dependency>
// <dependency>
//     <groupId>org.graalvm.polyglot</groupId>
//     <artifactId>js</artifactId>
//     <version>24.0.0</version>
// </dependency>

import org.graalvm.polyglot.*;

public class GraalVMScripting {
    public static void main(String[] args) {
        try (Context context = Context.create("js")) {

            // ── 基础执行 ──
            Value result = context.eval("js", "40 + 2");
            System.out.println(result.asInt());  // 42

            // ── 变量绑定 ──
            context.getBindings("js").putMember("name", "GraalVM");
            context.eval("js", "console.log('Hello ' + name)");

            // ── 函数定义与调用 ──
            context.eval("js", """
                function fibonacci(n) {
                    if (n <= 1) return n;
                    return fibonacci(n - 1) + fibonacci(n - 2);
                }
                """);
            Value fib = context.getBindings("js").getMember("fibonacci");
            System.out.println("fib(10) = " + fib.execute(10).asInt());  // 55

            // ── Java 与 JS 互操作 ──
            // JS 可访问 Java 类
            context.eval("js", """
                var ArrayList = Java.type('java.util.ArrayList');
                var list = new ArrayList();
                list.add('a'); list.add('b'); list.add('c');
                console.log('List size: ' + list.size());
                """);

            // ── 沙箱限制 ──
            try (Context sandbox = Context.newBuilder("js")
                    .allowHostAccess(HostAccess.ALL)
                    .allowHostClassLookup(className -> false)  // 禁止所有 Java 类访问
                    .build()) {
                sandbox.eval("js", "1 + 1");  // OK
                // sandbox.eval("js", "Java.type('java.io.File')");  // 拒绝
            }
        }
    }
}
```

### 2.3 迁移检查清单

| 检查项 | Nashorn | GraalVM | 迁移策略 |
|--------|---------|---------|---------|
| `print()` | ✅ 内置 | ❌ 改用 `console.log()` | 搜索替换 |
| `load()` | ✅ 内置 | ❌ 改用 `Polyglot.evalFile()` | 重新实现 |
| Java 类型访问 | `Java.type()` | `Java.type()` 需启用 | 打开 `allowHostAccess` |
| Java.from/Java.to | ✅ 内置 | ❌ 改用 JS 原生方法 | 用 `Array.from` 替代 |
| `Object.bindProperties()` | ✅ 内置 | ❌ 不支持 | 手动赋值属性 |
| 类型转换 | 自动隐式转换 | 更严格 | 显式转换 |
| 正则表达式 | Nashorn 特有扩展 | 标准 RegExp | 复查跨平台正则 |

```javascript
// ── Nashorn vs GraalVM 差异速查 ──

// 1. print → console.log
// Nashorn
print("Hello");
// GraalVM
console.log("Hello");

// 2. load → eval + read
// Nashorn
load("lib/helper.js");
// GraalVM（Java 侧处理）
// context.eval("js", Files.readString(Path.of("lib/helper.js")));

// 3. Java 互操作
// Nashorn（总是可用）
var File = Java.type('java.io.File');
// GraalVM（需要 HostAccess 权限）
var File = Java.type('java.io.File');
```

---

## 3. Groovy 脚本生态 {#3}

### 3.1 Groovy 脚本引擎

```xml
<dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy-jsr223</artifactId>
    <version>4.0.22</version>
</dependency>
```

```java
import javax.script.*;
import groovy.lang.*;

public class GroovyScripting {

    // ── 方式1：JSR 223 标准 API ──
    static void viaJsr223() throws Exception {
        ScriptEngine engine = new ScriptEngineManager()
            .getEngineByName("groovy");

        // 动态规则
        engine.put("user", Map.of("age", 25, "vip", true, "purchase", 1500));
        engine.put("config", Map.of("freeShipping", 99.0, "vipDiscount", 0.85));

        Object shippingFee = engine.eval("""
            def fee = 0.0
            if (user.purchase >= config.freeShipping) {
                fee = 0.0  // 满包邮
            }
            if (user.vip) {
                fee = fee * config.vipDiscount  // VIP 折扣
            }
            return fee
            """);
        System.out.println("运费: " + shippingFee);
    }

    // ── 方式2：GroovyShell 直接调用 ──
    static void viaGroovyShell() {
        var binding = new Binding();
        binding.setProperty("name", "Groovy");
        binding.setProperty("items", List.of(1, 2, 3, 4, 5));

        var shell = new GroovyShell(binding);
        Object result = shell.evaluate("""
            def doubled = items.collect { it * 2 }
            def sum = doubled.sum()
            return "${name}: items doubled = ${doubled}, sum = ${sum}"
            """);
        System.out.println(result);
    }

    // ── 方式3：GroovyScriptEngine（管理脚本目录）──
    static void viaGroovyScriptEngine() throws Exception {
        var gse = new GroovyScriptEngine("scripts/");
        var binding = new Binding();
        binding.setProperty("input", "world");

        // 动态加载外部脚本文件
        Object result = gse.run("greet.groovy", binding);
        System.out.println(result);
    }
}
```

### 3.2 Groovy DSL 构建能力

```groovy
// Groovy 的闭包委托机制天然适合构建 DSL

// ── 示例：自定义邮件 DSL ──
def email = new EmailBuilder().build {
    from "sender@example.com"
    to "receiver@example.com"
    cc "boss@example.com"
    subject "项目进度报告"

    body {
        header "本周工作总结"
        paragraph "功能A已完成，功能B进行中"
        list(["Spring Boot 3.3 升级", "Redis 集群部署", "日志系统改造"])
        footer "此邮件由系统自动生成"
    }
}

// 实现方式：Groovy 闭包的 delegate 策略
class EmailBuilder {
    def build(@DelegatesTo(EmailSpec) Closure cl) {
        def spec = new EmailSpec()
        cl.delegate = spec
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
        return spec.build()
    }
}

// 同类 DSL：Gradle build scripts、Jenkins Pipeline、Spock 测试
```

### 3.3 Jenkins Pipeline 就是 Groovy 脚本

```groovy
// Jenkinsfile —— 声明式 Pipeline 示例
pipeline {
    agent any

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-21'
        MAVEN_OPTS = '-Xmx2g'
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/company/project.git',
                    branch: 'main'
            }
        }

        stage('Build') {
            steps {
                sh './mvnw clean compile'
            }
        }

        stage('Test') {
            parallel {
                stage('Unit Tests') {
                    steps { sh './mvnw test' }
                }
                stage('Integration Tests') {
                    steps { sh './mvnw verify -Pintegration-test' }
                }
            }
        }

        stage('SonarQube') {
            steps {
                sh './mvnw sonar:sonar'
            }
        }
    }

    post {
        success  { emailext body: '构建成功 ✅', subject: 'CI 通知',
                    to: 'team@company.com' }
        failure  { emailext body: '构建失败 ❌', subject: 'CI 通知',
                    to: 'team@company.com' }
    }
}
```

### 3.4 Groovy 在 2025-2026 的定位

| 领域 | 地位 | 趋势 |
|------|:---:|:---:|
| **Jenkins Pipeline** | 事实标准 | 稳定，维护模式 |
| **Gradle 构建** | 被 Kotlin DSL 取代中 | ⬇️ 下降 |
| **Spock 测试** | 主流选择 | 稳定 |
| **Grails 框架** | 遗留维护 | ⬇️ 下降 |
| **DSL 构造** | 核心优势保持 | ➡️ 稳定 |
| **新后端服务** | 不推荐 | ⬇️ |

---

## 4. Kotlin 脚本 (.kts) {#4}

### 4.1 Kotlin 脚本基础

```bash
# Kotlin 脚本文件扩展名：.kts
# 需要 kotlin-main-kts 依赖

# Maven 依赖
# <dependency>
#     <groupId>org.jetbrains.kotlin</groupId>
#     <artifactId>kotlin-main-kts</artifactId>
#     <version>2.0.0</version>
# </dependency>
```

```kotlin
// demo.kts —— Kotlin 脚本示例
@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

import com.google.gson.Gson
import kotlinx.coroutines.*

// ── 声明外部依赖（注解方式）──
// 执行：kotlin demo.kts

// ── 调用 HTTP API ──
data class Repo(val name: String, val stars: Int, val language: String?)

suspend fun fetchRepos(): List<Repo> = coroutineScope {
    val url = java.net.URI.create("https://api.github.com/users/oracle/repos").toURL()
    val json = url.readText()
    val gson = Gson()
    val array = gson.fromJson(json, Array<Map<String, Any>>::class.java)
    array.map {
        Repo(
            name = it["name"] as String,
            stars = (it["stargazers_count"] as Double).toInt(),
            language = it["language"] as String?
        )
    }
}

// ── 执行 ──
runBlocking {
    val repos = fetchRepos()
    repos
        .sortedByDescending { it.stars }
        .take(5)
        .forEach { println("⭐ ${it.stars} - ${it.name} (${it.language ?: "N/A"})") }
}
```

```bash
# 运行 Kotlin 脚本
kotlinc -script demo.kts
# 或使用 kotlin 命令
kotlin demo.kts
```

### 4.2 Gradle Kotlin DSL

```kotlin
// build.gradle.kts —— 类型安全的构建脚本
plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### 4.3 Kotlin vs Groovy 脚本

| 维度 | Kotlin (.kts) | Groovy (.groovy) |
|------|:---:|:---:|
| **类型安全** | ✅ 静态类型 | ⚠️ 可选静态类型 |
| **IDE 支持** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **编译速度** | 中等 | 中等偏慢 |
| **脚本启动速度** | 较慢（编译开销） | 中等 |
| **Java 互操作** | ✅ 优秀 | ✅ 优秀 |
| **DSL 构建** | ✅ 扩展函数 + Lambda | ✅ 闭包委托 |
| **空安全** | ✅ 内建 | ❌ |
| **协程/异步** | ✅ 一等支持 | ❌ GPars 插件 |
| **Gradle 推荐** | ✅ 默认推荐 | 仍可用 |
| **Android** | ✅ 官方语言 | ❌ 不支持 |

---

## 5. JBang —— Java 脚本新范式 {#5}

### 5.1 什么是 JBang？

JBang 是一个让 Java 成为脚本语言的工具——无需项目结构、无需构建配置，写一个 `.java` 文件就能运行，还能自动管理 Maven/Gradle 依赖。

```bash
# 安装 JBang
# Windows: choco install jbang 或 scoop install jbang
# macOS: brew install jbangdev/tap/jbang
# Linux: curl -Ls https://sh.jbang.dev | bash -s - app setup

jbang --version
```

### 5.2 零配置脚本

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS com.google.code.gson:gson:2.10.1
//DEPS info.picocli:picocli:4.7.5

// github-stars.java —— 查询 GitHub Stars
import com.google.gson.*;

public class github_stars {

    public static void main(String[] args) throws Exception {
        String user = args.length > 0 ? args[0] : "oracle";
        var url = new java.net.URI(
            "https://api.github.com/users/" + user + "/repos").toURL();

        var connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

        String json;
        try (var reader = new java.io.InputStreamReader(connection.getInputStream())) {
            json = new String(reader.readToEnd().getBytes());
            // 实际项目中请用 try-with-resources 逐块读取
        }

        var repos = new Gson().fromJson(json, JsonArray.class);
        System.out.println("\n📦 " + user + " 的仓库 (Star 数):");
        System.out.println("=".repeat(50));

        for (var repo : repos) {
            var obj = repo.getAsJsonObject();
            System.out.printf("  ⭐ %-6d %s%n",
                obj.get("stargazers_count").getAsInt(),
                obj.get("name").getAsString());
        }
    }
}
```

```bash
# 直接运行！
jbang github-stars.java oracle
# 或 chmod +x 后直接执行
chmod +x github-stars.java
./github-stars.java oracle
```

### 5.3 JBang 核心注解

| 指令 | 功能 | 示例 |
|------|------|------|
| `//JAVA 21` | 指定 JDK 版本（JBang 自动下载） | `//JAVA 21` |
| `//DEPS` | Maven 坐标依赖 | `//DEPS com.google.code.gson:gson:2.10.1` |
| `//FILES` | 引用本地文件 | `//FILES helper.java` |
| `//SOURCES` | 引用源码目录 | `//SOURCES src/main/java` |
| `//REPOS` | 添加 Maven 仓库 | `//REPOS jcenter=https://jcenter.bintray.com` |
| `//DEPS` 传递 | 依赖的依赖自动解决 | 无需额外配置 |

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS org.springframework:spring-web:6.1.8
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.0
//DEPS org.slf4j:slf4j-simple:2.0.12

// 多个 DEPS —— JBang 自动解析传递依赖
// 支持 Maven BOM：
// //DEPS org.springframework.boot:spring-boot-dependencies:3.3.0@pom
```

### 5.4 JBang 实用命令

```bash
# ── 运行脚本 ──
jbang hello.java

# ── 初始化为可编辑项目 ──
jbang init hello.java              # 初始化为 JBang 脚本
jbang init --template=cli hello.java  # 使用 CLI 模板

# ── 导出为 Maven/Gradle 项目 ──
jbang export maven hello.java      # 导出为 Maven 项目
jbang export gradle hello.java     # 导出为 Gradle 项目

# ── 安装为系统命令 ──
jbang app install --name mytool tool.java
# 之后可直接执行 mytool

# ── 查看依赖树 ──
jbang info tools hello.java

# ── 编辑（自动下载依赖的 sources 和 javadoc）──
jbang edit hello.java              # 在 IDE 中打开
jbang edit --live hello.java       # 实时重载模式
```

### 5.5 JBang vs JShell vs 传统项目

| 维度 | JBang | JShell | 传统 Maven/Gradle |
|------|:---:|:---:|:---:|
| **上手成本** | 极低 | 极低 | 中等 |
| **依赖管理** | ✅ `//DEPS` 注解 | ❌ 手动 `--class-path` | ✅ pom.xml / build.gradle |
| **脚本可执行** | ✅ Shebang | ✅ Shebang | ❌ 需打包 JAR |
| **IDE 支持** | ✅ 原生 jbang edit | ✅ 原生集成 | ✅ 原生支持 |
| **导出为项目** | ✅ `jbang export` | ❌ | N/A |
| **生产部署** | ❌ 不推荐 | ❌ | ✅ 标准方式 |
| **适合场景** | 工具脚本、原型 | 学习、探索 | 正式项目 |

> 🎯 **核心要点**：JBang 填补了"Java 工具脚本"的空白——用 Java 写脚本，享受 JVM 生态全部类库，同时保持脚本的轻便性。

---

## 6. JVM 脚本语言全景对比 {#6}

### 6.1 综合对比矩阵

| 维度 | Java (JEP 330) | JShell | Groovy | Kotlin .kts | JBang | GraalVM JS |
|------|:---:|:---:|:---:|:---:|:---:|:---:|
| **JDK 版本** | 11+ | 9+ | 任意 | 任意 | 任意 | 任意 |
| **类型安全** | ✅ | ✅ | ⚠️ 可选 | ✅ | ✅ | ❌ 动态 |
| **启动速度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **依赖管理** | 手动 | 手动 | Gradle/Maven | `@DependsOn` | `//DEPS` | 手动 |
| **脚本即执行** | ❌ | ✅ Shebang | ✅ | ✅ | ✅ | ✅ |
| **交互式 REPL** | N/A | ✅ | ✅ groovysh | ✅ | ❌ | ❌ |
| **DSL 能力** | ⭐⭐ | ⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐ |
| **IDE 支持** | ✅ | ✅ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ✅ | ⭐⭐ |
| **生产嵌入** | ❌ | ❌ | ✅ ScriptEngine | ⚠️ 可行 | ❌ | ✅ 最佳 |

### 6.2 场景选型速查

| 场景 | 推荐工具 | 理由 |
|------|---------|------|
| **学习 Java API** | JShell | 交互式反馈，零上下文开销 |
| **验证一段逻辑** | JShell | 最快启动概念验证 |
| **运维工具脚本** | JBang | 即写即用，自动管理依赖 |
| **CI/CD 流水线** | Jenkins (Groovy) / GitHub Actions (YAML) | 业界标准 |
| **构建脚本** | Gradle Kotlin DSL | 类型安全 + 官方推荐 |
| **业务规则引擎** | Groovy via ScriptEngine | DSL 表达力 + Java 无缝互操作 |
| **嵌入 JavaScript** | GraalVM JS | 高性能 + 安全沙箱 |
| **快速 CRUD API** | Kotlin .kts | 协程 + 类型安全 |
| **跨语言多语言脚本** | GraalVM Polyglot | 真正多语言互操作 |

> 🎯 **核心要点**：没有万能工具——JShell 用于探索，JBang 用于工具脚本，Groovy 用于 DSL，Kotlin 用于类型安全的构建脚本，GraalVM 用于多语言集成。各取所长，组合使用。

---

**返回总览：** [00-脚本知识体系总览](./00-脚本知识体系总览.md) | **下一模块：** [04-构建脚本与 CI/CD 自动化](./04-构建脚本与CI-CD自动化.md)
