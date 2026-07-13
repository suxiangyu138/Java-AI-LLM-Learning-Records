# Java线程间通信核心知识点全解（含实操扩展）

线程间通信是多线程编程的核心环节，核心解决多线程协同工作、有序协作的问题——当多个线程并发执行、共享资源时，需通过特定通信机制避免无效轮询、数据不一致，实现等待-唤醒、顺序执行、结果传递等复杂逻辑。Java提供了从底层原生机制到高层封装工具的完整通信方案，覆盖从基础到进阶的所有应用场景，以下是详细解析（含扩展实操、底层原理及避坑指南）。

## 一、线程间通信的核心原理与背景

### 1.1 为什么必须要有线程间通信？

多线程环境中，不同线程通常承担不同的业务任务（如生产者生产数据、消费者处理数据、协作线程按步骤执行任务），若无通信机制，线程只能通过"无限循环+休眠"的轮询方式感知状态变化，会导致三大问题：

- **CPU资源浪费**：空轮询会持续占用处理器资源，即使线程无实际任务可执行，也会频繁检查状态，导致系统性能下降；
- **响应延迟**：轮询的休眠时间难以精准控制——休眠过久会导致线程无法及时响应状态变化，休眠过短则加剧CPU消耗；
- **数据不一致**：缺乏同步与通信的协同，多个线程并发访问共享资源时，会出现数据读取错误、修改覆盖等问题，破坏数据完整性。

> **线程间通信的核心目标**：让线程在条件不满足时主动等待，条件满足时被精准唤醒，实现高效协作，同时保证共享资源的线程安全。

### 1.2 通信的基础前提（必记）

所有Java线程间通信，都必须满足两个核心前提，否则会出现线程安全问题或通信失败：

- **共享载体**：多个线程必须基于同一个共享对象/资源进行通信（如共享队列、共享变量、锁对象），这是线程间传递状态、数据的基础；
- **线程安全**：对共享资源的操作必须保证原子性、可见性和有序性，通常通过`synchronized`内置锁或Lock显式锁实现，避免并发修改导致的数据错乱。

> **补充扩展**：共享资源的选择优先级——优先使用不可变对象（如String、Integer）作为通信载体，若使用可变对象（如自定义实体类），需确保所有修改操作都被同步控制，避免线程读取到中间态数据。

## 二、传统线程间通信机制（基于Object监视器）

这是Java最底层、最原生的线程通信方式，依赖Object类的三个核心方法（`wait()`、`notify()`、`notifyAll()`），适用于简单的等待-唤醒场景（如基础生产者-消费者模型），无需依赖任何额外工具类，原生支持。

### 2.1 核心方法详解（含扩展细节）

三个方法均为Object类的 native 方法，**必须在同步代码块/同步方法中调用（持有对象锁时）**，否则会抛出`IllegalMonitorStateException`异常，具体用法、行为特征及扩展细节如下：

| 方法 | 核心作用 | 是否释放锁 | 调用条件 | 扩展细节（易忽略点） |
| --- | --- | --- | --- | --- |
| `wait()` | 使当前线程进入无限期等待状态，直到被`notify()`/`notifyAll()`唤醒，或被中断 | 是（立即释放持有的对象锁） | 1. 必须在`synchronized`同步代码块/方法中；2. 调用线程必须持有当前对象的锁 | 1. 线程进入等待队列后，会释放锁，让其他线程有机会获取锁执行任务；2. 被唤醒后，线程不会立即执行，需重新竞争锁，竞争成功后才会继续执行`wait()`之后的代码；3. 支持带超时参数的重载方法（`wait(long timeout)`），超时后自动唤醒 |
| `notify()` | 随机唤醒一个在当前对象上等待的线程（无法指定唤醒目标） | 否（唤醒后仍持有锁，直到同步代码块/方法执行完毕才释放） | 与`wait()`一致，必须持有当前对象的锁 | 1. 唤醒的线程是"随机"的，无法精准控制，适用于所有等待线程执行逻辑一致的场景；2. 若没有线程在等待，调用`notify()`无任何效果；3. 唤醒后，被唤醒线程进入就绪状态，参与锁竞争 |
| `notifyAll()` | 唤醒所有在当前对象上等待的线程，让所有线程参与锁竞争 | 否（与`notify()`一致，需执行完同步代码才释放锁） | 与`wait()`一致，必须持有当前对象的锁 | 1. 避免"虚假唤醒"导致的逻辑异常，是实际开发中更常用的方式；2. 唤醒所有线程后，所有线程会竞争同一把锁，只有一个线程能获取锁执行，其余线程继续等待；3. 相比`notify()`，会增加锁竞争的开销，但更安全 |

