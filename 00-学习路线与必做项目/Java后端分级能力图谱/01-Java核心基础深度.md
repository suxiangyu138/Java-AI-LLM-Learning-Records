# Java 核心基础深度 —— 面试高频考点

> **原则：不是会用，而是理解原理。**
> 集合源码 + 多线程并发 + JVM 虚拟机 = Java 后端工程师的三大基本功。

---

## 一、集合框架源码（★★★★★ 必考）

### 1.1 核心类底层实现

| 类 | 底层结构 | 线程安全 | 关键参数 |
|----|---------|---------|---------|
| ArrayList | `Object[]` 数组 | ❌ | 默认容量 10，扩容 1.5 倍 |
| LinkedList | 双向链表 | ❌ | Node 节点 prev/next |
| HashMap (JDK8+) | 数组 + 链表 + 红黑树 | ❌ | 默认容量 16，负载因子 0.75，树化阈值 8，链化阈值 6 |
| ConcurrentHashMap (JDK8+) | Node 数组 + CAS + synchronized | ✅ | 分段锁升级为桶级别锁 |
| HashSet | HashMap 的 key（value 为 PRESENT） | ❌ | — |
| LinkedHashMap | HashMap + 双向链表 | ❌ | 按插入顺序/访问顺序 |

### 1.2 HashMap 源码要点

```
数据结构演进：
JDK7：数组 + 链表（头插法，扩容时可能死循环）
JDK8：数组 + 链表 + 红黑树（尾插法，解决死循环）

put() 流程：
1. 计算 hash → (h = key.hashCode()) ^ (h >>> 16)   // 高低 16 位异或，减少碰撞
2. 定位桶位 → (n - 1) & hash                       // 相当于 hash % n，位运算更快
3. 桶为空 → 直接放入
4. 桶有值 → 判断是链表还是红黑树
   - 链表：尾插法，遍历比较 key，相同则覆盖，否则插入尾部
   - 树节点数 ≥ 8 且数组长度 ≥ 64 → 链表转红黑树
   - 树节点数 ≤ 6 → 红黑树转链表
5. size++ 后检查是否超过 threshold，超过则扩容（2 倍）
```

**面试必答**："HashMap 为什么线程不安全？"

```
1. put() 时多线程数据覆盖：两个线程同时 hash 到同一空桶，后写入的覆盖先写入的
2. resize() 时可能形成死循环（JDK7 头插法）：多线程同时扩容时链表可能成环
3. size++ 非原子操作：多线程 put 后 size 可能偏小
4. ConcurrentmodificationException：迭代时其他线程修改抛出此异常
```

### 1.3 ConcurrentHashMap 线程安全原理（JDK8）

```
核心机制：CAS + synchronized + volatile

put() 流程：
1. 计算 hash，定位桶位
2. 桶为空 → CAS 尝试写入（自旋，无锁）
3. 桶不为空 → synchronized 锁住桶的头节点
4. 桶内链表/红黑树操作在锁保护下进行
5. 扩容时可多线程协助（transfer() 多线程迁移数据）

为什么比 HashTable 快？
- HashTable 对所有操作加同一把锁（全表锁）
- ConcurrentHashMap 只锁当前桶（桶级别锁），并发度 = 桶数量
```

### 1.4 能手写简化版 HashMap

面试中可能被要求手写。核心数据结构：

```java
class Node<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;
}

// 核心方法签名
V put(K key, V value);
V get(K key);
Node<K,V>[] resize();  // 扩容
```

---

## 二、多线程并发（★★★★★ 必考）

### 2.1 锁机制对比

| 特性 | synchronized | ReentrantLock |
|------|-------------|---------------|
| 实现 | JVM 级别（monitorenter/exit） | JDK 级别（AQS） |
| 锁获取 | 隐式，自动释放 | 显式，lock()/unlock() |
| 公平锁 | ❌ 非公平 | ✅ 支持公平/非公平 |
| 可中断 | ❌ 不可中断 | ✅ lockInterruptibly() |
| 条件变量 | wait/notify（1 个） | Condition（多个） |
| 尝试获取 | ❌ 一直阻塞 | ✅ tryLock() |
| 性能 | JDK6+ 优化后接近 | 略优于 synchronized |

**选型口诀**：普通场景用 synchronized（简单安全），需要公平锁/可中断/多条件/尝试获取时用 ReentrantLock。

### 2.2 AQS（AbstractQueuedSynchronizer）底层

```
核心数据结构：
- state（volatile int）：同步状态，0=未锁，1=已锁
- CLH 队列（双向链表）：存放等待获取锁的线程
- Node 节点：封装线程 + 等待状态（SIGNAL/CANCELLED/CONDITION）

加锁流程：
1. CAS 尝试将 state 从 0 改为 1
2. 成功 → 当前线程持有锁
3. 失败 → 线程封装为 Node 节点，加入 CLH 队列尾部（CAS 入队）
4. 前驱节点是 head → 再次尝试获取锁
5. 前驱节点不是 head → park() 挂起当前线程

解锁流程：
1. state 减为 0
2. unpark() 唤醒 CLH 队列中 head 的下一个节点
```

