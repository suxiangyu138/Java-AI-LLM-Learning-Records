# Java并发编程 面试宝典
> 基于104节课程大纲全面覆盖Java并发编程面试高频考点，包含基础概念、深度原理、实战场景、手写代码、系统设计及常见坑点，对标阿里、腾讯、字节、美团等大厂面试。

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（20题）

### 1. 创建线程有哪几种方式？
**三种核心方式：继承`Thread`、实现`Runnable`、实现`Callable`配合`FutureTask`。**
继承`Thread`需重写`run()`方法，缺点是Java单继承限制；实现`Runnable`更灵活，无返回值；实现`Callable`可通过`FutureTask`包装后提交给线程池执行，**支持返回值和异常抛出**。实际开发中**优先使用`Runnable`或`Callable`**，避免单继承带来的耦合。线程池提交时`submit(Callable)`返回`Future<?>`，通过`future.get()`阻塞获取结果。

### 2. `start()`和`run()`的区别是什么？
**`start()`启动新线程，`run()`是普通方法调用。**
调用`start()`会让JVM创建一个新线程，由新线程执行`run()`内的代码；直接调用`run()`则是由当前线程同步执行，没有创建新线程。面试常见陷阱：一道题问"调用两次`start()`会怎样"——**会抛出`IllegalThreadStateException`**，因为线程状态已从`NEW`变为`RUNNABLE`。

### 3. `sleep()`和`wait()`有什么区别？
| 区别点 | `sleep()` | `wait()` |
|--------|-----------|----------|
| 所属类 | `Thread`的静态方法 | `Object`的实例方法 |
| 是否释放锁 | **不释放**，抱着锁睡 | **释放**，让出Monitor |
| 唤醒方式 | 时间到自动唤醒 | `notify()`/`notifyAll()`或超时 |
| 调用前提 | 无需 | **必须**在`synchronized`块内 |
| 用途 | 暂停执行、模拟耗时 | 线程间通信协作 |

> 💡 **核心记忆点**：`sleep`抱着锁睡，`wait`放手等通知。

### 4. `synchronized`的底层实现原理是什么？
**基于Monitor（管程）对象实现，依赖操作系统的Mutex Lock。**
在JVM中，每个对象都有一个**对象头（Object Header）**，其中Mark Word存储锁状态信息。`synchronized`编译后会生成`monitorenter`和`monitorexit`两条字节码指令。执行`monitorenter`时尝试获取对象Monitor的所有权，如果计数器为0则获取成功并+1，**可重入**（计数器累加）。JDK6之后引入锁升级机制：无锁 → 偏向锁 → 轻量级锁 → 重量级锁。

### 5. volatile关键字的作用是什么？
**保证可见性和有序性，不保证原子性。**
- **可见性**：对`volatile`变量的写操作会立即刷新到主内存，读操作会从主内存重新加载，禁止线程本地缓存。
- **有序性**：通过**内存屏障（Memory Barrier）** 禁止指令重排序，JMM在`volatile`读写前后插入屏障。
- 典型应用：**DCL单例（Double-Checked Locking）**中防止半初始化对象被其他线程读取。
- 不适用场景：`count++`这种**读-改-写**操作，仍需加锁或使用`AtomicInteger`。

### 6. 什么是 happens-before 规则？
**Happens-before是JMM定义的多线程操作可见性偏序关系。** 如果一个操作happens-before另一个操作，则第一个操作的结果对第二个操作可见。核心规则有8条，面试重点掌握：
- **程序次序规则**：同一线程中，前一个操作happens-before后一个。
- **管程锁定规则**：`unlock` happens-before后一个`lock`。
- **volatile规则**：对`volatile`变量的写 happens-before 读。
- **传递性**：A happens-before B，B happens-before C => A happens-before C。
- **线程启动规则**：`Thread.start()` happens-before被启动线程的任意操作。
- **线程终止规则**：线程中所有操作 happens-before `Thread.join()`返回。

### 7. CAS的原理是什么？存在什么问题？
**CAS（Compare-And-Swap）是一种无锁原子操作，包含三个操作数：内存地址V、预期值A、新值B。** 当V的值等于A时，CAS才将V更新为B，否则不操作。底层由CPU的`cmpxchg`指令实现，保证原子性。
**三大问题**：
- **ABA问题**：变量从A→B→A，CAS误判未修改。解决：`AtomicStampedReference`带版本号，`AtomicMarkableReference`带布尔标记。
- **自旋开销大**：高竞争下CAS反复重试消耗CPU。解决：`LongAdder`分段降低冲突。
- **只能保证一个共享变量的原子操作**：需配合`AtomicReference`封装多个变量。

### 8. ReentrantLock和synchronized有什么区别？
| 对比项 | synchronized | ReentrantLock |
|--------|-------------|---------------|
| 锁机制 | 关键字，JVM层面 | API层面（`java.util.concurrent.locks`） |
| 可重入 | 是 | 是 |
| 中断响应 | 不支持 | 支持`lockInterruptibly()` |
| 超时获取 | 不支持 | 支持`tryLock(timeout, unit)` |
| 公平性 | 非公平 | 可配置公平/非公平 |
| 条件等待 | 一个等待集 | 多个`Condition` |
| 解锁方式 | 自动释放 | 必须**手动`unlock()`**（常放`finally`） |

> ⚠️ **注意**：`ReentrantLock`必须在`finally`中释放锁，否则异常时锁无法释放导致死锁。

### 9. 线程池的核心参数有哪些？
**`ThreadPoolExecutor`有7个核心参数**：
1. **`corePoolSize`**：核心线程数，即使空闲也保留。
2. **`maximumPoolSize`**：最大线程数。
3. **`keepAliveTime`**：非核心线程空闲存活时间。
4. **`unit`**：时间单位。
5. **`workQueue`**：任务阻塞队列（如`LinkedBlockingQueue`、`ArrayBlockingQueue`、`SynchronousQueue`）。
6. **`threadFactory`**：线程工厂（常用`Executors.defaultThreadFactory()`）。
7. **`handler`**：拒绝策略（`AbortPolicy`、`CallerRunsPolicy`、`DiscardPolicy`、`DiscardOldestPolicy`）。

**执行流程**：提交任务 → 核心线程未满则新建 → 队列未满则入队 → 线程数未达最大值则新建非核心线程 → 执行拒绝策略。

### 10. 线程池的5种状态是什么？
- **`RUNNING`**：接受新任务并处理队列任务。
- **`SHUTDOWN`**：不接受新任务，但处理队列中的剩余任务（调用`shutdown()`进入）。
- **`STOP`**：不接受新任务，不处理队列任务，中断正在执行的任务（调用`shutdownNow()`进入）。
- **`TIDYING`**：所有任务终止，线程数为0，即将执行`terminated()`。
- **`TERMINATED`**：`terminated()`执行完毕。

**状态流转**：`RUNNING → SHUTDOWN/STOP → TIDYING → TERMINATED`。

### 11. `ThreadLocal`的原理是什么？内存泄漏如何避免？
**每个线程维护一个`ThreadLocalMap`，key为`ThreadLocal`弱引用，value为实际数据。** 存取时以当前`ThreadLocal`实例为key操作当前线程的Map，实现了线程间数据隔离。

**内存泄漏问题**：`ThreadLocalMap`的key是弱引用，GC后key变为`null`，但value仍有强引用链（Thread → ThreadLocalMap → Entry → value）。线程不结束则value永远无法回收。

**解决方案**：使用完**务必调用`remove()`**清理Entry。Tomcat等线程池场景下线程复用，若不清理会导致下个请求读到脏数据。

### 12. `CountDownLatch`和`CyclicBarrier`的区别？
| 对比项 | CountDownLatch | CyclicBarrier |
|--------|---------------|---------------|
| 重用性 | **不可重用**，计数归零失效 | **可重用**，`reset()`重置 |
| 角色 | 一个或多个线程等待其他线程完成 | 所有线程互相等待到达屏障 |
| 触发动作 | 无 | `barrierAction`（到达时的Runnable） |
| 计数方式 | `countDown()`减计数 | `await()`减计数，到0触发 |
| 应用场景 | 并行任务汇总、服务启动等待 | 多阶段计算、并行处理拆分 |