### 2.2 关键行为细节（避坑重点）

- **`wait()`释放锁，`notify()`/`notifyAll()`不释放锁**：这是最核心的区别，也是最易踩坑的点——调用`wait()`后，线程立即释放锁，而`notify()`后，线程仍持有锁，需执行完同步代码块才会释放，被唤醒的线程需等待锁释放后才能竞争。
- **虚假唤醒（必须处理）**：线程可能被JVM意外唤醒（如系统中断、JVM内部调度），此时等待条件并未满足，若用`if`判断条件，会导致线程错误执行后续逻辑。规范要求：**必须用`while`循环检查等待条件，而非`if`**，确保即使被虚假唤醒，也会重新检查条件，不满足则继续等待。
- **锁对象一致性**：`wait()`、`notify()`、`notifyAll()`必须调用同一个对象的方法，否则无法实现通信（如线程A用`obj1.wait()`，线程B用`obj2.notify()`，线程A永远不会被唤醒）。

### 2.3 标准实操示例（生产者-消费者模型，扩展完善）

以"固定容量的消息队列"为共享资源，实现生产者生产消息、消费者消费消息的协同，完整覆盖`wait()`/`notifyAll()`的用法、虚假唤醒处理、线程安全控制，可直接复制运行：

```java
// 共享资源：固定容量的消息队列（通信载体）
class MessageQueue {
    // 用LinkedList实现队列，线程不安全，需同步控制
    private final Queue<String> queue = new LinkedList<>();
    private final int CAPACITY = 5; // 队列最大容量，避免无限生产

    // 生产者方法：添加消息（同步方法，保证线程安全）
    public synchronized void produce(String message) throws InterruptedException {
        // 关键：用while循环检查条件，防止虚假唤醒
        while (queue.size() == CAPACITY) {
            System.out.println("队列已满，生产者" + Thread.currentThread().getName() + "等待...");
            wait(); // 队列满，释放锁，进入等待
        }
        // 生产消息（临界区操作，仅保留必要逻辑，减少锁持有时间）
        queue.add(message);
        System.out.println("生产者" + Thread.currentThread().getName() + "生产：" + message
                         + "，当前队列大小：" + queue.size());
        // 唤醒消费者：有消息可消费，用notifyAll()避免遗漏
        notifyAll();
    }

    // 消费者方法：获取消息（同步方法，保证线程安全）
    public synchronized String consume() throws InterruptedException {
        // 关键：用while循环检查条件，防止虚假唤醒
        while (queue.isEmpty()) {
            System.out.println("队列空，消费者" + Thread.currentThread().getName() + "等待...");
            wait(); // 队列空，释放锁，进入等待
        }
        // 消费消息（临界区操作）
        String message = queue.poll();
        System.out.println("消费者" + Thread.currentThread().getName() + "消费：" + message
                         + "，当前队列大小：" + queue.size());
        // 唤醒生产者：有空间可生产
        notifyAll();
        return message;
    }
}

// 测试类：启动多个生产者和消费者，模拟并发通信
public class WaitNotifyTest {
    public static void main(String[] args) {
        MessageQueue queue = new MessageQueue();
        // 启动3个生产者线程
        for (int i = 0; i < 3; i++) {
            int producerId = i + 1;
            new Thread(() -> {
                try {
                    // 模拟持续生产消息
                    for (int j = 0; j < 4; j++) {
                        String message = "消息" + (j + 1);
                        queue.produce(message);
                        // 模拟生产耗时，避免消息生产过快
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    // 正确处理中断：恢复线程中断状态，避免中断丢失
                    Thread.currentThread().interrupt();
                    System.out.println("生产者" + Thread.currentThread().getName() + "被中断");
                }
            }, "P" + producerId).start();
        }
        // 启动2个消费者线程
        for (int i = 0; i < 2; i++) {
            int consumerId = i + 1;
            new Thread(() -> {
                try {
                    // 模拟持续消费消息
                    while (true) {
                        queue.consume();
                        // 模拟消费耗时
                        Thread.sleep(800);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("消费者" + Thread.currentThread().getName() + "被中断");
                }
            }, "C" + consumerId).start();
        }
    }
}
```

