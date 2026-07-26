# 01 - Java概述与开发环境
> 定位：理解Java语言背景、平台体系、开发环境搭建与程序运行全流程，建立Java开发全局观

## 目录
1. [Java语言概述](#1-java语言概述)
2. [JDK/JRE/JVM关系](#2-jdkjrejvm关系)
3. [Java程序编译与运行机制](#3-java程序编译与运行机制)
4. [开发环境搭建](#4-开发环境搭建)
5. [第一个Java程序](#5-第一个java程序)
6. [包机制与import](#6-包机制与import)
7. [main方法详解](#7-main方法详解)
8. [Java注释规范](#8-java注释规范)
9. [开发工具与IDE](#9-开发工具与ide)
10. [总结清单](#10-总结清单)

---

## 1. Java语言概述

### 1.1 Java的历史与演进

| 年份 | 版本 | 里程碑特性 |
|------|------|-----------|
| 1995 | JDK 1.0 | Sun公司发布，口号"Write Once, Run Anywhere" |
| 1998 | JDK 1.2 | 引入集合框架、Swing GUI |
| 2004 | JDK 5 | 泛型、枚举、自动装箱/拆箱、注解、增强for循环 |
| 2011 | JDK 7 | try-with-resources、NIO.2、Fork/Join框架 |
| 2014 | JDK 8 | Lambda表达式、Stream API、Optional、新日期API |
| 2017 | JDK 9 | 模块化系统（JPMS）、JShell交互式编程 |
| 2018 | JDK 11 | LTS版本，HttpClient标准化、ZGC实验性 |
| 2021 | JDK 17 | LTS版本，密封类、模式匹配预览、强封装JDK内部API |
| 2024 | JDK 21 | LTS版本，虚拟线程、Record模式、字符串模板 |

> 💡 Oracle宣布JDK每6个月发布一个新版本，建议生产环境使用**LTS版本**（JDK 8、11、17、21）。

### 1.2 Java语言核心特性

| 特性 | 说明 |
|------|------|
| **面向对象** | Java是纯面向对象语言，一切皆对象（除基本类型外） |
| **跨平台性** | 通过JVM实现"一次编写，到处运行" |
| **自动内存管理** | 垃圾回收器（GC）自动管理堆内存分配与回收 |
| **多线程支持** | 内置线程模型，JDK 21引入虚拟线程 |
| **丰富的标准库** | JDK内置大量成熟API覆盖IO、网络、数据结构等场景 |
| **强类型安全** | 编译期类型检查，避免运行时类型错误 |
| **异常处理机制** | 受检异常与运行时异常的完整体系 |
| **安全性** | 字节码校验、安全管理器、沙箱机制 |

### 1.3 Java的技术生态定位

```text
                      ┌───────────────────┐
                      │   Java 语言规范     │
                      └────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
      ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
      │  Java SE     │ │  Java EE     │ │  Java ME     │
      │（标准版）      │ │（企业版）      │ │（微型版）      │
      │ 桌面应用基础   │ │ Web/企业级应用  │ │ 嵌入式/IoT    │
      │ JDK内置核心API │ │ Spring/微服务  │ │ 移动设备      │
      └──────────────┘ └──────────────┘ └──────────────┘
```

> 🎯 Java是后端开发的主流语言，理解Java核心机制（JVM、集合、多线程）是后端面试的必备项。学C++理解底层，学Java理解工程化。

### 1.4 Java后端核心包速览

JDK内置的核心包覆盖了后端开发80%以上的基础场景，后续框架（Spring、MyBatis）均基于这些核心API封装。

| 包名 | 自动导入 | 核心功能 | 最常用类 |
|------|:--------:|----------|----------|
| `java.lang` | ✅ | 语言基础 | `String`、`Integer`、`Object`、`Thread`、`System` |
| `java.util` | ❌ | 集合+日期+工具 | `ArrayList`、`HashMap`、`LocalDateTime`、`Optional` |
| `java.io` | ❌ | 传统文件IO | `File`、`BufferedReader`、`BufferedWriter` |
| `java.nio` | ❌ | 非阻塞IO | `ByteBuffer`、`FileChannel` |
| `java.sql` | ❌ | 数据库操作 | `Connection`、`PreparedStatement`、`ResultSet` |
| `java.net` | ❌ | 网络通信 | `Socket`、`ServerSocket`、`URL` |

> ⚠️ `java.lang` 是唯一被JVM自动导入的包，使用其中的类无需手动 `import`。

---

## 2. JDK/JRE/JVM关系

### 2.1 三者的定义

```text
  ┌───────────────────────────────────┐
  │             JDK                   │
  │  ┌─────────────────────────────┐  │
  │  │            JRE              │  │
  │  │  ┌───────────────────────┐  │  │
  │  │  │         JVM           │  │  │
  │  │  │  (字节码执行引擎)       │  │  │
  │  │  └───────────────────────┘  │  │
  │  │  核心类库 + 运行时环境       │  │
  │  └─────────────────────────────┘  │
  │  开发工具：javac、jar、jdoc等      │
  └───────────────────────────────────┘
```

| 组件 | 全称 | 作用 | 包含关系 |
|------|------|------|----------|
| **JDK** | Java Development Kit | Java开发工具包，提供编译、调试、运行全套工具 | JDK = JRE + 开发工具 |
| **JRE** | Java Runtime Environment | Java运行时环境，为已编译程序提供运行条件 | JRE = JVM + 核心类库 |
| **JVM** | Java Virtual Machine | Java虚拟机，加载并执行字节码文件 | JVM是JRE的核心组件 |

### 2.2 JDK包含的常用开发工具

| 工具 | 命令 | 用途 |
|------|------|------|
| 编译器 | `javac` | 将 `.java` 源文件编译为 `.class` 字节码 |
| 解释器 | `java` | 启动JVM并执行字节码文件 |
| 归档工具 | `jar` | 打包类文件为 JAR 压缩包 |
| 文档工具 | `javadoc` | 从源码注释生成 API 文档 |
| 反汇编器 | `javap` | 反编译 `.class` 文件查看字节码 |
| 调试器 | `jdb` | 命令行调试工具 |
| 监控工具 | `jvisualvm` | 可视化JVM监控（JDK 6-8） |
| 打包工具 | `jlink` | 自定义JRE镜像（JDK 9+） |

### 2.3 JVM的核心价值

| 维度 | 说明 |
|------|------|
| **定义** | 运行Java字节码（`.class`）的虚拟计算机 |
| **核心作用** | 加载执行字节码、管理内存（分配/回收）、GC机制、解释/编译执行 |
| **关键价值** | 屏蔽操作系统和硬件差异，实现跨平台 |
| **主流实现** | HotSpot VM（Oracle JDK默认）、OpenJ9（Eclipse）、GraalVM |

```bash
# 查看当前JDK版本和JVM信息
java -version

# 输出示例
# openjdk version "21" 2024-09-17 LTS
# OpenJDK Runtime Environment (build 21+35)
# OpenJDK 64-Bit Server VM (build 21+35, mixed mode, sharing)
```

> ⚠️ **面试高频题**：JDK、JRE、JVM三者的区别与联系 —— JDK包含JRE，JRE包含JVM。写代码用JDK，跑程序用JRE，跨平台靠JVM。

---

## 3. Java程序编译与运行机制

### 3.1 编译与执行流程

```text
.java 源文件
    │
    ▼  javac 编译（前端编译器）
.class 字节码文件
    │
    ▼  类加载子系统（ClassLoader）
运行时数据区（内存结构）
    │
    ├──▶ 解释器：逐行解释执行（启动快，执行慢）
    │
    └──▶ JIT编译器：热点代码编译为机器码（启动慢，执行快）
            │
            ▼
     操作系统 & 硬件
```

**关键步骤**：

1. **编写**：创建 `.java` 源文件
2. **编译**：`javac` 将 `.java` 编译为 `.class` 字节码（平台无关的中间码）
3. **加载**：类加载器将 `.class` 加载到JVM运行时数据区
4. **执行**：解释器逐行执行 + JIT编译器将热点代码编译为本地机器码
5. **跨平台**：不同平台只需安装对应版本的JRE，同一份 `.class` 文件无需修改

### 3.2 字节码与跨平台原理

| 概念 | 说明 |
|------|------|
| **字节码** | 扩展名为 `.class` 的中间代码，与平台无关 |
| **跨平台原理** | Java源码→字节码（通用）→JVM翻译为特定平台机器码 |
| **与直接编译对比** | C/C++直接编译为平台机器码，不可跨平台；Java通过JVM间接实现跨平台 |

```bash
# 字节码反编译示例
javap -c HelloWorld.class

# 输出类似（简化版）：
# Compiled from "HelloWorld.java"
# public class HelloWorld {
#   public HelloWorld();
#     Code:
#        0: aload_0
#        1: invokespecial #1     // Method java/lang/Object."<init>":()V
#        4: return
#
#   public static void main(java.lang.String[]);
#     Code:
#        0: getstatic     #7     // Field java/lang/System.out
#        3: ldc           #13    // String "Hello, World!"
#        5: invokevirtual #15    // Method java/io/PrintStream.println
#        8: return
# }
```

### 3.3 解释执行与JIT编译

| 模式 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **解释执行** | 逐行读取字节码并翻译执行 | 启动快 | 执行慢 |
| **JIT编译** | 将热点代码（HotSpot）编译为本地机器码 | 执行快 | 启动慢，占用内存 |
| **AOT编译** | 提前编译为本地代码（GraalVM） | 冷启动极快 | 动态特性受限 |

> 💡 HotSpot VM会根据代码执行频率自动判断"热点"，触发JIT编译。JDK 8默认采用**分层编译**（C1+C2编译器），兼顾启动速度与峰值性能。

### 3.4 编译与运行命令

```bash
# 基本编译（无包名）
javac HelloWorld.java

# 基本运行（无包名，不加.class后缀）
java HelloWorld

# 带包名的编译（-d指定输出目录，自动创建包目录结构）
javac -d . com/example/demo/HelloWorld.java

# 带包名的运行（使用全限定类名）
java com.example.demo.HelloWorld

# 带classpath的编译
javac -cp lib/*:. HelloWorld.java

# 查看编译选项帮助
javac -help
```

### 3.5 类加载机制简介（5阶段）

```text
加载 ──▶ 验证 ──▶ 准备 ──▶ 解析 ──▶ 初始化
├── 查找并加载     ├── 校验     ├── 分配内存   ├── 符号引用   ├── 执行
│   类的二进制数据  │   字节码    │   并赋默认值  │   转直接引用  │   <clinit>
│   进JVM          │   安全性    │              │              │
```

| 阶段 | 说明 |
|------|------|
| **加载** | 通过类全限定名获取二进制字节流，在堆中生成Class对象 |
| **验证** | 确保字节码符合JVM规范，不危害JVM安全 |
| **准备** | 为静态变量分配内存并赋初始值（如 `static int a = 10` 在此阶段赋值为0） |
| **解析** | 将常量池的符号引用替换为直接引用 |
| **初始化** | 执行静态代码块和静态变量赋值操作 |

> ⚠️ **双亲委派模型**：类加载器收到加载请求时，先委派给父类加载器，只有当父类加载器无法加载时，才由自己尝试加载。这种机制避免核心类被篡改。

---

## 4. 开发环境搭建

### 4.1 JDK安装与版本选择

**版本选择建议**：

| 场景 | 推荐版本 | 说明 |
|------|----------|------|
| 新项目生产环境 | **JDK 21 LTS** | 最新LTS，包含虚拟线程等重大更新 |
| 现有企业项目 | **JDK 8 / 11 / 17 LTS** | 视项目原有JDK版本而定 |
| 个人学习 | **JDK 21 LTS** | 学习新特性，向下兼容 |
| 嵌入式/云原生 | **GraalVM** | 支持AOT编译，启动快、内存低 |

### 4.2 Oracle JDK vs OpenJDK

| 对比维度 | Oracle JDK | OpenJDK |
|----------|------------|---------|
| 许可证 | Oracle BCL协议（部分场景收费） | GPL v2 + Classpath Exception（免费） |
| 发布方 | Oracle公司 | Oracle开源社区 |
| 功能差异 | 基本一致（JDK 11起功能对齐） | 基本一致 |
| 商业支持 | Oracle官方提供 | 社区或第三方（Red Hat、Azul等） |
| 适用场景 | 有Oracle商业许可的企业 | 大多数开发和生产场景 |

> 💡 自JDK 11起，Oracle JDK与OpenJDK在功能上已无实质性差异。推荐使用 **OpenJDK** 或 **Adoptium（原AdoptOpenJDK）** 发行版。

### 4.3 环境变量配置

| 环境变量 | 作用 | 示例值 |
|----------|------|--------|
| `JAVA_HOME` | JDK安装根目录，供其他框架（Maven、Tomcat）引用 | `C:\Program Files\Java\jdk-21` |
| `PATH` | 添加 `%JAVA_HOME%\bin` 使 `java`/`javac` 命令全局可用 | `%JAVA_HOME%\bin` |
| `CLASSPATH` | 指定类文件搜索路径（JDK 5+通常不配置） | `.;%JAVA_HOME%\lib\dt.jar` |

**Windows配置示例**：
```bash
# 设置 JAVA_HOME（PowerShell）
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Java\jdk-21', 'User')

# 添加到 PATH
$path = [System.Environment]::GetEnvironmentVariable('PATH', 'User')
$path += ';%JAVA_HOME%\bin'
[System.Environment]::SetEnvironmentVariable('PATH', $path, 'User')

# 验证安装
java -version
javac -version
```

**Linux/macOS配置示例**：
```bash
# 在 ~/.bashrc 或 ~/.zshrc 中添加
export JAVA_HOME=/usr/lib/jvm/jdk-21
export PATH=$JAVA_HOME/bin:$PATH

# 验证安装
java -version
javac -version
```

### 4.4 安装验证清单

```bash
# 1. 验证JDK版本
java -version
# 输出示例：java version "21" 2024-09-17 LTS

# 2. 验证编译器
javac -version
# 输出示例：javac 21

# 3. 验证JRE（如单独安装）
java -version

# 4. 查看安装路径
where java        # Windows
which java        # Linux/macOS

# 5. 查看JAVA_HOME（确定框架可识别）
echo %JAVA_HOME%  # Windows
echo $JAVA_HOME   # Linux/macOS
```

---

## 5. 第一个Java程序

### 5.1 Hello World

```java
/**
 * 第一个Java程序
 * 文件名必须与public类名一致：HelloWorld.java
 */
public class HelloWorld {

    // 程序入口：main方法
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        // 创建对象并调用方法
        HelloWorld hw = new HelloWorld();
        hw.greet("Java");
    }

    /** 实例方法 */
    public void greet(String name) {
        System.out.println("Hello, " + name + "!");
    }
}
```

### 5.2 编译与运行步骤

```bash
# Step 1: 编写代码保存为 HelloWorld.java

# Step 2: 编译为字节码
javac HelloWorld.java
# 生成 HelloWorld.class

# Step 3: 运行（不加 .class 后缀）
java HelloWorld

# 预期输出：
# Hello, World!
# Hello, Java!

# Step 4: 查看生成的字节码文件
dir HelloWorld.class   # Windows
ls -la HelloWorld.class # Linux/macOS
```

### 5.3 程序结构图解

```java
// ===== 1. 包声明 =====
package com.example.demo;         // 必须位于文件第一行（非注释）

// ===== 2. 导入语句 =====
import java.util.Scanner;         // 导入其他包的类

// ===== 3. 类定义 =====
public class HelloWorld {         // public类名必须与文件名一致

    // ===== 4. 成员变量（字段） =====
    private String message;       // 实例变量
    private static int count;     // 类变量（静态变量）

    // ===== 5. 构造方法 =====
    public HelloWorld() {         // 无参构造
        this.message = "Hello";
    }

    // ===== 6. 主方法（入口） =====
    public static void main(String[] args) {
        // 局部变量
        String greeting = "Hello, World!";
        System.out.println(greeting);
    }

    // ===== 7. 实例方法 =====
    public void setMessage(String msg) {
        this.message = msg;
    }

    // ===== 8. 静态方法 =====
    public static void printCount() {
        System.out.println(count);
    }
}
```

### 5.4 文件命名与编译规则

| 规则 | 说明 |
|------|------|
| **public类** | 文件名必须与public类名一致（如 `HelloWorld.java` 对应 `public class HelloWorld`） |
| **非public类** | 文件名可与类名不同，但每个文件最多一个public类 |
| **大小写敏感** | Java文件名和类名都区分大小写 |
| **无public类** | 文件名可任意，但不推荐 |
| **编译产物** | 每个类生成一个 `.class` 文件（内部类也会生成独立的 `.class`） |

```java
// HelloWorld.java 中可以定义多个类
public class HelloWorld {
    public static void main(String[] args) { }
}

class Helper {           // 包级可见的辅助类
    void assist() { }
}

// 编译后生成：HelloWorld.class + Helper.class
```

---

## 6. 包机制与import

### 6.1 包（Package）的作用

| 作用 | 说明 |
|------|------|
| **命名空间隔离** | 避免类名冲突（同名类在不同包中互不干扰） |
| **组织代码** | 按模块/功能分层管理，提升可维护性 |
| **控制访问权限** | default（包级）可见性仅同包可访问 |
| **目录结构映射** | 包名与文件系统目录一一对应 |

### 6.2 包命名规范

| 规范 | 示例 | 说明 |
|------|------|------|
| **域名倒置** | `com.alibaba.project.module` | 以组织域名的倒序开头 |
| **全小写** | `com.company.project` | 包名全部小写 |
| **点分隔** | `com.alibaba.project.user` | 每个点对应一个目录层级 |
| **部门/项目名** | `com.company.project.module` | 逐级细化 |

```text
# 推荐包名结构
com.alibaba.project.user        # 用户模块
com.alibaba.project.order       # 订单模块
com.alibaba.project.common      # 公共工具
com.alibaba.project.common.util  # 工具子包

# 目录结构
com/
  alibaba/
    project/
      user/
        UserService.java
      order/
        OrderService.java
      common/
        util/
          StringUtils.java
```

### 6.3 包声明与import

```java
package com.example.demo;  // 包声明：必须位于文件第一行（非注释行之前）

import java.util.Scanner;  // 导入单个类
import java.util.*;        // 导入java.util包下所有类（不递归子包）
import java.io.*;          // 导入java.io包下所有类

// 静态导入（JDK 5+）：导入静态方法和常量，直接使用
import static java.lang.Math.*;
import static java.util.Collections.sort;

public class Demo {
    public static void main(String[] args) {
        double result = sqrt(pow(3, 2) + pow(4, 2));  // 直接使用Math的静态方法
        System.out.println(result);  // 5.0
    }
}
```

### 6.4 全限定类名（FQN）

完全限定名（Fully Qualified Name） = 包名 + 类名。当两个包有同名类时，使用全限定类名避免冲突：

```java
// 同时使用 java.util.Date 和 java.sql.Date
java.util.Date utilDate = new java.util.Date();
java.sql.Date sqlDate = java.sql.Date.valueOf("2026-07-26");
```

> 💡 一般优先用 `import` 简化代码，仅在冲突时使用全限定名。阿里巴巴规范推荐 **Import显式声明具体类**，避免使用 `import xxx.*` 通配符。

### 6.5 包与访问权限

| 访问修饰符 | 同包 | 不同包（子类） | 不同包（非子类） |
|------------|:----:|:--------------:|:----------------:|
| `private` | ❌ | ❌ | ❌ |
| `default`（无修饰符） | ✅ | ❌ | ❌ |
| `protected` | ✅ | ✅ | ❌ |
| `public` | ✅ | ✅ | ✅ |

---

## 7. main方法详解

### 7.1 main方法签名

```java
public static void main(String[] args) {
    // 程序入口
}
```

| 关键字 | 含义 | 解释 |
|--------|------|------|
| `public` | 访问权限公开 | JVM需要从外部调用main方法，必须公开 |
| `static` | 静态方法 | JVM无需创建对象即可直接调用main方法 |
| `void` | 无返回值 | main方法退出即进程结束，返回值没有意义 |
| `String[]` | 字符串数组参数 | 接收命令行传入的参数 |

### 7.2 命令行参数

```java
public class ArgsDemo {
    public static void main(String[] args) {
        System.out.println("接收参数个数: " + args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.println("args[" + i + "] = " + args[i]);
        }
    }
}
```

```bash
# 编译
javac ArgsDemo.java

# 运行时传入参数（以空格分隔）
java ArgsDemo hello world 123

# 输出：
# 接收参数个数: 3
# args[0] = hello
# args[1] = world
# args[2] = 123

# 参数中包含空格用引号包裹
java ArgsDemo "hello world" foo

# 输出：
# 接收参数个数: 2
# args[0] = hello world
# args[1] = foo
```

### 7.3 main方法变体

以下三种写法都合法，但标准写法是 `String[] args`：

```java
public static void main(String[] args)     // 标准写法
public static void main(String args[])     // C风格数组声明，不推荐
public static void main(String... args)    // 可变参数形式，不推荐
```

> ⚠️ `main` 方法签名中 `static` 和 `public` 的顺序可以互换，但习惯将 `public` 放在第一位。方法名必须为 `main`，JVM只识别固定签名的 `main` 方法。

### 7.4 main方法常见错误

| 错误写法 | 问题 | 正确写法 |
|----------|------|----------|
| `public void main(String[] args)` | 缺少 `static` | `public static void main(String[] args)` |
| `static void main(String[] args)` | 缺少 `public` | `public static void main(String[] args)` |
| `public static void main(String args)` | 参数类型错误 | `public static void main(String[] args)` |
| `public static void Main(String[] args)` | 首字母大写 | `public static void main(String[] args)` |
| 类中没有main方法 | 缺少入口 | 添加标准main方法 |

---

## 8. Java注释规范

### 8.1 三种注释方式

```java
// ===== 1. 单行注释 =====
// 用于简单说明，解释"为什么"而非"是什么"
// 将用户名转换为小写，因为数据库查询不区分大小写
String username = input.trim().toLowerCase();

// ===== 2. 多行注释 =====
/*
 * 多行注释用于较长的说明文字。
 * 每行开头的 * 不是必须的，但推荐使用以保持对齐。
 */

// ===== 3. Javadoc注释（重要） =====
/**
 * Javadoc注释用于生成HTML格式的API文档。
 * 使用 javadoc 工具可从源码提取为文档。
 *
 * @author Zhang San
 * @version 1.0
 * @since 2026-07-26
 */
public class Documentation {
    /**
     * 计算两数之和
     *
     * @param a 第一个加数
     * @param b 第二个加数
     * @return a + b 的计算结果
     * @throws IllegalArgumentException 当参数非法时抛出
     */
    public int add(int a, int b) {
        return a + b;
    }
}
```

### 8.2 Javadoc标签速查

| 标签 | 作用域 | 说明 |
|------|--------|------|
| `@author` | 类 | 作者（一个类可多个） |
| `@version` | 类 | 版本号 |
| `@since` | 类/方法 | 起始版本 |
| `@param` | 方法 | 参数说明 |
| `@return` | 方法 | 返回值说明 |
| `@throws` / `@exception` | 方法 | 异常说明 |
| `@see` | 类/方法 | 参考其他类或方法 |
| `@deprecated` | 类/方法 | 标记已废弃 |
| `{@code}` | 内联 | 代码片段（保留格式） |
| `{@link}` | 内联 | 链接到其他成员 |

### 8.3 企业级注释规范

```java
// ===== 良好注释 =====
// 跳过CSV第一行，因为它是表头而非数据
for (int i = 1; i < lines.size(); i++) {
    processLine(lines.get(i));
}

// ===== 无意义注释（应当避免） =====
// 加1
i = i + 1;

// ===== 阿里编程规约：类必须有Javadoc =====
/**
 * 用户服务——处理用户注册、登录、信息查询
 *
 * @author Zhang San
 * @since 2026-07-26
 */
public class UserService { }

// ===== 阿里编程规约：public方法必须有Javadoc =====
/**
 * 根据ID查询用户信息
 *
 * @param userId 用户ID，不能为空
 * @return 用户信息，不存在时返回 {@code Optional.empty()}
 * @throws IllegalArgumentException userId为null时抛出
 */
public Optional<User> findById(Long userId) { }
```

### 8.4 生成Javadoc文档

```bash
# 生成单个包的API文档
javadoc -d docs com.example.demo

# 生成全部源码的API文档
javadoc -encoding UTF-8 -charset UTF-8 -d docs src/**/*.java
```

---

## 9. 开发工具与IDE

### 9.1 IDE对比

| IDE | 特点 | 适用场景 |
|-----|------|----------|
| **IntelliJ IDEA** | 智能提示最强、重构功能丰富 | **推荐首选**，企业级开发 |
| **Eclipse** | 开源免费、插件生态丰富 | 老项目维护 |
| **VS Code** | 轻量、插件化、启动快 | 轻量开发、学习入门、多语言 |
| **NetBeans** | 官方IDE，开箱即用 | Swing桌面开发 |

### 9.2 推荐学习工具链

| 工具 | 用途 | 推荐项 |
|------|------|--------|
| **IDE** | 编写/调试代码 | IntelliJ IDEA Community版（免费） |
| **JDK** | 编译/运行环境 | OpenJDK 21 LTS（Adoptium发行版） |
| **构建工具** | 项目构建/依赖管理 | Maven（入门推荐）→ Gradle（进阶） |
| **版本控制** | 代码管理 | Git + GitHub/GitLab |
| **API文档** | 查阅标准库 | [docs.oracle.com](https://docs.oracle.com) 或离线JDK文档 |

### 9.3 IntelliJ IDEA基础设置

| 设置项 | 推荐值 | 说明 |
|--------|--------|------|
| JDK配置 | File → Project Structure → SDK | 指向JDK安装目录 |
| 编码 | File → Settings → File Encodings | **UTF-8**（全项目统一） |
| 缩进 | Settings → Code Style → Java | 4空格，禁用Tab |
| 自动导入 | Settings → Editor → Auto Import | 勾选Add unambiguous imports |
| 快捷键方案 | Settings → Keymap | Eclipse/VS Code/默认方案可选 |

### 9.4 使用javac命令行（无IDE）

即使使用IDE，也应当理解命令行编译流程：

```bash
# 1. 创建项目目录结构
mkdir -p MyProject/src/com/example/demo

# 2. 在 src/com/example/demo/ 下编写 HelloWorld.java

# 3. 编译（在项目根目录执行）
javac -d out src/com/example/demo/HelloWorld.java

# 4. 运行
java -cp out com.example.demo.HelloWorld

# 5. 打包为JAR
cd out
jar cf MyApp.jar com/
java -jar MyApp.jar    # 需要在MANIFEST.MF中指定Main-Class
```

---

## 10. 总结清单

### 10.1 本章核心要点

| 模块 | 必须掌握 | 理解即可 |
|------|----------|----------|
| Java概述 | Java的特点：面向对象、跨平台、自动GC | Java版本演进历史 |
| JDK/JRE/JVM | **三者关系与区别**（面试高频） | JVM各组件内部结构 |
| 编译运行 | `javac` + `java` 命令使用，编译流程 | AOT/JIT编译原理 |
| 环境搭建 | JDK安装、`JAVA_HOME`/`PATH`配置 | JDK发行版选择 |
| 第一个程序 | Hello World完整流程 | 命令行参数传递 |
| 包机制 | 包声明、`import`、命名规范 | 全限定类名使用场景 |
| main方法 | `public static void main(String[])`含义 | 各关键字的作用 |
| 注释规范 | 三种注释写法、Javadoc标签 | 生成API文档 |

### 10.2 常见陷阱

| 陷阱 | 说明 |
|------|------|
| **文件名与public类名不一致** | 编译报错，文件名必须与public类名完全一致 |
| **`java`命令带了 `.class` 后缀** | 运行时 `java HelloWorld.class` 会报错，应写 `java HelloWorld` |
| **`JAVA_HOME` 配置错误** | 路径含空格未用引号包裹，或路径指向JRE而非JDK |
| **包名与目录不匹配** | 包声明为 `com.example.demo` 但 `.java` 文件不在对应目录 |
| **忘记包声明** | 无包名的类无法被其他包引用，且 `default` 可见性受限 |
| **main方法参数类型错误** | 使用 `String[] args` 而不是 `String args` 或 `String[] arg` |
| **注释与代码不同步** | 修改代码后忘记更新注释，形成误导性文档 |

### 10.3 最佳实践

1. **IDE + 命令行结合学习**：用IDE提高效率，用命令行理解底层原理
2. **从第一个Hello World就养成包规范**：即使简单程序也使用 `com.example.demo` 包名
3. **每个文件一个public类**：保持代码组织清晰
4. **Javadoc从第一天开始**：对每个public方法写Javadoc，形成习惯
5. **理解编译错误**：编译错误是学习机会，仔细阅读错误信息比复制答案更有价值

### 10.4 学习建议与路线图

```text
学习步骤：
Step 1  ──▶ 完成本章"Hello World"并理解全部细节
Step 2  ──▶ 配置IDE并理解项目管理结构
Step 3  ──▶ 学习下一章《数据类型与变量》
                 │
                 ▼
进阶路线：通过命令行编译运行 → 过渡到IDE
         API文档查阅（JDK Doc）→ 独立解决问题
         练习简单程序 → 逐步增加复杂性
```

> 🎯 **总结**：Java概述与开发环境是Java学习的第一道门槛。理解JDK/JRE/JVM关系、掌握编译运行流程、熟练搭建开发环境、理解程序结构和包机制，是后续深入学习Java的坚实基础。**动手练习是最重要的** -- 不要只看教程，必须亲手写代码、编译、运行、调试。

---

> 📖 **推荐阅读**：《Java编程思想》（Thinking in Java）第1章"对象入门"和第2章"一切都是对象"；《Java程序设计与问题求解》第1章"计算机与Java引论"。