### 13. 什么是线程安全？如何实现？
**线程安全指多个线程访问共享数据时，保证数据的一致性和完整性。** 实现方式：
- **互斥同步**：`synchronized`、`ReentrantLock`、`ReentrantReadWriteLock`。
- **非阻塞同步**：CAS、原子类、`LongAdder`。
- **无同步方案**：`ThreadLocal`（线程封闭）、不可变对象（`final`修饰）、`CopyOnWriteArrayList`（写时复制）。
- **并发容器**：`ConcurrentHashMap`、`BlockingQueue`等。

### 14. ThreadPoolExecutor的拒绝策略有哪些？
- **`AbortPolicy`**：默认策略，直接抛出`RejectedExecutionException`。
- **`CallerRunsPolicy`**：调用线程（提交任务的线程）自己执行该任务，**降低任务提交速度**，起到限流作用。
- **`DiscardPolicy`**：直接丢弃，不抛异常。
- **`DiscardOldestPolicy`**：丢弃队列中最旧的任务（队首），然后重新提交。
- **自定义**：实现`RejectedExecutionHandler`接口。

> 🎯 **阿里开发规约**明确禁止使用`Executors.newFixedThreadPool()`（队列`Integer.MAX_VALUE`）和`Executors.newCachedThreadPool()`（最大线程`Integer.MAX_VALUE`），必须手动创建`ThreadPoolExecutor`。

### 15. AQS是什么？核心原理是什么？
**AQS（AbstractQueuedSynchronizer）是Java并发包的基石，一个用于构建锁和同步器的框架。** 核心思想：
- **内部状态`state`**：用`volatile int`表示同步状态，通过CAS修改。
- **CLH变体双向队列**：获取锁失败时线程封装成Node入队，通过自旋+`LockSupport.park()`阻塞。
- **模板方法模式**：子类实现`tryAcquire()`、`tryRelease()`等方法来控制`state`的获取与释放。

**基于AQS实现的同步器**：`ReentrantLock`、`CountDownLatch`、`Semaphore`、`ReentrantReadWriteLock`、`ThreadPoolExecutor`的Worker等。

### 16. `ScheduledThreadPoolExecutor`相比`Timer`的优势？
- **Timer单线程**：一个任务异常导致整个Timer终止，后续任务不执行。
- **Timer基于绝对时间**：系统时间调整影响任务触发。
- **Timer不支持循环调度**：`scheduleAtFixedRate`是`ScheduledThreadPoolExecutor`的优点。
- **Timer无法处理异常**：运行时异常会终止Timer线程；`ScheduledThreadPoolExecutor`是多线程，异常仅影响当前任务。

> 💡 **ScheduledThreadPoolExecutor**本质上是`ThreadPoolExecutor`的子类，使用`DelayedWorkQueue`（无界延迟队列）。

### 17. `ConcurrentHashMap` JDK7和JDK8的区别？
| 对比项 | JDK7 | JDK8 |
|--------|------|------|
| 数据结构 | Segment数组 + HashEntry数组 | Node数组 + 链表/红黑树 |
| 锁粒度 | **Segment分段锁**（ReentrantLock） | **CAS + synchronized**（仅锁链表/树头节点） |
| 并发度 | 默认16个Segment | 更细粒度，理论上支持更大并发 |
| 扩容 | Segment内扩容 | 多线程协作扩容 |
| 查询性能 | 遍历链表 | 链表转红黑树（≥8），O(n)→O(log n) |
| size() | 先不加锁尝试，失败锁所有Segment | 使用CounterCell数组累加，性能更高 |

### 18. `CopyOnWriteArrayList`的原理是什么？适用场景？
**写时复制**：所有修改操作（add、set、remove）都**先加锁**（ReentrantLock），复制一份新数组，在新数组上修改，然后将volatile数组引用指向新数组。读操作不加锁，直接在原数组上读取。

**优点**：读操作无锁，适合**读多写少**场景。
**缺点**：写操作复制数组开销大，内存占用翻倍；存在**弱一致性**问题（读写分离，读可能读到旧数据）。
**典型应用**：事件监听器列表、黑名单/白名单配置。

### 19. 什么是Fork/Join框架？
**Fork/Join是大任务拆分小任务并行计算，最后合并结果的框架，核心是**工作窃取（Work-Stealing）**算法**。工作线程的队列为空时，从其他线程的队列尾部"窃取"任务执行，减少线程等待。
- **`ForkJoinPool`**：线程池实现。
- **`RecursiveTask`**：有返回值的任务。
- **`RecursiveAction`**：无返回值的任务。
- **`ForkJoinTask`**：任务抽象基类。

> ⚠️ **适用条件**：任务可拆分的计算密集型场景（如归并排序、矩阵乘法），不适合IO密集型。

### 20. `LongAdder`的原理是什么？相比`AtomicLong`的优势？
**LongAdder采用分段CAS + 最终求和**的思想解决高并发下CAS自旋开销大的问题。内部维护一个`base`变量和一个`Cell[]`数组，线程通过hash映射到不同Cell上执行CAS累加，最后求和时`sum() = base + sum(Cells)`。

**优势**：高并发下性能显著优于`AtomicLong`（从单点CAS竞争变为多点CAS竞争）。
**劣势**：`sum()`不是强一致性的快照，适合**统计类场景**（如QPS计数），不适合精确扣减。

---

## 二、深度原理剖析（15题）

### 1. synchronized锁升级全过程（偏向锁 → 轻量级锁 → 重量级锁）

JDK 6之后`synchronized`是**可伸缩的锁**，根据竞争程度自动升级（**只能升级不能降级**）：

```
无锁 → 偏向锁 → 轻量级锁（自旋锁） → 重量级锁
```

| 阶段 | 触发条件 | Mark Word内容 | 开销 |
|------|----------|--------------|------|
| **偏向锁** | 同一线程第一次获取锁 | 线程ID + epoch | 极低（一次CAS） |
| **轻量级锁** | 另一线程竞争，但**错峰执行** | 锁记录指针（LR） | 自旋（CPU友好） |
| **重量级锁** | 自旋超过阈值或自旋线程数超过CPU核数一半 | Monitor指针 | 线程挂起（OS内核态） |

**偏向锁撤销**：当其他线程尝试获取偏向锁时，需**等待全局安全点（SafePoint）**，暂停偏向线程，检查其是否存活。这是偏向锁在JDK 15、JDK 21中被逐步废弃的原因之一。

> 🎯 **字节跳动面试题**："描述一个`synchronized`方法被5个线程轮流调用的锁开销变化。"

### 2. AQS源码分析——ReentrantLock的lock()流程

```java
// ReentrantLock.NonfairSync.lock()
final void lock() {
    // 第一步：尝试CAS设置state从0→1，直接抢占（非公平体现）
    if (compareAndSetState(0, 1))
        setExclusiveOwnerThread(Thread.currentThread());
    else
        acquire(1);  // 抢锁失败，进入AQS
}

// AbstractQueuedSynchronizer.acquire()
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&          // 子类实现的尝试获取逻辑
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))  // 入队 + 自旋
        selfInterrupt();
}
```

**完整流程**：
1. **快速抢占**：CAS设置`state`从0到1，成功直接获得锁（非公平体现）。
2. **tryAcquire**：若`state=0`再次尝试CAS；若当前线程已持有锁（`state>0`），累加`state`，实现**可重入**。
3. **addWaiter**：将线程包装为`Node`，尾插到CLH队列（双向链表）。
4. **acquireQueued**：在循环中判断前驱是否为`head`，若是则再次尝试获取锁；否则通过`shouldParkAfterFailedAcquire()`和`parkAndCheckInterrupt()`挂起线程。
5. **unlock()**：`state-1`，若为0则释放锁，唤醒后继节点的线程（`unparkSuccessor`）。

> 💡 **关键设计**：非公平锁在`acquire()`前后各有一个CAS抢锁机会，而非天然排到队尾。

### 3. ReentrantLock的公平锁如何实现？

公平锁的`tryAcquire()`多了一个**`hasQueuedPredecessors()`**检查：

```java
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (!hasQueuedPredecessors() &&    // 关键：检查队列中是否有前驱等待
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    } else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        setState(nextc);
        return true;
    }
    return false;
}
```

**`hasQueuedPredecessors()`逻辑**：如果CLH队列中`head.next`存在且不是当前线程，返回`true`（有前驱），当前线程不能抢锁，排队。如果队列为空或`head.next`是当前线程，返回`false`，可以尝试获取。