> **代码扩展说明**：1. 增加多生产者、多消费者场景，更贴近实际并发开发；2. 处理线程中断，避免中断丢失；3. 模拟生产/消费耗时，还原真实业务场景；4. 打印线程名称和队列状态，便于调试观察。

## 三、显式锁通信机制（基于Lock与Condition）

JUC（`java.util.concurrent`）框架引入的Lock（显式锁）和Condition（条件对象），是对传统wait/notify机制的重大升级，解决了传统机制灵活性不足、无法精准唤醒的问题，适用于复杂并发场景（如多条件控制、可中断等待、超时等待）。

### 3.1 核心优势（与传统机制对比，扩展细节）

传统`synchronized + wait/notify`存在明显局限性，而`Lock + Condition`完美解决，具体对比如下（补充底层实现差异）：

| 特性 | `synchronized + wait/notify` | `ReentrantLock + Condition` | 底层实现差异 |
| --- | --- | --- | --- |
| 锁类型 | 内置锁，JVM层面实现，自动获取/释放 | 显式锁，API层面实现，手动`lock()`/`unlock()` | `synchronized`依赖JVM监视器锁，Condition依赖Lock的AQS（抽象队列同步器）实现 |
| 等待队列 | 单个等待队列，所有等待线程共享 | 多个独立等待队列，每个Condition对应一个队列 | `synchronized`只有一个等待队列，`notifyAll()`会唤醒所有等待线程；Condition为每个条件维护一个队列，可精准唤醒 |
| 公平性 | 不支持公平锁，默认非公平 | 支持公平/非公平锁，可通过构造函数配置 | 公平锁会按线程等待顺序分配锁，非公平锁随机分配，公平锁性能略低但更有序 |
| 可中断锁 | 不支持，等待线程无法被中断 | 支持，`lockInterruptibly()`可响应中断 | 可中断锁允许等待线程被中断，避免无限等待，适用于需要取消任务的场景 |
| 超时获取锁 | 不支持，只能无限等待 | 支持，`tryLock(long timeout, TimeUnit unit)` | 超时获取锁可避免线程永久阻塞，提升系统稳定性 |
| 精准唤醒 | 不支持，只能随机唤醒或唤醒所有 | 支持，`signal()`唤醒指定队列的单个线程 | 通过多个Condition对象，实现"按条件唤醒"，减少无效上下文切换 |

### 3.2 核心API详解（含扩展用法）

#### （1）Lock核心API（以ReentrantLock为例）

- `lock()`：获取锁，若锁被占用则阻塞，直到获取锁为止（不可中断）；
- `lockInterruptibly()`：可中断获取锁，等待过程中若线程被中断，会抛出`InterruptedException`；
- `tryLock()`：尝试获取锁，立即返回boolean值（`true`：获取成功，`false`：获取失败），不阻塞；
- `tryLock(long timeout, TimeUnit unit)`：超时尝试获取锁，超时未获取则返回`false`，支持中断；
- `unlock()`：释放锁，必须在`finally`中调用，避免锁泄漏（即使出现异常，也能确保锁释放）；
- `newCondition()`：创建一个Condition对象，与当前Lock绑定，用于条件等待-唤醒。

#### （2）Condition核心API（与Object方法对应，扩展细节）