**AQS 的子类应用**：ReentrantLock、CountDownLatch、Semaphore、ReentrantReadWriteLock、ThreadPoolExecutor.Worker。

### 2.3 线程池 7 大参数 + 拒绝策略

```
ThreadPoolExecutor(
    corePoolSize,        // 核心线程数（常驻线程）
    maximumPoolSize,     // 最大线程数
    keepAliveTime,       // 空闲线程存活时间
    unit,                // 时间单位
    workQueue,           // 阻塞队列（存放等待执行的任务）
    threadFactory,       // 线程工厂（自定义线程名）
    rejectedExecutionHandler  // 拒绝策略
)

执行流程：
1. 当前线程数 < corePoolSize → 创建新线程执行
2. 当前线程数 ≥ corePoolSize → 放入 workQueue 等待
3. workQueue 满了 → 创建新线程（直到 maximumPoolSize）
4. 线程数 = maximumPoolSize 且 workQueue 满了 → 执行拒绝策略

4 种拒绝策略：
- AbortPolicy（默认）：抛 RejectedExecutionException
- CallerRunsPolicy：由提交任务的线程自己执行
- DiscardPolicy：直接丢弃（不抛异常）
- DiscardOldestPolicy：丢弃 workQueue 中最老的任务
```

**面试高频**："核心线程数和最大线程数如何设置？"

```
CPU 密集型：corePoolSize = CPU 核数 + 1
IO 密集型：corePoolSize = CPU 核数 × 2（或 CPU 核数 / (1 - 阻塞系数)）
混合型：分别设置两个线程池

实际生产建议：基于压测数据调优，公式只作参考。
```

### 2.4 JUC 工具类使用场景

| 工具 | 使用场景 |
|------|---------|
| CountDownLatch | 主线程等待多个子线程完成（如并行查询多数据源后汇总） |
| CyclicBarrier | 多个线程互相等待，然后一起执行（可复用） |
| Semaphore | 限流控制，限制同时访问资源的线程数 |
| CompletableFuture | 异步编排，任务组合（thenApply/thenCombine/allOf） |
| ReadWriteLock | 读多写少场景（缓存/配置中心） |

### 2.5 volatile 关键字

```
三大特性：
1. 可见性：一个线程修改后，其他线程立即可见（MESI 缓存一致性协议 + 内存屏障）
2. 有序性：禁止指令重排序（内存屏障）
3. 不保证原子性：i++ 这类复合操作仍不安全

使用场景：
- 状态标志位：volatile boolean flag = false;
- 双重检查锁（DCL）单例：instance 必须声明为 volatile
- CAS 中的 value（AtomicInteger 内部使用 volatile int value）
```

---

## 三、JVM 虚拟机（★★★★★ 必考）

### 3.1 内存模型

```
JVM 运行时数据区：

线程共享：
├── 堆（Heap）：对象实例，GC 主战场
│   ├── 年轻代：Eden + S0 + S1（8:1:1）
│   └── 老年代
└── 方法区/元空间：类信息、常量、静态变量（JDK8+ 移入本地内存）

线程私有：
├── 程序计数器：当前线程执行的字节码行号
├── 虚拟机栈：栈帧（局部变量表 + 操作数栈 + 方法出口）
└── 本地方法栈：Native 方法调用
```

### 3.2 垃圾回收算法对比

| 算法 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| 标记-清除 | 标记存活 → 清除未标记 | 简单 | 内存碎片 |
| 复制 | 将存活对象复制到新空间 | 无碎片，效率高 | 浪费一半内存 |
| 标记-整理 | 标记存活 → 向一端移动 | 无碎片 | STW 时间长 |

**分代回收策略**：年轻代用复制算法（对象朝生夕灭），老年代用标记-清除/标记-整理。

### 3.3 GC 收集器选型

| 收集器 | 代 | 算法 | 特点 | 适用场景 |
|--------|---|------|------|---------|
| Serial | 年轻代 | 复制 | 单线程，STW | 客户端/小内存 |
| Parallel | 年轻代 | 复制 | 多线程，吞吐量优先 | 后台计算（无交互） |
| CMS | 老年代 | 标记-清除 | 并发低延迟 | JDK8 默认老年代 |
| G1 | 全代 | 标记-整理+复制 | 可预测停顿 | 大堆（>4G），JDK9+ 默认 |
| ZGC | 全代 | 染色指针 | 亚毫秒级停顿 | 超大堆（TB 级），JDK15+ |

**面试必答**："G1 相比 CMS 的优势？"

```
1. 内存布局：CMS 连续分代，G1 将堆划分为大小相等的 Region（1-32MB）
2. 碎片化：CMS 标记-清除导致碎片，G1 标记-整理 + 复制减少碎片
3. 停顿可控：G1 可设置最大停顿时间（-XX:MaxGCPauseMillis）
4. 巨型对象：G1 专有 Humongous Region 处理大对象
5. 预测模型：G1 根据历史数据预测 Region 回收耗时
```