> ⚠️ 公平锁不保证绝对公平，线程调度仍然存在不确定性，但**不会出现线程饥饿**。

### 4. volatile的内存屏障机制

JMM在volatile读写前后插入**内存屏障**来禁止指令重排序：

| 屏障类型 | 插入位置 | 效果 |
|----------|----------|------|
| **LoadLoad** | 读后 | 禁止上面的读和下面的读重排序 |
| **LoadStore** | 读后 | 禁止上面的读和下面的写重排序 |
| **StoreStore** | 写前 | 禁止上面的写和下面的写重排序 |
| **StoreLoad** | 写后 | 禁止上面的写和下面的读重排序 |

**在JSR-133实现中**：
- 对`volatile`写：前插`StoreStore`，后插`StoreLoad`。
- 对`volatile`读：后插`LoadLoad` + `LoadStore`。

**DCL单例的volatile作用**：
```java
private volatile static Singleton instance;  // 禁止instance = new Singleton()的指令重排序
```
`new Singleton()`分为三步：①分配内存 ②初始化对象 ③赋值给引用。不加volatile时，②和③可能被重排序，导致其他线程拿到未初始化的对象。

### 5. ConcurrentHashMap JDK8的put()源码分析

```java
public V put(K key, V value) {
    return putVal(key, value, false);
}

final V putVal(K key, V value, boolean onlyIfAbsent) {
    // 省略key/value非空校验...
    for (int i = 0, binCount = 0;;) {  // 自旋
        Node<K,V>[] tab = table;
        if (tab == null) // 懒初始化
            tab = initTable();
        else if ((f = tabAt(tab, i)) == null) {
            // 槽位为空：CAS插入，无锁
            if (casTabAt(tab, i, null, new Node<>(key, value, null)))
                break;
        } else if (fh == MOVED)  // 正在扩容，帮助迁移
            helpTransfer(tab, f);
        else {  // 哈希冲突：锁住链表/树头节点
            synchronized (f) {
                // 遍历链表/树，插入或覆盖
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);  // 链表转红黑树
            }
        }
    }
    addCount(1L, binCount);  // 原子累加size
    return null;
}
```

**核心设计点**：
- **数组为空时**：通过`sizeCtl`（volatile）控制并发初始化，CAS竞争成功者构造。
- **槽位为空时**：无锁CAS插入，无性能损失。
- **槽位非空时**：`synchronized`锁住链表头节点（细粒度锁）。
- **扩容时**：多线程协作，`helpTransfer`让其他线程帮忙迁移数据。
- **红黑树优化**：链表长度≥8转红黑树，查询从O(n)变O(log n)。

### 6. ConcurrentHashMap扩容机制（多线程协助）

JDK8的扩容叫**transfer()**，多个线程共同完成：

**触发时机**：`addCount()`检查元素总数超过`threshold`（`0.75 * table.length`）。
**最小容量**：`MIN_TRANSFER_STRIDE = 16`，即每个线程至少负责16个桶的迁移。

**过程**：
1. 第一个扩容线程创建`nextTable`（新数组，容量翻倍）。
2. 将旧数组划分成若干段（stride），每个线程领取一段。
3. 每个桶的迁移：如果是链表就用**高低位拆分法**（`hash & oldCap`判断是留在原位还是移动到`index + oldCap`）；如果是红黑树就拆分或退化回链表。
4. 迁移完成后在旧桶位置设置`ForwardingNode`（`fwd`），标记已完成。
5. 其他线程在put/get时遇到`fwd`节点，调用`helpTransfer()`协助扩容。

> 🎯 **美团面试题**："JDK8 ConcurrentHashMap在多线程并发put时可能会触发扩容，描述扩容过程中怎么保证数据一致性。"

### 7. ThreadPoolExecutor的execute()完整流程

```java
public void execute(Runnable command) {
    int c = ctl.get();
    // 1. 如果工作线程数 < corePoolSize，创建新核心线程
    if (workerCountOf(c) < corePoolSize) {
        if (addWorker(command, true))
            return;
        c = ctl.get();  // 重新获取ctl
    }
    // 2. 尝试入队
    if (isRunning(c) && workQueue.offer(command)) {
        int recheck = ctl.get();
        // 双重检查：线程池状态变化则回滚
        if (!isRunning(recheck) && remove(command))
            reject(command);
        else if (workerCountOf(recheck) == 0)
            addWorker(null, false);
    }
    // 3. 尝试创建非核心线程
    else if (!addWorker(command, false))
        reject(command);  // 4. 拒绝
}
```

**ctl工具**：`ctl`是一个`AtomicInteger`，高3位表示线程池状态（5种分别对应`111/000/001/010/011`），低29位表示线程数。使用`ctlOf()`、`runStateOf()`、`workerCountOf()`进行位运算操作。

### 8. wait/notify的正确使用模式

```java
// 正确模式：synchronized + while循环 + notifyAll
synchronized (lock) {
    while (!conditionMet) {   // 必须在循环中检查条件
        lock.wait();           // 防止虚假唤醒（spurious wakeup）
    }
    // do something...
    conditionMet = false;
    lock.notifyAll();          // 通知所有等待线程
}
```

**关键点**：
- **必须在`synchronized`块内**：否则抛出`IllegalMonitorStateException`。
- **`wait()`释放锁**：进入`WaitSet`，其他线程可以获取锁。
- **使用`while`而非`if`**：防止虚假唤醒（线程被唤醒但条件未满足）。
- **优先`notifyAll()`而非`notify()`**：`notify()`随机唤醒一个，可能导致信号丢失（唤醒的线程条件不满足，又wait，无人再唤醒）。

### 9. ThreadLocal的内存泄漏分析

**引用链**：
```
Thread → ThreadLocalMap → Entry(key=WeakReference<ThreadLocal>, value=强引用)
                            ↑
                        GC后变为null   value始终有强引用链可达
```

**内存泄漏发生条件**：
1. `ThreadLocal`对象被GC回收（弱引用特性）。
2. `ThreadLocalMap`的Entry中key变为`null`。
3. 线程持续存活（线程池场景），value无法被回收。
4. 下次`get()`/`set()`时有机会清理，但如果不再访问此`ThreadLocal`则不会清理。

**最佳实践**：
```java
private static final ThreadLocal<UserContext> USER_HOLDER = new ThreadLocal<>();
// 使用完必须清理
finally {
    USER_HOLDER.remove();  // 清理当前线程的Entry
}
```

> ⚠️ **千万注意**：`remove()`会同时删除key和value；`set(null)`只将value置为null，key还是弱引用，Entry还在。必须用`remove()`。

### 10. CAS的ABA问题及解决方案

**ABA场景**：线程1读到A，被线程2改成B又改回A，线程1CAS成功但实际数据已变化。

**解决方案**：

| 方案 | 原理 | 适用场景 |
|------|------|----------|
| `AtomicStampedReference` | **版本号**，每次修改版本号+1 | 需要精确知道修改次数 |
| `AtomicMarkableReference` | **布尔标记**，标记是否被修改过 | 只需知道是否被改过（如链表节点删除标记） |

```java
AtomicStampedReference<String> ref = new AtomicStampedReference<>("A", 0);
int[] stampHolder = new int[1];
String value = ref.get(stampHolder);  // value="A", stampHolder[0]=0
// CAS时需要同时比较值和版本号
ref.compareAndSet("A", "B", stampHolder[0], stampHolder[0] + 1);
```

### 11. LongAdder的伪共享（False Sharing）处理

**伪共享**：CPU缓存行通常为64字节，不同线程修改同一缓存行中的不同变量，会导致缓存行频繁失效，即使数据本身无竞争。

**LongAdder的Cell类**使用`@sun.misc.Contended`注解（或手动填充）：

```java
@sun.misc.Contended static final class Cell {
    volatile long value;
    // 通过Contended注解或padding使每个Cell独占缓存行
}
```

`@Contended`会在字段前后各填充128字节（JDK8方式），确保不同Cell不在同一缓存行上，杜绝伪共享。从JDK17起，`@Contended`在Java层不可用，需通过`--add-exports`开启。

### 12. ReentrantReadWriteLock与StampedLock的区别