| Condition方法 | 对应Object方法 | 核心功能 | 扩展用法 |
| --- | --- | --- | --- |
| `await()` | `wait()` | 使当前线程进入等待状态，释放锁，可被中断 | 等待期间会释放锁，被唤醒后需重新竞争锁，与`wait()`行为一致，但属于Condition队列 |
| `awaitUninterruptibly()` | -（无对应方法） | 进入等待状态，不响应中断，直到被唤醒 | 适用于不允许中断等待的场景（如核心任务的等待逻辑） |
| `await(long time, TimeUnit unit)` | `wait(long timeout)` | 超时等待，超时后自动唤醒，返回是否超时 | 返回`true`：被唤醒；返回`false`：超时，可避免无限等待 |
| `signal()` | `notify()` | 唤醒当前Condition队列中的一个等待线程 | 精准唤醒，只唤醒对应条件的线程，避免无效唤醒 |
| `signalAll()` | `notifyAll()` | 唤醒当前Condition队列中的所有等待线程 | 适用于多个线程等待同一条件的场景，安全且不易遗漏 |

### 3.3 进阶实操示例（多条件精准控制，扩展完善）

以"高级阻塞队列"为场景，实现"生产者等待队列满、消费者等待队列空"的精准控制，每个条件对应独立的等待队列，避免无效唤醒，比传统wait/notify更高效，可直接用于实际开发：

```java
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// 高级阻塞队列：多条件精准控制（生产者/消费者分开等待）
class AdvancedBlockingQueue<T> {
    private final Lock lock = new ReentrantLock(true); // 公平锁，保证线程顺序执行
    // 两个独立Condition：分别对应"队列未满"和"队列非空"条件
    private final Condition notFull = lock.newCondition();  // 生产者等待的条件
    private final Condition notEmpty = lock.newCondition(); // 消费者等待的条件

    private final T[] items; // 存储队列元素
    private int count; // 当前队列元素数量
    private int takeIndex; // 消费索引（出队位置）
    private int putIndex; // 生产索引（入队位置）

    // 构造方法：初始化队列容量
    public AdvancedBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("队列容量必须大于0");
        }
        this.items = (T[]) new Object[capacity];
    }

    // 生产者入队：队列满则等待，不满则生产
    public void put(T t) throws InterruptedException {
        lock.lockInterruptibly(); // 可中断获取锁，避免无限等待
        try {
            // 循环检查条件，防止虚假唤醒
            while (count == items.length) {
                System.out.println("队列已满，生产者" + Thread.currentThread().getName() + "等待...");
                notFull.await(); // 在notFull队列等待，释放锁
            }
            // 入队操作（循环队列实现，提升空间利用率）
            items[putIndex] = t;
            if (++putIndex == items.length) {
                putIndex = 0; // 到达队列末尾，回到开头
            }
            count++;
            System.out.println("生产者" + Thread.currentThread().getName() + "生产：" + t
                             + "，当前队列大小：" + count);
            // 精准唤醒消费者：只有消费者在等待"队列非空"条件
            notEmpty.signal();
        } finally {
            lock.unlock(); // 必须在finally释放锁，避免锁泄漏
        }
    }

    // 消费者出队：队列空则等待，非空则消费
    public T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                System.out.println("队列空，消费者" + Thread.currentThread().getName() + "等待...");
                notEmpty.await(); // 在notEmpty队列等待，释放锁
            }
            // 出队操作
            T x = items[takeIndex];
            items[takeIndex] = null; // 置空，帮助GC回收
            if (++takeIndex == items.length) {
                takeIndex = 0;
            }
            count--;
            System.out.println("消费者" + Thread.currentThread().getName() + "消费：" + x
                             + "，当前队列大小：" + count);
            // 精准唤醒生产者：只有生产者在等待"队列未满"条件
            notFull.signal();
            return x;
        } finally {
            lock.unlock();
        }
    }
}

// 测试类：多生产者、多消费者精准通信
public class LockConditionTest {
    public static void main(String[] args) {
        AdvancedBlockingQueue<String> queue = new AdvancedBlockingQueue<>(5);
        // 启动2个生产者线程
        for (int i = 0; i < 2; i++) {
            int producerId = i + 1;
            new Thread(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        String message = "消息" + producerId + "-" + (j + 1);
                        queue.put(message);
                        Thread.sleep(600); // 模拟生产耗时
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("生产者" + Thread.currentThread().getName() + "被中断");
                }
            }, "P" + producerId).start();
        }
        // 启动3个消费者线程
        for (int i = 0; i < 3; i++) {
            int consumerId = i + 1;
            new Thread(() -> {
                try {
                    while (true) {
                        queue.take();
                        Thread.sleep(1000); // 模拟消费耗时
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("消费者" + Thread.currentThread().getName() + "被中断");
                }
            }, "C" + consumerId).start();
        }
    }
}
```

