# Java 核心类库深度解析

> 本文涵盖集合框架、并发包、NIO、Stream API与Lambda、日期时间API与Optional五大主题，从源码层面剖析核心实现原理与最佳实践。

---

## 目录

1. [集合框架 (Collections Framework)](#1-集合框架-collections-framework)
2. [并发包 (java.util.concurrent)](#2-并发包-javautilconcurrent)
3. [NIO (New I/O)](#3-nio-new-io)
4. [Stream API 与 Lambda](#4-stream-api-与-lambda)
5. [其他核心类库](#5-其他核心类库)
6. [总结清单](#6-总结清单)

---

## 1. 集合框架 (Collections Framework)

Java 集合框架是整个 JDK 中使用频率最高的类库，掌握其底层数据结构与算法是 Java 开发者的基本功。

### 1.1 List 体系

#### ArrayList vs LinkedList

| 特性 | ArrayList | LinkedList |
|------|-----------|------------|
| 底层结构 | Object[] 数组 | 双向链表 (Node first, last) |
| 随机访问 get(i) | O(1) | O(n) |
| 尾部插入 | O(1) 均摊 | O(1) |
| 中间插入/删除 | O(n) 元素位移 | O(n) 遍历找到节点后 O(1) |
| 内存占用 | 连续内存，更紧凑 | 每个节点额外存储 Node 对象 |

```java
// ArrayList 扩容机制源码分析
// 默认容量 10，每次扩容为原来的 1.5 倍 (oldCapacity + (oldCapacity >> 1))
// grow() 方法最终调用 Arrays.copyOf()

List<String> arrayList = new ArrayList<>();
arrayList.add("A"); // 底层数组容量从 0 扩容到 10
arrayList.add("B");

// LinkedList 的双向链表结构
List<String> linkedList = new LinkedList<>();
linkedList.add("A"); // 在尾部追加，等价于 linkLast()
linkedList.add(1, "B"); // 中间插入：先二分查找定位节点，再 linkBefore()
```

**ArrayList 扩容机制详解**：

- 初始容量为 0（JDK 8 懒加载），第一次 add 时扩容为 10
- 每次 add 时检查 `size + 1 > elementData.length`，若超过则触发 grow()
- `grow()` 新容量 = `oldCapacity + (oldCapacity >> 1)`，即 1.5 倍
- 若新容量仍小于 minCapacity，则取 minCapacity
- 若新容量超过 MAX_ARRAY_SIZE，调用 `hugeCapacity()` 处理
- 最终通过 `Arrays.copyOf()` 将原数组复制到新数组

#### Vector

Vector 是 JDK 1.0 遗留的线程安全集合，所有方法均使用 `synchronized` 修饰。由于其同步粒度太粗（方法级别），性能远不如使用 `Collections.synchronizedList()` 或 `CopyOnWriteArrayList`。

```java
// Vector 已不推荐使用
Vector<String> vector = new Vector<>();
vector.add("A"); // synchronized 方法
```

线程安全 List 的选择：
- **并发读写比例高**：`CopyOnWriteArrayList`（写时复制，适合读多写少场景）
- **写操作频繁且需要线程安全**：`Collections.synchronizedList(new ArrayList<>())`

#### CopyOnWriteArrayList

核心原理：**写时复制 (Copy-On-Write)**。所有写操作（add、set、remove）都会复制底层数组，写操作在新数组上进行，读操作在旧数组上无锁进行。

```java
CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>();
cowList.add("A"); // 内部 synchronized 块 + Arrays.copyOf()
cowList.get(0);   // 无锁，直接 array[index]

// 适用场景：读多写少，如监听器列表、缓存白名单
// 不适用场景：写频繁（每次写 O(n) 复制整个数组）
```

### 1.2 Set 体系

Set 的核心特征是**元素不可重复**，底层实际上全部依赖 Map 实现。

#### HashSet

```java
// HashSet 底层是 HashMap
// add(E) 实际调用 HashMap.put(E, PRESENT)，PRESENT 是常量 Object
// 利用 HashMap 的 key 不可重复特性

Set<String> hashSet = new HashSet<>();
hashSet.add("Java"); // hashSet.map.put("Java", PRESENT);
hashSet.add("Python");
```

**为什么 HashSet 的 add 只用 key？**

HashMap 存储的是 `key-value` 键值对。HashSet 将元素作为 HashMap 的 key，value 统一为一个 `static final` 的 `PRESENT` 虚值对象。HashMap 的 key 天然不重复，所以 HashSet 只需利用 key 的不可重复性即可。

**查找效率**：hashCode() 定位桶 -> equals() 比较链表/红黑树中的元素，平均 O(1)。

#### LinkedHashSet

继承自 HashSet，底层使用 `LinkedHashMap`，通过双向链表维护**插入顺序**。

```java
Set<String> linkedHashSet = new LinkedHashSet<>();
linkedHashSet.add("C");
linkedHashSet.add("A");
linkedHashSet.add("B");
// 迭代顺序：C -> A -> B（与插入顺序一致）
```

#### TreeSet

底层是 `TreeMap`（红黑树实现），元素按**自然排序**或**自定义比较器**排序。

```java
// 自然排序：元素需实现 Comparable 接口
Set<Integer> treeSet = new TreeSet<>();
treeSet.add(3);
treeSet.add(1);
treeSet.add(2);
// 迭代顺序：1 -> 2 -> 3（升序）

// 自定义排序：传入 Comparator
Set<String> customTreeSet = new TreeSet<>(
    (a, b) -> b.compareTo(a) // 降序
);
customTreeSet.add("A");
customTreeSet.add("C");
customTreeSet.add("B");
// 迭代顺序：C -> B -> A（降序）
```

### 1.3 Map 体系

#### HashMap 深度剖析

HashMap 是面试中出现频率最高的集合类，其演进历史展示了 Java 对性能的极致追求。

**JDK 7 vs JDK 8 对比**：

| 特性 | JDK 7 | JDK 8 |
|------|-------|-------|
| 数据结构 | 数组 + 链表 | 数组 + 链表 + 红黑树 |
| 节点类型 | Entry | Node / TreeNode |
| hash 扰动 | 9次扰动 | 2次扰动（高低位异或） |
| 插入方式 | 头插法 | 尾插法 |
| 树化阈值 | 无 | 链表长度 >= 8 |

**为什么 HashMap 容量是 2 的幂？**

```java
// HashMap 计算桶位置的公式：
// (n - 1) & hash  等价于  hash % n  （当 n 是 2 的幂时）
```

原因：
1. **位运算替代取模**：`(n - 1) & hash` 比 `hash % n` 快得多
2. **均匀散列**：2 的幂数减 1 的二进制全是 1，可以充分利用 hash 值的所有位
3. **扩容后 rehash 高效**：元素在新桶中的索引 = 原索引 或 原索引 + oldCap（通过 `hash & oldCap` 判断）

**负载因子 (loadFactor) 与扩容**：

```java
// 扩容阈值 threshold = capacity * loadFactor
// 默认负载因子 0.75
// 当 size > threshold 时，扩容为原来的 2 倍
```

负载因子为什么是 0.75？这是**空间与时间的权衡**：
- 负载因子越大（如 1.0），空间利用率高，但 hash 冲突概率增大，查询效率降低
- 负载因子越小（如 0.5），hash 冲突少，查询快，但空间浪费大
- 0.75 是经过泊松分布计算出的较优值

**红黑树化**：

当链表长度 >= 8（TREEIFY_THRESHOLD）且数组长度 >= 64（MIN_TREEIFY_CAPACITY）时，链表转为红黑树。树化阈值为 8 的原因是泊松分布计算出的概率结果——链表长度达到 8 的概率极低（约 0.00000006），此时说明 hash 函数出现了严重问题，需要用红黑树兜底。

```java
// HashMap 的基本使用
Map<String, Integer> hashMap = new HashMap<>(16, 0.75f);
hashMap.put("A", 1);
hashMap.put("B", 2);

// 遍历方式
hashMap.forEach((k, v) -> System.out.println(k + "=" + v));

// JDK 8 新增方法
hashMap.computeIfAbsent("C", k -> 3); // key 不存在时计算 value
hashMap.merge("A", 10, Integer::sum);  // 合并：key 存在则相加
```

#### LinkedHashMap

继承自 HashMap，额外维护双向链表保证**迭代顺序**。

```java
// 插入顺序：与插入顺序一致
Map<String, Integer> linkedMap = new LinkedHashMap<>();
linkedMap.put("A", 1);
linkedMap.put("B", 2);
// 迭代：A -> B

// accessOrder = true 实现 LRU 缓存
// 每次 get/put 会将访问的节点移到链表尾部
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxCapacity;

    public LRUCache(int maxCapacity) {
        super(16, 0.75f, true); // accessOrder = true
        this.maxCapacity = maxCapacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity; // 超过容量移除最久未使用的
    }
}

LRUCache<String, Integer> lru = new LRUCache<>(3);
lru.put("A", 1);
lru.put("B", 2); // 此时 A 被访问（accessOrder 会移除）
lru.get("A");    // 访问 A，A 移到尾部
// 此时若再 put C，会导致 C 加入，最久未访问的 B 被移除
```

#### TreeMap

基于**红黑树**的 NavigableMap 实现，键值对按 key 排序。

```java
TreeMap<String, Integer> treeMap = new TreeMap<>();
treeMap.put("B", 2);
treeMap.put("A", 1);
treeMap.put("C", 3);

// 按 key 遍历（自然升序）
treeMap.forEach((k, v) -> System.out.println(k)); // A, B, C

// NavigableMap 方法
treeMap.firstKey();        // A
treeMap.lastKey();         // C
treeMap.lowerKey("B");     // A（小于 B 的最大键）
treeMap.higherKey("B");    // C（大于 B 的最小键）
treeMap.subMap("A", "C");  // {A=1, B=2}（左闭右开）
```

#### ConcurrentHashMap 并发安全机制

ConcurrentHashMap 是并发场景下 Map 的首选，其内部实现经历了重大演进。

**JDK 7 分段锁 (Segment)**：

- 内部维护 Segment 数组，每个 Segment 继承 ReentrantLock，是一个小 HashMap
- 写操作只需锁住对应的 Segment，不同 Segment 之间并发写入
- 最大并发度 = Segment 数组长度（默认 16）
- 缺点：分段数量固定，扩容时整个 Segment 内 rehash，Segments 数组不扩容

**JDK 8 CAS + synchronized**：

- 抛弃分段锁，改用 **CAS + synchronized** 实现
- 并发粒度细化到每个数组桶
- 插入时，若桶为空，使用 **CAS 无锁插入**
- 若桶不为空，使用 **synchronized 锁住链表/红黑树头节点**
- 数据结构与 HashMap 一致：数组 + 链表 + 红黑树

```java
// ConcurrentHashMap JDK 8 put 流程简化
// 1. 检查 key/value 不为 null
// 2. 检查 table 是否初始化，未初始化则 initTable()
// 3. 计算 hash，定位桶
// 4. 桶为空 -> CAS 插入（无锁）
// 5. 桶不为空 -> synchronized(头节点) -> 链表/红黑树插入
// 6. 检查链表长度 >= 8 -> treeifyBin()
// 7. 检查 size > threshold -> transfer() 扩容

ConcurrentHashMap<String, Integer> concurrentMap =
    new ConcurrentHashMap<>(16);
concurrentMap.put("A", 1);
concurrentMap.put("B", 2);

// JDK 8 新增的并发操作方法
concurrentMap.computeIfAbsent("C", key -> {
    // 原子操作：key 不存在时才计算
    return fetchFromDB(key);
});

concurrentMap.forEach(4, (k, v) ->
    System.out.println(Thread.currentThread() + ":" + k + "=" + v)
    // 并行度为 4 的遍历
);
```

### 1.4 Queue / Deque

队列（Queue）和双端队列（Deque）在生产者-消费者模式、任务调度等场景中广泛使用。

```java
// LinkedList 作为队列（FIFO）
Queue<String> queue = new LinkedList<>();
queue.offer("A"); // 入队（推荐，不会抛异常）
queue.add("B");   // 入队（队列满时抛异常）
queue.poll();     // 获取并移除头（队列空返回 null）
queue.peek();     // 仅获取不移除（队列空返回 null）

// ArrayDeque：循环数组实现，无界，性能优于 LinkedList
Deque<String> deque = new ArrayDeque<>();
deque.addFirst("A");
deque.addLast("B");
deque.pollFirst();
deque.pollLast();

// PriorityQueue：基于二叉堆，元素按优先级出队
Queue<Integer> priorityQueue = new PriorityQueue<>();
priorityQueue.offer(5);
priorityQueue.offer(1);
priorityQueue.offer(3);
// 出队顺序：1 -> 3 -> 5（最小堆，自然升序）
priorityQueue.poll(); // 1

// 自定义优先级
Queue<Task> taskQueue = new PriorityQueue<>(
    Comparator.comparingInt(Task::getPriority)
);

// BlockingQueue：线程安全的阻塞队列
BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(10);
// 阻塞方法：put() / take()（队列满/空时阻塞等待）

ArrayBlockingQueue<String> arrayBlockingQueue = new ArrayBlockingQueue<>(10);
LinkedBlockingQueue<String> linkedBlockingQueue = new LinkedBlockingQueue<>();

// ArrayBlockingQueue vs LinkedBlockingQueue
// - ArrayBlockingQueue：有界，单一锁，直接 ReentrantLock
// - LinkedBlockingQueue：默认无界，两把锁（takeLock/putLock），吞吐量更高
```

### 1.5 集合工具类

#### Collections 工具类

```java
// 1. 不可变集合
List<String> mutableList = new ArrayList<>(Arrays.asList("A", "B"));
List<String> unmodifiableList = Collections.unmodifiableList(mutableList);
// unmodifiableList.add("C"); // 抛出 UnsupportedOperationException

// Java 9+ 不可变集合更简洁
List.of("A", "B");       // 不可变 List
Set.of("A", "B");        // 不可变 Set
Map.of("A", 1, "B", 2);  // 不可变 Map

// 2. 线程安全包装
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
// 注意：迭代时仍需外部同步
synchronized (syncList) {
    for (String s : syncList) { /* ... */ }
}

// 3. 类型安全视图
List rawList = new ArrayList();
List<String> checkedList = Collections.checkedList(rawList, String.class);
// checkedList.add(123); // ClassCastException 在插入时立即抛出（而非在获取时）

// 4. 其他实用方法
Collections.sort(list);
Collections.binarySearch(list, key);
Collections.reverse(list);
Collections.shuffle(list);
Collections.swap(list, i, j);
Collections.min(list);
Collections.max(list);
Collections.replaceAll(list, oldVal, newVal);
Collections.frequency(list, obj);
Collections.rotate(list, distance); // 旋转
```

#### Arrays 工具方法

```java
int[] arr = {3, 1, 4, 1, 5};

// 排序
Arrays.sort(arr);                              // 双轴快排
Arrays.parallelSort(arr);                      // 并行排序（大数组更优）

// 二分查找（需先排序）
int index = Arrays.binarySearch(arr, 4);

// 填充
Arrays.fill(arr, 0);                           // 全部填充为 0

// 复制
int[] copy = Arrays.copyOf(arr, arr.length);    // 扩容/截断数组
int[] rangeCopy = Arrays.copyOfRange(arr, 1, 3); // 复制指定范围

// 比较
int[] a = {1, 2, 3};
int[] b = {1, 2, 3};
Arrays.equals(a, b);                            // 值比较（非引用比较）

// 并行前缀计算
Arrays.parallelPrefix(arr, Integer::sum);       // arr = {3, 4, 8, 9, 14}

// 转为流
Arrays.stream(arr).filter(x -> x > 3).sum();

// 转 List（注意：返回的是固定大小视图，不支持结构性修改）
List<int[]> list = Arrays.asList(arr);
// 包装类型才能正常使用
Integer[] intArr = {1, 2, 3};
List<Integer> intList = Arrays.asList(intArr);
```

---

## 2. 并发包 (java.util.concurrent)

JUC 包是 Java 并发编程的核心武器，从原子变量到线程池，构建了一套完整的并发工具链。

### 2.1 原子类

原子类通过 **CAS (Compare And Swap)** 硬件指令实现线程安全的无锁操作。

```java
import java.util.concurrent.atomic.*;
import java.util.function.IntUnaryOperator;

// AtomicInteger: 基于 CAS 的原子整数
AtomicInteger counter = new AtomicInteger(0);

// 常用操作
counter.get();              // 获取当前值
counter.set(10);            // 设置新值
counter.getAndIncrement();  // i++ 返回旧值
counter.incrementAndGet();  // ++i 返回新值
counter.addAndGet(5);       // += 5 返回新值
counter.getAndUpdate(x -> x * 2); // 函数式更新

// CAS 核心方法
boolean success = counter.compareAndSet(10, 20);
// 如果当前值 == 10，则设为 20 返回 true；否则返回 false
```

**CAS 原理**：
- CAS 是一条 CPU 原子指令（cmpxchg），包含三个操作数：内存地址 V、期望值 A、新值 B
- 当 V 中的值 == A 时，将 V 中的值更新为 B，否则不更新
- `sun.misc.Unsafe` 提供 CAS 底层支持（`compareAndSwapInt`）

**ABA 问题**：

```java
// ABA 问题：变量从 A -> B -> A，CAS 认为值未改变，但实际上已被修改
// 解决方案：AtomicStampedReference（带版本号）

AtomicStampedReference<String> ref = new AtomicStampedReference<>("A", 0);
String reference = ref.getReference();
int stamp = ref.getStamp();
// 比较引用和版本号同时一致时才更新
boolean success = ref.compareAndSet("A", "B", stamp, stamp + 1);
```

#### LongAdder（高并发场景优于 AtomicLong）

```java
import java.util.concurrent.atomic.LongAdder;

// LongAdder：分段累加，高并发下性能远超 AtomicLong
LongAdder adder = new LongAdder();

adder.increment();    // ++
adder.add(10);        // += 10
long sum = adder.sum();  // 返回当前总和
adder.sumThenReset(); // 返回总和并重置
```

**LongAdder 原理**：
- 内部维护 **base 变量 + Cell[] 数组**
- 低并发时，直接 CAS 更新 base
- 高并发时，将线程 hash 到不同的 Cell 上进行累加，减少 CAS 竞争
- `sum()` 时遍历 base 和所有 Cell 求和
- 空间换时间，适合**统计计数**场景（不要求强一致性的精确值）

**性能对比**：
- 低并发（1-2 线程）：AtomicLong ≈ LongAdder
- 高并发（8+ 线程）：LongAdder 吞吐量可达 AtomicLong 的 3-5 倍
- 代价：LongAdder 的 sum() 不是精确的（并发累加中），精确场景用 AtomicLong

### 2.2 锁机制

#### ReentrantLock

基于 **AQS (AbstractQueuedSynchronizer)** 的可重入独占锁。

```java
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

ReentrantLock lock = new ReentrantLock();
// 构造参数 fair = true 为公平锁，false 为非公平锁（默认）

lock.lock();
try {
    // 临界区代码
} finally {
    lock.unlock(); // 务必在 finally 中释放
}

// Condition：精确唤醒指定线程
ReentrantLock conditionLock = new ReentrantLock();
Condition notEmpty = conditionLock.newCondition();
Condition notFull = conditionLock.newCondition();

// 生产者线程
conditionLock.lock();
try {
    while (queueIsFull()) {
        notFull.await(); // 等待，释放锁
    }
    enqueue(data);
    notEmpty.signal(); // 唤醒消费者
} finally {
    conditionLock.unlock();
}
```

**公平锁 vs 非公平锁**：

```java
// 公平锁：先来先服务，线程按 FIFO 排队获取锁
// 非公平锁：允许插队，减少线程唤醒开销，但可能导致线程饥饿

ReentrantLock fairLock = new ReentrantLock(true);   // 公平锁
ReentrantLock unfairLock = new ReentrantLock(false); // 非公平锁

// 性能：非公平锁通常优于公平锁（减少上下文切换）
// 场景：公平锁适用于对锁获取顺序有严格要求的场景
```

**AQS 原理**：
- 核心是一个 **volatile int state** + **CLH 双向队列**
- state 为 0 表示锁未被占用，> 0 表示重入次数
- CLH 队列存储等待获取锁的线程（每个节点是一个 Node）
- 通过 `tryAcquire()` / `tryRelease()` 模板方法实现自定义同步器

#### ReentrantReadWriteLock

读写锁：读读不互斥，读写互斥，写写互斥。

```java
import java.util.concurrent.locks.ReentrantReadWriteLock;

ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
Lock readLock = rwLock.readLock();
Lock writeLock = rwLock.writeLock();

// 读操作：多个线程可以同时持有
readLock.lock();
try {
    // 读数据
} finally {
    readLock.unlock();
}

// 写操作：必须独占
writeLock.lock();
try {
    // 写数据
} finally {
    writeLock.unlock();
}
```

**锁降级**：

```java
// 写锁 -> 读锁的降级（读锁不能升级为写锁，会导致死锁）
writeLock.lock();
try {
    // 修改数据
    readLock.lock(); // 在持有写锁时获取读锁
} finally {
    writeLock.unlock(); // 释放写锁，降级为读锁
}
// 此时仍持有读锁
readLock.unlock();
```

#### StampedLock

JDK 8 引入的新锁，支持**乐观读**，在读多写少场景下性能优于 ReadWriteLock。

```java
import java.util.concurrent.locks.StampedLock;

StampedLock stampedLock = new StampedLock();

// 1. 写锁
long stamp = stampedLock.writeLock();
try {
    // 写数据
} finally {
    stampedLock.unlockWrite(stamp);
}

// 2. 乐观读（无锁，性能极高）
long stampOpt = stampedLock.tryOptimisticRead();
int data = this.data; // 读取数据
if (!stampedLock.validate(stampOpt)) { // 检查是否有写操作干扰
    // 乐观读失败，升级为悲观读锁
    stampOpt = stampedLock.readLock();
    try {
        data = this.data;
    } finally {
        stampedLock.unlockRead(stampOpt);
    }
}
```

**StampedLock 注意事项**：
- 不可重入（每次加锁返回 stamp，释放时需传入）
- 支持锁升级（乐观读 -> 悲观读 -> 写锁）
- 不支持 Condition
- 使用 `tryOptimisticRead()` 后必须调用 `validate()` 校验

### 2.3 同步器

```java
// CountDownLatch：一个线程等待多个线程完成（一次性）
CountDownLatch latch = new CountDownLatch(3);

// 工作线程
new Thread(() -> {
    doWork();
    latch.countDown(); // 计数器 -1
}).start();

// 主线程等待
latch.await(); // 阻塞直到计数器为 0

// CyclicBarrier：多个线程互相等待到达屏障（可循环使用）
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    System.out.println("所有线程已到达屏障，执行屏障动作");
});

// 每个线程
new Thread(() -> {
    doPartWork();
    barrier.await(); // 等待其他线程
    doRemainingWork();
}).start();

// CountDownLatch vs CyclicBarrier
// - CountDownLatch：一等多（await 阻塞调用者，countDown 在工作者）
// - CyclicBarrier：多等多（所有线程在屏障点等待）
// - CountDownLatch 不可重置，CyclicBarrier 可 reset() 重置

// Semaphore：信号量，控制并发访问数
Semaphore semaphore = new Semaphore(3); // 最多 3 个线程同时访问

// 获取许可
semaphore.acquire();
try {
    accessLimitedResource();
} finally {
    semaphore.release(); // 释放许可
}

// 尝试获取（非阻塞）
if (semaphore.tryAcquire()) {
    try {
        // 获取成功
    } finally {
        semaphore.release();
    }
}

// Exchanger：两个线程交换数据
Exchanger<String> exchanger = new Exchanger<>();

new Thread(() -> {
    String data = "来自线程1的数据";
    String received = exchanger.exchange(data); // 等待另一个线程交换
    System.out.println("收到: " + received);
}).start();

new Thread(() -> {
    String data = "来自线程2的数据";
    String received = exchanger.exchange(data);
    System.out.println("收到: " + received);
}).start();
```

### 2.4 线程池

#### ThreadPoolExecutor 深度解析

**7 大核心参数**：

```java
import java.util.concurrent.*;

ThreadPoolExecutor executor = new ThreadPoolExecutor(
    corePoolSize,      // 核心线程数（常驻存活）
    maximumPoolSize,   // 最大线程数
    keepAliveTime,     // 非核心线程空闲存活时间
    TimeUnit.SECONDS,  // 时间单位
    workQueue,         // 阻塞队列（存放等待的任务）
    threadFactory,     // 线程工厂（自定义线程名、守护线程等）
    rejectionHandler   // 拒绝策略
);
```

**任务提交流程**：

```
提交任务
  |
  v
当前线程数 < corePoolSize?
  ├── 是 -> 创建核心线程执行任务
  └── 否 -> 尝试放入 workQueue
              ├── 队列未满 -> 放入队列等待执行
              └── 队列已满 -> 当前线程数 < maxPoolSize?
                              ├── 是 -> 创建非核心线程执行任务
                              └── 否 -> 执行拒绝策略
```

**4 种拒绝策略**：

```java
// 1. AbortPolicy（默认）：抛出 RejectedExecutionException
new ThreadPoolExecutor.AbortPolicy();

// 2. CallerRunsPolicy：调用者线程直接执行任务（减缓提交速度）
new ThreadPoolExecutor.CallerRunsPolicy();

// 3. DiscardPolicy：直接丢弃任务，不抛异常
new ThreadPoolExecutor.DiscardPolicy();

// 4. DiscardOldestPolicy：丢弃队列中最老的任务，重新提交
new ThreadPoolExecutor.DiscardOldestPolicy();
```

#### Executors 工厂方法的陷阱

```java
// 陷阱 1：无界队列导致 OOM
ExecutorService cachedPool = Executors.newCachedThreadPool();
// 内部是 SynchronousQueue + maxPoolSize = Integer.MAX_VALUE
// 提交过快时可能创建大量线程，导致 OOM

// 陷阱 2：无界队列导致 OOM
ExecutorService fixedPool = Executors.newFixedThreadPool(10);
// 内部是 LinkedBlockingQueue（无界），任务积压可能导致 OOM

// 陷阱 3：无界队列 + 单线程
ExecutorService singlePool = Executors.newSingleThreadExecutor();
// 同样使用无界 LinkedBlockingQueue

// 最佳实践：手动创建 ThreadPoolExecutor
ThreadPoolExecutor bestExecutor = new ThreadPoolExecutor(
    5,                        // corePoolSize
    10,                       // maxPoolSize
    60, TimeUnit.SECONDS,     // keepAliveTime
    new ArrayBlockingQueue<>(100), // 有界队列
    new ThreadFactoryBuilder()
        .setNameFormat("biz-pool-%d")
        .setDaemon(false)
        .build(),
    new ThreadPoolExecutor.CallerRunsPolicy()
);

// 优雅关闭
bestExecutor.shutdown(); // 不再接收新任务，等待已有任务完成
try {
    if (!bestExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
        bestExecutor.shutdownNow(); // 超时则强制关闭
    }
} catch (InterruptedException e) {
    bestExecutor.shutdownNow();
}
```

### 2.5 Future / CompletableFuture

#### Future 的局限性

```java
// 传统 Future
ExecutorService executor = Executors.newFixedThreadPool(4);
Future<String> future = executor.submit(() -> {
    Thread.sleep(1000);
    return "结果";
});

// 阻塞获取结果
String result = future.get(); // 阻塞主线程
// future.get(1, TimeUnit.SECONDS); // 带超时

// 局限：无法异步回调、无法组合多个异步任务
```

#### CompletableFuture（JDK 8 异步编程利器）

```java
import java.util.concurrent.CompletableFuture;

// 创建异步任务
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        // 异步执行
        return "Hello";
    });

// 1. thenApply：转换结果（同步）
CompletableFuture<String> transformed = future
    .thenApply(result -> result + " World");

// 2. thenAccept：消费结果，不返回值
future.thenAccept(result -> System.out.println(result));

// 3. thenCompose：异步组合（扁平化）
CompletableFuture<String> composed = future
    .thenCompose(result ->
        CompletableFuture.supplyAsync(() -> result + " from async")
    );

// 4. thenCombine：合并两个独立异步任务
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "A");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "B");
CompletableFuture<String> combined = f1.thenCombine(f2, (a, b) -> a + b);
// 结果: "AB"

// 5. 异常处理
CompletableFuture<String> withException = CompletableFuture
    .supplyAsync(() -> {
        if (Math.random() > 0.5) throw new RuntimeException("出错");
        return "成功";
    })
    .exceptionally(ex -> "默认值")    // 异常时返回默认值
    .handle((result, ex) -> {          // 无论是否异常都会执行
        return ex != null ? "降级" : result;
    });

// 6. 等待多个任务完成
CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> "A");
CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> "B");
CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> "C");

// allOf：等待所有完成
CompletableFuture<Void> allFutures = CompletableFuture
    .allOf(task1, task2, task3);
allFutures.thenApply(v ->
    Stream.of(task1, task2, task3)
        .map(CompletableFuture::join)
        .collect(Collectors.joining(","))
);

// anyOf：任一完成即返回
CompletableFuture<Object> anyFuture = CompletableFuture
    .anyOf(task1, task2, task3);
```

### 2.6 ForkJoinPool

工作窃取算法 (Work-Stealing) 的实现，适合**分治**计算密集型任务。

```java
import java.util.concurrent.*;

// 计算 1+2+...+100 的分治实现
class SumTask extends RecursiveTask<Long> {
    private static final int THRESHOLD = 10;
    private final int[] arr;
    private final int start;
    private final int end;

    public SumTask(int[] arr, int start, int end) {
        this.arr = arr;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            // 小任务直接计算
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += arr[i];
            }
            return sum;
        }
        // 大任务拆分
        int mid = (start + end) / 2;
        SumTask left = new SumTask(arr, start, mid);
        SumTask right = new SumTask(arr, mid, end);

        left.fork();   // 异步执行左子任务
        long rightResult = right.compute(); // 当前线程执行右子任务
        long leftResult = left.join();      // 等待左子任务结果

        return leftResult + rightResult;
    }
}

// 使用 ForkJoinPool
int[] arr = IntStream.rangeClosed(1, 100).toArray();
ForkJoinPool pool = new ForkJoinPool(); // 默认并行度 = CPU 核数
SumTask task = new SumTask(arr, 0, arr.length);
long result = pool.invoke(task); // 5050
```

**工作窃取原理**：
- 每个工作线程维护一个双端队列
- 线程执行自己队列中的任务，若队列空，则从其他线程队列尾部**窃取**
- 窃取时从尾部取，正常执行从头部取，减少竞争

---

## 3. NIO (New I/O)

NIO（JDK 1.4 引入）提供了非阻塞 I/O 模型，是高性能网络编程的基础。

### 3.1 Buffer 缓冲区

Buffer 是 NIO 的数据容器，核心三个指针：

```java
import java.nio.*;

// 创建 Buffer
ByteBuffer buffer = ByteBuffer.allocate(1024); // 堆内存
// ByteBuffer.allocateDirect(1024); // 直接内存（零拷贝）

// 核心属性
// capacity: 缓冲区容量（不可变）
// position: 当前操作位置
// limit:    可操作数据上限

// 写入数据
buffer.put((byte) 'H');
buffer.put((byte) 'e');
buffer.put((byte) 'l');

// 读取前必须调用 flip()
buffer.flip(); // limit = position, position = 0

// 读取数据
byte first = buffer.get(); // 'H'
byte second = buffer.get(); // 'e'

// 重置（复用缓冲区）
buffer.rewind(); // position = 0，可重新读取

// 清空（准备写入）
buffer.clear(); // position = 0, limit = capacity

// 压缩（保留未读数据）
buffer.compact(); // 将未读数据移到缓冲区头部
```

**flip() / clear() / compact() 对比**：

```java
// flip()：写模式 -> 读模式
//   limit = position; position = 0;
// clear()：清空缓冲区，准备写入
//   position = 0; limit = capacity;
// compact()：压缩，保留未读数据继续写入
//   将 position ~ limit 之间的数据移到 0 ~ (limit-position)
//   position = limit - position; limit = capacity;
```

### 3.2 Channel 通道

Channel 是双向数据传输通道（与 IO 流的单向不同）。

```java
import java.io.*;
import java.nio.channels.*;

// FileChannel：文件读写
try (RandomAccessFile file = new RandomAccessFile("data.txt", "rw");
     FileChannel channel = file.getChannel()) {

    // 从 Channel 读入 Buffer
    ByteBuffer buf = ByteBuffer.allocate(1024);
    int bytesRead = channel.read(buf); // 返回读取字节数（-1 表示 EOF）

    // 从 Buffer 写入 Channel
    buf.flip();
    channel.write(buf);
}

// SocketChannel / ServerSocketChannel：网络通信
// 非阻塞模式下可与 Selector 配合使用
SocketChannel socketChannel = SocketChannel.open();
socketChannel.configureBlocking(false); // 设置为非阻塞模式
socketChannel.connect(new InetSocketAddress("localhost", 8080));
```

### 3.3 Selector 多路复用

Selector 是 NIO 非阻塞 I/O 的核心，使用 **Reactor 模式**实现单线程处理多个 Channel。

```java
import java.nio.channels.*;
import java.util.*;

// 创建 Selector
Selector selector = Selector.open();

// 注册 Channel 到 Selector
ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.configureBlocking(false);
serverChannel.socket().bind(new InetSocketAddress(8080));

SelectionKey key = serverChannel.register(selector, SelectionKey.OP_ACCEPT);
// 感兴趣的事件类型（可组合）：
// OP_ACCEPT  - 接受连接（ServerSocketChannel）
// OP_CONNECT - 连接建立（SocketChannel）
// OP_READ    - 可读
// OP_WRITE   - 可写

// Reactor 模式的事件循环
while (true) {
    // 阻塞直到有事件发生
    int readyChannels = selector.select(); // 或 select(1000) 超时

    if (readyChannels == 0) {
        continue;
    }

    Set<SelectionKey> selectedKeys = selector.selectedKeys();
    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

    while (keyIterator.hasNext()) {
        SelectionKey sk = keyIterator.next();

        if (sk.isAcceptable()) {
            // 接受新连接
            ServerSocketChannel ssc = (ServerSocketChannel) sk.channel();
            SocketChannel clientChannel = ssc.accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);

        } else if (sk.isReadable()) {
            // 读取数据
            SocketChannel clientChannel = (SocketChannel) sk.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            clientChannel.read(buffer);
            buffer.flip();
            // 处理读取到的数据...
        }

        keyIterator.remove(); // 移除已处理的 key
    }
}
```

### 3.4 零拷贝 (Zero-Copy)

零拷贝技术避免了数据在内核空间和用户空间之间的多次复制，显著提升文件传输性能。

```java
import java.io.*;
import java.nio.channels.*;

// 传统 IO 数据拷贝路径：
// 磁盘 -> 内核缓冲 -> 用户缓冲 -> Socket 缓冲 -> 网卡
// 共 4 次拷贝 + 4 次上下文切换

// 零拷贝数据路径：
// 磁盘 -> 内核缓冲 -> 网卡（DMA 直接传输）
// 共 2-3 次拷贝 + 2 次上下文切换

// FileChannel.transferTo() / transferFrom()
try (RandomAccessFile sourceFile = new RandomAccessFile("source.pdf", "r");
     RandomAccessFile destFile = new RandomAccessFile("dest.pdf", "rw");
     FileChannel sourceChannel = sourceFile.getChannel();
     FileChannel destChannel = destFile.getChannel()) {

    // 从 sourceChannel 传输 0~length 数据到 destChannel
    long position = 0;
    long count = sourceChannel.size();
    sourceChannel.transferTo(position, count, destChannel);
}

// 网络文件传输（零拷贝到 Socket）
try (FileChannel fileChannel = new FileInputStream("large-file.zip").getChannel();
     SocketChannel socketChannel = SocketChannel.open()) {

    socketChannel.connect(new InetSocketAddress("host", 8080));

    long position = 0;
    long count = fileChannel.size();
    long transferred = fileChannel.transferTo(position, count, socketChannel);
    // Linux 上 transferTo() 底层调用 sendfile() 系统调用
}
```

---

## 4. Stream API 与 Lambda

Lambda 表达式和 Stream API 是 Java 8 最重要的两项革新，改变了 Java 的编程范式。

### 4.1 Lambda 语法

```java
// Lambda 语法：(参数列表) -> { 方法体 }

// 基本形式
// (Type param1, Type param2) -> { return expression; }

// 简化规则：
// 1. 参数类型可省略（编译器推断）
// 2. 只有一个参数时可省略括号
// 3. 方法体只有一条语句时可省略花括号和 return

// 示例
Runnable task = () -> System.out.println("Hello");       // 无参数
Consumer<String> printer = s -> System.out.println(s);   // 单参数
BinaryOperator<Integer> sum = (a, b) -> a + b;            // 多参数
```

**函数式接口 (@FunctionalInterface)**：

```java
// 四大核心函数式接口
// Supplier<T>：     T get()                     —— 提供者
// Consumer<T>：     void accept(T)              —— 消费者
// Function<T, R>：  R apply(T)                  —— 转换
// Predicate<T>：    boolean test(T)             —— 断言

Supplier<String> supplier = () -> "Hello";
Consumer<String> consumer = s -> System.out.println(s);
Function<String, Integer> function = String::length;
Predicate<String> predicate = s -> s.startsWith("A");

// 其他常用函数式接口
// BiFunction<T, U, R>：     R apply(T, U)
// UnaryOperator<T>：        T apply(T)（Function 特化）
// BinaryOperator<T>：       T apply(T, T)
// BiConsumer<T, U>：        void accept(T, U)
// BiPredicate<T, U>：       boolean test(T, U)
```

**方法引用**：

```java
// 静态方法引用：ClassName::staticMethod
Function<String, Integer> parseInt = Integer::parseInt; // (s) -> Integer.parseInt(s)

// 实例方法引用（特定对象）：instance::method
String str = "hello";
Supplier<Integer> length = str::length; // () -> str.length()

// 实例方法引用（任意对象）：ClassName::method
Function<String, Integer> strLength = String::length; // (s) -> s.length()

// 构造函数引用：ClassName::new
Supplier<List<String>> listCreator = ArrayList::new; // () -> new ArrayList<>()
Function<String, File> fileCreator = File::new;      // (name) -> new File(name)
```

### 4.2 Stream 操作

```java
import java.util.stream.*;

// ========== 创建 Stream ==========

// 1. 从集合创建
List<String> list = Arrays.asList("A", "B", "C");
Stream<String> streamFromList = list.stream();
Stream<String> parallelStream = list.parallelStream();

// 2. 从数组创建
int[] numbers = {1, 2, 3, 4, 5};
IntStream intStream = Arrays.stream(numbers);

// 3. 使用 Stream.of
Stream<Integer> streamOf = Stream.of(1, 2, 3);

// 4. 使用 range
IntStream.range(1, 10);     // 1,2,3,4,5,6,7,8,9（不包含 10）
IntStream.rangeClosed(1, 10); // 1,2,...,10（包含 10）

// 5. 无限流
Stream.generate(Math::random).limit(5);
Stream.iterate(0, n -> n + 2).limit(10);

// 6. 文件行流
// Stream<String> lines = Files.lines(Paths.get("file.txt"));

// ========== 中间操作（惰性求值） ==========

// filter：过滤
List<String> filtered = list.stream()
    .filter(s -> s.startsWith("A"))
    .collect(Collectors.toList());

// map：映射
List<Integer> lengths = list.stream()
    .map(String::length)
    .collect(Collectors.toList());

// flatMap：扁平化映射（Stream of Streams -> Stream）
List<String> words = Arrays.asList("Hello", "World");
List<String> letters = words.stream()
    .flatMap(word -> Arrays.stream(word.split("")))
    .distinct()
    .collect(Collectors.toList());

// distinct：去重
// sorted：排序
// peek：调试（中间操作，消费每个元素，不改变数据）
// limit / skip：截取

List<Integer> nums = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
List<Integer> processed = nums.stream()
    .filter(n -> n > 2)
    .distinct()
    .sorted()
    .peek(System.out::println) // 调试
    .limit(3)
    .collect(Collectors.toList());

// ========== 终端操作（触发计算） ==========

// collect：收集
List<String> collected = list.stream().collect(Collectors.toList());

// forEach：遍历
list.stream().forEach(System.out::println);

// reduce：归约
Optional<Integer> sum = nums.stream().reduce(Integer::sum);
int sumInt = nums.stream().reduce(0, Integer::sum); // 带初始值

// count：计数
long count = list.stream().filter(s -> s.length() > 2).count();

// anyMatch / allMatch / noneMatch：匹配检查
boolean hasA = list.stream().anyMatch(s -> s.equals("A"));
boolean allLong = list.stream().allMatch(s -> s.length() > 0);
boolean noneEmpty = list.stream().noneMatch(String::isEmpty);

// findFirst / findAny：查找
Optional<String> first = list.stream().findFirst();
Optional<String> any = list.parallelStream().findAny(); // 并行流用 findAny
```

### 4.3 Collector 收集器

```java
// 1. toList
List<String> listResult = stream.collect(Collectors.toList());

// 2. toMap
// keyMapper / valueMapper / mergeFunction（处理重复 key）
Map<Integer, String> map = list.stream()
    .collect(Collectors.toMap(
        String::length,   // key
        Function.identity(), // value
        (v1, v2) -> v1     // 重复 key 取第一个
    ));

// 3. groupingBy：分组
Map<Integer, List<String>> grouping = list.stream()
    .collect(Collectors.groupingBy(String::length));
// {1=[A], 3=[ABC, BCD], 5=[Hello]}

// 下游收集器
Map<Integer, Long> counting = list.stream()
    .collect(Collectors.groupingBy(
        String::length,
        Collectors.counting() // 统计每组个数
    ));

Map<Integer, Set<String>> groupingToSet = list.stream()
    .collect(Collectors.groupingBy(
        String::length,
        Collectors.toCollection(TreeSet::new) // 自定义下游容器
    ));

// 4. partitioningBy：分区（true/false 两组）
Map<Boolean, List<String>> partitioned = list.stream()
    .collect(Collectors.partitioningBy(s -> s.length() > 2));
// {true=[ABC, BCD, Hello], false=[A, B]}

// 5. joining：字符串拼接
String joined = list.stream().collect(Collectors.joining(", "));
// A, B, ABC, BCD, Hello

// 6. summarizingInt：统计
IntSummaryStatistics stats = nums.stream()
    .collect(Collectors.summarizingInt(Integer::intValue));
// stats.getCount(), stats.getSum(), stats.getMin(),
// stats.getMax(), stats.getAverage()

// 7. reducing：通用归约
Optional<Integer> max = nums.stream()
    .collect(Collectors.reducing(Integer::max));

// 8. collectingAndThen：收集后处理
List<String> unmodifiableList = list.stream()
    .collect(Collectors.collectingAndThen(
        Collectors.toList(),
        Collections::unmodifiableList
    ));
```

### 4.4 并行流

```java
// 并行流底层使用 ForkJoinPool.commonPool()
// 默认并行度 = Runtime.getRuntime().availableProcessors() - 1

// 启动并行流
list.parallelStream().forEach(System.out::println);
// 或
list.stream().parallel().map(x -> heavyComputation(x)).collect(Collectors.toList());

// 自定义 ForkJoinPool（JDK 8 起）
ForkJoinPool customPool = new ForkJoinPool(4);
try {
    customPool.submit(() ->
        list.parallelStream().forEach(System.out::println)
    ).get();
} finally {
    customPool.shutdown();
}
```

**何时使用并行流**：

```java
// 适用场景（同时满足）：
// 1. 数据量大（>10000 元素）
// 2. 计算密集型（CPU 密集型，非 IO 密集型）
// 3. 无状态（不依赖外部可变状态）
// 4. 可拆分（ArrayList/数组等随机访问数据结构）

// 不适用场景：
// - IO 密集型（大量调用网络/磁盘操作）
// - 有状态操作（多线程共享可变变量）
// - 数据量小（并行化开销 > 计算收益）
// - LinkedList（拆分困难，需遍历）

// 性能对比示例
long start = System.currentTimeMillis();
long sumSequential = LongStream.rangeClosed(1, 10_000_000)
    .sum(); // 串行
long seqTime = System.currentTimeMillis() - start;

start = System.currentTimeMillis();
long sumParallel = LongStream.rangeClosed(1, 10_000_000)
    .parallel()
    .sum(); // 并行
long parTime = System.currentTimeMillis() - start;

System.out.println("串行: " + seqTime + "ms, 并行: " + parTime + "ms");
// 通常并行比串行快 3-4 倍（4 核 CPU）
```

---

## 5. 其他核心类库

### 5.1 java.time 日期时间 API

JDK 8 引入的 `java.time` 包，修复了老版 Date/Calendar 的设计缺陷（可变、线程不安全、月份从 0 开始等）。

```java
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;

// ========== 核心类 ==========

// LocalDate：日期（年-月-日）
LocalDate today = LocalDate.now();              // 当前日期
LocalDate date = LocalDate.of(2024, 3, 15);     // 指定日期
LocalDate parsed = LocalDate.parse("2024-03-15"); // 字符串解析

// 日期运算
date.plusDays(1);        // +1 天
date.plusMonths(1);      // +1 月
date.plusWeeks(1);       // +1 周
date.minusYears(1);      // -1 年
date.withDayOfMonth(1);  // 设为本月第一天

// 获取信息
date.getYear();           // 2024
date.getMonth();          // MARCH（Month 枚举）
date.getMonthValue();     // 3
date.getDayOfWeek();      // FRIDAY（DayOfWeek 枚举）
date.getDayOfMonth();     // 15
date.lengthOfMonth();     // 31（本月天数）
date.isLeapYear();        // true（闰年）

// LocalTime：时间（时:分:秒.纳秒）
LocalTime now = LocalTime.now();                 // 当前时间
LocalTime time = LocalTime.of(14, 30, 15);       // 14:30:15
LocalTime timeWithNano = LocalTime.of(14, 30, 15, 500_000_000); // 14:30:15.5
LocalTime parsedTime = LocalTime.parse("14:30:15");

time.plusHours(2);
time.minusMinutes(30);
time.withSecond(0); // 秒清零

// LocalDateTime：日期+时间
LocalDateTime dateTime = LocalDateTime.now();
LocalDateTime dt = LocalDateTime.of(2024, 3, 15, 14, 30);
LocalDateTime combined = LocalDateTime.of(date, time);
String str = dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
// 2024-03-15T14:30:00

// Instant：时间戳（机器时间，格林威治）
Instant nowInstant = Instant.now(); // 当前 UTC 时间戳
long epochSecond = nowInstant.getEpochSecond(); // 秒级时间戳
long epochMilli = nowInstant.toEpochMilli();    // 毫秒级时间戳
Instant fromEpoch = Instant.ofEpochSecond(1_700_000_000);

// ========== Duration / Period ==========

// Duration：时间间隔（秒/纳秒）
LocalTime start = LocalTime.of(9, 0);
LocalTime end = LocalTime.of(17, 30);
Duration duration = Duration.between(start, end);
duration.toHours();       // 8
duration.toMinutes();     // 510
duration.getSeconds();    // 30600

// Period：日期间隔（年/月/日）
LocalDate startDate = LocalDate.of(2023, 1, 1);
LocalDate endDate = LocalDate.of(2024, 3, 15);
Period period = Period.between(startDate, endDate);
period.getYears();        // 1
period.getMonths();       // 2
period.getDays();         // 14

// ========== DateTimeFormatter ==========

DateTimeFormatter formatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm:ss");

// 格式化
LocalDateTime nowDt = LocalDateTime.now();
String formatted = nowDt.format(formatter);
// "2024-03-15 14:30:00"

// 解析
LocalDateTime parsedDt = LocalDateTime.parse(
    "2024-03-15 14:30:00", formatter
);

// 常用格式器
DateTimeFormatter.ISO_LOCAL_DATE;          // 2024-03-15
DateTimeFormatter.ISO_LOCAL_DATE_TIME;     // 2024-03-15T14:30:00
DateTimeFormatter.ISO_OFFSET_DATE_TIME;    // 2024-03-15T14:30:00+08:00
DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL); // 2024年3月15日 星期五

// ========== ZoneId / ZonedDateTime ==========

ZoneId shanghai = ZoneId.of("Asia/Shanghai");
ZonedDateTime zdt = ZonedDateTime.of(dateTime, shanghai);

// 时区转换
ZonedDateTime utcZdt = zdt.withZoneSameInstant(ZoneId.of("UTC"));
// 上海 14:30 -> UTC 06:30

// 获取所有可用时区
ZoneId.getAvailableZoneIds().stream()
    .filter(id -> id.startsWith("Asia"))
    .limit(5)
    .forEach(System.out::println);
// Asia/Aden, Asia/Almaty, Asia/Amman, Asia/Anadyr, Asia/Aqtau
```

### 5.2 Optional

Optional 用于优雅地处理可能为 null 的值，避免空指针异常（NPE）。

```java
import java.util.Optional;

// ========== 创建 Optional ==========

Optional<String> empty = Optional.empty();              // 空 Optional
Optional<String> nonNull = Optional.of("Hello");         // 非空值（null 时抛 NPE）
Optional<String> nullable = Optional.ofNullable(getData()); // 允许为 null

// ========== 消费值 ==========

// orElse：值存在则返回，否则返回默认值
String result = nullable.orElse("默认值");
// 注意：orElse 中默认值无论 Optional 是否为空都会计算

// orElseGet：惰性求值（推荐）
String resultLazy = nullable.orElseGet(() -> fetchDefault());
// 仅当 Optional 为空时才执行 lambda

// orElseThrow：为空时抛异常
String value = nullable.orElseThrow(
    () -> new IllegalArgumentException("值不存在")
);

// ========== 条件操作 ==========

// ifPresent：值存在时消费
nullable.ifPresent(s -> System.out.println(s));

// ifPresentOrElse：JDK 9，值存在/不存在分别处理
nullable.ifPresentOrElse(
    s -> System.out.println(s),
    () -> System.out.println("值不存在")
);

// ========== 链式操作 ==========

// map：转换（返回 Optional）
Optional<String> transformed = nullable
    .map(String::toUpperCase);

// filter：过滤
Optional<String> filtered = nullable
    .filter(s -> s.length() > 3);

// flatMap：扁平化（返回 Optional，避免嵌套）
// 适用于方法本身返回 Optional 的情况
Optional<Integer> lengthOpt = nullable
    .flatMap(s -> Optional.of(s.length()));

// ========== 实战示例 ==========

// 传统代码（多层 if-null 检查）
public String getCityFromUser(User user) {
    if (user != null) {
        Address address = user.getAddress();
        if (address != null) {
            City city = address.getCity();
            if (city != null) {
                return city.getName();
            }
        }
    }
    return "未知";
}

// Optional 重构
public String getCityFromUserOpt(User user) {
    return Optional.ofNullable(user)
        .map(User::getAddress)
        .map(Address::getCity)
        .map(City::getName)
        .orElse("未知");
}

// ========== 避免 Optional 滥用 ==========

// 不推荐：作为字段类型
// public class User {
//     private Optional<String> name; // 不推荐
// }

// 不推荐：作为方法参数
// public void setName(Optional<String> name) {
//     this.name = name;
// }

// 推荐：作为返回值，表示可能为空的结果
public Optional<String> findUserNameById(Long id) {
    // 查询数据库，可能不存在
    return Optional.ofNullable(dbQuery(id));
}

// 不推荐：在集合中使用 Optional
// List<Optional<String>> list = ...; // 不要这样用
// 推荐：使用空对象模式或过滤 null
List<String> nonNullList = list.stream()
    .filter(Objects::nonNull)
    .collect(Collectors.toList());
```

**orElse vs orElseGet 关键区别**：

```java
Optional<String> opt = Optional.of("有值");

// orElse：无论 opt 是否有值，默认值表达式都会执行
String result1 = opt.orElse(computeExpensiveDefault());
// computeExpensiveDefault() 一定执行，即使有值

// orElseGet：仅当 opt 为空时才执行默认值表达式
String result2 = opt.orElseGet(() -> computeExpensiveDefault());
// computeExpensiveDefault() 不会执行，因为 opt 有值

// 结论：默认值计算开销大时，务必使用 orElseGet
```

---

## 6. 总结清单

| 主题 | 关键知识点 | 掌握程度 |
|------|-----------|---------|
| **集合框架** | ArrayList 扩容机制、HashMap 红黑树化、ConcurrentHashMap CAS+synchronized、LRU 缓存实现 | 必须精通 |
| **并发包** | CAS 原理与 ABA 问题、LongAdder 分段累加、AQS 框架、线程池 7 参数与拒绝策略、CompletableFuture 异步编排 | 必须精通 |
| **NIO** | Buffer 指针与 flip、Selector 多路复用与 Reactor 模式、零拷贝 transferTo | 理解原理 |
| **Stream + Lambda** | 函数式接口、Stream 中间/终端操作、Collector 分组分区、并行流适用条件 | 熟练使用 |
| **java.time** | LocalDate/LocalDateTime 不可变设计、DateTimeFormatter 线程安全 | 熟练使用 |
| **Optional** | 正确链式使用、orElse vs orElseGet 惰性求值 | 熟练使用 |

### 面试高频考点

1. **HashMap**：put/get 流程、扩容机制、为什么是 2 的幂、红黑树化条件、与 ConcurrentHashMap 区别
2. **线程池**：核心参数含义、任务提交流程、拒绝策略选择、Executors 缺陷、如何合理配置线程数
3. **CAS**：底层实现、ABA 问题解决方案、与 synchronized 对比（适用场景）
4. **ConcurrentHashMap**：JDK 7 分段锁 vs JDK 8 CAS+synchronized、并发扩展示例
5. **CompletableFuture**：thenApply/thenCompose 区别、异常处理链、allOf/anyOf
6. **Stream**：中间操作 vs 终端操作、并行流原理与适用场景、Collector 自定义
7. **NIO**：BIO vs NIO vs AIO、零拷贝实现方式、Selector 事件循环

### 最佳实践建议

- **集合**：优先使用 ArrayList 和 HashMap；高并发用 ConcurrentHashMap；需要维持插入顺序用 LinkedHashMap
- **线程池**：**永远不要**用 Executors 创建线程池，使用 ThreadPoolExecutor 手动指定参数；CPU 密集型线程数 = CPU 核数 + 1，IO 密集型 = 2 * CPU 核数
- **锁**：读多写少用 ReadWriteLock 或 StampedLock 乐观读；轻量级竞争用 CAS 原子类
- **Stream**：数据量大且计算密集时用并行流；涉及 IO 或共享可变状态时用串行流
- **日期时间**：新代码全部使用 java.time 包；服务端通信使用 Instant 或 LocalDateTime + ZoneId
- **Optional**：仅作为返回值使用，不要用作字段或方法参数；有昂贵默认值时使用 orElseGet

---

> 本文深入解析了 Java 核心类库的核心原理与实战用法。集合框架是日常开发的基石，并发包是高性能服务的保障，NIO 是网络编程的基础，Stream + Lambda 让代码更简洁优雅，java.time 和 Optional 则解决了遗留 API 的设计缺陷。理解这些核心类库的实现原理，是 Java 开发者从中级迈向高级的必经之路。