| 对比项 | ReentrantReadWriteLock | StampedLock |
|--------|----------------------|-------------|
| 锁模式 | 读锁 + 写锁 | 读锁 + 写锁 + **乐观读** |
| 读写互斥 | 写锁阻塞读锁 | 乐观读不阻塞写锁 |
| 可重入 | 是 | **否**（不可重入） |
| 条件等待 | 支持 | 不支持 |
| 锁降级 | 支持写锁降级读锁 | 支持 |
| 适用场景 | 读多写少且需重入 | 读极多写极少，追求极致性能 |

```java
// StampedLock乐观读模式
StampedLock lock = new StampedLock();
long stamp = lock.tryOptimisticRead();  // 乐观读，不阻塞
// 读取数据...
if (!lock.validate(stamp)) {  // 检查期间是否有写操作
    stamp = lock.readLock();   // 升级为悲观读锁（阻塞）
    try {
        // 重新读取...
    } finally {
        lock.unlockRead(stamp);
    }
}
```

### 13. CompletableFuture的异步编排能力

CompletableFuture可以方便地编排多个异步任务，解决回调和线程池管理的复杂性：

```java
// 串行：thenApply / thenCompose
CompletableFuture.supplyAsync(() -> getPrice())
    .thenApply(price -> price * 0.9)
    .thenAccept(result -> save(result));

// 并行：thenCombine / allOf
CompletableFuture<A> f1 = CompletableFuture.supplyAsync(() -> queryDb());
CompletableFuture<B> f2 = CompletableFuture.supplyAsync(() -> callRpc());
f1.thenCombine(f2, (a, b) -> merge(a, b));

// 任意完成：applyToEither
f1.applyToEither(f2, fastest -> handle(fastest));

// 异常处理：exceptionally / handle
CompletableFuture.supplyAsync(() -> riskyOp())
    .exceptionally(e -> defaultValue)
    .handle((result, ex) -> ex == null ? result : fallback);
```

**默认线程池**：未指定线程池时使用`ForkJoinPool.commonPool()`。正式环境应自定义线程池。

### 14. 线程的6种状态及转换

| 状态 | 含义 | 进入方式 | 退出方式 |
|------|------|----------|----------|
| **NEW** | 新建 | `new Thread()` | `start()` |
| **RUNNABLE** | 就绪/运行 | `start()`、`yield()`、`notify()` | 调度、阻塞、终止 |
| **BLOCKED** | 阻塞（锁） | 未获取到synchronized锁 | 获取到锁 |
| **WAITING** | 无限等待 | `wait()`、`join()`、`park()` | `notify()`/`unpark()` |
| **TIMED_WAITING** | 限时等待 | `sleep(ms)`、`wait(ms)`、`join(ms)` | 超时/唤醒 |
| **TERMINATED** | 终止 | `run()`执行完毕 | - |

**状态转换图**：`NEW → RUNNABLE ↔ (BLOCKED / WAITING / TIMED_WAITING) → TERMINATED`

### 15. 线程上下文切换的开销与优化

**上下文切换（Context Switch）**：CPU从执行一个线程切换到另一个线程时，需要保存当前线程的**程序计数器、寄存器、栈帧**等状态，并加载新线程的状态。

**开销构成**：
- **直接开销**：寄存器保存与恢复（约1-2μs）、TLB刷新。
- **间接开销**：缓存丢失导致后续指令执行变慢（主要开销，可达数十μs）。

**优化手段**：
- 减少锁竞争（锁粗化、锁分段、无锁数据结构）。
- 使用CAS替代阻塞锁（自旋等待比挂起切换代价低）。
- 合理设置线程池大小（CPU密集型：N+1，IO密集型：2N）。
- 使用无锁编程（`ConcurrentHashMap`、`LongAdder`）。
- 使用协程/虚拟线程（JDK21 Virtual Threads，轻量级线程，切换开销极低）。

---

## 三、实战场景题（12题）

### 1. 线上CPU飙高，如何排查？
**典型场景**：某应用CPU使用率持续90%+，求排查方案。

**排查步骤**：
1. **`top -H`**：找到CPU最高的线程PID。
2. **`printf "%x\n" pid`**：将PID转为16进制。
3. **`jstack pid | grep -A 30 0x{nid} `**：查看线程堆栈，定位到具体代码行。
4. **分析堆栈**：
   - 如果在`run()`或业务代码中 → 分析死循环或密集计算。
   - 如果在`HashMap.get()` → 并发put导致死链（JDK7）。
   - 如果在`ThreadPoolExecutor$Worker.run()` → 线程池业务问题。
   - 如果在GC线程（`VM Thread`） → 频繁GC导致CPU高，需排查内存泄漏。
5. **排查GC**：`jstat -gcutil pid 1000`观察GC频率和Full GC情况。

> 💡 **速查口诀**：`top`看进程，`top -H`找线程，`jstack`查堆栈，`jstat`看GC。

### 2. 如何设计一个线程池参数？
**场景**：一个订单处理系统，峰值500QPS，每个订单处理耗时约50ms（包含DB操作和RPC调用）。

**估算过程**：
```
QPS = 500, RT = 50ms
吞吐量 = 线程数 / RT(秒)   →   线程数 = 500 * 0.05 = 25（理论值，实际留余量）
IO密集型（DB + RPC）建议 2*CPU + 1 或更高
```

**推荐配置**：
```java
int corePoolSize = 20;       // 核心线程数（略低于峰值估算）
int maxPoolSize = 50;        // 最大线程数（应对突发流量）
int keepAliveTime = 60;      // 非核心线程空闲回收
BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(200);  // 有界队列，防止OOM
ThreadFactory factory = new ThreadFactoryBuilder()
    .setNameFormat("order-pool-%d")
    .setDaemon(false)
    .build();
RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
```

**监控加日志**：重写`beforeExecute()`和`afterExecute()`记录执行时间；自定义`rejectedExecution()`记录拒绝次数。

### 3. 如何实现一个生产者-消费者模式？

**使用`BlockingQueue`（推荐）**：
```java
public class ProducerConsumerExample {
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>(10);

    static class Producer implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < 100; i++) {
                try {
                    queue.put("msg-" + i);  // 队列满时阻塞
                    System.out.println("Produced: msg-" + i);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    static class Consumer implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    String msg = queue.take();  // 队列空时阻塞
                    System.out.println("Consumed: " + msg);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
```

**使用`Object.wait/notify`（手写实现、面试考察）**：参见第四章代码题。

### 4. 如何实现一个带超时和重试的RPC调用？
**使用CompletableFuture + ScheduledThreadPoolExecutor**：

```java
public <T> CompletableFuture<T> callWithRetry(String service, T request, int maxRetries) {
    CompletableFuture<T> future = new CompletableFuture<>();
    retryAsync(service, request, maxRetries, future);
    return future;
}

private <T> void retryAsync(String service, T request, int retriesLeft,
                             CompletableFuture<T> future) {
    CompletableFuture.supplyAsync(() -> doRpcCall(service, request))
        .orTimeout(200, TimeUnit.MILLISECONDS)  // 超时机制
        .whenComplete((result, ex) -> {
            if (result != null) {
                future.complete(result);
            } else if (retriesLeft > 0) {
                // 指数退避重试
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                scheduler.schedule(() -> retryAsync(service, request, retriesLeft - 1, future),
                                   (long) Math.pow(2, MAX_RETRIES - retriesLeft) * 50,
                                   TimeUnit.MILLISECONDS);
            } else {
                future.completeExceptionally(ex);
            }
        });
}
```

### 5. 如何排查死锁？

**死锁的4个必要条件**：互斥、请求与保持、不可剥夺、循环等待。

**排查方法**：
1. **`jstack pid`**：找到Found one Java-level deadlock提示，会显示：
   ```
   Found one Java-level deadlock:
   "Thread-1":
     waiting to lock monitor 0x... (object at DeadlockDemo$A)
     which is held by "Thread-0"
   "Thread-0":
     waiting to lock monitor 0x... (object at DeadlockDemo$B)
     which is held by "Thread-1"
   ```
2. **`jconsole`可视化**：连接进程后在线程tab查看死锁检测。
3. **代码层面预防**：
   - 固定锁顺序（所有线程按同一顺序加锁）。
   - 使用`tryLock(timeout)`替代`lock()`，超时自动回退。
   - 使用`ReentrantReadWriteLock`缩小锁范围。

