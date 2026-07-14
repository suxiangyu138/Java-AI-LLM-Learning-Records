Java虚拟机（JVM）期末复习实战项目
一、项目说明（适配期末复习，贴合考点）
1.1 项目核心目标
本项目专为JVM期末复习设计，不追求复杂业务，重点贴合期末考试高频考点，通过“理论+实战”的方式，帮你巩固以下核心知识点，同时能直接作为期末复习项目提交（可根据学校要求微调）：
JVM内存模型（方法区、堆、虚拟机栈、本地方法栈、程序计数器）
垃圾回收（GC）核心机制（可达性分析、垃圾回收算法、常用收集器）
类加载机制（加载、验证、准备、解析、初始化）
JVM参数配置与调优（基础参数、GC日志分析）
JVM常见问题排查（内存溢出、内存泄漏）
1.2 项目环境要求（极简，易搭建）
JDK版本：JDK 8（期末复习重点，兼容性强，大部分考点基于JDK8）
开发工具：IntelliJ IDEA（或Eclipse，任选）
依赖：无额外依赖（纯JDK原生API，避免复杂配置，聚焦JVM本身）
1.3 项目难度与时长
难度：入门-中级（贴合期末考题难度，不涉及底层源码深度开发）
时长：1-2天（可完成核心功能，适合期末紧急复习+项目提交）
二、项目整体设计（模块化，贴合考点）
项目名称：JVMReviewDemo（JVM复习演示项目）
整体结构：分为5个核心模块，每个模块对应一个JVM考点，模块间独立可运行，方便单独复习、单独调试，也可整合为完整项目提交。
模块
核心考点
功能说明
MemoryModelDemo
JVM内存模型
模拟内存区域分配，演示不同内存区域的作用、生命周期，模拟内存溢出场景
GCDemo
垃圾回收机制
模拟对象创建与回收，配置GC参数，查看GC日志，分析垃圾回收过程
ClassLoadingDemo
类加载机制
自定义类加载器，演示类加载的5个阶段，验证类初始化时机
JvmParamDemo
JVM参数配置
演示常用JVM参数配置，查看参数生效情况，分析参数对程序的影响
JvmProblemDemo
内存溢出/泄漏排查
模拟内存溢出、内存泄漏场景，演示排查思路和工具使用（jmap、jconsole）
三、核心模块实战（含完整代码+考点解析）
模块1：MemoryModelDemo（JVM内存模型演示）
3.1 考点对应
重点考查：JVM五大内存区域的划分、各区域的作用、内存溢出的场景（期末高频考题）
3.2 代码实现（可直接复制运行）
package com.jvm.review.memory;
/**
 * JVM内存模型演示：模拟五大内存区域，重点演示堆、虚拟机栈、方法区的使用
 * 考点：内存区域划分、内存溢出场景（OOM）
     */
    public class MemoryModelDemo {
    // 类变量（存储在方法区）
    private static String staticVar = "JVM Method Area";
    // 实例变量（存储在堆）
    private String instanceVar = "JVM Heap";
    // 方法（执行时，方法栈帧存储在虚拟机栈）
    public void method() {
        // 局部变量（存储在虚拟机栈的栈帧中）
        String localVar = "JVM Virtual Machine Stack";
        System.out.println("局部变量（虚拟机栈）：" + localVar);
        System.out.println("实例变量（堆）：" + instanceVar);
        System.out.println("类变量（方法区）：" + staticVar);
    }
    // 模拟虚拟机栈溢出（StackOverflowError）
    public void stackOverflow() {
        // 递归调用，不断创建栈帧，耗尽虚拟机栈内存
        stackOverflow();
    }
    // 模拟堆内存溢出（OutOfMemoryError: Java heap space）
    public void heapOverflow() {
        // 不断创建对象，耗尽堆内存
        while (true) {
            new MemoryModelDemo();
        }
    }
    public static void main(String[] args) {
        MemoryModelDemo demo = new MemoryModelDemo();
        // 演示正常内存使用
        demo.method();
        // 演示内存溢出（按需运行，运行时会抛出异常，贴合期末考点）
        // 1. 模拟虚拟机栈溢出（取消注释运行）
        // demo.stackOverflow();
        // 2. 模拟堆内存溢出（取消注释运行，需配置JVM参数：-Xms10m -Xmx10m）
        // demo.heapOverflow();
    }
    }
    3.3 考点解析（期末必背）
    程序计数器：当前线程执行的字节码行号指示器，唯一不会抛出OOM的内存区域。
    虚拟机栈：存储方法栈帧（局部变量、操作数栈等），递归过深会导致StackOverflowError；栈容量不足会导致OOM。
    本地方法栈：与虚拟机栈类似，用于执行本地（native）方法。
    堆：存储对象实例和数组，是GC的主要区域，对象过多会导致OOM（Java heap space）。
    方法区：存储类信息、常量、静态变量等，JDK8后用元空间（Metaspace）实现，元空间不足会导致OOM（Metaspace）。
    3.4 运行说明
    1. 正常运行main方法，可看到不同内存区域的变量输出；
    2. 取消stackOverflow()注释，运行会抛出StackOverflowError（虚拟机栈溢出）；
    3. 取消heapOverflow()注释，同时配置JVM参数（-Xms10m -Xmx10m），运行会抛出OutOfMemoryError（堆溢出）。
    模块2：GCDemo（垃圾回收机制演示）
    3.1 考点对应
    重点考查：可达性分析算法、垃圾回收算法（标记-清除、复制、标记-整理）、GC收集器（SerialGC、ParallelGC）、GC日志分析。
    3.2 代码实现（可直接复制运行）
    package com.jvm.review.gc;
    /**
 * 垃圾回收（GC）演示：模拟对象创建与回收，配置GC参数，分析GC日志
 * 考点：可达性分析、GC算法、GC日志解读
     */
    public class GCDemo {
    // 模拟大对象（方便观察GC效果）
    private byte[] bigObject = new byte[1024 * 1024]; // 1MB
    public static void main(String[] args) {
        // 循环创建对象，触发GC
        for (int i = 0; i < 20; i++) {
            GCDemo demo = new GCDemo();
            // 置空对象，使其成为垃圾（不可达）
            demo = null;
            // 手动触发GC（注意：System.gc()只是建议，JVM不一定执行）
            System.gc();
        }
    }
    }
    3.3 考点解析（期末必背）
    垃圾判断标准：可达性分析算法（以GC Roots为起点，不可达的对象视为垃圾），GC Roots包括：虚拟机栈中引用的对象、方法区中静态变量引用的对象等。
    核心GC算法：
    标记-清除算法：先标记垃圾，再清除，会产生内存碎片（效率低）。
    复制算法：将内存分为两块，每次使用一块，GC时复制存活对象到另一块，无碎片（适合新生代）。
    标记-整理算法：标记存活对象，将其移动到内存一端，清除剩余垃圾（适合老年代）。
    常用GC收集器（JDK8默认）：
    SerialGC（串行GC）：单线程GC，适合小型程序，简单高效。
    ParallelGC（并行GC）：多线程GC，注重吞吐量（默认使用）。
    3.4 运行配置与日志分析（期末重点）
    1. 配置JVM参数（在IDEA中配置Run/Debug Configurations）：
    -Xms20m -Xmx20m -XX:+PrintGCDetails -XX:+UseSerialGC
    参数说明：
    -Xms20m：初始堆内存20MB
    -Xmx20m：最大堆内存20MB
    -XX:+PrintGCDetails：打印详细GC日志
    -XX:+UseSerialGC：使用串行GC收集器
    2. 运行程序，查看控制台GC日志，重点关注：
    GC前/后内存使用情况（used、free）
    GC耗时（real）
    新生代、老年代的内存变化
    模块3：ClassLoadingDemo（类加载机制演示）
    3.1 考点对应
    重点考查：类加载的5个阶段（加载、验证、准备、解析、初始化）、类加载器的层级（ Bootstrap、Extension、Application）、自定义类加载器。
    3.2 代码实现（可直接复制运行）
    package com.jvm.review.classloading;
    /**
 * 类加载机制演示：自定义类加载器，演示类加载的5个阶段
 * 考点：类加载流程、类加载器层级、类初始化时机
     */
    // 待加载的测试类
    class TestClass {
    // 静态代码块（类初始化阶段执行）
    static {
        System.out.println("TestClass 静态代码块执行（类初始化）");
    }
    public TestClass() {
        System.out.println("TestClass 构造方法执行（对象实例化）");
    }
    }
    // 自定义类加载器（继承ClassLoader）
    class CustomClassLoader extends ClassLoader {
    // 重写findClass方法，实现自定义类加载逻辑
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            // 简化逻辑：直接加载当前类路径下的类（模拟自定义加载）
            byte[] classData = loadClassData(name);
            // 调用defineClass方法，将字节码转换为Class对象（加载阶段核心）
            return defineClass(name, classData, 0, classData.length);
        } catch (Exception e) {
            throw new ClassNotFoundException(name);
        }
    }
    // 模拟加载类的字节码（实际中可从文件、网络加载）
    private byte[] loadClassData(String className) throws Exception {
        // 简化：获取类的字节码（这里直接使用系统类加载器的逻辑）
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        InputStream is = systemClassLoader.getResourceAsStream(className.replace(".", "/") + ".class");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = is.read()) != -1) {
            baos.write(b);
        }
        return baos.toByteArray();
    }
    }
    public class ClassLoadingDemo {
    public static void main(String[] args) throws Exception {
        // 1. 获取类加载器层级
        ClassLoader classLoader = ClassLoadingDemo.class.getClassLoader();
        System.out.println("当前类加载器（Application ClassLoader）：" + classLoader);
        System.out.println("父类加载器（Extension ClassLoader）：" + classLoader.getParent());
        System.out.println("祖父类加载器（Bootstrap ClassLoader）：" + classLoader.getParent().getParent()); // null（C++实现）
        // 2. 使用自定义类加载器加载TestClass
        CustomClassLoader customClassLoader = new CustomClassLoader();
        Class<?> testClass = customClassLoader.loadClass("com.jvm.review.classloading.TestClass");
        // 3. 实例化类（触发类初始化，静态代码块执行）
        testClass.newInstance();
        // 考点：验证类加载的初始化时机（只有当类被主动使用时才会初始化）
        System.out.println("-------------------");
        // 被动使用：仅获取类对象，不触发初始化（注释掉newInstance，静态代码块不会执行）
        Class<?> testClass2 = customClassLoader.loadClass("com.jvm.review.classloading.TestClass");
        System.out.println("仅获取类对象，未实例化，静态代码块不执行");
    }
    }
    3.3 考点解析（期末必背）
    类加载5个阶段：
    加载：通过类全限定名获取字节码，转换为Class对象。
    验证：校验字节码合法性（防止恶意字节码）。
    准备：为类变量（static）分配内存，设置默认值（如int默认0）。
    解析：将符号引用转换为直接引用（如将类名转换为内存地址）。
    初始化：执行静态代码块、为类变量赋值（真正的初始化）。
    类加载器层级（双亲委派模型）：
    Bootstrap ClassLoader（启动类加载器）：加载JDK核心类（如rt.jar），C++实现。
    Extension ClassLoader（扩展类加载器）：加载JDK扩展类（如ext目录下的类）。
    Application ClassLoader（应用类加载器）：加载用户编写的类（默认类加载器）。
    类初始化时机（主动使用才会初始化）：实例化对象、调用静态方法/变量、反射加载类等。
    模块4：JvmParamDemo（JVM参数配置演示）
    3.1 考点对应
    重点考查：常用JVM参数（堆、方法区、GC相关）、参数配置方式、参数生效验证。
    3.2 代码实现（可直接复制运行）
    package com.jvm.review.param;
    /**
 * JVM参数配置演示：查看常用JVM参数生效情况
 * 考点：JVM核心参数、参数配置与验证
     */
    public class JvmParamDemo {
    public static void main(String[] args) {
        // 获取JVM堆内存相关参数
        long maxHeap = Runtime.getRuntime().maxMemory() / 1024 / 1024; // 最大堆内存（MB）
        long totalHeap = Runtime.getRuntime().totalMemory() / 1024 / 1024; // 当前堆内存（MB）
        long freeHeap = Runtime.getRuntime().freeMemory() / 1024 / 1024; // 空闲堆内存（MB）
        System.out.println("JVM堆内存参数验证：");
        System.out.println("最大堆内存（-Xmx）：" + maxHeap + "MB");
        System.out.println("初始堆内存（-Xms）：" + totalHeap + "MB");
        System.out.println("空闲堆内存：" + freeHeap + "MB");
        // 获取方法区（元空间）相关参数（JDK8+）
        // 元空间初始大小：-XX:MetaspaceSize，最大大小：-XX:MaxMetaspaceSize
        System.out.println("\nJVM元空间参数说明：");
        System.out.println("元空间初始大小（-XX:MetaspaceSize）：默认约21MB");
        System.out.println("元空间最大大小（-XX:MaxMetaspaceSize）：默认无限制");
        // 其他常用参数说明
        System.out.println("\n常用JVM参数（期末必记）：");
        System.out.println("-Xms：初始堆内存，建议与-Xmx一致，避免频繁扩容");
        System.out.println("-Xmx：最大堆内存，决定JVM可使用的最大堆空间");
        System.out.println("-XX:MetaspaceSize：元空间初始大小");
        System.out.println("-XX:MaxMetaspaceSize：元空间最大大小");
        System.out.println("-XX:+PrintGCDetails：打印详细GC日志");
        System.out.println("-XX:+UseSerialGC：使用串行GC收集器");
        System.out.println("-XX:+UseParallelGC：使用并行GC收集器（默认）");
    }
    }
    3.3 考点解析（期末必记参数）
    堆内存参数（最常用）：
    -Xms：初始堆内存，如-Xms50m
    -Xmx：最大堆内存，如-Xmx100m（期末常考：设置为与-Xms一致，优化性能）
    元空间（方法区）参数：
    -XX:MetaspaceSize：元空间初始大小（默认约21MB）
    -XX:MaxMetaspaceSize：元空间最大大小（默认无限制，可设置为-XX:MaxMetaspaceSize=50m）
    GC相关参数：
    -XX:+PrintGCDetails：打印GC详细日志（期末常考日志分析）
    -XX:+UseSerialGC / -XX:+UseParallelGC：指定GC收集器
    3.4 运行说明
    1. 配置不同的JVM参数，运行程序，观察参数生效情况；
    2. 示例配置：-Xms50m -Xmx100m -XX:MaxMetaspaceSize=50m，运行后查看控制台输出，验证参数是否生效。
    模块5：JvmProblemDemo（JVM问题排查演示）
    3.1 考点对应
    重点考查：内存溢出（OOM）、内存泄漏的场景、排查工具（jmap、jconsole）的使用。
    3.2 代码实现（可直接复制运行）
    package com.jvm.review.problem;
    import java.util.ArrayList;
    import java.util.List;
    /**
 * JVM问题排查演示：模拟内存溢出、内存泄漏，演示排查思路
 * 考点：OOM场景、内存泄漏场景、排查工具使用
     */
    public class JvmProblemDemo {
    // 模拟内存泄漏（对象被长期引用，无法被GC回收）
    private static List<Object> leakList = new ArrayList<>();
    // 模拟内存泄漏方法
    public static void memoryLeak() {
        // 不断向列表中添加对象，列表是静态的，对象无法被回收，导致内存泄漏
        while (true) {
            leakList.add(new byte[1024 * 100]); // 每次添加100KB对象
            try {
                Thread.sleep(100); // 减缓泄漏速度，方便观察
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    // 模拟老年代内存溢出（OOM: Java heap space）
    public static void oldGenOOM() {
        // 创建大对象，直接进入老年代（JDK8默认大对象阈值为1MB）
        List<byte[]> list = new ArrayList<>();
        while (true) {
            list.add(new byte[1024 * 1024 * 2]); // 每次添加2MB大对象
        }
    }
    public static void main(String[] args) {
        // 演示内存泄漏（取消注释运行，需配置JVM参数：-Xms50m -Xmx50m）
        // memoryLeak();
        // 演示老年代内存溢出（取消注释运行，需配置JVM参数：-Xms50m -Xmx50m）
        // oldGenOOM();
    }
    }
    3.3 考点解析（期末必背）
    内存溢出（OOM）：内存不足，无法分配新对象，常见场景：堆溢出、元空间溢出、虚拟机栈溢出。
    内存泄漏：对象已经无用，但仍被引用，无法被GC回收，长期积累导致OOM（如静态集合持有对象引用）。
    排查工具（期末常考）：
    jmap：生成堆内存快照，查看对象分布，命令：jmap -dump:format=b,file=heap.hprof 进程ID
    jconsole：图形化工具，监控JVM内存、GC、线程等情况（JDK自带，直接命令行输入jconsole启动）。
    3.4 排查演示（期末实操重点）
    1. 运行memoryLeak()方法，配置参数：-Xms50m -Xmx50m；
    2. 打开命令行，输入jps获取进程ID（找到JvmProblemDemo的进程ID）；
    3. 输入jmap -dump:format=b,file=heap.hprof 进程ID，生成堆快照；
    4. 用JDK自带的jhat工具分析快照：jhat heap.hprof，访问http://localhost:7000，查看泄漏的对象（leakList中的对象）。
    四、项目整合与提交（期末适配）
    4.1 项目结构整理
    将上述5个模块整合为一个完整项目，包结构如下（规范清晰，适合提交）：
    com.jvm.review
    ├── memory          // 内存模型模块
    │   └── MemoryModelDemo.java
    ├── gc              // 垃圾回收模块
    │   └── GCDemo.java
    ├── classloading    // 类加载模块
    │   └── ClassLoadingDemo.java
    ├── param           // JVM参数模块
    │   └── JvmParamDemo.java
    └── problem         // 问题排查模块
    └── JvmProblemDemo.java
    4.2 项目提交要求（贴合期末）
    提交内容：完整代码（5个模块）+ 运行截图（每个模块的运行结果、GC日志截图）+ 考点解析（可复制本文中的考点解析）。
    运行截图要求：包含JVM参数配置、控制台输出、GC日志（如有），证明程序可正常运行。
    可选优化：添加一个Main类，整合所有模块的入口，一键运行所有演示。
    五、期末复习补充（重点必背）
    5.1 高频考点总结
    JVM内存模型五大区域的作用、异常类型（OOM、StackOverflowError）。
    垃圾回收的核心：可达性分析、三大GC算法、常用收集器的区别。
    类加载的5个阶段、双亲委派模型的作用（防止类重复加载、保证安全）。
    常用JVM参数配置、GC日志解读、内存溢出/泄漏的排查思路。
    5.2 常见考题预测
    简答题：简述JVM内存模型的五大区域及其作用。
    简答题：简述垃圾回收的可达性分析算法，以及三大GC算法的优缺点。
    实操题：配置JVM参数，模拟堆内存溢出，并使用jmap工具排查。
    编程题：编写代码，演示类加载机制或垃圾回收过程。
    六、注意事项
    本项目基于JDK8开发，贴合期末复习重点，若学校考点基于其他JDK版本，可微调代码（如JDK11的模块系统，可忽略，期末重点仍在JDK8）。
    运行内存溢出、内存泄漏相关代码时，记得配置JVM参数，否则可能因默认堆内存过大，无法快速看到效果。
    每个模块的代码均可独立运行，建议逐个模块调试，结合考点解析理解，避免直接复制粘贴不理解。