> **核心扩展点**：1. 采用循环队列实现，提升空间利用率，避免队列满后无法复用空间；2. 使用公平锁，保证线程按等待顺序获取锁，避免饥饿；3. 两个Condition精准区分生产者和消费者的等待条件，唤醒时只唤醒相关线程，减少上下文切换，提升并发效率。

## 四、JUC高级并发通信工具（封装式开发，提升效率）

对于复杂的线程协作场景（如主线程等待多子线程完成、多线程互相等待、限流控制），JUC提供了封装好的高级工具类，无需手动实现等待-唤醒逻辑，底层封装了Lock/Condition或CAS机制，简化开发、提升可维护性，以下是核心工具类的扩展详解（含实操场景）。

### 4.1 CountDownLatch（倒计时门闩）

#### 核心功能

一个线程（通常是主线程）等待其他N个线程完成任务后，再继续执行，是"单线程等待多线程"的典型场景，计数一次性有效（计数减为0后，无法重置，若需循环使用，需重新创建实例）。

#### 核心API（扩展细节）

- `CountDownLatch(int count)`：构造方法，初始化倒计时总数（count为需要等待的线程数/任务数）；
- `countDown()`：倒计时减1，由每个子线程完成任务后调用，线程安全（底层CAS实现）；
- `await()`：主线程调用，阻塞等待计数减为0，可响应中断；
- `await(long timeout, TimeUnit unit)`：超时等待，超时后无论计数是否为0，都继续执行，返回是否超时（`true`：未超时，`false`：超时）；
- `getCount()`：获取当前剩余计数，可用于调试或监控。

#### 实战场景扩展（多服务初始化）

模拟"主线程等待5个服务初始化完成后，启动应用"，贴合实际开发中的服务启动场景，补充异常处理和日志打印：

```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CountDownLatchTest {
    public static void main(String[] args) {
        // 初始化倒计时：等待5个服务启动
        CountDownLatch latch = new CountDownLatch(5);
        // 启动5个服务初始化线程
        for (int i = 0; i < 5; i++) {
            int serviceId = i + 1;
            new Thread(() -> {
                try {
                    // 模拟服务初始化耗时（1-3秒随机）
                    long time = (long) (Math.random() * 2000 + 1000);
                    Thread.sleep(time);
                    System.out.println("服务" + serviceId + "初始化完成，耗时：" + time + "ms");
                    latch.countDown(); // 服务完成，倒计时减1
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("服务" + serviceId + "初始化被中断");
                }
            }, "Service-" + serviceId).start();
        }
        System.out.println("主线程等待所有服务初始化...");
        try {
            // 超时等待：最多等待5秒，避免无限阻塞
            boolean isCompleted = latch.await(5, TimeUnit.SECONDS);
            if (isCompleted) {
                System.out.println("所有服务初始化完成，应用启动成功！");
            } else {
                System.out.println("超时警告：部分服务未完成初始化，应用启动失败！");
                System.out.println("剩余未完成服务数：" + latch.getCount());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("主线程等待被中断，应用启动终止");
        }
    }
}
```

### 4.2 CyclicBarrier（循环屏障）

#### 核心功能

一组线程互相等待，直到所有线程都到达"共同的屏障点"，然后一起继续执行，与CountDownLatch的核心区别是：**可循环使用**（计数重置后，可再次等待），支持"屏障任务"（所有线程到达后，先执行一个汇总任务）。

#### 核心API（扩展细节）