### 6. 如何设计一个本地缓存？
**高并发场景下使用ConcurrentHashMap + 过期机制**：

```java
public class LocalCache<K, V> {
    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newScheduledThreadPool(1);

    public LocalCache(long ttlMs) {
        // 定期清理过期条目
        cleaner.scheduleAtFixedRate(this::evictExpired, ttlMs, ttlMs, TimeUnit.MILLISECONDS);
    }

    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis()));
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) return null;
        if (entry.expired()) {
            cache.remove(key, entry);  // 惰性删除
            return null;
        }
        return entry.value;
    }

    private void evictExpired() {
        cache.entrySet().removeIf(e -> e.getValue().expired());
    }

    static class CacheEntry<V> {
        final V value;
        final long createTime;
        CacheEntry(V value, long createTime) { this.value = value; this.createTime = createTime; }
        boolean expired() { return System.currentTimeMillis() - createTime > TTL; }
    }
}
```

### 7. 如何实现一个简单的限流器？（令牌桶）
```java
public class TokenBucketRateLimiter {
    private final long capacity;          // 桶容量
    private final long refillInterval;    // 填充间隔（毫秒）
    private final long tokensPerRefill;   // 每次填充数量
    private long availableTokens;         // 当前令牌数（volatile？共享变量需synchronized或Lock）
    private long lastRefillTime;

    public TokenBucketRateLimiter(long capacity, long refillInterval, long tokensPerRefill) {
        this.capacity = capacity;
        this.refillInterval = refillInterval;
        this.tokensPerRefill = tokensPerRefill;
        this.availableTokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    public synchronized boolean tryAcquire(int permits) {
        refill();
        if (availableTokens >= permits) {
            availableTokens -= permits;
            return true;
        }
        return false;
    }

    private synchronized void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTime;
        if (elapsed >= refillInterval) {
            long tokensToAdd = (elapsed / refillInterval) * tokensPerRefill;
            availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
            lastRefillTime = now;
        }
    }
}
```

### 8. 如何优雅关闭线程池？
```java
public void shutdownGracefully(ExecutorService pool, long timeout, TimeUnit unit) {
    pool.shutdown();  // 不再接受新任务，等待已提交完成
    try {
        if (!pool.awaitTermination(timeout, unit)) {
            pool.shutdownNow();  // 强制终止，返回未执行任务列表
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Pool did not terminate");
            }
        }
    } catch (InterruptedException e) {
        pool.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

**两步关闭**：先`shutdown()`优雅停止，超时后`shutdownNow()`强制终止。这是Spring和Tomcat等框架的标准做法。

### 9. 线程池中的线程抛出异常会怎样？
**不同提交方式行为不同**：
- **`execute()`**：异常直接被抛出到线程的`uncaughtExceptionHandler`，如果没有自定义handler，会**打印堆栈并退出该线程**。线程池会新建一个线程替换（Worker.run()的while循环能捕获异常则重新addWorker）。
- **`submit()`**：异常**被封装在`Future`**中，调用`future.get()`时才抛出`ExecutionException`。线程本身不退出。

**最佳实践**：
```java
// 方法1：自定义ThreadFactory设置UncaughtExceptionHandler
ThreadFactory factory = r -> {
    Thread t = new Thread(r);
    t.setUncaughtExceptionHandler((thread, ex) -> 
        log.error("Thread {} failed", thread.getName(), ex));
    return t;
};

// 方法2：重写afterExecute()捕获异常
ThreadPoolExecutor pool = new ThreadPoolExecutor(...) {
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (t == null && r instanceof Future<?>) {
            try { ((Future<?>) r).get(); } catch (CancellationException | ExecutionException e) {
                log.error("Task failed", e.getCause());
            }
        } else if (t != null) {
            log.error("Task failed", t);
        }
    }
};
```

### 10. 为什么阿里开发规约禁止使用Executors创建线程池？
```java
// 禁止！队列边界为Integer.MAX_VALUE，OOM风险
ExecutorService fixed = Executors.newFixedThreadPool(10);
ExecutorService cached = Executors.newCachedThreadPool();  // 同上

// 推荐：手动创建
ExecutorService pool = new ThreadPoolExecutor(
    10, 20, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(500),   // 有界队列！
    new ThreadFactoryBuilder().setNameFormat("biz-%d").build(),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

| 工厂方法 | 队列/线程数限制 | 风险 |
|----------|----------------|------|
| `newFixedThreadPool` | `LinkedBlockingQueue`无界 | OOM（任务堆积） |
| `newCachedThreadPool` | `SynchronousQueue`，线程数无上限 | OOM（线程无限创建） |
| `newSingleThreadExecutor` | 无界队列 | OOM |

### 11. 如何实现多线程分批处理大量数据？
**场景**：从DB读取100万条记录，批量处理并写入。

```java
public void batchProcess(List<Record> allRecords, int batchSize) {
    int total = allRecords.size();
    int taskCount = (total + batchSize - 1) / batchSize;
    ExecutorService pool = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(),
        Runtime.getRuntime().availableProcessors() * 2,
        60L, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(taskCount)
    );
    CountDownLatch latch = new CountDownLatch(taskCount);

    for (int i = 0; i < total; i += batchSize) {
        int end = Math.min(i + batchSize, total);
        List<Record> batch = allRecords.subList(i, end);
        pool.submit(() -> {
            try { processBatch(batch); } finally { latch.countDown(); }
        });
    }
    latch.await(30, TimeUnit.MINUTES);  // 等待所有批次完成
    pool.shutdown();
}
```

### 12. 如何实现一个线程安全的计数器？
**三种方式对比**：

```java
// 方式1：AtomicInteger（非阻塞，高性能）
public class AtomicCounter {
    private final AtomicInteger count = new AtomicInteger(0);
    public int increment() { return count.incrementAndGet(); }
    public int get() { return count.get(); }
}

// 方式2：synchronized（阻塞，简单）
public class SyncCounter {
    private int count = 0;
    public synchronized int increment() { return ++count; }
}

// 方式3：LongAdder（高并发统计场景）
public class AdderCounter {
    private final LongAdder count = new LongAdder();
    public void increment() { count.increment(); }
    public int sum() { return count.sum(); }  // 弱一致性
}
```

> 💡 **选择建议**：简单计数选`AtomicInteger`；高并发写远多于读选`LongAdder`；有版本号或比较需求选`AtomicStampedReference`。

---

## 四、手写代码题（8题）

### 1. DCL单例模式（双重检查锁定）

> **考什么**：volatile防止指令重排序、synchronized保证原子性、双重检查的性能优化。

```java
public class Singleton {
    private volatile static Singleton instance;  // volatile禁止重排序

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {              // 第一次检查（不加锁）
            synchronized (Singleton.class) {  // 类锁
                if (instance == null) {       // 第二次检查（加锁）
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

**为什么需要volatile**：`instance = new Singleton()`不是原子操作，分为①分配内存、②初始化对象、③赋值引用。JVM可能重排序为①③②，导致其他线程读到未初始化的对象。

### 2. 生产者-消费者模式（wait/notify）

> **考什么**：wait/notify正确用法、while循环检查条件、虚假唤醒。

```java
public class ProducerConsumer {
    private static final int CAPACITY = 10;
    private final Queue<Integer> queue = new LinkedList<>();
    private final Object lock = new Object();

    class Producer implements Runnable {
        @Override
        public void run() {
            int value = 0;
            while (true) {
                synchronized (lock) {
                    while (queue.size() == CAPACITY) {  // 必须用while！
                        try { lock.wait(); } catch (InterruptedException e) { return; }
                    }
                    queue.offer(value++);
                    lock.notifyAll();  // 通知消费者
                }
                Thread.yield();  // 模拟间歇生产
            }
        }
    }

    class Consumer implements Runnable {
        @Override
        public void run() {
            while (true) {
                synchronized (lock) {
                    while (queue.isEmpty()) {
                        try { lock.wait(); } catch (InterruptedException e) { return; }
                    }
                    queue.poll();
                    lock.notifyAll();  // 通知生产者
                }
                Thread.yield();
            }
        }
    }
}
```

### 3. 自己实现BlockingQueue

> **考什么**：ReentrantLock + Condition的精确通知、await/signal的使用。

```java
public class MyBlockingQueue<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public MyBlockingQueue(int capacity) { this.capacity = capacity; }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();  // 队列满，阻塞生产者
            }
            queue.offer(item);
            notEmpty.signalAll();  // 唤醒消费者
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();  // 队列空，阻塞消费者
            }
            T item = queue.poll();
            notFull.signalAll();  // 唤醒生产者
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

