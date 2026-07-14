# JVM 与并发编程

## 📌 课程定位
JVM是Java程序员的底层功底——理解内存模型、GC机制、类加载，才能写出高效和正确的Java代码。并发编程是大厂面试的分水岭。

## 🎯 核心章节

### 第一部分：JVM

### 1. JVM 内存模型（⭐ 必会区域划分）
```
线程共享：[堆(Heap)]
         [方法区/元空间(Metaspace)] ← JDK8+移出堆，用本地内存
线程私有：[虚拟机栈(VM Stack)] ← 栈帧(局部变量表+操作数栈+动态链接+返回地址)
         [本地方法栈(Native Method Stack)]
         [程序计数器(PC Register)]
```
- **堆**：对象实例存放地，GC主战场——分新生代(Eden+S0+S1)和老年代
- **栈**：每个线程一个，每个方法对应一个栈帧——StackOverflowError
- **方法区→元空间**：类信息、常量、静态变量——OOM: MetaSpace

### 2. 垃圾回收（GC, ⭐ 面试核心）
- **判断对象存活**：
  - 引用计数法：循环引用问题
  - 可达性分析：GC Roots(栈引用/静态引用/常量池引用) → 引用链 → 不可达=可回收
- **GC 算法**：
  - 标记-清除：碎片化
  - 标记-复制：新生代常用(Eden→Survivor)——简单高效
  - 标记-整理：老年代常用——无碎片
- **垃圾回收器**：
  - Serial/ParNew：单/多线程，新生代
  - Parallel Scavenge：关注吞吐量
  - **CMS**：并发标记-清除，低停顿——但会产生碎片
  - **G1**（JDK9默认）：分Region，可预测停顿——大堆首选
  - **ZGC/Shenandoah**：超低延迟(<10ms)——JDK11/15+

### 3. 类加载机制
- **加载→验证→准备→解析→初始化→使用→卸载**
- **双亲委派模型**：Bootstrap ClassLoader → Extension → Application → 自定义
  - 从子→父委托，保证核心类不被篡改
  - 打破：Tomcat WebappClassLoader、SPI的线程上下文类加载器

### 第二部分：并发编程

### 4. Java 内存模型（JMM）
- **三大特性**：原子性、可见性、有序性
- **happens-before 原则**：程序顺序、锁的unlock→lock、volatile写→读、传递性
- **volatile**：保证可见性+禁止指令重排(内存屏障)——不保证原子性
- **CAS**：Compare-And-Swap——volatile变量+自旋 → AtomicInteger/AQS的基础

### 5. 线程与线程池
- **线程生命周期**：NEW→RUNNABLE↔BLOCKED/WAITING/TIMED_WAITING→TERMINATED
- **ThreadPoolExecutor 七大参数**：corePoolSize, maxPoolSize, keepAliveTime, unit, workQueue(有界/无界), threadFactory, handler(拒绝策略)
- **四种拒绝策略**：AbortPolicy(抛异常)、CallerRuns(调用者执行)、Discard(丢弃)、DiscardOldest

### 6. 锁（Lock）
- **synchronized**：对象头Mark Word、锁升级(偏向锁→轻量级锁→重量级锁)、monitorenter/exit
- **AQS（AbstractQueuedSynchronizer）**：CLH队列+state变量+park/unpark——ReentrantLock/CountDownLatch/Semaphore的底层
- **ReentrantLock**：可重入、公平/非公平锁、可中断、tryLock
- **synchronized vs ReentrantLock**：自动释放 vs 手动finally unlock，后者更灵活

## ✅ 学习建议
- 读《深入理解Java虚拟机》第2-3-7章——GC和内存模型的核心
- 并发编程：先理解volatile和CAS，再学synchronized和AQS，最后看线程池
- GC日志(用CheatSheet)能定位90%的内存问题