- `CyclicBarrier(int parties)`：构造方法，初始化参与等待的线程数（parties）；
- `CyclicBarrier(int parties, Runnable barrierAction)`：带屏障任务的构造方法，所有线程到达后，先执行`barrierAction`（由最后一个到达的线程执行）；
- `await()`：线程到达屏障点，阻塞等待其他线程，可响应中断；
- `await(long timeout, TimeUnit unit)`：超时等待，超时后抛出`BrokenBarrierException`；
- `getNumberWaiting()`：获取当前等待的线程数；
- `reset()`：重置屏障，计数恢复为初始值，可循环使用；
- `isBroken()`：判断屏障是否被破坏（如线程超时、中断，会导致屏障破坏）。

#### 实战场景扩展（多线程分批计算）

模拟"4个线程为一组，分批计算数据，每组计算完成后汇总结果"，循环执行3批，体现CyclicBarrier的循环复用特性：

```java
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class CyclicBarrierTest {
    public static void main(String[] args) {
        // 4个线程为一组，每组完成后执行汇总任务
        CyclicBarrier barrier = new CyclicBarrier(4, () -> {
            // 屏障任务：所有线程到达后执行，由最后一个线程触发
            System.out.println("=== 本组计算完成，开始汇总结果 ===");
        });
        // 启动8个线程，分2组执行（循环使用屏障）
        for (int i = 0; i < 8; i++) {
            int threadId = i + 1;
            new Thread(() -> {
                try {
                    // 模拟计算任务（每批任务耗时不同）
                    long time = (long) (Math.random() * 1500 + 500);
                    Thread.sleep(time);
                    System.out.println("线程" + threadId + "完成计算，耗时：" + time + "ms，等待同组线程");
                    // 到达屏障点，等待同组其他线程
                    barrier.await(3, TimeUnit.SECONDS); // 超时等待3秒
                    // 屏障放行后，继续执行后续逻辑
                    System.out.println("线程" + threadId + "进入下一批任务");
                } catch (Exception e) {
                    // 处理中断、超时、屏障破坏异常
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        System.out.println("线程" + threadId + "被中断");
                    } else if (e instanceof java.util.concurrent.TimeoutException) {
                        System.out.println("线程" + threadId + "等待超时，屏障破坏");
                    } else {
                        System.out.println("线程" + threadId + "等待异常：" + e.getMessage());
                    }
                }
            }, "Thread-" + threadId).start();
        }
    }
}
```

> **避坑要点**：若有线程超时或中断，会导致屏障被破坏，后续线程调用`await()`会直接抛出`BrokenBarrierException`，需通过`isBroken()`判断屏障状态，必要时调用`reset()`重置。

### 4.3 Semaphore（信号量）

#### 核心功能

控制同时访问特定资源的线程数量，本质是"并发限流"，适用于资源池管理（如数据库连接池、线程池）、接口限流等场景，底层通过AQS实现，维护一个许可池。

#### 核心API（扩展细节）

- `Semaphore(int permits)`：构造方法，初始化许可数量（permits为允许同时访问的线程数）；
- `Semaphore(int permits, boolean fair)`：支持公平/非公平锁，公平锁按线程等待顺序分配许可；
- `acquire()`：获取1个许可，无许可则阻塞，可响应中断；
- `acquire(int permits)`：获取指定数量的许可，适用于需要多个资源的场景；
- `release()`：释放1个许可，必须在`finally`中调用，避免许可泄漏；
- `release(int permits)`：释放指定数量的许可；
- `tryAcquire()`：尝试获取1个许可，立即返回boolean，不阻塞；
- `availablePermits()`：获取当前可用的许可数量，用于监控。

#### 实战场景扩展（数据库连接池限流）

模拟"数据库连接池有3个连接，10个并发请求访问，控制同时只有3个请求获取连接"，贴合实际开发中的资源池限流场景：