### 4. 死锁代码

> **考什么**：死锁的4个必要条件、如何构造死锁、如何排查。

```java
public class DeadlockDemo {
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("Thread 1: locked A");
                sleep(100);  // 确保t2拿到B
                synchronized (lockB) {
                    System.out.println("Thread 1: locked B");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("Thread 2: locked B");
                sleep(100);
                synchronized (lockA) {
                    System.out.println("Thread 2: locked A");
                }
            }
        });

        t1.start();
        t2.start();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

**修复方式**：固定锁顺序——两个线程都先锁A再锁B。

### 5. 线程安全的计数器（可进阶版）

> **考什么**：原子类 + 分段思想。

```java
public class StripedCounter {
    private final AtomicLong[] counters;
    private final int stripes;

    public StripedCounter(int stripes) {
        this.stripes = stripes;
        this.counters = new AtomicLong[stripes];
        for (int i = 0; i < stripes; i++) {
            counters[i] = new AtomicLong(0);
        }
    }

    public void increment() {
        int stripe = (int) (Thread.currentThread().getId() % stripes);
        counters[stripe].incrementAndGet();
    }

    public long sum() {
        long total = 0;
        for (AtomicLong c : counters) total += c.get();
        return total;
    }
}
```

### 6. 使用CompletableFuture实现异步任务编排

```java
public class AsyncTaskDemo {
    // 场景：先查用户信息，再查订单列表，最后合并结果
    public CompletableFuture<UserDetail> getUserDetail(Long userId) {
        return CompletableFuture.supplyAsync(() -> queryUser(userId))
            .thenCompose(user ->
                CompletableFuture.supplyAsync(() -> queryOrders(userId))
                    .thenApply(orders -> new UserDetail(user, orders))
            )
            .exceptionally(ex -> {
                log.error("Failed to get user detail", ex);
                return UserDetail.empty();
            });
    }

    // 场景：并行执行3个任务，全部完成后再处理
    public CompletableFuture<AggregatedResult> parallelAggregate() {
        CompletableFuture<A> f1 = CompletableFuture.supplyAsync(this::fetchA);
        CompletableFuture<B> f2 = CompletableFuture.supplyAsync(this::fetchB);
        CompletableFuture<C> f3 = CompletableFuture.supplyAsync(this::fetchC);

        return CompletableFuture.allOf(f1, f2, f3)
            .thenApply(v -> new AggregatedResult(f1.join(), f2.join(), f3.join()));
    }

    // 场景：多个服务任意一个返回即可（最佳结果）
    public CompletableFuture<String> fastestResponse() {
        CompletableFuture<String> service1 = CompletableFuture.supplyAsync(this::callService1);
        CompletableFuture<String> service2 = CompletableFuture.supplyAsync(this::callService2);
        return service1.applyToEither(service2, fastest -> fastest);
    }
}
```

### 7. 自定义AQS同步器（一次性门栓）

```java
public class OneShotLatch {
    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected int tryAcquireShared(int ignored) {
            // 返回1表示获取成功（门已打开），-1表示失败
            return getState() == 1 ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(int ignored) {
            setState(1);
            return true;  // 释放成功，唤醒等待队列中的线程
        }
    }

    private final Sync sync = new Sync();

    public void await() {
        sync.acquireShared(0);  // 阻塞直到state=1
    }

    public void signal() {
        sync.releaseShared(0);  // 设置state=1并唤醒所有等待线程
    }
}
```

### 8. 两阶段终止模式（Two-Phase Termination）

> **适用场景**：优雅终止线程，保证资源正确释放。

```java
public class TwoPhaseTermination {
    private Thread worker;

    public void start() {
        worker = new Thread(() -> {
            boolean interrupted = false;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // 执行任务...
                        doWork();
                    } catch (InterruptedException e) {
                        // 清理资源...
                        interrupted = true;  // 记录中断状态
                        Thread.currentThread().interrupt();  // 恢复中断标志
                        break;
                    }
                }
            } finally {
                cleanup();  // 最终资源清理
                if (interrupted) {
                    // 恢复中断标志给调用者
                    Thread.currentThread().interrupt();
                }
            }
        });
        worker.start();
    }

    public void stop() {
        if (worker != null) {
            worker.interrupt();  // 第一阶段：发送中断信号
        }
    }

    private void doWork() { /* 可能抛出InterruptedException */ }
    private void cleanup() { /* 释放资源 */ }
}
```

---

## 五、系统设计题（5题）

### 1. 如何设计一个高并发秒杀系统？

**核心挑战**：瞬间高并发 + 库存准确 + 防超卖 + 反作弊。

**分层架构设计**：

```
客户端 → CDN/静态资源 → Nginx/LVS → Gateway → 业务服务 → Redis → MySQL
                                                          ↓ 分层过滤
                                                    MQ削峰 → 异步扣库存
```

**关键设计点**：

| 层级 | 措施 | 说明 |
|------|------|------|
| **接入层** | 限流 | Nginx限流（令牌桶）+ 网关层限流（Sentinel/Semaphore） |
| **前置** | 按钮控制 | 前端限制点击频率（1s内不可重复提交）+ 验证码 |
| **业务层** | 分层过滤 | 时间校验 → 风控校验 → 资格校验 → 排队 |
| **缓存层** | Redis预扣库存 | **Lua脚本**保证原子性：`if stock>0 then stock-- return 1 else return 0` |
| **异步** | MQ削峰 | 抢购成功写入MQ，DB异步落单，避免DB被打满 |
| **一致性** | 最终一致 | Redis预扣 → MQ消费 → DB扣减 → 对账补偿 |

**代码实现（Redis Lua脚本原子扣库存）**：
```lua
-- lua脚本：库存充足则扣减并返回1，否则返回0
local stock = redis.call('GET', KEYS[1])
if stock and tonumber(stock) > 0 then
    redis.call('DECR', KEYS[1])
    return 1
end
return 0
```

### 2. 如何设计一个多级缓存系统？

**场景**：一个读写比约100:1的商品详情页系统，要求响应时间<20ms。

**缓存层级**：

| 层级 | 存储 | 容量 | 延迟 | 命中率策略 |
|------|------|------|------|-----------|
| **L1** | Caffeine本地缓存 | 几百MB | 纳秒级 | 定时刷新 + LRU淘汰 |
| **L2** | Redis分布式缓存 | 若干GB | 毫秒级 | 过期时间 + 主动更新 |
| **L3** | MySQL/DB | 全量 | 几十毫秒 | 兜底 |

**热点Key检测与处理**：
```java
// 滑动窗口计数：检测热点Key
public class HotKeyDetector {
    private final ConcurrentHashMap<String, AtomicLong> countMap = new ConcurrentHashMap<>();
    private static final int HOT_THRESHOLD = 100;  // 每秒超过100次认为是热点