### 3.4 JVM 参数速查

```
堆内存：
-Xms4g         初始堆大小
-Xmx4g         最大堆大小
-Xmn2g         年轻代大小
-XX:NewRatio   老年代:年轻代比例（默认 2，即老年代占 2/3）

GC 选择：
-XX:+UseG1GC         使用 G1
-XX:+UseZGC          使用 ZGC（JDK 15+）
-XX:+UseParallelGC   使用 Parallel

GC 日志（JDK 8）：
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/path/to/gc.log

GC 日志（JDK 9+）：
-Xlog:gc*:file=/path/to/gc.log:time,level,tags

OOM 排查：
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/path/to/dump.hprof
```

---

## 四、IO 与 NIO（★★★★☆）

### 4.1 BIO / NIO / AIO 区别

| 模型 | 阻塞 | 线程模型 | 适用场景 |
|------|------|---------|---------|
| BIO | 同步阻塞 | 一个连接一个线程 | 连接数少且固定 |
| NIO | 同步非阻塞 | 一个线程处理多个连接（Selector） | 连接数多且短（聊天/推送） |
| AIO | 异步非阻塞 | 回调机制 | 连接数多且长（文件操作） |

### 4.2 NIO 三大核心组件

```
Channel（通道）：双向读写，如 FileChannel、SocketChannel
Buffer（缓冲区）：读写数据的内存块，flip() 切换读写模式
Selector（选择器）：单个线程监控多个 Channel 的事件（OP_READ/OP_WRITE/OP_CONNECT/OP_ACCEPT）
```

### 4.3 零拷贝原理

```
传统传输：磁盘 → 内核缓冲区 → 用户缓冲区 → Socket 缓冲区 → 网卡（4 次拷贝，4 次上下文切换）
零拷贝（sendfile）：磁盘 → 内核缓冲区 → Socket 缓冲区 → 网卡（3 次拷贝，2 次上下文切换）
零拷贝（mmap）：磁盘映射到内核缓冲区，用户空间和内核空间共享（减少 1 次拷贝）

Kafka 高性能原因之一：大量使用零拷贝技术
```

---

## 五、反射与动态代理（★★★★☆）

### 5.1 反射机制

```
核心类：
- Class：类的元信息入口
- Constructor：构造器
- Method：方法
- Field：属性

获取 Class 对象的 3 种方式：
1. Class.forName("com.example.User")
2. User.class
3. user.getClass()

应用场景：
- Spring IOC 容器实例化 Bean
- MyBatis Mapper 接口动态代理
- 注解处理器（运行时解析注解）
- 序列化/反序列化（Jackson/Gson）
```

### 5.2 JDK 动态代理 vs CGLIB

| 对比维度 | JDK 动态代理 | CGLIB |
|---------|------------|-------|
| 原理 | 实现接口（Proxy + InvocationHandler） | 继承目标类（ASM 字节码增强） |
| 限制 | 目标类必须实现接口 | 不能代理 final 类和方法 |
| 性能 | JDK 8+ 大幅优化，接近 CGLIB | 早期版本更快 |
| Spring 选择 | 默认（当目标实现了接口） | 目标未实现接口时使用 |

---

## 六、设计模式（★★★★☆）

### 6.1 必须掌握的 10 种

| 模式 | 核心思想 | Spring 中的应用 |
|------|---------|---------------|
| 单例 | 全局唯一实例 | Spring Bean 默认单例 |
| 工厂 | 解耦对象创建 | BeanFactory |
| 策略 | 算法族可替换 | 选择不同实现类 |
| 模板方法 | 父类定义骨架，子类实现细节 | AbstractApplicationContext.refresh() |
| 代理 | 控制对象访问 | Spring AOP |
| 观察者 | 一对多通知 | ApplicationEvent/Listener |
| 适配器 | 接口转换 | HandlerAdapter |
| 装饰器 | 动态添加功能 | BeanDefinitionDecorator |
| 责任链 | 请求沿链传递 | FilterChain、Interceptor |
| 建造者 | 分步构建复杂对象 | RequestMappingInfo.Builder |

**面试技巧**：每种模式能说出 Spring 中的应用 + 自己项目中的使用场景，远胜于背诵定义。

---

## 面试自查清单

```
□ HashMap put() 流程能默写出来
□ ConcurrentHashMap JDK7 vs JDK8 的区别能说清
□ AQS 的 CLH 队列 + state + CAS 机制理解
□ 线程池 7 参数 + 4 种拒绝策略能默写
□ JVM 内存模型（堆/栈/方法区）能画图
□ G1 vs CMS 的 5 个核心区别
□ JDK 动态代理 vs CGLIB 的 2 个关键差异
□ 10 种设计模式各能说出 1 个 Spring 应用场景
```