```java
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SemaphoreTest {
    // 模拟数据库连接池，最多3个连接（许可数量为3）
    private static final Semaphore semaphore = new Semaphore(3, true); // 公平锁，按顺序分配连接

    public static void main(String[] args) {
        // 模拟10个并发请求
        for (int i = 0; i < 10; i++) {
            int requestId = i + 1;
            new Thread(() -> {
                try {
                    // 获取数据库连接（获取1个许可）
                    semaphore.acquire();
                    System.out.println("请求" + requestId + "获取数据库连接，开始执行SQL");
                    // 模拟SQL执行耗时
                    Thread.sleep((long) (Math.random() * 1000 + 500));
                    System.out.println("请求" + requestId + "执行完成，释放数据库连接");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("请求" + requestId + "被中断，未获取连接");
                } finally {
                    // 释放连接（释放许可），必须在finally中，避免许可泄漏
                    semaphore.release();
                }
            }, "Request-" + requestId).start();
        }
        // 监控许可使用情况（单独线程）
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    int available = semaphore.availablePermits();
                    System.out.println("当前可用数据库连接数：" + available);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Monitor-Thread").start();
    }
}
```

### 4.4 Exchanger（交换器）

#### 核心功能

用于两个线程之间点对点的数据交换，线程调用`exchange()`方法后，会阻塞等待另一个线程到达，两个线程都到达后，互相交换数据，适用于数据交换、配对处理等场景（如生产者-消费者数据交换、线程间结果传递）。

#### 核心API（扩展细节）

- `Exchanger<V>`：泛型类，V为交换的数据类型；
- `exchange(V data)`：交换数据，阻塞直到另一个线程调用`exchange()`，返回对方交换的数据；
- `exchange(V data, long timeout, TimeUnit unit)`：超时交换，超时后抛出`TimeoutException`。

#### 实战场景扩展（生产者-消费者数据交换）

模拟"生产者生产数据，消费者处理数据，两者交换数据（生产者传递原始数据，消费者传递处理结果）"：

```java
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

public class ExchangerTest {
    public static void main(String[] args) {
        // 交换器：交换String类型数据（生产者传递原始数据，消费者传递处理结果）
        Exchanger<String> exchanger = new Exchanger<>();

        // 生产者线程：生产原始数据
        new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    // 模拟生产数据
                    String rawData = "原始数据" + (i + 1);
                    System.out.println("生产者生产：" + rawData);
                    // 等待消费者，交换数据（传递原始数据，获取处理结果）
                    String result = exchanger.exchange(rawData, 2, TimeUnit.SECONDS);
                    System.out.println("生产者获取处理结果：" + result);
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    System.out.println("生产者被中断");
                } else {
                    System.out.println("生产者交换超时/异常：" + e.getMessage());
                }
            }
        }, "Producer").start();

        // 消费者线程：处理数据，返回处理结果
        new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    // 等待生产者，交换数据（获取原始数据，传递处理结果）
                    String rawData = exchanger.exchange(null); // 先传递null，获取原始数据
                    // 模拟数据处理
                    String processedData = "【已处理】" + rawData;
                    System.out.println("消费者处理：" + rawData + " → " + processedData);
                    // 再次交换，传递处理结果（此处可优化为一次交换，简化逻辑）
                    exchanger.exchange(processedData);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("消费者被中断");
            }
        }, "Consumer").start();
    }
}
```

> **扩展说明**：Exchanger仅适用于两个线程之间的交换，若需多个线程交换数据，需结合其他工具类（如ConcurrentHashMap），实际开发中常用于配对任务（如线程配对处理数据）。

## 五、线程间通信的选择与最佳实践（重点扩展）

### 5.1 场景选择指南（补充细节，便于实际开发选型）