    public void recordAccess(String key) {
        countMap.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    public Set<String> detectHotKeys() {
        return countMap.entrySet().stream()
            .filter(e -> e.getValue().getAndSet(0) > HOT_THRESHOLD)  // 重置计数
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }
}
```

**缓存一致性方案**：Cache-Aside模式 + **canal订阅binlog**变更推送双删。

### 3. 如何设计一个分布式ID生成器？

**要求**：全局唯一、趋势递增、高可用、高性能（单机10万+ TPS）。

**常见方案对比**：

| 方案 | 优点 | 缺点 | 适用 |
|------|------|------|------|
| **UUID** | 简单、去中心化 | 无序、太长（36字符）、索引性能差 | 不推荐 |
| **DB自增** | 有序、递增 | 性能瓶颈、单点故障 | 小规模 |
| **Redis INCR** | 高性能 | 持久化可能产生重复ID | 大并发 |
| **雪花算法** | 高性能、趋势递增、去中心化 | 时钟回拨问题 | **推荐** |
| **号段模式** | 批量获取、DB压力小 | 依赖DB | 美团Leaf方案 |

**雪花算法（Snowflake）**：
```
0 | 41-bit timestamp | 10-bit workerId | 12-bit sequence
```
- **1位符号位**：固定0。
- **41位时间戳**：69年容量（毫秒级），自定义起始时间减少位数浪费。
- **10位机器ID**：可支持1024台节点。
- **12位序列号**：同毫秒内可生成4096个ID。

**时钟回拨解决方案**：
```java
public synchronized long nextId() {
    long currentTs = timeGen();
    if (currentTs < lastTimestamp) {  // 时钟回拨
        long offset = lastTimestamp - currentTs;
        if (offset <= MAX_BACKWARD_MS) {
            // 等待时间追上
            Thread.sleep(offset);
            currentTs = timeGen();
        } else {
            // 严重回拨，报错或切换workerId
            throw new RuntimeException("Clock moved backwards too much");
        }
    }
    // ... 生成ID逻辑
}
```

### 4. 如何设计一个高并发消息队列？

**核心需求**：生产者高吞吐、消息不丢、消费顺序保证。

**架构设计**：

```
Producer → Broker(CommitLog + ConsumeQueue) → Consumer
               ↓
            PageCache
```

**关键设计**：

| 问题 | 方案 |
|------|------|
| **高吞吐** | 顺序写盘（Append Only）、mmap零拷贝、批量刷盘 |
| **消息不丢** | 刷盘 + 主从同步 + ACK确认机制 |
| **顺序消费** | 同一个队列/分区内有序，多分区只能保证分区内有序 |
| **消息去重** | 业务端幂等（唯一ID + 状态机） |
| **消息堆积** | 快速消费 + 扩容Topic分区 + 临时消费者 |
| **消息回溯** | CommitLog按时间索引，支持重新消费 |

**伪代码（内存队列实现）**：
```java
public class SimpleMessageQueue<T> {
    private final LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>();
    private final List<Consumer<T>> consumers = new CopyOnWriteArrayList<>();

    public void produce(T msg) throws InterruptedException {
        queue.put(msg);  // 生产者阻塞
    }

    public void registerConsumer(Consumer<T> consumer) {
        consumers.add(consumer);
        new Thread(() -> {
            while (true) {
                try {
                    consumer.consume(queue.take());  // 消费者阻塞消费
                } catch (InterruptedException e) { break; }
            }
        }).start();
    }
}
```

### 5. 如何设计一个高并发定时任务调度系统？

**场景**：支持百万级定时任务，秒级精度，高可用。

**核心思路**：时间轮（TimeWheel）算法 + 分布式协调。

**时间轮原理**：
```
tickMs=100ms, wheelSize=20 (覆盖2s周期)
             → 第0槽：过期任务
             其他槽：按到期时间放入

层级时间轮：级联升级，覆盖更大时间范围
```

**架构方案**：

| 组件 | 职责 | 技术选型 |
|------|------|----------|
| **API层** | 任务注册、取消 | Spring Boot |
| **存储层** | 任务持久化 | MySQL/Redis |
| **调度层** | 时间轮触发 | Netty HashedWheelTimer |
| **执行层** | 任务执行 + 重试 | 线程池 |
| **协调层** | 分布式选主 + 分片 | Zookeeper/Etcd |

**Java实现（时间轮核心）**：
```java
public class HashedWheelTimer {
    private final WheelBucket[] wheel;  // 时间轮数组
    private final AtomicLong tick = new AtomicLong();

    public void schedule(TimerTask task, long delayMs) {
        long ticks = delayMs / tickDuration;
        long targetTick = tick.get() + ticks;
        int slot = (int) (targetTick % wheelSize);
        wheel[slot].addTask(new TimerTaskWrapper(task, targetTick));
    }

