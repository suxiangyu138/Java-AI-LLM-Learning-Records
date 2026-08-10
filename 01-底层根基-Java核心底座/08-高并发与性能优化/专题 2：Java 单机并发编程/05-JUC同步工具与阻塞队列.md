# 05 JUC 同步工具与阻塞队列

> 线程协作的积木箱——CountDownLatch/CyclicBarrier/Semaphore/Phaser 的选型、Condition 与阻塞队列七兄弟、生产者-消费者范式

---

## 📚 目录

1. [四大协作工具：场景与选型](#1-四大协作工具场景与选型)
2. [Condition：精准唤醒](#2-condition精准唤醒)
3. [阻塞队列七兄弟](#3-阻塞队列七兄弟)
4. [生产者-消费者范式](#4-生产者-消费者范式)
5. [协作工具使用红线](#5-协作工具使用红线)

---

## 1. 四大协作工具：场景与选型

四个工具都建立在 AQS 之上（见 [03 篇](03-显式锁与AQS源码.md)），差异只在"协作语义"：

- **CountDownLatch（倒数门闩）**：N 个任务完成前，主线程 await 阻塞；每完成一个 countDown 一次，减到 0 放行。**一次性**，用完即弃。典型场景：并行加载多个配置/数据后统一继续、并发测试起点闸门。
- **CyclicBarrier（循环栅栏）**：N 个线程互相等待，全部到齐才同时放行；**可重用**（reset 后进入下一轮）。典型场景：多阶段并行计算（每阶段对齐）、分片数据处理。构造时还可传 barrierAction，到达时执行一次合并动作。
- **Semaphore（信号量）**：维护 N 个许可，acquire 拿走、release 归还，控制**并发进入数**而非"等齐"。典型场景：限流（同时最多 10 个任务访问数据库）、资源池保护。可公平/非公平，可中断。
- **Phaser（相位器）**：CountDownLatch + CyclicBarrier 的合体，支持**动态注册参与方**（register/bulkRegister），阶段数可动态增减。典型场景：分阶段、参与者数量动态变化的并行任务（并发分页处理）。动态性让它能优雅处理"每阶段参与者不同"的问题：

```java
Phaser phaser = new Phaser(1);          // 注册主线程
for (int i = 0; i < N; i++) {
    phaser.register();                  // 每加一个任务注册一个参与方
    executor.submit(() -> { phaser.arriveAndAwaitAdvance(); /* 本阶段工作 */ });
}
phaser.arriveAndDeregister();           // 主线程退出等待
```

选型口诀：**等齐放行 → CountDownLatch（一次）/ CyclicBarrier（多轮）；限并发数 → Semaphore；动态多阶段 → Phaser**。用错的最典型事故是拿 CountDownLatch 当可复用栅栏——第二轮直接穿透。

## 2. Condition：精准唤醒

wait/notify 的广播唤醒有两个痛点：**无法定向唤醒某一类等待者**（notifyAll 把所有 wait 线程都叫起来竞争），**一个锁只有一条等待队列**（条件混杂）。Condition 解决这两点：每个锁可以 new 多个 Condition，每条件一条独立队列，`await()`/`signal()` 精确操作。

```java
ReentrantLock lock = new ReentrantLock();
Condition notFull = lock.newCondition();   // 缓冲区未满
Condition notEmpty = lock.newCondition();  // 缓冲区非空

// 生产者
lock.lock();
try {
    while (queue.isFull()) notFull.await();   // 满则等"未满"信号
    queue.put(item);
    notEmpty.signal();                        // 只唤醒消费者
} finally { lock.unlock(); }
```

关键纪律与 wait/notify 相同：**条件判断必须用 while 循环**（虚假唤醒 + 信号丢失），await 前必须持有锁，await 会释放锁。Condition 的语义比 wait/notify 精确一个数量级，新代码一律用 Condition 替代。

## 3. 阻塞队列七兄弟

阻塞队列 = 线程安全队列 + 阻塞语义（空队列 take 阻塞、满队列 put 阻塞），是生产者-消费者与线程池任务队列的载体。七兄弟按结构与语义分四类：

- **有界数组**：`ArrayBlockingQueue`——固定容量，公平性可选。线程池定界队列首选。
- **无界链表**：`LinkedBlockingQueue`——默认容量 Integer.MAX_VALUE，可指定。**容量不设上限时内存风险巨大**，线程池默认用它就是生产事故源头之一。
- **直传**：`SynchronousQueue`——不存元素，put 必须等 take 配对，相当于"握手交接"。`Executors.newCachedThreadPool` 用它，无空闲线程时每任务新建线程。
- **延迟/优先**：`DelayQueue`（元素到期才可取出，定时任务调度）、`PriorityBlockingQueue`（按优先级出队，无界）。
- **传输**：`TransferQueue`（LinkedTransferQueue）——put 时优先找等待的消费者直接交接，找不到才入队；吞吐比 LinkedBlockingQueue 高，实现即"无锁 + 双重队列"。

选型要看**生产端会不会积压**：积压可控且有界 → ArrayBlockingQueue；必须无界 → 明确接受内存上限；性能极致 → LinkedTransferQueue。延迟队列做"本地定时任务去重合并"非常好用。阻塞队列天然是**背压（backpressure）的载体**：有界 + put 阻塞 = 生产端被迫放慢，消费不掉的压力显式呈现为"队列满"，而不是悄悄堆爆内存——这比无界队列的"限流无效 + 延迟放大"健康得多，也是"有界队列是生产底线"这句话的完整理由。

## 4. 生产者-消费者范式

生产者-消费者是并发协作的母题：解耦生产与消费速率、缓冲峰值。经典实现三选一：

1. **阻塞队列直连**（最常用）：生产者 put、消费者 take，队列自身解决互斥与阻塞——**零手写同步代码**，首选。
2. **Condition 显式控制**：需要多条件（满/非满）或精细策略时用 [02 节](#2-condition精准唤醒) 的姿势。
3. **无锁队列**：单生产者单消费者场景用 `ConcurrentLinkedQueue` 或 Disruptor 的环形缓冲（更高端场景），一般业务用不到。

生产者的多线程注意：多生产者时阻塞队列的 put 本身线程安全，但"检查-放入"复合逻辑（如先判断是否重复）需自己加锁；消费者的优雅停机：设置 poison pill（毒丸：特殊标记对象，消费者收到即退出）或中断 + 超时 take。

消费者侧还有一个高频坑：**消费失败的任务直接丢弃等于丢数据**。生产姿势是"失败重入队列（记次数）→ 超限进死信"——延迟队列天然适合做重试（DelayQueue 放"下次重试时间到期的任务"），比立刻重入更平滑。另外多消费者会共享队列，任务分配由队列仲裁，消费者的处理速度差异天然被均摊——但"任务有顺序依赖"（如同 key 必须顺序处理）就超出了队列语义，需要按 key 分片到独立队列，这是从"队列"到"分区"的思维跃迁。

## 5. 协作工具使用红线

1. **CountDownLatch 计数必须与任务数精确一致**：多减一次 → 直接放行；少减一次 → 永久阻塞。兜底用 `await(超时)`。
2. **CyclicBarrier 参与线程数固定**：线程不足会永远等不齐——结合超时 + 断开放行（BrokenBarrierException 处理）。
3. **Semaphore 许可泄漏**：acquire 后业务异常忘了 release → 许可被吃掉，服务逐渐限死。与锁一样 try-finally 包裹，或 tryAcquire 失败快速返回。
4. **阻塞队列的 offer/poll 带超时**：`offer(item, 1, SECONDS)` 比 put 可中断、可退让，生产代码默认用它。
5. **无界队列要防内存**：OOM 往往不是"内存不够"，而是"队列里堆了 1000 万个未消费任务"——有界队列 + 拒绝策略是线程池标配，见 [07 篇](07-线程池.md)。

> 🎯 **核心要点**：四个协作工具是 AQS 的四种语义（等齐/栅栏/限流/动态阶段），选型看场景；Condition 让唤醒精准化，new 代码不用 wait/notify；阻塞队列是协作的默认载体，有界 + 超时是生产红线。

---

**下一模块**：[06 并发容器](06-并发容器.md)

**返回总览**：[00-总览](00-总览.md)