| 通信场景 | 推荐方案 | 核心优势 | 避坑建议 |
| --- | --- | --- | --- |
| 简单等待-唤醒（如基础生产者-消费者） | `synchronized + wait/notify` | 原生支持，无需额外依赖，代码简洁 | 必须用`while`循环检查条件，避免虚假唤醒；确保wait/notify调用同一个锁对象 |
| 多条件精准控制（如多生产者/多消费者，不同条件等待） | `Lock + Condition` | 多等待队列，精准唤醒，支持可中断、超时等待，灵活性高 | Lock必须在`finally`中释放；Condition必须与同一个Lock绑定 |
| 主线程等待多子线程完成（如服务初始化） | `CountDownLatch` | 封装完善，无需手动实现等待-唤醒，代码简洁 | 计数一次性有效，需循环使用时，需重新创建实例；建议设置超时等待 |
| 多线程互相等待，循环复用（如分批计算） | `CyclicBarrier` | 可循环使用，支持屏障任务，适合分组协作 | 注意屏障破坏后的异常处理，必要时调用`reset()`重置 |
| 并发限流、资源池管理（如数据库连接池） | `Semaphore` | 精准控制并发数，支持公平/非公平锁，适配限流场景 | 许可必须在`finally`中释放，避免许可泄漏；根据场景选择公平/非公平锁 |
| 两个线程点对点数据交换（如数据处理、结果传递） | `Exchanger` | 专门用于双线程交换，代码简洁，无需手动同步 | 仅支持两个线程，多线程场景不适用；建议设置超时时间 |

### 5.2 避坑要点（扩展补充，覆盖高频踩坑场景）

- **虚假唤醒必处理**：无论使用`wait()`还是`await()`，都必须用`while`循环检查等待条件，绝对不能用`if`，这是JDK官方规范，避免线程被意外唤醒后执行错误逻辑。
- **锁释放要及时**：显式Lock必须在`finally`中调用`unlock()`，`synchronized`无需手动释放，但要避免同步代码块过长，减少锁持有时间；Semaphore的许可必须在`finally`中释放，避免泄漏。
- **中断处理要规范**：所有可中断的等待方法（`wait()`、`await()`、`acquire()`），都要捕获`InterruptedException`，并且恢复线程的中断状态（`Thread.currentThread().interrupt()`），避免中断丢失。
- **锁对象要一致**：`wait()`、`notify()`必须调用同一个对象的方法，Condition必须与创建它的Lock绑定，否则会抛出异常，导致通信失败。
- **避免过度同步**：无需同步的代码不要放入同步块，减少锁竞争；简单场景用基础方案，复杂场景用高级工具，避免过度封装导致代码冗余。
- **超时机制必添加**：所有阻塞等待的场景（如`await()`、`acquire()`、`exchange()`），建议添加超时时间，避免线程永久阻塞，提升系统稳定性。

### 5.3 性能优化建议（扩展补充）

- **减少锁竞争**：尽量缩小同步代码块范围，只包含临界区操作；用CAS机制替代锁（如AtomicInteger），减少上下文切换。
- **精准唤醒优先**：优先使用Lock+Condition的`signal()`，而非`notifyAll()`，避免无效唤醒导致的CPU消耗和上下文切换。
- **线程池配合使用**：大量短生命周期线程，用线程池复用，减少线程创建/销毁开销，与JUC通信工具配合，提升并发效率。
- **选择合适的公平性**：非公平锁性能优于公平锁，若无需保证线程执行顺序，优先使用非公平锁（默认）；需避免线程饥饿时，使用公平锁。

## 六、核心总结（完整版）

Java线程间通信的核心是"基于共享资源，通过等待-唤醒机制实现线程协同"，整个体系分为三个层级，层层递进，适配不同复杂度的场景：

- **基础层**：`synchronized + wait/notify`，原生支持，适用于简单等待-唤醒场景，核心注意虚假唤醒和锁对象一致性；
- **进阶层**：`Lock + Condition`，解决基础层灵活性不足的问题，支持多条件精准唤醒、可中断等待、超时等待，适用于复杂并发场景；
- **工具层**：JUC高级工具类（`CountDownLatch`、`CyclicBarrier`、`Semaphore`、`Exchanger`），封装底层通信逻辑，简化开发，提升可维护性，适配特定业务场景（限流、分组协作、数据交换）。

实际开发中，无需过度追求"高级方案"，应根据业务复杂度、性能需求、可维护性选择合适的通信方式，遵循"最小化同步、精准唤醒、规范异常处理"的原则，才能写出高效、稳定、易维护的多线程代码。同时，线程间通信必须与线程安全结合，确保共享资源的原子性、可见性和有序性，避免出现数据不一致、线程泄漏等问题。