    // 后台线程每秒驱动tick前进，触发slot中所有到期任务
}
```

> 🎯 **海量任务优化**：使用分层时间轮（类似Kafka），高层时间轮一个槽代表低层时间轮一圈，节约内存。

---

## 六、常见坑点与最佳实践

| 坑点 | 原因 | 解决方案 |
|------|------|----------|
| `ArrayList`/`HashMap`并发修改抛`ConcurrentModificationException` | 迭代器fast-fail机制在并发修改时触发 | 使用`CopyOnWriteArrayList`、`ConcurrentHashMap`，或在迭代时加锁 |
| `SimpleDateFormat`线程不安全 | 内部Calendar共享状态，多线程解析异常 | 使用`DateTimeFormatter`（JDK8，不可变且线程安全） |
| `synchronized`包装不是String/Integer | 字符串常量池导致不同地方引用同一对象；Integer缓存`[-128,127]` | 使用`new Object()`或类锁 |
| 线程池中`submit(Runnable)`异常静默消失 | 异常被封装到`Future`中 | 对`Future.get()`调用检查；重写`afterExecute()` |
| `ConcurrentHashMap.size()`精确性问题 | 在高并发下返回的是近似值 | 统计场景使用`mappingCount()`替代（返回`long`） |
| 使用`Thread.stop()`终止线程 | 强制终止导致资源未释放、对象状态不一致 | 使用中断机制（`interrupt()` + 检查标志位） |
| 线程池队列无界导致OOM | `newFixedThreadPool`默认无界队列 | 使用有界队列（`ArrayBlockingQueue`） |
| `synchronized`锁方法时锁对象不当 | 非静态方法锁this，静态方法锁Class对象 | 明确锁对象意图，避免方法间意外互斥 |
| `CompletableFuture`不指定线程池 | 默认使用`ForkJoinPool.commonPool()`，业务任务可能阻塞公共线程 | 所有业务异步**必须**传入自定义线程池 |
| `ThreadLocal`未清理导致内存泄漏 | 线程池场景线程复用，value无法GC | `finally`块中调用`remove()` |
| 读写锁降级陷阱 | 写锁降级读锁后直接释放写锁，其他读线程可能读到旧数据 | 写锁降级读锁后应保持读锁完成操作再释放 |
| 虚假唤醒（Spurious Wakeup） | 线程在没有收到`notify`/`notifyAll`时被唤醒 | 条件等待**必须**用`while`循环 |
| `ArrayList.subList()`返回的是视图 | 视图依赖原列表结构，原列表修改导致视图失效抛出`ConcurrentModificationException` | 创建新列表包装：`new ArrayList<>(list.subList(...))` |
| 线程数设置过大导致性能反而下降 | 上下文切换开销超过任务执行收益 | CPU密集型：`CPU核数+1`；IO密集型：`2*CPU核数`（含等待系数） |
| `LockSupport.park()`提前被调用导致线程一直阻塞 | park/unpark顺序问题 | 设置标志位检查；保证unpark在park之前不会丢失效果（许可证机制） |

---

## 七、面试回答模板（Top 5）

### 1. 问：请说说synchronized和ReentrantLock的区别？

> **一句话总结**：synchronized是JVM层面基于Monitor的关键字锁，ReentrantLock是JDK层面基于AQS的API锁。

**展开说明**：
- synchronized是隐式的，自动释放；ReentrantLock需手动在finally中unlock。
- JDK6后synchronized引入了偏向锁、轻量级锁等优化，性能与ReentrantLock差距不大。
- ReentrantLock提供了更丰富的功能：**可中断**（`lockInterruptibly`）、**可超时**（`tryLock`）、**可公平**、**多Condition**（精确唤醒）。

**举例论证**：在需要精确唤醒的场景（如生产者-消费者），ReentrantLock的Condition比synchronized的wait/notify更灵活，可以指定唤醒生产者或消费者。在流量控制场景，`tryLock`超时获取比synchronized阻塞更安全。

**引导追问**："如果您感兴趣，我可以详细介绍AQS是如何支撑ReentrantLock的这些特性的，包括排队、自旋和park的协作机制。"

### 2. 问：请描述一下ConcurrentHashMap的实现原理？

> **一句话总结**：JDK8的ConcurrentHashMap采用**Node数组 + 链表/红黑树**的数据结构，使用**CAS + synchronized**保证线程安全。

**展开说明**：
- 数组为空时通过CAS初始化，避免锁竞争。
- 桶位为空时通过CAS无锁插入，性能极高。
- 桶位有数据时锁住链表头节点（synchronized），并发度远高于JDK7的Segment分段锁。
- 链表长度≥8时转红黑树，优化查询性能。
- 扩容时支持多线程协作迁移，每个线程负责一段桶。

**举例论证**：线上百万QPS的缓存查询场景，ConcurrentHashMap能保持稳定的低延迟，而HashTable全表锁会严重阻塞。在热点Key场景下，JDK8的锁粒度已经是单节点，几乎不会产生锁争用。

**引导追问**："我可以继续介绍ConcurrentHashMap在扩容过程中如何保证数据一致性，以及ForwardingNode和helpTransfer的设计。另外，红黑树在扩容时如何退化为链表的细节也值得深入。"

### 3. 问：如何合理设置线程池的参数？

> **一句话总结**：线程池参数没有通用公式，需根据任务性质（CPU/IO密集型）、QPS和RT、队列类型综合确定。

**展开说明**：
- **CPU密集型**：`corePoolSize = CPU核数 + 1`（+1是为了补偿页缺失暂停）。
- **IO密集型**：`corePoolSize = 2 * CPU核数`，或通过公式 `线程数 = CPU核数 * (1 + 等待时间/计算时间)` 估算。
- **混合型**：区分IO和计算，考虑拆分或使用不同线程池隔离。
- **队列**：必须有界，大小根据积压容忍度和内存容量决定。
- **拒绝策略**：核心业务使用`CallerRunsPolicy`反压；可丢弃任务使用`DiscardPolicy`+日志告警。

**举例论证**：有一个订单处理服务，QPS 500，RT 50ms。根据`线程数 = QPS * RT` = 25，设置corePoolSize=20，maxPoolSize=50，有界队列200，`CallerRunsPolicy`。压测发现当积压超过200时执行老线程直接处理，降低提交速度，保护了下游数据库和RPC服务。

**引导追问**："如果需要我可以进一步介绍动态线程池的实践——使用配置中心动态调整corePoolSize和maxPoolSize，结合监控指标实现自适应调整。"

### 4. 问：volatile能保证原子性吗？为什么？

> **一句话总结**：volatile**不能保证原子性**，它只保证可见性和有序性。

**展开说明**：
- volatile的语义是：写操作立即刷新到主存，读操作从主存加载（可见性）；插入内存屏障禁止重排序（有序性）。
- 但是`count++`这种**读-改-写**操作，本质上是三步：读取 → 加1 → 写入。volatile无法保证这三步的原子性——线程A读到count=5，线程B也读到count=5，各自加1后写回，结果都是6而不是7。

**举例论证**：用volatile修饰的int变量做计数器，1000个线程每个自增10000次，最终结果几乎一定小于10000000。换成`AtomicInteger`（内部通过CAS保证原子性）或`synchronized`，结果才正确。

**引导追问**："看完volatile的局限，可以继续聊`AtomicInteger`的CAS原理和ABA问题的解决方案，以及`LongAdder`在原子类基础上做的分段优化。"

### 5. 问：ThreadLocal内存泄漏是怎么回事？

> **一句话总结**：ThreadLocalMap的Entry中key是弱引用，value是强引用，key被GC后value无法被回收导致内存泄漏。

**展开说明**：
- 每个Thread内部维护一个`ThreadLocalMap`，以当前`ThreadLocal`实例为key存储数据。
- Entry的key是`WeakReference<ThreadLocal>`，当外部强引用消失时，key在下一次GC时被回收，变为null。
- 但value是强引用，链路上：`当前线程 → ThreadLocalMap → Entry → value`，只要线程存活（尤其线程池场景），value就一直可达，无法回收。

**举例论证**：Tomcat线程池复用线程处理请求，如果在一个Web请求中使用了`ThreadLocal`存储用户上下文但没有`remove()`，下个请求可能读到上个请求的脏数据，而且用户对象无法回收，持续OOM。

**引导追问**："阿里规约强制要求ThreadLocal使用后调用remove()。实际上ThreadLocalMap的get和set方法内部会尝试清理key为null的Entry，但最好别依赖这个机制，显式remove()最安全。"

---

## 八、快速查漏补缺Checklist

### 基础必知
- [ ] 线程的6种状态及转换图
- [ ] `start()`与`run()`区别
- [ ] `sleep()`与`wait()`区别（锁行为）
- [ ] 创建线程的3种方式 + 线程池提交
- [ ] `interrupt()`的正确使用（不是立即停止）

### synchronized体系
- [ ] 锁升级过程：无锁 → 偏向锁 → 轻量级锁 → 重量级锁
- [ ] Monitor机制（对象头Mark Word、Owner、EntrySet、WaitSet）
- [ ] 可重入原理（计数器累加）
- [ ] 使用位置区别：实例方法（this）、静态方法（Class）、代码块（指定对象）
- [ ] 锁消除、锁粗化优化

### JMM与volatile
- [ ] JMM三大特性：**原子性、可见性、有序性**
- [ ] volatile：保证可见性 + 有序性，不保证原子性
- [ ] 8条happens-before规则（尤其程序次序、管程锁定、volatile、传递性）
- [ ] 内存屏障：LoadLoad、StoreStore、LoadStore、StoreLoad
- [ ] DCL单例为什么需要volatile

### CAS与原子类
- [ ] CAS原理（Unsafe.compareAndSwapInt、cmpxchg指令）
- [ ] ABA问题（AtomicStampedReference、AtomicMarkableReference）
- [ ] 原子类：AtomicInteger / AtomicReference / AtomicIntegerArray / AtomicFieldUpdater
- [ ] LongAdder：CAS分段 + Cell伪共享处理（@Contended）
- [ ] Unsafe类（CAS相关、allocateInstance）

### AQS与锁
- [ ] AQS核心：state + CLH队列 + CAS + LockSupport.park/unpark
- [ ] ReentrantLock：lock/unlock源码流程、可重入、公平/非公平
- [ ] Condition的await/signal vs Object的wait/notify
- [ ] ReentrantReadWriteLock：读写分离、锁降级
- [ ] StampedLock：乐观读模式、不可重入

### 线程池
- [ ] 7个参数含义 + 执行流程
- [ ] 5种状态 + 状态流转
- [ ] 拒绝策略：AbortPolicy（默认）、CallerRunsPolicy（反压）、DiscardPolicy、DiscardOldestPolicy
- [ ] Executors工厂方法的坑（无界队列）
- [ ] ThreadPoolExecutor异常处理（execute vs submit）
- [ ] ScheduledThreadPoolExecutor（DelayedWorkQueue）
- [ ] ForkJoinPool + 工作窃取（Work-Stealing）

### 并发容器
- [ ] ConcurrentHashMap JDK7（Segment） vs JDK8（Node + CAS + synchronized）
- [ ] ConcurrentHashMap：扩容transfer、ForwardingNode、红黑树退化
- [ ] CopyOnWriteArrayList：写时复制、读写分离、弱一致性
- [ ] BlockingQueue：LinkedBlockingQueue / ArrayBlockingQueue / SynchronousQueue / ConcurrentLinkedQueue

### 同步工具
- [ ] CountDownLatch vs CyclicBarrier（可重用、触发动作）
- [ ] Semaphore（信号量、限流）
- [ ] ThreadLocal原理 + 内存泄漏 + remove()
- [ ] CompletableFuture：thenApply/thenCompose/thenCombine/allOf/applyToEither/exceptionally
- [ ] FutureTask的原理

### 设计模式
- [ ] Two-Phase Termination（两阶段终止）
- [ ] Guarded Suspension（保护性暂停）
- [ ] Producer-Consumer（生产者-消费者）
- [ ] Balking（犹豫模式）

### 排查与调优
- [ ] CPU飙高排查（top -H + jstack）
- [ ] 死锁排查（jstack / jconsole）
- [ ] 线程堆栈分析
- [ ] 内存泄漏排查（jmap + MAT）
- [ ] 上下文切换开销理解
- [ ] 线程池参数调优（CPU密集型 vs IO密集型）
- [ ] 阿里Java开发手册并发规约

> 🎯 **面试加分项**：JDK 21虚拟线程（Virtual Threads）——轻量级线程，适用于IO密集型场景，可大幅减少线程池复杂度和上下文切换开销。面试中提到"虚拟线程会改变高并发编程范式"是高级话题。

---

> **参考**：本宝典基于104节Java并发编程课程整理，覆盖阿里、腾讯、字节、美团等大厂面试高频考点。建议配合实战代码练习和LeetCode多线程题目（如`1114.按序打印`、`1115.交替打印FooBar`、`1116.打印零与奇偶数`）强化理解。
