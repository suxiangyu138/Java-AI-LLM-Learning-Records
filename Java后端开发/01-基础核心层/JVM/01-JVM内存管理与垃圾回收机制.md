# JVM内存管理与垃圾回收机制

> 本文全面深入地解析Java虚拟机（JVM）的内存分区结构、垃圾回收（GC）原理、经典与现代垃圾收集器的工作机制、类加载流程及JVM调优实践，帮助开发者构建系统化的JVM知识体系。

---

## 目录

1. [JVM内存分区](#1-jvm内存分区)
2. [垃圾回收机制](#2-垃圾回收机制)
3. [经典垃圾收集器](#3-经典垃圾收集器)
4. [现代垃圾收集器](#4-现代垃圾收集器)
5. [类加载机制](#5-类加载机制)
6. [JVM参数与调优](#6-jvm参数与调优)
7. [GC参数速查表](#7-gc参数速查表)
8. [故障排查清单](#8-故障排查清单)

---

## 1. JVM内存分区

Java虚拟机在执行Java程序的过程中，会将其管理的内存划分为若干个不同的数据区域。这些区域各有用途，有的随虚拟机进程的启动而创建和销毁，有的则与线程生命周期一致。理解这些内存区域的划分和作用，是进行JVM调优和排查内存问题的基石。

### 1.1 程序计数器（Program Counter Register）

程序计数器是一块较小的内存空间，可以看作是当前线程所执行字节码的行号指示器。它是线程私有的——每个线程都有自己的程序计数器。

**核心功能：**

- 字节码解释器工作时，通过改变计数器的值来选取下一条需要执行的字节码指令。
- 分支、循环、跳转、异常处理、线程恢复等基础功能都依赖程序计数器完成。

**关键特性：**

- 线程私有，生命周期与线程相同。
- 如果线程正在执行的是Java方法，计数器记录的是正在执行的虚拟机字节码指令地址。
- 如果线程正在执行的是Native方法（本地方法），计数器的值为空（Undefined）。
- **此区域是JVM规范中唯一没有规定任何OutOfMemoryError情况的区域**，因为程序计数器所需的内存大小是固定的。

```java
// 程序计数器的作用示例：多线程切换
// 线程A执行到第5行字节码时被挂起，程序计数器保存了第5行的地址
// 当线程A重新获得CPU时间片时，通过程序计数器恢复执行
public class PCRegisterDemo {
    public static void main(String[] args) {
        // 每个线程都有自己独立的程序计数器
        new Thread(() -> {
            int sum = 0;
            for (int i = 0; i < 100; i++) {
                sum += i;  // 线程1的程序计数器追踪当前执行位置
            }
        }).start();

        new Thread(() -> {
            int product = 1;
            for (int i = 1; i <= 10; i++) {
                product *= i;  // 线程2的程序计数器独立追踪
            }
        }).start();
    }
}
```

### 1.2 虚拟机栈（Java Virtual Machine Stack）

虚拟机栈描述的是Java方法执行的线程内存模型：每个方法被执行时，JVM都会同步创建一个栈帧（Stack Frame），用于存储局部变量表、操作数栈、动态链接、方法出口等信息。每一个方法从调用到执行完成的过程，就对应一个栈帧在虚拟机栈中入栈到出栈的过程。

**栈帧的四个组成部分：**

| 组成部分 | 说明 | 存储内容 |
|---------|------|---------|
| 局部变量表（Local Variables） | 存放方法参数和方法内部定义的局部变量 | 基本数据类型、对象引用（reference）、returnAddress类型 |
| 操作数栈（Operand Stack） | 方法的执行引擎的工作空间 | 字节码指令的操作数和运算结果 |
| 动态链接（Dynamic Linking） | 指向运行时常量池中该方法的符号引用 | 将符号引用转换为直接引用 |
| 方法返回地址（Return Address） | 方法正常退出或异常退出时的返回信息 | 调用者的PC计数器值 |

**异常情况：**

- **StackOverflowError**：当线程请求的栈深度大于虚拟机所允许的最大深度时抛出。常见于递归调用过深或无限递归。

```java
public class StackOverflowDemo {
    private static int depth = 0;

    public static void recursiveCall() {
        depth++;
        recursiveCall(); // 无终止条件的递归
    }

    public static void main(String[] args) {
        try {
            recursiveCall();
        } catch (StackOverflowError e) {
            // -Xss 可以控制栈大小
            // 默认: 通常为1024KB（取决于平台）
            // 设置: -Xss256k
            System.out.println("递归深度: " + depth);
            throw e;
        }
    }
}
```

- **OutOfMemoryError**：如果虚拟机栈可以动态扩展（大部分Java虚拟机支持），但扩展时无法申请到足够的内存，或者创建新线程时无法分配新的栈空间，就会抛出OutOfMemoryError。

```java
public class StackOOMDemo {
    public static void main(String[] args) {
        // 创建大量线程导致栈内存溢出
        // 设置: -Xss2m 可以尝试，但操作系统线程数有限
        while (true) {
            new Thread(() -> {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
```

**设置栈大小：**
```bash
# 设置每个线程的栈大小为256KB（减少栈大小可以创建更多线程）
java -Xss256k StackOOMDemo
```

### 1.3 本地方法栈（Native Method Stack）

本地方法栈与虚拟机栈的作用非常相似，区别在于虚拟机栈为虚拟机执行Java方法（字节码）服务，而本地方法栈则为虚拟机使用到的Native方法服务。

- 与虚拟机栈一样，本地方法栈也会抛出StackOverflowError和OutOfMemoryError。
- HotSpot虚拟机直接将虚拟机栈和本地方法栈合二为一。
- Native方法通过JNI（Java Native Interface）调用，底层由C/C++实现。

```java
// JNI调用本地方法的经典示例
public class NativeMethodDemo {
    // 声明一个native方法，由C/C++实现
    public native void nativePrint(String message);

    // 加载本地库
    static {
        System.loadLibrary("native-lib");
    }

    public static void main(String[] args) {
        new NativeMethodDemo().nativePrint("Hello from JNI");
    }
}
```

### 1.4 堆（Heap）

堆是JVM管理的内存中**最大**的一块区域，被所有线程共享，在虚拟机启动时创建。堆的唯一目的就是存放对象实例——**几乎**所有的对象都在堆上分配。

> 注：随着JIT编译器和逃逸分析技术的发展，栈上分配和标量替换技术使得"所有对象都在堆上分配"变得不那么绝对，但堆仍然是对象分配的主战场。

#### 堆的分代结构

JVM堆被划分为不同的代（Generation），以支持分代垃圾回收算法：

```ascii
┌─────────────────────────────────────────────────────────────┐
│                         Java Heap                           │
├──────────────────────┬──────────────────────────────────────┤
│      新生代 (Young)   │           老年代 (Old)                │
├────────┬──────┬──────┤                                      │
│  Eden  │ S0  │ S1  │                                        │
├────────┴──────┴──────┴──────────────────────────────────────┤
│                         元空间 (Metaspace)                    │
│                  (非堆, 使用本地内存)                          │
└──────────────────────────────────────────────────────────────┘
```

**新生代（Young Generation）：**

新生代被划分为三个区域：Eden区和两个Survivor区（From Survivor和To Survivor）。

| 区域 | 默认比例 | 说明 |
|------|---------|------|
| Eden | 8/10的新生代 | 大多数对象首先在Eden区分配 |
| Survivor 0 (From) | 1/10的新生代 | 存放一次GC后存活的对象 |
| Survivor 1 (To) | 1/10的新生代 | 存放一次GC后存活的对象 |

默认比例 `Eden : S0 : S1 = 8 : 1 : 1`，可通过 `-XX:SurvivorRatio` 调整。

**对象分配过程（TLAB机制）：**

为了提高对象分配效率，JVM引入了TLAB（Thread Local Allocation Buffer，线程本地分配缓冲区）：

```ascii
线程1 ───→ [TLAB(线程1)] ──┐
线程2 ───→ [TLAB(线程2)] ──┼──→ [Eden区]
线程3 ───→ [TLAB(线程3)] ──┘
```

```java
// 对象分配过程演示
public class ObjectAllocationDemo {

    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) {
        // -Xms20m -Xmx20m -Xmn10m -XX:SurvivorRatio=8
        // -XX:+PrintGCDetails -XX:+UseSerialGC
        // -XX:PretenureSizeThreshold=3m (大于3MB的对象直接进入老年代)

        byte[] allocation1 = new byte[2 * _1MB];  // 在Eden分配
        byte[] allocation2 = new byte[2 * _1MB];  // 在Eden分配
        byte[] allocation3 = new byte[2 * _1MB];  // 在Eden分配
        byte[] allocation4 = new byte[4 * _1MB];  // 触发Minor GC
    }
}
```

**对象晋升过程：**

```ascii
         ┌────────┐         Minor GC         ┌────────┐
         │ Eden   │ ──── 存活对象复制到 ────→│ S0/S1  │
         │ (满)   │                          │ (age=1) │
         └────────┘                          └────────┘
                                                 │
                                          经过多次Minor GC
                                          age达到阈值(默认15)
                                                 ↓
                                          ┌────────┐
                                          │ 老年代   │
                                          │ (Old)   │
                                          └────────┘
```

**关键参数：**

```bash
# 设置堆大小
-Xms512m        # 初始堆大小
-Xmx512m        # 最大堆大小（生产环境建议与-Xms设相同，避免动态扩容）
-Xmn256m        # 新生代大小
-XX:NewRatio=2  # 老年代/新生代=2，即新生代占堆的1/3
-XX:SurvivorRatio=8  # Eden/Survivor=8
-XX:PretenureSizeThreshold=3m  # 大于3MB的对象直接进入老年代
-XX:MaxTenuringThreshold=15   # 晋升老年代的年龄阈值
-XX:+UseTLAB    # 启用TLAB（默认开启）
-XX:TLABSize    # 设置TLAB大小
```

#### 字符串常量池

在JDK 7之前，字符串常量池存在于方法区的运行时常量池中。JDK 7将字符串常量池移到了堆中，主要原因是方法区的GC效率较低，而字符串常量池中的字符串对象需要频繁回收。

```java
public class StringPoolDemo {
    public static void main(String[] args) {
        String s1 = "hello";                  // 字符串常量池中创建
        String s2 = "hello";                  // 从常量池中引用
        System.out.println(s1 == s2);         // true

        String s3 = new String("hello");      // 堆中新对象
        System.out.println(s1 == s3);         // false

        String s4 = s3.intern();              // 从常量池获取
        System.out.println(s1 == s4);         // true（JDK7+）
    }
}
```

### 1.5 方法区（元空间）

方法区是各个线程共享的内存区域，用于存储已被虚拟机加载的**类型信息、常量、静态变量、即时编译器编译后的代码缓存**等数据。

**别名演变：**

- **JDK 7及以前**：方法区的实现称为**永久代**（Permanent Generation），位于堆内。
- **JDK 8及以后**：永久被移除，取而代之的是**元空间**（Metaspace），使用**本地内存**（Native Memory）。

**为什么从永久代改为元空间？**

| 永久代的痛点 | 元空间的解决方案 |
|-------------|----------------|
| 有固定大小上限（-XX:MaxPermSize） | 使用本地内存，理论上限受物理内存限制 |
| 容易产生内存溢出（PermGen Space） | 极大地降低了OOM风险 |
| 与堆内存管理耦合，GC效率低 | 独立管理，类元数据卸载更灵活 |
| 调优困难 | 更少的调优参数 |

**元空间存储内容：**

- **类型信息**：类名、访问修饰符、常量池、字段描述、方法描述等。
- **运行时常量池**：Class文件中常量池的运行时表示。
- **静态变量**：类变量（JDK 7的静态变量已移至堆中存储）。
- **JIT编译后的代码缓存**：热点代码编译后的机器码。
- **方法字节码**：类中每个方法的字节码指令。

**元空间参数：**

```bash
# 元空间参数
-XX:MetaspaceSize=256m    # 元空间初始大小（触发GC的阈值）
-XX:MaxMetaspaceSize=512m # 元空间最大大小（建议设置，防止无限膨胀）
-XX:+UseCompressedClassPointers  # 启用类指针压缩（默认开启）
-XX:CompressedClassSpaceSize=1g  # 压缩类空间大小
```

```java
// 模拟方法区溢出（需在JDK 7或更早版本）
// JDK 7: -XX:PermSize=10m -XX:MaxPermSize=10m
// JDK 8+: 类信息存储在元空间，需大量动态生成类来模拟
public class MetaspaceOOMDemo {
    public static void main(String[] args) throws Exception {
        // 使用CGLIB或ASM动态创建大量类
        while (true) {
            // 动态生成代理类
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(MetaspaceOOMDemo.class);
            enhancer.setUseCache(false);
            enhancer.setCallback((MethodInterceptor) (obj, method, args1, proxy) -> {
                return proxy.invokeSuper(obj, args1);
            });
            enhancer.create();
        }
    }
}
```

### 1.6 直接内存（Direct Memory）

直接内存并不是JVM运行时数据区的一部分，也不是JVM规范中定义的内存区域，但它频繁被使用，也可能导致OutOfMemoryError。

**核心特点：**

- 使用Native函数库直接分配堆外内存。
- 通过堆中的DirectByteBuffer对象操作。
- 避免了在Java堆和Native堆之间来回复制数据，**提高了IO性能**。
- 大小不受Java堆限制，受本机总内存（含物理内存、SWAP分区或分页文件）限制。

```java
import java.nio.ByteBuffer;

public class DirectMemoryDemo {
    private static final int _1GB = 1024 * 1024 * 1024;

    public static void main(String[] args) {
        // -XX:MaxDirectMemorySize=1g
        // 分配直接内存
        ByteBuffer buffer = ByteBuffer.allocateDirect(_1GB);
        // 写入数据
        buffer.putInt(42);
        // 读取时需切换为读模式
        buffer.flip();
        int value = buffer.getInt();
        System.out.println(value);
        // 释放直接内存
        // DirectByteBuffer在GC时释放关联的本地内存
        buffer = null;
    }
}
```

**直接内存与堆内存的对比：**

| 对比项 | 堆内存（Heap） | 直接内存（Direct Buffer） |
|-------|--------------|-------------------------|
| 分配速度 | 快 | 慢（allocateDirect比allocate慢） |
| IO效率 | 需要一次中间复制 | 零复制，效率高 |
| 内存管理 | 由GC管理 | 需手动或依赖GC回收 |
| 适用场景 | 大部分Java对象 | 网络IO、文件IO、大块数据 |

```bash
# 设置直接内存最大值（默认等于-Xmx）
-XX:MaxDirectMemorySize=2g
```

---

## 2. 垃圾回收机制

### 2.1 判断对象是否存活

垃圾回收的第一步是确定哪些对象是"已死"的（即不可能再被任何途径使用的对象）。主要有两种判断方法。

#### 引用计数法（Reference Counting）

**原理：** 在每个对象中维护一个引用计数器，每当有一个地方引用该对象时，计数器+1；引用失效时，计数器-1。计数器为0的对象就是可回收对象。

**优点：** 实现简单，判定效率高。

**致命缺陷——循环引用问题：**

```java
public class ReferenceCountingDemo {
    public Object instance = null;

    public static void main(String[] args) {
        ReferenceCountingDemo objA = new ReferenceCountingDemo();
        ReferenceCountingDemo objB = new ReferenceCountingDemo();

        // 互相引用
        objA.instance = objB;
        objB.instance = objA;

        // 将外部引用置空
        objA = null;
        objB = null;

        // 此时objA和objB的引用计数都不为0
        // 引用计数法无法回收它们
        // 但可达性分析可以回收
        System.gc(); // 建议GC
    }
}
```

正因为循环引用问题无法解决，主流的Java虚拟机（包括HotSpot）**没有采用**引用计数法。

#### 可达性分析算法（Reachability Analysis）

**原理：** 通过一系列称为"GC Roots"的根对象作为起始节点集，从这些节点开始向下搜索，搜索所走过的路径称为引用链（Reference Chain）。如果一个对象到GC Roots没有任何引用链相连（即从GC Roots到这个对象不可达），则证明此对象是不可用的。

```ascii
           ┌─── GC Roots ───┐
           │                 │
     ┌─────┼─────┬─────┬─────┼─────┐
     │     │     │     │     │     │
   ObjectA ObjectB ObjectC 栈引用 JNI引用
     │             │
     │             │
   ObjectD      ObjectE
                     │
                     │
                  ObjectF  ← 不可达，将被回收
```

**GC Roots包括以下几类：**

| 类型 | 说明 | 示例 |
|------|------|------|
| 虚拟机栈引用 | 栈帧中局部变量表引用的对象 | 当前正在执行的方法中的参数、局部变量 |
| 静态引用 | 方法区中类的静态属性引用的对象 | static字段引用的对象 |
| JNI引用 | 本地方法栈中JNI引用的对象 | native方法引用的对象 |
| 活跃线程 | 所有活跃线程对象 | Thread对象 |
| 内部引用 | JVM内部的引用 | 基本数据类型对应的Class对象、常驻异常对象 |
| 同步监视器 | 被synchronized持有的对象 | 加锁的对象 |

```java
import java.util.ArrayList;
import java.util.List;

public class GCRootsDemo {
    private static final List<Object> STATIC_LIST = new ArrayList<>();  // 静态引用 → GC Roots

    public void method() {
        Object localVar = new Object();     // 栈帧引用 → GC Roots
        STATIC_LIST.add(localVar);
    }

    public static void main(String[] args) {
        GCRootsDemo demo = new GCRootsDemo(); // 栈帧引用 → GC Roots
        // 当demo = null后，demo对象不再属于GC Roots
    }
}
```

**对象被回收的两次标记过程：**

一个对象在可达性分析中被判定为不可达后，并不是"非死不可"的——它还需要经历两次标记过程：

1. **第一次标记**：可达性分析发现没有与GC Roots相连的引用链。
2. **Finalize检查**：判断该对象是否有必要执行finalize()方法。如果对象没有覆盖finalize()方法，或者finalize()已经被虚拟机调用过，则直接回收。
3. **第二次标记**：如果对象需要执行finalize()，会放入一个低优先级的队列中执行。如果对象在finalize()中重新与引用链上的对象建立了关联，则在第二次标记时被移出"即将回收"的集合。

> **重要：** finalize()方法已被Java官方声明为**已废弃**，不推荐使用。JDK 9+中标记为Deprecated。它的执行时机不确定，性能差，且容易导致问题。

### 2.2 引用类型

Java将引用分为四种类型，强度依次递减：

| 引用类型 | 类名 | 回收时机 | 典型用途 |
|---------|------|---------|---------|
| **强引用** | (普通引用) | 永不回收（除非不可达） | 普通对象 |
| **软引用** | SoftReference | 内存不足时回收 | 缓存、内存敏感缓存 |
| **弱引用** | WeakReference | GC发生时回收 | WeakHashMap、ThreadLocal |
| **虚引用** | PhantomReference | 随时可能回收（无法获取对象） | 对象回收追踪、堆外内存管理 |
| **终结器引用** | FinalReference | GC时触发finalize() | 已废弃 |

#### 强引用（Strong Reference）

最常见的引用类型，如 `Object obj = new Object()`。只要强引用还存在，GC就永远不会回收被引用的对象。

```java
public class StrongReferenceDemo {
    public static void main(String[] args) {
        Object obj = new Object();  // 强引用
        // 只要obj变量存在且指向该对象，对象就不会被GC回收
        obj = null;  // 显式解除引用后，对象才可被回收
    }
}
```

#### 软引用（Soft Reference）

用来描述一些"还有用但非必须"的对象。对于软引用关联的对象，在系统将要发生内存溢出异常之前，会将这些对象列进回收范围进行第二次回收。如果这次回收后还没有足够的内存，才会抛出OOM。

```java
import java.lang.ref.SoftReference;

public class SoftReferenceDemo {
    public static void main(String[] args) {
        // 创建一个软引用，指向一个占用内存的大对象
        SoftReference<byte[]> softRef = new SoftReference<>(new byte[4 * 1024 * 1024]);

        System.out.println("软引用对象: " + softRef.get());  // 获取对象

        // 尝试分配大内存，迫使GC回收软引用对象
        byte[] allocation = new byte[5 * 1024 * 1024];

        System.out.println("回收后: " + softRef.get());  // 很可能为null
    }
}
```

**应用场景：** 实现内存敏感的缓存。例如，图片缓存、浏览器页面缓存等。

```java
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

// 使用软引用实现内存敏感缓存
public class SoftCache<K, V> {
    private final Map<K, SoftReference<V>> cache = new HashMap<>();

    public void put(K key, V value) {
        cache.put(key, new SoftReference<>(value));
    }

    public V get(K key) {
        SoftReference<V> softRef = cache.get(key);
        return softRef != null ? softRef.get() : null;
    }
}
```

#### 弱引用（Weak Reference）

用来描述"非必须"的对象，强度比软引用更弱。被弱引用关联的对象**只能存活到下一次垃圾回收发生之前**。当GC发生时，无论当前内存是否足够，弱引用关联的对象都会被回收。

```java
import java.lang.ref.WeakReference;

public class WeakReferenceDemo {
    public static void main(String[] args) {
        Object obj = new Object();
        WeakReference<Object> weakRef = new WeakReference<>(obj);

        System.out.println("GC前: " + weakRef.get());  // 有值

        obj = null;  // 解除强引用
        System.gc();  // 建议GC

        System.out.println("GC后: " + weakRef.get());  // null（弱引用被回收）
    }
}
```

**WeakHashMap：** 当key不再被外部强引用时，对应的Entry会被自动清除。

```java
import java.util.WeakHashMap;

public class WeakHashMapDemo {
    public static void main(String[] args) throws InterruptedException {
        WeakHashMap<Object, String> map = new WeakHashMap<>();

        Object key = new Object();  // 强引用
        map.put(key, "value");

        System.out.println("GC前大小: " + map.size());  // 1

        key = null;  // 解除强引用
        System.gc();
        Thread.sleep(100);

        System.out.println("GC后大小: " + map.size());  // 0（Entry被自动清理）
    }
}
```

**ThreadLocal与弱引用：**

ThreadLocal的内存泄漏问题与弱引用密切相关。每个Thread维护一个ThreadLocalMap，其中的Entry以ThreadLocal实例为key，且是**弱引用**。

```java
// ThreadLocalMap.Entry的简化源码
static class Entry extends WeakReference<ThreadLocal<?>> {
    Object value;
    Entry(ThreadLocal<?> k, Object v) {
        super(k);  // key是弱引用
        value = v;
    }
}
```

```ascii
Thread ──→ ThreadLocalMap ──→ Entry(key: WeakRef(ThreadLocal), value: Object)
                                      │
                                      │ 弱引用
                                      ↓
                                ThreadLocal对象
```

当外部强引用 `threadLocal = null` 后，ThreadLocal对象只有Entry中的弱引用指向它，下次GC时就会被回收。此时Entry的key为null，但value仍然存在，这就造成了**内存泄漏**。解决方法是在使用后调用 `remove()`。

```java
public class ThreadLocalMemoryLeakDemo {
    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    public static void main(String[] args) {
        THREAD_LOCAL.set("value");
        String value = THREAD_LOCAL.get();
        // 使用后务必调用remove()清理，防止内存泄漏
        THREAD_LOCAL.remove();
    }
}
```

#### 虚引用（Phantom Reference）

虚引用是最弱的引用关系。一个对象是否有虚引用存在，完全不会对其生存时间构成影响，也无法通过虚引用获取一个对象实例。

**特性：**

- 无法通过 `get()` 方法获取对象（总是返回null）。
- 虚引用必须和引用队列（ReferenceQueue）联合使用。
- 当对象被GC回收时，如果它有虚引用，会收到一个系统通知（虚引用被加入引用队列）。

```java
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

public class PhantomReferenceDemo {
    public static void main(String[] args) throws InterruptedException {
        Object obj = new Object();
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        PhantomReference<Object> phantomRef = new PhantomReference<>(obj, queue);

        System.out.println("虚引用对象: " + phantomRef.get());  // null

        obj = null;
        System.gc();
        Thread.sleep(200);

        // 对象被回收后，虚引用被放入引用队列
        System.out.println("引用队列中是否有元素: " + (queue.poll() != null));  // true
    }
}
```

**核心应用场景：** 堆外内存的回收管理。NIO的DirectByteBuffer通过虚引用机制来释放直接内存。

### 2.3 垃圾回收算法

#### 标记-清除算法（Mark-Sweep）

最基础的垃圾收集算法。

**执行过程：**

1. **标记阶段**：从GC Roots出发，标记所有可达对象。
2. **清除阶段**：遍历整个堆，回收所有未被标记的对象。

```ascii
标记前:   [A] [B] [C] [D] [E]       A、C为存活对象
              ↓ 标记
标记后:   [A✓] [B] [C✓] [D] [E]     B、D、E被标记为可回收
              ↓ 清除
清除后:   [A] [C] [空闲] [空闲] [空闲]

问题: 内存碎片化，空闲区域不连续
```

**缺点：**

- **执行效率不稳定**：标记和清除两个过程的效率随对象数量增加而降低。
- **内存碎片化**：清除后产生大量不连续的内存碎片，可能导致后续大对象分配时因找不到连续空间而提前触发另一次GC。

#### 标记-整理算法（Mark-Compact）

适用于老年代的垃圾收集算法。

**执行过程：**

1. **标记阶段**：与标记-清除算法一样，标记所有存活对象。
2. **整理阶段**：将所有存活对象向内存空间一端移动，然后直接清理掉边界以外的内存。

```ascii
标记前:   [A] [B] [C] [D] [E]       A、C为存活对象
              ↓ 标记+整理
整理后:   [A] [C] [空闲] [空闲] [空闲]
           ---存活对象连续排列----

优点: 无内存碎片，分配大对象更方便
缺点: 移动对象需要STW（Stop The World），暂停用户线程
```

**优点：** 没有内存碎片，内存分配更高效（可使用指针碰撞分配）。

**缺点：** 移动存活对象需要暂停所有用户线程（Stop The World），增加了停顿时间。

#### 复制算法（Copying）

适用于新生代的垃圾收集算法。

**原理：** 将可用内存按容量划分为大小相等的两块，每次只使用其中一块。当这一块用完了，将还存活着的对象复制到另一块上面，然后再把已使用过的内存空间一次清理掉。

```ascii
GC前:
┌──────────────┬──────────────┐
│  Eden(已满)   │  From(S0)    │  To(S1)(空)
│  [A][B][C][D] │  [E][F]      │
└──────────────┴──────────────┘
        │
        ↓ Minor GC（复制存活对象到To）
        │ A、B、E存活
┌──────────────┬──────────────┐
│  空闲(清空)   │  From(S0)    │  To(S1)→From (A,B,E)
│              │  (空)        │
└──────────────┴──────────────┘
```

**HotSpot新生代的分区设计：**

HotSpot将新生代划分为一块较大的Eden区和两块较小的Survivor区，默认比例为 `Eden : Survivor = 8 : 1`。每次只使用Eden和其中一块Survivor，当发生Minor GC时，将Eden和Survivor中存活的对象一次性复制到另一块Survivor上，然后清理Eden和用过的Survivor。

- **可用空间**：90%（Eden + 1个Survivor）。
- **浪费空间**：10%（1个Survivor作为复制保留区）。

**当Survivor空间不足时：** 依赖老年代进行**分配担保**（Handle Promotion），将存活对象直接晋升到老年代。

**优点：** 实现简单，运行高效，没有内存碎片。

**缺点：** 可用内存缩小为原来的一半（优化后仅浪费10%）；对象存活率高时复制操作多。

#### 分代收集理论

当前商业虚拟机的垃圾收集器，大多遵循"分代收集"（Generational Collection）的设计原则。

**核心假设——弱分代假说（Weak Generational Hypothesis）：**

- **绝大多数对象都是朝生夕死的**（分配后很快变得不可达）。
- **熬过越多次GC的对象越难消亡**（长期存活的对象倾向于继续存活）。

**基于上述假说的分代策略：**

| 分代 | 回收频率 | 回收算法 | GC类型 |
|------|---------|---------|--------|
| 新生代（Young） | 频繁（Minor GC） | 复制算法 | Minor GC / Young GC |
| 老年代（Old） | 低频（Major/Full GC） | 标记-清除 或 标记-整理 | Major GC / Full GC |

**跨代引用问题：**

为了解决老年代对象引用新生代对象的问题，JVM引入了**记忆集（Remembered Set）**来避免扫描整个老年代。

---

## 3. 经典垃圾收集器

如果说垃圾回收算法是内存回收的方法论，那么垃圾收集器就是内存回收的具体实现。以下收集器覆盖了从单线程到多线程、从客户端到服务端的完整场景。

```ascii
JDK 7/8 经典收集器组合:

                     新生代                         老年代
               ┌──────────────┐            ┌──────────────┐
  Serial ─────→│    Serial    │────→───────│  Serial Old  │←──── Serial Old
               └──────────────┘            └──────────────┘
               ┌──────────────┐            ┌──────────────┐
  ParNew ─────→│    ParNew    │────→───────│      CMS     │←──── Concurrent
               └──────────────┘            └──────────────┘
               ┌──────────────┐            ┌──────────────┐
  Throughput → │  Parallel    │────→───────│ Parallel Old │←──── Throughput
               │  Scavenge    │            │              │
               └──────────────┘            └──────────────┘
               ┌──────────────────────────────────────────┐
  G1 ─────────→│      G1 (Garbage First)                  │
               │  (整堆管理，不区分新生代/老年代物理分区)       │
               └──────────────────────────────────────────┘
```

### 3.1 Serial收集器

**特点：** 单线程工作，进行垃圾收集时必须暂停所有用户线程（Stop The World）。

**工作模式：** `-XX:+UseSerialGC` 启用（Serial + Serial Old）。

**适用场景：** 客户端模式下的Java应用。对于单CPU环境，Serial收集器由于没有线程交互开销，反而可以获得最高的单线程收集效率。

```bash
# 启用Serial收集器（新生代Serial + 老年代Serial Old）
java -XX:+UseSerialGC -jar application.jar

# 对于运行在Client模式下的JVM，默认就是Serial收集器
```

### 3.2 ParNew收集器

**特点：** Serial收集器的多线程并行版本。除了使用多条线程进行垃圾收集之外，其余行为与Serial一致。

**工作模式：** `-XX:+UseParNewGC` 启用。

**关键点：** ParNew是唯一能与CMS收集器配合工作的新生代收集器。

```bash
# 启用ParNew + CMS组合
java -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -jar application.jar
# 在JDK 9+中，UseParNewGC已被废弃，UseConcMarkSweepGC会自动选择ParNew
```

**并行与并发的区别：**

| 概念 | 说明 |
|------|------|
| 并行（Parallel） | 多条垃圾收集线程并行工作，此时用户线程处于等待状态 |
| 并发（Concurrent） | 垃圾收集线程与用户线程同时执行（不一定是并行，可能交替执行） |

### 3.3 Parallel Scavenge收集器

**特点：** 关注**吞吐量**（Throughput）的收集器，也被称为"吞吐量优先收集器"。

**吞吐量定义：**
```
吞吐量 = 运行用户代码时间 / (运行用户代码时间 + 垃圾收集时间)
```

**核心参数：**

| 参数 | 说明 |
|------|------|
| `-XX:MaxGCPauseMillis` | 设置最大GC停顿时间（毫秒），值越小则停顿越短，但可能降低吞吐量 |
| `-XX:GCTimeRatio` | 设置吞吐量大小（0~100），默认99，即允许1%的GC时间 |
| `-XX:+UseAdaptiveSizePolicy` | 开启自适应调节策略（GC Ergonomics） |

**自适应调节（GC Ergonomics）：** Parallel Scavenge收集器有一个重要特性，即可以设置一个期望的停顿时间或吞吐量目标，由JVM自动调整堆大小、代比例等参数来满足目标。

```bash
# 启用Parallel Scavenge + Parallel Old组合
java -XX:+UseParallelGC -XX:+UseParallelOldGC -jar application.jar

# 设置最大GC停顿时间为100ms
java -XX:+UseParallelGC -XX:MaxGCPauseMillis=100 -jar application.jar

# 设置吞吐量（允许GC时间占总时间的1%）
java -XX:+UseParallelGC -XX:GCTimeRatio=99 -jar application.jar

# 开启自适应调节
java -XX:+UseParallelGC -XX:+UseAdaptiveSizePolicy -jar application.jar
```

### 3.4 CMS收集器

CMS（Concurrent Mark Sweep）收集器是一种以**获取最短回收停顿时间**为目标的收集器，特别适合对响应时间敏感的B/S架构服务端应用。

**工作流程（四个阶段）：**

```ascii
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 初始标记   │───→│ 并发标记   │───→│ 重新标记   │───→│ 并发清除   │
│ (STW)     │    │ (并发)    │    │ (STW)     │    │ (并发)    │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
  暂停短         耗时最长      暂停比初始      不暂停用户
                 但用户不暂停   标记略长        线程
```

| 阶段 | 是否STW | 说明 |
|------|---------|------|
| 初始标记（Initial Mark） | 是 | 标记GC Roots能直接关联到的对象，速度很快 |
| 并发标记（Concurrent Mark） | 否 | 从GC Roots开始遍历整个对象图，耗时最长 |
| 重新标记（Remark） | 是 | 修正并发标记期间因用户程序运行而产生的变动记录 |
| 并发清除（Concurrent Sweep） | 否 | 清理已被标记死亡的对象 |

**优点：** 并发收集，低停顿。

**缺点：**

1. **CPU资源敏感**：并发阶段占用CPU资源，导致应用变慢。
2. **浮动垃圾（Floating Garbage）**：并发标记和并发清除阶段，用户线程仍在运行，产生的新垃圾只能在下一次GC时清理。
3. **"Concurrent Mode Failure"**：如果在并发标记过程中老年代被浮动垃圾填满，CMS会退化为Serial Old进行Full GC（STW时间变长）。
4. **内存碎片**：基于标记-清除算法，产生大量内存碎片，可能引发提前Full GC。

```bash
# 启用CMS收集器
java -XX:+UseConcMarkSweepGC -jar application.jar

# 设置CMS在老年代占用70%时启动（默认92%）
java -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -jar application.jar

# 开启CMS碎片整理（默认开启）
java -XX:+UseCMSCompactAtFullCollection -jar application.jar

# 碎片整理前暂停时间
java -XX:CMSFullGCsBeforeCompaction=5 -jar application.jar
```

### 3.5 G1收集器（重点）

G1（Garbage First）收集器是JDK 7u4中引入的里程碑式垃圾收集器，在JDK 8u40+趋于成熟，JDK 9中将G1设为**默认垃圾收集器**。

#### 核心设计——Region

G1不再坚持固定大小和固定数量的分代划分，而是将堆划分为多个大小相等的**独立区域（Region）**，每个Region既可以是Eden、Survivor，也可以是老年代。

```ascii
┌────┬────┬────┬────┬────┬────┬────┬────┐
│ E  │ E  │ S  │ H  │ O  │ O  │ O  │ E  │
├────┼────┼────┼────┼────┼────┼────┼────┤
│ O  │ O  │ E  │ E  │ E  │ S  │ O  │ O  │
├────┼────┼────┼────┼────┼────┼────┼────┤
│ O  │ H  │ H  │ O  │ O  │ E  │ E  │ E  │
├────┼────┼────┼────┼────┼────┼────┼────┤
│ E  │ E  │ E  │ S  │ O  │ O  │ O  │ O  │
└────┴────┴────┴────┴────┴────┴────┴────┘
  E = Eden Region      S = Survivor Region
  O = Old Region       H = Humongous Region
```

**Region大小：** 默认将堆划分为2048个Region，每个Region大小为1MB~32MB（2的幂），由堆大小决定。

| 堆大小 | Region大小 |
|--------|-----------|
| < 4GB | 1MB |
| 4GB ~ 8GB | 2MB |
| 8GB ~ 16GB | 4MB |
| 16GB ~ 32GB | 8MB |
| 32GB ~ 64GB | 16MB |
| > 64GB | 32MB |

**Humongous对象：** 当对象大小超过Region大小的50%时，被视为"巨型对象"，直接分配到Humongous Region中（归属于老年代）。

#### 工作流程

G1的运作过程分为以下步骤：

1. **初始标记（Initial Marking, STW）**：标记GC Roots直接可达的对象。
2. **并发标记（Concurrent Marking）**：从GC Roots开始遍历对象图，耗时较长，与用户线程并发执行。
3. **最终标记（Final Marking, STW）**：处理SATB（Snapshot At The Beginning）缓冲区的剩余记录。
4. **筛选回收（Live Data Counting & Evacuation, STW）**：对各个Region的回收价值和成本进行排序，根据用户期望的停顿时间制定回收计划，然后回收价值最大的Region。

#### 可预测停顿

G1的核心优势在于**可预测的停顿时间模型**。通过`-XX:MaxGCPauseMillis`设置期望的最大停顿时间（默认200ms），G1会根据这个目标来规划每次回收哪些Region、回收多少Region。

```bash
# 启用G1收集器（JDK 9+默认）
java -XX:+UseG1GC -jar application.jar

# 设置期望最大停顿时间为100ms
java -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -jar application.jar

# 设置Region大小（可选，一般不手动设置）
java -XX:+UseG1GC -XX:G1HeapRegionSize=4m -jar application.jar

# 设置并发GC线程数（默认为Java进程CPU数量的1/4）
java -XX:+UseG1GC -XX:ConcGCThreads=4 -jar application.jar

# 触发Mixed GC的堆占用比例（默认45%）
java -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35 -jar application.jar
```

#### RSet（Remembered Set）

G1为了高效处理跨代引用，在每个Region上维护了一个RSet，记录了哪些其他Region引用了当前Region中的对象。

```ascii
Region A (Old) ──→ 引用 Region B (Eden) 中的对象
                      │
                      ↓
              Region B 的 RSet 中记录:
              "Region A 引用了本Region的对象"
```

- 在新生代回收时，只需扫描GC Roots + 老年代指向新生代的RSet，无需扫描整个老年代。
- RSet的维护：通过写屏障（Write Barrier）在对象引用变更时更新RSet。

#### SATB（Snapshot At The Beginning）

SATB是G1处理并发标记阶段对象变动问题的手段。

- 在初始标记时，为对象图拍一个"快照"——记录当前所有存活的对象。
- 在并发标记期间，所有新分配的对象都标记为黑色（存活）。
- 在并发标记期间被删除的引用，通过SATB写屏障记录到SATB缓冲区中，在最终标记阶段处理。

#### 三色标记算法

G1（以及其他并发收集器如CMS）使用三色标记法来进行并发可达性分析：

| 颜色 | 含义 | 处理方式 |
|------|------|---------|
| **白色** | 尚未被访问（不可达） | 最终如果仍是白色，表示不可达，将被回收 |
| **灰色** | 自身已被访问，但其引用的对象尚未被访问完 | 中间状态，需要继续扫描 |
| **黑色** | 自身和其引用的对象都已访问完 | 存活对象 |

```ascii
初始状态: [GC Roots] → [A(灰)] → [B(白)]
                                  [C(白)]

并发扫描中:
[GC Roots] → [A(黑)] → [B(灰)] → [D(白)]
                         [C(白)]

完成扫描:
[GC Roots] → [A(黑)] → [B(黑)] → [D(黑)]
                         [C(白)] ← 不可达，将回收
```

**并发标记的问题——对象消失：**

如果在并发标记期间，灰色对象到白色对象的引用被删除，而黑色对象新增了对该白色对象的引用，就会导致该白色对象被错误地当作垃圾回收。

**解决方案：**

- **增量更新（Incremental Update）**：CMS采用。当黑色对象新增对白色对象的引用时，将黑色对象变回灰色。
- **SATB（Snapshot At The Beginning）**：G1采用。记录并发标记开始时所有存活对象的快照，在并发标记期间被删除的引用会被记录下来，确保那些对象不会被错误回收。

```bash
# G1调优常见参数组合示例
java -Xms8g -Xmx8g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -XX:InitiatingHeapOccupancyPercent=35 \
     -XX:ConcGCThreads=4 \
     -XX:G1HeapRegionSize=4m \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -Xloggc:gc.log \
     -jar application.jar
```

---

## 4. 现代垃圾收集器

### 4.1 Shenandoah收集器

Shenandoah是Red Hat开发的一款低延迟垃圾收集器，在JDK 12中作为实验性功能引入（OpenJDK中可用，Oracle JDK中不可用）。

**核心特性：**

- **并发整理（Concurrent Compaction）**：与G1不同，Shenandoah的**整理阶段也是并发的**，这意味着GC的停顿时间与堆大小无关。
- **Brooks Pointer**：在对象头部添加一个指向自身的转发指针，用于解决并发整理时的对象访问问题。

```ascii
Shenandoah对象结构:
┌──────────────────────────────────────────────┐
│  Mark Word │ Klass Pointer │ Brooks Pointer  │  ... 数据  │
└──────────────────────────────────────────────┘
                               │
                          (指向自身或新位置)
```

**工作流程：**

1. 初始标记（STW, 极短）
2. 并发标记（并发）
3. 最终标记（STW, 短）
4. 并发清理（并发，回收无存活对象的Region）
5. 并发回收（并发，将存活对象复制到新Region）
6. 初始引用更新（STW, 短）
7. 并发引用更新（并发，更新所有引用）
8. 最终引用更新（STW, 短）
9. 并发清理（并发）

```bash
# 启用Shenandoah（JDK 12+）
java -XX:+UseShenandoahGC -jar application.jar

# Shenandoah参数
java -XX:+UseShenandoahGC \
     -XX:ShenandoahGarbageThreshold=15 \   # Region垃圾占比阈值
     -XX:ShenandoahUncommitDelay=60000 \   # 内存归还延迟
     -jar application.jar
```

### 4.2 ZGC收集器

ZGC（The Z Garbage Collector）是Oracle开发的实验性低延迟垃圾收集器，在JDK 11中引入，JDK 15中正式转正，JDK 17+的LTS版本中已成为服务器端的有力竞争者。

**核心特性：**

- **STW停顿时间极短**：一般不超过1ms（与堆大小无关）。
- **支持TB级堆**：可处理数TB的大堆。
- **并发所有阶段**：几乎所有阶段都是并发的（初始标记、重映射等少量STW阶段时间极短）。
- **染色指针（Colored Pointer）**：在指针的64位地址中编码GC元数据信息。

#### 染色指针技术

传统GC需要在对象头中标记GC状态，而ZGC的染色指针技术将GC状态信息直接编码在指针中：

```ascii
64位指针布局（AMD64）:
┌──┬──┬──┬──┬──┬──┬──┬──┬──────────────────────────────┐
│45│44│43│42│ 41 .. 0  │                   │
│  │  │  │  │          │                   │
│M0│M1│ R│ 0│  地址位  │
└──┴──┴──┴──┴──────────┴──────────────────────────────┘

M0: 标记位0（Mark 0）
M1: 标记位1（Mark 1）
R:  重映射位（Remapped）
0:  保留位（始终为0，用于指针压缩）
```

- 通过指针中的3个标志位（M0、M1、Remapped），ZGC在不修改对象头的情况下实现对象的状态管理。
- 由于染色指针的存在，ZGC只能管理4TB堆（42位地址空间）。

**ZGC的工作流程：**

1. **并发标记（Concurrent Mark）**：遍历对象图并更新染色指针的标记位。
2. **并发预备重分配（Concurrent Prepare for Relocation）**：确定哪些Region需要整理。
3. **并发重分配（Concurrent Relocation）**：将存活对象复制到新Region，使用**读屏障（Load Barrier）**确保用户线程可以访问到正确的对象副本。

#### 读屏障（Load Barrier）

ZGC在读操作时插入屏障，而不是在写操作时：

```java
// ZGC读屏障工作示意
// 当用户线程读取一个对象引用时，ZGC会插入一个读屏障
Object obj = someObject.field;  // ← ZGC读屏障在此处触发

// 如果对象已被移动（重分配），读屏障会：
// 1. 自动修复引用（指向新地址）
// 2. 返回新地址的对象
```

```bash
# 启用ZGC（JDK 11+实验性，JDK 15+正式）
java -XX:+UseZGC -jar application.jar

# ZGC参数
java -XX:+UseZGC \
     -Xms16g -Xmx16g \
     -XX:ZAllocationSpikeTolerance=2.0 \   # 分配波动容忍度
     -XX:ZCollectionInterval=300 \          # 最大GC间隔（秒）
     -XX:ZFragmentationLimit=25 \           # 最大碎片率（%）
     -jar application.jar

# 并发线程数（默认CPU数量的1/2）
java -XX:+UseZGC -XX:ConcGCThreads=4 -jar application.jar
```

**ZGC vs Shenandoah vs G1对比：**

| 特性 | G1 | Shenandoah | ZGC |
|------|----|-----------|-----|
| 停顿目标 | ~200ms | <10ms | <1ms |
| 整理阶段 | 部分并发 | 完全并发 | 完全并发 |
| 屏障类型 | 写屏障（RSet维护）+ SATB | 写屏障（Brooks Pointer） | 读屏障（染色指针） |
| 最大堆 | 约64GB | 约512GB | 4TB（染色指针限制） |
| JDK版本 | JDK 7+ | JDK 12+ | JDK 11+(实验)/JDK 15+(正式) |

---

## 5. 类加载机制

JVM将描述类的数据从Class文件加载到内存，并对数据进行校验、转换解析和初始化，最终形成可以被JVM直接使用的Java类型，这个过程称为**类加载机制**。

### 5.1 类加载的生命周期

一个类型从被加载到JVM内存中开始，到卸载出内存为止，整个生命周期经历以下七个阶段：

```ascii
加载 → 验证 → 准备 → 解析 → 初始化 → 使用 → 卸载
│                                    │
└──────── 连接（Linking）─────────────┘
```

**解析阶段在特定情况下可以在初始化之后开始**，这称为**动态绑定**或**晚期解析**。

#### 5.1.1 加载（Loading）

加载是类加载的第一个阶段，主要完成三件事：

1. 通过类的全限定名获取定义此类的二进制字节流。
2. 将字节流所代表的静态存储结构转化为方法区的运行时数据结构。
3. 在内存中生成代表该类的 `java.lang.Class` 对象，作为方法区该类各种数据的访问入口。

#### 5.1.2 验证（Verification）

验证是连接阶段的第一步，目的是确保Class文件的字节流中包含的信息符合JVM规范要求，不会危害JVM自身安全。

包含四个校验动作：

| 校验阶段 | 说明 |
|---------|------|
| 文件格式验证 | 验证字节流是否符合Class文件格式规范（魔数0xCAFEBABE、主次版本号等） |
| 元数据验证 | 对字节码描述的信息进行语义分析（是否有父类、是否继承了final类等） |
| 字节码验证 | 通过数据流和控制流分析，确定程序语义是合法的、符合逻辑的 |
| 符号引用验证 | 对类自身以外的信息（常量池中的符号引用）进行匹配性校验 |

#### 5.1.3 准备（Preparation）

正式为类中定义的**静态变量**分配内存并设置**类变量初始值**的阶段。

```java
public class PreparationDemo {
    public static int value = 123;
    // 在准备阶段，value被设置为0（默认初始值）
    // 在初始化阶段的<clinit>中，value被设置为123

    public static final int CONST = 456;
    // 对于final static常量，准备阶段直接设置为456（ConstantValue属性）
}
```

| 变量类型 | 准备阶段值 | 说明 |
|---------|-----------|------|
| `public static int value = 123` | 0 | 基本类型默认值，真正赋值在初始化阶段 |
| `public static final int CONST = 456` | 456 | final常量在准备阶段直接赋值 |
| `public static Object obj = new Object()` | null | 引用类型默认值为null |

#### 5.1.4 解析（Resolution）

将常量池内的**符号引用**替换为**直接引用**的过程。

- **符号引用**：以一组符号来描述目标，可以是任意字面量，但必须能无歧义地定位到目标。
- **直接引用**：直接指向目标的指针、相对偏移量或一个能间接定位到目标的句柄。

**解析的目标：** 类或接口、字段、方法、方法类型、方法句柄、调用点限定符。

#### 5.1.5 初始化（Initialization）

类加载过程的最后一个阶段。在这个阶段，JVM才真正开始执行类中编写的Java程序代码——**执行 `<clinit>()` 类构造器方法**。

**`<clinit>()` 方法的特点：**

- 由编译器自动收集类中的所有**静态变量的赋值动作**和**静态语句块（static block）**合并而成。
- 收集顺序由语句在源文件中出现的顺序决定。
- 虚拟机会保证在子类的 `<clinit>()` 执行之前，父类的 `<clinit>()` 已经执行完毕。
- 如果一个类没有静态变量赋值也没有静态语句块，则不会生成 `<clinit>()`。

```java
public class InitDemo {
    static {
        System.out.println("父类静态块执行");       // (1) 先执行
        value = 2;                                 // 可以赋值，但不能访问（非法向前引用）
    }
    public static int value = 1;                   // (2) 后执行，覆盖为1

    public static class Child extends InitDemo {
        static {
            System.out.println("子类静态块执行");    // (3) 最后执行
        }
    }
}
```

**主动引用（触发初始化）的六种情况：**

| 场景 | 示例 | 是否触发初始化 |
|------|------|--------------|
| new对象 | `new Demo()` | 是 |
| 访问静态字段 | `Demo.staticField` | 是 |
| 调用静态方法 | `Demo.staticMethod()` | 是 |
| 反射调用 | `Class.forName("Demo")` | 是 |
| 初始化子类 | `new Child()`时父类未初始化 | 父类会初始化 |
| 主类 | 包含main()方法的类 | 是 |
| MethodHandle | 调用MethodHandle时 | 是 |

**被动引用（不触发初始化）的示例：**

```java
// 示例1: 通过子类引用父类的静态字段，不会触发子类初始化
public class PassiveRefDemo {
    public static void main(String[] args) {
        System.out.println(Child.value); // 只触发Parent的初始化
    }
}

// 示例2: 通过数组定义引用类，不会触发初始化
Parent[] array = new Parent[10]; // 触发的是[LParent;类的初始化

// 示例3: 引用final常量，不会触发初始化
System.out.println(Child.CONST); // 在编译期放入常量池
```

### 5.2 类加载器

#### 5.2.1 三层类加载器体系

JDK 8及以前：

```ascii
┌─────────────────────────────────────┐
│   Bootstrap ClassLoader (C++实现)    │  ← 加载 rt.jar, jre/lib/
│   └── jre/lib/rt.jar, charset.jar   │
├─────────────────────────────────────┤
│   Extension ClassLoader (Java实现)   │  ← 加载 jre/lib/ext/
│   └── jre/lib/ext/*.jar             │
├─────────────────────────────────────┤
│   Application ClassLoader (Java实现) │  ← 加载 classpath
│   └── classpath 指定目录              │
├─────────────────────────────────────┤
│   自定义 ClassLoader                 │  ← 用户自定义
│   └── 自定义加载路径                  │
└─────────────────────────────────────┘
```

JDK 9+模块化后，类加载机制有所调整（引入Platform ClassLoader替代Extension ClassLoader）。

#### 5.2.2 双亲委派模型（Parent Delegation Model）

**工作流程：**

当一个类加载器收到类加载请求时，它首先不会自己去尝试加载这个类，而是把这个请求委派给父类加载器去完成，每一个层次的类加载器都是如此，因此所有的加载请求最终都应该传送到最顶层的启动类加载器中。只有当父类加载器反馈自己无法完成这个加载请求（它的搜索范围中没有找到所需的类）时，子加载器才会尝试自己去完成加载。

```java
// 双亲委派的核心实现（java.lang.ClassLoader.loadClass()）
protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
        // 首先，检查该类是否已经被加载
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                if (parent != null) {
                    // 委派给父类加载器
                    c = parent.loadClass(name, false);
                } else {
                    // 如果没有父加载器，委托给Bootstrap ClassLoader
                    c = findBootstrapClassOrNull(name);
                }
            } catch (ClassNotFoundException e) {
                // 父类加载器无法加载
            }
            if (c == null) {
                // 父类加载器无法加载时，自己尝试加载
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

**为什么使用双亲委派？——安全 + 避免重复加载：**

1. **安全性**：防止核心API被篡改。例如，自定义一个 `java.lang.String` 类，由于双亲委派机制，加载请求会委派给Bootstrap ClassLoader，Bootstrap ClassLoader发现已经加载了标准的 `java.lang.String`，就不会再加载自定义的String，从而保证了核心类库的安全性。
2. **避免重复加载**：同一个类不会被不同的类加载器重复加载。

```java
// 演示双亲委派的安全作用
public class ClassLoaderSecurityDemo {
    public static void main(String[] args) throws Exception {
        // 自定义的java.lang.String类永远不会被加载
        // 因为Bootstrap ClassLoader会优先加载rt.jar中的标准String
        Class<?> stringClass = Class.forName("java.lang.String");
        System.out.println(stringClass.getClassLoader()); // null (Bootstrap ClassLoader)

        // 查看类的加载器
        ClassLoader cl = ClassLoaderSecurityDemo.class.getClassLoader();
        System.out.println(cl); // AppClassLoader

        // 查看父加载器
        System.out.println(cl.getParent()); // ExtClassLoader
        System.out.println(cl.getParent().getParent()); // null (Bootstrap)
    }
}
```

#### 5.2.3 破坏双亲委派模型

在某些场景下，双亲委派模型需要被打破。

**场景1：Tomcat的WebAppClassLoader**

Tomcat需要为每个Web应用提供独立的类加载器，以实现应用隔离。WebAppClassLoader的加载顺序**优先加载自己Web应用中的类**，打破了双亲委派模型。

```ascii
Tomcat类加载器体系:
┌─────────────────────────────────────┐
│        Bootstrap ClassLoader         │
├─────────────────────────────────────┤
│        Extension ClassLoader         │
├─────────────────────────────────────┤
│        Application ClassLoader       │
├─────────────────────────────────────┤
│        Common ClassLoader            │  ← 加载Tomcat自身和所有应用共享的类
├─────────┬─────────┬─────────┬───────┤
│ WebApp  │ WebApp  │ WebApp  │ JSP   │
│CL for   │CL for   │CL for   │CL     │
│App1     │App2     │App3     │       │
└─────────┴─────────┴─────────┴───────┘
```

**场景2：SPI（Service Provider Interface）**

JDK的SPI机制（如JDBC DriverManager）需要加载由第三方厂商提供的实现类，这些实现类在classpath中，只能由Application ClassLoader加载。但加载SPI接口的却是Bootstrap ClassLoader。

解决方案：**线程上下文类加载器（Thread Context ClassLoader）**：

```java
// 线程上下文类加载器使用示例
public class SPIDemo {
    public static void main(String[] args) {
        // DriverManager是Bootstrap ClassLoader加载的
        // 但它通过线程上下文类加载器加载具体驱动实现
        ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class);
        for (Driver driver : drivers) {
            System.out.println(driver.getClass().getClassLoader());
        }
    }
}

// 线程上下文类加载器的默认设置
// Thread.currentThread().getContextClassLoader() 默认返回 ApplicationClassLoader
```

### 5.3 类的唯一性

在JVM中，**类的唯一性由"加载它的类加载器"和"类的全限定名"共同决定**。

```java
public class ClassUniquenessDemo {
    public static void main(String[] args) throws Exception {
        // 自定义类加载器
        ClassLoader myLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                try {
                    String fileName = name.substring(name.lastIndexOf(".") + 1) + ".class";
                    InputStream is = getClass().getResourceAsStream(fileName);
                    if (is == null) {
                        // 对系统类使用双亲委派
                        return super.loadClass(name);
                    }
                    byte[] b = new byte[is.available()];
                    is.read(b);
                    return defineClass(name, b, 0, b.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name);
                }
            }
        };

        // 使用自定义类加载器加载ClassUniquenessDemo
        Object obj = myLoader.loadClass("jvm.ClassUniquenessDemo").newInstance();
        System.out.println(obj.getClass());  // class jvm.ClassUniquenessDemo
        System.out.println(obj instanceof jvm.ClassUniquenessDemo);  // false!
        // 因为obj.getClass()和ClassUniquenessDemo.class由不同的类加载器加载
        // 在JVM中是两个不同的Class对象
    }
}
```

---

## 6. JVM参数与调优

### 6.1 堆内存参数

| 参数 | 说明 | 建议 |
|------|------|------|
| `-Xms<size>` | 初始堆大小 | 生产环境建议与-Xmx设相同 |
| `-Xmx<size>` | 最大堆大小 | 根据应用需求设定，不超过物理内存的70% |
| `-Xmn<size>` | 新生代大小 | 一般设为堆大小的1/3~1/4 |
| `-XX:NewRatio` | 老年代/新生代比例 | 默认2（老年代是新生代的2倍） |
| `-XX:SurvivorRatio` | Eden/Survivor比例 | 默认8（Eden: S0: S1 = 8:1:1） |

```bash
# 堆内存参数配置示例
java -Xms4g -Xmx4g \
     -Xmn1536m \
     -XX:NewRatio=2 \
     -XX:SurvivorRatio=8 \
     -jar application.jar
```

### 6.2 元空间参数

| 参数 | 说明 | 建议 |
|------|------|------|
| `-XX:MetaspaceSize` | 元空间初始大小（触发GC的阈值） | 建议256m~512m |
| `-XX:MaxMetaspaceSize` | 元空间最大大小 | 必须设置，防止无限膨胀 |
| `-XX:CompressedClassSpaceSize` | 压缩类空间大小 | 默认1g |

```bash
# 元空间参数配置
java -XX:MetaspaceSize=256m \
     -XX:MaxMetaspaceSize=512m \
     -jar application.jar
```

### 6.3 GC日志参数

#### JDK 8及以前

```bash
java -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -XX:+PrintHeapAtGC \
     -XX:+PrintTenuringDistribution \
     -XX:+PrintGCApplicationStoppedTime \
     -Xloggc:gc-%t.log \
     -XX:+UseGCLogFileRotation \
     -XX:NumberOfGCLogFiles=14 \
     -XX:GCLogFileSize=50m \
     -jar application.jar
```

#### JDK 9及以后（统一日志系统）

```bash
java -Xlog:gc*:file=gc.log:tags,time,uptime,level:filecount=10,filesize=50m \
     -jar application.jar

# GC日志各部分说明
-Xlog:gc*                                      # 所有GC相关日志
        :file=gc.log                           # 输出到文件
        :tags,time,uptime,level                # 格式：标签、时间、运行时长、级别
        :filecount=10,filesize=50m             # 日志轮转：10个文件，每个50MB

# 更精细的日志级别
-Xlog:gc+heap=debug                            # 堆信息
-Xlog:gc+ref=debug                             # 引用处理
-Xlog:gc+age=trace                             # 对象年龄
-Xlog:gc+ergo=trace                            # 自适应调节
```

**GC日志分析工具——GCeasy：**

推荐使用 https://gceasy.io 来分析GC日志，它可以自动生成可视化报告，包括：
- 吞吐量分析
- 停顿时间分析
- 堆使用趋势
- GC原因分析
- 优化建议

### 6.4 常用调优思路

JVM调优没有银弹，需要根据应用特点（响应时间优先 vs 吞吐量优先）和硬件配置来制定策略。以下是一般调优步骤：

#### 步骤一：明确目标

| 应用类型 | 目标 | 推荐收集器 |
|---------|------|-----------|
| 批处理、离线计算 | 高吞吐量 | Parallel Scavenge + Parallel Old |
| Web服务、API网关 | 低响应时间 | G1（JDK 9+默认）、ZGC（超低延迟） |
| 客户端应用 | 低内存占用 | Serial |

#### 步骤二：确定堆大小

```bash
# 服务器端典型配置（8C16G物理机）
java -Xms8g -Xmx8g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -jar application.jar
```

**经验法则：**
- 堆大小不超过物理内存的70%（留出给操作系统、元空间、直接内存、线程栈等）。
- 如果使用G1，堆大小建议在4GB~32GB之间。
- 如果使用ZGC，堆大小可以更大（TB级）。

#### 步骤三：调节代比例

```bash
# 针对对象创建频繁的应用，适当增大新生代
java -Xms8g -Xmx8g -Xmn3g -jar application.jar

# 针对大对象较多的应用，适当增大老年代
java -Xms8g -Xmx8g -XX:NewRatio=3 -jar application.jar
```

#### 步骤四：G1收集器调优

```bash
# G1调优标准配置
java -Xms8g -Xmx8g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \           # 目标停顿时间
     -XX:InitiatingHeapOccupancyPercent=45 \ # 触发并发标记的堆占用率
     -XX:G1ReservePercent=10 \             # 预留空间比例
     -XX:+ParallelRefProcEnabled \         # 并行处理引用
     -XX:-ResizePLAB \                     # 禁用PLAB大小调整（减少波动）
     -XX:+AlwaysPreTouch \                 # 启动时预申请物理内存
     -jar application.jar
```

#### 步骤五：内存泄漏排查

```bash
# 开启堆转储
-XX:+HeapDumpOnOutOfMemoryError           # OOM时自动dump堆
-XX:HeapDumpPath=/path/to/dumps/          # dump文件路径
-XX:+PrintClassHistogram                  # 打印类直方图

# 启用NMT（Native Memory Tracking）
-XX:NativeMemoryTracking=summary          # 追踪本地内存
# 查看NMT信息
jcmd <pid> VM.native_memory summary
```

```java
// 内存泄漏排查示例：使用VisualVM或MAT分析堆转储
public class MemoryLeakDemo {
    // 典型内存泄漏：静态集合类持有对象引用
    private static final List<byte[]> LEAK = new ArrayList<>();

    public void leak() {
        while (true) {
            LEAK.add(new byte[1024 * 1024]); // 不断向静态集合中添加对象
            try { Thread.sleep(100); } catch (InterruptedException e) { }
        }
    }

    public static void main(String[] args) {
        new MemoryLeakDemo().leak();
        // 使用 -XX:+HeapDumpOnOutOfMemoryError 捕获堆转储
        // 用MAT分析可以看到LEAK集合占据了几乎全部堆内存
    }
}
```

#### 步骤六：监控和持续调优

```bash
# JDK自带监控工具
jps          # 查看Java进程
jstat -gc <pid> 5000  # 每5秒查看GC统计
jmap -heap <pid>     # 查看堆配置和使用情况
jstack <pid>         # 查看线程栈（排查死锁、线程等待）
jcmd <pid> help      # 万能命令

# 生产环境推荐使用
# - JMX + Prometheus + Grafana
# - JDK Flight Recorder (JFR)
# - Arthas（阿里开源诊断工具）
```

---

## 7. GC参数速查表

### 7.1 堆与代参数

| 参数 | 默认值 | 说明 |
|------|-------|------|
| `-Xms` | 物理内存1/64 | 初始堆大小 |
| `-Xmx` | 物理内存1/4 | 最大堆大小 |
| `-Xmn` | 堆的1/3~1/4 | 新生代大小 |
| `-XX:NewRatio` | 2 | 老年代/新生代 |
| `-XX:SurvivorRatio` | 8 | Eden/Survivor |
| `-XX:MaxTenuringThreshold` | 15 | 晋升老年代年龄阈值 |
| `-XX:PretenureSizeThreshold` | 0（无限制） | 直接晋升老年代的对象大小阈值 |
| `-XX:+UseTLAB` | true | 启用TLAB |

### 7.2 元空间参数

| 参数 | 说明 |
|------|------|
| `-XX:MetaspaceSize` | 元空间触发GC的阈值（默认约21MB） |
| `-XX:MaxMetaspaceSize` | 元空间最大大小 |
| `-XX:CompressedClassSpaceSize` | 压缩类空间大小 |
| `-XX:+UseCompressedClassPointers` | 启用类指针压缩 |

### 7.3 垃圾收集器选择参数

| 收集器组合 | 参数 |
|-----------|------|
| Serial + Serial Old | `-XX:+UseSerialGC` |
| ParNew + CMS | `-XX:+UseConcMarkSweepGC` |
| Parallel Scavenge + Parallel Old | `-XX:+UseParallelGC` 或 `-XX:+UseParallelOldGC` |
| G1 | `-XX:+UseG1GC` |
| Shenandoah | `-XX:+UseShenandoahGC`（JDK 12+） |
| ZGC | `-XX:+UseZGC`（JDK 15+） |

### 7.4 G1专属参数

| 参数 | 默认值 | 说明 |
|------|-------|------|
| `-XX:MaxGCPauseMillis` | 200 | 期望最大停顿时间（ms） |
| `-XX:G1HeapRegionSize` | 自动 | Region大小（1MB~32MB） |
| `-XX:InitiatingHeapOccupancyPercent` | 45 | 触发并发标记的堆占用率(%) |
| `-XX:ConcGCThreads` | CPU数量的1/4 | 并发GC线程数 |
| `-XX:G1ReservePercent` | 10 | 预留空间比例(%) |
| `-XX:G1NewSizePercent` | 5 | 新生代初始大小占堆比例(%) |
| `-XX:G1MaxNewSizePercent` | 60 | 新生代最大占堆比例(%) |
| `-XX:ParallelGCThreads` | CPU数量 | STW时工作的线程数 |
| `-XX:+UnlockExperimentalVMOptions` | - | 解锁实验性参数 |

### 7.5 GC日志参数

| 参数（JDK 9+） | 说明 |
|--------------|------|
| `-Xlog:gc` | 基础GC日志 |
| `-Xlog:gc*` | 所有GC详细日志 |
| `-Xlog:gc+heap=debug` | 堆信息 |
| `-Xlog:gc+age=trace` | 对象年龄 |
| `-Xlog:gc+ergo=trace` | 自适应调节 |
| `-Xlog:gc*:file=gc.log` | 输出到文件 |
| `-Xlog:gc*:file=gc.log:tags,time,uptime` | 带格式输出 |
| `-Xlog:gc*:file=gc.log:filecount=10,filesize=50m` | 日志轮转 |

### 7.6 OOM与诊断参数

| 参数 | 说明 |
|------|------|
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM时自动dump堆 |
| `-XX:HeapDumpPath=<path>` | dump文件存放路径 |
| `-XX:OnOutOfMemoryError=<cmd>` | OOM时执行脚本 |
| `-XX:+PrintClassHistogram` | 打印类直方图 |
| `-XX:NativeMemoryTracking=summary` | 本地内存追踪 |
| `-XX:+UnlockDiagnosticVMOptions` | 解锁诊断参数 |
| `-XX:+PrintFlagsFinal` | 打印所有JVM参数最终值 |

---

## 8. 故障排查清单

### 8.1 OOM（OutOfMemoryError）排查

```
□ 确认OOM信息类型：
   - Java heap space       → 堆内存不足
   - Metaspace             → 元空间不足
   - Direct buffer memory  → 直接内存不足
   - GC overhead limit exceeded → GC频繁但回收效果差
   - Unable to create new native thread → 线程数超限
   - Requested array size exceeds VM limit → 数组过大

□ 使用 -XX:+HeapDumpOnOutOfMemoryError 获取堆转储

□ 使用MAT/Eclipse Memory Analyzer 分析dump文件：
   - 找出最大的对象（Dominator Tree）
   - 找出GC Roots引用链（Path to GC Roots）
   - 检查可疑的集合类（HashMap、ArrayList、ThreadLocal等）

□ 常见原因：
   - 静态集合类持有对象引用（内存泄漏）
   - 未关闭的资源（数据库连接、文件流、Socket）
   - ThreadLocal未调用remove()
   - 大对象直接进入老年代
   - 元空间中动态生成类（CGLIB、反射）
```

### 8.2 频繁GC排查

```
□ 确认GC频率和耗时：
   - jstat -gcutil <pid> 1000 （每秒查看GC统计）
   - 分析GC日志，关注Young GC和Full GC的频率、耗时

□ Young GC频繁的排查：
   - 使用 jmap -histo:live <pid> 查看存活对象
   - 检查是否新生代空间设置过小（-Xmn）
   - 检查是否对象过早晋升（查看GC日志中的晋升统计）
   - 分析对象创建速率是否过高

□ Full GC频繁的排查：
   - 检查老年代占用率是否过高
   - 检查是否存在内存泄漏（堆转储分析）
   - 检查CMS的InitiatingOccupancyFraction是否设置过低
   - 检查G1的InitiatingHeapOccupancyPercent是否设置过低
   - 检查是否频繁发生Concurrent Mode Failure（CMS）
   - 检查是否频繁发生Humongous Allocation（G1）

□ 调优方向：
   - 增大堆大小
   - 调整新生代/老年代比例
   - 更换更合适的垃圾收集器
   - 修复内存泄漏
```

### 8.3 高CPU排查

```
□ 找出CPU消耗高的线程：
   top -H -p <pid>           （Linux）
   jstack <pid> > stack.log  （获取线程栈）
   将高CPU线程的tid转成16进制，在stack.log中搜索

□ 常见原因：
   - 频繁的Full GC（GC线程消耗CPU）
   - 死循环或大量计算
   - 频繁的锁竞争（线程自旋）
   - 大量对象分配（分配线程消耗CPU）

□ 排查工具：
   - arthas：thread -n 3 （显示CPU占用最高的3个线程）
   - async-profiler：生成CPU火焰图
   - VisualVM：实时监控CPU使用
```

### 8.4 死锁排查

```
□ 使用jstack查找死锁：
   jstack -l <pid>
   输出中搜索 "Found one Java-level deadlock"

□ 使用arthas：
   thread -b  （查看阻塞的线程）

□ 使用JMX：
   ManagementFactory.getThreadMXBean().findDeadlockedThreads()

□ 预防措施：
   - 避免嵌套锁
   - 使用显式锁的tryLock()设置超时
   - 保持锁的顺序一致
   - 尽量使用java.util.concurrent包中的并发工具
```

### 8.5 应用启动慢或卡顿排查

```
□ 检查类加载情况：
   - -XX:+TraceClassLoading 查看类加载日志
   - 检查是否存在大量的类加载（动态代理生成类过多）

□ 检查编译情况：
   - -XX:+PrintCompilation 查看JIT编译日志
   - 检查是否有方法编译时间过长

□ 检查安全设置：
   - 检查JCE策略文件是否完整
   - 检查SecurityManager配置

□ 检查IO和网络：
   - 使用 iostat 查看磁盘IO
   - 使用 netstat 查看网络连接
   - 检查DNS解析是否正常
```

### 8.6 JVM参数最佳实践模板

```bash
# G1收集器配置（8C16G典型应用服务器）
java -Xms8g -Xmx8g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -XX:InitiatingHeapOccupancyPercent=45 \
     -XX:ConcGCThreads=4 \
     -XX:ParallelGCThreads=8 \
     -XX:G1ReservePercent=10 \
     -XX:+ParallelRefProcEnabled \
     -XX:+AlwaysPreTouch \
     -XX:MetaspaceSize=256m \
     -XX:MaxMetaspaceSize=512m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heapdump.hprof \
     -Xlog:gc*:file=/var/log/app/gc.log:tags,time,uptime:filecount=10,filesize=50m \
     -jar application.jar

# 低延迟ZGC配置（16C32G低延迟应用）
java -Xms16g -Xmx16g \
     -XX:+UseZGC \
     -XX:ConcGCThreads=4 \
     -XX:MetaspaceSize=256m \
     -XX:MaxMetaspaceSize=512m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heapdump.hprof \
     -Xlog:gc*:file=/var/log/app/gc.log:tags,time,uptime:filecount=10,filesize=50m \
     -jar application.jar

# 高吞吐量Parallel配置（批量处理应用）
java -Xms16g -Xmx16g \
     -XX:+UseParallelGC \
     -XX:+UseParallelOldGC \
     -XX:MaxGCPauseMillis=500 \
     -XX:GCTimeRatio=19 \
     -XX:+UseAdaptiveSizePolicy \
     -XX:MetaspaceSize=256m \
     -XX:MaxMetaspaceSize=512m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heapdump.hprof \
     -Xlog:gc*:file=/var/log/app/gc.log:tags,time,uptime:filecount=10,filesize=50m \
     -jar application.jar
```

---

## 总结

JVM内存管理与垃圾回收是Java开发者必须深入掌握的核心知识。理解JVM的内存分区结构（堆、栈、方法区、程序计数器、直接内存），掌握各种垃圾收集算法和收集器的工作机制（从Serial到ZGC），熟知类加载的双亲委派模型及其被破坏的场景，能够根据应用特点进行合理的JVM参数调优——这些是构建高可靠、高性能Java应用的基础。

在实际工作中，建议开发者：

1. **理解原理优先**：先透彻理解内存模型和GC算法原理，再学习具体工具的使用。
2. **监控先行**：在生产环境上线前，做好监控和GC日志收集，这是调优的基础。
3. **小步调优**：每次只调整一个参数，观察效果，避免同时调整多个参数导致无法判断影响。
4. **警惕过度调优**：大多数应用使用默认参数（尤其是G1）即可获得不错的表现，过度调优往往适得其反。
5. **持续学习**：JVM技术仍在不断发展（Project Lilliput、Project Valhalla等），保持对新技术的学习和关注。

---

> **扩展阅读：**
> - 《深入理解Java虚拟机（第3版）》——周志明
> - Oracle官方JVM调优指南：https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/
> - GCeasy在线分析：https://gceasy.io
> - JDK 17 GC变化：https://openjdk.org/projects/jdk/17/
