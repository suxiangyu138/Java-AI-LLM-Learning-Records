# Java 多线程与并发核心知识点

## 一、基础概念

1. **进程 vs 线程**
   - 进程：操作系统资源分配的最小单位（独立内存空间）
   - 线程：CPU 调度的最小单位（共享进程内存，私有栈、程序计数器）
2. **并发 vs 并行**
   - 并发：单核 CPU 交替执行多个任务（看起来同时）
   - 并行：多核 CPU 真正同时执行多个任务
3. **线程生命周期**
   `NEW` → `RUNNABLE` → `BLOCKED`/`WAITING`/`TIMED_WAITING` → `TERMINATED`
4. **线程安全**
   多个线程同时访问共享资源，**不加锁也不会出现数据错误**。

## 二、线程创建方式（4种）

1. **继承 Thread 类**
2. **实现 Runnable 接口**（无返回值）
3. **实现 Callable 接口**（有返回值，可抛异常）
4. **线程池 `ExecutorService`**（企业开发首选）

## 三、线程常用方法

- `start()`：启动线程（真正进入就绪态）
- `run()`：只是普通方法调用，不启动线程
- `sleep()`：休眠，**不释放锁**
- `yield()`：让出 CPU，回到就绪态
- `join()`：等待该线程执行完毕再继续
- `wait()` / `notify()` / `notifyAll()`：必须在**同步代码块**中，**释放锁**

## 四、Synchronized 关键字（重量级锁）

### 1. 作用

保证**原子性、可见性、有序性**，解决线程安全问题。

### 2. 使用位置

- 修饰实例方法：锁当前对象
- 修饰静态方法：锁当前类 Class 对象
- 修饰代码块：锁指定对象（推荐）

### 3. 锁升级过程（JDK1.6 优化）

`无锁 → 偏向锁 → 轻量级锁 → 重量级锁`

## 五、volatile 关键字（轻量级）

1. **保证可见性**：一个线程修改，其他线程立刻看到
2. **禁止指令重排**（DCL 单例必须用）
3. **不保证原子性**：不能替代锁

## 六、Lock 接口（JUC 锁）

比 `synchronized` 更灵活：
- `lock()`：获取锁
- `unlock()`：释放锁（必须 finally 里）
- `tryLock()`：尝试获取锁，不阻塞
- `lockInterruptibly()`：可中断

常用实现：
- `ReentrantLock`：可重入锁
- `ReentrantReadWriteLock`：读写锁（读共享，写互斥）

## 七、线程间通信

1. `wait()` + `notify()`
2. `Condition`：`await()` + `signal()`
3. 管道流
4. 共享变量 + volatile

## 八、JUC 核心工具类（java.util.concurrent）

### 1. 线程池（最重要）

**为什么用线程池？**
- 避免频繁创建销毁线程
- 控制并发数量
- 统一管理

**七大参数：**
1. `corePoolSize`：核心线程数
2. `maximumPoolSize`：最大线程数
3. `keepAliveTime`：空闲线程存活时间
4. `unit`：时间单位
5. `workQueue`：阻塞队列
6. `threadFactory`：线程工厂
7. `handler`：拒绝策略

**四大拒绝策略：**
- AbortPolicy：抛异常（默认）
- CallerRunsPolicy：让调用者执行
- DiscardOldestPolicy：丢弃最老任务
- DiscardPolicy：直接丢弃

**常用线程池：**
- `Executors.newFixedThreadPool()`：固定线程
- `Executors.newSingleThreadExecutor()`：单线程
- `Executors.newCachedThreadPool()`：缓存线程
- **开发禁用 Executors，必须手动创建线程池！**

### 2. 阻塞队列

- `ArrayBlockingQueue`
- `LinkedBlockingQueue`
- `SynchronousQueue`

### 3. 同步工具类

- `CountDownLatch`：等待多个线程完成
- `CyclicBarrier`：线程互相等待
- `Semaphore`：控制并发数量

### 4. 原子类（CAS 实现）

`AtomicInteger`、`AtomicBoolean`、`AtomicLong`
无锁高效，保证原子性。

## 九、CAS 机制（Compare And Swap）

### 1. 原理

内存值 V、预期值 A、要修改的值 B
- 若 V == A → 改为 B
- 否则不操作，重试

### 2. 优点

无锁，高并发下性能好

### 3. 缺点

- 循环消耗 CPU
- **ABA 问题**（加版本号解决：`AtomicStampedReference`）

## 十、ThreadLocal

1. 线程本地变量，**每个线程独立副本**
2. 不共享，无线程安全问题
3. 必须手动 `remove()`，否则**内存泄漏**

## 十一、死锁

### 1. 四个必要条件

1. 互斥
2. 请求与保持
3. 不可剥夺
4. 循环等待

### 2. 解决办法

- 破坏任意一个条件
- 统一**锁的顺序**
- 设置超时时间

## 十二、并发三大特性

1. **原子性**：不可分割（synchronized / Lock / CAS）
2. **可见性**：修改立刻被其他线程看到（volatile / synchronized）
3. **有序性**：禁止指令重排（volatile / synchronized）

## 十三、AQS 原理（AbstractQueuedSynchronizer）

JUC 锁的**底层基石**：
- 用一个 `volatile int state` 表示同步状态
- 双向链表存储等待线程
- 基于 CAS 修改状态

实现：ReentrantLock、CountDownLatch、Semaphore 等。

## 十四、高频面试题总结

1. 线程创建方式？
2. sleep 和 wait 区别？
3. synchronized 和 Lock 区别？
4. volatile 作用？能否保证原子性？
5. 线程池参数、拒绝策略？
6. CAS 原理、ABA 问题？
7. 死锁条件与解决？
8. ThreadLocal 原理与内存泄漏？
9. AQS 理解？
10. 线程安全的单例模式？

---

### 总结

- **基础**：线程状态、创建方法、通信
- **锁**：synchronized、volatile、Lock、CAS
- **JUC**：线程池、阻塞队列、同步工具、原子类
- **原理**：AQS、ThreadLocal、死锁
