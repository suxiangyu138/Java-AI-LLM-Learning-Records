# 01 — JVM 原理与内存模型

> **理解 JVM = 理解 Java 程序的"操作系统"**

| 版本 | 内容 |
|------|------|
| JDK 7 | PermGen, String Pool in Heap, G1 not default |
| JDK 8 | Metaspace replaces PermGen, String Pool deduplication |
| JDK 11 | ZGC experimental, Epsilon GC, Low-Pause GC improvements |
| JDK 17 | ZGC production, sealed classes, enhanced pseudo-random |
| JDK 21 | Virtual Threads, Record Patterns, ZGC generational mode |

---

## 目录

1. [JVM 架构总览](#1-jvm-架构总览)
2. [类加载机制](#2-类加载机制)
3. [类加载器详解](#3-类加载器详解)
4. [双亲委派模型](#4-双亲委派模型)
5. [运行时数据区](#5-运行时数据区)
6. [对象创建与内存布局](#6-对象创建与内存布局)
7. [对象访问定位](#7-对象访问定位)
8. [Java 内存模型 (JMM)](#8-java-内存模型-jmm)
9. [JDK 版本差异详解](#9-jdk-版本差异详解)
10. [常见 OOM 场景与分析](#10-常见-oom-场景与分析)
11. [JVM 调优参数速查](#11-jvm-调优参数速查)
12. [面试经典问题](#12-面试经典问题)

---

## 1. JVM 架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                     Java Source Code                        │
│                    (HelloWorld.java)                        │
└───────────────────────┬─────────────────────────────────────┘
                        │ javac 编译
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                   Bytecode (.class)                         │
│              Cafe Babe (Magic Number 0xCAFEBABE)             │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    JVM Runtime                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ ClassLoader   │  │ Runtime Data │  │ Execution Engine │  │
│  │ Subsystem    │──▶│    Areas    │──▶│ (Interpreter +   │  │
│  │ (Loading,    │  │              │  │  JIT Compiler)   │  │
│  │  Linking,    │  │ See §5 below │  │                  │  │
│  │  Init)       │  │              │  │  + GC (§02)      │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

JVM 是一个**规范**（由 JSR 定义），HotSpot 是其中最主流的实现。其他实现包括 OpenJ9 (IBM/Eclipse)、GraalVM、Zing (Azul)。

### 三个核心子系统

| 子系统 | 职责 | 对应章节 |
|--------|------|---------|
| ClassLoader Subsystem | 加载 .class 文件到内存 | §2-4 |
| Runtime Data Areas | 存储程序运行时数据 | §5 |
| Execution Engine | 执行字节码（解释 + JIT 编译 + GC） | §1, §02 |

---

## 2. 类加载机制

### 2.1 类加载的完整生命周期

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Loading  │───▶│ Linking  │───▶│Initial-  │───▶│  Using   │───▶│Unloading │
│ (加载)    │    │ (连接)   │    │ization   │    │ (使用)   │    │ (卸载)   │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
                     │
         ┌───────────┼───────────┐
         ▼           ▼           ▼
    Verification  Preparation  Resolution
    (验证)       (准备)       (解析)
```

**注意**：Resolution（解析）可以在 Initialization 之后才执行，这是 JVM 规范允许的**动态绑定**优化。

### 2.2 Loading（加载）

核心工作：
1. 通过类的全限定名获取二进制字节流（从 .class 文件、JAR、网络、动态代理生成等）
2. 将字节流转换为方法区的运行时数据结构
3. 在堆中生成 `java.lang.Class` 对象，作为方法区该类的入口

```java
// 触发类加载的几种方式
public class ClassLoadingDemo {
    public static void main(String[] args) throws Exception {
        // 方式1: 使用 Class.forName() —— 默认执行初始化
        Class<?> clazz1 = Class.forName("com.example.MyClass");

        // 方式2: 使用 ClassLoader.loadClass() —— 不执行初始化
        Class<?> clazz2 = ClassLoadingDemo.class.getClassLoader()
                .loadClass("com.example.MyClass");

        // 方式3: 使用 .class 字面量
        Class<?> clazz3 = MyClass.class;  // 不会触发 static 块

        // 方式4: 使用 Class.forName() 但不初始化
        Class<?> clazz4 = Class.forName("com.example.MyClass", false,
                Thread.currentThread().getContextClassLoader());
    }
}
```

**触发类初始化的 6 种主动引用（Initialization 阶段会执行）**：
1. `new` 关键字、读取/设置类的 static 字段、调用 static 方法
2. 反射调用 `Class.forName()`
3. 初始化子类时，如果父类未初始化则先触发父类
4. JVM 启动时指定的主类（含 `main()` 的类）
5. `java.lang.invoke.MethodHandle` 对应的类
6. 默认方法（JDK 8+）：当接口定义了默认方法时，实现类初始化前会触发接口初始化

**不会触发初始化的被动引用**：
```java
public class PassiveReferenceDemo {
    public static void main(String[] args) {
        // 1. 通过子类引用父类的 static 字段，不会触发子类初始化
        System.out.println(Sub.value);  // 只触发 Parent 的初始化

        // 2. 通过数组定义引用类
        Parent[] arr = new Parent[10];  // 触发 [Lcom.example.Parent; 类的初始化

        // 3. 访问常量（编译期常量传播）
        System.out.println(Sub.CONSTANT); // 不会触发任何初始化
    }
}

class Parent {
    static int value = 123;
    static { System.out.println("Parent init"); }
    static final String CONSTANT = "hello";
}

class Sub extends Parent {
    static { System.out.println("Sub init"); }
} // 输出：Parent init  123  （注意顺序）
```

### 2.3 Linking（连接）

#### 2.3.1 Verification（验证）
确保字节码符合 JVM 规范，不会危害 JVM 安全：
- 文件格式验证：Magic Number (0xCAFEBABE)、版本号
- 元数据验证：父类是否存在、是否有 final 修饰
- 字节码验证：操作数栈类型、跳转指令合法性（最复杂）
- 符号引用验证：引用的类/字段/方法是否可访问

> 可以通过 `-Xverify:none` 或 `-noverify` 关闭验证（JDK 13 起 deprecated）。

#### 2.3.2 Preparation（准备）
为 **static 字段**分配内存并设置**零值**（注意：非 final 的 static 字段此时为零值，而非代码中的初始值）。

```java
class PreparationDemo {
    // Preparation 阶段之后：a = 0, b = 10
    static int a = 5;        // 先设为 0，Initialization 阶段设为 5
    static final int b = 10; // final static 在 Preparation 阶段直接设为 10
}
```

#### 2.3.3 Resolution（解析）
将常量池中的符号引用替换为直接引用。涉及：
- 类/接口解析
- 字段解析
- 方法解析
- 接口方法解析
- 方法类型/方法句柄解析（JDK 7+）

### 2.4 Initialization（初始化）

执行 `<clinit>()` 方法——由编译器自动收集所有 static 赋值动作和 static 块合并而成。

```java
public class InitOrderDemo {
    // 执行顺序：从上到下，父类先于子类
    static int x = 10;
    static {
        System.out.println("static block 1: x = " + x); // x = 10
        x = 20;
    }
    static int y = x * 2; // y = 40
    static {
        System.out.println("static block 2: x = " + x + ", y = " + y);
    }

    public static void main(String[] args) {
        System.out.println("main: x = " + x + ", y = " + y);
    }
}
```

**`<clinit>()` 的关键特性**：
- 由 JVM 保证线程安全（加锁同步）
- 父类的 `<clinit>()` 先于子类执行
- 接口的 `<clinit>()` 不需要先于实现类
- 可通过 `-XX:+TraceClassLoading` 观察类加载过程

---

## 3. 类加载器详解

### 3.1 四种加载器层次

```
┌──────────────────────────────────────┐
│         Bootstrap ClassLoader        │
│   RT.jar / java.base (JDK 9+)        │
│   由 C++ 实现，null 表示              │
└────────────────┬─────────────────────┘
                 │
┌────────────────▼─────────────────────┐
│      Platform/Extension ClassLoader   │
│   JDK 8: 加载 jre/lib/ext/*.jar       │
│   JDK 9+: 加载 java.se, jdk.* 等模块  │
│   父加载器：Bootstrap (null)          │
└────────────────┬─────────────────────┘
                 │
┌────────────────▼─────────────────────┐
│         Application ClassLoader       │
│   加载 classpath (-cp) 下的类          │
│   父加载器：Platform ClassLoader       │
└────────────────┬─────────────────────┘
                 │
┌────────────────▼─────────────────────┐
│          Custom ClassLoader           │
│   用户自定义，打破双亲委派             │
└──────────────────────────────────────┘
```

```java
// 查看类加载器层次
public class ClassLoaderHierarchy {
    public static void main(String[] args) {
        ClassLoader app = ClassLoaderDemo.class.getClassLoader();
        System.out.println("Application ClassLoader: " + app);
        // ↑ jdk.internal.loader.ClassLoaders$AppClassLoader

        ClassLoader platform = app.getParent();
        System.out.println("Platform ClassLoader: " + platform);
        // ↑ jdk.internal.loader.ClassLoaders$PlatformClassLoader

        ClassLoader bootstrap = platform.getParent();
        System.out.println("Bootstrap ClassLoader: " + bootstrap);
        // ↑ null (C++ 实现)
    }
}
```

### 3.2 JDK 9+ 模块化对类加载器的影响

| 特性 | JDK 8 | JDK 9+ |
|------|-------|--------|
| 顶层类加载器 | Bootstrap (null) | Bootstrap (null) |
| 第二层 | Extension | Platform |
| 第三层 | Application | Application |
| 核心 API | rt.jar | 模块化 java.base, java.sql 等 |
| 加载方式 | 从 JAR 文件 | 从模块（jimage 格式） |

---

## 4. 双亲委派模型

### 4.1 工作机制

```java
// ClassLoader 的 loadClass 方法（简化版）
protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
        // 1. 检查类是否已经加载
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                // 2. 委派给父加载器
                if (parent != null) {
                    c = parent.loadClass(name, false);
                } else {
                    // 3. 没有父加载器，由 Bootstrap 尝试
                    c = findBootstrapClassOrNull(name);
                }
            } catch (ClassNotFoundException e) {
                // 父加载器找不到，忽略
            }
            if (c == null) {
                // 4. 自己尝试加载
                c = findClass(name);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }
}
```

### 4.2 为什么需要双亲委派？

1. **安全性**：防止核心 API 被篡改。例如自己写的 `java.lang.String` 永远会被 Bootstrap 加载，不会覆盖 JDK 核心类
2. **避免重复加载**：同一个类被不同加载器加载会产生不同的 Class 对象，导致 `instanceof` 失败

```java
// 尝试破坏核心类会失败
public class EvilString {
    public static void main(String[] args) {
        // Error: 找不到或无法加载主类 EvilString
        // 因为 j.l.String 已经由 Bootstrap 加载
        String s = new String();
    }
}
```

### 4.3 如何打破双亲委派？

#### 场景1: 自定义 ClassLoader 重写 loadClass

```java
/**
 * 打破双亲委派：直接重写 loadClass，不委派给父加载器
 * 使用场景：热部署、OSGi、框架隔离
 */
public class BreakParentDelegationClassLoader extends ClassLoader {

    private final String classPath;

    public BreakParentDelegationClassLoader(String classPath) {
        this.classPath = classPath;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // 核心类仍然由 Bootstrap 加载（否则 JVM 会崩溃）
        if (name.startsWith("java.") || name.startsWith("javax.")) {
            return super.loadClass(name);
        }

        try {
            // 先自己加载
            return findClass(name);
        } catch (ClassNotFoundException e) {
            // 自己加载失败，再委派给父加载器
            return super.loadClass(name);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String fileName = classPath + "/" + name.replace('.', '/') + ".class";
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(fileName));
            // 定义类
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name);
        }
    }
}
```

#### 场景2: Tomcat 的类加载模型

```
Tomcat 的类加载器层次（打破双亲委派）：
  Bootstrap
    └── System (Application)
          └── Common (common.loader 目录)
                ├── Catalina (catalina.loader 目录)
                │     └── Shared (shared.loader 目录)
                │           ├── Webapp1 (WEB-INF/lib, WEB-INF/classes)
                │           └── Webapp2 (WEB-INF/lib, WEB-INF/classes)
```

Tomcat 的做法：
1. **先尝试自己加载**（打破双亲委派），WEB-INF 中的类优先
2. 自己找不到再委派给父加载器
3. **每个 Webapp 有独立的 ClassLoader**，实现应用隔离
4. 对于 JSP 文件，使用额外的 JasperLoader 实现热加载

#### 场景3: JDBC 驱动加载（线程上下文类加载器）

```java
// JDBC 4.0 的 ServiceLoader 机制
public class JdbcDriverDemo {
    public static void main(String[] args) throws Exception {
        // DriverManager 由 Bootstrap 加载
        // 实际驱动（如 com.mysql.cj.jdbc.Driver）在 classpath 中
        // 需要线程上下文类加载器来委派
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/test");
        // ^ 底层会调用 Thread.currentThread().getContextClassLoader()
    }
}
```

---

## 5. 运行时数据区

### 5.1 总览

```
┌─────────────────────────────────────────────────────────────┐
│                     JVM Runtime Data Areas                   │
│                                                              │
│  ┌─────────────────┐  ┌──────────────────┐                  │
│  │   Thread 共享   │  │   Thread 私有    │                  │
│  │                 │  │                  │                  │
│  │ ┌─────────────┐ │  │ ┌──────────────┐ │                  │
│  │ │   Heap      │ │  │ │ Program      │ │                  │
│  │ │ (堆)        │ │  │ │ Counter      │ │                  │
│  │ │             │ │  │ │ (程序计数器) │ │                  │
│  │ │  Young Gen  │ │  │ └──────────────┘ │                  │
│  │ │    Eden     │ │  │ ┌──────────────┐ │                  │
│  │ │    S0/S1    │ │  │ │ JVM Stack   │ │                  │
│  │ │  Old Gen    │ │  │ │ (虚拟机栈)   │ │                  │
│  │ │             │ │  │ └──────────────┘ │                  │
│  │ └─────────────┘ │  │ ┌──────────────┐ │                  │
│  │ ┌─────────────┐ │  │ │ Native       │ │                  │
│  │ │ Method Area  │ │  │ │ Method Stack │ │                  │
│  │ │ (方法区)     │ │  │ │ (本地方法栈) │ │                  │
│  │ │ Metaspace   │ │  │ └──────────────┘ │                  │
│  │ │ (JDK 8+)    │ │  │                  │                  │
│  │ └─────────────┘ │  │                  │                  │
│  └─────────────────┘  └──────────────────┘                  │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Program Counter Register (程序计数器)

- **线程私有**，当前线程所执行的字节码行号指示器
- 如果执行的是 Java 方法，记录字节码指令地址
- 如果执行的是 Native 方法，值为 undefined
- **不会发生 OOM**（唯一一个没有 OOM 的区域）

### 5.3 JVM Stack (虚拟机栈)

每个线程创建一个 JVM Stack，内部包含多个 **Stack Frame（栈帧）**。

```
JVM Stack (线程)
  ┌──────────────────────┐
  │ Stack Frame (方法A)   │ ← 当前正在执行
  │   Local Variable     │
  │   Table              │
  │   Operand Stack      │
  │   Dynamic Linking    │
  │   Return Address     │
  ├──────────────────────┤
  │ Stack Frame (方法B)   │
  │   ...                │
  ├──────────────────────┤
  │ Stack Frame (方法C)   │
  │   ...                │
  └──────────────────────┘
```

每个 Stack Frame 包含：

#### 5.3.1 Local Variable Table (局部变量表)

- 存储方法参数和局部变量
- 以 **Slot** 为单位（4 字节），long/double 占 2 个 Slot
- 实例方法（非 static）的索引 0 存 `this`

```java
public class SlotDemo {
    // 局部变量表的大小在编译期确定
    public int calc(int a, int b) {
        // this  → slot 0
        // a     → slot 1
        // b     → slot 2
        int sum = a + b;   // sum → slot 3
        return sum;
    }

    // Slot 复用优化
    public void reuse() {
        int x = 1;        // slot 1
        // x 的作用域结束了
        int y = 2;        // slot 1 (复用 x 的 slot)
    }
}
```

#### 5.3.2 Operand Stack (操作数栈)

字节码指令的操作数存放位置，遵循**后进先出**原则。

```java
public class OperandStackDemo {
    public int sum() {
        int a = 1;
        int b = 2;
        return a + b;
    }
    // 对应的字节码：
    // 0: iconst_1        // 将常量 1 压入操作数栈
    // 1: istore_1        // 将栈顶弹出存入局部变量表 slot 1
    // 2: iconst_2        // 将常量 2 压入操作数栈
    // 3: istore_2        // 弹出存入 slot 2
    // 4: iload_1         // 从 slot 1 加载到操作数栈
    // 5: iload_2         // 从 slot 2 加载到操作数栈
    // 6: iadd            // 弹出两个值相加，结果压栈
    // 7: ireturn         // 返回栈顶值
}
```

#### 5.3.3 Dynamic Linking (动态链接)

每个栈帧包含指向运行时常量池中该方法的引用，支持**运行时动态绑定**。

```java
class DynamicLinkingDemo {
    static abstract class Animal {
        abstract void sound();
    }
    static class Dog extends Animal {
        void sound() { System.out.println("Woof"); }
    }
    static class Cat extends Animal {
        void sound() { System.out.println("Meow"); }
    }

    public static void main(String[] args) {
        // invokevirtual 指令：运行时根据实际类型确定方法
        Animal a = new Dog();
        a.sound();  // 输出 "Woof" —— 动态绑定

        Animal b = new Cat();
        b.sound();  // 输出 "Meow" —— 同一个指令但行为不同
    }
}
```

### 5.4 Native Method Stack (本地方法栈)

为 JVM 使用的 **Native 方法**（C/C++ 实现）服务。HotSpot 将 Native Method Stack 和 JVM Stack 合并实现。

**可能导致 StackOverflowError**：
```java
public class StackOverflowDemo {
    private static int depth = 0;

    public static void main(String[] args) {
        try {
            recursive();
        } catch (StackOverflowError e) {
            System.out.println("栈深度: " + depth);
        }
    }
    static void recursive() {
        depth++;
        recursive();
    }
    // 默认栈大小约 1MB，递归深度通常约 10000-20000
}
```

```bash
# 调整栈大小
java -Xss256k StackOverflowDemo   # 减小栈，递归深度降低
java -Xss2m StackOverflowDemo     # 增大栈，递归深度增加
```

### 5.5 Heap (堆)

**所有线程共享**，存储对象实例和数组。JVM 管理的最大一块内存。

```
Heap Memory Layout
┌─────────────────────────────────────────────────────────────┐
│                        Heap                                 │
│  ┌───────────────────────┬──────────────────────────────┐   │
│  │      Young Gen        │          Old Gen              │   │
│  │         (新生代)      │           (老年代)             │   │
│  │  ┌────┬────┬────┐    │                               │   │
│  │  │Eden│ S0 │ S1 │    │                               │   │
│  │  └────┴────┴────┘    │                               │   │
│  └───────────────────────┴──────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

| 区域 | 描述 | 比例（默认） |
|------|------|-------------|
| Young Gen | 新对象分配 | 1/3 堆 (-XX:NewRatio=2 → 老:新=2:1) |
| Eden | 大多数新对象分配于此 | 8/10 新生代 (-XX:SurvivorRatio=8) |
| S0 / S1 | 存活对象交换区 | 各 1/10 新生代 |
| Old Gen | 大对象、长期存活对象 | 2/3 堆 |
| String Pool (JDK 7+) | 字符串常量池 | 堆内 |

```java
// 查看堆使用情况
public class HeapInfoDemo {
    public static void main(String[] args) {
        // 获取 JVM 内存信息
        Runtime rt = Runtime.getRuntime();
        System.out.println("总内存: " + rt.totalMemory() / 1024 / 1024 + "MB");
        System.out.println("空闲内存: " + rt.freeMemory() / 1024 / 1024 + "MB");
        System.out.println("最大内存: " + rt.maxMemory() / 1024 / 1024 + "MB");

        // 使用 ManagementFactory（更详细）
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        System.out.println("Heap Init: " + heapUsage.getInit() / 1024 / 1024 + "MB");
        System.out.println("Heap Used: " + heapUsage.getUsed() / 1024 / 1024 + "MB");
        System.out.println("Heap Max: " + heapUsage.getMax() / 1024 / 1024 + "MB");
    }
}
```

### 5.6 Method Area (方法区)

存储类结构信息、常量、static 变量、JIT 编译后的代码。

| Java 版本 | 实现 | 变化 |
|-----------|------|------|
| JDK 6- | PermGen (永久代) | -XX:PermSize / -XX:MaxPermSize |
| JDK 7 | PermGen (但 String Pool 移入堆) | 逐步移除永久代 |
| JDK 8+ | Metaspace (元空间) | -XX:MetaspaceSize / -XX:MaxMetaspaceSize |
| JDK 8+ | 使用本地内存（而非 JVM 堆） | 默认无上限（取决于 OS） |

```
Metaspace 与 PermGen 对比
            PermGen                     Metaspace
  ┌─────────────────────┐    ┌─────────────────────────────┐
  │     JVM Heap 内      │    │       本地内存 (Native)      │
  │    大小固定           │    │       可动态扩展              │
  │  -XX:MaxPermSize     │    │  -XX:MaxMetaspaceSize        │
  │    默认 64M-82M      │    │    默认无上限                 │
  │    常量池 (含 String) │    │    常量池在堆                 │
  │    容易 OOM (PermGen) │    │    减少 OOM 概率             │
  └─────────────────────┘    └─────────────────────────────┘
```

---

## 6. 对象创建与内存布局

### 6.1 对象创建过程

```
Student s = new Student("Alice", 20);
```

```
1. 检查类是否已加载
   └─ new 指令在常量池中找到类的符号引用
   └─ 检查类是否已加载、解析、初始化
   └─ 未加载则执行类加载过程

2. 分配内存
   └─ 确定对象所需内存大小
   └─ 选择分配方式：
       ├─ 指针碰撞 (Bump-the-Pointer) — 规整 GC 后使用
       └─ 空闲列表 (Free List) — CMS 等使用
   └─ 线程本地分配缓冲 (TLAB) — 避免线程竞争

3. 初始化零值
   └─ 所有实例字段设为零值（0, null, false）
   └─ 保证字段在未赋值前可直接使用

4. 设置对象头
   └─ Mark Word: hash, GC age, lock info
   └─ Klass Pointer: 指向方法区的 Class 元数据

5. 执行 <init> 方法
   └─ 依次执行：父类构造器 → 实例变量初始化 → 构造代码块 → 构造器方法
```

#### TLAB 机制

```java
/**
 * TLAB (Thread Local Allocation Buffer)
 *
 * 每个线程在 Eden 区分配一小块私有缓冲区
 * 小对象直接在 TLAB 分配，无需同步
 * 大对象直接在 Old Gen 分配（-XX:PretenureSizeThreshold）
 */
public class TLABDemo {
    public static void main(String[] args) {
        // JVM 参数观察 TLAB：
        // -XX:+UseTLAB (默认开启)
        // -XX:+PrintTLAB (打印 TLAB 使用情况)
        // -XX:TLABSize=512k
        // -XX:TLABRefillWasteFraction=64

        byte[] smallObj = new byte[1024];       // TLAB 分配
        byte[] bigObj = new byte[1024 * 1024];  // 可能直接进入 Old Gen
    }
}
```

### 6.2 对象内存布局

```
HotSpot 对象布局 (以 64 位 JVM + 指针压缩为例)
┌─────────────────────────────────────────────────────────────┐
│                      Object Header                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Mark Word (8 bytes / 64 bits)                        │   │
│  │ [hash:25 | age:4 | biased:1 | lock:2 | 其他:32]      │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Klass Pointer (4 bytes with compressed OOPs)        │   │
│  │          (8 bytes without Compressed OOPs)          │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Instance Data (实例数据)                               │   │
│  │   - 父类字段 (按顺序)                                  │   │
│  │   - 子类字段 (按顺序)                                  │   │
│  │   - 顺序: double/long > int/float > char/short >     │   │
│  │            byte/boolean > reference (有利于对齐)       │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Padding (对齐填充)                                    │   │
│  │   对齐到 8 字节的整数倍                                │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

```java
// 使用 JOL (Java Object Layout) 工具查看对象内存布局
// 依赖: org.openjdk.jol:jol-core

import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;

public class JOLDemo {
    public static void main(String[] args) {
        MyObject obj = new MyObject();

        // 打印对象内部布局
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());

        // 打印对象总大小（含引用对象）
        System.out.println(GraphLayout.parseInstance(obj).totalSize());
    }
}

class MyObject {
    int id;           // 4 bytes
    long timestamp;   // 8 bytes
    String name;      // 4 bytes (compressed OOP)
    boolean active;   // 1 byte
    // 填充后对齐到 24 bytes（对象头 12 + 数据 17 + 填充 3 = 32? 实际需计算）
}
```

#### Mark Word 详解

64 位 Mark Word 在不同锁状态下的结构：

| 锁状态 | 25 bit | 31 bit | 1 bit | 4 bit | 1 bit (biased) | 2 bit (lock) |
|--------|--------|--------|-------|-------|----------------|--------------|
| 无锁 | unused | hashCode | unused | age | 0 | 01 |
| 偏向锁 | threadID (54) | epoch (2) | age | 1 | 01 |
| 轻量级锁 | 指向 Lock Record 的指针 (62) | | | | 00 |
| 重量级锁 | 指向 Monitor 的指针 (62) | | | | 10 |
| GC 标记 | 空 (62) | | | | 11 |

### 6.3 指针压缩 (Compressed OOPs)

```bash
-XX:+UseCompressedOops    # 默认开启（堆 < 32GB）
-XX:-UseCompressedOops    # 关闭后 Klass Pointer 从 4 字节变 8 字节
```

64 位 JVM 开启指针压缩后：
- 对象引用从 8 bytes 变为 4 bytes
- Klass Pointer 从 8 bytes 变为 4 bytes
- 减少内存占用，提升缓存命中率
- 堆 > 32GB 时自动关闭（地址空间不足 32 位表示）

---

## 7. 对象访问定位

两种方式：**句柄 (Handle)** vs **直接指针 (Direct Pointer)**

```
句柄访问:
  Stack              Heap                  Method Area
  ┌──────┐         ┌──────────┐          ┌──────────┐
  │ ref  │────────▶│ Handle   │─────────▶│ Klass    │
  │      │         │ Pool     │          │ Metadata │
  │      │         │┌────────┐│          └──────────┘
  │      │         ││对象指针 ││─────▶┌────────────┐
  │      │         ││类型指针 ││      │  Instance  │
  │      │         │└────────┘│      │  数据 (堆)  │
  └──────┘         └──────────┘      └────────────┘

直接指针 (HotSpot 使用):
  Stack              Heap
  ┌──────┐         ┌──────────────────────┐
  │ ref  │────────▶│ Mark Word            │
  │      │         │ Klass Pointer ───────┼──▶ Method Area
  │      │         │ Instance Data        │
  └──────┘         └──────────────────────┘
```

| 方式 | 优点 | 缺点 | 使用方 |
|------|------|------|--------|
| 句柄 | GC 移动对象时只需更新句柄中的指针，引用本身不变 | 每次访问需两次指针定位，速度慢 | IBM J9 |
| 直接指针 | 速度快（一次指针访问） | GC 移动对象时需要更新所有引用 | **HotSpot** |

---

## 8. Java 内存模型 (JMM)

### 8.1 为什么需要 JMM？

```
问题：多线程下的可见性问题
  CPU 核心 1          CPU 核心 2
  ┌────────┐         ┌────────┐
  │ 线程 A │         │ 线程 B │
  │ x = 1  │         │ y = x  │
  └───┬────┘         └───┬────┘
      │                  │
      ▼                  ▼
  ┌──────────┐      ┌──────────┐
  │ L1 Cache │      │ L1 Cache │
  │ x = 0    │      │ x = 0    │ ← 可能看不到线程 A 的修改
  └──────────┘      └──────────┘
      │                  │
      └────────┬─────────┘
               ▼
         ┌──────────┐
         │ Main Mem │
         │ x = ?    │
         └──────────┘
```

### 8.2 Happens-Before 规则

JMM 定义了一组偏序关系，保证前一个操作的结果对后一个操作可见：

| 规则 | 说明 | 示例 |
|------|------|------|
| 程序次序规则 | 同一个线程中，前面的操作 happens-before 后面的 | 代码顺序 |
| 监视器锁规则 | 解锁 happens-before 加锁 | synchronized |
| volatile 规则 | volatile 写 happens-before volatile 读 | volatile 变量 |
| 线程启动规则 | Thread.start() happens-before 该线程的任意操作 | |
| 线程终止规则 | 线程中所有操作 happens-before 其他线程检测到该线程终止 | Thread.join() |
| 中断规则 | 调用 interrupt() happens-before 检测中断 | Thread.interrupted() |
| 终结器规则 | 对象的构造函数结束 happens-before finalize() | |
| 传递性 | A happens-before B, B happens-before C → A happens-before C | |

```java
public class HappensBeforeDemo {
    private int x = 0;
    private volatile boolean flag = false;

    public void writer() {
        x = 42;                // 普通写
        flag = true;           // volatile 写
        // ↑ 通过 volatile 规则，x=42 对 reader 线程可见
    }

    public void reader() {
        if (flag) {            // volatile 读
            System.out.println(x);  // 保证看到 42
        }
    }
}
```

### 8.3 volatile 的内存语义

volatile 变量读写会插入**内存屏障**，禁止指令重排序。

```java
/**
 * volatile 的内存屏障策略 (JSR-133)
 *
 * 写 volatile v：
 *   [StoreStore] ← 确保之前普通写不重排到 volatile 写之后
 *   v = value
 *   [StoreLoad]  ← 确保 volatile 写不重排到之后 volatile 读之前
 *
 * 读 volatile v：
 *   v 的值
 *   [LoadLoad]   ← 确保之后普通读不重排到 volatile 读之前
 *   [LoadStore]  ← 确保之后普通写不重排到 volatile 读之前
 */
public class VolatileBarrierDemo {
    private volatile long counter = 0;

    // volatile 不保证原子性
    public void increment() {
        counter++; // 不是原子操作：read → modify → write
    }

    // 保证线程安全需要用 AtomicLong
    private final AtomicLong atomicCounter = new AtomicLong(0);

    public void safeIncrement() {
        atomicCounter.incrementAndGet();
    }
}
```

### 8.4 指令重排序

编译器、处理器都可能对指令重排序，JMM 只对特定场景提供保证。

```java
public class ReorderingDemo {
    private static int x = 0, y = 0;
    private static int a = 0, b = 0;

    public static void main(String[] args) throws Exception {
        for (int i = 0; ; i++) {
            x = 0; y = 0;
            a = 0; b = 0;

            Thread t1 = new Thread(() -> {
                a = 1;
                x = b;
            });
            Thread t2 = new Thread(() -> {
                b = 1;
                y = a;
            });

            t1.start(); t2.start();
            t1.join(); t2.join();

            // 理论上 (x,y) 可能是 (0,1), (1,0), (1,1), (0,0)
            // (0,0) 只在指令重排序时出现！
            if (x == 0 && y == 0) {
                System.out.println("第 " + i + " 次观测到重排序：x=0, y=0");
                break;
            }
        }
    }
}
```

### 8.5 JMM 的 8 种操作

| 操作 | 作用 | 对应动作 |
|------|------|---------|
| lock | 锁定主存变量 | synchronized |
| unlock | 解锁 | synchronized |
| read | 从主存读取到工作内存 | 普通读 |
| load | 工作内存放入变量副本 | 普通读 |
| use | 传递给执行引擎 | 读取变量值 |
| assign | 接收执行引擎的值 | 赋值 |
| store | 传递到主存 | 普通写 |
| write | 写入主存 | 普通写 |

---

## 9. JDK 版本差异详解

### 9.1 JDK 7 → JDK 8

| 特性 | JDK 7 | JDK 8 |
|------|-------|-------|
| 永久代 | PermGen | 逐步移除，部分移到 Metaspace |
| 字符串常量 | PermGen | **堆**（JDK 7 开始移到堆） |
| 默认 GC | Parallel Scavenge + Parallel Old | **Parallel Scavenge + Parallel Old**（未变） |
| G1 | -XX:+UseG1GC | 可用但非默认 |
| 方法区 | PermGen | Metaspace（JDK 8 正式替换） |
| 类加载 | 传统 | 基本不变 |

**为什么从 PermGen 改为 Metaspace？**
- PermGen 大小固定，容易 OOM（特别是动态生成类的场景）
- PermGen GC 效率低，Full GC 时才会回收
- Metaspace 使用本地内存，充分利用 OS 内存管理
- 减少 OOM 概率

### 9.2 JDK 8 → JDK 11

| 特性 | JDK 11 (LTS) |
|------|--------------|
| ZGC | 实验性（-XX:+UnlockExperimentalVMOptions -XX:+UseZGC） |
| Epsilon GC | 实验性，无操作 GC |
| 默认 GC | **G1**（JDK 9 开始） |
| 字符串去重 | String Deduplication（默认开启） |
| -Xverify:none | 废弃（JDK 13 移除） |

### 9.3 JDK 11 → JDK 17

| 特性 | JDK 17 (LTS) |
|------|--------------|
| ZGC | **生产就绪**（-XX:+UseZGC），停顿 < 1ms |
| 密封类 (Sealed Classes) | 正式版 |
| 默认 GC | 仍然是 G1 |
| 移除 CMS | CMS 已在 JDK 14 移除 |

### 9.4 JDK 17 → JDK 21

| 特性 | JDK 21 (LTS) |
|------|--------------|
| 虚拟线程 (Virtual Threads) | 正式版，Project Loom 产出 |
| 分代 ZGC | -XX:+ZGenerational |
| 记录模式 (Record Patterns) | 正式版 |
| 结构化并发 (Structured Concurrency) | 预览版 |

### 9.5 GC 默认配置沿革

```bash
# JDK 7: Parallel Scavenge + Parallel Old
java -XX:+UseParallelGC ...

# JDK 8: 仍然 Parallel
java -XX:+PrintCommandLineFlags -version
# 输出: -XX:InitialHeapSize=... -XX:MaxHeapSize=...
#       -XX:+UseParallelGC ...

# JDK 9+: 改为 G1
java -XX:+PrintCommandLineFlags -version
# 输出: -XX:+UseG1GC ...
```

### 9.6 String Constant Pool 迁移

```java
/**
 * String 常量池位置变化测试
 *
 * JDK 6: 在 PermGen (可能 OOM: PermGen space)
 * JDK 7: 移到 Heap (堆中)
 * JDK 8+: 仍在 Heap
 */
public class StringPoolLocation {
    public static void main(String[] args) {
        // 创建大量字符串，测试 OOM 位置
        List<String> list = new ArrayList<>();
        int i = 0;
        try {
            while (true) {
                // intern() 将字符串放入常量池
                list.add(String.valueOf(i++).intern());
            }
        } catch (OutOfMemoryError e) {
            System.out.println("OOM at " + i + " strings");
            System.out.println("Error: " + e);
            // JDK 6: java.lang.OutOfMemoryError: PermGen space
            // JDK 7+: java.lang.OutOfMemoryError: Java heap space
        }
    }
}
```

---

## 10. 常见 OOM 场景与分析

### 10.1 OOM 类型速查

| OOM 错误 | 区域 | 原因 | JVM 参数 |
|----------|------|------|---------|
| Java heap space | 堆 | 对象无法分配 | -Xmx |
| GC overhead limit exceeded | 堆 | 98% 时间花在 GC，回收 < 2% 堆 | -XX:-UseGCOverheadLimit |
| Metaspace | 方法区 | 类元数据太多 | -XX:MaxMetaspaceSize |
| Requested array size exceeds VM limit | 任意 | 数组太大 | - |
| Unable to create new native thread | 栈 | 线程太多 | ulimit / -Xss |
| Direct buffer memory | 直接内存 | DirectByteBuffer 太多 | -XX:MaxDirectMemorySize |
| StackOverflowError | 栈 | 递归太深 | -Xss |
| Kill process (OOM Killer) | 系统级 | OS 杀进程 | 加内存 / 优化 |

### 10.2 堆 OOM (Java heap space)

```java
/**
 * 堆 OOM 模拟
 * JVM: -Xms32m -Xmx32m -XX:+HeapDumpOnOutOfMemoryError
 */
public class HeapOOM {
    static class OOMObject {}

    public static void main(String[] args) {
        List<OOMObject> list = new ArrayList<>();
        while (true) {
            list.add(new OOMObject());
        }
    }
}
```

**排查步骤**：
1. 查看 `-Xmx` 是否合理
2. 使用 `jmap -histo` 查看对象分布
3. 分析 heap dump：`jmap -dump:format=b,file=heap.hprof`
4. 用 MAT / VisualVM 分析：是内存泄漏还是内存不足？
5. 如果是泄漏，找到 GC Root 的路径

### 10.3 Metaspace OOM

```java
/**
 * Metaspace OOM 模拟：动态生成大量类
 * JVM: -XX:MaxMetaspaceSize=32m -XX:+TraceClassLoading
 */
public class MetaspaceOOM {
    public static void main(String[] args) throws Exception {
        while (true) {
            // 使用 ASM 或 CGLIB 动态生成类
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(OOMObject.class);
            enhancer.setUseCache(false);
            enhancer.setCallback((MethodInterceptor) (obj, method, args1, proxy)
                    -> proxy.invokeSuper(obj, args1));
            // CGLIB 会生成新类，占用 Metaspace
            enhancer.create();
        }
    }

    static class OOMObject {}
}
```

### 10.4 栈溢出 (StackOverflowError)

```java
/**
 * 递归过深导致栈溢出
 * JVM: -Xss128k （缩小栈更容易溢出）
 */
public class StackSOF {
    private int stackLength = 1;

    public void stackLeak() {
        stackLength++;
        long a1=1, a2=2, a3=3, a4=4, a5=5, a6=6, a7=7, a8=8;
        stackLeak();
        // 注：局部变量越多，同一深度下栈帧越大
        System.out.println(a1); // 防优化
    }

    public static void main(String[] args) {
        StackSOF oom = new StackSOF();
        try {
            oom.stackLeak();
        } catch (StackOverflowError e) {
            System.out.println("stack length: " + oom.stackLength);
        }
    }
    // -Xss128k → stack length: ~800-1000
    // -Xss256k → stack length: ~2000-3000
}
```

### 10.5 直接内存 OOM

```java
/**
 * 直接内存 OOM (Direct Buffer)
 * JVM: -XX:MaxDirectMemorySize=32m -Xmx64m
 */
public class DirectMemoryOOM {
    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) throws Exception {
        List<ByteBuffer> list = new ArrayList<>();
        while (true) {
            // 分配直接内存
            ByteBuffer buffer = ByteBuffer.allocateDirect(_1MB);
            list.add(buffer);
        }
    }
}
```

---

## 11. JVM 调优参数速查

### 11.1 堆设置

```bash
# 堆大小
-Xms512m                   # 初始堆大小
-Xmx2g                     # 最大堆大小
-XX:NewSize=256m           # 新生代初始大小
-XX:MaxNewSize=512m        # 新生代最大大小
-XX:NewRatio=2             # 老年代/新生代=2（老年代占2/3）
-XX:SurvivorRatio=8        # Eden/Survivor=8（Eden占8/10）

# 元空间
-XX:MetaspaceSize=128m     # 初始 Metaspace
-XX:MaxMetaspaceSize=256m  # 最大 Metaspace
-XX:+UseCompressedOops     # 启用指针压缩（默认）
```

### 11.2 GC 选择

```bash
# JDK 8 默认 (Parallel)
-XX:+UseParallelGC
-XX:+UseParallelOldGC
-XX:ParallelGCThreads=N     # GC 线程数

# JDK 9+ 默认 (G1)
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200    # 期望最大停顿时间
-XX:G1HeapRegionSize=4m     # Region 大小 (1-32MB)
-XX:InitiatingHeapOccupancyPercent=45  # 启动并发标记的堆占用率

# ZGC (JDK 11+ 实验, 15+ 生产)
-XX:+UseZGC
-XX:ConcGCThreads=N

# CMS (JDK 9 deprecated, JDK 14 removed)
-XX:+UseConcMarkSweepGC
```

### 11.3 日志与诊断

```bash
# JDK 8 GC 日志
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/path/to/gc.log

# JDK 9+ 统一日志
-Xlog:gc*:file=/path/to/gc.log:time,uptime,level,tags

# 堆 Dump
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/path/to/dump.hprof

# 类加载追踪
-XX:+TraceClassLoading
-XX:+TraceClassUnloading
```

### 11.4 调优实战命令

```bash
# 查看 JVM 参数
jps -l                              # 列出 Java 进程
jinfo -flags <pid>                   # 查看 JVM Flags
jinfo -flag PrintGCDetails <pid>     # 查看某个参数值

# 查看堆使用
jstat -gc <pid> 1000 10             # 每秒输出 GC 情况，共 10 次
jstat -gccause <pid>                # 查看 GC 原因
jstat -gcutil <pid>                 # 查看 GC 使用率

# 堆 Dump 和对象分析
jmap -histo <pid>                   # 对象直方图
jmap -histo:live <pid>              # 仅查看存活对象（会触发 Full GC）
jmap -dump:format=b,file=dump.hprof <pid>  # Dump 堆

# 线程分析
jstack <pid>                        # 线程栈快照
jstack -l <pid>                     # 带锁信息

# 远程连接
# jstatd / JMX 方式远程监控
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```

---

## 12. 面试经典问题

### 基础问题

**Q1: JVM 内存分为哪几个区域？哪些是线程私有？哪些是线程共享？**
- 线程私有：程序计数器、JVM 栈、本地方法栈
- 线程共享：堆、方法区（Metaspace）

**Q2: 对象在堆中的分配过程是怎样的？**
- 优先在栈上分配（逃逸分析）→ TLAB 分配 → Eden 分配 → 大对象直接进入老年代

**Q3: 什么是双亲委派模型？为什么要这样设计？如何打破？**
- 安全性（保护核心类）、避免重复加载
- 打破方式：重写 loadClass、线程上下文类加载器、OSGi/Tomcat

### 进阶问题

**Q4: JMM 中 volatile 和 synchronized 的区别？**
- volatile：可见性 + 禁止重排序（不保证原子性）
- synchronized：可见性 + 原子性（互斥）
- volatile 无锁、无阻塞；synchronized 有锁开销

**Q5: happens-before 规则有哪些？举例说明。**
- 程序次序锁、监视器锁、volatile、线程启动/终止、中断、终结器、传递性

**Q6: String a = "ab"; String b = "a" + "b"; 这两个引用相等吗？**
- JDK 6+: 相等。字面量拼接在编译期优化为常量折叠

**Q7: 一个 Object 对象占多少内存？**
- 64 位 JVM 默认：Mark Word 8B + Klass Pointer 4B（压缩） + 0B 数据 + 4B 填充 = 16B

### 深度问题

**Q8: JVM 中对象创建的步骤？内存分配时如何保证线程安全？**
- CAS + 失败重试 / TLAB（线程本地分配缓冲）

**Q9: 什么是内存屏障？volatile 如何通过内存屏障实现可见性？**
- LoadLoad, StoreStore, LoadStore, StoreLoad 四种屏障
- volatile 写：StoreStore + StoreLoad；volatile 读：LoadLoad + LoadStore

**Q10: JDK 8 的 Metaspace 相比 JDK 7 的 PermGen 有什么优势？**
- 使用本地内存，避免 OOM；动态扩展；GC 更高效；减少 Full GC 频率

---

## 参考资源

- [The Java Virtual Machine Specification (Java SE 21 Edition)](https://docs.oracle.com/javase/specs/jvms/se21/html/)
- [JSR-133: Java Memory Model and Thread Specification](https://jcp.org/en/jsr/detail?id=133)
- [JDK 17 JVM Tuning Guide](https://docs.oracle.com/en/java/javase/17/gctuning/)
- [Shipilev: Java Memory Model Pragmatics](https://shipilev.net/blog/2016/close-encounters-of-jmm-kind/)
- [JOL (Java Object Layout)](https://openjdk.org/projects/code-tools/jol/)

---

*最后更新: 2026-05-31 | 适用于 JDK 8/11/17/21*
